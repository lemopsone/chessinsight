#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import math
import os
import platform
import re
import shutil
import subprocess
import time
import urllib.parse
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_OUTPUT_DIR = ROOT_DIR / "reports" / "perf" / "observability"
TRACE_EXPORT_FILE = ROOT_DIR / "reports" / "perf" / "otel" / "traces.jsonl"


@dataclass(frozen=True)
class Scenario:
    name: str
    tracing_enabled: bool
    log_level_chessinsight: str


SCENARIOS = [
    Scenario(name="baseline", tracing_enabled=False, log_level_chessinsight="INFO"),
    Scenario(name="tracing_enabled", tracing_enabled=True, log_level_chessinsight="INFO"),
    Scenario(name="extended_logging", tracing_enabled=False, log_level_chessinsight="DEBUG"),
    Scenario(name="tracing_and_extended_logging", tracing_enabled=True, log_level_chessinsight="DEBUG"),
]

EXTENDED_DEBUG_LOGGERS = (
    "LOG_LEVEL_SPRING_WEB",
    "LOG_LEVEL_SPRING_SECURITY",
    "LOG_LEVEL_SPRING_WEBFILTER",
    "LOG_LEVEL_MICROMETER_TRACING",
    "LOG_LEVEL_OPENTELEMETRY_EXPORTER",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compare backend resources with tracing/logging configurations")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--rps", type=int, default=8)
    parser.add_argument("--duration", default="45s")
    parser.add_argument("--warmup-duration", default="10s", help="Warmup load duration per scenario (set 0s to disable)")
    parser.add_argument("--warmup-rps", type=int, default=0, help="Warmup RPS (0 means use --rps)")
    parser.add_argument("--sleep-after-scenario", type=float, default=6.0)
    parser.add_argument("--prometheus-url", default="http://localhost:9090")
    parser.add_argument("--backend-base-url", default="http://localhost:8080")
    parser.add_argument("--grafana-base-url", default="http://localhost:3000")
    parser.add_argument("--jaeger-base-url", default="http://localhost:16686/jaeger")
    parser.add_argument(
        "--ui-mode",
        default="auto",
        choices=("auto", "off", "direct", "gateway"),
        help="UI mode for local diagnostics: auto (gateway locally, off in GitHub Actions), off, direct, gateway",
    )
    parser.add_argument("--keep-up", action="store_true", help="Keep docker compose environment after run")
    return parser.parse_args()


def run_command(cmd: list[str], env: dict[str, str] | None = None, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        cwd=ROOT_DIR,
        env=env,
        check=check,
        text=True,
        capture_output=True,
    )


def run_command_stream(cmd: list[str], env: dict[str, str] | None = None) -> None:
    subprocess.run(cmd, cwd=ROOT_DIR, env=env, check=True)


def compose_cmd(*args: str, profiles: tuple[str, ...] = ("mock",)) -> list[str]:
    cmd = ["docker", "compose"]
    for profile_name in profiles:
        cmd.extend(["--profile", profile_name])
    cmd.extend(args)
    return cmd


def compose_up_resilient(env: dict[str, str], profiles: tuple[str, ...], *services: str) -> None:
    up_cmd = compose_cmd("up", "-d", *services, profiles=profiles)
    try:
        run_command_stream(up_cmd, env=env)
        return
    except subprocess.CalledProcessError:
        print(
            "[benchmark] compose up failed, attempting cleanup of stale compose state and retry",
            flush=True,
        )
        run_command(
            compose_cmd("down", "--remove-orphans", profiles=profiles),
            env=env,
            check=False,
        )
        time.sleep(2.0)
        run_command_stream(up_cmd, env=env)


def wait_http_ok(url: str, timeout_sec: float, sleep_sec: float = 2.0) -> None:
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=5.0) as resp:
                if 200 <= int(resp.status) < 300:
                    return
        except Exception:
            pass
        time.sleep(sleep_sec)
    raise RuntimeError(f"Timed out waiting for {url}")


def wait_http_any_non_5xx(url: str, timeout_sec: float, sleep_sec: float = 2.0) -> None:
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=5.0) as resp:
                if int(resp.status) < 500:
                    return
        except urllib.error.HTTPError as exc:
            if int(exc.code) < 500:
                return
        except Exception:
            pass
        time.sleep(sleep_sec)
    raise RuntimeError(f"Timed out waiting for non-5xx response from {url}")


def wait_backend_ready(backend_base_url: str, timeout_sec: float) -> None:
    base = backend_base_url.rstrip("/")
    candidates = [
        f"{base}/api/actuator/health",
        f"{base}/actuator/health",
        f"{base}/api/v1/openapi.yaml",
    ]
    for url in candidates:
        try:
            wait_http_any_non_5xx(url, timeout_sec=timeout_sec, sleep_sec=2.0)
            return
        except RuntimeError:
            continue
    raise RuntimeError(
        "Backend did not become ready on known endpoints: "
        + ", ".join(candidates)
    )


def run_with_retry(
    cmd: list[str],
    env: dict[str, str],
    timeout_sec: float,
    sleep_sec: float = 3.0,
) -> None:
    deadline = time.time() + timeout_sec
    last_error: Exception | None = None
    while time.time() < deadline:
        try:
            run_command_stream(cmd, env=env)
            return
        except subprocess.CalledProcessError as exc:
            last_error = exc
            time.sleep(sleep_sec)
    raise RuntimeError(f"Command failed after retries: {' '.join(cmd)}") from last_error


def prom_query_value(prometheus_url: str, query: str, query_time: float | None = None) -> float:
    params: dict[str, str] = {"query": query}
    if query_time is not None:
        params["time"] = f"{query_time:.3f}"
    url = f"{prometheus_url.rstrip('/')}/api/v1/query?{urllib.parse.urlencode(params)}"
    try:
        with urllib.request.urlopen(url, timeout=15.0) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Prometheus query failed with HTTP {exc.code}: {query}. Response: {body}"
        ) from exc
    if payload.get("status") != "success":
        return 0.0
    data = payload.get("data", {})
    result = data.get("result") or []
    if not result:
        return 0.0
    value = result[0].get("value")
    if not value or len(value) < 2:
        return 0.0
    try:
        parsed = float(value[1])
    except (TypeError, ValueError):
        return 0.0
    if math.isnan(parsed) or math.isinf(parsed):
        return 0.0
    return parsed


def prom_query_vector(
    prometheus_url: str,
    query: str,
    metric_label: str,
    query_time: float | None = None,
) -> dict[str, float]:
    params: dict[str, str] = {"query": query}
    if query_time is not None:
        params["time"] = f"{query_time:.3f}"
    url = f"{prometheus_url.rstrip('/')}/api/v1/query?{urllib.parse.urlencode(params)}"
    try:
        with urllib.request.urlopen(url, timeout=15.0) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Prometheus query failed with HTTP {exc.code}: {query}. Response: {body}"
        ) from exc

    if payload.get("status") != "success":
        return {}

    data = payload.get("data", {})
    result = data.get("result") or []
    out: dict[str, float] = {}
    for item in result:
        label_value = str((item.get("metric") or {}).get(metric_label) or "unknown")
        value = item.get("value")
        if not value or len(value) < 2:
            continue
        try:
            parsed = float(value[1])
        except (TypeError, ValueError):
            continue
        if math.isnan(parsed) or math.isinf(parsed):
            continue
        out[label_value] = parsed
    return out


def prom_query_first_positive(
    prometheus_url: str,
    queries: list[str],
    query_time: float | None = None,
) -> tuple[float, str]:
    if not queries:
        return 0.0, ""
    last_query = queries[0]
    for query in queries:
        last_query = query
        value = prom_query_value(prometheus_url, query, query_time)
        if value > 0.0:
            return value, query
    return 0.0, last_query


def detect_resource_metrics_source(prometheus_url: str, query_time: float) -> str:
    cadvisor_count = prom_query_value(
        prometheus_url,
        'count(container_cpu_usage_seconds_total{container_label_com_docker_compose_service="backend"})',
        query_time,
    )
    return "cadvisor" if cadvisor_count > 0.0 else "process"


def slugify(value: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", value.strip()).strip("-").lower()
    return slug or "scenario"


def count_exported_spans(path: Path) -> int:
    if not path.is_file():
        return 0

    spans = 0
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            payload = json.loads(line)
        except json.JSONDecodeError:
            continue
        for resource_span in payload.get("resourceSpans", []) or []:
            for scope_span in resource_span.get("scopeSpans", []) or []:
                spans += len(scope_span.get("spans", []) or [])
    return spans


def get_container_logs(container_name: str) -> str:
    completed = run_command(["docker", "logs", container_name], check=False)
    return (completed.stdout or "") + (completed.stderr or "")


def count_backend_log_lines() -> int:
    logs = get_container_logs("backend")
    if not logs.strip():
        return 0
    return len(logs.splitlines())


def reset_trace_export_file(path: Path) -> None:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("", encoding="utf-8")
        return
    except PermissionError:
        pass

    temp_empty_file = Path("/tmp/otel-empty-traces.jsonl")
    temp_empty_file.write_text("", encoding="utf-8")
    completed = run_command(
        [
            "docker",
            "cp",
            str(temp_empty_file),
            "otel-collector:/var/lib/otel/traces.jsonl",
        ],
        check=False,
    )
    if completed.returncode != 0:
        stderr = (completed.stderr or "").strip()
        stdout = (completed.stdout or "").strip()
        details = stderr or stdout or "no output"
        raise RuntimeError(
            "Unable to reset trace export file. "
            f"Local path is not writable and docker fallback failed: {details}"
        )


def bytes_to_mib(value: float) -> float:
    return value / (1024.0 * 1024.0)


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def format_float(value: float, digits: int = 3) -> str:
    return f"{value:.{digits}f}"


def format_percent(value: float) -> str:
    return f"{value:.2f}%"


def write_markdown_report(path: Path, metadata: dict[str, Any], rows: list[dict[str, Any]]) -> None:
    baseline = next((row for row in rows if row["scenario"] == "baseline"), None)

    lines: list[str] = []
    lines.append("# Отчет по наблюдаемости")
    lines.append("")
    lines.append(f"Сформирован: {metadata['generated_at_utc']}")
    lines.append(
        f"Нагрузка: RPS={metadata['rps']}, длительность={metadata['duration']}, "
        f"прогрев={metadata.get('warmup_duration', '0s')}"
    )
    lines.append("")
    lines.append("## Метрики")
    lines.append("")
    lines.append("| Сценарий | Трейсинг | Логирование | CPU, с | RAM avg, MiB | RAM max, MiB | Запросы | Ошибки | p95, мс |")
    lines.append("|---|---|---|---:|---:|---:|---:|---:|---:|")

    for row in rows:
        lines.append(
            "| "
            f"{row['scenario']} | "
            f"{str(row['tracing_enabled']).lower()} | "
            f"{row['log_level_chessinsight']} | "
            f"{format_float(row['cpu_seconds'])} | "
            f"{format_float(row['avg_memory_mib'])} | "
            f"{format_float(row['max_memory_mib'])} | "
            f"{int(round(row['requests']))} | "
            f"{format_percent(row['error_rate_percent'])} | "
            f"{format_float(row['p95_ms'])} |"
        )

    lines.append("")
    lines.append("## Дельта к baseline")
    lines.append("")
    lines.append("| Сценарий | Δ CPU, с | Δ CPU, % | Δ RAM avg, MiB | Δ RAM avg, % | Δ p95, мс |")
    lines.append("|---|---:|---:|---:|---:|---:|")

    if baseline is None:
        for row in rows:
            lines.append(f"| {row['scenario']} | n/a | n/a | n/a | n/a | n/a |")
    else:
        baseline_cpu = baseline["cpu_seconds"]
        baseline_mem = baseline["avg_memory_mib"]
        baseline_p95 = baseline["p95_ms"]

        for row in rows:
            delta_cpu = row["cpu_seconds"] - baseline_cpu
            delta_mem = row["avg_memory_mib"] - baseline_mem
            delta_p95 = row["p95_ms"] - baseline_p95
            delta_cpu_pct = 0.0 if baseline_cpu <= 0 else (delta_cpu / baseline_cpu) * 100.0
            delta_mem_pct = 0.0 if baseline_mem <= 0 else (delta_mem / baseline_mem) * 100.0

            lines.append(
                "| "
                f"{row['scenario']} | "
                f"{format_float(delta_cpu)} | "
                f"{format_percent(delta_cpu_pct)} | "
                f"{format_float(delta_mem)} | "
                f"{format_percent(delta_mem_pct)} | "
                f"{format_float(delta_p95)} |"
            )

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def duration_to_seconds(duration: str) -> int:
    match = re.fullmatch(r"\s*(\d+)\s*([smhSMH])\s*", duration)
    if not match:
        raise ValueError(f"Unsupported duration format: {duration}")
    value = int(match.group(1))
    unit = match.group(2).lower()
    if unit == "s":
        return value
    if unit == "m":
        return value * 60
    if unit == "h":
        return value * 3600
    raise ValueError(f"Unsupported duration unit in {duration}")


def main() -> int:
    args = parse_args()
    if args.rps <= 0:
        raise SystemExit("--rps must be > 0")
    if args.warmup_rps < 0:
        raise SystemExit("--warmup-rps must be >= 0")

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    auth_state_file = Path("/tmp/chessinsight-perf/auth.json")

    base_env = os.environ.copy()
    base_env["ENGINE_STOCKFISH_HOST"] = "stockfish-mock"
    base_env["SECURITY_AUTH_REQUIRE_EMAIL_OTP"] = "false"
    base_env["SECURITY_AUTH_MAIL_ENABLED"] = "false"
    base_env["MANAGEMENT_HEALTH_ELASTICSEARCH_ENABLED"] = "false"
    base_env["PERF_AUTH_STATE_FILE"] = str(auth_state_file)

    host_system = platform.system().lower()
    runtime_profile = "macos" if host_system == "darwin" else "linux"
    in_github_actions = os.environ.get("GITHUB_ACTIONS", "").strip().lower() == "true"
    if args.ui_mode == "auto":
        effective_ui_mode = "off" if in_github_actions else "gateway"
    else:
        effective_ui_mode = args.ui_mode

    compose_profiles: tuple[str, ...] = ("mock", runtime_profile)
    infra_services = [
        "postgres-master",
        "redis",
        "stockfish-mock",
        "jaeger",
        "otel-collector",
    ]
    if runtime_profile == "macos":
        infra_services.append("cadvisor-desktop")
    else:
        infra_services.extend(["cadvisor", "node-exporter"])
    infra_services.extend(["prometheus", "backend"])

    if effective_ui_mode in {"direct", "gateway"}:
        infra_services.append("grafana")
    if effective_ui_mode == "gateway":
        infra_services.extend(["consul", "adminer", "gateway"])

    started_compose = False
    rows: list[dict[str, Any]] = []

    try:
        print("[benchmark] pre-cleaning stale compose state (if any)", flush=True)
        run_command(
            compose_cmd("down", "--remove-orphans", profiles=compose_profiles),
            env=base_env,
            check=False,
        )

        print("[benchmark] starting compose stack", flush=True)
        compose_up_resilient(
            base_env,
            compose_profiles,
            *infra_services,
        )
        started_compose = True

        wait_backend_ready(args.backend_base_url, timeout_sec=180)
        wait_http_ok(f"{args.prometheus_url.rstrip('/')}/-/healthy", timeout_sec=120)
        wait_http_any_non_5xx(args.jaeger_base_url.rstrip("/"), timeout_sec=120)

        grafana_ui_url = ""
        jaeger_ui_url = ""
        if effective_ui_mode == "direct":
            grafana_ui_url = args.grafana_base_url.rstrip("/")
            jaeger_ui_url = args.jaeger_base_url.rstrip("/")
            wait_http_any_non_5xx(grafana_ui_url, timeout_sec=120)
            wait_http_any_non_5xx(jaeger_ui_url, timeout_sec=120)
        elif effective_ui_mode == "gateway":
            grafana_ui_url = "http://localhost/monitoring/"
            jaeger_ui_url = "http://localhost/jaeger/"
            wait_http_any_non_5xx(grafana_ui_url, timeout_sec=180)
            wait_http_any_non_5xx(jaeger_ui_url, timeout_sec=180)
            print(
                f"[benchmark] UI via gateway is ready: grafana={grafana_ui_url} jaeger={jaeger_ui_url}",
                flush=True,
            )

        print("[benchmark] preparing auth token", flush=True)
        run_with_retry(
            [
                "python3",
                "ci/perf/get_token.py",
                "--base-url",
                args.backend_base_url,
                "--out-file",
                str(auth_state_file),
            ],
            env=base_env,
            timeout_sec=120,
        )

        duration_sec = duration_to_seconds(args.duration)
        warmup_duration_sec = duration_to_seconds(args.warmup_duration)
        warmup_rps = args.rps if args.warmup_rps == 0 else args.warmup_rps

        for scenario in SCENARIOS:
            slug = slugify(scenario.name)
            scenario_env = base_env.copy()
            scenario_env["TRACING_ENABLED"] = "true" if scenario.tracing_enabled else "false"
            scenario_env["TRACING_SAMPLING_PROBABILITY"] = "1.0"
            scenario_env["OTEL_EXPORTER_OTLP_ENDPOINT"] = "http://otel-collector:4318/v1/traces"
            scenario_env["LOG_LEVEL_ROOT"] = "INFO"
            scenario_env["LOG_LEVEL_CHESSINSIGHT"] = scenario.log_level_chessinsight
            for logger_name in EXTENDED_DEBUG_LOGGERS:
                scenario_env[logger_name] = "DEBUG" if scenario.log_level_chessinsight == "DEBUG" else "INFO"

            print(f"[benchmark] scenario={scenario.name}: recreate backend", flush=True)
            compose_up_resilient(
                scenario_env,
                compose_profiles,
                "--no-deps",
                "--force-recreate",
                "backend",
            )

            wait_backend_ready(args.backend_base_url, timeout_sec=180)

            if warmup_duration_sec > 0:
                print(
                    f"[benchmark] scenario={scenario.name}: warmup (rps={warmup_rps}, duration={args.warmup_duration})",
                    flush=True,
                )
                run_command_stream(
                    [
                        "python3",
                        "ci/perf/run_k6.py",
                        "--mode",
                        "fixed",
                        "--rps",
                        str(warmup_rps),
                        "--duration",
                        args.warmup_duration,
                        "--phase",
                        f"{slug}-warmup",
                        "--base-url",
                        "http://backend:8080",
                    ],
                    env=scenario_env,
                )

            reset_trace_export_file(TRACE_EXPORT_FILE)

            print(f"[benchmark] scenario={scenario.name}: run load (rps={args.rps}, duration={args.duration})", flush=True)
            start_time = time.time()
            run_command_stream(
                [
                    "python3",
                    "ci/perf/run_k6.py",
                    "--mode",
                    "fixed",
                    "--rps",
                    str(args.rps),
                    "--duration",
                    args.duration,
                    "--phase",
                    slug,
                    "--base-url",
                    "http://backend:8080",
                ],
                env=scenario_env,
            )
            end_time = time.time()

            if args.sleep_after_scenario > 0:
                time.sleep(args.sleep_after_scenario)

            query_time = end_time + 2.0
            measurement_window_sec = max(1, int(round(end_time - start_time)))
            # Anchor PromQL range vectors at measured load end to exclude warmup/cooldown time.
            range_selector = f"[{measurement_window_sec}s] @ {end_time:.3f}"
            resource_metrics_source = detect_resource_metrics_source(args.prometheus_url, query_time)

            if resource_metrics_source == "cadvisor":
                cpu_seconds = prom_query_value(
                    args.prometheus_url,
                    f"sum(increase(container_cpu_usage_seconds_total{{container_label_com_docker_compose_service=\"backend\"}}{range_selector}))",
                    query_time,
                )
                avg_memory_bytes = prom_query_value(
                    args.prometheus_url,
                    f"sum(avg_over_time(container_memory_working_set_bytes{{container_label_com_docker_compose_service=\"backend\"}}{range_selector}))",
                    query_time,
                )
                max_memory_bytes = prom_query_value(
                    args.prometheus_url,
                    f"sum(max_over_time(container_memory_working_set_bytes{{container_label_com_docker_compose_service=\"backend\"}}{range_selector}))",
                    query_time,
                )
            else:
                cpu_seconds, _ = prom_query_first_positive(
                    args.prometheus_url,
                    [
                        f"sum(avg_over_time(process_cpu_usage{{job=\"backend\"}}{range_selector})) * {measurement_window_sec}",
                        f"sum(rate(process_cpu_usage{{job=\"backend\"}}{range_selector})) * {measurement_window_sec}",
                    ],
                    query_time,
                )
                avg_memory_bytes, _ = prom_query_first_positive(
                    args.prometheus_url,
                    [
                        f"sum(avg_over_time(process_resident_memory_bytes{{job=\"backend\"}}{range_selector}))",
                        f"sum(avg_over_time(jvm_memory_used_bytes{{job=\"backend\"}}{range_selector}))",
                    ],
                    query_time,
                )
                max_memory_bytes, _ = prom_query_first_positive(
                    args.prometheus_url,
                    [
                        f"sum(max_over_time(process_resident_memory_bytes{{job=\"backend\"}}{range_selector}))",
                        f"sum(max_over_time(jvm_memory_used_bytes{{job=\"backend\"}}{range_selector}))",
                    ],
                    query_time,
                )
            requests = prom_query_value(
                args.prometheus_url,
                f"sum(increase(http_server_requests_seconds_count{{job=\"backend\",uri=\"/v1/move-evaluations\",method=\"POST\"}}{range_selector}))",
                query_time,
            )
            errors = prom_query_value(
                args.prometheus_url,
                f"sum(increase(http_server_requests_seconds_count{{job=\"backend\",uri=\"/v1/move-evaluations\",method=\"POST\",status!~\"2..\"}}{range_selector}))",
                query_time,
            )
            p95_ms = prom_query_value(
                args.prometheus_url,
                f"histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{job=\"backend\",uri=\"/v1/move-evaluations\",method=\"POST\"}}{range_selector}))) * 1000",
                query_time,
            )
            status_counts = prom_query_vector(
                args.prometheus_url,
                f"sum by (status) (increase(http_server_requests_seconds_count{{job=\"backend\",uri=\"/v1/move-evaluations\",method=\"POST\"}}{range_selector}))",
                "status",
                query_time,
            )
            backend_log_lines = count_backend_log_lines()
            exported_spans = count_exported_spans(TRACE_EXPORT_FILE)
            backend_logs = get_container_logs("backend")
            otel_logs = get_container_logs("otel-collector")
            jaeger_logs = get_container_logs("jaeger")

            trace_copy_path = output_dir / f"traces-{slug}.jsonl"
            if TRACE_EXPORT_FILE.exists():
                shutil.copyfile(TRACE_EXPORT_FILE, trace_copy_path)
            (output_dir / f"backend-{slug}.log").write_text(backend_logs, encoding="utf-8")
            (output_dir / f"otel-collector-{slug}.log").write_text(otel_logs, encoding="utf-8")
            (output_dir / f"jaeger-{slug}.log").write_text(jaeger_logs, encoding="utf-8")

            error_rate_percent = 0.0 if requests <= 0.0 else (errors / requests) * 100.0
            cpu_seconds_per_100_req = 0.0 if requests <= 0.0 else (cpu_seconds / requests) * 100.0
            status_4xx = sum(value for code, value in status_counts.items() if code.startswith("4"))
            status_5xx = sum(value for code, value in status_counts.items() if code.startswith("5"))

            row = {
                "scenario": scenario.name,
                "tracing_enabled": scenario.tracing_enabled,
                "log_level_chessinsight": scenario.log_level_chessinsight,
                "started_at_utc": datetime.fromtimestamp(start_time, tz=timezone.utc).isoformat(),
                "finished_at_utc": datetime.fromtimestamp(end_time, tz=timezone.utc).isoformat(),
                "measurement_window_sec": measurement_window_sec,
                "resource_metrics_source": resource_metrics_source,
                "cpu_seconds": cpu_seconds,
                "avg_memory_mib": bytes_to_mib(avg_memory_bytes),
                "max_memory_mib": bytes_to_mib(max_memory_bytes),
                "requests": requests,
                "errors": errors,
                "error_rate_percent": error_rate_percent,
                "p95_ms": p95_ms,
                "status_counts": status_counts,
                "status_counts_json": json.dumps(status_counts, ensure_ascii=False, sort_keys=True),
                "status_4xx": status_4xx,
                "status_5xx": status_5xx,
                "backend_log_lines": backend_log_lines,
                "exported_spans": exported_spans,
                "cpu_seconds_per_100_req": cpu_seconds_per_100_req,
            }
            rows.append(row)

            if scenario.tracing_enabled and exported_spans <= 0:
                print(
                    "[benchmark] warning: tracing is enabled but exported_spans=0. "
                    "For local runs, ensure backend image is rebuilt "
                    "(`docker compose build backend`) and inspect "
                    f"`backend-{slug}.log` / `otel-collector-{slug}.log`.",
                    flush=True,
                )
            if (not scenario.tracing_enabled) and exported_spans > 0:
                print(
                    "[benchmark] warning: tracing is disabled but spans were exported. "
                    "Check TRACING_ENABLED and management.tracing configuration.",
                    flush=True,
                )

            print(
                f"[benchmark] scenario={scenario.name}: cpu={row['cpu_seconds']:.3f}s "
                f"avg_ram={row['avg_memory_mib']:.2f}MiB spans={row['exported_spans']}",
                flush=True,
            )

        metadata: dict[str, Any] = {
            "generated_at_utc": datetime.now(tz=timezone.utc).isoformat(),
            "rps": args.rps,
            "duration": args.duration,
            "warmup_duration": args.warmup_duration,
            "warmup_rps": warmup_rps,
            "sleep_after_scenario": args.sleep_after_scenario,
            "prometheus_url": args.prometheus_url,
            "backend_base_url": args.backend_base_url,
            "grafana_base_url": args.grafana_base_url,
            "jaeger_base_url": args.jaeger_base_url,
            "ui_mode": effective_ui_mode,
            "ui_urls": {
                "grafana": grafana_ui_url,
                "jaeger": jaeger_ui_url,
            },
            "runtime_profile": runtime_profile,
            "scenarios": [scenario.__dict__ for scenario in SCENARIOS],
        }

        json_path = output_dir / "results.json"
        write_json(json_path, {"metadata": metadata, "results": rows})

        csv_path = output_dir / "results.csv"
        with csv_path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(
                f,
                extrasaction="ignore",
                fieldnames=[
                    "scenario",
                    "tracing_enabled",
                    "log_level_chessinsight",
                    "started_at_utc",
                    "finished_at_utc",
                    "measurement_window_sec",
                    "resource_metrics_source",
                    "cpu_seconds",
                    "avg_memory_mib",
                    "max_memory_mib",
                    "requests",
                    "errors",
                    "error_rate_percent",
                    "p95_ms",
                    "status_counts_json",
                    "status_4xx",
                    "status_5xx",
                    "backend_log_lines",
                    "exported_spans",
                    "cpu_seconds_per_100_req",
                ],
            )
            writer.writeheader()
            writer.writerows(rows)

        report_path = output_dir / "summary.md"
        write_markdown_report(report_path, metadata, rows)

        print(f"[benchmark] report: {report_path}", flush=True)
        return 0
    finally:
        if started_compose and not args.keep_up:
            print("[benchmark] stopping compose stack", flush=True)
            run_command(compose_cmd("down", "-v", profiles=compose_profiles), env=base_env, check=False)


if __name__ == "__main__":
    raise SystemExit(main())

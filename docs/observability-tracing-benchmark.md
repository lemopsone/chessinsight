# Трассировка, мониторинг и сравнение потребления ресурсов

Документ описывает практический контур для требований по трассировке/мониторингу и сравнению ресурсов:

- трассировка `OpenTelemetry` (через Spring Boot Micrometer tracing + OTLP);
- мониторинг `Prometheus + cAdvisor`;
- визуализация трасс в `Jaeger`;
- сравнение CPU/RAM в 4 режимах;
- сохранение отчета и raw-данных в артефакты CI.

## Что интегрировано

1. Backend поддерживает переключаемую трассировку:
- `TRACING_ENABLED=true|false`
- `TRACING_SAMPLING_PROBABILITY` (по умолчанию `1.0`)
- `OTEL_EXPORTER_OTLP_ENDPOINT` (по умолчанию `http://otel-collector:4318/v1/traces`)

2. В `docker-compose` добавлены:
- `otel-collector` (прием OTLP и fan-out экспорт);
- `jaeger` (UI и backend для просмотра трасс, `http://localhost:16686`).

3. `otel-collector` экспортирует трассы одновременно:
- `reports/perf/otel/traces.jsonl`
- в `jaeger:4317` (OTLP gRPC).

4. Для сравнения ресурсов используется автоматический benchmark:
- `ci/run-analysis-perf.sh`
- под капотом: `ci/perf/run_observability_benchmark.py`

5. Контур метрик кросс-платформенный:
- Linux: профиль `linux` (обычный `cadvisor` + `node-exporter`);
- macOS (Docker Desktop): профиль `macos` (`cadvisor-desktop`).

Benchmark автоматически выбирает профиль по ОС. Если container-метрики недоступны,
используется fallback на process/JVM метрики backend.

## Сценарии сравнения

Benchmark прогоняет 4 конфигурации на одинаковой нагрузке (`POST /api/v1/move-evaluations`):

1. `baseline`: tracing OFF, logging default (`INFO`)
2. `tracing_enabled`: tracing ON, logging default (`INFO`)
3. `extended_logging`: tracing OFF, `ru.chessinsight=DEBUG`
4. `tracing_and_extended_logging`: tracing ON, `ru.chessinsight=DEBUG`

Перед каждым измеряемым сценарием выполняется отдельный warmup-прогон (по умолчанию `10s`),
чтобы снизить влияние холодного старта/JIT и прогрева соединений на результаты.

Это позволяет отдельно оценить:
- вклад трассировки: `tracing_enabled - baseline`
- вклад расширенного логирования: `extended_logging - baseline`

В режимах `extended_logging*` дополнительно включается DEBUG для:
- `ru.chessinsight`
- `org.springframework.web`
- `org.springframework.security`
- `org.springframework.web.filter`
- `io.micrometer.tracing`
- `io.opentelemetry.exporter`

Ожидания по span:
- для `baseline` и `extended_logging` (`tracing OFF`) `traces-*.jsonl` должен быть пустым;
- для `tracing_*` сценариев `traces-*.jsonl` должен содержать span.

Если в `tracing_*` сценариях span по-прежнему 0, сначала пересоберите backend-образ:

```bash
docker compose build backend
```

Просмотр трасс в Jaeger:

```bash
docker compose --profile mock --profile linux up -d jaeger otel-collector backend
```

Откройте `http://localhost/jaeger/` (через gateway) или `http://localhost:16686/jaeger/` (напрямую), выберите сервис `backend` и нажмите `Find Traces`.

Для `POST /api/v1/move-evaluations` в trace теперь есть дополнительные business-span:
- `move.analysis.admission.wait` — ожидание слота в admission limiter;
- `move.analysis.http.handler` — обработка запроса в контроллере;
- `move.analysis.mapping.request` — маппинг API DTO -> application DTO;
- `move.analysis.workflow` — основной pipeline анализа хода;
- `move.analysis.engine.call` — вызов `ChessEngine.analyzeMove`;
- `move.analysis.engine.request.prepare` — подготовка данных для вызова движка;
- `move.analysis.engine.execute` — выполнение вызова движка;
- `move.analysis.engine.response.process` — обработка результата движка;
- `move.analysis.mapping.response` — маппинг application DTO -> API DTO.

## Какие ресурсы измеряются

Из Prometheus собираются:
- CPU (секунды): `increase(container_cpu_usage_seconds_total...)`
- RAM (средняя и пик): `avg_over_time/max_over_time(container_memory_working_set_bytes...)`

Дополнительно фиксируются:
- число обработанных запросов и ошибок;
- p95 latency;
- число строк логов backend-контейнера;
- число экспортированных span (по `traces.jsonl`).

Окно Prometheus-расчетов привязано строго к интервалу измеряемой фазы нагрузки
(`start_time..end_time`), поэтому warmup и post-scenario sleep не включаются в CPU/RAM/latency.

## Локальный запуск

```bash
./ci/run-analysis-perf.sh
```

Чтобы после прогона оставить стек поднятым для просмотра UI:

```bash
KEEP_UP=true ./ci/run-analysis-perf.sh
```

Режим UI настраивается через `UI_MODE`:
- `UI_MODE=auto` (по умолчанию): локально поднимается UI через `gateway`, в GitHub Actions UI отключается;
- `UI_MODE=gateway`: UI через `http://localhost/monitoring/` и `http://localhost/jaeger/`;
- `UI_MODE=direct`: UI напрямую по портам (`http://localhost:3000`, `http://localhost:16686/jaeger/`);
- `UI_MODE=off`: UI сервисы не поднимаются (минимальный benchmark-контур).

Параметры можно задавать либо аргументами, либо env:

```bash
RPS=10 DURATION=60s WARMUP_DURATION=12s WARMUP_RPS=10 ./ci/run-analysis-perf.sh
```

Пример с аргументами:

```bash
./ci/run-analysis-perf.sh --output-dir reports/perf/observability --rps 8 --duration 45s --warmup-duration 10s --warmup-rps 8 --sleep-after-scenario 6
```

## Выходные файлы

По умолчанию в `reports/perf/observability`:
- `summary.md` — сравнительный отчет;
- `results.json` — raw-метрики и метаданные;
- `results.csv` — табличное представление;
- `traces-*.jsonl` — выгрузка span для каждого сценария.
- `backend-*.log` — логи backend по каждому сценарию.
- `otel-collector-*.log` — логи OTel collector по каждому сценарию.
- `jaeger-*.log` — логи Jaeger по каждому сценарию.

## Просмотр UI после benchmark

При `KEEP_UP=true` доступны:
- в `UI_MODE=gateway` (или `auto` локально):
  - Grafana: `http://localhost/monitoring/`
  - Jaeger: `http://localhost/jaeger/`
- в `UI_MODE=direct`:
  - Grafana: `http://localhost:3000`
  - Jaeger: `http://localhost:16686/jaeger/`

В GitHub Actions (`GITHUB_ACTIONS=true`) режим `auto` автоматически переключается в `off`, чтобы UI-сервисы не запускались.

## CI/CD

В GitHub Actions добавлена job `Observability Benchmark` в `.github/workflows/tests.yml`,
которая запускает benchmark и публикует артефакт `ci-reports-observability`.

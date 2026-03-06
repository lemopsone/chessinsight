#!/usr/bin/env python3
"""Compute Halstead metrics for Java methods."""

from __future__ import annotations

import argparse
import math
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List


JAVA_KEYWORDS = {
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while",
    "true",
    "false",
    "null",
    "record",
    "yield",
}

OPERATORS = [
    ">>>=",
    "<<=",
    ">>=",
    "->",
    "::",
    "++",
    "--",
    "==",
    "!=",
    ">=",
    "<=",
    "&&",
    "||",
    "+=",
    "-=",
    "*=",
    "/=",
    "%=",
    "&=",
    "|=",
    "^=",
    "<<",
    ">>",
    ">>>",
    "+",
    "-",
    "*",
    "/",
    "%",
    "=",
    "&",
    "|",
    "^",
    "~",
    "!",
    "<",
    ">",
    "?",
    ":",
    ".",
    ",",
    ";",
    "(",
    ")",
    "[",
    "]",
    "{",
    "}",
]

METHOD_SIGNATURE = re.compile(
    r"\b(?:public|protected|private)\b[^{;=]*\([^;{}]*\)\s*(?:throws\s+[^{]+)?\{",
    flags=re.MULTILINE,
)
METHOD_NAME = re.compile(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*\([^()]*\)\s*(?:throws\s+[^{]+)?\{$")
IDENTIFIER = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")
NUMBER = re.compile(r"(?:0[xX][0-9A-Fa-f]+|\d+(?:\.\d+)?)")
OPERATOR = re.compile("|".join(re.escape(op) for op in sorted(OPERATORS, key=len, reverse=True)))


@dataclass(frozen=True)
class MethodMetric:
    path: Path
    line: int
    name: str
    n1: int
    n2: int
    N1: int
    N2: int
    vocabulary: int
    length: int
    volume: float


def sanitize_java(source: str) -> str:
    chars = list(source)
    i = 0
    n = len(chars)
    NORMAL, LINE_COMMENT, BLOCK_COMMENT, STRING, CHAR = 0, 1, 2, 3, 4
    state = NORMAL

    while i < n:
        c = chars[i]
        nxt = chars[i + 1] if i + 1 < n else ""

        if state == NORMAL:
            if c == "/" and nxt == "/":
                chars[i] = " "
                chars[i + 1] = " "
                i += 2
                state = LINE_COMMENT
                continue
            if c == "/" and nxt == "*":
                chars[i] = " "
                chars[i + 1] = " "
                i += 2
                state = BLOCK_COMMENT
                continue
            if c == '"':
                chars[i] = " "
                i += 1
                state = STRING
                continue
            if c == "'":
                chars[i] = " "
                i += 1
                state = CHAR
                continue
            i += 1
            continue

        if state == LINE_COMMENT:
            if c == "\n":
                state = NORMAL
            else:
                chars[i] = " "
            i += 1
            continue

        if state == BLOCK_COMMENT:
            if c == "*" and nxt == "/":
                chars[i] = " "
                chars[i + 1] = " "
                i += 2
                state = NORMAL
            else:
                if c != "\n":
                    chars[i] = " "
                i += 1
            continue

        if state == STRING:
            if c == "\\" and i + 1 < n:
                chars[i] = " "
                chars[i + 1] = " "
                i += 2
                continue
            if c == '"':
                chars[i] = " "
                i += 1
                state = NORMAL
            else:
                if c != "\n":
                    chars[i] = " "
                i += 1
            continue

        if state == CHAR:
            if c == "\\" and i + 1 < n:
                chars[i] = " "
                chars[i + 1] = " "
                i += 2
                continue
            if c == "'":
                chars[i] = " "
                i += 1
                state = NORMAL
            else:
                if c != "\n":
                    chars[i] = " "
                i += 1
            continue

    return "".join(chars)


def find_matching_brace(sanitized: str, start: int) -> int:
    depth = 0
    for idx in range(start, len(sanitized)):
        ch = sanitized[idx]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return idx
    return -1


def method_name_from_header(header: str) -> str:
    compact = " ".join(header.replace("\n", " ").split())
    match = METHOD_NAME.search(compact)
    return match.group(1) if match else "<anonymous>"


def tokenize_for_halstead(body: str) -> tuple[List[str], List[str]]:
    operators: List[str] = []
    operands: List[str] = []
    i = 0
    n = len(body)

    while i < n:
        ch = body[i]
        if ch.isspace():
            i += 1
            continue

        op_match = OPERATOR.match(body, i)
        if op_match:
            operators.append(op_match.group(0))
            i = op_match.end()
            continue

        ident_match = IDENTIFIER.match(body, i)
        if ident_match:
            token = ident_match.group(0)
            if token in JAVA_KEYWORDS:
                operators.append(token)
            else:
                operands.append(token)
            i = ident_match.end()
            continue

        number_match = NUMBER.match(body, i)
        if number_match:
            operands.append(number_match.group(0))
            i = number_match.end()
            continue

        i += 1

    return operators, operands


def analyze_file(path: Path) -> Iterable[MethodMetric]:
    source = path.read_text(encoding="utf-8", errors="ignore")
    sanitized = sanitize_java(source)

    for match in METHOD_SIGNATURE.finditer(sanitized):
        body_start = match.end() - 1
        body_end = find_matching_brace(sanitized, body_start)
        if body_end < 0:
            continue

        header = source[match.start() : body_start + 1]
        body = sanitized[body_start + 1 : body_end]
        line = source.count("\n", 0, match.start()) + 1
        name = method_name_from_header(header)

        operators, operands = tokenize_for_halstead(body)
        n1 = len(set(operators))
        n2 = len(set(operands))
        N1 = len(operators)
        N2 = len(operands)
        vocabulary = n1 + n2
        length = N1 + N2
        volume = float(length) * math.log2(vocabulary) if vocabulary > 0 and length > 0 else 0.0

        yield MethodMetric(
            path=path,
            line=line,
            name=name,
            n1=n1,
            n2=n2,
            N1=N1,
            N2=N2,
            vocabulary=vocabulary,
            length=length,
            volume=volume,
        )


def java_sources(root: Path) -> List[Path]:
    modules = ("core", "jpa", "web", "cli", "engine", "test-fixtures")
    files: List[Path] = []
    for module in modules:
        base = root / module / "src" / "main" / "java"
        if not base.exists():
            continue
        files.extend(sorted(base.rglob("*.java")))
    return [f for f in files if "/target/" not in f.as_posix()]


def write_report(report_path: Path, all_metrics: List[MethodMetric], top_n: int, root: Path) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    generated_at = datetime.now(timezone.utc).isoformat()
    sorted_metrics = sorted(all_metrics, key=lambda m: m.volume, reverse=True)
    if top_n > 0:
        ranked = sorted_metrics[:top_n]
    else:
        ranked = sorted_metrics

    max_volume = ranked[0].volume if ranked else 0.0
    avg_volume = (sum(m.volume for m in all_metrics) / len(all_metrics)) if all_metrics else 0.0
    report_scope = (
        f"Top {len(ranked)} Methods by Halstead Volume"
        if top_n > 0
        else "All Methods by Halstead Volume"
    )

    lines = [
        "# Halstead Complexity Report",
        "",
        f"- Generated (UTC): `{generated_at}`",
        f"- Methods analyzed: `{len(all_metrics)}`",
        f"- Average volume: `{avg_volume:.2f}`",
        f"- Max volume: `{max_volume:.2f}`",
        "",
        f"## {report_scope}",
        "",
        "| Volume | File | Line | Method | n1 | n2 | N1 | N2 |",
        "|---:|---|---:|---|---:|---:|---:|---:|",
    ]

    for metric in ranked:
        try:
            path_text = metric.path.relative_to(root).as_posix()
        except ValueError:
            path_text = metric.path.as_posix()
        lines.append(
            f"| {metric.volume:.2f} | {path_text} | {metric.line} | `{metric.name}` | "
            f"{metric.n1} | {metric.n2} | {metric.N1} | {metric.N2} |"
        )

    report_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Halstead metrics for Java methods")
    parser.add_argument("--root", default=".", help="Project root")
    parser.add_argument(
        "--report",
        default="reports/static-analysis/halstead-report.md",
        help="Report output path",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=0,
        help="Top N methods in the report (0 = all methods)",
    )
    parser.add_argument(
        "--max-volume",
        type=float,
        default=None,
        help="Optional fail threshold for maximum Halstead volume",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = Path(args.root).resolve()
    report_path = (root / args.report).resolve()

    metrics: List[MethodMetric] = []
    for source_file in java_sources(root):
        metrics.extend(analyze_file(source_file))

    write_report(report_path, metrics, args.top, root)

    print(f"Halstead report saved to: {report_path}")
    print(f"Methods analyzed: {len(metrics)}")
    if metrics:
        max_metric = max(metrics, key=lambda m: m.volume)
        print(
            f"Max Halstead volume: {max_metric.volume:.2f} "
            f"({max_metric.path}:{max_metric.line} {max_metric.name})"
        )

    if args.max_volume is not None and metrics:
        if max(m.volume for m in metrics) > args.max_volume:
            print(f"Max Halstead volume exceeds threshold: {args.max_volume}", flush=True)
            return 2

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

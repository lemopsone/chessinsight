#!/usr/bin/env bash

set -euo pipefail

ROOT_INPUT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
ROOT_DIR="$(cd "$ROOT_INPUT" && pwd)"
REPORT_REL_PATH="${2:-reports/static-analysis/cyclomatic-report.md}"
REPORT_PATH="${ROOT_DIR}/${REPORT_REL_PATH}"

MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"
MAVEN_ARGS=(
  --batch-mode
  --no-transfer-progress
  -DskipTests=true
  -DskipUnitTests=true
  -DskipITs=true
  -DskipE2E=true
  "-Dpmd.ruleset.path=${ROOT_DIR}/config/pmd/ruleset-cyclomatic-all.xml"
)

if [ -n "$MAVEN_REPO_LOCAL" ]; then
  mkdir -p "$MAVEN_REPO_LOCAL"
  MAVEN_ARGS+=("-Dmaven.repo.local=${MAVEN_REPO_LOCAL}")
fi

echo "[cyclomatic] PMD report for all methods"
mvn "${MAVEN_ARGS[@]}" -pl core,engine,cli,jpa,web -am compile pmd:pmd

tmp_rows="$(mktemp)"
trap 'rm -f "$tmp_rows"' EXIT

find "${ROOT_DIR}" -path "*/target/pmd.xml" -type f | while read -r pmd_file; do
  awk '
    function attr(line, key,    start, rest, value) {
      start = index(line, key "=\"")
      if (start == 0) {
        return ""
      }
      rest = substr(line, start + length(key) + 2)
      value = rest
      sub(/".*$/, "", value)
      return value
    }

    /<file name="/ {
      current_file = attr($0, "name")
      next
    }

    /<violation / {
      violation_line = $0
      method_name = attr(violation_line, "method")
      if (method_name == "") {
        next
      }

      class_name = attr(violation_line, "class")
      begin_line = attr(violation_line, "beginline")

      if (getline message_line <= 0) {
        next
      }

      complexity = ""
      if (match(message_line, /cyclomatic complexity of [0-9]+/)) {
        complexity = substr(message_line, RSTART, RLENGTH)
        gsub(/[^0-9]/, "", complexity)
      }

      if (complexity != "") {
        printf "%s\t%s\t%s\t%s\t%s\n", complexity, current_file, begin_line, class_name, method_name
      }
    }
  ' "$pmd_file" >> "$tmp_rows"
done

if [ ! -s "$tmp_rows" ]; then
  echo "[cyclomatic] No method-level cyclomatic entries found in PMD reports"
  exit 1
fi

sorted_rows="$(mktemp)"
trap 'rm -f "$tmp_rows" "$sorted_rows"' EXIT
sort -t$'\t' -k1,1nr -k2,2 -k3,3n "$tmp_rows" > "$sorted_rows"

count="$(wc -l < "$sorted_rows" | tr -d ' ')"
stats="$(awk -F'\t' '
  BEGIN {max = 0; sum = 0; n = 0}
  {
    n += 1
    sum += $1
    if ($1 > max) {
      max = $1
    }
  }
  END {
    if (n == 0) {
      printf "0\t0.00"
    } else {
      printf "%d\t%.2f", max, (sum / n)
    }
  }
' "$sorted_rows")"
max_complexity="${stats%%$'\t'*}"
avg_complexity="${stats#*$'\t'}"
generated_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

mkdir -p "$(dirname "$REPORT_PATH")"
{
  echo "# Cyclomatic Complexity Report"
  echo
  echo "- Generated (UTC): \`${generated_at}\`"
  echo "- Methods analyzed: \`${count}\`"
  echo "- Average complexity: \`${avg_complexity}\`"
  echo "- Max complexity: \`${max_complexity}\`"
  echo
  echo "## All Methods by Cyclomatic Complexity"
  echo
  echo "| Complexity | File | Line | Class | Method |"
  echo "|---:|---|---:|---|---|"

  while IFS=$'\t' read -r complexity file_path line_no class_name method_name; do
    relative_path="${file_path#${ROOT_DIR}/}"
    echo "| ${complexity} | ${relative_path} | ${line_no} | \`${class_name}\` | \`${method_name}\` |"
  done < "$sorted_rows"
} > "$REPORT_PATH"

echo "[cyclomatic] Report saved to: ${REPORT_PATH}"

#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"

MAVEN_ARGS=(
  --batch-mode
  --no-transfer-progress
  -DskipTests=true
  -DskipUnitTests=true
  -DskipITs=true
  -DskipE2E=true
)

if [ -n "$MAVEN_REPO_LOCAL" ]; then
  mkdir -p "$MAVEN_REPO_LOCAL"
  MAVEN_ARGS+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
fi

echo "[quality] Compile + PMD + Checkstyle"
mvn "${MAVEN_ARGS[@]}" compile pmd:check checkstyle:check

echo "[quality] Cyclomatic metrics via PMD (all methods)"
./ci/metrics/cyclomatic_pmd_report.sh "$ROOT_DIR"

echo "[quality] Halstead metrics (all methods)"
python3 ci/metrics/halstead_java.py --root "$ROOT_DIR"

echo "[quality] OK"

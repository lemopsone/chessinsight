#!/usr/bin/env bash
set -euo pipefail

proj="$(basename "$(pwd)")"
ts="$(date +"%Y%m%d-%H%M%S")"
zipname="${proj}-src-${ts}.zip"

EXCLUDES=(
  ".git/*" ".svn/*" ".hg/*"
  "*/build/*" "build/*"
  "*/target/*" "target/*"
  "*/dist/*" "dist/*"
  "*/out/*" "out/*"
  "*/node_modules/*" "node_modules/*"
  ".gradle/*" ".idea/*" ".vscode/*" ".cache/*"
  ".venv/*" "venv/*" ".pytest_cache/*" "coverage/*"
  ".next/*" ".nuxt/*"
  "$zipname"
)

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  # tracked + untracked, но не игнорируемые (.gitignore уважается)
  git -c core.quotepath=false ls-files -c -o --exclude-standard \
    | zip -q -@ "$zipname"
else
  zip -qr "$zipname" . -x "${EXCLUDES[@]}"
fi

echo "Created: $zipname"


#!/bin/bash

set -euo pipefail

repo_root=$(cd "$(dirname "$0")" && pwd)
cd "$repo_root"

flavor="${1:-googlePlay}"
flavor_capitalized="$(printf '%s' "$flavor" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
task=":app:analyze${flavor_capitalized}ReleaseR8Config"
report_dir="${R8_ANALYZER_OUT:-$repo_root/app/build/reports/r8}"

echo "Running $task"
./gradlew "$task"

echo "R8 Configuration Analyzer reports:"
if compgen -G "$report_dir"/*.html > /dev/null; then
  ls -1 "$report_dir"/*.html
else
  echo "No HTML report found in $report_dir"
  exit 1
fi

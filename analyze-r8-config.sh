#!/bin/bash

set -euo pipefail

repo_root=$(cd "$(dirname "$0")" && pwd)
cd "$repo_root"

flavor="${1:-googlePlay}"
flavor_capitalized="$(printf '%s' "$flavor" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"
task=":app:analyze${flavor_capitalized}ReleaseR8Config"
report_prefix="$repo_root/app/build/reports/r8/r8-config-analyzer-${flavor}Release"
reports=("$report_prefix.html" "$report_prefix.pb")

printf 'Running %s\n' "$task"
./gradlew "$task"

printf 'R8 Configuration Analyzer reports:\n'
for report in "${reports[@]}"; do
  if [[ ! -s "$report" ]]; then
    printf 'Missing or empty report: %s\n' "$report" >&2
    exit 1
  fi
  printf '%s\n' "$report"
done

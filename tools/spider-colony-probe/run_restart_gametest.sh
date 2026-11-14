#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
mkdir -p build
out=$(mktemp -d "$root/build/spider-restart-XXXXXXXX")
echo "evidence: $out"
fixture=$(basename "$out")
for phase in write reload; do
  extra=()
  if [ "$phase" = reload ]; then extra=(-x deleteGameTestRunDir); fi
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dfabric-api.gametest.filter=moreminecrafts-gametest:spider_colony_game_tests_state_survives_actual_server_restart -Dmm.spider.restart.phase=$phase -Dmm.spider.restart.fixture=$fixture" \
    ./gradlew --no-daemon --max-workers=2 "${extra[@]}" runGameTest > "$out/$phase.log" 2>&1
  marker="SPIDER_RESTART_$(echo "$phase" | tr '[:lower:]' '[:upper:]')_OK"
  grep -F "$marker" "$out/$phase.log"
done

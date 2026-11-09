#!/usr/bin/env bash
# ST-01 P10: real-process restart check on the real royal-tomb fixture (3 separate server processes per mode).
# Usage: tools/st01-p10/run_restart.sh <log_dir>   (JAVA_HOME must point to JDK 25; run from the repository root)
set -uo pipefail
LOG="${1:?log dir}"; mkdir -p "$LOG"
BASE_OPTS="${JAVA_TOOL_OPTIONS:-}"
rc=0
for mode in guiding open; do
  for phase in write reload verify; do
    JAVA_TOOL_OPTIONS="$BASE_OPTS -Dfabric-api.gametest.filter=moreminecrafts-gametest:royal_tomb_safety_game_tests_entrance_state_survives_process_restart_on_the_real_hall -Dmoreminecrafts.st01.p10.restart.phase=$phase -Dmoreminecrafts.st01.p10.restart.mode=$mode" \
      ./gradlew --offline runGameTest > "$LOG/restart-$mode-$phase.log" 2>&1 || rc=1
    grep -E "P10_RESTART_(WRITE|RELOAD|VERIFY)|All .* required tests passed|required tests failed" "$LOG/restart-$mode-$phase.log" | sed "s/^/[$mode $phase] /"
  done
done
exit $rc

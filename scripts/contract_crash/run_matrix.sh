#!/usr/bin/env bash
# Real process-interruption matrix for contract redemption.
# For each scenario: process A completes a redemption and terminates the JVM (Runtime.halt, no saves) at one
# transaction boundary; process B is a fresh JVM that reloads the same world and player and verifies recovery.
# Usage: [ONLY=<POINT>] scripts/contract_crash/run_matrix.sh [results-dir]
# PERSIST writes an accepted contract with progress and an unrolled placeholder, exits normally, and verifies both after
# a restart. Every other point terminates the JVM without saving at that boundary.
set -uo pipefail
RESULTS="${1:-contract-crash-results}"
mkdir -p "$RESULTS"
FILTER="moreminecrafts-gametest:contract_crash_game_tests_crash_restart_scenario"
SCENARIOS=(
  "BEFORE_PREPARE false"
  "AFTER_PREPARE false"
  "AFTER_CONSUME false"
  "AFTER_REWARD false"
  "AFTER_PLAYER_SAVE false"
  "AFTER_PLAYER_SAVE true"
  "AFTER_OVERFLOW_SPAWN true"
  "AFTER_OVERFLOW_FLUSH true"
  "AFTER_OVERFLOW_DROP true"
  "AFTER_LEDGER_COMMIT false"
  "AFTER_LEDGER_COMMIT true"
  "NONE true"
  "PERSIST false"
)
failed=0
for scenario in "${SCENARIOS[@]}"; do
  read -r point overflow <<<"$scenario"
  if [ -n "${ONLY:-}" ] && [ "$point" != "$ONLY" ]; then continue; fi
  name="${point}_overflow-${overflow}"
  rm -rf build/run/gameTest/world
  common="-Dfabric-api.gametest.filter=$FILTER -Dmoreminecrafts.contract.crash.point=$point -Dmoreminecrafts.contract.crash.overflow=$overflow"
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $common -Dmoreminecrafts.contract.crash.phase=write" \
    ./gradlew runGameTest --no-daemon --console=plain > "$RESULTS/$name.write.log" 2>&1
  write_exit=$?
  if [ "$point" = "PERSIST" ]; then
    write_ok=$([ $write_exit -eq 0 ] && grep -q "CONTRACT_PERSIST_WRITE" "$RESULTS/$name.write.log" && echo yes || echo no)
  elif [ "$point" = "NONE" ]; then
    write_ok=$([ $write_exit -eq 0 ] && grep -q "CONTRACT_CRASH_WRITE_COMPLETED" "$RESULTS/$name.write.log" && echo yes || echo no)
  else
    write_ok=$(grep -q "CONTRACT_CRASH_HALT point=$point" "$RESULTS/$name.write.log" && [ $write_exit -ne 0 ] && echo yes || echo no)
  fi
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $common -Dmoreminecrafts.contract.crash.phase=read" \
    ./gradlew runGameTest --no-daemon --console=plain > "$RESULTS/$name.read.log" 2>&1
  read_exit=$?
  read_ok=$([ $read_exit -eq 0 ] && grep -q "CONTRACT_CRASH_READ point=$point .* result=PASS" "$RESULTS/$name.read.log" && echo yes || echo no)
  line=$(grep -h "CONTRACT_CRASH_READ" "$RESULTS/$name.read.log" | head -1)
  echo "scenario=$name interrupted=$write_ok recovered=$read_ok :: ${line:-no read marker}" | tee -a "$RESULTS/summary.txt"
  if [ "$write_ok" != yes ] || [ "$read_ok" != yes ]; then failed=1; fi
done
exit $failed

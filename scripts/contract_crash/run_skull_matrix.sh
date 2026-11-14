#!/usr/bin/env bash
# 2-JVM forced-halt matrix runner for Wither Skeleton Skull P3 transactions.
# 13 boundary scenarios testing crash consistency (Runtime.halt, no saves).
set -uo pipefail
RESULTS="${1:-skull-crash-results}"
mkdir -p "$RESULTS"
FILTER="moreminecrafts-gametest:quest_wither_skull_crash_game_tests_crash_restart_scenario"
SCENARIOS=(
  "BEFORE_PREPARE false"
  "BEFORE_PREPARE true"
  "AFTER_PREPARE false"
  "AFTER_PREPARE true"
  "AFTER_ADD false"
  "AFTER_PLAYER_SAVE false"
  "AFTER_LEDGER_COMMIT false"
  "PLAYER_ONLY_SAVE false"
  "CHUNK_BEFORE_PLAYER false"
  "NONE false"
  "NONE true"
  "PERSIST false"
  "PERSIST true"
)
failed=0
for scenario in "${SCENARIOS[@]}"; do
  read -r point full_inv <<<"$scenario"
  if [ -n "${ONLY:-}" ] && [ "$point" != "$ONLY" ]; then continue; fi
  name="${point}_fullinv-${full_inv}"
  rm -rf build/run/gameTest/world quest-skull-crash-fixture.txt
  common="-Dfabric-api.gametest.filter=$FILTER -Dmoreminecrafts.questskull.crash.point=$point -Dmoreminecrafts.questskull.crash.full_inv=$full_inv"
  
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $common -Dmoreminecrafts.questskull.crash.phase=write" \
    ./gradlew runGameTest --no-daemon --console=plain > "$RESULTS/$name.write.log" 2>&1
  write_exit=$?

  if [ "$point" = "PERSIST" ]; then
    write_ok=$([ $write_exit -eq 0 ] && grep -q "SKULL_PERSIST_WRITE" "$RESULTS/$name.write.log" && echo yes || echo no)
  elif [ "$point" = "NONE" ]; then
    write_ok=$([ $write_exit -eq 0 ] && grep -q "SKULL_CRASH_WRITE_COMPLETED" "$RESULTS/$name.write.log" && echo yes || echo no)
  else
    write_ok=$(grep -q "SKULL_CRASH_HALT point=$point" "$RESULTS/$name.write.log" && [ $write_exit -ne 0 ] && echo yes || echo no)
  fi

  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $common -Dmoreminecrafts.questskull.crash.phase=read" \
    ./gradlew runGameTest --no-daemon --console=plain > "$RESULTS/$name.read.log" 2>&1
  read_exit=$?

  read_ok=$([ $read_exit -eq 0 ] && grep -q "SKULL_CRASH_READ point=$point .* result=PASS" "$RESULTS/$name.read.log" && echo yes || echo no)
  line=$(grep -h "SKULL_CRASH_READ" "$RESULTS/$name.read.log" | head -1)
  echo "scenario=$name interrupted=$write_ok recovered=$read_ok :: ${line:-no read marker}" | tee -a "$RESULTS/summary.txt"
  if [ "$write_ok" != yes ] || [ "$read_ok" != yes ]; then failed=1; fi
done
exit $failed

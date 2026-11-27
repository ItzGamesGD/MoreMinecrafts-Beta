#!/usr/bin/env bash
# ST-05 Stage 0 (A02/A03) feasibility probe driver for ONE seed: <seed> [final-radius-chunks] [plan-radius-chunks]
# Runs modes plan, fwd, rev, shuf, par (fresh worlds) and reload (restart on the saved fwd world).
# Output: build/st05-worldgen-probe/results/<seed>/<mode>.json   (requires JAVA_HOME = JDK 25)
set -euo pipefail
seed=$1; fr=${2:-4}; pr=${3:-12}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/st05-worldgen-probe/run"
res="$root/build/st05-worldgen-probe/results/$seed"
logs="$root/build/st05-worldgen-probe/logs"
mkdir -p "$res" "$logs" "$run"
echo "eula=true" > "$run/eula.txt"
write_props() {
cat > "$run/server.properties" <<PROPS
level-name=world
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
spawn-monsters=false
spawn-animals=false
max-tick-time=-1
enable-status=false
PROPS
}
run_mode() {
  local mode=$1
  write_props
  cd "$root"
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st05.out=$res/$mode.json -Dmm.st05.mode=$mode -Dmm.st05.finalradius=$fr -Dmm.st05.planradius=$pr" \
    timeout "${PROBE_TIMEOUT:-1800}" ./gradlew --no-daemon ${GRADLE_ARGS:-} runSt05CavernProbe -q > "$logs/$seed-$mode.log" 2>&1 || { echo "FAILED $seed $mode"; tail -30 "$logs/$seed-$mode.log"; exit 1; }
  test -s "$res/$mode.json"
}
for mode in ${MODES:-plan fwd rev shuf par}; do
  rm -rf "$run/world"
  run_mode $mode
  if [ "$mode" = fwd ]; then rm -rf "$run/world_fwd"; cp -r "$run/world" "$run/world_fwd"; fi
done
rm -rf "$run/world"; cp -r "$run/world_fwd" "$run/world"
run_mode reload
echo "seed $seed done"

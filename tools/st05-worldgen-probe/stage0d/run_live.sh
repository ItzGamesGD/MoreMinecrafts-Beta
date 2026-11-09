#!/usr/bin/env bash
# ST-05 Stage 0-D live validation: two SEPARATE Minecraft 26.3 dedicated-server processes on the same seed.
#   d-run1    : build plan-time snapshots in 3 column-generation orders, extract cores for each parameter set, resolve, persist state.
#   d-restart : new JVM; load persisted state, recompute under another order, reconcile, corruption cases.
# Usage: JAVA_HOME=<JDK25> [SEED=7001] tools/st05-worldgen-probe/stage0d/run_live.sh
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
seed=${SEED:-7001}
run="$root/build/st05-worldgen-probe/run"
res="$root/tools/st05-worldgen-probe/stage0d/live/seed-$seed"
state="$res/state"
mkdir -p "$run" "$res" "$state"
test ! -e "$state/state-A_10x10x6.txt" || { echo "state exists for seed $seed; use a clean checkout"; exit 1; }
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=stage0d-world-$seed
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
for mode in d-run1 d-restart; do
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx${HEAP:-8g} -Dmm.st05.mode=$mode -Dmm.st05.out=$res/$mode.json -Dmm.st05d.statedir=$state" \
    timeout "${PROBE_TIMEOUT:-3000}" ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/$mode.log" 2>&1 || { echo "FAILED $mode"; tail -30 "$res/$mode.log"; exit 1; }
  test -s "$res/$mode.json"
  python3 -c "import json,sys; d=json.load(open(sys.argv[1])); assert 'error' not in d, d; print(sys.argv[1],'ok')" "$res/$mode.json"
done

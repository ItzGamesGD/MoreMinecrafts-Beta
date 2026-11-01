#!/usr/bin/env bash
# Stage 0-E live run, two SEPARATE server processes on one seed/universe: run_e_live.sh <seed> <rx0> <rz0> <N>
#   e-run     : forward column order; pipeline; persist decisions.
#   e-restart : new JVM, reverse column order; pipeline; reload + reconcile against persisted decisions.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd); cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
seed=$1; rx0=$2; rz0=$3; n=$4
run="$root/build/st05-worldgen-probe/run"
res="$root/tools/st05-worldgen-probe/stage0e/live/seed-$seed-U${rx0}_${rz0}_$n"; state="$res/state"
mkdir -p "$run" "$res" "$state"
test -z "$(ls -A "$state")" || { echo "state exists: $state"; exit 1; }
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=stage0e-world-$seed
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
for mode in e-run e-restart; do
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx${HEAP:-10g} -Dmm.st05.mode=$mode -Dmm.st05.out=$res/$mode.json -Dmm.st05d.statedir=$state -Dmm.st05e.rx0=$rx0 -Dmm.st05e.rz0=$rz0 -Dmm.st05e.n=$n" \
    timeout "${PROBE_TIMEOUT:-4500}" ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/$mode.log" 2>&1 || { echo "FAILED $mode"; tail -30 "$res/$mode.log"; exit 1; }
  test -s "$res/$mode.json"
  python3 -c "import json,sys; d=json.load(open(sys.argv[1])); assert 'error' not in d, d; print(sys.argv[1],'ok')" "$res/$mode.json"
done

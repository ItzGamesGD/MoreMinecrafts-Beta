#!/usr/bin/env bash
# ST-05 Stage 0-F live construction run on a real Minecraft 26.3 dedicated server (Fabric dev server via Loom).
# Usage: JAVA_HOME=<JDK25> SEED=7001 MODE=f-run [ONLY=<id>] [CRASHID=<id> CRASHAFTER=<n>] [SETTLE=600] tools/st05-worldgen-probe/stage0f/run_f_live.sh
# The world directory (build/st05-worldgen-probe/run/stage0f-world-$SEED) is reused across modes of one seed so restarts see the persisted world.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
seed=${SEED:-7001}; mode=${MODE:-f-run}; tag=${TAG:-$mode}
run="$root/build/st05-worldgen-probe/run"
res="$root/tools/st05-worldgen-probe/stage0f/live/seed-$seed"
state="$res/state${WSUFFIX:-}"
mkdir -p "$run" "$res" "$state"
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=stage0f-world-$seed${WSUFFIX:-}
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
opts="-Xmx${HEAP:-10g} -Dmm.st05.mode=$mode -Dmm.st05.out=$res/$tag.json -Dmm.st05f.res=$root/tools/st05-worldgen-probe/stage0f/reservations-seed$seed.txt -Dmm.st05f.statedir=$state"
[ -n "${REVERSE:-}" ] && opts="$opts -Dmm.st05f.reverse=true"
[ -n "${ONLY:-}" ] && opts="$opts -Dmm.st05f.only=$ONLY"
[ -n "${CRASHID:-}" ] && opts="$opts -Dmm.st05f.crashid=$CRASHID -Dmm.st05f.crashafter=${CRASHAFTER:-0}"
[ -n "${RECOVERCRASH:-}" ] && opts="$opts -Dmm.st05f.recovercrash=$RECOVERCRASH"
[ -n "${SETTLE:-}" ] && opts="$opts -Dmm.st05f.settle=$SETTLE"
set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $opts" timeout "${PROBE_TIMEOUT:-5400}" ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/$tag.log" 2>&1
rc=$?
set -e
echo "gradle/server exit code: $rc (crash modes exit non-zero by design)" | tee -a "$res/$tag.log"
if [ "$mode" != "f-crash" ] && [ -z "${RECOVERCRASH:-}" ]; then
  test -s "$res/$tag.json" || { echo "NO RESULT JSON for $tag"; tail -40 "$res/$tag.log"; exit 1; }
  python3 -c "import json,sys; d=json.load(open(sys.argv[1])); assert 'error' not in d, d; print(sys.argv[1],'ok')" "$res/$tag.json"
fi

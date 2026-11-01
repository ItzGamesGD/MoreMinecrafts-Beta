#!/usr/bin/env bash
# Stage 0-E preflight v2 in a FRESH server process: run_e_preflight.sh <seed> <rx0> <rz0> <N>
# Loads/verifies the v1 persisted decisions, re-evaluates roll winners against final terrain with fluid-only failure, writes v2 state files.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd); cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
seed=$1; rx0=$2; rz0=$3; n=$4
run="$root/build/st05-worldgen-probe/run"
res="$root/tools/st05-worldgen-probe/stage0e/live/seed-$seed-U${rx0}_${rz0}_$n"
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
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx4g -Dmm.st05.mode=e-preflight -Dmm.st05.out=$res/e-preflight.json -Dmm.st05d.statedir=$res/state -Dmm.st05e.prev=$res/e-run.json -Dmm.st05e.rx0=$rx0 -Dmm.st05e.rz0=$rz0 -Dmm.st05e.n=$n" \
  timeout 1800 ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/e-preflight.log" 2>&1
test -s "$res/e-preflight.json"

#!/usr/bin/env bash
# Stage 0-E coarse suitability scan, one server process per seed: run_scan.sh <seed> [radius-blocks]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd); cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
seed=$1; radius=${2:-2048}
run="$root/build/st05-worldgen-probe/run"; res="$root/tools/st05-worldgen-probe/stage0e/scan"
mkdir -p "$run" "$res"
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=stage0e-scan-$seed
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Xmx${HEAP:-6g} -Dmm.st05.mode=e-scan -Dmm.st05.out=$res/scan-$seed.json -Dmm.st05e.radius=$radius" \
  timeout "${PROBE_TIMEOUT:-2400}" ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/scan-$seed.log" 2>&1
test -s "$res/scan-$seed.json"

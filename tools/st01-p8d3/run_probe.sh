#!/usr/bin/env bash
# ST-01 P8-D3 verification probe on a fresh Minecraft 26.3 dedicated server (validation only; not part of build/CI).
#   regression <seed-ignored>          : 20-seed decisions + footprint columns, P8-D3 terrain vs pre-P8-D3 terrain
#   bench <seed> <fast|legacy> <radius>: vanilla findNearestMapStructure + map creation on the server thread
# Output: build/st01-p8d3/results/<mode>-<seed>-<terrain>-r<radius>.json. Requires JAVA_HOME = JDK 25.
set -euo pipefail
mode=$1; seed=${2:-1004}; terrain=${3:-fast}; radius=${4:-20}
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d3/results"; logs="$root/build/st01-p8d3/logs"
fixed="$root/build/st01-p8c2b/run"   # gradle run directory of the st01C2BProbe run configuration (shared probe server)
mkdir -p "$results" "$logs" "$fixed"
name="$mode-$seed-$terrain-r$radius"
out="$results/$name.json"
rm -f "$out"; rm -rf "$fixed/world"
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo "eula=true" > "$fixed/eula.txt"
cat > "$fixed/server.properties" <<PROPS
level-name=world
level-seed=$seed
online-mode=false
server-port=$port
view-distance=2
simulation-distance=2
spawn-monsters=false
spawn-animals=false
spawn-protection=0
max-tick-time=-1
sync-chunk-writes=false
enable-status=false
PROPS
extra=""
if [ "$terrain" = legacy ]; then extra="-Dmm.st01.terrain.legacy=true"; fi
cd "$root"
set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d3.out=$out -Dmm.st01.d3.mode=$mode -Dmm.st01.d3.radius=$radius $extra" \
  timeout "${PROBE_TIMEOUT:-3600}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?
set -e
rm -rf "$fixed/world"
if [ ! -s "$out" ]; then echo "probe $name produced no result (rc=$rc); see $logs/$name.log"; tail -30 "$logs/$name.log"; exit 1; fi
echo "probe $name done: $out"

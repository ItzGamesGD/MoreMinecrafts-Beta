#!/usr/bin/env bash
# ST-01 P8-D2 isolated probe runner (evidence tooling, not production).
#   run_probe.sh <mode> <seed> <out.json> <pack 0|1> [extra -D args...]
# Uses the existing gradle run configuration runSt01C2BProbe (runDir build/st01-p8c2b/run) with the probe entrypoint
# dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d2.St01D2Probe temporarily listed in the gametest fabric.mod.json (never committed).
# KEEP=1 keeps the existing world (used for the restart/reload phase). Requires JAVA_HOME = JDK 25.
set -euo pipefail
mode=$1; seed=$2; out=$3; pack=$4; shift 4
root=$(cd "$(dirname "$0")/../../../.." && pwd)
d="$root/build/st01-p8c2b/run"
[ "${KEEP:-0}" = 1 ] || rm -rf "$d/world"
mkdir -p "$d/world/datapacks"
rm -rf "$d/world/datapacks/st01d2"
packs=vanilla,fabric
if [ "$pack" = 1 ]; then cp -r "$root/build/st01-p8d2/pack-src/st01d2" "$d/world/datapacks/st01d2"; sed -i "s/st01d2:map_r20\"/st01d2:${TAGTRADE:-map_r20}\"/" "$d/world/datapacks/st01d2/data/minecraft/tags/villager_trade/cartographer/level_2.json"; packs=vanilla,fabric,file/st01d2; fi
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo eula=true > "$d/eula.txt"
cat > "$d/server.properties" <<PROPS
level-name=world
level-seed=$seed
online-mode=false
server-port=$port
view-distance=2
simulation-distance=2
spawn-monsters=false
spawn-animals=false
enable-status=false
max-tick-time=-1
initial-enabled-packs=$packs
PROPS
args=""
for a in "$@"; do args="$args $a"; done
rm -f "$out"; cd "$root"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d2.out=$out -Dmm.st01.d2.mode=$mode $args" timeout "${PROBE_TIMEOUT:-1500}" ./gradlew --no-daemon runSt01C2BProbe -q > "$out.log" 2>&1 || echo "gradle rc=$? (see $out.log)"
[ -s "$out" ] && echo "ok $out" || { echo "NO RESULT $out"; tail -15 "$out.log"; exit 1; }

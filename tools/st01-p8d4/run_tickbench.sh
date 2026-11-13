#!/usr/bin/env bash
# ST-01 P8-D4 tick benchmark on a fresh Minecraft 26.3 dedicated server (validation only; not part of build/CI).
#   <seed> <radius> <legacy|nomemo|memo|prep>
#   legacy : pre-P8-D3 terrain, no memo      nomemo : C1+C2 (P8-D3) without the C4 memo
#   memo   : C1+C2+C4                         prep   : C1+C2+C4 plus a background preparation before the vanilla call (C6 experiment)
#   prefetch: prep, then the target chunk is loaded with a FORCED ticket (asynchronous) before the vanilla call
# Output: build/st01-p8d4/results/tick-<seed>-r<radius>-<mode>.json. Requires JAVA_HOME = JDK 25.
set -euo pipefail
seed=$1; radius=$2; mode=$3
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d4/results"; logs="$root/build/st01-p8d4/logs"
fixed="$root/build/st01-p8c2b/run"   # run directory of the existing st01C2BProbe run configuration
mkdir -p "$results" "$logs" "$fixed"
name="tick-$seed-r$radius-$mode"; out="$results/$name.json"
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
extra="-Dmm.st01.d4.radius=$radius"
case "$mode" in
  legacy) extra="$extra -Dmm.st01.terrain.legacy=true" ;;
  nomemo) extra="$extra -Dmm.st01.memo.disable=true" ;;
  memo)   ;;
  prep)   extra="$extra -Dmm.st01.d4.prepare=worker" ;;
  prefetch) extra="$extra -Dmm.st01.d4.prepare=worker -Dmm.st01.d4.prefetch=true" ;;
  *) echo "unknown mode $mode"; exit 2 ;;
esac
cd "$root"
set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d4.out=$out $extra" timeout "${PROBE_TIMEOUT:-2400}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?
set -e
rm -rf "$fixed/world"
if [ ! -s "$out" ]; then echo "tick bench $name produced no result (rc=$rc); see $logs/$name.log"; tail -30 "$logs/$name.log"; exit 1; fi
echo "tick bench $name done"

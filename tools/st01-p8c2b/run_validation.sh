#!/usr/bin/env bash
# ST-01 P8-C2-B natural worldgen validation for ONE seed: <seed> [radius-blocks]
#   scan    : fresh world; staged prediction vs actual starts; FULL generation in NORMAL chunk order; completion handoff;
#             Guardian/reward lifecycle; per-start hashes and natural-world dumps
#   reverse : fresh world, same seed, anchors and chunks in REVERSE order
#   random  : fresh world, same seed, anchors and chunks in seeded RANDOM order
#   reload  : the saved scan world restarted; registration, Guardian defeat and reward state must persist
# Output: build/st01-p8c2b/results/<seed>/{scan,reverse,random,reload}.json + dumps/. Requires JAVA_HOME = JDK 25. Validation only.
set -euo pipefail
seed=$1; radius=${2:-10240}
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8c2b"
results="$base/results/$seed"
logs="$base/logs/$seed"
fixed="$base/run"   # gradle run directory of the st01C2BProbe config
mkdir -p "$results" "$logs"

phase() {
  local name=$1
  local port
  port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
  rm -f "$results/$name.json"
  mkdir -p "$fixed"
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
  cd "$root"
  set +e
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.c2b.out=$results/$name.json -Dmm.st01.c2b.phase=$name -Dmm.st01.c2b.radius=$radius -Dmm.st01.c2b.dump=$results/dumps -Dmm.st01.c2b.scanResult=$results/scan.json" \
    timeout "${PROBE_TIMEOUT:-5400}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
  local rc=$?
  set -e
  if [ ! -s "$results/$name.json" ]; then
    echo "phase $name produced no result (gradle rc=$rc); see $logs/$name.log"; tail -40 "$logs/$name.log"; return 1
  fi
  echo "phase $name done"
}

rm -rf "$fixed" "$results/dumps"
phase scan
rm -rf "$base/world-$seed-scan" && cp -r "$fixed/world" "$base/world-$seed-scan"
for p in reverse random; do rm -rf "$fixed/world"; phase "$p"; done
rm -rf "$fixed/world" && cp -r "$base/world-$seed-scan" "$fixed/world"
phase reload
rm -rf "$base/world-$seed-scan"
python3 -I "$root/tools/st01-p8c2b/verify_results.py" "$results"

#!/usr/bin/env bash
# Runs ONE matched-sample cell: <label> <seed> [variant.json|baseline] [radius-chunks]
# Output: build/ore-sampling/results/<label>__<seed>.json
# Requires JAVA_HOME = JDK 25 (Minecraft 26.3). Deterministic: same args -> same world, same chunks.
set -euo pipefail
label=$1; seed=$2; variant=${3:-baseline}; radius=${4:-20}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/ore-sampling/run"
out="$root/build/ore-sampling/results/${label}__${seed}.json"
log="$root/build/ore-sampling/logs/${label}__${seed}.log"
mkdir -p "$(dirname "$out")" "$(dirname "$log")"
rm -rf "$run/world" "$out"
mkdir -p "$run/world/datapacks"
packs="vanilla"
if [ "$variant" != "baseline" ]; then
  python3 -I "$root/tools/ore-sampling/build_variant_pack.py" "$variant" "$run/world/datapacks/variant"
  packs="vanilla,file/variant"
fi
echo "eula=true" > "$run/eula.txt"
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
sync-chunk-writes=false
allow-nether=true
initial-enabled-packs=$packs
enable-status=false
PROPS
cd "$root"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.ore.out=$out -Dmm.ore.label=$label -Dmm.ore.radius=$radius" \
  timeout "${SAMPLING_TIMEOUT:-1500}" ./gradlew --no-daemon runOreSampling -q > "$log" 2>&1 || { echo "FAILED $label/$seed (see $log)"; exit 1; }
test -s "$out" && echo "$label/$seed ok: $(python3 -c "import json;d=json.load(open('$out'));print('error' in d and d['error'] or 'debris=%d adamantium=%d chunks=%d'%(d['ancient_debris']['total_blocks'],d['adamantium_debris']['total_blocks'],d['chunks_counted']))")"

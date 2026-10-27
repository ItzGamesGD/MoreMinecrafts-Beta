#!/usr/bin/env bash
# Runs ONE Overworld matched-sample cell: <label> <seed> [radius-chunks]
# Output: build/etherium-sampling/results/<label>__<seed>.json   (requires JAVA_HOME = JDK 25)
set -euo pipefail
label=$1; seed=$2; radius=${3:-20}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/etherium-sampling/run"
out="$root/build/etherium-sampling/results/${label}__${seed}.json"
log="$root/build/etherium-sampling/logs/${label}__${seed}.log"
mkdir -p "$(dirname "$out")" "$(dirname "$log")"
rm -rf "$run/world" "$out"
mkdir -p "$run"
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
enable-status=false
PROPS
cd "$root"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.ore.out=$out -Dmm.ore.label=$label -Dmm.ore.radius=$radius" \
  timeout "${SAMPLING_TIMEOUT:-1500}" ./gradlew --no-daemon ${GRADLE_ARGS:-} runEtheriumSampling -q > "$log" 2>&1 || { echo "FAILED $label/$seed (see $log)"; exit 1; }
test -s "$out" && python3 -I -c "import json;d=json.load(open('$out'));print('$label/$seed', d.get('error') or 'diamond=%d etherium=%d ratio=%.4f chunks=%d'%(d['diamond_blocks'],d['etherium_blocks'],d['etherium_to_diamond_ratio'],d['chunks_counted']))"

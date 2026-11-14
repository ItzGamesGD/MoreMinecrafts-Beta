#!/usr/bin/env bash
# Runs ONE probe cell: <adaptation> <profile> <placement> <seed> [samples] [shell: built|bare]
# Output: build/worldgen-probe/results/<shell>__<adaptation>__<profile>__<placement>__<seed>.json
# Requires JAVA_HOME = JDK 25 (Minecraft 26.3). Deterministic: same args -> same world/structures.
set -euo pipefail
adaptation=$1; profile=$2; placement=$3; seed=$4; samples=${5:-16}; shell=${6:-built}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/worldgen-probe/run"
label="${shell}__${adaptation}__${profile}__${placement}__${seed}"
out="$root/build/worldgen-probe/results/$label.json"
log="$root/build/worldgen-probe/logs/$label.log"
mkdir -p "$(dirname "$out")" "$(dirname "$log")"
rm -rf "$run/world"
mkdir -p "$run/world/datapacks"
python3 -I "$root/tools/worldgen-probe/build_probe_pack.py" "$run/world/datapacks/probe" \
  --adaptation "$adaptation" --profile "$profile" --placement "$placement" --shell "$shell"
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
initial-enabled-packs=vanilla,file/probe
enable-status=false
PROPS
start=$(date +%s)
cd "$root"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.probe.out=$out -Dmm.probe.samples=$samples -Dmm.probe.label=$label -Dmm.probe.slices=${PROBE_SLICES:-0} ${PROBE_MIN_SURFACE:+-Dmm.probe.minSurface=$PROBE_MIN_SURFACE} ${PROBE_SEARCH_RADIUS:+-Dmm.probe.searchRadius=$PROBE_SEARCH_RADIUS}" \
  timeout "${PROBE_TIMEOUT:-1500}" ./gradlew runWorldgenProbe -q > "$log" 2>&1 || { echo "FAILED $label (see $log)"; exit 1; }
echo "$label ok in $(( $(date +%s) - start ))s"

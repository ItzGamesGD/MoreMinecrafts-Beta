#!/usr/bin/env bash
# ST-01 P8-A live placement probe: two dedicated-server processes on one flat test world.
#   phase 1 "place":  fresh world, real StructureTemplate placement at (-40,-24,2), assertions, save, record state hash
#   phase 2 "reload": new server process on the saved world, no placement, same assertions + hash comparison
# Output: build/st01-p8a-probe/results/{place,reload}.json   logs: build/st01-p8a-probe/logs/{place,reload}.log
# Requires JAVA_HOME = JDK 25 (Minecraft 26.3). Validation-only; does not modify the structure.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8a-probe"
run="$base/run"
results="$base/results"
logs="$base/logs"
mkdir -p "$results" "$logs"
port=$((20000 + RANDOM % 20000))

write_props() {
  cat > "$run/server.properties" <<PROPS
level-name=world
level-type=minecraft:flat
generate-structures=false
online-mode=false
server-port=$port
view-distance=4
simulation-distance=4
spawn-monsters=false
spawn-animals=false
spawn-protection=0
max-tick-time=-1
enable-status=false
PROPS
}

phase() {
  local name=$1
  rm -f "$results/$name.json"
  mkdir -p "$run"
  echo "eula=true" > "$run/eula.txt"
  write_props
  cd "$root"
  set +e
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.p8a.out=$results/$name.json -Dmm.st01.p8a.phase=$name -Dmm.st01.p8a.hash=$results/state.sha256" \
    timeout "${PROBE_TIMEOUT:-1200}" ./gradlew --no-daemon runRoyalHallPlacementProbe -q > "$logs/$name.log" 2>&1
  local rc=$?
  set -e
  if [ ! -s "$results/$name.json" ]; then
    echo "phase $name produced no result (gradle rc=$rc); see $logs/$name.log"
    tail -40 "$logs/$name.log"
    return 1
  fi
}

# Phase 1 always starts from a fresh world so the placement is the one under test.
rm -rf "$run/world"
phase place
# Phase 2 reloads the world written by phase 1 (no second placement).
phase reload

python3 -I - "$results/place.json" "$results/reload.json" <<'PY'
import json, sys
for path in sys.argv[1:]:
    d = json.load(open(path))
    if "error" in d:
        print(path, "ERROR:", d["error"]); continue
    print(f"== {d['phase']}: all_pass={d['all_pass']} voxel_mismatches={d.get('voxel_mismatches')} sha256={d.get('state_sha256')}")
    for c in d["checks"]:
        print(f"  [{c['verdict']}] {c['id']}: {c['evidence'][:220]}")
PY

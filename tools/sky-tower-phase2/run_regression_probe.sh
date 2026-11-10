#!/usr/bin/env bash
# Runs the EXISTING C-3 composite probe (SkyTowerCompositeWorldgenProbe: spawner/vault counts, automatic site registration) on a fresh world,
# optionally under a disposable datapack override (P2_DATAPACK_DIR). usage: run_regression_probe.sh <label> <seed> [radius]
# env: LEVEL_TYPE, GENERATOR_SETTINGS, P2_DATAPACK_DIR. Output: build/sky-tower-phase2/results/<label>/composite.json
set -euo pipefail
label=$1; seed=$2; radius=${3:-160}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/sky-tower-phase2/run/$label"; out="$root/build/sky-tower-phase2/results/$label"; log="$root/build/sky-tower-phase2/logs/$label.log"
mkdir -p "$(dirname "$log")" "$run" "$out"; rm -rf "$run/world" "$out/composite.json"
echo "eula=true" > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=world
level-seed=$seed
${LEVEL_TYPE:+level-type=$LEVEL_TYPE}
${GENERATOR_SETTINGS:+generator-settings=$GENERATOR_SETTINGS}
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
if [[ -n "${P2_DATAPACK_DIR:-}" ]]; then
  mkdir -p "$run/world/datapacks"; cp -r "$P2_DATAPACK_DIR" "$run/world/datapacks/p2_override"
  echo "initial-enabled-packs=vanilla,fabric,file/p2_override" >> "$run/server.properties"
fi
cd "$root"
P2_RUN_DIR="build/sky-tower-phase2/run/$label" JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.sky.tower.out=$out/composite.json -Dmm.sky.tower.radius=$radius -Dmm.sky.tower.resume=false -Dmm.sky.tower.persistTestProgress=false -Dmm.sky.tower.persistFloorClear=false" \
  timeout "${PROBE_TIMEOUT:-1800}" ./gradlew --no-daemon -I tools/sky-tower-phase2/run_probe.init.gradle runSkyIslandsProbe -q > "$log" 2>&1 || true
test -s "$out/composite.json" || { echo "no composite.json for $label (see $log)"; tail -30 "$log"; exit 1; }
python3 -I - "$out/composite.json" <<'PY'
import json, sys
r = json.load(open(sys.argv[1]))
if "error" in r: sys.exit("probe error: " + r["error"])
for style, v in r["styles"].items():
    print(style, "pass=", v.get("pass"), "site=", v.get("site"), "spawners=", v.get("trial_spawners"), "vaults=", v.get("vaults"), "registered=", v.get("automatically_registered"))
print("overall pass", r["pass"], "styles_found", r["styles_found"])
PY

#!/usr/bin/env bash
# Runs the EXISTING C-3 composite probe (SkyTowerCompositeWorldgenProbe: spawner/vault counts, automatic site registration) on a fresh world,
# optionally under a disposable datapack override (AD_DATAPACK_DIR). usage: run_regression_probe.sh <label> <seed> [radius]
# env: LEVEL_TYPE, GENERATOR_SETTINGS, AD_DATAPACK_DIR. Output: build/sky-tower-adaptive/results/<label>/composite.json
set -euo pipefail
label=$1; seed=$2; radius=${3:-160}; resume=${4:-0}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/sky-tower-adaptive/run/$label"; out="$root/build/sky-tower-adaptive/results/$label"; log="$root/build/sky-tower-adaptive/logs/$label.log"
mkdir -p "$(dirname "$log")" "$run" "$out"; if [[ "$resume" != "1" ]]; then rm -rf "$run/world"; fi; rm -f "$out/composite$([[ "$resume" == "1" ]] && echo "-resume").json"
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
if [[ -n "${AD_DATAPACK_DIR:-}" ]]; then
  mkdir -p "$run/world/datapacks"; cp -r "$AD_DATAPACK_DIR" "$run/world/datapacks/ad_override"
  echo "initial-enabled-packs=vanilla,fabric,file/ad_override" >> "$run/server.properties"
fi
cd "$root"
AD_RUN_DIR="build/sky-tower-adaptive/run/$label" JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.sky.tower.out=$out/composite$([[ "$resume" == "1" ]] && echo "-resume").json -Dmm.sky.tower.radius=$radius -Dmm.sky.tower.resume=$([[ "$resume" == "1" ]] && echo true || echo false) -Dmm.sky.tower.persistTestProgress=${PERSIST_PROGRESS:-false} -Dmm.sky.tower.persistFloorClear=false" \
  timeout "${PROBE_TIMEOUT:-1800}" ./gradlew --no-daemon -I tools/sky-tower-adaptive/run_probe.init.gradle runSkyIslandsProbe -q > "$log" 2>&1 || true
test -s "$out/composite$([[ "$resume" == "1" ]] && echo "-resume").json" || { echo "no composite.json for $label (see $log)"; tail -30 "$log"; exit 1; }
python3 -I - "$out/composite$([[ "$resume" == "1" ]] && echo "-resume").json" <<'PY'
import json, sys
r = json.load(open(sys.argv[1]))
if "error" in r: sys.exit("probe error: " + r["error"])
for style, v in r["styles"].items():
    print(style, "pass=", v.get("pass"), "site=", v.get("site"), "spawners=", v.get("trial_spawners"), "vaults=", v.get("vaults"), "registered=", v.get("automatically_registered"))
print("overall pass", r["pass"], "styles_found", r["styles_found"])
PY

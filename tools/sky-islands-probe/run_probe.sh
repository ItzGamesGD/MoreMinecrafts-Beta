#!/usr/bin/env bash
# Runs the Sky Islands natural-generation probe for ONE seed: <seed> [max-radius-chunks]
# Output: build/sky-islands-probe/results/<seed>.json   (requires JAVA_HOME = JDK 25)
set -euo pipefail
seed=$1; radius=${2:-160}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/sky-islands-probe/run"
out="$root/build/sky-islands-probe/results/${seed}.json"
log="$root/build/sky-islands-probe/logs/${seed}.log"
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
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.sky.out=$out -Dmm.sky.radius=$radius" \
  timeout "${PROBE_TIMEOUT:-2400}" ./gradlew --no-daemon ${GRADLE_ARGS:-} runSkyIslandsProbe -q > "$log" 2>&1 || { echo "FAILED seed $seed (see $log)"; tail -40 "$log"; exit 1; }
test -s "$out" && python3 -I - "$out" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
if "error" in d:
    sys.exit("probe error: " + d["error"])
print("seed", d["seed"], "starts", d["structure_starts"], "variants", d["building_variants_seen"], "missing", d["missing_variants"],
      "pass", d["all_physical_checks_pass"])
sys.exit(0 if d["all_physical_checks_pass"] else 1)
PY

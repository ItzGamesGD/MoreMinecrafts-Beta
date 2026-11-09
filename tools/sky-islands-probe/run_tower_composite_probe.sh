#!/usr/bin/env bash
# Fresh-world C-3 natural Jigsaw placement and automatic-registration probe.
set -euo pipefail
seed=$1; radius=${2:-160}; resume=${3:-0}
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/sky-tower-composite-probe/run"
suffix=""
if [[ "$resume" == "1" ]]; then suffix="-resume"; fi
persist_progress=false
if [[ "${PERSIST_PROGRESS:-0}" == "1" && "$resume" != "1" ]]; then persist_progress=true; fi
out="$root/build/sky-tower-composite-probe/results/${seed}${suffix}.json"
log="$root/build/sky-tower-composite-probe/logs/${seed}.log"
mkdir -p "$(dirname "$out")" "$(dirname "$log")"
if [[ "$resume" != "1" ]]; then rm -rf "$run/world"; fi
rm -f "$out"
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
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.sky.tower.out=$out -Dmm.sky.tower.radius=$radius -Dmm.sky.tower.resume=$([[ "$resume" == "1" ]] && echo true || echo false) -Dmm.sky.tower.persistTestProgress=$persist_progress" \
  timeout "${PROBE_TIMEOUT:-2400}" ./gradlew --no-daemon ${GRADLE_ARGS:-} \
    -I tools/sky-islands-probe/tower_probe.init.gradle runSkyIslandsProbe -q > "$log" 2>&1 || {
    echo "FAILED seed $seed (see $log)"; tail -50 "$log"; exit 1;
  }
test -s "$out" || { echo "probe produced no report (see $log)"; exit 1; }
python3 -I - "$out" <<'PY'
import json, os, sys
report = json.load(open(sys.argv[1]))
if "error" in report:
    sys.exit("probe error: " + report["error"])
if report.get("restart"):
    print("restart", report["pass"], "sites", report["sites_before"], "->", report["sites_after"],
          "structure_starts", report["structure_start_reloaded_for_site"], "progress", report["same_progress"])
else:
    for style, result in report["styles"].items():
        print(style, result.get("pass"), result.get("site"), result.get("rotation"),
              result.get("trial_spawners"), result.get("vaults"),
              "registered=" + str(result.get("automatically_registered")))
    print("pass", report["pass"], "styles_found", report["styles_found"])
partial_ok = any(result.get("pass") for result in report.get("styles", {}).values())
sys.exit(0 if report["pass"] or (os.environ.get("ALLOW_PARTIAL") == "1" and partial_ok) else 1)
PY

#!/usr/bin/env bash
# ST-01 Phase 4 production worldgen validation for ONE seed: <seed> [radius-blocks]
#   scan   : fresh world, registration audit, staged prediction vs actual starts, FULL generation + physical verification, correction fixture
#   replay : fresh world, same seed, REVERSE generation order; per-start hashes must equal scan
#   reload : saved scan world restarted (no regeneration of anchors beyond loading); per-start hashes must equal scan
# Output: build/st01-phase4/results/<seed>/{scan,replay,reload}.json   Requires JAVA_HOME = JDK 25. Validation-only.
set -euo pipefail
seed=$1; radius=${2:-10240}
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-phase4"
run="$base/run-$seed"
results="$base/results/$seed"
logs="$base/logs/$seed"
mkdir -p "$results" "$logs"
port=$((20000 + RANDOM % 20000))

phase() {
  local name=$1
  rm -f "$results/$name.json"
  mkdir -p "$run"
  echo "eula=true" > "$run/eula.txt"
  cat > "$run/server.properties" <<PROPS
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
  # the Gradle run task shares one run directory per config: point it at a per-seed directory
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.p4.out=$results/$name.json -Dmm.st01.p4.phase=$name -Dmm.st01.p4.radius=$radius -Dmm.st01.p4.dump=$results/dump-$name" \
    timeout "${PROBE_TIMEOUT:-3000}" ./gradlew --no-daemon runSt01Phase4Probe -q > "$logs/$name.log" 2>&1
  local rc=$?
  set -e
  if [ ! -s "$results/$name.json" ]; then
    echo "phase $name produced no result (gradle rc=$rc); see $logs/$name.log"; tail -40 "$logs/$name.log"; return 1
  fi
}

# gradle's run directory is build/st01-phase4/run (fixed in build.gradle): stage the per-seed world there
fixed="$base/run"
rm -rf "$fixed" && mkdir -p "$fixed"
run="$fixed"
phase scan
rm -rf "$base/world-$seed-scan" && cp -r "$fixed/world" "$base/world-$seed-scan"
rm -rf "$fixed/world"
phase replay
rm -rf "$fixed/world" && cp -r "$base/world-$seed-scan" "$fixed/world"
phase reload

python3 -I "$root/tools/st01-phase4/verify_results.py" "$results/scan.json" "$results/replay.json" "$results/reload.json"

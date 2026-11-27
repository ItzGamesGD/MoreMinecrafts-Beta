#!/usr/bin/env bash
# SKY TOWER mountain-worldgen Phase 2 probe driver (validation tooling; not part of check/build/CI). Requires JAVA_HOME = JDK 25.
# usage: run_probe.sh <label> <seed> <mode: world|controlled|forced> [extra -Dmm.p2.* props...]
# env:   LEVEL_TYPE / GENERATOR_SETTINGS (flat worlds, as in tools/sky-islands-probe), PROBE_TIMEOUT
# output: build/sky-tower-phase2/results/<label>/{trace.csv,strategies.csv,natural.csv,topblocks.csv,elements.csv,summary.json,...}
set -euo pipefail
label=$1; seed=$2; mode=$3; shift 3
root=$(cd "$(dirname "$0")/../.." && pwd)
run="$root/build/sky-tower-phase2/run/$label"
out="$root/build/sky-tower-phase2/results/$label"
log="$root/build/sky-tower-phase2/logs/$label.log"
mkdir -p "$(dirname "$log")" "$run"
rm -rf "$run/world" "$out"; mkdir -p "$out"
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
# optional disposable datapack (validation only): P2_DATAPACK_DIR=<dir containing pack.mcmeta + data/>, enabled for the new world
if [[ -n "${P2_DATAPACK_DIR:-}" ]]; then
  mkdir -p "$run/world/datapacks"; cp -r "$P2_DATAPACK_DIR" "$run/world/datapacks/p2_override"
  echo "initial-enabled-packs=vanilla,fabric,file/p2_override" >> "$run/server.properties"
fi
props="-Dmm.p2.out=$out -Dmm.p2.mode=$mode -Dmm.p2.label=$label"
for p in "$@"; do props="$props $p"; done
cd "$root"
P2_RUN_DIR="build/sky-tower-phase2/run/$label" JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $props" \
  timeout "${PROBE_TIMEOUT:-3000}" ./gradlew --no-daemon -I tools/sky-tower-phase2/run_probe.init.gradle runSkyIslandsProbe -q > "$log" 2>&1 || true
if [[ -s "$out/ERROR.txt" ]]; then echo "ERROR in $label"; cat "$out/ERROR.txt"; tail -30 "$log"; exit 1; fi
test -s "$out/summary.json" || { echo "no summary for $label (see $log)"; tail -40 "$log"; exit 1; }
echo "OK $label -> $out"

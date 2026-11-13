#!/usr/bin/env bash
# ST-01 P8-D7 placeholder-trade experiment bench (validation only; not part of build/CI).
#   <seed> <pending|pending-save|reload> [name-suffix]  ; PRELOAD=1 enables the STRUCTURE_STARTS preload experiment.
# Uses tools/st01-p8d6/bench-pack (bench-only data). reload reuses the world saved by pending-save.
set -euo pipefail
seed=$1; scen=$2; suffix=${3:-}
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d7/results"; logs="$root/build/st01-p8d7/logs"; fixed="$root/build/st01-p8c2b/run"
mkdir -p "$results" "$logs" "$fixed"
name="d7-$seed-$scen$suffix"; out="$results/$name.json"; rm -f "$out"
if [ "$scen" != reload ]; then
  rm -rf "$fixed/world"; mkdir -p "$fixed/world/datapacks"; cp -r "$root/tools/st01-p8d6/bench-pack" "$fixed/world/datapacks/st01-bench"
fi
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo "eula=true" > "$fixed/eula.txt"
printf 'level-name=world\nlevel-seed=%s\nonline-mode=false\nserver-port=%s\nview-distance=2\nsimulation-distance=2\nspawn-monsters=false\nspawn-animals=false\nspawn-protection=0\nmax-tick-time=-1\nsync-chunk-writes=false\nenable-status=false\ninitial-enabled-packs=vanilla,file/st01-bench\n' "$seed" "$port" > "$fixed/server.properties"
p="-Dmm.st01.d7.scenario=$scen"
[ "${PRELOAD:-0}" = 1 ] && p="$p -Dmm.st01.d7.preload=true"
cd "$root"; set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d7.out=$out $p ${BENCH_PROPS:-}" timeout "${PROBE_TIMEOUT:-900}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?; set -e
[ -s "$out" ] || { echo "d7 bench $name produced no result (rc=$rc)"; tail -20 "$logs/$name.log"; exit 1; }
echo "d7 bench $name done (rc=$rc)"

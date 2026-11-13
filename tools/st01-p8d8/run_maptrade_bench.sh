#!/usr/bin/env bash
# ST-01 P8-D8 cartographer map-trade benchmark (validation only; not part of build/CI). Production data, no data pack.
#   <seed> <single|multi|save|reload> [name-suffix]
# BLOCKING=1 runs the vanilla synchronous baseline. SETTLE=<ticks> between screen opening and level-up (default 5).
# The map chance is forced to 1 so that every level-up offers the map. reload reuses the world saved by save.
set -euo pipefail
seed=$1; scen=$2; suffix=${3:-}
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d8/results"; logs="$root/build/st01-p8d8/logs"; fixed="$root/build/st01-p8c2b/run"
mkdir -p "$results" "$logs" "$fixed"
name="d8-$seed-$scen$suffix"; out="$results/$name.json"; rm -f "$out"
[ "$scen" = reload ] || rm -rf "$fixed/world"
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo "eula=true" > "$fixed/eula.txt"
printf 'level-name=world\nlevel-seed=%s\nonline-mode=false\nserver-port=%s\nview-distance=4\nsimulation-distance=4\nspawn-monsters=false\nspawn-animals=false\nspawn-protection=0\nmax-tick-time=-1\nsync-chunk-writes=false\nenable-status=false\n' "$seed" "$port" > "$fixed/server.properties"
p="-Dmm.st01.d8.scenario=$scen -Dmm.st01.maptrade.chance=1 -Dmm.st01.d8.settle=${SETTLE:-5}"
[ "${BLOCKING:-0}" = 1 ] && p="$p -Dmm.st01.maptrade.blocking=true"
cd "$root"; set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d8.out=$out $p ${BENCH_PROPS:-}" timeout "${PROBE_TIMEOUT:-1200}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?; set -e
[ -s "$out" ] || { echo "d8 bench $name produced no result (rc=$rc)"; tail -20 "$logs/$name.log"; exit 1; }
echo "d8 bench $name done (rc=$rc)"

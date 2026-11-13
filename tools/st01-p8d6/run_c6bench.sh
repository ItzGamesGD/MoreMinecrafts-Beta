#!/usr/bin/env bash
# ST-01 P8-D6 benchmark of the production C6 search preparation (validation only; not part of build/CI).
#   <seed> <scenario> [name-suffix]   scenarios: none ready notready multi cancel villager villager-notready villager-reload
# villager* scenarios install tools/st01-p8d6/bench-pack (bench-only data). KEEP_WORLD=1 reuses the previous world
# (villager-reload); SAVE=1 saves the world at the end. Extra JVM properties via BENCH_PROPS. Requires JAVA_HOME = JDK 25.
set -euo pipefail
seed=$1; scen=$2; suffix=${3:-}
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d6/results"; logs="$root/build/st01-p8d6/logs"; fixed="$root/build/st01-p8c2b/run"
mkdir -p "$results" "$logs" "$fixed"
name="d6-$seed-$scen$suffix"; out="$results/$name.json"; rm -f "$out"
[ "${KEEP_WORLD:-0}" = 1 ] || rm -rf "$fixed/world"
packs=vanilla
if [[ "$scen" == villager* ]]; then
  mkdir -p "$fixed/world/datapacks"; rm -rf "$fixed/world/datapacks/st01-bench"
  cp -r "$root/tools/st01-p8d6/bench-pack" "$fixed/world/datapacks/st01-bench"; packs="vanilla,file/st01-bench"
fi
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo "eula=true" > "$fixed/eula.txt"
printf 'level-name=world\nlevel-seed=%s\nonline-mode=false\nserver-port=%s\nview-distance=2\nsimulation-distance=2\nspawn-monsters=false\nspawn-animals=false\nspawn-protection=0\nmax-tick-time=-1\nsync-chunk-writes=false\nenable-status=false\ninitial-enabled-packs=%s\n' "$seed" "$port" "$packs" > "$fixed/server.properties"
p="-Dmm.st01.d6.scenario=$scen ${BENCH_PROPS:-}"
[ "${SAVE:-0}" = 1 ] && p="$p -Dmm.st01.d6.save=true"
cd "$root"; set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d6.out=$out $p" timeout "${PROBE_TIMEOUT:-900}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?; set -e
[ -s "$out" ] || { echo "d6 bench $name produced no result (rc=$rc)"; tail -20 "$logs/$name.log"; exit 1; }
echo "d6 bench $name done (rc=$rc)"

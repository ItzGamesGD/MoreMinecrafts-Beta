#!/usr/bin/env bash
# ST-01 P8-D5 C6 feasibility bench (experiment only): <seed> <radius> <scenario> [name-suffix]
# scenarios: noprep | prep | split | warm | val | valwarm | multi | cancel
# KEEP_WORLD=1 keeps the world of the previous run (restart experiments); SAVE=1 saves the world at the end.
set -euo pipefail
seed=$1; radius=$2; scen=$3; suffix=${4:-}
root=$(cd "$(dirname "$0")/../.." && pwd)
results="$root/build/st01-p8d5/results"; logs="$root/build/st01-p8d5/logs"; fixed="$root/build/st01-p8c2b/run"
mkdir -p "$results" "$logs" "$fixed"
name="c6-$seed-r$radius-$scen$suffix"; out="$results/$name.json"; rm -f "$out"
[ "${KEEP_WORLD:-0}" = 1 ] || rm -rf "$fixed/world"
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo "eula=true" > "$fixed/eula.txt"
printf 'level-name=world\nlevel-seed=%s\nonline-mode=false\nserver-port=%s\nview-distance=2\nsimulation-distance=2\nspawn-monsters=false\nspawn-animals=false\nspawn-protection=0\nmax-tick-time=-1\nsync-chunk-writes=false\nenable-status=false\n' "$seed" "$port" > "$fixed/server.properties"
p="-Dmm.st01.d5.radius=$radius"
case "$scen" in
  noprep)  p="$p -Dmm.st01.d5.prepare=false" ;;
  prep)    ;;
  split)   p="$p -Dmm.st01.d5.split=true" ;;
  warm)    p="$p -Dmm.st01.d5.warm=64" ;;
  val)     p="$p -Dmm.st01.d5.validate=offthread" ;;
  valwarm) p="$p -Dmm.st01.d5.validate=offthread -Dmm.st01.d5.warm=64" ;;
  multi)   p="$p -Dmm.st01.d5.validate=offthread -Dmm.st01.d5.warm=64 -Dmm.st01.d5.origins=8" ;;
  cancel)  p="$p -Dmm.st01.d5.cancelAfterMs=${CANCEL_MS:-300}" ;;
  *) echo "unknown scenario"; exit 2 ;;
esac
[ "${SAVE:-0}" = 1 ] && p="$p -Dmm.st01.d5.save=true"
cd "$root"; set +e
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.d5.out=$out $p" timeout "${PROBE_TIMEOUT:-1800}" ./gradlew --no-daemon runSt01C2BProbe -q > "$logs/$name.log" 2>&1
rc=$?; set -e
[ -s "$out" ] || { echo "c6 bench $name produced no result (rc=$rc)"; tail -20 "$logs/$name.log"; exit 1; }
echo "c6 bench $name done (rc=$rc)"

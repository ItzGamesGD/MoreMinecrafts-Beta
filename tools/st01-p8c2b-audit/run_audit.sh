#!/usr/bin/env bash
# ST-01 P8-C2-B counterfactual rarity audit, ONE seed: <seed> [radius-blocks] [repeat-count]
# Writes build/st01-p8c2b-audit/results/<seed>/observations-<n>.json (one per repeat). Diagnostic only; production worldgen is unmodified.
# Requires JAVA_HOME = JDK 25.
set -euo pipefail
seed=$1; radius=${2:-10240}; repeats=${3:-1}
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8c2b-audit"; results="$base/results/$seed"; logs="$base/logs/$seed"; fixed="$base/run"
mkdir -p "$results" "$logs"
for n in $(seq 1 "$repeats"); do
  rm -rf "$fixed"; mkdir -p "$fixed"
  port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
  echo "eula=true" > "$fixed/eula.txt"
  cat > "$fixed/server.properties" <<PROPS
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
  cd "$root"; rm -f "$results/observations-$n.json"
  set +e
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.cf.out=$results/observations-$n.json -Dmm.st01.c2b.radius=$radius" \
    timeout "${PROBE_TIMEOUT:-5400}" ./gradlew --no-daemon runSt01CounterfactualProbe -q > "$logs/run-$n.log" 2>&1
  rc=$?; set -e
  if [ ! -s "$results/observations-$n.json" ]; then echo "seed $seed run $n produced no result (rc=$rc)"; tail -30 "$logs/run-$n.log"; exit 1; fi
  echo "seed $seed run $n done"
done

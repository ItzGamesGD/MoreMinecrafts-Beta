#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
run="$root/build/st05-worldgen-probe/run"
mkdir -p "$run" "$root/build"
out=$(mktemp -d "$root/build/spider-full-XXXXXXXX")
printf 'eula=true\n' > "$run/eula.txt"
echo "evidence: $out"
for seed in 101 202 303 707; do
cat > "$run/server.properties" <<PROPS
level-name=spider-full-$(basename "$out")-$seed
level-seed=$seed
online-mode=false
server-port=0
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
 JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.spider.probe.chunks=64 -Dmm.spider.probe.out=$out/$seed.json" timeout 3600 ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$out/$seed.log" 2>&1 || { echo "FAILED seed=$seed log=$out/$seed.log"; exit 1; }
 test -s "$out/$seed.json"
 echo "seed $seed captured"
 python3 tools/spider-colony-probe/analysis.py "$out/$seed.json" || { echo "ANALYSIS FAILED seed=$seed"; exit 1; }
 echo "seed $seed analyzed"
done
python3 tools/spider-colony-probe/summarize.py "$out"

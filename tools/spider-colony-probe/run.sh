#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
run="$root/build/st05-worldgen-probe/run"
mkdir -p "$root/build"
out=$(mktemp -d "$root/build/spider-probe-XXXXXXXX")
mkdir -p "$run"
printf 'eula=true\n' > "$run/eula.txt"
for seed in 101 202 303 404 505 606 707 808; do
cat > "$run/server.properties" <<PROPS
level-name=spider-$(basename "$out")-$seed
level-seed=$seed
online-mode=false
server-port=0
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
 JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.spider.probe.chunks=16 -Dmm.spider.probe.out=$out/$seed.json" timeout 600 ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$out/$seed.log" 2>&1 || { tail -40 "$out/$seed.log"; exit 1; }
 test -s "$out/$seed.json"
 python3 tools/spider-colony-probe/analysis.py "$out/$seed.json"
 echo "seed $seed complete"
done
for seed in 101 202 303 404 505 606 707 808; do
 python3 tools/spider-colony-probe/bottleneck.py "$out/$seed.json"
done
python3 tools/spider-colony-probe/summarize.py "$out"
echo "evidence: $out"

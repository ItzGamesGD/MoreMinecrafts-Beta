#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
run="$root/build/st05-worldgen-probe/run"
mkdir -p "$run" "$root/build"
out=$(mktemp -d "$root/build/spider-707-diagnostic-XXXXXXXX")
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<PROPS
level-name=spider-707-$(basename "$out")
level-seed=707
online-mode=false
server-port=0
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
PROPS
echo "evidence: $out"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.spider.probe.chunks=16 -Dmm.spider.probe.out=$out/707.json" timeout 600 ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$out/707.log" 2>&1
python3 - "$out/707.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1])); print(sorted(r['chunk_times'],key=lambda x:x['ms'],reverse=True)[:10])
PY

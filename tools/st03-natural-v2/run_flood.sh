#!/usr/bin/env bash
set -uo pipefail
seed="${1:?seed required}"
out="$PWD/build/st03-natural/results/$seed"
run_dir="$PWD/build/st03-natural/run"
mkdir -p "$out" "$run_dir"
echo 'eula=true' > "$run_dir/eula.txt"
cat > "$run_dir/server.properties" <<PROPS
level-name=world
level-seed=$seed
online-mode=false
server-port=25575
view-distance=2
simulation-distance=2
spawn-monsters=false
spawn-animals=false
spawn-protection=0
max-tick-time=-1
sync-chunk-writes=false
enable-status=false
PROPS
JAVA_TOOL_OPTIONS="-Xmx4G -Dmm.st03.flood.out=$out" timeout 6600 ./gradlew runSt03NaturalProbe --no-daemon --max-workers=2 2>&1 | tee "$out/probe.log"
rc=${PIPESTATUS[0]}
if [ -f "$out/flood-summary.json" ]; then
  python3 tools/st03-natural-v2/summarize_flood.py "$out"
fi
exit "$rc"

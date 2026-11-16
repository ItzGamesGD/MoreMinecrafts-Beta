#!/usr/bin/env bash
set -euo pipefail
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
JAVA_TOOL_OPTIONS="-Xmx4G -Dmm.st03.natural.out=$out" timeout 6600 ./gradlew runSt03NaturalProbe --no-daemon --max-workers=2 2>&1 | tee "$out/probe.log"
python3 - "$out/result.json" <<'PY'
import json,sys
p=json.load(open(sys.argv[1]));assert p['status']=='PASS',p.get('error')
assert p['valid_starts']==len(p['starts'])
assert p['spacing']==66 and p['separation']==22
assert p['full_generation']['actual_start_valid']
assert p['full_generation']['assembly_pieces']==34
assert p['full_generation']['unsupported_columns']==0
print('PASS: actual starts, unchanged templates, full chunks and support plane')
PY

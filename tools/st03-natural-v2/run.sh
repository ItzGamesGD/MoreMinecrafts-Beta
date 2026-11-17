#!/usr/bin/env bash
set -euo pipefail
seed="${1:?seed required}"
full="${2:-true}"
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
scan_option=''
if [ -d build/st03-natural/source-download ]; then
 scan=$(find "$PWD/build/st03-natural/source-download" -name scan.json -print -quit)
 test -n "$scan"
 scan_option="-Dmm.st03.natural.scan=$scan"
fi
JAVA_TOOL_OPTIONS="-Xmx4G -Dmm.st03.natural.out=$out -Dmm.st03.natural.scanOnly=$([ "$full" = true ] && echo false || echo true) $scan_option" timeout 6600 ./gradlew runSt03NaturalProbe --no-daemon --max-workers=2 2>&1 | tee "$out/probe.log"
python3 - "$out/result.json" <<'PY'
import json,sys
p=json.load(open(sys.argv[1]));assert p['status']=='PASS',p.get('error')
assert p['valid_starts']==len(p['starts'])
assert p['spacing']==66 and p['separation']==22
assert p['potential_candidates']==2601
if p.get('scope')=='actual_start_scan_only':
 assert p['full_generation_status']=='NOT RUN'
 print('PASS: actual start survey; full generation NOT RUN')
 sys.exit(0)
assert len(p['full_generation'])>=(3 if p['seed']==10079 else 1)
for hq in p['full_generation']:
 assert hq['actual_start_valid'] and hq['assembly_pieces']==34 and hq['total_pieces']==35
 assert hq['unsupported_columns']==0 and hq['wet_columns']==0
 assert hq['completion_record_created'] and hq['defenders_first_proximity']==40
 assert hq['max_chunk_radius']<=8
 assert hq['actual_columns_checked']==12544 and hq['actual_max_cut']<=10 and hq['actual_max_skirt']<=12 and hq['actual_max_footing_fill']<=16
print('PASS: actual starts, unchanged templates, full chunks and support plane')
PY

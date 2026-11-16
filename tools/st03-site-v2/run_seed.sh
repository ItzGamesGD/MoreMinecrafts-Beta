#!/usr/bin/env bash
set -euo pipefail
seed="${1:?seed required}"
out_dir="$PWD/build/st03-site-v2/$seed"
run_dir="$PWD/build/st03-site-probe/run"
mkdir -p "$out_dir" "$run_dir"
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
python3 tools/st03-site-v2/publish_progress.py "$out_dir" &
progress_pid=$!
trap 'kill "$progress_pid" 2>/dev/null || true' EXIT
verification_input="${2:-}"
JAVA_TOOL_OPTIONS="-Xmx4G -Dmm.st03.v2.out=$out_dir${verification_input:+ -Dmm.st03.v2.verify.input=$verification_input}" \
  timeout 9900 ./gradlew --no-daemon --max-workers=2 runSt03SiteProbe 2>&1 | tee "$out_dir/probe.log"
python3 - "$out_dir/summary.json" <<'PY'
import json,sys
p=json.load(open(sys.argv[1]))
assert p['status']=='PASS',p
assert p['candidates']==2601,p['candidates']
assert p['search_locations']==2601*289,p['search_locations']
assert all(sum(v.values())==2601 for v in p['counts'].values())
assert all(sum(v.values())==2601 for v in p['search_counts'].values())
assert sum(p['legacy_counts'].values())==2601
for s in range(3):
    for b in (70,80):
        k=f'S{s}_B{b}_W5_E1_P_L2'
        assert p['counts'][k]==p['search_counts'][k+'_N0']
        a=[p['search_counts'][k+f'_N{n}']['PASS'] for n in (0,2,4,8)]
        assert a==sorted(a),a
print('PASS: complete counts, N=0 agreement, nested search monotonicity')
PY

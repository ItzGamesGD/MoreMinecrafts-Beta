#!/usr/bin/env bash
# Second evidence batch, run after run_all.sh with the patched probe (deferred reload check, decoration-based target check, worker dispatch for N3).
set -uo pipefail
root=$(cd "$(dirname "$0")/../../../.." && pwd); R="$root/docs/design/st01-p8d2/probe/run_probe.sh"; O="$root/build/st01-p8d2/out"
run() { "$R" "$@" || echo "INCOMPLETE: $*" >> "$O/incomplete.txt"; }
W="$root/build/st01-p8c2b/run/world"
TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture2-noskip-create.json" 1 -Dmm.st01.d2.phase=create -Dmm.st01.d2.tradeUnderTest=map_r20_noskip
( ls -la "$W"/data/ 2>/dev/null | grep -c "map_" ; du -sk "$W"/data 2>/dev/null; ls "$W"/data | grep -c "^map_" ; ls -la "$W"/data | grep "map_" | awk '{s+=$5} END {print "map_files_bytes_total",s,"count",NR}' ) > "$O/fixture2-map-storage.txt" 2>&1
uuid=$(python3 -I -c "import json;print(json.load(open('$O/fixture2-noskip-create.json'))['persisted_villager']['uuid'])"); at=$(python3 -I -c "import json;print(json.load(open('$O/fixture2-noskip-create.json'))['persisted_villager']['at'])")
KEEP=1 TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture2-noskip-reload.json" 1 -Dmm.st01.d2.phase=reload -Dmm.st01.d2.uuid=$uuid -Dmm.st01.d2.at=$at
for s in 119599970 1001 1004; do for k in 2 3; do run overlap $s "$O/overlap-$s-rep$k.json" 0; done; done
for s in 1004 1001 1003; do for r in 5 20; do run incr $s "$O/incrw-$s-r$r.json" 0 -Dmm.st01.d2.radius=$r "-Dmm.st01.d2.origins=0,0" -Dmm.st01.d2.dispatch=worker -Dmm.st01.d2.precheck=false -Dmm.st01.d2.inflight=16; done; done
echo EXTRA_DONE > "$O/EXTRA_DONE"

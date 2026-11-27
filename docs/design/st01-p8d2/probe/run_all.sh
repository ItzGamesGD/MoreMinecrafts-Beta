#!/usr/bin/env bash
# ST-01 P8-D2 full evidence run (sequential on purpose: timings must not contend). Output dir: build/st01-p8d2/out
set -uo pipefail
root=$(cd "$(dirname "$0")/../../../.." && pwd); R="$root/docs/design/st01-p8d2/probe/run_probe.sh"; O="$root/build/st01-p8d2/out"; mkdir -p "$O"
run() { "$R" "$@" || echo "INCOMPLETE: $*" >> "$O/incomplete.txt"; }

# 1. fixture, additive tag + skip_existing_chunks=false variant, then restart reload
TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture-noskip-create.json" 1 -Dmm.st01.d2.phase=create -Dmm.st01.d2.tradeUnderTest=map_r20_noskip
if [ -s "$O/fixture-noskip-create.json" ]; then
  uuid=$(python3 -I -c "import json;print(json.load(open('$O/fixture-noskip-create.json'))['persisted_villager']['uuid'])" 2>/dev/null || true)
  at=$(python3 -I -c "import json;print(json.load(open('$O/fixture-noskip-create.json'))['persisted_villager']['at'])" 2>/dev/null || true)
  [ -n "$uuid" ] && KEEP=1 TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture-noskip-reload.json" 1 -Dmm.st01.d2.phase=reload -Dmm.st01.d2.uuid=$uuid -Dmm.st01.d2.at=$at
fi
# 2. fixture, vanilla-default skip_existing_chunks (supply exhaustion)
TAGTRADE=map_r20 run fixture 1004 "$O/fixture-skipdefault-create.json" 1 -Dmm.st01.d2.phase=create -Dmm.st01.d2.tradeUnderTest=map_r20
# 3. control: no datapack
run fixture 1004 "$O/fixture-control-nopack.json" 0 -Dmm.st01.d2.phase=create
# 4. overlap
for s in 1001 119599970 1541117449 914490140 1004 1034165336; do run overlap $s "$O/overlap-$s.json" 0; done
# 5. benchmark: radii x seeds, fresh world each (cold), second origin partially warm, 3 warm repeats
for s in 1001 1003 1004 1005 407538027 1421442253; do
  for r in 5 10 20 40; do PROBE_TIMEOUT=900 run bench $s "$O/bench-$s-r$r.json" 0 -Dmm.st01.d2.radius=$r "-Dmm.st01.d2.origins=0,0;6000,-6000" -Dmm.st01.d2.repeats=3 -Dmm.st01.d2.truth=$([ $r = 5 ] && echo true || echo false); done
done
# radius 100: explicit budget, two seeds only
for s in 1004 1003; do PROBE_TIMEOUT=1500 run bench $s "$O/bench-$s-r100.json" 0 -Dmm.st01.d2.radius=100 "-Dmm.st01.d2.origins=0,0" -Dmm.st01.d2.repeats=1; done
# 6. incremental (N2/N3) vs vanilla cold on the same inputs
for s in 1004 1001 1003; do for r in 5 20; do run incr $s "$O/incr-$s-r$r.json" 0 -Dmm.st01.d2.radius=$r "-Dmm.st01.d2.origins=0,0"; done; done
echo ALL_DONE > "$O/ALL_DONE"

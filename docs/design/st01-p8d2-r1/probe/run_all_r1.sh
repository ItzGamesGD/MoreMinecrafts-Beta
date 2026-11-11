#!/usr/bin/env bash
# Sequential on purpose (timings must not contend). wtA = 04a44ffa + the unchanged P8-D2 probe; wtB = 04a44ffa + disposable instrumentation + St01R1Probe.
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd); R="$here/run_r1.sh"; A=/tmp/r1/wtA; B=/tmp/r1/wtB; O=/tmp/r1/out; mkdir -p "$O"
run() { "$R" "$@" || echo "INCOMPLETE: $*" >> "$O/incomplete.txt"; }
S="1001 1003 1004 1005 407538027 1421442253"
# 1. exact reproduction of the P8-D2 benchmark (same probe, same arguments)
for s in $S; do for r in 5 10 20 40; do run $A $s "$O/A-bench-$s-r$r.json" -Dmm.st01.d2.out=$O/A-bench-$s-r$r.json -Dmm.st01.d2.mode=bench -Dmm.st01.d2.radius=$r "-Dmm.st01.d2.origins=0,0;6000,-6000" -Dmm.st01.d2.repeats=3; done; done
# 2. instrumented vanilla locator (same call), cold origin only
for s in $S; do for r in 5 10 20 40; do run $B $s "$O/B-bench-$s-r$r.json" -Dmm.st01.instr=true -Dmm.st01.r1.out=$O/B-bench-$s-r$r.json -Dmm.st01.r1.mode=bench -Dmm.st01.r1.radius=$r -Dmm.st01.r1.origins=0,0 -Dmm.st01.r1.repeats=1; done; done
# 3. eligibility comparison over every anchor (no chunk generation)
for s in $S; do run $B $s "$O/B-evalcmp-$s.json" -Dmm.st01.r1.out=$O/B-evalcmp-$s.json -Dmm.st01.r1.mode=evalcmp; done
# 4. cheap-first locator (experimental eligibility) and the same loop with production eligibility
for s in $S; do for r in 10 20 40; do run $B $s "$O/B-cheap-exp-$s-r$r.json" -Dmm.st01.instr=true -Dmm.st01.r1.out=$O/B-cheap-exp-$s-r$r.json -Dmm.st01.r1.mode=cheaplocate -Dmm.st01.r1.elig=exp -Dmm.st01.r1.radius=$r -Dmm.st01.r1.origins=0,0; done
  run $B $s "$O/B-cheap-prod-$s-r20.json" -Dmm.st01.instr=true -Dmm.st01.r1.out=$O/B-cheap-prod-$s-r20.json -Dmm.st01.r1.mode=cheaplocate -Dmm.st01.r1.elig=prod -Dmm.st01.r1.radius=20 -Dmm.st01.r1.origins=0,0; done
echo done > "$O/ALL_DONE"

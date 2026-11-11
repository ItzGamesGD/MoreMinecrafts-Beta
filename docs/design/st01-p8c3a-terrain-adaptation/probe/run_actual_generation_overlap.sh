#!/usr/bin/env bash
# Pre-scan of real Policy B generation (worktree /tmp/p3/wtb): vanilla blocks left inside the footprint after the actual ST-01 start generated.
export JAVA_HOME=/opt/jdk25/jdk-25.0.5+8; export PATH=$JAVA_HOME/bin:$PATH
R=/home/user/MoreMinecrafts-Beta/docs/design/st01-p8d2-r1/probe/run_r1.sh; W=/tmp/p3/wtb; O=/tmp/p3/out
run() { $R $W $1 $O/actualB-$1.json -Dmm.st01.c3a.out=$O/actualB-$1.json -Dmm.st01.c3a.mode=correction "-Dmm.st01.c3a.targets=$2" || echo "INCOMPLETE actualB $1" >> $O/incomplete.txt; }
run 1004 "-9416,-7576,72"
run 1001 "-6360,-2872,74"
run 914490140 "7368,3848,68"
run 1541117449 "-9976,-2248,75"
run 527213223 "-9160,-2376,65"
run 287862831 "-7976,-5992,70"
echo done > $O/ACTUAL_DONE

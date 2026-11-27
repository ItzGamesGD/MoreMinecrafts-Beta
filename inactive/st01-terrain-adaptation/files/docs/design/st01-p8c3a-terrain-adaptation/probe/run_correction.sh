#!/usr/bin/env bash
export JAVA_HOME=/opt/jdk25/jdk-25.0.5+8; export PATH=$JAVA_HOME/bin:$PATH
R=/home/user/MoreMinecrafts-Beta/docs/design/st01-p8d2-r1/probe/run_r1.sh; W=/tmp/p3/wt; O=/tmp/p3/out
run() { $R $W $1 $O/corr-$1.json -Dmm.st01.c3a.out=$O/corr-$1.json -Dmm.st01.c3a.mode=correction "-Dmm.st01.c3a.targets=$2" || echo "INCOMPLETE corr $1" >> $O/incomplete.txt; }
run 1001 "776,-6936,69"
run 1004 "-9416,-7576,72"
run 287862831 "-7976,-5992,70"
run 527213223 "-9160,-2376,65;9960,-7352,65"
run 914490140 "7368,3848,68"
run 1034165336 "4216,-3848,74"
run 1541117449 "-9976,-2248,75"
echo done > $O/CORR_DONE

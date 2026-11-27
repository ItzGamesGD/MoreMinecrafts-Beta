#!/usr/bin/env bash
R=/home/user/MoreMinecrafts-Beta/docs/design/st01-p8d2-r1/probe/run_r1.sh; W=/tmp/p3/wt; O=/tmp/p3/out
SEEDS="1001 1002 1003 1004 1005 527213223 1034165336 1541117449 2048069562 407538027 914490140 1421442253 1928394366 287862831 794814944 119599970 218938829 318277688 417616547 516955406"
for s in $SEEDS; do $R $W $s $O/cov-$s.json -Dmm.st01.c3a.out=$O/cov-$s.json -Dmm.st01.c3a.mode=coverage || echo "INCOMPLETE cov $s" >> $O/incomplete.txt; done
for s in 1001 1003 1004 1005 407538027 1421442253; do $R $W $s $O/search-$s-r20.json -Dmm.st01.c3a.out=$O/search-$s-r20.json -Dmm.st01.c3a.mode=search -Dmm.st01.c3a.radius=20 || echo "INCOMPLETE search $s" >> $O/incomplete.txt; done
echo done > $O/COV_DONE

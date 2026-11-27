#!/usr/bin/env bash
# Disposable Policy B experiment: production pipeline with only the three St01PlacementConfig limits set to 40/24/32 (worktree /tmp/p3/wtb, never committed)
export JAVA_HOME=/opt/jdk25/jdk-25.0.5+8; export PATH=$JAVA_HOME/bin:$PATH
cd /tmp/p3/wtb
for s in 1004 1001; do
  ( time tools/st01-p8c2b/run_validation.sh $s 10240 ) > /tmp/p3/out/polb-$s.log 2>&1; echo "seed $s rc=$?" >> /tmp/p3/out/polb_chain.log
done
echo done >> /tmp/p3/out/polb_chain.log

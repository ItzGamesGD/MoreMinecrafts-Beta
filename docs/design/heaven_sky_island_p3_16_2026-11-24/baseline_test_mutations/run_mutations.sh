#!/bin/bash
cd /home/user/MoreMinecrafts-Beta; . /tmp/claude-0/-home-user/032b748b-711d-55e7-a4e1-d86e3cf64fba/scratchpad/env.sh
W=src/main/resources/data/moreminecrafts/worldgen; T=src/test/java/dev/itzgamesgd/moreminecrafts/heaven/HeavenP38ResourceTest.java
cp $T /tmp/test_patched.java; git show 2b182844:$T > /tmp/test_candidate.java
OUT=/tmp/p316_mut_results.txt; : > $OUT
apply() { case $1 in
  M1) printf ' ' >> $W/template_pool/sky_islands/snowy/start.json;;
  M2) printf ' ' >> $W/template_pool/sky_islands/meadow/start.json;;
  M3) echo '{}' > $W/structure/rogue_new.json;;
  M4b) printf ' ' >> $W/structure_set/st03_pillager_headquarters.json;;
  M4x) python3 - <<PY
p="$W/structure/st03_pillager_headquarters.json"; s=open(p).read(); i=s.index('{'); open(p,'w').write(s[:i+1]+'"leak":"sky_island_meadow",'+s[i+1:])
PY
  ;;
  M5) rm $W/structure_set/st03_pillager_headquarters.json;;
  M7) echo '{}' > $W/structure/st03_extra.json;;
  control) :;;
esac; }
run() { # version case
  cp /tmp/test_$1.java $T; apply $2
  ./gradlew --no-daemon test --tests 'dev.itzgamesgd.moreminecrafts.heaven.HeavenP38ResourceTest.liveWorldgen*' > /tmp/mut_$1_$2.log 2>&1; rc=$?
  echo "$1 $2 gradle_exit=$rc $( [ $rc -eq 0 ] && echo PASS || echo FAIL )  $(grep -o 'is not a baseline file\|must not reference sky-island content\|must be valid JSON\|only the approved pools switched\|live pool == pending[^"]*' /tmp/mut_$1_$2.log | head -1)" >> $OUT
  git checkout -q -- $W; git clean -fdq $W
}
for c in control M1 M2 M3 M4b M4x M5 M7; do run candidate $c; done
for c in control M1 M4b M4x M5 M7; do run patched $c; done
cp /tmp/test_patched.java $T
echo DONE >> $OUT

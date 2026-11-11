#!/usr/bin/env bash
# Production non-regression evidence for the audit branch. Usage: nonregression.sh <production-base-sha> (default 9e4379c7)
# Fails if any production file, resource or NBT asset differs from the verified P8-C2-B baseline, or if a safety bypass appears.
set -euo pipefail
base=${1:-9e4379c7}
cd "$(dirname "$0")/../.."
echo "== diff of production source/resources vs $base (must be empty) =="
changed=$(git diff --name-only "$base" -- src/main; git status --porcelain -- src/main)
if [ -n "$changed" ]; then echo "FAIL production files changed:"; echo "$changed"; exit 1; fi
echo "PASS src/main identical to $base"
echo "== files changed outside src/main =="
git diff --stat "$base" | cat
echo "== build.gradle change must be only the diagnostic run configuration =="
extra=$(git diff -U0 "$base" -- build.gradle | grep '^[+-]' | grep -v '^+++\|^---' | grep -v 'st01CounterfactualProbe\|server()\|name = "ST-01 P8-C2-B Counterfactual\|source sourceSets.gametest\|runDir = "build/st01-p8c2b-audit\|ideConfigGenerated false\|^+ *}\|^+ *// ST-01 P8-C2-B counterfactual' || true)
if [ -n "$extra" ]; then echo "FAIL unexpected build.gradle change:"; echo "$extra"; exit 1; fi
echo "PASS build.gradle: run configuration only"
echo "== NBT asset hashes =="
sha256sum src/main/resources/data/moreminecrafts/structure/pyramid/st01/assembly_east.nbt src/main/resources/data/moreminecrafts/structure/pyramid/st01/assembly_west.nbt \
  src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt src/main/resources/data/moreminecrafts/structure/pyramid/st01/d1_seam_exception.nbt | tee /tmp/nbt.sha
grep -q fe5afb19c2587f1678f2947040c43aec67503b60380599f45934dd6034d6616c /tmp/nbt.sha && grep -q 55cb285c5a727a5268c708d8fe9ab4ece9f56e995922c1776d936ce2139c2818 /tmp/nbt.sha \
 && grep -q 7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423 /tmp/nbt.sha && grep -q 72f32c062daecb018d4184910e9d11c980b864c527af0383e50178dcae521b89 /tmp/nbt.sha
echo "PASS the four NBT assets match the P8-C2-A hashes"
echo "== no production configuration switch or bypass for entrance/fluid safety in src/main =="
if grep -rniE 'disable.?(entrance|fluid)|skip.?(entrance|fluid)|bypass|counterfactual' src/main/java/dev/itzgamesgd/moreminecrafts/pyramid; then echo "FAIL suspicious token in production"; exit 1; fi
echo "PASS no bypass/switch tokens in production pyramid code"
echo "== diagnostic classes are not on the production classpath (gametest source set only) =="
test ! -e src/main/java/dev/itzgamesgd/moreminecrafts/worldgenprobe && ! grep -rl "St01CounterfactualProbe" src/main
echo "PASS probe lives only in src/gametest"
echo "NONREGRESSION_STATIC PASS"

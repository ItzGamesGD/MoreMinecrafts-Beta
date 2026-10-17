#!/usr/bin/env bash
# Full probe matrix. Sequential on purpose (fair gen-time comparison). ~1 h on 4 cores.
# Usage: JAVA_HOME=<jdk25> tools/worldgen-probe/run_matrix.sh [samples]
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
samples=${1:-12}
SEEDS=(1 42 1337 20261017 -8675309)
fail=0
run() { "$here/run_probe.sh" "$@" || fail=$((fail+1)); }
# A) main matrix: shell x adaptation x height profile x seed
for seed in "${SEEDS[@]}"; do
  for profile in mid deep high; do
    for shell in built bare; do
      for adapt in encapsulate none; do
        run "$adapt" "$profile" dense "$seed" "$samples" "$shell"
      done
    done
  done
done
# B) forced overlap with vanilla Trial Chambers (same start chunks / spacing / salt)
for seed in 1 42 1337; do
  for shell in built bare; do
    for adapt in encapsulate none; do
      run "$adapt" mid coincident "$seed" 10 "$shell"
    done
  done
done
# C) vanilla exclusion_zone infrastructure
for seed in 1 42 1337; do
  run encapsulate mid excluded "$seed" "$samples" built
done
# D) control: same heights as "deep" but dimension_padding 0 (isolates the padding failure mode)
for seed in 1 42; do
  for shell in built bare; do
    for adapt in encapsulate none; do
      run "$adapt" deepnopad dense "$seed" "$samples" "$shell"
    done
  done
done
# E) add our structure to the VANILLA minecraft:trial_chambers structure_set (shared placement)
for seed in 1 42 1337; do
  run encapsulate mid shared "$seed" "$samples" built
done
# F) mountain-targeted: start chunks whose base terrain height >= 120 (searched out to 400 chunks)
for seed in 1 42 1337; do
  for profile in mid high; do
    for shell in built bare; do
      for adapt in encapsulate none; do
        PROBE_MIN_SURFACE=120 PROBE_SEARCH_RADIUS=400 run "$adapt" "$profile" densemtn "$seed" "$samples" "$shell"
      done
    done
  done
done
# G) same placements as A, but structure step = strongholds (placed AFTER vanilla geodes/fossils/monster rooms)
for seed in "${SEEDS[@]}"; do
  for profile in mid high; do
    for shell in built bare; do
      run encapsulate "$profile" denselate "$seed" "$samples" "$shell"
    done
  done
done
echo "matrix done, failed cells: $fail"

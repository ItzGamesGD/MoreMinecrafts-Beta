#!/usr/bin/env bash
# P3.9 rollback rehearsal on a COPY of the live worldgen JSONs (the repo's production pools are never touched).
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd); tmp=$(mktemp -d); cd "$root"
mkdir -p "$tmp/src/main/resources/data/moreminecrafts"
cp -a src/main/resources/data/moreminecrafts/worldgen "$tmp/src/main/resources/data/moreminecrafts/"
mkdir -p "$tmp/src/main/resources/data/moreminecrafts/tags"; cp -a src/main/resources/data/moreminecrafts/tags/worldgen "$tmp/src/main/resources/data/moreminecrafts/tags/"
export HEAVEN_P38_LIVE="$tmp/src/main/resources/data/moreminecrafts/worldgen/template_pool/sky_islands"
check() { (cd "$tmp" && sha256sum -c "$root/tools/heaven-p38/worldgen_baseline.sha256" 2>&1 | grep -v ': OK$' || true); }
echo "[1] copy vs baseline: $(check | wc -l) deviations"
python3 tools/heaven-p38/pending_pools.py apply all | tail -1
echo "[2] after apply: $(check | grep -c FAILED || true) changed files (expected 6 = meadow+forest x start/satellite/tower_top)"; check | grep -v 'WARNING' | head -8
python3 tools/heaven-p38/pending_pools.py revert all | tail -1
echo "[3] after revert: $(check | wc -l) deviations (expected 0)"; check
echo "[4] repo production pools vs baseline:"; sha256sum -c tools/heaven-p38/worldgen_baseline.sha256 2>&1 | grep -vc ': OK$' || true
rm -rf "$tmp"

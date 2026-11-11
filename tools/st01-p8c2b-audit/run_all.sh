#!/usr/bin/env bash
# Sequential local driver: every seed of seeds.json (the original P8-C2-B 20-seed set), <repeats> runs each, then analysis + independent verification.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd); repeats=${1:-1}
radius=$(python3 -I -c "import json;print(json.load(open('$root/tools/st01-p8c2b-audit/seeds.json'))['radius_blocks'])")
for s in $(python3 -I -c "import json;print(' '.join(map(str,json.load(open('$root/tools/st01-p8c2b-audit/seeds.json'))['seeds'])))"); do
  "$root/tools/st01-p8c2b-audit/run_audit.sh" "$s" "$radius" "$repeats"
done
python3 -I "$root/tools/st01-p8c2b-audit/analyze.py" "$root/build/st01-p8c2b-audit/results" "$root/build/st01-p8c2b-audit/analysis"
python3 -I "$root/tools/st01-p8c2b-audit/verify_independent.py" "$root/build/st01-p8c2b-audit/analysis"

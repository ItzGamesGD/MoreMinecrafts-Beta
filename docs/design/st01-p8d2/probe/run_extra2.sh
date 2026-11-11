#!/usr/bin/env bash
# Third evidence batch: the restart/persistence check redone after fixing a probe defect (the villager's chunk was not loaded/forced when the
# entity was added, so it was never saved; see VANILLA_EXPLORER_MAP_AUDIT.md "probe defects"), with map-storage inspection.
set -uo pipefail
root=$(cd "$(dirname "$0")/../../../.." && pwd); R="$root/docs/design/st01-p8d2/probe/run_probe.sh"; O="$root/build/st01-p8d2/out"
run() { "$R" "$@" || echo "INCOMPLETE: $*" >> "$O/incomplete.txt"; }
W="$root/build/st01-p8c2b/run/world"
TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture3-noskip-create.json" 1 -Dmm.st01.d2.phase=create -Dmm.st01.d2.tradeUnderTest=map_r20_noskip
{ echo "files under world/data after create+save:"; find "$W/data" -type f | sed "s#$W/##" | sort | head -60; echo "total_files $(find "$W/data" -type f | wc -l)"; echo "map-like files: $(find "$W" -type f -iname '*map*' | wc -l)"; find "$W" -type f -iname '*map*' -printf '%s bytes %p\n' | head -8; du -sb "$W/data"; } > "$O/fixture3-map-storage.txt" 2>&1
uuid=$(python3 -I -c "import json;print(json.load(open('$O/fixture3-noskip-create.json'))['persisted_villager']['uuid'])"); at=$(python3 -I -c "import json;print(json.load(open('$O/fixture3-noskip-create.json'))['persisted_villager']['at'])")
KEEP=1 TAGTRADE=map_r20_noskip run fixture 1004 "$O/fixture3-noskip-reload.json" 1 -Dmm.st01.d2.phase=reload -Dmm.st01.d2.uuid=$uuid -Dmm.st01.d2.at=$at
echo EXTRA2_DONE > "$O/EXTRA2_DONE"

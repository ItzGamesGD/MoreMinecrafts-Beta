#!/usr/bin/env bash
# P3.10: real client + dedicated server natural-spawn check. usage: run_realplayer.sh <packs csv or empty> <label> [sites csv]
set -euo pipefail
packs=${1:-}; label=$2; sites=${3:-meadow,meadow#2,meadow#3,forest,forest#1,forest#3,mountain_meadow}
root=$(cd "$(dirname "$0")/../.." && pwd); cd "$root"; mkdir -p build/heaven-worldgen/results build/heaven-worldgen/logs
out="$root/build/heaven-worldgen/results/realplayer_${label}.json"; rm -f "$out"; rm -rf build/run/clientGameTest
export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.json LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmoreminecrafts.heaven.p310=true -Dmoreminecrafts.heaven.p310packs=$packs -Dmm.heaven.sitefile=$root/build/heaven-worldgen/sites/c10.json -Dmm.heaven.p310.out=$out -Dmm.heaven.p310.sites=$sites" \
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" bash -c './gradlew runClientGameTest --args="--width 1920 --height 1080" --no-daemon > build/heaven-worldgen/logs/realplayer_'"$label"'.log 2>&1' || { echo FAILED; tail -30 "build/heaven-worldgen/logs/realplayer_$label.log"; }
test -s "$out" && python3 -I -c "import json,sys;d=json.load(open('$out'));print(d.get('packs'),d.get('error'));[print(k,json.dumps(v)[:400]) for k,v in d['sites'].items()]"

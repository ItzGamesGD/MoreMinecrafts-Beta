#!/usr/bin/env bash
# P3.13 real-client runs. usage: run_client_p313.sh explore|render
set -euo pipefail
mode=$1; root=$(cd "$(dirname "$0")/../.." && pwd); cd "$root"; mkdir -p build/heaven-worldgen/results build/heaven-worldgen/logs; rm -rf build/run/clientGameTest
export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.json LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
case $mode in
  explore) OPTS="-Dmoreminecrafts.heaven.p313explore=true -Dmm.heaven.sitefile=$root/build/heaven-worldgen/sites/c10.json -Dmm.heaven.p313.reference=$root/build/heaven-worldgen/results/h1_s1_full.json -Dmm.heaven.p313.out=$root/build/heaven-worldgen/results/p313_explore.json";;
  render) OPTS="-Dmoreminecrafts.heaven.p313render=true -Dmm.heaven.sitefile=$root/build/heaven-worldgen/sites/c10.json";;
esac
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $OPTS" xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" bash -c './gradlew runClientGameTest --args="--width 1920 --height 1080" --no-daemon > build/heaven-worldgen/logs/p313_'"$mode"'.log 2>&1' || echo "gradle exit nonzero (see log)"

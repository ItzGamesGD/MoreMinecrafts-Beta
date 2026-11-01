#!/usr/bin/env bash
# Stage 0-G: real Minecraft 26.3 CLIENT capture of the worlds built by Stage 0-F (software GL under Xvfb, Fabric client gametest).
# Usage: JAVA_HOME=<JDK25> tools/st05-worldgen-probe/stage0g/run_client_capture.sh [scenes.json]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd); cd "$root"
scenes=${1:-$root/tools/st05-worldgen-probe/stage0g/scenes.json}
run=$root/build/run/clientGameTest; mkdir -p "$run"; rm -rf "$run/screenshots"
# run ONLY the Stage 0-G capture test: temporarily narrow the client-gametest entrypoint list (restored on exit)
mod=$root/src/gametest/resources/fabric.mod.json; cp "$mod" "$mod.bak"; trap 'mv -f "$mod.bak" "$mod"' EXIT
python3 - "$mod" <<'PY'
import json,sys
d=json.load(open(sys.argv[1])); d["entrypoints"]["fabric-client-gametest"]=["dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.St05ArenaVisualClientGameTest"]; json.dump(d,open(sys.argv[1],"w"),indent=2)
PY
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
vk=$(find /usr/share/vulkan/icd.d -maxdepth 1 -name 'lvp_icd*.json' -print -quit 2>/dev/null || true); [ -n "$vk" ] && export VK_ICD_FILENAMES="$vk"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st05g.scenes=$scenes -Dmm.st05g.worlds=$root/build/st05-worldgen-probe/run -Xmx6g" xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" \
  ./gradlew runClientGameTest --args="--width 1920 --height 1080" --no-daemon --stacktrace 2>&1 | tee build/st05g-client-capture.log

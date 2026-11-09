#!/usr/bin/env bash
# ST-01 P8-A rendered-client capture: a real Minecraft 26.3 client under Xvfb (software GL) connected to a dedicated
# server that holds the committed royal_hall.nbt at its approved origin. Validation-only; does not change the structure.
# Output: build/st01-p8a-client/captures/*.png, build/st01-p8a-client/manifest.json, build/st01-p8a-client/logs/client.log
# Requires JAVA_HOME = JDK 25 and xvfb-run.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8a-client"
mkdir -p "$base/logs" "$base/captures"
manifest="$base/manifest.json"
rm -f "$manifest"
cd "$root"
export LIBGL_ALWAYS_SOFTWARE=1
export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
set +e
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" env \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.p8a.client=true -Dmm.st01.p8a.client.out=$manifest" \
  timeout "${CLIENT_TIMEOUT:-1500}" ./gradlew --no-daemon runClientGameTest --args="--width 1920 --height 1080" \
  > "$base/logs/client.log" 2>&1
rc=$?
set -e
echo "gradle rc=$rc"
# Copy every screenshot the client wrote for this run next to the manifest.
find "$root/build" "$root/run" -path "*screenshots*" -name "*.png" -newer "$base/logs/client.log" 2>/dev/null | head -0
find "$root" -path "*/screenshots/*.png" -newer "$root/tools/st01-p8/run_p8a_client_capture.sh" -not -path "*/.git/*" 2>/dev/null | while read -r f; do
  cp "$f" "$base/captures/"
done
ls "$base/captures" 2>/dev/null | head -40
if [ -s "$manifest" ]; then
  python3 -I -c "
import json
d = json.load(open('$manifest'))
print('captures:', len(d.get('captures', [])))
print('walk reached dais:', d.get('walk', {}).get('reached_dais_top'), 'ticks', d.get('walk', {}).get('ticks'), 'end', d.get('walk', {}).get('end'))
print('chest open:', d.get('chest_open'))
print('client:', d.get('client'))
"
else
  echo "no manifest written; see $base/logs/client.log"
  grep -n "ERROR\|Exception\|AssertionError\|FAIL" "$base/logs/client.log" | head -30
fi

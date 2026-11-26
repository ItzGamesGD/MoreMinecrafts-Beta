#!/usr/bin/env bash
# Lava Forgemaster in-game animation capture: real client (software renderer) -> per-skill PNG sequences -> mp4.
# Mirrors .github/workflows/sky-mob-bbmodel-capture.yml. Appearance/animation review only, not gameplay-feel validation.
# Needs: xvfb, openbox, mesa-vulkan-drivers (lavapipe), ffmpeg, JDK 25 on JAVA_HOME.
set -euo pipefail
OUT=${OUT:-build/forgemaster-capture}
mkdir -p "$OUT"
KEEP=$(mktemp)
cp src/gametest/resources/fabric.mod.json "$KEEP"
trap 'cp "$KEEP" src/gametest/resources/fabric.mod.json; rm -f "$KEEP"' EXIT
# Isolate the capture entrypoint exactly like the CI capture workflows do.
python3 - <<'PY'
import json
from pathlib import Path
p = Path('src/gametest/resources/fabric.mod.json')
d = json.loads(p.read_text())
d['entrypoints']['fabric-client-gametest'] = ['dev.itzgamesgd.moreminecrafts.boss.PiglinForgemasterClientCapture']
p.write_text(json.dumps(d, indent=2) + '\n')
PY
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmoreminecrafts.forgemaster.capture=true"
icd=$(find /usr/share/vulkan/icd.d -name 'lvp_icd*.json' -print -quit)
if [[ -n "$icd" ]]; then export VK_ICD_FILENAMES="$icd"; fi
rm -rf build/run/clientGameTest/screenshots
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
  openbox --sm-disable > '"$OUT"'/openbox.log 2>&1 &
  trap "kill $! 2>/dev/null || true" EXIT
  ./gradlew runClientGameTest --args="--width 1280 --height 720" --no-daemon --max-workers=2 2>&1 | tee '"$OUT"'/client.log
'
grep -q FORGEMASTER_CAPTURE_COMPLETE "$OUT/client.log"
# One mp4 per clip. Frames are 2 game ticks apart, so 10 fps is real time.
for clip in $(ls build/run/clientGameTest/screenshots | sed -E 's/^[0-9]+_forgemaster-(.*)-[0-9]{3}\.png$/\1/' | sort -u); do
  mkdir -p "$OUT/frames/$clip"
  i=0
  for f in $(ls build/run/clientGameTest/screenshots | grep -E "_forgemaster-${clip}-[0-9]{3}\.png$" | sort); do
    cp "build/run/clientGameTest/screenshots/$f" "$OUT/frames/$clip/$(printf '%03d' $i).png"; i=$((i+1))
  done
  ffmpeg -y -loglevel error -framerate 10 -i "$OUT/frames/$clip/%03d.png" -c:v libx264 -pix_fmt yuv420p -vf "scale=1280:720" "$OUT/forgemaster-$clip.mp4"
done
ls -la "$OUT"/*.mp4

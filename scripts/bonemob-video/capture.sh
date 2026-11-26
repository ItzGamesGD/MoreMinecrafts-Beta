#!/usr/bin/env bash
# In-game animation capture of Deep Sea Poseidon, Sky Wyvern and Floating Jellyfish: real client (software renderer)
# -> PNG sequences -> one mp4 per mob/clip. Same harness as scripts/forgemaster-video/capture.sh. Review only.
# Needs: xvfb, openbox, mesa-vulkan-drivers (lavapipe), ffmpeg, JDK 25 on JAVA_HOME.
set -euo pipefail
OUT=${OUT:-build/bonemob-capture}
mkdir -p "$OUT"
KEEP=$(mktemp)
cp src/gametest/resources/fabric.mod.json "$KEEP"
trap 'cp "$KEEP" src/gametest/resources/fabric.mod.json; rm -f "$KEEP"' EXIT
python3 - <<'PY'
import json
from pathlib import Path
p = Path('src/gametest/resources/fabric.mod.json')
d = json.loads(p.read_text())
d['entrypoints']['fabric-client-gametest'] = ['dev.itzgamesgd.moreminecrafts.boss.BoneMobClientCapture']
p.write_text(json.dumps(d, indent=2) + '\n')
PY
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmoreminecrafts.bonemob.capture=true"
icd=$(find /usr/share/vulkan/icd.d -name 'lvp_icd*.json' -print -quit)
if [[ -n "$icd" ]]; then export VK_ICD_FILENAMES="$icd"; fi
S=build/run/clientGameTest/screenshots
rm -rf "$S"
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
  openbox --sm-disable > '"$OUT"'/openbox.log 2>&1 &
  trap "kill $! 2>/dev/null || true" EXIT
  ./gradlew runClientGameTest --args="--width 1280 --height 720" --no-daemon --max-workers=2 2>&1 | tee '"$OUT"'/client.log
'
grep -q BONEMOB_CAPTURE_COMPLETE "$OUT/client.log"
for clip in $(ls "$S" | sed -nE 's/^[0-9]+_bonemob-(.*)-[0-9]{3}\.png$/\1/p' | sort -u); do
  mkdir -p "$OUT/frames/$clip"
  i=0
  for f in $(ls "$S" | grep -E "_bonemob-${clip}-[0-9]{3}\.png$" | sort); do
    cp "$S/$f" "$OUT/frames/$clip/$(printf '%03d' $i).png"; i=$((i+1))
  done
  ffmpeg -y -loglevel error -framerate 10 -i "$OUT/frames/$clip/%03d.png" -c:v libx264 -pix_fmt yuv420p "$OUT/$clip.mp4"
done
ls -la "$OUT"/*.mp4

#!/usr/bin/env bash
set -euo pipefail
out="$PWD/build/st03-natural/captures"
mkdir -p "$out"
plan=$(find "$PWD/build/st03-natural/survey" -name capture-plan.json -print -quit)
test -n "$plan"
cp "$plan" "$out/source-plan.json"
python3 - <<'PY'
import json
from pathlib import Path
p=Path('src/gametest/resources/fabric.mod.json')
d=json.loads(p.read_text())
d['entrypoints']['fabric-client-gametest']=['dev.itzgamesgd.moreminecrafts.pillagerhq.St03NaturalClientCapture']
p.write_text(json.dumps(d,indent=2)+'\n')
PY
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export JAVA_TOOL_OPTIONS="-Xmx2G -Dmm.st03.capture.plan=$plan -Dmm.st03.capture.out=$out"
icd=$(find /usr/share/vulkan/icd.d -name 'lvp_icd*.json' -print -quit)
if [[ -n "$icd" ]]; then export VK_ICD_FILENAMES="$icd"; fi
xvfb-run -a -s '-screen 0 1600x1000x24 +extension GLX +render -noreset' bash -euo pipefail -c '
  openbox --sm-disable > build/st03-natural/captures/openbox.log 2>&1 &
  trap "kill $! 2>/dev/null || true" EXIT
  timeout 3300 ./gradlew runClientGameTest --args="--width 1280 --height 720" --no-daemon --max-workers=2 2>&1 | tee build/st03-natural/captures/client.log
'
rg 'ST03_NATURAL_CAPTURE_COMPLETE 6' "$out/client.log"
python3 - <<'PY'
import hashlib,json
from pathlib import Path
from PIL import Image
out=Path('build/st03-natural/captures')
p=out/'manifest.json';manifest=json.loads(p.read_text())
assert manifest['status']=='PASS' and manifest['generator'].endswith('NoiseBasedChunkGenerator')
assert len(manifest['captures'])==6
for shot in manifest['captures']:
 path=out/shot['file'];shot['original_sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
 with Image.open(path) as image:
  assert image.size==(1280,720),(path,image.size)
  palette=image.convert('RGB').quantize(colors=256,method=Image.Quantize.MEDIANCUT)
 palette.save(path,optimize=True)
 with Image.open(path) as saved:assert saved.mode=='P' and saved.size==(1280,720)
 shot['palette_sha256']=hashlib.sha256(path.read_bytes()).hexdigest();shot['png_mode']='P';shot['size']=[1280,720]
 assert shot['natural_completion']['structure_completed'] and shot['natural_completion']['first_proximity_defenders']==40
# Keep the dedicated-server log as well as the Gradle/client combined log.
for source in sorted(Path('build').rglob('logs/latest.log')):
 if out in source.parents:continue
 text=source.read_text(errors='replace')
 if 'ST03_CAPTURE_GENERATOR' in text:
  (out/'server.log').write_text(text)
p.write_text(json.dumps(manifest,indent=2)+'\n')
print('PASS: six native 1280x720 palette PNGs from three actual natural HQs')
PY

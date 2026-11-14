#!/usr/bin/env bash
# Runs ONLY the Ancient Crystal Guardian real-client GameTest under Xvfb + Mesa llvmpipe and collects its captures.
# The gametest mod lists every client GameTest in one entrypoint array; this script temporarily narrows that array to the guardian
# test (restored on exit) so unrelated client tests are not executed. Output: build/crystal-guardian-evidence/
set -euo pipefail
cd "$(dirname "$0")/../.."
MOD_JSON=src/gametest/resources/fabric.mod.json
BACKUP="$(mktemp)"
cp "$MOD_JSON" "$BACKUP"
trap 'cp "$BACKUP" "$MOD_JSON"; rm -f "$BACKUP"' EXIT
python3 - "$MOD_JSON" <<'PY'
import json, sys
p = sys.argv[1]
d = json.load(open(p))
d["entrypoints"]["fabric-client-gametest"] = ["dev.itzgamesgd.moreminecrafts.test.crystalguardian.AncientCrystalGuardianClientGameTest"]
json.dump(d, open(p, "w"), indent=2)
PY
OUT=build/crystal-guardian-evidence
rm -rf "$OUT" build/run/clientGameTest/screenshots build/run/clientGameTest/crystal-guardian-evidence
mkdir -p "$OUT"
# Minecraft 26.x renders through Vulkan: use Mesa lavapipe (apt: mesa-vulkan-drivers) when present.
if [[ -f /usr/share/vulkan/icd.d/lvp_icd.json ]]; then export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.json; fi
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmoreminecrafts.crystalguardian.client=true"
WIDTH="${WIDTH:-1280}"; HEIGHT="${HEIGHT:-720}"
set +e
xvfb-run -a -s "-screen 0 ${WIDTH}x${HEIGHT}x24 +extension GLX +render -noreset" \
  ./gradlew runClientGameTest --args="--width ${WIDTH} --height ${HEIGHT}" --no-daemon --stacktrace --console=plain 2>&1 | tee "$OUT/client-gametest.log"
STATUS=${PIPESTATUS[0]}
set -e
cp -r build/run/clientGameTest/screenshots "$OUT/screenshots" 2>/dev/null || true
cp build/run/clientGameTest/crystal-guardian-evidence/pose-series.csv "$OUT/" 2>/dev/null || true
echo "gradle exit status: $STATUS"
exit "$STATUS"

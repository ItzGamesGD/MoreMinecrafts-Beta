#!/usr/bin/env bash
# ST-01 Phase 4 rendered-client capture of naturally generated starts of ONE seed: <seed> <scan.json>
# Real Minecraft 26.3 client under Xvfb (software GL) + dedicated server with the seed. Output: build/st01-phase4/client/<seed>/captures/*.png + manifest.json
# Requires JAVA_HOME = JDK 25, xvfb-run. Visual evidence only; asserts nothing about gameplay.
set -euo pipefail
seed=$1; scan=$2
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-phase4/client/$seed"
mkdir -p "$base/logs" "$base/captures"
targets=$(python3 -I - "$scan" "${MAX_TARGETS:-2}" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
rows = sorted(d["starts"].values(), key=lambda r: -(r["terrain_exhaustive_audit"]["exhaustive_cut"] + r["terrain_exhaustive_audit"]["exhaustive_skirt"]))
print(";".join(f"{r['anchor']['x']},{r['anchor']['y']},{r['anchor']['z']},{r['rotation']}" for r in rows[: int(sys.argv[2])]))
PY
)
echo "targets: $targets"
cd "$root"
rm -rf run/screenshots "$root/build/run/clientGameTest/screenshots" 2>/dev/null || true
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
set +e
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render +iglx -noreset" env \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.p4.client=true -Dmm.st01.p4.client.seed=$seed -Dmm.st01.p4.client.targets=$targets -Dmm.st01.p4.client.out=$base/manifest.json" \
  timeout "${CLIENT_TIMEOUT:-2400}" ./gradlew --no-daemon runClientGameTest --args="--width 1920 --height 1080" > "$base/logs/client.log" 2>&1
rc=$?
set -e
echo "gradle rc=$rc"
find "$root" -path "*/screenshots/*.png" -newer "$root/tools/st01-phase4/run_client_capture.sh" -not -path "*/.git/*" 2>/dev/null | while read -r f; do cp "$f" "$base/captures/"; done
ls "$base/captures" | head -40
test -s "$base/manifest.json" || { echo "no manifest; see $base/logs/client.log"; grep -n "ERROR\|Exception\|AssertionError" "$base/logs/client.log" | head -20; exit 1; }

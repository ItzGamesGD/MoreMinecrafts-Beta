#!/usr/bin/env bash
# ST-01 P8-A visual-first recapture: deterministic cameras (no automated walk) + direct PRIMARY chest open check.
# Same rendering setup as run_p8a_client_capture.sh: Xvfb + Mesa llvmpipe. Minecraft 26.3 requests an sRGB-capable
# OpenGL framebuffer, which Xvfb's GLX does not offer, so the client falls back to the Vulkan (lavapipe) backend:
# requires mesa-vulkan-drivers (as the CI client-smoke job installs). Validation-only; does not change the structure.
# Output: build/st01-p8a-client/captures-r2/*.png, build/st01-p8a-client/manifest_r2.json, build/st01-p8a-client/logs/client_r2.log
# Requires JAVA_HOME = JDK 25 and xvfb-run.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8a-client"
mkdir -p "$base/logs" "$base/captures-r2"
manifest="$base/manifest_r2.json"
rm -f "$manifest"
cd "$root"
export LIBGL_ALWAYS_SOFTWARE=1
export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-$(mktemp -d)}"
chmod 700 "$XDG_RUNTIME_DIR"
stamp=$(mktemp)
set +e
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" env \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.p8a.r2=true -Dmm.st01.p8a.r2.out=$manifest" \
  timeout "${CLIENT_TIMEOUT:-1500}" ./gradlew --no-daemon runClientGameTest --args="--width 1920 --height 1080" \
  > "$base/logs/client_r2.log" 2>&1
rc=$?
set -e
echo "gradle rc=$rc"
# Copy only this run's R2 screenshots (originals in captures/ are never touched).
find "$root/build/run" -path "*screenshots*" -name "*R2-*.png" -newer "$stamp" 2>/dev/null | while read -r f; do
  cp "$f" "$base/captures-r2/"
done
rm -f "$stamp"
ls "$base/captures-r2" | head -40
grep -n "Using graphics backend\|Failed to create backend" "$base/logs/client_r2.log" | head -5

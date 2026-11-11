#!/usr/bin/env bash
# ST-01 P8-C2-B rendered-client capture of NATURALLY generated pyramids: real Minecraft 26.3 client under Xvfb (software GL)
# connected to a dedicated server with normal world generation for the seed of <scan.json> (from run_validation.sh).
# Output: build/st01-p8c2b/client/{manifest.json,captures/*.png,client.log}. Requires JAVA_HOME = JDK 25 and xvfb-run.
set -euo pipefail
scan=$(realpath "$1"); max=${2:-2}
root=$(cd "$(dirname "$0")/../.." && pwd)
base="$root/build/st01-p8c2b/client"
rm -rf "$base"; mkdir -p "$base/captures"
stamp="$base/.start"; touch "$stamp"
cd "$root"
export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
set +e
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render +iglx -noreset" env \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st01.c2b.client=true -Dmm.st01.c2b.client.out=$base/manifest.json -Dmm.st01.c2b.client.scan=$scan -Dmm.st01.c2b.client.max=$max" \
  timeout "${CLIENT_TIMEOUT:-3000}" ./gradlew --no-daemon runClientGameTest --args="--width 1920 --height 1080" > "$base/client.log" 2>&1
rc=$?
set -e
echo "gradle rc=$rc"
find "$root" -path "*/screenshots/*st01-p8c2b-*.png" -newer "$stamp" -not -path "*/.git/*" -exec cp {} "$base/captures/" \;
ls "$base/captures" | sort
grep -h "ST01_P8C2B_CLIENT_CAPTURE_DONE" "$base/client.log" || { grep -n "ERROR\|Exception\|AssertionError" "$base/client.log" | head -20; exit 1; }

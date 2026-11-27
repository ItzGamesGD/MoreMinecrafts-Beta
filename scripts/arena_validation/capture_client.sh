#!/usr/bin/env bash
set -euo pipefail
index="${1:?camera index}"
results_dir=arena-validation-results
name="arena-camera-${index}.png"
read -r x y z yaw pitch < <(python3 - "$index" <<'PY'
import json, sys
camera = json.load(open('arena-validation-results/worldgen.json'))['cameras'][int(sys.argv[1])]
print(*(camera[k] for k in ('x', 'y', 'z', 'yaw', 'pitch')))
PY
)
export ARENA_CAMERA_YAW="$yaw" ARENA_CAMERA_PITCH="$pitch" ARENA_SCREENSHOT_NAME="$name"
export MOREMINECRAFTS_ARENA_VISUAL=1 LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name 'lvp_icd*.json' -print -quit)"
test -n "$vulkan_icd"
export VK_ICD_FILENAMES="$vulkan_icd"
export ARENA_CAMERA_X="$x" ARENA_CAMERA_Y="$y" ARENA_CAMERA_Z="$z"
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
  client_log="arena-validation-results/client-${ARENA_SCREENSHOT_NAME}.log"
  setsid timeout 300s ./gradlew --no-daemon runClient --args="--width 1280 --height 720 --username ArenaValidation" > "$client_log" 2>&1 &
  client_pid=$!
  cleanup() { kill -TERM -- "-$client_pid" 2>/dev/null || true; wait "$client_pid" 2>/dev/null || true; }
  trap cleanup EXIT
  joined=0
  for attempt in $(seq 1 120); do
    if grep -Fq ARENA_CLIENT_JOINED "$client_log"; then joined=1; break; fi
    kill -0 "$client_pid" || exit 1
    sleep 2
  done
  test "$joined" -eq 1
  python3 scripts/arena_validation/rcon.py "gamemode spectator ArenaValidation" "effect give ArenaValidation minecraft:night_vision infinite 0 true" "tp ArenaValidation $ARENA_CAMERA_X $ARENA_CAMERA_Y $ARENA_CAMERA_Z $ARENA_CAMERA_YAW $ARENA_CAMERA_PITCH"
  for attempt in $(seq 1 120); do
    if grep -Fq ARENA_CAPTURE_WRITTEN "$client_log"; then
      cp "run/screenshots/$ARENA_SCREENSHOT_NAME" "arena-validation-results/$ARENA_SCREENSHOT_NAME"
      exit 0
    fi
    if grep -Fq ARENA_CAPTURE_FAILED "$client_log"; then exit 1; fi
    kill -0 "$client_pid" || exit 1
    sleep 2
  done
  exit 1
'

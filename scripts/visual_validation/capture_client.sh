#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="${1:-visual-validation-results}"
SERVER_ADDRESS="${2:-127.0.0.1:25565}"
mkdir -p "$RESULTS_DIR"
touch "$RESULTS_DIR/screenshot-start.marker"

xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" bash -euo pipefail -c '
  results_dir="$1"
  server_address="$2"
  client_log="$results_dir/client.log"
  export LIBGL_ALWAYS_SOFTWARE=1
  export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
  vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name "lvp_icd*.json" -print -quit)"
  if [ -n "$vulkan_icd" ]; then
    export VK_ICD_FILENAMES="$vulkan_icd"
  fi
  export MOREMINECRAFTS_VISUAL_VALIDATION=1

  setsid timeout 180s ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--quickPlayMultiplayer $server_address --width 1280 --height 720 --username VisualValidation" > "$client_log" 2>&1 &
  client_pid=$!

  stop_client() {
    kill -TERM -- "-$client_pid" 2>/dev/null || true
    wait "$client_pid" 2>/dev/null || true
  }
  trap stop_client EXIT

  ready=0
  for attempt in $(seq 1 90); do
    if grep -Fq "VISUAL_VALIDATION_WORLD_READY" "$client_log"; then
      ready=1
      break
    fi
    if ! kill -0 "$client_pid" 2>/dev/null; then
      cat "$client_log"
      echo "Minecraft client exited before the test-world readiness signal."
      exit 1
    fi
    sleep 2
  done

  if [ "$ready" -ne 1 ]; then
    cat "$client_log"
    echo "Timed out waiting for the test-world readiness signal."
    exit 1
  fi

  # Let the first-person renderer settle after the explicit in-world readiness signal.
  sleep 3
  xdotool key --clearmodifiers F2

  screenshot=""
  for attempt in $(seq 1 30); do
    screenshot="$(find . -type f -path "*/screenshots/*.png" -newer "$results_dir/screenshot-start.marker" -print -quit)"
    if [ -n "$screenshot" ]; then
      break
    fi
    sleep 1
  done

  if [ -z "$screenshot" ]; then
    cat "$client_log"
    echo "Minecraft did not create an F2 screenshot."
    exit 1
  fi

  cp "$screenshot" "$results_dir/visual-world.png"
  printf "Captured Minecraft F2 screenshot from %s\n" "$screenshot" | tee -a "$client_log"
  echo "Minecraft client reported the test world ready and saved an F2 screenshot."
  stop_client
  trap - EXIT
' bash "$RESULTS_DIR" "$SERVER_ADDRESS"

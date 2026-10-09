#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="${1:-visual-validation-results}"
SERVER_ADDRESS="${2:-127.0.0.1:25565}"
mkdir -p "$RESULTS_DIR"
touch "$RESULTS_DIR/screenshot-start.marker"

xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" env VISUAL_RESULTS_DIR="$RESULTS_DIR" VISUAL_SERVER_ADDRESS="$SERVER_ADDRESS" bash -euo pipefail -c '
  results_dir="$VISUAL_RESULTS_DIR"
  server_address="$VISUAL_SERVER_ADDRESS"
  client_log="$results_dir/client.log"
  export LIBGL_ALWAYS_SOFTWARE=1
  export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
  vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name "lvp_icd*.json" -print -quit)"
  if [ -n "$vulkan_icd" ]; then
    export VK_ICD_FILENAMES="$vulkan_icd"
  fi
  export MOREMINECRAFTS_VISUAL_VALIDATION=1
  export MOREMINECRAFTS_VISUAL_SERVER_ADDRESS="$server_address"

  setsid timeout 180s ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username VisualValidation" > "$client_log" 2>&1 &
  client_pid=$!

  stop_client() {
    kill -TERM -- "-$client_pid" 2>/dev/null || true
    wait "$client_pid" 2>/dev/null || true
  }
  trap stop_client EXIT

  joined=0
  for attempt in $(seq 1 90); do
    if grep -Fq "VISUAL_VALIDATION_PLAYER_JOINED" "$client_log"; then
      joined=1
      break
    fi
    if ! kill -0 "$client_pid" 2>/dev/null; then
      cat "$client_log"
      echo "Minecraft client exited before the test-world readiness signal."
      exit 1
    fi
    sleep 2
  done

  if [ "$joined" -ne 1 ]; then
    cat "$client_log"
    echo "Timed out waiting for the client to join the test world."
    exit 1
  fi

  python3 scripts/visual_validation/rcon.py "tp VisualValidation 0.5 64 12.5"

  ready=0
  for attempt in $(seq 1 60); do
    if grep -Fq "VISUAL_VALIDATION_WORLD_READY" "$client_log"; then
      ready=1
      break
    fi
    sleep 2
  done
  if [ "$ready" -ne 1 ]; then
    cat "$client_log"
    echo "Timed out waiting for the test-world readiness signal after teleport."
    exit 1
  fi

  # Focus the actual Minecraft window, then request the game native F2 screenshot.
  window_ids="$(xdotool search --onlyvisible --name '.*' 2>/dev/null || true)"
  window_id="$(printf "%s\\n" "$window_ids" | tail -n 1)"
  if [ -z "$window_id" ]; then
    window_id="$(xdotool getactivewindow 2>/dev/null || true)"
  fi
  if [ -z "$window_id" ]; then
    window_tree="$(xwininfo -root -tree 2>&1 || true)"
    printf "%s\\n" "$window_tree" | tee -a "$client_log"
    window_id="$(printf "%s\\n" "$window_tree" | awk '/^[[:space:]]*0x[[:xdigit:]]+/ { print $1; exit }')"
  fi
  if [ -z "$window_id" ]; then
    cat "$client_log"
    echo "Could not locate an X11 window for F2 capture."
    exit 1
  fi
  window_title="$(xdotool getwindowname "$window_id" 2>/dev/null || true)"
  printf "Focusing visible X11 window %s (%s)\\n" "$window_id" "$window_title" | tee -a "$client_log"
  xdotool windowfocus --sync "$window_id"
  sleep 2
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
'

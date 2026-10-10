#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="${1:-visual-validation-results}"
SERVER_ADDRESS="${2:-127.0.0.1:25565}"
mkdir -p "$RESULTS_DIR"

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

  openbox --sm-disable > "$results_dir/openbox.log" 2>&1 &
  window_manager_pid=$!
  sleep 2
  echo "Xvfb display=$DISPLAY window_manager_pid=$window_manager_pid" | tee -a "$client_log"

  setsid timeout 240s ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username VisualValidation" > "$client_log" 2>&1 &
  client_pid=$!

  stop_client() {
    kill -TERM -- "-$client_pid" 2>/dev/null || true
    wait "$client_pid" 2>/dev/null || true
    kill "$window_manager_pid" 2>/dev/null || true
    wait "$window_manager_pid" 2>/dev/null || true
  }
  collect_x11_diagnostics() {
    if command -v xwininfo >/dev/null 2>&1; then
      xwininfo -root -tree > "$results_dir/x11-window-tree.txt" 2>&1 || true
    fi
    if command -v glxinfo >/dev/null 2>&1; then
      glxinfo -B > "$results_dir/graphics-backend.txt" 2>&1 || true
    fi
    if command -v vulkaninfo >/dev/null 2>&1; then
      vulkaninfo --summary > "$results_dir/vulkan-backend.txt" 2>&1 || true
    fi
  }
  trap "collect_x11_diagnostics; stop_client" EXIT

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

  capture_done=0
  for attempt in $(seq 1 90); do
    if grep -Fq "VISUAL_CAPTURE_WRITTEN" "$client_log"; then
      capture_done=1
      break
    fi
    if grep -Fq "VISUAL_CAPTURE_FAILED" "$client_log"; then
      cat "$client_log"
      echo "Minecraft native screenshot capture reported failure."
      exit 1
    fi
    if ! kill -0 "$client_pid" 2>/dev/null; then
      cat "$client_log"
      echo "Minecraft client exited before native screenshot capture completed."
      exit 1
    fi
    sleep 1
  done
  if [ "$capture_done" -ne 1 ]; then
    collect_x11_diagnostics
    cat "$client_log"
    echo "Timed out waiting for VISUAL_CAPTURE_WRITTEN or VISUAL_CAPTURE_FAILED."
    exit 1
  fi

  screenshot="$(sed -n "s/.*VISUAL_CAPTURE_WRITTEN path=//p" "$client_log" | tail -n 1 | sed "s/ bytes=.*//")"
  if [ -z "$screenshot" ] || [ ! -s "$screenshot" ]; then
    cat "$client_log"
    echo "Client-reported native screenshot path does not exist: $screenshot"
    exit 1
  fi
  cp "$screenshot" "$results_dir/visual-world.png"
  printf "Minecraft native Screenshot.grab via mainRenderTarget\nsource=%s\n" "$screenshot" > "$results_dir/capture-source.txt"
  echo "Copied native Minecraft screenshot from $screenshot" | tee -a "$client_log"

  latest_screenshot="$(ls -t run/screenshots/*.png | head -n 1)"
  previous_hash="$(sha256sum "$latest_screenshot" | cut -d " " -f 1)"
  xdotool key --clearmodifiers l
  sleep 2
  xdotool key F2
  sleep 3
  latest_screenshot="$(ls -t run/screenshots/*.png | head -n 1)"
  current_hash="$(sha256sum "$latest_screenshot" | cut -d " " -f 1)"
  if [ "$current_hash" = "$previous_hash" ]; then echo "Advancement selection frame was not captured."; exit 1; fi
  previous_hash="$current_hash"
  cp "$latest_screenshot" "$results_dir/selection-window.png"

  xdotool mousemove 640 348 click 1
  sleep 2
  xdotool key F2
  sleep 3
  latest_screenshot="$(ls -t run/screenshots/*.png | head -n 1)"
  current_hash="$(sha256sum "$latest_screenshot" | cut -d " " -f 1)"
  if [ "$current_hash" = "$previous_hash" ]; then echo "Knowledge screen frame was not captured."; exit 1; fi
  previous_hash="$current_hash"
  cp "$latest_screenshot" "$results_dir/knowledge-screen.png"

  xdotool mousemove 477 181 click 1
  sleep 1
  xdotool mousemove 640 374 click 1
  sleep 2
  xdotool key F2
  sleep 3
  latest_screenshot="$(ls -t run/screenshots/*.png | head -n 1)"
  current_hash="$(sha256sum "$latest_screenshot" | cut -d " " -f 1)"
  if [ "$current_hash" = "$previous_hash" ]; then echo "Research screen frame was not captured."; exit 1; fi
  previous_hash="$current_hash"
  cp "$latest_screenshot" "$results_dir/research-screen.png"

  # The scripted fixture equips an approved purchasable research row. The click sequence
  # below follows the visible paginated list and purchase button through normal screen widgets.
  python3 scripts/visual_validation/rcon.py \
    "execute as VisualValidation run mmresearch test crafting" \
    "xp add VisualValidation 1200 points" \
    "give VisualValidation minecraft:chest 8" \
    "give VisualValidation minecraft:iron_block 4" \
    "give VisualValidation minecraft:paper 64"
  for page in $(seq 1 8); do
    xdotool mousemove 623 481 click 1
    sleep 0.2
  done
  xdotool mousemove 543 276 click 1
  sleep 1
  xdotool mousemove 698 482 click 1
  sleep 3
  xdotool key F2
  sleep 3
  latest_screenshot="$(ls -t run/screenshots/*.png | head -n 1)"
  current_hash="$(sha256sum "$latest_screenshot" | cut -d " " -f 1)"
  if [ "$current_hash" = "$previous_hash" ]; then echo "Research purchase state frame was not captured."; exit 1; fi
  cp "$latest_screenshot" "$results_dir/research-purchase-state.png"
  printf "Minecraft vanilla keyScreenshot F2 captures
selection-window.png
knowledge-screen.png
research-screen.png
research-purchase-state.png
" > "$results_dir/ui-capture-source.txt"
  echo "CLIENT_INTERACTION_PATH advancement-key -> selection -> knowledge -> research -> purchase-button" | tee -a "$client_log"

  collect_x11_diagnostics
  stop_client
  trap - EXIT
'


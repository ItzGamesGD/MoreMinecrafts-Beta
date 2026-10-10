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
  export MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL=1

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
    for screenshot in run/screenshots/*.png; do
      if [ -s "$screenshot" ]; then cp "$screenshot" "$results_dir/$(basename "$screenshot")"; fi
    done
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

  python3 scripts/visual_validation/rcon.py \
    "execute as VisualValidation run mmresearch test crafting" \
    "xp add VisualValidation 1200 points" \
    "give VisualValidation minecraft:chest 8" \
    "give VisualValidation minecraft:iron_block 4" \
    "give VisualValidation minecraft:paper 64" \
    "tp VisualValidation 0.5 64 12.5"

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

  # Complete the vanilla movement tutorial so its toast does not cover progression details.
  xdotool keydown w
  sleep 0.25
  xdotool keyup w
  sleep 1

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

  click_selector_target() {
    local target="$1" ready_line x y window_id
    for attempt in $(seq 1 45); do
      ready_line="$(grep -F "SELECTOR_PHYSICAL_READY target=$target " "$client_log" | tail -n 1 || true)"
      if [ -n "$ready_line" ]; then break; fi
      if grep -Fq "SELECTOR_PHYSICAL_TEST_FAILED" "$client_log"; then
        cat "$client_log"; echo "Physical selector test failed before target=$target."; exit 1
      fi
      sleep 1
    done
    if [ -z "$ready_line" ]; then cat "$client_log"; echo "No physical-click target announced: $target"; exit 1; fi
    x="$(sed -n 's/.* x=\([0-9][0-9]*\) y=.*/\1/p' <<< "$ready_line")"
    y="$(sed -n 's/.* y=\([0-9][0-9]*\) gui_scale=.*/\1/p' <<< "$ready_line")"
    if [ -z "$x" ] || [ -z "$y" ]; then echo "Invalid physical-click coordinates: $ready_line"; exit 1; fi
    window_id="$(xdotool getactivewindow)"
    eval "$(xdotool getwindowgeometry --shell "$window_id")"
    xdotool mousemove --sync "$((X + x))" "$((Y + y))" click --clearmodifiers 1
    for attempt in $(seq 1 20); do
      if grep -Fq "SELECTOR_PHYSICAL_VERIFIED target=$target input=xdotool" "$client_log"; then
        echo "Physical mouse click verified: $target at window=($x,$y) root=($((X+x)),$((Y+y)))" | tee -a "$client_log"
        printf '%s window=%s local=%s,%s root=%s,%s\n' "$target" "$window_id" "$x" "$y" "$((X+x))" "$((Y+y))" >> "$results_dir/selector-physical-inputs.txt"
        return 0
      fi
      if grep -Fq "SELECTOR_PHYSICAL_TEST_FAILED" "$client_log" || grep -Fq "CLIENT_UI_INTERACTION_FAILED" "$client_log"; then
        cat "$client_log"; echo "Physical click did not activate target=$target."; exit 1
      fi
      sleep 1
    done
    cat "$client_log"
    echo "Timed out verifying physical click for target=$target."
    exit 1
  }

  for target in knowledge research advancements xp_quest close; do
    click_selector_target "$target"
  done

  ui_done=0
  for attempt in $(seq 1 120); do
    if grep -Fq "CLIENT_INTERACTION_PATH synthetic Screen button callbacks" "$client_log"; then
      ui_done=1
      break
    fi
    if grep -Eq "CLIENT_UI_INTERACTION_FAILED|CLIENT_UI_PURCHASE_FAILED|CLIENT_UI_SCREENSHOT_FAILED|SELECTOR_PHYSICAL_TEST_FAILED" "$client_log"; then
      cat "$client_log"
      echo "Minecraft progression UI interaction or purchase assertion failed."
      exit 1
    fi
    sleep 1
  done
  if [ "$ui_done" -ne 1 ]; then
    cat "$client_log"
    echo "Timed out waiting for the in-client progression purchase journey."
    exit 1
  fi
  for frame in progression-selection knowledge-screen research-screen research-purchasable-entry research-purchase-confirmation research-purchase-cancelled research-purchase-state auto-farm-screen; do
    screenshot="run/screenshots/${frame}.png"
    if [ ! -s "$screenshot" ]; then echo "Missing native Minecraft frame: $screenshot"; exit 1; fi
    cp "$screenshot" "$results_dir/${frame}.png"
  done
  printf "Minecraft Screenshot.grab via mainRenderTarget\nprogression-selection.png\nknowledge-screen.png\nresearch-screen.png\nresearch-purchasable-entry.png\nresearch-purchase-confirmation.png\nresearch-purchase-cancelled.png\nresearch-purchase-state.png\nauto-farm-screen.png (screen backed by real menu; empty client-side container is a synthetic render fixture)\n" > "$results_dir/ui-capture-source.txt"
  printf "Selector hitboxes exercised via xdotool/XTEST mouse clicks; research purchase journey uses synthetic Screen widget callbacks.\n" >> "$results_dir/ui-capture-source.txt"
  echo "CLIENT_INTERACTION_PATH synthetic Screen button callbacks: selector -> knowledge -> research -> purchase" | tee -a "$client_log"

  collect_x11_diagnostics
  stop_client
  trap - EXIT
'


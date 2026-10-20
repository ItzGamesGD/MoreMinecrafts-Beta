#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="${1:-visual-validation-results}"
SERVER_ADDRESS="${2:-127.0.0.1:25565}"
VISUAL_USERNAME="${3:-VisualValidation}"
mkdir -p "$RESULTS_DIR"

xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" env VISUAL_RESULTS_DIR="$RESULTS_DIR" VISUAL_SERVER_ADDRESS="$SERVER_ADDRESS" VISUAL_USERNAME="$VISUAL_USERNAME" bash -euo pipefail -c '
  results_dir="$VISUAL_RESULTS_DIR"
  server_address="$VISUAL_SERVER_ADDRESS"
  username="$VISUAL_USERNAME"
  client_log="$results_dir/client.log"
  source scripts/visual_validation/x11_input.sh
  export LIBGL_ALWAYS_SOFTWARE=1
  export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
  vulkan_icd=""
  if [ -d /usr/share/vulkan/icd.d ]; then
    vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name "lvp_icd*.json" -print -quit)"
  fi
  if [ -n "$vulkan_icd" ]; then
    export VK_ICD_FILENAMES="$vulkan_icd"
  fi
  export MOREMINECRAFTS_VISUAL_VALIDATION=1
  export MOREMINECRAFTS_VISUAL_SERVER_ADDRESS="$server_address"
  # Minecraft 26.3 SDL client is not discoverable as a visible X11 top-level
  # window on this runner. Keep physical selector testing opt-in; the in-client
  # Screen callback journey validates selector and progression navigation here.

  openbox --sm-disable > "$results_dir/openbox.log" 2>&1 &
  window_manager_pid=$!
  sleep 2
  echo "Xvfb display=$DISPLAY window_manager_pid=$window_manager_pid" | tee -a "$client_log"

  setsid timeout 240s ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username $username" > "$client_log" 2>&1 &
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
    {
      echo "DISPLAY=$DISPLAY"
      echo "X11 root:"
      xwininfo -root -int 2>&1 || true
      echo "Active window: $(xdotool getactivewindow 2>&1 || true)"
      echo "Focused window: $(xdotool getwindowfocus 2>&1 || true)"
      echo "Root active property: $(xprop -root _NET_ACTIVE_WINDOW 2>&1 || true)"
      echo "Visible X11 windows (id, name, WM_CLASS, PID, process, geometry):"
      for candidate in $(xdotool search --onlyvisible --name ".*" 2>/dev/null || true); do
        candidate_name="$(xdotool getwindowname "$candidate" 2>&1 || true)"
        candidate_class="$(x11_window_property "$candidate" WM_CLASS)"
        candidate_pid="$(x11_window_property "$candidate" _NET_WM_PID)"
        candidate_process="$(ps -p "$candidate_pid" -o comm= 2>/dev/null || true)"
        candidate_geometry="$(xdotool getwindowgeometry --shell "$candidate" 2>&1 || true)"
        printf "id=%s name=%q wm_class=%q pid=%q process=%q geometry=%q\n" \
          "$candidate" "$candidate_name" "$candidate_class" "$candidate_pid" "$candidate_process" "$candidate_geometry"
      done
      echo "Recent Minecraft client log:"
      tail -n 160 "$client_log" 2>&1 || true
    } > "$results_dir/x11-input-diagnostics.txt" 2>&1
    if command -v glxinfo >/dev/null 2>&1; then
      glxinfo -B > "$results_dir/graphics-backend.txt" 2>&1 || true
    fi
    if command -v vulkaninfo >/dev/null 2>&1; then
      vulkaninfo --summary > "$results_dir/vulkan-backend.txt" 2>&1 || true
    fi
  }
  trap "collect_x11_diagnostics; stop_client" EXIT

  fail_selector_input() {
    echo "SELECTOR_PHYSICAL_INPUT_FAILED reason=$*" | tee -a "$client_log"
    collect_x11_diagnostics
    cat "$results_dir/x11-input-diagnostics.txt"
    exit 1
  }

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
    "execute as $username run mmresearch test purchase" \
    "execute as $username run mmresearch test crafting" \
    "xp add $username 1200 points" \
    "give $username minecraft:chest 8" \
    "give $username minecraft:iron_block 4" \
    "give $username minecraft:paper 64" \
    "tp $username 0.5 64 12.5"

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
    local target="$1" ready_line x y resolution render_width render_height event_count_before event_count_after
    local pointer_geometry pointer_x pointer_y pointer_window attempt
    for attempt in $(seq 1 45); do
      ready_line="$(grep -F "SELECTOR_PHYSICAL_READY target=$target " "$client_log" | tail -n 1 || true)"
      if [ -n "$ready_line" ]; then break; fi
      if grep -Fq "SELECTOR_PHYSICAL_TEST_FAILED" "$client_log"; then
        cat "$client_log"; echo "Physical selector test failed before target=$target."; exit 1
      fi
      sleep 1
    done
    if [ -z "$ready_line" ]; then fail_selector_input "no target announced: $target"; fi
    x="${ready_line#* x=}"
    x="${x%% *}"
    y="${ready_line#* y=}"
    y="${y%% *}"
    resolution="${ready_line##* resolution=}"
    render_width="${resolution%%x*}"
    render_height="${resolution#*x}"
    if [[ ! "$x" =~ ^[0-9]+$ || ! "$y" =~ ^[0-9]+$ \
          || ! "$render_width" =~ ^[1-9][0-9]*$ || ! "$render_height" =~ ^[1-9][0-9]*$ ]]; then
      fail_selector_input "invalid render coordinates: $ready_line"
    fi
    if ! discover_minecraft_window; then
      fail_selector_input "could not uniquely identify Minecraft X11 window"
    fi
    echo "Minecraft X11 window id=$X11_GAME_WINDOW_ID wm_class=$X11_GAME_WINDOW_CLASS pid=$X11_GAME_WINDOW_PID geometry=${X11_GAME_WINDOW_X},${X11_GAME_WINDOW_Y} ${X11_GAME_WINDOW_WIDTH}x${X11_GAME_WINDOW_HEIGHT}"
    if ! activate_minecraft_window; then
      fail_selector_input "focus/active mismatch target_id=$X11_GAME_WINDOW_ID active=$X11_GAME_ACTIVE_WINDOW focused=$X11_GAME_FOCUSED_WINDOW activate_status=$X11_WINDOWACTIVATE_STATUS focus_status=$X11_WINDOWFOCUS_STATUS"
    fi
    if ! x11_transform_coordinates "$x" "$y" "$render_width" "$render_height"; then
      fail_selector_input "render-to-window coordinate transform failed target=$x,$y render=${render_width}x${render_height} window=${X11_GAME_WINDOW_WIDTH}x${X11_GAME_WINDOW_HEIGHT}"
    fi
    event_count_before="$(grep -Fc "SELECTOR_MOUSE_EVENT" "$client_log" || true)"
    echo "Physical input target=$target window_id=$X11_GAME_WINDOW_ID local=($X11_CLICK_LOCAL_X,$X11_CLICK_LOCAL_Y) root=($X11_CLICK_ROOT_X,$X11_CLICK_ROOT_Y) active=$X11_GAME_ACTIVE_WINDOW focus=$X11_GAME_FOCUSED_WINDOW"
    xdotool mousemove --sync "$X11_CLICK_ROOT_X" "$X11_CLICK_ROOT_Y"
    xdotool click --clearmodifiers 1
    pointer_geometry="$(xdotool getmouselocation --shell 2>/dev/null || true)"
    pointer_x="$(x11_geometry_value "$pointer_geometry" X)"
    pointer_y="$(x11_geometry_value "$pointer_geometry" Y)"
    pointer_window="$(x11_geometry_value "$pointer_geometry" WINDOW)"
    echo "Physical pointer after click=($pointer_x,$pointer_y) window=$pointer_window focus=$(xdotool getwindowfocus 2>/dev/null || true)"
    for attempt in $(seq 1 20); do
      event_count_after="$(grep -Fc "SELECTOR_MOUSE_EVENT" "$client_log" || true)"
      if (( event_count_after > event_count_before )) \
          && grep -Fq "SELECTOR_PHYSICAL_VERIFIED target=$target input=xdotool" "$client_log"; then
        echo "Physical mouse event and screen transition verified: $target window_id=$X11_GAME_WINDOW_ID" | tee -a "$client_log"
        printf "%s window=%s wm_class=%s pid=%s local=%s,%s root=%s,%s focus=%s active=%s mouse_event_count=%s\n" \
          "$target" "$X11_GAME_WINDOW_ID" "$X11_GAME_WINDOW_CLASS" "$X11_GAME_WINDOW_PID" \
          "$X11_CLICK_LOCAL_X" "$X11_CLICK_LOCAL_Y" "$X11_CLICK_ROOT_X" "$X11_CLICK_ROOT_Y" \
          "$X11_GAME_FOCUSED_WINDOW" "$X11_GAME_ACTIVE_WINDOW" "$event_count_after" >> "$results_dir/selector-physical-inputs.txt"
        return 0
      fi
      if grep -Fq "SELECTOR_PHYSICAL_TEST_FAILED" "$client_log" || grep -Fq "CLIENT_UI_INTERACTION_FAILED" "$client_log"; then
        fail_selector_input "Minecraft rejected physical click target=$target"
      fi
      sleep 1
    done
    fail_selector_input "timed out waiting for Minecraft mouseClicked and screen transition target=$target event_count_before=$event_count_before event_count_after=$event_count_after"
  }

  if [[ "${MOREMINECRAFTS_VISUAL_SELECTOR_PHYSICAL:-0}" == "1" ]]; then
    # The first click is isolated: later selector actions run only after Minecraft logs
    # mouseClicked and confirms the knowledge screen transition.
    click_selector_target knowledge
    echo "First physical selector click passed; continuing staged interactions."
    for target in research advancements xp_quest close; do
      click_selector_target "$target"
    done
  else
    echo "Physical selector click path is opt-in; the in-client Screen callback journey is active."
  fi

  ui_done=0
  for attempt in $(seq 1 120); do
    if grep -Fq "CLIENT_INTERACTION_PATH synthetic Screen callbacks: selector -> both sector menus -> node hover -> one-click server purchase" "$client_log"; then
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
  gui_scale="$(sed -n 's/^guiScale://p' run/options.txt | tail -n 1)"
  required_frames=(progression-selection selector-xp-hover selector-xp-focus selector-xp-quest-opened-by-keyboard selector-normal-final xp-quest-tree-compact xp-quest-tree-zoomed-out xp-quest-tooltip-cave xp-quest-bestiary-tooltip xp-quest-bestiary-scrolled xp-quest-habitats-tooltip xp-quest-keyboard-focus-tooltip xp-quest-tooltip-edge-left xp-quest-tooltip-edge-right xp-quest-tooltip-edge-top xp-quest-tooltip-edge-bottom xp-quest-selected-tooltip structure-preview-initial structure-preview-overview structure-preview-index-card-click structure-preview-medium-zoom-exploration structure-preview-mid-zoom structure-preview-dense-exploration structure-preview-dense-mining structure-preview-sparse-ecology structure-preview-zoomed-in structure-preview-pan-farming structure-preview-pan-bosses structure-preview-tooltip-long-label structure-preview-tooltip-groups structure-preview-tooltip-scroll-list structure-preview-tooltip-scrolled structure-preview-keyboard-navigation structure-preview-mouse-prepare-far-node structure-preview-mouse-click-far-node structure-preview-regression-tooltip-open structure-preview-regression-stale-tooltip structure-preview-regression-stale-click structure-preview-regression-keyboard-tooltip structure-preview-regression-obstruction-prepare structure-preview-regression-obstructed-dismiss structure-preview-regression-obstructed-click structure-preview-regression-keyboard-resized structure-preview-zoom-roundtrip structure-preview-selected-exploration-left structure-preview-selected-exploration-right structure-preview-selected-exploration-top structure-preview-selected-exploration-bottom structure-preview-selected-ecology-left structure-preview-selected-ecology-right structure-preview-selected-ecology-top structure-preview-selected-ecology-bottom structure-preview-tooltip-edge-left structure-preview-tooltip-edge-right structure-preview-tooltip-edge-top structure-preview-tooltip-edge-bottom knowledge-sector-selection knowledge-branch-menu knowledge-screen research-sector-selection research-screen research-branch-menu knowledge-node-tooltip research-node-tooltip research-zoomed research-purchasable-entry research-purchase-state research-unavailable-tooltip auto-farm-screen)
  for frame in "${required_frames[@]}"; do
    screenshot="run/screenshots/${frame}.png"
    if [ ! -s "$screenshot" ]; then echo "Missing native Minecraft frame: $screenshot"; exit 1; fi
    cp "$screenshot" "$results_dir/${frame}.png"
  done
  printf "Minecraft Screenshot.grab via mainRenderTarget\nprogression-selection.png\nknowledge-sector-selection.png\nknowledge-branch-menu.png\nknowledge-screen.png\nresearch-sector-selection.png\nresearch-screen.png\nresearch-branch-menu.png\nknowledge-node-tooltip.png\nresearch-node-tooltip.png\nresearch-zoomed.png\nresearch-purchasable-entry.png\nresearch-purchase-state.png\nresearch-unavailable-tooltip.png\nauto-farm-screen.png (screen backed by real menu; empty client-side container is a synthetic render fixture)\n" > "$results_dir/ui-capture-source.txt"
  printf "Selector and progression tree input checks use in-client Screen callback invocation. Actual X11 wheel/drag/tree clicks are not asserted by this run.\n" >> "$results_dir/ui-capture-source.txt"
  echo "CLIENT_INTERACTION_PATH synthetic Screen callbacks: selector -> both sector menus -> node hover -> one-click server purchase" | tee -a "$client_log"

  collect_x11_diagnostics
  stop_client
  trap - EXIT
'


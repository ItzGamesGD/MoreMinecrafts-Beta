#!/usr/bin/env bash

x11_window_property() {
  local window_id="$1" property="$2" output
  output="$(xprop -id "$window_id" "$property" 2>/dev/null || true)"
  case "$property" in
    WM_CLASS) sed -n 's/^[^=]*= //p' <<<"$output" ;;
    _NET_WM_PID) sed -n 's/^[^=]*= \([0-9][0-9]*\).*$/\1/p' <<<"$output" ;;
  esac
}

x11_geometry_value() {
  local geometry="$1" key="$2"
  sed -n "s/^${key}=//p" <<<"$geometry" | head -n 1
}

discover_minecraft_window() {
  local root_id windows window_id class pid command_line geometry width height
  local -a matches=()

  root_id="$(xwininfo -root -int 2>/dev/null | sed -n 's/^xwininfo: Window id: \([0-9][0-9]*\).*/\1/p' | head -n 1)"
  if [[ ! "$root_id" =~ ^[0-9]+$ ]]; then
    echo "Could not identify the X11 root window ID." >&2
    return 1
  fi

  # The visible-name search alone was observed to omit the framed, active Minecraft window on GitHub runners,
  # so also take SDL's WM_CLASS matches in any state and require the map state explicitly below.
  windows="$( { xdotool search --onlyvisible --name '.*' 2>/dev/null || true; xdotool search --class 'com.mojang.minecraft' 2>/dev/null || true; } | sort -un)"
  for window_id in $windows; do
    [[ "$window_id" =~ ^[0-9]+$ ]] || continue
    [[ "$window_id" != "$root_id" ]] || continue
    xwininfo -id "$window_id" 2>/dev/null | grep -q 'Map State: IsViewable' || continue

    class="$(x11_window_property "$window_id" WM_CLASS)"
    pid="$(x11_window_property "$window_id" _NET_WM_PID)"
    [[ "$pid" =~ ^[0-9]+$ ]] || continue
    command_line="$(ps -p "$pid" -o args= 2>/dev/null || true)"
    case "$command_line" in
      *net.fabricmc.devlaunchinjector.Main*|*net.fabricmc.loader.impl.launch.knot.KnotClient*|*net.minecraft.client.main.Main*) ;;
      *) continue ;;
    esac

    geometry="$(xdotool getwindowgeometry --shell "$window_id" 2>/dev/null || true)"
    width="$(x11_geometry_value "$geometry" WIDTH)"
    height="$(x11_geometry_value "$geometry" HEIGHT)"
    [[ "$width" =~ ^[0-9]+$ && "$height" =~ ^[0-9]+$ ]] || continue
    (( width >= 640 && height >= 360 )) || continue

    matches+=("$window_id|$class|$pid")
  done

  if (( ${#matches[@]} != 1 )); then
    echo "Expected one visible Minecraft client window; found ${#matches[@]}." >&2
    return 1
  fi

  IFS='|' read -r X11_GAME_WINDOW_ID X11_GAME_WINDOW_CLASS X11_GAME_WINDOW_PID <<<"${matches[0]}"
  geometry="$(xdotool getwindowgeometry --shell "$X11_GAME_WINDOW_ID" 2>/dev/null || true)"
  X11_GAME_WINDOW_X="$(x11_geometry_value "$geometry" X)"
  X11_GAME_WINDOW_Y="$(x11_geometry_value "$geometry" Y)"
  X11_GAME_WINDOW_WIDTH="$(x11_geometry_value "$geometry" WIDTH)"
  X11_GAME_WINDOW_HEIGHT="$(x11_geometry_value "$geometry" HEIGHT)"
  [[ "$X11_GAME_WINDOW_X" =~ ^-?[0-9]+$ && "$X11_GAME_WINDOW_Y" =~ ^-?[0-9]+$ ]] || return 1
}

activate_minecraft_window() {
  X11_WINDOWACTIVATE_STATUS=0
  X11_WINDOWFOCUS_STATUS=0
  xdotool windowactivate --sync "$X11_GAME_WINDOW_ID" >/dev/null 2>&1 || X11_WINDOWACTIVATE_STATUS=$?
  xdotool windowfocus --sync "$X11_GAME_WINDOW_ID" >/dev/null 2>&1 || X11_WINDOWFOCUS_STATUS=$?

  local attempt
  for attempt in $(seq 1 20); do
    X11_GAME_ACTIVE_WINDOW="$(xdotool getactivewindow 2>/dev/null || true)"
    X11_GAME_FOCUSED_WINDOW="$(xdotool getwindowfocus 2>/dev/null || true)"
    if [[ "$X11_GAME_ACTIVE_WINDOW" == "$X11_GAME_WINDOW_ID" \
          && "$X11_GAME_FOCUSED_WINDOW" == "$X11_GAME_WINDOW_ID" ]]; then
      return 0
    fi
    sleep 0.1
  done
  return 1
}

x11_transform_coordinates() {
  local render_x="$1" render_y="$2" render_width="$3" render_height="$4"
  [[ "$render_x" =~ ^[0-9]+$ && "$render_y" =~ ^[0-9]+$ \
      && "$render_width" =~ ^[1-9][0-9]*$ && "$render_height" =~ ^[1-9][0-9]*$ ]] || return 1
  [[ "$X11_GAME_WINDOW_WIDTH" =~ ^[1-9][0-9]*$ && "$X11_GAME_WINDOW_HEIGHT" =~ ^[1-9][0-9]*$ ]] || return 1
  (( render_x < render_width && render_y < render_height )) || return 1

  X11_CLICK_LOCAL_X=$(((render_x * X11_GAME_WINDOW_WIDTH + render_width / 2) / render_width))
  X11_CLICK_LOCAL_Y=$(((render_y * X11_GAME_WINDOW_HEIGHT + render_height / 2) / render_height))
  (( X11_CLICK_LOCAL_X < X11_GAME_WINDOW_WIDTH && X11_CLICK_LOCAL_Y < X11_GAME_WINDOW_HEIGHT )) || return 1
  X11_CLICK_ROOT_X=$((X11_GAME_WINDOW_X + X11_CLICK_LOCAL_X))
  X11_CLICK_ROOT_Y=$((X11_GAME_WINDOW_Y + X11_CLICK_LOCAL_Y))
}

# Realistic left click: an explicit XTest press, a short hold, then release. "xdotool click" delivers press and release
# back to back and was observed to reach SDL as two presses, which makes the vanilla inventory screen send two
# container-click packets (place, then pick the item up again). Use this helper for every real-input click.
x11_click_held() {
  local hold="${1:-0.1}"
  xdotool mousedown 1
  sleep "$hold"
  xdotool mouseup 1
}

# Held click with Shift down (quick-move).
x11_shift_click_held() {
  xdotool keydown shift
  sleep 0.15
  x11_click_held "${1:-0.1}"
  sleep 0.15
  xdotool keyup shift
}

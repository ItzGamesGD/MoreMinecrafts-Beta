#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/x11_input.sh"

fixture_dir="$(mktemp -d)"
trap 'rm -rf "$fixture_dir"' EXIT

cat > "$fixture_dir/xwininfo" <<'EOF'
#!/usr/bin/env bash
if [[ "$1" == '-id' ]]; then
  if [[ "$2" == 888 ]]; then echo '  Map State: IsUnMapped'; else echo '  Map State: IsViewable'; fi
else
  echo 'xwininfo: Window id: 543 (the root window)'
fi
EOF

cat > "$fixture_dir/xdotool" <<'EOF'
#!/usr/bin/env bash
if [[ "$*" == 'search --onlyvisible --name .*' ]]; then
  if [[ "${X11_FIXTURE_NO_GAME:-0}" == 1 ]]; then
    printf '543\n2097439\n'
  else
    printf '543\n2097439\n777\n'
  fi
elif [[ "$*" == 'search --class com.mojang.minecraft' ]]; then
  # A second, unmapped SDL window must never be selected.
  if [[ "${X11_FIXTURE_NO_GAME:-0}" == 1 ]]; then printf '888\n'; else printf '777\n888\n'; fi
elif [[ "$*" == 'windowactivate --sync 777' || "$*" == 'windowfocus --sync 777' ]]; then
  exit 0
elif [[ "$*" == 'getwindowfocus' || "$*" == 'getactivewindow' ]]; then
  if [[ "$*" == 'getwindowfocus' ]]; then echo "${X11_FIXTURE_FOCUS:-777}"; else echo "${X11_FIXTURE_ACTIVE:-777}"; fi
elif [[ "$*" == 'getwindowgeometry --shell 777' ]]; then
  printf 'WINDOW=777\nX=18\nY=36\nWIDTH=1280\nHEIGHT=720\nSCREEN=0\n'
elif [[ "$*" == 'getwindowgeometry --shell 888' ]]; then
  printf 'WINDOW=888\nX=0\nY=0\nWIDTH=1280\nHEIGHT=720\nSCREEN=0\n'
elif [[ "$*" == 'getwindowgeometry --shell 2097439' ]]; then
  printf 'WINDOW=2097439\nX=0\nY=0\nWIDTH=1\nHEIGHT=1\nSCREEN=0\n'
else
  exit 1
fi
EOF

cat > "$fixture_dir/xprop" <<'EOF'
#!/usr/bin/env bash
id="$2"
property="$3"
case "$id:$property" in
  543:WM_CLASS) echo 'WM_CLASS(STRING) = "", ""' ;;
  543:_NET_WM_PID) echo '_NET_WM_PID(CARDINAL) = 0' ;;
  2097439:WM_CLASS) echo 'WM_CLASS(STRING) = "openbox", "Openbox"' ;;
  2097439:_NET_WM_PID) echo '_NET_WM_PID(CARDINAL) = 100' ;;
  777:WM_CLASS) echo 'WM_CLASS(STRING) = "java", "Minecraft"' ;;
  777:_NET_WM_PID) echo '_NET_WM_PID(CARDINAL) = 4242' ;;
  888:WM_CLASS) echo 'WM_CLASS(STRING) = "java", "com.mojang.minecraft"' ;;
  888:_NET_WM_PID) echo '_NET_WM_PID(CARDINAL) = 4242' ;;
  *) exit 1 ;;
esac
EOF

cat > "$fixture_dir/ps" <<'EOF'
#!/usr/bin/env bash
if [[ "$*" == '-p 4242 -o args=' ]]; then
  echo 'java net.fabricmc.devlaunchinjector.Main --width 1280 --height 720'
elif [[ "$*" == '-p 100 -o args=' ]]; then
  echo 'openbox --sm-disable'
else
  exit 1
fi
EOF

chmod +x "$fixture_dir"/*
export PATH="$fixture_dir:$PATH"
discover_minecraft_window

[[ "$X11_GAME_WINDOW_ID" == 777 ]]
[[ "$X11_GAME_WINDOW_CLASS" == *'"java", "Minecraft"'* ]]
[[ "$X11_GAME_WINDOW_PID" == 4242 ]]
[[ "$X11_GAME_WINDOW_X" == 18 ]]
[[ "$X11_GAME_WINDOW_Y" == 36 ]]
[[ "$X11_GAME_WINDOW_WIDTH" == 1280 ]]
[[ "$X11_GAME_WINDOW_HEIGHT" == 720 ]]
activate_minecraft_window
[[ "$X11_GAME_ACTIVE_WINDOW" == 777 ]]
[[ "$X11_GAME_FOCUSED_WINDOW" == 777 ]]
x11_transform_coordinates 248 183 640 360
[[ "$X11_CLICK_LOCAL_X" == 496 ]]
[[ "$X11_CLICK_LOCAL_Y" == 366 ]]
[[ "$X11_CLICK_ROOT_X" == 514 ]]
[[ "$X11_CLICK_ROOT_Y" == 402 ]]
if X11_FIXTURE_FOCUS=2097439 activate_minecraft_window; then
  echo 'FAIL: accepted Openbox as Minecraft focus' >&2
  exit 1
fi
if X11_FIXTURE_NO_GAME=1 discover_minecraft_window; then
  echo 'FAIL: selected root/Openbox with no Minecraft window present' >&2
  exit 1
fi
if x11_transform_coordinates 640 183 640 360; then
  echo 'FAIL: accepted an out-of-bounds render coordinate' >&2
  exit 1
fi
echo 'PASS: selects the viewable Minecraft process window, excluding root, Openbox and unmapped windows'

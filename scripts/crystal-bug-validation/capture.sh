#!/usr/bin/env bash
# Real Minecraft client capture for Crystal Bug P2-D1 (D1 static views + D2 animation sequences).
set -euo pipefail
OUT=crystal-bug-results
mkdir -p run "$OUT"
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=crystal-bug-validation
level-seed=2632026
server-ip=127.0.0.1
server-port=25565
online-mode=false
white-list=false
enforce-whitelist=false
enable-rcon=true
rcon.port=25575
rcon.password=$RCON_PASSWORD
spawn-protection=0
spawn-radius=0
PROPS
MOREMINECRAFTS_VISUAL_VALIDATION=1 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runServer --args="--safeMode" > "$OUT/server.log" 2>&1 &
server_pid=$!
trap 'kill -- -$server_pid 2>/dev/null || true' EXIT
for n in $(seq 1 180); do
  if grep -Fq 'Done (' "$OUT/server.log"; then break; fi
  if ! kill -0 "$server_pid" 2>/dev/null; then echo SERVER_START_FAILED; exit 1; fi
  sleep 2
done
grep -Fq 'Done (' "$OUT/server.log" || { echo SERVER_READY_TIMEOUT; exit 1; }
echo SERVER_READY
# NoAI keeps the bug still (D1 is a static model); Silent avoids ambient noise. Faces +Z (south).
python3 scripts/visual_validation/rcon.py 'setworldspawn 0 64 16' 'forceload add -16 -16 16 16' 'time set noon' 'weather clear' \
  'fill -12 63 -8 12 63 16 minecraft:stone' 'fill -12 64 -8 12 75 16 minecraft:air' \
  'summon moreminecrafts:crystal_bug 0.5 64 4.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["bug_visual"]}' \
  'execute if entity @e[type=moreminecrafts:crystal_bug,tag=bug_visual] run say CRYSTAL_BUG_CREATED' | tee "$OUT/scene.log"
printf 'guiScale:2\ntutorialStep:none\n' > run/options.txt
export OUT
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565 MOREMINECRAFTS_CRYSTAL_BUG_CAPTURE=1
 rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$OUT/scene.log"; }
 openbox --sm-disable > "$OUT/openbox.log" 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username BugViewer" > "$OUT/client.log" 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 60); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log"; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; tail -n 150 "$OUT/client.log"; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log" || { echo CLIENT_JOIN_TIMEOUT; tail -n 150 "$OUT/client.log"; exit 1; }
 echo CLIENT_JOINED
 rcon "gamemode spectator BugViewer"
 sleep 20
 source scripts/visual_validation/x11_input.sh
 discover_minecraft_window
 activate_minecraft_window
 xdotool key F1
 sleep 2
 # name cam_x cam_y cam_z target_y [target_x target_z]; the bug is centred near 0.5 64 4.5, head toward +Z.
 shoot() {
   local name=$1 x=$2 y=$3 z=$4 ty=$5 tx=${6:-0.5} tz=${7:-4.5}
   read -r yaw pitch < <(python3 -c "import math;dx=$tx-($x);dz=$tz-($z);dy=$ty-($y+1.62);print(f\"{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}\")")
   rcon "tp BugViewer $x $y $z $yaw $pitch"
   sleep 5
   scrot "$OUT/$name.png"
   echo "SCREEN_CAPTURED $name yaw=$yaw pitch=$pitch"
 }
 rcon "fill -12 64 -7 12 73 -7 minecraft:light_blue_concrete"
 shoot front 0.5 63.8 10.5 64.9
 shoot back 0.5 63.8 -1.5 64.9
 shoot left 6.8 63.8 4.5 64.9
 shoot right -5.8 63.8 4.5 64.9
 shoot top 0.5 69.5 4.51 64.0
 shoot front-three-quarter 4.8 64.6 8.8 64.9
 shoot back-three-quarter -3.8 64.6 0.2 64.9
 shoot head-close 0.5 63.4 7.6 64.3 0.5 5.5
 shoot crystals-close 2.6 65.4 4.5 65.0
 # Neutral (smooth stone) and bright (open sky) backgrounds, and a normal gameplay distance.
 rcon "fill -12 64 -7 12 73 -7 minecraft:smooth_stone" "fill -12 64 15 12 73 15 minecraft:smooth_stone"
 shoot neutral-front-three-quarter 4.8 64.6 8.8 64.9
 shoot neutral-back-three-quarter -3.8 64.6 0.2 64.9
 rcon "fill -12 64 -7 12 73 -7 minecraft:air" "fill -12 64 15 12 73 15 minecraft:air"
 shoot bright-front-three-quarter 4.8 64.6 8.8 64.9
 shoot gameplay-three-quarter 7.5 64.0 12.0 64.9
 echo CAPTURE_DONE
 OUT="$OUT" bash scripts/crystal-bug-validation/capture_sequences.sh
 grep "CRYSTAL_BUG_POSE" "$OUT/client.log" > "$OUT/pose.log" || true
 grep -Eq "walkSpeed=0\.[3-9]" "$OUT/pose.log" || { echo NO_WALK_POSE; exit 1; }
 grep -Eq "swing=0\.[3-9]" "$OUT/pose.log" || { echo NO_ATTACK_SWING; exit 1; }
 grep -Eq "hurt=0\.[5-9]" "$OUT/pose.log" || { echo NO_HURT_POSE; exit 1; }
 grep -Eq "death=(1[0-9]|20)\." "$OUT/pose.log" || { echo NO_DEATH_POSE; exit 1; }
 for s in idle-three-quarter idle-side walk-side walk-three-quarter turning attack-side attack-three-quarter hurt-three-quarter hurt-side death-three-quarter death-side walk-to-idle; do
   test -s "$OUT/sequences/$s/preview.mp4" || { echo "MISSING_SEQUENCE $s"; exit 1; }
 done
 echo SEQUENCES_VERIFIED
'
for v in front back left right top front-three-quarter back-three-quarter; do
  test -s "$OUT/$v.png" || { echo "MISSING_VIEW $v"; exit 1; }
done
python3 -I scripts/crystal-bug-validation/contact_sheet.py "$OUT"
echo VIEWS_VERIFIED

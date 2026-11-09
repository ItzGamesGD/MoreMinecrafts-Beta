#!/usr/bin/env bash
# Real Minecraft client capture for Skyguard Archer P2-C1 (static model, texture, bow-aim pose).
set -euo pipefail
OUT=skyguard-archer-results
mkdir -p run "$OUT"
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=archer-validation
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
python3 scripts/visual_validation/rcon.py 'setworldspawn 0 64 12' 'forceload add -16 -16 16 16' 'time set noon' 'weather clear' \
  'fill -12 63 -8 12 63 16 minecraft:stone' 'fill -12 64 -8 12 75 16 minecraft:air' 'fill -12 64 -7 12 73 -7 minecraft:light_blue_concrete' \
  'summon moreminecrafts:skyguard_archer 0.5 64 4.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["archer_visual"]}' \
  'execute if entity @e[type=moreminecrafts:skyguard_archer,tag=archer_visual] run say ARCHER_CREATED' | tee "$OUT/scene.log"
printf 'guiScale:2\ntutorialStep:none\n' > run/options.txt
export OUT
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565 MOREMINECRAFTS_ARCHER_CAPTURE=1
 rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$OUT/scene.log"; }
 openbox --sm-disable > "$OUT/openbox.log" 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username ArcherViewer" > "$OUT/client.log" 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 60); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log"; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; tail -n 150 "$OUT/client.log"; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log" || { echo CLIENT_JOIN_TIMEOUT; tail -n 150 "$OUT/client.log"; exit 1; }
 echo CLIENT_JOINED
 rcon "gamemode creative ArcherViewer"
 sleep 20
 source scripts/visual_validation/x11_input.sh
 discover_minecraft_window
 activate_minecraft_window
 xdotool key F1
 sleep 2
 # name cam_x cam_y cam_z target_y ; archer stands at 0.5 64 4.5 facing +Z (south). Sides are named from the archer.
 shoot() {
   local name=$1 x=$2 y=$3 z=$4 ty=$5 tx=${6:-0.5} tz=${7:-4.5}
   read -r yaw pitch < <(python3 -c "import math;dx=$tx-($x);dz=$tz-($z);dy=$ty-($y+1.62);print(f\"{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}\")")
   rcon "tp ArcherViewer $x $y $z $yaw $pitch"
   sleep 5
   scrot "$OUT/$name.png"
   echo "SCREEN_CAPTURED $name yaw=$yaw pitch=$pitch"
 }
 rcon "tp @e[type=moreminecrafts:skyguard_archer,tag=archer_visual,limit=1] 0.5 64 4.5 0 0"
 shoot front 0.5 64 8.5 65.0
 shoot back 0.5 64 0.5 65.0
 shoot left 4.5 64 4.5 65.0
 shoot right -3.5 64 4.5 65.0
 shoot front-three-quarter 3.3 64 7.3 65.0
 shoot hood-close 0.5 64.3 6.4 65.6
 shoot shoulder-close -1.3 64.3 6.1 65.3 -0.1 4.5
 shoot cape-close 0.5 64.4 2.0 64.9
 # Bow-aim: live AI archer (movement 0) aiming at an invulnerable, immobile iron golem to its west.
 rcon "kill @e[type=moreminecrafts:skyguard_archer,tag=archer_visual]" \
   "summon moreminecrafts:skyguard_archer 0.5 64 4.5 {PersistenceRequired:1b,Rotation:[90f,0f],Tags:[\"archer_aim\"]}" \
   "attribute @e[type=moreminecrafts:skyguard_archer,tag=archer_aim,limit=1] minecraft:movement_speed base set 0" \
   "summon minecraft:iron_golem -7.5 64 4.5 {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b}"
 sleep 4
 for i in 1 2 3 4 5 6; do
   shoot "bow-aim-$i" -2.0 64 8.0 65.0 0.5 4.5
   rcon "kill @e[type=minecraft:arrow]"
 done
 grep "ARCHER_POSE" "$OUT/client.log" | tee "$OUT/pose.log" || true
 grep -q "aggressive=true holdingBow=true rightPose=BOW_AND_ARROW" "$OUT/pose.log"
'

#!/usr/bin/env bash
# Real Minecraft client capture for Cloud Slime P2-E1 (static model, concept C): three sizes, five angles, two backgrounds.
set -euo pipefail
OUT=cloud-slime-results
mkdir -p run "$OUT"
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=cloud-slime-validation
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
difficulty=easy
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
# Sizes: NBT Size 0 = small, 1 = medium, 3 = large. NoAI keeps them still and facing +Z (south).
python3 scripts/visual_validation/rcon.py 'setworldspawn 0 64 16' 'forceload add -24 -16 24 24' 'time set noon' 'weather clear' \
  'fill -16 63 -10 16 63 18 minecraft:stone' 'fill -16 64 -10 16 80 18 minecraft:air' \
  'summon moreminecrafts:cloud_slime -5.5 64 4.5 {Size:3,NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["slime_visual"]}' \
  'summon moreminecrafts:cloud_slime 0.5 64 4.5 {Size:1,NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["slime_visual"]}' \
  'summon moreminecrafts:cloud_slime 5.5 64 4.5 {Size:0,NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["slime_visual"]}' \
  'execute if entity @e[type=moreminecrafts:cloud_slime,tag=slime_visual] run say CLOUD_SLIME_CREATED' | tee "$OUT/scene.log"
printf 'guiScale:2\ntutorialStep:none\n' > run/options.txt
export OUT
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565 MOREMINECRAFTS_CLOUD_SLIME_CAPTURE=1
 rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$OUT/scene.log"; }
 openbox --sm-disable > "$OUT/openbox.log" 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username SlimeViewer" > "$OUT/client.log" 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 90); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log"; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; tail -n 150 "$OUT/client.log"; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log" || { echo CLIENT_JOIN_TIMEOUT; tail -n 150 "$OUT/client.log"; exit 1; }
 echo CLIENT_JOINED
 rcon "gamemode spectator SlimeViewer"
 sleep 20
 source scripts/visual_validation/x11_input.sh
 discover_minecraft_window
 activate_minecraft_window
 xdotool key F1
 sleep 2
 # shoot name cam_x cam_y(feet) cam_z target_x target_y target_z
 shoot() {
   local name=$1 x=$2 y=$3 z=$4 tx=$5 ty=$6 tz=$7
   read -r yaw pitch < <(python3 -c "import math;dx=$tx-($x);dz=$tz-($z);dy=$ty-($y+1.62);print(f\"{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}\")")
   rcon "tp SlimeViewer $x $y $z $yaw $pitch"
   sleep 4
   scrot "$OUT/$name.png"
   echo "SCREEN_CAPTURED $name yaw=$yaw pitch=$pitch"
 }
 # size name, centre x, half-height (target y), camera distance
 views() {
   local bg=$1
   for spec in "large -5.5 1.0 5.2" "medium 0.5 0.5 2.9" "small 5.5 0.25 1.7"; do
     read -r size cx h d <<< "$spec"
     local cz=4.5 ty
     ty=$(python3 -c "print(64+$h)")
     local feet; feet=$(python3 -c "print($ty-1.62)")
     shoot "$bg-$size-front" "$cx" "$feet" "$(python3 -c "print($cz+$d)")" "$cx" "$ty" "$cz"
     shoot "$bg-$size-side" "$(python3 -c "print($cx+$d)")" "$feet" "$cz" "$cx" "$ty" "$cz"
     shoot "$bg-$size-back" "$cx" "$feet" "$(python3 -c "print($cz-$d)")" "$cx" "$ty" "$cz"
     shoot "$bg-$size-elevated" "$cx" "$(python3 -c "print($feet+$d*0.9)")" "$(python3 -c "print($cz+$d*0.75)")" "$cx" "$ty" "$cz"
     shoot "$bg-$size-three-quarter" "$(python3 -c "print($cx+$d*0.7)")" "$(python3 -c "print($feet+$d*0.25)")" "$(python3 -c "print($cz+$d*0.7)")" "$cx" "$ty" "$cz"
   done
 }
 views bright
 shoot bright-group -0.5 64.6 13.5 -0.5 64.7 4.5
 # Dark background: black concrete walls around the scene (sky light still reaches the mobs from above).
 rcon "fill -16 64 -10 16 76 -10 minecraft:black_concrete" "fill -16 64 18 16 76 18 minecraft:black_concrete" \
      "fill -16 64 -10 -16 76 18 minecraft:black_concrete" "fill 16 64 -10 16 76 18 minecraft:black_concrete" \
      "fill -16 63 -10 16 63 18 minecraft:black_concrete"
 views dark
 shoot dark-group -0.5 64.6 13.5 -0.5 64.7 4.5
 echo CAPTURE_DONE
'
python3 -I scripts/cloud-slime-validation/contact_sheet.py "$OUT"
for bg in bright dark; do for s in large medium small; do for v in front side back elevated three-quarter; do
  test -s "$OUT/$bg-$s-$v.png" || { echo "MISSING_VIEW $bg-$s-$v"; exit 1; }
done; done; done
grep -Eq "missing|MissingTexture" "$OUT/client.log" && grep -E "cloud_slime.*(missing|not found)" "$OUT/client.log" && { echo TEXTURE_MISSING; exit 1; } || true
echo VIEWS_VERIFIED

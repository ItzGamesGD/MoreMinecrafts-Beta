#!/usr/bin/env bash
# Real Minecraft client capture for Fallen Sentinel P2-F1 (concept D Blood Seraph): static views on bright and dark
# backgrounds, a size line-up with a vanilla husk and a player mannequin, and recorded walk / attack / hurt / death.
set -euo pipefail
OUT=${OUT:-fallen-sentinel-results}
mkdir -p run "$OUT"
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=fallen-sentinel-validation
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
rm -rf run/fallen-sentinel-validation
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
R() { python3 scripts/visual_validation/rcon.py "$@" >> "$OUT/scene.log"; }
# Static subject faces +Z (south). Line-up at z=-9.5: player mannequin, Fallen Sentinel, vanilla husk.
# Animation pen (invisible barriers) at x 20..30: an AI sentinel walks to a NoAI villager and attacks it. The villager is
# not Invulnerable (vanilla targeting ignores invulnerable entities); 1024 max health keeps it alive for the clip.
R 'setworldspawn 0 64 30' 'forceload add -24 -24 56 40' 'time set noon' 'weather clear' \
  'gamerule spawn_mobs false' \
  'fill -24 63 -24 56 63 40 minecraft:stone' 'fill -24 64 -24 56 80 40 minecraft:air' \
  'summon moreminecrafts:fallen_sentinel 0.5 64 4.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["keep"]}' \
  'summon minecraft:mannequin -2.5 64 -9.5 {Rotation:[0f,0f],Tags:["keep"]}' \
  'summon moreminecrafts:fallen_sentinel 0.5 64 -9.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["keep"]}' \
  'summon minecraft:husk 3.5 64 -9.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:["keep"]}' \
  'fill 19 64 2 31 67 6 minecraft:barrier' 'fill 20 64 3 30 67 5 minecraft:air' \
  'summon minecraft:villager 29.5 64 4.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[90f,0f],Tags:["keep"],attributes:[{id:"minecraft:max_health",base:1024d}],Health:1024f}' \
  'execute if entity @e[type=moreminecrafts:fallen_sentinel] run say FALLEN_SENTINEL_CREATED' | tee -a "$OUT/scene.log"
printf 'guiScale:2\ntutorialStep:none\nmaxFps:60\nenableVsync:false\n' > run/options.txt
export OUT
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565 MOREMINECRAFTS_FALLEN_SENTINEL_CAPTURE=1
 R() { python3 scripts/visual_validation/rcon.py "$@" >> "$OUT/scene.log"; }
 openbox --sm-disable > "$OUT/openbox.log" 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username SentinelViewer" > "$OUT/client.log" 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 120); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log"; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; tail -n 150 "$OUT/client.log"; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log" || { echo CLIENT_JOIN_TIMEOUT; tail -n 150 "$OUT/client.log"; exit 1; }
 echo CLIENT_JOINED
 R "gamemode spectator SentinelViewer"
 sleep 20
 source scripts/visual_validation/x11_input.sh
 discover_minecraft_window
 activate_minecraft_window
 xdotool key F1
 sleep 2
 clean() { R "kill @e[type=!minecraft:player,tag=!keep]"; }
 aim() { python3 -c "import math;x,y,z,tx,ty,tz=$1,$2,$3,$4,$5,$6;dx=tx-x;dz=tz-z;dy=ty-(y+1.62);print(f\"{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}\")"; }
 # shoot name cam_x cam_feet_y cam_z target_x target_y target_z
 shoot() {
   read -r yaw pitch < <(aim "$2" "$3" "$4" "$5" "$6" "$7")
   R "tp SentinelViewer $2 $3 $4 $yaw $pitch"
   sleep 4
   scrot "$OUT/$1.png"
   echo "SCREEN_CAPTURED $1"
 }
 # rec name seconds cam_x cam_feet_y cam_z target_x target_y target_z
 rec() {
   read -r yaw pitch < <(aim "$3" "$4" "$5" "$6" "$7" "$8")
   R "tp SentinelViewer $3 $4 $5 $yaw $pitch"
   sleep 3
   ffmpeg -nostdin -loglevel error -y -f x11grab -framerate 20 -video_size 1280x720 -i "$DISPLAY" -t "$2" \
     -c:v libx264 -preset ultrafast -crf 18 -pix_fmt yuv420p "$OUT/$1.mp4" &
   rec_pid=$!
 }
 views() {
   local bg=$1 cx=0.5 cz=4.5 ty=65.1 d=4.4 feet
   clean
   feet=$(python3 -c "print($ty-1.62)")
   shoot "$bg-front" $cx "$feet" "$(python3 -c "print($cz+$d)")" $cx $ty $cz
   shoot "$bg-left" "$(python3 -c "print($cx+$d)")" "$feet" $cz $cx $ty $cz
   shoot "$bg-right" "$(python3 -c "print($cx-$d)")" "$feet" $cz $cx $ty $cz
   shoot "$bg-back" $cx "$feet" "$(python3 -c "print($cz-$d)")" $cx $ty $cz
   shoot "$bg-front45" "$(python3 -c "print($cx-$d*0.7)")" "$(python3 -c "print($feet+0.4)")" "$(python3 -c "print($cz+$d*0.7)")" $cx $ty $cz
   shoot "$bg-back45" "$(python3 -c "print($cx+$d*0.7)")" "$(python3 -c "print($feet+0.4)")" "$(python3 -c "print($cz-$d*0.7)")" $cx $ty $cz
   shoot "$bg-elevated" $cx "$(python3 -c "print($feet+3.0)")" "$(python3 -c "print($cz+3.2)")" $cx $ty $cz
   shoot "$bg-helm-close" 0.5 64.3 6.6 0.5 65.6 4.5
   shoot "$bg-halo-close" 1.6 65.6 6.0 0.5 66.0 4.5
   shoot "$bg-wings-close" -1.2 64.3 1.9 0.5 65.2 4.5
   shoot "$bg-lineup" 0.5 63.9 -3.0 0.5 65.0 -9.5
 }
 views bright
 # Walk + attack: a fresh AI sentinel at the west end of the pen walks to the villager and swings at it.
 for cam in side front45; do
   clean
   R "kill @e[type=moreminecrafts:fallen_sentinel,tag=walker]"
   if [ $cam = side ]; then rec "walk-attack-$cam" 14 25.0 64.0 11.5 25.0 65.0 4.5; else rec "walk-attack-$cam" 14 31.5 64.6 9.0 25.0 65.0 4.5; fi
   R "summon moreminecrafts:fallen_sentinel 20.5 64 4.5 {Silent:1b,PersistenceRequired:1b,Rotation:[-90f,0f],Tags:[\"walker\",\"keep\"]}"
   wait $rec_pid; echo "RECORDED walk-attack-$cam"
 done
 R "kill @e[type=moreminecrafts:fallen_sentinel,tag=walker]"
 # Hurt then death on a NoAI sentinel.
 R "summon moreminecrafts:fallen_sentinel 40.5 64 4.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:[\"victim\",\"keep\"]}"
 rec hurt-death 9 43.2 64.0 8.4 40.5 65.0 4.5
 sleep 1.5; R "damage @e[tag=victim,limit=1] 4 minecraft:generic"
 sleep 2.0; R "damage @e[tag=victim,limit=1] 4 minecraft:generic"
 sleep 2.0; R "kill @e[tag=victim]"
 wait $rec_pid; echo "RECORDED hurt-death"
 # Dark: midnight with black concrete floor and walls (no roof; sky light only).
 R "time set 18000" "fill -24 63 -24 56 63 40 minecraft:black_concrete" \
   "fill -24 64 -24 56 72 -24 minecraft:black_concrete" "fill -24 64 40 56 72 40 minecraft:black_concrete" \
   "fill -24 64 -24 -24 72 40 minecraft:black_concrete" "fill 56 64 -24 56 72 40 minecraft:black_concrete"
 views dark
 echo CAPTURE_DONE
'
python3 -I scripts/fallen-sentinel-validation/contact_sheet.py "$OUT"
for bg in bright dark; do for v in front left right back front45 back45 elevated helm-close halo-close wings-close lineup; do
  test -s "$OUT/$bg-$v.png" || { echo "MISSING_VIEW $bg-$v"; exit 1; }
done; done
for clip in walk-attack-side walk-attack-front45 hurt-death; do test -s "$OUT/$clip.mp4" || { echo "MISSING_CLIP $clip"; exit 1; }; done
grep -E "fallen_sentinel.*(missing|not found)" "$OUT/client.log" && { echo TEXTURE_MISSING; exit 1; } || true
echo VIEWS_VERIFIED

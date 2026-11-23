#!/usr/bin/env bash
# Real server + real client video capture of the three Blockbench-model Sky Mobs (animation, not stills).
set -euo pipefail
OUT=three-mob-results
mkdir -p run "$OUT"
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=three-mob-validation
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
  if ! kill -0 "$server_pid" 2>/dev/null; then echo SERVER_START_FAILED; tail -n 80 "$OUT/server.log"; exit 1; fi
  sleep 2
done
grep -Fq 'Done (' "$OUT/server.log" || { echo SERVER_READY_TIMEOUT; exit 1; }
echo SERVER_READY
python3 scripts/visual_validation/rcon.py 'setworldspawn 0 64 16' 'forceload add -24 -24 24 24' 'time set noon' 'weather clear' \
  'gamerule mob_griefing false' 'gamerule do_mob_spawning false' | tee "$OUT/scene.log"
printf 'guiScale:2\ntutorialStep:none\n' > run/options.txt
export OUT
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565
 openbox --sm-disable > "$OUT/openbox.log" 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username MobViewer" > "$OUT/client.log" 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 90); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log"; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; tail -n 150 "$OUT/client.log"; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED "$OUT/client.log" || { echo CLIENT_JOIN_TIMEOUT; tail -n 150 "$OUT/client.log"; exit 1; }
 echo CLIENT_JOINED
 python3 scripts/visual_validation/rcon.py "gamemode spectator MobViewer" >> "$OUT/scene.log"
 sleep 20
 source scripts/visual_validation/x11_input.sh
 discover_minecraft_window
 activate_minecraft_window
 xdotool key F1
 sleep 2
 bash scripts/three-mob-video/sequences.sh
 echo SEQUENCES_DONE
'
n=$(find "$OUT/sequences" -name preview.mp4 | wc -l)
echo "VIDEOS=$n"
test "$n" -ge 12

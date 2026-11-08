#!/usr/bin/env bash
set -euo pipefail
mkdir -p run sky-wraith-results
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<PROPS
level-name=wraith-validation
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
MOREMINECRAFTS_VISUAL_VALIDATION=1 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runServer --args="--safeMode" > sky-wraith-results/server.log 2>&1 &
server_pid=$!
trap 'kill -- -$server_pid 2>/dev/null || true' EXIT
for n in $(seq 1 180); do
  if grep -Fq 'Done (' sky-wraith-results/server.log; then break; fi
  if ! kill -0 "$server_pid" 2>/dev/null; then echo 'SERVER_START_FAILED'; exit 1; fi
  sleep 2
done
grep -Fq 'Done (' sky-wraith-results/server.log || { echo 'SERVER_READY_TIMEOUT'; exit 1; }
echo SERVER_READY
python3 scripts/visual_validation/rcon.py 'setworldspawn 0 64 12' 'forceload add -8 -8 8 16' 'time set noon' 'weather clear' 'fill -8 63 -8 8 63 16 minecraft:stone' 'fill -8 64 -8 8 75 16 minecraft:air' 'fill -8 64 -7 8 73 -7 minecraft:light_blue_concrete' 'summon moreminecrafts:cloud_wraith 0.5 64 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["wraith_visual"]}' 'summon minecraft:husk -2.4 64 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["wraith_comparison"]}' 'execute if entity @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual] run say WRAITH_CREATED' | tee sky-wraith-results/scene.log
printf 'guiScale:2\ntutorialStep:none\n' > run/options.txt
xvfb-run -a -s '-screen 0 1280x720x24 +extension GLX +render -noreset' bash -euo pipefail -c '
 export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe MOREMINECRAFTS_VISUAL_VALIDATION=1 MOREMINECRAFTS_VISUAL_SERVER_ADDRESS=127.0.0.1:25565 MOREMINECRAFTS_WRAITH_CAPTURE=1
 openbox --sm-disable > sky-wraith-results/openbox.log 2>&1 &
 setsid ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runClient --args="--width 1280 --height 720 --username WraithViewer" > sky-wraith-results/client.log 2>&1 &
 client_pid=$!
 trap "kill -- -$client_pid 2>/dev/null || true" EXIT
 for n in $(seq 1 60); do
   if grep -Fq VISUAL_VALIDATION_PLAYER_JOINED sky-wraith-results/client.log; then break; fi
   if ! kill -0 "$client_pid" 2>/dev/null; then echo CLIENT_EXITED_BEFORE_JOIN; cat sky-wraith-results/client.log; exit 1; fi
   sleep 2
 done
 grep -Fq VISUAL_VALIDATION_PLAYER_JOINED sky-wraith-results/client.log || { echo CLIENT_JOIN_TIMEOUT; date -u; ps -ef > sky-wraith-results/processes.txt; for pid in $(pgrep -x java || true); do jcmd "$pid" Thread.print > "sky-wraith-results/threads-$pid.txt" 2>&1 || true; done; ss -tnp > sky-wraith-results/connections.txt 2>&1 || true; tail -n 150 sky-wraith-results/server.log; tail -n 150 sky-wraith-results/client.log; exit 1; }
 echo CLIENT_JOINED
 sleep 20
 for spec in "front 0 64 8 180 5" "side 4 64 4 90 5" "back 0 64 1 0 5"; do
   read -r name x y z yaw pitch <<< "$spec"
   python3 scripts/visual_validation/rcon.py "tp WraithViewer $x $y $z $yaw $pitch" "tp @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] 0.5 64 4.5 0 0" >> sky-wraith-results/scene.log
   sleep 5
   scrot "sky-wraith-results/$name.png"
   echo "SCREEN_CAPTURED $name"
 done
'

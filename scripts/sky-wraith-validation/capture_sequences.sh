#!/usr/bin/env bash
set -euo pipefail
rcon() { python3 scripts/visual_validation/rcon.py "$@" >> sky-wraith-results/sequence-events.log; }
scene_mark() { printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)" "$*" >> sky-wraith-results/sequence-events.log; rcon 'time query gametime'; }
record() { scene_mark "BEGIN $1"; /usr/bin/python3 scripts/sky-wraith-validation/sequence_frames.py "$1" "$2"; scene_mark "END $1"; }
reset_mob() {
  rcon 'kill @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual]' 'kill @e[type=minecraft:villager,tag=wraith_dummy]' \
    'summon moreminecrafts:cloud_wraith 0.5 64 4.5 {NoAI:1b,PersistenceRequired:1b,Tags:["wraith_visual"]}'
}
camera() {
  if [[ "$1" == front ]]; then rcon 'tp WraithViewer 0.5 64 8.5 180 5';
  else rcon 'tp WraithViewer 4.5 64 4.5 90 5'; fi
}
dummy() {
  rcon "summon minecraft:villager 0.5 64 $1 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:[\"wraith_dummy\"]}" \
    'effect give @e[type=minecraft:villager,tag=wraith_dummy,limit=1] minecraft:invisibility 120 0 true' \
    'attribute @e[type=minecraft:villager,tag=wraith_dummy,limit=1] minecraft:max_health base set 200' \
    'data merge entity @e[type=minecraft:villager,tag=wraith_dummy,limit=1] {Health:200.0f}' \
    'attribute @e[type=minecraft:villager,tag=wraith_dummy,limit=1] minecraft:knockback_resistance base set 1'
}
rcon 'gamemode spectator WraithViewer' 'tp @e[type=minecraft:husk,tag=wraith_comparison,limit=1] -5 64 5'
for view in front side; do
  reset_mob; camera "$view"; sleep 2
  record "${view}-idle" 4
  reset_mob; dummy 10.5; camera "$view"
  rcon 'data merge entity @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] {NoAI:0b}'
  sleep 1; record "${view}-walk" 4
  reset_mob; dummy 6.0; camera "$view"
  rcon 'attribute @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] minecraft:movement_speed base set 0' \
    'data merge entity @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] {NoAI:0b}' \
    'data get entity @e[type=minecraft:villager,tag=wraith_dummy,limit=1] Health'
  sleep 1; record "${view}-attack" 5
  rcon 'data get entity @e[type=minecraft:villager,tag=wraith_dummy,limit=1] Health' 'kill @e[type=minecraft:villager,tag=wraith_dummy]'
  record "${view}-recovery" 3
  reset_mob; camera "$view"; sleep 1
  (sleep 0.4; rcon 'damage @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] 2 minecraft:generic') & action_pid=$!
  record "${view}-hurt" 2; wait "$action_pid"
  reset_mob; camera "$view"; sleep 1
  (sleep 0.4; rcon 'damage @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] 1000 minecraft:generic') & action_pid=$!
  record "${view}-death" 2; wait "$action_pid"
done

cat sky-wraith-results/sequence-events.log

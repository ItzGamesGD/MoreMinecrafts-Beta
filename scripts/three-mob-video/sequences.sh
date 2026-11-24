#!/usr/bin/env bash
# Animation sequences (frames + MP4) for Crystal Bug, Skyguard Archer and Fallen Sentinel in the real client.
# Behaviour is driven only through vanilla AI goals: an immobile, very durable iron golem is the target ("provoke" =
# damage the mob "by" the golem), a wolf makes the archer walk away. The viewer is a spectator.
set -euo pipefail
OUT=${OUT:-three-mob-results}
export OUT
EV="$OUT/sequence-events.log"
V=MobViewer
G='@e[type=minecraft:iron_golem,tag=mob_target,limit=1]'
rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$EV"; }
mark() { printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)" "$*" >> "$EV"; }
record() { mark "BEGIN $1"; python3 scripts/three-mob-video/sequence_frames.py "$1" "$2"; mark "END $1"; }
cam() { # cam x y z target_x target_y target_z
  read -r yaw pitch < <(python3 -c "import math;dx=$4-($1);dz=$6-($3);dy=$5-($2+1.62);print(f'{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}')")
  rcon "tp $V $1 $2 $3 $yaw $pitch"
}
clear_scene() {
  rcon 'kill @e[type=moreminecrafts:crystal_bug]' 'kill @e[type=moreminecrafts:skyguard_archer]' \
    'kill @e[type=moreminecrafts:fallen_sentinel]' 'kill @e[type=minecraft:iron_golem]' 'kill @e[type=minecraft:wolf]' \
    'kill @e[type=minecraft:arrow]' 'kill @e[type=minecraft:item]' 'kill @e[type=minecraft:experience_orb]'
  sleep 1.5
}
# spawn <mob> x z yaw ai(0|1): the bow / sword stand-ins come from the renderer, the archer's bow is equipped here.
spawn() {
  local noai=1; [[ "$5" == 1 ]] && noai=0
  rcon "summon moreminecrafts:$1 $2 64 $3 {NoAI:${noai}b,Silent:1b,PersistenceRequired:1b,Rotation:[$4f,0f],Tags:[\"mob_seq\"]}"
  sleep 0.3
  if [[ "$1" == skyguard_archer ]]; then rcon "item replace entity @e[tag=mob_seq,limit=1] weapon.mainhand with minecraft:bow"; fi
}
golem() {
  rcon "summon minecraft:iron_golem $1 64 $2 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:[\"mob_target\"]}" \
    "attribute $G minecraft:max_health base set 1000" "data merge entity $G {Health:1000.0f}" \
    "attribute $G minecraft:knockback_resistance base set 1"
}
provoke() { mark PROVOKE; rcon "damage @e[tag=mob_seq,limit=1] 0.5 minecraft:mob_attack by $G"; }
M='@e[tag=mob_seq,limit=1]'

rcon 'gamemode spectator MobViewer' 'time set noon' 'weather clear' 'fill -16 64 -16 16 78 20 minecraft:air' \
  'fill -16 63 -16 16 63 20 minecraft:stone'
clear_scene

for mob in crystal_bug skyguard_archer fallen_sentinel; do
  case $mob in crystal_bug) cy=64.6; ty=64.6 ;; skyguard_archer) cy=65.0; ty=65.2 ;; *) cy=65.2; ty=65.6 ;; esac
  # Idle: AI-free, three-quarter and side.
  clear_scene; spawn $mob 0.5 4.5 0 0
  cam 4.5 $cy 9.0 0.5 $ty 4.5; sleep 2; record "$mob-idle-three-quarter" 4
  cam 8.0 $cy 4.5 0.5 $ty 4.5; sleep 1; record "$mob-idle-side" 4
  # Walk toward the golem 13 blocks ahead (inside follow range), side view.
  clear_scene; golem 0.5 9.5; spawn $mob 0.5 -3.5 0 1
  if [[ $mob == skyguard_archer ]]; then rcon "attribute $M minecraft:follow_range base set 40"; fi
  cam 10.0 $cy 3.0 0.5 $ty 3.0; sleep 1; provoke; sleep 0.6
  record "$mob-walk-side" 7
  # Attack: starts near the golem (archer: aims and shoots, sentinel/bug: melee).
  clear_scene; golem 0.5 9.0; spawn $mob 0.5 4.0 0 1
  cam 5.5 $cy 4.0 0.5 $ty 6.0; sleep 1; provoke; sleep 0.6
  record "$mob-attack" 6
  # Hurt and death, AI-free, three-quarter.
  clear_scene; spawn $mob 0.5 4.5 0 0
  cam 4.5 $cy 9.0 0.5 $ty 4.5; sleep 1.5
  ( sleep 0.5; mark HURT; rcon "damage $M 2 minecraft:generic" ) & act=$!
  record "$mob-hurt" 2.5; wait "$act"; sleep 0.6
  ( sleep 0.5; mark DEATH; rcon "damage $M 1000 minecraft:generic" ) & act=$!
  record "$mob-death" 3; wait "$act"
done
clear_scene
mark SEQUENCES_DONE

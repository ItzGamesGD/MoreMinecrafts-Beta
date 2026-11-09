#!/usr/bin/env bash
# P2-C2 animation sequences in the real client: timestamped frames + MP4 per scene (sequence_frames.py).
# Behaviour is driven only through vanilla AI goals: a wolf (AvoidEntityGoal) for a plain walk, and an immobile,
# non-invulnerable iron golem (NearestAttackableTargetGoal + bow attack goal) for aim / release / recovery.
set -euo pipefail
OUT=${OUT:-skyguard-archer-results}
EV="$OUT/sequence-events.log"
A='@e[type=moreminecrafts:skyguard_archer,tag=archer_seq,limit=1]'
rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$EV"; }
mark() { printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)" "$*" >> "$EV"; }
record() { mark "BEGIN $1"; python3 scripts/skyguard-archer-validation/sequence_frames.py "$1" "$2"; mark "END $1"; }
cam() { # cam x y z target_x target_y target_z
  read -r yaw pitch < <(python3 -c "import math;dx=$4-($1);dz=$6-($3);dy=$5-($2+1.62);print(f'{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}')")
  rcon "tp ArcherViewer $1 $2 $3 $yaw $pitch"
}
clear_scene() {
  rcon 'kill @e[type=moreminecrafts:skyguard_archer]' 'kill @e[type=minecraft:wolf]' 'kill @e[type=minecraft:iron_golem]' \
    'kill @e[type=minecraft:arrow]' 'kill @e[type=minecraft:item]' 'kill @e[type=minecraft:experience_orb]' \
    'fill -3 64 -4 4 66 16 minecraft:air'
}
# spawn x z yaw ai(0|1): summon with NBT skips finalizeSpawn, so the fixed bow is equipped explicitly afterwards.
spawn() {
  local noai=1; [[ "$4" == 1 ]] && noai=0
  rcon "summon moreminecrafts:skyguard_archer $1 64 $2 {NoAI:${noai}b,PersistenceRequired:1b,Rotation:[$3f,0f],Tags:[\"archer_seq\"]}"
  sleep 0.3
  rcon "item replace entity $A weapon.mainhand with minecraft:bow"
}
golem() {
  rcon "summon minecraft:iron_golem $1 64 $2 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:[\"archer_target\"]}" \
    'attribute @e[type=minecraft:iron_golem,tag=archer_target,limit=1] minecraft:max_health base set 1000' \
    'data merge entity @e[type=minecraft:iron_golem,tag=archer_target,limit=1] {Health:1000.0f}' \
    'attribute @e[type=minecraft:iron_golem,tag=archer_target,limit=1] minecraft:knockback_resistance base set 1'
}
arrows() { rcon 'execute store result score #arrows archer_seq if entity @e[type=minecraft:arrow]' 'scoreboard players get #arrows archer_seq'; }
count_arrows_during() { # seconds: logs the live arrow count every 0.5 s while a sequence records
  local n; n=$(python3 -c "print(int($1*2))")
  for _ in $(seq 1 "$n"); do mark ARROW_SAMPLE; arrows; sleep 0.5; done
}

rcon 'gamemode spectator ArcherViewer' 'scoreboard objectives add archer_seq dummy' 'time set noon' 'weather clear'
clear_scene

# 1-2. Idle (static AI-free archer, vanilla idle arm bob only).
spawn 0.5 4.5 0 0
cam 0.5 64 7.5 0.5 65.0 4.5; sleep 2; record front-idle 4
cam 3.5 64 4.5 0.5 65.0 4.5; sleep 2; record side-idle 4

# 3-4. Walking: a 1-block barrier corridor along z; a wolf at the north end makes the archer walk south (no target).
for view in front side; do
  clear_scene
  rcon 'fill -1 64 -3 -1 65 13 minecraft:barrier' 'fill 1 64 -3 1 65 13 minecraft:barrier' 'fill 0 64 13 0 65 13 minecraft:barrier' \
    'summon minecraft:wolf 0.5 64 -1.5 {NoAI:1b,PersistenceRequired:1b,Silent:1b}'
  if [[ "$view" == front ]]; then cam 0.5 64 15.5 0.5 65.0 7.0; else cam 5.5 64 7.0 0.5 65.0 7.0; fi
  spawn 0.5 1.5 0 1
  record "walk-$view" 5
done

# 5-7. Bow aim and release: immobile archer (movement 0) facing a golem 8 blocks east.
for view in front-three-quarter side; do
  clear_scene
  spawn 0.5 4.5 -90 1
  rcon "attribute $A minecraft:movement_speed base set 0"
  if [[ "$view" == side ]]; then cam 0.5 64 8.5 0.5 65.0 4.5; else cam 3.6 64 7.4 0.5 65.0 4.5; fi
  golem 8.5 4.5
  sleep 0.5
  mark "ARROWS_BEFORE aim-$view"; arrows
  count_arrows_during 7 & sampler=$!
  record "aim-$view" 7
  wait "$sampler"
  mark "ARROWS_AFTER aim-$view"; arrows
done
# 7b. Release close-up (front three-quarter, closer camera).
clear_scene
spawn 0.5 4.5 -90 1
rcon "attribute $A minecraft:movement_speed base set 0"
cam 2.4 64.2 6.6 0.5 65.2 4.5
golem 8.5 4.5
sleep 0.5
count_arrows_during 5 & sampler=$!
record release-close 5
wait "$sampler"

# Continuous: idle -> aim -> release -> recovery (target removed) in one recording.
clear_scene
spawn 0.5 4.5 -90 1
rcon "attribute $A minecraft:movement_speed base set 0"
cam 3.6 64 7.4 0.5 65.0 4.5; sleep 1
( sleep 2; mark "TARGET_ADDED"; golem 8.5 4.5; sleep 6; mark "TARGET_REMOVED"; rcon 'kill @e[type=minecraft:iron_golem]' ) & driver=$!
count_arrows_during 12 & sampler=$!
record transition-idle-aim-release-recovery 12
wait "$driver"; wait "$sampler"

# 8-9. Hurt and death (static archer, front and side).
for view in front side; do
  clear_scene
  spawn 0.5 4.5 0 0
  if [[ "$view" == front ]]; then cam 0.5 64 7.5 0.5 65.0 4.5; else cam 3.5 64 4.5 0.5 65.0 4.5; fi
  sleep 1
  ( sleep 0.5; mark HURT; rcon "damage $A 2 minecraft:generic" ) & act=$!
  record "hurt-$view" 2.5; wait "$act"
  ( sleep 0.5; mark DEATH; rcon "damage $A 1000 minecraft:generic" ) & act=$!
  record "death-$view" 3; wait "$act"
done
clear_scene
mark SEQUENCES_DONE

#!/usr/bin/env bash
# P2-D2 Crystal Bug animation sequences in the real client: timestamped frames + MP4 per scene (sequence_frames.py).
# Behaviour comes only from the bug's existing vanilla silverfish goals: hurting it "by" an immobile, very durable
# iron golem lets HurtByTargetGoal + MeleeAttackGoal walk it to the golem and attack; killing or moving the golem
# makes it stop or turn. The viewer is a spectator, so the bug never targets the camera.
set -euo pipefail
OUT=${OUT:-crystal-bug-results}
export OUT
EV="$OUT/sequence-events.log"
B='@e[type=moreminecrafts:crystal_bug,tag=bug_seq,limit=1]'
G='@e[type=minecraft:iron_golem,tag=bug_target,limit=1]'
rcon() { python3 scripts/visual_validation/rcon.py "$@" >> "$EV"; }
mark() { printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)" "$*" >> "$EV"; }
record() { mark "BEGIN $1"; python3 scripts/crystal-bug-validation/sequence_frames.py "$1" "$2"; mark "END $1"; }
cam() { # cam x y z target_x target_y target_z
  read -r yaw pitch < <(python3 -c "import math;dx=$4-($1);dz=$6-($3);dy=$5-($2+1.62);print(f'{math.degrees(math.atan2(-dx,dz)):.1f} {math.degrees(-math.atan2(dy,math.hypot(dx,dz))):.1f}')")
  rcon "tp BugViewer $1 $2 $3 $yaw $pitch"
}
clear_scene() {
  rcon 'kill @e[type=moreminecrafts:crystal_bug]' 'kill @e[type=minecraft:iron_golem]' \
    'kill @e[type=minecraft:item]' 'kill @e[type=minecraft:experience_orb]'
  sleep 1.5
}
spawn() { # spawn x z yaw ai(0|1)
  local noai=1; [[ "$4" == 1 ]] && noai=0
  rcon "summon moreminecrafts:crystal_bug $1 64 $2 {NoAI:${noai}b,Silent:1b,PersistenceRequired:1b,Rotation:[$3f,0f],Tags:[\"bug_seq\"]}"
}
golem() {
  rcon "summon minecraft:iron_golem $1 64 $2 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:[\"bug_target\"]}" \
    "attribute $G minecraft:max_health base set 1000" "data merge entity $G {Health:1000.0f}" \
    "attribute $G minecraft:knockback_resistance base set 1"
}
provoke() { mark PROVOKE; rcon "damage $B 0.5 minecraft:mob_attack by $G"; }

rcon 'gamemode spectator BugViewer' 'time set noon' 'weather clear' 'fill -12 64 -12 12 75 16 minecraft:air' \
  'fill -12 63 -12 12 63 16 minecraft:stone'
clear_scene

# 1. Idle (AI-free bug: breathing and small segment drift only).
spawn 0.5 4.5 0 0
cam 4.0 64.6 8.0 0.5 64.6 4.5; sleep 2; record idle-three-quarter 5
cam 6.5 64.2 4.5 0.5 64.6 4.5; sleep 1; record idle-side 4

# 2-3. Walking toward the golem (side and 45 degrees); the clip ends at the first attacks.
for view in side three-quarter; do
  clear_scene
  golem 0.5 13.5
  spawn 0.5 -4.0 0 1
  if [[ "$view" == side ]]; then cam 10.0 64.6 4.0 0.5 64.6 4.0; else cam 8.0 65.2 11.0 0.5 64.6 3.5; fi
  sleep 1; provoke; sleep 0.6
  record "walk-$view" 7
done

# 4. Turning: the golem jumps to the side, then behind the bug.
clear_scene
golem 0.5 12.5
spawn 0.5 2.0 0 1
cam 7.0 67.0 10.0 0.5 64.6 4.0
sleep 1; provoke; sleep 0.6
( sleep 1.4; mark GOLEM_EAST; rcon "tp $G 9.5 64 6.5"; sleep 2.6; mark GOLEM_WEST; rcon "tp $G -8.5 64 -2.5" ) & driver=$!
record turning 8
wait "$driver"

# 5. Attack: the bug starts beside the golem and attacks with the vanilla melee cadence.
clear_scene
golem 0.5 9.0
spawn 0.5 5.5 0 1
cam 5.0 64.4 5.5 0.5 64.6 6.5
sleep 1; provoke; sleep 0.6
record attack-side 6
cam 3.8 64.6 3.0 0.5 64.6 7.0
record attack-three-quarter 4

# 6-7. Hurt and death (AI-free bug, three-quarter and side).
for view in three-quarter side; do
  clear_scene
  spawn 0.5 4.5 0 0
  if [[ "$view" == side ]]; then cam 6.5 64.2 4.5 0.5 64.6 4.5; else cam 4.0 64.6 8.0 0.5 64.6 4.5; fi
  sleep 1.5
  ( sleep 0.5; mark HURT; rcon "damage $B 2 minecraft:generic" ) & act=$!
  record "hurt-$view" 2.5; wait "$act"
  sleep 0.6
  ( sleep 0.5; mark DEATH; rcon "damage $B 1000 minecraft:generic" ) & act=$!
  record "death-$view" 3; wait "$act"
done

# 8. Walk-to-idle: the target is removed mid-walk and the bug settles back into idle.
clear_scene
golem 0.5 13.5
spawn 0.5 -4.0 0 1
cam 9.0 64.8 3.0 0.5 64.6 3.0
sleep 1; provoke; sleep 0.6
( sleep 2.5; mark TARGET_REMOVED; rcon 'kill @e[type=minecraft:iron_golem]' ) & driver=$!
record walk-to-idle 7
wait "$driver"
clear_scene
mark SEQUENCES_DONE

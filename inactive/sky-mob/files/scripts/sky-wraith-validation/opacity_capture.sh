#!/usr/bin/env bash
# Fixed camera, light, mob position and panels for opaque/translucent comparisons.
set -euo pipefail
rcon() { python3 scripts/visual_validation/rcon.py "$@" >> sky-wraith-results/opacity-scene.log; }
rcon 'gamemode spectator WraithViewer' \
  'data merge entity @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] {NoGravity:1b,NoAI:1b}' \
  'tp @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] 0.5 74 4.5 0 0' \
  'tp @e[type=minecraft:husk,tag=wraith_comparison,limit=1] -5 64 5'
for backdrop in sky dark stone; do
  case "$backdrop" in
    sky) material=minecraft:air ;;
    dark) material=minecraft:black_concrete ;;
    stone) material=minecraft:stone ;;
  esac
  rcon "fill -8 70 0 8 81 0 $material" "fill -5 70 -5 -5 81 13 $material"
  for view in front side; do
    if [[ "$view" == front ]]; then rcon 'tp WraithViewer 0.5 72 8 180 -20';
    else rcon 'tp WraithViewer 4 72 4.5 90 -20'; fi
    sleep 2
    name="opacity-${backdrop}-${view}"
    scrot "sky-wraith-results/$name.png"
    echo "SCREENSHOT_BASE64_BEGIN $name"
    base64 "sky-wraith-results/$name.png"
    echo "SCREENSHOT_BASE64_END $name"
    if [[ "$view" == front && ( "$backdrop" == sky || "$backdrop" == dark ) ]]; then
      /usr/bin/python3 scripts/sky-wraith-validation/sequence_frames.py "particles-${backdrop}" 5
    fi
  done
done
rcon 'fill -8 70 0 8 81 0 minecraft:air' 'fill -5 70 -5 -5 81 13 minecraft:air' \
  'data merge entity @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] {NoGravity:0b}' \
  'tp @e[type=moreminecrafts:cloud_wraith,tag=wraith_visual,limit=1] 0.5 64 4.5 0 0'

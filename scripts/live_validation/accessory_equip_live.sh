#!/usr/bin/env bash
# Real-client accessory equip check against a running dedicated server.
#
# Drives the survival inventory with OS pointer input (XTest) and verifies the SERVER-side accessory attachment over
# RCON, so client prediction alone can never pass. Needs: a Minecraft client already joined to the server as $PLAYER
# on DISPLAY, GUI scale 2, the 1280x720 window, the server RCON settings below, xdotool and python3.
#
#   RCON_PASSWORD=... PLAYER=Alice DISPLAY=:77 scripts/live_validation/accessory_equip_live.sh [results-dir]
#
# Start with the player in the world and no screen open. Exit 0 only if every check passes. The instant-click control is classified, not required to equip (see below).
set -euo pipefail

RESULTS="${1:-live-validation-results}"
PLAYER="${PLAYER:-Alice}"
HERE="$(cd "$(dirname "$0")" && pwd)"
RCON="$HERE/../visual_validation/rcon.py"
mkdir -p "$RESULTS"
# shellcheck source=../visual_validation/x11_input.sh
source "$HERE/../visual_validation/x11_input.sh"

LOG="$RESULTS/accessory-equip.log"
: > "$LOG"
FAILED=0
say() { echo "$*" | tee -a "$LOG"; }
check() { if [ "$2" = "ok" ]; then say "PASS  $1"; else say "FAIL  $1 -- $3"; FAILED=1; fi; }
rcon() { python3 "$RCON" "$@"; }

# Find and focus the single Minecraft window; every pointer move is window-relative (root coordinates were observed to
# drift under the window manager frame).
discover_minecraft_window || { echo "no unique Minecraft window on DISPLAY=$DISPLAY"; exit 2; }
activate_minecraft_window || echo "WARN window focus not confirmed (activate=$X11_WINDOWACTIVATE_STATUS focus=$X11_WINDOWFOCUS_STATUS)"
W="$X11_GAME_WINDOW_WIDTH"; H="$X11_GAME_WINDOW_HEIGHT"
move_to() { xdotool mousemove --window "$X11_GAME_WINDOW_ID" --sync "$1" "$2"; sleep 0.5; }

# GUI scale 2: the inventory texture (176x166) is centred in the window, slot item origin + 8 px = centre.
SCALE=2
LEFT=$(( (W - 176 * SCALE) / 2 )); TOP=$(( (H - 166 * SCALE) / 2 ))
slot_x() { echo $(( LEFT + ($1) * SCALE + 8 * SCALE )); }
slot_y() { echo $(( TOP + ($1) * SCALE + 8 * SCALE )); }
ACC_X=$(slot_x -18)
acc_y() { slot_y $((8 + $1 * 18)); }          # accessory slot n (combat, defense, support, exploration, lifestyle)
hotbar_x() { slot_x $((8 + $1 * 18)); }
HOTBAR_Y=$(slot_y 142)
ROW1_Y=$(slot_y 84)

EMPTY="- - - - -"
ITEMS=(star_covenant ember_ring seed_of_life snow_charm whisper_of_the_sea)   # category order = slot order


click_at() { move_to "$1" "$2"; x11_click_held 0.1; sleep 1.0; }
shift_click_at() { move_to "$1" "$2"; x11_shift_click_held 0.1; sleep 1.0; }
instant_click_at() { move_to "$1" "$2"; xdotool click 1; sleep 1.0; }

# Server-side loadout as five ids ('-' for empty).
loadout() {
  rcon "data get entity $PLAYER \"fabric:attachments\".\"moreminecrafts:accessory_equipment\"" | python3 -c '
import re,sys
t=sys.stdin.read().split("data: ",1)[-1]
body=t[t.index("["):]
parts=re.findall(r"\{[^{}]*\}",body)
print(" ".join((re.search(r"id: \"moreminecrafts:([a-z_]+)\"",p).group(1) if "id:" in p else "-") for p in parts))'
}
# Items the server holds in the player inventory (not carried, not equipped).
held_ids() {
  rcon "data get entity $PLAYER Inventory" | python3 -c '
import re,sys
print(" ".join(sorted(re.findall(r"id: \"moreminecrafts:([a-z_]+)\"",sys.stdin.read()))))'
}
open_inventory() { xdotool key e; sleep 1.5; }
close_inventory() { xdotool key Escape; sleep 1.5; }
shot() { import -window root "$RESULTS/$1.png" 2>/dev/null || true; }
# Precondition for every call: no screen is open (Escape on the bare game would open the pause menu).
reset_fixture() {   # $1 = label for the report
  open_inventory
  for n in 0 1 2 3 4; do shift_click_at "$ACC_X" "$(acc_y $n)"; done   # quick-move every equipped accessory out
  close_inventory
  rcon "clear $PLAYER" >/dev/null || true                                # accessory slots are not cleared by /clear
  for item in "${ITEMS[@]}"; do rcon "give $PLAYER moreminecrafts:$item" >/dev/null; done
  check "fixture reset: server loadout empty ($1)" "$([ "$(loadout)" = "$EMPTY" ] && echo ok || echo no)" "$(loadout)"
}

say "== accessory equip live validation player=$PLAYER window=${X11_GAME_WINDOW_WIDTH}x${X11_GAME_WINDOW_HEIGHT} scale=$SCALE $(date -u +%FT%TZ)"

# --- A: realistic held click, all five slots, verified on the server ---------------------------------------------
reset_fixture A
open_inventory
expected=()
for n in 0 1 2 3 4; do
  click_at "$(hotbar_x "$n")" "$HOTBAR_Y"       # pick the accessory up from hotbar slot n
  click_at "$ACC_X" "$(acc_y "$n")"             # place it in its category slot
  expected+=("${ITEMS[$n]}")
  got="$(loadout)"
  want="$(printf '%s ' "${expected[@]}")"; want="$want$(for _ in $(seq $((n + 1)) 4); do printf -- '- '; done)"
  check "held click slot $n equips ${ITEMS[$n]} on the server" "$([ "$got" = "${want% }" ] && echo ok || echo no)" "got='$got' want='${want% }'"
done
shot "held-all-five-open"
check "no accessory left in the player inventory or cursor" "$([ -z "$(held_ids)" ] && echo ok || echo no)" "inventory holds: $(held_ids)"
close_inventory
check "equipped after closing the inventory" "$([ "$(loadout)" = "star_covenant ember_ring seed_of_life snow_charm whisper_of_the_sea" ] && echo ok || echo no)" "$(loadout)"
open_inventory; shot "held-all-five-reopened"; close_inventory
check "equipped after reopening the inventory" "$([ "$(loadout)" = "star_covenant ember_ring seed_of_life snow_charm whisper_of_the_sea" ] && echo ok || echo no)" "$(loadout)"

# --- B: wrong category stays unequipped ---------------------------------------------------------------------------
reset_fixture B
open_inventory
click_at "$(hotbar_x 0)" "$HOTBAR_Y"            # Star Covenant (combat) onto the defense slot
click_at "$ACC_X" "$(acc_y 1)"
check "wrong-category drop is rejected by the server" "$([ "$(loadout)" = "$EMPTY" ] && echo ok || echo no)" "$(loadout)"
click_at "$(hotbar_x 0)" "$HOTBAR_Y"            # put it back
close_inventory
check "wrong-category item returned, not lost" "$([[ " $(held_ids) " == *" star_covenant "* ]] && echo ok || echo no)" "inventory holds: $(held_ids)"

# --- C: control for the known input artifact ----------------------------------------------------------------------
# `xdotool click` reaches SDL as two presses, so the vanilla screen sends place + pick-up. This control records that
# behaviour; it does not require the unrealistic input to equip. It only requires that nothing is lost or duplicated.
# Measured on the live harness (Xvfb, SDL, Openbox): instant click equipped 0 of 12 attempts, held click 6 of 6.
reset_fixture C
open_inventory
instant_click_at "$(hotbar_x 0)" "$HOTBAR_Y"
instant_click_at "$ACC_X" "$(acc_y 0)"
after="$(loadout)"
close_inventory
total=$(( $(printf '%s\n' $(held_ids) $(loadout) | grep -c 'star_covenant') ))
check "instant-click control: Star Covenant neither lost nor duplicated (exactly one copy)" "$([ "$total" = 1 ] && echo ok || echo no)" "copies=$total"
if [ "${after%% *}" = "star_covenant" ]; then
  say "INFO  instant click equipped on this runtime (input artifact not reproduced)"
else
  say "INFO  INPUT_ARTIFACT instant click did not equip (server loadout: $after); held click is the supported input"
fi

if [ "$FAILED" -ne 0 ]; then say "RESULT FAIL"; exit 1; fi
say "RESULT PASS"

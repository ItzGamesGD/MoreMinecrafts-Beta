#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="$1"
if [[ -z "$RESULTS_DIR" ]]; then RESULTS_DIR="client-live-qa-results/bootstrap"; fi
mkdir -p "$RESULTS_DIR"

if [[ ! -v GRADLE_USER_HOME ]]; then GRADLE_USER_HOME="$PWD/.gradle"; fi
export GRADLE_USER_HOME

./gradlew compileClientQaJava verifyClientQaExcludedFromRemapJar --no-daemon

# Optional renderer integration is pinned and integrity-checked in the isolated QA runtime only.
# A download or metadata failure marks the Lantern case blocked but does not stop other cases.
mkdir -p run/mods
python3 - <<'PYLAMB' "$PWD/run/mods" "$PWD/$RESULTS_DIR/lambdynamiclights-install.json"
import hashlib, json, pathlib, sys, urllib.request
mods = pathlib.Path(sys.argv[1])
report = pathlib.Path(sys.argv[2])
version_id = "rylKnJ4M"
try:
    with urllib.request.urlopen(f"https://api.modrinth.com/v2/version/{version_id}", timeout=30) as response:
        metadata = json.load(response)
    files = [item for item in metadata["files"] if item.get("primary") and item["filename"].endswith(".jar")]
    if not files:
        files = [item for item in metadata["files"] if item["filename"].endswith(".jar")]
    if len(files) != 1:
        raise RuntimeError(f"expected one pinned primary jar, got {len(files)}")
    artifact = files[0]
    with urllib.request.urlopen(artifact["url"], timeout=120) as response:
        data = response.read()
    expected = artifact["hashes"]["sha512"].lower()
    actual = hashlib.sha512(data).hexdigest()
    if actual != expected:
        raise RuntimeError("SHA-512 mismatch for pinned Modrinth artifact")
    destination = mods / artifact["filename"]
    destination.write_bytes(data)
    result = {"status":"INSTALLED", "modrinth_version_id":version_id, "filename":artifact["filename"], "sha512":actual, "runtime_scope":"run/mods QA only"}
except Exception as exc:
    result = {"status":"BLOCKED", "modrinth_version_id":version_id, "reason":str(exc), "runtime_scope":"run/mods QA only"}
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(json.dumps(result, sort_keys=True) + "\\n")
print("STAGE23B_LAMBDYNAMICLIGHTS_" + result["status"] + " " + json.dumps(result, sort_keys=True))
PYLAMB

mkdir -p run
printf 'eula=true\n' > run/eula.txt
RCON_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')"
export RCON_PASSWORD
cat > run/server.properties <<EOF
level-name=visual-validation-world
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
max-players=2
motd=MoreMinecrafts isolated Stage 23-B client QA
EOF

MOREMINECRAFTS_VISUAL_VALIDATION=1 setsid timeout 900s \
  ./gradlew -I scripts/visual_validation/visual-validation.init.gradle runServer --args="--safeMode" \
  > "$RESULTS_DIR/test-server.log" 2>&1 &
server_pid=$!
cleanup_server() {
  kill -TERM -- "-$server_pid" 2>/dev/null || true
  wait "$server_pid" 2>/dev/null || true
}
trap cleanup_server EXIT

server_ready=0
for attempt in $(seq 1 150); do
  if grep -Fq "Done (" "$RESULTS_DIR/test-server.log"; then server_ready=1; break; fi
  if ! kill -0 "$server_pid" 2>/dev/null; then
    cat "$RESULTS_DIR/test-server.log"
    echo "Deterministic QA server exited before readiness." >&2
    exit 1
  fi
  sleep 2
done
if [[ "$server_ready" -ne 1 ]]; then
  cat "$RESULTS_DIR/test-server.log"
  echo "Timed out waiting for the deterministic QA server." >&2
  exit 1
fi

python3 scripts/visual_validation/rcon.py \
  "setworldspawn 0 64 12" \
  "forceload add -10 -10 10 18" \
  "time set noon" \
  "weather clear 1000000" \
  "fill -10 63 -10 10 63 18 minecraft:stone_bricks" \
  "fill -10 64 -10 10 80 18 minecraft:air" \
  "fill -5 64 4 5 64 4 minecraft:stone_bricks" \
  "setblock 0 64 4 minecraft:enchanting_table"

vulkan_icd=""
if [[ -d /usr/share/vulkan/icd.d ]]; then
  vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name 'lvp_icd*.json' -print -quit)"
fi
if [[ -n "$vulkan_icd" ]]; then export VK_ICD_FILENAMES="$vulkan_icd"; fi

set +e
setsid timeout 600s xvfb-run -a \
  -s "-screen 0 1280x720x24 +extension GLX +render -noreset" \
  env MOREMINECRAFTS_QA_RESULTS_DIR="$RESULTS_DIR" bash -euo pipefail -c '
    results_dir="$MOREMINECRAFTS_QA_RESULTS_DIR"
    openbox --sm-disable > "$results_dir/openbox.log" 2>&1 &
    window_manager_pid=$!
    trap "kill $window_manager_pid 2>/dev/null || true; wait $window_manager_pid 2>/dev/null || true" EXIT
    sleep 2
    export LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
    export MOREMINECRAFTS_CLIENT_QA=1
    export MOREMINECRAFTS_CLIENT_QA_SERVER_ADDRESS=127.0.0.1:25565
    ./gradlew runClient --args="--width 1280 --height 720 --username ClientQaProbe"
  ' > "$RESULTS_DIR/qa-mod-load.log" 2>&1 &
client_pid=$!
joined=0
for attempt in $(seq 1 180); do
  if grep -Fq "STAGE23B_QA_WORLD_JOINED" "$RESULTS_DIR/qa-mod-load.log"; then joined=1; break; fi
  if ! kill -0 "$client_pid" 2>/dev/null; then break; fi
  sleep 2
done
if [[ "$joined" -eq 1 ]]; then
  python3 scripts/visual_validation/rcon.py \
    "give ClientQaProbe moreminecrafts:large_backpack 1" \
    "give ClientQaProbe moreminecrafts:portable_furnace 1" \
    "give ClientQaProbe moreminecrafts:flame_sword 1" \
    "give ClientQaProbe moreminecrafts:iron_greatsword 1" \
    "give ClientQaProbe moreminecrafts:miner_lantern 1" \
    "give ClientQaProbe minecraft:stone 32" \
    "give ClientQaProbe minecraft:raw_iron 2" \
    "give ClientQaProbe minecraft:coal 2" \
    "tp ClientQaProbe 0.5 64 12.5 0 0" \
    "summon minecraft:armor_stand 0.5 64 15 {CustomName:'{\"text\":\"QA_GS_HIT\"}',Tags:[\"stage23b_gs_hit\"],NoGravity:1b,NoBasePlate:1b}" \
    "summon minecraft:armor_stand 8.5 64 15 {CustomName:'{\"text\":\"QA_GS_MISS\"}',Tags:[\"stage23b_gs_miss\"],NoGravity:1b,NoBasePlate:1b}"
fi
for attempt in $(seq 1 180); do
  if [[ -s run/client-qa-results/scenario-results.jsonl ]]; then break; fi
  if ! kill -0 "$client_pid" 2>/dev/null; then break; fi
  sleep 2
done
kill -TERM -- "-$client_pid" 2>/dev/null || true
wait "$client_pid"
client_status=$?
set -e

if ! grep -Fq "STAGE23B_QA_MOD_LOADED mod=moreminecrafts_client_qa" "$RESULTS_DIR/qa-mod-load.log"; then
  cat "$RESULTS_DIR/qa-mod-load.log"
  echo "The isolated client QA mod did not emit its development-load marker." >&2
  exit 1
fi

scenario_report="run/client-qa-results/scenario-results.jsonl"
if [[ ! -s "$scenario_report" ]]; then
  cat "$RESULTS_DIR/qa-mod-load.log"
  echo "The isolated client QA mod did not write its independent scenario ledger." >&2
  exit 1
fi
cp "$scenario_report" "$RESULTS_DIR/scenario-results.jsonl"
mkdir -p "$RESULTS_DIR/screenshots"
for screenshot in run/screenshots/stage23b_*.png; do
  [[ -s "$screenshot" ]] || continue
  cp "$screenshot" "$RESULTS_DIR/screenshots/"
done
find "$RESULTS_DIR/screenshots" -maxdepth 1 -type f -name '*.png' -printf '%f %s bytes\n' \
  > "$RESULTS_DIR/screenshot-manifest.txt"
python3 - "$scenario_report" "$RESULTS_DIR/screenshots" <<'PYREPORT'
import json
import sys
from pathlib import Path

rows = [json.loads(line) for line in Path(sys.argv[1]).read_text().splitlines() if line.strip()]
expected = {
    "four_card_selector",
    "large_backpack_icon_and_movement",
    "flame_sword_front_back_model_texture",
    "greatsword_hit_miss_sweep_particles",
    "enchanting_gui_multiple_scales",
    "portable_furnace_inventory_cooking_persistence",
    "miner_lantern_lambdynamiclights",
}
assert len(rows) == len(expected), f"Expected 7 independent scenarios, got {len(rows)}"
assert {row["scenario"] for row in rows} == expected, "Scenario ledger is incomplete or duplicated"
assert all(row["status"] in {"PASS", "FAIL", "BLOCKED", "LIVE PENDING"} for row in rows)
assert all(row["visual_status"] == "LIVE PENDING" for row in rows), "Visual PASS requires semantic review evidence"
assert all({"os_xtest_xdotool", "physical_pointer_status", "physical_pointer_evidence", "direct_state_fixture", "human_manual_play"} <= row.keys() for row in rows)
failed = [row["scenario"] for row in rows if row["status"] == "FAIL"]
if failed:
    raise SystemExit("Scenario regression assertions failed: " + ", ".join(failed))
if not any(row.get("scenario") == "four_card_selector" and row.get("status") == "PASS" for row in rows):
    raise SystemExit("In-world selector callback did not verify the displayed destination screen")
images = list(Path(sys.argv[2]).glob("*.png"))
by_case = {row["scenario"]: row for row in rows}
for case, phases in {
    "flame_sword_front_back_model_texture": ("first_person", "third_person_back", "third_person_front"),
    "enchanting_gui_multiple_scales": ("1280x720", "854x480", "640x360"),
}.items():
    if by_case[case]["status"] == "LIVE PENDING":
        missing_views = [phase for phase in phases if not any(
            image.name.startswith(f"stage23b_{case}_{phase}_") for image in images
        )]
        if missing_views:
            raise SystemExit(f"Missing real client frames for {case}: " + ", ".join(missing_views))
if by_case["greatsword_hit_miss_sweep_particles"]["status"] == "PASS":
    for phase in ("before", "after_hit", "after_miss"):
        if not any(image.name.startswith(f"stage23b_greatsword_hit_miss_sweep_particles_{phase}_") for image in images):
            raise SystemExit(f"Greatsword state PASS lacks its {phase} rendered frame")
required_cases = (
    "portable_furnace_inventory_cooking_persistence",
    "large_backpack_icon_and_movement",
    "four_card_selector",
)
missing = [case for case in required_cases if not any(
    image.name.startswith(f"stage23b_{case}_before_") for image in images
) or not any(image.name.startswith(f"stage23b_{case}_after_") for image in images)]
if missing:
    raise SystemExit("Missing genuine before/after Minecraft frames for: " + ", ".join(missing))
print("STAGE23B_QA_SCENARIO_LEDGER_VERIFIED count=7")
print(f"STAGE23B_QA_IN_WORLD_ACCEPTANCE_VERIFIED screenshots={len(images)}")
PYREPORT

if [[ "$client_status" -ne 124 && "$client_status" -ne 0 && "$client_status" -ne 143 ]]; then
  if grep -Fq "Couldn't find matching GLX visual" "$RESULTS_DIR/qa-mod-load.log"; then
    cat > "$RESULTS_DIR/client-runtime.json" <<'JSON'
{"status":"BLOCKED","capability":"OpenGL/GLX window creation","reason":"Xvfb did not expose a matching GLX visual; the QA mod loaded and wrote all independent nonvisual fixture results before the render backend failed.","visual_status":"LIVE PENDING"}
JSON
    echo "STAGE23B_QA_RENDER_BACKEND_BLOCKED reason=missing_matching_glx_visual; scenario_fixture_ledger_preserved=true"
  else
    cat "$RESULTS_DIR/qa-mod-load.log"
    echo "Client runtime exited unexpectedly with status $client_status." >&2
    exit "$client_status"
  fi
else
  cat > "$RESULTS_DIR/client-runtime.json" <<JSON
{"status":"RESULT_LEDGER_WRITTEN_CLIENT_STOPPED","exit_code":$client_status,"visual_status":"LIVE PENDING"}
JSON
fi

echo "STAGE23B_QA_MOD_LOAD_VERIFIED"

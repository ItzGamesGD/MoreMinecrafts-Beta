#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="$1"
if [[ -z "$RESULTS_DIR" ]]; then RESULTS_DIR="client-live-qa-results/bootstrap"; fi
mkdir -p "$RESULTS_DIR"

if [[ ! -v GRADLE_USER_HOME ]]; then GRADLE_USER_HOME="$PWD/.gradle"; fi
export GRADLE_USER_HOME

./gradlew compileClientQaJava verifyClientQaExcludedFromRemapJar --no-daemon

vulkan_icd=""
if [[ -d /usr/share/vulkan/icd.d ]]; then
  vulkan_icd="$(find /usr/share/vulkan/icd.d -maxdepth 1 -name 'lvp_icd*.json' -print -quit)"
fi
if [[ -n "$vulkan_icd" ]]; then export VK_ICD_FILENAMES="$vulkan_icd"; fi

set +e
timeout 240s xvfb-run -a \
  -s "-screen 0 1280x720x24 +extension GLX +render -noreset" \
  env LIBGL_ALWAYS_SOFTWARE=1 MESA_LOADER_DRIVER_OVERRIDE=llvmpipe \
  MOREMINECRAFTS_CLIENT_QA=1 \
  ./gradlew runClient --args="--width 1280 --height 720 --username ClientQaProbe" \
  > "$RESULTS_DIR/qa-mod-load.log" 2>&1
client_status=$?
set -e

if ! grep -Fq "STAGE23B_QA_MOD_LOADED mod=moreminecrafts_client_qa" "$RESULTS_DIR/qa-mod-load.log"; then
  cat "$RESULTS_DIR/qa-mod-load.log"
  echo "The isolated client QA mod did not emit its development-load marker." >&2
  exit 1
fi

if [[ "$client_status" -ne 124 && "$client_status" -ne 0 ]]; then
  cat "$RESULTS_DIR/qa-mod-load.log"
  echo "Client startup exited unexpectedly with status $client_status." >&2
  exit "$client_status"
fi

scenario_report="run/client-qa-results/scenario-results.jsonl"
if [[ ! -s "$scenario_report" ]]; then
  cat "$RESULTS_DIR/qa-mod-load.log"
  echo "The isolated client QA mod did not write its independent scenario ledger." >&2
  exit 1
fi
cp "$scenario_report" "$RESULTS_DIR/scenario-results.jsonl"
python3 - "$scenario_report" <<'PYREPORT'
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
assert all({"os_xtest_xdotool", "direct_state_fixture", "human_manual_play"} <= row.keys() for row in rows)
failed = [row["scenario"] for row in rows if row["status"] == "FAIL"]
if failed:
    raise SystemExit("Scenario regression assertions failed: " + ", ".join(failed))
print("STAGE23B_QA_SCENARIO_LEDGER_VERIFIED count=7")
PYREPORT

echo "STAGE23B_QA_MOD_LOAD_VERIFIED"

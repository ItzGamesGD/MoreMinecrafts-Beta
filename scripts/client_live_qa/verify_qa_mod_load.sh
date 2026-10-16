#!/usr/bin/env bash
set -euo pipefail

RESULTS_DIR="$1"
if [[ -z "$RESULTS_DIR" ]]; then RESULTS_DIR="client-live-qa-results/bootstrap"; fi
mkdir -p "$RESULTS_DIR"

if [[ ! -v GRADLE_USER_HOME ]]; then GRADLE_USER_HOME="$PWD/.gradle"; fi
export GRADLE_USER_HOME

./gradlew compileClientQaJava verifyClientQaExcludedFromRemapJar --no-daemon

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

echo "STAGE23B_QA_MOD_LOAD_VERIFIED"

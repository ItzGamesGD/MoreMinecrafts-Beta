#!/usr/bin/env bash
# REPLAY-only scan (no strategies, no natural phase) of many seeds to find peak-rich seeds. usage: scan_seeds.sh <radius> <parallel> <seed...>
set -euo pipefail
radius=$1; par=$2; shift 2
here=$(cd "$(dirname "$0")" && pwd)
printf '%s\n' "$@" | xargs -P "$par" -I{} "$here/run_probe.sh" scan_{} {} world -Dmm.p2.radius="$radius" -Dmm.p2.noStrategies=true -Dmm.p2.naturalMax=0 -Dmm.p2.naturalNegatives=0 -Dmm.p2.topBlockSamples=0

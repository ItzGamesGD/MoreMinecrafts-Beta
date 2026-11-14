#!/usr/bin/env bash
# Opt-in disposable Minecraft 26.3 recovery experiment; no production worldgen.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
run="$root/build/st05-stage0/run"
out=$(mktemp -d "$root/build/st05-stage0-fix-XXXXXX")
mkdir -p "$run"
world="$(basename "$out")"
printf 'eula=true\n' > "$run/eula.txt"
properties() {
  cat > "$run/server.properties" <<EOF
level-name=$1
level-seed=20261030
online-mode=false
server-ip=127.0.0.1
server-port=0
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
EOF
}
launch() {
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st05fix.out=$out/$1.json -Dmm.st05fix.store=$out/$2 -Dmm.st05fix.phase=$1" \
    timeout 600 bash ./gradlew --no-daemon --max-workers=2 runSt05Stage0Probe > "$out/$1.log" 2>&1
  test -s "$out/$1.json"
}
properties "$world"
launch prepare wal
launch interrupt wal
launch recover wal
launch verify wal
properties "$world-order"
launch order order-wal
python3 tools/st05-stage0/verify_fix.py "$out" | tee "$out/summary.json"

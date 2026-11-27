#!/usr/bin/env bash
# P8-D2-R1 runner: run_r1.sh <worktree> <seed> <out.json> [extra -D args...]   (fresh world every run; JAVA_HOME = JDK 25)
set -euo pipefail
wt=$1; seed=$2; out=$3; shift 3
d="$wt/build/st01-p8c2b/run"; rm -rf "$d/world"; mkdir -p "$d"; rm -f "$out"
port=$(python3 -I -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')
echo eula=true > "$d/eula.txt"
printf 'level-name=world\nlevel-seed=%s\nonline-mode=false\nserver-port=%s\nview-distance=2\nsimulation-distance=2\nspawn-monsters=false\nspawn-animals=false\nenable-status=false\nmax-tick-time=-1\n' "$seed" "$port" > "$d/server.properties"
cd "$wt"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $*" timeout "${PROBE_TIMEOUT:-1500}" ./gradlew --no-daemon runSt01C2BProbe -q > "$out.log" 2>&1 || echo "gradle rc=$? ($out.log)"
[ -s "$out" ] && echo "ok $out" || { echo "NO RESULT $out"; tail -5 "$out.log"; exit 1; }

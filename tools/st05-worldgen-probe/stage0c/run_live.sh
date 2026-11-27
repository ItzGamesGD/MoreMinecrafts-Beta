#!/usr/bin/env bash
# Three separate real server processes; touches only the existing disposable probe run directory.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
run="$root/build/st05-worldgen-probe/run"
res="$root/tools/st05-worldgen-probe/stage0c/live"
mkdir -p "$run" "$res"
# Refuse accidental reuse: preserve evidence/worlds rather than deleting them.
test ! -e "$run/stage0c-world" || { echo 'stage0c-world already exists; use a clean disposable checkout'; exit 1; }
printf 'eula=true\n' > "$run/eula.txt"
cat > "$run/server.properties" <<'EOF'
level-name=stage0c-world
level-seed=7001
online-mode=false
server-port=29517
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
EOF
for mode in c-prepare c-recover c-corrupt; do
  if [ "$mode" = c-corrupt ]; then
    cp "$res/fixture-journal.json" "$res/fixture-journal-before-corruption.json"
    printf '{truncated' > "$res/fixture-journal.json"
  fi
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st05.mode=$mode -Dmm.st05.out=$res/$mode.json" \
    timeout 600 ./gradlew --no-daemon --max-workers=2 runSt05CavernProbe > "$res/$mode.log" 2>&1
  python3 - "$res/$mode.json" <<'PY'
import json,sys
p=json.load(open(sys.argv[1]))
assert 'error' not in p,p
if p['mode']=='c-recover': assert p['status']=='VERIFIED_FIXTURE' and p['new_block_writes']==2,p
if p['mode']=='c-corrupt': assert p['status']=='QUARANTINED' and p['new_block_writes']==0,p
print(p)
PY
done

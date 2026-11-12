#!/usr/bin/env bash
# Disposable real-server fixture, never production worldgen. Requires JDK25 and Gradle cache/network.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
run="$root/build/st05-stage0/run"
out=$(mktemp -d "$root/build/st05-stage0-evidence-XXXXXX")
mkdir -p "$run"
# Do not reuse, remove or overwrite a pre-existing world.
world="st05-fixture-$(basename "$out")"
printf 'eula=true\n' > "$run/eula.txt"
write_properties() {
  cat > "$run/server.properties" <<EOF
level-name=$1
level-seed=20261030
online-mode=false
server-port=0
view-distance=2
simulation-distance=2
max-tick-time=-1
enable-status=false
EOF
}
launch() {
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.st05.out=$out/$1.json -Dmm.st05.phase=$2 -Dmm.st05.reverse=$3" \
    timeout 600 bash ./gradlew --no-daemon --max-workers=2 runSt05Stage0Probe > "$out/$1.log" 2>&1
  test -s "$out/$1.json"
}
write_properties "$world"
launch forward write false
launch restart scan false
write_properties "$world-reverse"
launch reverse scan true
python3 - "$out" <<'PY'
import json, pathlib, sys
p = pathlib.Path(sys.argv[1])
a,b,c = [json.loads((p / (n+'.json')).read_text()) for n in ('forward','restart','reverse')]
assert a['seed'] == b['seed'] == c['seed'] == 20261030
assert a['block_sha256'] == b['block_sha256'] == c['block_sha256']
assert a['components'] == b['components'] == c['components']
assert a['materialization_failure_before_writes']
assert b['marker_a'] and not b['marker_b'], 'expected saved partial-write counterexample'
assert not c['marker_a'] and not c['marker_b']
print(json.dumps({'sample_order_restart_match': True, 'partial_fixture_survives_restart': True,
                  'a02': 'BLOCKER', 'a03': 'BLOCKER', 'raw_evidence': str(p)}, indent=2))
PY

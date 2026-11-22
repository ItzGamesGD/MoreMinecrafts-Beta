#!/usr/bin/env bash
# P3.9 isolated real-worldgen stage runner (NOT production; touches only build/heaven-worldgen/). Needs JAVA_HOME = JDK 25.
# usage: run_worldgen.sh <world-name> <seed> <packs: none|meadow|forest|meadow+forest> <targets csv> <mode all|west|east|none> <spawn-attempts> <label> [compare-json]
# The world's data pack (copy of heaven_worldgen_pending/, i.e. the D-option pool JSONs) is written into <world>/datapacks and enabled in level.dat
# before the server starts, so worldgen registries load with the pools exactly as a production transition would at the next world load.
set -euo pipefail
world=$1; seed=$2; packs=$3; targets=$4; mode=$5; spawn=$6; label=$7; compare=${8:-}
root=$(cd "$(dirname "$0")/../.." && pwd); base="$root/build/heaven-worldgen"; run="$base/run"
out="$base/results/${world}_${label}.json"; log="$base/logs/${world}_${label}.log"
mkdir -p "$base/results" "$base/logs" "$run" "$base/sites"
[ -f "$run/eula.txt" ] || echo "eula=true" > "$run/eula.txt"
# one world dir per world name; runs always use level-name=world, so swap by symlink-free move
store="$base/worlds/$world"; mkdir -p "$store"
rm -rf "$run/world"; [ -d "$store/world" ] && cp -a "$store/world" "$run/world"
cat > "$run/server.properties" <<PROPS
level-name=world
level-seed=$seed
online-mode=false
server-port=$((20000 + RANDOM % 20000))
view-distance=2
simulation-distance=2
spawn-monsters=true
spawn-animals=false
max-tick-time=-1
sync-chunk-writes=false
enable-status=false
PROPS
python3 -I - "$root" "$run" "$packs" <<'PY'
import sys, os, shutil, json
root, run, packs = sys.argv[1:4]
dp = os.path.join(run, 'world', 'datapacks', 'heaven_pending'); shutil.rmtree(dp, ignore_errors=True)
old = packs.startswith('old:')   # P3.12: 'old:<themes>' = a data pack that restores the PRE-transition (vanilla) pools of those themes = simulates the old build for existing-world tests
sel = packs[4:] if old else packs
want = {'none': [], 'meadow': ['meadow'], 'forest': ['forest'], 'meadow+forest': ['meadow', 'forest']}[sel]
if want:
    os.makedirs(dp); json.dump({"pack": {"description": "P3.9 isolated heaven pools", "min_format": [121, 0], "max_format": [121, 0]}}, open(dp + '/pack.mcmeta', 'w'))
    src = os.path.join(root, 'docs/design/heaven_sky_island_p3_12_2026-11-22/backup_pre_p312/template_pool') if old else os.path.join(root, 'src/main/resources/heaven_worldgen_pending/data/moreminecrafts/worldgen/template_pool/sky_islands')
    for t in want: shutil.copytree(f'{src}/{t}', f'{dp}/data/moreminecrafts/worldgen/template_pool/sky_islands/{t}')
    # mountain_meadow shares sky_islands/meadow/* so it follows the meadow pools; nothing extra is written for it
lv = os.path.join(run, 'world', 'level.dat')
if os.path.exists(lv):
    import nbtlib
    f = nbtlib.load(lv); root = f[''] if '' in f else f; dpk = root['Data']['DataPacks']
    en = [str(x) for x in dpk['Enabled'] if str(x) != 'file/heaven_pending']
    if want: en.append('file/heaven_pending')
    dis = [str(x) for x in dpk['Disabled'] if str(x) != 'file/heaven_pending']
    dpk['Enabled'] = nbtlib.List[nbtlib.String]([nbtlib.String(x) for x in en]); dpk['Disabled'] = nbtlib.List[nbtlib.String]([nbtlib.String(x) for x in dis])
    f.save()
else:
    # first start: make the pack part of the initial enabled set
    p = os.path.join(run, 'server.properties'); s = open(p).read()
    if want: s += 'initial-enabled-packs=vanilla,file/heaven_pending\n'
    open(p, 'w').write(s)
PY
rm -f "$out"
cd "$root"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dmm.heaven.out=$out -Dmm.heaven.label=$label -Dmm.heaven.targets=$targets -Dmm.heaven.mode=$mode -Dmm.heaven.spawn=$spawn -Dmm.heaven.sitefile=$base/sites/${world}.json ${compare:+-Dmm.heaven.compare=$compare}" \
  timeout "${PROBE_TIMEOUT:-1800}" ./gradlew --no-daemon ${GRADLE_ARGS:-} runHeavenWorldgenProbe -q > "$log" 2>&1 || { echo "FAILED $world/$label (see $log)"; tail -30 "$log"; exit 1; }
rm -rf "$store/world"; cp -a "$run/world" "$store/world"
python3 -I - "$out" <<'PY'
import json, sys
d = json.load(open(sys.argv[1])); print(json.dumps({k: v for k, v in d.items() if k != 'islands'}))
for n, r in d.get('islands', {}).items(): print(n, json.dumps({k: v for k, v in r.items() if k != 'templates'}) if isinstance(r, dict) else r)
PY

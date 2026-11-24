"""Heaven P3.8: option D connection change (H1=D, H6), IMPLEMENTED BUT NOT ACTIVE, plus its isolated test twin.

pending/  src/main/resources/heaven_worldgen_pending/data/moreminecrafts/worldgen/template_pool/sky_islands/<theme>/{start,satellite,tower_top}.json
          = the live pool JSON with every element location moreminecrafts:sky_islands/<theme>/<n> replaced by
          moreminecrafts:sky_islands/heaven/<theme>/<n>. Same pool IDs, weights, fallback, projection, processors. The folder is NOT
          under data/, so the game never loads it. `python3 pending_pools.py apply` copies it over the live pools (the worldgen switch,
          needs owner approval); `revert` restores the live pools exactly; `status` reports which state the live pools are in.
test twin src/gametest/resources/data/moreminecrafts-gametest/{worldgen/template_pool,structure}/heaven_d/ : the same pools under the
          gametest namespace, and copies of the heaven templates whose jigsaws target the two repointed pools, with that pool value
          retargeted to the test pool (a real run cannot repoint a live pool ID without applying D). Everything else (ruins_top,
          temple_top, minecraft:empty) is the real, shared, unchanged pool."""
import os, sys, json, gzip, io, re
import nbtlib
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p3'))
import build_meadow as bm          # noqa: E402

ROOT = bm.ROOT; D = bm.D
LIVE = os.environ.get('HEAVEN_P38_LIVE') or f'{D}/worldgen/template_pool/sky_islands'   # P3.9: override so apply/revert can be rehearsed on a copy
PENDING = os.path.join(ROOT, 'src/main/resources/heaven_worldgen_pending/data/moreminecrafts/worldgen/template_pool/sky_islands')
GT_NS = 'moreminecrafts-gametest'; GT = os.path.join(ROOT, f'src/gametest/resources/data/{GT_NS}')
THEMES = ('meadow', 'forest'); POOLS = ('start', 'satellite', 'tower_top')


def repoint(text, theme):
    return text.replace(f'"location": "moreminecrafts:sky_islands/{theme}/', f'"location": "moreminecrafts:sky_islands/heaven/{theme}/')


def revert_text(text, theme):
    return text.replace(f'"location": "moreminecrafts:sky_islands/heaven/{theme}/', f'"location": "moreminecrafts:sky_islands/{theme}/')


def generate():
    for t in THEMES:
        for p in POOLS:
            src = open(f'{LIVE}/{t}/{p}.json').read(); assert 'heaven' not in src, 'live pool already repointed'
            new = repoint(src, t); assert new != src
            out = f'{PENDING}/{t}/{p}.json'; os.makedirs(os.path.dirname(out), exist_ok=True); open(out, 'w').write(new)
    # ---- test twin
    retarget = {}
    for t in THEMES:
        for p in ('satellite', 'tower_top'): retarget[f'moreminecrafts:sky_islands/{t}/{p}'] = f'{GT_NS}:heaven_d/{t}/{p}'
    linked = {t: set() for t in THEMES}
    for t in THEMES:
        for name in bm.NAMES:
            path = f'{D}/structure/sky_islands/heaven/{t}/{name}.nbt'
            n = nbtlib.load(path); pal = [str(x['id']) for x in n['palette']]
            jig = [b for b in n['blocks'] if pal[int(b['state'])] == 'minecraft:jigsaw']
            if not any(str(b['nbt']['pool']) in retarget for b in jig): continue
            for b in jig:
                v = str(b['nbt']['pool'])
                if v in retarget: b['nbt']['pool'] = nbtlib.String(retarget[v])
            buf = io.BytesIO(); n.write(buf)
            out = f'{GT}/structure/heaven_d/{t}/{name}.nbt'; os.makedirs(os.path.dirname(out), exist_ok=True)
            open(out, 'wb').write(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0)); linked[t].add(name)

    def loc(t, name): return f'{GT_NS}:heaven_d/{t}/{name}' if name in linked[t] else f'moreminecrafts:sky_islands/heaven/{t}/{name}'

    def single(location): return {'fallback': 'minecraft:empty', 'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element',
                                  'location': location, 'processors': 'minecraft:empty', 'projection': 'rigid'}}]}
    for t in THEMES:
        for p in POOLS:
            j = json.load(open(f'{PENDING}/{t}/{p}.json'))
            for e in j['elements']:
                if 'location' in e['element']: e['element']['location'] = loc(t, e['element']['location'].split('/')[-1])
            w = f'{GT}/worldgen/template_pool/heaven_d/{t}/{p}.json'; os.makedirs(os.path.dirname(w), exist_ok=True); json.dump(j, open(w, 'w'), indent=2)
        for name in bm.NAMES:
            if name == 'tower': continue                      # the Sky Tower island is reached through large_d / large_e (tower_top)
            w = f'{GT}/worldgen/template_pool/heaven_d/{t}/single/{name}.json'; os.makedirs(os.path.dirname(w), exist_ok=True)
            json.dump(single(loc(t, name)), open(w, 'w'), indent=2)
    for t in THEMES:                                      # pre-D (vanilla) single-template start pools: the "old" side of the partial-generation checks
        for name in ('plain_small_a', 'large_b'):
            w = f'{GT}/worldgen/template_pool/heaven_d/{t}/old/{name}.json'; os.makedirs(os.path.dirname(w), exist_ok=True)
            json.dump(single(f'moreminecrafts:sky_islands/{t}/{name}'), open(w, 'w'), indent=2)
    print('linked copies', {t: sorted(v) for t, v in linked.items()})


def state():
    s = {}
    for t in THEMES:
        for p in POOLS: s[f'{t}/{p}'] = 'heaven' if f'sky_islands/heaven/{t}/' in open(f'{LIVE}/{t}/{p}.json').read() else 'vanilla'
    return s


def targets(selection):
    """P3.11: theme selection. 'meadow' (mountain_meadow follows it: it shares sky_islands/meadow/start), 'forest' or 'all'."""
    if selection not in ('meadow', 'forest', 'all'): sys.exit(f"theme must be one of meadow, forest, all (got {selection!r})")
    return [t for t in THEMES if selection in ('all', t)]


def change(mode, themes, dry):
    """apply/revert for the chosen themes. Refuses (changes nothing) when any target is neither pristine-vanilla nor exactly the pending text."""
    plan = []
    for t in themes:
        for p in POOLS:
            live = f'{LIVE}/{t}/{p}.json'; pending = open(f'{PENDING}/{t}/{p}.json').read(); vanilla = revert_text(pending, t); cur = open(live).read()
            if cur not in (pending, vanilla): sys.exit(f'REFUSED: {t}/{p} is neither the vanilla nor the pending pool; nothing changed')
            want = pending if mode == 'apply' else vanilla
            plan.append((live, cur, want, f'{t}/{p}'))
    for live, cur, want, name in plan:
        print(('would ' if dry else '') + (f'change {name}' if cur != want else f'keep   {name} (already {mode}ed state)'))
        if not dry and cur != want: open(live, 'w').write(want)


if __name__ == '__main__':
    mode = sys.argv[1] if len(sys.argv) > 1 else 'generate'
    if mode == 'generate': generate()
    elif mode == 'status': print(json.dumps(state(), indent=1))
    elif mode in ('apply', 'revert'):
        if len(sys.argv) < 3: sys.exit(f'usage: pending_pools.py {mode} <meadow|forest|all> [--dry-run]')
        change(mode, targets(sys.argv[2]), '--dry-run' in sys.argv)
        print(mode, json.dumps(state()))

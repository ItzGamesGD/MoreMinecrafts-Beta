"""Heaven P3.5-B: heaven-to-heaven jigsaw pools, built and verified in ISOLATION (gametest mod namespace only).

Design (mirrors the meadow pools 1:1, weights and fallback unchanged):
  <ns>:heaven_link/meadow/start      = sky_islands/meadow/start with every location -> the heaven version
  <ns>:heaven_link/meadow/satellite  = sky_islands/meadow/satellite (satellite_a 3, satellite_b 3, empty 4) -> heaven
  <ns>:heaven_link/meadow/tower_top  = sky_islands/meadow/tower_top (tower 1) -> heaven tower
  ruins_top / temple_top             = UNCHANGED shared pools: the ruin and temple buildings are theme-independent
                                       functional pieces (chests, vaults, trial spawners) and are reused as they are.
Jigsaw blocks keep position, orientation, name, target, joint, priorities and final_state; only `pool` is retargeted
for the satellite and tower_top links. Templates whose jigsaws need no retarget are referenced from the P3 set.

Isolation: everything is written under src/gametest/resources/data/moreminecrafts-gametest/ (loaded only in test runs,
referenced by no structure / structure set). The P3 templates in src/main are not modified.
Production equivalent (NOT written): the same three pools under data/moreminecrafts/worldgen/template_pool/heaven_p3/
meadow/ and the same pool retarget in the P3 templates - a worldgen integration step (G7, deferred)."""
import os, json, gzip, io
import nbtlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
MAIN = os.path.join(ROOT, 'src/main/resources/data/moreminecrafts')
GT_NS = 'moreminecrafts-gametest'
GT = os.path.join(ROOT, f'src/gametest/resources/data/{GT_NS}')
RETARGET = {'moreminecrafts:sky_islands/meadow/satellite': f'{GT_NS}:heaven_link/meadow/satellite',
            'moreminecrafts:sky_islands/meadow/tower_top': f'{GT_NS}:heaven_link/meadow/tower_top'}


def jigsaw_pools(path):
    n = nbtlib.load(path)
    pal = [str(p['id']) for p in n['palette']]
    return n, [b for b in n['blocks'] if pal[int(b['state'])] == 'minecraft:jigsaw']


def main():
    linked = {}
    for f in sorted(os.listdir(f'{MAIN}/structure/heaven_p3/meadow')):
        name = f[:-4]
        n, jig = jigsaw_pools(f'{MAIN}/structure/heaven_p3/meadow/{f}')
        if not any(str(b['nbt']['pool']) in RETARGET for b in jig): continue
        for b in jig:
            p = str(b['nbt']['pool'])
            if p in RETARGET: b['nbt']['pool'] = nbtlib.String(RETARGET[p])
        buf = io.BytesIO(); n.write(buf)
        out = f'{GT}/structure/heaven_link/meadow/{name}.nbt'; os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, 'wb') as fh: fh.write(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0))
        linked[name] = [str(b['nbt']['pool']) for b in jig]

    def loc(old):
        name = old.split('/')[-1]
        return f'{GT_NS}:heaven_link/meadow/{name}' if name in linked else f'moreminecrafts:heaven_p3/meadow/{name}'

    pools = {}
    for p in ('start', 'satellite', 'tower_top'):
        j = json.load(open(f'{MAIN}/worldgen/template_pool/sky_islands/meadow/{p}.json'))
        for e in j['elements']:
            if 'location' in e['element']: e['element']['location'] = loc(e['element']['location'])
        pools[p] = j
    # test-only start pools (one template each) so a GameTest can assemble a chosen island deterministically
    def single(location): return {'fallback': 'minecraft:empty', 'elements': [{'weight': 1, 'element': {
        'element_type': 'minecraft:single_pool_element', 'location': location, 'processors': 'minecraft:empty', 'projection': 'rigid'}}]}
    for name in ('large_d', 'plain_medium_a', 'ruins'):
        pools[f'test/{name}'] = single(loc(name))
    pools['test/temple'] = single('moreminecrafts:heaven_p3/meadow/temple')
    pools['test/unlinked_plain_medium_a'] = single('moreminecrafts:heaven_p3/meadow/plain_medium_a')   # P3 as-is -> vanilla satellites
    for p, j in pools.items():
        out = f'{GT}/worldgen/template_pool/heaven_link/meadow/{p}.json'; os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, 'w') as fh: json.dump(j, fh, indent=2); fh.write('\n')
    print('linked templates', json.dumps(linked, indent=1))
    print('pools', sorted(pools))


if __name__ == '__main__':
    main()

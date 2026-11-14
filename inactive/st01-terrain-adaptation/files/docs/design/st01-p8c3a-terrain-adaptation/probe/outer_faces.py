"""Outer-face composition of the ST-01 assembly underground body (template Y-32..Y0) — what a Policy B skirt exposes."""
import sys, json
from collections import Counter
from pathlib import Path
ROOT = Path(sys.argv[1]); sys.path.insert(0, str(ROOT / 'tools/st01-g0'))
from audit import nbt as read_nbt
res = {}
for v in ('east', 'west'):
    t = read_nbt((ROOT / f'src/main/resources/data/moreminecrafts/structure/pyramid/st01/assembly_{v}.nbt').read_bytes())
    t = t.get('', t) if isinstance(t, dict) and '' in t else t
    pal = [(p.get('Name') or p.get('id')) for p in t['palette']]
    size = t['size']
    grid = {}
    for b in t['blocks']:
        x, y, z = b['pos']; grid[(x, y, z)] = pal[b['state']]
    W = size[0] - 1; D = size[2] - 1
    out = {}
    for side, cond in (('x0', lambda x, z: x == 0), ('xW', lambda x, z: x == W), ('z0', lambda x, z: z == 0), ('zD', lambda x, z: z == D)):
        c = Counter(); opening_layers = Counter()
        for y in range(0, 33):  # template y 0..32 == world Y-32..Y0
            for x in range(size[0]):
                for z in range(size[2]):
                    if not cond(x, z): continue
                    n = grid.get((x, y, z), 'minecraft:structure_void(absent)')
                    c[n] += 1
                    if n in ('minecraft:air', 'minecraft:cave_air', 'minecraft:structure_void(absent)', 'minecraft:structure_void') or 'door' in n or 'glass' in n or 'fence' in n or 'bars' in n:
                        opening_layers[y - 32] += 1
        out[side] = {'cells': sum(c.values()), 'non_solid_or_open': sum(opening_layers.values()),
                     'open_by_Y': dict(sorted(opening_layers.items())), 'top_blocks': c.most_common(6)}
    res[v] = {'size': size, 'faces': out}
print(json.dumps(res, indent=1))

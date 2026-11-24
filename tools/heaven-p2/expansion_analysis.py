"""P2-E: satellite jigsaw inventory of the meadow templates and terrain-shape reuse across the six themes.
Read-only over src/main/resources/data/moreminecrafts/structure/sky_islands/."""
import os, sys, json, glob, collections
import numpy as np, nbtlib
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
BASE = os.path.join(ROOT, 'src/main/resources/data/moreminecrafts/structure/sky_islands')
OUT = os.path.join(ROOT, 'docs/design/heaven_sky_island_p2_2026-11-20/data')
THEMES = ['meadow', 'forest', 'snowy', 'desert', 'badlands', 'rocky']
VEG = ('leaves', 'grass', 'fern', 'flower', 'poppy', 'daisy', 'bluet', 'allium', 'dandelion', 'cornflower', 'tulip', 'bush', 'cactus',
       'log', 'snow', 'water', 'jigsaw', 'air', 'vine', 'sapling', 'mushroom', 'lily', 'kelp', 'seagrass')


def load(path):
    n = nbtlib.load(path); pal = [p for p in n['palette']]
    return n, pal


def jigsaws():
    out = {}
    for f in sorted(glob.glob(f'{BASE}/meadow/*.nbt')):
        n, pal = load(f); name = os.path.basename(f)[:-4]; size = [int(v) for v in n['size']]; js = []
        for b in n['blocks']:
            p = pal[int(b['state'])]
            if str(p['id']) != 'minecraft:jigsaw': continue
            pos = [int(v) for v in b['pos']]; nbt = b.get('nbt', {})
            face = 'west' if pos[0] == 0 else 'east' if pos[0] == size[0] - 1 else 'north' if pos[2] == 0 else 'south' if pos[2] == size[2] - 1 else 'top' if pos[1] == size[1] - 1 else 'inner'
            js.append(dict(pos=pos, orientation=str(p.get('properties', {}).get('orientation', '')), face=face,
                           pool=str(nbt.get('pool', '')), name=str(nbt.get('name', '')), target=str(nbt.get('target', '')),
                           joint=str(nbt.get('joint', '')), final_state=str(nbt.get('final_state', ''))))
        out[name] = dict(size=size, jigsaws=js)
    return out


def terrain(path):
    n, pal = load(path)
    ids = [str(p['id']).replace('minecraft:', '') for p in pal]
    pts = set()
    for b in n['blocks']:
        i = ids[int(b['state'])]
        if any(k in i for k in VEG): continue
        pts.add(tuple(int(v) for v in b['pos']))
    return [int(v) for v in n['size']], pts


def theme_shapes():
    names = sorted(os.path.basename(f)[:-4] for f in glob.glob(f'{BASE}/meadow/*.nbt'))
    res = {}
    for name in names:
        if name == 'tower': continue
        sm, m = terrain(f'{BASE}/meadow/{name}.nbt'); row = {}
        for th in THEMES[1:]:
            p = f'{BASE}/{th}/{name}.nbt'
            if not os.path.exists(p): row[th] = None; continue
            st, t = terrain(p)
            row[th] = dict(same_size=st == sm, iou=round(len(m & t) / max(len(m | t), 1), 3))
        res[name] = dict(size=sm, terrain=len(m), vs=row)
    return res


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    j = jigsaws(); s = theme_shapes()
    json.dump(dict(jigsaws=j, theme_shape_comparison=s), open(f'{OUT}/expansion_analysis.json', 'w'), indent=1)
    print('templates with jigsaws:', sum(1 for v in j.values() if v['jigsaws']), '/', len(j))
    for k, v in j.items():
        print('%-15s %s' % (k, [(x['face'], x['pos'], x['pool'].split('/')[-1], x['joint'], x['final_state']) for x in v['jigsaws']]))
    ious = [vv['iou'] for v in s.values() for vv in v['vs'].values() if vv]
    print('theme IoU vs meadow: min %.3f mean %.3f max %.3f, same size %d/%d' % (min(ious), np.mean(ious), max(ious),
          sum(1 for v in s.values() for vv in v['vs'].values() if vv and vv['same_size']), len(ious)))

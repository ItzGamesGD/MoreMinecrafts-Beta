"""P2.5 static analysis (read-only over src/main/resources).

1. giant-flower connectivity in the P2 island (breaking policies A/B/C)
2. jigsaw inventory over every sky-island / sky-tower template, final_state classification, pool targets
3. 6-theme x 19-template survey: size, terrain, surface, plantable area, structures, functional blocks,
   jigsaws, giant-flower placement simulation (V5 rules + structure masks) and estimated P3 resources
Writes docs/design/heaven_sky_island_p2_5_2026-11-20/data/*.json."""
import os, sys, json, glob, collections, math
import numpy as np, nbtlib
from scipy import ndimage

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
RES = os.path.join(ROOT, 'src/main/resources/data/moreminecrafts')
OUT = os.path.join(ROOT, 'docs/design/heaven_sky_island_p2_5_2026-11-20/data')
THEMES = ['meadow', 'forest', 'snowy', 'desert', 'badlands', 'rocky']
VEG_KEYS = ('leaves', 'short_grass', 'tall_grass', 'fern', 'poppy', 'daisy', 'bluet', 'allium', 'dandelion', 'cornflower', 'tulip', 'dead_bush',
            'cactus', '_log', 'sapling', 'mushroom', 'vine', 'lily', 'kelp', 'seagrass', 'sweet_berry', 'moss_carpet', 'flower')
FUNCTIONAL_KEYS = ('chest', 'barrel', 'spawner', 'vault', 'lantern', 'sea_lantern', 'torch', 'structure_block', 'trial', 'bell', 'furnace',
                   'lectern', 'brewing', 'anvil', 'enchanting', 'beacon', 'campfire', 'copper', 'button', 'lever', 'door', 'trapdoor', 'ladder')


def load(path):
    n = nbtlib.load(path)
    pal = [(str(p['id']), {k: str(v) for k, v in p.get('properties', {}).items()}) for p in n['palette']]
    blocks = []
    for b in n['blocks']:
        bid, props = pal[int(b['state'])]
        blocks.append((tuple(int(v) for v in b['pos']), bid, props, b.get('nbt')))
    return [int(v) for v in n['size']], blocks


def short(bid): return bid.split(':', 1)[1]


def is_veg(bid): return any(k in short(bid) for k in VEG_KEYS) or bid in ('minecraft:snow',)


# ------------------------------------------------------------------ 1. giant flower connectivity
def giant_connectivity():
    size, blocks = load(f'{RES}/structure/heaven_prototype/plain_medium_d_v5_p2.nbt')
    manifest = json.load(open(f'{RES}/heaven/manifest_p2.json'))
    giant = {p for p, b, _, _ in blocks if b.startswith('moreminecrafts:heaven_giant_')}
    kind = {p: short(b) for p, b, _, _ in blocks if b.startswith('moreminecrafts:heaven_giant_')}
    occ = np.zeros(size, bool)
    for p in giant: occ[p] = True
    res = {}
    for conn, st in (('6', ndimage.generate_binary_structure(3, 1)), ('18', ndimage.generate_binary_structure(3, 2)), ('26', np.ones((3, 3, 3)))):
        lab, n = ndimage.label(occ, structure=st)
        sizes = sorted(np.bincount(lab.ravel())[1:].tolist(), reverse=True)
        res[f'{conn}-connectivity'] = dict(components=int(n), sizes=sizes[:10])
    # per species: flood fill from the stem base (option B) and BFS depth (option C)
    lab26, _ = ndimage.label(occ, structure=np.ones((3, 3, 3)))
    per = []
    for g in manifest['giants']:
        bx, by, bz = g['base']
        start = (bx, by, bz)
        if start not in giant:
            cands = [p for p in giant if abs(p[0] - bx) <= 1 and abs(p[2] - bz) <= 1 and p[1] == by]
            start = cands[0] if cands else None
        comp = lab26[start] if start else 0
        cells = [p for p in giant if lab26[p] == comp]
        # BFS depth over 26-neighbours from the base: number of collapse "waves" for option C
        depth = {start: 0}; q = collections.deque([start])
        while q:
            c = q.popleft()
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        nb = (c[0] + dx, c[1] + dy, c[2] + dz)
                        if nb in giant and nb not in depth: depth[nb] = depth[c] + 1; q.append(nb)
        kinds = collections.Counter(kind[p] for p in cells)
        touching_other = sum(1 for p in cells for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
                             if (p[0] + d[0], p[1] + d[1], p[2] + d[2]) not in giant)
        per.append(dict(species=g['species'], base=g['base'], flood_fill_blocks=len(cells), parts=dict(kinds),
                        reached_from_base=len(depth), collapse_waves=max(depth.values()) if depth else 0))
    res['per_species'] = per
    res['island_giant_blocks'] = len(giant)
    return res


# ------------------------------------------------------------------ 2. jigsaws
def pools():
    out = {}
    for f in glob.glob(f'{RES}/worldgen/template_pool/**/*.json', recursive=True):
        rel = os.path.relpath(f, f'{RES}/worldgen/template_pool')[:-5]
        if not (rel.startswith('sky_islands') or rel.startswith('sky_tower')): continue
        j = json.load(open(f))
        out['moreminecrafts:' + rel] = [e['element'].get('location') for e in j.get('elements', []) if e['element'].get('location')]
    return out


def jigsaws(theme_slots):
    pool_map = pools()
    files = sorted(glob.glob(f'{RES}/structure/sky_islands/**/*.nbt', recursive=True) + glob.glob(f'{RES}/structure/sky_tower/**/*.nbt', recursive=True))
    inv = []
    for f in files:
        rel = os.path.relpath(f, f'{RES}/structure')[:-4]
        size, blocks = load(f)
        theme = rel.split('/')[1] if rel.count('/') >= 2 and rel.startswith('sky_islands/') else None
        terrain_ids = set(theme_slots.get(theme, {}).get('all_terrain', [])) if theme else set()
        for p, bid, props, nbt in blocks:
            if bid != 'minecraft:jigsaw': continue
            fs = str(nbt.get('final_state', 'minecraft:air')) if nbt else 'minecraft:air'
            fs_id = fs.split('[')[0]
            cls = 'air' if fs_id == 'minecraft:air' else ('theme_terrain' if fs_id in terrain_ids else
                  ('vanilla_terrain' if short(fs_id) in ('dirt', 'grass_block', 'stone', 'sand', 'gravel', 'snow_block', 'red_sand', 'podzol') else 'other'))
            pool = str(nbt.get('pool', '')) if nbt else ''
            inv.append(dict(template=rel, pos=list(p), orientation=props.get('orientation', ''), pool=pool,
                            name=str(nbt.get('name', '')) if nbt else '', target=str(nbt.get('target', '')) if nbt else '',
                            joint=str(nbt.get('joint', '')) if nbt else '', final_state=fs, final_state_class=cls,
                            pool_elements=pool_map.get(pool, [])))
    # what attaches: vanilla terrain carried by the attached templates
    attached = {}
    for loc in sorted({l for j in inv for l in j['pool_elements']}):
        path = f'{RES}/structure/{loc.split(":", 1)[1]}.nbt'
        if not os.path.exists(path): attached[loc] = 'missing'; continue
        size, blocks = load(path)
        c = collections.Counter(short(b) for _, b, _, _ in blocks)
        terr = {k: v for k, v in c.items() if k in ('grass_block', 'dirt', 'coarse_dirt', 'stone', 'andesite', 'tuff', 'podzol', 'mossy_cobblestone',
                                                      'snow_block', 'packed_ice', 'cobblestone', 'sand', 'sandstone', 'smooth_sandstone', 'cut_sandstone',
                                                      'red_sand', 'red_sandstone', 'terracotta', 'gravel')}
        attached[loc] = dict(size=size, blocks=sum(c.values()), vanilla_terrain=terr)
    return inv, attached


# ------------------------------------------------------------------ 3. template survey
def theme_slot_roles():
    """Classify each theme's terrain blocks by their mean depth below the column top (template evidence)."""
    roles = {}
    for th in THEMES:
        depth = collections.defaultdict(list); count = collections.Counter()
        for f in glob.glob(f'{RES}/structure/sky_islands/{th}/*.nbt'):
            if f.endswith('tower.nbt'): continue
            size, blocks = load(f)
            terr = [(p, b) for p, b, _, _ in blocks if not is_veg(b) and b not in ('minecraft:jigsaw', 'minecraft:water', 'minecraft:air')]
            top = {}
            for p, b in terr: top[(p[0], p[2])] = max(top.get((p[0], p[2]), -1), p[1])
            for p, b in terr:
                depth[b].append(top[(p[0], p[2])] - p[1]); count[b] += 1
        total = sum(count.values())
        rows = {}
        for b, n in count.most_common():
            if n / total < 0.005: continue
            d = float(np.mean(depth[b]))
            rows[b] = dict(share=round(n / total, 3), mean_depth=round(d, 2),
                           role='surface' if d < 0.6 else 'subsurface' if d < 2.6 else 'rock')
        roles[th] = dict(blocks=rows, all_terrain=list(rows))
    return roles


def survey(roles):
    out = {}
    for th in THEMES:
        surface_ids = {b for b, r in roles[th]['blocks'].items() if r['role'] == 'surface'}
        terrain_ids = set(roles[th]['all_terrain'])
        for f in sorted(glob.glob(f'{RES}/structure/sky_islands/{th}/*.nbt')):
            name = os.path.basename(f)[:-4]
            size, blocks = load(f)
            c = collections.Counter(b for _, b, _, _ in blocks)
            terrain = [(p, b) for p, b, _, _ in blocks if b in terrain_ids]
            structure = [(p, b) for p, b, _, _ in blocks if b not in terrain_ids and not is_veg(b) and b not in ('minecraft:jigsaw', 'minecraft:water', 'minecraft:air')]
            functional = collections.Counter(short(b) for _, b in structure if any(k in short(b) for k in FUNCTIONAL_KEYS))
            jig = [(p, (n or {}).get('pool', '')) for p, b, _, n in blocks if b == 'minecraft:jigsaw']
            X, Y, Z = size
            top = np.full((X, Z), -1); topb = {}
            for p, b, _, _ in blocks:
                if b in terrain_ids or b == 'minecraft:water':
                    if p[1] > top[p[0], p[2]]: top[p[0], p[2]] = p[1]; topb[(p[0], p[2])] = b
            foot = top >= 0
            surf = np.zeros((X, Z), bool)
            for (x, z), b in topb.items():
                if b in surface_ids: surf[x, z] = True
            water = np.zeros((X, Z), bool)
            for (x, z), b in topb.items():
                if b == 'minecraft:water': water[x, z] = True
            # structure mask: columns holding structure blocks + 2, inner jigsaws (attached tops) + 6, side jigsaws + 4
            mask = np.zeros((X, Z), bool)
            for p, _ in structure: mask[max(0, p[0] - 2):p[0] + 3, max(0, p[2] - 2):p[2] + 3] = True
            for p, pool in jig:
                inner = 0 < p[0] < X - 1 and 0 < p[2] < Z - 1
                r = 6 if inner else 4
                xs, zs = np.ogrid[:X, :Z]; mask |= (xs - p[0]) ** 2 + (zs - p[2]) ** 2 <= r * r
            plant = surf & ~mask
            edge = ndimage.distance_transform_edt(np.pad(foot, 1))[1:-1, 1:-1]
            pond = ndimage.distance_transform_edt(~water) if water.any() else np.full((X, Z), 99.0)
            # V5 giant rules: edge >= 5, pond >= 4, spacing >= 9, plant cell with its 3x3 free
            cand = [(x, z) for x in range(X) for z in range(Z) if plant[x, z] and edge[x, z] >= 5 and pond[x, z] >= 4
                    and plant[max(0, x - 1):x + 2, max(0, z - 1):z + 2].all()]
            cand.sort(key=lambda c: -edge[c])
            chosen = []
            for cnd in cand:
                if all((cnd[0] - o[0]) ** 2 + (cnd[1] - o[1]) ** 2 >= 81 for o in chosen): chosen.append(cnd)
            area = int(plant.sum())
            recommend = min(len(chosen), max(0 if area < 150 else 1, round(area / 270)))   # V5 density: 3 giants on 807 grass columns
            out[f'{th}/{name}'] = dict(
                size=size, terrain_blocks=len(terrain), surface_columns=int(surf.sum()), plantable_columns=area,
                pond_columns=int(water.sum()), structure_blocks=len(structure), structure_kinds=dict(collections.Counter(short(b) for _, b in structure).most_common(8)),
                functional_blocks=dict(functional), jigsaws=[dict(pos=list(p), pool=pool) for p, pool in jig],
                giant_positions_feasible=len(chosen), giant_recommended=int(recommend),
                est_vegetation_blocks=int(round(area * 1232 / 807)),
                est_new_models=0)
    return out


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    roles = theme_slot_roles()
    g = giant_connectivity()
    inv, attached = jigsaws(roles)
    sv = survey(roles)
    json.dump(dict(giant_connectivity=g), open(f'{OUT}/giant_connectivity.json', 'w'), indent=1)
    json.dump(dict(jigsaws=inv, attached_templates=attached), open(f'{OUT}/jigsaw_inventory.json', 'w'), indent=1)
    json.dump(dict(theme_roles=roles, templates=sv), open(f'{OUT}/template_survey.json', 'w'), indent=1)
    print(json.dumps(g, indent=1))
    cls = collections.Counter((j['final_state'], j['final_state_class']) for j in inv)
    print('jigsaws', len(inv), 'by final_state:', cls.most_common())
    for th in THEMES: print(th, {b: r['role'] for b, r in roles[th]['blocks'].items()})

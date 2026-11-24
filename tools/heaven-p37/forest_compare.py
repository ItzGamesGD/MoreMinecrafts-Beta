"""Heaven P3.7: forest design comparison (H4 palette A/B x H5 tree density V5 / forest), all from the forest ORIGINALS.

Variants (identical conditions: same generator, seeds, constraints, rules; only the two axes differ):
  A_v5      palette A (all heaven)                       tree_rate 2 / 807 columns, spacing 8   (V5)
  A_forest  palette A                                    tree_rate 6 / 807 columns, spacing 5   (forest-strengthened)
  B_v5      palette B (podzol + mossy cobblestone stay)  V5
  B_forest  palette B                                    forest-strengthened
Metrics per template: trees, canopy cover, giant flowers, vegetation, walkability, largest walkable region, openness
(share of unobstructed 8-direction, 8-block eye-height rays from walkable columns), structure-anchor reachability and
detour (BFS path / straight distance between jigsaw anchors), plus size figures. Representative templates are also
written as NBT into the isolated gametest resources (heaven_forest_cmp/<variant>/) for the placement / client checks.
Reference: the ORIGINAL forest and meadow templates' canopy cover and tree density (measured from their NBT)."""
import os, sys, json, math, collections, time, io
import numpy as np
from collections import deque
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p3'))
import build_meadow as bm          # noqa: E402

ROOT = bm.ROOT; D = bm.D; NS = bm.NS
DOC = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_7_2026-11-21')
GT = os.path.join(ROOT, 'src/gametest/resources/data/moreminecrafts-gametest/structure/heaven_forest_cmp')
REPRESENTATIVE = ('large_a', 'plain_medium_d', 'ruins', 'temple')
P = {'A': {'minecraft:grass_block': f'{NS}:heaven_grass_block', 'minecraft:dirt': f'{NS}:heaven_loam', 'minecraft:podzol': f'{NS}:heaven_loam',
           'minecraft:coarse_dirt': f'{NS}:heaven_loam', 'minecraft:stone': f'{NS}:heaven_cream_stone', 'minecraft:andesite': f'{NS}:heaven_cream_stone_pale',
           'minecraft:tuff': f'{NS}:heaven_cream_stone_pale', 'minecraft:mossy_cobblestone': f'{NS}:heaven_cream_stone_pale'}}
P['B'] = {k: v for k, v in P['A'].items() if k not in ('minecraft:podzol', 'minecraft:mossy_cobblestone')}
P['B'].update({'minecraft:podzol': 'minecraft:podzol', 'minecraft:mossy_cobblestone': 'minecraft:mossy_cobblestone'})
DENSITY = {'v5': dict(tree_rate=2.0, tree_spacing=8), 'forest': dict(tree_rate=6.0, tree_spacing=5),
           'forest_max': dict(tree_rate=9.0, tree_spacing=4)}      # reference only (aims at the original forest's 30% canopy cover); no NBT written
HARD = {f'{NS}:heaven_giant_stem', 'minecraft:oak_log', 'minecraft:cherry_leaves', f'{NS}:heaven_white_blossom_leaves'}
VEG_KEYS = ('leaves', 'short_grass', 'tall_grass', 'fern', 'poppy', 'daisy', 'bluet', 'allium', 'dandelion', 'cornflower', '_log', 'sapling', 'mushroom', 'vine',
            'sweet_berry', 'moss_carpet', 'flower', 'bush', 'petals')


def is_veg(b): return any(k in b.split(':', 1)[1] for k in VEG_KEYS)


def metrics(isl):
    X, _, Z = isl.size
    top = isl.top; grass = isl.grass
    leaf_cols = collections.defaultdict(list); hard = set()
    for (x, y, z), (bid, _, _) in isl.veg.items():
        if bid in HARD:
            hard.add((x, y, z))
        if bid.endswith('_leaves'): leaf_cols[(x, z)].append(y)
    cover = sum(1 for (x, z) in zip(*np.nonzero(grass)) if any(y > top[x, z] for y in leaf_cols.get((int(x), int(z)), [])))
    walk = np.zeros((X, Z), bool)
    for x, z in zip(*np.nonzero(grass)):
        y = int(top[x, z]); walk[x, z] = all((int(x), y + dy, int(z)) not in hard for dy in (1, 2))
    # openness: eye-height rays
    rng = np.random.default_rng(7); cols = list(zip(*np.nonzero(walk))); rng.shuffle(cols)
    dirs = [(1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)]; open_n = tot = 0
    for x, z in cols[:600]:
        y = int(top[x, z]) + 2
        for dx, dz in dirs:
            blocked = False
            for r in range(1, 9):
                if (int(x) + dx * r, y, int(z) + dz * r) in hard or (int(x) + dx * r, y - 1, int(z) + dz * r) in hard: blocked = True; break
            open_n += (not blocked); tot += 1
    # anchors = nearest walkable column to every jigsaw
    anchors = []
    for p, _, _ in isl.jigsaws:
        if walk.any():
            d = (np.arange(X)[:, None] - p[0]) ** 2 + (np.arange(Z)[None, :] - p[2]) ** 2
            d = np.where(walk, d, 10 ** 9); i = np.unravel_index(np.argmin(d), d.shape); anchors.append((int(i[0]), int(i[1])))
    # BFS with step height <= 1
    def bfs(src):
        dist = {src: 0}; q = deque([src])
        while q:
            c = q.popleft()
            for dx, dz in dirs:
                n = (c[0] + dx, c[1] + dz)
                if 0 <= n[0] < X and 0 <= n[1] < Z and walk[n] and n not in dist and abs(int(top[n]) - int(top[c])) <= 1:
                    dist[n] = dist[c] + (1.414 if dx and dz else 1.0); q.append(n)
        return dist
    reach = detour = None; pairs = 0; ok = 0; ratios = []
    if len(anchors) >= 2:
        for i, a in enumerate(anchors):
            dist = bfs(a)
            for b in anchors[i + 1:]:
                pairs += 1
                if b in dist:
                    ok += 1; e = math.hypot(a[0] - b[0], a[1] - b[1]); ratios.append(dist[b] / max(e, 1e-6))
    # largest region (4-connected, step <= 1)
    from scipy import ndimage
    lab, n = ndimage.label(walk); largest = int(np.bincount(lab.ravel())[1:].max()) if n else 0
    return dict(canopy_cover=round(cover / max(1, int(grass.sum())), 4), walkable_share=round(float(walk.sum() / max(1, grass.sum())), 4),
                largest_region_share=round(largest / max(1, int(grass.sum())), 4),
                openness=round(open_n / max(1, tot), 4), anchors=len(anchors), anchor_pairs=pairs, anchor_pairs_reachable=ok,
                mean_detour=round(float(np.mean(ratios)), 3) if ratios else None)


def original_reference():
    out = {}
    from scipy import ndimage
    for th in ('meadow', 'forest'):
        g = c = k = 0
        for name in bm.NAMES:
            if name == 'tower': continue
            n, b = bm.load(f'{D}/structure/sky_islands/{th}/{name}.nbt')
            grass = {(p[0], p[2]): p[1] for p, (i, _, _) in b.items() if i == 'minecraft:grass_block'}
            leaf = collections.defaultdict(list); logs = np.zeros((int(n['size'][0]), int(n['size'][2])), bool)
            for p, (i, _, _) in b.items():
                if i.endswith('_leaves'): leaf[(p[0], p[2])].append(p[1])
                if i.endswith('_log'): logs[p[0], p[2]] = True
            g += len(grass); c += sum(1 for cc, y in grass.items() if any(l > y for l in leaf.get(cc, [])))
            k += ndimage.label(logs, structure=np.ones((3, 3)))[1]
        out[th] = dict(grass_columns=g, canopy_cover=round(c / g, 4), trees=int(k), trees_per_807_columns=round(k / g * 807, 2))
    return out


def main():
    species = bm.species_structures(); pools = bm.attached_footprints()
    res = {'original_reference': original_reference(), 'variants': {}}
    for pal, dens in (('A', 'v5'), ('A', 'forest'), ('B', 'v5'), ('B', 'forest'), ('A', 'forest_max')):
        if True:
            var = f'{pal}_{dens}'.lower(); rows = {}; t0 = time.time()   # resource paths must be lower case
            for name in bm.NAMES:
                _, blocks = bm.load(f'{D}/structure/sky_islands/forest/{name}.nbt')
                vv = {b for _, (b, _, _) in blocks.items() if is_veg(b)}
                isl = bm.Island(name, pools, species, theme='forest', slot=P[pal], veg=vv).build(**DENSITY[dens])
                m = metrics(isl)
                fam = collections.Counter(v[2] for v in isl.veg.values())
                rows[name] = dict(plantable=int(isl.plant.sum()), grass=int(isl.grass.sum()), trees_requested=isl.tree_req, trees=len(isl.trees),
                                  giants_requested=isl.giant_req, giants=len(isl.giants), vegetation=len(isl.veg), by_family=dict(fam),
                                  survival_violations=isl.survival_violations(), **m)
                if name in REPRESENTATIVE and dens != 'forest_max':
                    bm.OUT = f'{GT}/{var}'; path, pal_n = isl.write(); rows[name]['nbt_bytes'] = os.path.getsize(path); rows[name]['palette'] = pal_n
            res['variants'][var] = dict(templates=rows, seconds=round(time.time() - t0, 1))
            tt = rows.values()
            tot = lambda k: sum(r[k] for r in tt)
            g = tot('grass')
            res['variants'][var]['totals'] = dict(
                trees=tot('trees'), giants=tot('giants'), vegetation=tot('vegetation'),
                canopy_cover=round(sum(r['canopy_cover'] * r['grass'] for r in tt) / g, 4), walkable=round(sum(r['walkable_share'] * r['grass'] for r in tt) / g, 4),
                openness=round(sum(r['openness'] * r['grass'] for r in tt) / g, 4), violations=sum(sum(r['survival_violations'].values()) for r in tt),
                anchor_pairs=tot('anchor_pairs'), anchor_pairs_reachable=tot('anchor_pairs_reachable'))
            print(var, res['variants'][var]['totals'], res['variants'][var]['seconds'], 's')
    os.makedirs(f'{DOC}/data', exist_ok=True)
    json.dump(res, open(f'{DOC}/data/forest_compare.json', 'w'), indent=1)
    print(res['original_reference'])


if __name__ == '__main__':
    main()

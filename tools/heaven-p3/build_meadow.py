"""Heaven P3-C: separate heaven versions of the 19 meadow sky-island templates.

Inputs (read only): structure/sky_islands/meadow/*.nbt (originals), structure/heaven_flora/*.nbt and
heaven/manifest_p2.json (P2 species structures + pivots), heaven/plant_layouts.json.
Outputs: structure/heaven_p3/meadow/<name>.nbt (NOT referenced by worldgen) and
docs/design/heaven_sky_island_p3_2026-11-20/data/meadow_p3.json (per-template report).

Conversion (P2.5 D / E rules, P3 brief section 5):
  terrain   P1 slot map (grass->heaven grass, dirt/coarse->loam, stone->cream stone, andesite/tuff->pale).
            A terrain-id block is STRUCTURE (kept vanilla) when it lies above its column's grass surface or above a
            structure block in its column (the Sky Tower's andesite walls) - P2.5 open issue 1.
  kept      structure / functional blocks, water, explicit air, jigsaw positions + NBT (pool, name, target, joint,
            orientation, priorities). Only final_state grass_block / dirt are mapped (air, stone_bricks, quartz kept).
  removed   the original vanilla vegetation (oak trees, grass, flowers), as in P1 / P2.
  planted   V5 recipe recomputed per template (never V5 coordinates): giants (F10 trial formula), trees, canopy
            wisteria, medium flowers, small flowers, grass, petals, cliff wisteria / vines, with the V5 densities.
Constraints: structure mask (+2), side-jigsaw radius 4, inner-jigsaw radius 6, attached-piece footprint (all rotations,
+2), ponds, island edge, template bounds (no resizing), no cell over open air beyond the island footprint, giant
spacing 9, every placed block satisfies its P3 survival rule. Unplaceable requests are recorded with the reason.
Deterministic (seeded per template)."""
import os, sys, json, zlib, gzip, io, time, collections, hashlib
import numpy as np, nbtlib
from scipy import ndimage

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
D = os.path.join(ROOT, 'src/main/resources/data/moreminecrafts')
SRC = f'{D}/structure/sky_islands'
OUT = f'{D}/structure/heaven_p3/meadow'
REPORT = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_2026-11-20/data')
NS = 'moreminecrafts'
NAMES = ['large_a', 'large_b', 'large_c', 'large_d', 'large_e', 'plain_medium_a', 'plain_medium_b', 'plain_medium_c',
         'plain_medium_d', 'plain_medium_e', 'plain_small_a', 'plain_small_b', 'plain_small_c', 'plain_small_d',
         'ruins', 'satellite_a', 'satellite_b', 'temple', 'tower']
SLOT = {'minecraft:grass_block': f'{NS}:heaven_grass_block', 'minecraft:dirt': f'{NS}:heaven_loam',
        'minecraft:coarse_dirt': f'{NS}:heaven_loam', 'minecraft:stone': f'{NS}:heaven_cream_stone',
        'minecraft:andesite': f'{NS}:heaven_cream_stone_pale', 'minecraft:tuff': f'{NS}:heaven_cream_stone_pale'}
FINAL_STATE = {'minecraft:grass_block': f'{NS}:heaven_grass_block', 'minecraft:dirt': f'{NS}:heaven_loam'}
VANILLA_VEG = {'minecraft:oak_leaves', 'minecraft:oak_log', 'minecraft:short_grass', 'minecraft:tall_grass', 'minecraft:dandelion',
               'minecraft:poppy', 'minecraft:allium', 'minecraft:cornflower', 'minecraft:azure_bluet', 'minecraft:oxeye_daisy'}
GRASS = f'{NS}:heaven_grass_block'
TERRAIN_OUT = set(SLOT.values())
FACINGS = ['north', 'east', 'south', 'west']
SMALL = [f'{NS}:heaven_flower_{c}' for c in ('red', 'yellow', 'white', 'pink', 'blue', 'violet')]
# canonical strands recovered from the P2 island (top -> tip): heaven_vine 1,2,3,0 ; wisteria cliff 2,3,4 ; hanging 0,1
VINE = [1, 2, 3, 0]; WIS_CLIFF = [2, 3, 4]; WIS_HANG = [0, 1]
# V5 recipe on plain_medium_d (807 grass columns): 2 trees, 9 canopy drapes / tree, 10 cliff wisteria, 8 vines
V5_COLUMNS = 807
GIANT_AREA = 270            # F10 trial: one giant per 270 plantable columns, 0 below 150 (P2.5 E-2)


def dh(*a): return zlib.crc32(repr(a).encode())


def load(path):
    n = nbtlib.load(path)
    pal = [(str(p['id']), {k: str(v) for k, v in p.get('properties', {}).items()}) for p in n['palette']]
    blocks = {}
    for b in n['blocks']:
        bid, props = pal[int(b['state'])]
        blocks[tuple(int(v) for v in b['pos'])] = (bid, props, b.get('nbt'))
    return n, blocks


@__import__('functools').lru_cache(None)
def parts_of(name):
    j = json.load(open(f'{D}/heaven/flora/{name}.json'))
    return j['count']


def species_structures():
    piv = json.load(open(f'{D}/heaven/manifest_p2.json'))['p2']['structures']
    out = {}
    for name, info in piv.items():
        n, blocks = load(f'{D}/structure/heaven_flora/{name}.nbt')
        pv = info['pivot']
        out[name] = [((p[0] - pv[0], p[1] - pv[1], p[2] - pv[2]), bid, props) for p, (bid, props, _) in sorted(blocks.items())]
    return out


def rot(d, r):
    u, w = d[0], d[2]
    for _ in range(r % 4): u, w = -w, u
    return (u, d[1], w)


def noise2(shape, seed, cells=6):
    rng = np.random.default_rng(seed)
    g = rng.random((cells + 1, cells + 1))
    xs = np.linspace(0, cells, shape[0]); zs = np.linspace(0, cells, shape[1])
    return ndimage.map_coordinates(g, np.meshgrid(xs, zs, indexing='ij'), order=1)


def attached_footprints():
    """Pool -> list of (template size, jigsaw pos inside it) of every piece the pool can attach."""
    pools = {}
    for f in ('ruins_top', 'temple_top', 'meadow/tower_top', 'meadow/satellite'):
        j = json.load(open(f'{D}/worldgen/template_pool/sky_islands/{f}.json'))
        pieces = []
        for e in j['elements']:
            loc = e['element'].get('location')
            if not loc: continue
            n, blocks = load(f'{D}/structure/{loc.split(":", 1)[1]}.nbt')
            jig = [p for p, (bid, _, _) in blocks.items() if bid == 'minecraft:jigsaw']
            pieces.append(dict(location=loc, size=[int(v) for v in n['size']], jigsaws=jig))
        pools[f'{NS}:sky_islands/{f}'] = pieces
    return pools


class Island:
    def __init__(self, name, pools, species, theme='meadow', slot=None, veg=None):
        """theme / slot / veg default to the meadow conversion; other themes are only used for the P3-E dry run."""
        self.name = name; self.theme = theme
        SLOT = slot or globals()['SLOT']; VANILLA_VEG = veg or globals()['VANILLA_VEG']
        self.slot = SLOT
        self.nbt, self.src = load(f'{SRC}/{theme}/{name}.nbt')
        self.size = [int(v) for v in self.nbt['size']]
        X, Y, Z = self.size
        self.species = species
        self.rng = np.random.default_rng(dh('P3', theme, name))
        self.stats = collections.Counter(); self.reasons = collections.Counter()
        self.world = {}                     # pos -> (id, props, nbt) of the converted template
        self.veg = {}                       # pos -> (id, props, family) planted
        cols = collections.defaultdict(list)
        for p, (bid, props, nbt) in self.src.items(): cols[(p[0], p[2])].append((p[1], bid))
        surface = [k for k, v in SLOT.items() if v == GRASS]
        grass_top = {c: max(y for y, b in l if b in surface) for c, l in cols.items() if any(b in surface for _, b in l)}
        top_grass = max(grass_top.values()) if grass_top else -1
        struct_min = {}
        for c, l in cols.items():
            ys = [y for y, b in l if b not in SLOT and b not in VANILLA_VEG and b not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air')]
            if ys: struct_min[c] = min(ys)
        self.jigsaws = []
        for p, (bid, props, nbt) in self.src.items():
            c = (p[0], p[2])
            if bid in VANILLA_VEG:
                self.stats['vanilla_vegetation_removed'] += 1; continue
            if bid in SLOT:
                protected = (c in grass_top and p[1] > grass_top[c]) or (c in struct_min and p[1] > struct_min[c]) \
                    or (struct_min and p[1] > top_grass)        # tower buttresses hanging outside the island
                if protected:
                    self.world[p] = (bid, props, nbt); self.stats['terrain_id_kept_as_structure'] += 1
                else:
                    self.world[p] = (SLOT[bid], {}, None); self.stats['terrain_converted'] += 1
                continue
            if bid == 'minecraft:jigsaw':
                nn = nbtlib.Compound(dict(nbt))
                fs = str(nn['final_state'])
                if fs in FINAL_STATE: nn['final_state'] = nbtlib.String(FINAL_STATE[fs]); self.stats['jigsaw_final_state_mapped'] += 1
                self.world[p] = (bid, props, nn); self.jigsaws.append((p, props, nn)); continue
            self.world[p] = (bid, props, nbt)
            self.stats['water' if bid == 'minecraft:water' else 'structure_kept'] += 1
        # ---------------- surface
        self.top = np.full((X, Z), -1); self.topb = {}
        for p, (bid, _, _) in self.world.items():
            if bid == 'minecraft:air': continue
            if p[1] > self.top[p[0], p[2]]: self.top[p[0], p[2]] = p[1]; self.topb[(p[0], p[2])] = bid
        self.foot = np.zeros((X, Z), bool)
        for p, (bid, _, _) in self.world.items():
            if bid in TERRAIN_OUT: self.foot[p[0], p[2]] = True
        self.grass = np.zeros((X, Z), bool); self.water = np.zeros((X, Z), bool)
        for (x, z), bid in self.topb.items():
            if bid == GRASS and self.top[x, z] + 2 < Y: self.grass[x, z] = True
            if bid == 'minecraft:water': self.water[x, z] = True
        self.edge = ndimage.distance_transform_edt(np.pad(self.foot, 1))[1:-1, 1:-1]
        self.pond = ndimage.distance_transform_edt(~self.water) if self.water.any() else np.full((X, Z), 99.0)
        # ---------------- masks
        xs, zs = np.ogrid[:X, :Z]
        self.mask = np.zeros((X, Z), bool); self.attach = np.zeros((X, Z), bool)
        for p, (bid, _, _) in self.world.items():
            if bid not in TERRAIN_OUT and bid not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air'):
                self.mask[max(0, p[0] - 2):p[0] + 3, max(0, p[2] - 2):p[2] + 3] = True
        self.mask_parts = collections.Counter()
        for p, props, nn in self.jigsaws:
            inner = 0 < p[0] < X - 1 and 0 < p[2] < Z - 1
            r = 6 if inner else 4
            self.mask |= (xs - p[0]) ** 2 + (zs - p[2]) ** 2 <= r * r
            orient = props.get('orientation', '')
            if orient.startswith('up_'):
                # a piece is attached ON this island around the jigsaw: its footprint in every rotation, +2
                reach = 0
                for piece in pools.get(str(nn['pool']), []):
                    for jp in piece['jigsaws']:
                        reach = max(reach, jp[0], piece['size'][0] - 1 - jp[0], jp[2], piece['size'][2] - 1 - jp[2])
                reach += 2
                self.attach[max(0, p[0] - reach):p[0] + reach + 1, max(0, p[2] - reach):p[2] + reach + 1] = True
                self.mask_parts['attached_piece_reach'] = reach
        self.mask |= self.attach
        self.plant = self.grass & ~self.mask
        self.occ = np.zeros((X, Z), int)      # 0 free, 1 soft, 2 hard (V5 Layout.occ2)

    # ---------------------------------------------------------------- helpers
    def inb(self, p): return 0 <= p[0] < self.size[0] and 0 <= p[1] < self.size[1] and 0 <= p[2] < self.size[2]

    def air(self, p): return self.inb(p) and p not in self.world and p not in self.veg

    def free(self, x, z, r, allow_soft=False):
        X, Z = self.occ.shape
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                a, b = x + dx, z + dz
                if not (0 <= a < X and 0 <= b < Z) or not self.plant[a, b]: return False
                if self.occ[a, b] == 2 or (self.occ[a, b] == 1 and not allow_soft): return False
        return True

    def mark(self, x, z, r, v):
        X, Z = self.occ.shape
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if 0 <= x + dx < X and 0 <= z + dz < Z: self.occ[x + dx, z + dz] = max(self.occ[x + dx, z + dz], v)

    def cells_for(self, struct, root, r=0):
        out = []
        for d, bid, props in self.species[struct]:
            if d[1] < 0: continue                     # below the ground row: never replaces terrain (P2 rule)
            dd = rot(d, r); p = (root[0] + dd[0], root[1] + dd[1], root[2] + dd[2])
            pr = dict(props)
            if 'facing' in pr: pr['facing'] = FACINGS[r]
            out.append((p, bid, pr))
        return out

    def try_plant(self, struct, family, x, z, r=0, strict_columns=True):
        root = (x, int(self.top[x, z]) + 1, z)
        cells = self.cells_for(struct, root, r)
        for p, _, _ in cells:
            if not self.inb(p): return 'out_of_template_bounds'
            if not self.air(p): return 'cell_collision'
            if strict_columns:
                if not self.foot[p[0], p[2]]: return 'overhangs_open_air'
                if self.attach[p[0], p[2]]: return 'inside_attached_piece'
        for p, bid, pr in cells: self.veg[p] = (bid, pr, family)
        return None

    def candidates(self, min_edge, min_pond):
        c = [(x, z) for x, z in zip(*np.nonzero(self.plant)) if self.edge[x, z] >= min_edge and self.pond[x, z] >= min_pond]
        c = [(int(x), int(z)) for x, z in c]
        self.rng.shuffle(c)
        return c

    # ---------------------------------------------------------------- V5 recipe
    def build(self):
        area = int(self.plant.sum())
        # giants (F10 trial)
        req = 0 if area < 150 else max(1, round(area / GIANT_AREA))
        sp = ['giant_starlotus', 'giant_lily', 'giant_daffodil']
        self.giants = []
        feasible = 0
        for (x, z) in self.candidates(5, 4):
            if not self.free(x, z, 1): continue
            if any((x - a) ** 2 + (z - b) ** 2 < 81 for a, b, _ in self.giants): continue
            feasible += 1
            if len(self.giants) >= req: continue
            kind = sp[len(self.giants) % 3]
            why = self.try_plant(kind, 'giant', x, z)
            if why: self.reasons['giant:' + why] += 1; continue
            self.giants.append((x, z, kind)); self.occ[x, z] = 2; self.mark(x, z, 2, 1)
        self.giant_req = req; self.giant_feasible = feasible
        # trees (V5: 2 per 807 columns, spacing 8 incl. giants)
        treq = round(area * 2 / V5_COLUMNS)
        self.trees = []
        kinds = ['tree_blossom', 'tree_white']
        for (x, z) in self.candidates(3, 3):
            if len(self.trees) >= treq: break
            if not self.free(x, z, 1): continue
            if any((x - a) ** 2 + (z - b) ** 2 < 64 for a, b, _ in self.giants + self.trees): continue
            kind = kinds[len(self.trees) % 2]
            why = self.try_plant(kind, 'tree', x, z)
            if why: self.reasons['tree:' + why] += 1; continue
            self.trees.append((x, z, kind)); self.occ[x, z] = 2; self.mark(x, z, 2, 1)
        self.tree_req = treq
        # canopy wisteria (V5 canopy_drapes(9) per tree)
        for (x, z, kind) in self.trees:
            leaves = [p for p, (bid, pr, fam) in self.veg.items() if fam == 'tree' and bid.endswith('leaves')
                      and abs(p[0] - x) <= 4 and abs(p[2] - z) <= 4]
            under = [p for p in leaves if self.air((p[0], p[1] - 1, p[2])) and p[1] - 1 - self.top[p[0], p[2]] >= 3]
            self.rng.shuffle(under); k = 0
            for p in under:
                if k >= 9: break
                seg = WIS_HANG if self.rng.random() < 0.3 else WIS_HANG[-1:]
                ps = [(p[0], p[1] - 1 - i, p[2]) for i in range(len(seg))]
                if not all(self.air(q) and q[1] - self.top[q[0], q[2]] >= 2 for q in ps): continue
                f = FACINGS[dh('drape', p) % 4]
                for q, part in zip(ps, seg): self.veg[q] = (f'{NS}:heaven_wisteria', {'part': str(part), 'facing': f}, 'drape')
                k += 1
        # medium flowers (V5 scatter 0.10, r_free 1)
        meds = [f'medium_{s}_{k}' for s in ('hydrangea', 'lavender', 'phlox') for k in (0, 1)]
        for (x, z) in sorted(map(tuple, np.argwhere(self.plant).tolist())):
            if self.rng.random() > 0.10 or not self.free(x, z, 1): continue
            st = meds[int(self.rng.integers(0, len(meds)))]; r = int(self.rng.integers(0, 4))
            why = self.try_plant(st, 'medium', x, z, r)
            if why: self.reasons['medium:' + why] += 1; continue
            self.mark(x, z, 1, 1)
        # small flowers (V5 0.40 where noise >= 0.4), grass (0.25, short:tall 3:1), petals (0.05 patches)
        field = noise2(self.plant.shape, 5, 6)
        self.scatter_single(lambda: SMALL[int(self.rng.integers(0, 6))], 0.40, 'small', field, 0.4)
        self.scatter_single(lambda: f'{NS}:heaven_tall_grass' if self.rng.integers(0, 4) == 3 else f'{NS}:heaven_short_grass', 0.25, 'grass')
        for (x, z) in sorted(map(tuple, np.argwhere(self.plant).tolist())):
            if self.rng.random() > 0.05 or not self.free(x, z, 0): continue
            n = 0
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    a, b = x + dx, z + dz
                    if (dx or dz) and self.rng.random() > 0.55: continue
                    if not (0 <= a < self.size[0] and 0 <= b < self.size[2]) or not self.plant[a, b] or self.occ[a, b]: continue
                    p = (a, int(self.top[a, b]) + 1, b)
                    if not self.air(p): continue
                    self.veg[p] = (f'{NS}:heaven_petals', {'part': str(int(self.rng.integers(0, 3))), 'facing': FACINGS[int(self.rng.integers(0, 4))]}, 'ground')
                    self.occ[a, b] = 1; n += 1
        # cliff wisteria (10 / 807) and vines (8 / 807)
        self.cliff(round(area * 10 / V5_COLUMNS), f'{NS}:heaven_wisteria', WIS_CLIFF, 'wisteria')
        self.cliff(round(area * 8 / V5_COLUMNS), f'{NS}:heaven_vine', VINE, 'vine')
        return self

    def scatter_single(self, pick, density, family, field=None, thresh=None):
        for (x, z) in sorted(map(tuple, np.argwhere(self.plant).tolist())):
            if field is not None and field[x, z] < thresh: continue
            if self.rng.random() > density or not self.free(x, z, 0): continue
            bid = pick(); p = (x, int(self.top[x, z]) + 1, z)
            if not self.air(p): continue
            props = {}
            if not bid.startswith(f'{NS}:heaven_flower_'):
                props = {'part': str(int(self.rng.integers(0, parts_of(bid.split(':')[1])))), 'facing': FACINGS[int(self.rng.integers(0, 4))]}
            self.veg[p] = (bid, props, family); self.occ[x, z] = 1

    def cliff(self, n, bid, canon, family):
        X, Z = self.size[0], self.size[2]; cands = []
        for x, z in zip(*np.nonzero(self.plant)):
            x, z = int(x), int(z); y = int(self.top[x, z])
            for dx, dz in ((1, 0), (0, 1)):                       # V5 prefer_front
                a, b = x + dx, z + dz
                if not (0 <= a < X and 0 <= b < Z) or self.top[a, b] >= y or self.mask[a, b]: continue
                cands.append((x, z, dx, dz))
        self.rng.shuffle(cands); k = 0
        for (x, z, dx, dz) in cands:
            if k >= n: break
            a, b, y = x + dx, z + dz, int(self.top[x, z])
            facing = {(-1, 0): 'west', (0, -1): 'north'}[(-dx, -dz)]     # from the vine column back to the cliff
            want = int(self.rng.integers(1, len(canon) + 1))
            run = []
            for i in range(want):
                q = (a, y - i, b); behind = (x, y - i, z)
                if not self.air(q) or self.world.get(behind, ('',))[0] not in TERRAIN_OUT: break
                run.append(q)
            if not run: continue
            seg = canon[:len(run) - 1] + canon[-1:]                     # P2: last segment is always the tip
            for q, part in zip(run, seg): self.veg[q] = (bid, {'part': str(part), 'facing': facing}, family)
            k += 1
        self.stats[f'{family}_strands'] = k; self.stats[f'{family}_requested'] = n

    # ---------------------------------------------------------------- checks / output
    def survival_violations(self):
        """Static replay of the P3 runtime rules on the generated template."""
        def at(p):
            if p in self.veg: return self.veg[p][0]
            return self.world.get(p, ('minecraft:air',))[0]
        DIR = {'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0)}
        sturdy = TERRAIN_OUT | {'minecraft:oak_log', 'minecraft:cherry_leaves', f'{NS}:heaven_white_blossom_leaves'}
        bad = collections.Counter()
        for p, (bid, pr, fam) in self.veg.items():
            below = at((p[0], p[1] - 1, p[2])); above = at((p[0], p[1] + 1, p[2]))
            if fam in ('small', 'grass', 'ground'):
                if below not in (GRASS, f'{NS}:heaven_loam'): bad[fam] += 1
            elif bid.endswith('heaven_vine'):
                d = DIR[pr['facing']]
                if at((p[0] + d[0], p[1], p[2] + d[2])) not in TERRAIN_OUT and above != bid: bad['vine'] += 1
            elif bid.endswith('heaven_wisteria'):
                d = DIR[pr['facing']]
                if at((p[0] + d[0], p[1], p[2] + d[2])) not in TERRAIN_OUT and above != bid and above not in sturdy: bad['wisteria'] += 1
        for (x, z, kind) in self.giants:
            if at((x, int(self.top[x, z]), z)) != GRASS: bad['giant_root'] += 1
        return dict(bad)

    def walkability(self):
        X, Z = self.plant.shape
        hard = {f'{NS}:heaven_giant_stem', 'minecraft:oak_log', 'minecraft:cherry_leaves', f'{NS}:heaven_white_blossom_leaves'}
        walk = np.zeros((X, Z), bool)
        for x, z in zip(*np.nonzero(self.grass)):
            y = int(self.top[x, z])
            walk[x, z] = all(self.veg.get((int(x), y + dy, int(z)), ('',))[0] not in hard for dy in (1, 2))
        def largest(m):
            lab, n = ndimage.label(m); return int(np.bincount(lab.ravel())[1:].max()) if n else 0
        g = int(self.grass.sum())
        return dict(grass_columns=g, walkable_share=round(float(walk.sum() / g), 4) if g else None,
                    largest_region_before=largest(self.grass), largest_region_after=largest(walk))

    def write(self):
        blocks = {p: (bid, props, nbt) for p, (bid, props, nbt) in self.world.items()}
        for p, (bid, props, fam) in self.veg.items():
            assert p not in blocks; blocks[p] = (bid, props, None)
        pal = []; idx = {}; out = []
        for pos in sorted(blocks):
            bid, props, nbt = blocks[pos]; key = (bid, tuple(sorted(props.items())))
            if key not in idx:
                c = nbtlib.Compound({'id': nbtlib.String(bid)})
                if props: c['properties'] = nbtlib.Compound({k: nbtlib.String(v) for k, v in props.items()})
                idx[key] = len(pal); pal.append(c)
            e = nbtlib.Compound({'pos': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in pos]), 'state': nbtlib.Int(idx[key])})
            if nbt is not None: e['nbt'] = nbt
            out.append(e)
        f = nbtlib.File({'DataVersion': nbtlib.Int(int(self.nbt['DataVersion'])), 'size': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in self.size]),
                         'palette': nbtlib.List[nbtlib.Compound](pal), 'blocks': nbtlib.List[nbtlib.Compound](out),
                         'entities': nbtlib.List[nbtlib.Compound]([])})
        buf = io.BytesIO(); f.write(buf); os.makedirs(OUT, exist_ok=True)
        path = f'{OUT}/{self.name}.nbt'
        with open(path, 'wb') as fh: fh.write(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0))
        return path, len(pal)


def main(only=None):
    species = species_structures(); pools = attached_footprints()
    report = {}
    for name in NAMES:
        if only and name not in only: continue
        t0 = time.time()
        isl = Island(name, pools, species).build()
        path, pal = isl.write()
        fam = collections.Counter(v[2] for v in isl.veg.values())
        blk = collections.Counter(v[0].split(':')[1] for v in isl.veg.values())
        src_sha = hashlib.sha256(open(f'{SRC}/meadow/{name}.nbt', 'rb').read()).hexdigest()
        report[name] = dict(
            size=isl.size, source_sha256=src_sha, output=os.path.relpath(path, ROOT), output_bytes=os.path.getsize(path), palette=pal,
            conversion=dict(isl.stats), grass_columns=int(isl.grass.sum()), plantable_columns=int(isl.plant.sum()),
            masked_grass_columns=int((isl.grass & isl.mask).sum()), attached_piece_columns=int((isl.grass & isl.attach).sum()),
            attached_piece_reach=isl.mask_parts.get('attached_piece_reach'),
            giants=dict(trial_formula_requested=isl.giant_req, feasible_positions=isl.giant_feasible, placed=len(isl.giants),
                        species=dict(collections.Counter(k for _, _, k in isl.giants)), positions=[[x, int(isl.top[x, z]) + 1, z] for x, z, _ in isl.giants]),
            trees=dict(requested=isl.tree_req, placed=len(isl.trees), kinds=dict(collections.Counter(k for _, _, k in isl.trees))),
            unplaced_reasons=dict(isl.reasons), vegetation_blocks=len(isl.veg), by_family=dict(fam), by_block=dict(sorted(blk.items())),
            distinct_families=len([k for k in blk if k not in ('cherry_leaves', 'oak_log')]),
            vegetation_per_plantable_column=round(len(isl.veg) / max(1, int(isl.plant.sum())), 3),
            walkability=isl.walkability(), survival_violations=isl.survival_violations(), seconds=round(time.time() - t0, 2))
        print(name, json.dumps({k: report[name][k] for k in ('plantable_columns', 'giants', 'trees', 'vegetation_blocks', 'survival_violations', 'seconds')}))
    os.makedirs(REPORT, exist_ok=True)
    if not only:
        json.dump(report, open(f'{REPORT}/meadow_p3.json', 'w'), indent=1)
    return report


if __name__ == '__main__':
    main(sys.argv[1:] or None)

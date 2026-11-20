"""Heaven sky island P2: reusable vegetation resources + a separate P2 single-island prototype.

Source of truth: the committed V5 design (re-run with instance capture, crc-checked) and the
P1 natural blocks (unchanged).  P1 files are only read.

Resource structure (per family, placement-independent so it is reusable across templates):
  giant flowers  : exact V5 voxel cells of each species, split by function into
                   heaven_giant_stem / heaven_giant_leaf / heaven_giant_bloom (property part),
                   plus one standalone structure per species (heaven_flora/giant_<species>).
  trees          : vanilla oak_log + cherry_leaves (blossom) / heaven_white_blossom_leaves (white),
                   full-block conversion of the V5 canopy; structure per tree (heaven_flora/tree_<kind>).
  medium flowers : heaven_lavender / heaven_hydrangea / heaven_phlox, 2 canonical variants x 4
                   rotations (facing), structure per variant.
  small flowers  : heaven_flower_<colour> (6), one model each (identical to V5).
  grass          : heaven_short_grass (3 variants) / heaven_tall_grass (2 variants), facing.
  vines          : heaven_vine (canonical cliff segments, facing).
  wisteria       : heaven_wisteria (canonical hanging + cliff segments, facing).
  ground petals  : vanilla pink_petals.
"""
import os, sys, json, zlib, gzip, io, hashlib, collections
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import capture_v5 as cv                 # noqa: E402
bp, vl, vm = cv.bp, cv.vl, cv.vm
import nbtlib                           # noqa: E402

R = vm.R; NS = 'moreminecrafts'
ROOT = bp.ROOT; RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/moreminecrafts'); D = os.path.join(RES, 'data/moreminecrafts')
FACINGS = ['north', 'east', 'south', 'west']
COLOUR = {'red': 'red', 'yel': 'yellow', 'wht': 'white', 'pnk': 'pink', 'blu': 'blue', 'vio': 'violet'}
MID = vm.MAT_ID
STEM_M = {MID['stem'], MID['stem_dk']}
BARK_M = {MID['bark'], MID['bark_dk']}
LEAF_M = {MID[m] for m in ('leaf', 'leaf_lt', 'leaf_dk', 'vein')}
TREE_LEAF_THRESHOLD = 24     # voxels of 64: volume-preserving for both V5 canopies (x1.02 / x1.08)
TREE_BARK_THRESHOLD = 8
MEDIUM_VARIANTS = 2
CANON_SEED = 4242


def dh(*a):
    return zlib.crc32(repr(a).encode())


# ------------------------------------------------------------------ voxel helpers
def rot_vox(vox, r):
    """Rotate {(x,y,z): m} (model coords around the base axis) clockwise (seen from above) r*90deg
    about the base block centre (voxel centre = x+0.5)."""
    out = {}
    for (x, y, z), m in vox.items():
        u, w = x + 0.5, z + 0.5
        for _ in range(r % 4):
            u, w = -w, u
        out[(int(round(u - 0.5)), y, int(round(w - 0.5)))] = m
    return out


def cells_from_model(vox):
    """model voxels (axis at x=z=0, y=0 = base) -> {cell offset (dx,dy,dz): 4x4x4 array}."""
    cells = collections.defaultdict(lambda: np.zeros((4, 4, 4), np.uint8))
    for (x, y, z), m in vox.items():
        fx, fy, fz = x + 2, y, z + 2
        cells[(fx // 4, fy // 4, fz // 4)][fx % 4, fy % 4, fz % 4] = MID[m] if isinstance(m, str) else m
    return dict(cells)


def boxes(arr):
    """greedy same-material boxes for an (X,Y,Z) voxel array -> [(x0,y0,z0,x1,y1,z1,mat)]."""
    X, Y, Z = arr.shape; used = np.zeros_like(arr, bool); out = []
    for x in range(X):
        for y in range(Y):
            for z in range(Z):
                m = arr[x, y, z]
                if m == 0 or used[x, y, z]: continue
                x2 = x
                while x2 + 1 < X and arr[x2 + 1, y, z] == m and not used[x2 + 1, y, z]: x2 += 1
                z2 = z
                while z2 + 1 < Z and all(arr[i, y, z2 + 1] == m and not used[i, y, z2 + 1] for i in range(x, x2 + 1)): z2 += 1
                y2 = y
                while y2 + 1 < Y and all(arr[i, y2 + 1, j] == m and not used[i, y2 + 1, j] for i in range(x, x2 + 1) for j in range(z, z2 + 1)): y2 += 1
                used[x:x2 + 1, y:y2 + 1, z:z2 + 1] = True
                out.append((x, y, z, x2 + 1, y2 + 1, z2 + 1, int(m)))
    return out


FACE_AX = {'north': (2, 0), 'south': (2, 1), 'west': (0, 0), 'east': (0, 1), 'down': (1, 0), 'up': (1, 1)}


def hidden(arr, b, face):
    ax, side = FACE_AX[face]; lo = list(b[:3]); hi = list(b[3:6])
    layer = hi[ax] if side else lo[ax] - 1
    if layer < 0 or layer >= arr.shape[ax]: return False
    sl = [slice(lo[0], hi[0]), slice(lo[1], hi[1]), slice(lo[2], hi[2])]; sl[ax] = slice(layer, layer + 1)
    return bool((arr[tuple(sl)] > 0).all())


# ------------------------------------------------------------------ library
class Lib:
    """Unique part patterns per block. A pattern is (array, origin) where origin is the voxel
    offset of array[0,0,0] inside the block cell (lets single-block plants exceed the cell)."""
    def __init__(self):
        self.blocks = collections.OrderedDict()

    def block(self, name, facing=False, collide_mats=frozenset(), family=''):
        if name not in self.blocks:
            self.blocks[name] = dict(parts=[], keys={}, facing=facing, collide=set(collide_mats), family=family)
        return self.blocks[name]

    def add(self, name, arr, origin=(0, 0, 0)):
        b = self.blocks[name]; key = (arr.shape, tuple(origin), arr.tobytes())
        if key not in b['keys']:
            b['keys'][key] = len(b['parts']); b['parts'].append((arr.copy(), tuple(origin)))
        return b['keys'][key]


def single_block_pattern(vox):
    """voxels of a one-block plant (may exceed the cell in y / slightly in x,z) -> (array, origin)."""
    pts = np.array([(x + 2, y, z + 2) for (x, y, z) in vox]); lo = pts.min(0); lo = np.minimum(lo, 0); hi = pts.max(0) + 1
    arr = np.zeros(tuple(hi - lo), np.uint8)
    for (x, y, z), m in vox.items():
        arr[x + 2 - lo[0], y - lo[1], z + 2 - lo[2]] = MID[m] if isinstance(m, str) else m
    return arr, tuple(int(v) for v in lo)


# ------------------------------------------------------------------ canonical shapes
def canonical_vine(n_cells=4, wis=False, attached=True):
    """Canonical strand pattern per cell, top to bottom. attached=True: hugs the cell's north face."""
    L = n_cells * 4
    V = vm.vine(L, seed=CANON_SEED + (1 if wis else 0), wis=wis)
    vox = {}
    for (x, y, z), m in V.items():
        # vm.vine hangs from y=0 downward with its wiggle in x; place the strand in one cell column
        lx = int(round(x)) + 2; ly = y + L - 1; lz = (z + (1 if wis else 0)) if attached else z + 2
        if attached and not wis: lz = 0 if (abs(z) == 0) else 1
        vox[(min(max(lx, 0), 3), ly, min(max(lz, 0), 3))] = m
    cells = []
    for c in range(n_cells):
        arr = np.zeros((4, 4, 4), np.uint8)
        for (x, y, z), m in vox.items():
            if (L - 1 - y) // 4 == c:
                arr[x, y % 4, z] = MID[m]
        cells.append(arr)
    return cells  # index 0 = top


# ------------------------------------------------------------------ conversion
def cells_of_record(r):
    c = collections.defaultdict(lambda: np.zeros((4, 4, 4), np.uint8))
    for (fx, fy, fz), m in zip(r['voxels'], r['mats']):
        c[(fx // R, fy // R + vl.YLO, fz // R)][fx % R, fy % R, fz % R] = m
    return dict(c)


def build():
    s, L, rec = cv.capture()
    lib = Lib()
    lib.block('heaven_giant_stem', collide_mats=STEM_M, family='giant')
    lib.block('heaven_giant_leaf', collide_mats=STEM_M, family='giant')
    lib.block('heaven_giant_bloom', collide_mats=STEM_M, family='giant')
    for sp in ('lavender', 'hydrangea', 'phlox'):
        lib.block(f'heaven_{sp}', facing=True, family='medium')
    for c in vm.SMALL_COLORS:
        lib.block(f'heaven_flower_{COLOUR[c[3:]]}', family='small')
    lib.block('heaven_short_grass', facing=True, family='grass'); lib.block('heaven_tall_grass', facing=True, family='grass')
    lib.block('heaven_vine', facing=True, family='vine'); lib.block('heaven_wisteria', facing=True, family='vine')
    lib.block('heaven_petals', facing=True, family='ground')

    placed = {}          # world block pos -> (block_id, props dict, record index, part key for recon)
    recon = {}           # world block pos -> voxel array (4x4x4) representing what P2 renders (for metrics)
    occupied = set(map(tuple, np.argwhere(np.isin(s.terrain, list(vl.rc.K_OPAQUE) + [vl.rc.K_WATER]))))
    stats = collections.defaultdict(lambda: collections.Counter())
    structures = {}      # name -> {rel pos: (block, props)}, pivot

    def put(pos, block, props, cat, arr_recon):
        if pos in occupied or pos in placed:
            stats[cat]['cell_conflicts'] += 1; return False
        if not (0 <= pos[0] < 45 and 0 <= pos[1] < 43 and 0 <= pos[2] < 37):
            stats[cat]['outside'] += 1; return False
        placed[pos] = (block, props, cat); recon[pos] = arr_recon; return True

    def giant_block_for(arr):
        u = set(np.unique(arr)) - {0}
        if u <= STEM_M: return 'heaven_giant_stem'
        n = (arr > 0).sum(); stem = np.isin(arr, list(STEM_M)).sum(); leaf = np.isin(arr, list(LEAF_M)).sum()
        bloom = n - stem - leaf
        return 'heaven_giant_bloom' if bloom >= max(stem, leaf) else ('heaven_giant_leaf' if leaf >= stem else 'heaven_giant_stem')

    # canonical models -------------------------------------------------------------------------
    med_models = {sp: [cells_from_model(vm.MEDIUM[sp](seed=900 + k)) for k in range(MEDIUM_VARIANTS)] for sp in ('lavender', 'hydrangea', 'phlox')}
    short_models = [single_block_pattern(vm.tuft(700 + k, False)) for k in range(3)]
    tall_models = [single_block_pattern(vm.tuft(710 + k, True)) for k in range(2)]
    vine_cells = canonical_vine(4, wis=False)
    wis_hang = canonical_vine(3, wis=True, attached=False)
    wis_cliff = canonical_vine(3, wis=True, attached=True)

    def rot_cell_offset(d, r):
        dx, dy, dz = d; u, w = dx, dz
        for _ in range(r % 4): u, w = -w, u
        return (u, dy, w)

    def rot_arr(arr, r):
        # clockwise about the block centre (seen from above): (x,z)->(3-z,x) per step
        out = arr
        for _ in range(r % 4):
            out = np.ascontiguousarray(np.rot90(out, k=1, axes=(0, 2)))   # maps (x,z)->(3-z,x)
        return out

    for ri, r in enumerate(rec):
        cat, sp = r['cat'], r['species']
        if cat == 'giant':
            M = r['model']
            cells = cells_from_model(M)
            struct = {}
            for d, arr in cells.items():
                blk = giant_block_for(arr); part = lib.add(blk, arr)
                struct[d] = (blk, {'part': str(part)})
                pos = (r['bx'] + d[0], r['by'] + d[1], r['bz'] + d[2])
                put(pos, f'{NS}:{blk}', {'part': str(part)}, cat, arr)
            structures[f'giant_{sp}'] = struct
        elif cat == 'tree':
            cells = cells_from_model(r['model']); struct = {}
            leaf_block = 'minecraft:cherry_leaves' if sp == 'blossom' else f'{NS}:heaven_white_blossom_leaves'
            for d, arr in cells.items():
                nb = np.isin(arr, list(BARK_M)).sum(); nl = ((arr > 0) & ~np.isin(arr, list(BARK_M))).sum()
                if nb >= TREE_BARK_THRESHOLD:
                    b, props, fill = 'minecraft:oak_log', {'axis': 'y'}, MID['bark']
                elif nl >= TREE_LEAF_THRESHOLD:
                    b, props, fill = leaf_block, {'distance': '1', 'persistent': 'true', 'waterlogged': 'false'}, (MID['bl_1'] if sp == 'blossom' else MID['wt_1'])
                else:
                    stats[cat]['cells_dropped_below_threshold'] += 1; continue
                struct[d] = (b, props)
                pos = (r['bx'] + d[0], r['by'] + d[1], r['bz'] + d[2])
                put(pos, b, props, cat, np.full((4, 4, 4), fill, np.uint8))
            structures[f'tree_{sp}'] = struct
        elif cat == 'drape':
            # one raceme per connected V5 strand; its length follows the strand's visible voxels
            from scipy import ndimage
            occ = np.zeros(L.fine.shape, bool); occ[tuple(r['voxels'].T)] = True
            lab, n_strands = ndimage.label(occ, structure=np.ones((3, 3, 3)))
            for k in range(1, n_strands + 1):
                vox = np.argwhere(lab == k)
                cx, cz = collections.Counter((int(v[0]) // R, int(v[2]) // R) for v in vox).most_common(1)[0][0]
                top = int(vox[:, 1].max()) // R + vl.YLO
                span = int(vox[:, 1].max()) - int(vox[:, 1].min()) + 1
                segs = [wis_hang[2]] if span <= 4 else [wis_hang[0], wis_hang[2]]   # V5 drapes are mostly hidden in the canopy
                rr = dh('drape', cx, cz) % 4; y = top
                while (cx, y, cz) in placed and y > top - 6: y -= 1        # start under the converted canopy
                for i, seg in enumerate(segs):
                    part = lib.add('heaven_wisteria', seg)
                    put((cx, y - i, cz), f'{NS}:heaven_wisteria', {'part': str(part), 'facing': FACINGS[rr]}, 'drape', rot_arr(seg, rr))
        elif cat == 'medium':
            k = dh('med', r['bx'], r['bz']) % MEDIUM_VARIANTS; rr = dh('rot', r['bz'], r['bx']) % 4
            for d, arr in med_models[sp][k].items():
                part = lib.add(f'heaven_{sp}', arr)
                dd = rot_cell_offset(d, rr)
                put((r['bx'] + dd[0], r['by'] + dd[1], r['bz'] + dd[2]), f'{NS}:heaven_{sp}', {'part': str(part), 'facing': FACINGS[rr]}, cat, rot_arr(arr, rr))
            for k2 in range(MEDIUM_VARIANTS):
                structures.setdefault(f'medium_{sp}_{k2}', {d: (f'heaven_{sp}', {'part': str(lib.add(f"heaven_{sp}", a)), 'facing': 'north'}) for d, a in med_models[sp][k2].items()})
        elif cat == 'small':
            arr, org = single_block_pattern(r['model'])
            blk = f'heaven_flower_{COLOUR[sp[6:]]}'; part = lib.add(blk, arr, org)
            cell = np.zeros((4, 4, 4), np.uint8)
            put((r['bx'], r['by'], r['bz']), f'{NS}:{blk}', {}, cat, (arr, org, 0))
        elif cat == 'grass':
            tall = sp == 'tuft_tall'; models = tall_models if tall else short_models
            k = dh('grass', r['bx'], r['bz']) % len(models); rr = dh('grot', r['bx'], r['bz']) % 4
            arr, org = models[k]; blk = 'heaven_tall_grass' if tall else 'heaven_short_grass'
            part = lib.add(blk, arr, org)
            put((r['bx'], r['by'], r['bz']), f'{NS}:{blk}', {'part': str(part), 'facing': FACINGS[rr]}, cat, (arr, org, rr))
        elif cat == 'ground':
            # V5 petal patches -> heaven_petals (vanilla pink_petals cannot survive on heaven grass without a
            # plant-support tag, which is an unapproved gameplay policy; see the P2-D table)
            cnt = collections.Counter((fx // R, fy // R + vl.YLO, fz // R) for (fx, fy, fz) in r['voxels'])
            for pos, n in cnt.items():
                amt = min(4, max(1, (n + 1) // 2)); rr = dh('pet', *pos) % 4
                arr = petals_recon(amt); part = lib.add('heaven_petals', arr)
                put(pos, f'{NS}:heaven_petals', {'part': str(part), 'facing': FACINGS[rr]}, cat, rot_arr(arr, rr))
        elif cat == 'vine':
            cells = cells_of_record(r)
            if not cells:                                     # every voxel was already taken in V5 too
                stats[cat]['instances_without_voxels_in_v5'] += 1; stats[cat]['instances'] += 1; continue
            cols = collections.Counter((p[0], p[2]) for p in cells)
            (cx, cz), _ = cols.most_common(1)[0]
            dx, dz = r['bx'] - cx, r['bz'] - cz               # direction from the vine column to the cliff
            target = {(0, -1): 0, (1, 0): 1, (0, 1): 2, (-1, 0): 3}.get((int(np.sign(dx)), int(np.sign(dz))), 0)
            ys = sorted({p[1] for p in cells if (p[0], p[2]) == (cx, cz)}, reverse=True)
            canon = wis_cliff if sp == 'wisteria' else vine_cells
            blk = 'heaven_wisteria' if sp == 'wisteria' else 'heaven_vine'
            for i, y in enumerate(ys[:len(canon)]):
                seg = canon[i] if i < len(ys) - 1 else canon[len(canon) - 1]
                part = lib.add(blk, seg)
                put((cx, y, cz), f'{NS}:{blk}', {'part': str(part), 'facing': FACINGS[target]}, cat, rot_arr(seg, target))
        stats[cat]['instances'] += 1
    return s, L, rec, lib, placed, recon, stats, structures


def recon_single(arr, org, rr=0):
    """voxels of a one-block plant clipped to its own cell, for metrics."""
    out = np.zeros((4, 4, 4), np.uint8)
    for (x, y, z) in np.argwhere(arr > 0):
        cx, cy, cz = x + org[0], y + org[1], z + org[2]
        if 0 <= cx < 4 and 0 <= cy < 4 and 0 <= cz < 4: out[cx, cy, cz] = arr[x, y, z]
    for _ in range(rr % 4): out = np.ascontiguousarray(np.rot90(out, k=1, axes=(0, 2)))
    return out


def petals_recon(amt):
    out = np.zeros((4, 4, 4), np.uint8)
    for i in range(amt): out[(i % 2) * 2:(i % 2) * 2 + 2, 0, (i // 2) * 2:(i // 2) * 2 + 2] = MID['petal_g']
    return out


# ------------------------------------------------------------------ emission
def wjson(path, obj, compact=True):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, separators=(',', ':') if compact else None, indent=None if compact else 2); f.write('\n')


def model_json(arr, org, mat_tex):
    texk = {}; elements = []
    for b in boxes(arr):
        mat = vm.MAT_NAMES[b[6] - 1]; t = texk.setdefault(mat, 't%d' % len(texk))
        faces = {f: {'texture': '#' + t} for f in FACE_AX if not hidden(arr, b, f)}
        if not faces: continue
        fr = [(b[0] + org[0]) * 4, (b[1] + org[1]) * 4, (b[2] + org[2]) * 4]
        to = [(b[3] + org[0]) * 4, (b[4] + org[1]) * 4, (b[5] + org[2]) * 4]
        elements.append({'from': fr, 'to': to, 'faces': faces})
    tex = {t: mat_tex(m) for m, t in texk.items()}; tex['particle'] = tex[next(iter(texk.values()))]
    return {'parent': 'minecraft:block/block', 'textures': tex, 'elements': elements}, len(elements), sum(len(e['faces']) for e in elements)


def shape_boxes(arr, org, mats=None):
    out = []
    for b in boxes(arr if mats is None else np.where(np.isin(arr, list(mats)), arr, 0)):
        lo = [max(0, min(16, (b[i] + org[i]) * 4)) for i in range(3)]; hi = [max(0, min(16, (b[3 + i] + org[i]) * 4)) for i in range(3)]
        if all(hi[i] > lo[i] for i in range(3)): out.append(lo + hi)
    return out


def write_nbt(path, blocks, size, data_version):
    """blocks: {(x,y,z): (block_id, props)} -> gzip structure NBT in the repo's id/properties format."""
    pal = []; idx = {}; out = []
    for pos in sorted(blocks):
        bid, props = blocks[pos]; key = (bid, tuple(sorted(props.items())))
        if key not in idx:
            c = nbtlib.Compound({'id': nbtlib.String(bid)})
            if props: c['properties'] = nbtlib.Compound({k: nbtlib.String(v) for k, v in props.items()})
            idx[key] = len(pal); pal.append(c)
        out.append(nbtlib.Compound({'pos': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in pos]), 'state': nbtlib.Int(idx[key])}))
    f = nbtlib.File({'DataVersion': nbtlib.Int(data_version), 'size': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in size]),
                     'palette': nbtlib.List[nbtlib.Compound](pal), 'blocks': nbtlib.List[nbtlib.Compound](out),
                     'entities': nbtlib.List[nbtlib.Compound]([])})
    buf = io.BytesIO(); f.write(buf); os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as fh: fh.write(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0))
    return len(pal)


def white_leaves_texture():
    """16x16 cutout leaves from the V5 white-canopy materials (wt_1/wt_2/wt_3), ~22% transparent
    like the V5 canopy's 72% outer fill density, with the design's per-voxel brightness jitter."""
    rng = np.random.default_rng(CANON_SEED)
    cols = [vm.MAT_RGB[MID[m]] for m in ('wt_1', 'wt_2', 'wt_3', 'wt_2')]
    img = np.zeros((16, 16, 4), np.uint8)
    for y in range(0, 16, 2):
        for x in range(0, 16, 2):
            if rng.random() < 0.22: continue
            c = cols[rng.integers(0, 4)] * (0.93 + 0.14 * rng.random())
            img[y:y + 2, x:x + 2, :3] = np.clip(c, 0, 255); img[y:y + 2, x:x + 2, 3] = 255
    return img


def clean_outputs(lib):
    import shutil, glob
    for d in (f'{A}/models/block/heaven', f'{A}/textures/block/heaven', f'{D}/heaven/flora', f'{D}/structure/heaven_flora'):
        shutil.rmtree(d, ignore_errors=True)
    for p in glob.glob(f'{A}/blockstates/heaven_*.json'):
        name = os.path.basename(p)[:-5]
        if name not in ('heaven_grass_block', 'heaven_loam', 'heaven_cream_stone', 'heaven_cream_stone_pale', 'heaven_flora_prototype'):
            os.remove(p)          # P2-owned blockstates only; P1 blockstates are kept


def emit(s, L, rec, lib, placed, recon, stats, structures):
    from PIL import Image
    clean_outputs(lib)
    used = set()
    for name, b in lib.blocks.items():
        for arr, org in b['parts']: used |= set(int(v) for v in np.unique(arr)) - {0}
    tex_dir = f'{A}/textures/block/heaven/flora'; os.makedirs(tex_dir, exist_ok=True)
    tex_bytes = 0
    for m in sorted(used):
        name = vm.MAT_NAMES[m - 1]; p = f'{tex_dir}/{name}.png'
        Image.fromarray(np.clip(np.round(bp.material_texture(name)), 0, 255).astype(np.uint8)).save(p, optimize=True); tex_bytes += os.path.getsize(p)
    mat_tex = lambda m: f'{NS}:block/heaven/flora/{m}'
    res = dict(blocks={}, models=0, elements=0, faces=0, textures=len(used), texture_bytes=tex_bytes)
    for name, b in lib.blocks.items():
        if not b['parts']: continue
        tables = []
        for i, (arr, org) in enumerate(b['parts']):
            mj, ne, nf = model_json(arr, org, mat_tex)
            wjson(f'{A}/models/block/heaven/{name}/p_{i:03d}.json', mj)
            res['models'] += 1; res['elements'] += ne; res['faces'] += nf
            tables.append({'outline': shape_boxes(arr, org), 'collision': shape_boxes(arr, org, b['collide']) if b['collide'] else []})
        n = len(b['parts'])
        variants = {}
        for i in range(n):
            for fi, f in enumerate(FACINGS if b['facing'] else [None]):
                key = ','.join(([f'facing={f}'] if f else []) + ([f'part={i}'] if n > 1 else []))
                v = {'model': f'{NS}:block/heaven/{name}/p_{i:03d}'}
                if f and fi: v['y'] = 90 * fi
                variants[key] = v
        wjson(f'{A}/blockstates/{name}.json', {'variants': variants})
        wjson(f'{D}/heaven/flora/{name}.json', {'count': n, 'facing': b['facing'], 'unit': 'model pixels, unrotated (north)', 'parts': tables})
        res['blocks'][name] = dict(parts=n, family=b['family'], facing=b['facing'])
    # white blossom leaves (vanilla leaves behaviour, cutout texture)
    lp = f'{A}/textures/block/heaven/heaven_white_blossom_leaves.png'
    Image.fromarray(white_leaves_texture(), 'RGBA').save(lp, optimize=True)
    wjson(f'{A}/models/block/heaven/heaven_white_blossom_leaves.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/heaven/heaven_white_blossom_leaves'}}, compact=False)
    wjson(f'{A}/blockstates/heaven_white_blossom_leaves.json', {'variants': {'': {'model': f'{NS}:block/heaven/heaven_white_blossom_leaves'}}}, compact=False)
    res['models'] += 1; res['textures'] += 1; res['texture_bytes'] += os.path.getsize(lp)
    res['blocks']['heaven_white_blossom_leaves'] = dict(parts=1, family='tree', facing=False)

    # P2 island = P1 terrain/water (read from the P1 template, unchanged) + converted vegetation
    p1 = nbtlib.load(f'{D}/structure/heaven_prototype/plain_medium_d_v5.nbt')
    pal = [(str(p['id']), {k: str(v) for k, v in p.get('properties', {}).items()}) for p in p1['palette']]
    blocks = {}
    for b in p1['blocks']:
        bid, props = pal[int(b['state'])]
        if bid == f'{NS}:heaven_flora_prototype': continue
        blocks[tuple(int(v) for v in b['pos'])] = (bid, props)
    terrain_n = len(blocks)
    for pos, (bid, props, cat) in placed.items():
        assert pos not in blocks
        blocks[pos] = (bid, props)
    size = [int(v) for v in p1['size']]
    pal_n = write_nbt(f'{D}/structure/heaven_prototype/plain_medium_d_v5_p2.nbt', blocks, size, int(p1['DataVersion']))
    # reusable structures (pivot = base block of the plant, recorded in the manifest)
    struct_info = {}
    for name, cells in structures.items():
        ds = np.array(list(cells)); lo = ds.min(0); hi = ds.max(0) + 1
        sb = {tuple(int(v) for v in (np.array(d) - lo)): ((bid if ':' in bid else f'{NS}:{bid}'), props) for d, (bid, props) in cells.items()}
        n = write_nbt(f'{D}/structure/heaven_flora/{name}.nbt', sb, [int(v) for v in hi - lo], int(p1['DataVersion']))
        struct_info[name] = dict(size=[int(v) for v in hi - lo], pivot=[int(v) for v in -lo], blocks=len(sb), palette=n)
    return dict(resources=res, terrain_blocks=terrain_n, vegetation_blocks=len(placed), island_palette=pal_n,
                island_bytes=os.path.getsize(f'{D}/structure/heaven_prototype/plain_medium_d_v5_p2.nbt'),
                structures=struct_info)


def metrics(s, L, placed, recon, rec):
    """voxel-level comparison of the P2 vegetation with the V5 design grid, per design category."""
    F = L.fine; O = L.owner
    P = np.zeros_like(F); OWN = np.zeros(F.shape, np.int16)
    code_of = lambda cat: 99 if cat == 'drape' else vl.CAT[cat]
    for (x, y, z), item in recon.items():
        if isinstance(item, tuple):                       # one-block plant: (array, origin, rotation)
            arr, org, rr = item
            vox = {(int(i) + org[0] - 2, int(j) + org[1], int(k) + org[2] - 2): int(arr[i, j, k]) for i, j, k in np.argwhere(arr > 0)}
            for (vx, vy, vz), m in rot_vox(vox, rr).items():
                fx, fy, fz = x * R + 2 + vx, (y - vl.YLO) * R + vy, z * R + 2 + vz
                if 0 <= fx < F.shape[0] and 0 <= fy < F.shape[1] and 0 <= fz < F.shape[2] and P[fx, fy, fz] == 0: P[fx, fy, fz] = m; OWN[fx, fy, fz] = code_of(placed[(x, y, z)][2])
            continue
        fy = (y - vl.YLO) * R
        if fy < 0 or fy + 4 > F.shape[1]: continue
        sl = (slice(x * R, (x + 1) * R), slice(fy, fy + 4), slice(z * R, (z + 1) * R))
        new = (P[sl] == 0) & (item > 0)
        P[sl] = np.where(new, item, P[sl]); OWN[sl] = np.where(new, code_of(placed[(x, y, z)][2]), OWN[sl])
    cats = {v: k for k, v in vl.CAT.items()}; out = {}
    design_cat = np.where((F > 0) & (O == 0), 99, O)          # canopy drapes are written without an owner
    for code in sorted(set(np.unique(design_cat)) - {0}):
        name = cats.get(code, 'drape')
        d = design_cat == code
        # P2 voxels of the same family: the cells this category owns in P2
        fam = np.isin(OWN, [code])
        p = (P > 0) & fam
        inter = (d & p).sum(); union = (d | p).sum()
        # silhouette at block resolution: which block columns hold this category, and how tall it is there
        def tops(mask):
            X, Y, Z = mask.shape
            m = mask.reshape(X // R, R, Y, Z // R, R).any(axis=(1, 4))          # (bx, fy, bz)
            any_ = m.any(1); h = np.where(any_, Y - 1 - np.argmax(m[:, ::-1, :], axis=1), -1)
            return any_, h
        da, dh_ = tops(d); pa, ph = tops(p); both = da & pa
        hd = np.abs(dh_ - ph)[both] / R
        out[name] = dict(design_voxels=int(d.sum()), p2_voxels=int(p.sum()), iou=round(float(inter / union), 3) if union else None,
                         volume_ratio=round(float(p.sum() / max(d.sum(), 1)), 2),
                         column_overlap=round(float(both.sum() / max((da | pa).sum(), 1)), 3),
                         mean_top_height_diff_blocks=round(float(hd.mean()), 2) if hd.size else None)
    allp = P > 0; alld = F > 0
    out['ALL'] = dict(design_voxels=int(alld.sum()), p2_voxels=int(allp.sum()), iou=round(float((alld & allp).sum() / (alld | allp).sum()), 3))
    return out


def p1_stats():
    import glob
    mdl = glob.glob(f'{A}/models/block/heaven_flora/*.json'); el = fc = 0
    for p in mdl:
        j = json.load(open(p)); el += len(j['elements']); fc += sum(len(e['faces']) for e in j['elements'])
    tx = glob.glob(f'{A}/textures/block/heaven_flora/*.png')
    n = nbtlib.load(f'{D}/structure/heaven_prototype/plain_medium_d_v5.nbt')
    return dict(blocks=1, models=len(mdl), elements=el, faces=fc, textures=len(tx), texture_bytes=sum(os.path.getsize(t) for t in tx),
                island_palette=len(n['palette']), island_bytes=os.path.getsize(f'{D}/structure/heaven_prototype/plain_medium_d_v5.nbt'),
                p1_island_sha256=hashlib.sha256(open(f'{D}/structure/heaven_prototype/plain_medium_d_v5.nbt', 'rb').read()).hexdigest())


if __name__ == '__main__':
    p1 = p1_stats()
    s, L, rec, lib, placed, recon, stats, structures = build()
    em = emit(s, L, rec, lib, placed, recon, stats, structures)
    met = metrics(s, L, placed, recon, rec)
    giants = []
    for r in rec:
        if r['cat'] == 'giant':
            lo, hi = vm.bbox(r['model'])
            giants.append(dict(species=r['species'], base=[r['bx'], r['by'], r['bz']], voxel_top_blocks=round((hi[1] + 1) / R, 2),
                               bbox_blocks=[round(float(v) / R, 2) for v in (*lo, *hi)]))
    man = dict(design='V5 (docs/design/heaven_sky_island_vegetation_2026-11-19), crc32 %d' % cv.V5_CRC,
               p1=p1, p2=em, voxel_comparison=met, conversion_stats={k: dict(v) for k, v in stats.items()}, giants=giants,
               rules=dict(tree_leaf_threshold=TREE_LEAF_THRESHOLD, tree_bark_threshold=TREE_BARK_THRESHOLD, medium_variants=MEDIUM_VARIANTS,
                          stem_library='rejected: canonical stems 44->26 cells at mean IoU 0.68 (min 0.25)'))
    wjson(f'{D}/heaven/manifest_p2.json', man, compact=False)
    print(json.dumps({k: man[k] for k in ('p1', 'voxel_comparison', 'conversion_stats')}, indent=1))
    print(json.dumps({k: v for k, v in em.items() if k != 'structures'}, indent=1))

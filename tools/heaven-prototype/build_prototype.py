"""Heaven sky island P1 prototype generator.

Reads the committed design sources (no re-typed colours or shapes):
  * natural blocks : docs/design/heaven_sky_island_vegetation_2026-11-19/render/palettes_veg.py
                     (A grass top, A fringe on B loam side, B loam, B2 cream rock)
  * vegetation     : the V5 layout from veg_layout.build('V5') (quarter-block voxel grid)
  * terrain        : src/main/resources/data/moreminecrafts/structure/sky_islands/meadow/plain_medium_d.nbt

Writes (all new files; the original template is only read):
  assets/moreminecrafts/textures/block/heaven_*.png, textures/block/heaven_flora/*.png
  assets/moreminecrafts/models/block/heaven_*.json, models/block/heaven_flora/part_NNNN.json
  assets/moreminecrafts/blockstates/heaven_*.json, assets/moreminecrafts/items/heaven_*.json
  data/moreminecrafts/structure/heaven_prototype/plain_medium_d_v5.nbt
  data/moreminecrafts/heaven_prototype/flora_parts.json   (per-part outline/collision boxes)
  data/moreminecrafts/heaven_prototype/manifest.json      (giant flowers, counts, provenance)

The vegetation is baked cell by cell: every non-empty 1x1x1 block cell of the V5
voxel grid becomes one block model whose elements are the cell's quarter-block
voxels (4 model units), merged into same-material boxes.  Identical cells share a
model.  All parts are states of one prototype block (`heaven_flora_prototype`,
property `part`).  This is a PROTOTYPE mechanism to reproduce V5 exactly in game;
the production resource architecture is specified separately (docs)."""
import sys, os, json, hashlib, zlib, collections
import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
DESIGN = os.path.join(ROOT, 'docs/design/heaven_sky_island_vegetation_2026-11-19/render')
sys.path.insert(0, DESIGN)
os.environ.setdefault('MC_TEX', os.path.join(os.path.dirname(ROOT), 'MoreMinecrafts-Structure-Generator/vendor/mcbuild/src/mcbuild/assets/textures/block/'))
os.environ.setdefault('SKY_NBT', os.path.join(ROOT, 'src/main/resources/data/moreminecrafts/structure/sky_islands/'))
os.chdir(DESIGN)
import nbtlib                                   # noqa: E402
import palettes_veg as pv, veg_layout as vl, veg_models as vm   # noqa: E402

RES = os.path.join(ROOT, 'src/main/resources')
A = os.path.join(RES, 'assets/moreminecrafts'); D = os.path.join(RES, 'data/moreminecrafts')
NS = 'moreminecrafts'
R = vm.R

# ---------------------------------------------------------------- ids (PROVISIONAL, owner approval)
NATURAL = {
    # id                       : (design texture key, faces)
    'heaven_grass_block':       ('heaven_grass_AL', 'grass'),
    'heaven_loam':              ('garden_loam_B', 'all'),
    'heaven_cream_stone':       ('cream_stone_B2', 'all'),
    'heaven_cream_stone_pale':  ('cream_stone_pale_B2', 'all'),
}
SLOT_TO_ID = {'grass_block': 'heaven_grass_block', 'dirt': 'heaven_loam', 'coarse_dirt': 'heaven_loam',
              'stone': 'heaven_cream_stone', 'andesite': 'heaven_cream_stone_pale', 'tuff': 'heaven_cream_stone_pale'}
FLORA = 'heaven_flora_prototype'
SOLID_MATS = {'stem', 'stem_dk', 'bark', 'bark_dk'}   # giant-flower stems and tree trunks collide (design 'lenient' rule)


def wjson(path, obj, compact=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, separators=(',', ':') if compact else None, indent=None if compact else 2)
        f.write('\n')


def wpng(path, arr):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(np.round(arr), 0, 255).astype(np.uint8)).save(path, optimize=True)


# ---------------------------------------------------------------- natural blocks
def natural_blocks():
    V, N = pv.build()
    # owner-confirmed palette must be the one the design rounds rendered with
    assert pv.PALETTES['HG'] == dict(grass_block='heaven_grass_AL', dirt='garden_loam_B', coarse_dirt='garden_loam_B',
                                     stone='cream_stone_B2', andesite='cream_stone_pale_B2', tuff='cream_stone_pale_B2')
    tex = {}
    g = N['heaven_grass_AL']
    tex['heaven_grass_block_top'] = g['top']; tex['heaven_grass_block_side'] = g['side']
    tex['heaven_loam'] = N['garden_loam_B']['side']
    tex['heaven_cream_stone'] = N['cream_stone_B2']['side']
    tex['heaven_cream_stone_pale'] = N['cream_stone_pale_B2']['side']
    for k, a in tex.items():
        wpng(f'{A}/textures/block/{k}.png', a)
    wjson(f'{A}/models/block/heaven_grass_block.json', {
        'parent': 'minecraft:block/cube_bottom_top',
        'textures': {'top': f'{NS}:block/heaven_grass_block_top', 'side': f'{NS}:block/heaven_grass_block_side',
                     'bottom': f'{NS}:block/heaven_loam', 'particle': f'{NS}:block/heaven_loam'}})
    for k in ('heaven_loam', 'heaven_cream_stone', 'heaven_cream_stone_pale'):
        wjson(f'{A}/models/block/{k}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{k}'}})
    for k in NATURAL:
        wjson(f'{A}/blockstates/{k}.json', {'variants': {'': {'model': f'{NS}:block/{k}'}}})
        wjson(f'{A}/items/{k}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{k}'}})
    # A-grass side before/after (kept for the report; the A original stays in the design folder)
    a_side = N['heaven_grass_A']['side']
    return dict(textures={k: hashlib.sha256(np.clip(np.round(v), 0, 255).astype(np.uint8).tobytes()).hexdigest()[:16] for k, v in tex.items()},
                a_side_soil_vs_loam=float(np.abs(a_side[9:] - tex['heaven_loam'][9:]).mean()),
                new_side_soil_vs_loam=float(np.abs(tex['heaven_grass_block_side'][9:] - tex['heaven_loam'][9:]).mean()))


# ---------------------------------------------------------------- flora parts
def boxes(cell):
    """Greedy same-material box merge inside one 4x4x4 cell -> list of (x0,y0,z0,x1,y1,z1,mat_id) in voxels."""
    used = np.zeros_like(cell, bool); out = []
    for x in range(4):
        for y in range(4):
            for z in range(4):
                m = cell[x, y, z]
                if m == 0 or used[x, y, z]:
                    continue
                x2 = x
                while x2 + 1 < 4 and cell[x2 + 1, y, z] == m and not used[x2 + 1, y, z]: x2 += 1
                z2 = z
                while z2 + 1 < 4 and all(cell[i, y, z2 + 1] == m and not used[i, y, z2 + 1] for i in range(x, x2 + 1)): z2 += 1
                y2 = y
                while y2 + 1 < 4 and all(cell[i, y2 + 1, j] == m and not used[i, y2 + 1, j] for i in range(x, x2 + 1) for j in range(z, z2 + 1)): y2 += 1
                used[x:x2 + 1, y:y2 + 1, z:z2 + 1] = True
                out.append((x, y, z, x2 + 1, y2 + 1, z2 + 1, int(m)))
    return out


FACES = {'north': (2, 0), 'south': (2, 1), 'west': (0, 0), 'east': (0, 1), 'down': (1, 0), 'up': (1, 1)}


def face_hidden(cell, b, face):
    """True when the whole face of box b is covered by other voxels inside the same cell."""
    ax, side = FACES[face]
    lo = list(b[:3]); hi = list(b[3:6])
    layer = hi[ax] if side else lo[ax] - 1
    if layer < 0 or layer > 3:
        return False
    sl = [slice(lo[0], hi[0]), slice(lo[1], hi[1]), slice(lo[2], hi[2])]
    sl[ax] = slice(layer, layer + 1)
    return bool((cell[tuple(sl)] > 0).all())


def material_texture(mat):
    """16x16 swatch: the design colour with a fixed per-quarter-block brightness jitter (0.93-1.07),
    the same jitter range the design renderer applies per voxel."""
    base = vm.MAT_RGB[vm.MAT_ID[mat]]
    rng = np.random.default_rng(zlib.crc32(mat.encode()))
    j = 0.93 + 0.14 * rng.random((4, 4))
    return np.repeat(np.repeat(j, 4, 0), 4, 1)[..., None] * base[None, None, :]


def flora():
    s = vl.Surface(); L = vl.build('V5', s)
    F = L.fine
    used_mats = sorted({vm.MAT_NAMES[i - 1] for i in np.unique(F) if i})
    for m in used_mats:
        wpng(f'{A}/textures/block/heaven_flora/{m}.png', material_texture(m))
    cells = {}
    X, Y, Z = (n // R for n in F.shape)
    for bx in range(X):
        for by in range(Y):
            for bz in range(Z):
                c = F[bx * R:(bx + 1) * R, by * R:(by + 1) * R, bz * R:(bz + 1) * R]
                if c.any():
                    cells[(bx, by + vl.YLO, bz)] = np.ascontiguousarray(c)
    order = {}; parts = []; placements = {}
    for pos in sorted(cells):
        key = cells[pos].tobytes()
        if key not in order:
            order[key] = len(parts); parts.append(cells[pos])
        placements[pos] = order[key]
    shapes = []; n_elem = 0; n_faces = 0
    for i, c in enumerate(parts):
        bs = boxes(c)
        texk = {}; elements = []
        for b in bs:
            mat = vm.MAT_NAMES[b[6] - 1]
            t = texk.setdefault(mat, 't%d' % len(texk))
            faces = {f: {'texture': '#' + t} for f in FACES if not face_hidden(c, b, f)}
            if not faces:
                continue
            n_faces += len(faces)
            elements.append({'from': [b[0] * 4, b[1] * 4, b[2] * 4], 'to': [b[3] * 4, b[4] * 4, b[5] * 4], 'faces': faces})
        n_elem += len(elements)
        textures = {t: f'{NS}:block/heaven_flora/{m}' for m, t in texk.items()}
        textures['particle'] = textures[next(iter(texk.values()))]
        wjson(f'{A}/models/block/heaven_flora/part_{i:04d}.json', {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements}, compact=True)
        solid = [[v * 4 for v in b[:6]] for b in bs if vm.MAT_NAMES[b[6] - 1] in SOLID_MATS]
        shapes.append({'outline': [[v * 4 for v in b[:6]] for b in bs], 'collision': solid})
    wjson(f'{A}/blockstates/{FLORA}.json', {'variants': {f'part={i}': {'model': f'{NS}:block/heaven_flora/part_{i:04d}'} for i in range(len(parts))}}, compact=True)
    wjson(f'{D}/heaven_prototype/flora_parts.json', {'count': len(parts), 'unit': 'model pixels (1/16 block)', 'parts': shapes}, compact=True)
    return s, L, placements, dict(parts=len(parts), cells=len(cells), elements=n_elem, faces=n_faces, materials=len(used_mats))


# ---------------------------------------------------------------- prototype NBT
def prototype_nbt(s, L, placements):
    src = os.path.join(RES, 'data/moreminecrafts/structure/sky_islands/meadow/plain_medium_d.nbt')
    raw = open(src, 'rb').read()
    n = nbtlib.load(src)
    pal = [(str(p['id']).replace('minecraft:', ''), p) for p in n['palette']]
    terrain = set(SLOT_TO_ID); keep = {'water'}
    new_pal = []; idx = {}
    def state(key, props=None):
        k = (key, tuple(sorted((props or {}).items())))
        if k not in idx:
            c = nbtlib.Compound({'id': nbtlib.String(key)})
            if props:
                c['properties'] = nbtlib.Compound({a: nbtlib.String(v) for a, v in props.items()})
            idx[k] = len(new_pal); new_pal.append(c)
        return idx[k]
    blocks = []; dropped = collections.Counter(); terrain_pos = {}
    for b in n['blocks']:
        name, p = pal[int(b['state'])]
        pos = tuple(int(v) for v in b['pos'])
        if name in terrain:
            si = state(f'{NS}:{SLOT_TO_ID[name]}'); terrain_pos[pos] = name
        elif name in keep:
            si = state('minecraft:water', {k: str(v) for k, v in p.get('properties', {}).items()} or None)
        else:
            dropped[name] += 1; continue
        blocks.append(nbtlib.Compound({'pos': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in pos]), 'state': nbtlib.Int(si)}))
    occupied = {tuple(int(v) for v in b['pos']) for b in blocks}
    clash = 0
    for pos, part in sorted(placements.items()):
        if pos in occupied:
            clash += 1; continue
        si = state(f'{NS}:{FLORA}', {'part': str(part)})
        blocks.append(nbtlib.Compound({'pos': nbtlib.List[nbtlib.Int]([nbtlib.Int(v) for v in pos]), 'state': nbtlib.Int(si)}))
    out = nbtlib.File({'DataVersion': n['DataVersion'], 'size': n['size'], 'palette': nbtlib.List[nbtlib.Compound](new_pal),
                       'blocks': nbtlib.List[nbtlib.Compound](blocks), 'entities': nbtlib.List[nbtlib.Compound]([])}, gzipped=True)
    dst = os.path.join(D, 'structure/heaven_prototype/plain_medium_d_v5.nbt')
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    import gzip, io
    buf = io.BytesIO(); out.write(buf)          # uncompressed NBT payload
    with open(dst, 'wb') as fh:                  # mtime=0 -> byte-reproducible gzip
        fh.write(gzip.compress(buf.getvalue(), compresslevel=9, mtime=0))
    assert open(src, 'rb').read() == raw, 'original template must be untouched'
    return dict(source_sha256=hashlib.sha256(raw).hexdigest(), terrain_blocks=len(terrain_pos),
                dropped_template_blocks=dict(dropped), flora_blocks=len(placements), flora_terrain_clash=clash,
                palette_entries=len(new_pal), size=[int(v) for v in n['size']])


def manifest(s, L, nat, fl, nb):
    giants = []
    for i in L.instances:
        if i['cat'] != 'giant':
            continue
        M = vm.GIANTS[i['species']](i['H'], seed=vl.dh('V5', i['species']) % 997)
        lo, hi = vm.bbox(M)
        giants.append(dict(species=i['species'], base=[i['x'], i['y'], i['z']], design_height_blocks=i['H'],
                           voxel_top_blocks=round((hi[1] + 1) / R, 2),
                           bbox_blocks=[round(float(v) / R, 2) for v in (*lo, *hi)]))
    trees = [dict(kind=i['species'], base=[i['x'], i['y'], i['z']], H=i['H']) for i in L.instances if i['cat'] == 'tree']
    chk = vl.checks(L)
    m = dict(design='V5 Fantastic Heaven Garden (docs/design/heaven_sky_island_vegetation_2026-11-19)',
             template=f'{NS}:heaven_prototype/plain_medium_d_v5', natural_palette=SLOT_TO_ID,
             natural=nat, flora=fl, nbt=nb, giants=giants, trees=trees,
             design_counts=chk['counts'], design_checks={k: chk[k] for k in ('walkable_share', 'strict_walkable_share', 'pond_columns_covered_low', 'clipped_voxels_outside_template')},
             fine_grid_crc32=zlib.crc32(L.fine.tobytes()), jigsaw_clearance=[list(j) for j in vl.JIGSAWS])
    wjson(f'{D}/heaven_prototype/manifest.json', m)
    return m


if __name__ == '__main__':
    nat = natural_blocks()
    s, L, placements, fl = flora()
    nb = prototype_nbt(s, L, placements)
    m = manifest(s, L, nat, fl, nb)
    print(json.dumps({k: m[k] for k in ('flora', 'nbt', 'giants')}, indent=1))

"""Vegetation layouts V1-V5 on the fixed plain_medium_d terrain.

The terrain block coordinates are never modified: the template's own vanilla
vegetation (oak trees, short/tall grass, flowers) is removed and replaced by a
prototype layout, rasterised into a quarter-block voxel grid (temporary,
visualisation-only; nothing is written to any game NBT)."""
import numpy as np, collections, zlib


def dh(*a):
    """Deterministic hash (Python's hash() is salted per process)."""
    return zlib.crc32(repr(a).encode())
from scipy import ndimage
import raycast as rc, veg_models as vm, veg_render as vr

R = vm.R
TEMPLATE = 'meadow/plain_medium_d.nbt'
SIZE = (45, 43, 37)            # template bounding box (blocks)
YLO, YHI = 3, 31               # fine grid vertical extent (blocks)
JIGSAWS = [(0, 17, 18), (44, 8, 18)]  # satellite links: keep clear


class Surface:
    def __init__(self):
        grid, _ = rc.load_grid(TEMPLATE)
        g = vr.strip_vegetation(grid)
        self.raw = grid
        Gt = np.zeros((SIZE[0], YHI + 2, SIZE[2]), np.uint8)
        Gt[:, :g.shape[1], :] = g
        self.terrain = Gt
        X, Y, Z = Gt.shape
        solid = np.isin(Gt, list(rc.K_OPAQUE))
        self.solid = solid
        self.top = np.full((X, Z), -1)
        self.kind = np.zeros((X, Z), int)
        for x in range(X):
            for z in range(Z):
                ys = np.nonzero(Gt[x, :, z])[0]
                if ys.size:
                    self.top[x, z] = ys.max(); self.kind[x, z] = Gt[x, ys.max(), z]
        self.grass = self.kind == rc.KID['grass_block']
        self.water = self.kind == rc.K_WATER
        foot = self.top >= 0
        self.edge_dist = ndimage.distance_transform_edt(np.pad(foot, 1))[1:-1, 1:-1]
        self.pond_dist = ndimage.distance_transform_edt(~self.water)
        jz = np.zeros((X, Z), bool)
        for (jx, jy, jzz) in JIGSAWS:
            for x in range(X):
                for z in range(Z):
                    if (x - jx) ** 2 + (z - jzz) ** 2 <= 4 ** 2:
                        jz[x, z] = True
        self.jig = jz

    def cells(self, mask):
        xs, zs = np.nonzero(mask); return list(zip(xs.tolist(), zs.tolist()))


CAT = {'giant': 1, 'tree': 2, 'medium': 3, 'small': 4, 'grass': 5, 'ground': 6, 'bush': 7, 'vine': 8}


class Layout:
    def __init__(self, surf, name):
        self.s = surf; self.name = name
        self.fine = np.zeros((SIZE[0] * R, (YHI - YLO) * R, SIZE[2] * R), np.uint8)
        self.owner = np.zeros_like(self.fine)             # category code per voxel
        self.occ2 = np.zeros(self.s.top.shape, int)     # 0 free, 1 soft (plants), 2 hard (stems/trunks)
        self.instances = []
        self.clipped = 0; self.inside_terrain = 0
        self.rng = np.random.default_rng(dh(name))

    def place(self, cat, species, model, bx, bz, H=None, hard=0, soft=0, by=None, extra=None):
        by = (self.s.top[bx, bz] + 1) if by is None else by
        ox, oy, oz = bx * R + 2, (by - YLO) * R, bz * R + 2
        for (x, y, z), m in model.items():
            fx, fy, fz = ox + x, oy + y, oz + z
            if not (0 <= fx < self.fine.shape[0] and 0 <= fy < self.fine.shape[1] and 0 <= fz < self.fine.shape[2]):
                self.clipped += 1; continue
            wx, wy, wz = fx // R, fy // R + YLO, fz // R
            if self.s.solid[wx, wy, wz]:
                self.inside_terrain += 1; continue
            if self.fine[fx, fy, fz] == 0:
                self.fine[fx, fy, fz] = vm.MAT_ID[m]; self.owner[fx, fy, fz] = CAT[cat]
        for dx in range(-hard, hard + 1):
            for dz in range(-hard, hard + 1):
                if 0 <= bx + dx < self.occ2.shape[0] and 0 <= bz + dz < self.occ2.shape[1]:
                    self.occ2[bx + dx, bz + dz] = 2
        for dx in range(-soft, soft + 1):
            for dz in range(-soft, soft + 1):
                if 0 <= bx + dx < self.occ2.shape[0] and 0 <= bz + dz < self.occ2.shape[1]:
                    self.occ2[bx + dx, bz + dz] = max(self.occ2[bx + dx, bz + dz], 1)
        lo, hi = vm.bbox(model)
        self.instances.append(dict(cat=cat, species=species, x=bx, y=int(by), z=bz, H=H,
                                   bbox_blocks=[round(float(v) / R, 2) for v in (*lo, *hi)], extra=extra or {}))

    # ---------------------------------------------------------- pickers
    def free(self, bx, bz, r, allow_soft=False):
        X, Z = self.occ2.shape
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                x, z = bx + dx, bz + dz
                if not (0 <= x < X and 0 <= z < Z) or not self.s.grass[x, z]:
                    return False
                if self.occ2[x, z] == 2 or (self.occ2[x, z] == 1 and not allow_soft):
                    return False
        return True

    def pick(self, n, min_edge, min_pond, spacing, r_free, prefer=None, others=()):
        cand = [c for c in self.s.cells(self.s.grass & ~self.s.jig) if self.s.edge_dist[c] >= min_edge and self.s.pond_dist[c] >= min_pond]
        self.rng.shuffle(cand)
        if prefer is not None:
            cand.sort(key=prefer)
        out = []
        for c in cand:
            if len(out) >= n: break
            if not self.free(c[0], c[1], r_free):
                continue
            if any((c[0] - o[0]) ** 2 + (c[1] - o[1]) ** 2 < spacing ** 2 for o in out + list(others)):
                continue
            out.append(c)
        return out

    def giants(self, specs, spacing=8, min_edge=5, min_pond=4, prefer=None):
        bloom_r = {'tulip': 2.2, 'lily': 2.6, 'daffodil': 2.4, 'peony': 2.3, 'starlotus': 3.2}
        pos = self.pick(len(specs), min_edge, min_pond, spacing, 1, prefer=prefer)
        if len(pos) < len(specs):
            raise RuntimeError('%s: only %d/%d giant positions' % (self.name, len(pos), len(specs)))
        for (sp, H), (x, z) in zip(specs, pos):
            self.place('giant', sp, vm.GIANTS[sp](H, seed=dh(self.name, sp) % 997), x, z, H=H, hard=0, soft=2,
                       extra=dict(bloom_radius_blocks=bloom_r[sp] * H / 10))
            self.occ2[x, z] = 2
        self.giant_pos = pos
        return pos

    def trees(self, specs, spacing=7, min_edge=3, min_pond=3, prefer=None):
        pos = self.pick(len(specs), min_edge, min_pond, spacing, 1, prefer=prefer, others=getattr(self, 'giant_pos', []))
        for (kind, H), (x, z) in zip(specs, pos):
            self.place('tree', kind, vm.tree(kind, H, seed=dh(self.name, kind, x) % 997), x, z, H=H, hard=0, soft=2)
            self.occ2[x, z] = 2
        self.tree_pos = pos
        return pos

    def scatter(self, cat, makers, density, mask=None, r_free=0, field=None, thresh=None, allow_soft=False):
        cells = self.s.cells(self.s.grass & (mask if mask is not None else True))
        n = 0
        for (x, z) in cells:
            if field is not None and field[x, z] < thresh:
                continue
            if self.rng.random() > density or not self.free(x, z, r_free, allow_soft):
                continue
            sp, mk, H = makers[self.rng.integers(0, len(makers))]
            self.place(cat, sp, mk(int(self.rng.integers(0, 10 ** 6))), x, z, H=H, soft=r_free)
            n += 1
        return n

    def vines(self, n, wis=False, Lrange=(6, 16), prefer_front=True):
        s = self.s; X, Z = s.top.shape; cands = []
        for x in range(X):
            for z in range(Z):
                if not s.grass[x, z]: continue
                y = s.top[x, z]
                for dx, dz in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    nx, nz = x + dx, z + dz
                    if 0 <= nx < X and 0 <= nz < Z and s.top[nx, nz] >= y:
                        continue
                    if prefer_front and (dx, dz) not in ((1, 0), (0, 1)):
                        continue
                    if s.jig[x, z]: continue
                    cands.append((x, z, dx, dz))
        self.rng.shuffle(cands); k = 0
        for (x, z, dx, dz) in cands[:n]:
            L = int(self.rng.integers(*Lrange))
            M = vm.vine(L, seed=int(self.rng.integers(0, 10 ** 6)), wis=wis)
            if dx != 0:  # wiggle along z (face tangent)
                M = vm.Model({(0, y, xx): m for (xx, y, zz), m in M.items()})
            off = int(self.rng.integers(-1, 2))
            ox = 2 + dx * 2 + (off if dx == 0 else 0); oz = 2 + dz * 2 + (off if dz == 0 else 0)
            shifted = vm.Model({(xx + ox - 2, yy - 1, zz + oz - 2): m for (xx, yy, zz), m in M.items()})
            self.place('vine', 'wisteria' if wis else 'vine', shifted, x, z, by=s.top[x, z] + 1, H=L / R)
            k += 1
        return k

    def canopy_drapes(self, n, wis=True, Lrange=(4, 10)):
        """Hang wisteria/vines from the underside of tree canopies already rasterised."""
        F = self.fine; k = 0
        for (tx, tz) in getattr(self, 'tree_pos', []):
            sub = F[max(0, (tx - 4) * R):(tx + 5) * R, :, max(0, (tz - 4) * R):(tz + 5) * R]
            xs, ys, zs = np.nonzero(sub)
            if xs.size == 0: continue
            ymin_canopy = np.percentile(ys, 55)
            idx = [i for i in range(xs.size) if ys[i] > ymin_canopy and (ys[i] == 0 or sub[xs[i], ys[i] - 1, zs[i]] == 0)]
            self.rng.shuffle(idx)
            for i in idx[:n]:
                fx, fy, fz = xs[i] + max(0, (tx - 4) * R), ys[i], zs[i] + max(0, (tz - 4) * R)
                L = int(self.rng.integers(*Lrange)); M = vm.vine(L, seed=int(self.rng.integers(0, 10 ** 6)), wis=wis)
                for (x, y, z), m in M.items():
                    px, py, pz = fx + x, fy - 1 + y, fz + z
                    if 0 <= px < F.shape[0] and 0 <= py < F.shape[1] and 0 <= pz < F.shape[2] and F[px, py, pz] == 0:
                        wx, wy, wz = px // R, py // R + YLO, pz // R
                        if not self.s.solid[wx, wy, wz]:
                            F[px, py, pz] = vm.MAT_ID[m]
                k += 1
        self.instances.append(dict(cat='vine', species='wisteria_canopy' if wis else 'vine_canopy', count=k))
        return k


# ------------------------------------------------------------------ makers

SMALL = [('small_' + c[3:], (lambda c: (lambda s: vm.small_flower(c, s, H=1.0)))(c), 1.0) for c in vm.SMALL_COLORS]
MED = {k: (lambda k: (lambda s: vm.MEDIUM[k](seed=s)))(k) for k in vm.MEDIUM}
MED_H = {'lavender': 2.5, 'bells': 2.0, 'phlox': 1.75, 'yellowcluster': 1.75, 'hydrangea': 2.0}
TUFT = [('tuft_short', lambda s: vm.tuft(s, False), 0.75), ('tuft_tall', lambda s: vm.tuft(s, True), 1.75)]


def noise2(shape, seed, cells=6):
    rng = np.random.default_rng(seed)
    g = rng.random((cells + 1, cells + 1))
    xs = np.linspace(0, cells, shape[0]); zs = np.linspace(0, cells, shape[1])
    return ndimage.map_coordinates(g, np.meshgrid(xs, zs, indexing='ij'), order=1)


def build(name, surf=None, scale=None):
    s = surf or Surface(); L = Layout(s, name)
    center = lambda c: (c[0] - 26) ** 2 + (c[1] - 17) ** 2
    if name == 'V1':
        L.giants([('tulip', 10), ('lily', 10), ('peony', 10)], spacing=9)
        L.trees([('heaven', 8), ('blossom', 8)], spacing=9)
        f = noise2(s.top.shape, 1)
        L.scatter('medium', [(k, MED[k], MED_H[k]) for k in MED], 0.10, r_free=1)
        L.scatter('small', SMALL, 0.45, field=f, thresh=0.45)
        L.scatter('grass', TUFT[:1] * 3 + TUFT[1:], 0.30)
        L.scatter('ground', [('petals', lambda sd: vm.petals_patch(sd, 5, 0.12), 0.06)], 0.02)
        L.vines(10)
    elif name == 'V2':
        L.giants([('tulip', 10), ('lily', 10), ('daffodil', 10), ('peony', 10), ('starlotus', 10)], spacing=7, min_edge=4, min_pond=4)
        ring = np.zeros(s.top.shape, bool)
        for (gx, gz) in L.giant_pos:
            for x in range(s.top.shape[0]):
                for z in range(s.top.shape[1]):
                    if 2.5 <= np.hypot(x - gx, z - gz) <= 5: ring[x, z] = True
        L.scatter('medium', [(k, MED[k], MED_H[k]) for k in ('lavender', 'hydrangea', 'phlox', 'bells')], 0.30, mask=ring, r_free=1)
        L.scatter('small', SMALL, 0.08)
        L.scatter('grass', TUFT[:1] * 3 + TUFT[1:], 0.30)
        L.vines(6)
    elif name == 'V3':
        L.giants([('peony', 10)], prefer=center)
        L.trees([('white', 7)], spacing=8)
        f = noise2(s.top.shape, 3, 5)
        L.scatter('medium', [(k, MED[k], MED_H[k]) for k in ('lavender', 'phlox', 'yellowcluster', 'hydrangea')], 0.20, r_free=1)
        L.scatter('small', SMALL, 0.80, field=f, thresh=0.25)
        L.scatter('small', SMALL, 0.35)
        L.scatter('grass', TUFT[:1], 0.20)
        L.scatter('ground', [('petals', lambda sd: vm.petals_patch(sd, 4, 0.15), 0.06)], 0.02)
        L.vines(6)
    elif name == 'V4':
        L.trees([('heaven', 8), ('heaven', 7), ('blossom', 8)], spacing=6, prefer=lambda c: (c[0] - 12) ** 2 + (c[1] - 12) ** 2)
        L.giants([('tulip', 10), ('daffodil', 10)], spacing=10, prefer=lambda c: -((c[0] - 12) ** 2 + (c[1] - 12) ** 2))
        near_tree = np.zeros(s.top.shape, bool); waterside = (s.pond_dist <= 2.5) & s.grass
        for (tx, tz) in L.tree_pos:
            for x in range(s.top.shape[0]):
                for z in range(s.top.shape[1]):
                    if np.hypot(x - tx, z - tz) <= 4: near_tree[x, z] = True
        L.scatter('ground', [('bigleaf', lambda sd: vm.big_leaf_plant(sd), 0.75)], 0.12, mask=near_tree, r_free=0, allow_soft=True)
        L.scatter('bush', [('leafbush', lambda sd: vm.bush(sd), 1.4)], 0.14, mask=near_tree, r_free=0, allow_soft=True)
        L.scatter('medium', [('bells', MED['bells'], 2.0)], 0.35, mask=waterside, r_free=1)
        L.scatter('grass', [TUFT[1]], 0.6, mask=waterside)
        f = noise2(s.top.shape, 4, 7)
        L.scatter('medium', [(k, MED[k], MED_H[k]) for k in ('lavender', 'phlox', 'yellowcluster')], 0.10, mask=~near_tree, r_free=1, field=f, thresh=0.55)
        L.scatter('small', SMALL, 0.55, mask=~near_tree, field=f, thresh=0.5)
        L.scatter('bush', [('flowerbush', lambda sd: vm.bush(sd, ['sm_pnk', 'sm_wht']), 1.4)], 0.03, r_free=1)
        L.scatter('grass', TUFT[:1] * 2 + TUFT[1:], 0.45)
        L.vines(12)
    elif name == 'V5':
        L.giants([('starlotus', 10), ('lily', 10), ('daffodil', 10)], spacing=9)
        L.trees([('blossom', 8), ('white', 9)], spacing=8)
        L.canopy_drapes(9, wis=True)
        f = noise2(s.top.shape, 5, 6)
        L.scatter('medium', [(k, MED[k], MED_H[k]) for k in ('hydrangea', 'lavender', 'phlox')], 0.10, r_free=1)
        L.scatter('small', SMALL, 0.40, field=f, thresh=0.4)
        L.scatter('grass', TUFT[:1] * 3 + TUFT[1:], 0.25)
        L.scatter('ground', [('petals', lambda sd: vm.petals_patch(sd, 6, 0.14), 0.06)], 0.05)
        L.vines(10, wis=True, Lrange=(6, 14))
        L.vines(8, wis=False)
    else:
        raise ValueError(name)
    return L


def checks(L):
    """Placement checks requested in the brief."""
    s = L.s; res = {}
    res['clipped_voxels_outside_template'] = int(L.clipped)
    res['voxels_skipped_inside_terrain'] = int(L.inside_terrain)
    gi = [i for i in L.instances if i['cat'] == 'giant']
    res['giants'] = []
    for g in gi:
        x, z = g['x'], g['z']; br = g['extra']['bloom_radius_blocks']
        res['giants'].append(dict(species=g['species'], pos=[x, g['y'], z], H=g['H'],
                                  edge_margin_blocks=round(float(s.edge_dist[x, z] - br), 1),
                                  pond_margin_blocks=round(float(s.pond_dist[x, z] - br), 1)))
    tr = [i for i in L.instances if i['cat'] == 'tree']
    pts = [(i['x'], i['z']) for i in gi + tr]
    res['min_giant_tree_spacing'] = round(float(min([np.hypot(a[0] - b[0], a[1] - b[1]) for k, a in enumerate(pts) for b in pts[k + 1:]] or [0])), 1)
    # pond: vegetation voxels directly above pond columns, within 3 blocks of the surface
    F = L.fine; over = 0
    for (x, z) in s.cells(s.water):
        y0 = (s.top[x, z] + 1 - YLO) * R
        over += int((F[x * R:(x + 1) * R, y0:y0 + 3 * R, z * R:(z + 1) * R] > 0).sum() > 0)
    res['pond_columns_covered_low'] = f'{over}/{int(s.water.sum())}'
    # walkability: grass columns whose 2-block headroom has no "hard" vegetation
    # walkability: a grass column is walkable when its 2-block headroom holds no stem/trunk
    # voxel and fewer than 16 voxels (1/4 block) of other collision-likely vegetation
    # (giant leaves/petals, tree leaves, bushes).  Small/medium flowers and grass are passable.
    stem_ids = [vm.MAT_ID[m] for m in ('stem', 'stem_dk', 'bark', 'bark_dk')]
    hard_ids = [vm.MAT_ID[m] for m in vm.MAT if m.startswith(('tul', 'lil', 'daf', 'peo', 'fan', 'tl_', 'bl_', 'wt_', 'sepal'))]
    walk = np.zeros(s.top.shape, bool); strict = np.zeros(s.top.shape, bool)
    for (x, z) in s.cells(s.grass):
        y0 = (s.top[x, z] + 1 - YLO) * R
        col = F[x * R:(x + 1) * R, y0:y0 + 2 * R, z * R:(z + 1) * R]
        own = L.owner[x * R:(x + 1) * R, y0:y0 + 2 * R, z * R:(z + 1) * R]
        walk[x, z] = not (np.isin(col, stem_ids).any() or np.isin(col, hard_ids).sum() >= 16)
        strict[x, z] = walk[x, z] and np.isin(own, [CAT['giant'], CAT['tree'], CAT['bush']]).sum() < 16
    lab, n = ndimage.label(walk)
    sizes = np.bincount(lab.ravel())[1:] if n else np.array([0])
    res['walkable_share'] = round(float(walk.sum() / s.grass.sum()), 3)
    res['largest_walkable_region_share'] = round(float(sizes.max() / s.grass.sum()), 3)
    lab2, n2 = ndimage.label(strict); sz2 = np.bincount(lab2.ravel())[1:] if n2 else np.array([0])
    res['strict_walkable_share'] = round(float(strict.sum() / s.grass.sum()), 3)
    res['strict_largest_region_share'] = round(float(sz2.max() / s.grass.sum()), 3)
    cnt = collections.Counter((i['cat'], i['species']) for i in L.instances if 'count' not in i)
    for i in L.instances:
        if 'count' in i: cnt[(i['cat'], i['species'])] += i['count']
    res['counts'] = {f'{c}:{sp}': v for (c, sp), v in sorted(cnt.items())}
    res['veg_voxels'] = int((F > 0).sum())
    return res

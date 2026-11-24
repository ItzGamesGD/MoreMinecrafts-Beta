"""Shade a stored hit buffer with one palette.  Lighting, camera and context
(vegetation, logs, water) are identical for every palette."""
import numpy as np, sys, os, json
from PIL import Image
import raycast as rc
import palettes as pl

FACE_LIGHT = np.array([0.62, 0.62, 0.52, 1.0, 0.82, 0.82])  # -x +x -y +y -z +z
AO_LEVEL = np.array([0.50, 0.66, 0.82, 1.0])
AXIS = np.array([0, 0, 1, 1, 2, 2]); NSIGN = np.array([-1, 1, -1, 1, -1, 1])


def ao(grid, cell, face, local):
    occ = np.isin(grid, list(rc.K_OPAQUE) + [rc.K_LEAF])
    S = np.array(grid.shape)
    def solid(p):
        ok = (p >= 0).all(1) & (p < S).all(1)
        r = np.zeros(len(p), bool)
        q = p[ok]; r[ok] = occ[q[:, 0], q[:, 1], q[:, 2]]
        return r
    a = AXIS[face]; n = np.zeros((len(face), 3), int); n[np.arange(len(face)), a] = NSIGN[face]
    o = cell + n
    b1 = (a + 1) % 3; b2 = (a + 2) % 3
    e1 = np.zeros_like(n); e1[np.arange(len(face)), b1] = 1
    e2 = np.zeros_like(n); e2[np.arange(len(face)), b2] = 1
    f1 = local[np.arange(len(face)), b1]; f2 = local[np.arange(len(face)), b2]
    val = np.zeros(len(face))
    for s1 in (0, 1):
        for s2 in (0, 1):
            d1 = e1 * (2 * s1 - 1); d2 = e2 * (2 * s2 - 1)
            A = solid(o + d1); B = solid(o + d2); C = solid(o + d1 + d2)
            lvl = np.where(A & B, 0, 3 - (A.astype(int) + B + C))
            w = (f1 if s1 else 1 - f1) * (f2 if s2 else 1 - f2)
            val += w * AO_LEVEL[lvl]
    return val


def hash3(c):
    h = (c[:, 0].astype(np.int64) * 73856093) ^ (c[:, 1].astype(np.int64) * 19349663) ^ (c[:, 2].astype(np.int64) * 83492791)
    return np.abs(h)


def sky(H, W):
    t = np.linspace(0, 1, H)[:, None, None]
    return (np.array([150, 196, 255]) * (1 - t) + np.array([214, 233, 255]) * t) * np.ones((1, W, 1))


def shade(hbdir, view, palette_name, V, N, out_ids=False):
    grid = np.load(os.path.join(hbdir, 'grid.npy'))
    hb = np.load(os.path.join(hbdir, 'hit_%s.npz' % view))
    W, H = int(hb['W']), int(hb['H'])
    Wv, Hv, yaw, pitch, c, s = rc.VIEWS[view]
    P0, d = rc.camera(W, H, yaw, pitch, c, s)
    kind = hb['kind']; t = hb['t']; cell = hb['cell'].astype(int); face = hb['face']
    u = hb['u']; v = hb['v']; half = hb['half']; wt = hb['wt']
    img = sky(H, W).reshape(-1, 3).copy()
    ui = np.clip((u * 16).astype(int), 0, 15); vi = np.clip((v * 16).astype(int), 0, 15)
    pal = pl.PALETTES[palette_name]
    blk = np.full(len(kind), '', object)
    hit = kind > 0
    col = np.zeros((len(kind), 3))
    # opaque terrain + logs
    solid = np.nonzero(np.isin(kind, list(rc.K_OPAQUE)) & (face < 6))[0]
    hp = P0[solid] + d * t[solid, None]
    local = np.clip(hp - cell[solid], 0, 1)
    rot = hash3(cell[solid]) % 4
    for kid in np.unique(kind[solid]):
        name = rc.KINDS[kid]
        bname = pal.get(name, name)
        tex = (N.get(bname) or V.get(bname))
        m = kind[solid] == kid
        idxs = solid[m]
        f = face[idxs]
        for ft, faces in (('top', [3]), ('bottom', [2]), ('side', [0, 1, 4, 5])):
            mm = np.isin(f, faces)
            if not mm.any(): continue
            ii = idxs[mm]
            uu, vv = ui[ii], vi[ii]
            if ft == 'top' and name in ('grass_block', 'dirt', 'coarse_dirt'):
                r = rot[m][mm]
                uu2, vv2 = uu.copy(), vv.copy()
                for k in range(1, 4):
                    q = r == k
                    for _ in range(k):
                        uu2[q], vv2[q] = 15 - vv2[q], uu2[q]
                uu, vv = uu2, vv2
            T = tex[ft]
            cc = T[vv, uu]
            if ft == 'top' and 'top2' in tex:
                alt = (hash3(cell[ii]) // 7) % 3 == 0
                cc[alt] = tex['top2'][vv[alt], uu[alt]]
            col[ii] = cc
            blk[ii] = bname
    light = np.ones(len(kind))
    light[solid] = FACE_LIGHT[face[solid]] * ao(grid, cell[solid], face[solid], local)
    # leaves
    lf = np.nonzero(kind == rc.K_LEAF)[0]
    if lf.size:
        L = pl.tint(pl.load('oak_leaves'), pl.PLAINS_FOLIAGE)[..., :3]
        col[lf] = L[vi[lf], ui[lf]]
        light[lf] = FACE_LIGHT[np.clip(face[lf], 0, 5)] * 0.95
        blk[lf] = 'oak_leaves'
    # plants
    pli = np.nonzero(face == 6)[0]
    for kid in np.unique(kind[pli]):
        name = rc.KINDS[kid]; m = pli[kind[pli] == kid]
        if name == 'tall_grass':
            for h, tn in ((0, 'tall_grass_bottom'), (1, 'tall_grass_top')):
                mh = m[half[m] == h]
                T = pl.tint(pl.load(tn), pl.PLAINS_GRASS)[..., :3]
                col[mh] = T[vi[mh], ui[mh]]
        elif name == 'short_grass':
            T = pl.tint(pl.load('short_grass'), pl.PLAINS_GRASS)[..., :3]
            col[m] = T[vi[m], ui[m]]
        else:
            T = pl.load(name)[..., :3]
            col[m] = T[vi[m], ui[m]]
        light[m] = 0.92
        blk[m] = name
    lit = col * light[:, None]
    # water overlay
    w = np.isfinite(wt) & (wt < t)
    wc = np.array(pl.PLAINS_WATER, float)
    lit[w] = lit[w] * 0.45 + wc * 0.55
    img[hit] = lit[hit]
    img[w & ~hit] = img[w & ~hit] * 0.4 + wc * 0.6
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8).reshape(H, W, 3))
    if out_ids:
        return im, blk.reshape(H, W), face.reshape(H, W), w.reshape(H, W)
    return im


if __name__ == '__main__':
    hbdir, outdir = sys.argv[1], sys.argv[2]
    views = sys.argv[3].split(',') if len(sys.argv) > 3 else list(rc.VIEWS)
    pals = sys.argv[4].split(',') if len(sys.argv) > 4 else list(pl.PALETTES)
    os.makedirs(outdir, exist_ok=True)
    V, N = pl.build()
    for p in pals:
        for vw in views:
            im = shade(hbdir, vw, p, V, N)
            im.save(os.path.join(outdir, '%s_%s.png' % (p, vw)))
            print('saved', p, vw)

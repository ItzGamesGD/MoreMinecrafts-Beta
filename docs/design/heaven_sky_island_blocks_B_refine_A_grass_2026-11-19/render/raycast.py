"""Orthographic voxel ray caster for a single structure NBT.

Casts rays once per camera and stores a geometry hit buffer (block kind, cell,
face, texel uv, water overlay).  Shading with a palette is done separately so
all five palettes share exactly the same geometry, camera and lighting.
"""
import numpy as np, nbtlib, json, sys, os
from PIL import Image

import os
TEX = os.environ.get('MC_TEX', '../../../../../MoreMinecrafts-Structure-Generator/vendor/mcbuild/src/mcbuild/assets/textures/block/')
BASE = os.environ.get('SKY_NBT', '../../../../src/main/resources/data/moreminecrafts/structure/sky_islands/')

OPAQUE = ['grass_block', 'dirt', 'coarse_dirt', 'stone', 'andesite', 'tuff', 'oak_log']
LEAVES = ['oak_leaves']
PLANTS = ['short_grass', 'tall_grass', 'dandelion', 'poppy', 'allium', 'cornflower', 'azure_bluet', 'oxeye_daisy']
WATER = ['water']
KINDS = ['air'] + OPAQUE + LEAVES + PLANTS + WATER
KID = {k: i for i, k in enumerate(KINDS)}
K_OPAQUE = set(KID[k] for k in OPAQUE)
K_LEAF = KID['oak_leaves']
K_PLANT = set(KID[k] for k in PLANTS)
K_WATER = KID['water']


def load_grid(rel):
    n = nbtlib.load(BASE + rel)
    pal = [str(p['id']).replace('minecraft:', '') for p in n['palette']]
    props = [dict((str(k), str(v)) for k, v in p.get('properties', {}).items()) for p in n['palette']]
    S = [int(x) for x in n['size']]
    g = np.zeros(S, np.uint8)
    half = np.zeros(S, np.uint8)  # tall_grass upper half flag
    for b in n['blocks']:
        name = pal[int(b['state'])]
        if name not in KID:
            continue
        x, y, z = (int(v) for v in b['pos'])
        g[x, y, z] = KID[name]
        if name == 'tall_grass' and props[int(b['state'])].get('half') == 'upper':
            half[x, y, z] = 1
    ymax = int(np.nonzero(g.any(axis=(0, 2)))[0].max()) + 2
    return g[:, :ymax, :].copy(), half[:, :ymax, :].copy()


def rgba(name):
    im = Image.open(TEX + name + '.png').convert('RGBA')
    a = np.asarray(im)
    return a[:16, :16].copy()


def alpha_masks():
    m = {K_LEAF: rgba('oak_leaves')[..., 3] > 0}
    for k in PLANTS:
        if k == 'tall_grass':
            m[(KID[k], 0)] = rgba('tall_grass_bottom')[..., 3] > 0
            m[(KID[k], 1)] = rgba('tall_grass_top')[..., 3] > 0
        else:
            m[KID[k]] = rgba(k)[..., 3] > 0
    return m


def camera(W, H, yaw, pitch, center, scale):
    yaw, pitch = np.radians(yaw), np.radians(pitch)
    d = np.array([-np.cos(pitch) * np.sin(yaw), -np.sin(pitch), -np.cos(pitch) * np.cos(yaw)])
    up = np.array([0.0, 1.0, 0.0])
    r = np.cross(d, up); r /= np.linalg.norm(r)
    u = np.cross(r, d)
    ii, jj = np.meshgrid(np.arange(W) + 0.5, np.arange(H) + 0.5)
    P = (np.asarray(center, float)[None, None, :]
         + ((ii - W / 2) * scale)[..., None] * r
         + ((H / 2 - jj) * scale)[..., None] * u
         - d * 400.0)
    return P.reshape(-1, 3), d


def cast(grid, half, P0, d, masks):
    S = np.array(grid.shape, float)
    N = P0.shape[0]
    D = np.broadcast_to(d, (N, 3)).astype(float)
    inv = np.where(np.abs(D) < 1e-12, 1e12, 1.0 / np.where(np.abs(D) < 1e-12, 1, D))
    ta = (0 - P0) * inv; tb = (S - P0) * inv
    tmin = np.minimum(ta, tb); tmax = np.maximum(ta, tb)
    t0 = tmin.max(1); t1 = tmax.min(1)
    ent_axis = tmin.argmax(1)
    alive = (t1 > t0) & (t1 > 0)
    out_kind = np.zeros(N, np.int16); out_t = np.full(N, np.inf)
    out_cell = np.zeros((N, 3), np.int16); out_face = np.full(N, -1, np.int8)
    out_u = np.zeros(N, np.float32); out_v = np.zeros(N, np.float32)
    out_half = np.zeros(N, np.uint8)
    w_t = np.full(N, np.inf)
    idx = np.nonzero(alive)[0]
    P = P0[idx]; Dd = D[idx]; ivv = inv[idx]
    te = t0[idx]
    p = P + Dd * (te[:, None] + 1e-6)
    cell = np.clip(np.floor(p).astype(np.int32), 0, (S - 1).astype(np.int32))
    step = np.sign(Dd).astype(np.int32); step[step == 0] = 1
    tDelta = np.abs(ivv)
    tMax = ((cell + (step > 0)) - P) * ivv
    tMax[np.abs(Dd) < 1e-12] = np.inf
    axis = ent_axis[idx]
    Si = np.array(grid.shape)
    while idx.size:
        k = grid[cell[:, 0], cell[:, 1], cell[:, 2]]
        texit = tMax.min(1)
        done = np.zeros(idx.size, bool)
        # face hit point for entry
        hp = P + Dd * te[:, None]
        local = hp - cell
        normal_sign = -step[np.arange(idx.size), axis]
        face = axis * 2 + (normal_sign > 0)  # 0:-x 1:+x 2:-y 3:+y 4:-z 5:+z
        fu, fv = face_uv(local, face)
        op = np.isin(k, list(K_OPAQUE))
        lf = k == K_LEAF
        if lf.any():
            ui = np.clip((fu[lf] * 16).astype(int), 0, 15); vi = np.clip((fv[lf] * 16).astype(int), 0, 15)
            op[np.nonzero(lf)[0][masks[K_LEAF][vi, ui]]] = True
        sel = np.nonzero(op)[0]
        if sel.size:
            gi = idx[sel]
            out_kind[gi] = k[sel]; out_t[gi] = te[sel]; out_cell[gi] = cell[sel]
            out_face[gi] = face[sel]; out_u[gi] = fu[sel]; out_v[gi] = fv[sel]
            done[sel] = True
        # water: remember first entry
        wt = np.nonzero((k == K_WATER) & ~np.isfinite(w_t[idx]))[0]
        if wt.size:
            w_t[idx[wt]] = te[wt]
        # plants: two diagonal planes
        pl = np.nonzero(np.isin(k, list(K_PLANT)))[0]
        if pl.size:
            best_t = np.full(pl.size, np.inf); bu = np.zeros(pl.size); bv = np.zeros(pl.size)
            Pp = P[pl] - cell[pl]; Dp = Dd[pl]
            for plane in (0, 1):
                if plane == 0:  # x - z = 0
                    den = Dp[:, 0] - Dp[:, 2]; num = -(Pp[:, 0] - Pp[:, 2])
                else:          # x + z - 1 = 0
                    den = Dp[:, 0] + Dp[:, 2]; num = 1 - (Pp[:, 0] + Pp[:, 2])
                with np.errstate(divide='ignore', invalid='ignore'):
                    tt = num / den
                q = Pp + Dp * tt[:, None]
                ok = (tt >= te[pl] - 1e-6) & (tt <= texit[pl] + 1e-6) & (q[:, 0] >= 0) & (q[:, 0] <= 1) & (q[:, 1] >= 0) & (q[:, 1] <= 1)
                uu = q[:, 0]; vv = 1 - q[:, 1]
                kk = k[pl]
                hh = half[cell[pl, 0], cell[pl, 1], cell[pl, 2]]
                ui = np.clip((uu * 16).astype(int), 0, 15); vi = np.clip((vv * 16).astype(int), 0, 15)
                a = np.zeros(pl.size, bool)
                for kind in np.unique(kk[ok]):
                    m = ok & (kk == kind)
                    if kind == KID['tall_grass']:
                        for h in (0, 1):
                            mh = m & (hh == h)
                            a[mh] = masks[(kind, h)][vi[mh], ui[mh]]
                    else:
                        a[m] = masks[kind][vi[m], ui[m]]
                better = a & (tt < best_t)
                best_t[better] = tt[better]; bu[better] = uu[better]; bv[better] = vv[better]
            hit = np.nonzero(np.isfinite(best_t))[0]
            if hit.size:
                s2 = pl[hit]; gi = idx[s2]
                out_kind[gi] = k[s2]; out_t[gi] = best_t[hit]; out_cell[gi] = cell[s2]
                out_face[gi] = 6; out_u[gi] = bu[hit]; out_v[gi] = bv[hit]
                out_half[gi] = half[cell[s2, 0], cell[s2, 1], cell[s2, 2]]
                done[s2] = True
        # advance
        ax = tMax.argmin(1)
        r = np.arange(idx.size)
        cell[r, ax] += step[r, ax]
        te = tMax[r, ax].copy()
        tMax[r, ax] += tDelta[r, ax]
        axis = ax
        inside = (cell >= 0).all(1) & (cell < Si).all(1)
        keep = ~done & inside
        idx = idx[keep]; P = P[keep]; Dd = Dd[keep]; cell = cell[keep]; step = step[keep]
        tDelta = tDelta[keep]; tMax = tMax[keep]; te = te[keep]; axis = axis[keep]
    return dict(kind=out_kind, t=out_t, cell=out_cell, face=out_face, u=out_u, v=out_v, half=out_half, wt=w_t)


def face_uv(local, face):
    x, y, z = local[:, 0], local[:, 1], local[:, 2]
    u = np.zeros_like(x); v = np.zeros_like(x)
    # top/bottom
    m = face == 3; u[m] = x[m]; v[m] = z[m]
    m = face == 2; u[m] = x[m]; v[m] = 1 - z[m]
    m = face == 1; u[m] = 1 - z[m]; v[m] = 1 - y[m]   # +x (east)
    m = face == 0; u[m] = z[m]; v[m] = 1 - y[m]       # -x (west)
    m = face == 5; u[m] = x[m]; v[m] = 1 - y[m]       # +z (south)
    m = face == 4; u[m] = 1 - x[m]; v[m] = 1 - y[m]   # -z (north)
    return np.clip(u, 0, 0.9999), np.clip(v, 0, 0.9999)


VIEWS = {
    # name: W, H, yaw, pitch, center(x,y,z), scale (blocks per pixel)
    'full':      (1600, 1000, 35, 14, (22.5, 11.5, 18.5), 0.040),
    'edge':      (1200, 800, 35, 12, (29.0, 9.5, 31.0), 0.0150),
    'underside': (1200, 800, 35, -30, (24.0, 5.0, 22.0), 0.026),
}

if __name__ == '__main__':
    rel = sys.argv[1]; outdir = sys.argv[2]
    views = sys.argv[3:] or list(VIEWS)
    os.makedirs(outdir, exist_ok=True)
    grid, half = load_grid(rel)
    np.save(os.path.join(outdir, 'grid.npy'), grid); np.save(os.path.join(outdir, 'half.npy'), half)
    masks = alpha_masks()
    for v in views:
        W, H, yaw, pitch, c, s = VIEWS[v]
        P0, d = camera(W, H, yaw, pitch, c, s)
        hb = cast(grid, half, P0, d, masks)
        np.savez_compressed(os.path.join(outdir, 'hit_%s.npz' % v), W=W, H=H, d=d, **hb)
        print(v, 'hit px', int((hb['kind'] > 0).sum()), 'of', W * H)

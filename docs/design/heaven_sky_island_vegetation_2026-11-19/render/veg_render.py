"""Composite renderer: NBT terrain (block grid, textured, from raycast.py) +
vegetation prototypes (quarter-block voxel grid, flat-coloured with per-voxel
jitter).  Same orthographic camera for both; per-pixel depth composite; MC-style
face light + vertex AO on both; directional shadows cast by vegetation."""
import numpy as np
from PIL import Image
import raycast as rc, palettes_veg as pl, veg_models as vm

R = vm.R
FACE_LIGHT = np.array([0.62, 0.62, 0.52, 1.0, 0.82, 0.82])
AO_LEVEL = np.array([0.50, 0.66, 0.82, 1.0])
AXIS = np.array([0, 0, 1, 1, 2, 2]); NSIGN = np.array([-1, 1, -1, 1, -1, 1])
SUN = np.array([0.45, 1.0, -0.35]); SUN = SUN / np.linalg.norm(SUN)
SHADOW = 0.70
TERRAIN_KEEP = set(rc.K_OPAQUE) - {rc.KID['oak_log']} | {rc.K_WATER}


def strip_vegetation(grid):
    g = grid.copy()
    g[~np.isin(g, list(TERRAIN_KEEP))] = 0
    return g


# ------------------------------------------------------------ generic DDA (opaque voxels)

def cast_opaque(G, P0, D, tmax=None):
    """Ray-cast through occupancy grid G (nonzero = hit).  P0 (N,3) in grid units,
    D (3,) or (N,3).  Returns t (inf = miss), cell (N,3), face (N,)."""
    S = np.array(G.shape, float); N = P0.shape[0]
    D = np.broadcast_to(np.asarray(D, float), (N, 3))
    inv = np.where(np.abs(D) < 1e-12, 1e12, 1.0 / np.where(np.abs(D) < 1e-12, 1, D))
    ta = (0 - P0) * inv; tb = (S - P0) * inv
    tmin = np.minimum(ta, tb); tmx = np.maximum(ta, tb)
    t0 = np.maximum(tmin.max(1), 0); t1 = tmx.min(1)
    if tmax is not None:
        t1 = np.minimum(t1, tmax)
    out_t = np.full(N, np.inf); out_cell = np.zeros((N, 3), np.int32); out_face = np.full(N, -1, np.int8)
    alive = t1 > t0
    idx = np.nonzero(alive)[0]
    P = P0[idx]; Dd = D[idx]; ivv = inv[idx]; te = t0[idx]; tlim = t1[idx]
    ent_axis = np.where(tmin.max(1) > 0, tmin.argmax(1), -1)[idx]
    p = P + Dd * (te[:, None] + 1e-6)
    cell = np.clip(np.floor(p).astype(np.int32), 0, (S - 1).astype(np.int32))
    step = np.sign(Dd).astype(np.int32); step[step == 0] = 1
    tDelta = np.abs(ivv)
    tMax = ((cell + (step > 0)) - P) * ivv
    tMax[np.abs(Dd) < 1e-12] = np.inf
    axis = ent_axis.copy()
    Si = np.array(G.shape)
    while idx.size:
        k = G[cell[:, 0], cell[:, 1], cell[:, 2]]
        hit = np.nonzero(k > 0)[0]
        if hit.size:
            gi = idx[hit]; ax = axis[hit]
            ax2 = np.where(ax < 0, 1, ax)
            out_t[gi] = te[hit]; out_cell[gi] = cell[hit]
            out_face[gi] = ax2 * 2 + (-step[hit, ax2] > 0)
        done = k > 0
        ax = tMax.argmin(1); r = np.arange(idx.size)
        cell[r, ax] += step[r, ax]; te = tMax[r, ax].copy(); tMax[r, ax] += tDelta[r, ax]; axis = ax
        inside = (cell >= 0).all(1) & (cell < Si).all(1)
        keep = ~done & inside & (te < tlim)
        idx = idx[keep]; P = P[keep]; Dd = Dd[keep]; cell = cell[keep]; step = step[keep]
        tDelta = tDelta[keep]; tMax = tMax[keep]; te = te[keep]; axis = axis[keep]; tlim = tlim[keep]
    return out_t, out_cell, out_face


def vertex_ao(occ, cell, face, local):
    S = np.array(occ.shape)
    def solid(p):
        ok = (p >= 0).all(1) & (p < S).all(1); r = np.zeros(len(p), bool)
        q = p[ok]; r[ok] = occ[q[:, 0], q[:, 1], q[:, 2]]; return r
    n_ = len(face); a = AXIS[face]
    n = np.zeros((n_, 3), int); n[np.arange(n_), a] = NSIGN[face]
    o = cell + n; b1 = (a + 1) % 3; b2 = (a + 2) % 3
    e1 = np.zeros_like(n); e1[np.arange(n_), b1] = 1; e2 = np.zeros_like(n); e2[np.arange(n_), b2] = 1
    f1 = local[np.arange(n_), b1]; f2 = local[np.arange(n_), b2]
    val = np.zeros(n_)
    for s1 in (0, 1):
        for s2 in (0, 1):
            d1 = e1 * (2 * s1 - 1); d2 = e2 * (2 * s2 - 1)
            A = solid(o + d1); B = solid(o + d2); C = solid(o + d1 + d2)
            lvl = np.where(A & B, 0, 3 - (A.astype(int) + B + C))
            val += (f1 if s1 else 1 - f1) * (f2 if s2 else 1 - f2) * AO_LEVEL[lvl]
    return val


def normals(face):
    n = np.zeros((len(face), 3)); n[np.arange(len(face)), AXIS[face]] = NSIGN[face]; return n


# ------------------------------------------------------------ scene

class Scene:
    """terrain: block grid (rc kinds); fine: vegetation material grid; fine_origin: world
    position of fine index 0 (blocks)."""
    def __init__(self, terrain, fine, fine_origin, palette='HG'):
        self.terrain = terrain; self.fine = fine; self.fo = np.asarray(fine_origin, float)
        self.palette = palette
        self.V, self.N = pl.build()


def terrain_colors(S, hb, P0, d):
    """Textured terrain colours from a raycast.cast() hit buffer."""
    kind = hb['kind']; t = hb['t']; cell = hb['cell'].astype(int); face = hb['face']
    ui = np.clip((hb['u'] * 16).astype(int), 0, 15); vi = np.clip((hb['v'] * 16).astype(int), 0, 15)
    pal = pl.PALETTES[S.palette]
    col = np.zeros((len(kind), 3)); light = np.ones(len(kind))
    solid = np.nonzero(np.isin(kind, list(rc.K_OPAQUE)) & (face >= 0) & (face < 6))[0]
    hp = P0[solid] + d * t[solid, None]; local = np.clip(hp - cell[solid], 0, 1)
    h = (np.abs((cell[solid, 0].astype(np.int64) * 73856093) ^ (cell[solid, 1].astype(np.int64) * 19349663) ^ (cell[solid, 2].astype(np.int64) * 83492791)))
    rot = h % 4
    for kid in np.unique(kind[solid]):
        name = rc.KINDS[kid]; tex = S.N.get(pal.get(name, name)) or S.V.get(pal.get(name, name))
        m = kind[solid] == kid; ii = solid[m]; f = face[ii]
        for ft, faces in (('top', [3]), ('bottom', [2]), ('side', [0, 1, 4, 5])):
            mm = np.isin(f, faces)
            if not mm.any(): continue
            jj = ii[mm]; uu, vv = ui[jj].copy(), vi[jj].copy()
            if ft == 'top' and name in ('grass_block', 'dirt', 'coarse_dirt'):
                r = rot[m][mm]
                for k in range(1, 4):
                    q = r == k
                    for _ in range(k):
                        uu[q], vv[q] = 15 - vv[q], uu[q].copy()
            col[jj] = tex[ft][vv, uu]
    occ = np.isin(S.terrain, list(rc.K_OPAQUE))
    light[solid] = FACE_LIGHT[face[solid]] * vertex_ao(occ, cell[solid], face[solid], local)
    return col, light, solid


def render(S, view, W=None, H=None, yaw=None, pitch=None, center=None, scale=None, out_ids=False, sky=True):
    if view is not None:
        W, H, yaw, pitch, center, scale = rc.VIEWS[view]
    P0, d = rc.camera(W, H, yaw, pitch, center, scale)
    Np = P0.shape[0]
    # terrain
    hb = rc.cast(S.terrain, np.zeros_like(S.terrain), P0, d, rc.alpha_masks())
    tcol, tlight, tsolid = terrain_colors(S, hb, P0, d)
    t_ter = np.where(np.isin(hb['kind'], list(rc.K_OPAQUE)), hb['t'], np.inf)
    # vegetation (fine grid units: (world - fo) * R; t scales by R)
    Pf = (P0 - S.fo) * R
    tf, cf, ff = cast_opaque(S.fine, Pf, d, tmax=np.where(np.isfinite(t_ter), t_ter * R + 1e-3, np.inf))
    t_veg = tf / R
    use_veg = t_veg < t_ter
    hit = use_veg | np.isfinite(t_ter)
    col = np.zeros((Np, 3)); light = np.ones(Np)
    col[~use_veg] = tcol[~use_veg]; light[~use_veg] = tlight[~use_veg]
    vi = np.nonzero(use_veg)[0]
    if vi.size:
        c = cf[vi]; m = S.fine[c[:, 0], c[:, 1], c[:, 2]]
        hsh = (np.abs((c[:, 0].astype(np.int64) * 73856093) ^ (c[:, 1].astype(np.int64) * 19349663) ^ (c[:, 2].astype(np.int64) * 83492791)) % 1000) / 1000.0
        col[vi] = vm.MAT_RGB[m] * (0.93 + 0.14 * hsh)[:, None]
        hp = Pf[vi] + d * tf[vi, None]; local = np.clip(hp - c, 0, 1)
        light[vi] = FACE_LIGHT[ff[vi]] * vertex_ao(S.fine > 0, c, ff[vi], local)
    # shadows from vegetation (sun direction), applied to every hit pixel
    hi = np.nonzero(hit)[0]
    if hi.size:
        tw = np.where(use_veg[hi], t_veg[hi], t_ter[hi])
        nrm = np.zeros((hi.size, 3))
        vsel = use_veg[hi]
        nrm[vsel] = normals(ff[hi[vsel]])
        ts = ~vsel; tfc = hb['face'][hi[ts]]
        okf = (tfc >= 0) & (tfc < 6); tmp = np.zeros((ts.sum(), 3)); tmp[okf] = normals(tfc[okf]); nrm[ts] = tmp
        pw = P0[hi] + d * tw[:, None] + nrm * 0.02
        ts_, _, _ = cast_opaque(S.fine, (pw - S.fo) * R + SUN * 0.3, SUN)
        sh = np.isfinite(ts_) & ((nrm @ SUN) > 0)   # faces turned away from the sun are already dark
        light[hi[sh]] *= SHADOW
    img = col * light[:, None]
    # water (terrain buffer) in front of terrain hit, behind vegetation
    w = np.isfinite(hb['wt']) & (hb['wt'] < np.where(use_veg, t_veg, t_ter))
    wc = np.array(pl.PLAINS_WATER, float)
    img[w] = img[w] * 0.45 + wc * 0.55
    bg = sky_bg(H, W).reshape(-1, 3) if sky else np.full((Np, 3), 245.0)
    img[~hit & ~w] = bg[~hit & ~w]
    img[~hit & w] = bg[~hit & w] * 0.4 + wc * 0.6
    im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8).reshape(H, W, 3))
    if out_ids:
        vm_id = np.zeros(Np, np.int16)
        if vi.size: vm_id[vi] = S.fine[cf[vi, 0], cf[vi, 1], cf[vi, 2]]
        return im, dict(use_veg=use_veg.reshape(H, W), veg_mat=vm_id.reshape(H, W), water=w.reshape(H, W),
                        terrain_kind=hb['kind'].reshape(H, W), hit=hit.reshape(H, W))
    return im


def sky_bg(H, W):
    t = np.linspace(0, 1, H)[:, None, None]
    return (np.array([150, 196, 255]) * (1 - t) + np.array([214, 233, 255]) * t) * np.ones((1, W, 1))

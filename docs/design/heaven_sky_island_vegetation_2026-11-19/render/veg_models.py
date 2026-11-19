"""Vegetation design prototypes as voxel models at quarter-block resolution
(R = 4 voxels per block edge).  These are DESIGN PROTOTYPES: they stand in for
future custom block models / multiblock structures and are not game assets.

Every generator returns a Model: a dict {(x, y, z): material} in fine units
relative to the base block's bottom centre (axis at x=z=0, y=0 = top of the
supporting grass block).  Materials are keys of MAT."""
import numpy as np

R = 4  # voxels per block

MAT = {
    # stems / leaves
    'stem': '#4f9a33', 'stem_s': '#4f9a33', 'stem_dk': '#3d7f27', 'leaf': '#4fae38', 'leaf_lt': '#6cc44a', 'leaf_dk': '#36852a', 'vein': '#8fd665',
    # tulip (red)
    'tul_out': '#d8332b', 'tul_in': '#a8191f', 'tul_tip': '#f0574a', 'tul_base': '#f2c13a',
    # lily (orange)
    'lil_out': '#f08a1e', 'lil_in': '#ffae3a', 'lil_spot': '#b5441a', 'fil': '#d8e6a0', 'anther': '#7a3a12',
    # daffodil (yellow)
    'daf_pet': '#ffe45a', 'daf_back': '#e8d44a', 'daf_cor': '#ffb21e', 'daf_rim': '#ff8c14',
    # peony (pink)
    'peo_1': '#c8306e', 'peo_2': '#e0538c', 'peo_3': '#f07aa8', 'peo_4': '#fba3c4', 'peo_5': '#ffc8dc', 'sepal': '#3f8f2c',
    # fantasy star lotus (violet -> white)
    'fan_1': '#6a3fd0', 'fan_2': '#8a62e6', 'fan_3': '#b195f2', 'fan_4': '#d9cdfb', 'fan_5': '#f6f2ff',
    'fan_core': '#ffe08a', 'fan_crys': '#bdf2ff', 'bud': '#7d5bc9',
    # medium / small flowers
    'lav': '#8a5ad8', 'lav_lt': '#a97cf0', 'bell': '#f7f7ef', 'bell_sh': '#dfe3d4',
    'phl_p': '#f06fb0', 'phl_c': '#ffe0f0', 'yel_p': '#ffd52e', 'yel_c': '#f39a12',
    'hyd_1': '#4f86e8', 'hyd_2': '#6fa3f5', 'hyd_3': '#9cc2ff',
    'sm_red': '#e23a33', 'sm_yel': '#ffd02a', 'sm_wht': '#f7f5ea', 'sm_pnk': '#f27ab8', 'sm_blu': '#3f7fe6', 'sm_vio': '#9a6ae6', 'sm_ctr': '#f6b21c',
    # grass tufts (matched to heaven_grass_A ramp)
    'gr_1': '#3f9a2c', 'gr_2': '#52b437', 'gr_3': '#67c943', 'gr_4': '#86da5a',
    # trees
    'bark': '#8a6440', 'bark_dk': '#6b4a2e', 'tl_1': '#3f9a2c', 'tl_2': '#52b437', 'tl_3': '#6cc44a',
    'bl_1': '#f28dbb', 'bl_2': '#f7a9cb', 'bl_3': '#ffc6de', 'wt_1': '#f4f1e6', 'wt_2': '#fffdf6', 'wt_3': '#e6ead6',
    # vines / wisteria / ground
    'vine': '#4c9f35', 'vine_lt': '#6cc44a', 'wis_1': '#7b55c9', 'wis_2': '#9a78e0', 'wis_3': '#c2abf3',
    'petal_g': '#ffb3d1', 'bigleaf': '#3f9a2c', 'bigleaf_lt': '#5cb83f',
    'player': '#5a7fb8', 'player_hd': '#d9a77c',
}
MAT_NAMES = list(MAT)
MAT_ID = {k: i + 1 for i, k in enumerate(MAT_NAMES)}


def rgb(h):
    h = h.lstrip('#'); return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


MAT_RGB = np.zeros((len(MAT_NAMES) + 1, 3)); MAT_RGB[1:] = [rgb(MAT[k]) for k in MAT_NAMES]


class Model(dict):
    def put(self, p, m, over=True):
        k = (int(round(p[0])), int(round(p[1])), int(round(p[2])))
        if over or k not in self:
            self[k] = m

    def ball(self, c, r, m, over=True, keep=None):
        c = np.asarray(c, float); ri = int(np.ceil(r))
        for x in range(-ri, ri + 1):
            for y in range(-ri, ri + 1):
                for z in range(-ri, ri + 1):
                    if x * x + y * y + z * z <= r * r + 0.25:
                        if keep is None or keep(x, y, z):
                            self.put(c + (x, y, z), m, over)

    def line(self, a, b, m, r=0.0, over=True):
        a = np.asarray(a, float); b = np.asarray(b, float)
        n = int(np.ceil(np.linalg.norm(b - a) * 2)) + 1
        for t in np.linspace(0, 1, n):
            p = a + (b - a) * t
            if r <= 0.5:
                self.put(p, m, over)
            else:
                self.ball(p, r, m, over)

    def merge(self, other, off=(0, 0, 0), over=True):
        for (x, y, z), m in other.items():
            self.put((x + off[0], y + off[1], z + off[2]), m, over)


def rot_y(p, a):
    c, s = np.cos(a), np.sin(a); x, y, z = p; return np.array([c * x - s * z, y, s * x + c * z])


def rot_x(p, a):
    c, s = np.cos(a), np.sin(a); x, y, z = p; return np.array([x, c * y - s * z, s * y + c * z])


# ------------------------------------------------------------------ parts

def stem(M, H, bend=(0.0, 0.0), r0=2.0, r1=1.2, mat='stem', t_bend=0.8):
    """Curved stem from base to (bend_x, H, bend_z); returns top point and point fn."""
    pts = []
    n = int(H * 1.6) + 2
    for i in range(n + 1):
        t = i / n
        off = np.sin(np.pi * t * t_bend / 1.0) if t_bend else t
        p = np.array([bend[0] * off, H * t, bend[1] * off])
        r = r0 * (1 - t) + r1 * t
        M.ball(p, r, mat if (i % 7) else 'stem_dk', over=False)
        pts.append(p)
    return pts


def leaf(M, at, ang, L, W, rise=0.6, droop=1.0, mat='leaf', cup=0.25):
    """Big blade leaf: midrib rises then droops; cupped across width."""
    d = np.array([np.cos(ang), 0, np.sin(ang)]); perp = np.array([-np.sin(ang), 0, np.cos(ang)])
    at = np.asarray(at, float)
    ns = int(L * 2.5) + 2
    for i in range(ns + 1):
        s = i / ns
        mid = at + d * (L * s) + np.array([0, L * (rise * s - droop * s * s), 0])
        w = W * np.sin(np.pi * min(s * 1.05, 1.0)) ** 0.7 * (1 - 0.25 * s)
        nv = int(np.ceil(w * 2)) + 1
        for v in np.linspace(-w, w, nv * 2 + 1) if w > 0.3 else [0]:
            p = mid + perp * v + np.array([0, cup * v * v / max(W, 1), 0])
            m = 'vein' if abs(v) < 0.5 and 0.1 < s < 0.85 else (mat if (i + int(v * 2)) % 5 else 'leaf_lt')
            M.put(p, m)


def petal_strip(M, c, ang, L, W, prof, mat_up, mat_dn, tilt=0.0, spots=None, grad=None, rng=None):
    """Petal as a strip along a profile prof(s)->(r, y); two layers (upper/lower)."""
    c = np.asarray(c, float)
    ns = int(L * 2.5) + 2
    for i in range(ns + 1):
        s = i / ns
        r, y = prof(s)
        w = W(s)
        base = np.array([r, y, 0.0])
        # local tangent of profile for layer offset (normal in r-y plane)
        r2, y2 = prof(min(s + 0.02, 1)); r1, y1 = prof(max(s - 0.02, 0))
        tg = np.array([r2 - r1, y2 - y1]); tg /= (np.linalg.norm(tg) + 1e-9)
        nrm = np.array([-tg[1], tg[0]])  # points "up/inward"
        nv = max(1, int(np.ceil(w * 2)))
        for v in np.linspace(-w, w, nv * 2 + 1) if w > 0.25 else [0.0]:
            for layer, mm in ((0.0, mat_up), (-1.0, mat_dn)):
                p = np.array([base[0] + nrm[0] * layer, base[1] + nrm[1] * layer, v])
                if grad is not None and layer == 0.0:
                    mm = grad(s)
                if spots is not None and layer == 0.0 and rng is not None and 0.15 < s < 0.6 and rng.random() < spots:
                    mm = 'lil_spot'
                q = rot_y(p, ang)
                M.put(c + q, mm)


# ------------------------------------------------------------------ giant flowers (H in blocks)

def giant_tulip(H=10, seed=1):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    Hb = 0.30 * Hu; Rb = 0.20 * Hu; Hs = Hu - Hb * 0.85
    pts = stem(M, Hs, bend=(0.10 * Hu, 0.04 * Hu), r0=2.2, r1=1.4)
    top = pts[-1]
    for k, (a, h) in enumerate([(0.3, 0.12), (2.4, 0.2), (4.3, 0.32)]):
        leaf(M, (top[0] * h, Hs * h, top[2] * h), a, L=0.42 * Hu, W=0.10 * Hu, rise=0.9, droop=0.9)
    c = top
    for layer, (n0, rs, hs, mo, mi) in enumerate([(0.0, 1.0, 1.0, 'tul_out', 'tul_in'), (np.pi / 3, 0.82, 1.08, 'tul_tip', 'tul_in')]):
        for k in range(3):
            phi = n0 + k * 2 * np.pi / 3
            for hi in np.arange(0, Hb * hs, 0.5):
                h = hi / (Hb * hs)
                rr = Rb * rs * (0.42 + 0.58 * np.sin(np.pi * (0.12 + 0.72 * h)))
                aw = (np.pi / 3) * (1.18 - 0.75 * h ** 2.2)
                for a in np.arange(phi - aw, phi + aw, 0.6 / max(rr, 1)):
                    for dr, m in ((0, mo if h < 0.92 else 'tul_tip'), (-1, mi)):
                        M.put(c + np.array([(rr + dr) * np.cos(a), hi, (rr + dr) * np.sin(a)]), m)
    M.ball(c + (0, 1, 0), Rb * 0.45, 'tul_in', over=False)
    for k in range(3):
        a = k * 2 * np.pi / 3 + 0.5
        M.line(c + (0, 1, 0), c + (1.6 * np.cos(a), Hb * 0.45, 1.6 * np.sin(a)), 'tul_base')
    return M


def giant_lily(H=10, seed=2):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    Hs = 0.80 * Hu
    pts = stem(M, Hs, bend=(-0.06 * Hu, 0.08 * Hu), r0=2.0, r1=1.2)
    top = pts[-1]
    for a, h in [(1.0, 0.15), (3.3, 0.28), (5.2, 0.42), (2.0, 0.55)]:
        leaf(M, (top[0] * h, Hs * h, top[2] * h), a, L=0.30 * Hu, W=0.05 * Hu, rise=0.5, droop=1.2)
    # side bud on a branching stem
    b0 = pts[int(len(pts) * 0.62)]
    tip = b0 + np.array([0.12 * Hu, 0.16 * Hu, -0.05 * Hu])
    M.line(b0, tip, 'stem', r=1.0)
    M.ball(tip + (0, 2, 0), 2.2, 'lil_out'); M.ball(tip + (0, 4, 0), 1.6, 'lil_out')
    c = top + np.array([0, 1, 0])
    Lp = 0.29 * Hu
    prof = lambda s: (Lp * s, Lp * (0.95 * s - 0.85 * s * s))
    W = lambda s: 0.065 * Hu * np.sin(np.pi * min(s ** 0.8, 1)) ** 0.8 * (1 - 0.35 * s) + 0.3
    for k in range(6):
        ang = k * np.pi / 3 + (0.15 if k % 2 else 0)
        petal_strip(M, c, ang, Lp, W, prof, 'lil_in', 'lil_out', spots=0.12, rng=rng)
    M.ball(c, 1.6, 'lil_in')
    for k in range(6):
        a = k * np.pi / 3 + 0.5
        e = c + np.array([0.38 * Lp * np.cos(a), 0.75 * Lp, 0.38 * Lp * np.sin(a)])
        M.line(c, e, 'fil'); M.put(e + (0, 1, 0), 'anther'); M.put(e + (1, 1, 0), 'anther')
    M.line(c, c + (0, 0.9 * Lp, 0), 'fil'); M.ball(c + (0, 0.9 * Lp + 1, 0), 0.9, 'leaf_lt')
    return M


def giant_daffodil(H=10, seed=3):
    M = Model(); Hu = H * R
    Hs = 0.84 * Hu
    pts = stem(M, Hs, bend=(0.05 * Hu, -0.03 * Hu), r0=1.8, r1=1.2)
    top = pts[-1]
    for a, h in [(0.6, 0.0), (2.2, 0.0), (3.9, 0.0), (5.3, 0.0)]:
        leaf(M, (0, 1, 0), a, L=0.55 * Hu, W=0.045 * Hu, rise=1.4, droop=1.05, mat='leaf_dk')
    # nodding neck
    neck = top + np.array([0.10 * Hu, 0.02 * Hu, 0.06 * Hu])
    M.line(top, neck, 'stem', r=1.2)
    B = Model()
    Lp = 0.20 * Hu
    prof = lambda s: (Lp * (0.25 + 0.75 * s), 0.12 * Lp * s)
    W = lambda s: 0.075 * Hu * np.sin(np.pi * (0.18 + 0.82 * s)) ** 0.7 + 0.3
    for k in range(6):
        petal_strip(B, (0, 0, 0), k * np.pi / 3, Lp, W, prof, 'daf_pet', 'daf_back')
    Rc = 0.36 * Lp; Lc = 0.62 * Lp
    for y in np.arange(0, Lc, 0.5):
        rr = Rc * (0.85 + 0.25 * y / Lc)
        for a in np.arange(0, 2 * np.pi, 0.5 / rr):
            B.put((rr * np.cos(a), y, rr * np.sin(a)), 'daf_cor')
            B.put(((rr - 1) * np.cos(a), y, (rr - 1) * np.sin(a)), 'daf_rim' if y > Lc * 0.7 else 'daf_cor', over=False)
    for a in np.arange(0, 2 * np.pi, 0.25):
        rr = Rc * 1.12 + (0.8 if np.sin(8 * a) > 0 else 0)
        B.put((rr * np.cos(a), Lc, rr * np.sin(a)), 'daf_rim')
    B.ball((0, 1, 0), Rc * 0.6, 'daf_rim', over=False)
    # tilt bloom: face sideways-down toward +x+z (camera side)
    yaw = np.pi * 0.25; pitch = np.radians(70)
    for (x, y, z), m in B.items():
        p = rot_y(rot_x(np.array([x, y, z], float), pitch), yaw - np.pi / 2)
        M.put(neck + p, m)
    return M


def giant_peony(H=10, seed=4):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    Rb = 0.19 * Hu; Hs = Hu - 2 * Rb
    pts = stem(M, Hs, bend=(0.02 * Hu, 0.03 * Hu), r0=2.4, r1=1.6, t_bend=0.5)
    top = pts[-1]
    for a, h in [(0.2, 0.18), (1.9, 0.26), (3.4, 0.34), (4.9, 0.22)]:
        p0 = (top[0] * h, Hs * h, top[2] * h)
        leaf(M, p0, a, L=0.24 * Hu, W=0.085 * Hu, rise=0.6, droop=0.9, mat='leaf_dk')
        leaf(M, p0, a + 0.5, L=0.17 * Hu, W=0.07 * Hu, rise=0.8, droop=0.9)
    # side bud
    b0 = pts[int(len(pts) * 0.7)]; tip = b0 + np.array([-0.10 * Hu, 0.10 * Hu, 0.06 * Hu])
    M.line(b0, tip, 'stem', r=1.0); M.ball(tip, 2.0, 'peo_2'); M.ball(tip + (0, -1.5, 0), 1.6, 'sepal', over=False)
    c = top + np.array([0, Rb * 0.9, 0])
    ri = int(Rb) + 2
    cols = ['peo_1', 'peo_2', 'peo_3', 'peo_4', 'peo_5']
    for x in range(-ri, ri + 1):
        for y in range(-ri, ri + 1):
            for z in range(-ri, ri + 1):
                d = np.sqrt(x * x + y * y * 1.15 + z * z)
                if d > Rb or y < -0.55 * Rb:
                    continue
                a = np.arctan2(z, x); el = np.arcsin(np.clip(y / max(d, 1e-6), -1, 1))
                ruff = np.sin(5 * a + 3 * el + d * 0.9) * 0.5 + np.sin(9 * a - 2 * el) * 0.3
                shell = (d / 1.45) % 1.0
                if d < 0.28 * Rb or (shell < 0.55 + 0.25 * ruff and d > 0.28 * Rb) or (d > Rb - 1.2 and ruff > -0.2):
                    M.put(c + (x, y, z), cols[min(4, int(d / Rb * 5))])
    # cupped guard petals and sepals underneath
    for k in range(5):
        a = k * 2 * np.pi / 5 + 0.3
        prof = lambda s: (Rb * (0.3 + 0.9 * s), -0.45 * Rb + Rb * 0.7 * s * s)
        W = lambda s: 0.55 * Rb * np.sin(np.pi * (0.1 + 0.85 * s)) ** 0.6 + 0.3
        petal_strip(M, c, a, Rb, W, prof, 'peo_4', 'peo_3')
    for k in range(5):
        a = k * 2 * np.pi / 5
        M.line(c + (0, -0.55 * Rb, 0), c + (0.75 * Rb * np.cos(a), -0.75 * Rb, 0.75 * Rb * np.sin(a)), 'sepal', r=0.8)
    return M


def giant_starlotus(H=10, seed=5):
    """Fantasy form: two crowns of long pointed petals (violet base -> white tip),
    crystalline golden core spire, branching stem with closed violet buds."""
    M = Model(); Hu = H * R
    Hs = 0.68 * Hu
    pts = stem(M, Hs, bend=(-0.08 * Hu, -0.06 * Hu), r0=2.0, r1=1.3)
    top = pts[-1]
    for a, h in [(0.9, 0.1), (2.6, 0.2), (4.4, 0.3)]:
        leaf(M, (top[0] * h, Hs * h, top[2] * h), a, L=0.30 * Hu, W=0.075 * Hu, rise=1.0, droop=1.3, mat='leaf')
    for frac, d in [(0.45, (0.16, 0.12, 0.05)), (0.6, (-0.12, 0.18, -0.08))]:
        b0 = pts[int(len(pts) * frac)]; tip = b0 + np.array(d) * Hu
        M.line(b0, tip, 'stem', r=0.9)
        for k in range(4):
            M.ball(tip + (0, 1.5 + k * 1.2, 0), 2.2 - k * 0.5, 'bud' if k < 3 else 'fan_3')
    c = top
    grad = lambda s: ['fan_1', 'fan_2', 'fan_3', 'fan_4', 'fan_5'][min(4, int(s * 5))]
    for n, (L, rise, off) in enumerate([(0.30 * Hu, 1.0, 0.0), (0.22 * Hu, 1.35, np.pi / 5)]):
        prof = (lambda L, rise: (lambda s: (L * (0.15 + 0.55 * np.sin(np.pi * 0.5 * s)) + L * 0.25 * s * s, L * rise * (0.75 * s - 0.15 * s * s))))(L, rise)
        W = (lambda L: (lambda s: 0.24 * L * np.sin(np.pi * min(0.15 + s, 1)) ** 0.9 * (1 - 0.6 * s) + 0.25))(L)
        for k in range(5):
            petal_strip(M, c, off + k * 2 * np.pi / 5, L, W, prof, 'fan_3', 'fan_2', grad=grad)
    for k in range(7):
        M.ball(c + (0, 1 + k * 1.3, 0), max(0.6, 2.6 - k * 0.35), 'fan_core' if k % 3 else 'fan_crys')
    for a in (0.0, 2.1, 4.2):
        M.line(c + (0, 3, 0), c + (2.5 * np.cos(a), 9, 2.5 * np.sin(a)), 'fan_crys')
    return M


GIANTS = {'tulip': giant_tulip, 'lily': giant_lily, 'daffodil': giant_daffodil, 'peony': giant_peony, 'starlotus': giant_starlotus}


# ------------------------------------------------------------------ medium flowers (height in blocks)

def med_lavender(H=2.5, seed=10):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    for k in range(7):
        a = rng.random() * 2 * np.pi; r = rng.random() * 3
        base = np.array([r * np.cos(a), 0, r * np.sin(a)]); h = Hu * (0.7 + 0.3 * rng.random())
        lean = np.array([rng.normal(0, 0.8), 0, rng.normal(0, 0.8)])
        M.line(base, base + lean + (0, h, 0), 'stem_s')
        for y in np.arange(h * 0.55, h, 0.8):
            p = base + lean * (y / h) + (0, y, 0)
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if rng.random() < 0.8:
                    M.put(p + (dx, 0, dz), 'lav' if rng.random() < 0.6 else 'lav_lt')
    for k in range(5):
        leaf(M, (0, 0, 0), rng.random() * 6.3, L=4, W=0.6, rise=0.8, droop=0.5, mat='leaf_dk')
    return M


def med_bells(H=2.0, seed=11):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    for k in range(3):
        a = k * 2.1 + rng.random(); d = np.array([np.cos(a), 0, np.sin(a)])
        top = d * 2.5 + (0, Hu, 0)
        M.line((0, 0, 0), top, 'stem_s'); M.line(top, top + d * 3 + (0, -1, 0), 'stem_s')
        for i in range(4):
            p = top + d * (0.8 * i) + (0, -0.4 * i - 1.6, 0)
            M.put(p, 'bell'); M.put(p + (0, -1, 0), 'bell_sh'); M.put(p + (1, -1, 0), 'bell'); M.put(p + (0, -1, 1), 'bell')
        leaf(M, (0, 0, 0), a + 0.8, L=Hu * 0.6, W=1.4, rise=1.4, droop=0.8)
    return M


def med_cluster(H=1.75, seed=12, pet='phl_p', ctr='phl_c', R0=4.5):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    for x in range(-int(R0), int(R0) + 1):
        for z in range(-int(R0), int(R0) + 1):
            for y in range(0, int(Hu)):
                e = (x * x + z * z) / R0 ** 2 + ((y - Hu * 0.35) / (Hu * 0.7)) ** 2
                if e < 1 and rng.random() < 0.85:
                    M.put((x, y, z), 'leaf' if rng.random() < 0.6 else 'leaf_dk')
    for k in range(int(R0 * 3)):
        a = rng.random() * 2 * np.pi; r = rng.random() * (R0 - 0.5)
        x, z = r * np.cos(a), r * np.sin(a)
        y = Hu * 0.35 + Hu * 0.7 * np.sqrt(max(0, 1 - (x * x + z * z) / R0 ** 2)) + 0.3
        p = np.array([x, y, z])
        for o in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1)):
            M.put(p + o, pet)
        M.put(p + (0, 1, 0), ctr)
    return M


def med_hydrangea(H=2.0, seed=13):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    for k in range(6):
        leaf(M, (0, 1, 0), k * 1.05, L=4.5, W=1.6, rise=0.5, droop=0.5, mat='leaf_dk')
    for k, (dx, dz, hh) in enumerate([(0, 0, 1.0), (2.6, 1.5, 0.8), (-2.2, 1.8, 0.75)]):
        c = np.array([dx, Hu * hh - 2.4, dz]); M.line((dx * 0.3, 0, dz * 0.3), c, 'stem_s')
        M.ball(c, 2.6, 'hyd_1'); M.ball(c + (0, 0.6, 0), 2.4, 'hyd_2', keep=lambda x, y, z: (x + y + z) % 2 == 0)
        M.ball(c + (0, 1.2, 0), 1.7, 'hyd_3', keep=lambda x, y, z: (x * 3 + z) % 4 == 0)
    return M


MEDIUM = {'lavender': med_lavender, 'bells': med_bells,
          'phlox': lambda H=1.75, seed=12: med_cluster(H, seed, 'phl_p', 'phl_c'),
          'yellowcluster': lambda H=1.75, seed=14: med_cluster(H, seed, 'yel_p', 'yel_c'),
          'hydrangea': med_hydrangea}


# ------------------------------------------------------------------ small flowers (cross-sprite equivalents)

SMALL_COLORS = ['sm_red', 'sm_yel', 'sm_wht', 'sm_pnk', 'sm_blu', 'sm_vio']


def small_flower(col='sm_red', seed=20, H=1.0):
    rng = np.random.default_rng(seed); M = Model(); h = int(round(H * R)) - 1
    M.line((0, 0, 0), (0, h, 0), 'stem_s')
    M.put((1, 1, 0), 'leaf'); M.put((-1, 2, 0), 'leaf')
    top = np.array([0, h, 0])
    for o in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1)):
        M.put(top + o, col)
    M.put(top, 'sm_ctr' if col != 'sm_yel' else 'sm_red'); M.put(top + (0, 1, 0), col)
    return M


# ------------------------------------------------------------------ grass, bushes, ground

def tuft(seed=30, tall=False):
    rng = np.random.default_rng(seed); M = Model()
    for k in range(rng.integers(3, 6)):
        a = rng.random() * 2 * np.pi; b = np.array([np.cos(a), 0, np.sin(a)]) * rng.random() * 1.5
        h = rng.integers(5, 8) if tall else rng.integers(2, 4)
        lean = np.array([rng.normal(0, 0.6), 0, rng.normal(0, 0.6)])
        M.line(b, b + lean + (0, h, 0), ['gr_1', 'gr_2', 'gr_3', 'gr_4'][k % 4])
    return M


def bush(seed=31, flowers=None, R0=4.0, H=1.4):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    for x in range(-int(R0), int(R0) + 1):
        for z in range(-int(R0), int(R0) + 1):
            for y in range(0, int(Hu) + 1):
                e = (x * x + z * z) / R0 ** 2 + ((y - Hu * 0.45) / (Hu * 0.6)) ** 2
                if e < 1 and rng.random() < 0.9:
                    M.put((x, y, z), ['leaf', 'leaf_dk', 'leaf_lt'][rng.integers(0, 3)])
    if flowers:
        pts = [p for p in M if rng.random() < 0.10 and p[1] > Hu * 0.4]
        for p in pts:
            M.put(p, flowers[rng.integers(0, len(flowers))])
    return M


def big_leaf_plant(seed=32):
    M = Model()
    for k in range(6):
        leaf(M, (0, 1, 0), k * 1.05 + 0.2, L=6.5, W=2.2, rise=0.9, droop=0.7, mat='bigleaf')
    return M


# ------------------------------------------------------------------ trees (height in blocks)

def tree(kind='heaven', H=8, seed=40):
    rng = np.random.default_rng(seed); M = Model(); Hu = H * R
    pal = {'heaven': ['tl_1', 'tl_2', 'tl_3'], 'blossom': ['bl_1', 'bl_2', 'bl_3', 'bl_1', 'tl_2'],
           'white': ['wt_1', 'wt_2', 'wt_3', 'wt_2']}[kind]
    th = Hu * 0.50
    lean = np.array([rng.normal(0, 1.5), 0, rng.normal(0, 1.5)])
    for i, t in enumerate(np.linspace(0, 1, int(th) + 1)):
        M.ball(lean * t + (0, th * t, 0), 2.4 - 0.9 * t, 'bark' if i % 5 else 'bark_dk')
    top = lean + (0, th, 0)
    blobs = [top + (0, Hu * 0.16, 0)]
    for k in range(4):
        a = k * np.pi / 2 + rng.random() * 0.8
        tip = top + np.array([np.cos(a) * Hu * 0.22, Hu * (0.04 + 0.08 * rng.random()), np.sin(a) * Hu * 0.22])
        M.line(top - (0, 3, 0), tip, 'bark', r=1.0)
        blobs.append(tip + (0, 2, 0))
    rr = Hu * 0.24
    for bi, b in enumerate(blobs):
        r = rr * (1.15 if bi == 0 else 0.85 + 0.25 * rng.random())
        ri = int(r) + 1
        for x in range(-ri, ri + 1):
            for y in range(-int(r * 0.85), int(r * 0.9) + 1):
                for z in range(-ri, ri + 1):
                    e = (x * x + z * z) / r ** 2 + (y / (r * 0.85)) ** 2
                    if e < 1 and (e < 0.55 or rng.random() < 0.72):
                        M.put(b + (x, y, z), pal[rng.integers(0, len(pal))], over=False)
    return M


# ------------------------------------------------------------------ hanging

def vine(L=10, seed=50, wis=False):
    rng = np.random.default_rng(seed); M = Model(); x = 0.0
    for y in range(0, -L, -1):
        if wis:
            frac = -y / L
            w = max(0, int(round(1.6 * (1 - frac))))
            col = ['wis_1', 'wis_2', 'wis_3'][min(2, int(frac * 3))]
            for dx in range(-w, w + 1):
                for dz in range(-w, w + 1):
                    if abs(dx) + abs(dz) <= w and rng.random() < 0.85:
                        M.put((x + dx, y, dz), col)
            M.put((x, y, 0), col)
        else:
            M.put((round(x), y, 0), 'vine')
            if y % 2 == 0:
                M.put((round(x) + (1 if (y // 2) % 2 else -1), y, 0), 'vine_lt')
            x += rng.normal(0, 0.25)
    return M


def petals_patch(seed=60, R0=6, dens=0.18):
    rng = np.random.default_rng(seed); M = Model()
    for x in range(-R0, R0 + 1):
        for z in range(-R0, R0 + 1):
            if x * x + z * z <= R0 * R0 and rng.random() < dens:
                M.put((x, 0, z), 'petal_g')
    return M


def player():
    M = Model()
    for y in range(0, 6):
        for x in (-1, 0):
            for z in (-1, 0, 1) if y >= 3 else (-1, 0):
                M.put((x, y, z), 'player')
    for y in (6, 7):
        for x in (-1, 0):
            for z in (-1, 0):
                M.put((x, y, z), 'player_hd')
    return M


def bbox(M):
    a = np.array(list(M.keys()))
    return a.min(0), a.max(0)

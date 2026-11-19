"""Block texture sets for the reference (vanilla) palette and the five heaven
concept palettes A-E.  Each new texture is a 16x16 pixel-art swatch built from
either a vanilla luminance grain (gradient-mapped) or a seeded procedural
pattern.  These are concept swatches, not final production art."""
import numpy as np
from PIL import Image

import os
TEX = os.environ.get('MC_TEX', '../../../../../MoreMinecrafts-Structure-Generator/vendor/mcbuild/src/mcbuild/assets/textures/block/')

PLAINS_GRASS = (0x91, 0xBD, 0x59)
PLAINS_FOLIAGE = (0x77, 0xAB, 0x2F)
PLAINS_WATER = (0x3F, 0x76, 0xE4)


def hx(s):
    s = s.lstrip('#'); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


def load(name):
    a = np.asarray(Image.open(TEX + name + '.png').convert('RGBA'))[:16, :16].astype(float)
    return a


def rgb(name):
    return load(name)[..., :3]


def tint(gray_rgba, col):
    a = gray_rgba.copy(); a[..., :3] = a[..., :3] * np.array(col, float) / 255.0; return a


def lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def gmap(src_lum, ramp, lo=None, hi=None, snap=True):
    """Gradient-map luminance onto a dark->light ramp of hex colours."""
    R = np.array([hx(c) for c in ramp])
    lo = src_lum.min() if lo is None else lo; hi = src_lum.max() if hi is None else hi
    t = np.clip((src_lum - lo) / max(hi - lo, 1e-6), 0, 1) * (len(R) - 1)
    if snap:
        return R[np.clip(np.round(t).astype(int), 0, len(R) - 1)]
    i = np.clip(np.floor(t).astype(int), 0, len(R) - 2); f = (t - i)[..., None]
    return R[i] * (1 - f) + R[i + 1] * f


def vnoise(seed, cells=4, octaves=2):
    rng = np.random.default_rng(seed)
    out = np.zeros((16, 16)); amp = 1.0; tot = 0
    for o in range(octaves):
        c = cells * (2 ** o)
        g = rng.random((c + 1, c + 1))
        g[-1, :] = g[0, :]; g[:, -1] = g[:, 0]  # tileable
        xs = np.linspace(0, c, 17)[:-1]
        i = np.floor(xs).astype(int); f = xs - i; f = f * f * (3 - 2 * f)
        a = g[i][:, i]; b = g[i][:, i + 1]; cc = g[i + 1][:, i]; d = g[i + 1][:, i + 1]
        fy = f[:, None]; fx = f[None, :]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (cc * (1 - fx) + d * fx) * fy)
        tot += amp; amp *= 0.5
    return out / tot


def pix(seed, field, ramp, jitter=0.18, snap=True):
    rng = np.random.default_rng(seed + 991)
    f = field + rng.normal(0, jitter, (16, 16))
    return gmap(f, ramp, snap=snap)


# ---------------------------------------------------------------- grass sides

def side_from_mask(soil, depth, grass_ramp, seed, edge_dark=True, sparkle=None):
    """Compose a grass side: soil texture plus a per-column fringe of `depth` px."""
    rng = np.random.default_rng(seed)
    out = soil.copy()
    R = [hx(c) for c in grass_ramp]
    for x in range(16):
        dpt = int(depth[x])
        for y in range(dpt):
            # lighter at the very top, darker toward the drip tip
            k = len(R) - 1 - min(len(R) - 1, int((y / max(dpt, 1)) * (len(R) - 1) + rng.random() * 0.9))
            out[y, x] = R[max(0, k)]
        if edge_dark and dpt < 16:
            out[dpt, x] = out[dpt, x] * 0.78
    if sparkle:
        for (x, y) in sparkle:
            out[y, x] = hx('#fff3b0')
    return out


def depth_profile(kind, seed):
    rng = np.random.default_rng(seed)
    if kind == 'overhang':        # B: thick rounded scallops 5-8 px
        xs = np.arange(16)
        return np.clip(np.round(6 + 1.6 * np.sin(xs * np.pi / 4 + 0.5) + rng.integers(0, 2, 16)), 5, 8)
    if kind == 'crisp':           # C: short straight band 3 px, rare 4
        return np.where(rng.random(16) < 0.2, 4, 3)
    if kind == 'roots':           # D: irregular drips 2-6
        return np.clip(np.round(3 + rng.normal(0, 1.3, 16)), 2, 6)
    if kind == 'soft':            # E: 4 px solid, dither added separately
        return np.where(rng.random(16) < 0.3, 5, 4)
    raise ValueError(kind)


def dither_fade(side, grass_ramp, start, seed):
    rng = np.random.default_rng(seed)
    c = hx(grass_ramp[1])
    out = side.copy()
    for y in range(start, start + 4):
        for x in range(16):
            dens = [0.6, 0.4, 0.22, 0.1][y - start]
            if ((x + y) % 2 == 0 and rng.random() < dens * 1.6) or rng.random() < dens * 0.25:
                out[y, x] = out[y, x] * 0.45 + c * 0.55
    return out


def roots(side, start_depth, seed, col='#6b4a2e'):
    rng = np.random.default_rng(seed)
    out = side.copy()
    for _ in range(4):
        x = int(rng.integers(0, 16)); y = int(start_depth[x])
        for k in range(int(rng.integers(2, 5))):
            if y + k < 16:
                out[y + k, x] = hx(col)
            if rng.random() < 0.35:
                x = (x + rng.choice([-1, 1])) % 16
    return out


# ---------------------------------------------------------------- rocks

def strata_side(ramp, seed):
    rng = np.random.default_rng(seed)
    rows = np.zeros(16); y = 0; band = 0
    while y < 16:
        h = int(rng.integers(2, 5)); rows[y:y + h] = [0.75, 0.45, 0.62, 0.3][band % 4]; y += h; band += 1
    field = np.repeat(rows[:, None], 16, 1)
    # wavy band boundaries
    for x in range(16):
        if rng.random() < 0.35:
            sh = rng.choice([-1, 1]); field[:, x] = np.roll(field[:, x], sh)
    field += vnoise(seed + 5, 4, 2) * 0.25
    return pix(seed, field, ramp, jitter=0.07)


def veins(base, seed, col, n=2):
    rng = np.random.default_rng(seed)
    out = base.copy()
    for _ in range(n):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        dx = rng.choice([-1, 1])
        for k in range(int(rng.integers(5, 9))):
            out[y % 16, x % 16] = hx(col)
            if rng.random() < 0.6: x += dx
            else: y += 1
    return out


def sparkle_top(base, seed, col='#fff1a8', n=3):
    rng = np.random.default_rng(seed); out = base.copy()
    for _ in range(n):
        out[int(rng.integers(0, 16)), int(rng.integers(0, 16))] = hx(col)
    return out


# ---------------------------------------------------------------- builders

def vanilla_grass(tint_col=PLAINS_GRASS):
    top = rgb('grass_block_top') * np.array(tint_col) / 255
    side = rgb('grass_block_side').copy()
    ov = load('grass_block_side_overlay'); m = ov[..., 3] > 0
    side[m] = ov[..., :3][m] * np.array(tint_col) / 255
    return dict(top=top, side=side, bottom=rgb('dirt'))


def cube(t):
    return dict(top=t, side=t, bottom=t)


def build():
    V = {}
    V['dirt'] = cube(rgb('dirt')); V['coarse_dirt'] = cube(rgb('coarse_dirt'))
    V['stone'] = cube(rgb('stone')); V['andesite'] = cube(rgb('andesite')); V['tuff'] = cube(rgb('tuff'))
    V['calcite'] = cube(rgb('calcite')); V['diorite'] = cube(rgb('diorite'))
    V['dripstone_block'] = cube(rgb('dripstone_block')); V['rooted_dirt'] = cube(rgb('rooted_dirt'))
    V['grass_block'] = vanilla_grass()
    V['oak_log'] = dict(top=rgb('oak_log_top'), side=rgb('oak_log'), bottom=rgb('oak_log_top'))
    N = {}
    gl = lum(rgb('grass_block_top')); dl = lum(rgb('dirt')); sl = lum(rgb('stone'))
    vov = load('grass_block_side_overlay')

    # ---------------- A: vanilla extension
    rampA = ['#2f7a22', '#3f9a2c', '#52b437', '#67c943', '#86da5a']
    topA = gmap(gl, rampA)
    sideA = rgb('dirt').copy(); m = vov[..., 3] > 0
    sideA[m] = gmap(lum(vov), rampA[1:], lo=lum(vov)[m].min(), hi=lum(vov)[m].max())[m]
    N['heaven_grass_A'] = dict(top=topA, side=sideA, bottom=rgb('dirt'))

    # ---------------- B: bright garden
    rampB = ['#6fb04e', '#86c862', '#9fd977', '#b6e68c', '#cdf0a6']
    topB = pix(11, vnoise(11, 4, 2), rampB, jitter=0.16)
    loamB = gmap(dl, ['#6e4b33', '#86603f', '#9c7550', '#b38b63', '#c5a07a'])
    sideB = side_from_mask(loamB, depth_profile('overhang', 12), rampB, 13)
    creamB = gmap(sl * 0.6 + vnoise(14, 3, 2) * 120, ['#bfae8e', '#d3c4a3', '#e2d6b8', '#ece3ca', '#f6efdc'])
    N['garden_grass_B'] = dict(top=topB, side=sideB, bottom=loamB)
    N['garden_loam_B'] = cube(loamB)
    N['cream_stone_B'] = cube(creamB)

    # ---------------- C: vivid fantasy
    rampC = ['#1f9a3c', '#29b446', '#3ccc4e', '#5ee05a', '#8ff06c']
    topC = sparkle_top(pix(21, vnoise(21, 4, 2), rampC, jitter=0.2), 22)
    soilC = gmap(dl, ['#4f3a4a', '#634a58', '#775c68', '#8c6f79', '#a0848a'])
    sideC = side_from_mask(soilC, depth_profile('crisp', 23), rampC, 24, sparkle=[(3, 0), (11, 1)])
    skyC = veins(pix(25, vnoise(25, 3, 2), ['#c9d3dc', '#d9e1e8', '#e6ecf1', '#f0f4f7', '#fafcfd'], jitter=0.12), 26, '#a9c7e6', n=2)
    N['radiant_grass_C'] = dict(top=topC, side=sideC, bottom=soilC)
    N['aether_soil_C'] = cube(soilC)
    N['skystone_C'] = cube(skyC)

    # ---------------- D: natural strata
    rampD1 = ['#1f5e2a', '#2b7533', '#378a3c', '#47a047', '#5db456']   # blue-green tuft
    rampD2 = ['#3a6a1f', '#4c8227', '#5e9a30', '#73ad3c', '#8cc24d']   # yellow-green tuft
    topD1 = gmap(gl, rampD1); topD2 = gmap(np.rot90(gl), rampD2)
    dd = depth_profile('roots', 31)
    sideD = roots(side_from_mask(rgb('dirt'), dd, rampD1, 32), dd, 33)
    strD_side = strata_side(['#8f8c86', '#a6a39c', '#bab7b0', '#cdcac3', '#dedbd5'], 34)
    strD_top = gmap(sl, ['#a6a39c', '#bab7b0', '#cdcac3', '#dedbd5'])
    N['lush_grass_D'] = dict(top=topD1, top2=topD2, side=sideD, bottom=rgb('dirt'))
    N['strata_stone_D'] = dict(top=strD_top, side=strD_side, bottom=strD_top)

    # ---------------- E: gentle heaven
    rampE = ['#7fae4c', '#93c25a', '#a7d06a', '#b9dc7c', '#cbe692']
    topE = pix(41, vnoise(41, 3, 2), rampE, jitter=0.10)
    loamE = gmap(dl, ['#7a5636', '#93693f', '#a97c4b', '#bd915c', '#cfa66f'])
    de = depth_profile('soft', 42)
    sideE = dither_fade(side_from_mask(loamE, de, rampE, 43, edge_dark=False), rampE, 4, 44)
    haloE = pix(45, vnoise(45, 3, 2), ['#d7ccb6', '#e2d9c6', '#ebe4d4', '#f2ede2', '#f8f5ee'], jitter=0.09)
    N['halo_grass_E'] = dict(top=topE, side=sideE, bottom=loamE)
    N['honey_loam_E'] = cube(loamE)
    N['halo_stone_E'] = cube(haloE)

    for k, v in N.items():
        if 'bottom' not in v: v['bottom'] = v['top']
    return V, N


# slot -> block mapping per palette (1:1 slot replacement; geometry untouched)
SLOTS = ['grass_block', 'dirt', 'coarse_dirt', 'stone', 'andesite', 'tuff']
PALETTES = {
    'REF': dict(grass_block='grass_block', dirt='dirt', coarse_dirt='coarse_dirt', stone='stone', andesite='andesite', tuff='tuff'),
    'A': dict(grass_block='heaven_grass_A', dirt='dirt', coarse_dirt='coarse_dirt', stone='stone', andesite='andesite', tuff='calcite'),
    'B': dict(grass_block='garden_grass_B', dirt='garden_loam_B', coarse_dirt='garden_loam_B', stone='cream_stone_B', andesite='calcite', tuff='dripstone_block'),
    'C': dict(grass_block='radiant_grass_C', dirt='aether_soil_C', coarse_dirt='aether_soil_C', stone='skystone_C', andesite='diorite', tuff='calcite'),
    'D': dict(grass_block='lush_grass_D', dirt='dirt', coarse_dirt='rooted_dirt', stone='strata_stone_D', andesite='stone', tuff='tuff'),
    'E': dict(grass_block='halo_grass_E', dirt='honey_loam_E', coarse_dirt='rooted_dirt', stone='halo_stone_E', andesite='calcite', tuff='halo_stone_E'),
}

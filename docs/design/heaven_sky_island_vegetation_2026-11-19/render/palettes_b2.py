"""Second-round palettes: original B plus refinements B1-B5.

Grass (garden_grass_B), loam (garden_loam_B) and the soil slot mapping are
taken unchanged from the original B in palettes.py.  Only the three rock slots
(stone / andesite / tuff) and the rock textures change."""
import numpy as np
from palettes import *          # noqa: F401,F403  original builders, unchanged
import palettes as _p


def soft_rock(seed, ramp, cells=3, octaves=2, grain=0.10, base=None, base_w=0.0):
    """Isotropic low-contrast rock: value-noise clumps + fine pixel grain.
    `base` (optional luminance array) keeps a vanilla grain relationship."""
    f = vnoise(seed, cells, octaves)
    if base is not None:
        b = (base - base.min()) / max(base.max() - base.min(), 1e-6)
        f = f * (1 - base_w) + b * base_w
    return pix(seed, f, ramp, jitter=grain)


def two_hue(seed, ramp_a, ramp_b, cells=3):
    """Mix two close ramps inside one texture (greige + beige) by a soft mask."""
    a = soft_rock(seed, ramp_a, grain=0.09); b = soft_rock(seed + 1, ramp_b, grain=0.09)
    m = vnoise(seed + 2, cells, 1) > 0.5
    out = np.where(m[..., None], a, b)
    return out


def glints(base, seed, col, n=3):
    rng = np.random.default_rng(seed); out = base.copy()
    for _ in range(n):
        y, x = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        out[y, x] = out[y, x] * 0.4 + hx(col) * 0.6
    return out


def build():
    V, N = _p.build()
    sl = lum(rgb('stone'))
    # B1 - uniform bright: B's own ramp/grain, contrast compressed to the 3 middle steps
    N['cream_stone_B1'] = cube(gmap(sl * 0.6 + vnoise(14, 3, 2) * 120, ['#d3c4a3', '#dccfb1', '#e2d6b8', '#e8ddc2', '#eee5cf']))
    # B2 - natural cream: greige+beige mixed inside each texture; two close rocks
    N['cream_stone_B2'] = cube(two_hue(51, ['#c4c0b6', '#cfcbc2', '#d9d6ce', '#e3e0d9', '#ecebe5'], ['#cebf9e', '#d8c9a8', '#e2d4b5', '#ebdfc4', '#f2e8d2'], cells=2))
    N['cream_stone_pale_B2'] = cube(two_hue(53, ['#cbc8c0', '#d6d3cc', '#e0ded8', '#e9e8e3', '#f2f1ee'], ['#d6c8a9', '#e0d2b4', '#e8dcc1', '#efe6cf', '#f5eedd'], cells=2))
    # B3 - neutral off-white (pairs with vanilla calcite)
    N['pale_stone_B3'] = cube(soft_rock(61, ['#bfbcb4', '#ccc9c1', '#d8d5ce', '#e3e0da', '#eeece7'], grain=0.16, base=sl, base_w=0.45))
    # B4 - graded strata by slot (no stripes): light / mid / deep warm, small dE steps
    N['cream_stone_B4'] = cube(soft_rock(71, ['#cdbe9e', '#d7c9aa', '#e0d3b6', '#e8dcc2', '#efe5cf'], cells=3, grain=0.16))
    N['cream_stone_light_B4'] = cube(soft_rock(72, ['#dbd1bb', '#e3dac6', '#ebe3d2', '#f2ebde', '#f8f3ea'], cells=4, grain=0.14))
    N['cream_stone_deep_B4'] = cube(soft_rock(73, ['#b9a785', '#c4b291', '#cebd9d', '#d8c8aa', '#e1d3b8'], cells=2, grain=0.18))
    # B5 - soft fantasy: warm pearl-white, broad soft clumps, faint pearl glints
    N['pearl_stone_B5'] = cube(glints(soft_rock(81, ['#ddcfb9', '#e6d9c6', '#eee3d3', '#f5ece0', '#faf4ec'], cells=2, octaves=2, grain=0.12), 82, '#fff8f0', n=4))
    return V, N


_B = _p.PALETTES['B']
def _rock(s, a, t):
    return dict(_B, stone=s, andesite=a, tuff=t)

PALETTES = {
    'B':  dict(_B),
    'B1': _rock('cream_stone_B1', 'cream_stone_B1', 'cream_stone_B1'),
    'B2': _rock('cream_stone_B2', 'cream_stone_pale_B2', 'cream_stone_pale_B2'),
    'B3': _rock('pale_stone_B3', 'calcite', 'pale_stone_B3'),
    'B4': _rock('cream_stone_B4', 'cream_stone_light_B4', 'cream_stone_deep_B4'),
    'B5': _rock('pearl_stone_B5', 'pearl_stone_B5', 'pearl_stone_B5'),
}

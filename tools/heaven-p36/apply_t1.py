"""Heaven P3.6: owner-approved T1 texture style (H2=T1, H3=all 48) applied to every heaven flora material.

The 10 giant-lily materials are exactly the approved P3.5 T1 proposal (tools/heaven-p35/flower_textures.py make(m, 1)).
The other 38 get the same T1 principles - 16x16, V5 mean colour kept (+-1%), restrained amplitude (about +-3-6%),
structure inside every 4x4 tile so a voxel face is no longer one flat colour - but a pattern chosen per material
character (petal grain, fan-petal diagonal grain, corona rings, floret bumps, pollen grain, crystal facets, grass
blades, vine twist, leaf veins, stem fibres). Writes the production textures; models, block IDs, NBT untouched.
Writes docs/design/heaven_sky_island_p3_6_2026-11-21/data/t1_stats.json and images/t1_sheet.png."""
import os, sys, json
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p35'))
import flower_textures as ft      # noqa: E402

ROOT = ft.ROOT; SRC = ft.SRC
DOC = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_6_2026-11-21')
LILY = set(ft.KIND)
KIND = {**{m: 'petal' for m in ('daf_back', 'daf_pet', 'daf_rim', 'sm_red', 'sm_blu', 'sm_pnk', 'sm_vio', 'sm_yel')},
        'sm_wht': 'pale_petal', 'fan_5': 'pale_petal', 'petal_g': 'ground_petal',
        **{m: 'fan' for m in ('fan_1', 'fan_2', 'fan_3', 'fan_4')}, 'daf_cor': 'corona',
        'sm_ctr': 'pollen', 'fan_core': 'pollen', 'fan_crys': 'crystal',
        **{m: 'floret' for m in ('hyd_1', 'hyd_2', 'hyd_3', 'lav', 'lav_lt', 'phl_p', 'phl_c', 'bud', 'wis_1', 'wis_2', 'wis_3')},
        **{m: 'blade' for m in ('gr_1', 'gr_2', 'gr_3', 'gr_4')}, 'leaf_dk': 'leaf', 'stem_s': 'stem', 'vine': 'vine', 'vine_lt': 'vine'}


def make(m):
    if m in LILY: return ft.make(m, 1)
    b = ft.base(m); k = KIND[m]; r = ft.rng(m, 61)
    xs, ys = np.meshgrid(np.arange(16), np.arange(16))
    n = ft.smooth_noise(r, 2) * 0.03
    if k == 'petal':        n += np.where(xs % 3 == 0, -0.035, 0.012)                       # vertical petal grain
    elif k == 'pale_petal': n = n * 0.6 + np.where(xs % 3 == 0, -0.022, 0.008)              # near-white: half amplitude, no greying
    elif k == 'ground_petal': n += (r.random((16, 16)) < 0.12) * 0.06 - 0.007               # soft scattered speckle
    elif k == 'fan':        n += np.where((xs + 2 * ys) % 5 == 0, -0.04, 0.012)             # fan petals: shallow diagonal grain
    elif k == 'corona':     n += np.where(ys % 4 == 0, -0.045, 0.015)                       # trumpet rings
    elif k == 'pollen':     n += (r.random((16, 16)) < 0.18) * 0.08 - 0.014                 # granular centre
    elif k == 'crystal':    n += np.where((xs - ys) % 6 == 0, 0.06, 0) - 0.01               # faint facet glints
    elif k == 'floret':                                                                     # small floret bumps
        n += np.where((xs % 2 == 0) & (ys % 2 == 0), 0.04, np.where((xs % 2 == 1) & (ys % 2 == 1), -0.03, 0))
    elif k == 'blade':      n += np.where(xs % 2 == 0, 0.03, -0.03) * (1 - ys / 30)         # grass blades, fading upward
    elif k == 'leaf':       n += np.where((xs + ys) % 5 == 0, 0.05, 0)                      # same vein rule as lily leaves
    elif k == 'stem':       n += np.where(xs % 3 == 0, -0.03, 0.01)
    elif k == 'vine':       n += np.where((xs + ys // 2) % 4 == 0, -0.04, 0.012)            # twisting strand
    img = np.repeat(np.repeat(b[None, None, :], 16, 0), 16, 1) * (1 + n)[..., None]
    return np.round(ft.keep_mean(img, b)).astype(np.uint8)


def main():
    mats = sorted(f[:-4] for f in os.listdir(SRC) if f.endswith('.png'))
    assert set(mats) == LILY | set(KIND), sorted(set(mats) ^ (LILY | set(KIND)))
    stats = {}; os.makedirs(f'{DOC}/images', exist_ok=True); os.makedirs(f'{DOC}/data', exist_ok=True)
    cols = 8; sheet = Image.new('RGB', (cols * 2 * 72 + 8, ((len(mats) + cols - 1) // cols) * 72 + 8), (40, 40, 48))
    for i, m in enumerate(mats):
        before = np.array(Image.open(f'{SRC}/{m}.png').convert('RGB'))
        a = make(m)
        stats[m] = dict(kind='lily_T1' if m in LILY else KIND[m], before=ft.stats(before), after=ft.stats(a))
        x, y = 8 + (i % cols) * 144, 8 + (i // cols) * 72
        sheet.paste(Image.fromarray(before).resize((64, 64), Image.NEAREST), (x, y))
        sheet.paste(Image.fromarray(a).resize((64, 64), Image.NEAREST), (x + 66, y))
        Image.fromarray(a).save(f'{SRC}/{m}.png', optimize=True)
    sheet.save(f'{DOC}/images/t1_sheet.png')
    json.dump(stats, open(f'{DOC}/data/t1_stats.json', 'w'), indent=1)
    for m, s in stats.items(): print(m, s['kind'], s['after']['colours_per_4x4_tile'], s['after']['std'])


if __name__ == '__main__':
    main()

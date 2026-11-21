"""Heaven P3.5-D: flower texture proposals T1 / T2 / T3 for the representative flower (giant lily).

Why the P2 flowers read as flat colour (measured, see the P3.5 report):
  * tools/heaven-prototype/build_prototype.py material_texture(): the V5 design colour times a 4x4 grid of
    4x4-pixel tiles with +-7% brightness jitter -> every 4x4 tile is ONE flat colour;
  * the P2 models are quarter-block voxels (4 model px) with no explicit uv, so each element face samples exactly
    the 4x4 texture region under it -> every voxel face shows exactly one tile = one flat colour.
So the texture, not the model or lighting, removes all in-voxel detail. These proposals keep the 16x16 size, the
material's mean colour (V5 palette), the models, block IDs and NBT; they only add pixel structure that survives the
4x4 sampling and continues across neighbouring voxels of the same block.

Output (isolated, NOT the production textures):
  src/gametest/resources/resourcepacks/heaven_tex_t{1,2,3}/   built-in packs of the gametest mod, off by default and
                                                             registered only under -Dmoreminecrafts.heaven.p35capture
  docs/design/heaven_sky_island_p3_5_2026-11-21/images/textures_sheet.png   design sheet (NOT the game renderer)
  docs/design/heaven_sky_island_p3_5_2026-11-21/data/texture_stats.json"""
import os, sys, json, colorsys
import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SRC = os.path.join(ROOT, 'src/main/resources/assets/moreminecrafts/textures/block/heaven/flora')
PACKS = os.path.join(ROOT, 'src/gametest/resources/resourcepacks')
DOC = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_5_2026-11-21')
sys.path.insert(0, os.path.join(ROOT, 'docs/design/heaven_sky_island_vegetation_2026-11-19/render'))
import veg_models as vm          # noqa: E402  (design palette only)

KIND = {'lil_out': 'petal', 'lil_in': 'petal', 'lil_spot': 'petal', 'leaf': 'leaf', 'leaf_lt': 'leaf', 'vein': 'leaf',
        'stem': 'stem', 'stem_dk': 'stem', 'fil': 'filament', 'anther': 'anther'}


def base(m): return np.array(vm.MAT_RGB[vm.MAT_ID[m]], float)


def rng(m, t): return np.random.default_rng(sum(map(ord, m)) * 7919 + t)


def smooth_noise(r, scale=2):
    g = r.random((16 // scale + 2, 16 // scale + 2))
    from scipy import ndimage
    up = ndimage.zoom(g, scale, order=1)[:16, :16]
    return (up - up.mean()) / (up.std() + 1e-6)


def shade(rgb, f):
    """multiply value; f>1 lightens, f<1 darkens."""
    return np.clip(rgb * f, 0, 255)


def tint(rgb, target, a):
    return np.clip(rgb * (1 - a) + np.array(target, float) * a, 0, 255)


def keep_mean(img, b):
    """rescale so the mean colour stays the V5 material colour (+-1%)."""
    m = img.reshape(-1, 3).mean(0)
    return np.clip(img * (b / np.maximum(m, 1))[None, None, :], 0, 255)


def make(m, t):
    b = base(m); k = KIND[m]; r = rng(m, t)
    img = np.repeat(np.repeat(b[None, None, :], 16, 0), 16, 1).astype(float)
    xs, ys = np.meshgrid(np.arange(16), np.arange(16))           # img[y, x]
    if t == 1:                                                  # T1: restrained pixel texture
        n = smooth_noise(r, 2) * 0.035
        if k in ('petal', 'stem', 'filament'):
            n += np.where(xs % 3 == 0, -0.03, 0.01)             # faint vertical grain
        elif k == 'leaf':
            n += np.where((xs + ys) % 5 == 0, 0.05, 0)          # faint diagonal veins
        elif k == 'anther':
            n += (r.random((16, 16)) < 0.10) * 0.10
        img = img * (1 + n)[..., None]
    elif t == 2:                                                # T2: rich natural texture
        n = smooth_noise(r, 2) * 0.05
        if k == 'petal':
            wob = (r.random(16) < 0.35).astype(int)
            lines = ((xs + wob[ys]) % 4 == 0)
            n += np.where(lines, -0.13, 0.04)                   # petal grain lines
            n += np.where((ys % 8 == 0) & ~lines, 0.06, 0)      # soft cross ridges
        elif k == 'stem':
            n += np.where(xs % 4 == 1, 0.10, np.where(xs % 4 == 3, -0.10, 0))  # fibres
        elif k == 'leaf':
            n += np.where((xs + ys) % 4 == 0, 0.14, 0) + np.where((xs - ys) % 8 == 0, -0.06, 0)  # veins
        elif k == 'filament':
            n += np.where(xs % 2 == 0, 0.05, -0.04)
        elif k == 'anther':
            pol = r.random((16, 16)) < 0.22
            img = np.where(pol[..., None], tint(img, (232, 176, 62), 0.55), img)
        img = img * (1 + n)[..., None]
    else:                                                       # T3: fantastic heaven garden
        n = smooth_noise(r, 2) * 0.05
        grad = (7.5 - ys) / 7.5 * 0.10                          # light from above inside every block
        if k == 'petal':
            lines = (xs % 4 == 0)
            n += np.where(lines, -0.12, 0.05) + grad
            img = img * (1 + n)[..., None]
            spark = (r.random((16, 16)) < 0.03) & ~lines
            img = np.where(spark[..., None], tint(img, (255, 246, 214), 0.75), img)
            img = np.where(((ys % 4) == 0)[..., None] & ~lines[..., None], tint(img, (255, 236, 190), 0.18), img)
        elif k == 'leaf':
            n += np.where((xs + ys) % 4 == 0, 0.20, 0) + grad * 0.6
            img = img * (1 + n)[..., None]
            img = np.where(((xs + ys) % 4 == 0)[..., None], tint(img, (200, 255, 190), 0.25), img)
        elif k == 'stem':
            n += np.where(xs % 4 == 1, 0.16, np.where(xs % 4 == 3, -0.10, 0)) + grad * 0.5
            img = img * (1 + n)[..., None]
        elif k == 'filament':
            n += np.where(xs % 2 == 0, 0.08, -0.04) + grad
            img = img * (1 + n)[..., None]
            img = tint(img, (240, 250, 230), 0.15)
        elif k == 'anther':
            img = img * (1 + n)[..., None]
            pol = r.random((16, 16)) < 0.28
            img = np.where(pol[..., None], tint(img, (255, 214, 92), 0.75), img)
    img = keep_mean(img, b)
    return np.round(img).astype(np.uint8)


def stats(a):
    rgb = a.reshape(-1, 3).astype(float)
    tiles = [a[y:y + 4, x:x + 4].reshape(-1, 3) for y in range(0, 16, 4) for x in range(0, 16, 4)]
    in_tile = float(np.mean([len({tuple(p) for p in t}) for t in tiles]))
    return dict(colours=len({tuple(p) for p in rgb.astype(int)}), mean=[round(float(v), 1) for v in rgb.mean(0)],
                std=round(float(rgb.std(0).mean()), 1), colours_per_4x4_tile=round(in_tile, 2))


def main():
    out = {}
    sheet = Image.new('RGB', (4 * (16 * 8 + 8) + 8, len(KIND) * (16 * 8 + 8) + 8), (40, 40, 48))
    for row, m in enumerate(KIND):
        orig = np.array(Image.open(f'{SRC}/{m}.png').convert('RGB'))
        out[m] = {'P3': stats(orig), 'v5_colour': vm.MAT[m]}
        imgs = [orig]
        for t in (1, 2, 3):
            a = make(m, t); imgs.append(a); out[m][f'T{t}'] = stats(a)
            d = f'{PACKS}/heaven_tex_t{t}/assets/moreminecrafts/textures/block/heaven/flora'
            os.makedirs(d, exist_ok=True); Image.fromarray(a).save(f'{d}/{m}.png', optimize=True)
        for col, a in enumerate(imgs):
            sheet.paste(Image.fromarray(a).resize((128, 128), Image.NEAREST), (8 + col * 136, 8 + row * 136))
    for t in (1, 2, 3):
        with open(f'{PACKS}/heaven_tex_t{t}/pack.mcmeta', 'w') as f:
            json.dump({'pack': {'description': f'Heaven P3.5 flower texture proposal T{t} (gametest only, not production)',
                                'min_format': [97, 1], 'max_format': [97, 1]}}, f)
    os.makedirs(f'{DOC}/images', exist_ok=True); os.makedirs(f'{DOC}/data', exist_ok=True)
    sheet.save(f'{DOC}/images/textures_sheet.png')
    json.dump(out, open(f'{DOC}/data/texture_stats.json', 'w'), indent=1)
    for m, v in out.items():
        print(m, {k: (x['colours'], x['std'], x['colours_per_4x4_tile']) for k, x in v.items() if k != 'v5_colour'})


if __name__ == '__main__':
    main()

"""P1-A spec inputs measured from the V5 design source:
  * per-species geometry if each species were baked to custom block models at the design resolution
  * natural-block texture sheet and A-grass side before/after comparison
Outputs into docs/design/heaven_sky_island_p1_prototype_2026-11-20/{data,images}."""
import os, sys, json, collections
import numpy as np
from PIL import Image, ImageDraw, ImageFont
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import build_prototype as bp          # sets up design imports
import veg_models as vm, veg_layout as vl, palettes_veg as pv
OUT = os.path.join(bp.ROOT, 'docs/design/heaven_sky_island_p1_prototype_2026-11-20')
F = lambda s, b=False: ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf' % ('-Bold' if b else ''), s)
R = vm.R


def bake(M):
    """cells, unique cells, elements (greedy per cell), materials for one model."""
    cells = collections.defaultdict(dict)
    for (x, y, z), m in M.items():
        cells[((x + 2) // R, y // R, (z + 2) // R)][((x + 2) % R, y % R, (z + 2) % R)] = vm.MAT_ID[m]
    uniq = set(); elems = 0
    for k, vox in cells.items():
        a = np.zeros((4, 4, 4), np.uint8)
        for (x, y, z), m in vox.items(): a[x, y, z] = m
        uniq.add(a.tobytes()); elems += len(bp.boxes(a))
    lo, hi = vm.bbox(M)
    return dict(block_cells=len(cells), unique_cells=len(uniq), elements=elems, materials=len(set(M.values())),
                height_blocks=round((hi[1] + 1) / R, 2), vertical_extent_blocks=round((hi[1] - lo[1] + 1) / R, 2), footprint_blocks=[round((hi[0] - lo[0] + 1) / R, 2), round((hi[2] - lo[2] + 1) / R, 2)])


def species_metrics():
    L = vl.build('V5'); inst = collections.Counter((i['cat'], i['species']) for i in L.instances if 'count' not in i)
    out = {}
    reps = {('giant', 'starlotus'): vm.giant_starlotus(10, seed=vl.dh('V5', 'starlotus') % 997),
            ('giant', 'lily'): vm.giant_lily(10, seed=vl.dh('V5', 'lily') % 997),
            ('giant', 'daffodil'): vm.giant_daffodil(10, seed=vl.dh('V5', 'daffodil') % 997),
            ('tree', 'blossom'): vm.tree('blossom', 8, 1), ('tree', 'white'): vm.tree('white', 9, 1),
            ('medium', 'hydrangea'): vm.med_hydrangea(), ('medium', 'lavender'): vm.med_lavender(), ('medium', 'phlox'): vm.MEDIUM['phlox'](),
            ('small', 'small_red'): vm.small_flower('sm_red'), ('grass', 'tuft_short'): vm.tuft(1), ('grass', 'tuft_tall'): vm.tuft(2, True),
            ('ground', 'petals'): vm.petals_patch(1, 6, 0.14), ('vine', 'vine'): vm.vine(10, 1), ('vine', 'wisteria'): vm.vine(10, 1, wis=True)}
    for (cat, sp), M in reps.items():
        out[f'{cat}:{sp}'] = dict(instances_in_V5=inst.get((cat, sp), 0), **bake(M))
    out['vine:wisteria_canopy'] = dict(instances_in_V5=[i['count'] for i in L.instances if i.get('species') == 'wisteria_canopy'][0], note='same raceme model as wisteria, hung from tree canopies')
    out['small:all_6_colours'] = dict(instances_in_V5=sum(v for (c, s), v in inst.items() if c == 'small'), note='one model per colour')
    return out


def textures():
    V, N = pv.build()
    rows = [('heaven_grass_block top', N['heaven_grass_AL']['top']), ('heaven_grass_block side (NEW)', N['heaven_grass_AL']['side']),
            ('A grass side (round-1 original)', N['heaven_grass_A']['side']), ('heaven_loam (B loam)', N['garden_loam_B']['side']),
            ('heaven_cream_stone (B2)', N['cream_stone_B2']['side']), ('heaven_cream_stone_pale (B2)', N['cream_stone_pale_B2']['side'])]
    S = 160; im = Image.new('RGB', (len(rows) * (S + 24) + 24, S + 70), (246, 247, 249)); d = ImageDraw.Draw(im)
    for i, (lab, t) in enumerate(rows):
        x = 24 + i * (S + 24)
        im.paste(Image.fromarray(np.clip(np.round(t), 0, 255).astype(np.uint8)).resize((S, S), Image.NEAREST), (x, 34))
        d.text((x, 10), lab, font=F(12, True), fill=(30, 30, 40))
    d.text((24, S + 44), 'Exported 16x16 game textures (shown x10). Source: palettes_veg.py / palettes_b2.py (design rounds).', font=F(12), fill=(70, 70, 80))
    im.save(f'{OUT}/images/natural_textures.png')
    # before/after seam strip: grass side over two loam blocks
    a = N['heaven_grass_A']['side']; n = N['heaven_grass_AL']['side']; loam = N['garden_loam_B']['side']
    def col(side): return np.concatenate([side, loam, loam], 0)
    strip = np.concatenate([col(a), np.full((48, 4, 3), 246.0), col(n)], 1)
    big = Image.fromarray(np.clip(np.round(strip), 0, 255).astype(np.uint8)).resize((strip.shape[1] * 10, strip.shape[0] * 10), Image.NEAREST)
    canvas = Image.new('RGB', (big.width + 40, big.height + 70), (255, 255, 255)); canvas.paste(big, (20, 40)); d = ImageDraw.Draw(canvas)
    d.text((20, 10), 'BEFORE: A side on vanilla dirt', font=F(13, True), fill=(30, 30, 30)); d.text((20 + 200, 10), 'AFTER: A fringe on B loam', font=F(13, True), fill=(30, 30, 30))
    soil_a = np.abs(a[4:] - loam[4:]).mean(); soil_n = np.abs(n[4:] - loam[4:]).mean()
    d.text((20, big.height + 46), f'grass-side soil rows vs loam block: mean |dRGB| before {soil_a:.1f}, after {soil_n:.1f}; fringe pixels identical: {bool((a[:4][n[:4] != loam[:4]] == n[:4][n[:4] != loam[:4]]).all())}', font=F(12), fill=(60, 60, 70))
    canvas.save(f'{OUT}/images/grass_side_before_after.png')
    return dict(soil_rows_mean_abs_diff_before=round(float(soil_a), 2), soil_rows_mean_abs_diff_after=round(float(soil_n), 2))


def flora_sheet():
    mats = sorted({vm.MAT_NAMES[i - 1] for i in np.unique(vl.build('V5').fine) if i})
    cols = 12; S = 64; rows = (len(mats) + cols - 1) // cols
    im = Image.new('RGB', (cols * (S + 50) + 20, rows * (S + 30) + 40), (246, 247, 249)); d = ImageDraw.Draw(im)
    for i, m in enumerate(mats):
        x = 10 + (i % cols) * (S + 50); y = 30 + (i // cols) * (S + 30)
        im.paste(Image.fromarray(np.clip(np.round(bp.material_texture(m)), 0, 255).astype(np.uint8)).resize((S, S), Image.NEAREST), (x, y))
        d.text((x, y + S + 2), m, font=F(10), fill=(40, 40, 50))
    d.text((10, 8), f'{len(mats)} flora material textures (16x16, design colour x per-quarter-block jitter 0.93-1.07)', font=F(13, True), fill=(20, 20, 30))
    im.save(f'{OUT}/images/flora_material_textures.png')


if __name__ == '__main__':
    os.makedirs(f'{OUT}/images', exist_ok=True); os.makedirs(f'{OUT}/data', exist_ok=True)
    res = dict(species=species_metrics(), textures=textures())
    flora_sheet()
    json.dump(res, open(f'{OUT}/data/spec_metrics.json', 'w'), indent=1)
    print(json.dumps(res, indent=1))

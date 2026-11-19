"""Produce every image and data file of the vegetation round.
usage: python3 make_all.py OUTDIR [--jobs N]"""
import sys, os, json, time, collections
import numpy as np
from multiprocessing import Pool
from PIL import Image, ImageDraw, ImageFont
import veg_models as vm, veg_layout as vl, veg_render as vr, showcase as sc, resources as rs

OUT = sys.argv[1]; JOBS = int(sys.argv[sys.argv.index('--jobs') + 1]) if '--jobs' in sys.argv else 4
os.makedirs(OUT, exist_ok=True)
F = lambda s, b=False: ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf' % ('-Bold' if b else ''), s)
VARIANTS = ['V1', 'V2', 'V3', 'V4', 'V5']
TITLE = {'V1': 'V1 - Bright heaven garden (balanced)', 'V2': 'V2 - Giant-flower garden', 'V3': 'V3 - Abundant flower field',
         'V4': 'V4 - Natural ecosystem', 'V5': 'V5 - Fantastic heaven garden'}
TAG_NBT = 'Terrain: NBT-BASED (meadow/plain_medium_d, fixed A-grass/B-loam/B2-rock) | Vegetation: DESIGN PROTOTYPE voxels (1/4 block), not in-game'
TAG_SHOW = 'DESIGN PROTOTYPE models on a showcase platform (not NBT terrain, not in-game)'


def stamp(im, title, tag):
    d = ImageDraw.Draw(im); d.rectangle((0, 0, im.width, 40), fill=(20, 28, 40))
    d.text((12, 8), title, font=F(20, True), fill=(255, 255, 255))
    d.rectangle((0, im.height - 22, im.width, im.height), fill=(235, 240, 248))
    d.text((10, im.height - 19), tag, font=F(12), fill=(40, 50, 70))
    return im


# ------------------------------------------------------------------ variant renders
def hero_and_field(L):
    g = [i for i in L.instances if i['cat'] == 'giant'][0]
    pts = collections.Counter()
    for i in L.instances:
        if i.get('cat') in ('small', 'medium'):
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    pts[(i['x'] + dx, i['z'] + dz)] += 1
    giants = [(i['x'], i['z']) for i in L.instances if i['cat'] in ('giant', 'tree')]
    cand = [(c, n) for c, n in pts.items() if L.s.grass[min(max(c[0], 0), 44), min(max(c[1], 0), 36)] and all(np.hypot(c[0] - a, c[1] - b) > 4 for a, b in giants)]
    fx, fz = max(cand, key=lambda t: (t[1], -t[0][0]))[0]
    return g, (fx, fz)


def render_variant(v):
    t0 = time.time(); s = vl.Surface(); L = vl.build(v, s)
    S = vr.Scene(s.terrain, L.fine, (0, vl.YLO, 0))
    out = {}
    im = vr.render(S, 'full'); stamp(im, TITLE[v] + '  [full - same camera as natural-block rounds]', TAG_NBT).save(f'{OUT}/{v}_full.png', optimize=True)
    g, (fx, fz) = hero_and_field(L)
    im = vr.render(S, None, W=1000, H=800, yaw=35, pitch=8, center=(g['x'] + 0.5, g['y'] + 5.0, g['z'] + 0.5), scale=0.017)
    stamp(im, f"{TITLE[v]}  [giant close-up: {g['species']} H{g['H']}]", TAG_NBT).save(f'{OUT}/{v}_giant.png', optimize=True)
    fy = s.top[fx, fz] + 1
    im = vr.render(S, None, W=1000, H=800, yaw=35, pitch=26, center=(fx + 0.5, fy + 0.8, fz + 0.5), scale=0.0115)
    stamp(im, f'{TITLE[v]}  [flower field close-up @ {fx},{fz}]', TAG_NBT).save(f'{OUT}/{v}_field.png', optimize=True)
    chk = vl.checks(L)
    chk['hero_giant'] = g['species']; chk['field_center'] = [fx, fz]
    chk['render_seconds'] = round(time.time() - t0, 1)
    chk['fine_grid_crc32'] = hash_fine(L.fine)
    return v, chk


def hash_fine(a):
    return __import__('zlib').crc32(a.tobytes())


# ------------------------------------------------------------------ showcase sheets
def cells_faces(M, c):
    cells = collections.Counter(((x + 2) // c, y // c, (z + 2) // c) for (x, y, z) in M)
    filled = {k for k, n in cells.items() if n >= 0.25 * c ** 3}
    f = 0
    for (x, y, z) in filled:
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            if (x + d[0], y + d[1], z + d[2]) not in filled:
                f += 1
    return len(filled), f


def giant_detail():
    rows = []; stats = {}
    for k in ['tulip', 'lily', 'daffodil', 'peony', 'starlotus']:
        M = vm.GIANTS[k](10)
        tiles = []
        for lab, model, yaw in (('front 35deg', M, 35), ('back 215deg', M, 215),
                                ('half-block (1/2) elements', sc.downsample(M, 2)[0], 35),
                                ('full-block multiblock', sc.downsample(M, 4)[0], 35)):
            S = sc.platform_scene([(model, 4)], 9, 9)
            im = vr.render(S, None, W=420, H=560, yaw=yaw, pitch=12, center=(4.5, 8.0, 4.5), scale=0.03)
            d = ImageDraw.Draw(im); d.text((8, 6), lab, font=F(15, True), fill=(20, 20, 40)); tiles.append(im)
        f_fine = rs.exposed_faces(M); n2, f2 = cells_faces(M, 2); n4, f4 = cells_faces(M, 4)
        mats = collections.Counter(M.values())
        stats[k] = dict(voxels_quarter=len(M), exposed_faces_quarter=f_fine, half_cells=n2, exposed_faces_half=f2,
                        full_blocks=n4, exposed_faces_full=f4, materials=len(mats))
        row = Image.new('RGB', (4 * 420 + 300, 560), (248, 248, 250))
        for i, t in enumerate(tiles): row.paste(t, (300 + i * 420, 0))
        d = ImageDraw.Draw(row); d.text((12, 14), k.upper(), font=F(26, True), fill=(30, 30, 30))
        y = 60
        for line in [f'height 10 blocks', f'quarter voxels {len(M)}', f'faces 1/4: {f_fine}', f'faces 1/2: {f2}', f'full blocks: {n4}', f'faces 1/1: {f4}', f'materials: {len(mats)}']:
            d.text((14, y), line, font=F(16), fill=(50, 50, 60)); y += 28
        rows.append(row)
    sheet = Image.new('RGB', (rows[0].width, 60 + 560 * len(rows) + 26), (255, 255, 255))
    d = ImageDraw.Draw(sheet); d.text((12, 14), 'Giant flower forms (H=10) - detail and multiblock resolution comparison', font=F(26, True), fill=(10, 10, 10))
    for i, r in enumerate(rows): sheet.paste(r, (0, 60 + i * 560))
    d.text((10, sheet.height - 22), TAG_SHOW, font=F(13), fill=(60, 60, 60))
    sheet.save(f'{OUT}/giant_detail.png', optimize=True)
    return stats


def catalog():
    rows = []
    def lineup(items, W, title, ruler_h, cam_h, scale, yaw=20):
        models = []; x = 3
        for M, w, lab in items:
            models.append((M, x + w // 2)); x += w
        models += [(sc.ruler(ruler_h), x + 1), (vm.player(), x + 3)]
        S = sc.platform_scene(models, x + 5, 7)
        im = vr.render(S, None, W=W, H=int(W * 0.30), yaw=yaw, pitch=10, center=((x + 5) / 2, cam_h, 3.5), scale=scale)
        d = ImageDraw.Draw(im); d.text((10, 6), title, font=F(18, True), fill=(20, 20, 40))
        return im
    G = [(vm.GIANTS[k](10), 8, k) for k in ['tulip', 'lily', 'daffodil', 'peony', 'starlotus']]
    rows.append(lineup(G, 1800, 'Giant flowers (H=10) | ruler 10 blocks | player ~2 blocks', 10, 8.0, 0.031))
    Mm = [(vm.MEDIUM[k](), 3, k) for k in ['lavender', 'bells', 'phlox', 'yellowcluster', 'hydrangea']] + \
         [(vm.tree('heaven', 8), 7, 'heaven tree'), (vm.tree('blossom', 8), 7, 'blossom tree'), (vm.tree('white', 9), 7, 'white tree')]
    rows.append(lineup(Mm, 1800, 'Medium flowers (H 1.75-2.5) and trees (H 8-9) | ruler 9 blocks', 9, 7.0, 0.031))
    Sm = [(vm.small_flower(c, i), 2, c) for i, c in enumerate(vm.SMALL_COLORS)] + [(vm.tuft(1), 2, 'tuft'), (vm.tuft(2, True), 2, 'tall tuft'),
          (vm.bush(3), 3, 'leaf bush'), (vm.bush(4, ['sm_pnk', 'sm_wht']), 3, 'flower bush'), (vm.big_leaf_plant(), 4, 'big leaf'), (vm.petals_patch(5, 5, 0.25), 3, 'petals')]
    rows.append(lineup(Sm, 1800, 'Small flowers (H~1, cross-sprite equivalents), grass, bushes, ground decoration | ruler 3 blocks', 3, 2.6, 0.0125, yaw=25))
    sheet = Image.new('RGB', (1800, sum(r.height for r in rows) + 30), (255, 255, 255)); y = 0
    for r in rows: sheet.paste(r, (0, y)); y += r.height
    ImageDraw.Draw(sheet).text((10, sheet.height - 22), TAG_SHOW, font=F(13), fill=(60, 60, 60))
    sheet.save(f'{OUT}/model_catalog.png', optimize=True)


def height_study():
    s = vl.Surface(); tiles = []; data = {}
    for lab, gH, tH, mH in (('S: giant 7 / tree 6 / medium 1.5', 7, 6, 1.5), ('M: giant 10 / tree 8 / medium 2.5', 10, 8, 2.5), ('L: giant 13 / tree 10 / medium 4', 13, 10, 4.0)):
        L = vl.Layout(s, 'HS')
        L.place('giant', 'tulip', vm.giant_tulip(gH), 18, 18, H=gH); L.place('giant', 'peony', vm.giant_peony(gH), 28, 26, H=gH)
        L.place('tree', 'heaven', vm.tree('heaven', tH, 7), 25, 9, H=tH)
        for i, (x, z) in enumerate([(21, 21), (31, 22), (14, 24), (23, 13), (33, 15)]):
            L.place('medium', 'lavender', vm.med_lavender(mH, i), x, z, H=mH)
        L.place('ground', 'player', vm.player(), 30, 30, H=2)
        S = vr.Scene(s.terrain, L.fine, (0, vl.YLO, 0))
        im = vr.render(S, None, W=800, H=620, yaw=35, pitch=14, center=(22.5, 14.5, 18.5), scale=0.062)
        d = ImageDraw.Draw(im); d.text((10, 8), lab, font=F(18, True), fill=(20, 20, 40)); tiles.append(im)
        data[lab] = dict(giant_to_island_width=round(gH / 45, 2), giant_over_surface_top=gH, giant_to_tree=round(gH / tH, 2), giant_to_player=round(gH / 1.8, 1),
                         medium_to_player=round(mH / 1.8, 2), clipped=L.clipped)
    sheet = Image.new('RGB', (2400, 620 + 50), (255, 255, 255))
    for i, t in enumerate(tiles): sheet.paste(t, (i * 800, 0))
    ImageDraw.Draw(sheet).text((10, 630), 'Height study on the fixed island (same camera). Player figure at front right. Terrain NBT-BASED, vegetation DESIGN PROTOTYPE.', font=F(16), fill=(30, 30, 30))
    sheet.save(f'{OUT}/height_study.png', optimize=True)
    return data


def thumb(M, w=150, h=190, cam_h=None, scale=None):
    lo, hi = vm.bbox(M); Hb = (hi[1] + 1) / 4
    S = sc.platform_scene([(M, 3)], 7, 7)
    sc_ = scale or max(0.012, (Hb + 2.0) / h * 1.05)
    return vr.render(S, None, W=w, H=h, yaw=30, pitch=14, center=(3.5, 3 + Hb * 0.5, 3.5), scale=sc_)


def species_model(key):
    cat, sp = key.split(':')
    if cat == 'giant': return vm.GIANTS[sp](10)
    if cat == 'medium': return vm.MEDIUM[sp]()
    if cat == 'small': return vm.small_flower('sm_' + sp[6:], 1)
    if cat == 'tree': return vm.tree(sp, 8)
    if cat == 'grass': return vm.tuft(1, sp == 'tuft_tall')
    if cat == 'bush': return vm.bush(3, ['sm_pnk', 'sm_wht'] if sp == 'flowerbush' else None)
    if cat == 'ground': return vm.big_leaf_plant() if sp == 'bigleaf' else vm.petals_patch(5, 5, 0.25)
    if cat == 'vine': return vm.vine(10, 1, wis=sp.startswith('wisteria'))
    return None


H_OF = {'giant': '10', 'medium': None, 'small': '~1', 'grass': '0.75 / 1.75', 'bush': '~1.4', 'ground': '<1', 'vine': '1.5-4 (hanging)'}


def palette_sheet(v, chk, cost_cat, cost_tot, thumbs):
    keys = [k for k in chk['counts'] if chk['counts'][k] > 0]
    keys.sort(key=lambda k: ['giant', 'tree', 'medium', 'bush', 'small', 'grass', 'ground', 'vine'].index(k.split(':')[0]))
    cols = 8; tw, th = 150, 236
    rows_n = (len(keys) + cols - 1) // cols
    im = Image.new('RGB', (cols * tw + 20, 70 + rows_n * th + 120), (247, 248, 250)); d = ImageDraw.Draw(im)
    d.text((10, 10), f'{TITLE[v]} - vegetation palette and sizes', font=F(20, True), fill=(20, 20, 30))
    d.text((10, 40), 'counts = placed instances in this layout; heights in blocks; thumbnails are prototype models', font=F(12), fill=(80, 80, 90))
    for i, k in enumerate(keys):
        x = 10 + (i % cols) * tw; y = 70 + (i // cols) * th
        if k in thumbs: im.paste(thumbs[k], (x, y))
        cat, sp = k.split(':')
        hh = H_OF.get(cat) or (str(vl.MED_H.get(sp, '')) if cat == 'medium' else '')
        if cat == 'tree': hh = '7-9'
        d.text((x + 2, y + 192), sp[:18], font=F(12, True), fill=(30, 30, 40))
        d.text((x + 2, y + 208), f'{cat} | n={chk["counts"][k]} | H {hh}', font=F(11), fill=(70, 70, 80))
    y = 70 + rows_n * th + 8
    d.text((10, y), 'Estimated new resources (blocks / textures / models / NBT structures):', font=F(14, True), fill=(20, 20, 30))
    line = '   '.join(f'{c}: {"/".join(map(str, cost_cat[c]))}' for c in cost_cat)
    d.text((10, y + 24), line, font=F(12), fill=(50, 50, 60))
    d.text((10, y + 48), f'TOTAL  blocks {cost_tot[0]}  |  textures {cost_tot[1]}  |  models {cost_tot[2]}  |  NBT structures {cost_tot[3]}   (estimate)', font=F(15, True), fill=(150, 60, 10))
    d.text((10, y + 76), f'walkable {chk["walkable_share"]:.0%} (strict {chk["strict_walkable_share"]:.0%}) | pond low-cover {chk["pond_columns_covered_low"]} | clipped voxels {chk["clipped_voxels_outside_template"]}', font=F(12), fill=(60, 60, 70))
    im.save(f'{OUT}/{v}_palette.png', optimize=True)


def board():
    cells = []
    for v in VARIANTS:
        a = Image.open(f'{OUT}/{v}_full.png').crop((300, 60, 1300, 900)).resize((700, 588))
        g = Image.open(f'{OUT}/{v}_giant.png').crop((0, 40, 1000, 778)).resize((350, 258))
        f = Image.open(f'{OUT}/{v}_field.png').crop((0, 40, 1000, 778)).resize((350, 258))
        c = Image.new('RGB', (700, 36 + 588 + 258), (255, 255, 255)); c.paste(a, (0, 36)); c.paste(g, (0, 624)); c.paste(f, (350, 624))
        ImageDraw.Draw(c).text((8, 6), TITLE[v], font=F(20, True), fill=(15, 15, 15)); cells.append(c)
    B = Image.new('RGB', (3 * 710 + 10, 2 * 892 + 90), (255, 255, 255))
    for i, c in enumerate(cells): B.paste(c, (10 + (i % 3) * 710, 56 + (i // 3) * 892))
    d = ImageDraw.Draw(B); d.text((12, 12), 'Heaven garden vegetation V1-V5 (identical terrain, natural palette, camera, light, background)', font=F(24, True), fill=(10, 10, 10))
    x0, y0 = 10 + 2 * 710, 56 + 892
    d.text((x0 + 10, y0 + 20), 'Each cell: full view (top),\ngiant close-up (bottom-left),\nflower-field close-up (bottom-right).', font=F(18), fill=(40, 40, 50))
    d.text((x0 + 10, y0 + 120), 'Terrain: NBT-BASED render of\nsky_islands/meadow/plain_medium_d\n(A grass / B loam / B2 rock, fixed).\n\nVegetation: DESIGN PROTOTYPE voxel\nmodels (1/4 block) placed on the\nNBT surface; not in-game, not\nwritten to any game NBT.', font=F(16), fill=(60, 60, 70))
    B.save(f'{OUT}/comparison_board.png', optimize=True)


if __name__ == '__main__':
    t0 = time.time()
    with Pool(JOBS) as p:
        res = dict(p.map(render_variant, VARIANTS))
    print('variants', round(time.time() - t0, 1))
    data = {'variants': res}
    data['giant_detail'] = giant_detail(); print('giant detail', round(time.time() - t0, 1))
    catalog(); print('catalog', round(time.time() - t0, 1))
    data['height_study'] = height_study(); print('height', round(time.time() - t0, 1))
    keys = sorted({k for v in res.values() for k in v['counts']})
    thumbs = {k: thumb(species_model(k)) for k in keys if species_model(k) is not None}
    data['costs'] = {}
    for v in VARIANTS:
        cc, ct = rs.variant_cost(res[v]['counts']); data['costs'][v] = dict(per_category=cc, total=ct)
        palette_sheet(v, res[v], cc, ct, thumbs)
    board()
    for v in VARIANTS: res[v].pop('render_seconds', None)
    json.dump(data, open(f'{OUT}/../render/results.json', 'w'), indent=1)
    print('done', round(time.time() - t0, 1))

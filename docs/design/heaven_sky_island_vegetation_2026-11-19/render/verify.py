"""Verification for the vegetation round.
1. terrain blocks used by every render == the NBT's terrain blocks (vegetation removed only)
2. natural palette == owner-fixed combination (A grass top / A fringe on B loam / B loam / B2 rock)
3. layout determinism: rebuild every layout in this process, compare fine-grid crc32 with results.json
4. render reproducibility: re-render V1 three views to a temp dir, compare pixels with images/"""
import sys, json, hashlib, tempfile, os, zlib, numpy as np
from PIL import Image
import raycast as rc, veg_layout as vl, palettes_veg as pv
res = {}
nbt = rc.BASE + vl.TEMPLATE
res['nbt_sha256'] = hashlib.sha256(open(nbt, 'rb').read()).hexdigest()
raw, _ = rc.load_grid(vl.TEMPLATE); s = vl.Surface()
terr_ids = list(rc.K_OPAQUE - {rc.KID['oak_log']}) + [rc.K_WATER]
a = np.isin(raw, terr_ids); b = s.terrain[:, :raw.shape[1], :]
res['terrain_identical_to_nbt'] = bool(((raw * a) == b).all() and (s.terrain[:, raw.shape[1]:, :] == 0).all())
res['terrain_blocks'] = int(a.sum()) - int((raw == rc.K_WATER).sum())
res['removed_template_vegetation'] = {rc.KINDS[k]: int((raw == k).sum()) for k in np.unique(raw) if k and k not in terr_ids}
P = pv.PALETTES['HG']
res['natural_palette'] = P
res['natural_palette_ok'] = P == dict(grass_block='heaven_grass_AL', dirt='garden_loam_B', coarse_dirt='garden_loam_B', stone='cream_stone_B2', andesite='cream_stone_pale_B2', tuff='cream_stone_pale_B2')
R0 = json.load(open('results.json'))
res['layout_crc32_match'] = {v: zlib.crc32(vl.build(v, s).fine.tobytes()) == R0['variants'][v]['fine_grid_crc32'] for v in ['V1', 'V2', 'V3', 'V4', 'V5']}
T = tempfile.mkdtemp(); sys.argv = ['make_all.py', T]
import make_all
make_all.render_variant('V1')
res['render_V1_pixel_identical'] = {f: bool((np.asarray(Image.open(os.path.join(T, f))) == np.asarray(Image.open('../images/' + f))).all()) for f in ('V1_full.png', 'V1_giant.png', 'V1_field.png')}
print(json.dumps(res, indent=1)); json.dump(res, open('verify.json', 'w'), indent=1)

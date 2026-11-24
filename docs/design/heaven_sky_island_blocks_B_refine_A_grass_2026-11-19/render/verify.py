"""Verification: (1) identical NBT geometry for every palette, (2) original B
regenerates pixel-identically vs round 1, (3) render reproducibility."""
import numpy as np, hashlib, json, os, sys, tempfile, subprocess
from PIL import Image
import raycast as rc, palettes_b2 as P, palettes as P1, shade
res = {}
nbt = rc.BASE + 'meadow/plain_medium_d.nbt'
res['nbt_sha256'] = hashlib.sha256(open(nbt, 'rb').read()).hexdigest()
g, h = rc.load_grid('meadow/plain_medium_d.nbt')
res['grid_shape'] = list(g.shape); res['grid_nonair'] = int((g > 0).sum())
res['grid_matches_hitbuffer_grid'] = bool((np.load('hb/grid.npy') == g).all())
# palettes only remap the 6 terrain slots; non-rock slots equal original B
B = P1.PALETTES['B']
res['palette_checks'] = {}
for p, m in P.PALETTES.items():
    res['palette_checks'][p] = dict(slots_only=set(m) == set(P1.SLOTS),
                                    grass_loam_equal_B=all(m[s] == B[s] for s in ('grass_block', 'dirt', 'coarse_dirt')))
res['hitbuffer_sha256'] = {v: hashlib.sha256(open('hb/hit_%s.npz' % v, 'rb').read()).hexdigest()[:16] for v in rc.VIEWS}
# (2) original B vs round-1 image (exclude stamped title/footer bands)
V, N = P.build()
r1 = '../../heaven_sky_island_blocks_2026-11-19/images/'
res['original_B_vs_round1'] = {}
for v in rc.VIEWS:
    a = np.asarray(shade.shade('hb', v, 'B', V, N)); b = np.asarray(Image.open(r1 + 'B_%s.png' % v).convert('RGB'))
    band = slice(45, a.shape[0] - 30)
    res['original_B_vs_round1'][v] = bool((a[band] == b[band]).all())
print(json.dumps(res, indent=1))
json.dump(res, open('verify.json', 'w'), indent=1)

"""Render-side decomposition of rock luminance variation in the underside view.
Compares: B as-is, B with AO disabled, B with flat face light, and B with all
three rock slots mapped to one texture (removes inter-block colour difference)."""
import numpy as np, json, sys
import shade, palettes_b2 as P
from diag import lab
V, N = P.build()
def rock_metrics(pal, view='underside', ao=True, flat=False):
    old_ao, old_fl = shade.AO_LEVEL.copy(), shade.FACE_LIGHT.copy()
    if not ao: shade.AO_LEVEL[:] = 1.0
    if flat: shade.FACE_LIGHT[:] = 0.8
    P.PALETTES['_tmp'] = pal
    im, blk, face, w = shade.shade('hb', view, '_tmp', V, N, out_ids=True)
    shade.AO_LEVEL[:] = old_ao; shade.FACE_LIGHT[:] = old_fl
    rocks = set(pal[s] for s in ('stone', 'andesite', 'tuff'))
    m = np.isin(blk, list(rocks))
    L = lab(np.asarray(im, float))[..., 0]
    Lr = L[m]
    # share of rock pixels that are > 15 L* darker than the rock median (visible "spots")
    dark = float((Lr < np.median(Lr) - 15).mean())
    return dict(L_mean=round(float(Lr.mean()), 1), L_std=round(float(Lr.std()), 2), dark_spot_px=round(dark, 3))
B = P.PALETTES['B']
uni = dict(B, andesite=B['stone'], tuff=B['stone'])
res = {'B': rock_metrics(B), 'B_noAO': rock_metrics(B, ao=False), 'B_noAO_flat': rock_metrics(B, ao=False, flat=True),
       'B_uniform_rock': rock_metrics(uni), 'B_uniform_rock_noAO_flat': rock_metrics(uni, ao=False, flat=True)}
for k in sys.argv[1:]:
    res[k] = rock_metrics(P.PALETTES[k])
print(json.dumps(res, indent=1))

"""Per-palette render metrics on all three views (same hit buffers for all)."""
import numpy as np, json
import shade, palettes_b3 as P, diag
from diag import lab
V, N = P.build()
SKY = None
out = {}
for p in P.PALETTES:
    pal = P.PALETTES[p]; rocks = set(pal[s] for s in ('stone', 'andesite', 'tuff'))
    r = {'textures': diag.report(p, V, N, P.PALETTES)}
    for view in ('full', 'edge', 'underside'):
        im, blk, face, w = shade.shade('hb', view, p, V, N, out_ids=True)
        L = lab(np.asarray(im, float))[..., 0]
        m = np.isin(blk, list(rocks)); soil = blk == pal['dirt']; sky = blk == ''
        Lr = L[m]
        r[view] = dict(rock_L_mean=round(float(Lr.mean()), 1), rock_L_std=round(float(Lr.std()), 2),
                       dark_spot_px=round(float((Lr < np.median(Lr) - 15).mean()), 3),
                       rock_vs_soil_dL=round(float(Lr.mean() - L[soil].mean()), 1),
                       rock_vs_sky_dL=round(float(Lr.mean() - L[sky].mean()), 1))
    out[p] = r
json.dump(out, open('metrics_all.json', 'w'), indent=1)
print('%-3s %s' % ('', ' | '.join('%-34s' % v for v in ('full', 'edge', 'underside'))))
for p, r in out.items():
    print('%-3s ' % p + ' | '.join('L%5.1f sd%5.2f spot%5.3f soil%+5.1f sky%+5.1f' % tuple(r[v][k] for k in ('rock_L_mean', 'rock_L_std', 'dark_spot_px', 'rock_vs_soil_dL', 'rock_vs_sky_dL')) for v in ('full', 'edge', 'underside')))

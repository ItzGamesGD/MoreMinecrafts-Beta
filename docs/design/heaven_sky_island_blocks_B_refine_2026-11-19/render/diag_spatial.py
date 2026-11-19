"""Spatial and render-side diagnostics for the rock slots of the reference NBT."""
import numpy as np, json, collections
import raycast as rc
from scipy import ndimage
g, half = rc.load_grid('meadow/plain_medium_d.nbt')
K = {n: rc.KID[n] for n in ('stone', 'andesite', 'tuff')}
rock = np.isin(g, list(K.values()))
air = ~np.isin(g, list(rc.K_OPAQUE) + [rc.K_LEAF])
exp = np.zeros_like(rock)
for ax in range(3):
    for s in (-1, 1):
        sh = np.roll(air, s, axis=ax)
        # out-of-bounds counts as air
        idx = [slice(None)] * 3; idx[ax] = 0 if s == 1 else -1; sh[tuple(idx)] = True
        exp |= sh
exp &= rock
res = {}
# 1. same-slot neighbour probability vs. random expectation
tot = {n: int((g == k).sum()) for n, k in K.items()}
N = sum(tot.values())
same = collections.Counter(); pairs = collections.Counter()
for ax in range(3):
    a = [slice(None)] * 3; b = [slice(None)] * 3; a[ax] = slice(0, -1); b[ax] = slice(1, None)
    A = g[tuple(a)]; B = g[tuple(b)]; m = rock[tuple(a)] & rock[tuple(b)]
    for n, k in K.items():
        pairs[n] += int(((A == k) & m).sum() + ((B == k) & m).sum())
        same[n] += 2 * int(((A == k) & (B == k) & m).sum())
res['neighbour_same_slot'] = {n: dict(observed=round(same[n] / pairs[n], 3), random_expect=round(tot[n] / N, 3)) for n in K}
# 2. cluster sizes of exposed tuff (6-connected over exposed rock voxels)
for n in ('tuff', 'andesite'):
    lab_, cnt = ndimage.label((g == K[n]) & exp)
    sizes = np.bincount(lab_.ravel())[1:]
    res['exposed_%s_clusters' % n] = dict(count=int(cnt), single_block_share=round(float((sizes == 1).mean()), 3), median=float(np.median(sizes)), max=int(sizes.max()))
# 3. protrusions: rock voxels with <=1 horizontal rock/terrain neighbour (pillars, drips)
solid = ~air
hn = sum(np.roll(solid, s, axis=ax).astype(int) for ax in (0, 2) for s in (-1, 1))
pill = rock & (hn <= 1)
ys = np.nonzero(rock)[1]
res['protrusion_voxels'] = dict(count=int(pill.sum()), share_of_exposed_rock=round(float(pill.sum() / exp.sum()), 3),
                                tuff_share_in_protrusions=round(float((pill & (g == K['tuff'])).sum() / pill.sum()), 3))
print(json.dumps(res, indent=1))
json.dump(res, open('diag_spatial.json', 'w'), indent=1)

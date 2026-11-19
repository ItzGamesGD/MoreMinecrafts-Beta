"""Diagnose original B rock blotchiness: texture contrast, inter-block colour
difference, spatial distribution of rock slots, protrusion share and the
renderer's own lighting contribution."""
import numpy as np, json, sys, collections
import palettes as pl, raycast as rc

def lab(rgb):
    c = np.asarray(rgb, float) / 255
    c = np.where(c > 0.04045, ((c + 0.055) / 1.055) ** 2.4, c / 12.92)
    M = np.array([[0.4124, 0.3576, 0.1805], [0.2126, 0.7152, 0.0722], [0.0193, 0.1192, 0.9505]])
    xyz = c @ M.T / np.array([0.95047, 1.0, 1.08883])
    f = np.where(xyz > 0.008856, np.cbrt(xyz), 7.787 * xyz + 16 / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], -1)

def tex_stats(t):
    L = lab(t); m = L.reshape(-1, 3).mean(0)
    dE = np.linalg.norm(L - m, axis=-1)
    Lc = L[..., 0]
    # neighbour step: mean |dL| between 4-neighbours (high = speckly)
    nb = (np.abs(np.diff(Lc, axis=0)).mean() + np.abs(np.diff(Lc, axis=1)).mean()) / 2
    return dict(L=round(m[0], 1), a=round(m[1], 1), b=round(m[2], 1), L_std=round(Lc.std(), 2),
                L_range=round(Lc.max() - Lc.min(), 1), dE_p90=round(np.percentile(dE, 90), 1), nb_step=round(nb, 2))

def block_tex(V, N, name):
    t = N.get(name) or V.get(name); return t['side']

def report(pal_name, V, N, PAL):
    pal = PAL[pal_name]; rocks = [pal[s] for s in ('stone', 'andesite', 'tuff')]
    out = {'textures': {}, 'inter_dE': {}}
    for r in dict.fromkeys(rocks + [pal['dirt']]):
        out['textures'][r] = tex_stats(block_tex(V, N, r))
    means = {r: lab(block_tex(V, N, r)).reshape(-1, 3).mean(0) for r in dict.fromkeys(rocks + [pal['dirt']])}
    keys = list(means)
    for i in range(len(keys)):
        for j in range(i + 1, len(keys)):
            out['inter_dE'][keys[i] + ' / ' + keys[j]] = round(float(np.linalg.norm(means[keys[i]] - means[keys[j]])), 1)
    return out

if __name__ == '__main__':
    import palettes_b2 as P
    V, N = P.build()
    res = {p: report(p, V, N, P.PALETTES) for p in sys.argv[1:]}
    print(json.dumps(res, indent=1, ensure_ascii=False))

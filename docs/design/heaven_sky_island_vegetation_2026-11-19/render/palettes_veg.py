"""Fixed natural palette for the vegetation round (owner-confirmed combination):
  grass top  : round-1 A  heaven_grass_A top
  grass side : A fringe pixels kept, soil base re-based onto B loam
  soil       : B garden_loam_B (dirt + coarse_dirt)
  rock       : B2 natural cream (stone -> cream_stone_B2, andesite/tuff -> cream_stone_pale_B2)
Built from the unchanged round-1 / round-2 builders."""
import numpy as np
from palettes import *          # noqa: F401,F403
import palettes_b2 as _b2


def build():
    V, N = _b2.build()
    a = N['heaven_grass_A']; loam = N['garden_loam_B']['side']
    ov = load('grass_block_side_overlay')[..., 3] > 0          # A's fringe mask (vanilla overlay shape)
    side = loam.copy(); side[ov] = a['side'][ov]
    N['heaven_grass_AL'] = dict(top=a['top'], side=side, bottom=loam)
    return V, N


PALETTES = {'HG': dict(grass_block='heaven_grass_AL', dirt='garden_loam_B', coarse_dirt='garden_loam_B',
                       stone='cream_stone_B2', andesite='cream_stone_pale_B2', tuff='cream_stone_pale_B2')}

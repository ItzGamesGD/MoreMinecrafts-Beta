"""Round 2b: B / B1-B5 rock palettes from palettes_b2.py with only the grass
slot swapped to round-1 A grass (heaven_grass_A).  Soil and rock unchanged."""
from palettes_b2 import *       # noqa: F401,F403
import palettes_b2 as _b2
build = _b2.build
PALETTES = {k + 'g': dict(v, grass_block='heaven_grass_A') for k, v in _b2.PALETTES.items()}

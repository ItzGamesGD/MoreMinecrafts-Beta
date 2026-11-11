#!/usr/bin/env python3
"""Rise statistics from EXPORTED tread tops (stdlib): neighbour slope, rising/flat path length, capacity.
usage: python3 -I p3_rise_stats.py  -> data/rise_stats.json"""
import json, math
from pathlib import Path
HERE = Path(__file__).resolve().parent; C = HERE / 'data' / 'candidates'
RISES = (11, 12, 14, 17); NB4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
out = {}
for d in sorted(p for p in C.iterdir() if p.is_dir()):
    tt = {tuple(map(int, k.split(','))): v for k, v in json.loads((d / 'tread_tops_C1.json').read_text()).items()}
    band = {(x, z) for L, x, z in tt}
    maxd = max(abs(tt[L, x, z] - tt[L, x + dx, z + dz]) for (L, x, z) in tt for dx, dz in NB4 if (L, x + dx, z + dz) in tt
               and abs(math.atan2(z, x) % (2 * math.pi) - math.atan2(z + dz, x + dx) % (2 * math.pi)) < math.pi)   # exclude the lap seam at theta=0
    # centreline ring cells (17 <= r < 18) ordered by angle: flat vs rising steps per lap
    ring = sorted((c for c in band if 17 <= math.hypot(*c) < 18), key=lambda c: math.atan2(c[1], c[0]) % (2 * math.pi))
    laps = {}
    for L in range(4):
        h = [tt[L, x, z] for x, z in ring]
        steps = [b - a for a, b in zip(h, h[1:])]
        laps[L] = {'centreline_cells': len(ring), 'rising_steps': sum(1 for s in steps if s > 0), 'flat_steps': sum(1 for s in steps if s == 0),
                   'max_step': max(steps), 'rise': RISES[L], 'path_cells_needed_at_max_step': math.ceil(RISES[L] / max(steps))}
    # angular capacity: largest angle between 4-adjacent band cells -> min angle per half-step
    amax = max(abs(((math.atan2(z + dz, x + dx) - math.atan2(z, x)) + math.pi) % (2 * math.pi) - math.pi)
               for x, z in band for dx, dz in NB4 if (x + dx, z + dz) in band)
    out[d.name] = {'max_neighbour_height_difference': maxd, 'per_lap_centreline': laps,
                   'max_angle_between_adjacent_band_cells_deg': round(math.degrees(amax), 2),
                   'theoretical_max_rise_per_lap_blocks_if_all_rising_half_step': round(0.5 * (360 - 24) / math.degrees(amax), 1),
                   'theoretical_max_rise_per_lap_blocks_if_all_rising_full_step': round(1.0 * (360 - 24) / math.degrees(amax), 1)}
(HERE / 'data' / 'rise_stats.json').write_text(json.dumps(out, indent=1) + '\n')
for k, v in out.items(): print(k, v['max_neighbour_height_difference'], v['max_angle_between_adjacent_band_cells_deg'], v['theoretical_max_rise_per_lap_blocks_if_all_rising_half_step'], v['per_lap_centreline'][3])

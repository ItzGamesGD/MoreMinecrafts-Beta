#!/usr/bin/env python3
"""GEOMETRY-03C-P2 3D audit. Uses the FROZEN 03B verify.py/navigation modules (not the P2 generator) plus
an independent exported-CSV D45/D51/height boundary audit.  stages: 1 stairs, 2 +pillar, 3 +rail only.
usage: python3 -I p2_audit3d.py exported_dir out.json
"""
import csv, importlib.util, json, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('v03b', HERE.parents[1] / 'sky_tower_geometry_03b/verify.py')
v = importlib.util.module_from_spec(spec); spec.loader.exec_module(v)
v.STAGES = v.STAGES[:5]   # stairs, +pillar, +rail, +balcony members, +arcade arches (no links/canopy/gateway built by P2)


def boundary(folder, conv):
    rows = list(csv.DictReader((folder / f'blocks_{conv}.csv').open()))
    cols = {}; ys = []
    for r in rows:
        x, y, z = int(r['x']), int(r['y']), int(r['z']); ys.append(y)
        cols.setdefault(r['group'], set()).add((x, z))
    normal = set().union(*(v for k, v in cols.items() if not k.startswith('balcony')))
    bal = set().union(*(v for k, v in cols.items() if k.startswith('balcony')), set())
    out45c = sorted(c for c in normal if not 4 * (c[0] ** 2 + c[1] ** 2) < 45 ** 2)
    out45w = sorted(c for c in normal if not (2 * abs(c[0]) + 1) ** 2 + (2 * abs(c[1]) + 1) ** 2 <= 45 ** 2)
    return {'blocks': len(rows), 'normal_columns': len(normal), 'outside_D45_center': len(out45c),
            'outside_D45_whole_block_alternative': len(out45w), 'y_min': min(ys), 'y_max': max(ys),
            'height_within_0_79': min(ys) >= (0 if conv == 'C1' else -1) and max(ys) <= 79, 'balcony_columns': len(bal), 'balcony_outside_D51_center': len([c for c in bal if not 4 * (c[0] ** 2 + c[1] ** 2) < 51 ** 2]),
            'balcony_outside_D51_whole_block_alternative': len([c for c in bal if not (2 * abs(c[0]) + 1) ** 2 + (2 * abs(c[1]) + 1) ** 2 <= 51 ** 2]),
            'per_group_outside_D45_center':
            {g: len({c for c in cs if not 4 * (c[0] ** 2 + c[1] ** 2) < 45 ** 2}) for g, cs in sorted(cols.items()) if not g.startswith('balcony')}}


def overlaps(folder, conv):
    g = {}
    for r in csv.DictReader((folder / f'blocks_{conv}.csv').open()):
        g.setdefault(r['group'], set()).add((int(r['x']), int(r['y']), int(r['z'])))
    names = sorted(g); out = {}
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            n = len(g[a] & g[b])
            if n: out[f'{a}&{b}'] = n
    return out


def main(folder, outp):
    folder = Path(folder); res = {}
    for conv in ('C1', 'C2'):
        a = v.audit(folder, conv, run_bfs=True)
        stages = [{'stage': s['stage'], 'members': s['members'], 'status': s['status'],
                   'width_status': s['width']['status'], 'first_width_failure': s['width']['first_failure'],
                   'min_overhead_clearance': s['width']['minimum_overhead_clearance'],
                   'reached_F2_F3_F4_SANCTUM_then_balconies': (s['navigation'] or {}).get('reached'), 'first_disconnected_target': s['first_disconnected_target'],
                   'balconies': [(b['id'], b['pass']) for b in s['balconies']], 'visited_states': (s['navigation'] or {}).get('visited_states'), 'limit_hit': (s['navigation'] or {}).get('limit_hit')}
                  for s in a['stages']]
        res[conv] = {'stages': stages, 'pillar_spacing_projected': [s['projected_station_difference'] for s in a['pillar_spacing'][:12]],
                     'boundary': boundary(folder, conv), 'voxel_overlaps_between_groups': overlaps(folder, conv), 'in_game_validation': 'NOT RUN'}
    Path(outp).write_text(json.dumps(res, ensure_ascii=False, separators=(',', ':')) + '\n')
    for c, r in res.items():
        print(c, [(s['stage'], s['status'], s['reached_F2_F3_F4_SANCTUM_then_balconies'], s['width_status']) for s in r['stages']], r['boundary']['outside_D45_center'], r['voxel_overlaps_between_groups'])


if __name__ == '__main__':
    main(*sys.argv[1:3])

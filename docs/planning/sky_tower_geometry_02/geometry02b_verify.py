#!/usr/bin/env python3
"""Coordinate-only GEOMETRY-02B. No structure generation or design decisions.

Run: python3 -I geometry02b_verify.py [--out DIR]
Exit 0 means the verifier ran and its invariants passed, NOT design approval.
All proposed carving/exterior masks are probes, never merged into the baseline.
"""
import argparse
from collections import Counter, deque
import csv
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('geometry02_original', HERE / 'geometry02_model.py')
old = importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)
DIRS = ((1, 0), (-1, 0), (0, 1), (0, -1))
SOURCE_COMMIT = 'a5fcb95fe4c81ee0ec422f78f4883aa623480ba1'
PINNED = {
    'geometry02_model.py': '24295fa1513bfcee66e9c2999cac163219fb77d4',
    'data/plan_B25-T3.json': '23b69bc4fa6349be2bcf5fc2b7d9ada7880de4d9',
    'data/model3d_B25-T3_C1_s1.json': '6a5c70c828f1a15fd7c5b8c1e9c609f4ed89187c',
    '../SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md': 'b258e49ae80a04c02a806dd881b701295f9cb288',
}


def square(center, size):
    x, z = center
    return {(x + u, z + v) for u in range(-(size // 2), size // 2 + 1)
            for v in range(-(size // 2), size // 2 + 1)}


def probe_niche(start, direction, width, wall, full):
    """Axis-aligned two-deep carving; require solid immediate back and side faces.

    width=1 is a minimum voxel/vault probe; width=3 is sensitivity, not authority.
    Front faces open toward interior. Height/arch/functional access are separate.
    """
    x, z = start
    dx, dz = direction
    tx, tz = -dz, dx
    run = 0
    while (x + run * dx, z + run * dz) in wall:
        run += 1
    cavity = {(x + k * dx + j * tx, z + k * dz + j * tz)
              for k in range(2) for j in range(-(width // 2), width // 2 + 1)}
    rear = {(x + 2 * dx + j * tx, z + 2 * dz + j * tz)
            for j in range(-(width // 2), width // 2 + 1)}
    sides = {(x + k * dx + j * tx, z + k * dz + j * tz)
             for k in range(2) for j in (-(width // 2) - 1, width // 2 + 1)}
    front = {(x - dx + j * tx, z - dz + j * tz)
             for j in range(-(width // 2), width // 2 + 1)}
    inner = full - wall
    return {'start': start, 'outward': direction, 'width_probe': width, 'run': run,
            'cavity': sorted(cavity), 'rear': sorted(rear), 'sides': sorted(sides),
            'front': sorted(front), 'cavity_in_wall': cavity <= wall,
            'backing': rear <= wall, 'front_in_inner': front <= inner,
            'enclosed': cavity <= wall and (rear | sides) <= wall and front <= inner}


def wall_distances(wall, target):
    """Face-neighbor wall layers until reaching target (first wall layer = 1)."""
    distance = {p: 1 for p in wall if any((p[0] + dx, p[1] + dz) in target for dx, dz in DIRS)}
    todo = deque(distance)
    while todo:
        x, z = todo.popleft()
        for dx, dz in DIRS:
            p = x + dx, z + dz
            if p in wall and p not in distance:
                distance[p] = distance[x, z] + 1
                todo.append(p)
    assert set(distance) == wall
    return distance


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, default=HERE / 'geometry02b')
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    provenance = {}
    for path, expected in PINNED.items():
        raw = (HERE / path).read_bytes()
        actual = hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()
        assert actual == expected, (path, actual, expected)
        provenance[path] = {'git_blob': actual, 'sha256': hashlib.sha256(raw).hexdigest()}
    plan = json.loads((HERE / 'data/plan_B25-T3.json').read_text())
    inner, wall, reserve = (set(map(tuple, plan[k])) for k in ('inner', 'wall', 'reserve_band'))
    full, env = inner | wall, inner | wall | reserve
    assert inner == old.disc('B', 25) and full == old.disc('B', 31)
    assert env == old.disc('B', 45) and not (inner & wall or full & reserve)
    checks = []

    def check(cid, status, evidence):
        checks.append({'id': cid, 'status': status, 'evidence': evidence})

    models, vertical = {}, {}
    for conv in ('C1', 'C2'):
        occ, rows = old.build_3d('B', inner, wall, conv, 1)
        assert all(0 <= y < 80 and (x, z) in full for x, y, z in occ)
        assert not any(y >= 65 for x, y, z in occ)
        assert [r['clear_h'] for r in rows[1:5]] == [10, 11, 13, 16]
        layers = [{'y': y, 'zone': zone} for y, zone in sorted({(y, zone) for (x, y, z), zone in occ.items()})]
        models[conv] = {'layers': layers, 'voxel_count': len(occ), 'zone_cells':
                        {'BASE_CORE': 'inner+wall', 'SLAB': 'inner+wall', 'WALL': 'wall'}}
        for i, r in enumerate(rows[1:], 1):
            # r.slab in C2 is the ceiling owned by r, NOT the walking floor.
            r['walking_floor_y'] = r['y_start'] if conv == 'C1' else rows[i - 1]['y_end']
            r['walking_surface_y'] = r['walking_floor_y'] + 1
        vertical[conv] = rows
        check('HEIGHT_' + conv, 'PASS', {'budgets': [10, 11, 12, 14, 17, 16],
              'total_budget': 80, 'occupied_y': [0, max(y for x, y, z in occ)],
              'reserved_apex_y': 79, 'apex_placed': False, 'slab_thickness': 1})
    oldmodel = json.loads((HERE / 'data/model3d_B25-T3_C1_s1.json').read_text())
    assert models['C1']['layers'] == oldmodel['layers']
    baseline = {'task': 'SKY-TOWER-GEOMETRY-02B', 'source_commit': SOURCE_COMMIT,
        'authorization': '2026-11-06 task: B25-T3; slab=1; X/Z occupancy measure for base D45',
        'candidate': 'B25-T3', 'slab_thickness': 1, 'slab_ownership': None,
        'axis_point_xz': [0.5, 0.5], 'block_cube': '[x,x+1) x [y,y+1) x [z,z+1)',
        'columns': {k: sorted(v) for k, v in [('inner', inner), ('wall', wall), ('reserve_band', reserve)]},
        'models': models, 'vertical': vertical, 'source_hashes': provenance,
        'note': 'Expand each (y,zone) layer over its columns. No carving or exterior probes applied. Arches unplaced.'}
    check('SLAB_OWNERSHIP', 'UNVERIFIED', 'C1 and C2 fit 80; sanctum clear 15 vs 16, walking floors differ by 1.')
    check('AXIS_DIAMETER', 'PASS', {'inner': old.extent(inner), 'wall_outer': old.extent(full),
          'reserved_base_envelope': old.extent(env), 'base_axis_limit': 45,
          'wall_far_corner_diameter': old.far_corner_diameter('B', full),
          'envelope_far_corner_diameter': old.far_corner_diameter('B', env),
          'note': 'Envelope is permitted/reserved coordinates, not fully built exterior.'})

    faces = []
    for x, z in sorted(inner):
        for dx, dz in DIRS:
            start = x + dx, z + dz
            if start in wall:
                for width in (1, 3):
                    faces.append(probe_niche(start, (dx, dz), width, wall, full))
    valid = [r for r in faces if r['enclosed']]
    external = {(x, z) for x in range(-24, 25) for z in range(-24, 25)} - full
    din, dout = wall_distances(wall, inner), wall_distances(wall, external)
    with (args.out / 'wall_cells.csv').open('w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['x', 'z', 'face_steps_to_inner', 'face_steps_to_outside',
                         'run_plus_x', 'run_minus_x', 'run_plus_z', 'run_minus_z'])
        for x, z in sorted(wall):
            runs = []
            for dx, dz in DIRS:
                n = 0
                while (x + n * dx, z + n * dz) in wall:
                    n += 1
                runs.append(n)
            writer.writerow([x, z, din[x, z], dout[x, z], *runs])
    check('NICHE_DEPTH_EXISTENCE', 'PASS' if valid else 'UNVERIFIED', {
        'interior_faces': len(faces) // 2, 'run_histogram': dict(sorted(Counter(r['run'] for r in faces if r['width_probe'] == 1).items())),
        'valid_by_probe_width': {str(w): sum(r['enclosed'] for r in faces if r['width_probe'] == w) for w in (1, 3)},
        'meaning': 'Individual 2-deep axis carvings retain >=1 rear/side block; not simultaneous final niche layout.'})
    sample = next(r for r in valid if r['start'] == (13, 0) and r['outward'] == (1, 0) and r['width_probe'] == 3)
    cavity3d = sorted((x, y, z) for x, z in sample['cavity'] for y in range(12, 15))
    vault = (14, 12, 0)
    for conv in models:
        occ, _ = old.build_3d('B', inner, wall, conv, 1)
        assert all(occ.get(p) == 'WALL' for p in cavity3d) and vault in cavity3d
        assert all((x, y, z) not in occ for x, z in sample['front'] for y in range(12, 15))
    niche_result = {'faces': faces, 'wall_cells': 'wall_cells.csv', 'sample_only': {
        **sample, 'cavity_xyz': cavity3d, 'vault_voxel_xyz': vault,
        'note': '3-wide/3-high existence witness only, not approved dimensions; vault activation, arch, openings, stair and symmetry layout UNVERIFIED.'}}
    check('VAULT_FINAL_LAYOUT', 'UNVERIFIED', 'Voxel can fit in sample niche; width, height, arch, interaction and full layout unspecified.')

    central, band_sensitivity = old.disc('B', 17), old.disc('B', 19) - old.disc('B', 17)
    # Most generous union of individually valid 2-deep carvings; not a simultaneous approved carve.
    niche_floor = inner | {tuple(p) for r in valid if r['width_probe'] == 1 for p in r['cavity']}
    pattern = {'mask_semantics': {'central_D17': 'filled reserved envelope, NOT exact mosaic strokes',
        'square7': '7x7 bounding footprint, NOT final annular motif',
        'ring7_minus3': '40-cell square annulus sensitivity only, NOT approved ring',
        'band_D19_minus_D17': '1-block radial sensitivity only; Q70 width unspecified'},
        'central_D17': sorted(central), 'band_sensitivity': sorted(band_sensitivity), 'domains': {}}
    for label, domain in [('inner', inner), ('optimistic_niche_union', niche_floor)]:
        trials = []
        for center in sorted(domain):
            s7, s3 = square(center, 7), square(center, 3)
            if not s7 <= domain:
                continue
            ring = s7 - s3
            trials.append({'center': center, 'overlap_square_central': sorted(s7 & central),
                'overlap_ring_central': sorted(ring & central), 'overlap_altar_central': sorted(s3 & central),
                'overlap_square_band_sensitivity': sorted(s7 & band_sensitivity)})
        assert trials
        pattern['domains'][label] = {'columns': sorted(domain), 'placements': trials,
            'fitting_square7': len(trials),
            'disjoint_square7': sum(not r['overlap_square_central'] for r in trials),
            'disjoint_ring7': sum(not r['overlap_ring_central'] for r in trials),
            'minimum_square_overlap': min(len(r['overlap_square_central']) for r in trials),
            'minimum_ring_overlap': min(len(r['overlap_ring_central']) for r in trials),
            'altar3_disjoint_centers': [c for c in sorted(domain) if square(c, 3) <= domain and not square(c, 3) & central]}
    trials = pattern['domains']['inner']['placements']
    witness = min(trials, key=lambda r: (len(r['overlap_square_central']), r['center']))
    pattern['minimum_overlap_witness'] = witness
    check('DISJOINT_ENVELOPE_IN_INNER', 'CONFLICT' if all(r['overlap_square_central'] for r in trials) else 'PASS',
          {'condition': 'ONLY if full 7x7 footprint and filled central D17 must be disjoint inside D25',
           'placements': len(trials), 'minimum_overlap': len(witness['overlap_square_central']), 'witness': witness})
    check('ACTUAL_PATTERN_ALTAR_COMPATIBILITY', 'UNVERIFIED',
          'Exact ring/circle strokes, overlap permission, border width, altar count and full pillar/niche/entry layout missing. Conditional envelope conflict is not a design impossibility proof.')

    # Local cardinal witnesses: 4 walk columns + 3 pillar columns exhaust axial reserve.
    # Rail shares pillar radial lane at a different tangential station, never walk lane.
    local = {'walk': {(x, z) for x in range(16, 20) for z in range(-1, 2)},
             'pillar': {(x, z) for x in range(20, 23) for z in range(-1, 2)},
             'rail': {(22, z) for z in range(2, 5)},
             'canopy_inside_support': {(x, z) for x in range(19, 23) for z in range(-1, 2)},
             'canopy_beyond_outer_pillar': {(23, z) for z in range(-1, 2)}}
    assert all(local[k] <= reserve for k in ('walk', 'pillar', 'rail', 'canopy_inside_support'))
    assert not (local['walk'] & (local['pillar'] | local['rail']))
    rotations = []
    for turn in range(4):
        def rotate(p):
            x, z = p
            for _ in range(turn):
                x, z = -z, x
            return x, z
        rotated = {k: {rotate(p) for p in points} for k, points in local.items()}
        rotations.append({k: sorted(v) for k, v in rotated.items()})
        assert all(rotated[k] <= reserve for k in ('walk', 'pillar', 'rail', 'canopy_inside_support'))
    exterior = {'local_cardinal_probe_rotations': rotations,
        'semantics': 'Different tangential rail station; canopy at unspecified higher y. Local footprints only. No staircase or roof placed.',
        'canopy_outer_support_overflow': sorted(local['canopy_beyond_outer_pillar'] - env),
        'connected_reserve_8conn_components': old.components_8(reserve),
        'axis_reserve_columns_east_z0': sorted(p for p in reserve if p[1] == 0 and p[0] > 0)}
    check('EXTERIOR_LOCAL_FOOTPRINT', 'PASS', 'Four cardinal witnesses hold 4-wide walking lane, separate 3x3 pillar and separate rail station within reserve.')
    check('CANOPY_AT_OUTERMOST_SUPPORT', 'CONFLICT', {'condition': 'IF support outer edge is x22, +1 outward canopy at x23 exceeds X/Z limit',
          'coordinates': exterior['canopy_outer_support_overflow'],
          'note': 'Alternative support placement/integration not disproved; overall canopy feasibility UNVERIFIED.'})
    check('EXTERIOR_COMPLETE', 'UNVERIFIED', 'Full turn, diagonal walking width, rail continuity, ~7 spacing, vertical clearance, eaves, supports, arches, landings and balcony attachments not placed.')
    check('BASE_DETAILS', 'UNVERIFIED', 'Tier radii/count/heights, curved twin stairs width/rise, entry merge geometry, island join not defined.')
    check('SANCTUM_DETAILS', 'UNVERIFIED', '12 arch radius/pier section/rise, reward altar, floating ring, floor extent and boundary/railing coordinates unresolved; roofless maintained.')
    results = {'source_commit': SOURCE_COMMIT, 'candidate': 'B25-T3', 'checks': checks,
        'geometry03': {'ready_for_bounded_coordinate_exploration': True, 'ready_for_final_layout': False,
                       'ready_for_NBT_Java_worldgen': False,
                       'prerequisites': ['slab ownership C1/C2', 'exact motif strokes and overlap rule',
                           'Q70 border width', 'niche/vault dimensions and layout',
                           '3D stairs/pillars/rail/canopy integration', 'base/sanctum detail dimensions']}}
    for filename, data in [('baseline.json', baseline), ('niches.json', niche_result),
                           ('patterns.json', pattern), ('exterior.json', exterior), ('results.json', results)]:
        # Large exhaustive sets use compact JSON; all coordinates remain explicit.
        options = {'separators': (',', ':')} if filename in ('patterns.json', 'niches.json') else {'indent': 1}
        (args.out / filename).write_text(json.dumps(data, ensure_ascii=False, sort_keys=True, **options) + '\n', encoding='utf-8')
    summary = '\n'.join(r['id'] + ': ' + r['status'] for r in checks) + '\n'
    (args.out / 'run_output.txt').write_text(summary, encoding='utf-8')
    print(summary, end='')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

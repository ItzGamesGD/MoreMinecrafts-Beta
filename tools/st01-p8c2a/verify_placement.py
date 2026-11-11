#!/usr/bin/env python3
"""ST-01 P8-C2-A: verify the eight actual Minecraft placements dumped by St01PyramidAssemblyGameTests.

Every check reads the block data that a real Minecraft 26.3 server produced by placing the exported templates. The
approved blueprint only supplies the expected design and the authored goal coordinates.

  python3 tools/st01-p8c2a/verify_placement.py --dumps build/st01-p8c2a/placement --out build/st01-p8c2a/verification.json
"""
import argparse, base64, gzip, importlib.util, json, sys
from collections import deque
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('g0r1', ROOT / 'tools/st01-g0-r1/blueprint.py')
G = importlib.util.module_from_spec(spec); spec.loader.exec_module(G)  # approved semantics: recipe, rotated_state, reach
sys.path.insert(0, str(Path(__file__).parent))
import export_assembly as X

ORIENT_R = {'SOUTH': 0, 'WEST': 1, 'NORTH': 2, 'EAST': 3}
ROT_NAME = {'SOUTH': 'NONE', 'WEST': 'CLOCKWISE_90', 'NORTH': 'CLOCKWISE_180', 'EAST': 'COUNTERCLOCKWISE_90'}
FACE_VEC = {'NORTH': (0, -1), 'EAST': (1, 0), 'SOUTH': (0, 1), 'WEST': (-1, 0)}
PASSABLE = ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:wall_torch', 'minecraft:torch')
LO = X.LO; SHAPE = X.SIZE
rot = G.rotate
def idx(p): return tuple(p[i] - LO[i] for i in range(3))
def key(s): return G.canonical(s)


def design(route, bp, d1):
    """Expected authored block array (unwritten = air) and its palette, by the approved operation semantics."""
    variant = bp['variants'][route]
    palette = [dict(s) for s in variant['palette']]; lookup = {key(s): i for i, s in enumerate(palette)}
    grid = np.zeros(SHAPE, np.uint16); written = np.zeros(SHAPE, bool)
    air = lookup[key({'id': 'minecraft:air'})]; grid[:] = air
    for op in variant['operations']:
        sl = G.box_slice(op['box']); grid[sl] = lookup[key(op['state'])]; written[sl] = True
    d1_states = {}
    for b in d1['blocks']:
        p = tuple(a + o for a, o in zip(b['pos'], X.D1_ORIGIN)); s = d1['palette'][b['state']]
        grid[idx(p)] = lookup[key(s)]; written[idx(p)] = True; d1_states[p] = s
    for p in G.SEAM: grid[idx(p)] = air
    return palette, grid, written, d1_states


def compatible(expected, actual):
    """Expected state (possibly omitting defaulted properties) vs Minecraft's full state. Returns (ok, extra_props)."""
    if expected['id'] != actual['id']: return False, None
    ep, ap = expected.get('properties', {}), actual.get('properties', {})
    if any(ap.get(k) != v for k, v in ep.items()): return False, None
    return True, sorted(set(ap) - set(ep))


def load(path):
    d = json.loads(gzip.decompress(path.read_bytes()))
    raw = np.frombuffer(base64.b64decode(d['grid_u16le_xyz_b64']), '<u2')
    d['grid'] = raw.reshape(tuple(d['frame_size'])).astype(np.uint16)
    return d


def flood(passable, seeds):
    seen = np.zeros(passable.shape, bool); q = deque()
    for s in seeds:
        if passable[s] and not seen[s]: seen[s] = True; q.append(s)
    while q:
        x, y, z = q.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            p = (x + dx, y + dy, z + dz)
            if all(0 <= p[i] < passable.shape[i] for i in range(3)) and passable[p] and not seen[p]: seen[p] = True; q.append(p)
    return seen


def components(mask):
    """6-connected components of a boolean mask -> list of (size, authored-frame index bbox)."""
    seen = np.zeros(mask.shape, bool); out = []
    for start in map(tuple, np.argwhere(mask)):
        if seen[start]: continue
        comp = flood(mask & ~seen, [start]); seen |= comp
        pts = np.argwhere(comp); lo = pts.min(0) + np.array(LO); hi = pts.max(0) + np.array(LO)
        out.append({'cells': int(len(pts)), 'bounds': [int(lo[0]), int(hi[0]), int(lo[1]), int(hi[1]), int(lo[2]), int(hi[2])]})
    return sorted(out, key=lambda c: (-c['cells'], c['bounds']))


def _edge(ax, sh):
    """Cells whose rolled neighbour wrapped around the array edge (excluded from adjacency)."""
    m = np.zeros(SHAPE, bool); sl = [slice(None)] * 3; sl[ax] = 0 if sh == 1 else -1; m[tuple(sl)] = True; return m


def check_case(d, bp, d1, designs, recipes):
    o, route = d['orientation'], d['access']; r = ORIENT_R[o]
    palette, grid, written, d1_states = designs[route]; p2, p4 = recipes[route]
    act_pal = d['palette']; act = d['grid']
    res = {'case': o + '/' + route, 'rotation': d['rotation']}
    res['rotation_mapping'] = d['rotation'] == ROT_NAME[o]
    res['frame_is_completion_box'] = (tuple(d['frame_size']) == SHAPE and
                                      [d['frame_min'][i] - d['ground'][i] for i in range(3)] == list(LO))
    # 1. Complete block-by-block comparison against the rotated approved design (Minecraft-rotated states).
    exp_rot = np.rot90(grid, r, axes=(0, 2)); exp_pal = [G.rotated_state(s, r) for s in palette]
    pairs = np.unique(np.stack([exp_rot.ravel(), act.ravel()], 1), axis=0)
    bad_pairs, extras = [], set()
    for e, a in pairs:
        ok, extra = compatible(exp_pal[e], act_pal[a])
        if not ok: bad_pairs.append([exp_pal[e], act_pal[a], int(((exp_rot == e) & (act == a)).sum())])
        elif extra: extras.add(act_pal[a]['id'] + ':' + ','.join(extra))
    res['full_grid_matches_rotated_design'] = not bad_pairs
    res['mismatched_state_pairs'] = bad_pairs[:20]
    res['unspecified_default_properties'] = sorted(extras)
    aid = [s['id'] for s in act_pal]
    solid = ~np.isin(act, [i for i, s in enumerate(aid) if s in PASSABLE])
    is_air = np.isin(act, [i for i, s in enumerate(aid) if s in ('minecraft:air', 'minecraft:cave_air')])
    def at(p): return act_pal[int(act[idx(p)])]
    # 2. D1 preservation: compare every D1 cell with the rotated original, seam excluded only where approved.
    changed = sorted(p for p, s in d1_states.items() if not compatible(G.rotated_state(s, r), at(rot(p, r)))[0])
    res['d1_cells_checked'] = len(d1_states)
    res['d1_changed_cells_exactly_r1_01'] = set(changed) == G.SEAM and all(at(rot(p, r))['id'] == 'minecraft:air' for p in G.SEAM)
    res['d1_changed_count'] = len(changed)
    res['cover_intact_closed'] = all(solid[idx(rot(p, r))] for p in G.COVER)
    # 3. Traversal on actual blocks (rotated authored goals; navigator from the approved G0-R1 semantics).
    opened = solid.copy()
    for p in G.COVER: opened[idx(rot(p, r))] = False
    start = tuple(rot((0, 0, 40), r))
    closed_reach = G.reach(solid, start); open_reach = G.reach(opened, start)
    target = tuple(rot(tuple(p4['goals']['CHEST_FRONT'][2]), r)); crypt = tuple(rot(tuple(p4['goals']['CRYPT_IN']), r))
    res['entrance_to_primary'] = tuple(rot((-21, -20, 32), r)) in closed_reach
    res['secret_concealed_closed'] = target not in closed_reach and crypt not in closed_reach
    res['secret_open_treasury'] = target in open_reach
    res['secret_open_crypt'] = crypt in open_reach
    goals = {}
    for name, s in p2['spaces'].items():
        if name in ('T1', 'T2', 'T3', 'B1', 'B2', 'B3', 'A', 'V12', 'V23', 'ANNEX1'):
            b = G.bound(s['boxes']); goals[name] = tuple(rot(((b[0] + b[1]) // 2, b[2], (b[4] + b[5]) // 2), r))
    res['unreachable_room_goals'] = [n for n, g in goals.items() if g not in closed_reach]
    a2 = lambda x, z: tuple(rot((x, 0, z), r))
    s3, g3 = a2(0, 34), a2(7, 26)
    res['three_wide_landing_to_vault'] = G.flat_agent(opened, (s3[0], s3[2]), (g3[0], g3[2]))
    stair_fail = []
    for name, s in p2['spaces'].items():
        if s['kind'] != 'stair': continue
        for b in s['boxes']:
            p = rot(((b[0] + b[1]) // 2, b[2], (b[4] + b[5]) // 2), r)
            if not solid[idx((p[0], p[1] - 1, p[2]))] or any(solid[idx((p[0], p[1] + k, p[2]))] for k in range(3)): stair_fail.append(name)
    res['p2_stair_clearance_failures'] = stair_fail
    res['fa_descent_treads_4high'] = all(opened[idx(rot((x, y - 1, 34), r))] and not any(opened[idx(rot((x, y + k, 34), r))] for k in range(4))
                                         for x, y in p4['spaces']['FA']['treads'])
    res['guardian_clearance_3x3x5'] = all(not solid[idx(rot((x, y, z), r))] for x in range(-22, -19) for y in range(-22, -17) for z in range(25, 28))
    # 4. Exterior orientation and entrance.
    v = rot((0, 0, 1), r); res['entrance_faces'] = next(k for k, (vx, vz) in FACE_VEC.items() if (vx, vz) == (v[0], v[2]))
    res['entrance_orientation_correct'] = res['entrance_faces'] == o
    res['entrance_cell_open'] = not solid[idx(start)] and not solid[idx((start[0], 1, start[2]))]
    # 5. Route selection and fixed chambers.
    res['selected_route_open'] = not solid[idx(rot((0, -30, 27) if route == 'EAST' else (-10, -30, 30), r))]
    res['inactive_route_filled'] = bool(solid[idx(rot((-10, -30, 30) if route == 'EAST' else (0, -30, 27), r))])
    # 6. PRIMARY handedness, block entities, registration through the unchanged P8-B2 API.
    left, right = at(rot((-21, -20, 33), r)), at(rot((-20, -20, 33), r))
    f = left['properties']['facing']; dirs = ('north', 'east', 'south', 'west')
    vx, vz = {'north': (0, -1), 'east': (1, 0), 'south': (0, 1), 'west': (-1, 0)}[dirs[(dirs.index(f) + 1) % 4]]
    lp, rp = rot((-21, -20, 33), r), rot((-20, -20, 33), r)
    res['primary_double_handedness'] = (left['id'] == right['id'] == 'minecraft:chest' and left['properties']['type'] == 'left'
                                        and right['properties']['type'] == 'right' and right['properties']['facing'] == f
                                        and f == G.rotated_state({'id': 'x', 'properties': {'facing': 'north'}}, r)['properties']['facing']
                                        and (rp[0] - lp[0], rp[2] - lp[2]) == (vx, vz))
    expected_be = sorted(tuple(rot(tuple(e['pos']), r)) for e in bp['variants'][route]['block_entities'])
    actual_be = sorted(tuple(e['pos'][i] + LO[i] for i in range(3)) for e in d['block_entities'])
    res['block_entity_positions'] = expected_be == actual_be
    res['block_entity_types'] = sorted({e['type'] for e in d['block_entities']}) == ['minecraft:chest']
    res['block_entities_empty_no_loot'] = all(e.get('empty') and 'LootTable' not in e['nbt'] for e in d['block_entities'])
    ui = tuple(rot((25, -26, 26), r))
    ui_state = at(ui)
    res['treasury_ui_anchor_single_chest'] = ui_state['id'] == 'minecraft:chest' and ui_state['properties']['type'] == 'single'
    g = d['ground']; reg = d['registration']; meta = d['metadata']
    world = lambda p: [g[i] + rot(p, r)[i] for i in range(3)]
    res['metadata_matches_world'] = (meta['primary_left'] == world((-21, -20, 33)) and meta['primary_right'] == world((-20, -20, 33))
                                     and meta['guardian'] == world((-21, -22, 26)) and meta['treasury_ui_anchor'] == world((25, -26, 26)))
    res['registration'] = all(reg[k] for k in ('canonical_id_matches', 'primary_matches_metadata', 'spawn_matches_metadata', 'idempotent')) and not reg['secondary_present']
    # 7. Lighting placement on actual blocks.
    torches, unsupported = 0, []
    for i, s in enumerate(act_pal):
        if s['id'] != 'minecraft:wall_torch': continue
        dx, dz = {'east': (-1, 0), 'west': (1, 0), 'south': (0, -1), 'north': (0, 1)}[s['properties']['facing']]
        for q in np.argwhere(act == i):
            torches += 1; sup = (q[0] + dx, q[1], q[2] + dz)
            if not solid[sup]: unsupported.append([int(q[j]) + LO[j] for j in range(3)])
    res['wall_torches'] = torches; res['unsupported_torches'] = unsupported
    # 8. Shell, boundary and exposure on actual blocks.
    under = solid[:, :32, :]
    res['underground_boundary_sealed'] = bool(under[0].all() and under[-1].all() and under[:, :, 0].all() and under[:, :, -1].all() and solid[:, 0, :].all())
    nonair = np.argwhere(~is_air); lo = nonair.min(0) + np.array(LO); hi = nonair.max(0) + np.array(LO)
    res['non_air_extent'] = [int(lo[0]), int(hi[0]), int(lo[1]), int(hi[1]), int(lo[2]), int(hi[2])]
    passable = ~solid
    shell = [(x, y, z) for x in (0, SHAPE[0] - 1) for y in range(SHAPE[1]) for z in range(SHAPE[2])] + \
            [(x, y, z) for z in (0, SHAPE[2] - 1) for y in range(SHAPE[1]) for x in range(SHAPE[0])] + \
            [(x, SHAPE[1] - 1, z) for x in range(SHAPE[0]) for z in range(SHAPE[2])]
    outside = flood(passable, shell)
    wr = np.rot90(written, r, axes=(0, 2))
    interior_air = passable & wr
    res['exterior_connected_interior_cells'] = int((outside & interior_air).sum())
    sealed = interior_air & ~outside
    res['sealed_pockets_authored'] = components(np.rot90(sealed, -r, axes=(0, 2)))  # bounds in authored coordinates
    pa = np.rot90(passable, -r, axes=(0, 2)); outside_air = pa & ~written
    near = np.zeros(SHAPE, bool)
    for ax in range(3):
        for sh in (1, -1):
            near |= np.roll(outside_air, sh, axis=ax) & ~_edge(ax, sh)
    res['shell_openings_authored'] = components(pa & written & near)
    res['secret_cavity_sealed_closed'] = not outside[idx(target)] and not outside[idx(crypt)]
    res['closed_reachable'] = len(closed_reach); res['open_reachable'] = len(open_reach)
    # Authored-frame copy for cross-route/rotation comparisons.
    res['_authored'] = np.rot90(act, -r, axes=(0, 2)); res['_pal'] = [G.rotated_state(s, (4 - r) % 4) for s in act_pal]
    return res


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--dumps', type=Path, required=True); ap.add_argument('--out', type=Path, required=True)
    args = ap.parse_args()
    bp = json.loads(X.BLUEPRINT.read_bytes()); d1 = X.read_nbt(X.D1.read_bytes())
    designs = {r: design(r, bp, d1) for r in X.ROUTES}
    recipes = {}
    for r in X.ROUTES:
        _, p2, p4 = G.recipe(r); recipes[r] = (p2, p4)
    rows = []
    for o in ('NORTH', 'EAST', 'SOUTH', 'WEST'):
        for route in X.ROUTES:
            f = args.dumps / ('case-%s-%s.json.gz' % (o.lower(), route.lower()))
            rows.append(check_case(load(f), bp, d1, designs, recipes)); print('checked', o, route, flush=True)
    # Cross-case: fixed treasury/crypt/D1 identical across routes; each route identical across rotations (authored frame).
    def region(row, b):
        a = row['_authored'][G.box_slice(b)]; pal = row['_pal']
        return [G.canonical(pal[int(v)]) for v in a.ravel()]
    fixed = {'D1': [-40, -2, -24, -5, 2, 40], 'TREASURY': [6, 30, -31, -20, 16, 36], 'CRYPT': [8, 22, -31, -23, -7, 5]}
    cross = {}
    for row in rows:
        other = next(x for x in rows if x['case'].split('/')[0] == row['case'].split('/')[0] and x is not row)
        row['fixed_chambers_identical_across_routes'] = all(region(row, b) == region(other, b) for b in fixed.values())
        same_route_south = next(x for x in rows if x['case'] == 'SOUTH/' + row['case'].split('/')[1])
        row['authored_frame_identical_to_south'] = all(region(row, b) == region(same_route_south, b) for b in [[-40, 40, -32, 39, -40, 40]])
    pockets = {row['case']: [row['sealed_pockets_authored'], row['shell_openings_authored']] for row in rows}
    # Negative controls: the same checks must reject a deliberately corrupted actual dump.
    controls = {}
    base = load(args.dumps / 'case-east-west.json.gz')
    def mutated(fn):
        d = dict(base); d['grid'] = base['grid'].copy(); d['palette'] = [dict(x) for x in base['palette']]; fn(d); return check_case(d, bp, d1, designs, recipes)
    def set_state(d, authored, state):
        d['palette'].append(state); d['grid'][idx(rot(authored, ORIENT_R['EAST']))] = len(d['palette']) - 1
    c = mutated(lambda d: set_state(d, (30, 20, 0), {'id': 'minecraft:stone'}))
    controls['shell_block_replaced_detected'] = c['full_grid_matches_rotated_design'] is False
    c = mutated(lambda d: set_state(d, (-21, -21, 20), {'id': 'minecraft:sandstone'}))
    controls['d1_cell_changed_detected'] = c['d1_changed_cells_exactly_r1_01'] is False and c['full_grid_matches_rotated_design'] is False
    c = mutated(lambda d: set_state(d, (-21, -19, 26), {'id': 'minecraft:sandstone'}))
    controls['guardian_obstruction_detected'] = c['guardian_clearance_3x3x5'] is False
    c = mutated(lambda d: [set_state(d, (x, -29, 30), {'id': 'minecraft:sandstone'}) for x in range(-15, -2)])
    controls['blocked_west_route_detected'] = c['secret_open_treasury'] is False or c['three_wide_landing_to_vault'] is False
    def rotate_torch(d):
        for i, st in enumerate(d['palette']):
            if st['id'] == 'minecraft:wall_torch': st['properties'] = {'facing': {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}[st['properties']['facing']]}
    c = mutated(rotate_torch)
    controls['wrong_torch_rotation_detected'] = c['full_grid_matches_rotated_design'] is False and bool(c['unsupported_torches'])
    print('negative controls', controls, flush=True)
    for row in rows: del row['_authored'], row['_pal']
    if not all(len({json.dumps(v) for k, v in pockets.items() if k.endswith('/' + rt)}) == 1 for rt in X.ROUTES):
        rows[0]['sealed_pockets_rotation_invariant'] = False
    bool_fail = lambda row: [k for k, v in row.items() if v is False] + (['unreachable_room_goals'] if row['unreachable_room_goals'] else []) + \
        (['p2_stair_clearance_failures'] if row['p2_stair_clearance_failures'] else []) + (['unsupported_torches'] if row['unsupported_torches'] else [])
    failures = {row['case']: bool_fail(row) for row in rows if bool_fail(row)}
    report = {'status': 'FAIL' if failures else 'PASS',
              'meaning': 'Checks executed on block data read back from real Minecraft 26.3 placements of the exported templates',
              'failures': failures, 'cases': rows,
              'negative_controls': controls,
              'sealed_pockets_identical_per_route_across_rotations': all(
                  len({json.dumps(v) for k, v in pockets.items() if k.endswith('/' + rt)}) == 1 for rt in X.ROUTES)}
    if not all(controls.values()): report['status'] = 'FAIL'; failures['negative_controls'] = [k for k, v in controls.items() if not v]
    args.out.parent.mkdir(parents=True, exist_ok=True); args.out.write_text(json.dumps(report, indent=1, default=lambda o: o.tolist() if hasattr(o, 'tolist') else str(o)) + '\n')
    print(json.dumps({'status': report['status'], 'failures': failures,
                      'sealed_pockets_identical_per_route': report['sealed_pockets_identical_per_route_across_rotations']}, indent=1))
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())

#!/usr/bin/env python3
"""ST-01 P8-C2-B: verify one seed's natural-worldgen probe results.

    python3 tools/st01-p8c2b/verify_results.py build/st01-p8c2b/results/<seed>

Checks, from data read back from real Minecraft 26.3 servers:
  * every phase ran without error; the structure/structure set are registered with the locked values;
  * fresh-world phases (scan / reverse / random order) predict every potential anchor exactly as production decides it
    and find the identical set of starts with identical anchor, orientation and route;
  * per start, every phase passes its in-game checks (zero structure-owned cell mismatches, no leftover terrain in
    structure void, no fluid in the box, full Desert, MODERATE limits, entrance, completion only after the last chunk,
    registration, lifecycle) and the structure-owned hash AND the whole completion-box hash are identical across the
    normal, reverse and random generation orders and the restart;
  * the P8-C2-A per-case verifier (tools/st01-p8c2a/verify_placement.py: full-grid equality with the rotated approved
    design, D1 preservation and the 12 seam cells, traversal and room reachability, secret concealment, route
    open/filled, Guardian clearance, PRIMARY handedness, block entities, torches, sealed underground boundary) passes on
    every naturally generated start of every phase;
  * rejected candidates leave no start, reference, record or pyramid block.
Writes <dir>/verification.json and exits 1 on any failure.
"""
import gzip, importlib.util, json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('vp', ROOT / 'tools/st01-p8c2a/verify_placement.py')
VP = importlib.util.module_from_spec(spec); spec.loader.exec_module(VP)

FRESH = ('scan', 'reverse', 'random')
PHASES = FRESH + ('reload',)


def main():
    d = Path(sys.argv[1])
    res = {p: json.loads((d / f'{p}.json').read_text()) for p in PHASES if (d / f'{p}.json').exists()}
    failures = []
    for p in PHASES:
        if p not in res: failures.append(f'{p}: missing'); continue
        if 'error' in res[p]: failures.append(f'{p}: error {res[p]["error"]}')
    if failures: return finish(d, res, failures, {})
    reg = res['scan']['registration']
    if not (reg['structure_registered'] and reg['structure_class_ok'] and reg['piece_types_registered'] and reg.get('placement_matches_locked_values')
            and reg['structure_sets_containing'] == ['moreminecrafts:st01_pyramid']):
        failures.append('registration audit')
    for p in FRESH:
        st = res[p]['anchor_statistics']
        if st['prediction_vs_actual_mismatches']: failures.append(f'{p}: {st["prediction_vs_actual_mismatches"]} prediction mismatches')
    keys = {p: sorted(res[p]['starts']) for p in PHASES}
    if len({json.dumps(v) for v in keys.values()}) != 1: failures.append(f'start sets differ between phases: {keys}')
    ident = ('anchor', 'orientation', 'route', 'canonical_id', 'owned_hash', 'box_hash')
    per_start = {}
    for k in keys['scan']:
        row = {}
        for p in PHASES:
            s = res[p]['starts'].get(k)
            if s is None: continue
            if not s.get('all_checks_pass'): failures.append(f'{p} {k}: in-game checks failed')
        for f in ident:
            vals = {p: json.dumps(res[p]['starts'][k].get(f), sort_keys=True) for p in PHASES if k in res[p]['starts']}
            row[f + '_identical_across_phases'] = len(set(vals.values())) == 1
            if not row[f + '_identical_across_phases']: failures.append(f'{k}: {f} differs across phases {vals}')
        row['registered_every_phase'] = all(res[p]['starts'][k].get('registered') for p in PHASES if k in res[p]['starts'])
        lc = res['scan']['starts'][k].get('lifecycle') or {}
        rl = res['reload']['starts'].get(k, {}).get('lifecycle') or {}
        row['reward_state_persisted_exactly'] = bool(lc) and lc.get('royal_data_json') is not None and _instance_rows(lc.get('royal_data_json'), res['scan']['starts'][k]['canonical_id']) == _instance_rows(rl.get('royal_data_json'), res['scan']['starts'][k]['canonical_id'])
        if not row['reward_state_persisted_exactly']: failures.append(f'{k}: reward state changed across restart')
        per_start[k] = row
    # P8-C2-A per-case structural verification on the naturally generated blocks
    bp = json.loads(VP.X.BLUEPRINT.read_bytes()); d1 = VP.X.read_nbt(VP.X.D1.read_bytes())
    designs = {r: VP.design(r, bp, d1) for r in VP.X.ROUTES}
    recipes = {}
    for r in VP.X.ROUTES:
        _, p2, p4 = VP.G.recipe(r); recipes[r] = (p2, p4)
    structural = {}
    for p in PHASES:
        for f in sorted((d / 'dumps' / p).glob('start-*.json.gz')):
            row = VP.check_case(VP.load(f), bp, d1, designs, recipes)
            row.pop('_authored', None); row.pop('_pal', None)
            bad = [k for k, v in row.items() if v is False]
            for k in ('unreachable_room_goals', 'p2_stair_clearance_failures', 'unsupported_torches'):
                if row.get(k): bad.append(k)
            structural[f'{p}/{f.name}'] = {'case': row['case'], 'failures': bad, 'non_air_extent': row.get('non_air_extent'),
                                           'sealed_pockets_authored': row.get('sealed_pockets_authored'),
                                           'shell_openings_authored': row.get('shell_openings_authored')}
            if bad: failures.append(f'verify_placement {p}/{f.name}: {bad}')
    for t in res['scan'].get('rejected_candidate_audit_targets', []):
        if not t.get('nothing_left'): failures.append(f'rejected candidate {t.get("chunk")} left something: {t}')
    return finish(d, res, failures, {'per_start': per_start, 'structural': structural})


def _instance_rows(js, iid):
    if js is None: return None
    data = json.loads(js)
    inst = [i for i in data.get('instances', []) if i.get('id') == iid]
    chests = [c for c in data.get('chests', []) if c.get('instance') == iid]
    return json.dumps([inst, chests], sort_keys=True)


def finish(d, res, failures, extra):
    out = {'status': 'FAIL' if failures else 'PASS', 'failures': failures, **extra,
           'starts': len(res.get('scan', {}).get('starts', {})),
           'statistics': res.get('scan', {}).get('anchor_statistics')}
    (d / 'verification.json').write_text(json.dumps(out, indent=1, default=str) + '\n')
    print(json.dumps({'status': out['status'], 'failures': failures[:40], 'starts': out['starts']}, indent=1))
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())

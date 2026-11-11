#!/usr/bin/env python3
"""ST-01 P8-C2-C: compare real Policy D generation with the counterfactual audit (no hardcoded targets).

    python3 -I tools/st01-p8c2c/compare_policy_d.py <agg-dir> <audit-candidates.csv> <out.json> <out.md>

<agg-dir>/<seed>/{scan,reverse,random,reload,verification}.json come from tools/st01-p8c2b/run_validation.sh.
The audit candidates.csv (docs/design/st01-p8c2b/counterfactual-audit/) supplies the historical per-candidate
observations; its terrain-qualified set, scenario counts and eligible_D set are DERIVED from it, then compared with
what the real server generated. Exits 1 on any disagreement.
"""
import csv, json, math, random, sys
from pathlib import Path

agg, cand_csv, out_json, out_md = (Path(a) for a in sys.argv[1:5])
audit = list(csv.DictReader(open(cand_csv)))
tq = [r for r in audit if r['terrain_ok'] == 'true' and r['footprint_desert'] == 'true' and r['stub_cell_desert'] == 'true']
key = lambda r: (int(r['seed']), int(r['chunk_x']), int(r['chunk_z']))
audit_D = {key(r) for r in audit if r['eligible_D'] == 'true'}
audit_A = {key(r) for r in audit if r['eligible_A'] == 'true'}
audit_B = sum(r['eligible_B'] == 'true' for r in audit); audit_C = sum(r['eligible_C'] == 'true' for r in audit)
by_key = {key(r): r for r in audit}

fails = []
seeds, per_seed, starts_all = [], {}, {}
for d in sorted(p for p in agg.iterdir() if p.is_dir() and (p / 'scan.json').exists()):
    scan = json.loads((d / 'scan.json').read_text())
    ver = json.loads((d / 'verification.json').read_text()) if (d / 'verification.json').exists() else {'status': 'MISSING'}
    seed = scan['seed']; st = scan['anchor_statistics']; seeds.append(seed)
    per_seed[seed] = {'potential': st['potential_anchors'], 'full_desert': st['full_desert_qualified'], 'terrain_qualified': st['terrain_qualified'],
                      'eligible_predicted': st['eligible_predicted'], 'actual_starts': st['actual_valid_starts'], 'verification': ver['status'],
                      'mismatches': st['prediction_vs_actual_mismatches'], 'area_km2': st['area_km2']}
    for k, s in scan['starts'].items():
        cx, cz = (int(v) for v in k.split(','))
        starts_all[(seed, cx, cz)] = s
    per_seed[seed]['completed'] = sum(1 for s in scan['starts'].values() if s['all_checks_pass'])
    if ver['status'] != 'PASS': fails.append(f'seed {seed}: verification {ver["status"]}')
    if st['prediction_vs_actual_mismatches']: fails.append(f'seed {seed}: prediction mismatches')

actual = set(starts_all)
if actual != audit_D: fails.append(f'actual starts differ from audit eligible_D: missing={sorted(audit_D - actual)} extra={sorted(actual - audit_D)}')
if len(tq) != len(audit_D): fails.append('audit terrain-qualified != audit D')
if sum(v['terrain_qualified'] for v in per_seed.values()) != len(tq): fails.append('terrain-qualified count differs from audit')
if sum(v['eligible_predicted'] for v in per_seed.values()) != len(tq): fails.append('Policy D eligible != terrain-qualified')

def flag(s, k): return bool(s.get(k))
rows = []
for k, s in sorted(starts_all.items()):
    r = by_key.get(k, {})
    lc = s.get('lifecycle') or {}
    rows.append({'seed': k[0], 'chunk': f'{k[1]},{k[2]}', 'orientation': s['orientation'], 'route': s['route'], 'ground': [s['anchor']['x'], s['anchor']['y'], s['anchor']['z']],
                 'audit_entrance_ok': r.get('entrance_ok'), 'audit_fluid_ok': r.get('fluid_ok'), 'audit_entrance_max_obstruction': r.get('entrance_max_obstruction'),
                 'audit_fluid_blocks_total': r.get('fluid_blocks_total'),
                 'was_policy_A_start': k in audit_A,
                 'entrance_buried': s.get('entrance_buried'), 'entrance_approach_max_rise': s.get('entrance_approach_max_rise'),
                 'entrance_opening_clear': s.get('entrance_opening_clear'), 'approach_columns_air_at_noise_surface': s.get('entrance_approach_columns_air_at_noise_surface'), 'approach_row_outside_box': s.get('entrance_approach_row_outside_structure_box'),
                 'fluid_blocks_in_completion_box': s.get('fluid_blocks_in_completion_box'), 'fluid_in_void_cells': s.get('fluid_blocks_in_structure_void_cells'),
                 'fluid_shell': s.get('fluid_blocks_in_1_block_shell_outside_underground'),
                 'owned_cell_mismatches': s.get('owned_cell_mismatches'), 'void_cells_non_air': s.get('void_cells_non_air'),
                 'registered': s.get('registered'), 'lifecycle_pass': lc.get('pass'), 'all_checks_pass': s.get('all_checks_pass')})
n = len(rows)
cnt = lambda f: sum(1 for r in rows if f(r))
summary = {
    'seeds': len(seeds), 'area_km2': sum(v['area_km2'] for v in per_seed.values()),
    'audit_derived': {'terrain_qualified': len(tq), 'eligible_A': len(audit_A), 'eligible_B': audit_B, 'eligible_C': audit_C, 'eligible_D': len(audit_D)},
    'eligible_candidates': sum(v['eligible_predicted'] for v in per_seed.values()), 'actual_structure_starts': n,
    'fully_generated_pyramids': cnt(lambda r: r['all_checks_pass'] is not None),
    'complete_assembly_integrity': cnt(lambda r: r['owned_cell_mismatches'] == 0 and r['void_cells_non_air'] == 0),
    'registered_royal_chests': cnt(lambda r: r['registered']), 'guardian_lifecycle_pass': cnt(lambda r: r['lifecycle_pass']),
    'all_checks_pass': cnt(lambda r: r['all_checks_pass']),
    'buried_entrance_starts': cnt(lambda r: r['entrance_buried']), 'buried_entrance_all_pass': cnt(lambda r: r['entrance_buried'] and r['all_checks_pass']),
    'previously_fluid_rejected_starts': cnt(lambda r: r['audit_fluid_ok'] == 'false'),
    'previously_fluid_rejected_all_pass': cnt(lambda r: r['audit_fluid_ok'] == 'false' and r['all_checks_pass']),
    'starts_with_fluid_in_completion_box': cnt(lambda r: (r['fluid_blocks_in_completion_box'] or 0) > 0),
    'approach_rows_outside_structure_box': cnt(lambda r: r['approach_row_outside_box']),
    'approach_columns_air_at_noise_surface_total_observation': sum(r['approach_columns_air_at_noise_surface'] or 0 for r in rows),
    'new_starts_vs_policy_A': cnt(lambda r: not r['was_policy_A_start']),
    'routes': {x: cnt(lambda r, x=x: r['route'] == x) for x in ('EAST', 'WEST')},
    'orientations': {x: cnt(lambda r, x=x: r['orientation'] == x) for x in ('NORTH', 'EAST', 'SOUTH', 'WEST')},
}
if summary['registered_royal_chests'] != n: fails.append('not every start registered a royal chest')
if summary['all_checks_pass'] != n: fails.append('not every start passed all in-game checks')
if summary['approach_rows_outside_structure_box'] != n: fails.append('an approach row lies inside a structure box')
if len(audit_A) and not audit_A <= actual: fails.append('a Policy A start disappeared')

area = summary['area_km2']
def pc(mu, lo, hi): return lo * 100 / area, hi * 100 / area
def cdf(k, mu): return sum(math.exp(-mu + i * math.log(mu) - math.lgamma(i + 1)) for i in range(k + 1)) if mu > 0 else 1.0
def bis(pred, hi):
    lo = 0.0
    for _ in range(100):
        m = (lo + hi) / 2
        lo, hi = (lo, m) if pred(m) else (m, hi)
    return (lo + hi) / 2
k = n
glo = 0.0 if k == 0 else bis(lambda mu: 1 - cdf(k - 1, mu) >= 0.025, k + 60.0)
ghi = bis(lambda mu: cdf(k, mu) <= 0.025, k + 60 + 10 * math.sqrt(k + 1))
counts = [sum(1 for kk in actual if kk[0] == s) for s in seeds]
rng = random.Random(20261111); boots = []
for _ in range(20000):
    boots.append(sum(counts[rng.randrange(len(counts))] for _ in counts))
boots.sort(); per = area / len(seeds)
summary['density_per_100km2'] = k * 100 / area
summary['garwood_95_per_100km2'] = [glo * 100 / area, ghi * 100 / area]
summary['seed_cluster_bootstrap_95_per_100km2'] = [boots[int(0.025 * 20000)] * 100 / area, boots[int(0.975 * 20000) - 1] * 100 / area]
summary['per_seed_actual'] = dict(zip(map(str, seeds), counts)); summary['seeds_with_zero'] = sum(c == 0 for c in counts)
summary['mean_per_seed'] = k / len(seeds); summary['median_per_seed'] = sorted(counts)[len(counts) // 2] if len(counts) % 2 else sum(sorted(counts)[len(counts) // 2 - 1:len(counts) // 2 + 1]) / 2
summary['failures'] = fails; summary['status'] = 'PASS' if not fails else 'FAIL'
out_json.write_text(json.dumps({'summary': summary, 'per_seed': per_seed, 'starts': rows}, indent=1, default=str) + '\n')
md = ['| seed | potential | full desert | terrain | eligible | starts | all checks pass | verification |', '|---|---|---|---|---|---|---|---|']
for s in seeds:
    v = per_seed[s]; md.append(f"| {s} | {v['potential']} | {v['full_desert']} | {v['terrain_qualified']} | {v['eligible_predicted']} | {v['actual_starts']} | {v['completed']} | {v['verification']} |")
md += ['', '```json', json.dumps(summary, indent=1, default=str), '```']
out_md.write_text('\n'.join(md) + '\n')
print(json.dumps(summary, indent=1, default=str))
sys.exit(1 if fails else 0)

#!/usr/bin/env python3
"""Render the report's machine-derived tables (markdown) from analysis/summary.json + candidates.json. No numbers are typed by hand.

    python3 -I tools/st01-p8c2b-audit/make_tables.py <analysis-dir> > tables.md
"""
import json, sys
from pathlib import Path
a = Path(sys.argv[1]); s = json.loads((a / 'summary.json').read_text()); C = json.loads((a / 'candidates.json').read_text())
def f(x, n=3): return 'n/a' if x is None else ('%.*f' % (n, x))
out = []
P = out.append
fu = s['funnel']; c = s['contingency_terrain_qualified']; st = s['scenario_stats']
P('### Funnel (reproduced)\n\n| Stage | Count |\n|---|---|')
for k in ('potential_anchors', 'full_desert_footprint', 'terrain_qualified', 'entrance_qualified_sequential', 'fluid_qualified_after_entrance', 'actual_valid_starts_in_real_world'):
    P('| %s | %s |' % (k, fu[k]))
P('\nProduction verdict counts (unmodified `St01Candidate.decide`): `%s`\n' % json.dumps(s['production_verdict_counts'], sort_keys=True))
P('### 2x2 contingency (terrain-qualified, n = %d)\n\n| | Fluid PASS | Fluid FAIL | Row total |\n|---|---|---|---|' % c['total'])
P('| **Entrance PASS** | n11 = %d | n10 = %d | %d |' % (c['n11_ent_pass_fluid_pass'], c['n10_ent_pass_fluid_fail'], c['n11_ent_pass_fluid_pass'] + c['n10_ent_pass_fluid_fail']))
P('| **Entrance FAIL** | n01 = %d | n00 = %d | %d |' % (c['n01_ent_fail_fluid_pass'], c['n00_ent_fail_fluid_fail'], c['n01_ent_fail_fluid_pass'] + c['n00_ent_fail_fluid_fail']))
P('| Column total | %d | %d | %d |\n' % (c['n11_ent_pass_fluid_pass'] + c['n01_ent_fail_fluid_pass'], c['n10_ent_pass_fluid_fail'] + c['n00_ent_fail_fluid_fail'], c['total']))
i = s['interaction']
P('| Interaction measure | Value |\n|---|---|')
for k, v in i.items(): P('| %s | %s |' % (k.replace('|', '\\|'), f(v, 4)))
P('\n### Scenarios (area %.3f km2 = %d seeds x %.4f km2; label: COUNTERFACTUAL_ELIGIBLE except A)\n' % (s['area_km2_total'], len(s['seeds']), s['area_km2_per_seed']))
P('| Scenario | Entrance | Fluid | Eligible | Per 100 km2 | Poisson 95% | Cluster-bootstrap 95% | vs A (x) | vs A (abs) | Seeds with 0 | Mean/seed | Median/seed |\n|---|---|---|---|---|---|---|---|---|---|---|---|')
names = {'A': ('ON', 'ON'), 'B': ('OFF', 'ON'), 'C': ('ON', 'OFF'), 'D': ('OFF', 'OFF')}
for k in 'ABCD':
    x = st[k]
    P('| %s | %s | %s | %d | %s | %s-%s | %s-%s | %s | %s | %d | %s | %s |' % (k, *names[k], x['eligible_candidates'], f(x['density_per_100km2']), f(x['poisson_ci95_per_100km2'][0]), f(x['poisson_ci95_per_100km2'][1]),
      f(x['cluster_bootstrap_ci95_per_100km2'][0]), f(x['cluster_bootstrap_ci95_per_100km2'][1]), f(x['relative_to_A'], 2), f(x['absolute_difference_vs_A_per_100km2']), x['seeds_with_zero'], f(x['mean_per_seed'], 2), f(x['median_per_seed'], 1)))
P('\nRatio vs A, paired seed-cluster bootstrap 95%%: %s\n' % json.dumps(s['ratio_bootstrap_vs_A']))
P('### Per seed\n\n| Seed | Anchors | Full Desert | Terrain-qualified | A | B | C | D | Actual starts |\n|---|---|---|---|---|---|---|---|---|')
for sd in s['seeds']:
    r = s['per_seed'][str(sd)]
    P('| %d | %d | %d | %d | %d | %d | %d | %d | %d |' % (sd, r['potential_anchors'], r['full_desert'], r['terrain_qualified'], r['scenario_A'], r['scenario_B'], r['scenario_C'], r['scenario_D'], r['actual_valid_starts']))
P('\n### The terrain-qualified candidates (all %d)\n\n| Seed | Chunk | Anchor (x,y,z) | Orient | Route | Max entrance obstruction | Fluid blocks (in volume / total) | Fluid class | Entrance | Fluid | Overlaps |\n|---|---|---|---|---|---|---|---|---|---|---|' % c['total'])
for r in C:
    if not r['terrain_ok']: continue
    ov = ', '.join(o['structure'].split(':')[-1] for o in (r['vanilla_overlaps'] or [])) or '-'
    P('| %d | %d,%d | %d,%d,%d | %s | %s | %+d | %d / %d | %s | %s | %s | %s |' % (r['seed'], r['chunk_x'], r['chunk_z'], r['anchor_x'], r['foundation_y'], r['anchor_z'], r['orientation'], r['route'], r['entrance_max_obstruction'],
      r['fluid_blocks_in_volume'], r['fluid_blocks_total'], r['fluid_class'], 'pass' if r['entrance_ok'] else 'FAIL', 'pass' if r['fluid_ok'] else 'FAIL', ov))
e = s['entrance']; fl = s['fluid']
P('\n### Entrance failures (n = %d)\n\n```json\n%s\n```\n' % (e['failing'], json.dumps(e, indent=1)))
P('### Fluid failures (n = %d)\n\n```json\n%s\n```\n' % (fl['failing'], json.dumps(fl, indent=1)))
P('### Vanilla overlaps among terrain-qualified candidates\n\n```json\n%s\n```\n' % json.dumps({k: v for k, v in s['overlaps'].items() if k != 'rows'}, indent=1))
P('Integrity: `%s`\n' % json.dumps(s['integrity']))
print('\n'.join(out))

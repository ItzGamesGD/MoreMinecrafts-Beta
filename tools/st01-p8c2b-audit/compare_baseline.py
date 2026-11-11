#!/usr/bin/env python3
"""Compare the audit's recomputed baseline (scenario A) with the values REPORTED by P8-C2-B. Exit 1 on any mismatch.

    python3 -I tools/st01-p8c2b-audit/compare_baseline.py <analysis-dir>
"""
import json, sys
from pathlib import Path
here = Path(__file__).resolve().parent
exp = json.loads((here / 'baseline_reported.json').read_text())
adir = Path(sys.argv[1])
s = json.loads((adir / 'summary.json').read_text())
cands = json.loads((adir / 'candidates.json').read_text())
bad = []
def chk(name, got, want):
    ok = got == want if not isinstance(want, float) else abs(got - want) < 5e-4
    print(('PASS ' if ok else 'MISMATCH ') + name, 'got', got, 'reported', want)
    if not ok: bad.append(name)
f = s['funnel']; pv = s['production_verdict_counts']
chk('potential_anchors', f['potential_anchors'], exp['funnel']['potential_anchors'])
chk('full_desert (production verdict != REJECT_BIOME)', sum(v for k, v in pv.items() if k != 'REJECT_BIOME'), exp['funnel']['full_desert_footprint'])
chk('full_desert footprint flag (audit)', f['full_desert_footprint'], exp['funnel']['full_desert_footprint'])
chk('terrain_qualified', f['terrain_qualified'], exp['funnel']['terrain_qualified'])
chk('entrance_qualified', f['entrance_qualified_sequential'], exp['funnel']['entrance_qualified'])
chk('complete (scenario A)', s['scenario_counts']['A'], exp['funnel']['fluid_safe_complete'])
chk('actual valid starts (real server)', f['actual_valid_starts_in_real_world'], exp['funnel']['fluid_safe_complete'])
for k, v in exp['rejections'].items(): chk(k, pv.get(k, 0), v)
chk('area_km2', s['area_km2_total'], exp['area_km2_total'])
chk('density A per 100 km2 (reported 0.048)', round(s['scenario_stats']['A']['density_per_100km2'], 3), exp['density_per_100km2'])
got = sorted((c['seed'], '%d,%d' % (c['chunk_x'], c['chunk_z']), [c['anchor_x'], c['foundation_y'], c['anchor_z']], c['orientation'], c['route']) for c in cands if c['eligible_A'])
want = sorted((x['seed'], x['chunk'], x['ground'], x['orientation'], x['route']) for x in exp['starts'])
chk('the four production starts (seed, chunk, ground, orientation, route)', got, want)
print('BASELINE_REPRODUCTION', 'FAIL' if bad else 'PASS', bad)
sys.exit(1 if bad else 0)

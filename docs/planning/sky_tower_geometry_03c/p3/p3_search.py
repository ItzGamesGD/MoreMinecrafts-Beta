#!/usr/bin/env python3
"""GEOMETRY-03C-P3 studies: CP-SAT selections (pillar window coverage x balconies) for the ANN model,
plus boundary-sensitivity runs. Writes data/sel_*.json and data/search_log.txt. Requires ortools."""
import json, sys, importlib.util
from pathlib import Path
HERE = Path(__file__).resolve().parent
s = importlib.util.spec_from_file_location('gen', HERE / 'p3_generate.py'); gen = importlib.util.module_from_spec(s); s.loader.exec_module(gen)
D = HERE / 'data'; D.mkdir(exist_ok=True)
log = open(D / 'search_log.txt', 'a')


def run(name, geom_kw, **kw):
    g = gen.Geometry(**geom_kw)
    r = gen.select(g, **kw); r['name'] = name; r['geometry'] = {k: list(v) if isinstance(v, tuple) else v for k, v in geom_kw.items()}
    r['pillar_candidates'] = len(g.pillar_cands); r['band_columns'] = len(g.band); r['ring_columns'] = len(g.ring)
    (D / f'sel_{name}.json').write_text(json.dumps(r, separators=(',', ':')) + '\n')
    line = f"{name} {r['status']} {r['seconds']}s gap<{kw.get('gap_deg')}deg pillars/lap={[len(v) for v in r.get('pillars', {}).values()]} balconies={len(r.get('balconies', []))}"
    print(line, flush=True); log.write(line + '\n'); log.flush()
    return r


def bisect(prefix, geom_kw, lo=10, hi=80, **kw):
    """smallest integer window (degrees) that is feasible; returns per-gap records (proved INFEASIBLE below)."""
    best = None
    while lo < hi:
        mid = (lo + hi) // 2
        r = run(f'{prefix}_gap{mid}', geom_kw, gap_deg=mid, **kw)
        if r['status'] in ('OPTIMAL', 'FEASIBLE'): hi = mid; best = mid
        elif r['status'] == 'INFEASIBLE': lo = mid + 1
        else: return {'prefix': prefix, 'result': 'UNKNOWN at ' + str(mid)}
    if best != lo: run(f'{prefix}_gap{lo}', geom_kw, gap_deg=lo, **kw)
    return {'prefix': prefix, 'min_feasible_gap_deg': lo}


if __name__ == '__main__':
    only = sys.argv[1] if len(sys.argv) > 1 else ''
    summary = []
    G = dict(rb=19.5)
    plan = [
        ('S1_ann_pillars_only_A5strict', G, dict(balconies=False)),
        ('S2_ann_pillars_8balc_contact3_A5strict', G, dict(balconies=True, contact_min=3)),
        ('S3_ann_pillars_8balc_contact3_A5relaxed', G, dict(balconies=True, contact_min=3, a5_strict=False)),
        ('S4_ann_pillars_8balc_contact7_A5strict', G, dict(balconies=True, contact_min=7)),
        ('S5_ann_pillars_8balc_contact7_A5relaxed', G, dict(balconies=True, contact_min=7, a5_strict=False)),
        ('S6_ann_rb20_pillars_only', dict(rb=20.0), dict(balconies=False)),
        ('T1_whole_block_D45_pillars_only', dict(rb=19.5, domain=('whole', 45)), dict(balconies=False, objective=False)),
        ('T2_box45_pillars_only', dict(rb=19.5, domain=('box', 45)), dict(balconies=False, objective=False)),
        ('T3_disc46_pillars_only', dict(rb=19.5, domain=('disc', 46)), dict(balconies=False, objective=False)),
        ('T4_disc47_pillars_only', dict(rb=19.5, domain=('disc', 47)), dict(balconies=False, objective=False)),
    ]
    for name, gk, kw in plan:
        if only and only not in name: continue
        if only == 'Q': continue
        summary.append(bisect(name, gk, **kw))
    if not only or 'Q' in only:
        fixed = {tuple(q) for q in json.loads((D / 'sel_S1_ann_pillars_only_A5strict_gap27.json').read_text())['pillars']['0']}
        for name, kw in (('Q1_sequential_S1pillars_then_8balc_contact3', dict(contact_min=3)),
                         ('Q2_sequential_S1pillars_then_8balc_contact1', dict(contact_min=1))):
            r = run(name, G, gap_deg=360, balconies=True, fixed_pillars=fixed, **kw)
            summary.append({'prefix': name, 'status': r['status']})
        r = run('Q3_balconies_only_contact7_any_pillars', G, gap_deg=360, balconies=True, contact_min=7)
        summary.append({'prefix': 'Q3_balconies_only_contact7_any_pillars', 'status': r['status']})
        r = run('Q4_balconies_only_contact3', G, gap_deg=360, balconies=True, contact_min=3)
        summary.append({'prefix': 'Q4_balconies_only_contact3', 'status': r['status']})
    (D / f'bisect_summary{"_" + only if only else ""}.json').write_text(json.dumps(summary, indent=1) + '\n')
    print(summary)

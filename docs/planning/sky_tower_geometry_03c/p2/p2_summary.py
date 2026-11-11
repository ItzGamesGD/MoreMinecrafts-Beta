#!/usr/bin/env python3
"""Aggregate data/study_*.json into data/summary.json and print a markdown table (stdlib)."""
import glob, json
from pathlib import Path
D = Path(__file__).resolve().parent / 'data'
rows = []
for f in sorted(D.glob('study_*.json')):
    r = json.loads(f.read_text()); o = r['options']
    rows.append({'study': r['name'], 'status': r['status'], 'seconds': r['seconds'], 'objective': r.get('objective'), 'bound': r.get('bound'),
                 'rise_stations': r.get('eligible_stations_in_solution'), 'pillars': len(r.get('pillars', [])), 'balconies': len(r.get('balconies', [])),
                 'loop_stations': len(r.get('loop', [])), 'options': o})
(D / 'summary.json').write_text(json.dumps(rows, ensure_ascii=False, indent=1) + '\n')
print('| study | status | optimum/bound | rise stations | pillars | balconies |\n|---|---|---|---|---|---|')
for x in rows:
    ob = '' if x['objective'] is None else f"{x['objective']:.0f}/{x['bound']:.0f}"
    print(f"| {x['study']} | {x['status']} | {ob} | {x['rise_stations'] if x['rise_stations'] is not None else ''} | {x['pillars'] or ''} | {x['balconies'] or ''} |")

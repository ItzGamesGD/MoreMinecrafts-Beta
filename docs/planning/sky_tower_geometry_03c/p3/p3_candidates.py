#!/usr/bin/env python3
"""Build every P3 candidate from its selection (generator side), then run the INDEPENDENT verifier in a separate
process, render SVGs from the exported CSV (P2 renderer, unchanged), and write data/results_summary.json and
data/collisions.csv.  usage: python3 -I p3_candidates.py"""
import csv, importlib.util, json, subprocess, sys
from pathlib import Path
HERE = Path(__file__).resolve().parent; D = HERE / 'data'; C = D / 'candidates'
CANDS = [  # name, selection, rise, landing cells
    ('K1_pillars_slab', 'S1_ann_pillars_only_A5strict_gap27', 'slab', 9),
    ('K2_8balc_A5relaxed_slab_land9', 'S3_ann_pillars_8balc_contact3_A5relaxed_gap34', 'slab', 9),
    ('K2_8balc_A5relaxed_slab_land3', 'S3_ann_pillars_8balc_contact3_A5relaxed_gap34', 'slab', 3),
    ('K2_8balc_A5relaxed_slab_land0', 'S3_ann_pillars_8balc_contact3_A5relaxed_gap34', 'slab', 0),
    ('K3_pillars_stairblocks', 'S1_ann_pillars_only_A5strict_gap27', 'stair', 9),
    ('K4_8balc_A5strict_slab_land9', 'S2_ann_pillars_8balc_contact3_A5strict_gap53', 'slab', 9),
]
s = importlib.util.spec_from_file_location('r2', HERE.parent / 'p2' / 'p2_render.py'); r2 = importlib.util.module_from_spec(s); s.loader.exec_module(r2)
summary, coll = {}, []
for name, sel, rise, land in CANDS:
    out = C / name
    subprocess.run([sys.executable, '-I', str(HERE / 'p3_generate.py'), '--selection', str(D / f'sel_{sel}.json'), '--out', str(out),
                    '--rise', rise, '--landing', str(land)], check=True, capture_output=True)
    subprocess.run([sys.executable, '-I', str(HERE / 'p3_verify.py'), str(out), str(C / f'verify_{name}.json')], check=True, capture_output=True)
    v = json.loads((C / f'verify_{name}.json').read_text()); o = json.loads((out / 'objects_C1.json').read_text())
    c1 = v['C1']['checks']
    sp = c1['V7_pillar_spacing']['per_lap']
    summary[name] = {'selection': sel, 'rise': rise, 'landing_cells': land,
                     'C1': v['C1']['summary'], 'C2': v['C2']['summary'], 'C1_equals_C2': v['C1']['summary'] == v['C2']['summary'],
                     'pillars_per_lap': {k: x['count'] for k, x in sp.items()},
                     'pillar_arc_max_on_r17.5': max(x['longest_pillar_free_arc'] for x in sp.values()),
                     'pillar_arc_median_by_lap': {k: x['arc_on_walk_centreline_r17.5']['median'] for k, x in sp.items()},
                     'pillar_arc_min': min(x['arc_on_walk_centreline_r17.5']['min'] for x in sp.values()),
                     'balconies': c1['V6_balconies']['count'], 'balconies_flat': [b['flat_landing'] for b in c1['V6_balconies']['items']],
                     'arch_pairs_connected': f"{c1['V8_arch_continuity']['connected']}/{c1['V8_arch_continuity']['pairs']}",
                     'outside_D45_center': c1['V1_boundary']['normal_outside_D45_center'],
                     'outside_D45_whole_block_alt': c1['V1_boundary']['normal_outside_D45_whole_block_alt'],
                     'balcony_outside_D51_center': c1['V1_boundary']['balcony_outside_D51_center'],
                     'lanes_failed': [(l['lap'], l['lane']) for l in c1['V4_lane_width4']['lanes'] if not l['pass']],
                     'min_headroom': c1['V5_headroom']['min_clearance'], 'reach': c1['V3_reach']['reached'],
                     'stair_voxels': sum(1 for _ in csv.DictReader((out / 'blocks_C1.csv').open()) if _['group'] == 'stairs'),
                     'arch_max_opening': max([a['opening'] for a in o['arches']] or [0]), 'arch_footprint_failures': len(o['arch_failures'])}
    for conv in ('C1', 'C2'):
        ch = v[conv]['checks']
        for a, b, x, y, z in ch['V2_overlaps']['collisions']: coll.append([name, conv, 'voxel_collision', f'{a}&{b}', x, y, z])
        for e in ch['V9_fall_edges']['sample']: coll.append([name, conv, 'unguarded_edge', 'stairs', e[0], e[1], e[2]])
        for l in ch['V4_lane_width4']['lanes']:
            if not l['pass']: coll.append([name, conv, 'lane_width_fail', f"lap{l['lap']}_lane{l['lane']}", '', '', ''])
    if name in ('K1_pillars_slab', 'K2_8balc_A5relaxed_slab_land3', 'K4_8balc_A5strict_slab_land9'):
        lab = name.split('_')[0]; svg = D / 'svg'; svg.mkdir(exist_ok=True); g = r2.load(out)
        r2.plan(g, f'{svg}/{lab}_plan_all_layers.svg', f'{name}: top view, all layers')
        for nm, a, b in (('F1', 10, 20), ('F2', 21, 32), ('F3', 33, 46), ('F4', 47, 63)):
            r2.plan(g, f'{svg}/{lab}_plan_{nm}.svg', f'{name}: y {a}-{b}', a, b)
        r2.section(g, f'{svg}/{lab}_section_XY.svg', f'{name}: X/Y section', 'XY'); r2.section(g, f'{svg}/{lab}_section_ZY.svg', f'{name}: Z/Y section', 'ZY')
        r2.iso(g, f'{svg}/{lab}_iso.svg', f'{name}: 3D projection')
    print(name, summary[name]['C1'])
(D / 'results_summary.json').write_text(json.dumps(summary, indent=1) + '\n')
with (D / 'collisions.csv').open('w', newline='') as f:
    w = csv.writer(f, lineterminator='\n'); w.writerow(['candidate', 'convention', 'kind', 'members', 'x', 'y', 'z']); w.writerows(coll)

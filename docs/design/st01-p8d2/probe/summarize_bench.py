#!/usr/bin/env python3
"""Summarize P8-D2 benchmark outputs: python3 -I summarize_bench.py <out-dir>  -> markdown on stdout."""
import glob, json, re, statistics, sys
from pathlib import Path
d = Path(sys.argv[1]); rows = []
for f in sorted(d.glob('bench-*.json')):
    m = re.match(r'bench-(\d+)-r(\d+)\.json', f.name)
    j = json.loads(f.read_text())
    if 'error' in j: rows.append((int(m[1]), int(m[2]), None, j['error'])); continue
    rows.append((int(m[1]), int(m[2]), j, None))
print('| seed | radius | origin | state | wall ms (= server thread blocked) | thread CPU ms | peak heap MB | GC ms | ST-01 start evaluations | found | result | valid ST-01 start | ring (regions) | distance blocks |')
print('|---|---|---|---|---|---|---|---|---|---|---|---|---|---|')
cold = {}
for seed, r, j, err in sorted(rows, key=lambda x: (x[1], x[0])):
    if j is None: print(f'| {seed} | {r} | - | ERROR {err} |'); continue
    for c in j['calls']:
        print(f"| {seed} | {r} | {c['origin']} | {c['state']} | {c['wall_ms']:.1f} | {c['thread_cpu_ms']:.1f} | {c['peak_heap_mb_during_call']} | {c['gc_ms_delta']} | {c['st01_start_evaluations_delta']} | {c['found']} | {c.get('result','')} | {c.get('result_is_valid_st01_start','')} | {c.get('result_ring_regions','')} | {round(c['result_distance_blocks']) if c.get('result_distance_blocks') is not None else ''} |")
        if c['state'] == 'cold': cold.setdefault(r, []).append(c)
print('\n### Cold first call per radius (origin 0,0, fresh world), across seeds\n')
print('| radius | n | median ms | p90 ms | max ms | min ms | found / n | median ST-01 evaluations |')
print('|---|---|---|---|---|---|---|---|')
for r in sorted(cold):
    w = sorted(c['wall_ms'] for c in cold[r]); n = len(w)
    p90 = w[min(n - 1, int(0.9 * n))]
    print(f"| {r} | {n} | {statistics.median(w):.0f} | {p90:.0f} | {w[-1]:.0f} | {w[0]:.0f} | {sum(c['found'] for c in cold[r])}/{n} | {statistics.median(c['st01_start_evaluations_delta'] for c in cold[r]):.0f} |")
warm = [c['wall_ms'] for _, _, j, _ in rows if j for c in j['calls'] if c['state'] == 'warm-repeat']
if warm: print(f"\nWarm repeats (same origin, immediately after): n={len(warm)}, median {statistics.median(warm):.3f} ms, max {max(warm):.3f} ms")

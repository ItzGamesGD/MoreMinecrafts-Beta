#!/usr/bin/env python3
"""Repeat-run determinism: every <seed>/observations-1.json must equal observations-2.json (ignoring only the wall-clock field scan_ms).

    python3 -I tools/st01-p8c2b-audit/determinism.py <results-dir>
"""
import json, sys
from pathlib import Path
bad, n = [], 0
for d in sorted(p for p in Path(sys.argv[1]).iterdir() if p.is_dir()):
    runs = sorted(d.glob('observations-*.json'))
    if len(runs) < 2: continue
    docs = []
    for r in runs:
        o = json.loads(r.read_text()); o.pop('scan_ms', None); docs.append(o)
    n += 1
    same = all(x == docs[0] for x in docs[1:])
    print(('PASS ' if same else 'FAIL ') + d.name, len(runs), 'runs')
    if not same: bad.append(d.name)
print('DETERMINISM', 'FAIL' if bad or n == 0 else 'PASS', 'seeds_compared=%d' % n, bad)
sys.exit(1 if bad or n == 0 else 0)

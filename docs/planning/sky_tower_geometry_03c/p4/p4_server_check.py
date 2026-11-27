#!/usr/bin/env python3
"""Compare the PLACED template in a live vanilla server with section_blocks.csv, block by block, over RCON
(`execute if block`), including every non-listed position of the bounding box (must be air).
usage: RCON_PASSWORD=... python3 -I p4_server_check.py WX WY WZ out.json   (W = world offset added to C1 coords)"""
import csv, importlib.util, json, sys
from pathlib import Path
HERE = Path(__file__).resolve().parent
s = importlib.util.spec_from_file_location('rcon', HERE.parents[3] / 'scripts/arena_validation/rcon.py'); rc = importlib.util.module_from_spec(s); s.loader.exec_module(rc)
W = tuple(map(int, sys.argv[1:4]))
rows = list(csv.DictReader((HERE / 'data' / (sys.argv[5] if len(sys.argv) > 5 else 'section_blocks.csv')).open()))
want = {(int(r['x']), int(r['y']), int(r['z'])): r['block_state'] for r in rows}
xs, ys, zs = zip(*want)
mism, n, i = [], 0, 1
with rc.connect() as sock:
    def ask(cmd):
        global i
        i += 1; rc.send_packet(sock, i, 2, cmd); _, _, resp = rc.read_packet(sock); return resp
    for x in range(min(xs), max(xs) + 1):
        for y in range(min(ys), max(ys) + 1):
            for z in range(min(zs), max(zs) + 1):
                st = want.get((x, y, z), 'minecraft:air'); n += 1
                resp = ask(f'execute if block {x + W[0]} {y + W[1]} {z + W[2]} {st}')
                if 'Test passed' not in resp: mism.append([x, y, z, st, resp[:80]])
res = {'positions_checked': n, 'listed_blocks': len(want), 'mismatches': len(mism), 'sample': mism[:20], 'world_offset': W,
       'status': 'STATIC PASS (live placement matches export)' if not mism else 'FAIL'}
Path(sys.argv[4]).write_text(json.dumps(res, indent=1) + '\n'); print({k: v for k, v in res.items() if k != 'sample'}, mism[:3])

#!/usr/bin/env python3
"""Read-only centered-placement overlap witness. Not a proposed worldgen implementation."""
import ast
from collections import Counter
import hashlib
import json
from pathlib import Path

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
DATA=ROOT/'src/main/resources/data/moreminecrafts'
SOURCE=HERE/'audit_worldgen.py'
tree=ast.parse(SOURCE.read_text())
nodes=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))]
namespace={'__file__':str(SOURCE)}
exec(compile(ast.Module(body=nodes,type_ignores=[]),str(SOURCE),'exec'),namespace)
read,blocks=namespace['read'],namespace['blocks']
approved=DATA/'structure/sky_tower/full_01.nbt'
raw=approved.read_bytes()
assert hashlib.sha256(raw).hexdigest()=='cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27'
tower=blocks(read(approved));void={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'}
result={'approved_sha256':hashlib.sha256(raw).hexdigest(),
        'scope':'center-aligned diagnostic origin (-3,17,-3); no blocks placed; full simulation NOT performed',
        'origin':[-3,17,-3],'styles':{}}
for style in ('snowy','meadow','forest','badlands','desert','rocky'):
    island=blocks(read(DATA/f'structure/sky_islands/{style}/tower.nbt'))
    overlaps=[]
    for (x,y,z),state in sorted(tower.items()):
        pos=(x-3,y+17,z-3);existing=island.get(pos)
        if existing and existing['id'] not in void and state['id'] not in void:
            overlaps.append({'island_pos':pos,'existing':existing['id'],'tower':state['id']})
    def occupied(p):
        b=island.get(p)
        return bool(b and b['id'] not in void)
    # A coordinate witness for one possible apron, not a collision/route approval.
    deck=[(x,16,z) for x in range(41,46) for z in range(19,24)]
    head=[(x,y,z) for x in range(40,46) for z in range(19,24) for y in (17,18)]
    surface=[]
    for x in range(36,47):
        ys=[y for X,y,z in island if X==x and z==21 and occupied((X,y,z))]
        surface.append([x,max(ys) if ys else None])
    result['styles'][style]={'solid_overlap_count':len(overlaps),
        'existing_types':dict(sorted(Counter(o['existing'] for o in overlaps).items())),
        'solid_overlap_blocks':overlaps,'centerline_top_y':surface,
        'witness_deck_cells':deck,'witness_deck_conflicts':[p for p in deck if occupied(p)],
        'witness_head_conflicts':[p for p in head if occupied(p)]}
print(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=1))

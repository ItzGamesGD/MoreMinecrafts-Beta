#!/usr/bin/env python3
"""Bounded same-Y offset screen, diagnostic only; writes JSON to stdout."""
import ast
from pathlib import Path
import hashlib
import json

HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
DATA=ROOT/'src/main/resources/data/moreminecrafts';source=HERE/'audit_worldgen.py'
tree=ast.parse(source.read_text());ns={'__file__':str(source)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(source),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];air={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void','minecraft:jigsaw'}
approved=DATA/'structure/sky_tower/full_01.nbt';sha=hashlib.sha256(approved.read_bytes()).hexdigest()
assert sha=='cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27'
tower={p for p,v in blocks(read(approved)).items() if v['id'] not in air}
styles=('snowy','meadow','forest','badlands','desert','rocky');results={}
for style in styles:
    island={p:v['id'] for p,v in blocks(read(DATA/f'structure/sky_islands/{style}/tower.nbt')).items() if v['id'] not in air}
    occupied=[p for p in island if p[1]>=17];zero=[];supported=[]
    for dx in range(-24,25):
        for dz in range(-24,25):
            if any((x+3-dx,y-17,z+3-dz) in tower for x,y,z in occupied):continue
            supports=[(46+dx,16,z+dz) for z in range(19,24) if (46+dx,16,z+dz) in island]
            row={'offset':[dx,dz],'entrance_nonair':supports}
            zero.append(row)
            if supports:supported.append(row)
    results[style]={'no_overlap_count':len(zero),'no_overlap_with_nonair_under_entrance_count':len(supported),
                    'no_overlap_candidates':zero,'supported_candidates':supported}
print(json.dumps({'approved_sha256':sha,'offsets_each_axis':[-24,24],'examined_per_style':2401,
 'origin_rule':'(-3+dx,17,-3+dz)','rotation':'none',
 'limits':'Nonair below entrance is only an optimistic indicator: not proven solid, reachable, 5-wide or supported. Other Y, rotations, bridges and alternate connectors not tested.',
 'styles':results},ensure_ascii=False,sort_keys=True,indent=1))

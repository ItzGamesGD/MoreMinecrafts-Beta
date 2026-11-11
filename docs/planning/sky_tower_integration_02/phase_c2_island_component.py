#!/usr/bin/env python3
"""Conservative flat-parent connectivity; not a gameplay route or physics model."""
import ast,json
from collections import deque
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2];src=P/'audit_worldgen.py';tree=ast.parse(src.read_text());ns={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];d=ROOT/'src/main/resources/data/moreminecrafts';tower=blocks(read(d/'structure/sky_tower/full_01.nbt'))
solid={'minecraft:stone','minecraft:andesite','minecraft:cobblestone','minecraft:tuff','minecraft:dirt','minecraft:coarse_dirt','minecraft:grass_block','minecraft:podzol','minecraft:snow_block','minecraft:packed_ice','minecraft:orange_terracotta','minecraft:yellow_terracotta','minecraft:white_terracotta','minecraft:red_sandstone','minecraft:sandstone','minecraft:cut_sandstone','minecraft:smooth_sandstone','minecraft:gravel','minecraft:sand','minecraft:red_sand','minecraft:mossy_cobblestone'}
candidates=json.loads((P/'phase_c2_side_access.json').read_text())['candidates'];results=[]
for c in candidates:
 island=blocks(read(d/f'structure/sky_islands/{c["style"]}/tower.nbt'));world={p:q['id'] for p,q in island.items()}
 for (x,y,z),q in tower.items():
  if q['id']!='minecraft:structure_void':world[(x-15,y+17,z-3)]=q['id']
 for v in c['bridge_and_support_voxels']:world[tuple(v['pos'])]=v['block']
 # Floor stays the same parent block; two blocks overhead must be strictly air.
 valid=set()
 for x in range(43):
  for z in range(43):
   p=(x,16,z);before=island.get(p,{}).get('id','minecraft:air')
   if before in solid and world.get(p,'minecraft:air')==before and all(world.get((x,y,z),'minecraft:air')=='minecraft:air' for y in (17,18)):
    valid.add((x,z))
 start=c['route'][0]['floor'];start=(start[0],start[2]);component=set();todo=deque([start]) if start in valid else deque()
 while todo:
  p=todo.popleft()
  if p in component:continue
  component.add(p);x,z=p
  todo.extend(q for q in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)) if q in valid and q not in component)
 results.append({'style':c['style'],'start':start,'flat_component_count':len(component),'flat_component_xz':sorted(component),'scope':'only unchanged parent floor y16, strictly air at y17/y18, cardinal adjacency; excludes steps and unknown collision shapes','identified_existing_player_path':'UNVERIFIED','real_player_walk':'UNVERIFIED'})
(P/'phase_c2_island_component.json').write_text(json.dumps({'cases':results,'limits':'original-parent and tower/addon ID replay; no other jigsaw children, server processors, block collision shapes or actual movement'},indent=1)+'\n')
for r in results:print(r['style'],r['start'],r['flat_component_count'])

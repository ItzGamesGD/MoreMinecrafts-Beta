#!/usr/bin/env python3
"""Independent, conservative static inspection of island terrain just beyond a bridge."""
import ast,json
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2]
src=P/'audit_worldgen.py';tree=ast.parse(src.read_text());ns={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),ns)
read,blocks=ns['read'],ns['blocks']; data=ROOT/'src/main/resources/data/moreminecrafts'
# Limited set of full-cube terrain IDs, not a Minecraft shape database.
solid={'minecraft:stone','minecraft:andesite','minecraft:cobblestone','minecraft:tuff','minecraft:dirt','minecraft:coarse_dirt','minecraft:grass_block','minecraft:podzol','minecraft:snow_block','minecraft:packed_ice','minecraft:orange_terracotta','minecraft:yellow_terracotta','minecraft:white_terracotta','minecraft:red_sandstone','minecraft:sandstone','minecraft:cut_sandstone','minecraft:smooth_sandstone','minecraft:gravel','minecraft:sand','minecraft:red_sand','minecraft:mossy_cobblestone'}
candidates=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']; result=[]
for style in ('snowy','meadow','forest','badlands','desert','rocky'):
 c=next(q for q in candidates if q['id']==f'{style}_dx-12_dz0_r0_w3')
 island=blocks(read(data/f'structure/sky_islands/{style}/tower.nbt'))
 # At x39 the tower and add-ons do not occupy any of the inspected columns.
 x=39; cells=[]
 for z in (20,21,22):
  occupied=[(y,island[(x,y,z)]['id']) for y in range(0,18) if (x,y,z) in island and island[(x,y,z)]['id'] in solid]
  if not occupied:
   cells.append({'column':[x,z],'floor':'not found in y=0..17','step_from_bridge':None});continue
  y,bid=max(occupied); above=[]
  for h in (y+1,y+2):
   q=island.get((x,h,z));above.append(q['id'] if q else 'minecraft:air')
  cells.append({'column':[x,z],'floor':[x,y,z],'block':bid,'assumed_cube_top_y':y+1,'difference_from_slab_top':y+1-17.5,'two_cells_above':above})
 result.append({'style':style,'candidate':c['id'],'bridge_end_slab_top_y':17.5,'beyond_end_columns':cells,'reachable_from_island_route':'UNVERIFIED','collision_shapes_and_player_movement':'UNVERIFIED'})
(P/'phase_c2_landing_audit.json').write_text(json.dumps({'scope':'parent template only; hardcoded full-cube ID assumptions; not actual server player movement, not a full route','cases':result},indent=1)+'\n')
for r in result:
 print(r['style'],[(x['column'],x.get('difference_from_slab_top')) for x in r['beyond_end_columns']])

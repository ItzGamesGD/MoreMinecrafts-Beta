#!/usr/bin/env python3
"""Six non-final side-opening variants, with conservative voxel headroom inspection."""
import ast,json
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2]
src=P/'audit_worldgen.py'; tree=ast.parse(src.read_text());ns={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];d=ROOT/'src/main/resources/data/moreminecrafts'
tower=blocks(read(d/'structure/sky_tower/full_01.nbt'));allc=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']
# Fixed, explicit trial configurations only. Rail openings are not approved or claimed safe.
choices={'snowy':(35,23,(35,36)),'meadow':(36,19,(36,37)),'forest':(36,19,(36,37)),'badlands':(36,19,(35,36)),'desert':(36,23,(36,37)),'rocky':(36,19,(35,36))}
res=[]
for style,(start_x,edge_z,gap_x) in choices.items():
 c=next(v for v in allc if v['id']==f'{style}_dx-12_dz0_r0_w3')
 island=blocks(read(d/f'structure/sky_islands/{style}/tower.nbt')); merged=dict(island)
 for (x,y,z),q in tower.items():
  if q['id']!='minecraft:structure_void':merged[(x-15,y+17,z-3)]=q
 removed={(x,18,edge_z) for x in gap_x};addons=[]
 for v in c['bridge_and_support_voxels']:
  if v['component']=='rail' and tuple(v['pos']) in removed:continue
  addons.append(v);merged[tuple(v['pos'])]={'id':v['block']}
 # Enter from the island over the side edge and move toward local x34 entrance.
 inner_z=20 if edge_z==19 else 22
 route=[{'kind':'parent_ground','floor':[start_x,16,edge_z],'top_y':17.0}, {'kind':'slab','floor':[start_x,17,inner_z],'top_y':17.5}]
 if inner_z!=21:route.append({'kind':'slab','floor':[start_x,17,21],'top_y':17.5})
 for x in range(start_x-1,33,-1):route.append({'kind':'slab','floor':[x,17,21],'top_y':17.5})
 for step in route:
  x,y,z=step['floor'];q=merged.get((x,y,z));step['floor_id']=q['id'] if q else 'minecraft:air'
  heads=(y+1,y+2) if step['kind']=='slab' else (y+1,y+2)
  step['head_blocks']=[{'pos':[x,h,z],'id':merged.get((x,h,z),{}).get('id','minecraft:air')} for h in heads]
  step['head_nonair']=[a for a in step['head_blocks'] if a['id'] not in ('minecraft:air','minecraft:cave_air','minecraft:void_air')]
 result={'style':style,'id':style+'_side_opening_trial','base_candidate':c['id'],'removed_rail_positions':sorted(removed),'bridge_and_support_voxels':addons,'route':route,
 'head_nonair_cells':[a for v in route for a in v['head_nonair']], 'land_top_y':17.0,'bridge_top_y':17.5,'actual_player_route':'UNVERIFIED','fall_protection':'UNVERIFIED','existing_island_route':'UNVERIFIED','first_floor':'UNVERIFIED'}
 res.append(result)
(P/'phase_c2_side_access.json').write_text(json.dumps({'scope':'3D candidate side openings; block-id based conservative occupancy; not a Minecraft collision-shape model','candidates':res},indent=1)+'\n')
for r in res:print(r['style'],'opened',r['removed_rail_positions'],'route steps',len(r['route']),'nonair head blocks',len(r['head_nonair_cells']))

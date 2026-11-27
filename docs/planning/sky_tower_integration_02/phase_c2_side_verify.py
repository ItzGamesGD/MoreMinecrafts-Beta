#!/usr/bin/env python3
"""Independent side-route replay and server-log comparison. Not Minecraft physics."""
import ast,json
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2];src=P/'audit_worldgen.py';tree=ast.parse(src.read_text());ns={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];data=ROOT/'src/main/resources/data/moreminecrafts';tower=blocks(read(data/'structure/sky_tower/full_01.nbt'))
assert tower[(49,0,24)]['id']=='minecraft:stone_brick_slab' and tower[(49,0,24)]['properties']['type']=='bottom'
cs=json.loads((P/'phase_c2_side_access.json').read_text())['candidates'];logs=json.loads((P/'phase_c2_side_server_results.json').read_text());by_style={x['style']:x for x in logs['cases']};out=[]
for c in cs:
 island=blocks(read(data/f'structure/sky_islands/{c["style"]}/tower.nbt'));world={p:q['id'] for p,q in island.items()}
 for (x,y,z),q in tower.items():
  if q['id']!='minecraft:structure_void':world[(x-15,y+17,z-3)]=q['id']
 for v in c['bridge_and_support_voxels']:world[tuple(v['pos'])]=v['block']
 assert len(c['removed_rail_positions'])==2
 assert all(not any(v['component']=='rail' and v['pos']==p for v in c['bridge_and_support_voxels']) for p in c['removed_rail_positions'])
 route=c['route'];log=by_style[c['style']];assert len(route)==len(log['route_cells'])
 assert route[-1]['floor']==[34,17,21]
 for a,b in zip(route,route[1:]):
  assert abs(a['floor'][0]-b['floor'][0])+abs(a['floor'][2]-b['floor'][2])==1
  assert abs(a['top_y']-b['top_y'])<=.5
 for step,observed in zip(route,log['route_cells']):
  floor=tuple(step['floor']);assert world.get(floor,'minecraft:air')==step['floor_id']
  assert observed['floor']==step['floor'] and observed['floor_expected']==step['floor_id'] and 'Test passed' in observed['floor_response']
  for h,got in zip(step['head_blocks'],observed['head']):
   assert world.get(tuple(h['pos']),'minecraft:air')==h['id'] and got['pos']==h['pos'] and got['expected']==h['id'] and 'Test passed' in got['response']
 nonair=[h for step in route for h in step['head_blocks'] if h['id']!='minecraft:air']
 out.append({'style':c['style'],'independent_cardinal_and_half_height':'PASS','server_sampled_ids_match':'PASS','nonair_head_blocks':nonair,'actual_movement':'UNVERIFIED','island_network_and_floor_1':'UNVERIFIED'})
(P/'phase_c2_side_verification.json').write_text(json.dumps({'results':out,'scope':'coordinate and server ID checks only; no collision shape or player movement'},indent=1)+'\n')
print('PASS independent replay and server ID samples for',len(out),'side variants')

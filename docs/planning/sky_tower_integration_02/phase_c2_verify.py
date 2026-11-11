#!/usr/bin/env python3
"""Independent coordinate replay: no import of candidate generator or its transform."""
import ast, gzip, json
from collections import Counter
from pathlib import Path
P=Path(__file__).resolve().parent
src=P/'audit_worldgen.py'; tree=ast.parse(src.read_text()); env={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),env)
read,blocks=env['read'],env['blocks'];root=P.parents[2];data=root/'src/main/resources/data/moreminecrafts'
old=blocks(read(data/'structure/sky_tower/full_01.nbt'))
assert old[(49,0,24)]['id']=='minecraft:stone_brick_slab'
assert old[(49,0,24)]['properties']['type']=='bottom'
candidates=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']
islands={s:blocks(read(data/f'structure/sky_islands/{s}/tower.nbt')) for s in ('snowy','meadow','forest','badlands','desert','rocky')}
AIR={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'}
def world(p,dx,dz,y,r):
 x,h,z=p
 # Explicit four formulas, not the generator's iterative transform.
 x,z=((x,z),(48-z,x),(48-x,48-z),(z,48-x))[r]
 return (x+dx-3,h+y,z+dz-3)
# Test different transformation formula for corners and inverse all rotations.
for r in range(4):
 for p in ((0,0,0),(49,0,24),(48,79,48),(24,12,24)):
  q=world(p,0,0,0,r); a,b=q[0]+3,q[2]+3
  recovered=((a,b),(b,48-a),(48-a,48-b),(48-b,a))[r]
  assert recovered==(p[0],p[2])
results=[]
for c in candidates:
 style,dx,dz,r,h=c['style'],c['dx'],c['dz'],c['relative_rotation'],c['base_y']; island=islands[style]
 t={world(p,dx,dz,h,r):q for p,q in old.items() if q['id']!='minecraft:structure_void'}
 expected=world((49,0,24),dx,dz,h,r);assert list(expected)==c['entrance_slab']
 reported={tuple(x['pos']):(x['before'],x['after']) for x in c['tower_overwrites_parent']}
 actual={p:(island[p]['id'],q['id']) for p,q in t.items() if p in island and island[p]['id'] not in AIR and island[p]['id']!=q['id']}
 assert actual==reported,(c['id'],'overwrite mismatch')
 merged=dict(island);merged.update(t)
 for v in c['bridge_and_support_voxels']:
  p=tuple(v['pos']);assert merged.get(p,{}).get('id','minecraft:air')==v['before'];merged[p]={'id':v['block']}
 deck={tuple(v['pos']) for v in c['bridge_and_support_voxels'] if v['component']=='deck'}
 expected_deck={world((x,0,z),dx,dz,h,r) for x in range(50,54) for z in range(24-c['width']//2,25+c['width']//2)}
 assert deck==expected_deck
 blockers=[]
 for x in range(49,54):
  for y in (1,2):
   p=world((x,y,24),dx,dz,h,r);q=merged.get(p,{}).get('id','minecraft:air')
   if q not in AIR:blockers.append({'pos':p,'block':q})
 beyond=world((54,-1,24),dx,dz,h,r);outside=merged.get(beyond,{}).get('id','minecraft:air')
 results.append({'id':c['id'],'independent_entrance_and_replay':'PASS','order_sensitive_different_id_cells':len(actual),'deck_center_head_cells_nonair':blockers,'ground_one_below_beyond_bridge':{'pos':beyond,'block':outside},'actual_player_path':'UNVERIFIED','content_preservation':'UNVERIFIED','four_world_rotations':'UNVERIFIED'})
(P/'phase_c2_verification.json').write_text(json.dumps({'method':'independent direct four-formula replay; conservative nonair head-cell listing; not block-shape/player validation','candidates':results},sort_keys=True,separators=(',',':'))+'\n')
print('Independent replay PASS:',len(results),'candidates; player path, jigsaw and live rotation UNVERIFIED')

#!/usr/bin/env python3
"""Opt-in disposable RCON test. Only for the fresh, dedicated Phase C-2 world."""
import importlib.util,json,os,time
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2]
world=Path(os.environ['PHASE_C2_SERVER_DIR'])
assert world.resolve()==Path('/tmp/sky-tower-phase-c2-server-20261109')
props=dict(x.split('=',1) for x in (world/'server.properties').read_text().splitlines() if '=' in x)
os.environ['RCON_PASSWORD']=props['rcon.password'];os.environ['MC_RCON_PORT']=props['rcon.port']
spec=importlib.util.spec_from_file_location('rcon',ROOT/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
candidates=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']
result={'scope':'isolated manual place template and sampled block checks; not worldgen, complete block comparison or actual player movement','cases':[]}
with rc.connect() as sock:
 sock.settimeout(120);seq=[1]
 def cmd(value):
  seq[0]+=1;rc.send_packet(sock,seq[0],2,value);return rc.read_packet(sock)[2]
 try:
  for i,style in enumerate(('snowy','meadow','forest','badlands','desert','rocky')):
   c=next(v for v in candidates if v['style']==style and v['dx']==-12 and v['dz']==0 and v['width']==3)
   ox=int(os.environ.get('PHASE_C2_ORIGIN_X','14000'))+i*200;oy=80;oz=1000;case={'style':style,'island_origin':[ox,oy,oz],'candidate_id':c['id'],'commands':[]}
   def issue(command):
    value=cmd(command);case['commands'].append({'command':command,'response':value});return value
   response=issue(f'forceload add {ox-32} {oz-32} {ox+64} {oz+64}')
   if 'too many' in response.lower():raise RuntimeError(response)
   for template,x,y,z in ((f'moreminecrafts:sky_islands/{style}/tower',ox,oy,oz),('moreminecrafts:sky_tower/full_01',ox-15,oy+17,oz-3)):
    response=issue(f'place template {template} {x} {y} {z} none none 1.0 0 strict')
    if 'Loaded template' not in response:raise RuntimeError(response)
   for voxel in c['bridge_and_support_voxels']:
    x,y,z=voxel['pos'];block=voxel['block'];state='[type=bottom,waterlogged=false]' if block.endswith('_slab') else ''
    response=issue(f'setblock {ox+x} {oy+y} {oz+z} {block}{state}')
    if 'Changed the block' not in response and 'already' not in response.lower():raise RuntimeError(response)
   x,y,z=c['entrance_slab'];response=issue(f'execute if block {ox+x} {oy+y} {oz+z} minecraft:stone_brick_slab[type=bottom]')
   case['entrance_block_check']='Test passed' in response
   deck=next(v for v in c['bridge_and_support_voxels'] if v['component']=='deck');x,y,z=deck['pos']
   response=issue(f'execute if block {ox+x} {oy+y} {oz+z} minecraft:stone_brick_slab[type=bottom]')
   case['deck_sample_check']='Test passed' in response
   result['cases'].append(case)
   (P/'phase_c2_server_results.json').write_text(json.dumps(result,indent=1)+'\n')
  result['save_response']=cmd('save-all flush')
 finally:
  result['stop_response']=cmd('stop')
  (P/'phase_c2_server_results.json').write_text(json.dumps(result,indent=1)+'\n')
print('server placement cases:',len(result['cases']))

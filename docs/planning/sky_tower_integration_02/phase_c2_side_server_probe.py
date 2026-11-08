#!/usr/bin/env python3
"""Opt-in side-access experiment on a dedicated local world at explicitly fresh origins."""
import importlib.util,json,os
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2]
world=Path(os.environ['PHASE_C2_SERVER_DIR']).resolve();assert world==Path('/tmp/sky-tower-phase-c2-server-20261109')
base=int(os.environ['PHASE_C2_SIDE_ORIGIN_X'])
props=dict(x.split('=',1) for x in (world/'server.properties').read_text().splitlines() if '=' in x)
os.environ['RCON_PASSWORD']=props['rcon.password'];os.environ['MC_RCON_PORT']=props['rcon.port']
spec=importlib.util.spec_from_file_location('rc',ROOT/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
data=json.loads((P/'phase_c2_side_access.json').read_text())['candidates'];out={'scope':'Manual template and side-opening prototypes; id checks only, not player or collision-shape validation','cases':[]}
with rc.connect() as sock:
 sock.settimeout(120);seq=[0]
 def cmd(s):seq[0]+=1;rc.send_packet(sock,seq[0],2,s);return rc.read_packet(sock)[2]
 try:
  for i,c in enumerate(data):
   ox,oy,oz=base+i*200,80,4000;case={'style':c['style'],'origin':[ox,oy,oz],'candidate':c['id'],'commands':[],'route_cells':[]};out['cases'].append(case)
   def ask(s):
    a=cmd(s);case['commands'].append({'command':s,'response':a});return a
   def position(p):return f'{ox+p[0]} {oy+p[1]} {oz+p[2]}'
   ask(f'forceload add {ox-32} {oz-32} {ox+48} {oz+48}')
   for template,x,y,z in ((f'moreminecrafts:sky_islands/{c["style"]}/tower',ox,oy,oz),('moreminecrafts:sky_tower/full_01',ox-15,oy+17,oz-3)):
    a=ask(f'place template {template} {x} {y} {z} none none 1.0 0 strict')
    if 'Loaded template' not in a:raise RuntimeError(a)
   for v in c['bridge_and_support_voxels']:
    state='[type=bottom,waterlogged=false]' if v['block'].endswith('_slab') else ''
    a=ask(f'setblock {position(v["pos"])} {v["block"]}{state}')
    if 'Changed the block' not in a and 'already' not in a.lower():raise RuntimeError(a)
   for step in c['route']:
    floor=ask(f'execute if block {position(step["floor"])} {step["floor_id"]}')
    heads=[{'pos':b['pos'],'expected':b['id'],'response':ask(f'execute if block {position(b["pos"])} {b["id"]}')} for b in step['head_blocks']]
    case['route_cells'].append({'floor':step['floor'],'floor_expected':step['floor_id'],'floor_response':floor,'head':heads})
   ask('save-all flush');ask(f'forceload remove {ox-32} {oz-32} {ox+48} {oz+48}')
   print(c['style'],'route cells',len(case['route_cells']),flush=True)
   (P/'phase_c2_side_server_results.json').write_text(json.dumps(out,indent=1)+'\n')
 finally:
  try:out['stop_response']=cmd('stop')
  finally:(P/'phase_c2_side_server_results.json').write_text(json.dumps(out,indent=1)+'\n')

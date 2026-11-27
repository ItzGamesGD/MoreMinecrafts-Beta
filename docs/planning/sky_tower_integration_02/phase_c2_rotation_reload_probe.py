#!/usr/bin/env python3
"""Opt-in destructive-to-dedicated-test-world only: template rotations and sampled reload."""
import importlib.util,json,os,time
from pathlib import Path
P=Path(__file__).resolve().parent;ROOT=P.parents[2]
world=Path(os.environ['PHASE_C2_SERVER_DIR']).resolve()
assert world==Path('/tmp/sky-tower-phase-c2-server-20261109')
props=dict(x.split('=',1) for x in (world/'server.properties').read_text().splitlines() if '=' in x)
os.environ['RCON_PASSWORD']=props['rcon.password'];os.environ['MC_RCON_PORT']=props['rcon.port']
spec=importlib.util.spec_from_file_location('rcon',ROOT/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
candidates=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']
styles=('snowy','meadow','forest','badlands','desert','rocky')
# Caller must select fresh, unoccupied coordinates for every run.
origin_base=int(os.environ['PHASE_C2_ROTATION_ORIGIN_X'])
rotations=('none','clockwise_90','180','counterclockwise_90')
def rot(x,z,r):return ((x,z),(-z,x),(-x,-z),(z,-x))[r]
def pos(origin,p,r):
 x,z=rot(p[0],p[2],r);return f'{origin[0]+x} {origin[1]+p[1]} {origin[2]+z}'
output={'description':'Actual dedicated server template placements; sampled anchors; not actual worldgen, complete voxel/state check or player movement. Four meadow cases additionally attempt unload and reload.', 'cases':[]}
with rc.connect() as sock:
 sock.settimeout(180);seq=[0]
 def cmd(value):
  seq[0]+=1;rc.send_packet(sock,seq[0],2,value);return rc.read_packet(sock)[2]
 try:
  for i,style in enumerate(styles):
   c=next(v for v in candidates if v['id']==f'{style}_dx-12_dz0_r0_w3')
   for r,rotation in enumerate(rotations):
    origin=(origin_base+i*1200+r*200,80,3000);ox,oy,oz=origin
    case={'style':style,'rotation':rotation,'island_origin':origin,'candidate_id':c['id'],'commands':[],'checks':{}}
    def ask(value):
     answer=cmd(value);case['commands'].append({'command':value,'response':answer});return answer
    def check(name,coord,block):
     q=f'execute if block {pos(origin,coord,r)} {block}'
     answer=ask(q);case['checks'][name]={'command':q,'response':answer,'pass':'Test passed' in answer}
    def save():
     (P/'phase_c2_rotation_reload_results.json').write_text(json.dumps(output,indent=1)+'\n')
    output['cases'].append(case);save()
    answer=ask(f'forceload add {ox-48} {oz-48} {ox+48} {oz+48}')
    if 'too many' in answer.lower():raise RuntimeError(answer)
    tx,tz=rot(-15,-3,r)
    for template,x,y,z in ((f'moreminecrafts:sky_islands/{style}/tower',ox,oy,oz),('moreminecrafts:sky_tower/full_01',ox+tx,oy+17,oz+tz)):
     answer=ask(f'place template {template} {x} {y} {z} {rotation} none 1.0 0 strict')
     if 'Loaded template' not in answer:raise RuntimeError(answer)
    for v in c['bridge_and_support_voxels']:
     block=v['block'];state='[type=bottom,waterlogged=false]' if block.endswith('_slab') else ''
     answer=ask(f'setblock {pos(origin,v["pos"],r)} {block}{state}')
     if 'Changed the block' not in answer and 'already' not in answer.lower():raise RuntimeError(answer)
    check('island_socket',(21,16,21),'minecraft:jigsaw')
    check('entrance',c['entrance_slab'],'minecraft:stone_brick_slab[type=bottom]')
    check('deck',next(v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='deck'),'minecraft:stone_brick_slab[type=bottom]')
    check('rail',next(v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='rail'),'minecraft:stone_brick_wall')
    check('support',next(v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='support'),'minecraft:stone_bricks')
    check('trial_spawner',(2,29,14),'minecraft:trial_spawner') # tower origin (-15,17,-3) + local (17,12,17)
    check('vault',(1,28,12),'minecraft:vault')
    save()
    ask('save-all flush');ask(f'forceload remove {ox-48} {oz-48} {ox+48} {oz+48}')
    if style=='meadow':
     case['unload_sprint']=ask('tick sprint 1200');time.sleep(2)
     q=f'execute if loaded {pos(origin,c["entrance_slab"],r)}'; answer=ask(q)
     case['unload_check']={'command':q,'response':answer,'observed_unloaded':'Test failed' in answer}
     ask(f'forceload add {ox-48} {oz-48} {ox+48} {oz+48}')
     check('entrance_after_reload',c['entrance_slab'],'minecraft:stone_brick_slab[type=bottom]')
     check('support_after_reload',next(v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='support'),'minecraft:stone_bricks')
     check('spawner_after_reload',(2,29,14),'minecraft:trial_spawner')
     ask(f'forceload remove {ox-48} {oz-48} {ox+48} {oz+48}')
    save();print(style,rotation,'checks',sum(x['pass'] for x in case['checks'].values()),'/',len(case['checks']),flush=True)
 finally:
  try:output['stop_response']=cmd('stop')
  finally:(P/'phase_c2_rotation_reload_results.json').write_text(json.dumps(output,indent=1)+'\n')

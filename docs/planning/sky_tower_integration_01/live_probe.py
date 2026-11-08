"""Disposable mod-server probe. Completion states injected synthetically, not a real combat test."""
import os,json,importlib.util,time,sys
from pathlib import Path
H=Path(__file__).resolve().parent;R=H.parents[2];S=Path('/tmp/sky-tower-integration-server')
p=dict(line.split('=',1) for line in (S/'server.properties').read_text().splitlines() if '=' in line)
os.environ['RCON_PASSWORD']=p['rcon.password'];os.environ['MC_RCON_PORT']=p['rcon.port']
spec=importlib.util.spec_from_file_location('rc',R/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
base=int(os.environ.get('PROBE_ORIGIN_X','7000'))
mode=sys.argv[1];out={'mode':mode,'base_origin_x':base,'scope':'actual Fabric26.3 server; synthetic cooldown states; no combat/client/gates/natural generation claim'}
with rc.connect() as sock:
 sock.settimeout(60);n=1
 def ask(cmd):
  global n
  n+=1;rc.send_packet(sock,n,2,cmd);response=rc.read_packet(sock)[2];out.setdefault('commands',[]).append({'command':cmd,'response':response});(H/'live_probe_partial.json').write_text(json.dumps(out,indent=1)+'\n');return response
 if mode=='setup':
  ask('forceload remove all')
  for i,rotation in enumerate(('none','clockwise_90','180','counterclockwise_90')):
   x=base+300*i;ask(f'forceload add {x-50} 950 {x+50} 1050');time.sleep(2)
   place=ask(f'place template moreminecrafts:sky_tower/full_01 {x} 65 1000 {rotation} none 1.0 0 strict')
   register=ask(f'mmskytower register {x} 65 1000 {i}')
   assert 'Loaded template' in place and 'Registered ' in register,(place,register)
  out['before_completion']=ask('mmskytower status')
  assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask=0' in out['before_completion']
  for x,z in ((base+17,1017),(base+17,1031),(base+31,1017),(base+31,1031)):
   ask(f'setblock {x} 77 {z} minecraft:trial_spawner[trial_spawner_state=cooldown]')
  ask('tick sprint 100');time.sleep(2);status=ask('mmskytower status');out['status']=status
  assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask=15' in status,status
  for i in range(1,4):assert f'minecraft:overworld|{base+300*i},65,1000|{i} completedSpawnerMask=0' in status,status
  out['floor_masks']=[15]
  for floorY,expectedMask in ((21,255),(33,4095),(47,65535)):
   for x,z in ((base+17,1017),(base+17,1031),(base+31,1017),(base+31,1031)):
    ask(f'setblock {x} {65+floorY+2} {z} minecraft:trial_spawner[trial_spawner_state=cooldown]')
   ask('tick sprint 100');time.sleep(1);status=ask('mmskytower status')
   assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask={expectedMask}' in status,status
   out['floor_masks'].append(expectedMask)
  out['status']=status
  ask(f'setblock {base+17} 77 1017 minecraft:trial_spawner[trial_spawner_state=cooldown]');ask('tick sprint 20');time.sleep(1)
  ask(f'mmskytower register {base} 65 1000 0');again=ask('mmskytower status');assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask=65535' in again
  out['site_count']=status.count('completedSpawnerMask=')
  ask('save-all flush');out['saved_files']=[str(f.relative_to(S)) for f in (S/'world').rglob('*sky_tower*')]
  out['pass']=True
 elif mode=='restore':
  status=ask('mmskytower status');out['status']=status
  assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask=65535' in status,status
  assert status.count('completedSpawnerMask=')==json.loads((H/'live_setup.json').read_text())['site_count'],status
  assert 'quarantined=false' in status,status
  assert status==json.loads((H/'live_setup.json').read_text())['status'],status
  out['pass']=True
 elif mode=='chunk_cycle':
  before=ask('mmskytower status')
  ask(f'forceload remove {base-50} 950 {base+50} 1050');ask('tick sprint 1000');time.sleep(3)
  unloaded=ask(f'execute if loaded {base+17} 77 1017');out['unloaded']=unloaded
  assert 'Test failed' in unloaded,unloaded
  ask(f'forceload add {base-50} 950 {base+50} 1050');ask('tick sprint 100');time.sleep(2)
  loaded=ask(f'execute if loaded {base+17} 77 1017');out['loaded']=loaded
  assert 'Test passed' in loaded,loaded
  after=ask('mmskytower status');assert before==after,(before,after)
  ask('save-all flush');out['pass']=True
 elif mode=='stop':
  ask('stop');out['pass']=True
 else:raise ValueError(mode)
(H/f'live_{mode}.json').write_text(json.dumps(out,indent=1)+'\n');print(mode,out.get('pass'),out.get('status',''))

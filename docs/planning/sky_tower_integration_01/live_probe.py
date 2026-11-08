"""Disposable mod-server probe. Completion states injected synthetically, not a real combat test."""
import os,json,importlib.util,time,sys
from pathlib import Path
H=Path(__file__).resolve().parent;R=H.parents[2];S=Path('/tmp/sky-tower-integration-server')
p=dict(line.split('=',1) for line in (S/'server.properties').read_text().splitlines() if '=' in line)
os.environ['RCON_PASSWORD']=p['rcon.password'];os.environ['MC_RCON_PORT']=p['rcon.port']
spec=importlib.util.spec_from_file_location('rc',R/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
base=int(os.environ.get('PROBE_ORIGIN_X','4000'))
mode=sys.argv[1];out={'mode':mode,'base_origin_x':base,'scope':'actual Fabric26.3 server; synthetic cooldown states; no combat/client/gates/natural generation claim'}
with rc.connect() as sock:
 sock.settimeout(60);n=1
 def ask(cmd):
  global n
  n+=1;rc.send_packet(sock,n,2,cmd);response=rc.read_packet(sock)[2];out.setdefault('commands',[]).append({'command':cmd,'response':response});(H/'live_probe_partial.json').write_text(json.dumps(out,indent=1)+'\n');return response
 if mode=='setup':
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
  ask(f'setblock {base+17} 77 1017 minecraft:trial_spawner[trial_spawner_state=cooldown]');ask('tick sprint 20');time.sleep(1)
  ask(f'mmskytower register {base} 65 1000 0');again=ask('mmskytower status');assert 'completedSpawnerMask=15' in again
  out['site_count']=status.count('completedSpawnerMask=')
  ask('save-all flush');out['saved_files']=[str(f.relative_to(S)) for f in (S/'world').rglob('*sky_tower*')]
  out['pass']=True
 elif mode=='restore':
  status=ask('mmskytower status');out['status']=status
  assert f'minecraft:overworld|{base},65,1000|0 completedSpawnerMask=15' in status,status
  assert status.count('completedSpawnerMask=')==json.loads((H/'live_setup.json').read_text())['site_count'],status
  assert 'quarantined=false' in status,status
  out['pass']=True
 elif mode=='stop':
  ask('stop');out['pass']=True
 else:raise ValueError(mode)
(H/f'live_{mode}.json').write_text(json.dumps(out,indent=1)+'\n');print(mode,out.get('pass'),out.get('status',''))

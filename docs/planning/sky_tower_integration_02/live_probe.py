"""Dedicated disposable mod-server probe. Command removal/synthetic cooldown, NOT player combat/client proof."""
import os,json,importlib.util,time,sys,re
from pathlib import Path
H=Path(__file__).resolve().parent;R=H.parents[2];S=Path('/tmp/sky-tower-integration-server')
p=dict(line.split('=',1) for line in (S/'server.properties').read_text().splitlines() if '=' in line)
os.environ['RCON_PASSWORD']=p['rcon.password'];os.environ['MC_RCON_PORT']=p['rcon.port']
spec=importlib.util.spec_from_file_location('rc',R/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
base=int(os.environ.get('PROBE_ORIGIN_X','10000'));mode=sys.argv[1]
out={'mode':mode,'base':base,'scope':'Fabric26.3 actual server; command removals and synthetic cooldown; not natural generation/combat/client/multiplayer'}
def at(x,r,lx,ly,lz):
 for _ in range(r):lx,lz=-lz,lx
 return f'{x+lx} {65+ly} {1000+lz}'
with rc.connect() as sock:
 sock.settimeout(60);seq=1
 def ask(cmd):
  global seq
  seq+=1;rc.send_packet(sock,seq,2,cmd);response=rc.read_packet(sock)[2]
  out.setdefault('commands',[]).append({'command':cmd,'response':response})
  (H/'live_partial.json').write_text(json.dumps(out,indent=1)+'\n');return response
 def check(pos,block):
  result=ask(f'execute if block {pos} minecraft:{block}');assert 'Test passed' in result,(pos,block,result)
 def wait_ticks(n=100):ask(f'tick sprint {n}');time.sleep(2)
 def gate(x,r,f,block):
  y=(11,22,34,48)[f]
  for lx in range(39,45):
   for ly in range(y,y+3):check(at(x,r,lx,ly,27),block)
 def status():return ask('mmskytower status')
 if mode=='setup':
  ask('forceload remove all')
  for r,rotation in enumerate(('none','clockwise_90','180','counterclockwise_90')):
   x=base+300*r;ask(f'forceload add {x-50} 950 {x+50} 1050');time.sleep(1)
   assert 'Loaded template' in ask(f'place template moreminecrafts:sky_tower/full_01 {x} 65 1000 {rotation} none 1.0 0 strict')
   assert 'Registered ' in ask(f'mmskytower register {x} 65 1000 {r}')
  wait_ticks()
  for r in range(4):
   for f in range(4):gate(base+300*r,r,f,'barrier')
  for lx,lz in ((17,17),(17,31),(31,17)):ask(f'setblock {at(base,0,lx,12,lz)} minecraft:air')
  wait_ticks();s=status();assert f'{base},65,1000|0 completedSpawnerMask=7 destroyedSpawnerMask=7' in s,s
  gate(base,0,0,'barrier')
  ask(f'setblock {at(base,0,31,12,31)} minecraft:trial_spawner[trial_spawner_state=cooldown]');wait_ticks()
  s=status();assert f'{base},65,1000|0 completedSpawnerMask=15 destroyedSpawnerMask=7' in s,s
  gate(base,0,0,'air');gate(base,0,1,'barrier')
  # Owned barrier -> real removal -> foreign stone. Never remove this stone on floor completion.
  foreign=at(base,0,39,22,27);ask(f'setblock {foreign} minecraft:air');ask(f'setblock {foreign} minecraft:stone')
  for floor,spawnerY in ((1,23),(2,35),(3,49)):
   for lx,lz in ((17,17),(17,31),(31,17),(31,31)):ask(f'setblock {at(base,0,lx,spawnerY,lz)} minecraft:air')
  wait_ticks();s=status();assert f'{base},65,1000|0 completedSpawnerMask=65535 destroyedSpawnerMask=65527' in s,s
  check(foreign,'stone')
  for f in (2,3):gate(base,0,f,'air')
  for r in range(1,4):assert f'{base+300*r},65,1000|{r} completedSpawnerMask=0 destroyedSpawnerMask=0' in s,s
  # Reinstalling a completed spawner neither resets progress nor produces an extra completion.
  ask(f'setblock {at(base,0,17,12,17)} minecraft:trial_spawner');wait_ticks()
  assert f'{base},65,1000|0 completedSpawnerMask=65535 destroyedSpawnerMask=65527' in status()
  assert 'Already registered' in ask(f'mmskytower register {base} 65 1000 0')
  # Actual unload/load of a tower whose 72 barriers are still owned.
  x=base+300;ask(f'forceload remove {x-50} 950 {x+50} 1050');wait_ticks(1000)
  assert 'Test failed' in ask(f'execute if loaded {at(x,1,39,11,27)}')
  ask(f'forceload add {x-50} 950 {x+50} 1050');wait_ticks();gate(x,1,0,'barrier')
  out['status']=status();ask('save-all flush');out['pass']=True
 elif mode=='restore':
  before=status();expected=json.loads((H/'live_setup.json').read_text())['status'];assert before==expected,(before,expected)
  # Persisted barrier ownership must be usable, not merely an old barrier left in world data.
  x=base+300
  for lx,lz in ((17,17),(17,31),(31,17),(31,31)):ask(f'setblock {at(x,1,lx,12,lz)} minecraft:air')
  wait_ticks();gate(x,1,0,'air');gate(x,1,1,'barrier')
  gate(base,0,0,'air');check(at(base,0,39,22,27),'stone')
  out['status_before']=before;out['status_after']=status();ask('save-all flush');out['pass']=True
 elif mode=='check':
  before=status();assert before==json.loads((H/'live_restore.json').read_text())['status_after'],before
  gate(base,0,0,'air');check(at(base,0,39,22,27),'stone')
  gate(base+300,1,0,'air');gate(base+300,1,1,'barrier')
  x=base+600;r=2;p=at(x,r,39,11,27);check(p,'barrier')
  ask(f'setblock {p} minecraft:air');wait_ticks();check(p,'barrier')
  for lx,lz in ((17,17),(17,31),(31,17),(31,31)):ask(f'setblock {at(x,r,lx,12,lz)} minecraft:air')
  wait_ticks();gate(x,r,0,'air');gate(x,r,1,'barrier')
  out['status']=status();ask('save-all flush');out['pass']=True
 elif mode=='state':
  assert status()==json.loads((H/'live_check.json').read_text())['status']
  x=base+900;r=3;p=at(x,r,39,11,27)
  check(p,'barrier[waterlogged=false]')
  ask(f'setblock {p} minecraft:barrier[waterlogged=true]')
  for lx,lz in ((17,17),(17,31),(31,17),(31,31)):ask(f'setblock {at(x,r,lx,12,lz)} minecraft:air')
  wait_ticks();check(p,'barrier[waterlogged=true]')
  assert 'Test passed' in ask(f'execute unless block {at(x,r,39,12,27)} minecraft:barrier')
  out['status']=status();ask('save-all flush');out['pass']=True
 elif mode=='vaults':
  out['codec_note']='VaultConfig.DEFAULT is TRIAL_CHAMBERS_REWARD; vanilla lenientOptionalFieldOf omits default loot_table when saving. See vault_codec_reference.txt.'
  for r in range(4):
   for y in (11,22,34,48):
    for z in (15,33):
     result=ask(f'data get block {at(base+300*r,r,16,y,z)} config')
     assert ('minecraft:chests/trial_chambers/reward' in result or 'loot_table' not in result) and 'minecraft:trial_key' in result,result
     assert 'moreminecrafts:chests/sky_islands' not in result,result
  out['pass']=True
 elif mode=='stop':ask('stop');out['pass']=True
 else:raise ValueError(mode)
(H/f'live_{mode}.json').write_text(json.dumps(out,indent=1)+'\n');print(mode,out.get('pass'))

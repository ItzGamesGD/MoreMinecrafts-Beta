"""RCON strict placement and full box validation; credentials only read from temporary server."""
import os,json,importlib.util,sys
from pathlib import Path
H=Path(__file__).resolve().parent;S=Path(sys.argv[1]);pr=dict(l.split('=',1) for l in (S/'server.properties').read_text().splitlines() if '=' in l)
os.environ['RCON_PASSWORD']=pr['rcon.password'];os.environ['MC_RCON_PORT']='25586'
s=importlib.util.spec_from_file_location('rc',H.parents[2]/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(s);s.loader.exec_module(rc)
m=json.loads((H/'server_manifest.json').read_text());out={}
with rc.connect() as sock:
 sock.settimeout(60);n=1
 def ask(cmd):
  global n
  n+=1;rc.send_packet(sock,n,2,cmd);return rc.read_packet(sock)[2]
 for cmd in ('forceload add 960 960 1040 1040','scoreboard objectives add fullbuild dummy','difficulty peaceful','time set day','weather clear','gamerule advance_time false','gamerule advance_weather false'):
  out[cmd]=ask(cmd)
 # Terrain pad supplies the documented ground-height placement interface.
 for x in range(970,1031,20):out[f'ground_{x}']=ask(f'fill {x} 64 970 {x+19} 64 1030 minecraft:stone')
 # NBT omits air. Clear prior candidate occupancy before every placement.
 for x in range(976,1026,5):out[f'clear_{x}']=ask(f'fill {x} 65 976 {min(x+4,1025)} 144 1024 minecraft:air')
 out['strict_place']=ask('place template fullbuild:tower '+' '.join(map(str,m['placement']))+' none none 1.0 0 strict')
 out['functions']=[ask('function fullbuild:'+f) for f in m['functions']]
 out['error_score']=ask('scoreboard players get mismatch fullbuild');out['positions']=m['positions']
 out['pass']='has 0 ' in out['error_score'] and 'Loaded template' in out['strict_place'] and all('Running function' in r for r in out['functions'])
 for p in json.loads((H/'model.json').read_text())['spawners'][:1]:
  x,y,z=p['position'];out['spawner_nbt']=ask(f'data get block {1000+x} {65+y} {1000+z}')
 out['op']=ask('op FullBuilder');out['spawn']=ask('setworldspawn 1023 66 1000');out['tp']=ask('tp FullBuilder 1023.5 66 1000.5 90 0')
(H/'server_placement.json').write_text(json.dumps(out,indent=1)+'\n');print(out['strict_place'],out['error_score'],'PASS',out['pass'])

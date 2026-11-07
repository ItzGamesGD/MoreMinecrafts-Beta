"""Prepare temporary 26.3 test pack, then strict-place C1/C2 and compare full bbox.
Usage: python p5_server.py prepare /tmp/test-server ; python p5_server.py check /tmp/test-server
Server must use rcon on 25586 and loopback. Credentials read from local server.properties, never logged.
Only temporary world's datapack changes. No repository NBT/worldgen changed.
"""
import csv,json,struct,gzip,sys,os,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;D=H/'data'
def text(s):
 b=s.encode();return struct.pack('>H',len(b))+b
def tag(t,n,p):return bytes([t])+text(n)+p
def integer(n):return struct.pack('>i',n)
def ints(v):return bytes([3])+integer(len(v))+b''.join(integer(n) for n in v)
def compound(v):return b''.join(v)+b'\0'
def nbt(rows):
 claims={}
 for r in rows:
  p=tuple(int(r[k]) for k in ('x','y','z'));shape=r['shape']
  if p not in claims or shape=='full':claims[p]=shape
 lo=tuple(min(p[i] for p in claims) for i in range(3));hi=tuple(max(p[i] for p in claims) for i in range(3));pal=['full','slab']
 states=['minecraft:stone_bricks','minecraft:stone_brick_slab[type=bottom,waterlogged=false]']
 palette=[]
 for shape,st in zip(pal,states):
  palette.append(compound([tag(8,'id',text(st.split('[')[0]))]+([tag(10,'properties',compound([tag(8,'type',text('bottom')),tag(8,'waterlogged',text('false'))]))] if shape=='slab' else [])))
 blocks=[compound([tag(9,'pos',ints([p[i]-lo[i] for i in range(3)])),tag(3,'state',integer(pal.index(sh)))]) for p,sh in sorted(claims.items())]
 root=compound([tag(3,'DataVersion',integer(5023)),tag(9,'size',ints([hi[i]-lo[i]+1 for i in range(3)])),tag(9,'palette',bytes([10])+integer(2)+b''.join(palette)),tag(9,'blocks',bytes([10])+integer(len(blocks))+b''.join(blocks)),tag(9,'entities',bytes([10])+integer(0))])
 return gzip.compress(tag(10,'',root),mtime=0),claims,lo,hi,dict(zip(pal,states))
def prepare(server):
 pack=server/'world/datapacks/p5';(pack/'data/p5/structure').mkdir(parents=True,exist_ok=True);(pack/'data/p5/function').mkdir(exist_ok=True)
 (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'description':'P5 isolated geometry check','min_format':[121,0],'max_format':[121,0]}}))
 manifests={}
 for c,wx in [('C1',1000),('C2',1100)]:
  blob,claims,lo,hi,states=nbt(list(csv.DictReader((D/f'blocks_{c}.csv').open())))
  (pack/f'data/p5/structure/{c.lower()}.nbt').write_bytes(blob)
  commands=['scoreboard players set errors p5 0'];count=0
  for x in range(lo[0],hi[0]+1):
   for y in range(lo[1],hi[1]+1):
    for z in range(lo[2],hi[2]+1):
     st=states[claims[x,y,z]] if (x,y,z) in claims else 'minecraft:air';count+=1
     commands.append(f'execute unless block {x+wx} {y+90} {z+1000} {st} run scoreboard players add errors p5 1')
  # split into <65536-command chunks; gamerule maximum remains default
  chunks=[]
  for i in range(0,len(commands),10000):
   name=f'{c.lower()}_{i//10000}';chunks.append(name);(pack/f'data/p5/function/{name}.mcfunction').write_text('\n'.join(commands[i:i+10000])+'\n')
  manifests[c]={'positions_checked':count,'blocks':len(claims),'origin':lo,'max':hi,'offset':[wx,90,1000],'functions':chunks,'template_sha256':__import__('hashlib').sha256(blob).hexdigest()}
 (D/'server_manifest.json').write_text(json.dumps(manifests,indent=1)+'\n');print('temporary test pack prepared')
def check(server):
 props=dict(line.split('=',1) for line in (server/'server.properties').read_text().splitlines() if '=' in line)
 os.environ['RCON_PASSWORD']=props['rcon.password'];os.environ['MC_RCON_PORT']='25586'
 spec=importlib.util.spec_from_file_location('rc',H.parents[3]/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(spec);spec.loader.exec_module(rc)
 m=json.loads((D/'server_manifest.json').read_text());results={}
 with rc.connect() as sock:
  sock.settimeout(60)
  n=1
  def ask(cmd):
   nonlocal n
   n+=1;rc.send_packet(sock,n,2,cmd);return rc.read_packet(sock)[2]
  startup={cmd:ask(cmd) for cmd in ('datapack list enabled','scoreboard objectives add p5 dummy')}
  for c,mm in m.items():
   wx,wy,wz=mm['offset'];x,y,z=mm['origin']
   load=ask(f'forceload add {wx-32} {wz-32} {wx+32} {wz+32}')
   placement=ask(f'place template p5:{c.lower()} {wx+x} {wy+y} {wz+z} none none 1.0 0 strict')
   replies=[ask('function p5:'+f) for f in mm['functions']]
   score=ask('scoreboard players get errors p5')
   success='has 0 ' in score and 'Loaded template' in placement and all('Running function' in r for r in replies)
   results[c]={**mm,'forceload':load,'placement':placement,'function_responses':replies,'error_score':score,'status':'PASS' if success else 'CONFLICT','scope':'actual strict placement only; not player movement'}
  results['startup']=startup
 (D/'server_placement.json').write_text(json.dumps(results,indent=1)+'\n');print({k:v.get('status') for k,v in results.items()})
if __name__=='__main__':
 {'prepare':prepare,'check':check}[sys.argv[1]](Path(sys.argv[2]))

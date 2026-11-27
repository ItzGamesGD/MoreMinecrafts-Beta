"""Read-only approved NBT/anchor/resource verification. No builder import or NBT mutation."""
import gzip,hashlib,json,struct,subprocess
from pathlib import Path
H=Path(__file__).resolve().parent;R=H.parents[2]
source=R/'docs/planning/sky_tower_full_build_01/sky_tower_full_01.nbt';installed=R/'src/main/resources/data/moreminecrafts/structure/sky_tower/full_01.nbt'
expected=subprocess.check_output(['git','show','16ad9d5:docs/planning/sky_tower_full_build_01/sky_tower_full_01.nbt'],cwd=R)
assert source.read_bytes()==expected==installed.read_bytes(),'approved architecture bytes changed'
data=gzip.decompress(expected);offset=0
def take(n):
 global offset
 b=data[offset:offset+n];assert len(b)==n;offset+=n;return b
def number(fmt):return struct.unpack(fmt,take(struct.calcsize(fmt)))[0]
def string():return take(number('>H')).decode()
def value(t):
 if t==3:return number('>i')
 if t==8:return string()
 if t==9:
  tag=number('>B');n=number('>i');return [value(tag) for _ in range(n)]
 if t==10:
  out={}
  while (tag:=number('>B'))!=0:
   key=string();out[key]=value(tag)
  return out
 raise ValueError('unexpected NBT tag '+str(t))
assert number('>B')==10;string();root=value(10);assert offset==len(data)
assert root['DataVersion']==5023 and root['size']==[50,80,49]
blocks={tuple(b['pos']):root['palette'][b['state']]['id'] for b in root['blocks']};spawners=[];vaults=[]
for L,f in enumerate((10,21,33,47)):
 for x,z in ((17,17),(17,31),(31,17),(31,31)):
  p=(x,f+2,z);assert blocks[p]=='minecraft:trial_spawner';spawners.append({'index':len(spawners),'floor':L+1,'local':p})
 for z in (15,33):
  p=(16,f+1,z);assert blocks[p]=='minecraft:vault';vaults.append({'index':len(vaults),'floor':L+1,'local':p})
barriers=[]
for L,f in enumerate((10,21,33,47)):
 cells=[(x,y,27) for x in range(39,45) for y in range(f+1,f+4)]
 overlap=[p for p in cells if p in blocks and blocks[p]!='minecraft:air']
 assert not overlap,overlap
 barriers.append({'floor':L+1,'cells':cells,'status':'PROPOSED ONLY — awaiting approval; no blocks changed'})
result={'approved_commit':'16ad9d5','sha256':hashlib.sha256(expected).hexdigest(),'byte_identical':True,'size':root['size'],'occupied':len(blocks),'spawners':spawners,'vaults':vaults,'barrier_proposal':barriers,'natural_generation':'NOT CONFIGURED'}
(H/'anchor_map.json').write_text(json.dumps(result,indent=1)+'\n');print('PASS frozen NBT, 16 spawners, 8 vaults, 72 proposed gate cells are air')

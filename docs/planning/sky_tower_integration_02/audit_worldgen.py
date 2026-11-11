"""Independent read-only NBT/socket/terrain audit; no generator imports or mutations."""
from pathlib import Path
import gzip,struct,json,hashlib
H=Path(__file__).resolve().parent;R=H.parents[2];D=R/'src/main/resources/data/moreminecrafts'
def read(path):
 data=gzip.decompress(path.read_bytes());o=0
 def take(n):
  nonlocal o
  v=data[o:o+n];o+=n;assert len(v)==n;return v
 def num(fmt):return struct.unpack(fmt,take(struct.calcsize(fmt)))[0]
 def string():return take(num('>H')).decode()
 def val(t):
  if t in (1,2,3,4,5,6):return num({1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}[t])
  if t==8:return string()
  if t==9:
   tag=num('>B');return [val(tag) for _ in range(num('>i'))]
  if t==10:
   v={}
   while (tag:=num('>B')):
    key=string();v[key]=val(tag)
   return v
  if t in (7,11,12):return [val({7:1,11:3,12:4}[t]) for _ in range(num('>i'))]
  raise ValueError(t)
 assert num('>B')==10;string();v=val(10);assert o==len(data);return v
def blocks(n):return {tuple(b['pos']):dict(n['palette'][b['state']],nbt=b.get('nbt',{})) for b in n['blocks']}
approved=D/'structure/sky_tower/full_01.nbt';tower=read(approved);tb=blocks(tower)
result={'approved_sha256':hashlib.sha256(approved.read_bytes()).hexdigest(),'tower_size':tower['size'],'styles':{},'old_children':{},'policy':{}}
for style in ('snowy','meadow','forest','badlands','desert','rocky'):
 p=D/f'structure/sky_islands/{style}/tower.nbt';n=read(p);b=blocks(n)
 sockets=[(pos,s['nbt']) for pos,s in b.items() if s['id']=='minecraft:jigsaw']
 assert len(sockets)==1
 socket,meta=sockets[0];cx,sy,cz=socket
 # Align approved centre (24.5,24.5) with the old integral centre. Half-cell difference retained explicitly.
 origin=(cx-24,sy+1,cz-24)
 mapped=[(x+origin[0],y+origin[1],z+origin[2]) for x,y,z in ((0,0,0),(49,79,48))]
 inside=all(0<=p[i]<n['size'][i] for p in mapped for i in range(3))
 # Main entrance apron ends at +25 (NBT49), with a half-step there; island surface below it.
 entrance=(origin[0]+49,sy,origin[2]+24)
 under=[(entrance[0],y,entrance[2]) for y in range(sy+1) if (entrance[0],y,entrance[2]) in b]
 result['styles'][style]={'path':str(p.relative_to(R)),'size':n['size'],'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'socket':socket,'socket_nbt':meta,'provisional_centered_origin':origin,'approved_bounds':mapped,'child_fits_parent_internal_box':inside,'entrance_ground_probe':entrance,'ground_blocks_below_entrance':under,'requires_new_connector':True}
for f in (5,6,7):
 p=D/f'structure/sky_islands/tower_{f}.nbt';n=read(p);b=blocks(n)
 result['old_children'][str(f)]={'size':n['size'],'jigsaws':[(p,s['nbt']) for p,s in b.items() if s['id']=='minecraft:jigsaw']}
for name in ('general','mountain'):
 p=D/f'worldgen/structure_set/sky_islands_{name}.json';result['policy'][name]={'path':str(p.relative_to(R)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'json':json.loads(p.read_text())}
assert all(not s['child_fits_parent_internal_box'] and not s['ground_blocks_below_entrance'] for s in result['styles'].values())
(H/'worldgen_audit.json').write_text(json.dumps(result,indent=1)+'\n');print('CONFIRMED all six original island sockets cannot directly contain the approved tower; centered entrance has no terrain support')

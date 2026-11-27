"""Eight injected defects in independent exported-coordinate checker, no generator import."""
import importlib.util,json,math
from pathlib import Path
H=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('v',H/'p5_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
rows,obj,res=v.read(H/'data','C1');base=v.check(rows,obj,res,'C1',False);results=[]
def pos(r):return tuple(int(r[k]) for k in ('x','y','z'))
def row(g,x,y,z):return dict(group=g,x=str(x),y=str(y),z=str(z),shape='full',facing='')
def check(name,rr,predicate,paths=False,rs=None):
 r=v.check(rr,obj,rs if rs is not None else res,'C1',paths);ok=bool(predicate(r));results.append(dict(name=name,detected=ok));print(('PASS ' if ok else 'FAIL ')+name,flush=True)
# one ordinary block at x22 in bounds, x23 one-block beyond disc
check('N1 one block D45 escape',rows+[row('rail',23,20,0)],lambda r:not r['boundary']['pass'])
# one full block on walking tread consumes headroom (width obstacle, not just a lane test)
p=next(pos(r) for r in rows if r['group']=='stairs' and int(r['x'])==0 and int(r['z'])==17)
check('N2 one block passage intrusion',rows+[row('rail',p[0],p[1]+1,p[2])],lambda r:len(r['headroom']['below_1_8'])>0)
# sever lap2 through whole cross section; no metadata changes
rr=[r for r in rows if not(r['group']=='stairs' and 100<=math.degrees(math.atan2(int(r['z']),int(r['x'])))%360<=112 and 33<=int(r['y'])<=47)]
check('N3 inter-floor cut',rr,lambda r:not r['reach']['reached'][2] and not r['reach']['reached'][3],True)
b=obj['balconies'][0];T=b['top'];rr=rows[:]
for x,z in b['clear_columns']:
 for y in range(math.floor(T),math.ceil(T)+2):rr.append(row('balcony_rail',x,y,z))
check('N4 balcony access blocked',rr,lambda r:not r['reach']['reached'][4] and all(r['reach']['reached'][:4]),True)
a={tuple(p) for p in obj['arches'][5]['cells']}
check('N5 arch pair omitted',[r for r in rows if not(r['group']=='arcade_arch' and pos(r) in a)],lambda r:len(r['arches']['missing'])==1)
# full vertical rail column removed; detect NEW unguarded edge, despite preexisting terminal seams
p=next(pos(r) for r in rows if r['group']=='rail' and int(r['x'])==-20 and int(r['z'])==3 and int(r['y'])==16)
check('N6 fall hole in rail',[r for r in rows if not(r['group']=='rail' and int(r['x'])==p[0] and int(r['z'])==p[2] and abs(int(r['y'])-p[1])<3)],lambda r:bool(set(map(tuple,r['fall_edges']['edges']))-set(map(tuple,base['fall_edges']['edges']))))
p=next(pos(r) for r in rows if r['group']=='tower_slab' and int(r['y'])==21 and int(r['x'])==0 and int(r['z'])==0)
check('N7 slab ownership error',[r for r in rows if not(r['group']=='tower_slab' and pos(r)==p)]+[row('tower_slab',0,20,0)],lambda r:not r['floor_ownership']['pass'])
# reservation initially empty; then an ACTUAL block injected. Do not treat an existing intersected envelope as success.
probe=dict(id='negative_probe',kind='test',min=[0,75,0],max_exclusive=[1,76,1])
clean=v.check(rows,obj,[probe],'C1',False)
assert not clean['reservations']['intersections'][0]['counts']
check('N8 reservation actual-member collision',rows+[row('ornament',0,75,0)],lambda r:r['reservations']['intersections'][0]['counts'].get('ornament')==1,rs=[probe])
(H/'data/negative_controls.json').write_text(json.dumps(results,indent=1)+'\n')
assert all(r['detected'] for r in results)

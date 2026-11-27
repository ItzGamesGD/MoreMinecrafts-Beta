"""Experimental portal-edge guard injection; no P5 files are modified."""
import csv,json,math,importlib.util
from collections import defaultdict
from pathlib import Path
H=Path(__file__).resolve().parent; D=H.parent/'p5/data'
rows=list(csv.DictReader((D/'blocks_C1.csv').open()));cols=defaultdict(dict)
for r in rows:
 x,y,z=(int(r[k]) for k in ('x','y','z'));cols[x,z][y]=(r['shape'],r['group'])
def solids(x,z):
 return [(y,y+(0.5 if shape=='slab' else 1),g) for y,(shape,g) in cols[x,z].items()]
edges=[]
for r in rows:
 if r['group']!='stairs':continue
 x,y,z=(int(r[k]) for k in ('x','y','z'));T=y+(.5 if r['shape']=='slab' else 1)
 if y not in (10,64):continue
 for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
  x2,z2=x+dx,z+dz; blocks=solids(x2,z2)
  if any(abs(b-T)<=.5 for a,b,g in blocks):continue
  # Merge vertically contiguous collision intervals and determine if a guard spans standing height.
  ints=sorted((a,b) for a,b,g in blocks); merged=[]
  for a,b in ints:
   if merged and a<=merged[-1][1]:merged[-1]=(merged[-1][0],max(b,merged[-1][1]))
   else:merged.append((a,b))
  if any(a<=T+.5 and b>=T+1.5 for a,b in merged):continue
  edges.append((x,y,z,x2,z2))
expected={tuple(v) for v in json.loads((D/'verify_C1.json').read_text())['fall_edges']['edges']}
found=set(edges); print('independent',len(found),'P5',len(expected),'only_P6',len(found-expected),'only_P5',len(expected-found))
# Restriction y=10/64 generates extra non-portal edges? record explicit rather than assume.
with (H/'P6_CONFLICT_COORDINATES.csv').open('w',newline='') as f:
 w=csv.writer(f,lineterminator="\n");w.writerow(['x','y','z','neighbor_x','neighbor_z','portal','direction','neighbor_boxes','D45_center_margin','p5_match','guard_trial'])
 for x,y,z,X,Z in sorted(found|expected):
  w.writerow([x,y,z,X,Z,'F1' if y==10 else 'sanctum',f'{X-x},{Z-z}',json.dumps(solids(X,Z)),round(22.5-math.hypot(X,Z),6),(x,y,z,X,Z) in expected,f'({X},{y+1}..{y+2},{Z})'])
# Trial closes flagged faces with two full blocks. This is a diagnostic, never an accepted design.
added={(X,Y,Z) for x,y,z,X,Z in expected for Y in (y+1,y+2)}
trial=rows+[dict(group='rail',x=str(x),y=str(y),z=str(z),shape='full',facing='') for x,y,z in sorted(added)]
with (H/'candidate_guard_C1.csv').open('w',newline='') as f:
 w=csv.DictWriter(f,fieldnames=list(rows[0]),lineterminator='\n');w.writeheader();w.writerows(trial)
s=importlib.util.spec_from_file_location('p5v',H.parent/'p5/p5_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
_,obj,res=v.read(D,'C1');result=v.check(trial,obj,res,'C1',True); result.pop('witness_paths',None)
(H/'candidate_guard_checks.json').write_text(json.dumps(result,indent=1)+'\n')
print('trial', {k:result[k].get('pass',result[k].get('pass_all')) for k in ('fall_edges','reach','overlaps','boundary','headroom')})
print('only_P6',sorted(found-expected),'only_P5',sorted(expected-found))

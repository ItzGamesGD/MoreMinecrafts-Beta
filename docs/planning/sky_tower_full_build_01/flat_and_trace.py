"""Final balcony floors and actual recorded main-route body-envelope replay."""
import csv,json,math,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;rows=list(csv.DictReader((H/'blocks.csv').open()));b={tuple(int(r[k]) for k in ('x','y','z')):r for r in rows};model=json.loads((H/'model.json').read_text());bad=[]
for i,a in enumerate(model['balconies']):
 for x,z in a['clear']:
  T=a['top'];y=math.ceil(T)-1;r=b.get((x,y,z));actual=y+(.5 if r and r['block'].endswith('_slab') else 1)
  if not r or actual!=T:bad.append([i+1,x,y,z,T,actual])
s=importlib.util.spec_from_file_location('v',H.parent/'sky_tower_geometry_03c/p3/p3_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
scene=v.SlabScene({p:('slab' if r['block'].endswith('_slab') else 'full','') for p,r in b.items()});live=json.loads((H/'live_walk.json').read_text());trace=live['trace'];failed=[];epsilon=.00001
for i,t in enumerate(trace):
 x,h,z=(t[1]-1000)*40,(t[2]-65)*40,(t[3]-1000)*40
 if not scene.clear_box((x-12+epsilon,h+epsilon,z-12+epsilon,x+12-epsilon,h+72-epsilon,z+12-epsilon)):failed.append(i)
out={'NBT_sha256':model['sha256'],'flat_columns_expected':168,'flat_defects':bad,'actual_main_route_samples':len(trace),'body_collision_samples':failed,'numeric_epsilon_blocks':epsilon/40,'minimum_actual_height_delta':min(t[2]-a[2] for a,t in zip(trace,trace[1:])),'scope':'actual previously recorded main route replayed against final occupancy after external balcony-floor-only repair; not a new client run'}
(H/'flat_and_trace.json').write_text(json.dumps(out,indent=1)+'\n');print(out);assert not bad and not failed

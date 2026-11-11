"""Two disjoint radial lane witnesses; static geometry, not a two-client test."""
import csv,json,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('v',H.parent/'sky_tower_geometry_03c/p3/p3_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
b={tuple(int(r[k]) for k in ('x','y','z')):('slab' if r['block'].endswith('_slab') else 'full','') for r in csv.DictReader((H/'blocks.csv').open())};scene=v.SlabScene(b);floors=[0,10,21,33,47,64];out=[]
for L,(a,b) in enumerate(zip(floors,floors[1:])):
 for lane in ((16,17),(18,19)):
  result=v.lane_bfs(scene,lane,a+1,b+1,(16,22),(346,355),b+1)
  out.append({'flight':L,'lane':lane,**result})
(H/'passing.json').write_text(json.dumps({'lanes':out,'two_clients_live':'NOT RUN','scope':'two disjoint sampled radial lanes, independent player AABB routes only; no proof of all local passing configurations'},indent=1)+'\n');print([(r['flight'],r['lane'],r['pass']) for r in out])

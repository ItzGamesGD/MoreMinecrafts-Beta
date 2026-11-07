"""Independent six-neighbour voxel component audit; intended relic exempted explicitly."""
import csv,json
from collections import deque
from pathlib import Path
H=Path(__file__).resolve().parent;rr=list(csv.DictReader((H/'blocks.csv').open()));b={tuple(int(r[k]) for k in ('x','y','z')):r for r in rr};left=set(b);components=[]
while left:
 start=min(left);left.remove(start);q=deque([start]);cells=[]
 while q:
  x,y,z=q.popleft();cells.append((x,y,z))
  for p in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
   if p in left:left.remove(p);q.append(p)
 components.append(cells)
components.sort(key=len,reverse=True);out=[]
for cells in components[1:]:
 groups=sorted({b[p]['group'] for p in cells});out.append({'count':len(cells),'groups':groups,'sample':cells[:8],'intended_relic':all(g in ('floating_relic','relic_core') for g in groups)})
result={'main_component':len(components[0]),'other_components':out,'unintended':sum(not c['intended_relic'] for c in out),'note':'Voxel face connectivity; slab half-height support also needs physical audit.'};(H/'connectivity.json').write_text(json.dumps(result,indent=1)+'\n');print('unintended',result['unintended'],[x for x in out if not x['intended_relic']][:5])

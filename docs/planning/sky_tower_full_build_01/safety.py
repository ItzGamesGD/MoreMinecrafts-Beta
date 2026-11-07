"""Independent exposed walking-face audit, reads final blocks; no generator imports."""
import csv,json,math
from collections import defaultdict
from pathlib import Path
H=Path(__file__).resolve().parent;rows=list(csv.DictReader((H/'blocks.csv').open()));cols=defaultdict(list)
for r in rows:
 x,y,z=(int(r[k]) for k in ('x','y','z'));T=y+(.5 if r['block'].endswith('_slab') else 1);cols[x,z].append((y,T,r['group']))
falls=[]
for r in rows:
 if r['group'] not in ('stair','portal_floor','floor','balcony','entrance'):continue
 x,y,z=(int(r[k]) for k in ('x','y','z'));T=y+(.5 if r['block'].endswith('_slab') else 1)
 if any(a<T+1.8 and b>T+1e-8 for a,b,g in cols[x,z]):continue
 for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
  X,Z=x+dx,z+dz;iv=sorted((a,b) for a,b,g in cols[X,Z]);merged=[]
  for a,b in iv:
   if merged and a<=merged[-1][1]:merged[-1]=(merged[-1][0],max(merged[-1][1],b))
   else:merged.append((a,b))
  if any(abs(b-T)<=.5 for a,b in iv) or any(a<=T+.5 and b>=T+1.5 for a,b in merged):continue
  falls.append([x,T,z,X,Z])
(H/'safety.json').write_text(json.dumps({'exposed_faces':falls,'count':len(falls),'definition':'standable-column four-neighbour faces; actual diagonal movement and live pushes separate'},indent=1)+'\n');print('exposed faces',len(falls),falls[:20])

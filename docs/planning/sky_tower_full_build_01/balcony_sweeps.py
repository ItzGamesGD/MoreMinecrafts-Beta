"""Continuous swept 0.6x1.8 AABB on eight straight, flat balcony connectors."""
import csv,json
from pathlib import Path
def colliders(rows,b):
 T=b['top'];sg=b['sign'];lo,hi=sorted((.5+sg*17.5,.5+sg*22));hits=[]
 for r in rows:
  x,y,z=(int(r[k]) for k in ('x','y','z'));top=y+(.5 if r['block'].endswith('_slab') else 1)
  if x<.8 and x+1>.2 and y<T+1.8 and top>T and lo-.3<z+1 and hi+.3>z:hits.append([x,y,z,r['group']])
 return hits

H=Path(__file__).resolve().parent
if __name__=='__main__':
 rows=list(csv.DictReader((H/'blocks.csv').open()));out=[{'balcony':i+1,'colliders':colliders(rows,b)} for i,b in enumerate(json.loads((H/'model.json').read_text())['balconies'])]
 (H/'balcony_sweeps.json').write_text(json.dumps(out,indent=1)+'\n');print(out);assert all(not r['colliders'] for r in out)

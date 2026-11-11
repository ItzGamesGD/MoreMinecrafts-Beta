"""Bounded one-block translations of the identified pillar only; no arch redesign."""
import csv,json,copy,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;D=H.parent/'p5/data'
s=importlib.util.spec_from_file_location('v',H.parent/'p5/p5_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
rows,obj,res=v.read(D,'C1');p=next(p for p in obj['pillars'] if p['id']=='P1-7');cells={tuple(c) for c in p['cells']};results=[]
for dx,dz in ((-1,0),(1,0),(0,-1),(0,1),(-1,-1),(-1,1),(1,-1),(1,1)):
 rr=[]
 for r in rows:
  t=dict(r);xyz=tuple(int(r[k]) for k in ('x','y','z'))
  if r['group']=='pillar' and xyz in cells:t['x']=str(xyz[0]+dx);t['z']=str(xyz[2]+dz)
  rr.append(t)
 ob=copy.deepcopy(obj);post=next(q for q in ob['pillars'] if q['id']=='P1-7');post['xz']=[p['xz'][0]+dx,p['xz'][1]+dz];post['cells']=[[x+dx,y,z+dz] for x,y,z in p['cells']]
 c=v.check(rr,ob,res,'C1',False)
 results.append({'dx':dx,'dz':dz,'boundary':c['boundary']['pass'],'overlaps':c['overlaps']['pass'],'arches':c['arches']['pass'],'fall_edges':c['fall_edges']['count'],'headroom':c['headroom']['pass'],'width':'NOT RUN','reach':'NOT RUN','note':'single pillar translation with original arch fixed; diagnostic only'})
(H/'pillar_candidates.json').write_text(json.dumps(results,indent=1)+'\n');print(json.dumps(results))

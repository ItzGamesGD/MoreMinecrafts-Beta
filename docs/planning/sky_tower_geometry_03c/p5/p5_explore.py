"""Bounded local balcony alternatives: translate entire 7x3+ring assembly tangentially ±3.
Does not accept/reject overall design. Global Q43 failure applies to every baseline-preserving variant.
"""
import csv,json,math
from pathlib import Path
H=Path(__file__).resolve().parent;D=H/'data'
rows=list(csv.DictReader((D/'blocks_C1.csv').open()));obj=json.loads((D/'objects_C1.json').read_text());records=[];coords=[]
for b in obj['balconies']:
 original=set(map(tuple,b['clear_columns']));xs,zs=zip(*original);horiz=max(xs)-min(xs)==6; T=b['top']
 for shift in range(-3,4):
  dx,dz=(shift,0) if horiz else (0,shift);cl={(x+dx,z+dz) for x,z in original}
  X,Z=zip(*cl);ring={(x,z) for x in range(min(X)-1,max(X)+2) for z in range(min(Z)-1,max(Z)+2)}-cl
  band={(int(r['x']),int(r['z'])):int(r['y'])+(.5 if r['shape']=='slab' else 1) for r in rows if r['group']=='stairs' and abs(int(r['y'])-T)<2}
  contact={p for p in ring if p in band and any(abs(p[0]-x)+abs(p[1]-z)==1 for x,z in cl)}
  foot=cl|(ring-set(band)); reasons=[]
  if any(4*(x*x+z*z)>=51**2 for x,z in cl|ring):reasons.append('D51')
  if any(15.5<=math.hypot(x,z)<19.5 for x,z in cl):reasons.append('walk_band_overlap')
  if not contact:reasons.append('no_contact')
  if any(abs(band[p]-T)>1e-8 for p in contact):reasons.append('not_flat_at_existing_top')
  hits=[r for r in rows if r['group'] in ('pillar','arcade_arch','rail') and (int(r['x']),int(r['z'])) in foot and T-2<=int(r['y'])<T+2]
  if hits:reasons.append('existing_member_intersection')
  centre=(sum(x for x,z in cl)/21,sum(z for x,z in cl)/21)
  offsets=[abs((p[0]-centre[0]) if horiz else (p[1]-centre[1])) for p in contact]
  score=sum(offsets)/len(offsets) if offsets else None
  records.append(dict(id=b['id'],shift=shift,opening_columns=len(contact),mean_long_axis_offset=score,local_rejections=';'.join(reasons),global_status='CONFLICT: unchanged K2 width witness <4',note='Local screen only; rail edge sweep, 3D BFS and full template not certified for alternatives'))
  for x,z in sorted(cl|ring):coords.append(dict(id=b['id'],shift=shift,x=x,y=math.floor(T) if T%1 else int(T)-1,z=z,claim='clear_floor' if (x,z) in cl else 'ring_candidate'))
for name,rr in [('balcony_alternatives.csv',records),('balcony_alternative_coordinates.csv',coords)]:
 with (D/name).open('w') as f:
  w=csv.DictWriter(f,fieldnames=list(rr[0]),lineterminator='\n');w.writeheader();w.writerows(rr)
# actual baseline openings, no metadata-only success
out=[]
for b in obj['balconies']:
 rr=next(r for r in records if r['id']==b['id'] and r['shift']==0);rr=dict(rr);rr['top']=b['top'];rr['origin']=b['origin'];rr['landing_deg']=b['landing_deg'];out.append(rr)
(D/'balcony_connections.json').write_text(json.dumps(out,indent=1)+'\n')
print('56 local alternatives retained; no globally certified alternative')

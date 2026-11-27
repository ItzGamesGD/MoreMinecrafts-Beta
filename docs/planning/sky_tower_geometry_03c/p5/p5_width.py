"""Independent exact radial ray/AABB intervals at sampled angles; no generator import.
A sample is exact in its radial coordinate, NOT a global angular minimum certificate.
Physical span uses point cross-section, centre span uses world-axis 0.6 square Minkowski boxes.
Support: point footprint at reference height ±0.5. Full-footprint support NOT certified.
"""
import math,csv,json
from collections import defaultdict
from pathlib import Path

def ray_box(dx,dz,box,pad=0):
 lo,hi=0.,30.
 for d,a,b in [(dx,box[0]-pad,box[2]+pad),(dz,box[1]-pad,box[3]+pad)]:
  if abs(d)<1e-12:
   if not a<=0<=b:return None
  else:
   u,v=sorted((a/d,b/d));lo=max(lo,u);hi=min(hi,v)
 if hi-lo<=1e-9:return None
 return lo,hi

def union(iv):
 out=[]
 for a,b in sorted(iv):
  if out and a<=out[-1][1]+1e-9:out[-1]=(out[-1][0],max(b,out[-1][1]))
  else:out.append((a,b))
 return out

def subtract(support,blocked):
 out=[]
 for a,b in union(support):
  for c,d in union(blocked):
   if d<=a or c>=b:continue
   if c>a:out.append((a,c))
   a=max(a,d)
   if a>=b:break
  if a<b:out.append((a,b))
 return out

def analyse(rows,obj,step=.25):
 cols=defaultdict(list)
 for r in rows:
  x,y,z=(int(r[k]) for k in ('x','y','z')); top=y+(.5 if r['shape']=='slab' else 1)
  assert r['shape'] in ('full','slab'), 'Only K2 full/bottom slab supported; never silently approximate stairs'
  cols[x,z].append((y,top,r['group']))
 dy=obj['transform']['dy']; records=[]
 for L,(f0,f1) in enumerate(zip(obj['floors'],obj['floors'][1:])):
  for i in range(round(360/step)):
   deg=i*step; a=math.radians(deg); dx,dz=math.cos(a),math.sin(a)
   x,z=math.floor(.5+17.5*dx),math.floor(.5+17.5*dz)
   ts=[t for y,t,g in cols[x,z] if g=='stairs' and f0+1<=t<=f1+1]
   if not ts:continue
   # portal seam belongs to both levels: choose lower at start, upper at end
   h=min(ts) if deg<180 else max(ts)
   # Lift at most one half-step to the highest intersected tread on this section.
   cross=[]
   for X in range(-21,22):
    for Z in range(-21,22):
     iv=ray_box(dx,dz,(X-.5,Z-.5,X+.5,Z+.5))
     if iv and iv[1]>15.5 and iv[0]<19.5:
      cross.extend(t for y,t,g in cols.get((X,Z),[]) if g=='stairs' and abs(t-h)<=.5)
   if cross:h=max(cross)
   near=[];support=[]
   for X in range(-21,22):
    for Z in range(-21,22):
     box=(X-.5,Z-.5,X+.5,Z+.5)
     if not ray_box(dx,dz,box,.3):continue
     for y,t,g in cols.get((X,Z),[]):
      iv=ray_box(dx,dz,box)
      if g=='stairs' and abs(t-h)<=.5 and iv:support.append((max(14.,iv[0]),min(21.,iv[1])))
      if t>h+1e-8 and y<h+1.8-1e-8:near.append((box,(X,y,Z,g)))
   result=[]
   for pad in (0,.3):
    blocked=[v for box,p in near if (v:=ray_box(dx,dz,box,pad))]
    spans=subtract([(a,b) for a,b in support if a<b],blocked)
    # longest supported clear component intersecting nominal ring centre
    spans=[s for s in spans if s[1]>15.5 and s[0]<19.5]
    result.append(max(spans,key=lambda s:s[1]-s[0],default=(0.,0.)))
   physical,centre=result
   records.append({'lap':L,'angle_deg':deg,'feet_y':h,'physical_lo':physical[0],'physical_hi':physical[1],'physical_width':physical[1]-physical[0],'centre_lo':centre[0],'centre_hi':centre[1],'centre_width':centre[1]-centre[0], 'limiting_boxes':json.dumps([p for box,p in near if any(iv and any(abs(v-e)<1e-7 for v in iv for e in result[j]) for j,pad in enumerate((0,.3)) for iv in [ray_box(dx,dz,box,pad)])],separators=(',',':'))})
 return records
if __name__=='__main__':
 H=Path(__file__).resolve().parent
 for c in ('C1','C2'):
  rows=list(csv.DictReader((H/'data'/f'blocks_{c}.csv').open()));obj=json.loads((H/'data'/f'objects_{c}.json').read_text())
  rr=analyse(rows,obj)
  with (H/'data'/f'width_{c}.csv').open('w') as f:
   w=csv.DictWriter(f,fieldnames=list(rr[0]),lineterminator='\n');w.writeheader();w.writerows(rr)
  summary={str(L):{key:min((r for r in rr if r['lap']==L),key=lambda r:r[key]) for key in ('physical_width','centre_width')} for L in range(4)}
  (H/'data'/f'width_summary_{c}.json').write_text(json.dumps(summary,indent=1)+'\n')
  print(c,[(L,round(v['physical_width']['physical_width'],6),round(v['centre_width']['centre_width'],6)) for L,v in summary.items()])

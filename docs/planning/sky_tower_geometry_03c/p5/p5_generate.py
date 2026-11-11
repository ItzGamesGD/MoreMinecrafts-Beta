"""Export immutable K2 + B25-T3 claims. Reservations are trial envelopes, never blocks."""
import csv, json, hashlib, argparse
from pathlib import Path
H=Path(__file__).resolve().parent
SRC=H.parent/'p3/data/candidates/K2_8balc_A5relaxed_slab_land3'
BASE=H.parents[1]/'sky_tower_geometry_02/geometry02b/baseline.json'
def dump(p,v): p.write_text(json.dumps(v,ensure_ascii=False,indent=1)+'\n')
def main(out):
 out.mkdir(parents=True,exist_ok=True); base=json.loads(BASE.read_text()); hashes={}
 for p in [*sorted(SRC.glob('*')),BASE]:
  if p.is_file(): hashes[str(p.relative_to(H.parents[3]))]=hashlib.sha256(p.read_bytes()).hexdigest()
 for conv,dy in [('C1',0),('C2',-1)]:
  rows=list(csv.DictReader((SRC/f'blocks_{conv}.csv').open()))
  floors=[10+dy,21+dy,33+dy,47+dy,64+dy]
  for layer in base['models'][conv]['layers']:
   y=layer['y']; cols=base['columns']['wall'] if layer['zone']=='WALL' else base['columns']['wall']+base['columns']['inner']
   for x,z in cols:
    if layer['zone']=='WALL' and 12<=x<=15 and -1<=z<=2 and any(f+1<=y<f+8 for f in floors[:4]): continue
    rows.append(dict(group='tower_'+layer['zone'].lower(),x=x,y=y,z=z,shape='full',facing=''))
  with (out/f'blocks_{conv}.csv').open('w') as f:
   w=csv.DictWriter(f,fieldnames=['group','x','y','z','shape','facing'],lineterminator='\n');w.writeheader();w.writerows(rows)
  obj=json.loads((SRC/f'objects_{conv}.json').read_text()); dump(out/f'objects_{conv}.json',obj)
  dump(out/f'layers_{conv}.json',base['vertical'][conv])
  reservations=[]
  def add(id,kind,lo,hi,note): reservations.append(dict(id=id,kind=kind,min=lo,max_exclusive=hi,status='UNVERIFIED',note=note))
  for i,f in enumerate(floors[:4]):
   for kind,x0,x1,z0,z1,a,b in [('wall_link_arch',14,22,-3,4,3,9),('partial_canopy_steps',14,23,-4,5,8,11),('horizontal_moulding',14,23,-4,5,8,9),('gate_double_arch',12,17,-3,4,3,9),('gate_shallow_pilasters',14,17,-4,5,1,7),('gate_geometric_relief',14,17,-3,4,7,10)]:
    add(f'F{i+1}_{kind}',kind,[x0,f+a,z0],[x1,f+b,z1],'Trial envelope; joint faces may overlap tower. Dimensions/shape unapproved; apex must eventually align with adjacent arcade (Q107/Q111).')
  for b in obj['balconies']:
   xs,zs=zip(*b['clear_columns']); t=b['top']+dy
   add(b['id']+'_support','balcony_stepped_support_and_central_arch',[min(xs)-1,int(t)-4,min(zs)-1],[max(xs)+2,int(t)-1,max(zs)+2],'Trial depth 1–3 below existing support. No new ground-reaching columns.')
  add('base_approach','base_access',[12,0,-8],[23,11,9],'Unplaced Q13 twin approach stairs; footprint not decided. This is a search box, not approved stair dimensions.')
  add('sanctum_ring','roofless_sanctum_arches',[-15,65+dy,-15],[16,80,16],'Broad search envelope for 12 arches. NOT a solid box, roof, or proof of apex at y79.')
  dump(out/f'reservations_{conv}.json',reservations)
 dump(out/'source_hashes.json',hashes)
 dump(out/'model.json',{'axis':[.5,.5],'budget_y':[0,80],'actual_vs_reserved':'CSV contains claims only; reservation boxes contain no blocks. Existing K2 unchanged. Tower portal voids reproduce P3 baseline.', 'base_diameter':'B25-T3 core only; stepped D45 podium detail unresolved','sanctum':'floor only; upper arches unplaced'})
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--out',type=Path,default=H/'data');main(ap.parse_args().out)

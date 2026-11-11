"""Independent integrated export verifier. No import of P5/P3 generators.
Frozen P3 SlabScene reused for swept 0.6x1.8 AABB BFS (quarter-grid, no jumping).
"""
import csv,json,math,importlib.util,argparse
from collections import defaultdict,deque
from pathlib import Path
H=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('v3',H.parent/'p3/p3_verify.py');v3=importlib.util.module_from_spec(s);s.loader.exec_module(v3)
BASE=H.parents[1]/'sky_tower_geometry_02/geometry02b/baseline.json'
def read(folder,c):
 with (folder/f'blocks_{c}.csv').open() as f:rows=list(csv.DictReader(f))
 return rows,json.loads((folder/f'objects_{c}.json').read_text()),json.loads((folder/f'reservations_{c}.json').read_text())
def check(rows,obj,res,conv,paths=True):
 groups=defaultdict(dict);claims=defaultdict(list)
 for r in rows:
  p=tuple(int(r[k]) for k in ('x','y','z'));groups[r['group']][p]=(r['shape'],r['facing']);claims[p].append((r['group'],r['shape']))
 blocks={p:('full' if any(s=='full' for g,s in cl) else 'slab','') for p,cl in claims.items()}
 scene=v3.SlabScene(blocks);C={};dy=obj['transform']['dy'];floors=[10+dy,21+dy,33+dy,47+dy,64+dy]
 C['boundary']={'outside':[[g,*p] for g,bb in groups.items() for p in bb if 4*(p[0]**2+p[2]**2)>=(51 if g.startswith('balcony') else 45)**2]}
 C['boundary']['pass']=not C['boundary']['outside']
 normal={(x,z) for g,bb in groups.items() if not g.startswith('balcony') for x,y,z in bb}
 balcony={(x,z) for g,bb in groups.items() if g.startswith('balcony') for x,y,z in bb}
 C['boundary']['whole_block_alternative']={'normal_D45_outside_columns':sum((2*abs(x)+1)**2+(2*abs(z)+1)**2>45**2 for x,z in normal),'balcony_D51_outside_columns':sum((2*abs(x)+1)**2+(2*abs(z)+1)**2>51**2 for x,z in balcony)}
 overlaps=defaultdict(list)
 for p,cl in claims.items():
  gs=sorted(set(g for g,s in cl))
  for i,a in enumerate(gs):
   for b in gs[i+1:]:overlaps[a+'&'+b].append(p)
 allowed={'arcade_arch&pillar','pillar&rail','stairs&tower_slab','stairs&tower_base_core'}
 C['overlaps']={'pairs':{k:len(v) for k,v in overlaps.items()},'intentional_candidate_joins':sorted(allowed),'unexpected':{k:v for k,v in overlaps.items() if k not in allowed}}
 # A same-voxel join must have identical collision shape, and portal floor joins
 # must be in the actual 5x4 bridge footprint at one of the five floor levels.
 invalid=[]
 for p,cl in claims.items():
  gs={g for g,sh in cl}
  if len(gs)<2:continue
  if len({sh for g,sh in cl})>1:invalid.append([*p,'mixed_collision_shapes'])
  if any(g.startswith('tower_') for g in gs) and 'stairs' in gs:
   if not(11<=p[0]<=15 and -1<=p[2]<=2 and p[1] in floors):invalid.append([*p,'outside_portal_floor_join'])
 C['overlaps']['invalid_joins']=invalid
 C['overlaps']['pass']=not C['overlaps']['unexpected'] and not invalid
 base=json.loads(BASE.read_text());expected={}
 for layer in base['models'][conv]['layers']:
  if layer['zone']=='SLAB':expected[layer['y']]=set(map(tuple,base['columns']['inner']))|set(map(tuple,base['columns']['wall']))
 missing=[[x,y,z] for y,cols in expected.items() for x,z in cols if (x,y,z) not in groups['tower_slab']]
 extra=[list(p) for p in groups['tower_slab'] if p[1] not in expected or (p[0],p[2]) not in expected[p[1]]]
 C['floor_ownership']={'pass':not missing and not extra,'missing':missing[:50],'extra':extra[:50],'slab_ys':sorted(expected)}
 ys=[p[1] for p in claims];C['height']={'occupied_y':[min(ys),max(ys)],'occupied_height':max(ys)-min(ys)+1,'budget_height':80,'pass':min(ys)==0 and max(ys)==79,'status':'UNVERIFIED: reserved apex is not a placed block'}
 # Arch components recomputed from occupied voxels, metadata supplies only pair endpoints.
 arch=set(groups['arcade_arch']);comp={};i=0
 for p in sorted(arch):
  if p in comp:continue
  i+=1;comp[p]=i;q=deque([p])
  while q:
   x,y,z=q.popleft()
   for n in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
    if n in arch and n not in comp:comp[n]=i;q.append(n)
 def touched(post):
  ids=set()
  for x,Y,z in post['cells']:
   y=Y+dy
   if (x,y,z) not in groups['pillar']:continue
   for n in ((x,y,z),(x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
    if n in comp:ids.add(comp[n])
  return ids
 posts=obj['pillars'];touch={p['id']:touched(p) for p in posts}
 missing_pairs=[[a['id'],b['id']] for a,b in zip(posts,posts[1:]) if not touch[a['id']]&touch[b['id']]]
 C['arches']={'pairs':len(posts)-1,'missing':missing_pairs,'pass':not missing_pairs and len(posts)==48}
 cols=defaultdict(list)
 for (x,y,z),(shape,_) in blocks.items():cols[x,z].append((y,y+(.5 if shape=='slab' else 1)))
 heads=[];falls=[]
 clear_bal={(x,math.floor(b['top']+dy) if (b['top']+dy)%1 else int(b['top']+dy)-1,z) for b in obj['balconies'] for x,z in b['clear_columns']}
 walking={**groups['stairs'],**{p:sh for p,sh in groups['balcony_floor'].items() if p in clear_bal}}
 for (x,y,z),(shape,_) in walking.items():
  T=y+(.5 if shape=='slab' else 1)
  above=[a-T for a,b in cols[x,z] if b>T+1e-8]
  if above:heads.append((min(above),(x,y,z)))
  for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
   n=(x+dx,z+dz)
   neighbour=cols[n]
   # same-level extension OR >=1.5 high solid guard; do not count a floor far below as a guard
   supported=any(abs(b-T)<=.5 for a,b in neighbour)
   guard=any(a<=T+.5 and b>=T+1.5 for a,b in neighbour)
   # guard may comprise two contiguous full blocks
   guard=guard or any(a<=T+.5 and b>=T+.5 and any(abs(c-b)<1e-8 and d>=T+1.5 for c,d in neighbour) for a,b in neighbour)
   if not supported and not guard:falls.append([x,y,z,n[0],n[1]])
 C['headroom']={'minimum':min(heads) if heads else None,'below_1_8':[v for v in heads if v[0]<1.8],'pass':not any(v[0]<1.8 for v in heads)}
 C['fall_edges']={'count':len(falls),'edges':falls,'pass':not falls,'note':'face-edge test only; diagonal dynamics require live testing'}
 rcoll=[]
 for r in res:
  lo,hi=r['min'],r['max_exclusive'];hit=defaultdict(list)
  for p,cl in claims.items():
   if all(a<=v<b for a,v,b in zip(lo,p,hi)):
    for g,sh in cl:hit[g].append(p)
  D=51 if r['kind']=='balcony_stepped_support_and_central_arch' else 45
  outside=[[x,z] for x in range(lo[0],hi[0]) for z in range(lo[2],hi[2]) if 4*(x*x+z*z)>=D*D]
  rcoll.append({'id':r['id'],'trial_box_outside_boundary_columns':len(outside),'counts':{g:len(v) for g,v in hit.items()},'samples':{g:v[:8] for g,v in hit.items()}})
 C['reservations']={'status':'UNVERIFIED','intersections':rcoll,'note':'Trial outer envelopes overlap intentionally at attachments and potentially at passage; no shape feasibility proof.'}
 C['balconies']={'count':len(obj['balconies']),'items':[]}
 for b in obj['balconies']:
  T=b['top']+dy;cl=b['clear_columns'];ok=[]
  for x,z in cl:
   ok.append(scene.stand(4*x+1,4*z+1,round(T*40)))
  C['balconies']['items'].append({'id':b['id'],'columns':len(cl),'standable':sum(ok),'pass':len(cl)==21 and all(ok)})
 C['balconies']['pass']=len(obj['balconies'])==8 and all(b['pass'] for b in C['balconies']['items'])
 if paths:
  goals=[(45,f+1,1) for f in floors[1:]];names=['F2','F3','F4','SANCTUM']
  for b in obj['balconies']:
   cl=b['clear_columns'];X=round(sum(x for x,z in cl)/21);Z=round(sum(z for x,z in cl)/21)
   goals.append((4*X+1,b['top']+dy,4*Z+1));names.append(b['id'])
  nr=scene.search((45,floors[0]+1,1),goals,limit=600000)
  C['reach']={k:v for k,v in nr.items() if k!='witness_paths'};C['reach'].update(names=names,pass_all=all(nr['reached']))
  C['witness_paths']=dict(zip(names,nr['witness_paths']))
  def reverse_ok(path):
   if not path:return False
   rev=list(reversed(path))
   for a,b in zip(rev,rev[1:]):
    x,h,z=[round(v*40) for v in a];X,H,Z=[round(v*40) for v in b];top=max(h,H)
    if not h-40<=H<=h+20:return False
    if not scene.clear_box((min(x,X)-12,top,min(z,Z)-12,max(x,X)+12,top+72,max(z,Z)+12)):return False
    if H>h and not scene.clear_box((x-12,h,z-12,x+12,H+72,z+12)):return False
   return True
  C['reach']['reverse_witness_replay']={n:reverse_ok(p) for n,p in zip(names,nr['witness_paths'])}
 C['base_access']={'status':'UNVERIFIED','reason':'No Q13 approach stair geometry exists. F1-start BFS does not establish ground→F1.'}
 return C
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--folder',type=Path,default=H/'data');a=ap.parse_args()
 for c in ('C1','C2'):
  r=check(*read(a.folder,c),c);paths=r.pop('witness_paths')
  (a.folder/f'verify_{c}.json').write_text(json.dumps(r,indent=1)+'\n');(a.folder/f'paths_{c}.json').write_text(json.dumps(paths,separators=(',',':'))+'\n')
  print(c,{k:v.get('pass',v.get('pass_all',v.get('status'))) for k,v in r.items()})

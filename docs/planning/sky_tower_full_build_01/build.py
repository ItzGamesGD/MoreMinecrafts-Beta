"""Standalone 26.3 full-tower candidate. No runtime/worldgen changes."""
import math,json,csv,gzip,struct,hashlib
from pathlib import Path
H=Path(__file__).resolve().parent;B={};anchors=[];walk=[]
def put(x,y,z,mat,group='detail',props=None,nbt=None):
 if 0<=y<80:B[round(x),y,round(z)]=(mat,props or {},nbt,group)
def box(x0,y0,z0,x1,y1,z1,mat,group='detail'):
 for x in range(x0,x1+1):
  for y in range(y0,y1+1):
   for z in range(z0,z1+1):put(x,y,z,mat,group)
def disc(r):return {(x,z) for x in range(-30,31) for z in range(-30,31) if x*x+z*z<r*r}
def tread(x,z,T,group='stair'):
 y=math.ceil(T)-1;put(x,y,z,'stone_brick_slab' if T%1 else 'stone_bricks',group,{'type':'bottom','waterlogged':'false'} if T%1 else {})
 walk.append([x,T,z])
def top(L,a):
 f=[0,10,21,33,47,64];lo,hi=f[L]+1,f[L+1]+1
 # Flat portal and two balcony landings. Integral of nonflat angular intervals.
 flats=[(0,15),(345,360)]
 if L:flats += [(80,104),(260,284)]
 length=sum(b-a for a,b in flats);m=a
 for u,v in flats:m-=max(0,min(a,v)-u)
 return lo+math.floor(2*(hi-lo)*max(0,m)/(360-length)+1e-8)/2
# Foundation core, battle slabs and faceted exterior shell.
for y in range(10):
 for x,z in disc(14.5):put(x,y,z,'polished_andesite' if y%3==0 else 'stone_bricks','podium')
floors=[10,21,33,47,64]
for f in floors:
 for x,z in disc(14.5):
  r=math.hypot(x,z);mat='smooth_stone' if int(r)%3 else 'polished_deepslate'
  if abs(x)==abs(z) or x==0 or z==0:mat='polished_andesite'
  put(x,f,z,mat,'floor')
for L,f in enumerate(floors[:4]):
 ceiling=floors[L+1]
 for x,z in disc(14.5)-disc(13):
  a=math.degrees(math.atan2(z,x))%360
  for y in range(f+1,ceiling):
   # Slim arched clerestory windows; full piers between openings.
   window=abs((a+22.5)%45-22.5)<7 and f+4<=y<ceiling-2
   if not window:put(x,y,z,'stone_bricks' if int(a)%45>8 else 'polished_andesite','wall')
 # Slightly recessed ring cornice within wall envelope; never protrudes into spiral.
 for y in (f+1,ceiling-1):
  for x,z in disc(14.5)-disc(13.5):put(x,y,z,'smooth_stone','cornice')
 # Interior eight columns, with bases/capitals, and connecting ceiling ribs.
 for k in range(8):
  a=math.radians(k*45+22.5);cx,cz=round(11.5*math.cos(a)),round(11.5*math.sin(a))
  box(cx-1,f+1,cz-1,cx+1,f+1,cz+1,'polished_andesite','inner_column')
  box(cx,f+2,cz,cx,ceiling-3,cz,'smooth_stone','inner_column')
  box(cx-1,ceiling-2,cz-1,cx+1,ceiling-2,cz+1,'chiseled_stone_bricks','inner_column')
 # Central mosaic at diameter 11; outer four functional altars, clear central arena.
 for x,z in disc(5.5):
  mat='oxidized_cut_copper' if max(abs(x),abs(z))==4 and (x==0 or z==0) else ('polished_deepslate' if int(math.hypot(x,z))%2 else 'smooth_stone')
  put(x,f,z,mat,'mosaic')
 for k,(x,z) in enumerate(((-7,-7),(-7,7),(7,-7),(7,7))):
  box(x-1,f+1,z-1,x+1,f+1,z+1,'chiseled_stone_bricks','altar')
  put(x,f+2,z,'trial_spawner','spawner',{'ominous':'false','trial_spawner_state':'inactive'},{'id':'minecraft:trial_spawner','normal_config':'minecraft:trial_chamber/melee/zombie/normal','ominous_config':'minecraft:trial_chamber/melee/zombie/ominous'})
  anchors.append({'type':'trial_spawner','floor':L+1,'position':[x,f+2,z]})
  # Half-step approach on inward side.
  sx=x-(1 if x>0 else -1)*2;tread(sx,z,f+1.5,'altar_step')
 for z in (-9,9):
  put(-8,f+1,z,'vault','reward',{'facing':'east','ominous':'false','vault_state':'inactive'},{'id':'minecraft:vault','config':{'loot_table':'minecraft:chests/trial_chambers/reward','key_item':{'id':'minecraft:trial_key','count':1}}})
 for x,z in ((-11,0),(0,-11),(0,11)):
  put(x,f+3,z,'sea_lantern','light');put(x,f+4,z,'chiseled_stone_bricks','sconce')
# Spiral tread band; base flight plus four battle transitions.
band=disc(20.5)-disc(14.5);height={}
for L in range(5):
 for x,z in band:
  a=math.degrees(math.atan2(z,x))%360;T=top(L,a);height[L,x,z]=T;tread(x,z,T)
  # Structural underside, not a hollow detached plane.
  put(x,math.ceil(T)-2,z,'andesite','stair_support')
# Eight balconies: broad tangential contact around +/-Z; identical horizontal position,
# different elevations. Per-floor alternating offset yields rotational appearance.
balconies=[]
for L in range(1,5):
 for sign in (-1,1):
  T=top(L,90 if sign>0 else 270);z0,z1=(20,24) if sign>0 else (-24,-20)
  clear=[]
  for x in range(-4,5):
   for z in range(z0,z1+1):
    tread(x,z,T,'balcony');put(x,math.ceil(T)-2,z,'polished_andesite','balcony_support')
    if abs(x)<=3 and abs(z) in (21,22,23):clear.append([x,z])
    edge=abs(x)==4 or abs(z)==24
    if edge:
     for y in (math.ceil(T),math.ceil(T)+1):put(x,y,z,'stone_bricks','balcony_rail')
  # Layered corbels and small underside arch motif, below walking plane.
  for x in range(-3,4):
   y=math.ceil(T)-3-round(1.5*math.sqrt(max(0,1-(x/3)**2)))
   put(x,y,sign*22,'chiseled_stone_bricks','balcony_arch')
  for x in (-3,3):
   box(x,math.ceil(T)-4,sign*21,x,math.ceil(T)-2,sign*21,'andesite','corbel')
  put(0,math.ceil(T)-2,sign*24,'sea_lantern','light')
  balconies.append({'flight':L,'top':T,'clear':clear,'sign':sign})
# Outer guards per flight, skip balcony clear entries.
for (L,x,z),T in height.items():
 for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
  X,Z=x+dx,z+dz
  if (X,Z) in band:continue
  if L and abs(X)<=3 and 20<=abs(Z)<=23:continue
  for y in (math.floor(T),math.floor(T)+1):put(X,y,Z,'stone_bricks','rail')
# Arcade: 16 posts per flight (spacing about 8.8 along radius22.5), omit balcony bays.
posts=[]
for L in range(5):
 pp=[]
 for k in range(16):
  deg=k*22.5
  if abs((deg-90+180)%360-180)<15 or abs((deg-270+180)%360-180)<15:continue
  a=math.radians(deg);x,z=round(23*math.cos(a)),round(23*math.sin(a));T=top(L,deg)
  if T+8>=80:continue
  y=math.ceil(T)-1
  box(x-1,y-1,z-1,x+1,y,z+1,'polished_andesite','column_base')
  for Y in range(y+1,y+5):
   for dx,dz in ((0,0),(-1,0),(1,0),(0,-1),(0,1)):
    put(x+dx,Y,z+dz,'polished_andesite' if dx==dz==0 else 'smooth_stone','column')
  # Radial corbel plate ties the pier foot into the stair underside.
  for j in range(9):
   t=j/8;R=19.5+3.5*t;X,Z=round(R*math.cos(a)),round(R*math.sin(a))
   put(X,y-1,Z,'andesite','pier_corbel')
  box(x-1,y+5,z-1,x+1,y+5,z+1,'chiseled_stone_bricks','capital')
  pp.append((deg,x,z,y+5));posts.append([L,deg,x,z,y])
 for A,C in zip(pp,pp[1:]):
  previous=None
  _,x,z,y=A;_,X,Z,Y=C;dist=math.hypot(X-x,Z-z);steps=math.ceil(dist*3)
  for i in range(steps+1):
   t=i/steps;xx,zz=round(x+(X-x)*t),round(z+(Z-z)*t);yy=round(y+(Y-y)*t+dist/2*math.sqrt(max(0,1-(2*t-1)**2)))
   if previous:
    px,py,pz=previous
    while px!=xx:
     px+=1 if xx>px else -1;put(px,py,pz,'smooth_stone','arcade_arch')
    while pz!=zz:
     pz+=1 if zz>pz else -1;put(px,py,pz,'smooth_stone','arcade_arch')
    for Y in range(min(py,yy),max(py,yy)+1):put(xx,Y,zz,'smooth_stone','arcade_arch')
   put(xx,yy,zz,'smooth_stone','arcade_arch');previous=(xx,yy,zz)
   put(xx,yy+1,zz,'stone_brick_slab','arch_moulding',{'type':'bottom','waterlogged':'false'})
# Join the arcade across each revolution boundary.
for A,C in zip(posts,posts[1:]):
 if A[0]==C[0]:continue
 L,deg,x,z,y=A;M,de,X,Z,Y=C;previous=None;dist=math.hypot(X-x,Z-z)
 for i in range(33):
  t=i/32;xx,zz=round(x+(X-x)*t),round(z+(Z-z)*t);yy=round(y+5+(Y-y)*t+dist/2*math.sqrt(max(0,1-(2*t-1)**2)))
  if previous:
   px,py,pz=previous
   while px!=xx:
    px+=1 if xx>px else -1;put(px,py,pz,'smooth_stone','arcade_arch')
   while pz!=zz:
    pz+=1 if zz>pz else -1;put(px,py,pz,'smooth_stone','arcade_arch')
   for V in range(min(py,yy),max(py,yy)+1):put(xx,V,zz,'smooth_stone','arcade_arch')
  put(xx,yy,zz,'smooth_stone','arcade_arch');previous=(xx,yy,zz)
# Battle portals: four-block clear door, paired frame, geometric symbols above.
for i,f in enumerate(floors):
 for x in range(10,21):
  for z in range(-2,3):tread(x,z,f+1,'portal_floor')
 for x in range(12,16):
  for z in range(-2,3):
   for y in range(f+1,f+5):B.pop((x,y,z),None)
 for z in (-3,3):
  box(13,f+1,z,14,f+4,z,'chiseled_stone_bricks','gate')
 for z in range(-3,4):
  y=f+5+round(2*math.sqrt(max(0,1-(z/3)**2)))
  put(14,y,z,'smooth_stone','gate_arch')
 if i<4:
  for X in range(14,21):
   for Z in range(-3,4):
    put(X,f+8,Z,'stone_brick_slab','portal_canopy',{'type':'bottom','waterlogged':'false'})
  for Z in (-3,3):put(17,f+7,Z,'sea_lantern','light')
  for z in range(-i-1,i+2):put(14,f+8,z,'oxidized_cut_copper' if z==0 else 'polished_deepslate','gate_symbol')
# Roofless sanctum: floor, perimeter parapet, 12 arches and floating turquoise relic.
for x,z in disc(14.5)-disc(13.5):
 for y in (65,66):put(x,y,z,'smooth_stone','sanctum_rail')
for z in range(-2,3):
 for x in range(12,15):
  for y in range(65,68):B.pop((x,y,z),None)
ring=[]
for k in range(12):
 a=math.radians(k*30);x,z=round(12*math.cos(a)),round(12*math.sin(a));ring.append((x,z))
 box(x-1,65,z-1,x+1,65,z+1,'chiseled_stone_bricks','sanctum_base');box(x,66,z,x,73,z,'smooth_stone','sanctum_column')
 box(x-1,74,z-1,x+1,74,z+1,'polished_andesite','sanctum_capital')
for (x,z),(X,Z) in zip(ring,ring[1:]+ring[:1]):
 previous=None
 for i in range(25):
  t=i/24;yy=75+round(4*math.sqrt(max(0,1-(2*t-1)**2)));xx,zz=round(x+(X-x)*t),round(z+(Z-z)*t)
  if previous:
   px,py,pz=previous
   while px!=xx:
    px+=1 if xx>px else -1;put(px,py,pz,'smooth_stone','sanctum_arch')
   while pz!=zz:
    pz+=1 if zz>pz else -1;put(px,py,pz,'smooth_stone','sanctum_arch')
   for Y in range(min(py,yy),max(py,yy)+1):put(xx,Y,zz,'smooth_stone','sanctum_arch')
  put(xx,yy,zz,'smooth_stone','sanctum_arch');previous=(xx,yy,zz)
for k in range(24):
 a=math.radians(k*15);put(round(3*math.cos(a)),70,round(3*math.sin(a)),'oxidized_cut_copper','floating_relic')
put(0,70,0,'sea_lantern','relic_core');put(0,71,0,'oxidized_cut_copper','relic_core')
# Main entrance apron, open at exterior start.
for x in range(16,25):
 for z in range(-2,3):put(x,0,z,'polished_andesite','entrance')
# Low entrance opening through the base flight guard.
for x in range(20,25):
 for z in range(-2,3):
  for y in range(1,4):B.pop((x,y,z),None)
for x in range(21,25):
 for z in (-3,3):
  for y in (1,2):put(x,y,z,'stone_bricks','entrance_guard')
# Terminal seam protection, computed from final occupied walking columns.
from collections import defaultdict
occupied=defaultdict(list)
for (x,y,z),(mat,pr,n,g) in B.items():occupied[x,z].append((y,y+(.5 if mat.endswith('_slab') else 1),g))
guards=set()
for (x,y,z),(mat,pr,n,g) in list(B.items()):
 if g not in ('stair','portal_floor','floor','balcony','entrance'):continue
 T=y+(.5 if mat.endswith('_slab') else 1)
 if any(a<T+1.8 and b>T+1e-8 for a,b,k in occupied[x,z]):continue
 for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
  X,Z=x+dx,z+dz
  if X==25 and T==1:continue # Ground-facing entrance: supplied half-step below.
  iv=sorted((a,b) for a,b,k in occupied[X,Z]);merged=[]
  for a,b in iv:
   if merged and a<=merged[-1][1]:merged[-1]=(merged[-1][0],max(b,merged[-1][1]))
   else:merged.append((a,b))
  if any(abs(b-T)<=.5 for a,b in iv) or any(a<=T+.5 and b>=T+1.5 for a,b in merged):continue
  for Y in (math.ceil(T),math.ceil(T)+1):guards.add((X,Y,Z))
for x,y,z in guards:put(x,y,z,'stone_bricks','seam_guard')
for z in range(-2,3):tread(25,z,.5,'entrance_step')
# Serialization, stdlib only. 26.3 lowercase block-state names.
def st(s):b=s.encode();return struct.pack('>H',len(b))+b
def payload(v):
 if isinstance(v,str):return 8,st(v)
 if isinstance(v,int):return 3,struct.pack('>i',v)
 if isinstance(v,dict):return 10,b''.join(bytes([payload(a)[0]])+st(k)+payload(a)[1] for k,a in v.items())+b'\0'
 if isinstance(v,list):
  t=payload(v[0])[0] if v else 10
  return 9,bytes([t])+struct.pack('>i',len(v))+b''.join(payload(a)[1] for a in v)
 raise TypeError(v)
lo=[min(p[i] for p in B) for i in range(3)];hi=[max(p[i] for p in B) for i in range(3)];pal=[];entries=[];lookup={}
for p,(mat,props,nbt,g) in sorted(B.items()):
 key=(mat,tuple(sorted(props.items())))
 if key not in lookup:
  lookup[key]=len(pal);state={'id':'minecraft:'+mat}
  if props:state['properties']=props
  pal.append(state)
 entry={'pos':[p[i]-lo[i] for i in range(3)],'state':lookup[key]}
 if nbt:entry['nbt']=nbt
 entries.append(entry)
root={'DataVersion':5023,'size':[hi[i]-lo[i]+1 for i in range(3)],'palette':pal,'blocks':entries,'entities':[]}
blob=gzip.compress(bytes([10])+st('')+payload(root)[1],mtime=0);(H/'sky_tower_full_01.nbt').write_bytes(blob)
with (H/'blocks.csv').open('w') as f:
 w=csv.writer(f,lineterminator='\n');w.writerow(['x','y','z','block','properties','group'])
 for p,(m,pr,n,g) in sorted(B.items()):w.writerow([*p,m,json.dumps(pr,sort_keys=True),g])
(H/'model.json').write_text(json.dumps({'origin':lo,'max':hi,'size':root['size'],'axis':[.5,.5],'blocks':len(B),'sha256':hashlib.sha256(blob).hexdigest(),'floors':floors,'balconies':balconies,'spawners':anchors,'posts':posts,'walk_surfaces':walk},separators=(',',':'))+'\n')
print('NBT',root['size'],len(B),hashlib.sha256(blob).hexdigest())

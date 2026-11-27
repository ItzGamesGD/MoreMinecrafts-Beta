#!/usr/bin/env python3
"""Design-only materialized blueprint; emits JSON specifications/evidence, never production NBT."""
import argparse, copy, hashlib, importlib.util, json, sys
from collections import deque
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/st01-g0'))
from audit import nbt, cells, rotate, D1_HASH
for stage in ('st01-p6','st01-p5','st01-p4','st01-p3','st01-p2','st01-p1'):
    sys.path.insert(0,str(ROOT/'tools'/stage))
import st01_p1_model as P1
import p2c_overlay as OV
from st01_p2_model_east import make_east
import p4_spec as P4
LO=(-40,-32,-40); SHAPE=(81,72,81); ORIGIN=(-40,-24,2)
SEAM={(x,-24,z) for x in range(-8,-4) for z in range(33,36)}
COVER={(x,-23,z) for x in range(-9,-5) for z in range(33,36)}
D1=ROOT/'src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt'

def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'))
def index(p):return tuple(p[i]-LO[i] for i in range(3))
def inside(p):return all(0<=a<n for a,n in zip(index(p),SHAPE))
def box_slice(b):return tuple(slice(b[k]-LO[k//2],b[k+1]-LO[k//2]+1) for k in (0,2,4))
def bound(boxes):return [f(b[k] for b in boxes) for k,f in [(0,min),(1,max),(2,min),(3,max),(4,min),(5,max)]]
def state(name,**props):return {'id':name,'properties':props} if props else {'id':name}

def recipe(route):
    p2=OV.apply(make_east(),OV.extract());p4=P4.make_variant('B')
    if route=='WEST':
        p4['spaces']['CB1']['boxes']=[[-2,2,-30,-27,29,31],[-15,-3,-30,-27,29,31],[-15,-13,-30,-27,20,28],[-12,2,-30,-27,20,22],[0,2,-30,-27,23,26]]
    # Existing Q35/Q36 annex concept receives one explicit PROPOSED representative layout, not new authority.
    p2['spaces']['ANNEX_PORT']={'name':'Q36 branch proposal','kind':'main','boxes':[[-32,-31,-22,-20,-8,-6]]}
    p2['spaces']['ANNEX1']={'name':'Q35 one annex proposal','kind':'room','boxes':[[-38,-33,-22,-18,-11,-3]]}
    p2['links'] += [('A','ANNEX_PORT'),('ANNEX_PORT','ANNEX1')]
    components=[]
    def add(name,kind,boxes,**kwargs):
        components.append(dict(id=name,kind=kind,boxes=boxes,bounds=bound(boxes),status='PROPOSED',parent=kwargs.pop('parent','PYRAMID'),origin=[0,0,0],assembly_offset=[0,0,0],**kwargs))
    for k,b in p2['p1'].items():add('P1:'+k,'air',[b])
    for k,s in p2['spaces'].items():add('P2:'+k,'air',s['boxes'],source_kind=s['kind'])
    for name,kind,parent,x,y,z in P1.ELEMS:
        b=[*x,*y,*z]
        if all(b[k]<=b[k+1] for k in (0,2,4)):add('P1E:'+name,'solid',[b],material='minecraft:chiseled_sandstone' if kind=='column' else 'minecraft:cut_sandstone',parent='P1:'+parent)
    add('P1E:support_W','solid',[[-8,-6,0,4,-3,-1]],material='minecraft:chiseled_sandstone')
    add('P1E:support_E','solid',[[6,8,0,4,-3,-1]],material='minecraft:chiseled_sandstone')
    add('P1E:altar','solid',[[-3,3,11,12,-19,-17]],material='minecraft:cut_sandstone')
    for k,s in p4['spaces'].items():add('P4:'+k,'air',s['boxes'],source_kind=s['kind'])
    for k,e in p4['elements'].items():
        mat='minecraft:cut_sandstone'
        if e['kind']=='pile':mat='minecraft:gold_block' if k.endswith('P') else 'minecraft:diamond_block'
        if k.startswith('CHEST_'):mat='minecraft:black_terracotta' if k!='CHEST_BASE' else 'minecraft:gold_block'
        if 'column' in e['kind']:mat='minecraft:chiseled_sandstone'
        add('P4E:'+k,'solid',e['boxes'],material=mat)
    for cx in (-27,-15):
        add('P2E:guardian_statue_'+str(cx),'solid',[[cx-1,cx+1,-22,-22,-5,-3],[cx,cx,-21,-18,-4,-4],[cx-1,cx+1,-20,-20,-4,-4]],material='minecraft:chiseled_sandstone')
    for i,c in enumerate(components):c['placement_order']=i
    components.append({'id':'D1_ROYAL_HALL','kind':'immutable_nbt','bounds':[-40,-2,-24,-5,2,40],
                       'boxes':[[-40,-2,-24,-5,2,40]],'parent':'PYRAMID','origin':list(ORIGIN),
                       'assembly_offset':list(ORIGIN),'placement_order':900,'status':'APPROVED_EXCEPT_R1_01_MASK'})
    return components,p2,p4

class Model:
    def __init__(self):
        self.palette=[state('minecraft:air')];self.lookup={canonical(self.palette[0]):0}
        self.grid=np.zeros(SHAPE,np.uint16);self.ops=[];self.entities=[]
    def pid(self,s):
        key=canonical(s)
        if key not in self.lookup:self.lookup[key]=len(self.palette);self.palette.append(s)
        return self.lookup[key]
    def putbox(self,b,s,reason):
        assert all(b[k]<=b[k+1] for k in (0,2,4)) and inside((b[0],b[2],b[4])) and inside((b[1],b[3],b[5])),b
        self.grid[box_slice(b)]=self.pid(s);self.ops.append(dict(box=b,state=s,reason=reason))
    def put(self,p,s,reason):self.putbox([p[0],p[0],p[1],p[1],p[2],p[2]],s,reason)
    def get(self,p):return self.palette[int(self.grid[index(p)])]
    def solid(self):
        nonsolid={i for i,s in enumerate(self.palette) if s['id'] in ('minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:torch','minecraft:wall_torch')}
        return ~np.isin(self.grid,list(nonsolid))

def build(route):
    components,p2,p4=recipe(route);m=Model()
    m.putbox([-40,40,-32,-1,-40,40],state('minecraft:sandstone'),'proposed underground reservation mass')
    for y in range(40):
        h=40-y;m.putbox([-h,h,y,y,-h,h],state('minecraft:sandstone'),'81x81x40 step shell')
    for c in components:
        if c['kind']=='air':
            for b in c['boxes']:
                # Stepped-in ceilings: original floor plan and bottom three layers stay clear.
                for y in range(b[2],b[3]+1):
                    inset=0
                    if c['id'].startswith('P2:') and c.get('source_kind') in ('main','corr','optional','room'):
                        inset=min(max(0,y-(b[2]+2)),2,(min(b[1]-b[0]+1,b[5]-b[4]+1)-3)//2)
                    # Inset lateral axis only: longer bbox axis is longitudinal; ties use X longitudinal.
                    bb=b.copy();bb[2]=bb[3]=y
                    if inset>0:
                        axis=4 if b[1]-b[0]>=b[5]-b[4] else 0;bb[axis]+=inset;bb[axis+1]-=inset
                    m.putbox(bb,state('minecraft:air'),c['id'])
    for c in components:
        if c['kind']=='solid':
            for b in c['boxes']:m.putbox(b,state(c['material']),c['id'])
    # Source room positions are proposals. Rear is selected opposite the declared corridor contact.
    for name,s in p2['spaces'].items():
        if name not in ('T1','T2','T3','B1','B2','B3'):continue
        b=bound(s['boxes']);x0,x1,y0,y1,z0,z1=b
        adjacent=[p2['spaces'][a if d==name else d] for a,d in p2['links'] if name in (a,d)]
        nc=[bound(a['boxes']) for a in adjacent]
        x=(x0+x1)//2;z=(z0+z1)//2
        # Rear wall farthest from the adjacent corridor centre.
        options=[(abs(x0-(a[0]+a[1])/2),'xmin') for a in nc]+[(abs(x1-(a[0]+a[1])/2),'xmax') for a in nc]+[(abs(z0-(a[4]+a[5])/2),'zmin') for a in nc]+[(abs(z1-(a[4]+a[5])/2),'zmax') for a in nc]
        face=max(options)[1]
        if face=='xmin':x=x0+1
        if face=='xmax':x=x1-1
        if face=='zmin':z=z0+1
        if face=='zmax':z=z1-1
        # Small deterministic 3x2x1/1x2x3 sarcophagus/shelf preserves room centre.
        bb=[x-1,x+1,y0,y0+1,z,z] if face.startswith('z') else [x,x,y0,y0+1,z-1,z+1]
        m.putbox(bb,state('minecraft:cut_sandstone' if name.startswith('T') else 'minecraft:oak_planks'),'rear '+name)
        if name.startswith('B'):
            for zpot in (z0+1,z0+2):
                pedestal=(x0+1,y0,zpot);pot=(x0+1,y0+1,zpot)
                if m.get(pedestal)['id']=='minecraft:air' and m.get(pot)['id']=='minecraft:air':
                    m.put(pedestal,state('minecraft:cut_sandstone'),'pottery pedestal '+name)
                    m.put(pot,state('minecraft:flower_pot'),'grouped pottery '+name)

    m.putbox([-37,-34,-22,-21,-10,-10],state('minecraft:cut_sandstone'),'Q35 ceremonial annex rear shelf proposal')
    for p in [(-37,-22,-4),(-34,-22,-4)]:m.put(p,state('minecraft:flower_pot'),'Q35 annex offering pottery, no loot')
    # Materialize floor surfaces/inlays from final non-D1 cavities, not vague regional descriptions.
    occupied=m.solid()
    for c in components:
        if c['kind']!='air':continue
        for b in c['boxes']:
            y=b[2]-1
            for x in range(b[0],b[1]+1):
                for z in range(b[4],b[5]+1):
                    p=(x,y,z)
                    if inside(p) and occupied[index(p)] and not occupied[index((x,y+1,z))]:
                        central=(x==(b[0]+b[1])//2 if b[1]-b[0]<b[5]-b[4] else z==(b[4]+b[5])//2)
                        m.put(p,state('minecraft:cut_sandstone' if c['id'].startswith('P2:') and central else 'minecraft:smooth_sandstone'),'regional walking surface '+c['id'])
    # Exact, supported torch fixtures selected by component/corner availability, no global regular interval.
    for c in components:
        if c['kind']!='air' or c['id'].startswith('P1:S00'):continue
        b=c['bounds'];floor=b[2]
        found=0
        for x,z in [(b[0],b[4]),(b[1],b[5])]:
            p=(x,floor+2,z)
            if not inside(p) or m.get(p)['id']!='minecraft:air':continue
            for dx,dz,facing in [(0,-1,'south'),(-1,0,'east'),(0,1,'north'),(1,0,'west')]:
                q=(x+dx,floor+2,z+dz)
                if inside(q) and m.get(q)['id'] not in ('minecraft:air','minecraft:wall_torch'):
                    m.put(p,state('minecraft:wall_torch',facing=facing),'supported endpoint fixture '+c['id']);found+=1;break
    # Explicit accent masks, never D1. These are only applied to source solid decorative structures.
    for c in components:
        if c['id'].startswith('P4E:CHEST'):
            b=c['bounds']
            for p in cells(b):
                if m.get(p)['id']=='minecraft:black_terracotta' and (p[2] in (b[4],b[5]) or p[1]==b[3]):m.put(p,state('minecraft:gold_block'),'treasury edge trim')
    m.put((25,-26,26),state('minecraft:chest',facing='west',type='single',waterlogged='false'),'PROPOSED treasury UI anchor, not royal SECONDARY')
    m.entities.append({'pos':[25,-26,26],'nbt':{'id':'minecraft:chest'},'status':'PROPOSED_EMPTY_NO_LOOT'})
    m.put((25,-25,26),state('minecraft:air'),'treasury chest lid clearance')
    m.put((25,-24,26),state('minecraft:cyan_concrete'),'proposed turquoise emblem')
    # Gently arched three-high lid: outer width ends lower by one block; exact gold pixel wings.
    for z in (24,28):m.put((26,-24,z),state('minecraft:air'),'arched lid outer shoulders')
    for z in (24,25,27,28):m.put((25,-25,z),state('minecraft:gold_block'),'winged front gold pixels')
    for c in components:
        if c['id'].startswith('P4E:COL') and c['kind']=='solid':
            for b in c['boxes']:
                for p in cells(b):
                    if p[1]==-27:m.put(p,state('minecraft:lapis_block'),'vault shaft blue accent')
    # D1 is the last authoritative region; exact palette, states and block entities survive.
    parsed=nbt(D1.read_bytes());original={}
    m.entities=[e for e in m.entities if not (-40<=e['pos'][0]<=-2 and -24<=e['pos'][1]<=-5 and 2<=e['pos'][2]<=40)]
    for block in parsed['blocks']:
        p=tuple(a+b for a,b in zip(block['pos'],ORIGIN));s=parsed['palette'][block['state']];original[p]=s
        m.grid[index(p)]=m.pid(s)
        if 'nbt' in block:m.entities.append({'pos':p,'nbt':block['nbt'],'status':'D1_VERBATIM'})
    seam=[]
    for p in sorted(SEAM):
        assert original[p]['id'] not in ('minecraft:air','minecraft:cave_air')
        m.put(p,state('minecraft:air'),'R1-01 exactly bounded assembly exception')
        seam.append({'assembly':p,'d1_local':[p[i]-ORIGIN[i] for i in range(3)],'original':original[p],
                     'post':state('minecraft:air'),'reason':'R1-01 descent clearance below cover','constraints':'Only these cells change at assembly time; Y=-23 cover stays solid CLOSED; four-high descent; no PRIMARY/Guardian edits.',
                     'rotated':{n:rotate(p,r) for r,n in enumerate(('SOUTH','WEST','NORTH','EAST'))}})
    return m,components,original,seam,p2,p4

def reach(solid,start):
    def ok(p):
        x,y,z=p
        return inside((x,y-1,z)) and inside((x,y+2,z)) and solid[index((x,y-1,z))] and not solid[index(p)] and not solid[index((x,y+1,z))]
    if not ok(start):return set()
    seen={start};q=deque([start])
    while q:
        x,y,z=q.popleft()
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            for dy in [0,1,-1]:
                p=x+dx,y+dy,z+dz
                if p in seen:continue
                if ok(p) and (dy!=1 or not solid[index((x,y+2,z))]):seen.add(p);q.append(p);break
    return seen

def rotated_state(s,r):
    s=copy.deepcopy(s);p=s.get('properties',{})
    directions=('north','east','south','west')
    if p.get('facing') in directions:p['facing']=directions[(directions.index(p['facing'])+r)%4]
    if r%2 and p.get('axis') in ('x','z'):p['axis']='z' if p['axis']=='x' else 'x'
    if 'rotation' in p:p['rotation']=str((int(p['rotation'])+4*r)%16)
    connections={d:p[d] for d in directions if d in p}
    for d,v in connections.items():p[directions[(directions.index(d)+r)%4]]=v
    return s

def compact(ops):
    out=[]
    for op in ops:
        b=op['box']
        if out and op['state']==out[-1]['state'] and op['reason']==out[-1]['reason'] and b[:4]==out[-1]['box'][:4] and b[4]==out[-1]['box'][5]+1:
            out[-1]['box'][5]=b[5]
        else:out.append(copy.deepcopy(op))
    return out

def selection(seed,x,z):
    mask=(1<<64)-1
    h=(seed ^ (((x & 0xffffffff)<<32) | (z & 0xffffffff))) & mask
    h ^= h>>33;h=(h*0xff51afd7ed558ccd)&mask
    h ^= h>>33;h=(h*0xc4ceb9fe1a85ec53)&mask
    h ^= h>>33
    return 'EAST' if h&1==0 else 'WEST'

def flat_agent(solid,start,goal):
    def valid(x,z):
        return all(inside((xx,-31,zz)) and solid[index((xx,-31,zz))] and all(not solid[index((xx,y,zz))] for y in range(-30,-27))
                   for xx in range(x-1,x+2) for zz in range(z-1,z+2))
    if not valid(*start):return False
    seen={start};q=deque([start])
    while q:
        x,z=q.popleft()
        if (x,z)==goal:return True
        for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
            p=x+dx,z+dz
            if p not in seen and valid(*p):seen.add(p);q.append(p)
    return False

def main(out):
    assert hashlib.sha256(D1.read_bytes()).hexdigest()==D1_HASH
    out.mkdir(parents=True,exist_ok=True);results=[];specs={};manifest=None
    for route in ('EAST','WEST'):
        m,components,original,seam,p2,p4=build(route);manifest=seam
        closed=m.solid();opened=closed.copy()
        for p in COVER:opened[index(p)]=False
        start=(0,0,40);rs=reach(closed,start);ro=reach(opened,start)
        target=tuple(p4['goals']['CHEST_FRONT'][2]);crypt=tuple(p4['goals']['CRYPT_IN'])
        # Independent graph checks against the final voxel materialization, not source PASS counts.
        room_goals={}
        for name,s in p2['spaces'].items():
            if name in ('T1','T2','T3','B1','B2','B3','A','V12','V23','ANNEX1'):
                b=bound(s['boxes']);room_goals[name]=((b[0]+b[1])//2,b[2],(b[4]+b[5])//2)
        unreachable=[name for name,p in room_goals.items() if p not in rs]
        p2stairs=[]
        for name,s in p2['spaces'].items():
            if s['kind']!='stair':continue
            for b in s['boxes']:
                p=((b[0]+b[1])//2,b[2],(b[4]+b[5])//2)
                if not closed[index((p[0],p[1]-1,p[2]))] or any(closed[index((p[0],p[1]+k,p[2]))] for k in range(3)):
                    p2stairs.append([name,p])
        illegal_route=[]
        route_component=next(c for c in components if c['id']=='P4:CB1')
        for other in components:
            if not other['id'].startswith(('P1:','P2:')):continue
            for a in route_component['boxes']:
                for b in other['boxes']:
                    if all(max(a[k],b[k])<=min(a[k+1],b[k+1]) for k in (0,2,4)):
                        illegal_route.append(other['id'])
        # A complete independent replay of the recorded operations plus immutable overlay/exception.
        replay=np.zeros(SHAPE,np.uint16)
        for op in compact(m.ops):replay[box_slice(op['box'])]=m.lookup[canonical(op['state'])]
        for p,st in original.items():replay[index(p)]=m.lookup[canonical(st)]
        for p in SEAM:replay[index(p)]=0
        replay_ok=np.array_equal(replay,m.grid)
        # Exact voxel unions, not pairwise bbox assumptions; only a declared CC/CB1 join may overlap.
        overlaps=[];illegal=[]
        air=[c for c in components if c['kind']=='air']
        for j,a in enumerate(air):
            for b in air[j+1:]:
                if not all(max(a['bounds'][k],b['bounds'][k])<=min(a['bounds'][k+1],b['bounds'][k+1]) for k in (0,2,4)):continue
                sa={p for box in a['boxes'] for p in cells(box)};sb={p for box in b['boxes'] for p in cells(box)}
                common=sa & sb
                if common:
                    allowed={a['id'],b['id']}=={'P4:CB1','P4:CC'}
                    overlaps.append({'a':a['id'],'b':b['id'],'voxel_count':len(common),'allowed':allowed})
                    if not allowed:illegal.append(overlaps[-1])
        reference=json.loads((ROOT/'docs/design/st01-g0-r1/block-state-reference.json').read_text())
        palette_errors=[]
        for st in m.palette:
            entry=reference['blocks'].get(st['id'])
            if entry is None:palette_errors.append(st);continue
            for key,val in st.get('properties',{}).items():
                if val not in entry['properties'].get(key,[]):palette_errors.append(st)
        three_wide=flat_agent(opened,(0,34),(7,26))
        underground=closed[:,:32,:]
        boundary_sealed=bool(underground[0,:,:].all() and underground[-1,:,:].all() and underground[:,:,0].all() and underground[:,:,-1].all())
        modified=sorted(p for p,s in original.items() if m.get(p)!=s)
        floor_states={canonical(s):s for s in m.palette}
        sig=hashlib.sha256(canonical(m.palette).encode()+m.grid.astype('<u2').tobytes()+canonical(m.entities).encode()).hexdigest()
        specs[route]={'status':'PROPOSED_EXCEPT_EXPLICIT_APPROVALS','components':components,'connections':{'P1':[list(edge) for edge in P1.CONNECT if 'S07' not in edge]+[['S06','P2:C1']],'P2':p2['links'],'P4':p4['links'],'explicit_d1_port':'P2:T -> D1 at X-22..-20,Y-22..-20,Z2..4'},'palette':m.palette,
                      'operations':compact(m.ops),'d1_overlay':{'path':str(D1.relative_to(ROOT)),'origin':ORIGIN,'sha256':D1_HASH},
                      'block_entities':m.entities,'closed_signature':sig,'cover_cells':sorted(COVER),
                      'route_selection':'external stable per-instance O-14 selection; no new RNG stream'}
        for r,name in enumerate(('SOUTH','WEST','NORTH','EAST')):
            # Independent array rotation vs direct integer coordinate transform at every represented cell.
            rotated=np.rot90(m.grid,r,axes=(0,2))
            mapping_ok=True
            for p in [(-40,-32,-40),(40,39,40),(-21,-20,33),(-20,-20,33),(-21,-22,26),*sorted(SEAM)]:
                q=rotate(p,r)
                mapping_ok &= int(rotated[index(q)])==int(m.grid[index(p)])
            xs,ys,zs=np.indices(SHAPE)
            # Second method maps array indices directly, without calling rotate().
            xx,zz=[(xs,zs),(80-zs,xs),(80-xs,80-zs),(zs,80-xs)][r]
            mapping_ok &= np.array_equal(rotated[xx,ys,zz],m.grid)
            rotated_reach=reach(np.rot90(closed,r,axes=(0,2)),rotate(start,r))
            rotated_open_reach=reach(np.rot90(opened,r,axes=(0,2)),rotate(start,r))
            palette_r=[rotated_state(s,r) for s in m.palette]
            states_inverse=all(rotated_state(s,(4-r)%4)==old for s,old in zip(palette_r,m.palette))
            fa=p4['spaces']['FA']
            stairs_ok=all(opened[index((x,y-1,34))] and all(not opened[index((x,y+k,34))] for k in range(4)) for x,y in fa['treads'])
            lights=[];support_ok=True
            for i,pstate in enumerate(m.palette):
                if pstate['id']!='minecraft:wall_torch':continue
                dx,dz={'east':(-1,0),'west':(1,0),'south':(0,-1),'north':(0,1)}[pstate['properties']['facing']]
                for idx in np.argwhere(m.grid==i):
                    p=tuple(int(idx[j])+LO[j] for j in range(3));q=(p[0]+dx,p[1],p[2]+dz)
                    support_ok &= inside(q) and bool(closed[index(q)])
                    lights.append(p)
            be_ok=all(m.get(tuple(e['pos']))['id']==e['nbt']['id'] for e in m.entities)
            left=rotate((-21,-20,33),r);right=rotate((-20,-20,33),r)
            facing=rotated_state(m.get((-21,-20,33)),r)['properties']['facing']
            directions=('north','east','south','west');vx,vz={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}[directions[(directions.index(facing)+1)%4]]
            primary_pair=(right[0]-left[0],right[2]-left[2])==(vx,vz) and m.get((-21,-20,33))['properties']['type']=='left' and m.get((-20,-20,33))['properties']['type']=='right'
            row={'whole_rotation'  :name,'access':route,'d1_only_exact_exception':set(modified)==SEAM,
                 'rotated_navigation_executed':rotate((-21,-20,32),r) in rotated_reach and rotate(target,r) in rotated_open_reach and rotate(crypt,r) in rotated_open_reach,'three_wide_access_from_landing':three_wide,'underground_boundary_sealed':boundary_sealed,'inactive_route_filled':bool(closed[index((-10,-30,30) if route=='EAST' else (0,-30,27))]),'mandatory_room_goals':not unreachable,'unreachable_room_goals':unreachable,'cavity_overlap_contract':not illegal,'overlap_report':overlaps,'minecraft_26_3_states':not palette_errors,'palette_errors':palette_errors,'p2_stair_clearance':not p2stairs,'p2_stair_failures':p2stairs,'new_access_vs_p1_p2':not illegal_route,'illegal_access_overlaps':illegal_route,'operation_replay':bool(replay_ok),'cover_intact_closed':all(closed[index(p)] for p in COVER),
                 'entrance_to_primary':(-21,-20,32) in rs,'guardian_clearance':all(not closed[index((x,y,z))] for x in range(-22,-19) for y in range(-22,-17) for z in range(25,28)),
                 'secret_concealed':target not in rs and crypt not in rs,
                 'secret_open_treasury':target in ro,'secret_open_crypt':crypt in ro,
                 'independent_array_transform':bool(mapping_ok),'block_state_rotation_inverse':states_inverse,'descent_4high_and_treads':bool(stairs_ok),'light_supports':bool(support_ok),'light_count':len(lights),'block_entity_positions':be_ok,'palette_complete':int(m.grid.max())<len(m.palette),'primary_double_handedness':primary_pair,'anchor':rotate((-21,-20,33),r),'primary_right':rotate((-20,-20,33),r),'guardian':rotate((-21,-22,26),r),
                 'fixed_treasury':rotate((6,-30,26),r),'fixed_crypt':rotate((15,-30,-1),r),
                 'rotated_palette':palette_r,'bounds':[-40,40,-32,39,-40,40], 'closed_reachable':len(rs),'open_reachable':len(ro)}
            results.append(row)
        print(route,'closed',len(rs),'open',len(ro),'target',target, 'crypt',crypt,'modified',len(modified),flush=True)
    source_hashes={str(f.relative_to(ROOT)):hashlib.sha256(f.read_bytes()).hexdigest()
                   for stage in range(1,7) for f in sorted((ROOT/f'tools/st01-p{stage}').glob('*.py'))}
    source_hashes['docs/design/st01-g0-r1/block-state-reference.json']=hashlib.sha256((ROOT/'docs/design/st01-g0-r1/block-state-reference.json').read_bytes()).hexdigest()
    source_hashes['tools/st01-g0/audit.py']=hashlib.sha256((ROOT/'tools/st01-g0/audit.py').read_bytes()).hexdigest()
    source_hashes['tools/st01-g0-r1/blueprint.py']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    spec={'source_sha256':source_hashes,'status' :'PROPOSED_NOT_OWNER_APPROVED','baseline':'6b64106f72a99a91305057b1786ea52dd2875092',
          'coordinate_convention':'+X east,+Z south,+Y up; ground-centre origin; inclusive boxes',
          'runtime_metadata':{'authored_anchor':[-21,-20,33],'authored_primary_right':[-20,-20,33],'authored_guardian':[-21,-22,26],'secondary':None,'d1_reference_origin':list(ORIGIN),'completion_bounds':[-40,40,-32,39,-40,40],'world_mapping':'groundWorldOrigin + rotation(authoredPosition)','identity':'existing B2 dimensionIdentifier/x,y,z of transformed anchor; no new RNG stream'},'operation_semantics':'Apply operations in order, then exact D1 overlay, then the enumerated R1-01 exception. Closed is initial state. Open cover is diagnostic only.',
          'selection_contract':{'status':'EXACT_PORT_OF_MEASUREMENT_FUNCTION_NOT_A_NEW_RUNTIME','source':'O-14 7e37b786:St01DesertWorldgenProbe.mix64/selectOrientation','input':'seed and unchanged candidate centre X/Z, not mirrored hall coordinates','mapping':'low bit 0 EAST, 1 WEST','vectors':[[seed,x,z,selection(seed,x,z)] for seed,x,z in [(0,0,0),(1001,16,32),(1002,-320,400),(1003,-1,-1),(1004,2147483647,-2147483648)]]},'inputs':{'whole_rotation':['NORTH','EAST','SOUTH','WEST'],'access':['EAST','WEST']},'variants':specs}
    # Prove fixed chambers and D1 identity across access variants for each whole rotation.
    for row in results:
        other=next(x for x in results if x['whole_rotation']==row['whole_rotation'] and x['access']!=row['access'])
        row['fixed_destinations_across_routes']=all(row[k]==other[k] for k in ('anchor','fixed_treasury','fixed_crypt'))
    failures=[{'case':row['whole_rotation']+'/'+row['access'],'checks':[k for k,v in row.items() if v is False]} for row in results if any(v is False for v in row.values())]
    (out/'blueprint.json').write_text(json.dumps(spec,indent=1)+'\n')
    (out/'d1-exceptions.json').write_text(json.dumps(manifest,indent=2)+'\n')
    (out/'validation.json').write_text(json.dumps({'status':'FAIL' if failures else 'PASS','matrix':results,'failures':failures,'limits':['All new room/decor/palette fields remain proposals. This report does not approve them.','Conservative full-cube collision for stairs/slabs/chests; 1-block ascent allows jumping.','Navigation is executed on all eight rotated occupancy grids; integer and array transforms cross-check independently.','No production build or Minecraft execution.']},indent=2)+'\n')
    lines=['# G0-R1 coordinate inventory','', 'PROPOSED unless explicitly approved in AUTHORITY-DELTA.md. Exact box unions and operations are in blueprint.json.','']
    for name,variant in specs.items():
        lines += ['## '+name+' access','','| ID | Kind | Bounds X/Y/Z | Parent | Order |','|---|---|---|---|---|']
        for c in variant['components']:lines.append(f"| {c['id']} | {c['kind']} | {c['bounds']} | {c['parent']} | {c['placement_order']} |")
    (out/'COMPONENTS.md').write_text('\n'.join(lines)+'\n')
    print('FAILURES',failures)
    return 1 if failures else 0

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);sys.exit(main(p.parse_args().out))

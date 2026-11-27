"""GEOMETRY-03B finite coordinate search. No design authority or game implementation."""
from collections import defaultdict, deque
import csv
import importlib.util
import math
from pathlib import Path
import json

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]


def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result


old=load(HERE.parent/'sky_tower_geometry_03/geometry03_model.py','original03')


def box(r):return {(x,z) for x in range(-r,r+1) for z in range(-r,r+1)}
def small_pad(p):return {(p[0]+i,p[1]+j) for i in (0,1) for j in (0,1)}
def square(p):return {(p[0]+i,p[1]+j) for i in (-1,0,1) for j in (-1,0,1)}


def schedule(loop):
    N=len(loop)
    ds=[(loop[(i+1)%N][0]-p[0],loop[(i+1)%N][1]-p[1]) for i,p in enumerate(loop)]
    protected={(c+j)%N for c,r in ((0,4),(N//4,9),(3*N//4,9)) for j in range(-r,r+1)}
    eligible=[i for i in range(1,N) if i not in protected and len({ds[(i+k)%N] for k in range(-5,6)})==1]
    if len(eligible)<17:raise ValueError('insufficient safe straight stair stations')
    out=[];height=10
    for lap,rise in enumerate((11,12,14,17)):
        count=0;prev=height
        for i,p in enumerate(loop):
            count+=i in eligible
            y=height+rise*count//len(eligible)
            out.append({'lap':lap,'station':i,'xz':p,'floor':y,'direction':ds[i],
                        'rise':y-prev,'turn':ds[i]!=ds[(i-1)%N],'protected':i in protected})
            prev=y
        assert prev==height+rise
        height=prev
    out.append({'lap':4,'station':0,'xz':loop[0],'floor':64,'direction':ds[0],
                'rise':0,'turn':False,'protected':True})
    return out


def arch_path(a,b,walking,domain):
    valid={p for p in domain if small_pad(p)<=domain and not small_pad(p)&walking}
    starts=sorted(p for p in valid if small_pad(p)<=square(a))
    targets={p for p in valid if small_pad(p)<=square(b)}
    parent={p:None for p in starts};q=deque(starts)
    end=None
    while q:
        p=q.popleft()
        if p in targets:end=p;break
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            n=p[0]+dx,p[1]+dz
            if n in valid and n not in parent:
                parent[n]=p;q.append(n)
    path=[]
    while end is not None:path.append(end);end=parent[end]
    return list(reversed(path))


def stairs(a,b):
    base,_=old.source_inputs();loop=old.grid_loop(a,b);records=schedule(loop)
    heights=defaultdict(dict)
    for i,r in enumerate(records):
        for p in old.pad(r['xz']):heights[p][r['floor']]=i
    states={};floors=defaultdict(list)
    for (x,z),levels in sorted(heights.items()):
        clusters=[]
        for y in sorted(levels):
            if clusters and y-clusters[-1][-1]<=1:clusters[-1].append(y)
            else:clusters.append([y])
        for cluster in clusters:
            y=max(cluster);r=records[levels[y]];dx,dz=r['direction']
            back=heights.get((x-dx,z-dz),{})
            stair=y-1 in back and y not in back
            states[x,y,z]=('stair',old.FACING[dx,dz]) if stair else ('full','')
            floors[x,z].append(y)
    for fy in (10,21,33,47,64):
        states.update({(x,fy,z):('full','') for x in range(11,20) for z in range(-1,3)})
    frames=[]
    for i,r in enumerate(records):
        x,z=r['xz'];dx,dz=r['direction']
        cells=[]
        for j in (-1,0,1,2):
            X,Z=(x+j,z) if dz else (x,z+j)
            y=min(floors[X,Z],key=lambda y:abs(y-r['floor']))
            cells.append((X,y,Z))
        pad=[]
        if r['turn']:
            for X,Z in sorted(old.pad(r['xz'])):
                y=min(floors[X,Z],key=lambda y:abs(y-r['floor']));pad.append((X,y,Z))
        frames.append({'index':i,'lap':r['lap'],'station':r['station'],'direction':r['direction'],
                       'cells':cells,'turn_pad':pad})
    return base,loop,records,states,frames


def build(a=17,b=10,phase=3,domain_name='axis45',shift_direction=1,arch_lift=3,canopy_lift=0):
    base,loop,records,stepstates,frames=stairs(a,b)
    walk={(x,z) for x,y,z in stepstates}
    full=old.disc(31)
    domain=(old.disc(45) if domain_name=='disc45' else box(22))-full
    groups={'stairs':dict(stepstates)};objects=[];balconies=[];posts=[];arches=[];reserves={}

    def put(name,cells):
        groups.setdefault(name,{}).update({tuple(p):('full','') for p in cells})

    N=len(loop)
    for lap,shift in enumerate((-3,-1,1,3)):
        shift*=shift_direction
        for quarter in (1,3):
            station=quarter*(N//4)+shift
            h=records[lap*N+station]['floor']
            turn=quarter
            clear={old.rotate_cell((x,z),turn) for x in range(20,23) for z in range(shift-3,shift+4)}
            connector={old.rotate_cell((x,z),turn) for x in range(16,20) for z in range(shift-3,shift+4)}
            footprint=clear|{old.rotate_cell((x,z),turn) for x in range(19,24) for z in range(shift-4,shift+5)
                if ((x-19)/5)**2+((z-shift)/5)**2<=1}
            edge={p for p in footprint if any((p[0]+dx,p[1]+dz) not in footprint|connector for dx,dz in old.FACING)}
            floor={(x,h,z) for x,z in footprint|connector}
            rail_edge=edge-walk
            rail={(x,h+1,z) for x,z in rail_edge}|{(x,y,z) for x,z in sorted(rail_edge)[::4] for y in (h+1,h+2)}
            support={(x,h-1,z) for x,z in footprint}|{(x,h-2,z) for x,z in footprint-edge}
            small=set()
            for s in range(-2,3):
                x,z=old.rotate_cell((21,shift+s),turn);small.add((x,h-4+round(math.sqrt(max(0,4-s*s))),z))
            support|=small
            put('balcony_floor',floor);put('balcony_rail',rail);put('balcony_support',support)
            obj={'id':f'B{lap+1}-{quarter}','lap':lap,'quarter':quarter,'shift':shift,'station':station,
                 'floor':h,'clear':sorted((x,h,z) for x,z in clear),'connector':sorted((x,h,z) for x,z in connector),
                 'floor_cells':sorted(floor),'rail_cells':sorted(rail),'support_cells':sorted(support),
                 'small_arch_cells':sorted(small)}
            balconies.append(obj)
    clear_by_lap={lap:{(x,z) for b in balconies if b['lap']==lap for x,y,z in b['clear']} for lap in range(4)}
    centers=[(p,square(p)) for p in sorted(domain) if square(p)<=domain]
    for lap in range(4):
        used=set()
        for nominal in range(phase,N,7):
            px,pz=loop[nominal];r=math.hypot(px+.5,pz+.5)
            target=((px+.5)*(r+3.5)/r,(pz+.5)*(r+3.5)/r)
            options=[]
            for (x,z),sq in centers:
                if (x-px)**2+(z-pz)**2>81:continue
                clash=len(sq&walk)+len(sq&clear_by_lap[lap])+10*len(sq&used)
                options.append((10000*clash+(x-target[0])**2+(z-target[1])**2,x,z,sq,clash))
            if not options:raise ValueError('no local pillar options')
            _,x,z,sq,clash=min(options,key=lambda x:x[:3]);used|=sq
            projected=min(range(N),key=lambda i:((loop[i][0]-x)**2+(loop[i][1]-z)**2,i))
            fy=records[lap*N+projected]['floor']
            posts.append({'id':f'P{lap+1}-{nominal}','lap':lap,'nominal_station':nominal,
                'station':projected,'xz':[x,z],'floor':fy,'plan_clash_columns':clash})
    # Terminal arcade pier; actual staircase still winds exactly four turns.
    posts.append({'id':'P-END','lap':4,'nominal_station':0,'station':0,
                  'xz':posts[0]['xz'],'floor':64,'plan_clash_columns':0})
    for A,B in zip(posts,posts[1:]):
        path=arch_path(tuple(A['xz']),tuple(B['xz']),walk,domain)
        mode='outside_walk_two_by_two'
        if not path:
            mode='fallback_chord_candidate_failure'
            x,z=A['xz'];X,Z=B['xz'];count=max(abs(X-x),abs(Z-z),1)
            path=[(round(x+(X-x)*j/count),round(z+(Z-z)*j/count)) for j in range(count+1)]
        span=max(1,len(path)-1);spring=max(A['floor'],B['floor'])+arch_lift
        cells=set();prev=spring
        for i,p in enumerate(path):
            y=spring+math.ceil(math.sqrt(max(0,(span/2)**2-(i-span/2)**2)))
            for Y in range(min(y,prev),max(y,prev)+1):
                cells.update((x,Y,z) for x,z in small_pad(p))
            prev=y
        obj={'id':f'A{len(arches)}','from':A['id'],'to':B['id'],'spring':spring,
             'apex':max(y for x,y,z in cells),'path_xz':path,'raster_mode':mode,'cells':sorted(cells)}
        arches.append(obj);put('arcade_arch',cells)
    for p in posts:
        adjacent=[a for a in arches if p['id'] in (a['from'],a['to'])]
        cap=max(a['spring'] for a in adjacent)
        x,z=p['xz'];fy=p['floor']
        body={(X,y,Z) for X,Z in square((x,z)) for y in range(fy,cap)}
        foot={(X,fy-1,Z) for X,Z in square((x,z))}|{(x,fy-2,z)}
        capital={(x,cap,z)}|{(X,cap+1,Z) for X,Z in square((x,z))}
        put('pillar',body|foot|capital)
        p.update(capital_start=cap,cells=sorted(body|foot|capital),dark_stripe=sorted((X,y,Z)
            for X,Z in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)) for y in range(fy,cap)))
    for bobj in balconies:
        lap,h,turn=bobj['lap'],bobj['floor'],bobj['quarter']
        bcenter=tuple(bobj['clear'][10][i] for i in (0,2))
        p=min((p for p in posts if p['lap']==lap),key=lambda p:(p['xz'][0]-bcenter[0])**2+(p['xz'][1]-bcenter[1])**2)
        # Transform the support to the east cardinal frame to build a wall link.
        X,Z=old.rotate_cell(tuple(p['xz']),-turn)
        spring=p['capital_start'];span=max(1,X-15);path=[];prev=spring
        for x in range(15,X+1):
            y=spring+math.ceil(math.sqrt(max(0,(span/2)**2-(x-15-span/2)**2)))
            for Y in range(min(prev,y),max(prev,y)+1):
                for z in (Z,Z+1):
                    xx,zz=old.rotate_cell((x,z),turn);path.append((xx,Y,zz))
            prev=y
        put('wall_link_arch',path)
        apex=max(y for x,y,z in path)
        link={'id':'L'+bobj['id'],'support':p['id'],'spring':spring,'apex':apex,'cells':sorted(set(path))}
        objects.append(link)
        canopy=set()
        zlo,zhi=min(bobj['shift']-4,Z),max(bobj['shift']+4,Z+1)
        for tier,edge in enumerate((min(22,X+1),min(21,X),min(20,X-1))):
            for x in range(16,edge+1):
                for z in range(zlo,zhi+1):
                    xx,zz=old.rotate_cell((x,z),turn);canopy.add((xx,apex+1+tier+canopy_lift,zz))
        # A thin horizontal joining molding, no added outward projection.
        molding={(x,apex+1,z) for x,y,z in path if y==apex}
        canopy|=molding;put('canopy',canopy)
        objects.append({'id':'C'+bobj['id'],'link':link['id'],'cells':sorted(canopy),
                        'support_outer_column':X,'outer_column':min(22,X+1),'tiers':3})
    # Gateway shapes remain paired double arches, placed clear of the outer walking lane.
    for lap,fy in enumerate((10,21,33,47)):
        near=min((p for p in posts if p['lap']==lap),key=lambda p:abs(p['xz'][1])+abs(p['xz'][0]-21))
        assoc=min((a for a in arches if near['id'] in (a['from'],a['to'])),key=lambda a:abs(a['apex']-(fy+7)))
        apex=assoc['apex'];cells=set()
        for x in (11,12,14,15):
            for z in range(-3,5):
                top=apex-4+round(math.sqrt(max(0,16-(z-.5)**2)))
                cells.add((x,top,z))
            cells.update((x,y,z) for z in (-3,4) for y in range(fy+1,apex-1))
        put('gateway_frame',cells)
        reserves[f'F{lap+1}']={'arch_cells':sorted(cells),'apex':apex,'associated_arcade':assoc['id'],
            'motif':sorted((x,y,z) for x in (14,15) for y in (apex,apex+1) for z in range(-5,7)),
            'pilaster':sorted((15,y,z) for y in range(fy+1,apex-1) for z in (-4,5)),
            'detailed_pattern':None,'progression':lap+1}
    # Real outer and inner falling edges, with openings only at same-height connections.
    floor_cells=set(stepstates)|set(groups['balcony_floor'])
    rail=set()
    for x,y,z in stepstates:
        for dx,dz in old.FACING:
            q=x+dx,z+dz
            if q not in walk and (q[0],y,q[1]) not in floor_cells and q not in full:
                rail.add((q[0],y+1,q[1]))
    put('rail',rail)
    baseline={}
    for layer in base['models']['C1']['layers']:
        cols=old.disc(31)-old.disc(25) if layer['zone']=='WALL' else old.disc(31)
        baseline.update({(x,layer['y'],z):('full','') for x,z in cols})
    void={(x,y,z) for fy in (10,21,33,47) for x in range(12,16)
          for y in range(fy+1,min(fy+8,80)) for z in range(-1,3)}&set(baseline)
    params={'a':a,'b':b,'phase':phase,'domain':domain_name,'shift_direction':shift_direction,
            'arch_lift':arch_lift,'canopy_lift':canopy_lift}
    return {'params':params,'groups':groups,'baseline':baseline,'void':void,'frames':frames,
            'records':records,'posts':posts,'arches':arches,'balconies':balconies,'objects':objects,'gateways':reserves,
            'domain_outside_disc45':sorted({(x,z) for name,g in groups.items() if not name.startswith('balcony')
                for x,y,z in g if x*x+z*z>=22.5**2 and abs(x)<=22 and abs(z)<=22})}


def export(model,out,conv='C1',rotation=0):
    out.mkdir(parents=True,exist_ok=True)
    dy=0 if conv=='C1' else -1
    def xyz(p):
        x,y,z=p;X,Z=old.rotate_cell((x,z),rotation);return X,y+dy,Z
    def state(v):
        kind,facing=v
        if not facing:return v
        d=next(k for k,v in old.FACING.items() if v==facing)
        d=old.rotate_cell(d,rotation);return kind,old.FACING[d]
    # Separate category rows preserve intentional contacts and incompatible shapes for audit.
    with (out/f'blocks_{conv}.csv').open('w',newline='') as f:
        w=csv.writer(f,lineterminator='\n');w.writerow(['group','x','y','z','shape','facing'])
        for group,blocks in sorted(model['groups'].items()):
            for p,v in sorted(blocks.items()):w.writerow([group,*xyz(p),*state(v)])
    def save(name,data):
        (out/f'{name}_{conv}.json').write_text(json.dumps(data,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
    frames=[{**r,'cells':[xyz(p) for p in r['cells']],'turn_pad':[xyz(p) for p in r['turn_pad']],
             'direction':old.rotate_cell(tuple(r['direction']),rotation)} for r in model['frames']]
    save('claims',{'frames':frames,'portal_void':[xyz(p) for p in sorted(model['void'])],
        'base_head':'dd20521d4d2613429af40f1444b525ae31465b10','convention':conv,'rotation':rotation,
        'floors':[y+dy for y in (10,21,33,47,64)],'parameters':model['params'],
        'outside_original_disc45_columns':sorted(old.rotate_cell(tuple(p),rotation) for p in model['domain_outside_disc45'])})
    # Object definitions exported in canonical C1 coordinates; explicit transform accompanies C2.
    save('objects',{'canonical_convention':'C1','transform':{'rotation_quarters':rotation,'dy':dy},
        'pillars':model['posts'],'arches':model['arches'],'balconies':model['balconies'],
        'links_canopies':model['objects'],'gateway_reservations':model['gateways'],
        'movement_stations':model['records']})

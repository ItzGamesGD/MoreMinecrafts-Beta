"""Independent exported-coordinate auditor. Does NOT import search.py.

Static width certificates use actual four-column treads, both half-stair surfaces,
and 4x4 corner pads. Collision-space BFS is additional, never a width substitute.
"""
from collections import defaultdict
import csv
import importlib.util
import json
import math
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('frozen_nav',HERE.parent/'sky_tower_geometry_03/geometry03_navigation.py')
nav=importlib.util.module_from_spec(spec);spec.loader.exec_module(nav)
STAGES=[('stairs',),('stairs','pillar'),('stairs','pillar','rail'),
        ('stairs','pillar','rail','balcony_floor','balcony_rail','balcony_support'),
        ('stairs','pillar','rail','balcony_floor','balcony_rail','balcony_support','arcade_arch','wall_link_arch'),
        ('stairs','pillar','rail','balcony_floor','balcony_rail','balcony_support','arcade_arch','wall_link_arch','canopy','gateway_frame')]


def stand_at(scene,x,feet,z):
    x,z,h=round(40*x),round(40*z),round(40*feet)
    bounds=(x-12,h,z-12,x+12,h+72,z+12)
    support=any(Y==h and a<x+12 and A>x-12 and c<z+12 and C>z-12
                for a,b,c,A,Y,C in scene.boxes(x-12,z-12,x+12,z+12))
    return support and scene.clear_box(bounds)


def check_lane(cells,blocks,scene=None):
    scene=scene or nav.Scene(blocks)
    blocked=[];samples=[];column_samples=[]
    for X,y,Z in cells:
        kind,facing=blocks.get((X,y,Z),('missing',''))
        tests=[(X+.5,y+1,Z+.5)] if kind=='full' else []
        if kind=='stair':
            dx,dz={'east':(1,0),'west':(-1,0),'south':(0,1),'north':(0,-1)}[facing]
            for f in (-.375,-.125,.125,.375):
                x,z=X+.5+f*dx,Z+.5+f*dz
                candidates=[h for h in (y+1.5,y+1,y+.5) if stand_at(scene,x,h,z)]
                tests.append((x,max(candidates) if candidates else y+.5,z))
        if (not tests or not all(stand_at(scene,*p) for p in tests)
                or any(not swept_move(scene,a,b) for a,b in zip(tests,tests[1:]))):blocked.append([X,y,Z])
        samples.extend(tests)
        column_samples.append({'cell':[X,y,Z],'samples':tests})
    same_height=len({p[1] for p in cells})==1
    return {'pass':not blocked and same_height,'blocked_columns':blocked,
            'same_floor_height':same_height,'samples':samples,'column_samples':column_samples}


def swept_move(scene,a,b):
    x,h,z=(round(c*40) for c in a);X,H,Z=(round(c*40) for c in b)
    if abs(h-H)>20:return False
    high=max(h,H)
    if not scene.clear_box((min(x,X)-12,high,min(z,Z)-12,max(x,X)+12,high+72,max(z,Z)+12)):return False
    return H<=h or scene.clear_box((x-12,h,z-12,x+12,H+72,z+12))


def merge(base,groups,names):
    result=dict(base)
    for name in names:
        for p,state in groups.get(name,{}).items():
            if p not in result or state[0]=='full':result[p]=state
    return result


def transform(p,rot,dy):
    x,y,z=p
    for _ in range(rot%4):x,z=-z,x
    return x,y+dy,z


def read(out,conv):
    groups=defaultdict(dict)
    with (out/f'blocks_{conv}.csv').open() as f:
        for row in csv.DictReader(f):
            groups[row['group']][int(row['x']),int(row['y']),int(row['z'])]=(row['shape'],row['facing'])
    claims=json.loads((out/f'claims_{conv}.json').read_text())
    objects=json.loads((out/f'objects_{conv}.json').read_text())
    base=json.loads((HERE.parent/'sky_tower_geometry_02/geometry02b/baseline.json').read_text())
    inner,wall=(set(map(tuple,base['columns'][k])) for k in ('inner','wall'))
    baseline={}
    for layer in base['models'][conv]['layers']:
        cols=wall if layer['zone']=='WALL' else inner|wall
        baseline.update({(x,layer['y'],z):('full','') for x,z in cols})
    void=set(map(tuple,claims['portal_void']))
    for p in void:baseline.pop(p,None)
    return groups,claims,objects,baseline


def width_certificate(frames,blocks,scene):
    bad=[];turn_bad=[];transition_bad=[];minimum=None;minimum_at=None;paths={}
    for r in frames:
        cells=r['cells'];dx,dz=r['direction']
        positions=sorted((x,z) for x,y,z in cells)
        axis_sorted=sorted(x if dz else z for x,y,z in cells)
        geometry=(len(cells)==4 and len(set(positions))==4 and abs(dx)+abs(dz)==1 and
                  axis_sorted==list(range(min(axis_sorted),min(axis_sorted)+4)))
        check=check_lane(cells,blocks,scene)
        paths[r['index']]={tuple(c['cell']):c['samples'] for c in check['column_samples']}
        for X,H,Z in check['samples']:
            x,h,z=round(X*40),round(H*40),round(Z*40)
            heights=[b/40-H for a,b,c,A,Y,C in scene.boxes(x-12,z-12,x+12,z+12)
                     if Y>h and b>=h and a<x+12 and A>x-12 and c<z+12 and C>z-12]
            if heights and (minimum is None or min(heights)<minimum):
                minimum=min(heights);minimum_at=[X,H,Z]
        if not geometry or not check['pass']:
            bad.append({'index':r['index'],'lap':r['lap'],'station':r['station'],'cells':cells,
                        **{k:v for k,v in check.items() if k not in ('samples','column_samples')},'coordinate_geometry_ok':geometry})
        pad=r['turn_pad']
        if pad:
            safe=all(check_lane([p],blocks,scene)['pass'] for p in pad)
            if len({tuple(p) for p in pad})!=16 or len({p[1] for p in pad})!=1 or not safe:
                turn_bad.append({'index':r['index'],'cells':pad,'all_standable':safe})
    for a,b in zip(frames,frames[1:]):
        if a['direction']!=b['direction']:
            if not (a['turn_pad'] or b['turn_pad']):transition_bad.append({'index':a['index'],'reason':'no corner pad'})
            continue
        dx,dz=a['direction'];nexts={(x,z):(x,y,z) for x,y,z in b['cells']}
        for x,y,z in a['cells']:
            p=nexts.get((x+dx,z+dz))
            if p is None:
                transition_bad.append({'index':a['index'],'from':[x,y,z],'reason':'disconnected cross-section'});continue
            dy=p[1]-y
            expected={(1,0):'east',(-1,0):'west',(0,1):'south',(0,-1):'north'}[dx,dz]
            if not 0<=dy<=1 or (dy==1 and blocks.get(p)!=('stair',expected)):
                transition_bad.append({'index':a['index'],'from':[x,y,z],'to':p,'reason':'unsupported rise/shape'})
            else:
                A=paths[a['index']].get((x,y,z),[]);B=paths[b['index']].get(tuple(p),[])
                if not A or not B or not swept_move(scene,A[-1],B[0]):
                    transition_bad.append({'index':a['index'],'from':[x,y,z],'to':p,'reason':'continuous swept AABB obstruction'})
    return {'status':'PASS' if not (bad or turn_bad or transition_bad) else 'CANDIDATE FAILURE',
            'frames_checked':len(frames),'failed_frames':bad,'failed_turn_pads':turn_bad,
            'failed_transitions':transition_bad,'first_failure':(bad+turn_bad+transition_bad or [None])[0],
            'minimum_overhead_clearance':minimum,'minimum_overhead_at':minimum_at,
            'method':'four contiguous full-width tread columns, two half-stair samples, actual collision AABB, flat 4x4 turns, connected rise-compatible cross-section sequence'}


def audit(out,conv,run_bfs=True):
    groups,claims,objects,base=read(out,conv)
    rot,dy=objects['transform']['rotation_quarters'],objects['transform']['dy']
    trans=lambda p:transform(p,rot,dy)
    goals=[]
    for fy in claims['floors'][1:]:
        x,_,z=trans((11,0,0));goals.append((4*x+1,fy+1,4*z+1))
    x,_,z=trans((11,0,0));start=(4*x+1,claims['floors'][0]+1,4*z+1)
    bgoals=[]
    for obj in objects['balconies']:
        clear=[trans(p) for p in obj['clear']]
        X=round(sum(p[0] for p in clear)/len(clear));Z=round(sum(p[2] for p in clear)/len(clear))
        bgoals.append((4*X+1,obj['floor']+dy+1,4*Z+1))
    results=[]
    for stage,names in enumerate(STAGES,1):
        blocks=merge(base,groups,names);scene=nav.Scene(blocks)
        width=width_certificate(claims['frames'],blocks,scene)
        balcs=[]
        if stage>=4:
            for obj in objects['balconies']:
                clear=[trans(p) for p in obj['clear']]
                checks=[check_lane([p],blocks,scene) for p in clear]
                balcs.append({'id':obj['id'],'columns':len(clear),'pass':len(clear)==21 and all(c['pass'] for c in checks),
                              'bad_columns':[p for p,c in zip(clear,checks) if not c['pass']]})
        nav_result=None
        if run_bfs:
            print(conv,'stage',stage,'BFS',flush=True)
            nav_result=scene.search(start,goals+(bgoals if stage>=4 else []))
        else:scene.surfaces.cache_clear();scene.stand.cache_clear()
        geometry=set().union(*(set(groups.get(k,{})) for k in names))
        normal=set().union(*(set(groups.get(k,{})) for k in names if not k.startswith('balcony')))
        balcony=geometry-normal
        over45=sorted(p for p in normal if abs(p[0])>22 or abs(p[2])>22)
        over51=sorted(p for p in balcony if abs(p[0])>25 or abs(p[2])>25)
        over80=sorted(p for p in geometry if not 0<=p[1]<=79)
        all_numeric=width['status']=='PASS' and not any((over45,over51,over80)) and all(b['pass'] for b in balcs)
        status='PASS' if all_numeric and nav_result and all(nav_result['reached']) else 'CANDIDATE FAILURE'
        if not run_bfs and all_numeric:status='UNVERIFIED'
        results.append({'stage':stage,'members':names,'status':status,'width':width,'balconies':balcs,
                        'overflow45':over45,'overflow51':over51,'overflow80':over80,'navigation':nav_result,
                        'first_disconnected_target':next((('F'+str(i+2) if i<3 else 'SANCTUM' if i==3 else 'BALCONY'+str(i-3))
                            for i,v in enumerate(nav_result['reached']) if not v),None) if nav_result else None})
    spacing=[]
    stations_per_turn=(len(claims['frames'])-1)//4
    for a,b in zip(objects['pillars'],objects['pillars'][1:]):
        if a['lap']!=b['lap']:continue
        x,z=a['xz'];X,Z=b['xz']
        spacing.append({'from':a['id'],'to':b['id'],'actual_xz_distance':math.hypot(X-x,Z-z),
            'projected_station_difference':(b['station']-a['station'])%stations_per_turn,
            'raw_station_index_difference':b['station']-a['station'],
            'actual_3d_distance':math.sqrt((X-x)**2+(Z-z)**2+(b['floor']-a['floor'])**2)})
    # Check hard adjacency using exact coordinate sets, not names alone.
    def touches(A,B):
        return bool(A&B) or any((x+dx,y+dy,z+dz) in B for x,y,z in A for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))
    piers={p['id']:set(map(trans,p['cells'])) for p in objects['pillars']}
    joints=[]
    for arch in objects['arches']:
        cells=set(map(trans,arch['cells']))
        joints.append({'id':arch['id'],'touch_from':touches(cells,piers[arch['from']]),
                       'touch_to':touches(cells,piers[arch['to']]),'raster_mode':arch['raster_mode']})
    links={o['id']:o for o in objects['links_canopies'] if o['id'].startswith('L')}
    for obj in objects['links_canopies']:
        if obj['id'].startswith('C'):
            cells=set(map(trans,obj['cells']));link=links[obj['link']]
            joints.append({'id':obj['id'],'touch_link':touches(cells,set(map(trans,link['cells']))),
                           'outward_extension':obj['outer_column']-obj['support_outer_column']})
    final=results[-1]
    return {'convention':conv,'stages':results,'pillar_spacing':spacing,'joints':joints,
            'numeric_final_status':final['status'],'overall':'UNVERIFIED',
            'unresolved':['Q85 about-7 tolerance','Q93/Q94 exact step profiles','Q113-Q118 detailed gateway motifs',
                          'original circular reservation mask versus X/Z measurement; outside-mask occupancy explicitly reported'],
            'design_conflict_proven':False,'in_game_validation':'NOT RUN'}


if __name__=='__main__':
    import argparse
    ap=argparse.ArgumentParser();ap.add_argument('directory',type=Path);ap.add_argument('--convention',default='C1')
    args=ap.parse_args();r=audit(args.directory,args.convention)
    (args.directory/f'audit_{args.convention}.json').write_text(json.dumps(r,ensure_ascii=False,separators=(',',':'))+'\n')

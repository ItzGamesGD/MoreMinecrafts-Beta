#!/usr/bin/env python3
"""P2 preflight and bounded stair-family screen; not a complete search."""
import csv, hashlib, importlib.util, json, math
from collections import Counter, defaultdict, deque
from pathlib import Path

HERE=Path(__file__).resolve().parent
P=HERE.parent/'sky_tower_geometry_03b/data/best'
EXPECTED={'C1':'b6748c4023d794dae01d729ea95af870e7b63316','C2':'286e77cde6a0a41fc2e3c16e147c093d9bbd07dc'}
s=importlib.util.spec_from_file_location('p1',HERE/'audit.py');p1=importlib.util.module_from_spec(s);s.loader.exec_module(p1)
s=importlib.util.spec_from_file_location('g03b',HERE.parent/'sky_tower_geometry_03b/search.py');gen=importlib.util.module_from_spec(s);s.loader.exec_module(gen)

def read(conv):
    raw=(P/f'blocks_{conv}.csv').read_bytes();sha=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()
    if sha!=EXPECTED[conv]:raise ValueError('base blob mismatch')
    groups=defaultdict(set)
    with (P/f'blocks_{conv}.csv').open() as f:
        for r in csv.DictReader(f):groups[r['group']].add(tuple(int(r[k]) for k in ('x','y','z')))
    return groups

def locality(groups,objects,conv):
    dy=0 if conv=='C1' else -1;rot=objects['transform']['rotation_quarters']
    def T(p):
        x,y,z=p
        for _ in range(rot):x,z=-z,x
        return x,y+dy,z
    declared=set();per=[]
    for b in objects['balconies']:
        cells={T(p) for key in ('floor_cells','rail_cells','support_cells') for p in b[key]}
        declared|=cells
        # Connected components are measured per balcony, without assuming adjacent general members are exempt.
        remain=set(cells);components=0
        while remain:
            components+=1;q=deque([remain.pop()])
            while q:
                x,y,z=q.popleft()
                for p in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
                    if p in remain:remain.remove(p);q.append(p)
        per.append({'id':b['id'],'components_6':components,'count':len(cells)})
    actual=set().union(*(v for k,v in groups.items() if k.startswith('balcony_')))
    return {'objects':per,'declared_matches_export':declared==actual,
            'missing':sorted(actual-declared),'extra':sorted(declared-actual),
            'status':'UNVERIFIED','reason':'D51 bounds and membership do not establish airborne/local geometric exception or support intent.'}

def main():
    data={};both={}
    for conv in ('C1','C2'):
        groups=read(conv);both[conv]=groups
        normal={p for k,vs in groups.items() if not k.startswith('balcony_') for p in vs}
        bal={p for k,vs in groups.items() if k.startswith('balcony_') for p in vs}
        bad=sorted({(x,z) for x,y,z in normal if not p1.center_inside(x,z,45)})
        bb=sorted({(x,z) for x,y,z in bal if not p1.center_inside(x,z,51)})
        allgroups=sorted(groups);objects=json.loads((P/f'objects_{conv}.json').read_text())
        data[conv]={'normal_outside_D45_columns':bad,'balcony_outside_D51_columns':bb,
                    'groups':allgroups,'balcony_locality':locality(groups,objects,conv),
                    'normal_whole_block_D45_outside_columns':sorted({(x,z) for x,y,z in normal if not p1.corner_inside(x,z,45)})}
        assert len(bad)==259 and not bb
    assert {k:{(x,y-1,z) for x,y,z in v} for k,v in both['C1'].items()}==both['C2']
    # Compare the generator's original D45 membership to P1 for every coordinate in a bounded grid.
    disc=gen.old.disc(45);assert all(((x,z) in disc)==p1.center_inside(x,z,45) for x in range(-30,31) for z in range(-30,31))
    options=[]
    for a in (16,17,18):
        for b in range(6,15):
            if b>=a:continue
            loop=gen.old.grid_loop(a,b);walk=set().union(*(gen.old.pad(p) for p in loop))
            boundary=sorted(walk-disc);wall=sorted(walk & gen.old.disc(31))
            try:
                records=gen.schedule(loop);schedule='PASS';rise=[records[(i+1)*len(loop)]['floor']-records[i*len(loop)]['floor'] for i in range(4)]
            except ValueError:
                schedule='CANDIDATE FAILURE';rise=None
            options.append({'a':a,'b':b,'walk_columns_outside_D45':len(boundary),'wall_overlap':len(wall),
                            'schedule':schedule,'rises':rise,
                            'status':'CANDIDATE FAILURE' if boundary or wall or schedule!='PASS' else 'UNVERIFIED'})
    result={'base':'a9364ebfca51539e10440548d8551e182de2b050','preflight':data,
            'C1_C2_exact_y_translation':True,'P1_generator_disc_agreement_grid':True,
            'screen_family':'existing 4x4-pad route and shared long-straight rise schedule only',
            'candidates':options,'overall':'UNVERIFIED',
            'note':'No candidate passed this bounded A-screen; no global infeasibility proof. P2-B/C/D not executed.'}
    assert all(o['status']=='CANDIDATE FAILURE' for o in options)
    (HERE/'p2_checkpoint_results.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
    print('preflight C1/C2 D45 outside=259 each; D51 balcony outside=0; tested candidates',len(options),'A-family pass=0')
    print('locality components',[(c,[x['components_6'] for x in data[c]['balcony_locality']['objects']]) for c in ('C1','C2')])

if __name__=='__main__':main()

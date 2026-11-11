#!/usr/bin/env python3
"""Candidate-only voxel survey. No NBT or worldgen is written or selected."""
import ast, csv, gzip, hashlib, io, json, math
from collections import Counter
from pathlib import Path

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2];DATA=ROOT/'src/main/resources/data/moreminecrafts'
source=HERE/'audit_worldgen.py';tree=ast.parse(source.read_text());ns={'__file__':str(source)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(source),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];air={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'}
file=DATA/'structure/sky_tower/full_01.nbt';assert hashlib.sha256(file.read_bytes()).hexdigest()=='cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27'
tower=blocks(read(file));ts={p:v['id'] for p,v in tower.items() if v['id'] not in air}
styles=('snowy','meadow','forest','badlands','desert','rocky')
islands={s:blocks(read(DATA/f'structure/sky_islands/{s}/tower.nbt')) for s in styles}

def rot(x,z,r):
    for _ in range(r):x,z=-z,x
    return x,z

def category(block):
    if block in {'minecraft:jigsaw','minecraft:chest','minecraft:barrel','minecraft:trial_spawner','minecraft:vault','minecraft:spawner'} or any(s in block for s in ('chest','spawner','vault','barrel')):return 'functional_or_connector'
    if any(s in block for s in ('leaves','log','grass','fern','bush','flower','poppy','allium','sapling','azalea','dandelion','cornflower','tulip','daisy','cactus','snow')):return 'vegetation_or_snow'
    if any(s in block for s in ('stone_brick','stairs','slab','wall','fence','lantern','torch','copper','glass','brick')):return 'possible_architecture'
    return 'terrain_or_unclassified'

def at(p,dx,dz,h,r):
    x,y,z=p;X,Z=rot(x-24,z-24,r);return 21+dx+X,h+y,21+dz+Z

def nonair(block):return block and block['id'] not in air

def inspect(style,dx,dz,h,r):
    island=islands[style];overlap=[];types=Counter()
    for p,q in island.items():
        if not nonair(q) or p[1]<h:continue
        X,Z=rot(p[0]-21-dx,p[2]-21-dz,(-r)%4);local=(X+24,p[1]-h,Z+24)
        if local in ts:
            cat='functional_or_connector' if q.get('nbt') else category(q['id']);types[cat]+=1;overlap.append({'pos':p,'old':q['id'],'new':ts[local],'category':cat})
    base=[at(p,dx,dz,h,r) for p in ts if p[1]==0]
    supported=[p for p in base if nonair(island.get((p[0],p[1]-1,p[2])))]
    # Coarse deck candidates are only voxel footprints; no route or structural acceptance is implied.
    ax,az=rot(1,0,r);tx,tz=rot(0,1,r);entry=at((49,0,24),dx,dz,h,r)
    deck=[];blocked=[];contacts=[]
    for step in range(1,13):
        for width in range(-2,3):
            x=entry[0]+step*ax+width*tx;z=entry[2]+step*az+width*tz;p=(x,h-1,z)
            deck.append(p)
            for y in (h-1,h,h+1):
                q=island.get((x,y,z))
                if nonair(q):blocked.append({'pos':(x,y,z),'id':q['id'],'category':category(q['id'])})
            if nonair(island.get((x,h-1,z))):contacts.append(p)
    pts=[at(p,dx,dz,h,r) for p in ts];bounds=[[min(p[i] for p in pts),max(p[i] for p in pts)] for i in range(3)]
    # Relative chunk alignment varies with world origin mod 16; count all 256 residues.
    spans=[]
    for ox in range(16):
        for oz in range(16):
            spans.append(((bounds[0][1]+ox)//16-(bounds[0][0]+ox)//16+1)*((bounds[2][1]+oz)//16-(bounds[2][0]+oz)//16+1))
    return {'style':style,'offset':[dx,dz],'base_y':h,'relative_rotation_quarters':r,'tower_bounds':bounds,
      'entrance_center':entry,'overlap_by_category':dict(sorted(types.items())),'overlap_count':len(overlap),'overlap_blocks':overlap,
      'base_columns':len(base),'base_columns_with_nonair_below':len(supported),'base_support_samples':supported,
      'deck_footprint_5x12':deck,'deck_island_contacts':contacts,'deck_existing_blocks':blocked,
      'chunk_rectangle_min_max': [min(spans),max(spans)],'route_status':'UNVERIFIED','support_status':'UNVERIFIED',
      'note':'Nonair below is not proven solid support; deck is an unapproved hypothetical envelope, not a walkable path.'}

def main():
    # Same candidates across all six source islands; each is evaluated at four relative rotations.
    poses=[('A_center',0,0,17),('B_offset',-12,0,17),('C_offset_raised',-18,-8,20)]
    rows=[]
    for name,dx,dz,h in poses:
        for r in range(4):
            for style in styles:
                row=inspect(style,dx,dz,h,r);row['candidate']=name;rows.append(row)
    out={'source_sha256':hashlib.sha256(file.read_bytes()).hexdigest(),
        'island_source_sha256':{s:hashlib.sha256((DATA/f'structure/sky_islands/{s}/tower.nbt').read_bytes()).hexdigest() for s in styles},'candidates':rows,
        'general_styles':['snowy','meadow','forest','badlands','desert'],
        'mountain_styles':['snowy','rocky','meadow'],
        'definition':'coordinates island-local, before parent world rotation; actual jigsaw placement and mirror unverified',
        'candidate_selection':'three explicit comparisons, not an exhaustive search or final approval'}
    (HERE/'candidate_phase_c.json').write_text(json.dumps(out,sort_keys=True,separators=(',',':'))+'\n')
    with (HERE/'candidate_phase_c_table.csv').open('w',newline='') as f:
        fields=['candidate','style','offset','base_y','relative_rotation_quarters','overlap_count','overlap_by_category','base_columns','base_columns_with_nonair_below','entrance_center','deck_island_contacts','chunk_rectangle_min_max','route_status','support_status']
        w=csv.DictWriter(f,fieldnames=fields,extrasaction='ignore',lineterminator="\n");w.writeheader();w.writerows(rows)
    # Actual tower voxels and probe footprints for the three pictured, unrotated poses.
    with (HERE/'candidate_phase_c_voxels.csv.gz').open('wb') as raw:
        with gzip.GzipFile(fileobj=raw,mode='wb',mtime=0,filename='') as packed:
            with io.TextIOWrapper(packed,encoding='utf-8',newline='') as text:
                w=csv.writer(text);w.writerow(['candidate','component','x','y','z','block'])
                for name,dx,dz,h in poses:
                    for p,block in sorted(ts.items()):
                        w.writerow([name,'tower',*at(p,dx,dz,h,0),block])
                    row=next(v for v in rows if v['candidate']==name and v['style']=='meadow' and v['relative_rotation_quarters']==0)
                    for p in row['deck_footprint_5x12']:w.writerow([name,'hypothetical_deck',*p,''])
    return rows

if __name__=='__main__':
    rows=main();print('Candidate/style/rotation evaluations:',len(rows),'No final candidate is approved.')

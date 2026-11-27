#!/usr/bin/env python3
"""Finite Phase C-2 probes. Produces candidate data, never changes worldgen or NBT."""
import csv, gzip, hashlib, importlib.util, io, json
from collections import Counter
from pathlib import Path
P=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('old',P/'candidate_screen.py')
old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
AIR=old.air
SOLID={'minecraft:stone','minecraft:andesite','minecraft:cobblestone','minecraft:tuff','minecraft:dirt','minecraft:coarse_dirt','minecraft:grass_block','minecraft:podzol','minecraft:snow_block','minecraft:packed_ice','minecraft:orange_terracotta','minecraft:yellow_terracotta','minecraft:white_terracotta','minecraft:red_sandstone','minecraft:sandstone','minecraft:cut_sandstone','minecraft:smooth_sandstone','minecraft:gravel','minecraft:sand','minecraft:red_sand','minecraft:mossy_cobblestone'}

def transform(p,dx,dz,h,r):
    x,y,z=p
    for _ in range(r): x,z=48-z,x
    return x-3+dx,y+h,z-3+dz

def generate(style,dx,dz,r,width):
    h=17;island=old.islands[style]; tower={transform(p,dx,dz,h,r):q for p,q in old.tower.items() if q['id'] not in ('minecraft:structure_void',)}
    base=[transform(p,dx,dz,h,r) for p,q in old.tower.items() if p[1]==0 and q['id'] not in AIR]
    # Local template +X entrance; slab is lower half at y=17, top y=17.5.
    deck=[];rail=[]
    for x in range(50,54):
        for z in range(24-width//2,25+width//2): deck.append(transform((x,0,z),dx,dz,h,r))
        for z in (24-width//2-1,24+width//2+1):rail.append(transform((x,1,z),dx,dz,h,r))
    supports=[];support_status=[]
    # Four deliberately small experimental piers; not a claim of adequate support.
    for x,z in ((14,14),(14,34),(34,14),(34,34)):
        px,_,pz=transform((x,0,z),dx,dz,h,r);found=False;col=[]
        for y in range(h-1,h-13,-1):
            p=(px,y,pz);q=island.get(p)
            if q and q['id'] in SOLID:found=True;break
            if q and q['id'] not in AIR:break
            col.append(p)
        if found:supports.extend(col)
        support_status.append({'column':[px,pz],'anchored_to_whitelisted_parent':found,'blocks':col if found else [],'reason':'no known-solid anchor within 12 blocks or unknown intervening block' if not found else 'known-solid parent contact; geometry only'})
    # Exploratory horizontal brackets: stop at first existing known-solid parent block.
    # These are trial voxels only, not a proven support design.
    brackets=[]
    for z in (14,34):
        span=[]; anchor=None
        for x in range(14,35):
            px,_,pz=transform((x,0,z),dx,dz,h,r); p=(px,h-2,pz); q=island.get(p)
            if q and q['id'] in SOLID:
                anchor=p;break
            if q and q['id'] not in AIR:
                break
            span.append(p)
        if anchor and span:
            top=(span[0][0],h-1,span[0][2]); q=island.get(top)
            if not q or q['id'] in AIR:
                supports.extend(span+[top])
            else:
                span=[]
        brackets.append({'z_local':z,'anchor':anchor,'span':span,'note':'experimental bracket; adequacy and player clearance unverified'})
    types=Counter();changes=[]
    for p,q in tower.items():
        prior=island.get(p)
        if prior and prior['id'] not in AIR and prior['id'] != q['id']:
            kind='functional_or_connector' if prior.get('nbt') else old.category(prior['id']);types[kind]+=1
            changes.append({'pos':p,'before':prior['id'],'after':q['id'],'category':kind})
    merged=dict(island);merged.update(tower)
    addons=[]
    for name,pts,block in (('deck',deck,'minecraft:stone_brick_slab'),('rail',rail,'minecraft:stone_brick_wall'),('support',supports,'minecraft:stone_bricks')):
        for p in pts:
            before=merged.get(p,{}).get('id','minecraft:air')
            addons.append({'pos':p,'component':name,'block':block,'before':before})
            merged[p]={'id':block}
    below=Counter()
    for x,y,z in base:
        q=island.get((x,y-1,z));bid=q['id'] if q else 'minecraft:air'
        below['known_solid' if bid in SOLID else ('empty' if bid in AIR else 'other_or_unknown')]+=1
    addon_changes=[a for a in addons if a['before'] not in AIR and a['before']!=a['block']]
    entrance=transform((49,0,24),dx,dz,h,r)
    return {'id':f'{style}_dx{dx}_dz{dz}_r{r}_w{width}','style':style,'dx':dx,'dz':dz,'base_y':h,'relative_rotation':r,'width':width,'bridge_length':4,'entrance_slab':entrance,'entrance_slab_top_y':h+0.5,
      'tower_overwrites_parent':changes,'tower_change_categories':dict(types),'bridge_and_support_voxels':addons,'addon_overwrites_nonair':addon_changes,
      'base_below':dict(below),'base_below_coordinates':[{'base':p,'below':island.get((p[0],p[1]-1,p[2]),{}).get('id','minecraft:air')} for p in base],
      'pier_probes':support_status,'bracket_probes':brackets,'status':'UNVERIFIED', 'warnings':['Provisional parent→tower→addons order only; processors/jigsaw, neighboring children, air semantics and state rotation not validated.', '4-block straight slab bridge; island route, step geometry, headroom and protection from falls unverified.', 'Four small piers are prototypes, not support adequacy.']}

def main():
    # Densify near prior B: dx -12 and dz 0; compare widths, not a selected design.
    rows=[generate(s,dx,dz,r,w) for s in old.styles for dx in (-14,-12,-10) for dz in (-2,0,2) for r in range(4) for w in (3,5)]
    out={'base':'92f862f73b3eede3eab8190b075bc1fc7d255cbd','tower_sha256':hashlib.sha256(old.file.read_bytes()).hexdigest(), 'description':'sampled eccentric B neighborhood; island-local coordinates; no final selection','candidates':rows}
    (P/'phase_c2_candidates.json').write_text(json.dumps(out,separators=(',',':'),sort_keys=True)+'\n')
    with (P/'phase_c2_candidate_table.csv').open('w',newline='') as f:
        w=csv.writer(f,lineterminator='\n');w.writerow(['id','tower_changes','functional_changes','addon_overwrites','base_known_solid','base_empty','base_other','anchored_piers','bridge_length','width'])
        for v in rows:
            c=v['base_below'];w.writerow([v['id'],len(v['tower_overwrites_parent']),v['tower_change_categories'].get('functional_or_connector',0),len(v['addon_overwrites_nonair']),c.get('known_solid',0),c.get('empty',0),c.get('other_or_unknown',0),sum(x['anchored_to_whitelisted_parent'] for x in v['pier_probes']),v['bridge_length'],v['width']])
    # Exact local tower block states, including explicitly stored air; poses transform them.
    with (P/'phase_c2_tower_local_voxels.json.gz').open('wb') as f:
        with gzip.GzipFile(fileobj=f,mode='wb',mtime=0,filename='') as g:
            g.write(json.dumps([{'pos':p,'state':q} for p,q in sorted(old.tower.items())],sort_keys=True,separators=(',',':')).encode())
    print(f'{len(rows)} candidates written; none declared safe')
if __name__=='__main__':main()

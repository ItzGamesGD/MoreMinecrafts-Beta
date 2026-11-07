"""Independent occupancy and swept-AABB navigation; never imports generator."""
import csv,json,math,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('v',H.parent/'sky_tower_geometry_03c/p3/p3_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
rows=list(csv.DictReader((H/'blocks.csv').open()));blocks={};groups={};occ={}
for r in rows:
 p=tuple(int(r[k]) for k in ('x','y','z'));shape='slab' if r['block'].endswith('_slab') else 'full';blocks[p]=(shape,'');groups[p]=r['group'];occ[p]=r
scene=v.SlabScene(blocks);model=json.loads((H/'model.json').read_text())
goals=[(1,f+1,1) for f in model['floors']];names=['F1','F2','F3','F4','SANCTUM']
for i,b in enumerate(model['balconies']):
 goals.append((1,b['top'],4*(22*b['sign'])+1));names.append(f'balcony{i+1}')
r=scene.search((4*23+1,1,1),goals,limit=1200000);paths=r.pop('witness_paths');out={'navigation':r,'targets':names,'height':max(p[1] for p in blocks)-min(p[1] for p in blocks)+1,'boundary_center_diameter':2*max(math.hypot(p[0],p[2]) for p in blocks),'block_counts':{},'notes':['Full blocks and bottom slabs modeled. Spawner/vault treated conservatively as full cubes. Actual client validation separate.']}
for row in rows:out['block_counts'][row['block']]=out['block_counts'].get(row['block'],0)+1
(H/'verify.json').write_text(json.dumps(out,indent=1)+'\n');(H/'paths.json').write_text(json.dumps(dict(zip(names,paths)),separators=(',',':'))+'\n');print('reached',dict(zip(names,r['reached'])),'visited',r['visited_states'])

"""Actual occupied-geometry regression probes; never imports the builder."""
import csv,json,math,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('sa',H/'safety.py');sa=importlib.util.module_from_spec(s);s.loader.exec_module(sa)
s=importlib.util.spec_from_file_location('v',H.parent/'sky_tower_geometry_03c/p3/p3_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
rows=list(csv.DictReader((H/'blocks.csv').open()));b={tuple(int(r[k]) for k in ('x','y','z')):('slab' if r['block'].endswith('_slab') else 'full','') for r in rows};results=[]
def ok(name,condition):
 results.append({'name':name,'pass':bool(condition)});print(('PASS ' if condition else 'FAIL ')+name)
ok('final occupied walking faces guarded',not sa.audit(rows))
# Remove the two-block terminal seam guard, retaining its structural base.
cut=[r for r in rows if not(int(r['x'])==16 and int(r['z'])==3 and int(r['y']) in (65,66))]
ok('removed sanctum seam guard is detected',bool(sa.audit(cut)))
# Inject a wall/pier at a known clear source footprint; frozen AABB checker must reject it.
u,vv,h=93,1,40;scene=v.SlabScene(b);ok('entrance player box clear',scene.stand(u,vv,h))
for name in ('pillar intrusion','wall intrusion','insufficient headroom'):
 altered=dict(b);altered[23,1 if name!='insufficient headroom' else 2,0]=('full','');sc=v.SlabScene(altered);ok(name+' detected',not sc.stand(u,vv,h))
# A barricade across the entire ground-level entrance mouth.
rr=rows+[{'x':'24','y':str(y),'z':str(z),'block':'stone_bricks','group':'probe','properties':'{}'} for y in (1,2) for z in range(-2,3)]
bb={tuple(int(r[k]) for k in ('x','y','z')):('slab' if r['block'].endswith('_slab') else 'full','') for r in rr}
ok('entrance barricade blocks player box',not v.SlabScene(bb).stand(4*24+1,1,40))
def outside(rr):return [r for r in rr if 4*(int(r['x'])**2+int(r['z'])**2)>=51**2]
ok('baseline chosen D51 boundary clear',not outside(rows))
ok('outward block crosses chosen D51 envelope',bool(outside(rows+[dict(x='26',y='0',z='0')])))
s=importlib.util.spec_from_file_location('sweep',H/'balcony_sweeps.py');sweep=importlib.util.module_from_spec(s);s.loader.exec_module(sweep)
bal=json.loads((H/'model.json').read_text())['balconies'][3]
ok('central balcony connector clear',not sweep.colliders(rows,bal))
obstacle=dict(x='0',y=str(math.ceil(bal['top'])),z='21',block='stone_bricks',group='probe')
ok('blocked balcony connector detected',bool(sweep.colliders(rows+[obstacle],bal)))
paths=json.loads((H/'paths.json').read_text());ok('balcony route witnesses exist',all(paths['balcony'+str(i)] for i in range(1,9)))
# Arc connectivity audit separately detects disconnected pieces, not just walking faces.
conn=json.loads((H/'connectivity.json').read_text());ok('all non-relic masonry is face-connected',conn['unintended']==0)
(H/'regressions.json').write_text(json.dumps(results,indent=1)+'\n');assert all(r['pass'] for r in results)

"""Sample continuous radial collision intervals; full-build mapping, no generator import.
P5 interval code reused with five flights and actual occupancy; no global-minimum claim.
"""
import csv,json,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('w',H.parent/'sky_tower_geometry_03c/p5/p5_width.py');w=importlib.util.module_from_spec(s);s.loader.exec_module(w)
rows=[]
for r in csv.DictReader((H/'blocks.csv').open()):
 rows.append({'x':r['x'],'y':r['y'],'z':r['z'],'shape':'slab' if r['block'].endswith('_slab') else 'full','group':'stairs' if r['group'] in ('stair','portal_floor') else r['group']})
obj={'transform':{'dy':0},'floors':[0,10,21,33,47,64]};rr=w.analyse(rows,obj,step=.5)
summary={str(L):{key:min((r for r in rr if r['lap']==L),key=lambda r:r[key]) for key in ('physical_width','centre_width')} for L in range(5)}
(H/'width.json').write_text(json.dumps({'angle_step_degrees':.5,'nominal_tread_band':[14.5,20.5],'sample_minima':summary,'limitations':['sampled angles; not global minimum','fixed-height cross-section; step transitions can collapse centre interval without route failure','physical width distinct from centre span and multiplayer passing']},indent=1)+'\n')
print([(L,v['physical_width']['physical_width']) for L,v in summary.items()])

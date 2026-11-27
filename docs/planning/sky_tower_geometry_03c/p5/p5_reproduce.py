"""Reproduction evidence, P3 regeneration in temporary directory; P1–P4 read-only."""
import csv,hashlib,importlib.util,json,tempfile,subprocess,sys
from pathlib import Path
H=Path(__file__).resolve().parent;P3=H.parent/'p3';SRC=P3/'data/candidates/K2_8balc_A5relaxed_slab_land3';D=H/'data'
s=importlib.util.spec_from_file_location('gen',P3/'p3_generate.py');g=importlib.util.module_from_spec(s);s.loader.exec_module(g)
o=json.loads((SRC/'objects_C1.json').read_text());sel=dict(o['selection']);sel['pillars']={L:[tuple(p['xz']) for p in o['pillars'] if p['lap']==L] for L in range(4)}
res={}
with tempfile.TemporaryDirectory() as tmp:
 out=Path(tmp);g.export(*g.build(g.Geometry(19.5),sel,rise='slab',landing_cells=3),out,sel)
 res['P3_regenerated_equal']={f:(SRC/f).read_bytes()==(out/f).read_bytes() for f in ('blocks_C1.csv','blocks_C2.csv','objects_C1.json','objects_C2.json','tread_tops_C1.json')}
 subprocess.run([sys.executable,'-I',str(H/'p5_generate.py'),'--out',str(out/'integrated')],check=True)
 res['P5_regenerated_equal']={p.name:p.read_bytes()==(out/'integrated'/p.name).read_bytes() for p in (out/'integrated').iterdir() if (D/p.name).exists() for p in [D/p.name]}
# Representative actual section coordinates vs immutable P3 exports
p3={(r['group'],r['x'],r['y'],r['z'],r['shape'],r['facing']) for r in csv.DictReader((SRC/'blocks_C1.csv').open())}
res['P4_sections']={}
for fn in ('section_blocks.csv','section_blocks_k2_lap3.csv'):
 rr=list(csv.DictReader((H.parent/'p4/data'/fn).open()));mem=[r for r in rr if r['group']!='tower'];missing=[r for r in mem if tuple(r[k] for k in ('group','x','y','z','shape','facing')) not in p3]
 res['P4_sections'][fn]={'non_tower_claims':len(mem),'not_in_P3':len(missing)}
res['P4_historic_walk_results']=json.loads((H.parent/'p4/data/walk_summary.json').read_text())
res['P4_historic_placement']={p.name:json.loads(p.read_text()) for p in sorted((H.parent/'p4/data').glob('server_placement_check*.json'))}
res['note']='P4 evidence carried as historical only; no new LIVE pass. P3 regeneration uses archived solved selection; CP-SAT search not rerun.'
(D/'reproduction.json').write_text(json.dumps(res,indent=1)+'\n')
print('P3:',res['P3_regenerated_equal']);print('P5:',res['P5_regenerated_equal']);print('P4:',res['P4_sections'])
assert all(res['P3_regenerated_equal'].values()) and all(res['P5_regenerated_equal'].values()) and all(not r['not_in_P3'] for r in res['P4_sections'].values())

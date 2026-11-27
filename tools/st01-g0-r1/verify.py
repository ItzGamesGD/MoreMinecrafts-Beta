#!/usr/bin/env python3
"""Two clean deterministic design runs and independent Java long-arithmetic selection reference."""
import argparse,hashlib,json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);args=p.parse_args()
args.out.mkdir(parents=True,exist_ok=True)
for name in ('first','second'):
    subprocess.run([sys.executable,str(ROOT/'tools/st01-g0-r1/blueprint.py'),'--out',str(args.out/name)],check=True)
files={}
for name in ('blueprint.json','d1-exceptions.json','validation.json','COMPONENTS.md'):
    a=(args.out/'first'/name).read_bytes();b=(args.out/'second'/name).read_bytes()
    assert a==b, 'Repeated output differs: '+name
    files[name]=hashlib.sha256(a).hexdigest()
bp=json.loads((args.out/'first/blueprint.json').read_text())
java=subprocess.check_output(['java','-XX:ActiveProcessorCount=2',str(ROOT/'tools/st01-g0-r1/SelectionReference.java')],text=True).splitlines()
expected=[v[-1] for v in bp['selection_contract']['vectors']]
assert java==expected,(java,expected)
report={'status':'PASS','meaning':'Reproducible design verification, NOT owner approval or production placement',
        'repeated_output_sha256':files,'selection_reference_java':java,
        'java_reference_pass':True,'python_java_vectors':bp['selection_contract']['vectors']}
(args.out/'reproducibility.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))

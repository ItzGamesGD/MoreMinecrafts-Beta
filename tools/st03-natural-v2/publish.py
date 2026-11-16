import json,os,subprocess,sys
from pathlib import Path
folder=Path(sys.argv[1]);name=sys.argv[2]
p=folder/'result.json'
if p.exists():data=json.loads(p.read_text())
else:data={'status':'FAIL','error':'No completed probe result'}
if (folder/'build.log').exists():data['build_log_tail']=(folder/'build.log').read_text()[-30000:]
if (folder/'structure-api.txt').exists():data['structure_api']=(folder/'structure-api.txt').read_text()[:60000]
if not p.exists() and (folder/'probe.log').exists():data['probe_log_tail']=(folder/'probe.log').read_text()[-25000:]
data['head_sha']=os.environ['GITHUB_SHA'];data['run_id']=os.environ['GITHUB_RUN_ID']
text=json.dumps(data,separators=(',',':'))
for n,i in enumerate(range(0,len(text),50000),1):
 payload={'name':f'st03-natural-{name}-{n}','head_sha':os.environ['GITHUB_SHA'],'status':'completed',
  'conclusion':'success' if data['status']=='PASS' else 'failure',
  'output':{'title':f'ST03 V2 {name}','summary':f"Run {os.environ['GITHUB_RUN_ID']} {data['status']}",'text':text[i:i+50000]}}
 p=subprocess.run(['gh','api','-X','POST',f"repos/{os.environ['GITHUB_REPOSITORY']}/check-runs",'--input','-'],input=json.dumps(payload),text=True,capture_output=True)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])

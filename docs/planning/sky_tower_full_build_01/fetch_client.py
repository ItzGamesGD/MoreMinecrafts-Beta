import json,urllib.request,concurrent.futures,hashlib
from pathlib import Path
h=Path('/tmp/fullbuild-mc');h.mkdir(exist_ok=True)
if not (h/'version.json').exists():(h/'version.json').write_bytes(urllib.request.urlopen('https://piston-meta.mojang.com/v1/packages/80608ff6753705b6a82e2cb86a5937d4d233abd4/26.3.json').read())
v=json.loads((h/'version.json').read_text());jobs=[]
def add(url,path,sha):jobs.append((url,path,sha))
add(v['downloads']['server']['url'],h/'server.jar',v['downloads']['server']['sha1'])
add(v['downloads']['client']['url'],h/'client.jar',v['downloads']['client']['sha1'])
for lib in v['libraries']:
 # Linux only rules: omit artifacts expressly limited to other OSes.
 rules=lib.get('rules',[])
 if rules and not any(r.get('action')=='allow' and (not r.get('os') or r['os'].get('name')=='linux') for r in rules):continue
 a=lib.get('downloads',{}).get('artifact')
 if a:add(a['url'],h/'libraries'/a['path'],a['sha1'])
idx=v['assetIndex'];data=urllib.request.urlopen(idx['url']).read();(h/'assets/indexes').mkdir(parents=True,exist_ok=True);(h/f"assets/indexes/{idx['id']}.json").write_bytes(data)
for value in json.loads(data)['objects'].values():
 sha=value['hash'];add(f'https://resources.download.minecraft.net/{sha[:2]}/{sha}',h/'assets/objects'/sha[:2]/sha,sha)
def fetch(job):
 url,p,sha=job
 if p.exists() and hashlib.sha1(p.read_bytes()).hexdigest()==sha:return
 p.parent.mkdir(parents=True,exist_ok=True)
 for attempt in range(3):
  try:
   raw=urllib.request.urlopen(url,timeout=40).read();assert hashlib.sha1(raw).hexdigest()==sha;p.write_bytes(raw);return
  except Exception:
   if attempt==2:raise
with concurrent.futures.ThreadPoolExecutor(max_workers=24) as e:list(e.map(fetch,jobs))
print('verified downloads',len(jobs));(h/'classpath.txt').write_text(':'.join([str(h/'client.jar')]+[str(p) for u,p,s in jobs if str(p).endswith('.jar') and p.name!='client.jar']))

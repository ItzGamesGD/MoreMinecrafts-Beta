"""Package the built NBT as a manual-install vanilla datapack; no worldgen registration."""
import zipfile,json,hashlib
from pathlib import Path
H=Path(__file__).resolve().parent;m=json.loads((H/'model.json').read_text());assert hashlib.sha256((H/'sky_tower_full_01.nbt').read_bytes()).hexdigest()==m['sha256']
files={'pack.mcmeta':json.dumps({'pack':{'description':'Sky Tower Full Build 01 — manual placement candidate','min_format':[121,0],'max_format':[121,0]}},indent=1).encode(),'data/sky_tower/structure/full_01.nbt':(H/'sky_tower_full_01.nbt').read_bytes(),'data/sky_tower/function/place_here.mcfunction':b'place template sky_tower:full_01 ~-24 ~ ~-24 none none 1.0 0 strict\n'}
with zipfile.ZipFile(H/'sky_tower_full_01_datapack.zip','w',compression=zipfile.ZIP_DEFLATED) as z:
 for name,data in sorted(files.items()):
  info=zipfile.ZipInfo(name,(2026,11,7,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,data)
print('manual placement datapack packaged')

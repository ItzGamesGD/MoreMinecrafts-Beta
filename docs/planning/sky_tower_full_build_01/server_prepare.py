"""Prepare isolated server datapack and strict full-bbox comparison functions."""
import json,csv,shutil,sys
from pathlib import Path
H=Path(__file__).resolve().parent;S=Path(sys.argv[1]);pack=S/'world/datapacks/fullbuild';(pack/'data/fullbuild/structure').mkdir(parents=True,exist_ok=True);(pack/'data/fullbuild/function').mkdir(exist_ok=True)
(pack/'pack.mcmeta').write_text(json.dumps({'pack':{'description':'Sky tower full build candidate','min_format':[121,0],'max_format':[121,0]}}))
shutil.copyfile(H/'sky_tower_full_01.nbt',pack/'data/fullbuild/structure/tower.nbt')
m=json.loads((H/'model.json').read_text());rows=list(csv.DictReader((H/'blocks.csv').open()));b={tuple(int(r[k]) for k in ('x','y','z')):r for r in rows};commands=['scoreboard players set mismatch fullbuild 0'];count=0
for x in range(m['origin'][0],m['max'][0]+1):
 for y in range(80):
  for z in range(m['origin'][2],m['max'][2]+1):
   r=b.get((x,y,z));state='minecraft:air'
   if r:
    props=json.loads(r['properties']);props.pop('trial_spawner_state',None);state='minecraft:'+r['block']+('['+','.join(f'{k}={v}' for k,v in sorted(props.items()))+']' if props else '')
   commands.append(f'execute unless block {x+1000} {y+65} {z+1000} {state} run scoreboard players add mismatch fullbuild 1');count+=1
names=[]
for i in range(0,len(commands),10000):
 name=f'check_{i//10000}';names.append(name);(pack/f'data/fullbuild/function/{name}.mcfunction').write_text('\n'.join(commands[i:i+10000])+'\n')
(H/'server_manifest.json').write_text(json.dumps({'positions':count,'functions':names,'world_offset':[1000,65,1000],'placement':[m['origin'][0]+1000,65,m['origin'][2]+1000]},indent=1)+'\n');print(count)

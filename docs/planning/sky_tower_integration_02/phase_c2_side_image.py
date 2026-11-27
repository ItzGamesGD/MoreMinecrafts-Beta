#!/usr/bin/env python3
"""Top-down coordinate comparison; not an in-game screenshot."""
import ast,json
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
P=Path(__file__).resolve().parent;ROOT=P.parents[2];src=P/'audit_worldgen.py';tree=ast.parse(src.read_text());ns={'__file__':str(src)}
exec(compile(ast.Module(body=[n for n in tree.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.FunctionDef))],type_ignores=[]),str(src),'exec'),ns)
read,blocks=ns['read'],ns['blocks'];d=ROOT/'src/main/resources/data/moreminecrafts'
cs=json.loads((P/'phase_c2_side_access.json').read_text())['candidates'];fig,axes=plt.subplots(2,3,figsize=(15,9),constrained_layout=True)
for ax,c in zip(axes.flat,cs):
 island=blocks(read(d/f'structure/sky_islands/{c["style"]}/tower.nbt'))
 for x in range(32,41):
  for z in range(17,26):
   b=island.get((x,16,z),{}).get('id','minecraft:air');color='#d6e8cd' if b not in ('minecraft:air','minecraft:structure_void') else '#f3f5f7'
   ax.add_patch(Rectangle((x-.5,z-.5),1,1,facecolor=color,edgecolor='#dddddd',lw=.4))
 for v in c['bridge_and_support_voxels']:
  x,y,z=v['pos'];
  if not (32<=x<=40 and 17<=z<=25):continue
  if v['component']=='deck':ax.add_patch(Rectangle((x-.45,z-.45),.9,.9,color='#dbaf67',alpha=.7))
  if v['component']=='rail':ax.scatter(x,z,marker='s',s=45,c='#a84d31',zorder=4)
 route=c['route'];ax.plot([q['floor'][0] for q in route],[q['floor'][2] for q in route],'-o',color='#0868ac',lw=2,ms=5,zorder=5)
 for x,y,z in c['removed_rail_positions']:ax.scatter(x,z,marker='x',s=70,c='red',zorder=6)
 ax.set(xlim=(31.5,40.5),ylim=(25.5,16.5),xticks=range(32,41,2),yticks=range(17,26,2),xlabel='X',ylabel='Z',title=f'{c["style"]}: {len(c["head_nonair_cells"])} nonair head cells')
 ax.set_aspect('equal')
fig.suptitle('Candidate side access: island y16 green, slab gold, rail brown, opening red X, proposed route blue. Coordinate diagram; not a game screenshot.',fontsize=11)
fig.savefig(P/'phase_c2_side_access.png',dpi=140)

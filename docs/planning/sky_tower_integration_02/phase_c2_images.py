#!/usr/bin/env python3
"""Coordinate-derived static prototypes, not live Minecraft screenshots."""
import importlib.util,json
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
P=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('old',P/'candidate_screen.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
candidates=json.loads((P/'phase_c2_candidates.json').read_text())['candidates']
fig=plt.figure(figsize=(18,12),constrained_layout=True)
for i,style in enumerate(m.styles):
 c=next(x for x in candidates if x['style']==style and x['dx']==-12 and x['dz']==0 and x['width']==3)
 ax=fig.add_subplot(2,3,i+1,projection='3d')
 island={}
 for (x,y,z),q in m.islands[style].items():
  if m.nonair(q) and y>island.get((x,z),-999):island[(x,z)]=y
 ip=[(x,y,z) for (x,z),y in island.items()]
 solids=set(m.ts);tp=[m.at(p,-12,0,17,0) for p in solids if sum(p)%4==0 and any((p[0]+a,p[1]+b,p[2]+d) not in solids for a,b,d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))]
 for pts,col,size,alpha in ((ip,'#668b58',2,.4),(tp,'#928b80',.5,.17),([v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='deck'],'#ffbd37',24,1),([v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='rail'],'#ef6f3f',15,1),([v['pos'] for v in c['bridge_and_support_voxels'] if v['component']=='support'],'#275eb0',12,1)):
  if pts:ax.scatter([q[0] for q in pts],[q[2] for q in pts],[q[1] for q in pts],c=col,s=size,alpha=alpha,depthshade=False)
 ax.set_title(f'{style} / dx=-12 dz=0 y=17 / width=3\n{len(c["tower_overwrites_parent"])} tower overwrites; {sum(x["anchored_to_whitelisted_parent"] for x in c["pier_probes"])}/4 piers reach parent')
 ax.set_xlabel('X');ax.set_ylabel('Z');ax.set_zlabel('Y');ax.set_box_aspect((1,1,1.3));ax.view_init(25,-55)
fig.suptitle('Coordinate reconstruction: island top, sampled tower surface, proposed bridge and piers. Gold=deck; orange=railing; blue=piers. NOT a Minecraft screenshot or approved design.',fontsize=10)
fig.savefig(P/'phase_c2_candidate_comparison.png',dpi=140)

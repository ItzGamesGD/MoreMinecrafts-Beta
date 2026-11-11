#!/usr/bin/env python3
"""Voxel-coordinate visual comparison. Requires matplotlib; not a Minecraft render."""
import importlib.util
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

HERE=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('candidates',HERE/'candidate_screen.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)

def surface(cells):
    top={}
    for x,y,z in cells:
        if (x,z) not in top or y>top[x,z]:top[x,z]=y
    return [(x,y,z) for (x,z),y in sorted(top.items())]

fig=plt.figure(figsize=(18,12),constrained_layout=True)
tower_positions=set(m.ts)
exposed=[p for p in sorted(tower_positions) if (p[0]+p[1]+p[2])%3==0 and any((p[0]+dx,p[1]+dy,p[2]+dz) not in tower_positions for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))]
poses=[('A: centered',0,0,17),('B: eccentric',-12,0,17),('C: eccentric + raised',-18,-8,20)]
for row,style in enumerate(('meadow','rocky')):
    island=surface(p for p,v in m.islands[style].items() if m.nonair(v))
    for col,(label,dx,dz,h) in enumerate(poses):
        ax=fig.add_subplot(2,3,row*3+col+1,projection='3d')
        model=[m.at(p,dx,dz,h,0) for p in exposed]
        evidence=m.inspect(style,dx,dz,h,0)
        for pts,color,size,alpha in ((island,'#6b9366',1.4,.45),(model,'#918979',.65,.32),
                                     ([tuple(o['pos']) for o in evidence['overlap_blocks']],'#cc3040',5,.9),
                                     (evidence['deck_footprint_5x12'],'#eca63f',3,.65)):
            if pts:ax.scatter([p[0] for p in pts],[p[2] for p in pts],[p[1] for p in pts],s=size,c=color,alpha=alpha,depthshade=False)
        ax.set_title(f'{label} / {style}\noverlap {evidence["overlap_count"]}; base contacts {evidence["base_columns_with_nonair_below"]}/{evidence["base_columns"]}',fontsize=10)
        ax.set_xlabel('X');ax.set_ylabel('Z');ax.set_zlabel('Y');ax.set_box_aspect((1,1,1.3));ax.view_init(elev=24,azim=-57)
fig.suptitle('Coordinate-derived candidate comparisons; island top, sampled exposed tower blocks, rotation 0. Green: island; grey: tower; red: overlap; gold: hypothetical deck. Not Minecraft renders or approved builds.',fontsize=10)
fig.savefig(HERE/'candidate_phase_c_comparison.png',dpi=150)

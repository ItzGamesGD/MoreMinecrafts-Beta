#!/usr/bin/env python3
"""Bounded screening, not a route search or an approved design."""
import csv
import importlib.util
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('screen',HERE/'candidate_screen.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)

def run():
    out=HERE/'candidate_grid.csv'
    with out.open('w',newline='') as f:
        w=csv.writer(f,lineterminator="\n");w.writerow(['style','dx','dz','base_y','relative_rotation_quarters','solid_overlap','base_columns','nonair_below_base'])
        for r in range(4):
            rot_tower={(s.rot(x-24,z-24,r)[0],y,s.rot(x-24,z-24,r)[1]) for x,y,z in s.ts}
            base={(x,z) for x,y,z in rot_tower if y==0}
            for dx in (-18,-12,-6,0,6):
                for dz in (-12,-6,0,6,12):
                    for h in (15,17,20):
                        for style in s.styles:
                            island=s.islands[style];cx,cz=21+dx,21+dz
                            overlap=sum(1 for (x,y,z),q in island.items() if y>=h and s.nonair(q) and (x-cx,y-h,z-cz) in rot_tower)
                            support=sum(1 for x,z in base if s.nonair(island.get((cx+x,h-1,cz+z))))
                            w.writerow([style,dx,dz,h,r,overlap,len(base),support])
    print('1800 bounded pose/style rows; no route or support proof:',out)

if __name__=='__main__':run()

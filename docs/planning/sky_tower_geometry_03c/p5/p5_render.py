"""Horizontal and vertical sections from exported actual claims; transparent dashed reservations."""
import csv,json
from pathlib import Path
H=Path(__file__).resolve().parent;D=H/'data';O=D/'svg';O.mkdir(exist_ok=True)
colors={'stairs':'#e9a23b','pillar':'#36759b','rail':'#b84b4b','arcade_arch':'#63a4c7','balcony_floor':'#7cbe75','balcony_rail':'#b84b4b','balcony_support':'#647556'}
for conv in ('C1','C2'):
 rows=list(csv.DictReader((D/f'blocks_{conv}.csv').open()));res=json.loads((D/f'reservations_{conv}.json').read_text());obj=json.loads((D/f'objects_{conv}.json').read_text())
 def render(name,select,proj,bounds):
  x0,y0,w,h=bounds;parts=[f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x0} {y0} {w} {h}"><rect x="{x0}" y="{y0}" width="{w}" height="{h}" fill="white"/><text x="{x0+1}" y="{y0+2}" font-size="1.3">{name}: actual blocks; dashed = trial reservation</text>']
  for r in rows:
   if select(r):
    x,y=proj(r);color=colors.get(r['group'],'#c9c9c9');parts.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{color}" stroke="#555" stroke-width=".035"><title>{r["group"]} ({r["x"]},{r["y"]},{r["z"]})</title></rect>')
  parts.append('</svg>');(O/f'{name}.svg').write_text(''.join(parts))
 for y in range(80):render(f'{conv}_horizontal_y{y:02}',lambda r,Y=y:int(r['y'])==Y,lambda r:(int(r['x']),int(r['z'])),(-28,-30,57,59))
 for z in (0,5,19,-19):render(f'{conv}_XY_z{z}',lambda r,Z=z:int(r['z'])==Z,lambda r:(int(r['x']),79-int(r['y'])),(-28,-4,57,86))
 for x in (0,5,19,-19):render(f'{conv}_ZY_x{x}',lambda r,X=x:int(r['x'])==X,lambda r:(int(r['z']),79-int(r['y'])),(-28,-4,57,86))
 # explicit reservation projection separate so solid search envelopes never look like placed architecture
 parts=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="-30 -5 61 90"><rect x="-30" y="-5" width="61" height="90" fill="white"/><text x="-29" y="-2" font-size="1.5">Trial reservation projection XY; no solid occupancy implied</text>']
 for r in res:
  a,b=r['min'],r['max_exclusive'];parts.append(f'<rect x="{a[0]}" y="{80-b[1]}" width="{b[0]-a[0]}" height="{b[1]-a[1]}" stroke="#9347b6" stroke-dasharray=".5,.3" stroke-width=".15" fill="none"><title>{r["id"]}</title></rect>')
 parts.append('</svg>');(O/f'{conv}_reservations_XY.svg').write_text(''.join(parts))
print('160 horizontal sections, 16 vertical sections, 2 reservation projections')

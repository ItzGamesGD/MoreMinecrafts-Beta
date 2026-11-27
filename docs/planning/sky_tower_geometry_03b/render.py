"""Failure plans/sections are derived directly from collision boxes and CSV cells."""
import importlib.util
import json
import math
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('independent_plot_input',HERE/'verify.py')
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
COL={'stairs':'#d7a549','pillar':'#67564a','rail':'#c75d4d','balcony_floor':'#72a9d7',
     'balcony_rail':'#b44545','balcony_support':'#8a9ba9','arcade_arch':'#658b9f',
     'wall_link_arch':'#658b9f','canopy':'#3a8b77','gateway_frame':'#9979ac'}


def draw(out,groups,base,title,target,section=False):
    x0,y0,z0=target
    content=['<svg xmlns="http://www.w3.org/2000/svg" width="940" height="650">',
             '<rect width="940" height="650" fill="#faf9f5"/>',
             f'<text x="20" y="28" font-family="sans-serif" font-size="17">{title}</text>',
             f'<text x="20" y="52" font-family="sans-serif" font-size="13">Target block {target}; '+
             (f'XY section at z={z0}' if section else f'XZ occupied-cell plan at y={y0}')+'</text>']
    if section:
        minx,maxx=max(-26,x0-12),min(26,x0+12);miny,maxy=max(0,y0-6),min(79,y0+8)
        scale=min(34,800/(maxx-minx+1),500/(maxy-miny+1))
        def rect(x,y,X,Y,color):
            content.append(f'<rect x="{55+(x-minx)*scale:.2f}" y="{85+(maxy+1-Y)*scale:.2f}" width="{(X-x)*scale:.2f}" height="{(Y-y)*scale:.2f}" fill="{color}" stroke="#fff" stroke-width=".3"/>')
        for name,blocks in [('base',base),*groups.items()]:
            for (x,y,z),(kind,facing) in sorted(blocks.items()):
                if z!=z0 or not minx<=x<=maxx or not miny<=y<=maxy:continue
                if kind=='full':rect(x,y,x+1,y+1,COL.get(name,'#ddd9d0'))
                else:
                    rect(x,y,x+1,y+.5,COL.get(name,'#ddd9d0'))
                    if facing in ('east','west'):
                        a,b=(x+.5,x+1) if facing=='east' else (x,x+.5)
                        rect(a,y+.5,b,y+1,COL.get(name,'#ddd9d0'))
                    else:rect(x,y+.5,x+1,y+1,COL.get(name,'#ddd9d0'))
        # Exact transverse stair shape is in CSV; this section shows projected cube half extents.
        X,Y=55+(x0-minx)*scale,85+(maxy-y0)*scale
    else:
        minx,maxx=-26,26;minz,maxz=-26,26;scale=9
        for name,blocks in [('base',base),*groups.items()]:
            for x,y,z in sorted(blocks):
                if y!=y0:continue
                content.append(f'<rect x="{55+(x-minx)*scale}" y="{85+(z-minz)*scale}" width="9" height="9" fill="{COL.get(name,"#ddd9d0")}" stroke="#fff" stroke-width=".3"/>')
        X,Y=55+(x0-minx)*scale,85+(z0-minz)*scale
    content.append(f'<rect x="{X:.2f}" y="{Y:.2f}" width="{scale}" height="{scale}" fill="none" stroke="#ff00b7" stroke-width="2.5"/>')
    content.append('<text x="20" y="600" font-family="sans-serif" font-size="12">gold stair; brown pillar; blue balcony; red rail; grey-blue arch; teal canopy; purple gate.</text>')
    content.append('<text x="20" y="623" font-family="sans-serif" font-size="12">Coordinate projection, not in-game acceptance. Non-XY-facing stair halves are projected; audit uses full 3D boxes.</text></svg>')
    out.write_text('\n'.join(content)+'\n')


def render(out):
    rep=json.loads((out/'reproduction.json').read_text());r=rep['cases']['C1']
    groups,claims,objects,base=v.read(out/'original','C1')
    targets={'pillar':r['pillar_first'],'overhead':[r['overhead_first']['tread'][0],r['overhead_first']['lowest_overhead_y'],r['overhead_first']['tread'][2]],
             'balcony':r['balcony_first']['blocked_air'][0]}
    for name,p in targets.items():
        for section in (False,True):
            draw(out/f'failure_{name}_{"section" if section else "plan"}.svg',groups,base,
                 f'GEOMETRY-03 original failure: {name}',p,section)
    envelopes={}
    for conv in ('C1','C2'):
        groups,claims,obj,base=v.read(out/'best',conv)
        normal=set().union(*(set(c) for name,c in groups.items() if not name.startswith('balcony')))
        balcony=set().union(*(set(c) for name,c in groups.items() if name.startswith('balcony')))
        def bounds(cells):
            return {'x':[min(x for x,y,z in cells),max(x for x,y,z in cells)],
                    'y':[min(y for x,y,z in cells),max(y for x,y,z in cells)],
                    'z':[min(z for x,y,z in cells),max(z for x,y,z in cells)],
                    'far_corner_diameter':2*max(math.hypot(x+dx-.5,z+dz-.5) for x,y,z in cells for dx,dz in ((0,0),(0,1),(1,0),(1,1)))}
        envelopes[conv]={'normal':bounds(normal),'balcony':bounds(balcony),
                         'outside_original_disc45_columns':claims['outside_original_disc45_columns']}
    (out/'envelope.json').write_text(json.dumps(envelopes,sort_keys=True,separators=(',',':'))+'\n')

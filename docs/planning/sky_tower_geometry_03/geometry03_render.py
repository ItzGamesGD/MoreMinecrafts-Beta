"""SVGs are projections of exported voxel sets; no hand-drawn layout geometry."""
from collections import defaultdict


COLORS = {'stairs':'#d6a34e','bridge':'#b69b70','pillar':'#65594e','arch':'#858f9e',
          'rail':'#b65d50','balcony_floor':'#72a7ce','balcony_rail':'#a35146',
          'balcony_support':'#9cadb8','canopy':'#3e8884','gateway_frame':'#a68bb7'}


def render(model, out):
    conv = model['convention']
    for i,floor in enumerate(model['floors']):
        upper = model['floors'][i+1]-1 if i<4 else 79
        svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="800" height="850" viewBox="0 0 800 850">',
               '<rect width="800" height="850" fill="#faf9f5"/>',
               f'<text x="20" y="25" font-family="sans-serif" font-size="16">{conv} F{i+1 if i<4 else "SANCTUM"} occupied voxel projection y{floor}..{upper}</text>',
               '<text x="20" y="48" font-family="sans-serif" font-size="12">Candidate has measured failures; projection is NOT an approved layout. +X right, +Z down.</text>']
        for name,cells in [('baseline',set(model['baseline'])-model['void']),*model['groups'].items()]:
            cols = {(x,z) for x,y,z in cells if floor<=y<=upper}
            for x,z in sorted(cols):
                svg.append(f'<rect x="{400+x*13}" y="{405+z*13}" width="13" height="13" fill="{COLORS.get(name,"#dedbd3")}" stroke="#fff" stroke-width=".25"/>')
        for name,data in model['reservations'].items():
            if name == 'F'+str(i+1):
                for x,y,z in data['opening']:
                    if y==floor+1:
                        svg.append(f'<rect x="{400+x*13}" y="{405+z*13}" width="13" height="13" fill="none" stroke="#9c29ae"/>')
        svg.append('<text x="20" y="796" font-family="sans-serif" font-size="12">gold stairs; brown pillars; grey arches; red rails; blue balcony; teal canopy; purple gateway.</text>')
        svg.append('<text x="20" y="817" font-family="sans-serif" font-size="12">Voxel occupancy envelopes; exact full/bottom-stair collision states are in blocks CSV.</text></svg>')
        (out/f'plan_{conv}_{"F"+str(i+1) if i<4 else "SANCTUM"}.svg').write_text('\n'.join(svg)+'\n')
    # Wireframes: horizontal boundaries at actual selected voxel planes, plus vertical runs.
    # This is a sparse view of occupied cube envelopes, not a full-block substitute in BFS.
    def project(x,y,z):
        return 420+(x-z)*7,790-y*8+(x+z)*2
    svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="900" height="940">',
           '<rect width="900" height="940" fill="#faf9f5"/>',
           f'<text x="15" y="25" font-family="sans-serif" font-size="17">{conv} coordinate-derived sparse occupancy wireframe</text>',
           '<text x="15" y="46" font-family="sans-serif" font-size="12">Selected real y-plane outlines and column runs. Stair cube envelopes shown; BFS uses two boxes.</text>']
    planes = set(model['floors']) | {y for name in ('canopy','arch') for x,y,z in model['groups'][name]}
    for name,cells in model['groups'].items():
        layers = defaultdict(set)
        columns = defaultdict(set)
        for x,y,z in cells:
            layers[y].add((x,z));columns[x,z].add(y)
        lines = []
        def line(a,b):
            X,Y=project(*a);U,V=project(*b)
            lines.append(f'M{X:.0f},{Y:.0f}L{U:.0f},{V:.0f}')
        for y,cols in sorted(layers.items()):
            if y not in planes:
                continue
            for x,z in sorted(cols):
                for dx,dz,a,b in [(1,0,(x+1,y,z),(x+1,y,z+1)),(-1,0,(x,y,z),(x,y,z+1)),
                                  (0,1,(x,y,z+1),(x+1,y,z+1)),(0,-1,(x,y,z),(x+1,y,z))]:
                    if (x+dx,z+dz) not in cols:
                        line(a,b)
        if name in ('pillar','gateway_frame','balcony_support'):
            for (x,z),ys in sorted(columns.items()):
                # Vertical runs are actual consecutive cells, never spanning an empty gap.
                start=prev=None
                for y in sorted(ys)+[max(ys)+2]:
                    if prev is not None and y!=prev+1:
                        line((x,start,z),(x,prev+1,z));start=None
                    if start is None:start=y
                    prev=y
        svg.append(f'<path d="{" ".join(lines)}" fill="none" stroke="{COLORS[name]}" stroke-width=".65"/>')
    svg.append('<text x="15" y="914" font-family="sans-serif" font-size="12">Geometry-03 exploration only. See report for obstructions, clearances and unverified conditions.</text></svg>')
    (out/f'wireframe_{conv}.svg').write_text('\n'.join(svg)+'\n')

#!/usr/bin/env python3
"""P2 visualisation from EXPORTED block CSVs only (stdlib). usage: p2_render.py candidate_dir out_dir label"""
import csv, math, sys
from pathlib import Path
COL = {'stairs': '#e0ad57', 'pillar': '#5a4f48', 'rail': '#b66c57', 'arcade_arch': '#4f8f8a', 'balcony_floor': '#6aa3d6',
       'balcony_rail': '#d98a8a', 'balcony_support': '#8ca0ab'}
ORDER = ['balcony_support', 'balcony_floor', 'rail', 'balcony_rail', 'stairs', 'arcade_arch', 'pillar']


def load(d):
    g = {}
    for r in csv.DictReader((Path(d) / 'blocks_C1.csv').open()):
        g.setdefault(r['group'], set()).add((int(r['x']), int(r['y']), int(r['z'])))
    return g


def circle_cells(D, r=30): return {(x, z) for x in range(-r, r + 1) for z in range(-r, r + 1) if 4 * (x * x + z * z) < D * D}


def hdr(w, h, title): return [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" font-family="monospace" font-size="11">', f'<rect width="{w}" height="{h}" fill="#faf9f5"/>', f'<text x="8" y="14">{title}</text>']


def outline(cells, px, ox, oy, color, sw=1.2, proj=lambda x, z: (x, z)):
    d = []
    for x, z in cells:
        for (dx, dz), (a, b, c, e) in {(1, 0): (x + 1, z, x + 1, z + 1), (-1, 0): (x, z, x, z + 1), (0, 1): (x, z + 1, x + 1, z + 1), (0, -1): (x, z, x + 1, z)}.items():
            if (x + dx, z + dz) not in cells: d.append(f'M{ox + (a + 30) * px} {oy + (b + 30) * px}L{ox + (c + 30) * px} {oy + (e + 30) * px}')
    return f'<path d="{" ".join(d)}" stroke="{color}" stroke-width="{sw}" fill="none"/>'


def plan(g, out, title, ylo=None, yhi=None, px=11):
    W = 61 * px + 20
    s = hdr(W, W + 40, title); ox, oy = 10, 24
    top = {}
    for grp in ORDER:
        for x, y, z in g.get(grp, ()):
            if (ylo is None or ylo <= y <= yhi): top[(x, z)] = (grp, y)
    for (x, z), (grp, y) in top.items():
        s.append(f'<rect x="{ox + (x + 30) * px}" y="{oy + (z + 30) * px}" width="{px}" height="{px}" fill="{COL[grp]}" stroke="#fff" stroke-width=".3"/>')
    s.append(outline(circle_cells(45), px, ox, oy, '#202020')); s.append(outline(circle_cells(51), px, ox, oy, '#2a7fbf', .8))
    s.append(outline(circle_cells(31), px, ox, oy, '#888', .8))
    s.append(f'<text x="8" y="{W + 24}">black = D45 (centre rule), blue = D51 (balcony-only), grey = wall disc D31. +x right, +z down. colours: ' + ', '.join(COL) + '</text></svg>')
    Path(out).write_text('\n'.join(s) + '\n')


def section(g, out, title, axis, px=7):
    ymin = min(y for v in g.values() for _, y, _ in v) - 1; ymax = max(y for v in g.values() for _, y, _ in v) + 2
    W, H = 61 * px + 20, (ymax - ymin + 1) * px + 50; s = hdr(W, H, title)
    for grp in ORDER:
        for x, y, z in g.get(grp, ()):
            if (axis == 'XY' and z in (0, 1)) or (axis == 'ZY' and x in (0, 1)):
                u = x if axis == 'XY' else z
                s.append(f'<rect x="{10 + (u + 30) * px}" y="{24 + (ymax - y) * px}" width="{px}" height="{px}" fill="{COL[grp]}"/>')
    s.append(f'<text x="8" y="{H - 8}">{"x–y slice z∈{0,1}" if axis == "XY" else "z–y slice x∈{0,1}"}; floors y=10,21,33,47,64 (C1)</text></svg>')
    Path(out).write_text('\n'.join(s) + '\n')


def iso(g, out, title):
    k = 6.0
    P = lambda x, y, z: (380 + (x - z) * k * .866, 560 - y * k * 0.9 + (x + z) * k * .25)
    s = hdr(760, 640, title)
    for grp in ORDER:
        if grp == 'balcony_support': continue
        for x, y, z in sorted(g.get(grp, ()), key=lambda c: (c[0] + c[2], c[1])):
            X, Y = P(x + .5, y + .5, z + .5)
            s.append(f'<circle cx="{X:.1f}" cy="{Y:.1f}" r="{k * .55:.1f}" fill="{COL[grp]}" fill-opacity=".85"/>')
    s.append('<text x="8" y="630">isometric block-centre projection of exported blocks (not a render of game visuals)</text></svg>')
    Path(out).write_text('\n'.join(s) + '\n')


if __name__ == '__main__':
    d, o, lab = sys.argv[1:4]; Path(o).mkdir(parents=True, exist_ok=True); g = load(d)
    plan(g, f'{o}/{lab}_plan_all_layers.svg', f'{lab}: top view, all layers (top member per column)')
    for name, a, b in (('F1', 10, 20), ('F2', 21, 32), ('F3', 33, 46), ('F4', 47, 63), ('SANCTUM', 64, 79)):
        plan(g, f'{o}/{lab}_plan_{name}.svg', f'{lab}: y {a}-{b}', a, b)
    section(g, f'{o}/{lab}_section_XY.svg', f'{lab}: X/Y section', 'XY'); section(g, f'{o}/{lab}_section_ZY.svg', f'{lab}: Z/Y section', 'ZY')
    iso(g, f'{o}/{lab}_iso.svg', f'{lab}: 3D projection')

#!/usr/bin/env python3
"""SKY-TOWER-GEOMETRY-02 -- base-structure coordinate model and geometric verification.

Isolated verification tool (stdlib only). It is NOT structure-generation code: it writes no NBT,
touches no Java/worldgen, and is not imported by anything in the build.

Usage:  python3 geometry02_model.py [--out DIR]
Writes: data/*.json, data/*.csv, svg/*.svg, results.json and prints a PASS/CONFLICT/UNVERIFIED summary.

Design inputs (docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md @ 1449bf1):
  Q26 base-structure max outer diameter 45 (stairs, railing, eaves included)
  Q79 airborne balconies only: max outer diameter 51
  Q42 combat-room inner diameter "about 26" (target before coordinate verification)
  Q43 stair walking width 4 (railing / pillars excluded)
  Q98 stair arcade pillars 3x3
  Q27 total height 80 from stone-base bottom to sanctum arch apex; island thickness excluded
  Q28 10 / 11 / 12 / 14 / 17 / 16 = 80, inter-floor thickness included
  Q41 sanctum roofless
Wall thickness and slab thickness are NOT defined by the design; they are enumerated as candidates.
"""
import argparse
import json
import math
import os
from collections import deque

# ---------------------------------------------------------------------------------------------
# Coordinate system
#   Block (x, y, z) is the unit cube [x, x+1) x [y, y+1) x [z, z+1) in local model space.
#   y = 0 is the lowest block layer of the stone base (Q27 "석조 기단 바닥"); y = 79 is the
#   highest permitted layer (sanctum arch apex).  Natural island blocks are below y = 0.
#   BLOCK-centred ("B") layouts: tower axis passes through the centre of column (0, 0), i.e. the
#     point (0.5, 0.5); a column's radial distance is hypot(x, z).
#   CORNER-centred ("C") layouts: axis passes through the shared corner of columns (-1..0)^2,
#     i.e. the point (0, 0); a column's radial distance is hypot(x + 0.5, z + 0.5).
#   A disc of diameter D (blocks along X or Z) is {column : dist < D / 2}.  D must be odd for B
#   and even for C, so that the X and Z extents are exactly D.
#   Reference axis (Q61 main entrance) is +Z ("south").  The base model is D4-symmetric, so the
#   choice does not change any block; it only labels sections.
# ---------------------------------------------------------------------------------------------
ENV_D = {"B": 45, "C": 44}       # Q26: <= 45.  Corner-centred discs must be even -> 44.
BALC_D = {"B": 51, "C": 50}      # Q79: <= 51 (balconies only; not used by this model).
SEGMENTS = [("BASE", 10), ("F1", 11), ("F2", 12), ("F3", 14), ("F4", 17), ("SANCTUM", 16)]
TOTAL_H = 80
STAIR_WALK = 4                   # Q43
PILLAR = 3                       # Q98
RESERVE_REQ = {                  # minimum radial band outside the wall (GEOMETRY-03 elements)
    "walk4+rail1": STAIR_WALK + 1,
    "walk4+pillar3": STAIR_WALK + PILLAR,
}


def dist(c, x, z):
    return math.hypot(x, z) if c == "B" else math.hypot(x + 0.5, z + 0.5)


def disc(c, d):
    r = d / 2.0
    n = int(math.ceil(r)) + 1
    return {(x, z) for x in range(-n, n + 1) for z in range(-n, n + 1) if dist(c, x, z) < r}


def extent(cells):
    xs = [x for x, _ in cells]
    zs = [z for _, z in cells]
    return min(xs), max(xs), min(zs), max(zs), max(xs) - min(xs) + 1, max(zs) - min(zs) + 1


def far_corner_diameter(c, cells):
    """Largest distance from the axis to any cube corner, doubled (diagonal-growth check)."""
    ox, oz = (0.5, 0.5) if c == "B" else (0.0, 0.0)
    best = 0.0
    for x, z in cells:
        for dx in (0, 1):
            for dz in (0, 1):
                best = max(best, math.hypot(x + dx - ox, z + dz - oz))
    return 2 * best


def symmetric(c, cells):
    s = set(cells)
    if c == "B":
        maps = [lambda x, z: (-x, z), lambda x, z: (x, -z), lambda x, z: (z, x), lambda x, z: (-z, x)]
    else:
        maps = [lambda x, z: (-1 - x, z), lambda x, z: (x, -1 - z), lambda x, z: (z, x), lambda x, z: (-1 - z, x)]
    return {name: all(m(x, z) in s for x, z in s) for name, m in zip(("mirrorX", "mirrorZ", "diag", "rot90"), maps)}


def ray_profile(c, inner, wall, rays=3600, step=0.01, rmax=30.0):
    """Independent method: march rays from the axis through continuous space."""
    ox, oz = (0.5, 0.5) if c == "B" else (0.0, 0.0)
    rin, thick = [], []
    for i in range(rays):
        a = 2 * math.pi * i / rays
        ca, sa = math.cos(a), math.sin(a)
        t, first_wall, first_out = 0.0, None, None
        while t < rmax:
            px, pz = ox + t * ca, oz + t * sa
            cell = (math.floor(px), math.floor(pz))
            if first_wall is None and cell in wall:
                first_wall = t
            if first_wall is not None and cell not in wall and cell not in inner:
                first_out = t
                break
            if first_wall is None and cell not in inner and cell not in wall:
                break  # leak: ray left the interior without crossing wall
            t += step
        rin.append(first_wall)
        thick.append(None if first_wall is None or first_out is None else first_out - first_wall)
    return rin, thick


def closed_4conn(inner, wall, box):
    """Flood-fill (face adjacency) from the interior over non-wall columns; must not escape."""
    lo, hi = -box, box
    start = next(iter(inner))
    seen, q = {start}, deque([start])
    while q:
        x, z = q.popleft()
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if (nx, nz) in seen or (nx, nz) in wall:
                continue
            if not (lo <= nx <= hi and lo <= nz <= hi):
                return False, len(seen)
            seen.add((nx, nz))
            q.append((nx, nz))
    return True, len(seen)


def corner_joints(inner, wall):
    """Corner-only seams: an interior and an outside column touching only at a corner while the two
    columns sharing that corner are wall.  Not walkable (no face gap) but a visible pinhole."""
    n = 0
    for x, z in inner:
        for dx, dz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
            o = (x + dx, z + dz)
            if o not in inner and o not in wall and (x + dx, z) in wall and (x, z + dz) in wall:
                n += 1
    return n


def components_8(cells):
    cells, comps = set(cells), 0
    while cells:
        comps += 1
        q = deque([cells.pop()])
        while q:
            x, z = q.popleft()
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    n = (x + dx, z + dz)
                    if n in cells:
                        cells.remove(n)
                        q.append(n)
    return comps


def octant_steps(c, cells):
    """Outer boundary profile in the first octant: for each x, max z (grid step pattern)."""
    prof = {}
    for x, z in cells:
        if x >= 0 and z >= 0:
            prof[x] = max(prof.get(x, -99), z)
    xs = sorted(prof)
    out = [(x, prof[x]) for x in xs if prof[x] >= x] if c == "B" else [(x, prof[x]) for x in xs if prof[x] >= x]
    runs, prev, n = [], None, 0
    for x, z in out:
        if z == prev:
            n += 1
        else:
            if prev is not None:
                runs.append(n)
            prev, n = z, 1
    runs.append(n)
    return out, runs


# ---------------------------------------------------------------------------------------------
# Vertical model
# ---------------------------------------------------------------------------------------------
def segments():
    out, y = [], 0
    for name, h in SEGMENTS:
        out.append({"segment": name, "budget_h": h, "y_start": y, "y_end": y + h - 1})
        y += h
    return out


def vertical_table(convention, s):
    """convention C1: slab at the bottom of each segment (owned by the room above it).
       convention C2: slab at the top of each segment (owned by the room below it).
       s: slab thickness in blocks (undefined by design -> candidate)."""
    rows = []
    for seg in segments():
        a, b, name = seg["y_start"], seg["y_end"], seg["segment"]
        r = dict(seg)
        if name == "BASE":
            if convention == "C1":
                r.update(slab=None, base_mass=[a, b], clear=None, walk_surface_top=b,
                         note="podium mass y%d-%d; F1 floor slab lies in F1" % (a, b))
            else:
                r.update(slab=[b - s + 1, b], base_mass=[a, b], clear=None, walk_surface_top=b,
                         note="podium mass y%d-%d; its top %d layer(s) are the F1 floor" % (a, b, s))
        elif name == "SANCTUM":
            if convention == "C1":
                r.update(slab=[a, a + s - 1], clear=[a + s, b], clear_h=b - (a + s) + 1,
                         arch_zone=[a + s, b], apex_y=b, roof=None)
            else:
                r.update(slab=None, clear=[a, b], clear_h=b - a + 1, arch_zone=[a, b], apex_y=b, roof=None,
                         note="floor is F4's top slab")
        else:
            if convention == "C1":
                r.update(slab=[a, a + s - 1], clear=[a + s, b], clear_h=b - (a + s) + 1,
                         ceiling="next segment floor slab")
            else:
                r.update(slab=[b - s + 1, b], clear=[a, b - s], clear_h=(b - s) - a + 1,
                         floor="previous segment top slab / base top")
        rows.append(r)
    return rows


def build_3d(c, inner, wall, convention, s):
    """Occupancy model: {(x,y,z): zone}.  Only base-structure elements of this step."""
    full = inner | wall
    occ = {}
    rows = vertical_table(convention, s)
    for r in rows:
        a, b, name = r["y_start"], r["y_end"], r["segment"]
        if name == "BASE":
            for y in range(a, b + 1):
                for x, z in full:
                    occ[x, y, z] = "BASE_CORE"
            continue
        if r.get("slab"):
            for y in range(r["slab"][0], r["slab"][1] + 1):
                for x, z in full:
                    occ[x, y, z] = "SLAB"
        if name != "SANCTUM":
            for y in range(r["clear"][0], r["clear"][1] + 1):
                for x, z in wall:
                    occ[x, y, z] = "WALL"
        # SANCTUM above its floor: nothing occupied in this step (arches = reserved, roofless)
    return occ, rows


# ---------------------------------------------------------------------------------------------
# Forward check for GEOMETRY-03 (input only, not a verdict of this step)
# ---------------------------------------------------------------------------------------------
def square_fits(c, inner, keep_out, size):
    h = size // 2
    hits = []
    span = range(-25, 26)
    for px in span:
        for pz in span:
            cells = [(px + dx, pz + dz) for dx in range(-h, h + 1) for dz in range(-h, h + 1)]
            if all(cl in inner for cl in cells) and not any(cl in keep_out for cl in cells):
                hits.append((px, pz))
    return hits


# ---------------------------------------------------------------------------------------------
# SVG output (generated strictly from the coordinate sets)
# ---------------------------------------------------------------------------------------------
COL = {"INNER": "#f3ead2", "WALL": "#5b5f66", "RESERVE": "#9fd3c7", "BALC": "#e6f4f1", "OUT": "#ffffff",
       "SLAB": "#8a7b62", "BASE_CORE": "#b8a98a", "ARCH_RES": "#f2b8a0", "AXIS": "#c0392b"}


def _runs(out, grid, ox, oy, px):
    """grid: list of rows (top to bottom) of colours; emit merged horizontal runs, skip white."""
    for j, row in enumerate(grid):
        i = 0
        while i < len(row):
            k = i
            while k < len(row) and row[k] == row[i]:
                k += 1
            if row[i] != "#ffffff":
                out.append('<rect x="%d" y="%d" width="%d" height="%d" fill="%s"/>'
                           % (ox + i * px, oy + j * px, (k - i) * px, px, row[i]))
            i = k
    cols, rows = len(grid[0]), len(grid)
    out.append('<path d="%s" stroke="#00000022" stroke-width="0.5" fill="none"/>' % " ".join(
        ["M%d %dV%d" % (ox + i * px, oy, oy + rows * px) for i in range(cols + 1)] +
        ["M%d %dH%d" % (ox, oy + j * px, ox + cols * px) for j in range(rows + 1)]))


def svg_plan(path, title, c, layers, n=26, px=14):
    w = (2 * n + 1) * px
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" font-family="monospace" font-size="11">'
           % (w + 20, w + 60), '<rect width="100%" height="100%" fill="white"/>',
           '<text x="10" y="16">%s</text>' % title]
    oy = 30
    grid = []
    for z in range(-n, n + 1):
        row = []
        for x in range(-n, n + 1):
            zone = "OUT"
            for name, cells in layers:
                if (x, z) in cells:
                    zone = name
                    break
            row.append(COL[zone])
        grid.append(row)
    _runs(out, grid, 10, oy, px)
    # axis marks
    ax = 10 + (n + (0.5 if c == "B" else 0)) * px
    ay = oy + (n + (0.5 if c == "B" else 0)) * px
    out.append('<line x1="%g" y1="%d" x2="%g" y2="%d" stroke="%s" stroke-width="1"/>' % (ax, oy, ax, oy + w, COL["AXIS"]))
    out.append('<line x1="10" y1="%g" x2="%d" y2="%g" stroke="%s" stroke-width="1"/>' % (ay, 10 + w, ay, COL["AXIS"]))
    out.append('<text x="10" y="%d">+X right, +Z down (+Z = entrance reference axis). 1 square = 1 block column. '
               'axis = red cross.</text>' % (oy + w + 16))
    out.append('</svg>')
    open(path, "w").write("\n".join(out))


def svg_section(path, title, occ, axis, c, inner, n=26, px=8):
    """axis 'XY': slice z = 0 ; axis 'ZY': slice x = 0."""
    h = TOTAL_H + 4
    W = (2 * n + 1) * px
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" font-family="monospace" font-size="11">'
           % (W + 170, h * px + 60), '<rect width="100%" height="100%" fill="white"/>',
           '<text x="10" y="16">%s</text>' % title]
    oy = 30
    lo, hi = (-(ENV_D[c] // 2), ENV_D[c] // 2) if c == "B" else (-(ENV_D[c] // 2), ENV_D[c] // 2 - 1)
    grid = []
    for y in range(h - 2, -2, -1):
        row = []
        for u in range(-n, n + 1):
            key = (u, y, 0) if axis == "XY" else (0, y, u)
            zone = occ.get(key)
            col = (u, 0) if axis == "XY" else (0, u)
            if zone:
                row.append(COL[zone])
            elif col in inner and 0 <= y < TOTAL_H:
                row.append(COL["INNER"])
            else:
                row.append("#e9e9e9" if lo <= u <= hi and 0 <= y < TOTAL_H else "#ffffff")
        grid.append(row)
    _runs(out, grid, 10, oy, px)
    for seg in segments():
        Y = oy + (h - 2 - seg["y_start"]) * px + px
        out.append('<line x1="10" y1="%d" x2="%d" y2="%d" stroke="#c0392b" stroke-width="0.8"/>' % (Y, 10 + W, Y))
        out.append('<text x="%d" y="%d">%s y%d</text>' % (14 + W, Y - 2, seg["segment"], seg["y_start"]))
    Y = oy + (h - 2 - (TOTAL_H - 1)) * px
    out.append('<line x1="10" y1="%d" x2="%d" y2="%d" stroke="#000" stroke-dasharray="4 2"/>' % (Y, 10 + W, Y))
    out.append('<text x="%d" y="%d">y79 top / apex</text>' % (14 + W, Y - 2))
    out.append('<text x="10" y="%d">cream = interior air column, grey = 45-block envelope outside the wall (reserved, empty); horizontal: %s</text>'
               % (oy + h * px + 14, "X (z=0 slice)" if axis == "XY" else "Z (x=0 slice)"))
    out.append('</svg>')
    open(path, "w").write("\n".join(out))


def boundary_edges(cells):
    """Unit edges on the boundary of a column set (actual grid outline, not an analytic circle)."""
    e = []
    for x, z in cells:
        if (x + 1, z) not in cells:
            e.append(((x + 1, z), (x + 1, z + 1)))
        if (x - 1, z) not in cells:
            e.append(((x, z), (x, z + 1)))
        if (x, z + 1) not in cells:
            e.append(((x, z + 1), (x + 1, z + 1)))
        if (x, z - 1) not in cells:
            e.append(((x, z), (x + 1, z)))
    return e


def svg_wireframe(path, title, c, inner, full, env, rows):
    """Isometric wireframe built from the grid outlines of the coordinate sets."""
    def iso(x, y, z, k=7.0):
        return 380 + (x - z) * k * 0.866, 660 - y * k + (x + z) * k * 0.25

    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="760" height="760" font-family="monospace" font-size="11">',
           '<rect width="100%" height="100%" fill="white"/>', '<text x="10" y="16">%s</text>' % title]

    def outline(cells, y, color, width=0.8):
        d = []
        for (x0, z0), (x1, z1) in boundary_edges(cells):
            X0, Y0 = iso(x0, y, z0)
            X1, Y1 = iso(x1, y, z1)
            d.append("M%.1f %.1fL%.1f %.1f" % (X0, Y0, X1, Y1))
        out.append('<path d="%s" stroke="%s" stroke-width="%g" fill="none"/>' % (" ".join(d), color, width))

    outline(env, 0, "#2a9d8f", 0.6)
    for r in rows:
        outline(full, r["y_start"], "#333")
        outline(inner, r["y_start"], "#aaa", 0.6)
    outline(full, TOTAL_H, "#c0392b")
    # vertical edges at the 4 axis extremes and 4 diagonal extremes of the wall outline
    ext = extent(full)
    picks = [(ext[1] + 1, 0), (ext[0], 0), (0, ext[3] + 1), (0, ext[2])]
    for x, z in picks:
        X0, Y0 = iso(x, 0, z)
        X1, Y1 = iso(x, TOTAL_H, z)
        out.append('<line x1="%.1f" y1="%.1f" x2="%.1f" y2="%.1f" stroke="#888" stroke-width="0.6"/>' % (X0, Y0, X1, Y1))
    out.append('<text x="10" y="740">black: wall outer outline at each segment start y; grey: inner face; '
               'teal: 45 envelope at y0; red: y80 top plane</text>')
    out.append('</svg>')
    open(path, "w").write("\n".join(out))


# ---------------------------------------------------------------------------------------------
def candidates():
    out = []
    for c, din in (("B", 25), ("B", 27), ("C", 26)):
        for t in (1, 2, 3, 4):
            dw = din + 2 * t
            out.append({"id": "%s%d-T%d" % (c, din, t), "center": c, "inner_d": din, "wall_t": t,
                        "wall_outer_d": dw, "reserve_band": (ENV_D[c] - dw) // 2})
    return out


def analyse(cand, ray=True):
    c, din, dw = cand["center"], cand["inner_d"], cand["wall_outer_d"]
    inner = disc(c, din)
    full = disc(c, dw)
    wall = full - inner
    env = disc(c, ENV_D[c])
    reserve = env - full
    res = dict(cand)
    res["cells"] = {"interior": len(inner), "wall_per_layer": len(wall), "slab_per_layer": len(full),
                    "reserve_band": len(reserve), "envelope45": len(env)}
    res["extent_wall_outer"] = extent(full)[4:]
    res["extent_inner"] = extent(inner)[4:]
    res["extent_envelope"] = extent(env)[4:]
    res["far_corner_diameter_wall"] = round(far_corner_diameter(c, full), 3)
    res["far_corner_diameter_env"] = round(far_corner_diameter(c, env), 3)
    res["row_section"] = {
        "inner_cells_on_axis_row": sum(1 for x, z in inner if z == 0),
        "wall_cells_on_axis_row_plus": sum(1 for x, z in wall if z == 0 and x > 0),
        "wall_cells_on_axis_row_minus": sum(1 for x, z in wall if z == 0 and x < 0),
        "inner_cells_on_main_diagonal": sum(1 for x, z in inner if x == z),
        "wall_cells_on_main_diagonal_plus": sum(1 for x, z in wall if x == z and x > 0),
    }
    res["symmetry_wall"] = symmetric(c, wall)
    res["symmetry_inner"] = symmetric(c, inner)
    closed, n_air = closed_4conn(inner, wall, 30)
    res["closed_face_adjacency"] = closed
    res["wall_components_8conn"] = components_8(wall)
    res["corner_only_joints"] = corner_joints(inner, wall)
    res["inner_contiguous"] = components_8(inner) == 1
    prof, runs = octant_steps(c, full)
    res["outer_octant_profile"] = prof
    res["outer_octant_runs"] = runs
    if ray:
        rin, thick = ray_profile(c, inner, wall)
        valid_t = [t for t in thick if t is not None]
        res["ray"] = {
            "rays": len(rin), "leaks": sum(1 for r in rin if r is None),
            "inner_face_radius_min": round(min(r for r in rin if r is not None), 3),
            "inner_face_radius_max": round(max(r for r in rin if r is not None), 3),
            "wall_thickness_min": round(min(valid_t), 3), "wall_thickness_max": round(max(valid_t), 3),
        }
    res["fits"] = {k: res["reserve_band"] >= v for k, v in RESERVE_REQ.items()}
    return res, inner, wall, full, env, reserve


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.dirname(os.path.abspath(__file__)))
    args = ap.parse_args()
    data_dir, svg_dir = os.path.join(args.out, "data"), os.path.join(args.out, "svg")
    os.makedirs(data_dir, exist_ok=True)
    os.makedirs(svg_dir, exist_ok=True)

    results = {"segments": segments(), "candidates": [], "vertical": {}, "checks": [], "forward_geometry03": {}}

    # ---- vertical (independent methods) ----
    segs = segments()
    sum_h = sum(s["budget_h"] for s in segs)
    span = segs[-1]["y_end"] - segs[0]["y_start"] + 1
    cover = [sum(1 for s in segs if s["y_start"] <= y <= s["y_end"]) for y in range(TOTAL_H)]
    contiguous = all(segs[i + 1]["y_start"] == segs[i]["y_end"] + 1 for i in range(len(segs) - 1))
    results["vertical"]["sum_of_budgets"] = sum_h
    results["vertical"]["span_from_coordinates"] = span
    results["vertical"]["every_layer_exactly_one_segment"] = all(v == 1 for v in cover)
    results["vertical"]["contiguous"] = contiguous
    for conv in ("C1", "C2"):
        for s in (1, 2):
            results["vertical"]["%s_s%d" % (conv, s)] = vertical_table(conv, s)

    # ---- plan candidates ----
    primary = {"B27-T2", "B25-T3", "C26-T2"}
    for cand in candidates():
        res, inner, wall, full, env, reserve = analyse(cand, ray=True)
        results["candidates"].append(res)
        with open(os.path.join(data_dir, "plan_%s.json" % cand["id"]), "w") as f:
            json.dump({"id": cand["id"], "center": cand["center"],
                       "axis_point": [0.5, 0.5] if cand["center"] == "B" else [0.0, 0.0],
                       "inner": sorted(inner), "wall": sorted(wall), "reserve_band": sorted(reserve)}, f)
        if cand["id"] in primary:
            with open(os.path.join(data_dir, "wall_%s.csv" % cand["id"]), "w") as f:
                f.write("x,z,radial_dist\n")
                for x, z in sorted(wall):
                    f.write("%d,%d,%.4f\n" % (x, z, dist(cand["center"], x, z)))
            svg_plan(os.path.join(svg_dir, "plan_%s.svg" % cand["id"]),
                     "%s plan (all combat floors, wall layer): inner D%d / wall t%d / outer D%d / reserve %d"
                     % (cand["id"], cand["inner_d"], cand["wall_t"], cand["wall_outer_d"], res["reserve_band"]),
                     cand["center"], [("INNER", inner), ("WALL", wall), ("RESERVE", reserve),
                                      ("BALC", disc(cand["center"], BALC_D[cand["center"]]))])
            # top-down sanctum view: floor slab + reserved arch band + 12 directions
            svg_plan(os.path.join(svg_dir, "top_%s.svg" % cand["id"]),
                     "%s top-down (sanctum floor slab, roofless; arch band reserved, not placed)" % cand["id"],
                     cand["center"], [("SLAB", inner), ("ARCH_RES", wall), ("RESERVE", reserve),
                                      ("BALC", disc(cand["center"], BALC_D[cand["center"]]))])
            for conv in ("C1",):
                occ, rows = build_3d(cand["center"], inner, wall, conv, 1)
                # vertical alignment check: wall columns identical on every wall layer
                layers = {}
                for (x, y, z), zone in occ.items():
                    if zone == "WALL":
                        layers.setdefault(y, set()).add((x, z))
                aligned = all(v == wall for v in layers.values())
                over = [k for k in occ if (k[0], k[2]) not in full]
                above = [k for k in occ if k[1] >= TOTAL_H or k[1] < 0]
                sanct = next(r for r in rows if r["segment"] == "SANCTUM")
                roof = [k for k in occ if k[1] > sanct["slab"][1]]
                res["model3d_%s_s1" % conv] = {
                    "voxels": len(occ), "wall_layers": sorted(layers), "wall_vertically_aligned": aligned,
                    "voxels_outside_wall_outer_disc": len(over), "voxels_outside_y0_y79": len(above),
                    "voxels_above_sanctum_floor": len(roof),
                    "zone_counts": {zz: sum(1 for v in occ.values() if v == zz) for zz in set(occ.values())},
                }
                with open(os.path.join(data_dir, "model3d_%s_%s_s1.json" % (cand["id"], conv)), "w") as f:
                    json.dump({"id": cand["id"], "convention": conv, "slab_t": 1,
                               "layers": [{"y": y, "zone": z} for y, z in sorted({(k[1], v) for k, v in occ.items()})],
                               "zone_cells": {"BASE_CORE": "inner+wall", "SLAB": "inner+wall", "WALL": "wall"},
                               "note": "expand: each layer's zone uses the 2-D column set named in zone_cells "
                                       "from plan_%s.json" % cand["id"]}, f, indent=0)
                for ax in ("XY", "ZY"):
                    svg_section(os.path.join(svg_dir, "section_%s_%s_%s.svg" % (ax, cand["id"], conv)),
                                "%s %s section, convention %s, slab 1 (candidate, not final)" % (cand["id"], ax, conv),
                                occ, ax, cand["center"], inner)
                svg_wireframe(os.path.join(svg_dir, "wireframe_%s.svg" % cand["id"]),
                              "%s wireframe (grid outlines from coordinate sets)" % cand["id"],
                              cand["center"], inner, full, env, rows)

    # ---- 12-direction grid symmetry (Q40) on block-centred grid ----
    sym12 = {}
    for rad in (13.5, 14.0, 14.5, 15.0):
        pts = {(round(rad * math.cos(2 * math.pi * k / 12)), round(rad * math.sin(2 * math.pi * k / 12))) for k in range(12)}
        sym12[str(rad)] = {"points": sorted(pts), "count": len(pts), "symmetry": symmetric("B", pts)}
    results["sanctum_12dir_rounding"] = sym12
    # largest odd block-centred disc whose cube corners all stay within a 45-block Euclidean circle
    results["euclid45_max_axis_D"] = max(d for d in range(1, 46, 2) if far_corner_diameter("B", disc("B", d)) <= 45.0)

    # ---- forward check: Q55 3x3 / Q59 7x7 altar patterns vs Q67 central D17 (+ Q70 band) ----
    for din in (25, 27):
        inner = disc("B", din)
        fw = {}
        for label, keep in (("central_D17", disc("B", 17)), ("central_D17+band1(D19)", disc("B", 19))):
            fw[label] = {"7x7_positions": len(square_fits("B", inner, keep, 7)),
                         "3x3_positions": len(square_fits("B", inner, keep, 3))}
        # informational: if Q51 2-block niches were counted as usable floor beyond the inner face
        niche = disc("B", din + 4)
        fw["inner+niche2(D%d)_vs_D17" % (din + 4)] = {
            "7x7_positions": len(square_fits("B", niche, disc("B", 17), 7))}
        results["forward_geometry03"]["B%d" % din] = fw
    for din in (26,):
        inner = disc("C", din)
        results["forward_geometry03"]["C26"] = {"note": "odd-size patterns (3x3/7x7/9x9/17) cannot be centred on a "
                                                         "corner axis; not evaluated"}

    # ---- stair ring input (Q29 ~1 turn per floor) ----
    st = {}
    for cand in results["candidates"]:
        if cand["id"] in primary:
            r_mid = cand["wall_outer_d"] / 2 + STAIR_WALK / 2
            circ = 2 * math.pi * r_mid
            st[cand["id"]] = {"walk_centerline_radius": r_mid, "circumference": round(circ, 1),
                              "rise_per_turn": {"%s->%s" % (a["segment"], b["segment"]): a["budget_h"]
                                                for a, b in zip(segs[1:-1], segs[2:])},
                              "avg_grade_per_block": {"%s->%s" % (a["segment"], b["segment"]): round(a["budget_h"] / circ, 3)
                                                      for a, b in zip(segs[1:-1], segs[2:])}}
    results["stair_input"] = st

    # ---- summary checks ----
    def chk(cid, item, status, evidence):
        results["checks"].append({"id": cid, "item": item, "status": status, "evidence": evidence})

    chk("V1", "height sum = 80 (Q27/Q28)", "PASS" if sum_h == 80 and span == 80 else "CONFLICT",
        "sum budgets %d; coordinate span y0..y79 = %d" % (sum_h, span))
    chk("V2", "no overlap / gap between segments", "PASS" if contiguous and all(v == 1 for v in cover) else "CONFLICT",
        "each of 80 layers belongs to exactly one segment")
    for cand in results["candidates"]:
        ok_env = cand["extent_wall_outer"][0] <= ENV_D[cand["center"]]
        chk("P-" + cand["id"], "plan candidate", "PASS" if (ok_env and cand["closed_face_adjacency"]
                                                           and all(cand["symmetry_wall"].values())
                                                           and cand["corner_only_joints"] == 0) else "CANDIDATE_FAIL",
            "outer %s, inner %s, reserve %d, corner joints %d, fits %s" % (
                cand["extent_wall_outer"], cand["extent_inner"], cand["reserve_band"],
                cand["corner_only_joints"], cand["fits"]))

    with open(os.path.join(args.out, "results.json"), "w") as f:
        json.dump(results, f, indent=1)

    # ---- console summary ----
    print("vertical: sum=%d span=%d contiguous=%s" % (sum_h, span, contiguous))
    print("%-8s %5s %4s %5s %5s %5s %6s %6s %6s %7s %7s %3s %s" % ("cand", "inner", "t", "outer", "resv", "wallN",
                                                            "innerN", "tmin", "tmax", "diagD", "closed", "cj", "fits"))
    for c in results["candidates"]:
        print("%-8s %5s %4d %5s %5d %5d %6d %6.2f %6.2f %7.2f %7s %3d %s" % (
            c["id"], c["extent_inner"][0], c["wall_t"], c["extent_wall_outer"][0], c["reserve_band"],
            c["cells"]["wall_per_layer"], c["cells"]["interior"], c["ray"]["wall_thickness_min"],
            c["ray"]["wall_thickness_max"], c["far_corner_diameter_wall"], c["closed_face_adjacency"],
            c["corner_only_joints"], c["fits"]))
    print("euclid45_max_axis_D", results["euclid45_max_axis_D"])
    for k, v in results["forward_geometry03"].items():
        print("forward", k, v)
    for c in results["candidates"]:
        if "model3d_C1_s1" in c:
            m = c["model3d_C1_s1"]
            print("3d", c["id"], {k: m[k] for k in ("voxels", "wall_vertically_aligned", "voxels_outside_wall_outer_disc",
                                                    "voxels_outside_y0_y79", "voxels_above_sanctum_floor")})
    print("12dir", {k: (v["count"], all(v["symmetry"].values())) for k, v in sym12.items()})
    bad = [c for c in results["checks"] if c["status"] != "PASS"]
    print("checks: %d total, %d non-PASS" % (len(results["checks"]), len(bad)))
    for c in bad:
        print("  ", c["id"], c["status"], c["evidence"])
    print("stair", {k: v["avg_grade_per_block"] for k, v in results["stair_input"].items()})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

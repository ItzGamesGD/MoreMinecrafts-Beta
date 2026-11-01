#!/usr/bin/env python3
"""Stage 0-G blueprint renderer. Input: *.json.gz written by BlueprintExport (same ArenaDesign.build / persisted live plans as Stage 0-F).
Usage: render_blueprints.py <data-dir> <out-dir> [meta-dir]   (meta-dir: dir with f-<id>.meta route files for the 'built-*' scenarios)"""
import gzip, json, sys, os, math, colorsys, hashlib
from collections import Counter, defaultdict
from PIL import Image, ImageDraw, ImageFont

S = 9  # px per block (plan/section)
try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    FONTB = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
except Exception:
    FONT = FONTB = ImageFont.load_default()

OVR = {"air": (246, 246, 246), "deepslate_bricks": (70, 72, 82), "polished_deepslate": (96, 98, 108), "deepslate_tiles": (52, 54, 64),
       "cracked_deepslate_bricks": (82, 82, 92), "cracked_deepslate_tiles": (62, 62, 72), "cobbled_deepslate": (110, 110, 118),
       "deepslate": (90, 90, 96), "chiseled_deepslate": (60, 64, 80), "vault": (30, 160, 190), "ominous_vault": (140, 40, 200),
       "redstone_lamp": (255, 214, 80), "soul_lantern": (90, 220, 230), "wall_torch": (255, 150, 30), "torch": (255, 150, 30), "soul_torch": (80, 200, 220),
       "deepslate_brick_stairs": (74, 76, 90), "polished_deepslate_stairs": (100, 102, 114), "deepslate_tile_stairs": (56, 58, 70),
       "deepslate_brick_slab": (74, 76, 90), "deepslate_tile_slab": (56, 58, 70), "stone_bricks": (130, 130, 130), "tuff_bricks": (150, 140, 120)}

def base(spec):
    return spec.split(":", 1)[-1].split("[", 1)[0]

def color(spec):
    b = base(spec)
    if b in OVR: return OVR[b]
    h = int(hashlib.md5(b.encode()).hexdigest()[:6], 16)
    r, g, bl = colorsys.hsv_to_rgb((h % 360) / 360.0, 0.45, 0.75)
    return (int(r * 255), int(g * 255), int(bl * 255))

def load(p):
    with gzip.open(p, "rt") as f:
        return json.load(f)

class Sc:
    def __init__(s, d):
        s.d = d; s.cx, s.fy, s.cz = d["center"]; s.pal = d["palette"]
        s.cells = {(x, y, z): pi for x, y, z, pi in d["cells_xyz_palette"]}
        s.spec = lambda c: s.pal[s.cells[c]] if c in s.cells else None

def label(dr, xy, t, f=FONT, fill=(20, 20, 20)):
    dr.text(xy, t, font=f, fill=fill)

def routes_from_meta(path):
    rs = []
    if not path or not os.path.exists(path): return rs
    cur = None
    for ln in open(path):
        p = ln.split()
        if not p: continue
        if p[0] == "route": cur = []; rs.append((int(p[1]), cur))
        elif p[0] == "variant": continue
        elif cur is not None and len(p) >= 6 and p[0].lstrip("-").isdigit(): cur.append(tuple(int(v) for v in p[:6]))
    return rs

def slice_img(sc, y, ext, title, routes=None):
    n = 2 * ext + 1
    im = Image.new("RGB", (n * S, n * S + 36), (200, 200, 205)); dr = ImageDraw.Draw(im)
    for dx in range(-ext, ext + 1):
        for dz in range(-ext, ext + 1):
            c = (sc.cx + dx, y, sc.cz + dz)
            sp = sc.spec(c)
            col = (200, 200, 205) if sp is None else color(sp)   # grey-blue = not edited by the plan (natural terrain, unchanged)
            px, py = (dx + ext) * S, (dz + ext) * S + 36
            dr.rectangle([px, py, px + S - 1, py + S - 1], fill=col)
    label(dr, (6, 8), title, FONTB)
    return im

def ring(dr, ext, r, off=36, col=(220, 60, 60)):
    cx = (ext + 0.5) * S
    dr.ellipse([cx - r * S, cx - r * S + off, cx + r * S, cx + r * S + off], outline=col, width=2)

def plan_view(sc, ext=52, routes=None):
    y = sc.fy
    im = slice_img(sc, y, ext, f"{sc.d['name']}  [{sc.d['variant']}]  floor-level plan (Y={y}, centre X={sc.cx} Z={sc.cz})  N up, E right")
    dr = ImageDraw.Draw(im)
    for r, col in ((34, (220, 60, 60)), (29, (60, 160, 60)), (27, (40, 120, 200)), (24, (170, 120, 30)), (12, (170, 120, 30))):
        ring(dr, ext, r, col=col)
    for vx, vz in sc.d["vaults_xz"]:
        px, py = (vx - sc.cx + ext) * S, (vz - sc.cz + ext) * S + 36
        dr.rectangle([px - 2, py - 2, px + S + 2, py + S + 2], outline=(0, 150, 200), width=2)
    for qx, qz in sc.d["columns_xz"]:
        px, py = (qx - sc.cx + ext) * S, (qz - sc.cz + ext) * S + 36
        dr.rectangle([px - 1, py - 1, px + S, py + S], outline=(255, 0, 255), width=1)
    for i, (dx, dz) in enumerate(sc.d["constants"]["DIR_N_E_S_W"]):
        px, py = (ext + dx * 37 + 0.5) * S, (ext + dz * 37 + 0.5) * S + 36
        label(dr, (px - 6, py - 8), "NESW"[i], FONTB, (200, 0, 0))
    for d, cells in (routes or []):
        for x, z, yf, h, s, dy in cells:
            px, py = (x - sc.cx + ext) * S, (z - sc.cz + ext) * S + 36
            if 0 <= px < im.width and 36 <= py < im.height: dr.rectangle([px + 3, py + 3, px + S - 4, py + S - 4], fill=(230, 0, 90))
    return im

def section_view(sc, axis="x", yr=(-4, 27), ext=44):
    h = yr[1] - yr[0] + 1; n = 2 * ext + 1
    im = Image.new("RGB", (n * S, h * S + 40), (200, 200, 205)); dr = ImageDraw.Draw(im)
    for a in range(-ext, ext + 1):
        for oy in range(yr[0], yr[1] + 1):
            c = (sc.cx + a, sc.fy + oy, sc.cz) if axis == "x" else (sc.cx, sc.fy + oy, sc.cz + a)
            sp = sc.spec(c); col = (200, 200, 205) if sp is None else color(sp)
            px, py = (a + ext) * S, (yr[1] - oy) * S + 40
            dr.rectangle([px, py, px + S - 1, py + S - 1], fill=col)
    for oy in (0, 12, 23):
        py = (yr[1] - oy) * S + 40
        dr.line([0, py, im.width, py], fill=(220, 60, 60), width=1); label(dr, (4, py - 14), f"floor+{oy} (Y={sc.fy + oy})" if oy else f"floor Y={sc.fy}")
    label(dr, (6, 8), f"{sc.d['name']} [{sc.d['variant']}] vertical section through centre, {'W-E (z=%d)' % sc.cz if axis == 'x' else 'N-S (x=%d)' % sc.cx}; blue-grey = block not edited (natural terrain)", FONTB)
    return im

def layers(sc, offsets, ext=40, outdir=None):
    ims = []
    table = {}
    for o in offsets:
        y = sc.fy + o
        cnt = Counter(base(sc.pal[pi]) for (x, yy, z), pi in sc.cells.items() if yy == y and abs(x - sc.cx) <= ext and abs(z - sc.cz) <= ext)
        im = slice_img(sc, y, ext, f"{sc.d['name']} layer offset +{o} (Y={y})")
        ims.append(im)
        key = {"vault": [], "ominous_vault": [], "redstone_lamp": [], "wall_torch": []}
        for (x, yy, z), pi in sc.cells.items():
            b = base(sc.pal[pi])
            if yy == y and b in key and len(key[b]) < 40: key[b].append([x, y, z])
        table[f"+{o}"] = {"Y": y, "block_counts": dict(cnt.most_common()), "key_block_coordinates": {k: v for k, v in key.items() if v}}
    return ims, table

def legend(sc):
    cnt = Counter(base(sp) for sp in (sc.pal[pi] for pi in sc.cells.values()))
    im = Image.new("RGB", (560, 40 + 22 * (len(cnt) + 16)), (255, 255, 255)); dr = ImageDraw.Draw(im)
    label(dr, (8, 8), f"LEGEND - {sc.d['name']} ({sc.d['variant']})", FONTB)
    y = 40
    for b, c in cnt.most_common():
        dr.rectangle([8, y, 24, y + 14], fill=color(b), outline=(0, 0, 0)); label(dr, (32, y - 1), f"{b}  ({c} cells)"); y += 22
    dr.rectangle([8, y, 24, y + 14], fill=(200, 200, 205), outline=(0, 0, 0)); label(dr, (32, y - 1), "not edited (natural terrain unchanged)"); y += 28
    c = sc.d["constants"]
    notes = [f"centre (X,Y,Z)=({sc.cx},{sc.fy},{sc.cz}); floorY={sc.fy}; id={sc.d['id']}",
             f"exterior r={c['EXT_R']} (68 dia); combat floor r={c['IN_R']}; vault ring r={c['VAULT_R']}; spawn annulus r=12..24",
             f"wall {c['WALL_H']} + dome {c['DOME_H']} = {c['TOTAL_H']}; entrances {c['ENT_W']}x{c['ENT_H']} at N(-Z) E(+X) S(+Z) W(-X)",
             f"vault blocks: {len(sc.d['vaults_xz'])} on r=27 + 1 ominous at centre; columns: {len(sc.d['columns_xz'])} (r=28)",
             "plan overlays: red r=34, green r=29, blue r=27, ochre r=12/24; cyan boxes=vaults; magenta=columns; pink=corridor route",
             f"plan hash {sc.d['plan_hash'][:16]}..., axes: +X east, +Z south, +Y up"]
    for t in notes: label(dr, (8, y), t, FONT); y += 20
    return im

def sheet(images, cols):
    w = max(i.width for i in images); h = max(i.height for i in images)
    rows = math.ceil(len(images) / cols)
    out = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for k, i in enumerate(images): out.paste(i, ((k % cols) * w, (k // cols) * h))
    return out

def main():
    data, out = sys.argv[1], sys.argv[2]; metadir = sys.argv[3] if len(sys.argv) > 3 else None
    os.makedirs(out, exist_ok=True)
    scs = [Sc(load(os.path.join(data, f))) for f in sorted(os.listdir(data)) if f.endswith(".json.gz")]
    allt = {}
    for sc in scs:
        n = sc.d["name"]
        routes = routes_from_meta(os.path.join(metadir, "f-%s.meta" % sc.d["id"])) if metadir and n.startswith("built") else []
        plan_view(sc, routes=routes).save(f"{out}/{n}-plan.png")
        section_view(sc, "x").save(f"{out}/{n}-section-WE.png")
        section_view(sc, "z").save(f"{out}/{n}-section-NS.png")
        legend(sc).save(f"{out}/{n}-legend.png")
        ims, tab = layers(sc, (-1, 0, 2, 6, 11, 14, 19, 22))
        sheet(ims, 4).save(f"{out}/{n}-layers.png")
        allt[n] = tab
        json.dump({"scenario": n, "variant": sc.d["variant"], "id": sc.d["id"], "center": sc.d["center"], "vaults_xz": sc.d["vaults_xz"], "columns_xz": sc.d["columns_xz"],
                   "routes": [{"dir": d, "cells_x_z_floorY_heading_s_dy": c} for d, c in routes], "layers": tab}, open(f"{out}/{n}-blueprint.json", "w"), indent=1)
    refs = [s for s in scs if s.d["name"].startswith("reference")]
    if refs:
        sheet([plan_view(s, ext=40) for s in refs], 3).save(f"{out}/variant-comparison-plan.png")
        sheet([section_view(s, "x", ext=40) for s in refs], 1).save(f"{out}/variant-comparison-section.png")
    print("rendered", [s.d["name"] for s in scs])

main()

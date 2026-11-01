#!/usr/bin/env python3
"""Builds the scene list for St05ArenaVisualClientGameTest from the exported blueprint data (same plans the live run built).
Usage: make_scenes.py <blueprint-data-dir> <meta-dir> <out.json>"""
import gzip, json, math, sys, os
data, metas, out = sys.argv[1:4]
DIRS = [(0, -1), (1, 0), (0, 1), (-1, 0)]
ARENAS = [  # (blueprint name, world level dir name, seed, original world dir)
    ("built-20d89ee7", "capw-a", 7001, "stage0f-world-7001"), ("built-809b9bf8", "capw-b", 7002, "stage0f-world-7002"),
    ("built-2ac822e8", "capw-c", 7001, "stage0f-world-7001-g"), ("built-77ba83e0", "capw-d", 7001, "stage0f-world-7001-p")]

def look(c, t):
    dx, dy, dz = t[0] - c[0], t[1] - c[1], t[2] - c[2]
    return round(math.degrees(math.atan2(-dx, dz)), 1), round(-math.degrees(math.atan2(dy, math.hypot(dx, dz))), 1)

def routes(i):
    rs, cur = [], None
    for ln in open(os.path.join(metas, f"f-{i}.meta")):
        p = ln.split()
        if p[0] == "route": cur = []; rs.append((int(p[1]), cur))
        elif p[0] != "variant" and cur is not None and len(p) >= 6: cur.append([int(v) for v in p[:6]])
    return rs

worlds = []
for name, level, seed, orig in ARENAS:
    d = json.load(gzip.open(os.path.join(data, name + ".json.gz"), "rt"))
    cx, fy, cz = d["center"]; var = d["variant"]; tag = f"{d['id'][:8]}_{var.lower()}"
    sc = []
    def add(n, eye, tgt, what, cut="", wait=120):
        yaw, pitch = look(eye, tgt)
        sc.append({"name": f"{tag}__{n}", "x": eye[0], "y": eye[1] - 1.62, "z": eye[2], "yaw": yaw, "pitch": pitch, "what": what, "cutaway": cut,
                   "cx": cx, "cz": cz, "fy": fy, "eye": eye, "target": tgt, "wait": wait})
    add("02_interior_overview", (cx - 24.5, fy + 2.5, cz + 8.5), (cx + 12, fy + 12, cz - 6), "interior from the combat floor, far dome / wall")
    add("02b_interior_dome_up", (cx + 0.5, fy + 2.5, cz + 14.5), (cx, fy + 21, cz - 6), "interior looking up into the dome / ceiling")
    for i, (ax, az) in enumerate(DIRS):
        add(f"03_entrance_{'NESW'[i]}", (cx - ax * 12 + 0.5, fy + 2.5, cz - az * 12 + 0.5), (cx + ax * 33 + 0.5, fy + 3.5, cz + az * 33 + 0.5), f"entrance {'NESW'[i]} (7x8 arch) from the floor, columns around")
    vx, vz = d["vaults_xz"][0]
    add("04_vault_ring", (cx + 0.5, fy + 4.5, cz + 0.5), (vx + 0.5, fy + 0.5, vz + 0.5), "vault ring on r=27, seen from the centre")
    ox, oz = (vx - cx), (vz - cz); n = math.hypot(ox, oz); ux, uz = ox / n, oz / n
    add("04b_vault_close", (vx - ux * 6 + 0.5, fy + 2.5, vz - uz * 6 + 0.5), (vx + 0.5, fy + 0.8, vz + 0.5), "one ordinary Vault close up")
    add("04c_ominous_vault_centre", (cx + 0.5, fy + 2.8, cz + 5.5), (cx + 0.5, fy + 0.6, cz + 0.5), "Ominous Vault at the exact centre")
    rts = routes(d["id"])
    for k, (dr, cells) in enumerate(rts[:2]):
        a, b = cells[min(2, len(cells) - 1)], cells[-1]
        mid = cells[len(cells) // 2]
        add(f"05_corridor_{k}_entrance_side", (a[0] + 0.5, a[2] + 3.4, a[1] + 0.5), (mid[0] + 0.5, mid[2] + 2.5, mid[1] + 0.5), f"corridor {k} (dir {'NESW'[dr]}) from the arena end looking outward")
        e = cells[-4] if len(cells) > 4 else cells[0]
        add(f"05b_corridor_{k}_cave_junction", (e[0] + 0.5, e[2] + 3.4, e[1] + 0.5), (b[0] + 0.5, b[2] + 2.5, b[1] + 0.5), f"corridor {k} junction with the natural cave")
    add("06_dome_exterior_aerial", (cx + 62.5, fy + 58, cz + 62.5), (cx, fy + 8, cz), "dome exterior from above, terrain above fy+24 removed in the photo copy", cut="top", wait=200)
    add("06b_dome_cross_section", (cx + 52.5, fy + 10, cz + 0.5), (cx, fy + 9, cz + 0.5), "vertical section through the centre (east half removed in the photo copy)", cut="half", wait=200)
    add("06c_dome_cross_section_oblique", (cx + 48.5, fy + 26, cz + 36.5), (cx - 3, fy + 8, cz), "oblique view of the section", cut="half", wait=60)
    worlds.append({"level": level, "orig_world": orig, "seed": seed, "arena": name, "id": d["id"], "variant": var, "center": [cx, fy, cz], "scenes": sc})
json.dump({"worlds": worlds}, open(out, "w"), indent=1)
print(sum(len(w["scenes"]) for w in worlds), "scenes")

#!/usr/bin/env python3
"""Builds docs/design/st01-p1/ from the verified model. Usage: python3 -I build_report.py <repo_root>"""
import json, os, shutil, subprocess, sys
root = sys.argv[1]
tools = os.path.join(root, "tools/st01-p1")
dest = os.path.join(root, "docs/design/st01-p1")
gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True)
os.makedirs(gen)
subprocess.run([sys.executable, "-I", os.path.join(tools, "st01_p1_model.py"), gen], check=True, capture_output=True)
R = json.load(open(os.path.join(gen, "verification.json")))
def rd(n): return open(os.path.join(gen, n)).read().rstrip("\n")
def rng(a): return f"{a[0]}..{a[1]}" if a[0] != a[1] else f"{a[0]}"

rows = []
for t in R["table"]:
    rows.append(f"| {t['id']} | {t['name']} | {rng(t['x'])} | {rng(t['y'])} | {rng(t['z'])} | {t['size'][0]}×{t['size'][1]}×{t['size'][2]} | {t['feet_floor_Y']} | {t['clear_h']} | "
                f"{t['min_shell'] if t['min_shell'] is not None else 'n/a (below ground)'} | {t['min_roof_mass']} | {t['min_headroom_walked']} | {t['status']} |")
space_tbl = "\n".join(rows)

el = {}
for nm, kind, parent, xs, ys, zs in R["elems"]:
    el.setdefault(kind, []).append((nm, parent, xs, ys, zs))
erows = []
for kind in ("column", "ledge", "parapet"):
    for nm, parent, xs, ys, zs in el[kind]:
        erows.append(f"| {nm} | {kind} | {parent} | {rng(xs)} | {rng(ys)} | {rng(zs)} |")
erows.append(f"| descent_tread1..5 | stair | S05 | -5..5 | per tread (feet F=-1..-5; solid below F) | z=-1..-5 |")
erows.append(f"| altar_tread1..3 | stair | S09 | -3..3 | feet F=8,9,10 (tread 1 is the carved passage floor) | z=-9..-11 |")
erows.append(f"| galstair_W1..6 / galstair_E1..6 | stair | S04 | -18..-12 / 12..18 | feet F=1..6 (solid fill Y0..F-1) | z=15..10 |")
elem_tbl = "\n".join(erows)

orows = []
nm = {t["id"]: t["name"] for t in R["table"]}
for a, b, ax, r, area in R["openings"]:
    (x0, x1), (y0, y1), (z0, z1) = r
    rect = []
    for k, (lo, hi) in zip("XYZ", ((x0, x1), (y0, y1), (z0, z1))):
        if lo > hi: rect.append(f"{k}={hi}|{lo} (contact plane)")
        else: rect.append(f"{k}={lo}..{hi} ({hi-lo+1})")
    orows.append(f"| {a} ↔ {b} | {nm[a]} → {nm[b]} | {'; '.join(rect)} | {area} |")
open_tbl = "\n".join(orows)

vrows = []
for c in R["checks"]:
    vrows.append(f"| {c['id']} | {c['verdict']} | {c['title']} |")
verd_tbl = "\n".join(vrows)
evid = "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in R["checks"])
npass = sum(c["verdict"] == "PASS" for c in R["checks"]); nfail = sum(c["verdict"] == "FAIL" for c in R["checks"])

doc = open(os.path.join(tools, "report_template.md")).read()
for k, v in {
    "{{SPACE_TABLE}}": space_tbl, "{{ELEM_TABLE}}": elem_tbl, "{{OPEN_TABLE}}": open_tbl,
    "{{VERDICT_TABLE}}": verd_tbl, "{{EVIDENCE}}": evid, "{{NPASS}}": str(npass), "{{NFAIL}}": str(nfail),
    "{{PLAN_Y0}}": rd("plan_Y+0_crop.txt"), "{{PLAN_Y7}}": rd("plan_Y+7_crop.txt"),
    "{{PLAN_Y11}}": rd("plan_Y+11_crop.txt"), "{{PLAN_YM6}}": rd("plan_Y-6_crop.txt"),
    "{{SEC_X0}}": rd("section_X+0_crop.txt"), "{{SEC_Z5}}": rd("section_Z+5_crop.txt"), "{{SEC_ZM10}}": rd("section_Z-10_crop.txt"),
}.items():
    doc = doc.replace(k, v)
open(os.path.join(dest, "ST01_P1_GROUND_3D_COORDINATE_DESIGN_2026-11-06.md"), "w").write(doc)
os.remove(os.path.join(gen, "section_X+0_full.txt"))
print("written", len(doc), "bytes;", npass, "PASS", nfail, "FAIL")

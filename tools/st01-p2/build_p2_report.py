#!/usr/bin/env python3
"""Builds docs/design/st01-p2/ST01_P2_UNDERGROUND_3D_COORDINATE_DESIGN_2026-11-06.md and generated/ from the verified models.
Usage: python3 -I build_p2_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
from st01_p2_model_east import make_east
from st01_p2_model_west import make_west
root = sys.argv[1]
dest = os.path.join(root, "docs/design/st01-p2"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen)
rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, "st01_p2_verify.py"), gen], capture_output=True, text=True)
assert rc.returncode == 0, rc.stdout + rc.stderr
J = json.load(open(os.path.join(gen, "p2_verification.json")))
E, W = make_east(), make_west()
RE, RW = J["east"], J["west"]

def bbox(sp):
    b = sp["boxes"]; return (min(x[0] for x in b), max(x[1] for x in b), min(x[2] for x in b), max(x[3] for x in b), min(x[4] for x in b), max(x[5] for x in b))
def table(m):
    rows = []
    for k, s in m["spaces"].items():
        x0, x1, y0, y1, z0, z1 = bbox(s)
        rows.append(f"| {k} | {s['name']} | {s['level']} | {x0}..{x1} | {y0}..{y1} | {z0}..{z1} | {x1-x0+1}×{y1-y0+1}×{z1-z0+1} | {y0} | {s.get('req_w','-')} | {s['kind']} |")
    for k, r in m["res"].items():
        x0, x1, y0, y1, z0, z1 = r["box"]
        rows.append(f"| {k} | {r['name']} | reserve | {x0}..{x1} | {y0}..{y1} | {z0}..{z1} | {x1-x0+1}×{y1-y0+1}×{z1-z0+1} | - | - | reservation |")
    return "\n".join(rows)
def vtable():
    rows = []
    for a, b in zip(RE["checks"], RW["checks"]):
        rows.append(f"| {a['id']} | {a['title']} | {a['verdict']} | {b['verdict']} |")
    return "\n".join(rows)
def evid(R): return "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in R["checks"])
def trim(txt):
    L = txt.split("\n"); head = L[:3]; body = L[3:]
    keep = [i for i, l in enumerate(body) if any(ch not in " !" for ch in l[6:])]
    if not keep: return txt
    body = body[keep[0]:keep[-1]+1]
    cols = [j for l in body for j, ch in enumerate(l) if j >= 6 and ch not in " !"]
    c0, c1 = max(6, min(cols)-1), max(cols)+2
    return "\n".join([head[1][:6] + head[1][c0:c1]] + [head[2][:6] + head[2][c0:c1]] + [l[:6] + l[c0:c1] for l in body]) if head[2].lstrip().startswith("Z") else "\n".join([l[:6] + l[c0:c1] for l in L[1:3]] + [l[:6] + l[c0:c1] for l in body])
def rd(side, f, tr=True):
    t = open(os.path.join(gen, side, f)).read().rstrip("\n")
    lines = t.split("\n")
    if tr and f.startswith("plan"):
        head = lines[1:3]; body = lines[3:]
        keep = [i for i, l in enumerate(body) if any(ch not in " !" for ch in l[6:])]
        body = body[keep[0]:keep[-1]+1]
        cols = [j for l in body for j, ch in enumerate(l) if j >= 6 and ch not in " !"]
        c0, c1 = max(6, min(cols)-2), max(cols)+3
        return "\n".join([lines[1]] + [h[:6]+h[c0:c1] for h in lines[2:3]] + [l[:6]+l[c0:c1] for l in body])
    return "\n".join(lines[1:])
EQ = J["equivalence"]; me, mw = RE["metrics"], RW["metrics"]
cmp_rows = [
 ("지하 공동 X 최소..최대", f"{me['cavity_x'][0]}..{me['cavity_x'][1]}", f"{mw['cavity_x'][0]}..{mw['cavity_x'][1]}"),
 ("지하 공동 Z 최소..최대", f"{me['cavity_z'][0]}..{me['cavity_z'][1]}", f"{mw['cavity_z'][0]}..{mw['cavity_z'][1]}"),
 ("예약 체적 X / Z", f"{me['reserve_x'][0]}..{me['reserve_x'][1]} / {me['reserve_z'][0]}..{me['reserve_z'][1]}", f"{mw['reserve_x'][0]}..{mw['reserve_x'][1]} / {mw['reserve_z'][0]}..{mw['reserve_z'][1]}"),
 ("±40 초과 최대 거리(공동·예약)", me["ext_blocks"], mw["ext_blocks"]),
 ("±40 밖 공동 복셀 수 (복셀 / 해석)", f"{me['cavity_voxels_outside_pm40']} / {me['analytic_cavity_outside']}", f"{mw['cavity_voxels_outside_pm40']} / {mw['analytic_cavity_outside']}"),
 ("±40 밖 예약 복셀 수", me["reserved_voxels_outside_pm40"], mw["reserved_voxels_outside_pm40"]),
 ("전체 최저 공동 Y (L1–L3)", me["lowest_cavity_Y"], mw["lowest_cavity_Y"]),
 ("예약 포함 최저 Y (이 모델, 비밀 후보 A 포함)", me["lowest_reserved_Y"], mw["lowest_reserved_Y"]),
 ("P2 공동 복셀 총수", me["p2_cavity_voxels"], mw["p2_cavity_voxels"]),
 ("P1 착지→왕실 홀 입구 보행 거리(4방향 걸음)", RE["route_distance"]["MAIN:PORT"], RW["route_distance"]["MAIN:PORT"]),
 ("검증 PASS / 전체", f"{sum(c['verdict']=='PASS' for c in RE['checks'])} / {len(RE['checks'])}", f"{sum(c['verdict']=='PASS' for c in RW['checks'])} / {len(RW['checks'])}"),
 ("복셀 거울 동치(solid/owner/reserved)", f"{EQ['voxel_solid']}/{EQ['voxel_owner']}/{EQ['voxel_reserved']}", "(동일 비교)"),
]
cmp_tbl = "\n".join(f"| {a} | {b} | {c} |" for a, b, c in cmp_rows)
hall_tbl = "\n".join(f"| {r['corner']} | X {r['x'][0]}..{r['x'][1]}, Z {r['z'][0]}..{r['z'][1]} | {', '.join(r['conflicts_at_design_depth']) or '없음'} | {r['extra_depth_to_clear']} | {r['lowest_Y_if_cleared']} |" for r in J["hall_corner_search"])
vs = J["vault_search"]
vault_tbl = "\n".join(f"| {k} | e={v['e']} | {v['placements']} | X {v['first'][0]}..{v['first'][1]}, Z {v['first'][2]}..{v['first'][3]} | Y {v['y'][0]}..{v['y'][1]} | 홀과의 평면 간격 {v['nearest_to_hall']['plan_gap_blocks']}: X {v['nearest_to_hall']['x_z'][0]}..{v['nearest_to_hall']['x_z'][1]}, Z {v['nearest_to_hall']['x_z'][2]}..{v['nearest_to_hall']['x_z'][3]} |" for k, v in vs.items())
depth_tbl = "\n".join(f"| {a} | {b} | {c} |" for a, b, c in J["depth"])
vsep = "\n".join(f"| {g} | {a} | {b} |" for g, a, b in RE["vsep"])
sc = J["secret_candidate"]
wrows = "\n".join(f"| {k} | {rw} | {w} | {rh} | {h} |" for k, rw, w, rh, h in RE["metrics"]["width_rows"])
cent = RE["metrics"]["centroids"]
dist = RE["route_distance"]

doc = open(os.path.join(HERE, "p2_report_template.md")).read()
subs = {"{{E_TABLE}}": table(E), "{{W_TABLE}}": table(W), "{{VERDICTS}}": vtable(), "{{E_EVIDENCE}}": evid(RE), "{{W_EVIDENCE}}": evid(RW),
        "{{CMP}}": cmp_tbl, "{{HALL}}": hall_tbl, "{{VAULT}}": vault_tbl, "{{DEPTH}}": depth_tbl, "{{VSEP}}": vsep, "{{WIDTHS}}": wrows,
        "{{PLAN_L1}}": rd("east", "plan_L1_Y-6.txt"), "{{PLAN_L12}}": rd("east", "plan_L12_Y-10.txt"), "{{PLAN_L2}}": rd("east", "plan_L2_Y-14.txt"),
        "{{PLAN_L23}}": rd("east", "plan_L23_Y-18.txt"), "{{PLAN_L3}}": rd("east", "plan_L3_Y-22.txt"), "{{PLAN_V}}": rd("east", "plan_secret_vault_candidate_Y-33.txt"),
        "{{PLAN_W_L3}}": rd("west", "plan_L3_Y-22.txt"), "{{PLAN_W_L1}}": rd("west", "plan_L1_Y-6.txt"),
        "{{SEC_X9}}": rd("east", "section_X+9.txt", False), "{{SEC_ZM7}}": rd("east", "section_Z-7.txt", False), "{{SEC_Z7}}": rd("east", "section_Z+7.txt", False),
        "{{CENT_L1}}": f"({cent['L1'][0]:.1f}, {cent['L1'][1]:.1f})", "{{CENT_L2}}": f"({cent['L2'][0]:.1f}, {cent['L2'][1]:.1f})", "{{CENT_L3}}": f"({cent['L3'][0]:.1f}, {cent['L3'][1]:.1f})",
        "{{DIST_PORT}}": str(dist["MAIN:PORT"]), "{{SC_STRIP}}": str(sc["strip_width_for_stair"]), "{{SC_RUN}}": str(sc["horizontal_run_available"]), "{{SC_RISE}}": str(sc["rise_hall_to_vault"]),
        "{{NPASS_E}}": str(sum(c['verdict']=='PASS' for c in RE['checks'])), "{{NPASS_W}}": str(sum(c['verdict']=='PASS' for c in RW['checks'])), "{{NCHK}}": str(len(RE['checks']))}
for k, v in subs.items(): doc = doc.replace(k, v)
assert "{{" not in doc, [l for l in doc.split("\n") if "{{" in l][:3]
open(os.path.join(dest, "ST01_P2_UNDERGROUND_3D_COORDINATE_DESIGN_2026-11-06.md"), "w").write(doc)
print("report", len(doc), "bytes")

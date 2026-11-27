#!/usr/bin/env python3
"""Builds docs/design/st01-p3/ (report, P4 handoff, generated plans/sections/json) from the verified P3 models.
Usage: python3 -I build_p3_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p3_spec as S
root = sys.argv[1]
dest = os.path.join(root, "docs/design/st01-p3"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen, exist_ok=True)
rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, "st01_p3_verify.py"), gen], capture_output=True, text=True)
assert rc.returncode == 0, rc.stdout + rc.stderr
rm = subprocess.run([sys.executable, "-I", os.path.join(HERE, "st01_p3_mutation_tests.py"), "--out", os.path.join(gen, "p3_mutation_results.json")], capture_output=True, text=True)
assert rm.returncode == 0, rm.stdout + rm.stderr
rs = subprocess.run([sys.executable, "-I", os.path.join(HERE, "p2_wall_fit_study.py"), os.path.join(gen, "p2_wall_fit_study.json")], capture_output=True, text=True)
assert rs.returncode == 0, rs.stdout + rs.stderr
J = json.load(open(os.path.join(gen, "p3_verification.json"))); M = json.load(open(os.path.join(gen, "p3_mutation_results.json"))); WF = json.load(open(os.path.join(gen, "p2_wall_fit_study.json")))
E = S.make_hall_east(); W = S.mirror_spec(E)
RE, RW = J["east"], J["west"]
def rd(side, name): return open(os.path.join(gen, side, name)).read().rstrip("\n")
def rng(a, b): return f"{a}..{b}" if a != b else f"{a}"
def bbox(boxes): return [min(b[0] for b in boxes), max(b[1] for b in boxes), min(b[2] for b in boxes), max(b[3] for b in boxes), min(b[4] for b in boxes), max(b[5] for b in boxes)]
def vol(boxes, cells=False): return sum((b[1]-b[0]+1)*(b[3]-b[2]+1)*(b[5]-b[4]+1) for b in boxes)
def cnt(R, v): return sum(c["verdict"] == v for c in R["checks"])
H = E["hall"]; F = H["floor"]; cx, cz = H["cx"], H["cz"]

# ---------------------------------------------------------------- element tables
rows = []
for k, e in E["elements"].items():
    if e["kind"].startswith("pillar") or e["kind"] == "lintel": continue
    b = bbox(e["boxes"])
    rows.append(f"| {k} | {e['kind']} | {rng(b[0],b[1])} | {rng(b[2],b[3])} | {rng(b[4],b[5])} | {b[1]-b[0]+1}×{b[3]-b[2]+1}×{b[5]-b[4]+1} | {vol(e['boxes'])} |")
elem_tbl = "\n".join(rows)
rrows = []
for k, r in E["recesses"].items():
    b = r["box"]; rrows.append(f"| {k} | {r['kind']} | {rng(b[0],b[1])} | {rng(b[2],b[3])} | {rng(b[4],b[5])} | {b[1]-b[0]+1}×{b[3]-b[2]+1}×{b[5]-b[4]+1} |")
rec_tbl = "\n".join(rrows)
P, L = S.make_pillars()
prow = []
for p in P:
    bb = bbox(p["base"]); sb = bbox(p["shaft"]); cb = bbox(p["cap"])
    prow.append(f"| {p['id']} | {p['side']} | {p['s']} | ({p['centre'][0]},{p['centre'][1]}) | X{rng(sb[0],sb[1])} Z{rng(sb[4],sb[5])} | X{rng(bb[0],bb[1])} Z{rng(bb[4],bb[5])} ({bb[1]-bb[0]+1}×{bb[5]-bb[4]+1}) | X{rng(cb[0],cb[1])} Z{rng(cb[4],cb[5])} ({cb[1]-cb[0]+1}×{cb[5]-cb[4]+1}) | {vol(p['base'])}/{vol(p['shaft'])}/{vol(p['cap'])} |")
pil_tbl = "\n".join(prow)
lrow = []
for l in L:
    b = bbox(l["boxes"]); lrow.append(f"| {l['id']} | {', '.join(l['pillars'])} | X{rng(b[0],b[1])} Z{rng(b[4],b[5])} | Y{F+7} | {vol(l['boxes'])} |")
lint_tbl = "\n".join(lrow)
spans = RE["metrics"]["corbel_spans"]
crow = []
for y in sorted(int(k) for k in spans):
    xa, xb, za, zb = spans[str(y)]
    kind = "벽(수직)" if y <= F + 7 else f"코벨 {y-(F+7)}단"
    crow.append(f"| {y} | {kind} | X{xa}..{xb} Z{za}..{zb} | {xb-xa+1}×{zb-za+1} |")
corbel_tbl = "\n".join(crow)
# pillar layout diagram
def layout(spec):
    h = spec["hall"]; grid = {}
    ids = {}
    for pid, e in spec["elements"].items():
        if e["kind"] == "pillar_shaft":
            ids[e["pillar"]] = chr(ord("a") + len(ids))
            for b in e["boxes"]:
                if b[2] == h["floor"] + 1:
                    for x in range(b[0], b[1] + 1):
                        for z in range(b[4], b[5] + 1): grid[(x, z)] = ids[e["pillar"]]
    L_ = [f"PILLAR LAYOUT (Y={h['floor']+1}, body cells). west left / east right, north up. a..j = pillar bodies; D=door (north wall), P=dais, O=obelisk, H=hatch zone"]
    cxx, czz, half = h["cx"], h["cz"], h["half"]
    for z in range(czz - half, czz + half + 1):
        row = ""
        for x in range(cxx - half, cxx + half + 1):
            c = grid.get((x, z))
            if c: row += c
            elif abs(x - cxx) <= 2 and abs(z - czz) <= 2: row += "O"
            elif z >= czz + 9 and abs(x - cxx) <= 5: row += "P"
            elif spec["hatch"][0] <= x <= spec["hatch"][1] and spec["hatch"][4] <= z <= spec["hatch"][5]: row += "H"
            elif z == czz - half and abs(x - cxx) <= 1: row += "D"
            else: row += "."
        L_.append(f"{z:+4d}  {row}")
    L_.append("Legend: " + ", ".join(f"{v}={k}" for k, v in ids.items()))
    return "\n".join(L_)
lay_e = layout(E); lay_w = layout(W)
open(os.path.join(gen, "east", "pillar_layout.txt"), "w").write(lay_e + "\n"); open(os.path.join(gen, "west", "pillar_layout.txt"), "w").write(lay_w + "\n")

def vtable(key):
    out = []
    for a, b in zip(RE["checks"], RW["checks"]):
        out.append(f"| {a['id']} | {a['title']} | {a['verdict']} | {b['verdict']} |")
    return "\n".join(out)
evid = "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in RE["checks"])
mut_rows = "\n".join(f"| {m['mutation']} | {', '.join(m['expected'])} | {', '.join(m['failed'])} | {'검출' if m['detected'] else '**미검출**'} |" for m in M["results"])
nm_det = sum(m["detected"] for m in M["results"]); nm = len(M["results"])
eq = J["equivalence"]
rmet = RE["metrics"]
sp = rmet["boss_spawn"]
hand_e = rmet["handedness"]; hand_w = RW["metrics"]["handedness"]

doc = open(os.path.join(HERE, "p3_report_template.md")).read()
rep = {
 "{{NPASS_E}}": str(cnt(RE, "PASS")), "{{NFAIL_E}}": str(cnt(RE, "FAIL")), "{{NUNV_E}}": str(cnt(RE, "UNVERIFIED")),
 "{{NPASS_W}}": str(cnt(RW, "PASS")), "{{NFAIL_W}}": str(cnt(RW, "FAIL")), "{{NUNV_W}}": str(cnt(RW, "UNVERIFIED")),
 "{{NCHECK}}": str(len(RE["checks"])), "{{NMUT}}": str(nm), "{{NMUT_DET}}": str(nm_det),
 "{{ELEM_TABLE}}": elem_tbl, "{{RECESS_TABLE}}": rec_tbl, "{{PILLAR_TABLE}}": pil_tbl, "{{LINTEL_TABLE}}": lint_tbl, "{{CORBEL_TABLE}}": corbel_tbl,
 "{{LAYOUT_E}}": lay_e, "{{VERDICT_TABLE}}": vtable(None), "{{EVIDENCE}}": evid, "{{MUT_TABLE}}": mut_rows,
 "{{PLAN_FLOOR_E}}": rd("east", "plan_floor_Y-22.txt"), "{{PLAN_BODY_E}}": rd("east", "plan_body_Y-20.txt"), "{{PLAN_LINTEL_E}}": rd("east", "plan_lintel_Y-15.txt"),
 "{{PLAN_CORBEL_E}}": rd("east", "plan_corbel_Y-12.txt"), "{{PLAN_PEAK_E}}": rd("east", "plan_peak_Y-7.txt"),
 "{{SEC_NS_E}}": rd("east", "section_axis_NS_X-21.txt"), "{{SEC_EW_E}}": rd("east", "section_axis_EW_Z21.txt"), "{{SEC_ALC_E}}": rd("east", "section_alcove_Z25.txt"),
 "{{PLAN_FLOOR_W}}": rd("west", "plan_floor_Y-22.txt"),
 "{{BOSS_SPAWN}}": "; ".join(f"{k}: min-corner {tuple(v)}" for k, v in sp.items()),
 "{{ESC_MIN}}": str(rmet["escape_min_dirs"]), "{{ESC_WEAK}}": str(rmet["escape_weak_positions"][:6]),
 "{{MAXCIRC}}": str(rmet["boss_max_circling_agent"]), "{{ROUTE_DAIS_HATCH}}": str(rmet["route_dais_to_hatch"]), "{{ROUTE_DAIS_C2}}": str(rmet["route_dais_to_chest2"]),
 "{{WALK_ENT}}": str(rmet["walk"]["ENTRANCE_IN"]), "{{WALK_DAIS}}": str(rmet["walk"]["DAIS_TOP_FRONT"]), "{{WALK_HATCH}}": str(rmet["walk"]["HATCH"]), "{{WALK_C2}}": str(rmet["walk"]["CHEST2_ACC0"]),
 "{{HAND_E}}": f"보조 상자 벽감 {hand_e['secondary_alcove_side']}쪽(남쪽을 향해 입장 시 {hand_e['seen_entering_facing_south']}손), 비밀 입구 구역 {hand_e['hatch_side']}쪽",
 "{{HAND_W}}": f"보조 상자 벽감 {hand_w['secondary_alcove_side']}쪽(남쪽을 향해 입장 시 {hand_w['seen_entering_facing_south']}손), 비밀 입구 구역 {hand_w['hatch_side']}쪽",
 "{{EQ}}": ", ".join(f"{k}={v}" for k, v in eq["voxel_mirror"].items()), "{{EQV}}": str(eq["verdicts_equal"]),
 "{{WF_BASE}}": str(WF["baseline_over40"]), "{{WF_A}}": f"동 {WF['variantA_east']['passes']}/{WF['variantA_east']['total']} PASS, 서 {WF['variantA_west']['passes']}/{WF['variantA_west']['total']} PASS, P3 홀 {WF['variantA_p3_hall']['passes']} PASS/{len(WF['variantA_p3_hall']['fails'])} FAIL",
 "{{WF_EXT_BASE}}": str(WF["baseline_east_wall_extent"]["x"]["max"]), "{{WF_EXT_A}}": str(WF["variantA_wall_extent"]["x"]["max"]),
}
for k, v in rep.items(): doc = doc.replace(k, v)
assert "{{" not in doc, [x for x in doc.split("{{")[1:3]]
open(os.path.join(dest, "ST01_P3_ROYAL_HALL_3D_DESIGN_2026-11-06.md"), "w").write(doc)
h4 = open(os.path.join(HERE, "p4_handoff_template.md")).read()
for k, v in rep.items(): h4 = h4.replace(k, v)
assert "{{" not in h4
open(os.path.join(dest, "ST01_P4_SECRET_AREA_HANDOFF_2026-11-06.md"), "w").write(h4)
print("written", len(doc), "+", len(h4), "bytes; east", cnt(RE, "PASS"), "PASS", cnt(RE, "FAIL"), "FAIL; west", cnt(RW, "PASS"), "PASS", cnt(RW, "FAIL"), "FAIL; mutations", nm_det, "/", nm)

#!/usr/bin/env python3
"""Builds docs/design/st01-p5/ from the verified integrated model. Usage: python3 -I build_p5_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p5", "st01-p4", "st01-p3", "st01-p2"): sys.path.insert(0, os.path.join(TOOLS, d))
import p2f1_variants as PV, st01_p2_verify as P2V
import p4_spec as S4
from st01_p2_model_east import make_east, mirror_model
root = sys.argv[1]; dest = os.path.join(root, "docs/design/st01-p5"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen)
for cmd in ([ "st01_p5_verify.py", gen], ["st01_p5_mutation_tests.py", "--out", os.path.join(gen, "p5_mutation_results.json")]):
    rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, cmd[0])] + cmd[1:], capture_output=True, text=True); assert rc.returncode == 0, rc.stdout[-1500:] + rc.stderr[-1500:]
J = json.load(open(os.path.join(gen, "p5_verification.json"))); M = json.load(open(os.path.join(gen, "p5_mutation_results.json")))
def cnt(r, v): return sum(c["verdict"] == v for c in r["checks"])
def rd(p): return open(os.path.join(gen, "plans", p)).read().rstrip("\n")
# P2-F1 comparison
pe = make_east(); rows = []
for nm, f, desc in (("기준 P2", lambda x: x, "변경 없음"), ("변형 A (미채택)", PV.variant_A, "V12·LND12·FA 동쪽 2열 제거 → 참 7×9"), ("후보 C (신규, 미채택)", PV.variant_C, "전이부(V12·FA·LND12·FB) 서쪽 2칸 이동, C1·C2 각 2칸 단축, BRB1 재배치 → 참 9×9 유지")):
    v = f(pe); out = []
    for sd, m in (("동", v), ("서", mirror_model(v))):
        R, G = P2V.run_side(m); out.append(f"{sum(c['verdict']=='PASS' for c in R['checks'])}/{len(R['checks'])}")
        rt = R["route_distance"]["MAIN:PORT"]
    lnd = v["spaces"]["LND12"]["boxes"][0]; v12 = v["spaces"]["V12"]["boxes"][0]
    over = sorted({k for k, s in v["spaces"].items() for b in s["boxes"] if max(abs(b[0]) + 2, abs(b[1]) + 2, abs(b[4]) + 2, abs(b[5]) + 2) > 40})
    mx = max(b[1] for s in v["spaces"].values() for b in s["boxes"]) + 2
    rows.append(f"| {nm} | {desc} | 동 {out[0]} / 서 {out[1]} | {lnd[1]-lnd[0]+1}×{lnd[5]-lnd[4]+1} | {v12[1]-v12[0]+1}×{v12[5]-v12[4]+1} | {mx} | {', '.join(over) or '없음'} | {rt} | 0 |")
f1_tbl = "\n".join(rows)
# Q97 footprints (B and A), base layout
def fp(boxes): return (min(b[0] for b in boxes), max(b[1] for b in boxes), min(b[4] for b in boxes), max(b[5] for b in boxes))
def lvl(m, names): return fp([b for k in names for b in m["spaces"][k]["boxes"]])
L1 = lvl(pe, ["C1", "BR1", "T1", "BR2", "T2", "BR3", "T3", "V12"]); L2 = lvl(pe, ["C2", "BRB1", "B1", "BRB2", "B2", "BRB3", "B3", "V23"]); L3 = lvl(pe, ["C3", "R", "A", "T"])
HALL = (-40, -2, 2, 40)
def cen(f): return ((f[0] + f[1]) / 2, (f[2] + f[3]) / 2)
def ovf(a, b): return max(0, min(a[1], b[1]) - max(a[0], b[0]) + 1) * max(0, min(a[3], b[3]) - max(a[2], b[2]) + 1) / ((b[1] - b[0] + 1) * (b[3] - b[2] + 1))
def q97(var):
    s = S4.make_variant(var); v = s["envelopes"]["VAULT_ENV"]; c = s["envelopes"]["CRYPT_ENV"]
    chain = [("L1", L1), ("L2", L2), ("L3", L3), ("왕실 홀", HALL), ("비밀 묘실", (c[0], c[1], c[4], c[5])), ("보물창고", (v[0], v[1], v[4], v[5]))]
    out = []
    for (an, a), (bn, b) in zip(chain, chain[1:]):
        ca, cb = cen(a), cen(b); out.append(f"| {an} → {bn} | ({ca[0]:.1f},{ca[1]:.1f}) → ({cb[0]:.1f},{cb[1]:.1f}) | {((ca[0]-cb[0])**2+(ca[1]-cb[1])**2)**.5:.1f} | {ovf(a, b)*100:.0f}% |")
    return "\n".join(out)
mut = "\n".join(f"| {m['mutation']} | {', '.join(m['expected'])} | {', '.join(m['failed'])} | {'검출' if m['detected'] else '**미검출**'} |" for m in M["results"])
def met(k, sd="east", c="B|base"): return J[c][sd]["metrics"][k]
def vt(c):
    return "\n".join(f"| {a['id']} | {a['title']} | " + " | ".join(J[k][s]["checks"][i]["verdict"] for k in ("B|base", "B|C", "A|base", "A|C") for s in ("east", "west")) + " |" for i, a in enumerate(J["B|base"]["east"]["checks"]))
ev = "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in J["B|C"]["east"]["checks"])
evb = "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in J["B|base"]["east"]["checks"] if c["id"] in ("X1", "L1"))
stB = met("stair"); stA = met("stair", c="A|base")
rep = {"{{F1}}": f1_tbl, "{{Q97B}}": q97("B"), "{{Q97A}}": q97("A"), "{{MUT}}": mut, "{{NMUT}}": str(len(M["results"])), "{{NDET}}": str(sum(m["detected"] for m in M["results"])),
 "{{VERD}}": vt(None), "{{EVID_C}}": ev, "{{EVID_BASE}}": evb, "{{STAIR_B}}": ", ".join(map(str, stB["levels"])), "{{STAIR_A}}": ", ".join(map(str, stA["levels"])),
 "{{CH_B}}": str(met("chain_steps")), "{{CH_A}}": str(met("chain_steps", c="A|base")), "{{CH_BC}}": str(met("chain_steps", c="B|C")),
 "{{PLAN30}}": rd("B_C_east/plan_Y-30_secret.txt"), "{{SEC35}}": rd("B_C_east/section_z35.txt"), "{{SEC26}}": rd("B_C_east/section_z26.txt")}
for n in ("B|base", "B|C", "A|base", "A|C"):
    for sd in ("east", "west"):
        k = "{{" + n.replace("|", "_") + "_" + sd[0].upper() + "}}"
        rep[k] = f"{cnt(J[n][sd],'PASS')} PASS / {cnt(J[n][sd],'FAIL')} FAIL / {cnt(J[n][sd],'UNVERIFIED')} UNVERIFIED"
rep["{{MIRROR}}"] = "; ".join(f"{k}: " + ",".join(f"{a}={b}" for a, b in J[k]["mirror"].items()) for k in J)
d = open(os.path.join(HERE, "p5_report_template.md")).read()
for k, v in rep.items(): d = d.replace(k, v)
assert "{{" not in d, d[d.index("{{"):d.index("{{") + 40]
open(os.path.join(dest, "ST01_P5_INTEGRATION_VERIFICATION_2026-11-07.md"), "w").write(d)
print("ok", len(d))

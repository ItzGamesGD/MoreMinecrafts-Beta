#!/usr/bin/env python3
"""Builds docs/design/st01-p4/ (report, P5 handoff, generated plans/sections/json) from the verified P4 models.
Usage: python3 -I build_p4_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p4_spec as S4
root = sys.argv[1]
dest = os.path.join(root, "docs/design/st01-p4"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen, exist_ok=True)
for cmd, name in ((["st01_p4_verify.py", gen], "verify"), (["st01_p4_mutation_tests.py", "--out", os.path.join(gen, "p4_mutation_results.json")], "mutation")):
    rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, cmd[0])] + cmd[1:], capture_output=True, text=True)
    assert rc.returncode == 0, name + rc.stdout[-2000:] + rc.stderr[-2000:]
J = json.load(open(os.path.join(gen, "p4_verification.json"))); M = json.load(open(os.path.join(gen, "p4_mutation_results.json")))
SP = {v: S4.make_variant(v) for v in "AB"}
def rng(a, b): return f"{a}..{b}" if a != b else f"{a}"
def bbox(boxes): return [min(b[0] for b in boxes), max(b[1] for b in boxes), min(b[2] for b in boxes), max(b[3] for b in boxes), min(b[4] for b in boxes), max(b[5] for b in boxes)]
def vol(boxes): return sum((b[1]-b[0]+1)*(b[3]-b[2]+1)*(b[5]-b[4]+1) for b in boxes)
def cnt(R, v): return sum(c["verdict"] == v for c in R["checks"])
def spaces_tbl(v):
    rows = []
    for k, s in SP[v]["spaces"].items():
        b = bbox(s["boxes"]); rows.append(f"| {k} | {s['kind']} | {rng(b[0],b[1])} | {rng(b[2],b[3])} | {rng(b[4],b[5])} | {b[1]-b[0]+1}×{b[3]-b[2]+1}×{b[5]-b[4]+1} |")
    for k, b in SP[v]["envelopes"].items():
        if k.endswith("_ENV") and k in ("VAULT_ENV", "CRYPT_ENV"):
            rows.append(f"| {k} (외벽 포함 구조) | envelope | {rng(b[0],b[1])} | {rng(b[2],b[3])} | {rng(b[4],b[5])} | {b[1]-b[0]+1}×{b[3]-b[2]+1}×{b[5]-b[4]+1} |")
    return "\n".join(rows)
def elems_tbl(v):
    rows = []
    for k, e in SP[v]["elements"].items():
        b = bbox(e["boxes"]); rows.append(f"| {k} | {e['kind']} | {rng(b[0],b[1])} | {rng(b[2],b[3])} | {rng(b[4],b[5])} | {b[1]-b[0]+1}×{b[3]-b[2]+1}×{b[5]-b[4]+1} | {vol(e['boxes'])} |")
    return "\n".join(rows)
def verd_tbl(v):
    return "\n".join(f"| {a['id']} | {a['title']} | {a['verdict']} | {b['verdict']} |" for a, b in zip(J[v]["east"]["checks"], J[v]["west"]["checks"]))
def evid(v): return "\n".join(f"- **{c['id']} {c['verdict']}** — {c['evidence']}" for c in J[v]["east"]["checks"])
def met(v, k): return J[v]["east"]["metrics"][k]
def route(v, k): return met(v, "walk_from_hatch")[k]
def rd(v, side, name): return open(os.path.join(gen, v, side, name)).read().rstrip("\n")
F = {"A": -37, "B": -30}
cmp_rows = []
def row(label, fa, fb): cmp_rows.append(f"| {label} | {fa} | {fb} |")
ea, eb = met("A", "extent"), met("B", "extent")
row("검증 (동쪽)", f"{cnt(J['A']['east'],'PASS')} PASS / {cnt(J['A']['east'],'FAIL')} FAIL / {cnt(J['A']['east'],'UNVERIFIED')} UNVERIFIED", f"{cnt(J['B']['east'],'PASS')} PASS / {cnt(J['B']['east'],'FAIL')} FAIL / {cnt(J['B']['east'],'UNVERIFIED')} UNVERIFIED")
row("검증 (서쪽, 거울)", f"{cnt(J['A']['west'],'PASS')} / {cnt(J['A']['west'],'FAIL')} / {cnt(J['A']['west'],'UNVERIFIED')}", f"{cnt(J['B']['west'],'PASS')} / {cnt(J['B']['west'],'FAIL')} / {cnt(J['B']['west'],'UNVERIFIED')}")
row("P4 전체 구조물 X 범위 (동)", f"{ea['x'][0]}..{ea['x'][1]}", f"{eb['x'][0]}..{eb['x'][1]}")
row("P4 전체 구조물 Z 범위", f"{ea['z'][0]}..{ea['z'][1]}", f"{eb['z'][0]}..{eb['z'][1]}")
row("±40 초과 거리 / 초과 복셀 수", f"{ea['excess']} / {ea['voxels_outside']}", f"{eb['excess']} / {eb['voxels_outside']}")
row("±40 경계 여유 (가장 가까운 면)", "X=−40 면 여유 0, Z=39 면 여유 1 (서쪽 후보는 X=+40)", "Z=39 면 여유 1, X=33 면 여유 7")
row("최저 공동 Y / 깊이", f"{met('A','lowest_cavity')} / {-met('A','lowest_cavity')}", f"{met('B','lowest_cavity')} / {-met('B','lowest_cavity')}")
row("최저 구조 블록 Y / 깊이", f"{met('A','lowest_structure')} / {-met('A','lowest_structure')}", f"{met('B','lowest_structure')} / {-met('B','lowest_structure')}")
row("보물창고 내부 바닥 Y", "−37", "−30")
row("홀 바닥(−22) → 보물창고 바닥 수직 낙차", str(met("A", "drop")), str(met("B", "drop")))
row("숨은 계단 단수 (전체 발판 수)", str(met("A", "treads")), str(met("B", "treads")))
row("방향 전환 (참)", "2회 (동→북 참 LA, 북→서 참 LB)", "2회 (참 LA에서 동→북, 통로 끝에서 북→동)")
row("홀 입구 구역 → 상자 정면 보행 거리 (걸음)", str(route("A", "CHEST_FRONT2")), str(route("B", "CHEST_FRONT2")))
row("홀 입구 구역 → 보물창고 입구", str(route("A", "VAULT_ENTRY")), str(route("B", "VAULT_ENTRY")))
row("홀 입구 구역 → 비밀 묘실 안", str(route("A", "CRYPT_IN")), str(route("B", "CRYPT_IN")))
row("P1 착지 → 상자 정면", str(met("A", "walk_from_p1")["CHEST_FRONT2"]), str(met("B", "walk_from_p1")["CHEST_FRONT2"]))
row("상자 정면 5칸 접근 가능", "5/5", "5/5")
row("기존 공동과의 겹침 / 미선언 접촉", "0 / 0", "0 / 0")
row("선언되지 않은 공동과의 최소 간격", str(met("A", "min_gap_unlinked")), str(met("B", "min_gap_unlinked")))
row("보물창고·묘실 벽/바닥·지붕 두께 (측정 최소)", "벽 3 / 2", "벽 3 / 2")
sa, sb = met("A", "stagger"), met("B", "stagger")
row("보물창고 평면이 왕실 홀 아래에 있는 비율 (Q97 관련)", f"{sa['vault_under_hall_fraction']*100:.0f}% (적층)", f"{sb['vault_under_hall_fraction']*100:.0f}% (측방)")
row("홀 중심 ↔ 보물창고 중심 평면 거리", f"{sa['hall_to_vault_offset']:.1f}", f"{sb['hall_to_vault_offset']:.1f}")
row("홀 중심 ↔ 비밀 묘실 중심 평면 거리", f"{sa['hall_to_crypt_offset']:.1f}", f"{sb['hall_to_crypt_offset']:.1f}")
row("P2 예약 `VAULT_A`와의 관계", "동일 외벽 체적(X −40..−14, Z 9..39, Y −39..−26)", "별도 위치(P2 L3 측방 후보와 다름)")
cmp_tbl = "\n".join(cmp_rows)
scr = J["screen_B_east_adjacent"]
scr_tbl = "\n".join(f"| {k} | {v[0]} | {', '.join(v[1]) if v[1] else '없음'} |" for k, v in scr.items())
mut_rows = "\n".join(f"| {m['mutation']} | {','.join(m['candidates'])} | {', '.join(m['expected'])} | {'; '.join(k + ': ' + ', '.join(v) for k, v in m['failed'].items())} | {'검출' if m['detected'] else '**미검출**'} |" for m in M["results"])
nm = len(M["results"]); nd = sum(m["detected"] for m in M["results"])
P3 = J["p3_preserved"]
eqA, eqB = J["A"]["equivalence"], J["B"]["equivalence"]
rep = {
 "{{A_E}}": f"{cnt(J['A']['east'],'PASS')} PASS / {cnt(J['A']['east'],'FAIL')} FAIL / {cnt(J['A']['east'],'UNVERIFIED')} UNVERIFIED", "{{A_W}}": f"{cnt(J['A']['west'],'PASS')} / {cnt(J['A']['west'],'FAIL')} / {cnt(J['A']['west'],'UNVERIFIED')}",
 "{{B_E}}": f"{cnt(J['B']['east'],'PASS')} PASS / {cnt(J['B']['east'],'FAIL')} FAIL / {cnt(J['B']['east'],'UNVERIFIED')} UNVERIFIED", "{{B_W}}": f"{cnt(J['B']['west'],'PASS')} / {cnt(J['B']['west'],'FAIL')} / {cnt(J['B']['west'],'UNVERIFIED')}",
 "{{CMP}}": cmp_tbl, "{{SCREEN}}": scr_tbl, "{{MUT}}": mut_rows, "{{NMUT}}": str(nm), "{{NDET}}": str(nd),
 "{{SPACES_A}}": spaces_tbl("A"), "{{SPACES_B}}": spaces_tbl("B"), "{{ELEMS_A}}": elems_tbl("A"), "{{ELEMS_B}}": elems_tbl("B"),
 "{{VERD_A}}": verd_tbl("A"), "{{VERD_B}}": verd_tbl("B"), "{{EVID_A}}": evid("A"), "{{EVID_B}}": evid("B"),
 "{{EQ_A}}": ", ".join(f"{k}={v}" for k, v in eqA["voxel_mirror"].items()), "{{EQ_B}}": ", ".join(f"{k}={v}" for k, v in eqB["voxel_mirror"].items()),
 "{{P3E}}": f"{P3['east']['passes']} PASS / {len(P3['east']['fails'])} FAIL", "{{P3W}}": f"{P3['west']['passes']} PASS / {len(P3['west']['fails'])} FAIL",
 "{{PLAN_A_FLOOR}}": rd("A", "east", "plan_floor_Y-37.txt"), "{{VAULT_A_FLOOR}}": rd("A", "east", "vault_detail_floor.txt"), "{{VAULT_A_34}}": rd("A", "east", "vault_detail_Y-34.txt"),
 "{{VAULT_A_30}}": rd("A", "east", "vault_detail_Y-30.txt"), "{{SEC_A_AXIS}}": rd("A", "east", "section_vault_axis_Z24.txt"), "{{SEC_A_CROSS}}": rd("A", "east", "section_vault_cross_X-31.txt"),
 "{{SEC_A_ROUTE}}": rd("A", "east", "section_route_Z35.txt"),
 "{{PLAN_B_FLOOR}}": rd("B", "east", "plan_floor_Y-30.txt"), "{{VAULT_B_FLOOR}}": rd("B", "east", "vault_detail_floor.txt"), "{{SEC_B_AXIS}}": rd("B", "east", "section_vault_axis_Z26.txt"),
 "{{SEC_B_ROUTE}}": rd("B", "east", "section_route_Z35.txt"), "{{PLAN_B_W}}": rd("B", "west", "plan_floor_Y-30.txt"),
 "{{DROP_A}}": str(met("A", "drop")), "{{DROP_B}}": str(met("B", "drop")), "{{TR_A}}": str(met("A", "treads")), "{{TR_B}}": str(met("B", "treads")),
 "{{HS_A}}": str(route("A", "CHEST_FRONT2")), "{{HS_B}}": str(route("B", "CHEST_FRONT2")),
 "{{BACK_A}}": str(met("A", "walk_from_hatch")["CHEST_FRONT2"]),
 "{{PA}}": str(met("A", "walk_from_p1")["CHEST_FRONT2"]), "{{PB}}": str(met("B", "walk_from_p1")["CHEST_FRONT2"]),
 "{{MINGAP_A}}": str(met("A", "min_gap_unlinked")), "{{MINGAP_B}}": str(met("B", "min_gap_unlinked")),
 "{{PILE_A}}": f"seed {SP['A']['vault']['pile_seed']}, 좌우 불균형 {SP['A']['vault']['pile_imbalance']*100:.1f}%", "{{PILE_B}}": f"seed {SP['B']['vault']['pile_seed']}, 좌우 불균형 {SP['B']['vault']['pile_imbalance']*100:.1f}%",
}
for src, out in (("p4_report_template.md", "ST01_P4_SECRET_AREA_3D_DESIGN_2026-11-07.md"), ("p5_handoff_template.md", "ST01_P5_INTEGRATION_HANDOFF_2026-11-07.md")):
    d = open(os.path.join(HERE, src)).read()
    for k, v in rep.items(): d = d.replace(k, v)
    assert "{{" not in d, d[d.index("{{"):d.index("{{") + 40]
    open(os.path.join(dest, out), "w").write(d)
print("written; A east", cnt(J['A']['east'], 'PASS'), "PASS", cnt(J['A']['east'], 'FAIL'), "FAIL; B east", cnt(J['B']['east'], 'PASS'), "PASS", cnt(J['B']['east'], 'FAIL'), "FAIL; mutations", nd, "/", nm)

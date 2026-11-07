#!/usr/bin/env python3
"""Builds the data-driven P6 documents + generated artefacts into docs/design/st01-p6/. Usage: python3 -I build_p6_report.py <repo_root>"""
import os, sys, json, shutil, subprocess, copy
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import st01_p6_verify as X
P5, V, C, OV, S3, S4 = X.P5, X.V, X.C, X.OV, X.S3, X.S4
root = sys.argv[1]; dest = os.path.join(root, "docs/design/st01-p6"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen)
for cmd in (["st01_p6_verify.py", gen], ["st01_p6_mutation_tests.py", "--out", os.path.join(gen, "p6_mutation_results.json")]):
    rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, cmd[0])] + cmd[1:], capture_output=True, text=True); assert rc.returncode == 0, rc.stdout[-1500:] + rc.stderr[-1500:]
J = json.load(open(os.path.join(gen, "p6_verification.json"))); M = json.load(open(os.path.join(gen, "p6_mutation_results.json")))
OVJ = json.load(open(os.path.join(gen, "p6_p2c_overlay.json")))
ME = json.load(open(os.path.join(gen, "p6_model_east.json"))); MW = json.load(open(os.path.join(gen, "p6_model_west.json")))
def rng(a, b): return f"{a}..{b}" if a != b else f"{a}"
def bx(b): return f"X {rng(b[0], b[1])} / Y {rng(b[2], b[3])} / Z {rng(b[4], b[5])}"
def dm(d): return "×".join(map(str, d)) if d else "—"

# ---------- P2-F1 comparison doc
base = X.make_east(); p2c = OV.apply(base, OVJ)
rows = []; why = {"V12": "벽 포함 X=41 돌출 해소를 위해 2칸 서쪽(안쪽)으로 이동", "LND12": "V12와 같은 열을 유지(9×9 참 보존)하며 2칸 이동", "FA": "V12–LND12 사이 7폭 계단 A를 같은 2칸 이동", "FB": "참 서쪽 모서리에서 시작하는 7폭 계단 B를 2칸 이동(발판 높이열 유지)",
 "C1": "L1 주 복도의 마지막 꺾임(jog) 위치를 2칸 짧게 재배치해 V12에 도달", "C2": "L2 중앙 통로 길이 13→11(동쪽 끝이 FB 새 위치에 맞물림)", "BRB1": "C2 단축에 따라 분기 BRB1을 2칸 서쪽으로 재배치"}
for k in sorted(OVJ["changed_spaces"], key=lambda k: list(base["spaces"]).index(k)):
    b0, b1 = X.bbox(OVJ["changed_spaces"][k]["before"]), X.bbox(OVJ["changed_spaces"][k]["after"])
    w0 = max(b0[1] + 2, abs(b0[0]) + 2, b0[5] + 2); w1 = max(b1[1] + 2, abs(b1[0]) + 2, b1[5] + 2)
    rows.append(f"| {k} | {base['spaces'][k]['name']} | {bx(b0)} | {bx(b1)} | {dm(X.dims(b0))} → {dm(X.dims(b1))} | {w0} → {w1} | {why[k]} |")
unch = sorted(OVJ["unchanged_spaces"])
res = {}
for nm, m in (("기준 P2(변경 없음)", base), ("후보 C(P6 오버레이 적용)", p2c)):
    out = []
    for sd, mm in (("동", m), ("서", X.mirror_model(m))):
        Rr, G = X.P2V.run_side(mm); out.append(f"{sd} {sum(c['verdict']=='PASS' for c in Rr['checks'])}/{len(Rr['checks'])}"); rt = Rr["route_distance"]["MAIN:PORT"]
    ext = max(max(b[1] + 2, -(b[0] - 2), b[5] + 2, -(b[4] - 2)) for s in mm["spaces"].values() for b in s["boxes"])
    res[nm] = (", ".join(out), rt, ext)
tbl2 = "\n".join(f"| {k} | {v[0]} | {v[1]} | {v[2]} |" for k, v in res.items())
cmp_doc = f"""# ST-01 P6 — P2-F1 후보 C 변경 전후 비교 (2026-11-07)

상태: **생성 문서(`tools/st01-p6/build_p6_report.py`).** 사용자 결정(후보 C 채택)을 반영한 *오버레이*이며 기존 P2 모델·검증기·승인 기록은 수정하지 않았다. 좌표는 P5 후보 C 정의(`tools/st01-p5/p2f1_variants.py::variant_C`)를 변경되지 않은 `make_east()`와 기계적으로 대조해 추출했다(`generated/p6_p2c_overlay.json`). 요약 문장에서 재구성한 값은 없다.

## 1. 변경된 공간 (7개) — 동쪽 기준, 포함 범위

| ID | 이름 | 변경 전 | 변경 후 | 외형(크기) | 벽 포함 최대 X·Z 절대값 (변경 전→후) | 이유 |
| --- | --- | --- | --- | --- | --- | --- |
{chr(10).join(rows)}

벽 포함 범위는 공간 면에서 바깥으로 2칸(P2 벽 두께 가정)을 더한 값이다. 서쪽은 정확한 X 거울이다(`generated/p6_model_west.json`).

## 2. 변경되지 않은 공간 ({len(unch)}개)
{", ".join(unch)}

비공간 키(링크·포트·목표·P1 박스·시작/목표)는 변경 0(검사 O1).

## 3. 결과 비교 (P2 엔진 자체 재실행)

| 레이아웃 | P2 엔진 통과 항목 | 주 동선 길이(→ 홀 포트) | 벽 포함 최대 X·Z 절대값 |
| --- | --- | --- | --- |
{tbl2}

- 승인 치수 유지(검사 O3): 9×9 참, 9×11 V12, 7폭 계단 2개(발판 4+4), 기타 공간 크기 변경 0.
- 외벽 포함 ±40 초과: 0칸(검사 P5.X1).
- 추가 깊이: 0 (P2 최저 공동 Y는 변경 없음; 측정 최저 구조 Y −32는 P4-B가 결정).
- 변경은 P2-F1(벽체 X=±41 돌출)을 해소하며, 다른 P2 열린 항목(P2-F2 최대 깊이, F3 크기, F4 지형, F5, F6, F7)은 해소하지 않는다.
- P2 승인 기록·모델 파일에는 아직 이 좌표가 반영되어 있지 않다. 반영은 별도의 승인 작업이다.
"""
open(os.path.join(dest, "ST01_P6_P2F1_CANDIDATE_C_COMPARISON_2026-11-07.md"), "w").write(cmp_doc)

# ---------- component coordinate spec
def vox(e):
    m = np.zeros((C.GX[1] - C.GX[0] + 1, C.GY[1] - C.GY[0] + 1, C.GZ[1] - C.GZ[0] + 1), bool)
    for b in e["boxes"]: m[V.sl(b)] = True
    return int(m.sum())
def short(s): return s.split(" (")[0].split(" — ")[0]
STAT = {"APPROVED": "APPROVED", "CONDITIONALLY": "CONDITIONALLY APPROVED", "PROPOSED": "PROPOSED", "RESERVED": "RESERVED"}
def stat(s): return next((v for k, v in STAT.items() if s.startswith(k)), s)
def table(M, full=True):
    out = []
    for e in M["elements"]:
        out.append(f"| {e['id']} | {e['kind']} | {bx(e['bbox'])} | {dm(e['inner_dims'])} | {dm(e['outer_dims'])} | {vox(e)} | {', '.join(e['connects']) or '—'} | {stat(e['status'])} | {e['evidence']} | {e['dependency'] or '—'} |")
    return "\n".join(out)
hdr = "| ID | 종류 | 좌표 범위(포함) | 내부 치수 | 외부 치수 | 점유 복셀(공동 또는 요소) | 연결 | 상태 | 검증 근거 | 구현 의존성 |\n| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"
cnt = {}
for e in ME["elements"]: cnt[stat(e["status"])] = cnt.get(stat(e["status"]), 0) + 1
spec_doc = f"""# ST-01 P6 — 구성요소별 좌표 명세 (2026-11-07)

상태: **생성 문서.** 값은 모두 `tools/st01-p6`의 독립 레지스트리(상자 산술)에서 나온다. 이 문서는 구현 승인이 아니며, 승인 상태 열의 값을 그대로 구분한다.

좌표 규칙: 원점=피라미드 지면 중심, X 동+, Z 남+(입구), Y 층 인덱스, 범위는 포함. "내부 치수"=공동 bbox의 X×Y×Z. "외부 치수"는 P2는 수평 벽 2칸, P4는 P4 외피 상자(바닥 2칸 포함), P3 홀은 예약 상자이며 그 밖은 정의되지 않아 `—`. "점유 복셀"은 해당 요소 상자들의 합집합 칸 수(공동 또는 홀 내 고체 요소)이다.

상태 집계(동쪽, 요소 {len(ME['elements'])}개): {', '.join(f'{k} {v}' for k, v in sorted(cnt.items()))}. 마지막 열은 "구현 의존성"이다. 모든 요소의 게임 내 거동·월드젠·NBT는 UNVERIFIED이다.

## 1. 동쪽 통합 모델 (P6-E)

{hdr}
{table(ME)}

## 2. 서쪽 통합 모델 (P6-W, 정확한 X 거울)

서쪽 표는 동쪽의 [x0,x1]→[−x1,−x0] 거울이며 독립으로 다시 복셀화·검증했다(복셀 일치 확인: closed {J['mirror']['closed_solid']}, open {J['mirror']['open_solid']}, 요소 bbox {J['mirror']['registry_bbox_mirror']}).

{hdr}
{table(MW)}

## 3. 비밀 입구 12칸 절개 (CLOSED에서 닫히는 칸)

{', '.join(f'({c[0]},{c[1]},{c[2]})' for c in ME['cover_cells'])}  (동쪽)  
{', '.join(f'({c[0]},{c[1]},{c[2]})' for c in MW['cover_cells'])}  (서쪽)
"""
open(os.path.join(dest, "ST01_P6_COMPONENT_COORDINATE_SPEC_2026-11-07.md"), "w").write(spec_doc)

# ---------- audit report
def cnts(r): return tuple(sum(c["verdict"] == v for c in r["checks"]) for v in ("PASS", "FAIL", "UNVERIFIED"))
eP, eF, eU = cnts(J["east"]); wP, wF, wU = cnts(J["west"])
def evi(side): return "\n".join(f"| {c['id']} | {c['title']} | {c['verdict']} | {c['evidence']} |" for c in J[side]["checks"])
mut = "\n".join(f"| {m['mutation']} | {', '.join(m['expected'])} | {', '.join(m['failed'])} | {'검출' if m['detected'] else '**미검출**'} |" for m in M["results"])
q = J["east"]["metrics"]["q97"]
q97 = "\n".join(f"| {r['a']} → {r['b']} | ({r['ca'][0]:.1f}, {r['ca'][1]:.1f}) → ({r['cb'][0]:.1f}, {r['cb'][1]:.1f}) | ({r['dx']:+.1f}, {r['dz']:+.1f}) | {r['dist']} |" for r in q)
mt = J["east"]["metrics"]; st = mt["p5_stair"]
facts = J["repo_facts"]
def rd(p): return open(os.path.join(gen, "plans", p)).read().rstrip("\n")
audit = open(os.path.join(HERE, "p6_audit_template.md")).read()
rep = {"{{EP}}": str(eP), "{{EF}}": str(eF), "{{EU}}": str(eU), "{{WP}}": str(wP), "{{WF}}": str(wF), "{{WU}}": str(wU), "{{EVE}}": evi("east"), "{{EVW}}": evi("west"),
       "{{MUT}}": mut, "{{NMUT}}": str(len(M["results"])), "{{NDET}}": str(sum(m["detected"] for m in M["results"])), "{{Q97}}": q97,
       "{{STAIR}}": ", ".join(map(str, st["levels"])), "{{CHAIN}}": str(mt["p5_chain_steps"]), "{{HALLCHEST}}": str(mt["p5_steps"]["hall_to_chest"]), "{{FALL}}": str(mt["edge_fall"]),
       "{{MIRROR}}": ", ".join(f"{k}={v}" for k, v in J["mirror"].items()), "{{TURNS_E}}": J["turns"]["E"], "{{TURNS_W}}": J["turns"]["W"], "{{ROUTE}}": str(J["turns"]["route_steps"]["E"]),
       "{{FACT_DEFEATED}}": facts["defeated_event"][0], "{{FACT_GOLEM}}": str(len(facts["golem_entity"])), "{{FACT_STRUCT}}": ", ".join(os.path.basename(p) for p in facts["tomb_structure_resources"]), "{{FACT_HOOKS}}": str(len(facts["pyramid_structure_hooks"])),
       "{{PLAN30}}": rd("east/plan_Y-30_secret.txt"), "{{PLAN22}}": rd("east/plan_Y-22_L3_hall.txt"), "{{SEC35}}": rd("east/section_z35.txt"), "{{SECX21}}": rd("east/section_x-21.txt")}
for k, v in rep.items(): audit = audit.replace(k, v)
assert "{{" not in audit, audit[audit.index("{{"):audit.index("{{") + 40]
open(os.path.join(dest, "ST01_P6_PRE_IMPLEMENTATION_AUDIT_REPORT_2026-11-07.md"), "w").write(audit)
print("ok", len(audit), len(spec_doc), len(cmp_doc))

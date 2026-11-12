#!/usr/bin/env python3
"""Builds the data-driven P7 documents + generated artefacts into docs/design/st01-p7/. Usage: python3 -I build_p7_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import p7_statemachine as M
root = sys.argv[1]; dest = os.path.join(root, "docs/design/st01-p7"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen)
for cmd in (["st01_p7_verify.py", gen], ["st01_p7_mutation_tests.py", "--out", os.path.join(gen, "p7_mutation_results.json")]):
    rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, cmd[0])] + cmd[1:], capture_output=True, text=True); assert rc.returncode == 0, rc.stdout[-1500:] + rc.stderr[-1500:]
J = json.load(open(os.path.join(gen, "p7_verification.json"))); MU = json.load(open(os.path.join(gen, "p7_mutation_results.json")))
ch = J["checks"]; ev = {(c["side"], c["id"]): c for c in ch}
cnt = lambda v: sum(c["verdict"] == v for c in ch)
E = J["metrics"]["east"]; W = J["metrics"]["west"]
rowsG = E["ga1"]
fall_tbl = "\n".join(f"| ({q['col'][0]}, {q['col'][1]}) | {q['landing_y']} | {q['fall']} | {max(0, q['fall'] - 3)} | {q['headroom']} | {'예' if q['climb_out_to_hall'] else '아니오'} | {'예' if q['reach_vault'] else '아니오'} |" for q in rowsG)
zones = "\n".join(f"| {k} | {v['cells']} | {v['min_dist_to_cover']} | {v['r_safe_w1']} | {v['r_safe_w2']} |" for k, v in E["zones"].items())
routes = {}
for k, v in E["route_len"].items():
    a, b = k.split("->"); routes.setdefault(a, {})[b] = v
rt = "\n".join(f"| {a} | " + " | ".join(str(routes[a].get(z, '—')) for z in E['zones']) + " |" for a in routes)
zh = " | ".join(E["zones"])
sens = "\n".join(f"| {k} | " + ", ".join(f"{a}={b}" for a, b in v.items()) + " |" for k, v in E["agent_sensitivity"].items())
Lmax = E["Lmax"]
sizing = "\n".join(f"| {sp} | {t} | {t/20:.0f} s |" for sp, t in J["fallback_sizing_illustrative"].items())
trans = "\n".join(f"| {a} | {e} | {b} |" for a, e, b in J["transition_table"])
scn = []
for k, steps in J["scenarios"].items():
    seq = " → ".join(e for e, _ in steps[1:]); fin = steps[-1][1]
    scn.append(f"| {k} | {seq} | {fin['rec']} | {'적용' if fin['cover'] else '닫힘'} | {fin['ngolem']} |")
scn = "\n".join(scn)
mut = "\n".join(f"| {m['kind']} | {m['mutation']} | {', '.join(m['expected'])} | {', '.join(m['failed'])} | {'검출' if m['detected'] else '**미검출**'} |" for m in MU["results"])
evid = "\n".join(f"| {c['side']} | {c['id']} | {c['title']} | {c['verdict']} | {c['evidence']} |" for c in ch)
off = E["cover_offsets"]; anchor = E["anchor"]

def w(name, txt): open(os.path.join(dest, name), "w").write(txt)

w("ST01_P7_STATE_TRANSITION_SPEC_2026-11-07.md", f"""# ST-01 P7 — 상태 전이 명세 (2026-11-07)

상태: **설계 명세 / 구현 승인 아님.** 참조 모델은 `tools/st01-p7/p7_statemachine.py`(순수 Python, 게임 코드 아님). 대기 시간은 추상 단위 `D`(기호 값, 1~4로 모두 검증)이며 실제 값은 미승인이다.

## 1. 상태

| 상태 | 의미 | 영속 |
| --- | --- | --- |
| SEALED | 보스 처치 전. 덮개 닫힘. 기록이 없는 것과 동일 | 기록 없음 허용 |
| GUARDIAN_DEFEATED | 처치 확인(보스 영속 기록이 근거). 골렘 생성 요청 대기 또는 생성 실패 후 대기 | 예 |
| GOLEM_GUIDING | 금 골렘 안내 중 | 예 |
| ENTRANCE_OPEN | 골렘이 도착 구역에 도달해 개방 | 예(종단) |
| FALLBACK_OPEN | 제한 시간 만료로 개방 | 예(종단) |

종단 상태는 되돌리지 않는다(복원 규칙 미확정 → 자동 복원 로직을 확정하지 않음). 개방 상태의 *물리적* 덮개 제거는 상태에서 파생되는 멱등 조정(reconcile)이며 청크가 로드될 때 적용된다.

## 2. 상태 전이도

```
                       (보스 영속 기록 = 처치됨 에서만)
 SEALED ──처치 확인──▶ GUARDIAN_DEFEATED ──골렘 생성 성공──▶ GOLEM_GUIDING ──도착 구역 도달──▶ ENTRANCE_OPEN
                              │                                   │
                              │ 생성 실패 / 정지 / 소실 / 경로 실패   │
                              └──────── 제한 시간 만료 ────────────┴──────────────▶ FALLBACK_OPEN
 (보스 처치 전에는 어떤 이벤트·타이머·요청도 SEALED를 벗어나지 못함)
```

모델이 도달 가능한 상태 {J['metrics']['model']['states']}개, 간선 {J['metrics']['model']['edges']}개를 전수 탐색해 얻은 **기록 상태 전이** 전부:

| 이전 | 이벤트 | 이후 |
| --- | --- | --- |
{trans}

그 외 이벤트(중복 처치 이벤트, 중복 개방 요청, 골렘 소실, 재시작, 청크 언로드, 플레이어 이탈 등)는 기록 상태를 바꾸지 않는다.

## 3. 핵심 불변식 (전수 탐색, D=1..4)

| ID | 불변식 |
| --- | --- |
| I1 | 보스 처치 전에는 기록·덮개 어느 쪽도 열리지 않는다(잘못된/오래된/타 무덤 처치 이벤트, 상자 조기 접촉, 타이머, 개방 요청 포함) |
| I2 | 기록은 앞으로만 간다. 덮개는 복원되지 않는다 |
| I3 | 처치 이벤트·도착·개방 요청·조기 상자 접촉·경로 실패의 중복은 멱등이다 |
| I4 | 재시작은 영속 4항목(보스 처치 기록, 입구 상태, 남은 시간, 덮개 적용)을 보존하고 남은 시간을 잃지 않는다 |
| I5 | 골렘은 무덤당 최대 1마리 |
| I6 | P7 이벤트는 보스 처치 조건(=상자 봉인 해제/보상 조건)을 바꾸지 않는다. 대체 개방은 보상 조건을 우회하지 않는다 |
| I7 | (활성도) 보스가 처치된 모든 도달 가능 상태에서 *이로운 사건만*(청크 로드, 시간 경과, 플레이어 접속) 있으면 제한 시간 안에 덮개가 열린다 — 골렘 상태와 무관 |
| I8 | 덮개는 열림 기록이 있을 때만 제거된다 |
| I9 | 개방이 플레이어를 매몰시키지 않는다 |

## 4. 정상·실패 시나리오 (스크립트 실행 결과)

| 시나리오 | 이벤트 열 | 최종 기록 | 덮개 | 골렘 |
| --- | --- | --- | --- | --- |
{scn}

시나리오 해설: F1 생성 실패, F2 경로탐색 실패(정지), F3 소실·사망, F4 안내 중 서버 재시작, F5 만료 시 청크 언로드 → 재로드에서 적용, F6 처치 이벤트 유실(리스너 실패) → 보스 영속 기록으로부터 복구, E3 처치 전 상자 접촉은 입구에 영향 없음, E4 덮개 위 점유 중 개방(§ 덮개 안전성), E5 모든 플레이어 이탈 중에도 타이머가 개방, E6 만료 후 도착은 무효.

## 5. 설계 결정 사항 (제안; 승인 아님)

1. 타이머는 **GUARDIAN_DEFEATED 수락 시점**에 시작한다(생성 실패·소실·정지가 타이머를 취소하지 않음). 안내 시작 시점 기준과 비교는 열린 결정 O-3.
2. 시간 경과는 게임 시간(서버 가동 중에만 진행)이며 플레이어 존재·청크 로드와 무관하게 계산한다. 물리적 제거는 청크가 로드되는 첫 틱에 멱등하게 적용한다.
3. 도착 판정과 타이머가 같은 틱에 경합하면 먼저 기록된 쪽이 이긴다(기록은 한 번만 쓰인다). 만료 후 도착은 무시한다.
4. 처치 이벤트는 **신호**일 뿐이다. 개방 판단은 항상 보스 영속 기록(`isDefeated`)을 다시 읽어 결정한다.
""")

w("ST01_P7_BOSS_CODE_CONNECTIONS_2026-11-07.md", """# ST-01 P7 — 기존 보스 코드 연결점과 제안 인터페이스 (2026-11-07)

상태: **읽기 전용 조사 + 인터페이스 제안.** 저장소 코드는 수정하지 않았다. "제안" 항목은 **아직 존재하지 않으며 구현된 것으로 표현하지 않는다.**

## 1. 기존 코드에서 확인한 사실

| 사실 | 위치 |
| --- | --- |
| 처치 이벤트 `Event<Defeated> DEFEATED`. 시그니처 `onDefeated(ServerLevel, BlockPos tombAnchor, @Nullable LivingEntity killer)` | `boss/TombGuardianEncounters.java:31` |
| 이벤트는 `guardianDied` 안에서 `markDefeated`가 true를 돌려준 *첫 한 번만* 호출된다 | `TombGuardianEncounters.java:138-143` |
| `guardianDied`는 `TombGuardian.die()`(내부, `super.die` 이후)에서 호출 → 보스 엔티티는 아직 사망 처리 중 | `TombGuardian.java:240-248` |
| 처치 기록은 `TombEncounterData`(SavedData `moreminecrafts:tomb_guardian_encounters`)에 `defeated` 목록으로 영속 | `TombEncounterData.java:24-40, 127-132` |
| `isRoyalChestSealed` / `isDefeated(level, tombAnchor)`는 영속 기록을 읽는다 | `TombGuardianEncounters.java:65-71` |
| 조우 시작은 `onRoyalChestInteraction(level, tombAnchor, spawnPos, player)`; 멱등, 동시 상호작용도 추가 보스를 만들지 않음 | `TombGuardianEncounters.java:77-104` |
| 처치 전 소멸(비사망 제거)은 바인딩을 해제해 재시도 가능; 청크 언로드는 바인딩 유지 | `TombGuardian.java:257-266` |
| `killer`는 null 가능(비플레이어 처치도 DEFEATED 발생) | `TombGuardianEncounters.java:37-39` |
| 보스 코드는 블록을 설치/파괴하지 않는다 | `boss/*.java` 전수 grep: `setBlock/destroyBlock/explode` 0건 |
| `DEFEATED`를 구독하는 **운영 코드는 없다**; 구독은 게임테스트 한 곳뿐 | `src/gametest/.../TombGuardianGameTests.java:48` |
| 금 골렘 엔티티·ST-01 구조물·비밀 입구 코드는 **없다** | `src/main` 전수 grep |

## 2. 설계에 미치는 영향

- **신호와 진실 분리:** 이벤트는 인메모리 호출이고 `defeated` 기록은 영속이다. 크래시·리스너 예외(배열 이벤트는 앞 리스너 예외가 뒤 리스너를 막을 수 있음)로 이벤트가 유실될 수 있으므로, 입구 상태는 이벤트가 아니라 `isDefeated`로 *재조정*해야 한다(시나리오 F6).
- **사망 처리 중 호출:** 리스너에서 골렘 엔티티를 즉시 생성하지 말고 다음 틱 작업으로 미룬다(보스 사망 처리 변경 금지 요건).
- **비플레이어 처치:** `killer == null`을 정당한 승리로 볼지는 열린 결정(O-9).
- **보스 재시도:** 처치 전 소멸은 `SEALED` 유지(입구 전이 없음).

## 3. 제안 인터페이스 (신규; 구현되지 않음)

| 책임 | 제안 형태 | 비고 |
| --- | --- | --- |
| 처치 이벤트 수신 | `TombGuardianEncounters.DEFEATED.register(coordinator::onDefeated)` — 신호만 받고 `coordinator.reconcile(level, anchor)` 호출 | 기존 이벤트를 *구독*할 뿐 기존 코드 변경 없음 |
| 금 골렘 생성 요청 | `GoldGuideSpawner.request(level, anchor, spawnPos)` → 결과 `OK/FAILED` | 다음 틱 실행. 실패 시 로그 + 타이머 유지 |
| 안내 상태 추적 | `SecretEntranceData` 레코드의 `state`, `guideUuid(옵션)` | 엔티티 UUID로 존재 확인 |
| 도착 판정 | `GuideProgress.arrived(guide, stopPoint, radius)` — 반경은 미승인 | 도착 구역은 별도 문서의 후보 중 선택 |
| 실패 타이머 | `remaining`(게임 틱)을 레코드에 저장, 틱 핸들러가 감소 | 값 미승인 |
| 덮개 개방 요청 | `CoverOpener.openIfNeeded(level, anchor)` — 멱등. 12칸 중 공기가 아닌 칸만 제거 | 청크 미로드 시 보류, 로드 시 재조정 |
| 영속 상태 | 신규 `SavedData` `moreminecrafts:st01_secret_entrance` (아래 필드) | 기존 `TombEncounterData`는 수정하지 않음 |

## 4. 호출 순서 제안 (의사 순서)

1. `DEFEATED(anchor)` 수신 → 큐에 `reconcile(anchor)` 등록(다음 틱).
2. `reconcile`: `isDefeated(anchor)` 아니면 종료. 레코드가 없으면 `GUARDIAN_DEFEATED`로 생성(남은 시간 설정) 후 골렘 생성 요청.
3. 생성 성공 → `GOLEM_GUIDING`. 생성 실패 → 상태 유지(타이머 계속).
4. 매 틱(무덤당 하나의 가벼운 핸들러): 남은 시간 감소; 골렘 도착 확인; 만료 시 `FALLBACK_OPEN`.
5. 열림 상태이고 덮개 청크가 로드되면 `CoverOpener.openIfNeeded` (멱등).
""")

w("ST01_P7_GOLEM_ROUTE_AND_ARRIVAL_CANDIDATES_2026-11-07.md", f"""# ST-01 P7 — 금 골렘 경로 및 도착 구역 후보 (2026-11-07)

상태: **정적 후보 분석 / 값은 승인 아님.** 도착 판정 반경, 골렘 크기와 속도는 미승인이므로 *관계식과 측정값*만 제시하고 최종 값을 정하지 않았다. 동쪽 기준 수치이며 서쪽은 정확한 거울(검사 EW1)이다.

## 1. 구조 관련 사실

- 12칸 덮개: X −9..−6 × Z 33..35 (Y −23). 상자 앵커(제안: P3 `CHEST_PRIMARY`) = {tuple(anchor)} 기준 오프셋 dx {off['dx'][0]}..{off['dx'][1]}, dy {off['dy'][0]}, dz {off['dz'][0]}..{off['dz'][1]} (서쪽은 dx 부호 반전).
- 덮개 주변 홀 바닥은 대부분 P3 석관 단(제단)에 막혀 있고, 덮개로 가는 홀 바닥 길은 남쪽(Z 36–37)과 동쪽 열을 지난다. 따라서 "홀 안쪽(서쪽)에서 직선 접근" 가정은 틀리며 경로는 BFS로 측정했다.

## 2. 도착 구역 후보 (덮개 직사각형으로부터의 거리 구간)

골렘이 설 수 있는 홀 바닥(발 높이 −22) 중 상자 접근점에서 CLOSED 상태로 걸어 도달할 수 있는 칸을 덮개로부터의 칸 중심–가장자리 거리로 나눴다. **덮개 칸 위는 후보가 아니다**(골렘이 개방 시 떨어짐).

| 구역 | 칸 수 | 최근접 거리(블록) | r_safe(골렘 폭1) | r_safe(골렘 폭2) |
| --- | --- | --- | --- | --- |
{zones}

r_safe = 정지 지점 둘레의 도착 반경이 이 값 이하이면 도착 판정 영역 안에서 골렘 몸체가 덮개 위에 놓일 수 없다. 반경을 r로 승인한다면 정지 지점은 r_safe ≥ r인 구역(또는 덮개 칸을 제외하는 판정)이어야 한다. 가장 가까운 구역 Z0의 r_safe(폭1)는 0이라 반경이 0보다 크면 몸체가 덮개에 걸칠 수 있다.

## 3. 경로 길이 (CLOSED 월드, 폭1·높이2 에이전트, 걸음 수)

| 출발 / 구역 | {zh} |
| --- |{' --- |' * len(E['zones'])}
{rt}

최대 {Lmax}걸음(가장 긴 경로 = 홀 입구 → 덮개에 가장 가까운 구역 Z0). 모든 출발점이 모든 구역에 도달한다(GA3).

## 4. 골렘 크기 민감도 (측정)

출발점별로 도달 가능한 구역 수(전체 {len(E['zones'])}개). 크기는 미결정이다.

| 에이전트(폭×폭×높이) | 출발점별 도달 구역 수 |
| --- | --- |
{sens}

- `CHEST_APPROACH`는 단 위(발 높이 −20) 칸이라 폭2 이상 에이전트의 시작 자세가 성립하지 않아 0으로 나온다(출발 자세 제약이며 경로 부재가 아님).
- 폭 3×3×4 에이전트는 일부 구역만 도달한다 → 큰 골렘이 필요하면 도착 구역·통로 재검토가 필요하다.

## 5. 후보 요약

| 후보 | 설명 | 장점 | 위험 |
| --- | --- | --- | --- |
| AZ-1 정지 지점을 Z2 이상(덮개에서 ≥1.58)에 두고 반경 ≤ r_safe | 도착 시 골렘이 덮개와 떨어져 있음 | 개방 시 골렘이 떨어지지 않음 | 플레이어 입장에서 구멍과 떨어진 곳에 서 있음(연출) |
| AZ-2 정지 지점 Z0 + 덮개 칸 제외 판정 | 구멍 바로 옆에 도착 | 연출상 자연스러움 | 판정 코드가 덮개 칸을 제외해야 함; 개방 순간 골렘이 구멍 가장자리 |
| AZ-3 도착 후 개방 직전 골렘을 정지 지점으로 이동 | 반경과 무관 | 위치 보장 | 추가 이동 상태 필요 |

선택은 열린 결정 O-4이다. 도착 반경·속도·크기는 미승인이다.
""")

w("ST01_P7_FALLBACK_TIMER_AND_PERSISTENCE_2026-11-07.md", f"""# ST-01 P7 — 실패 타이머 및 영속 상태 설계 (2026-11-07)

상태: **설계 제안 / 값 미승인.**

## 1. 실패 타이머

| 항목 | 제안 | 근거 |
| --- | --- | --- |
| 시작 | GUARDIAN_DEFEATED 수락 시 `remaining = T_fb`(게임 틱) | 생성 실패·소실·정지가 타이머를 취소하지 않음(음성 대조 "골렘 소실이 타이머 취소" 검출) |
| 진행 | 서버 가동 중 게임 틱 감소; 플레이어 존재·청크 로드와 무관 | 모델 시나리오 E5, F5 |
| 만료 | `FALLBACK_OPEN`; 이후 도착은 무시 | E6 |
| 일시정지 | 하지 않음(제안). 대안: 플레이어 접근 시에만 진행 | 열린 결정 O-3 |
| 값 | **미승인** | `T_fb` 기호 |

### 값의 하한 관계식 (수치 결정 아님)

`T_fb` > (가장 긴 정직한 안내 시간) + (플레이어 대기 여유) + 여유. 가장 긴 정직한 안내 시간 = L_max / v. 측정된 L_max = **{Lmax}걸음**(골렘 이동 속도 v는 미승인).

예시(**설명용**; 제안 아님): L_max를 속도 v(블록/틱)로 걸을 때 틱 수

| v (블록/틱) | 틱 | 초(20틱/초) |
| --- | --- | --- |
{sizing}

골렘이 "플레이어가 뒤처지면 기다린다"는 권위 요구 때문에 정직한 안내 시간에는 상한이 없다 → `T_fb`는 *대기 포함 최대 안내 시간*으로 정의해야 하며, 너무 짧으면 정상 안내를 끊고 너무 길면 정지/소실 시 대기가 길어진다. 값 결정은 사용자 몫(O-2).

## 2. 영속 상태 (신규 SavedData 제안, 기존 `TombEncounterData` 불변)

키: 무덤 앵커(`BlockPos.asLong`), 보스 기록과 동일 키 사용.

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `state` | enum | SEALED(기록 없음 허용) / GUARDIAN_DEFEATED / GOLEM_GUIDING / ENTRANCE_OPEN / FALLBACK_OPEN | 예 |
| `remaining` | long(틱) | 남은 대체 개방 시간 | 예(개방 전) |
| `coverApplied` | boolean | 12칸이 물리적으로 제거되었는지(보조; 블록이 진실) | 예 |
| `guideUuid` | UUID? | 현재 골렘(있으면) | 선택 |
| `openedAt` | long? | 개방 게임 시간(진단) | 선택 |

모델 검사 SM3: 보스 처치 여부(보스 기록에서 읽음) + `state` + `remaining` + `coverApplied` 4가지를 JSON 직렬화/복원한 값이 *모든 도달 가능 상태에서* 재시작 후 상태를 정확히 재현한다. 골렘 엔티티는 재시작 후 존재한다고 가정하지 않는다(보수적): 안내 중 재시작 시 골렘 재생성 여부는 열린 결정이며, 재생성이 없어도 타이머가 개방을 보장한다(F4).

### 조정(reconcile) 규칙

1. 로드/틱: `isDefeated`이면서 레코드가 SEALED/없음 → GUARDIAN_DEFEATED로 승격(이벤트 유실 복구).
2. 레코드가 열림 상태이면 덮개 청크 로드 시 12칸 제거(공기가 아닌 칸만; 멱등).
3. **불일치(레코드 열림인데 보스 처치 기록 없음):** 모델은 이 상태를 도달 불가로 증명하지만 현실의 부분 저장 실패로 생길 수 있다. 처치 전 개방 금지 원칙에 따라 새 제거는 수행하지 않고 이상 로그만 남긴다(복원도 하지 않음). 처리 정책은 열린 결정 O-8.
""")

w("ST01_P7_COVER_SAFETY_ANALYSIS_2026-11-07.md", f"""# ST-01 P7 — 덮개 안전성 분석 (2026-11-07)

상태: **정적 분석.** P6의 12블록 절개와 CLOSED/OPEN 복셀을 그대로 사용(P6 검증기로 월드 생성, 수정 없음). 서쪽은 정확한 거울(EW1).

## 1. 위험 추적

| ID | 위험 | 측정 | 상태 |
| --- | --- | --- | --- |
| R-P7-1 (= FIND-P6-1) | 열린 구멍 가장자리 4블록 낙하 | 최대 낙차 {E['max_fall']}(피해 1) | OPEN, 설계 결정 필요(O-5). 변경하지 않음 |
| R-P7-2 (= FIND-P6-2) | 덮개 위 점유 | 12열 모두 서 있을 수 있음; 개방 시 매몰 없음(아래) | **개방 방향은 안전**. 복원 방향은 미설계(복원 규칙 미확정이므로 자동 복원 확정 안 함) |
| R-P7-3 | 골렘이 덮개 위에서 개방 | r_safe 관계식으로 정지 지점 선택 필요 | OPEN(O-4) |
| R-P7-4 | 밀폐된 숨은 공동 12칸(CLOSED)의 몹 스폰/광원 | 게임 내 거동 UNVERIFIED | UNVERIFIED |
| R-P7-5 | 다수 플레이어가 구멍 가장자리에서 밀침 | 서버 검증 필요 | UNVERIFIED |

## 2. 개방 시 점유 플레이어 (검사 GA1)

덮개 12열 각각에서: 닫힘 상태에서 서 있을 수 있고, 개방 시 착지 칸이 서 있기 가능, 머리 공간 ≥4, 홀 바닥으로 되돌아 올라갈 수 있고 비밀 창고 입구까지 걸어갈 수 있다.

| 열 (X, Z) | 착지 Y | 낙차 | 바닐라 피해(3블록 초과분) | 머리 공간 | 홀로 복귀 | 창고 입구 도달 |
| --- | --- | --- | --- | --- | --- | --- |
{fall_tbl}

결과: 개방 시 *매몰 없음*, 갇힘 없음. 최대 낙차 {E['max_fall']}블록(X=−6 열)이며 해당 열에서만 피해 1이 가능하다. 서쪽은 X 부호 반전으로 동일.

## 3. CLOSED 유지 구간

SEALED, GUARDIAN_DEFEATED, GOLEM_GUIDING 동안 월드는 CLOSED이며 홀은 P3와 복셀 동일(GA6, 차이 0칸). 따라서 골렘 이동·보스 전투·플레이어 이동은 P3에서 검증된 바닥을 본다.

## 4. 개방 중 플레이어 이동 가능성

OPEN에서 구멍 열은 서 있을 수 있는 위치이며(P6 CO6, 12개) 계단으로 이어져 창고까지 도보 연결이 있다(P6 P5.K3). 개방은 12칸 제거 한 번이고 다른 칸은 변하지 않는다(P6 CO2/CO3).

## 5. 복원

복원 규칙이 미확정이므로 상태기계에 복원 전이를 넣지 않았다(불변식 I2). 복원이 승인될 경우 필요한 사전 조건: 점유 처리(구멍 열 12위치와 계단), 낙하 중 엔티티, 아이템 엔티티, 골렘 위치 — 별도 설계가 필요하다.
""")

w("ST01_P7_OPEN_DECISIONS_2026-11-07.md", """# ST-01 P7 — 구현 전 미해결 결정 목록 (2026-11-07)

상태: **결정 요청 목록.** 구현용 기본값을 만들지 않았다. D-3, Q15 비밀 묘실 사양, Q98 최대 깊이 상한은 미확정으로 유지한다(P6 목록의 D-I, D-G, D-H).

| ID | 결정 | 막는 것 | 선택지 요약 |
| --- | --- | --- | --- |
| O-1 | 대체 개방 대기 시간 `T_fb` | 타이머 구현 | 기호만 정의됨. 하한 관계식은 타이머 문서 참조 |
| O-2 | `T_fb` 정의: "대기 포함 최대 안내 시간" 기준 | 값 산정 | 정직한 안내 시간에는 상한이 없음 |
| O-3 | 타이머 시작 시점(처치 수락 vs 안내 시작)과 일시정지 여부(플레이어 없을 때) | 타이머 구현 | 제안: 처치 수락 시점, 일시정지 없음 |
| O-4 | 도착 판정 반경, 정지 지점, 몸체-덮개 분리 방식(AZ-1/2/3) | 도착 판정 | r_safe 관계식 참조 |
| O-5 | 구멍 가장자리 4블록 낙하(FIND-P6-1) 처리 | 안전성 | 허용 / 난간 / 형상 변경(변경 시 P3–P6 재검증) |
| O-6 | 금 골렘 크기·이동 속도·생성 위치(처치 지점 vs 고정 지점) | 골렘 구현, 경로 검증 재실행 | 크기 민감도 표 참조 |
| O-7 | 안내 중 서버 재시작 시 골렘 재생성 여부 | 영속 설계 | 타이머가 개방을 보장하므로 선택 사항 |
| O-8 | 저장 불일치(열림 기록 + 처치 기록 없음) 정책 | 오류 복구 | 기록 무시·로그 / 덮개 상태 수동 복구 |
| O-9 | 비플레이어 처치(`killer==null`)도 정당한 승리인가 | 입구 개방 조건 | 포함 / 제외(제외 시 보스 코드와 불일치 가능) |
| O-10 | 다중 플레이어: 누구를 따라가는가, 입장 권한(처치 기여자 제한 여부) | 안내 대상 | 가장 가까운 플레이어 / 상자 상호작용자 / 모두 |
| O-11 | 복원(재폐쇄) 여부 | 상태기계 확장 | 이번에 확정하지 않음 |
| O-12 | 개방 연출(사운드·파티클·단계적 제거) | 연출 | P6 D-C |
| O-13 | 보스 등장 지점·상자 앵커 확정(P3-F4) | 앵커 기준 오프셋 | P6 D-E |
| O-14 | 월드젠 배치 | 구조 생성 | P6 D-N |

BOSS-R1(문턱/접근로 높이 대 보스 4.35)은 실전 전투 검증 대기이며 종결하지 않았다.
""")

w("ST01_P7_USER_APPROVAL_RECORD_2026-11-07.md", """# ST-01 P7 — 사용자 승인 기록 (2026-11-07)

상태: **기록 문서 / 설계 전용.** Q01–Q100 설계 권위를 변경하지 않고 구현을 승인하지 않는다. 기준: `work/st01-p6-final-spec-20261107` @ `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f`, 새 브랜치 `work/st01-p7-secret-entrance-state-20261107`.

## 1. 승인된 개방 규칙

1. Tomb Guardian 처치 후 금 골렘으로 전환한다.
2. 금 골렘은 비밀 입구까지 플레이어를 안내한다.
3. 금 골렘이 지정된 도착 구역에 도달하면 비밀 입구를 개방한다.
4. 골렘이 경로탐색 실패, 정지, 소실 등으로 도착하지 못하면 제한 시간 후 자동 개방한다.
5. 대체 개방이 발생해도 보스 처치 및 보상 조건을 우회하지 않는다.

## 2. 미승인(수치 확정 금지)

대기 시간, 도착 판정 반경, 골렘 이동 속도. 이 문서와 P7 산출물에는 이 값들의 *기호와 관계식*만 있으며 최종 사양이 아니다.

## 3. 이 규칙의 해석 (결정 범위를 넘지 않음)

- "자동 개방"은 비밀 입구의 CLOSED→OPEN(P6 12칸 제거)만을 뜻한다. 복원은 승인되지 않았다.
- 규칙 5는 *개방 조건이 보스 처치에 종속*됨을 의미한다고 적용했다: 어떤 경로로도 처치 전에는 열리지 않고, 대체 개방은 보스 처치 기록·상자 봉인 해제·보상 지급에 관여하지 않는다. 거대 상자(P6 Q72–Q93)는 입구가 열린 뒤 일반 상자로 열린다는 점은 규칙 5와 모순되지 않는다(입구가 처치에 종속되므로).
- 앞선 Q63(A)은 골렘이 기존 Q16 확장 보물창고로 안내한다고 결정했다. 이 규칙은 그 안내의 중간 지점(비밀 입구)을 구체화한다.

## 4. 유지되는 미확정

D-3 동·서 방향, Q15 비밀 묘실 사양, Q98 최대 깊이 상한, BOSS-R1 실전 검증, 복원 규칙.
""")

final = f"""# ST-01 P7 — 최종 보고서 (2026-11-07)

상태: **설계 완료 보고 / 구현 승인 아님.** 정적 검증과 실제 서버 검증을 구분한다.

- 기준: `work/st01-p6-final-spec-20261107` @ `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (작업 트리 깨끗, origin 일치 확인), 신규 브랜치 `work/st01-p7-secret-entrance-state-20261107`
- 도구: `tools/st01-p7/` (`p7_statemachine.py`, `st01_p7_geometry.py`, `st01_p7_verify.py`, `st01_p7_mutation_tests.py`, `build_p7_report.py`)

## 1. 판정 요약

PASS {cnt('PASS')} / FAIL {cnt('FAIL')} / UNVERIFIED {cnt('UNVERIFIED')} (모델 5 + 기하 6×2 + 거울 1 + 실제 서버 1).

- **정적 PASS:** 상태기계 전수 탐색(D=1..4)과 불변식 I1–I9, 시나리오 13개, 영속 충분성, 무덤 격리, 처치 전 봉인 유지, 덮개 개방 안전성, 골렘 경로·도착 구역, 동·서 거울 일치.
- **UNVERIFIED:** 실제 서버 거동(LV1) — 블록 제거 타이밍, 엔티티 생성·경로탐색, 실제 재시작 영속, 청크 로딩, 다중 플레이어 경합, 낙하 피해. 구현이 없어 검증 불가.
- 음성 대조 {sum(m['detected'] for m in MU['results'])}/{len(MU['results'])} 검출(상태기계 결함 16 + 기하 결함 4), 무결함 대조군 FAIL 0.

## 2. 핵심 불변식 결과

| 불변식 | 결과 |
| --- | --- |
| 보스 처치 전 개방 불가 | PASS (SM1 I1, SM5; 도달 가능한 처치 전 상태 전부 SEALED·덮개 닫힘·골렘 0) |
| 중복 이벤트 멱등성 | PASS (I3, 시나리오 E1/E2/E6) |
| 재시작 후 진행 복원 | PASS (I4, SM3, F4) |
| 대체 개방 보장 | PASS (I7: 골렘 생성 실패·정지·소실·재시작·청크 언로드·플레이어 이탈 모든 경로에서 이로운 사건만으로 개방) — 단 *실제 서버 UNVERIFIED* |
| 대체 개방이 보상 조건 우회 안 함 | PASS (I6: P7 이벤트가 보스 처치 조건을 바꾸지 않음; 음성 대조 검출) |

## 3. 산출물

| 요구 산출물 | 파일 |
| --- | --- |
| 사용자 승인 기록 | `ST01_P7_USER_APPROVAL_RECORD_2026-11-07.md` |
| 상태 전이도, 정상·실패 시나리오 | `ST01_P7_STATE_TRANSITION_SPEC_2026-11-07.md` |
| 기존 보스 코드 연결점 | `ST01_P7_BOSS_CODE_CONNECTIONS_2026-11-07.md` |
| 금 골렘 경로 및 도착 구역 후보 | `ST01_P7_GOLEM_ROUTE_AND_ARRIVAL_CANDIDATES_2026-11-07.md` |
| 실패 타이머, 영속 상태 설계 | `ST01_P7_FALLBACK_TIMER_AND_PERSISTENCE_2026-11-07.md` |
| 덮개 안전성 분석 | `ST01_P7_COVER_SAFETY_ANALYSIS_2026-11-07.md` |
| 검증기, 음성 대조 | `tools/st01-p7/`, `generated/p7_verification.json`, `generated/p7_mutation_results.json` |
| 미해결 결정 목록 | `ST01_P7_OPEN_DECISIONS_2026-11-07.md` |
| 최종 보고서 | 이 문서 |

## 4. 핵심 발견

1. 기존 코드에는 처치 이벤트와 영속 처치 기록이 있으나 **`DEFEATED` 운영 구독자, 금 골렘, 비밀 입구 코드는 없다.** 모두 신규 구현이다.
2. 이벤트는 인메모리 신호이고 처치 기록이 영속 진실이므로 입구 상태는 항상 `isDefeated`로 재조정해야 한다(리스너 실패·크래시에도 개방 보장).
3. 개방 방향 안전성: 12칸 위 점유 플레이어는 매몰되지 않고 모든 열에서 홀로 복귀·창고 도달 가능, 최대 낙차 {E['max_fall']}블록(피해 1 가능, FIND-P6-1 유지).
4. 홀 바닥은 단(제단)에 막혀 덮개 주변 접근이 남쪽·동쪽 열로 제한된다. 골렘 도착 구역 후보와 안내 경로(최대 {Lmax}걸음)를 측정했다.
5. 골렘이 덮개 위에 서면 개방 시 떨어진다 → 정지 지점은 r_safe 관계식을 만족해야 한다(승인된 반경이 없으므로 규칙만 제시).

## 5. 미해결 결정

`ST01_P7_OPEN_DECISIONS_2026-11-07.md` O-1..O-14. 구현에 직접 필요한 것은 O-1, O-3, O-4, O-6, O-9, O-10, O-13, O-14; D-3, Q15, Q98은 미확정 유지.

## 6. LIVE VALIDATION PENDING

덮개 제거, 골렘 생성·경로탐색·대기, 재시작 영속, 청크 언로드, 다중 플레이어, 낙하 피해, 시각·사운드, BOSS-R1.

## 7. 음성 대조 결과

| 종류 | 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- | --- |
{mut}

## 8. 전체 검증 증거

| 측 | ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- | --- |
{evid}

## 9. 범위 밖 발견

- 없음. P1 O-1(권위 인덱스 동기화) 및 P6 D-J(P2 승인 기록에 후보 C 반영)는 여전히 별도 승인 작업이다.
"""
w("ST01_P7_FINAL_REPORT_2026-11-07.md", final)
print("ok", cnt("PASS"), cnt("FAIL"), cnt("UNVERIFIED"))

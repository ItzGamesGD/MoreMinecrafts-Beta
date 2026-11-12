# ST-01 P7 — 최종 보고서 (2026-11-07)

상태: **설계 완료 보고 / 구현 승인 아님.** 정적 검증과 실제 서버 검증을 구분한다.

- 기준: `work/st01-p6-final-spec-20261107` @ `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (작업 트리 깨끗, origin 일치 확인), 신규 브랜치 `work/st01-p7-secret-entrance-state-20261107`
- 도구: `tools/st01-p7/` (`p7_statemachine.py`, `st01_p7_geometry.py`, `st01_p7_verify.py`, `st01_p7_mutation_tests.py`, `build_p7_report.py`)

## 1. 판정 요약

PASS 18 / FAIL 0 / UNVERIFIED 1 (모델 5 + 기하 6×2 + 거울 1 + 실제 서버 1).

- **정적 PASS:** 상태기계 전수 탐색(D=1..4)과 불변식 I1–I9, 시나리오 13개, 영속 충분성, 무덤 격리, 처치 전 봉인 유지, 덮개 개방 안전성, 골렘 경로·도착 구역, 동·서 거울 일치.
- **UNVERIFIED:** 실제 서버 거동(LV1) — 블록 제거 타이밍, 엔티티 생성·경로탐색, 실제 재시작 영속, 청크 로딩, 다중 플레이어 경합, 낙하 피해. 구현이 없어 검증 불가.
- 음성 대조 20/20 검출(상태기계 결함 16 + 기하 결함 4), 무결함 대조군 FAIL 0.

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
3. 개방 방향 안전성: 12칸 위 점유 플레이어는 매몰되지 않고 모든 열에서 홀로 복귀·창고 도달 가능, 최대 낙차 4블록(피해 1 가능, FIND-P6-1 유지).
4. 홀 바닥은 단(제단)에 막혀 덮개 주변 접근이 남쪽·동쪽 열로 제한된다. 골렘 도착 구역 후보와 안내 경로(최대 42걸음)를 측정했다.
5. 골렘이 덮개 위에 서면 개방 시 떨어진다 → 정지 지점은 r_safe 관계식을 만족해야 한다(승인된 반경이 없으므로 규칙만 제시).

## 5. 미해결 결정

`ST01_P7_OPEN_DECISIONS_2026-11-07.md` O-1..O-14. 구현에 직접 필요한 것은 O-1, O-3, O-4, O-6, O-9, O-10, O-13, O-14; D-3, Q15, Q98은 미확정 유지.

## 6. LIVE VALIDATION PENDING

덮개 제거, 골렘 생성·경로탐색·대기, 재시작 영속, 청크 언로드, 다중 플레이어, 낙하 피해, 시각·사운드, BOSS-R1.

## 7. 음성 대조 결과

| 종류 | 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- | --- |
| state machine | a defeat event is trusted without re-checking the defeated record (spoofed/stale/wrong-tomb event opens the entrance) | SM1, SM5 | SM1, SM4, SM5 | 검출 |
| state machine | timer starts when the chest is touched before the kill | SM1, SM5 | SM1, SM2, SM4, SM5 | 검출 |
| state machine | fallback timer also marks the guardian defeated / unlocks the chest (reward bypass) | SM1 | SM1, SM2, SM5 | 검출 |
| state machine | duplicate defeat events spawn a second golem | SM1 | SM1 | 검출 |
| state machine | golem loss cancels the fallback timer | SM1 | SM1, SM2 | 검출 |
| state machine | server restart forgets the entrance record | SM1, SM3 | SM1, SM2, SM3 | 검출 |
| state machine | server restart forgets the remaining time | SM1, SM3 | SM1, SM2, SM3 | 검출 |
| state machine | opening never applied physically when the cover chunk is unloaded at open time | SM1 | SM1, SM2 | 검출 |
| state machine | lost defeat event is never reconciled from the boss record | SM1, SM2 | SM1, SM2 | 검출 |
| state machine | timer opens a SEALED entrance | SM1, SM5 | SM1, SM2, SM4, SM5 | 검출 |
| state machine | early chest interaction opens the entrance | SM1, SM5 | SM1, SM2, SM4, SM5 | 검출 |
| state machine | opening buries a player standing on the cover | SM1 | SM1 | 검출 |
| state machine | cover is restored automatically when the player leaves (no restore rule is approved) | SM1 | SM1 | 검출 |
| state machine | auto-restore under a player | SM1 | SM1 | 검출 |
| state machine | duplicate open request opens a SEALED entrance | SM1, SM5 | SM1, SM2, SM4, SM5 | 검출 |
| state machine | tombs share one record | SM4 | SM4 | 검출 |
| geometry | anchor-relative cover offsets wrong (chest anchor moved one block) | GA5 | GA5 | 검출 |
| geometry | stair void top lowered by one (the cut region changes) | GA5 | GA5 | 검출 |
| geometry | last stair tread missing (landing 2 below, no climb-out) | GA1 | GA1 | 검출 |
| geometry | cover region grows to 15 cells (stray floor cuts) | GA2, GA5 | GA2, GA5 | 검출 |

## 8. 전체 검증 증거

| 측 | ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- | --- |
| - | SM1 | Exhaustive exploration (fallback length D = 1..4 abstract units, all event orders, duplicates, restarts, chunk unload/reload, player leave/join, occupancy): invariants I1-I9 hold in every reachable state/transition | PASS | reachable states/edges per D [(1, 141, 2292), (2, 216, 3504), (3, 291, 4716), (4, 366, 5928)]; violations 0 []; invariants: I1 no opening before defeat, I2 monotone (no regress, no restore), I3 idempotent duplicate events, I4 restart keeps persistent data and deadline, I5 <=1 golem, I6 reward condition untouched, I7 liveness (benign events always open it), I8 cover only with an open record, I9 nobody buried |
| - | SM2 | Normal and failure scenario scripts (N1, F1-F6, E1-E6) end in the expected record/cover/golem state | PASS | N1=ok; F1=ok; F2=ok; F3=ok; F4=ok; F5=ok; F6=ok; E1=ok; E2=ok; E3=ok; E4=ok; E5=ok; E6=ok |
| - | SM3 | Persistence sufficiency: the 4 persisted fields (boss-defeated record, entrance state, remaining time, cover applied) serialised to JSON and restored reproduce exactly the post-restart state for every reachable state | PASS | 216 reachable states, mismatches 0 |
| - | SM4 | Tomb isolation: records keyed by tomb anchor; random event sequences on tomb A never open tomb B (4000 runs x 14 events, fixed seed) | PASS | leaks 0 |
| - | SM5 | Before the guardian is defeated (any history of early chest touches, spoofed/stale defeat events, timers, restarts, chunk churn) the record stays SEALED, the cover stays closed and no golem exists | PASS | reachable pre-defeat states 6, leaking 0 |
| E | GA1 | Player standing on a cover cell when the cover opens (FIND-P6-2): each of the 12 columns is standable while CLOSED, the player lands on a standable cell (no burial), headroom >= 4, can climb back to the hall floor and can walk on to the vault; drop <= 4 (FIND-P6-1) | PASS | max drop 4 (fall damage 1 under the vanilla 'above 3' rule); per column (drop/headroom): (-9, 33):1/5, (-9, 34):1/5, (-9, 35):1/5, (-8, 33):2/5, (-8, 34):2/5, (-8, 35):2/5, (-7, 33):3/5, (-7, 34):3/5, (-7, 35):3/5, (-6, 33):4/5, (-6, 34):4/5, (-6, 35):4/5; all can climb out True; all reach vault entry True |
| E | GA2 | Arrival-zone candidates: hall-floor cells reachable from the chest approach in CLOSED, binned by distance from the cover rectangle; the cover is a full rectangle, none lies on the cover, all standable in CLOSED; the analytic rectangle distance equals the per-cell minimum; r_safe = largest arrival radius (around the stop point) that keeps the golem body off the cover | PASS | Z0: 8 cells, nearest centre-to-cover 0.5, r_safe(w=1) 0.0, r_safe(w=2) -0.5; Z1: 9 cells, nearest centre-to-cover 0.707, r_safe(w=1) 0.207, r_safe(w=2) -0.293; Z2: 7 cells, nearest centre-to-cover 1.581, r_safe(w=1) 1.081, r_safe(w=2) 0.581; Z3: 6 cells, nearest centre-to-cover 2.55, r_safe(w=1) 2.05, r_safe(w=2) 1.55; Z4: 9 cells, nearest centre-to-cover 3.536, r_safe(w=1) 3.036, r_safe(w=2) 2.536 |
| E | GA3 | Golem walking route in the CLOSED world: every emergence candidate, the chest approach and the hall door reach every arrival-zone class (shortest steps measured; inputs for the fallback timer) | PASS | unreachable []; steps to the rim zone Z0: BOSS_S=20, BOSS_N=32, BOSS_E=20, BOSS_W=32, CHEST_APPROACH=17, HALL_DOOR=42; overall max 42 |
| E | GA4 | Agent footprint sensitivity (golem size UNDECIDED): number of zone classes reachable per start for 1x1x2, 1x1x3, 2x2x3, 3x3x4 agents (measurement, not a size proposal); the 1x1x2 agent reaches all classes | PASS | 1x1x2: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 5, 'HALL_DOOR': 5}; 1x1x3: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 5, 'HALL_DOOR': 5}; 2x2x3: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 0, 'HALL_DOOR': 5}; 3x3x4: {'BOSS_S': 3, 'BOSS_N': 3, 'BOSS_E': 3, 'BOSS_W': 3, 'CHEST_APPROACH': 0, 'HALL_DOOR': 3} |
| E | GA5 | Cover cells relative to the royal-chest anchor (PROPOSED anchor = P3 CHEST_PRIMARY): dx 12..15 (east) / -15..-12 (west), dy -3, dz 0..2; 12 cells | PASS | anchor (-21, -20, 33); offsets dx (12, 15), dy [-3], dz (0, 2), count 12 |
| E | GA6 | While SEALED / GUARDIAN_DEFEATED / GOLEM_GUIDING the world is the CLOSED world: hall voxel-identical to P3 above the floor top, so golem walking and boss/player movement see the P3-verified floor | PASS | differing cells 0 |
| W | GA1 | Player standing on a cover cell when the cover opens (FIND-P6-2): each of the 12 columns is standable while CLOSED, the player lands on a standable cell (no burial), headroom >= 4, can climb back to the hall floor and can walk on to the vault; drop <= 4 (FIND-P6-1) | PASS | max drop 4 (fall damage 1 under the vanilla 'above 3' rule); per column (drop/headroom): (6, 33):4/5, (6, 34):4/5, (6, 35):4/5, (7, 33):3/5, (7, 34):3/5, (7, 35):3/5, (8, 33):2/5, (8, 34):2/5, (8, 35):2/5, (9, 33):1/5, (9, 34):1/5, (9, 35):1/5; all can climb out True; all reach vault entry True |
| W | GA2 | Arrival-zone candidates: hall-floor cells reachable from the chest approach in CLOSED, binned by distance from the cover rectangle; the cover is a full rectangle, none lies on the cover, all standable in CLOSED; the analytic rectangle distance equals the per-cell minimum; r_safe = largest arrival radius (around the stop point) that keeps the golem body off the cover | PASS | Z0: 8 cells, nearest centre-to-cover 0.5, r_safe(w=1) 0.0, r_safe(w=2) -0.5; Z1: 9 cells, nearest centre-to-cover 0.707, r_safe(w=1) 0.207, r_safe(w=2) -0.293; Z2: 7 cells, nearest centre-to-cover 1.581, r_safe(w=1) 1.081, r_safe(w=2) 0.581; Z3: 6 cells, nearest centre-to-cover 2.55, r_safe(w=1) 2.05, r_safe(w=2) 1.55; Z4: 9 cells, nearest centre-to-cover 3.536, r_safe(w=1) 3.036, r_safe(w=2) 2.536 |
| W | GA3 | Golem walking route in the CLOSED world: every emergence candidate, the chest approach and the hall door reach every arrival-zone class (shortest steps measured; inputs for the fallback timer) | PASS | unreachable []; steps to the rim zone Z0: BOSS_S=20, BOSS_N=32, BOSS_E=20, BOSS_W=32, CHEST_APPROACH=17, HALL_DOOR=42; overall max 42 |
| W | GA4 | Agent footprint sensitivity (golem size UNDECIDED): number of zone classes reachable per start for 1x1x2, 1x1x3, 2x2x3, 3x3x4 agents (measurement, not a size proposal); the 1x1x2 agent reaches all classes | PASS | 1x1x2: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 5, 'HALL_DOOR': 5}; 1x1x3: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 5, 'HALL_DOOR': 5}; 2x2x3: {'BOSS_S': 5, 'BOSS_N': 5, 'BOSS_E': 5, 'BOSS_W': 5, 'CHEST_APPROACH': 0, 'HALL_DOOR': 5}; 3x3x4: {'BOSS_S': 3, 'BOSS_N': 3, 'BOSS_E': 3, 'BOSS_W': 3, 'CHEST_APPROACH': 0, 'HALL_DOOR': 3} |
| W | GA5 | Cover cells relative to the royal-chest anchor (PROPOSED anchor = P3 CHEST_PRIMARY): dx 12..15 (east) / -15..-12 (west), dy -3, dz 0..2; 12 cells | PASS | anchor (21, -20, 33); offsets dx (-15, -12), dy [-3], dz (0, 2), count 12 |
| W | GA6 | While SEALED / GUARDIAN_DEFEATED / GOLEM_GUIDING the world is the CLOSED world: hall voxel-identical to P3 above the floor top, so golem walking and boss/player movement see the P3-verified floor | PASS | differing cells 0 |
| E/W | EW1 | East and west geometric results are exact mirrors (drops, headroom, zone sizes/distances, route lengths, cover offsets) | PASS | {'ga1': True, 'zones': True, 'routes': True, 'offsets': True} |
| - | LV1 | Real-server behaviour: block removal timing, entity spawn/pathfinding, persistence across a real restart, chunk loading, multiplayer races, damage | UNVERIFIED | requires an implementation and a running server; not available in this design-only task |

## 9. 범위 밖 발견

- 없음. P1 O-1(권위 인덱스 동기화) 및 P6 D-J(P2 승인 기록에 후보 C 반영)는 여전히 별도 승인 작업이다.

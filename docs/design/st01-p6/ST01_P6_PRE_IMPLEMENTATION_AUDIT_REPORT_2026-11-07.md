# ST-01 P6 — 구현 전 최종 감사 보고서 (2026-11-07)

상태: **설계 검증 문서 / 구현 승인 아님.** 이 문서는 Q01–Q100 설계 권위를 변경하지 않고 NBT·Java·월드젠·보스·보상 구현을 승인하지 않는다. 아래 모든 PASS는 *정적 좌표·복셀 검증*의 결과이며 게임 내 거동은 검증되지 않았다(LIVE VALIDATION PENDING).

- 기준: 브랜치 `work/st01-p5-integration-20261107` @ `f7bf07fc96dca6ee150cfa01a597264167459ce0` (작업 트리 깨끗, origin 일치 확인)
- 권위: `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md` (무변경)
- 도구: `tools/st01-p6/` (`st01_p6_verify.py`, `st01_p6_mutation_tests.py`, `p2c_overlay.py`, `build_p6_report.py`)

## 1. 판정 요약

| 측 | PASS | FAIL | UNVERIFIED |
| --- | --- | --- | --- |
| 동쪽 통합 모델 | 26 | 0 | 1 |
| 서쪽 통합 모델 | 26 | 0 | 1 |

- 단계별 PASS의 합산이 아니다: P1·P2·P3·P4를 후보 C 오버레이가 적용된 **하나의 복셀 월드**에 합치고(P1 하강 계단 포함) 요소 레지스트리를 별도 코드 경로로 복셀화해 대조했다(R1). P2·P3·P4 기존 검증기는 같은 월드에서 회귀(REG34)로만 재실행했으며 합계에 넣지 않는다.
- UNVERIFIED 1건은 `P5.C1`(덮개 기계장치의 게임 내 구현 가능성). FAIL 0건.
- 음성 대조: 16/16 검출, 무결함 대조군 FAIL 0(§8).
- 동·서 거울 동치: closed_solid=True, open_solid=True, own=True, elem=True, registry_bbox_mirror=True, turn_sequence_swapped=True, route_length_equal=True. 주 경로 길이 동·서 모두 237 걸음, 회전열은 좌우가 정확히 뒤바뀜.

## 2. 상태 분류 (APPROVED / CONDITIONALLY APPROVED / PROPOSED / UNVERIFIED / BLOCKED)

| 분류 | 항목 |
| --- | --- |
| **APPROVED** (사용자 직접 결정) | P4-B 측방형을 통합 기준으로 채택(조건부); 비밀 입구 덮개는 A안(바닥 블록 제거형); P2-F1 후보 C 채택; Q97 최소 해석 적용; Q01–Q100 보존 |
| **CONDITIONALLY APPROVED** | P1 좌표(P1 승인 기록); P2 좌표 및 후보 C 적용 7개 공간(P2 승인 기록 + 후보 C 결정; 좌표는 오버레이로만 존재); P3 홀 좌표(P4 잠정 기준선, Q43–Q62 값은 권위); P4-B 연결 구조·보물창고 좌표(조건부 채택; 내부 치수 Q65–Q93은 권위); 비밀 입구 12칸 절개(A안 결정, 메커니즘 미정) |
| **PROPOSED** | 비밀 묘실(Q15) 15×13×7 및 벽감 6개; 보물 더미·들보 등 장식 요소; 12칸 절개의 구체 위치(P3 후방 안쪽 모서리 구역 내) 외 P4 연결 통로의 세부 치수; 모든 외부 치수 가정(P2 벽 두께 2) |
| **UNVERIFIED** | 덮개 기계장치/애니메이션; 열린 구멍 낙하 거동; 보스 전투·BOSS-R1; 골렘 경로탐색; 월드젠 배치·지형·청크; NBT; 시각; 광원/유체/몹 누출의 실제 게임 거동; 모든 게임 내 항목 |
| **BLOCKED** | §9 미해결 결정 목록 참조(트리거·복원 규칙, 금 골렘 구현, 등장 지점, Q15 사양, Q98 상한, D-3 등) |

## 3. P2-F1 후보 C 적용 결과

`ST01_P6_P2F1_CANDIDATE_C_COMPARISON_2026-11-07.md` 참조. 오버레이는 변경 7개 공간(V12, FA, LND12, FB, C1, C2, BRB1)만 건드리며(O1) 나머지 19개 P2 공간·링크·포트·목표·P1 박스는 변경 0이다. 9×9 참, 9×11 V12, 7폭 계단(발판 4+4)이 유지되고(O3) 벽 포함 ±40 초과 0칸(P5.X1)이며 P2 엔진 재실행 모두 PASS(P5.P2R 15점검, P2 검증기 동·서 18/18), 추가 깊이 0이다. 기존 P2 모델은 수정하지 않았다.

## 4. 비밀 입구 CLOSED / OPEN 검증

- 덮개 영역은 P3 슬래브 상단층의 12칸(4×3, 동쪽 X −9..−6 × Z 33..35, Y −23)이며 해석적 위치와 일치(CO1).
- CLOSED: 홀은 P3 모델과 복셀 단위로 동일하며 구멍 0(P5.K1); 홀 공기에서 6방향 flood-fill이 비밀 구역의 덮개층 이하 칸에 0칸 도달(CO4); 걸어서 도달 가능한 비밀 구역 칸 0(P5.K2). 덮개 아래 숨은 공동 12칸은 밀폐된다.
- OPEN: 홀 변화는 12칸 절개 하나뿐이며 해석 상자 교집합과 일치하고 관통 체적 안에 있다(P5.K4); 진입광장→…→거대 상자 앞 5칸이 도보로 연결(P5.K3, 총 237걸음, 홀 계단 시작→상자 앞 42걸음).
- 전환 대수(CO2): OPEN→CLOSED는 정확히 12칸을 채우고, CLOSED→OPEN은 정확히 12칸을 제거하며, OPEN→CLOSED→OPEN이 복셀 단위로 원상 복귀한다. 인접 바닥 고리 14칸은 두 상태에서 P3 바닥과 동일(CO3, 구조 손상 0).
- **FIND-P6-1 (측정값, 수정 안 함):** OPEN에서 구멍 가장자리(동쪽 열 X=−6)로 옆에서 걸어 들어가면 최대 낙차 4블록(발 높이 −22 → 첫 보이는 단 −26)이다. 바닐라 낙하 피해는 3블록 초과부터이므로 1 피해가 발생할 수 있다. 가장자리 처리(난간·단 모양)는 설계 결정이며 이번에 만들지 않았다.
- **FIND-P6-2 (측정값):** OPEN 상태에서 절개 열에 서 있을 수 있는 위치가 12개다. OPEN→CLOSED 복원 시 그 안에 플레이어가 있을 때의 처리는 미정(BLOCKED). 구현용 기본값을 만들지 않았다.
- CLOSED/OPEN 전환의 **트리거·애니메이션·복원 조건·구현 방식은 미확정**이다. 덮개 후보 B(슬라이딩)·C(피스톤)는 2칸 슬래브 안 12칸 주머니가 홀 바닥과의 분리 두께 1(요구 2)이 되어 슬래브 증축 또는 외부 주머니가 필요하므로(P5.C1) 이번에도 선택하지 않았다.

## 5. P4-B 통합 및 계단 낙차

- P4-B(측방형)는 후보 C 배치의 P1–P3 위에 모델 수준에서 통합된다. 단계 간 겹침 0(홀 관통 체적 18칸 제외), 선언되지 않은 접촉 0, 관통 체적 밖 굴착 0(P5.I1), 단계 간 최소 벽 2 이상(P5.I2).
- 계단 독립 재추출(ST1, P5.S1): 걸어서 얻은 발 높이 열 -22, -23, -24, -25, -26, -27, -28, -29, -30. 하강 8번, 상승 0, 모든 인접 차 1, 계단 위 머리 공간 4 이상. "7단"은 계단 공간의 발판 수이고 첫 칸 1단을 더해 총 낙차 8이다(정의 차이이며 결함 아님).

## 6. Q97 / Q98

**Q97 (사용자 승인 최소 해석만 적용):** 연속 지하 구역의 수평 중심이 일치하지 않는다. 추가 최소 거리·방향·단조성·완전 비중첩 규칙은 만들지 않았다.

| 구간 | 중심 이동 | (Δx, Δz) | 거리 |
| --- | --- | --- | --- |
| L1 → L2 | (22.5, -11.5) → (15.5, 6.0) | (-7.0, +17.5) | 18.85 |
| L2 → L3 | (15.5, 6.0) → (-15.5, -4.0) | (-31.0, -10.0) | 32.57 |
| L3 → HALL | (-15.5, -4.0) → (-21.0, 21.0) | (-5.5, +25.0) | 25.6 |
| HALL → CRYPT | (-21.0, 21.0) → (15.0, -1.0) | (+36.0, -22.0) | 42.19 |
| CRYPT → VAULT | (15.0, -1.0) → (18.0, 26.0) | (+3.0, +27.0) | 27.17 |

판정: PASS (모든 구역 쌍의 중심이 서로 다름 포함). 구역 정의(L1/L2/L3는 해당 층 P2 공간들의 평면 bbox, 홀은 예약 상자, 묘실/창고는 P4 외피)는 이 검증의 입력이며 Q97 자체의 조항이 아니다.

**Q98:** 선택 모델의 *측정값*: 최저 공동 Y −30, 최저 구조 블록 Y −32(복셀·상자 산술 일치), 계산 깊이 32. 이는 현재 모델의 측정값이며 **최종 설계 깊이 상한은 미승인(BLOCKED)** 이다.

## 7. 구현 전 감사 15항목

| # | 항목 | 판정 | 근거 / 비고 |
| --- | --- | --- | --- |
| 1 | 승인 조건 누락 | PASS(정적) | P3 회귀 25 PASS / P4 회귀 16 PASS(V1–V16, Q65–Q96 값) 및 P6 O1–O3·Q97·Q98. Q15 크기·Q16 일부는 권위에 수치가 없어 PROPOSED 유지 |
| 2 | 승인 조건과 좌표 충돌 | PASS | O3, P5.L1, REG34 |
| 3 | 내부/외부 치수 혼동 | PASS(범위 한정) | R1에서 요소마다 내부(공동 bbox)와 외부(정의된 경우)를 분리 기록. P2 외부 Y 방향 두께는 정의 없음(UNVERIFIED) |
| 4 | 구조물 전체 경계 초과 | PASS | P5.X1: ±40 초과 0칸(후보 C). 기존 P2 배치 단독은 X=±41 돌출(해소됨) |
| 5 | 방·통로·계단 연결 오류 | PASS | P5.K2, K3, ST1, BF1 |
| 6 | 공간 중복 | PASS | P5.I1, R1 |
| 7 | 벽체 두께 부족 | PASS(P3-F1 이월) | P5.I2 최소 2(선언된 연결 제외). P3-F1(지붕 2, 벽감 뒤 벽 1 등 여유 0~1)은 기존 열린 항목 그대로 |
| 8 | 비밀 입구 CLOSED/OPEN 간섭 | PASS + 2 findings | CO1–CO6, P5.K1/K4. FIND-P6-1(낙차 4), FIND-P6-2(복원 시 점유) |
| 9 | 계단 높이 불연속 | PASS | ST1, P5.S1 |
| 10 | 보물상자 접근 불가 | PASS | P5.K3: 상자 앞 5칸 모두 도달 |
| 11 | 보스 전투 공간 간섭 | PASS(정적) / UNVERIFIED(실전) | CLOSED 홀이 P3와 복셀 동일(P5.K1). 기존 보스 코드는 블록을 변경하지 않음(`setBlock` 계열 호출 0건, 읽기 전용 확인). BOSS-R1 종결 아님 |
| 12 | 동·서 방향 차이 | PASS | 복셀·요소 거울 동일. 차이는 손 방향뿐(§10). D-3 미확정 |
| 13 | 구현에 필요한 미정 파라미터 | BLOCKED | §9 |
| 14 | 기존 월드젠과의 연결 | BLOCKED(의존성 기록) | §11 |
| 15 | 기존 보스 구현과의 연결 | BLOCKED(의존성 기록) | §11 |

## 8. 음성 대조 테스트

의도적 결함을 넣은 16개 중 16개를 기대한 검사가 검출했다.

| 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- |
| overlay: V12 left at old X (vestibule not shifted) | O2, P5.X1 | O1, O2, P5.K2, P5.K3, P5.P2R, P5.X1 | 검출 |
| overlay: landing LND12 shrunk to 7x9 | O2, O3, P5.L1 | O2, O3, P5.L1 | 검출 |
| overlay: unapproved space B1 also moved | O1, O2 | O1, O2 | 검출 |
| overlay: flight B moved one block too far (breaks the stair-landing/C2 contact) | O2, P5.P2R | O2, P5.K2, P5.K3, P5.P2R, R1 | 검출 |
| cover never placed (closed hall has a hole) | CO1, CO2 | CO1, CO2 | 검출 |
| cover is 13 blocks (one extra floor cell) | CO1, CO2 | CO1, CO2 | 검출 |
| cut widened by one floor cell outside the declared 12 | CO1 | CO1 | 검출 |
| Q97 violated: vault+crypt centred under the hall | Q97 | Q97 | 검출 |
| secret area deeper than the recorded -32 (vault envelope lowered by 4) | Q98 | Q98 | 검출 |
| stair headroom 3 | P5.S1 | CO1, P5.S1 | 검출 |
| last stair tread missing | P5.S1, ST1 | BF1, CO4, P5.K3, P5.S1, ST1 | 검출 |
| registry gap (P2:B2 not registered) | R1 | R1 | 검출 |
| stray P4 void inside the hall reservation (outside the pierce) | P5.I1 | P5.I1, P5.I2 | 검출 |
| vault cross-section walled off by a pile | P5.K3 | BF1, P5.K3 | 검출 |
| air leak: P4 tunnel joined to the L3 passage | CO4, P5.I1 | CO4, P5.I1, P5.I2, P5.K2, P5.K3 | 검출 |
| E/W asymmetry: west crypt shifted one block | EW1 | EW1 | 검출 |

## 9. 구현 전 필수 사용자 결정 (미해결)

`ST01_P6_OPEN_DECISIONS_2026-11-07.md` 참조. 요약: (1) 덮개 CLOSED→OPEN 트리거와 OPEN→CLOSED 복원 규칙·시점·애니메이션, 골렘 안내 실패 시 비잠금 대체 접근 경로; (2) 보스 등장 지점(P3-F4)과 왕실 상자 앵커 좌표 지정; (3) 금 골렘 안내 엔티티 설계(미구현); (4) Q15 묘실 사양과 D-3 방향; (5) Q98 최대 깊이 상한; (6) FIND-P6-1 가장자리 처리; (7) P2 승인 기록/모델에 후보 C 좌표 반영 여부; (8) 보물 더미 채굴 가능 여부·보상 구성.

## 10. 동·서 방향 차이

- 복셀 거울 동치: closed_solid=True, open_solid=True, own=True, elem=True, registry_bbox_mirror=True, turn_sequence_swapped=True, route_length_equal=True. 요소 전부(동·서 동일 개수) bbox가 정확한 X 거울.
- 주 경로(진입광장→거대 상자 앞) 길이: 동 237 = 서 237 걸음.
- 회전열(L=좌, R=우): 동 `LRLRRRLRRLRLRLLLRLLRLRL` / 서 `RLRLLLRLLRLRLRRRLRRLRLR` — 좌우 반전. 즉 차이는 손 방향(좌우)뿐이다. D-3(어느 쪽을 실제 방향으로 쓸지)은 미확정이며 두 쪽 모두 독립 검증했다.

## 11. 기존 구현과의 의존성 (읽기 전용 조사)

**월드젠:** 저장소에는 ST-01 거대 피라미드 구조물/구조 집합/템플릿 풀이 없다(`data/moreminecrafts/structure`: ancient_city, sky_islands; 피라미드 관련 훅 0개). 따라서 NBT 템플릿, 구조 배치(어디에 생성되는지), 지형 정렬, 청크 경계 처리, 구조물 내 `TombGuardianEncounters` 연결은 모두 신규 구현 항목이다.

**보스 (`boss/TombGuardianEncounters.java`):**
- 구조물이 왕실 상자 블록과 상호작용을 소유하고 `onRoyalChestInteraction(level, tombAnchor, spawnPos, …)`를 호출해야 한다. `tombAnchor`(의도: 왕실 상자 위치)와 `spawnPos`(구조물이 사전 검증한 등장 지점)는 구조물이 공급한다 → P3-F4 미정.
- 상자 봉인은 `isRoyalChestSealed`로 조회한다. 상자 블록 쪽에서 이를 호출하는 코드는 아직 없다.
- 처치 이벤트 `31:    public static final Event<Defeated> DEFEATED = EventFactory.createArrayBacked(Defeated.class,` 가 존재한다. 이 이벤트의 구독자가 저장소에 없으므로 비밀 입구 개방, 금 골렘 등장은 신규 구독자 구현이 필요하다.
- 금 골렘 안내 엔티티: 구현 없음(사용자 정의 골렘 엔티티 0개). 권위는 "분리된 안내 엔티티, 보스 사망 처리 변경 금지, 안내 실패 시 비잠금 대체 접근 필요"를 요구한다.
- 보스 코드는 블록을 설치/파괴하지 않는다(바닥 읽기만). 따라서 전투가 덮개/바닥을 바꾸지 않는다는 *코드 수준* 전제가 성립한다. 충격파 반경 6.0, 석상 높이 4.0, 보스 높이 4.35는 `TombGuardianTuning`의 값이며 홀 설계(P3)와의 상호작용은 BOSS-R1 실전 검증 대기이다.

**보스 흐름 대조 (설계 수준):**

| 단계 | 권위/코드 대조 | 상태 |
| --- | --- | --- |
| 1 왕실 묘실 진입 | P3 홀 입구, CLOSED 홀 동일 | 정적 PASS |
| 2 메인 상자 최초 상호작용 | `onRoyalChestInteraction` 존재, 구조물 쪽 호출 없음 | 코드 존재 / 연결 UNVERIFIED |
| 3 Tomb Guardian 등장 | `spawnPos` 구조물 공급 필요(P3-F4 미정) | BLOCKED |
| 4 처치 전 상자 잠금 유지 | `isRoyalChestSealed` 존재, 상자 블록 측 미구현 | UNVERIFIED |
| 5 보스 처치 | `DEFEATED` 이벤트/영속 기록 존재 | 코드 존재 |
| 6 금 골렘으로 축소 | 구현 없음 | BLOCKED |
| 7 비밀 구역 안내 | 구현 없음; 정적 경로 존재(BF1: 상자 접근점→입구 18걸음, 입구→거대 상자 42걸음) | 정적 PASS / 실제 BLOCKED |
| 8 비밀 입구 개방 | 트리거 미확정 | BLOCKED |
| 9 비밀 계단 진입 | OPEN 상태에서 도보 연결, 낙차 findings | 정적 PASS |

권위 간 불일치 메모: 권위 §"gold golem" 5항은 "골렘이 Q16 비밀 보물창고로 안내하는지 별도 보물창고인지 OPEN"이라 하나, 이후 Q63(A)가 "기존 Q16 확장 보물창고로 안내"로 결정했다. 후속 결정이므로 Q63을 따랐고 충돌로 보지 않았다.

## 12. 전체 구조물 도면 (동쪽, 변경 후, OPEN 상태)

범례: `1`=P1, `2`=P2, `3`=P3 홀 공기, `e`=홀 기둥·단 등, `4`=P4 공동, `#`=P4 벽/바닥, 공백=고체. 전체 평면(5개 높이)과 단면(5개)은 `generated/plans/east/`, 서쪽은 `generated/plans/west/`에 있다.

### 12.1 평면 Y=−30 (비밀 구역)
```
INTEGRATED PLAN Y=-30 X[-45,45] Z[-45,45] north(-Z) up, east(+X) right. 1=P1 2=P2 3=P3 hall air e=hall columns/dais 4=P4 #=P4 wall/slab; ' '=solid. (open state)
   Z\X      '         '         '         '         +         '         '         '         '     
 -45                                                                                             
 -44                                                                                             
 -43                                                                                             
 -42                                                                                             
 -41                                                                                             
 -40                                                                                             
 -39                                                                                             
 -38                                                                                             
 -37                                                                                             
 -36                                                                                             
 -35                                                                                             
 -34                                                                                             
 -33                                                                                             
 -32                                                                                             
 -31                                                                                             
 -30                                                                                             
 -29                                                                                             
 -28                                                                                             
 -27                                                                                             
 -26                                                                                             
 -25                                                                                             
 -24                                                                                             
 -23                                                                                             
 -22                                                                                             
 -21                                                                                             
 -20                                                                                             
 -19                                                                                             
 -18                                                                                             
 -17                                                                                             
 -16                                                                                             
 -15                                                                                             
 -14                                                                                             
 -13                                                                                             
 -12                                                                                             
 -11                                                                                             
 -10                                                    #####################                    
  -9                                                    ####444#444#444######                    
  -8                                                    ####444#444#444######                    
  -7                                                    ###444444444444444###                    
  -6                                                    ###444444444444444###                    
  -5                                                    ###444444444444444###                    
  -4                                                    ###444444444444444###                    
  -3                                                    ###444444444444444###                    
  -2                                             ##########444444444444444###                    
  -1                                             ##########444444444444444###                    
  +0                                             ##44444444444444444444444###                    
  +1                                             ##44444444444444444444444###                    
  +2                                             ##44444444444444444444444###                    
  +3                                             ##444#####444444444444444###                    
  +4                                             ##444#####444444444444444###                    
  +5                                             ##444#####444444444444444###                    
  +6                                             ##444######444#444#444######                    
  +7                                             ##444######444#444#444######                    
  +8                                             ##444#######################                    
  +9                                             ##444##                                         
 +10                                             ##444##                                         
 +11                                             ##444##                                         
 +12                                             ##444##                                         
 +13                                             ##444###############################            
 +14                                             ##444###############################            
 +15                                             ##444###############################            
 +16                                             ##444###4444444444444444444444444###            
 +17                                             ##444###4444444444444444444444444###            
 +18                                             ##444###4444444444444444444444444###            
 +19                                             ##444###4444444444444444444444444###            
 +20                                             ##444###4444444444444444444444444###            
 +21                                             ##444###4444444444444444444444444###            
 +22                                           ####444###4444444444444444444444444###            
 +23                                           ####444###4444444444444444444444444###            
 +24                                           ##444444444444444444444444444444444###            
 +25                                           ##444444444444444444444444444444444###            
 +26                                           ##444444444444444444444444444444444###            
 +27                                           ##444444444444444444444444444444444###            
 +28                                           ##444444444444444444444444444444444###            
 +29                                           ##44444###4444444444444444444444444###            
 +30                                           ##44444###4444444444444444444444444###            
 +31                                    #########44444###4444444444444444444444444###            
 +32                                    #########44444###4444444444444444444444444###            
 +33                                    #########44444###4444444444444444444444444###            
 +34                                    #########44444###4444444444444444444444444###            
 +35                                    #########44444###4444444444444444444444444###            
 +36                                    #########44444###4444444444444444444444444###            
 +37                                    #############################################            
 +38                                           ######################################            
 +39                                                  ###############################            
 +40                                                                                             
 +41                                                                                             
 +42                                                                                             
 +43                                                                                             
 +44                                                                                             
 +45                                                                                             
```

### 12.2 평면 Y=−22 (왕실 홀 층)
```
INTEGRATED PLAN Y=-22 X[-45,45] Z[-45,45] north(-Z) up, east(+X) right. 1=P1 2=P2 3=P3 hall air e=hall columns/dais 4=P4 #=P4 wall/slab; ' '=solid. (open state)
   Z\X      '         '         '         '         +         '         '         '         '     
 -45                                                                                             
 -44                                                                                             
 -43                                                                                             
 -42                                                                                             
 -41                                                                                             
 -40                                                                                             
 -39                                                                                             
 -38                                                                                             
 -37                                                                                             
 -36                                                                                             
 -35                                                                                             
 -34                                                                                             
 -33                                                                                             
 -32                                                                                             
 -31                                                                                             
 -30                                                                                             
 -29                                                                                             
 -28                                                                                             
 -27                                                                                             
 -26                                                                                             
 -25                                                                                             
 -24                                                                                             
 -23                                                                                             
 -22                                                                                             
 -21                                                                                             
 -20                                                                                             
 -19                                                                                             
 -18                                                                                             
 -17                                                                                             
 -16                                                                                             
 -15                                                                                             
 -14                                                                                             
 -13                                                                                             
 -12                 2222222222222222222                                                         
 -11                 2222222222222222222                                                         
 -10                 2222222222222222222                #####################                    
  -9                 2222222222222222222222222222222    #####################                    
  -8                 2222222222222222222222222222222    #####################                    
  -7                 2222222222222222222222222222222    #####################                    
  -6                 2222222222222222222222222222222    #####################                    
  -5                 2222222222222222222222222222222    #####################                    
  -4                 2222222222222222222        2222    #####################                    
  -3                 2222222222222222222        2222    #####################                    
  -2                 2222222222222222222                #####################                    
  -1                         222                        #####################                    
  +0                         222                        #####################                    
  +1                         222                        #####################                    
  +2                         222                        #####################                    
  +3                         222                        #####################                    
  +4                     ee  222  ee                    #####################                    
  +5          333333333333333333333333333333333         #####################                    
  +6          333333333333333333333333333333333         #####################                    
  +7          33333eeeee3333333333333eeeee33333         #####################                    
  +8          33333eeeee3333333333333eeeee33333         #####################                    
  +9          33333eeeee3333333333333eeeee33333                                                  
 +10          33eee3eeee3333333333333eeee3eee33                                                  
 +11          33eeee333333333333333333333eeee33                                                  
 +12          33eeee333333333333333333333eeee33                                                  
 +13          33eeee333333333333333333333eeee33       ###############################            
 +14          33eeee333333333333333333333eeee33       ###############################            
 +15        3e333333333333333333333333333333333e3     ###############################            
 +16        e33333333333333333333333333333333333e     ###############################            
 +17        e33333333333333333333333333333333333e     ###############################            
 +18        3e333333333333333333333333333333333e3     ###############################            
 +19          33eeee33333333eeeee33333333eeee33       ###4444444444444444444444444###            
 +20          33eeee33333333eeeee33333333eeee33       ###4444444444444444444444444###            
 +21          33eeee33333333eeeee33333333eeee33       ###4444444444444444444444444###            
 +22          33eeee33333333eeeee33333333eeee33       ###4444444444444444444444444###            
 +23          33eeee33333333eeeee33333333eeee33       ###4444444444444444444444444###            
 +24        3e333333333333333333333333333333333e3     ###4444444444444444444444444###            
 +25        e33333333333333333333333333333333333e     ###4444444444444444444444444###            
 +26        e33333333333333333333333333333333333e     ###4444444444444444444444444###            
 +27        3e333333333333333333333333333333333e3     ###4444444444444444444444444###            
 +28          33eeee333333333333333333333eeee33       ###4444444444444444444444444###            
 +29          33eeee333333333333333333333eeee33       ###4444444444444444444444444###            
 +30          33eeee333333eeeeeeeee333333eeee33       ###4444444444444444444444444###            
 +31          33eeee333333eeeeeeeee333333eeee33####   ###4444444444444444444444444###            
 +32          33eee3eeee3eeeeeeeeeee3eeee3eee33####   ###4444444444444444444444444###            
 +33          33333eeeee3eeeeeeeeeee3eeeee44433####   ###4444444444444444444444444###            
 +34          33333eeeee3eeeeeeeeeee3eeeee44433####   ###############################            
 +35          33333eeeee3eeeeeeeeeee3eeeee44433####   ###############################            
 +36          33333333333eeeeeeeeeee33333333333####   ###############################            
 +37          33333333333eeeeeeeeeee33333333333####   ###############################            
 +38            ee                         ee         ###############################            
 +39                                                  ###############################            
 +40                                                                                             
 +41                                                                                             
 +42                                                                                             
 +43                                                                                             
 +44                                                                                             
 +45                                                                                             
```

### 12.3 수직 단면 X=−21 (홀 중심축)
```
INTEGRATED SECTION X=-21 horizontal Z -45..45, Y -40..8 (open state)
  +8                                                                                             
  +7                                                                                             
  +6                                                                                             
  +5                                                                                             
  +4                                                                                             
  +3                                                                                             
  +2                                                                                             
  +1                                                                                             
  +0                                                                                             
  -1                                                                                             
  -2                                                                                             
  -3                                                                                             
  -4                                                                                             
  -5                                                                                             
  -6                                                                                             
  -7                                                                    3                        
  -8                                                                  33333                      
  -9                                                                333333333                    
 -10                                                              333333e333333                  
 -11                                                            33333333e33333333                
 -12                                                          333333333eee333333333              
 -13                                   22222222222          33333333333eee33333333333            
 -14                                   22222222222        3333333333333eee3333333333333          
 -15                                   22222222222      333333333333333eee333333333333333        
 -16                                   22222222222      333333333333333eee333333333333333        
 -17                                   22222222222      333333333333333eee333333333333333        
 -18                                   22222222222      333333333333333eee333333333333333        
 -19                                   22222222222      333333333333333eee33333333333eeee        
 -20                                   22222222222222222333333333333333eee3333333333eeeee        
 -21                                   2222222222222222233333333333333eeeee33333333eeeeee        
 -22                                   2222222222222222233333333333333eeeee333333eeeeeeee        
 -23                                                                                             
 -24                                                                                             
 -25                                                                                             
 -26                                                                                             
 -27                                                                                             
 -28                                                                                             
 -29                                                                                             
 -30                                                                                             
 -31                                                                                             
 -32                                                                                             
 -33                                                                                             
 -34                                                                                             
 -35                                                                                             
 -36                                                                                             
 -37                                                                                             
 -38                                                                                             
 -39                                                                                             
 -40                                                                                             
```

(숨은 계단 축 Z=35 단면은 `generated/plans/east/section_z35.txt`.)

## 13. 구현 준비 가능 여부

**구현 준비 불가(BLOCKED).** 좌표 사양은 정적으로 정합하지만, §9의 결정이 없으면 (a) 비밀 입구 개방 규칙, (b) 보스 등장 지점과 상자 앵커, (c) 골렘 안내 설계를 구현할 수 없고, 월드젠 구조 배치가 저장소에 전혀 없다. 좌표 사양 자체는 구현 단계 인계 문서(`ST01_P6_IMPLEMENTATION_HANDOFF_2026-11-07.md`)에서 입력으로 사용할 수 있다.

## 14. 검증 증거 전체 (동쪽)

| ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- |
| O1 | Overlay touches exactly the user-listed candidate-C components (V12, FA, LND12, FB, C1, C2, BRB1); every other P2 space, link, port, goal and P1 box is unchanged | PASS | changed spaces ['BRB1', 'C1', 'C2', 'FA', 'FB', 'LND12', 'V12']; changed non-space keys []; unchanged spaces 19 |
| O2 | Overlay data equals the live P5 candidate-C definition (overlay not stale, not hand-typed) | PASS | overlay spaces ['BRB1', 'C1', 'C2', 'FA', 'FB', 'LND12', 'V12']; equals fresh extraction True |
| O3 | Approved dimensions preserved after the overlay: every P2 space keeps its bbox size (C1/C2 only shorten in length, height/width fixed), landing 9x9, V12 9x11, flights 7 wide with 4 treads | PASS | dimension changes []; LND12 (9, 9), V12 (9, 11), flight widths {'FA': 7, 'FB': 7}, tread counts {'FA': 4, 'FB': 4}; C2 length 13 -> 11 |
| P5.K1 | Closed state: the royal hall (all cells of the P3 reservation at floor-top level and above) is voxel-identical to the P3 model - safe walking surface, no hole, no change to columns/dais/obelisk/arena | PASS | differing cells 0; cover cells placed 12; hidden cavity cells left below the cover (inside the 2-thick slab zone, y<-23) 12 |
| P5.K4 | Open state: the only change to the hall at/above the floor-top level is the stair cut implied by the P4 stair boxes (voxel diff == analytic box intersection), inside the declared pierce volume, no extra holes | PASS | changed cells 12 (analytic 12) all inside pierce True; cut footprint at floor-top layer 12 cells |
| P5.K2 | Closed state: forecourt -> P1 hall -> P1 descent -> P2 L1-L3 -> royal hall (entrance, dais) all walkable; no walkable cell of the secret area is reachable (player cannot fall into / enter the stair) | PASS | unreachable hall goals []; secret-area walkable cells reachable below the hall floor 0; steps forecourt->hall entry 154, ->dais top 185; total walkable states 3876 |
| P5.K3 | Open state: complete chain forecourt -> ... -> royal hall -> hidden stair -> landing(s) -> vault entrance -> aisle -> dais stair -> chest front (5 cells) and the crypt are reachable on foot | PASS | unreachable []; chain length forecourt->chest front 237 steps; goals checked 12 |
| P5.S1 | Independent stair drop check: shortest walk hall floor -> vault entrance has only unit steps, no rise, no missing first/last step, consecutive heights, headroom >= 4 on stair cells; drop = number of unit descents | PASS | start Y -22 -> end Y -30; total drop 8; unit descents 8, rises 0, max |dy| 1; heights visited [-22, -23, -24, -25, -26, -27, -28, -29, -30]; stair-space treads (P4 definition) 7; descents per space {'FA': 7, 'LA': 1}; min headroom on stair cells 4, along whole path 4 |
| P5.I1 | Whole-structure collision/contact scan on one combined voxel world (P1 context, P2, P3 hall cavity, P4): no overlap except the declared hatch pierce, face contacts only at the declared links (P1<->P2 landing, P2 port<->hall, hall<->stair cut) | PASS | overlap voxels per stage pair {(1, 2):0, (1, 3):0, (1, 4):0, (2, 3):0, (2, 4):0, (3, 4):18}; pierce overlap (3,4) 18, outside the pierce 0; face-contact voxels per pair {(1, 2):35, (1, 3):0, (1, 4):0, (2, 3):9, (2, 4):0, (3, 4):42}; unexpected contact pairs []; P4 internal overlaps 0; carved inside HALL_RES outside pierce 0 |
| P5.P2R | Re-run of the P2 engine on the P2 layout used in this integrated world (intra-P2 collisions, undeclared contacts, widths, reachability) | PASS | 15 P2 engine checks, not PASS: []; P2 cavity overlaps 0 |
| P5.I2 | Analytic (box arithmetic) minimum wall between cavities of different stages is >= 2 except at the declared links; P4 spaces other than the stair cut keep >= 2 to the hall | PASS | stage-pair minimum Chebyshev gaps {(1, 2):0, (1, 3):6, (1, 4):16, (2, 3):0, (2, 4):3, (3, 4):0}; hall<->(P4 except FA) 2; violations {} |
| P5.X1 | Whole generated underground structure (P2 cavities + wall 2, P3 reservation, P4 vault/crypt/corridor structures) stays inside X/Z +-40 (D01 whole-structure reading) | PASS | max excess 0 blocks; voxels outside 0; P2 spaces whose wall-inclusive extent exceeds 40: [] |
| P5.D1 | Lowest cavity and lowest structure block of the whole integrated structure recomputed from boxes and from voxels (Q98 input; not an approved maximum) | PASS | lowest P4 cavity Y -30 (box -30); lowest cavity overall -30; lowest structure block -32 = depth 32 |
| P5.L1 | P2 transition spaces keep the approved-by-P2 proportions: landings 9x9, L1/L2 vestibule 9x11, 7-wide stair flights | PASS | landings ((9, 9), (9, 9)), V12 (9, 11), flight A width 7 |
| P5.C1 | Cover candidates: spatial requirement of block removal (A), sliding panel (B), piston/structure (C) in the 2-thick hall slab | UNVERIFIED | cut at floor-top layer = 12 blocks. A needs 0 extra cells. B/C need >= 12 pocket cells beside the cut; a pocket in the lower slab layer (Y -24) leaves 1 solid layer(s) to the hall floor air (needs >= 2), so B/C require a thicker slab or outside-footprint pocket (not designed). Mechanism feasibility/in-game behaviour not verifiable here. |
| R1 | Element registry coverage: independently voxelised registry (P1/P2 incl. overlay, P4) equals the builder-made cavity masks cell for cell; every cavity voxel belongs to exactly one registered element (no orphan, no double ownership); every element has id, stage, bbox, dims, status, evidence | PASS | 141 registered elements; P1+P2 cavity diff 0 cells; P4 cavity diff 0 cells; cells owned by >1 element 0 |
| CO1 | Cover region = exactly the 12-block cut (4 x 3 at the slab-top layer, analytic position from the P5-verified stair box), contiguous rectangle, inside the pierce volume | PASS | cells 12; X (-9, -6) Z (33, 35) Y [-23]; expected X (-9, -6) Z (33, 35) Y -23 |
| CO2 | Transition algebra: OPEN->CLOSED places exactly the cover cells, CLOSED->OPEN removes exactly them, OPEN->CLOSED->OPEN restores the original voxel for voxel; no other cell changes in either direction | PASS | changed cells per transition 12; closed==open+cover True; reopen==open True |
| CO3 | Static damage check: the floor ring around the cut (same layer) is untouched P3 floor in OPEN and CLOSED; removing/restoring the cover does not change any neighbouring cell | PASS | ring cells 14, solid in OPEN 14; cells differing from the P3 floor 0 |
| CO4 | Air/entity seal: in CLOSED the hall's air volume (6-neighbour flood fill from the hall centre) reaches 0 cells of the secret area; in OPEN it reaches the vault interior (design-level proxy for light/fluid/mob leakage) | PASS | CLOSED: secret-area cells at/below the cover layer reached 0 (stair-headroom cells above the floor are hall air); OPEN: vault cells reached 3597/4800 |
| CO5 | OPEN state fall exposure (measured, not judged): maximum vertical drop for a player stepping sideways from the hall floor into the open cut column | PASS | max drop 4 blocks (vanilla fall damage begins above 3); per-column {(-9, 33):1, (-9, 34):1, (-9, 35):1, (-8, 33):2, (-8, 35):2, (-7, 33):3, (-7, 35):3, (-6, 33):4, (-6, 34):4, (-6, 35):4} -> design finding FIND-P6-1 if > 3 (hazard edge or cover-rail decision is a user decision; NOT corrected) |
| CO6 | Restore hazard (measured): number of standable positions inside the cut columns in OPEN, i.e. where a player/entity could be when a restore is attempted | PASS | 12 standable positions in the 12 cut columns -> restore rule is a BLOCKED user decision (no default invented) |
| ST1 | P4-B stair independent re-extraction on the layout-C world: walked heights hall floor -> vault entrance are consecutive (no gap/duplicate level), total drop 8 = 7 treads + 1 landing step | PASS | heights [-22, -23, -24, -25, -26, -27, -28, -29, -30]; drop 8; heights on stair-space cells [-23, -24, -25, -26, -27, -28, -29] |
| BF1 | Static guide-route proxy: walkable route exists in OPEN from the main-chest approach cell to the hidden-stair entry and on to the giant-chest front (1-wide agent, headroom >= 2); guardian emergence candidates keep a measured distance from the cut | PASS | main-chest approach -> hatch 18 steps; hatch -> giant chest front 42 steps; min headroom on guide route 2; distance from emergence candidates to nearest cut cell {'BOSS_S': 13.4, 'BOSS_N': 21.6, 'BOSS_E': 13.4, 'BOSS_W': 21.6} (gameplay meaning UNVERIFIED, BOSS-R1 open) |
| Q97 | Q97 minimum reading (user-approved): centres of consecutive underground zones L1, L2, L3, hall, crypt, vault differ horizontally (no stacked-centre pair); no extra distance/direction/monotonic/non-overlap rule is applied | PASS | L1->L2 d=(-7.0,+17.5) |18.85|; L2->L3 d=(-31.0,-10.0) |32.57|; L3->HALL d=(-5.5,+25.0) |25.6|; HALL->CRYPT d=(+36.0,-22.0) |42.19|; CRYPT->VAULT d=(+3.0,+27.0) |27.17|; all-pairs distinct centres True |
| Q98 | Measured depth of the selected layout-C + P4-B model: lowest cavity voxel, lowest structure (envelope) voxel and box arithmetic agree and equal the P5-reported -32 (a measured value, NOT an approved depth cap) | PASS | lowest cavity Y -30; lowest structure Y -32 (voxel) / -32 (box); baseline -32; depth 32; design depth ceiling: UNAPPROVED (BLOCKED, Q98) |
| REG34 | Regression: the unmodified P3 and P4 verifiers re-run on the layout-C world (reported separately, NOT summed into the P6 verdict) | PASS | P3 verifier: 25 PASS / 0 FAIL [] / 1 UNV; P4 verifier: 16 PASS / 0 FAIL [] / 2 UNV |

### 서쪽

| ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- |
| O1 | Overlay touches exactly the user-listed candidate-C components (V12, FA, LND12, FB, C1, C2, BRB1); every other P2 space, link, port, goal and P1 box is unchanged | PASS | changed spaces ['BRB1', 'C1', 'C2', 'FA', 'FB', 'LND12', 'V12']; changed non-space keys []; unchanged spaces 19 |
| O2 | Overlay data equals the live P5 candidate-C definition (overlay not stale, not hand-typed) | PASS | overlay spaces ['BRB1', 'C1', 'C2', 'FA', 'FB', 'LND12', 'V12']; equals fresh extraction True |
| O3 | Approved dimensions preserved after the overlay: every P2 space keeps its bbox size (C1/C2 only shorten in length, height/width fixed), landing 9x9, V12 9x11, flights 7 wide with 4 treads | PASS | dimension changes []; LND12 (9, 9), V12 (9, 11), flight widths {'FA': 7, 'FB': 7}, tread counts {'FA': 4, 'FB': 4}; C2 length 13 -> 11 |
| P5.K1 | Closed state: the royal hall (all cells of the P3 reservation at floor-top level and above) is voxel-identical to the P3 model - safe walking surface, no hole, no change to columns/dais/obelisk/arena | PASS | differing cells 0; cover cells placed 12; hidden cavity cells left below the cover (inside the 2-thick slab zone, y<-23) 12 |
| P5.K4 | Open state: the only change to the hall at/above the floor-top level is the stair cut implied by the P4 stair boxes (voxel diff == analytic box intersection), inside the declared pierce volume, no extra holes | PASS | changed cells 12 (analytic 12) all inside pierce True; cut footprint at floor-top layer 12 cells |
| P5.K2 | Closed state: forecourt -> P1 hall -> P1 descent -> P2 L1-L3 -> royal hall (entrance, dais) all walkable; no walkable cell of the secret area is reachable (player cannot fall into / enter the stair) | PASS | unreachable hall goals []; secret-area walkable cells reachable below the hall floor 0; steps forecourt->hall entry 154, ->dais top 185; total walkable states 3876 |
| P5.K3 | Open state: complete chain forecourt -> ... -> royal hall -> hidden stair -> landing(s) -> vault entrance -> aisle -> dais stair -> chest front (5 cells) and the crypt are reachable on foot | PASS | unreachable []; chain length forecourt->chest front 237 steps; goals checked 12 |
| P5.S1 | Independent stair drop check: shortest walk hall floor -> vault entrance has only unit steps, no rise, no missing first/last step, consecutive heights, headroom >= 4 on stair cells; drop = number of unit descents | PASS | start Y -22 -> end Y -30; total drop 8; unit descents 8, rises 0, max |dy| 1; heights visited [-22, -23, -24, -25, -26, -27, -28, -29, -30]; stair-space treads (P4 definition) 7; descents per space {'FA': 7, 'LA': 1}; min headroom on stair cells 4, along whole path 4 |
| P5.I1 | Whole-structure collision/contact scan on one combined voxel world (P1 context, P2, P3 hall cavity, P4): no overlap except the declared hatch pierce, face contacts only at the declared links (P1<->P2 landing, P2 port<->hall, hall<->stair cut) | PASS | overlap voxels per stage pair {(1, 2):0, (1, 3):0, (1, 4):0, (2, 3):0, (2, 4):0, (3, 4):18}; pierce overlap (3,4) 18, outside the pierce 0; face-contact voxels per pair {(1, 2):35, (1, 3):0, (1, 4):0, (2, 3):9, (2, 4):0, (3, 4):42}; unexpected contact pairs []; P4 internal overlaps 0; carved inside HALL_RES outside pierce 0 |
| P5.P2R | Re-run of the P2 engine on the P2 layout used in this integrated world (intra-P2 collisions, undeclared contacts, widths, reachability) | PASS | 15 P2 engine checks, not PASS: []; P2 cavity overlaps 0 |
| P5.I2 | Analytic (box arithmetic) minimum wall between cavities of different stages is >= 2 except at the declared links; P4 spaces other than the stair cut keep >= 2 to the hall | PASS | stage-pair minimum Chebyshev gaps {(1, 2):0, (1, 3):6, (1, 4):16, (2, 3):0, (2, 4):3, (3, 4):0}; hall<->(P4 except FA) 2; violations {} |
| P5.X1 | Whole generated underground structure (P2 cavities + wall 2, P3 reservation, P4 vault/crypt/corridor structures) stays inside X/Z +-40 (D01 whole-structure reading) | PASS | max excess 0 blocks; voxels outside 0; P2 spaces whose wall-inclusive extent exceeds 40: [] |
| P5.D1 | Lowest cavity and lowest structure block of the whole integrated structure recomputed from boxes and from voxels (Q98 input; not an approved maximum) | PASS | lowest P4 cavity Y -30 (box -30); lowest cavity overall -30; lowest structure block -32 = depth 32 |
| P5.L1 | P2 transition spaces keep the approved-by-P2 proportions: landings 9x9, L1/L2 vestibule 9x11, 7-wide stair flights | PASS | landings ((9, 9), (9, 9)), V12 (9, 11), flight A width 7 |
| P5.C1 | Cover candidates: spatial requirement of block removal (A), sliding panel (B), piston/structure (C) in the 2-thick hall slab | UNVERIFIED | cut at floor-top layer = 12 blocks. A needs 0 extra cells. B/C need >= 12 pocket cells beside the cut; a pocket in the lower slab layer (Y -24) leaves 1 solid layer(s) to the hall floor air (needs >= 2), so B/C require a thicker slab or outside-footprint pocket (not designed). Mechanism feasibility/in-game behaviour not verifiable here. |
| R1 | Element registry coverage: independently voxelised registry (P1/P2 incl. overlay, P4) equals the builder-made cavity masks cell for cell; every cavity voxel belongs to exactly one registered element (no orphan, no double ownership); every element has id, stage, bbox, dims, status, evidence | PASS | 141 registered elements; P1+P2 cavity diff 0 cells; P4 cavity diff 0 cells; cells owned by >1 element 0 |
| CO1 | Cover region = exactly the 12-block cut (4 x 3 at the slab-top layer, analytic position from the P5-verified stair box), contiguous rectangle, inside the pierce volume | PASS | cells 12; X (6, 9) Z (33, 35) Y [-23]; expected X (6, 9) Z (33, 35) Y -23 |
| CO2 | Transition algebra: OPEN->CLOSED places exactly the cover cells, CLOSED->OPEN removes exactly them, OPEN->CLOSED->OPEN restores the original voxel for voxel; no other cell changes in either direction | PASS | changed cells per transition 12; closed==open+cover True; reopen==open True |
| CO3 | Static damage check: the floor ring around the cut (same layer) is untouched P3 floor in OPEN and CLOSED; removing/restoring the cover does not change any neighbouring cell | PASS | ring cells 14, solid in OPEN 14; cells differing from the P3 floor 0 |
| CO4 | Air/entity seal: in CLOSED the hall's air volume (6-neighbour flood fill from the hall centre) reaches 0 cells of the secret area; in OPEN it reaches the vault interior (design-level proxy for light/fluid/mob leakage) | PASS | CLOSED: secret-area cells at/below the cover layer reached 0 (stair-headroom cells above the floor are hall air); OPEN: vault cells reached 3597/4800 |
| CO5 | OPEN state fall exposure (measured, not judged): maximum vertical drop for a player stepping sideways from the hall floor into the open cut column | PASS | max drop 4 blocks (vanilla fall damage begins above 3); per-column {(6, 33):4, (6, 34):4, (6, 35):4, (7, 33):3, (7, 35):3, (8, 33):2, (8, 35):2, (9, 33):1, (9, 34):1, (9, 35):1} -> design finding FIND-P6-1 if > 3 (hazard edge or cover-rail decision is a user decision; NOT corrected) |
| CO6 | Restore hazard (measured): number of standable positions inside the cut columns in OPEN, i.e. where a player/entity could be when a restore is attempted | PASS | 12 standable positions in the 12 cut columns -> restore rule is a BLOCKED user decision (no default invented) |
| ST1 | P4-B stair independent re-extraction on the layout-C world: walked heights hall floor -> vault entrance are consecutive (no gap/duplicate level), total drop 8 = 7 treads + 1 landing step | PASS | heights [-22, -23, -24, -25, -26, -27, -28, -29, -30]; drop 8; heights on stair-space cells [-23, -24, -25, -26, -27, -28, -29] |
| BF1 | Static guide-route proxy: walkable route exists in OPEN from the main-chest approach cell to the hidden-stair entry and on to the giant-chest front (1-wide agent, headroom >= 2); guardian emergence candidates keep a measured distance from the cut | PASS | main-chest approach -> hatch 18 steps; hatch -> giant chest front 42 steps; min headroom on guide route 2; distance from emergence candidates to nearest cut cell {'BOSS_S': 13.4, 'BOSS_N': 21.6, 'BOSS_E': 13.4, 'BOSS_W': 21.6} (gameplay meaning UNVERIFIED, BOSS-R1 open) |
| Q97 | Q97 minimum reading (user-approved): centres of consecutive underground zones L1, L2, L3, hall, crypt, vault differ horizontally (no stacked-centre pair); no extra distance/direction/monotonic/non-overlap rule is applied | PASS | L1->L2 d=(+7.0,+17.5) |18.85|; L2->L3 d=(+31.0,-10.0) |32.57|; L3->HALL d=(+5.5,+25.0) |25.6|; HALL->CRYPT d=(-36.0,-22.0) |42.19|; CRYPT->VAULT d=(-3.0,+27.0) |27.17|; all-pairs distinct centres True |
| Q98 | Measured depth of the selected layout-C + P4-B model: lowest cavity voxel, lowest structure (envelope) voxel and box arithmetic agree and equal the P5-reported -32 (a measured value, NOT an approved depth cap) | PASS | lowest cavity Y -30; lowest structure Y -32 (voxel) / -32 (box); baseline -32; depth 32; design depth ceiling: UNAPPROVED (BLOCKED, Q98) |
| REG34 | Regression: the unmodified P3 and P4 verifiers re-run on the layout-C world (reported separately, NOT summed into the P6 verdict) | PASS | P3 verifier: 25 PASS / 0 FAIL [] / 1 UNV; P4 verifier: 16 PASS / 0 FAIL [] / 2 UNV |

## 15. 범위 밖 발견

- 없음. P1 기록의 O-1(권위 인덱스가 승인 기록을 가리키도록 하는 갱신)은 여전히 별도 승인 작업으로 남아 있다.

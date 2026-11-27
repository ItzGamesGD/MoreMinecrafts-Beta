# ST-01 P4 — 비밀 구역(숨은 계단·비밀 묘실·비밀 보물창고) 3D 좌표 설계 및 적층형/측방형·동/서 검증 (2026-11-07)

**상태: PROPOSED 설계 + 자동 검증 결과. 설계 권위가 아니며 실제 건축·게임 내 검증·구현(NBT·월드젠·보스·보상)의 승인이 아닙니다.** D-3(동·서 회전)은 **확정하지 않았고** 후보 A·B 각각을 동쪽·서쪽으로 독립 검증했습니다. 어느 후보도 선택하지 않았습니다. Q 번호가 명시된 값을 제외한 모든 수치는 **PROPOSED**이며, 특히 **비밀 묘실(Q15)의 크기·개수는 권위 문서에 존재하지 않아** 아래 치수는 승인 조건이 아닌 대표 후보입니다.

## 0. 요약

- **후보 A (적층)**: 보물창고를 왕실 홀 바로 아래(내부 바닥 Y −37, P2 `VAULT_A` 예약과 동일 체적)에 둠. **후보 B (측방)**: 보물창고를 홀 동쪽에 접해(내부 바닥 Y −30) 둠.
- 결과: A 동 16 PASS / 0 FAIL / 2 UNVERIFIED, A 서 16 / 0 / 2; B 동 16 PASS / 0 FAIL / 2 UNVERIFIED, B 서 16 / 0 / 2. 서쪽은 동쪽의 정확한 X 거울을 다시 복셀화해 같은 검증기를 따로 실행했고 복셀 단위로 일치합니다(A: solid=True, own=True, elem=True, env=True; B: solid=True, own=True, elem=True, env=True).
- 음성 대조: 결함을 일부러 주입한 스펙 19개 중 19개 검출, 대조군은 FAIL 0. P3 홀은 수정 없이 재검증해 동 25 PASS / 0 FAIL, 서 25 PASS / 0 FAIL 유지.
- **±40 초과: 두 후보 모두 0**(벽체·바닥·지붕·통로 벽 2 포함). A는 X=−40 면 여유 0(서쪽은 +40).
- **최저 깊이**: A 구조 최저 Y −39(깊이 39), B 최저 Y −32(깊이 32). **최대 깊이는 확정하지 않았습니다**(후보 선택·비밀 묘실 크기 미정, Q98).
- **Q97 수평 엇갈림**: A는 보물창고 평면의 100%가 홀 바로 아래라서 *적층*이고, B는 0%로 *엇갈림*. 'staggered'의 해석은 소유자 결정(UNVERIFIED).
- 새 필수 퍼즐·방은 추가하지 않았습니다(연결 통로·계단·참, Q15 묘실만).
- 해소가 필요한 설계 사실: 숨은 계단은 **P3 비밀 입구 구역(5칸)을 열린 단면으로 파고** 홀 동쪽 벽 아래로 빠집니다. 덮개 메커니즘이 정해지기 전까지 홀 바닥의 열린 구멍은 낙하 위험입니다.

## 1. 기준 및 감사

| 항목 | 값 |
| --- | --- |
| 저장소 / 작업 브랜치 | `ItzGamesGD/MoreMinecrafts-Beta` / `work/st01-p4-secret-area-20261107` |
| 기준 브랜치·커밋 | `work/st01-p3-royal-tomb-20261106` @ `37fed9a621e1d333c83117f35fdde9004a77347a` (시작 시 작업 트리 깨끗, origin 일치) |
| 설계 권위 | `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md` (무변경) |
| 승인 기록 | `docs/design/st01-p3/ST01_P3_CONDITIONAL_APPROVAL_RECORD_2026-11-07.md` (신규) |
| 비밀 묘실 크기 확정 여부 | **없음**(권위 문서 Q15: "optional separate burial zone; exact loot undecided"; 전역 검색에서 크기 규정 없음). 후보 치수는 PROPOSED |

좌표계는 P1–P3와 동일(원점 = 피라미드 지면 중심, X 동+, Z 남+, Y 층 인덱스, 범위 양 끝 포함). 아래 표는 모두 동쪽 후보이고 서쪽은 X 부호 반전입니다.

## 2. 승인 조건 대응

| Q | 조건 | 이 설계 | 검사 |
| --- | --- | --- | --- |
| Q13/Q14 | 숨은 하강 계단, 방향 전환 + 참, 충분한 길이 | 1:1 계단 2~3 구간 + 5×5 참, 낙차 A 15 / B 8 | V9, V7 |
| Q15 | 중앙 석관 + 측면 매장 벽감(선택 구역) | 대표 묘실 PROPOSED: 내부 13×15×7(A) / 15×13×7(B), 석관 3×5×2, 벽감 6개(개수 미승인) | V10, V11 |
| Q16/Q63 | Q16 확장 = 대형 보물창고, 골렘 안내 | 보물창고 1개(방 추가 없음) | V1 |
| Q65 | 내부 21×25 | 21×25 (A: 폭 25·깊이 21 / B: 폭 21·깊이 25) | V1 |
| Q66–Q68 | 최대 10, 계단식 천장, 측면 8/중간 9/중앙 10 | 중앙 9폭 10, 중간 폭 8(A)/6(B) 9, 측면 폭 8/6 8 *(경계 PROPOSED)* | V1, V2 |
| Q64/Q69/Q70 | 후방 단, 양쪽 불규칙 광물 더미, 중앙 통로 ≥5 | 더미 seed 18, 좌우 불균형 0.6% (A) / seed 24, 좌우 불균형 1.4% (B), 통로 5 | V8, V3 |
| Q71, Q85, Q89–Q91 | 단 높이 3, 15×11, 정면 계단 폭 7·런 5(외부 2 + 내부 3) | 동일 | V4 |
| Q72, Q92, Q93 | 상자 본체 5×3, 후방 중앙, 받침 최대 7×5 | 본체 5×3(높이 PROPOSED 4 = 받침 1 + 몸 2 + 뚜껑 1), 받침 7×5 | V6 |
| Q80–Q88, Q96 | 단 모서리 기둥 4개 3×3, 높이 7, 주두 ≤5×5, 2×2 중첩·1칸 돌출, 바닥 기초, 얇은 보 | 동일(주두는 후벽 쪽 잘림) | V5, V5b |
| Q95 | 5폭 통로·상자 접근·뚜껑 우선, 장식은 남는 공간 | 상자 정면 5칸 접근, 뚜껑 위 2칸 공기 | V6, V7b |
| Q97 | 엇갈림 연장 | A 적층(100%), B 엇갈림(0%) — 해석 미정 | V15 |
| Q98 | 최대 깊이는 3D 적합 계산 후 | 후보별 계산값만 제시, 확정하지 않음 | V13, §7 |
| Q99 | 사암 주체, 금·청금석·청록 강조(홀·창고에 집중) | 재질·강조색은 리소스 영역 — 미구현 | — |
| 상자 구현 | 일반 상자와 동일한 인벤토리·열기 동작 | 구현하지 않음. 뚜껑 열림 공간만 예약 | V6 |

## 3. 설계 구조 (공통)

### 3.1 홀 연결부 (P3 입력)

P3 비밀 입구 구역(X −9..−5, Z 33..37, Y −22..−19 + 슬래브 Y −24..−23)을 **관통 체적**으로 사용합니다. 숨은 계단은 이 구역의 Z 33..35(폭 3)에서 **동쪽으로** 내려가 홀 동쪽 벽(X −4..−2) 아래를 지납니다.

이유(계산): 홀 예약 `HALL_RES`는 Y −24까지이므로 관통 체적 밖에서는 계단 공동의 윗면이 Y ≤ −25여야 합니다(공동–홀 공동 간격 ≥2). 숨은 계단 머리 높이를 4로 두면 발판 발이 Y ≤ −28이어야 하고 이는 6번째 발판입니다. 구역의 N–S 길이는 5라서 그 안에서 도달할 수 없고, 구역의 동쪽 끝에서 같은 방향으로 계속 내려가면 6번째 발판이 홀 동쪽 벽 아래(X −4)에 놓입니다. 서쪽(홀 안쪽 아래) 방향은 참 5×5가 후보 A의 `VAULT_A` 동벽(X ≥ −14)과 겹쳐 배제했습니다(수작업 검토, 자동 검증 대상 아님).

계단 폭 3, 머리 높이 4, 참 5×5, 통로 폭 5는 모두 PROPOSED입니다(권위에 규정 없음). **열린 단면**: 이 설계는 구역 안 Z 33..35 × X −9..−5의 바닥을 파낸 상태(열림)로 모델링했습니다. P3 모델은 닫힌 상태이며 덮개 메커니즘은 설계하지 않았습니다.

### 3.2 보물창고 모듈 (A·B 공통, 국소 좌표 a=입구 쪽 0 → 후벽, b=축에서의 횡 오프셋)

- 내부 21×25, 중앙 폭 9는 높이 10, 중간 구역 높이 9, 측면 구역 높이 8. 외벽 두께 3, 바닥·지붕 2.
- 입구 통로 5폭·6높이, 벽 관통 길이 3.
- 후방 단 15(b −7..7)×11(a D−12..D−2)×3, 후벽과 1칸 띄움, 정면 계단 폭 7·런 5(바깥 a D−14, D−13: 높이 1,1 / 내부 D−12, D−11, D−10: 2,2,3).
- 기둥 4개: 중심 (a D−2, D−12) × (b ±7). 몸체 3×3, 받침 1 + 몸 4 + 주두 2 = 7. 주두 1단 5×5 모서리 제외, 2단 5×5. 단 모서리 2×2 겹침(겹침 칸은 기둥 소유), 바깥 1칸 돌출.
- 얇은 보 1×1: 기둥 상단 Y +7에서 4개 기둥 연결(중간 구역 천장 9 아래 공기 1).
- 거대 상자: 받침 7×5×1(a D−7..D−3), 본체 5(b)×3(a D−6..D−4)×2, 뚜껑 1, 전면 접근 행 a = D−8.
- 광물 더미: 입구 쪽 구역(|b| ≥ 3)과 단 옆 띠(|b| ≥ 9), 높이 0–4의 결정적 불규칙 패턴(seed 탐색으로 좌우 부피 균형).
- Q16 원안의 "주변 보관 벽감"은 승인 조건으로 남아 있으나 Q64/Q69가 대체 배치를 규정하므로 이번에 *설계하지 않았습니다*(벽 두께 3에서 깊이 ≤2 벽감 예약 가능, 미배치).

## 4. 후보 A — 적층 (동쪽 좌표)

보물창고가 P3 왕실 홀 바로 아래(홀 평면 안)에 놓이는 배치입니다. 보물창고 외벽 체적은 P2 `VAULT_A` 예약(X −40..−14, Z 9..39, Y −39..−26)과 일치합니다.

### 4.1 공간

| id | 종류 | X | Y | Z | 크기 |
| --- | --- | --- | --- | --- | --- |
| FA | stair | -9..-1 | -31..-20 | 33..35 | 9×12×3 |
| LA | landing | 0..4 | -32..-28 | 32..36 | 5×5×5 |
| FB | stair | 1..3 | -37..-30 | 27..31 | 3×8×5 |
| LB | landing | 0..4 | -37..-33 | 22..26 | 5×5×5 |
| CV | corr | -13..-1 | -37..-32 | 22..26 | 13×6×5 |
| VAULT | vault | -37..-17 | -37..-28 | 12..36 | 21×10×25 |
| VAULT_ENT | corr | -16..-14 | -37..-32 | 22..26 | 3×6×5 |
| CR_ENT | corr | 5..7 | -37..-33 | 23..25 | 3×5×3 |
| CRYPT | crypt | 8..20 | -37..-31 | 17..31 | 13×7×15 |
| CRYPT_NICHE_N0 | niche | 9..11 | -37..-35 | 15..16 | 3×3×2 |
| CRYPT_NICHE_S0 | niche | 9..11 | -37..-35 | 32..33 | 3×3×2 |
| CRYPT_NICHE_N1 | niche | 13..15 | -37..-35 | 15..16 | 3×3×2 |
| CRYPT_NICHE_S1 | niche | 13..15 | -37..-35 | 32..33 | 3×3×2 |
| CRYPT_NICHE_N2 | niche | 17..19 | -37..-35 | 15..16 | 3×3×2 |
| CRYPT_NICHE_S2 | niche | 17..19 | -37..-35 | 32..33 | 3×3×2 |
| VAULT_ENV (외벽 포함 구조) | envelope | -40..-14 | -39..-26 | 9..39 | 27×14×31 |
| CRYPT_ENV (외벽 포함 구조) | envelope | 5..23 | -39..-29 | 14..34 | 19×11×21 |

경로: 홀 입구 구역 → FA(동쪽 9단, 폭 3) → LA(5×5) → FB(북쪽 5단) → LB(5×5) → CV(서쪽 통로 13) → VAULT_ENT(벽 관통 3) → 보물창고. LB 동쪽에서 CR_ENT → 비밀 묘실(선택 지선).

### 4.2 요소 (보물창고·묘실 내부 고체)

| id | 종류 | X | Y | Z | 크기 | 복셀 수 |
| --- | --- | --- | --- | --- | --- | --- |
| COL0:base | column_base | -37..-35 | -37 | 30..32 | 3×1×3 | 9 |
| COL0:shaft | column_shaft | -37..-35 | -36..-33 | 30..32 | 3×4×3 | 36 |
| COL0:cap | column_cap | -37..-34 | -32..-31 | 29..33 | 4×2×5 | 38 |
| COL1:base | column_base | -37..-35 | -37 | 16..18 | 3×1×3 | 9 |
| COL1:shaft | column_shaft | -37..-35 | -36..-33 | 16..18 | 3×4×3 | 36 |
| COL1:cap | column_cap | -37..-34 | -32..-31 | 15..19 | 4×2×5 | 38 |
| COL2:base | column_base | -27..-25 | -37 | 30..32 | 3×1×3 | 9 |
| COL2:shaft | column_shaft | -27..-25 | -36..-33 | 30..32 | 3×4×3 | 36 |
| COL2:cap | column_cap | -28..-24 | -32..-31 | 29..33 | 5×2×5 | 46 |
| COL3:base | column_base | -27..-25 | -37 | 16..18 | 3×1×3 | 9 |
| COL3:shaft | column_shaft | -27..-25 | -36..-33 | 16..18 | 3×4×3 | 36 |
| COL3:cap | column_cap | -28..-24 | -32..-31 | 15..19 | 5×2×5 | 46 |
| DAIS | dais | -36..-26 | -37..-35 | 17..31 | 11×3×15 | 384 |
| DAIS_STAIR | dais_stair | -28..-24 | -37..-35 | 21..27 | 5×3×7 | 63 |
| BEAMS | beam | -36..-26 | -30 | 17..31 | 11×1×15 | 48 |
| CHEST_BASE | chest_base | -35..-31 | -34 | 21..27 | 5×1×7 | 35 |
| CHEST_BODY | chest_body | -34..-32 | -33..-32 | 22..26 | 3×2×5 | 30 |
| CHEST_LID | chest_lid | -34..-32 | -31 | 22..26 | 3×1×5 | 15 |
| PILES_P | pile | -37..-17 | -37..-34 | 27..36 | 21×4×10 | 160 |
| PILES_N | pile | -37..-17 | -37..-34 | 12..21 | 21×4×10 | 161 |
| CRYPT:SARC | sarcophagus | 13..15 | -37..-36 | 22..26 | 3×2×5 | 30 |

## 5. 후보 B — 측방 (동쪽 좌표)

보물창고가 홀 동쪽에 접해(홀 예약과 3칸 간격) 내부 바닥 Y −30에 놓입니다. 내부 깊이가 25(동서), 폭이 21(남북)입니다.

**깊이 선정 근거(스크리닝)**: 홀 동쪽 접면 위치(외벽 X 3..33, Z 13..39, 높이 14)에 대해 내부 바닥을 바꿔가며 L1–L3 공동·예약과 충돌을 검사(여유 2 포함):

| 보물창고 내부 바닥 Y | 충돌 복셀 수 | 충돌 공간 |
| --- | --- | --- |
| -22 | 808 | B1, B2, BRB1, BRB2, LND12, V23 |
| -24 | 588 | B1, B2, BRB1, BRB2, V23 |
| -26 | 294 | B1, B2, BRB1, BRB2, V23 |
| -28 | 0 | 없음 |
| -30 | 0 | 없음 |
| -32 | 0 | 없음 |
| -34 | 0 | 없음 |
| -36 | 0 | 없음 |
| -38 | 0 | 없음 |
| -40 | 0 | 없음 |

Y −22(=홀 바닥, "같은 깊이")는 L2 보관실(B1·B2)·전이부(V23·LND12)와 충돌해 불가능하고, −28부터 가능합니다. 이 설계는 **−30**을 사용했습니다. 이유: 계단 끝 통로가 홀 예약 평면(X ≤ −2) 아래를 지나므로 윗면이 Y ≤ −25여야 하며, 6높이 통로(−30..−25)가 이를 정확히 만족합니다. −28은 통로 높이 ≤4로 재설계해야 가능하며 *탐색하지 않았습니다*(UNVERIFIED).

### 5.1 공간

| id | 종류 | X | Y | Z | 크기 |
| --- | --- | --- | --- | --- | --- |
| FA | stair | -9..-3 | -29..-20 | 33..35 | 7×10×3 |
| LA | landing | -2..2 | -30..-26 | 32..36 | 5×5×5 |
| CB1 | corr | -2..2 | -30..-25 | 24..31 | 5×6×8 |
| VAULT | vault | 6..30 | -30..-21 | 16..36 | 25×10×21 |
| VAULT_ENT | corr | 3..5 | -30..-25 | 24..28 | 3×6×5 |
| CC | corr | 0..2 | -30..-26 | 0..23 | 3×5×24 |
| CC3 | corr | 3..7 | -30..-26 | 0..2 | 5×5×3 |
| CRYPT | crypt | 8..22 | -30..-24 | -7..5 | 15×7×13 |
| CRYPT_NICHE_N0 | niche | 9..11 | -30..-28 | -9..-8 | 3×3×2 |
| CRYPT_NICHE_S0 | niche | 9..11 | -30..-28 | 6..7 | 3×3×2 |
| CRYPT_NICHE_N1 | niche | 13..15 | -30..-28 | -9..-8 | 3×3×2 |
| CRYPT_NICHE_S1 | niche | 13..15 | -30..-28 | 6..7 | 3×3×2 |
| CRYPT_NICHE_N2 | niche | 17..19 | -30..-28 | -9..-8 | 3×3×2 |
| CRYPT_NICHE_S2 | niche | 17..19 | -30..-28 | 6..7 | 3×3×2 |
| VAULT_ENV (외벽 포함 구조) | envelope | 3..33 | -32..-19 | 13..39 | 31×14×27 |
| CRYPT_ENV (외벽 포함 구조) | envelope | 5..25 | -32..-22 | -10..8 | 21×11×19 |

경로: 홀 입구 구역 → FA(동쪽 7단) → LA(5×5, 바닥 Y −30) → CB1(북쪽 통로 8) → VAULT_ENT(동쪽) → 보물창고. CB1 북단에서 CC(북쪽 24) → CC3(동쪽) → 비밀 묘실(선택 지선).

### 5.2 요소

| id | 종류 | X | Y | Z | 크기 | 복셀 수 |
| --- | --- | --- | --- | --- | --- | --- |
| COL0:base | column_base | 28..30 | -30 | 32..34 | 3×1×3 | 9 |
| COL0:shaft | column_shaft | 28..30 | -29..-26 | 32..34 | 3×4×3 | 36 |
| COL0:cap | column_cap | 27..30 | -25..-24 | 31..35 | 4×2×5 | 38 |
| COL1:base | column_base | 28..30 | -30 | 18..20 | 3×1×3 | 9 |
| COL1:shaft | column_shaft | 28..30 | -29..-26 | 18..20 | 3×4×3 | 36 |
| COL1:cap | column_cap | 27..30 | -25..-24 | 17..21 | 4×2×5 | 38 |
| COL2:base | column_base | 18..20 | -30 | 32..34 | 3×1×3 | 9 |
| COL2:shaft | column_shaft | 18..20 | -29..-26 | 32..34 | 3×4×3 | 36 |
| COL2:cap | column_cap | 17..21 | -25..-24 | 31..35 | 5×2×5 | 46 |
| COL3:base | column_base | 18..20 | -30 | 18..20 | 3×1×3 | 9 |
| COL3:shaft | column_shaft | 18..20 | -29..-26 | 18..20 | 3×4×3 | 36 |
| COL3:cap | column_cap | 17..21 | -25..-24 | 17..21 | 5×2×5 | 46 |
| DAIS | dais | 19..29 | -30..-28 | 19..33 | 11×3×15 | 384 |
| DAIS_STAIR | dais_stair | 17..21 | -30..-28 | 23..29 | 5×3×7 | 63 |
| BEAMS | beam | 19..29 | -23 | 19..33 | 11×1×15 | 48 |
| CHEST_BASE | chest_base | 24..28 | -27 | 23..29 | 5×1×7 | 35 |
| CHEST_BODY | chest_body | 25..27 | -26..-25 | 24..28 | 3×2×5 | 30 |
| CHEST_LID | chest_lid | 25..27 | -24 | 24..28 | 3×1×5 | 15 |
| PILES_P | pile | 6..30 | -30..-27 | 29..36 | 25×4×8 | 141 |
| PILES_N | pile | 6..30 | -30..-27 | 16..23 | 25×4×8 | 139 |
| CRYPT:SARC | sarcophagus | 14..16 | -30..-29 | -3..1 | 3×2×5 | 30 |

## 6. 적층형 vs 측방형 비교

| 지표 | A 적층 | B 측방 |
| --- | --- | --- |
| 검증 (동쪽) | 16 PASS / 0 FAIL / 2 UNVERIFIED | 16 PASS / 0 FAIL / 2 UNVERIFIED |
| 검증 (서쪽, 거울) | 16 / 0 / 2 | 16 / 0 / 2 |
| P4 전체 구조물 X 범위 (동) | -40..23 | -11..33 |
| P4 전체 구조물 Z 범위 | 9..39 | -10..39 |
| ±40 초과 거리 / 초과 복셀 수 | 0 / 0 | 0 / 0 |
| ±40 경계 여유 (가장 가까운 면) | X=−40 면 여유 0, Z=39 면 여유 1 (서쪽 후보는 X=+40) | Z=39 면 여유 1, X=33 면 여유 7 |
| 최저 공동 Y / 깊이 | -37 / 37 | -30 / 30 |
| 최저 구조 블록 Y / 깊이 | -39 / 39 | -32 / 32 |
| 보물창고 내부 바닥 Y | −37 | −30 |
| 홀 바닥(−22) → 보물창고 바닥 수직 낙차 | 15 | 8 |
| 숨은 계단 단수 (전체 발판 수) | 14 | 7 |
| 방향 전환 (참) | 2회 (동→북 참 LA, 북→서 참 LB) | 2회 (참 LA에서 동→북, 통로 끝에서 북→동) |
| 홀 입구 구역 → 상자 정면 보행 거리 (걸음) | 53 | 42 |
| 홀 입구 구역 → 보물창고 입구 | 41 | 26 |
| 홀 입구 구역 → 비밀 묘실 안 | 30 | 54 |
| P1 착지 → 상자 정면 | 212 | 201 |
| 상자 정면 5칸 접근 가능 | 5/5 | 5/5 |
| 기존 공동과의 겹침 / 미선언 접촉 | 0 / 0 | 0 / 0 |
| 선언되지 않은 공동과의 최소 간격 | 5 | 2 |
| 보물창고·묘실 벽/바닥·지붕 두께 (측정 최소) | 벽 3 / 2 | 벽 3 / 2 |
| 보물창고 평면이 왕실 홀 아래에 있는 비율 (Q97 관련) | 100% (적층) | 0% (측방) |
| 홀 중심 ↔ 보물창고 중심 평면 거리 | 6.7 | 39.3 |
| 홀 중심 ↔ 비밀 묘실 중심 평면 거리 | 35.1 | 42.2 |
| P2 예약 `VAULT_A`와의 관계 | 동일 외벽 체적(X −40..−14, Z 9..39, Y −39..−26) | 별도 위치(P2 L3 측방 후보와 다름) |

해석(측정값만): B는 낙차 8·계단 7단·최저 구조 Y −32로 A(낙차 15·14단·−39)보다 얕고 보행 거리가 짧으며 ±40 여유가 크지만, A는 기존 P2 예약 `VAULT_A`와 정확히 일치하고 홀 평면 안에 있어 새로운 평면 점유가 적습니다. **Q97**을 "연속 구역의 평면이 겹치지 않음"으로 읽으면 B가 부합하고 "계단식 중심 이동"으로 읽으면 두 후보 모두 해석이 필요합니다. **어느 쪽도 선택하지 않았습니다.**

## 7. 최대 지하 깊이 분석 (Q98)

| 요소 | Y | 깊이 | 근거 |
| --- | --- | --- | --- |
| L3 바닥 / 왕실 홀 바닥 | −22 | 22 | P2·P3 |
| 홀 예약 바닥(슬래브 2) | −24 | 24 | P2 잠정 깊이 |
| 숨은 계단 발판 최저(A: LB·CV·보물창고 / B: LA·CB1) | A −37 / B −30 | 37 / 30 | 측정 |
| **A 구조 최저 블록**(보물창고 바닥 슬래브 −39..−38) | **−39** | **39** | V13 |
| **B 구조 최저 블록**(보물창고 바닥 슬래브 −32..−31) | **−32** | **32** | V13 |
| 비밀 묘실(A: 바닥 −37, B: 바닥 −30) | 위와 동일 | | 대표 후보 |

- 계산으로 **확정 가능한 것**: 각 후보의 최저 구조 Y(A −39, B −32), 두 후보 모두 홀 바닥(−22) 위로 올라가지 않음.
- **확정할 수 없는 이유**: ① 후보 A/B 선택이 소유자 결정이다. ② 비밀 묘실 크기·위치가 미승인이라 묘실 자체가 깊이를 늘릴 수 있다(대표 후보는 보물창고와 같은 바닥에 둠). ③ 층간 8 최소화(P2-F6) 미탐색. 따라서 **새 상한을 설정하지 않았고 무제한도 아닙니다.** 선택 시 근거값: A 39, B 32(기존 20 한계는 Q13으로 완화됨).

## 8. 지하 확장량 비교표 (D01-B 유지, 전체 구조물 기준)

| 지표 | A | B |
| --- | --- | --- |
| 전체 X 점유(P4) | −40..23 | −11..33 |
| 전체 Z 점유(P4) | 9..39 | −10..39 |
| ±40 초과 거리 | 0 | 0 |
| 초과 복셀 | 0 | 0 |
| 확장 필요성 | 불필요(확장 0으로 승인 조건 충족) | 불필요 |

범위에는 보물창고·묘실 외벽(두께 3, 바닥·지붕 2)과 통로·계단 벽(공동 + 2)을 포함했습니다. 왕실 홀(P3)은 이미 X=−40, Z=40에 접하고 변경하지 않았습니다. P2-F1(L1/L2 전이부의 X=±41)은 P4와 무관하며 열려 있습니다. 서쪽 후보에서는 A의 보물창고가 X=+40에 접합니다.

## 9. 평면도·단면도

### 9.1 후보 A — 전체 바닥층 Y=−37

```
PLAN Y=-37 X[-41,24] Z[8,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X  '         '         '         '         '         '         '    
  +8                                                                    
  +9   ###########################                                      
 +10   ###########################                                      
 +11   ###########################                                      
 +12   ###mm..mmmm..mmmm...mmm.###                                      
 +13   ###m.mm..mm.m..mmm...mmm###                                      
 +14   ###...mmm...mmm...mmm...###                  ################### 
 +15   ###.mmmm..mmmm...mmm..m.###                  ####nnn#nnn#nnn#### 
 +16   ###PPP.......PPP..mmmm..###                  ####nnn#nnn#nnn#### 
 +17   ###PPPDDDDDDDPPP.m....mm###                  ###ccccccccccccc### 
 +18   ###PPPDDDDDDDPPP...m.mm.###                  ###ccccccccccccc### 
 +19   ###.DDDDDDDDDDD..mm..mmm###                  ###ccccccccccccc### 
 +20   ###.DDDDDDDDDDD....mm...########################ccccccccccccc### 
 +21   ###.DDDDDDDDddddd.mm..mm########################ccccccccccccc### 
 +22   ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll###cccccSSSccccc### 
 +23   ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll,,,cccccSSSccccc### 
 +24   ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll,,,cccccSSSccccc### 
 +25   ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll,,,cccccSSSccccc### 
 +26   ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll###cccccSSSccccc### 
 +27   ###.DDDDDDDDdddddmmm..m.#################sss####ccccccccccccc### 
 +28   ###.DDDDDDDDDDD...mmm...########################ccccccccccccc### 
 +29   ###.DDDDDDDDDDD..m...mmm###            #########ccccccccccccc### 
 +30   ###PPPDDDDDDDPPP....mmm.###            #########ccccccccccccc### 
 +31   ###PPPDDDDDDDPPP.m...mmm###            #########ccccccccccccc### 
 +32   ###PPP.......PPP..mmm...###            ##########nnn#nnn#nnn#### 
 +33   ###.mmmm..mmmm..mmmm...m###            ##########nnn#nnn#nnn#### 
 +34   ###...mmm...mmm...mmm...###                  ################### 
 +35   ###.mmm..m.mm..mm.m..mmm###                                      
 +36   ###m...mmmm..mmmm..mmmm.###                                      
 +37   ###########################                                      
 +38   ###########################                                      
 +39   ###########################                                      
 +40                                                                    
```

### 9.2 후보 A — 보물창고 상세 배치도

바닥층(Y −37; 단·기둥 기초·계단 하단):

```
PLAN Y=-37 X[-41,-13] Z[8,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X  '         '         '       
  +8                               
  +9   ########################### 
 +10   ########################### 
 +11   ########################### 
 +12   ###mm..mmmm..mmmm...mmm.### 
 +13   ###m.mm..mm.m..mmm...mmm### 
 +14   ###...mmm...mmm...mmm...### 
 +15   ###.mmmm..mmmm...mmm..m.### 
 +16   ###PPP.......PPP..mmmm..### 
 +17   ###PPPDDDDDDDPPP.m....mm### 
 +18   ###PPPDDDDDDDPPP...m.mm.### 
 +19   ###.DDDDDDDDDDD..mm..mmm### 
 +20   ###.DDDDDDDDDDD....mm...####
 +21   ###.DDDDDDDDddddd.mm..mm####
 +22   ###.DDDDDDDDddddd.......,,,,
 +23   ###.DDDDDDDDddddd.......,,,,
 +24   ###.DDDDDDDDddddd.......,,,,
 +25   ###.DDDDDDDDddddd.......,,,,
 +26   ###.DDDDDDDDddddd.......,,,,
 +27   ###.DDDDDDDDdddddmmm..m.####
 +28   ###.DDDDDDDDDDD...mmm...####
 +29   ###.DDDDDDDDDDD..m...mmm### 
 +30   ###PPPDDDDDDDPPP....mmm.### 
 +31   ###PPPDDDDDDDPPP.m...mmm### 
 +32   ###PPP.......PPP..mmm...### 
 +33   ###.mmmm..mmmm..mmmm...m### 
 +34   ###...mmm...mmm...mmm...### 
 +35   ###.mmm..m.mm..mm.m..mmm### 
 +36   ###m...mmmm..mmmm..mmmm.### 
 +37   ########################### 
 +38   ########################### 
 +39   ########################### 
 +40                               
```

Y −34(단 윗면 위: 상자 받침·기둥 몸체·광물 더미):

```
PLAN Y=-34 X[-41,-13] Z[8,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X  '         '         '       
  +8                               
  +9   ########################### 
 +10   ########################### 
 +11   ########################### 
 +12   ###.m.....m.....m.....m.### 
 +13   ###...m.....m...........### 
 +14   ###.....................### 
 +15   ###....m.....m.....m....### 
 +16   ###PPP.......PPP.....m..### 
 +17   ###PPP.......PPP........### 
 +18   ###PPP.......PPP......m.### 
 +19   ###...............m.....### 
 +20   ###.....................####
 +21   ###..XXXXX.........m....####
 +22   ###..XXXXX..............,,,,
 +23   ###..XXXXX..............,,,,
 +24   ###..XXXXX..............,,,,
 +25   ###..XXXXX..............,,,,
 +26   ###..XXXXX..............,,,,
 +27   ###..XXXXX.........m....####
 +28   ###.....................####
 +29   ###.....................### 
 +30   ###PPP.......PPP......m.### 
 +31   ###PPP.......PPP........### 
 +32   ###PPP.......PPP........### 
 +33   ###....m.....m.....m....### 
 +34   ###.....................### 
 +35   ###...m.....m.....m.....### 
 +36   ###.......m.....m.....m.### 
 +37   ########################### 
 +38   ########################### 
 +39   ########################### 
 +40                               
```

Y −30(보 층):

```
PLAN Y=-30 X[-41,-13] Z[8,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X  '         '         '       
  +8                               
  +9   ########################### 
 +10   ########################### 
 +11   ########################### 
 +12   ###.....................### 
 +13   ###.....................### 
 +14   ###.....................### 
 +15   ###.....................### 
 +16   ###.....................### 
 +17   ###.BBBBBBBBBBB.........### 
 +18   ###.B.........B.........### 
 +19   ###.B.........B.........### 
 +20   ###.B.........B.........####
 +21   ###.B.........B.........####
 +22   ###.B.........B.........####
 +23   ###.B.........B.........####
 +24   ###.B.........B.........####
 +25   ###.B.........B.........####
 +26   ###.B.........B.........####
 +27   ###.B.........B.........####
 +28   ###.B.........B.........####
 +29   ###.B.........B.........### 
 +30   ###.B.........B.........### 
 +31   ###.BBBBBBBBBBB.........### 
 +32   ###.....................### 
 +33   ###.....................### 
 +34   ###.....................### 
 +35   ###.....................### 
 +36   ###.....................### 
 +37   ########################### 
 +38   ########################### 
 +39   ########################### 
 +40                               
```

### 9.3 후보 A — 수직 단면

보물창고 축(Z=24, 동서) — 입구 통로, 단, 계단, 기둥, 비밀 묘실이 함께 보임:

```
SECTION Z=24 horizontal = X -42..25, Y up
 -24                                                                      
 -25                                                                      
 -26    ###########################                                       
 -27    ###########################                                       
 -28    ###.....................###                                       
 -29    ###.....................###                  ###################  
 -30    ###.B.........B.........##################   ###################  
 -31    ###...XXX...............########################ccccccccccccc###  
 -32    ###...XXX...............,,,,,,,,,,,,,,,,########ccccccccccccc###  
 -33    ###...XXX...............,,,,,,,,,,,,,,,,lllll,,,ccccccccccccc###  
 -34    ###..XXXXX..............,,,,,,,,,,,,,,,,lllll,,,ccccccccccccc###  
 -35    ###.DDDDDDDDd...........,,,,,,,,,,,,,,,,lllll,,,ccccccccccccc###  
 -36    ###.DDDDDDDDddd.........,,,,,,,,,,,,,,,,lllll,,,cccccSSSccccc###  
 -37    ###.DDDDDDDDddddd.......,,,,,,,,,,,,,,,,lllll,,,cccccSSSccccc###  
 -38    ################################################################  
 -39    ################################################################  
 -40                                                                      
 -41                                                                      
```

보물창고 횡단면(X=−31, 남북) — 중앙 10 / 중간 9 / 측면 8 계단식 천장:

```
SECTION X=-31 horizontal = Z 8..40, Y up
 -24                                   
 -25                                   
 -26   ############################### 
 -27   ############################### 
 -28   ###########.........########### 
 -29   #######.................####### 
 -30   ###.....B.............B.....### 
 -31   ###.........................### 
 -32   ###.........................### 
 -33   ###.........................### 
 -34   ###.........XXXXXXX.........### 
 -35   ###m....DDDDDDDDDDDDDDD....m### 
 -36   ###m....DDDDDDDDDDDDDDD....m### 
 -37   ###mm...DDDDDDDDDDDDDDD...mm### 
 -38   ############################### 
 -39   ############################### 
 -40                                   
 -41                                   
```

경로 단면(Z=35, 홀 구역에서 계단 FA·참 LA):

```
SECTION Z=35 horizontal = X -41..24, Y up
  -8                                                                    
  -9                                                                    
 -10                                                                    
 -11                                                                    
 -12                                                                    
 -13                                                                    
 -14        :::::::::::::::::::::::::::::                               
 -15      ::::::   :::::::::::::::   ::::::                             
 -16      ::::       :::::::::::       ::::                             
 -17      :::::     :::::::::::::     :::::                             
 -18      :::::     :::::::::::::   ##:::::######                       
 -19      ::::::   ::::::   ::::::  #::::::######                       
 -20      ::::::   ::::::   ::::::  #:s::::######                       
 -21      ::::::   ::           ::  #:ss:::######                       
 -22      :::::     :           :   ##sss::######                       
 -23                                ##ssss#######                       
 -24                                ###ssss######                       
 -25                                ####ssss#####                       
 -26   ###########################  #####ssss#########                  
 -27   ###########################  ######ssss########                  
 -28   ###########################  #######sssslllll##                  
 -29   ###########################  ########ssslllll##                  
 -30   ###.....................###  #########sslllll##                  
 -31   ###.....................###  ##########slllll##                  
 -32   ###.....................###  ###########lllll##                  
 -33   ###.....................###  ##################                  
 -34   ###...m.....m.....m.....###           #########                  
 -35   ###.m.m.....m...m.m...m.###                                      
 -36   ###.mmm....mm...m.m...mm###                                      
 -37   ###.mmm..m.mm..mm.m..mmm###                                      
 -38   ###########################                                      
 -39   ###########################                                      
 -40                                                                    
 -41                                                                    
```

### 9.4 후보 B — 바닥층 Y=−30 (서쪽 거울 포함)

```
PLAN Y=-30 X[-12,34] Z[-11,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X   '         '         '         '         '    
 -11                                                 
 -10                   #####################         
  -9                   ####nnn#nnn#nnn######         
  -8                   ####nnn#nnn#nnn######         
  -7                   ###ccccccccccccccc###         
  -6                   ###ccccccccccccccc###         
  -5                   ###ccccccccccccccc###         
  -4                   ###ccccccccccccccc###         
  -3                   ###ccccccSSScccccc###         
  -2            ##########ccccccSSScccccc###         
  -1            ##########ccccccSSScccccc###         
  +0            ##,,,,,,,,ccccccSSScccccc###         
  +1            ##,,,,,,,,ccccccSSScccccc###         
  +2            ##,,,,,,,,ccccccccccccccc###         
  +3            ##,,,#####ccccccccccccccc###         
  +4            ##,,,#####ccccccccccccccc###         
  +5            ##,,,#####ccccccccccccccc###         
  +6            ##,,,######nnn#nnn#nnn######         
  +7            ##,,,######nnn#nnn#nnn######         
  +8            ##,,,#######################         
  +9            ##,,,##                              
 +10            ##,,,##                              
 +11            ##,,,##                              
 +12            ##,,,##                              
 +13            ##,,,############################### 
 +14            ##,,,############################### 
 +15            ##,,,############################### 
 +16            ##,,,###...mmm...mmm...mmm....mm.### 
 +17            ##,,,###m...mmmm..mmmm..mmmm...mm### 
 +18            ##,,,###...mmm..m.m.PPP.......PPP### 
 +19            ##,,,###mmm...mmm...PPPDDDDDDDPPP### 
 +20            ##,,,###.mmm...mmmm.PPPDDDDDDDPPP### 
 +21            ##,,,###mmm...mmm....DDDDDDDDDDD.### 
 +22          ####,,,###...mmm...mm..DDDDDDDDDDD.### 
 +23          ####,,,###.m..mmm...mdddddDDDDDDDD.### 
 +24          ##,,,,,,,,...........dddddDDDDDDDD.### 
 +25          ##,,,,,,,,...........dddddDDDDDDDD.### 
 +26          ##,,,,,,,,...........dddddDDDDDDDD.### 
 +27          ##,,,,,,,,...........dddddDDDDDDDD.### 
 +28          ##,,,,,,,,...........dddddDDDDDDDD.### 
 +29          ##,,,,,###m...mmmm..mdddddDDDDDDDD.### 
 +30          ##,,,,,###...mmm...mm..DDDDDDDDDDD.### 
 +31   #########,,,,,###mmm..m.mm....DDDDDDDDDDD.### 
 +32   #########lllll###.mmmm..mmmm.PPPDDDDDDDPPP### 
 +33   #########lllll###mmm...mmm...PPPDDDDDDDPPP### 
 +34   #########lllll###..m.mm..mm..PPP.......PPP### 
 +35   #########lllll###mm..mmmm..mmmm...mmm..m.m### 
 +36   #########lllll###...mmm...mmm....mm...m.m.### 
 +37   ############################################# 
 +38          ###################################### 
 +39                 ############################### 
 +40                                                 
```

```
PLAN Y=-30 X[2,34] Z[12,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X         '         '         '    
 +12  ,##                              
 +13  ,############################### 
 +14  ,############################### 
 +15  ,############################### 
 +16  ,###...mmm...mmm...mmm....mm.### 
 +17  ,###m...mmmm..mmmm..mmmm...mm### 
 +18  ,###...mmm..m.m.PPP.......PPP### 
 +19  ,###mmm...mmm...PPPDDDDDDDPPP### 
 +20  ,###.mmm...mmmm.PPPDDDDDDDPPP### 
 +21  ,###mmm...mmm....DDDDDDDDDDD.### 
 +22  ,###...mmm...mm..DDDDDDDDDDD.### 
 +23  ,###.m..mmm...mdddddDDDDDDDD.### 
 +24  ,,,,...........dddddDDDDDDDD.### 
 +25  ,,,,...........dddddDDDDDDDD.### 
 +26  ,,,,...........dddddDDDDDDDD.### 
 +27  ,,,,...........dddddDDDDDDDD.### 
 +28  ,,,,...........dddddDDDDDDDD.### 
 +29  ,###m...mmmm..mdddddDDDDDDDD.### 
 +30  ,###...mmm...mm..DDDDDDDDDDD.### 
 +31  ,###mmm..m.mm....DDDDDDDDDDD.### 
 +32  l###.mmmm..mmmm.PPPDDDDDDDPPP### 
 +33  l###mmm...mmm...PPPDDDDDDDPPP### 
 +34  l###..m.mm..mm..PPP.......PPP### 
 +35  l###mm..mmmm..mmmm...mmm..m.m### 
 +36  l###...mmm...mmm....mm...m.m.### 
 +37  ################################ 
 +38  ################################ 
 +39   ############################### 
 +40                                   
```

```
SECTION Z=26 horizontal = X -13..35, Y up
 -17  :::::::::                                        
 -18  :::::::::::                                      
 -19  :::::::::::     ###############################  
 -20  :::::::::::     ###############################  
 -21  :::::::::::     ###.........................###  
 -22  ::::::::::      ###.........................###  
 -23           ##########.............B.........B.###  
 -24           ##########...................XXX...###  
 -25           ##,,,,,,,,...................XXX...###  
 -26           ##,,,,,,,,...................XXX...###  
 -27           ##,,,,,,,,..................XXXXX..###  
 -28           ##,,,,,,,,...............dDDDDDDDD.###  
 -29           ##,,,,,,,,.............dddDDDDDDDD.###  
 -30           ##,,,,,,,,...........dddddDDDDDDDD.###  
 -31           ######################################  
 -32           ######################################  
 -33                                                   
 -34                                                   
```

```
SECTION Z=35 horizontal = X -12..34, Y up
  -8                                                 
  -9                                                 
 -10                                                 
 -11                                                 
 -12                                                 
 -13                                                 
 -14  ::::::                                         
 -15    ::::::                                       
 -16      ::::                                       
 -17     :::::                                       
 -18   ##:::::####                                   
 -19   #::::::####   ############################### 
 -20   #:s::::####   ############################### 
 -21   #:ss:::####   ############################### 
 -22   ##sss::####   ############################### 
 -23   ##ssss#####   ###.........................### 
 -24   ###ssss##########...........KKKKK.....KKKK### 
 -25   ####ssss#########............KKK.......KKK### 
 -26   #####sssslllll###.........................### 
 -27   ######ssslllll###....m.....m...........m..### 
 -28   #######sslllll###....mm....mm.....m....m..### 
 -29   ########slllll###m...mmm...mmm....mm...m.m### 
 -30   #########lllll###mm..mmmm..mmmm...mmm..m.m### 
 -31   ############################################# 
 -32          ###################################### 
 -33                                                 
 -34                                                 
```

서쪽 후보 B 바닥층(정확한 거울):

```
PLAN Y=-30 X[-34,12] Z[-11,40] north(-Z) up, east(+X) right. .=vault ,=corridor s=stair l=landing c=crypt n=niche D/d=dais/stair P=column K=capital B=beam X=chest m=mineral pile S=sarcophagus :=P1-P3 void #=wall/slab
   Z\X     '         '         '         '         '  
 -11                                                 
 -10           #####################                 
  -9           ######nnn#nnn#nnn####                 
  -8           ######nnn#nnn#nnn####                 
  -7           ###ccccccccccccccc###                 
  -6           ###ccccccccccccccc###                 
  -5           ###ccccccccccccccc###                 
  -4           ###ccccccccccccccc###                 
  -3           ###ccccccSSScccccc###                 
  -2           ###ccccccSSScccccc##########          
  -1           ###ccccccSSScccccc##########          
  +0           ###ccccccSSScccccc,,,,,,,,##          
  +1           ###ccccccSSScccccc,,,,,,,,##          
  +2           ###ccccccccccccccc,,,,,,,,##          
  +3           ###ccccccccccccccc#####,,,##          
  +4           ###ccccccccccccccc#####,,,##          
  +5           ###ccccccccccccccc#####,,,##          
  +6           ######nnn#nnn#nnn######,,,##          
  +7           ######nnn#nnn#nnn######,,,##          
  +8           #######################,,,##          
  +9                                ##,,,##          
 +10                                ##,,,##          
 +11                                ##,,,##          
 +12                                ##,,,##          
 +13   ###############################,,,##          
 +14   ###############################,,,##          
 +15   ###############################,,,##          
 +16   ###.mm....mmm...mmm...mmm...###,,,##          
 +17   ###mm...mmmm..mmmm..mmmm...m###,,,##          
 +18   ###PPP.......PPP.m.m..mmm...###,,,##          
 +19   ###PPPDDDDDDDPPP...mmm...mmm###,,,##          
 +20   ###PPPDDDDDDDPPP.mmmm...mmm.###,,,##          
 +21   ###.DDDDDDDDDDD....mmm...mmm###,,,##          
 +22   ###.DDDDDDDDDDD..mm...mmm...###,,,####        
 +23   ###.DDDDDDDDdddddm...mmm..m.###,,,####        
 +24   ###.DDDDDDDDddddd...........,,,,,,,,##        
 +25   ###.DDDDDDDDddddd...........,,,,,,,,##        
 +26   ###.DDDDDDDDddddd...........,,,,,,,,##        
 +27   ###.DDDDDDDDddddd...........,,,,,,,,##        
 +28   ###.DDDDDDDDddddd...........,,,,,,,,##        
 +29   ###.DDDDDDDDdddddm..mmmm...m###,,,,,##        
 +30   ###.DDDDDDDDDDD..mm...mmm...###,,,,,##        
 +31   ###.DDDDDDDDDDD....mm.m..mmm###,,,,,######### 
 +32   ###PPPDDDDDDDPPP.mmmm..mmmm.###lllll######### 
 +33   ###PPPDDDDDDDPPP...mmm...mmm###lllll######### 
 +34   ###PPP.......PPP..mm..mm.m..###lllll######### 
 +35   ###m.m..mmm...mmmm..mmmm..mm###lllll######### 
 +36   ###.m.m...mm....mmm...mmm...###lllll######### 
 +37   ############################################# 
 +38   ######################################        
 +39   ###############################               
 +40                                                 
```

그 밖의 층·단면은 `generated/{A,B}/{east,west}/` 에 있습니다.

## 10. 검증 결과

### 10.1 후보 A (동 / 서)

| ID | 검사 | 동 | 서 |
| --- | --- | --- | --- |
| V1 | Vault clear interior plan 21x25 (Q65) and maximum clear height 10 (Q66): voxel scan vs frame arithmetic | PASS | PASS |
| V2 | Ceiling steps 8 (side) -> 9 (intermediate) -> 10 (centre), symmetric, constant along the depth (Q67/Q68; boundaries PROPOSED) | PASS | PASS |
| V3 | Central aisle >= 5 wide from the entrance to the stair foot (Q70): per-depth clear run through the axis, a 5x5 walker, entrance corridor width | PASS | PASS |
| V4 | Rear dais 15x11 plan, 3 high (Q85/Q71); frontal stair 7 wide, run 5 = 2 outside + 3 inset, rise 3 with horizontal portions (Q89-Q91) | PASS | PASS |
| V5 | Four floor-founded papyrus columns at the dais corners: body 3x3, 7 high (base through capital), capital <= 5x5, 2x2 plan overlap with the dais, 1 block projecting out (Q80-Q88) | PASS | PASS |
| V5b | Thin overhead beams (1x1) join the four column tops and keep an air gap to the stepped ceiling (Q96: visually independent) | PASS | PASS |
| V6 | Giant chest: body 5 (front width) x 3 (depth), stepped base <= 7x5, centred laterally and toward the rear of the dais, lid opening clear above (Q72, Q92, Q93) | PASS | PASS |
| V7 | Real connection: royal-hall floor -> hidden stair -> landings -> vault entrance -> aisle -> dais stair -> all five chest-front interaction cells, and the crypt branch, reachable on foot from the P1 landing | PASS | PASS |
| V7b | Chest front interaction row (5 cells facing the chest) is open and standable, 2 high | PASS | PASS |
| V8 | Irregular mineral piles on both sides, left/right balanced (<=10%), varied heights, none in the 5-wide aisle (Q64/Q69/Q70) | PASS | PASS |
| V9 | Hidden stair: 1:1 gradient, walked headroom >= 4, width >= 3, landings >= 5x5, at least one direction change at a landing (Q13/Q14; widths/headroom PROPOSED) | PASS | PASS |
| V10 | No collision with L1-L3 / royal hall (P1-P3) and no unintended contact: P4 voids overlap no existing void, enter the hall reservation only through the declared hatch pierce volume, touch only declared links, keep >= 2 blocks to unlinked cavities | PASS | PASS |
| V11 | Structural separation: vault/crypt envelopes (walls 3, floor/roof 2) overlap no other structure or reservation and stay >= 2 from foreign cavities; measured wall/roof/floor thickness around the vault and crypt | PASS | PASS |
| V12 | Whole generated P4 structure (vault/crypt walls and slabs, corridors and stairs with minimum wall 2) stays inside X/Z +-40 (D01 whole-structure reading); excess distance and voxel count | PASS | PASS |
| V13 | Lowest cavity and lowest generated block of the P4 structure (depth input for Q98): voxel vs box arithmetic | PASS | PASS |
| V14 | P3 hall is untouched: P4 placed no block inside the hall except the declared pierce volume; hall elements keep their voxels | PASS | PASS |
| V15 | Q97 horizontal stagger through crypt and vault: plan overlap with the royal hall and centre offsets (interpretation of 'staggered' is an owner decision) | UNVERIFIED | UNVERIFIED |
| V16 | Terrain / worldgen / chunk / neighbouring structures, real-client look, chest model and vanilla-GUI behaviour, mechanism of the hidden cover, golem guidance route, mineral-block mineability, loot | UNVERIFIED | UNVERIFIED |

### 10.2 후보 B (동 / 서)

| ID | 검사 | 동 | 서 |
| --- | --- | --- | --- |
| V1 | Vault clear interior plan 21x25 (Q65) and maximum clear height 10 (Q66): voxel scan vs frame arithmetic | PASS | PASS |
| V2 | Ceiling steps 8 (side) -> 9 (intermediate) -> 10 (centre), symmetric, constant along the depth (Q67/Q68; boundaries PROPOSED) | PASS | PASS |
| V3 | Central aisle >= 5 wide from the entrance to the stair foot (Q70): per-depth clear run through the axis, a 5x5 walker, entrance corridor width | PASS | PASS |
| V4 | Rear dais 15x11 plan, 3 high (Q85/Q71); frontal stair 7 wide, run 5 = 2 outside + 3 inset, rise 3 with horizontal portions (Q89-Q91) | PASS | PASS |
| V5 | Four floor-founded papyrus columns at the dais corners: body 3x3, 7 high (base through capital), capital <= 5x5, 2x2 plan overlap with the dais, 1 block projecting out (Q80-Q88) | PASS | PASS |
| V5b | Thin overhead beams (1x1) join the four column tops and keep an air gap to the stepped ceiling (Q96: visually independent) | PASS | PASS |
| V6 | Giant chest: body 5 (front width) x 3 (depth), stepped base <= 7x5, centred laterally and toward the rear of the dais, lid opening clear above (Q72, Q92, Q93) | PASS | PASS |
| V7 | Real connection: royal-hall floor -> hidden stair -> landings -> vault entrance -> aisle -> dais stair -> all five chest-front interaction cells, and the crypt branch, reachable on foot from the P1 landing | PASS | PASS |
| V7b | Chest front interaction row (5 cells facing the chest) is open and standable, 2 high | PASS | PASS |
| V8 | Irregular mineral piles on both sides, left/right balanced (<=10%), varied heights, none in the 5-wide aisle (Q64/Q69/Q70) | PASS | PASS |
| V9 | Hidden stair: 1:1 gradient, walked headroom >= 4, width >= 3, landings >= 5x5, at least one direction change at a landing (Q13/Q14; widths/headroom PROPOSED) | PASS | PASS |
| V10 | No collision with L1-L3 / royal hall (P1-P3) and no unintended contact: P4 voids overlap no existing void, enter the hall reservation only through the declared hatch pierce volume, touch only declared links, keep >= 2 blocks to unlinked cavities | PASS | PASS |
| V11 | Structural separation: vault/crypt envelopes (walls 3, floor/roof 2) overlap no other structure or reservation and stay >= 2 from foreign cavities; measured wall/roof/floor thickness around the vault and crypt | PASS | PASS |
| V12 | Whole generated P4 structure (vault/crypt walls and slabs, corridors and stairs with minimum wall 2) stays inside X/Z +-40 (D01 whole-structure reading); excess distance and voxel count | PASS | PASS |
| V13 | Lowest cavity and lowest generated block of the P4 structure (depth input for Q98): voxel vs box arithmetic | PASS | PASS |
| V14 | P3 hall is untouched: P4 placed no block inside the hall except the declared pierce volume; hall elements keep their voxels | PASS | PASS |
| V15 | Q97 horizontal stagger through crypt and vault: plan overlap with the royal hall and centre offsets (interpretation of 'staggered' is an owner decision) | UNVERIFIED | UNVERIFIED |
| V16 | Terrain / worldgen / chunk / neighbouring structures, real-client look, chest model and vanilla-GUI behaviour, mechanism of the hidden cover, golem guidance route, mineral-block mineability, loot | UNVERIFIED | UNVERIFIED |

복셀 측정과 해석 계산의 대조: V1(복셀 스캔 vs 프레임 산술), V2(층별 천장 높이 vs 구역 규칙), V4(단 평면 vs 프레임 직사각), V12(복셀 부피 vs 상자 합집합), V13(복셀 최저 Y vs 상자 최소값). 서쪽은 별도로 복셀화해 동쪽 복셀과 비교했습니다.

### 10.3 증거 (A 동쪽)

- **V1 PASS** — floor-layer interior X[-37,-17] Z[12,36] = 21x25, 525 cells (525); max clear height 10; frame D=21 W=25
- **V2 PASS** — clear height by |b| (scan at 3 depths) {0: 10, 1: 10, 2: 10, 3: 10, 4: 10, 5: 9, 6: 9, 7: 9, 8: 9, 9: 8, 10: 8, 11: 8, 12: 8}; analytic {0: 10, 1: 10, 2: 10, 3: 10, 4: 10, 5: 9, 6: 9, 7: 9, 8: 9, 9: 8, 10: 8, 11: 8, 12: 8}; zone widths centre 9, intermediate 8, side 8; symmetric True
- **V3 PASS** — clear run through the axis per depth a=0..6: min 5 max 9; 5x5 walker reaches the stair foot True; entrance corridor width 5
- **V4 PASS** — dais plan 15x11 (165 cells, 165), top 3; stair tread heights by run cell [1, 1, 2, 2, 3]; outside cells 2, inset cells 3, stair width 7; monotone step <=1 True
- **V5 PASS** — 4 columns; (id, body, height, base y offset, capital bbox, overlap cells, outside cells, founded on floor) [('COL0', (3, 3), 7, 0, (4, 5), 4, 5, True), ('COL1', (3, 3), 7, 0, (4, 5), 4, 5, True), ('COL2', (3, 3), 7, 0, (5, 5), 4, 5, True), ('COL3', (3, 3), 7, 0, (5, 5), 4, 5, True)]; element overlaps 0
- **V5b PASS** — 48 beam voxels at Y [-30]; min air gap to ceiling 1
- **V6 PASS** — body 5x3 (b x a), base 7x5; local a-range body 15..17 of D=21; lid clearance 2 above True; lateral centring True; base on dais top True; chest height PROPOSED 4
- **V7 PASS** — unreachable []; steps hatch edge -> chest front centre 53; P1 landing -> chest front 212; -> vault entry 41; -> crypt interior 30; front cells reached 5/5
- **V7b PASS** — cells [(-30, -34, 22), (-30, -34, 23), (-30, -34, 24), (-30, -34, 25), (-30, -34, 26)] free True
- **V8 PASS** — volumes {'PILES_P': 160, 'PILES_N': 161} imbalance 0.006; column heights present [1, 2, 3, 4]; cells inside |b|<=2 aisle 0
- **V9 PASS** — flights (id, treads, 1:1, walked headroom, width) [('FA', 9, True, 4, 3), ('FB', 5, True, 4, 3)]; landings [('LA', 5, 5), ('LB', 5, 5)]; total treads 14; vertical drop hall floor -22 -> vault floor -37 = 15
- **V10 PASS** — minimum gap to unlinked existing cavities 5; void overlap with existing voids 0; P4/P4 overlap 0; cells inside HALL_RES outside the pierce volume 0; undeclared face contacts []; declared without contact []; contact with pre-existing voids outside the pierce 0; closer than 2 to unlinked cavities []; elements placed into solid 0
- **V11 PASS** — envelope faults []; minimum solid thickness scanned (excluding declared openings) (lateral, vertical): vault (3, 2), crypt (3, 2) (designed: walls 3, floor/roof 2; required here: walls >= 3, floor/roof >= 2)
- **V12 PASS** — max excess 0 blocks (analytic over 40 boxes); voxels outside +-40 0 (box-union count 0); P4 structure X[-40,23] Z[9,39]
- **V13 PASS** — lowest cavity Y -37 (analytic -37) = depth 37; lowest structure block Y -39 = depth 39; hall floor -22, P2 provisional depth 24; NOT an approved maximum (crypt size and candidate selection open)
- **V14 PASS** — cells carved inside HALL_RES outside the pierce volume 0; pierce volume [-9, -5, -24, -19, 33, 37] (P3 hatch zone X -9..-5 Z 33..37 plus the 2-thick slab); pierce voxels actually carved 42
- **V15 UNVERIFIED** — hall centre (-21.0,21.0); vault centre (-27.0,24.0), 100% of the vault plan lies under the hall, offset 6.7; crypt centre (14.0,24.0), 0% under the hall, offset 35.1
- **V16 UNVERIFIED** — no runtime, NBT or world data exists; open stair cut in the hall floor is a fall hazard until a cover mechanism (not designed) closes it

### 10.4 증거 (B 동쪽)

- **V1 PASS** — floor-layer interior X[6,30] Z[16,36] = 25x21, 525 cells (525); max clear height 10; frame D=25 W=21
- **V2 PASS** — clear height by |b| (scan at 3 depths) {0: 10, 1: 10, 2: 10, 3: 10, 4: 10, 5: 9, 6: 9, 7: 9, 8: 8, 9: 8, 10: 8}; analytic {0: 10, 1: 10, 2: 10, 3: 10, 4: 10, 5: 9, 6: 9, 7: 9, 8: 8, 9: 8, 10: 8}; zone widths centre 9, intermediate 6, side 6; symmetric True
- **V3 PASS** — clear run through the axis per depth a=0..10: min 5 max 9; 5x5 walker reaches the stair foot True; entrance corridor width 5
- **V4 PASS** — dais plan 15x11 (165 cells, 165), top 3; stair tread heights by run cell [1, 1, 2, 2, 3]; outside cells 2, inset cells 3, stair width 7; monotone step <=1 True
- **V5 PASS** — 4 columns; (id, body, height, base y offset, capital bbox, overlap cells, outside cells, founded on floor) [('COL0', (3, 3), 7, 0, (4, 5), 4, 5, True), ('COL1', (3, 3), 7, 0, (4, 5), 4, 5, True), ('COL2', (3, 3), 7, 0, (5, 5), 4, 5, True), ('COL3', (3, 3), 7, 0, (5, 5), 4, 5, True)]; element overlaps 0
- **V5b PASS** — 48 beam voxels at Y [-23]; min air gap to ceiling 1
- **V6 PASS** — body 5x3 (b x a), base 7x5; local a-range body 19..21 of D=25; lid clearance 2 above True; lateral centring True; base on dais top True; chest height PROPOSED 4
- **V7 PASS** — unreachable []; steps hatch edge -> chest front centre 42; P1 landing -> chest front 201; -> vault entry 26; -> crypt interior 54; front cells reached 5/5
- **V7b PASS** — cells [(23, -27, 24), (23, -27, 25), (23, -27, 26), (23, -27, 27), (23, -27, 28)] free True
- **V8 PASS** — volumes {'PILES_P': 141, 'PILES_N': 139} imbalance 0.014; column heights present [1, 2, 3, 4]; cells inside |b|<=2 aisle 0
- **V9 PASS** — flights (id, treads, 1:1, walked headroom, width) [('FA', 7, True, 4, 3)]; landings [('LA', 5, 5)]; total treads 7; vertical drop hall floor -22 -> vault floor -30 = 8
- **V10 PASS** — minimum gap to unlinked existing cavities 2; void overlap with existing voids 0; P4/P4 overlap 0; cells inside HALL_RES outside the pierce volume 0; undeclared face contacts []; declared without contact []; contact with pre-existing voids outside the pierce 0; closer than 2 to unlinked cavities []; elements placed into solid 0
- **V11 PASS** — envelope faults []; minimum solid thickness scanned (excluding declared openings) (lateral, vertical): vault (3, 2), crypt (3, 2) (designed: walls 3, floor/roof 2; required here: walls >= 3, floor/roof >= 2)
- **V12 PASS** — max excess 0 blocks (analytic over 32 boxes); voxels outside +-40 0 (box-union count 0); P4 structure X[-11,33] Z[-10,39]
- **V13 PASS** — lowest cavity Y -30 (analytic -30) = depth 30; lowest structure block Y -32 = depth 32; hall floor -22, P2 provisional depth 24; NOT an approved maximum (crypt size and candidate selection open)
- **V14 PASS** — cells carved inside HALL_RES outside the pierce volume 0; pierce volume [-9, -5, -24, -19, 33, 37] (P3 hatch zone X -9..-5 Z 33..37 plus the 2-thick slab); pierce voxels actually carved 42
- **V15 UNVERIFIED** — hall centre (-21.0,21.0); vault centre (18.0,26.0), 0% of the vault plan lies under the hall, offset 39.3; crypt centre (15.0,-1.0), 0% under the hall, offset 42.2
- **V16 UNVERIFIED** — no runtime, NBT or world data exists; open stair cut in the hall floor is a fall hazard until a cover mechanism (not designed) closes it

## 11. 음성 대조 테스트

결함을 일부러 주입한 스펙 19개 중 19개 검출. 무결함 대조군은 두 후보 모두 FAIL 0. (V8은 처음에 이름이 다른 통로 더미를 못 잡아 검증기를 강화했습니다.)

| 주입한 결함 | 후보 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- | --- |
| vault interior 23 wide instead of 25 (candidate A frame) | A | V1 | A: V1 | 검출 |
| centre ceiling zone only 9 high (Q68) | A,B | V2 | A: V1, V2; B: V1, V2 | 검출 |
| pile dropped into the 5-wide aisle (Q70) | A,B | V3, V8 | A: V3, V8; B: V3, V8 | 검출 |
| dais built 4 high (Q71) | A,B | V4 | A: V10, V4, V5, V7, V7b; B: V10, V4, V5, V7, V7b | 검출 |
| front stair narrowed to 5 (Q89) | A,B | V4 | A: V4; B: V4 | 검출 |
| column lifted off the floor (not floor-founded, Q86) | A,B | V5 | A: V10, V4, V5; B: V10, V4, V5 | 검출 |
| column capital grown to 7x7 (Q84) | A | V5 | A: V5 | 검출 |
| chest body 4 wide instead of 5 (Q72) | A,B | V6 | A: V6; B: V6 | 검출 |
| chest base 8 wide (Q93 max 7x5) | A,B | V6 | A: V6; B: V6 | 검출 |
| block placed in front of the chest (front access lost) | A,B | V7, V7b | A: V7, V7b, V8; B: V7, V7b, V8 | 검출 |
| stair treads skip every other block (gradient 2:1) | A | V7 | A: V10, V14, V7, V7b | 검출 |
| stair headroom 3 instead of 4 | A,B | V9 | A: V9; B: V9 | 검출 |
| vault raised 10 blocks into the royal-hall reservation | A | V10 | A: V10, V11, V14, V7 | 검출 |
| candidate B vault pushed 10 east past X=+40 | B | V12 | B: V10, V12, V7, V7b | 검출 |
| declared link vault entrance <-> corridor removed | A | V10 | A: V10 | 검출 |
| crypt interior slid into the vault | A | V10 | A: V1, V10, V11, V2, V3, V4, V5, V7 | 검출 |
| beam raised into the stepped ceiling (Q96 independence) | A,B | V5b | A: V5b; B: V5b | 검출 |
| only one side has mineral piles (balance, Q69) | A,B | V8 | A: V8; B: V8 | 검출 |
| stair carved under the hall outside the hatch pierce volume | A | V10 | A: V10, V14 | 검출 |

## 12. 구조적 간섭 요약

- 기존 공동(P1·P2·P3 홀)과 P4 공동의 겹침 0, P4끼리 겹침 0, 홀 예약 안 굴착은 선언된 관통 체적 42복셀뿐(두 후보 동일), 미선언 접촉 0.
- 선언되지 않은 공동과의 최소 간격: A 5, B 2(기준 ≥2).
- 보물창고·묘실 벽 3, 바닥·지붕 2 측정.
- 홀 요소(기둥·석관단 등)는 한 칸도 변경하지 않았음(V14).

## 13. 미해결·미검증

**UNVERIFIED**: 지형·월드젠·청크·이웃 구조물, 실제 클라이언트 시각, 상자 모델·바닐라 GUI 동작, 숨은 덮개 메커니즘과 열린 단면의 낙하 위험, 황금 골렘 안내 경로(홀 석관단 → 입구 구역 19걸음 + 입구 구역 → 상자 정면 A 53 / B 42걸음)와 실패 시 비잠금 대체 경로, 광물 블록 채굴 가능 여부, 거대 상자 높이·용량, 비밀 묘실 크기·벽감 개수, 최대 깊이(Q98), Q97 해석, 홀 평면 위 보스 전투 중 열린 단면 영향.

## 14. 소유자 결정 필요 사항

1. **후보 A(적층) vs B(측방)**: 선택 근거는 §6. 선택 전에는 최대 깊이를 정할 수 없음.
2. **D-3 동·서 방향**: 미확정 유지.
3. **Q15 비밀 묘실**: 크기·개수·위치 승인(현재 대표 후보), 보물창고 경유 여부(현재 별도 지선).
4. **숨은 입구 메커니즘/덮개**와 열린 단면 처리.
5. **P4 PROPOSED 좌표를 P5 통합 검증의 잠정 기준선으로 채택**할지.
6. 선택 사항: Q16 보관 벽감 배치 여부, 계단 폭 3·머리 높이 4 등 PROPOSED 통로 규격.

## 15. 보존 확인 및 완료 기준

- 변경은 신규 파일뿐: `tools/st01-p4/*`, `docs/design/st01-p4/*`, `docs/design/st01-p3/ST01_P3_CONDITIONAL_APPROVAL_RECORD_2026-11-07.md`. 커밋 전 `git diff --name-status 37fed9a6 HEAD`로 확인.
- Q01–Q100 문서, 권위 인덱스, P1–P3 모델·검증기·보고서·생성물, `src/**`는 수정하지 않았고 PR·병합은 없습니다.
- **P4 설계 완료는 실제 NBT 또는 월드젠 구현 승인이 아니며, 게임 내 검증 완료도 아닙니다.**

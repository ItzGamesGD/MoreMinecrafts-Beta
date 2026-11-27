# ST-01 P6 — 구성요소별 좌표 명세 (2026-11-07)

상태: **생성 문서.** 값은 모두 `tools/st01-p6`의 독립 레지스트리(상자 산술)에서 나온다. 이 문서는 구현 승인이 아니며, 승인 상태 열의 값을 그대로 구분한다.

좌표 규칙: 원점=피라미드 지면 중심, X 동+, Z 남+(입구), Y 층 인덱스, 범위는 포함. "내부 치수"=공동 bbox의 X×Y×Z. "외부 치수"는 P2는 수평 벽 2칸, P4는 P4 외피 상자(바닥 2칸 포함), P3 홀은 예약 상자이며 그 밖은 정의되지 않아 `—`. "점유 복셀"은 해당 요소 상자들의 합집합 칸 수(공동 또는 홀 내 고체 요소)이다.

상태 집계(동쪽, 요소 141개): APPROVED 7, CONDITIONALLY APPROVED 123, PROPOSED 10, RESERVED 1. 마지막 열은 "구현 의존성"이다. 모든 요소의 게임 내 거동·월드젠·NBT는 UNVERIFIED이다.

## 1. 동쪽 통합 모델 (P6-E)

| ID | 종류 | 좌표 범위(포함) | 내부 치수 | 외부 치수 | 점유 복셀(공동 또는 요소) | 연결 | 상태 | 검증 근거 | 구현 의존성 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1:S00 | exterior | X -12..12 / Y 0..11 / Z 29..40 | 25×12×12 | — | 3600 | S01 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S01 | opening | X -4..4 / Y 0..9 / Z 25..28 | 9×10×4 | — | 360 | S00, S02 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S02 | room | X -10..10 / Y 0..11 / Z 21..24 | 21×12×4 | — | 1008 | S01, S03 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S03 | opening | X -6..6 / Y 0..10 / Z 19..20 | 13×11×2 | — | 286 | S02, S04 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S04 | room | X -18..18 / Y 0..17 / Z -6..18 | 37×18×25 | — | 16650 | S03, S05, S08 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S05 | stair | X -5..5 / Y -6..-1 / Z -6..-1 | 11×6×6 | — | 396 | S04, S06 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S06 | room | X -7..7 / Y -6..-1 / Z -14..-7 | 15×6×8 | — | 720 | S05, S07 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S08 | opening | X -3..3 / Y 7..12 / Z -8..-7 | 7×6×2 | — | 84 | S04, S09 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S09 | stair | X -3..3 / Y 8..13 / Z -11..-9 | 7×6×3 | — | 126 | S08, S10 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S10 | room | X -7..7 / Y 11..17 / Z -20..-12 | 15×7×9 | — | 945 | S09 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1R:S07 | reserved | X 8..15 / Y -6..-3 / Z -13..-7 | 8×4×7 | — | 224 | — | RESERVED | P1 record; P2 model | — |
| P2:C1 | main | X 8..28 / Y -6..-2 / Z -13..-5 | 21×5×9 | 25×5×13 | 745 | P1:S06, BR1, BR2, BR3, V12 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR1 | optional | X 11..13 / Y -6..-2 / Z -17..-14 | 3×5×4 | 7×5×8 | 60 | C1, T1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T1 | optional | X 9..15 / Y -6..-2 / Z -24..-18 | 7×5×7 | 11×5×11 | 245 | BR1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR2 | optional | X 20..22 / Y -6..-2 / Z -17..-14 | 3×5×4 | 7×5×8 | 60 | C1, T2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T2 | optional | X 18..24 / Y -6..-2 / Z -26..-18 | 7×5×9 | 11×5×13 | 315 | BR2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR3 | optional | X 11..13 / Y -6..-3 / Z -6..-4 | 3×4×3 | 7×4×7 | 36 | C1, T3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T3 | optional | X 9..15 / Y -6..-3 / Z -3..3 | 7×4×7 | 11×4×11 | 196 | BR3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:V12 | main | X 29..37 / Y -6..-2 / Z -12..-2 | 9×5×11 | 13×5×15 | 495 | C1, FA | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FA | stair | X 30..36 / Y -10..-3 / Z -1..2 | 7×8×4 | 11×8×8 | 140 | V12, LND12 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:LND12 | main | X 29..37 / Y -10..-6 / Z 3..11 | 9×5×9 | 13×5×13 | 405 | FA, FB | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FB | stair | X 25..28 / Y -14..-7 / Z 4..10 | 4×8×7 | 8×8×11 | 140 | LND12, C2 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:C2 | main | X 14..24 / Y -14..-10 / Z 4..10 | 11×5×7 | 15×5×11 | 385 | FB, BRB1, BRB2, BRB3, V23 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB1 | optional | X 22..24 / Y -14..-10 / Z 11..14 | 3×5×4 | 7×5×8 | 60 | C2, B1 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B1 | optional | X 22..28 / Y -14..-10 / Z 15..21 | 7×5×7 | 11×5×11 | 245 | BRB1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB2 | optional | X 16..18 / Y -14..-10 / Z 11..14 | 3×5×4 | 7×5×8 | 60 | C2, B2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B2 | optional | X 11..19 / Y -14..-7 / Z 15..21 | 9×8×7 | 13×8×11 | 504 | BRB2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB3 | optional | X 19..21 / Y -14..-10 / Z 0..3 | 3×5×4 | 7×5×8 | 60 | C2, B3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B3 | optional | X 17..23 / Y -14..-10 / Z -9..-1 | 7×5×9 | 11×5×13 | 315 | BRB3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:V23 | main | X 3..13 / Y -14..-10 / Z 3..11 | 11×5×9 | 15×5×13 | 495 | C2, FC | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FC | stair | X 5..11 / Y -18..-11 / Z -1..2 | 7×8×4 | 11×8×8 | 140 | V23, LND23 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:LND23 | main | X 4..12 / Y -18..-14 / Z -10..-2 | 9×5×9 | 13×5×13 | 405 | FC, FD | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FD | stair | X 0..3 / Y -22..-15 / Z -9..-3 | 4×8×7 | 8×8×11 | 140 | LND23, C3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:C3 | main | X -3..-1 / Y -22..-18 / Z -9..-3 | 3×5×7 | 7×5×11 | 105 | FD, R | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:R | main | X -11..-4 / Y -22..-19 / Z -9..-5 | 8×4×5 | 12×4×9 | 160 | C3, A | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:A | main | X -30..-12 / Y -22..-13 / Z -12..-2 | 19×10×11 | 23×10×15 | 2090 | R, T | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T | main | X -22..-20 / Y -22..-20 / Z -1..4 | 3×3×6 | 7×3×10 | 54 | A | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P3:HALL | hall | X -37..-5 / Y -22..-7 / Z 5..37 | 33×16×33 | 39×20×39 | 17424 | P3:DOOR | CONDITIONALLY APPROVED | P3 conditional approval record + P3 verifier | boss: royal chest anchor + emergence point must be supplied by structure |
| P3:DOOR | port | X -22..-20 / Y -22..-20 / Z 2..4 | 3×3×3 | — | 27 | P2:T, P3:HALL | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:base | pillar_base | X -32..-28 / Y -22 / Z 7..10 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:shaft | pillar_shaft | X -31..-29 / Y -21..-19 / Z 7..9 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:cap | pillar_cap | X -33..-27 / Y -18..-16 / Z 6..11 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:base | pillar_base | X -14..-10 / Y -22 / Z 7..10 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:shaft | pillar_shaft | X -13..-11 / Y -21..-19 / Z 7..9 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:cap | pillar_cap | X -15..-9 / Y -18..-16 / Z 6..11 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:base | pillar_base | X -32..-28 / Y -22 / Z 32..35 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:shaft | pillar_shaft | X -31..-29 / Y -21..-19 / Z 33..35 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:cap | pillar_cap | X -33..-27 / Y -18..-16 / Z 31..36 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:base | pillar_base | X -14..-10 / Y -22 / Z 32..35 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:shaft | pillar_shaft | X -13..-11 / Y -21..-19 / Z 33..35 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:cap | pillar_cap | X -15..-9 / Y -18..-16 / Z 31..36 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:base | pillar_base | X -35..-32 / Y -22 / Z 10..14 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:shaft | pillar_shaft | X -35..-33 / Y -21..-19 / Z 11..13 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:cap | pillar_cap | X -36..-31 / Y -18..-16 / Z 9..15 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:base | pillar_base | X -35..-32 / Y -22 / Z 19..23 | 4×1×5 | — | 20 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:shaft | pillar_shaft | X -35..-33 / Y -21..-19 / Z 20..22 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:cap | pillar_cap | X -36..-31 / Y -18..-16 / Z 18..24 | 6×3×7 | — | 84 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:base | pillar_base | X -35..-32 / Y -22 / Z 28..32 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:shaft | pillar_shaft | X -35..-33 / Y -21..-19 / Z 29..31 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:cap | pillar_cap | X -36..-31 / Y -18..-16 / Z 27..33 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:base | pillar_base | X -10..-7 / Y -22 / Z 10..14 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:shaft | pillar_shaft | X -9..-7 / Y -21..-19 / Z 11..13 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:cap | pillar_cap | X -11..-6 / Y -18..-16 / Z 9..15 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:base | pillar_base | X -10..-7 / Y -22 / Z 19..23 | 4×1×5 | — | 20 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:shaft | pillar_shaft | X -9..-7 / Y -21..-19 / Z 20..22 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:cap | pillar_cap | X -11..-6 / Y -18..-16 / Z 18..24 | 6×3×7 | — | 84 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:base | pillar_base | X -10..-7 / Y -22 / Z 28..32 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:shaft | pillar_shaft | X -9..-7 / Y -21..-19 / Z 29..31 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:cap | pillar_cap | X -11..-6 / Y -18..-16 / Z 27..33 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_N1 | lintel | X -31..-29 / Y -15 / Z 7..9 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_N2 | lintel | X -13..-11 / Y -15 / Z 7..9 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_S1 | lintel | X -31..-29 / Y -15 / Z 33..35 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_S2 | lintel | X -13..-11 / Y -15 / Z 33..35 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_W1 | lintel | X -35..-33 / Y -15 / Z 11..31 | 3×1×21 | — | 63 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_E1 | lintel | X -9..-7 / Y -15 / Z 11..31 | 3×1×21 | — | 63 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:plinth | obelisk | X -23..-19 / Y -22..-21 / Z 19..23 | 5×2×5 | — | 50 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:shaft | obelisk | X -22..-20 / Y -20..-12 / Z 20..22 | 3×9×3 | — | 81 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:tip | obelisk | X -21 / Y -11..-10 / Z 21 | 1×2×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:DAIS | dais | X -26..-16 / Y -22..-21 / Z 32..37 | 11×2×6 | — | 132 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:DAIS:tread | dais | X -25..-17 / Y -22 / Z 30..31 | 9×1×2 | — | 18 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:SARCOPHAGUS | sarcophagus | X -22..-20 / Y -20..-19 / Z 34..37 | 3×2×4 | — | 24 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:CHEST_PRIMARY | chest_primary | X -21 / Y -20 / Z 33 | 1×1×1 | — | 1 | — | CONDITIONALLY APPROVED | P3 verifier | boss/loot: chest interaction hook (TombGuardianEncounters.onRoyalChestInteraction) not wired |
| P3:ALCOVE_E:col1 | alcove_col | X -4 / Y -22..-19 / Z 24 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:ALCOVE_E:col2 | alcove_col | X -4 / Y -22..-19 / Z 27 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:CHEST_SECONDARY | chest_secondary | X -3 / Y -22 / Z 25..26 | 1×1×2 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | boss/loot: chest interaction hook (TombGuardianEncounters.onRoyalChestInteraction) not wired |
| P3:NICHE_W:col1 | alcove_col | X -38 / Y -22..-19 / Z 24 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W:col2 | alcove_col | X -38 / Y -22..-19 / Z 27 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_W | statue | X -39 / Y -22..-18 / Z 25..26 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A:col1 | alcove_col | X -4 / Y -22..-19 / Z 15 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A:col2 | alcove_col | X -4 / Y -22..-19 / Z 18 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_E_A | statue | X -3 / Y -22..-18 / Z 16..17 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A:col1 | alcove_col | X -38 / Y -22..-19 / Z 15 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A:col2 | alcove_col | X -38 / Y -22..-19 / Z 18 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_W_A | statue | X -39 / Y -22..-18 / Z 16..17 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NW:bowl | brazier | X -26..-25 / Y -22 / Z 4 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NE:bowl | brazier | X -17..-16 / Y -22 / Z 4 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SW:bowl | brazier | X -35..-34 / Y -22 / Z 38 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SE:bowl | brazier | X -8..-7 / Y -22 / Z 38 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:ALCOVE_E | alcove | X -4..-3 / Y -22..-18 / Z 24..27 | 2×5×4 | — | 40 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W | statue_niche | X -39..-38 / Y -22..-17 / Z 24..27 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A | statue_niche | X -4..-3 / Y -22..-17 / Z 15..18 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A | statue_niche | X -39..-38 / Y -22..-17 / Z 15..18 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NW | brazier_recess | X -26..-25 / Y -22..-20 / Z 4 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NE | brazier_recess | X -17..-16 / Y -22..-20 / Z 4 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SW | brazier_recess | X -35..-34 / Y -22..-20 / Z 38 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SE | brazier_recess | X -8..-7 / Y -22..-20 / Z 38 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P4:FA | stair | X -9..-3 / Y -29..-20 / Z 33..35 | 7×10×3 | 11×14×7 | 84 | HALL, LA | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:LA | landing | X -2..2 / Y -30..-26 / Z 32..36 | 5×5×5 | 9×9×9 | 125 | FA, CB1 | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CB1 | corr | X -2..2 / Y -30..-25 / Z 24..31 | 5×6×8 | 9×10×12 | 240 | LA, VAULT_ENT, CC | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:VAULT | vault | X 6..30 / Y -30..-21 / Z 16..36 | 25×10×21 | 31×14×27 | 4800 | VAULT_ENT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:VAULT_ENT | corr | X 3..5 / Y -30..-25 / Z 24..28 | 3×6×5 | 7×10×9 | 90 | CB1, VAULT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CC | corr | X 0..2 / Y -30..-26 / Z 0..23 | 3×5×24 | 7×9×28 | 360 | CB1, CC3 | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CC3 | corr | X 3..7 / Y -30..-26 / Z 0..2 | 5×5×3 | 9×9×7 | 75 | CC, CRYPT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CRYPT | crypt | X 8..22 / Y -30..-24 / Z -7..5 | 15×7×13 | 21×11×19 | 1365 | CRYPT_NICHE_N0, CRYPT_NICHE_S0, CRYPT_NICHE_N1, CRYPT_NICHE_S1, CRYPT_NICHE_N2, CRYPT_NICHE_S2, CC3 | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N0 | niche | X 9..11 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S0 | niche | X 9..11 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N1 | niche | X 13..15 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S1 | niche | X 13..15 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N2 | niche | X 17..19 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S2 | niche | X 17..19 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4E:COL0:base | column_base | X 28..30 / Y -30 / Z 32..34 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL0:shaft | column_shaft | X 28..30 / Y -29..-26 / Z 32..34 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL0:cap | column_cap | X 27..30 / Y -25..-24 / Z 31..35 | 4×2×5 | — | 38 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:base | column_base | X 28..30 / Y -30 / Z 18..20 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:shaft | column_shaft | X 28..30 / Y -29..-26 / Z 18..20 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:cap | column_cap | X 27..30 / Y -25..-24 / Z 17..21 | 4×2×5 | — | 38 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:base | column_base | X 18..20 / Y -30 / Z 32..34 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:shaft | column_shaft | X 18..20 / Y -29..-26 / Z 32..34 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:cap | column_cap | X 17..21 / Y -25..-24 / Z 31..35 | 5×2×5 | — | 46 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:base | column_base | X 18..20 / Y -30 / Z 18..20 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:shaft | column_shaft | X 18..20 / Y -29..-26 / Z 18..20 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:cap | column_cap | X 17..21 / Y -25..-24 / Z 17..21 | 5×2×5 | — | 46 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:DAIS | dais | X 19..29 / Y -30..-28 / Z 19..33 | 11×3×15 | — | 384 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:DAIS_STAIR | dais_stair | X 17..21 / Y -30..-28 / Z 23..29 | 5×3×7 | — | 63 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:BEAMS | beam | X 19..29 / Y -23 / Z 19..33 | 11×1×15 | — | 48 | — | PROPOSED | P4 verifier | — |
| P4E:CHEST_BASE | chest_base | X 24..28 / Y -27 / Z 23..29 | 5×1×7 | — | 35 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:CHEST_BODY | chest_body | X 25..27 / Y -26..-25 / Z 24..28 | 3×2×5 | — | 30 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:CHEST_LID | chest_lid | X 25..27 / Y -24 / Z 24..28 | 3×1×5 | — | 15 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:PILES_P | pile | X 6..30 / Y -30..-27 / Z 29..36 | 25×4×8 | — | 141 | — | PROPOSED | P4 verifier | loot/decor: undecided (Q-doc: mineable or decoration UNDECIDED) |
| P4E:PILES_N | pile | X 6..30 / Y -30..-27 / Z 16..23 | 25×4×8 | — | 139 | — | PROPOSED | P4 verifier | loot/decor: undecided (Q-doc: mineable or decoration UNDECIDED) |
| P4E:CRYPT:SARC | sarcophagus | X 14..16 / Y -30..-29 / Z -3..1 | 3×2×5 | — | 30 | — | CONDITIONALLY APPROVED | P4 verifier | — |

## 2. 서쪽 통합 모델 (P6-W, 정확한 X 거울)

서쪽 표는 동쪽의 [x0,x1]→[−x1,−x0] 거울이며 독립으로 다시 복셀화·검증했다(복셀 일치 확인: closed True, open True, 요소 bbox True).

| ID | 종류 | 좌표 범위(포함) | 내부 치수 | 외부 치수 | 점유 복셀(공동 또는 요소) | 연결 | 상태 | 검증 근거 | 구현 의존성 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1:S00 | exterior | X -12..12 / Y 0..11 / Z 29..40 | 25×12×12 | — | 3600 | S01 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S01 | opening | X -4..4 / Y 0..9 / Z 25..28 | 9×10×4 | — | 360 | S00, S02 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S02 | room | X -10..10 / Y 0..11 / Z 21..24 | 21×12×4 | — | 1008 | S01, S03 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S03 | opening | X -6..6 / Y 0..10 / Z 19..20 | 13×11×2 | — | 286 | S02, S04 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S04 | room | X -18..18 / Y 0..17 / Z -6..18 | 37×18×25 | — | 16650 | S03, S05, S08 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S05 | stair | X -5..5 / Y -6..-1 / Z -6..-1 | 11×6×6 | — | 396 | S04, S06 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S06 | room | X -7..7 / Y -6..-1 / Z -14..-7 | 15×6×8 | — | 720 | S05, S07 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S08 | opening | X -3..3 / Y 7..12 / Z -8..-7 | 7×6×2 | — | 84 | S04, S09 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S09 | stair | X -3..3 / Y 8..13 / Z -11..-9 | 7×6×3 | — | 126 | S08, S10 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1:S10 | room | X -7..7 / Y 11..17 / Z -20..-12 | 15×7×9 | — | 945 | S09 | CONDITIONALLY APPROVED | P1 conditional approval record + P1 verifier | worldgen: pyramid shell/terrain fit UNVERIFIED |
| P1R:S07 | reserved | X -15..-8 / Y -6..-3 / Z -13..-7 | 8×4×7 | — | 224 | — | RESERVED | P1 record; P2 model | — |
| P2:C1 | main | X -28..-8 / Y -6..-2 / Z -13..-5 | 21×5×9 | 25×5×13 | 745 | P1:S06, BR1, BR2, BR3, V12 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR1 | optional | X -13..-11 / Y -6..-2 / Z -17..-14 | 3×5×4 | 7×5×8 | 60 | C1, T1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T1 | optional | X -15..-9 / Y -6..-2 / Z -24..-18 | 7×5×7 | 11×5×11 | 245 | BR1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR2 | optional | X -22..-20 / Y -6..-2 / Z -17..-14 | 3×5×4 | 7×5×8 | 60 | C1, T2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T2 | optional | X -24..-18 / Y -6..-2 / Z -26..-18 | 7×5×9 | 11×5×13 | 315 | BR2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BR3 | optional | X -13..-11 / Y -6..-3 / Z -6..-4 | 3×4×3 | 7×4×7 | 36 | C1, T3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T3 | optional | X -15..-9 / Y -6..-3 / Z -3..3 | 7×4×7 | 11×4×11 | 196 | BR3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:V12 | main | X -37..-29 / Y -6..-2 / Z -12..-2 | 9×5×11 | 13×5×15 | 495 | C1, FA | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FA | stair | X -36..-30 / Y -10..-3 / Z -1..2 | 7×8×4 | 11×8×8 | 140 | V12, LND12 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:LND12 | main | X -37..-29 / Y -10..-6 / Z 3..11 | 9×5×9 | 13×5×13 | 405 | FA, FB | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FB | stair | X -28..-25 / Y -14..-7 / Z 4..10 | 4×8×7 | 8×8×11 | 140 | LND12, C2 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:C2 | main | X -24..-14 / Y -14..-10 / Z 4..10 | 11×5×7 | 15×5×11 | 385 | FB, BRB1, BRB2, BRB3, V23 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB1 | optional | X -24..-22 / Y -14..-10 / Z 11..14 | 3×5×4 | 7×5×8 | 60 | C2, B1 | APPROVED | P5 candidate C + P6 overlay | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B1 | optional | X -28..-22 / Y -14..-10 / Z 15..21 | 7×5×7 | 11×5×11 | 245 | BRB1 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB2 | optional | X -18..-16 / Y -14..-10 / Z 11..14 | 3×5×4 | 7×5×8 | 60 | C2, B2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B2 | optional | X -19..-11 / Y -14..-7 / Z 15..21 | 9×8×7 | 13×8×11 | 504 | BRB2 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:BRB3 | optional | X -21..-19 / Y -14..-10 / Z 0..3 | 3×5×4 | 7×5×8 | 60 | C2, B3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:B3 | optional | X -23..-17 / Y -14..-10 / Z -9..-1 | 7×5×9 | 11×5×13 | 315 | BRB3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:V23 | main | X -13..-3 / Y -14..-10 / Z 3..11 | 11×5×9 | 15×5×13 | 495 | C2, FC | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FC | stair | X -11..-5 / Y -18..-11 / Z -1..2 | 7×8×4 | 11×8×8 | 140 | V23, LND23 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:LND23 | main | X -12..-4 / Y -18..-14 / Z -10..-2 | 9×5×9 | 13×5×13 | 405 | FC, FD | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:FD | stair | X -3..0 / Y -22..-15 / Z -9..-3 | 4×8×7 | 8×8×11 | 140 | LND23, C3 | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:C3 | main | X 1..3 / Y -22..-18 / Z -9..-3 | 3×5×7 | 7×5×11 | 105 | FD, R | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:R | main | X 4..11 / Y -22..-19 / Z -9..-5 | 8×4×5 | 12×4×9 | 160 | C3, A | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:A | main | X 12..30 / Y -22..-13 / Z -12..-2 | 19×10×11 | 23×10×15 | 2090 | R, T | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P2:T | main | X 20..22 / Y -22..-20 / Z -1..4 | 3×3×6 | 7×3×10 | 54 | A | CONDITIONALLY APPROVED | P2 conditional approval record + P2 engine | worldgen: NBT not created; wall thickness 2 assumed (P2) |
| P3:HALL | hall | X 5..37 / Y -22..-7 / Z 5..37 | 33×16×33 | 39×20×39 | 17424 | P3:DOOR | CONDITIONALLY APPROVED | P3 conditional approval record + P3 verifier | boss: royal chest anchor + emergence point must be supplied by structure |
| P3:DOOR | port | X 20..22 / Y -22..-20 / Z 2..4 | 3×3×3 | — | 27 | P2:T, P3:HALL | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:base | pillar_base | X 28..32 / Y -22 / Z 7..10 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:shaft | pillar_shaft | X 29..31 / Y -21..-19 / Z 7..9 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N1:cap | pillar_cap | X 27..33 / Y -18..-16 / Z 6..11 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:base | pillar_base | X 10..14 / Y -22 / Z 7..10 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:shaft | pillar_shaft | X 11..13 / Y -21..-19 / Z 7..9 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_N2:cap | pillar_cap | X 9..15 / Y -18..-16 / Z 6..11 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:base | pillar_base | X 28..32 / Y -22 / Z 32..35 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:shaft | pillar_shaft | X 29..31 / Y -21..-19 / Z 33..35 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S1:cap | pillar_cap | X 27..33 / Y -18..-16 / Z 31..36 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:base | pillar_base | X 10..14 / Y -22 / Z 32..35 | 5×1×4 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:shaft | pillar_shaft | X 11..13 / Y -21..-19 / Z 33..35 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_S2:cap | pillar_cap | X 9..15 / Y -18..-16 / Z 31..36 | 7×3×6 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:base | pillar_base | X 32..35 / Y -22 / Z 10..14 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:shaft | pillar_shaft | X 33..35 / Y -21..-19 / Z 11..13 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W1:cap | pillar_cap | X 31..36 / Y -18..-16 / Z 9..15 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:base | pillar_base | X 32..35 / Y -22 / Z 19..23 | 4×1×5 | — | 20 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:shaft | pillar_shaft | X 33..35 / Y -21..-19 / Z 20..22 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W2:cap | pillar_cap | X 31..36 / Y -18..-16 / Z 18..24 | 6×3×7 | — | 84 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:base | pillar_base | X 32..35 / Y -22 / Z 28..32 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:shaft | pillar_shaft | X 33..35 / Y -21..-19 / Z 29..31 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_W3:cap | pillar_cap | X 31..36 / Y -18..-16 / Z 27..33 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:base | pillar_base | X 7..10 / Y -22 / Z 10..14 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:shaft | pillar_shaft | X 7..9 / Y -21..-19 / Z 11..13 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E1:cap | pillar_cap | X 6..11 / Y -18..-16 / Z 9..15 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:base | pillar_base | X 7..10 / Y -22 / Z 19..23 | 4×1×5 | — | 20 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:shaft | pillar_shaft | X 7..9 / Y -21..-19 / Z 20..22 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E2:cap | pillar_cap | X 6..11 / Y -18..-16 / Z 18..24 | 6×3×7 | — | 84 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:base | pillar_base | X 7..10 / Y -22 / Z 28..32 | 4×1×5 | — | 19 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:shaft | pillar_shaft | X 7..9 / Y -21..-19 / Z 29..31 | 3×3×3 | — | 27 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:P_E3:cap | pillar_cap | X 6..11 / Y -18..-16 / Z 27..33 | 6×3×7 | — | 78 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_N1 | lintel | X 29..31 / Y -15 / Z 7..9 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_N2 | lintel | X 11..13 / Y -15 / Z 7..9 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_S1 | lintel | X 29..31 / Y -15 / Z 33..35 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_S2 | lintel | X 11..13 / Y -15 / Z 33..35 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_W1 | lintel | X 33..35 / Y -15 / Z 11..31 | 3×1×21 | — | 63 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:L_E1 | lintel | X 7..9 / Y -15 / Z 11..31 | 3×1×21 | — | 63 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:plinth | obelisk | X 19..23 / Y -22..-21 / Z 19..23 | 5×2×5 | — | 50 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:shaft | obelisk | X 20..22 / Y -20..-12 / Z 20..22 | 3×9×3 | — | 81 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:OBELISK:tip | obelisk | X 21 / Y -11..-10 / Z 21 | 1×2×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:DAIS | dais | X 16..26 / Y -22..-21 / Z 32..37 | 11×2×6 | — | 132 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:DAIS:tread | dais | X 17..25 / Y -22 / Z 30..31 | 9×1×2 | — | 18 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:SARCOPHAGUS | sarcophagus | X 20..22 / Y -20..-19 / Z 34..37 | 3×2×4 | — | 24 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:CHEST_PRIMARY | chest_primary | X 21 / Y -20 / Z 33 | 1×1×1 | — | 1 | — | CONDITIONALLY APPROVED | P3 verifier | boss/loot: chest interaction hook (TombGuardianEncounters.onRoyalChestInteraction) not wired |
| P3:ALCOVE_E:col1 | alcove_col | X 4 / Y -22..-19 / Z 24 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:ALCOVE_E:col2 | alcove_col | X 4 / Y -22..-19 / Z 27 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:CHEST_SECONDARY | chest_secondary | X 3 / Y -22 / Z 25..26 | 1×1×2 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | boss/loot: chest interaction hook (TombGuardianEncounters.onRoyalChestInteraction) not wired |
| P3:NICHE_W:col1 | alcove_col | X 38 / Y -22..-19 / Z 24 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W:col2 | alcove_col | X 38 / Y -22..-19 / Z 27 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_W | statue | X 39 / Y -22..-18 / Z 25..26 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A:col1 | alcove_col | X 4 / Y -22..-19 / Z 15 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A:col2 | alcove_col | X 4 / Y -22..-19 / Z 18 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_E_A | statue | X 3 / Y -22..-18 / Z 16..17 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A:col1 | alcove_col | X 38 / Y -22..-19 / Z 15 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A:col2 | alcove_col | X 38 / Y -22..-19 / Z 18 | 1×4×1 | — | 4 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:STATUE_W_A | statue | X 39 / Y -22..-18 / Z 16..17 | 1×5×2 | — | 10 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NW:bowl | brazier | X 25..26 / Y -22 / Z 4 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NE:bowl | brazier | X 16..17 / Y -22 / Z 4 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SW:bowl | brazier | X 34..35 / Y -22 / Z 38 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SE:bowl | brazier | X 7..8 / Y -22 / Z 38 | 2×1×1 | — | 2 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:ALCOVE_E | alcove | X 3..4 / Y -22..-18 / Z 24..27 | 2×5×4 | — | 40 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W | statue_niche | X 38..39 / Y -22..-17 / Z 24..27 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_E_A | statue_niche | X 3..4 / Y -22..-17 / Z 15..18 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:NICHE_W_A | statue_niche | X 38..39 / Y -22..-17 / Z 15..18 | 2×6×4 | — | 48 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NW | brazier_recess | X 25..26 / Y -22..-20 / Z 4 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_NE | brazier_recess | X 16..17 / Y -22..-20 / Z 4 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SW | brazier_recess | X 34..35 / Y -22..-20 / Z 38 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P3:BRAZ_SE | brazier_recess | X 7..8 / Y -22..-20 / Z 38 | 2×3×1 | — | 6 | — | CONDITIONALLY APPROVED | P3 verifier | — |
| P4:FA | stair | X 3..9 / Y -29..-20 / Z 33..35 | 7×10×3 | 11×14×7 | 84 | HALL, LA | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:LA | landing | X -2..2 / Y -30..-26 / Z 32..36 | 5×5×5 | 9×9×9 | 125 | FA, CB1 | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CB1 | corr | X -2..2 / Y -30..-25 / Z 24..31 | 5×6×8 | 9×10×12 | 240 | LA, VAULT_ENT, CC | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:VAULT | vault | X -30..-6 / Y -30..-21 / Z 16..36 | 25×10×21 | 31×14×27 | 4800 | VAULT_ENT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:VAULT_ENT | corr | X -5..-3 / Y -30..-25 / Z 24..28 | 3×6×5 | 7×10×9 | 90 | CB1, VAULT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CC | corr | X -2..0 / Y -30..-26 / Z 0..23 | 3×5×24 | 7×9×28 | 360 | CB1, CC3 | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CC3 | corr | X -7..-3 / Y -30..-26 / Z 0..2 | 5×5×3 | 9×9×7 | 75 | CC, CRYPT | CONDITIONALLY APPROVED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented |
| P4:CRYPT | crypt | X -22..-8 / Y -30..-24 / Z -7..5 | 15×7×13 | 21×11×19 | 1365 | CRYPT_NICHE_N0, CRYPT_NICHE_S0, CRYPT_NICHE_N1, CRYPT_NICHE_S1, CRYPT_NICHE_N2, CRYPT_NICHE_S2, CC3 | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N0 | niche | X -11..-9 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S0 | niche | X -11..-9 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N1 | niche | X -15..-13 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S1 | niche | X -15..-13 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_N2 | niche | X -19..-17 / Y -30..-28 / Z -9..-8 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4:CRYPT_NICHE_S2 | niche | X -19..-17 / Y -30..-28 / Z 6..7 | 3×3×2 | — | 18 | CRYPT | PROPOSED | P4 verifier + P5 integration | worldgen: NBT/placement not implemented; crypt spec Q15 TBD |
| P4E:COL0:base | column_base | X -30..-28 / Y -30 / Z 32..34 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL0:shaft | column_shaft | X -30..-28 / Y -29..-26 / Z 32..34 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL0:cap | column_cap | X -30..-27 / Y -25..-24 / Z 31..35 | 4×2×5 | — | 38 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:base | column_base | X -30..-28 / Y -30 / Z 18..20 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:shaft | column_shaft | X -30..-28 / Y -29..-26 / Z 18..20 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL1:cap | column_cap | X -30..-27 / Y -25..-24 / Z 17..21 | 4×2×5 | — | 38 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:base | column_base | X -20..-18 / Y -30 / Z 32..34 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:shaft | column_shaft | X -20..-18 / Y -29..-26 / Z 32..34 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL2:cap | column_cap | X -21..-17 / Y -25..-24 / Z 31..35 | 5×2×5 | — | 46 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:base | column_base | X -20..-18 / Y -30 / Z 18..20 | 3×1×3 | — | 9 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:shaft | column_shaft | X -20..-18 / Y -29..-26 / Z 18..20 | 3×4×3 | — | 36 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:COL3:cap | column_cap | X -21..-17 / Y -25..-24 / Z 17..21 | 5×2×5 | — | 46 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:DAIS | dais | X -29..-19 / Y -30..-28 / Z 19..33 | 11×3×15 | — | 384 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:DAIS_STAIR | dais_stair | X -21..-17 / Y -30..-28 / Z 23..29 | 5×3×7 | — | 63 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:BEAMS | beam | X -29..-19 / Y -23 / Z 19..33 | 11×1×15 | — | 48 | — | PROPOSED | P4 verifier | — |
| P4E:CHEST_BASE | chest_base | X -28..-24 / Y -27 / Z 23..29 | 5×1×7 | — | 35 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:CHEST_BODY | chest_body | X -27..-25 / Y -26..-25 / Z 24..28 | 3×2×5 | — | 30 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:CHEST_LID | chest_lid | X -27..-25 / Y -24 / Z 24..28 | 3×1×5 | — | 15 | — | CONDITIONALLY APPROVED | P4 verifier | — |
| P4E:PILES_P | pile | X -30..-6 / Y -30..-27 / Z 29..36 | 25×4×8 | — | 141 | — | PROPOSED | P4 verifier | loot/decor: undecided (Q-doc: mineable or decoration UNDECIDED) |
| P4E:PILES_N | pile | X -30..-6 / Y -30..-27 / Z 16..23 | 25×4×8 | — | 139 | — | PROPOSED | P4 verifier | loot/decor: undecided (Q-doc: mineable or decoration UNDECIDED) |
| P4E:CRYPT:SARC | sarcophagus | X -16..-14 / Y -30..-29 / Z -3..1 | 3×2×5 | — | 30 | — | CONDITIONALLY APPROVED | P4 verifier | — |

## 3. 비밀 입구 12칸 절개 (CLOSED에서 닫히는 칸)

(-9,-23,33), (-9,-23,34), (-9,-23,35), (-8,-23,33), (-8,-23,34), (-8,-23,35), (-7,-23,33), (-7,-23,34), (-7,-23,35), (-6,-23,33), (-6,-23,34), (-6,-23,35)  (동쪽)  
(6,-23,33), (6,-23,34), (6,-23,35), (7,-23,33), (7,-23,34), (7,-23,35), (8,-23,33), (8,-23,34), (8,-23,35), (9,-23,33), (9,-23,34), (9,-23,35)  (서쪽)

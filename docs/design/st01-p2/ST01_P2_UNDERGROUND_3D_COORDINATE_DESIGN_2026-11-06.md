# ST-01 P2 — L1–L3 지하 3D 좌표 설계 및 양방향(동/서) 검증 (2026-11-06)

**상태: PROPOSED 설계 + 자동 검증 결과. 설계 권위가 아니며 구현(NBT·월드젠·보스·보상) 승인이 아닙니다.** D-3(회전 방향)은 **확정하지 않았고**, 동쪽·서쪽 후보를 독립 모델로 검증해 비교했습니다. 모든 수치는 Q 번호가 명시된 값을 제외하고 **PROPOSED**입니다. 기존 P1 모델·검증기·Q01–Q100 문서는 수정하지 않았습니다.

- 재현: `python3 -I tools/st01-p2/st01_p2_verify.py <out>` (동/서 독립 검증, 비교, 탐색, 도면) · `python3 -I tools/st01-p2/st01_p2_mutation_tests.py` (음성 대조 10건) · `python3 -I tools/st01-p2/build_p2_report.py <repo>` (이 문서와 `generated/` 재생성)
- 좌표는 P1 규약: X 동+, Z 남+(정문), Y 층 인덱스(Y=0 = 지상 최저층), 모든 범위 양끝 포함. 발 층 F, 바닥 슬래브 = F−1.

## 1. 기준 및 감사

| 항목 | 내용 |
| --- | --- |
| 기준 | `work/st01-p1-conditional-approval-20261106` @ `d2e00531…` (작업 트리 깨끗, origin 일치). 작업 브랜치 `work/st01-p2-underground-20261106` |
| 권위 | `ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md` (Q01–Q100; 설계 브랜치 대비 무변경 확인) |
| 입력 | P1 보고서, P1 조건부 승인 기록, P2 인계 문서. P1 SPACES는 P1 모델 파일을 *읽기만* 해(문법 파싱) 가져오며 수정하지 않음 |
| 병렬 작업 | origin의 ST-01 관련 브랜치(`design/st01-build…`, P1 두 브랜치)만 확인. P2 경로와 겹치는 파일 없음 |
| 사용자 결정 | **ST01-P2-D01(B)**: 필요 시 X/Z ±40 밖 공동 허용(필요성 입증 시에만, 최소량, 정량 보고, 확정 공간 축소 금지, 위험 별도 기록, 무제한·신규 방 자유 추가 아님). **D-3 미결정 유지** |

## 2. 승인 조건과 PROPOSED 구분

**승인(Q)·사용자 결정으로서 이 설계가 따르는 조건:** 81×81×40 외곽(불변), Q06–Q08 구성(L1 일반 묘실+짧은 분기, L2 다양한 보관실+분기, L3 전실→왕실), Q13/Q98 깊이 상한 완화(신규 상한 미정), Q17 주 통로 계단식 천장, Q21 전이 전실, Q25 L2 천장 높이가 방 크기별, Q26/Q27 대칭 왕실 접근과 개방 중앙 전실, Q36 부속실은 전실에서 분기(개수 미정), Q37 수평 엇갈림, Q38 넓은 참+1회 방향 전환 계단, Q39 완만한 굴절, **Q40 폭 7 → 5 → 3(문턱 길이 5–7)**, Q41 폭이 좁아지면 천장 낮아짐·전실/왕실 홀은 높음, **Q42 문턱 5–7**, **Q43 33×33 청정 내부, Q44 중앙 최대 높이 16, Q45 벽 8**, Q97 엇갈림 연장, D01-B.

**PROPOSED(이 문서가 도입한 값, 승인 전까지 기준선 아님):** 층 바닥 F = L1 −6(P1 E1) / 참 −10 / L2 −14 / 참 −18 / L3 −22(층간 8, 계단 2구간×4단), 통로 높이(7폭 5, 5폭 4, 3폭 3), 모든 방 위치·치수(전실 19×11 높이 10, 일반 묘실 7×7·7×9·7×7, 보관실 7×7·9×7(높이 8)·7×9, 분기 폭 3 길이 4), 점프형 굴절(L1: 8블록에 걸쳐 2블록), 최소 벽 2, 덮개(상단 ≤ −2, 지상 대전당 구역 아래 ≤ −3), 왕실 예약의 구조 두께(측면 3, 상·하 2), 비밀 후보 A/B. 대표 방 집합(L1 3개·L2 3개)은 **개수 승인이 아니라** prep V1의 치수를 인용한 대표 예시이며 개수는 Q06/Q07에서 미정입니다. L1 7폭 통로를 높이 5로 한 것은 P1 스텁(4)과 다르며, P1이 "천장은 P2 조정(Q41)"으로 위임한 범위입니다.

## 3. A. 좌표 설계표 (두 후보 모두)

열: ID · 공간 · 수준 · X · Y · Z · 크기 X×Y×Z(경계상자) · 바닥 F · 요구 폭 · 종류. 계단·굴절 통로는 여러 상자의 합집합이며 아래는 경계상자입니다(정확한 상자는 `st01_p2_model_east.py`).

### 3.1 동쪽 후보 (E1 → +X)

| ID | 공간 | 수준 | X | Y | Z | 크기 | F | 폭 | 종류 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| C1 | L1 main corridor (E1 continuation, 7 wide, gentle jog) | L1 | 8..30 | -6..-2 | -13..-5 | 23×5×9 | -6 | 7 | main |
| BR1 | L1 branch to tomb T1 (3 wide, 4 long) | L1 | 11..13 | -6..-2 | -17..-14 | 3×5×4 | -6 | - | optional |
| T1 | L1 ordinary tomb T1 7x7 | L1 | 9..15 | -6..-2 | -24..-18 | 7×5×7 | -6 | - | optional |
| BR2 | L1 branch to tomb T2 | L1 | 20..22 | -6..-2 | -17..-14 | 3×5×4 | -6 | - | optional |
| T2 | L1 ordinary tomb T2 7x9 | L1 | 18..24 | -6..-2 | -26..-18 | 7×5×9 | -6 | - | optional |
| BR3 | L1 branch to tomb T3 (under surface hall: clear 4) | L1 | 11..13 | -6..-3 | -6..-4 | 3×4×3 | -6 | - | optional |
| T3 | L1 ordinary tomb T3 7x7 (clear 4) | L1 | 9..15 | -6..-3 | -3..3 | 7×4×7 | -6 | - | optional |
| V12 | L1->L2 transition vestibule 9x11 | L1 | 31..39 | -6..-2 | -12..-2 | 9×5×11 | -6 | - | main |
| FA | Flight A (south, 4 treads, width 7) | L12 | 32..38 | -10..-3 | -1..2 | 7×8×4 | -10 | 7 | stair |
| LND12 | Landing L1/L2 (9x9, F=-10) | L12 | 31..39 | -10..-6 | 3..11 | 9×5×9 | -10 | - | main |
| FB | Flight B (west, 4 treads, width 7) - turn #1 | L12 | 27..30 | -14..-7 | 4..10 | 4×8×7 | -14 | 7 | stair |
| C2 | L2 central passage 7 wide | L2 | 14..26 | -14..-10 | 4..10 | 13×5×7 | -14 | 7 | main |
| BRB1 | L2 branch to B1 | L2 | 24..26 | -14..-10 | 11..14 | 3×5×4 | -14 | - | optional |
| B1 | L2 storage B1 7x7 flat | L2 | 22..28 | -14..-10 | 15..21 | 7×5×7 | -14 | - | optional |
| BRB2 | L2 branch to B2 | L2 | 16..18 | -14..-10 | 11..14 | 3×5×4 | -14 | - | optional |
| B2 | L2 storage B2 9x7 large, taller (clear 8) | L2 | 11..19 | -14..-7 | 15..21 | 9×8×7 | -14 | - | optional |
| BRB3 | L2 branch to B3 | L2 | 19..21 | -14..-10 | 0..3 | 3×5×4 | -14 | - | optional |
| B3 | L2 storage B3 7x9 flat | L2 | 17..23 | -14..-10 | -9..-1 | 7×5×9 | -14 | - | optional |
| V23 | L2->L3 transition vestibule 11x9 | L2 | 3..13 | -14..-10 | 3..11 | 11×5×9 | -14 | - | main |
| FC | Flight C (north, 4 treads, width 7) | L23 | 5..11 | -18..-11 | -1..2 | 7×8×4 | -18 | 7 | stair |
| LND23 | Landing L2/L3 (9x9, F=-18) | L23 | 4..12 | -18..-14 | -10..-2 | 9×5×9 | -18 | - | main |
| FD | Flight D (west, 4 treads, width 7) - turn #2 | L23 | 0..3 | -22..-15 | -9..-3 | 4×8×7 | -22 | 7 | stair |
| C3 | L3 main passage 7 wide | L3 | -3..-1 | -22..-18 | -9..-3 | 3×5×7 | -22 | 7 | main |
| R | L3 royal approach 5 wide (Q40/Q26) | L3 | -11..-4 | -22..-19 | -9..-5 | 8×4×5 | -22 | 5 | main |
| A | Ceremonial antechamber 19x11, clear 10 (Q27/Q41) | L3 | -30..-12 | -22..-13 | -12..-2 | 19×10×11 | -22 | - | main |
| T | Exit threshold 3 wide, 6 long incl. 3-block hall-wall port (Q40/Q42) | L3 | -22..-20 | -22..-20 | -1..4 | 3×3×6 | -22 | 3 | main |
| HALL_RES | Royal hall reservation (interior 33x33x16 + >=3 shell) | reserve | -40..-2 | -24..-5 | 2..40 | 39×20×39 | - | - | reservation |
| HALL_INT | Royal hall clear interior 33x33x16 (Q43/Q44) | reserve | -37..-5 | -22..-7 | 5..37 | 33×16×33 | - | - | reservation |
| VAULT_A | Secret vault candidate A (interior 21x25x10, below hall) | reserve | -40..-14 | -39..-26 | 9..39 | 27×14×31 | - | - | reservation |
| STAIR_A | Secret descent candidate A (below hall slab; hall opening TBD in P4) | reserve | -13..-3 | -39..-25 | 9..39 | 11×15×31 | - | - | reservation |

### 3.2 서쪽 후보 (P1 포함 정확한 X 거울; E1 → −X)

| ID | 공간 | 수준 | X | Y | Z | 크기 | F | 폭 | 종류 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| C1 | L1 main corridor (E1 continuation, 7 wide, gentle jog) | L1 | -30..-8 | -6..-2 | -13..-5 | 23×5×9 | -6 | 7 | main |
| BR1 | L1 branch to tomb T1 (3 wide, 4 long) | L1 | -13..-11 | -6..-2 | -17..-14 | 3×5×4 | -6 | - | optional |
| T1 | L1 ordinary tomb T1 7x7 | L1 | -15..-9 | -6..-2 | -24..-18 | 7×5×7 | -6 | - | optional |
| BR2 | L1 branch to tomb T2 | L1 | -22..-20 | -6..-2 | -17..-14 | 3×5×4 | -6 | - | optional |
| T2 | L1 ordinary tomb T2 7x9 | L1 | -24..-18 | -6..-2 | -26..-18 | 7×5×9 | -6 | - | optional |
| BR3 | L1 branch to tomb T3 (under surface hall: clear 4) | L1 | -13..-11 | -6..-3 | -6..-4 | 3×4×3 | -6 | - | optional |
| T3 | L1 ordinary tomb T3 7x7 (clear 4) | L1 | -15..-9 | -6..-3 | -3..3 | 7×4×7 | -6 | - | optional |
| V12 | L1->L2 transition vestibule 9x11 | L1 | -39..-31 | -6..-2 | -12..-2 | 9×5×11 | -6 | - | main |
| FA | Flight A (south, 4 treads, width 7) | L12 | -38..-32 | -10..-3 | -1..2 | 7×8×4 | -10 | 7 | stair |
| LND12 | Landing L1/L2 (9x9, F=-10) | L12 | -39..-31 | -10..-6 | 3..11 | 9×5×9 | -10 | - | main |
| FB | Flight B (west, 4 treads, width 7) - turn #1 | L12 | -30..-27 | -14..-7 | 4..10 | 4×8×7 | -14 | 7 | stair |
| C2 | L2 central passage 7 wide | L2 | -26..-14 | -14..-10 | 4..10 | 13×5×7 | -14 | 7 | main |
| BRB1 | L2 branch to B1 | L2 | -26..-24 | -14..-10 | 11..14 | 3×5×4 | -14 | - | optional |
| B1 | L2 storage B1 7x7 flat | L2 | -28..-22 | -14..-10 | 15..21 | 7×5×7 | -14 | - | optional |
| BRB2 | L2 branch to B2 | L2 | -18..-16 | -14..-10 | 11..14 | 3×5×4 | -14 | - | optional |
| B2 | L2 storage B2 9x7 large, taller (clear 8) | L2 | -19..-11 | -14..-7 | 15..21 | 9×8×7 | -14 | - | optional |
| BRB3 | L2 branch to B3 | L2 | -21..-19 | -14..-10 | 0..3 | 3×5×4 | -14 | - | optional |
| B3 | L2 storage B3 7x9 flat | L2 | -23..-17 | -14..-10 | -9..-1 | 7×5×9 | -14 | - | optional |
| V23 | L2->L3 transition vestibule 11x9 | L2 | -13..-3 | -14..-10 | 3..11 | 11×5×9 | -14 | - | main |
| FC | Flight C (north, 4 treads, width 7) | L23 | -11..-5 | -18..-11 | -1..2 | 7×8×4 | -18 | 7 | stair |
| LND23 | Landing L2/L3 (9x9, F=-18) | L23 | -12..-4 | -18..-14 | -10..-2 | 9×5×9 | -18 | - | main |
| FD | Flight D (west, 4 treads, width 7) - turn #2 | L23 | -3..0 | -22..-15 | -9..-3 | 4×8×7 | -22 | 7 | stair |
| C3 | L3 main passage 7 wide | L3 | 1..3 | -22..-18 | -9..-3 | 3×5×7 | -22 | 7 | main |
| R | L3 royal approach 5 wide (Q40/Q26) | L3 | 4..11 | -22..-19 | -9..-5 | 8×4×5 | -22 | 5 | main |
| A | Ceremonial antechamber 19x11, clear 10 (Q27/Q41) | L3 | 12..30 | -22..-13 | -12..-2 | 19×10×11 | -22 | - | main |
| T | Exit threshold 3 wide, 6 long incl. 3-block hall-wall port (Q40/Q42) | L3 | 20..22 | -22..-20 | -1..4 | 3×3×6 | -22 | 3 | main |
| HALL_RES | Royal hall reservation (interior 33x33x16 + >=3 shell) | reserve | 2..40 | -24..-5 | 2..40 | 39×20×39 | - | - | reservation |
| HALL_INT | Royal hall clear interior 33x33x16 (Q43/Q44) | reserve | 5..37 | -22..-7 | 5..37 | 33×16×33 | - | - | reservation |
| VAULT_A | Secret vault candidate A (interior 21x25x10, below hall) | reserve | 14..40 | -39..-26 | 9..39 | 27×14×31 | - | - | reservation |
| STAIR_A | Secret descent candidate A (below hall slab; hall opening TBD in P4) | reserve | 3..13 | -39..-25 | 9..39 | 11×15×31 | - | - | reservation |

### 3.3 계단·참 상세 (동쪽 기준, 서쪽은 X 부호 반전)
- 계단 A(L1→L2 전반): 남향 4단, 폭 7(X 32..38), 발 층 −7,−8,−9,−10 (Z −1,0,1,2), 단당 위 5층 자유. 참 L1/L2: X 31..39 × Z 3..11(9×9), F=−10, 높이 5.
- 계단 B(후반, **방향 전환 1회**): 서향 4단, 폭 7(Z 4..10), X 30,29,28,27, 발 층 −11…−14. L2 통로로 이어짐.
- 계단 C(L2→L3 전반): 북향 4단, 폭 7(X 5..11), Z 2,1,0,−1, 발 층 −15…−18. 참 L2/L3: X 4..12 × Z −10..−2(9×9), F=−18.
- 계단 D(후반, **방향 전환 1회**): 서향 4단, 폭 7(Z −9..−3), X 3,2,1,0, 발 층 −19…−22.
- 각 연결: 수직 낙차 8 = 4+4, 경사 1:1(45°), 수평 경로 4 + 참 통과 + 4. 참은 양변 ≥7로 주 통로(7폭)보다 넓음(S2).

## 4. 전체 공간 연결 그래프

```
[P1 S06 착지 (F=-6)] ══E1══ C1 L1 주통로(7폭, 굴절) ══ V12 전이전실 ══ 계단A↓ ══ 참L1/L2 ══ 계단B↓(서향 전환) ══ C2 L2 중앙통로(7폭)
   C1 ─ BR1 ─ T1,  C1 ─ BR2 ─ T2,  C1 ─ BR3 ─ T3                                  (L1 선택 분기: 막다른 방, 같은 통로로 복귀)
   C2 ─ BRB1 ─ B1,  C2 ─ BRB2 ─ B2(높은 방),  C2 ─ BRB3 ─ B3                       (L2 선택 분기)
C2 ══ V23 전이전실 ══ 계단C↓ ══ 참L2/L3 ══ 계단D↓(서향 전환) ══ C3 L3 통로(7폭) ══ R 왕실 접근(5폭) ══ A 의식 전실 ══ T 3폭 문턱(길이 6) ══▶ [왕실 묘실 예약 입구 포트]
후속(P4, 미설계): 왕실 묘실 → 비밀 하강 계단 → 비밀 묘실(크기 미정) / 비밀 보물창고(21×25×10)
```
주동선 상의 유일 경로이며 선택 분기는 모두 막다른 방입니다. 주동선 총 보행 거리(P1 착지 → 왕실 입구 포트): **117** 걸음.

## 5. 도면 (동쪽 후보; 전체 도면은 `generated/east/`, `generated/west/`)

기호: `=` 주 통로 `-` 선택 분기 `o` 선택 방 `v` 전이 전실 `s` 계단 `l` 참 `r` 왕실 접근(5폭) `A` 의식 전실 `t` 3폭 문턱 `P` P1 `h`/`H` 왕실 홀 외피/청정 내부 예약 `V`/`S` 비밀 후보 `!` ±40 경계(|X|=41,|Z|=41).

### 5.1 L1 (Y=−6)
```
PLAN  layer Y=-6  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '         +         '         '         '         '
 -26   !                                                          ooooooo                !
 -25   !                                                          ooooooo                !
 -24   !                                                 ooooooo  ooooooo                !
 -23   !                                                 ooooooo  ooooooo                !
 -22   !                                                 ooooooo  ooooooo                !
 -21   !                                                 ooooooo  ooooooo                !
 -20   !                                                 ooooooo  ooooooo                !
 -19   !                                                 ooooooo  ooooooo                !
 -18   !                                                 ooooooo  ooooooo                !
 -17   !                                                   ---      ---                  !
 -16   !                                                   ---      ---                  !
 -15   !                                                   ---      ---                  !
 -14   !                                 PPPPPPPPPPPPPPP   ---      ---                  !
 -13   !                                 PPPPPPPPPPPPPPP================                 !
 -12   !                                 PPPPPPPPPPPPPPP====================   vvvvvvvvv !
 -11   !                                 PPPPPPPPPPPPPPP=======================vvvvvvvvv !
 -10   !                                 PPPPPPPPPPPPPPP=======================vvvvvvvvv !
  -9   !                                 PPPPPPPPPPPPPPP=======================vvvvvvvvv !
  -8   !                                 PPPPPPPPPPPPPPP=======================vvvvvvvvv !
  -7   !                                 PPPPPPPPPPPPPPP=======================vvvvvvvvv !
  -6   !                                   PPPPPPPPPPP     ---         ========vvvvvvvvv !
  -5   !                                   PPPPPPPPPPP     ---             ====vvvvvvvvv !
  -4   !                                   PPPPPPPPPPP     ---                 vvvvvvvvv !
  -3   !                                   PPPPPPPPPPP   ooooooo               vvvvvvvvv !
  -2   !                                   PPPPPPPPPPP   ooooooo               vvvvvvvvv !
  -1   !                                   PPPPPPPPPPP   ooooooo                sssssss  !
  +0   !                                                 ooooooo                sssssss  !
  +1   !                                                 ooooooo                sssssss  !
  +2   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh          ooooooo                sssssss  !
  +3   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh          ooooooo               lllllllll !
  +4   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
  +5   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
  +6   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
  +7   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
  +8   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
  +9   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
 +10   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
 +11   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                lllllllll !
 +12   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +13   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +14   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +15   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +16   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +17   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +18   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +19   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +20   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +21   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +22   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +23   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +24   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +25   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +26   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +27   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +28   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +29   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +30   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +31   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +32   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +33   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +34   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +35   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +36   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +37   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +38   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +39   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +40   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
```
### 5.2 L1/L2 계단·참 (Y=−10)
```
PLAN  layer Y=-10  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '         +         '         '         '         '
  -9   !                                                         ooooooo                 !
  -8   !                                                         ooooooo                 !
  -7   !                                                         ooooooo                 !
  -6   !                                                         ooooooo                 !
  -5   !                                                         ooooooo                 !
  -4   !                                                         ooooooo                 !
  -3   !                                                         ooooooo                 !
  -2   !                                                         ooooooo                 !
  -1   !                                                         ooooooo                 !
  +0   !                                                           ---                   !
  +1   !                                                           ---                   !
  +2   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                    ---          sssssss  !
  +3   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    vvvvvvvvvvv     ---         lllllllll !
  +4   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    vvvvvvvvvvv=============sssslllllllll !
  +5   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
  +6   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
  +7   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
  +8   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
  +9   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
 +10   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============sssslllllllll !
 +11   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv  ---     ---    lllllllll !
 +12   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---              !
 +13   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---              !
 +14   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---              !
 +15   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +16   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +17   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +18   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +19   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +20   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +21   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo            !
 +22   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +23   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +24   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +25   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +26   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +27   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +28   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +29   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +30   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +31   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +32   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +33   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +34   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +35   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +36   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +37   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                          !
 +38   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +39   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
 +40   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                          !
```
### 5.3 L2 (Y=−14)
```
PLAN  layer Y=-14  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '         +         '         '         
 -12   !          AAAAAAAAAAAAAAAAAAA                                          
 -11   !          AAAAAAAAAAAAAAAAAAA                                          
 -10   !          AAAAAAAAAAAAAAAAAAA               lllllllll                  
  -9   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -8   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -7   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -6   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -5   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -4   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -3   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -2   !          AAAAAAAAAAAAAAAAAAA               lllllllll    ooooooo       
  -1   !                                             sssssss     ooooooo       
  +0   !                                             sssssss       ---         
  +1   !                                             sssssss       ---         
  +2   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh      sssssss       ---         
  +3   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    vvvvvvvvvvv     ---         
  +4   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    vvvvvvvvvvv=============s   
  +5   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
  +6   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
  +7   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
  +8   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
  +9   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
 +10   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv=============s   
 +11   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    vvvvvvvvvvv  ---     ---    
 +12   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---    
 +13   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---    
 +14   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                 ---     ---    
 +15   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +16   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +17   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +18   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +19   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +20   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +21   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh            ooooooooo  ooooooo  
 +22   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +23   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +24   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +25   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +26   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +27   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +28   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +29   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +30   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +31   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +32   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +33   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +34   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +35   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +36   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +37   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                                
 +38   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                
 +39   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                
 +40   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                                
```
### 5.4 L2/L3 계단·참 (Y=−18)
```
PLAN  layer Y=-18  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '         +         '   
 -12   !          AAAAAAAAAAAAAAAAAAA                          
 -11   !          AAAAAAAAAAAAAAAAAAA                          
 -10   !          AAAAAAAAAAAAAAAAAAA               lllllllll  
  -9   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -8   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -7   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -6   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -5   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -4   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -3   !          AAAAAAAAAAAAAAAAAAA        ===sssslllllllll  
  -2   !          AAAAAAAAAAAAAAAAAAA               lllllllll  
  -1   !                                             sssssss   
  +0   !                                                       
  +1   !                                                       
  +2   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
  +3   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
  +4   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
  +5   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
  +6   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
  +7   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
  +8   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
  +9   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +10   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +11   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +12   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +13   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +14   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +15   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +16   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +17   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +18   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +19   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +20   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +21   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +22   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +23   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +24   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +25   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +26   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +27   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +28   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +29   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +30   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +31   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +32   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +33   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +34   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +35   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +36   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +37   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh                
 +38   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
 +39   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
 +40   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh                
```
### 5.5 L3 + 왕실 묘실 예약 (Y=−22)
```
PLAN  layer Y=-22  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '         + 
 -12   !          AAAAAAAAAAAAAAAAAAA              
 -11   !          AAAAAAAAAAAAAAAAAAA              
 -10   !          AAAAAAAAAAAAAAAAAAA              
  -9   !          AAAAAAAAAAAAAAAAAAArrrrrrrr===s  
  -8   !          AAAAAAAAAAAAAAAAAAArrrrrrrr===s  
  -7   !          AAAAAAAAAAAAAAAAAAArrrrrrrr===s  
  -6   !          AAAAAAAAAAAAAAAAAAArrrrrrrr===s  
  -5   !          AAAAAAAAAAAAAAAAAAArrrrrrrr===s  
  -4   !          AAAAAAAAAAAAAAAAAAA        ===s  
  -3   !          AAAAAAAAAAAAAAAAAAA        ===s  
  -2   !          AAAAAAAAAAAAAAAAAAA              
  -1   !                  ttt                      
  +0   !                  ttt                      
  +1   !                  ttt                      
  +2   !hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh    
  +3   !hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh    
  +4   !hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh    
  +5   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
  +6   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
  +7   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
  +8   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
  +9   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +10   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +11   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +12   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +13   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +14   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +15   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +16   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +17   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +18   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +19   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +20   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +21   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +22   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +23   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +24   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +25   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +26   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +27   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +28   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +29   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +30   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +31   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +32   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +33   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +34   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +35   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +36   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +37   !hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh    
 +38   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    
 +39   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    
 +40   !hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh    
```
### 5.6 비밀 보물창고 후보 A 층 (Y=−33; 왕실 홀 바로 아래, 예약만)
```
PLAN  layer Y=-33  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    '         '         '         '        
  +9   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +10   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +11   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +12   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +13   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +14   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +15   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +16   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +17   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +18   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +19   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +20   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +21   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +22   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +23   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +24   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +25   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +26   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +27   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +28   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +29   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +30   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +31   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +32   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +33   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +34   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +35   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +36   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +37   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +38   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
 +39   !VVVVVVVVVVVVVVVVVVVVVVVVVVVSSSSSSSSSSS  
```
### 5.7 수직 단면 X=9 (남북)
```
SECTION X=9  (left=north(-Z) right=south(+Z))  Y[-42,0]
  +0  #####!##################################PPPPPPPPPPPPPPPPPPPPPPPPP##PPPP####PPPPPPPPPPPP!#####
  -1  #####!#################################################################################!#####
  -2  #####!################ooooooo####=======###############################################!#####
  -3  #####!################ooooooo####=======###ooooooo#####################################!#####
  -4  #####!################ooooooo####=======###ooooooo#####################################!#####
  -5  #####!################ooooooo####=======###ooooooo#####################################!#####
  -6  #####!################ooooooo####=======###ooooooo#####################################!#####
  -7  #####!#################################################################################!#####
  -8  #####!#################################################################################!#####
  -9  #####!#################################################################################!#####
 -10  #####!###########################################vvvvvvvvv#############################!#####
 -11  #####!##########################################svvvvvvvvv#############################!#####
 -12  #####!#########################################ssvvvvvvvvv#############################!#####
 -13  #####!########################################sssvvvvvvvvv#############################!#####
 -14  #####!##############################lllllllllssssvvvvvvvvv#############################!#####
 -15  #####!##############################lllllllllssss######################################!#####
 -16  #####!##############################lllllllllsss#######################################!#####
 -17  #####!##############################lllllllllss########################################!#####
 -18  #####!##############################llllllllls#########################################!#####
 -19  #####!#################################################################################!#####
 -20  #####!#################################################################################!#####
 -21  #####!#################################################################################!#####
 -22  #####!#################################################################################!#####
 -23  #####!#################################################################################!#####
 -24  #####!#################################################################################!#####
 -25  #####!#################################################################################!#####
 -26  #####!#################################################################################!#####
 -27  #####!#################################################################################!#####
 -28  #####!#################################################################################!#####
 -29  #####!#################################################################################!#####
 -30  #####!#################################################################################!#####
 -31  #####!#################################################################################!#####
 -32  #####!#################################################################################!#####
 -33  #####!#################################################################################!#####
 -34  #####!#################################################################################!#####
 -35  #####!#################################################################################!#####
 -36  #####!#################################################################################!#####
 -37  #####!#################################################################################!#####
 -38  #####!#################################################################################!#####
 -39  #####!#################################################################################!#####
 -40  #####!#################################################################################!#####
 -41  #####!#################################################################################!#####
 -42  #####!#################################################################################!#####
```
### 5.8 수직 단면 Z=−7 (동서; P1 착지–L3 통로–전실)
```
SECTION Z=-7  (left=west(-X) right=east(+X))  Y[-42,0]
  +0  #####!#################################################################################!#####
  -1  #####!#################################PPPPPPPPPPPPPPP#################################!#####
  -2  #####!#################################PPPPPPPPPPPPPPP=======================vvvvvvvvv#!#####
  -3  #####!#################################PPPPPPPPPPPPPPP=======================vvvvvvvvv#!#####
  -4  #####!#################################PPPPPPPPPPPPPPP=======================vvvvvvvvv#!#####
  -5  #####!#################################PPPPPPPPPPPPPPP=======================vvvvvvvvv#!#####
  -6  #####!#################################PPPPPPPPPPPPPPP=======================vvvvvvvvv#!#####
  -7  #####!#################################################################################!#####
  -8  #####!#################################################################################!#####
  -9  #####!#################################################################################!#####
 -10  #####!#########################################################ooooooo#################!#####
 -11  #####!#########################################################ooooooo#################!#####
 -12  #####!#########################################################ooooooo#################!#####
 -13  #####!##########AAAAAAAAAAAAAAAAAAA############################ooooooo#################!#####
 -14  #####!##########AAAAAAAAAAAAAAAAAAA###############lllllllll####ooooooo#################!#####
 -15  #####!##########AAAAAAAAAAAAAAAAAAA##############slllllllll############################!#####
 -16  #####!##########AAAAAAAAAAAAAAAAAAA#############sslllllllll############################!#####
 -17  #####!##########AAAAAAAAAAAAAAAAAAA############ssslllllllll############################!#####
 -18  #####!##########AAAAAAAAAAAAAAAAAAA########===sssslllllllll############################!#####
 -19  #####!##########AAAAAAAAAAAAAAAAAAArrrrrrrr===ssss#####################################!#####
 -20  #####!##########AAAAAAAAAAAAAAAAAAArrrrrrrr===sss######################################!#####
 -21  #####!##########AAAAAAAAAAAAAAAAAAArrrrrrrr===ss#######################################!#####
 -22  #####!##########AAAAAAAAAAAAAAAAAAArrrrrrrr===s########################################!#####
 -23  #####!#################################################################################!#####
 -24  #####!#################################################################################!#####
 -25  #####!#################################################################################!#####
 -26  #####!#################################################################################!#####
 -27  #####!#################################################################################!#####
 -28  #####!#################################################################################!#####
 -29  #####!#################################################################################!#####
 -30  #####!#################################################################################!#####
 -31  #####!#################################################################################!#####
 -32  #####!#################################################################################!#####
 -33  #####!#################################################################################!#####
 -34  #####!#################################################################################!#####
 -35  #####!#################################################################################!#####
 -36  #####!#################################################################################!#####
 -37  #####!#################################################################################!#####
 -38  #####!#################################################################################!#####
 -39  #####!#################################################################################!#####
 -40  #####!#################################################################################!#####
 -41  #####!#################################################################################!#####
 -42  #####!#################################################################################!#####
```
### 5.9 수직 단면 Z=7 (동서; L2 통로·계단 B·V23)
```
SECTION Z=7  (left=west(-X) right=east(+X))  Y[-42,0]
  +0  #####!######################PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP######################!#####
  -1  #####!#################################################################################!#####
  -2  #####!#################################################################################!#####
  -3  #####!#################################################################################!#####
  -4  #####!#################################################################################!#####
  -5  #####!hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh##########################################!#####
  -6  #####!hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh################################lllllllll#!#####
  -7  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh###############################slllllllll#!#####
  -8  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##############################sslllllllll#!#####
  -9  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh#############################ssslllllllll#!#####
 -10  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh####vvvvvvvvvvv=============sssslllllllll#!#####
 -11  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh####vvvvvvvvvvv=============ssss##########!#####
 -12  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh####vvvvvvvvvvv=============sss###########!#####
 -13  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh####vvvvvvvvvvv=============ss############!#####
 -14  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh####vvvvvvvvvvv=============s#############!#####
 -15  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -16  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -17  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -18  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -19  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -20  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -21  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -22  #####!hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh##########################################!#####
 -23  #####!hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh##########################################!#####
 -24  #####!hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh##########################################!#####
 -25  #####!#################################################################################!#####
 -26  #####!#################################################################################!#####
 -27  #####!#################################################################################!#####
 -28  #####!#################################################################################!#####
 -29  #####!#################################################################################!#####
 -30  #####!#################################################################################!#####
 -31  #####!#################################################################################!#####
 -32  #####!#################################################################################!#####
 -33  #####!#################################################################################!#####
 -34  #####!#################################################################################!#####
 -35  #####!#################################################################################!#####
 -36  #####!#################################################################################!#####
 -37  #####!#################################################################################!#####
 -38  #####!#################################################################################!#####
 -39  #####!#################################################################################!#####
 -40  #####!#################################################################################!#####
 -41  #####!#################################################################################!#####
 -42  #####!#################################################################################!#####
```
### 5.10 서쪽 후보 (거울) — L1, L3
```
PLAN  layer Y=-6  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X   '         '         '         '         +         '         '         '         ' 
 -26  !                ooooooo                                                          ! 
 -25  !                ooooooo                                                          ! 
 -24  !                ooooooo  ooooooo                                                 ! 
 -23  !                ooooooo  ooooooo                                                 ! 
 -22  !                ooooooo  ooooooo                                                 ! 
 -21  !                ooooooo  ooooooo                                                 ! 
 -20  !                ooooooo  ooooooo                                                 ! 
 -19  !                ooooooo  ooooooo                                                 ! 
 -18  !                ooooooo  ooooooo                                                 ! 
 -17  !                  ---      ---                                                   ! 
 -16  !                  ---      ---                                                   ! 
 -15  !                  ---      ---                                                   ! 
 -14  !                  ---      ---   PPPPPPPPPPPPPPP                                 ! 
 -13  !                 ================PPPPPPPPPPPPPPP                                 ! 
 -12  ! vvvvvvvvv   ====================PPPPPPPPPPPPPPP                                 ! 
 -11  ! vvvvvvvvv=======================PPPPPPPPPPPPPPP                                 ! 
 -10  ! vvvvvvvvv=======================PPPPPPPPPPPPPPP                                 ! 
  -9  ! vvvvvvvvv=======================PPPPPPPPPPPPPPP                                 ! 
  -8  ! vvvvvvvvv=======================PPPPPPPPPPPPPPP                                 ! 
  -7  ! vvvvvvvvv=======================PPPPPPPPPPPPPPP                                 ! 
  -6  ! vvvvvvvvv========         ---     PPPPPPPPPPP                                   ! 
  -5  ! vvvvvvvvv====             ---     PPPPPPPPPPP                                   ! 
  -4  ! vvvvvvvvv                 ---     PPPPPPPPPPP                                   ! 
  -3  ! vvvvvvvvv               ooooooo   PPPPPPPPPPP                                   ! 
  -2  ! vvvvvvvvv               ooooooo   PPPPPPPPPPP                                   ! 
  -1  !  sssssss                ooooooo   PPPPPPPPPPP                                   ! 
  +0  !  sssssss                ooooooo                                                 ! 
  +1  !  sssssss                ooooooo                                                 ! 
  +2  !  sssssss                ooooooo          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +3  ! lllllllll               ooooooo          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +4  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +5  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +6  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +7  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +8  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
  +9  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +10  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +11  ! lllllllll                                hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +12  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +13  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +14  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +15  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +16  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +17  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +18  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +19  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +20  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +21  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +22  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +23  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +24  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +25  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +26  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +27  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +28  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +29  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +30  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +31  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +32  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +33  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +34  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +35  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +36  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +37  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +38  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +39  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +40  !                                          hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
```
```
PLAN  layer Y=-22  X[-46,46] Z[-46,46]  north(-Z) up, east(+X) right.  '!' marks |X|=41 / |Z|=41 (pyramid projection +-40 boundary)
  Z\X    +         '         '         '         ' 
 -12                AAAAAAAAAAAAAAAAAAA          ! 
 -11                AAAAAAAAAAAAAAAAAAA          ! 
 -10                AAAAAAAAAAAAAAAAAAA          ! 
  -9    s===rrrrrrrrAAAAAAAAAAAAAAAAAAA          ! 
  -8    s===rrrrrrrrAAAAAAAAAAAAAAAAAAA          ! 
  -7    s===rrrrrrrrAAAAAAAAAAAAAAAAAAA          ! 
  -6    s===rrrrrrrrAAAAAAAAAAAAAAAAAAA          ! 
  -5    s===rrrrrrrrAAAAAAAAAAAAAAAAAAA          ! 
  -4    s===        AAAAAAAAAAAAAAAAAAA          ! 
  -3    s===        AAAAAAAAAAAAAAAAAAA          ! 
  -2                AAAAAAAAAAAAAAAAAAA          ! 
  -1                        ttt                  ! 
  +0                        ttt                  ! 
  +1                        ttt                  ! 
  +2      hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh! 
  +3      hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh! 
  +4      hhhhhhhhhhhhhhhhhhttthhhhhhhhhhhhhhhhhh! 
  +5      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
  +6      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
  +7      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
  +8      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
  +9      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +10      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +11      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +12      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +13      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +14      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +15      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +16      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +17      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +18      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +19      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +20      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +21      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +22      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +23      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +24      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +25      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +26      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +27      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +28      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +29      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +30      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +31      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +32      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +33      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +34      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +35      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +36      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +37      hhhHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHhhh! 
 +38      hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +39      hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
 +40      hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh! 
```

## 6. 충돌·성능 검증 결과

**방법.** ① 129×101×129 복셀 모델 (P1 공동 포함)에서 공동 카빙·소유권·인접 접촉·BFS 보행 ② 같은 항목을 복셀 없이 AABB로 독립 계산(I1–I3: 경계, 쌍별 교차 부피, 통로 폭 합집합) ③ **음성 대조 10건**(홀을 동쪽으로 10 이동·±40 밖으로 이동·접근로 3폭 축소·문턱 9·분기 분리·방 겹침·덮개 위반·홀 31폭·C1 분리·계단 축 동일) 모두 검출됨. ④ 서쪽 모델은 동쪽에서 *기계적 X 거울*(P1 컨텍스트 포함)로 만든 뒤 **별도로** 같은 검증기를 실행하고, 복셀 solid/owner/reserved 거울 일치와 판정 일치를 확인했습니다.

| ID | 검사 | 동쪽 | 서쪽 |
| --- | --- | --- | --- |
| X1 | Cavities and reserved volumes stay within the pyramid projection X/Z +-40 (D01-B: extension only if needed) | PASS | PASS |
| C1 | No voxel shared by two different cavities (3D, Y included), P1 context included | PASS | PASS |
| C2 | Reserved volumes (royal hall, secret candidates) free of cavities except declared entrance ports | PASS | PASS |
| C3 | Every face contact between cavities is a declared connection and every declared connection has real face contact | PASS | PASS |
| C4 | Face-to-face wall/slab between unconnected cavities >= 2 blocks (all axes, P1 context included) | PASS | PASS |
| C5 | Cover above cavities: top <= -2 (<= -3 where the plan overlaps the surface hall zone X-18..18 Z-6..18) | PASS | PASS |
| R1 | Every program space (incl. optional rooms and the royal-hall entrance port) reachable on foot from the P1 landing | PASS | PASS |
| R2 | Royal hall port reachable with ALL optional rooms/branches blocked (optional exploration not required) | PASS | PASS |
| R3 | Return path from every optional room to the main route / start exists | PASS | PASS |
| W1 | Actual clear passage width (contiguous walkable cells across the corridor, per cross-section) and walked headroom meet Q40/Q41 targets | PASS | PASS |
| S1 | Stairs: 1:1 gradient (every tread rises exactly 1), each connection is two flights with different axes (one turn) and the planned total rise | PASS | PASS |
| S2 | Landings are broad (both plan dimensions >= 7, wider than the 7-wide main passage) | PASS | PASS |
| Q1 | Q40-Q42: 7-wide passage -> 5-wide royal approach -> 3-wide threshold of 5..7 blocks; ceiling lowers with narrowing; antechamber tall | PASS | PASS |
| H1 | Royal hall reservation contains the Q43/Q44 clear interior 33x33 and 16 high, with >=3 lateral and >=2 vertical structural thickness (PROPOSED thickness) | PASS | PASS |
| Q2 | Q37: L1/L2/L3 are horizontally staggered (distinct plan centres, monotone drift) - exact centres are PROPOSED | PASS | PASS |
| I1 | Independent AABB geometry agrees with the voxel model (cavity extents, lowest Y) | PASS | PASS |
| I2 | Independent pairwise AABB intersection volume between different cavities is zero (no collision, Y included) | PASS | PASS |
| I3 | Independent analytic corridor widths (union of box extents per cross-section) meet Q40 | PASS | PASS |

동쪽 18/18 PASS, 서쪽 18/18 PASS, FAIL 0. 항목별 증거(동쪽):

- **X1 PASS** — max excess 0 blocks; cavity voxels outside 0; reserved voxels outside 0; analytic outside-volume 0 (must equal voxel count 0); cavity X[-30,39] Z[-26,21], reserved X[-40,-2] Z[2,40]
- **C1 PASS** — overlaps: []
- **C2 PASS** — violations: []; declared ports: [('T', 'HALL_RES', [-22, -20, -22, -20, 2, 4])]
- **C3 PASS** — undeclared contacts []; declared-without-contact []; 26 declared links
- **C4 PASS** — thin: []
- **C5 PASS** — violations []
- **R1 PASS** — 16 goals; unreachable []
- **R2 PASS** — main goals missing when optional blocked: []
- **R3 PASS** — rooms without return: []
- **W1 PASS** — C1: w 7>=7, h 5>=5; FA: w 7>=7, h 5>=5; FB: w 7>=7, h 5>=5; C2: w 7>=7, h 5>=5; FC: w 7>=7, h 5>=5; FD: w 7>=7, h 5>=5; C3: w 7>=7, h 5>=5; R: w 5>=5, h 4>=4; T: w 3>=3, h 3>=3; violations []
- **S1 PASS** — flights [('FA', 4, 4, True, 'z'), ('FB', 4, 4, True, 'x'), ('FC', 4, 4, True, 'z'), ('FD', 4, 4, True, 'x')]; connections {'L1-L2': ('FA', 'FB', 8), 'L2-L3': ('FC', 'FD', 8)}; bad [] []
- **S2 PASS** — landings (name, X, Z): [('L1/L2', 9, 9), ('L2/L3', 9, 9)]
- **Q1 PASS** — widths [('C3', 7), ('R', 5), ('T', 3)]; walked headroom along approach [5, 4, 3] (non-increasing); threshold length 6 (cavity + hall-wall port); antechamber clear height 10
- **H1 PASS** — interior 33x33x16 at X[-37,-5] Z[5,37] Y[-22,-7]; reservation X[-40,-2] Z[2,40] Y[-24,-5]; shell thickness W/E/N/S/floor/roof = (3, 3, 3, 3, 2, 2)
- **Q2 PASS** — plan bounding-corner centroids {'L1': (20.333333333333332, -10.916666666666666), 'L2': (18.75, 8.9375), 'L3': (-12.875, -4.625)}
- **I1 PASS** — analytic {'x': [-30, 39], 'z': [-26, 21], 'ymin': -22} vs voxel X[-30, 39] Z[-26, 21] Ymin -22
- **I2 PASS** — intersections: []
- **I3 PASS** — analytic min widths {'C1': 7, 'C2': 7, 'C3': 7, 'R': 5, 'T': 3}; violations []

서쪽 증거(좌표가 거울): 
- **X1 PASS** — max excess 0 blocks; cavity voxels outside 0; reserved voxels outside 0; analytic outside-volume 0 (must equal voxel count 0); cavity X[-39,30] Z[-26,21], reserved X[2,40] Z[2,40]
- **C1 PASS** — overlaps: []
- **C2 PASS** — violations: []; declared ports: [('T', 'HALL_RES', [20, 22, -22, -20, 2, 4])]
- **C3 PASS** — undeclared contacts []; declared-without-contact []; 26 declared links
- **C4 PASS** — thin: []
- **C5 PASS** — violations []
- **R1 PASS** — 16 goals; unreachable []
- **R2 PASS** — main goals missing when optional blocked: []
- **R3 PASS** — rooms without return: []
- **W1 PASS** — C1: w 7>=7, h 5>=5; FA: w 7>=7, h 5>=5; FB: w 7>=7, h 5>=5; C2: w 7>=7, h 5>=5; FC: w 7>=7, h 5>=5; FD: w 7>=7, h 5>=5; C3: w 7>=7, h 5>=5; R: w 5>=5, h 4>=4; T: w 3>=3, h 3>=3; violations []
- **S1 PASS** — flights [('FA', 4, 4, True, 'z'), ('FB', 4, 4, True, 'x'), ('FC', 4, 4, True, 'z'), ('FD', 4, 4, True, 'x')]; connections {'L1-L2': ('FA', 'FB', 8), 'L2-L3': ('FC', 'FD', 8)}; bad [] []
- **S2 PASS** — landings (name, X, Z): [('L1/L2', 9, 9), ('L2/L3', 9, 9)]
- **Q1 PASS** — widths [('C3', 7), ('R', 5), ('T', 3)]; walked headroom along approach [5, 4, 3] (non-increasing); threshold length 6 (cavity + hall-wall port); antechamber clear height 10
- **H1 PASS** — interior 33x33x16 at X[5,37] Z[5,37] Y[-22,-7]; reservation X[2,40] Z[2,40] Y[-24,-5]; shell thickness W/E/N/S/floor/roof = (3, 3, 3, 3, 2, 2)
- **Q2 PASS** — plan bounding-corner centroids {'L1': (-20.333333333333332, -10.916666666666666), 'L2': (-18.75, 8.9375), 'L3': (12.875, -4.625)}
- **I1 PASS** — analytic {'x': [-39, 30], 'z': [-26, 21], 'ymin': -22} vs voxel X[-39, 30] Z[-26, 21] Ymin -22
- **I2 PASS** — intersections: []
- **I3 PASS** — analytic min widths {'C1': 7, 'C2': 7, 'C3': 7, 'R': 5, 'T': 3}; violations []

설계 과정에서 검증기가 잡아낸 **실제 결함**(모두 수정, 재실행 후 PASS): 분기 BRB2가 V23에 인접 접촉, 분기 BRB3가 C2에서 1칸 떨어져 B3 도달 불가. 검증기 **자체 오류**로 판명되어 수정한 것: 계단 낙차 계산식(S1), 엇갈림 단조성이 +X에만 맞춰져 서쪽에서 FAIL(Q2). 왕실 예약 두께 기준은 임의로 6면 3으로 두었다가 Q 근거가 없어 측면 3·상하 2로 명시 완화(H1).

타이트한 수직 간격(서로 다른 공동의 평면이 겹치는 쌍):

| 사이 고체 층 수 | 공동 A | 공동 B |
| --- | --- | --- |
| 2 | BR3 | P1:S04 |
| 2 | T3 | P1:S04 |
| 3 | C1 | B3 |
| 3 | C1 | B3 |
| 3 | T3 | V23 |
| 4 | T3 | FC |

실제 통로 폭/헤드룸 측정(복셀 단면의 연속 보행 셀 최소값):

| 공간 | 요구 폭 | 측정 폭 | 요구 높이 | 측정 높이 |
| --- | --- | --- | --- | --- |
| C1 | 7 | 7 | 5 | 5 |
| FA | 7 | 7 | 5 | 5 |
| FB | 7 | 7 | 5 | 5 |
| C2 | 7 | 7 | 5 | 5 |
| FC | 7 | 7 | 5 | 5 |
| FD | 7 | 7 | 5 | 5 |
| C3 | 7 | 7 | 5 | 5 |
| R | 5 | 5 | 4 | 4 |
| T | 3 | 3 | 3 | 3 |

**Q37 엇갈림**: 수준별 평면 중심 L1 (20.3, -10.9), L2 (18.8, 8.9), L3 (-12.9, -4.6) (경계 모서리 평균) → X 방향으로 단조 이동. 정확한 중심은 Q37에서 TBD이므로 PROPOSED.

## 7. 지하 확장량 (D01-B)

**절차.** ① ±40 이내 배치를 먼저 탐색 → ② 승인 조건 충족 확인 → ③ 불가능 시에만 확장. 결과: **확장 0**(공동·예약 모두 ±40 이내)으로 모든 승인 조건을 만족하는 배치가 존재함. 따라서 확장 단계(e>0)를 시도할 필요가 없었고 "확장이 필요한 이유"는 없음.

| 항목 | 동쪽 | 서쪽 |
| --- | --- | --- |
| 지하 공동 X 최소..최대 | -30..39 | -39..30 |
| 지하 공동 Z 최소..최대 | -26..21 | -26..21 |
| 예약 체적 X / Z | -40..-2 / 2..40 | 2..40 / 2..40 |
| ±40 초과 최대 거리(공동·예약) | 0 | 0 |
| ±40 밖 공동 복셀 수 (복셀 / 해석) | 0 / 0 | 0 / 0 |
| ±40 밖 예약 복셀 수 | 0 | 0 |
| 전체 최저 공동 Y (L1–L3) | -22 | -22 |
| 예약 포함 최저 Y (이 모델, 비밀 후보 A 포함) | -39 | -39 |
| P2 공동 복셀 총수 | 8195 | 8195 |
| P1 착지→왕실 홀 입구 보행 거리(4방향 걸음) | 117 | 117 |
| 검증 PASS / 전체 | 18 / 18 | 18 / 18 |
| 복셀 거울 동치(solid/owner/reserved) | True/True/True | (동일 비교) |

- 이는 **탐색된 후보 중 최소값(0)** 입니다. 가능한 모든 배치를 탐색한 것은 아니며, 0 미만은 정의상 없으므로 최소성 자체는 자명하지만 *다른 비교 지표*(깊이, 동선 길이)에서의 최적성은 주장하지 않습니다.
- 왕실 홀 예약(39×39)은 탐색한 4개 코너 배치 중 하나만 추가 깊이 없이 성립하며 그 배치는 **경계 여유 0**(X=−40, Z=40 접함, 서쪽 후보는 X=+40). 승인된 33×33×16을 줄이지 않았습니다.

**왕실 홀 코너 배치 탐색**(설계 깊이 F=−22에서 충돌 여부, 충돌 해소에 필요한 추가 깊이; L3 동선 C3/R/A/T는 대안마다 재해결하지 않음 → 한계):

| 코너 | 위치 | 설계 깊이의 충돌 공간 | 추가 깊이 | 해소 시 최저 Y |
| --- | --- | --- | --- | --- |
| SW | X -40..-2, Z 2..40 | 없음 | 0 | -24 |
| SE | X 2..40, Z 2..40 | B1, B2, BRB1, BRB2, BRB3, C2, FA, FB, FC, LND12, T3, V23 | 15 | -39 |
| NW | X -40..-2, Z -40..-2 | P1:S05, P1:S06 | 4 | -28 |
| NE | X 2..40, Z -40..-2 | B3, BR1, BR2, BR3, BRB3, C1, FA, FC, LND23, P1:S05, P1:S06, T1, T2, T3, V12 | 16 | -40 |

SW(동쪽 후보의 선택)만 추가 깊이 없이 성립합니다. 다른 코너는 같은 ±40 안에서도 충돌 해소를 위해 깊이를 더 써야 합니다(NW +4, SE +15 등).

**비밀 보물창고(27×31 외벽 포함 × 14 높이) 후보 탐색**(벽 2칸 여유, 모든 공동·예약 회피, 확장 e를 0부터 증가):

| 수준 | 필요 확장 | 가능 배치 수 | 첫 배치 | Y | 홀에 가장 가까운 배치 |
| --- | --- | --- | --- | --- | --- |
| L3-level (lateral, vault interior at F=-22) | e=0 | 40 | X -9..21, Z -40..-14 | Y -24..-11 | 홀과의 평면 간격 14: X -9..21, Z -39..-13 |
| stacked below royal hall (vault interior F=-37) | e=0 | 2248 | X -40..-14, Z -40..-10 | Y -39..-26 | 홀과의 평면 간격 2: X -40..-14, Z -31..-1 |

→ 비밀 보물창고도 **확장 없이** 두 수준에서 배치 가능: (A) 왕실 홀 바로 아래 적층(깊이↑, 홀과 평면 일치), (B) L3 수준 측방(깊이 +0, 그러나 홀에서 평면상 14블록 이상 떨어진 북중앙부에만 가능 → 왕실 홀 이후 연결 통로가 길어짐). 후보 A의 상세: 외벽 포함 11폭 계단 띠가 홀 평면 안에 있고(X −13..−3, 길이 31), 홀 바닥에서 보물창고 바닥까지 낙하 15; 보물창고와 홀 바닥 사이 고체 3층, 계단 후보와 겹침 없음(후보 A 자체 점검 OK).

**지형·월드젠·청크·타 구조물 위험(D01 항목 5; 평가 불가, UNVERIFIED):** 구조 전체(지상 81×81)는 청크 정렬과 무관하게 항상 한 축당 6개 청크에 걸침(81 = 5×16+1). 공동은 X −30..39 / Z −26..21(동쪽 후보)이라 지상 외곽 안쪽이지만, 지하 벽체는 최소 2칸을 가정하면 **동쪽 후보의 V12·계단 A·참 L1/L2 동벽(공동 X=39)이 X=41에서 투영 1블록 밖으로 나감**(서쪽은 X=−41). D01은 공동 기준이므로 판정은 PASS로 했으나, 벽체 기준으로 보면 1블록 초과이므로 해석 확인을 **소유자 결정 항목**으로 올림. 실제 지형(동굴·수중·광맥)·인접 구조물·모래 중력 블록은 월드 배치가 정해져야 평가 가능.

## 8. 최대 지하 깊이 (Q98)

깊이는 층 인덱스의 절대값(Y=−N → N블록 아래)으로 표기.

| 요소 | Y | 근거 |
| --- | --- | --- |
| L1 floor (E1, from P1) | -6 | P1 interface (sourced) |
| L1->L2 total rise | 8 | 2 flights x 4 risers, 1:1 (PROPOSED) |
| L2 floor | -14 | derived |
| L2->L3 total rise | 8 | 2 flights x 4 risers, 1:1 (PROPOSED) |
| L3 floor / royal-hall floor | -22 | derived |
| Floor slab beneath L3 (2) -> lowest placed block, L1-L3 + hall | -24 | PROPOSED slab 2 |
| Hall reservation bottom | -24 | interior floor -22, slab 2 |
| Secret-vault candidate A bottom (stacked under hall) | -39 | UNVERIFIED stage-P4 candidate |

- **P2 잠정 깊이: 24** (L3 바닥 −22 + 슬래브 2; 왕실 홀 예약 바닥도 −24). 기존 20블록 상한은 Q13으로 이미 완화.
- **후속 단계 필요 깊이: 비밀 보물창고 후보 A 적층이면 39, 후보 B 측방이면 24 유지.** 비밀 묘실(Q15)은 크기 미정이라 깊이를 계산하지 않음 → **최대 깊이는 P2만으로 확정할 수 없음**(UNVERIFIED). 새 상한을 설정하지 않았습니다.
- 층간 8(4+4 단)은 현 평면 겹침 쌍(C1/B3, T3/V23: 사이 고체 3)에서 최소 벽 2 기준으로는 1씩 줄일 여지가 있어 보이나, 이 경우 계단 구성(4+3)과 모든 L2/L3 좌표가 바뀌므로 **미탐색**. 깊이 최소화 최적성은 주장하지 않음.

## 9. 동쪽 vs 서쪽 비교 및 D-3

두 후보는 정확한 거울이며 복셀 단위로 일치(`solid/owner/reserved` 모두 True, 판정 일치). 따라서 **지하 점유·충돌·확장·깊이·동선 지표가 모두 동일**합니다. 억지로 차이를 만들지 않습니다.

### 구분 가능한 유일한 요소 (모델 밖 정보 필요)
1. **지형/월드 배치**: 동쪽 후보는 L1·L2가 +X 쪽, 왕실 홀이 −X·+Z 사분면(남서); 서쪽 후보는 정반대. 실제 월드의 경사·수원·동굴·인접 구조물이 어느 쪽에 있는지.
2. **벽체 1블록 초과 방향**: 동쪽 후보는 X=41, 서쪽 후보는 X=−41. 해당 방향에 이웃 구조물/청크 경계 제약이 있는지.
3. **서사적 선호**: 왕실 홀이 입구 기준 좌측(서)에 놓일지 우측(동)에 놓일지(남쪽을 정면으로 들어올 때 지상 경로가 동으로 꺾였다가 반대편으로 돌아옴).
4. **미승인 prep V1 배치**: 동쪽에 비교적 큰 방을 배정하나 승인 사항이 아니며 본 대표 방 집합과 1:1이 아님 → 판단 근거가 되지 못함.

**결론: 모델상 우열 없음 (동등).** 위 정보 중 하나가 주어지지 않는 한 어느 쪽도 "더 적합"하다고 주장하지 않습니다. 소유자 선호나 세계 정보가 없을 때의 *관례적* 기본값으로 P1의 제안(동쪽)이 있으나 이는 장점이 아니라 관례입니다. **D-3은 확정하지 않았습니다.**

## 10. P2-C/D: 왕실 묘실 예약과 비밀 구역 평가

- 왕실 묘실 예약 `HALL_RES`(동쪽): X −40..−2, Z 2..40, Y −24..−5 (39×39×20). 청정 내부 `HALL_INT`: X −37..−5, Z 5..37, Y −22..−7 (**33×33×16**, Q43/Q44). 측면 두께 3, 바닥·지붕 슬래브 2. 코벨 천장의 계단식 형상·벽 높이 8(Q45)·기둥(Q47–Q62)·석관단은 **P3** 몫이며 이 예약 안에 들어가야 함(예약의 내부는 비워 두었으며 어떤 공동도 침범하지 않음: C2 PASS).
- 입구 포트: 문턱 `T`(X −22..−20 폭 3, Z −1..4 길이 6 = 공동 3 + 홀 벽 포트 3)이 홀 북벽 중앙에 접속. 포트 겹침 27복셀이 선언값과 일치(C2).
- 비밀 구역: 후보 A(적층)·B(측방) 모두 확장 없이 성립하나 **연결 가능성만 평가**했고 비밀 묘실(크기 미정)은 평가 불가. 비밀 입구 위치(홀 내부)는 P4 결정.

## 11. 미해결·미검증 (PASS를 주장하지 않는 항목)

- **UNVERIFIED**: 지형·월드젠·청크·이웃 구조물 간섭; 실제 클라이언트에서의 시각·조명·공간감; 플레이어 물리·몹 AI; 계단식 천장(Q17/Q41) 프로파일—P2는 직사각 포락선만 예약; 부속실(Q35/Q36) 개수·위치—P3 포트만 남김; 비밀 묘실 크기·위치, 비밀 구역 최종 좌표(P4); 최대 깊이 확정; 층간 간격 최소화; 서쪽 후보의 실제 월드 적합성.
- **필요한 소유자 결정(최소)**:
  1. **D-3**: 동/서 방향 (위 §9 정보 중 하나로 판단 가능).
  2. **D01 해석 확인**: 투영 ±40 기준을 공동으로 볼지 벽체까지 포함할지 (벽체 포함이면 V12·계단 A·참 동벽을 1~2블록 안쪽으로 옮기는 재배치가 필요; 이때 영향 요소는 모두 L1/L2 전이부).
  3. **P2 PROPOSED 좌표를 P3의 잠정 기준선으로 채택**할지 (P1 때와 같은 조건부 채택 요청).

## 12. P3/P4 인계

**P3(왕실 묘실 상세)에 전달:** `HALL_RES`/`HALL_INT` 좌표(§10), 입구 포트 `T` 끝 셀 (−21, −22, 4), 의식 전실 `A`(X −30..−12, Z −12..−2, Y −22..−13, 높이 10) — 동·북쪽 면에 부속실 분기 여유: 전실 서벽(X=−30) 밖 X −40..−31 10칸, 북벽(Z=−12) 밖 Z −40..−13 28칸이 모두 비어 있음(평면 기준; 수직 분리 필요 시 L1/L2와 무관). 통로 폭 규칙: C3 7 → R 5 → T 3(PASS). 주의: 부속실 개수는 미정(Q36).

**P4(비밀 구역)에 전달:** 후보 A: `VAULT_A` X −40..−14, Z 9..39, Y −39..−26(내부 21×25×10), `STAIR_A` X −13..−3, Z 9..39, Y −39..−25; 후보 B: L3 수준 측방 배치(§7 표, Y −24..−11). 홀 바닥의 비밀 입구 위치·황금 골렘 대체 접근 방식·비밀 묘실 크기·최대 깊이는 모두 P4 입력 대기.

**서쪽 후보 사용 시:** 모든 좌표의 X 부호를 반전(정확한 거울)하면 됨. 서쪽 좌표표는 §3.2.

## 13. 기존 설계·구현 파일 보존

- 변경은 신규 파일뿐: `tools/st01-p2/*`, `docs/design/st01-p2/ST01_P2_UNDERGROUND_3D_COORDINATE_DESIGN_2026-11-06.md`, `docs/design/st01-p2/generated/*`. 커밋 전 `git diff --name-status d2e00531 HEAD`가 위 신규 파일만을 보이도록 확인.
- Q01–Q100 문서, P1 문서·모델·검증기, 인덱스 문서, `src/**`(NBT·Java·월드젠·보스·보상·몹·전리품)는 수정하지 않음. PR·병합 없음.
- 이번 작업에서 승인된 설계 결정은 없음(제안만) → 권위 동기화 대상 없음. ST01-P2-D01(B)·D-3 미결정 상태는 사용자 지시문에 따르며, 권위 문서 반영은 별도 승인 작업.

## 14. 완료 기준 판정

| # | 기준 | 판정 |
| --- | --- | --- |
| 1 | L1–L3 주요 좌표·연결 정의 | 충족 (§3, §4) |
| 2 | 동·서 후보 모두 검증 | 충족 (각 18/18, FAIL 0) |
| 3 | 왕실 묘실 예약 체적 | 충족 (§10) |
| 4 | 비밀 구역 후속 연결 가능성 | **부분**: 보물창고 배치 가능성만 평가, 비밀 묘실은 미평가 |
| 5 | 수평 확장량 정량화 | 충족 (§7: 0) |
| 6 | 깊이 산정 근거 | 부분: P2 깊이 24 확보, 최대 깊이는 미확정 |
| 7 | Q01–Q100·P1 조건부 승인 보존 | 확인 (§13) |
| 8 | 미검증·소유자 결정 분리 | 충족 (§11) |

**P3 진행 가능 여부:** 가능(잠정). 단 PROPOSED 좌표 채택(결정 3)이 확인되기 전에는 P3 산출물도 "잠정" 표기가 필요하며, D-3이 확정되기 전까지 P3 좌표는 방향 가변(거울)으로 다뤄야 합니다.

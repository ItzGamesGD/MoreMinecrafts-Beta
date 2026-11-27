# ST-01 P5 — P1–P4 통합 3D 검증 (P4-B 우선 기준선) (2026-11-07)

**상태: PROPOSED 설계의 통합 검증 결과. 새 설계 권위가 아니며 구현(NBT·월드젠·보스·보상)·게임 내 검증의 승인이 아닙니다.** 기존 모델(P1–P4)과 Q01–Q100 문서는 수정하지 않았습니다. 동·서 방향은 모두 유지했고 D-3은 확정하지 않았습니다.

## 0. 요약

- **P4-B 통합 가능: 가능(모델 수준).** P1 진입광장부터 P1 대전당·하강 계단·P2 L1–L3·P3 왕실 묘실·P4-B 숨은 계단·보물창고 상자 정면까지 한 복셀 월드에서 보행 연결을 확인했습니다(광장 → 상자 정면 241걸음, 후보 C 배치 시 237걸음). 동·서 각각 독립 실행하여 복셀 거울 동치입니다.
- 결과(PASS/FAIL/UNVERIFIED), 각 조합은 동 / 서:
  - B + 기존 P2 배치: 10 PASS / 1 FAIL / 1 UNVERIFIED / 10 PASS / 1 FAIL / 1 UNVERIFIED — **FAIL 1건은 X1(P2-F1: 벽체 포함 X=±41)이며 새 결함이 아니라 기존 열린 항목의 재현**.
  - B + P2-F1 후보 C: 11 PASS / 0 FAIL / 1 UNVERIFIED / 11 PASS / 0 FAIL / 1 UNVERIFIED.
  - A(비교) + 기존 P2 배치: 10 PASS / 1 FAIL / 1 UNVERIFIED / 10 PASS / 1 FAIL / 1 UNVERIFIED; A + 후보 C: 11 PASS / 0 FAIL / 1 UNVERIFIED / 11 PASS / 0 FAIL / 1 UNVERIFIED.
- **계단 낙차**: B는 Y −22 → −30, 총 낙차 8 = 보고된 계단 단수 7 + 참 첫 칸 1. **결함 아님(정의 차이)**. A도 동일 방식(14단 + 참 1 = 15).
- **덮개**: 닫힘·열림 두 상태를 모델링. 닫힘 상태의 홀은 P3와 복셀 단위로 동일, 열림 상태의 차이는 12블록의 계단 절개뿐. **권장 후보: A(바닥 블록 제거형)** — 추가 공간 0. 슬라이딩/피스톤형은 현 2칸 슬래브에서 분리 두께 기준을 만족하지 못함. 게임 내 거동은 UNVERIFIED.
- **P2-F1 보완 가능: 가능.** 후보 C가 참 9×9·7폭 계단·기존 동선을 유지한 채 전체 구조물을 ±40 안(벽 포함 최대 X=39)에 넣고 P2 동·서 18/18, 통합 검증 전부 PASS, 추가 깊이 0, 주 동선 117 → 113으로 단축. **자동 채택하지 않았습니다.**
- **Q97**: B는 연속 구역 사이의 평면 중심이 모두 어긋나며 홀 아래에 놓이지 않음(§7). 문구의 정확한 의미(중심 이동 방향·최소 거리)는 권위에 없으므로 *최소 해석*에서만 충족, 강화 해석은 소유자 판단.
- **Q98**: B 최저 구조 블록 Y −32(깊이 32)를 박스 산술과 복셀로 독립 재계산해 일치. **최대 깊이는 확정하지 않음**(UNVERIFIED).

## 1. 기준·현재 승인 상태

| 항목 | 값 |
| --- | --- |
| 브랜치 / 기준 | `work/st01-p5-integration-20261107` / `work/st01-p4-secret-area-20261107` @ `0d486fd131638bc08de20b5be8f2846bab3c9177` (시작 시 작업 트리 깨끗, origin 일치) |
| 권위 | `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md` — Q01–Q100 확정, 모든 좌표 PROPOSED (재확인) |
| 승인 기록 | `docs/design/st01-p4/ST01_P4B_CONDITIONAL_ADOPTION_RECORD_2026-11-07.md` |
| 통합 방식 | P2 격자(X/Z −64..64, Y −60..40)에 P1 맥락 박스 + P1 하강 계단 발판 5개(P1 보고서 표) + P2 공동 + P3 홀(원본 모델 그대로) + P4 후보를 결합한 **하나의 복셀 월드**를 새로 구성 후 검증. 단계별 PASS를 합산하지 않음 |

## 2. 비밀 입구 덮개 (닫힘/열림 모델)

비밀 입구 구역은 P3의 5×5(X −9..−5, Z 33..37)이고 P4-B 계단은 Z 33..35·X −9 에서 동쪽으로 내려갑니다. 열린 상태에서 홀 바닥 높이(슬래브 윗면 Y −23)에 뚫리는 구멍은 **X −9..−6 × Z 33..35 = 12블록**(X −5는 계단 천장이 슬래브 1겹으로 남음)입니다.

| 상태 | 검증 | 결과 |
| --- | --- | --- |
| 닫힘 | K1: 덮개 12블록을 채운 홀이 P3 모델과 슬래브 윗면 이상 전 구역에서 복셀 동일(차이 0) → 보행면 연속·기둥/석관/오벨리스크/전투장 변화 없음 | PASS |
| 닫힘 | K2: 광장→홀 입구→석관단 보행 가능, 비밀 구역 보행 칸 도달 0 → 추락·진입 불가 | PASS |
| 열림 | K4: 홀 변경 = 계단 절개 12블록, 해석(박스 교차)과 일치, 선언된 관통 체적 내부, 추가 구멍 없음 | PASS |
| 열림 | K3·S1: 첫 발판(Y −23)으로 홀 바닥(Y −22)에서 한 칸 내려가 이동, 계단 머리 높이 ≥4, 벽·기초와 충돌 없음 | PASS |

덮개 후보 비교 (구현 없음, 공간·간섭은 모델 수치):

| 후보 | 필요 공간 | 구조 간섭 | 서버 구현 난이도 | 플레이 영향 | 검증 가능성 |
| --- | --- | --- | --- | --- | --- |
| A 바닥 블록 제거형 | 추가 0 (덮개 12블록 자체) | 없음(K1/K4 PASS) | 낮음(블록 교체) | 즉시 개방, 소리·입자 연출로 보완 필요 | 모델 수준 검증 완료, 게임 내 UNVERIFIED |
| B 슬라이딩 덮개형 | 덮개 12블록 + 같은 크기의 포켓과 레일 | 현 2칸 슬래브의 하층(Y −24)에 포켓을 두면 홀 바닥 공기와 사이 고체 1겹(기준 ≥2 미달); 포켓을 슬래브 밖으로 빼려면 홀 예약(P3) 또는 계단 변경 필요 | 중간(엔티티/블록 이동 + 충돌 처리) | 연출 좋음, 이동 중 끼임·보스전 중 오작동 위험 | 이동 중 거동은 UNVERIFIED |
| C 피스톤/구조물 이동형 | 12블록 이동 한계 근처, 피스톤 몸체·확장 공간·신호선 | B보다 큰 포켓 + 신호 경로, 마찬가지로 분리 두께 미달 | 높음(레드스톤/타이밍, 서버 틱 의존) | 소음·지연, 우회 가능성 | 거동 UNVERIFIED |

**권장: A**(덮개 방식은 확정하지 않음). 이유: 추가 공간·간섭이 없고 닫힘/열림 상태가 모델에서 완전히 검증됨. 대가: 연출 비용과 "의도치 않은 조기 개방" 방지 로직 필요(구현 단계). B/C를 원하면 P3 슬래브 두께 확대(P3 수정) 또는 포켓 위치 설계가 선행되어야 하며 이 경우 깊이가 늘 수 있음(예: 슬래브 +1 → 최저 Y −33, 가정값).

## 3. 계단 낙차 독립 검증

P4 검증기와 별개로 **최단 보행 경로를 추출해 높이를 순서대로 기록**했습니다(S1).

| 후보 | 방문한 발 높이 Y (시작 홀 바닥 → 목적지 바닥) | 총 낙차 | 단위 하강 | 상승 | 최대 |dy| | 계단 공간 발판 수(P4 정의) |
| --- | --- | --- | --- | --- | --- | --- |
| B | -22, -23, -24, -25, -26, -27, -28, -29, -30 | 8 | 8 (FA 7 + 참 LA 첫 칸 1) | 0 | 1 | 7 |
| A | -22, -23, -24, -25, -26, -27, -28, -29, -30, -31, -32, -33, -34, -35, -36, -37 | 15 | 15 (FA 9 + 참 LA 첫 칸 1 + FB 5) | 0 | 1 | 14 |

- 인접 보행면 높이 차는 모두 1 이하이고 하강만 있으며 높이가 연속(누락된 첫 단·마지막 단 없음). 계단 위 머리 공간 최소 4.
- **정의 차이로 설명됨**: "계단 단수 7"은 P4 계단 공간(FA)의 발판 수이고, 총 낙차 8은 거기에 참 첫 칸으로 내려서는 1단이 더해진 값(7 + 1). 같은 정의로 A는 14 + 1 = 15. **결함 없음, 수정 후보 없음.**

## 4. P2-F1 보완 후보 탐색

우선순위: 9×9 참 유지, 7폭 계단 유지, 기존 L1–L3 동선 유지, 승인 치수 유지, 전체 ±40 이내, 추가 깊이·보행 거리 증가 최소. 기존 P2 모델은 수정하지 않고 메모리 사본에서만 계산했습니다.

| 안 | 내용 | P2 엔진 | 참 | V12 | 벽 포함 최대 X | 초과 공간 | 주 동선(포트까지) | 추가 깊이 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 기준 P2 | 변경 없음 | 동 18/18 / 서 18/18 | 9×9 | 9×11 | 41 | LND12, V12 | 117 | 0 |
| 변형 A (미채택) | V12·LND12·FA 동쪽 2열 제거 → 참 7×9 | 동 18/18 / 서 18/18 | 7×9 | 7×11 | 39 | 없음 | 115 | 0 |
| 후보 C (신규, 미채택) | 전이부(V12·FA·LND12·FB) 서쪽 2칸 이동, C1·C2 각 2칸 단축, BRB1 재배치 → 참 9×9 유지 | 동 18/18 / 서 18/18 | 9×9 | 9×11 | 39 | 없음 | 113 | 0 |

**후보 C**: 전이부(V12·FA·LND12·FB)를 서쪽으로 2칸 옮기고, 그 때문에 닿는 C1(L1 주 통로)과 C2(L2 주 통로)를 각각 2칸 줄이고 BRB1을 C2 끝에 맞게 X 22..24로 재배치. 모든 승인된 치수(참 9×9, 7폭 계단, 전이 전실 9×11, 통로 폭 7)와 층 높이·계단 단수가 그대로이고 동선이 4걸음 짧아짐. 통합 월드(P3·P4-B 포함) 전 항목 PASS(§6). **자동 채택하지 않았습니다.** 미탐색: 이 안의 L1/L2 방 위치(예: 분기 BRB1·B1)에 대한 소유자 선호, P1 E1 인터페이스와의 시각적 정렬.

## 5. 통합 평면·단면 (후보 C 배치 + P4-B, 열린 상태, 동쪽; 나머지는 `generated/plans/`)

Y=−30 (보물창고·묘실·통로 층):

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

단면 Z=35 (홀 바닥에서 숨은 계단):

```
INTEGRATED SECTION Z=35 horizontal X -45..45, Y -40..8 (open state)
  +8                                   1111111111111111111111111                                 
  +7                                   1111111111111111111111111                                 
  +6                                   1111111111111111111111111                                 
  +5                                   1111111111111111111111111                                 
  +4                                   1111111111111111111111111                                 
  +3                                   1111111111111111111111111                                 
  +2                                   1111111111111111111111111                                 
  +1                                   1111111111111111111111111                                 
  +0                                   1111111111111111111111111                                 
  -1                                                                                             
  -2                                                                                             
  -3                                                                                             
  -4                                                                                             
  -5                                                                                             
  -6                                                                                             
  -7                                                                                             
  -8                                                                                             
  -9                                                                                             
 -10                                                                                             
 -11                                                                                             
 -12                                                                                             
 -13                                                                                             
 -14            33333333333333333333333333333                                                    
 -15          333333eee333333333333333eee333333                                                  
 -16          3333eeeeeee33333333333eeeeeee3333                                                  
 -17          33333eeeee3333333333333eeeee33333                                                  
 -18          33333eeeee3333333333333eeeee33333####                                              
 -19          333333eee333333eee333333eee333333####   ###############################            
 -20          333333eee333333eee333333eee343333####   ###############################            
 -21          333333eee33eeeeeeeeeee33eee344333####   ###############################            
 -22          33333eeeee3eeeeeeeeeee3eeeee44433####   ###############################            
 -23                                    ##4444#####   ###4444444444444444444444444###            
 -24                                    ###4444##########4444444444444444444444444###            
 -25                                    ####4444#########4444444444444444444444444###            
 -26                                    #####444444444###4444444444444444444444444###            
 -27                                    ######44444444###4444444444444444444444444###            
 -28                                    #######4444444###4444444444444444444444444###            
 -29                                    ########444444###4444444444444444444444444###            
 -30                                    #########44444###4444444444444444444444444###            
 -31                                    #############################################            
 -32                                           ######################################            
 -33                                                                                             
 -34                                                                                             
 -35                                                                                             
 -36                                                                                             
 -37                                                                                             
 -38                                                                                             
 -39                                                                                             
 -40                                                                                             
```

단면 Z=26 (보물창고 축):

```
INTEGRATED SECTION Z=26 horizontal X -45..45, Y -40..8 (open state)
  +8                                           111111111                                         
  +7                                           111111111                                         
  +6                                           111111111                                         
  +5                                           111111111                                         
  +4                                           111111111                                         
  +3                                           111111111                                         
  +2                                           111111111                                         
  +1                                           111111111                                         
  +0                                           111111111                                         
  -1                                                                                             
  -2                                                                                             
  -3                                                                                             
  -4                                                                                             
  -5                                                                                             
  -6                                                                                             
  -7                                                                                             
  -8                                                                                             
  -9                                                                                             
 -10                    3333333333333                                                            
 -11                  33333333333333333                                                          
 -12                333333333333333333333                                                        
 -13              3333333333333333333333333                                                      
 -14            33333333333333333333333333333                                                    
 -15          33eee33333333333333333333333eee33                                                  
 -16          333333333333333333333333333333333                                                  
 -17        33333333333333333333333333333333333                                                  
 -18        e333333333333333333333333333333333333                                                
 -19        e333333333333333333333333333333333333     ###############################            
 -20        e333333333333333333333333333333333333     ###############################            
 -21        e333333333333333333333333333333333333     ###4444444444444444444444444###            
 -22        e33333333333333333333333333333333333e     ###4444444444444444444444444###            
 -23                                           ##########4444444444444444444444444###            
 -24                                           ##########4444444444444444444444444###            
 -25                                           ##444444444444444444444444444444444###            
 -26                                           ##444444444444444444444444444444444###            
 -27                                           ##444444444444444444444444444444444###            
 -28                                           ##444444444444444444444444444444444###            
 -29                                           ##444444444444444444444444444444444###            
 -30                                           ##444444444444444444444444444444444###            
 -31                                           ######################################            
 -32                                           ######################################            
 -33                                                                                             
 -34                                                                                             
 -35                                                                                             
 -36                                                                                             
 -37                                                                                             
 -38                                                                                             
 -39                                                                                             
 -40                                                                                             
```

## 6. 통합 검증 결과 (열 순서: B+기존 동/서, B+C 동/서, A+기존 동/서, A+C 동/서)

| ID | 검사 | B기존 E | B기존 W | BC E | BC W | A기존 E | A기존 W | AC E | AC W |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| K1 | Closed state: the royal hall (all cells of the P3 reservation at floor-top level and above) is voxel-identical to the P3 model - safe walking surface, no hole, no change to columns/dais/obelisk/arena | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| K4 | Open state: the only change to the hall at/above the floor-top level is the stair cut implied by the P4 stair boxes (voxel diff == analytic box intersection), inside the declared pierce volume, no extra holes | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| K2 | Closed state: forecourt -> P1 hall -> P1 descent -> P2 L1-L3 -> royal hall (entrance, dais) all walkable; no walkable cell of the secret area is reachable (player cannot fall into / enter the stair) | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| K3 | Open state: complete chain forecourt -> ... -> royal hall -> hidden stair -> landing(s) -> vault entrance -> aisle -> dais stair -> chest front (5 cells) and the crypt are reachable on foot | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| S1 | Independent stair drop check: shortest walk hall floor -> vault entrance has only unit steps, no rise, no missing first/last step, consecutive heights, headroom >= 4 on stair cells; drop = number of unit descents | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| I1 | Whole-structure collision/contact scan on one combined voxel world (P1 context, P2, P3 hall cavity, P4): no overlap except the declared hatch pierce, face contacts only at the declared links (P1<->P2 landing, P2 port<->hall, hall<->stair cut) | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| P2R | Re-run of the P2 engine on the P2 layout used in this integrated world (intra-P2 collisions, undeclared contacts, widths, reachability) | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| I2 | Analytic (box arithmetic) minimum wall between cavities of different stages is >= 2 except at the declared links; P4 spaces other than the stair cut keep >= 2 to the hall | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| X1 | Whole generated underground structure (P2 cavities + wall 2, P3 reservation, P4 vault/crypt/corridor structures) stays inside X/Z +-40 (D01 whole-structure reading) | FAIL | FAIL | PASS | PASS | FAIL | FAIL | PASS | PASS |
| D1 | Lowest cavity and lowest structure block of the whole integrated structure recomputed from boxes and from voxels (Q98 input; not an approved maximum) | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| L1 | P2 transition spaces keep the approved-by-P2 proportions: landings 9x9, L1/L2 vestibule 9x11, 7-wide stair flights | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| C1 | Cover candidates: spatial requirement of block removal (A), sliding panel (B), piston/structure (C) in the 2-thick hall slab | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED |

거울 동치(복셀, 조합별): B|base: closed_solid=True,open_solid=True,own=True,elem=True; A|base: closed_solid=True,open_solid=True,own=True,elem=True; B|C: closed_solid=True,open_solid=True,own=True,elem=True; A|C: closed_solid=True,open_solid=True,own=True,elem=True

해석 검증과 복셀 검증의 대조: K4(복셀 차이 vs 박스 교차), S1(경로 높이 vs 낙차 산술), I1(복셀 마스크 겹침/접촉) vs I2(박스 간격), X1(박스 극값 vs 복셀 합집합), D1(박스 최저 vs 복셀 최저).

### 증거 (B + 후보 C, 동쪽)

- **K1 PASS** — differing cells 0; cover cells placed 12; hidden cavity cells left below the cover (inside the 2-thick slab zone, y<-23) 12
- **K4 PASS** — changed cells 12 (analytic 12) all inside pierce True; cut footprint at floor-top layer 12 cells
- **K2 PASS** — unreachable hall goals []; secret-area walkable cells reachable below the hall floor 0; steps forecourt->hall entry 154, ->dais top 185; total walkable states 3876
- **K3 PASS** — unreachable []; chain length forecourt->chest front 237 steps; goals checked 12
- **S1 PASS** — start Y -22 -> end Y -30; total drop 8; unit descents 8, rises 0, max |dy| 1; heights visited [-22, -23, -24, -25, -26, -27, -28, -29, -30]; stair-space treads (P4 definition) 7; descents per space {'FA': 7, 'LA': 1}; min headroom on stair cells 4, along whole path 4
- **I1 PASS** — overlap voxels per stage pair {(1, 2):0, (1, 3):0, (1, 4):0, (2, 3):0, (2, 4):0, (3, 4):18}; pierce overlap (3,4) 18, outside the pierce 0; face-contact voxels per pair {(1, 2):35, (1, 3):0, (1, 4):0, (2, 3):9, (2, 4):0, (3, 4):42}; unexpected contact pairs []; P4 internal overlaps 0; carved inside HALL_RES outside pierce 0
- **P2R PASS** — 15 P2 engine checks, not PASS: []; P2 cavity overlaps 0
- **I2 PASS** — stage-pair minimum Chebyshev gaps {(1, 2):0, (1, 3):6, (1, 4):16, (2, 3):0, (2, 4):3, (3, 4):0}; hall<->(P4 except FA) 2; violations {}
- **X1 PASS** — max excess 0 blocks; voxels outside 0; P2 spaces whose wall-inclusive extent exceeds 40: []
- **D1 PASS** — lowest P4 cavity Y -30 (box -30); lowest cavity overall -30; lowest structure block -32 = depth 32
- **L1 PASS** — landings ((9, 9), (9, 9)), V12 (9, 11), flight A width 7
- **C1 UNVERIFIED** — cut at floor-top layer = 12 blocks. A needs 0 extra cells. B/C need >= 12 pocket cells beside the cut; a pocket in the lower slab layer (Y -24) leaves 1 solid layer(s) to the hall floor air (needs >= 2), so B/C require a thicker slab or outside-footprint pocket (not designed). Mechanism feasibility/in-game behaviour not verifiable here.

### 증거 (B + 기존 P2 배치의 FAIL 재현)

- **X1 FAIL** — max excess 1 blocks; voxels outside 140; P2 spaces whose wall-inclusive extent exceeds 40: ['LND12', 'V12']
- **L1 PASS** — landings ((9, 9), (9, 9)), V12 (9, 11), flight A width 7

## 7. Q97 · Q98

**Q97 원문**: "Extend Q37–Q39 staggered horizontal underground layout through secret crypt and Q16 treasury; actual coordinates TBD." Q37: "L1–L3 horizontally staggered zones; exact centers TBD." 권위에는 *엇갈림의 방향·최소 거리·단조성*이 규정되어 있지 않습니다. 이 검토는 Q97을 강화하지 않았습니다.

연속 구역 사이의 평면 중심 이동과 앞 구역 아래에 놓이는 비율(뒤 구역 평면 기준):

B (기준 P2 배치):

| 구간 | 중심 | 이동 거리 | 앞 구역 평면에 겹치는 비율 |
| --- | --- | --- | --- |
| L1 → L2 | (23.5,-11.5) → (15.5,6.0) | 19.2 | 34% |
| L2 → L3 | (15.5,6.0) → (-15.5,-4.0) | 32.6 | 0% |
| L3 → 왕실 홀 | (-15.5,-4.0) → (-21.0,21.0) | 25.6 | 6% |
| 왕실 홀 → 비밀 묘실 | (-21.0,21.0) → (15.0,-1.0) | 42.2 | 0% |
| 비밀 묘실 → 보물창고 | (15.0,-1.0) → (18.0,26.0) | 27.2 | 0% |

A (비교):

| 구간 | 중심 | 이동 거리 | 앞 구역 평면에 겹치는 비율 |
| --- | --- | --- | --- |
| L1 → L2 | (23.5,-11.5) → (15.5,6.0) | 19.2 | 34% |
| L2 → L3 | (15.5,6.0) → (-15.5,-4.0) | 32.6 | 0% |
| L3 → 왕실 홀 | (-15.5,-4.0) → (-21.0,21.0) | 25.6 | 6% |
| 왕실 홀 → 비밀 묘실 | (-21.0,21.0) → (14.0,24.0) | 35.1 | 0% |
| 비밀 묘실 → 보물창고 | (14.0,24.0) → (-27.0,24.0) | 41.0 | 0% |

판정: *최소 해석*("연속 구역의 평면이 같은 자리에 쌓이지 않고 중심이 어긋남")으로는 B가 충족합니다(홀 → 비밀 묘실/보물창고가 겹침 0%이거나 큰 이동). A는 보물창고가 홀 평면 안에 100% 놓여 이 해석에서 적층입니다. "일정 방향으로 계속 이동"(단조) 같은 강화 해석은 B에서도 성립하지 않으며(묘실 ↔ 보물창고 방향 반전) 권위에 없으므로 **해석이 필요한 항목 = UNVERIFIED(소유자 판단)**.

**Q98**: B 최저 구조 블록 Y −32(깊이 32)를 박스 산술과 복셀로 독립 재계산해 일치(D1). 민감도:
- 비밀 묘실 크기 변경: 묘실 바닥을 보물창고와 같은 Y −30으로 두면 폭·길이·높이는 최저 Y를 바꾸지 않음(높이는 위쪽으로만 늘어남). 바닥을 더 낮추는 변경만 깊이를 늘림.
- P2-F1 후보 C: X/Z 이동뿐이라 깊이 변화 0.
- 덮개 B/C형: 슬래브 확대가 필요하면 깊이 증가(가정값, 미설계).
- 보물창고 바닥 Y −28(4높이 통로로 재설계 시 가능)은 탐색하지 않음.
따라서 **잠정 근거값 32**이며 **최대 깊이는 확정하지 않습니다(UNVERIFIED)**: 묘실 사양, 덮개 방식, 층간 8 최소화(P2-F6), 후보 선택(B 우선·A 보존)이 미확정입니다.

## 8. 동·서 방향 비교

모든 통합 조합에서 동쪽 월드를 X 반전한 것과 서쪽 월드가 복셀 단위로 일치하고(solid/own/elem 및 닫힘/열림), 검증 판정 목록도 같습니다. 기하 지표는 동일하며 구별 가능한 것은 **손 방향**(숨은 입구와 보조 상자 벽감이 피라미드 축 쪽; 남쪽을 향해 입장 시 동쪽 후보는 왼손, 서쪽 후보는 오른손)과 P2-F1의 초과 방향(동 X=+41 / 서 X=−41, 후보 C 채택 시 무관)뿐입니다. **D-3은 확정하지 않았습니다.**

## 9. 음성 대조 테스트

결함을 일부러 주입한 12개 중 12개 검출, 무결함 대조군 FAIL 0 (후보 B + 후보 C 배치, 동쪽 기준).

| 주입한 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- |
| hidden stair starts one block too low (first step 2 high) | S1 | K3, S1 | 검출 |
| last stair tread missing (landing 2 below) | S1 | K3, S1 | 검출 |
| stair headroom 3 | S1 | S1 | 검출 |
| cover never placed (closed hall has a hole) | K1 | K1, K2 | 검출 |
| cover placed one layer too high (floor raised) | K1 | K1 | 검출 |
| pierce volume shrunk (cut leaves the declared volume) | I1, K4 | I1, K4 | 검출 |
| candidate B vault dropped to the hall depth (collides with L2) | I1 | I1, I2, K3, S1 | 검출 |
| candidate B vault pushed past X=+40 | X1 | K3, S1, X1 | 검출 |
| P2-F1 layout C landing shrunk to 7x9 | L1 | L1 | 검출 |
| P2-F1 layout C flight B left overlapping C2 | P2R | K2, K3, P2R | 검출 |
| P2-F1 layout C vestibule left at the old X (not shifted) | P2R, X1 | K2, K3, P2R, X1 | 검출 |
| P1 descent treads missing from the integrated world (chain broken) | K2, K3 | K2, K3 | 검출 |

처음 실행에서 3건이 기대한 검사가 아닌 다른 검사로 검출되었습니다(P2 내부 충돌을 통합 검사가 보지 않던 빈틈). 검증기에 P2 엔진 재실행 항목(P2R)과 관통 체적 밖 굴착 항목을 추가했습니다.

## 10. 판정 구분

**PASS**(모델과 검증 근거로 확인): 닫힘 상태 홀 동일성, 닫힘 상태 안전, 열림 상태 절개 일치, 전체 보행 연결(광장 → 상자 정면 5칸), 단계 간 겹침 0·미선언 접촉 0, 단계 간 최소 벽 ≥2, 계단 낙차 연속성, 깊이 독립 재계산, 후보 C 배치의 ±40 이내, 동·서 거울 동치, P2 엔진 재확인.

**FAIL**(기존 승인 조건과 충돌): 새 FAIL 없음. 기존 P2 배치의 X1(벽체 포함 ±41, D01 전체 구조물 기준)은 이미 열린 P2-F1이며 후보 C로 해소 가능함을 보였을 뿐 채택되지 않았습니다.

**UNVERIFIED**: 덮개 메커니즘의 실제 거동, 열린 구멍의 낙하·끼임, 보스 전투(BOSS-R1)와 개방 타이밍, 황금 골렘 안내 경로, 지형·월드젠·청크, 클라이언트 시각, 상자 구현, 비밀 묘실 사양, Q97 강화 해석, Q98 최대 깊이.

## 11. 미해결 소유자 결정 (필요한 것만)

1. **덮개 방식**(권장 A): A 확정 또는 B/C를 위한 슬래브·포켓 설계 허용.
2. **P2-F1**: 후보 C 채택 여부(채택 시 P2 좌표 갱신은 별도 승인 작업).
3. **Q15 비밀 묘실 사양**과 **D-3 방향**(미확정 유지 가능하나 P6/구현 전에는 필요).
4. **Q97 해석** 확인(최소 해석으로 충분한지).
5. **BOSS-R1**: 실제 전투 검증 일정(검증 전까지 홀 입구 높이 불변).

## 12. 보존 확인

변경은 신규 파일뿐입니다(`tools/st01-p5/*`, `docs/design/st01-p5/*`, `docs/design/st01-p4/ST01_P4B_CONDITIONAL_ADOPTION_RECORD_2026-11-07.md`). Q01–Q100 문서, P1–P4 모델·검증기·보고서·생성물, `src/**`는 수정하지 않았고 PR·병합은 없습니다.

# ST-01 P10 — 최종 검증 보고서 (산출물 10)

작업: 실제 왕실 묘실 기하 기반 금 골렘 통합 검증. 게임테스트·측정 단계이며 최종 보스 AI·월드젠·보상 정책은 확장하지 않았다.

## 1. 기준과 브랜치
| 항목 | 값 |
| --- | --- |
| 저장소 | ItzGamesGD/MoreMinecrafts-Beta |
| 기준 브랜치 / 커밋 | `work/st01-p9-victory-split-20261108` @ `eecf09b5b1478ba13be4382beb3913682e375a5e` (시작 시 HEAD = origin, 작업 트리 깨끗, 일치 확인) |
| 작업 브랜치 | `work/st01-p10-real-tomb-golem-20261108` (새 브랜치) |
| 최종 커밋 | 브랜치 HEAD(이 문서를 포함한 커밋; 문서가 자기 SHA를 담을 수 없어 SHA는 작업 완료 보고에 기재) |
| PR / 병합 | **하지 않음** |
| 환경 | Temurin JDK 25.0.5 (이 컨테이너에는 JDK 21만 있어 Adoptium에서 받아 사용), Minecraft 26.3, Fabric Loom 1.17.21, 헤드리스 GameTest 서버 |

## 2. 권위·변경 제한 준수
- 유지: Q01–Q100, P1–P4 조건부 승인, P2-F1 후보 C, P4-B 측면 배치, Q97 최소 해석, P6 덮개 제거형 A안, P7 상태 전이, P8–P9 승인 및 운영 코드.
- 미확정으로 유지: **D-3**(동·서; 둘 다 시험했을 뿐 채택하지 않음), **Q15**, **Q98**, O-1, O-4, O-6, O-9, O-12, O-14.
- **운영 코드(`src/main`, `src/client`) 변경 0줄.** 구조물 기하 변경 0칸(픽스처 다이제스트가 승인된 P6 모델과 동일, 산출물 1 V1). 평탄 우회 경로 추가 없음.
- 변경한 기존 파일은 시험 자원/시험 코드뿐: `src/gametest/resources/fabric.mod.json`(신규 시험 클래스 3개 등록), `SecretEntranceGameTests.java`(아래 §3의 시험 전용 수정 2건).

## 3. 변경 파일 목록
| 분류 | 파일 |
| --- | --- |
| 시험 코드(신규) | `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/{P10Support,RoyalTombFixture,RoyalTombGolemGameTests,RoyalTombFlowGameTests,RoyalTombSafetyGameTests}.java`, `src/test/java/dev/itzgamesgd/moreminecrafts/boss/TombVictoryRuleComparisonTest.java` |
| 시험 자원(신규) | `src/gametest/resources/st01/p10_fixture_{E,W}.json`, `src/gametest/resources/data/moreminecrafts-gametest/test_environment/{p10_flows,p10_isolated,p10_sweeps,p10_load,p10_load_150,p10_load_250}.json` |
| 시험 코드/자원(수정) | `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/SecretEntranceGameTests.java`(§3.1), `src/gametest/resources/fabric.mod.json`(신규 시험 클래스 3개 등록) |
| 도구(신규) | `tools/st01-p10/{export_fixture,verify_fixture,analyze,o1_candidates}.py`, `tools/st01-p10/run_restart.sh` |
| 문서(신규) | `docs/design/st01-p10/ST01_P10_01…10_*.md`, `docs/design/st01-p10/generated/*`, `docs/design/st01-p10/data/**` (측정 CSV) |
| 운영 코드 | **없음** |

### 산출물 색인
| # | 산출물 | 파일 |
| --- | --- | --- |
| 1 | 실제 묘실 픽스처·복셀 일치 | `ST01_P10_01_REAL_HALL_FIXTURE_AND_VOXEL_CONFORMANCE_2026-11-08.md` |
| 2 | 골렘 경로별 이동 결과 | `ST01_P10_02_GOLEM_ROUTE_RESULTS_2026-11-08.md` |
| 3 | 반복 측정 데이터·통계 | `ST01_P10_03_REPEATED_MEASUREMENTS_AND_STATISTICS_2026-11-08.md` |
| 4 | 300틱 미도착 원인 분석 | `ST01_P10_04_300_TICK_MISS_ANALYSIS_2026-11-08.md` |
| 5 | O-1 대체 개방 시간 후보 | `ST01_P10_05_O1_FALLBACK_TIME_CANDIDATES_2026-11-08.md` |
| 6 | O-4 도착 반경·정지 지점 후보 | `ST01_P10_06_O4_ARRIVAL_RADIUS_AND_STOP_POINT_2026-11-08.md` |
| 7 | O-6 크기·속도·생성 위치 후보 | `ST01_P10_07_O6_GOLEM_SIZE_SPEED_SPAWN_2026-11-08.md` |
| 8 | O-9 승리 판정 비교 | `ST01_P10_08_O9_VICTORY_RULE_COMPARISON_2026-11-08.md` |
| 9 | 덮개 낙하 안전성 | `ST01_P10_09_COVER_FALL_SAFETY_REPORT_2026-11-08.md` |
| 10 | 최종 검증 보고서 | 이 문서 |

### 3.1 기존 P9 시험에 가한 최소 시험 전용 변경 (원인 분석 결과, 산출물 4)
1. `buildArena()`에서 `SecretEntranceRuntime.forgetLiveState()` 호출 제거 — 프로세스 전역 `LIVE` 맵을 지워 다른 시험의 안내 중 코어를 소거함(P9의 300틱 미도착의 원인).
2. `openStateSurvivesLiveStateLossAndRepairsRevertedBlocksButNeverCloses`를 별도 환경(`moreminecrafts-gametest:p10_isolated`)으로 분리 — 의도적으로 라이브 상태를 지우므로 다른 입구 시험과 같은 배치에서 돌면 안 된다.
운영 코드와 시험의 기대값은 바꾸지 않았다.

## 4. 검증 결과
표기: PASS / FAIL / UNVERIFIED / NOT RUN. "GAMETEST"는 실제 서버 코드 경로를 도는 헤드리스 서버 GameTest, "LIVE"는 실제 클라이언트·플레이어.

| 계층 | 명령/범위 | 결과 | 상태 |
| --- | --- | --- | --- |
| 컴파일 | `clean compileJava compileClientJava compileGametestJava` (JDK 25) | 성공 | **COMPILE PASS** |
| 단위 테스트 | `./gradlew test` | 593개(P9의 591 + 신규 O-9 비교 2), 실패 0 | **UNIT PASS** |
| 설계 도구 | `verify_fixture.py` | 16 PASS / 0 FAIL (승인된 P6 다이제스트 일치, 동·서 거울, 요소 상자, 33×33·코벨, 덮개 12칸, 변이 대조 5종) | **PASS** |
| P9 관련 게임테스트 | `secret_entrance_game_tests_*` | 17/17 | **GAMETEST PASS** |
| P9 관련 게임테스트 | `contract_catalog_game_tests_*` | 7/7 | **GAMETEST PASS** |
| 기존 보스 회귀 | `tomb_guardian_game_tests_*` | 24/24 (3회) · **24개 중 1개 실패 (1회)**: `risen_statue_is_solid_at_the_approved_size_and_telegraph_stays_walkable`(tick 33에서 충돌 상자 검사). 이번 변경과 무관(해당 코드·클래스 미변경), 이후 3회 연속 통과 및 전체 스위트 3회 통과 → **불안정(FLAKY) 관찰로 기록, 수정하지 않음** | **GAMETEST PASS (1/4 실행에서 불안정)** |
| P10 실제 묘실 게임테스트 | `royal_tomb_*` 기본(경량) 모드 | 30/30 (복셀·좌표·거울·음성 대조 5종·경로 회귀·파이프라인·경합·소실/언로드·덮개 점유자·개방 후 이동·피해원 귀속) | **GAMETEST PASS** |
| P10 측정 모드 | `-Dmoreminecrafts.p10.measure=true` 전체 + 최종 HEAD에서 서쪽 기본 스윕 270회·서쪽 참가자 표본 32회 재확인 | 데이터 표본 수는 §6, 재확인 270/270·32/32 | **GAMETEST PASS** |
| 동·서 미러링 | 모델/월드 거울(복셀) + 경로 540회 | 불일치 0, 도착 차 ≤ 1.5틱 | **PASS** |
| 반복 경로 측정 | 540 + 270 + … (§6, 산출물 3) | 실패 0 | **PASS (결정적 시뮬레이션, 독립 표본 아님)** |
| 실제 서버 프로세스 재시작 | `run_restart.sh` 6단계(open/guiding × write/reload/verify), 최종 HEAD | 6/6 | **GAMETEST PASS** |
| 음성 대조 | 벽 1칸 제거·장애물 추가·잘못된 방향·비거울·경로 차단 + Python 변이 5종 + `forgetLiveState` 재현 | 전부 검출 | **PASS** |
| 전체 게임테스트 스위트 | `./gradlew runGameTest` (574개: 기본 544 + P10 30), 최종 HEAD 3회 | 실행마다 **1 / 18 / 18개 실패, 전부 `colosseum_*`**; ST-01·P10 실패 0; OOM 없음 | **FAIL (기존 Colosseum 불안정; 전체 PASS로 보고하지 않음)** |
| 실제 클라이언트·실제 플레이어 | — | 수행하지 않음 | **UNVERIFIED / LIVE VALIDATION PENDING** |
| 골렘 렌더링 크기 ↔ 충돌 크기 시각 일치 | — | 수행하지 않음(코드 판독만) | **UNVERIFIED** |
| 플레이어 4블록 낙하 피해 | — | 모의 플레이어는 피해 경로를 타지 않아 측정 불가 | **UNVERIFIED** |

Colosseum 실패 이력(이번 작업 중 같은 코드): 13(계측 실행) · 18(최종 HEAD) · 1 · 18 · 10(첫 실행, 이 중 1건은 ST-01 간섭 사례)개. 기준 커밋에서도 재현됨(P9 기록). 이번 변경과 무관하게 별도 추적한다.

### 4.1 명령
```
# 컴파일/단위
./gradlew clean compileJava compileClientJava compileGametestJava
./gradlew test
# P9 관련
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:secret_entrance_game_tests_*  ./gradlew runGameTest
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:tomb_guardian_game_tests_*    ./gradlew runGameTest
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:contract_catalog_game_tests_* ./gradlew runGameTest
# P10 실제 묘실 (기본: 경량 회귀 모드)
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:royal_tomb_*                  ./gradlew runGameTest
# P10 전체 측정 모드 (표본 수 전부, 수 분~수십 분) + CSV 출력
JAVA_TOOL_OPTIONS="-Dmoreminecrafts.p10.measure=true -Dmoreminecrafts.p10.out=<dir> -Dfabric-api.gametest.filter=moreminecrafts-gametest:royal_tomb_*" ./gradlew runGameTest
# 실제 프로세스 재시작 (3단계 × 2모드)
tools/st01-p10/run_restart.sh <log_dir>
# 복셀 일치(설계 도구)
python3 -I tools/st01-p10/verify_fixture.py src/gametest/resources/st01 <out.json>
# 전체 스위트
./gradlew runGameTest
```

## 5. 실제 게임테스트 실행 여부
**예 — 실제 Minecraft 26.3 헤드리스 서버에서 실행했다**(블록 배치, 엔티티 AI, 경로탐색, SavedData 저장, 프로세스 재시작). **실제 클라이언트·사람 플레이어는 없다.** 참가자는 모의 서버 플레이어이며 입력·충돌·지연이 없다. 따라서 이 보고서의 어떤 항목도 "실제 플레이로 검증했다"고 주장하지 않는다(LIVE: UNVERIFIED).

## 6. 측정 표본 수와 분포 (자세한 내용은 산출물 2–7)
| 측정 | 표본 | 결과 |
| --- | --- | --- |
| 기본 경로 도착 | 540(9 출발점 × 동/서 × 30) | 540/540, 실패 0; BOSS_S 146–155틱(중앙 153), 먼 출발점 295–350틱 |
| 단독 vs 동시 실행 | 270 vs 270 | 출발점별 최소/최대 동일 ±1틱 |
| 동·서 비교 | 540 중 동 270 / 서 270 | 중앙값 차 ≤ 1.5틱, 정지 위치 거울 오차 ≤ 0.01칸 |
| 정지 칸 후보 | 33칸 × (6 + 4) = 330 | Z3/Z4 11칸만 양쪽 접근에서 100%·겹침 0·간격 ≥ 2.55 |
| 크기·속도 한 변수씩 | 280 + 문 통과 36 | 문 통과 한계 scale ≈ 1.1, ≥0.75는 비밀 복도 진입 불가 |
| 서버 틱 부하 | 90 (64/154/254 ms) | 틱 단위 도착 불변 |
| 전체 파이프라인 | 99 | 참가자 표본 64/64 정상 개방(149–404틱), 경합 12(8 도착 + 4 대체), 소실·언로드·무반응 시나리오는 대체 개방으로 수렴 |
| 덮개 점유자 | 동·서 각 20 개체 | 매몰 0, 최대 낙하 4(주민 피해 1) |

## 7. 기존 P9 대비 차이
| 항목 | P9 | P10 |
| --- | --- | --- |
| 지오메트리 | 합성 평지 아레나(덮개 칸 = 바닥 블록) | 실제 P6 왕실 묘실 106,288칸(승인 모델과 다이제스트 동일), 동·서 |
| 생성→정지 도착 | 약 113틱 | **BOSS_S 146–155틱** (같은 기본 생성 칸, 기둥·제단·좁은 구역 포함) |
| 덮개 낙하 | 최대 4(마을 주민 20→19) | 열 깊이 1/2/3/4, 매몰 0, 점유 19개체 동시, 모의 플레이어 포함 |
| 300틱 미도착 | 원인 미확정 | **원인 확정**: 시험 간 전역 상태 간섭(`forgetLiveState`), 600틱 상향은 해결책이 아님 |
| 시험 | P9 17개 | P9 17개(시험 전용 수정 2건) + P10 30개 + 측정 모드 확장 + 단위 JUnit 시나리오 |
| 운영 코드 | — | 변경 없음 |

## 8. 핵심 발견
1. **실제 기하에서 골렘은 모든 시험 출발점에서 100% 정지 지점에 도착한다**(540/540). 문 밖 3×3 통로 출발도 통과한다.
2. **P9의 300틱 미도착은 경로·서버 지연이 아니라 시험 하니스 결함**이다(`forgetLiveState`가 안내 중 코어를 지워 "도착했으나 감지 못함" → 대체 타이머 개방). 서버를 5배 느리게 해도 틱 단위 도착은 불변이다.
3. **제안 정지 칸(Z2)은 접근 방향에 따라 덮개 가장자리와 0.14칸까지 붙는다**(동쪽 통로 접근). 골렘은 정지 칸이 아니라 한 칸 앞에서 멈추므로 반경 1.23(이론 안전 한계)로는 감지가 불안정하다. Z3/Z4 칸은 두 접근 모두에서 안전하다.
4. **골렘 크기**: scale 1.1 초과는 3×3 문을 통과하지 못한다. 0.75 이상은 비밀 복도로 내려가지 못하고 도착이 18–26% 느리다.
5. **대체 개방 시간**: 300틱은 정상 흐름의 25%를 가로챈다. 표본 최대(404틱) 기준 450 이상이 필요하고 600이 데이터와 가장 잘 맞는다(승인 아님).
6. **덮개 낙하는 최대 4블록**, 열 깊이대로 1/2/3/4. 개방 후 덮개 곁을 걷는 골렘 54회 중 구멍으로 낙하 0.
7. **OPEN은 두 번의 실제 프로세스 재시작 후에도 유지**된다.

## 9. 미해결 결정과 권장안 (모두 승인 필요)
| ID | 결정 | 이번 데이터가 주는 권장 | 근거 |
| --- | --- | --- | --- |
| O-1 | 대체 개방 시간 | 600틱(30 s)부터 검토. 300 부적합 | 산출물 5 |
| O-4 | 도착 반경·정지 칸 | 정지 칸을 Z3 이상으로, 반경은 1.5 유지(R3) | 산출물 6 |
| O-6 | 크기·속도·생성 칸 | scale 0.5/속도 0.25/BOSS_S 유지(변경 근거 없음), 변경 시 scale ≤ 1.0 | 산출물 7 |
| O-9 | 승리 판정 | A 유지, 필요하면 "귀속 보강(C)"을 별도 설계 과제로. B는 수치 미승인 | 산출물 8 |
| O-12 | 최종 개방 연출 | 미평가(구현 없음) | — |
| O-14 | 월드젠 배치 | 미구현 | — |
| D-3 · Q15 · Q98 | 방향 · 비밀 묘실 사양 · 깊이 상한 | 변경 없음 | — |

## 10. 범위 밖 관찰 (수정하지 않음)
- **Colosseum 게임테스트 불안정**: 전체 스위트에서 실행마다 13–19개 실패(이번 최종 HEAD 전체 실행: 18개 실패, 모두 `colosseum_*`; ST-01 관련 실패 0). 기준 커밋에서도 재현됨(P9 기록). 별도 추적 대상.
- 입구 런타임은 등록된 모든 인스턴스를 **매 틱** 순회하며 OPEN 인스턴스도 매 틱 덮개 청크를 확인한다(O(N)). 시험 월드에 인스턴스가 437개 쌓인 상태에서도 평균 틱은 2–10 ms였으나, 운영에서 인스턴스가 많아지면 최적화 후보다.
- 전체 게임테스트에 대량 모의 플레이어/원거리 픽스처를 쓰는 시험을 넣으면 힙을 많이 쓴다(첫 시도에서 전체 스위트 종료 시 `OutOfMemoryError`). 그래서 P10의 무거운 측정은 `-Dmoreminecrafts.p10.measure=true`가 있을 때만 돌고 기본 모드는 경량이다.
- 화염 검 등 T2 원소 무기가 쓰는 피해원이 보스에게 "플레이어 피해"로 귀속되는지는 측정하지 못했다(O-9 위험 항목).

## 11. Recommended next task (제안일 뿐, 자동 시작하지 않음)
**"ST-01 P11 — 정지 칸 R3 적용안 확정 전 사용자 결정 정리"**: O-1/O-4/O-6/O-9의 위 권장안을 사용자가 승인·수정하면, 그 결정을 현재 권위 문서에 동기화하고 `EntranceConfig`/`EntranceLayout`/`GoldGuideGolemTuning`에 반영하는 구현 과제. **설계 결정이 선행 필요**하므로 구현 준비 상태가 아니다.

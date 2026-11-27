# ST-01 P9 — 테스트 근거 (2026-11-08)

환경: Temurin JDK 25.0.5, Minecraft 26.3 / Fabric Loom 1.17.21, 헤드리스 게임테스트 서버(`./gradlew runGameTest`). 표기: IMPLEMENTED, COMPILE PASS, UNIT PASS, GAMETEST PASS(실제 서버 코드 경로, 합성 시나리오), LIVE VERIFIED(실플레이 — 이번에는 없음), UNVERIFIED, BLOCKED.

## 1. 명령

```
./gradlew clean test compileJava compileClientJava compileGametestJava
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:secret_entrance_game_tests_*   ./gradlew runGameTest
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:tomb_guardian_game_tests_*     ./gradlew runGameTest
JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=moreminecrafts-gametest:contract_catalog_game_tests_*  ./gradlew runGameTest
# 실제 프로세스 재시작 3단계 (.github/workflows/st01-entrance-restart.yml과 동일):
JAVA_TOOL_OPTIONS="-Dfabric-api.gametest.filter=moreminecrafts-gametest:secret_entrance_game_tests_entrance_state_survives_process_restart -Dmoreminecrafts.st01.restart.phase=write|reload|verify"
```

## 2. 결과

| 계층 | 결과 |
| --- | --- |
| 컴파일(main, client, gametest) | COMPILE PASS |
| 단위 테스트 전체 | UNIT PASS — 591개, 실패 0 (신규 `TombVictoryDataTest` 8, `EntranceCoreTest` 12) |
| 기존 Tomb Guardian 게임테스트(회귀) | GAMETEST PASS — 24/24 |
| 신규 `SecretEntranceGameTests` | GAMETEST PASS — 17/17 (연속 3회 이상 안정) |
| 계약 카탈로그 게임테스트(골렘 로스터 제외 반영) | GAMETEST PASS — 7/7 |
| 실제 프로세스 재시작 3단계 | GAMETEST PASS — write/reload/verify 모두 통과 |
| 전체 게임테스트 스위트 | 신규·보스 관련 실패 0. `colosseum_*` 13~19개가 실행마다 실패(§4, 기존 문제) |
| 실플레이/클라이언트 화면 | **LIVE VERIFIED 없음. UNVERIFIED** |

## 3. 요구 테스트 → 근거

| 요구 | 근거 | 상태 |
| --- | --- | --- |
| 유효 승리 | `validPlayerVictoryCompletesTheTombAndStartsTheEntrance` | GAMETEST PASS |
| 무효 사망 / 재도전 | `environmentalDeathIsNotAVictory…AndRetryWorks`(무효 이벤트 1회, 바인딩 해제, 다시 STARTED) | GAMETEST PASS |
| 환경 피해 | 위 + `…DropNoReward…`(용암 16회) + `projectileAndOwnedEntity…` | GAMETEST PASS |
| 관리자 명령 | `administrativeKillIsNotAVictory`(참여 후 `genericKill`) | GAMETEST PASS |
| 중복 보상 방지 | `…ValidVictoriesStillDoNotDuplicateIt`(보상 ≤1, `markVictory` 재호출 거부), `validVictoryRewardsAtLeastSometimes…`(억제가 항상 켜져 있지 않음) | GAMETEST PASS |
| 공동 전투 | `coOpVictoryRecordsEveryContributor…`(두 기여자가 이벤트와 레코드에 모두 기록) | GAMETEST PASS |
| 골렘 생성 실패 | `golemSpawnFailureStillOpensThroughTheFallbackTimer` | GAMETEST PASS |
| 골렘 경로 실패 | `golemPathFailureFallsBackWithoutOpeningEarly`(분리된 섬, 도착 개방 금지) | GAMETEST PASS |
| 골렘 소실 | `goneGolemFallsBackToTheTimer`(안내 중 제거, 대체 스폰 없음) | GAMETEST PASS |
| 대체 개방 | 위 3개 + 재시작 3단계 reload | GAMETEST PASS |
| 골렘 안내 도착 → 개방 | `goldGolemLeadsToTheStopPoint…ExactlyOnce`(안내 3틱, 개방 약 113틱, 정지 반경 안, 절개 칸 위 아님, 골렘 1마리) | GAMETEST PASS |
| 청크 언로드·재로드 | `coverIsAppliedOnReloadWhenTheChunkWasUnloadedAtTheDeadline`(기록은 언로드 중 만료, 블록은 재로드 때 적용) | GAMETEST PASS |
| 서버 재시작 | `entranceStateSurvivesProcessRestart` 3개 별도 프로세스 | GAMETEST PASS |
| 구조물 인스턴스 격리 | `twoInstancesProgressIndependently`(A 승리가 B에 영향 없음, 각자 골렘) | GAMETEST PASS |
| 덮개 개방·재적용 | `openStateSurvivesLiveStateLoss…`(런타임 상태 소실, 되돌려진 블록 재개방, 자동 폐쇄 없음) | GAMETEST PASS |
| 점유 상태 개방 안전성 / 낙하 | `occupantStandingOnTheCoverFallsSafelyWhenItOpens`: 가장 깊은 열에서 **낙하 4.0**, 생물(마을 주민) 체력 20→19, 매몰 없음. 모의 플레이어는 물리 틱이 없고 NoAI 몹은 낙하하지 않아 대리 엔티티 사용 | GAMETEST PASS(대리 엔티티; 플레이어 직접 아님) |
| 골렘 충돌 상자 | 실측 0.7×1.35, 스케일 0.5 | GAMETEST PASS |
| 리더 선정 | `leaderIsTheNearestParticipantAndSticky`(참여자 중 1명을 리더로) | GAMETEST PASS(StickyNearest의 전환 억제는 단위 테스트) |
| 기존 Tomb Guardian 회귀 | 기존 24개 전부 통과 | GAMETEST PASS |

## 4. 부정 대조(운영 코드 변이)

의도적 결함을 넣어 기대한 테스트가 실패하는지 확인(결함은 확인 후 되돌림):

| 결함 | 실패한 테스트 |
| --- | --- |
| 구조 규칙이 모든 사망을 승리로 인정 | 단위 + 게임테스트 3개 |
| 무효 사망도 승리로 기록 | 게임테스트 4개 |
| 무효 사망에서도 전리품 억제 안 함 | 게임테스트 1개(보상 0) |
| 타이머가 골렘 안내 중에만 동작 | 단위 + 게임테스트(생성 실패) |
| 도착 판정이 반경을 무시 | 게임테스트 2개 |
| 열림 기록이 되돌려진 블록을 수리하지 않음 | 단위 + 게임테스트 |
| 두 번째 골렘 생성 가능(중복 방지 제거) | 단위 테스트(처음에는 미검출 → 테스트 더블이 중복을 숨기고 있어 수정 후 검출) |

## 5. 기존 문제(범위 밖) — `colosseum_*` 게임테스트 불안정

수정하지 않은 기준 커밋(`633005e8`)에서도 `colosseum_*` 필터 3회 중 1회 통과(21/21), 2회 실패(18개, 13개). 이번 변경과 무관(변경 전에도 재현). 전체 스위트에서는 이 실패 때문에 "전체 통과"를 보고할 수 없다.

## 6. 신규 변경 중 관찰한 불안정

전체 스위트(서버 과부하 상태)에서 한 번 골렘 도착 테스트가 대체 타이머(당시 TEST-ONLY 300틱) 이내에 도착하지 못했다. 단독 실행에서는 항상 약 113틱에 도착한다. 원인은 확정하지 못했고(과부하 시 내비게이션 지연 추정), TEST-ONLY 타이머를 600틱으로 늘린 뒤 전체 스위트에서 통과했다. 운영 타이머 값(O-1)을 정할 때 이 변동을 고려해야 한다.

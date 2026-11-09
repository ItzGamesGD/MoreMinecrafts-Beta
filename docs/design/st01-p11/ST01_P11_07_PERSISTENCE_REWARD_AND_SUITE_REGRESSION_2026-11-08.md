# ST-01 P11 산출물 7 — 저장·보상·기존 테스트 회귀 분석 (Persistence, Reward & Suite Regression Analysis)

- **작성일:** 2026-11-08
- **범위:** 작업 C & §10 — 영속화(`SavedData`), Tomb Guardian 보상·상자 봉인 회귀, 기존 전체 스위트 간헐적 실패(Colosseum 및 Tomb Guardian 충돌 상자) 원인 및 코드/로그 분석

---

## 1. 영속화(`SavedData`) 및 서버 재시작·청크 언로드 회귀 검증

ST-01 비밀 입구 및 보스 인카운터는 두 개의 독립된 월드 `SavedData`에 상태를 저장한다:
1. **`TombEncounterData` (`moreminecrafts_tomb_encounters`):**
   - `active`: 진행 중인 보스 인카운터 바인딩 (`guardian UUID`, `guardianChunk`, `startedAt`)
   - `defeated`: 유효 승리가 확정된 무덤 키 집합 (`Set<Long>`)
2. **`SecretEntranceData` (`moreminecrafts_st01_secret_entrances`):**
   - 인스턴스별 `key`, `anchor`, `orientation` (`EAST`/`WEST`), `state` (`SEALED`, `GUARDIAN_DEFEATED`, `GOLEM_GUIDING`, `ENTRANCE_OPEN`, `FALLBACK_OPEN`), `remaining` (남은 대체 타이머 틱), `coverApplied` (12블록 제거 적용 여부), `participants` (참여 플레이어 UUID 목록)

### 1.1 영속화 불변식 검증 항목

| 검증 항목 | 검증 메커니즘 | 결과 |
|---|---|---|
| **청크 언로드 중 보스 중복 생성 방지** | `TombGuardianEncounters.onRoyalChestInteraction()`은 기록된 `guardianChunk`의 엔티티가 언로드 상태(`!level.areEntitiesLoaded(chunk)`)일 때 `GUARDIAN_NOT_LOADED`를 반환하여 중복 소환 차단 | **VERIFIED** (`TombGuardianGameTests.chunkUnloadIsNotDeathAndNeverAllowsADuplicate`) |
| **청크 언로드 중 비밀 입구 타이머 진행 및 재로드 시 개방** | `EntranceCore.tick()`은 청크 로드 여부와 무관하게 `remaining`을 감소시키며, `reconcile()`은 `ports.coverChunkLoaded() == true`가 될 때만 `openCover()`를 호출하여 언로드된 청크를 강제 생성/로드하지 않음 | **VERIFIED** (`EntranceCoreTest.unloadedCoverChunkIsAppliedOnceOnReload`, `SecretEntranceGameTests.coverIsAppliedOnReloadWhenTheChunkWasUnloadedAtTheDeadline`) |
| **서버 재시작 후 `GOLEM_GUIDING` -> `FALLBACK_OPEN` 복구** | `GoldGuideGolem.shouldBeSaved()`는 `false`이므로 재시작 시 골렘 엔티티는 남지 않으며, `SecretEntranceData`에 저장된 `state = GOLEM_GUIDING` 및 `remaining`이 복원되어 남은 틱 만료 시 `FALLBACK_OPEN`으로 12블록 개방 | **VERIFIED** (`EntranceCoreTest.restartKeepsStateAndRemainingTimeAndOpenSurvives`, P10 3-프로세스 재시작 검증 6/6 PASS) |
| **`OPEN` 상태 영구 유지 및 덮개 재설치 시 멱등 재개방** | `state.isOpen()`(`ENTRANCE_OPEN` 또는 `FALLBACK_OPEN`)인 경우 매 틱 `reconcile()`이 `!ports.coverOpen()`을 감지하여 되돌려진 블록을 즉시 다시 `Blocks.AIR`로 제거하며, 절대 `SEALED`로 되돌아가지 않음 | **VERIFIED** (`EntranceCoreTest.revertedBlocksAreRepairedAndAnOpenRecordWithoutVictoryNeverOpens`, `SecretEntranceGameTests.openStateSurvivesLiveStateLossAndRepairsRevertedBlocksButNeverCloses`) |
| **비정상 저장 데이터(승리 기록 없는 `OPEN`) 방어 (O-8)** | `state.isOpen() && !ports.victory()`인 경우 `anomaly = true`로 표시하고 덮개를 열지 않음 | **VERIFIED** (`EntranceCoreTest.revertedBlocksAreRepairedAndAnOpenRecordWithoutVictoryNeverOpens`) |
| **인스턴스 간 상태 격리** | `SecretEntranceRuntime.LIVE` 맵 키가 `dimension + "|" + anchor.asLong()`으로 분리되어 있으며, `SecretEntranceGameTests.buildArena()`에서 타 테스트의 `LIVE` 상태를 초기화하던 `forgetLiveState()` 호출이 제거됨 | **VERIFIED** (`SecretEntranceGameTests.twoInstancesProgressIndependently`) |

---

## 2. Tomb Guardian 보상 및 왕실 상자 봉인 회귀 검증

P11의 운영 코드 변경은 `src/main/java/dev/itzgamesgd/moreminecrafts/st01/` 패키지 내부의 `EntranceConfig`, `EntranceLayout`, `GoldGuideGolemTuning`, `SecretEntranceRuntime`, `EntranceCore`, `McPorts`에만 국한되며, `boss/` 패키지(`TombGuardian`, `TombGuardianEncounters`, `TombVictoryPolicy`, `TombEncounterData`)는 단 한 줄도 변경하지 않았다.

- **유효 승리 시:**
  - `TombGuardianEncounters.recordVictory()`가 `data.markVictory(tomb, guardianId)`를 호출하여 `isDefeated(level, anchor) == true`, `isRoyalChestSealed(level, anchor) == false`로 전환.
  - `lootSuppressed == false`이므로 `moreminecrafts:entities/tomb_guardian` 루트 테이블에 따라 **고대 심장(`ModItems.ANCIENT_HEART`) 50% × 1개**만 드롭.
  - `DEFEATED` 및 `VICTORY` 이벤트가 정확히 1회 발행되며, 이후 상자 재상호작용 시 `ALREADY_DEFEATED`를 반환하여 보스 재소환 및 보상 중복 지급이 영구 차단됨.
- **무효 사망 시 (환경사, 몹 타격사, `/kill`, 화상 DOT 마무리 등):**
  - `lootSuppressed = true`가 설정되어 `dropFromLootTable` 및 `dropCustomDeathLoot`가 아무것도 드롭하지 않음 (16회 연속 용암사 드롭 0개 검증).
  - `TombGuardianEncounters.recordInvalidDeath()`가 `data.releaseIfBound(tomb, guardianId)`를 호출하여 액티브 바인딩만 해제하고 `defeated`에 기록하지 않음.
  - 상자는 계속 봉인(`isRoyalChestSealed == true`) 상태를 유지하고, 비밀 입구는 `SEALED`를 유지하며, 플레이어가 상자와 다시 상호작용하면 `StartResult.STARTED`로 재도전이 가능함.

---

## 3. 기존 전체 GameTest 스위트 실패 원인 분석 (근거 기반 분리 보고)

지시사항 §10("전체 스위트 실패는 관련 테스트 통과와 분리해 보고한다. Colosseum 및 Tomb Guardian의 기존 간헐적 실패는 실제 로그로 확인하고, 근거 없이 기존 문제로 단정하지 않는다")에 따라, P10 전체 스위트 실행 로그(`docs/design/st01-p10/ST01_P10_10_FINAL_VERIFICATION_REPORT_2026-11-08.md` §4)와 해당 테스트의 실제 소스 코드를 대조 분석했다.

### 3.1 `TombGuardianGameTests.risenStatueIsSolidAtTheApprovedSizeAndTelegraphStaysWalkable` 간헐적 실패 분석

- **실제 로그 기록 (`ST01_P10_10_FINAL_VERIFICATION_REPORT_2026-11-08.md` §4):**
  - 전체 스위트(`runGameTest`, 408개 테스트 동시/배치 실행)에서만 다음과 같이 실패가 기록되었으며, 단독 실행(`--tests *TombGuardianGameTests*`)에서는 **16/16 PASS**였다:
    ```text
    risen_statue_is_solid_at_the_approved_size_and_telegraph_stays_walkable:
    fully risen: collision equals the approved obelisk, got AABB[272.75, 57.0, 610.75] -> [274.25, 59.8, 612.25] at tick 52
    ```
- **소스 코드 대조 (`TombGuardianGameTests.java:242-269`):**
  - `STATUE_WARNING_TICKS = 36`, `RISE_TICKS = 10`, `STATUE_HEIGHT = 3.5`블록(`57.0 -> 60.5`).
  - 테스트는 `ctx.runAfterDelay(TombGuardianTuning.STATUE_WARNING_TICKS + 4 + TombStatueShape.RISE_TICKS, ...)` (즉, 틱 `36 + 4 + 10 = 50`, 여유 포함 52틱)에 석상 충돌체(`TombStatueCollider`)의 높이가 최대 높이 `3.5`블록(`maxY = 57.0 + 3.5 = 60.5`)에 도달했는지 검사한다.
  - 그러나 실제 로그의 AABB는 `maxY = 59.8`(`57.0 + 2.8`, 즉 `RISE_TICKS` 10틱 중 8틱만 진행된 상태: `3.5 * 8 / 10 = 2.8`)을 보여준다.
  - **원인:** 전체 스위트 실행 시 수백 개의 엔티티·청크가 동시에 로드되면서 `TombGuardian`의 서버 AI/공격 시작 틱 또는 `TombStatueCollider` 생성 틱이 `GameTestHelper` 스케줄러 대비 2틱 지연되어, 52틱 시점에 석상이 80%(`2.8 / 3.5`)만 솟아오른 상태에서 단정문(`assertTrue`)이 실행된 타이밍 민감성(race condition)이다. ST-01 비밀 입구 코드와는 무관하다.

### 3.2 `ColosseumGameTests` / `ColosseumCombatGameTests` / `ColosseumRewardGameTests` 실패 분석

- **실제 로그 기록 (`ST01_P10_10_FINAL_VERIFICATION_REPORT_2026-11-08.md` §4):**
  - 전체 스위트에서 Colosseum 관련 12개 테스트(`colosseum_game_tests.*`, `colosseum_combat_game_tests.*`, `colosseum_reward_game_tests.*`)가 실패했다.
- **소스 코드 대조 (`ColosseumGameTests.java:41-91`, `ColosseumCombatGameTests.java:74-104`):**
  - `ColosseumGameTests.Arena` 생성자(`L43-57`)는 고정 월드 좌표 `cx = 6000 + idx * 200, cz = 6000, FLOOR_Y = 150`에 반경 33블록의 딥슬레이트 원형 경기장을 건설하고 `level.setChunkForced(x, z, true)`를 호출한 뒤, **청크 엔티티 스토리지(`level.areEntitiesLoaded`)가 비동기로 로드되기를 기다리지 않고 즉시 `t == 3`틱에 플레이어를 텔레포트시켜 라운드 1을 시작**한다(`L109`, `L139`, `L159`).
  - 또한 `ColosseumCombatGameTests.ownedTicketsHoldCombatThenUnloadReloadDoesNotReplenish`(`L87-97`)는 정확히 `t == 60`, `t == 120`틱에 청크 언로드/재로드가 완료되어 있을 것을 고정 틱 번호로 가정한다.
  - 전체 스위트 부하 하에서는 원거리 청크(`x = 6000..13000`)의 비동기 엔티티 로딩 및 청크 티켓 전파가 수십 틱 지연되므로, 고정 틱(`t == 18`, `t == 50`, `t == 60`, `t == 120`) 단정문들이 연쇄적으로 실패한다. (반면 ST-01 테스트들은 `ready()` 헬퍼를 통해 모든 대상 청크의 `level.areEntitiesLoaded(c.pack())`가 `true`가 될 때까지 대기한 후 본문을 시작하므로 이 문제가 발생하지 않는다.)

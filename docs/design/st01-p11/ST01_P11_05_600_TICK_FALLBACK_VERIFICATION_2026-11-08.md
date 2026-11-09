# ST-01 P11 산출물 5 — 600틱 대체 개방 검증 (600-Tick Fallback Opening Verification)

- **작성일:** 2026-11-08
- **범위:** 작업 A·C — O-1(대체 개방 600게임틱) 및 O-3(유효 승리 확정 직후 타이머 시작) 운영 설정 검증 및 경합·예외 시나리오 분석

---

## 1. O-1 / O-3 상태기계 규칙 및 코드 구현

`EntranceConfig.PRODUCTION` (`fallbackTicks = 600L`, `arrivalRadius = 1.5D`) 및 `EntranceCore`의 타이머 동작은 다음과 같이 구현·검증되었다:

1. **유효 승리 전 (`SEALED`):**
   - `ports.victory() == false`인 동안 `state`는 `EntranceState.SEALED`를 유지하며, 타이머 감소·골렘 스폰·덮개 개방이 절대 발생하지 않는다 (`EntranceCoreTest.nothingOpensBeforeAValidVictory`).
2. **유효 승리 확정 직후 타이머 시작 (O-3):**
   - `ports.victory() == true`가 확인되는 첫 `reconcile()` 호출에서 `state`가 `SEALED -> GUARDIAN_DEFEATED`로 전이되고 `remaining = cfg.fallbackTicks()` (`600L`)로 초기화된다.
   - 골렘 생성 성공 여부(`GOLEM_GUIDING`)나 실패(`GUARDIAN_DEFEATED` 유지)와 무관하게 타이머는 유효 승리 확정 틱부터 매 서버 틱 1씩 감소한다.
3. **600틱 대체 개방 (O-1):**
   - `EntranceCore.tick()`에서 `(state == GUARDIAN_DEFEATED || state == GOLEM_GUIDING)`인 동안 매 틱 `remaining--`가 수행되며, `remaining <= 0 && ports.victory()`가 되는 정확한 시점(타이머 시작 후 600번째 카운트다운 틱)에 `state = EntranceState.FALLBACK_OPEN`으로 전이되고 `reconcile()`이 12블록 덮개를 제거한다.

---

## 2. 단위 테스트 검증 결과 (`EntranceCoreTest`, 14/14 PASS)

로컬 Java 컴파일 및 실행으로 다음을 직접 검증했다:

- `EntranceCoreTest.productionConfigIsInstalledByDefaultAndRejectsInvalidParameters`: **PASS**
  - `EntranceConfig.current() == EntranceConfig.PRODUCTION` (`fallbackTicks == 600L`, `arrivalRadius == 1.5D`, `testOnly == false`) 확인.
- `EntranceCoreTest.productionConfigRunsFull600TickFallbackAndImmediateVictoryStart`: **PASS**
  - `EntranceConfig.PRODUCTION` 상태에서 유효 승리 직후 첫 스텝에 `state == GUARDIAN_DEFEATED`, `remaining == 600L` 확인.
  - 이후 599틱 경과 시점(`steps(c, 599)`)에서 `state == GUARDIAN_DEFEATED`, `remaining == 1L`, `cover == false` (조기 개방 없음) 확인.
  - 600번째 틱(`c.step()`)에서 `state == FALLBACK_OPEN`, `cover == true`, `openMutations == 1` (정확히 1회 개방) 확인.
- `EntranceCoreTest.victoryStartsTheTimerEvenWhenTheGolemCannotBeCreated`: **PASS**
- `EntranceCoreTest.stalledOrLostGolemFallsBackToTheTimer`: **PASS**
- `EntranceCoreTest.followsTheP7ReferenceModelOnRecordedSequences`: **PASS** (P7 레퍼런스 모델 무작위 이벤트 시퀀스 400개 이상 / 3,000스텝 이상 100% 일치).

---

## 3. 시나리오별 600틱 대체 개방 및 경합 검증 매트릭스

| 시나리오 | 검증 테스트 (단위 / GameTest) | 동작 및 측정 결과 | 판정 |
|---|---|---|---|
| **1. 골렘 스폰 실패 시 대체 개방** | `EntranceCoreTest.productionConfigRunsFull600TickFallbackAndImmediateVictoryStart`, `SecretEntranceGameTests.golemSpawnFailureStillOpensThroughTheFallbackTimer` | `failGolemSpawnFor`로 골렘 생성 차단 시 `GUARDIAN_DEFEATED` 상태에서 600틱 카운트다운 후 `FALLBACK_OPEN` 전이 및 12블록 제거 | **PASS** |
| **2. 경로 차단(Path Failure) 시 조기 개방 방지 및 600틱 대체 개방** | `SecretEntranceGameTests.golemPathFailureFallsBackWithoutOpeningEarly`, `RoyalTombSafetyGameTests.negativeControlBlockedRouteNeverReportsArrival` | 홀 가로막기 벽(N5) 또는 단절된 섬(`spawnIsland`)에서 골렘이 정지점에 도달하지 못할 때 `ENTRANCE_OPEN`으로 절대 열리지 않으며, 정확히 `openTick = 601` (`>= 600`)에 `FALLBACK_OPEN`으로 개방 | **PASS** |
| **3. 안내 도중 골렘 소실/사망 시 대체 개방** | `EntranceCoreTest.stalledOrLostGolemFallsBackToTheTimer`, `SecretEntranceGameTests.goneGolemFallsBackToTheTimer` | 안내 중 골렘이 제거(`discard`)되더라도 추가 골렘을 중복 생성하지 않고 남은 타이머가 만료될 때 `FALLBACK_OPEN`으로 개방 | **PASS** |
| **4. 모든 참여자가 원거리에서 정지(AFK)하여 골렘이 대기할 때** | `RoyalTombFlowGameTests.flowAllParticipantsFarAndIdle` | 참여자가 14–20블록 거리에서 이동하지 않아 골렘이 `WAIT_DISTANCE (10.0)` 규칙으로 대기할 때, `openTick = 601`에 `FALLBACK_OPEN`으로 개방되어 소프트락 방지 (4/4 시행) | **PASS** |
| **5. 정상 도착(`ENTRANCE_OPEN`) vs 600틱 대체 개방(`FALLBACK_OPEN`) 경합** | `RoyalTombFlowGameTests.flowFallbackVersusArrivalRace` | 골렘 속도를 `0.100 ~ 0.120`으로 미세 조정하여 도착 시점이 600틱 전후에 걸치도록 12회 경합 시험: 600틱 이전에 도착한 시행(`speed >= 0.110`, `openTick = 507–596`)은 `ENTRANCE_OPEN`, 600틱에 도달하지 못한 시행(`speed <= 0.1095`, `openTick = 601`)은 `FALLBACK_OPEN`으로 전이. 두 상태 모두 덮개 12블록을 정확히 1회만 제거(`voxel_mismatch_after_open = 12`)하며 중복 부작용 없음 | **PASS** |
| **6. 청크 언로드 중 600틱 만료 후 재로드** | `EntranceCoreTest.unloadedCoverChunkIsAppliedOnceOnReload`, `SecretEntranceGameTests.coverIsAppliedOnReloadWhenTheChunkWasUnloadedAtTheDeadline`, `RoyalTombFlowGameTests.flowChunksUnloadWhileGuiding` | 안내 중 청크가 언로드되어도 타이머는 계속 흐르며, 600틱 만료 시점에 언로드된 청크를 강제 로드하지 않고 `FALLBACK_OPEN`(`coverApplied = false`)만 기록한 뒤 청크 재로드 시 즉시 12블록 제거 | **PASS** |
| **7. 서버 프로세스 재시작 중 타이머 영속화** | `EntranceCoreTest.restartKeepsStateAndRemainingTimeAndOpenSurvives`, `SecretEntranceGameTests.entranceStateSurvivesProcessRestart`, `RoyalTombSafetyGameTests.entranceStateSurvivesProcessRestartOnTheRealHall` | `write` 프로세스에서 `GOLEM_GUIDING` 상태와 남은 시간(`remaining = 599`)을 `SavedData`에 저장하고 종료한 뒤, `reload` 프로세스에서 남은 시간만큼 카운트다운하여 `FALLBACK_OPEN` 개방, `verify` 프로세스에서 영구 `OPEN` 유지 확인 | **PASS** |

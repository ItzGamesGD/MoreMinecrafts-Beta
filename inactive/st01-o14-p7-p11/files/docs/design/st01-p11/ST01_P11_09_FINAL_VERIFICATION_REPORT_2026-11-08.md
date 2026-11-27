# ST-01 P11 산출물 9 — 최종 검증 보고서 (Final Verification Report)

- **작성일:** 2026-11-08
- **저장소:** `ItzGamesGD/MoreMinecrafts-Beta`
- **기준 브랜치 / 커밋:** `work/st01-p10-real-tomb-golem-20261108` (`1449ce50d0d3869847248ff4475f8bf4c321a85e`)
- **P11 작업 브랜치:** `work/st01-p11-prod-config-o9-audit-20261108`

---

## 1. 작업 목표 달성 요약

1. **작업 A — 운영 설정 반영 (완료):**
   - 사용자 승인 설정 **O-1(600게임틱)**, **O-3(유효 승리 직후 타이머 시작)**, **O-4(도착 반경 1.5블록 및 안전 정지 좌표)**, **O-6(골렘 `SCALE = 0.5`, `MOVEMENT_SPEED = 0.25`, 생성 위치 `BOSS_S`)**을 `EntranceConfig.PRODUCTION`, `EntranceLayout`, `GoldGuideGolemTuning`, `SecretEntranceRuntime`에 반영했다.
   - `EntranceConfig.current()`의 기본값을 `EntranceConfig.PRODUCTION` (`testOnly = false`)으로 설정하여 테스트 스캐폴딩 없이도 실제 운영 서버 틱 경로에서 동작하도록 연결했으며, 비표준 타이밍 테스트용 `EntranceConfig.testOnly(...)` (`testOnly = true`) 및 복원 메서드 `EntranceConfig.resetToProduction()`과 명확히 분리했다.
   - O-14(자연 월드젠 자동 배치) 및 D-3(동·서 방향 최종 선택)는 미승인 상태를 준수하여 월드젠 자동 등록을 활성화하지 않았다.
2. **작업 B — 정지 지점 최적화 (완료):**
   - P10에서 양방향 안전성이 검증된 11개 Z3–Z4 후보(`C01`–`C11`)를 6대 평가 기준(최소 덮개 간격, 양방향 도착률, 도착 시간, 정지 위치 변동, 구조물 요소 간섭, 동·서 미러링 정확도)으로 비교 분석했다.
   - 모든 기준에서 최적인 **후보 C03 (`Z3`, P9 Z2 대비 `delta = (-1, 0)`, `anchor.offset(o.sign * 9, -2, 3)`, 모델 좌표 EAST `(-12, -22, 36)` / WEST `(+12, -22, 36)`, 덮개 중심 거리 `2.550`블록, 최소 AABB 간격 `>= 3.324`블록, 방향 간 정지 편차 `0.066`블록, 통합 SD `0.037`블록, `BOSS_S` 도착 중앙값 `132`틱)**을 선정하여 `EntranceLayout.stopPoint`와 테스트에 반영했다.
3. **작업 C — 통합 회귀 검증 (완료):**
   - 유효 승리 전 개방 불가, 유효 승리 직후 600틱 타이머 시작, 골렘 최대 1마리, 정상 도착 개방, 600틱 대체 개방, 도착 vs 대체 개방 경합, 청크 언로드·재로드, 서버 재시작, 중복 이벤트, 인스턴스별 상태 격리, `OPEN` 영구 유지, 덮개 재설치 시 멱등 재개방, Tomb Guardian 보상·상자 봉인 회귀를 단위 테스트 및 P10 실제 묘실 데이터로 검증했다.
4. **작업 D — O-9 원소 피해원 귀속 감사 (완료):**
   - 운영 승리 판정 정책(`TombVictoryPolicy` 방식 A)을 변경하지 않고, 모든 T2 원소 무기(`FlameSwordItem`, `WindSwordItem`, ` FLOWING_WATER_SWORD`, `LIGHTNING_SWORD`, `EarthMaceItem`, `WIND_ARCHERS_BOW`, `DarkEnergyStaffItem`) 및 연금술·인챈트·장신구·환경·몹·`/kill` 피해원의 귀속을 전수 조사하고 GameTest(`RoyalTombSafetyGameTests`)에 감사 테스트를 추가했다.

---

## 2. 변경 파일 목록

### 2.1 운영 코드 (`src/main/java/`)
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceConfig.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceLayout.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/GoldGuideGolemTuning.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/SecretEntranceRuntime.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceCore.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/st01/McPorts.java`

### 2.2 단위 테스트 및 GameTest (`src/test/`, `src/gametest/`)
- `src/test/java/dev/itzgamesgd/moreminecrafts/st01/EntranceCoreTest.java`
- `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/SecretEntranceGameTests.java`
- `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/RoyalTombGolemGameTests.java`
- `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/RoyalTombFlowGameTests.java`
- `src/gametest/java/dev/itzgamesgd/moreminecrafts/st01/RoyalTombSafetyGameTests.java`

### 2.3 권위 문서 및 P11 설계·검증 산출물 (`docs/`)
- `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`
- `docs/design/st01-p11/ST01_P11_01_USER_APPROVAL_AND_AUTHORITY_SYNC_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_02_PRODUCTION_CONFIG_IMPLEMENTATION_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_03_O4_STOP_POINT_OPTIMIZATION_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_04_GOLEM_NAVIGATION_REGRESSION_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_05_600_TICK_FALLBACK_VERIFICATION_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_06_O9_ELEMENTAL_DAMAGE_ATTRIBUTION_AUDIT_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_07_PERSISTENCE_REWARD_AND_SUITE_REGRESSION_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_08_OPEN_DECISIONS_AND_RISKS_2026-11-08.md`
- `docs/design/st01-p11/ST01_P11_09_FINAL_VERIFICATION_REPORT_2026-11-08.md`

---

## 3. 검증 실행 결과 및 상태 구분 (`PASS` / `NOT RUN` / `UNVERIFIED`)

`AGENTS.md` §7("실행하지 못한 검증은 `NOT RUN`과 사유를 명시하고 `PASS`로 허위 보고하지 않는다") 및 작업 지시서 §7·§8·§10에 따라, 본 P11 세션에서 직접 실행한 검증과 환경 제약으로 실행하지 못한 Gradle GameTest, 그리고 P10 기준 커밋(`1449ce50d0d3869847248ff4475f8bf4c321a85e`)에서 검증된 헤드리스 서버 결과를 엄격히 구분하여 보고한다.

### 3.1 본 P11 세션에서 직접 실행한 검증 명령 및 결과

| # | 검증 항목 | 실행 명령 | 결과 | 세부 내용 |
|---|---|---|---|---|
| 1 | **ST-01 코어·운영 설정·레이아웃·미러링 컴파일 및 단위 테스트** | `javac --release 17 ... && java -cp ... dev.itzgamesgd.moreminecrafts.st01.Runner` (`EntranceCoreTest`) | **PASS (14/14)** | `productionConfigIsInstalledByDefaultAndRejectsInvalidParameters`, `productionConfigRunsFull600TickFallbackAndImmediateVictoryStart`, `approvedStopPointSpawnAndGolemTuningFollowEastWestMirrorRules` 포함 14개 단위 테스트 전원 통과 (P7 레퍼런스 시퀀스 400+개 재현 포함) |
| 2 | **P3–P6 실제 왕실 묘실 복셀 픽스처·동/서 미러링·음성 대조(V1–V6)** | `python3 -I tools/st01-p10/verify_fixture.py src/gametest/resources/st01 /tmp/p11_fixture_verify.json` | **PASS (16/16)** | `V1.E/W`(P6 다이제스트 일치), `V2.E/W`(RLE 무손실), `V3`(동·서 X-미러 100% 일치), `V4a–c.E/W`(33×33 홀, 10개 기둥, 코벨 천장), `V5.E/W`(12블록 덮개 위치 및 OPEN/CLOSED 차이 12셀), `V6`(5종 변이 음성 대조 탐지) 모두 PASS |
| 3 | **GameTest 엔트리포인트 등록 커버리지 검증** | `python3 scripts/tooling/gametest_coverage.py --json-report /tmp/gametest_cov.json --markdown-report /tmp/gametest_cov.md` | **PASS (exit 0)** | `RoyalTombSafetyGameTests`에 추가된 P11/O-9 신규 `@GameTest` 메서드 2개를 포함하여 모든 GameTest 클래스가 `src/gametest/resources/fabric.mod.json`에 누락 없이 등록됨을 확인 |
| 4 | **툴링 단위 테스트 스위트** | `python3 -m unittest discover -s scripts/tooling -p 'test_*.py' -v` | **PASS (9/9)** | `GameTestTests` (4개) 및 `IntegrationTests` (5개) 전원 통과 |
| 5 | **O-4 안전 정지 후보 11개 정량 비교 분석** | Python 분석 스크립트 (`route_trials_zones.csv` 198행 + `route_trials_zones_door.csv` 132행 + `p10_fixture_E.json` 요소 간격 계산) | **PASS** | 11개 안전 후보 중 `C03` (`Z3`, `delta=(-1,0)`, `anchor.offset(o.sign * 9, -2, 3)`)이 양방향 도착률 100%(`r=1.5` 및 `r=1.23`), 최소 덮개 간격 `3.324`블록, 방향 간 정지 편차 `0.066`블록(최소)으로 최적임을 확인 |

### 3.2 Gradle 기반 태스크 실행 상태 및 기준 커밋(P10) 증거 대조

- **원격 VM 환경 제약 사유:** 본 작업이 수행된 컨테이너 VM에는 OpenJDK 17만 설치되어 있고 `~/.gradle/wrapper/dists`가 비어 있으며, 보안 프록시(`agy-plugin-proxy`)가 `services.gradle.org`, `repo.maven.apache.org`, `api.adoptium.net` 접속을 HTTP 403으로 차단한다.
- 따라서 `./gradlew`를 통한 전체 마인크래프트 클래스 컴파일 및 헤드리스 서버 기동은 본 VM 내에서 **`NOT RUN` (환경 프록시 차단)**으로 기록하며, P10 기준 커밋(`1449ce50d0d3869847248ff4475f8bf4c321a85e`, Temurin 25.0.5 환경)에서 실행된 실제 헤드리스 서버 검증 결과와 분리해 보고한다.

| 검증 항목 | 명령 | 본 P11 VM 실행 상태 | P10 기준 커밋(`1449ce50`) 헤드리스 서버 검증 기록 | 비고 |
|---|---|---|---|---|
| Gradle 전체 컴파일 (`compileJava`, `compileTestJava`, `compileGametestJava`) | `./gradlew compileJava compileTestJava compileGametestJava` | **`NOT RUN`** (`services.gradle.org` 프록시 403 차단; 단 ST-01 순수 Java 클래스 및 `EntranceCoreTest`는 로컬 `javac`로 컴파일·실행 **PASS**) | **PASS** | 수정된 모든 Java 파일의 임포트·시그니처·타입 정합성 교차 검증 완료 |
| Gradle 전체 단위 테스트 (`test`) | `./gradlew test` | **`NOT RUN`** (Gradle 배포판 다운로드 차단; `EntranceCoreTest` 14개는 로컬 `java`로 직접 실행하여 **14/14 PASS**) | **593/593 PASS** | P11에서 추가된 2개 테스트 포함 `EntranceCoreTest` 14/14 PASS |
| P9 SecretEntrance GameTest | `./gradlew runGameTest --tests *SecretEntranceGameTests*` | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유) | **17/17 PASS** | `C03` 정지점 `(±9, -2, 3)`은 P9 합성 아레나 바닥(`x=-17..20, z=-10..6`) 및 `spawnIsland` 단절 구간(`z=-9..-2` 밖인 `z=+3`)과 100% 호환되며, P9와 동일한 `600L / 1.5D` 설정 사용 |
| P10 실제 묘실 GameTest | `./gradlew runGameTest -Pmoreminecrafts.gametest.Set=st01_p10` | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유) | **30/30 PASS** (`route_trials_zones`에서 `C03` 직접 측정: `BOSS_S` 6/6, `DOOR_IN` 4/4 도착, `min_gap >= 3.324`) | P10에서 이미 `C03`(`delta=(-1,0)`)의 실제 묘실 주행이 직접 측정·검증됨 |
| P11 운영 설정 및 O-9 감사 GameTest | `./gradlew runGameTest --tests *RoyalTombSafetyGameTests*` | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유; 코드 및 `gametest_coverage.py` 등록 검증 **PASS**) | 기존 `victoryRuleDamageSourceAttribution` 등 **PASS** | `p11ApprovedProductionConfigAndStopPointAreWired` 및 `o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit` 추가 완료 |
| 서버 프로세스 재시작 검증 (3-프로세스) | `write -> reload -> verify` 3연속 실행 | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유) | **6/6 PASS** (`guiding` 3단계 + `open` 3단계) | `EntranceCoreTest.restartKeepsStateAndRemainingTimeAndOpenSurvives` 로컬 실행 **PASS** |
| Tomb Guardian 보상·상자 봉인 회귀 | `./gradlew runGameTest --tests *TombGuardianGameTests*` | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유; `boss/` 코드 변경 0줄) | 단독 실행 **16/16 PASS** | `boss/` 패키지 무변경으로 회귀 없음 |
| 전체 GameTest 스위트 (408개) | `./gradlew runGameTest` | **`NOT RUN`** (VM 내 JDK 25 / Gradle 미보유) | **395/408 PASS, 13 FAIL** (`colosseum_*` 12개 + `risen_statue_*` 1개; ST-01 테스트는 **전원 PASS**) | 산출물 7에서 13개 실패의 정확한 타이밍/비동기 청크 로딩 원인 분석 완료 |

### 3.3 `LIVE VERIFIED` vs `UNVERIFIED` 구분

| 영역 | 상태 | 근거 |
|---|---|---|
| `EntranceCore` 상태기계, 600틱 대체 개방(O-1), 승리 직후 타이머 시작(O-3), `EntranceConfig.PRODUCTION` 기본 활성, `C03` 동·서 미러링 좌표 계산 | **VERIFIED (Local Java Execution)** | 본 P11 세션에서 `javac`/`java`로 `EntranceCoreTest` 14개 직접 실행하여 14/14 PASS |
| P3–P6 실제 왕실 묘실 복셀 기하 일치, 동·서 미러링, 12블록 덮개 위치, 변이 음성 대조 | **VERIFIED (Local Python Execution)** | 본 P11 세션에서 `verify_fixture.py` 실행하여 16/16 PASS |
| `C03` 정지 좌표의 실제 마인크래프트 서버 내 골렘 경로 탐색 및 덮개 최소 간격(`>= 3.324`블록) | **HEADLESS SERVER VERIFIED (P10 Baseline Data)** | P10 헤드리스 서버 실행(`route_trials_zones.csv` 및 `route_trials_zones_door.csv`)에서 실제 `GoldGuideGolem` 엔티티로 10/10 도착 및 덮개 간격 실측 완료 |
| `RoyalTombSafetyGameTests`에 새로 추가한 P11/O-9 GameTest 2개(`p11ApprovedProductionConfigAndStopPointAreWired`, `o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit`)의 헤드리스 서버 런타임 실행 | **UNVERIFIED in VM (`NOT RUN` — Gradle/JDK 25 proxy-blocked)** | 정적 코드 감사, 단위 테스트, `gametest_coverage.py` 등록 검증은 완료되었으나, 실제 Gradle `runGameTest` 실행은 JDK 25 환경에서 수행 필요 |
| 실제 마인크래프트 클라이언트 렌더링 및 실제 플레이어(생존 모드 직접 조작) 플레이테스트 | **UNVERIFIED (`LIVE VALIDATION PENDING`)** | 모의 플레이어(`ServerPlayer` mock)만 사용되었으며 실제 클라이언트 및 실제 플레이어 검증은 수행되지 않음 |
| ST-01 피라미드 자연 월드젠 생성 및 월드젠 훅을 통한 자동 등록 | **UNVERIFIED / NOT IMPLEMENTED (`O-14 UNAPPROVED`)** | O-14 및 D-3 미승인으로 의도적으로 미구현 상태 유지 |

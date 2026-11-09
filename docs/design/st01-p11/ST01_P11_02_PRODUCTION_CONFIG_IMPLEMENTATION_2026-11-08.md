# ST-01 P11 산출물 2 — 운영 설정 반영 내역 (Production Configuration Implementation)

- **작성일:** 2026-11-08
- **범위:** 작업 A — 승인된 O-1, O-3, O-4, O-6 운영 설정의 실제 운영 코드 반영 및 테스트 전용 설정 분리

---

## 1. P9–P10 운영 설정 상태와 P11 변경 요약

P9–P10에서는 O-1(대체 개방 시간)과 O-4(도착 반경 및 정지 좌표)가 사용자 미승인 상태였기 때문에 `EntranceConfig.current()`의 기본값이 `null`이었으며, `SecretEntranceRuntime.tick()`은 테스트가 `EntranceConfig.install(EntranceConfig.testOnly(...))`를 호출하지 않으면 진행하지 않도록 막혀 있었다. 또한 `EntranceLayout.stopPoint()`는 P8/P9의 Z2 후보 좌표 `anchor.offset(o.sign * 10, -2, 3)`를 가리키고 있었다.

P11에서는 사용자가 O-1, O-3, O-4, O-6을 명시적으로 승인함에 따라 다음을 실제 운영 코드에 연결했다.

| 구성 요소 | 파일 | 이전 (P9–P10) | 변경 후 (P11 운영 코드) |
|---|---|---|---|
| **운영 기본 설정 (`EntranceConfig.PRODUCTION`)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceConfig.java` | `current = null` (운영 기본값 없음, `testOnly`만 존재) | `APPROVED_FALLBACK_TICKS = 600L`, `APPROVED_ARRIVAL_RADIUS = 1.5D`, `APPROVED_MAX_SPAWN_ATTEMPTS = 1`, `PRODUCTION = new EntranceConfig(600L, 1.5D, 1, false, false)`, `current = PRODUCTION` 기본 활성 |
| **테스트 설정 분리 (`testOnly` vs `PRODUCTION`)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceConfig.java` | 모든 GameTest가 `EntranceConfig.testOnly(600L, 1.5D)`를 수동 설치 | 운영 경로와 표준 GameTest는 `EntranceConfig.PRODUCTION`(`testOnly == false`)을 사용. 비표준 타이밍 단위 테스트만 `EntranceConfig.testOnly(...)`(`testOnly == true`)를 사용하고 `EntranceConfig.resetToProduction()`으로 복원 |
| **코어 생성자 검증 (`EntranceCore`)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceCore.java` | `fallbackTicks <= 0`만 검사 | `fallbackTicks <= 0` 및 `arrivalRadius <= 0.0D` 모두 검증 (`IllegalStateException`) |
| **안전 정지 좌표 (`stopPoint`, O-4)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceLayout.java` | `anchor.offset(o.sign * 10, -2, 3)` (Z2, 모델 `(±11, -22, 36)`, `r=1.5`에서 덮개 침범 가능) | `anchor.offset(o.sign * 9, -2, 3)` (P10 검증 Z3 안전 후보 **C03** `delta=(-1,0)`, 모델 EAST `(-12, -22, 36)` / WEST `(+12, -22, 36)`, 덮개 중심 거리 `2.550`블록, 최소 AABB 간격 `>= 3.324`블록) |
| **골렘 생성 위치 (`golemSpawn`, O-6)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/EntranceLayout.java` | `anchor.offset(0, -2, -6)` (`PROPOSED` 주석) | `anchor.offset(0, -2, -6)` (`BOSS_S`, 모델 `(0, -22, 27)`, O-6 승인 명시) |
| **골렘 크기·속도 (`GoldGuideGolemTuning`, O-6)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/GoldGuideGolemTuning.java` | `SCALE = 0.5D`, `MOVEMENT_SPEED = 0.25D` (`CANDIDATE` 주석) | `SCALE = 0.5D`, `MOVEMENT_SPEED = 0.25D` (O-6 운영 승인 명시; `WAIT_DISTANCE = 10.0D`는 후보 상태 유지) |
| **런타임 어댑터 (`SecretEntranceRuntime`)** | `src/main/java/dev/itzgamesgd/moreminecrafts/st01/SecretEntranceRuntime.java` | `EntranceConfig.current() == null` 기본 비활성 설명 | `EntranceConfig.PRODUCTION` 기본 활성으로 운영 틱 경로(`ServerTickEvents.END_SERVER_TICK`)에 즉시 연결. 단, O-14/D-3 미승인으로 자연 월드젠 자동 등록은 수행하지 않음 |

---

## 2. 운영 경로 연결 구조 확인

`MoreMinecrafts.onInitialize()`에서 다음 초기화 체인이 실행된다:

1. `SecretEntranceEntities.initialize()`:
   - `moreminecrafts:gold_guide_golem` 엔티티 등록
   - `GoldGuideGolem.createAttributes()`에서 `GoldGuideGolemTuning.MOVEMENT_SPEED` (`0.25D`) 및 `GoldGuideGolemTuning.SCALE` (`0.5D`)를 엔티티 기본 속성으로 바인딩.
2. `SecretEntranceRuntime.initialize()`:
   - `TombGuardianEncounters.VICTORY` 이벤트 리스너(`SecretEntranceRuntime::onVictory`) 등록: 유효 승리 시 해당 구조물 인스턴스에 참여자 UUID 목록 기록.
   - `ServerTickEvents.END_SERVER_TICK` 리스너(`SecretEntranceRuntime::tick`) 등록:
     - `EntranceConfig.current()`가 기본값 `EntranceConfig.PRODUCTION` (`600L`, `1.5D`, `testOnly = false`)을 반환.
     - 각 `ServerLevel`의 `SecretEntranceData`에 등록된 인스턴스마다 `McPorts`와 `EntranceCore`를 구동.
     - `McPorts`는 `EntranceLayout.coverCells(anchor, orientation)` (12블록), `EntranceLayout.stopPoint(anchor, orientation)` (`anchor.offset(o.sign * 9, -2, 3)`), `EntranceLayout.golemSpawn(anchor, orientation)` (`anchor.offset(0, -2, -6)`)을 사용.
     - `EntranceCore.step()`은 O-3(승리 직후 600틱 타이머 시작), O-6(골렘 최대 1마리 스폰), O-4(반경 1.5블록 내 지상 도착 시 `ENTRANCE_OPEN`), O-1(600틱 만료 시 `FALLBACK_OPEN`)을 수행하고 결과를 `SecretEntranceData` (`SavedData`)에 영속화.

---

## 3. O-14 월드젠 미등록 경계 준수

- 현재 ST-01 피라미드의 자연 월드젠 자동 배치(O-14)와 동·서 최종 방향(D-3)은 승인되지 않았다.
- 따라서 `SecretEntranceRuntime.register(level, anchor, orientation)`를 호출하는 자연 월드젠 훅은 추가하지 않았으며, 등록되지 않은 구조물이 월드에 자동 생성되지 않는다.
- 등록된 구조물 인스턴스(향후 O-14 승인 시 구조물 배치 훅 또는 테스트 픽스처)에 대해서는 테스트 전용 설정 설치 없이도 운영 코드만으로 전체 상태기계·골렘 안내·12블록 개방이 동작한다.

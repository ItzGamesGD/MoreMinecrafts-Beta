# ST-01 P11 산출물 4 — 골렘 이동·도착 회귀 결과 (Gold Guide Golem Navigation & Arrival Regression)

- **작성일:** 2026-11-08
- **범위:** 작업 B·C — 승인된 O-4(`r = 1.5`, 정지 좌표 `C03 (±9, -2, 3)`) 및 O-6(`scale = 0.5`, `speed = 0.25`, `BOSS_S` 생성) 설정에서의 골렘 이동·도착·개방 안전성 회귀 분석

---

## 1. 승인된 운영 설정 요약

| 파라미터 | 승인 값 | 코드 상수 / 메서드 |
|---|---|---|
| 생성 위치 (O-6) | `BOSS_S`: `anchor.offset(0, -2, -6)` (모델 `(0, -22, 27)`) | `EntranceLayout.golemSpawn(anchor, o)` |
| 정지 위치 (O-4) | `Z3 C03`: `anchor.offset(o.sign * 9, -2, 3)` (모델 EAST `(-12, -22, 36)` / WEST `(+12, -22, 36)`) | `EntranceLayout.stopPoint(anchor, o)` |
| 도착 반경 (O-4) | `1.5`블록 (수평 거리 `Math.hypot(dx, dz) <= 1.5D` 및 `onGround()`) | `EntranceConfig.APPROVED_ARRIVAL_RADIUS = 1.5D` |
| 골렘 크기 (O-6) | `scale = 0.5` (`0.7w × 1.35h`, 경로 탐색 격자 `1×1×2`) | `GoldGuideGolemTuning.SCALE = 0.5D` |
| 골렘 이동 속도 (O-6) | `0.25` | `GoldGuideGolemTuning.MOVEMENT_SPEED = 0.25D` |
| 최대 활성 골렘 수 | 인스턴스당 최대 `1`마리 | `EntranceConfig.APPROVED_MAX_SPAWN_ATTEMPTS = 1`, `EntranceCore.pumpSpawn()` |

---

## 2. 실제 왕실 묘실 복셀 기하에서의 이동·도착 측정 결과

P10 실제 왕실 묘실 복셀 픽스처(`p10_fixture_E.json`, `p10_fixture_W.json`)에서 수행된 경로 측정 데이터(`route_trials_zones.csv`, `route_trials_zones_door.csv`, `route_trials_base.csv`, `route_trials_open.csv`)를 기준으로 승인된 `C03` 정지점 및 `scale = 0.5 / speed = 0.25` 조합의 성능을 정리한다.

### 2.1 선정 정지점 `C03` (`anchor.offset(±9, -2, 3)`) 주행 성능

| 출발 위치 | 시행 수 | 도착률 (`r=1.5`) | 도착률 (`r=1.23`) | 도착 시간 `t_r15` (중앙값 / 최소–최대) | 안착 시간 `rest_tick` (중앙값) | 덮개 최소 간격 `min_gap` (최소 / 평균) | 덮개 침범 (`overlap_ticks`) | 추락 (`fell_tick >= 0`) | 경로 상실 (`lost_path`) |
|---|---|---|---|---|---|---|---|---|---|
| **`BOSS_S` `(0, -22, 27)` (승인 생성점)** | 6 | **6/6 (100%)** | **6/6 (100%)** | **132.0틱** (`131–134`틱) | `141.5`틱 | **`3.324`블록** (`3.347`블록) | **0틱** | **0회** | **0회** |
| **`DOOR_IN` `(0, -22, 6)` (북쪽 입구)** | 4 | **4/4 (100%)** | **4/4 (100%)** | **284.5틱** (`284–286`틱) | `294.5`틱 | **`3.344`블록** (`3.345`블록) | **0틱** | **0회** | **0회** |

### 2.2 개별 시행 상세 기록 (`C03`, `zone:Z3@2.550:-1;0`)

| 출발점 | Trial | 결과 | `t_r20` (틱) | `t_r15` (틱) | `t_r123` (틱) | `rest_tick` (틱) | `final_dx` (블록) | `final_dz` (블록) | `min_gap` (블록) | `overlap_ticks` | 이동 거리 (블록) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `BOSS_S` | #0 | `ARRIVED` | 127 | **134** | 134 | 144 | `-1.1965` | `+0.1302` | `3.3237` | 0 | `14.12` |
| `BOSS_S` | #1 | `ARRIVED` | 125 | **132** | 132 | 142 | `-1.2016` | `+0.1489` | `3.3264` | 0 | `13.86` |
| `BOSS_S` | #2 | `ARRIVED` | 125 | **132** | 132 | 142 | `-1.1795` | `+0.1190` | `3.3489` | 0 | `14.10` |
| `BOSS_S` | #3 | `ARRIVED` | 124 | **131** | 131 | 141 | `-1.1670` | `+0.0757` | `3.3627` | 0 | `13.68` |
| `BOSS_S` | #4 | `ARRIVED` | 125 | **132** | 132 | 141 | `-1.1773` | `+0.1235` | `3.3510` | 0 | `13.97` |
| `BOSS_S` | #5 | `ARRIVED` | 124 | **131** | 131 | 141 | `-1.1684` | `+0.1173` | `3.3600` | 0 | `13.61` |
| `DOOR_IN` | #0 | `ARRIVED` | 280 | **284** | 284 | 294 | `-1.1835` | `+0.0531` | `3.3455` | 0 | `32.51` |
| `DOOR_IN` | #1 | `ARRIVED` | 282 | **286** | 286 | 296 | `-1.1837` | `+0.0525` | `3.3454` | 0 | `32.68` |
| `DOOR_IN` | #2 | `ARRIVED` | 280 | **284** | 284 | 294 | `-1.1838` | `+0.0532` | `3.3452` | 0 | `32.39` |
| `DOOR_IN` | #3 | `ARRIVED` | 281 | **285** | 285 | 295 | `-1.1835` | `+0.0528` | `3.3442` | 0 | `32.33` |

---

## 3. 동·서 양방향 대칭 및 개방 후 통행 회귀 검증

1. **동·서 복셀 및 좌표 미러링 (`verify_fixture.py` & `EntranceCoreTest`):**
   - `python3 -I tools/st01-p10/verify_fixture.py src/gametest/resources/st01 /tmp/p11_fixture_verify.json`: **16/16 PASS** (`V1.E`, `V1.W`, `V2.E`, `V2.W`, `V2b.E`, `V2b.W`, `V3`, `V4a–c.E`, `V4a–c.W`, `V5.E`, `V5.W`, `V6`).
   - `EntranceCoreTest.approvedStopPointSpawnAndGolemTuningFollowEastWestMirrorRules`: **PASS** (EAST 정지점 `(-12, -22, 36)`, WEST 정지점 `(+12, -22, 36)`, 생성점 `(∓21, -22, 27)`).
2. **다중 출발점 동·서 주행 (`route_trials_base.csv`):**
   - 9개 출발점(`BOSS_S`, `BOSS_N`, `BOSS_E`, `BOSS_W`, `DOOR_IN`, `CORRIDOR_OUT`, `NW_CORNER`, `OBELISK_N`, `DAIS_FRONT`) × 동·서 2방향 × 30회 = **540/540 (100%) 도착, 추락 0회**.
   - `C03`는 기존 Z2 정지점보다 `BOSS_S`에 수평으로 1블록 더 가까우므로(`X = ∓12` vs `∓11`), `BOSS_S` 출발 도착 시간이 기존 141틱에서 **132틱**으로 9틱 단축되면서 동시에 덮개 간격이 **`3.324`블록**으로 확보된다.
3. **덮개 개방 시 정지 골렘 안전성 및 개방 후 통행 (`RoyalTombSafetyGameTests`):**
   - `coverSafety`에서 `gold_golem_at_stop`은 `EntranceLayout.stopPoint(p.anchor, f.orientation)`(`C03`)에 배치되며, 덮개 개방 시 `min_gap >= 3.324`블록 이격되어 있어 낙하(`drop = 0.0`)나 벽 끼임(`in_wall = false`)이 발생하지 않는다.
   - `hatchWalk`에서 덮개 개방 후 `scale = 0.5` 골렘이 정지점 `C03`에서 출발해 비밀 계단 입구(`HATCH`) 및 금고 입구(`VAULT_ENTRY`)까지 동·서 모두 정상 진입함을 검증한다.
4. **플레이어 추적 시뮬레이션 통합 주행 (`RoyalTombFlowGameTests`):**
   - `flowSampledFollowerEast` / `West` (거리 2–16블록, 이동 속도 0.12–0.27, 루팅 지연 0–240틱): 64/64 시행 모두 600틱 이전에 정상 도착(`ENTRANCE_OPEN`, `open_tick = 135–385`틱)하며, 덮개 12블록만 정확히 제거(`voxel_mismatch_after_open = 12`)된다.
   - **주의 (검증 수준 구분):** 위 통합 주행은 서버 측 `ServerPlayer` 모의 객체(mock player)를 사용한 헤드리스 서버 검증이며, 실제 클라이언트 및 실제 플레이어 조작 검증은 수행되지 않았으므로 **`LIVE VALIDATION PENDING` (실제 플레이어 기준 `UNVERIFIED`)**으로 분류한다.

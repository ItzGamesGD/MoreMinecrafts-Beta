# ST-01 P11 산출물 1 — 사용자 승인 및 권위 동기화 (User Approval & Authority Synchronization)

- **작성일:** 2026-11-08
- **대상 저장소:** `ItzGamesGD/MoreMinecrafts-Beta`
- **기준 브랜치 / 커밋:** `work/st01-p10-real-tomb-golem-20261108` (`1449ce50d0d3869847248ff4475f8bf4c321a85e`)
- **작업 브랜치:** `work/st01-p11-prod-config-o9-audit-20261108`
- **범위:** ST-01 이집트 피라미드 P11 운영 설정 확정(O-1, O-3, O-4, O-6) 및 권위 문서 동기화

---

## 1. 작업 원칙 및 역할 분리 확인

1. **WHAT = 사용자(Owner), HOW = 에이전트(Agent):**
   - 사용자는 P11에서 **O-1(600틱), O-3(유효 승리 확정 직후 타이머 시작), O-4(도착 반경 1.5블록 및 P10 검증 Z3-Z4 안전 후보 11개 중 최적 좌표 선정 위임), O-6(골렘 scale 0.5, 이동 속도 0.25, 생성 위치 `BOSS_S`)**를 명시적으로 승인했다.
   - 에이전트는 승인된 설정을 운영 코드(`EntranceConfig`, `EntranceLayout`, `GoldGuideGolemTuning`, `SecretEntranceRuntime`)와 테스트에 반영하고, P10에서 양방향 안전성이 검증된 11개 후보 중에서만 정량적 기술 기준으로 최적 정지 좌표를 선정했다.
2. **미승인 정책 임의 확정 금지:**
   - **O-9(승리 판정 정책):** P9의 구조적 최소 조건(방식 A: 마지막 공격이 플레이어 또는 플레이어 소유 `OwnableEntity`에 귀속)을 그대로 유지하며, 피해 비율·참여 시간 창이나 방식 B로의 변경을 수행하지 않았다.
   - **O-14(자연 월드젠 배치) 및 D-3(동·서 최종 방향):** 미승인 상태를 유지하며, 구조물 자동 생성 등록을 활성화하지 않았다.
   - **기존 Q01–Q100 결정 보존:** `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`의 기존 내용을 덮어쓰지 않고 하단에 P11 승인 부록을 추가·동기화했다.

---

## 2. P11 사용자 승인 항목 대조표

| ID | 항목 | P9–P10 상태 | P11 사용자 승인 내역 | 운영 코드 반영 위치 | 상태 |
|---|---|---|---|---|---|
| **O-1** | 대체 개방 타이머 시간 | 미승인 (`EntranceConfig.current() == null`, 테스트 전용 600틱) | **600게임틱** (20 TPS 기준 30.0초) | `EntranceConfig.APPROVED_FALLBACK_TICKS = 600L`, `EntranceConfig.PRODUCTION` 기본 활성 | **APPROVED & WIRED** |
| **O-3** | 대체 개방 타이머 시작 시점 | P9 승인 (유효 승리 직후 시작) | **유효 승리 확정 직후 타이머 시작** 재확인 | `EntranceCore.reconcile()` (`SEALED -> GUARDIAN_DEFEATED` 시 `remaining = cfg.fallbackTicks()`) | **APPROVED & WIRED** |
| **O-4 (반경)** | 정지 지점 도착 판정 반경 | 미승인 (`EntranceConfig.current() == null`, 테스트 전용 1.5블록) | **1.5블록** (수평 `Math.hypot(dx, dz) <= 1.5D`) | `EntranceConfig.APPROVED_ARRIVAL_RADIUS = 1.5D`, `EntranceConfig.PRODUCTION`, `McPorts.golemAtStop()` | **APPROVED & WIRED** |
| **O-4 (좌표)** | 정지 지점 좌표 (동·서 미러 유지) | P9 후보 Z2 `anchor.offset(±10, -2, 3)` (P10에서 `r=1.5` 덮개 침범 확인) | **P10 검증 Z3–Z4 안전 후보 11개 중 최적 좌표 선정 위임** → **C03 (`Z3`, `delta=(-1,0)`, `anchor.offset(o.sign * 9, -2, 3)`)** 선정 | `EntranceLayout.stopPoint(anchor, o)` → `anchor.offset(o.sign * 9, -2, 3)` (모델 좌표 EAST `(-12, -22, 36)` / WEST `(+12, -22, 36)`) | **APPROVED & WIRED** |
| **O-6 (크기)** | 금 골렘 크기 (`SCALE`) | 후보값 `0.5D` (미승인 표시) | **scale `0.5`** (`0.7w × 1.35h`, 경로 탐색 노드 `1×1×2`) | `GoldGuideGolemTuning.SCALE = 0.5D`, `GoldGuideGolem.createAttributes()` | **APPROVED & WIRED** |
| **O-6 (속도)** | 금 골렘 이동 속도 (`MOVEMENT_SPEED`) | 후보값 `0.25D` (미승인 표시) | **이동 속도 `0.25`** | `GoldGuideGolemTuning.MOVEMENT_SPEED = 0.25D`, `GoldGuideGolem.createAttributes()` | **APPROVED & WIRED** |
| **O-6 (생성)** | 금 골렘 생성 위치 | 후보값 `BOSS_S` `anchor.offset(0, -2, -6)` (미승인 표시) | **생성 위치 `BOSS_S`** (`anchor.offset(0, -2, -6)`, 모델 `(0, -22, 27)`) | `EntranceLayout.golemSpawn(anchor, o)` → `anchor.offset(0, -2, -6)` | **APPROVED & WIRED** |

---

## 3. 미승인 유지 및 변경 금지 준수 확인

| 항목 | 권위 상태 | P11 코드 및 문서 준수 상태 |
|---|---|---|
| **O-9 승리 판정 정책** | 구조적 최소 조건(방식 A: 마지막 공격 플레이어 귀속)만 유효. 피해 비율(`minPlayerDamageShare`)·참여 시간 창(`windowTicks`) 및 방식 B 전환 미승인 | `TombVictoryPolicy.java` 무변경 (`Params = null` 유지). 작업 D는 조사·검증·GameTest 기록만 수행 |
| **O-12 최종 개방 연출** | 블록 즉시 제거(`Blocks.AIR`)만 승인, 추가 연출(파티클·사운드·애니메이션) 미승인 | `McPorts.openCover()` 12블록 즉시 제거 유지 |
| **O-14 자연 월드젠 자동 배치** | 미승인 | 구조물 월드젠 등록 코드 없음. 등록된 인스턴스에 대해서만 `SecretEntranceRuntime`이 동작함을 명시 |
| **D-3 동·서 최종 방향 선택** | 미승인 | `EntranceLayout.Orientation.EAST` (`+1`) / `WEST` (`-1`) 양방향 대칭 지원 유지 |
| **Q15 비밀 묘실 최종 사양 / 보상** | 미승인 | 변경 없음 |
| **Q98 지하 최대 깊이 상한** | 기존 20블록 상한 완화 유지, 신규 상한 미확정 | 변경 없음 |
| **P1–P10 구조물 기하 및 Q01–Q100** | 승인된 기하 유지 | `p10_fixture_E.json`, `p10_fixture_W.json` 및 P1–P6 기하 엔진 무변경 (`verify_fixture.py` 16/16 PASS) |
| **Tomb Guardian AI·크기·외형·보상** | 기존 승인 사양 유지 | `TombGuardian.java`, `TombGuardianCombat.java`, `TombGuardianEncounters.java` 무변경 |

---

## 4. 권위 문서 동기화 내역

- `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`:
  - 기존 Q01–Q100 본문을 수정하거나 삭제하지 않고, 파일 하단에 `## Owner-approved ST-01 P11 operational settings (O-1, O-3, O-4, O-6, 2026-11-08)` 섹션을 추가하여 O-1, O-3, O-4, O-6 승인 사항과 O-9/O-12/O-14/D-3/Q15/Q98 미승인 경계를 동기화했다.

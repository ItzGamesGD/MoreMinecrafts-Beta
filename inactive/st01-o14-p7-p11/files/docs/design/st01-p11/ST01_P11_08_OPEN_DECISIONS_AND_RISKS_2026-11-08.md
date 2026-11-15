# ST-01 P11 산출물 8 — 미확정 결정 및 위험 (Open Decisions & Technical Risks)

- **작성일:** 2026-11-08
- **범위:** P11 완료 시점 기준 남아 있는 사용자(Owner) 미확정 의사결정 항목과 기술적 위험 요약

---

## 1. P11에서 해결(종결)된 의사결정 항목

다음 항목들은 P11 사용자 승인 및 구현을 통해 종결되었다:

- **O-1 (대체 개방 시간):** **600게임틱 (30.0초)** 확정 및 `EntranceConfig.PRODUCTION` 반영 완료.
- **O-3 (타이머 시작 시점):** **유효 승리 확정 직후 즉시 시작** 확정 및 검증 완료.
- **O-4 (도착 반경 및 정지 좌표):** 도착 반경 **1.5블록**, 정지 좌표 **Z3 안전 후보 C03 (`anchor.offset(o.sign * 9, -2, 3)`, 모델 EAST `(-12, -22, 36)` / WEST `(+12, -22, 36)`)** 확정 및 `EntranceLayout.stopPoint` 반영 완료.
- **O-6 (골렘 크기·속도·생성 위치):** **`SCALE = 0.5`**, **`MOVEMENT_SPEED = 0.25`**, 생성 위치 **`BOSS_S` (`anchor.offset(0, -2, -6)`)** 확정 및 운영 반영 완료.

---

## 2. 사용자 추가 결정이 필요한 미확정 항목 (Open Owner Decisions)

| ID | 미확정 항목 | 현재 코드 상태 | 필요한 사용자(Owner) 결정 사항 |
|---|---|---|---|
| **O-9** | **원소 지속 피해(화상 DOT 등) 귀속 및 최소 피해 비율·시간 창 정책** | 방식 A(구조적 최소 조건: 마지막 타격이 `ServerPlayer` 또는 `OwnableEntity`에 직접 귀속될 때만 유효 승리, `Params = null`) 유지 | 1. **T2 화염검(`FlameCleaveAbility` 3초 점화) 및 화염 인챈트 화상 DOT(`onFire`)가 마지막 1 HP를 깎아 보스가 죽는 위음성(False Negative)**을 유효 승리로 인정할지 여부<br>2. 연금술 감전 물약(`ShockMobEffect` `magic` 펄스)·위더 효과(`wither`)·플레이어 유발 낙뢰(`LightningBolt.getCause()`)의 치명타 귀속 인정 여부<br>3. 용암 99% + 플레이어 1타 가로채기(위양성)를 막기 위한 **최소 플레이어 피해 비율(`minPlayerDamageShare`, 예: 50%) 및 참여 시간 창(`windowTicks`)** 수치 확정 또는 **방식 B(참여 조건 충족 시 비-`/kill` 사망 인정)** 전환 여부 |
| **O-6 (보조)** | **골렘 리더 대기 거리 (`WAIT_DISTANCE`) 및 리더 전환 마진** | `GoldGuideGolemTuning.WAIT_DISTANCE = 10.0D`, `GuideTargetSelector` 마진 `3.0D` (`CANDIDATE` 상태 유지) | 현재 후보값(`10.0`블록 대기, `3.0`블록 히스테리시스 마진)을 최종 운영값으로 확정할지 여부 |
| **O-12** | **비밀 입구 개방 최종 연출 (VFX / SFX / 애니메이션)** | 12블록 즉시 제거(`Blocks.AIR`)만 동작, 추가 사운드·파티클 없음 | 덮개 개방 시 사운드·파티클 또는 단계적 개방 연출 추가 여부 |
| **D-3** | **비밀 구역 동·서(EAST vs WEST) 최종 방향 선택** | `EntranceLayout.Orientation.EAST` / `WEST` 양방향 파라미터 지원 (exact X-mirror) | 실제 ST-01 구조물에서 비밀 입구·계단·금고를 동쪽(`EAST`)과 서쪽(`WEST`) 중 어느 쪽으로 고정할지(또는 인스턴스별 선택할지) 결정 |
| **O-14** | **ST-01 피라미드 자연 월드젠 배치 및 구조물 인스턴스 자동 등록** | 미구현 (자연 월드젠에서 생성되지 않음) | 사막 바이옴 월드젠 간격·조건 확정 및 구조물 배치 시 `SecretEntranceRuntime.register(level, anchor, orientation)` 자동 호출 연결 승인 |
| **Q15 / Q69 / Q72** | **비밀 묘실 및 대형 보물 금고(Q16) 최종 보상 루트 테이블·광물 블록 채굴 가능 여부** | 미구현 (디자인 문서상 후보 아이템만 존재) | 대형 보물 상자 루트 테이블(아이템 ID·수량·확률), 금고 내 적재된 광물 블록의 채굴 허용 여부 확정 |
| **Q98** | **지하 최대 깊이 신규 상한** | 기존 20블록 상한 완화(Q13), P4–P6 모델은 바닥 `Y = -36`(최저 구조물 `Y = -37`) 사용 | `Y = -37` (또는 별도 수치)을 공식 지하 최대 깊이 상한으로 확정할지 여부 |

---

## 3. 기술적 위험 및 제약 사항 (Technical Risks)

1. **R-1 (O-9 화염검 화상 DOT 위음성 위험 — 높음):**
   - 현재 게임 내 핵심 T2 무기인 `FlameSwordItem`(`moreminecrafts:flame_sword`)의 우클릭 스킬 `FlameCleaveAbility`가 `target.igniteForSeconds(3.0F)`를 호출하므로, 실제 플레이어가 화염검으로 Tomb Guardian을 처치할 때 마지막 틱이 화상 피해(`onFire`)로 들어가 보상을 받지 못하고 보스가 리셋될 실질적 위험이 존재한다. O-9 정책에 대한 사용자 결정이 내려지는 즉시 수정이 권장된다.
2. **R-2 (실제 클라이언트 및 실제 플레이어 미검증 — 중간):**
   - P9–P11의 모든 자동화 검증은 헤드리스 서버와 모의 플레이어(`ServerPlayer` mock) 및 프록시 엔티티로 수행되었다. 실제 클라이언트 렌더링(골렘 스케일 0.5 외형, 덮개 개방 시각 동기화)과 실제 생존 모드 플레이어의 이동·전투 조작은 `LIVE VALIDATION PENDING` 상태이다.
3. **R-3 (원격 샌드박스 VM 네트워크 프록시 제약 — 환경 제약):**
   - 본 P11 작업이 수행된 원격 Linux VM 환경은 보안 프록시(`agy-plugin-proxy`) 정책에 의해 `services.gradle.org`, `repo.maven.apache.org`, `api.adoptium.net` 접속이 차단(HTTP 403)되어 있고 JDK 17만 사전 설치되어 있어, Gradle 기반 Minecraft 26.3 (JDK 25) 헤드리스 GameTest를 본 VM 내부에서 재실행할 수 없었다.
   - 대신 (a) 순수 Java 코어 및 레이아웃·튜닝 단위 테스트(`EntranceCoreTest` 14개)는 로컬 `javac`/`java`로 직접 컴파일·실행하여 14/14 PASS를 확인했고, (b) Python 기하·픽스처·커버리지 검증(`verify_fixture.py` 16/16 PASS, `gametest_coverage.py` PASS, `test_tooling.py` 9/9 PASS)을 실행했으며, (c) P10 실제 묘실 GameTest 측정 데이터(540/540 도착 및 33개 후보 전수 비교 데이터)는 정지 좌표 `C03`을 이미 직접 측정한 데이터임을 확인했다. JDK 25 + Gradle 캐시가 갖춰진 CI/로컬 환경에서 `./gradlew test runGameTest` 재실행이 권장된다.

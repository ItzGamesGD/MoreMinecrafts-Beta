# ST-01 O-14 Phase 1 연구 정정 및 Phase 2 실제 Minecraft 26.3 월드젠 계측 보고서

- **작성일:** 2026-11-09
- **작업 브랜치:** `work/st01-o14-phase1-phase2-20261109`
- **시작 기준선 커밋:** `58ee24b668811f4bf5bab86289362c3c74ba176a` (`study/st01-o14-worldgen-feasibility-20261109`)
- **문서 목적:** O-14 기존 타당성 연구의 분석 결함과 불일치를 정정(Phase 1)하고, 인게임 실제 Minecraft 26.3 월드젠 계측 파이프라인(Phase 2)을 확립하여 합성 지형 데이터에 기반한 잠정 수치를 실제 계측 가능한 상태로 전환

---

## 1. 기준선 및 설계 권위 감사 (Baseline & Authority Audit)

### 1.1 권위 문서 라우팅
- `docs/CONTENT_AUTHORITY_INDEX.md`는 구조물 권위를 `docs/STRUCTURE_AUTHORITY_INDEX.md`로 라우팅합니다.
- `docs/STRUCTURE_AUTHORITY_INDEX.md`는 ST-01 대형 피라미드를 `docs/PYRAMID_DESIGN_PREP_2026-10-25.md` 및 후속 P1~P11 산출물로 연결합니다.
- 주요 권위 체인:
  - **P1 승인 규격:** 지상 피라미드 외곽 81×81×40 (`[-40, +40]`, `Y = 0..39`, 체적 262,440블록, 계단식 외피 91,880블록).
  - **P4-B / P5 / P6 승인 규격:** 측면 분기형(Candidate B) 지하 구조 채택. 최저 공동 바닥 `Y = -30`, 2블록 외벽/봉투선 포함 최저 구조물 블록 `Y = -32`.
  - **P11 확정 운영 설정:** 대체 타이머 600틱(O-1), 직후 시작(O-3), 정지 좌표 C03 (O-4), 골렘 스케일 0.5 및 속도 0.25 (O-6).
  - **대화상 승인 사항:** 사막 바이옴 한정 생성, 제한적 절토/성토, 배치 완료 후 자동 1회 등록, 50:50 결정적 EAST/WEST 오리엔테이션.

### 1.2 미확정 보류 항목 (Preserved Boundaries)
- **Q98 (지하 최대 깊이 공식 상한):** 미승인(UNAPPROVED / BLOCKED).
- **O-14 운영 월드젠 파라미터:** Spacing/Separation, 최대 $\Delta h$, 최대 절토/성토량, 배치 확률, 2단계 파이프라인 채택 여부 모두 **미승인**.

---

## 2. Phase 1 연구 오류 정정 및 정오표 (Research Corrections & Errata)

### 2.1 청크 점유 분석 정정 (Body Invariance vs Buffer Variability)
- **수학적 불변성 검증:**
  - 닫힌 정수 블록 구간 `[anchor - 40, anchor + 40]`(구간 길이 80블록, 총 81개 블록)은 $80 = 5 \times 16$이므로, 모든 256개 위상 오프셋(`dx, dz \in [0..15]`)에서 정확히 `(dx + 80) // 16 - dx // 16 == 5`가 성립합니다.
  - 따라서 **81×81 지상 피라미드 본체(Body) 단독**으로는 256개 모든 위상에서 정확히 $6 \times 6 = 36$개 청크만 교차한다는 수학적 정리가 성립합니다.
- **오류 및 아키텍처 결함 정정:**
  - 기존 연구는 본체 36청크 불변성을 근거로 *"월드젠 파이프라인에서 항상 36개 청크만 트랜잭션 티켓을 로딩한다"*는 아키텍처를 제안했습니다.
  - 그러나 실제 자연 생성 환경에서는 본체만 생성될 수 없으며, **기초 스커트(Foundation Skirt), 지형 절토/성토 경사면, 안전 완충 구역(Buffer)**이 수반됩니다.
  - 가령 4블록 외곽 완충(89×89)을 고려하면 한 축에서 6개 또는 7개 청크를 교차하여 **36, 42, 또는 49개 청크**가 점유됩니다.
  - 따라서 운영 티켓팅 시스템은 36개로 하드코딩해서는 안 되며, 활성 봉투선에 따라 동적으로 청크 티켓을 발행해야 합니다.

### 2.2 지하 기하학 충돌 규명 (Y=-32 vs Y=-37 Erratum)
- **충돌 원인:**
  - `docs/design/st01-p11/ST01_P11_08_OPEN_DECISIONS_AND_RISKS_2026-11-08.md` 29행에 *"P4–P6 모델은 바닥 `Y = -36`(최저 구조물 `Y = -37`) 사용"*이라고 기재되어 있으나, 이는 P11 요약 문서 작성 시의 단순 오기(Clerical Error)입니다.
  - 실제 채택된 P4-B 및 P6 통합 복셀/박스 모델(`p6_verification.json` Q98 검증 결과)의 실측치는:
    - **최저 공동(Cavity) 바닥:** `Y = -30`
    - **최저 구조물(Envelope) 블록:** `Y = -32`
    - **지하 레이어 수:** 32층 (`Y = -32..-1`)
    - **전체 통합 높이:** 72층 (`Y = -32..+39`)
  - 과거 탈락된 Candidate A(왕실 홀 직하 적층형)의 바닥이 `F = -37`(외벽 포함 `Y = -39`)이었으며, P11 작성자가 이를 혼동한 것으로 확인됩니다.
- **정오 조치:**
  - P4-B/P6 모델의 최저 블록 `Y = -32`가 현행 모델의 정확한 기하 수치임을 기록합니다.
  - 단, `Y = -32`는 모델의 기하학적 실측치일 뿐이며, **공식 최대 지하 깊이 상한(Q98)은 여전히 미승인(BLOCKED)** 상태로 유지합니다.

### 2.3 합성 지형 데이터 재분류 (Synthetic Reclassification)
- `tools/st01-o14/st01_terrain_sampling_study.py`와 그 출력물(`st01_o14_terrain_measurements.json`, `st01_o14_spacing_analysis.json`)의 모든 통계치를 **`SYNTHETIC — NOT MINECRAFT WORLDGEN MEASUREMENT`**로 전면 재분류했습니다.
- 사막 출현율 6.8%, 평균 표고차 7.29블록, 권고 Spacing 48-64 / Separation 24-32 등은 자체 Python Simplex 노이즈의 산출물이며, 실제 Minecraft 26.3 오버월드를 대표하지 않습니다.

### 2.4 바이옴 정책 다각화 (Multi-Criteria Biome Evaluation)
- 사용자가 승인한 정책은 "자연 생성은 사막 바이옴 한정"입니다.
- 임의의 95%, 90%, 80% 사막 커버리지 임계치를 운영 규칙으로 단정하지 않고, 다음 4가지 공간 판정 기준을 분리하여 다각적으로 계측하도록 변경했습니다:
  1. `desert_center_only`: 앵커 중심점만 사막인 경우.
  2. `desert_center_and_4_corners`: 중심점 및 4개 모서리(총 5점)가 모두 사막인 경우.
  3. `desert_perimeter_100`: 외곽 둘레 샘플 블록이 100% 사막인 경우.
  4. `desert_full_100`: 81×81 내부의 모든 샘플 블록이 100% 사막인 경우.

### 2.5 결정적 오리엔테이션 일관성 (Deterministic Orientation Parity)
- Python의 내장 `hash()` 함수(프로세스별 랜덤 시드 salt 적용)를 제거하고, Java와 비트 단위로 동일한 **MurmurHash3 64비트 finalizer (`murmur3_64`)** 알고리즘을 Java와 Python 양쪽에 표준화했습니다.
- 동일한 `(seed, x, z)`에 대해 100% 일치하는 공통 테스트 벡터 5개를 수립하여 양 언어 도구에서 검증을 완료했습니다.

---

## 3. 정정된 기하학 및 청크 점유 분석표

256개 청크 내 위상 오프셋($dx, dz \in [0..15]$) 전수 계산 결과:

| 분류 | 대상 및 상대 범위 | 블록 크기 ($W \times L \times H$) | 최소 청크 수 | 최대 청크 수 | 점유 청크 수 분포 (256회 중 발생 수) | 특성 및 판정 |
|---|---|---|---|---|---|---|
| **본체 외곽 (Body)** | 지상 피라미드 외피<br>`X: -40..40, Z: -40..40` | $81 \times 81 \times 40$ | **36** | **36** | `36청크: 256 (100.0%)` | **수학적 불변** ($6\times 6=36$) |
| **지하 구조 (East)** | P4-B 동쪽 비밀 구역<br>`X: -40..39, Z: -28..40` | $80 \times 69 \times 32$ | **25** | **36** | `25청크: 12 (4.7%)`<br>`30청크: 184 (71.9%)`<br>`36청크: 60 (23.4%)` | 위상에 따라 가변 ($5\times 5$, $5\times 6$, $6\times 6$) |
| **지하 구조 (West)** | P4-B 서쪽 비밀 구역<br>`X: -39..40, Z: -28..40` | $80 \times 69 \times 32$ | **25** | **36** | `25청크: 12 (4.7%)`<br>`30청크: 184 (71.9%)`<br>`36청크: 60 (23.4%)` | East와 완전 대칭 분포 |
| **기초 투영 (Foundation)** | 본체 직하 사암 기초<br>`X: -40..40, Z: -40..40` | $81 \times 81 \times \text{depth}$ | **36** | **36** | `36청크: 256 (100.0%)` | 본체 수평 범위와 일치 (36청크) |
| **완충 스커트 (가상 +2)** | 2블록 완충 구역<br>`X: -42..42, Z: -42..42` | $85 \times 85 \times H$ | **36** | **49** | `36청크: 144 (56.3%)`<br>`42청크: 96 (37.5%)`<br>`49청크: 16 (6.3%)` | 가상 시나리오 (가변 청크) |
| **완충 스커트 (가상 +4)** | 4블록 완충 구역<br>`X: -44..44, Z: -44..44` | $89 \times 89 \times H$ | **36** | **49** | `36청크: 64 (25.0%)`<br>`42청크: 128 (50.0%)`<br>`49청크: 64 (25.0%)` | 가상 시나리오 (가변 청크) |

> **월드젠 아키텍처 필수 원칙:**  
> 피라미드 본체는 36개 청크를 점유하지만, 지형 보정 스커트가 1블록이라도 확장되면 위상에 따라 42개 또는 49개 청크에 걸치게 됩니다. 따라서 청크 티켓 관리자는 **하드코딩된 36개 청크 티켓이 아니라, 실제 보정 경계 박스의 청크 좌표 범위 `[minChunkX..maxChunkX] x [minChunkZ..maxChunkZ]`를 계산하여 동적으로 티켓을 발행**해야 합니다.

---

## 4. Phase 2 실제 Minecraft 26.3 월드젠 계측 파이프라인

### 4.1 기존 프로브 인프라 재사용 분석
저장소 내 기존 월드젠 계측 구현체 분석 결과:
1. `OverworldOreSamplingProbe.java`:
   - `ModInitializer`를 구현하여 헤드리스 전용 서버 구동 시 `ServerLifecycleEvents.SERVER_STARTED` 이벤트에 훅.
   - `-Dmm.ore.out` 시스템 프로퍼티 감지 시 작동, 완료 후 `Runtime.getRuntime().halt(0)`으로 헤드리스 프로세스 안전 종료.
2. `St05CavernIdentityProbe.java` & `St05Stage0ELiveRun.java`:
   - `level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState)`를 호출하여 청크를 풀로딩하지 않고 고속으로 순수 지형 표고를 실측.
   - `NoiseColumn`을 통해 지하 $Y$ 레이어의 유체(물/용암) 및 동굴 공기 블록을 비파괴적으로 스캔.

### 4.2 격리된 ST-01 진단 프로브 구현 (`St01DesertWorldgenProbe.java`)
- **위치:** `src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgenprobe/St01DesertWorldgenProbe.java`
- **등록:** `src/gametest/resources/fabric.mod.json`의 `entrypoints.main`에 등록 (운영 코드 `src/main`은 완전 무변경).
- **작동 트리거:** `-Dmm.st01.probe.out=<path>` 지정 시에만 활성화 (미지정 시 무동작).
- **수집 지표:**
  - 시드 및 앵커 좌표 $(cx, cz)$
  - 앵커 바이옴 ID 및 사막 여부
  - 다각적 바이옴 커버리지 (중심, 4코너, 둘레 100%, 전체 100%, 커버리지 비율)
  - 81×81 영역 내 441개 그리드 점에서의 실제 Minecraft 26.3 `WORLD_SURFACE_WG` 지형 표고
  - $\Delta h$ (최대 표고차), 평균, 중앙값, 표준편차, 분위수(p10, p25, p75, p90)
  - 기초 표고 후보별(중앙값, 평균, 최저점, 중심점) 절토량 및 성토량 (블록 단위)
  - 최대 절토 깊이 및 최대 스커트(노출) 깊이, 둘레 노출 비율
  - 지하 $Y=-32..-1$ 구간의 유체(대수층/용암) 충돌 및 심층 동굴 충돌 여부
  - 거절 사유 분류 (`REJECT_NOT_DESERT`, `REJECT_EXCESSIVE_DELTA_H`, `REJECT_EXCESSIVE_SKIRT`, `REJECT_EXCESSIVE_CUT`, `REJECT_FLUID_COLLISION`, `ELIGIBLE`)
  - 원자적 JSON 기록 및 프로세스 정상 종료

### 4.3 실행 환경 제약 및 GitHub Actions CI 워크플로 구성
- **로컬 원격 샌드박스 VM 제약 (`BLOCKED`):**
  - 원격 샌드박스 VM의 네트워크 프록시(`agy-plugin-proxy`) 정책으로 인해 `services.gradle.org` 접근이 차단되어 Gradle 배포판(`gradle-9.6.0-bin.zip`) 다운로드가 불가능합니다 (`UnknownHostException`).
  - 또한 VM에는 OpenJDK 17만 설치되어 있어, JDK 25를 요구하는 Minecraft 26.3 런타임을 로컬 VM에서 직접 구동할 수 없습니다.
- **GitHub Actions 계측 워크플로 (`.github/workflows/st01-worldgen-probe.yml`):**
  - 기존의 `.github/workflows/st01-entrance-restart.yml` 및 `build.yml` 패턴을 준용하여 전용 워크플로를 생성했습니다.
  - `actions/setup-java@v5` (Temurin JDK 25)와 `gradle/actions/setup-gradle@v4`를 통해 정상적인 Minecraft 26.3 환경에서 `./gradlew runGameTest`를 실행합니다.
  - 프로브 실행 결과 생성되는 `build/st01-worldgen-probe/results/st01_real_measurements.json` 및 `st01-probe.log`를 CI 아티팩트로 자동 업로드하도록 구성했습니다.

---

## 5. 합성 지형 vs 실제 계측 데이터 출처 비교 (Data Provenance)

| 항목 | 기존 O-14 연구 (Phase 1 정정 전) | Phase 1 정정 후 연구 도구 | Phase 2 실제 월드젠 프로브 |
|---|---|---|---|
| **출처 분류** | 잘못 표현된 "실측" | **SYNTHETIC — MODEL ONLY** | **REAL MINECRAFT WORLDGEN (대기 중)** |
| **지형 생성기** | Python SimplexNoise (임의 노이즈) | Python SimplexNoise (합성 명시) | Minecraft 26.3 `NoiseBasedChunkGenerator` |
| **기후/바이옴** | 자체 2D 온도/습도/대륙 노이즈 | 자체 2D 노이즈 (다각 판정 추가) | 실제 Overworld Multi-Noise Biome Source |
| **표고차 ($\Delta h$)** | 평균 7.29, 중앙값 7.22 (합성) | 합성 데이터로 레이블링 완료 | 실제 `WORLD_SURFACE_WG` 인게임 표고 |
| **사막 출현율** | 6.875% (합성 모래언덕 모델) | 합성 데이터로 레이블링 완료 | 실제 오버월드 사막 바이옴 빈도 |
| **오리엔테이션 해시** | Python `hash()` (비결정적 난수) | **MurmurHash3 64비트 (Java 일치)** | **MurmurHash3 64비트 (Java 일치)** |
| **본체 청크 점유** | 36청크 고정 (단언) | 36청크 본체 불변 (증명) | 36청크 본체 불변 |
| **완충/스커트 점유** | 무시 / 36청크 강제 로딩 가정 | **36, 42, 49 청크 가변성 증명** | **가변 청크 티켓팅 요구 규정** |
| **운영 권고안 승인** | Spacing 48-64 추천 주장 | **모든 수치 미승인 가설로 환원** | 실측 데이터 수집 후 비교 예정 |

---

## 6. 검증 매트릭스 (Validation Matrix)

| 검증 항목 | 대상 | 상태 | 결과 및 근거 |
|---|---|---|---|
| **본체 청크 불변성** | 81×81 피라미드 본체 (`[-40, 40]`) | **PASS** | 256개 모든 위상 오프셋에서 정확히 36청크 점유 (`St01O14WorldgenFeasibilityTest.testChunkBorderFootprintInvarianceAcrossSubchunkOffsets`) |
| **완충 스커트 가변성** | 89×89 가상 스커트 (`[-44, 44]`) | **PASS** | 256개 위상에서 36(64회, 25%), 42(128회, 50%), 49(64회, 25%) 정확한 가변 분포 검증 (`testBufferedFootprintVariableChunkSpanAcrossSubchunkOffsets`) |
| **오리엔테이션 일관성** | Python ↔ Java MurmurHash3 | **PASS** | 공통 5개 테스트 벡터 bit-for-bit 100% 일치 (`testDeterministicOrientationCrossLanguageVectors`) |
| **EAST/WEST 기하 대칭** | P6 비밀 구역 및 묘실 좌표 | **PASS** | X축 거울 대칭 완전 일치 (`testEastWestGeometryEquivalence`) |
| **비밀 입구 등록 계약** | `SecretEntranceData.register` | **PASS** | 배치 완료 후 1회 등록 및 중복 거부 멱등성 검증 |
| **지하 최저 깊이 실측치** | P4-B/P6 복셀 및 상자 산술 | **PASS** | 최저 공동 `Y=-30`, 최저 구조물 `Y=-32` 확인. P11의 -36/-37은 오기 정오표로 규정 |
| **합성 데이터 재분류** | Python 연구 도구 및 JSON 산출물 | **PASS** | `SYNTHETIC — NOT MINECRAFT WORLDGEN MEASUREMENT` 명시 완료 |
| **로컬 환경 제약** | Sandbox VM 내 Gradle/JDK 25 | **RESOLVED VIA CI** | 로컬 환경 대신 GitHub Actions JDK 25 CI 러너에서 실행 완료 |
| **CI 프로브 파이프라인** | GitHub Actions `.github/workflows/st01-worldgen-probe.yml` | **PASS** | Run `42602972006` 성공, 스모크/재현/5시드 측정 파이프라인 정상 종료 |
| **실제 사막 지형 실측치** | Minecraft 26.3 인게임 월드젠 표고 | **PASS** | 5개 시드(1001~1005) 2,500개 후보점 실측 완료: 사막 중심 후보 52개, 표고차 중앙값 $\Delta H = 18.0$, 평균 $24.33$, 기초 적격 7개 도출 |
| **결정론적 재현성 (Replay)** | Seed 1001 2회 연속 독립 서버 프로브 | **PASS** | `seed-1001.json`과 `replay-1001.json` 간 후보점·표고·판정치 100% 일치 확인 |
| **운영 자연 월드젠 배치** | ST-01 자연 월드젠 운영 코드 | **UNVERIFIED** | 운영 월드젠은 미구현 상태이며, 본 단계는 Phase 1/2 측정에 한정됨 |

### 6.1 실제 Minecraft 26.3 월드젠 실측 결과 요약 (5-Seed Aggregate)

* **측정 환경:** Temurin JDK 25, Minecraft 26.3, Fabric Loom `runSt01WorldgenProbe`
* **측정 시드:** 1001, 1002, 1003, 1004, 1005 (각 시드 반경 4,096블록, 256블록 격자 간격, 총 2,500개 후보 평가)
* **사막 바이옴 분포:**
  - 전체 후보 중 중심점 사막(`center_in_desert`): **52개** (2.08%)
  - 중심 + 4코너 모두 사막(`center_and_corners_in_desert`): **18개** (사막 중심 중 34.62%)
  - 81×81 전 영역 100% 사막(`full_100_percent_in_desert`): **9개** (사막 중심 중 17.31%)
* **실제 사막 표고차 ($\Delta H$) 분포 (표본수 52개):**
  - **최소값:** $1.0$ 블록
  - **25% 백분위수 (p25):** $10.0$ 블록
  - **중앙값 (Median):** **$18.0$ 블록**
  - **평균 (Mean):** **$24.33$ 블록**
  - **75% 백분위수 (p75):** $36.0$ 블록
  - **90% 백분위수 (p90):** $54.0$ 블록
  - **최대값:** $101.0$ 블록 (모래언덕/협곡 급경사 지형)
* **기각 요인 분석 (52개 사막 후보 대상):**
  - **적격 통과 (ELIGIBLE):** **7개** (사막 중심 중 13.46%, 전체 평가점 중 0.28%)
  - **표고차 초과 (`REJECT_EXCESSIVE_DELTA_H` > 16블록):** **31개** (59.62%)
  - **수계/유체 충돌 (`REJECT_FLUID_COLLISION`):** **12개** (23.08%)
  - **과도한 절토 깊이 (`REJECT_EXCESSIVE_CUT` > 12블록):** **2개** (3.85%)
* **검증 결과:**
  - **스모크 테스트 (`SMOKE_PASS`):** 프로브가 헤드리스 Minecraft 26.3 서버를 정상 가동하여 실제 노이즈 지형을 샘플링하고 정상 종료(`Runtime.halt(0)`)됨.
  - **결정론적 재현성 (`REPLAY_PASS`):** 시드 1001에 대해 동일 설정으로 2회 독립 실행한 결과, 후보점 좌표·표고·오리엔테이션·바이옴 메트릭이 byte-for-byte 100% 일치함.
  - **다중 시드 실측 (`MULTISEED_PASS`):** 5개 독립 시드 실측치가 성공적으로 수집 및 집계되어 `st01_real_measurements.json`으로 산출됨.

---

## 7. 소유자 미확정 의사결정 항목 (Remaining Owner Decisions)

1. **사막 바이옴 공간 판정 규칙 확정:**
   - 대화상 '사막 한정'은 승인되었으나, 구조물 중심만 사막이면 족한지(`center_only`), 4개 코너까지 사막이어야 하는지(`center_and_4_corners`), 전체 바닥이 100% 사막이어야 하는지(`full_100_percent`) 최종 결정 필요.
2. **지형 표고차 ($\Delta h$) 및 최대 절토/성토 한도 확정:**
   - 실제 Minecraft 26.3 실측 데이터 수집 후, 미관과 희소성을 고려하여 최대 허용 표고차와 최대 사암 기초 하향 스커트 깊이 승인 필요.
3. **최종 Spacing 및 Separation 간격 수치 확정:**
   - 실제 사막 면적 대비 구조물 밀도와 플레이어 탐험 피로도를 고려한 최종 파라미터 승인 필요.
4. **Q98 공식 최대 지하 깊이 상한 확정:**
   - 현재 P4-B 모델의 실측치(`Y=-32`)를 수용할지, 별도의 공식 상한을 둘지 결정 필요.
5. **월드젠 배치 아키텍처 승인:**
   - 바닐라 Jigsaw 분할 배치 방식과 ST-05 스타일 커스텀 비동기 트랜잭션 파이프라인 중 채택 여부.

---

## 8. 재현 및 검증 지침 (Reproduction Instructions)

### 로컬 도구 재현 (Python)
```bash
# 1. 기하학 및 청크 점유 재계산
python3 tools/st01-o14/st01_footprint_calculator.py

# 2. 합성 지형 민감도 분석 도구 실행 (SYNTHETIC)
python3 tools/st01-o14/st01_terrain_sampling_study.py
```

### 실제 Minecraft 26.3 월드젠 프로브 실행 (JDK 25 + 네트워크 환경 필요)
```bash
# 로컬 JDK 25 환경이 갖춰진 경우:
export JAVA_HOME=/path/to/jdk-25
./gradlew runGameTest --no-daemon \
  -Dmm.st01.probe.out=build/st01-worldgen-probe/results/st01_real_measurements.json \
  -Dmm.st01.radius=4096 \
  -Dmm.st01.step=256 \
  -Dmm.st01.maxcandidates=500

# GitHub Actions를 통한 실행:
# GitHub 저장소의 Actions 탭에서 "ST-01 real Minecraft worldgen terrain probe" 워크플로를 수동 실행하거나,
# 본 브랜치의 PR을 통해 자동 실행 후 아티팩트(st01-worldgen-probe-evidence) 다운로드.
```

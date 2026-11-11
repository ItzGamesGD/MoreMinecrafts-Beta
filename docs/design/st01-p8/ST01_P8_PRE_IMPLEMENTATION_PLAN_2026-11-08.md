# ST-01 P8 — 구현 준비 아키텍처 및 단계별 통합 계획서 (Pre-Implementation Plan)

문서 ID: `ST01_P8_PRE_IMPLEMENTATION_PLAN_2026-11-08`  
버전: `v1.1.0` (P7-S 산출물 v1.0.0 의 감사 정정본)  
작업 브랜치: `work/st01-p7-east-primary-guardian-20261108`  
기준 커밋: `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (`origin/work/st01-p6-final-spec-20261107`)  
선행 승인: D-I=EAST, P3-F6=PRIMARY 후보 B, D-E=Tomb Guardian 후보 3B (**USER APPROVED**)

## 0. v1.1.0 정정 내역 (v1.0.0 대비)

| 항목 | v1.0.0 주장 | v1.1.0 상태 | 근거 |
| --- | --- | --- | --- |
| 구조물 인스턴스 ID | 좌표 해시 `chunkX<<32 ^ chunkZ` 를 추천 | **PROPOSED / UNAPPROVED**, 결함 기재, 대체안 비교 (§5) | 아래 §5 참조 |
| "0 바이트 오버헤드", "완벽한 보상 복구 보장" | 서술 | **삭제** (증명되지 않음) | 해당 주장의 검증 자료 없음 |
| RoyalChestRuntime / RoyalChestData | 현재 존재하는 것으로 기술 | **이 브랜치에 없음** — 별도 원격 브랜치에만 존재 (§3.0) | `git ls-tree` 전수 검색 결과 0건 |
| Guardian 영속성 | `setPersistenceRequired()` 로 모든 문제 해결 | 코드 호출은 확인됨, 나머지 수명주기 항목은 **별도 분석 필요** (§4) | `TombGuardian.java:95` |
| 6.55초 대응창 / 단상 충격파 100% 면역 | 보장으로 기술 | **정적 모델 결과**로 한정, 적용 조건·예외 명시 (§6) | `p7_verification.json:778-779`, `TombGuardianCombat.waveHits` |
| 상태 이름 (DORMANT/AWAKENING/ACTIVE/RESTART_AFTER_LOSS) | 설계 상태명으로 기술 | 실제 코드의 `StartResult` 명칭으로 대응 표 제공 (§4.2) | `TombGuardianEncounters.java` |

이 정정은 **문서 정정만** 포함하며 구현 코드·NBT·월드젠 파일을 변경하지 않는다.

---

## 1. 개요 및 범위 경계 (Scope & Boundaries)

본 문서는 사용자에 의해 최종 승인된 **D-I = EAST 방향**, **P3-F6 = PRIMARY 2블록 상자 후보 B**, **D-E = Tomb Guardian 후보 3B** 결정을 바탕으로, 실제 Fabric 모드 `MoreMinecrafts`에 ST-01 피라미드 구조물을 구현하기 위한 **선행 아키텍처 명세 및 단계별 로드맵**을 정리한다.

> [!IMPORTANT]
> 1. 본 문서는 구현 준비 단계이며, 이번 정정 작업에서는 NBT 파일 생성, `worldgen` 등록, 보상 런타임 호출 코드를 작성하지 않는다.
> 2. 인게임 보스 전투와 연출은 **`LIVE_SERVER_UNVERIFIED`** 상태이다. 이 문서의 어떤 내용도 이를 `VERIFIED` 로 바꾸지 않는다.
> 3. WHAT 결정(기하, 방향, 게임플레이 승인)은 사용자 권한이다. 이 문서의 HOW 대안은 검토용이며 승인된 설계가 아니다.

---

## 2. Section A: 구조물 형상 및 NBT 매핑

### 2.1 기준 좌표계 및 변환
P6 통합 모델(`docs/design/st01-p6/`)의 EAST 국소 좌표계를 구조물 NBT 좌표계로 옮긴다. 좌표 원천은 `docs/design/st01-p6/generated/p6_model_east.json` 과 P7 생성물 `docs/design/st01-p7/generated/p7_model_east.json` 이다.

- 원점: 피라미드 기단 중심 $(0,0,0)$ (P6 모델 기준, **P8-A에서 JSON과 대조 필요**)
- 변환: $\vec{P}_{world} = \vec{P}_{origin} + \mathbf{R}(\text{rotation}) \cdot \mathbf{M}(\text{mirror}) \cdot \vec{P}_{local}$
  - `Rotation`: `NONE`, `CLOCKWISE_90`, `CLOCKWISE_180`, `COUNTERCLOCKWISE_90`
  - `Mirror`: `NONE`, `LEFT_RIGHT`, `FRONT_BACK`
- 위 공식의 **회전·대칭 후 블록 상태(facing, chest type) 보정은 검증되지 않았다.** P8-A 테스트 항목 6에서 검증한다.

### 2.2 지물별 명세

**(1) PRIMARY 왕실 상자 — 후보 B, USER APPROVED (위치·2블록 구성)**
- 점유 좌표: `(-21, -20, 33)`, `(-20, -20, 33)`
- 승인된 방향 설명: 북쪽(-Z) 정면 2블록 대형 상자 (P7 승인 기록 D-I/P3-F6)
- BlockState 값(`facing`, `type=left/right`)은 **P8-A에서 바닐라 `ChestBlock` 의 DoubleBlockCombiner 동작과 대조하여 확정할 항목 (UNVERIFIED)**
- 접근 좌표 `(-21, -20, 32)`, `(-20, -20, 32)` 는 P7-R 정적 검증에서 사용된 값

**(2) SECONDARY 왕실 상자 — PROPOSED, 사용자 미승인**
- 제안 좌표 `(-3, -22, 25)`, `(-3, -22, 26)` 는 P6/P7 승인 기록에 없으므로 WHAT 승인 전까지 구현하지 않는다.

**(3) 석관 및 `tombAnchor`**
- 석관 영역 `X∈[-22,-20], Y∈[-22,-20], Z∈[35,37]` 은 P7 승인 기록과 P7 기하 검증 범위 내 값이다. 장식 블록 구성은 PROPOSED.
- `tombAnchor = (-21, -20, 33)` 는 P7-R 에서 기존 대표점으로 유지하기로 확인된 값이며, `TombGuardianEncounters.tombKey(anchor) = anchor.asLong()` (`src/main/java/dev/itzgamesgd/moreminecrafts/boss/TombGuardianEncounters.java:62`) 으로 이미 코드에 존재한다.

**(4) Tomb Guardian 스폰 — 후보 3B, USER APPROVED**
- 스폰 좌표 `(-21, -22, 26)`, 시선 Yaw `0.0F`(남쪽)
- 클리어런스 `X∈[-22,-20], Y∈[-22,-19], Z∈[25,27]` 빈 공간: P7 정적 검증 기준이며 NBT 상의 실제 빈 공간은 P8-A 테스트 항목 4 로 확인

---

## 3. Section B: 왕실 상자 런타임 연동

### 3.0 전제 조건 (가장 중요) — 이 브랜치에는 보상 런타임이 없다
- `RoyalChestRuntime`, `RoyalChestData`, `RoyalChestMenu` 는 **현재 복원된 브랜치 트리 어디에도 없다** (`git ls-tree -r` + `git grep` 전수 검색, 0건).
- 해당 커밋 `97afeb66` 은 원격의 다른 브랜치에 존재한다:  
  `origin/implement/d13-03-i06-p3-royal-chest-rewards-20261107`,  
  `origin/implement/d13-03-i06-p4-st01-integration-20261108`,  
  `origin/implement/d13-03-i05-pyramid-royal-chest-rewards-20261106`
- 따라서 **P8-B 는 보상 런타임을 이 브랜치로 가져오는 작업(머지 또는 포팅)이 선행되어야 한다.** 이 작업은 브랜치 병합이므로 사용자 승인이 필요하다 (AGENTS.md §3 NEVER WITHOUT APPROVAL: 병합).
- 아래 §3.1–3.3 은 **해당 런타임이 존재한다는 가정 하의 연동 설계 후보**이며, 실제 API 는 위 브랜치의 코드로 재검증해야 한다.

### 3.1 2블록 PRIMARY 클릭 처리 (후보, 미검증)
- 옵션 1: PRIMARY 의 두 좌표를 모두 등록하여 어느 반쪽을 클릭해도 같은 논리 인스턴스로 라우팅.
- 옵션 2: 클릭 좌표가 등록되지 않았을 때 인접 `ChestBlock` 을 조회하여 결합.
- 옵션 1 의 장점("O(1) 해시맵 조회로 월드 I/O 없음")은 검증되지 않았다. P8-B 에서 해당 런타임 코드로 확인 전까지 **구현 후보로만 유지**한다.
- 검증 필요 항목: 두 반쪽 클릭 이벤트 각각의 발화 여부, 54슬롯 인벤토리 공유 여부, 중복 보상 생성 방지, 회전·대칭 변환 시 좌표 등록의 일관성, 청크 언로드 후 재등록, 저장·복구.

### 3.2 보상 결정론 (미검증)
- "시드·인스턴스 ID·역할의 순수 함수" 라는 설계는 **§5 의 인스턴스 ID 가 안정적이어야 성립한다.** ID 가 확정되기 전에는 "재부팅 후 동일 보상 보장" 을 주장하지 않는다.
- 54슬롯 출금 전용 제약(`RoyalChestMenu`)도 해당 런타임 코드로 재검증 대상이다.

### 3.3 조우 격발 흐름 (설계 후보)
```
[플레이어 상호작용] -> 상자 클릭 이벤트 -> 봉인 여부 확인
   |-- 봉인 중 & PRIMARY  -> TombGuardianEncounters.onRoyalChestInteraction(...)
   |                          결과 StartResult 에 따라 안내/대기
   |-- 봉인 해제(DEFEATED) -> 보상 인벤토리 로드(없으면 생성) -> 메뉴 오픈
```
실제 호출 시그니처는 `TombGuardianEncounters.java` 기준이며, 위 "봉인 중" 판정은 `isRoyalChestSealed(level, anchor)` 로 존재한다.

---

## 4. Section C: Tomb Guardian 조우 수명주기

### 4.1 식별자
- `tombKey(anchor) = anchor.asLong()` — 코드에 존재 (`TombGuardianEncounters.java:62`).
- `bossSpawn` 은 `(-21, -22, 26)` 의 월드 좌표.
- 구조물 회전·배치 시 월드 좌표 변환 방식은 **아직 구현되지 않았으므로 미검증**.

### 4.2 실제 코드의 상태 결과 (`StartResult`)
v1.0.0 의 상태명(DORMANT, ACTIVE, RESTART_AFTER_LOSS 등)은 설계 제안이었으며, 실제 코드는 아래 결과값으로 구분한다 (`TombGuardianEncounters.java`, GameTest `TombGuardianGameTests.java`).

| 코드 결과 | 의미 (코드 주석 기준) | 설계 상태 대응 |
| --- | --- | --- |
| `STARTED` | 새 가디언 스폰, 각성 시작 | 각성 시작 |
| `RESTARTED` | 이전 가디언이 사라짐(죽지 않고 소멸), 새로 스폰 | 재도전 |
| `ALREADY_ACTIVE` | 살아있는 가디언이 이미 바인딩됨 | 전투 중 중복 클릭 무시 |
| `GUARDIAN_NOT_LOADED` | 바인딩된 가디언 청크가 로딩 중 | 재상호작용 요청 |
| `ALREADY_DEFEATED` | 영구 처치 기록 있음 | 재소환 불가 |

- 봉인: `isRoyalChestSealed` 는 처치 전까지 true, `markDefeated` 이후 false (GameTest 로 검증 기술됨, **이 세션에서 실행하지 않음**).
- 언로드는 처치로 기록되지 않는다 (`GameTest: "unload is never a defeat"`, 실행 미검증).

### 4.3 영속성 항목별 분석
| 항목 | 현재 근거 | 상태 |
| --- | --- | --- |
| 디스폰 방지 | `TombGuardian.java:95` 에서 `setPersistenceRequired()` 호출 | 호출 확인, 실제 효과는 미검증 |
| 엔티티 영속 저장 | 엔티티 UUID 를 `TombEncounterData` 에 바인딩 (GameTest 기술) | 코드 존재, 재시작 후 동작 미검증 |
| 인카운터 상태 저장 | `TombEncounterData` (`src/main/java/.../boss/TombEncounterData.java`) | 존재, 저장 포맷 검토 필요 |
| 중복 스폰 방지 | `ALREADY_ACTIVE` / `GUARDIAN_NOT_LOADED` 분기 | 코드 존재, GameTest 미실행 |
| 재시작 후 상태 복원 | 설계 주장 | **미검증** |
| 사망·재도전 정책 | `RESTARTED` 분기 | 정책 자체는 사용자 WHAT 확인 필요 (사망 시 처리 미정) |
| 상자 해금과의 일관성 | `markDefeated` → 봉인 해제 | 코드 존재, 미실행 |

`setPersistenceRequired()` 는 "모든 재시작·언로드 문제 해결"을 의미하지 않는다. 위 항목을 P8-B/P8-C 에서 각각 검증한다.

---

## 5. Section D: 구조물 인스턴스 식별 (PROPOSED / UNAPPROVED)

> 이 섹션의 어떤 후보도 **승인되지 않았다.** 보상 영속성에 영향을 주므로 P8-B 착수 전에 사용자 확인이 필요하다 (STOP & ASK 대상).

### 5.1 v1.0.0 제안의 결함
v1.0.0 의 형식 `st01:<dim>:<chunkX<<32 ^ chunkZ>` 에는 다음 결함이 있다.
1. **타입 결함**: `chunkX` 가 Java `int` 이면 `chunkX << 32` 의 시프트 거리는 `32 & 31 = 0` 이 되어 식이 `chunkX ^ chunkZ` 로 축약된다. 이 경우 `(a,b)` 와 `(b,a)` 가 충돌한다. 예시 값 `48103957291` 은 32비트 범위를 넘으므로 `long` 이 필요하며, 그 경우에도 연산 순서·부호 확장을 명시해야 한다.
2. **공간 유일성 가정 미증명**: "구조물 시작 청크는 고유하다"는 주장은 같은 구조물 세트 안에서만 성립할 수 있다. 서로 다른 구조물 타입이 같은 청크를 시작점으로 가질 수 있으므로 식별자에 구조물 타입/세트를 포함해야 한다. 월드젠 배치 코드는 아직 없으므로 이 점은 **미검증**이다.
3. **"0 바이트"·"보상 복구 보장"**: 근거 없음, 삭제.

### 5.2 대체안 비교 (검토용)

| 비교 항목 | A: 기존 `tombAnchor` 키 | B: 수정된 청크 키 (`long` 패킹) | C: 저장소(SavedData) 원장 |
| --- | --- | --- | --- |
| 구현 근거 | **코드에 이미 존재** (`tombKey = anchor.asLong()`) | 신규 | 신규 |
| 충돌 | 같은 월드 좌표에 두 구조물이 겹치지 않으면 유일 | `(x,z)` 패킹은 단사(injective) 로 충돌 없음, 구조물 타입 추가 필요 | 충돌 없음 |
| 재배치·회전 시 | 앵커 좌표가 바뀌면 키도 바뀜 (재배치 = 새 인스턴스로 취급) | 시작 청크가 같으면 유지 | 원장 기준 유지 |
| 데이터 의존 | 없음 | 없음 | 파일 유실 시 매핑 깨짐 |
| 미검증 항목 | 같은 좌표에 복수 구조물 불가 여부 | 구조물 시작 청크 계산 방식 | 원장 동기화 |

**권고(검토용):** A 를 기본 후보로 두고 "구조물 1개 = tombAnchor 1개" 가 월드젠 배치로 보장되는지 P8-B 에서 검증한다. 보장되지 않으면 B 를 쓰되 구조물 타입을 함께 인코딩한다. **최종 선택은 사용자 확인 후 진행한다.**

---

## 6. Section E: 전투 판정 — 정적 검증 범위와 예외

### 6.1 6.55초 초기 대응창
- 산출 근거: 각성 80틱 + 13틱 + 26틱 + 12틱 = **131틱 = 6.55초** (`docs/design/st01-p7/generated/p7_verification.json:778-779`, `tools/st01-p7/st01_p7_verify.py:257`).
- 적용 범위: **정적 타이밍 모델의 결과**이다. 실제 Java 틱 처리, GameTest, 실제 클라이언트 전투로 확인되지 않았다.
- 코드 상수: `TombGuardianTuning.AWAKENING_TICKS = 80` 은 존재 확인. 13/26/12틱 항목의 코드 상수는 **미확인**.

### 6.2 단상 충격파 면역
- 코드: `TombGuardianCombat.waveHits` 는 `feetAboveFloor >= WAVE_JUMP_CLEARANCE(0.6)` 이면 충격파를 맞지 않는다 (`TombGuardianCombat.java:52-53`, `TombGuardianTuning.java:48`).
- 적용 조건과 예외:
  - 적용되는 것: **이 충격파 함수**의 판정, 발 높이가 바닥보다 0.6m 이상일 때.
  - "100%" 가 아닌 것: 충격파 외 공격(`statueHits`, 근접 공격, 소닉 차지 등)은 같은 면역을 받지 않는다. 석상 판정은 `feetAboveFloor > STATUE_HEIGHT` 에서 제외되지만 면역 조건이 다르다.
  - 경계 조건: `feetAboveFloor < -WAVE_FLOOR_TOLERANCE` 인 경우도 충격파 미적중. 단상 위 2.0m 조건은 **정적 모델의 기하** 이며 플레이어 점프·넉백·충돌 판정은 미검증.
- 따라서 "실제 게임 전체에서 무조건 면역" 으로 확대하지 않는다.

### 6.3 검증 단계 구분
| 단계 | 현재 상태 |
| --- | --- |
| 정적 모델 검증 | 완료 (P7 도구, 생성 JSON) |
| 실제 Java 코드 판정 | 함수 존재 확인, 실행 미검증 |
| Fabric GameTest | 테스트 소스 존재 (`TombGuardianGameTests.java`), **이 세션에서 실행하지 않음** |
| 실제 클라이언트 전투 | **LIVE_SERVER_UNVERIFIED** |

---

## 7. Section F: 월드젠 정책 (미승인 항목)

다음은 승인되지 않았으며 P8-C 에서 사용자와 확정한다.
1. 생성 바이옴 후보 `minecraft:desert`, `minecraft:badlands` — UNAPPROVED
2. `spacing = 32`, `separation = 8`, `salt = 0x53543031` — UNAPPROVED, 바닐라 값과의 비교 근거 없음
3. `terrain_adaptation = BEARD_THIN | BURY`, Y 앵커링 — UNAPPROVED

---

## 8. Section G: 단계 로드맵과 Gate

```
[P8-A] 구조물 NBT 및 기하 배치 검증 (오프라인, 보상 런타임 불필요)
   |  - P6/P7 EAST 모델 기반 NBT 생성, 좌표 정합성 검증
   |  - PRIMARY 2블록 BlockState 확정 (바닐라 ChestBlock 대조)
   |  - Guardian 3B 스폰 공간 검증
   v
[P8-B] 런타임 연결 (선행: 보상 런타임 가져오기 승인 + §5 ID 확정)
   |  - 원격 implement 브랜치의 RoyalChest 런타임 도입 (병합 승인 필요)
   |  - PRIMARY 양쪽 클릭 라우팅, 인스턴스 ID 적용
   |  - TombGuardianEncounters 와 봉인 해제 연동
   v
[P8-C] 월드젠 및 인게임 검증
   |  - 월드젠 JSON(structure_set, template_pool, biome_tag) — §7 승인 후
   |  - GameTest 실행, 실제 서버·클라이언트 검증
   v
   LIVE_SERVER_VERIFIED 획득 시에만 완료
```

### Gate
- **P8-A Gate**: 생성된 NBT 를 오프라인 파서로 읽어 PRIMARY 2블록과 스폰 좌표가 승인 좌표와 오차 0 으로 일치.
- **P8-B Gate**: 런타임 GameTest 에서 두 반쪽 클릭 라우팅, 처치 후 봉인 해제, 중복 소환 차단이 실행으로 통과.
- **P8-C Gate**: 실제 서버·클라이언트에서 자연 생성, 보스 전투, 재시작 후 상태 유지 확인 (`LIVE_SERVER_VERIFIED`).

---

## 9. Section H: P8-A 실행 지시서 (독립 세션용)

이 지시서는 새 세션에서 단독으로 실행 가능하도록 작성되었다. **이번 세션에서는 실행하지 않는다.**

### 9.1 기준선 (BASE)
- 브랜치: `work/st01-p7-east-primary-guardian-20261108`
- 커밋: `363f45f4d64dc22de54bfc439a48d4f89cbc3aa4` (P7-S), 부모 `86d50ec1`, `a260fcab`, `06a262d7`
- 시작 전 `git rev-parse HEAD` 가 위 SHA 와 일치하는지 확인한다. 불일치하면 중단하고 보고한다.

### 9.2 승인된 좌표 (변경 금지)
- EAST 방향 (D-I, USER APPROVED)
- PRIMARY B: `(-21,-20,33)`, `(-20,-20,33)`, 북쪽 정면 2블록 (P3-F6, USER APPROVED)
- Tomb Guardian 3B: `(-21,-22,26)`, Yaw `0.0F` (D-E, USER APPROVED)
- `tombAnchor = (-21,-20,33)` (기존 키, 변경 금지)

### 9.3 수정 대상 파일 (후보, 시작 전 확인)
- 구조물 NBT 신규: `src/main/resources/data/moreminecrafts/structure/` 아래 기존 템플릿 디렉터리 구조를 따라 작성 (예: `git ls-files 'src/main/resources/data/*/structure*'` 로 기존 위치 확인)
- 좌표 원천: `docs/design/st01-p6/generated/p6_model_east.json`, `docs/design/st01-p7/generated/p7_model_east.json`
- 검증 도구: `tools/st01-p7/st01_p7_verify.py` 패턴을 따라 `tools/st01-p8/` 에 NBT 파서 검증 스크립트 신설 가능

### 9.4 구현 범위 (P8-A 만)
1. P6/P7 EAST 모델 기반 구조물 NBT 생성 (피라미드 본체, 홀, 석관, 제의 광장)
2. PRIMARY 2블록 `minecraft:chest` 배치 (facing/type 은 바닐라 DoubleBlockCombiner 로 확인 후 확정)
3. Guardian 스폰 기준점 `(-21,-22,26)` 의 빈 공간 확보
4. 회전·Mirror 변환 검증 (월드 좌표 대조)

### 9.5 수정 금지 (명시)
- 보상 런타임(`RoyalChest*`) 코드·병합 금지 (P8-B 범위)
- `TombGuardianEncounters`, `TombGuardianCombat`, `TombGuardianTuning` 등 전투 코드 변경 금지
- 월드젠 JSON(`structure_set`, `template_pool`, `biome_tag`) 등록 금지 (§7 승인 전)
- SECONDARY 상자 좌표 배치 금지 (미승인)
- `main` 수정, 브랜치 삭제, 병합, force push 금지
- 새로운 설계 결정 금지 (좌표·방향 변경 필요 시 중단하고 보고)

### 9.6 자동 테스트
| # | 항목 | 자동 가능 여부 |
| --- | --- | --- |
| 1 | NBT 로드 및 구조물 무결성 | 자동 (오프라인 파서) |
| 2 | 블록 배치와 승인 좌표 일치 | 자동 |
| 3 | PRIMARY 방향 및 좌우 타입 | 자동 (바닐라 규칙 대조) |
| 4 | Guardian 스폰 공간 (3×4×3 빈 공간) | 자동 |
| 5 | 석관·계단·벽 충돌 | 자동 |
| 6 | 회전·대칭 변환 | 자동 |
| 7 | 구조물 경계 및 청크 경계 | 자동 |
| 8 | 실제 클라이언트 시각 검증 | **수동, LIVE VALIDATION PENDING** |

### 9.7 인게임 검증
- 이 단계에서는 인게임 검증을 완료로 보고하지 않는다. 시각 검증은 `LIVE VALIDATION PENDING` 으로 표기한다.

### 9.8 커밋·PR 정책
- 동일 브랜치에 커밋, 원격 fast-forward 푸시만 허용, force push 금지.
- 기존 Draft PR(작성 시점 #246) 에 추가 커밋으로 반영. 새 PR 생성 금지.
- 커밋 메시지에 모델 식별자 포함 금지.

### 9.9 완료 기준
- 자동 테스트 1–7 전부 실행하여 통과 또는 실패를 보고
- 실행하지 않은 항목은 `NOT RUN` 으로 표기
- 좌표 불일치 0 확인 결과 첨부
- `LIVE VALIDATION PENDING` 항목 명시
- 보고 후 중단 (다음 단계 자동 시작 금지)

---

## 10. 미결정 사항 (사용자 WHAT / 확정 필요)

| # | 항목 | 상태 |
| --- | --- | --- |
| 1 | 보상 런타임(`RoyalChest*`) 이 원격 implement 브랜치에만 있음 — 이 브랜치로 가져올지 | 승인 필요 |
| 2 | 구조물 인스턴스 식별 키 (§5 A/B/C) | 승인 필요 |
| 3 | SECONDARY 상자 좌표 및 방향 | 미승인 |
| 4 | 가디언 사망·재도전 정책 (사망 시 처리) | 미승인 |
| 5 | 월드젠 바이옴·spacing·separation·terrain | 미승인 |
| 6 | 석관 장식 블록 구성 | 미승인 |

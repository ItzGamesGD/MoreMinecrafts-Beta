# ST-01 P8 — 구현 준비 아키텍처 및 단계별 통합 계획서 (Pre-Implementation Plan)

문서 ID: `ST01_P8_PRE_IMPLEMENTATION_PLAN_2026-11-08`  
버전: `v1.0.0` (P7-S 산출물)  
작업 브랜치: `work/st01-p7-east-primary-guardian-20261108`  
기준 커밋: `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (`origin/work/st01-p6-final-spec-20261107`)  
선행 승인: D-I=EAST, P3-F6=PRIMARY 후보 B, D-E=Tomb Guardian 후보 3B (**USER APPROVED**)

---

## 1. 개요 및 범위 경계 (Scope & Boundaries)

본 문서는 사용자에 의해 최종 승인된 **D-I = EAST 방향**, **P3-F6 = PRIMARY 2블록 상자 후보 B**, **D-E = Tomb Guardian 후보 3B** 결정을 바탕으로, 실제 Fabric 모드 `MoreMinecrafts`에 ST-01 피라미드 구조물을 구현하기 위한 **선행 아키텍처 명세 및 단계별 통합 로드맵**을 수립한다.

> [!IMPORTANT]
> **구현 착수 전 엄격한 경계 준수 (Non-Implementation Boundary)**  
> 1. 본 문서는 구현 **준비 및 설계** 단계이며, 실제 Minecraft 구조물 NBT 파일 생성, `worldgen` 레지스트리 등록, `RoyalChestRuntime.register(...)` 실제 호출 코드는 **이번 세션에서 일체 작성하거나 커밋하지 않는다**.  
> 2. 실제 서버/클라이언트 환경에서의 인게임 보스 전투와 연출은 여전히 **`LIVE_SERVER_UNVERIFIED`** 상태로 관리되며, P8-C 단계의 Fabric GameTest 및 인게임 테스트베드 검증 전까지 완료로 간주하지 않는다.  
> 3. 모든 WHAT 결정(기하, 방향, 게임플레이 승인)은 사용자의 고유 권한이며, 어시스턴트는 HOW(기술적 구현 경로, 안전성 검증, 데이터 직렬화)의 대안을 분석하고 제시한다.

---

## 2. Section A: 구조물 형상 및 NBT 매핑 (Structure Shape & NBT Mapping)

### 2.1 기준 좌표계 및 상대 좌표 변환
P6 최종 통합 모델(`docs/design/st01-p6/ST01_P6_INTEGRATED_SPEC_2026-11-07.md`)에서 확정된 ST-01 EAST 피라미드의 국소 좌표계(Local Coordinate System, $LCS$)를 구조물 NBT 바이너리 좌표계로 매핑한다.

- **원점 피벗(Pivot Point)**: 피라미드 중앙 기단 중심 $(X=0, Y=0, Z=0)$
- **홀 영역 Bounding Box**: $X \in [-24, 0], Y \in [-25, 0], Z \in [0, 40]$
- **월드 절대 좌표 변환 공식**:
  $$\vec{P}_{world} = \vec{P}_{structure\_origin} + \mathbf{R}(\text{rotation}) \cdot \mathbf{M}(\text{mirror}) \cdot \vec{P}_{local}$$
  여기서 $\mathbf{R}$은 Minecraft의 `Rotation` (`NONE`, `CLOCKWISE_90`, `CLOCKWISE_180`, `COUNTERCLOCKWISE_90`), $\mathbf{M}$은 `Mirror` (`NONE`, `LEFT_RIGHT`, `FRONT_BACK`) 변환 매트릭스이다.

### 2.2 승인된 주요 지물 NBT 블록 상태 (BlockState) 명세

1. **PRIMARY 왕실 상자 (후보 B - USER APPROVED)**:
   - **점유 좌표**: `(-21, -20, 33)`, `(-20, -20, 33)` (2블록 대형 상자)
   - **방향(Facing)**: `NORTH` (-Z, 왕실 홀 입구 방향을 향함)
   - **NBT BlockState**:
     - `(-21, -20, 33)`: `minecraft:chest[facing=north, type=right, waterlogged=false]`
     - `(-20, -20, 33)`: `minecraft:chest[facing=north, type=left, waterlogged=false]`
   - **상호작용 접근점**: `(-21, -20, 32)` 및 `(-20, -20, 32)` (단상 위 $Y=-20$, 전면 1블록 통로).
   - **간섭 검증**: 북쪽 개폐 시 후방 석관($Z=35$)과 2블록 여유 확보, 석관 몸체 충돌 0%.

2. **SECONDARY 왕실 상자 (보조 보관함)**:
   - **점유 좌표**: `(-3, -22, 25)`, `(-3, -22, 26)` (동쪽 비밀 구역 입구 인접 보물 벽감)
   - **방향(Facing)**: `WEST` (+X 방향 벽을 등지고 중앙 광장을 향함)
   - **NBT BlockState**:
     - `(-3, -22, 25)`: `minecraft:chest[facing=west, type=right, waterlogged=false]`
     - `(-3, -22, 26)`: `minecraft:chest[facing=west, type=left, waterlogged=false]`

3. **석관 (Sarcophagus) 및 묘실 앵커 (`tombAnchor`)**:
   - **석관 점유 영역**: $X \in [-22, -20], Y \in [-22, -20], Z \in [35, 37]$ (장식 블록: 깎은 사암, 금 블록, 레드스톤 토치 매립)
   - **`tombAnchor` 고정 좌표**: `(-21, -20, 33)` (PRIMARY 상자의 기준 좌측 반쪽이자 조우 식별 키).

4. **Tomb Guardian 출현 지점 (후보 3B - USER APPROVED)**:
   - **스폰 좌표**: `(-21, -22, 26)`
   - **시선 방향(Facing/Yaw)**: `SOUTH` (Yaw = `0.0F`, 북쪽 입구에서 진입하여 남쪽 상자로 접근한 플레이어를 정면 응시)
   - **클리어런스**: 폭 3블록($X=-22..-20$), 높이 4블록($Y=-22..-19$), 깊이 3블록($Z=25..27$) 완전 빈 공간 확보.

---

## 3. Section B: 왕실 상자 런타임 연동 (RoyalChestRuntime Integration)

I06-P3 단계에서 구현된 독립 보상 엔진 `RoyalChestRuntime` 및 `RoyalChestData`(`commit 97afeb66`)와의 완벽한 연동 규격을 정의한다.

### 3.1 2블록 상자 클릭 처리 및 인스턴스 매핑
- **기존 런타임 제약**: `RoyalChestRuntime.register`는 현재 단일 `BlockPos primary`만을 인자로 수신하며, `RoyalChestData.byPos` 맵에 단일 좌표만 등록함.
- **P8-B 연동 개선안 (선택 옵션)**:
  - **옵션 1 (권장 - 런타임 양방향 등록)**:  
    `register` 메서드를 확장하여 PRIMARY의 두 좌표 `primaryLeft=(-21, -20, 33)`와 `primaryRight=(-20, -20, 33)`를 모두 `RoyalChestData.byPos`에 `Link(instance, RoyalChestRole.PRIMARY)`로 등록한다.
    - 장점: `UseBlockCallback`에서 추가적인 월드 블록 조회(`level.getBlockState(pos)`) 없이 $O(1)$ 해시맵 조회로 즉시 식별 가능.
  - **옵션 2 (DoubleBlockCombiner 동적 결합)**:  
    `linkAt(pos)` 실패 시 해당 블록이 `ChestBlock`이고 `ChestType != SINGLE`인 경우 인접한 연결 블록을 찾아 재조회.
    - 단점: 상자가 파괴되거나 블록 상태가 비정상일 때 월드 I/O 오버헤드 발생.
- **결론**: **옵션 1(좌표 동시 등록 방식)**을 채택하여, 플레이어가 대형 상자의 좌/우 어느 반쪽을 클릭하더라도 동일한 `RoyalChestRole.PRIMARY` 인스턴스로 라우팅한다.

### 3.2 중복 보상 지급 방지 및 보존 일관성
- **단일 인벤토리 공유**: `RoyalChestData.Chest`의 `RoyalChestContainer`는 54슬롯 단일 컨테이너로 관리된다. 두 블록 중 어느 쪽을 열어도 동일한 가상 인벤토리가 오픈된다.
- **출금 전용 (Withdraw-Only)**: `RoyalChestMenu`의 제약에 따라 플레이어는 보상을 꺼낼 수만 있으며, 아이템을 넣거나 덮어쓰는 것이 불가능하다.
- **시드 고정 결정론적 드랍**: 보상 생성은 `(level.getSeed(), structureInstanceId, role)`의 순수 함수로 구동되므로, 서버 재부팅이나 청크 언로드 후 다시 생성되어도 동일한 아이템 스택이 보장된다.

### 3.3 조우 격발 및 봉인 해제 전이
```
[플레이어 상호작용] -> UseBlockCallback -> RoyalChestRuntime.tryOpen(pos)
       |
       v
  [isRoyalChestSealed?] --(YES: 봉인 중)--> [Role이 PRIMARY인가?]
       |                                           |
       |                                    (YES)  |  (NO: SECONDARY)
       |                                      v    v
       |                    TombGuardianEncounters.onRoyalChestInteraction
       |                                      v
       |                    Guardian 출현 (STARTED / RESTARTED) -> 안내 메시지
       |
       +--(NO: 가디언 처치 완료 / DEFEATED)-->
       |
       v
  [보상 인벤토리 기생성 여부?]
       |--(아직 미생성)--> RoyalChestRewardModel.chestRewards(...) 생성 및 저장
       |--(기생성됨)-----> 기존 저장된 RoyalChestContainer 로드
       v
  player.openMenu(RoyalChestMenu) -> 54슬롯 보상 UI 오픈!
```

---

## 4. Section C: Tomb Guardian 조우 수명주기 (Encounter Lifecycle)

### 4.1 월드 절대 좌표 변환 및 `tombAnchor` 식별자
- `tombAnchor`: 피라미드 구조물 내에서 유일하고 불변인 64비트 공간 식별자(`BlockPos.asLong()`)로 사용된다.  
  - EAST 기준: `(-21, -20, 33)`의 월드 절대 좌표.
- `bossSpawn`: `(-21, -22, 26)`의 월드 절대 좌표.
- 구조물이 회전/배치될 때 `StructurePiece`의 배치 로직에서 월드 좌표로 변환되어 전달된다.

### 4.2 조우 상태 머신 (Encounter State Machine)
1. **DORMANT (동면 상태)**:
   - 가디언 미소환, 상자 봉인 상태 (`isRoyalChestSealed = true`).
2. **AWAKENING (각성 연출, 80틱 / 4.0초)**:
   - 플레이어가 PRIMARY 상자를 클릭하는 순간 `TombGuardian` 엔티티가 `bossSpawn`에 스폰됨.
   - 보스는 석상 포즈에서 각성하며 무적 및 이동 불가 상태 유지.
   - 사운드 및 모래 폭풍 파티클 방출.
   - 플레이어는 단상 위에서 6.55초의 초기 대응 여유 시간 확보.
3. **ACTIVE (전투 상태)**:
   - 보스 AI 활성화, 지면 강타(Ground Smash) 및 근접 공격 개시.
   - 단상($Y=-20$) 위에 머무는 플레이어는 2.0m 고도차로 인해 지면 충격파에 면역(`feetAboveFloor >= 0.6m`).
4. **DEFEATED (토벌 완료 및 봉인 해제)**:
   - 보스 체력 0 도달 시 `TombGuardianEncounters.DEFEATED` 이벤트 발화.
   - `TombEncounterData`에 영구 기록되어 재소환 영구 차단.
   - 왕실 상자 봉인 즉시 해제, SECONDARY 상자 개폐 허용.

### 4.3 예외 상황 복구 (Fail-Safe & Anti-Abuse)
- **플레이어 사망 또는 도주**: 보스가 플레이어를 잃고 디스폰되거나 비정상 소멸한 경우, 상태 머신은 `RESTART_AFTER_LOSS`로 전이되어 다음 상자 클릭 시 재도전을 허용한다.
- **서버 재시작 / 청크 언로드**:
  - `TombGuardian`은 `setPersistenceRequired()`가 호출되어 청크 언로드 시에도 디스폰되지 않는다.
  - 엔티티 UUID가 `TombEncounterData`에 바인딩되어 서버 재부팅 후에도 조우 상태를 유지한다.
- **중복 소환 원천 차단**: 이미 ACTIVE 상태인 동안의 추가 클릭은 무시된다(`ALREADY_ACTIVE`).

---

## 5. Section D: 구조물 인스턴스 식별자 체계 (Structure Instance Identity)

`RoyalChestRuntime`은 고유한 `structureInstanceId` 문자열을 요구한다. 피라미드마다 완전히 독립된 보상 인벤토리를 부여하기 위한 3가지 식별자 경로를 비교·평가한다.

| 비교 항목 | 후보 1: 시작 좌표 공간 해시 (추천) | 후보 2: SavedData 증분 원장 | 후보 3: NBT 블록엔티티 태그 |
| --- | --- | --- | --- |
| **방식** | `st01_<dim>_<chunkX>_<chunkZ>` | 서버 `SavedData`에서 UUID 발급 | NBT 구조물 내 CommandBlock/Sign 주입 |
| **결정론성** | **100% 완전 결정론적** (시드 고정) | 비결정론적 (생성 순서 의존) | 부분 결정론적 |
| **영속성** | 청크 재로드/재배치 시에도 항상 동일 | 데이터 파일 유실 시 ID 매핑 깨짐 | NBT 손상 위험 |
| **저장 비용** | 0 바이트 (좌표로부터 즉시 계산) | 별도 ID 원장 테이블 영구 저장 필요 | NBT 데이터 오버헤드 |
| **복구 용이성** | 좌표만 알면 즉시 보상 복구 가능 | 역방향 조회 불가 | 블록 파괴 시 소실 |

### 권장 경로: 후보 1 (공간 좌표 기반 안정적 해시)
- **식별자 형식**:  
  `st01:` + `dimension_name` + `:` + `(chunkX << 32 ^ chunkZ)` (예: `st01:minecraft_overworld:48103957291`)
- **선정 사유**: Minecraft의 구조물 생성 시스템(`StructureStart`)은 시작 청크 좌표를 고유하게 보유하므로, 별도의 상태 저장소 없이도 영구적이고 충돌 없는 인스턴스 ID를 즉시 생성할 수 있다.

---

## 6. Section E: 월드젠 정책 (Worldgen Policies - 미승인 항목)

다음 월드젠 세부 정책은 아직 사용자 승인이 완료되지 않은 상태이며, P8-C 단계에서 논의 및 확정해야 할 항목이다.

1. **생성 바이옴 (Biomes)**:
   - 권장 후보: `minecraft:desert` (사막), `minecraft:badlands` (악지).
   - 제약: 해양, 산악, 강 바이옴 제외 (`#moreminecrafts:has_structure/st01_pyramid` 바이옴 태그 정의 필요).
2. **생성 빈도 및 간격 (Spacing & Separation)**:
   - 기준 제안: `spacing = 32`, `separation = 8`, `salt = 0x53543031` (바닐라 사막 피라미드와 유사한 희귀도 유지).
3. **지형 적응 및 높이 분포 (Terrain Adaptation & Elevation)**:
   - `terrain_adaptation = BEARD_THIN` 또는 `BURY`: 피라미드 기단 하부가 지형 굴곡으로 인해 공중에 뜨는 현상 방지.
   - $Y$ 오프셋: 입구 바닥이 주변 평균 지표면($Y=63..70$)과 완만하게 연결되도록 높이 앵커링 설계 필요.

---

## 7. Section F: 단계별 분할 실행 계획 (P8 Roadmap)

ST-01의 실제 구현은 검증 가능성을 극대화하고 리스크를 최소화하기 위해 **3개의 하위 단계**로 분할하여 실행한다.

```
+---------------------------------------------------------------------------------+
|                                 ST-01 P8 ROADMAP                                |
+---------------------------------------------------------------------------------+
|                                                                                 |
|   [P8-A] 구조물 NBT 및 기하 배치 검증 (Structure NBT & Spatial Layout)          |
|   - P6 EAST 모델 기반 NBT 구조물 파일 컴파일 (피라미드 본체, 비밀 구역)          |
|   - PRIMARY B (2블록) 및 SECONDARY 상자 BlockState 배치                          |
|   - Tomb Guardian 3B 스폰 공간 및 석관 기하 완성                                |
|   - Voxel 단위 기하 무결성 및 구조물 로더 오프라인 검증                          |
|                                       |                                         |
|                                       v                                         |
|   [P8-B] 인스턴스 ID 체계 및 런타임 연결 (Instance ID & Runtime Wiring)        |
|   - StructureStart 기반 공간 해시 인스턴스 ID 생성기 구현                       |
|   - RoyalChestRuntime 2블록 PRIMARY 상자 등록 확장                              |
|   - TombGuardianEncounters 조우 트리거 및 봉인 해제 이벤트 연동                |
|   - Mock / 단위 테스트를 통한 런타임 상태 전이 검증                              |
|                                       |                                         |
|                                       v                                         |
|   [P8-C] 월드젠 등록 및 인게임 종합 검증 (Worldgen & Integration Verification)  |
|   - 월드젠 JSON 설정 (structure_set, template_pool, biome_tag) 등록            |
|   - Fabric GameTest 작성: 자동 스폰, 가디언 전투 판정, 보상 메뉴 오픈 검증      |
|   - LIVE_SERVER_VERIFIED 전환 및 최종 병합 PR 준비                              |
|                                                                                 |
+---------------------------------------------------------------------------------+
```

### 각 하위 단계별 통과 기준 (Gate Criteria)
- **P8-A Gate**: 구조물 NBT가 생성되고, 오프라인 NBT 파서로 검증했을 때 PRIMARY 상자 2블록과 Guardian 스폰 좌표가 승인 좌표와 오차 0으로 일치할 것.
- **P8-B Gate**: Fabric 단위 테스트 환경에서 상자 클릭 시 가디언이 소환되고, 가디언 처치 시 봉인이 풀려 54슬롯 보상 창이 정상 출력될 것.
- **P8-C Gate**: 실제 Minecraft 클라이언트 및 전용 서버에서 구조물이 자연 생성되고 프레임 드랍, 청크 깨짐, 보스 전투 이상이 없을 것 (`LIVE_SERVER_VERIFIED` 획득).

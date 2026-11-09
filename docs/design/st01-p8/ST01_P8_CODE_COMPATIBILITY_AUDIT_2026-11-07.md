# ST-01 P8 — 기존 코드 호환성 감사 (2026-11-07)

상태: **읽기 전용 조사.** 저장소 코드는 수정하지 않았다. 구분: EXISTING(이미 있음), MISSING(없음), INCOMPATIBLE(승인된 정책과 맞지 않음). 인메모리 코드 읽기와 grep 결과이며 게임 내 동작은 UNVERIFIED이다.

## 1. 요약표

| # | 항목 | 판정 | 근거 |
| --- | --- | --- | --- |
| 1 | 처치 이벤트 발행 위치 | **EXISTING** | `TombGuardianEncounters.DEFEATED`(`:31`)는 `guardianDied`(`:138-143`)에서 `markDefeated`가 true일 때 1회 발행. `guardianDied`는 `TombGuardian.die()`(`:240-248`)의 `super.die` 이후 호출 |
| 2 | 영속 처치 기록 = 승리 인정? | **INCOMPATIBLE** | `die()`는 `isBound(...)`이면(`:243`) 사망 원인과 무관하게 `credited`로 보고 `markDefeated`(`TombEncounterData:127`)로 영구 기록한다. "사망"과 "승리"가 구분되지 않는다 |
| 3 | 전투 참여 플레이어 추적 | **MISSING**(보스) / **EXISTING**(관례) | `TombGuardian`에는 피해 기록이 없고 `getKillCredit()`(`:245`)만 쓴다. 저장소에는 선례 `progression/QuestMonumentParticipation`(피해 기반, 투사체·소유 엔티티는 플레이어로 귀속, 존재만으로는 증거 아님, 12,000틱 창)과 Fabric `ServerLivingEntityEvents` 사용(`ColosseumRuntime`, `QuestMonumentParticipation`)이 있다 |
| 4 | 보상 지급 조건 | **INCOMPATIBLE** | 보스 전리품은 루트 테이블 `loot_table/entities/tomb_guardian.json`(`ancient_heart` 50%, `random_chance` 조건만 있고 플레이어 처치 조건 없음)과 `shouldDropLoot = !lootSuppressed && super`(`:254`)로 결정된다. `lootSuppressed`는 고아·비바인딩 때만 켜진다(`:181, :244`). 따라서 바인딩된 보스가 환경 요인으로만 죽어도 전리품이 나올 수 있다(바닐라 드롭 경로는 UNVERIFIED) |
| 5 | 상자 봉인·해제 조건 | **INCOMPATIBLE**(조건) / **MISSING**(블록) | `isRoyalChestSealed = !isDefeated`(`:65-67`)이므로 비플레이어 사망도 봉인을 푼다. 상자 블록 자체와 호출부는 없다 |
| 6 | 구조물 인스턴스 식별 | **EXISTING**(키) / **MISSING**(구조물) | 무덤 키 = 차원별 SavedData + `tombAnchor.asLong()`(`TombGuardianEncounters:62`). ST-01 구조물·인스턴스 등록부는 없다 |
| 7 | 재시작·청크 복구 | **EXISTING**(보스) / **MISSING**(입구·골렘) | `TombEncounterData` SavedData(`:24-40`), `presence`/`NOT_LOADED` 처리(`TombGuardianEncounters:123-131`), 보스 청크 추적(`TombGuardian:176-186`). 입구 상태·골렘 영속은 없다 |
| 8 | `DEFEATED` 구독자 | **MISSING** | 운영 코드 구독 0, 게임테스트 1곳(`TombGuardianGameTests:48`)뿐 |
| 9 | 아이언 골렘 엔티티·렌더 | **MISSING**(사용) / **EXISTING**(선례) | `IronGolem` 사용 없음(`ContractCatalog` 문자열만). 선례: `Attributes.SCALE`을 믹스인으로 설정(`ZombieMixin:72`, `SkeletonMixin:81`), 바닐라 렌더러+텍스처만 교체(`CustomMobRenderers`), 커스텀 엔티티 `sized(w,h)` 등록과 바닐라 모델 스케일 렌더러(`TombGuardianEntities`, `TombGuardianRenderer`) |
| 10 | 라이브 검증 경로 | **EXISTING** | Fabric 게임테스트 러너와 `TombGuardianGameTests`(보스 단독 시나리오). 이번 세션은 Java 25/Minecraft 26.3 도구체인이 없어 실행하지 못했다 |

## 2. 충돌 해설과 변경 요청(승인 대기)

**CR-1 (INCOMPATIBLE 2·4·5의 공통 원인): 사망과 승리를 분리.** 정책 1.3은 "비플레이어 요인만의 사망은 승리가 아니며, 유효하지 않은 처치로 개방·보상이 없어야 한다"고 한다. 기존 코드는 영속 `defeated`가 곧 승리·상자 봉인 해제·전리품 조건이다. 둘을 구분하려면 운영 코드 변경이 필요하다.

| 항목 | 내용 |
| --- | --- |
| 범위 | `boss/TombGuardian.java`(`die`의 credited 판정), `boss/TombEncounterData.java`(승리 목록 + 하위 호환 코덱), `boss/TombGuardianEncounters.java`(`guardianDied`) 및 해당 단위 테스트 |
| 행동 | 처치 시 참여 판정(더미 명세 §3)을 거쳐 유효할 때만 `markDefeated`/`DEFEATED`/전리품. 무효 처치는 바인딩 해제(재도전 가능), `lootSuppressed=true` |
| 마이그레이션 | 기존 세이브의 `defeated`는 승리로 간주(필드 유지) |
| 위험 | 기존 보상 동작이 바뀐다(비플레이어 사망 시 전리품 소멸) → 사용자 승인 필요 |
| 상태 | **적용하지 않음. 승인 대기.** P8 프로토타입은 이 구분을 가정한 `EncounterStore`(`markDeath`/`markVictory`)로 격리 구현했다 |

**CR-2 (선택): 참여 추적.** `TombGuardian`을 고치지 않고 `ServerLivingEntityEvents.AFTER_DAMAGE`를 구독하는 신규 클래스로 구현 가능(`tombAnchor()` 접근자가 이미 공개). 선례 `QuestMonumentParticipation`의 귀속 규칙을 재사용한다.

## 3. 보류하지 않은 사실

- 기존 이벤트는 인메모리 신호이고 영속 기록이 진실이므로, 입구 상태는 항상 영속 기록에서 재조정해야 한다(P7과 동일, 프로토타입 T9).
- 전투 중 `die()`에서 이벤트가 호출되므로 리스너는 골렘 생성을 다음 틱으로 미뤄야 한다(프로토타입은 `pumpSpawn`을 틱 핸들러로 분리).

# ST-01 P7 — 기존 보스 코드 연결점과 제안 인터페이스 (2026-11-07)

상태: **읽기 전용 조사 + 인터페이스 제안.** 저장소 코드는 수정하지 않았다. "제안" 항목은 **아직 존재하지 않으며 구현된 것으로 표현하지 않는다.**

## 1. 기존 코드에서 확인한 사실

| 사실 | 위치 |
| --- | --- |
| 처치 이벤트 `Event<Defeated> DEFEATED`. 시그니처 `onDefeated(ServerLevel, BlockPos tombAnchor, @Nullable LivingEntity killer)` | `boss/TombGuardianEncounters.java:31` |
| 이벤트는 `guardianDied` 안에서 `markDefeated`가 true를 돌려준 *첫 한 번만* 호출된다 | `TombGuardianEncounters.java:138-143` |
| `guardianDied`는 `TombGuardian.die()`(내부, `super.die` 이후)에서 호출 → 보스 엔티티는 아직 사망 처리 중 | `TombGuardian.java:240-248` |
| 처치 기록은 `TombEncounterData`(SavedData `moreminecrafts:tomb_guardian_encounters`)에 `defeated` 목록으로 영속 | `TombEncounterData.java:24-40, 127-132` |
| `isRoyalChestSealed` / `isDefeated(level, tombAnchor)`는 영속 기록을 읽는다 | `TombGuardianEncounters.java:65-71` |
| 조우 시작은 `onRoyalChestInteraction(level, tombAnchor, spawnPos, player)`; 멱등, 동시 상호작용도 추가 보스를 만들지 않음 | `TombGuardianEncounters.java:77-104` |
| 처치 전 소멸(비사망 제거)은 바인딩을 해제해 재시도 가능; 청크 언로드는 바인딩 유지 | `TombGuardian.java:257-266` |
| `killer`는 null 가능(비플레이어 처치도 DEFEATED 발생) | `TombGuardianEncounters.java:37-39` |
| 보스 코드는 블록을 설치/파괴하지 않는다 | `boss/*.java` 전수 grep: `setBlock/destroyBlock/explode` 0건 |
| `DEFEATED`를 구독하는 **운영 코드는 없다**; 구독은 게임테스트 한 곳뿐 | `src/gametest/.../TombGuardianGameTests.java:48` |
| 금 골렘 엔티티·ST-01 구조물·비밀 입구 코드는 **없다** | `src/main` 전수 grep |

## 2. 설계에 미치는 영향

- **신호와 진실 분리:** 이벤트는 인메모리 호출이고 `defeated` 기록은 영속이다. 크래시·리스너 예외(배열 이벤트는 앞 리스너 예외가 뒤 리스너를 막을 수 있음)로 이벤트가 유실될 수 있으므로, 입구 상태는 이벤트가 아니라 `isDefeated`로 *재조정*해야 한다(시나리오 F6).
- **사망 처리 중 호출:** 리스너에서 골렘 엔티티를 즉시 생성하지 말고 다음 틱 작업으로 미룬다(보스 사망 처리 변경 금지 요건).
- **비플레이어 처치:** `killer == null`을 정당한 승리로 볼지는 열린 결정(O-9).
- **보스 재시도:** 처치 전 소멸은 `SEALED` 유지(입구 전이 없음).

## 3. 제안 인터페이스 (신규; 구현되지 않음)

| 책임 | 제안 형태 | 비고 |
| --- | --- | --- |
| 처치 이벤트 수신 | `TombGuardianEncounters.DEFEATED.register(coordinator::onDefeated)` — 신호만 받고 `coordinator.reconcile(level, anchor)` 호출 | 기존 이벤트를 *구독*할 뿐 기존 코드 변경 없음 |
| 금 골렘 생성 요청 | `GoldGuideSpawner.request(level, anchor, spawnPos)` → 결과 `OK/FAILED` | 다음 틱 실행. 실패 시 로그 + 타이머 유지 |
| 안내 상태 추적 | `SecretEntranceData` 레코드의 `state`, `guideUuid(옵션)` | 엔티티 UUID로 존재 확인 |
| 도착 판정 | `GuideProgress.arrived(guide, stopPoint, radius)` — 반경은 미승인 | 도착 구역은 별도 문서의 후보 중 선택 |
| 실패 타이머 | `remaining`(게임 틱)을 레코드에 저장, 틱 핸들러가 감소 | 값 미승인 |
| 덮개 개방 요청 | `CoverOpener.openIfNeeded(level, anchor)` — 멱등. 12칸 중 공기가 아닌 칸만 제거 | 청크 미로드 시 보류, 로드 시 재조정 |
| 영속 상태 | 신규 `SavedData` `moreminecrafts:st01_secret_entrance` (아래 필드) | 기존 `TombEncounterData`는 수정하지 않음 |

## 4. 호출 순서 제안 (의사 순서)

1. `DEFEATED(anchor)` 수신 → 큐에 `reconcile(anchor)` 등록(다음 틱).
2. `reconcile`: `isDefeated(anchor)` 아니면 종료. 레코드가 없으면 `GUARDIAN_DEFEATED`로 생성(남은 시간 설정) 후 골렘 생성 요청.
3. 생성 성공 → `GOLEM_GUIDING`. 생성 실패 → 상태 유지(타이머 계속).
4. 매 틱(무덤당 하나의 가벼운 핸들러): 남은 시간 감소; 골렘 도착 확인; 만료 시 `FALLBACK_OPEN`.
5. 열림 상태이고 덮개 청크가 로드되면 `CoverOpener.openIfNeeded` (멱등).

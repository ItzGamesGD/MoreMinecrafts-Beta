# ST-01 P9 — CR-1/CR-2 구현 및 저장 데이터 마이그레이션 (2026-11-08)

상태: **IMPLEMENTED / COMPILE PASS / UNIT PASS / GAMETEST PASS.** 게임 내 실플레이 검증은 하지 않았다(LIVE VERIFIED 아님).

## 1. 코드 변경 (운영)

| 파일 | 변경 |
| --- | --- |
| `boss/TombVictoryPolicy.java`(신규) | 참여 기록(`Participation`: 플레이어별 피해, 기타 피해, 마지막 플레이어 피해 틱), 판정(`judge`), 사망 원인 분류(`classify`: PLAYER/MOB/ENVIRONMENT/COMMAND_KILL), 소유자 귀속(`attributedPlayer`: 직접 공격자, 투사체 사수, `OwnableEntity` 주인), Fabric `AFTER_DAMAGE` 구독, 설치 가능한 `Params`(기본 없음) |
| `boss/TombGuardian.java` | 참여 기록 필드와 엔티티 NBT 저장/복원(`participation`), `recordDamage`, `die()`: **`super.die()` 이전에** 막타를 반영하고 판정해 무효이면 전리품을 억제한 뒤 `guardianDied`에 판정을 전달 |
| `boss/TombEncounterData.java` | `victories` 집합 추가(저장 필드 `victories`, 선택값), `markVictory`, `isVictory`, `legacyOnlyDefeats`, 명시적 `adoptLegacyAsVictory`. 기존 `markDefeated`는 하위 호환으로 유지 |
| `boss/TombGuardianEncounters.java` | `guardianDied(…, verdict)`: 유효 → `markVictory` + `DEFEATED` + 신규 `VICTORY`(참여자 포함), 무효 → 바인딩 해제(재도전 가능) + 신규 `INVALID_DEATH`. `DEFEATED`의 의미는 "유효한 승리"로 좁혀짐 |
| `boss/TombGuardianEntities.java` | `TombVictoryPolicy.initialize()` 호출 |

## 2. 의미 정의 (기존 기록 vs 신규 기록)

| 기록 | 의미 | 쓰는 곳 |
| --- | --- | --- |
| `defeated`(기존 저장 필드) | **무덤 완료**: 다시 소환되지 않고 상자 봉인 해제. P9 이전에는 바인딩된 보스의 *모든* 사망에 기록됨(유효성 불명). P9 이후에는 유효한 승리에만 기록 | 시작 거부(`ALREADY_DEFEATED`), `isRoyalChestSealed`, 디버그 명령 |
| `victories`(신규) | **검증된 유효 승리** (`defeated`의 부분집합) | 비밀 입구 진행(`McPorts.victory`) |
| 무효 사망 | 어느 기록에도 쓰이지 않음. 바인딩만 해제 → 무덤은 휴면 상태로 돌아가 재도전 가능 | — |

## 3. 판정 규칙 (운영, 수치 없음)

- 관리자 `/kill`(`GENERIC_KILL`): 항상 무효.
- `Params` 미설치(운영 기본): **막타가 플레이어(직접·투사체·소유 엔티티)에 귀속될 때만 유효**. 환경 피해(용암·낙사·허공), 다른 몹 막타는 무효.
- `Params(minShare, windowTicks)` 설치 시(TEST-ONLY 검증용): 참여자 존재 + 플레이어 누적 피해 ≥ 비율×최대 체력 + (플레이어 막타 또는 마지막 플레이어 피해가 창 이내). 공동 기여는 합산.
- 참여자 = 보스에게 실제 피해를 준 플레이어. 존재만으로는 증거 아님(`QuestMonumentParticipation`과 동일). 오프라인/재접속은 UUID 기록이라 영향 없음(전투 중 서버 재시작은 엔티티 NBT로 유지).

## 4. 마이그레이션 정책 (별도 보고)

- **기존 `defeated` 기록은 승리로 자동 변환하지 않는다.** 로드해도 `defeated`에만 남는다(유효성 불명 기록, 완료 상태 유지).
- 효과: 기존 완료 무덤은 계속 완료(재소환 없음, 상자 봉인 해제 유지) — 단 비밀 입구는 자동 진행하지 않는다.
- `legacyOnlyDefeats()`로 변환 대상 목록을 조회할 수 있고, 변환은 명시 호출 `adoptLegacyAsVictory(tomb)`로만 가능(운영 코드에서 호출하지 않음).
- 실제 영향 범위: ST-01 구조물/월드젠이 없으므로 운영 세이브에 ST-01 무덤 기록이 생길 경로는 디버그 명령뿐이다.
- 하위 호환: 신규 저장 필드 `victories`는 선택값(없으면 빈 목록). 이전 버전이 새 세이브를 읽어도 알려지지 않은 필드는 무시된다(`defeated` 의미는 유지). 보스 엔티티 NBT의 `participation`이 없는 이전 세이브는 빈 기록으로 읽는다.

## 5. 회귀 위험

[ST01_P9_REGRESSION_RISK_2026-11-08.md](ST01_P9_REGRESSION_RISK_2026-11-08.md) 참조.

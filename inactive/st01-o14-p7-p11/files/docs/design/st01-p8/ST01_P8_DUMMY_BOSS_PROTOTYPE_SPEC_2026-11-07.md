# ST-01 P8 — 더미 보스 프로토타입 및 승리 판정 명세 (2026-11-07)

상태: **IMPLEMENTED(격리 하네스)** + **제안(승리 판정 규칙)**. 파일: `tools/st01-p8/java/st01proto/{DummyBoss,ParticipationTracker,Ports}.java`. 최종 보스 크기·외형·공격·AI는 포함하지 않으며 결정하지 않는다.

## 1. 경계(교체 가능 인터페이스)

| 인터페이스 | 역할 | 실제 보스와의 연결(제안, 미구현) |
| --- | --- | --- |
| `BossLifecycle.setListener(Consumer<DefeatReport>)` | 사망 보고를 받는 쪽 | `TombGuardianEncounters.DEFEATED`를 어댑터가 `DefeatReport`로 변환 |
| `DefeatReport(id, validVictory, participants, reason)` | 사망 결과(유효 여부 포함) | CR-1 이후 보스 쪽이 판정 |
| `EncounterStore.markDeath/markVictory` | 사망과 승리의 분리 영속 | 기존 `TombEncounterData`는 단일 `defeated` → CR-1 필요 |
| `RewardPort.grant` | 보상 | 기존 전리품 로직은 건드리지 않음(더미는 카운터) |

## 2. 더미 동작

피해(플레이어/기타)를 받고 `die(cause, tick)`에서 판정 후 `store`를 갱신하고 보고를 발행한다. 공격·AI·외형·크기 없음. 유효 처치만 `markVictory`+`grant`, 무효 처치는 봉인 유지·보상 없음·재도전 허용.

## 3. 승리 판정 규칙(제안; 모든 수치는 TEST-ONLY)

기존 선례(`QuestMonumentParticipation`)와 일치: **참여자 = 보스에게 피해를 준 플레이어**(소유 엔티티·투사체는 주인에게 귀속), **존재만으로는 증거 아님**.

| 정책 | 정의 |
| --- | --- |
| ANY_DAMAGE | 플레이어가 한 번이라도 피해를 줌 |
| LAST_HIT_WINDOW | 마지막 플레이어 피해가 사망 직전 창 이내(플레이어가 막타면 항상 충족) |
| DAMAGE_SHARE | 플레이어 누적 피해 ≥ 최대 체력 × 비율 |
| COMBINED(권장) | DAMAGE_SHARE 그리고 LAST_HIT_WINDOW |

공통: 관리자 `/kill`(COMMAND_KILL)은 항상 무효. 파라미터(비율 0.5, 창 100틱)는 **TEST-ONLY**이며 승인되지 않았다. 창 후보의 근거: 바닐라 플레이어 처치 기억 100틱(UNVERIFIED), 저장소 선례 12,000틱(몬먼트 참여).

판정 결과 행렬(하네스 실행 결과; VALID/invalid):

| 상황 | ANY_DAMAGE | LAST_HIT_WINDOW | DAMAGE_SHARE | COMBINED |
| --- | --- | --- | --- | --- |
| player kills (full damage) | VALID | VALID | VALID | VALID |
| player 60% then lava kills within window | VALID | VALID | VALID | VALID |
| player 60% then lava kills long after | VALID | invalid | VALID | invalid |
| player 10% then lava kills quickly | VALID | VALID | invalid | invalid |
| no player damage, environment | invalid | invalid | invalid | invalid |
| no player damage, other mob | invalid | invalid | invalid | invalid |
| player 100% then /kill | invalid | invalid | invalid | invalid |
| player 10% then void | VALID | VALID | invalid | invalid |

COMBINED를 권장하는 이유: ANY_DAMAGE는 "한 대 치고 용암에 빠뜨려도 승리", LAST_HIT_WINDOW는 "한 대 치고 환경이 거의 다 깎아도 승리", DAMAGE_SHARE는 "환경이 막타를 치고 한참 뒤 죽어도 승리"를 허용한다. 최종 비율·창은 사용자 결정이다(O-9 세분화).

## 4. 하네스 검증 결과(IMPLEMENTED, 인메모리)

| T0 | no production default for the fallback timer (TEST-ONLY values never become defaults) | PASS |
| T1 | entrance cannot open before a valid defeat (timer, requests, spoofed signals) | PASS |
| T2 | environment-only death is not a victory: no entrance progress, chest sealed, no reward, retry allowed | PASS |
| T3 | administrative kill after participation is not a victory | PASS |
| T3b | a token player hit followed by an environmental death (share 10%, window long past) is not a victory (COMBINED) | PASS |
| T4a | valid victory starts the timer immediately at the defeat | PASS |
| T4b | golem spawn failure never stops the timer: FALLBACK_OPEN, cover applied once, reward granted once | PASS |
| T5a | duplicate defeat signals create exactly one golem and one guiding state | PASS |
| T5b | arrival opens (ENTRANCE_OPEN); duplicate arrival/open requests mutate the world exactly once; reward once | PASS |
| T5c | OPEN is terminal: no timer effect, no auto-restore | PASS |
| T6a | stalled golem (path failure) never opens by arrival; fallback opens | PASS |
| T6b | golem lost/killed while guiding: fallback opens | PASS |
| T7a | restart while guiding keeps state and remaining time; no golem is assumed | PASS |
| T7b | after restart the fallback still opens it | PASS |
| T7c | OPEN survives another restart, cover stays open, no second world mutation | PASS |
| T7d | OPEN record with reverted cover blocks (lost/regenerated chunk): the OPEN record is authoritative and the cover is re-opened idempotently (never closed) | PASS |
| T14 | respawn-after-restart option: exactly one new golem, state GUIDING, remaining time unchanged | PASS |
| T15 | OPEN record but no persisted victory (inconsistent save): cover is NOT opened, anomaly is flagged for reporting | PASS |
| T8 | unloaded cover chunk: record opens, blocks change only when the chunk loads, exactly once | PASS |
| T9 | lost defeat signal: the persisted victory record alone creates the entrance record and the timer | PASS |
| T10a | two simultaneous defeats: A opens by arrival while B keeps guiding (no cross-talk) | PASS |
| T10b | B opens by fallback on its own timer; one golem per instance; one reward per instance | PASS |
| T10c | a defeat in instance A never moves instance B | PASS |
| T11 | selector Nearest: never follows an offline player; empty when everyone is offline | PASS |
| T11 | selector StickyNearest: never follows an offline player; empty when everyone is offline | PASS |
| T11 | selector Farthest: never follows an offline player; empty when everyone is offline | PASS |
| T12 | matrix: COMBINED accepts a full player kill and rejects /kill | PASS |
| T13 | differential conformance: 4000 random event sequences / 29743 steps replayed against the P7 reference model give identical (state, cover, golems, victory, remaining, spawn-pending) | PASS |

주의: 위는 서버 없이 동작하는 인메모리 하네스이다. 실제 사망 원인 분류(`DamageSource` 매핑), 소유 엔티티 귀속, 이벤트 타이밍은 **UNVERIFIED**이다.

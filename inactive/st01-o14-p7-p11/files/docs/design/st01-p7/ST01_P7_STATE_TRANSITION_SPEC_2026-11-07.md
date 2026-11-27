# ST-01 P7 — 상태 전이 명세 (2026-11-07)

상태: **설계 명세 / 구현 승인 아님.** 참조 모델은 `tools/st01-p7/p7_statemachine.py`(순수 Python, 게임 코드 아님). 대기 시간은 추상 단위 `D`(기호 값, 1~4로 모두 검증)이며 실제 값은 미승인이다.

## 1. 상태

| 상태 | 의미 | 영속 |
| --- | --- | --- |
| SEALED | 보스 처치 전. 덮개 닫힘. 기록이 없는 것과 동일 | 기록 없음 허용 |
| GUARDIAN_DEFEATED | 처치 확인(보스 영속 기록이 근거). 골렘 생성 요청 대기 또는 생성 실패 후 대기 | 예 |
| GOLEM_GUIDING | 금 골렘 안내 중 | 예 |
| ENTRANCE_OPEN | 골렘이 도착 구역에 도달해 개방 | 예(종단) |
| FALLBACK_OPEN | 제한 시간 만료로 개방 | 예(종단) |

종단 상태는 되돌리지 않는다(복원 규칙 미확정 → 자동 복원 로직을 확정하지 않음). 개방 상태의 *물리적* 덮개 제거는 상태에서 파생되는 멱등 조정(reconcile)이며 청크가 로드될 때 적용된다.

## 2. 상태 전이도

```
                       (보스 영속 기록 = 처치됨 에서만)
 SEALED ──처치 확인──▶ GUARDIAN_DEFEATED ──골렘 생성 성공──▶ GOLEM_GUIDING ──도착 구역 도달──▶ ENTRANCE_OPEN
                              │                                   │
                              │ 생성 실패 / 정지 / 소실 / 경로 실패   │
                              └──────── 제한 시간 만료 ────────────┴──────────────▶ FALLBACK_OPEN
 (보스 처치 전에는 어떤 이벤트·타이머·요청도 SEALED를 벗어나지 못함)
```

모델이 도달 가능한 상태 216개, 간선 3504개를 전수 탐색해 얻은 **기록 상태 전이** 전부:

| 이전 | 이벤트 | 이후 |
| --- | --- | --- |
| GOLEM_GUIDING | GOLEM_ARRIVE | ENTRANCE_OPEN |
| GOLEM_GUIDING | TICK | FALLBACK_OPEN |
| GUARDIAN_DEFEATED | SPAWN_OK | GOLEM_GUIDING |
| GUARDIAN_DEFEATED | TICK | FALLBACK_OPEN |
| SEALED | BOSS_KILL | GUARDIAN_DEFEATED |

그 외 이벤트(중복 처치 이벤트, 중복 개방 요청, 골렘 소실, 재시작, 청크 언로드, 플레이어 이탈 등)는 기록 상태를 바꾸지 않는다.

## 3. 핵심 불변식 (전수 탐색, D=1..4)

| ID | 불변식 |
| --- | --- |
| I1 | 보스 처치 전에는 기록·덮개 어느 쪽도 열리지 않는다(잘못된/오래된/타 무덤 처치 이벤트, 상자 조기 접촉, 타이머, 개방 요청 포함) |
| I2 | 기록은 앞으로만 간다. 덮개는 복원되지 않는다 |
| I3 | 처치 이벤트·도착·개방 요청·조기 상자 접촉·경로 실패의 중복은 멱등이다 |
| I4 | 재시작은 영속 4항목(보스 처치 기록, 입구 상태, 남은 시간, 덮개 적용)을 보존하고 남은 시간을 잃지 않는다 |
| I5 | 골렘은 무덤당 최대 1마리 |
| I6 | P7 이벤트는 보스 처치 조건(=상자 봉인 해제/보상 조건)을 바꾸지 않는다. 대체 개방은 보상 조건을 우회하지 않는다 |
| I7 | (활성도) 보스가 처치된 모든 도달 가능 상태에서 *이로운 사건만*(청크 로드, 시간 경과, 플레이어 접속) 있으면 제한 시간 안에 덮개가 열린다 — 골렘 상태와 무관 |
| I8 | 덮개는 열림 기록이 있을 때만 제거된다 |
| I9 | 개방이 플레이어를 매몰시키지 않는다 |

## 4. 정상·실패 시나리오 (스크립트 실행 결과)

| 시나리오 | 이벤트 열 | 최종 기록 | 덮개 | 골렘 |
| --- | --- | --- | --- | --- |
| N1 normal: kill -> golem spawns -> arrives -> OPEN | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → GOLEM_ARRIVE | ENTRANCE_OPEN | 적용 | 1 |
| F1 golem spawn fails -> timer -> FALLBACK | BOSS_KILL → DEFEATED_EVENT → SPAWN_FAIL → TICK → TICK | FALLBACK_OPEN | 적용 | 0 |
| F2 path failure -> timer -> FALLBACK | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → GOLEM_PATH_FAIL → TICK → TICK | FALLBACK_OPEN | 적용 | 1 |
| F3 golem lost/dies while guiding -> timer -> FALLBACK | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → GOLEM_LOST → TICK → TICK | FALLBACK_OPEN | 적용 | 0 |
| F4 server restart while guiding -> record kept -> FALLBACK | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → TICK → RESTART → TICK | FALLBACK_OPEN | 적용 | 0 |
| F5 chunk unloaded at deadline -> cover applied on reload | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → CHUNK_UNLOAD → TICK → TICK → CHUNK_LOAD | FALLBACK_OPEN | 적용 | 1 |
| F6 DEFEATED event lost (listener failure) -> reconcile from boss record | BOSS_KILL → TICK → SPAWN_OK → GOLEM_ARRIVE | ENTRANCE_OPEN | 적용 | 1 |
| E1 duplicate DEFEATED events -> one golem | BOSS_KILL → DEFEATED_EVENT → DEFEATED_EVENT → SPAWN_OK → DEFEATED_EVENT → GOLEM_ARRIVE | ENTRANCE_OPEN | 적용 | 1 |
| E2 duplicate open requests | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → GOLEM_ARRIVE → OPEN_REQUEST_DUP → OPEN_REQUEST_DUP | ENTRANCE_OPEN | 적용 | 1 |
| E3 chest touched before the kill | CHEST_INTERACT_EARLY → TICK → TICK → TICK → OPEN_REQUEST_DUP | SEALED | 닫힘 | 0 |
| E4 player stands on the cover when it opens | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → PLAYER_STEP_ON_COVER → GOLEM_ARRIVE | ENTRANCE_OPEN | 적용 | 1 |
| E5 all players leave during guiding; timer still opens it | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → PLAYER_LEAVE → TICK → TICK → PLAYER_JOIN | FALLBACK_OPEN | 적용 | 1 |
| E6 arrival after fallback is a no-op | BOSS_KILL → DEFEATED_EVENT → SPAWN_OK → TICK → TICK → GOLEM_ARRIVE | FALLBACK_OPEN | 적용 | 1 |

시나리오 해설: F1 생성 실패, F2 경로탐색 실패(정지), F3 소실·사망, F4 안내 중 서버 재시작, F5 만료 시 청크 언로드 → 재로드에서 적용, F6 처치 이벤트 유실(리스너 실패) → 보스 영속 기록으로부터 복구, E3 처치 전 상자 접촉은 입구에 영향 없음, E4 덮개 위 점유 중 개방(§ 덮개 안전성), E5 모든 플레이어 이탈 중에도 타이머가 개방, E6 만료 후 도착은 무효.

## 5. 설계 결정 사항 (제안; 승인 아님)

1. 타이머는 **GUARDIAN_DEFEATED 수락 시점**에 시작한다(생성 실패·소실·정지가 타이머를 취소하지 않음). 안내 시작 시점 기준과 비교는 열린 결정 O-3.
2. 시간 경과는 게임 시간(서버 가동 중에만 진행)이며 플레이어 존재·청크 로드와 무관하게 계산한다. 물리적 제거는 청크가 로드되는 첫 틱에 멱등하게 적용한다.
3. 도착 판정과 타이머가 같은 틱에 경합하면 먼저 기록된 쪽이 이긴다(기록은 한 번만 쓰인다). 만료 후 도착은 무시한다.
4. 처치 이벤트는 **신호**일 뿐이다. 개방 판단은 항상 보스 영속 기록(`isDefeated`)을 다시 읽어 결정한다.

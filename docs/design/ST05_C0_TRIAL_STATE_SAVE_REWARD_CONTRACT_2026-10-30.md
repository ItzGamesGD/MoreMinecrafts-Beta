# ST-05 Phase C0 — 시험 상태·저장·보상 계약 설계

기록일: 2026-10-30 (C0-SYNC 갱신)  
기준: `checkpoint/wave15-unified-20261029` @ `4e1ff87a2693237f727c8a91a478a3d418402bc7` (PR #212 병합)  
상태: **설계·감사 전용. 이 문서 자체는 권위가 아니다.** 규칙의 근거는 [ST-05 현행 권위](../ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md)(Q1–Q116)다. 이 문서는 그 규칙을 구현 가능한 상태 머신·저장 계약·보상 계약으로 구체화한다. 권위와 충돌하면 권위가 이긴다.

판정: C1–C4 **IMPLEMENTATION READY**, C5(R7 보스) **CONDITIONAL READY** (§14).

---

## 1. 기준점 확인 결과

| 항목 | 결과 |
|---|---|
| 원격 Wave15 HEAD | `origin/checkpoint/wave15-unified-20261029` = `4e1ff87a` ✅ |
| PR #212 | Wave15에 병합됨. 병합 트리 = PR #212 head `423144c` 트리(`6407ab29`) |
| 현행 권위 | 이 브랜치에서 Q111–Q116과 R3 역할 분류를 추가함(§2). Q1–Q110 문구는 변경 없음 |
| 기존 구현 | Wave15에는 ST-05 코드 없음. 미병합 Draft PR #213(`befe135`, CI 14/14 성공)이 R1–R6 컨트롤러 기반을 구현. 기준 트리가 Wave15와 같아 그대로 적용됨 |
| 충돌 가능성 | PR #211(W16)이 #213과 같은 두 파일(`ModRegistries.java`, GameTest `fabric.mod.json`)을 수정 → 텍스트 충돌만 예상. R3 역할 분류는 #213과 **같은 위치에 같은 문장**으로 추가했으므로 #213과 이 브랜치가 그 줄에서 충돌하지 않음 |

## 2. 권위 동기화 결과

| 규칙 | 현행 권위 기록 | 이 계약의 해당 절 |
|---|---|---|
| R3 역할 분류(2026-10-29 승인) | Threat budget 절, #213 브랜치와 같은 문장 | §6.1 |
| Q111-A 미인정 사망·실제 소실 → 배정 반환·재소환 | 신규 절 "Persistence, time limit and reward records" | §6.4 |
| Q112-A 재시작 후 보존·재개, 오프라인 타이머 정지 | 같은 절 | §7, §8 |
| Q113-B 공유 열쇠와 개인 자격·수령 기록 분리 | 같은 절 | §9 |
| Q114-C 라운드 시작 등록 + 클리어 시 현장 존재 | 같은 절 | §9.2 |
| Q115-B 실제 개인 보상 지급 시에만 수령 완료, 기록 무기한 보존 | 같은 절 | §9.3, §9.5 |
| Q116 시험 전체 제한 시간 65분(쿨다운과 별개) | 같은 절 | §4, §7, §9.6 |

Q116 세부(소유자 답변, 2026-10-30):
- **시계:** R1 시작부터 센다. 전원 부재 정지 중에도 흐르고, 오프라인 시간은 제외한다.
- **만료:** 시험이 시간 초과로 끝나고 78,000틱 쿨다운이 시작된다.
- **남은 것들:** 남은 시험 몹, R3·R5 보급 상자, R7 보스는 제거하지 않는다.
- **남은 것 처치:** 바닐라 드롭과 XP만 나온다. 시험 연출과 보상은 없다.

## 3. 식별자 모델

| 식별자 | 형식 | 수명 | 발급 주체 |
|---|---|---|---|
| `structureInstanceId` | 문자열. 자연 생성은 `<dimension>|<structure id>|<start chunk packed>`, 테스트는 `test|<dimension>|<center packed>` | 구조물이 존재하는 동안 영구 | 월드젠(C4) 또는 테스트 등록 |
| `arenaKey` | 정확한 중심 블록의 `BlockPos.asLong()` | 인스턴스와 같음. 차원 내 조회용 색인 | 등록 시 파생 |
| `trialId` | 무작위 UUID | 한 번의 시험 | 컨트롤러 |
| `trialSerial` | 인스턴스별 단조 증가 long | 영구 | 컨트롤러 |
| `allocationIndex` | 라운드 내 0부터 시작하는 int | 한 라운드 | 편성 시 |
| 참가자 | 플레이어 UUID | 영구 | 바닐라 |
| 소유 태그 | 몹 엔티티의 영구 attachment `(structureInstanceId, trialId, round, allocationIndex)` | 몹이 존재하는 동안 | 소환 시 |
| 열쇠 태그 | 공유 열쇠 `ItemEntity`의 attachment `(structureInstanceId, trialId, round)`. 아이템 스택에는 표식 없음 | 줍기·병합·디스폰까지 | 발급 시 |

- **I-ID1** 한 `structureInstanceId`에는 동시에 활성 `trialId`가 많아야 1개다.
- **I-ID2** 엔티티를 로드할 때 소유 태그의 시험이 어떤 상태인지에 따라 처리한다.

| 소유 태그의 시험 상태 | 로드 시 처리 |
|---|---|
| 활성 시험이고 할당이 `ALIVE(entityUuid)`로 정확히 일치 | 시험 소유 몹으로 수락 |
| 활성 시험이지만 할당이 일치하지 않음 | 고아로 거부 |
| 실패·초기화로 끝난 시험 | 거부. Q102의 정리를 지연 완료하는 것 |
| 시간 초과로 끝난 시험(**해제된 시험**) | 외부 몹으로 수락. 집계·정리 대상 아님(Q116) |

## 4. 상태 머신

### 4.1 단계와 독립 값

- **단계:** `READY → ROUND(r) → INTERMISSION(r) → … → BOSS → SUCCESS | FAILURE | TIMED_OUT → COOLDOWN → READY`.
- **일시 단계:** SUCCESS·FAILURE·TIMED_OUT은 같은 틱 안에서 기록과 처리를 마치고 COOLDOWN으로 넘어간다.
- **단계와 독립적으로 관리하는 값:**
  - `absenceTicks`(부재 정지)
  - `spawnBlocked`/`blockedTicks`(Q110)
  - `trialRemaining`(Q116, 78,000에서 감소)
  - `bossStarted`

### 4.2 전이표

틱 판정 순서:
1. Q116 제한 시간 감소와 만료 판정
2. ALIVE 할당 재확인(§6.3)
3. 전멸 판정
4. 부재 판정
5. 단계별 진행

Q116이 가장 먼저 판정된다. 부재 정지 중에도 감소하기 때문이다.

| # | 현재 | 조건 | 다음 | 부수 효과 |
|---|---|---|---|---|
| T1 | READY | 적격 플레이어가 중심 활성 반경 안 | ROUND(1) | `trialId` 발급, `trialSerial++`, `trialRemaining=78,000`, 파란 입자, R1 참가자 잠금 |
| T2 | ROUND(r) | 모든 할당 KILLED, 미정지 | INTERMISSION(r) | 웨이브 기록·자격 기록·공유 열쇠 발급(§9), 사망 제외 해제, r=3·5면 보급 상자 |
| T3 | INTERMISSION(r), r<6 | 휴식 잔여 0, 미정지 | ROUND(r+1) | 재잠금, 예산 산정 |
| T4 | INTERMISSION(6) | 휴식 잔여 0, 미정지 | BOSS | R7 참가자 잠금 |
| T5 | BOSS | `bossStarted=false` | BOSS | 보스 핸들러 시작 시도. 핸들러가 없으면 대기(가짜 성공 금지) |
| T6 | BOSS | 처치 보고(`trialId` 일치, `bossStarted`) | SUCCESS → COOLDOWN | 보급 상자 정리(Q96), 쿨다운 78,000 |
| T7 | ROUND/BOSS | 이번 라운드 잠금 참가자 전원 사망 | FAILURE → COOLDOWN | 이 시험의 몹·보스·보급 정리(무드롭·무XP), 열쇠 회수 없음 |
| T8 | 활성 단계 | `absenceTicks` ≥ 1,200 | FAILURE → COOLDOWN | T7과 같음 |
| T9 | ROUND | `blockedTicks` ≥ 1,200, 새로 확인해도 차단 | FAILURE → COOLDOWN | T7과 같음 |
| T10 | ROUND | 편성 불가(데이터 검증 실패) | FAILURE → COOLDOWN | T7과 같음 |
| T11 | 활성 단계 | 운영자·통합 강제 초기화 | FAILURE → COOLDOWN | T7과 같음 |
| **T13** | 활성 단계 | `trialRemaining` = 0 | **TIMED_OUT → COOLDOWN** | **정리 없음**: 이 시험을 *해제된 시험*으로 기록(§8.2 `released_trials`), 몹·보급 상자·보스는 그대로, 쿨다운 78,000. 웨이브 진행·기록·보상 없음 |
| T12 | COOLDOWN | 잔여 0 | READY | — |

부재 판정 기준:
- ROUND·BOSS: 이번 라운드 잠금 참가자 중 사망 제외가 아닌 사람이 구역 안에 있는가.
- INTERMISSION: 적격 플레이어가 구역 안에 있는가.

### 4.3 불변식

- **I1** 동시 생존 시험 몹 ≤ 12(경고 포함).
- **I2** 할당 상태는 PENDING/WARNING/ALIVE/KILLED 중 하나이고 합은 할당 총수다. 라운드 중 할당 수는 변하지 않는다.
- **I3** KILLED는 ALIVE에서만 바뀔 수 있고 되돌릴 수 없다.
- **I4** 한 `(trialId, round)`에서 웨이브 기록은 1회, 공유 열쇠 **발급 완료**는 1개다(§9.4).
- **I5** 소유 태그가 없거나 해제된 시험의 엔티티는 예산·집계·정리·진행에 영향을 주지 않는다.
- **I6** 위치 확인 실패는 할당을 소모하지 않는다.
- **I7** 부재 정지 중에는 라운드 완료, 휴식 진행, 새 소환 배치가 없다. Q116 제한 시간만 흐른다.
- **I8** 장부(웨이브·자격·수령·열쇠·해제 시험 기록)는 어떤 시험 결과로도 삭제되지 않는다(Q115-B).
- **I9** 컨트롤러는 Vault 블록 엔티티를 읽거나 쓰지 않는다.
- **I10** 제한 시간(`trialRemaining`)과 쿨다운(`cooldownRemaining`)은 별개의 필드다. 하나가 다른 하나를 초기화하거나 대신하지 않는다.

## 5. 참가자

| 사건 | 즉시 효과 | 시험 시스템 효과 |
|---|---|---|
| 구역 진입 | 전투 가능 | 다음 라운드 시작에 잠금 |
| 이탈·접속 종료 | 존재 종료 | 잠금 유지. 전원 이탈이면 부재 정지 |
| 같은 UUID로 재접속 | 구역 안이면 존재 | 잠금 유지. 정지 해제 |
| 라운드 중 사망 | — | 이번 라운드 사망 제외. 클리어 시 구역 안에 없으므로 그 웨이브의 개인 자격도 없음(Q114-C) |
| 휴식 중 사망 | — | 제외 없음 |
| 늦은 합류 | 전투 가능 | 다음 라운드부터 반영. 현재 웨이브의 개인 자격 없음(Q114-C ①) |

크리에이티브·관전자는 적격 플레이어가 아니다(R-3).

## 6. 몬스터 할당·소유권·유효 처치

### 6.1 편성

권위 그대로 따른다:
- 활성 15종만 쓴다.
- 예산은 `round(solo × (1 + 0.6(n−1)))`이다.
- 점수 상한은 4/7/10이다.
- 채움 대역은 85–100%, 다음 105%, 다음 예산 이하 최대치 순서다.
- R3는 정예 정확히 1체와 근접·원거리·특수 역할을 모두 포함한다.

**R3 역할 대조**(#213 `ColosseumMob`와 권위 문장):

| 역할 | 권위 | #213 코드 | 일치 |
|---|---|---|---|
| 근접 | Zombie, Baby Zombie, Husk, Cave Zombie, Cave Skeleton, Bandit, Large Zombie, Lesser Wither Skeleton, Bandit Warrior | 같은 9종 `Role.MELEE` | ✅ |
| 원거리 | Skeleton, Bogged, Stray, Bandit Archer, Armored Skeleton, Breeze | 같은 6종 `Role.RANGED` | ✅ |
| 특수능력 | Breeze, Bogged, Stray, Husk, Lesser Wither Skeleton | 같은 5종 `Role.SPECIAL` | ✅ |
| 정예 후보 | 점수 7: Breeze, Armored Skeleton, Bandit Warrior | `isElite()` = 점수 ≥ 7 | ✅ |

### 6.2 소환

- 1초 주기로 소환을 판정한다.
- 생존 + 경고가 6 이하이면 12까지 경고를 건다.
- 위치는 12–24 고리 안에서 고른다. 경고 약 1초 뒤 위치를 다시 검사하고 생성한다.
- 생성 전에 할당을 ALIVE로 묶은 다음 월드에 추가한다. 추가에 실패하면 PENDING으로 되돌린다.

### 6.3 생존 재확인

| 확인 결과 | 처리 |
|---|---|
| 엔티티 있음 | 청크 갱신 |
| 없음, 마지막 청크의 엔티티가 로드됨 | 실제 소실 → §6.4 |
| 없음, 청크 미로드 | ALIVE 유지(중복 소환 금지) |

### 6.4 Q111-A — 배정 반환과 재소환

다음 사건은 할당을 PENDING으로 되돌리고 정상 배치에서 다시 소환한다. 유효 처치 수는 늘지 않고 `requeues`만 증가한다.

| 사건 | 분류 |
|---|---|
| 처치 판정 순간 구역 안 적격 플레이어 없음 | 미인정 사망 |
| 디스폰, `discard`, 차원 이동, 변환(원본) | 실제 소실 |
| 재시작 후 마지막 청크가 로드됐는데 엔티티 없음 | 실제 소실 |
| 청크 언로드 | 소실 아님 → 유지 |

TIMED_OUT 이후에는 할당이 없으므로 재소환도 없다(§9.6).

## 7. 시간 관리 — 온라인 틱 기준(Q112-A)

| 필드 | 초기값 | 감소 조건 | 만료 시 |
|---|---|---|---|
| `trialRemaining` (Q116) | 78,000 (R1 시작) | 활성 단계의 매 틱. **부재 정지 중에도 감소** | T13 TIMED_OUT |
| `absenceTicks` | 0 | 부재 중 매 틱 **증가** | T8 |
| `blockedTicks` | 0 | Q110: 배치가 필요한데 유효 위치가 없는 미정지 틱마다 증가, 정원·6 초과 대기 중 보류, 유효 위치가 생기면 0 | T9 |
| `intermissionRemaining` | 200 / 700 | 미정지 INTERMISSION | T3/T4 |
| 경고 `warningTicks` | 20 | 미정지 ROUND | 생성 시도 |
| `cooldownRemaining` | 78,000 | COOLDOWN 매 틱 | T12 |

- 모든 값은 남은(또는 누적된) **틱 수**로 저장한다.
- 서버가 해당 차원을 틱할 때만 움직인다. 서버 정지나 빈 서버 일시정지(`pause-when-empty-seconds`) 동안은 멈춘다.
- 컨트롤러는 아레나 청크가 미로드여도 매 틱 돈다.
- 벽시계와 `gameTime` 차이는 판정에 쓰지 않는다. `gameTime`은 기록용으로만 쓴다.
- 이미 75,000틱이 지난 시험이 R7에 있어도 남은 3,000틱이 끝나면 TIMED_OUT이다. 보스 핸들러가 없는 현재는 R7 대기가 결국 TIMED_OUT 또는 부재 FAILURE로 끝난다.

## 8. 저장 데이터 스키마와 복구

### 8.1 위치

차원별 `SavedData`, id `moreminecrafts:st05_colosseum_arenas`, `DataFixTypes.LEVEL`.

### 8.2 스키마 v1

```text
root { schema: int = 1, arenas: [ArenaRecord] }
ArenaRecord {
  instance_id: string
  geometry: { center, combat_radius, min_y, max_y, activation_radius, spawn_min_y, spawn_max_y }
  trial:  TrialState
  ledger: Ledger                       # 추가 전용, 무기한 보존
}
TrialState {
  core:    { phase, last_outcome, trial?, last_trial?, serial, seed, round }
  timers:  { trial_remaining, intermission, absence, spawn_blocked_ticks, spawn_blocked,
             batch_timer, cooldown, boss_started }
  party:   { locked:[uuid], dead:[uuid], ever:[uuid] }
  content: { allocations:[Allocation], supplies:[long] }
}
Allocation { index, mob, focal, status, entity?, chunk, warning_pos, warning_ticks, requeues }
Ledger {
  waves:           [WaveRecord]        # 유일 키 (trial, round)
  eligibility:     [EligibilityRecord] # 유일 키 (trial, round, player)
  claims:          [ClaimRecord]       # 유일 키 (trial, round, player, reward_id)
  released_trials: [uuid]              # TIMED_OUT으로 해제된 시험
}
WaveRecord {
  trial, serial, round, locked:[uuid], present_at_clear:[uuid], cleared_game_time,
  key: { state: PENDING|SPAWNED|CONFIRMED|CONSUMED, entity?, chunk, issue_count:int }
}
EligibilityRecord { trial, round, player, basis = "Q114-C", recorded_game_time }
ClaimRecord       { trial, round, player, reward_id, delivered_game_time }
```

- **enum 저장:** 이름 문자열로 저장한다.
- **필드 형식:** 새 필드는 `optionalFieldOf`에 기본값을 둔다. 덕분에 v1 안의 확장은 하위 호환이 된다.
- **outcome:** `last_outcome`에 `TIMED_OUT`을 추가한다.

### 8.3 버전 관리

| 읽은 `schema` | 처리 |
|---|---|
| 없음 (v0 = #213 형식) | `instance_id = test|<dim>|<center>`. `cleared_waves`를 `ledger.waves`로 옮긴다(`key.state = CONSUMED`: v0에는 열쇠 발급이 없었으므로 새로 발급하지 않음). `trial_remaining`이 없으면 활성 시험은 78,000으로 시작 |
| 1 | 그대로 |
| > 1 | **보존 모드**: 틱하지 않고, 원본을 그대로 다시 쓰고, 오류 로그를 남긴다. 삭제 금지 |

### 8.4 저장 순서와 크래시 대응

같은 틱 안의 상태 변경과 장부 변경은 같은 `SavedData`에서 일어난다. 엔티티는 청크 저장에 들어가며 두 저장은 원자적이지 않다.

| 위험 | 대응 |
|---|---|
| WARNING인데 엔티티가 이미 저장됨 | 로드 시 고아로 거부 → 경고 종료 후 1회만 생성 |
| ALIVE인데 엔티티가 저장되지 않음 | 실제 소실 → 재소환(Q111-A) |
| 처치는 저장됐는데 죽어 가던 엔티티가 저장됨 | 로드 시 KILLED로 판정해 거부 |
| 공유 열쇠 발급 중 크래시 | §9.4의 상태 기계로 유실 복구 |

### 8.5 복구 절차(서버 시작 시)

1. 스키마 버전을 처리한다(§8.3).
2. 일시 단계가 남아 있으면 해당 결과의 처리를 마치고 COOLDOWN으로 만든다(TIMED_OUT은 정리 없이).
3. ALIVE 할당은 청크 엔티티 로드가 존재 또는 부재를 증명할 때까지 유지한다.
4. WARNING은 남은 틱부터 이어 간다.
5. 엔티티를 로드할 때 I-ID2를 적용한다(활성 일치는 수락, 불일치·실패 시험은 거부, 해제 시험은 외부 몹으로 수락).
6. 공유 열쇠 레코드가 `SPAWNED`이면 §9.4의 확인 절차를 따른다.
7. 장부는 §9.4의 열쇠 상태 전이 외에는 쓰지 않는다.
8. BOSS이고 `bossStarted=true`면 보스 핸들러에 재개를 알린다.
9. 타이머는 저장값부터 이어 간다. 참가자가 재접속하기 전에는 부재 시계와 Q116 시계가 흐른다.

## 9. 보상 계약

### 9.1 원칙(Q113-B)

- 웨이브당 **공유 일반 Trial Key는 정확히 1개**다.
- 개인 자격과 개인 수령은 공유 열쇠와 독립된 장부에 기록한다.
- 개인 기록은 열쇠 수를 바꾸지 않는다.
- Vault의 바닐라 개인별 개방도 바꾸지 않는다.

### 9.2 개인 자격(Q114-C)

라운드 r 클리어(T2) 순간, 다음 두 조건을 모두 만족하는 플레이어마다 `EligibilityRecord(trial, r, player, "Q114-C")`를 1건 추가한다.

1. 라운드 r 시작 때 잠긴 참가자다.
2. 지금 구역 안에 있는 적격 플레이어다(살아 있고, 크리에이티브·관전자가 아님).

- 유일 키가 이미 있으면 추가하지 않는다.
- 늦은 합류자, 라운드 중 사망 후 구역 밖에 있는 사람, 이탈자는 자격이 없다.
- TIMED_OUT 이후 처치로는 자격이 생기지 않는다.

### 9.3 개인 수령(Q115-B)

- `ClaimRecord`는 **실제 개인 보상이 그 플레이어에게 지급된 경우에만** 쓴다.
- 공유 열쇠를 줍는 것, 자격 기록이 생기는 것, Vault를 여는 것은 수령이 아니다.
- 현재 승인된 개인 보상이 없으므로 C1–C4는 수령 기록을 만들지 않는다. API `tryClaim(trial, round, player, rewardId)`만 제공한다.
  - 자격이 없으면 거부한다.
  - 이미 수령 기록이 있으면 거부한다.
  - 그 외에는 기록을 먼저 쓰고 지급한다.
  - 기록 직후 크래시가 나면 지급이 유실될 수 있다. 이는 C6 설계 대상이다(O-7).

### 9.4 공유 열쇠 — 중복 방지와 크래시 유실 복구

목표는 두 가지다.
- 같은 `(trial, round)`에 대해 세상에 존재하는 공유 열쇠를 1개 이하로 유지한다(중복 금지).
- 크래시로 저장되지 못한 열쇠를 다시 발급한다(유실 복구).

`WaveRecord.key.state`:

| 상태 | 의미 | 들어가는 시점 |
|---|---|---|
| PENDING | 발급 결정됨, 아직 생성 안 함 | T2에서 웨이브 기록과 함께 |
| SPAWNED | 열쇠 태그가 붙은 `ItemEntity`를 중심에 추가함, 디스크 저장 미확인 | 생성 직후 같은 틱(`entity`, `chunk`, `issue_count++` 기록) |
| CONFIRMED | 그 엔티티가 청크 저장에 들어간 것이 확인됨 | 엔티티 `UNLOADED_TO_CHUNK` 언로드, 또는 재시작 후 같은 UUID 로드 확인 |
| CONSUMED | 엔티티가 세상에서 정상 종료됨(줍기·호퍼·다른 열쇠와 병합·디스폰·파괴) | 열쇠 엔티티의 `DISCARDED`/`KILLED` 제거 이벤트 |

규칙:

1. PENDING이면 다음 틱에 생성하고 SPAWNED로 바꾼다. 아레나 중심 청크가 미로드면 로드될 때까지 PENDING으로 둔다.
2. SPAWNED나 CONFIRMED 상태에서 재시작 후 **마지막 청크의 엔티티가 로드됐는데 그 UUID가 없으면**:
   - SPAWNED: 저장 전에 크래시가 난 유실로 본다 → PENDING으로 되돌려 **1회 재발급**.
   - CONFIRMED: 정상 종료를 놓친 경우(제거 이벤트가 저장되기 전에 크래시)로 보고 CONSUMED로 처리한다. 재발급하지 않는다.
3. 같은 UUID가 로드되면 CONFIRMED로 바꾼다. 같은 열쇠 태그를 가진 엔티티가 두 개 이상 로드되면 기록된 UUID 하나만 남기고 나머지를 거부한다(중복 제거).
4. CONSUMED는 되돌리지 않는다. 시험 실패·초기화·시간 초과·다음 시험에도 열쇠는 회수하지 않는다(Q97).
5. `issue_count`는 진단용이다. 정상이면 1, 크래시 복구 시 2 이상이 된다.

잔여 위험:
- SPAWNED 상태에서 플레이어가 열쇠를 줍고(CONSUMED가 메모리에만 있음) 장부보다 플레이어 데이터가 먼저 저장된 뒤 크래시가 나면, 재발급과 플레이어 인벤토리의 열쇠가 공존할 수 있다.
- 바닐라는 플레이어 데이터와 월드 데이터를 원자적으로 저장하지 않으므로 이 창은 완전히 닫을 수 없다(R-2).
- 정상 종료와 자동 저장 경로에서는 발생하지 않는다.

### 9.5 기록 보존(Q115-B)

- 장부의 네 목록(`waves`, `eligibility`, `claims`, `released_trials`)과 각 웨이브의 `key` 상태는 추가와 상태 전진만 허용한다. 삭제는 없다.
- 다음 시험 시작은 활성 상태만 초기화한다.
- 아레나 등록 해제는 장부가 비어 있지 않으면 거부한다. 운영 목적의 강제 해제는 장부를 같은 `instance_id`의 보관 레코드로 옮긴다.
- 보존 기간은 무기한이다. 시험당 최대 6웨이브이고 참가자 4명 기준 24건 수준이라 크기 위험은 낮다.

### 9.6 시간 초과 후(Q116)

- 해제된 시험의 몹과 보스는 외부 몹처럼 동작한다. 처치하면 바닐라 드롭과 XP는 나오지만, 웨이브 진행·연출·Trial Key·Ominous Trial Key·자격·수령 기록은 없다.
- 보급 상자와 그 내용물은 남는다. 정리 목록(`supplies`)을 해제하고 다시 다루지 않는다.
- 쿨다운이 끝나 새 시험이 시작되어도 이전 시험의 남은 몹은 새 시험의 할당·예산·정원에 포함되지 않는다(I5).

### 9.7 Vault 보존

- 컨트롤러와 장부는 Vault 블록·블록 엔티티(`rewarded_players` 포함)를 읽거나 쓰지 않는다.
- 일반 6 + Ominous 1, 모두 바닐라 독립 개방이다.
- 열쇠 아이템 스택에는 컴포넌트를 추가하지 않는다. Minecraft 26.3 `VaultBlockEntity.isValidToInsert`가 `ItemStack.isSameItemSameComponents`로 열쇠를 대조함을 바이트코드로 확인했다. 열쇠 추적은 엔티티 attachment로만 한다.

## 10. 인터페이스와 책임 분리

| 구성요소 | 책임 | 하지 않는 것 |
|---|---|---|
| `ColosseumController` (순수) | 전이, 타이머, 할당, 장부 쓰기 결정, 열쇠 상태 전이 | 월드 접근 |
| `ColosseumWorldPort` | 존재 판정, 위치 검사, 소환, 위치 확인, 정리, 효과, 열쇠 엔티티 생성·조회 | 규칙 판단 |
| `ColosseumArenaData` | 스키마 v1, v0 이전, 보존 모드 | 판정 |
| `ColosseumOwnership` | 몹 소유 태그, 열쇠 태그 | — |
| `ColosseumRuntime` | 틱, 사망, 언로드, 로드 허용 연결. I-ID2 적용 | — |
| `RewardLedger` (C1) | 네 목록과 열쇠 상태, 유일 키, 추가 전용 | 아이템 생성 |
| `KeyIssuer` (C2) | 공유 열쇠 생성과 §9.4 확인 | 발급 여부 결정 |
| `SupplyHandler` (C3) | 보급 상자 배치, SUCCESS·FAILURE 시 정리, TIMED_OUT 시 방치 | 플레이어 상자 접근 |
| `StructureRegistration` (C4) | `instance_id`와 기하로 등록 | 시험 진행 |
| `BossHandler` (C5) | 생성, 재개, 정리, **해제(TIMED_OUT)**, 처치 보고 | 웨이브 장부 |
| 이벤트 | `WAVE_CLEARED`(장부 기록 후 1회), `TRIAL_ENDED`(결과 포함, TIMED_OUT 추가) | — |

### 10.1 PR #213과 C0 계약의 차이 7건

#213은 상태 머신, I1–I7, Q111-A 재소환, 경고·차단 시계 복원, 소유 검증을 이미 구현하고 검증했다(CI 14/14). 차이는 다음과 같다. PR #213 코드 수정은 이 문서 범위 밖이며 별도 작업이다.

| # | #213 현재 | 계약 | 분류 |
|---|---|---|---|
| D1 | 저장 루트에 `schema` 없음 | v1 + v0 이전 + 미래 버전 보존 모드 | 저장 형식 |
| **D2** | `startTrial`이 `clearedWaves.clear()` 호출 | 장부 무기한 보존(I8) | **삭제 금지 위반** |
| **D3** | `unregisterArena`가 기록째 삭제(`data.remove`) | 장부가 있으면 거부, 강제 시 보관 | **삭제 금지 위반** |
| D4 | 식별 = 중심 좌표 long | `instance_id` 추가(키는 색인으로 유지) | 월드젠 연동 |
| D5 | 웨이브 기록에 `locked`·`present`만 | 네 목록 장부와 열쇠 상태, Q114-C 자격 기록 | 보상 계약 |
| D6 | 열쇠 발급 없음(의도) | §9.4 상태 기계 | 보상 |
| D7 | 소유 태그 `(arena, trial, index)` | `round` 추가 권장 | 선택 |

Q116으로 새로 생긴 차이(위 7건과 별도):

| # | #213 현재 | 계약 |
|---|---|---|
| D8 | 시험 전체 제한 시간 없음 | `trialRemaining` 78,000, T13 TIMED_OUT |
| D9 | 모든 종료에서 `trialId`를 지워 남은 태그 몹을 로드 시 거부(삭제) | TIMED_OUT 시험은 `released_trials`에 기록하고 그 몹을 외부 몹으로 수락. 실패·초기화는 지금처럼 거부 |

### 10.2 D2·D3 수정안(PR #213 별도 작업용)

**D2 — `ColosseumController.startTrial`**

현재:
```java
state.clearedWaves.clear();
```
수정:
- `startTrial`에서 이 줄을 삭제한다.
- 웨이브 기록 판정 `waveCleared(round)`가 `(trialId, round)`로 비교하도록 바꾼다. 지금은 round만 비교하므로, 이 줄을 지우면 지난 시험의 R1 기록 때문에 새 시험의 R1이 완료되지 않는다.

```java
public boolean waveCleared(UUID trial, int round) {
    for (WaveRecord r : clearedWaves) if (r.trialId().equals(trial) && r.round() == round) return true;
    return false;
}
// completeRound: if (state.waveCleared(state.trialId, round)) return;
```

테스트:
- 시험 A에서 R1을 클리어하고 쿨다운을 해제한 뒤 시험 B를 시작한다. A의 기록이 남아 있고, B의 R1이 정상 완료되며, `WAVE_CLEARED`가 B의 R1에 대해 1회 발생해야 한다.
- 이어서 재시작한 뒤에도 A·B 기록이 모두 유지되어야 한다.

**D3 — `ColosseumIntegration.unregisterArena`**

현재:
```java
ColosseumController.forceReset(...); data(level).remove(arena.key()); return true;
```
수정:
- 장부가 비어 있지 않으면 등록 해제를 거부한다(`false` 반환, 이유를 로그에 남김). 활성 시험의 초기화는 별도 명령 `reset`으로만 한다.
- 꼭 해제해야 하는 운영 경로는 `archiveAndUnregister`로 분리한다. 장부를 `ColosseumArenaData.archived`(키 `instance_id`)로 옮긴 뒤 활성 레코드만 지운다.
- 같은 중심에 다시 등록되면 보관된 장부를 복원한다.

`/mmcolosseum unregister` 명령과 GameTest 픽스처:
- 새 의미(장부가 있으면 거부)에 맞춘다.
- 테스트 정리는 `archiveAndUnregister` 또는 테스트 전용 아레나를 쓴다.

테스트:
- 장부가 있는 아레나에 대한 `unregister`는 거부되고 데이터가 유지되어야 한다.
- `archiveAndUnregister` 후 다시 등록하면 장부가 복원되어야 한다.

## 11. 테스트 시나리오와 승인 기준

| ID | 시나리오 | 승인 기준 | 방식 |
|---|---|---|---|
| S1 | 1인 R1–R6 정상 진행 | 웨이브 기록 6, 공유 열쇠 6(웨이브당 1), 휴식 10/10/35/10/35/10초, 핸들러 없이는 성공 없음 | JUnit + GameTest |
| S2 | 1–4인 예산·편성 | 예산표, 85–100%, 조건부 8종 없음, R3 정예 1·역할 3(§6.1 대조표) | JUnit |
| S3 | 정원과 보충 | 생존 ≤ 12, 6 이하에서만 보충, 할당 수 불변 | GameTest |
| S4 | 부재 60초 이내 복귀 | 정지 후 재개, Q116 시계는 정지 중에도 감소 | JUnit + GameTest |
| S5 | 전원 이탈 60초 | FAILURE, 시험 몹만 무드롭 제거, 외부 몹·열쇠 보존, 쿨다운 | GameTest |
| S6 | 사망·복귀 | 라운드 중 복귀 무효, 클리어 후 복귀는 다음 라운드 잠금, 전원 사망은 FAILURE | JUnit + GameTest |
| S7 | 같은 UUID 재접속 | 잠금 유지, 정지 해제 | 프로세스 테스트 |
| S8 | Q111-A 무인 처치 | 미인정 → PENDING → 재소환, KILLED 불변 | JUnit + GameTest |
| S9 | Q111-A 실제 소실 | 디스폰·`discard`·차원 이동 후 재소환, 중복 없음 | GameTest |
| S10 | 청크 언로드 | 같은 UUID 복귀, 재소환 없음 | GameTest |
| S11 | 재시작: 처치 직후 / 경고 중 / Q110 차단 중 | #213 프로세스 테스트와 같음 | 프로세스 테스트 |
| S12 | 재시작: 클리어 직후 | 웨이브·자격 기록 유지, 이벤트 재발생 없음 | 프로세스 테스트 |
| S13 | 열쇠 중복 방지 | 같은 (trial, round) 완료를 두 번 처리해도 열쇠 1개, 같은 태그 엔티티 2개를 로드하면 1개만 남음 | JUnit + GameTest |
| S14 | 열쇠 유실 복구 | SPAWNED 저장 후 엔티티 없이 재시작하면 1회 재발급(`issue_count`=2), CONFIRMED에서 엔티티가 없으면 재발급 없음 | JUnit + 프로세스 테스트 |
| S15 | Q114-C 자격 | 잠금∧클리어 시 존재만 기록. 늦은 합류, 라운드 중 사망 후 밖, 이탈자는 제외 | JUnit + GameTest |
| S16 | Q115-B 수령 | 자격만으로는 수령 없음, 열쇠 줍기는 수령 아님, `tryClaim` 두 번째 거부 | JUnit |
| S17 | 기록 보존 | 다음 시험 시작·실패·시간 초과 후에도 장부 불변(D2), `unregister` 거부(D3) | JUnit |
| S18 | Q116 만료 | 78,000틱에 TIMED_OUT, 몹·상자·보스 잔존, 그 몹 처치 시 바닐라 드롭·XP만, 기록·열쇠 없음, 쿨다운 78,000 별도 | JUnit + GameTest |
| S19 | Q116 재시작 | 남은 제한 시간 보존, 오프라인 시간 미소모 | 프로세스 테스트 |
| S20 | 해제 시험 몹 로드 | TIMED_OUT 시험의 몹은 청크 재로드 후에도 남음(삭제 금지), 실패 시험 몹은 거부 | GameTest |
| S21 | 스키마 | v0 → v1 이전, 미래 버전 보존 모드 | JUnit |
| S22 | Vault 비간섭 | 시험 전·중·후 Vault NBT 불변, 공유 열쇠로 Vault 개방 가능 | GameTest |
| S23 | 회귀 | 기존 GameTest 490 + 신규 전부, JUnit 전체, 서버 스모크 | CI |

LIVE VALIDATION PENDING: 몹 AI·표적, 입자, 밸런스, 실제 클라이언트 다인 플레이·재접속, 열쇠 픽업·Vault 개방 체감.

## 12. 단계별 작업 분할

| 단계 | 내용 | 선행 조건 |
|---|---|---|
| C1 | #213 수정: D1–D5, D7–D9(스키마 v1, 장부, Q114-C 자격 기록, Q116 시계와 해제 시험, `tryClaim` API). S4·S12·S15–S21 | 이 PR 승인(권위 동기화) |
| C2 | 공유 Trial Key 발급과 §9.4 유실 복구. S1·S13·S14·S22 | C1 |
| C3 | `SupplyHandler`: 참가자 수만큼 고정 내용물 상자, SUCCESS·FAILURE 정리, TIMED_OUT 방치 | C1, 위치는 C4와 연계 |
| C4 | 월드젠 등록(`instance_id`, 실제 기하) | 구조물 수치(권위상 구현·밸런스 작업) |
| C5 | R7 `BossHandler`(해제 포함) | O-8, O-9 결정 |
| C6 | 실제 개인 보상 지급과 수령 기록 | 승인된 개인 보상 정의(O-7) |

## 13. 남은 미해결 사항과 위험

해결됨: 구 O-1(Q111–Q113 원문) → Q111-A·Q112-A·Q113-B로 권위에 기록. 구 O-2 → Q114-C. 구 O-3 → Q115-B. 구 O-4 → Q116. 구 O-5 → R3 역할 동기화. 구 O-6 → 무기한 보존(Q115-B).

남은 사항(구현 차단 여부 표시):

- **O-7**(C6만 차단): 승인된 개인 보상 품목이 아직 없다. 실제 지급 대상과 지급 시점이 정해져야 수령 기록 경로를 구현할 수 있다. 기록 후 지급 전 크래시의 처리도 그때 정한다.
- **O-8**(C5만 차단): 시간 초과 후 남은 보스를 처치할 때 "바닐라 드롭"이 무엇인지 정해야 한다. 보스는 신규 몹이라 바닐라 전리품이 없다. 권위의 보스 직접 드롭은 Ominous Trial Key와 별도 승인 전리품뿐이고, Q116은 Ominous Trial Key를 포함한 시험 보상을 금지한다. 해제된 보스의 드롭이 없음인지 다른 무엇인지 결정이 필요하다.
- **O-9**(C5만 차단): Q91은 보스가 부순 외부 블록을 시험 종료·초기화 시 복원하도록 한다. TIMED_OUT으로 해제된 보스가 그 뒤에도 장애물 파괴 돌진을 계속하는지, 복원은 언제 하는지(만료 시점까지 부순 블록만, 또는 보스가 사라질 때) 결정이 필요하다.

위험:

- **R-1** 평화로움 난이도에서는 몬스터가 즉시 제거되어 재소환이 반복된다. 부재 60초나 Q116 65분에서 끝난다.
- **R-2** 공유 열쇠의 크래시 창: 플레이어 데이터와 월드 데이터가 원자적으로 저장되지 않아 극히 드물게 중복이 생길 수 있다(§9.4). 정상 종료·자동 저장에서는 발생하지 않는다.
- **R-3** 크리에이티브·관전자를 존재로 세지 않는다(바닐라 Trial Spawner 관례).
- **R-4** 플레이어가 남아 있는 동안 몹이 미로드 청크로 가면 라운드가 정체된다. Q116이 상한이 된다.
- **R-5** PR #211과 텍스트 병합 충돌이 예상된다.
- **R-6** GameTest 서버는 실시간 속도와 무관하게 틱을 돌린다. 무인 구간이 있는 재시작 테스트는 부재 만료, 그리고 이제 Q116 만료를 정상 결과로 허용해야 한다.
- **R-7** Q116 해제 시험의 몹은 소유 태그를 단 채 월드에 남는다. `released_trials`가 지워지면 실패 시험 몹으로 오인되어 삭제되므로, 이 목록은 장부와 같은 보존 규칙(I8)을 따른다.

## 14. 판정

- **C1–C4: IMPLEMENTATION READY.** 이 PR의 권위 동기화를 소유자가 승인하면 착수할 수 있다. 새 게임 규칙이 필요하지 않다.
- **C5(R7 보스): CONDITIONAL READY.** O-8과 O-9가 결정되어야 한다.
- **C6(개인 보상 지급): BLOCKED.** 승인된 개인 보상이 없다(O-7). 장부 스키마와 `tryClaim` API는 C1에서 준비된다.

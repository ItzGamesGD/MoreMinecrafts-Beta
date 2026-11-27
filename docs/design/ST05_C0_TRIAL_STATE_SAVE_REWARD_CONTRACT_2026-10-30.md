# ST-05 Phase C0 — 시험 상태·저장·보상 계약 설계

기록일: 2026-10-30 (C0-SYNC 갱신, C0-FINAL에서 Q117 반영)  
기준: `checkpoint/wave15-unified-20261029` @ `4e1ff87a2693237f727c8a91a478a3d418402bc7` (PR #212 병합)  
상태: **설계·감사 전용. 이 문서 자체는 권위가 아니다.** 규칙의 근거는 [ST-05 현행 권위](../ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md)(Q1–Q118)다. 이 문서는 그 규칙을 구현 가능한 상태 머신·저장 계약·보상 계약으로 구체화한다. 권위와 충돌하면 권위가 이긴다.

판정: C1–C4 **IMPLEMENTATION READY**(Q117 배관 포함), C5(R7 보스) **IMPLEMENTATION READY**(C4의 구조물 점유 마스크 이후), C6 **BLOCKED** (§15).

---

## 1. 기준점 확인 결과

| 항목 | 결과 |
|---|---|
| 원격 Wave15 HEAD | `origin/checkpoint/wave15-unified-20261029` = `4e1ff87a` ✅ |
| PR #212 | Wave15에 병합됨. 병합 트리 = PR #212 head `423144c` 트리(`6407ab29`) |
| 현행 권위 | 이 브랜치에서 Q111–Q118과 R3 역할 분류를 추가함(§2). Q1–Q110 문구는 변경 없음. Q116의 R7 보스 조항만 Q117이 대체하며(권위 문서에 주석 표시), Q116의 일반 몹·보급 상자 규칙은 그대로 유지 |
| 보고 HEAD 대조 (C0-FINAL) | PR #214 `c1a1eb35`, PR #213 `befe135`, Wave15 `4e1ff87a` 모두 보고값과 일치 ✅ |
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
| Q117 시간 초과 시 보스 소멸(일반 몹 잔류는 유지) | 같은 절 | §4.4, §10 |
| Q118-A 보스가 부순 자리에 다른 블록이 있으면 덮어쓰지 않음 | 같은 절 | §10.4 |

Q116 세부(소유자 답변, 2026-10-30):
- **시계:** R1 시작부터 센다. 전원 부재 정지 중에도 흐르고, 오프라인 시간은 제외한다.
- **만료:** 시험이 시간 초과로 끝나고 78,000틱 쿨다운이 시작된다.
- **남은 것들:** 남은 시험 몹과 R3·R5 보급 상자는 제거하지 않는다. *(R7 보스는 이 항목에서 빠진다. 아래 Q117이 대체한다.)*
- **남은 것 처치:** 일반 몹은 바닐라 드롭과 XP만 나온다. 시험 연출과 보상은 없다.

Q117 세부(소유자 지시, ST-05-C0-FINAL):
- **일반 몹:** Q116대로 잔류하고 이후 바닐라 드롭과 XP만 제공한다. Q116의 이 규칙은 삭제하지 않는다.
- **보스:** Immortal Gladiator는 중앙 수정의 종료 연출과 함께 소멸한다. 소멸은 처치로 인정하지 않는다.
- **보상:** Ominous Trial Key, 보스 전리품, XP를 지급하지 않는다.
- **쿨다운:** 78,000틱 재도전 쿨다운을 적용한다.
- **지형:** 보스가 파괴한 자연 블록은 Q91의 보호 조건에 따라 복원한다.
- **재시작:** 서버가 재시작되면 미완료 소멸·복원 작업을 재개한다.

## 3. 식별자 모델

| 식별자 | 형식 | 수명 | 발급 주체 |
|---|---|---|---|
| `structureInstanceId` | 문자열. 자연 생성은 `<dimension>|<structure id>|<start chunk packed>`, 테스트는 `test|<dimension>|<center packed>` | 구조물이 존재하는 동안 영구 | 월드젠(C4) 또는 테스트 등록 |
| `arenaKey` | 정확한 중심 블록의 `BlockPos.asLong()` | 인스턴스와 같음. 차원 내 조회용 색인 | 등록 시 파생 |
| `trialId` | 무작위 UUID | 한 번의 시험 | 컨트롤러 |
| `trialSerial` | 인스턴스별 단조 증가 long | 영구 | 컨트롤러 |
| `allocationIndex` | 라운드 내 0부터 시작하는 int | 한 라운드 | 편성 시 |
| 참가자 | 플레이어 UUID | 영구 | 바닐라 |
| 소유 태그 | 엔티티의 영구 attachment `(structureInstanceId, trialId, round, kind, allocationIndex)`. `kind`는 `MOB`(R1–R6 일반 몹) 또는 `BOSS`(R7, `allocationIndex = -1`) | 엔티티가 존재하는 동안 | 소환 시 |
| 열쇠 태그 | 공유 열쇠 `ItemEntity`의 attachment `(structureInstanceId, trialId, round)`. 아이템 스택에는 표식 없음 | 줍기·병합·디스폰까지 | 발급 시 |

- **I-ID1** 한 `structureInstanceId`에는 동시에 활성 `trialId`가 많아야 1개다.
- **I-ID2** 엔티티를 로드할 때 소유 태그가 가리키는 시험의 상태와 `kind`에 따라 처리한다. 로드 허용·거부 판정은 모두 이 표를 따른다. "거부"는 엔티티를 월드에 올리지 않는 것이다.

| 소유 태그가 가리키는 시험 | `kind = MOB` | `kind = BOSS` |
|---|---|---|
| 활성 시험이고 기록과 정확히 일치(ALIVE 할당의 `entityUuid` / `boss.entity`) | 시험 소유 몹으로 수락 | 시험 소유 보스로 수락 |
| 활성 시험이지만 일치하지 않음(중복 사본, 낡은 저장본) | 고아로 거부 | 고아로 거부 |
| 실패·초기화로 끝난 시험 | 거부(Q102 정리를 지연 완료) | 거부(Q102) |
| 성공으로 끝난 시험 | 거부(낡은 사본) | 거부(낡은 사본) |
| 시간 초과로 끝난 시험(**해제된 시험**) | **외부 몹으로 수락하고 태그를 제거**(Q116) | **UUID와 무관하게 거부, 사망 없이 제거 = 소멸 완료**(Q117) |

## 4. 상태 머신

### 4.1 단계와 독립 값

- **단계:** `READY → ROUND(r) → INTERMISSION(r) → … → BOSS → SUCCESS | FAILURE | TIMED_OUT → COOLDOWN → READY`.
- **일시 단계:** SUCCESS·FAILURE·TIMED_OUT은 같은 틱 안에서 기록과 처리를 마치고 COOLDOWN으로 넘어간다. 세 종료는 서로 다른 결과(outcome)이고, 한 시험은 정확히 하나로 끝난다(§4.4).
- **단계와 독립적으로 관리하는 값:**
  - `absenceTicks`(부재 정지)
  - `spawnBlocked`/`blockedTicks`(Q110)
  - `trialRemaining`(Q116, 78,000에서 감소)
  - `boss`(`started`, `entity`, `chunk`): #213의 `bossStarted`를 대체한다. 보스가 실제로 월드에 있는지를 추적하며 Q117 소멸의 대상이다.
- **단계와 독립적인 작업 항목:** `boss_vanish`(보스 소멸 작업)와 `restore_journal`(블록 복원 저널)이다(§10). 시험이 끝나 COOLDOWN이나 READY가 되어도 작업이 끝날 때까지 남는다.

### 4.2 전이표

틱 판정 순서(아레나마다):

0. 작업 항목 처리(§10.2, §10.4). 모든 단계에서 수행한다.
1. Q116 제한 시간 감소와 만료 판정(T13)
2. ALIVE 할당·보스 재확인(§6.3)
3. 전멸 판정(T7)
4. 부재 판정(T8)
5. 단계별 진행

Q116이 가장 먼저 판정된다. 부재 정지 중에도 감소하기 때문이다. 같은 틱에 둘 이상이 성립할 때의 결과는 §4.5에 정한다.

| # | 현재 | 조건 | 다음 | 부수 효과 |
|---|---|---|---|---|
| T1 | READY | 적격 플레이어가 중심 활성 반경 안 | ROUND(1) | `trialId` 발급, `trialSerial++`, `trialRemaining=78,000`, 파란 입자, R1 참가자 잠금 |
| T2 | ROUND(r) | 모든 할당 KILLED, 미정지 | INTERMISSION(r) | 웨이브 기록·자격 기록·공유 열쇠 발급(§9), 사망 제외 해제, r=3·5면 보급 상자 |
| T3 | INTERMISSION(r), r<6 | 휴식 잔여 0, 미정지 | ROUND(r+1) | 재잠금, 예산 산정 |
| T4 | INTERMISSION(6) | 휴식 잔여 0, 미정지 | BOSS | R7 참가자 잠금 |
| T5 | BOSS | `boss.started=false` | BOSS | 보스 핸들러 시작 시도. 핸들러가 없으면 대기(가짜 성공 금지) |
| T6 | BOSS | 처치 보고(`trialId` 일치, `boss.started`) | SUCCESS → COOLDOWN | 보급 상자 정리(Q96), 복원 저널을 복원 대상으로 전환(Q91), 쿨다운 78,000 |
| T7 | ROUND/BOSS | 이번 라운드 잠금 참가자 전원 사망 | FAILURE → COOLDOWN | 이 시험의 몹·보스 제거(무드롭·무XP, Q102), 보급 정리, 저널 복원 대상 전환, 열쇠 회수 없음 |
| T8 | 활성 단계 | `absenceTicks` ≥ 1,200 | FAILURE → COOLDOWN | T7과 같음 |
| T9 | ROUND | `blockedTicks` ≥ 1,200, 새로 확인해도 차단 | FAILURE → COOLDOWN | T7과 같음 |
| T10 | ROUND | 편성 불가(데이터 검증 실패) | FAILURE → COOLDOWN | T7과 같음 |
| T11 | 활성 단계 | 운영자·통합 강제 초기화 | FAILURE → COOLDOWN | T7과 같음 |
| T12 | COOLDOWN | 잔여 0 | READY | — |
| **T13** | 활성 단계 | `trialRemaining` = 0 | **TIMED_OUT → COOLDOWN** | §4.4·§10. **일반 몹과 보급 상자는 그대로 두고** 시험을 해제 시험으로 기록(`released_trials`). 보스가 있으면(`boss.started`) **`boss_vanish` 작업을 만들고 로드된 보스는 즉시 제거**, 중앙 수정 종료 연출 이벤트를 1회 발행. 저널을 복원 대상으로 전환, 쿨다운 78,000. 웨이브 진행·기록·보상 없음 |

부재 판정 기준:
- ROUND·BOSS: 이번 라운드 잠금 참가자 중 사망 제외가 아닌 사람이 구역 안에 있는가.
- INTERMISSION: 적격 플레이어가 구역 안에 있는가.

### 4.3 불변식

- **I1** 동시 생존 시험 몹 ≤ 12(경고 포함).
- **I2** 할당 상태는 PENDING/WARNING/ALIVE/KILLED 중 하나이고 합은 할당 총수다. 라운드 중 할당 수는 변하지 않는다.
- **I3** KILLED는 ALIVE에서만 바뀔 수 있고 되돌릴 수 없다.
- **I4** 한 `(trialId, round)`에서 웨이브 기록은 1회, 공유 열쇠 **발급 완료**는 1개다(§9.4).
- **I5** 소유 태그가 없는 엔티티(외부 몹), 그리고 해제된 시험의 **일반 몹**은 예산·집계·정리·진행에 영향을 주지 않는다. 보스는 해제되지 않는다(I11).
- **I6** 위치 확인 실패는 할당을 소모하지 않는다.
- **I7** 부재 정지 중에는 라운드 완료, 휴식 진행, 새 소환 배치가 없다. Q116 제한 시간만 흐른다.
- **I8** 장부(웨이브·자격·수령·열쇠·해제 시험 기록)는 어떤 시험 결과로도 삭제되지 않는다(Q115-B). 보스 소멸 작업과 복원 저널은 기록이 아니라 완료되면 제거되는 작업 항목이며 I8의 대상이 아니다.
- **I9** 컨트롤러는 Vault 블록 엔티티를 읽거나 쓰지 않는다.
- **I10** 제한 시간(`trialRemaining`)과 쿨다운(`cooldownRemaining`)은 별개의 필드다. 하나가 다른 하나를 초기화하거나 대신하지 않는다.
- **I11** 시간 초과가 보스에 미치는 영향은 소멸뿐이다. 소멸은 사망·처치·성공이 아니며 보스 사망 이벤트, 전리품, XP, Ominous Trial Key, 장부 기록을 만들지 않는다. 성공은 시험이 BOSS 단계이고 `trialId`가 일치할 때의 처치 보고로만 생긴다.
- **I12** 보스 소멸 작업은 멱등이고 재시작 가능하다. 한 시험에는 소멸 작업이 최대 1개이고, 보스는 소멸을 한 번만 겪는다.
- **I13** 복원 저널에는 좌표마다 미완료 항목이 최대 1개 있고, 그 항목은 그 좌표가 처음 파괴되기 전의 상태를 가진다.

### 4.4 종료 결과 구분

세 종료는 처리 내용이 서로 다르다. 성공·실패·시간 초과는 `last_outcome`으로 구분하며 서로 바뀌지 않는다.

| 종료 | outcome | 일반 시험 몹 | 보급 상자 | R7 보스 | 블록 복원(Q91) | 열쇠·장부 | 쿨다운 | 중앙 수정 종료 연출 |
|---|---|---|---|---|---|---|---|---|
| 성공(T6) | `SUCCESS` | 이미 전부 처치됨 | 정리(Q96) | 처치됨. Ominous Trial Key는 보스 발밑 드롭(C5) | 복원 | 장부 유지 | 78,000 | 없음 |
| 실패·초기화(T7–T11) | `FAILED_*` | **제거**(무드롭·무XP, Q102) | **정리**(Q96) | **제거**(무드롭·무XP, Q102). 연출은 권위에 규정 없음 | 복원 | 열쇠 회수 없음(Q97) | 78,000 | 없음(권위에 규정 없음) |
| 시간 초과(T13) | `TIMED_OUT` | **잔류, 해제**(Q116) | **잔류**(Q116) | **소멸**(Q117). 처치 아님 | 복원 | 열쇠 회수 없음(Q97), `released_trials` 추가 | 78,000 | **있음**(보스가 있었을 때만, Q117) |

### 4.5 같은 틱 경합의 처리 순서

권위에 같은 틱 경합의 규정이 없으므로, 아래는 새 게임 규칙이 아니라 결정적 구현 순서다. 발생 확률은 1틱 일치라 극히 낮고 O-11에서 확인을 권한다.

1. 보스 처치 보고는 엔티티 사망 처리 중에 즉시 반영된다. 컨트롤러 틱(`END_LEVEL_TICK`)보다 먼저 일어났다면 SUCCESS가 된다.
2. T13이 처리된 뒤에는 BOSS 단계가 아니므로 처치 보고는 거부된다. 이때 보스는 이미 제거되어 있다.
3. 컨트롤러 틱 안의 순서는 만료(T13) → 전멸(T7) → 부재(T8) → 단계 진행이다. 같은 틱에 여럿이 성립하면 앞선 것이 결과가 된다.

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

`boss.entity`도 같은 표로 확인한다. 단 BOSS 단계에서 보스가 `GONE`인 것은 Q111-A의 재소환 대상이 아니다. 보스를 다시 만들지는 C5 핸들러가 정하며, 컨트롤러는 이를 처치로 해석하지 않는다(R-8).

### 6.4 Q111-A — 배정 반환과 재소환

다음 사건은 할당을 PENDING으로 되돌리고 정상 배치에서 다시 소환한다. 유효 처치 수는 늘지 않고 `requeues`만 증가한다.

| 사건 | 분류 |
|---|---|
| 처치 판정 순간 구역 안 적격 플레이어 없음 | 미인정 사망 |
| 디스폰, `discard`, 차원 이동, 변환(원본) | 실제 소실 |
| 재시작 후 마지막 청크가 로드됐는데 엔티티 없음 | 실제 소실 |
| 청크 언로드 | 소실 아님 → 유지 |

TIMED_OUT 이후에는 할당이 없으므로 재소환도 없다(§9.6). 해제된 일반 몹의 소실과 사망은 Q111-A 대상이 아니다(§10.6).

## 7. 시간 관리 — 온라인 틱 기준(Q112-A)

| 필드 | 초기값 | 감소 조건 | 만료 시 |
|---|---|---|---|
| `trialRemaining` (Q116) | 78,000 (R1 시작) | 활성 단계의 매 틱. **부재 정지 중에도 감소** | T13 TIMED_OUT |
| `absenceTicks` | 0 | 부재 중 매 틱 **증가** | T8 |
| `blockedTicks` | 0 | Q110: 배치가 필요한데 유효 위치가 없는 미정지 틱마다 증가, 정원·6 초과 대기 중 보류, 유효 위치가 생기면 0 | T9 |
| `intermissionRemaining` | 200 / 700 | 미정지 INTERMISSION | T3/T4 |
| 경고 `warningTicks` | 20 | 미정지 ROUND | 생성 시도 |
| `cooldownRemaining` | 78,000 | COOLDOWN 매 틱 | T12 |
| 작업 항목(`boss_vanish`, `restore_journal`) | — | 시간으로 줄지 않는다. 매 틱 처리하고 복원은 틱당 N개(구현 상수) | 작업 완료 |

- 모든 값은 남은(또는 누적된) **틱 수**로 저장한다.
- 서버가 해당 차원을 틱할 때만 움직인다. 서버 정지나 빈 서버 일시정지(`pause-when-empty-seconds`) 동안은 멈춘다.
- 컨트롤러는 아레나 청크가 미로드여도 매 틱 돈다. Q117의 소멸 시작과 복원 시작도 만료 틱의 온라인 시간에 일어나며, 서버가 꺼져 있던 시간은 작업에 영향을 주지 않는다.
- 벽시계와 `gameTime` 차이는 판정에 쓰지 않는다. `gameTime`은 기록용으로만 쓴다.
- 이미 75,000틱이 지난 시험이 R7에 있어도 남은 3,000틱이 끝나면 TIMED_OUT이다. 보스 핸들러가 없는 현재는 R7 대기가 결국 TIMED_OUT 또는 부재 FAILURE로 끝난다.

## 8. 저장 데이터 스키마와 복구

### 8.1 위치

차원별 `SavedData`, id `moreminecrafts:st05_colosseum_arenas`, `DataFixTypes.LEVEL`.

### 8.2 스키마 v1

```text
root { schema: int = 1, arenas: [ArenaRecord], archived: [ArchivedArena] }
ArenaRecord {
  instance_id: string
  geometry: { center, combat_radius, min_y, max_y, activation_radius, spawn_min_y, spawn_max_y }
  trial:  TrialState
  ledger: Ledger                       # 추가 전용, 무기한 보존
  boss_vanish:     [BossVanish]        # Q117 보스 소멸 작업(완료되면 제거되는 작업 항목)
  restore_journal: [JournalEntry]      # Q91·Q117 복원 작업 단위(복원되면 제거되는 작업 항목)
}
ArchivedArena { instance_id, ledger }  # D3: 등록 해제된 아레나의 장부 보관
TrialState {
  core:    { phase, last_outcome, trial?, last_trial?, serial, seed, round }
  timers:  { trial_remaining, intermission, absence, spawn_blocked_ticks, spawn_blocked, batch_timer, cooldown }
  party:   { locked:[uuid], dead:[uuid], ever:[uuid] }
  content: { allocations:[Allocation], supplies:[long], boss: { started, entity?, chunk } }
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
  key: { state: NONE|PENDING|SPAWNED|CONFIRMED|CONSUMED, entity?, chunk, issue_count:int }
}
EligibilityRecord { trial, round, player, basis = "Q114-C", recorded_game_time }
ClaimRecord       { trial, round, player, reward_id, delivered_game_time }
BossVanish        { trial, entity, chunk, state: PENDING|REMOVED, started_game_time }
JournalEntry      { trial, pos, prior_state, block_entity_nbt?, left_state? }
```

- **enum 저장:** 이름 문자열로 저장한다.
- **필드 형식:** 새 필드는 `optionalFieldOf`에 기본값을 둔다. 덕분에 v1 안의 확장은 하위 호환이 된다.
- **outcome:** `last_outcome`에 `TIMED_OUT`을 추가한다.
- **기록과 작업 항목의 구분:** `ledger`의 네 목록과 각 웨이브의 열쇠 상태가 기록이다(삭제 금지, I8). `boss_vanish`와 `restore_journal`은 작업 항목이라 완료되면 제거한다.
- **로드 검증:** 장부의 유일 키가 중복되면 첫 항목만 남기고 로그를 남긴다. 다른 항목은 지우지 않는다.

### 8.3 버전 관리

| 읽은 `schema` | 처리 |
|---|---|
| 없음 (v0 = #213 형식) | `instance_id = test|<dim>|<center>`. `cleared_waves`를 `ledger.waves`로 옮기고 `key.state = NONE`(v0에는 열쇠 발급이 없었으므로 새로 발급하지 않는다). `timers.boss_started`를 `content.boss.started`로 옮긴다. `trial_remaining`이 없으면 활성 시험은 78,000으로 시작 |
| 1 | 그대로 |
| > 1 | **보존 모드**: 틱하지 않고, 원본을 그대로 다시 쓰고, 오류 로그를 남긴다. 삭제 금지 |

### 8.4 저장 순서와 크래시 대응

같은 틱 안의 상태 변경과 장부·작업 항목 변경은 같은 `SavedData`에서 일어난다. 엔티티와 블록은 청크 저장에 들어가며 두 저장은 원자적이지 않다.

| 위험 | 대응 |
|---|---|
| WARNING인데 엔티티가 이미 저장됨 | 로드 시 고아로 거부 → 경고 종료 후 1회만 생성 |
| ALIVE인데 엔티티가 저장되지 않음 | 실제 소실 → 재소환(Q111-A) |
| 처치는 저장됐는데 죽어 가던 엔티티가 저장됨 | 로드 시 KILLED로 판정해 거부 |
| 공유 열쇠 발급 중 크래시 | §9.4의 상태 기계로 유실 복구 |
| T13 처리 중 크래시: 보스 제거는 청크에 저장됐는데 SavedData는 이전 상태 | 시험이 BOSS 단계에서 보스 없이 재개된다. 처치로 해석하지 않는다(I11). 만료는 다시 평가되고 보스가 `GONE`이면 소멸 작업이 `REMOVED`가 된다(R-8) |
| 저널 항목은 저장됐는데 블록은 아직 안 부서짐 | 복원 때 현재 상태가 `prior_state`와 같으므로 아무것도 하지 않고 항목만 제거(§10.4) |
| 블록은 부서졌는데 저널이 저장되지 않음 | 저널을 먼저 쓰는 순서(`journalBreak` → 파괴)로 막는다. 저장 비원자성 때문에 크래시에서는 극히 드물게 복원이 누락될 수 있다(R-11) |

"정확한 복원"(Q112-A)의 범위: 단계, 라운드, 참가자, 모든 할당 상태와 엔티티 ID, 보스 기록, 경고 위치와 남은 틱, 모든 타이머, 장부, 보스 소멸 작업, 복원 저널. 몹의 위치·체력·AI 목표는 바닐라 엔티티 저장에 따른다.

### 8.5 복구 절차(서버 시작 시)

1. 스키마 버전을 처리한다(§8.3).
2. 일시 단계가 남아 있으면 해당 결과의 처리를 마치고 COOLDOWN으로 만든다(TIMED_OUT은 일반 몹·상자를 건드리지 않는다).
3. ALIVE 할당과 `boss.entity`는 청크 엔티티 로드가 존재 또는 부재를 증명할 때까지 유지한다.
4. WARNING은 남은 틱부터 이어 간다.
5. 엔티티를 로드할 때 I-ID2를 적용한다(활성 일치는 수락, 불일치·실패·성공 시험은 거부, 시간 초과 시험은 일반 몹 수락·보스 거부).
6. `boss_vanish=PENDING`은 §10.2의 세 경로로 이어서 완료한다.
7. `restore_journal`은 복원 대상(활성 시험이 아닌 항목)을 청크가 로드된 만큼 틱당 N개씩 처리한다(§10.4).
8. 공유 열쇠 레코드가 `SPAWNED`이면 §9.4의 확인 절차를 따른다.
9. 장부는 §9.4의 열쇠 상태 전이 외에는 쓰지 않는다.
10. BOSS 단계이고 `boss.started`이면 보스 핸들러에 재개를 알린다.
11. 타이머는 저장값부터 이어 간다. 참가자가 재접속하기 전에는 부재 시계와 Q116 시계가 흐른다.

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
| NONE | 이 웨이브에는 열쇠 발급 장치가 없었음(v0 이전, C2 이전 빌드). **발급하지 않는다** | 이전·C1 단독 빌드의 웨이브 기록 |
| PENDING | 발급 결정됨, 아직 생성 안 함 | T2에서 웨이브 기록과 함께(C2 이후) |
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
- 아레나 등록 해제는 장부가 비어 있지 않거나 미완료 작업 항목이 있으면 거부한다. 운영 목적의 해제는 장부를 같은 `instance_id`의 보관 레코드로 옮기며, 복원 저널이 남아 있으면 이 경로도 거부한다(§11.2).
- 보존 기간은 무기한이다. 시험당 최대 6웨이브이고 참가자 4명 기준 24건 수준이라 크기 위험은 낮다.

### 9.6 시간 초과 후(Q116, Q117)

- 해제된 시험의 **일반 몹**은 외부 몹처럼 동작한다. 처치하면 바닐라 드롭과 XP는 나오지만, 웨이브 진행·연출·Trial Key·Ominous Trial Key·자격·수령 기록은 없다.
- **보스는 남지 않는다.** Q117에 따라 소멸하며 처치로 인정하지 않는다(§10). 보스 전리품, XP, Ominous Trial Key는 없다.
- 보급 상자와 그 내용물은 남는다. 정리 목록(`supplies`)을 해제하고 다시 다루지 않는다.
- 쿨다운이 끝나 새 시험이 시작되어도 이전 시험의 남은 몹은 새 시험의 할당·예산·정원에 포함되지 않는다(I5).

### 9.7 Vault 보존

- 컨트롤러와 장부는 Vault 블록·블록 엔티티(`rewarded_players` 포함)를 읽거나 쓰지 않는다.
- 일반 6 + Ominous 1, 모두 바닐라 독립 개방이다.
- 열쇠 아이템 스택에는 컴포넌트를 추가하지 않는다. Minecraft 26.3 `VaultBlockEntity.isValidToInsert`가 `ItemStack.isSameItemSameComponents`로 열쇠를 대조함을 바이트코드로 확인했다. 열쇠 추적은 엔티티 attachment로만 한다.

## 10. 시간 초과 종료와 보스 소멸(Q117)

### 10.1 규칙과 이 계약의 대응

| Q117 | 이 계약의 처리 |
|---|---|
| 65분이 만료되면 시험 종료 | T13, outcome `TIMED_OUT`(§4.2, §4.4) |
| 일반 몹은 Q116대로 잔류, 이후 바닐라 드롭·XP만 | 해제 시험(`released_trials`)과 I-ID2의 `MOB` 행. 태그를 떼어 외부 몹으로 만든다(§10.6) |
| 보스는 중앙 수정 종료 연출과 함께 소멸 | T13이 `boss_vanish` 작업을 만들고 `CRYSTAL_ENDING` 이벤트를 1회 발행(§10.2, §10.3) |
| 소멸은 처치가 아님 | `discard`(사망 없이 제거). 처치 보고 불가(I11) |
| Ominous Trial Key·보스 전리품·XP 없음 | `discard`는 전리품과 XP를 만들지 않는다. 열쇠·장부 기록 없음 |
| 78,000틱 쿨다운 | T13의 쿨다운(제한 시간과 별개 필드, I10) |
| 보스가 파괴한 자연 블록은 Q91 조건으로 복원 | 복원 저널(§10.4) |
| 재시작 시 미완료 소멸·복원 재개 | 작업 항목의 영속성과 재개 절차(§10.5) |

권위 문구에서 구현이 한 가지로 정해지지 않는 곳은 아래의 최소 해석을 쓴다. 새 게임 규칙이 아니며, (b)는 O-12에서 확인을 권한다.

- (a) 소멸은 시험 종료와 같은 처리(T13)에서 시작한다. 보스가 로드되어 있으면 그 틱에 제거한다. 종료 뒤에 보스가 계속 행동하는 구간을 만들지 않는다.
- (b) 종료 연출은 보스가 있었던 시험(`boss.started`)에서만 발행한다. R1–R6 중 시간 초과면 소멸할 보스가 없으므로 연출 이벤트가 없다. 수정은 쿨다운 상태(어두운 빛, Q94)로 바뀐다.
- (c) 연출의 길이·입자·소리는 계약이 정하지 않는다. 권위는 시각 에셋과 연출 세부를 구현 작업으로 남겼다. 연출은 알림일 뿐 결과에 영향을 주지 않는다.

### 10.2 보스 소멸 작업

`boss_vanish` 항목은 T13에서 `boss.started`일 때 만들며 한 시험에 최대 1개다.

| 상태 | 의미 | 다음 |
|---|---|---|
| `PENDING` | 소멸이 아직 끝나지 않음 | 아래 세 경로 중 하나로 `REMOVED` |
| `REMOVED` | 보스가 월드에서 없어졌음이 확인됨 | 다음 틱에 항목 제거, 로그와 `BOSS_VANISHED` 알림 |

`PENDING`에서 `REMOVED`로 가는 경로:

1. **로드된 보스를 제거함:** T13 처리 중 `locate`가 `ALIVE`이면 `discard`(RemovalReason `DISCARDED`)로 제거한다. 사망 처리를 거치지 않는다.
2. **로드 때 거부함:** 보스의 청크가 미로드이면 `PENDING`으로 둔다. 나중에 그 청크의 보스가 로드되려 할 때 ALLOW_LOAD가 I-ID2의 `BOSS` 행에 따라 거부한다. 이때도 사망 없이 제거되며 작업이 `REMOVED`가 된다.
3. **이미 없음을 확인함:** 보스 청크의 엔티티가 로드됐는데 그 UUID가 없으면(`GONE`) 이미 사라진 것이다. `REMOVED`로 처리한다. 처치로 해석하지 않는다.

- 같은 시험의 낡은 보스 사본은 UUID와 무관하게 `BOSS` 행으로 거부한다. 보스는 시험당 하나뿐이라는 전제를 사본에도 적용하는 것이다.
- 소멸이 끝나기 전에 새 시험이 시작되어도 두 시험의 작업은 시험 ID로 구분되어 서로 영향을 주지 않는다.
- 보스는 차원을 넘지 않는다. 보스가 포털로 다른 차원에 갈 수 없게 하는 것은 C5 보스의 요건이다(C5 선행조건).

### 10.3 중앙 수정 종료 연출 — 서버 권위 이벤트

- **API:** `ColosseumIntegration.CRYSTAL_ENDING : Event<CrystalEnding>`, `onCrystalEnding(ServerLevel level, ColosseumArena arena, UUID trialId, EndingReason reason)`. `EndingReason`은 `TIME_OUT` 하나다.
- **발행 주체:** 컨트롤러가 포트(`crystalEnding(trialId, reason)`)를 통해 발행한다. 서버 상태 전이의 결과이며 클라이언트 입력이 아니다.
- **발행 조건과 횟수:** T13에서 `boss.started`이면 정확히 1회, `boss_vanish` 작업을 만드는 같은 처리에서 발행한다. 발행 순서는 상태 전이 → 로드된 보스 제거 → `CRYSTAL_ENDING` → `TRIAL_ENDED`다.
- **수신자:** 이후 C5 보스와 시각 효과(입자, 소리, 클라이언트 패킷)다. 수신자는 상태를 바꾸거나 소멸을 늦추거나 취소할 수 없고, 수신자의 예외는 기록만 한다.
- **위치:** 수정은 정확한 중심 바로 위이므로 `arena.geometry().center()`를 기준으로 한다.
- **재실행:** 저장되기 전에 크래시가 나면 T13이 재시작 뒤 다시 평가되어 시각 효과가 한 번 더 나올 수 있다. 상태에는 영향이 없다(R-10).

### 10.4 복원 저널과 복원 절차

**기록(C5 보스의 책임):** 보스가 블록을 부수기 **전에** `ColosseumIntegration.journalBreak(level, arena, trialId, pos, priorState)`를 호출해 저널에 항목을 추가한다. 항목은 `(trial, pos, prior_state, block_entity_nbt?, left_state?)`다.

- Q91의 보호 조건은 호출 쪽에서 판정한다. 허용된 외부 자연 석재·목재 계열만 호출할 수 있다.
- 원래의 구조물 블록, 바닥, 벽, 관람석, 장식, Vault, 수정, 부술 수 없는 블록, 플레이어 건축은 호출하지 않는다. Q91이 "블록 종류만으로는 불충분"이라고 하므로 구조물 점유 마스크가 필요하다.
- 구조물 점유 마스크는 C4(월드젠)가 제공한다. 그래서 C5의 선행조건이다.
- `left_state`는 보스가 남긴 상태(보통 공기)다. Q118-A의 "빈 위치" 판정에 쓴다(없으면 공기).

**복원 대상 전환:** 저널 항목의 `trial`이 지금 활성 시험이 아니게 되는 순간 그 항목은 복원 대상이 된다. 성공, 실패, 초기화, 시간 초과가 모두 해당한다. 활성 시험 중에는 복원하지 않는다.

**복원 절차:** 아레나마다 틱당 최대 N개(구현 상수)를 처리한다. 청크가 로드된 항목만 처리한다.

| 현재 블록 상태 | 처리 |
|---|---|
| `prior_state`와 같음 | 할 일 없음. 항목 제거(멱등) |
| `left_state`와 같음(보통 공기) | `prior_state`로 설정하고 항목 제거 |
| 그 밖(플레이어가 놓은 블록, 물이나 모래 등이 채움) | **Q118-A: 덮어쓰지 않는다.** 항목은 `CONFLICT`로 기록되고(시각, 발견한 블록) 블록은 그대로다 |

- 마지막 행은 Q91("정확한 이전 상태로 복원")과 Q105("플레이어 개조를 되돌리지 않음")가 만나는 경계였고, 소유자가 Q118-A로 결정했다.
- 충돌 항목은 이후에도 복원 대상으로 다시 평가된다(구현 해석, O-13). 자리가 비면 첫 두 행처럼 복원되고, 계속 점유되어 있으면 충돌로 남는다.
- 청크가 로드되지 않은 항목은 충돌이 아니라 대기다. 충돌 기록과 대기 항목은 재시작 뒤에도 유지된다.
- `journalBreak`가 이미 미완료 항목이 있는 좌표에 오면 새 항목을 만들지 않고 기존 항목을 유지한다. `left_state`만 갱신한다(I13). 그래야 최종 복원 결과가 처음의 자연 상태가 된다.
- 저널을 먼저 쓰고 블록을 부수는 순서를 지킨다. 그 사이에 크래시가 나면 항목은 있는데 블록은 그대로이므로 첫 행에 해당해 아무 일도 하지 않는다.

### 10.5 청크 언로드와 재시작

| 시점 | 저장된 상태 | 재개 방법 |
|---|---|---|
| 만료 때 보스 청크가 미로드 | `boss_vanish=PENDING`, `released_trials`에 시험 ID | 청크가 로드되어 보스가 올라올 때 거부(제거)하고 `REMOVED` |
| 만료 직후 재시작, 보스 로드 전 | 위와 같음(같은 SavedData) | 재시작 후 보스 엔티티가 월드에 들어가기 전에 `BOSS` 행으로 거부 |
| 복원 항목의 청크가 미로드 | 저널 항목 유지 | 청크가 로드된 뒤 틱당 N개 처리 |
| 복원 도중 재시작 | 남은 저널 항목 | 이어서 처리. 이미 복원한 항목은 블록 변경과 항목 제거가 같은 틱에 일어나 저장 시 함께 반영되고, 어긋나도 첫 행(멱등)이 처리 |
| T13 처리 중 저장 이전 크래시 | T13 이전의 상태(`trialRemaining`이 거의 0) | 재시작 뒤 T13이 다시 평가됨. 보스가 `GONE`이면 `REMOVED`(처치 아님). 일반 몹은 해제 전 상태로 돌아가고 T13에서 다시 해제 |
| 재시작 후 `trialRemaining` | 저장된 값 그대로 | 오프라인 시간은 소모하지 않는다(Q112-A) |

### 10.6 일반 몹의 시험 소유권 해제

- T13에서 현재 로드된 ALIVE 할당 몹은 태그를 제거해 외부 몹으로 만든다. 로드되지 않은 몹은 `released_trials`와 I-ID2에 따라 로드될 때 같은 처리를 받는다(수락하고 태그 제거).
- 할당 목록을 비우므로 해제된 몹은 새 시험의 예산·정원·할당에 포함되지 않는다(I5).
- 해제된 몹의 지속성 플래그, AI, 장비, 드롭 테이블은 바꾸지 않는다. Q116이 정리하지 않는다고 했고 그 변경은 승인된 적이 없다(R-9).
- 해제된 몹이 죽으면 바닐라 드롭과 XP만 나온다. 이벤트·웨이브 진행·자격·열쇠는 없다.
- 해제된 몹의 소실과 사망은 Q111-A 대상이 아니다(할당이 없다).
- `released_trials`는 삭제하지 않는다(R-7).

### 10.7 Q117과 기존 규칙의 정합성

| 규칙 | Q117과의 관계 | 결과 |
|---|---|---|
| Q116 | 보스 조항만 Q117이 대체. 일반 몹·보급 상자 잔류와 "잔류물 처치는 바닐라 드롭·XP만"은 그대로 | 충돌 없음. 권위 문서에 대체 범위를 주석으로 표시 |
| Q91 | 복원 대상(허용된 외부 자연 블록)과 복원 정확도(정확한 이전 상태)를 따른다. 시간 초과도 "시험 종료"에 해당 | 충돌 없음. 플레이어가 점유한 자리는 Q118-A |
| Q97 | 이미 지급한 열쇠는 회수하지 않는다. 소멸은 열쇠를 만들지도 회수하지도 않는다 | 충돌 없음 |
| Q99 | 78,000틱 쿨다운이 성공·실패·초기화·시간 초과에 같다 | 충돌 없음 |
| Q102 | 실패·초기화의 보스는 즉시 제거(연출 규정 없음). Q117은 시간 초과의 보스를 연출과 함께 제거 | 서로 다른 종료라 충돌 없음(§4.4) |
| Q105 | 플레이어 건축은 정리 때문에 지워지지 않는다. Q117은 보스가 부순 자연 블록만 복원 | 충돌 없음. 경계 사례는 Q118-A로 결정됨 |
| Q110 | Q110은 R1–R6의 소환 차단(T9)이고 BOSS 단계에는 적용되지 않는다. 만료는 T9보다 앞선다 | 충돌 없음. 경합 순서는 §4.5 |
| Q112-A | 만료 판정과 작업 처리는 모두 온라인 틱으로 움직인다. 재시작 뒤 미완료 작업을 재개한다 | 충돌 없음 |

## 11. 인터페이스와 책임 분리

| 구성요소 | 책임 | 하지 않는 것 |
|---|---|---|
| `ColosseumController` (순수) | 전이, 타이머, 할당 집계, 장부 쓰기 결정, 열쇠 상태 전이, 종료 결과별 처리(§4.4), 보스 소멸 작업 상태 전이 | 월드 접근 |
| `ColosseumWorldPort` | 존재 판정(`locate`), 위치 검사, 소환, `discard`, 효과, 열쇠 엔티티 생성·조회, `crystalEnding`, `restoreBlock(entry)` | 규칙 판단 |
| `ColosseumArenaData` | 스키마 v1, v0 이전, 보존 모드, `archived` | 판정 |
| `ColosseumOwnership` | 몹·보스 소유 태그(`kind`), 열쇠 태그 | — |
| `ColosseumRuntime` | 틱, 사망, 언로드, 로드 허용 연결, I-ID2 적용, 테스트용 컨트롤러 보류 플래그(§11.5) | — |
| `RewardLedger` (C1) | 장부 4목록, 열쇠 상태, 유일 키, 추가 전용 | 아이템 생성 |
| `RestoreJournal` (C1) | 좌표당 1항목, 복원 대상 전환, 복원 절차, Q118-A 충돌 기록 | 보스 행동, 블록 파괴 |
| `KeyIssuer` (C2) | 공유 열쇠 생성과 §9.4 확인 | 발급 여부 결정 |
| `SupplyHandler` (C3) | 보급 상자 배치, 성공·실패 정리, 시간 초과 시 방치 | 플레이어 상자 접근 |
| `StructureRegistration` (C4) | `instance_id`, 기하, **구조물 점유 마스크**(Q91 보호 조건용) 제공 | 시험 진행 |
| `BossHandler` (C5) | 보스 생성·추적, 엔티티 밖 정리(보스 바 등), 처치 보고 | 웨이브 장부, 소멸 시점 결정 |
| 이벤트 | `WAVE_CLEARED`(장부 기록 후 1회), `TRIAL_ENDED`(outcome에 `TIMED_OUT` 추가), `CRYSTAL_ENDING`(Q117, §10.3), `BOSS_VANISHED`(작업 완료 알림) | — |

`BossHandler` 계약:

```java
boolean handles(ServerLevel level, ColosseumArena arena);
// 월드에 추가하기 전에 bind(entityUuid, chunk)를 호출한다(일반 몹 소환과 같은 방식). 성공하면 true.
boolean start(ServerLevel level, ColosseumArena arena, UUID trialId, List<UUID> locked, Binder bind);
// 보스가 제거된 직후 호출. 전리품·XP·사망 연출을 만들지 않는다. cause = VANISH(Q117) | REMOVE(Q102)
void onRemoved(ServerLevel level, ColosseumArena arena, UUID trialId, RemovalCause cause);
```

#213의 `cleanup(level, arena, trialId)`는 `onRemoved`로 대체한다. 보스 엔티티의 제거 자체는 컨트롤러가 `discard`로 수행하므로 핸들러가 소멸 시점을 정하지 않는다.

### 11.1 PR #213과 C0 계약의 차이

기준은 #213 `befe135`의 코드다. D1–D9는 이전 보고의 번호를 유지하고, Q117과 테스트 결정성에서 나온 D10–D12를 추가했다. 코드 위치는 최신 코드와 다시 대조한 값이다.

| # | #213 현재(`befe135`에서 확인) | 계약 | 분류 |
|---|---|---|---|
| D1 | `ColosseumArenaData`의 코덱이 아레나 리스트(`ColosseumArena.CODEC.listOf()`)이고 루트에 `schema`가 없다 | v1 루트 `{schema, arenas, archived}`, v0 이전, 미래 버전 보존 모드 | 저장 형식 |
| **D2** | `ColosseumController.startTrial`(l.50)이 `state.clearedWaves.clear()`를 호출한다. `ColosseumTrialState.waveCleared(int round)`(l.141)는 라운드 번호만 비교하고 `completeRound`가 이 값으로 중복 완료를 막는다 | 장부 무기한 보존(I8). 비교 키는 `(trialId, round)` | **삭제 금지 위반, 필수** |
| **D3** | `ColosseumIntegration.unregisterArena`(l.94–100)가 `forceReset` 뒤 `data.remove`로 기록째 지운다. GameTest가 이를 여러 곳에서 호출한다(l.88, 119, 509–511, 591, 599, 625) | 장부가 있으면 거부, 강제 해제는 `archiveAndUnregister`(보관 후 해제) | **삭제 금지 위반, 필수** |
| D4 | 아레나 식별이 중심 좌표 long뿐이다 | `instance_id` 추가(좌표 키는 색인으로 유지) | 월드젠 연동 |
| D5 | 웨이브 기록 `WaveRecord{trial, round, locked, present, gameTime}` 하나뿐이다 | 장부 4목록, 열쇠 상태, Q114-C 자격 기록 | 보상 |
| D6 | 공유 열쇠 발급이 없다(의도) | C2에서 §9.4 상태 기계 | 보상 |
| D7 | `ColosseumOwnership.Tag{arena, trial, allocation}` | `{instance, trial, round, kind, allocation}`. **`kind`는 Q117로 필수**(보스 행 구분). 이전에는 선택이었다 | 필수 |
| D8 | 시험 전체 제한 시간이 없다 | `trialRemaining`, T13 | Q116 |
| D9 | `endTrial`(l.252)이 비성공 종료마다 ALIVE 몹을 `discard`하고 보급 상자를 정리한다. `trialId=null`과 `allocations.clear()`로 남은 태그 몹을 로드 때 거부한다 | 시간 초과는 몹·상자 잔류, `released_trials`, 일반 몹 수락과 태그 제거 | Q116 |
| D10 | `bossStarted`는 boolean 하나다(`ColosseumTrialState` l.110). `BossHandler.start`는 보스 UUID를 알리지 않는다. `cleanup`은 모든 종료에 동기로 호출된다 | `boss{started, entity, chunk}`, `start(…, Binder)`, `onRemoved`, `boss_vanish` 작업, `CRYSTAL_ENDING`, 복원 저널 | Q117 |
| D11 | `endTrial`이 종료 원인을 구분하지 않고 같은 정리를 한다 | 종료 결과별 처리(§4.4). 시간 초과는 일반 몹·상자를 건드리지 않고 보스만 소멸 | Q117 |
| D12 | 재시작 테스트가 실시간 틱에 의존한다. reload는 테스트가 시작되기 전부터 컨트롤러가 틱해 부재 시계가 쌓인다. verify는 플레이어가 없어 `FAILED_ABSENCE_RESET`을 정상으로 허용한다. write는 `blockedTicks`가 421–423으로 흔들려 `>= 300` 느슨 비교를 쓴다 | 컨트롤러 보류 플래그와 결정적 구동(§11.5), 정확한 값 단언 | 테스트 |

### 11.2 D2·D3 수정안(PR #213 별도 작업용)

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
- 장부가 비어 있지 않거나, 미완료 작업 항목(`boss_vanish`, `restore_journal`)이 있으면 등록 해제를 거부한다(`false` 반환, 이유를 로그에 남김). 작업 항목을 두고 해제하면 지형 복원과 보스 소멸이 영구히 누락되기 때문이다. 활성 시험의 초기화는 별도 명령 `reset`으로만 한다.
- 꼭 해제해야 하는 운영 경로는 `archiveAndUnregister`로 분리한다. 장부를 `ColosseumArenaData.archived`(키 `instance_id`)로 옮긴 뒤 활성 레코드만 지운다. 이 경로도 `restore_journal`이 비어 있지 않으면 거부한다(복원이 끝나야 한다).
- 같은 중심에 다시 등록되면 보관된 장부를 복원한다.

`/mmcolosseum unregister` 명령과 GameTest 픽스처:
- 새 의미(장부가 있으면 거부)에 맞춘다.
- 테스트 정리는 `archiveAndUnregister` 또는 테스트 전용 아레나를 쓴다.

테스트:
- 장부가 있는 아레나에 대한 `unregister`는 거부되고 데이터가 유지되어야 한다.
- 미완료 `boss_vanish`나 `restore_journal`이 있으면 `archiveAndUnregister`도 거부되어야 한다.
- `archiveAndUnregister` 후 다시 등록하면 장부가 복원되어야 한다.

### 11.3 우선 항목별 수정 계획 (PR #213 코드는 이 PR에서 수정하지 않는다)

| 항목 | 현재 코드(확인됨) | 변경 | 테스트 |
|---|---|---|---|
| D2 시험 ID별 라운드 기록 | `startTrial`의 `clearedWaves.clear()`, 라운드 번호만 비교하는 `waveCleared` | `.clear()` 삭제, 기록을 `RewardLedger`로 이동, 비교 키 `(trialId, round)`. 설계는 §11.2 | S17, S12 |
| D3 등록 해제 시 기록 삭제 | `unregisterArena`가 기록째 삭제. 테스트 픽스처가 여러 곳에서 호출 | 장부가 있으면 거부, `archiveAndUnregister` 추가, `/mmcolosseum unregister` 의미 조정. 픽스처는 정리 때 보관 해제를 쓰고, 시작 때의 방어적 해제 호출은 지운다(실행마다 무작위 기준 좌표가 충돌을 막는다) | S17 |
| D9 시간 초과와 일반 몹 | `endTrial`이 비성공 종료마다 몹·상자를 정리, `trialId=null`로 태그 몹을 로드 때 거부 | 종료 결과별 `endTrial`(§4.4), `released_trials`, I-ID2 표(§3), 해제 몹의 태그 제거 | S18, S20, S30, S33 |
| 재시작 테스트 비결정 | D12 | 컨트롤러 보류 플래그와 결정적 구동(§11.5) | S11, S12, S19, S27, S34 |
| Q111-A 미인정 소실 재소환 | `onTrialMobDeath`가 구역 안 플레이어가 없으면 `requeue`, `onTrialMobLost`와 `reconcileAlive`의 소실 판정, 언로드 사유 분기가 있고 JUnit·GameTest로 검증됨 | 코드 변경 없음 예상. 변환(`convertTo`) 사례를 검증한다. 소유 attachment는 `copyOnDeath`를 쓰지 않아(`ColosseumOwnership.TAG`) 변환된 몹에 복사되지 않는다고 판단하나, 테스트로 확인한다. D9 이후 해제 몹이 이 경로로 들어가지 않는지도 확인 | S8, S9 |
| Q112-A 저장·재시작 복구 | 상태·할당·경고·차단 시계는 저장·복원됨(프로세스 테스트 통과) | 스키마 v1, `trial_remaining`, `boss`, 작업 항목 저장. 재시작 시 오프라인 시간 미소모를 정확한 값으로 단언 | S11, S19, S21, S27, S34 |
| Q113~Q115 장부 정합성 | 웨이브 기록만 있고 자격·수령·열쇠 상태 없음 | 장부 4목록, 유일 키, 자격 기록(Q114-C), `tryClaim`(Q115-B), 열쇠 상태 `NONE`(C1) → C2에서 발급. 로드 때 중복 키는 첫 항목만 남기고 로그(삭제가 아니라 같은 키 중복의 정리) | S13–S17 |

### 11.4 커밋 순서와 테스트 범위

기본은 #213 브랜치에 이어서 커밋하는 것이다(검토 이력 보존). 8번 항목은 diff가 커서 검토가 어렵다면 별도 PR로 나눌 수 있다. 어느 쪽이든 설계 질문은 없다.

| 순서 | 내용 | 주요 파일 | 추가 테스트 | 승인 기준 |
|---|---|---|---|---|
| C1-1 | 식별자와 태그(D4, D7): `instanceId`, `Tag{round, kind}`, 이전 태그의 기본값 | `ColosseumArena`, `ColosseumOwnership`, `ColosseumServerPort` | `ColosseumOwnershipTagTest`(코덱 왕복, v0 기본값) | 기존 JUnit·GameTest 통과, 새 태그가 소환 경로에 붙음 |
| C1-2 | 스키마 v1(D1): 루트 `{schema, arenas, archived}`, v0 이전, 미래 버전 보존 | `ColosseumArenaData`, `ColosseumTrialState` | `ColosseumSchemaTest`(S21) | v0 입력이 장부 동등으로 이전, `schema>1`은 바이트 보존 |
| C1-3 | 장부(D2, D5, Q113–Q115): `RewardLedger`, `(trial, round)` 비교, 자격 기록, `tryClaim`, 중복 키 정리 | `ColosseumController`, `ColosseumTrialState`, 새 `RewardLedger` | `ColosseumLedgerTest`(S15–S17) | 시험이 바뀌어도 기록 보존, `WAVE_CLEARED`는 `(trial, round)`당 1회 |
| C1-4 | 등록 해제 보호(D3): 거부, `archiveAndUnregister`, 보관 복원 | `ColosseumIntegration`, `ColosseumArenaData`, `ColosseumDebugCommand`, GameTest 픽스처 | S17의 해제 사례 | 장부가 있는 아레나 해제가 거부, 보관 후 재등록 시 복원 |
| C1-5 | 제한 시간과 종료 결과 분리(D8, D9, D11): `trialRemaining`, T13, `endTrial` 분기, `released_trials`, I-ID2 표 | `ColosseumController`, `ColosseumRuntime`, `ColosseumTrialState` | `ColosseumTimeoutTest`(S18, S20, S31, S32) + GameTest | 시간 초과에 일반 몹·상자 잔류, 실패는 기존대로 제거 |
| C1-6 | Q117 배관(D10): `boss` 기록, `BossHandler` 계약, `boss_vanish`, `CRYSTAL_ENDING`, 복원 저널(Q118-A 충돌 기록 포함). 테스트용 스텁 보스(gametest 소스) 사용 | `ColosseumController`, `ColosseumIntegration`, `ColosseumServerPort`, 새 `RestoreJournal`, gametest 스텁 | `ColosseumBossVanishTest`, `RestoreJournalTest`, GameTest(S24–S26, S28–S30, S33, S35, S36) | 스텁 보스가 사망 없이 소멸, 전리품·XP·열쇠 없음, 저널 정확 복원 |
| C1-7 | 재시작 결정성(D12)과 Q117 재시작 시나리오: 보류 플래그, 3단계 재작성, 워크플로 확장 | `ColosseumRuntime`, gametest 테스트 모드 엔트리, `st05-colosseum-restart.yml` | S11, S12, S19, S27, S34 | 어떤 러너에서도 같은 값(허용 오차 0), 만료 허용 분기 삭제 |
| C1-8 | 회귀와 보고: 전체 JUnit·GameTest, 서버 스모크, PR 본문 갱신 | — | S23 | `clean test build compileGametestJava runGameTest` 통과, CI 전부 성공 |

선행 관계: C1-1 → C1-2 → C1-3 → C1-4 → C1-5 → C1-6이고, C1-7은 C1-2 이후에 병행할 수 있다. C1-4의 거부 규칙은 장부가 있어야 의미가 있으므로 C1-3 뒤에 둔다.

### 11.5 재시작 테스트의 결정성 설계

**원인(D12):** 재시작한 서버에서 컨트롤러는 테스트가 시작되기 전부터 틱한다. GameTest 서버는 틱을 실시간으로 제한하지 않으므로 러너 속도에 따라 부재 시계가 얼마나 쌓일지 달라진다. 그래서 verify는 만료를 정상으로 허용해야 했고, write의 `blockedTicks`는 421–423으로 흔들렸다.

**설계:**

- 메인 코드에 `ColosseumRuntime.setControllerHeld(boolean)`를 둔다. 정적 휘발성 값이고 저장하지 않으며 기본값은 `false`다.
- 보류 중이면 `tickLevel`이 컨트롤러를 틱하지 않는다. 사망·언로드·로드 허용 이벤트 처리는 계속한다.
- gametest 쪽 `main` 엔트리가 `SERVER_STARTING`(월드 로드와 첫 틱 이전)에서 시스템 속성 `moreminecrafts.st05.hold=true`이면 보류를 켠다. 메인 코드는 시스템 속성을 읽지 않는다.
- write: 보류 상태로 시작해 `advanceController`와 명시적 처치로만 진행한다. 값이 결정적이다(예: `blockedTicks == 400`).
- reload: 보류 상태로 시작해 저장된 값을 정확히 단언한다. 같은 UUID의 플레이어를 접속시킨 뒤 `setControllerHeld(false)`로 풀면 첫 틱부터 `absenceTicks == 0`이다.
- verify: 보류를 유지하고 저장된 상태만 단언한다. `FAILED_ABSENCE_RESET`을 허용하는 분기를 삭제한다.
- 워크플로의 write·reload·verify 모든 단계가 `JAVA_TOOL_OPTIONS`에 `-Dmoreminecrafts.st05.hold=true`를 넣는다.

**승인 기준:** 같은 입력에서 러너 속도와 무관하게 같은 값이 나온다(허용 오차 0). 부재나 시간 초과 만료를 정상 결과로 받아들이는 분기가 없다.

### 11.6 C1 구현에서 확인된 사항(PR #213)

구현 중에 계약과 달라졌거나 새로 확인된 점이다. 어느 것도 새 게임 규칙이 아니다.

- `BossVanish.State.REMOVED`는 일시 상태다. 같은 처리에서 항목을 제거하므로 저장되지 않는다. 저장되는 값은 `PENDING`뿐이다.
- 같은 자리에 떨어진 Trial Key는 바닐라 아이템처럼 한 스택으로 합쳐진다. 그러면 엔티티 수가 발급 수보다 작아진다. 합쳐져 사라진 엔티티의 기록은 정상 소멸(`CONSUMED`)이 되고, 검증은 엔티티 수가 아니라 스택 수량의 합으로 한다.
- 시험이 끝나 등록 해제된 아레나의 보관 장부도 해제된 시험을 기억한다. 그래서 보관 뒤 로드되는 일반 몹은 계속 잔류한다(Q116).
- 더 새로운 스키마로 저장된 데이터는 읽기 전용으로 보존하며, 그동안 소유 태그가 붙은 엔티티도 판정하지 않고 모두 받아들인다(알 수 없는 데이터로 엔티티를 지우지 않는다).
- `journalBreak`는 구조물 마스크가 등록되지 않으면 예외를 던진다(Q91은 블록 종류만으로 보호 대상을 가를 수 없다고 한다).
- 재시작 테스트의 결정성을 위해 `ColosseumRuntime.setControllerHeld`가 있다. 저장하지 않고 기본값은 `false`이며, 메인 코드는 시스템 속성을 읽지 않는다.

## 12. 테스트 시나리오와 승인 기준

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
| S9 | Q111-A 실제 소실 | 디스폰·`discard`·차원 이동·**변환** 후 재소환, 중복 없음, 변환된 몹은 태그 없음 | GameTest |
| S10 | 청크 언로드 | 같은 UUID 복귀, 재소환 없음 | GameTest |
| S11 | 재시작: 처치 직후 / 경고 중 / Q110 차단 중 | 정확한 값(허용 오차 0). 처치 유지, 죽어 가던 몹 거부, 경고 1회만 생성, 차단 시계 보존 | 프로세스 테스트 |
| S12 | 재시작: 클리어 직후 | 웨이브·자격 기록 유지, 이벤트 재발생 없음 | 프로세스 테스트 |
| S13 | 열쇠 중복 방지 | 같은 (trial, round) 완료를 두 번 처리해도 열쇠 1개, 같은 태그 엔티티 2개를 로드하면 1개만 남음 | JUnit + GameTest |
| S14 | 열쇠 유실 복구 | SPAWNED 저장 후 엔티티 없이 재시작하면 1회 재발급(`issue_count`=2), CONFIRMED에서 엔티티가 없으면 재발급 없음 | JUnit + 프로세스 테스트 |
| S15 | Q114-C 자격 | 잠금 ∧ 클리어 시 존재만 기록. 늦은 합류, 라운드 중 사망 후 밖, 이탈자 제외 | JUnit + GameTest |
| S16 | Q115-B 수령 | 자격만으로는 수령 없음, 열쇠 줍기는 수령 아님, `tryClaim` 두 번째 거부 | JUnit |
| S17 | 기록 보존 | 다음 시험 시작·실패·시간 초과 후에도 장부 불변(D2), `(trial, round)` 키로 새 시험 R1 정상 완료, `unregister` 거부와 보관 복원(D3) | JUnit |
| S18 | Q116 만료(R1–R6 중) | 78,000틱에 TIMED_OUT, 일반 몹·상자 잔류, 그 몹 처치 시 바닐라 드롭·XP만, 기록·열쇠 없음, 쿨다운 78,000 별도, 보스 작업·연출 없음 | JUnit + GameTest |
| S19 | Q112-A/Q116 재시작 시계 | 남은 제한 시간·쿨다운 보존, 오프라인 시간 미소모 | 프로세스 테스트 |
| S20 | 해제 시험의 일반 몹 로드 | TIMED_OUT 시험의 일반 몹은 청크 재로드 후에도 남고 태그가 제거됨. 실패 시험 몹은 거부 | GameTest |
| S21 | 스키마 | v0 → v1 이전(`boss_started` → `boss.started`, 열쇠 `NONE`), 미래 버전 보존 모드 | JUnit |
| S22 | Vault 비간섭 | 시험 전·중·후 Vault NBT 불변, 공유 열쇠로 Vault 개방 가능 | GameTest |
| S23 | 회귀 | 기존 GameTest 490 + 신규 전부, JUnit 전체, 서버 스모크 | CI |
| S24 | Q117 만료(BOSS) | 스텁 보스가 사망 없이 소멸, 일반 몹·보급 상자 잔류, outcome `TIMED_OUT`, 쿨다운 78,000, `CRYSTAL_ENDING` 정확히 1회, 이후 처치 보고 거부 | JUnit + GameTest |
| S25 | 소멸은 처치가 아님 | 보스 사망 이벤트 없음, 전리품·XP·Ominous Trial Key 아이템 엔티티 없음, `WAVE_CLEARED` 없음, 장부 불변 | GameTest |
| S26 | 보스 청크 미로드 상태의 만료 | `boss_vanish=PENDING` 유지, 청크 로드 때 사망 없이 거부·`REMOVED`, `BOSS_VANISHED` 정확히 1회 | GameTest |
| S27 | Q117 재시작 | 미완료 소멸과 복원 항목이 재시작 뒤 재개되어 한 번씩만 완료. 아래 단계 표 참조 | 프로세스 테스트 |
| S28 | 복원 저널 정확 복원 | 보스가 부순 허용 블록이 정확한 이전 상태로 복원, 구조물 블록은 변하지 않음, 미로드 청크 항목은 로드 뒤 복원 | JUnit + GameTest |
| S29 | 저널 좌표당 1항목 | 같은 좌표 재호출은 기존 항목 유지(원래 상태 보존), 복원은 멱등 | JUnit |
| S30 | 보스 소멸과 일반 몹 잔류의 분리 | 같은 만료에서 보스는 사라지고 일반 몹은 남음, 일반 몹 사망은 바닐라 드롭·XP | GameTest |
| S31 | 보스 없는 만료 | R1–R6 중 시간 초과면 `boss_vanish`와 연출 이벤트가 없음 | JUnit |
| S32 | 같은 틱 경합 | 같은 틱 선행 처치는 SUCCESS, 만료와 전멸·부재가 같은 틱이면 TIMED_OUT, 만료 뒤 처치 보고 거부 | JUnit |
| S33 | 낡은 사본 처리 | 해제 시험의 낡은 보스 사본은 UUID와 무관하게 거부(사망 없음), 일반 몹은 수락하고 태그 제거 | GameTest |
| S34 | Q112-A 오프라인 | 재시작 사이에 `trialRemaining`과 쿨다운이 정확히 같음 | 프로세스 테스트 |
| S35 | Q91 보호 조건 | 구조물·Vault·수정·플레이어 건축 좌표는 `journalBreak`가 거부하고 블록이 변하지 않음 | JUnit + GameTest |
| S36 | 저널 선기록 크래시 | 항목은 있는데 블록이 그대로면 복원 시 아무 일도 하지 않음 | JUnit |

S27(Q117 재시작) 단계: 모두 컨트롤러 보류 상태로 시작한다(§11.5).

| 단계 | 내용 |
|---|---|
| write | BOSS 단계의 스텁 보스와 일반 몹을 같은 먼 청크에 두고, 보스가 부순 블록 3곳을 저널에 기록한다. 그 청크를 언로드한 뒤 `trialRemaining`을 0으로 만들어 T13을 실행하고 저장한다 |
| reload | `boss_vanish=PENDING`, 저널 3개, `released_trials`가 저장됐음을 단언한다. 청크를 로드하면 보스가 사망 없이 제거되고, 일반 몹은 수락되어 태그가 제거되며, 블록 3곳이 복원된다. 저장한다 |
| verify | `boss_vanish`와 저널이 비어 있고, 보스 없음, 일반 몹 존재, 블록 복원, 쿨다운 보존, 장부 불변, `CRYSTAL_ENDING`과 `BOSS_VANISHED` 각 1회를 단언한다 |

LIVE VALIDATION PENDING: 몹 AI·표적, 입자, 밸런스, 종료 연출의 시각 품질, 실제 클라이언트 다인 플레이·재접속, 열쇠 픽업·Vault 개방 체감, 해제된 몹의 장기 누적 정도.

## 13. 단계별 작업 분할

| 단계 | 내용 | 선행 조건 |
|---|---|---|
| C1 | #213 후속 수정(§11.4의 C1-1 ~ C1-8). Q117 배관(보스 기록, 소멸 작업, 종료 연출 이벤트, 복원 저널)을 테스트용 스텁 보스로 포함 | 이 PR 승인(권위 동기화) |
| C2 | 공유 Trial Key 발급과 §9.4 유실 복구. S1·S13·S14·S22 | C1 |
| C3 | `SupplyHandler`: 참가자 수만큼 고정 내용물 상자, 성공·실패 정리, 시간 초과 방치 | C1, 위치는 C4와 연계 |
| C4 | 월드젠 등록(`instance_id`, 실제 기하)과 **구조물 점유 마스크** | 구조물 수치(권위상 구현·밸런스 작업) |
| C5 | R7 Immortal Gladiator 본체: `BossHandler`, 장애물 파괴 돌진의 `journalBreak` 호출, 포털 이동 금지, `onRemoved` | C4의 구조물 점유 마스크(O-10은 Q118-A로 해결됨) |
| C6 | 실제 개인 보상 지급과 수령 기록 | 승인된 개인 보상 정의(O-7) |

## 14. 남은 미해결 사항과 위험

해결됨(권위로 확정되어 이 계약에 반영):

- 구 O-1 ~ O-6: Q111 ~ Q116과 R3 역할 동기화로 해결(이전 보고).
- 구 **O-8**(시간 초과 후 보스 처치 시 드롭): Q117로 해결. 보스는 소멸하고 전리품·XP·열쇠가 없다.
- 구 **O-9**(시간 초과 뒤 보스의 블록 파괴와 복원 시점): Q117로 해결. 보스는 소멸하므로 돌진이 없고, 복원은 시험 종료 때 Q91의 조건으로 한다.
- 구 **O-10**(복원 자리에 다른 블록이 있을 때): Q118-A로 해결. 덮어쓰지 않고, 빈 자리는 복원하고, 충돌을 기록하며, 청크 재로드와 재시작 뒤에도 이어진다.

남은 사항:

- **O-7**(C6만 차단): 승인된 개인 보상 품목이 아직 없다. 지급 대상과 시점이 정해져야 수령 기록 경로를 구현할 수 있다. 기록 후 지급 전 크래시의 처리도 그때 정한다.
- **O-13**(차단 아님, 확인 권장): Q118-A는 충돌 항목을 나중에 다시 평가하는지 말하지 않는다. 구현(PR #213)은 충돌 항목을 계속 다시 평가해, 플레이어가 자기 블록을 치우면 그 자리를 복원한다. 이 경우 오래 지난 뒤에도 자연 블록이 다시 나타날 수 있다. 대안은 충돌을 기록하고 항목을 종료하는 것이다. 어느 쪽이든 덮어쓰기는 없다.
- **O-11**(차단 아님, 확인 권장): 같은 틱 경합의 순서(§4.5). 1틱 일치라 발생 확률이 극히 낮고, 구현 순서의 결정일 뿐이다.
- **O-12**(차단 아님, 확인 권장): R1–R6 중 시간 초과일 때는 소멸할 보스가 없으므로 종료 연출을 하지 않는다는 해석(§10.1 (b)).

위 항목 중 구현을 막는 것은 O-7(C6)뿐이다.

위험:

- **R-1** 평화로움 난이도에서는 몬스터가 즉시 제거되어 재소환이 반복된다. 부재 60초나 Q116 65분에서 끝난다.
- **R-2** 공유 열쇠의 크래시 창: 플레이어 데이터와 월드 데이터가 원자적으로 저장되지 않아 극히 드물게 중복이 생길 수 있다(§9.4). 정상 종료·자동 저장에서는 발생하지 않는다.
- **R-3** 크리에이티브·관전자를 존재로 세지 않는다(바닐라 Trial Spawner 관례).
- **R-4** 플레이어가 남아 있는 동안 몹이 미로드 청크로 가면 라운드가 정체된다. Q116이 상한이 된다.
- **R-5** PR #211과 텍스트 병합 충돌이 예상된다.
- **R-6** GameTest 서버는 실시간 속도와 무관하게 틱을 돌린다. C1-7의 보류 플래그로 재시작 테스트의 시간 의존을 제거한다.
- **R-7** 해제된 시험의 일반 몹은 소유 태그를 단 채 월드에 남을 수 있다. `released_trials`가 지워지면 실패 시험 몹으로 오인되어 삭제되므로, 이 목록은 장부와 같은 보존 규칙(I8)을 따른다.
- **R-8** T13 처리 중 저장 불일치: 보스 제거는 청크에 저장됐는데 SavedData는 이전 상태일 수 있다. 시험이 BOSS 단계인데 보스가 없다. 이를 처치로 해석하지 않으며(I11), 보스를 다시 만들지는 C5 핸들러가 정한다.
- **R-9** 해제된 일반 몹은 지속성 플래그를 유지해 거리 디스폰이 없다. 시간 초과가 반복되면 아레나에 몹이 누적될 수 있다. Q116이 정리하지 않는다고 했고 그 변경은 미승인이라 그대로 둔다. 누적 정도는 라이브 확인 대상이다.
- **R-10** 종료 연출의 시각 효과가 크래시 뒤 한 번 더 나올 수 있다. 상태에는 영향이 없다.
- **R-11** 저널과 블록 변경이 원자적으로 저장되지 않는다. 블록이 부서진 채 저널이 저장되지 않는 크래시에서는 극히 드물게 복원이 누락된다. 저널을 먼저 쓰는 순서가 반대 방향을 막는다.
- **R-12** 복원 대상 청크가 오래 로드되지 않으면 부서진 지형이 그대로 남는다. 로드되는 즉시 복원된다.

## 15. 판정

- **C1–C4: IMPLEMENTATION READY.** Q117을 포함해 새 게임 규칙 없이 구현할 수 있다. PR #213의 후속 수정은 §11.4의 순서로 추가 설계 질문 없이 착수할 수 있다.
- **C5(R7 보스): IMPLEMENTATION READY.** 남은 설계 질문이 없다(O-10은 Q118-A로 해결). 단 C4의 구조물 점유 마스크가 있어야 보스가 부술 수 있는 블록을 가를 수 있다.
- **C6(개인 보상 지급): BLOCKED.** 승인된 개인 보상이 없다(O-7). 장부 스키마와 `tryClaim` API는 C1에서 준비된다.

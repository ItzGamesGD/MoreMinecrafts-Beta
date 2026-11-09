# ST-05 A02 — RESOLVE Q25-Q43 owner decision ledger (2026-11-01)

**Status: OWNER-APPROVED DESIGN ONLY. NOT IMPLEMENTED. TECHNICAL VALIDATION AND AUTHORITY RECONCILIATION PENDING.**

Repository: ItzGamesGD/MoreMinecrafts-Beta; Draft PR #217; Stage 0 decision continuation. Supplements `docs/st05/ST05_A02_FIX_Q19_AND_RESOLVE_Q01_Q24_OWNER_DECISIONS_2026-11-01.md` and earlier FIX Q01-Q18 ledgers. No previously approved decisions are silently superseded.

| ID | Option | Binding decision |
|---|---|---|
| Q25 | B | 고정 명부 전원 사망 즉시 도전 실패. 전투 중지, 동일 attempt ID 재개 불가; 실패 원인·라운드·보상 기록 보존. 이탈/접속 종료와 구분. |
| Q26 | B | 생존 명부 참가자가 경기장에 아무도 없지만 전원 사망은 아닌 경우 복귀 유예 및 전투 중단; 유예 만료 시 실패, 복귀 시 검증 후 재개. 65분 상한 유지. |
| Q27 | B | 복귀 유예 180초. 전체 65분과 유예 만료가 경합하면 먼저 만료되는 제한 우선. 전원 사망은 즉시 실패. |
| Q28 | B | 복귀 유예 중 라운드·잔존 몬스터·HP·진행 상태 보존하고 전투 중단; 검증된 복귀만 재개. 불확실 시 격리, 초기화/보상 금지. |
| Q29 | B | 복귀 유예 중 아레나 소유 공격 예고·스폰 경고·라운드·보급·내부 쿨다운 타이머 동결 및 잔여시간 재개. 65분·180초는 계속 경과. 바닐라/타 모드 타이머 동결은 미승인. |
| Q30 | B | 서버 재시작 시 실제 경과시간 기준으로 도전 시작·복귀 유예 만료 타임스탬프 검증. 다운타임도 기본 65분·기존 유예에 포함; 시간 불확실 시 격리. 시계 드리프트 대응 미검증. |
| Q31 | B | 정상 전투 중 서버 종료 후 저장 상태의 일관성 검증에 성공하면 전투 중단 상태로 복구하여 기존 참가자 재접속 대기. 불확실 시 격리, 자동 라운드 리셋/보상 금지. 65분 상한 유지. |
| Q32 | B | 정상 전투 중 서버 장애 발생 후 검증 복구 시점부터 신규 180초 재접속 유예. 종료 전 이미 유예 중이었다면 기존 만료 시각 유지; 65분 전체 제한 연장 없음. |
| Q33 | B | 고정 명부의 생존 참가자 1명 이상 경기장 복귀 시 검증 후 전투 재개. 미복귀 명부 보존; 완료 보상 자격은 Q24-A. 라운드·몬스터·보상 초기화 금지. |
| Q34 | B | 각 라운드 시작 시 생존·현장 참가자 기준 보정 계수 확정, 라운드 중 인원 변동으로 변경 없음; 다음 라운드 재산정. 기존 M(n)=1+0.6(n-1) 유지. Q35 하한 적용. |
| Q35 | B | 라운드 보정 인원은 현재 생존·현장 인원과 해당 도전의 역대 최대 동시 활성 생존 인원 중 큰 값. 최대 기록은 감소하지 않음. 정상 이탈 시 난이도 완화 제한은 명시적 트레이드오프. |
| Q36 | B | 최대 활성 생존 인원은 고정 명부·생존·경기장 내부·서버가 검증한 전투 참여 가능 상태를 동일 판정 시점에 충족한 인원으로 계산. 단순 접속/로딩 제외, 실제 공격 기여도 불필요. |
| Q37 | B | 도전 ID와 최대 활성 생존 인원 영속 저장; 재시작 시 명부·라운드·도전 상태와 일관성 검증. 정상 복구 시 감소 불가; 손상/모순 시 격리. 원자성 미증명. |
| Q38 | B | 라운드 ID·보정 인원·계수·몬스터 생성 계획을 선영속 기록, 생성 결과를 식별·대조. 재시작 시 실제 월드와 기록이 일치할 때만 복구, 불명 시 격리. 다중 저장 원자성 가정 금지. |
| Q39 | B | 몬스터·처치·보충 이벤트 고유 ID로 기록, 보충 계획 선저장 후 생성 결과 대조. 중복 처리 방지; 언로드와 사망 구분, 불명 시 격리. 위협 예산·동시 12마리 상한 유지. |
| Q40 | B | 활성 전투 영역 청크 로딩 관리; 엔티티 존재 확인 불가 시 처치·보충·라운드 종료 판정 중지. 재검증 후 복구, 불가 시 격리. 로딩만으로 저장 일관성 보장 안 됨. |
| Q41 | B | 검증된 서버 내부 장애로 전투 불가능 시 운영상 격리. 실제 65분 경과시간은 기록하되 격리 중 시간 만료만으로 실패 확정 금지; 자동 연장/재개 금지. 플레이어 이탈은 별개. |
| Q42 | B | 검증된 서버 내부 전투 불가능 구간만 유효 경과시간에서 차감: T_effective=T_elapsed-T_verified_outage. 구간 중복/불명 시 자동 보정 없이 격리. 일반 렉/이탈/복귀 유예 자동 포함 금지. |
| Q43 | B | 정상 전투 중 서버 장애로 완전 종료된 사실 및 종료·복구 구간을 신뢰성 있게 검증한 경우에만 Q42의 장애 시간으로 보정. 정상 종료·의도적 재시작은 자동 보정 제외. 원인/시간 불명 시 격리. 실제 경과시간 기록 유지. |

## Explicit precedence and unresolved verification

1. **Q34/Q35:** Recompute at each round boundary, but apply monotonic maximum-verified-active-player floor; do not claim difficulty necessarily decreases when players leave. Formula application target (mob HP vs threat budget vs count) remains subject to existing authoritative design and reconciliation.
2. **Q29/Q30/Q32:** Arena-owned combat timers freeze during approved pause. Wall-clock 65-minute cap and an already-running 180-second return grace normally keep running through restart. Only an outage from normal active combat, after verified recovery, starts a *new* 180-second reconnection grace; never reset an already-running grace.
3. **Q41-Q43 exception to Q30:** Preserve raw wall-clock elapsed time and verified outage evidence separately. A proven internal server outage, including a proven abnormal full shutdown, may reduce *effective* elapsed time; this is an explicit exception to the earlier unadjusted 65-minute calculation, not a rewrite of its timestamp evidence. Uncertain evidence -> quarantine, not inferred compensation or automatic failure. Ordinary admin restarts and player disconnects are not automatically outages. Existing 180-second grace is not double-credited.
4. **Q38-Q40:** Plan-before-effect and unique IDs are design requirements, **not proof of exactly-once persistence**. Verify actual Minecraft 26.3 entity lifecycle, chunk unload, event ordering, and crash windows; quarantine ambiguous state rather than spawning twice or erasing records.
5. **Reward and arena:** Q21-Q24 entitlements and prior chest/key policy remain unchanged. No automatic regrant, arena reroll, reset, or change of approved 7-round threat budgets, 12-mob cap, 65-minute cap, or 180-second grace except explicit verified outage accounting above.
6. **Outstanding design:** precise server time/clock rollback handling; abnormal termination evidence and outage boundary detection; no double-counting overlapping outages; idempotent event/monster identity and save consistency; operational quarantine release and resumption; remaining retry, key/chest, payout and authority reconciliation questions. Q44 is not yet approved.

**No implementation, no tests, no merge, no checkpoint movement, no branch deletion authorized by this ledger.**

## Subsequent owner approval: RESOLVE Q44 (B)

**Q44 B — verified outage boundary only.** Do not treat the last normal save timestamp as the start of a server outage. Cross-check termination evidence, durable runtime records, and recovery timestamps. Compensate only intervals affirmatively established as outage; do not automatically credit ambiguous normal-running intervals. If the uncertainty changes whether the attempt has expired, retain operational quarantine. No fabricated timestamps or arbitrary compensation.

## Owner-directed decision workflow change (2026-11-01)

- Stop serial owner questions for ordinary technical details, implementation choices, deterministic IDs, persistence schema, routine exception handling, and test mechanics that can be derived from already approved policy. Engineering may choose and document such details, with verification and explicit uncertainty.
- Escalate only **material design conflicts** or choices changing approved gameplay, worldgen frequency/geometry, difficulty, rewards, eligibility, resource constraints, or policy, and cases where approved constraints cannot be simultaneously satisfied. Do not silently override existing owner approvals.
- Unverified feasibility must be tested or recorded as a blocker, not transformed into an invented owner choice.
- **Q45 has not been owner-approved.** Clock drift/rollback and cross-restart time integrity remain engineering design/verification work; do not label Q45 B as approved.
- Proceed to the next substantive stage through readiness audit and Stage 0-C validation planning; Stage 1 remains blocked by mandatory failed/unverified checks. This note does not authorize game-code implementation, test execution, merge, or checkpoint changes on its own.

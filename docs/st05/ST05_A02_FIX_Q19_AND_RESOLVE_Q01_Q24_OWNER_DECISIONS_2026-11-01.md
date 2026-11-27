# ST-05 A02 — FIX Q19 and RESOLVE Q01-Q24 owner decision ledger (2026-11-01)

**Status: OWNER-APPROVED DESIGN ONLY; NOT IMPLEMENTED; TECHNICAL VALIDATION AND AUTHORITY RECONCILIATION PENDING.**

Scope: Stage 0 A02 follow-up to Draft PR #217. This document supplements existing FIX ledgers and is NOT a silent rewrite of authoritative WG decisions. Existing FIX Q01-Q18 are preserved in:
- `docs/st05/ST05_A02_FIX_OWNER_DECISIONS_Q01_Q12_2026-10-31.md`
- `docs/st05/ST05_A02_FIX_OWNER_DECISIONS_Q13_Q18_2026-10-31.md`

## FIX-Q19 (approved B)
Stage 0-C verification matrix covers same-seed/restart/generation-order/parallel determinism, cross-window chamber deduplication, grid boundaries, multiway 256-block spacing, bounded budgets/dependency propagation, 75% roll denominator and state/count invariants, fluids/paths, construction atomicity/restart, and real Minecraft 26.3 behavior. Any failed or unverified mandatory item blocks Stage 1. **Approval does not authorize implementation or running tests.**

## RESOLVE decisions (all owner approved)

| ID | Option | Binding decision |
|---|---|---|
| Q01 | B | 동일 자연 챔버임이 증명된 복수 앵커만 결정적 우선순위로 중복 제거; 동일성 불확실 시 병합·독립 확정 금지, 보류. |
| Q02 | B | 고정 예산 안에서 미해결 클러스터와 무관함이 증명된 후보만 확정; 영향 가능 후보 BLOCKED_BY_UNRESOLVED. |
| Q03 | B | 동일 seed/설계버전/예산/계획시점 입력 아래 미판정 클러스터의 결정적 재평가 허용. 확정 결과 불변, 미해결이면 재보류. |
| Q04 | B | 월드 최초 ST-05 알고리즘 버전 고정; 향후 버전은 새 월드에만 적용. 구버전 알고리즘/자료 부재 시 보류. |
| Q05 | B | 버전 정보가 없으면 ST-05 최초 적용이 증명된 월드만 현 버전 등록; 과거 이력 또는 이력 불명은 보류. |
| Q06 | B | 결정적 계획(적합성·간격·굴림) 영속 기록과 실제 시공 완료 검증 분리. 재시작 시 계획·실제 블록 대조; 다중 청크 원자성 가정 금지. |
| Q07 | B | 중단된 시공은 계획과 실제 블록 대조로 안전이 증명될 때만 재개; 그 외 격리/보류, 근거 없는 삭제·덮어쓰기·전리품 지급 금지. |
| Q08 | B | 최종 선택된 아레나의 256블록 XZ 간격 예약은 시공 실패·격리에도 유지; 탈락 후보 재선택 금지. 예약은 PLACED 수와 구별. |
| Q09 | B | 실제 아레나 중심을 간격 검사 전에 결정적으로 확정하고 그 중심으로 거리 측정. 간격 이후 재배치 금지. 순서: 클러스터 식별→적합성/중심→간격→75% 굴림→사전검증→시공 완료 검증. |
| Q10 | B | 아레나 중심은 소유 256×256 구역 밖 최대 128블록 확장 범위까지 허용; 원래 앵커 구역 소유권 유지. 중복/소유 불명 시 보류. |
| Q11 | B | 클러스터 탐색 128블록 마진과 별도로 본체·통로 전체를 포괄하는 유한 시공 안전성 검증 범위 산정; 검증 실패·예산 초과는 보류/실패. 중심·소유권 변경 금지. |
| Q12 | B | 256블록 중심 간격에 더해 본체·연결 통로·필수 안전 영역의 실제 점유 충돌 금지. 굴림 전 결정적 탐욕 선택의 배제 조건으로 확장; 불명 시 보류. |
| Q13 | B | 최대 시공 영역 확장에 따라 별도 유한 충돌 후보 탐색 범위 산정; 모든 직접 충돌 가능 후보 확인 불가 시 보류. 128블록 클러스터 탐색과 256블록 간격 유지. |
| Q14 | B | 결정적 탐욕 선택의 간접 연쇄 의존성이 유한 예산 안에 해결되고 결과 불변임이 증명될 때만 확정; 나머지 및 영향 후보 보류. 보류 비율 측정 필요. |
| Q15 | B | 후보별 결정적 총연산 예산과 단계별 상한; 시간/스레드 순서 아닌 결정적 연산 단위 사용. 초과 시 보류. 실제 수치 미정, CPU/메모리 한도 별도. |
| Q16 | B | 검증된 결정적 체크포인트에서 분석 재개; 동일 버전 누적 총연산 예산 불변, 재시작 횟수로 예산 증가 금지. 소진 시 추가 분석 불가. |
| Q17 | B | 버전·후보 ID·입력 식별·누적 연산·진행·검증 결과의 일관성 확인된 체크포인트만 복구. 손상/불일치 시 보류; 저장 원자성은 미증명. |
| Q18 | B | 최종 확정 근거와 분석 체크포인트를 별도 보존. 확정 근거가 독립 검증되면 유지; 불명 시 운영 격리·추가 시공/보상 중단, 선택/예약 불변, 자동 삭제/대체 금지. |
| Q19 | B | 배치 판정(선택·예약·굴림)과 운영 상태(시공·검증·격리·운영 가능) 분리. 상태 전이/집계 불변식 재정의, 75% 분모 소급 변경 금지. |
| Q20 | B | 격리 해제는 배치 확정 근거·실제 구조물/통로·시공 완료·운영 안전을 독립 검증해 모두 통과할 때만. 운영 상태만 변경; 불명 시 격리 유지. |
| Q21 | B | 승인된 보상 권리마다 고유 지급 ID와 영속 지급 상태(준비/처리 중/완료). 재시작 후 지급 증거 대조; 불명 시 자동 재지급 금지·보류, 완료 기록 보존. 정확히 한 번 지급 원자성 미증명. |
| Q22 | B | 아레나 생성/도전 시작 아닌 승인된 보상 획득 조건 충족 시 도전 기록·수령 주체 기준 지급 권리/ID 확정. 재시작/복구로 중복 생성 금지; 보상 구성 불변. |
| Q23 | B | 도전 시작 시 참가자 명부 고정; 승인된 개인별 자격을 만족한 참가자에게만 권리. 중간 합류 자동 추가 금지. ID는 도전 ID·플레이어 UUID·권리 식별자 기준. |
| Q24 | A | 도전 완료 시점에 생존하며 경기장 내부에 있는 고정 명부 참가자만 해당 완료 보상 자격을 얻음. 사망/이탈/오프라인 참가자는 해당 완료 보상 대상 제외. 도전 자체의 성공·실패와 구분. |

## Explicit constraints and reconciliation requirements
1. Existing FIX Q01-Q18 remain approved; this ledger adds clarifications. FIX Q16 deterministic greedy selection now includes direct physical footprint conflicts as well as 256-block XZ center spacing (RESOLVE Q12), subject to unresolved-chain blocking (Q14).
2. The 128-block search margin is for bounded chamber observation and center allowance; it does NOT prove complete 256-block spacing or physical footprint dependency coverage (Q10-Q14).
3. FIX Q18's proposed single terminal-state enumeration is NOT a complete final state machine after RESOLVE Q18-Q20. Preserve distinct placement-decision and operational-status axes, and explicitly reconcile all counters/terminal states before Stage 1.
4. Roll denominator is the eligible suitable spacing survivors, before 75% roll; construction failure/quarantine does not retrospectively alter denominator or reactivate losers.
5. The Q24 A choice applies to **completion reward eligibility** of original roster members; it does not itself decide challenge failure, all reward categories, or whether a reconnecting player is considered present at the exact completion event.
6. Owner decisions are policy, not feasibility proof. Stage 0-C must verify bounded dependency completion, exact resource budgets, checkpoint persistence/recovery, cross-chunk construction safety, proof records, and idempotent payout. Do not claim atomic exactly-once reward grants without evidence.
7. Explicit authority amendments required for old worldgen WG Q11 (global cave vs chamber cluster), Q10/Q41 (75% denominator), Q43 (bounded budget), Q09/Q26/Q38 (fluid/path), and any related ownership, state, reward, multiplayer or restart rules. Do not silently overwrite old approved requirements.

## Remaining owner decisions / unresolved design questions
- Challenge lifecycle: death, all-party wipe, everyone leaves, disconnect/reconnect, timeout, restart, abandon, and when challenge success/failure is finalized; how these interact with Q24 A at the completion instant.
- Roster membership and presence edge cases: temporary unload, disconnect during completion tick, spectators, dead-but-respawned players, simultaneous exit/completion, late joiners; event ordering must be deterministic.
- Retry policy: whether/when same arena can be retried, new attempt ID, cooldown, whether prior completion and reward entitlement prevent farming; never reset completed payment IDs.
- Reward categories: completion vs chest/key/round rewards; per-player rights vs preapproved identical-NBT chest multiplication, concurrent chest access, key ownership, vault reopening, and post-quarantine payout eligibility.
- Persisted state schema: exact state transitions, proof-record lifetime, terminal accounting, quarantine and manual recovery protocol.
- Budget values and finite bound proof: stage operation counters, maximum footprint/connection corridor, complete candidate discovery, dependency propagation and acceptable unresolved rate.
- Stage 0-C technical verification and authority-document reconciliation; these are blockers, not new owner approvals to be inferred.

**No code changes, no Stage 1 implementation, no tests run by this ledger, no merge, no checkpoint movement, no branch deletion authorized.**

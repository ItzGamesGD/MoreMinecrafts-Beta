# ST-05 A02 FIX — Owner decisions Q13-Q18 (2026-10-31)

Status: OWNER-APPROVED DESIGN; NOT IMPLEMENTED; VALIDATION PENDING.
Prior approved ledger: docs/st05/ST05_A02_FIX_OWNER_DECISIONS_Q01_Q12_2026-10-31.md. This supplements, does not replace, the authoritative worldgen specification. Reconcile conflicts explicitly before implementation. No Stage 1 authorization.

| ID | Choice | Approved rule |
|---|---|---|
| Q13 | B | Fixed ownership grid 256x256 blocks (16x16 chunks), independent of 256-block XZ arena spacing. |
| Q14 | B | XZ search margin 128 blocks outside each fixed grid region. This bounds local observation, NOT global cave connectivity or deduplication. |
| Q15 | B | Deterministic canonical anchor candidates from world seed + fixed grid; bounded local chamber suitability checks; overlapping candidates subject to deterministic ownership resolution. Same anchor candidate has same ID. Must preserve chamber-based B-prime semantics rather than silently switch to generic random candidate sites. Cross-window same-natural-chamber duplicate detection remains unproven. |
| Q16 | B | Deterministic greedy priority selection for spacing: process by seed + cluster ID priority; reject only if within 256-block XZ Euclidean distance of an ALREADY ACCEPTED candidate. In A-B-C chain where A-C do not conflict, A and C can survive. Unresolved dependencies deferred under Q08/Q17. |
| Q17 | B | Deterministic dependency propagation for unresolved cluster effects on ownership/spacing; mark potentially affected candidates BLOCKED_BY_UNRESOLVED; unaffected candidates proceed. Finite operation bound/termination not yet established, so failure to prove it remains blocker, never silently finalize. |
| Q18 | B | Mutually exclusive terminal states with stepwise count invariants and separate roll denominator, roll wins and placement counts. Proposed labels: UNSUITABLE, BUDGET_EXHAUSTED, BLOCKED_BY_UNRESOLVED, SPACING_EXCLUDED, ROLL_FAILED, PREFLIGHT_FAILED, PLACED. These are design labels, not code or proof that all cases are covered. |

## Remaining Q19 and technical blockers
- Q19 Stage 0-C test matrix, exit criteria and failure disposition still to be approved.
- Prove or disprove bounded deterministic ownership, same-chamber deduplication, cross-region anchor stability, and multiway/long-chain unresolved propagation.
- Q17 requires explicit finite operation budget and deterministic fail-closed semantics; cannot assume finite global connected-component enumeration.
- Test 75% denominator conservation and per-state mutually exclusive accounting (including no-roll outcomes and post-roll construction failures).
- Verify full preflight vs actual worldgen feature sequencing, no partial structure across chunk boundaries, restart/reload, fluid seals, and deterministic results under generation order.
- Explicitly reconcile Q02/Q06 changes to existing WG Q11 and Q10/Q41 and other relevant authority decisions.

No implementation, merge, checkpoint update, or Stage 1 start is authorized.

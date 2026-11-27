# ST-05 Stage 1 — lifecycle authority reconciliation (2026-11-02)

**Status: documentation-only reconciliation of existing owner approvals. No new gameplay approval, implementation acceptance, merge, or checkpoint authorization.**

## Controlling order for the specific conflicts

1. **Participation:** `docs/st05/ST05_STAGE1_ROUND_BOUNDARY_JOIN_AMENDMENT_2026-11-02.md` supersedes the immutable *attempt-wide* roster interpretation in RESOLVE Q23 and dependent clauses. Roster snapshots are fixed **per round**; valid pending entrants join at the next round boundary, never retroactively.
2. **Return grace and pause:** `docs/st05/ST05_A02_RESOLVE_Q25_Q43_OWNER_DECISIONS_2026-11-01.md` Q25–Q33 supersedes the **60-second** absence grace and live-mob progression assumptions in the older `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md` (Q60-era text). Grace is **180 seconds**. During approved grace, freeze arena-owned combat/progression, spawn-warning, supply and internal cooldown timers and preserve combat state. Do not claim to freeze vanilla or unrelated mods. A full wipe of current admitted participants fails immediately.
3. **Wall-clock trial limit and restarts:** Q29–Q32 and Q41–Q44 of the November 1 ledger supersede the older Q112-A rule that **all** timers, including the trial limit, exclude offline time. The **65-minute attempt cap** and an already-running **180-second grace** normally continue by wall clock across shutdown/restart; ordinary planned restarts are **not** automatically credited as outages. On verified recovery after shutdown during active combat, a **new** 180-second reconnection grace begins, without extending the 65-minute cap; a preexisting grace keeps its original expiry. Uncertain clocks/evidence -> quarantine. Verified internal outage compensation is allowed only under Q41–Q44 with independently supported interval evidence; do not invent outage periods.
4. **Cooldown remains separate:** The approved **78,000-tick cooldown** retains Q112-A's **offline time excluded** behavior, as a distinct post-attempt cooldown. Do not confuse it with the 65-minute wall-clock attempt cap. Other older Q112-A timer language remains subject to Q29's pause semantics; do not globally reinterpret every timer as wall-clock.
5. **Rewards:** Q113–Q115 personal eligibility and shared-key semantics remain in force, subject to the later round-boundary admission amendment. No retroactive eligibility, duplicate key, or duplicate personal payout.
6. **Unchanged:** 7 rounds, existing budgets, Q40 chunk loading obligation, Q91/Q118 terrain preservation, and deferred boss/final reward work are not approved or implemented by this reconciliation.

## Historical text and implementation status

The older `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md` remains an important historical record. Its conflicting 60-second grace, attempt-wide fixed roster, and Q112-A blanket offline-time exclusion are **SUPERSEDED only on the dimensions listed above**. Do not silently delete original decisions.

Draft PR #219 reports Stage 1-A controller, roster and persistence with JUnit/GameTest evidence. **This documentation change does not independently validate those tests or mark live-client acceptance.** Known remaining implementation includes R7 Immortal Gladiator hook fulfillment, Q40 chunk tickets, Q91/Q118 terrain restoration, final rewards and real-client acceptance.

When implementing follow-up stages, use this reconciliation and the linked November 1–2 owner ledgers before applying the older authority file's conflicting clauses.

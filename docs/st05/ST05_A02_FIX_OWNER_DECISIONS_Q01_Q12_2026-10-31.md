# ST-05 A02 FIX — Owner decisions Q01-Q12 (2026-10-31)

Status: **OWNER-APPROVED DESIGN DECISIONS; NOT IMPLEMENTED; VALIDATION PENDING**.
Scope: Stage 0-B follow-up to Draft PR #217. This is a decision ledger, not a modification of implementation or automatic supersession of existing authoritative Q text. Conflicts with WG-Q01-Q45 / FIX-01 must be explicitly reconciled before implementation. Stage 1 remains blocked.

| ID | Approved option | Exact decision / constraints |
|---|---|---|
| Q01 | B | Fixed world-coordinate regions, anchor ownership, bounded-radius chamber clusters (B-prime). Exact cluster identity, radius and margins unresolved. |
| Q02 | B | Maximum one arena per cluster. A single globally connected natural cave may have multiple arenas in different clusters subject to spacing. Explicit Q11 definition change required. |
| Q03 | B | Minimum arena-center XZ Euclidean separation **256 blocks**. Measured tuning not yet established. |
| Q04 | B | Seed + cluster-ID deterministic priority resolves spacing conflicts; independent of generation order/thread scheduling. Multiway conflict algorithm unresolved. |
| Q05 | A | Check cluster suitability and resolve spacing conflicts BEFORE one 75% roll on survivors. Spacing-excluded clusters receive no roll. Preserve existing fit-before-roll ordering. |
| Q06 | A | The 75% roll denominator is suitable clusters surviving spacing exclusion; separately retain initial suitable cluster count and spacing exclusions. Explicit Q10/Q41 denominator amendment required. |
| Q07 | B | Search budget exhaustion -> BUDGET_EXHAUSTED, neither suitable nor unsuitable, no roll and no placement. |
| Q08 | B | Candidates whose ownership, suitability or spacing outcome might be affected by unresolved clusters -> BLOCKED_BY_UNRESOLVED. Only provably unaffected candidates proceed. Deterministic bounded influence algorithm pending. |
| Q09 | B | Conservative plan-time safety check plus construction-time enforcement of fluid/path safety. Seal small fluids under existing Q26/Q38; reject large lava bodies or unsafe seals. Construction order/feature interactions unverified. |
| Q10 | B | Full preflight before any placement; reject entire arena on hard failure, no partial construction permitted. True atomicity/persistence across chunks and restarts NOT YET VERIFIED; Stage 0 A03 tests outstanding. |
| Q11 | B | Keep original roll denominator after post-roll construction/preflight failure; separately record roll wins, construction failures and final placements. No post-hoc denominator adjustment. |
| Q12 | A | Do not recompute spacing or reactivate spacing-excluded clusters following construction failure. Keep prior roll population fixed. |

## Unresolved / blockers
- Q13+: fixed region size, chamber radius/margins, anchor normalization and deterministic bounded cluster identity; cross-border cases.
- Multiway spacing resolution, possible indirect conflict chains and bounded unresolved propagation.
- Exact deterministic operation budget and exhaustion behavior within ownership resolution.
- Construction-time fluid/path safety versus worldgen stage ordering; no partial structures, cross-chunk persistence, restart tests.
- Explicit authority reconciliation for Q11, Q10/Q41, Q43, conditional Q09/Q26/Q38; no silent rewrite.
- Stage 0-B finding: namespace minecraft: for sulfur/cinnabar does not alone establish the generating mod/vanilla feature. PR #217 text correction should be handled separately with evidence.

**No implementation authorized. No merge, checkpoint change or Stage 1 authorized.**

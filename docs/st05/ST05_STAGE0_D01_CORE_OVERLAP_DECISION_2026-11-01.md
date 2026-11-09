# ST-05 Stage 0-D01 — Owner-approved chamber identity direction and implementation contract

Status: **D01 OPTION B APPROVED; DETAILED ALGORITHM PROVISIONAL; IMPLEMENTATION AND LIVE VALIDATION PENDING**.
Reference: Draft PR #217, baseline `b7d4eb88467a2630e3ff7f381af55140f77ccadb`.

## Owner decision (binding)
Two anchor observations identify the same natural B-prime chamber **only when evidence establishes that they observe the same broad core space**. Mere air connectivity or passage connectivity is insufficient. Ambiguous cross-boundary identity must be deferred, never silently merged or independently finalized.

## Existing invariants retained
- 256x256 fixed ownership grid; 128-block observation margin; anchor-based ownership.
- Natural-chamber semantics (not merely arbitrary anchor influence zones).
- Width 32, height 18, path connectivity 64 are suitability conditions, **not automatically an identity threshold**.
- Deterministic ID and cross-region duplicate handling; no double finalization of a proven same chamber.
- Candidate ambiguity -> BLOCKED_BY_UNRESOLVED with bounded deterministic influence analysis and finite operation budgets.
- Accepted centers at least 256 blocks apart in XZ, plus physical footprint conflict exclusion; suitability and spacing before one 75% roll.
- Existing selected reservations, counts, and finalized results remain unchanged; no rerolls.

## Engineering candidate, not owner-approved gameplay thresholds
1. Read an identical, versioned terrain snapshot for both observations. Distinguish worldgen plan-time terrain from final terrain; if snapshots are incomparable, defer.
2. Extract broad traversable *core* regions using a deterministic geometric predicate that excludes narrow connectors. Evaluate multiple candidate predicates/thresholds as experimental parameters, **not silently authoritative rules**.
3. For each core, produce stable canonical evidence (world-space voxel/core mask, deterministic component signature, bounded observation envelope, provenance and snapshot hash). Anchor IDs alone cannot prove same chamber.
4. Match two observations only if there is positive evidence they refer to the same continuous broad core, with an identity witness reproducible from both views. A nonempty air-mask intersection, connected narrow tunnel, or overlapping observation windows alone is not sufficient.
5. If boundary truncation or incomplete candidate discovery prevents proof of identity or distinctness, defer affected candidates. An apparently disjoint partial view does not prove distinct natural chambers.
6. Resolve proven duplicates with the approved seed+cluster deterministic priority; propagate unresolved dependencies without changing already finalized reservations or denominator.
7. Persist candidate evidence, algorithm version, cumulative deterministic budget, finalization proof, and operational status separately. Verify restart and damaged checkpoint cases; do not assume multi-chunk atomicity.

## Mandatory adversarial fixtures
- One broad chamber seen from two adjacent 256 regions: exactly one canonical ownership.
- Two separate broad chambers linked by a narrow tunnel: must not collapse solely due to connectivity.
- One broad chamber clipped by observation boundaries: no false distinct finalization.
- Two nearby distinct cores whose observation windows overlap: no false merge.
- Three-way overlap and long dependency chains: stable order-independent outcome, finite budget and fail-closed deferral.
- Same seed across forward/reverse/shuffle/parallel/restart with identical plan-time snapshots.
- Minecraft 26.3 live world evidence, not synthetic-only pass.
- Compare different core extraction parameters: quantify effect on chamber count, eligible candidates, deferred fraction, 75% roll denominator and performance.

## Decision gate
Because core extraction geometry and matching tolerance may materially change natural-chamber identity and spawn frequency, **do not promote a provisional threshold to approved production rules** without evidence and an explicit owner decision if alternatives yield materially different results. Engineer routine IDs, caching, serialization, tests, exception handling independently.

## Next execution
Implement a Stage 0-D probe in the existing PR branch; write a failing cross-boundary identity fixture first, then implement candidate extraction, evidence-based matching, fail-closed dependency handling, persistence, and targeted 26.3 tests. Iterate fix -> retest. Report PASS/FAIL/UNKNOWN and measured counterexamples in PR #217. No Stage 1 gameplay implementation, merge, checkpoint movement, or branch deletion.

This document records owner approval of **direction B**, not proof that the candidate algorithm is correct or complete.

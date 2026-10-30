# ST-05 WG-Q01–Q45 — Preimplementation design audit (2026-10-30)

**Status: DESIGN AUDIT / IMPLEMENTATION ON HOLD.** This document records a document-level audit, not a Minecraft 26.3 runtime test, successful generator implementation, or approval to merge. The owner requests record -> correct -> brief reaudit -> delegate final implementation, with design changes subject to the existing approval boundary.

## Sources and authority
- [WG-Q01–Q45 owner decisions](ST05_WORLDGEN_OWNER_DECISIONS_Q01_Q18_2026-10-30.md) on this branch; newer overlay for ST-05 worldgen and architecture.
- `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md` (Q1–Q118) for combat, lifecycle, rewards and persistence. Its older stands/huge-cave geometry is superseded for worldgen only.
- `docs/STRUCTURE_AUTHORITY_INDEX.md` must route to the overlay explicitly.
- WG-Q17 adjacent Vault placement superseded by WG-Q19: exact-center Ominous Vault directly beneath activation crystal.

## Findings and required disposition

| ID | Severity | Finding | Gate / resolution |
| --- | --- | --- | --- |
| A01 | BLOCKER (specification) | WG-Q10 describes 75% after fit checks; WG-Q31 and WG-Q41 describe one roll before bounded candidate fit search. These are not automatically equivalent. | Owner must choose the probability denominator and roll/fit precedence. Do not silently reinterpret 75%. |
| A02 | BLOCKER (feasibility) | Continuous vanilla caves lack an established deterministic unique connected-cavern identity. Spacing and one-roll-per-cave also interact. | Prove deterministic cavern segmentation, order independence, min separation conflict resolution and duplicate prevention with seed/restart tests. |
| A03 | BLOCKER (feasibility) | WG-Q28 full deterministic placement plan before chunk writes is not yet demonstrated on Minecraft 26.3 APIs. | Prototype in isolation and prove cross-chunk ordering, reload, restart, and no partial placements before implementation acceptance. |
| A04 | MAJOR | WG-Q27 other-structure occupied mask availability is not guaranteed. | Use known protected occupancy/clearance, conservative reject on unknown overlap; verify modded structures. |
| A05 | MAJOR | 68 exterior diameter, floor radius 29, wall 12 + dome 11, 7x8 arches, 6 Vaults radius 27, spawn annulus 12–24 require exact voxel layout/clearance tests. | Derive block-level plans and collision masks for all 50/25/25 variants; do not infer acceptance from arithmetic alone. |
| A06 | MAJOR | WG-Q35–Q39 scoring weights exist, but fixed-reference 0–100 measurement functions do not. | Define bounded, monotonic, reproducible score transforms before ranking. |
| A07 | MAJOR (documentation) | Older Q1–Q118 structural description mentions stands and enormous cave, contrary to newer WG overlay. | Update routing index with precedence; do not rewrite combat authority or silently reintroduce stands. |
| A08 | MAJOR (statistics) | 30 seeds and configured 75% are not evidence of realized generation rate. | Log eligible cavern count, roll count, winning rolls, candidate fit, spacing exclusions, final placements and sample uncertainty. |

## Noncontroversial corrections authorized without changing owner constraints
1. State explicitly in the structure authority index that WG-Q01–Q45 supersedes older worldgen/architecture text, **not** combat/reward/lifecycle/persistence.
2. Keep WG-Q19 over WG-Q17 and treat all candidate scoring as subordinate to mandatory safety gates.
3. Record A01 and A02 as **unresolved** rather than inventing a 75% semantics or relaxing min separation.
4. Q43's deterministic work budget must not use elapsed-time aborts to select geometry.

## Brief reaudit / acceptance gates
- Design record: 45 unique consecutive decision IDs; latest document and index point to same overlay.
- Specification: A01 requires owner selection; A02/A03 need isolated proof, not just prose.
- Architecture: confirm no stands, 68 exterior diameter, 23 total height, 7x8 arches, exact-center crystal/Vault, six radius-27 Vaults, spawn annulus radius 12–24.
- Functional: at least one valid <=32-block cave path with >=5x5 internal clearance; collision/fluid/terrain preservation hard gates.
- Sampling: 30 initial seeds, expand based on eligible cavern count; BLOCKER prevents final approval.
- **Implementation remains NOT AUTHORIZED until owner approves resolution of A01 and a separate explicit implementation handoff is made.** This audit does not claim the other technical blockers are solved.

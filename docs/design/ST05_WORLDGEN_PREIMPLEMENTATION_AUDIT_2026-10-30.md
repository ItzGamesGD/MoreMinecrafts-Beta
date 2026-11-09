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
| A01 | **RESOLVED — FIX-01 A** | Owner chose deterministic bounded fit checks before a single 75% roll per fit-eligible cavern. WG-Q10/Q31/Q41 amended accordingly. | Verify consistency and denominator in implementation; cross-cavern spacing conflict remains A02. |
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
3. A01 resolved by explicit owner FIX-01 A. Keep A02 and A03 unresolved; never relax min separation or invent a deterministic cavern identity.
4. Q43's deterministic work budget must not use elapsed-time aborts to select geometry.

## Brief reaudit / acceptance gates
- Design record: 45 unique consecutive decision IDs; latest document and index point to same overlay.
- Specification: A01 owner-approved FIX-01 A recorded; A02/A03 need isolated proof, not just prose.
- Architecture: confirm no stands, 68 exterior diameter, 23 total height, 7x8 arches, exact-center crystal/Vault, six radius-27 Vaults, spawn annulus radius 12–24.
- Functional: at least one valid <=32-block cave path with >=5x5 internal clearance; collision/fluid/terrain preservation hard gates.
- Sampling: 30 initial seeds, expand based on eligible cavern count; BLOCKER prevents final approval.
- **Full implementation and integration remain NOT AUTHORIZED by this audit.** The owner requested a staged delegation instruction; feasibility probes for A02/A03 must pass before their dependent implementation phases. No technical blocker is deemed resolved by documentation alone.

## Short post-FIX-01 re-audit (document-level)

- **PASS**: owner selected FIX-01 A; Q10, Q31 and Q41 now consistently put bounded fit before the sole 75% roll. No additional candidate count or new roll.
- **PASS**: structure authority index routes newer WG architecture/worldgen over older stands/huge-cave description, preserving combat/rewards/persistence.
- **PENDING BLOCKER A02**: connected cavern identity, min separation precedence and order-independent roll/placement still need an actual reproducible probe.
- **PENDING BLOCKER A03**: cross-chunk full-plan atomicity and restart consistency need a Minecraft 26.3 technical probe.
- **PENDING MAJOR A04–A06/A08**: protected occupancy, voxel clearances, normalized scoring and statistically meaningful sampling are not live-verified.
- **Disposition: CONDITIONAL GO FOR ISOLATED FEASIBILITY PROBES ONLY. NO GO for integrated generator, merge, checkpoint or final live acceptance.**

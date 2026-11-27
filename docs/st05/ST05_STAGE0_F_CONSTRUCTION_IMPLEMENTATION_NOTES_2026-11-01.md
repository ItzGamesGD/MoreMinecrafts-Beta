# ST-05 Stage 0-F — construction implementation notes (engineering interpretations, no new gameplay rules)

Implements the approved Stage 0-F contract (`ST05_STAGE0_F_OWNER_DECISIONS_AND_IMPLEMENTATION_2026-11-01.md`) in the `gametest` source set
(`worldgenprobe/stage0f`). Reservation (spacing + the single 75% roll) is read from Stage 0-E records and is never recomputed.

## Approved numbers used (unchanged)
Exterior radius 34 (68-block diameter), combat floor radius 29, outer wall 12 + dome 11 = 23, four 7x8 arch entrances (N/E/S/W), 8-12 perimeter
columns, six Vaults on radius 27 and one Ominous Vault at the exact centre, deepslate-family masonry, intact 50% / ruined partial dome 25% /
ruined natural ceiling 25%; terrain repair margin 4; corridor path cap 32 measured from the entrance exterior, 5x5 minimum clearance tapering from
7x8, stairs plus level stretches, one guaranteed connection and at most two; eight alternative placements; fluid zero tolerance replaced by a
seal-or-fail decision at construction time; structure collision via actual piece boxes, conservative when unknown.

## Engineering choices made to realise those rules (not owner decisions)
* **Eight alternatives** (WG-Q31/Q32) are eight deterministic floor levels `dy = 0,+1,-1,+2,-2,+3,-3,+4` at the *fixed* XZ centre (RESOLVE Q09/Q11
  forbid moving the centre after spacing); each is tried with all four entrance directions. Eligible candidates are ranked by the approved
  fixed-reference score S = 0.4C + 0.4T + 0.1F + 0.1E; ties go to the lower index. A failed candidate simply advances to the next; if all fail the
  placement is recorded `CONSTRUCTION_FAILED` and nothing else changes (no new roll, no reselection, denominator unchanged).
* **Genuinely traversable natural cave**: a component of standing positions (solid floor, 2 air above, 1-block steps) outside the arena body whose
  path length (double-sweep BFS) is at least 64, i.e. the WG-Q09 path class. The corridor must end on such a position within 32 steps.
* **Fluids**: every fluid cell touching the final open volume is replaced by cobbled deepslate *before* any cell is opened. Water of any size
  is sealable (WG-Q26: only a *major lava lake* is rejected); a lava body is only accepted if it is fully contained in the inspected 118-block radius,
  otherwise the candidate fails closed (no tolerated-fluid count is invented). Gravity blocks directly
  above the open volume are replaced by deepslate. The same flood-safety invariant is re-checked on the real world after construction and after
  server ticks with neighbour updates forced on every fluid-adjacent plan cell.
* **Application order and atomicity**: phases SEAL -> SOLID -> CARVE -> DECOR, batched per chunk column. Multi-chunk atomicity is NOT assumed:
  the plan and the pre-state are persisted before the first edit; recovery accepts a cell only if it is at the pre-state or a planned state and
  otherwise quarantines without deleting or overwriting anything.
* **Not built**: the activation crystal (Stage 1 controller content). The space above the centre Vault is kept free for it.

## Live evidence (real Minecraft 26.3 dedicated server, Fabric dev server via Loom; JDK 25.0.5)
Evidence is under `tools/st05-worldgen-probe/stage0f/live/seed-7001` and `seed-7002` (JSON, server logs, journals, persisted plans/pre-states).

| Check | Result |
|---|---|
| Seed 7001, four Stage 0-E reservations | 3 built and VERIFIED (`20d89ee7…`, `77ba83e0…`, `e1c48dbd…`), 1 `CONSTRUCTION_FAILED` (`2ac822e8…`: block-entity block, a sculk sensor, inside the body footprint, rejected conservatively for all 8 candidates); reservation, centre and roll WIN preserved |
| Each built arena (about 76,000 block edits) | plan conformance 0 mismatches; exterior radius 34; highest solid offset 22 (intact, 23 blocks total); 7 Vault blocks, Ominous at the exact centre; combat floor and 12-24 spawn annulus clear; four 7x8 entrances clear; one guaranteed + one second connection, each 5x5 clear and walkable from inside the arena to a qualified cave in 12-24 steps; 0 fluid cells adjacent to the open volume |
| Settle | 600 server ticks after construction with neighbour updates forced on every fluid-adjacent plan cell: re-verification clean |
| Restart (new JVM) | all VERIFIED placements re-verified read-only, 0 world writes; second full pass 0 writes (no duplicate construction); the failed placement stays failed |
| Generation/build order | seed 7001 rebuilt in a fresh world with the reservations in reverse order: identical plan hash and result for all four |
| Hard crash (seed 7002, natural-ceiling variant) | halt after 30 of 67 batches (last durable flush at batch 27) -> a second crash during recovery (batch 12) -> final recovery: 8,240 cells at pre-state / 1,166 intermediate / 43,335 final / 0 foreign -> resumed -> VERIFIED; restart verify 0 writes |
| Pure suite | `ConstructionSelfTest` 169 checks (routing, 8-alternative order/failover, clearance, sealable vs unsealable fluid, structure collision, determinism, crash/recovery, foreign-edit quarantine, torn/corrupt journal, plan tamper, terminal states) |

## Defects found by running the real thing and fixed (each re-verified)
1. All 8 candidates were rejected for any large water body: an invented "unbounded water" rule. Approved text rejects only major lava lakes; water is now sealable (iteration kept under `history/`).
2. Live verification quarantined a built arena: a stair directly beside a corridor turn made two overlapping 5-wide cross-sections demand different floor heights. Router now requires two level cells before and after a turn (regression test K).
3. Column layout produced 4 columns for some placement ids (approved 8-12): replaced by nearest-valid-slot selection; swept over 3,000 ids.
4. Verifier false positive: gravity-support seals above a natural ceiling were counted as arena height (regression test A2).

## Remaining blockers / limits (honest)
* The activation crystal is Stage 1 controller content and is not placed. Vault loot tables are vanilla defaults.
* Only the INTACT (3) and RUINED_NATURAL_CEILING (1) variants occurred in live placements; RUINED_PARTIAL_DOME is covered by the pure suite only.
* One of five live placements failed closed (conservative protection of block-entity blocks); no tolerance was invented. Whether natural deep-dark sculk should count as a "protected structure" is an owner/design question, not decided here.
* Multi-chunk atomicity is not claimed: recovery is by comparing every cell with {pre-state, planned states}, and was tested for process crashes (not power loss or disk corruption).

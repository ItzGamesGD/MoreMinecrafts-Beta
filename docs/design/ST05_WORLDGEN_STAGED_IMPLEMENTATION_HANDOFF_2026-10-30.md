# ST-05 Ancient Colosseum — gated implementation delegation (2026-10-30)

**Status: HANDOFF INSTRUCTIONS, NOT EXECUTION OR APPROVAL OF INTEGRATED WORLDGEN.** The owner requests a staged implementation delegation after a design audit. Only isolated feasibility probes may start initially. Do not merge, promote a checkpoint, change main, or silently relax owner-approved design rules. Pause and report when a gate fails.

## Pinned design sources
- [WG-Q01–Q45 + FIX-01 A](ST05_WORLDGEN_OWNER_DECISIONS_Q01_Q18_2026-10-30.md)
- [Preimplementation audit and re-audit](ST05_WORLDGEN_PREIMPLEMENTATION_AUDIT_2026-10-30.md)
- `docs/STRUCTURE_AUTHORITY_INDEX.md` (worldgen overlay precedence)
- `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md` (combat, lifecycle, persistence, rewards)
- Existing controller PR #213 and protected-mask PR #216: check live heads and compatibility; do not infer they are merged or complete.

## Mandatory invariant
FIX-01 A: deterministic fit checks (up to **8 candidates**) **before** a single **75% roll** for each demonstrably placeable eligible cavern. No second roll per candidate. Preserve minimum separation, one arena per cavern, exact 68 diameter / 23 height / 12 wall + 11 dome, no stands, floor radius 29, Vault radius 27, spawn annulus radius 12–24, four 7x8 entrances, at least one <=32 traversable cave path with >=5x5 unobstructed internal clearance, and all structure/fluid/terrain hard gates. Center Ominous Vault directly beneath activation crystal. Q43: deterministic work budgets; elapsed wall-clock time cannot change placement results. 50/25/25 architectural variants. Preserve player edits and existing trial contracts.

## Stage 0 — Baseline and feasibility (ONLY stage initially authorized to start)
1. Pin the current relevant branch, PR heads and authority file blobs. Verify Minecraft 26.3 mappings/APIs; no assumptions from unrelated Ancient City arena or test fixture radius.
2. **A02 probe**: propose and test deterministic connected-cavern identity across region boundaries and restarts. Show same seed / differing chunk orders / concurrent generation produces identical cavern IDs and no duplicates. Define an explicit deterministic priority for neighboring caverns competing under minimum separation. **If this changes the 75% denominator or Q10 semantics, STOP for owner approval.** Do not invent a silent priority policy.
3. **A03 probe**: demonstrate whole placement/connection/collision plan can be established before per-chunk writes, with no partial/duplicate structure across save/reload, restart, and varied chunk generation order. Confirm failure behavior when plan cannot be materialized atomically.
4. Probes may use isolated scratch code/tests and an unmerged Draft PR; **do not connect generator to production worldgen yet**. Report concrete evidence, counterexamples, performance and any unresolved design choices.
5. STOP if A02/A03 fail. Do not progress to Stage 1 without separate owner approval of probe results and any necessary design clarification.

## Stage 1 — Geometry prototype (conditional, NOT automatically authorized)
- Implement standalone 68-diameter circular shell, flat floor radius 29, 12-high wall, 11-high dome, four 7x8 masonry arches, 8–12 perimeter columns, no stands, center crystal/Vault and six ordinary Vaults at radius 27.
- Procedural architecture plus ST-05-specific NBT decorative modules. Three variants 50/25/25. Verify voxel clearances, protected occupancy mask, flat and bright combat floor, spawn annulus and no vault obstructions. No worldgen hook.

## Stage 2 — Cave fitting and connection (conditional)
- Implement deterministic natural-cave qualification (chamber width >=32, height >=18, traversable connected cave path >=64), Y -40 to -10, <=8 candidate placements, fixed-reference weighted score (40/40/10/10 with Q36–Q39 subweights). Define actual monotonic 0–100 transforms.
- At least one, at most two, natural cave links; each <=32 traversable path, >=5x5 clearance, stairs where needed. Preserve terrain outside 4 blocks except approved corridor/junction. Reject protected structures and unsafe fluids. Validate cave topology before/after.

## Stage 3 — Deterministic placement and sampling (conditional)
- Only after A02/A03 resolved: implement one-roll-per-fit-eligible-cavern at 75% and minimum spacing with owner-approved precedence, deterministic region/chunk generation, no partial structures.
- Start 30 seed sampling, extend for low eligible-cavern counts. Log denominator, fit-eligible caverns, roll wins, spacing conflicts, actual placements, all three variants, timing distributions, memory, reload and restart consistency. One reproducible BLOCKER stops final acceptance; MINOR issues documented.

## Stage 4 — Integration and final acceptance (conditional)
- Integrate only with compatible controller/structure mask heads, preserve seven-round trial, supplies, Vault/key semantics, boss block protection and persistence. Client/server smoke and Minecraft 26.3 real-server visual screenshots and structure probes required.
- Produce Draft PR with full validation matrix, seeds/coords, before-after screenshots, raw failures and exact SHAs. Request owner review. **No automatic merge, branch deletion, checkpoint promotion or declaration of DONE.**

## Reporting protocol
Report briefly in the PR body: Stage, branch, HEAD SHA, tests and raw evidence links, PASS/FAIL/UNKNOWN, blockers, design change requests. Do not rerun expensive checks on unchanged SHAs without reason. Explicitly distinguish simulated/unit proofs from live 26.3 tests.

## Decision gates
- A01: RESOLVED by owner FIX-01 A.
- A02/A03: OPEN, must pass before production worldgen.
- A04–A06/A08: OPEN, validate in relevant stage.
- Any design conflict, undefined minimum spacing precedence, unsound cave identity, cross-chunk nondeterminism or geometry deviation: **STOP / OWNER_DECISION_REQUIRED**.

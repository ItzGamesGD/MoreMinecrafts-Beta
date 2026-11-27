# Implementation Report — Terrain-Adaptive Mountain Sky Tower Placement (Option B)

Labels: IMPLEMENTED · RUNTIME_VERIFIED · SOURCE_VERIFIED · PARTIALLY_VERIFIED · UNVERIFIED · BLOCKED.

## Starting points (verified at the start)
* `claude/sky-tower-phase-2-validation-twragg` = `d12a6e114f00055b6cc62e7e272474ed46f33bbb` (remote == expected; read Phase 2 reports/evidence).
* `feature/sky-tower-integration-02` = `5c22f9fb764d545f0ce4241c75bc47f5b54e447d` (remote == expected, no divergence). Clean tree. Implementation branch `implementation/sky-tower-mountain-adaptive-b` created from it; the Phase 2 branch was **not** merged — only the needed probe/tooling ideas were re-implemented (`tools/sky-tower-adaptive/`, `SkyTowerAdaptiveProbe`).

## Approach (IMPLEMENTED)
A custom structure type `moreminecrafts:adaptive_jigsaw` (`AdaptiveJigsawStructure`) wraps — does not copy — `JigsawPlacement.addPieces`. It preserves vanilla draw order, uses the really selected element's `sizeY`, limits the offset to `min(70, 315 − T − (sizeY−1))`, rejects if that is `< 18`, and otherwise keeps the vanilla roll when it fits or draws a replacement **uniformly on `[18, h_eff]`** (rejection sampling, never clamping). Details: `PLACEMENT_ALGORITHM.md`. No Mixins, no new registries other than one structure type, structure IDs/pools/sets/tags unchanged.

### Files changed
| File | Change |
|---|---|
| `src/main/java/.../worldgen/structure/AdaptiveJigsawStructure.java` (new), `AdaptivePlacementMath.java` (new) | the placement type and its pure arithmetic |
| `src/main/java/.../MoreMinecrafts.java` | +1 line: initialise (register) the type first |
| `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_{snowy,rocky,meadow}.json` | `type` → adaptive; `start_height` → `minimum_offset 18`, `maximum_offset 70` |
| `scripts/structures/sky_islands/generate.py` | emits the same JSON for the mountain ids |
| `src/test/.../AdaptivePlacementMathTest.java` (new), `SkyIslandsResourceTest.java` | boundary tests; mountain structure assertions |
| `src/gametest/.../SkyTowerAdaptiveProbe.java` (new), `src/gametest/resources/fabric.mod.json` (+1 entrypoint) | inert validation probe |
| `tools/sky-tower-adaptive/**` | drivers, legacy JSON copies for pairing, analysis |
| `docs/implementation/sky_tower_mountain_adaptive/**` | this evidence |

Not touched: NBT, template pools, structure sets, biome tags, general variants, tower/barrier/registration/progression code, loot, Vaults, Trial Spawners.

## Requirement status
| ID | Requirement | Status |
|---|---|---|
| B-01 | terrain-/element-adaptive placement | IMPLEMENTED, RUNTIME_VERIFIED (natural + controlled) |
| B-02 | minimum offset 18 preserved (not redefined as footprint clearance) | RUNTIME_VERIFIED: min generated offset 18 in every run; footprint measured separately (`STRUCTURE_INTEGRITY.md`) |
| B-03 | original 18..70 kept wherever valid | RUNTIME_VERIFIED: draw-1 roll kept whenever `≤ h_eff` (11,851 identical, 0 different among fitting) |
| B-04 | reject when infeasible; no clip/relocate/shrink/substitute | IMPLEMENTED; RUNTIME_VERIFIED: T=202 → 0/797 per variant, 0/120 natural flat |
| B-05 | only the three mountain variants | RUNTIME_VERIFIED: general set 37,199 rows identical |
| B-06 | gameplay untouched | SOURCE_VERIFIED (empty diff); integrity RUNTIME_VERIFIED |
| §3 | no clamp; correct bounded sampling; element not preselected; weights preserved | RUNTIME_VERIFIED (1/47 at upper boundary; selection share 14.50 % vs nominal 14.29 %, unchanged draws) |
| §4 | heightmap, padding, sky-biome check, rotations, determinism | engine-enforced and RUNTIME_VERIFIED (all four rotations, 3 deterministic runs) |
| §5 | existing IDs/saves | RUNTIME_VERIFIED with a baseline-generated saved world |

## Final answers
* **18-block minimum preserved?** Yes — never reduced; infeasible candidates are rejected.
* **18..70 preserved wherever feasible?** Yes — bit-identical to baseline whenever the original roll fits; uniform on `[18, h_eff]` otherwise.
* **Mountain tower acceptance (natural, 5 seeds, 1,725 tower draws):** **99.42 %** [98.94, 99.68] vs 96.70 % baseline; 100 % of towers at T 140–201 in the samples (311/311); 100 % (797/797) in the controlled sweep for T ≤ 201 per variant.
* **Remaining impossible terrain:** tower with offset ≥ 18: **T ≥ 202** (policy). (Physical limit with any offset ≥ 1: T ≥ 219.) Non-tower islands fit up to higher terrain (temple fits to T = 253, plain/ruins to 271 at offset 18 — but note non-towers still only reject when *their* `h_eff < 18`).
* **Overall rarity:** towers +2.8 % (521 → 536 per 10,000 km²), all islands +0.4 %, tower share of generated 14.09 % → 14.43 %; effect small because only 1.7 % of mountain candidates are at T ≥ 180.
* **Performance:** +0.18 ms and +0.9 heightmap queries per biome-eligible candidate (+28 % time, +12 % allocation on those); none for ineligible candidates beyond one RNG object (`PERFORMANCE_AND_DETERMINISM.md`).
* **Regression:** unit tests 594/0; validators pass; game tests: fresh worktree = identical single pre-existing Colosseum failure as baseline (`REGRESSION_REPORT.md`).

## Unresolved owner decisions / notes
1. Physical terrain-footprint clearance is not guaranteed by B-02; 0.9 % of sampled islands have ≥ 1 column where an island block is at/below terrain (towers 2/185). Decide only if a footprint-clearance guarantee is wanted.
2. T ≥ 202 stays tower-free by policy; confirm acceptable (tower-free peaks in jagged/frozen/stony biomes).
3. `start_height` on these three structures is replaced by `minimum_offset`/`maximum_offset`; datapacks overriding `start_height` for them stop applying.
4. `generate.py --check` not run (needs `nbtlib`).
5. Game-test runs inside a stale working directory fail 20–25 tests even on pristine baseline code (environmental; not investigated).

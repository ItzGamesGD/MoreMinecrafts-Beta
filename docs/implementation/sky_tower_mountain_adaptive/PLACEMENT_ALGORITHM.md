# Placement Algorithm — Adaptive Mountain Sky Islands

Status labels: IMPLEMENTED · RUNTIME_VERIFIED · SOURCE_VERIFIED · PARTIALLY_VERIFIED · UNVERIFIED · BLOCKED.

## Source
* `src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/AdaptiveJigsawStructure.java` — structure type `moreminecrafts:adaptive_jigsaw` (registered in `MoreMinecrafts.onInitialize`, first line). **IMPLEMENTED**
* `.../AdaptivePlacementMath.java` — pure arithmetic (unit-tested). **IMPLEMENTED**
* `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_{snowy,rocky,meadow}.json` — only `type` changed; `start_height` replaced by explicit `minimum_offset: 18`, `maximum_offset: 70`. Structure IDs, pools, biome tags, structure sets, `dimension_padding: 4`, heightmap `WORLD_SURFACE_WG`, `max_distance_from_center`, step, `terrain_adaptation` unchanged. **IMPLEMENTED**
* `scripts/structures/sky_islands/generate.py` emits the same JSON for the mountain ids (generator `--check` could not be run: `nbtlib` is not installed here → **UNVERIFIED**; JSON key order was matched to the generator by hand).

## Engine semantics used (Minecraft 26.3, decompiled `JigsawStructure`/`JigsawPlacement`; verified at runtime in Phase 2 and again here) — SOURCE_VERIFIED + RUNTIME_VERIFIED
* `T` = top terrain block = `getFirstOccupiedHeight` = heightmap first-free − 1. Root piece `minY = h + T`, `maxY = minY + sizeY − 1`.
* Reject iff `maxY > level.getMaxY() − padding.top` with `getMaxY()` inclusive = 319 → **limit 315**. Tower: `h ≤ 219 − T`.
* Draw order of vanilla on the structure random: (1) height, (2) `Rotation.getRandom`, (3) `getRandomTemplate` (single draw, no retry).

## Algorithm (`findGenerationPoint`, lines 92–110)
1. `roll = random.nextInt(max−min+1)+min` on the real structure random — **bit-identical to vanilla `UniformHeight`** (`Mth.randomBetweenInclusive`). Vanilla's next two draws (rotation, element) are then made by `JigsawPlacement.addPieces` exactly as before, so **pool-element probabilities (3/21 for the tower), rotation and element choice are unchanged for the same seed**.
2. Look-ahead on a *copy* of the stream (`setLargeFeatureSeed(seed, chunkX, chunkZ)`): draw 1, rotation, element → learns the element/rotation the engine will pick without consuming the real stream. The element is never re-drawn or substituted.
3. Box of that element/rotation gives centre and `sizeY` (rotation-dependent XZ, invariant Y). Same early-out as vanilla if the biome column is invalid.
4. `T` from one `getFirstOccupiedHeight` at the centre (same column `addPieces` uses). `h_allowed = 315 − T − (sizeY−1)`; `h_eff = min(70, h_allowed)`.
5. `h_eff < 18` ⇒ return empty (reject). No clip, no horizontal move, no other element, no retry, no relaxed minimum (B-04). **IMPLEMENTED**
6. `roll ≤ h_eff` ⇒ use `roll` unchanged (so every candidate that vanilla accepted is bit-identical). `roll > h_eff` ⇒ replacement drawn **uniformly from `[18, h_eff]`** from the look-ahead stream (after its three draws): rejection sampling, **not clamping**. The result is exactly uniform on `[18, h_eff]` and equals the original 18..70 distribution restricted to the feasible range.
7. `JigsawPlacement.addPieces(...)` with start offset `roll`, heightmap `WORLD_SURFACE_WG`, `dimension_padding` and biome stub check unchanged (sky-position biome check and padding still enforced by the engine).

## Why not clamp / why this sampling
Clamping puts probability `P(roll>h_eff)` on `h_eff` (47 of 47 resampled natural towers would sit at the upper boundary). Measured with rejection sampling: **1 of 47** at the boundary (uniform expectation 3.8). **RUNTIME_VERIFIED** (`DISTRIBUTION_COMPARISON.md`).

## Random-number consequences (`PERFORMANCE_AND_DETERMINISM.md`)
Main stream consumption identical to vanilla (1 draw before `addPieces`); the look-ahead uses a separate stream. Only candidates with `roll > h_eff` get a different offset; all other results are identical to baseline (11,851 identical, 0 different among fitting candidates).

## Conflicts / limitations (none block the requirements)
* The first RNG draw uses the configured `minimum_offset..maximum_offset` range (not a `HeightProvider`); a datapack that overrides `start_height` on these three structures no longer applies (field removed). Documented, intentional.
* Vanilla's optional `start_jigsaw_name` and `pool_aliases` are not supported by the type (unused by these structures).
* `T` is sampled at the box centre (as vanilla does for the start), not under the whole footprint (B-02 / §7).

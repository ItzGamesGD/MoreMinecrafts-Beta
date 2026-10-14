# K5 production integration

Status: implementation in Draft PR #84. Binding gameplay policy is the approved redesign at the top of PR #81's `docs/K5_NATURAL_TREE_PROVENANCE_DESIGN.md`. The older 128-log item-acquisition design below its divider is historical.

## Rule and ownership

K5 is unlocked after one Survival player directly and successfully mines 200 distinct marked log blocks from naturally world-generated vanilla trees. Receiving, dropping, stacking, crafting, gifting, storing or trading log items cannot advance K5. Creative and Spectator breaks, grown or placed trees, unknown-origin logs, structure generation, explosions and pistons are ineligible. There are no item tokens, player redemption ledgers or client authority.

The existing `research_progress_v4` player attachment stores `condition.natural_vanilla_logs_mined` and `knowledge.5`; its codec, copy-on-death behavior, delta sync, UI row, notification and reward paths are reused. The old `condition.large_tree_logs_acquired` key is ignored. No other Knowledge or Research state is reset.

## Position provenance

`K5NaturalTreeProvenance` owns a Fabric persistent chunk attachment `moreminecrafts:k5_natural_tree_logs_v1`: schema version 1 and a sparse set of packed absolute block positions. A missing attachment, unknown schema, cross-chunk position or absent vanilla-registered log fails closed. Old chunks without this attachment receive no inferred markers. The probe's former `k5_natural_tree_probe_v1` attachment is test-only and has no production migration; it was never distributed as gameplay state.

Minecraft 26.3 mapped hooks:

| Scope | Mapped class and method | Descriptor | Purpose |
| --- | --- | --- | --- |
| Natural decoration | `ChunkGenerator.applyBiomeDecoration` | `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V` | Open natural generation scope |
| Placed feature | `FeaturePlacer.placeWithBiomeCheck` | `(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z` | Require registered Minecraft placed-feature key |
| Trunk setter | `TreeFeature.place` wrapping `doPlace` | See `K5TreeFeatureMixin` | Record successful actual log writes only for pinned vanilla TreeFeature registry IDs |
| Fallen-tree placement | `FallenTreeFeature.place` | `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z` | Collect changed log writes within only the pinned vanilla fallen-tree feature call |
| Server mutation | `Level.setBlock` | `(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z` | Invalidate changed positions |
| Worldgen mutation | `WorldGenRegion.setBlock` | `(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z` | Invalidate later worldgen replacements and capture actual log writes only inside an active pinned FallenTreeFeature call |
| Direct mining | `ServerPlayerGameMode.destroyBlock` | `(Lnet/minecraft/core/BlockPos;)Z` | Attribute one successful Survival mining action |

These names and signatures were checked against locally resolved Minecraft 26.3 sources and the mapped-source workflow. `FallenTreeFeature.place` was checked against the resolved source and the required GameTest Mixin config loads its wrapper. The existing `ServerPlayerGameModeMixin` wraps the method once, retaining the enchantment break scope. The four old test-only worldgen and mutation wrappers were removed; test observations now listen to production hooks and use the production attachment, leaving one provenance store.

The mining wrapper snapshots valid position provenance before the original method removes the block. It credits only when the result is true, the block is air and the *stored marker itself* is absent afterward. This extra postcondition matters because mapped `destroyBlock` returns true even if `Level.removeBlock` returned false. Fabric cancellation is exercised by a runtime test. `Level.removeBlock` routes through `Level.setBlock`; successful changes consume the marker before progress is recorded. Later replacement logs at the same position cannot inherit it. The server's single world thread serializes competing break calls.

## Tests and evidence

- Deterministic natural fixture: seed 0, `minecraft:forest`, `minecraft:overworld` noise settings, chunk `[0,0]`, genuine `applyBiomeDecoration`, runtime-observed `minecraft:oak_bees_0002_leaf_litter`, 23 final markers in the local run. The test then directly mines actual marked logs and checks 0→1, independent players, no double count, Creative/Spectator exclusions, 199→200 unlock, cancelled break, indirect removal and replacement.
- Existing sapling, bonemeal and player-placement GameTests additionally mine their ineligible logs and assert zero K5 progress.
- Separate GameTests exercise vanilla piston movement, vanilla explosion destruction, Minecraft StructureTemplate log placement, ItemEntity pickup, chest-to-hopper movement, a registered test-namespace tree placement during natural decoration, and the vanilla player respawn path. The non-vanilla feature test confirms that real oak trunks grow but remain unmarked and give no mining credit.
- Existing probe lifecycle test checks save, real unload/reload, invalidation, replacement, cross-chunk and cross-dimension isolation with the production attachment.
- The dedicated `K5 Production Restart` workflow filters one GameTest across two JVM processes sharing one world. Process A saves an actual natural marker, consumed/replaced markers and a fixed-UUID player's progress; process B loads the chunk and player data through Minecraft APIs. The small test manifest contains expected positions and is not gameplay storage. This is a headless GameTest server restart; the separate Fabric CI job runs dedicated-server and client smoke checks.
- JUnit checks the 200 target, counter cap, codec round trip and existing UI progress row. No client packet can grant K5; serverbound networking remains limited to existing non-K5 requests.

## Production K5 audit remediation

The approved PR #81 rule remains unchanged: Survival players must directly and successfully mine 200 marked logs from naturally world-generated vanilla trees. Progress remains server-side, position-backed, persistent and capped at 200.

### Findings and changes

- Before remediation, `recordTrunkWrite` copied and revalidated the full immutable marker set for each trunk callback. For a tree with *n* trunk writes in one chunk, that repeated copy/validation path was O(n²). `TreeFeature.doPlace` callbacks are now buffered until successful placement, grouped by chunk and committed as one immutable attachment update per affected chunk.
- `data(chunk)` previously rescanned all packed positions for chunk ownership on every lookup. It now validates each immutable attachment object once and keeps a weak-key cache of the validated object identity. A replacement attachment object is revalidated; malformed schema or cross-chunk data is removed and fails closed. The cache holds no strong reference from values back to chunk keys.
- `invalidatePosition` still uses copy-on-write for each removal. Fabric's persistent attachment dirty tracking is driven by `setAttached` value changes; mutating an attached set in place would risk an unsaved chunk. The removal path remains O(n), deliberately preserving correct serialization and dirty tracking. The 1,000-marker stress run below measured the complete consume loop at 74.1 ms (about 74 μs per removal in this synthetic fixture); this is not a server TPS measurement.
- Review found that nested non-vanilla placed features did not shadow an eligible outer frame. Every nested feature now pushes an identity frame, so the current top frame fails closed when it is unknown or untrusted.
- Runtime registry inspection found six `FallenTreeFeature` IDs nested in vanilla biome-generation settings: `minecraft:fallen_oak_tree`, `fallen_birch_tree`, `fallen_super_birch_tree`, `fallen_spruce_tree`, `fallen_jungle_tree` and `fallen_poplar_tree`. These are real vanilla natural-tree variants, separate from `TreeFeature`, and use the same feature placements that can also be reached by sapling growth. The production probe now recognizes only these six registered identities inside natural biome decoration and captures their actual changed log writes through `WorldGenRegion.setBlock`; sapling growth remains outside the natural-decoration scope. Unknown fallen-tree IDs remain rejected. A runtime scan of seed-0 forest did not execute a fallen variant, so an actual fallen-tree marker is not claimed as runtime verified.
- Test-only cooperating Mixin wrappers now wrap `ServerPlayerGameMode.destroyBlock` and `Level.setBlock` during GameTests. The natural-log mining test confirms that both wrappers ran alongside production credit and marker invalidation. This is focused MixinExtras composition evidence, not a claim of broad modpack compatibility.

### High-density microbenchmark

The GameTest builds synthetic marker sets in one chunk at 100, 500 and 1,000 positions. It checks exact counts, repeated marker lookup, immutable attachment codec/NBT round-trip, serialized payload size and complete marker consumption. Each timing is a median of three measured repetitions after one warmup in the same test JVM. These are focused operation timings, not realistic world-generation or server TPS measurements; allocations were not sampled with a profiler.

| Markers | Insert batch | Lookup batch | Consume all | Codec/NBT round-trip | NBT payload |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 0.314 ms | 0.047 ms | 1.731 ms | 0.295 ms | 857 B |
| 500 | 0.911 ms | 0.104 ms | 19.515 ms | 0.321 ms | 4,057 B |
| 1,000 | 1.219 ms | 0.204 ms | 74.072 ms | 0.593 ms | 8,057 B |

The recorded pre-change baseline used the prior per-position insertion and uncached validation path in the same GameTest shape: 100 markers took 2.455 ms insert / 1.181 ms lookup / 2.187 ms consume; 500 took 26.998 / 2.518 / 24.632 ms; 1,000 took 62.521 / 8.885 / 61.211 ms. Payload sizes were 857 / 4,057 / 8,057 B. The new batched insertion and cached lookup paths improve these measured cases. Removal remains linear per mutation; measured total is noisy and was not improved. The before/after runs were not collected under a dedicated benchmark harness, so treat the ratios as indicative microbenchmark evidence only.

### Identity and compatibility limits

- Deterministic seed-0 `minecraft:forest` positive runtime generation still verifies the actual registered `minecraft:oak_bees_0002_leaf_litter` `TreeFeature`, 28 observed trunk callbacks and 23 final marked log positions. The new registry GameTest confirms all six runtime-registered vanilla `FallenTreeFeature` IDs are allowlisted and an unknown fallen ID is rejected.
- An attempted extra runtime scan over 48 generated chunks did not observe an executed fallen feature and produced unregistered-biome serialization noise from the synthetic fixture. It was removed rather than treated as evidence. The new fallen-tree capture path is source- and Mixin-transform-verified, but a deterministic natural `FallenTreeFeature` marker test remains outstanding.
- Tree features newly introduced by mods, unknown identities and non-vanilla placed features fail closed. A deliberately malicious mod reusing the `minecraft` namespace and an allowlisted feature identity remains outside the approved threat model. Synthetic wrappers only establish cooperation with the tested MixinExtras wrappers; arbitrary Mixin ordering and modpack combinations still require release testing.
- Same-process chunk unload/reload and the existing two-process restart workflow continue to exercise persisted marker and progress state. Record final-HEAD results after all checks complete.

### Final-HEAD validation

Final code commit `fd01a6f5d1798566671d8c36ba00626315345af3` was validated by:

- [Fabric CI run 38519513607](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38519513607): the first attempt hit the existing unrelated custom-mob repeated-finalize assertion. The failed job was rerun without code changes; the rerun passed `clean test build compileGametestJava runGameTest`, all 85 required GameTests, artifact creation and upload. Dedicated-server smoke and client smoke passed in the same run. The rerun K5 density lines and all-85 result appear in its Build logs.
- [K5 mapped-source run 38519513633](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38519513633): passed. The generated-source inspection confirmed `FallenTreeFeature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos): boolean` with descriptor `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z` and source writes through `WorldGenLevel.setBlock`.
- [K5 two-process restart run 38519513615](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38519513615): passed. Process A wrote and saved the provenance/progress fixture; process B restored it from the same world. Each phase passed 1/1 GameTest.

Local `./gradlew clean test build` also passed 225/225 JUnit tests. Local fresh-world GameTests were variable: one 85/85 pass, one retry pass, and two failures of only the unrelated custom-mob multiplier assertion. The final-commit CI rerun passed all required tests. No custom-mob production code or assertion was changed. The seed-0 positive tree trace remained 28 callbacks / 23 markers. The final CI benchmark run reported: 100 markers 0.527 ms insert / 0.090 ms lookup / 3.515 ms consume / 0.309 ms codec, 857 B; 500 markers 1.059 / 0.203 / 27.216 / 0.396 ms, 4,057 B; 1,000 markers 1.169 / 0.291 / 62.582 / 0.707 ms, 8,057 B. These remain synthetic microbenchmarks, not TPS or allocation-profiler results.

## Limits and release gate

The vanilla feature allowlist is pinned to observed Minecraft 26.3 TreeFeature registry keys. Unknown configured keys fail closed. A mod deliberately registering new content in the `minecraft` namespace while reusing an allowed TreeFeature is outside the approved non-malicious-mod threat model. Real world generation beyond the tested vanilla feature variants, high-density chunk performance and modpack compatibility need release validation. Build and GameTest success do not by themselves certify release readiness.

The server-restart test must report its process boundary separately from same-process chunk unload/reload. Any CI failure must be diagnosed and rerun on the final commit. Do not merge until the required checks pass and the Draft PR is reviewed.

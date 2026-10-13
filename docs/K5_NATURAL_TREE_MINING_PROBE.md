# K5 natural-tree mining technical probe — execution checklist

Status: **BLOCKED BEFORE PROBE IMPLEMENTATION**. The source-generation gate could not resolve the pinned Fabric Loom plugin in this environment. Per the execution checklist, mapped hooks were not guessed and no probe code was added. The CI result below validates the pre-existing PR #82 tree only; it does not validate a natural-tree probe.

## Authority

- Design: PR #81, current fetched head `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86`, first section of `docs/K5_NATURAL_TREE_PROVENANCE_DESIGN.md`.
- Base: PR #80 commit `1a17f8e77792ab1b106d92ac03fe10c86227d599` at branch creation. Re-fetch both PRs before implementation.
- K5: **200 naturally world-generated vanilla tree log blocks directly broken by a Survival player**. No item receipt, gifts, pickups, trading, or container transfers count.
- Exclude player-grown trees, player-placed logs, structure trees, Creative, pistons, explosions, unknown origin.
- No per-item UUIDs, item components, player redemption ledgers, stack merge/split hooks, or legacy migration.

## Work to execute in a Java 25 network-enabled checkout

1. `./gradlew genSources`. Record exact resolved named classes/methods/descriptors. Stop if source resolution fails; do not invent mappings.
2. Instrument a test-only, no-credit probe at the natural biome tree feature boundary; record final placed natural-tree log positions in persistent chunk data. Distinguish sapling/bonemeal tree growth by actual source path, not tree shape or species.
3. Probe successful server-side Survival player block break; prove cancellation and Creative paths cannot credit. No K5 progress hook or unlock changes.
4. Run tests across vanilla natural tree species, natural 2×2 trees, sapling/bonemeal growth, player placement, structure tree, piston/explosion, chunk unload/reload, server save/reload.
5. `./gradlew compileGametestJava runGameTest`, build, and relevant CI smoke. Log exact run URLs and results. Mark unexecuted scenarios UNVERIFIED.
6. Report mapped hooks, lifecycle persistence evidence, unresolved blockers and whether production K5 implementation is ready.

## Safety / scope

Only probe/test code and audit documentation in this PR. Keep PR #79, #80, #81 unchanged. Do not merge, enable K5 progress, or start Stage 2-C.

## Current evidence

- Re-fetched PR #82 before work: head `6d49d87c74faadbd2938043e55870eca56759dda`, Draft, targeting PR #80 head `1a17f8e77792ab1b106d92ac03fe10c86227d599`. Re-fetched PR #81 head is `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86`; its 200-log direct Survival mining rules match this checklist. No conflict with current K5 approval was found.
- Toolchain: Java `25.0.4.1`, Gradle wrapper `9.6.0`, Minecraft `26.3`, Fabric Loom `1.17.21`, Fabric API `0.161.0+26.3`.
- **`./gradlew genSources`: BLOCKED.** Attempted with Java 25 and the configured Gradle cache, including a network-enabled retry. Gradle could not resolve `net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:1.17.21` from Fabric Maven, the Gradle Plugin Portal, or Maven Central. `--info` showed `java.net.SocketException: Network is unreachable` for `https://maven.fabricmc.net/`; the retry ended with the same unresolved plugin. No mapped Minecraft source or Mixin descriptor was available locally.
- **`./gradlew compileGametestJava runGameTest build`: BLOCKED locally** at the same Loom plugin resolution step. No local GameTest ran.
- **GitHub Actions on pre-probe PR #82 head `6d49d87c74faadbd2938043e55870eca56759dda`:** run [#38326823336](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38326823336) passed `build` (workflow command includes `clean test build compileGametestJava runGameTest`) and `server-smoke`; the last observed state had `client-smoke` in progress. This is baseline CI only; it does not exercise probe code.
- **GitHub Actions on documentation-update head `80ef646f10cd13066ae6002a1fa967d85fb6aa89`:** run [#38327473302](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38327473302) passed all three jobs: `build` (including `clean test build compileGametestJava runGameTest`), `server-smoke`, and `client-smoke`. This run validates the existing project and documentation-only change; it does not exercise probe code.
- **Natural-tree probe/GameTest: NOT IMPLEMENTED / NOT RUN.** The checklist requires stopping when source resolution fails, so natural feature and block-break hooks were not guessed. No K5 condition update, counter increment, or unlock path was added or enabled.
- **Probe scenarios (natural tree species, sapling growth, placement, structures, piston/explosion, persistence): UNVERIFIED.**
- **Readiness:** blocked pending a Java 25 environment that can resolve the pinned Loom/Minecraft sources. Resume at checklist item 1, record mapped signatures, then implement the diagnostic-only probe and its GameTests before claiming K5 technical readiness.

## 2026-10-13 verified mapped-source probe and runtime checks

This section supersedes the earlier local-network blocker and the earlier “probe not implemented” status. PR #81 was re-fetched before this work and still has head `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86`; its approved rule remains **200 naturally world-generated vanilla tree log blocks, directly mined by a Survival player**. Sapling-grown, player-placed, structure-generated, modded-source, Creative, piston/explosion and unknown-origin logs remain excluded. K5 progress is still not enabled.

### Evidence from mapped Minecraft 26.3 sources

- Focused source-inspection Actions run [#38342540199](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38342540199) passed `genSources` and the signature-inspection step. It uploaded only the 4.6 KB selected API report and generation log, not Minecraft source archives.
- The earlier user-cited source-generation run [#38338849034](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38338849034) also passed `genSources`; local sandbox network failures are not treated as an overall verification failure.
- Mapped declarations and descriptors recorded in the report:
  - `ChunkGenerator.applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureManager): void` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V`.
  - `TreeFeature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos): boolean` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z`.
  - `TreeFeature.doPlace(WorldGenLevel, RandomSource, BlockPos, BiConsumer, BiConsumer, FoliagePlacer.FoliageSetter): boolean` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;)Z`.
  - `SaplingBlock.advanceTree(ServerLevel, BlockPos, BlockState, RandomSource): void` — `(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V`.
  - `TreeGrower.growTree(ServerLevel, ChunkGenerator, BlockPos, BlockState, RandomSource): boolean` — `(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z`.
- Generated-source call-path evidence: `ChunkGenerator.applyBiomeDecoration` calls `FeaturePlacer.placeWithBiomeCheck`; `TreeFeature.place` calls `doPlace` with separate root and trunk callbacks; `SaplingBlock.advanceTree` delegates to `TreeGrower.growTree`, which calls `Feature.place` directly. This distinguishes their source paths without inferring provenance from a tree's shape.
- Fabric attachment method descriptors inspected: `AttachmentRegistry.create(Identifier, Consumer): AttachmentType` and `AttachmentTarget.getAttached(AttachmentType): Object` / `setAttached(AttachmentType,Object): Object`. Applicability to runtime chunks is also exercised by the GameTest below.

### Diagnostic-only implementation

- Added under `src/gametest` only: no production Java, main-mod Mixin, K5 progression collector/counter, unlock, or gameplay behavior was added. The GameTest mod alone loads `moreminecrafts-k5probe.mixins.json`; its diagnostic invocation counter only asserts that the wrapper ran.
- The test-only `ChunkGenerator` wrapper scopes `applyBiomeDecoration` using the mapped descriptor above. The test-only `TreeFeature` wrapper observes its actual `doPlace` trunk callback, accepts only final `BlockTags.LOGS` writes, and stores packed positions in a persistent Fabric chunk attachment.
- Attachment type ID: `moreminecrafts:k5_natural_tree_probe_v1`; schema version `1`; fields `schema_version` and `natural_tree_log_positions`. A position is added idempotently. Data is chunk-local and codec-backed; there is no global per-world position map.
- The sapling route does not enter the biome-decoration scope, so the same `TreeFeature` class being used by a sapling does not make its output eligible. No direct-break or K5 credit hook exists in this probe.

### Actions runtime result

- Full Fabric CI run [#38342540238](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38342540238) on probe commit `191e316b5503f7270c20bc20b2e7ce64dbf1f778`: `./gradlew clean test build compileGametestJava runGameTest` **passed**; the GameTest server reported **all 68 required tests passed**. Build, server-smoke and client-smoke jobs all completed successfully.
- K5-specific GameTests executed in that run:
  - `k5NaturalBiomeDecorationHookRunsAndRestoresScope`: directly invokes the mapped world-decoration method in the Minecraft GameTest runtime and verifies the test Mixin observes it and clears scope afterward.
  - `k5ChunkAttachmentStoresOnlyActualTreeTrunkLogWrites`: exercises chunk attachment use, schema, log filtering and duplicate-position idempotency.
  - `k5PlayerGrownTwoByTwoSpruceTreeIsNotTagged`: grows a real 2×2 spruce from saplings using the mapped sapling growth method, verifies logs were produced, and verifies no natural marker was added.
- The initial Actions compile failure was limited to the missing `CustomTestMethodInvoker.invokeTestMethod` override. It was corrected before the successful run above; it is not the status of the final probe commit.

### Limits and remaining technical gates

- The current marker observes `TreeFeature` trunks in natural biome-decoration scope. It does not yet prove that the active placed feature is one of the approved **vanilla** tree features; a modded feature using `TreeFeature` could be marked in this diagnostic. PR #81 excludes modded sources, so production K5 must add a verified vanilla-feature identity allowlist or another source-level discriminator before consuming these markers.
- The natural-worldgen GameTest invokes biome decoration and checks hook scope, but it does not assert that a deterministic natural tree was successfully placed and then positively marked. The attachment test calls the diagnostic recorder directly. Add a deterministic natural placed-feature fixture before treating positive provenance end-to-end as verified.
- The codec-backed persistent attachment was set/read during GameTest, but disk save/reload and chunk unload/reload were not exercised. Those lifecycle tests remain required before production K5.
- No survival block-break consumption, creative rejection, structure exclusion fixture, old-chunk behavior, vanilla species matrix, piston/explosion behavior, or K5 threshold test was implemented or run. All remain outside this diagnostic-only probe.
- Local `genSources` and Gradle invocations remain blocked by the workspace's network policy; GitHub Actions is the successful mapped-source/runtime environment. This is an environment limit, not a failed current Actions verification.

**Readiness:** the verified entry paths and diagnostic mixins compile and run in Actions. This is not approval to enable K5 progress. The remaining gates are vanilla feature identity, a positive deterministic natural-tree marker test, and persistent chunk save/unload/reload validation, followed by a separate approved production implementation.


## 2026-10-13 GitHub Actions source-resolution breakthrough and next gate

- Dedicated workflow: `.github/workflows/k5-mapped-sources-probe.yml`.
- **PASS:** [K5 Mapped Sources Probe run #38338849034](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38338849034), commit `415cb0d9e995f55390ed118079fe2699b5f5ff58`. The `Resolve mapped sources` step (`./gradlew genSources --stacktrace --no-daemon`) completed successfully on GitHub-hosted Ubuntu with Java 25. Diagnostic log artifact upload also succeeded.
- This **supersedes the earlier statement that source resolution is globally blocked**. The earlier local sandbox remains network-blocked. A passing genSources step **does not** verify natural tree hooks, persistence, or K5 gameplay.
- **Next implementation gate:** Obtain actual generated mapped source/signatures from the GitHub Actions runner. The current workflow uploads the command log, **not the generated source files**. If local access remains blocked, add a narrowly scoped source-inspection CI step that outputs only verified relevant class/method descriptors and/or a small inspection report artifact; do not upload proprietary/full Minecraft sources or redistribute them. Inspect Fabric API hook sources and mapping correctness before coding.
- Once verified, implement an isolated **no-credit** worldgen-versus-sapling diagnostic probe and targeted GameTests, run them on Actions, capture results, and only then assess production K5 readiness.
- Keep PR #82 Draft; do not alter PR #79-#81, enable K5 progress, merge PRs, or start Stage 2-C.
- At the time of this addendum, **K5-specific runtime probe and tests remain NOT IMPLEMENTED / NOT RUN**.

## 2026-10-13 final technical validation

This review re-fetched PR #82 at code head `989d63e30f6c13d3e5a45aece9ef7ba77f6f5830`, PR #80 at `1a17f8e77792ab1b106d92ac03fe10c86227d599`, and PR #81 at `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86`. PR #81's approved section still defines K5 as 200 naturally world-generated vanilla tree log blocks directly mined by a Survival player. No authority conflict was found. No K5 progress or unlock path was enabled.

### Gate results

| Gate | Status | Evidence and remaining limit |
|---|---|---|
| Deterministic positive natural-tree detection | **UNVERIFIED** | The updated GameTest calls the mapped `ChunkGenerator.applyBiomeDecoration` on a real test-world chunk. The test requires actual `TreeFeature` trunk callbacks, a nonempty position attachment, and each marked position to resolve to a current log block. It passed in the successful Actions attempt below. A fixed seed/biome fixture is not pinned, so the positive case is not yet deterministic across independently created test worlds. |
| Vanilla feature identity | **UNVERIFIED** | The diagnostic now resolves actual registry identifiers through `Registry.getKey`: the top-level `PlacedFeature` must be a registered `minecraft` entry and the trunk callback must resolve to the exact vanilla `minecraft:tree` feature identity, and the feature implementation must be `TreeFeature`. Missing registry keys fail closed. The GameTest exercises vanilla identity, a mod feature, an unknown key, a spoofed `minecraft:custom_tree` feature key, and a non-tree predicate case; the positive natural worldgen test uses live registry lookups. A custom/test-registered `TreeFeature` object and each vanilla large-tree variant have not been exercised through live registry generation. |
| Persistent position provenance | **UNVERIFIED** | The test verifies that changing/removing a marked block removes its marker and replacing that position with a player-placed log does not restore it. The test calls `ServerChunkCache.save(true)` and a test-only wrapper confirms `ChunkMap.save(ChunkAccess)` successfully saved a chunk carrying the marker. This exercises Minecraft's real save path; no GameTest has yet unloaded/reloaded the marked chunk or restarted the server and read the marker back from disk. |

The successful positive test is evidence that the current GameTest world generated and marked a real natural tree on that run. It does not close the determinism gate because the test does not set a fixed world seed and biome.

### Verified Minecraft 26.3 mapped APIs

Mapped-source Actions run [#38358418443](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38358418443) passed `genSources` and its focused signature/source inspection. Its 6,688-byte artifact contains only selected API signatures/source excerpts and logs; no Minecraft source archive was uploaded.

Verified declarations and call-path evidence:

- `ChunkGenerator.applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureManager): void` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V`.
- `FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos): boolean` — `(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z`. `ChunkGenerator.applyBiomeDecoration` invokes this method for its placed-feature entries.
- `TreeFeature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos): boolean` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z`.
- `TreeFeature.doPlace(WorldGenLevel, RandomSource, BlockPos, BiConsumer, BiConsumer, FoliagePlacer.FoliageSetter): boolean` — `(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;)Z`. The trunk callback writes through `WorldGenLevel.setBlock`.
- `Registry.getKey(T): Identifier` — `(Ljava/lang/Object;)Lnet/minecraft/resources/Identifier;`. This Minecraft 26.3 version uses the non-generic `Feature` interface and a concrete `TreeFeature`; it does not expose the older `Feature.TREE` constant or `ResourceKey.location()`. The initial compile exposed these mapping differences; the implementation was corrected to use the resolved registry API.
- `WorldGenLevel.setCurrentlyGenerating(Supplier<String>): void` is present, but its interface default is empty. The probe therefore uses the verified `FeaturePlacer` invocation and registry lookups rather than treating that string as authentication.
- Sapling source remains distinct: `SaplingBlock.advanceTree(ServerLevel, BlockPos, BlockState, RandomSource): void` calls `TreeGrower.growTree(...): boolean`, which calls `Feature.place` directly and does not use `ChunkGenerator.applyBiomeDecoration` or `FeaturePlacer.placeWithBiomeCheck`.
- Persistent save/unload source: `ServerChunkCache.save(boolean): void` calls `ChunkMap.saveAllChunks(boolean)`; `ChunkMap.scheduleUnload(long, ChunkHolder)` saves a chunk and invokes `ServerLevel.unload(LevelChunk)`; the mapped `ChunkMap.scheduleChunkLoad` path reads chunk data and parses `SerializableChunkData`. The test observes the `ChunkMap.save` write path at runtime, but has not yet tested reload or restart persistence.

The local `./gradlew genSources` attempt remains blocked before Gradle startup because the sandbox denies downloading the Gradle 9.6.0 distribution (`SocketException: Operation not permitted`). The hosted Actions source job completed successfully; this local restriction is not counted as a failed source inspection.

### Diagnostic implementation and persistence strategy

All new hooks remain in `src/gametest` and the GameTest-only Mixin configuration:

- The worldgen scope is attached to the verified `applyBiomeDecoration` invocation.
- Each `FeaturePlacer.placeWithBiomeCheck` call resolves the placed feature's identifier from `Registries.PLACED_FEATURE`. A stack-scoped ThreadLocal holds only the active identifier and is cleared in `finally`.
- The actual `TreeFeature` trunk callback resolves its feature identifier from `Registries.FEATURE`, checks the placed-feature namespace, requires the exact registered vanilla feature ID `minecraft:tree`, and requires the runtime source object to be a `TreeFeature`. No tree-shape or log-type heuristic grants provenance.
- Final log positions are stored as packed longs in the persistent chunk attachment `moreminecrafts:k5_natural_tree_probe_v1`, schema 1. Storage is chunk-local; there is no global position map.
- The test-only `Level.setBlock(BlockPos, BlockState, int, int): boolean` wrapper invalidates a marker after a successful state change. Empty chunk attachments are removed. This prevents a placed log at a consumed position from reusing an old marker.
- Ordinary sapling growth remains outside the biome-decoration / placed-feature scope.

Chunk lifecycle cleanup and persistence remain incomplete until an actual storage read/write round trip is tested. The next diagnostic step is to drive a marked, non-player-ticketed chunk through Minecraft's `ChunkMap` save, unload, and reload path in GameTest, then assert marker preservation. Repeat with a removed marker and a player-placed replacement, and add a two-process server restart check if the Gradle GameTest server supports reusing a world directory.

### Runtime and CI results

Full Fabric CI run [#38358418452](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38358418452), on probe code head `989d63e30f6c13d3e5a45aece9ef7ba77f6f5830`, passed on the successful retry:

- `./gradlew clean test build compileGametestJava runGameTest` completed with `BUILD SUCCESSFUL`.
- JUnit `test`, production build, GameTest compilation and runtime completed.
- GameTest server reported **all 68 required tests passed**.
- The first attempt failed one unrelated existing mob GameTest assertion (`CustomMobGameTests.allRosterEntriesRegisterAttributesAndInstantiate`, catalog stat multiplier assertion); rerunning the build job passed. No unrelated gameplay code was changed.
- Dedicated-server and client-smoke both passed on the successful run [#38358418452](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38358418452).
- The earlier mapping compile failure on run [#38356783314](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38356783314) was caused by using obsolete feature constants and a guessed `ResourceKey.location()` accessor. It was corrected using the inspected 26.3 registry types and `Registry.getKey`.

GameTests present in the probe source cover: positive natural-decoration trunk writes and actual log positions; feature namespace/type predicate cases; chunk attachment value storage; real 2×2 spruce sapling growth remaining unmarked; marker invalidation on block replacement; no marker restoration after placing a log at the same position; and successful passage through Minecraft's actual `ChunkMap.save` method with a marked chunk. The attachment-value test is a fixture test only and is not treated as persistence evidence. The save assertion is not a readback test.

### Final readiness

**Recommendation: NOT READY for production K5 implementation.** The positive path is observed in runtime, the mapped source path is verified, and the no-credit identity gate is implemented diagnostically. However, the task's deterministic fixture, live synthetic/modded feature identity test, and actual chunk unload/reload persistence gates are not all closed. No progress counter, Knowledge unlock, player-break collector, or production Mixin was added. The approved K5 semantics remain unchanged.


### Final branch-head CI confirmation (2026-10-13)

On documentation/probe head `577de9df5bfcb6d6acad8f89291e9dfeb26ef149`, focused mapped-source workflow [#38358732806](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38358732806) completed successfully: `genSources` and selected signature inspection passed. The only uploaded source artifact was `k5-mapped-api-report` (6,687 bytes); no Minecraft source archive was uploaded.

Fabric CI [#38358732792](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38358732792) completed successfully across build, dedicated-server smoke, and client smoke. The build job ran `./gradlew clean test build compileGametestJava runGameTest`; all **68 required GameTests** passed, including the K5 diagnostic GameTests. This confirms compilation and runtime suite execution on that head, but does not close the three acceptance gates above: the positive generation test is not pinned to deterministic inputs, a live synthetic non-vanilla `TreeFeature` is not tested, and persisted chunk data is not read back after unload/reload or server restart. Final K5 technical recommendation remains **NOT READY**.


### Deterministic natural-tree positive gate — current result (2026-10-13)

Authority was re-fetched before this update: PR #80 remains at `1a17f8e77792ab1b106d92ac03fe10c86227d599`; PR #81 remains at `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86` and its approved rule is 200 naturally world-generated vanilla tree logs directly mined by a Survival player. PR #82 is Draft. Its live head was `948b8ace84b023f9d128dc6a1cdff3f878cc7b8d`, 44 commits ahead of the supplied prior reference `3b439bf5f32cdd9c375bfc9ea64aa58ebd5387a6`; the intervening PR changes are confined to the K5 probe workflow, documentation, and GameTest-only source/configuration. No authority conflict was found.

The deterministic fixture in `K5NaturalTreeProbeGameTests.k5DeterministicNaturalForestDecorationMarksRealVanillaTrunkLogs` invokes the verified `ChunkGenerator.applyBiomeDecoration` path. Its pinned inputs are server seed `0`, dimension `minecraft:overworld`, a `NoiseBasedChunkGenerator` using `minecraft:overworld` noise settings, `minecraft:forest` as the fixed biome, chunk `[0,0]`, and a fixed dirt/grass platform at Y 1–3. The biome's registered tree placement is `minecraft:trees_birch_and_oak_leaf_litter`.

Fabric CI run [#38363866846](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38363866846) executed the build/test/GameTest pipeline on head `948b8ace84b023f9d128dc6a1cdff3f878cc7b8d`. The runtime trace showed the natural biome-decoration scope, 28 real `TreeFeature` trunk callbacks, source ID `minecraft:oak_bees_0002_leaf_litter`, and callback positions whose final states included actual `minecraft:oak_log` blocks. However, the marker count was **zero**. The current predicate requires feature ID `minecraft:tree`, so the observed registered vanilla feature is rejected. The positive GameTest therefore failed at the required marker assertion. This is evidence of deterministic natural generation, but it does not prove marker creation.

The deterministic positive test was executed twice across the recorded recent runs; **zero executions passed** the positive marker assertions. Run [#38363603434](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38363603434) failed the earlier assertion that treated a dirt root-callback position as a trunk log. On the updated assertion, run #38363866846 independently reported final callback block states and confirmed oak logs, but still recorded zero markers. No successful repeated runtime run is claimed.

On run #38363866846, 70 of 72 required GameTests passed. The exact failed tests were:

- `k5DeterministicNaturalForestDecorationMarksRealVanillaTrunkLogs`: natural marker set remained empty.
- `k5PlayerGrownTwoByTwoSpruceTreeIsNotTagged`: its fixture did not produce the expected real tree trunk, so this negative-control test failed before proving its marker assertion.

The identity predicate GameTest and in-memory chunk-attachment value test passed, as did the other 68 required tests. Bonemeal-grown and player-placed negative-control GameTests are not present. No K5 progress or unlock code is enabled.

**Gate status: BLOCKED.** The fixture deterministically invokes real natural biome decoration and observes actual vanilla tree trunk logs, but the existing feature-identity filter prevents a positive marker. Changing that filter would address the separate vanilla feature-identity gate, which was explicitly excluded from this task. Persistence remains unverified; this update does not change its status. The required negative controls and a passing regression run are also outstanding.

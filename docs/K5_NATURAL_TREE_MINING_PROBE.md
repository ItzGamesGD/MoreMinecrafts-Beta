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

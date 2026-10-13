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
- **GitHub Actions on pre-probe PR #82 head `6d49d87c74faadbd2938043e55870eca56759dda`:** run [#38326823336](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38326823336), Fabric CI, was observed with `build` successful (workflow command includes `clean test build compileGametestJava runGameTest`) and `server-smoke` successful. `client-smoke` was still in progress at the time of this update. These results are baseline CI only; they do not exercise probe code, because none was added.
- **Natural-tree probe/GameTest: NOT IMPLEMENTED / NOT RUN.** The checklist requires stopping when source resolution fails, so natural feature and block-break hooks were not guessed. No K5 condition update, counter increment, or unlock path was added or enabled.
- **Probe scenarios (natural tree species, sapling growth, placement, structures, piston/explosion, persistence): UNVERIFIED.**
- **Readiness:** blocked pending a Java 25 environment that can resolve the pinned Loom/Minecraft sources. Resume at checklist item 1, record mapped signatures, then implement the diagnostic-only probe and its GameTests before claiming K5 technical readiness.

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


## 2026-10-13 GitHub Actions source-resolution breakthrough and next gate

- Dedicated workflow: `.github/workflows/k5-mapped-sources-probe.yml`.
- **PASS:** [K5 Mapped Sources Probe run #38338849034](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38338849034), commit `415cb0d9e995f55390ed118079fe2699b5f5ff58`. The `Resolve mapped sources` step (`./gradlew genSources --stacktrace --no-daemon`) completed successfully on GitHub-hosted Ubuntu with Java 25. Diagnostic log artifact upload also succeeded.
- This **supersedes the earlier statement that source resolution is globally blocked**. The earlier local sandbox remains network-blocked. A passing genSources step **does not** verify natural tree hooks, persistence, or K5 gameplay.
- **Next implementation gate:** Obtain actual generated mapped source/signatures from the GitHub Actions runner. The current workflow uploads the command log, **not the generated source files**. If local access remains blocked, add a narrowly scoped source-inspection CI step that outputs only verified relevant class/method descriptors and/or a small inspection report artifact; do not upload proprietary/full Minecraft sources or redistribute them. Inspect Fabric API hook sources and mapping correctness before coding.
- Once verified, implement an isolated **no-credit** worldgen-versus-sapling diagnostic probe and targeted GameTests, run them on Actions, capture results, and only then assess production K5 readiness.
- Keep PR #82 Draft; do not alter PR #79-#81, enable K5 progress, merge PRs, or start Stage 2-C.
- At the time of this addendum, **K5-specific runtime probe and tests remain NOT IMPLEMENTED / NOT RUN**.

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
| Server mutation | `Level.setBlock` | `(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z` | Invalidate changed positions |
| Worldgen mutation | `WorldGenRegion.setBlock` | `(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z` | Invalidate later worldgen replacements |
| Direct mining | `ServerPlayerGameMode.destroyBlock` | `(Lnet/minecraft/core/BlockPos;)Z` | Attribute one successful Survival mining action |

These names and signatures were checked against locally resolved Minecraft 26.3 sources and the mapped-source workflow. The existing `ServerPlayerGameModeMixin` wraps the method once, retaining the enchantment break scope. The four old test-only worldgen and mutation wrappers were removed; test observations now listen to production hooks and use the production attachment, leaving one provenance store.

The mining wrapper snapshots valid position provenance before the original method removes the block. It credits only when the result is true, the block is air and the *stored marker itself* is absent afterward. This extra postcondition matters because mapped `destroyBlock` returns true even if `Level.removeBlock` returned false. Fabric cancellation is exercised by a runtime test. `Level.removeBlock` routes through `Level.setBlock`; successful changes consume the marker before progress is recorded. Later replacement logs at the same position cannot inherit it. The server's single world thread serializes competing break calls.

## Tests and evidence

- Deterministic natural fixture: seed 0, `minecraft:forest`, `minecraft:overworld` noise settings, chunk `[0,0]`, genuine `applyBiomeDecoration`, runtime-observed `minecraft:oak_bees_0002_leaf_litter`, 23 final markers in the local run. The test then directly mines actual marked logs and checks 0→1, independent players, no double count, Creative/Spectator exclusions, 199→200 unlock, cancelled break, indirect removal and replacement.
- Existing sapling, bonemeal and player-placement GameTests additionally mine their ineligible logs and assert zero K5 progress.
- Separate GameTests exercise vanilla piston movement, vanilla explosion destruction and the vanilla player respawn path.
- Existing probe lifecycle test checks save, real unload/reload, invalidation, replacement, cross-chunk and cross-dimension isolation with the production attachment.
- The dedicated `K5 Production Restart` workflow filters one GameTest across two JVM processes sharing one world. Process A saves an actual natural marker, consumed/replaced markers and a fixed-UUID player's progress; process B loads the chunk and player data through Minecraft APIs. The small test manifest contains expected positions and is not gameplay storage. This is a headless GameTest server restart; the separate Fabric CI job runs dedicated-server and client smoke checks.
- JUnit checks the 200 target, counter cap, codec round trip and existing UI progress row. No client packet can grant K5; serverbound networking remains limited to existing non-K5 requests.

Local development evidence before the final CI run: `genSources` succeeded; `compileGametestJava runGameTest` passed 78/78 before the respawn and worldgen-mutation additions; the focused respawn test passed 1/1; the two-process restart write and read phases each passed 1/1. These local runs are intermediate evidence only. Record final-HEAD CI results in the Draft PR before recommending review.

## Limits and release gate

The vanilla feature allowlist is pinned to observed Minecraft 26.3 TreeFeature registry keys. Unknown configured keys fail closed. A mod deliberately registering new content in the `minecraft` namespace while reusing an allowed TreeFeature is outside the approved non-malicious-mod threat model. Real world generation beyond the tested vanilla feature variants, high-density chunk performance and modpack compatibility need release validation. Build and GameTest success do not by themselves certify release readiness.

The server-restart test must report its process boundary separately from same-process chunk unload/reload. Any CI failure must be diagnosed and rerun on the final commit. Do not merge until the required checks pass and the Draft PR is reviewed.

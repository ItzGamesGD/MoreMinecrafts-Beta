# K5 natural-tree mining technical probe — execution checklist

Status: **NOT EXECUTED**. This document prepares the isolated runtime probe and records no passing tests.

## Authority

- Design: PR #81, commit `be0b6c7874a5c78e4e87fee4431c2a1c78ae3c86`, first section of `docs/K5_NATURAL_TREE_PROVENANCE_DESIGN.md`.
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

- Branch and checklist created from PR #80.
- **genSources: NOT RUN**
- **GameTests: NOT RUN**
- **K5 runtime probe: NOT IMPLEMENTED**
- **CI: NOT RUN**

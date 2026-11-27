# Stage 23-A Batch 3 Implementation Manifest

- **Branch:** `codex/stage23a-batch3-20261015`
- **Base:** PR #94 branch `codex/stage23a-integrated-research-20261015`, exact HEAD `5e48deacbeed95783902f2b71e04e2ae25005606`
- **Target:** PR #94's branch; do not merge automatically.
- **Manifest state:** the manifest and authority were published before gameplay changes. `.08` is implemented; `.06`, `.07`, and `.11` remain deferred by their documented stop gates.
- **Selected IDs:** `v3.research.06`, `.07`, `.08`, and `.11`.

## Base and ownership verification

- Live GitHub metadata verified PR #94 remains open, Draft, unmerged, and at head `5e48deacbeed95783902f2b71e04e2ae25005606`. Its base is PR #91's branch at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; that SHA is an ancestor of #94.
- PR #93 is an alternative sibling integration from the same PR #91 base, not a dependency. Its current head is `bfb2b35b6fc5ad7cd495293d2dfd3f3afdb57865`; it includes alternative `.19/.55/.67` implementations and tests. Its changed paths include the shared authority/audit, `ResearchRewards.java`, `moreminecrafts.mixins.json`, fishing and movement/XP handlers, and the general mapped-source workflow. It does not change `KnowledgeRecipeCraftCountMixin.java`, existing crafting runtime, or item-tag paths. Batch 3 will avoid the shared reward registry and mixin-registration files, reuse the already-registered craft-result mixin, and keep its source probe in a separate workflow. No PR #93 code is imported.
- No newer Stage 23-A integration PR or branch supersedes #94. Existing #94 functionality (`.19`, `.55`, `.67`, inherited #90/#91 handlers and tests) remains owned by the base. No existing code, PR, or branch is reset, rewritten, or merged.
- `ResearchRewards.java`, the current authority document, canonical audit, general mapped-source workflow, and mixin registration are shared paths. Batch 3 will not edit the reward registry or mixin registration; its `.08` hook uses the direct server-side purchased level from the existing authoritative progression attachment and extends the already-registered `KnowledgeRecipeCraftCountMixin.java`. The exact `.08` transaction-hook region is reserved to this batch. Furnace runtime files are unassigned because `.06/.07/.11` stop below.

## Authority and invariant

The controlling design authority is `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, including the Batch 3 amendment committed with this manifest. The research purchase gates, maximum levels, graph, costs, prerequisites, and player progression attachment remain unchanged. The amendment records the exact user-approved effect, attribution, classification, persistence, and conservation rules.

## Selection and implementation status

| ID | Approved effect | Planned server hook and data ownership | Manifest status |
|---|---|---|---|
| `v3.research.06` Fuel Saving | Fuel-use reductions 8.5/17/25.5% at L1/L2/L3; fixed-point per-furnace remainder; preserve fuel/container behavior and compose with multiplier efficiency. | `AbstractFurnaceBlockEntity.serverTick` plus portable-furnace processing; attributed UUID and remainder in persistent block-entity state. | **DEFERRED.** No production authoritative progression lookup by offline attributed UUID exists. The only `PlayerList.loadPlayerData` usage restores a synthetic `ServerPlayer` in a restart GameTest. The portable furnace is a non-persistent session menu without a block entity. |
| `v3.research.07` Precision Smelting | Mineral-only extra-product chance 7.5/15/22.5/30/37.5% at L1–L5; one roll at base-product creation, no recursion, independent from Auto Smelt. | Existing recipe-completion path for each audited furnace family; attributed player's paid level; conservative recipe/tag classifier; lossless output commit. | **DEFERRED** for the offline-attribution and portable-furnace persistence blockers. No output or random-roll code will be added while ownership is unresolved. |
| `v3.research.08` Precision Crafting | Approved material-class chances and L2/L4 interpolation; choose exactly one rarest eligible consumed slot, lowest slot index on ties; preserve at most one unit after a successful roll. | Server-side `ResultSlot.onTake`; select from the pre-consumption `CraftingInput`, then intercept its one-unit `CraftingContainer.removeItem` call for the chosen slot. The actual mapped bytecode owner is `CraftingContainer`; 26.3 source confirms remainder calculation precedes consumption, and `CraftingMenu.quickMoveStack` calls `onTake` only after successful transfer. Successful shift-click preservation refreshes its result at the menu return boundary. No persistent state. | **IMPLEMENTED; Java 25 automated validation passed on code commit `72b8da58162e2cf4df585224799f4ebf34d8e1fd`.** Server-only paid level, exact approved class probabilities, one transaction-scoped preservation, vanilla remainders, and `.09` integration. Live verification remains outstanding. |
| `v3.research.11` Faster Smelting | Processing multipliers 110/120/130% at L1/L2/L3; fractional cooking progress, persisted and composable with `.06` and existing furnace multipliers. | `AbstractFurnaceBlockEntity.serverTick` plus portable-furnace processing; attributed UUID and fractional progress in persistent block-entity state. | **DEFERRED** for the offline-attribution and portable-furnace persistence blockers. |

### Furnace stop-gate findings

The baseline has no production UUID-to-`ResearchProgressState` lookup for offline players. `KnowledgeRuntime.state` and `ResearchRewards.level` both require a live `ServerPlayer`. The restart GameTest loads saved player NBT into a synthetic player; this is test-only behavior, not an approved production lookup. Shared vanilla furnace processing has no player parameter; output collection is too late for `.07` creation-time rolls. Do not use the placer, nearest player, output collector, a synthetic player restored through the test path, or another purchaser as a substitute.

The portable furnace runs only while an online player has its menu open. Its `PortableContainer` and cooking state are session-local; the opener item and a block entity do not hold the required persistent state. Supporting it under the approved permanent block-entity storage requirement would need a separately approved storage path. Accordingly `.06`, `.07`, and `.11` remain unimplemented and DATA_ONLY until both blockers are resolved.

## `.08` ownership and persistence plan

No parallel branch contained a `.08` implementation at ownership review. The dedicated Java 25 Batch 3 crafting probe run `38709897738` passed on code commit `72b8da58162e2cf4df585224799f4ebf34d8e1fd`; it verifies source ordering, the `CraftingContainer.removeItem` bytecode owner, and the `quickMoveStack` completion boundary. An earlier generic probe had a faulty assertion about the inherited method's declaration owner; the dedicated check corrects it. Expected Batch 3 files:

| File/path | Batch 3 ownership |
|---|---|
| `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md` | Record only the approved Batch 3 rules. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRuntime.java` (or a narrow extension to existing crafting runtime) | Classify ingredients, calculate chance, select one eligible slot, and apply only after a confirmed server craft. No saved state. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeRecipeCraftCountMixin.java` | Add the narrow paid-level-gated `ResultSlot.onTake` transaction hook; preserve `.09` Wood Processing and existing knowledge counters. Never change preview result creation. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeCraftingMenuMixin.java` | Batch 3 owns only the quick-move transaction boundary needed to refresh a preserved recipe preview after vanilla finishes clearing the transferred result. Keep the existing `.09` result augmentation path intact. |
| `src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/*.json` | Explicit vanilla classes, extension tags for modded/data-driven materials, and a compressed research-token exclusion tag; ambiguous and unique ingredients remain excluded. |
| `src/test/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRulesTest.java` | Deterministic probability/interpolation, class ordering, registration, and purchase-gate tests. |
| `src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeAcquisitionGameTests.java` | Extend the already-registered `CustomTestMethodInvoker` with actual server crafting transactions, normal and shift-click, remainders, full inventories, repeated extraction, and `.09` composition. This avoids the concurrently changed GameTest metadata file. |
| `.github/workflows/k5-stage23a-batch3-crafting-probe.yml` | Isolated mapped-source probe for the successful result-transfer hook, one-unit consumption call, remainder ordering, and shift-click completion. |
| `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md` | Recount from actual evidence; mark `.08` PARTIAL only after its handler and focused tests exist; leave three deferred IDs DATA_ONLY. |

The `.08` implementation adds no player/world persistence and does not change progression serialization. The three deferred furnace IDs add no runtime code, mixin registration, save format, or tests in this batch.

## Focused acceptance matrix

| ID | Required automated coverage |
|---|---|
| `.06` | Deferred. Future tests: levels 0–3, long-run fractional convergence, fuel/bucket/container behavior, multiplayer/offline attribution, interaction ordering, hopper-only processing, save/reload, break/replacement, and multiplier composition. |
| `.07` | Deferred. Future tests: levels 0–5/boundaries, mineral tags and explicit extension, ambiguous/food/stone/glass/recycling exclusion, one roll per base unit, creation-time/no recursion, full output conservation, and Auto Smelt independence. |
| `.08` | All five paid levels and classes; intermediate interpolation; purchase gating; rarest-class then lowest-slot selection; unique exclusions; one preserved unit maximum; normal and shift-click completion; failed/full-inventory extraction; remainders; repeated extraction; preview/client isolation; `.09` composition. Drive actual result hooks, not only math helpers. Successful shift-click preservation queues one result refresh at the `CraftingMenu.quickMoveStack` return boundary, after vanilla clears the transferred result slot. |
| `.11` | Deferred. Future tests: levels 0–3/timing boundaries, fractional progress, save/reload/restart, level changes mid-recipe, `.06` composition, multiplier furnaces, and no reset/loss/duplicate output. |

## Audit and validation

The canonical audit is `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md`, one row per 87 IDs. The PR #94 baseline had 60 DATA_ONLY / 25 PARTIAL / 0 COMPLETE / 0 MISSING / 2 BLOCKED. After implementing `.08`, the 87-row recount is 59 DATA_ONLY / 26 PARTIAL / 0 COMPLETE / 0 MISSING / 2 BLOCKED. `.08` remains PARTIAL pending live validation; no node is COMPLETE solely from automation.

## Validation evidence

All listed runs tested code commit `72b8da58162e2cf4df585224799f4ebf34d8e1fd` using Java 25:

| Check | Result | Run |
|---|---|---|
| `./gradlew clean test build compileGametestJava runGameTest`; 101 GameTests | Passed | [Fabric CI #780](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897588) |
| Fabric server smoke | Passed | [Fabric CI #780](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897588) |
| Fabric client smoke | Passed | [Fabric CI #780](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897588) |
| Minecraft 26.3 crafting completion/remainder and mapped-owner probe | Passed | [Batch 3 crafting probe #13](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897738) |
| General Minecraft 26.3 mapped-source probe | Passed | [Mapped Sources #201](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897732) |
| Two-process world/player persistence regression | Passed | [Production Restart #107](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38709897582) |
| Tag JSON validation and `git diff --check` | Passed locally | — |

Live gameplay validation remains outstanding for multiplayer crafting attribution, tag extensions, remainder compatibility, and client/server synchronization. The audit keeps `.08` PARTIAL and the furnace nodes DATA_ONLY. Merge readiness remains **NOT READY**. PRs #88–#94 are unchanged and unmerged; Batch 3 does not implement Stage 23-B reserved features.

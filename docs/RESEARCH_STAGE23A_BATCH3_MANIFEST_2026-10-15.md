# Stage 23-A Batch 3 Implementation Manifest

**Branch:** `codex/stage23a-batch3-20261015`  
**Base:** PR #94 branch `codex/stage23a-integrated-research-20261015`, exact HEAD `5e48deacbeed95783902f2b71e04e2ae25005606`  
**Target:** PR #94's branch; do not merge automatically.  
**Manifest state:** the published manifest and authority commits contain no gameplay edits; publication precedes all gameplay changes.  
**Selected IDs:** `v3.research.06`, `.07`, `.08`, and `.11`.

## Base and ownership verification

- Live GitHub metadata verified PR #94 remains open, Draft, unmerged, and at head `5e48deacbeed95783902f2b71e04e2ae25005606`. Its base is PR #91's branch at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; that SHA is an ancestor of #94.
- PR #93 is an alternative sibling integration from the same PR #91 base, not a dependency. A live `git ls-remote` and fetch found its branch at `f899101a9c56079e9e970b812346f4ff202dfabc` (parent `75d008ccc1ef8a98f6782a24126034664895e2ef`); its newest commit only registers the Fisheries Study GameTest. Its cumulative changes overlap general authority/audit and `ResearchRewards`/mixin-registration files but do not touch the `.06/.07/.08/.11` furnace or crafting runtime paths. No PR #93 content is imported.
- No newer Stage 23-A integration PR or branch supersedes #94. Existing #94 functionality (`.19`, `.55`, `.67`, inherited #90/#91 handlers and tests) remains owned by the base. No existing code, PR, or branch is reset, rewritten, or merged.
- `ResearchRewards.java`, the current authority document, canonical audit, and mixin registration are shared paths. Batch 3 adds only its own IDs and rows; it does not replace existing registrations or handlers. The `.08` transaction-hook region is reserved to this batch after mapped-source confirmation. Furnace runtime files are unassigned because `.06/.07/.11` stop below.

## Authority and invariant

The controlling design authority is `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, including the Batch 3 amendment committed with this manifest. The research purchase gates, maximum levels, graph, costs, prerequisites, and player progression attachment remain unchanged. The amendment records the exact user-approved effect, attribution, classification, persistence, and conservation rules.

## Selection and implementation status

| ID | Approved effect | Planned server hook and data ownership | Manifest status |
|---|---|---|---|
| `v3.research.06` Fuel Saving | Fuel-use reductions 8.5/17/25.5% at L1/L2/L3; fixed-point per-furnace remainder; preserve fuel/container behavior and compose with multiplier efficiency. | `AbstractFurnaceBlockEntity.serverTick` plus portable-furnace processing; attributed UUID and remainder in persistent block-entity state. | **DEFERRED.** No production authoritative progression lookup by offline attributed UUID exists. The only `PlayerList.loadPlayerData` usage restores a synthetic `ServerPlayer` in a restart GameTest. The portable furnace is a non-persistent session menu without a block entity. |
| `v3.research.07` Precision Smelting | Mineral-only extra-product chance 7.5/15/22.5/30/37.5% at L1–L5; one roll at base-product creation, no recursion, independent from Auto Smelt. | Existing recipe-completion path for each audited furnace family; attributed player's paid level; conservative recipe/tag classifier; lossless output commit. | **DEFERRED** for the offline-attribution and portable-furnace persistence blockers. No output or random-roll code will be added while ownership is unresolved. |
| `v3.research.08` Precision Crafting | Approved material-class chances and L2/L4 interpolation; choose exactly one rarest eligible consumed slot, lowest slot index on ties; preserve at most one unit after a successful roll. | Server-side completed result transfer. `ResultSlot.onTake` is the candidate success boundary; inspect Minecraft 26.3 mapped `ResultSlot`, `CraftingContainer`, and `CraftingMenu` call order before selecting the exact consumption hook. No persistent state. | **SELECTED; hook gated on mapped-source confirmation.** If source cannot establish successful per-craft completion, consumption order, and remainder behavior for repeated shift-clicks, stop `.08` without gameplay code. |
| `v3.research.11` Faster Smelting | Processing multipliers 110/120/130% at L1/L2/L3; fractional cooking progress, persisted and composable with `.06` and existing furnace multipliers. | `AbstractFurnaceBlockEntity.serverTick` plus portable-furnace processing; attributed UUID and fractional progress in persistent block-entity state. | **DEFERRED** for the offline-attribution and portable-furnace persistence blockers. |

### Furnace stop-gate findings

The baseline has no production UUID-to-`ResearchProgressState` lookup for offline players. `KnowledgeRuntime.state` and `ResearchRewards.level` both require a live `ServerPlayer`. The restart GameTest loads saved player NBT into a synthetic player; this is test-only behavior, not an approved production lookup. Shared vanilla furnace processing has no player parameter; output collection is too late for `.07` creation-time rolls. Do not use the placer, nearest player, output collector, a synthetic player restored through the test path, or another purchaser as a substitute.

The portable furnace runs only while an online player has its menu open. Its `PortableContainer` and cooking state are session-local; the opener item and a block entity do not hold the required persistent state. Supporting it under the approved permanent block-entity storage requirement would need a separately approved storage path. Accordingly `.06`, `.07`, and `.11` remain unimplemented and DATA_ONLY until both blockers are resolved.

## `.08` ownership and persistence plan

No parallel branch contains a `.08` implementation. Expected Batch 3 files:

| File/path | Batch 3 ownership |
|---|---|
| `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md` | Record only the approved Batch 3 rules. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java` | Add `.08` descriptor only; preserve existing mappings. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRuntime.java` (or a narrow extension to existing crafting runtime) | Classify ingredients, calculate chance, select one eligible slot, and apply only after a confirmed server craft. No saved state. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeRecipeCraftCountMixin.java` and, only if mapped source requires, a dedicated result-slot mixin | Add the narrow paid-level-gated success hook; preserve `.09` Wood Processing and existing knowledge counters. Never change preview result creation. |
| `src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/*.json` | Explicit vanilla material classes and extension tags for modded/data-driven materials; ambiguous and unique ingredients remain excluded. |
| `src/main/resources/moreminecrafts.mixins.json` | Register only a needed `.08` adapter once; retain all current entries. |
| `src/test/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRulesTest.java` | Deterministic probability/interpolation, class ordering, registration, and purchase-gate tests. |
| `src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingGameTests.java` and GameTest registration | Actual server crafting transactions, normal and shift-click, remainders, full inventories, repeated extraction, and `.09` composition. |
| `.github/workflows/k5-mapped-sources-probe.yml` | Extend probe for `ResultSlot`, `CraftingContainer`, and `CraftingMenu` call order/remainder behavior before implementing the hook. |
| `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md` | Recount from actual evidence; mark `.08` PARTIAL only after its handler and focused tests exist; leave three deferred IDs DATA_ONLY. |

The `.08` implementation adds no player/world persistence and does not change progression serialization. The three deferred furnace IDs add no runtime code, mixin registration, save format, or tests in this batch.

## Focused acceptance matrix

| ID | Required automated coverage |
|---|---|
| `.06` | Deferred. Future tests: levels 0–3, long-run fractional convergence, fuel/bucket/container behavior, multiplayer/offline attribution, interaction ordering, hopper-only processing, save/reload, break/replacement, and multiplier composition. |
| `.07` | Deferred. Future tests: levels 0–5/boundaries, mineral tags and explicit extension, ambiguous/food/stone/glass/recycling exclusion, one roll per base unit, creation-time/no recursion, full output conservation, and Auto Smelt independence. |
| `.08` | All five paid levels and classes; intermediate interpolation; purchase gating; rarest-class then lowest-slot selection; unique exclusions; one preserved unit maximum; normal and shift-click completion; failed/full-inventory extraction; remainders; repeated extraction; preview/client isolation; `.09` composition. Drive actual result hooks, not only math helpers. |
| `.11` | Deferred. Future tests: levels 0–3/timing boundaries, fractional progress, save/reload/restart, level changes mid-recipe, `.06` composition, multiplier furnaces, and no reset/loss/duplicate output. |

## Audit and validation

The canonical audit is `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md`, one row per 87 IDs. PR #94 baseline is 60 DATA_ONLY / 25 PARTIAL / 0 COMPLETE / 0 MISSING / 2 BLOCKED. After only `.08` has runtime code and focused tests, expected counts are 59 DATA_ONLY / 26 PARTIAL / 0 COMPLETE / 0 MISSING / 2 BLOCKED; recalculate from every row rather than imposing those totals. Live validation remains pending, so no node is COMPLETE solely from automation.

After implementation, use Java 25 and run `./gradlew clean test build compileGametestJava runGameTest`, mapped-source checks, server/client smoke, two-process persistence checks, mixin/resource validation, audit recount, and `git diff --check`. Report unrun checks as NOT RUN. Merge readiness remains **NOT READY** pending live gameplay checks. Do not modify PRs #88–#94, merge any PR, or implement Stage 23-B reserved features.

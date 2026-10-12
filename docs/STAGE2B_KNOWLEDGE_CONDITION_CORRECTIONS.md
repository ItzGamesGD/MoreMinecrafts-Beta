# Stage 2-B Knowledge acquisition condition corrections

Baseline: Stage 2-A PR #79 head `04fd5780658f847f74be6beda76754e08a0b5520`. This branch changes acquisition observation only. Knowledge rewards, Research prices, V3 prerequisite graphs, saved Knowledge flags, and unrelated progression behavior are out of scope.

The existing `ResearchProgressState` attachment remains the durable counter store. Distinct observations use the existing `condition.<key>.seen.<id>` keys, so repeat observations do not add progress and save/reload preserves both progress and deduplication. Previously unlocked Knowledge is never cleared. The human-approved 2026-10-12 K5/K8/K14 decisions below supersede the earlier provisional interpretations in this document.

## Save compatibility

K8 distinct block IDs and K14 cumulative tool quantity use the existing player attachment and do not replace or migrate its serialized counter map. Existing unlocked flags and saved counters remain readable; new K8/K14 progress is stored using the same attachment mechanism. Vanilla per-player crafted-tool statistics remain available for K14 reconciliation after restart. Furnace and brewing pending-output provenance use version-1 block-entity codec fields; older saves with no such field load with no pending provenance and cannot award credit for unrecorded outputs. Chunk data without K5 provenance is intentionally ineligible; no log origin is inferred from old chunks. Crossbow-firework in-flight attribution is a runtime map, so a save/reload between launch and impact loses that attribution. No save-format migration was added for the existing Knowledge catalog or unlock flags.

| Node | Stage 2-B status | Event and evidence / limitation |
|---|---|---|
| K5 거목 생태 | BLOCKED_BY_DESIGN | Approved semantics: acquire 128 logs from naturally world-generated 2×2 large-tree features. Player-grown, player-placed, and ordinary small-tree logs are excluded. Provenance must be recorded before progress is granted. See the provenance investigation below; no heuristic was substituted. Existing progress and unlocks remain unchanged. |
| K8 암석 분류 | VERIFIED_RUNTIME | Approved semantics: mine eight distinct allowlisted rock types; successful server-side player mining is required. The explicit roster is in the K8 roster section below. The post-break observer rejects creative breaks, unlisted blocks, canceled breaks, and repeats via persistent distinct keys. The registered action-level GameTest exercises eight successful survival breaks, a duplicate, a creative break, an excluded block, and a failed air break. |
| K12 고온 제련 | IMPLEMENTED_NOT_VERIFIED | Requires a player's vanilla crafted-blast-furnace statistic and taking a non-empty product from a blast furnace output. Merely attempting to use a furnace, placing one, or crafting it without collected output does not qualify. The output-taking player must have a recorded blast-furnace craft. |
| K14 제작 기술 | IMPLEMENTED_NOT_VERIFIED | Approved semantics: craft five tools total, with repeats counting. The result-slot observer counts output quantity for the five approved vanilla tool families across each registered vanilla material variant; creative crafting, loot, pickup, unrelated equipment, and modded tools are excluded. The roster and persistence strategy are below. The GameTest checks direct collector attribution for two players, but does not run a real recipe/result-slot transaction. |
| K18 물류 공학 | IMPLEMENTED_NOT_VERIFIED | Counts successful server-side hopper item placements after the block-item placement reports success; item-use statistics and failed placement attempts do not count. Each successful placement is one event. |
| K19 자동화 원리 | PARTIAL | Counts successful placements of the existing eligible set: redstone dust, repeater, comparator, observer, piston, and sticky piston. Redstone dust has its own placement hook. A runtime GameTest covers one successful redstone-dust placement; the remaining five placement paths are not action-level verified. |
| K22 토양 관리 | IMPLEMENTED_NOT_VERIFIED | Counts successful server-side hoe actions only when the clicked block is farmland after the action. Failed or no-op interactions do not count. |
| K23 관개 기술 | IMPLEMENTED_NOT_VERIFIED | Counts a successful server-side water-bucket action only when it places source water within vanilla farmland hydration reach (horizontal Manhattan distance four, farmland at the same level or one block below the water). The condition target remains one. |
| K25 가축 사육 | IMPLEMENTED_NOT_VERIFIED | Counts a species after an Animal interaction succeeds, consumes offered breeding food, and the animal accepts that food. The entity registry ID is the persistent distinct token. Pre-interaction callback attempts do not count. |
| K30 식품 가공 | IMPLEMENTED_NOT_VERIFIED | Counts distinct food item IDs with actual crafted-item statistics; crafted result-slot output also records its item ID directly. Repeated crafting or cooking of the same type does not add progress. Cooking-stat behavior and every food-component item remain CI/runtime validation items. |
| K34 궁술 이해 | IMPLEMENTED_NOT_VERIFIED | Credits hostile deaths to the player owner of a direct arrow whose persisted weapon item is a bow. Holding a bow at death is irrelevant. |
| K35 석궁 구조 | IMPLEMENTED_NOT_VERIFIED | Credits hostile deaths to the player owner of a direct arrow whose persisted weapon item is a crossbow, including arrow variants. Crossbow fireworks use temporary launch attribution because their projectile has no native weapon field; this path is not reload-persistent and remains unverified. |
| K38 투척 무기 | IMPLEMENTED_NOT_VERIFIED | Credits a hostile death only when the direct damage entity is a thrown trident whose stored weapon is a trident and whose owner is a ServerPlayer. Holding a trident is irrelevant. |

## Validation limits

The CI `:test` task passed on head `b74788048e43a69e71a2044847fa9d49623b3dea`, including `KnowledgeConditionSemanticsTest`, `KnowledgeEventRulesTest`, and K14 quantity, roster, exclusion, and attachment-codec tests. These unit checks do not prove that every Fabric action hook executes in a live server. The same CI run completed the full build and GameTest tasks: 36 of 36 required GameTests passed.

The existing K19 action-level GameTest proves one successful redstone-dust placement only. The K8 action-level GameTest passed through the installed `PlayerBlockBreakEvents.AFTER` hook: eight unique allowlisted blocks advanced progress and unlocked K8; repeat, creative, excluded-block, and failed-air-break cases did not grant extra progress. K14 has a runtime `ResultSlot.onTake` hook, JUnit quantity/roster/codec cases, and a GameTest for direct collector attribution between two players. That GameTest does not execute `ResultSlot.onTake`; identical-tool and mixed-family real crafting remain **IMPLEMENTED_NOT_VERIFIED**. K12 crafting/output attribution, K18 placement, K22/K23 world transitions, K25 species attribution, K30 cooking, K34–K38 real damage-source attribution, and all Stage 2-A collectors remain **IMPLEMENTED_NOT_VERIFIED** pending action-level evidence.

## Stage 2-A integration and regression audit (2026-10-12)

Verified remote sources before integration: PR #79 `204f706cb2ffd1bf60fa950f201a33446b55850f`, PR #80 `cba72162ecd5c80f1101d9a2024d6c3647acc1a9`, common ancestor `04fd5780658f847f74be6beda76754e08a0b5520`. The authority index, Research/Knowledge tier design, approved node-tree spec, and node-tree manifest have identical Git blob IDs at all three commits. The PR bodies and status documents were treated as implementation claims, not authority changes. PR #79 remains untouched.

The integration retains Stage 2-B's observers and merges Stage 2-A's later stabilization commits. In `KnowledgeRuntime.recordCondition`, Stage 2-A's `KnowledgeConditionProgress.advance` now caps the shared counter at the maximum target for every matching catalog entry; each entry is then evaluated independently. This preserves K31/K32, K60/K62, and K63/K69 progress without disturbing Stage 2-B's K5 pause, K8 mining, K14 quantity behavior, placement/feeding/food observers, or projectile kill attribution. `KnowledgeEventCollectors` retains both stages' methods. The furnace output hook invokes both K12's blast-furnace collector and K11's provenance collector. The projectile launch hook retains K40 special-ammunition observation and K34/K35 origin capture. The mixin list retains all Stage 2-A and Stage 2-B registrations, with no duplicate names. Furnace and brewing version-1 provenance codecs and save/load hooks are present in the combined tree. Unlock announcements and silent join reconciliation remain in `KnowledgeRuntime`.

Local `./gradlew clean test build compileGametestJava runGameTest` could not start because Gradle reported `Could not determine a usable wildcard IP for this machine`; sandbox security was not changed. GitHub Actions run [38273820852](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38273820852), for code head `b74788048e43a69e71a2044847fa9d49623b3dea`, passed the `:test`, `build`, `compileGametestJava`, and `runGameTest` tasks. Its runtime log records 36/36 required GameTests passed. Server smoke passed. Client smoke was still running at this document update; the final audit below will record its completed result and the documentation-only final-head run. The other Stage 2-A collector actions, remaining Stage 2-B acquisition actions, furnace/brewing output save-reload scenarios, and notification delivery are not exercised by those 36 GameTests. Compilation and startup are not action-level verification for other conditions.

The final-tree dedicated server smoke reached registry/common initialization and `Done` readiness. A local `runClient` attempt timed out after 65 seconds during Gradle's `downloadAssets` task, before the client process started. Client startup requires separate CI evidence; this local attempt does not verify rendering or UI behavior.

The audit found a K19 double count in Stage 2-B's preexisting redstone path: Minecraft 26.3 registers `Items.REDSTONE` as a `BlockItem`, so one successful placement reached both `KnowledgeBlockPlacementMixin` and `KnowledgeItemUseMixin`. A new action-level GameTest placed one dust item and failed on the expected single increment before the fix. `KnowledgeBlockPlacementMixin` now leaves dust to the existing `KnowledgeItemUseMixin` observer; other eligible block placements continue through the BlockItem observer. This is a narrowly scoped correction to retain K19's intended one-event-per-placement behavior. Other K19 placement scenarios remain unverified.

Crossbow firework attribution remains temporary: launch records the player's UUID against the live `FireworkRocketEntity` only when the `shootProjectile` callback says crossbow and the projectile owner is that shooter. A hostile death is credited only when the direct damage entity is that same projectile and the player is still online. Vanilla's firework damage source uses the rocket as direct entity, but the weak map has no reload codec; save/reload between launch and impact loses kill attribution. This origin state is intentionally in-memory and is not serialized. A launch-to-kill test, explosion splash attribution test, disconnect test, and projectile save/reload test are still needed. Bow and crossbow arrows instead use the arrow's persisted weapon and owner; tridents use the direct thrown trident's stored weapon and owner. K40 special-ammunition launch observation is separate from kill credit.

## Approved K8 rock roster

All selected blocks are registered vanilla blocks in Minecraft 26.3. MoreMinecrafts registers ore blocks, adamantium debris, and refined metal blocks, but no additional geological rock block eligible for K8. Ore and metal blocks are not counted as rock types. Eligibility is a fixed object allowlist, not the vanilla stone tag, so structure blocks, crafted masonry, and unrelated blocks cannot enter by tag membership.

| Registry ID | Origin | Survival acquisition | Rock classification |
|---|---|---|---|
| `minecraft:stone` | Vanilla | Mine exposed Overworld stone with a pickaxe; without Silk Touch it drops cobblestone. | Common natural rock substrate. |
| `minecraft:granite` | Vanilla | Mine Overworld granite deposits with a pickaxe. | Naturally generated igneous rock variant. |
| `minecraft:diorite` | Vanilla | Mine Overworld diorite deposits with a pickaxe. | Naturally generated igneous rock variant. |
| `minecraft:andesite` | Vanilla | Mine Overworld andesite deposits with a pickaxe. | Naturally generated igneous rock variant. |
| `minecraft:tuff` | Vanilla | Mine underground tuff deposits with a pickaxe. | Naturally generated volcanic rock. |
| `minecraft:deepslate` | Vanilla | Mine below the Overworld deepslate transition with a pickaxe. | Naturally generated deep-crust rock. |
| `minecraft:calcite` | Vanilla | Mine calcite from amethyst-geode formations with a pickaxe. | Naturally generated crystalline mineral rock. |
| `minecraft:dripstone_block` | Vanilla | Mine dripstone cave deposits with a pickaxe. | Naturally generated cave limestone. |
| `minecraft:basalt` | Vanilla | Mine basalt deltas or naturally generated basalt formations with a pickaxe. | Naturally generated volcanic rock. |
| `minecraft:blackstone` | Vanilla | Mine Nether terrain and bastion formations with a pickaxe. | Naturally generated Nether rock. |
| `minecraft:netherrack` | Vanilla | Mine exposed Nether terrain with a pickaxe. | Common naturally generated Nether rock. |
| `minecraft:end_stone` | Vanilla | Mine exposed End-island terrain with a pickaxe. | Common naturally generated End rock. |

`KnowledgeEventCollectors.rockTypeMined` is called only from the successful Fabric player-break `AFTER` event, with creative mode excluded. It stores the block registry ID through the existing distinct-condition counter. The existing flat player attachment persists the counter and deduplication keys; no existing value is cleared.

## Approved K14 tool roster and persistence

Allowed vanilla material prefixes are `wooden`, `stone`, `copper`, `iron`, `golden`, `diamond`, and `netherite`; allowed families are `pickaxe`, `axe`, `shovel`, `hoe`, and `sword`. The exact candidate registry IDs are:

```text
minecraft:{wooden,stone,copper,iron,golden,diamond,netherite}_{pickaxe,axe,shovel,hoe,sword}
```

The full candidate list is:

```text
minecraft:wooden_pickaxe, minecraft:wooden_axe, minecraft:wooden_shovel, minecraft:wooden_hoe, minecraft:wooden_sword
minecraft:stone_pickaxe, minecraft:stone_axe, minecraft:stone_shovel, minecraft:stone_hoe, minecraft:stone_sword
minecraft:copper_pickaxe, minecraft:copper_axe, minecraft:copper_shovel, minecraft:copper_hoe, minecraft:copper_sword
minecraft:iron_pickaxe, minecraft:iron_axe, minecraft:iron_shovel, minecraft:iron_hoe, minecraft:iron_sword
minecraft:golden_pickaxe, minecraft:golden_axe, minecraft:golden_shovel, minecraft:golden_hoe, minecraft:golden_sword
minecraft:diamond_pickaxe, minecraft:diamond_axe, minecraft:diamond_shovel, minecraft:diamond_hoe, minecraft:diamond_sword
minecraft:netherite_pickaxe, minecraft:netherite_axe, minecraft:netherite_shovel, minecraft:netherite_hoe, minecraft:netherite_sword
```

At server start, the roster contains only IDs from that list that are actually registered in the target version; missing IDs are excluded. The JUnit registry test checks each family/material candidate against the runtime registry. No MoreMinecrafts or other namespace item can match. Bows, crossbows, tridents, maces, brushes, armor, and other equipment are outside the roster.

`KnowledgeRecipeCraftCountMixin` observes `ResultSlot.onTake` after the player successfully takes a non-empty crafting result. Its server player receives the actual output stack count; K14 uses the existing shared monotonic counter capped at its target. Creative crafting is excluded. Existing `Stats.ITEM_CRAFTED` values for eligible tools continue to reconcile into the counter as a restart/migration source; vanilla item-crafted statistics are per-player and saved by Minecraft. The persistent `ResearchProgressState` counter preserves already-recorded progress and unlocks. JUnit covers quantity, mixed families, the full candidate roster, exclusions, and attachment codec round-trip. The runtime collector GameTest verifies two-player attribution directly but does not exercise the installed result-slot hook, so crafting-hook behavior remains `IMPLEMENTED_NOT_VERIFIED`.

## K5 provenance investigation and safety gate

Approved semantics: acquire 128 logs originating in naturally world-generated 2×2 large-tree features. Player-grown trees, player-placed logs, ordinary small trees, leaves, and tree-species-only observations do not qualify. The old generic log statistic is not used, and existing saved counters/unlocks are left intact.

The current source has no tree-generation provenance on blocks or dropped items. `PlayerBlockBreakEvents` supplies the player and broken state, but not whether a log came from natural world generation. Tree placement APIs can be invoked by both natural biome-feature generation and player-grown sapling growth; a later 2×2 shape or nearby leaf check cannot distinguish them. The relevant source and target Fabric API therefore expose no non-invasive event that identifies both the cause (natural generation versus sapling growth) and the configured 2×2 trunk placer. A reliable implementation would need a generation-scope mixin around biome decoration, capture of the specific successful large-trunk feature's placed log positions, persistent per-chunk provenance, one-time consumption on player break, provenance transfer to actual item drops, and player-only item pickup attribution. This crosses worldgen, chunk persistence, block-drop, and item-pickup lifecycles and cannot be validated here; K5 remains **BLOCKED_BY_DESIGN**, with no heuristic credit.

Minimal alternative design for approval: add a versioned persistent chunk attachment containing only packed positions for logs emitted by approved 2×2 trunk placers while natural biome decoration is active. On a successful non-creative player break, consume each source position once and tag only the matching log drops with a compact provenance count. Consume that count only when a player picks up the item; never credit hopper transfer, nearby players, or untagged old-chunk logs. Retain item/entity metadata across unload/reload and test normal drops, Silk Touch, split stacks, re-placement, duplicate pickup, and two-player attribution before enabling K5. This task does not implement that invasive pipeline without runtime verification.


Remaining action-level scenarios: K11 completed ore smelt, partial output take, manually inserted output, hopper extraction, and furnace chunk/world reload; K36 actual shield damage reduction; K40 tipped-arrow and firework launch plus ordinary/spectral exclusion; K48 successful/empty barter and disconnected offerer; K59 successful archaeology spawn count; K63/K64/K65/K66/K69/K70 brewed output, distinct potion types, strengthened count, inserted output, partial take, and brewing chunk/world reload. For Stage 2-B: K12 player craft plus collected blast-furnace output and negative cases; K18/K19 successful/failed placement and one increment per action; K22/K23 real farmland and hydration transitions; K25 accepted species feeding and item consumption; K30 crafted/cooked food deduplication after save/reload; K34/K35/K38 real hostile kills with bow, crossbow arrow/firework, trident, and incorrect-weapon/owner controls. Multiplayer announcement recipients, gamerule behavior, first-ever guidance, translated presentation, reconnect silence, prior unlocked flags, saved counters, and research/UI interaction across save/reload remain unexercised by the current tests.

## Final condition status summary

`VERIFIED_RUNTIME` requires an action-level test of the installed hook in an actual Minecraft server/GameTest. Compilation and smoke startup alone do not qualify. For this head, K8 is action-level verified; K19 is partial because only one of its six approved placements has action-level coverage.

| Status | Conditions |
|---|---|
| `BLOCKED_BY_DESIGN` | K5 — generation provenance requires the invasive pipeline above. |
| `IMPLEMENTED_NOT_VERIFIED` | K11, K12, K14, K18, K22, K23, K25, K30, K34, K35, K36, K38, K40, K48, K59, K63, K64, K65, K66, K69, K70. The hooks are implemented, but action-level verification is still outstanding. |
| `PARTIAL` | K19. Only successful redstone-dust placement is covered at action level; repeater, comparator, observer, piston, and sticky-piston paths remain unverified. |
| `VERIFIED_RUNTIME` | K8. The Minecraft GameTest drove successful server player block breaks through the registered Fabric event and checked unique progress, duplicates, creative mode, exclusions, and failed breaks. |

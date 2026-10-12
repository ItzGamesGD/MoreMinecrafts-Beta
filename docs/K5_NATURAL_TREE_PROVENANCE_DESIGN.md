# K5 Natural Giant Tree Provenance Architecture

**Status:** Design only; no production code, migration, or gameplay change is included.  
**Ancestry baseline:** PR #80 head 096d9c825422a2fc1220b88ea6ee75967e7fa1bf  
**Runtime dependencies:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25.  
**Remote review snapshot (2026-10-13):** PR #80 head 1a17f8e77792ab1b106d92ac03fe10c86227d599; PR #81 head 98f71908f87b6bc15a6832d9eac71eaaa891b4f4. GitHub compare reports common ancestor 096d9c825422a2fc1220b88ea6ee75967e7fa1bf, with PR #81 seven commits ahead and 37 behind PR #80. PR #81's diff remains documentation-only.

## Approved semantics

K5 unlocks when a player acquires 128 distinct log units from naturally world-generated 2×2 giant-tree features. It counts acquired log units, not blocks broken or trees discovered. Player-grown trees, player-placed logs, ordinary small natural trees and unknown-origin logs are ineligible. Credit requires the receiving player to be in Survival when units enter their inventory.

Progress is player-specific. The same source log may count once per player; returning or repeatedly transferring it cannot add credit again to that player. Legitimate Survival transfers and ordinary stack/storage/drop/pickup operations must preserve source IDs and eligible quantity.

No geometry, leaf, biome or species-only heuristic proves origin. Previously unlocked Knowledge remains unlocked.

### User-approved amendments

**2026-10-12 — player-specific credit.** The same source identity can count once for each player; only that player's exact redemption history deduplicates it. This supersedes any global first-owner or globally redeemed-token model.

**2026-10-13 — ordinary Survival threat scope.** Administrator command duplication, external mod inventory manipulation, malicious component injection by privileged actors, and arbitrary nonstandard item duplication are excluded security requirements. Do not build exhaustive anti-cheat, global custody or global ownership tracking for these cases. Normal Survival transfers and exact per-player deduplication remain required.

### Explicit security non-goals

- Detecting duplication performed through administrator commands.
- Defending against privileged component injection, malicious server operators or external mods that rewrite inventories.
- Tracking every physical copy in a global ledger or enforcing global token ownership/redemption.
- Scanning the world or retroactively assigning provenance to old chunks.

Under this threat model, a cryptographic HMAC and persistent world secret are unnecessary. Ordinary Survival players cannot normally create the registered provenance component, and server inventory state is authoritative. The component remains server-issued by the generation/drop pipeline; version and codec validation fail closed. A signature would not prevent copying a valid component. Privileged/hostile-mod protection would require separate approval.

## Recommendation

Retain the hybrid provenance architecture:

1. During a positively identified natural biome tree-feature invocation, record each final qualifying trunk-log position and one server-minted source UUID per log in a persistent chunk attachment.
2. At the matching log's Survival drop or approved block-movement path, consume that position marker once and transfer its UUID into a provenance data component on the corresponding log unit.
3. Preserve component UUIDs through ordinary Survival stack splits, merges, item drops/pickups, menus, chests and hoppers.
4. On successful inventory receipt by a Survival player, credit only source UUIDs absent from that player's exact persistent redemption set.
5. Maintain no global redemption, first-owner, custody or ownership ledger.
6. Keep exact player history bounded by remaining pre-unlock progress. At 128, retain the Knowledge unlock; no post-unlock history is required unless statistics/reset behavior is later approved.
7. Missing, corrupt or unknown-version provenance fails closed. Do not infer provenance for old chunks or untagged items.

The UUID is a source identity, not a cryptographic security token. Server-only issuance after consuming a recorded natural-generation marker is sufficient for the approved ordinary Survival threat model. No HMAC/world secret is recommended. Exact per-player history remains persistent; normal supported stack operations must conserve identity quantity.

## Investigated APIs and evidence

The repository's current PR #80 head was re-fetched before review; the dependency declarations in gradle.properties and build.gradle at 1a17f8e77792ab1b106d92ac03fe10c86227d599 pin the following. The repository pins Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25, and JUnit 5.11.4 in [gradle.properties](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/1a17f8e77792ab1b106d92ac03fe10c86227d599/gradle.properties) and [build.gradle](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/1a17f8e77792ab1b106d92ac03fe10c86227d599/build.gradle). Mod metadata limits the supported game version to 26.3.

Minecraft source evidence inspected:
- Mojang version metadata for 26.3 and the official client jar; SHA-1 e877b6a07acd633fb3bb475002175cec036e7b87 matches the version manifest. Relevant Mojang-named classes include ChunkGenerator, FeaturePlacer, PlacedFeature, Feature, TreeFeature, TreeGrower, SaplingBlock, TreeFeatures, DarkOakTrunkPlacer, GiantTrunkPlacer, MegaJungleTrunkPlacer, Block, ItemEntity, ItemStack, Inventory, AbstractContainerMenu, and Slot. The listed method descriptors are from the class files in that jar and CFR 0.152 output.
- Loom source generation was attempted against the pinned build but Gradle could not resolve the Loom 1.17.21 plugin marker/artifact in this environment. Therefore this review did not claim Loom-generated named sources or a successful Mixin remap/build.

The baseline K5 audit in [Stage 2-B Knowledge condition corrections](STAGE2B_KNOWLEDGE_CONDITION_CORRECTIONS.md) records that current player-break callbacks lack natural-origin provenance and explicitly rejects matching trunk shape or leaves as a substitute.

Fabric API source was inspected at tag 0.161.0+26.3:

- [ServerChunkEvents.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/api/event/lifecycle/v1/ServerChunkEvents.java) provides server chunk load/unload events. These describe lifecycle, not what placed a block.
- [PlayerBlockBreakEvents.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/api/event/player/PlayerBlockBreakEvents.java) supplies level, player, position, pre-break block state, and optional block entity. AFTER is called after a successful server-side break; canceled breaks use CANCELED. It supplies no feature cause or drop stack.
- [AttachmentRegistry.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-data-attachment-api-v1/src/main/java/net/fabricmc/fabric/api/attachment/v1/AttachmentRegistry.java) exposes persistent attachments with codecs.
- [AttachmentType.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-data-attachment-api-v1/src/main/java/net/fabricmc/fabric/api/attachment/v1/AttachmentType.java) names chunks as supported attachment targets and notes ProtoChunk-to-LevelChunk copying. The attachment API does not list ItemStack as a target, so item provenance needs a custom codec-backed item data component or equivalent integration.

The project uses the existing persistent player attachment for condition progress. It has no K5 provenance implementation at the baseline.

### Natural feature source and tree configuration

A later break callback cannot distinguish a natural tree from an identical sapling-grown tree. The inspected Fabric events provide no generation-cause callback, but Minecraft's 26.3 source has a promising call-path boundary.

The official Mojang 26.3 client jar SHA-1 e877b6a07acd633fb3bb475002175cec036e7b87 matches Mojang's version manifest. CFR 0.152 decompilation of that jar verifies these Mojang-named paths and descriptors:
- ChunkGenerator.applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureManager):void iterates biome placed features and calls FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos):boolean.
- FeaturePlacer.placeWithBiomeCheck calls the configured Feature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos):boolean.
- SaplingBlock.randomTick(BlockState, ServerLevel, BlockPos, RandomSource):void and performBonemeal(ServerLevel, RandomSource, BlockPos, BlockState, BonemealSource):void both invoke advanceTree(ServerLevel, BlockPos, BlockState, RandomSource):void. TreeGrower.growTree(ServerLevel, ChunkGenerator, BlockPos, BlockState, RandomSource):boolean resolves a configured feature and directly calls Feature.place. It bypasses ChunkGenerator.applyBiomeDecoration and FeaturePlacer.placeWithBiomeCheck in the inspected code.
This verifies distinguishable natural-biome and sapling source calls at source level, but not runtime Mixin remapping or all worldgen sources. Gradle could not resolve Loom 1.17.21, so Loom-generated mapped sources were unavailable; verify the descriptors in resolved bytecode and run a GameTest probe before implementation.

The vanilla configured 2×2 trunk placer configurations found in TreeFeatures are:
- DarkOakTrunkPlacer: DARK_OAK, PALE_OAK, PALE_OAK_BONEMEAL, and PALE_OAK_CREAKING.
- MegaJungleTrunkPlacer: MEGA_JUNGLE_TREE; it extends GiantTrunkPlacer.
- GiantTrunkPlacer: MEGA_SPRUCE and MEGA_PINE.
Their decompiled implementations place four trunk columns at a 2×2 base. Use an explicit feature-key + configured-placer + expected log-state allowlist; do not infer provenance from observed shape. PALE_OAK_BONEMEAL is a sapling/bonemeal feature reached through TreeGrower and is not eligible as natural provenance. The natural candidate feature keys are DARK_OAK, PALE_OAK, PALE_OAK_CREAKING, MEGA_JUNGLE_TREE, MEGA_SPRUCE, and MEGA_PINE, subject to registry-level runtime confirmation that their placed-feature entries invoke those configured features. Explicitly decide structure scope and modded opt-in before implementation.

Capture must be invocation-scoped. Buffer qualifying final log writes for one allowed natural feature call, discard them if placement fails, and persist only final surviving trunk logs after success. Unknown feature keys, placers, state providers, or output states fail closed.

### Drop and acquisition boundary

The decompiled Block.dropResources overload with descriptor (BlockState, Level, BlockPos, BlockEntity, Entity, ItemStack):void invokes loot and then Block.popResource(Level, BlockPos, ItemStack):void. Fabric PlayerBlockBreakEvents.AFTER is server-side success evidence with the pre-break state and no resulting drops. Use it for cleanup/validation only; correlate the exact source marker with matching log output at the server-side loot/item creation boundary before ItemEntity insertion. Creative/tool/loot conditions must be checked before issuing a token. The recommended rule is that a surviving source identity follows a qualifying natural log through Survival block movement and any Survival-world drop that produces that same log item, including piston/explosion/automation paths; credit still waits until a player legitimately receives it. Piston moves require moving the position marker. A destructive action that produces no matching log item invalidates the marker. Creative-mode paths and command-created items fail closed. This automation policy is a recommendation requiring approval.

ItemEntity.playerTouch(Player):void runs server-side and calls player.getInventory().add(ItemStack) before discarding the emptied entity. This is a direct pickup integration point. No general Fabric API item-pickup or inventory-acquired callback was found in Fabric API 0.161.0+26.3. Vanilla menu transfers route through AbstractContainerMenu.doClick(int, int, ContainerInput, Player):void and moveItemStackTo(ItemStack, int, int, boolean):boolean; these are candidate vanilla-container hooks. Inventory.add(ItemStack):boolean has non-pickup callers, so it cannot by itself define eligible acquisition. Backpacks and modded inventories require specific integration adapters; generic support is not established.

Acquisition means the actual unique-token delta successfully moved into a Survival ServerPlayer inventory, including item pickup and supported menu extraction. Hopper movement between storage blocks is not player acquisition. Creative receipt earns no credit. The recommendation to invalidate provenance on Creative receipt is a policy proposal and remains unapproved. Furnace fuel use consumes a tagged log and must not pass its token to furnace output.

Item data components can be serialized with ItemStack and participate in stack equality, but that alone does not preserve per-unit identities. ItemStack.split(int):ItemStack uses copyWithCount(min(amount,count)) and then shrinks the source, so the full component value is copied to the split result unless a hook partitions token identities. ItemStack.isSameItemSameComponents(ItemStack,ItemStack):boolean requires equal component maps for ordinary merging; stacks with different provenance payloads will not naturally merge. Chest/menu and hopper paths therefore need verified split/merge behavior. A container that strips or reconstructs components loses provenance and fails closed.

## Architecture comparison

| Approach | Correctness and persistence | Stacking, automation, ownership, compatibility | Cost and decision |
|---|---|---|---|
| A. Generation-time block positions | Records origin before harvest in sparse persistent chunk data; old chunks naturally lack markers. Does not prove later acquisition. | Chunk lifecycle callbacks do not identify origin. Requires natural-feature capture and marker handling when the source log moves or drops. | Necessary first half, insufficient alone. |
| B. Provenance-bearing item identities | Carries each source UUID through item entities and ordinary transfers; enables acquisition credit. Cannot establish natural origin alone. | ItemStack is not an Attachment target. Custom components participate in stack matching. Different UUID lists need provenance-aware merges, and split copies the list unless explicitly partitioned. | Necessary second half, insufficient alone. |
| C. Hybrid positions, item UUIDs and player histories | Connects natural generation, the physical log item and each receiving player. Chunk and player data persist; item component follows ordinary transfers. | Crosses worldgen, chunk, drop, stack, inventory and save lifecycles. Vanilla merge hooks require careful quantity accounting. | Recommended; fits approved threat model without global ownership or anti-cheat. |

### Stack split/merge and container behavior

A provenance component participates in ItemStack component equality. Vanilla merges require matching component maps, so eligible stacks with different UUID lists will not ordinarily merge. The implementation must add a provenance-aware merge path for ordinary vanilla inventory/menu, item-entity and hopper merges without weakening unrelated component equality. The merge operation unions UUIDs only for units actually transferred. A split partitions UUIDs between result and remainder in the same quantities as moved item units. Pickup remainders retain only the IDs for units not inserted. Duplicate IDs never increase eligible quantity. If a vanilla route cannot preserve these invariants, it is not ready for K5 credit.

Administrator or mod-injected duplicates are outside the approved threat model. This design does not attempt global detection. Under ordinary Survival routes, operation hooks must not copy source IDs during splits, lose IDs during merges or credit a player twice. The same legitimate unit may still credit a second player once after transfer.

Vanilla chest/menu, hopper storage, item drops/pickups and player-to-player transfers must preserve the component. Hopper-to-container movement alone earns no progress; each receiving player's first successful Survival inventory receipt may earn credit. Nonstandard mod containers are unsupported until explicitly integrated. Furnace fuel consumption consumes the log and must not pass its UUID to output.

## Persistent schema proposal

All data is server-authoritative. Decode errors and unknown versions reject provenance; they never grant it.

### Chunk attachment, version 1

Identifier: moreminecrafts:k5_natural_tree_logs. Target: persistent chunk attachment via AttachmentRegistry.createPersistent.

Value is an immutable sparse record:

| Field | Type | Meaning |
|---|---|---|
| version | integer | Exactly 1; unknown values fail closed. |
| trees | list | Sparse feature records for this chunk. |
| feature_id | UUID | Unique successful natural feature invocation. |
| feature_key | namespaced ID, optional | Diagnostic feature identity, not sufficient alone. |
| placer_key | namespaced ID | Reviewed trunk-placer registry identity. |
| log_positions | sorted entries of packed position + source UUID | One persistent identity for each final surviving eligible log unit. |

Validate each position belongs to the chunk and still contains the expected log state. Remove or move the marker when the source log is replaced or moved; consume it exactly once when issuing its item UUID. Store no leaves, attempted writes, or scan results. Test attachment copying through ProtoChunk-to-LevelChunk conversion.

### Provenance item component, version 1

Identifier: moreminecrafts:k5_natural_log_provenance. Persistent fields: component schema version and a bounded list of source UUIDs, one per eligible log unit. Generate each UUID once when committing a qualifying natural tree feature's chunk provenance. Move the same UUID from the source-position record to the corresponding log item; do not mint a replacement at harvest.

The server issues and reads this component. No HMAC or world secret is needed under the approved threat model. Validate schema version, eligible log item type, list size no greater than item count, and UUID uniqueness. Missing, malformed or unsupported data contributes zero eligible units. This is correctness validation, not anti-cheat against privileged actors or external mods.

A stack may contain ordinary logs as well as eligible logs, so its UUID count may be less than item count. The number of distinct valid UUIDs is its eligible quantity. Splitting partitions UUIDs between result and remainder. Merging unions only identities of units actually transferred. Duplicate UUIDs never increase eligible quantity. A source-position record is consumed once before its UUID is placed on a Survival drop.

### Player-specific redemption history, version 1

Persist on the existing player progress attachment or a separately versioned attachment. Store the schema version and an exact set of source UUIDs already credited to this player while K5 is locked. On successful inventory receipt while the player is in Survival:
1. Validate component schema and eligible log item.
2. Compute distinct source UUIDs actually moved into the player's inventory, bounded by actual item units moved.
3. Ignore UUIDs already in this player's history.
4. Add unseen UUIDs and increment only this player's K5 progress by that count, capped at 128.
5. Keep the same UUIDs on items so other players may receive their own first credit.

Repeated receipt by the same player, including take/return/re-take, is idempotent. Reconnect, inventory movement and world reload preserve redemption history through the player attachment lifecycle.

### Exact storage bound

For a locked player starting with valid K5 progress p, at most min(127, 128 - p) new token identities can affect the achievement before it unlocks. Thus the exact set is bounded by 127 tokens per locked player, assuming progress is derived only from valid token acquisitions. The set can be stored as exact token values, optionally grouped by feature UUID with sorted source positions/bitsets; it is not a fixed-size probabilistic filter and has no eviction. At unlock, no later acquisition can add K5 progress, so progress-relevant history can be discarded while the permanent unlock flag remains.

This bound does not apply if the game later tracks post-unlock collection statistics, raises the target, or allows K5 progress to decrease/reset. Those features would require retaining more history or a new design. A nonzero legacy K5 counter without corresponding source-token history is also unresolved: its prior source identities cannot be reconstructed. See approval decisions.

### Storage and security feasibility

A locked player starting at zero needs at most 127 exact UUIDs before unlock, about 2 KiB raw at 16 bytes per UUID, plus codec overhead. At 100,000 players all reaching that cap, the raw upper bound is about 203 MiB; actual size scales with players and progress. No eviction is allowed while progress remains possible. Once K5 unlocks, history can be discarded if there is no approved post-unlock counter or reset.

A full stack of 64 eligible logs carries about 1 KiB of raw UUID payload before component/codec overhead. Chunk provenance needs a packed position plus source UUID per eligible block (about 24 raw bytes per record), plus feature metadata. Measure actual save size, packet/component sync size, split/merge cost and pickup latency.

No global issuance, custody or redemption ledger is required. A copied UUID cannot be counted twice by the same player. Administrator commands, malicious component injection, external mods that rewrite data and arbitrary nonstandard duplication are excluded. Ordinary vanilla Survival splits, merges and transfers remain in-scope and must conserve identity quantity.

HMAC is intentionally omitted: it adds secret persistence and rotation behavior for attacks outside the approved threat model, and does not stop copying a valid component. Version validation and fail-closed decoding remain necessary.

### Save compatibility

Previously unlocked Knowledge remains unlocked. The current Stage 2-B authority at PR #80 head 1a17f8e77792ab1b106d92ac03fe10c86227d599 says existing K5 saved counters and unlocks remain intact. This design draft's prior recommendation to reset incomplete legacy counters conflicts with that baseline; the 2026-10-13 instruction keeps legacy incomplete progress open for approval. Do not reset/migrate saved counters or enable credit for players with nonzero legacy K5 progress until that conflict receives an explicit decision.

New chunk positions without provenance, old item stacks without a component, and unknown-origin logs are ineligible; do not scan/migrate old chunks. Missing/corrupt/unknown-version chunk/component/player history fails closed. If player history is corrupt, disable additional K5 credit for that player rather than resetting its set. Any later schema migration must be explicit and versioned.

## Hook integration plan

The following descriptors are from Mojang-named class files in the official 26.3 client jar and CFR 0.152 output. They are not yet verified in Loom's runtime mapping namespace. Exact Mixin target strings, callback injection points and ordering remain unverified until mapped source generation/build and runtime probes succeed.

| Purpose | Class, method and descriptor | Verified behavior / implementation boundary |
|---|---|---|
| Natural biome tree call | ChunkGenerator.applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureManager):void; calls FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos):boolean | Natural biome feature loop exposes the PlacedFeature configuration. Scope capture to one approved tree feature call. |
| Tree feature placement | FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos):boolean; Feature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos):boolean; TreeFeature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos):boolean | Actual configured feature and success result are available. Runtime probe must identify the correct final log-write capture site. |
| Sapling exclusion | SaplingBlock.randomTick(BlockState, ServerLevel, BlockPos, RandomSource):void; advanceTree(ServerLevel, BlockPos, BlockState, RandomSource):void; performBonemeal(ServerLevel, RandomSource, BlockPos, BlockState, BonemealSource):void; TreeGrower.growTree(ServerLevel, ChunkGenerator, BlockPos, BlockState, RandomSource):boolean | Sapling/bonemeal directly reach TreeGrower/Feature.place and bypass natural biome FeaturePlacer.placeWithBiomeCheck in inspected source. |
| Persistent chunk data | Fabric AttachmentRegistry.createPersistent(Identifier, Codec):AttachmentType; ServerChunkEvents.Load(ServerLevel, LevelChunk, boolean); Unload(ServerLevel, LevelChunk) | Codec persistence and chunk lifecycle are available. AttachmentType docs identify ChunkAccess support and ProtoChunk-to-LevelChunk copying. Callbacks do not establish placement cause. |
| Break/drop source | PlayerBlockBreakEvents.AFTER(Level, Player, BlockPos, BlockState, @Nullable BlockEntity); Block.dropResources(BlockState, Level, BlockPos, BlockEntity, Entity, ItemStack):void; Block.popResource(Level, BlockPos, ItemStack):void | AFTER has no resulting drops. Correlate source marker to the matching item before ItemEntity insertion. |
| Item pickup | ItemEntity.playerTouch(Player):void; Inventory.add(ItemStack):boolean | Server-side playerTouch adds to Inventory before removing an emptied ItemEntity. Inventory.add has other callers and cannot alone define acquisition. Fabric API exposes no universal pickup/acquisition event in the pinned source tree. |
| Menu/inventory merge | AbstractContainerMenu.doClick(int, int, ContainerInput, Player):void; moveItemStackTo(ItemStack, int, int, boolean):boolean; ItemStack.split(int):ItemStack; isSameItemSameComponents(ItemStack, ItemStack):boolean | Vanilla menu paths use ItemStack split and component-aware matching. A split copies the full component map unless explicitly partitioned. Different component lists do not ordinarily merge. |
| Item entity merge | ItemEntity.areMergable(ItemStack, ItemStack):boolean; merge(ItemStack, ItemStack, int):ItemStack | Vanilla merge requires same components; merge copies/grows target and shrinks source. Provenance-aware union must accompany moved unit count. |
| Hopper/container transfer | HopperBlockEntity.tryMoveInItem(Container, Container, ItemStack, int, Direction):ItemStack; canMergeItems(ItemStack, ItemStack):boolean; addItem(Container, ItemEntity):boolean | Hopper transfers are non-player storage movement. Source removes one item; destination either sets or merges stacks. Vanilla merge also requires component equality. Hook transfer atomically; do not credit hopper movement. |
| Item persistence/components | ItemStack.MAP_CODEC / CODEC, DataComponentPatch, BuiltInRegistries.DATA_COMPONENT_TYPE | ItemStack codec persists the component patch and the built-in registry includes component types. Custom component registration/bootstrap under the resolved Fabric/Loom namespace remains to be verified. |

Fabric API source was fetched at tag 0.161.0+26.3:
- PlayerBlockBreakEvents.AFTER callback is after a successful server break and receives level/player/position/pre-break state/optional block entity, but no drops.
- ServerChunkEvents load and unload callbacks use ServerLevel and LevelChunk with a generated flag on load.
- AttachmentRegistry.createPersistent registers codec-backed data persistent across restarts; AttachmentType documents chunk targets and ProtoChunk conversion.
- A source-tree search found no general Fabric pickup or acquired-inventory callback. The direct vanilla pickup and menu paths require mixins or equivalent observers.

### Staged implementation plan

**Phase 0 — resolve approval gates.** Before enabling K5, settle incomplete legacy progress against Stage 2-B's unchanged-counter authority, and obtain decisions for structure trees, modded placers, Creative token handling, and Survival piston/explosion source scope. Preserve all already-unlocked Knowledge.

**Phase 1 — resolve mappings and establish a source probe.** In an environment with the pinned dependencies accessible, run Loom source generation and compilation, record the actual mapping namespace, confirm every descriptor and injection point, and add temporary runtime probes for biome generation versus random-tick/bonemeal growth. Stop on descriptor or target mismatch. No gameplay progress hook is enabled.

**Phase 2 — qualify and persist source positions.** Build a reviewed registry feature/placer/log-state allowlist. Capture final positions only from successful natural feature placement, assign one UUID per qualifying log, persist it in a versioned chunk attachment, and verify ProtoChunk conversion plus unload/reload. Measure generation overhead and attachment bytes.

**Phase 3 — transfer provenance to drops/moves.** Consume each source marker once when the marked log is harvested or follows an approved Survival movement/drop path. Attach the existing UUID to only the corresponding log item unit. Validate normal tools/loot and partial item output. No identity may be minted from shape or from a player-placed block.

**Phase 4 — implement ordinary stack conservation.** Add explicit provenance-aware operations for split and merge at verified vanilla sites: player inventory add/pickup, AbstractContainerMenu transfers, ItemEntity merging, HopperBlockEntity transfers and item drop/remainder handling. Different provenance sets must still merge when ordinary item rules permit; token UUID union/partition must match actual transferred units. Keep provenance component out of unrelated equality semantics where possible. Test the entire ordinary Survival operation graph. No admin/mod anti-cheat or global custody ledger.

**Phase 5 — player receipt and persistence.** Observe successful actual token delta entering a Survival player's inventory. Atomically add unseen UUIDs to that player's exact attachment set and increment progress once per UUID. Preserve item UUIDs for other players. Verify reconnect, death/copy semantics, save/reload and simultaneous player receipts.

**Phase 6 — compatibility, measurement and rollback.** Publish tested vanilla routes and explicit mod integrations, run the matrix below, measure costs, and gate enablement on all approval and runtime checks. Rollback disables new issuance/credit without clearing data or unlocked Knowledge; unknown provenance remains ineligible.

## Performance and memory

No benchmark was run. Expected work is sparse and proportional to qualifying tree logs and provenance-bearing item operations:
- Generation: one configuration check per candidate tree; record only final qualifying trunk-log positions.
- Chunk storage: about 24 raw bytes per position/source-UUID record, plus feature metadata.
- Item data: 16 raw bytes per eligible UUID; a full 64-log eligible stack carries about 1 KiB before codec overhead.
- Player history: at most 127 UUIDs per locked player starting at zero progress, no eviction.
- Transient state: one feature-scoped capture and bounded stack-transfer context; no retained chunks or global maps.

Measure generation median/p95 and allocation deltas with probes enabled/disabled; serialized chunk/item/player attachment sizes; 0/64/127-entry history save-load latency; stack split/merge, pickup and container transfer latency; and allocation under repeated ordinary operations.

## Abuse and edge cases

- Player-grown trees and player-built imitations receive no marker; shape is not proof.
- Small natural trees and unapproved feature/placer types are excluded.
- One source UUID is issued per recorded natural log position and transferred only through its corresponding log item.
- Per-player redemption is exact; the same UUID may credit multiple players once each. No global first owner or redemption exists.
- Ordinary Survival splits partition IDs; merges union IDs for transferred units without increasing eligible quantity; pickup remainder retains IDs for units not inserted.
- Ordinary Survival chest/menu/hopper storage and player-to-player dropping/pickup preserve provenance. Hopper movement alone grants no progress.
- Creative receipt grants no credit. Whether it destroys provenance remains an approval gate.
- Survival piston movement and matching Survival-world drops should preserve provenance under the current recommendation; confirm that scope before implementation.
- Furnace fuel use consumes the tagged log and does not pass its identity to furnace output.
- Old chunks/items without provenance remain ineligible. Corrupt/unknown-version chunk, item component or player history fails closed.
- Administrator command duplication, malicious privileged component injection, external mod inventory manipulation and arbitrary nonstandard duplication are explicitly outside the security scope.

## Test plan

These tests are proposed and not executed. Existing PR #80 CI does not verify K5 provenance.

### Deterministic unit tests

| Test | Expected assertion |
|---|---|
| Component codec | Versioned UUID-list codec round-trips; malformed version, excessive list, duplicate UUIDs and invalid item type fail closed. |
| Player history | Exact persistent insert/contains; duplicate UUID is idempotent; no eviction; maximum 127 new IDs from zero progress while locked. |
| Split/merge arithmetic | Split partitions UUIDs by moved units; merge unions only IDs from units actually transferred; eligible quantity never exceeds moved item count or unique UUID count. |
| Player independence | Same UUID is new for B after A claims it; repeat is old for A. |
| Threshold | 127 unique receipts remain locked; the 128th unlocks exactly once. |
| Partial/canceled receipt | Credit equals distinct eligible IDs actually inserted into the Survival inventory; failed/partial pickup credits no more than moved IDs. |
| Legacy progress | Exercise only the migration choice after approval; old source identities cannot be reconstructed from the prior counter. |

### Minecraft 26.3 runtime/GameTests

| Scenario | Runtime assertion |
|---|---|
| Natural 2×2 tree | Approved natural feature call commits one position marker/source UUID per final qualifying trunk log. |
| Player-grown 2×2, small natural tree, player-built imitation | No eligible markers. |
| Survival harvest/drop | Matching source position is consumed once; same UUID appears on exactly the corresponding log item unit. |
| Survival pickup | Credit follows successful inventory delta, including partial inventory capacity. |
| A receives 64 and transfers to B | Both receive 64 progress from the same UUIDs. |
| B returns logs to A; repeated A/B transfers | A gains zero on return; neither player gains again. |
| Partial transfers and splitting/merging | UUIDs partition/union with actual item quantities; no eligible ID is lost or credited twice. |
| Chest/menu and hopper | IDs survive chest extraction, hopper insertion/extraction and item-entity routes; hopper movement alone grants no credit. |
| Drop/pickup and concurrent multiplayer | Dropped stack retains IDs; server-thread receipts credit each player independently once. |
| Save/reload and chunk unload/reload | Chunk markers, item UUID components and player histories persist; absent old data does not gain provenance. |
| Threshold 127→128 | Unlock occurs only on the 128th distinct acquired source UUID. |
| Creative receipt | No progress; token retention/destruction behavior is tested only after policy approval. |
| Survival piston/explosion path | Source movement/matching drop only after source-scope approval. |
| Existing old chunks | Existing tree-shaped logs without recorded markers remain unknown-origin and ineligible. |
| Administrator/mod duplication | Explicitly outside required acceptance; no anti-cheat pass claim. |

Report deterministic and runtime results separately. Do not describe unexecuted scenarios as passed.

## Policy recommendations and approval gates

These are recommendations, not approved rules. Do not implement a policy-changing migration or eligibility expansion without an explicit decision.

1. **Legacy incomplete progress.** Preserve all unlocked Knowledge. Stage 2-B authority at PR #80 current head says existing K5 counters and unlocks remain intact, while this design's previous recommendation proposed resetting incomplete legacy counters. This conflict is unresolved. Old source UUIDs cannot be reconstructed, so exact dedup across the migration boundary is impossible. Approval is required before enabling K5 credit for any player with nonzero legacy progress. Until then, do not alter saved values.
2. **Structure-generated trees.** Current recommendation: exclude structure trees initially and count only approved biome-decoration tree features. A natural giant tree inside a structure would not count. Broader inclusion needs a separately verified source path and tests.
3. **Modded trees.** Current recommendation: explicit opt-in per supported mod/placer with source integration and runtime tests; unknown placers fail closed. No compatibility claim is made for unapproved mods.
4. **Creative provenance.** Credit only in Survival is approved. Current recommendation: invalidate a token when it enters a Creative player's inventory, so it cannot later count after returning to Survival. Decide whether Creative receipt should instead retain provenance without credit; do not silently strip it.
5. **Survival piston/explosion scope.** Current recommendation: preserve identity when a marked log is moved by a Survival piston or produces a matching Survival-world drop; later Survival inventory receipt may credit it. This adds hooks and eligibility surface. Confirm before implementation.
6. **Nonstandard containers.** Vanilla player inventory, chest/menu, item entity and hopper routes are in the required test scope. Nonstandard mod containers need an explicit integration decision; arbitrary external mod manipulation remains outside the threat model.

## Implementation acceptance criteria

- Resolved 26.3/Loom sources confirm target names/descriptors and runtime probes prove natural-versus-sapling source distinction.
- Explicit natural feature/placer/log-state allowlist identifies only approved natural 2×2 trees.
- Each qualifying source log position issues one server-minted UUID; matching Survival drops/moves transfer that UUID rather than create another.
- Exact per-player persistent history prevents repeat credit; the same UUID may count once for multiple players; no global first-owner/redemption ledger exists.
- Ordinary Survival split, merge, inventory, chest/menu, hopper, drop/pickup, player-transfer and save/reload routes conserve eligible quantity.
- Credit equals distinct UUIDs actually received into a Survival player's inventory; 128 threshold is unchanged.
- No HMAC, global custody ledger, privileged-command defense, hostile-mod anti-cheat or unbounded global map is introduced for excluded threats.
- Existing unlocked Knowledge is preserved. Legacy incomplete counters remain untouched until the stated approval conflict is resolved.
- Structure, modded, Creative-token and piston/explosion policies follow only approved decisions.
- Unit/runtime tests run on pinned dependencies, and results are accurately separated.
- Worldgen, chunk, item component, player history and transfer costs are measured.

## Verified versus unverified findings

**Verified from the pinned source artifacts:**
- Mojang 26.3 official client jar SHA-1 e877b6a07acd633fb3bb475002175cec036e7b87 matches the version manifest; CFR 0.152 inspection verified the Mojang-named natural biome decoration, tree grower/sapling, trunk placer, drop, item entity, stack, inventory, menu and hopper call paths/descriptors recorded above.
- The natural-biome feature path is distinct in source from sapling random-tick and bonemeal growth. Vanilla 2×2 configurations use DarkOakTrunkPlacer, MegaJungleTrunkPlacer and GiantTrunkPlacer on the listed feature configurations.
- ItemStack.CODEC persists a DataComponentPatch; split copies component data; component equality controls ordinary merges. ItemEntity pickup adds into player Inventory on the server. HopperBlockEntity transfers through one-unit remove/split and component-sensitive merge.
- Fabric API 0.161.0+26.3 source confirms persistent chunk attachments, ProtoChunk-to-LevelChunk copy documentation, chunk load/unload events, and successful break callbacks without drops. A general pickup/acquisition event was not found.

**Not verified:**
- Loom-generated/runtime mapped target names and Mixin descriptors, because Gradle could not resolve Loom 1.17.21 in the prior source-generation attempt.
- Custom data component registration/bootstrap in the actual resolved mappings.
- Runtime proof that the proposed capture hook is entered only for eligible natural placed features and never for sapling, structure or other feature calls.
- Provenance-aware split/merge behavior at every ordinary Survival operation; menu/chest/hopper/item-entity save and transfer persistence; exact inventory receipt attribution; multiplayer concurrency.
- Runtime measurements and all K5 tests. Nothing in this design is marked passed.

## Implementation readiness and exact next step

**Readiness: not ready to enable K5 progress.** Source-level hooks are plausible, but mapped mixin targets, custom component registration, ordinary stack conservation and actual player receipt have not passed compilation/runtime probes. The unresolved approval gates above, especially nonzero legacy counters, must remain closed before enabling K5 for affected players.

**Exact next step:** in a network-enabled checkout synchronized to current PR #80, run `./gradlew genSources`, verify the candidate target descriptors against generated sources, then compile and run a no-credit source probe with `./gradlew compileGametestJava runGameTest` for natural feature versus sapling/bonemeal paths. Do not add the production progress hook until this probe passes and policy gates are decided. Reproducible blocker: the prior environment's Gradle failed to resolve pinned Loom 1.17.21; if repeated, obtain a provisioned dependency cache/network environment and record the resolution failure before any implementation.

## Verification status

This task added no K5 code and ran no K5 tests. Fabric API sources were fetched from tag 0.161.0+26.3. Minecraft source evidence comes from Mojang's official 26.3 client jar with matching manifest SHA-1, decompiled with CFR 0.152. Gradle source generation could not complete because Gradle could not resolve the pinned Loom 1.17.21 plugin in this environment; direct official jar inspection was the fallback. Thus Mojang-named source paths and descriptors are verified against the jar, but mapped Mixin descriptors, custom component registration under the project's resolved Loom namespace, and runtime hook order remain unverified implementation gates. No K5 tests were executed.

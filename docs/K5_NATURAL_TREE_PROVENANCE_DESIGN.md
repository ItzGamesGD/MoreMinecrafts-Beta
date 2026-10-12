# K5 Natural Giant Tree Provenance Architecture

**Status:** Design only; no production code, migration, or gameplay change is included.  
**Ancestry baseline:** PR #80 head 096d9c825422a2fc1220b88ea6ee75967e7fa1bf  
**Runtime dependencies:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25.  
**Remote review snapshot (2026-10-12):** PR #80 current head 7fc0b2094e41ff65c931af50a6384acc7ca22134; PR #81 current head before this amendment ee1112066ba9fe98af7d15795bd2e25e34341a56. GitHub compare reports PR #81's head diverged from current PR #80 with common ancestor 096d9c825422a2fc1220b88ea6ee75967e7fa1bf (3 commits ahead, 16 behind). The requested dependency pins were re-fetched from PR #80's current head and match this review. This update remains documentation-only.

## Approved semantics

K5 unlocks when a player acquires 128 distinct log units originating from naturally world-generated 2×2 large-tree features. This counts acquired logs, not blocks broken and not discovery of a tree. Player-grown trees, player-placed logs, small natural trees, Creative-mode acquisition, repeat collection of the same source log by the same player, and old-world logs without recorded provenance are ineligible.

The achievement and its progress are player-specific. The same authentic natural log unit may contribute once to each player's progress. A transfer does not consume the log's eligibility globally. Example: A receives 64 eligible logs and gains 64; B receives those logs from A and also gains 64; when B returns them to A, A gains zero; later transfers between them grant neither player additional progress for those source units.

The existing target remains 128 per player. No geometry, leaf, biome, or species-only heuristic establishes provenance. Previously saved Knowledge state remains subject to the existing save-compatibility authority; the unresolved legacy-counter issue is listed below.

### User-approved design amendment (2026-10-12)

The user approved player-specific K5 credit in this amendment. Provenance authenticity is global and non-consuming; redemption history is per player. The same authentic generated log may count once for each player, but no player may count the same token twice. The original 128 acquired qualifying logs per-player threshold and all earlier origin exclusions remain in force. This amendment supersedes the earlier draft's proposed world-global first-claim ownership and globally redeemed-token ledger.

## Recommendation

Use a hybrid of generation-position provenance, authenticated item-carried source tokens, and persistent player-specific redemption history:

1. During an identified natural tree-feature invocation, record only surviving qualifying log positions in a persistent chunk attachment.
2. At successful harvest, consume the exact position marker and attach an authenticated unique source token to the matching log drop before it enters the world.
3. Preserve the token through inventory and container transfers. Each player records the token only on that player's first successful Survival inventory acquisition.
4. Do not maintain a global redeemed/owner set. A token remains authentic and transferable after any player's acquisition.
5. Keep per-player token history exact while the player is progressing. Since K5 progress caps at 128, at most 127 source identities are needed before unlock; after permanent unlock, further K5 credit is impossible and the history can be discarded.
6. Fail closed for missing, corrupt, forged, unknown-version, or unsupported provenance.

The original global claim ledger proposed in the first draft is removed. Global authenticity and per-player redemption are separate: a server-held world secret authenticates token contents without globally consuming a token; each player's bounded persistent history prevents only that player from counting the same token twice. The exact history is bounded because the achievement target is fixed and progress saturates, not because entries are evicted from a cache.

The project’s Fabric callbacks do not supply generation cause or resulting drops. A narrow worldgen source mixin, block-drop mixin, authenticated item data component, inventory-acquisition observer, and player attachment update are needed. The natural feature path and exact 26.3 mixin descriptors remain runtime gates.

## Investigated APIs and evidence

The repository's current PR #80 head was re-fetched before review; the dependency declarations in gradle.properties and build.gradle at 7fc0b2094e41ff65c931af50a6384acc7ca22134 pin the following. The repository pins Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25, and JUnit 5.11.4 in [gradle.properties](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/096d9c825422a2fc1220b88ea6ee75967e7fa1bf/gradle.properties) and [build.gradle](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/096d9c825422a2fc1220b88ea6ee75967e7fa1bf/build.gradle). Mod metadata limits the supported game version to 26.3.

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
| A. Generation-time block positions | Records origin before harvesting; sparse persistent chunk data survives reload; old chunks naturally lack markers. Does not prove later acquisition. | Chunk unload/load callbacks do not identify origin. Requires natural-feature mixin and invalidation on replacement. Modded placers need explicit support. | Necessary first half, insufficient alone. |
| B. Provenance-bearing item stacks | Carries origin through item entities and normal inventory transfer; enables later acquisition credit. Cannot establish provenance on its own. | ItemStack is not a Fabric Attachment target. Custom item components alter stack equality. Different provenance payloads do not ordinarily merge. Component-stripping mods lose provenance. Split copies need deduplication. | Necessary second half, insufficient alone. |
| C. Hybrid positions plus item tokens | Connects generated block, actual item drop, and later player recipient. Persistent chunk and world data survive reload. | Crosses worldgen, chunk, block-drop, item, inventory, and save lifecycles. Multiplayer and automation require exact transfer ownership. Modded behavior is version-specific. | Recommended for strict semantics; highest complexity and requires runtime gates. |

### Stack split/merge and container behavior

A provenance component participates in ItemStack component equality. Vanilla can merge equal payloads; distinct token payloads will not ordinarily merge. The implementation must preserve eligibility while merging by an explicit tested operation that forms the set union of unique authenticated tokens, bounded by resulting stack count. Splitting partitions tokens between resulting stacks so each eligible physical unit has one token. Any stack count without a corresponding token is ineligible. If any vanilla split, copy, merge, or inventory path cannot preserve these rules, it must fail closed for that path.

Item duplication may copy a token payload, but does not create a new source identity. Each player's exact redemption history means a duplicated token cannot give that player a second progress unit. Another player may redeem the same authentic token once, as approved. A transfer does not strip or globally consume the token.

Chest, hopper, and supported backpack storage carries the component with the stack. Hopper-to-container movement grants no player credit; the first successful Survival transfer into each player's inventory can grant that player credit. Container code that strips/rebuilds item data loses eligibility. Furnace fuel consumption yields no log acquisition and does not pass provenance to furnace output.

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
| log_positions | sorted packed positions | Exact surviving eligible logs in this chunk. |

Validate each position belongs to the chunk and still contains the expected log state. Remove the marker on replacement or other invalidating mutation. Store no leaves, attempted writes, or scan results. Test attachment copying through ProtoChunk-to-LevelChunk conversion.

### Authenticated item component, version 1

Identifier: moreminecrafts:k5_natural_log_provenance. Fields: version, source token, and authentication tag. A token identifies world-instance UUID, dimension key, feature UUID, source packed position, and log registry ID. The server signs the canonical token fields with an HMAC-SHA-256 key held only in versioned world SavedData. Verify the tag on every server-side credit path. A copied or edited token cannot be turned into a new source identity; copying an authentic token retains the same identity. The secret is never sent to clients or exposed through commands.

The world secret is fixed-size rather than a global issuance list. Loss/corruption of the secret invalidates token verification and disables new K5 credit; do not silently mint a replacement key for an existing world. This protects against item/NBT forgery by clients and ordinary gameplay. It is not a trust boundary against a world operator or server mod that can read/replace server save data.

A stack carries one unique token per eligible log unit, up to its legal count. Component decoding is bounded. Tokens must be partitioned on split and unioned without duplication on merge.

### Player-specific redemption history, version 1

Persist on the existing player attachment (or a separately versioned player attachment if isolation is needed). Store the schema version and an exact canonical set of authenticated source tokens already counted for this player's still-locked K5 progress. On successful Survival inventory receipt:

1. Verify token authenticity.
2. Compute the actual number of eligible units received.
3. Ignore tokens already in this player's history.
4. Record no more distinct tokens than the transferred eligible quantity and remaining K5 target.
5. Increase only this player's existing K5 counter by that count.

Do not remove or globally consume tokens when a player receives them. Transfer to another player is valid because that player's history is independent. Player death, reconnect, inventory movement, and world reload must preserve the history according to the existing player-attachment lifecycle.

### Exact storage bound

For a locked player starting with valid K5 progress p, at most min(127, 128 - p) new token identities can affect the achievement before it unlocks. Thus the exact set is bounded by 127 tokens per locked player, assuming progress is derived only from valid token acquisitions. The set can be stored as exact token values, optionally grouped by feature UUID with sorted source positions/bitsets; it is not a fixed-size probabilistic filter and has no eviction. At unlock, no later acquisition can add K5 progress, so progress-relevant history can be discarded while the permanent unlock flag remains.

This bound does not apply if the game later tracks post-unlock collection statistics, raises the target, or allows K5 progress to decrease/reset. Those features would require retaining more history or a new design. A nonzero legacy K5 counter without corresponding source-token history is also unresolved: its prior source identities cannot be reconstructed. See approval decisions.

### Storage and security feasibility

The exact player-specific redemption set is reasonably small: at 127 source identities, a compact 32-byte identity costs about 4 KiB raw, before codec/map overhead. At 100,000 locked players that is roughly 400 MiB raw in the worst case if all reach the cap, but normal total storage scales with actual player count and progress. Per-player history is exact, persistent and capped only because K5 progress permanently saturates at 128. Never evict entries while the player can still gain K5 progress. Existing unlocked players need only retain the permanent Knowledge unlock if no post-unlock statistic is introduced.

Item-carried identity has a different scaling profile. At an estimated 64–96 bytes per signed identity, a full 64-log stack can carry roughly 4–6 KiB of token payload before component/container encoding overhead. Measure real serialized bytes, save size, network sync cost, merge cost and pickup latency. Chunk position provenance is sparse and approximately 8 bytes per packed position before codec overhead.

A world HMAC authenticates that token fields were minted by this server key. It does not prevent an operator, command, mod, or copied ItemStack from duplicating a valid signed component. Player history ensures any one player can count a token identity at most once, but it cannot tell a valid transfer to a new player from a copied token delivered to that player. Under the approved rule both can result in that player counting the same source identity once. If “no duplication” requires global physical-unit conservation across players, an additional exact live-custody ledger and exhaustive interception of every copy/split/merge/serialization/container path would be required. Such a ledger must track extant units and likely retain invalidation/tombstone information; it can grow with lifetime world provenance, unlike the bounded player set. Arbitrary modded inventories, commands and offline saves make exhaustive custody guarantees impractical without restricting supported paths. This is an unresolved strict-semantics blocker, not a problem solved by signing.

The recommended bounded alternative preserves the approved per-player result: mint one identity per natural source position, carry identity sets on item stacks, reject forged/new identities, and credit each player once per identity. A copied token cannot increase that player's count for that identity, but may be presented to another player with the same effect as a legitimate transfer. If this is not sufficient for the no-duplication rule, implementation must stop for policy/architecture review rather than claiming exact physical conservation.

### Save compatibility

Existing Knowledge unlocks/counters remain subject to current save compatibility policy. New chunk data without an attachment, old item stacks without a signed component, and unknown-origin logs are ineligible. Do not scan/migrate old chunks. Missing/corrupt/unknown-version chunk, token, key, or player-history data fails closed. If player history is corrupt, disable further K5 credit for that player rather than replace it with an empty set. Any schema migration must be explicit and versioned.

## Hook integration plan

The following Mojang-named class/method descriptors were verified from the official 26.3 jar's class files and CFR output. Fabric/Loom-mapped compile descriptors and runtime invocation order are still gates.

| Purpose | Candidate class/method and descriptor | Verified boundary |
|---|---|---|
| Natural biome source | ChunkGenerator.applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureManager):void, wrapping its call to FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos):boolean | Biome placed-feature loop; inspect the actual PlacedFeature. Wrap only the candidate tree feature call. |
| Feature/config/output | FeaturePlacer.placeWithBiomeCheck(PlacedFeature, RandomSource, BlockPos):boolean; Feature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos):boolean; TreeFeature.place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos):boolean | Configuration and success are available to a scoped capture. Exact write-capture site requires implementation probing. |
| Sapling exclusion | SaplingBlock.randomTick(BlockState, ServerLevel, BlockPos, RandomSource):void; advanceTree(ServerLevel, BlockPos, BlockState, RandomSource):void; performBonemeal(ServerLevel, RandomSource, BlockPos, BlockState, BonemealSource):void; TreeGrower.growTree(ServerLevel, ChunkGenerator, BlockPos, BlockState, RandomSource):boolean | Both growth triggers enter advanceTree; TreeGrower directly places configured Feature rather than using natural biome feature decoration. |
| Chunk storage | Fabric AttachmentRegistry.createPersistent(Identifier, Codec):AttachmentType; ServerChunkEvents Load(ServerLevel, LevelChunk, boolean), Unload(ServerLevel, LevelChunk) | Persistent codec attachments and chunk lifecycle callbacks exist. AttachmentType documents ChunkAccess targets and ProtoChunk-to-LevelChunk copying. Load/unload are not generation-cause signals. |
| Player break/drop | PlayerBlockBreakEvents.AFTER(Level, Player, BlockPos, BlockState, @Nullable BlockEntity):void; Block.dropResources(BlockState, Level, BlockPos, BlockEntity, Entity, ItemStack):void; Block.popResource(Level, BlockPos, ItemStack):void | Break callback has no drops; correlate exact loot output before entity insertion. |
| Pickup | ItemEntity.playerTouch(Player):void; Inventory.add(ItemStack):boolean | Pickup adds to inventory on server then discards an emptied ItemEntity; generic inventory adds have other sources. |
| Menu transfer/stack math | AbstractContainerMenu.doClick(int, int, ContainerInput, Player):void; moveItemStackTo(ItemStack, int, int, boolean):boolean; ItemStack.split(int):ItemStack; isSameItemSameComponents(ItemStack, ItemStack):boolean | Vanilla component-aware merge plus component-copying split; requires explicit token partition/union logic and tests. |
| Item data | ItemStack.CODEC and its component map | Components serialize with stacks and equality considers components. Exact custom component registration under the resolved Fabric/Loom namespace remains unverified. |

Pinned Fabric source evidence:
- PlayerBlockBreakEvents.AFTER callback signature is void afterBlockBreak(Level, Player, BlockPos, BlockState, @Nullable BlockEntity); the source documents it fires after a successful server-side break, and CANCELED is separate.
- ServerChunkEvents.Load and Unload signatures are void onChunkLoad(ServerLevel, LevelChunk, boolean) and void onChunkUnload(ServerLevel, LevelChunk).
- AttachmentRegistry.createPersistent(Identifier, Codec) uses a codec for restart persistence; AttachmentType documents chunk targets and ProtoChunk-to-LevelChunk copying.
- A source-tree search of Fabric API 0.161.0+26.3 found no universal item-pickup/inventory-acquisition event. This does not rule out per-mod APIs.

### Staged implementation plan and gates

1. **Resolve source/dependency baseline.** Resolve Loom 1.17.21 in the pinned Gradle environment, generate mapped sources and verify the descriptors above against mappings/bytecode. Stop if dependency resolution fails or signatures differ.
2. **Worldgen proof.** In a separate implementation task, exercise natural biome generation, 2×2 sapling random tick, bonemeal, structure placement, and player placement. Confirm only approved natural feature calls enter capture. Approve structure scope first. Build the explicit feature-key/placer/log allowlist from runtime registry objects.
3. **Position persistence prototype.** Use a versioned immutable persistent chunk attachment. Capture only final source-log positions from successful allowed feature calls. Test ProtoChunk conversion, chunk unload/reload, replacement, piston, explosion and command invalidation. Measure chunk bytes and worldgen overhead.
4. **Drop/token prototype.** Correlate a successful Survival break with exactly one source unit in loot output and prevent Fortune/multi-drop from multiplying identities. Prototype a codec-backed component and world-key save/load/loss behavior. Stop if the mixin cannot distinguish exact output units from unrelated drops.
5. **Stack conservation gate.** Before enabling gameplay credit, instrument ItemStack.split, copy/copyWithCount, shrink/grow, vanilla menu merges, ItemEntity merges, pickup remainders, Creative clone, commands, hopper/container movement and save/load. Prove supported operations never create new token identities or duplicate per-player credit. A valid HMAC does not prevent copying a valid signed stack. If policy requires detecting copies delivered to other players separately from legitimate transfers, prove a global live-custody design and cost first; otherwise stop and request a policy/architecture decision.
6. **Player credit.** Persist exact per-player token history. On successful Survival receipt, atomically insert unseen identities and increment only that player's K5 progress by the unique-token delta. A token may be redeemed once by A and once by B; no global redemption restriction. Exercise concurrent pickups on the server thread, reconnect and reload.
7. **Compatibility and rollout.** Obtain decisions for legacy/structure/modded/Creative policies. Run deterministic and dedicated-server runtime matrices. Retain a disable/rollback gate: stop issuing and crediting tokens while preserving saved data and unlocked Knowledge; never regenerate the signing key to accept old tokens. Re-enable only after compatibility, schema and duplication checks pass.

No universal modded backpack or inventory guarantee is identified. Publish the tested compatibility list and fail closed outside it.

## Performance and memory

No benchmark was run. Work is sparse and proportional to qualifying feature calls, placed logs, and eligible inventory transfers:

- Worldgen: one scope/config check per candidate tree; capture only successful log writes for supported placers. No full-world scan.
- Chunk data: approximately 8 raw bytes per packed log position before codec/container overhead, plus per-tree metadata.
- Item data: one authenticated token per eligible log unit while held; bounded by legal stack count.
- World authentication key: fixed-size secret plus schema metadata; no world-global redeemed-token list.
- Player history: at most 127 exact source tokens per locked player under the current 128 target. This is a mathematical bound from the achievement cap, not an expiring cache.
- Transient data: a feature-scoped local capture object and bounded operation context. No unbounded static maps or retained chunks.

Measure generation median/p95 and allocations with mixin on/off, chunk bytes, item component size, per-player history size at 0/64/127 claims, key/history load-save duration, and concurrent acquisition contention. If future semantics require post-unlock collection counting or progress reset, redo the storage analysis.

## Abuse and edge cases

- Sapling-grown 2×2 trees and player-built imitations receive no marker regardless of final shape.
- Small natural trees and unknown trunk placers are excluded.
- Successful Survival harvest binds at most one authentic token to the matching log unit. Failed/canceled breaks preserve the source marker.
- Creative-generated/acquired items, forged token fields/signatures, and unknown-origin logs never gain provenance or progress.
- A token remains globally authentic and transferable. Each player independently receives credit at most once per token.
- Repeated A/B transfers do not re-credit either player after that player's first receipt.
- Splitting partitions tokens; merging unions unique tokens; duplicated payloads do not create new token identities or repeat credit for a player.
- Fortune/multiple loot outputs issue no more tokens than original qualifying log units.
- Hopper movement grants no progress; player inventory receipt does. Component-stripping containers fail closed.
- Furnace fuel use earns no credit and transfers no token to output.
- A Survival piston/explosion/automation path should preserve the source identity when it moves the log or produces its matching log item; player progress still occurs only at Survival inventory receipt. Destructive replacement with no matching log output invalidates the position. This source-path policy needs approval and runtime coverage.
- Old chunks/items without provenance remain ineligible. Corrupt/unknown data fails closed.
- Data key loss invalidates verification; do not regenerate a key and accept old tokens.
- Modded block-moving or inventory paths not covered by hooks remain unsupported until tested.

## Test plan

These tests are proposed and were not executed. Existing PR #80 CI results do not verify K5 provenance.

### Deterministic unit tests

| Test | Expected assertion |
|---|---|
| Token codec/authentication | Valid signed tokens verify; modified fields, forged signatures, unknown versions, oversized payloads, and duplicates fail closed. |
| Player history | Exact insert/contains round-trip; duplicate token is idempotent; no eviction; locked history cannot exceed 127 valid progress tokens. |
| Split/merge arithmetic | Split partitions token sets; merge unions without duplicates; stack eligible quantity never exceeds unique authentic token count or item count. |
| Per-player independence | Same token is new for B after A claimed it; repeat is old for A. |
| Progress threshold | 127 valid per-player acquisitions remain locked; 128th unlocks exactly once. |
| Transfer arithmetic | Partial, failed, canceled, Creative, Survival, and mixed stacks credit the actual unique-token delta only. |
| Corrupt history | Corrupt/unknown player history disables credit instead of resetting to empty. |

### Minecraft 26.3 runtime/GameTests

| Scenario | Runtime assertion |
|---|---|
| Natural 2×2 tree | Real natural feature path creates markers only for final qualifying logs. |
| Sapling-grown 2×2 tree | Same-looking/player-grown counterpart creates no markers. |
| Small natural tree / player-built imitation | Neither creates provenance. |
| Survival drop and pickup | Exact matching log output receives authenticated source token; credit occurs only after successful inventory receipt. |
| Creative-generated/rejected item | Creative source is untagged; forged/copied unsigned component is rejected; no progress. |
| A receives 64 logs | A gains 64 and records those 64 source IDs. |
| A transfers 64 logs to B | B gains 64; A retains its own 64-entry history; token remains authentic. |
| B returns logs to A | A gains zero additional progress. |
| Repeated A/B transfers | Neither player gains further progress for those tokens. |
| Partial stack transfers | Each player gains only the unique token count actually transferred, capped by inventory delta. |
| Split/merge | Normal splits partition tokens; merges union IDs; duplicates do not create eligible IDs or repeat credit. |
| Chest/hopper/backpack | Storage transfer alone earns none; each player's first Survival inventory receipt earns once; supported container preserves signed tokens. |
| Save/reload between transfers | Item tokens, key, and both players' histories persist; after restart A gets zero and B still gets one first credit if not yet received. |
| Player reconnect | History survives disconnect/reconnect and prevents repeat credit. |
| Concurrent multiplayer pickup | Server serialization gives each player only their own first redemption; simultaneous receipt does not mutate another player's history. |
| Duplication/forgery | Clone/copy the same signed token; one player cannot count that identity twice. Alter token/ID/signature; invalid. Presenting a copied authentic token to a different player is indistinguishable from a legitimate transfer and may count once for that player under the approved per-player rule; global physical-copy detection remains an unresolved constraint. |
| Threshold 127→128 | Locked at 127; 128th distinct authentic token unlocks once; repeats do not advance. |
| Old chunks | Old tree-shaped logs remain ineligible without attachment; no migration scan. |
| Survival piston/explosion/automation | Preserve the identity through block movement or matching log drop; no credit until Survival player inventory receipt. Test that replacement without matching output invalidates the marker. |
| Modded trees | Explicitly supported natural placers qualify; unknown placers and player-grown variants reject. |

Runtime tests require a dedicated 26.3 server with pinned Fabric versions and installed mixins. Report execution results separately.

## Policy recommendations and approval gates

These are recommendations only. None of the following is approved by this document; obtain a decision before implementation.

1. **Legacy K5 progress.** Preserve all already-unlocked Knowledge. Recommend resetting an old partial K5 counter to zero when its exact prior token history is absent, because old provenance cannot be reconstructed and keeping the counter can double-count a log that is still in the world. Consequence: locked players may lose unverified partial progress. Alternative: grandfather the numeric counter, accepting that strict distinct-source semantics cannot be guaranteed across the migration boundary. Do not change existing saves until approved.
2. **Structure-generated trees.** Recommend excluding structure trees initially and counting only approved biome-decoration tree features. This bounds the source hook and prevents player-built/structure template logs from being mislabeled. Consequence: a naturally generated structure containing a giant tree would not count. A broader rule needs structure placement cause coverage and separate tests.
3. **Modded natural 2×2 trees.** Recommend vanilla feature/placer allowlist by default and explicit mod opt-in per supported mod, with a source hook and runtime test. Unknown/modded placers fail closed. Consequence: eligible modded trees do not count until integrated; this avoids shape heuristics and accidental player-grown attribution.
4. **Piston, explosion and automation paths.** Recommend carrying provenance when a marked log is moved by a Survival piston or becomes a matching Survival-world log drop; a later Survival player inventory receipt may credit it. A removal that produces no matching log item invalidates the marker. Consequence: additional piston, explosion and automated drop hooks are required, increasing compatibility and duplication risk. Commands fail closed. Approve or reject this source-path scope before implementation.
5. **Creative receipt and duplication.** Recommend no credit in Creative and invalidating a token when received into a Creative player's inventory; Creative-created items never receive tokens. Consequence: an otherwise authentic log taken into Creative cannot later count after returning to Survival. This is stricter than merely withholding credit, but helps close creative-copy paths. Authentication alone cannot prevent copying an already valid signed component. Per-player exact history prevents repeated credit for that player; it cannot distinguish a copied authentic token offered to another player from a legitimate transfer. Decide whether the approved no-duplication rule means no new token identities/per-player progress from copies, or requires global live physical-unit custody.
6. **Acquisition boundary and compatibility.** Recommend pickup and explicit supported vanilla container/menu transfers into a Survival inventory. Hopper-only moves do not count. Backpack/mod support requires a tested integration preserving per-unit identity and stack arithmetic; otherwise fail closed.
7. **Exact physical conservation feasibility.** If copies to different players must be distinguished from legitimate transfers, require a global live-custody system and constrain supported item paths. This is materially larger and can become lifetime-growing storage. Approve that tradeoff or approve the per-player identity interpretation above; no implementation should silently choose.

The 128 threshold remains per-player and acquisition-based. Existing unlocked Knowledge must remain unlocked. K5 is assumed permanently unlocked at 128; storing post-unlock collection statistics or allowing reset requires a new storage review.

## Implementation acceptance criteria

- Exact 26.3 natural-generation and sapling call paths verified; runtime tests prove source distinction.
- Reviewed trunk-placer allowlist reflects actual 2×2 implementations.
- Only successful natural log positions persist; replacement/non-player removal invalidates them.
- Exact source block mints one authenticated token only on its matching log drop.
- Authenticity is global and non-consuming; redemption history is per player.
- Each player counts a token at most once, while different players may each count the same token once.
- Player history is exact and bounded by the current target; no cache eviction or probabilistic filter.
- Splits partition and merges union provenance without increasing unique eligible quantity; copied valid components do not create a new source identity.
- Tests establish whether supported paths enforce the required per-player interpretation; the design does not claim HMAC prevents copying or claim global physical custody without a tested ledger.
- Item/container/inventory/save/reload/concurrent transfer paths preserve or fail closed correctly.
- Creative, player-grown, small, imitation, old-world, canceled, forged, and unknown-origin cases never add progress; approved Survival automation paths preserve authentic IDs and still credit only on player inventory receipt.
- Existing K5 save state is handled according to an approved legacy policy; threshold is exactly 127 locked/128 unlocked.
- Runtime and deterministic tests run on pinned dependencies; outcomes are reported separately.
- Worldgen, item component, per-player history, and save/load costs are measured.

## Verification status

This task added no K5 code and ran no K5 tests. Fabric API sources were fetched from tag 0.161.0+26.3. Minecraft source evidence comes from Mojang's official 26.3 client jar with matching manifest SHA-1, decompiled with CFR 0.152. Gradle source generation could not complete because Gradle could not resolve the pinned Loom 1.17.21 plugin in this environment; direct official jar inspection was the fallback. Thus Mojang-named source paths and descriptors are verified against the jar, but mapped Mixin descriptors, custom component registration under the project's resolved Loom namespace, and runtime hook order remain unverified implementation gates. No K5 tests were executed.

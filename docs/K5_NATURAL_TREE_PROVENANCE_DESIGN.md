# K5 Natural Giant Tree Provenance Architecture

**Status:** Design only; no production code, migration, or gameplay change is included.  
**Baseline:** PR #80 head 096d9c825422a2fc1220b88ea6ee75967e7fa1bf  
**Runtime dependencies:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.

## Approved semantics

K5 unlocks when a player acquires 128 distinct log units originating from naturally world-generated 2×2 large-tree features. This counts acquired logs, not blocks broken and not discovery of a tree. Player-grown trees, player-placed logs, small natural trees, Creative-mode acquisition, repeat collection of the same source log by the same player, and old-world logs without recorded provenance are ineligible.

The achievement and its progress are player-specific. The same authentic natural log unit may contribute once to each player's progress. A transfer does not consume the log's eligibility globally. Example: A receives 64 eligible logs and gains 64; B receives those logs from A and also gains 64; when B returns them to A, A gains zero; later transfers between them grant neither player additional progress for those source units.

The existing target remains 128 per player. No geometry, leaf, biome, or species-only heuristic establishes provenance. Previously saved Knowledge state remains subject to the existing save-compatibility authority; the unresolved legacy-counter issue is listed below.

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

The repository pins Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25, and JUnit 5.11.4 in [gradle.properties](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/096d9c825422a2fc1220b88ea6ee75967e7fa1bf/gradle.properties) and [build.gradle](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/096d9c825422a2fc1220b88ea6ee75967e7fa1bf/build.gradle). Mod metadata limits the supported game version to 26.3.

The baseline K5 audit in [Stage 2-B Knowledge condition corrections](STAGE2B_KNOWLEDGE_CONDITION_CORRECTIONS.md) records that current player-break callbacks lack natural-origin provenance and explicitly rejects matching trunk shape or leaves as a substitute.

Fabric API source was inspected at tag 0.161.0+26.3:

- [ServerChunkEvents.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/api/event/lifecycle/v1/ServerChunkEvents.java) provides server chunk load/unload events. These describe lifecycle, not what placed a block.
- [PlayerBlockBreakEvents.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/api/event/player/PlayerBlockBreakEvents.java) supplies level, player, position, pre-break block state, and optional block entity. AFTER is called after a successful server-side break; canceled breaks use CANCELED. It supplies no feature cause or drop stack.
- [AttachmentRegistry.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-data-attachment-api-v1/src/main/java/net/fabricmc/fabric/api/attachment/v1/AttachmentRegistry.java) exposes persistent attachments with codecs.
- [AttachmentType.java](https://github.com/FabricMC/fabric-api/blob/0.161.0%2B26.3/fabric-data-attachment-api-v1/src/main/java/net/fabricmc/fabric/api/attachment/v1/AttachmentType.java) names chunks as supported attachment targets and notes ProtoChunk-to-LevelChunk copying. The attachment API does not list ItemStack as a target, so item provenance needs a custom codec-backed item data component or equivalent integration.

The project uses the existing persistent player attachment for condition progress. It has no K5 provenance implementation at the baseline.

### Natural feature source and tree configuration

A later break callback cannot distinguish a natural tree from an identical sapling-grown tree. No public Fabric event inspected at the pinned version provides both generation cause and tree configuration.

The proposed discriminator is a narrow mixin scope around natural biome feature decoration. Within that scope, inspect the actual configured tree feature and trunk placer, then capture successful final log writes. Sapling growth must not enter this scope. Qualify only trunk-placer types verified to emit the approved 2×2 trunks. This does not infer origin from the final shape.

The exact Minecraft 26.3 mapped call chain, method descriptors, complete vanilla 2×2 trunk-placer allowlist, and whether sapling growth bypasses that natural-decoration boundary were not independently verified from a 26.3 mapped source artifact in this environment. Verify against Loom-resolved sources before implementation. A runtime probe must show natural feature generation enters the scope and sapling growth does not. If a reliable source-specific boundary cannot be verified, block implementation.

Capture must be invocation-scoped: keep candidate final block writes locally, discard on failed feature placement, and commit only surviving logs on success. Use a reviewed registry-backed trunk-placer allowlist. Unknown placers fail closed. Whether explicitly registered modded trees count is an approval decision.

### Drop and acquisition boundary

A source position must be consumed at the loot-to-item boundary, not inferred from an ItemEntity's later location. PlayerBlockBreakEvents.AFTER is a success signal, but does not provide the resulting drop. A Minecraft 26.3 mixin must correlate the exact successful Survival break with only matching log output before ItemEntity insertion.

Canceled/failed breaks preserve the marker. Creative breaks do not issue a token and invalidate the original marker. Explosions, pistons, commands, and other non-player removals invalidate the position without minting an item token. A matching log drop receives at most one source token even if loot produces multiple items/stacks.

Acquisition means successful addition of eligible units to a Survival ServerPlayer inventory. This includes direct item pickup and taking items from supported containers. A hopper moving items between storage blocks is not player acquisition. The inventory observer must check successful before/after transfer counts and cover pickup, partial pickup, quick-move, shift-click, and cursor transfers. Creative receipt invalidates provenance without credit. A tagged log used as furnace fuel is consumed and cannot pass provenance to furnace output.

Vanilla containers generally move the ItemStack and its data. Backpack/mod compatibility depends on retaining unknown item data; if a mod strips or reconstructs it, provenance is lost and must not be inferred.

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

### Save compatibility

Existing Knowledge unlocks/counters remain subject to current save compatibility policy. New chunk data without an attachment, old item stacks without a signed component, and unknown-origin logs are ineligible. Do not scan/migrate old chunks. Missing/corrupt/unknown-version chunk, token, key, or player-history data fails closed. If player history is corrupt, disable further K5 credit for that player rather than replace it with an empty set. Any schema migration must be explicit and versioned.

## Hook integration plan

Exact method names/descriptors require validation against resolved Minecraft 26.3 sources before implementation.

1. Scope only the natural biome feature-decoration invocation of a configured tree; prove sapling growth is outside it.
2. Inspect actual tree configuration and trunk placer against a reviewed 2×2 allowlist.
3. Capture successful log writes locally; commit surviving positions only after successful feature placement.
4. Invalidate a marker on replacement or non-player removal, including piston/explosion/commands. Do not retain chunk objects in global maps.
5. At successful Survival break loot materialization, consume that exact marker and issue its signed token only to matching log output before ItemEntity insertion. Failed/canceled breaks preserve; Creative/non-player removals do not mint.
6. Register a bounded codec-backed item component. Verify signature, split/merge, ItemEntity save/load, pickup, and container persistence.
7. Observe successful Survival inventory additions across pickup and supported container transfers. Verify signatures, consult only the acquiring player's history, credit new tokens once for that player, and retain tokens for future players.
8. Keep hopper-to-container transfer uncredited. Different token sets require a tested union merge; never drop token identities to force a stack.
9. Persist player history on the player attachment and key material in world SavedData. Chunk unload discards only transient invocation state; persistent attachment/component/player history/world key survive reload.

No global redeemed-token history or first-owner rule is used.

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
- Explosions, pistons, commands, automation, and other non-player removal invalidate the block-position marker and do not mint a player-acquisition token.
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
| Duplication/forgery | Clone/copy same signed token; no player can count it twice. Alter token/ID/signature; invalid. Replaying an authentic token to a different player may count once for that player, as approved. |
| Threshold 127→128 | Locked at 127; 128th distinct authentic token unlocks once; repeats do not advance. |
| Old chunks | Old tree-shaped logs remain ineligible without attachment; no migration scan. |
| Piston/explosion/automation | No player progress; source position becomes invalid. |
| Modded trees | Explicitly supported natural placers qualify; unknown placers and player-grown variants reject. |

Runtime tests require a dedicated 26.3 server with pinned Fabric versions and installed mixins. Report execution results separately.

## Decisions requiring approval

1. **Modded trees:** recommendation: vanilla allowlist plus explicit modded opt-in and runtime coverage; unknown placers fail closed.
2. **Acquisition boundary:** recommendation: successful transfer into a Survival player's inventory, including chest/backpack extraction; hopper-to-container movement is not acquisition.
3. **Creative transition:** recommendation: Creative-generated logs are untagged; token-bearing items received into Creative inventory are invalidated. Confirm whether Creative receipt should invalidate the item for later Survival transfers.
4. **Natural generation scope:** confirm whether structure-generated or other non-biome natural features count. Proposed mixin scope covers biome tree-feature decoration.
5. **Legacy K5 progress:** an old nonzero K5 counter may have no token history, so its prior source logs cannot be deduplicated against new tokens. Decide whether to grandfather that progress, defer new K5 credit for such players, or authorize a targeted migration/reset. No value should be silently cleared or treated as verified.
6. **Post-unlock behavior:** current bounded-history proof assumes K5 is permanently unlocked at 128 and no post-unlock acquisition count is required. If post-unlock statistics or resetting K5 is desired, approve a new storage design.
7. **Data-stripping mods:** recommendation: fail closed unless integration explicitly preserves and tests the signed token.

## Implementation acceptance criteria

- Exact 26.3 natural-generation and sapling call paths verified; runtime tests prove source distinction.
- Reviewed trunk-placer allowlist reflects actual 2×2 implementations.
- Only successful natural log positions persist; replacement/non-player removal invalidates them.
- Exact source block mints one authenticated token only on its matching log drop.
- Authenticity is global and non-consuming; redemption history is per player.
- Each player counts a token at most once, while different players may each count the same token once.
- Player history is exact and bounded by the current target; no cache eviction or probabilistic filter.
- Splits partition and merges union provenance without increasing unique eligible quantity.
- Item/container/inventory/save/reload/concurrent transfer paths preserve or fail closed correctly.
- Creative, player-grown, small, imitation, old-world, canceled, automated, forged, and unknown-origin cases never add progress.
- Existing K5 save state is handled according to an approved legacy policy; threshold is exactly 127 locked/128 unlocked.
- Runtime and deterministic tests run on pinned dependencies; outcomes are reported separately.
- Worldgen, item component, per-player history, and save/load costs are measured.

## Verification status

This task added no K5 code and ran no K5 tests. API evidence above is based on the declared project dependency and Fabric API sources at the pinned tag. Minecraft 26.3 mapped mixin descriptors and runtime call-path tests remain unverified and are implementation gates.

# K5 Natural Giant Tree Provenance Architecture

**Status:** Design only; no production code, migration, or gameplay change is included.  
**Baseline:** PR #80 head 096d9c825422a2fc1220b88ea6ee75967e7fa1bf  
**Runtime dependencies:** Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.

## Approved semantics

K5 unlocks after one player acquires 128 distinct log units originating from naturally world-generated 2×2 large-tree features. This counts acquired logs, not blocks broken and not discovery of a tree. Player-grown trees, player-placed logs, small natural trees, Creative-mode acquisition, repeat collection of a source log, and old-world logs without recorded provenance are ineligible.

The existing per-player K5 target remains 128. No existing K5 counter or unlock is cleared. No geometry, leaf, biome, or species-only heuristic establishes provenance.

## Recommendation

Use a hybrid of generation-position provenance and item-carried source tokens:

1. During an identified natural tree-feature invocation, record only surviving qualifying log positions in a persistent chunk attachment.
2. At successful harvest, consume the exact position token and attach a unique source token to the matching log drop before the item enters the world.
3. Preserve that token in the ItemStack data through item entities and containers. Credit only after a successful transfer into a Survival player's inventory.
4. Persist a world claim ledger so duplicate stack copies, repeated pickup, and another player's later collection cannot redeem one source log twice.
5. Fail closed for missing, corrupt, unknown-version, or unsupported provenance.

The project’s Fabric callbacks do not supply generation cause or resulting drops. A narrow worldgen source mixin, block-drop mixin, item data component, inventory-acquisition observer, and persistent claim ledger are needed. The design is feasible but invasive and version-sensitive. The most material ongoing cost is the monotonically growing anti-duplication ledger. Do not substitute block-break counting unless the user approves that semantic change.

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

A provenance component participates in ItemStack component equality. Vanilla can merge equal payloads; unequal payloads safely remain separate. Do not overwrite or discard distinct token lists to force a merge. A future custom union merge must handle every vanilla merge path and requires separate design/testing.

Vanilla split/copy operations may copy component data. Thus tokens in a stack are references to eligible source logs, not proof each copy owns the source. On actual inventory receipt, claim no more tokens than the number of units transferred and atomically mark each token in the persistent ledger. Split copies may carry overlapping tokens, but only one can redeem a token. Partial transfers claim only the transferred quantity; the unclaimed payload remains eligible.

Hopper movement preserves stack data but earns no credit; a later Survival player extracting the item does. Chest/container storage preserves provenance if the implementation preserves data components. Furnace fuel use destroys the log without acquisition credit. Custom backpacks and recipes that strip/rebuild stack data fail closed.

## Persistent schema proposal

All records are server-authoritative. Decode errors and unknown versions reject provenance; they never grant it.

### Chunk attachment, version 1

Identifier: moreminecrafts:k5_natural_tree_logs. Target: persistent chunk attachment via AttachmentRegistry.createPersistent.

Value is an immutable sparse record:

| Field | Type | Meaning |
|---|---|---|
| version | integer | Exactly 1; unknown values fail closed. |
| trees | list | Sparse feature records for this chunk. |
| feature_id | UUID | Unique successful natural feature invocation. |
| feature_key | namespaced ID, optional | Diagnostic configured/placed feature identity; not sufficient alone. |
| placer_key | namespaced ID | Reviewed trunk-placer registry identity. |
| log_positions | sorted packed positions | Exact surviving eligible logs in this chunk. |

Validate each position belongs to the chunk and still contains the expected log state. Remove the marker when a tracked position changes. Store no leaves, attempted writes, or scan results. Test attachment copying through ProtoChunk-to-LevelChunk conversion.

### Item component, version 1

Identifier: moreminecrafts:k5_natural_log_provenance. Codec fields: version integer (exactly 1) and bounded unique token list. Bound the list by legal item stack size plus a defensive codec limit.

Token identity is the tuple of world-instance UUID, dimension key, feature UUID, and source packed position. Do not use ItemEntity UUID, which can change during ordinary movement/merge.

### Persistent world claim ledger

Store a versioned sparse set of redeemed token identities in overworld/world SavedData. After a successful Survival inventory transfer, atomically claim up to the actual eligible transferred count, add that quantity to the existing player's K5 condition, and remove claimed tokens from that transferred stack. The server thread serializes the state transition; mutations mark data dirty.

The ledger stores redeemed logs, not all generated logs or loaded chunks. It grows monotonically with eligible acquired logs. It cannot safely be an in-memory map, expire, or reset on reload: old copies could then be credited again. Measure its size and save/load cost. If this growth is unacceptable, pause for approval of the semantic alternative below.

### Save compatibility

Existing player counters/unlocks remain unchanged. Old chunks without this attachment and old container stacks without this component are ineligible. Do not scan or migrate old chunks. Missing/corrupt/unknown-version chunk, item, or ledger data fails closed. A new version unable to read the claim ledger must disable K5 credit rather than reset it. Any future migration must be explicit and versioned.

## Hook integration plan

Exact method names/descriptors require validation against the resolved 26.3 sources before implementation.

1. Scope only the natural biome feature-decoration invocation of a configured tree; prove sapling growth is outside it.
2. Inspect actual tree configuration and trunk placer against reviewed 2×2 allowlist.
3. Capture successful log writes locally; commit surviving positions only after successful feature placement.
4. Invalidate a marker on any replacement or non-player removal, including piston/explosion/commands. Do not retain chunk objects in global maps.
5. At successful Survival break loot materialization, consume that exact marker and tag only its matching log output before ItemEntity insertion. Failed/canceled breaks preserve; Creative/non-player removals do not mint.
6. Register a bounded codec-backed custom item data component; verify ItemStack, ItemEntity save/load, split, pickup, and container persistence.
7. Observe successful Survival inventory additions across pickup and supported container transfer paths; compute actual delta, atomically claim tokens, and update existing K5 progress once.
8. Keep hopper-to-container transfer uncredited. Preserve vanilla non-merge behavior for differing payloads. Never credit an already claimed token.
9. Mark chunk/world data dirty on mutation; invocation context is transient and scoped. Chunk unload discards transient state; persistent attachment/component/ledger reloads.

## Performance and memory

No benchmark was run. Work is sparse and proportional to qualifying feature calls, placed logs, and acquired token lists:

- Worldgen: one scope/config check per candidate tree; capture only successful log writes for supported placers. No full-world scan.
- Chunk data: approximately 8 raw bytes per packed log position before codec/container overhead, plus per-tree metadata.
- Item data: one token per eligible log unit while unredeemed, bounded by the item max stack size.
- Ledger: one compact redeemed token per acquired eligible log, monotonically growing.
- Transient data: a feature-scoped local capture object and bounded operation context. No unbounded static maps or retained chunks.

Measure generation median/p95 and allocations with mixin on/off at representative tree densities, bytes per saved chunk, ledger size at 128/10,000/1,000,000 claims, and ledger decode/save duration. Agree a regression budget before implementation; no performance number is claimed here.

## Abuse and edge cases

- Sapling-grown 2×2 and player-built imitations receive no marker, regardless of final shape.
- Small natural trees and unknown trunk placers are excluded.
- Failed/canceled breaks preserve source marker. Successful Survival harvest transfers at most one token for the source log.
- Creative acquisition, explosions, pistons, automation, commands, and other non-player removals earn no credit and invalidate original block provenance.
- Another player who first receives an unclaimed token gets credit; later copies/recipients cannot reuse it.
- Fortune/multiple loot outputs: one token maximum for one original log block.
- Furnace fuel consumption earns no credit and does not transfer token to output.
- Old chunks/container items without provenance remain ineligible.
- Corrupt positions, duplicate tokens, oversized data, invalid chunk membership, absent feature records, and unknown schema fail closed with rate-limited diagnostics.
- A modded block-moving path not observed by mutation hooks is unsupported until integration exists.
- All chunk unload/reload, item entity unload/reload, world restart, and player reconnect paths must preserve the durable records.

## Test plan

These tests are proposed and were not executed. Existing PR #80 CI results do not verify K5 provenance.

### Deterministic unit tests

| Test | Expected assertion |
|---|---|
| Codecs and versions | Version 1 round-trips; unknown versions, oversized lists, malformed positions, and duplicates fail closed. |
| Position store | Insert/remove/consume is idempotent and chunk-scoped; empty old data stays empty. |
| Trunk allowlist | Verified 2×2 vanilla placers qualify; small/unknown placers reject. |
| Claim ledger | Repeated/competing claims have one winner; claimed count never exceeds transfer count. |
| Progress | 127 distinct acquisitions locked; 128th unlocks exactly once; duplicate does not advance. |
| Transfer arithmetic | Partial, failed, canceled, Creative, Survival, and mixed stacks produce exact credit. |
| Split/merge | Overlapping split copies cannot over-credit; equal payloads merge safely; different payloads remain distinct. |
| Compatibility | Missing component or unsupported version yields zero eligible units. |

### Minecraft 26.3 runtime/GameTests

| Scenario | Runtime assertion |
|---|---|
| Natural 2×2 tree | Real biome feature path creates markers only for final qualifying logs. |
| Sapling-grown 2×2 tree | Same feature/tree configuration from saplings creates no markers. |
| Small natural tree | No markers or item tokens. |
| Player-built imitation | No markers or item tokens. |
| Survival drop/pickup | One matching log token; credit only after successful pickup. |
| Creative acquisition | No progress; provenance invalidated. |
| Split/merge/duplication | Partial pickups credit actual quantity; clones/repeats never exceed unique token count. |
| Hopper/chest | Hopper moves tags but earns none; Survival extraction earns once, including after reload. |
| Backpack/container | Supported implementations preserve data; stripping implementation fails closed. |
| Furnace | Fuel use yields no new credit or tagged output. |
| Save/reload and chunk unload/reload | Chunk markers, item tokens, containers, and claim ledger survive; no duplicate minting. |
| Multiplayer | First successful Survival receipt gets token; other player and duplicate copy get none. |
| Threshold 127→128 | Locked at 127; exactly one unlock on 128th distinct acquired log. |
| Old chunks | Previously generated tree-shaped logs remain ineligible; no scan/migration. |
| Piston/explosion/automation | No player credit; source token invalidated and not recoverable at destination/replacement. |
| Modded tree | Each explicitly supported natural placer qualifies; unknown placer and sapling counterpart reject. |

Each runtime test must use a dedicated 26.3 server with pinned Fabric versions and installed mixins. Record the action and resulting state. Compilation alone is not runtime evidence.

## Decisions requiring approval

1. **Modded trees:** recommendation: vanilla allowlist plus explicit modded opt-in and runtime coverage; unknown placers fail closed.
2. **Acquisition meaning:** recommendation: any successful transfer into a Survival player's inventory, including taking from supported containers; hopper-to-container is not player acquisition.
3. **Global first claim:** recommendation: one world-global redemption; the first Survival player to receive a token owns that credit. Confirm logs should not count for multiple players.
4. **Creative handling:** recommendation: invalidate token when received into Creative inventory so it cannot later qualify after moving to Survival.
5. **Ledger growth:** strict repeat/duplication resistance needs durable source identities. Approve sparse monotonically growing world data and agree on supported scale. Smallest alternative: credit each qualifying log at successful Survival block break and consume its chunk position. That changes “acquired logs” to “logs harvested” and requires approval.
6. **Natural generation scope:** confirm whether structure-generated or other non-biome natural features count. Proposed hook covers biome tree-feature decoration only.
7. **Data-stripping mods:** recommendation: fail closed unless an integration explicitly preserves and tests the token.

## Implementation acceptance criteria

- Exact 26.3 natural generation and sapling call chains verified; runtime tests prove source distinction.
- Reviewed trunk-placer allowlist reflects actual 2×2 implementations.
- Only successful final natural log positions persist; replacement/non-player removal invalidates them.
- Exact source block mints at most one token on its matching actual log drop.
- Item, container, split, partial-transfer, multiplayer, unload/reload, and restart paths preserve/fail closed correctly.
- Player-grown, small, imitation, Creative, old-world, canceled, automated, and unknown-provenance cases never add progress.
- Persistent ledger prevents repeat/duplicate claims across players and restart.
- Existing K5 state remains compatible; unlock is exactly 127 locked/128 unlocked.
- All deterministic and runtime tests run on pinned dependencies; outcomes reported separately.
- Generation cost, allocations, chunk bytes, ledger growth, and save/load costs are measured against an agreed budget.

## Verification status

This task added no K5 code and ran no K5 tests. API evidence above is based on the declared project dependency and Fabric API sources at the pinned tag. Minecraft 26.3 mapped mixin descriptors and runtime call-path tests remain unverified and are implementation gates.

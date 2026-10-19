# Q1-C PR #128 P1/P2 repair — minimum safety design
Status: WIP / UNVERIFIED. A-approved crash-safe counting is the requirement; this document is not a claim of implementation or atomicity.
Base: `claude/q1c-skull-provenance-20261019` @ `6ba15ac2cb40cc919dfee41b24f7813231d11efb`. Parent #127; independent reviewer ChatGPT Work. Keep #128 and repair PR Draft; no main merge or Q1-D.

## Confirmed defect paths
P1: the entity's Boolean and player counter are in independently saved records. Restoring an older chunk after saving A's playerdata replays the same entity to B. Entity UUID is not a durable consumption record.
P2: Minecraft 26.3 Inventory.add can clear an input stack in full creative inventory without increasing inventory contents. Input decrement and return value do not prove receipt.

## P2 isolated change
Copy the original item/component identity before Inventory.add. Count matching items in the receiving inventory before and after the call using ItemStack.isSameItemSameComponents. Credit max(0, min(original count, after - before)). Leave vanilla pickup behavior and all game modes enabled. Cover full creative, creative with capacity, partial survival, existing-stack merge and distinct recipients through playerTouch, plus mismatched components.

## P1 proposed durable model
Use a world-scoped, server-only issuance/ownership journal as the authority; playerdata is a materialized projection. One random origin token per individual genuine skull, issued only from the existing Wither Skeleton death-loot scope. Journal issuance must succeed before attaching any token. The legacy Boolean alone never authenticates anything. Missing, unknown, foreign-world, repeated or malformed tokens fail closed.

Entity attachment stores an ordered list of per-unit tokens and world identity, not item custom data (inventory re-drops must not gain provenance). The list length must agree with stack count. Genuine/ordinary merge remains blocked. Genuine/genuine merges must move the exact token prefix matching the real moved count and retain disjoint remainder lists; mismatch invalidates provenance rather than minting replacement tokens. Partial pickups similarly consume exact prefixes. Entity NBT and dimension-change copying must explicitly preserve the attachment; portal/chunk restore replays old tokens, which the journal rejects. Do not infer token identity from a newly generated entity UUID.

Store all dimensions' journal in the server world root, never per-dimension. Hold the server world's exclusive lock throughout its lifetime. Close handles on server stop; do not use a static cross-world singleton. Backups/restores must include journal and playerdata together. Independent rollback/deletion of the journal is outside crash recovery and must disable tracking, not recreate an empty ledger for known authenticated entities.

## Ownership, projection and crash windows
A successful durable acquisition record must bind origin token to recipient UUID and that recipient's cumulative journal count. Replay is idempotent for that owner and rejected for another owner. Project the absolute cumulative count into a separate versioned player evidence key; never increment an already projected counter on recovery. Reconcile before quest evaluation on join and periodic refresh, including when playerdata is older than the journal. Preserve prior legacy progress separately; it cannot authenticate old item entities. Preserve existing claim-once XP behavior; this repair does not claim to make XP grants crash transactional.

A plain append-after-Inventory.add hook is insufficient: halt between actual transfer and append can leave a received skull with no durable owner. Append-before-Inventory.add is also insufficient: creative deletion/no receipt must not credit, and a pending claim must not be silently lost. A durable intent must therefore precede insertion, and receipt/projection evidence must be recoverable across the intent/insert/finalize windows. Before enabling this design, prove a receipt protocol against the actual playerdata serialization path (inventory plus a transaction receipt in one serialized player snapshot) or implement a replayable server-side delivery transaction. Pending intents must remain bound to their original recipient; do not release them to B or guess they succeeded. An unresolved intent is BLOCKED, not a completed acquisition. No implementation may advertise A's guarantee while silently losing A's progress.

## Disk and corruption behavior
Proposed journal format: versioned world header, sequence numbers, length-delimited records and checksums; append-only issue/intent/finalize records. Write all bytes, then FileChannel.force(true), before making finalized evidence visible to quest evaluation. Any write/force/close uncertainty poisons the in-memory service and stops new provenance and progress; never continue from cached success after I/O failure.
On restart validate header/world identity, ordering, checksums, token uniqueness and ownership consistency. Do not skip bad records, truncate and resume, or silently create a replacement ledger. Incomplete tail/interior corruption disables the service and preserves bytes for recovery. Recovery requires a verified consistent backup or explicit audited repair.
FileChannel.force(true) is a requested durability barrier, not proof of the storage provider's physical guarantees. Minecraft SavedData dirty/save scheduling and player save calls are not a cross-file atomic commit. Review mapped server/player/chunk serialization and file-replacement/flush paths before claiming otherwise. Process Runtime.halt tests do not establish OS-power-loss or hardware-fsync durability.

## Lifetime and growth
Retain spent-token tombstones for the world's entire lifetime: old chunk backups and portal copies can replay arbitrarily old tokens. No age-based deletion. Future compaction may fold records into a checksummed full snapshot retaining every issued/consumed identity and owner total, using force plus atomic replacement and directory durability where supported, with crash tests at each step. Until verified, use no compaction. Disk exhaustion fails closed and requires operator attention; size grows with skull issuance/consumption.

## Verification gates
First reproduce P2 and P1 on the reviewed base. Java 25/JUnit; actual Fabric playerTouch, hopper block tick, Player.drop, genuine/ordinary and genuine/genuine merges, partial/split multiplayer pickup, creative full/partial capacity, chunks, portals, legacy upgrade; normal 3-skull completion. Two independent Minecraft JVMs must cover A-player-only save followed by halt(77), journal-only boundary, player-save boundary and reversed save order. Preserve world and UUID evidence. Also run 76-quest/catalog invariants and existing combat/boss/raid/structure/research suites, server/client smoke, then observe CI.
A standalone journal test or single-JVM NBT round trip is not a substitute for these Minecraft restart gates.

## Environment observed at first checkpoint
Local checkout was clean on work @ 5f92307555ce5c486a2e0e8091f956bcbb6c6646 (older than #128). Remote #128 still matches reviewed HEAD and is Draft. Independent repair branch created via GitHub connector.
Repeated git fetch fails: "Failed to connect to proxy port 8080 ... Could not connect to server". Do not bypass network policy. Connected GitHub API remains usable for checkpoints. Java 25 is installed at /workspace/.setup/jdk, while default java is 21. Full source sync/build and actual Minecraft save-path review are pending.

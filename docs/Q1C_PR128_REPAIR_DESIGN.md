# Q1-C PR #128 P1/P2 repair — minimum safety design
Status: WIP. P2 has executed server/CI evidence below. P1 is BLOCKED and remains reproducibly defective. A-approved crash-safe counting is the requirement; the proposed P1 design below is not an implementation or atomicity claim.
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

## Environment observed at first checkpoint (historical)
Local checkout was clean on work @ 5f92307555ce5c486a2e0e8091f956bcbb6c6646 (older than #128). Remote #128 still matches reviewed HEAD and is Draft. Independent repair branch created via GitHub connector.
Repeated git fetch fails: "Failed to connect to proxy port 8080 ... Could not connect to server". Do not bypass network policy. Connected GitHub API remains usable for checkpoints. Java 25 is installed at /workspace/.setup/jdk, while default java is 21. Full source sync/build and actual Minecraft save-path review are pending.

## Repair checkpoint disposition

Repair PR: https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/130

P2 implementation copies the offered identity, counts matching inventory contents before/after Inventory.add, and bounds positive gain by the offered count. Return value and input decrease are not used as proof of receipt. No mode is excluded.

P1 production provenance is deliberately unchanged. There is no origin-token journal, no restart projection/reconciliation implementation, and no ledger-I/O-failure guarantee in this checkpoint. Legacy Boolean-only genuine entities are still accepted by the old implementation: the requested tokenless-world fail-closed upgrade is NOT implemented. Do not deploy this branch as a P1 fix or approve Q1-C.

The P1 design is blocked at the intent/receipt recovery contract: it has not established how a durable intent is resolved when insertion/finalization is interrupted. A journal-first owner/cumulative-count record can repair lost player projections, but does not by itself establish actual receipt. A journal-after-receipt critical section is another candidate; it requires proof that every relevant persistence path is excluded between insertion and commit. This checkpoint does not claim that proof or that these designs are impossible. A replayable receipt protocol or a verified serialization/linearization argument must precede implementation. Shipping an untested journal would falsely imply the accepted A guarantee.

### Minecraft 26.3 save-path review

Reviewed the actual downloaded minecraft-common.jar with Java 25 javap -c -p. SHA-256:
`d7f66778d514166918b204cac61da128ead475f4813276c12707a3d320c83552`.

- Inventory.add(int, ItemStack), bytecode offsets 63–87: when no more space exists and player.hasInfiniteMaterials() is true, it sets the input count to zero and returns true. This is the actual P2 cause.
- MinecraftServer.saveEverything, offsets 5–16: PlayerList.saveAll precedes saveAllChunks. These are distinct saves, consistent with the reproduced P1 window.
- ServerLevel.save, offsets 48–72: chunk save and entityManager.saveAll/autoSave are separate; saveLevelData uses SavedDataStorage.saveAndJoin/scheduleSave.
- PlayerDataStorage.save serializes the player, writes a temporary compressed NBT file, calls Util.safeReplaceFile, and catches/logs Exception before returning void. A successful caller return alone is not evidence that playerdata saved.
- NbtIo.writeCompressed(Path) uses a stream write/close. These inspected methods do not establish a shared player/entity/journal commit or physical fsync guarantee. No OS-power-loss or storage-hardware guarantee is claimed.

### Executed evidence

Code under test after P2/test repairs: `c4be3f8fa734636ebb12f50a26392358c8c310a5`.

- P2 RED: detached worktree `fe80eda` (reviewed production code plus the new tests). Actual Fabric server ran the three creative cases; full inventory failed with `held=0 credited=1`, and partial creative pickup also failed. Creative with capacity passed.
- P2 GREEN: normal Fabric CI on `dccd10971313baa5230970d34b916a221f035560`, run 39207163985, build job: all 312 required GameTests passed, plus clean JUnit/build/GameTest compilation. Subsequent source-head checks are on the PR.
- Local Java 25 server-only profile: 348 JUnit tests, 80 suites, zero failures/errors/skips; main and GameTest compilation passed. The profile is described below and does not establish a full client build.
- Final local server-only run on the code/test tree at c4be3f8, using a fresh disposable world: all 314 required GameTests passed in 46.40 seconds and Gradle succeeded. This includes the real Player.drop and scheduled hopper tests, plus existing combat/boss/raid/structure/research regressions. The opt-in P1 halt fixture was inactive in this run and failed separately as recorded below.
- Intermediate test repairs: the new forced-halt fixture initially used the obsolete ChunkPos(BlockPos) constructor; corrected to integer chunk coordinates. A new Player.drop roundtrip assertion initially assumed one held skull after manually repeated callbacks to an already removed entity; corrected to compare the inventory immediately before/after the actual drop roundtrip. Quest credit remains asserted as exactly one.
- P1 RED in two independent actual Minecraft JVMs, on the P2-fixed branch, using the same world and actual saved ground entity:

```text
QUEST_SKULL_HALT_WRITE pid=3995 entity=a9e50523-004f-485a-936f-37215f7443d0 A=1 heldA=1
game JVM finished with non-zero exit value 77 (Runtime.halt)
QUEST_SKULL_HALT_READ pid=4170 entity=a9e50523-004f-485a-936f-37215f7443d0 A=1 B=1 heldA=1 heldB=1
FAILED: B cannot credit the same restored skull a second time
```

This reproduces the supplied Work finding; there is no P1 GREEN/post-fix result. The fixture uses ServerLevel.save(null,true,false) before pickup, PlayerList.saveAll after pickup, then halt(77). It does not substitute an NBT round trip for world storage.

### Reproducing the forced-halt acceptance test

Use a clean disposable checkout/world. Never run the write phase against a valuable save: it intentionally terminates the game JVM. Filter:
`moreminecrafts-gametest:quest_wither_skull_game_tests_player_only_save_then_forced_halt_cannot_credit_second_recipient`.

1. Compile GameTests.
2. Set JAVA_TOOL_OPTIONS to `-Dfabric-api.gametest.filter=<filter> -Dmoreminecrafts.questskull.halt.phase=write`; run `./gradlew --no-daemon runGameTest`. Require the WRITE marker and game-process exit 77 (Gradle itself reports failure).
3. In a second invocation, keep the same build/run/gameTest/world and quest-skull-halt-fixture.txt, set phase=read, and run the same command. Require a distinct PID and identical entity UUID. Current expected acceptance result is RED with A=1,B=1; eventual P1 fix must make this A=1,B=0.

Without the phase property this opt-in destructive fixture is a no-op. A passing default suite does NOT mean forced-halt acceptance passed.

### Local dependency limitation and diagnostic profile

Git transport was restored by granting the command network access. Java additionally required proxy system properties and the installed CA trust store. The normal requested command `clean test build compileGametestJava runGameTest` was attempted before and after P2 and stopped in dependency resolution:
`maven.gegy.dev/.../lambdynamiclights-api-4.13.0+26.3.pom: 403 Forbidden`.
No dependency version, production build file, TLS checks, or host policy was changed.

For additional server evidence only, the temporary init script installed this dependency callback:

```groovy
gradle.beforeProject { p ->
    p.configurations.configureEach { c ->
        c.withDependencies { deps ->
            deps.removeAll { d -> d.group == "dev.lambdaurora.lambdynamiclights"
                    && d.name == "lambdynamiclights-api" }
        }
    }
}
```

Invocation: `JAVA_HOME=/workspace/.setup/jdk GRADLE_USER_HOME=/workspace/.setup/gradle ./gradlew --no-daemon -I <init-script> -x compileClientJava -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 test runGameTest`.
The initial diagnostic attempt without -x compileClientJava failed on the expected missing dynamic-light API imports; adding the exclusion allowed server compilation/testing. This profile uses real Minecraft/Fabric classes and server paths; it cannot establish client compilation, packaging, client smoke, or normal full-build acceptance. Normal CI uses the unchanged production dependency configuration.

### Outstanding acceptance and Work review

The catalog is unchanged from the reviewed base: 200 entries, 76 registered; skull ID, counter, target 3 and reward 1,100 XP remain intact.
P2 regression coverage includes creative full/partial/free capacity, full survival inventory with another recipient, genuine merges split between players, component mismatch, actual Player.drop, and scheduled hopper block ticking.

P1 journal issuance/consumption, journal-only halt, projection recovery, reversed save order, journal write/force failure, corrupt journal recovery, token-preserving merges/partial pickup, portal transfer and real item chunk unload/reload, and tokenless-world upgrade are NOT VERIFIED. Existing in-process item NBT coverage is not a substitute. Real-client/multiplayer/fortress live checks remain LIVE PENDING.

Work should review P2 inventory measurement and fixture validity, independently rerun the two-JVM P1 RED test, and review the missing receipt/linearization contract before any P1 implementation is called safe. Preserve #128/#130 as Draft and do not start Q1-D.

### GitHub-first operating rule

Apply the user's checkpoint rule to this task and subsequent supplied development prompts: isolate repair work, commit/push small valid units, create Draft PR early, preserve WIP/BLOCKED results even on failures or interruption, and verify status/HEAD/remote before ending. Never commit secrets, credentials, personal data, or large temporary worlds/logs. This document, tests and repair code are the reviewable checkpoint; scratch dependency profiles and generated worlds are excluded.

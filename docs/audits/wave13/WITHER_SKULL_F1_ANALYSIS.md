# Wither-skull provenance (F-1) — failure reproduction and protocol evaluation

Date: 2026-10-27. Status: **analysis only.** Nothing here registers the quest, changes the catalog, or implements a protocol.
`xpq.cmb.wither_skulls_3` stays **unregistered** (target 3, 1,100 raw XP), as F-1 and the authority index require. The Wave 12
checkpoint contains no skull code, and `XpQuestCatalogDataTest` already asserts the quest is unregistered
(`witherSkulls.registered()` is false).

Authority: F-1 in [PHASE2_OWNER_DECISIONS_2026-10-26.md](../../preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md). Only the **first pickup** of a skull
**genuinely dropped by a Wither Skeleton** counts; repeated pickup of the same skull does not count again. "PRs #128/#130 are not
accepted as release-ready; the reported crash-consistency defect must be corrected and tested before registration."

## 1. Reproduction of PR #130's forced-halt failure

| Item | Value |
|---|---|
| Code under test | PR #130 head `9c9f22577370cd0160dd2b43b0e92caa7832f420` (`codex/q1c-pr128-p1-p2-repair-20261019`), checked out in a throwaway worktree. It is based on the Stage 23 line, not on Wave 12 |
| Environment | Linux, Temurin 25.0.5+8, Gradle 9.6.0, Minecraft 26.3, Fabric Loader 0.19.5 / API 0.161.0+26.3, two separate Minecraft JVM processes sharing one world directory |
| Test | `QuestWitherSkullGameTests.playerOnlySaveThenForcedHaltCannotCreditSecondRecipient`, run exactly as the #130 design record prescribes (`-Dfabric-api.gametest.filter=… -Dmoreminecrafts.questskull.halt.phase=write`, then `=read`) |
| Write phase | `QUEST_SKULL_HALT_WRITE pid=9780 entity=002072fc-1e93-41ac-a302-426773846c57 A=1 heldA=1`; game JVM `finished with non-zero exit value 77` (`Runtime.halt`) |
| Read phase | `QUEST_SKULL_HALT_READ pid=10201 entity=002072fc-1e93-41ac-a302-426773846c57 A=1 B=1 heldA=1 heldB=1`; `FAILED: B cannot credit the same restored skull a second time` |
| Result | **REPRODUCED.** Distinct PIDs, identical entity UUID, two players each credited for one skull. Identical to the 2026-10-19 report (`A=1,B=1`; entity UUID differs per run because the fixture spawns a new entity) |

Logs: [`skull-repro/`](skull-repro/).

**Mechanism.** Player A picks up the skull: the entity is removed in memory, A's file gets the skull and the credit counter. The fixture then saves **only
player data** and the JVM dies. Chunk entity storage still holds the older state, so on restart the ground entity is back, with its genuine-origin Boolean
still set, while A's file already records the credit. B picks the entity up and is credited again. The skull's identity lives in one durable domain (the chunk)
and the credit in another (the player file). Vanilla saves them separately (`MinecraftServer.saveEverything` saves players before chunks; `PlayerDataStorage`
writes one temp file and replaces it).

## 2. Existing infrastructure in the Wave 12 checkpoint

The bounty system already solves a structurally identical problem, "consume one unique item exactly once across world, player and chunk storage":

| Piece | Where | Reusable for skulls |
|---|---|---|
| World two-phase ledger (`PENDING` → `COMMITTED`) in `SavedData`, synchronous `saveAndJoin` flush | `bounty/ContractLedger` | Yes: same shape, keyed by token instead of contract id |
| Prepare → apply → commit → finalize transaction with fault-injection points | `bounty/ContractTransactions` | Yes: the pattern, not the code |
| Player-attached receipts saved in the same file as the inventory | `bounty/ContractAttachments.Receipts` | Yes. This is the "inventory plus a transaction receipt in one serialized player snapshot" that #130's design says is missing |
| Recovery that decides roll-forward/rollback from the player's own receipt, idempotently, no clock | `ContractTransactions.recover` | Yes |
| Synchronous player save | `mixin/PlayerListAccessor.moreminecrafts$save` | Yes |
| Measuring real inventory gain instead of trusting `Inventory.add` (creative zeroes the stack without storing) | `ContractTransactions.insert`; PR #130's P2 fix | Yes: both solved the same Minecraft 26.3 quirk independently |
| Real two-JVM `Runtime.halt` matrix (13 boundary scenarios) | `ContractCrashGameTests`, `scripts/contract_crash/run_matrix.sh` | Yes: the validation harness to extend |
| Claim-once XP with rollback | `RawXp`, `QuestProgressService`; not crash-transactional by design | Not needed; the credit counter is quest evidence, not XP |

Differences that matter: a contract is consumed by one known player and its item is destroyed; a skull stays in play after pickup and can be dropped, traded,
copied by chunk restore, moved by hoppers and carried through portals. The token therefore has to live on the **item entity**, and a consumed token must be
refused forever, wherever its entity reappears.

## 3. Candidate protocols, evaluated exhaustively

Model: `tools/protocol-model/skull_protocol_model.py` (committed on the code branch). Three domains persist independently (chunk entity, player file,
world ledger). For every crash point and every independent persistence outcome (each domain may be at any version at or after its last explicit flush),
the server restarts, recovery runs, then a second player B tries the surviving entity. All 7 to 24 reachable crash states per protocol are enumerated.

| Protocol | Crash states | INV1 double credit | INV2 holds skull, no credit | INV3 credit, no skull |
|---|---:|---:|---:|---:|
| **P0** PR #130 as shipped: Boolean on entity, counter in player file | 7 | **1** | 0 | 0 |
| **P1** journal appended after `Inventory.add` | 15 | 0 | **4** | **3** |
| **P2** journal appended before `Inventory.add` | 15 | 0 | **2** | **5** |
| **P3** `PENDING` flushed → inventory + receipt + credit in one player file → player save (commit) → ledger commit | 24 | 0 | 0 | 0 |

- **P0 is the observed bug.** Its one violating state is "crash after the player write, chunk still has the entity", exactly the two-JVM result above. This ties the
  model to the real failure.
- **P1 and P2 each fail in both directions**, not one. P1: an uncredited skull in an inventory, or credit with no skull. P2: the same two in different states. This confirms
  #130's remark that neither append-before nor append-after is sufficient, and shows it is not repairable by choosing the order.
- **P3 violates nothing** in any enumerated state. The two properties it rests on: (a) the `PENDING` flush precedes any player write, so a durable receipt implies a durable
  `PENDING` record; (b) skull, credit and receipt change in one player file, so they are atomic with each other. Recovery needs only the player's own receipt.
- My first written expectation for P1/P2 (one violated invariant each) was wrong; the model corrected it, and the check now records what was measured.

**What P3 does not do, and cannot.** The physical skull stays non-atomic across chunk and player storage, as for every item in vanilla. In 8 of 24 P3 states a copy survives
on the ground after A durably holds the skull (a duplicate item, which carries **no credit**, so quest evidence stays exact), and in 2 states a durable chunk removal precedes A's
durable file (the skull is lost, with no credit; the player can earn another). The guarantee is therefore about **credit**, not item conservation.

## 4. Recommended protocol (needs owner approval before any implementation)

1. **Token.** At the existing Wither Skeleton death-loot bracket, attach a random token (persistent attachment on the item entity). No issuance journal: an unconsumed token
   needs no record, and a token with no entity cannot be used. This removes the issuance journal #130 proposed.
2. **No merging of genuine skull entities.** A Wither Skeleton drops at most one skull per kill, so each genuine entity holds one skull and one token. Cancelling merges
   (genuine with genuine as well as with ordinary, which #128 already does) removes #130's per-unit token lists and the partial-pickup prefix logic.
3. **Pickup transaction** (`ContractTransactions` pattern): ledger `PENDING(token, player)` and synchronous flush; add the skull and measure actual inventory gain; zero gain
   aborts without credit; otherwise credit counter and receipt in the player's attachment; synchronous player save (commit point); ledger `COMMITTED` and flush; clear receipt.
4. **Recovery** on join and periodically: a `PENDING` entry with the owner's receipt rolls forward, without it rolls back; idempotent.
5. **Refusal.** Any token that is `PENDING` or `COMMITTED` earns no credit for anyone. Entities without a token (chest, command, creative, re-dropped, legacy #128-era) never earn credit.
6. **Cost.** Two synchronous writes and one player save per credited pickup, at most three per player ever; negligible.

## 5. What is and is not proven

- **Proven (model, exhaustive):** the protocol logic under the stated persistence semantics, and that P0/P1/P2 fail.
- **Proven (real JVMs):** the P0 failure, and the contract protocol's recovery across 13 boundary scenarios (documented in `docs/bounty_contract/BC-03_IMPLEMENTATION.md`;
  not re-run here).
- **Not proven:** the Minecraft hooks themselves. The model does not exercise `ItemEntity.playerTouch`, hopper and dispenser intake, creative pickup, chunk unload/reload of
  tagged entities, Fabric attachment copy on dimension change, or two real players on a dedicated server. An implementation needs the two-JVM matrix extended with the skull
  boundaries (before/after prepare, after add, after player save, after ledger commit, the #130 player-only-save case, and chunk-saved-before-player), asserting `A+B ≤ 1`,
  `holds ⇒ credited` and `credited ⇒ holds`.
- Process halts do not establish OS-power-loss or storage-hardware durability; `FileChannel.force`-class guarantees are not claimed.

## 6. Minimum owner decision

**D-F1-1 (required).** Approve this guarantee as the meaning of "crash-safe counting" for the skull quest:

> A skull never earns more than one credit; a skull that is durably in a player's inventory is always credited; credit is never given without a durably held skull.
> A crash can still cost the player the skull and its credit (they must kill another Wither Skeleton), and can leave a duplicate uncredited skull item on the ground, exactly as vanilla
> can for any item.

If the owner instead requires physical skull conservation across crashes, no protocol can deliver it with vanilla storage, and the quest should stay unregistered.

Defaults that do not need a separate decision unless the owner objects: no merging of genuine skull entities; legacy and tokenless skulls never credit; synchronous writes per credited pickup.
Once D-F1-1 is approved, implementation is bounded: a skull transaction class modelled on `ContractTransactions`, the four item-entity hooks from #128, #130's P2 inventory-gain
measurement, and the extended halt matrix. Registration stays a separate step after that matrix passes.

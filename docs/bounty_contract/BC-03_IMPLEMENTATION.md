# BC-03 implementation record and BC-04 audit handoff

This document describes the code on the BC-03 implementation branch. It does not declare gameplay acceptance. Approved gameplay rules are in `../BOUNTY_CONTRACT_APPROVED_2026-10-22.md`; this file is the current implementation status and replaces earlier checkpoint wording. Classifications used below: CODE VERIFIED, SERVER VERIFIED, CLIENT VERIFIED, LIVE PENDING, PARTIAL, BLOCKED.

## Status

| Area | Status |
| --- | --- |
| BC-03A item-bound contracts, redeemed-ID protection, durable redemption | SERVER VERIFIED, including real process interruption at nine transaction boundaries (11 interrupted scenarios). Exactly-once is established for process termination, not for OS or power failure (see limitations). |
| BC-03B combat contribution, structure entry | SERVER VERIFIED against a real vanilla `StructureStart`; combat attribution is CODE VERIFIED. No live combat session was run. |
| BC-03C objective pools, loot, trades, special conversion | SERVER VERIFIED. Seller stock is exact. Restock of contract stock follows vanilla and is an open owner decision. |
| BC-03D contract screen and actions | Server side SERVER VERIFIED. Client rendering and input: compiled only. **LIVE PENDING**. |

## Defects found and fixed during stabilization

- **Redemption was not crash-safe**: the ledger claim and the inventory change were never flushed, so a crash could pay twice (player file new, ledger old) or make a contract permanently unredeemable (ledger new, player file old). Replaced by the durable transaction below.
- **Creative players lost overflow rewards**: `Inventory.add` zeroes the stack for a creative player when nothing fits, so a creative player completing a contract with a full inventory lost the reward instead of having it dropped. The inserted amount is now measured from the inventory. This predated the transaction work.
- **Earlier overflow evidence was vacuous**: an earlier in-process test and the first crash-matrix overflow rows were reported as covering a full inventory, but both consumed items that freed enough slots, or hit the creative quirk above, so no overflow was exercised. They now keep the delivery stack occupied and assert that the bonus item is absent from the inventory and dropped exactly once.
- **Overflow durability flush did not cover entities**: the first version flushed with `ServerChunkCache.save`, which writes chunk blocks but not the entity sections that hold dropped items, so a drop could be lost after the receipt said it was delivered. The strict overflow scenarios exposed it; the flush is now `ServerLevel.save`.
- **Overflow recovery could miss a surviving drop**: at join the chunk's entities may not be loaded yet, so the duplicate check could be blind. Recovery now waits until the player's position is entity-ticking and retries once a second.
- `ContractGameTests` had been missing from the GameTest entrypoints, so its tests never ran until the previous stabilization step.

## What the code does

- **Items and state**: `bounty_contract` and `special_contract` are unstackable items. A `moreminecrafts:contract` component carries a UUID, tier, status, fixed objectives with counters, and a fixed reward. Progress lives on the item, so it follows transfers, death drops, and restarts.
- **Acquisition**: trade and loot results are tier-only placeholders (`moreminecrafts:pending_contract`). `ContractMaterializer` rolls the objectives and reward when the placeholder first reaches a player's inventory, so a seller or container never shows rolled contents. A 5% Advanced-to-Special conversion applies at that moment.
- **Generation** (`ContractGenerator`, `ContractCatalog`): 22 vanilla hostile targets (21 ordinary, 1 strong) and 36 MM hostile targets (27 ordinary, the 9 approved strong); `elder_guardian`, `warden` and `ghast` are special-only and `wild_piglin` is withheld pending live aggression verification, with hostility verified from entity behavior; 74 ordinary and 11 rare BC-01 delivery IDs in the approved count bands; diamond delivery 2–4 and adamantium delivery 1, at most one high-value delivery per contract; seven accessible Overworld structures for exploration. Combat ordinary/strong weights are 100/0, 75/25, 50/50 by tier (owner-approved 2026-10-23), tier-gated targets are enforced per target, a strong draw with no valid candidate rejects and regenerates the whole objective set instead of substituting an ordinary target, and one contract never mixes combat dimensions.
- **Rewards**: approved rarity lists with item caps (`minecraft:diamond` ≤2, `moreminecrafts:titanium_ingot` ≤4, `moreminecrafts:adamantium_ingot` ≤1).
- **Loot** (`ContractLootIntegration`): one added pool in each of `abandoned_mineshaft`, `desert_pyramid`, `jungle_temple`, `trial_chambers/reward`, `ancient_city`, `bastion_treasure`, at 5/10/18% with tier weights 75/22/3, 45/40/15, 20/45/35. Existing vanilla pools are untouched. Four of these tables are also replaced by the built-in `trade_rebalance` datapack, and the modifier applies to whichever table is loaded.
- **UI** (`ContractScreen`): right-click opens one vertical screen with tier and status, objectives with counters, live delivery holdings, the fixed reward, accept, complete, and abandon. Abandon needs a confirmation and is available while Offered or Accepted. Every action is a `ContractActionPayload` naming the contract ID and the exact inventory slot; the server refuses it unless that slot still holds that contract, so a stale client cannot affect another copy of the same ID.

## Seller stock

One stock unit is one contract offer with `maxUses = 1`, added after vanilla produces its offers, by `ContractTradeStockMixin` on `AbstractVillager.getOffers`. It is idempotent: a sold-out offer still counts, so nothing is regenerated, and it also tops up villagers saved before this feature.

| Seller | Stock |
| --- | --- |
| Farmer, Fisherman, Fletcher, Weaponsmith, Toolsmith, Cartographer, Cleric at Apprentice (level 2) | Beginner 2 |
| ... at Journeyman (level 3) | plus Intermediate 1 |
| ... at Expert (level 4) and Master | plus Advanced 1 |
| Other professions, Novice villagers | none |
| Wandering trader (rolled once per trader) | Beginner 1–2, Intermediate 0–1, Advanced 0–1, uniform inside each range |

Offers are priced at the midpoint of the approved band (3 / 8 / 16 emeralds, PROVISIONAL), with xp 0 and a zero price multiplier so they neither level villagers nor drift. `ContractTradeGameTests` covers every eligible profession at levels 1–5 against these numbers, ineligible professions, real `Villager` and `WanderingTrader` entities through the mixin (400 wandering traders, confirming both ends of every range occur), exhaustion, idempotence, and vanilla restock.

**Restock**: vanilla `Villager.restock()` resets `uses` on every offer, so contract stock refills with vanilla restocking. The approved quantities do not say whether sold stock should refill. Changing it would be a new rule, so it is not implemented. This is an open owner decision.

## Durable completion transaction

Vanilla offers no transaction across the world ledger file, the player file, and chunk entity storage. Each file is replaced atomically on its own, but a crash can leave any older/newer combination on disk. Before this work the order was: claim the ID in memory, consume items, grant rewards, with nothing flushed. A crash could therefore leave the player file new and the ledger old (a surviving copy could redeem again), or the ledger new and the player file old (the contract permanently unredeemable), and an overflow drop could be repeated by a retry.

`ContractTransactions.redeem` now orders the steps around one commit point:

1. **Validate**: the contract is accepted, the live inventory satisfies it, the reward item exists, and the ID is absent from the ledger in any state.
2. **Prepare**: a `PENDING` ledger entry `{id, tx, player}` is written and flushed to disk before any inventory change.
3. **Apply**: delivery items and the contract are consumed, rewards are inserted, and a receipt `{tx, contract, overflow, delivered}` is added to the player's attachment. Inventory and receipt belong to one file, so they are atomic with each other.
4. **Commit**: the player file is saved synchronously. This is the commit point.
5. **Overflow**: only after the commit, rewards that did not fit are dropped, tagged with the transaction ID, and chunk and entity storage are flushed with `ServerLevel.save`; then the receipt is marked delivered and the player is saved again.
6. **Finalize**: the ledger entry becomes `COMMITTED`, the ledger is flushed, and the receipt is cleared.

`ContractTransactions.recover` runs on player join and then once a second while the player has an unresolved transaction. It uses only the player's own durable receipt:

| Durable state found | Meaning | Action |
| --- | --- | --- |
| `PENDING`, no receipt | the player file predates the transaction | abort: remove the entry, contract and materials intact, redeemable once |
| `PENDING`, receipt | the commit point was reached | roll forward: deliver any undelivered overflow, mark `COMMITTED`, clear the receipt |
| `COMMITTED`, stale receipt | cleanup was lost | clear the receipt |
| receipt, no entry | should not arise from the write order | heal the ledger as `COMMITTED` |

Overflow recovery waits until the player's position is entity-ticking, then skips the drop if an item entity tagged with the transaction already exists nearby. Recovery is idempotent, never needs a clock, and never grants anything that the player's own saved receipt does not describe.

Ownership: the redeeming player owns the transaction. While an entry is `PENDING` every copy of the contract is refused, whoever holds it, and abandonment is refused too. A concurrent request cannot interleave because the server thread serializes them; concurrent submission from eight threads yields exactly one success.

Retention: `COMMITTED` entries are kept for the life of the world, because any surviving copy of the item must stay refused (about 100 bytes per redeemed contract). `PENDING` entries and receipts exist only between prepare and finalize, or after a crash until the owner joins. The ledger reads the earlier bare-ID format as committed entries.

### Limitations (classified PARTIAL)

- **OS or power failure**: the guarantee covers termination of the server process while the operating system survives. It does not add `fsync`, and it relies on vanilla's file replacement.
- **Overflow entities** live in chunk storage, a third non-atomic domain. A crash between the post-drop flush and the durable "delivered" mark is resolved by the tagged-entity check. A duplicate is still possible if that entity is picked up or destroyed within that window, which is milliseconds wide. Eliminating it would need overflow held in the player file (a mailbox) instead of dropped, which changes Q53 and needs an owner decision.
- **Liveness**: a `PENDING` entry blocks its contract ID until its owner joins again. There is no offline resolution, so an owner who never returns leaves that one ID blocked. No reward is lost or duplicated meanwhile.

## Crash and restart evidence

`scripts/contract_crash/run_matrix.sh` drives `ContractCrashGameTests` in two real JVM processes sharing one world directory. The first process stops with `Runtime.halt` at one injected boundary: no shutdown hooks and no saves, which is how a SIGKILL looks to the files. The second process is a fresh JVM that reloads the world and the player from disk, joins, and checks the result. This is real process termination at deterministic boundaries; it is not a randomly timed external kill, and no synthetic exception is used as crash evidence.

Each scenario ends with exactly 5 emeralds and exactly 1 bonus item, the contract and delivery items consumed once, no receipt left, a refused duplicate copy, and unchanged counts after recovery runs two more times. In the overflow rows the delivery stack keeps its slot, so the bonus item must be absent from the inventory and dropped exactly once; a lost drop or a second drop fails the row. Every halting row also logs each boundary reached before the halt (`CONTRACT_CRASH_BOUNDARY`), which confirms the process took the intended path.

| Interruption point | Overflow | Ledger on disk at restart | Recovery |
| --- | --- | --- | --- |
| before prepare | no | absent | nothing to undo; retry pays once |
| after prepare | no | pending | rolled back; retry pays once |
| after consume | no | pending | rolled back; retry pays once |
| after reward | no | pending | rolled back; retry pays once |
| after player save | no | pending | rolled forward |
| after player save | yes | pending | rolled forward, overflow dropped once |
| after overflow spawn | yes | pending | rolled forward; the unflushed drop died with the process, so it is dropped once |
| after overflow flush | yes | pending | rolled forward; the flushed drop survived, its tag is found, and it is not dropped again |
| after overflow dropped | yes | pending | rolled forward |
| after ledger commit | no / yes | committed | stale receipt cleared |
| completed normally | yes | committed | reloaded unchanged |
| `PERSIST` | no | n/a | accepted contract with progress and a fixed rolled placeholder survive a restart, then transfer and complete |

In-process `ContractTransactionGameTests` cover duplicate completion, two players on one ID, a transfer during a pending completion, recovery invoked repeatedly, recovery with a full inventory, ledger formats, and receipt serialization. `ContractRuntimeGameTests` cover death drops (identity and progress preserved, finishable by another player), concurrent submission, rejected malformed requests, structure entry on a real vanilla `StructureStart` (entry after acceptance counts, revisit does not double count, proximity does not), and the full loop from trade purchase through acceptance, progress, and completion with generated contracts of all three tiers.

## Validation

| Check | Result |
| --- | --- |
| Unit tests | CODE VERIFIED, 256 pass |
| Fresh-world GameTests | SERVER VERIFIED, 145 pass |
| Real process interruption and restart matrix | SERVER VERIFIED, all scenarios in the table pass |
| `./gradlew build` and dedicated-server smoke | pass; the smoke run shows the mixins apply with no registry, datapack, or mixin errors |
| Contract UI in a real client | **LIVE PENDING** (below) |
| OS or power failure durability | not tested |

**Real client**: the client starts in this container but cannot create a render backend. OpenGL fails with "Couldn't find matching GLX visual" and Vulkan with "Installed Vulkan doesn't implement the VK_KHR_surface extension". The packages that would fix this (Mesa Vulkan drivers, `libglx-mesa0`, `xdotool`, `x11-utils`, `scrot`) cannot be installed because package indexes are not reachable. Without a window there is no real mouse input and no native screenshot, so no UI claim is made beyond compilation and the server-side action tests. CI's `client-visual-validation` job exercises selector screens through in-client callbacks, which this record does not count as contract UI evidence. The documented steps to finish it are: join a world with a contract, right-click it, accept, advance an objective, complete, abandon with confirmation, and reopen after transferring it to another player, capturing native screenshots.

**K5 natural-tree failures**: three K5 tick-0 fixture checks fail when `runGameTest` reuses a previously generated world. Fresh-world runs, used for all evidence here, do not show them. K5 production code is unchanged. Separately, a K5 chunk-persistence test ("didn't succeed within 180 ticks") timed out in 2 of about 9 fresh-world runs while this PR's in-process overflow tests performed a full level save in the same batch, and it never failed in the earlier runs without them. Those three tests now skip the save through `ContractTransactions.setEntityFlush` (a test seam; production and the real-process matrix keep the real flush), and 7 consecutive fresh-world runs then passed. That is consistent with the interference explanation but is not proof, so the K5 test's timing sensitivity remains a separate, unfixed item. No K5 production or test code was changed.

**Secret scan**: `full-history-secret-scan` fails on two `generic-api-key` findings in other remote branches (a territory design document and a model-delegation experiment). Neither commit is in this PR, and scanning this PR's own range (`origin/main..HEAD`) reports zero findings. The check was not suppressed and no history was rewritten.

## Provisional balance values

Combat ordinary/strong weights 100/0, 75/25, 50/50 (owner-approved 2026-10-23); vanilla strong threshold `MAX_HEALTH` ≥ 40; diamond delivery 2–4, adamantium delivery 1, one high-value delivery per contract; reward caps diamond ≤2, titanium ≤4, adamantium ≤1; offer prices 3 / 8 / 16 emeralds; uniform draw inside the wandering-trader ranges; uniform category choice among combat, delivery, and exploration; Overworld-only exploration pool; contract dimension choice for combat.

## Remaining PARTIAL and BLOCKED

- **LIVE PENDING**: the real client UI, as above.
- **PARTIAL**: the overflow-entity edge and OS-failure durability described under the limitations.
- **Not implemented**: offline resolution of a `PENDING` entry, and any reward mailbox.

## Owner decisions still required

1. Should sold contract stock refill with vanilla restocking? Today it does.
2. Should overflow rewards be held in a player mailbox instead of dropped? That would remove the last non-atomic domain from redemption but changes Q53.
3. Distribution inside the wandering-trader ranges, category weights, the exploration structure matrix, and whether price should vary with objectives inside the band (all currently provisional).

## BC-04 independent audit handoff

- Compare the final head with Q1–Q62, the owner overlays, and this file; verify the provisional values above and the BC-01 corrections (`minecraft:chain` is unregistered, and the structure IDs are `jungle_pyramid` and `mansion`).
- Re-run `scripts/contract_crash/run_matrix.sh` on a fresh world, and consider an externally timed `kill -9` during redemption as an independent check on the injected boundaries.
- Finish the UI check with a real window, real mouse input, and native screenshots on a machine with working OpenGL or Vulkan.
- Inspect final-head CI, including the secret scan, and confirm the two findings are still outside this PR's range.

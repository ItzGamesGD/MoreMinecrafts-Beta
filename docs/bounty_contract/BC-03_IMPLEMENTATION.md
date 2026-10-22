# BC-03 implementation and BC-04 audit handoff

This document describes the code on the BC-03 implementation branch. It does not declare gameplay acceptance. Approved gameplay rules are in `../BOUNTY_CONTRACT_APPROVED_2026-10-22.md`.

## Implemented code

- BC-03A: registered ordinary and special unstackable items; item-bound contract component with UUID, tier, fixed objective/reward data, status and progress; server action service for acceptance, abandonment and manual redemption; persisted redeemed-ID ledger; item reward overflow drop. A GameTest exercises transfer and duplicate-ID redemption. Death-drop preservation still needs a direct runtime check.
- BC-03B: server damage hooks record actual health loss attributable to players in the final 600 ticks; final blows qualify independently; one credited death advances every matching entry in every carried accepted contract. Server structure-piece lookup records distinct structure starts per objective after acceptance. Delivery availability is read from current inventory and duplicate delivery requirements aggregate.
- BC-03C policy code: approved price/payout bands, rarity thresholds, bonus quantity bands, structure appearance and conditional tier thresholds, 5% advanced conversion, approved professions/unlocks and seller stock. `ContractBalance.DELIVERY_EXCEPTIONS` holds the two **PROVISIONAL BALANCE** delivery quantities: `minecraft:diamond` 2–4 and `moreminecrafts:adamantium_ingot` 1.

## Pending before BC-03 is complete

- Acquisition generation, concrete objective pools, reward item pools, villager and wandering trader offers, and eligible loot integration are not wired. The registered contract items are currently not obtainable through normal gameplay. Exact reward identities/caps, eligible loot tables, and the converted special contract's emerald payout await owner decisions under `AGENTS.md` authority rules.
- BC-03D's single-screen UI, abandonment confirmation, and server action packets are not implemented. The server service is not yet reachable through normal gameplay.
- Combat hostile-target filtering currently depends on future generation pool selection. Reconcile every generated MM target against the 40 registered entities. Exclude `feral_wolf`, `cave_bat`, `giant_bee`, neutral piglins and all passive/friendly targets from ordinary combat.
- Special objective progress and availability need integration with only currently registered content. MM structures, meteors, and disabled events must remain excluded; direct special drops remain deferred.
- GameTests cover item transfer and duplicate redemption, but death drops, real restart, multiplayer network transfer, concurrent requests, and inventory overflow still need targeted runtime checks. The unit codec round trip is not a real server restart test.
- World ledger and player inventory save independently. A process crash between ledger claim, material consumption and reward grant can lose or duplicate effects. Exactly-once durability is **PARTIAL**, not proven. A transaction/recovery design and interruption tests remain necessary.

## Validation evidence at this checkpoint

| Check | Result |
| --- | --- |
| `/workspace/cloud-setup/gradle.sh test` after BC-03B | CODE VERIFIED; passed |
| `/workspace/cloud-setup/gradle.sh runGameTest` | SERVER VERIFIED for executed cases; 96/96 required tests passed |
| `/workspace/cloud-setup/gradle.sh build` after clearing generated `build/run/gameTest/world` | CODE/SERVER VERIFIED for executed cases; build and 96/96 GameTests passed |
| `build` with a reused generated GameTest world | Failed three existing K5 natural-tree tick-0 fixture checks; no contract GameTest failed. A fresh generated world passed. |
| Client UI and real gameplay | LIVE PENDING / NOT RUN; no BC-03D UI yet |
| Real persistence/restart, death drops, overflow, concurrent requests | PARTIAL / NOT RUN as listed above |

The PR #150 head inherited a failing full-history secret audit and a passing Fabric CI. BC-03 does not suppress the audit or expose matched values. Check final-head CI separately; inherited historical findings must be distinguished from any new findings.

## Continuation status (2026-10-22, BC-03C/D)

### Implemented in this continuation
- **Owner reward pool and caps:** `ContractRewards` implements the approved Common/High-grade/Rare/Legendary item lists, Q31/Q35 quantity bands, and item caps (`minecraft:diamond` ≤2, `moreminecrafts:titanium_ingot` ≤4, `moreminecrafts:adamantium_ingot` ≤1, all PROVISIONAL BALANCE). Both MM IDs are registered in `ModItems`.
- **Special contract economy:** a converted special keeps the Advanced price band (12–20) and payout band (25–50); special rarity distribution 20/45/30/5 is unchanged.
- **Single-screen contract UI (BC-03D):** right-click opens `ContractScreen` (tier/status, objectives with progress, live delivery holdings, fixed reward preview, accept/complete/abandon, abandonment confirmation). The screen re-reads the contract from the live inventory each frame. Client button state is a convenience only; `ContractActionPayload` is re-validated by `ContractService.act` on the server, and rejected actions send `moreminecrafts.contract.action_failed`.
- Authority synchronized: the owner decisions above are recorded in `../BOUNTY_CONTRACT_APPROVED_2026-10-22.md`.

### Test-wiring defect found and fixed
`ContractGameTests` was not listed in `src/gametest/resources/fabric.mod.json` `fabric-gametest` entrypoints. Its transfer and duplicate-redemption tests therefore did not run in earlier `runGameTest` results, despite the PR #151 body's earlier wording. The class is now registered; four contract GameTests execute and pass. A deliberate probe failure confirmed execution before it was removed.

### Superseded: previously blocked items (implemented in the final continuation below)
- **Objective generation pools.** Generating objectives needs an approved ordinary/strong/special classification for mobs (the BC-01 mob classifications are PROPOSED only) and approved tier-weight selection rules. Neither was in the owner decisions. Hostile-only filtering (neutral/tameable/passive exclusion) is recorded in the overlay, but its concrete mob list is not.
- **Acquisition wiring.** Structure loot integration (Easy/Medium/Hard sources and 5/10/18% rates), wandering-trader stock, and villager offers all produce contracts, so they depend on objective generation. Verified vanilla loot-table IDs are also not yet checked against the mapped jar.
- Direct special drops remain DEFERRED by design.

### Validation at this continuation
| Check | Result |
| --- | --- |
| `./gradlew test` (JDK 25) | CODE VERIFIED: 252/252 unit tests pass, including `ContractRewardsTest` (4) and the special-band test |
| `./gradlew runGameTest`, fresh generated world | SERVER VERIFIED: 100/100 required tests pass, including the 4 contract GameTests |
| Reused generated world | Earlier K5 natural-tree tick-0 failures reproduced; not changed. Fresh-world runs are the valid evidence |
| Contract UI rendering and clicks | CLIENT: compile only (`compileClientJava`). LIVE PENDING |
| Persistence across real restart, death drops, overflow, concurrency, crash interruption | NOT RUN in this continuation; still PARTIAL per the record above |

## Final continuation status (2026-10-22, BC-03C/D completion)

### Implemented
- **Combat pools** (`ContractCatalog`): 25 vanilla hostile ordinary mobs and 3 strong vanilla mobs, 28 MM ordinary mobs including `moreminecrafts:wild_piglin`, and the 9 approved MM strong mobs. Exclusions: vanilla neutral, tameable, boss, raid-only, summoned, and conversion-only mobs; MM `feral_wolf`, `cave_bat`, `giant_bee`. Hostility is verified by `ContractCatalogGameTests` (`Enemy`, not `NeutralMob` or `TamableAnimal`). Vanilla strong means non-boss `MAX_HEALTH` of at least 40: `elder_guardian`, `hoglin`.
- **Combat weighting** (`ContractGenerator.combat`): Beginner 90/10, Intermediate 70/30, Advanced 50/50 ordinary/strong, PROVISIONAL BALANCE. One dimension per contract: Beginner Overworld; Intermediate Overworld or Nether; Advanced any of the three (uniform). Empty pools fall back to the other strength.
- **Delivery**: BC-01 vanilla IDs in ordinary (16–32 / 32–64 / 64–128) and rare (2–4 / 4–12 / 8–24) bands. Diamond fixed 2–4 and adamantium ingot fixed 1, at most one high-value delivery per contract. `minecraft:chain` is **not registered** in 26.3 and was removed. The BC-01 catalog wrongly listed it.
- **Exploration**: seven accessible Overworld structures (`village_plains`, `desert_pyramid`, `jungle_pyramid`, `mineshaft`, `stronghold`, `mansion`, `trial_chambers`), count 1 / 1–2 / 2–3. Note the vanilla IDs `jungle_pyramid` and `mansion`, not `jungle_temple` or `woodland_mansion`.
- **Acquisition**: trade and loot results are pending placeholders carrying only a tier (`moreminecrafts:pending_contract`). `ContractMaterializer` rolls the objectives and reward on first server-side inventory pickup, and 5% Advanced-to-Special conversion applies there. Each tier's objective slot count follows the approved rules; category selection is uniform (PROVISIONAL).
- **Loot** (`ContractLootIntegration`): one added pool per approved table. Verified 26.3 IDs: `minecraft:chests/abandoned_mineshaft`, `minecraft:chests/desert_pyramid` (Easy); `minecraft:chests/jungle_temple`, `minecraft:chests/trial_chambers/reward` (Medium); `minecraft:chests/ancient_city`, `minecraft:chests/bastion_treasure` (Hard). Appearance 5 / 10 / 18%, tier weights 75/22/3, 45/40/15, 20/45/35. Existing pools are untouched. The `trade_rebalance` built-in datapack also replaces four of these tables, and the modifier applies to whichever table is loaded.
- **Villagers**: data-driven trades for Farmer, Fisherman, Fletcher, Weaponsmith, Toolsmith, Cartographer, and Cleric. Beginner at level 2 (2 entries), Intermediate at level 3 (1), Advanced at level 4 (1). Entries are appended to the vanilla `minecraft:<profession>/level_N` tags, and vanilla trades are kept.
- **Wandering trader**: Beginner appended to `common`, Intermediate and Advanced to `uncommon`.
- **UI**: offered and accepted abandonment, slot-scoped actions, and confirmation (see the validation table).

### Provisional balance values (all PROVISIONAL BALANCE)
- Combat ordinary/strong weights 90/10, 70/30, 50/50.
- Vanilla strong threshold `MAX_HEALTH` ≥ 40.
- High-value delivery exceptions: diamond 2–4, adamantium 1; one high-value delivery per contract.
- Reward item caps: diamond ≤2, titanium ingot ≤4, adamantium ingot ≤1.
- Trade prices fixed at band midpoints: Beginner 3, Intermediate 8, Advanced 16 emeralds.
- Structure appearance 5/10/18%, tier weights as above.

### Partial or not done
- **Stock:** trade and wandering-trader stock is approximated through vanilla random selection from tags. Exact per-trader stock needs a code hook. PARTIAL.
- **Real client UI:** LIVE PENDING (see below).
- **Crash-safe exactly-once:** NOT proven. See below.

### Validation (final continuation, 2026-10-22)
| Check | Result |
| --- | --- |
| `./gradlew test` (JDK 25) | CODE VERIFIED: unit tests pass |
| `./gradlew runGameTest` in a fresh world | SERVER VERIFIED: 125/125 required GameTests pass, including catalog registration, hostility, strength rule, loot appearance (20,000 seeded rolls per table) and tier shares, untouched control chest, profession stock, wandering-trader tags, offered abandonment, stale-slot rejection, redeemed-copy refusal, pending materialization, serialization round trip, competing deliveries, and full-inventory overflow with a single payout |
| `./gradlew build` in a fresh world | Build successful |
| Real client UI | LIVE PENDING. Not run. The repository's own visual-validation harness reports that the Minecraft window is not discoverable for physical input on this runner class, so no real mouse-driven evidence was captured. The GameTests above are server-side only. |
| Crash interruption during redemption, real process restart, death drops in a live session | NOT RUN. No crash-injection harness exists. See below. |

### Completion integrity analysis (not crash-safe)
- The redeemed-ID ledger is a world `SavedData`. Player inventories are saved in separate per-player files. Vanilla provides no transaction across those files.
- Crash windows: if a crash occurs after the ledger claim but before the player file saves, the contract may reappear unredeemed or be lost, depending on save ordering. If a copy of a redeemed ID exists in another player's saved file, a copy can redeem again after reload. Duplication is therefore not prevented across a crash, and reward delivery is not exactly-once.
- Focused mitigations implemented: slot-scoped actions, ledger refusal of abandonment for redeemed IDs, and a serialization round-trip test.
- Proposed mechanism (NOT implemented): a world-level write-ahead journal recording `{id, player, phase}` before any inventory mutation, with a reconciliation pass at player load. It requires a crash-injection test (process kill during redemption) to establish any PASS. Until then, exactly-once is PARTIAL.

## BC-04 independent audit handoff

- Compare the final BC-03 HEAD with Q1–Q62 and the later owner overlay in the authority document.
- Verify exact runtime entity/item/structure IDs, hostile classification, tier access, quantity caps, reward identities, and no unavailable objective.
- Sample trade stock, loot appearance, tier conditional distributions, special conversion and bonus rarity/quantity distributions.
- Exercise acquire → inspect → accept → progress → complete → reward, acquire → accept → abandon, and transfer → progress → complete in real clients.
- Verify damage source/actual-health attribution, environmental death, healing, excess damage, multiple contributors, duplicate objectives and multi-contract sharing.
- Test structure entry, revisits after acceptance, nonmatching structures and distinct structure instances.
- Test delivery inventory competition, failed redemption, duplicate IDs, simultaneous requests, overflow, death drops, full restart and deliberate interruption during redemption.
- Record server/client/live evidence separately and inspect final-head CI, including the unsuppressed secret audit. Do not treat a build as final gameplay acceptance.

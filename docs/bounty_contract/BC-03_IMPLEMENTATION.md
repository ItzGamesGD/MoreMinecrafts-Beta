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

### Still NOT implemented (blocked on owner design decisions)
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

## BC-04 independent audit handoff

- Compare the final BC-03 HEAD with Q1–Q62 and the later owner overlay in the authority document.
- Verify exact runtime entity/item/structure IDs, hostile classification, tier access, quantity caps, reward identities, and no unavailable objective.
- Sample trade stock, loot appearance, tier conditional distributions, special conversion and bonus rarity/quantity distributions.
- Exercise acquire → inspect → accept → progress → complete → reward, acquire → accept → abandon, and transfer → progress → complete in real clients.
- Verify damage source/actual-health attribution, environmental death, healing, excess damage, multiple contributors, duplicate objectives and multi-contract sharing.
- Test structure entry, revisits after acceptance, nonmatching structures and distinct structure instances.
- Test delivery inventory competition, failed redemption, duplicate IDs, simultaneous requests, overflow, death drops, full restart and deliberate interruption during redemption.
- Record server/client/live evidence separately and inspect final-head CI, including the unsuppressed secret audit. Do not treat a build as final gameplay acceptance.

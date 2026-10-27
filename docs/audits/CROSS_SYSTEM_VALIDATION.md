# Cross-system validation — Wave 12 (2026-10-27)

Labels: **CODE VERIFIED** (inspection of wiring/semantics), **AUTOMATED TEST VERIFIED** (named tests pass on the integrated tree), **LIVE VERIFIED** (human interactive play — none claimed), **BLOCKED**, **FAILED**, **NOT TESTED**.

Code tree validated locally: `0333a5d2` (integration branch after the arena port and doc restoration). The later Wave 12 commits change only `docs/`. Remote CI on the final checkpoint HEAD is recorded in [FINAL_CHECKPOINT_ACCEPTANCE.md](FINAL_CHECKPOINT_ACCEPTANCE.md).

Environment: Linux container, Temurin 25.0.5+8, Gradle 9.6.0 wrapper, Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Xvfb, Mesa 25.2.8 llvmpipe (Vulkan `lvp_icd.json`).

## Local validation runs

| # | Command | Tree | Result |
|---|---|---|---|
| L1 | `./gradlew build -x test` (baseline sanity) | `c47c69f3` (Wave 11) | PASS |
| L2 | `./gradlew --no-parallel clean test build compileGametestJava runGameTest` | `209c251e` (arena port; same code as final) | PASS — JUnit **463/463** (0 failed, 0 skipped); server GameTests **"All 425 required tests passed"**; resource validators (accessory, fishing, Flame Sword ×2, lower equipment, Stage 6 data) PASS; only ERROR line in the log is the expected missing `server.properties` of the GameTest run dir |
| L3 | `scripts/structures/validate_ancient_city_arena.py` | final | PASS |
| L4 | `scripts/structures/generate_ancient_city_arena.py --check` | final | NBT payload identical; gzip header OS byte differs under local Python 3.13 (environmental — see INTEGRATION_GAPS G-1) |
| L5 | Arena real terrain generation: `runServer` with seed 2632027 + `mmarena validate` via RCON (workflow steps reproduced) | `0333a5d2` | PASS — `ARENA_WORLDGEN_VERIFIED`; same bounds/city chunk/72 vanilla pieces as PR #83; ordinary city without arena also observed |
| L6 | Arena native client capture ×2 (`capture_client.sh 0/1`, `check_screenshots.py`) + visual inspection | `0333a5d2` | PASS (automated non-blank/detail check) + inspected by the auditor: arena ring, seating, sculk inside a vanilla city. Not an owner art acceptance |
| L7 | `xvfb-run … ./gradlew runClientGameTest` (all 8 client GameTest classes, 1920×1080) | `0333a5d2` | PASS — BUILD SUCCESSFUL; markers: accessory reconnect, normal death/respawn, keepInventory, dimension (3), rapid-click convergence, shift equip/unequip, 15 `VISUAL_LAYOUT_OK`, anvil catalyst click/enhance/insufficient/maximum/repair (×3 scales), fishing close left-click. Evidence: [wave12-20261027/client-gametest-markers.txt](wave12-20261027/client-gametest-markers.txt) |
| L8 | Static wiring checks (scripted) | final | PASS — every `initialize()/register()` has a caller; all 111 main/client mixins resolve to classes and every `@Mixin` class is listed; every `@GameTest` class registered (60 server + 8 client entrypoints); no dangling `moreminecrafts:` model/texture/data id; no duplicate item/block/entity registration literals; 0 `@Overwrite`; 3 `@Redirect` on 3 distinct targets |
| L9 | Independent XP Quest catalog recount (Python, not the project's tests) | final | PASS — 200 unique ids; NOW 163 / VERIFY 17 / FUTURE 20; 180,000 active + 29,100 future = 209,100; 75 registered = 73,850 XP; 0 VERIFY registered; `xpq.cmb.wither_skulls_3` unregistered, target 3, 1,100 XP (F-1) |

Not run locally: the K5 two-process restart, three-process accessory persistence, mapped-source probes, Batch 3 crafting probe, client visual validation workflow and secret scan. These run as GitHub Actions on the PR. See the acceptance record.

## Interaction matrix

| Interaction | Evidence | Status |
|---|---|---|
| Research unlocks ↔ XP Quest tracking (owned research value counts toward quests, retroactive) | XpQuestRulesTest.researchValueIsOwnedApprovedCost, XpQuestTrackingGameTests.ownedResearchValueAndCountsAreRetroactive | AUTOMATED TEST VERIFIED |
| Research purchase ↔ raw-XP transactions (exact debit, rollback) | ProgressionPurchaseGameTests, ExactXpTransactionGameTests, ProgressionPurchaseGateTest | AUTOMATED TEST VERIFIED |
| Quest claim ↔ raw XP (claim once, rollback when the grant fails) | ExactXpTransactionGameTests.claimIsOnceExactAndRefusesInvalidRequests, QuestClaimGateTest | AUTOMATED TEST VERIFIED |
| Accessory enhancement ↔ XP cost (exact raw XP, atomic with materials, duplicate inputs rejected) | AccessoryEnhancementGameTests (incl. accessoryPaymentIsAnExactRawXpTransaction, postMilestoneCostAndRejectedDuplicateInputsAreAtomic), real-client anvil markers (L7) | AUTOMATED TEST VERIFIED |
| Ordinary enhancement ↔ XP cost | EnhancementTransactionGameTests.costIsFixedRawXpRegardlessOfLevel | AUTOMATED TEST VERIFIED |
| Accessory persistence ↔ inventory movement, death, reconnect, dimension, restart | AccessoryLifecycleGameTests, AccessoryEquipmentGameTests, AccessoryAbilityGameTests.cooldownSurvivesDeathRespawnAndDimensionTravel, accessoryCooldownSurvivesProcessRestart, real-client markers (L7), accessory-cooldown-restart workflow (CI) | AUTOMATED TEST VERIFIED |
| Bounty rewards ↔ economy (research discount + Hero of the Village, vanilla restock) | ContractPricingGameTests.researchDiscountStacksWithHeroOnContractsWhenPresent, ContractTradeGameTests, ContractEconomyTest | AUTOMATED TEST VERIFIED |
| Bounty exploration ↔ generated structures | ContractExplorationGameTests, ContractExplorationTest; arena pieces become part of the vanilla `minecraft:ancient_city` start, so the structure identity used by entry objectives is unchanged (CODE VERIFIED) | AUTOMATED TEST VERIFIED + CODE VERIFIED |
| Custom mob kills ↔ combat attribution (quests and contracts) | DamageCreditTest, ContractCatalogGameTests.allFortyMmEntitiesAreAccountedForExactlyOnce, QuestCombatGameTests, ContractIsolationGameTests | AUTOMATED TEST VERIFIED |
| Structure generation ↔ world persistence (arena) | AncientCityArenaGameTests; L5 real generation; vanilla structure-start persistence (no custom save data added) | AUTOMATED TEST VERIFIED (generation); save/reload of generated arena chunks NOT TESTED separately |
| Ancient City arena ↔ Ancient Heart acquisition (F-2: route must remain) | Arena template contains no containers or loot tables; vanilla `ancient_city` loot pools unchanged; T2CoreAcquisition untouched | CODE VERIFIED |
| UI actions ↔ server authority (research purchase, quest claim, contract actions) | Payload handlers run on the server thread (`ResearchPurchaseService` rejects off-thread calls); QuestClaimGateTest; ProgressionCanvasInputTest; client-visual-validation (CI) | AUTOMATED TEST VERIFIED |
| Save/load, death, reconnect, dimension (quests, K5, contracts) | QuestCombatRestart/QuestLootRestart/QuestMonumentRestart/QuestOreRestart/QuestStructureRestart/QuestVaultRestart GameTests, K5NaturalTreeProbeGameTests.k5ProgressSurvivesVanillaRespawn, ContractRuntimeGameTests.deathDropKeepsContractIdentityAndProgress, ContractCrashGameTests, k5-production-restart (CI) | AUTOMATED TEST VERIFIED |
| Multiplayer isolation | AccessoryLifecycleGameTests.twoPlayersKeepIndependentLoadoutsAndCategoryRules, VillagerTradingGameTests.discountIsPlayerSpecific…, ContractIsolationGameTests, QuestMonumentGameTests.everyRealCombatParticipantIsCredited… | AUTOMATED TEST VERIFIED (mock players); two real clients NOT TESTED |
| Duplicate rewards | ExactXpTransactionGameTests (duplicate claim), ContractIsolationGameTests.acceptanceIsIdempotent…, AccessoryAbilityIntegrationGameTests.registeredServerTickHandlersRunExactlyOncePerTick, Research23AXpCollectionGameTests (one award per orb) | AUTOMATED TEST VERIFIED |

## Defect checks requested by the task

| Check | Result |
|---|---|
| Duplicate registrations | None (L8). The arena adds a pool contribution, not a registry entry; the contribution guard applies once per registry load (`applied` set) |
| Lost event handlers | None (L8) |
| Conflicting mixins | None: one new mixin (`StructureTemplatePoolMixin`, accessor-style append), no other mixin targets `StructureTemplatePool`; no `@Overwrite`; redirects on distinct targets |
| Duplicate rewards | Covered above |
| Unreachable UI actions | Client GameTests (L7) and client-visual-validation exercise selector, inventory accessory slots, anvil and fishing close |
| Invalid recipe/resource identifiers | None (L8) plus Gradle validators |
| Serialization incompatibilities / migration regressions | No serialized format changed in Wave 12 (arena adds no saved data). Existing codec/restart tests pass (58 tests) |
| Incorrect client/server execution | Arena probe and capture harness are env-gated (`MOREMINECRAFTS_ARENA_VALIDATION`, `MOREMINECRAFTS_ARENA_VISUAL`) and are off in normal play; the pool contribution runs on dynamic-registry load on both sides |
| Missing textures or models | None for new content (the arena uses vanilla blocks). Pre-existing deferred: `relic_catalyst` has no item model (art deferred by authority) |
| Stale tests that pass without exercising behavior | Every `@GameTest` class is registered (the #166 class of defect is absent). The arena GameTests assert the loaded pool's weight and the template attach; L5 re-checks real generation independently |

## Live validation

**LIVE VALIDATION PENDING** for everything that needs human interaction or perception: arena visual/feel acceptance in normal survival play; T2 combat feel; accessory ability feel; Quest/Research UI physical input at GUI scales 1–4; two real clients; Windows `runClient`. No LIVE PASS is claimed. Screen callbacks, mock players and headless or Xvfb runs are labelled AUTOMATED.

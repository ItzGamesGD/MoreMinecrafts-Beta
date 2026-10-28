# Feature integration matrix — Wave 12 (2026-10-27)

Status: audit record, not design authority. Authority status is copied from the routes in `docs/CONTENT_AUTHORITY_INDEX.md`.

- Baseline: `checkpoint/wave11-unified-20261027` @ `c47c69f3`. Final integrated tree: `checkpoint/wave12-unified-20261027` (exact SHA in [FINAL_CHECKPOINT_ACCEPTANCE.md](FINAL_CHECKPOINT_ACCEPTANCE.md)).
- "Present" is never inferred from file existence alone. Each Complete row was checked for the runtime entry point (initializer call, registry entry, mixin registration in `moreminecrafts.mixins.json`, network payload, GameTest entrypoint registration). Cross-checks run on the integrated tree: every `public static void initialize()/register()` has a caller; every listed mixin class exists and every `@Mixin` class is listed; every `@GameTest` class is registered in `src/gametest/resources/fabric.mod.json`; no dangling `moreminecrafts:` model/texture/data reference; no duplicate registry IDs; no `@Overwrite`; three `@Redirect`s on three distinct targets.
- Test evidence column: names refer to classes in `src/test` (JUnit) and `src/gametest` (server/client GameTests). Results on the final tree: see [CROSS_SYSTEM_VALIDATION.md](CROSS_SYSTEM_VALIDATION.md).
- Verification labels: **CODE** = code/wiring verified by inspection; **AUTO** = automated test passes on the integrated tree; **LIVE PENDING** = needs interactive/human play.

Legend for *Equivalence*: **Verified** — branch behavior present (same or evolved implementation, checked against authority); **Superseded** — branch behavior intentionally replaced by newer authority; **Recovered** — was missing, integrated in Wave 12; **Missing** — absent; **Blocked** — absent by explicit owner decision.

## 1. Equipment, weapons, tools, lifestyle

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| EQ-01 | Enhancement Stone + ordinary Enhancement (T1/T2 max +20, milestones +5/10/15/20, exact raw-XP cost) | #2–#11, #30, #68, #115 | Approved (Stage 2 reconciliation) | Complete | `enhancement/*`, `T1AnvilScreen` | Verified; old +30/+40 tests on pre-recovery lineage Superseded | RawXp | EnhancementRulesTest, EnhancementOperationTest, EnhancementTransactionGameTests, MaceEnhancementTest | Keep |
| EQ-02 | T1 weapon extensions: Dagger, Greatsword (sweep VFX), Gauntlet | #69 | Approved (progression-loop checkpoint) | Complete | `equipment/BasicWeaponItem`, `GreatswordItem`, `GauntletItem` | Verified | — | WeaponBalanceTest, GreatswordSweepVfxTest, LowerEquipmentRegressionGameTests (greatsword ×4) | Keep |
| EQ-03 | T2 elemental roster (Flame, Water, Wind, Earth Mace, Lightning) with right-click skills; Wind Step and F/Shift+F removed | #12–#13, #31–#33, #66 | Approved; LIVE PENDING; VFX DEFERRED | Complete | `special/elemental`, `special/flame`, `special/wind`, `special/vfx` | Verified; old F/Shift+F `InputBindingTable`, `WindStepAbility` Superseded | ElementalHitProgress | ElementalHitProgressTest, Earth/Flame/WindCombatMathTest, FlameSwordModelTest | Keep |
| EQ-04 | Flame Sword production asset + six-face handheld model | #37, #38, #61 | Approved (immutable PNG) | Complete | `textures/item/flame_sword*`, model | Verified (PNG blob identical to Stage 13) | — | FlameSwordModelTest, `validateFlameSword*` Gradle tasks | Keep |
| EQ-05 | Dark Energy Staff | Stage 20 | DEFERRED (gameplay and Enhancement) | Partial (item only) | `special/elemental/DarkEnergyStaffItem` | Verified as deferred | — | — | Defer |
| EQ-06 | Lower equipment: Backpack, Miner Lantern (LambDynamicLights bridge), Mining Drill, Potion Injector, Parachute | #62–#65 | Approved (Utility Equipment Finalization) | Complete | `equipment/*`, `LanternDynamicLightBridge` | Verified | Lantern slot container | LowerEquipmentRulesTest, LowerEquipmentGameTests, **LowerEquipmentRegressionGameTests (16, restored from #65 in Wave 12)**, InventoryRegressionClientGameTest | Keep; Large Backpack icon defect is a known open visual item |
| EQ-07 | Portable Furnace incl. Stage 23-B persistence (slots + progress follow the item) | #62, #105 | Approved (2026-10-16 amendment) | Complete | `PortableFurnaceItem/Menu/Contents` | Verified; #96 close-and-drop test Superseded | Furnace research | PortableFurnaceResearchGameTests, PortableFurnaceRestartFixture, WeaponBalanceTest.portableFurnaceRetains… | Keep |
| EQ-08 | Mob capture tool (T3, 30 durability, no bosses) | #109 | Approved (V4) | Complete | `MobCaptureRuntime`, `CapturedMobEggItem` | Verified | Research `mob_capture` gate | ResearchContent/Expanded GameTests | Keep |
| EQ-09 | T1 enchantment skill tree, Book Option, villager/book integration; Feather Boots kept (Q3) | #51–#56, #67–#68 | Approved | Complete | `enchantment/*`, `EnchantingProgressionScreen` | Verified | XP | EnchantmentMathTest, EnchantmentResourcesTest, T1ProgressionGameTests | Keep |
| EQ-10 | Adamantium equipment (upgrade chain, stats, templates) | design #122/#131 | Approved v1 design; not implemented | Absent | — | Missing (never implemented on any branch) | Caravan, worldgen F-3 | — | Owner: authorize implementation task |

## 2. Knowledge and Research

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| KR-01 | 70 free Knowledge nodes, event collectors, K5 natural-tree rule, K14 | #75–#87 | Approved (V4; Stage 2-A/B; K5 integration) | Complete | `progression/Knowledge*`, K5 probe | Verified | Progression attachment | KnowledgeAcquisition/Placement/OutputLifecycle/RemainingCollectors/WorldAction/AnimalFeed GameTests, K5NaturalTreeProbeGameTests, k5-* workflows | Keep |
| KR-02 | Research catalog (V3 + V4, 87 nodes, seven paid roots), prerequisite validation incl. V3 purchases | #87–#89, #111 | Approved (Stage 22 completion authority) | Complete | `ResearchCatalog`, `ResearchPurchaseService.purchasedResearch` | Verified (#88 fix present) | Knowledge, RawXp, inventory | ResearchV3CatalogTest, ProgressionPurchaseGameTests, ProgressionPurchasePolicyTest | Keep |
| KR-03 | One-click server-authoritative Research purchase; seven-sector picker; four-card selector | #76, #156 | Approved (UI 2026-10-10/11/12) | Complete | `ProgressionSelectionScreen`, `ProgressionBrowseScreen`, `ResearchPurchaseRequestPayload` | Verified | Networking | ProgressionPurchaseGateTest, ProgressionCanvasInputTest, client-visual-validation | Keep; LIVE PENDING (physical input) |
| KR-04 | Research effects Batch 1 (.02 Lumbering, .03 Mineral Conservation, .18 Beekeeping, .23 Fishing Technique) | #90, #92 | Approved | Complete | `ResearchContentRuntime`, `ItemStackMixin`, `ResearchBeehiveMixin`, `FishingChallenges` | Verified (handlers queried per ID) | Research levels | ResearchContentRulesTest, ResearchContentGameTests.beekeeping…, Research23AReconciliationGameTests | Keep |
| KR-05 | .19 Fisheries Study (largest-remainder, lower-grade tie) | #92, #93, #97 | Approved | Complete | `FishingChallenges`, `Research23AEffectRules` | Verified | Fishing | Research23AEffectRulesTest.fisheries… (4), FisheriesStudyGameTests | Keep |
| KR-06 | .55 sprint/swim exhaustion 8/16/24% | #92, #93 | Approved | Complete | `ResearchMovementExhaustionMixin` | Verified | — | Research23AEffectRulesTest.survival…, Research23AMovementExhaustionGameTests | Keep |
| KR-07 | .67 XP Control instant collection 4/6 blocks via vanilla `playerTouch`, once per orb, composes with .64 | #92, #93 | Approved 2026-10-15 — **authority text was missing; restored in Wave 12** | Complete | `ResearchExperienceOrbMixin` | Verified; authority **Recovered** | .64 | Research23AXpCollectionGameTests (4) | Keep (doc restored) |
| KR-08 | Batch 2: .17 livestock, .28 axe first strike, .64 XP acquisition, wall_jump, fall_roll | #91 | Approved | Complete | `ResearchLivestockRuntime`, `ResearchAxeCombatRuntime`, `ResearchXpAcquisition`, `ResearchMovementRuntime` | Verified | — | Research23ABatch2GameTests | Keep |
| KR-09 | Furnace infrastructure + .06/.07/.11 + .08 precision crafting; actual interaction outranks GUI open | #95, #96, #98 | Approved (Batch 3 amendment) | Complete | `furnace/*`, `KnowledgeFurnaceMixin` | Verified; #96 "latest open wins" Superseded | Furnaces | FurnaceResearchStateTest, FurnaceResearchInfrastructureGameTests, PrecisionCraftingRulesTest, k5-stage23a-batch3-crafting-probe | Keep |
| KR-10 | Expanded effects .38/.39/.40/.44/.45/.47/.51/.62 | #98, #108 | Approved | Complete | `ResearchCreeperMixin`, `ResearchSpiderLeapMixin`, `ResearchEndermanMixin`, `ResearchStatusEffectMixin`, `ResearchBatch2Runtime` | Verified | Mob tags | Research23AExpandedGameTests | Keep |
| KR-11 | .33 consecutive sweeps, .61 splash/lingering, .50 Riding (horse), mob_capture gate | #109 | Approved | Complete | `ResearchConsecutiveSweep`, `ResearchSplash/LingeringPotionMixin`, `ResearchRidingRuntime` | Verified | — | Research GameTests | Keep |
| KR-12 | .53 Villager Trading 5/10/15% first-trade, ceil discount, min 1 | #110, #116, #123/#124 | Approved (2026-10-26 ceil rounding) | Complete | `ResearchVillagerDiscount` | Verified; branch round-to-nearest test Superseded | Villager trades | ResearchVillagerDiscountTest.villagerDiscountRoundsReductionUp…, VillagerTradingGameTests | Keep |
| KR-13 | .16 Soil Study (cultivator attribution, hydration 6 at L3, trample, seed yield) | #110 | Approved | Complete | `SoilStudyRuntime` | Verified | Chunk attachment | SoilStudyRulesTest (5), SoilStudyGameTests (14) | Keep |
| KR-14 | Remaining research nodes (DATA_ONLY/PARTIAL/BLOCKED per 87-node audit; .35 Tactical Shooting, .58, .65 blocked) | Stage 22 | Mixed: some approved, some deferred | Partial | 87-node audit doc | Verified as documented partial | — | — | Defer (owner/design) |

## 3. XP Quest

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| XQ-01 | 200-entry catalog, 209,100 XP total, 180,000 active, 75 registered (73,850 XP), VERIFY unregistered | #114, #121, #140, #156 | Approved (2026-10-17 amendment) | Complete | `data/moreminecrafts/xp_quest/catalog.json`, `XpQuestCatalogData` | Verified independently in this audit (Python recount) | — | XpQuestCatalogDataTest, QuestDefectRegressionGuardTest | Keep |
| XQ-02 | Event tracking: combat, structures, loot, raid, monument, dragon rematch, ore positive provenance, C3 thresholds | #119–#121 | Approved (six tracking policies) | Complete | `XpQuestTracking`, `XpQuestHooks`, `Quest*Mixin` (≈20) | Verified; #119 chunk-generator ore mixin Superseded by #120 positive provenance; nine #119 registrations intentionally withdrawn by #120 | Mixins | QuestCombat/Raid/Monument/Structure*/Loot/Vault/Ore/DragonRematch GameTests + restart variants | Keep |
| XQ-03 | Claim-once server-authoritative exact raw-XP claim with rollback; unclaimed quests as XP store | #115, #156 | Approved | Complete | `QuestProgressService`, `RawXp`, `QuestClaimRequestPayload` | Verified | RawXp | ExactXpTransactionGameTests, QuestClaimGateTest | Keep |
| XQ-04 | Custom Quest screen (ProgressionBrowseScreen/TreeCanvas), three node states, completion toast, keyboard nav, semantic zoom | #126, #129, #135, #156, #164, #168, #169 | Approved (2026-10-24 UI amendment) | Complete | client `Progression*`, `QuestStructurePreview`, `QuestTreeLayout/Structure` | Verified (Wave 7 A/D supersede earlier click/contrast code) | Networking | ProgressionCanvas*Test, QuestPreviewIsolationTest, QuestTreeStressTest, client-visual-validation | Keep; LIVE PENDING (owner visual) |
| XQ-05 | Wither-skull first-pickup provenance and `xpq.cmb.wither_skulls_3` registration | #127, #128, #130 | Rule approved (F-1); implementation **not release-ready** | Absent | branch-only | Blocked | Item-entity mixins; durable receipt protocol | #130's opt-in P1 forced-halt test is RED | Owner/design decision (see gaps) |

## 4. Accessories and relics

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| AC-01 | 12 accessories, categories, five slots left of armor, persistence, death drop, two-player isolation | #142–#149, #160, #161 | Approved (2026-10-16, UI 2026-10-23) | Complete | `accessory/*`, `AccessoryInventoryScreenMixin` | Verified | Attachments | AccessoryCatalogTest, AccessoryEquipment/Lifecycle GameTests, accessory client GameTests, accessory-cooldown-restart (3-process) | Keep |
| AC-02 | Accessory Enhancement +0…+10, exact raw-XP + material cost, catalyst milestones, anvil catalyst slot | #153, #167 | Approved (2026-10-17; layout 2026-10-23 conditional) | Complete | `AccessoryEnhancement*`, `AnvilCatalystUiLayout` | Verified | RawXp | AccessoryEnhancementRulesTest/CatalogTest, AccessoryEnhancementGameTests.accessoryPaymentIsAnExactRawXpTransaction, AccessoryAnvilClientGameTest | Keep; catalyst art/recipe/acquisition DEFERRED |
| AC-03 | Accessory visual assets (32×32 identities) | #152 | Approved (2026-10-22) | Complete | `textures/item/*` | Verified | — | AccessoryVisualClientGameTest, magenta scan | Keep |
| AC-04 | Active abilities: Star Covenant, Snow Charm, Ember Ring, Ring of Life (Option A), Deep Sea Pearl; persistent per-ability cooldowns | #171, #173–#175, #183, #185 | Approved (2026-10-25) | Complete | `StarCovenantAbility`, `SnowCharmAbility`, `AccessoryAbilities`, `AccessoryCooldownRuntime` | Verified | Cooldown attachment | AccessoryAbilityGameTests (death/respawn/dimension/restart), AccessoryAbilityIntegrationGameTests, AbilityCooldownsTest, AccessoryAbilityInputTest | Keep; LIVE PENDING (feel) |

## 5. Bounty, contracts, economy

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| BC-01 | Physical contract items, tiered generation, rarity distributions, rewards, manual redemption | #151, #162, #177, #178 | Approved (2026-10-22 + BC-03 overlay + 2026-10-25) | Complete | `bounty/*` | Verified | Loot integration, trades | ContractGenerator/Catalog/Rewards/Distribution/Balance tests, ContractGameTests | Keep |
| BC-02 | Combat contribution attribution (hostile-only), duplicate progress | #151, #178 | Approved | Complete | `DamageCredit`, `ContractObjectives` | Verified | Mob kills incl. custom mobs | DamageCreditTest, ContractCombatPoolTest, ContractIsolationGameTests, ContractCatalogGameTests.allFortyMmEntities… | Keep |
| BC-03 | Exploration objectives (1 / 1–2 / 2–3 distinct structure instances), structure names on screen | #178, #180 | Approved | Complete | `ContractExploration*`, `ContractScreen` | Verified | Structures (incl. Ancient City, now with arena pieces) | ContractExplorationTest, ContractExplorationGameTests, ContractStructureNamesTest | Keep |
| BC-04 | Pricing: base prices before Hero of the Village and research discount; vanilla restocking | #178 | Approved | Complete | `ContractTrades`, `ContractPricing` | Verified | KR-12 | ContractPricingGameTests.researchDiscountStacksWithHero…, ContractTradeGameTests | Keep |
| BC-05 | Persistence: crash/restart, death drop keeps identity | #177 | Approved (integrity) | Complete | contract data components / ledger | Verified | — | ContractCrashGameTests, ContractRuntimeGameTests.deathDropKeepsContractIdentityAndProgress, ContractLedgerTest | Keep |
| EC-01 | Rare caravan (tiers, runes, contract sales rules Q2/F-5) | design #122/#131 | Approved design; not implemented | Absent | — | Missing (never implemented) | Contracts, runes | — | Owner: authorize implementation task |
| EC-02 | Fisherman custom fish sale, Multi Catch | #69 | Approved (Oct 7 overlay) | Complete | `FishermanFishSale`, `FishingLogic` | Verified | Fishing | FishingSaleRulesTest, FishingLogicTest.multiCatch… | Keep |

## 6. Fishing, materials, alchemy

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| FI-01 | 12 species, Reel/Minnow/Crankbait I–III, Grade I–V, length-dependent weight, minigame, equipment screen | #17, #36, #60, Stage 3 recovery | Approved (Stage 3 reconciliation) | Complete | `fishing/*`, `FishingRodScreen` | Verified; Tier IV–V recipes and old independent rolls Superseded | — | FishingLogicTest, FishingTimingTest, FishingEquipmentTest, WaterBodyValidatorTest, FishingRodCloseClientGameTest, `validateFishingResources` | Keep |
| MA-01 | Materials and ores (Titanium, Tungsten, Mythril, Etherium, Adamantium), deepslate variants | #15, #27, #28, #35 | Approved; Adamantium worldgen amended by F-3 | Partial | `worldgen/ModOreGeneration`, `data/.../worldgen` | Titanium–Etherium Verified; **Adamantium generates Y 8–24 uniform, size 3 — F-3 requires Y 8–22 biased to 15, veins 1–2, fewer blocks than Ancient Debris** | — | `validateStage6Data` | Owner: authorize F-3 implementation + matched sampling |
| AL-01 | Shock Potion | #29 | Approved | Complete | `alchemy/*` | Verified | Brewing | ShockTimingTest | Keep |

## 7. Mobs, bosses, structures, world events

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| MB-01 | Bomb and Leap Zombie, Mining Giant | #19, #20 | Approved | Complete | `zombie/*` | Verified | — | ZombieVariantRulesTest, MiningGiantRulesTest | Keep |
| MB-02 | Skeleton King boss | #22 | Approved | Complete | `skeleton/*` | Verified | — | SkeletonKingRulesTest, SkeletonKingGameTests | Keep |
| MB-03 | 40 vanilla-model custom mobs: AI, spawning, loot aliasing, production textures; Wild Piglin keeps identity | #69, #72, #104 | Approved (roster + 2026-10-08 identity rule) | Complete | `mob/*`, `CustomMobRenderers` | Verified; main's conversion test Superseded by identity rule | Bounty catalog | CustomMobRosterDefinitionTest, CustomMobGameTests | Keep |
| ST-A | **Ancient City Arena** (additive pool contribution, weight 1/47, rigid template, degradation processor) | #83 | Approved first prototype (Ancient City expansion §2-A, §4) | Complete | `worldgen/structure/*`, `StructureTemplatePoolMixin`, `structure/ancient_city/arena.nbt` | **Recovered** in Wave 12 | Vanilla Ancient City pools | AncientCityArenaGameTests (2), local real-worldgen seed 2632027 + native captures, ancient-city-arena-validation workflow | Keep; owner visual acceptance pending |
| ST-B | Pyramid (Tomb Guardian, Earth Mace recipe with Ancient Heart F-2), Pillager HQ, Spider Colony, ST-05+ | #147, #181, design branches | Design records; implementation **not authorized** | Absent | — | Not applicable (no implementation on any branch) | — | — | Await owner authorization |
| ST-C | Underground kingdom worldgen probe | #117 | Experiment evidence only | Experiment | branch | Not applicable | — | — | Keep on branch |
| T2-ACQ | T2 core loot (Trial, Bastion, Ancient City) + one-time Ocean Monument claim (correct `minecraft:monument` id) | #69 | Approved identities; chances provisional | Complete | `T2CoreAcquisition`, `MonumentRewardData` | Verified (main's `ocean_monument` id bug fixed in checkpoint) | Loot tables | MonumentRewardDataTest, QuestMonument GameTests | Keep; LIVE PENDING |
| WE-01 | World event: pillager patrols (5 variants + captain) | #122 | Design finalized; not implemented | Absent | — | Missing (never implemented) | — | — | Owner: authorize implementation task |
| SKY | T2 weapons / Sky Feather / sky islands context | `docs/sky-islands-design-context-20261027` | Design in progress (not authority) | Absent | `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` | **Recovered** (doc preserved) | — | — | Continue design |

## 8. Platform: networking, persistence, CI, security

| ID | Feature | Original source | Authority | Impl. | Location | Equivalence | Dependencies | Test evidence | Action |
|---|---|---|---|---|---|---|---|---|---|
| PL-01 | Client/server payloads (progression snapshot/delta/sync, research purchase, quest claim, contract action, fishing, potion injector, movement jump) | various | Required by approvals | Complete | `network/*` (11 payloads) | Verified (registered in `ModNetworking`) | — | server-smoke, client-smoke, client GameTests | Keep |
| PL-02 | Save/load, death, reconnect, dimension, restart persistence | various | Required | Complete | attachments + codecs | Verified | — | 58 restart/codec/persist tests; k5-production-restart (two-process), accessory-cooldown-restart (three-process), ContractCrashGameTests | Keep |
| PL-03 | GameTest orphan registration repair (15 research GameTests) | #166 | — | Complete | gametest `fabric.mod.json` | Verified: every `@GameTest` class registered | — | 425 server GameTests | Keep |
| PL-04 | Real SDL mouse input + OS-level input verification | #168, #188 | — | Complete | client mixins, live-input helpers | Verified | — | client-visual-validation | Keep |
| PL-05 | CI workflows (Fabric CI, K5, accessory client, visual validation, tooling readiness, security scan) | #54, #77, #155, #170, #186 | Governance | Complete | `.github/workflows` (10 + arena) | Verified | — | PR checks | Keep |
| PL-06 | Security: pre-public Actions hardening, gitleaks allowlist, paced exposure audit | #77, #155, #186 | Governance | Complete | workflows, `.gitleaksignore`, `scripts/security` | Verified; #184 parallel variant Superseded | — | full-history-secret-scan | Keep |
| PL-07 | Stage 23-B cloud live-client QA harness (separate `clientQa` source set) | #106, #107 | Approved as tooling design | Not integrated | branch | Tooling (not product) | — | its own workflow | Retain on branch |
| PL-08 | Free-model delegation experiment; experimental skills; global sweep-texture override | #141, branches | Excluded from production | Not integrated | branches | Rejected for production | — | — | Retain/decide |

## Totals

| Measure | Count |
|---|---:|
| Distinct features identified | 60 |
| Already integrated in Wave 11 and verified present (Complete) | 47 (includes KR-07, whose code was present but whose authority text was missing) |
| Recovered or repaired in Wave 12 | 3 — ST-A Ancient City Arena (code + resources + tests), KR-07 approved authority text, SKY design record; plus restored regression coverage for EQ-02/EQ-06/EQ-07 (16 GameTests from #65) |
| Intentionally excluded with authority evidence | 4 — ST-B (implementation not authorized), ST-C (experiment only), PL-07 (test tooling, not product), PL-08 (excluded from production) |
| Approved design never implemented on any branch (new work, not an integration gap) | 3 — EQ-10, EC-01, WE-01 |
| Partial by authority (remainder explicitly deferred) | 3 — EQ-05, KR-14, MA-01 |
| Unresolved | 2 — XQ-05 wither-skull provenance (blocked by owner decision F-1 and an open P1 durability defect); MA-01 Adamantium worldgen differs from approved F-3 values (requires an authorized implementation + matched-sampling task) |

Arithmetic: 47 + 2 newly present (ST-A, SKY) + 4 + 3 + 3 + 1 (XQ-05) = 60. MA-01 is counted once (Partial) and also listed as unresolved.

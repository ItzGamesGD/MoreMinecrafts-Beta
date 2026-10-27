# XP Quest owner decisions — 2026-10-21 (content review checkpoint)

**Authority:** explicit owner decisions in the XP Quest 200-entry design review (2026-10-21). This is a **design-decision log**, not evidence that gameplay code, catalog JSON, localization, tests or live play were changed. Q1-C snapshot remains pinned to `17e980912d636b0572a3f25b24995ffde31c754c`. Do not retroactively treat the catalog A proposals as owner approvals.

**Scope:** modify content in the future implementation branch after full 200-quest design review; preserve all 200 quest slots where possible. All raw-XP rewards are **provisional, unchanged** pending a separate cross-quest economy pass. All status/registration and test gates remain as currently implemented unless explicitly stated otherwise. Do not merge any PR or mark Ready based on this document.

## A. Combat ecology — 3 reviewed quests

- `xpq.cmb.cave_dwellers` (MID, 500 XP): target remains six distinct kills, one of each: `cave_zombie`, `cave_skeleton`, `cave_creeper`, `small_spider`, `cave_bat`, **`cave_bug`**. Replace the non-cave `swamp_spider`. These are the six cave-spawn roster variants. Preserve reliable server-side kill attribution; live verification still required.
- `xpq.cmb.bestiary_25` (LATE, 1,350 XP): keep 25 **distinct** MoreMinecrafts monster types killed; a long-running collection objective. Verify that 25 eligible types naturally spawn and their kills actually count. `cave_bat` is AMBIENT, so do not silently assume it is part of a monster-only count.
- `xpq.cmb.habitats_5` (LATE, 1,100 XP): **actual habitat exploration plus combat**, not simply checking which mob species was killed anywhere. Five progress groups, each requires a kill **in the designated biome**: (1) desert/desert spider or sand slime; (2) swamp or mangrove swamp/swamp zombie, swamp spider or mud slime; (3) snowy habitat/frozen zombie or ice spider; (4) fungal habitat **restricted to mushroom fields or lush caves**/fungal zombie (a fungal zombie killed in a swamp does not count); (5) forest habitat/wild slime (a wild slime killed in a swamp does not count). Confirm precise valid biome tags/IDs at implementation and test every negative case. Each completed group can be credited once. Current `stat_killed_groups_each` tracker is insufficient without kill-position biome evidence and likely needs a new evidence path. No retroactive credit from species-only kill statistics.

All three are reviewed content candidates for NOW/registered, but **design approval does not equal technical/live acceptance**; reconcile stale ecology documentation with current JSON after implementation review.

## B. Exploration — 24 existing quest slots reviewed

### Replace all six distance-count quests with actual exploration objectives

Keep existing IDs only as stable migration keys if safe; player-facing names/objectives become:

| Current ID | New Korean title | Tier | Provisional XP | Required objective |
|---|---|---|---:|---|
| `xpq.exp.walk_10k` | 지하 탐험가 | EARLY | 250 | Enter both a naturally generated lush cave and dripstone cave underground. |
| `xpq.exp.walk_50k` | 지하 세계의 비밀 | LATE | 1,150 | Enter Deep Dark biome, Ancient City interior and naturally generated Mineshaft interior. |
| `xpq.exp.sail_20k` | 일곱 바다의 탐험가 | LATE | 1,150 | Visit a warm-ocean coral reef and a frozen-ocean iceberg area; explore a deep-ocean underwater site and underwater ruins. Exact feature predicates still require implementation specification. |
| `xpq.exp.ride_10k` | 육로의 개척자 | LATE | 1,000 | While riding a land mount (e.g. horse/camel), enter desert, badlands, savanna and snowy biome groups. |
| `xpq.exp.elytra_25k` | 하늘에서 본 세계 | LATE | 1,100 | While actually gliding with elytra above mountain peaks, jungle, cherry grove and badlands, satisfy each region's height/flight predicate. Freeze safe height thresholds at implementation. |
| `xpq.exp.travel_250k` | 세계의 끝을 향하여 | ENDGAME | 3,150 | Enter the interiors of Ancient City, Woodland Mansion, Nether Fortress, Bastion Remnant and End City. |

Distance alone must never complete these objectives. Confirm natural-world predicates and any vanilla ID/structure validity. New trackers, server-side observations and negative tests are necessary. Existing distance stats must not be imported as evidence for the replacement objectives. Review overlap with `structures_15`, `grand_expedition` and other exploration goals; **overlap is allowed**, but objectives should remain distinct.

### Structure entry — approved attribution and overlap

- `xpq.exp.villages_5`: player-facing title **다섯 문화의 마을**, with a separate required visit to each of five **village types**: plains, desert, savanna, snowy, taiga; not five arbitrary village instances. Existing tier MID, 550 XP provisional.
- `xpq.exp.structures_15`, `xpq.exp.grand_expedition`, `xpq.exp.villages_5`: credit **actual entry inside generated structure pieces**, not proximity. Old Knowledge proximity observations cannot be retroactively interpreted as entry and must not award progress. Correct the stale catalog A `R` claim and player copy, without rewriting old save evidence.
- **Shared progress:** one valid structure entry may advance multiple quests in parallel; players must not re-enter the same place solely because a different quest tracks it. Each quest grants its reward only upon its own completion. Valid forward-time structure-entry records can be shared across quest evaluations.

### Biome milestones — approved

- `xpq.exp.biomes_25` (MID 500 XP) and `xpq.exp.biomes_40` (LATE 1,150 XP): retain unique biome discovery milestones and share observed biome evidence.
- `xpq.exp.rare_biomes`: player-facing name **희귀 경관**; **all five** of `mushroom_fields`, `ice_spikes`, `eroded_badlands`, `bamboo_jungle`, `cherry_grove` required; change tier **MID -> LATE**. 600 XP provisional. Concurrent milestone progress allowed.
- `xpq.exp.biomes_all_overworld`: player-facing name **완전한 지도**; complete the frozen, versioned roster of **all naturally generated survival-accessible biomes across Overworld, Nether and End**, excluding `the_void` and non-survival biomes. Show denominator, discovered count and missing list. Trustworthy persisted Knowledge visit evidence may count retroactively (unlike structure proximity evidence). Keep **VERIFY/unregistered** until version-specific exact roster, evaluator and live availability are validated. Existing `overworld` identifier conflicts with three-dimension objective; audit references/migration before renaming stored ID.

### Other exploration content — disposition

- **Retain current objectives** (existing tiers/rewards provisional): `first_structure_loot`, `mineshaft_loot`, `dungeon_loot`, `temples`, `ocean_relics`, `height_extremes`, `archaeology_30`, `sherds_15`.
- `xpq.exp.sleeping_bag_biomes`, `xpq.exp.parachute_landing`: retain proposed objective but keep **VERIFY/unregistered** until feature/event detection and survival attainability are verified.
- `xpq.exp.explorer_maps`: rename **지도 제작자의 발자취**, promote **MID -> LATE**, keep original **500 XP provisional**. Collect each of three explorer maps (ocean, woodland, trial) and **enter the exact corresponding destination structure targeted by that map**, in order (map acquisition -> matching actual visit). A preexisting visit without obtaining the matching map cannot satisfy this map-specific quest. Record each map's actual target identity and server-side entry; don't grant completion simply for visiting any monument/mansion/trial chamber. Keep **NOW/unregistered**, as current tracker is pending and reliable evidence implementation/tests are required.

## C. Shared rules, rollout constraints, and remaining work

1. **Quest overlap is intentional:** the same credible discovery or structure-entry event can advance multiple quests; each quest tracks its distinct own completion and payout.
2. **No fake retroactivity:** old structure proximity and mere species kill stats cannot establish structure entry or biome-qualified kill. Previously persisted *credible* biome discovery can be credited where reliable and appropriate.
3. No XP economy retune yet: retain stated raw XP amounts temporarily, even after tier promotions and replaced objectives. The prior aggregate 200-row totals will be revisited as a separate explicit owner decision, not silently changed.
4. All 24 exploration slots have undergone **first-pass design review**, but detailed predicates, event availability, migrations, negative tests and live acceptance remain open; 200-entry review is not complete.
5. **Implementation is not authorized by this document alone.** No registered/status flips, runtime copy changes or catalog mutation happened in this PR. The Q1-C/catalog and separate code PR branches remain unchanged.
6. Next review queue: the remaining **combat tab** entries excluding the three ecology quests already decided; inspect high-count kill ladders, mob types and distinctive fights against real code and pacing.

## Source of truth distinction

This file records **owner-approved design intent from conversation**, not an implementation claim. The existing `quest_inventory.csv` retains pinned Q1-C data for audit reproducibility; do **not** rewrite its snapshot rows to show future approved designs as though they were implemented.


## D. Combat follow-up — weapon quest removals and block target (2026-10-21)

Owner explicitly **rejects and orders removal** of the two repetitive, weapon-specific 100-kill quest slots:
- `xpq.cmb.dagger_100` / 그림자 칼날 — **DELETE** (not defer).
- `xpq.cmb.greatsword_100` / 거인의 일격 — **DELETE** (not defer).

Rationale: Dagger/greatsword progression roles and distinctive gameplay are not approved; forcing 100 kills with inventory-registered weapons is not meaningful content. Do not replace these entries with invented content merely to preserve the 200-entry count. Before implementation, reconcile roster cardinality and the approved XP ledger via an explicit decision; do not silently redistribute the 1,000 XP combined awards. `xpq.cmb.mm_arms` (one kill with each weapon) is a separate quest and **has not been explicitly removed** in this decision; flag it for later re-review.

Also confirmed by owner: `xpq.cmb.shield_500` / 철벽 goal **500 -> 100 successful shield blocks**, pending implementation and reward review. Ordinary hostile-kill ladder 100/500/2,000/5,000 remains approved as-is, including farmable cumulative progression. This is documentation-only owner intent, not a catalog edit or a registered-quest migration.

## E. Targeted combat — owner approval (2026-10-21)

- `xpq.cmb.ravagers_5` / 파괴수 사냥: **reduce required ravager kills from 5 to 3**; retain LATE and provisional 1,000 raw XP pending economy review. Identifier contains old count, so review compatibility and migrations before renaming.
- **Keep existing proposed objectives**: `xpq.cmb.bandits_20` (all three bandit variants represented, 20 total), `xpq.cmb.breeze_10`, `xpq.cmb.guardians_30`, `xpq.cmb.lesser_wither_10`, `xpq.cmb.brutes_3`, `xpq.cmb.wither_skulls_3`, and `xpq.cmb.raids_3`.
- These are content approvals only; missing trackers and live acceptance remain outstanding. No code, runtime catalog, status/registration, or XP ledger change was made in this documentation branch.


## F. Combat closure — additional weapon-specific removals (2026-10-21)

Owner explicitly approved **DELETE, not defer**, for:
- `xpq.cmb.mm_arms` / 새로운 무기 — dagger and greatsword, one kill each.
- `xpq.cmb.netherite_blades` / 네더라이트 칼날 — netherite dagger and netherite greatsword, one kill each.

Combined with earlier owner-approved deletions of `xpq.cmb.dagger_100` and `xpq.cmb.greatsword_100`, the design-review removal set is exactly **four quest IDs**. Preserve this exact list; do not remove other weapon quests by inference. No automatic replacement or redistribution of the deleted quests' 2,350 provisional raw XP is authorized. Review 200-entry roster count and aggregate XP budget explicitly during implementation planning. Existing production/catalog data and quest save migrations remain unchanged until separately implemented.

First-pass remaining combat decisions: keep the MoreMinecrafts 5/10/25-type bestiary milestones, vanilla 20-species milestone, trident kills, long-range shots, mace smashes; future elemental weapon, elemental skill, gauntlet and tactical ammo quests remain FUTURE pending their feature design. Any unresolved tracker/live acceptance is still a gate. Combat content first-pass review complete; XP economy remains separately deferred.

## G. Dimension-tab first-pass owner decisions (2026-10-21)

The owner approved the following **content decisions**, not production edits:

- **DELETE** `xpq.dim.shulker_boxes` (four differently dyed shulker boxes); do not transfer it to crafting or invent a replacement merely to maintain roster count. Provisional deleted award: 1,000 raw XP.
- **REPLACE** `xpq.dim.nether_travel_50k` (Nether 50,000-block travel) with player-facing **네더의 비밀**, an actual Nether exploration objective: visit soul sand valley, basalt deltas, crimson forest, warped forest, **and discover a naturally generated Nether fossil**. Keep ENDGAME and provisional 2,500 raw XP for now. Travel distance itself no longer contributes. Implementation must specify dependable server-side biome visits and fossil detection, define the exact natural-world discovery predicate (not merely proximity or arbitrary placed bone blocks), negative tests, and safe save migration before changing the existing ID. Do not retroactively award the new objective from existing travel distance.
- **KEEP** the other eight existing dimension-tab quests/objectives: `xpq.dim.fortress_loot`, `xpq.dim.blaze_rods_32`, `xpq.dim.trial_vaults_5`, `xpq.dim.ancient_city_loot`, `xpq.dim.bastion_all`, `xpq.dim.ominous_vaults_5`, `xpq.dim.end_cities_3`, `xpq.dim.end_city_loot_10`. Existing tier and raw XP values remain provisional.
- New total owner-approved deletions so far: four combat IDs (`dagger_100`, `greatsword_100`, `mm_arms`, `netherite_blades`) plus one dimension ID (`shulker_boxes`) = **five deleted slots** (3,350 provisional raw XP); no automatic replacement or reallocation approved. The replacement for `nether_travel_50k` retains its slot.
- All implementation, registration/activation, testing and economy reconciliation await separate execution approval. The pinned 200-row CSV remains a faithful pre-change inventory.

Next design review: boss tab.

## H. Boss-tab owner decisions (2026-10-21)

- **DELETE** `xpq.boss.warden_3` (three Warden kills), not merely defer: repeated Warden farming is misaligned with its intended avoidance-focused encounter. Do not substitute an invented quest simply to preserve the previous catalog count. The single-Warden quest is retained.
- **KEEP** all other eight boss quest concepts and their current targets: `xpq.boss.elder_guardian`, `xpq.boss.monument_clear`, `xpq.boss.warden`, `xpq.boss.wither`, `xpq.boss.twin_bosses`, `xpq.boss.dragon_rematch`, `xpq.boss.omen_raid`, `xpq.boss.monuments_3`.
- The two monument-clear quests must credit **verified meaningful combat participants**, not players merely present in the monument; preserve distinct genuine monument identities for the 3-clear objective. Both monument quests stay `VERIFY`/unregistered until their natural-world and multiplayer attribution acceptance is completed.
- Preserve all other boss target/tier/reward decisions as provisional. No runtime catalog, code, registration, migration, or balance changes are made by this documentation-only update.
- **Current owner-approved deletions: six IDs**: combat four (`xpq.cmb.dagger_100`, `xpq.cmb.greatsword_100`, `xpq.cmb.mm_arms`, `xpq.cmb.netherite_blades`), dimension one (`xpq.dim.shulker_boxes`), boss one (`xpq.boss.warden_3`). The deleted entries had 6,500 raw XP combined in the pinned pre-change catalog, but no reward reallocation is authorized. Overall catalog reconciliation is deferred.
- Boss-tab first-pass content review complete. Next: mining tab.

## I. Mining tab — first owner decision (2026-10-21)

- **DELETE** `xpq.min.pickaxe_5000` / 곡괭이 내구도 5,000 사용, not defer or retarget. This is arbitrary tool-wear repetition without sufficiently distinct mining gameplay. Do not auto-replace to preserve 200 quests.
- **KEEP** the current naturally generated ore count ladder `xpq.min.ore_1000`, `xpq.min.ore_5000`, `xpq.min.ore_20000`; the broad stone-block count `xpq.min.stone_10000`; and naturally generated log count `xpq.min.logs_1000`. Existing targets and tiers stay unchanged; XP rewards provisional pending economy pass.
- All five retained quests must keep their stated counting semantics, particularly player-placed-vs-natural provenance for ore/log counters where the catalog already distinguishes it. Preserve progression sharing when the same eligible mined block contributes to multiple threshold quests.
- **Approved deletion count is now seven**: four combat, one dimension, one boss and one mining (`xpq.min.pickaxe_5000`). Deleted-slot XP for the mining quest was 700 provisional raw XP; total provisional XP for seven deleted entries is 7,200. The aggregate XP ledger and save migration remain separate implementation decisions.
- Remaining mining discovery, ore-type, collection and enchantment quests have not been owner-reviewed yet. This is a design document edit only; no production catalog, code, tests, or runtime status changed.

## J. Mining tab — remainder first-pass approval (2026-10-21)

- **REPLACE** `xpq.min.amethyst_spyglass` / 자수정 관측 with player-facing **자수정 탐사대** (MID; provisional 400 raw XP). The approved replacement objective is to **discover three distinct naturally generated amethyst geodes and personally mine amethyst shards inside each**. Do not require crafting a spyglass or merely collecting 32 shards. Implementation must confirm genuine naturally generated geode identity/location, distinguish three actual geodes (not three positions in one geode), and attribute the direct harvest to the player; include negative tests for placed blocks, reentry, and artificially assembled geodes. The old quest ID is a migration key only until safe compatibility review. No prior spyglass/crafting or item-acquisition counter may retroactively satisfy this new exploration objective.
- **KEEP** the following existing mining objectives, targets, tiers, and provisional XP: `xpq.min.first_custom_ore`, `xpq.min.custom_ore_survey`, `xpq.min.deepslate_custom`, `xpq.min.obsidian_64`, `xpq.min.diamond_ore_64`, `xpq.min.debris_16`, `xpq.min.adamantium_debris`, `xpq.min.adamantium_10`, `xpq.min.all_wood`.
- **KEEP VERIFY/unregistered** for `xpq.min.vein_500` and `xpq.min.lumberjack_200` until enchantment behavior/event evidence and survival testing are sufficient. **KEEP FUTURE/unregistered** for `xpq.min.drill_1000` until drill functionality is implemented and reviewed. Adamantium progression/spawn scarcity must be validated separately before changing its targets or declaring natural survival acceptance.
- Together with section I, mining-tab first-pass content review is complete. This update documents design intent only, with no runtime catalog/registration, game code, XP economy or test change. Next tab: farming.

## K. Farming-tab first decision — meaningful 800-harvest replacement (2026-10-21)

The owner explicitly approved replacing the repetitive `xpq.frm.till_1000` / till 1,000 farmland blocks with **풍요로운 농장**, a **real crop-harvest activity** at approximately **800 harvests' worth of effort**, retaining the original **MID tier and 400 raw XP**. This is a **replacement of the existing quest slot**, not an additional 200th+ quest and not a simple count-only duplicate of `xpq.frm.harvest_1000`.

**Proposed concrete acceptance breakdown for implementation/detail review:** personally harvest 800 eligible **mature, farm-grown crop blocks in aggregate**, covering at least **six crop groups** (wheat, carrots, potatoes, beetroot, melon, pumpkin), with independent per-crop minimums provisionally set to wheat 200, carrots 150, potatoes 150, beetroot 100, melon 100, pumpkin 100 (total 800). These *individual crop quotas are assistant elaboration, not yet separately approved by the owner*; owner-approved binding constraints are crop harvesting instead of tilling, equivalent 800-level effort, and original 400 XP reward. If individual quotas are accepted later, require all six quotas, not 800 harvests of one crop. Ensure counts reflect actual successfully matured crop harvesting, not farmland conversion, planting, immature breakage, artificial repeated placement or duplicated drop counts. Validate melon/pumpkin farming behavior and legitimate automation attribution before implementation.

Retain `xpq.frm.harvest_1000` as a separate generic lifetime crop-harvest milestone; credible eligible harvest events may progress both quests concurrently. Existing till counters cannot retroactively complete the replacement. Catalog/runtime registration remains unchanged in this documentation-only PR, and safe ID/save migration plus positive/negative GameTests and live survival acceptance remain required.

## L. Farming, husbandry and trading owner decisions (2026-10-21)

- `xpq.frm.tame_5` (wolf, cat, parrot, horse and llama, **all five distinct tame types**) — **PROMOTE EARLY_MID -> MID_LATE**. Keep original 5-species objective and provisional 350 raw XP until separate global XP pass. Registration and gameplay changes are not authorized by this design-document update.
- `xpq.frm.mending_book` (formerly librarian-traded Mending book) — **REPLACE with an actual Mending experience quest**, not acquisition of a book nor a required librarian transaction. The owner asked that **moving it from farming to the equipment tab be evaluated**, but has not yet approved a destination tab. Proposed verification-ready mechanic for later approval: while the player has a genuinely XP-Mending-enchanted, previously damaged piece of equipment equipped or in an eligible slot, gain an XP orb and verify that the item's durability actually increases from the Mending effect. A mere Mending book/item acquisition, enchantment application alone, or gaining XP without repair does not qualify. Exact eligible gear/slot, attribution, trigger count and cross-tab routing must be finalized before implementation. Keep existing MID/450 XP provisionally, NOW/unregistered and pending tracker until approved.
- **KEEP** existing breeding `xpq.frm.breed_200`, villager trade quests `xpq.frm.master_trade`, `xpq.frm.professions_10`, `xpq.frm.trades_200`, and piglin barter `xpq.frm.barter_200` with current objectives, tiers and provisional raw XP. No new obligations or reward edits.
- Other previously reviewed crop/mining decisions remain unchanged. This entry documents owner-approved intent, not a runtime implementation or live validation. Continue remaining farming-tab review and then advance to the next tab.

## M. Fishing tab — entire design review ON HOLD (2026-10-21)

**Explicit owner decision: DEFER REVIEW of all 16 fishing-tab quest entries as a group.** Rationale: the MoreMinecrafts fishing minigame's currently awkward/incorrectly partitioned gameplay flow, previous fishing UI crashes and other unresolved fishing-system defects make individual quest goals, tiers, events and rewards premature to approve. The owner did **not** approve the assistant's proposed Reel/Minnow/Crankbait III equipment-use objective; that proposal remains unapproved.

- No fishing quest was approved, deleted, redesigned, promoted, downgraded or reward-adjusted by this decision. Preserve the 16 original catalog entries as a **frozen pre-review snapshot** and retain their respective existing NOW/VERIFY/FUTURE and registered flags without claiming they are technically ready or live accepted. The prior three registered fishing quests are not validation of minigame/UI readiness.
- Block meaningful fishing quest content review until: (1) minigame flow/feel is redesigned or validated in actual gameplay, (2) fishing UI crash and critical interactions are repaired and regression-tested, (3) fishing modules, grades, species, catch events and rewards are verified against actual mechanics, and (4) server-authoritative tracking and live acceptance are possible. These are review prerequisites, not an authorization to change production code in this docs PR.
- Next XP Quest tab to review: crafting. Do not silently skip/reclassify fishing quests as owner-approved, and do not change the pinned 200-entry inventory or its ledger.

## N. Owner correction: preservation of 200 slots through replacement (2026-10-21)

**This section supersedes all earlier references in this document to permanently deleting seven XP Quest rows without replacement.** The owner identified that deleting quests invalidates the expected 200-entry structure and agreed XP economy. **New binding policy: redesign/replace problematic objectives in their existing quest slots; preserve the intended 200-entry roster and per-slot provisional XP and tier unless a separately approved adjustment is recorded.** The seven previously approved "DELETE" decisions are now **REMOVE OLD OBJECTIVE; REPLACEMENT REQUIRED**, not permission to shrink the catalog:

1. `xpq.cmb.dagger_100` (MID 500 XP)
2. `xpq.cmb.greatsword_100` (MID 500 XP)
3. `xpq.cmb.mm_arms` (EARLY 250 XP)
4. `xpq.cmb.netherite_blades` (LATE 1,100 XP)
5. `xpq.dim.shulker_boxes` (LATE 1,000 XP)
6. `xpq.boss.warden_3` (ENDGAME 3,150 XP)
7. `xpq.min.pickaxe_5000` (MID_LATE 700 XP)

**Crafting tab: replace, do not delete**, `xpq.cft.all_daggers` (MID 500 XP) and `xpq.cft.all_greatswords` (LATE 1,100 XP). The old forced crafting-all-dagger/greatsword-tier goals are rejected, but their two quest slots and provisional 1,600 raw XP should be retained. Exact replacement goals across all nine slots require follow-up design and owner approval. Do not substitute arbitrary busywork, silently invent finalized objectives, or claim the original 200-entry implementation is changed. The old pinned total remains **209,100 raw XP** as the provisional budget target, not as proof that gameplay pacing is correct; an economy pass is still planned.

### Equipment readiness: large backpack and portable furnace

Owner requests **implementation preparation** for `xpq.cft.backpack_large` and `xpq.cft.portable_furnace`, not immediate automatic activation or unreviewed recipe commitments. Their Q1-C catalog status remains FUTURE/unregistered pending craftability, event hooks and live tests.

**Evidence checked** on pinned Q1-C `17e980912d636b0572a3f25b24995ffde31c754c`:
- `BackpackItem.java`, `BackpackStorage.java`, `PortableFurnaceItem.java`, `PortableFurnaceMenu.java` already exist; `large_backpack.json` and `portable_furnace.json` item-model assets exist.
- Recursive Git tree inspection of `src/main/resources/data/moreminecrafts/recipe/` found **no matching backpack/small_backpack/large_backpack/portable_furnace crafting recipe JSON** at the pinned Q1-C ref. This confirms missing recipe data in that snapshot, not absence of runtime classes or an exhaustive review of other branches.
- `docs/UTILITY_EQUIPMENT_FINALIZATION.md` authorizes player-bound backpack shared inventory tiers (9/27/54), stable storage/death retention, chest armor attachment and a recipe **progression direction**: Small Backpack needs a bundle plus leather/string-type materials; Backpack upgrades Small Backpack with an Ender Pearl and stronger metal structure; Large Backpack upgrades Backpack with a Shulker Shell and diamond-like structure. **Specific ingredient counts and arrangements are not yet approved.**
- Portable furnace is an item-operated UI machine (not a block), 3x normal smelting speed and 3x fuel consumption; 64-stack items are capped at 16 per slot; Stage 23-B approved persistence of contents, fuel and cooking progress across menu close/reopen, with no furnace research bonuses. **Exact recipe materials still undecided**. The older Stage 23-A furnace infrastructure document has outdated session-persistence statements superseded by Stage 23-B authority.

**Implementation-readiness checklist**, not completed work:
1. Audit `ModItems`, recipe unlock policies, component persistence and menu owner/source stack handling on the **actual current integration target**, since a pinned Q1-C branch may not include recent stacked PR changes.
2. Propose concrete balanced shaped/shapeless recipes for all three Backpack tiers and the Portable Furnace in a separate recipe decision; preserve the approved ingredient *roles*; do not treat the ingredient proposals as approved until owner accepts them.
3. After approval, add data recipes to `src/main/resources/data/moreminecrafts/recipe/`, ensure proper result IDs, valid Minecraft version codec, progression unlock/gating, and recipe-book behavior.
4. Validate fresh survival craftability, no unintended ingredient duplication, shared-storage persistence, higher-slot retention after downgrade, death/respawn, player binding, attached chestplate behavior, no nesting/duplication; verify portable furnace input/fuel/output persistence, recipe validity, 3x speed and fuel, 16-slot stack cap, menu close/drop, restart and multiplayer.
5. Then connect `xpq.cft.backpack_large` to **legitimate acquisition/crafting** of a Large Backpack and `xpq.cft.portable_furnace` to **64 actually processed outputs in that item** with server-authoritative attribution and no double-counting. Assess FUTURE -> NOW only after feature and quest evidence pass their respective gates.

Neither actual crafting recipes nor runtime quest tracking were created by this documentation update. PR #140 remains Draft.

## O. Owner-approved crafting recipe revision (2026-10-21)

**Owner recipe decisions (supersede the unapproved Ender Pearl direction for normal Backpack):**

1. **Backpack (27 slots)** upgrades the **Small Backpack (9 slots)** using a **vanilla Chest instead of an Ender Pearl**; retain the earlier structural metal component direction. Owner explicitly rejected Ender Pearl as thematically inappropriate. For a concrete shaped recipe proposal, use 1 Small Backpack, 1 Chest, and 4 Iron Ingots:

   ```text
   I C I
   . S .
   I . I
   ```
   `I` iron ingot; `C` chest; `S` small backpack; `.` empty. **The exact four-iron quantity and layout are assistant choices, not individually prescribed by owner**, though owner authorized recipe layout decisions. Do not treat this as approval of other two backpack recipes.

2. **Portable Furnace** recipe is **owner-locked** to exactly **1 vanilla Chest + 1 vanilla Furnace + 4 Iron Ingots**, no additional ingredients. Owner delegated pattern layout. Adopt this shaped 3×3 recipe:

   ```text
   I . I
   . F .
   I C I
   ```
   `I` iron ingot; `F` furnace; `C` chest; `.` empty.

The above layouts are actionable recipe intent but this remains a documentation-only design PR: **no JSON recipe files, registration/status activation, or tests were executed here**. At implementation, check shaped recipe JSON format for current Minecraft version, recipe unlock/gates, ingredient consumption, server-side outputs, single-container ownership/persistence and test fresh-world crafting. Preserve tiered Backpack shared player-bound storage and Portable Furnace previously approved 3× smelting/fuel + persistence behavior. Small and Large Backpack ingredient counts/layouts remain to be separately finalized; earlier role direction (Small: bundle/leather/string; Large: Backpack/Shulker Shell/diamond-like) remains intact.

## P. Owner approval: Small and Large Backpack shaped recipes (2026-10-21)

Owner approved both proposed 3×3 shaped recipes, completing the four utility-item recipe decisions from sections O and P.

**Small Backpack (9 shared player-bound slots):**
```text
L S L
L B L
L S L
```
`L` vanilla leather ×6; `S` vanilla string ×2; `B` vanilla bundle ×1. Output: one `moreminecrafts:small_backpack`. This is a genuine survival crafting recipe, not a knowledge-only grant.

**Large Backpack (54 shared player-bound slots):**
```text
D H D
D P D
D H D
```
`D` vanilla diamond ×6; `H` vanilla shulker shell ×2; `P` existing `moreminecrafts:backpack` ×1. Output: one `moreminecrafts:large_backpack`.

The intermediate **Backpack (27 slots)** remains as approved in section O (Small Backpack ×1, Chest ×1, Iron Ingot ×4); **Portable Furnace** remains Chest ×1, Furnace ×1, Iron Ingot ×4. All four recipe ingredient sets and layouts are now owner-approved. Implementation remains separate: validate registry IDs, data recipe codec and unlock conditions against the actual integration branch, craftability, equipment/shared-storage and furnace persistence tests, and quest attribution. Do not claim recipe JSON was added, PR merged or FUTURE quest flags activated by this documentation change.

Resume crafting XP Quest review after recipe implementation preparation; replace old dagger/greatsword crafting quests rather than delete slots, per section N.

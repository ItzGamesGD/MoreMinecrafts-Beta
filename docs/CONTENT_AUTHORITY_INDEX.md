
> **HISTORICAL UI DECISION — SUPERSEDED (2026-10-10):** The earlier three-active-choice selector and unapproved landing-window note remain in the linked record for history only. The current approved decision is four visible cards: 발전 과제, 지식, 연구 are active; XP 퀘스트 is disabled and marked for later. Current authority is recorded below and in the amended UI documents. **The XP 퀘스트 card's disabled/later status is itself superseded by the XP Quest amendment — 2026-10-17 (see the XP Quest section below).**

# Current content authority and deferred design debt

This file is the compact authority map for the recovered cumulative line.

It does not replace the detailed design documents. When an older document conflicts with a newer reconciliation document listed here, the newer reconciliation is authoritative for implementation.

## Accessory cooldown lifecycle — approved 2026-10-25

Accessory cooldown lifecycle is governed by the [owner-approved 2026-10-25 decision](ACCESSORY_COOLDOWN_DECISION_2026-10-25.md). It specifies remaining online server ticks per player and ability type, including persistence through death, reconnect and restart. The same record holds the approved Deep Sea Pearl motionless definition (no horizontal movement input), the Ring of Life re-arm rule (a ring that is still armed may activate once when its cooldown expires while health remains low; after activation it re-arms only above 25%) and the Ember Ring first-hit exclusion; it does not finalize other ability mechanics.

## Accessory visual identities — approved 2026-10-22

The 12 registered accessories' visual identities (shape, material, palette, signature feature) and the native 32×32 item-texture size are approved in [ACCESSORY_VISUAL_CONCEPT_2026-10-22.md](validation/ACCESSORY_VISUAL_CONCEPT_2026-10-22.md). Accessory category indicators remain text labels. This does not change accessory gameplay.

## Accessory anvil catalyst slot layout — approved 2026-10-23 (conditional on real-client acceptance)

Relic Catalyst slot inline in the anvil's Enhance-mode input row, inside the 176px panel and clickable: [ACCESSORY_ANVIL_CATALYST_LAYOUT_APPROVED_2026-10-23.md](ACCESSORY_ANVIL_CATALYST_LAYOUT_APPROVED_2026-10-23.md). Catalyst artwork, recipe and acquisition remain deferred; GUI scale 1 and cosmetic follow-ups are tracked in [validation/ACCESSORY_INTEGRATION_FOLLOWUPS_2026-10-23.md](validation/ACCESSORY_INTEGRATION_FOLLOWUPS_2026-10-23.md).

## Accessory inventory UI — approved 2026-10-23

Five accessory slots integrated left of the vanilla armor slots, no side panel or permanent labels, tooltips only, and an independently positioned survival recipe book: [ACCESSORY_INVENTORY_UI_APPROVED_2026-10-23.md](ACCESSORY_INVENTORY_UI_APPROVED_2026-10-23.md). Gameplay, IDs, art and persistence are unchanged.

## Latest user-approved UI authority — 2026-10-11 follow-up

- [Approved advancement/knowledge/research integration UI](UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md), as amended by the 2026-10-11 navigation follow-up and 2026-10-12 interaction correction: retain the four-card selector, route Research and Knowledge through the shared seven-sector picker, and use each existing branch list for top-left direct tree navigation. The 2026-10-12 correction supersedes only node-screen footer controls, the separate detail panel, and confirmation purchase flow; it requires exact vanilla-frame hit bounds, one-click server-authoritative Research purchase, Knowledge non-purchase, and localized item-rich tooltips. Preserve vanilla advancement visuals, source discipline/branch IDs, progression data, prerequisites, costs, and gameplay rules.
- [Complete UI/system inventory audit](UI_SYSTEM_FULL_AUDIT_2026-10-10.md): amended with the approved selector, chest sort, hopper filter, Mob Tracker range controls, boat inventory interpretation, and separate potion systems. Historical conflicts remain labeled as superseded/deferred rather than silently erased.

## XP Quest and Enhancement XP-cost authority (2026-10-17)

Approved XP Quest decisions are recorded in the **XP Quest amendment — 2026-10-17** section at the end of [UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md](UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md):
- vanilla Advancements-screen reuse with click-to-claim;
- three node states and a completion toast;
- server-authoritative claim-once rewards;
- unclaimed quests as an XP store;
- exact raw-XP grants and deductions;
- reward scope: 180,000 raw XP exactly across the 180 active quests, plus 29,100 for 20 future quests (209,100 target), with no reduction of existing rewards when content is added;
- VERIFY quests stay unregistered until they pass verification;
- each of the 160 NOW quests registers only after its own automated test passes;
- the claim rolls back if the XP grant fails;
- the six owner-locked tracking policies apply. The ore policy counts only verified-natural ores; unknown origin does not count.

Individual quest content and values remain subject to approval. The Enhancement raw-XP cost table and transaction rule are recorded in the Enhancement section of [STAGE_2_GAMEPLAY_RECONCILIATION.md](STAGE_2_GAMEPLAY_RECONCILIATION.md). These are design authority only; implementation has not started.

## Stage 22 research completion authority (2026-10-15)

The user approved delegation of the remaining seven-discipline research-tree, new paid roots, motorized boat, cross-dimensional transport and automation edge-case design. The current controlling completion addendum is [RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md](RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md). It supersedes older incompatible research-root/topology, counter-shield recipe and potion-combination restrictions while retaining the approved V3/V4 price ledgers and already locked research effects. Tactical Shooting ammunition/effects/recipes remain DEFERRED to another conversation. This is design authority, not implementation or merge approval.


### XP Quest UI architecture (2026-10-24)

The owner-approved [2026-10-24 amendment](UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md) supersedes only the 2026-10-17 vanilla-screen architecture: XP Quest retains `ProgressionBrowseScreen` and `ProgressionTreeCanvas`. All other October 17 Quest gameplay and transaction rules remain authoritative. The four-card selector, vanilla Advancements access, and seven-discipline Knowledge/Research behavior remain unchanged. The 200-slot presentation does not expand the current 75 registrations. See [the client validation matrix](validation/WAVE4B_CUSTOM_XP_QUEST_CLIENT_MATRIX_2026-10-24.md).

## Stage 23-A Batch 1 execution record (2026-10-15)

The scoped implementation manifest is [RESEARCH_STAGE23A_BATCH1_MANIFEST_2026-10-15.md](RESEARCH_STAGE23A_BATCH1_MANIFEST_2026-10-15.md). It selects eight existing M1 research events and records their approved behavior, hooks, dependencies, and focused verification. It does not replace the research effect authority above. The 87-node audit is maintained in [RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md](RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md); its verified pre-batch baseline was 74 DATA_ONLY, 11 PARTIAL, and 2 BLOCKED. With eight Batch 1 handlers and focused tests added but integration evidence pending, the current counts are 66 DATA_ONLY, 19 PARTIAL, and 2 BLOCKED.

## Stage 23-A Batch 2 execution record (2026-10-15)

The Batch 2 manifest is [RESEARCH_STAGE23A_BATCH2_MANIFEST_2026-10-15.md](RESEARCH_STAGE23A_BATCH2_MANIFEST_2026-10-15.md). It continues from PR #90, selects three DATA_ONLY M1 V3 effects (`v3.research.17`, `.28`, `.64`), and adds focused movement validation for the existing `wall_jump` and `fall_roll` handlers. Its 87-row recount is 63 DATA_ONLY, 22 PARTIAL, and 2 BLOCKED. Fisheries Study was excluded at that point because integer residual allocation had not been specified; the user approved largest-remainder allocation with lower-grade-index tie-breaking in the later Stage 23-A integration authority amendment below.

## Stage 23-A integration of unique Batch 1 effects (2026-10-15)

The scoped integration manifest is [RESEARCH_STAGE23A_INTEGRATION_MANIFEST_2026-10-15.md](RESEARCH_STAGE23A_INTEGRATION_MANIFEST_2026-10-15.md). It starts from PR #91 and imports only unique PR #92 behavior for `v3.research.19`, `.55`, and `.67`, retaining PR #90 ownership of `.02`, `.03`, `.18`, and `.23` and PR #91 ownership of `.17`, `.28`, `.64`, `wall_jump`, and `fall_roll`. The current Fisheries Study rounding authority is recorded at the end of `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`. The regenerated canonical 87-row audit currently counts 60 DATA_ONLY, 25 PARTIAL, and 2 BLOCKED; it separates automated evidence from live gameplay verification. Java 25 Fabric CI #754, mapped-source probe #179, and K5 restart #87 passed on code commit `227749d`; live gameplay checks remain pending.

## Stage 23-A Expanded Batch 2 execution record (2026-10-17)

[Expanded Batch 2 manifest](RESEARCH_STAGE23A_EXPANDED_BATCH2_MANIFEST_2026-10-17.md) records all 53 remaining DATA_ONLY candidate evaluations at PR #105, five added runtime effects (`.38/.39/.40/.51/.62`), and persistent portable `.06/.07/.11` integration. The canonical audit recount is 48 DATA_ONLY, 37 PARTIAL, 2 BLOCKED, zero COMPLETE/MISSING. This is an implementation record; it introduces no design authority or live acceptance.

## Stage 23-A remaining implementation campaign record (2026-10-17)

The [remaining candidate survey and campaign manifest](RESEARCH_STAGE23A_REMAINING_CAMPAIGN_MANIFEST_2026-10-17.md) starts from PR #108 and evaluates all 48 then-DATA_ONLY nodes. Group 1 adds `.33` consecutive sword sweeps and `.61` splash/lingering potion behavior. Group 2 adds ordinary-Horse `.50` Riding support and repairs the existing `mob_capture` purchase gate. The canonical 87-node audit now records 45 DATA_ONLY, 40 PARTIAL and 2 BLOCKED; no node is COMPLETE without live acceptance. This is an implementation record and does not amend effect design authority.

The Claude economy/soil implementation adds `.53` Villager Trading and `.16` Soil Study (group 4 of the same manifest); the canonical audit now records 43 DATA_ONLY, 42 PARTIAL and 2 BLOCKED. Still an implementation record only; the approved values come from the separate user-decision document and live acceptance is pending.

## Current research/knowledge authority (latest human-approved V4)

The earlier **Knowledge / Research — IDEA / DEFERRED** paragraph at the bottom of this index is HISTORICAL and superseded, not active. The current, explicitly user-approved Knowledge / Research design authority is [RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md](RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md), especially its **RESEARCH V4 EXECUTION AUTHORITY — 2026-10-10** amendment and the later user-confirmed recipe/mob-capture decisions.

This explicitly promotes the 7-disciplines / 70 free-knowledge roster and paid research with materials + raw XP to active design authority for the scoped V4 project. Research tier markers T1/T2/T3 are development classifications, NOT mandatory uninterrupted purchase gates: T1 may link directly to T3. Existing combat/ecology discipline headings can be their own very small paid branch-root research nodes; do NOT insert new intermediate paths. Previously approved V3 prices remain authoritative until rebalanced deliberately; V4 additions are specified in the linked price addendum.

For Stage 2-A/B acquisition semantics and verification status, use [Stage 2-A Knowledge event collectors](STAGE2A_KNOWLEDGE_EVENT_COLLECTORS.md) and [Stage 2-B Knowledge condition corrections](STAGE2B_KNOWLEDGE_CONDITION_CORRECTIONS.md). The 2026-10-12 K5/K8/K14 wording and K8/K14 registries are recorded there as historical Stage 2-B context. For K5, the later user-approved PR #81 redesign and [K5 production integration record](K5_PRODUCTION_INTEGRATION.md) supersede the historical 128-acquired-log wording: the active rule is 200 successful direct Survival mines of naturally generated eligible vanilla logs. The user's 2026-10-14 K14 amendment in Stage 2-B supersedes raw `Stats.ITEM_CRAFTED` reconciliation: persisted K14 progress/unlocks remain authoritative, and mode-ambiguous statistics grant no new progress. These rules do not change the 70-node catalog.

13 user-approved knowledge-gated recipe alternatives are retained, all vanilla recipes remain available. Automatic crafting was rejected entirely. T3 mob capture (30-durability tool, original mob persistent state including villager trades, no bosses) and the latest mobility/hostile-target redirection/parachute clarifications are active scoped design. Defer rope arrows. This promotion does NOT itself assert runtime implementation and does not overturn separate existing equipment/fishing/enchantment authorities outside the new research scope.

## Oct 7 follow-up authority overlay

The current design baseline is [the Oct 7 continuation record](CONTENT_FINALIZATION_CONTINUATION_2026-10-07.md). The implementation choices and provisional values made under the user's scoped follow-up are recorded in [the Oct 7 design and implementation follow-up](CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md). Those two documents supersede older `[OPEN]` statements in this index and the current design checklist for T2 core acquisition, Multi Catch, Fisherman fish sales, and the first one-time activity rewards. Numeric loot chances, the Emerald conversion divisor/cap, and all interactive behavior remain balance/live-validation pending.

Documentation history is split across branches: the Oct 7 continuation came from `origin/main`; the source/implementation worktree is based on Stage 20 / PR #69. The continuation was copied into this worktree so its role is explicit. Do not use the older index text below to override those Oct 7 decisions.


## Bounty and contracts — approved 2026-10-22

Current approved design through Q62 and the later BC-03 owner overlay: [Bounty & Contract System](BOUNTY_CONTRACT_APPROVED_2026-10-22.md). It records physical contract acquisition, item-bound transferable progress, manual redemption, tiered target generation, expanded target pools, approved rarity distributions, reward quantities, price ranges, seller/source rules, combat contribution, duplicate progress, delivery semantics, UI information, and completion integrity requirements. The overlay resolves hostile-only combat, two provisional delivery exceptions, structure entry and tier distributions, and special conversion/direct-drop status. Exact unresolved pool identities and specified engineering edge cases remain open. The final 2026-10-25 owner decisions in that document keep vanilla villager restocking, require 1 / 1–2 / 2–3 distinct structure instances per exploration objective, treat purchase ranges as base prices before Hero of the Village and research discounts, and defer profession-relevant objective weighting. This is design authority; implementation and validation status belong to the BC-03 branch and PR.

## Current authority

### Cumulative recovery foundation

Use the Stage 1 cumulative recovery line as the trusted implementation base. Historical checkpoint branches remain forensic history and are not implementation authority when they conflict with the recovered cumulative tree.

### Enhancement and T2 gameplay

Current contract: [Stage 2 gameplay and progression reconciliation](STAGE_2_GAMEPLAY_RECONCILIATION.md).

Authoritative ordinary Enhancement rules:

- T1 Vanilla: maximum +20
- T2 Elemental: maximum +20
- deterministic milestones at +5 / +10 / +15 / +20
- no ordinary T3/T4 Enhancement
- historical +30 / +40 rules are superseded

Accessory Enhancement is a separate approved contract and does not inherit the ordinary T1/T2 +20 cap or equipment growth tables. Use [Accessory Enhancement Decisions (2026-10-17)](ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md) for the twelve independent +0…+10 tables, raw-XP and material costs, catalyst milestones, anvil integration, and accessory-only rules. Use [Accessory / Relic System Decisions (2026-10-16)](ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md) for the approved roster and behavioral direction, except where the later 2026-10-17 enhancement record supersedes proposed values or unresolved labels. The one-catalyst-per-milestone quantity is explicitly provisional; catalyst acquisition and final anvil visual layout remain deferred. Ordinary T1/T2 Enhancement rules below and in the Stage 2 reconciliation remain unchanged.

Current T2 input/gameplay rules include the seven-item final roster, right-click active skills, cumulative Lightning charge semantics, and the removal of Wind Step and the old F / Shift+F T2 path.

The dedicated T2 gameplay finalization accepted the existing Fire and Wind hit-threshold passive structures, including their broad AoE passives. This specific overlap is not pending redesign; their other mechanics and visual identities remain distinct under the Stage 2 contract.


### Stage 19 T2 acceptance state

The Stage 19 checkpoint records four separate T2 acceptance states:

- **T2 IMPLEMENTATION: COMPLETE** — the gameplay structures for Fire Sword, Water Sword, Wind Sword, Earth Mace, and Lightning Sword are integrated.
- **T2 AUTOMATED ACCEPTANCE: PASS** — PR #66 passed Fabric CI #291, including `./gradlew clean build`, JUnit, GameTests, configured validators, server smoke, client smoke, and registry initialization.
- **T2 LIVE ACCEPTANCE: PENDING** — no interactive Minecraft live acceptance is claimed. Combat input, targeting, impact behavior, PvP/allied filtering, multiplayer authority, and runtime cleanup remain for direct live validation as listed in the Stage 2 reconciliation.
- **T2 VFX: DEFERRED** — provisional runtime effects may remain. VFX quality and polish do not block gameplay acceptance.

**T2 BALANCE / FEEL TUNING: LIVE PENDING.** Documented provisional combat and movement values remain subject to live tuning. Wind Bow homing/auto-aim remains deferred. Dark Energy Staff gameplay and Enhancement remain deferred.

### Equipment progression and T1 vanilla-style weapon extensions

Current broader equipment authority: [Equipment Progression Finalization](EQUIPMENT_PROGRESSION_FINALIZATION.md), except where Stage 2 explicitly supersedes its historical T2/Enhancement sections.

Current early-game / T1 / high-level T2 survival progression-loop checkpoint: [Early / T1 / T2 Survival Progression Loop Checkpoint](EARLY_T1_T2_SURVIVAL_PROGRESSION_LOOP_CHECKPOINT.md).

The current T1 vanilla-style expansion recorded there adds Dagger, Greatsword, and a single non-tiered Gauntlet as ordinary/basic weapon extensions rather than a T1.5 active-skill system.

The progression-loop checkpoint is authoritative for the vanilla-first early game, T1 `play Minecraft and grow` identity, Enhancement Stone's cheap supporting-material role, XP-supply philosophy, deferred one-time Quest role, and the high-level T2 `exploration/challenge -> elemental acquisition -> T2 equipment` direction. Exact Quest content and T2 loot/encounter rules remain open.

### Utility equipment

Current Utility Equipment authority: [Utility Equipment Finalization](UTILITY_EQUIPMENT_FINALIZATION.md).

Use that document for the active Utility roster, deleted/deferred concepts, helmet socket, Miner Lantern, Potion Injector, Exploration Sleeping Bag, Portable Furnace, Mining Drill, and Backpack contracts.

The **Stage 23-B Portable Furnace persistence amendment (2026-10-16)** in that document supersedes the earlier close-and-drop rule: the three inventory slots and burn/cook progress follow the single-stack Portable Furnace item across close/reopen and item transfer. This does not authorize portable furnace research bonuses.

### Fishing

Current Fishing implementation and runtime/resource contract: [Stage 3 Fishing runtime/resource/UX reconciliation](STAGE_3_FISHING_RESOURCE_RECONCILIATION.md).

Older Stage 7 and content-finalization sections are historical where they conflict with Stage 3.

Current Fishing authority:

- final 12-species roster
- Reel / Minnow / Crankbait equipment Tier I–III only
- Fish Grade I–V
- species first, minigame, Minnow capture permission, Grade, Length, then Length-dependent Weight
- approved Resource D master PNGs remain immutable
- runtime item presentation is a separate model contract and must not default to unsafe generated alpha-extrusion geometry

Tier I–V modules, independent Length/Weight rolls, late-only Minnow notice, retired fish, and generated-item geometry are superseded where they conflict with Stage 3.

**Oct 7 implementation overlay:** Multi Catch is a one-level Fishing Rod node after Lure III for a provisional 1,000 XP; only an unlocked rod resolves two independently rolled fish records, with Minnow permission and Minnow/Crankbait wear applied per fish. The custom Fisherman sale is Shift-right-click with one custom fish and uses all four stored fish fields. Its provisional conversion and explicit remaining validation are in `CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md`.

### T2 acquisition

The five T2 elemental core identities and acquisition sources are locked by the Oct 7 continuation record. Current code adds rare loot to the vanilla normal/ominous Trial reward pools, Bastion treasure, and Ancient City tables, plus a saved one-time Ocean Monument completion claim. Chance values and the Trial table mapping are provisional; no recipes or equipment links are authorized until content demand is known.

### Activity rewards

The first activity set uses one-time vanilla advancements and ordinary XP only. Advancement progress is per player and saved by vanilla. The 60 XP total, activity triggers, and no-daily/no-shop boundary are specified in `CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md`.

### Approved resource assets

Runtime presentation rules: [Resource asset presentation contract](RESOURCE_ASSET_PRESENTATION_CONTRACT.md).

An approved master image and its Minecraft runtime model are separate concerns.

Human-approved production PNGs remain immutable unless an explicit later art task changes them. Runtime model geometry, display mode, and performance constraints may be changed without altering the approved master asset.

Flame Sword C1-A/C1-B remains separately authoritative for its approved production PNG, which is immutable. Its runtime presentation contract was corrected by Stage 18 live-test repair: the obsolete bounded two-face handheld model is superseded by the bounded solid six-face handheld model defined in the Resource asset presentation contract. This geometry correction does not authorize artwork changes.

## Enchantments

Current enchantment gameplay-semantics authority: [Enchantment Branch Skeleton](ENCHANTMENT_BRANCH_SKELETON.md).

Current T1 enchantment progression, skill-tree placement, compatibility policy, and XP-cost authority: [T1 Enchantment Skill Tree Finalization](T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md).

Current Enchanted Book / vanilla villager-trade integration checkpoint: [Villager Trade and Enchanted Book Integration Checkpoint](VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md).

The post-audit authority addenda in these two current documents and the Early / T1 / T2 Survival Progression Loop Checkpoint supersede earlier OPEN statements only for the newly confirmed decisions recorded there. Their older history remains useful context.

The T1 finalization supersedes the skeleton wherever the older skeleton describes vanilla/random-book acquisition, progression placement, or compatibility rules that conflict with the XP-funded skill-tree model. The villager/book checkpoint supersedes the old direct book/anvil acquisition assumption and records the confirmed Book Option architecture plus profession-by-profession trade status. The skeleton remains authoritative for retained custom-enchantment gameplay semantics and unresolved integration behavior.

Historical Paper enchantment behavior and older enchantment proposals are reference material only where they conflict with these current authorities.

## Phase 2 preservation routes — 2026-10-26

Owner decisions F-1, F-2, F-3, F-5, F-6 and the mercenary status are recorded in [PHASE2_OWNER_DECISIONS_2026-10-26.md](preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md). They are applied to the affected documents by the [Phase 2 authority reconciliation](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md), which wins over those documents on the specific points it names and leaves every other approved rule unchanged. In short: the Tomb Guardian's drop is **Ancient Heart**, which replaces Tomb Guardian Core in the approved shaped Earth Mace recipe, and the Ancient City remains an Ancient Heart source; Adamantium generates at Y 8–22, biased toward Y 15, in veins of 1–2, and must produce fewer ore blocks than Ancient Debris; the rare caravan never sells special contracts directly, and the 5% Advanced-to-Special conversion is kept; the Ancient City arena and ST-05 are separate structures; mercenaries are a territory-independent CANDIDATE / DEFERRED concept; the wither-skull quest counts only the first pickup of a genuinely dropped skull and stays unregistered. Recovered records and their source commits: [provenance manifest](preservation/PROVENANCE_MANIFEST_2026-10-26.md). These are design records only; nothing in them is implemented or validated by this route.

- **Stage 23-A remaining research:** [RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md](preservation/recovered/stage23a-remaining-approval/RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md) is APPROVED design authority for the topics it names. Its unpriced and unspecified items remain open.
- **Structures** (pyramid, pillager headquarters, structure candidates, Ancient City arena versus ST-05): [STRUCTURE_AUTHORITY_INDEX.md](STRUCTURE_AUTHORITY_INDEX.md).
- **Territory:** SHELVED per [TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md](preservation/recovered/territory-chain/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md). The other files in that recovered folder are historical reference only and must not be implemented.
- **Independent designs from the territory chain:** world event [pillager patrols](preservation/recovered/territory-chain/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md) (design finalized); [Adamantium equipment](preservation/recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) (approved v1, worldgen amended by F-3); [rare caravan and runes](preservation/recovered/territory-chain/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md) (user-confirmed; read with the [conflict register](preservation/recovered/territory-chain/DESIGN_CONFLICT_REGISTER.md) F20/F21 and F-5). Mercenary and desert-artifact material in [DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md](preservation/recovered/territory-chain/DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md) is candidate or historical only.
- **Unreconciled records:** the 2026-10-10 [design idea decisions](preservation/recovered/content-first-axis-review/DESIGN_IDEA_DECISIONS_2026-10-10.md) (Feather Boots conflict) and the 2026-10-19 [commission board](preservation/recovered/territory-chain/BOUNTY_COMMISSION_APPROVED_2026-10-19.md) are listed as open owner conflicts in the reconciliation. They do not override current authority.

## Explicitly deferred design debt

The following items are known design questions. They are not missing recovery work and must not be silently "fixed" by unrelated stages.

### Fishing environment eligibility

**Binary substantial-water eligibility — DEFERRED**

The current tolerant substantial-water threshold is accepted for the recovered vertical slice. Similar-looking bodies of water can still fall on opposite sides of the custom-fishing threshold.

Revisit only during a future ecology/environment pass. Do not block the current Fishing implementation on this issue.

### Materials and resource economy

**Material sink identity — DEFERRED**

Titanium, Tungsten, Mythril, Etherium, and Adamantium should each have a meaningful anchor use before final spawn rarity and long-term material economy are considered fully settled.

Resolve during future crafting/equipment/material-economy design rather than by changing worldgen opportunistically.

### Utility progression

**Deferred Utility concepts — DEFERRED**

The active Utility roster and its mechanics are now defined in `UTILITY_EQUIPMENT_FINALIZATION.md`.

Glider, Oxygen Tank, and Grappling Hook are intentionally deferred and are not part of the active implementation-ready Utility roster.

Equipment-form Jet Boots and Return Crystal are retired/deleted from Utility Equipment and must not be reintroduced by treating older movement-overlap notes as current authority. The separate Jet Boots enchantment remains authoritative under `ENCHANTMENT_BRANCH_SKELETON.md`.

### Alchemy discoverability

**Brewing discovery UX — DEFERRED**

Alchemy remains a Brewing Stand extension, but the custom effects/catalysts need discoverability support such as advancement hints, ingredient tooltips, recipe hints, or a compact discovery record.

Avoid turning this into a full combinatorial catalyst matrix. Resolve during the Alchemy UX/content pass.

### Boss and higher-tier dependency order

**Boss -> T3/T4 reward dependency — DEFERRED**

Do not hard-lock T3/T4 recipes around undefined boss drops.

Preferred order for later design:

1. boss role and encounter identity
2. reward identity
3. rare material/drop
4. T3/T4 recipe/progression connection

### T4 progression

**Generic kill-to-Mastery farming risk — DEFERRED**

Do not implement unrestricted generic monster-kill XP as the final T4 progression contract without a dedicated design review.

If T4 Mastery is retained later, its progression source should avoid making weak-mob farming the dominant strategy.

### Progression-system proliferation

**T3 separate growth system — REVIEW BEFORE IMPLEMENTATION**

Ordinary Enhancement already covers T1/T2. T3 should not automatically receive another independent grind system merely because an older document proposed one.

Re-evaluate whether T3 needs any separate growth axis before implementation.

### Coating

**Coating — TEMPORARILY ON HOLD**

Coating is not deleted from project history, but it is not part of the active progression implementation.

Do not implement, rebalance, expand, or connect Coating to T3/T4 until higher-tier armor/progression is reconsidered. The historical coating matrix is reference material only.

## Idea parking lot

The following concepts are intentionally preserved only as ideas. They are not current design authority and are not implementation-ready.

### New gameplay-axis concepts

**New gameplay-axis design — DEFERRED BY DEFAULT**

New standalone or cross-system gameplay axes that are not already part of current authority are intentionally deferred.

Examples include:

- Knowledge / Research,
- Trading as a new independent economy/exchange axis (this does not prohibit targeted vanilla villager-trade integration already routed through the current villager/book checkpoint),
- other newly proposed progression, economy, research, social, or cross-system axes.

Preserve promising concepts here at idea level only. Do not expand them into detailed design, promote them into current authority, or implement them unless a later human-approved design task explicitly selects that concept.

### Knowledge / Research (historical superseded proposal)

**Historical only — NOT current authority; V4 research is approved above.**

~~Knowledge / Research — IDEA / DEFERRED~~

Potential new horizontal progression axis.

Possible direction:

- spend vanilla XP to acquire persistent knowledge,
- provide small benefits across multiple gameplay systems,
- do not gate recipes or ordinary content access.

Exact categories, effects, progression structure, balance, and UI are intentionally undefined.

Do not implement or infer missing design from this idea entry. A dedicated design decision is required before promotion into current authority or implementation scope.

## Live validation

Items marked LIVE PENDING in the Stage 2/Stage 3 reconciliation documents remain live-test work rather than documentation defects.

Headless CI must not be treated as proof of combat feel, visual presentation, interaction feel, or economy balance.

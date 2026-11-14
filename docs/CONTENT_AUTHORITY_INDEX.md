
> **HISTORICAL UI DECISION — SUPERSEDED (2026-10-10):** The earlier three-active-choice selector and unapproved landing-window note remain in the linked record for history only. The current approved decision is four visible cards: 발전 과제, 지식, 연구 are active; XP 퀘스트 is disabled and marked for later. Current authority is recorded below and in the amended UI documents. **The XP 퀘스트 card's disabled/later status is itself superseded by the XP Quest amendment — 2026-10-17 (see the XP Quest section below).**

# Current content authority and deferred design debt

> **ST-05 CURRENT OVERRIDE (2026-11-02):** For Ancient Colosseum participation, absence grace, wall-clock trial time, restarts, and cooldown, use [Stage 1 lifecycle authority reconciliation](st05/ST05_STAGE1_LIFECYCLE_AUTHORITY_RECONCILIATION_2026-11-02.md) before older Q60/Q112-A text. This routes existing owner approvals only; it does not approve merging Draft PR #219 or certify live-client validation.


This file is the compact authority map for the recovered cumulative line.

It does not replace the detailed design documents. When an older document conflicts with a newer reconciliation document listed here, the newer reconciliation is authoritative for implementation.

## Latest T2 material, recipe, and acquisition authority — 2026-10-27 (OWNER APPROVED, DESIGN ONLY)

> **CURRENT OVERRIDE for T2 crafting and elemental core acquisition.** Read [Sky Islands / T2 Context — §18 Q1–Q21 reconciliation](design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md#18-2026-10-27-t2-q1q21-최종-설계-통합-체크포인트--current-owner-authority) first. This section is **design authority**, not implementation, CI, or live-test acceptance.

- §18 takes precedence over earlier T2 recipe proposals in the same document and over the Oct 7 continuation/implementation acquisition assumptions below. Retain older records as **SUPERSEDED/HISTORICAL**, not active instructions.
- Approved six T2 equipment recipes: Fire Sword, Water Sword, Wind Sword, Wind Bow, Earth Mace, Lightning Sword; all use **Etherium Ingot ×1** with approved weapon-specific ingredients and explicit 3×3 shaped layouts. Dark Energy Staff remains deferred/placeholder.

## 2026-10-27 sky-islands and T2 design updates (design only)

The newest owner-approved Sky Islands ecology, floating ruins, Sky Temple (distinct from the **cancelled ST-02 Sun Temple**), aerial trial tower, flying mobs, Sky Feather and T2 weapon material amendments are recorded in [SKY_ISLANDS_T2_CONTEXT_2026-10-27.md](design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md). Later numbered sections in that document supersede earlier provisional descriptions on the same point. In particular, the 2026-10-27 Earth Mace amendment changes the **bottom Netherite Ingot to Etherium Ingot** while retaining the Ancient Heart centre required by the 2026-10-26 [F-2 reconciliation](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md). The later lightning-sword recipe correction in section 17 supersedes section 16. No code, recipe, worldgen or live implementation is implied. The [structure portfolio](STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md) preserves ST-02 as a cancelled historical ID.

## SKY-MOB celestial normal mobs — Issue #230 registered (2026-11-06, DESIGN ONLY)

The five Sky Realm normal mobs (Cloud Wraith, Skyguard Archer, Crystal Bug, Cloud Slime, Fallen Sentinel; Korean names in the linked record) are governed by **MoreMinecrafts-Beta Issue #230 — SKY-MOB Q01–Q87**, registered in [SKY_MOB_NORMAL_MOBS_AUTHORITY_2026-11-06.md](design/SKY_MOB_NORMAL_MOBS_AUTHORITY_2026-11-06.md). They are **added alongside, not instead of**, the nine mobs preserved in [SKY_ISLANDS_T2_CONTEXT_2026-10-27.md](design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md) §3/§5 (four new flying/special entities + five vanilla hostile variants), for a total plan of **14** Sky Realm mobs; see that document's §19. The three groups keep separate roles and are not merged. The existing [sky habitat contract](validation/w16-sky-habitat/README.md) is unchanged; the registration document §4 separates sky-island *region* membership from *spawn-surface* judgement. Unresolved conflicts (C2, C4, C5, V1–V6, spawn weights, combo table) are recorded there as owner-decision-pending and are not resolved by this index. No code, NBT, worldgen or live validation is implied.

**2026-11-13 owner update:** the owner resumed Sky Mob work for authority synchronization and registration/spawn/Sky Tower preparation. Section 15 of that record holds the confirmed items: R1-R3 identity and no group B/C merge; N1-N7 natural spawn, with relative weights 24/18/20/24/14 in a sky-island-only list, Q29 and Q80 kept, and Q33 tower block; T1-T6 Sky Tower, covering 4 floors / 16 spawners, unchanged spawner behaviour, group C-only 2-3 mob equal-weight combos and no change to old tower/temple/ruin spawners; O1-O3 ominous and Cloud Slime, covering no random equipment per Q55 and a split investigation only. Pending items P1-P7 were then resolved by the owner the same day (section 15.6: Q34 amendment, Q31 list, Q80 piece-bounds region, combos and floor layout, Cloud Slime medium/large 3:1, vanilla ominous behaviour, full replacement of the 16 tower zombie configs). The Blood Seraph look (PR #265 head `dcd93ba`) is owner final-approved in section 14. Implementation was approved on 2026-11-13 on `task/sky-mob-register-and-spawn`; implemented does not mean live-validated.

P1 implementation findings, including approved SKY-MOB-P1-Q01 B handling, are tracked separately in [SKY_MOB_P1_IMPLEMENTATION_AUDIT_2026-11-06.md](design/SKY_MOB_P1_IMPLEMENTATION_AUDIT_2026-11-06.md). That record does not amend Issue #230 or D1–D6; the entry above remains design authority only.

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

The five T2 elemental core identities and acquisition sources are locked by the Oct 7 continuation record. Current code adds rare loot to the vanilla normal/ominous Trial reward pools, Bastion treasure, and Ancient City tables, plus a saved one-time Ocean Monument completion claim. Chance values and the Trial table mapping are provisional; no recipes or equipment links are authorized until content demand is known. **Exception (F-2, 2026-10-26):** one shaped Earth Mace recipe using Ancient Heart is owner-approved; see the [Phase 2 reconciliation](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md#f-2--ancient-heart-replaces-tomb-guardian-core). It is not implemented, and no other T2 recipe is authorized.

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

Owner decisions F-1, F-2, F-3, F-5, F-6 and the mercenary status are recorded in [PHASE2_OWNER_DECISIONS_2026-10-26.md](preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md). They are applied to the affected documents by the [Phase 2 authority reconciliation](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md), which wins over those documents on the specific points it names and leaves every other approved rule unchanged. In short: the Tomb Guardian's drop is **Ancient Heart**, which replaces Tomb Guardian Core in the approved shaped Earth Mace recipe, and the Ancient City remains an Ancient Heart source; Adamantium generates at Y 8–22 (every generated block, per the 2026-10-28 F-3 amendment), biased toward Y 15, in veins of 1–2, one attempt per chunk and one placed feature, and must produce fewer ore blocks than Ancient Debris, targeting about 50% of Ancient Debris in matched samples (aggregate; supersedes the ~80% tuning target; candidate E `scattered_ore` size 2, origin 9–21); the rare caravan never sells special contracts directly, and the 5% Advanced-to-Special conversion is kept; the Ancient City arena and ST-05 are separate structures; mercenaries are a territory-independent CANDIDATE / DEFERRED concept; the wither-skull quest counts only the first pickup of a genuinely dropped skull and stays unregistered. Recovered records and their source commits: [provenance manifest](preservation/PROVENANCE_MANIFEST_2026-10-26.md). These are design records only; nothing in them is implemented or validated by this route.

- **Stage 23-A remaining research:** [RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md](preservation/recovered/stage23a-remaining-approval/RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md) is APPROVED design authority for the topics it names. Its unpriced and unspecified items remain open. For `v3.research.53` Villager Trading, the current approved L1/L2/L3 first-trade discounts are 5%/10%/15%, player-specific, with minimum price 1; calculate the percentage discount by **rounding the discount amount upward**, as stated in the [current research authority](RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md#villager-trading-discount-rounding--owner-approved-2026-10-26). The historical territory record is provenance, not the sole current authority.
- **Structures** (pyramid, pillager headquarters, structure candidates, Ancient City arena versus ST-05): [STRUCTURE_AUTHORITY_INDEX.md](STRUCTURE_AUTHORITY_INDEX.md).
- **Territory:** SHELVED per [TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md](preservation/recovered/territory-chain/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md). The other files in that recovered folder are historical reference only and must not be implemented.
- **Independent designs from the territory chain:** world event [pillager patrols](preservation/recovered/territory-chain/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md) (design finalized); [Adamantium equipment](preservation/recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) (approved v1, worldgen amended by F-3 and its 2026-10-28 amendment); [rare caravan and runes](preservation/recovered/territory-chain/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md) (user-confirmed; read with the [conflict register](preservation/recovered/territory-chain/DESIGN_CONFLICT_REGISTER.md) F20/F21 and F-5). Mercenary and desert-artifact material in [DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md](preservation/recovered/territory-chain/DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md) is candidate or historical only.
- **Owner follow-up decisions Q1–Q4 (2026-10-26)** are recorded in the [reconciliation](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md#owner-follow-up-decisions--q1q4-2026-10-26) and are approved design authority, not evidence of implementation:
  - **Q1:** the 2026-10-22 contract-item system (`BOUNTY_CONTRACT_APPROVED_2026-10-22.md`) is the sole current bounty/contract authority. The 2026-10-19 [commission board](preservation/recovered/territory-chain/BOUNTY_COMMISSION_APPROVED_2026-10-19.md) is a historical record only and must not be implemented or restored.
  - **Q2:** rare caravan merchants may directly sell ordinary Beginner, Intermediate and Advanced contracts. They may never directly sell Special contracts. Caravan prices, stock weights and quantity limits are open for balance design.
  - **Q3 / OD-1:** Preserve the existing implemented T1 Feather Boots enchantment under [T1 finalization](T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md) and the retained [enchantment skeleton](ENCHANTMENT_BRANCH_SKELETON.md). Only its proposed 2026-10-10 double-jump/fall-immunity redesign and Hot Step (뜨거운 걸음) remain CANDIDATE / DEFERRED / NOT AUTHORIZED FOR IMPLEMENTATION. The [design idea decisions](preservation/recovered/content-first-axis-review/DESIGN_IDEA_DECISIONS_2026-10-10.md) remain historical; do not remove or disable the current enchantment.
  - **Q4:** the Tomb Guardian's core signature drop is Ancient Heart. Do not add a guardian-exclusive material or reintroduce Tomb Guardian Core. Its additional rare-mineral and higher-tier-material reward pool is approved in direction only; items, quantities and rates are not yet approved.

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

## D13-03 — T2 accessory acquisition Q01–Q47 (owner-approved design reconstruction, 2026-11-06)

[Separate D13-03 acquisition decisions](planning/D13_03_DECISIONS_Q01-Q47.md) document 47 owner-approved acquisition, probability, allocation and verification decisions for 11 T2 accessories. Source is the user-supplied reconstruction of a ChatGPT design conversation, not verbatim question transcripts. **D13-03-Q01–Q47 are distinct from D13-DESIGN-Q01–Q72**, which remain pinned to [Draft PR #215](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/fdaeac6c0c9032691d77813274627601b3f53e0a/docs/planning/D13_ACCESSORY_ACQUISITION_DECISIONS_Q01-Q11_2026-11-04.md). Preserve PR #228's catalyst implementation and all existing loot. This registration is design authority only, not implementation or integration acceptance. Snow Charm remains deferred.


## ST-01 royal chest reward authority — I06 Q01–Q45 (owner approved 2026-11-07; design only)

For the two designated ST-01 royal reward chests, the current owner-approved **reward quantities, probabilities, 54-slot inventory, access, seal, automation/insertion protection, catalyst assignment, and persistence contract** are recorded in [D13-03-I06 Royal Chest Reward Authority](planning/D13_03_I06_ROYAL_CHEST_REWARDS_AUTHORITY_2026-11-07.md). This I06-specific overlay supersedes older provisional GUI/hopper-first-evaluation semantics in D13-03 Q25–Q47 and PR #237, while retaining unrelated D13 gameplay. Q45 approves titanium and tungsten **ingots**. ST-01 P4 spatial candidate **B** is owner-selected; physical orientation and final integration are tracked separately. This is **DESIGN ONLY**, not implementation or live validation.

## ST-01 G0 geometry reconciliation — 2026-11-10

[G0 authority matrix](design/st01-g0/G0-A-AUTHORITY.md) records the owner's supplied C1 constraints (32/8 TRIANGULAR; height variation12/cutting8/skirt6; complete pyramid; four verified rotations; reject unsafe conflicts; footprint-based ground with entrance access). Its linked architecture packet is **G0 BLOCKED / proposal only**, not approval of provisional room geometry or changes to D1. The immutable-D1/secret-descent and independent-WEST conflicts require explicit owner resolution. Existing approved D1, canonical identity, absent SECONDARY and reward policy remain unchanged.

## ST-01 G0-R1 approved interface delta — 2026-11-10

The owner explicitly approved [R1-01/R1-02](design/st01-g0-r1/AUTHORITY-DELTA.md): keep original D1 bytes; permit only the enumerated twelve Y−24 assembly seam cells in addition to the separately governed Y−23 cover; keep D1/treasury/crypt fixed and select EAST/WEST access routes rather than mirroring chambers. These decisions supersede only the two conflicting G0 interface assumptions. The [G0-R1 blueprint](design/st01-g0-r1/README.md) is **DESIGN READY FOR OWNER APPROVAL**, not approval of numerical room counts, new WEST coordinates, crypt dimensions, palette or decorative/treasury representations. Production construction and all other approved constraints remain unchanged pending the stated approvals.

## ST-01 G0-R1 owner approval and P8-C2-A construction — 2026-11-10

The owner approved all three G0-R1 bundles (numerical architecture; WEST access route; visual/physical representation for initial construction) for exactly commit `c26ea0f871e788d1d331b40151779d86590469f2`. [The approval record](design/st01-p8c2a/G0-R1-OWNER-APPROVAL-RECORD.md) is the current construction authority for the complete ST-01 pyramid geometry, palette, decoration and block-entity representation. It supersedes the PROPOSED / CONDITIONALLY_APPROVED markers in the G0-R1 packet only to the extent of those bundles. It does not authorize new loot, reward, mineability, protection, secret-unlock, route-selection runtime or natural-worldgen behavior, and does not change the PRIMARY chest, Guardian, D1 bytes or the P8-C1 worldgen policy. The P8-C2-A construction of the assets is documented in [design/st01-p8c2a/README.md](design/st01-p8c2a/README.md).

## ST-01 natural-worldgen placement and MODERATE terrain amendment — 2026-11-10

The owner approved the locked ST-01 natural-worldgen configuration (81×81 full-Desert footprint; spacing 32, separation 8, TRIANGULAR, salt 91401; MODERATE terrain: height variation ≤16, cut ≤10, skirt ≤8; underground fluid safety required). [The amendment record](design/st01-p8c2b/AUTHORITY-AMENDMENT-O14-MODERATE.md) is the current authority for these values. It supersedes the C1 12/8/6 terrain values in the G0-A matrix and the G0-R1 AUTHORITY-DELTA **only** as worldgen terrain-eligibility/adaptation limits; those records are preserved unchanged. It does not change pyramid geometry, D1, the PRIMARY chest, canonical identity, SECONDARY, Guardian or reward policy, and does not approve a vanilla-structure overlap policy.

## ST-01 Policy D — owner-approved natural-worldgen eligibility — 2026-11-11

The owner approved **Policy D** as final: ST-01 natural generation no longer rejects a candidate for entrance obstruction or underground fluid; buried entrances and natural fluid irregularities are accepted outcomes. [The decision record](design/st01-p8c2c/OWNER-DECISION-POLICY-D.md) is the current authority and supersedes the "underground fluid safety required" clause of the amendment above (and the entrance-approach rule of P8-C2-B) **only** for those two criteria, prospectively. Footprint, Desert, spacing/separation/spread/salt, MODERATE terrain limits, rotation, route, assembly geometry, royal chest, Guardian and reward design are unchanged. The [counterfactual rarity audit](design/st01-p8c2b/ST01_P8C2B_COUNTERFACTUAL_RARITY_AUDIT_2026-11-11.md) is preserved as historical evidence. A future exploration-discovery direction (contracts, villager trading, location information) is recorded as direction only in the [future-design note](design/st01-p8c2c/FUTURE-DESIGN-NOTE-EXPLORATION-DISCOVERY.md); it is not implemented.

## ST-01 vanilla overlap permitted; P8-D1 discovery design proposals — 2026-11-11

The owner confirmed that overlap between natural ST-01 Pyramids and vanilla structures (villages, mineshafts, …) is **permitted** and must not become a generation rejection condition: [amendment](design/st01-p8d1/AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED.md) (supersedes only the "overlap policy unresolved" status, prospectively; historical records preserved). The [P8-D1 design package](design/st01-p8d1/README.md) (bounty-contract and villager-trade discovery as two independent channels, shared location architecture) is a **proposal awaiting owner decisions — not approved and not implemented**. The ST-01 worldgen and Policy D remain on unmerged branches (PRs #259/#262/#266), not on `main`.

## ST-01 explorer-map direction (A4, B1, hybrid) and P8-D2 feasibility — 2026-11-11

The owner approved, as **direction only**: Channel A4 (bounty system must eventually provide a vanilla-style ST-01 explorer map), Channel B1 (villager trading must eventually provide ST-01 explorer maps) and Architecture 3 (hybrid: vanilla where safe, minimal custom infrastructure); the channels stay independent. [The decision record](design/st01-p8d2/OWNER_DECISION_A4_B1_HYBRID.md) lists what is approved and what remains provisional (acquisition mechanics, profession, level, price, stock, refresh). The [P8-D2 feasibility package](design/st01-p8d2/README.md) contains measurements and **proposals awaiting owner decisions — none is approved or implemented**. It supersedes P8-D1 decisions D2/D3 (format of the two channels) only. The ST-01 worldgen, Policy D and the P8-D1 package remain on unmerged branches (PRs #266, #268), not on `main`.

## ST-01 P8-D2-R1 structure-search audit — 2026-11-11

[Investigation package](design/st01-p8d2-r1/README.md) (documentation and evidence only; **no decision, no implementation**): reconstructs the P8-D2 benchmark (vanilla `findNearestMapStructure` with ST-01's unchanged production eligibility), attributes 97.8 % of search time to terrain height sampling and ≈ 20 % to a duplicate evaluation of each successful candidate, confirms the Desert prefilter already exists, and recommends caching/orchestration (R3) plus an exact-equivalent eligibility reordering (R2) for owner decision.

## ST-01 cartographer explorer-map trade (Channel B1) — 2026-11-13

The owner decided the Channel B1 trade: **cartographer, level 4, 16 emeralds + 1 compass, offered with probability 50 %**, with a non-blocking unpurchasable placeholder replaced in the same slot once the search is prepared (retry limits, substitute trade when no ST-01 is found, self-expiring target-chunk preload). [The decision record](design/st01-p8d8/OWNER-DECISION-CARTOGRAPHER-MAP-TRADE.md) is the current authority for these items and supersedes the corresponding *provisional* B1 items (profession, level, price) of the P8-D2 record; the remaining B1 items (radius, stock, refresh, icon, repeat sales, unclaimed-only) use vanilla explorer-map values listed there and stay revisable. Placement, Policy D and all other ST-01 authority are unchanged.

## Grand integration 2026-11-11 — design records, audit evidence and diagnostic tooling routed — no implementation

This section routes records that the unified integration brought onto `integration/unified-20261111`. It changes no gameplay value and implements nothing. Implementation status of each item is stated only as the integrated code shows it; see [GRAND_UNIFIED_20261111_MANIFEST.md](integration/GRAND_UNIFIED_20261111_MANIFEST.md) for source branches and SHAs.

* **ST-03 owner decision ledger, Q01–Q42** ([planning/ST03_OWNER_APPROVED_DESIGN_Q01_Q42_2026-11-04.md](planning/ST03_OWNER_APPROVED_DESIGN_Q01_Q42_2026-11-04.md), plus the Q106–Q108 implementation status snapshot on `feat/st03-implement-01-20261105` @ `7c9520a1`). Records approved design decisions only. Its own text names an external repository as the implementation coordinate authority. No ST-03 runtime code is integrated. **Owner to confirm** whether this ledger is the current ST-03 authority (not decided here).
* **D01 T1 accessory owner decisions, Q1–Q108** ([planning/T1_ACCESSORIES_D01_OWNER_DECISIONS_2026-10-30.md](planning/T1_ACCESSORIES_D01_OWNER_DECISIONS_2026-10-30.md)). The record is labelled "partial design, not an implementation instruction". Q39 stays deferred. The seven accessories it describes are implemented in code by `2bdf4462`; this routing does not change their validation status.
* **Content design inventory and roadmap, wave 15** ([planning/CONTENT_DESIGN_INVENTORY_AND_ROADMAP_2026-10-30.md](planning/CONTENT_DESIGN_INVENTORY_AND_ROADMAP_2026-10-30.md)). A proposal, not an approval. It also keeps reachable the commit `fdaeac6c` that the D13 provenance links above pin.
* **Sky Tower mountain worldgen height audit, Phase 1 and Phase 2** ([audits/sky_tower_worldgen_height/HEIGHT_VALIDATION_REPORT.md](audits/sky_tower_worldgen_height/HEIGHT_VALIDATION_REPORT.md)). Evidence only. The diagnostic GameTest `SkyTowerPhase2Probe` is inert for production behaviour.
* **ST-01 P8-C2-B counterfactual rarity audit report** ([design/st01-p8c2b/ST01_P8C2B_COUNTERFACTUAL_RARITY_AUDIT_2026-11-11.md](design/st01-p8c2b/ST01_P8C2B_COUNTERFACTUAL_RARITY_AUDIT_2026-11-11.md)) is historical evidence and is present. Its probe, tooling and workflow (`audit/st01-p8c2b-counterfactual-rarity-20261111` @ `569fc789`) are **not** integrated: the probe targets the pre-Policy-D `St01PlacementConfig` API and does not compile on this tree. Production is unchanged and Policy D remains the authority in the section above.
* **Paused or preserved, not integrated:** the mob track (paused by the owner), the ST-05 and pre-wave-16 lines, the ST-01 P7–P11 and O-14 secret-entrance and gold-golem line, and the counterfactual audit probe above. Each stays on its source branch. The manifest lists them with SHAs and reasons.

## Grand Unified working branch 2026-11-12 — routing only, no gameplay change

This section routes documents added by `work/grand-unified-20261112`. It changes no gameplay value, approves nothing and implements nothing. Source, history corrections and preservation references: [GRAND_UNIFIED_20261112_SOURCE_AND_HISTORY.md](integration/GRAND_UNIFIED_20261112_SOURCE_AND_HISTORY.md). Status of every feature: [FEATURE_STATUS_REGISTRY_20261112.md](integration/FEATURE_STATUS_REGISTRY_20261112.md). How each preserved development state is rebuilt without its original branch, with results: [RECOVERY_RUNBOOK_20261112.md](integration/RECOVERY_RUNBOOK_20261112.md). Whether each family can resume on the unified code: [FORWARD_DEVELOPMENT_20261112.md](integration/FORWARD_DEVELOPMENT_20261112.md). History archive: [HISTORY_ARCHIVE_20261112.md](integration/HISTORY_ARCHIVE_20261112.md).

* **Piglin trading and Mob Tracker UI decisions (2026-10-10), PENDING currency confirmation.** [UI_PIGLIN_TRADE_MOB_TRACKER_APPROVED_2026-10-10.md](UI_PIGLIN_TRADE_MOB_TRACKER_APPROVED_2026-10-10.md) is a user-decision record that states "design authority / implementation requirements only". It is consistent with U04 and U07 in [UI_SYSTEM_FULL_AUDIT_2026-10-10.md](UI_SYSTEM_FULL_AUDIT_2026-10-10.md). No implementation exists in the baseline. The owner has not re-confirmed that it is still current, so it is routed as pending, not as new authority.
* **Historical or non-authoritative copies** under [preservation/grand-unified-20261112/](preservation/grand-unified-20261112/README.md): the 2026-10-28 ST-05 owner design record (superseded by [ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md](ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md) and the 2026-11-02 lifecycle override), alchemy plot notes (explicitly non-authoritative) and the XP Quest UI integration checkpoint. They do not override any section above.
* **Inactive development assets** (Sky Mob, ST-01 P7-P11/O-14, ST-01 Phase 4 and terrain adaptation, ST-05 alternative controller and structure mask, Q1C Wither skull, client-QA and validation tools, deferred Drive mirror) are copied byte-identically under [../inactive/](../inactive/README.md). They are not compiled, registered or run, and nothing in them is authority. Sky Mob code stays inactive, but its design authority was resumed by the owner on 2026-11-13 (see the SKY-MOB entry above; implementation scope still pending); the Policy D decision above stays final; Policy B stays research; the Wither-skull quest stays unregistered.

## ST-01 secret entrance and gold guide golem — Policy D port (2026-11-12) — IMPLEMENTED, CI PASSED, LIVE VALIDATION PENDING

The owner approved the Policy D port of the preserved P7–P11 secret-entrance and gold-guide-golem runtime from [../inactive/st01-o14-p7-p11/](../inactive/st01-o14-p7-p11/SOURCE.md) onto the current single-`PRIMARY` ST-01 assembly (Draft PR #282; owner direction approved 2026-11-12). For that runtime only, this supersedes the "secret-unlock behavior not authorized" clause of the [G0-R1 owner approval record](design/st01-p8c2a/G0-R1-OWNER-APPROVAL-RECORD.md) and the "not integrated" status of the P7–P11 line in the two grand-integration sections above. It resolves P6 D-A and D-B as follows:

* **D-A (trigger):** after the Tomb Guardian is defeated (the existing persisted completion record that also unseals the `PRIMARY` chest) a gold guide golem appears at `BOSS_S` and walks to the stop point; its arrival opens the cover. If it does not arrive, the cover opens 600 game ticks after the defeat (P7 rules 1–5; P11 O-1/O-3/O-4/O-6: 600 ticks, timer starts at the defeat, 1.5-block arrival radius, stop point anchor+(9,−2,3), golem scale 0.5, speed 0.25).
* **D-B (restore):** the opening is permanent; there is no transition back to CLOSED.
* **Cut range:** exactly the 12 approved Y−23 cover cells (authored X−9..−6 / Z33..35, shared by both access routes), mapped with the whole-pyramid rotation. Whatever solid block occupies them is removed; fluids are left in place (Policy D items 5–7). Nothing outside those cells is written; no wider opening, ramp or excavation is added.
* **Unchanged:** Policy D terrain/eligibility parameters, single `PRIMARY` chest, Tomb Guardian behaviour and reward, D1 bytes and assembly geometry.
* **Existing behaviour kept (owner decision):** P9 CR-1/CR-2 and O-9 (what counts as a valid kill) are **not** implemented in this change; the existing credited-defeat behaviour is kept.
* **Provisional, not approved final values:** golem `WAIT_DISTANCE` = 10 blocks and leader-switch margin = 3 blocks are provisional P7/P8 candidates.
* **Still undecided (preserved):** O-12 opening VFX/SFX; D-D / O-5 open-edge drop; Q15 secret crypt specification; Q98 maximum underground depth.

Status: implemented on Draft PR #282 with local unit tests and GameTests passing (607/607 required GameTests). GitHub Actions CI passed (all 18 checks on PR head `7f0a2076`, including Fabric CI `build`). LIVE VALIDATION (client appearance of the golem, feel of the opening, multiplayer leader following) is **pending** and is a separate owner-approved step; the PR stays Draft and this is **not** a COMPLETE status. Code: `src/main/java/dev/itzgamesgd/moreminecrafts/pyramid/entrance/`.

## 2026-11-14 documentation sync — routing and status only

Routing/status corrections from owner-confirmed decisions; no gameplay value is invented. Details are in [integration/FEATURE_STATUS_REGISTRY_20261112.md](integration/FEATURE_STATUS_REGISTRY_20261112.md) ("2026-11-14 status update").

* **OWN-01 / OWN-02 / OWN-04:** resolved 2026-11-12~13 (Sky Mob registration/spawn; ST-01 Policy D secret entrance port, above; Q1C Wither-skull P3 and quest registration). Live validation still pending for the first two. The "Q1C Wither skull stays unregistered" and "mob track paused" statements in the 2026-11-11/12 sections above are superseded by this entry.
* **Pyramid ordinary mobs:** [PYRAMID_MOB_ROSTER_DESIGN_2026-10-26.md](PYRAMID_MOB_ROSTER_DESIGN_2026-10-26.md) — implemented, owner review passed (branch `claude/pyramid-mummy-variants-fu6ijm`, not merged here); HP 20/20/20/28 owner-confirmed; no conversion rule (variants are not converted).
* **Tomb Guardian:** [PYRAMID_DESIGN_PREP_2026-10-25.md](PYRAMID_DESIGN_PREP_2026-10-25.md) — the 2026-10-26 lock and 2026-10-28 Option B records were not owner-approved; design is being redone and the current implementation is a temporary dummy. **No current authority for the Tomb Guardian design (decision pending).**
* **Sky mobs:** group A held; group B vanilla-variant policy in [SKY_MOB_NORMAL_MOBS_AUTHORITY_2026-11-06.md](design/SKY_MOB_NORMAL_MOBS_AUTHORITY_2026-11-06.md) §15.7 and [SKY_ISLANDS_T2_CONTEXT_2026-10-27.md](design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md) §20; creeper/enderman/slime variant weights are decision pending.
* **ST-03 pillager headquarters:** [planning/ST03_OWNER_APPROVED_DESIGN_Q01_Q42_2026-11-04.md](planning/ST03_OWNER_APPROVED_DESIGN_Q01_Q42_2026-11-04.md) remains the design ledger; Q96–Q105 text does not exist and will not be recovered; new design only where needed; Draft PR #55 (structure pass-through of walls, towers, main gate and ground road) is implementation status, not authority. Whether the ledger is the current ST-03 authority is still for the owner to confirm (unchanged).
* **Spider colony:** [SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md](SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md) is the design record; implementation on `work/spider-colony-probe` is in progress, not connected to worldgen, textures provisional.


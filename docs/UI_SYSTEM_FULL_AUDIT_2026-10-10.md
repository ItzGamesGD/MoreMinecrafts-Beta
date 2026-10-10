# MoreMinecrafts — UI/System Inventory and Visual-Approval Audit
Date: 2026-10-10. Scope: current Draft PR #75 branch tree, V4 research design (938 lines before this audit), content authority, utility, fishing, equipment, T1 enchantment, and current design finalization records. This is a **design/source-document and repository inventory audit**, not an assertion that all code paths or 70 research effects have been functionally tested.

## Authority and exclusions
- Latest approved visual baseline: [UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md](UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md).
- Advancements + free Knowledge + paid Research in the **same vanilla-advancement-key experience**, with XP Quests reserved for later. Knowledge tooltip: **해금 조건 / 해금 보상**. Research: prerequisite, materials, raw XP, explicit purchase. Seven disciplines. No mandatory T1→T2→T3 chain.
- Older V4 design sections calling for a separate L-key 3-way landing screen, an obligatory radial hub, and a completely separate 85–95% research UI are **historical superseded alternatives**; preserve only nonconflicting interaction details. Do not treat mockup sample costs/labels as canonical.
- **REJECTED**: automatic crafting and its JEI-like selector; controlled logging, chain logging, field repair, auto-feeder, standalone Jet Boots, removed Fishing Net. Do not create UI for them.
- **DEFERRED**: XP Quest screen, Rope Arrow behavior/design, other unapproved content. No invented standalone UI for ordinary movement/combat passive effects.
- Draft PR #75 has implementation experiments, not UI sign-off. Its AutoFarmScreen is **provisional/unapproved**, not the visual reference. No merge authorization.

## A. NEW OR REWORKED UI REQUIRING VISUAL DESIGN APPROVAL (15 groups)

| ID | System | UI form / required elements | Source status | Design priority |
|---|---|---|---|---|
| U01 | Advancements + Knowledge + Research | Unified vanilla-advancement-style tree, 7 discipline tabs, node icons/sizes/click behavior; knowledge condition + reward tooltip; research prereqs/costs/confirm purchase | **APPROVED concept and visual direction**; precise geometry and art not final | **P0 approved baseline** |
| U02 | Auto Farm | Block UI, 5×5 replanting stock + 5×5 collected output; inventory transfer and state; 9×9 farm below farmland | **APPROVED mechanic; screen visual not approved** | **P1** |
| U03 | Automated Lumber Device | 1×1 block right-click, axe slot + coal/fuel slot, no output storage | **APPROVED mechanic** | P1 |
| U04 | Mob Tracker | Species selection/filter, glowing target indicator, radius 32 loaded blocks | **APPROVED concept; UI arrangement pending** | P1 |
| U05 | Brewing Process Control | **Extension of vanilla brewing stand**: 9 waiting ingredient slots + 9 finished output slots, goal recipe selection, ON/OFF, intermediate state | **APPROVED mechanics; layout pending** | P1 |
| U06 | Potion Mixing / Concentration | **Additional tab on vanilla brewing stand**, two compatible finished potions, resulting combined effects | **APPROVED mechanic; UI pending** | P1 |
| U07 | Piglin Trading | Villager-like trade interaction and offers; preserve original random barter research | **APPROVED concept; trade inventory/prices pending** | P1 |
| U08 | Nearby Chest Ingredient Reference | **Existing crafting table recipe-book extension**: preview craftability using chests in 16×16-block horizontal area; autofill withdrawal, cancel-return | **APPROVED behavior; visual display conditional on feasibility** | P1 |
| U09 | Hopper Filtering | Small vanilla-hopper extension/filter controls; do not invent large logistics dashboard | **Concept approved, exact UI/whitelist mechanics deferred** | P2 |
| U10 | Chest Sorting | **Existing chest UI extension**, exactly two side controls: item/name sort and category/tag sort; no quantity sort | **APPROVED** | P1 |
| U11 | Villager Trading Stock | **Existing villager trade UI extension**, current/max stock labels and cost research effect | **APPROVED** | P2 |
| U12 | Motorized Boat | **Shift-right-click small fuel-slot UI**, throttle via hotbar slots 1–9, no giant inventory | **APPROVED mechanic** | P2 |
| U13 | Cross-Dimensional Item Transport | Destination/home storage status or minimal confirmation/feedback if needed; server-owned queued transfer | **Mechanic adopted conditionally; UI itself NOT APPROVED/required** | P3 / avoid UI until safety proven |
| U14 | Potion Refining/Concentrating T3 | Distinct refining station if the approved research is implemented; actual station slots/layout not settled | **Research accepted, station UI detail pending** | P3 |
| U15 | Enchantment skill-tree & book option/enhancement | Existing dedicated equipment progression screen and anvil-like modes; retain their previously approved layout rather than designing duplicate research UI | **Existing implemented UI / separate authority** | Existing review only |

## B. EXISTING VANILLA-DERIVED EQUIPMENT/ACTIVITY UI (9 groups)
These already have design or implementation contracts and should be **audited for consistency**, not arbitrarily replaced.

| ID | System | Required form | Notes |
|---|---|---|---|
| E01 | Miner Lantern helmet socket | One square slot immediately left of helmet in player inventory | Existing contract; check real visibility and hitbox |
| E02 | Potion Injector | 9-slot inventory, left-click open, right-click sequential use | Existing contract |
| E03 | Portable Furnace | Vanilla furnace-style input/fuel/output, 16 stack cap, drop on close | Existing contract |
| E04 | Mining Drill | Small furnace-like fuel slot and FP state | Existing contract |
| E05 | Backpack | Vanilla chest 9/27/54 slots, player-bound shared storage, chest attachment | Existing contract |
| E06 | Fishing Rod modules | One-row Reel/Crankbait/Minnow menu, left-click to open | Existing contract |
| E07 | Fishing minigame HUD | Green/yellow/orange/red zones, progress and success timing | Existing visual validation backlog |
| E08 | Fish Trap | Existing nine-slot container, vanilla chest grammar | Existing contract |
| E09 | T1 Enchantment / Anvil / Book Option | Existing skill-tree + book/upgrade/combination screens | Existing PR validation screenshots; not a new Research purchase screen |

## C. VISUAL ASSETS OR IN-WORLD INDICATORS, NOT NEW UI (11 groups)
- V01 Auto Farm **block model/item icon** (machine is under farmland).
- V02 Automated Lumber Device **block model/item icon** (one-block footprint).
- V03 Irrigation Device **block model** with SIX charge appearances, 0–5, **NO complex GUI**.
- V04 Mob Capture Tool **item icon/model**; 30 durability.
- V05 Captured Individual Spawn Egg **item icon** and per-individual identifying tooltip; NBT/data safety first.
- V06 Mob Tracker **held item icon/model**, plus target glowing marker.
- V07 Motorized Boat engine/attachment **in-world appearance**.
- V08 Research material compressed tokens **existing item texture + enchant glint**, clear tooltip; research-only.
- V09 Parachute **ordinary item icon only**; no deployed 3D model, Slow Falling I.
- V10 Ballistic Control **projected impact-point marker**, no autoaim and no new menu.
- V11 Fishing modules, custom fish/item icons, fish minigame HUD, plus already-existing enchantment icons: **existing asset validation**, not redesign by default.

## D. NO SEPARATE UI REQUIRED
Wall Climbing + Ledge Hanging (W / Shift), Wall Jump (Space), Fall Roll (Shift, no animation), Mob Aggro Redirection, Throwable Pickaxe, Shield Counter, Bow Charging Mobility, Falling Axe Smash, Weapon Swap Handling, automatic tool switching, knowledge-gated recipes themselves, precision smelting/crafting, farming/sword passive bonuses. The knowledge/research nodes still appear in U01; do not create a second controls menu. Rope Arrow deferred.

## E. Findings requiring attention before UI image batches
1. **Critical stale text** in RESEARCH_KNOWLEDGE_TIER_DESIGN: older mandatory T1→T2→T3 and independent three-way screen paragraphs conflict with newer approvals. The V4 and dedicated UI approval documents override; code must follow newer authority.
2. **Automatic crafting** remains described at length in older appendices but explicitly abandoned later. Never add its screen to design inventory.
3. **Auto Farm**: current Draft PR #75 has AutoFarmScreen but it is **not user-approved art**. Code must be reconciled with approved image; 25+25 inventory, direct drops, seed-first replanting, bottom-only hopper, no forced chunks. Visual acceptance and integrity tests remain.
4. **Auto Farm earlier top-hopper text conflicts** with later no-top-hopper directive. Latest rule wins: only bottom hopper extracts results; seed stock supplied through GUI unless another input method approved.
5. **Lumber machine** early coal-per-five note conflicts with latest one-coal-per-eight-logs. Latest 1/8 is authoritative.
6. **Knowledge discoveries**: current Draft PR #75 contains partial runtime/recipe implementations, NOT complete 70-node acquisition or server-side purchase system. Do not claim U01 is implemented.
7. **Cross-dimensional transport**: no new GUI approved, and feature itself should be abandoned if transactional safety/complexity cannot be controlled. Treat U13 as conditional, not a confirmed screen.
8. **UI count interpretation**: U01–U15 are 15 design/extension groups, NOT 15 distinct new full-screen interfaces; U08/U10/U11/U12 and brewing modes extend vanilla UI. E01–E09 are existing screen contracts. V01–V11 are assets/indicators, not windows.
9. **Quest XP** remains deferred; reserve navigation integration only.
10. **Progression cost reconciliation**: 17 new research price lines in V4 addendum are a delegated planning schedule. Deduplicate against older node identities before final UI displays exact XP; avoid hardcoded mockup costs.
11. **Unapproved visual implementation** in Draft PR #75 must not be taken as acceptance; keep PR draft and don't merge on smoke tests alone.

## Proposed image-design sequence (no generation performed by this audit)
1. U01 unified advancement/knowledge/research: **already visually approved**, only knowledge tooltip fields added; use as shared vanilla style reference.
2. U02 Auto Farm UI, then U03 Automated Lumber Device UI.
3. U05/U06 brewing stand extension and potion mixing tabs, then U07 Piglin Trading.
4. U04 Mob Tracker, U08 crafting-table recipe-book extension, U10 Chest Sort, U09 Hopper Filter.
5. U11 villager stock, U12 boat fuel, U14 potion refining (after detailed design).
6. E01–E09 only if existing visual audit identifies a genuine mismatch. V01–V11 as a **separate asset design batch**.

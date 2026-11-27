> **HISTORICAL DECISION — SUPERSEDED (2026-10-10):** The three-choice selector correction below is retained as an earlier record. The later approved concept reference and direct clarification specify four visible cards, with XP Quest disabled and marked for later.

# MoreMinecrafts — Approved Advancement / Knowledge / Research UI (2026-10-10)

Status: **USER-APPROVED DESIGN AUTHORITY — UI AND VISUAL REFERENCE**. The attached approved concept reference set and direct implementation instruction supersede conflicting earlier selector/device-screen statements in this file. This task authorizes implementation of the in-scope UI.

## Seven-sector navigation follow-up — 2026-10-11

The later user task and `MoreMinecrafts_Vanilla_UI_Alignment_v1.zip` supersede only the older navigation/grouping decision below that ruled out a separate seven-sector selector. Preserve the existing four-card selector unchanged. Research and Knowledge each enter the same navigation-only seven-sector screen, then open the selected discipline's node canvas; Back returns to the sector screen, and the sector screen returns to the four-card selector. The sector screen has no purchase nodes.

Use the existing source discipline IDs and display names: `resources` / 자원학, `production` / 생산공학, `living` / 생활기술학, `combat` / 전투학, `ecology` / 생태학, `world` / 세계학, and `alchemy` / 연금·마법학. The 7-sector package supplies geometry and hit-area reference only. Render the menu in the Minecraft advancement visual language with the default font, vanilla advancement background/frame sprites, and actual item icons; do not import its preview or selected-sector PNGs as final art.

The selected discipline's tree reuses vanilla advancement box/frame sprites, item rendering, tooltips and connector style. Tree-only mouse-wheel zoom and blank-canvas drag remain separate from sector selection. Existing knowledge discovery/unlock behavior, research costs/prerequisites, server-authoritative purchase checks, detail view and confirm/cancel flow remain unchanged. This follow-up changes navigation and presentation only.

## Superseding selector and device decisions — 2026-10-10

- Pressing the vanilla advancement key opens a selector with four visible cards: **발전 과제**, **지식**, and **연구** are active; **XP 퀘스트** is visibly disabled and marked as coming later. The disabled Quest card opens no screen. The first three cards each open their corresponding separate screen. Vanilla Advancements content remains accessible. *(Superseded for the XP Quest card by the XP Quest amendment — 2026-10-17 at the end of this document.)*
- Use the attached selector concept art for the card composition, borders, colors, Korean labels, disabled Quest treatment, and close button. Exact texture assets may use compatible vanilla resources when the concept illustration files are not available in the repository.
- Chest sorting uses the standalone chest reference and its four controls: sort by type, merge identical items, tidy empty slots, and toggle sort direction. The combined chest/hopper image is not the chest authority.
- Hopper filtering uses only the hopper portion of the combined reference: hopper slots, allow-list selector, filter slots, and NBT-ignore toggle. Matching and transfer behavior remain governed by gameplay authority; the picture does not define additional mechanics.
- Mob Tracker exposes species selection and distance choices 8, 16, 32, 64, and 128. Existing range meaning, loaded-chunk limits, and performance must be validated before changing detection behavior.
- Motorized Boat has a fuel interface and retains its existing throttle behavior. The lower slots in its reference are the player's ordinary inventory; no boat-specific storage is added.
- Brewing Process Control, vanilla brewing mixing, and Potion Concentration/Refining are separate features. Potion Concentration is a distinct device interface, not a brewing-stand tab or a replacement for brewing process control.

## Main experience

- Use the **vanilla Minecraft Advancements screen** as the authoritative layout, textures, visual language, connected-node tree and navigation reference. Avoid a new RPG dashboard, decorative custom art, floating purchase side-panel or unrelated visual theme.
- The **same advancement key** opens the four-card selector described above. 발전 과제 opens vanilla Advancements, 지식 opens its own screen, and 연구 opens its own screen. XP 퀘스트 remains a disabled, visible future placeholder and has no functioning screen. *(Superseded for the XP Quest card by the XP Quest amendment — 2026-10-17 at the end of this document.)*
- Existing 7 disciplines: 자원학 / 생산공학 / 생활기술학 / 전투학 / 생태학 / 세계학 / 연금·마법학. Retain them as navigation/grouping within the vanilla-advancement-like layout; distinguish knowledge and research through **node size, icon and click behavior**, rather than wholesale redesign. The exact screen dimensions are not yet pixel-locked.
- Existing vanilla advancement content and mechanics remain accessible and intact.
- Keep ordinary vanilla-style connected nodes and pan/navigation behavior. The later seven-sector navigation follow-up above supersedes the earlier sentence that rejected a separate radial discipline selector.
- Knowledge nodes **automatically unlock for free** upon their achievement condition. Clicking a knowledge node opens vanilla-style details containing exactly the newly approved explicit information sections:
  1. **해금 조건** — concrete discovery/action requirement.
  2. **해금 보상** — actual unlocked recipe/effect/research access.
  Show achieved/not-achieved state. No knowledge purchase button.
- Research nodes show **선행 조건 + 재료/XP 연구 비용 + 구매 버튼**, using an advancement-like detail presentation; button only enabled when all conditions are met. Server-authoritative validation/deduction, explicit confirmation and per-player synchronization remain required. Clicking a node must not silently purchase it.
- T1/T2/T3 are **classification labels**, NOT a mandatory continuous prerequisite chain. Preserve only real logical dependencies, including permitted T1 -> T3 paths.
- XP quest objectives and rewards remain **DEFERRED**; only the disabled selector placeholder is approved. *(Superseded for the XP Quest card by the XP Quest amendment — 2026-10-17 at the end of this document.)*
- No extra visual effects or animations, custom large configuration panels, or newly invented node families without further approval.

## Approval boundary and current code

- The user approved the **visual mockup overall**, then specifically added the two knowledge detail fields above and confirmed it.
- The illustrative mockup's sample names, sample icons, XP values, example item costs and arbitrary example connections are **NOT authoritative content or economy**. Source actual node names, prerequisites and prices from the V4 research authority document.
- Existing Draft PR #75 contains earlier provisional gameplay/UI work. Apply the attached Auto Farm reference to its existing supported menu, and retain the backend limitations for device screens with no functioning feature implementation.
- Runtime capture and interaction evidence must be reported separately. A render or successful CI job alone does not prove gameplay interaction or visual match.

## XP Quest amendment — 2026-10-17 (user-approved)

This amendment supersedes, for the XP Quest card only, the earlier lines stating that the XP Quest card is a disabled placeholder and that XP quest objectives and rewards are DEFERRED. The approved items below are design authority, not implementation.

### Approved UI

- The XP Quest screen **directly reuses the vanilla Advancements screen** (`AdvancementsScreen`) through client-side synthesis. The server keeps quest progress, completion and claim state in the existing `quest.progress.*`, `quest.complete.*` and `quest.claimed.*` keys of the player progression attachment and syncs them as today. The client builds a separate `ClientAdvancements` instance from the quest catalog and that synced state, then opens `AdvancementsScreen` with it.
- Kept vanilla: tabs, nodes, connector lines, tooltips, scrolling/dragging, frame types and obtained/unobtained display. No custom UI design is added.
- The only interaction added is **clicking a completed, unclaimed node to claim its XP**. Clicking any other node does nothing beyond vanilla behaviour.
- Three node states must be distinguishable:
  1. **Not achieved** — vanilla unobtained display.
  2. **Achieved, unclaimed** — vanilla obtained display plus a claim indicator.
  3. **Claimed** — vanilla obtained display plus a claimed indicator.
  The exact indicator art is an implementation detail and stays LIVE VALIDATION PENDING.
- On completion, the client shows a **vanilla-style toast**. Completing a quest grants no XP. XP is granted **only** when the player clicks to claim.
- Visual node connections are not unlock locks.

### Approved reward and transaction rules

- Claims are server-authoritative and claim-once. The existing `QuestProgressService.claim` order is retained: check claimed → record claimed → grant XP, on the server thread. Duplicate requests return `ALREADY_CLAIMED`. **Grant-failure rollback (approved 2026-10-17):** if the XP grant fails after `claimed` has been recorded, the claim is rolled back in the same server operation — `quest.claimed.<id>` is cleared and the player's level, progress and `totalExperience` are restored to their pre-claim values — and the request returns a failure result. A claim must never end with `claimed` set and the XP not granted, or the XP granted twice. Claim state persists across death, relog and restart through the persistent, copy-on-death attachment.
- **Unclaimed quests may be held as an XP store.** Claiming at any later time is allowed.
- Quest XP grants and all MoreMinecrafts XP deductions use **exact raw-XP calculation**.
  - **Implementation requirement:**
    1. Compute the new total raw XP (current `RawXp.available` ± amount).
    2. Convert it to level + remaining points.
    3. Apply `ServerPlayer.setExperienceLevels(level)` **first**, then `setExperiencePoints(remainingPoints)`.
    4. **Set `player.totalExperience` to the same new total in the same operation.** Neither setter updates `totalExperience` (verified in 26.3 bytecode).
  - The setters reset the server's experience sync marker, so the client receives the updated level, progress and total on the next tick.
  - The float-accumulating `giveExperiencePoints(±n)` path is not used for these transactions.
- Quest rewards are delivered directly, not as experience orbs, so Research XP-acquisition multipliers and Mending do not apply.
- **XP Quest reward scope (decided 2026-10-17): 180,000 raw XP across the 180 active quests (NOW + VERIFY), exactly.**
  - Rounding loss from 50-XP rounding is redistributed across individual quest rewards so the active sum is exactly 180,000. Method: largest remainder in 50-XP units; ties go to catalog order.
  - The 20 FUTURE / SURVIVAL-LOCKED quests add **29,100 raw XP** on top, for a target of **209,100 raw XP across all 200 quests**.
  - When new content or quests are added, existing rewards are, in principle, **not reduced**. New rewards are added on top.
  - FUTURE quests are not registered until their survival acquisition route exists.
  - **VERIFY quests (20) are not registered until they pass verification.**
  - **Registration rule (approved 2026-10-17):** the 160 NOW quests are registered **individually, each only after its own automated test passes**. A NOW quest without a passing test is not registered. VERIFY → NOW moves follow the same rule. Moving a quest between NOW and VERIFY never changes its reward or the 180,000 active ledger.
  - Individual quest content and values in the working catalog remain subject to approval.
- No separate research-only XP currency is introduced.
- No dedicated Creative-mode design or validation is required for XP Quests.

### Owner-locked XP Quest tracking policies (approved 2026-10-17)

These six policies are design authority. They may be reopened only on a concrete technical impossibility.

1. **Ore mining:**
   - Player-placed ores never count toward mining quests.
   - An ore counts only when it is **verified naturally generated**.
   - An ore of unknown origin is **not** assumed natural; it does not count.
   - A mining condition whose naturalness cannot be determined exactly stays VERIFY.
2. **Building:** every successful server-authoritative block placement counts, including repeated placement of the same block. Failed or cancelled placement does not count.
3. **Retroactivity:**
   - Historical progress is fully recognised when supported by reliable persisted evidence.
   - Unrecorded historical events are never invented.
4. **Ocean monument:**
   - All verified participants are credited, not only the final killer.
   - A participant qualifies through valid Elder Guardian damage or verified combat participation inside the monument.
   - Mere presence is insufficient.
   - Credit is given once per player per unique monument.
5. **Enhancement quests:** possession of qualifying equipment is sufficient, regardless of who performed the enhancement.
6. **Research expenditure quests:**
   - These quests evaluate the cumulative approved raw-XP cost of currently owned research.
   - They do not use historical purchase transactions.

## XP Quest UI architecture amendment — 2026-10-24 (owner-approved)

**Current authority for XP Quest screen architecture.** The owner approves retaining the existing custom `ProgressionBrowseScreen` and `ProgressionTreeCanvas` for XP Quest. This amendment supersedes **only** the 2026-10-17 instruction above to directly open vanilla `AdvancementsScreen` with a synthesized `ClientAdvancements` tree. That earlier instruction remains in this document as an audit record. Precedence: **2026-10-24 XP Quest UI architecture > 2026-10-17 vanilla-screen architecture**.

The four-card selector and its vanilla Advancements destination remain. Knowledge and Research retain their seven-discipline navigation and their separately approved interaction rules. Vanilla visual conventions remain preferred where practical; the custom Quest chapter index, semantic zoom and hysteresis, sub-branch layout, tooltips, node rendering and input handling are approved as the Quest presentation architecture. Architectural approval is **not** physical-input verification or owner acceptance of every visual or interaction.

Keep the 200-slot presentation model. The current 75 registered quests are the implementation baseline; this decision registers no additional quest. It approves no new prerequisites, objectives, rewards, XP costs, economy rules or content release. Visual connectors and grouping remain presentation, not gameplay locks.

All other 2026-10-17 Quest rules remain authoritative, including server-owned `quest.progress.*`, `quest.complete.*` and `quest.claimed.*` state; completion without XP; completed-unclaimed claim interaction; claim-once ordering and grant-failure rollback; persistent state; exact raw-XP accounting; registration and reward gates; and the six owner-locked tracking policies. The custom client must continue to enforce those rules through server validation. No code change or live acceptance is implied by this documentation decision.

Historical design and handoff records on the protected #156 branch that prescribe `QuestClientAdvancements` or a vanilla Quest `AdvancementsScreen`—notably `docs/design/xp-quest/D_UI_XP_COST_UNIT_DESIGN.md` and `docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md`—remain source evidence. Their screen-architecture prescriptions are superseded by this amendment; their independent transaction and tracking content is not.

## Research tree visual redesign — pilot scene "Research / Resources" — 2026-11-25 (owner-approved)

**Status:** owner-approved design (2026-11-25, after review of the design captures below). Implementation and live validation are tracked separately; this record approves the visual design only.

**Scope.** Only the Research tree of the Resources (자원학) discipline. The other six Research disciplines, all seven Knowledge trees, XP Quest, the four-card selector and the seven-sector picker keep their current visuals until their own scene designs are approved. Each scene gets its own background sheet; the kit below is the shared Research (violet) kit.

**What this supersedes, for this scene only.** The 2026-10-11 instruction to "preserve vanilla advancement visuals" and the "exact vanilla-frame hit bounds" wording of the 2026-10-12 correction: node frames, connectors, branch headers, the viewport frame and the screen buttons use the approved kit, and a node's hit bounds are the approved node frame as drawn. Everything else in this document stays authoritative: one-click server-authoritative Research purchase, Knowledge non-purchase, localized item-rich tooltips, source discipline/branch IDs, progression data, prerequisites, costs and gameplay rules.

**Approved design evidence.** [Scene](images/research_tree_pilot/research_resources_scene.png), [hover, selection and tooltip](images/research_tree_pilot/research_resources_hover_tooltip.png), [asset kit](images/research_tree_pilot/asset_contact_sheet.png).

**Approved visual kit** (49 transparent PNGs; drawn on the GUI pixel grid and stored at 2 art px per GUI px):

| Element | Files | Rule |
|---|---|---|
| Node frame | `node_locked`, `node_available`, `node_owned`, `node_maxed` (32×32 GUI px) | locked = not purchasable now; available = purchasable now; owned = level ≥ 1 and below maximum; maxed = maximum level. Item icon drawn by code in the centre window; a locked node's icon is dimmed. |
| Hover / selection | `node_hover` (gold halo behind the frame), `node_selected` (white ring) | |
| Connectors | `link_*_off`, `link_*_on` (2 GUI px line) | on (gold) when the prerequisite node is owned, otherwise off (grey). Routes follow the real research prerequisites; they are presentation only and add no gameplay lock. |
| Branch banner | `banner_normal`, `banner_highlight`, `banner_dim` (horizontal nine-slice, 12 GUI px caps) | Branch name drawn by code. |
| Buttons | `button_back`, `button_zoom_in`, `button_zoom_out`, `button_close` × normal / hover / pressed / disabled | Back keeps its existing destination; zoom in/out equal one mouse-wheel step at the viewport centre; close closes the screen. |
| Screen frame | `screen_frame` (nine-slice, 24 GUI px corners) | Gold corner filigree, violet rune band. |
| Background sheet | `sheet_research_resources` (2176×1120 art px = 1088×560 GUI px) | Dark violet stone, low-contrast runes, ore specks, gold zone separators. No nodes, banners or text baked in. |

**Approved layout.** Four horizontal zones from top to bottom: Discipline Foundation (학문 기초), Geology and Quarrying (지질과 채석), Forestry (임업), Field Mobility (현장 기동), each with its banner on the left. Slot centres (sheet px): `resources_root` (1256,120); `v3.research.01` (896,420); `throwable_pickaxe` (1256,316); `v3.research.04` (1256,524); `v3.research.03` (1616,316); `v3.research.02` (576,724); `v3.research.05` (896,724); `climbing` (1616,984). Connector edges: root→01, root→02, root→03, 01→04, 01→throwable_pickaxe, 02→05, 03→climbing. These positions are a presentation overlay for this scene; the node manifest coordinates used elsewhere are unchanged.

**Text.** No text is baked into art. Titles, branch names, the available-XP readout and tooltips are drawn by code in the game font and follow the language setting. The header shows the screen and discipline title (for example "연구 · 자원학") and the player's available XP.

### Knowledge tree visual redesign — same grammar as the Research pilot — 2026-11-25 (owner-approved)

Owner decision (2026-11-25): the Knowledge tree for all seven disciplines uses the same visual grammar as the approved Research / Resources pilot above: the research kit (node frames, banners, buttons, screen frame, hover halo and selection ring), a per-discipline background sheet, horizontal branch lanes with one banner per branch, 64 GUI px nodes, and zoom / pan.

Presentation rules derived from the current Knowledge data (presentation only; no gameplay change):
- Knowledge nodes have no prerequisite edges in the catalog, so no connectors are drawn for Knowledge.
- Knowledge nodes have only locked / unlocked state. Unlocked nodes use the owned frame; locked nodes use the locked frame. The available and maxed frames are not used for Knowledge.
- Knowledge remains non-purchase. Unlock conditions, rewards, tooltips and progression data are unchanged.
- Node positions come from the scene files only; the node manifest coordinates in `node_layout.json` are unchanged.

The connector and state mapping above follow directly from the existing data and the owner's "same grammar" instruction. Any later change to them is a new design decision.

### Discipline selector hub — unified research grammar — 2026-11-25 (owner-approved)

Owner decision (2026-11-25): the seven-sector discipline selector hub used by both Research and Knowledge uses the same visual grammar as the approved research pilot. The vanilla advancement window and stone background are removed from these two hubs.

- Frame: the research screen frame (nine-slice kit texture) around the hub, on the dark research sheet background.
- Header: centred gold title (research or knowledge), with the research back button (kit) at top left.
- Sector plates: violet fills from the research palette. The hovered or focused sector uses a lighter violet fill and a gold outline; the other sectors use a muted outline.
- Sector icons: research kit locked frames instead of vanilla advancement sprites.
- Navigation, sector order, geometry of the seven sectors, and sector titles are unchanged. The Quest selector is not affected.

If an asset is needed beyond the approved research kit, it is created to this grammar and recorded here.

### Enchanting and anvil layout and skin — keep current proportions — 2026-11-26/27 (owner decision)

Owner decision (2026-11-26): the enchanting and anvil screens keep their current layout. Their screen sizes and slot coordinates do not change, including the anvil's vanilla proportions. A wide anvil layout from the reference images is not adopted.

- Enchanting: the screen stays 330×360 GUI px, with the current equipment, book, and tree positions.
- Anvil: the screen keeps the vanilla anvil proportions and the current input, output, catalyst, and inventory slot positions.
- Visual skin follows the approved research grammar. Component art is produced to the current layout, so screen logic and slot mapping are not changed.
- Decorative elements from the reference images (runes, gems, sparkles) are approximated per component unless the owner approves a specific element.

Owner decisions (2026-11-27) for the skin assets:
- Controls are plain (no ornament); decoration is concentrated in the two panel frames and a faint centre overlay.
- Control states are derived from each control's normal state so silhouettes never change. Hover: brighter gold border and a faint inner glow. Pressed: one tone darker with a dark inner top shadow. Disabled: desaturated grey-purple with matte grey-gold border. Selected: bright gold border with a purple inner glow.
- Assets are authored on the native art grid at 2 art px per GUI px. Frame thickness is 8 GUI px so it never overlaps content that starts at 8 GUI px.

Changing any layout dimension or slot coordinate requires a new owner decision.

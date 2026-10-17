
> **HISTORICAL DECISION — SUPERSEDED (2026-10-10):** The three-choice selector correction below is retained as an earlier record. The later approved concept reference and direct clarification specify four visible cards, with XP Quest disabled and marked for later.

# MoreMinecrafts — Approved Advancement / Knowledge / Research UI (2026-10-10)

Status: **USER-APPROVED DESIGN AUTHORITY — UI AND VISUAL REFERENCE**. The attached approved concept reference set and direct implementation instruction supersede conflicting earlier selector/device-screen statements in this file. This task authorizes implementation of the in-scope UI.

## Seven-sector navigation follow-up — 2026-10-11

The later user task and `MoreMinecrafts_Vanilla_UI_Alignment_v1.zip` supersede only the older navigation/grouping decision below that ruled out a separate seven-sector selector. Preserve the existing four-card selector unchanged. Research and Knowledge each enter the same navigation-only seven-sector screen, then open the selected discipline's node canvas; Back returns to the sector screen, and the sector screen returns to the four-card selector. The sector screen has no purchase nodes.

Use the existing source discipline IDs and display names: `resources` / 자원학, `production` / 생산공학, `living` / 생활기술학, `combat` / 전투학, `ecology` / 생태학, `world` / 세계학, and `alchemy` / 연금·마법학. The 7-sector package supplies geometry and hit-area reference only. Render the menu in the Minecraft advancement visual language with the default font, vanilla advancement background/frame sprites, and actual item icons; do not import its preview or selected-sector PNGs as final art.

The selected discipline's tree reuses vanilla advancement box/frame sprites, item rendering, tooltips and connector style. Tree-only mouse-wheel zoom and blank-canvas drag remain separate from sector selection. Existing knowledge discovery/unlock behavior, research costs/prerequisites, server-authoritative purchase checks, detail view and confirm/cancel flow remain unchanged. This follow-up changes navigation and presentation only.

## Superseding selector and device decisions — 2026-10-10

- Pressing the vanilla advancement key opens a selector with four visible cards: **발전 과제**, **지식**, and **연구** are active; **XP 퀘스트** is visibly disabled and marked as coming later. The disabled Quest card opens no screen. The first three cards each open their corresponding separate screen. Vanilla Advancements content remains accessible.
- Use the attached selector concept art for the card composition, borders, colors, Korean labels, disabled Quest treatment, and close button. Exact texture assets may use compatible vanilla resources when the concept illustration files are not available in the repository.
- Chest sorting uses the standalone chest reference and its four controls: sort by type, merge identical items, tidy empty slots, and toggle sort direction. The combined chest/hopper image is not the chest authority.
- Hopper filtering uses only the hopper portion of the combined reference: hopper slots, allow-list selector, filter slots, and NBT-ignore toggle. Matching and transfer behavior remain governed by gameplay authority; the picture does not define additional mechanics.
- Mob Tracker exposes species selection and distance choices 8, 16, 32, 64, and 128. Existing range meaning, loaded-chunk limits, and performance must be validated before changing detection behavior.
- Motorized Boat has a fuel interface and retains its existing throttle behavior. The lower slots in its reference are the player's ordinary inventory; no boat-specific storage is added.
- Brewing Process Control, vanilla brewing mixing, and Potion Concentration/Refining are separate features. Potion Concentration is a distinct device interface, not a brewing-stand tab or a replacement for brewing process control.

## Main experience

- Use the **vanilla Minecraft Advancements screen** as the authoritative layout, textures, visual language, connected-node tree and navigation reference. Avoid a new RPG dashboard, decorative custom art, floating purchase side-panel or unrelated visual theme.
- The **same advancement key** opens the four-card selector described above. 발전 과제 opens vanilla Advancements, 지식 opens its own screen, and 연구 opens its own screen. XP 퀘스트 remains a disabled, visible future placeholder and has no functioning screen.
- Existing 7 disciplines: 자원학 / 생산공학 / 생활기술학 / 전투학 / 생태학 / 세계학 / 연금·마법학. Retain them as navigation/grouping within the vanilla-advancement-like layout; distinguish knowledge and research through **node size, icon and click behavior**, rather than wholesale redesign. The exact screen dimensions are not yet pixel-locked.
- Existing vanilla advancement content and mechanics remain accessible and intact.
- Keep ordinary vanilla-style connected nodes and pan/navigation behavior. The later seven-sector navigation follow-up above supersedes the earlier sentence that rejected a separate radial discipline selector.
- Knowledge nodes **automatically unlock for free** upon their achievement condition. Clicking a knowledge node opens vanilla-style details containing exactly the newly approved explicit information sections:
  1. **해금 조건** — concrete discovery/action requirement.
  2. **해금 보상** — actual unlocked recipe/effect/research access.
  Show achieved/not-achieved state. No knowledge purchase button.
- Research nodes show **선행 조건 + 재료/XP 연구 비용 + 구매 버튼**, using an advancement-like detail presentation; button only enabled when all conditions are met. Server-authoritative validation/deduction, explicit confirmation and per-player synchronization remain required. Clicking a node must not silently purchase it.
- T1/T2/T3 are **classification labels**, NOT a mandatory continuous prerequisite chain. Preserve only real logical dependencies, including permitted T1 -> T3 paths.
- XP quest objectives and rewards remain **DEFERRED**; only the disabled selector placeholder is approved.
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

- Claims are server-authoritative and claim-once. The existing `QuestProgressService.claim` order is retained: check claimed → record claimed → grant XP, on the server thread. Duplicate requests return `ALREADY_CLAIMED`. Claim state persists across death, relog and restart through the persistent, copy-on-death attachment.
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
  - **VERIFY quests (19) are not registered until they pass verification.** Until then the registered set is the 161 NOW quests.
  - Individual quest content and values in the working catalog remain subject to approval.
- No separate research-only XP currency is introduced.
- No dedicated Creative-mode design or validation is required for XP Quests.

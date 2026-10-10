
> **HISTORICAL DECISION — SUPERSEDED (2026-10-10):** The three-choice selector correction below is retained as an earlier record. The later approved concept reference and direct clarification specify four visible cards, with XP Quest disabled and marked for later.

# MoreMinecrafts — Approved Advancement / Knowledge / Research UI (2026-10-10)

Status: **USER-APPROVED DESIGN AUTHORITY — UI AND VISUAL REFERENCE**. The attached approved concept reference set and direct implementation instruction supersede conflicting earlier selector/device-screen statements in this file. This task authorizes implementation of the in-scope UI.

## Superseding selector and device decisions — 2026-10-10

- Pressing the vanilla advancement key opens a selector with four visible cards: **발전 과제**, **지식**, and **연구** are active; **XP 퀘스트** is visibly disabled and marked as coming later. The disabled Quest card opens no screen. The first three cards each open their corresponding separate screen. Vanilla Advancements content remains accessible.
- Use the attached selector concept art for the card composition, borders, colors, Korean labels, disabled Quest treatment, and close button. Exact texture assets may use compatible vanilla resources when the concept illustration files are not available in the repository.
- Chest sorting uses the standalone chest reference and its four controls: sort by type, merge identical items, tidy empty slots, and toggle sort direction. The combined chest/hopper image is not the chest authority.
- Hopper filtering uses only the hopper portion of the combined reference: hopper slots, allow-list selector, filter slots, and NBT-ignore toggle. Matching and transfer behavior remain governed by gameplay authority; the picture does not define additional mechanics.
- Mob Tracker exposes species selection and distance choices 8, 16, 32, 64, and 128. Existing range meaning, loaded-chunk limits, and performance must be validated before changing detection behavior.
- Motorized Boat has a fuel interface and retains its existing throttle behavior. The lower slots in its reference are the player's ordinary inventory; no boat-specific storage is added.
- Brewing Process Control, vanilla brewing mixing, and Potion Concentration/Refining are separate features. Potion Concentration is a distinct device interface, not a brewing-stand tab or a replacement for brewing process control.

## Main experience

- Use the **vanilla Minecraft Advancements screen** as the authoritative layout, textures, visual language, connected-node tree and navigation reference. Avoid a new RPG dashboard, bespoke radial research wheel, floating purchase side-panel or unrelated visual theme.
- The **same advancement key** opens the four-card selector described above. 발전 과제 opens vanilla Advancements, 지식 opens its own screen, and 연구 opens its own screen. XP 퀘스트 remains a disabled, visible future placeholder and has no functioning screen.
- Existing 7 disciplines: 자원학 / 생산공학 / 생활기술학 / 전투학 / 생태학 / 세계학 / 연금·마법학. Retain them as navigation/grouping within the vanilla-advancement-like layout; distinguish knowledge and research through **node size, icon and click behavior**, rather than wholesale redesign. The exact screen dimensions are not yet pixel-locked.
- Existing vanilla advancement content and mechanics remain accessible and intact.
- Keep ordinary vanilla-style connected nodes and pan/navigation behavior; the prior mandatory seven-discipline *radial* layout and separate three-choice landing screen are **superseded** if they prevent the approved vanilla-like unified UI.
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

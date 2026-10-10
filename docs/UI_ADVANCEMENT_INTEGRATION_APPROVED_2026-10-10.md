# MoreMinecrafts — Approved Advancement / Knowledge / Research UI (2026-10-10)

Status: **USER-APPROVED DESIGN AUTHORITY — UI ONLY**. No new implementation authorization is implied by this document. This is the most recent direct instruction and overrides earlier research UI sketches only where conflicting.

## Main experience

- Use the **vanilla Minecraft Advancements screen** as the authoritative layout, textures, visual language, connected-node tree and navigation reference. Avoid a new RPG dashboard, bespoke radial research wheel, floating purchase side-panel or unrelated visual theme.
- The **same advancement key** is the entry point for a unified experience containing **vanilla Advancements (발전 과제), free Knowledge (지식), and paid Research (연구)**. The user specifically approved one integrated entry, rather than a separate hotkey or independent menu. XP quests (XP 퀘스트) will be integrated into this same experience **later**; do not design, implement or expose an XP-quest tab now.
- Existing 7 disciplines: 자원학 / 생산공학 / 생활기술학 / 전투학 / 생태학 / 세계학 / 연금·마법학. Retain them as navigation/grouping within the vanilla-advancement-like layout; distinguish knowledge and research through **node size, icon and click behavior**, rather than wholesale redesign. The exact screen dimensions are not yet pixel-locked.
- Existing vanilla advancement content and mechanics remain accessible and intact.
- Keep ordinary vanilla-style connected nodes and pan/navigation behavior; the prior mandatory seven-discipline *radial* layout and separate three-choice landing screen are **superseded** if they prevent the approved vanilla-like unified UI.
- Knowledge nodes **automatically unlock for free** upon their achievement condition. Clicking a knowledge node opens vanilla-style details containing exactly the newly approved explicit information sections:
  1. **해금 조건** — concrete discovery/action requirement.
  2. **해금 보상** — actual unlocked recipe/effect/research access.
  Show achieved/not-achieved state. No knowledge purchase button.
- Research nodes show **선행 조건 + 재료/XP 연구 비용 + 구매 버튼**, using an advancement-like detail presentation; button only enabled when all conditions are met. Server-authoritative validation/deduction, explicit confirmation and per-player synchronization remain required. Clicking a node must not silently purchase it.
- T1/T2/T3 are **classification labels**, NOT a mandatory continuous prerequisite chain. Preserve only real logical dependencies, including permitted T1 -> T3 paths.
- XP quests are explicitly **DEFERRED**, even though the unified navigation should allow future integration.
- No extra visual effects or animations, custom large configuration panels, or newly invented node families without further approval.

## Approval boundary and current code

- The user approved the **visual mockup overall**, then specifically added the two knowledge detail fields above and confirmed it.
- The illustrative mockup's sample names, sample icons, XP values, example item costs and arbitrary example connections are **NOT authoritative content or economy**. Source actual node names, prerequisites and prices from the V4 research authority document.
- Existing Draft PR #75 contains earlier provisional gameplay/UI work, including an unapproved AutoFarmScreen. Passing CI does **not** mean those visuals are approved. Do not finalize, merge, or expand UI implementation before its corresponding image design is accepted.
- No GitHub PR merge, client-side screenshot validation, or implementation completion is claimed here.

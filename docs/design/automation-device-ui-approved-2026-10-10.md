# Automation Device UI & Behavior — Approved Design (2026-10-10)

Status: **DESIGN APPROVED / NOT IMPLEMENTED**. This document is normative. Generated concept images, mockups, and provisional PNGs are **REFERENCE ONLY**, never authoritative for slot count, mechanics, texture fidelity, or implementation readiness. Where images conflict with this document, **this document wins**. Do not claim asset integration or testing from images.

## 1. Automatic Farm
- Collection footprint: horizontal **9×9 centered on device**. Collect eligible dropped crop items located **above farmland** within the footprint. This is a collection footprint, not a guarantee of 81 farmland blocks. A central water source hydrates farmland up to four blocks horizontally; center-water layout permits at most 80 farmland positions in 9×9. Device placement/center-water coexistence must be validated before implementation; do not silently redefine either.
- Storage: **25 seed slots (5×5)** and **25 harvest slots (5×5)**, distinct inventories. These are storage sizes, **not** farming dimensions.
- Route seed-only items to seed storage; harvest-only items to harvest storage.
- Dual-role crops (e.g. carrot, potato) are routed **seed storage first**, then **harvest storage** if seed storage is full or unavailable. Absence of seed storage capacity must not prevent use of harvest storage.
- A hopper placed **below the device** may extract **only from harvest storage**. Seed storage is inaccessible to that hopper, including for dual-role crops stored there.
- Do not invent a processing/replanting cycle or UI controls beyond approved design.
- Pending implementation details: handling of items when both destinations are full (must avoid silent loss), exact vertical collection bounds, water/device placement geometry, and crop classification edge cases.

## 2. Automatic Logging Device
- Exactly two primary slots: **axe/tool** and **fuel**.
- Logs harvested by the machine **drop at the device's location**. No machine output inventory is required.
- Display **the vanilla furnace flame-shaped fuel burn indicator**, below the fuel slot, using the existing vanilla texture/behavior, not a custom fuel bar, extra progress bar, or redesigned flame.
- Do not add unapproved tree filters, area sliders, storage or start/stop controls merely because a mockup depicts them.
- Pending implementation details: fuel consumption rate, supported axes and durability rules, harvesting target/range; use existing authoritative design if available.

## 3. Brewing Process Control
- **Preserve the vanilla brewing stand UI and its native ingredient, fuel, three-bottle slots and indicators**. Do not draw a replacement brewing stand interface.
- Build only the **left-side ingredient inventory extension** and **right-side potion inventory extension**, scaled/aligned to flank the vanilla UI.
- Left/right extension storage capacities and exact pixel dimensions are **design discretion / provisional**, not previously approved gameplay constraints; ensure the player inventory and vanilla brewing functionality remain usable.
- Avoid embedding item icons, counts, tooltips or dynamic states into static UI textures.
- Pending implementation details: transfer/automation behavior and precise slot coordinates after integration.

## UI Asset Rules
- Produce **separate, package-ready GUI textures per screen/extension**, not a collage, promotional concept art or world-scene render.
- Use vanilla-aligned slot spacing, pixel-perfect dimensions, and existing vanilla elements where required; game code renders actual items, counts, progress, tooltips, and state.
- UI screenshots and AI-generated images are **visual references only**. Never infer or override mechanics from them.
- Actual package compatibility requires texture path/atlas mapping, Screen/ScreenHandler slot alignment and runtime validation; **none is claimed complete by this document**.

## 8. Chest Sorting UI — APPROVED (2026-10-10)
- **Chest sorting, NOT chest filtering.** Do not introduce filter slots, whitelist/blacklist, NBT-ignore settings or rename the chest UI to "상자 정렬".
- Keep the existing vanilla chest title (e.g. **상자**) and its **original inventory slot count, grid geometry, slot spacing, proportions and player inventory**. Do not shrink or replace the vanilla grid to fit controls. Single/double chest sizes must follow their actual vanilla layouts; reference artwork is not a slot-count specification.
- Add a **sorting-only side panel** on the left or right (approved visual reference places it on the right), adjacent to—not overlapping—the vanilla chest inventory.
- Approved side-panel actions: **sort by item type**, **merge compatible stacks of identical items**, **compact empty gaps**, **toggle sorting direction**. Each is a separate control, not an item-filtering rule.
- Preserve item counts, item identity, components/data, stack-size limits, and inventory ownership; never combine incompatible stacks or move items into the player's inventory. Sorting affects the opened chest's slots only.
- Generated images are **visual references only** and are not package-ready resources or binding implementation requirements. Pixel positions, sorting comparison/tie-break rules, and button behaviors must be checked during implementation; no implementation or runtime verification is claimed.
- This section updates **only item 8 (chest)**. It does not modify crafting-table nearby-ingredient rules (item 7), hopper filtering rules (item 9), or automation device rules (items 1–3).

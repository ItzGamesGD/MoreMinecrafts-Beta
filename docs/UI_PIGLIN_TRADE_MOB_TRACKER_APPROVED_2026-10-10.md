# MoreMinecrafts — Piglin Trading and Mob Tracker UI: user decisions (2026-10-10)

**Status: design authority / implementation requirements only.** No feature implementation, UI asset import, research price approval, or PR merge authorized. The images produced in the chat are **visual references ONLY**. In any disagreement, **the explicit behavior in this document prevails over pixels, labels, icons, example numbers, and fabricated mockup mechanics**. Do not copy fake example recipes or costs from mockups.

## 0. Context / supersession
- User reviewed UI image candidates #4 Brewing Process Control, #5 Piglin Trading, #6 Mob Tracker.
- **#4 Brewing Process Control UI candidate is PASSED/REMOVED from this image-design batch** because it overlaps with the previously approved #3 brewing UI. **Do not implement a second duplicate UI**. Reconcile brewing features into the existing vanilla-brewing-stand extension; this decision alone does not delete already-approved brewing gameplay behavior. No additional brewing UI image approval is implied.
- #5 and #6 below supersede prior generated image arrangements and any conflicting provisional design notes.
- Other UI/visual work is happening in a separate conversation. **Keep this documentation isolated from ongoing PR #75 and other branch work; no cherry-pick, merge or cross-branch overwrite without checking current remote refs and conflicts.**

## 1. #5 Piglin Trading — exactly vanilla villager trading UX
1. The piglin trading **screen, controls, slot placement, list selection, scroll, input/output behavior, stock limits, trade availability and transaction timing must match the actual vanilla villager merchant trading screen** as closely as possible. Reuse the vanilla merchant screen/menu architecture where practical, not a custom 'trade dashboard'.
2. **NO '거래하기' / 'Trade' button**. The player selects an offer from the left-side vanilla list, places or auto-populates the required input items in the standard input slot(s), and **takes the output item from the result slot**. That output take is the **transaction**: consume the appropriate inputs atomically, update usage/stock and award output exactly once. Simply selecting an offer, hovering, or previewing must never consume payment.
3. Respect shift-click and repeated transactions exactly as merchant inventory semantics allow; validate server-side to prevent duplication or free purchases. Reject trades if inputs insufficient or offer unavailable; do not fabricate special purchase buttons.
4. Existing vanilla random piglin bartering remains distinct. The research unlock grants the approved structured trade interaction; do not replace unrelated bartering behavior without separate approval.
5. **Offers, gold prices, stock amounts, trade items, experience effects and unlock prerequisites shown in generated mockups are placeholders, NOT APPROVED BALANCE.** Derive actual offers from separately approved content/economy data. No invented extra currency or new workstation.
6. Visual rule: use vanilla villager trading UI appearance. The concept image is only a reference to subject matter, **not an authorization to implement its custom wide gray panel or the depicted examples**.

## 2. #6 Mob Tracker — resource-fed, range-scaled, client-visible glowing
1. The item/device has **NO durability system**. Detection is paid for by **consuming actual items placed in dedicated material input slot(s)** in its screen. Do not show a read-only 'cost icons' panel in lieu of usable slots.
2. UI contains only the necessary interaction elements:
   - **Mob species/type selection** (allow player to select target mob type; precise list and any multi-select semantics must follow separately approved rules).
   - **Detection range slider** (range in blocks, current value visible).
   - **Actual material input slot(s)** into which the player puts the required detection material(s). Clearly distinguish occupied/input and insufficient states. Exact materials and number of slots not yet locked.
   - **Calculated cost feedback** proportional to selected range (and any other separately approved variables), as small text/tooltip adjacent to the input slot(s); do not turn it into a separate non-interactive cost-display inventory.
   - **Apply / detect action** only when required items are present. Consumes the correct quantity **once upon a successful server-authoritative activation**, never on slider movement, preview or simply opening the screen.
   - Vanilla player inventory may be shown for transfer of materials.
3. **REMOVE ENTIRELY from the device UI**: display options / toggles, map, minimap, radar, directional markers, HUD distance labels, tracked mob result list, on-screen mob icons or any UI-rendered detection visualization. The detection results are **NOT drawn in the GUI**.
4. When the player selects the species, adjusts radius, provides payment and activates, **matching entities within the actual world detection range appear with the vanilla-style glowing outline on that activating player's game client only**, visible in normal gameplay/world view (including vanilla glowing through blocks where supported). No world-space floating icons, minimap or special HUD. Other players must **not** receive the outline merely because the initiating player ran a scan.
5. Implementation must achieve player-specific highlighting without globally toggling a shared entity glowing flag that broadcasts to everyone. Use a safe per-viewer client rendering / entity metadata approach with server validation, species matching, loaded-entity limits and cleanup. Preserve existing entity data and preexisting unrelated glow effects. Validate multiplayer isolation with at least two clients.
6. The **radius slider increases material cost** as radius grows. **Exact min/max radius, slider steps, ingredient type, price curve, effect duration, refresh semantics and maximum simultaneous targets remain UNDECIDED**; generated images showing 8/16/32/64/96/128 blocks or Ender Pearls/Redstone/Amethyst are **illustrative only**, not accepted game rules. Do not hardcode them without explicit later approval.
7. The user specifically corrected the last mockup: **only actual material-application slots are needed in the '소모 재료' area**. A read-only three-icon cost showcase is not the desired interaction. If multiple materials eventually become necessary, derive number of input slots from the actual approved economy, not the image.
8. Server-authoritative transaction: verify selected species/radius, materials, available inventory and permitted entities, then atomically consume payment and issue a **per-player** visual scan effect. On failure do not consume materials. Define cancellation/refund behavior before coding, avoid duplicate scans on button spam, and never scan unloaded chunks by force.
9. For the first gameplay test, verify (a) no durability decrement, (b) increasing radius costs more, (c) no consumption on slider changes, (d) insufficient material blocks activation, (e) matching world mobs glow for activating player, (f) other player's view unchanged, (g) no map/radar/HUD/options, (h) effects clear correctly without damaging existing glowing effects.

## 3. Image acceptance / implementation guardrails
- Concept images are **not authoritative blueprints**. Their sample trade prices, made-up material costs, custom map, preview card, or control labels are NOT binding.
- User specifically rejected the mockup's **trade button** and **mob radar/map/display options**. Their absence is a **hard acceptance criterion**.
- The last concept image still contained a read-only material-cost showcase; replace it with **real item input slots**. No need to generate a new image unless requested.
- #4 brewing duplication should be marked **not a separate new UI**, pending reconciliation with previously accepted brewing UI.
- Preserve existing Draft PR #75 and other active branches; no code changes or PR merge. GitHub documentation commit is a record of requirements only.

## 4. Provenance
- Confirmed through direct user corrections in this conversation on 2026-10-10. The concept art produced in chat may be consulted visually, but the art file is **not committed** by this text-only documentation operation.

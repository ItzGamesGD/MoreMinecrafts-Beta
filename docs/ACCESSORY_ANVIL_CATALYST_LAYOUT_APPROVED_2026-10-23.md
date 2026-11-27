# Accessory anvil catalyst slot layout — approved 2026-10-23

**Status:** owner-approved layout, conditional on real-client acceptance (recorded below). Final catalyst artwork, recipe and acquisition remain **deferred** (`ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md`).

## Decision

Under the owner's functionality-first acceptance policy, the accessory Relic Catalyst slot sits in the anvil's **Enhance-mode input row, between the Enhancement Stone slot and the Enhance button** (candidate `INLINE_RIGHT`):

| Element | Position (panel-relative GUI units) |
|---|---|
| Catalyst slot | x=102, y=47 (18px cell drawn behind it) |
| Enhance button | x=122, y=42, 49×20 |
| Cost lines | `Raw XP / Stone` on one line, `Catalyst: N` on its own line only when N > 0 |

Constants live in `AnvilCatalystUiLayout`. Vanilla anvil slots (27,47 / 76,47 / 134,47), the Anvil and Combine Books tabs, and the vanilla panel size are unchanged.

## Why not the shipped placement

The enhancement PR placed the slot at x=180, outside the 176px panel. In the real client it was **not clickable at GUI scales 2, 3 or 4** (vanilla treats a click right of the panel as a click outside the window), its label overflowed the panel, and it was also shown for ordinary equipment. The comparison of all three candidates is preserved in `validation/anvil-catalyst-layout/anvil-catalyst-layout-comparison.png`, with assertions in `comparison-assertions-run-39906586280.txt` (Client GameTest run 129). `LEFT_OF_INPUT` (slot left of the target slot) also passed geometry and click checks and remains a fallback.

## Acceptance evidence (real Minecraft client, Fabric Client GameTest mouse input, GUI scales 2, 3, 4)

Run 130 (`f6fb8a49`), `validation/anvil-catalyst-layout/inline-right-functional-assertions-run-39907483683.txt`:

- Catalyst slot inside the panel, no overlaps, genuinely clickable (picked up and returned).
- Pressing Enhance for +4 → +5 consumes exactly 40 raw XP, 2 Enhancement Stones, 1 Relic Catalyst and raises the level to 5.
- Insufficient resources and +10 maximum: button inactive, click changes nothing.
- Ordinary equipment still enhances with its own stone cost and never consumes the catalyst.
- Vanilla Anvil tab: slots unchanged, catalyst slot rejects items, Enhance hidden, a damaged sword is repaired and taken from the real result slot.

## Not decided / VISUAL PENDING

- Final Relic Catalyst icon (a clearly marked test-only fixture exists under `src/gametest/resources`).
- Cosmetic refinement of the Enhance button/slot spacing and hiding the unused catalyst slot for ordinary equipment — tracked in `validation/ACCESSORY_INTEGRATION_FOLLOWUPS_2026-10-23.md`.
- GUI scale 1 compatibility — tracked separately, same file.

Gameplay, costs, IDs and persistence are unchanged.

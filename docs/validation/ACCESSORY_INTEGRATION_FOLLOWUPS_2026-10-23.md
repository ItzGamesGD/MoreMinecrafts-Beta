# Accessory integration follow-ups — 2026-10-23

Owner policy: functionality before visual polish. These items are **not** part of PR #161 and are not a GUI redesign.

## 1. Enchanting UI at GUI scale 1 (investigation, not fixed)

Screen: `EnchantingProgressionScreen` (mod enchanting table), fixed 330×360 GUI-unit panel. Real-client measurements, 1920×1080 window, Client GameTest run 39907483683 (`validation/anvil-catalyst-layout/enchanting-gui-scale-diagnostic-run-39907483683.txt`, screenshots `enchant-scale-*` in the run's artifacts):

| GUI scale | GUI size | Panel fits? | Panel physical size | Smallest widget |
|---|---|---|---|---|
| 1 | 1920×1080 | yes | 330×360 px | 18 px |
| 2 | 960×540 | yes | 660×720 px | 36 px |
| 3 | 640×360 | yes (exactly full height) | 990×1080 px | 54 px |
| 4 | 480×270 | **no** — top −45; tabs and the inventory/equipment slots off-screen | — | — |

Findings:

- **Scale 1:** nothing is off-screen or overlapping, but the whole panel is only 330×360 physical pixels (about 17% of the width and 33% of the height of a 1080p window) with 18 px node buttons and ~9 px text. Tiny hit targets and unreadable item/tooltip text are the likely usability problem. The original report's exact symptom was not available, so this is an inference — **please attach the report** (resolution, window size, what was hard to use).
- **Scale 4 (and above, or any window shorter than 360 GUI units):** a confirmed, separate defect — the fixed 360-unit height overflows the screen and cuts off the tabs and the player inventory.
- Likely fix direction (not implemented, needs a design decision): derive the panel size from the available GUI size (or scale the content) instead of a fixed 330×360.

## 2. Remaining cosmetic items (VISUAL PENDING)

- Final Relic Catalyst artwork (test-only fixture in use).
- The catalyst slot is shown in Enhance mode even for ordinary equipment, where it is unused.
- Enhance button is narrowed to 49 px to make room for the catalyst slot; spacing against the slot is tight.
- Vanilla anvil "Enchantment Cost" label sits close to the "Inventory" label in Anvil mode (vanilla layout, unchanged).

## 3. Other follow-ups

- Real-client GUI scale 1 pass for the accessory inventory, recipe book and anvil (not run; scales 2/3/4 are covered).
- Final catalyst recipe/acquisition and the 12 accessory abilities remain separate design/implementation phases.

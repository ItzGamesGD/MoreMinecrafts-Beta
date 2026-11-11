# Accessory client repair — 2026-10-22

## Baseline and evidence levels

This Draft PR is stacked on #145 at `d7e8effd4052313e9965ff417001e5c2ad489c06`. The parent Client GameTest rendered the actual Minecraft inventory and passed cursor equip/unequip and invalid-category rejection, but failed TestInput Shift-click equip. The server GameTests on the parent exercise `InventoryMenu.quickMoveStack` in both directions, replay rejection, full inventory, and item conservation. Those server tests do not establish the client input action.

Fabric API `0.161.0+26.3`, tag `84831249df47bd923b48186d169bc0d8501b547b`, implements `TestInput.holdShift()` by holding `InputConstants.KEY_LSHIFT`. Its `TestInputImpl.pressOrReleaseKey` constructs mouse input as `new MouseButtonInfo(key.getValue(), 0)`. The client trace in [run 39701501629](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39701501629) recorded `Minecraft.hasShiftDown()=true` before and after the click, yet the Ring of Life moved to the cursor and the defense slot remained empty. This supports an input-simulation modifier defect rather than a server quick-move defect. The key mapping's `isDown()` was false, illustrating why the held key alone was an insufficient dispatch check.

The follow-up fixture retains that negative TestInput probe, returns the picked-up ring to its inventory slot, and sends a Shift-modified `MouseButtonEvent` through `InventoryScreen.mouseClicked`. This path still uses the actual client screen and server menu. It is classified as real-client simulated screen input, not external OS pointer input. The initial screen-event helper incorrectly used button `0`; Minecraft 26.3 uses SDL left button `1`. See the recovered and resumed results below.

## Recovery and controlled input experiment

Recovered Draft PR: [#146](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/146), branch `codex/accessory-client-repair-20261022`, recovered HEAD `41a727cd7dd97516c9b2c2be795c613de67eda5d`. All six commits survived: `82876f8`, `737440a`, `25e5bdd`, `b28485b`, `c91471d`, and `41a727c`. Their changes are limited to this document, the client fixture, and its workflow. No runtime repair survived because none had been made.

Importantly, `41a727c` already restored the source slot and held Shift during the modified screen event. [Run 39702194055 (#55)](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39702194055) compiled successfully, then logged `handled=true shift=true menuSlot=11 cursor=0 minecraft:air` before timing out. This did not establish QUICK_MOVE: the helper sent mouse button 0.

The resumed one-variable experiment `128e2fa` changed only that button to `SDL_MOUSE_BUTTON_LEFT` (1), retaining Shift and source restoration. [Run 39715163595 (#57)](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39715163595) reached and passed both equip and unequip assertions, including server conservation. It then failed fixture teardown: `finished while a server is still running`. This isolates the modified helper's failure from the production quick-move implementation.

The next fixture commit `72d0538` closes the connection/server with try-with-resources, uses Fabric's packet barriers around operations, initializes unequip from a separate authoritative state, and adds a test-only `AbstractContainerMenu.clicked` observer. Its `ACCESSORY_MENU_INPUT` lines identify client/server side, slot, button, and dispatched action, distinguishing the zero-modifier PICKUP probe from modified QUICK_MOVE. Acceptance of that final fixture requires its CI result; a queued run is not a pass.

Recovered Fabric CI #1044 failed the unrelated K5 assertion `modded-feature target begins unmarked on tick 0`; accessory persistence/restart and mapped-source runs on the recovered HEAD succeeded. Fabric CI #1050's build passed on `128e2fa`. Existing server persistence and lifecycle tests are unchanged.

Remaining client acceptance: reconnect, dimension transfer, normal and keepInventory death/respawn synchronization, rapid-click ghost-state checks, and backpack/armor/offhand/crafting interaction regressions. Server tests and screenshots are not substitutes for those client checks. External OS pointer input remains unvalidated.

## Accessory resource audit

Each row was checked under `src/main/resources/assets/moreminecrafts/`. The item definition path is `items/<id>.json`; the model path is `models/item/<id>.json`; the texture path is `textures/item/<id>.png`. No matching accessory artwork was found elsewhere in the assets tree. Item IDs are unchanged.

| Item ID | Item definition | Model | Texture |
|---|---|---|---|
| `snow_charm` | missing | missing | missing |
| `deep_sea_pearl` | missing | missing | missing |
| `ember_ring` | missing | missing | missing |
| `ring_of_life` | missing | missing | missing |
| `whisper_of_the_sea` | missing | missing | missing |
| `star_covenant` | missing | missing | missing |
| `phantom_mirror` | missing | missing | missing |
| `ring_of_time` | missing | missing | missing |
| `blood_covenant` | missing | missing | missing |
| `seed_of_life` | missing | missing | missing |
| `heart_of_gravity` | missing | missing | missing |
| `soul_chain` | missing | missing | missing |

**VISUAL PENDING:** All 12 require authoritative artwork, an item definition, and a matching model/texture reference. The parent real-client screenshots show missing-texture checkerboards. No placeholder art was introduced.

# Accessory client repair — 2026-10-22

## Baseline and evidence levels

This Draft PR is stacked on #145 at `d7e8effd4052313e9965ff417001e5c2ad489c06`. The parent Client GameTest rendered the actual Minecraft inventory and passed cursor equip/unequip and invalid-category rejection, but failed TestInput Shift-click equip. The server GameTests on the parent exercise `InventoryMenu.quickMoveStack` in both directions, replay rejection, full inventory, and item conservation. Those server tests do not establish the client input action.

Fabric API `0.161.0+26.3`, tag `84831249df47bd923b48186d169bc0d8501b547b`, implements `TestInput.holdShift()` by holding `InputConstants.KEY_LSHIFT`. Its `TestInputImpl.pressOrReleaseKey` constructs mouse input as `new MouseButtonInfo(key.getValue(), 0)`. The client trace in [run 39701501629](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39701501629) recorded `Minecraft.hasShiftDown()=true` before and after the click, yet the Ring of Life moved to the cursor and the defense slot remained empty. This supports an input-simulation modifier defect rather than a server quick-move defect. The key mapping's `isDown()` was false, illustrating why the held key alone was an insufficient dispatch check.

The follow-up fixture retains that negative TestInput probe, returns the picked-up ring to its inventory slot, and sends a Shift-modified `MouseButtonEvent` through `InventoryScreen.mouseClicked`. This path still uses the actual client screen and server menu. It is classified as real-client simulated screen input, not external OS pointer input. Its CI result is pending.

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

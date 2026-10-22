# Accessory client repair — 2026-10-22

## Baseline and evidence levels

This Draft PR is stacked on #145 at `d7e8effd4052313e9965ff417001e5c2ad489c06`. The parent Client GameTest rendered the actual Minecraft inventory and passed cursor equip/unequip and invalid-category rejection, but failed TestInput Shift-click equip. The server GameTests on the parent exercise `InventoryMenu.quickMoveStack` in both directions, replay rejection, full inventory, and item conservation. Those server tests do not establish the client input action.

Fabric API `0.161.0+26.3`, tag `84831249df47bd923b48186d169bc0d8501b547b`, implements `TestInput.holdShift()` by holding `InputConstants.KEY_LSHIFT`. Its `TestInputImpl.pressOrReleaseKey` constructs mouse input as `new MouseButtonInfo(key.getValue(), 0)`. The zero modifier value is a specific input-simulation hypothesis. The added client trace records both `Screen.hasShiftDown()` and the Shift key mapping before and after the failing click. The CI result is needed before attributing the failure to this layer.

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

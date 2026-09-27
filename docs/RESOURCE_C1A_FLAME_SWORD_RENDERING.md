# Resource C1-A: Flame Sword rendering foundation

- Base: `stage-13/checkpoint` at `a8b6299f16a491aa3dc885360325427a29190763`
- Implementation branch: `resource-c1/flame-sword-render-foundation`
- Status: resource foundation; approved Flame Sword artwork and live visual validation are pending.

## Repository audit

The Java package is `dev.itzgamesgd.moreminecrafts`. `ModItems` registers `moreminecrafts:flame_sword` once with `Registry.register`, a `ResourceKey<Item>`, `FlameSwordItem extends Item`, and `Item.Properties().sword(ToolMaterial.NETHERITE, 4.0F, -2.6F).fireResistant().setId(...)`. These existing combat values are inherited from Stage 5A and are not approved or changed by C1-A. `ModRegistries` initializes the items and binds an existing active ability; `FlameSwordItem` already invokes an existing splash effect. C1-A does not add, revise, or approve those gameplay prototypes. The existing Wind Sword also uses `Item.Properties().sword(...)` and the same item/model structure.

The client initializer has HUD, screen, tooltip, network, and key handling, but no sword item renderer or sword-specific transform registration. Existing JUnit tests cover pure logic; `validateStage6Data` parses source JSON and checks mod-local item-model targets. CI runs `./gradlew build`, then server and client startup smoke tests. The Flame Sword item and model files originated in the Stage 5A vertical slice; no approved Flame Sword art exists in this checkpoint.

## Sword family resource contract

The item ID `moreminecrafts:flame_sword` resolves through `assets/moreminecrafts/items/flame_sword.json` (`minecraft:model`) to `assets/moreminecrafts/models/item/flame_sword.json`. That model inherits `minecraft:item/handheld` and currently selects `minecraft:item/netherite_sword` for `layer0`. The parent and texture are supplied by Minecraft. No custom renderer, separate first-person logic, display overrides, or special hand transforms are installed.

This is **TEMPORARY / NOT APPROVED artwork**: the vanilla Netherite Sword appearance is a development reference only. No Flame Sword texture or final model art was created. Later approved artwork can replace the model's texture reference (and approved model geometry if required) while retaining the ordinary client item definition and vanilla sword presentation. A build proves resource linkage, not the on-screen pose.

For later Flowing Water, Wind-Cutting, and Lightning swords, use the same `Registry.register` + `Item.Properties().sword(...)` item path and `assets/moreminecrafts/items/<id>.json` → `models/item/<id>.json` with the vanilla handheld parent. Each needs its own approved visual resources and separately specified gameplay ability. The existing Wind Sword is already an example of this path; this document neither registers nor changes any other sword. Do not create an elemental sword renderer. Bows, maces, and staffs require separate resource decisions.

## Static validation

`validateFlameSwordResources` runs from Gradle `check`: it verifies one Flame Sword key and registration declaration; parses source client-item, model, and English/Korean locale JSON; checks the model reference, handheld parent, and temporary vanilla texture identifier; and checks that each resource appears exactly once with matching JSON in the distributable `jar` output. The existing `validateStage6Data` and JUnit suite remain in `./gradlew build`. The vanilla texture is deliberately external to the mod JAR and is provided by Minecraft.

This task changes only the Flame Sword creative tab listing and Korean name, the scoped Gradle validation, and this document. It does not alter existing Fishing, Enhancement, Mining, other gameplay assets, or Flame Sword ability behavior.

## LIVE PENDING

- [ ] Inventory / GUI appearance
- [ ] Dropped-item appearance
- [ ] First-person right-hand appearance
- [ ] First-person left-hand / off-hand appearance
- [ ] Third-person appearance
- [ ] Attack animation and orientation
- [ ] Scale
- [ ] Rotation
- [ ] Hand placement
- [ ] Item-frame / display appearance
- [ ] Absence of generic flat or paper-like held presentation

A real Minecraft 26.3 client session must record observations for every item above before visual appearance can be marked verified. C1-A does not complete Resource C1.

# Resource C1-B: approved Flame Sword production texture

- Base: `stage-13/checkpoint` at `a8b6299f16a491aa3dc885360325427a29190763`
- Branch: `resource-c1b/flame-sword-production-texture`
- Source: attached `flame_sword_production_asset(1).zip`, entry `flame_sword.png`
- Source and repository SHA-256: `67292219456c8975319c6d45caf5c9ac1f4b583176407d24eedcd7f643cad186`
- Status: static resource integration with bounded item geometry; live presentation remains pending.

## Baseline and C1-A synchronization

C1-A PR #37 was merged into `stage-13/checkpoint` at `925f136798e6b680748b69feb02fae65f88bece2`. This C1-B branch began at the prior checkpoint `a8b6299f16a491aa3dc885360325427a29190763`; it is being synchronized with the merged foundation through a two-parent merge commit. The production model and bounded two-face geometry from C1-B are retained. The merged C1-A registration, Korean translation, documentation, and foundation validator are included. The C1-A validator allows either its temporary vanilla reference or the approved production texture; the C1-B validator checks the production texture and bounded model contract. Both are expected to pass in the cumulative build.
## Compatibility and geometry audit

The approved PNG is an unchanged, static 1263 × 1246 8-bit RGBA file with transparent, partial-alpha, and opaque pixels. It has no animation metadata. Its width and height are unequal and not powers of two. The source remains byte-for-byte intact in the repository and distributable JAR.

An automated review identified a performance risk in the first C1-B revision: allowing the high-resolution layer through generated item extrusion produced per-pixel edge geometry, reported at roughly 20,450 total quads for this sprite. That is inappropriate as the reference path for multiple elemental weapons.

The corrected model keeps `minecraft:item/handheld` as the parent so the normal handheld display transforms remain inherited, but supplies its own explicit element. In the model format, child `elements` override inherited geometry. C1-B therefore uses one thin 16 × 16 × 1 element with only north and south faces, both mapped to the complete approved `layer0` texture. Geometry is statically bounded at two declared faces instead of following every alpha edge in the 1263 × 1246 sprite.

This deliberately does **not** add custom transforms, a custom renderer, VFX, animation, or a derived/resized texture. The approved PNG is unchanged.

The two-face model is a performance-safe static reference. Whether the lack of generated edge thickness is visually preferable in first/third person is a LIVE PENDING question; visual tuning must not silently reintroduce unbounded per-pixel extrusion.

The CI client smoke uses Mesa software rendering and requires graphics-backend selection, resource reload, and item-atlas creation markers while rejecting common missing-model/texture, decode, stitch, and bake failures.

## Resource graph and reuse

`ModItems.FLAME_SWORD`
→ `moreminecrafts:flame_sword`
→ `assets/moreminecrafts/items/flame_sword.json` (`minecraft:model`)
→ `assets/moreminecrafts/models/item/flame_sword.json`
→ parent `minecraft:item/handheld`
→ one explicit bounded two-face element
→ `layer0: moreminecrafts:item/flame_sword`
→ `assets/moreminecrafts/textures/item/flame_sword.png`.

Minecraft's normal item display contexts continue to select the same item model for GUI, held first/third person, ground, and fixed/item frame. There are no separate context textures, custom renderer, special hand transforms, VFX, or animation.

If the bounded model passes live presentation review, this route becomes the reference for later Water, Wind, and Lightning sword resources. Those branches should reuse the same bounded geometry contract with unique approved static textures instead of returning to generated high-resolution extrusion.

## Non-live validation

`./gradlew build` runs the existing JUnit and `validateStage6Data` checks plus `validateFlameSwordProductionAsset`.

The Flame Sword validator checks:

- the pinned source SHA-256,
- PNG signature and IHDR,
- exact 1263 × 1246 dimensions,
- 8-bit RGBA and alpha distribution,
- client item → model → texture linkage,
- `minecraft:item/handheld` parent,
- exactly one explicit bounded element,
- exactly two declared faces (`north` and `south`),
- full-texture UVs on both faces,
- single item registration,
- exact item/model/PNG contents in the built JAR.

CI also runs server and software-rendered client smoke tests and checks for resource/model/atlas failures.

## LIVE PENDING

- [ ] Inventory / GUI appearance
- [ ] First-person right hand
- [ ] First-person left hand / off-hand
- [ ] Third-person appearance
- [ ] Dropped item
- [ ] Item frame / fixed display
- [ ] Attack animation and orientation
- [ ] Scale
- [ ] Rotation
- [ ] Hand placement
- [ ] Pixel density and filtering
- [ ] Visual effect of unusual source dimensions
- [ ] Whether the bounded two-face geometry appears acceptably weapon-like rather than overly flat
- [ ] Absence of unwanted generic/paper-like held presentation

Non-live success does not complete C1 or establish a live rendering pass.

# Resource C1-B: approved Flame Sword production texture

- Base: `stage-13/checkpoint` at `a8b6299f16a491aa3dc885360325427a29190763`
- Branch: `resource-c1b/flame-sword-production-texture`
- Source: attached `flame_sword_production_asset(1).zip`, entry `flame_sword.png`
- Source and repository SHA-256: `67292219456c8975319c6d45caf5c9ac1f4b583176407d24eedcd7f643cad186`
- Status: static resource integration; live presentation remains pending.

## Baseline and C1-A dependency

C1-A PR #37 (`resource-c1/flame-sword-render-foundation`, handoff head `7828eb268eb6a02f3c3bd5f36dcd747583a3e548`) remains separate and is **not** in this checkpoint. C1-B starts directly from the verified checkpoint without cherry-picking or rebuilding C1-A's creative tab, Korean translation, documentation, or Gradle validator. The checkpoint already has the needed registry and vanilla handheld model chain. C1-A's validator still expects the temporary `minecraft:item/netherite_sword` texture; reconcile that assertion to `moreminecrafts:item/flame_sword` when integrating the two PRs, or the combined branch's `check` will fail. Keep both PRs unmerged until their order and validation are resolved.

## Compatibility audit

The approved PNG is an unchanged, static 1263 × 1246 8-bit RGBA file with transparent, partial-alpha, and opaque pixels. It has no animation metadata. Its width and height are unequal and not powers of two. The checkpoint's client item resolves a single ordinary model, which inherits `minecraft:item/handheld`; the model has no explicit UV or display transforms. The full sprite is selected as layer0, and transparent padding remains part of the sprite. This file is about 1.5 million pixels (about 6.3 MB of decoded RGBA data before atlas overhead). There is no source-level requirement here to crop, rescale, rotate, or re-encode it. The Gradle validator checks that the Java PNG decoder can read it, while client resource loading requires a successfully created graphics backend. The earlier CI smoke (run #176) passed despite both OpenGL and Vulkan backend creation failing under Xvfb; its success did not verify atlas/model loading. This branch also configures Mesa software rendering for CI and fails the client smoke when neither backend starts or resource error patterns appear. Review the next CI log for resource reload completion.

Non-square/high-resolution sprites can have different edge extrusion, filtering, mipmap behavior, and perceived size from conventional low-resolution item icons. Static checks and startup logs can establish that this specific resource parses, packs, and loads, but do not prove attractive appearance, hand placement, or every display context visually. Any renderer/atlas warnings must be reviewed separately. The image remains byte-for-byte identical to the approved input.

## Resource graph and reuse

`ModItems.FLAME_SWORD` → `moreminecrafts:flame_sword` → `assets/moreminecrafts/items/flame_sword.json` (`minecraft:model`) → `assets/moreminecrafts/models/item/flame_sword.json` (`parent: minecraft:item/handheld`, `layer0: moreminecrafts:item/flame_sword`) → `assets/moreminecrafts/textures/item/flame_sword.png`.

Minecraft's normal item model display contexts select the same model for GUI, held first/third person, ground, and fixed/item frame. There are no separate context textures, custom renderer, special hand transforms, VFX, or animation. This static sword resource path is the reference for later Water, Wind, and Lightning sword resources: each supplies its unique approved texture through the same client item → handheld model route, with gameplay handled separately. This PR does not implement those swords.

## Non-live validation

`./gradlew build` runs the existing JUnit and `validateStage6Data` checks plus `validateFlameSwordProductionAsset`. The latter checks registration, JSON/model/texture graph, PNG signature and IHDR (1263 × 1246, color type 6 RGBA), alpha distribution, pinned source SHA-256, and exact JAR entries/JSON/PNG bytes. Compare the original ZIP entry, repository file, and built JAR SHA-256 again in the delivery report. CI runs server and software-rendered client smoke tests; review client logs for texture/model/JSON/atlas/mipmap/decode/bake failures. An initialization log without a graphics backend does not count as resource loading.

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
- [ ] Absence of unwanted flat or paper-like presentation

Non-live success does not complete C1 or establish a live rendering pass.

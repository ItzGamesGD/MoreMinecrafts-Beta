# Stage 1: cumulative foundation recovery

- Recovery base: `stage-11/checkpoint` at `3cb27187e3be0141bcd175e4e355f9fa9cdc6300`.
- Recovery branch: `recovery/post-stage8-cumulative`.
- PR target: `main` (the checkpoint branches remain unchanged).
- This line preserves validated Stage 11 implementation and reapplies later approved resource, deepslate, Fishing, and Flame Sword C1 work.

## Reconciliation

Stage 11 remains the source for Enhancement 2.0, Alchemy/Shock, Stage 9 Skeleton King, T2 gameplay, LiveTestCommand, existing validation coverage, and Stage 10–12 recovery documentation. The latest reconciled Fishing implementation and approved Resource D assets are taken from the post-Stage-11 tree. Obsolete Fishing species and equipment Tier IV/V resource definitions are removed; equipment remains Tier I–III and Fish Grade remains I–V.

Approved material and Flame Sword production assets are reused from their existing repository blobs. The Flame Sword artwork is not modified. The C1 item/model/texture chain remains registration → client item definition → `minecraft:item/handheld` model → `moreminecrafts:item/flame_sword` texture. No custom renderer or VFX is introduced by this recovery.

Deepslate ore variants are registered and linked to their block states, models, loot tables, tool tags, and textures. Worldgen uses the Minecraft 26.3 `worldgen/feature` schema. Stone replaceables target the normal ore blocks; deepslate replaceables target dedicated deepslate ore blocks. Adamantium remains Nether-only. The Stage 14 legacy `worldgen/configured_feature` schema is not carried forward.

PR #39's audited Stage 6 and server-smoke checks are ported as file-level changes, not as a branch merge. Stage 11's stricter rejection of legacy block-state target objects is retained in the validator. Server smoke requires the real `Done (...)! For help, type "help"` readiness line and rejects fatal registry/datapack/server markers.

## Validation status

Cumulative validation must pass in this PR's CI before this recovery is considered complete:

- Gradle build and JUnit tests
- `validateStage6Data`
- Flame Sword C1-A and C1-B asset validators, including JAR linkage/hash checks
- available Fishing/resource validation
- Fabric server smoke with actual readiness marker and fatal-marker scan
- Fabric client smoke with renderer backend, MoreMinecrafts resource reload, item atlas, and no missing/decode/bake/stitch errors

PR #39 CI run #188 passed on its Stage 14 based branch, including server readiness and client resource smoke. That result is prior evidence for the ported fixes, not a substitute for cumulative validation on this recovery branch.

## Deliberately pending or deferred

Live visual checks remain pending for inventory/GUI, first/third-person and off-hand, dropped item, item frame/fixed display, animation/orientation, scale, rotation, hand placement, pixel density/filtering, and unwanted flat presentation.

Stage 2 separately handles gameplay/progression design cleanup. Fishing card interaction, high-resolution generated geometry, T3/T4, balance changes, and Coating remain out of scope; Coating is on hold.

# W16 C1 — natural sky islands, native-client evidence and ground habitat

Base: `checkpoint/wave15-unified-20261029`, `c9f7a4bf748a3f688c4a3f8103bc16ba789d9b50`.
The Wave 15 SHA/PR inclusion/CI audit was not repeated. The separate sky-island/structure audit's final closure remains **UNKNOWN**.

## Contract and scope

The owner selected ground-surface membership on 2026-10-29: a saved, generated sky-island StructureStart must reference the actual Jigsaw piece; the support coordinate, inverse-transformed by that piece's saved origin/rotation, must be an originally solid voxel; the current world must have solid support, empty feet/head collision and no fluid at the feet. Jigsaw placeholders are interpreted through their `final_state`, not their authoring solidity. Filling originally empty bounding-box space with player blocks does not extend habitat.

`SkySpawnRules.allows` is an **opt-in prerequisite** for future C2 ground variants and accepts NATURAL only. Callers must additionally retain their vanilla species placement, light, collision and mob-cap rules. COMMAND, STRUCTURE, spawner and other sources do not enter this natural-spawn gate. No spawn lists, entity registrations, existing SKY mobs, vanilla phantoms, NBT, frequencies or worldgen rules were modified. Flying-mob airspace remains deferred.

Eight exact existing structure IDs are accepted. The query uses this chunk's saved structure references and per-piece bounds, never `/locate`, a global structure scan, or an inferred radius. Original support sets are cached by template object, cleared on datapack reload/server stop. No world-start cache survives unload/restart. Unloaded query chunks return empty without being loaded. The result includes structure/template identity, mountain-family identity and base-noise terrain-relative elevation; elevation is context, never an arbitrary eligibility threshold.

## Natural generation

Fresh Minecraft **26.3**, normal Overworld generation, unchanged #209 data. Worldgen probe is opt-in test instrumentation. No `/place`, synthetic NBT renderer or mob summon was used as natural-generation or visual evidence.

| Seed | Chunk search radius | Chunks examined | Natural starts | Observations |
|---|---:|---:|---:|---|
| 42 | 40 | 6,561 | 3 | forest tower, general meadow, rocky mountain island |
| 173 | 80 | 25,921 | 17 | general forest/meadow and mountain meadow/snowy families; ruins and towers |
| 20261029 | 80 | 25,921 | 12 | ruins and towers including snowy mountain tower |

Full initial outputs are in `worldgen/`. These are bounded samples, not a frequency estimate. Every discovered building selected for the physical sample passed its stored vault/chest/spawner/config/support checks. Across the three worlds all four ruin variants and tower sizes 5/6/7 were found. **Temple was not found in this search window.** Existing probe `all_physical_checks_pass=false` also requires all eight variants; it must not be represented as a full-coverage PASS.

An additional radius-160 seed173 scan exited **137/SIGKILL** before writing results. Memory pressure is suspected, not established. `expanded-scan-failure.txt` records this incomplete attempt; no generation-failure conclusion follows from it.

`coordinates.csv` indexes physical-sample bounds. Exact biome was independently read from the saved natural seed42 world: `minecraft:birch_forest` at the tower and `minecraft:windswept_hills` at the rocky island. Other initial physical rows did not record biome; this limitation is explicit, not inferred from the structure's style.

Representative natural locations (not camera positions):

| Seed | Structure/template | Bounds or validated surface | Biome |
|---|---|---|---|
| 42 | sky_island_forest / forest/tower | root (-352,116,-266)…(-310,189,-224) | birch_forest |
| 42 | same / tower_5 | building (-340,133,-254)…(-322,171,-236) | birch_forest |
| 42 | sky_island_mountain_rocky / rocky/plain_small_b | (-608,125,-560)…(-582,148,-538) | windswept_hills |
| 42 | forest ground surface | (-349,131,-238), terrain-relative +60 | birch_forest |
| 42 | tower surface | (-339,157,-253), terrain-relative +87 | birch_forest |

## Original in-game images

Actual rendered Minecraft 26.3 client, software Vulkan/lavapipe with Xvfb; connected to the normal-generation seed42 server. Spectator teleport, time/weather changes and structure location search were used only for observation. All image files are original uncomposited captures, with no generation, cropping, resizing or image editing. `*-native.png` is a native **F2** screenshot; the other files are direct full-size X11 captures of the running client.

`screenshots.json` gives filename, seed, dimension, actual camera coordinates, biome, structure, yaw/pitch, natural-generation flag, observation and SHA-256 for every image. All cameras face yaw 0 / pitch 22; the scenes are different positions/elevations, not full 360-degree coverage.

| Image | Scene |
|---|---|
| [tower overview](screenshots/seed42-tower-overview.png) | complete tower/island at a distance |
| [tower environment](screenshots/seed42-tower-environment.png) | original surrounding vanilla terrain |
| [tower top](screenshots/seed42-tower-top.png) | upper combat floor, vault/spawners/chests |
| [ground connection](screenshots/seed42-tower-ground-connection.png) | vegetation, tower foundation and surface |
| [lower side](screenshots/seed42-tower-lower-side.png) | original hanging rock and visible ground below |
| [tower content](screenshots/seed42-tower-content.png) | interior content through original opening |
| [vault/spawners](screenshots/seed42-vault-spawners.png) | close content view |
| [native F2 vault/spawners](screenshots/seed42-vault-spawners-native.png) | independent native-client original |
| [mountain environment](screenshots/seed42-mountain-environment.png) | floating rocky island and mountain context |
| [mountain top](screenshots/seed42-mountain-top.png) | snow-covered upper surface |
| [mountain lower side](screenshots/seed42-mountain-lower-side.png) | lower rock, separation from vanilla mountains |

## Visual assessment — observed scenes only

| Criterion | Verdict | Evidence / limitation |
|---|---|---|
| Outline / floating landform | MINOR | Rounded/tapered volume is legible; tiered shelves and repeated hanging stone projections remain visually apparent. This is a style/tuning observation, not an established generation defect. |
| Top-to-rock transition | PASS | Tower ground connection and lower-side views show connected original terrain, no observed detached building. |
| Scale and relative height | PASS | General tower island and small mountain island are distinguishable against original terrain; not a universal all-seed claim. |
| Building orientation / terrain fit | PASS | Sample tower sits on the island; original floors/content remain inside it. |
| Flat cuts / repetition / floating blocks | MINOR | Repeated shelves/projections visible. No unsupported building or isolated error block confirmed in inspected views. |
| Vegetation / lighting / decoration | MINOR | Forest planting is dense against the tower facade and limits visibility of the ground route; moss/weathering and snowy mountain surface are coherent. |
| Access / exploration route | MINOR (visual assessment) | Dense facade vegetation obscures the ground route in the inspected view. Spectator observation establishes spatial layout only; actual survival ascent, tower traversal and active trial gameplay were **NOT RUN**. |
| Vanilla-terrain harmony | PASS | Forest and mountain scenes visibly retain surrounding terrain and separation beneath the island. Fluids/other-structure intersections are not exhaustively covered. |

No confirmed MAJOR visual generation defect was established. Do not change architecture from these observations. A separate owner-approved worldgen/vegetation task may consider entrance visibility and less repetitive shelf silhouettes using these same reproduction coordinates. Temple visual acceptance and survival traversal remain pending.

## Implementation and reproduction

- `src/main/java/.../mob/sky/SkyIslandHabitat.java`: provenance, original solid masks, inverse rotation and terrain context.
- `SkySpawnRules.java`: natural-only opt-in gate; no existing spawn behavior changed.
- `ModRegistries.java`: cache lifecycle registration.
- `SkyHabitatGameTests.java`: no generated provenance => outside; non-natural-source isolation; connector final-state regression.
- `SkyHabitatNaturalProbe.java`: opt-in generated-world positive/negative and persistence checks; no template placement or entity creation.

Normal checks: `./gradlew test build runGameTest` (JDK 25; repository's fixed 26.3 dependencies).
Natural probe: configure a **new isolated** normal server world/seed in `build/sky-islands-probe/run/server.properties`, then run `runSkyIslandsProbe` with `-Dmm.habitat.out=<absolute json>` and optional `-Dmm.habitat.cx=<start chunk x> -Dmm.habitat.cz=<start chunk z>`. With no `mm.sky.out`, the broad existing worldgen scan remains inactive. Reuse that same saved world for the restart check. `-Dmm.habitat.unload=true` additionally waits for test chunks to unload, verifies query does not load them, explicitly reloads and repeats positive membership.

See `verification.json` for final statuses and counts. `habitat-fresh.json` and `habitat-restart.json` are process-separated runs on the same naturally generated saved world, not template-placement fixtures.

## Final local verification

- JUnit **511/511**; GameTest **493/493**; `test build runGameTest` **BUILD SUCCESSFUL** after the connector fix.
- Connector regression was first observed failing at tick 0, then included in the successful full suite.
- Separate real-server seed42 fresh/restart surface records are identical. Actual chunk unload, unloaded query without forced load, and explicit reload passed in `habitat-unload-reload.json`.
- Independent read-only code review found the connector issue; the fix was reviewed with no further important findings.
- Visual assessment is limited to the eleven committed original images; survival traversal and temple visual acceptance remain pending.
- The additional radius-160 scan failure remains reported and does not invalidate the completed three bounded initial scans.

## Supplementary natural ground provenance (final code)

The opt-in habitat probe also generated new normal worlds for seeds 173 and 20261029. Both passed natural-source acceptance, command-source rejection, below-island and high-air rejection. Full surface coordinates, actual biomes and terrain-relative elevations are in `habitat-seed173.json` and `habitat-seed20261029.json`. These server-only probes supplement, and do not replace, the seed42 client screenshots.

| Seed | Validated surface | Structure | Actual biome |
|---|---|---|---|
| 173 | (114,180,-24) | moreminecrafts:sky_island_mountain_meadow | minecraft:meadow |
| 20261029 | (-807,112,-252) | moreminecrafts:sky_island_mountain_snowy | minecraft:stony_shore |

**C2 readiness:** the ground provenance/gate is ready for a separately authorized five-variant implementation. C2 must call the gate in addition to vanilla species checks and separately settle its remaining per-variant stats/resources/drop details. No C2 mob or flying-airspace definition is included here.

# ST-05 Stage 0-G — block handling, blueprints, real-client visual verification

Scope: already-implemented Stage 0-F arenas. No new design; no Stage 1 content. Evidence lives under `tools/st05-worldgen-probe/stage0g/`
(`blueprints/`, `blueprint-data/`, `raw/` originals + `CAPTURE_METADATA.{tsv,json}`, `overview/`, `defect-evidence/`) and
`tools/st05-worldgen-probe/stage0f/live/seed-7001|7002/h-*` (live rebuild with the final code).

## A. Block handling (natural sculk)
Block-entity blocks stay `PROTECTED` unless **all** provenance evidence holds (`St05Stage0FLive.naturalPatchBlock`): type is one of the three deep-dark
patch blocks (sculk sensor / shrieker / catalyst), position is in the `deep_dark` biome, no structure piece box contains it (ancient-city blocks stay protected),
the chunk's `InhabitedTime` is 0 (no player ever occupied it, so nothing can be player-placed), and it sits on/beside sculk/sculk vein. Type alone never decides.
Everything else (chests, skulls, spawners, anything player-touched or unknown) is still protected. Counters are written per placement (`block_entity_provenance`).
Result: placement `2ac822e8…` (failed closed in Stage 0-F) now builds (RUINED_NATURAL_CEILING, VERIFIED): 370 natural patch blocks accepted; 154 inside the
ancient-city boxes, 15 non-patch block-entity blocks, 2 outside deep_dark, chests and skulls rejected (kept protected). Regression: 169-check pure suite 169/0; live re-verification, settle ticks, 0 plan mismatches.

## B. Blueprints (from the production definition)
`BlueprintExport` calls the same `ArenaDesign.build` and reads the same persisted live plans (`f-<id>.plan.gz`) the server applied; `render_blueprints.py` only projects them.
Per built arena (INTACT `20d89ee7`, NATURAL_CEILING `809b9bf8`, NATURAL_CEILING `2ac822e8`, PARTIAL_DOME fixture `77ba83e0`) plus three reference-geometry variants:
`*-plan.png` (floor plan with r=34/29/27/24/12 rings, entrances N/E/S/W, columns, 6 vaults + centre, corridor routes), `*-section-WE/NS.png`,
`*-layers.png` (offsets −1,0,2,6,11,14,19,22), `*-legend.png` (dimensions, materials, orientation, reference coordinates), `variant-comparison-{plan,section}.png`,
`*-blueprint.json` (vaults, columns, routes, per-layer block counts and key block coordinates) and the full cell data `blueprint-data/*.json.gz`.

## C. Real Minecraft 26.3 client capture
72 original 1920x1080 screenshots (18 per arena), `raw/`, metadata (seed, arena id, variant, centre, camera eye/yaw/pitch/target, night-vision flag, cut-away flag) in
`raw/CAPTURE_METADATA.*`. Real Minecraft 26.3 client (Fabric client gametest), Vulkan llvmpipe software renderer under Xvfb, spectator camera, HUD hidden.
Scenes: aerial (06), interior (02/02b/02c), entrances N/E/S/W + columns (03), vault ring / vault close / centre Ominous Vault (04), corridors to the cave and the junction (05),
dome exterior side (06d), cross-section + oblique section (06b/06c), PARTIAL_DOME arena (`77ba83e0`), NATURAL_CEILING arenas (`809b9bf8`, `2ac822e8`).

Capture conditions that must be stated honestly:
* The client-gametest harness always creates its own world, so the saved Stage 0-F worlds could not be opened. The same seed is regenerated and the **persisted live plan
  is replayed** (all ops). Before replay every plan cell is compared with the recorded pre-state: 98–99.99% identical (per arena: 99.99%, 99.998%, 98.1%, 99.9%); the remainder were accepted only as flowing-fluid level,
  ore/vein substitution, non-solid decoration or natural sculk state differences (counts logged `ST05G terrain-identity`). Any other difference aborts.
* Night vision is on for all scenes except `02c` (true block lighting). Sections 06/06b/06c/06d are **photo copies with natural terrain removed** (`cutaway_photo_copy`); no such removal exists in the arena itself.
* The PARTIAL_DOME arena is a fixture: id `77ba83e05adb2600` at a Stage 0-E centre (variant is a function of the id; no real reservation had that variant). It is not a Stage 0-E roll result.

## D. Visual verification and defects
| Check | Result |
|---|---|
| Overall form / proportions | OK: 68-block drum, 12-high wall, stepped spherical dome, four 7x8 entrances (aerial/side shots vs blueprint) |
| Dome naturalness | OK as voxel dome; concentric stepped rings are visible from outside and inside (aesthetic, unchanged) |
| Column rhythm | OK: ribbed wall with evenly spread columns, repeated pattern, balanced on all four entrance views |
| Corridor / cave junction | OK: 5x5 tunnel with stairs, lining fades into rock, opens into natural cave in every arena |
| Vault accessibility | OK: six vaults on the floor ring with free space; Ominous Vault at the centre, floor clear around |
| Terrain fusion | OK: natural-ceiling arenas show the original cave ceiling (incl. deep-dark sculk) above the 12-high wall |
| Ruined variants | PARTIAL_DOME: natural rock/ore/moss visible through dome gaps; NATURAL_CEILING: wall only. Acceptable; no incomplete geometry seen |
| **Lighting / visibility** | **DEFECT (fixed)**: with true lighting the combat floor was almost black although WG-Q12 requires a bright floor. Only 16 flush lamps existed. |

Fix (commit `26feffff`): five concentric flush-lamp rings (5/11/16/22/28 lamps = 82), `Verify.floorLight` propagates block light from the planned lit lamps through the open
volume and fails construction/verification if any combat-floor air cell is below 8 (my engineering reading of "bright"; the authority gives no number). All four arenas were rebuilt live and VERIFIED
with minimum floor light 8 (`h-run.json`, `h-partial.json`); before/after: `defect-evidence/BEFORE_…` vs `AFTER_…`.
Capture-harness defects fixed on the way (not arena defects): wrong world served by the harness, watchdog on a 1M-block cut, client keep-alive timeout, first-shot meshing lag.

### Remaining visual issues / limits
* In true lighting the dome and upper wall stay dark; only the floor is bright. Adding lanterns/ceiling light would be a design change (not made). Torches are the only wall light.
* Entrance doorways in some interior shots glare white: far unmeshed sections of the software renderer render as sky; corridors themselves are shown in the 05 shots. Section scenes 06b/06c also contain such renderer gaps.
* Software (llvmpipe) renderer, not a GPU; colours/lighting follow the vanilla light engine but no shader/GPU parity claim.
* Capture world = replay on the same seed (see above), not the byte-identical saved world. Only 4 arenas were photographed; the INTACT variant occurred 1x here, the fixture 1x.
* No behavioural/combat/mob-spawn testing (Stage 1). Light level 8 is not a spawn-proofing claim.

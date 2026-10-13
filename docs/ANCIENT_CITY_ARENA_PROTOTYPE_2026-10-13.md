# Ancient City Arena prototype

## Authority and implementation

Verified implementation base: `stage-20/checkpoint` at
`20121ca7cceae03602038de31076bd9ace115208` (merged cumulative recovery PR #69).
The approved design is `docs/ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md`
on `docs/2026-10-07-content-first-axis-review` at
`8bf157f7988569c715bde4dfd298e9af20305415`.
Implementation branch: `codex/ancient-city-arena-prototype-20261013`.
No gameplay, progression, Warden behavior, or other city variants are added.

Target: Minecraft 26.3, Fabric API 0.161.0+26.3, Java 25; official client
SHA-1 `e877b6a07acd633fb3bb475002175cec036e7b87`, data version 5023.
The shipped structure NBT uses `id` / `properties`, not legacy palette keys.

## Integration and layout

Fabric's dynamic-registry setup callback watches pool entry loading. A small
`StructureTemplatePool` mixin appends contributions to both the raw weighted
entries and the actual expanded candidate list, invalidating the height cache.
Per-registry state prevents duplicate insertion and handles either load order.
All vanilla entries, fallback, and normal jigsaw placement rules are retained.

Contribution: `moreminecrafts:ancient_city/arena_additions`, targeting
`minecraft:ancient_city/structures`. Production weight **1**, alongside **46**
vanilla weighted slots: approximately **2.13%** to lead a shuffled candidate
list at an eligible connector. This is **not a per-city probability**: empty
candidates, rotation, collision space, and city assembly affect actual placement.
Change the contribution JSON weight for balancing; add independent contribution
resources and mappings for future structures.

Template: `data/moreminecrafts/structure/ancient_city/arena.nbt`, **37 × 13 × 37**.
A round fighting floor, four supported seating tiers, city-style chiseled piers,
soul lanterns, wool entrances, sculk patches, and a collapsed southeast sector
reuse vanilla Ancient City materials. It contains no entities or new rewards.
The west-facing `connect_structure` jigsaw matches vanilla city-wall connectors.
Rigid placement uses vanilla `ancient_city_generic_degradation`; authored air
clears the bowl, while unwritten corner cells preserve surrounding terrain.

The reproducible Python source is `scripts/structures/generate_ancient_city_arena.py`.
Install `scripts/structures/requirements.txt`, run the generator with `--check`,
then run `scripts/structures/validate_ancient_city_arena.py`.

## Evidence and status

- **IMPLEMENTED: PASS** — production template and additive registration exist.
- **STATICALLY VERIFIED: PASS** — reproducible NBT, palette/index/bounds and
  entrance checks; actual runtime template loading and vanilla connector attachment.
- **RUNTIME VERIFIED: PASS** — local `./gradlew clean build`, all **154 unit tests** in 34 suites, resource validators and all **32 GameTests** passed. A normal Fabric
  server reached `Done` with the complete production resources.
- **WORLDGEN VERIFIED: PASS** — seed 2632026, production weight 1, arena at
  bounds `(-7088,-51,-3902)` through `(-7052,-39,-3866)`, rotation
  COUNTERCLOCKWISE_90, city chunk `(-443,-238)`, 52 vanilla pieces. Five physical
  floor samples resolved to wool/sculk. A city without the arena at chunk
  `(246,-353)` retained 76 vanilla pieces. Found after 19 deterministic locate origins.
- **VISUALLY VERIFIED: pending** — native Minecraft client captures and actual
  image inspection are required; a PNG quality check alone is insufficient.

Deterministic selection test: 4,096 seeds, arena selected **88** times by
`getRandomTemplate`, arena first **106** times in the real jigsaw shuffled list,
and ordinary non-empty candidates selected **3,399** times. The test verifies
the configured weight participates in the expanded pool and preserves all
vanilla weighted slots. A second GameTest uses actual `JigsawBlock.canAttach`.

Runtime world seed: **2632026**. `MOREMINECRAFTS_ARENA_VALIDATION=1` enables the
operator-only `mmarena validate` observer. It locates normal Ancient City starts,
inspects actual assembled pieces, generates full terrain, and checks physical
voxel samples. It never places a template or increases production weight.
`worldgen.json` records seed, city, rotation, bounds, vanilla pieces, samples,
and spectator cameras; a city without the arena is recorded when found.

The dedicated Actions workflow uses Java 25, full resources, Xvfb and Mesa
llvmpipe Vulkan. It captures two views with Minecraft's native `Screenshot.grab`
after client world rendering. It uses spectator mode and night vision for
visibility, without editing the generated terrain. No safe-mode resource
exclusion or external renderer is used. Validation hooks are disabled normally.

First Actions run **38343896620** passed the build, template validation, and
normal worldgen. Native capture was blocked by the validation server whitelist;
the isolated workflow now explicitly disables its whitelist, matching the
proven repository capture setup. Diagnostics artifact ID **11787588477**
(`ancient-city-arena-visual-validation-38343896620`) preserves that attempt.
Screenshot inspection: pending the corrected capture run.
Artifact name: `ancient-city-arena-visual-validation-<run-id>`.
Screenshot names: `arena-camera-0.png`, `arena-camera-1.png`.

Limitations: prototype weight is provisional; a template can be rejected by
normal jigsaw collision rules. Vanilla degradation and terrain may alter the
authored appearance. Cave clipping, accessibility, and visual coherence must
be judged from the real generated-world captures before visual acceptance.

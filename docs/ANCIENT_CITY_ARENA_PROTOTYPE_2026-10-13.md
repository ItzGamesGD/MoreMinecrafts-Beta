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
- **WORLDGEN VERIFIED: PASS** — final seed **2632027**, production weight 1,
  arena bounds `(7824,-51,-5616)` through `(7860,-39,-5580)`, rotation
  COUNTERCLOCKWISE_90, city chunk `(485,-348)`, **72 vanilla pieces**. Five
  physical floor samples resolved to wool/sculk. A city without the arena at
  chunk `(194,-423)` retained **77 vanilla pieces**. Found after 17 locate origins.
  The earlier seed 2632026 independently produced an arena with 52 vanilla pieces.
- **VISUALLY VERIFIED: PASS** — two final native Minecraft screenshots were
  opened and inspected after a visually motivated layout/camera revision.


Deterministic selection test: 4,096 seeds, arena selected **88** times by
`getRandomTemplate`, arena first **106** times in the real jigsaw shuffled list,
and ordinary non-empty candidates selected **3,399** times. The test verifies
the configured weight participates in the expanded pool and preserves all
vanilla weighted slots. A second GameTest uses actual `JigsawBlock.canAttach`.

Runtime world seeds: **2632026**, then **2632027** for the visual revision. `MOREMINECRAFTS_ARENA_VALIDATION=1` enables the
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
Corrected Actions run **38344533474** at `04c2986a9871b1289b384e78d602685abcecb63a`
passed all automated stages. Artifact ID **11788422346** is
`ancient-city-arena-visual-validation-38344533474`; both 1280×720 native
screenshots were opened and visually inspected. The ring, chiseled piers,
seating, ruins and palette belonged to the surrounding city; there were no
obvious detached components or catastrophic clipping. However, the near
parapet occluded much of the fighting floor, and a natural waterfall crossed
the bowl. Both initial images remain preserved in the artifact.

Visually motivated revision: lower the perimeter parapet by one block, keep
the supported colonnade and seating, raise the overview camera, and move the
second camera inside the ring to expose the floor. Capture another normal
world sample at seed 2632027 with unchanged production weight 1. The revised captures visibly improve floor and seating readability. Natural cave water can still enter an open arena;
this prototype does not suppress terrain aquifers or claim all seeds are dry.
Final successful workflow run: **38345195208**, validated source revision
`41c429e8d073cf96ef916e8d4617515db0ad03c7`.
Artifact: **ancient-city-arena-visual-validation-38345195208**, ID **11787944376**.
Native screenshots: `arena-validation-results/arena-camera-0.png` and
`arena-validation-results/arena-camera-1.png`, both **1280×720**.
[Workflow and artifact](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38345195208).
The artifact includes worldgen JSON, server/client logs, test reports and mod JARs.

Final visual inspection: the overview shows an identifiable arena ring in an
existing city, stepped seating, supported masonry, a lower broken sector, and
wool paths meeting the city. The interior view clearly shows the dry fighting
floor, sculk patches/sensors, four seating tiers, and soul-lit colonnade.
There are no obvious disconnected pieces, catastrophic terrain intersections,
malformed palette substitutions, or inaccessible buried room. Small floor holes
are expected vanilla degradation, not wholesale template failure. Natural cave
waterfalls/lava remain visible outside this final arena; the earlier seed's
waterfall through the bowl remains a documented terrain limitation.

The final authored template has **3,004 non-air blocks** (including the connector,
which becomes wool in generation), in one face-connected component. Local and
Actions normal-worldgen evidence agrees. The static connectivity validator also
rejected an isolated detached-block regression probe. Shell/Python syntax checks
passed; no existing validation was weakened. No external inference API or
external renderer was used.

Limitations: prototype weight is provisional; a template can be rejected by
normal jigsaw collision rules. Vanilla degradation and terrain may alter the
authored appearance. The inspected final world sample passes cave clipping, accessibility, and
visual coherence checks. This is a prototype validation of two deterministic
seeds, not a guarantee of placement quality in every possible cave.

## Changed files

- `.github/workflows/ancient-city-arena-validation.yml`
- `docs/ANCIENT_CITY_ARENA_PROTOTYPE_2026-10-13.md`
- `scripts/arena_validation/capture_client.sh`
- `scripts/arena_validation/check_screenshots.py`
- `scripts/arena_validation/rcon.py`
- `scripts/structures/generate_ancient_city_arena.py`
- `scripts/structures/requirements.txt`
- `scripts/structures/validate_ancient_city_arena.py`
- `src/client/java/dev/itzgamesgd/moreminecrafts/client/ArenaVisualValidationHarness.java`
- `src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java`
- `src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgen/AncientCityArenaGameTests.java`
- `src/gametest/resources/fabric.mod.json`
- `src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/StructureTemplatePoolMixin.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/ArenaWorldgenProbe.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/StructurePoolContributions.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/StructurePoolExtension.java`
- `src/main/resources/data/moreminecrafts/structure/ancient_city/arena.nbt`
- `src/main/resources/data/moreminecrafts/worldgen/template_pool/ancient_city/arena_additions.json`
- `src/main/resources/moreminecrafts.mixins.json`

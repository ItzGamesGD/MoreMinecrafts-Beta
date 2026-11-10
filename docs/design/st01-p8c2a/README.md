# ST-01 P8-C2-A — complete pyramid construction

Authority: [owner approval of G0-R1 bundles 1–3](G0-R1-OWNER-APPROVAL-RECORD.md), blueprint commit
`c26ea0f871e788d1d331b40151779d86590469f2`. Implementation base: that commit. It contains P8-B2 `03685656`,
G0 `6b64106f` and D1 `e229d001`. O-14 `7e37b786` is not an ancestor, and nothing from it is needed here.

## What was built

The approved machine blueprint is exported, not re-designed. `tools/st01-p8c2a/export_assembly.py` replays the
blueprint's ordered operations, overlays original D1 and applies the 12 R1-01 cells, exactly as
`operation_semantics` prescribes. It then asserts that the result equals the approved per-route `closed_signature`
(SHA-256 over palette, the full 81×72×81 grid and the block entities), and splits that identical array into three
placement pieces:

| Order | Piece | Resource | Content | SHA-256 |
|---|---|---|---|---|
| 1 | ASSEMBLY (route EAST) | `moreminecrafts:pyramid/st01/assembly_east` | 273,062 operation-written cells outside the D1 box; 81×72×81 frame at authored (−40,−32,−40) | `fe5afb19c2587f1678f2947040c43aec67503b60380599f45934dd6034d6616c` |
| 1 | ASSEMBLY (route WEST) | `moreminecrafts:pyramid/st01/assembly_west` | same frame, WEST access route | `55cb285c5a727a5268c708d8fe9ab4ece9f56e995922c1776d936ce2139c2818` |
| 2 | D1_ROYAL_HALL | `moreminecrafts:pyramid/royal_hall` | original D1, byte-identical, 39×20×39 at (−40,−24,2) | `7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423` |
| 3 | D1_SEAM_EXCEPTION | `moreminecrafts:pyramid/st01/d1_seam_exception` | exactly the 12 R1-01 air cells, 4×1×3 at (−8,−24,33) | `72f32c062daecb018d4184910e9d11c980b864c527af0383e50178dcae521b89` |

The 168,910 cells that no operation writes are left out of the templates (structure void). They are the open air above
ground, outside the stepped shell. So the asset never claims terrain outside the authored envelope. Clearing or keeping
terrain there is a C2-B worldgen decision. D1 owns every cell of its box (30,420 entries), so the pieces never write
the same cell. The seam piece is the only one placed over another piece's cells.

[`generated/assembly-manifest.json`](generated/assembly-manifest.json) records:
- for each route, all 114 components (52 cavity components, 61 solid components and the immutable D1);
- origins, parents, placement order and the P1/P2/P4 connection graphs;
- placement origins and rotation pivots;
- per-state cell counts;
- runtime metadata and asset hashes.

`python3 tools/st01-p8c2a/export_assembly.py --check` reproduces every byte.

The exporter is stdlib Python, following MM's in-repo generator conventions: lowercase `id`/`properties` palette,
sorted positions, gzip mtime 0, OS byte 3, DataVersion 5023. The MM-SG exporter was not used or modified. MM-SG
`61a09eeb` contributes only through the G0-R1 26.3 block-state extract.

### Component inventory (both routes; WEST differs only in `P4:CB1`)

- P1 aboveground cavities (10): S00 entrance, S01–S03, S04 gallery, S05/S06 descent, S08/S09, S10 altar room.
- P1 solid elements (38):
  - 6 hall columns and 4 court columns
  - 3 ledges and 3 parapets
  - 12 gallery stair treads, 5 descent treads and 2 altar treads
  - 2 gallery support piers and the altar pedestal
- P2 underground (28):
  - L1 tombs T1–T3 with branches BR1–BR3
  - L2 stores B1–B3 with branches BRB1–BRB3
  - V12/V23 vestibules, landings LND12/LND23, stairs FA–FD
  - corridors C*, R, the antechamber A and the royal-hall port T
  - the annex port ANNEX_PORT and ANNEX1
- P2 solid elements (2): the antechamber statues at X−27 and X−15.
- P4 fixed secret complex (14 cavities):
  - secret descent FA and landing LA
  - access route CB1 (EAST: one segment; WEST: five segments)
  - VAULT treasury and its entrance VAULT_ENT
  - corridors CC and CC3
  - CRYPT and six niches
- P4 solid elements (21): four columns, each with base, shaft and cap; dais and dais stair; beams; chest sculpture base,
  body and lid; gold and diamond piles; crypt sarcophagus.
- Furniture, lighting and accents (operations): rear sarcophagi in the L1 tombs, oak shelves in the L2 stores,
  flower-pot pottery on cut-sandstone pedestals, the annex shelf and pots, and smooth/cut-sandstone walking surfaces and
  inlays. Also 94 wall torches, the gold edge trim and pixel wings, the cyan emblem, the lapis shaft accents, and the
  empty treasury UI-anchor chest.
- D1 royal hall (immutable NBT), including the PRIMARY double chest at (−21,−20,33)/(−20,−20,33) and the Guardian
  emergence point (−21,−22,26).

## Runtime contract (P8-B2 compatible)

`St01PyramidAssembly` (production code) provides:
- the `Orientation` enum (direction the entrance faces: SOUTH=NONE, WEST=CW90, NORTH=CW180, EAST=CCW90);
- the `AccessRoute` enum (EAST/WEST);
- `pieces(route)`;
- `worldPos`;
- `completionBox`;
- `describe(...)`, which returns the D1 placement origin and settings, the PRIMARY left/right, the Guardian spawn, the
  treasury anchor and the completion bounds;
- `place(...)`.

Every piece uses `placementOrigin = ground + authoredMinCorner`, `rotationPivot = −authoredMinCorner`, the same
`Rotation` and `Mirror.NONE`. The D1 piece uses `St01RoyalChestPlacement.placementSettings`, so
`St01RoyalChestPlacement.registerPlaced(level, d1PlacementOrigin, d1Settings)` works unchanged.

Not implemented here, by design:
- natural worldgen and terrain handling;
- the server-thread completion handoff;
- the EAST/WEST selection policy;
- automatic royal-chest registration;
- any loot, unlock, mineability or protection behavior.

## Validation

| Check | Evidence | Result |
|---|---|---|
| Blueprint reproduction | `tools/st01-g0-r1/verify.py`: two runs, byte-compared with the committed files and the Java selection reference | PASS (local and CI) |
| Export reproduction and approved signatures | `export_assembly.py --check`: both routes equal `closed_signature`; D1 hash unchanged | PASS |
| Java contract vs manifest; asset hashes; D1 bytes; orientation mapping | `St01PyramidAssemblyTest` (JUnit, 4 tests) | PASS |
| Eight-case actual placement | `St01PyramidAssemblyGameTests`: the exported templates placed in a real 26.3 server; the full 81×72×81 box and all block entities dumped; PRIMARY pair, `registerPlaced`, canonical id, idempotence and no SECONDARY asserted in game | 8/8 PASS |
| Independent verification of the placed blocks | `verify_placement.py` on the dumps (see below) | PASS, 8/8 |
| Negative controls | the same verifier rejects corrupted dumps: shell block replaced, D1 cell changed, Guardian volume obstructed, WEST route blocked, torches mis-rotated | 5/5 detected |
| Rendered client | `St01PyramidAssemblyClientCaptureTest`: 18 views of two placed assemblies (SOUTH/WEST, EAST/EAST) | captured; owner review pending |

Per case, `verify_placement.py` checks the actual Minecraft block data:
- **Full-grid equality** with the rotated design. Every one of 472,392 cells must match, with Minecraft's own rotated
  states compared against the blueprint's rotation contract.
- **D1 preservation.** Of the 30,420 D1 cells, exactly the 12 R1-01 cells differ, and they are air. The Y−23 cover is
  intact.
- **Traversal:**
  - entrance to PRIMARY (closed);
  - secret concealed when closed, treasury and crypt reachable when open;
  - all ten room goals reachable, including the annex;
  - a three-wide path from the landing to the vault;
  - P2 stair and FA tread clearances.
- **Clearance and orientation.** Guardian 3×3×5 clearance; the entrance faces the requested orientation.
- **Route selection.** The selected route is open and the inactive route is filled.
- **Chests and block entities.** PRIMARY handedness and pair vector; block-entity positions and types (three chests,
  all empty, no loot table); the treasury anchor is a single chest.
- **Metadata and registration.** The metadata equals the world positions, and registration succeeds.
- **Lighting.** 94 wall torches, all supported.
- **Shell and boundary.** The underground boundary and floor are sealed, and the non-air extent equals the completion
  box. There is exactly one exterior opening, the authored entrance (X−12..12, Y1..11, Z29..40). There is exactly one
  sealed pocket: the secret complex when closed (EAST 5,987 cells; WEST 6,251 cells), identical across all rotations.
- **Fixed chambers.** Treasury, crypt and D1 are identical between EAST and WEST, and every rotation equals SOUTH in
  the authored frame.

Commands:

    python3 tools/st01-p8c2a/export_assembly.py --check
    JAVA_TOOL_OPTIONS="-Dmm.st01.assembly=true -Dmm.st01.assembly.out=$PWD/build/st01-p8c2a/placement \
      -Dfabric-api.gametest.filter=moreminecrafts-gametest:st01pyramid_assembly_game_tests*" ./gradlew runGameTest
    python3 tools/st01-p8c2a/verify_placement.py --dumps build/st01-p8c2a/placement --out build/st01-p8c2a/verification.json
    tools/st01-p8c2a/run_client_capture.sh

Committed evidence: [`evidence/verification.json`](evidence/verification.json),
[`evidence/client-manifest.json`](evidence/client-manifest.json) and [`evidence/captures/`](evidence/captures/).
CI: `.github/workflows/st01-p8c2a-assembly.yml`.

## Limits

- **Placement flags.** Placement uses `Block.UPDATE_CLIENTS` into cleared air. Natural-terrain interaction, chunk-split
  worldgen pieces and post-placement physics in natural chunks are C2-B.
- **Navigation model.** Navigation uses the approved G0-R1 voxel navigator (full-cube stairs and chests, one-block
  ascents). Real player-movement traversal of the full route is NOT RUN.
- **Night vision.** Screenshots use night vision. They show geometry and materials, not the torch light level, and no
  illuminance or spawn-proofing is claimed. Visual acceptance remains the owner's.
- **Open cover.** The open-cover state is diagnostic only. No unlock mechanism exists.
- **Inherited edge risk.** The G0-R1 open-cover edge-drop risk is unchanged.

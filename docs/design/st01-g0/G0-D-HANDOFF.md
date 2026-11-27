# G0-D — Conditional construction handoff

Status: INCOMPLETE / NOT AUTHORIZED FOR CONSTRUCTION. Do not resume C2-A from this packet alone.

## Machine-readable inputs

- generated/candidate-components.json: 141 source records with exact inclusive boxes, source statuses, dependencies and graph edges; candidate-C overlay already reflected.
- generated/validation.json: pinned source hashes, actual D1 inspection, exact conflicting cell set and eight source-variant/rotation diagnostics.
- generated/COMPONENTS.md: human-readable component coordinate table.
- Existing P1 engine: aboveground solid elements/treads still need explicit transcription into the final blueprint. Generic cavity registry cannot substitute for them.

## Coordinate convention

Assembly origin is pyramid ground-centre, feet datum Y=0. X east+, Z south+, Y up. D1 template minimum reference corner is (−40,−24,2). For a reference coordinate p and whole rotation R about ground-centre, world(p)=worldGroundCentre+R(p). Local D1 origin must be computed using Minecraft's placement pivot semantics, not by rotating its corner alone while changing its internal pivot independently.

Authored entrance faces SOUTH (+Z). Whole orientation names denote the resulting entrance direction: SOUTH=NONE, WEST=CLOCKWISE_90, NORTH=CLOCKWISE_180, EAST=COUNTERCLOCKWISE_90. The spatial EAST hall designation is not an entrance-facing label. No whole-assembly mirror is authorized.

A compatible D1 placement contract is: world placement origin = worldGroundCentre + referenceOrigin; StructurePlaceSettings rotation pivot = −referenceOrigin. This gives worldGroundCentre+R(p) for every p while retaining B2 worldPos(reference, placementOrigin, settings). C2-A must independently verify this contract in Minecraft before use. Guardian authored facing rotates consistently; do not change B2 interaction-facing semantics.

D1 anchor=(-21,−20,33), right=(-20,−20,33), Guardian=(-21,−22,26). Identity remains dimension + transformed anchor. SECONDARY stays absent; no reward generation or runtime registration is part of G0. Secret orientation must become a separate field, not a whole-template mirror flag.

## Proposed ordering, subject to complete voxel verification

1. Materialize approved final shell/base masks.
2. Carve final P1/P2 and secret cavities; install their declared solid elements.
3. Place immutable D1 as an overriding module; reject unexpected overlaps rather than hiding them.
4. Apply only expressly authorized seam exceptions (NONE currently authorized).
5. Validate mandatory route and closed secret isolation; preserve block entities with explicit state maps.
6. Expose metadata only. Natural handoff is C2-B; durable placement progress is C2-C.

## Restart conditions for C2-A

Resolve D1-SEAM and SECRET-WEST explicitly. Finalize all component/solid masks, ceiling/junction profiles, floor-turn inlays, lighting and block entities. Approve numerical/visual bundles or their replacements. Validate the resulting materialized blueprint, including the two independent secret layouts under all four whole rotations. Only then call this a complete construction handoff. Existing abstract P1–P6 PASS results do not satisfy actual-D1 assembly verification.

The historical `P3:CHEST_SECONDARY` entry is evidence only and is explicitly excluded from construction alongside every P3 reconstruction entry. It must never become a physical chest or runtime registration position. Source registry statuses are not a permit to place that obsolete placeholder.

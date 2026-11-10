# G0-R1 geometric verification

Verdict: **DESIGN READY FOR OWNER APPROVAL**, not APPROVED / CONSTRUCTION READY. Required owner-level numerical and visual choices remain proposals. The two prior interface conflicts are resolved under the narrowly approved exceptions, not hidden or weakened.

## Reproducible checks

Run with Python3, NumPy2.3.5 and Java25 on PATH:

    python tools/st01-g0-r1/verify.py --out work/st01-g0-r1-verify

This independently executes the candidate generator twice in separate processes; compares blueprint.json, d1-exceptions.json, validation.json and COMPONENTS.md byte-for-byte; and compares Python route-selector vectors against the separate Java unsigned-long reference. `generated/reproducibility.json` records SHA-256 of all four files. The model signatures are calculated over palette, the entire little-endian uint16 block grid, and block entities, not merely bounding boxes.

No production NBT or runtime code is generated. The machine recipe is a materialized candidate, not a schematic asserted to exist. Source hashes pin all contributing P1–P6 Python sources, the G0 NBT decoder and new generator/reference extract.

## Eight cases

| Whole entrance-facing orientation | EAST access | WEST access |
|---|---|---|
| NORTH | PASS | PASS |
| EAST | PASS | PASS |
| SOUTH | PASS | PASS |
| WEST | PASS | PASS |

Each case checks exact D1 exception set; intact CLOSED cover; primary double-chest handedness; unchanged fixed hall/chamber coordinates relative to rotation; Guardian emergence clearance (3×3×5 conservative volume); entrance-to-PRIMARY navigation; fixed crypt and treasury reachability OPEN and non-reachability CLOSED; inactive route sample filled; three-wide landing-to-vault traversal; stair clearances; legal cavity overlap contract; correct block entities; supported light fixtures; state-reference validity; complete palette assignment; operation replay; and an independently transformed complete voxel array. Navigation is actually executed on every rotated occupancy grid.

## Detailed scope

- D1 hash: `7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423`; 39×20×39, unchanged bytes.
- Exactly 12 original solid cells become air in CLOSED assembly. All other D1 states equal the decoded original. The diagnostic OPEN state removes only the separate 12 cover cells, not a larger opening.
- Both routes share FA and LA, preserve D1 anchor/right/Guardian, and instantiate fixed chambers once. The new WEST path does not mirror any hall or chamber.
- Cavity intersection scans use exact voxel sets after bbox rejection. EAST has zero component cavity intersections. WEST has exactly the declared 48-cell CB1/CC destination junction; no unexpected inter-component cavity overlaps.
- All required source rooms, transition vestibules and the proposed Q35 annex have reachable interior sample goals in CLOSED. Source P2 staircase centreline support and three-high clearance are checked; the whole connected path uses final materialized blocks.
- Closed underground external boundary is solid throughout the proposed reservation. Body mass and named forecourt projections remain inside X/Z±40 and Y≤39. Proposed walls use mass masks; no claim of a universal minimum thickness at every connected junction is made. Actual exception/junction masks are explicit for review.
- Every new block state/property is checked against the 21-block extract of MM-SG's Minecraft26.3 server-derived reference. D1 state properties and block entities remain exact. Palette assignment covers the whole 472,392-cell design domain, including explicit air.
- Torch locations are exact operations; support checks run after D1 overlay. There are 94 retained supported wall-torch fixtures per variant. No D1 lighting changes or illuminance/spawn-safety guarantees.
- Rotations: NumPy array rotation is compared against direct index remapping over all cells; critical points and all exception cells also use the independent integer coordinate function. Rotated palette properties undergo inverse checks and primary pair alignment checks.
- Selector: reference is the existing O-14 measurement function, not a newly activated runtime. Five vectors, including negative/extreme signed coordinates, match Java. No reward RNG is invoked and no statistical 50% proof is claimed from five samples.

## Limits

The voxel navigator uses a conservative full-cube approximation for stairs, chests and flower pots, two-cell player height and one-block ascents with overhead clearance (jump-capable). The landing-to-vault test additionally checks a three-wide, three-high footprint; actual AI pathfinding and Minecraft interaction are NOT RUN. Block-state validity is offline reference validation, not an in-game placement test. Exported-asset checks, real placement flags, block-entity load/interaction and final visual review remain C2-A checks after approval.

No existing failed tests were modified, deleted or bypassed. Prior B2 full-suite ST-05 and intermittent Guardian failures remain causally unresolved. The scoped design workflow is separate from the unchanged existing Fabric CI. Actual CI run results are recorded in the draft PR/handoff report; pending is not PASS.

Inherited open-cover edge risk remains: the original P6 geometry records an exposed drop up to four blocks at some cover edges. R1 does not authorize additional D1 rails or reshaping, so no mitigation is invented here. The verified route uses consecutive tread levels; it does not establish fall-damage-free behavior for every approach to the opening.

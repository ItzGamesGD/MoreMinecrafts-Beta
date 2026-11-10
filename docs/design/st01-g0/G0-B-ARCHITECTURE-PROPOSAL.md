# G0-B — Architecture proposal (incomplete; NOT approved)

This is an exact-coordinate candidate inventory plus explicit unresolved interfaces, not a complete construction-ready blueprint. Read generated/COMPONENTS.md and candidate-components.json together with G0-A. Source statuses in copied records are retained as evidence; their presence does not promote authority.

## Preferred numerical adoption package

Retain the authored EAST composition. Recommend adopting P1 S00–S10 and its existing element geometry; P2's three L1 tombs and three L2 storage rooms; P6's candidate-C-adjusted coordinates; P4-B treasury and the representative crypt. Exact cavity boxes and 141 source registry records are in generated/candidate-components.json. Superseded P1R:S07 is a reservation only, not a second carve. P3 registry geometry is reference evidence only: D1 NBT must replace all P3 reconstruction.

Cavity boxes are inclusive integer block ranges, not rough bounding envelopes. Stairway unions must use every source box, not their enclosing bbox. Do not carve all of a concave corridor's bounding box. Preserve the source adjacency graph and source element solids; the registry alone omits some aboveground tread/ledge details and is not an executable full build recipe.

Recommend the P1 support alternative already studied in P1 F-6: piers X−8..−6 and X6..8, Y0..4, Z−3..−1. They avoid the central nave and X−5..5 stair opening; complete materialized collision verification is still required. Retain the current altar envelope without adding exterior decoration where its roof margin is only 3.

## Shell and base proposal

For every Y in 0..39, the pre-carve solid square is abs(X),abs(Z) <= 40−Y. No shell blocks outside X/Z±40. Subsurface mass proposal is X/Z±40, Y−32..−1, before cavity carving. This is a proposed construction prescription, not a newly approved excavation policy. Required voids, elements, interfaces, actual D1 and terrain boundary must be evaluated before accepting it.

## Block-state proposal

All identifiers below are exact vanilla full-block states with no properties:

| Region | Preferred state | Rule / status |
|---|---|---|
| Shell/base/wall mass | minecraft:sandstone | All pre-carve mass; PROPOSED |
| Room walking surface | minecraft:smooth_sandstone | Supporting floor blocks at each declared walking elevation; proposed mapping requires explicit region list |
| Corridor inlay | minecraft:cut_sandstone | Proposed centre strip, outer tiles smooth sandstone; concave turns and stairs require explicit masks before handoff |
| Columns/piers | minecraft:chiseled_sandstone | Source column volumes plus proposed piers; visual approval needed |
| Sarcophagi | minecraft:cut_sandstone | Source element boxes; no loot inference |
| Cavities | minecraft:air | Only cavity masks; never override immutable D1 silently |
| D1 | Exact existing palette/properties/NBT | No material substitution or regenerated NBT |

No random decoration is proposed. No extra chest, loot table or light fixture is implicitly added. Floor/inlay corner masks, lighting coordinates, ceiling profiles and treasury block-entity specifications remain incomplete; this prevents DESIGN READY FOR APPROVAL. Full-block stepped treads are the preferred proposal, without silently replacing them with half slabs or stair blocks.

Corbel proposal for corridors with width>=5 and clear height>=5: preserve the bottom three clear layers; inset the top two cavity layers by one and two blocks per lateral side, stopping at a three-block clear centre. For height4, use at most one top-layer inset where three-block centre survives. Height3 corridors require a separately specified roof envelope; do not reduce their clear section. Junction/turn masks and inter-floor clearance have NOT been validated; do not apply this generic rule as production geometry.

## Crypt candidate

Recommend the existing representative cavity X8..22/Y−30..−24/Z−7..5, central sarcophagus X14..16/Y−30..−29/Z−3..1. Niches: X9..11,13..15,17..19 at Y−30..−28, with north Z−9..−8 and south Z6..7. These are six niches, not an approved count. Geometry/material adoption requires owner approval. The treasury and crypt source profiles are retained without inventing new contents.

## Secret orientation proposal requiring resolution

Do NOT mirror the full source WEST composition: it moves the royal hall and anchor. A possible alternative is a west access route to a fixed treasury, but that changes the meaning of secret-area EAST/WEST and needs explicit owner approval and a newly validated corridor. No such route is specified as authoritative or declared complete here. See G0-C.

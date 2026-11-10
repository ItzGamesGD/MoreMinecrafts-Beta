# Materialized construction candidate

**PROPOSED except fields explicitly approved in AUTHORITY-DELTA.** Exact output: generated/blueprint.json. Every component has an ID, kind, inclusive bounds/boxes, origin, offset, parent and placement order. Every non-D1 write has an explicit box, block state and reason. The ordered operations, D1 overlay and exception mask fully specify a finite block array; a constructor must not improvise decoration.

## Coordinate and transform contract

Origin is ground-centre; +X east, +Z south, +Y up. Bounds are inclusive `[xmin,xmax,ymin,ymax,zmin,zmax]`. Authored entrance faces SOUTH. SOUTH=NONE, WEST=CW90, NORTH=CW180, EAST=CCW90. D1's authored EAST spatial location does not mean that its entrance faces EAST.

For authored p, world(p)=groundWorldOrigin+R(p). D1 reference corner is (−40,−24,2), local p=authored−referenceCorner. A compatible Minecraft/B2 mapping uses placementOrigin=groundWorldOrigin+referenceCorner and rotationPivot=−referenceCorner, with the same rotation for all pieces. No mirror is used. Properties `facing`, `axis`, numeric `rotation` and horizontal connection keys rotate; stair half/shape, chest handedness and waterlogging remain unchanged. Generated validation records the complete transformed palettes for every case. Block-entity positions rotate with their blocks; payloads are retained.

## Geometry and placement order

1. Proposed underground mass X/Z−40..40, Y−32..−1.
2. Main pyramid pre-carve layer Y=0..39: X/Z from −(40−Y) to +(40−Y). Named forecourt columns are intentional temple projections from this body, within the same 81×81×40 envelope.
3. Carve the P1/P2 candidate-C and selected secret cavity masks. All source concave corridors use their exact box unions, never just enclosing boxes.
4. Apply declared supports, treads, gallery/parapet elements, altar, room furniture and fixed treasury/crypt elements. One block value exists per cell; later explicit writes determine shared structural material.
5. Apply regional floors/inlays, supported lighting, treasury trims and the proposed empty treasury UI anchor.
6. Overlay every original D1 block and block-entity payload verbatim. Exclude obsolete P3 SECONDARY and all P3 reconstruction.
7. Replace exactly the 12 R1-01 cells with air. CLOSED retains all 12 separate Y−23 cover blocks. Opening the cover is a validation-only alternate state, not an implemented unlock trigger.

The JSON operation list is the exact authority candidate when a verbal summary omits a fine detail. Current numerical fields remain proposed until owner adoption. No natural registration occurs.

## Rooms and architecture

Retain source P1 S00–S10, P2 L1–L3 and P6 candidate-C adjustments. Recommend three L1 tombs and three L2 stores, retaining their source cavity dimensions and adjacencies. L1 sarcophagi and L2 shelves are located against the rear wall chosen opposite the corridor contact; their exact boxes are emitted as named operations. Each L2 store groups two non-loot vanilla flower pots on one-block cut-sandstone pedestals, alongside the rear oak-plank crate/shelf representation. Exact positions are the emitted operations. Source gallery treads, altar treads, parapets and columns are materialized, not omitted as in the coarse P6 cavity context.

Two proposed gallery piers: X−8..−6 and X6..8, Y0..4, Z−3..−1. Proposed altar pedestal: X−3..3/Y11..12/Z−19..−17. Ceremonial antechamber statues flank its clear centre: centres X−27 and −15, Z−4; 3×3 base at Y−22, 1×1 body through Y−18 and shoulders at Y−20.

The unresolved Q35/Q36 annex concept receives one OPTIONAL candidate: X−38..−33/Y−22..−18/Z−11..−3 (6×9×5), joined to A by X−32..−31/Y−22..−20/Z−8..−6. Rear offering shelf X−37..−34/Y−22..−21/Z−10; pots at (−37,−22,−4)/(−34,−22,−4). This is a proposed numerical instantiation of an existing concept, not approved addition of a mandatory room.

For each P2 non-stair air box, keep its bottom three clear layers. At higher layers, inset lateral sides by `min(max(0,y−(floor+2)),2,floor((shortWidth−3)/2))`, applied only when positive. Longest horizontal axis is longitudinal; ties choose X. Exact junction geometry is the union of these per-box masks, with no extra carving. Stair boxes keep the original full profiles; floor elevations and supporting tread mass are preserved. This defines reproducible stepped-in ceilings without narrowing approved floor-level corridor widths.

## Fixed chambers and routes

Fixed treasury: source P4-B interior X6..30/Z16..36, feet Y−30; its 8/9/10-height zones, dais, stairs and columns retain source masks. Fixed proposed crypt: X8..22/Y−30..−24/Z−7..5; sarcophagus X14..16/Y−30..−29/Z−3..1. Six niches: X9..11,13..15,17..19, Y−30..−28, north Z−9..−8 / south Z6..7. Both route variants instantiate these chambers ONCE.

Both variants share the approved cover, seven-tread FA descent and landing LA. FA footprints X−9..−3, Z33..35, feet Y−23 down through −29; each has four clear layers. LA is X−2..2/Y−30..−26/Z32..36. They then differ:

| Access | Segment | Inclusive air box |
|---|---|---|
| EAST | CB1 direct | X−2..2/Y−30..−25/Z24..31 |
| WEST | departure spine | X−2..2/Y−30..−27/Z29..31 |
| WEST | westbound | X−15..−3/Y−30..−27/Z29..31 |
| WEST | north turn | X−15..−13/Y−30..−27/Z20..28 |
| WEST | east return | X−12..2/Y−30..−27/Z20..22 |
| WEST | destination join | X0..2/Y−30..−27/Z23..26 |

WEST has a three-wide, at least three-high agent route from landing to the fixed treasury entrance; the direct EAST corridor cell (0,−30,27) is filled. EAST fills the WEST-exclusive cell (−10,−30,30). The selected WEST route deliberately shares 48 cavity cells with existing CC at the destination junction; this is the only permitted inter-component cavity overlap. No FA tread is undercut. The chamber entrance remains the existing five-wide VAULT_ENT; CC/CC3 also reach the same crypt. These route coordinates are PROPOSED; only the fixed-chamber/access-selection policy is approved.

## Materials and block entities

All states are in the generated 21-entry palette and checked against the pinned Minecraft26.3 reference extract. Full blocks have no additional properties. Sandstone mass; smooth-sandstone walking surfaces; cut-sandstone centre inlay on P2 corridor floors; chiseled-sandstone columns/statues; oak-plank crate/shelf representations; flower-pot pottery. Inlay axes/centres and all turn cells are enumerated in operations, not left to interpretation.

Fixed treasury source piles use gold_block on PILES_P and diamond_block on PILES_N. Chest presentation is a deterministic black_terracotta/gold_block sculpture, with lowered outer lid shoulders, gold pixel wings and cyan_concrete centre emblem. Column shaft layer Y−27 uses lapis_block accents. This is a block-scale visual proposal, NOT approval of final custom chest rendering or harvest/loot behavior. Its proposed vanilla UI anchor is one empty chest at (25,−26,26), facing west, single, not waterlogged, with id-only default-empty block-entity data; clear cell above it is air. It is the existing treasury-chest concept, never royal SECONDARY. D1's two block entities remain verbatim. No loot table or reward contents are generated.

Wall torches: deterministic available opposite component endpoints at feet+2, attached to the first valid supporting wall in N/W/S/E order, with matching facing. Final positions are explicit operations; validation checks support after all overlays. This is component-based endpoint placement, not a globally regular spacing pattern. D1 receives no additional light fixtures. No illuminance, mob-spawn prevention or light-level guarantee is claimed.

No intentionally randomized decoration exists. Physical pile mining and chest loot remain separate unresolved gameplay decisions; this candidate does not authorize either.

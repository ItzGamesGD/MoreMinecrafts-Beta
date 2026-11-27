# 03 — Terrain correction architecture under Policy B

Status: **analysis only. No production correction code is changed.** Options marked "owner decision" would change approved behaviour or authority and are not implemented.

## Current architecture (unchanged by B)

| Stage | Production behaviour (`0743aa74`) | Effect of B |
|---|---|---|
| Eligibility | `St01Eligibility.evaluate`: Desert centre → 484-cell Desert sweep → 6,561 heights → median `yf` → diff / cut / skirt → stub biome | only the three limits change |
| Excavation | `St01FootprintCorrectionPiece` (box Y+1..Y+39, inside 81×81 only) sets to air every non-air block from Y+1 up to `min(Y+39, max(h, Y+1) + 8)` | cut ≤ 24 ⇒ top ≤ Y+32 < Y+39: never truncated |
| Foundation | the assembly owns every footprint cell Y−32..Y0 and writes all of them | skirt ≤ 32 ⇒ the body floor is never above the lowest footprint column |
| Edge blending | **none**: nothing is ever written outside the 81×81 footprint | cliffs and exposed body faces at the edge grow with cut and skirt |
| Inputs | generator base height (pure function of the seed) and the chunk's own blocks | unchanged ⇒ order independence argument unchanged ([07](07-determinism-and-chunk-safety.md)) |

## 1. Excavation

| Metric (median plane) | Policy D (46) | Policy B (172) | B-only (126) |
|---|---|---|---|
| Excavated blocks — median | 2,927 | 5,258 | 6,712 |
| — p90 | 8,293 | 13,915 | 15,763 |
| — max | 11,332 | 28,733 | 28,733 |
| — total over 20 seeds | 170,111 | 1,187,088 | 1,016,977 |

On real generated chunks the production `correct()` cleared 1,628–17,198 blocks per B-only site, including vegetation and structure blocks ([results/correction](results/correction)). Wall time was about 3 s per site regardless of policy ([06](06-search-and-worldgen-performance.md)).

## 2. Foundation fill

The foundation is not a separate fill pass. The assembly body, solid sandstone on all four outer faces from Y−32 to Y−1 ([results/assembly-outer-faces.json](results/assembly-outer-faces.json)), replaces every footprint cell up to Y0.

| Body cells above natural ground (median plane) | Policy D | Policy B | B-only |
|---|---|---|---|
| median | 9,410 | 11,862 | 12,524 |
| p90 | 14,686 | 18,224 | 19,308 |
| max | 17,134 | 44,439 | 44,439 |

The outer faces of the underground body are fully solid, and so are the D1 royal hall's own outer walls. Exposing them opens **no room**.

## 3. Edge treatment: exposed body and boundary cliffs

| Edge metric on the one-block outside ring (median / p90 / max) | Policy D | Policy B |
|---|---|---|
| Exposed body face (outside ground below Y0) | 5 / 8 / 11 | 7 / 16 / **55** |
| Boundary cliff (outside ground above Y0) | 4 / 7 / 10 | 6 / 16 / **23** |

In the real-pipeline experiment ([07](07-determinism-and-chunk-safety.md)) the edge-ring rise above Y0 reached 21 on a B-only start and 76 next to a start that had only cut 3: a mountain flank right outside the footprint, also possible under D. The edge-ring drop reached 17.

**Entrance.** The access route opens at Y0 through a 25-block-wide opening (x −12..12) on one footprint edge, with its floor at Y−1. Under D the outside ground can be up to about 8 below that threshold; under B up to about 32. The real-pipeline experiment measured an entrance-approach rise of −10, a 10-block drop at the threshold (seed 1001, start −17,−395), against a minimum of −4 among its D-eligible starts. Policy D already accepts buried entrances (`entrance_buried` is true on 12 of the 21 experiment starts, under both policies), so blockage is not new. **Large drops below the entrance are new, and whether they are acceptable or need a ramp is an owner decision.** A ramp is new geometry.

| Edge option | Changes authority? | Status |
|---|---|---|
| Accept cliffs and exposed faces as generated | no | measurable today; visual acceptance pending |
| Edge blending ring outside the footprint (slope down or up over N blocks) | **yes**: the correction piece "never writes outside the footprint" and the overlap permission does not cover writing into neighbours | owner decision; would also need a larger piece box and a new determinism proof |
| Entrance approach ramp or steps | **yes**: new geometry | owner decision |

## 4. Ravines and caves

| Measurement on real chunks (8 targets) | Range |
|---|---|
| Open (air or fluid) cells inside the footprint at or below Y0, overwritten by the body | 12,954 – 30,793 |
| Open cells directly under the body floor (caves under Y−32) | 8 – 833 |
| Air cells on the outside ring at or below Y0 (caves or ravines touching the body wall) | 577 – 1,019 |

Ravines and caves inside the footprint are sealed by the body. Caves under the floor leave the floor overhanging a void. The body blocks have no gravity (sandstone, smooth sandstone), so nothing collapses. Caves on the ring expose the solid outer wall from below; they are visible, but do not breach. The same happens under D at a smaller scale.

## 5. Water and lava

| Measurement on real chunks | Range |
|---|---|
| Fluid cells inside the footprint (aquifer or lake water replaced by body or cleared) | 0 – 9,701 |
| Fluid cells on the outside ring at or below Y0, touching the solid body wall | 0 – 581 |
| Fluid cells on the outside ring **above** Y0 | **0 on all 8 targets** |
| Post-correction leak faces (outside fluid next to a cleared footprint cell above Y0) | **0 on all 8 targets** |

In the real-pipeline experiment, fluid inside the completion box was 0 on all 21 starts. The production check counts fluid in the one-block shell outside the underground part: 0 – 1,402 on B-only starts versus 7 – 245 on D-eligible starts. That is natural water lapping against the solid body, which Policy D already accepts as a natural irregularity.

Policy D accepts natural fluid irregularities. No leak path into the cleared volume was observed, but 8 targets is not a proof. A footprint-edge lake **above** Y0 next to a deep cut would leak into the excavation, and it would show as flowing water after generation. This is **unverified at scale** ([09](09-implementation-feasibility-decision.md)).

## 6. Protective masks and local suppression (structure overlap)

See [04](04-structure-overlap-safety-matrix.md). A mask that skips correction on cells inside vanilla piece boxes would leave vanilla blocks inside the footprint, contradicting "the assembly owns every footprint cell". Suppressing only the excavation band would leave floating vanilla remnants. Both change the approved assembly contract and are **owner decisions**. No overlap rejection rule was implemented.

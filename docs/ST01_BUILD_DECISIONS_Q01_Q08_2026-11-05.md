# ST-01 Giant Pyramid / Royal Tomb — BUILD Q01-Q36 Decision Record

Date: 2026-11-05; updated 2026-11-06
Status: OWNER-APPROVED DESIGN DECISIONS; NOT IMPLEMENTED
Base reference: checkpoint/wave16-unified-20261104
Prior design authority: docs/PYRAMID_DESIGN_PREP_2026-10-25.md
Scope: Building layout choices only; this document does not replace prior design, loot, encounter, or implementation authority.

## Owner-approved BUILD decisions

| ID | Selected | Decision | Preserved interpretation |
| --- | --- | --- | --- |
| ST01-BUILD-Q01 | B | Temple-integrated pyramid exterior | Monumental entrance, columns and Egyptian-inspired sandstone embellishment while retaining pyramid silhouette |
| ST01-BUILD-Q02 | B | Central descent stair with intermediate direction change | Wide staircase at far end of great hall, turns at landing into underground corridor; exact geometry TBD |
| ST01-BUILD-Q03 | C | Open central grand hall | Broad open center, monumental columns near perimeter, tall ceiling and visibility from upper gallery |
| ST01-BUILD-Q04 | B | U-shaped upper gallery | Gallery runs along left, right and rear of grand hall, overlooking open center |
| ST01-BUILD-Q05 | C | Rear-central elevated separate altar chamber | Short ascending stair from rear gallery to separate altar room; independent from underground descent |
| ST01-BUILD-Q06 | B | L1 main corridor with short branches | Readable central route, short side corridors to ordinary tombs and optional exploration |
| ST01-BUILD-Q07 | B | L2 distributed grave-goods storage rooms | Differently sized and shaped rooms around central route, preserving optional branches; exact count not decided here |
| ST01-BUILD-Q08 | B | L3 antechamber then large royal burial hall | Dramatic transition to boss/royal reward chamber; prior royal side annex proposals are NOT deleted |

| ST01-BUILD-Q09 | B | Square central-obelisk royal burial hall | Broad central combat area and peripheral royal architecture; dimensions pending |
| ST01-BUILD-Q10 | B | Rear raised sarcophagus dais | Royal sarcophagus on a low elevated rear platform, away from central guardian combat area |
| ST01-BUILD-Q11 | B | Primary chest near rear sarcophagus dais, secondary chest in side alcove | Two royal chests preserved; no new secondary-chest unlock or reward behavior implied |
| ST01-BUILD-Q12 | B | Perimeter colonnade in royal hall | Columns near outer walls, central guardian movement and shockwave avoidance space protected |
| ST01-BUILD-Q13 | C | Hidden descending staircase to optional secret crypt | May descend beyond previous 20-block underground limit; no forced short staircase |
| ST01-BUILD-Q14 | B | Direction-changing hidden descent with landing | Sufficient length and height difference allowed; secret crypt revealed after descent |
| ST01-BUILD-Q15 | C | Secret crypt with central sarcophagus and side burial niches | Optional separate burial zone; exact loot undecided |
| ST01-BUILD-Q16 | C | Secret treasury with central treasure dais and peripheral storage niches | Architectural display only; no new item/reward guarantee |
| ST01-BUILD-Q17 | C | Corbelled / stepped-in stone ceilings for main underground corridors | Preserve usable corridor width and headroom; special room ceilings decided separately |
| ST01-BUILD-Q18 | B | Recessed wall niches and stone statues | Carve into walls rather than protruding into corridor; maintain corridor clear width; check adjacent rooms/wall thickness |
| ST01-BUILD-Q19 | Custom: torch-centered | Warm, atmospheric torch lighting in underground corridors | User selected torch-like light sources, NOT the regular spacing pattern of option A; fixture placement and interval TBD |

## Invariants preserved from previous ST-01 design

- New Overworld desert giant pyramid, distinct from vanilla desert pyramid and separately planned Sun Temple.
- Exterior footprint 81 x 81 blocks and aboveground height 40 blocks remain approved. **Underground maximum depth of 20 blocks is RELAXED by explicit owner decision at Q13**: deeper excavation permitted as needed for architectural quality and full-length hidden stairs. New maximum depth NOT YET FIXED; do not assume unlimited excavation or silently choose a new cap.
- Aboveground architecture: monumental, elegant, symmetric, open sightlines, generous ceiling and hall space; grand hall, overlooking balcony/gallery, separate altar chamber.
- Underground: denser interconnected tomb exploration but a clearly legible main route to the royal chamber; side rooms/secrets optional, not mandatory puzzle/key gating.
- Underground encounters balanced, with lighter ordinary crypts and stronger high-value/royal areas.
- Tomb Guardian and royal chest interaction/defeat persistence requirements preserved, including reattempt after interrupted encounter and no respawn/duplicate rewards after defeat.
- D13-03-Q29 owner decision: TWO royal chests; the primary royal chest's first interaction triggers guardian encounter, and royal chest unlock follows victory. Exact placement and secondary chest behavior must be checked against full D13-03 authority before implementation.
- Prior proposal of four ordinary L1 tombs, four L2 grave-goods rooms, two royal annexes, U00-U07 spine and optional A/B/C/D branches is NOT automatically finalized by BUILD Q01-Q08.
- No loot table, recipe, boss numeric tuning, trap implementation, worldgen configuration, or structure NBT is approved or changed by this record.

## Open design/engineering items

1. Verify a dimensioned 3D blueprint: shrinking pyramid cross-section, grand hall clear height, U-gallery, altar ascent, separate descending stair and landings.
2. Validate vertical room and stair clearances and determine a justified NEW maximum underground depth after Q13 relaxation; L1/L2/L3 are conceptual zones, not necessarily three fixed-height floors.
3. Confirm underground room positions, corridor widths, and guardian arena clearance against existing boss behavior and structure constraints.
4. Reconcile historical Tomb Guardian Core recipe ingredient vs later Ancient Heart drop decision before reward implementation; do not silently create or substitute items.
5. Recover/check full D13-03 Q01-Q47 decisions before final royal chest/reward integration; only Q29=two royal chests is explicitly confirmed in this record.
6. Implement structure in Structure-Generator after blueprint approval; obtain real-client screenshots, then wire worldgen, boss, chest and loot integration with tests.

## Next design question

ST01-BUILD-Q37: next owner architectural design choice (pending).

This file records owner-approved Q01-Q36 choices, not a claim of completed construction, runtime validation, or merge. Q19 was a free-form torch preference, not a selection of A's regular spacing.

## Additional owner-approved BUILD decisions (Q20-Q36, 2026-11-06)

| ID | Selected | Architectural decision |
| --- | --- | --- |
| ST01-BUILD-Q20 | B | L1-L3 main corridor floor: central geometric inlay, outer sandstone tiles |
| ST01-BUILD-Q21 | C | Transition vestibules between L1/L2/L3; L2-L3 vestibule distinct from Q08 ceremonial antechamber |
| ST01-BUILD-Q22 | B | L1 ordinary tomb sarcophagus against rear wall; central floor clear |
| ST01-BUILD-Q23 | A | L1 tomb entrances: simple rectangular sandstone frame |
| ST01-BUILD-Q24 | C | L2 storage grouped pottery, crates and pedestals; decorative containers not automatically loot |
| ST01-BUILD-Q25 | C | L2 storage ceiling varies by room size: flat small, taller/stepped large |
| ST01-BUILD-Q26 | B | L3 royal entry corridor symmetrical monumental columns with clear center |
| ST01-BUILD-Q27 | C | Ceremonial antechamber open center, decorated royal entrance with guardian statues; no new boss trigger/loot |
| ST01-BUILD-Q28 | B | Royal hall tall four-sided corbelled ceiling; obelisk attack clearance |
| ST01-BUILD-Q29 | C | Royal hall hieroglyphic-like reliefs and occasional large guardian statues recessed/peripheral |
| ST01-BUILD-Q30 | B | Royal hall flat ceremonial central floor geometry and orderly perimeter tiles, distinguish attack telegraphs |
| ST01-BUILD-Q31 | B | Royal hall symmetrical perimeter stone braziers, outside combat clearance; corridors remain torch-lit |
| ST01-BUILD-Q32 | B | Rear royal sarcophagus dais with broad low frontal stairs; exact height/steps TBD |
| ST01-BUILD-Q33 | B | Royal sarcophagus decorated sandstone with gold-colored accents and glyph-like motifs; ornamental, no loot addition |
| ST01-BUILD-Q34 | B | Secondary royal chest in recessed side alcove with small flanking columns and upper relief; avoid colonnade/statue collisions |
| ST01-BUILD-Q35 | C | Royal annex interior style: arranged ceremonial offerings, stone shelves and pottery; annex count/location not finalized |
| ST01-BUILD-Q36 | C | Royal annex access branches off Q27 ceremonial antechamber, rather than royal hall; number/length of branches TBD |

### Interpretation and pending fit verification

- Q36=C approved. The ceremonial antechamber's side branches are a *connection concept*, not approval of exactly two annexes. The earlier two-annex arrangement remains a proposal, and final count/positions require owner approval.
- Q21 region transition vestibules are not merged with the distinct Q08/Q27 pre-royal-hall ceremonial antechamber.
- Q11/Q34 preserve two royal chests, primary near sarcophagus and secondary in a side wall alcove; chest mechanics/rewards unchanged.
- Keep central Tomb Guardian/obelisk combat space clear; check Q12 colonnade, Q29 statues, Q31 braziers, Q34 side alcove, and Q36 annex access for collisions.
- Keep exterior 81 x 81, height 40 above ground; Q13's old 20-block underground cap remains relaxed with no replacement maximum approved. L1/L2/L3 are conceptual zones, not fixed elevation layers.
- The 2026-11-06 conceptual illustration chiefly depicts L3 and is **not** a dimensioned full pyramid blueprint: aboveground grand hall, L1, L2 and deeper secret crypt/treasury are not fully represented. It must not be treated as approved coordinates, exact scale, or annex count.
- All architectural approvals remain design-only, with no structure NBT, loot, boss or worldgen implementation implied.

## Next design focus

Prioritize a full aboveground-to-secret-depth vertical topology and dimensioned 3D fit, including descent stairs, vestibules, L1/L2/L3 and hidden crypt/treasury, before adding more decoration.


## Owner-approved BUILD decisions (Q37-Q62, 2026-11-06)

All entries below are **ST-01 GIANT PYRAMID internal royal hall architecture**, not an independent royal tomb structure. Design approved only; not NBT/code implementation.

| ID | Choice | Approved decision |
| --- | --- | --- |
| Q37 | B | L1-L3 horizontally staggered zones; exact centers TBD |
| Q38 | B | L1-L2 and L2-L3 connecting stairs with broad landing and one turn |
| Q39 | B | Main underground corridor gently bends; central route remains legible |
| Q40 | Custom D | Progressively narrow main passage: 7-wide, then 5-wide royal approach, then 3-wide short threshold at exit of ceremonial antechamber |
| Q41 | B | Corridor ceiling lowers with narrowing; ceremonial antechamber and royal hall remain tall |
| Q42 | B | Final 3-wide threshold length 5-7 blocks |
| Q43 | B | Royal hall 33×33 clear internal plan (walls excluded; pillars and objects within) |
| Q44 | B | Royal hall maximum central clear height 16 blocks |
| Q45 | B | Royal hall vertical perimeter walls height 8 blocks |
| Q46 | B | Four-sided stepped corbel ceiling rising 1 vertically per 2 horizontally toward center; voxel fit pending |
| Q47 | B | Medium-spaced symmetrical perimeter colonnade; center kept open |
| Q48 | B | Pillar body 3×3 blocks |
| Q49 | B | Pillars connected by upper horizontal stone lintels around perimeter; interrupt near entrances/dais |
| Q50 | B | Pillar body setback 2 blocks from inner wall; rear passage preserved |
| Q51 | B | Ordinary runs target 6 clear blocks between adjacent 3×3 pillar bodies; exceptions near corners/doors/dais |
| Q52 | B | Corners left open, no corner pillars; terminate beams appropriately |
| Q53 | B | Low nominal 5×5 expanded pillar base |
| Q54 | B | Trim wallward base projection locally to preserve 2-block clear passage; actual footprint not symmetric 5×5 at wall |
| Q55 | B | Lotus/papyrus-inspired flared plant capital |
| Q56 | B | Capital maximum nominal 7×7 blocks; projection toward wall subject to Q57 |
| Q57 | B | Asymmetric wallward capital projection limit; avoid direct wall connection |
| Q58 | B | Capital vertical height 3 blocks |
| Q59 | A | Horizontal lintel vertical thickness 1 block |
| Q60 | B | Lintel/colonnade visually separate from stepped ceiling, no direct ceiling support; 3D fit pending |
| Q61 | B | Entire column assembly including base, shaft, capital, lintel 8 blocks high |
| Q62 | A | Base height 1, shaft height 3, capital height 3, lintel height 1 (total 8) |

### Mandatory scope clarification (owner request 2026-11-06)

- ST-01's **internal royal burial hall** is part of the giant pyramid. The **independent royal tomb/mausoleum structure** is a separate structure with separate boss, arena, treasury and reward authority. Do not interpret this document's historic title “Royal Tomb” as a reference to the independent structure.
- ST-01 Tomb Guardian, D09, D13-03-I05/I06 and royal chest rules remain ST-01-specific and MUST NOT be imported into the independent royal tomb automatically.
- Do not change any independent royal tomb design/implementation based on this ST-01 ledger. The independent royal tomb request is recorded in a separate document.
- Q43-Q62 dimensions and geometry still require full 3D fit validation (particularly column/corbel ceiling clearance). No code, NBT, worldgen or boss implementation was performed.

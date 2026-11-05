# ST-01 Giant Pyramid / Royal Tomb — BUILD Q01-Q19 Decision Record

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

ST01-BUILD-Q20: next owner architectural design choice (pending).

This file records owner-approved Q01-Q19 choices, not a claim of completed construction, runtime validation, or merge. Q19 was a free-form torch preference, not a selection of A's regular spacing.

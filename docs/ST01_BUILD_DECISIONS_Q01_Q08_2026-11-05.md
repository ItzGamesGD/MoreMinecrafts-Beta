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


## 2026-11-06 owner correction — gold golem / treasury requirements belong to ST-01

**Latest owner authority supersedes the earlier “independent royal tomb” interpretation.** The owner clarified that there is no intended separate royal tomb in this design workflow; the royal burial hall and underground complexes belong to ST-01 giant pyramid. Owner explicitly approved integration of the previously recorded gold-golem/treasury requirements into ST-01. Do NOT create an independent royal tomb as a consequence of that earlier document.

### Approved ST-01 design requirements (NOT implemented)

1. **Existing Tomb Guardian preserved.** Do not introduce or substitute a separate royal-tomb boss. Preserve its existing combat, death, drop, chest trigger, retry and defeat persistence rules, including D09 and D13-03 decisions. Any incompatibility must be audited and reported before altering authority.
2. **After legitimate Tomb Guardian victory**, process boss death and encounter completion normally; then show golden-particle disappearance / apparent transformation into a small gold golem. Prefer a distinct guide entity rather than reusing the boss entity or altering death logic. The golem bows/acknowledges the player, leads toward the ST-01 treasure room, waits when the player lags, and never makes treasury access permanently impossible if guidance fails or is interrupted. Exact spawn, routing, despawn and repeat-victory behavior remain to design/validate.
3. **Expand ST-01 treasury presentation:** investigate a dedicated large royal treasure chest; audit current ST-01 chest count, positions and rules before approving changes. Stack physical gold/diamond and other mineral blocks for a visibly rich treasure vault. Whether these blocks are mineable rewards or decoration-only is **UNDECIDED**.
4. **Candidate chest loot only:** Ethereum, Adamantium, Mithril, gold, Netherite, diamond, accessories, enhancement stones, smithing templates. Verify actual registered item IDs, existing availability and balance before selecting items, quantities or probabilities. No loot table is approved by this paragraph.
5. **No silent architectural rewrite:** Q11/Q34 two royal chests, Q16 secret treasury, Q13-Q15 secret descent/crypt, Q43-Q62 hall geometry, Q27 ceremonial antechamber and existing access logic remain intact pending explicit conflict resolution. **Whether golem guides to Q16 secret treasury or a new separate treasury is OPEN**; do not silently equate them. Existing primary chest encounter trigger and unlock behavior remain unchanged.
6. **Design integration only.** No Java/NBT/worldgen/asset/loot implementation authorized by this clarification. Verify existing GitHub design and implementation before detailed integration or any future implementation approval.

The separate file `docs/INDEPENDENT_ROYAL_TOMB_OWNER_REQUIREMENTS_2026-11-06.md` is a **superseded historical request**, not an active authority to build an independent royal tomb.


## Owner-approved BUILD decisions (Q63-Q71, 2026-11-06)

These decisions extend the ST-01 giant pyramid's existing Q16 secret treasury. They do not create an independent structure.

| ID | Choice | Approved decision |
| --- | --- | --- |
| Q63 | A | Expand the existing Q16 secret treasury as the large treasure vault; the post-victory gold golem guides the player there. No separate new room is added by this choice. |
| Q64 | B | Rear dais at the back for a dedicated large treasure chest; irregular mineral-block piles on both sides; keep a clear central aisle. The former central display concept is relocated to the rear dais. |
| Q65 | B | Vault clear internal floor plan 21×25 blocks (525 blocks²), subject to fit against existing Q16 footprint and adjacent crypt geometry. |
| Q66 | B | Maximum clear interior height 10 blocks. |
| Q67 | B | Stepped rising ceiling, with central high zone and lower side zones. |
| Q68 | A | Gentle 3-level rise: side zones 8 blocks, intermediate zone 9 blocks, central zone 10 blocks. Exact span boundaries and ceiling thickness remain TBD. |
| Q69 | B | Irregular stacked mineral-block piles on both sides; overall left/right placement remains balanced, individual piles vary in height/form. |
| Q70 | B | Central aisle minimum clear width 5 blocks; entrance and dais approach may widen. |
| Q71 | C | Rear treasure-chest dais height 3 blocks above vault floor; chest dimensions and dais footprint/stairs remain TBD. |

### Gold golem / treasure-vault flow (approved design requirements, not implemented)

- Preserve normal Tomb Guardian death, encounter completion, drops, respawn/retry and persistence rules. The guide is a separate presentation/entity concept, not a replacement boss or altered boss death handler.
- On legitimate victory, golden-particle disappearance suggests transformation into a small gold golem; it acknowledges/bows, guides toward the existing Q16 expanded vault, and waits if the player falls behind.
- If guidance is interrupted or the guide fails/despawns, access to the vault must remain possible through a non-softlocking fallback. Exact fallback route/access mechanism is still an open design decision.
- The large dedicated chest is a new vault feature; do not silently modify the two existing royal chests (Q11/Q34) or their encounter trigger/unlock rules.
- Mineral blocks are physically stacked as visual treasure. Whether they are mineable or decoration-only remains undecided. Candidate loot names are not approved loot-table entries; verify registered IDs and balance before quantities/chances.
- Existing Q16 footprint, Q13-Q15 secret descent/crypt and adjacent room geometry must be audited before claiming 21×25×10 fits. No implementation was performed.

### Superseding scope clarification

The owner's latest clarification is authoritative: **there is no separate independent royal tomb in this design workflow**. The word “royal tomb” refers informally to ST-01's internal royal burial hall/crypt. All Q01-Q71 decisions and the gold-golem/treasury extension belong to ST-01 giant pyramid. The earlier section titled “Mandatory scope clarification” above that describes an independent royal tomb is superseded where it conflicts with this correction; it is retained only as historical context. The separate INDEPENDENT_ROYAL_TOMB_OWNER_REQUIREMENTS_2026-11-06.md file is marked superseded and is not an active structure request.

### Status

Q63-Q71 are owner-approved design decisions. This is a documentation-only update. No Java code, NBT, worldgen, assets, loot tables or runtime behavior were changed. The geometric fit of Q65-Q68 to existing Q16 and neighboring underground rooms remains unverified.


## Owner-approved BUILD decisions (Q72-Q93, 2026-11-06)

All decisions below extend ST-01's existing Q16 secret treasury, not an independent tomb. These are architectural/design decisions only. Preserve Q01-Q71, existing Tomb Guardian and Q11/Q34 royal chests, vanilla chest inventory behavior, and the minimum 5-block central aisle.

| ID | Choice | Approved decision |
| --- | --- | --- |
| Q72 | B | Dedicated giant chest body footprint 5×3 blocks (front width × depth); height TBD. |
| Q73 | B | Gold-toned chest exterior with turquoise/lapis gemstone accents and royal central motif. |
| Q74 | B | Large turquoise central magical-gem seal with surrounding golden royal patterns, visual only; no additional locking mechanic approved. |
| Q75 | A | Conventional vanilla-style chest lid opening on interaction; no multi-stage seal animation. |
| Q76 | Clarified | Owner explicitly requires vanilla Minecraft chest UI and inventory interaction (original Q76 option A); external chest model/texture customization only. In a revised Q76 question, owner chose B for gently arched lid. Do NOT interpret as approval for multi-compartment custom GUI. |
| Q77 | B | Front emblem: gold winged sun disk / royal-power motif alongside turquoise central gem. |
| Q78 | B | Side surfaces carry smaller winged sun disks and royal-symbol carved reliefs. |
| Q79 | B | Stepped royal pedestal/base beneath giant chest; outer footprint clarified at Q93. |
| Q80 | B | Four decorative plant-motif columns at corners of rear treasure dais, forming a small royal sanctuary. |
| Q81 | B | Four column bodies each 3×3 blocks. |
| Q82 | B | Column overall height 7 blocks (base through capital); starting elevation resolved Q86. |
| Q83 | B | Flared open-papyrus capital; stairs/slabs may express details. |
| Q84 | B | Capital nominal maximum 5×5 blocks, with fit/clearance subject to coordinate validation. |
| Q85 | B | Rear treasure dais platform footprint 15×11 blocks; 3-block elevation from Q71. |
| Q86 | B | All four columns originate on vault floor, NOT on the raised dais. Thus column top nominally at floor+7, pending local ceiling clearance. |
| Q87 | B | Four columns partially embedded in the plan corners of the dais; dais wraps around columns; columns remain floor-founded. |
| Q88 | B | Each 3×3 column overlaps the dais corner by 2×2 blocks in plan; column projects 1 block outward in each relevant direction. No duplicate block placement at intersecting coordinates. |
| Q89 | B | Front central ceremonial stair width 7 blocks, widening from minimum 5-block clear approach aisle. |
| Q90 | B | Stair horizontal run 5 blocks to ascend dais height 3, with intermediate horizontal portions. |
| Q91 | B | Stair run divided into 2 blocks projecting in front of dais plus 3 blocks inset within its 15×11 plan. |
| Q92 | B | Giant chest centered toward rear of dais, leaving front stair-top interaction/approach area. Exact coordinates pending. |
| Q93 | B | Giant chest BODY retains 5×3 footprint; stepped base maximum outer footprint 7×5 (up to 1-block expansion per side). This is a maximum, not mandatory full-solid 7×5 at every level. |

### Required geometric and functional checks before implementation

1. Dimensioned vault plan for 21×25 internal footprint, rear 15×11 dais, 7-wide 5-long stairs (2 external / 3 inset), four floor-founded 3×3 corner columns with 2×2 plan overlap, 5×5 capitals, and rear 5×3 chest body / max 7×5 base. Ensure no overlapping physical blocks and preserve 5-wide central path and chest interaction access.
2. Validate 7-high column/capital against local stepped ceiling zones 8/9/10 high (not merely max 10), and verify chest lid opening clearance and any required visual model animation.
3. Check dais, chest pedestal and columns for overlap, rear wall clearance and chest access; no exact coordinates or chest body height have been approved.
4. Verify entire vault, hidden stair and crypt fit into ST-01's underground geometry, with Q13 depth cap relaxed but no replacement max approved.
5. Chest inventory is vanilla UI/behavior; do not create multi-compartment custom GUI. Exact container capacity, persistence and implementation method need separate engineering verification.
6. Mineral block mining/reward status and giant-chest loot table remain undecided; candidate loot is not approved.
7. Preserve boss, guide golem and royal-chest existing trigger/reward authority; no code, NBT, worldgen or runtime changes authorized here.

### Status after Q93

Q72-Q93 recorded as owner-approved design-only decisions. Architectural fit and implementation remain UNVERIFIED. The next stage should prioritize high-impact geometry, access and buildability decisions over decorative micro-choices.


## Owner-approved BUILD decisions (Q94-Q100, 2026-11-06)

| ID | Choice | Approved decision |
| --- | --- | --- |
| Q94 | B | Compress remaining decisions to high-impact architecture and verification; stop routine decorative micro-questions. |
| Q95 | B | Preserve minimum 5-block central aisle, chest approach and lid opening first; retain approved dais, stairs and columns, adjusting decoration only in remaining space. Report conflicts. |
| Q96 | B | Four floor-founded 7-high papyrus columns linked by thin overhead decorative beams, visually independent of ceiling. Beam dimensions and elevation require clearance verification. |
| Q97 | B | Extend Q37-Q39 staggered horizontal underground layout through secret crypt and Q16 treasury; actual coordinates TBD. |
| Q98 | B | Establish maximum underground depth after dimensioned 3D fit calculations. Former 20-block cap remains relaxed; no new depth cap or unlimited-depth authority. |
| Q99 | B | Sandstone variants as principal structure material, with gold, lapis-blue and turquoise accents concentrated in royal hall and vault. |
| Q100 | B | Stage 3D design and verification: aboveground, L1-L3, royal hall, secret crypt/vault, then integrated fit. Ask new questions only for demonstrated conflicts or essential missing owner decisions. |

### Stage 1: aboveground 3D coordinate design and verification

- Design/verification only. Do not change approved Q01-Q100, existing NBT, worldgen, code, loot, boss mechanics, assets or implementation files.
- ST-01 is one desert Overworld giant pyramid, NOT an independent royal tomb.
- Approved aboveground envelope 81×81 footprint and height 40. Q01 temple-integrated monumental exterior; Q02 wide descending central stair from rear grand hall with landing/turn; Q03 high open grand hall with peripheral columns; Q04 U-shaped left/right/rear gallery; Q05 separate raised altar behind gallery, not merged with descent.
- Derive a dimensioned coordinate proposal with explicit origin, datum, roof taper, wall thickness, room bounds, stair gradients and landing, gallery support, altar access, door clearance and Stage-2 underground interface.
- Mark all newly proposed dimensions PROPOSED rather than approved. Provide plan and vertical section, collision/clearance evidence, PASS/FAIL/UNVERIFIED, unresolved owner choices, and Stage-2 handoff.
- No actual voxel construction, client screenshots, NBT/worldgen or runtime tests are implied.

Q01-Q100 are now documented as design choices; physical feasibility remains UNVERIFIED.

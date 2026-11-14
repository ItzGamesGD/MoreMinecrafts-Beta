# ST-03 owner decision ledger — Q01–Q42 (2026-11-04)

> **Status: APPROVED DESIGN DECISIONS ONLY.** This document records the owner's sequential selections in conversation; it does not imply NBT implementation, in-game validation, worldgen integration, or approval of previously deferred balance parameters. This addendum supplements (and does not erase) `docs/PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md`. Where a later Q decision specifies a formerly OPEN architectural detail, use the later approved decision for that detail only.
>
> **Implementation coordinate authority:** `ItzGamesGD/MoreMinecrafts-Structure-Generator`, `tools/seam01/data/st03_placements.json` at `experiment/st03-b5a-house-a-formal`. The 112×112 perimeter, 20 facilities, fixed positions, and module bounds must not be silently changed. This ledger is not a geometry export.

## Shared architectural and implementation rules

- Architectural vocabulary: **stone-brick family + spruce and dark oak**. Stone bricks (including restrained mossy/cracked variation) for foundations, frames, corners and roof trim where appropriate; spruce for common timber; dark oak for heavy beams/doors/accents. Reference imagery is **visual inspiration, not an exact blueprint**.
- Building-specific previously approved material proportions override the general palette: notably **F05 armory is all-stone** (Q27 A); F06 has a **simple dark-oak fence** (Q36 A), not masonry posts.
- Maintain each module's fixed 3D size and placement. Do not widen porches, roofs, doors, eaves, unloading platforms or decorations outside allocated bounds; check rotations, road-facing direction and passable access in the actual layout.
- Grounding: level architectural floors, same-material footing down to natural solid terrain, localized entrance/path transitions, **no compound-wide flattening**. Preserve the approved ~4–5-block natural height-variation design direction and separately validate engineering limits.
- Usable workstations, inventory containers and loot tables must be real where specified; cosmetic crates/racks do not create extra loot rolls. No new scripted AI, training behavior, garrison respawns or premium-item rules from these design choices.
- Preserve premium reward contracts: manor **65% once per structure**, armory **35% once per structure**, commander independent **10% once on unique death**. Specific premium item eligibility, category weights, ordinary loot weights/counts and enchanted-gear policy beyond existing authority remain deferred. Do not refill rewards on chunk reload/restart/reopening.

## Owner-approved selections (exact Q numbering)

| ID | Selected | Approved scope |
|---|---|---|
| Q01 | B | Houses B/C/D are one architectural family with distinct detail. |
| Q02 | B | B: regular gable; C: asymmetric gable; D: small hip roof. |
| Q03 | B | B: covered porch within bounds; C: recessed door; D: defensive stone-framed doorway. |
| Q04 | B | B: timber/cobble base; C: timber with partial stone; D: stone lower + timber upper. |
| Q05 | B | B: larger shuttered windows; C: 2–3 small asymmetric windows; D: narrow barred windows; lanterns. |
| Q06 | B | B: two-bed communal interior; C: one bed + shelves; D: one guard bed + weapon decor; passable single story. |
| Q07 | B | Orderly, lived-in, house-specific exterior decoration. |
| Q08 | B | B/C share module family, with placement-specific cosmetic variations at F12/F15 and F13/F16. |
| Q09 | B | Level floors, stone footings to natural terrain, localized entry smoothing/path steps, no global leveling. |
| Q10 | B | Specify B/C/D palette, roofs, walls and rooms, then batch-build, real MC 26.3 screenshots and essential fixes; defer global worldgen. |
| Q11 | C | Distinct support-building identities: closed stone kitchen, stone forge/chimney smithy, open timber cart shed. |
| Q12 | B | F18 kitchen: wall fireplace/chimney, preparation counter, storage and central aisle. |
| Q13 | B | F19 smithy: open front, stone back/sides, forge/chimney, anvil and grindstone. |
| Q14 | B | F20 cart shed: front and side open, small cart model, storage/tools within bounds. |
| Q15 | B+C | Usable workstations **and** real inventory-bearing ordinary-loot containers; not merely decorative. |
| Q16 | B | Separate facility-specific ordinary loot tables for kitchen, smithy and cart shed; no duplicated reward rolls. |
| Q17 | B | Batch-build 3 support buildings; MC screenshots, workstation/container and loot checks; defer overall worldgen. |
| Q18 | C | Barracks: fortified stone/timber hybrid; stone lower + sturdy dark-oak upper. |
| Q19 | C | F02 standard symmetrical; F03 more fortified stone/defensive; F04 more residential timber/ventilation/supplies. |
| Q20 | C | Barracks one story + partial accessible loft; 1F 3–4 beds; loft 1–2 beds or storage; high central ceiling, within 13×12×11. |
| Q21 | B | Barracks ordinary food, arrows, crossbows, iron, shields; F02 balanced, F03 military, F04 household; no added enchanted-gear rule. |
| Q22 | B | Ordered military bunks with lived-in possessions; F02 standard, F03 equipment, F04 lifestyle. |
| Q23 | B | 2-block-wide central barracks entrance, stone frame + timber awning, entirely within footprint. |
| Q24 | B | Barracks-specific exterior military supplies; keep roads clear. |
| Q25 | B | F02 small regular shuttered windows; F03 narrow barred; F04 broader/loft ventilation; lanterns. |
| Q26 | B | Batch-build all 3 barracks; actual MC 26.3 screenshots and core function checks; defer global worldgen. |
| Q27 | **A** | **F05 armory heavy all-stone**, thick walls, reinforced pillars, narrow windows, sturdy low roof. |
| Q28 | B | F05 central display/storage hall, one-side maintenance workstations, rear supplies, road-facing front entrance, auxiliary entrance toward adjoining F06. |
| Q29 | B | Decorative weapon/armor displays + usable containers and maintenance; armory premium 35% **once per structure**, independent of chest count. |
| Q30 | B | Fortified stone frames, barred slit windows, lanterns, banners, crates; no locks, alarms or extra spawns. |
| Q31 | B | Finish F06 design first, then batch-build F05+F06, MC 26.3 screenshots/core checks; defer global worldgen. |
| Q32 | B | F06 divided into archery shooting zone (3–4 targets initially proposed) and melee zone; central path, armory-adjacent equipment access, outer fence. **Q33 fixes exact target count at 3**. |
| Q33 | **A** | **Simple archery range:** exactly **3** evenly spaced vanilla target blocks, marked firing line; **no added stone backstop, cover or complex firing structure**. |
| Q34 | B | Melee area: **2** hay/timber practice dummies, separately marked small sparring area, peripheral axe/gear decor; no scripted training AI. |
| Q35 | B | Packed dirt/gravel shooting ground with stone firing line; worn dirt sparring ground with stone outline; cobble/stone-brick central path and armory transition; restrained edge wear. |
| Q36 | **A** | **Simple dark-oak fence**, one internal-road entrance and one F05 connection entrance; minimal lighting/decor at this stage; **no stone pillars or low defensive wall**. Q37 adds restrained lighting and equipment decor. |
| Q37 | B | Practical decor: lanterns near entrances/corners, 1–2 illager banners by road entrance, arrow supply crate, axe/shield racks, small supplies near F05; keep central passage open. |
| Q38 | B | Batch-build F05+F06; verify F05/F06 adjacency and doors, MC 26.3 screenshots, storage/workstations/loot core checks, passable routes; **no full worldgen regression yet**. |
| Q39 | B | F07 military warehouse: more stone, thick timber supports, strong gable; F08 general warehouse: more timber, wider entrance, taller ventilated roof; common palette. |
| Q40 | B | Both warehouses: ground-floor storage + partial accessible loft. F07 central aisle, military racks and small rear loft; F08 unloading area, barrels/crates/hay and timber loft. |
| Q41 | B | Separate **ordinary** loot tables: F07 arrows/crossbows/iron/shields; F08 food/wood/coal/leather; real usable chests/barrels; quantities/weights still OPEN. |
| Q42 | B | F07 stone-brick framed **2-block double timber door** with nearby supply crates/lanterns; F08 **3-block-wide unloading entrance**, timber awning and small adjacent barrel/crate area. Both connect to passable interior aisles, stay within bounds and face existing roads as verified. |

## Fixed module dimensions and coordinates (read-only cross-reference)

From the existing Structure Generator placement file (inclusive x/z rectangles; no edits to that file):

- F05 armory: **17×12×13**, x=12–28, z=39–51.
- F06 yard: **25×5×21**, x=12–36, z=52–72. **They are edge-adjacent across z=51/52 at x=12–28**; exact walk-through door coordinate remains a building decision. Do not assume the F05 side-door orientation before verifying actual module facade/roads.
- F07 warehouse A: **15×12×13**, x=10–24, z=79–91.
- F08 warehouse B: **13×12×15**, x=75–87, z=53–67.
- F02/F03/F04 barracks: each **13×12×11**; F18 kitchen **9×11×9**; F19 smithy **9×11×9**; F20 cart shed **8×9×7**. House B **9×10×7**, C **7×9×7**, D **7×9×7**.
- Full authoritative placement and rotation list is **not duplicated** here; always read `st03_placements.json` before producing block geometry.

## Unresolved and non-implementation status

1. **Q43–Q86 are approved in the continuation section below**; Q87 onward remains undecided.
2. Exact wall/roof/block-state coordinates, entrance orientation, loft headroom, collision and screenshots require building and actual MC 26.3 checks. House A porch/terrain integration has separate prior work and is not re-approved or changed here.
3. Ordinary loot item weights, quantities, premium item pool, spawn points, biome/frequency tuning, anti-duplication implementation, terrain edge cases and whole-structure worldgen remain outside this approval.
4. This ledger is a **conversation decision record**; it is **not** evidence that Q01–Q42 have been built or tested. Future implementation must distinguish design acceptance from runtime PASS.

## Recommended next handoff

Resume **ST03-DESIGN-Q87** (gatehouse upper interior/observation/decor). Preserve earlier facility-specific batch construction and focused checks; avoid repeated broad audits.

---
## Owner-approved continuation — Q43–Q86 (2026-11-04)

This section supersedes the earlier statement that Q43 onward is unapproved. The Q01–Q42 decisions above remain unchanged. These are design approvals only, **not** evidence of construction, worldgen integration, tests, or screenshots. All existing module placements/dimensions remain authoritative except the expressly approved F01 height change. Q49 was a clarification, not a separate A/B/C selection.

### Warehouses F07/F08
- **Q43 B:** F07 2–3 small partly barred windows, wall lanterns and small loft vent; F08 2–3 medium shuttered windows, eave vents, lanterns/wood beams; no ventilation mechanic.
- **Q44 B:** F07 neat military crates/barrels, small pallet and stone/gravel approach; F08 crates, barrels, hay, loading platform and worn gravel/dirt; remain within module and preserve road.
- **Q45 B:** Build F07/F08 together; MC26.3 exterior/interior/loft screenshots, entrances, circulation, ladders, functional ordinary containers/loot and basic nonduplication. Defer whole HQ worldgen.

### Prison F09
- **Q46 B with owner correction:** Stone-heavy prison; **no upper guard loft or guard room**. Four cells intentionally replace administrative space. Limited wood only as reinforcement.
- **Q47 A:** Straight central corridor with two cells on each side = **four cells**, within 11×10×13, no guard room.
- **Q48 B:** Each cell simple bed/straw, minimal differing details, chains/iron bars; corridor lanterns; no prisoner NPC or escape mechanic.
- **Q49 clarification only:** Owner explicitly rejected any guard room or loft; do not treat as a separate selected option.
- **Q50 A:** Simple single iron entry door, stone-brick frame, two tiny barred windows, lantern above; button operates iron door, no extra guard post/awning/banner.
- **Q51 A:** Heavy stone brick with sparing cracked/mossy variation, low stone gable, minimal internal roof wood, entrance/corridor lights, no excessive trim.
- **Q52 B:** Build and screenshot F09, check four cells/corridor fit and functional iron door, defer broad worldgen.

### Ravager stable F10
- **Q53 B:** Fortified stone lower walls/pillars, dark-oak frame, spruce upper walls, high gable roof, broad entrance, 17×13×13.
- **Q54 B:** Two large side pens, straight central management aisle, individual pen entries/feeders, small rear hay/feed storage. Does not approve two actual ravager spawns.
- **Q55 B:** Stone-brick pillars/low walls, dark-oak fences, wide reinforced openings; real ravager collision/path unverified.
- **Q56 B:** Coarse/packed dirt and hay in pens; cobblestone/stone-brick aisle; feeders, rear hay/barrels/crates and hanging lanterns, no feeding mechanic.
- **Q57 B:** Large central stone-and-timber entrance targeting >=3-wide x >=4-high clearance (to verify), decorative gate with open passage, lanterns/hay/barrels, road aligned.
- **Q58 B:** 2–3 side windows per side, shutters/fences, high vents, exposed timber beams and hanging lanterns, ample headroom within height 13.
- **Q59 B:** Build F10, real MC26.3 screenshots, verify pens, corridor, actual ravager hitbox/entry/movement and no decoration obstruction; defer full HQ tests.

### Commander manor F01
- **Q60 B:** Fortified manor stone lower, dark-oak/spruce upper, high gable, banners/lanterns.
- **Q61 B:** 1F hall/guards/supplies; 2F command/war room; 3F private quarters; small lookout attic.
- **Q62 B:** Continuous side staircase 1F–3F with landings, ladder to attic; 2-block stair width is a target to verify.
- **Q63 C, explicit dimension change:** Grand double-height central hall with hanging chandelier and 2F balcony/gallery. **F01 changed from 17×23×15 to 17×27×15**; x/z unchanged. Update module definition and verify collisions before implementation.
- **Q64 B:** U-shaped 2F wooden gallery overlooking hall/chandelier; rear war room with map/war table.
- **Q65 B:** Large 3F private bedroom and open study, chest/armor stand, attic ladder.
- **Q66 B:** Symmetric dark-oak gable with central dormer/lookout window; small attic lantern/chest, timber beams, all within 27 height.
- **Q67 B:** 2-wide dark-oak front doors, thick stone pillars, projecting timber porch within bounds, lanterns, two banners, tall hall windows, direct entry.
- **Q68 B:** Tall front paired hall windows, narrow barred 1F side windows, 2F gallery windows, wider shuttered 3F windows, stone/dark-oak trim.
- **Q69 B:** Two-tier hanging chandelier on chains; wood rings ~3 and ~5 blocks across, 6–8 lanterns; verify gallery clearance.
- **Q70 B:** Symmetrical stone/stone-brick floor motif with central accent, stone-bottom/timber-top columns, banners, lanterns and timber ties; preserve walking paths.
- **Q71 B:** Commander spawns in rear 2F war room, 40 HP and vanilla AI, can reach gallery if path allows; no new combat mechanic. Independent commander 10% special drop unchanged.
- **Q72 B:** 1F ordinary supplies, 2F ordinary military containers, 3F commander rare reward chest, attic ordinary supply. **65% rare reward roll once per structure**, not 65% chest generation; independent commander death 10%; no keys/locks, no reload reroll.
- **Q73 B:** Build F01 17×27×15 and obtain real MC26.3 screenshots, check movement, chandelier/gallery/stair collision, 2F commander combat, reward once-only and common storage. Defer full HQ worldgen/multiseed/regression.

### Build, integration, roads and perimeter
- **Q74 B:** Preserve existing built/tested artifacts; build in approved facility-group batches (living/support; military/supply; special), with real screenshots and narrow core tests, then integrate whole HQ, walls/gates/roads/terrain/worldgen. Do not assume all structures are already built.
- **Q75 B:** Group completion requires actual dimensions/appearance, real MC26.3 screenshots, entrance/circulation/container and unique core function tests; fix blocking defects; record PASS/FAIL/UNTESTED. No repeated audit absent changes or new defects. Broad worldgen deferred.
- **Q76 B:** Integration planning: preserve placement coordinates, determine per-building ground height, place foundations to natural terrain, connect existing roads/entrances, locally smooth terrain, check clashes. Actual generator execution order requires code inspection.
- **Q77 B:** 1-block step/slab; 2–3 block stairs or short sloped path; 4–5 block stairs plus stone retaining wall and foundation; preserve road width and coordinates, local-only adjustment; verify ravager movement separately.
- **Q78 B:** Central military roads stone-brick/cobble, commander approach neat stone, living areas gravel/dirt/cobble, warehouse/stable rough gravel and stone, intersections stone emphasis; preserve road layout.
- **Q79 C:** **High-density military roadside decor**: lanterns, banners, crates, weapon racks, checkpoint-like barriers, hay/barrels in service areas, including residential roads; do not obstruct valid movement, add no combat or loot mechanics.
- **Q80 B:** South gate inside has small checkpoint/muster space with timber barriers, weapon racks, banners, lanterns, stone connection to central road; existing wall/towers connected visually, no new AI, locks or spawns.
- **Q81 A:** **Four independently accessible corner towers**, ladder from ground to lookout, **no required continuous walkable wall-top patrol route**; preserve wall/tower dimensions.
- **Q82 B:** Stone crenellations/arrow openings with military decorations, tower observation gaps, lanterns, banners and timber details; no automatic shooting mechanic.
- **Owner-wide stone material principle (after Q82):** For stone architecture and roads, naturally mix stone, andesite, cobblestone, stone bricks, cracked/mossy variants, etc. **No fixed mixture ratios or repeated owner approval**. Keep coherent structural edges/pillars and contextual weathering, avoid indiscriminate random noise.
- **Q83 A:** Tower 1F-to-top **direct ladder**, no intermediate floor; basic internal layout identical across four towers.
- **Q84 B:** Stone tower with dark-oak pitched roof, open observation gaps below roof, timber supports, hanging lanterns and external banners, within existing module dimensions.
- **Q85 B:** Same dimensions, height, roof and ladder for all four towers, with slight placement-specific weathering/stone texture, banner/lantern/timber accent variations; do not silently create new module dimensions.
- **Q86 B, owner override:** Massive dark-oak double-leaf gate appearance with timber/iron decorative bracing, stone/andesite pillars and upper stone gatehouse/crenellations, banners and lanterns. **Regardless of gate style, it must remain permanently OPEN, STATIC, and NONFUNCTIONAL.** No opening/closing interaction, redstone, scripted gate, or custom mechanism. Preserve unobstructed entrance and existing gate bounds; visual leaves may be represented fixed open to the sides. Verify geometry before implementation.

### Remaining design and implementation notes
- **Next design question: ST03-DESIGN-Q91**, perimeter build and targeted verification.
- This is a design ledger, not a build authorization or runtime PASS. Do not merge or modify gameplay code as part of this documentation update.
- Q01–Q90 recorded; future decisions Q91+ are not yet approved.

---
## Owner-approved continuation — Q87–Q90 (2026-11-04)

- **Q87 B — South gate upper gatehouse:** Small accessible observation room above the gate, independent internal side ladder, observation openings toward exterior approach and interior checkpoint, stone battlements, timber supports, lanterns, pillager banners, small ordinary supply chest and weapon rack. No continuous wall patrol walkway; preserve gate module bounds. Gate remains **permanently open, static and nonfunctional** (Q86 owner override).
- **Q88 B — Exterior military-garrison gate approach:** Banners and lanterns flanking gate; naturally mixed cobblestone/andesite/stone-brick paving; timber barricades, weapon racks, barrels, crates and timber stockpiles toward the sides. Keep a broad straight central passage and visual continuity with Q80 interior checkpoint. Decorative only; no path obstruction, new defense AI, gate interaction, or expansion beyond existing 112×112 perimeter.
- **Q89 B — Natural stone-reinforced wall foundations:** Partly projecting masonry footing adapting locally to slope; stone, andesite, cobblestone and mossy/cracked variants naturally mixed; fill ground-to-wall gaps with supporting masonry; short retaining support where slope is steep; preserve grass/soil and natural topography where possible. **Do not alter existing wall top elevation or placement coordinates**, do not globally flatten terrain.
- **Q90 B — Natural military-garrison exterior wall dressing:** Irregularly placed pillager banners; concentrate lanterns/timber reinforcements near gate and towers; occasional decorative wood piles, barrels and supply crates at wall base; natural rocks/grass/moss at terrain transitions. Strongest military decor near gate, comparatively restrained elsewhere. Preserve roads, passages and 112×112 bounds; decorative only, no extra spawns or combat systems.

**Next design question:** ST03-DESIGN-Q96 — real spawn and gameplay integration phase.

**Status:** Q01–Q95 owner-approved design only; this document does not certify built geometry, worldgen, screenshot or test PASS. Q96 onward undecided. Q49 was a clarification, not a separate choice. The existing filename is historical and intentionally retained to avoid moving an in-flight PR file.

---
## Owner-approved continuation — Q91–Q95 (2026-11-04)

- **Q91 B — Component-by-component perimeter construction and integrated check:** (1) wall and locally adapted stone footing; (2) four towers with roof, direct ladders and observation spaces; (3) south permanently open/nonfunctional gate and accessible upper observation room; (4) connect perimeter components and inspect routes. Capture real Minecraft 26.3 screenshots at each stage, repair significant defects, defer whole-HQ worldgen until integration; preserve existing validated work.
- **Q92 C — Detailed perimeter validation:** Individually capture interiors and exteriors of all four towers; photograph all wall segments from multiple viewpoints, all key wall/tower/gate joints and terrain/footing interfaces; inspect movement, ladders, lighting, block states and collisions; test perimeter generation on multiple world seeds/terrain conditions. Record PASS/FAIL/UNTESTED with evidence. Reuse unchanged prior evidence rather than repeat identical tests. Q92's owner-selected C scope supersedes the lighter Q91 baseline for perimeter completion.
- **Q93 B — Full architectural placement/roads/terrain integration:** Preserve the existing 34 placement coordinates and rotations, integrate perimeter and completed internal facilities, connect south gate/main roads to facility entrances, locally adapt height and terrain, check access and collisions; reflect F01's 17×27×15 size. Do not change interiors or loot rules and do not treat unbuilt facilities as completed. Gameplay/spawn validation is **not** part of Q93 architectural completion.
  - **Mandatory handoff comment:** On completion of architectural integration, the completion report **MUST explicitly recommend as the next separate task** real Minecraft 26.3 spawn and gameplay integration checks: pillager/commander/ravager spawning, movement and combat, ordinary/premium loot and nonduplication, and persistence after restart. These checks remain UNTESTED until actually performed.
- **Q94 B — Function/path first, minimal repairs:** Prioritize inaccessible entrances, blocked roads and geometry collisions; first use localized terrain adjustments and noncritical decoration changes. Preserve module dimensions and placement coordinates. If structural or placement changes are unavoidable, request owner approval instead of silently modifying them.
- **Q95 B — Architectural completion acceptance:** Verify all 34 placements' positions/rotations/dimensions, real Minecraft overview and major-facility screenshots, south gate-to-road-to-facility access, required entrances/stairs/ladders, resolved major building/wall/road/terrain collisions, and F01 17×27×15. Explicitly report PASS/FAIL/UNTESTED, including unresolved issues. Reuse Q92 perimeter detailed-validation evidence where applicable; no claim of gameplay completion.
  - **Mandatory follow-up recommendation in final architectural report:** Summarize untested or outstanding areas and **explicitly recommend launching a separate real-spawn and gameplay integration phase** (pillagers, commander, ravager, AI/movement/combat, loot generation/once-only safeguards, reload persistence, and full-worldgen interaction as appropriate). Q95 completion is **not** evidence of these gameplay checks passing. This recommendation is required even when all architectural checks PASS.

**Next design question:** ST03-DESIGN-Q96 — define scope/order of the subsequent real spawn and gameplay integration phase.

**Status:** Q01–Q95 approved as design and process decisions only. No new build, runtime verification or screenshot evidence is asserted by this document update. Q96+ undecided.

---
## Owner-approved finalization — Q106–Q108 (2026-11-05, ST03-IMPLEMENT-01)

Design/process decisions only. They do not certify built geometry, worldgen, spawn behavior or any test result.

- **Q106 — Initial garrison via vanilla entity storage:** when the structure is generated, its initial garrison is placed and saved through the vanilla entity-storage system. No bespoke spawn, tracking or persistence layer.
- **Q107 — No per-spawn identifier and no recovery system:** no individual spawn identifiers and no separate recovery/restoration system are introduced.
- **Q108 — No whole-headquarters clear state:** no separate "headquarters cleared" determination is implemented.
- **Superseded / discarded:** the earlier, more complex Q106–Q107 state-management design is **discarded**. It must not be implemented and does not override the three decisions above.
- **Unchanged rules carried forward:** 1 commander, 2 ravagers, 35–45 ordinary pillagers; the initial garrison is finite and **never automatically respawns**; no new AI, assault-state or recovery systems; rare rewards stay F01 manor 65% (once per structure), F05 armory 35% (once per structure), commander 10% (once, on unique death).
- **Research/knowledge integration:** added only if the current implementation shows an actual need; none is assumed.

**Not recorded here:** the contents of Q96–Q105 were not available in the repository or in the task text at the time of this update. They are intentionally not paraphrased or reconstructed; the owner-supplied text must be added in a follow-up edit.

**Implementation status snapshot (2026-11-05, evidence: `MoreMinecrafts-Structure-Generator` branch `experiment/st03-b5a-house-a-formal` @ `62cec3e`; this repo's checkpoint `wave16-unified-20261104`):**
- Placement table: 25 modules / 34 placements exist in `tools/seam01/data/st03_placements.json` (read-only authority).
- Built NBT: **House A (F11/F14) only**, experimental, owner visual review pending. All other facilities, the perimeter wall, towers and gatehouse have **no** built NBT: UNBUILT.
- Roads, terrain adaptation, worldgen hookup, garrison spawning and loot generation: no ST-03 code in this repo: NOT IMPLEMENTED (loot/spawn: UNTESTED).

## Owner decision — Q96–Q105 text absent; no recovery effort (2026-11-14, documentation sync)

- The body text of **Q96–Q105** does not exist in this repository (see "Not recorded here" above) and was not recovered. Those entries stay unrecorded and are not reconstructed or paraphrased.
- **Owner decision:** do not insist on recovering them; proceed by making only the design decisions that are actually needed, newly. This supersedes the "owner-supplied text must be added in a follow-up edit" note for Q96–Q105.
- Additional mob generation and design suited to the pillager headquarters (beyond the Q106–Q108 garrison rules and the unchanged 1 commander / 2 ravagers / 35–45 pillagers) is **follow-up work**, not decided here. Q96 ("real spawn and gameplay integration phase" scope/order) therefore remains an open design question to be re-asked as needed.
- Related status (2026-11-14, from the owner): ST-03 pillager headquarters has passed through structures to the walls, towers, main gate and ground road (Draft PR #55; see `docs/integration/FEATURE_STATUS_REGISTRY_20261112.md`). Design approval only; not certified here.


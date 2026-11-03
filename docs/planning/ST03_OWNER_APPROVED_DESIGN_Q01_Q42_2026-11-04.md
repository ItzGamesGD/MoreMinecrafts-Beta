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

1. **Q43 onward** (starting with F07/F08 windows, ventilation and lighting) remains unapproved. No inferred selections.
2. Exact wall/roof/block-state coordinates, entrance orientation, loft headroom, collision and screenshots require building and actual MC 26.3 checks. House A porch/terrain integration has separate prior work and is not re-approved or changed here.
3. Ordinary loot item weights, quantities, premium item pool, spawn points, biome/frequency tuning, anti-duplication implementation, terrain edge cases and whole-structure worldgen remain outside this approval.
4. This ledger is a **conversation decision record**; it is **not** evidence that Q01–Q42 have been built or tested. Future implementation must distinguish design acceptance from runtime PASS.

## Recommended next handoff

Resume **ST03-DESIGN-Q43** (F07/F08 windows/ventilation/interior lighting). After completing warehouse design, schedule F07+F08 build/screenshots and targeted interaction/loot validation. Avoid repeated broad audits or main-branch merges before the relevant modules are built.

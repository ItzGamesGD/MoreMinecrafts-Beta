# Colossal Pyramid — design preparation (2026-10-25)

> **Status: PREPARATION ONLY.** No block model, structure registration, loot table, mobs, gameplay script, test world or worldgen implementation is authorized by this document. Track as ST-01 in [STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md](STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md).

## Owner-established starting facts

- A **new, large Colossal Pyramid** in the **Overworld desert**, distinct from the ordinary vanilla desert pyramid.
- **Sun Temple is a separate selected structure**, not a room inside the pyramid unless the owner later decides otherwise.
- Project-wide dungeon philosophy favors **exploration, combat and trials**, with **no mandatory intricate puzzle / key / lever chain** as the default.
- Exact pyramid base width, height, stepped silhouette, entrance count, floor count, treasure value, mob mix, special boss, traps, biome tags and generation rarity have **not** been confirmed for this structure. Do not transplant vanilla pyramid traps or prior vanilla pyramid redesign assumptions without owner approval.

## Proposed design sequence (not yet owner approved)

1. **P1 — Scale and silhouette:** select outer footprint (e.g. candidate 65×65, 81×81, 97×97; **all illustrative**) and height; choose full solid stepped pyramid vs partially ruined exterior. Consider Minecraft 16×16 chunk boundaries and available desert terrain.
2. **P2 — Internal exploration:** choose a primary circulation model: (A) descending tomb floors, (B) rising internal galleries, (C) central shaft plus side chambers. Define whether any room is optional or hidden; avoid mandatory convoluted puzzles.
3. **P3 — Functional rooms:** decide entry hall, burial chamber, galleries, storage, collapsed sections, enemy chambers, treasury and optional secret areas. Counts and room sizes OPEN.
4. **P4 — Combat and threats:** choose vanilla mobs vs MM entities, initial garrison vs repeated spawns, traps and optional major encounter. No boss is presumed.
5. **P5 — Loot:** define expected progression tier and distinguish generic supplies, treasure chamber reward and any later MM equipment. Avoid duplicating HQ's 65/35/10 rewards.
6. **P6 — Worldgen:** terrain acceptance, foundation/fill and sand physics, partial burial, sand/sandstone palette, conflict with vanilla desert structures, seed-deterministic generation and performance tests. Do not reuse HQ's 4–5-block tolerance without separate decision.
7. **P7 — Claude/SG handoff:** only after owner WHAT approval, request architectural block blueprint with orientation/state and chunk-boundary checks; SG handles implementation and QA.

## Initial owner decision prompt (not yet answered)

**First question:** How monumental should the pyramid be? Consider (A) 65×65 relatively accessible, (B) 81×81 imposing regional dungeon, (C) 97×97 very large landmark, or owner-defined footprint. These are proposed *outer* widths, not locked dimensions. Height and underground depth are a separate decision.

**Guardrails:** The 10-candidate register is a selection inventory, not ten fully implemented dungeons. Preserve exact owner decisions as they arrive, label proposed alternatives clearly, and keep all files in Draft PR #147 until instructed otherwise.

## Owner decisions — 2026-10-25 (supersedes OPEN dimensions above)

**APPROVED:** Outer base **81×81 blocks**, above-ground exterior height **40 blocks**, maximum underground depth **20 blocks** measured from the agreed local ground reference. These are intended design envelope values; specify exact inclusive coordinate convention during block blueprint production. Do not confuse maximum depth with the height of each underground sector.

**CLARIFICATION, not yet owner-approved as an exact room ledger:** “3 above-ground + 2 underground sectors” means **five broad thematic exploration sectors**, **not** five rooms and **not** necessarily five strict architectural floors. Each sector can contain multiple rooms, galleries, staircases, optional chambers, and connecting passages. The owner explicitly wants substantial internal subdivision without a maze; the proposed ~22–30 traversable spaces and sector counts are illustrative only, not approved quotas.

**OPEN:** actual room list, layout, floor elevations, vertical circulation, voids and structural support, encounter and treasure distribution, precise 40-block height interpretation, local ground reference for -20 depth, and how much of the pyramid volume is intentionally solid. Avoid giant empty rooms merely to fill footprint. No new gameplay code or worldgen changes authorized.

## Owner-approved architectural experience — 2026-10-26 (latest authority)

**APPROVED — supersedes earlier symmetric dungeon-density proposals:** The above-ground portion should be **beautiful, elegant, grand, and architecturally legible**, not a maze-like dungeon packed with many small combat rooms. Prioritize long sightlines, generous ceiling height, monumental columns, visual symmetry, ceremonious halls, and a few memorable large spaces. The **underground** should provide the more intricate exploration: interconnected tomb chambers, branching passages, hidden treasures, optional secrets, and danger, but **not an oppressive mandatory maze**.

**Experience contrast:** Surface/upper pyramid = the ancient civilization's grandeur; underground = secrets and rewards concealed beneath it. The earlier proposed 22–30 rooms distributed across five thematic sectors is **not an approved target** and should be rebalanced away from the above-ground areas. “Three above-ground + two underground sectors” remains a conceptual grouping, **not** a required five-floor plan.

**Suggested spatial program (PROPOSED, not approved as exact rooms):** A monumental entrance/vestibule, a tall colonnaded central hall, an elegant upper ceremonial space; underground upper burial galleries and branching side crypts; underground lower royal burial chamber and concealed treasure rooms. Tall multi-level voids may replace upper-floor room counts. Keep a clear main route with discoverable optional side branches.

**Already approved dimensions unchanged:** base 81×81, exterior height 40, maximum underground depth 20 blocks. Exact interior dimensions, heights, room counts, decorations, enemies, traps and loot remain OPEN. Architectural beauty does not imply zero hostile mobs; exact combat intensity is for later owner decision.

## Owner-approved upper-level concept — 2026-10-26

**APPROVED concept:** Upper ceremonial circulation combines an **overlook balcony above the monumental central hall** with a **separate, smaller ceremonial altar chamber** connected from the balcony. Preserve views down into the great hall, architectural symmetry, an elegant and spacious ground-level route, and an intentional contrast with the denser underground crypts. Ground-level conceptual sequence: south entrance and vestibule -> grand central colonnaded hall -> far-end transition to underground monumental stair; a separate ascent accesses balcony and altar chamber. This is an architectural relationship, **not yet an approved exact floor plan**.

**PROPOSED only:** great hall nominal length 36, width 19, clear height 18 blocks; south entry axis, balcony location and stair alignments, decorative pillars, exact altar dimensions. Verify the shrinking pyramid envelope and structural clearances at each elevation before treating any numbers or coordinates as final. In a 1-inward-per-1-up 81-wide pyramid, width near elevation +20 is ~41 blocks before wall thickness; this does not by itself prove a 36-long hall can retain 18 blocks clear height along its full length.

**NEXT OWNER DESIGN DECISIONS:** approve final hall geometry and clearances; balcony elevation/extent and ascent route; ceremonial altar size; transition to underground plan. No implementation authorized by this concept decision.

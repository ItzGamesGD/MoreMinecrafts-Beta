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

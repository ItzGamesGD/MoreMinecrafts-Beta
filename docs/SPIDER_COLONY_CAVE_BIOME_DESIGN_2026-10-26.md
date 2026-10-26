# ST-04 Spider Colony Cave — design decisions (2026-10-26)

Status: OWNER APPROVED concept, DESIGN ONLY. Implementation deferred.

## Portfolio change
- ST-02 Sun Temple: **CANCELLED by owner**. Remove from active selected portfolio, retain historical ID and do not renumber other ST IDs. Any reuse of motifs requires a later decision.
- ST-04 Spider Colony Cave: active design priority.

## Approved ST-04 identity and scale
- Create a **wide, naturally connected underground biome-like environment**, comparable in experience and distribution to Minecraft Dripstone Caves.
- Owner selected **scale option A**, not a rare, finite single nest or prefabricated dungeon.
- The cave's natural terrain continuity is primary. Webs, nests and spider-colony decoration transform the cave ecology over a broad connected area.
- This is a design-level biome identity, **not yet authorization or requirement for a specific technical biome registry**.
- No standalone building, rigid corridors or mandatory maze.

## Next decisions (OPEN)
- Q2 visual direction: **OWNER APPROVED A+C hybrid** — natural cave rock and white-grey webs across ordinary areas, with egg sacs, organic nest masses and denser web clusters in concentrated colony areas. Do not make the whole biome uniformly grotesque or replace natural cave terrain.
- Web density and navigation safety; natural transition at edges.
- Fauna: **OWNER APPROVED exactly three mob types** for this cave: vanilla Spider (common), vanilla Cave Spider (more dangerous in dense nests), and one new **Mother Spider / 어미거미** (rare high-tier colony inhabitant). The prior white web spider and brown nest spider variants are rejected for this cave. Mother Spider exact size, HP, attack behavior, spawn conditions, loot, and boss-vs-elite classification remain OPEN; do not assume infinite summons. Check the existing 40-mob roster before entity ID assignment.
- Encounter intensity: **OWNER APPROVED B / balanced** — safer outer areas, moderately hazardous nest-dense areas, optional routes around fighting; no forced waves, unlimited reinforcements or inescapable cobweb corridors. A special mandatory boss or dangerous heart is NOT approved.
- Rewards, decorative blocks, generation depth, biome frequency, and cave/structure collision rules.

## Guardrails
Design only. No game code, worldgen, assets, or tests authorized. Do not merge or alter Wave 9 release-frozen PR #181. Keep ST-05 underground colosseum separate.

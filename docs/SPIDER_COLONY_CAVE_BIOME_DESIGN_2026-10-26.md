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
- Egg sacs: **OWNER APPROVED option B** — most egg sacs decorative; a minority react **only when broken** by spawning 1–2 juvenile spiders. Proximity alone does not activate them; no automatic or infinite reproduction. Exact active proportion, juvenile entity/loot and block behavior OPEN.
- Web density: **OWNER APPROVED option B (medium)**. Webs plentiful on walls and ceilings; some passage barriers but traversable natural paths remain. Avoid pervasive floor webs or mandatory cutting. Natural gradient at biome edges; exact decoration probabilities OPEN.
- Fauna: **OWNER APPROVED exactly three mob types** for this cave: vanilla Spider (common), vanilla Cave Spider (more dangerous in dense nests), and one new **Mother Spider / 어미거미** (rare high-tier colony inhabitant). The prior white web spider and brown nest spider variants are rejected for this cave. Mother Spider **combat identity OWNER APPROVED option B**: stronger melee combat plus a strictly limited number of baby-spider summons during combat, **never infinite summons**. Exact summon cap, cooldown, baby-spider entity implementation, size, HP, spawn conditions, loot, and boss-vs-elite classification remain OPEN. Check the existing 40-mob roster before entity ID assignment.
- Encounter intensity: **OWNER APPROVED B / balanced** — safer outer areas, moderately hazardous nest-dense areas, optional routes around fighting; no forced waves, unlimited reinforcements or inescapable cobweb corridors. A special mandatory boss or dangerous heart is NOT approved.
- Rewards: **OWNER APPROVED B as future design direction, but CURRENT EFFECTIVE POLICY IS A**. Vanilla resources and mob drops only; do not add Spider Cocoon or any new material until a concrete downstream crafting/use case is approved. No new recipes or special nest treasure approved.
- Generation height/frequency: **OWNER APPROVED option C** — broad underground vertical range (shallow and deep caves), low overall occurrence, but extensive naturally connected area when encountered. Exact Y bounds, noise parameters and generation rate OPEN.
- Biome borders: **OWNER APPROVED option B / gradual transition** — cobwebs and nest decorations taper in from ordinary caves and increase smoothly inward, without sharp artificial boundaries. Avoid overwriting neighboring cave-biome terrain features; exact blending implementation OPEN.
- Decorative blocks and cave/structure collision rules.

## Guardrails
Design only. No game code, worldgen, assets, or tests authorized. Do not merge or alter Wave 9 release-frozen PR #181. Keep ST-05 underground colosseum separate.

- Mother Spider spawning: **OWNER APPROVED option B** — only in rare large natural nest clusters within the spider-colony cave, not general biome-wide random spawning. Nest clusters use existing cave geometry, dense webs and egg sacs, not artificial boss rooms. Exact spawning and respawn rules OPEN.

- Mother Spider respawn: **OWNER APPROVED option A / one-time per large nest**. After its Mother Spider is killed, that nest never spawns another Mother Spider, including after reload/server restart; ordinary spiders and cave spiders retain normal spawning. Webs/egg sacs remain. Implementation must persist per-nest defeat state, but storage mechanism is HOW and not decided. Handling despawn/unloaded-before-kill and duplicate spawning requires subsequent design/QA.

- Mother Spider initial activation: **OWNER APPROVED option B** — first appears once a player approaches sufficiently close to the interior of a large natural nest; no summoning item, special egg-sac break, or boss-room ceremony. Exact detection radius and spawn location are HOW/open. The policy for retreat, chunk unload, despawn, logout or restart *before defeat* is OPEN and must not be conflated with permanent per-nest defeat.

- Mother Spider disengagement: **OWNER APPROVED option A** — once spawned, the same individual persists across player retreat, chunk unload, logout and server restart, preserving remaining health and state. No reset, disappearance or reroll on disengagement; mark nest permanently cleared **only on actual death**. Never spawn a duplicate for the same nest; persistence mechanics and edge-case QA are HOW.

- Mother Spider juvenile summoning trigger: **OWNER APPROVED option A / periodic during active combat**, not HP thresholds or random on-hit procs. Concurrent juvenile cap is mandatory; at cap no additional summon. Summoning pauses outside active combat, and reload must not bypass caps. Exact interval, cap, per-cycle count and juvenile entity identity OPEN for further WHAT decisions.

- Juvenile identity: **OWNER APPROVED option A** — Mother Spider summons smaller, lower-health/lower-damage, non-poisonous variants of vanilla regular spiders, not cave spiders and not a new standalone mob species. Retain three-species colony roster (regular spider, cave spider, Mother Spider). Exact scale, stats, persistence and implementation strategy OPEN.

- Mother Spider summoning initial balance: **OWNER APPROVED option B** — during active combat, attempt summon every 15 seconds, 2 miniature regular spiders per cycle, hard cap 4 concurrent summons belonging to that Mother Spider. Never exceed cap; if fewer slots remain, spawn at most the available slots. Vacancies can refill on later cycles; no lifetime total cap approved. These are initial balance values subject to live testing; timer behavior during unload/disengagement is HOW.

- Mother Spider base stats: **OWNER APPROVED option B** — 60 max HP (30 hearts), 6 raw melee damage (3 hearts before armor), normal-difficulty reference. No poison attack in current design. Strong elite-like encounter with periodic juvenile summoning, not a mandatory formal boss. Initial balance values, tunable after live combat QA.

- Mother Spider mobility: **OWNER APPROVED option B** — movement speed comparable to vanilla regular spider, retains wall climbing; no dash, teleport, or enhanced chase mobility. Exact movement attribute and navigation tuning are HOW, subject to live QA.

- Mother Spider defeat reward: **OWNER APPROVED option D** — enhanced vanilla-item drops (including string/spider eyes, with any rare vanilla drop subject to later approval) **plus additional XP**. No new unique materials or recipes. Exact quantities, rare item pool/probabilities and XP amount remain OPEN for further owner decisions; avoid excessive reward power for one-time nests.

- Mother Spider bonus drop: **OWNER APPROVED Q21 option C + explicit exception for one new crystal-like item**. Award vanilla cobweb blocks as additional drops, and add a distinctive spider-themed crystal drop reserved for future possible use. The earlier 'no purposeless new materials' policy remains applicable to the biome generally, but this single Mother Spider trophy/material is an owner-authorized exception. Crystal name, art, drop chance/quantity, future utility and cobweb count OPEN. Do not invent recipes or functions yet.

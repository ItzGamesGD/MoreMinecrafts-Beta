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

- Mother Spider crystal concept: **OWNER APPROVED Q22 option B** — working name `군락의 수정` (Colony Crystal), a translucent violet/purple crystal with spiderweb-like internal filaments, visually distinct from spider eyes. Exclusive Mother Spider reward concept; actual use/crafting and drop guarantee/chance/quantity are not yet approved. Cobweb blocks and enhanced XP/vanilla loot remain approved.

- Colony Crystal drop: **OWNER APPROVED Q23 option A** — exactly one Colony Crystal guaranteed (100%) on Mother Spider death, effectively one per permanently cleared large nest. No random crystal quantity or drop chance. Intended to remain usable for possible future recipes, but no usage yet approved. Loot attribution/anti-duplication and gamerule interactions are implementation/QA considerations.

- Large nest count: **OWNER APPROVED Q24 option B** — each connected spider-colony cave region contains 1–3 large natural nests, each with its own one-time Mother Spider. With the guaranteed single crystal per defeated Mother Spider, intended crystal yield is 1–3 per fully cleared colony. Exact region identification, placement spacing and chunk-border handling are HOW/QA; avoid duplicate nests on reload.

- Large nest placement: **OWNER APPROVED Q25 option B** — disperse the 1–3 large nests through suitable spacious natural cave sections with a minimum separation, avoiding overlapping Mother Spider activation/combat areas. No artificial boss rooms. Exact distance, placement algorithm, collision safeguards and validation are HOW/QA.

- Ordinary egg-sac distribution: **OWNER APPROVED Q26 option A** — distribute ordinary egg sacs relatively evenly throughout the spider-colony cave interior, not only near nests or in random concentrated patches. Preserve previously approved gradual taper at biome borders and visually denser decoration at large nests. Most sacs decorative; a minority trigger 1–2 juvenile spiders only when broken. Exact distribution probabilities are HOW.

- Egg-sac harvesting: **OWNER APPROVED Q27 option B** — Silk Touch harvests intact decorative egg-sac blocks; normal breaking yields no egg-sac item and reactive sacs can release 1–2 juvenile spiders. Silk Touch extraction is safe (no hatching), and player-placed sacs remain decorative, never providing repeatable spider farming. Exact block state/loot implementation is HOW.

- Egg-sac visual variants: **OWNER APPROVED Q28 option A** — exactly one egg-sac block model/texture, with natural placement variation only. No separate small/medium/large models or attachment-specific visual variants. Preserve previously approved Silk Touch harvest and natural-only reactive behavior.

- Egg-sac spawn surfaces: **OWNER APPROVED Q29 option A** — ordinary egg sacs primarily generate on cave floors and in floor-level rocky crevices, with very rare or no wall/ceiling placement. Keep single egg-sac visual model, colony-wide even distribution, Silk Touch harvesting and natural-only reactive behavior. Exact substrate validation and placement rates are HOW.

- Reactive egg-sac rate: **OWNER APPROVED Q30 custom value 35%** of naturally generated egg sacs reactive; remaining 65% decorative. Only ordinary breaking of naturally generated reactive sacs releases 1–2 miniature spiders. Silk Touch harvesting does not hatch them; player-placed sacs are decorative and non-reactive. No proximity activation or renewable spider farming.

- Egg-sac juvenile aggression: **OWNER APPROVED Q31 option B** — juveniles hatched by breaking natural reactive sacs follow vanilla spider light-dependent hostility (hostile in darkness, normally neutral in sufficient light; retaliate when attacked), with reduced juvenile stats and no poison. Distinguish from Mother Spider combat summons, which retain their summoner's combat-target behavior. Exact vanilla AI reuse and edge cases are HOW.

- Egg-sac juvenile despawn: **OWNER APPROVED Q32 option A** — juveniles spawned from reactive egg sacs follow vanilla hostile-mob natural despawn rules, with no fixed 60-second timer and no forced persistence. This does not change the Mother Spider's approved persistence across unloads/restarts. Summoned juveniles' independent lifecycle remains subject to separate design decisions.

- Mother Spider summoned-juvenile despawn: **OWNER APPROVED Q33 option A** — summons follow vanilla natural hostile-mob despawn rules, just like egg-sac juveniles. No forced despawn at combat end or Mother Spider death. Despawned/killed summons no longer count toward their mother's maximum four concurrent summons; during active combat, summon cycles remain every 15 seconds with up to two juveniles per cycle. Mother Spider persistence is unaffected.

- Juvenile loot: **OWNER APPROVED Q34 modified vanilla-spider drop types at reduced probabilities and Q35 option B** — each juvenile has a 50% chance to drop exactly one string, a 15% chance to drop exactly one spider eye, and grants 2 XP on eligible kill. Applies to both egg-sac juveniles and Mother Spider combat summons. Looting enchantment interactions and potential renewable-farm balance are HOW/QA and not approved extra bonuses.

- Juvenile natural spawning: **OWNER APPROVED Q36 option A** — miniature spiders do not spawn naturally in the biome, at large nests, or elsewhere. Only two sources are permitted: breaking a naturally generated reactive egg sac (1–2) and Mother Spider combat summons (up to 2 per 15 seconds, maximum 4 concurrent per mother). Regular vanilla spiders and cave spiders remain the naturally spawning species.

- Natural spider species ratio: **OWNER APPROVED Q37 option A** — relative spawn weighting among vanilla spider and vanilla cave spider in the colony is **80:20**. This does not prescribe their percentage among all hostile mobs or total spawn density. Juvenile spiders remain non-natural spawns; Mother Spiders remain one-time large-nest encounters.

- Other hostile mob natural spawning: **OWNER APPROVED Q38 option A** — within the spider-colony cave's own ordinary natural hostile-spawn roster, allow only vanilla spiders and vanilla cave spiders at the approved relative 80:20 ratio. Exclude ordinary natural zombie, skeleton, creeper and other non-spider hostile spawns. Do not forcibly remove mobs entering from outside or spawned by unrelated structures/spawners; preserve special Mother Spider and juvenile-spider appearance rules.

- Natural spawn density: **OWNER APPROVED Q39 option B** — target roughly **1.5x the ordinary cave's hostile-mob spawn density/frequency** within the spider-colony cave, retaining the approved spider:cave-spider 80:20 weighting and spider-only ordinary natural hostile spawn roster. Do not increase or bypass vanilla global/local mob caps. This is a target to validate empirically under comparable conditions; exact biome spawn settings and measurements are HOW/QA.

- Natural lighting: **OWNER APPROVED Q40 option A** — preserve natural cave darkness throughout the spider-colony biome. No new emissive decorations, glowing egg sacs, or special large-nest lights. Player-placed light sources and existing natural cave light sources (e.g., lava) continue to function normally. No forced darkness mechanic.

- Cave floor composition: **OWNER APPROVED Q41 option A** — retain vanilla cave floor substrates (stone, deepslate, gravel, etc.) without replacing them with new spider-themed floor blocks or introducing floor-specific slow effects. Previously approved egg sacs and cobwebs may still decorate suitable surfaces, and large nests remain integrated into natural cave geometry. No dedicated nest floor block.

- Ore generation: **OWNER APPROVED Q42 option A** — retain vanilla ore-generation rules, ore distributions and height dependencies in spider-colony caves. No colony-specific ore multiplier, additional iron/copper deposits, or special ore clusters near large nests. The colony is distinguished by ecology and exploration, not mining yield.

- Treasure chests: **OWNER APPROVED Q43 option A** — do not generate any spider-colony-specific treasure chests, either in ordinary cave areas or large nests. Exploration rewards remain vanilla resources, spider drops, collectable webs/egg sacs and one guaranteed Colony Crystal per defeated Mother Spider. Do not remove or alter pre-existing/overlapping unrelated structure chests.

- Water and lava generation: **OWNER APPROVED Q44 option A** — preserve vanilla water and lava generation/distribution in spider-colony caves, with no suppression, replacement or forced drainage. Place egg sacs and cobwebs only where valid without conflicting with fluids, and choose suitable navigable natural areas for large nests rather than rewriting fluid features. Exact fluid-safe placement validation is HOW/QA.

- Vertical generation range: **OWNER APPROVED Q45 option B** — spider-colony cave biome may generate at Y=-48 through Y=48 inclusive, covering shallow and deep underground cave systems. Preserve low overall frequency, connected large regions, gradual biome transitions and natural cave geometry. This is an eligibility range, not a promise of uniform generation probability at every Y. Exact generation algorithm and tests are HOW/QA.

- Height-based generation weighting: **OWNER APPROVED Q46 option B** — within the approved Y=-48..48 eligibility range, spider-colony cave generation likelihood peaks near Y=0 and gradually tapers at both higher and lower elevations. Maintain low overall rarity and large connected cave regions. Exact weighting curve and noise implementation are HOW, subject to validation; no guarantee of uniform distribution.

- Horizontal colony scale: **OWNER APPROVED Q47 option B** — target an irregular, connected natural cave colony spanning approximately 128–256 blocks horizontally, not a fixed square or rectangular structure. Preserve 1–3 sufficiently separated large nests, gradual boundaries, Y=-48..48 eligibility and Y≈0 peak weighting. Exact region shape/measurement, connectivity and generation performance are HOW/QA.


## Q48-Q66 owner decisions (2026-10-27) — latest authority where conflicting
These are design decisions only. No gameplay implementation or merge authorization. Earlier statements remain historical; explicit conflicts below require reconciliation rather than silently erasing prior approvals.

- **Q48 C, very rare:** Colony caves are optional secondary exploration content, not mandatory progression. Initial HOW proposal: evaluate one candidate per 32x32-chunk (512x512-block) region with 12.5% candidate probability, minimum colony-center spacing 768 blocks; require connected natural caves, viable nests and collision checks. Candidate density is approximately one per 2048 chunks (2,097,152 square blocks) before rejection and spacing, **not actual realized frequency**. Use deterministic region priority/neighbor exclusion independent of chunk generation order; benchmark and tune before implementation. Preserve Y=-48..48 with peak weighting near Y=0. This numerical algorithm is an initial design proposal, not independently owner-approved final parameters.
- **Q49 B:** Harvestable Colony Crystal deposits cluster near large nests, with sparse deposits at outer colony areas.
- **Q50 A:** Naturally generated harvestable crystal deposits do not regenerate.
- **Q51 B:** Natural spider spawning across colony plus ordinary spider spawners at large nests.
- **Q52 A:** Ordinary natural hostile spawn roster restricted to vanilla spider and cave spider; outside entrants unaffected. This is consistent with earlier Q38. Mother Spider remains a separate approved rare nest encounter, not removed.
- **Q53 A:** 1-2 vanilla regular-spider spawners per large nest; no cave-spider spawners. Cave spiders spawn naturally.
- **Q54 B:** Medium cobweb density, increasing toward nests; preserve traversable natural routes.
- **Q55 B:** Enhance natural habitat with web masses, hanging cocoons, bones and prey remains without replacing the cave with a built dungeon.
- **Q56 C:** Some breakable cocoons release spiders, others give small loot; no proximity activation or renewable farming.
- **Q57 A:** Dangerous and loot-bearing cocoons are visually indistinguishable.
- **Q58 A:** Dangerous cocoons release 1-2 **regular vanilla spiders only**, not cave spiders.
- **Q59 A:** No colony-specific treasure chests; primary rewards are crystals and cocoon drops.
- **Q60 B:** Harvestable crystal ore requires iron-tier-or-better mining capability. Wooden, stone and gold pickaxes cannot yield crystal items.
- **Q61 A:** Crystal ore follows vanilla-style Fortune-enhanced drops and Silk Touch block harvesting; exact base count/loot table to be tested, not fabricated.
- **Q62 B:** Crystal ore emits faint light, subject to maintaining viable natural spider spawning in nest regions.
- **Q63 A:** Preserve natural cave connectivity; no artificial entrances or forced connecting corridors.
- **Q64 A:** Gradual transition of cobwebs, cocoons and debris at colony boundaries.
- **Q65 B:** Large nests partially expand existing natural caves where necessary, with irregular shapes and preserved major passages.
- **Q66 B:** Each large nest targets approximately 20-36 blocks horizontal diameter, not a perfect circle.

### Explicit reconciliation required before implementation
1. Earlier Q21-Q23 designate **one guaranteed Colony Crystal on each one-time Mother Spider death**, previously exclusive to Mother Spider. Q49-Q61 now also approve mineable Colony Crystal deposits. **Preserve the guaranteed Mother Spider drop**, remove only the earlier *exclusivity* assumption; do not infer total crystal yield is still 1-3 per colony. Exact crystal ore deposit quantity and economy impact remain open.
2. Earlier Q40 forbids new emissive decorations; Q62 explicitly supersedes that prohibition **for faintly luminous crystal ore only**. No blanket permission for glowing webs or egg sacs. Validate light levels against hostile-spawn mechanics.
3. Earlier Q26-Q36 approve ordinary floor egg sacs, 35% reactive, yielding 1-2 **miniature spiders** when broken, with Silk Touch and juvenile loot rules. Q55-Q58 introduce **hanging cocoons** and specify 1-2 **regular vanilla spiders**. Treat hanging cocoons as a **distinct feature** from ordinary floor egg sacs unless owner later chooses to unify them; retain the earlier egg-sac decisions. Cocoon loot rates, hazard probability and exact visuals remain open.
4. Earlier Q2 and Q28 describe egg sacs; hanging cocoons are a separate decoration type approved by Q55. No new monster species authorized.
5. Existing Mother Spider combat, one-time spawning/persistence, 60 HP, summoning and guaranteed reward remain intact; Q52 only governs ordinary natural hostile spawns.
6. Do not equate initial candidate generation density with measured realized colony density. Validate minimum spacing, natural-cave fit, chunk order independence, biome boundaries, ore quantities, lighting and worldgen performance.

Status: **DESIGN ONLY**. Do not change game code, assets, tests, release-frozen PR #181, or main. Implementation requires separate authorization.


## Q67-Q70 owner decisions (2026-10-27)
- **Q67 B — Large nest vertical scale:** Target approximately 12-24 blocks of internal height, naturally irregular rather than a fixed-height chamber. Horizontal extent remains Q66's approximately 20-36 blocks. Preserve cave continuity and validate navigability and Mother Spider combat space.
- **Q68 B — Natural interior terrain:** Retain rocky protrusions, small ledges, sloped floors and uneven ceilings. Avoid forced artificial multi-level mazes, impassable paths and mob-navigation traps. Partial expansion under Q65 remains permitted.
- **Q69 B — Cobweb placement:** Concentrate webs on walls and ceilings, with some web obstacles in rocky gaps and selected passages. Preserve navigable primary routes and allow removal or bypass. Maintain Q54 medium overall density and Q64 gradual outer transitions.
- **Q70 B — Spider spawner distribution:** Place the previously approved 1-2 **vanilla regular-spider** spawners per large nest at separated suitable locations such as near natural rock protrusions or walls, rather than clustering them centrally. Do not add cave-spider spawners; avoid blocking Mother Spider encounter area and key routes. Exact spacing, valid placement and spawn-volume checks are implementation/QA.

These decisions are **DESIGN ONLY**. No game code, worldgen, assets, tests, merges, or changes to release-frozen PR #181 authorized.


## Q71-Q79 owner decisions (2026-10-27)
- **Q71 B — Mother Spider first appearance:** On player approach into a large nest, choose a suitable spacious position near its interior/center with valid navigation, not an arbitrary fixed coordinate. Preserve one-time per-nest activation, persistent individual and no duplication.
- **Q72 B — Natural pursuit:** Mother Spider may pursue players through connected natural caves under ordinary mob AI aggro, distance and pathfinding limits; no hard nest boundary or colony-wide forced pursuit.
- **Q73 B — Return home after disengagement:** Once combat/aggro ends, the same Mother Spider attempts to return to its original nest. No HP reset, respawn, or despawn; retain saved individual state across unload/restart.
- **Q74 A — Normal combat AI takes priority during return:** Detect and fight new targets or retaliate against attacks while returning, then resume return after combat ends. No invulnerability or scripted teleport.
- **Q75 A — No bespoke passive regeneration:** No automatic Mother Spider HP recovery from elapsed time, disengagement or returning to nest; damage persists. Ordinary external healing effects are not categorically prohibited.
- **Q76 A — Natural pathfinding for return:** Use normal movement/wall-climbing to find a route. If obstructed, remain in a reachable place and retry later; never teleport home or destroy blocks to clear a path. Exact retry mechanics are HOW.
- **Q77 C — Large body size:** Target Mother Spider visual size approximately **2.3 times** a vanilla spider. Actual entity collision box, corridor clearance, wall climbing, pathfinding and combat reach require implementation validation; do not silently reduce the owner-approved visual scale.
- **Q78 B — Distinctive shape and markings:** Enlarged abdomen with white/off-white spiderweb-like markings across back and abdomen; recognizable vanilla-inspired spider silhouette, without extra armored spikes or exaggerated fantasy-boss protrusions.
- **Q79 B — Base coloration:** Very dark violet/purple mixed with black; contrast against approved white/off-white web markings. Keep muted, cave-natural coloration, not neon purple. Visually relates to previously approved purple Colony Crystal but does not imply new abilities.

All Q71-Q79 choices are **OWNER APPROVED DESIGN ONLY**. Do not implement code, assets, worldgen or tests, do not merge, and do not alter PR #181. Existing Mother Spider stats (60 HP, 6 melee damage), summon limits, one-time death, guaranteed crystal drop and persistence remain unchanged.

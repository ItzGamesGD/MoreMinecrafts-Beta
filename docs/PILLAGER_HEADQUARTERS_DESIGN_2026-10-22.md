# Pillager Headquarters — owner-approved concept and proposed detailed layout (2026-10-22)

> **Status: DESIGN ONLY.** Separate from structure-generation implementation and from the smaller vanilla pillager outpost or patrol variants. No blocks, loot tables, generation settings, custom mobs, AI, world files or code are implemented by this document.
>
> **Authority key:** **APPROVED** = directly selected by owner in current conversation. **PROPOSED** = designer elaboration for review. **OPEN** = requires specific future owner decision or generated-world verification. Do not accidentally promote PROPOSED building counts, dimensions or balance to approved requirements.

## Owner confirmation addendum — 2026-10-22 (later authority)

**APPROVED — footprint:** The pillager headquarters uses a **112 × 112 block outer square** (12,544 horizontal blocks² including the walls and towers). This is the overall outside footprint, **not** an additional 112 × 112 buildable interior. The perimeter remains a geometrically precise square with four corner watchtowers and a single main gate. Do not infer that 112 is the inner courtyard dimension.

**APPROVED — settlement density direction:** The inside should be a **plausibly populated ominous military village**, not an empty parade ground with only eight large standalone buildings. Distribute **additional small pillager houses and supporting outbuildings** between the key military facilities to fill the compound organically. The **exact number, footprints and placements** of these small buildings remain PROPOSED until the block-coordinate plan is reviewed.

**PROPOSED sizing baselines retained for further review, NOT new owner approvals:** wall height ≈8 blocks; tower footprint ≈11 × 11 blocks; central gate width ≈7 blocks; two or three ≈13 × 11 barracks; command hall ≈23 × 19; armory ≈17 × 13; fenced training yard ≈25 × 21; supply warehouse ≈15 × 13; prison ≈13 × 11; ravager stable ≈17 × 13. These estimates must be checked against the usable interior left by 112 × 112 external walls, road widths, corner tower overlap and gatehouse. Do not interpret rough area examples as final block placements.

**Still APPROVED:** flat plains and forests with flat ground under the trees (option A); do not make stepped terrain-adaptive wall segments. Architectural direction is the **A+C hybrid** with relieved 3D walls and wall-integrated corner towers. Exact block palette, wall/tower/gate dimensions, terrain thresholds, building count, combat roster and rewards remain OPEN.

---

## Owner-approved architectural identity — 2026-10-22

**APPROVED concept, not a block-by-block blueprint:**
- Architectural direction is a **blend of option A (grounded realistic castle wall) and option C (heavy military fortress)**: substantial stone-built defenses with rough, utilitarian **vanilla pillager outpost** detailing in dark oak and banners. It must feel intimidating yet Minecraft-native, not an ornate fantasy palace.
- **Walls require clear three-dimensional relief, not flat vertical planes.** Vary depth using a substantial foundation/buttress rhythm, recessed wall panels or arrow slits, and pronounced top battlements / walkways. Exact extrusion depths, brick variants, dimensions and repeating cadence are **OPEN** for visual design.
- The four corner watchtowers are **structurally continuous with the walls**: two perpendicular wall segments flow into each tower, and wall-top patrol access should visually/physically connect to the tower where feasible. Do not render four unrelated towers standing beside the wall.
- Preserve the **112×112 inclusive outer square**, the four integrated corner towers, the **single main gate**, and the flat-ground-only plains/forest placement rule; avoid adaptive stair-stepped perimeter walls.
- **Defer actual architectural modeling** (stairs/slabs/fences, window rhythm, tower silhouette, wall section and gate detailing, palettes and exact dimensions) to a later **Claude-led visual/architectural design pass**. Previous wall height ~8, tower ~11×11 and height ~16, thickness ~3, and gate width ~7–8 are merely exploratory estimates, **NOT approvals**. No requirement to create a wall prototype now.

**Next workstream:** shift to **interior militarized-village layout and gameplay concept**, while leaving detailed external architectural styling on hold. Unapproved building counts, enemy roster, loot and precise internal coordinates remain proposals.

---

## Owner-approved interior concept — 2026-10-22 (latest authority)

**APPROVED:**
1. Interior facility density **approximately 19–22 distinct buildings/facilities**, yielding a balanced, inhabited military village, not a sparse empty courtyard or a tightly packed metropolis.
2. The **headquarters / command hall is a two-story illager manor-style building**, visually the most important internal structure, not a three-story military command tower.
3. Road topology is **single main gate → central square → two-story headquarters along an immediately legible primary street**, with several smaller connecting village-like alleys. The street is the orientation spine; do not impose a rigid square city grid or random maze.
4. **Both a prison and a ravager stable/pen are included** as distinct interior facilities.
5. Previously approved: armory and fenced training yard share a direct boundary; surrounding square wall is 112×112 external, four integrated corner towers, single main gate, flat plains/flat-under-tree forests. Detailed A+C hybrid exterior styling remains deferred to Claude.

**Authority distinction:** the following **20-facility ledger, number of homes/barracks, zones, encounter flow, road widths and placement logic are DESIGNER PROPOSALS** consistent with the above approvals; they are not separately approved as exact recipes or block placements. One fenced training yard counts as **one facility**, even though it is an outdoor space; perimeter wall, corner towers, gatehouse, connecting streets and central public square are **shared infrastructure**, not counted among the 20 interior facilities.

### Proposed 20-facility program (checkable sum)

| Facility type | Proposed count | Use and important adjacency |
|---|---:|---|
| Two-story illager manor headquarters | **1** | Far visual anchor of the main street beyond the plaza; ground-floor planning/guard rooms, upper-floor commander chambers. Not a confirmed scripted boss arena. |
| Barracks | **3** | Medium communal sleeping houses grouped on secondary streets near training and plaza, with differing silhouettes. |
| Armory | **1** | Equipment racks, crossbows, axes, repairs; **directly borders the fenced training yard**; a door/path connects without intervening buildings. |
| Fenced training yard | **1** | Outdoor targets, drill and sparring zone; internal fence gate(s) are allowed and **are not fortress perimeter secondary gates**. |
| Supply warehouses | **2** | One near gate/logistics lane, the other near barracks/HQ for supplies; no required conveyor or quest delivery mechanic. |
| Prison | **1** | Defensible side street, barred cells, little overlap with residential courtyard; captives are not a confirmed spawn rule. |
| Ravager stable/pen | **1** | At wider street and a courtyard adjacent to gate-side logistics, enough room for animal pathing; live ravager count is OPEN. |
| Small illager residential houses | **7** | Groups of 2–3 interspersed with barracks and workshops; varied façades and yards avoid monotonous identical rows. |
| Support outbuildings | **3** | Suggested communal kitchen, smith/repair workshop, and covered cart/tools shed; exact functions may be adjusted. |
| **TOTAL** | **20** | Inside approved **19–22** target; 20 excludes shared walls/streets/plaza/corner towers/gatehouse. |

### Proposed internal zoning / relationships

The schematic below expresses **adjacency and navigational order**, not a final physical layout. The main gate is drawn at south **only as an illustrative rotation**: the entire site can be rotated, with no additional exit.

```text
 NORTH WALL ============================================ NORTH WALL
 | NW WALL-INTEGRATED TOWER       NE WALL-INTEGRATED TOWER  |
 |      residential cluster       barracks + secondary store|
 |          [H H H]                 [B B]      [W2]         |
 |                     [HQ: 2 floors]                     |
 |                           ||                            |
 | [prison]----small lane--[CENTRAL SQUARE]--[houses H H]  |
 |   [P]                     ||                   [H H]     |
 | [house H]     [ARMORY][FENCED TRAINING YARD] [barrack B] |
 |                    shared boundary                     |
 | [kitchen]  [repair shop]    ||    [ravager stable]       |
 |     [shed]           [warehouse W1]         [R]        |
 |                           ||                            |
 | SW WALL-INTEGRATED TOWER  MAIN GATE  SE WALL-INTEGRATED  |
 SOUTH WALL ===================       ================= WALL
```

**Diagram is intentionally non-metric**; every facility in the ledger must be packed and validated independently. Some roads and house locations must shift when real footprints, tower reservations and wall thickness are decided. In particular, the armory–yard boundary and fully traversable gate→square→HQ route are hard conceptual constraints; diagram symbols are not build instructions.

### Proposed road and service hierarchy

- **Primary road** leads from the only gate to the central square and then the manor's entrance; align major vistas but allow setbacks, courtyards and architectural offsets so it is not a giant empty parade road.
- **Secondary lanes** reach barracks and the two residential clusters, feed warehouses, and connect prison, training, stable and outbuildings. Loops and short connectors are preferable to inaccessible cul-de-sacs, but exact road graph is OPEN.
- **Armory + fenced yard** forms a single contiguous training compound; the yard's own fence gate is an **internal facility door**, not a second perimeter entrance.
- **Stable** sits near a broader gate-side route, away from narrow housing alleys; exterior perimeter still has just one gate.
- **Prison** is reachable from a side lane, not directly in the middle of the residential cluster.
- **Two-story manor** retains clear silhouette from the gate or the central square, with lower, smaller houses around it.

### Proposed military-village experience (non-linear)

| Area | Discoverable activity | Potential opposition / tension | Completion or reward contract |
|---|---|---|---|
| Outer wall, single gate, four integrated towers | Observe patrols and defenses, pick an approach | Ranged guards / line-of-sight pressure (roster OPEN) | No forced stealth, gate-switch puzzle or indestructible wall |
| Logistics and stable quarter | Search stores, stables and cart shed | Pillager patrols; ravager **only if later approved** | Optional exploration, no mandatory stable event |
| Houses and barracks | Explore a recognizable ominous village, find everyday details | Mixed short alley and building fights | Ordinary loot candidate, no locked gate requirement |
| Armory and directly adjacent training yard | Discover equipment/targets, use cover and fence geometry | Potential veteran/training enemies, exact rules OPEN | Higher risk/reward candidate, no obligatory wave trigger |
| Prison and side quarter | Explore cells and guard room | Local guard presence | Captive rescue mechanic **not approved**, no forced prisoner objective |
| Central square and two-story HQ | Visible landmark, explore planning rooms and command chambers | Higher-ranked defenders and captain as **PROPOSED** only | No approved boss/reward rarity; open access, not a prescribed last step |

The **natural danger gradient** may rise from gate to command hall, but is not a forced progression sequence. Minecraft-native wall scaling/mining and roaming routes remain legitimate; do not create required keys, irreversible fights, special switches or magic barrier logic.

### Concept-level validation before block plans / implementation

- Facility ledger totals 20 and all independently countable objects respect 19–22 density; walls/towers/gate/plaza/roads are not counted as additional buildings.
- Reserve full extents for walls/corner towers/gate, primary road and central square **before** placing the 20 facilities. Actual building footprints, road widths, fire clearance and pedestrian pathing remain OPEN; no claim that a complete 20-building packing proof was run.
- Keep armory and fenced training yard adjacent, while barracks and equipment workshop have straightforward access.
- Preserve a continuous gate→square→manor route; at least one accessible lane leads to each major facility and the stable is not trapped behind narrow alleys.
- Include a prison and ravager stable without claiming their entity spawn counts or special behavior.
- Maintain meaningful gaps/courtyards, readable housing clusters, and a convincing military-village atmosphere; avoid completely uniform village-grid rows.
- This is **documentation only**, with no generated build, Minecraft client screenshots, implementation, balance test or terrain-compatibility acceptance.

**Next design topic:** flesh out each building's room program and reward identity, decide exact combat stats and loot separately, then integrate a tested finite-garrison implementation plan; hand off exterior 3D stone/dark-oak detailing to Claude as separately requested.

---

## Owner-approved garrison/combat policy — 2026-10-22 (latest authority)

**APPROVED, overriding earlier 26–34 / local-alert / partial-respawn proposals:**
- **35–45 initial stationed hostile entities** for the entire 112×112 headquarters (large-garrison choice). This is the **total intended first-clear garrison**, not simultaneous active attackers and not a per-zone count to add on top.
- **Vanilla mob combat/targeting AI only.** Do **not** add a headquarters-wide alert, adjacent-zone reinforcement chain, aggro propagation, scripted return-to-post AI, patrol pathfinding system, mob command/shout abilities, or a new behavior tree. The ordinary Minecraft AI for pillagers, vindicators and ravagers handles detection, target choice and chasing. Standing/spawn positions provide spatial identity.
- **Commander = enhanced Vindicator-based officer** in the two-story 17×15-block-proposed headquarters. Enhancement may be through basic attributes, equipment and visual identity (specific values OPEN), **not** custom summons, multi-phase boss skills, commander buff aura or military orders. Vindicator melee behavior remains vanilla.
- **One-time garrison:** soldiers defeated as part of the structure's initial garrison do **not** naturally respawn as that structure's soldiers. The structure must not have replenishment spawners or scheduled restocks. The initial survivors must be persistent until genuinely eliminated (see persistence contract below).
- Other owner approvals remain: square 112×112 including walls, exactly four integrated corner towers, one main gate, flat plains or sufficiently flat forest ground; 19–22 internal facilities; two-story illager-style small manor; main street gate→square→manor plus alleys; prison and ravager stable, armory directly adjoining fenced yard; A+C hybrid 3D fort wall styling delegated to Claude.

### Proposed nominal garrison — exactly 40 entities (counts not individually approved)

Choose the **40-defender example** inside the approved 35–45 range as the balance baseline. Every table here counts the **same** 40 entities, not additional independent groups. “Elite” is a better-equipped/stat-tuned **Vindicator** using vanilla melee AI, not a new mob with a special behavior script.

| Type | Nominal count | Role |
|---|---:|---|
| Standard pillagers | 23 | Crossbow guards at wall, lanes, storehouses and square |
| Standard vindicators | 10 | Melee guards at alleys, prison, internal rooms |
| Elite vindicators | 5 | Better-equipped close defenders in training and headquarters |
| Commander, enhanced vindicator | 1 | Unique named officer in upper manor; **included in 40**, never extra |
| Ravager | 1 | Stable compound threat with vanilla AI |
| **TOTAL** | **40** | Counts satisfy approved 35–45 |

**Variant target:** 35–45 total may be achieved by varying **ordinary pillager/vindicator headcounts by up to ±5 combined** around the 40-defender baseline while preserving 5 elite vindicators, 1 commander and 1 ravager in this **proposed** variant. Do not independently randomize each zone without checking the whole-structure cap; the exact probability distribution and ravager number are still OPEN.

### Proposed zone ledger — same 40 units, no double counting

| Zone | Pillager | Vindicator | Elite vindicator | Commander | Ravager | Subtotal |
|---|---:|---:|---:|---:|---:|---:|
| Single gate, wall walkways, four towers | 8 | 2 | 0 | 0 | 0 | **10** |
| Barracks, seven small homes, central square | 6 | 3 | 1 | 0 | 0 | **10** |
| Two warehouses, prison, support outbuildings | 4 | 2 | 0 | 0 | 0 | **6** |
| Armory + adjacent fenced training yard | 3 | 1 | 3 | 0 | 0 | **7** |
| Ravager stable and its immediate access yard | 1 | 1 | 0 | 0 | 1 | **3** |
| Two-story manor (17×15 preferred design baseline) | 1 | 1 | 1 | 1 | 0 | **4** |
| **TOTAL** | **23** | **10** | **5** | **1** | **1** | **40** |

**Placement intent, not an activation script:** Wall defenders occupy distinguishable gate/tower posts; living quarters provide dispersed first contact; training yard has the biggest concentration of elite melee defenders; stable contains only a modest local escort around its ravager; the small manor contains **four** stationed enemies across two floors (including its single commander) to avoid overcrowding a 17×15 footprint. Concrete coordinate positions, floor allocations and navigation clearances remain OPEN.

**High 35–45 total must not imply a 35–45-entity simultaneous rush.** Distribute the nominal 40 across structures, yards and walls with sensible line-of-sight breaks and natural vanilla hearing/targeting behavior; no artificial global aggro mechanism. Do not promise a hard simultaneous-engagement cap without a separately approved system. Nearby mobs may still join combat under vanilla AI naturally.

### Commander encounter (concept approved, stats proposed)

- **Identity:** one enhanced Vindicator-based officer, visually distinct by name/banner/armor or equipment **where compatible with vanilla Vindicator attack mechanics**; placement proposed on the manor's second-floor command area.
- **Vanilla melee AI preserved:** walks and attacks as a vindicator; any health/attack/knockback/resistance changes are basic attribute tuning, not new behaviors. Avoid a shield/crossbow specification that would silently require replacing Vindicator AI.
- **Initial tuning candidates, NOT approved:** more health than standard vindicators, a modest attack bonus, appropriately readable gear and an optional unique drop. Exact health, damage, movement speed, difficulty scaling, persistent nametag, equipment models and drop chances remain OPEN.
- **No forced boss lock:** killing the officer isn't needed to unlock the rest of the fortress; no protected loot gate, permanent alarm, magical ability or special combat phase.
- **Non-respawn:** at most one initial commander per generated HQ; death doesn't generate a replacement on a later visit.

### One-time spawn and save/reload contract (implementation requirements for later)

**Approved behavioral outcome:** first-clear population is finite; dead garrison members never come back merely because a chunk unloaded, a world reloaded, a server restarted or a player revisited.

**Proposed technical contract to achieve it, implementation NOT done:**
1. Use **per-structure identity** and persistent recorded initialization/spawn-slot state, not per-chunk load event blindly creating mobs. Candidate slot states: `UNINITIALIZED`, `SPAWNED` and `ELIMINATED` (or a simpler equivalent that produces the same outcome).
2. Spawn all allocated members **once**, when their regions become safely available/loaded, and avoid duplicate creation from overlapping chunk-load events; never use a normal repeating spawner for the fixed garrison.
3. Ensure **surviving structure garrison mobs are protected from ordinary distance-based despawn** so an uncaptured, undefeated stronghold doesn't empty merely because the player travels away. Do not confuse this with respawning: persistence keeps survivors, it does not recreate dead entities.
4. Resolve crashes/restarts idempotently: both a saved living entity and its recorded slot ownership must not spawn duplicate instances; a killed entity's slot remains terminal. Persist identifiers/slots across save/load, and test server restart before and after kills.
5. Respect **vanilla ambient/nighttime spawns, raids, commands and separately spawned entities** as different populations. “No garrison respawn” cannot guarantee there will never be any future hostile mob inside an otherwise normally spawnable fortress. Avoid claiming total spawn suppression, global gamerule changes or world-wide pillager spawn removal.
6. Additional garrison presence must not accidentally become infinite via vanilla raiding/patrol spawns or structure event re-triggers. Natural patrols/raids, if they occur under vanilla rules, are not replacements for dead garrison slots.
7. If a commander or ravager is intentionally named/persistent, account for death, unloading, teleport, peaceful difficulty, world upgrade and unintended despawns separately in acceptance tests; **peaceful difficulty handling remains OPEN** rather than secretly inventing a new respawn rule.

**Validation gates for future implementation:** exact total in [35,45] across multiple generated instances; table count/taxonomy; no custom alarm/reinforcement/summon/commander AI; controlled test of pull distance and LOS using vanilla AI; chunk unload and restart (survivors remain, dead don't return); chunk-border duplicate-spawn checks; fresh versus revisited fortress; no recurring spawners; fixed commander uniqueness; no accidental interaction with existing patrol variant system PR #122. No such tests have been run yet.

### Open combat decisions

Actual zone positions and safe mob separation; exact roster randomization within 35–45; spawn difficulty scaling; captain attribute/equipment/drop values; elite equipment and any attribute tweaks; reward contents and chances; possible peaceful-world behavior; exact persisted slot storage and first-generation initialization API. **Do not claim any are owner-approved by these four combat choices.**

---

## 1. Design identity

**APPROVED:** Build a large, intimidating **pillager military headquarters** (약탈자 본거지) that feels like a **vanilla villager village transformed into an ominous pillager settlement**, rather than an arbitrary maze. Internal facilities have readable adjacency and village-like roads and clusters. Primary experience is physical infiltration, fighting through a believable enemy garrison, and exploring a military village; do not add mandatory complex puzzles, key insertion or switch-gated progression.

**APPROVED exterior geometry:**
- A **precise square** perimeter wall enclosing the main compound, with **exactly one watchtower at each of the four corners**.
- **One principal gate**, no secondary entrance/gate.
- The perimeter walls and gate are consistent and regular; **do not use a terrain-adaptive stepped/zigzag castle wall algorithm**.
- The square surrounds the whole main settlement; it is not a set of separate disconnected fortifications.
- **Flat terrain candidate A** governs placement: preferentially plains; **forests are also eligible if the underlying ground, ignoring trees, is sufficiently flat**. Forest eligibility is *not* a requirement for an already naturally treeless clearing.
- Reject problematic hilly/sloped sites rather than solving them with elaborate wall elevation matching. Do not bulldoze player builds or unrelated structures.

**Dimension authority:** outer side length is now **112 blocks (APPROVED)**. Wall height/thickness, exact gate side, tower floor height and other geometry remain to be finalized. The layout below is a **concept plan**, not an approved block-level blueprint.

## 2. Proposed masterplan — square military village

Orient the plan with the sole gate on the south as an **illustrative orientation**, not a fixed compass requirement. Rotate the entire square to suit valid terrain. All four corner towers are integrated into a single continuous straight-sided square wall.

```text
              NORTH WALL — continuous straight line
       [NW WATCHTOWER]==================[NE WATCHTOWER]
       ||                                        ||
       ||  FOOD/STORE      BARRACKS   BARRACKS  ||
       ||     [S]            [B1]      [B2]     ||
       ||        \           |         /       ||
       ||          CENTRAL MUSTER SQUARE        ||
       ||                [PLAZA]                ||
       ||       /                     \         ||
       || [PRISON]     CAPTAIN'S HALL   [STABLE] ||
       ||    [P]          [HQ]            [R]   ||
       ||       \                        /      ||
       ||   [ARMORY]—[FENCED TRAINING YARD]      ||
       ||      [A]              [T]              ||
       ||                 |                      ||
       [SW WATCHTOWER]====MAIN GATE====[SE WATCHTOWER]
               SOUTH WALL — single entrance
```

Diagram indicates **functional adjacency**, not exact dimensions or coordinates. In particular, **armory and fenced training yard must be neighbors**; the owner explicitly asked for spatial logic of that kind. The HQ should be a visual landmark when entering the central village street. Do not treat this diagram's building positions as mandatory cardinal locations.

### Proposed facility program

| Facility | Layout purpose | Architectural and encounter character |
|---|---|---|
| Four corner watchtowers | APPROVED perimeter requirement | Vertical dark-oak/stone defensive towers, wall access and vantage points; always exactly four, one at each square corner. |
| Main gatehouse | APPROVED only gate | A single imposing fortified entry, direct access to an internal street and central plaza; no separate staff exits. |
| Headquarters / command hall | PROPOSED visual and combat focus | Larger dark-oak hall modeled after an ominous town hall, banners, map tables, war planning room and elevated commander chamber; possible final encounter but **boss rules OPEN**. |
| Central assembly square | PROPOSED village anchor | Dirt-path/cobblestone village plaza, flags, supply carts, notice board, drill/muster points. Visually binds houses rather than presenting isolated arena rooms. |
| Barracks cluster (e.g. 2) | PROPOSED sleeping/inhabitation | Village-like houses rebuilt with barred windows, banners, equipment racks and bunks. Adjacent to square and training yard. |
| Armory / weapon workshop | PROPOSED combat infrastructure | Weapon racks, crossbows, arrows, grindstones, anvils, smithing/workbench details; **directly next to fenced training ground**. |
| Fenced training yard | PROPOSED adjacent to armory | Plain fenced arena with target blocks, sparring dummies, obstacle lane and room for patrols; **not a mandatory wave trigger**. |
| Supply warehouse | PROPOSED logistical feel | Barrels, crates, food, spare shields, carts; casual village silhouette, grim pillager purpose. |
| Prison / cage compound | PROPOSED pillager character | Small cells, fenced pens, optional captives (villagers/allays only if spawning and safety are separately approved); no forced rescue quest. |
| Ravager stables / beast pen | PROPOSED intimidating landmark | Fenced large stable near roads for beast handling; **whether live ravagers spawn here is OPEN**. |
| Small functional houses/outbuildings | PROPOSED density | Additional pillager huts, kitchens, covered workshops and latrines for a plausible occupied settlement; no fixed quantity approved. |

### Village-like atmosphere

**PROPOSED:** preserve a recognizable village grammar—paths, courtyards, repeated small residential volumes, small fences, workstations, lampposts, crops/food storage—then turn it threatening with dark oak, cobblestone/deepslate trim, pillager banners, barred openings, training equipment and restrained sinister lighting. Architecture should be legible as a working settlement, **not** an enormous empty walled box. Keep paths navigable to players and pillager AI.

## 3. Generation placement policy

### Approved terrain selection

- **Choose largely flat sites.** Plains and forest both qualify when actual *terrain*, after ignoring the trees as obstacles for candidate evaluation, is relatively flat.
- This is **not a license to manufacture a flat site by leveling hills**: substantial terrain elevation changes invalidate the candidate.
- **Strict square wall geometry** is prioritized over accepting every biome site. Flatness wins over frequent generation.
- The wall and internal village must fit together; reject candidates that would leave corners, the gate or buildings hanging in midair.
- Forest trees may need conservative clearing **inside the approved footprint** to make the compound placeable. The forest site's original terrain elevation must be evaluated from solid supporting ground **below the foliage/log columns**, not by sampling leaf-canopy height.

### Proposed generator preflight (not yet implemented)

1. Select candidate biome and bounding square, including clearance margins for corner watchtowers and gate.
2. Sample a terrain-support heightmap that ignores tree trunks and leaves, but **does not ignore actual soil, stone, water, cliffs or player modifications**. Natural forest floor and low vegetation count as ground.
3. Require a contiguous safe plateau supporting all four wall sides, corner towers, gate, roads and major facility footprints. Use maximum elevation variation, adjacent-step height, cave/void risk and water thresholds; **exact numbers OPEN**.
4. Reject sites intersecting villages, existing structures, artificial builds, nonremovable block entities, cliffs, significant water or unsupported underground voids. Check **all** planned bounds rather than only the anchor.
5. Optionally remove only identified natural tree logs/leaves and replace minor vegetation **within the building footprint** under an explicit safe-clearing policy; avoid removal outside the footprint and avoid claiming that detecting natural versus player-grown trees is solved. If classification is ambiguous, reject or preserve the area.
6. Place the **single rigid square** and modular internal village cluster only after every critical substructure is validated. Reject the whole attempt rather than spawning a half-built fort.
7. Later run **real world-generation tests** across multiple plains and forest seeds, plus near-miss slopes, tree canopies, ravines, cave ceilings and overlap conditions. Existing generator pipelines/results are not automatically evidence for this proposed fortress.

**Do not confuse this non-destructive generator preflight with a promise of historical block provenance or backups.** No complicated terrain-following crenellation algorithm is required or selected. Minor path leveling or foundation detailing can be considered later, but any material terrain editing must be separately specified and tested.

### Proposed layout implementation freedom

Even though the perimeter square is fixed, **internal buildings may use a small curated set of predefined village-style layouts**, all placed on prevalidated footprints, to introduce visual variety. Reordering houses or mirroring rooms is optional and must never break the two essential spatial relationships: **single gate→recognizable main street/plaza**, and **armory next to fenced training yard**. All house/road variants must preserve access to headquarters and all four towers.

## 4. Player-facing gameplay — proposed, not final

- **Approach:** distant sight of a symmetrical square fort with four corner towers, banners and dark-roofed village buildings visible over the wall.
- **Gate combat:** fight guards, breach **the sole designed gate** or use normal Minecraft player capabilities to scale/mine walls. "Only gate" is an architectural rule, **not** invulnerable walls or a prohibition on emergent play.
- **Village sweep:** explore workshops, alleys, barracks and side courtyards; opponents inhabit the compound according to believable military roles, not scripted puzzle rooms.
- **Command encounter:** captain/higher ranking pillager in HQ is a **candidate only**, not a newly approved boss or encounter specification.
- **Reward:** valuable military supplies, weapons and selective loot, with final rarity, items and duplication safeguards **OPEN**. No special exclusive rewards approved by this document.
- Use existing pillager/vindicator/crossbow and ravager behavior where appropriate; existing PR #122 patrol-variant authority is **separate** from this structure's unapproved resident roster.

## 5. Accepted versus open decisions

| APPROVED now | Designer PROPOSED, review later | OPEN for detailed design |
|---|---|---|
| **112×112 external square**, four integrated corner towers, one perimeter gate, A+C dimensional wall concept (exact modeling delegated to Claude), plains/flat-under-tree forests. **Interior 19–22 facilities, 2-story illager manor HQ, gate→square→HQ main road plus alleys, prison and ravager stable. Combat: 35–45 initial garrison, vanilla AI only, enhanced Vindicator commander, no natural garrison replenishment after deaths.** | **20** proposed facilities; **40**-unit nominal roster with 23 pillagers, 10 standard vindicators, 5 elite vindicators, 1 commander and 1 ravager; proposed zone allocations, persistence implementation outline and optional encounter gradient | Wall/terrain/building coordinates; exact spawn mechanics and saving; difficulty scaling; commander/elite tuning, rewards, other natural spawns, peaceful behavior and generated-world QA |

## 6. Immediate next design milestone

**Interior and combat concepts now recorded** with owner-approved 19–22 facilities, 2-story manor, primary road + alleys, prison and ravager stable; 35–45 first-clear defenders, vanilla AI, enhanced Vindicator commander and no defeated-garrison respawn. Next refine room programs, exact facility sizes/coordinates, road packing, commander stats and loot after reviewing the proposed 20-facility and 40-unit ledgers. **Defer** wall height/thickness, corner tower geometry, gatehouse block modeling, architectural palette and other detailed exterior visuals to a later Claude-led design task. Terrain thresholds and forest tree-clearing policy still need explicit preflight decisions before generation. Do not start implementation merely from this design record.

**Scope guard:** This document does not resolve unrelated desert pyramid, End city remodeling, spider cave or colosseum design and does not authorize implementing new structures on an existing active generation branch.

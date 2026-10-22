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

**Next design topic:** verify the reward probabilities and existing-item eligibility from the new reward addendum, audit rune merchant exclusivity and actual registered items, then integrate a tested finite-garrison / one-time-container implementation plan; hand off exterior 3D stone/dark-oak detailing to Claude as separately requested.

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

## Owner-approved reward direction and proposed quantitative loot plan — 2026-10-22 (latest authority)

### Owner-approved outcomes

- **Reward tier:** a successful pillager-headquarters raid should reward players **meaningfully better than an ordinary vanilla Woodland Mansion**, appropriate for its 35–45 initial defenders and one-time garrison. This is a gameplay target, **not** an already demonstrated empirical comparison of generated loot values.
- **Rare reward sources:** use **existing MoreMinecrafts accessories, actual-enchantment runes and equipment** alongside fitting vanilla supplies, rather than creating a new headquarters-only currency, upgrade system or invented accessory. Respect the existing roster, item IDs, progression and drop eligibility when implemented.
- **Commander drops:** **ordinary rare loot plus a separate low-probability special-reward roll**. A special reward is **not guaranteed** on each commander kill. The commander's upper-floor manor loot chest(s) remain **independent of the kill**; no commander-key door or forced completion gate.
- The previously approved **35–45 finite garrison with no garrison respawn** remains independent of chest refill logic; one-time loot initialization and stable saved claims must be specified below before implementation.

**Authority separation:** numbers/weights/counts and per-category exact items below are **PROPOSED initial balancing targets**, not implicitly approved. In particular, there is no approval to mint a new rune item ID, to distribute merchant-only stock without evaluating that economy, to guarantee a unique accessory or to convert a rune into a stat token.

### Compatibility constraints based on existing design records

- Reference **`docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md`** in stacked docs PR #131 (original accessory approval PR #101, with follow-up enhancement PR #113): 12 adopted accessories, 5 equip slots, acquisition direction exploration/dungeons/bosses; the **specific structure loot pools have not been finalized**. Their existing names/effects must not be overwritten. Proposed HQ eligibility should prioritize appropriately themed **combat-accessory** options, subject to their currently approved slot/activation mechanics.
- Reference **`docs/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md`** in stacked docs PR #131: 24 approved **above-table-cap real-enchantment rune** level SKUs; no generic rune currency. Runes are *real enchanted equipment* application items and are separate from the ordinary enchanted-book option system. Whether/which caravan rune SKUs may also appear in dungeon chests, and their relative economic exclusivity, need a dedicated cross-content decision. This proposal makes **no blanket promise that every rune is available as drops**.
- Existing mod equipment/upgrade resources must be referenced by their *current actual registry entries* after integration audit; **do not invent item IDs or assume the distributed draft documents are merged to main**.

### Proposed facilities and loot envelopes

These are **container-group envelopes** for a whole facility type (or group of seven houses), **not quantities per individual barrel, and not guaranteed cumulative valuables**. Containers that are purely visual must not roll separate high-end loot.

| Site (approved facility program) | Proposed searchable inventory budget | Typical vanilla/mod loot theme | Rare-pool access |
|---|---|---|---|
| 7 residential houses, collectively | 5–8 minor searchable containers total | Bread/potatoes, low emerald count, coal, tools, occasional arrows; small varied civilian loot | **None** for restricted HQ rare pool |
| 3 barracks, collectively | 3–5 containers total | Arrows 8–24 per awarded stack, occasional crossbow, iron gear, food and ordinary equipment materials | Common equipment only, no top-tier rune guarantee |
| 2 supply warehouses, collectively | 4–6 containers total | Iron ingots 4–16, gold ingots 2–8, emeralds 2–12, food/consumables, mod crafting materials only if IDs verified | One **candidate** ordinary enhanced-gear roll across both stores; not guaranteed |
| Armory | 2–3 accessible loot inventories plus optional static/display gear | Crossbows, axes, armor pieces, arrows 16–48 per stack, iron and repair materials, modest chance enchanted/augmented equipment | Primary optional **rare equipment** pool; share global cap for premium drops |
| Fenced training yard | 0–1 chest/crate | Training arrows, low-grade weapon supplies, target props | No independent premium roll |
| Prison | 1–2 containers | Confiscated minor belongings, maps, emeralds, occasional gear | Rare gear only via shared premium allocation, not automatically |
| Ravager stables | 1–2 containers | Leather, saddles if appropriate, feed, animal care gear | No independent accessory/rune roll |
| 3 outbuildings, collectively | 2–3 containers | Cooking supplies, ingots/tools, carts and spare building materials | No independent premium roll |
| Two-story manor / command building | 2–3 primary inventories (office plus small upper-level valuables chest) | Map/plunder display, emeralds 6–18, diamonds 1–3 *as optional rolls*, rare weapons and existing MM assets | **Primary** shared premium allocation and HQ special-chest candidate, physically reachable without officer defeat |
| Commander | **One death-loot transaction** | Existing uncommon/rare military drops; separate low-probability high-end reward | See two-layer drop policy below |

**Do not multiply each proposed numeric range by every chest.** For example, armory's arrow 16–48 is a stack roll candidate rather than a guarantee for each of three chests. Overall economy quantities are provisional and should be compared to actual Woodland Mansion chest expectations, mod progression stage and adjacent structures.

### Proposed premium distribution (one HQ instance, bounded)

To reward a large one-time encounter **without farming every small cottage for guaranteed accessories**, allocate scarce items through **at most two premium successes from structure containers**, each sampled from *a preselected eligible pool* at structure creation, not individually minted by every chest. This cap is a design proposal (not yet approved).

1. **Manor premium chest:** 1 premium-category draw candidate, recommended success chance **65%**. On success choose one eligible category among **existing MM enhanced equipment (55% of successes), existing accessory (30%), eligible real-enchantment rune (15%)**. No guaranteed rune or accessory. Ordinary chest supplies remain regardless of result.
2. **Armory premium group:** 1 separate premium-category draw candidate, recommended success chance **35%**. On success choose **existing MM enhanced equipment (75%), existing accessory (15%), eligible real-enchantment rune (10%)**. Draw may live in the armory's main chest or a safely accessible display inventory (subject to gameplay support), not a new third premium location.
3. **All other supply buildings:** no additional independent *premium* accessory/rune roll in v1. They can still offer ordinary good equipment, supplies and minor enhancement materials that are defined in the existing mod.
4. **Global container maximum: 2 premium-category items per one headquarters**, consisting of 0–1 manor and 0–1 armory successes. Results are randomized once and thereafter fixed for that generated site. Premium classification is by rule, not by physical chest count. A multi-roll container must not bypass this cap.
5. Rune eligibility is **OPEN**, pending cross-approval with rare caravan economy; if there are no approved HQ-eligible runes, exclude the rune branch and **renormalize weights over remaining eligible categories**. Likewise never replace it with a made-up rune SKU or missing item. Only registered, approved item IDs are roll candidates.

Probability interpretation: success rates **65% / 35% are for the proposed container premium draw attempts**, and the listed category weights are **conditional on success**, **not** absolute per-world loot rates. Both successes are independent between manor and armory; items within each success are mutually exclusive category choices. **No guaranteed premium from either chest**; good ordinary materials and equipment serve as guaranteed useful fallback.

### Proposed commander two-layer drop (non-repeatable)

| Roll layer | Initial proposal | Notes |
|---|---|---|
| Ordinary drop | One military rare-loot bundle candidate (e.g., emeralds **3–8**, crossbow/axe or eligible existing enhanced-gear component) with vanilla combat loot interaction audited | Owner approved general rare rewards in principle; exact bundle and deterministic/probabilistic nature OPEN |
| **Separate special** | **10%** chance of **one** item, chosen from **eligible existing MM accessory (50% conditional), existing upgraded/enhanced equipment (40%), HQ-authorized real-enchantment rune (10%)** | This **10% is proposed**, satisfies owner's "low probability" qualitative choice but is **not** an approved exact percent; no custom dedicated boss artifact implicitly created. |
| Failed special roll | **90%** → no special item; ordinary rare drop behavior still applies | On failed roll there is **no consolation guaranteed accessory** |
| Repeat attempts | A given one-off commander can produce its special roll **at most once** | Prevent additional rewards through unload/reload, duplicate spawn and other exploit paths |

**Distinct pools:** the commander special is *in addition to* the container premium maximum of 2, so the total proposed high-end opportunities are **three maximum** across one HQ (manor, armory, commander). The commander should not share the same one-and-only success token as the manor; killing it is optional. A commander who dies with a non-player source may require a separately specified drops policy, but **never reroll on a subsequent load**.

### Loot generation / persistence contract (design outcome, not implemented)

1. Tie every searchable loot container and commander reward to a **stable structure identity + stable logical slot ID**; map physical chest blocks/inventories to those slots. Avoid using chunk-load frequency or repeated screen opening to grant extra rolls.
2. **Roll premium allocation once per generated structure** (or persist an equivalent deterministic seed and allocation). Keep unopened container items/loot tables stable across world saves, server restarts, and simultaneous multiplayer interaction.
3. **Vanilla-compatible unopened loot chest:** loot-table-on-first-open behavior can be used if it has an invariant saved seed and is consumed once under server authority; first player doesn't create a new independently rerollable pool just by reopening. Decorative chests and multi-block double chests must not provide duplicated logical premium slots.
4. **Once looted, permanently depleted by normal inventory behavior.** Never auto-refill structure containers on unload/reload, time intervals or boss defeat. No per-player duplicate instances of the same structure chest unless a future explicit design says so.
5. Commander special drop is one transaction associated with its initial unique commander; persist death/claim outcome and do not award again after a server restart. Implement appropriately for killed-before-save, loaded-mob duplicate and lag/reentrant kill callbacks.
6. Do not suppress unrelated vanilla ambient mobs or duplicate legitimate player-to-player transfers. Looting enchantment interactions, container destruction and peaceful-world behavior need explicit later verification.
7. **Unknown mod registry items are ineligible**: don't silently substitute ordinary enchanted books for runes, invent placeholder accessories or rely on draft item IDs absent from the chosen integration head.
8. Validate anti-duplication, inventory split/merge, independent manor/commander rewards, structure-level 2-premium-cap, and replay-safe generation across chunk borders. This specification is **NOT CODE VERIFIED**, **NOT LIVE VERIFIED**, **NOT MERGED**.

### Outstanding approval and balancing checklist

- Compare **expected total-value distribution**, not only flashy best-case loot, to Woodland Mansion and other MM dungeons. Target is clearly better than mansion; precise resource quantities and stage availability need owner decision/balance testing.
- Approve/adjust proposed **65% manor + 35% armory** premiums, capped at 2, and **10% commander special**; clarify zero-premium outcome acceptability.
- Choose eligible accessory whitelist and binding to accepted accessory slot roster; respect strong active-combat power and avoid automatic level +10 rewards.
- Decide whether the rare merchant's approved over-cap rune SKU set can partially appear in HQ, and if so which levels/weights; if none, reweight to accessories and equipment.
- Confirm armor/equipment rarity, emerald/diamond quantities, enchanted weapon strength and any enhancement stone or catalyst drops without undermining existing research/economy progression.
- Confirm chest locations, counts, accessibility and precise single-use semantics under multiplayer, save/load and worldgen.
- No new custom item, loot implementation, boss mechanics, code, tests or artwork is authorized by documenting these candidate values.

---

## Design checkpoint and next decision queue — 2026-10-22

**Saved checkpoint:** The reward direction and proposed loot allocations in the immediately preceding reward section are safely recorded. The owner has asked to preserve that record and prepare the next decisions, **not to approve unselected probability proposals**.

### Locked decisions to retain through future handoff

- Footprint 112×112 exterior square, four continuous wall-integrated corner towers, one perimeter gate; flat plains or flat natural forest floor only.
- A+C imposing dimensional fortress styling, with exterior block architecture **deferred to Claude**.
- Interior 19–22 facilities, recognizable militarized village, gate→square→two-story small manor primary road with small alleys; both prison and ravager stable; armory immediately adjacent to fenced training yard.
- Initial garrison 35–45, vanilla AI only, one enhanced-Vindicator commander, no natural replenishment of defeated structure garrison.
- Rewards meaningfully better than Woodland Mansion; source rare items from existing MM accessories/equipment and only compatible real-enchantment runes; ordinary rare commander drops **plus low-probability extra special reward**. Independent accessible upper manor reward chest and commander drop.

### Still only proposals, not owner confirmations

- Facility and defender exact counts (20 and 40), commander/manor dimension 17×15 unless separately approved, commander **attack** (40 HP now approved), elite equipment and drops; staged zone allocation.
- 65% manor premium draw, 35% armory premium draw, up to 2 container premium successes and separate 10% commander special. All numerical reward ranges in the tables remain candidates.
- Which of the existing 12 accessories and 24 real-enchantment rune SKUs are legal in this dungeon; merchant exclusivity and game-stage availability remain under review.
- Exact structure separation, regional rarity, site selection constraints, forest-clearing tolerance, minimum distance from villages/outposts and dimension scope.

### Next decision pass — proposals for owner review

**D1 — Commander tuning:** **40 HP is APPROVED**, with a proposed **+20–30% melee damage relative to an unmodified same-difficulty Vindicator** for testing. 30/50 HP alternatives are superseded. Exact attack attribute and version-specific damage must be audited.

**D2 — Spawn rarity:** **B: rare regional landmark is APPROVED**. Exact spacing/separation, biome filters, terrain flatness acceptance and measured encounter frequency are still OPEN pending seed tests; the 1,200–2,000 block spacing experiment described above is not approved.

**D3 — Reward numeric signoff:** owner should expressly approve/modify the 65% / 35% / 10% candidates, premium caps and zero-premium possibility; avoid promoting them to locked rules by silence.

**D4 — Actual item allowlists:** cross-check the existing MM registry and distributed accessory/rune decisions, then approve specific accessible combat accessories and high-level rune SKUs. Preserve the difference between runes that apply real enchantments and option books. Ineligible/non-registered candidates are excluded, never silently replaced with invented items.

**D5 — Placement QA policy:** define fit criteria for a complete 112×112 square including all wall sections, towers and gate; evaluate flat soil under forest trees, reject significant height steps, slopes, liquids, village overlaps and preexisting player builds. No generation code is implemented as part of this checkpoint.

**Next action after owner choices:** update exact design authority, then form a Claude handoff covering architectural build shapes and separate generator implementation checklist. Keep PR Draft and do not merge other PR branches.

---

## Owner approval — commander 40 HP and rare regional landmark (2026-10-22, latest authority)

### Newly confirmed

1. **Commander maximum health = 40 HP (20 hearts)**. The unique headquarters officer remains a **Vindicator-based enhanced mob** operating with **vanilla Vindicator combat/targeting AI**. This is an actual numerical owner decision, superseding older proposed 30/40/50 HP alternatives. The 40 HP is the officer's base maximum-health design setting, **not extra HP on top of vanilla health**, not a boss bar requirement, and not a signal to add a custom skill or army-wide buff.
2. **Generation rarity = B: rare regional landmark.** A headquarters should feel like an unusual, memorable military fortress encountered during meaningful exploration, rather than a common outpost or an extremely inaccessible once-per-world structure. This is a **qualitative discovery target**, not an approved structure spacing, chance per chunk, frequency, or guaranteed distance to a player.
3. Carry forward all approved characteristics unchanged: the 112×112 **outer** square and its four wall-integrated corner towers/single perimeter gate, 19–22 interior facilities, two-story compact manor HQ, vanilla mob AI, 35–45 original garrison, non-respawning defeated garrison, loot better than Woodland Mansion and mixed pre-existing MM reward sources.

### Commander combat — detail proposals, subordinate to approved 40 HP

| Parameter | Authority / recommendation | Design and acceptance consequence |
|---|---|---|
| Max HP | **APPROVED exactly 40 HP** | 20 hearts. Spawn at full base health, then let normal vanilla damage/healing rules apply. |
| Mob type / targeting | **APPROVED enhanced Vindicator; vanilla AI only** | Preserve regular melee navigation/attacks. No new commander aura, rally, alerts, summon phases, shield AI or scripted locking. |
| Attack damage | **PROPOSED: 1.20–1.30× unmodified vanilla Vindicator base melee attack damage on the same difficulty** | Exact `generic.attack_damage` modifier/base value depends on version/difficulty and must be checked against Minecraft 26.3 mappings. Do not double-apply existing difficulty scaling or confuse held-axe damage with generic attributes. |
| Movement | **PROPOSED: vanilla Vindicator movement speed** | Make lethality come from guarded room geometry and melee damage rather than artificial speed boosts. |
| Equipment | **PROPOSED: distinct axe and pillager-themed look/name, if supported** | Does not change core attack implementation. No required new weapon texture or unregistered drop. |
| Support | **PROPOSED: 2–3 other stationed guards spread across ground floor / stairs** | Existing nominal 40-person ledger assigns **four total manor defenders including commander**; no additional reinforcements and no force-spawn when commander aggroes. |
| Difficulty | **OPEN** | Verify Easy/Normal/Hard effective outgoing damage using actual version. Do not treat HP 40 as requiring an automatic custom difficulty multiplier. |
| Reward | **Already APPROVED qualitatively:** ordinary rare drop + separate low-probability special | Earlier proposed **10%** special chance remains only proposed, as do loot item weights and exact money/gear amounts. |

**Suggested 17×15 manor spatial encounter:** 1–2 ordinary defenders around first-floor lobby/stairs, 1 elite guard upstairs near the officer, commander in office. Counts must sum to **four total**, not four guards plus commander; the precise room/level placement and 17×15 footprint itself are still design proposals, subject to Claude architecture and actual pathfinding tests. Allow player to access all loot without mandatory commander kill.

**Testing gates:** server-spawned commander is truly 40 HP, correct loaded/unloaded health persists, vanilla targeting is unchanged, damage matches chosen baseline on multiple difficulties, manor melee navigation remains possible, exactly one original officer appears per HQ, officer never respawns after actual death, manor loot is independent.

### Rare regional landmark — world-generation design

**APPROVED intent:** rarer than a routine pillager outpost as a **design goal**, but not so rare that a normal exploration-focused world effectively never finds one. Its 112×112 bounding square and demanding naturally flat site must be honored; it is okay to reject unsuitable generation sites rather than terraform or staircase the walls.

**PROPOSED implementation policy to evaluate (none of these specific numbers are owner approved):**

- **Dimensions / biomes:** Overworld only as the obvious initial design target. Favor plains and qualifying temperate forests with flat supporting soil **under the natural tree canopy**; exact biome tags and exclusions OPEN. Forest is eligible without a pre-existing huge clearing, provided trees can be handled safely. Do not add desert, mountains or swamps merely to improve spawn acceptance.
- **Placement model:** sparse attempted regional candidate sites + **strict whole-footprint preflight**, instead of forcing one spawn every N chunks even when terrain is bad. Structure rarity should be measured as **successful accepted fortresses**, not attempts alone.
- **Initial spacing experiment:** test approximately **1,200–2,000 blocks between accepted landmark centers** as a *provisional spacing trial only*, **not a committed generation range or guarantee**. Whether the structure API can offer this spacing with site rejection and without biased clustering needs measurement. Test tighter/looser alternatives if success rate is too low/high. Never write as a hard minimum until generator behavior is verified.
- **Flatness:** sample underneath trunks/leaves, not canopy; bound max terrain elevation spread **trial candidate 2 blocks**, then test 1–3 as necessary. All four tower platforms, wall lengths, gate, roads and major buildings must fit without unsupported floors or collision with steep hills/water/other structures. This **2-block tolerance is not approved**.
- **Tree clearing:** distinguish candidate native trees from player-built / player-grown material **only if technically reliable**; otherwise reject or preserve the site. Do not erase protected structures or owner builds. No giant landscaping pass or broad-area bulldozing.
- **Regional separation:** proposal to refuse a location if it directly overlaps a village, existing structure or other headquarters; exact protection buffer and unique-location registry remain OPEN. Do not make nearest HQ distribution promises until the real generator is instrumented.
- **Y / underground support:** candidate's **entire** 112×112 foundation, including four corners, must be safe to place; preflight against surface water, deep ravines, significant caves, unloaded block claims and chunk borders, without declaring all naturally occurring caves forbidden.
- **Rotation:** the whole square and internal gate→square→manor road may rotate as one unit to fit the chosen valid site; do not change the exact square perimeter or create secondary entrances.

**Generation measurement gates before choosing concrete spacing/weights:**
1. Record attempted versus *accepted* sites across a deterministic many-seed suite (plains and forest sampled separately), actual nearest-neighbor spacing, acceptance failure categories and chunk-generation cost. Include full 112×112 site validation, not just structure-center height.
2. Compare observed discovery frequency qualitatively and quantitatively to nearby existing pillager outposts on the **same seeds**, allowing for biome/topography differences. Rarity target is a comparison, **not** a borrowed undocumented Minecraft outpost frequency constant.
3. Confirm sites in flat forests occur when canopy is present (not only naturally cleared grass), without unintended natural/player-built tree removal; classify ambiguous tree provenance as a blocker rather than falsely claiming it is solved.
4. Ensure deterministic accepted/not-accepted outcome across repeated world generation, chunk border ordering and save/reload; no partially spawned walls, duplicate HQ, carved village, or broken roads.
5. Performance-check size, prospective enemy 35–45 load and long-lived garrison persistence together; very large defensive layout should not cause unbounded entity/structure work.

### Status and next choices

**APPROVED now:** Commander 40 HP, regional-landmark rarity B, Vindicator/vanilla AI and existing fixed-garrison/no-respawn rules.

**Still OPEN:** exact attack-damage modifier (initial candidate +20–30%); difficulty tuning and elite stats; exact rarity spacing/frequency (1,200–2,000 block trial is only a proposal); allowed biome tags; flatness limit; tree removal safeguards; 65%/35%/10% loot candidates; existing accessory/rune allowlist and economy compatibility.

**Design next pass:** decide commander attack tuning, actual eligible loot whitelist and premium odds; then verify generator sampling before any quantitative spawn-spacing sign-off. Exterior wall block modeling stays deferred to Claude. This section is **design only / NOT CODE VERIFIED / NOT WORLDGEN VERIFIED**.

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
| **112×112 external square**, four integrated corner towers, one perimeter gate, A+C dimensional wall concept (exact modeling delegated to Claude), plains/flat-under-tree forests. **Interior 19–22 facilities, 2-story illager manor HQ, gate→square→HQ main road plus alleys, prison and ravager stable. Combat: 35–45 initial garrison, vanilla AI only, enhanced Vindicator commander, no natural garrison replenishment after deaths.** | **20** proposed facilities; **40**-unit nominal roster with 23 pillagers, 10 standard vindicators, 5 elite vindicators, 1 commander and 1 ravager; proposed zone allocations, persistence implementation outline and optional encounter gradient | Wall/terrain/building coordinates; exact spawn mechanics and saving; difficulty scaling; commander/elite tuning, **exact loot counts, item whitelist, premium odds and rune-caravan exclusivity**, other natural spawns, peaceful behavior and generated-world QA |

## 6. Immediate next design milestone

**Interior and combat concepts now recorded** with owner-approved 19–22 facilities, 2-story manor, primary road + alleys, prison and ravager stable; 35–45 first-clear defenders, vanilla AI, enhanced Vindicator commander and no defeated-garrison respawn. Next refine room programs, exact facility sizes/coordinates, road packing, commander attack tuning (40 HP approved), rarity implementation metrics and loot after reviewing the proposed 20-facility and 40-unit ledgers. **Defer** wall height/thickness, corner tower geometry, gatehouse block modeling, architectural palette and other detailed exterior visuals to a later Claude-led design task. Terrain thresholds and forest tree-clearing policy still need explicit preflight decisions before generation. Do not start implementation merely from this design record.

**Scope guard:** This document does not resolve unrelated desert pyramid, End city remodeling, spider cave or colosseum design and does not authorize implementing new structures on an existing active generation branch.

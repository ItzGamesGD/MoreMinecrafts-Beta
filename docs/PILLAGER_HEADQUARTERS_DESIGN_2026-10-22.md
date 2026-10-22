# Pillager Headquarters — owner-approved concept and proposed detailed layout (2026-10-22)

> **Status: DESIGN ONLY.** Separate from structure-generation implementation and from the smaller vanilla pillager outpost or patrol variants. No blocks, loot tables, generation settings, custom mobs, AI, world files or code are implemented by this document.
>
> **Authority key:** **APPROVED** = directly selected by owner in current conversation. **PROPOSED** = designer elaboration for review. **OPEN** = requires specific future owner decision or generated-world verification. Do not accidentally promote PROPOSED building counts, dimensions or balance to approved requirements.

## Owner confirmation addendum — 2026-10-22 (later authority)

**APPROVED — footprint:** The pillager headquarters uses a **112 × 112 block outer square** (12,544 horizontal blocks² including the walls and towers). This is the overall outside footprint, **not** an additional 112 × 112 buildable interior. The perimeter remains a geometrically precise square with four corner watchtowers and a single main gate. Do not infer that 112 is the inner courtyard dimension.

**APPROVED — settlement density direction:** The inside should be a **plausibly populated ominous military village**, not an empty parade ground with only eight large standalone buildings. Distribute **additional small pillager houses and supporting outbuildings** between the key military facilities to fill the compound organically. The **exact number, footprints and placements** of these small buildings remain PROPOSED until the block-coordinate plan is reviewed.

**PROPOSED sizing baselines retained for further review, NOT new owner approvals:** wall height ≈8 blocks; tower footprint ≈11 × 11 blocks; central gate width ≈7 blocks; two or three ≈13 × 11 barracks; command hall ≈23 × 19; armory ≈17 × 13; fenced training yard ≈25 × 21; supply warehouse ≈15 × 13; prison ≈13 × 11; ravager stable ≈17 × 13. These estimates must be checked against the usable interior left by 112 × 112 external walls, road widths, corner tower overlap and gatehouse. Do not interpret rough area examples as final block placements.

**Still APPROVED:** flat plains and forests with flat ground under the trees (option A); do not make stepped terrain-adaptive wall segments. Remaining terrain thresholds, wood/stone palette, exact building count, combat roster and rewards are OPEN.

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
| Pillager HQ is a large military-village-like stronghold; square continuous outer wall; exactly four corner watchtowers; only one main entrance; flat terrain type A, plains and sufficiently flat forests ignoring trees; reject stepped terrain adaptive walls | Command hall, village plaza, 2+ barracks, armory beside fenced training yard, jail, storehouses, beast pens, path network; gate placed south in the sketch; defensive staging and environmental storytelling | Wall height/thickness and internal usable dimensions (outer square fixed at 112×112); exact flatness and tree clearing tolerances; mandatory facility roster/count; roof/material palette; interior road graph and variant generation; mob composition and captain; loot tables, rarity and balancing; whether ravager/villager captives spawn; exact allowed forest biomes; generation frequency; preflight algorithm and worldgen QA |

## 6. Immediate next design milestone

**Detailed headquarters layout pass** (still design-only): retain approved 112×112 outer square; approve wall height/thickness, corner tower modules, entrance gatehouse, internal path topology, number/size of barracks and mandatory buildings, tile palette, battle density, safe terrain thresholds and forest tree policy. Produce a readable plan and area schedule before asking the structure generator to place anything.

**Scope guard:** This document does not resolve unrelated desert pyramid, End city remodeling, spider cave or colosseum design and does not authorize implementing new structures on an existing active generation branch.

# ST-05 Worldgen Owner Decisions — WG-Q01–Q18 (2026-10-30)

**Status: OWNER-APPROVED DESIGN ONLY.** This document records the owner's worldgen and architectural decisions in conversation. No worldgen implementation, merge, checkpoint promotion, or live acceptance is authorized. It is a **newer overlay** for the ST-05 structure/worldgen clauses in `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md`; combat, lifecycle, persistence and rewards remain governed by that authority. Ancient City `arena.nbt` is unrelated to ST-05.

## Decisions

| ID | Owner decision |
| --- | --- |
| WG-Q01 | Delegated parameter selection **B**: owner sets target experiences and permitted bounds; implementer may choose precise generation spacing/cave-fit parameters from sampled results within those bounds. Do not silently exceed owner constraints. |
| WG-Q02 | Circular colosseum, **68-block exterior diameter**, **23-block total height**; **no spectator stands**. Natural-cave connection or burial in underground rock permitted. |
| WG-Q03 | Buried-underground-first placement, center/reference Y **-40 to -10**. Intersecting caves may be connected. Do not require a cave large enough to contain the entire arena. Check bedrock/surface interference. |
| WG-Q04 | **Hybrid construction**: procedural circular shell, flat floor, terrain embedding; NBT modules for entrances, pillars and decoration. Create new ST-05-specific modules, not Ancient City arena assets. |
| WG-Q05 | Four player entrances N/E/S/W. Guarantee at least **one** connection to a valid natural cave; other entrances may remain buried. Reject candidate when no acceptable connection exists. |
| WG-Q06 | Flat combat floor **radius 29**; six normal Vaults evenly spaced on **radius 27**; one central Ominous Vault and activation crystal. Spawn annulus **radius 12–24** unchanged. Ensure spawn points do not intersect Vaults/architecture. |
| WG-Q07 | Selective terrain repair up to **4 blocks beyond outer wall**; do not uniformly excavate a four-block shell. Cave-connection corridor is separately governed. Preserve natural terrain where possible. |
| WG-Q08 | Cave connection path **at most 32 blocks** measured from entrance exterior to a genuinely traversable natural cave, not straight-line proximity alone. Reject unsuitable candidates. |
| WG-Q09 | Initial qualifying natural cavern: representative connected chamber **width >=32**, **height >=18**, and **traversable connected cave path >=64 blocks**. Reject tiny cracks or disconnected voids. Implementer may calibrate measurement algorithm and detailed thresholds through samples while preserving intended medium-large cave class and owner bounds. |
| WG-Q10 | Target **75% placement chance per eligible natural cavern** *after* Q03/Q05/Q08/Q09 and actual structure-fit checks, **at most one roll per eligible cavern**. No extra arbitrary rarity multiplier. The owner's **15–25%** estimate of caves satisfying Q09 is **low-confidence and unverified**, not a measured probability. Actual rates require worldgen sampling. |
| WG-Q11 | **B**: at most one colosseum per connected natural cavern, plus a minimum separation between colosseums in different caverns. Exact separation delegated to implementation/sampling. Do not modify vanilla cheese-cave noise generation; use deterministic cavern identity and generation-order-independent decisions, with cross-chunk/restart duplicate prevention. |
| WG-Q12 | Both **A and B** architectural states: largely intact ancient arena, and partially ruined arena. Flat, unobstructed, **bright** combat floor in both. Integrate lighting naturally into design with lanterns, torches, and embedded redstone-lamp floor decoration; avoid obviously artificial floodlighting. |
| WG-Q13 | Roof is coupled to Q12: intact state -> complete masonry dome; ruined state -> **50% partially ruined dome**, **50% exposed natural cave ceiling**. No surface breakthrough or dangerous falling hazards. |
| WG-Q14 | Intact and ruined states **50% each**. Thus final variant distribution: intact/full dome **50%**, ruined/partial dome **25%**, ruined/natural ceiling **25%**. |
| WG-Q15 | **A**: **8–12 perimeter columns**, no interior columns; keep the central fight and boss-charge lanes clear. Ruin detail concentrated near perimeter. |
| WG-Q16 | **B** ancient geometric floor pattern with concentric/radial details, **deepslate-family blocks only for stone decor**: polished deepslate, deepslate bricks and tiles, with cracked variants for weathering. Integrated flush floor lighting permitted; floor remains level. |
| WG-Q17 | **B**: central crystal as centerpiece; nearby Ominous Vault integrated into a floor decoration recess, accessible and without hazardous drop or obstruction. **Conflict requiring reconciliation:** the older Q1–Q118 authority specifies the crystal floating above the *exact-center* Ominous Vault; this newer owner decision changes their spatial relationship. Before implementation, update authoritative geometry/activation assumptions consistently. |
| WG-Q18 | **B**: four ancient masonry arch entrances, **7 blocks wide x 8 blocks high**, built from deepslate bricks and polished deepslate, with columns and lanterns. Intact variant has sound arches; ruined variant may show cracks while retaining clear access. |

## Implementation constraints and outstanding work

- This is **design synchronization**, not authorization to build or merge.
- **ST-05 generator and template do not yet exist**; PR #216 C4 mask storage/placement hook is not equivalent to worldgen integration. Record actual placed source positions for C4 only after successful generation.
- Do not mistake test-fixture geometry (e.g. radius 26) for approved geometry.
- Vanilla cheese caves are continuous noise-carved terrain, not discrete structure candidates. **75% per cavern is a target**, requiring deterministic identification of connected qualifying caverns and one roll per cavern; feasibility and realized density are unverified.
- Q10's 75% and Q11 minimum spacing may conflict in dense cave regions: define precedence and measure realized chance rather than claiming 75% realized placements automatically.
- WG-Q17 needs explicit reconciliation with the older exact-center Vault/crystal interaction contract before code changes.
- Need a live worldgen probe for cavern qualification, multi-chunk determinism, cave access, lighting, and all three architecture variants.
- Existing PR #213 controller and PR #216 C4 are not changed by this document.

**Provenance:** owner answers in the current 2026-10-30 ST-05-WG design conversation, WG-Q01–Q18.

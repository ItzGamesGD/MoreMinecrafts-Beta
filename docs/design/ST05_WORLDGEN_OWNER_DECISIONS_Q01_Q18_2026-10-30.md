# ST-05 Worldgen Owner Decisions — WG-Q01–Q45 (2026-10-30)

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
| WG-Q17 | **SUPERSEDED BY WG-Q19**. Earlier B proposed adjacent Ominous Vault; do not implement that geometry. WG-Q19 restores the exact-center vertical crystal/Vault arrangement. |
| WG-Q18 | **B**: four ancient masonry arch entrances, **7 blocks wide x 8 blocks high**, built from deepslate bricks and polished deepslate, with columns and lanterns. Intact variant has sound arches; ruined variant may show cracks while retaining clear access. |

| WG-Q19 | **A**: Ominous Vault directly **beneath the activation crystal at exact arena center**, preserving older combat authority. Vertical arrangement must not obstruct Vault interaction, item pickup or trial activation. Explicitly supersedes WG-Q17. |
| WG-Q20 | **B**: outer wall height **12 blocks**, upper dome height **11 blocks**, total **23 blocks**. Preserve 7x8 entrances. |
| WG-Q21 | **A**: concentric masonry dome of polished deepslate, bricks and tiles; intact symmetrical rings, ruined variant broken rings; natural-ceiling variant has no artificial dome. |
| WG-Q22 | **B**: selective terrain integration; intact architecture remains complete, ruined variant may blend some cave rock into outer walls. Never compromise combat floor, entrances, Vaults, crystal or protected structure blocks. |
| WG-Q23 | **B**: natural-cave-transition corridor; deepslate masonry near colosseum gradually yields to original rock toward cave. Walkable and within Q08 32-block traversable path cap. |
| WG-Q24 | **B**: corridor minimum **5-wide x 5-high unobstructed internal clearance**, tapering from 7x8 entrance; decoration cannot reduce clearance. |
| WG-Q25 | **B**: stairs plus level stretches accommodate cave-floor elevation differences; preserve 5x5 clearance and 32-block traversable route cap; reject impossible connections. Validate stair facing/half/shape. |
| WG-Q26 | **B**: selectively remove/block minor water/lava intrusions with masonry seals; reject major lava-lake interference. Combat floor and Vault area stay fluid-free after chunk reload. |
| WG-Q27 | **B**: structure collision via protected actual occupied blocks and mandatory passage clearances, not only coarse bounding-box overlap. Reject true overlap; when another structure's occupancy cannot be reliably determined, reject conservatively. Never overwrite another structure. |
| WG-Q28 | **B**: finalize full placement/connection/collision plan before per-chunk writes; all chunks use identical deterministic plan. Generation order and restarts must not change output or leave partial/duplicate arena. Feasibility in Minecraft 26.3 APIs requires verification. |
| WG-Q29 | **B**: preserve terrain beyond 4 blocks from outer wall except approved cave connection corridor and safe junction excavation/reinforcement; never damage other protected structures. |
| WG-Q30 | **B**: guarantee one valid cave connection, allow **up to two** if a second safe, short connection is available. Each independently obeys 32-block path, 5x5 clearance, stairs, fluids and collision conditions. |
| WG-Q31 | **B**: one 75% roll per eligible cavern; if selected, deterministically retry alternative placements/directions within same cavern with a finite budget; never re-roll probability for each candidate. |
| WG-Q32 | **B**: inspect **at most 8** placement candidates per selected cavern. Initial cap subject to seed-sampling and generation-time profiling, not a measured optimum. |
| WG-Q33 | **B**: rank eligible candidates for natural cave connection, terrain damage, fluid interference and architectural exposure; select highest quality, tie-break with deterministic seed-based order. Mandatory constraints are hard rejection, never traded for score. |
| WG-Q34 | **B**: balanced initial ranking weights: **cave connection quality 40%, terrain preservation 40%, fluid interference 10%, architectural exposure 10%**. Provisional design weights, not empirically optimal; validate and potentially recalibrate through sampled seeds. |

| WG-Q35 | **B**: absolute, fixed-reference normalization of each candidate's four score components to **0–100**. Weighted total S = 0.4C + 0.4T + 0.1F + 0.1E, with higher better. Never normalize only relative to the other candidates. Hard eligibility/safety gates precede scoring. |
| WG-Q36 | **B**: cave-connection component C = **40% traversable corridor length**, **30% elevation change/stair continuity**, **30% natural-cave junction quality**. Each submetric needs reproducible, measurable scoring definitions. These weights are *within* C. |
| WG-Q37 | **B**: terrain-preservation component T = **60% natural block modification amount**, **40% original cave geometry/connectivity preservation**. Track unavoidable arena construction excavation separately from optional correction; destroying mandatory traversable cave routes is a hard reject. |
| WG-Q38 | **B**: fluid-interference component F = **50% fluid removal/blocking work**, **50% re-entry risk** after block updates/chunk reload. Any unsafe seal or persistent combat-floor/Vault flooding is a hard reject, not a scoring tradeoff. |
| WG-Q39 | **B**: architectural-exposure component E = **60% discoverability of entrance from existing natural cave**, **40% architectural completeness of naturally exposed outer wall**. Keep arena primarily embedded in rock; do not excavate additional cave just to increase exposure score. |
| WG-Q40 | **B**: staged validation starts with **30 world seeds**; expand if qualifying-cavern samples are inadequate or outcomes unstable. Record functional integrity, cave access, fluid safety, restart/multi-chunk determinism, qualifying cavern counts, rolls, fit success, actual placements, generation latency and memory. Seed count alone is not statistical confidence. |
| WG-Q41 | **B**: validate separately **roll fraction** (winning rolls / eligible caverns actually rolled), **fit fraction** (successful placements / winning rolls), and **final fraction** (successful placements / eligible caverns actually rolled). Record spacing-excluded caverns separately and state denominator/eligibility explicitly. The 75% is the configured roll probability, not a guaranteed observed final rate. |
| WG-Q42 | **B**: implementer may independently optimize deterministic candidate ordering, cave-identification algorithm, and validation/runtime performance **without changing approved constraints**. Owner approval required for changing structure dimensions, 32-block path cap, 5x5 corridor clearance, 75% roll, 8-candidate maximum, structure/fluid/terrain safety constraints, or provisional scoring weights. Sampling may motivate a proposal, not automatic constraint changes. |

| WG-Q43 | **B**: deterministic work budgets, not elapsed-time aborts. Keep 8-candidate cap and bound inspected chunks/blocks/search nodes, with actual time, tail latency and memory measured separately. Exact operation budgets remain implementation parameters requiring profiling; outcomes must be seed/restart/order reproducible. |
| WG-Q44 | **B**: severity-based validation. **BLOCKER**: missing/duplicated or incomplete structures, impassable required cave connection, unsafe fluid entry, damage to other structures, or other reproducible safety/integrity failures; any reproduced blocker withholds final approval. **MINOR**: limited cosmetic/decor issues documented for conditional progression. Statistical sampling uncertainty is not automatically an implementation defect. |
| WG-Q45 | **B**: full design audit first, then staged implementation of architecture, cave connections and placement algorithm for audit-cleared scope, each independently validated. Final integrated/live acceptance is separate. Do not change owner-approved constraints to work around technical infeasibility; stop affected portion and seek owner approval for alternatives. **Owner explicitly forbids starting implementation yet**; this record and audit request are not implementation authorization. |

## Implementation constraints and outstanding work

- This is **design synchronization**, not authorization to build or merge.
- **ST-05 generator and template do not yet exist**; PR #216 C4 mask storage/placement hook is not equivalent to worldgen integration. Record actual placed source positions for C4 only after successful generation.
- Do not mistake test-fixture geometry (e.g. radius 26) for approved geometry.
- Vanilla cheese caves are continuous noise-carved terrain, not discrete structure candidates. **75% per cavern is a target**, requiring deterministic identification of connected qualifying caverns and one roll per cavern; feasibility and realized density are unverified.
- Q10's 75% and Q11 minimum spacing may conflict in dense cave regions: define precedence and measure realized chance rather than claiming 75% realized placements automatically.
- WG-Q19 explicitly supersedes WG-Q17: exact-center Ominous Vault directly under activation crystal. Ensure authority index and downstream implementation use Q19.
- Q31–Q39 require deterministic bounded candidate search, **fixed-reference 0–100 submetric definitions**, and sampling; never claim target 75% as measured placement rate.
- Q40–Q45 establish validation scope, separate probability denominators, severity gates, deterministic work budgets and an owner-approval gate for any changes to design constraints. No implementation is authorized by Q45. 30 seeds is a starting point, not statistical proof.
- Q27–Q28 collision/whole-plan behavior require technical validation under Minecraft 26.3 multi-chunk worldgen.
- Need a live worldgen probe for cavern qualification, multi-chunk determinism, cave access, lighting, and all three architecture variants.
- Existing PR #213 controller and PR #216 C4 are not changed by this document.

**Provenance:** owner answers in the current 2026-10-30 ST-05-WG design conversation, WG-Q01–Q45.

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

## Owner decision — underground navigation, 2026-10-26

**APPROVED:** Underground navigation pattern **A: one clear central main route with optional side branches**. A player can reach the royal burial chamber without clearing all optional crypts, storage rooms, treasure rooms, or secrets. Optional branches should reward observation and exploration rather than require confusing mandatory maze traversal. This supersedes any competing interlocking-maze or increasingly labyrinthine mandatory-route concepts. No fixed room count or floor count was approved by this decision.

### Underground layout proposal V1 — NOT YET OWNER-APPROVED

**Coordinate convention:** Pyramid ground-center X=0, Z=0; positive Z = south (entrance), negative Z = north. Ground datum Y=0, underground values negative. Exterior base footprint X,Z=-40..+40. All room footprints below are conceptual clear-space targets (not NBT coordinates); walls, stairs, foundations, transitions, door openings, ceiling slabs, collision clearance and worldgen safety still need validation. Never treat a schematic floor projection as rooms sharing the same elevation.

**Three architectural levels, four functional zones** (proposal, not mandatory 3 floors): L1 general burial zone, L2 grave goods + optional hidden crypt zone, L3 royal burial zone. Proposed **floor datums** L1=-5, L2=-11, L3=-17; suggested usable room clear heights ~4,4,3 blocks respectively, and a 1–2-block structural ceiling/floor separation where possible. The deepest placed blocks including floor foundations must not cross Y=-20. Exact datums require geometry and stair-run audit: each ~6-block descent at 1 rise/run needs ~6 horizontal blocks plus landings, and the above-ground grand stair must connect safely to L1.

**Proposed room program and internal clear dimensions (X width × Z length, blocks):**
- Main spine: U00 monumental descent / threshold, U01 L1 reception vestibule 11×9, U02 L1 burial gallery 9×21, U03 L1→L2 stair and landing, U04 L2 central passage 7×21, U05 L2→L3 stair and landing, U06 L3 royal antechamber 11×9, U07 L3 royal burial chamber 17×15. Main path remains continuous and visibly marked; no keys or optional chambers required.
- Zone A, general tombs (L1): A1 west small crypt 7×7, A2 west ossuary 7×9, A3 east family crypt 9×9, A4 east ritual supplies 7×7. All optional off U02; at least one legible route back.
- Zone B, grave goods (L2): B1 west pottery room 7×7, B2 west armory/storage 9×7, B3 east offerings 7×9, B4 east guarded treasure room 9×9. All optional off U04.
- Zone C, hidden annex (primarily L2): C1 concealed west passage, C2 sealed-looking but discoverable secret crypt 7×7, C3 concealed east passage, C4 secret treasure chamber 7×9. No mandatory access; exact discovery method, rewards and traps OPEN.
- Zone D, royal complex (L3): U06 antechamber, U07 primary tomb, D1 west burial annex 7×7, D2 east treasury annex 9×7, both optional; no boss or mandatory puzzle presumed.

This is a **program inventory** rather than a quota: the 8 main-spine named elements include stair/threshold passages; A(4)+B(4)+C(4)+D(2) = 14 branch elements, for 22 named spaces/elements total, not 22 closed rooms. Room counts, dimensions and zone allocations are proposed and can be reduced or rearranged after collision and experience review.

**Indicative horizontal allocation:** Main route generally near X=-5..+5; L1 side rooms at X≈-25..-10 and +10..+25; L2 branch rooms similar; royal chamber centered around X=0. South→north traversal for narrative clarity; descent transitions can fold sideways as necessary. Do not assume the conceptual sketch's side branches all occupy one Y plane. Maintain ≥2-block comfortable public passage where possible, and explicit turning/return landmarks. Outer pyramid foundation and cave collision may restrict underground room footprints; the approved 81×81 exterior does not guarantee underground empty volume.

**Design priorities:** Ground-level halls remain grand, elegant and sparse; underground density carries the exploration burden. Treasure frequency, enemy rosters, traps, secret mechanics, decoration palette and exact room graph remain OPEN. No gameplay/worldgen code authorized. Verify dimension envelope, excavation volume, 3D stair feasibility, cave/water interference, chunk crossing and save/reload determinism before a build-ready blueprint.

## Owner decision — underground encounter density, 2026-10-26

**APPROVED: B — balanced exploration and combat.** Preserve the grandeur and relative calm of the above-ground pyramid. Ordinary underground burial rooms prioritize atmosphere and discovery, with occasional small encounters. Valuable grave-goods chambers and the royal burial complex carry the more demanding encounters. Secret branches are optional, and the risk/reward gradient should be apparent to observant players. Do **not** put enemies in every room or make all optional fights mandatory. This approval sets encounter **philosophy**, not an approved enemy count, custom mob, loot item, probability, or trap implementation.

### Encounter, trap, and reward design V1 — PROPOSED / OPEN for owner review

**Baseline recommendation:** vanilla mobs first (Husks as desert-undead identity; Skeletons as ranged guardians; rare Spiders as ambient crypt threat), with no invented custom mummy or boss. Cave Spiders only if a deliberately readable nest and poison difficulty are explicitly approved. Do not automatically import the Pillager HQ commander or its 65%/35%/10% reward rules. Keep above-ground hall largely calm: optional 0–2 incidental defenders; avoid disrupting architectural sightlines. Suggested underground distribution for a complete generated structure, **not approved**:
- L1 general tombs / A: 3–5 Husks and 1–2 Skeletons total, dispersed across only some rooms; 1–2 intentionally empty atmospheric chambers.
- L2 goods / B: 4–6 Husks and 2–3 Skeletons; guarded treasure storage is a clearly signposted encounter.
- L2 hidden annex / C: 2–4 Husks, optionally 1 Spider; the secret route can also be a purely exploratory discovery, no guaranteed combat.
- L3 royal complex / D: 4–6 Husks and 2–3 Skeletons in antechamber/royal vicinity; avoid cramped mob stacking or inaccessible ranged attackers.
- This gives **16–25 underground mobs** depending on selections, but the arithmetic and biome/difficulty scaling must be rechecked after room geometry is fixed. No exact mob count, spawn persistence, elite attributes, boss, equipment, or difficulty scaling approved.

**Trap candidates — all PROPOSED:** (T1) visible arrow-dispenser lane with floor trigger and side bypass, especially B4; (T2) short, reversible falling-sand/gravel obstruction with guaranteed escape and no player suffocation trap; (T3) obvious cracked-floor drop to a shallow lower corridor with safe recovery, not lethal void/lava; (T4) decoy chest with a short-lived, escapable pressure-plate response. Each should be understandable, telegraphed, bounded, and reset-safe. No unavoidable instant kills, mandatory redstone puzzles, TNT chain destruction, persistent softlocks, or traps that irreversibly destroy the structure. Before implementation choose which traps are approved and whether vanilla redstone or script-controlled mechanisms are permissible; verify chunk loading and restart behavior.

**Reward ladder — PROPOSED, no approved item tables or rates:**
- Entry and A general tombs: minor archaeology-themed supplies, modest gold/iron, pottery, food or mundane materials; some rooms intentionally empty.
- B grave-goods branches: moderate gold, gems, enchantment-related equipment or appropriate existing mod loot; B4 clearly guarded premium candidate.
- C hidden annex: rarer thematic curios, treasure, or a meaningful accessory/rune **only if currently implemented and approved**; discoveries should feel worthwhile but never be required for main-route completion.
- D royal burial chamber: guaranteed meaningful baseline chest/reward concept plus optional higher-tier roll, with no guaranteed top-tier accessory or duplicated one-time special drop. Exact chest numbers, rarity, drop chance, item allowlist and any generated structure-specific treasure table remain OPEN.
- Avoid restock/exploitable repeat rewards; specify one-time vs repeatable generation and persistence explicitly before coding.

**Room-by-room V1 placement suggestions:**
- U01/U02 entrance/gallery: visual storytelling, no forced ambush; A1 empty crypt; A2 one small Husk encounter; A3 mixed tomb with one guardian; A4 supplies, optional low-value chest.
- U04 L2 central passage: legible route and quiet breathing room; B1 pottery storage, minimal risk; B2 one guarded store; B3 optional material reward; B4 conspicuous high-value guarded chamber with a single readable trap candidate.
- C1/C3 secret approaches: environmental hints, no compulsory combat; C2 sealed-looking tomb with moderate risk; C4 secret treasure chamber, optional reward.
- U06 royal antechamber: main encounter; U07 royal burial chamber: culminating treasure and smaller follow-up threat or vice versa (choose after room geometry); D1/D2 optional royal side crypt/treasury.
- Preserve the approved central spine: no key hunt, secret switch, side-room kill count, or mandatory hidden chest to open U07.

**Balance / technical gates:** Validate difficulty-scaled vanilla AI, mob placement collision, no persistent unintended natural respawn, generated chest inventories and deterministic loot seeding, trap activation/reset across chunk unload/restart, player recovery from drops, accessibility without secret detection, structural protection from TNT/falling blocks, and absence of interference with nearby caves and player builds. Distinguish structure-generated guardians from vanilla ambient spawns; no persistence policy approved yet.

**Owner decisions still required:** (1) vanilla-only vs new unique guardian, (2) combat counts and whether one-time defenders, (3) selected trap families and severity, (4) premium reward categories / actual item allowlist, (5) royal tomb finale style (elite encounter, ordinary guards, or environmental climax). No code, NBT, or loot table modifications authorized by this documentation update.

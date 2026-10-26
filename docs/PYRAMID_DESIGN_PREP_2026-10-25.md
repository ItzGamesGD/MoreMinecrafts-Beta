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

## Owner decision — royal tomb boss trigger, 2026-10-26

**APPROVED, superseding previous noise-stack proposal:** The royal tomb's unique boss is triggered by interacting with the **primary royal treasure chest**, not by building up a long-lived 100-point noise meter while walking/exploring. The prior walking/jumping/mining/combat noise accumulation mechanic is **REJECTED for this boss**. The intended pacing is prompt: after chest interaction, a short warning and a near-immediate appearance from beneath the floor. The boss is a massive, brutal armored golem-like guardian with a dark helmet and **two red glowing eyes**. Use a low-budget Fabric model approach that references/reuses Warden model anatomy/animation where technically viable, adding 3D armor-like cuboids; this is a separate custom entity, **not** a reskin applied to vanilla Wardens. It uses conventional sight-based monster combat, **not** Warden blindness, sonic boom or vibration-tracking AI. The chest trigger does not make ordinary optional tomb chests mandatory.

**PROPOSED interaction sequence, not yet approved in exact timings or chest access policy:** First player interaction with the royal primary chest -> prevent ordinary loot extraction during awakening -> 3–5 seconds of stone rumble/dust/cracks -> spawn/animate guardian at a prevalidated fixed point with no real terrain destruction -> normal boss combat -> unlock primary reward chest on defeat. Treat the chest lock/unlock policy, whether the player can flee, encounter reset after death/unload, one-time per structure persistence, anti-duplication, and any multiplayer contention as OPEN technical/gameplay decisions pending owner approval. The user agreed to a short warning/appearance concept but did not separately approve every suggested chest restriction.

**Implementation prerequisites:** prototype armored Warden-derived visual without altering vanilla Warden renderer; verify actual model-part hierarchy, eyes emissive render layer, hitbox and model proportions, animation and spawn clearance in current Fabric/Minecraft version. No code authorized by this document.

## Owner approval — royal chest unlock gate, 2026-10-26

**APPROVED:** The **primary royal treasure chest is sealed until the unique Tomb Guardian boss is defeated**. First interaction with the sealed chest starts the encounter; the player cannot remove its loot before defeating the guardian. Use the previously agreed short warning (~3–5 seconds as an initial target, not frame-locked), rumble/dust, then apparent emergence from the floor without destructive terrain edits. Defeat removes the seal and allows the chest to open. Other optional tomb chests remain independent and must not inherit this boss gate. The old cumulative noise meter remains superseded.

**Encounter state machine proposal (technical HOW, not owner-locked):** DORMANT -> AWAKENING -> ACTIVE -> DEFEATED / UNLOCKED. Guard first-interaction concurrency; server authority for loot lock and boss spawn; persist structure identity and state across unload/restart; make activation idempotent; avoid duplicate bosses and repeated reward generation. **OPEN:** death/despawn/reset policy, escape behavior, multiplayer rules, encounter recovery after crash, exact animation duration, boss stats, abilities, and loot contents. Do not code before implementation authorization.

## Owner decision — Tomb Guardian enrage, 2026-10-26

**APPROVED: Simple enrage (Option A).** When the guardian drops to **40% HP or lower**, it enters a single enrage phase: its two red eyes glow more intensely, attack cadence becomes modestly faster, and movement speed increases slightly. No additional complex move, larger shockwave, or mandatory new animation is implied. This is an encounter-design approval, not authorization to implement code. Keep the guardian a heavy, armored, sight-based melee boss with a clearly telegraphed ground slam, chest-triggered awakening, and royal chest unlocked only after defeat.

### Balance V1 — proposals, NOT owner-approved numeric values

- Suggested HP: 180 (90 hearts); enrage at <=72 HP for this HP choice.
- Suggested baseline: melee damage 10 HP, melee interval ~2.0 s, movement speed slower than zombie, knockback resistance ~80%, sight detection ~24 blocks.
- Suggested slam: 12 HP with 1.2 s readable wind-up, safe dodge/retreat zone; no actual block destruction. Radius/cooldown still OPEN.
- Suggested enrage: movement x1.15 and melee cadence ~1.7 s; leave damage, slam radius, slam telegraph and knockback resistance unchanged. Actual implementation should derive phase threshold from max HP rather than hard-code 72 HP.
- Suggested arena: 17×15 clear floor for royal chamber is provisional, not validated against ~4–5 block tall armored model, hitbox, columns, player dodging, doorway choke and projectile safety. Evaluate whether room needs expansion.
- Avoid compulsory damage from unblockable instant attacks; preserve vanilla armor/shield interactions unless owner approves exceptions. Test solo baseline at plausible mid/late-game gear, multiplayer, high armor, melee/ranged kiting, accessibility and server lag.

### Rewards V1 — proposals, NOT owner-approved loot table or rates

**Guaranteed completion concept:** defeating the guardian changes the unique structure state to DEFEATED and opens the primary royal chest; the chest should contain meaningful but bounded guaranteed treasure (e.g. gold/emerald/diamond resources and thematic rare material). This is the reward for boss defeat, not a second infinitely farmable boss drop. **Optional premium roll:** a separately weighted bonus slot can draw from existing implemented artifacts/accessories/runes only after checking catalog and progression; avoid guaranteeing an unimplemented or top-tier item. Keep ordinary side crypt chests independently lootable before boss fight, and secret chambers worthwhile without being mandatory.

**Open owner choices:** exact guaranteed reward identity/amount; whether a guardian-specific unique item exists; premium categories and probability; boss entity drops vs chest-only; one-time/repeatability; death/escape/unload/reset rules; whether arena blocks entry/exit. These must be explicitly approved before implementation.

**Technical acceptance gates:** client screenshot confirms armor cuboids and helmet-recessed two emissive red eyes; no mutation of vanilla Warden; actual Fabric version model-layer and animations compile; server-authoritative chest lock and boss single-spawn; phase transition exactly once at 40% HP; persistence after unload/restart; multiplayer chest contention; death/unload recovery; no unintentional structure damage; combat and reward exploit QA.

## Owner decision — Tomb Guardian unique reward category, 2026-10-26

**APPROVED (Option B):** The Tomb Guardian has exactly **one unique crafting-material item type** as its boss-exclusive signature reward. Do not replace it with a boss-exclusive artifact/accessory. Item name, quantity, exact acquisition, recipe uses and rates remain unapproved.

### Reward proposal V2 — NOT OWNER-APPROVED

- Working name: 왕릉 수호자의 핵 / Tomb Guardian Core.
- Suggested acquisition: one guaranteed core per newly completed pyramid, awarded through the primary royal chest unlocked on boss defeat. Persist reward claim and guard multiplayer duplication.
- Suggested royal chest resources for balance testing: 8–16 gold ingots, 4–10 emeralds, 1–3 diamonds; compare against rare trader economy before approval.
- Suggested optional premium roll: a low-probability extra from existing, verified registered/implemented runes or artifact/accessory materials. Do not promise unimplemented items.
- Side crypts retain independently accessible loot; secret branches offer extra risk/reward without becoming required.
- Possible material uses: a bounded existing gear/accessory upgrade ingredient or themed utility recipe, avoiding dead-end materials and best-in-slot progression bypass.
- **OPEN owner choices:** final name/texture, quantity and acquisition, exact resource table, rare roll probability/categories, concrete crafting use, structure repeatability/respawn/reloot.

**Existing approved rules preserved:** sealed primary chest triggers the short warning and guardian emergence; unlock only after boss defeat; no cumulative noise meter; at <=40% HP guardian has simple enrage with slightly faster movement and attacks and brighter red eyes.

## Progression-fit review — boss material use, 2026-10-26

**Owner design direction:** Evaluate links to existing equipment, existing progression materials, or newly designed materials/artifacts; prefer an existing well-fitting growth loop, and invent a new system/item only if no suitable existing fit exists. The owner has **not** yet approved a specific recipe, output amount, exclusive drop policy, or new artifact.

**Verified existing fit:** The approved accessory enhancement design in ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md already requires a Relic Catalyst at +5 and +10 (one at each milestone is provisional), and explicitly leaves its acquisition sources UNDESIGNED. ACCESSORY_ANVIL_CATALYST_LAYOUT_APPROVED_2026-10-23.md documents the existing anvil catalyst input and a focused +4-to-+5 test. Ordinary T1/T2 equipment enhancement uses Enhancement Stones and does not use Relic Catalyst. Thus catalyst acquisition is a genuine open progression link; avoid adding a parallel mandatory enhancement currency.

**RECOMMENDED PROPOSAL, not owner-approved:** One Tomb Guardian Core from the first completion of each pyramid may be crafted into **two Relic Catalysts** (conversion rate illustrative). This uses the already approved one unique boss crafting-material type without inventing a new exclusive artifact. The core may use a straightforward crafting recipe with existing materials; exact recipe, acquisition placement, rate and item name remain OPEN. Relic Catalyst must also have **independent alternative acquisition sources** (other dungeons/trials/rare rewards) so the pyramid does not become a mandatory gate for all accessory +5/+10 enhancement. Do not convert the boss core directly into a universally required +5/+10 catalyst exclusive to this structure. Verify actual item registry, crafting and accessory enhancement runtime before implementation.

**Alternative designs for owner choice:** (A) core -> catalysts, with alternative catalyst sources elsewhere (preferred); (B) core as optional substitute for a milestone catalyst, not exclusive gate; (C) core as a new artifact/utility item ingredient only if its gameplay role is distinct and justified. The prior decision to have one boss-exclusive crafting material still stands; any new artifact would be crafted from that material unless owner explicitly supersedes it.

**Still open:** final material name and appearance, core quantity, output quantity, recipe, alternative acquisition sources, treasure chest resource table, rare bonus pool and all implementation authorization.

## Owner direction — themed elite trophies as catalyst ingredients, 2026-10-26

**APPROVED DIRECTION (not final recipes):** Give elite and themed mobs individual thematic trophy drops, analogous to ordinary mob loot, and combine these drops with other existing items to craft the already-defined Relic Catalyst used for accessory milestone enhancement. This is a cross-content monster-loot/crafting loop, not an exclusive pyramid-only catalyst source. The Tomb Guardian retains its one signature crafting-material type; that item should participate in the shared catalyst ecosystem. Do not introduce a new catalyst currency per monster or require all trophy types in one recipe.

**Design proposal for owner review:** Different trophy recipes may share a catalyst output while demanding different ingredient quantities according to encounter difficulty; elite/boss encounters should feel more rewarding through better drop reliability, quantity, or conversion yield, not merely by inventing stronger mandatory catalyst tiers. Ordinary thematic enemies can have low/moderate chances and modest conversion efficiency; unique bosses can guarantee their signature trophy and yield more catalysts. Avoid common-mob farming becoming faster than dungeon progression. Exact roster, loot probabilities, crafting recipes, yields, eligibility of existing elite mobs, and chest-vs-entity drops remain OPEN.

**Clarification needed:** Owner's final phrase about elite-monster trophy rewards was truncated; do not presume the exact reward premium mechanism until confirmed. Prior numerical suggestion 'one core -> two catalysts' remains unapproved and may be superseded by this broader design.

## Owner clarification — elite signature equipment rewards, 2026-10-26

**APPROVED DESIGN INTENT / supersedes prior ambiguity:** Elite enemies should offer **directly useful, thematically expressive rewards**, such as a unique piece of equipment, artifact, immediately usable core, or signature crafting ingredient for equipment that inherits the elite enemy's fighting style. The intended distinction is qualitative (distinctive gameplay reward), not merely more ordinary drops or better catalyst conversion efficiency. The same thematic trophy may optionally also feed the common Relic Catalyst crafting ecosystem, allowing a choice between broad progression and a unique reward. Do not infer that all elite drops must be direct usable items: a dedicated ingredient for a meaningful thematic piece of gear qualifies.

**Tomb Guardian fit assessment:** Its heavy downward smash and ground shockwave have a particularly close thematic match with the **existing Earth Mace**. STAGE_2_GAMEPLAY_RECONCILIATION.md records Earth Mace's vanilla-valid smash-triggered radial ground wave and its separate right-click directional shockwave; automated T2 acceptance was reported but live gameplay acceptance and final balancing remain pending. Prefer exploring a **non-mandatory Earth Mace variant / signature upgrade** using the guardian's unique core, without changing ordinary T1/T2 +20 Enhancement or inserting a mandatory boss gate. The special variant, effect, recipe, item registration, stats and crafting implementation are **PROPOSALS ONLY**, pending owner approval. Alternatives: a separate heavy mace (risk: overlap with Earth Mace), or a smash-themed artifact (risk: interaction with existing accessories).

**Potential player choice:** guardian core -> generic Relic Catalyst recipe OR guardian core -> thematic Earth Mace upgrade/variant, with neither conversion rate nor equipment design approved yet. Preserve exactly one boss-exclusive material item type unless owner explicitly changes that rule. Do not modify gameplay code until authorized.

## Owner direction — Tomb Guardian Core as Earth Mace crafting ingredient, 2026-10-26

**APPROVED thematic link:** The Tomb Guardian's unique crafting material should serve as an ingredient in the **existing Earth Mace crafting recipe**, rather than defaulting to a new Earth Mace variant or unrelated artifact. Preserve the existing Earth Mace identity and combat abilities. This supersedes the preceding proposal of a special variant as the preferred primary use, but does not yet lock a specific recipe or authorize code changes.

**Owner balance requirement:** Obtaining the Earth Mace through this encounter must be a meaningful achievement. Two viable directions explicitly raised: require defeating elite mobs **multiple times**, or promote the encounter's difficulty/classification from elite to **boss**. Do not treat a single ordinary elite kill as sufficient for a powerful T2 item without further balancing. The Tomb Guardian already has a unique royal-chest-triggered arena encounter and two-phase boss design; boss classification is a natural candidate, but the final combination of boss difficulty, required core count, other ingredients, and crafting rarity remains **OPEN**.

**Recommended candidate, NOT APPROVED:** treat Tomb Guardian as a true pyramid boss and use one guaranteed core plus other significant but attainable crafting components for the Earth Mace; compare against existing actual Earth Mace recipe and T2 progression before locking amounts. Alternative: require 2–3 cores from distinct pyramid clears (potential repetitive world exploration); do not quietly add respawns or boss farming to an originally one-time per-structure reward. Verify whether other sources or upgrade paths for Earth Mace already exist to avoid destructive recipe replacement.

**Additional balance checks:** difficulty at intended equipment tier, whether boss rewards a weapon needed to fight itself, progression sequence, worldgen frequency and multiplayer access, resource economy, optional catalyst conversion of surplus cores, compatibility with existing enhancement levels and enchantments, and loot duplication safeguards.

## Owner decision — Earth Mace requires additional ingredients, 2026-10-26

**APPROVED:** Crafting the existing Earth Mace from the Tomb Guardian signature core must ALSO require other crafting ingredients. The core alone must never craft the Earth Mace. The goal is to preserve a meaningful T2 acquisition journey while linking the pyramid encounter to the mace's ground-smash identity. This decision does not finalize the guardian's classification, exact core count, other ingredient identities/quantities, or whether the current recipe is replaced versus supplemented.

**Authority conflict to resolve before implementation:** CONTENT_AUTHORITY_INDEX.md currently states that the five T2 elemental core identities/acquisition sources are locked by the Oct 7 record, and that equipment recipes/links are not authorized without demand. The Earth Mace is already a registered T2 item. Audit its actual recipe, T2 core mapping, and any research gates before deciding whether the guardian core is a new mandatory component or an alternate recipe. Do not silently override prior T2 core progression.

**Illustrative recipe architecture, NOT approved quantities:** Tomb Guardian Core x1 + appropriate existing T2 elemental core/earth-linked ingredient x1 + a durable vanilla base material (e.g. heavy core or other verified item) + secondary mined resource(s) -> Earth Mace. Verify that every ingredient exists, is obtainable in intended game version, and does not force an unintended second boss or impossible progression. Prefer preserving existing T2 core relevance, avoiding duplicate mandatory boss gates, and allowing a fair balance of combat, exploration and gathering.

**Open:** boss-tier promotion vs repeat kills, existing Earth Mace recipe compatibility, precise recipe grid, catalyst alternative use, core drop/claim behavior, exact tier and resource amounts, implementation approval.

## Owner authorization — freely propose initial Earth Mace recipe, 2026-10-26

**APPROVED DESIGN FREEDOM:** Design the Earth Mace recipe from scratch, with the Tomb Guardian signature core and additional ingredients; do not presume an existing locked recipe. This is permission to propose recipe design, NOT to implement gameplay code or to lock every ingredient without owner review.

**First concrete recipe PROPOSAL (not yet approved):**
D = diamond block, N = netherite ingot, C = Tomb Guardian Core, H = vanilla Heavy Core, . = empty.
```
D N D
. C .
. H .
```
Output: existing Earth Mace x1. Costs: diamond blocks x2, netherite ingot x1, guardian core x1, Heavy Core x1. The recipe deliberately connects pyramid boss, Trial Chambers, and late-game mining. **Balance warning:** Heavy Core is exceptionally rare and the two-boss/structure progression burden may be too steep for T2; compare alternatives using breeze rod or other accessible components before approval. Check exact runtime item IDs and recipe absence/presence before implementing.

**OPEN:** whether Heavy Core is appropriate, final layout, counts, boss classification, guardian core acquisition, duplicate existing recipe handling, implementation approval.

## OWNER APPROVED — Earth Mace shaped crafting recipe, 2026-10-26

The owner explicitly selected the original high-cost recipe using a vanilla Heavy Core (NOT the proposed Breeze Rod alternative), with **Heavy Core and Netherite Ingot swapping positions**. The exact approved 3x3 layout is:

```
Diamond Block | Heavy Core         | Diamond Block
Empty         | Tomb Guardian Core | Empty
Empty         | Netherite Ingot   | Empty
```

Output: **Earth Mace x1**. Total ingredients: Diamond Block x2, Heavy Core x1, Tomb Guardian Core x1, Netherite Ingot x1. This supersedes the earlier provisional D-N-D / .-C-. / .-H-. proposal and the Breeze Rod alternative. The shaped positions are significant; do not use a shapeless recipe. Preserve the existing Earth Mace combat functionality. This approves recipe DESIGN only; no gameplay code changes until separately authorized. Verify actual recipe registration/item IDs before implementation and flag any conflicting existing recipes rather than silently overwriting.

## Owner approval — guardian core obtained on kill; boss balance proposal, 2026-10-26

**APPROVED:** Tomb Guardian Core is obtained **by killing the Tomb Guardian**, not by opening its royal chest. The royal chest interaction remains the encounter trigger; the chest's other loot remains independent and may unlock after the boss dies. The exact quantity, per-player/per-boss ownership, and inventory-vs-world-drop delivery need technical and owner decisions; default design proposal is one core per successful boss kill, with no duplication.

**PROPOSED COMBAT BALANCE (NOT OWNER-APPROVED NUMBERS):** Treat the guardian as a deliberate, heavyweight pyramid boss intended for a prepared solo player in diamond/netherite-tier equipment on Normal difficulty. Max HP 200; melee damage 10 HP raw with 0.65s telegraph; ground slam raw damage 12 HP with a 1.3s windup and a visible outward-traveling ground shockwave, radius 6 blocks, ~0.8-block wave thickness, ~9s cooldown. Ground wave should be jumpable or avoidable by leaving its radius, with no permanent block destruction. Melee interval ~2.2s; knockback resistance ~80%; ordinary movement 100%. At <=40% HP, owner-approved simple enrage raises movement speed ~15% and reduces attack interval to ~1.9s, ground-slam windup ~1.1s and cooldown ~8s; **no new attacks and no increase in wave radius**, per earlier owner direction. Enrage numbers are provisional; visual red-eye intensification is already approved. Avoid simultaneous stacked melee and shockwave damage, offer punish windows, prevent safe unreachable ranged cheese without forced teleports or unavoidable damage.

**Gameplay proposal:** Ordinary punches blockable with shield, shockwave best countered with jump/distance rather than being trivialized by shield. Boss remains sight-driven, not Warden's vibration system. Single active guardian per tomb; persistent defeat/loot state and multiplayer duplication protection required. Verify arena geometry, pathfinding, animation hit frames, actual armor-mitigated damage, armor progression and 1/2+ player combat in live tests.

**OPEN:** final numbers, difficulty classification formalization, loot core count, boss reset/death/relog handling, shield behavior, ranged anti-cheese details, precise encounter mechanics and implementation authorization. Preserve approved Earth Mace recipe D-H-D / .-C-. / .-N-. and its boss-core-on-kill dependency.

## Owner-approved new attack identity — Stone Eruption, 2026-10-26

**APPROVED:** Add a distinct Tomb Guardian boss attack in which **rock spikes/pillars erupt upward from the floor**. This supplements the existing heavy melee and outward-moving ground shockwave, providing a location-targeted dodge challenge and a fuller boss moveset. The previously approved <=40% HP enrage remains a **speed/tempo intensification only**, not a phase that introduces a new attack; Stone Eruption is available in both phases.

**PROPOSED, NOT APPROVED, initial tuning:** Guardian plants a hand; a visible cracked-floor telegraph marks a target location fixed when the warning begins, then stone erupts ~1.2 seconds later within radius ~1.5 blocks for 8 raw HP damage. Cooldown ~11 seconds, shared attack-selection rules to avoid simultaneous undodgeable melee/slam/eruption damage. Telegraph must be readable at all supported graphical settings. The eruption uses temporary non-destructive geometry/particles/visual entities, not persistent world block replacement; collision and damage windows are bounded and tested for multiplayer. Allow side-step evasion; do not continually retarget after telegraph placement. Adjust timing/damage after live tests.

**Attack identity:** melee = close-range punish; ground slam = expanding wave, jump/distance evade; Stone Eruption = fixed-position delayed spike, lateral movement evade. Do not conflate ground slam and Stone Eruption into one unavoidable combo. Exact targeting frequency, whether multiple pillars appear, animation and numbers remain OPEN pending owner review and testing.

## OWNER APPROVED visual identity — inscribed rising stone statue, 2026-10-26

The Stone Eruption attack must visually manifest as a **long, narrow ancient stone statue/pillar engraved with Egyptian-hieroglyph-like characters**, rising vertically out of the arena floor. This supersedes the generic jagged rock spike visual in the earlier proposal. Aim for an obelisk-like elongated angular stone form, visible engraved glyphs on its faces, dust/stone-fragment emergence and a grounded ancient royal-tomb aesthetic. Exact model proportions, palette, glyph designs, animation duration and name remain artist/owner-review details. Preserve the approved positional telegraph/dodge gameplay concept; no permanent world terrain destruction. The glyphs are a fictional Egyptian-inspired visual motif, not a claim of authentic translation. No gameplay code or asset implementation is authorized by this design note alone.

## FINAL OWNER DESIGN LOCK — Tomb Guardian encounter, 2026-10-26

**OWNER APPROVED:** Lock the previously discussed Tomb Guardian encounter concept and proceed to **implementation preparation / feasibility audit**, not automatic gameplay implementation. The boss is the royal-tomb guardian in the separate colossal pyramid. Royal treasure chest interaction initiates the encounter; boss is sight-driven, heavy armored Warden-inspired custom entity with red glowing eyes, not vanilla Warden vibration AI. Attacks: (1) heavy melee fist, (2) ground smash sending an outward-moving floor shockwave, (3) a fixed-position telegraphed **long narrow Egyptian-hieroglyph-inspired inscribed stone statue/obelisk rising from the floor**. **A clearly visible magical summoning circle (마법진)** appears at the marked eruption site before the statue rises; this circle is a mandatory visual tell, not an optional flourish. A readable glyph-ring motif and dust/stone emergence communicate the tomb magic; do not rely solely on color to convey danger. Players can evade by moving away from the fixed marked location. The statue is temporary visual/collision geometry and does not permanently edit the world.

Enrage at <=40% HP brightens red eyes and accelerates movement/attack cadence, but introduces no extra attacks or wider shockwave. Distinct tells and recovery windows, no unavoidable simultaneous hits. Boss defeat directly awards/drops the **Tomb Guardian Core**; royal treasure chest unlocks after defeat. The **Earth Mace shaped recipe is locked**: Diamond Block / Heavy Core / Diamond Block; empty / Tomb Guardian Core / empty; empty / Netherite Ingot / empty -> Earth Mace x1. Preserve the existing Earth Mace mechanics. The signature core may also be part of future common Relic Catalyst recipes, but that recipe is not locked.

**PREPARATION GATE:** Audit exact active branch, code item/entity/recipe registries, existing Earth Mace recipe status, worldgen arena footprint and spawn/loot persistence, Warden-derived model feasibility and client rendering, magic-circle readability across graphics settings, multiplayer/respawn/escape/restart edge cases, no terrain destruction, and CI/live test plan. Deliver a staged implementation plan with blockers and proposed test criteria. Do not merge, mark Ready, or change gameplay code without a separate owner implementation instruction.

**NUMBERS REMAIN PROVISIONAL despite encounter design lock:** 200 HP, 10 HP melee, 12 HP shockwave, 8 HP statue, 40% enrage threshold (threshold previously approved), ~1.2s statue warning, radius ~1.5, cooldowns, drop quantity one, boss classification naming, and any detailed hitbox/timing or shield policy. Report balance values for final review or testing; do not silently treat them as locked numeric authority.

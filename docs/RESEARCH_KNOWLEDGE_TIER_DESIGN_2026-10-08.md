# MoreMinecrafts — Knowledge / Research / Quest Design Snapshot (2026-10-08)

> Design-only backup. Research taxonomy and tier hierarchy are LOCKED; individual research rewards, level caps, values and dependencies are PROVISIONAL. This snapshot is separate from the mod/modpack inspiration-extraction workflow. Do not modify implementation or main branch.

## Locked: system principles
- Knowledge is persistent evidence/understanding earned from gameplay; it determines eligibility to research.
- Research consumes materials + Minecraft XP; rewards include passive capabilities, deterministic workflow improvements, and newly unlocked crafting recipes.
- Reverse-design knowledge prerequisites **after** defining valuable research outcomes.
- Do not lock existing vanilla recipes/features behind research; new improved recipes/devices may require it.
- Quests supply XP through one-time activities/challenges; no daily reroll/shop system decided.
- Keep compatible with existing T1 enchant skill trees, elemental weapons, custom fishing, portable furnace, mining drill, fish trap, and 40 custom mobs. Avoid duplicate capabilities and unchecked enchant stacking.

## Locked: seven research disciplines
1. Resource Studies (자원학): mining, lumbering, gathering.
2. Production Engineering (생산공학): crafting, smelting, processing.
3. Living Technologies (생활기술학): agriculture, livestock, fishing, daily-life automation; includes applicable machinery.
4. Combat Studies (전투학): weapon operation, defense, combat technique.
5. Ecology (생태학): creature traits, weaknesses, species-specific responses.
6. World Studies (세계학): exploration, trade, archaeology, mobility.
7. Alchemy & Magic (연금·마법학): brewing, magic, elemental applications.
Machinery/redstone topics may be allocated by the actual improvement to Production Engineering or Living Technologies; do not invent an eighth discipline without user approval.

## Locked: research tier hierarchy (separate from equipment T1–T4)
- T1 **basic ability**: small, often passive/probabilistic benefits during existing gameplay, e.g. a chance of extra ore drop. Not necessarily random.
- T2 **operational efficiency**: tangible, more reliable or deterministic intervention in how an action/tool operates; not merely larger T1 numbers. Example: harvest and replant in one operation.
- T3 **applied recipe**: unlock an optional new item/block/device/recipe with high situational value, e.g. 3x3 mining equipment or a 5x furnace. T3 is not necessarily a universally stronger passive.
- T3 unifies the earlier separate 'new recipe' and 'new device' reward categories.
- Branching skill-tree topology analogous to enchantment trees. One research may fork to several others, and multiple paths can connect; a rigid three-node ladder is not required.
- **Research tier vs level are separate.** A single research can progress T1-1, T1-2, ... T1-N (and where meaningful T2-1 ... T2-M), adding incremental numeric effects. **Maximum level is set per research**, not globally or per discipline. T3 generally single unlock, with distinct recipes as branches rather than arbitrary numerical leveling; this is a design recommendation, not an absolute ban.
- Each level is a research purchase using materials + XP. Precise costs, caps, numerical values, unlock prerequisites and whether previous research must be purchased remain open. Prefer knowledge-gated independent unlocks except justified prerequisites (recommendation, not locked).
- Examples such as 2% per level / max 5, +damage, 5x furnace speed, and 3x3 mining are illustrative candidates, not locked balance.

## Provisional content pool (88 ideas, not 88 approved implementations)
Resource R01–R08: ore vein analysis; improved wood conversion; resource sifting; precision gathering; geological guide; wide excavation; mineral refining; special block recovery.
Production R09–R20: 5x furnace; parallel smelter; advanced logistics; fuel-efficient furnace; stonecutting; woodworking; bulk crafting; advanced autocrafting; equipment servicing; fuel processing; redstone signal control; building material processing.
Living R21–R34: crop breeding; husbandry; fishing technique; soil enrichment; irrigation; beekeeping; mushroom cultivation; feed; automated breeding; aquaculture; fish trap improvement; food processing; food preservation; daily-life automation.
Combat R35–R46: sword technique; shield technique; axe combat; archery; crossbow; throwing weapons; mobile combat; underwater combat; siege; special weapons; ammunition; armor operation.
Ecology R47–R59: undead analysis; illager analysis; arthropods; explosive creatures; nether mobs; end mobs; aquatic hostiles; slime; loot analysis; behavior guide; rare mobs; bosses; mob-material crafting.
World R60–R75: terrain survey; cave exploration; sailing; riding; flight; dimensional exploration; archaeology; ruins; maps; survival; transport/storage; villager trade; piglin barter; village systems; trial chambers; dimensional ruins.
Alchemy R76–R88: advanced brewing; catalysts; concentrated potions; splash; lingering; status effects; magical materials; XP application; soul materials; elemental analysis; elemental synthesis; magic devices; enchantment support.

## Example tree sketches (provisional)
- Resource > ore: T1 bonus ore chance (individually levelable) -> T2 direct mining workflow change -> T3 3x3 excavation recipe (watch existing drill).
- Living > crops: T1 bonus harvest chance -> T2 harvest-and-replant -> T3 irrigation device.
- Living > fishing: T1 catch-related bonus -> T2 meaningful mini-game operational improvement -> T3 aquaculture recipe (protect existing fishing balance).
- Production > smelting: T1 chance to save fuel -> T2 improve how furnace fuel cycles are used -> T3 5x furnace / fuel-saving / parallel furnace recipes (different tradeoffs).
- Combat > weapon: T1 small conditional advantage -> T2 different weapon operation -> T3 specialized equipment recipe only if meaningful.
- Ecology > undead: species knowledge -> levelable undead damage bonus; account for Smite and overlapping mob taxonomies.

## Outstanding decisions / audit flags
- Convert the 88 brainstorm entries into real tiered, branching research; prune redundant or vague entries, do not force every idea into a tree.
- Distinguish T1 probabilistic baseline from T2 operational mechanics with concrete examples; avoid plain 5% -> 10% disguised as tiers.
- Assign research-specific level caps and XP/material costs later; calibrate quest XP supply.
- Design knowledge prerequisites last, using real gameplay achievements/discoveries.
- Decide multiplayer ownership and sharing of recipe unlocks and devices.
- Avoid vanilla obsolescence and clashes with Fortune/Efficiency/Smite, enchant skill tree, Crafter, portable furnace (3x throughput and 3x fuel), Mining Drill, Fish Trap.
- Audit underrepresented vanilla activities: building/decor, cooking, villagers, beacon, dimensions, archaeology, trial chambers, etc.
- No implementation changes authorized by this snapshot.


==================================================
RESEARCH / KNOWLEDGE DESIGN UPDATE — 2026-10-08
Authority: user-confirmed design discussion. This section supersedes conflicting older brainstorm and 88/154-candidate notes. No gameplay implementation authorized.
Legend: [LOCKED] direct user decision; [PROPOSAL] assistant specification not explicitly ratified; [PENDING] unresolved; [REMOVED] excluded.
==================================================

FOUNDATION [LOCKED]
Knowledge = permanently acquired gameplay discoveries, serving as research prerequisites. Research spends materials + XP to unlock benefits; quest/challenge XP is one-off. Reverse-design: tangible reward first, then knowledge prerequisites, costs and economy. Never gate existing vanilla recipes. Seven disciplines: resources, production engineering, living technology, combat, ecology, world studies, alchemy/magic. T1 baseline benefit; T2 existing interaction/operation change; T3 new recipe/device, with exception explicitly noted below for chest UI-only upgrade. Tier levels/caps vary by research. Do not duplicate vanilla enchantments, vanilla Crafter, existing drill, portable furnace, fishing systems, fish trap, or custom mobs. Candidate ideas not marked LOCKED are NOT implementation approvals.

1. RESOURCES
[LOCKED T1] Ore mining, lumbering, gathering, mineral preservation are adopted research names; individual effects/levels still require balance and enchant-overlap audit.
[LOCKED T2] Prior vein mining/tree felling candidates not adopted due to enchantment duplication. Do not fill by force.
[LOCKED T3] Wide excavation research retained; must differentiate from existing single-block FP Mining Drill. Forest operation research is an INSTALLABLE automated lumber device, NOT another tree-felling enchant or axe.
[LOCKED forest device] Dedicated craft recipe, 1x1 or 1x2 block footprint undecided. Place anywhere within target chunk. Right-click UI with axe slot and fuel slot. Processes trees in its chunk in sequence; no extra visual effects. One coal per FIVE LOG BLOCKS, latest user instruction overrides older one coal per 20 logs. Upgrade potential in fuel efficiency and axe behavior.
[PROPOSAL forest internals] Reuse existing Lumberjack enchant tree-detection/classification rules including jungle giant trees, while separating machine block-break executor from player mining; bounded work per tick, persist work queue, avoid force-loading chunks, collect drops safely, pause on no fuel, broken axe, or full output. Credit five log breaks per coal and preserve remainder; tool durability/loot/protection and chunk-border trees need tests.
[PENDING forest] 1x1 vs 1x2; storage slots; scan frequency; cross-chunk tree; tool breakage and upgrades.

2. PRODUCTION ENGINEERING
[LOCKED T1] Fuel saving; precision smelting; precision crafting; wood processing; equipment maintenance.
[LOCKED precision smelting] ONLY mineral smelting in vanilla furnace/blast furnace; roll additional finished products AT CREATION TIME, not when taken from output. Respect vanilla blocked-output behavior; avoid duplicate bonus rolls and item overflow.
[LOCKED precision crafting] When crafted result is taken and ingredients consumed, individual 3x3 ingredient slots may probabilistically retain materials. Special container remainders, shift-click crafting, damaged ingredients require exception audit.
[LOCKED T2] Faster smelting, logistics control retained. Bulk crafting and fuel control REMOVED.
[LOCKED T3] Fast thermal processing + efficient combustion unified, parallel production, and chest sorting.
[LOCKED chest sorting] Unlock research and show controls immediately in LEFT or RIGHT extension of existing chest-opening UI. NO extra module/block or craft recipe. Exactly TWO sort modes: (1) item-type/name order and (2) item tag/category grouping. Quantity sort REMOVED. Preserve distinct item components, NBT, durability, enchantments and nonstackable custom fish.
[PROPOSAL chest] Consolidate only genuinely stack-compatible items, deterministic server-side sort, stable category precedence and fallback. Category precedence and detailed sorting keys PENDING. Note chest sorting is explicit exception to default T3 recipe rule.
[PENDING] Logistics control behavior; exact sorting order/categories; furnace bonus rates.

3. LIVING TECHNOLOGY
[LOCKED T1] Crop cultivation, seed study, soil, livestock, beekeeping, fisheries, cooking reserved pending cooking content.
[LOCKED fisheries] Modify existing fish GRADE probability distribution (not species access, length or weight). Five research levels; at Lv5 for crankbait tier 0: grade I/II/III/IV/V = 40/32/17/8/3%; tier 1 = 25/33/23/13/6%; tier 2 = 7/24/33/24/12%; tier 3 = 0/6/33/37/24%. Intermediate levels interpolate from existing FishingLogic.java baselines 45/30/16/7/2; 30/32/22/12/4; 10/25/32/24/9; 0/8/34/38/20, summing to 100.
[LOCKED T2] 3x3 area tilling; selective mature-crop-only harvest follows area tilling; throw-feed to attract animals and trigger growth/breeding; fishing technique; bulk cooking reserved.
[LOCKED T3] Automatic farm machine, irrigation, improved existing fish trap catch efficiency; auto-feeder REMOVED. Cooking processing/preservation reserved; other proposals not approved.
[LOCKED auto farm] Device installed UNDER farmland, manages 9x9 farmland footprint (vanilla water hydration reaches 4 blocks in each horizontal direction). No fuel. While redstone signal active, automatically harvest mature crops and replant. Right-click UI: 5x5 seed inventory plus 5x5 collected-products inventory. Harvested drops are captured DIRECTLY into machine inventory, NOT spawned as item entities. Replant consumes seed inventory first, then suitable crop items in output inventory. Hopper output DOWNWARD from product inventory; hopper insertion for seed inventory intended from ABOVE. No extra hopper-like harvest device needed.
[PENDING auto farm] Physical conflict of top-facing hopper and farmland immediately above device: consider side-access seed hopper mapped to input, or an alternative access channel, WITHOUT adding extra farm blocks; preserve 9x9 surface. Define full-inventory, special crops, redstone edge vs continuous mode, unloaded chunks.
[LOCKED irrigation] Single block with NO complex UI; water-bucket right-click fills one of five internal units, returns empty bucket. Consume 1 unit every 20 real-time minutes of operation, max five units = 100 minutes. Keep 9x9 farmland hydrated WITHOUT adjacent water. Block exterior gauge shows charge; states 0..5 means SIX distinct visual levels including empty (five filled states). When empty revert to vanilla hydration.
[LOCKED fish trap] Improve existing trap efficiency, not a new fishing mechanic.

4. COMBAT
[LOCKED T1] Swordsmanship, axe fighting, combined archery/crossbow 'Shooting', shield, armor operation, throwing; underwater combat TEMPORARILY ON HOLD. Prior separate bow and crossbow researches merged.
[LOCKED Shooting] Reduce charge/draw times for both bow and crossbow; server/client charging animations MUST synchronize. Crossbow Quick Charge enchant bonus and research reduction combine ADDITIVELY (not multiplicatively); minimum charge duration clamp. Fabric implementation deemed feasible and approved for future implementation.
[PROPOSAL other T1] Sword sweep bonus, axe fully charged first hit, shield durability savings, armor conditional mitigation, thrown-weapon travel bonus; specific effects not all user-approved.
[LOCKED T2] Consecutive sword hits build up to FIVE sweep-strength stages; reset FIVE SECONDS after last valid hit. Sweep buff, not generic attack-speed increase. Other previously selected T2 nodes remain subject to prior notes; no invented replacements approved.
[LOCKED T3] Defense engineering retained; tactical shooting research retained as CONCEPT of special arrows and fireworks, exact ammo types/design DEFERRED. Other newly proposed T3 ideas rejected.

5. ECOLOGY
[LOCKED T1 direction] Narrow biological lineage categories, e.g. zombie, skeleton, creeper, pig lineage (pig, piglin, zombified piglin, hoglin etc), not generic Undead=Smite duplicate. Species tag membership and overlapping bonuses pending.
[LOCKED T2] Creeper explosion windup suppression/delay; skeleton aiming/shooting interruption PROBABILISTIC; zombie reinforcement spawning suppression; piglin aggression avoidance PROBABILISTIC when opening containers or mining gold and related triggers. Spider climbing suppression and Enderman teleport restriction REJECTED.
[PROPOSAL T2 numbers] Creeper delay up to 1 sec and per-target 5 sec cooldown; skeleton 20/30/40% chance by levels; zombie reinforcement chance relative reduction 25/50/75%; piglin anger avoidance 30/45/60%. These are NOT locked.
[LOCKED T3] Mob tracking device with species UI filter and glowing highlight. Loot processing and special mob materials remain tentative; define nonduplicative conversion recipes before approval.

6. WORLD STUDIES
[LOCKED T1] Navigation, riding, flight, archaeology, trading, piglin bartering, survival, and expedition provisioning. Expedition provisioning adopted. Other additional T1 candidates not adopted.
[LOCKED T2] Precision flight is TRANSFERRED FROM ENCHANTMENT TO RESEARCH, NOT a new duplicate feature. User recalls a Paper implementation with hotbar-slot-based speed control; Fabric stage-20 checkpoint did not show equivalent precision-flight runtime. Need inspect Paper source before port; do not falsely claim completed or exact speed table. Other T2 proposals rejected or not compelling.
[LOCKED T3] Motorized boating and cross-dimensional home-bound item transport adopted; no forced extra T3.
[LOCKED boat] Shift-right-click boat opens small FUEL SLOT UI, not separate large inventory. Hotbar 1..9 controls boat speed using same principle as precision flight. Speed and fuel consumption scale LINEARLY with speed setting. Engine craft/attachment rules and balance TBD.
[LOCKED dimension transport] Item delivery visibly flies toward player's bed/home even across dimensions. Retain previous suggested server-authoritative queued delivery and destination storage, BUT ABANDON FEATURE if implementation complexity/bug risk is excessive. Never promise zero-risk cross-dimension transport. Bed missing, storage full, chunks unloaded, duplication and crash recovery must be handled.
[PROPOSAL precision flight] server-controlled flight speed modes and client hotbar sync; source-specific input/exit behavior TBD until Paper implementation inspected.

7. ALCHEMY / MAGIC
[LOCKED T1] Brewing, potion study, splash+lingering potion study, XP gain, magic material study, status-effect study, catalyst preservation, magic ingredient gathering. Elemental affinity REMOVED. Distinguish overlapping potion duration effects.
[LOCKED T2] Advanced brewing, potion mixing, XP control, and newly adopted BREWING PROCESS CONTROL. Potion-effect separation, potion recombination, XP allocation/distribution and magic-material conversion REJECTED.
[LOCKED brewing process control] Extend VANILLA brewing stand UI with limited material holding area (PROPOSAL nine slots) and finished-potion pickup/output akin vending machine. Hopper insertion is allowed, but regardless of random material arrival order, select correct ingredient according to chosen target recipe. Automatically repeat recipe chains until material supply exhausted OR output capacity full; manual ON/OFF toggle controls work. Hopper extraction should take only final finished product, not intermediate potions. No arbitrary extra material duplication. Recipe goal and intermediate state must persist and sync.
[LOCKED T3] Potion refining/concentrating machine, allowing effects ABOVE vanilla brewable maximum strength, not just longer duration; include Bad Omen (normally bottled ominous bottle, requires special recipe handling). Avoid assuming all potion effects benefit from amplifier; determine effect-specific caps, ingredients, input-output, Bad Omen tier limits.
[PROPOSAL potion refining] Separate station, inputs/outputs and amplifier/cost caps TBD; previous assistant suggestion of 2 matching potions -> 1 concentrate is not explicitly approved.
[REMOVED] Elemental catalyst research and elemental infusion; no useful distinct brewing application identified. Potion effect storage and transfer removed. Catalyst preservation T1 remains distinct from removed elemental catalyst T3.

IMPLEMENTATION / DESIGN BACKLOG [PENDING]
- Forest device: chunk tree scanner and safe throttled break queue, 5 logs/coal, axe semantics, output.
- Chest UI sort: category precedence, item identity comparator, network permissions.
- Precision smelt/craft: event timing, output compatibility, special recipe returns.
- Farm: 9x9 redstone harvesting and direct drops, seed+output 25 slots each, top hopper physical routing, side/bottom I/O.
- Irrigation: 0..5 exterior states, bucket interaction, 20-min drain, chunk persistence.
- Fisheries: level interpolation and FishingLogic grade weight integration.
- Shooting: bow model/use-duration animations, crossbow Quick Charge additive timing and minimum.
- Sword sweep: 5 levels and 5-second expiration.
- Ecology: targeted mob event hooks and overlap with custom species; proposal rates.
- Precision flight: find actual Paper implementation, then migrate enchant -> research.
- Boat: fuel UI, linear 9-level throttle and cost, safe hotbar sync.
- Dimension transport: safe queued transfer, optional cosmetic flight, abort if complexity too high.
- Brewing process: material queue/recipe selector/ON-OFF/continuous run/output hopper behavior.
- Potion refining: above-vanilla effect amplification, Bad Omen handling, caps and recipes.
- Design final numerical values, recipes, research prerequisites, materials and XP only after gameplay rules reviewed.
==================================================

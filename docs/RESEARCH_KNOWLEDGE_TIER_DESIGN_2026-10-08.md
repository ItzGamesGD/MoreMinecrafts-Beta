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

==================================================
KNOWLEDGE / RESEARCH CHECKPOINT — 2026-10-08 — KNOWLEDGE 70, ECOLOGY REVISIONS, COST POLICY, 64:1 RESEARCH COMPRESSION
Authority: follow-up user-confirmed conversation. For conflicts with prior parts of this document, THIS SECTION takes precedence. DESIGN RECORD ONLY; no mod code or asset implementation approved.
Legend: [LOCKED] user accepted; [PROPOSAL] assistant example, not final balance; [PENDING] unresolved; [SUPERSEDED] earlier decision replaced.
==================================================

A. [LOCKED] SYSTEM, EXPERIENCE, AND RESEARCH UI
- Knowledge is acquired permanently and FREE via normal Minecraft gameplay counters/events. It gates research eligibility, grants a very small independent knowledge bonus, and is NOT bought with XP or materials. It is distinct from research and quests. The user accepted a 70-entry knowledge roster across seven disciplines, detailed in section B.
- Research must be explicitly purchased using Minecraft XP plus relevant materials. Research rewards permanently change gameplay; research cost is consumed only once per purchase, with no recurring upkeep for the research benefit. Individual level increases, when offered, are independently purchased; each research may have a different cap. Do not lock vanilla recipes or existing functionality.
- Quests/activities will supply one-time XP; no daily quest shop/reroll decision. Do not finalize quest XP payout until research XP sinks and existing enchant/enhancement sinks are reconciled.
- L / advancement key route displays three choices: 지식 (Knowledge), 연구 (Research), 발전 과제 (vanilla Advancements). The vanilla advancement screen itself remains unchanged. Knowledge/research are larger, custom advancement-like tree screens (approximately 85–95% of available display proposed; exact size implementation-dependent). Keep vanilla-size nodes; central hub leads radially to 7 fields then branches; stable fixed locations. Wheel zoom, drag pan, and recenter controls are the intended interface.
- Knowledge nodes auto-unlock on fulfilled conditions. Research node click opens purchase confirmation with prerequisites, material quantities, and XP; cancel/confirm; only server validates and deducts; multiplayer state/sync required. No auto-purchase upon knowledge acquisition. This UI is DESIGN-LOCKED but has NOT undergone Fabric runtime feasibility testing.
- Discipline names: 자원학, 생산공학, 생활기술학, 전투학, 생태학, 세계학, 연금·마법학.

B. [LOCKED] KNOWLEDGE LIST — 70 ENTRIES / 7 FIELDS
The following 70 entries and their simple gameplay acquisition criteria were accepted as a design roster, NOT evidence of code implementation. Research connections must use actual knowledge and should not silently omit species-specific ecology linkages. Information/encyclopedia rewards are knowledge-side micro-rewards, NOT research mechanics.
[자원학 10]
01 광석 식별: 광석 5종 채굴; 광석 채굴 XP +1%.
02 심층 광물학: 심층암 광석 5종 채굴; 심층암 광석 채굴 XP +1%.
03 희귀 광물학: 다이아몬드·에메랄드·고대 잔해 획득; 희귀 광물 도감.
04 수목 관찰: 원목 6종 획득; 묘목 드롭 +1%.
05 거목 생태: 대형 나무 원목 128개 수집; 나무 도감.
06 식물 채집: 식물 10종 획득; 식물 도감.
07 토양 분석: 흙·거친 흙·회백토·진흙 획득; 토양 도감.
08 암석 분류: 암석 8종 채굴; 암석 도감.
09 광맥 탐사: 광석 256개 채굴; 광석 채굴 XP +1%.
10 자원 보존: 곡괭이 내구도 500 소비; 곡괭이 내구도 소비 -0.5%.
[생산공학 9]
11 제련 원리: 광물 5종 제련; 제련 XP +1%.
12 고온 제련: 용광로 제작 및 사용; 제련 XP +1%.
13 합금 이해: 네더라이트 주괴 제작; 합금 도감.
14 제작 기술: 도구 5개 제작; 제작 도감.
15 정밀 제작: 레시피 50회 완료; 레시피 힌트.
16 장비 정비: 모루로 장비 10회 수리; 수리 정보.
17 보관 기술: 상자 10개 제작; 보관 도감.
18 물류 공학: 호퍼 5개 설치; 물류 도감.
19 자동화 원리: 레드스톤 구성요소 20개 설치; 레드스톤 도감.
[생활기술학 11]
20 작물 생장: 작물 100개 수확; 성장 정보.
21 종자 연구: 씨앗 5종 획득; 종자 도감.
22 토양 관리: 농지 100칸 경작; 경작 정보.
23 관개 기술: 양동이를 써 농지에 물 공급; 관개 정보.
24 농업 자동화: 관찰자·피스톤·호퍼 제작; 자동화 도감.
25 가축 사육: 동물 5종에게 먹이 주기; 사육 도감.
26 번식 생태: 동물 30회 번식; 번식 정보.
27 어류 도감: 물고기 5종 낚기; 어류 도감.
28 수역 분석: 서로 다른 수역에서 낚시; 수역 정보.
29 낚시 기술: 낚시 100회 성공; 낚시 XP +1%.
30 식품 가공: 음식 10종 제작 또는 조리; 음식 도감.
[전투학 10]
31 검술 관찰: 검으로 적 50마리 처치; 검술 정보.
32 연속 공격: 검으로 적 100마리 처치; 검 전투 XP +1%.
33 중량 무기: 도끼로 적 30마리 처치; 중량 무기 정보.
34 궁술 이해: 활로 적 50마리 처치; 궁술 정보.
35 석궁 구조: 석궁으로 적 30마리 처치; 석궁 정보.
36 방패 방어: 방패로 공격 50회 방어; 방패 내구도 소비 -0.5%.
37 방어구 구조: 전신 방어구 착용; 방어구 정보.
38 투척 무기: 삼지창으로 적 10마리 처치; 투척 무기 정보.
39 전투 기동: 달리면서 적 30마리 처치; 기동 정보.
40 특수 탄약: 물약 화살 또는 폭죽 탄약 사용; 탄약 도감.
[생태학 12]
41 좀비 생태: 좀비 30마리 처치.
42 스켈레톤 생태: 스켈레톤 30마리 처치.
43 크리퍼 생태: 크리퍼 30마리 처치.
44 거미 생태: 거미 30마리 처치.
45 엔더맨 생태: 엔더맨 15마리 처치.
46 슬라임 생태: 슬라임 20마리 처치.
47 일리저 사회: 약탈자 및 변명자 처치.
48 피글린 사회: 피글린과 물물교환 20회.
49 호글린 생태: 호글린 20마리 처치.
50 수중 적대 생물: 드라운드 및 가디언 처치.
51 희귀 생물: 희귀 몹 3종 조우; 대상 종 목록 [PENDING].
52 보스 생태: 엔더 드래곤 또는 위더 처치.
**ALL 12 ECOLOGY KNOWLEDGE REWARDS [LOCKED]: +5% damage to the relevant mob/family, replacing older encyclopedia/info rewards for these 12 entries. Knowledge reward applies separately from paid research. Do not stack multiple knowledge +5% bonuses on a single mob via overlapping family membership. Exact taxonomy/tagging remains [PENDING].**
[세계학 10]
53 지형 탐사: 생물군계 15종 방문; 지형 도감.
54 동굴 탐사: 동굴 생물군계 3종 방문; 동굴 도감.
55 해양 항해: 보트로 5,000블록 이동; 항법 정보.
56 승마 기술: 말로 2,000블록 이동; 승마 정보.
57 비행 관찰: 겉날개로 2,000블록 비행; 비행 정보.
58 차원 탐험: 네더와 엔드 방문; 차원 도감.
59 고고학: 수상한 모래/자갈에서 솔질로 아이템 10개 획득; 고고학 도감.
60 고대 유적: 구조물 5종 발견; 유적 도감.
61 주민 사회: 주민 거래 30회; 거래 정보.
62 원정 기술: 구조물 10종 방문; 원정 도감.
[연금·마법학 8]
63 양조 기초: 첫 물약 양조; 양조 도감.
64 물약 성질: 물약 5종 양조; 물약 정보.
65 투척 물약: 첫 투척 물약 양조; 투척 물약 도감.
66 잔류 물약: 첫 잔류 물약 양조; 잔류 물약 도감.
67 상태 효과: 상태 효과 8종 경험; 효과 정보.
68 마법 재료: 양조 재료 8종 획득; 재료 도감.
69 고급 양조: 물약 50개 양조; 양조 XP +1%.
70 물약 농축: 강화 물약 5개 양조; 농축 연구 힌트.
[IMPLEMENTATION REVIEW] Counter semantics, retrospective unlocks, custom-mob classification, exact 1% event applicability, and UI text are not tested or implemented by this design checkpoint.

C. [LOCKED] ECOLOGY FOLLOW-UP OVERRIDES (MORE RECENT THAN THE PRIOR SECTION 5)
- Earlier note "Enderman teleport restriction REJECTED" is [SUPERSEDED]: the user explicitly chose Enderman teleport prevention as a research effect.
- Spider research: prevent poison application and prevent spider jumping. An older claim about spider climbing prevention is NOT the newest decision; do not replace the agreed jump restriction with climbing restriction.
- Enderman research: prevent teleportation.
- Slime research: prevent splitting; distribute drops and XP equivalent to killing all descendant slimes down to smallest, with no duplicate payout.
- Illager research: slightly reduce movement speed, NOT attack speed or spellcasting speed.
- Boss, rare mobs, underwater hostiles, hoglins: simple additional damage bonus in paid research; exact magnitudes [PENDING].
- Existing approved zombie reinforcement suppression, skeleton aim disruption, creeper fuse delay, and piglin anger avoidance remain. Detailed activation, levels, cooldowns, numerical chance and target membership [PENDING], unless explicitly fixed elsewhere.
- Every one of the 12 ecology knowledge branches must connect to its corresponding species/family research; lack of fully specified mechanics is NOT license to orphan a knowledge node.
- The assistant's separate sample T1 ecology placeholder researches with "generic response ability TBD" are NOT established finished mechanics. Decide whether to consolidate with the concrete T2 effect or design genuine T1 effects without invention.

D. [LOCKED] COST PHILOSOPHY AND RESEARCH-MATERIAL COMPRESSION
- User purpose: research permanently alters gameplay, so even basic research should consume SUBSTANTIAL QUANTITIES of thematic materials, plus XP. A large material requirement (examples mentioned: 6 iron blocks, 2 iron blocks + 2 diamond blocks for mining, 128 logs for lumbering) illustrates scale; none is a universally mandated standard or locked node price.
- Match expense to field and activity: ore/minerals for mining, wood for lumbering, crops/seeds for farming, creature drops for ecology, brewing ingredients for alchemy. Distinct materials and quantities per research; do NOT force equality in a fictitious iron-block value exchange rate.
- Remove arbitrary bulk submissions of completed tools/equipment/workstations (e.g. iron axes x2, iron swords x8, brewing stands x4, workbenches x16, furnaces x16, fishing rods x8). Such items may be considered only if a specific research genuinely studies that item; do not reintroduce as default cost fillers.
- Materials plus XP are consumed on purchase. Exact XP points vs XP levels semantics, per-level multipliers, node-by-node costs, caps, and total research XP demand remain [PENDING] or [PROPOSAL]; the assistant's earlier entire XP/material table is NOT user-ratified. The suggested bands T1 100–700 raw XP, T2 700–2500, T3 2500–5000 are only [PROPOSAL], not hard limits. Quest payout must be calibrated after research and existing enhancement/enchant XP demand are known.
- [LOCKED] Research-only compression: 64 identical original materials -> 1 compressed variant; maintain original visual/texture with enchantment glint and a recognizable "압축된 ..." item name, stackable up to 64. Compression is ONE-WAY: no decompression recipe/action.
- [LOCKED] Compressed item is a dedicated research payment token representing exactly 64 of the source item. It is NOT a normal ingredient/tool or replacement for the vanilla item. It must be unusable in all ordinary crafting, repair/anvil, smelting/fuel, brewing, trading, and material-consuming machine/automation operations. For example compressed wheat must NEVER craft bread as if it were one wheat; safety is more important than convenience.
- [LOCKED] Research payment accepts mixed ordinary + compressed counts at exactly 64:1 equivalence: e.g. a requirement of 150 wheat can be met with 2 compressed wheat + 22 original wheat, with correct server-authoritative deduction and no overcharge.
- [LOCKED] Mark compression as irreversible in descriptive tooltip to warn against accidental conversion.
- [IMPLEMENTATION RECOMMENDATION, NOT LOCKED CODE] Give compressed variants their own item identifiers while reusing source textures, rather than merely rename ordinary ItemStacks or apply only a glint; disallow normal recipe ingredient matching by structural item identity. Dedicate only explicit 64:1 conversion and research consumption paths. Audit recipe books, crafting grids, hoppers, auto-crafting, recipe remainder behavior, tags and third-party mod recipes for leakage/duplication. 2x2/3x3 recipe placement and exact registries require implementation review.
- [PENDING] Which materials get variants (prefer ordinarily 64-stackable materials), handling of recipe-tag alternatives such as log species, item identity, overflow/overpayment, and anti-dupe server behavior.

E. [PROPOSAL ONLY] EXAMPLE COSTS WERE SHOWN, NOT APPROVED
- Example materials from the prior conversation included 128 logs for wood, 256 rotten flesh for zombies, 512 gunpowder for creeper later-tier research, 512 nether wart for brewing, and mixed iron/diamond blocks for mining. These are merely first-pass examples, NOT fixed requirements or accepted node effects.
- The assistant's sample list sometimes introduced new or placeholder researches such as broad T1 "mob response", "logistics control", "XP research" without defined effects; they must NOT become implementation requirements on the strength of having a price row.
- A proposed Lv1..Lv5 XP multiplier curve (1, 1.5, 2, 3, 4), universal T3 cost bands, and recipe-specific inventory counts were not explicitly approved.
- Special-source rare materials (tridents, saddles, nether stars, dragon breath) may block progression; balance and alternative designs [PENDING].
- Device research costs and device crafting costs are distinct economic events; exact budget [PENDING].

F. [PENDING] NEXT DESIGN PASS (NOT IMPLEMENTATION)
1. Complete exact, gameplay-meaningful effects for T1 researches currently defined only by direction; do not silently invent effects to fill a chart.
2. Fill remaining T2/T3 mechanisms only where explicitly undecided; preserve confirmed mechanics and ecology overrides.
3. Set independent research level caps, per-level benefits, prerequisite graph, and then exact materials and XP point costs.
4. Audit compression item isolation in all vanilla/mod recipe and automation flows; approve implementation mechanics after design.
5. Sum research XP sink together with existing enchant skill tree, book option/combining, equipment enhancement and vanilla XP consumption; only then design one-time quest XP rewards.
6. Keep GitHub and Google Drive checkpoint text in sync. No code edits, PR merges, stage promotion, or claims of client/server tests from this documentation-only update.
==================================================

==================================================
RESEARCH EFFECTS V2 — USER-APPROVED DESIGN OVERLAY
PRECEDENCE: This newest section supersedes conflicting V1 / 88-idea / initial provisional price rows. DESIGN ONLY. No mod implementation, merge or main-branch promotion was performed.
AUTHORITY: [DIRECT] explicit user specifications; [ACCEPTED-DELEGATED] assistant detail subsequently accepted by user ("위임한거 나쁘지 않아", "장비 정비표도 좋다", "어업 연구 위임 좋다", "경작용 괭이 레시피도 좋고", "이대로 확정해도 될정도야"); [FOLLOW-UP] deferred design, balance or runtime testing; [REMOVED] removed. Numerical example prices are TEMPORARY and will be fully recalibrated after design is complete.
==================================================

0. IMMEDIATE AMENDMENTS AND SCOPE
[REMOVED/DIRECT] Delete paid Resource Studies T1 '채집' / Gathering research altogether: too much overlap. Do NOT delete knowledge entry #06 '식물 채집' from the LOCKED set of 70 knowledge nodes. Retarget #06 to T1 seed studies and/or magical ingredient research. Knowledge #07 토양 분석 now targets soil study, not deleted gathering. No paid gathering research, effects, cost, recipe, or node.
[DIRECT] 3x3 mining PICK is a 150-durability, non-enchantable, diamond-pickaxe-class tool, mainly convenient for flattening land. The originally proposed costly recipe containing a diamond block and other blocks is SUPERSEDED: reduce repeat craft cost.
[ACCEPTED-DELEGATED / REVISED ECONOMY] New lighter, shaped 3x3 mining tool recipe, positions numbered 1–9:
1 iron ingot; 2 diamond; 3 iron ingot;
4 copper ingot; 5 diamond pickaxe; 6 copper ingot;
7 redstone dust; 8 stick; 9 redstone dust.
This uses 1 extra diamond, 2 iron ingots, 2 copper ingots, 2 redstone dust, 1 stick plus an existing diamond pickaxe; no diamond blocks/netherite. Recipe is authorized as the cost-lowered resolution within the user's delegated recipe scope; later economic rebalance may tune crafting recipes separately.
[DIRECT] Automated lumbering must visibly process TREE LOG BLOCKS ONE AT A TIME over multiple ticks, with a normal wood-block-breaking sound on each block. No instant whole-tree disappearance. [ACCEPTED-DELEGATED] use a bounded server work queue; propose a starting interval of 4 ticks per block, adjustable after live tests. Drops appear immediately above the 1×1 machine; no built-in inventory. No chunk force-load. Coal: 1 per 8 log blocks (supersedes 1 per 5); axe durability: 1 point per 3 logged blocks. Apply accumulated metering so interruptions do not duplicate cost. Tree identification, build protection and unload/restore are implementation responsibilities; do not claim solved or tested.
[DIRECT] Earlier 70 knowledge acquisition events and tiny bonuses remain approved. Do NOT redesign, claim they were unspecified, or confuse free knowledge with paid research. 7 disciplines still exist.
[DIRECT] Research reset/refund is NOT allowed. Each research T2 requires prior T1 unlock and each T3 requires a prior T2 unlock. Branch rather than linear three-node ladder. Do not force nonsensical cross-domain prerequisites merely to satisfy the hierarchy; exact valid graph and node positions are [FOLLOW-UP].
[DIRECT] Prior assistant example material/XP costs are TEMPORARILY used, NOT economically final. Final per-research recosting after content design; remove spurious completed tool/workstation bulk costs. Compression rule remains 64:1 research-only, irreversible; see section 9.
[DIRECT] Checklist section 10-6 onward (anti-dupe and technical auditing), quest+XP economy and UI/multiplayer implementation are FOLLOW-UP, not work claimed complete here.

1. RESOURCE STUDIES / 자원학
[DIRECT] Ore mining Lv1–5: level 1 grants base additional-drop effect; Lv2 increases proc chance, Lv3 increases max added drop, Lv4 increases proc chance, Lv5 increases max added drop. [ACCEPTED-DELEGATED] chances/max extra: L1 10%,1; L2 20%,1; L3 20%,2; L4 30%,2; L5 30%,3. Successful extras uniformly select 1..max. Works on vanilla+MoreMinecrafts ore; audit Fortune/Silk Touch and existing auto-smelt drop buffs before implementation.
[DIRECT] Lumbering Lv1–5 follows SAME ladder and [ACCEPTED-DELEGATED] same numeric distribution: 10/20/20/30/30%, extra max 1/1/2/2/3 logs. Tree-harvest drops only; this does not fell extra blocks or duplicate chain logging enchant.
[DIRECT] Mineral conservation Lv1–5: mining tool durability-consumption prevention, linear chance; [ACCEPTED-DELEGATED] 5% per level up to 25%, additive independent prevention check without double-consuming damage.
[DIRECT] Wide excavation T3: 3×3 according to face and diamond-pickaxe permitted blocks; 150 durability; 1 durability for the whole successful operation even if 9 blocks mined; no enchantments; documented future protected-block exclusion despite no current protect-block structure. Full ore/loot/tool compatibility and non-minable exclusions require runtime audit. REVISED CRAFT as section 0.
[DIRECT] Lumber machine T3: size 1×1; axe + coal UI, no item storage; chunk-local single-tree sequential processing, drop above machine, no sound/VFX except vanilla-like wood breaking SFX per block; coal 1/8 logs; axe durability -1/3 logs; no loaded chunk work when unloaded; no unintended building destruction.
[REMOVED] Gathering T1 removed (see 0).

2. PRODUCTION ENGINEERING / 생산공학
[DIRECT] Fuel Saving Lv1–3: ALL fuel types and ALL furnaces (vanilla furnace, blast furnace, smoker, portable furnace, multiplier furnaces and future modded furnace family). Fuel consumption -8.5% per level: 8.5/17/25.5%. Maintain a permanent compatibility checklist for every new furnace/recipe as well as code changes.
[DIRECT] Precision Smelting Lv1–5: +7.5 percentage points per level to grant one extra finished product (7.5/15/22.5/30/37.5%), ALL vanilla AND modded MINERAL smelting; exclude nonmineral/cooking. Bonus rolled at product creation, not output pickup. Auto-smelt enchant has a distinct bonus-drop axis; roll research no more than ONCE per smelted base unit, plus auto-smelt bonus independently, without recursive processing or re-rolling bonus products.
[DIRECT] Precision Crafting has rarity-dependent preservation distribution [ACCEPTED-DELEGATED]: max 5 levels, at levels 1/3/5: ordinary materials 10/20/30%; processed iron/copper/redstone 7/14/21%; precious gold/diamond/emerald 4/8/12%; rare netherite/special part 2/4/6%; unique/nonreplaceable 0%. Intermediate L2/L4 interpolate. On each complete crafting transaction at most ONE input slot may be saved; apply probability of the candidate ingredient class, special remainders and shift-click guarded, and exclude compressed research tokens.
[DIRECT] Wood Processing max L2: each level adds +1 produced item to ALL wood-based crafting results: e.g. 4 planks ->5, doors 3->4, trapdoors 2->3, slabs 6->7 at L1, +2 by L2. Account for recipes returning different wood objects without overwriting existing results.
[DIRECT] Equipment Maintenance max L5: L1/L3/L5 each +10 percentage points to per-material anvil durability restoration (+10/+20/+30% cumulative); L2/L4 each reduce all anvil XP consumption by 4.5 percentage points (-4.5/-9% cumulative), rounded, minimum 1 XP LEVEL when applicable. Restoration only actual repair; XP discount for renaming/repair/enchant/anvil operations. Prior approved table preserved.
[DIRECT] Faster Smelting max L3: +10% speed each (110/120/130% baseline); same ALL furnace families as Fuel Saving, and STACKS with it. Default functional interpretation: -fuel consumption and +processing speed apply together; reconcile fuel duration and cooking timeline transactionally.
[RECOVERY STATUS] Logistics Control T2: older record lists node but no verified, sufficiently defined effect. User says recheck and DELETE if absent. As no completed rule was verified in consulted GitHub/checklist material, EXCLUDE it from approved functional roster, marking [NOT RECONSTRUCTED; CONDITIONAL REMOVAL]. Do not present invention as restored fact.
[DIRECT] High-Speed High-Efficiency Furnace T3: one research unlocks ALL 3x/5x/7x furnaces; both speed and fuel efficiency scale by 3,5,7 respectively; separate craft recipes:
3x: slots 1,3,7,9 iron ingot; 2,4,6,8 iron BLOCK; center slot 5 furnace.
5x: slots 1,2,3,4,6,7,8,9 iron BLOCK; center furnace.
7x: slots 1,3,7,9 netherite INGOT; 2,4,6,8 gold BLOCK; center furnace.
Extra research speed/fuel saving stack with these furnaces.
[RECOVERY STATUS] Parallel Production T3: name/idea exists, finished exact machine behavior not recovered from source; omit from currently approved functional roster, mark [NOT RECONSTRUCTED; CONDITIONAL REMOVAL]; don't invent or silently keep it.
[DIRECT/PRIOR LOCKED] Chest Sort T3 is UI-ONLY unlock: existing chest screen gains TWO side buttons, name/item-type sorting and tag/category sorting. No module, new block, extra recipe, quantity sorting. Preserve per-item data/components/enchants/durability/custom non-stackable fish. Exact comparator and special slot interaction implementation tests are follow-up.

3. LIVING TECHNOLOGIES / 생활기술학
[DIRECT] Crop Cultivation max L5 bonus harvested items: L1 exactly +1; L2 +2 with 40% else +1; L3 exactly +2; L4 +3 with 30% else +2; L5 +3 with 50% else +2. Includes future registered crops via compatibility hook, exact extra drop type per crop.
[ACCEPTED-DELEGATED] Seed Study max L3: extra seed proc 15/30/45% for eligible seed harvests. Do not modify crop bonus recursively.
[DIRECT + ACCEPTED-DELEGATED] Soil Study L1 farmland trample protection; L2 +30% seed yield; L3 farmland stays hydrated for up to 5 additional minutes during water interruption (conditional on no other sustaining mechanism). Soil and seed bonus interactions tested once per ordinary crop event.
[DIRECT] Livestock L1: offspring TWO with 60% chance else normal ONE; L2 TWO guaranteed; L3 THREE 20% else TWO. Standard breeding conditions, parents/cooldown remain vanilla.
[ACCEPTED-DELEGATED] Beekeeping max L3: honeycomb harvest +1 with 20/40/60% chance; no duplicate harvest activation.
[PRIOR LOCKED + ACCEPTED-DELEGATED] Fisheries Study L1–5 changes existing Fishing GRADE probabilities, never species/length/weight. BASE tier 0/1/2/3 distributions = [45,30,16,7,2]; [30,32,22,12,4]; [10,25,32,24,9]; [0,8,34,38,20]. L5 = [40,32,17,8,3]; [25,33,23,13,6]; [7,24,33,24,12]; [0,6,33,37,24]. L1–L4 linear interpolate from baseline to L5 (L0 baseline), distribute rounding residual to ensure exactly 100%.
[DIRECT + ACCEPTED-DELEGATED] 3x3 tilling HOE T2: vanilla hoe-like tilling mechanics, 3x3 footprint, durability 100; no enchantments and no huge bulk-workstation requirements. APPROVED recipe shaped 1 iron ingot, 2 iron block, 3 iron ingot / 4 copper ingot, 5 diamond hoe, 6 copper ingot / 7 stick, 8 redstone block, 9 stick. Earlier reply's exact numbers retained as accepted, with future recipe economy recheck.
[DIRECT] Mature-only AoE harvest T2: future custom crops plus ALL vanilla eligible crops; does not itself consume tool durability. Only mature crops targeted; process item rewards safely and permit crop-type registration for future mod additions.
[DIRECT] Throw Feed/Breeding T2: 7x7 horizontal area; vanilla animal-specific feed, breeding/growth and cooldown rules, no bypass of vanilla restrictions.
[ACCEPTED-DELEGATED] Fishing Technique T2 L1–3: broaden fishing minigame SUCCESS window by +2/+4/+6 percentage points respectively, without changing Grade, species, size, fish instance, or fishing reward rerolls.
[DIRECT] Auto Farm T3: manages exactly ONE vertical layer of a 9x9 farm; never any stacked farms at same x/z at other elevations. Automatically breaks MATURE plants and replants instantly; harvest drops directly captured, not item entities. GUI 5x5 'seed/replanting stock' first, 5x5 output second; harvested replantable material/seed refills SEED stock first and excess flows to results. Carrot, potato, sugar cane etc need no rigid seed-vs-result item type partition; seed/inventory slot priority remains. NO top hopper needed or inserted; ONLY bottom hopper extracts results. When both compartments full, pause safely; sugarcane/bamboo-like crop handlers preserve growing base. Redstone active mode per prior design; no work in unloaded chunks; support future crops via hook/documented compatibility list.
[DIRECT] Irrigation T3: 9x9 hydration, 5 water-bucket charges, 20 REAL minutes operation each, max 100 min, six exterior gauge states 0..5; hydration stops at zero. ALL devices inactive in unloaded chunks. Irrigation pauses consumption/operation when RAINING (doesn't consume charges while rain already wets land); resume remaining timer afterward. No imaginary top feeder.
[ACCEPTED-DELEGATED] Fish Trap Improvement T3: existing fish trap interval 5 min -> 3 min 30 sec, no species/grade/length/weight modifications.

4. COMBAT / 전투학
[ACCEPTED-DELEGATED] T1:
Swordsmanship L1–3: full-charge sword hit damage +2/+4/+6%.
Axe Combat L1–3: first strike after >=3 sec without attack +3/+6/+9%.
Shooting L1–3: bow draw and crossbow charge time -6/-12/-18%; crossbow Quick Charge and research durations additively combine (not multiplied); clamp minimum duration; server/client charging animation sync.
Shield L1–3: 10/20/30% chance prevent shield durability loss.
Armor Operation L1–3: knockback taken while equipped -5/-10/-15%.
Throwing Combat L1–3: trident/custom thrown projectile speed +6/+12/+18%.
[DIRECT + ACCEPTED-DELEGATED] Consecutive Sword Sweeps T2: valid consecutive sword hits build max 5 stages; reset after 5sec from LAST valid hit. Each stage +4/+8/+12/+16/+20% sweep damage, not generic attack speed, and targeting a different enemy does not reset chain.
[USER CORRECTION + ACCEPTED-DELEGATED] Defense Engineering T3 is a CRAFTABLE COUNTER-SHIELD (NOT generic protective armor), which KNOCKS AN ATTACKER BACK upon a successful shield block; suggested exact effect: eligible attacker within 3 blocks, knockback about 2 blocks, cooldown 2 sec; vanilla shield blocking otherwise retained. Recipe itself not settled and is future recipe-balance content.
[DIRECT] Tactical Shooting T3: retain SPECIAL ARROWS and FIREWORK AMMO as research concepts; exact kinds, effects, recipes and art are FOLLOW-UP, do not invent them.

5. ECOLOGY / 생태학
[DIRECT] All 12 already-locked FREE knowledge branches give relevant mob-family +5% damage; NOT duplicated when classification overlaps; do not multiply with research default blindly.
[USER DELEGATION ACCEPTED] Favor one substantive lineage research over fabricated empty T1 nodes, with tier semantics relaxed except unique T3 mob tracker. Research mechanics:
Zombie reinforcement relative chance reduction L1–3 25/50/75%.
Skeleton aim reset L1–3 probability 20/30/40%.
Creeper fuse delay L1–3 +0.3/+0.6/+1.0 sec.
Spider: BLOCK poison application and jump attack; NO climbing restriction.
Enderman: BLOCK teleporting for an Enderman engaged with the researcher; previous REJECTED label superseded.
Slime: STOP splitting; give all descendant-equivalent loot and XP recursively from large to smallest exactly once, Looting safe, without spawning extra slimes.
Illager: movement speed -12%, not attack/cast speed.
Piglin: chest-opening/gold-mining anger prevention L1–3 chance 30/45/60%; ongoing anger handling must not be silently overridden.
Hoglin paid-research damage L1–3 +4/+6/+8%; underwater-hostile paid damage same; rare mob paid damage +5/+8/+12%; bosses paid damage +3/+5/+7%, in addition to separate knowledge +5% applied once per classification.
[ACCEPTED-DELEGATED] Rare knowledge discovery 3 tentative species for baseline: Endermite, Evoker, Elder Guardian; integrate more custom 40-mob roster after audit. Cross-family overlaps don't add multiple +5% knowledge awards.
[ACCEPTED-DELEGATED] Mob Tracking T3: detector radius 32 blocks in loaded world area, species filter UI and glowing highlighting; no cross-dimensional/unloaded-chunk scanning.
Exact mob tags, paid-effect trigger exceptions and implementation compatibility with 40 custom mobs remain runtime follow-up.

6. WORLD STUDIES / 세계학
[DIRECT] Navigation/Boating T1 L1–3: ordinary boat movement speed +10/+20/+30%. Applies to powered boat too.
[DIRECT + ACCEPTED-DELEGATED] Riding T1 L1–5: every level increases movement speed, jump strength and max health each by 5% of that ATTRIBUTE'S OWN VANILLA NATURAL DISTRIBUTION WIDTH (max-min), not flat +4 or generic multiplicative +5% on disparate units; retain each individual animal's native base.
[DIRECT] Flight T1 L1–3: firework propulsion duration +8/+16/+24%; also cooperate with precision flight firework-use interval.
[ACCEPTED-DELEGATED] Archaeology T1 L1–3: brush time -10/-20/-30%; archaeology-related structure chest rare reward weight +3/+6/+9%; extra thematic loot roll probability 10/20/30%. Limit scope to archaeology-relevant loot sources.
[ACCEPTED-DELEGATED] Villager Trading T1 L1–3: show current/maximum trade STOCK in existing trade UI from L1; cost -2/-4/-6% (min 1).
[ACCEPTED-DELEGATED] Piglin Bartering T1 L1–3: rare-barter reward weight +5/+10/+15%, not duplicate piglin anger prevention.
[ACCEPTED-DELEGATED] Survival & Expedition T1 L1–3: sprint/swim hunger consumption -8/-16/-24%.
[VERIFIED ORIGINAL SOURCE] Precision Flight T2 moves the REAL existing Paper 'precision_flight' ENCHANT feature to research, no duplicate enchant. Source: ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper: src/main/resources/enchants.yml, EquipmentEnchantContentService.java. Paper original 3 levels, hotbar-slot target speeds 1..9 = [0.45,0.60,0.75,0.88,1.00,1.30,1.60,1.90,2.20] velocity values; acceleration 0.20; fireworks & elytra damage intervals 200 ticks; damage-debt recovery intervals by level = [120,100,80] ticks. Activates on Elytra firework boost; deactivates at gliding stop, sneak, invalid gear, missing fuel. The native data are CODE REFERENCE, not proof of Fabric port/live performance. Move this mechanic from enchant to paid research.
[DIRECT + ACCEPTED-DELEGATED] Motorized Boat T3: Shift+right-click fuel-slot UI, hotbar speeds 1–9, speed-proportional fuel consumption. Proposed speed multipliers start at 0.5x for slot 1, 1.0x for slot 5, 2.0x for slot 9; exact intervening table FOLLOW-UP. Navigation research multiplies speed, without extra fuel charge from that bonus.
[PRIOR LOCKED + ACCEPTED-DELEGATED] Cross-Dimension Home Item Transport T3: send items toward player's bed/home across dimensions, server-authoritative persisted idempotent delivery queue plus bound receiving storage. Cosmetic in-world flight only in loaded chunks. Existing Paper InventoryDeliveryService is only ordinary reward delivery and is NOT verified implementation of this research. ABANDON feature if risk/complexity exceeds reasonable reliability. Crash, missing bed, unloaded chunks and full inventory are FOLLOW-UP and must never cause dupes.

7. ALCHEMY / MAGIC / 연금·마법학
[DIRECT] Brewing Basics T1 L1–3: brewing time -15/-30/-45%.
[DIRECT + ACCEPTED-DELEGATED] Potion Study T1 L1–3: redstone extended potion duration bonus +8/+16/+24%; glowstone-associated duration penalty mitigation 10/20/30%. Explicit catalyst interactions tracked.
[ACCEPTED-DELEGATED] Splash/Lingering T1 L1–3: splash effect reach +10/+20/+30%; lingering-cloud duration +8/+16/+24%.
[DIRECT + ACCEPTED-DELEGATED] Status Effects T1 L1–3: beneficial effect time +5/+10/+15%; harmful effect time -5/-10/-15%, for effects applied to player. No arbitrary amplifier boost.
[ACCEPTED-DELEGATED] Magical Material Study T1 L1–3: extra magic/brewing ingredient on actual gathering/mob drops 10/20/30%, no output recursive duplication.
[OLDER DIRECTION FOUND; DETAILS ACCEPTED-DELEGATED] XP acquisition research T1 L1–3: XP ORB amounts +3/+6/+9%, not reduced research/enchant spend. Note this is not proof all underlying original mechanics were located.
[OLDER DIRECTION FOUND; DETAILS ACCEPTED-DELEGATED] Catalyst preservation T1 L1–3: brewing catalyst preservation probability 5/10/15%, where it makes sense, distinct from removed T3 elemental catalyst research.
[ACCEPTED-DELEGATED] Advanced Brewing T2 single unlock: increase brewing stand fuel efficiency from 20 to 25 brew operations per blaze powder, otherwise retain vanilla rules.
[ACCEPTED-DELEGATED] XP Control T2 two levels: XP pickup radius 4 / 6 blocks; not XP assignment, splitting or redistribution (older rejected proposals remain removed).
[DIRECT + ACCEPTED-DELEGATED] Potion Mixing T2: not anvil anymore (earlier idea superseded). Add MIXING/CONCENTRATION tab to vanilla brewing stand UI; two compatible different finished potions become one combined potion containing both effects with original durations x0.70, consuming the two input potions; conflict pair restrictions. The exact compatibility whitelist to be audited.
[PRIOR LOCKED + ACCEPTED-DELEGATED] Brewing Process Control T2: vanilla brewing stand extended UI, 9 material waiting slots and 9 finished output slots; choose goal recipe, take ingredients regardless of insertion order, process vanilla recipe steps in order automatically, respect fuel/material/output limits, manual ON/OFF, hopper input accepted and lower hopper extracts FINISHED output only (not intermediates). Persist unfinished brew state; no loss or dupe.
[DIRECT + ACCEPTED-DELEGATED] Potion Concentration T3: allow ONE amplification stage past normally obtainable max for qualifying potion effects, never repeated concentration; implemented in same custom Brewing Stand MIXING/CONCENTRATION TAB instead of anvil. Example Strength II -> Strength III. Bad Omen special handling: existing level I–V ominous bottle system, one-step increase allowed only while not exceeding vanilla V as conservative delegated design. Effect-specific restrictions, data components and balance to audit.

8. KNOWLEDGE PREREQUISITE LINKS: ALL 70 RETAINED, PAID GATHERING DELETED
Original 70 knowledge names, triggers, knowledge micro-rewards are preserved from previous checkpoint; these mappings are user-delegated design connections, not replacement knowledge achievements.
Resource 01 광석 식별 -> 광석 채굴; 02 심층 광물학 -> 광석 채굴/광역 굴착; 03 희귀 광물학 -> 광역 굴착; 04 수목 관찰 -> 벌목; 05 거목 생태 -> 자동 벌목 장치; 06 식물 채집 -> 종자 연구/마법 재료 연구 (NOT deleted 채집 연구); 07 토양 분석 -> 토양 연구; 08 암석 분류 -> 광물 보존; 09 광맥 탐사 -> 광석 채굴/광역 굴착; 10 자원 보존 -> 광물 보존.
Production 11 제련 원리 -> 연료 절약/정밀 제련; 12 고온 제련 -> 고속 제련/상위 화로; 13 합금 이해 -> 상위 화로; 14 제작 기술 -> 정밀 제작/목재 가공; 15 정밀 제작 -> 정밀 제작; 16 장비 정비 -> 장비 정비; 17 보관 기술 -> 상자 정렬; 18 물류 공학 -> 상자 정렬; 19 자동화 원리 -> 자동 농장/자동 벌목 장치.
Living 20 작물 생장 -> 작물 재배; 21 종자 연구 -> 종자 연구; 22 토양 관리 -> 토양 연구; 23 관개 기술 -> 관개 장치; 24 농업 자동화 -> 자동 농장; 25 가축 사육 -> 가축 사육; 26 번식 생태 -> 먹이 투척/번식; 27 어류 도감 -> 어업 연구; 28 수역 분석 -> 낚시 기술/통발 개선; 29 낚시 기술 -> 낚시 기술; 30 식품 가공 -> 연료 절약/고속 제련의 조리 파트.
Combat 31 검술 관찰 -> 검술; 32 연속 공격 -> 연속 검격; 33 중량 무기 -> 도끼 전투; 34 궁술 이해 -> 사격; 35 석궁 구조 -> 사격/전술 사격; 36 방패 방어 -> 방패/반격 방패; 37 방어구 구조 -> 방어구 운용/반격 방패; 38 투척 무기 -> 투척 전투; 39 전투 기동 -> 방어구 운용; 40 특수 탄약 -> 전술 사격.
Ecology 41 좀비 -> 좀비 연구; 42 스켈레톤 -> 스켈레톤 연구; 43 크리퍼 -> 크리퍼 연구; 44 거미 -> 거미 연구; 45 엔더맨 -> 엔더맨 연구; 46 슬라임 -> 슬라임 연구; 47 일리저 사회 -> 일리저 연구; 48 피글린 사회 -> 피글린 연구; 49 호글린 -> 호글린 연구; 50 수중 적대 생물 -> 수중 적대 생물 연구; 51 희귀 생물 -> 희귀 생물 연구; 52 보스 생태 -> 보스 연구.
World 53 지형 탐사 -> 항법/원정 준비; 54 동굴 탐사 -> 원정 준비; 55 해양 항해 -> 항법/동력 보트; 56 승마 -> 승마 연구; 57 비행 관찰 -> 비행/정밀 비행; 58 차원 탐험 -> 차원 운송; 59 고고학 -> 고고학; 60 고대 유적 -> 고고학; 61 주민 사회 -> 주민 거래; 62 원정 기술 -> 원정 준비/차원 운송.
Alchemy 63 양조 기초 -> 양조 기초; 64 물약 성질 -> 물약 연구; 65 투척 물약 -> 투척/잔류 물약; 66 잔류 물약 -> 투척/잔류 물약; 67 상태 효과 -> 상태 효과 연구; 68 마법 재료 -> 마법 재료/촉매 보존; 69 고급 양조 -> 고급 양조/공정 제어; 70 물약 농축 -> 물약 혼합/농축 장치.
[WARNING] The above 70-node linkage is a design mapping, NOT validated final branching dependency topology. Build natural same-domain T1->T2->T3 branches without forced mismatched links (e.g. shield not forced behind sword skill; motorboat not forced behind elytra precision flight). User tier prerequisite rule is LOCKED, precise tree topology is FOLLOW-UP.

9. RESEARCH COMPRESSION — LATEST USER DECISIONS
[DIRECT] Compress every ordinary 64-stackable MATERIAL and all researched material inputs where representable. Compression input is any crafting-grid slot, not only center; sum up to 64 eligible source materials for a conversion; mixed wood species become ONE generic '압축된 원목' material value. Other item-type family identity remains distinguishable unless explicitly unified.
[DIRECT] 64 units -> 1 compressed research-only item, original texture + enchant-glint, recognizable name, max 64 stack of compressed. Irreversible: no decompression, and NEVER ordinary craft, repair/anvil, smelt/fuel, brew, villager trade or material-consuming automation. Only server-authoritative research payment accepts compressed plus original by 64:1 exact equivalence. Deduct REQUIRED quantities, not arbitrary inventory extras; e.g. wheat 150 = 2 compressed + 22 normal, never overcharge; tooltip warns irreversible.
[FOLLOW-UP] Specific data registration, handling research materials with stack cap <64, multi-species match/input shapes, normal recipe collision protections, shift-click, automation and anti-dupe require implementation proof, not asserted as tested.

10. FOLLOW-UP / IMPLEMENTATION SAFETY / ECONOMY
- Research economic prices from assistant's prior draft remain TEMPORARY until complete redesign after approved mechanics. Final cost per level, resource amounts, rare materials and device separate crafting economy not yet final.
- T1->T2->T3 prior tier purchase and 7-discipline radial UI are required; final branching topology and research-per-level gates remain implementation/design follow-up. Do not set false locked edges.
- Protect existing item NBT/data, all 40 custom mobs, Fortune/Silk Touch/AutoSmelt enchant behavior, brewing caps and other mods' special recipes; no work has been compiled or client-tested by this checkpoint.
- Deferred after checklist item 10-6: compression anti-dupe, quest/XP demand, user interface and multiplayer runtime audits. These are not completed by writing this design record.
- Retain resource/production research name lists only for features above; the old 88-item brainstorm is historical, NOT an approved scope.
==================================================

============================================================
RESEARCH COSTS V3 — USER-APPROVED FINAL PRICE CHECKPOINT
STATUS: [LOCKED — EXPLICIT USER APPROVAL] The user approved the complete research materials/XP proposal. This checkpoint overrides all previous provisional/recommendation cost tables, except research effects and mechanics previously locked remain governed by V2. This is documentation only; no mod implementation or runtime validation.
RULE: All prices below are paid once PER RESEARCH LEVEL purchased. Listed XP numbers are raw XP POINTS, not XP levels. Materials are consumed alongside XP and may be paid in normal material and 64:1 research-only compressed variants as defined in the V2 design. No compression discount or overpayment allowed.
LEGEND: '압N' means N compressed units, i.e. 64×N original resource items. Other items and numbers are literal original counts. The exact server-side compression/recipe transaction remains an implementation follow-up; non-stackable/below-64-cap items require exception testing.
LIMITS: 70 paid research nodes, 178 level purchases, 130,360 raw XP for buying every listed level. This is not the cost of completing ordinary enchantment skill trees, enhancement systems, equipment/device craft recipes or quests. Skills, books, anvil etc. have independent XP sinks.
EXCLUSIONS: paid T1 채집 research removed; 70 free knowledge entries including 식물 채집 retained. Logistics Control T2 and Parallel Production T3 excluded after inconclusive recovery (conditional removal pending stronger primary source); do not reintroduce costs. Tactical Shooting T3 retains content category but detailed arrow/rocket recipes pending. Cross-dimensional transport T3 remains conditional on technical feasibility.
SUPERSESSION: The earlier lower 'crying obsidian' costs override the first draft: Ecology Piglin Lv1 8 / Lv2 16 / Lv3 32; World Piglin Bartering Lv3 16. All other listed costs reproduce the user-approved proposal. This section is the price authority over V1 and V2 example prices.
============================================================
SUMMARY BY DISCIPLINE
자원학: 5개 연구, 17회 구매, 12,320 XP
생산공학: 8개 연구, 25회 구매, 20,920 XP
생활기술학: 13개 연구, 31회 구매, 23,280 XP
전투학: 9개 연구, 21회 구매, 13,320 XP
생태학: 13개 연구, 29회 구매, 18,580 XP
세계학: 10개 연구, 28회 구매, 22,630 XP
연금·마법학: 12개 연구, 27회 구매, 19,310 XP
TOTAL: 70 RESEARCH NODES; 178 LEVEL TRANSACTIONS; 130,360 RAW XP.

FULL APPROVED PER-LEVEL XP AND MATERIAL COSTS

[자원학]
• 광석 채굴 [T1] Lv.5 | total 2,960 XP
  Lv.1: 260 raw XP | 철 블록 2, 석탄 압1
  Lv.2: 390 raw XP | 철 블록 4, 레드스톤 압2
  Lv.3: 560 raw XP | 금 블록 2, 다이아 8, 청금석 압2
  Lv.4: 750 raw XP | 다이아 블록 2, 구리 블록 8
  Lv.5: 1,000 raw XP | 다이아 블록 3, 에메랄드 블록 2, 흑요석 압4
• 벌목 [T1] Lv.5 | total 2,280 XP
  Lv.1: 200 raw XP | 원목 압2, 철 주괴 16
  Lv.2: 300 raw XP | 원목 압4, 구리 주괴 압1
  Lv.3: 420 raw XP | 원목 압6, 철 블록 2
  Lv.4: 580 raw XP | 원목 압8, 다이아 8
  Lv.5: 780 raw XP | 원목 압12, 다이아 16
• 광물 보존 [T1] Lv.5 | total 2,680 XP
  Lv.1: 240 raw XP | 철 블록 2, 흑요석 32
  Lv.2: 360 raw XP | 철 블록 4, 흑요석 압1
  Lv.3: 500 raw XP | 다이아 8, 청금석 압2
  Lv.4: 680 raw XP | 다이아 블록 1, 레드스톤 압4
  Lv.5: 900 raw XP | 다이아 블록 2, 청금석 블록 8
• 광역 굴착 [T3] Lv.1 | total 1,600 XP
  Lv.1: 1,600 raw XP | 철 블록 8, 다이아 8, 레드스톤 블록 8, 흑요석 압2
• 자동 벌목 장치 [T3] Lv.1 | total 2,800 XP
  Lv.1: 2,800 raw XP | 원목 압16, 철 블록 12, 석탄 압8, 레드스톤 블록 8

[생산공학]
• 연료 절약 [T1] Lv.3 | total 1,080 XP
  Lv.1: 220 raw XP | 석탄 압2, 철 블록 2
  Lv.2: 340 raw XP | 석탄 압4, 블레이즈 막대 16, 철 블록 4
  Lv.3: 520 raw XP | 석탄 압8, 블레이즈 가루 압2, 금 블록 4
• 정밀 제련 [T1] Lv.5 | total 3,700 XP
  Lv.1: 320 raw XP | 철 블록 4, 금 주괴 압1
  Lv.2: 480 raw XP | 철 블록 6, 레드스톤 압2
  Lv.3: 700 raw XP | 금 블록 4, 네더 석영 압2
  Lv.4: 950 raw XP | 금 블록 8, 다이아 8, 발광석 가루 압4
  Lv.5: 1,250 raw XP | 금 블록 12, 다이아 블록 1, 블레이즈 가루 압4
• 정밀 제작 [T1] Lv.5 | total 3,370 XP
  Lv.1: 300 raw XP | 철 블록 3, 레드스톤 압2
  Lv.2: 450 raw XP | 금 블록 2, 네더 석영 압2
  Lv.3: 620 raw XP | 다이아 8, 청금석 압3
  Lv.4: 850 raw XP | 다이아 16, 슬라임볼 압3
  Lv.5: 1,150 raw XP | 다이아 블록 2, 에메랄드 블록 4, 레드스톤 블록 8
• 목재 가공 [T1] Lv.2 | total 790 XP
  Lv.1: 240 raw XP | 원목 압4, 판자 압8
  Lv.2: 550 raw XP | 원목 압12, 철 블록 2
• 장비 정비 [T1] Lv.5 | total 2,980 XP
  Lv.1: 260 raw XP | 철 블록 4, 네더 석영 압1
  Lv.2: 400 raw XP | 철 블록 6, 청금석 압1
  Lv.3: 560 raw XP | 금 블록 4, 다이아 8
  Lv.4: 760 raw XP | 철 블록 12, 레드스톤 압4
  Lv.5: 1,000 raw XP | 다이아 블록 2, 청금석 블록 8
• 고속 제련 [T2] Lv.3 | total 3,900 XP
  Lv.1: 850 raw XP | 석탄 블록 8, 레드스톤 압4, 구리 블록 8
  Lv.2: 1,250 raw XP | 석탄 블록 16, 금 블록 8, 블레이즈 가루 압2
  Lv.3: 1,800 raw XP | 석탄 블록 24, 금 블록 12, 다이아 블록 2
• 고속·고효율 화로 [T3] Lv.1 | total 3,500 XP
  Lv.1: 3,500 raw XP | 석탄 블록 32, 철 블록 16, 레드스톤 블록 16, 금 블록 8
• 상자 정렬 [T3] Lv.1 | total 1,600 XP
  Lv.1: 1,600 raw XP | 레드스톤 압4, 종이 압4, 철 블록 4, 에메랄드 압1

[생활기술학]
• 작물 재배 [T1] Lv.5 | total 2,740 XP
  Lv.1: 240 raw XP | 밀 압2, 밀 씨앗 압2
  Lv.2: 350 raw XP | 밀 압4, 당근 압2
  Lv.3: 500 raw XP | 감자 압4, 호박 압2
  Lv.4: 700 raw XP | 밀 압8, 황금 당근 16
  Lv.5: 950 raw XP | 밀 압16, 건초 더미 16
• 종자 연구 [T1] Lv.3 | total 790 XP
  Lv.1: 160 raw XP | 밀 씨앗 압2, 호박 씨앗 압1
  Lv.2: 250 raw XP | 밀 씨앗 압4, 수박 씨앗 압2
  Lv.3: 380 raw XP | 밀 씨앗 압8, 비트 씨앗 압4
• 토양 연구 [T1] Lv.3 | total 1,000 XP
  Lv.1: 180 raw XP | 흙 압4, 뼛가루 압1
  Lv.2: 320 raw XP | 흙 압8, 뼛가루 압3
  Lv.3: 500 raw XP | 흙 압16, 진흙 압8, 레드스톤 압2
• 가축 사육 [T1] Lv.3 | total 1,420 XP
  Lv.1: 260 raw XP | 밀 압4, 당근 압2, 가죽 압1
  Lv.2: 460 raw XP | 밀 압8, 가죽 압2, 생 소고기 압1
  Lv.3: 700 raw XP | 밀 압16, 황금 당근 32, 가죽 압4
• 양봉 연구 [T1] Lv.3 | total 1,080 XP
  Lv.1: 220 raw XP | 벌집 조각 압1, 민들레 압2
  Lv.2: 340 raw XP | 벌집 조각 압2, 꿀이 든 병 16
  Lv.3: 520 raw XP | 벌집 조각 압4, 꿀 블록 16
• 어업 연구 [T1] Lv.5 | total 2,600 XP
  Lv.1: 250 raw XP | 생대구 압2, 생연어 압1
  Lv.2: 360 raw XP | 생대구 압4, 실 압2
  Lv.3: 490 raw XP | 생연어 압4, 프리즈머린 조각 압2
  Lv.4: 650 raw XP | 생대구 압8, 생연어 압4
  Lv.5: 850 raw XP | 생대구 압8, 생연어 압8, 앵무조개 껍데기 4
• 3×3 경작 [T2] Lv.1 | total 850 XP
  Lv.1: 850 raw XP | 철 블록 6, 레드스톤 블록 4, 흙 압8
• 성숙 작물 일괄 수확 [T2] Lv.1 | total 1,050 XP
  Lv.1: 1,050 raw XP | 밀 압8, 사탕수수 압4, 뼛가루 압4, 다이아 8
• 먹이 투척·번식 [T2] Lv.1 | total 950 XP
  Lv.1: 950 raw XP | 밀 압8, 당근 압8, 황금 당근 32
• 낚시 기술 [T2] Lv.3 | total 3,300 XP
  Lv.1: 700 raw XP | 생대구 압4, 실 압4, 프리즈머린 조각 압2
  Lv.2: 1,100 raw XP | 생대구 압8, 생연어 압4, 청금석 압2
  Lv.3: 1,500 raw XP | 생연어 압8, 앵무조개 껍데기 4, 다이아 블록 1
• 자동 농장 [T3] Lv.1 | total 3,500 XP
  Lv.1: 3,500 raw XP | 밀 압16, 밀 씨앗 압16, 철 블록 16, 레드스톤 블록 8, 다이아 블록 2
• 관개 장치 [T3] Lv.1 | total 2,200 XP
  Lv.1: 2,200 raw XP | 흙 압16, 구리 블록 16, 프리즈머린 조각 압4, 청금석 블록 4
• 통발 개선 [T3] Lv.1 | total 1,800 XP
  Lv.1: 1,800 raw XP | 생대구 압16, 생연어 압8, 프리즈머린 조각 압4, 다이아 8

[전투학]
• 검술 [T1] Lv.3 | total 1,330 XP
  Lv.1: 250 raw XP | 철 블록 4, 썩은 살점 압2
  Lv.2: 430 raw XP | 다이아 8, 뼈 압2
  Lv.3: 650 raw XP | 다이아 블록 1, 화약 압3
• 도끼 전투 [T1] Lv.3 | total 1,220 XP
  Lv.1: 230 raw XP | 철 블록 4, 원목 압2
  Lv.2: 390 raw XP | 철 블록 8, 원목 압4
  Lv.3: 600 raw XP | 다이아 8, 원목 압8
• 사격 [T1] Lv.3 | total 1,330 XP
  Lv.1: 260 raw XP | 화살 압4, 실 압2
  Lv.2: 420 raw XP | 화살 압8, 화약 압2
  Lv.3: 650 raw XP | 화살 압12, 깃털 압4, 다이아 8
• 방패 [T1] Lv.3 | total 1,050 XP
  Lv.1: 200 raw XP | 철 블록 4, 판자 압4
  Lv.2: 330 raw XP | 철 블록 8, 가죽 압2
  Lv.3: 520 raw XP | 철 블록 12, 흑요석 압2
• 방어구 운용 [T1] Lv.3 | total 1,250 XP
  Lv.1: 250 raw XP | 가죽 압4, 철 블록 4
  Lv.2: 400 raw XP | 철 블록 8, 네더 석영 압2
  Lv.3: 600 raw XP | 다이아 8, 철 블록 12
• 투척 전투 [T1] Lv.3 | total 1,340 XP
  Lv.1: 270 raw XP | 철 블록 4, 프리즈머린 조각 압2
  Lv.2: 420 raw XP | 프리즈머린 조각 압4, 화살 압4
  Lv.3: 650 raw XP | 프리즈머린 조각 압8, 다이아 8
• 연속 검격 [T2] Lv.1 | total 900 XP
  Lv.1: 900 raw XP | 철 블록 12, 금 블록 4, 네더 석영 압4
• 반격 방패 [T3] Lv.1 | total 2,300 XP
  Lv.1: 2,300 raw XP | 철 블록 12, 다이아 블록 2, 흑요석 압4
• 전술 사격 [T3] Lv.1 | total 2,600 XP
  Lv.1: 2,600 raw XP | 화살 압16, 화약 압8, 레드스톤 블록 16, 다이아 블록 2

[생태학]
• 좀비 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,000 XP
  Lv.1: 200 raw XP | 썩은 살점 압4, 철 블록 2
  Lv.2: 320 raw XP | 썩은 살점 압8, 철 블록 4
  Lv.3: 480 raw XP | 썩은 살점 압16, 철 블록 8
• 스켈레톤 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,030 XP
  Lv.1: 210 raw XP | 뼈 압4, 화살 압2
  Lv.2: 330 raw XP | 뼈 압8, 화살 압4
  Lv.3: 490 raw XP | 뼈 압16, 화살 압8
• 크리퍼 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,250 XP
  Lv.1: 250 raw XP | 화약 압4, 흑요석 32
  Lv.2: 400 raw XP | 화약 압8, 흑요석 압1
  Lv.3: 600 raw XP | 화약 압16, 흑요석 압2
• 거미 [생태 계열 연구 / 티어 분리 완화] Lv.1 | total 850 XP
  Lv.1: 850 raw XP | 실 압8, 거미 눈 압2, 발효된 거미 눈 16
• 엔더맨 [생태 계열 연구 / 티어 분리 완화] Lv.1 | total 1,200 XP
  Lv.1: 1,200 raw XP | 엔더 진주 압3, 흑요석 압2, 엔더의 눈 8
• 슬라임 [생태 계열 연구 / 티어 분리 완화] Lv.1 | total 1,050 XP
  Lv.1: 1,050 raw XP | 슬라임볼 압8, 슬라임 블록 16, 마그마 크림 압2
• 일리저 [생태 계열 연구 / 티어 분리 완화] Lv.1 | total 1,000 XP
  Lv.1: 1,000 raw XP | 에메랄드 블록 4, 화살 압8, 철 블록 4
• 피글린 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,400 XP
  Lv.1: 300 raw XP | 금 블록 4, 우는 흑요석 8
  Lv.2: 450 raw XP | 금 블록 8, 우는 흑요석 16
  Lv.3: 650 raw XP | 금 블록 12, 우는 흑요석 32, 블레이즈 막대 16
• 호글린 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,160 XP
  Lv.1: 240 raw XP | 생 돼지고기 압4, 진홍빛 자루 압2
  Lv.2: 380 raw XP | 생 돼지고기 압8, 진홍빛 자루 압4
  Lv.3: 540 raw XP | 생 돼지고기 압12, 진홍빛 자루 압6
• 수중 적대 생물 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,260 XP
  Lv.1: 250 raw XP | 썩은 살점 압4, 프리즈머린 조각 압2
  Lv.2: 410 raw XP | 프리즈머린 조각 압4, 구리 블록 4
  Lv.3: 600 raw XP | 프리즈머린 조각 압8, 프리즈머린 수정 압2, 다이아 8
• 희귀 생물 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 1,780 XP
  Lv.1: 350 raw XP | 엔더 진주 32, 에메랄드 32, 프리즈머린 조각 32
  Lv.2: 580 raw XP | 엔더 진주 압1, 에메랄드 블록 4, 프리즈머린 조각 압2
  Lv.3: 850 raw XP | 엔더 진주 압2, 에메랄드 블록 8, 프리즈머린 수정 압4
• 보스 생태 [생태 계열 연구 / 티어 분리 완화] Lv.3 | total 2,800 XP
  Lv.1: 600 raw XP | 흑요석 압4, 블레이즈 막대 압2
  Lv.2: 900 raw XP | 엔더 진주 압4, 금 블록 8
  Lv.3: 1,300 raw XP | 엔더의 눈 16, 다이아 블록 2
• 생물 추적 장치 [T3] Lv.1 | total 2,800 XP
  Lv.1: 2,800 raw XP | 엔더의 눈 32, 에메랄드 블록 8, 다이아 블록 4, 레드스톤 블록 16

[세계학]
• 항법 [T1] Lv.3 | total 1,040 XP
  Lv.1: 200 raw XP | 원목 압4, 종이 압2
  Lv.2: 340 raw XP | 원목 압8, 구리 블록 4
  Lv.3: 500 raw XP | 원목 압16, 레드스톤 압4
• 승마 [T1] Lv.5 | total 2,390 XP
  Lv.1: 220 raw XP | 건초 더미 4, 가죽 압2
  Lv.2: 320 raw XP | 건초 더미 8, 가죽 압4
  Lv.3: 450 raw XP | 가죽 압8, 황금 당근 16
  Lv.4: 600 raw XP | 건초 더미 16, 황금 당근 32
  Lv.5: 800 raw XP | 건초 더미 24, 황금 당근 압1, 다이아 블록 1
• 비행 [T1] Lv.3 | total 1,550 XP
  Lv.1: 300 raw XP | 팬텀 막 8, 화약 압2
  Lv.2: 500 raw XP | 팬텀 막 16, 화약 압4
  Lv.3: 750 raw XP | 팬텀 막 32, 화약 압8, 다이아 8
• 고고학 [T1] Lv.3 | total 1,340 XP
  Lv.1: 260 raw XP | 벽돌 압2, 점토 덩이 압2
  Lv.2: 430 raw XP | 벽돌 압4, 에메랄드 16
  Lv.3: 650 raw XP | 벽돌 압8, 에메랄드 32, 다이아 8
• 주민 거래 [T1] Lv.3 | total 1,430 XP
  Lv.1: 280 raw XP | 에메랄드 블록 2, 종이 압4
  Lv.2: 450 raw XP | 에메랄드 블록 4, 종이 압8
  Lv.3: 700 raw XP | 에메랄드 블록 8, 책 32
• 피글린 교역 [T1] Lv.3 | total 1,360 XP
  Lv.1: 260 raw XP | 금 블록 4, 흑요석 압1
  Lv.2: 420 raw XP | 금 블록 8, 네더 석영 압4
  Lv.3: 680 raw XP | 금 블록 12, 우는 흑요석 16
• 생존·원정 준비 [T1] Lv.3 | total 1,020 XP
  Lv.1: 200 raw XP | 밀 압4, 가죽 압2
  Lv.2: 320 raw XP | 당근 압4, 양털 압2
  Lv.3: 500 raw XP | 밀 압8, 철 블록 4
• 정밀 비행 [T2] Lv.3 | total 5,500 XP
  Lv.1: 1,200 raw XP | 팬텀 막 16, 폭죽 로켓 압4
  Lv.2: 1,800 raw XP | 팬텀 막 32, 폭죽 로켓 압8, 네더 석영 압4
  Lv.3: 2,500 raw XP | 팬텀 막 압1, 폭죽 로켓 압16, 다이아 블록 2
• 동력 보트 [T3] Lv.1 | total 3,000 XP
  Lv.1: 3,000 raw XP | 원목 압16, 철 블록 16, 구리 블록 16, 레드스톤 블록 12
• 차원 간 물품 운송 [T3] Lv.1 | total 4,000 XP
  Lv.1: 4,000 raw XP | 엔더 진주 압4, 흑요석 압8, 다이아 블록 6, 엔더의 눈 32

[연금·마법학]
• 양조 기초 [T1] Lv.3 | total 1,040 XP
  Lv.1: 200 raw XP | 네더 사마귀 압2, 블레이즈 가루 32
  Lv.2: 340 raw XP | 네더 사마귀 압4, 레드스톤 압2
  Lv.3: 500 raw XP | 네더 사마귀 압8, 블레이즈 가루 압2
• 물약 연구 [T1] Lv.3 | total 1,300 XP
  Lv.1: 260 raw XP | 레드스톤 압4, 발광석 가루 압2
  Lv.2: 420 raw XP | 레드스톤 압8, 발광석 가루 압4
  Lv.3: 620 raw XP | 레드스톤 압12, 발광석 가루 압8
• 투척·잔류 물약 [T1] Lv.3 | total 1,280 XP
  Lv.1: 260 raw XP | 화약 압2, 발광석 가루 압1
  Lv.2: 420 raw XP | 화약 압4, 드래곤의 숨결 4
  Lv.3: 600 raw XP | 화약 압8, 드래곤의 숨결 8
• 상태 효과 [T1] Lv.3 | total 1,240 XP
  Lv.1: 240 raw XP | 발효된 거미 눈 압2, 레드스톤 압2
  Lv.2: 400 raw XP | 발효된 거미 눈 압4, 마그마 크림 압2
  Lv.3: 600 raw XP | 발효된 거미 눈 압8, 가스트의 눈물 8, 금 블록 4
• 마법 재료 [T1] Lv.3 | total 1,240 XP
  Lv.1: 240 raw XP | 블레이즈 막대 압2, 청금석 압4
  Lv.2: 400 raw XP | 엔더 진주 압2, 블레이즈 가루 압4
  Lv.3: 600 raw XP | 블레이즈 막대 압4, 엔더 진주 압4, 다이아 8
• XP 획득 [T1] Lv.3 | total 1,530 XP
  Lv.1: 300 raw XP | 청금석 압4, 네더 석영 압2
  Lv.2: 480 raw XP | 청금석 압8, 금 블록 4
  Lv.3: 750 raw XP | 청금석 압12, 에메랄드 블록 8, 다이아 블록 1
• 촉매 보존 [T1] Lv.3 | total 1,130 XP
  Lv.1: 220 raw XP | 네더 사마귀 압2, 레드스톤 압2
  Lv.2: 360 raw XP | 네더 사마귀 압4, 발광석 가루 압4
  Lv.3: 550 raw XP | 네더 사마귀 압8, 블레이즈 가루 압4
• 고급 양조 [T2] Lv.1 | total 900 XP
  Lv.1: 900 raw XP | 블레이즈 막대 압4, 네더 사마귀 압8, 금 블록 4
• XP 제어 [T2] Lv.2 | total 1,350 XP
  Lv.1: 500 raw XP | 청금석 압4, 엔더 진주 32
  Lv.2: 850 raw XP | 청금석 압8, 엔더 진주 압2
• 물약 혼합 [T2] Lv.1 | total 1,700 XP
  Lv.1: 1,700 raw XP | 네더 사마귀 압8, 발광석 가루 압8, 레드스톤 압8, 드래곤의 숨결 16
• 양조 공정 제어 [T2] Lv.1 | total 2,400 XP
  Lv.1: 2,400 raw XP | 네더 사마귀 압16, 블레이즈 가루 압8, 레드스톤 블록 12, 철 블록 8
• 물약 농축 [T3] Lv.1 | total 4,200 XP
  Lv.1: 4,200 raw XP | 네더 사마귀 압24, 블레이즈 막대 압8, 드래곤의 숨결 32, 다이아 블록 4, 네더라이트 주괴 2

ECONOMY INTERPRETATION
- The research prices are LOCKED. The research effects, unlock mechanics and basic equipment recipes continue to follow the V2 accepted checkpoint. A separate craftable tool or device also consumes its already-approved crafting ingredients upon actual crafting; no multiple finished gear/workstation inputs silently reappear as research payment.
- Precision Smelting includes all vanilla and MoreMinecrafts ores, while auto-smelt enchant bonus drops stay independently processed; eventual loot interaction audit is required. Large material costs (logs, crops, creature drops) use 64:1 compression, but scarce goods are priced as literal quantities.
- No additional exact costs may be invented for removed nodes or for tactical ammunition subtypes whose design was deferred. Any player-facing display must render each level independently and preview total XP and each required material before purchase.

NEXT STAGE — PREPARATION ONLY, NOT YET USER-APPROVED NEW MECHANICS
1) Complete natural advancement/research graph connections: T2 must require a related T1, T3 a related T2; avoid misfit links (e.g., shield<-sword, motorboat<-elytra). Gaps: resource studies needs T2 bridges before 3×3 mining / automated lumber; production chest sorting T3 needs relevant T2 storage link; combat counter-shield/tactical shooting need corresponding T2 lines; ecology tracker needs T2/lineage policy; world motorboat and delivery need valid T2 branches. Identify minimal remedies (reclassification, meaningful branch link, or genuinely needed nodes) without silently adding content.
2) Then calculate XP supply/one-time development quest (발전 과제) rewards from 130,360 research XP maximum plus existing enchant/book/anvil/equipment XP systems. No invented daily quest or assumption every player purchases all 70 research nodes.
3) Audit compressed research-only item transaction safety, exact XP+inventory atomicity, recipe selection/shift-click, save+reload/dupe, player ownership and shared machines, custom crop/ore/mob compatibility and Minecraft Fabric client/server testing. This remains a follow-up, not checked off by documentation.
4) Keep 3×3 mining tool (150 durability, no enchantment, lower-cost recipe) and 3×3 hoe (100 durability, approved recipe) CRAFTING costs separate from research unlock purchase costs. Automated logging is sequential with break sound; do not reinterpret research material costs as fuel/upkeep.
============================================================

============================================================
T2 RESEARCH BRANCH ADDITIONS — USER-APPROVED SELECTIONS
DECISION STATUS: [USER-APPROVED CONCEPT / SCOPE AS SPECIFIED], NOT code implementation and NOT blanket approval of suggested numerical values. This post-V3 addendum records the user's latest selections without changing previously locked effects, 70 knowledge nodes, 70 priced research nodes, 178 priced level purchases, 130,360 raw XP baseline, or recipe economies.
PRIORITY: Where previous exploratory T2 ideas conflict with this section, these user choices supersede exploratory descriptions. All unselected ideas remain candidates only. The new research effects must be reconciled into a future research graph and newly priced separately after full tier placement; do not silently add purchases or alter V3 cost totals.

A. COMBAT — PREVIOUSLY SELECTED (ADOPTED DESIGN DIRECTION)
1) [APPROVED CONCEPT] 방패 반격 준비 / Shield Counter Preparation (T2 proposed): precise/timed successful shield defense empowers a subsequent melee counterattack; this is a reactive preparation mechanic, not the T3 counter-shield's automatic attacker knockback. Exact perfect-block timing window, boosted damage, duration, applicability, cooldown and input implementation NOT LOCKED. Natural intended branch: T1 방패 -> T2 방패 반격 준비 -> T3 반격 방패, but exact graph edges await formal graph approval.
2) [APPROVED CONCEPT] 탄도 제어 / Ballistic Control (T2 proposed): show the EXPECTED projectile impact point / 예상 탄착 위치 during bow/crossbow aiming; explicitly prediction/visual feedback, NOT gravity reduction, trajectory flattening, auto-aim or increased projectile speed. Projectile physics, collision prediction, draw/charge handling, marker rendering, mod compatibility and server/client mechanics need validation. Natural intended branch: T1 사격 -> T2 탄도 제어 -> T3 전술 사격, graph edges to be confirmed.

B. RESOURCE + PRODUCTION — CONCEPT APPROVAL ONLY, DETAILED RULES DEFERRED
3) [IDEA-ONLY ADOPTED; DEFER DETAIL] 제어 벌목 / Controlled Logging (T2 proposed): adopt the GENERAL IDEA of player-directed controlled logging as research content, with the behavior/range/number of broken logs, ordering, durability, sound, wood definitions, protection and other mechanics left for future user design. Earlier assistant suggestion 'automatically break 2–4 extra logs' is NOT an approved numerical requirement. Plausible T1 벌목 -> T2 제어 벌목 -> T3 자동 벌목 장치 path remains candidate.
4) [IDEA-ONLY ADOPTED; DEFER DETAIL] 호퍼 필터링 / Hopper Filtering (T2 proposed): adopt the GENERAL IDEA of filtering item transfer through vanilla hoppers. Whitelist/blacklist, filter source/storage, GUI, performance, automation permissions, protected component handling, throughput and exact implementation are NOT approved yet. T2 -> T3 상자 정렬 is a suggested connection, NOT yet final edge.

C. PRODUCTION — DETAILED ACCEPTED BEHAVIOR FOR 주변 재료 참조
5) [APPROVED FUNCTIONAL DIRECTION] 주변 재료 참조 / Nearby Chest Ingredient Reference (T2 proposed):
   • Search/usage range is '1청크' as the user explicitly stated. The exact geometric interpretation (the single chunk containing the crafting table versus 1-chunk-radius neighboring chunks) was interpreted differently in an assistant suggestion and MUST BE CONFIRMED BEFORE IMPLEMENTATION; do not silently bind the user's words to a 3x3-chunk or single-chunk-only rule.
   • Use nearby CHEST inventory content in addition to the player's inventory when resolving crafting-table recipe inputs (not a general remote-crafting system).
   • Move/allocate materials from eligible chests ONLY when the player triggers RECIPE-BOOK AUTO-FILL/AUTO-COMPLETE. Never withdraw storage contents merely on hovering/previewing recipes or ordinary manual crafting.
   • Pull ONLY the necessary quantity for the chosen recipe/autofill; no automatic stack-wide drain or nonrequested transport.
   • On cancelling the crafting-table action without completing the craft, RETURN unused materials to their source storage. After partial craft, only inputs consumed by successful crafts remain consumed; unspent reserved/placed materials are returned on cancellation/close.
   • [CONDITIONAL: '가능하면'] BEFORE auto-fill, the recipe-book's craftable/un-craftable status IN THE CRAFTING TABLE should include eligible nearby chest materials. This display is desirable if feasible; no claim it is implemented or fully guaranteed.
   • [SAFETY FOLLOW-UP, NOT USER-LOCKED MECHANICS] Protect exact item components, stack accounting and ownership; guarantee no duplicate/lost items under concurrent access, chest removal/full inventory, disconnect, partial crafting, server restart, shift-click, protected blocks/claim plugins, chunk unloading. Decide safe fallback policy only after implementation research. Standard crafting-table scope; do not extrapolate unsupported other workstations.
   • Auto-fill input correctness and cancellation rollback must be server authoritative; implementation and compatibility proof pending. Detailed recipe selection / transaction atomicity deferred.

D. COMBAT — NEW USER-APPROVED RESEARCH DIRECTIONS
6) [APPROVED EFFECT DIRECTION] 활 차징 기동성 / Bow Charging Mobility: lower the movement-speed penalty while CHARGING A BOW. Exact penalty reduction, levels, placement as separate T2 node versus combined effect, and interactions with current bow mechanics are not specified. DO NOT replace this with bow draw-time reduction, which already exists in the T1 사격 research; do not extend to crossbows without approval.
7) [APPROVED CONCEPT] 도끼 낙하 강타 / Falling Axe Smash (T2 candidate): accept axe-based falling attack as a distinct combat mechanic. Fall-height trigger, bonus damage/AoE, fall-damage mitigation, cooldown, cap and visuals are NOT finalized, and should not be invented as locked values.
8) [APPROVED MECHANICAL PRINCIPLE] 무기 교체 숙련 / Weapon Swap Handling: use a weapon-switch/readying SPEED DISTINCT FROM the equipped weapon's attack speed (e.g., even a slow-swing axe may be readied quickly when swapping weapons). Do NOT tie swap draw/ready time directly to axe/sword attack speed. Exact switch timing, attack-cooldown carryover, swap-spam exploit prevention, whether this is a dedicated T2 research node or combat-system modifier, and inter-mod interaction remain unfinalized. Do not assume swapping resets ordinary attack charge; that would require independent deliberate approval.

E. SCOPE, DEPENDENCIES, ECONOMY, VERIFICATION
• Confirmed selections: 8 concept/mechanic entries in this addendum. Two 'idea only' entries must retain a prominent deferred-detail marker. 주변 재료 참조 includes precise selected UX rules plus conditional preview support. No additional numerical stats were approved.
• The two previous T2 combat choices (Shield Counter Preparation and Ballistic Control) are NOT implementations and are not authorized to be included automatically in costs without follow-up tier/graph/cost approval.
• Prior assistant suggestion that the gaps are 'nearly solved' was a DESIGN ASSESSMENT, not an authorized new node graph. Remaining likely T2 gaps: mining -> T3 area mining, ecology -> T3 tracker, world -> T3 powered boat and cross-dimensional delivery. Branch topology, prerequisite level, placement and number of new research purchases are pending.
• Earlier V3 locked base cost total remains 130,360 raw XP for 70 PREVIOUSLY PRICED research nodes / 178 purchases. These new candidates may increase it in a later user-approved V4; do not report unchanged total as a complete total including future additions.
• No coding, tests, CI, modpack research, fabricated compatibility guarantees, or remote branch merge claimed by this addendum.
============================================================

============================================================
T2 NEARBY CHEST RANGE — USER CLARIFICATION (2026-10-09)
STATUS: USER-APPROVED, OVERRIDES THE PRIOR UNRESOLVED CHUNK-GEOMETRY NOTE.
주변 재료 참조 / Nearby Chest Ingredient Reference: the '1청크' range means a fixed 16×16-block HORIZONTAL AREA centered/aligned relative to the crafting table, NOT the actual Minecraft chunk grid or chunk borders. The search must NOT abruptly stop at chunk boundaries; a crafting table near a chunk edge can include eligible chests on the other side of that boundary if inside the 16×16 area. Vertical Y range, exact centered even-width rectangle origin/tie-breaking, access permissions and performance bounds remain implementation details requiring confirmation; do not infer a 16×16×16 volume or 3×3 chunk radius. Previously approved rules remain: nearby chest material included in recipe-book craftability if feasible, chest withdrawals ONLY upon recipe-book auto-fill and only required quantities, unspent materials returned when crafting is cancelled/closed, with safe server-authoritative transactions.
Remaining T2 branches for next design: resource mining -> T3 3×3 mining tool; ecology observation/tracking -> T3 mob tracker; world navigation/transport -> T3 powered boat and cross-dimension home delivery. Generate candidate designs for user selection; DO NOT approve any new candidates, prices or topology without user decision.
============================================================

============================================================
FINAL USER DECISIONS — VANILLA-PLUS RESEARCH CANDIDATES AND KNOWLEDGE-GATED RECIPES (2026-10-10)
AUTHORITY: This section records the user's explicit selections/corrections and supersedes older contradictory [IDEA-ONLY] candidate statements. Design approval ONLY. No implementation, cost rebalance, main merge, or tier/graph-level price authorization. Do not turn unselected earlier suggestions into approved nodes.
DESIGN PHILOSOPHY: MoreMinecrafts is a vanilla-expansion exploration/fantasy mod, NOT an industrial technology/factory mod. Research should unlock novel actions, nature/mob interaction and convenient vanilla crafting, not force new industrial machines.

A. REMOVALS / MERGES — FINAL
1. [REJECTED / REMOVE FROM FUTURE CANDIDATE ROSTER] 제어 벌목 / Controlled Logging T2. This reverses the earlier "idea only adopted" note. Do NOT keep a separate paid node. Existing Lumberjack (나무꾼) enchant already handles connected tree felling. The existing separately approved T3 자동 벌목 장치 / automated lumber machine IS RETAINED UNCHANGED: 1x1 machine, axe+coal, processes logs sequentially, 1 coal/8 logs, 1 axe durability/3 logs, drops above, no chunk force load. Its scope is a single chunk-local tree, NOT automatically all trees across a 16x16 area; broad-area harvesting was not approved. Any T2 prerequisite link for this T3 remains to be designed.
2. [REJECTED] 야전 수리 / Field Repair: remove from candidate roster after overlap audit. Existing T1 장비 정비 (anvil-material recovery boost and XP discount), vanilla anvil/grindstone, Mending enchantment remain. No replacement repair node implicitly approved.
3. [MERGED; NO SEPARATE PAID RESEARCH] 화로 잔열 활용 / Furnace Residual Heat is incorporated into existing T1 연료 절약 / Fuel Saving concept. Preserve its locked L1–3 fuel-saving percentages 8.5/17/25.5%, applicability to all approved furnace families, and existing smelting/Furnace balance. Heat usage effect details, technical interaction and stacking semantics still need separate verification; do NOT invent an extra bonus magnitude or free-fuel processing beyond the locked economy.
4. [MERGED] 절벽 매달리기 / Ledge Hang is incorporated into 암벽 등반 / Wall Climbing rather than a second research node; relax overly strict pickaxe-only assumptions in the later integrated movement design. Exact equipment requirement, climb duration, stamina, grab conditions and collision/movement safety pending.
5. [REJECTED] 연쇄 벌목 / Chain Logging as separate research, because already overlaps Lumberjack enchant. Any former implementation examples are not adopted.

B. NEWLY USER-ACCEPTED FEATURES — CONCEPTS AND SPECIFIED INPUTS
1. [ACCEPTED; T2 CANDIDATE] 곡괭이 투척 / Throwable Pickaxe: throw a pickaxe to mine an eligible distant block and recover the tool. Projectile reach, loot, durability, enchantment interactions and recovery safety not yet locked.
2. [ACCEPTED; INTEGRATED WITH LEDGE HANG] 암벽 등반 / Wall Climbing: climb/grab cliff/rock surfaces; includes ledge hanging as part of same skill. Final controls and allowed materials to be designed.
3. [ACCEPTED CONCEPT; DESIGN PENDING] 자동제작 / Inventory-Triggered Auto Crafting is NOT "repeat crafts at a workbench" and NOT simply vanilla redstone Crafter. Users search/filter selectable craft results/recipes in a dedicated UI and register MULTIPLE desired automatic craft rules. Whenever items are ACQUIRED into the player's inventory, the system detects availability and automatically crafts registered results. Multiple recipes may chain into a player-defined automation/script-like graph. UI, event semantics, priorities, rule limit, minimum reserve, recipes with multiple valid variants, recursive crafting cycles, crafted-item re-entry, ownership, shift-click, nested inventory, XP/refund/material preservation, server authoritative atomicity, output full and other anti-dupe behaviors ARE NOT LOCKED. Do not infer unconditional craft on any inventory change beyond the user-specified item acquisition event. Also do not conflate with the distinct previously approved nearby chest ingredients feature (16x16 BLOCK horizontal range independent of actual chunk grid, chest withdrawals only during recipe-book auto-fill).
4. [APPROVED DESIGN PRINCIPLE — NOT NECESSARILY SEPARATE PAID RESEARCH] 레시피 확장 / Knowledge-Gated Recipe Expansion: add NEW vanilla-compatible and alternative-ingredient recipes progressively upon unlocking relevant FREE knowledge entries; do not delete/overwrite ordinary vanilla recipes. May additionally attach recipes to research unlocks when thematically justified; but USER'S PREFERRED PRIMARY METHOD IS knowledge unlock -> incremental new recipes. Exact recipes, categories, substitute equivalence, costs/benefits, visual discoverability and acceptance tests pending. Do not assume a standalone T2 research node is required.
5. [ACCEPTED] 자동 도구 교체 / Automatic Tool Swap: on left-click on a block, identify the targeted block and swap suitable tool from inventory/hotbar before mining. Tool-selection priority, hand/slot restoration and interactions with Lumberjack, Vein Mining, Auto Smelt, cooldown and components need design. Not the same as T1 Equipment Maintenance / 장비 정비.

C. ECOLOGY / COMBAT / WORLD ACCEPTED IDEAS
1. [ACCEPTED CONCEPT] 몹 어그로 전환 / Hostile Target Redirection: when a hostile mob such as creeper/skeleton initially acquires player as target due to aggro, a RESEARCH-BASED PROC CHANCE may redirect to NEAREST ELIGIBLE ENTITY, EXCLUDING villagers, animals and other friendly/passive mobs; goal is hostile mobs fighting each other, and creepers detonating against the hostile target if technically supported. Exact eligible mob tag, chance, retaliation/fuse AI, self-target protection, event-per-aggro checks and cooldown pending; preserve vanilla AI safety and custom mob support.
2. [ACCEPTED T3] 몹 포획 / Mob Capture: convert captured entity (pig, zombie and in principle all eligible vanilla/MoreMinecrafts mob species, rather than a whitelist silently excluding them) to a SPAWN EGG that STORES THAT INDIVIDUAL'S DATA and can later reconstruct it. Data includes individual attributes/components/state as supported; capture exclusion/boss/ownership/UUID anti-duplication and restore rules pending. NOT ordinary species-only spawn egg.
3. [ACCEPTED T3 CONCEPT] 피글린 거래 / Piglin Trading: unlock villager-LIKE transaction interaction/UI with piglins rather than only vanilla random bartering. Exact offered inventory, exchange, prices, AI friendliness and non-undermining relation to existing T1 bartering study pending.
4. [FUTURE EQUIPMENT IDEA ONLY] water-themed armor or equipment may protect from / ignore Warden sonic boom; not approved as an existing item or a separate research node.
5. [ACCEPTED, INCORPORATE INTO EXISTING T3 TACTICAL SHOOTING] 로프 화살 / Rope Arrow as ONE SPECIAL ARROW TYPE, not new independent T3 research node. Exact rope climb, attachment, arrow consumption, physics and crafting pending.
6. [ACCEPTED] 벽면 도약 / Wall Jump: on touching wall, player can STICK for several to several dozen TICKS as a grace period (NOT immediate wall bounce), and jump DURING that window to launch AWAY from wall. Exact tick window/impulse and collision handling pending.
7. [ACCEPTED] 낙하 구르기 / Fall Roll: use SNEAK/CROUCH key near landing to roll and REDUCE fall damage. Timing window/amount and interactions with armor/Feather Falling/other mod enchantments pending.

D. PREVIOUS SELECTED RESEARCH REMAINS IN FORCE
Earlier approved Shield Counter Preparation, Ballistic Control expected impact marker (not auto-aim), Bow Charging Mobility, Falling Axe Smash, separate Weapon Swap Ready Speed, nearby chest ingredient reference with 16x16 BLOCK search independent of chunk borders, and Hopper Filtering [IDEA ONLY, DETAILS DEFERRED] remain accepted subject to their older status and scope. Do not revive Controlled Logging from prior appendices.

E. ECONOMY / VALIDATION
Previously locked priced roster (70 paid research nodes / 178 level purchases / 130,360 raw XP) remains a HISTORICAL APPROVED BASELINE ONLY; new candidate nodes and all new knowledge-gated recipes are not added to pricing or formally wired in graph yet. No claims of compiled/live tests or feature implementation. Keep original T3 lumber-machine specs, item recipes and all earlier approved research effects unless specifically overridden above.
============================================================

============================================================
FINAL CORRECTION — DISCIPLINE ROOT NODES AND MINIMAL AUTO-CRAFT UX
AUTHORITY: USER-DIRECT FINAL CLARIFICATION. This section supersedes any earlier assistant proposal for an intermediate '전투 기초' or '생태 기초' purchase-node, separate route, full skill-tree editor, scripting UI, prioritization UI, or rule-chain UI. Concept/design only; no code, cost ledger, or locked XP edits.

A. DISCIPLINE ROOTS AS SMALL PAID RESEARCH NODES
- The EXISTING LARGE DISCIPLINE CATEGORIES THEMSELVES, such as 전투학 (Combat) and 생태학 (Ecology), may be low-cost, small-reward research targets and act as the skill-tree branch-access node.
- The root/category node branches DIRECTLY into its existing research children. This is NOT the creation of any additional intervening node such as '전투 기초'/'생태 기초', NOT a new extra research path, and NOT an instruction to insert starter nodes between every tier.
- Focus this lightweight category-as-research design on especially branched areas such as Combat and Ecology; do NOT indiscriminately require all seven disciplines to receive a new intermediate node. Tier labels T1/T2/T3 are development classifications, and a valid dependency can skip T2 and connect T1 directly to T3.
- Exact tiny reward, minimal cost, which categories receive paid root treatment, whether category-root purchase gating affects existing knowledge and research, and concrete edges remain for follow-up design. No silent global branch redesign, separate path or extra prerequisite approved.

B. AUTOMATIC CRAFTING UI — SIMPLE JEI-LIKE INVENTORY SEARCH
- [APPROVED] UI is a MINIMAL inventory-attached, JEI-like recipe/item search. Click a searched item to toggle its auto-craft ACTIVE state.
- Provide ONLY ONE ADDITIONAL per-item toggle/control for putting a selected auto-craft target ON HOLD / PAUSED as a bookmark/favorite for later, without crafting it. This is a small additional toggle, not a separate extensive rule editor, priority arrangement, custom scripting language, or complex workflow screen. Distinguish OFF, ACTIVE, HELD; ensure paused items are not active.
- Auto-crafting concept remains: acquiring items into the player's inventory triggers crafting of selected, compatible recipe targets when inputs become available; multiple selected outputs may support natural chained crafting. The player voluntarily selects recipes; do NOT add user-facing cycle-breaking settings or arbitrary rules screens as if required.
- [NEW APPROVED CONFLICT RULE] Two DIFFERENT auto-craft recipes that REQUIRE THE SAME/OVERLAPPING INPUT MATERIAL are NOT ALLOWED TO BE ACTIVE SIMULTANEOUSLY. Example: fence and trapdoor both use wooden planks (even though fence has additional stick requirements); selecting another auto-craft recipe that competes for any same ingredient type must be blocked from activating simultaneously. A held/bookmarked target is not active and may remain selected. Enforce competition using actual ingredient match/alternatives, with details about ingredient tags/variant resolution, quantities and conflict UX still to be implemented. Do not assume requiring an IDENTICAL entire recipe ingredient list is necessary; overlapping demand for material is enough. The requirement applies to concurrent active recipe targets, not to adding additional vanilla recipes or free knowledge unlocks.
- Server-side elementary safety guard may still cap work per acquisition event to prevent re-entrance, but should not surface complex scripting or configuration tools in UI. No priority editor is authorized.
- Keep prior research links, recipe expansion via free-knowledge unlock, nearby chest 16x16 BLOCK horizontal range independent of Minecraft chunk borders, no automatic chest withdrawals except recipe-book autofill, and all previously final accepted mechanics.
- This USER clarification supersedes prior assistant text referring to an additional '전투 기초' node or elaborate auto-craft priorities/modes. The earlier display/mockup was only an illustrative proposal, not user-approved design.

C. CONTINUITY
- Piglin villager-like T3 trading belongs under WORLD studies / 세계학, not Ecology. T1 piglin barter bonus remains.
- Controlled Logging T2 and separate Field Repair remain rejected; T3 lumber machine retained; Furnace Residual Heat folded into existing Fuel Saving; ledge hanging integrated with Wall Climbing; T3 Rope Arrow embedded in Tactical Shooting.
- Research mindmaps are topology drafts only; do not falsely claim that paid prerequisite graph was previously ratified or implemented. Respect already confirmed functions/cost baseline until explicit rescope.
============================================================

============================================================
USER-APPROVED KNOWLEDGE-GATED CRAFTING RECIPES — FIVE SELECTIONS
AUTHORITY: USER-DIRECT acceptance of ONLY the listed five recipes from an assistant's 24-candidate brainstorming pass. These unlock through existing FREE knowledge entries (numbers/names below), without making new paid research nodes. Keep the original vanilla recipes available, with new alternatives coexisting. Approval is DESIGN ONLY, not code, registered JSON, implemented unlock, art, tested balancing or main merge.

[APPROVED] #15 정밀 제작 (Production knowledge): 4 wooden planks + 2 sticks -> 2 trapdoors (다락문). New ALTERNATIVE recipe to existing vanilla trapdoor crafting, not replacement. Determine compatible wood species/output type, shaped geometry and recipe-book mapping in implementation design. Note vanilla's trapdoor baseline is 6 planks -> 2 trapdoors, so added 2 sticks is still lower wood-equivalent consumption than vanilla, but not universally in all resource contexts.
[APPROVED] #25 가축 사육 (Living knowledge): 3 leather + 2 string -> 2 leads (끈). Alternative to existing vanilla lead recipes; keep original.
[APPROVED] #42 스켈레톤 생태 (Ecology knowledge): 2 bones + 1 flint -> 4 arrows (화살). NO stick in this approved alternative. Preserve original vanilla arrow recipe; distinguish material substitutes and shape placement later.
[APPROVED] #44 거미 생태 (Ecology knowledge): 9 string -> 1 cobweb block (거미줄). The material count is nine string, not one.
[APPROVED] #57 비행 관찰 (World knowledge): 4 wool + 2 string + 1 leather -> 1 parachute (낙하산), a NEW ITEM; actual behavior, cooldown, durability/consumption, deployment, model, fall-roll/elytra overlaps remain TO BE DESIGNED. Do not claim those unspecified mechanics are approved.

[EXPLICITLY NOT APPROVED; REJECTED FROM THIS BATCH]
#17 보관 기술: 6 planks + 2 logs -> 1 chest; criticized as MORE EXPENSIVE than existing vanilla chest, do NOT add.
#20 작물 생장: 1 hay bale -> 3 bread; criticized as MORE EXPENSIVE than converting the hay back to wheat and using ordinary bread recipe, do NOT add.
All other unselected recipe suggestions from the 24-candidate set remain NOT APPROVED; neither insert nor price them as confirmed designs. The five newly approved recipes do not automatically approve their exact crafting-grid arrangement or currently absent client assets. The 70 preexisting knowledge acquisition rewards and research cost baseline remain untouched.
============================================================

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

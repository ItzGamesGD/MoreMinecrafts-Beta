# MoreMinecrafts 콘텐츠 최종 확정 체크리스트

> **Historical note:** The Enhancement +30/+40 entries below are superseded by the Stage 10 Enhancement 2.0 redesign and Stage 2 reconciliation. Preserve this checklist as history; do not use those old caps or cost bands as current rules.

> 목적: 이 문서는 Mod 이식 단계와 별개다.
> 현재 작업은 최종 Paper 플러그인과 기존 설계 위에 새 콘텐츠 결정을 덧씌워, MoreMinecrafts의 최종 게임 디자인을 잠그는 단계다.
> Stage 1~2의 실제 결정 과정·보류·폐기·최종 상태는 `docs/CONTENT_FINALIZATION_RESTORED_RECORD_STAGE_1_2.md`에 복원되어 있다. 체크리스트는 상태판이며 복원 기록과 각 단계 확정안이 상세 source of truth다.
> 구현 방식은 나중의 Fabric Mod Stage에서 다시 설계하되, 여기서는 "무엇을 만들 것인가"를 먼저 확정한다.

## 상태 표기
- [LOCKED] 콘텐츠 설계 확정. 명시적인 재설계가 없는 한 유지.
- [DECIDE] 사용자 최종 결정 필요.
- [PROVISIONAL] 현재 단계에서 임시 확정. 구현/플레이테스트에서 세부 수치나 방식 재조정 가능.
- [BALANCE] 기능은 확정, 수치/확률/비용 조정 필요.
- [IMPLEMENTATION] 설계는 확정, Fabric 구현 방식은 Mod Stage에서 결정.
- [REVIEW] 기존 Paper 구현/패키지와 대조 필요.
- [OPTIONAL] 없어도 전체 구조가 깨지지 않는 후보.

---

# 0. 전체 진행 원칙

- [LOCKED] 바닐라를 대체하지 않고 바닐라의 끝을 확장한다.
- [LOCKED] Coin 경제, 직업 레벨, Mining GUI, 별도 대형 제작 GUI를 다시 도입하지 않는다.
- [LOCKED] 제작은 기본적으로 Crafting Table / Furnace / Blast Furnace / Anvil / Smithing Table / Brewing Stand 등 바닐라 인프라를 확장한다.
- [LOCKED] Paper 코드는 최종 reference implementation이며, Fabric에서 줄 단위로 이식하지 않는다.
- [LOCKED] 현재 단계는 Mod migration이 아니라 콘텐츠 설계 finalization이다.
- [LOCKED] 콘텐츠 간 의존성은 허용하되, 시스템 분류는 분리한다. 예: Titanium이 낚시 장비에 쓰여도 Fishing은 Mining의 하위 시스템이 아니다.

---

# 1. Resource / Mining / Materials 축

## 이미 확정
- [LOCKED] 실제 광물/재료 성질에서 기능을 출발한다.
- [LOCKED] 비슷한 요구는 재료를 통합해 해결한다.
- [LOCKED] 재료 수를 채우기 위한 억지 광물 추가 금지.
- [LOCKED] 합금 중심 체계 대신 직접 재료 사용을 우선한다.
- [LOCKED] Titanium 사용.
- [LOCKED] Tungsten 사용.
- [LOCKED] Mythril 사용.
- [LOCKED] Adamantium = 절대적/극단적 내구 성격.
- [LOCKED] 원소 재료는 여러 장비/시스템에서 재사용 가능.
- [LOCKED] Aluminum은 현재 필수 재료 아님.
- [LOCKED] Aquamarine / Etherium은 coating에서 제외.
- [LOCKED] Adamantium / Etherium 모두 자연 월드젠을 통해 survival 획득 가능.

## 사용자 결정 필요
- [LOCKED] 최종 추가 광석 목록: Titanium / Tungsten / Mythril / Adamantium / Etherium.
- [LOCKED] 차원: Titanium / Tungsten / Mythril / Etherium = Overworld, Adamantium = Nether.
- [PROVISIONAL] 생성 높이/분포: Titanium Y -16~80(피크 32), Tungsten Y -48~32(피크 -16), Mythril Y -64~0(피크 -48), Etherium Y -32~64(피크 16), Adamantium = Ancient Debris 유사 초안.
- [PROVISIONAL] 희귀도: Titanium/Tungsten은 Copper보다 희귀하고 Iron보다 흔한 체감 목표, Mythril/Etherium은 Diamond보다 희귀, Adamantium은 Ancient Debris 유사.
- [PROVISIONAL] 광맥: Mythril/Etherium 최대 6블록, Titanium/Tungsten은 높이 범위를 제외한 광맥/placement 출발 규칙을 Iron과 동기화, Adamantium은 Ancient Debris 유사.
- [PROVISIONAL] Silk Touch는 전 광물 공통으로 광석 블록 획득. Fortune은 Titanium/Tungsten=현대 Iron/Gold 대응, Mythril/Etherium=Diamond 대응, Adamantium=Ancient Debris처럼 미적용.
- [PROVISIONAL] 가공: Titanium/Tungsten = ore → raw → ingot, Mythril/Etherium = ore → 완성 material 직접 드롭, Adamantium = ore/debris형 블록 → 제련 material.
- [LOCKED] Mythril은 Overworld 자연 월드젠 광석.
- [LOCKED] Adamantium은 Nether 자연 월드젠으로 survival 획득 가능.
- [LOCKED] Mythril/Etherium 등 현재 fantasy mineral은 기본적으로 자연 월드젠 채굴로 획득.
- [BALANCE] 재료별 장비 recipe 수량.
- [PROVISIONAL] 실제 체감 공급량의 상대 목표만 확정. 세부 수치는 구현 후 플레이테스트에서 조정.

## 구현 단계에서 확인
- [IMPLEMENTATION] Fabric worldgen registration.
- [IMPLEMENTATION] ore block/item/tag/datagen 구조.
- [IMPLEMENTATION] vanilla smelting/blasting recipe 연동.
- [IMPLEMENTATION] GameTest/worldgen sanity test.

---

# 2. Equipment Progression 축

## 이미 확정
- [LOCKED] 무기 T1 = Vanilla.
- [LOCKED] 무기 T2 = Elemental.
- [LOCKED] 무기 T3 = Special.
- [LOCKED] 무기 T4 = Endgame.
- [LOCKED] 갑옷은 무기보다 적은 수의 큰 Tier를 사용.
- [LOCKED] Enhancement = 주로 수치 성장.
- [LOCKED] Coating = 기능적 성장.
- [LOCKED] Enchantment = Minecraft native enchant 체계를 중심으로 확장.
- [LOCKED] Promotion 시스템은 폐기.
- [LOCKED] Coin / Fragment 기반 장비 성장은 폐기.

## 사용자 결정 필요
- [LOCKED] T2 Elemental roster: 불꽃검 / 흐르는 물의 검 / 바람을 가르는 검 / 바람궁수의 활 / 대지의 철퇴 / 마기가 깃든 지팡이. Ice T2 장비는 없음.
- [LOCKED] T2 기본 타입: Fire=Sword, Water=Sword, Wind=Sword+Bow, Earth=Mace, Ice=없음, Dark Energy=Staff.
- [IMPLEMENTATION] T2는 Paper 능력의 핵심 컨셉을 계승하되, 모델링/VFX/SFX/정확한 능력치/쿨다운/범위/키 입력/세부 판정은 Fabric 구현 단계에서 재설계.
- [LOCKED] T4 Endgame baseline = 무극검 / 천궁 / 천서 3종. 세계수 활은 baseline 밖의 OPTIONAL 후보이며 필요 시 T3로 재분류 가능.
- [LOCKED] T1/T2/T3/T4는 서로 독립적인 설계 카테고리이며 진화/승급 트리가 아니다. 특정 recipe/해금 조건에서 다른 Tier 장비를 요구할 수는 있으나 장비 상속은 아니다.
- [LOCKED] T3 Special 7종 전부 유지: 타나토스의 철퇴 / 포세이돈의 창 / 화염 도끼 / 뇌신의 도끼 / 성검 · 솔라리스 / 월야잔광 / 빙결검. 핵심 정체성은 유지하고 Fabric 표현·구현은 재해석.
- [SUPERSEDED / HISTORICAL] Enhancement 상한 기록: Vanilla +30 / T2 Elemental +40 / T3 Special·T4 Endgame 일반 강화 불가.
- [SUPERSEDED / STALE] Enhancement 비용 baseline 기록: Upgrade Stone 1개 고정 + XP level, 구형 +30/+40 범위. 플레이테스트에서 조정 가능하다고 기록했으나 현재 비용 규칙이 아님.
- [LOCKED] Enhancement 성공률 100%. 실패/하락/파괴/실패 누적 보정 없음. Anvil + Upgrade Stone + Minecraft XP 구조 유지.
- [PROVISIONAL] 장비 획득 밸런스는 실제 재료 recipe를 핵심으로 한다. T4는 희귀재료 제작 중심이며 탐험 상자 완제품 보상을 핵심 획득축으로 삼지 않는다. 정확한 재료 수량은 후속 공급량/플레이테스트와 함께 조정.
- [REVIEW-COMPLETE] Paper Special Equipment vs Content Freeze v3 대조 완료. Freeze v3에는 개별 T2/T3/T4 weapon roster/능력 명세가 없고 `02_EQUIPMENT.md`는 Utility Equipment 문서이므로 충돌하는 개별 Special Equipment 설계 없음.
- [LOCKED] 상세 authoritative record: `docs/EQUIPMENT_PROGRESSION_FINALIZATION.md`.

---

# 3. Coating subsystem

## 이미 확정
- [LOCKED] Smithing Template 계열을 기능성 업그레이드 매체로 활용.
- [LOCKED] 핵심 공식 = Template + Mineral + Equipment Part → Functional Effect.
- [LOCKED] 서로 다른 coating은 중첩 가능.
- [LOCKED] 같은 mineral coating 반복은 일반적으로 불가.
- [LOCKED] Enhancement와 역할을 겹치지 않는다.
- [LOCKED] optional parallel progression으로 둔다.
- [LOCKED] Copper / Quartz / Amethyst / Lapis / Emerald / Gold / Diamond / Netherite / Iron / Redstone / Titanium / Tungsten / Mythril matrix 확정.
- [LOCKED] Iron Leggings = KB Resist +30% + ranged damage -15%.
- [LOCKED] Gold Boots = block slowing 무시, 용암 이동 관련 확정 효과 포함.
- [LOCKED] Aquamarine / Adamantium / Etherium coating 제외.

## 사용자 결정 필요
- [DECIDE] coating tier 자체를 둘지.
- [DECIDE] template 종류를 1종 공용으로 할지 mineral별/계층별로 나눌지.
- [DECIDE] template 획득처.
- [DECIDE] template 소모 여부.
- [BALANCE] 각 coating 확률/쿨다운/지속시간/수치.
- [BALANCE] durability cost가 필요한 효과와 필요 없는 효과.
- [DECIDE] 일부 효과의 정확한 Minecraft 판정 정의.
- [REVIEW] Content Freeze v3 matrix를 authoritative source로 유지.

---

# 4. Utility Equipment

## 채택 장비
- [LOCKED] 암벽 장갑.
- [LOCKED] 광부 랜턴.
- [LOCKED] 휴대용 포션 주입기.
- [LOCKED] 글라이더.
- [LOCKED] 잠수 산소통.
- [LOCKED] 탐험용 침낭.
- [LOCKED] 제트 부츠.
- [LOCKED] 조명탄.
- [LOCKED] 부력 캡슐 계열 장비.
- [LOCKED] 귀환 수정.
- [LOCKED] 휴대용 화로.
- [LOCKED] 채굴용 드릴.
- [OPTIONAL] Grappling Hook 보류.
- [LOCKED] Generic battery/fuel 체계는 만들지 않는다.

## 이미 확정된 주요 동작
- [LOCKED] Miner Lantern = Light 15, 손/헬멧 통합 가능.
- [LOCKED] Oxygen Tank = custom oxygen 우선 사용 후 vanilla air.
- [LOCKED] Sleeping Bag = 밤 건너뛰기, respawn point 변경 없음.
- [LOCKED] Jet Boots = 공중 점프 stack, ground/slime reset, 별도 연료 없음.
- [LOCKED] Flare = 충전 발사형 moving light projectile.
- [LOCKED] Return Crystal = 고가 1회용 channel, 이동/피격 취소.
- [LOCKED] Portable Furnace = food only, smoker 수준, on-demand fuel.
- [LOCKED] Drill = endgame, coal block fuel, Fortune/Silk/Unbreaking 가능, Efficiency 제외.

## 사용자 결정 필요
- [DECIDE] 암벽 장갑 durability.
- [DECIDE] Lantern 정확한 조합 recipe.
- [DECIDE] Potion Injector 저장 한도/세부 UI.
- [DECIDE] Glider 최종 중력/속도/조향 물리.
- [DECIDE] Oxygen Tank 용량.
- [DECIDE] Jet Boots 각 단계별 jump stack 및 recipe.
- [DECIDE] Flare 재료와 발광 지속시간.
- [DECIDE] 부력 장비 최종 이름.
- [DECIDE] 부력 장비 재료.
- [DECIDE] Return Crystal 목적지 규칙.
- [DECIDE] Return Crystal channel 시간 최종값.
- [DECIDE] Portable Furnace 연료 효율.
- [DECIDE] Drill 채굴 속도 threshold.
- [DECIDE] Drill 연료 소비량.
- [DECIDE] Drill 최대 연료 128 coal blocks 유지 여부.
- [BALANCE] 전체 recipe 수량 및 희귀도.

---

# 5. Fishing 축

## 이미 확정
- [LOCKED] vanilla cast/wait 흐름은 유지.
- [LOCKED] bite 이후 좌우 spectrum timing minigame.
- [LOCKED] 물고기는 entity spawn이 아니라 ItemStack.
- [LOCKED] species + 약 5개 grade + length.
- [LOCKED] 가치 개념은 species × grade × length 기반.
- [LOCKED] Reel = 접근/discovery pool.
- [LOCKED] Crankbait = 해당 pool 내 확률 조정.
- [LOCKED] Minnow = capture permission.
- [LOCKED] 각 모듈은 5단계.
- [LOCKED] 우클릭 casting.
- [LOCKED] 좌클릭 one-row module UI.
- [LOCKED] module은 rod 내부에 실제 ItemStack 형태로 저장.
- [LOCKED] UI open 동안 move/drop/dupe 방지 필요.

## 사용자 결정 필요
- [DECIDE] fish species 최종 roster.
- [DECIDE] 각 species의 biome/dimension/water 조건.
- [DECIDE] 5개 grade의 이름.
- [DECIDE] grade별 확률.
- [DECIDE] species별 length min/max.
- [DECIDE] length 분포 방식.
- [DECIDE] minigame 성공 구간 크기 계산.
- [DECIDE] minigame에서 grade/length가 난이도에 영향을 주는 방식.
- [DECIDE] 실패 시 결과.
- [DECIDE] Reel 1~5가 각각 여는 discovery pool.
- [DECIDE] Crankbait 1~5의 확률 변화.
- [DECIDE] Minnow 1~5가 각각 허용하는 대상.
- [DECIDE] 세 module의 durability.
- [DECIDE] 세 module의 recipe.
- [DECIDE] fish의 최종 보상/경제적 의미.
- [DECIDE] Fisherman/Villager exchange를 최종 채택할지.
- [DECIDE] Emerald 외 대체 보상이 필요한지.
- [BALANCE] 전체 드롭/보상 곡선.

---

# 6. Alchemy 축

## 이미 확정
- [LOCKED] 별도 거대 custom crafting GUI를 만들지 않는다.
- [LOCKED] Brewing Stand 중심으로 확장.
- [LOCKED] Potion / Effect / Catalyst 개념 유지.
- [LOCKED] Paper 구현은 reference로 활용.
- [LOCKED] 점착 젤 계열 아이디어는 Alchemy/Brewing 쪽으로 분류.

## 사용자 결정 필요
- [DECIDE] Mod에서 유지할 custom potion 최종 roster.
- [DECIDE] catalyst 최종 roster.
- [DECIDE] vanilla potion과 custom potion의 경계.
- [DECIDE] custom effect 최종 roster.
- [DECIDE] 각 potion/effect의 재료.
- [DECIDE] brewing recipe/formula.
- [DECIDE] catalyst가 Brewing Stand에 개입하는 방식.
- [DECIDE] vanilla Brewing UI를 그대로 쓸지 확장 UI를 추가할지.
- [BALANCE] 효과 지속시간/증폭/부작용/희귀도.
- [REVIEW] 최종 Paper alchemy/effect implementation과 실제 유지 대상 대조.

---

# 7. Mobs / Entities 축

## 현재 존재하는 reference 후보
- [REVIEW] Bomb Zombie.
- [REVIEW] Leap Zombie.
- [REVIEW] Mining Giant.
- [REVIEW] Piglin Spearman.
- [REVIEW] Rapid Shooter.
- [REVIEW] Swapping Witch.
- [REVIEW] Frost Blaze.
- [REVIEW] Riptide Drowned.
- [REVIEW] 기타 Paper MonsterBehaviorService 계열.
- [REVIEW] 과거 Exploration 전용 몹 중 독립적으로 살릴 가치가 있는 것.

## 사용자 결정 필요
- [DECIDE] 최종 mob roster.
- [DECIDE] 삭제할 old mob roster.
- [DECIDE] vanilla variant로 구현할 몹.
- [DECIDE] 완전 custom entity/model이 필요한 몹.
- [DECIDE] 각 mob의 biome/dimension.
- [DECIDE] spawn condition.
- [DECIDE] rarity.
- [DECIDE] group spawn 여부.
- [DECIDE] drop table.
- [DECIDE] Mining/material progression과 연결할 몹.
- [DECIDE] equipment/alchemy progression과 연결할 몹.
- [DECIDE] 각 AI의 최종 combat pattern.
- [BALANCE] HP/damage/speed/cooldown.

---

# 8. Boss / Major Combat 축

## 기존 reference
- [REVIEW] Thousand Eyes.
- [REVIEW] Gateway boss prototype.
- [REVIEW] Paper에서 구현된 boss/prototype logic 중 유지 가치가 있는 것.

## 사용자 결정 필요
- [DECIDE] 최종 boss roster.
- [DECIDE] prototype을 정식 boss로 승격할지.
- [DECIDE] 각 boss의 progression 위치.
- [DECIDE] summon / structure / natural encounter 여부.
- [DECIDE] boss 전용 drop/material.
- [DECIDE] Endgame 장비와의 연결.
- [DECIDE] arena 필요 여부.
- [DECIDE] phase 수와 핵심 gimmick.
- [BALANCE] 난이도.

---

# 9. World / Structures / Exploration 축

## 원칙
- [LOCKED] vanilla world/biome/structure를 삭제하지 않고 확장.
- [LOCKED] 거대한 별도 RPG world progression은 만들지 않는다.
- [LOCKED] 기존 Paper Exploration event engine은 그대로 이식하지 않는다.

## 기존 reference
- [REVIEW] Desert Pyramid.
- [REVIEW] Shipwreck.
- [REVIEW] Ocean Monument dormant logic.
- [REVIEW] Outpost / Swamp Hut 등 과거 structure 확장안.

## 사용자 결정 필요
- [DECIDE] 어떤 structure 확장을 최종 생존시킬지.
- [DECIDE] vanilla structure 내부 변주 vs 새로운 structure 추가 구분.
- [DECIDE] 구조물별 mob/loot/puzzle.
- [DECIDE] 광물/특수재료 획득과의 연결.
- [DECIDE] boss encounter와의 연결.
- [DECIDE] 완전 신규 biome가 필요한지.
- [DECIDE] dimension 추가 여부.
- [BALANCE] 발견 빈도/loot 가치.

---

# 10. 최종 콘텐츠 잠금 순서

권장 확정 순서:

1. Resource / Mining / Materials 마감
2. Equipment roster / Enhancement 잔여 결정
3. Coating 세부 수치 및 template economy 마감
4. Utility Equipment recipe + 수치 마감
5. Fishing 전체 설계 마감
6. Alchemy 전체 설계 마감
7. Mob roster / spawn / drop 마감
8. Boss roster / progression 마감
9. World / Structures / Exploration 마감
10. 전체 의존성 검사
11. Content Freeze 새 버전 생성
12. 그 후 Mod Stage에서 Fabric-native 구현

---

# 11. 현재 한 줄 상태

- Mining/Materials: 콘텐츠 구조와 상대 밸런스 기준은 임시 확정. recipe 소비량과 구현 후 실측 밸런스는 후속 단계.
- Equipment: 콘텐츠 설계 단계 마감. T2/T3/T4 baseline, Enhancement 구조, recipe 획득 원칙, Freeze v3 대조까지 확정. 정확한 Enhancement 비용·recipe 수량·Fabric 표현/전투 수치는 PROVISIONAL/IMPLEMENTATION으로 후속 조정.
- Coating: 설계 거의 LOCKED, template economy와 수치 미완.
- Utility Equipment: 기능 대부분 확정, recipe/수치 미완.
- Fishing: 핵심 시스템 확정, species/확률/보상/recipe 미완.
- Alchemy: 구조는 존재, 최종 콘텐츠 roster 미완.
- Mobs: 아이디어/reference 다수, 최종 roster와 spawn/drop 체계 미완.
- Boss: prototype/reference 존재, 정식 roster/progression 미완.
- World/Exploration: 과거 reference 존재, 무엇을 살릴지부터 재확정 필요.


---

# 13. Fishing current design overlay — 2026-09-26

> This section supersedes stale Fishing [DECIDE] entries above where the same topic is explicitly resolved here. Existing Stage 7 code remains implementation evidence, not authority over later user-approved design changes.

## Core fish model
- [LOCKED] Fishing remains item-based. No fish entity implementation is planned.
- [LOCKED] Final fish roster will contain roughly 10–12 species. Selection priority is: familiar/intuitive fishing-game species first, while preserving a meaningful real rarity/progression hierarchy. If 10–12 species cannot satisfy both conditions, familiarity is relaxed before rarity/progression integrity.
- [LOCKED] Every species definition will include English name, Korean display name, species/item identifier, min/max length, species rarity/weight, depth tendency, Reel requirement, Minnow requirement, biome/water conditions, and dimension conditions where relevant.
- [LOCKED] Biome/water ecology is part of the final design, but biome filtering is temporarily disabled during the current implementation/live-validation phase so valid custom fish may be tested broadly.
- [LOCKED] Per-fish persistent metrics are species, grade, length, and weight. Grade is displayed as e.g. `등급: III`. Length is shown in centimeters with decimals. Weight is an independent generated variable; no entity simulation is required.
- [DECIDE] Exact value/reward formula and the relative contribution/order of species rarity, grade, weight, and length remain to be balanced after the species roster and distributions are finalized.
- [DECIDE] Whether fish weight should require a separate tiered Fishing Rod/frame capacity is open. Candidate direction: Fishing Rod becomes the frame/carrier, modules mount into it, and rod/frame tier determines maximum supported fish weight.

## Module identity and progression
- [LOCKED] Reel, Crankbait, and Minnow each remain one equipment family with grades I–V.
- [LOCKED] Grades are not enchantment-style numeric levels. Higher-grade equipment is obtained as a crafted/smithed higher-grade result while remaining the same equipment family conceptually.
- [LOCKED] Higher Reel/Minnow grades include all lower-grade capabilities.
- [LOCKED] Reel = access possibility/discovery reach. Minnow = permission to capture an accessed species. Crankbait = manipulation of grade distribution inside the otherwise valid pool.
- [LOCKED] If Reel access is sufficient but Minnow permission is not, the fish cannot be captured even after the minigame; the player is told the required Minnow grade.
- [LOCKED] Crankbait reduces lower-grade probabilities and may, at sufficiently strong bias, eliminate the lowest grades from the active distribution. Exact weights are deferred.
- [DECIDE] Reel progression model remains open until the species roster is fixed: either a scalable distribution/range model that naturally accepts future species, or a curated hierarchy updated when new species are added.
- [PROVISIONAL] Upgrade recipe direction is either `current module + materials -> next grade` or a Smithing Template-style upgrade. Crafting Table vs Smithing Table is deferred. Exact recipes wait for a broader material/intermediate-component roster; rough recipes may use metal fragments/metal/string-like vanilla-native components.

## Durability and failure
- [LOCKED] Fishing Rod itself normally does not show a durability bar in ordinary inventory use. Module durability is shown in the module UI.
- [LOCKED] Modules do not disappear when exhausted. At durability 1 they are considered broken/inactive and completely lose their role until repaired.
- [LOCKED] Repair direction is Anvil-based; exact repair materials/costs are deferred.
- [LOCKED] Reel durability is consumed on cast/start of the fishing attempt. Crankbait and Minnow durability are consumed only at the actual successful permitted capture resolution.
- [LOCKED] On minigame failure the fish is lost; Reel wear has already occurred, while Minnow and Crankbait are not consumed.
- [LOCKED] Reel maximum durability should be structurally higher than Minnow/Crankbait because it is consumed on every cast. Exact I–V durability values are deferred.

## Minigame
- [LOCKED] The minigame uses four qualitative zones: green / yellow / orange / red.
- [LOCKED] Green = 100% capture. Yellow already has a substantially reduced capture chance; orange is reduced further; red = failure. Exact probabilities are deferred to UI/live balance.
- [LOCKED] The minigame modifies capture success only. It does not downgrade an already generated fish's grade, weight, or length.
- [LOCKED] Minigame difficulty is influenced only by fish species. Grade, weight, length, Reel, Minnow, and Crankbait do not alter minigame difficulty.
- [PROVISIONAL] Marker speed starts relatively slow and is tuned by live testing.
- [DECIDE] Exact zone widths are deferred until the HUD/UI art establishes the relative geometry.

## Controls and UI
- [LOCKED] Module UI input is LEFT CLICK. Stage 7's Shift+Right Click behavior is a design mismatch and must be corrected.
- [LOCKED] The one-row module UI retains dedicated Reel / Crankbait / Minnow slots and source-rod locking/anti-dupe behavior.
- [LOCKED] Broken state is primarily conveyed through durability; an optional exclamation mark above the affected equipment slot is allowed.
- [LOCKED] Module tooltip content should include description, grade, and relevant numeric stats.
- [DEFERRED] All visual assets and exact UI appearance are moved to the final Resource/UI pass after mechanics are locked: fish textures, module tier appearances, rod/frame appearance if adopted, fish container appearance, module UI and minigame HUD.

## Fish storage and fisherman trade
- [LOCKED] Add a portable fish-only container item (working-name family: `어망`; final display name TBD).
- [LOCKED] The container occupies one inventory slot and preserves its contents like a Shulker Box. Only fish may be stored.
- [LOCKED] Each fish occupies exactly one internal slot and fish do not stack, regardless of size. Species/grade/length/weight data remain per individual fish.
- [LOCKED] Fish item presentation places the fish name first, followed by grade, length, and weight.
- [LOCKED] Cooking remains available/open for fish.
- [LOCKED] Preserve normal Fisherman villager trading and add a fishing-trade area beside it rather than replacing vanilla trades.
- [LOCKED] The player can place the fish container into the fishing-trade UI; the system reads contained fish data, calculates the pre-defined value formula, and pays the resulting reward while transferring/removing sold fish data.
- [PROVISIONAL] Emerald is the current payout baseline because no better reward currency/item has been selected. Alternative rewards remain open.

## Next design order
1. Final species roster and ecology.
2. Grade I–V base distribution.
3. Length/weight generation model.
4. Value/reward formula.
5. Reel hierarchy and possible rod/frame weight-capacity decision.
6. Minnow hierarchy.
7. Crankbait grade-bias curves.
8. I–V durability values and repair costs.
9. Rough upgrade recipes / Crafting Table vs Smithing Table.
10. Minigame numeric probabilities and live tuning.
11. Fish-container capacity/name details.
12. Fisherman payout formula.
13. Final resource/UI asset pass.


# 14. Fishing design finalization reconciliation — 2026-09-26

> This section is authoritative over stale Fishing entries and the earlier section 13 overlay where they conflict. Resource art remains deferred until the code-side fishing design below is implemented and live-testable.

## Vocabulary and independent fish variables
- [LOCKED] Equipment uses Tier I–III only. Fish quality uses Grade I–V only. Equipment Tier and Fish Grade are separate concepts and must never be mapped to each other.
- [LOCKED] A fish instance persists four independent variables: Species / Grade / Length / Weight.
- [LOCKED] Grade does not determine Length or Weight. Length and Weight do not determine Grade.
- [LOCKED] Each species defines its own independent length distribution and its own independent weight distribution; there is no universal size curve shared by all species.
- [LOCKED] Fish remain ItemStacks; no fish entity implementation is planned.

## Species baseline roster
- [LOCKED] Current 12-species baseline: 붕어 (Crucian Carp, `crucian_carp`), 잉어 (Common Carp, `common_carp`), 메기 (Amur Catfish, `amur_catfish`), 무지개송어 (Rainbow Trout, `rainbow_trout`), 큰입배스 (Largemouth Bass, `largemouth_bass`), 파이크 (Northern Pike, `northern_pike`), 대서양연어 (Atlantic Salmon, `atlantic_salmon`), 황다랑어 (Yellowfin Tuna, `yellowfin_tuna`), 피라루쿠 (Arapaima, `arapaima`), 황새치 (Swordfish, `swordfish`), 블루마린 (Atlantic Blue Marlin, `blue_marlin`), 벨루가 철갑상어 (Beluga Sturgeon, `beluga_sturgeon`).
- [LOCKED] Species rarity/progression integrity takes priority over familiarity when those goals conflict.
- [BALANCE] Exact per-species min/max Length, min/max Weight, distribution curves, encounter weights and final ecology metadata are data/balance work. They must remain species-specific.

## Grade and Crankbait
- [LOCKED] Fish Grade remains I–V.
- [LOCKED] Crankbait affects Fish Grade distribution only. It never changes Species, Length, Weight, Reel/Minnow requirements or minigame difficulty.
- [LOCKED] No Crankbait baseline Grade I–V distribution = 45 / 30 / 16 / 7 / 2 percent.
- [LOCKED] Crankbait Tier I = 30 / 32 / 22 / 12 / 4 percent.
- [LOCKED] Crankbait Tier II = 10 / 25 / 32 / 24 / 9 percent.
- [LOCKED] Crankbait Tier III = 0 / 8 / 34 / 38 / 20 percent.
- [LOCKED] The same Crankbait Grade distribution applies regardless of species.

## Length and Weight
- [LOCKED] Length and Weight are rolled independently after Species is known and use that species' own distributions.
- [LOCKED] Length is displayed in centimeters with one decimal place; Weight is displayed in kilograms with one decimal place.
- [LOCKED] Extreme individuals become progressively rarer according to each species' distribution. Exact curve parameters are balance data, not a new gameplay system.
- [LOCKED] No separate Rod Frame / weight-capacity equipment. Fishing Rod remains a carrier for the three modules. Weight remains an individual fish/value variable and does not impose equipment capacity.

## Value model
- [LOCKED] Internal value baseline: `Species Base Value × Grade Multiplier × (1 + Length Bonus + Weight Bonus)`.
- [LOCKED] Grade multiplier baseline: I=1.00, II=1.15, III=1.40, IV=1.80, V=2.50.
- [LOCKED] Length Bonus and Weight Bonus use percentile within that species' own respective distribution, not raw cm/kg and not a universal normalized range.
- [LOCKED] Baseline bonus form: `LengthBonus = lengthPercentile × 0.75`; `WeightBonus = weightPercentile × 0.75`.
- [LOCKED] Length and Weight bonuses are additive inside Size Modifier rather than multiplied together, preventing extreme multiplicative price explosions.
- [LOCKED] Species Base Value is an internal value index, not an Emerald amount. Fisherman payout converts the final value index to Emerald only at the final economy boundary.
- [BALANCE] Current relative Species Base Value index baseline in roster order: 10 / 13 / 16 / 22 / 25 / 30 / 42 / 55 / 75 / 100 / 140 / 200.
- [BALANCE] Exact Value Index → Emerald conversion remains live-balance work.

## Equipment Tier I–III
- [LOCKED] Reel, Minnow and Crankbait each have exactly three equipment tiers: I / II / III.
- [LOCKED] Reel controls Species access/discovery only. Higher Reel includes lower access and never changes encounter weights, Fish Grade, Length, Weight or minigame difficulty.
- [LOCKED] Reel baseline progression groups the 12-species roster into three broad four-species access bands, subject only to final ecology/data tuning.
- [LOCKED] Minnow controls capture permission only. Species has a required Minnow Tier independent of Grade/Length/Weight. Higher Minnow includes lower requirements.
- [LOCKED] Reel and Minnow requirements do not have to be identical for a species; this preserves distinct roles.
- [LOCKED] Insufficient Minnow does not remove the species from the encounter pool. The fish may bite and the minigame may run; after a successful minigame the capture fails with a message stating the required Minnow Tier.
- [LOCKED] Reel consumes durability when a valid custom fishing attempt/cast starts. Minnow and Crankbait consume durability only when a permitted capture succeeds.
- [LOCKED] Failed minigame or insufficient Minnow consumes no Minnow/Crankbait durability.
- [LOCKED] Module durability reaching 1 means broken/inactive; the module persists rather than disappearing and can be repaired through the Anvil.
- [BALANCE] Exact Tier I–III module durability and Smithing/crafting materials remain implementation/live-balance work.

## Minigame
- [LOCKED] Four timing zones: light green / yellow / orange / red.
- [LOCKED] Light green = 100% capture; yellow already has a sharply reduced probability; orange is substantially lower; red = 0%.
- [PROVISIONAL] Live-test starting probabilities: green 100%, yellow 45%, orange 15%, red 0%. Tune after HUD/live testing.
- [LOCKED] Minigame determines capture/escape only and never modifies Grade/Length/Weight.
- [LOCKED] Only Species affects minigame difficulty. Equipment Tier, Grade, Length and Weight do not.
- [LOCKED] Species difficulty baseline uses levels 1–5 and changes deterministic ping-pong marker speed only; zone geometry/rules do not become species-specific.
- [PROVISIONAL] One-way marker travel starting points for difficulty 1–5: 1.8s / 1.5s / 1.25s / 1.0s / 0.8s. Live tuning may change these.
- [LOCKED] Fish Grade, Length and Weight are generated only after minigame success and Minnow validation. Server flow: ecology/custom-fishing eligibility → Reel-filtered Species roll → minigame → Minnow validation → Grade roll → Length roll → Weight roll → fish item.

## Rod/module UI
- [LOCKED] Right click preserves casting/retrieval. Left-clicking air while holding the Fishing Rod opens the equipment UI; entity attacks/block interactions must not be swallowed.
- [LOCKED] One-row UI contains Reel / Crankbait / Minnow slots only and validates module type.
- [LOCKED] Each Fishing Rod stores its own three module ItemStacks and retains source-rod lock / anti-dupe behavior.
- [LOCKED] Fishing Rod itself has no Tier, Grade, Enhancement, weight capacity or separate progression.
- [LOCKED] Broken Reel prevents custom fishing; broken Minnow behaves as no active Minnow permission; broken Crankbait falls back to the no-Crankbait Grade distribution.
- [LOCKED] Module durability is displayed in the module UI/tooltips; optional broken-slot `!` indicator remains allowed.

## Fish container / 어망
- [LOCKED] Working/final baseline name is 어망: a portable fish-only container item. Fish do not stack and each fish consumes one internal slot.
- [LOCKED] 어망 itself has Tier I–III storage progression: Tier I=9 slots, Tier II=18 slots, Tier III=27 slots.
- [LOCKED] Upgrading an 어망 preserves its contained fish and all per-fish persistent data.
- [LOCKED] Fish-only validation inherently prevents nesting another 어망 or arbitrary items.
- [LOCKED] Holding the 어망 and right-clicking opens its GUI. It is not required to be a placeable block.

## Fisherman sale path
- [LOCKED] Vanilla Fisherman villager trades remain intact. A separate fish-trade panel/entry is added.
- [LOCKED] Fish can be sold individually or in bulk from an 어망; the UI previews fish count/total internal Value/expected payout before confirmation.
- [LOCKED] Sale confirmation removes/transfers the sold fish and pays the computed reward.
- [PROVISIONAL] Emerald remains the baseline payout currency. Exact conversion is live-balance work.

## Ecology / custom-fishing activation
- [LOCKED] Do not implement per-species hard depth bands or continuous depth curves.
- [LOCKED] Custom fishing itself requires a sufficiently large body of water, using approximately a 2×2-chunk (about 32×32 block) local-water scale and about 8 blocks/meters of depth as the baseline activation requirement.
- [IMPLEMENTATION] The 2×2-chunk rule must be tolerant of natural shorelines/terrain rather than requiring every block in a literal 32×32 prism to be water. Exact connected-water/coverage test is implementation/live-validation work.
- [LOCKED] Once custom fishing is eligible, Species ecology is primarily biome/water-environment based; small local depth differences do not gate individual species.
- [LOCKED] Ecology/environment filtering → Reel access filtering → Species encounter-weight selection.
- [IMPLEMENTATION] Ecology metadata should be defined now but restrictions may be temporarily bypassed during broad live validation.

## Resource Stage D order
- [LOCKED] Code implementation comes before the remaining Fishing resource-art pass because the design now introduces substantial runtime/data/UI changes.
- [IMPLEMENTATION] First reconcile Stage 7 code with this section: Tier I–III modules, four independent fish fields, species-specific Length/Weight distributions, value model, minigame rules, left-click UI, broken-module behavior, 어망 I–III, Fisherman sale path, ecology/custom-fishing eligibility and updated server resolution order.
- [DEFERRED] After code-side behavior is live-testable, create/integrate final fish/module/어망/UI/HUD assets and perform the Resource Stage D live-validation pass.

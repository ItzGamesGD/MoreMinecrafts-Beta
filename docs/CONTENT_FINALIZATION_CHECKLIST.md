# MoreMinecrafts 콘텐츠 최종 확정 체크리스트

> 목적: 이 문서는 Mod 이식 단계와 별개다.
> 현재 작업은 최종 Paper 플러그인과 기존 설계 위에 새 콘텐츠 결정을 덧씌워, MoreMinecrafts의 최종 게임 디자인을 잠그는 단계다.
> 구현 방식은 나중의 Fabric Mod Stage에서 다시 설계하되, 여기서는 "무엇을 만들 것인가"를 먼저 확정한다.

## 상태 표기
- [LOCKED] 설계 확정. 구현 시 임의 변경 금지.
- [DECIDE] 사용자 최종 결정 필요.
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
- [LOCKED] Adamantium / Etherium의 survival 획득 여부는 별도 결정.

## 사용자 결정 필요
- [DECIDE] 최종 추가 광석 목록 확정.
- [DECIDE] 각 광석의 차원: Overworld / Nether / End / 기타.
- [DECIDE] 각 광석의 생성 높이와 분포 방식.
- [DECIDE] 각 광석의 희귀도.
- [DECIDE] 광맥 크기 및 노출 빈도.
- [DECIDE] Silk Touch / Fortune 적용 여부와 방식.
- [DECIDE] 각 재료의 가공 단계: ore → raw → ingot/material 중 어디까지 둘지.
- [DECIDE] Mythril의 실제 월드 생성 여부.
- [DECIDE] Adamantium의 survival 획득 여부.
- [DECIDE] fantasy mineral을 월드젠으로 얻을지 boss/structure loot로 얻을지.
- [BALANCE] 재료별 장비 recipe 수량.
- [BALANCE] 희귀 재료의 실제 체감 공급량.

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
- [DECIDE] T2 Elemental 장비 최종 전체 roster.
- [DECIDE] 각 원소 장비의 기본 아이템 타입.
- [DECIDE] T4 Endgame 장비의 존재 방식과 개수.
- [DECIDE] 특수장비 중 Paper에서 그대로 유지할 것 / Mod에서 재해석할 것.
- [BALANCE] Enhancement 최대 수치.
- [BALANCE] Enhancement 비용.
- [BALANCE] Enhancement 실패/보정 규칙을 최종적으로 유지할지 재설계할지.
- [BALANCE] 장비 recipe 비용.
- [REVIEW] 최종 Paper special equipment roster와 Content Freeze v3 대조.

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

- Mining/Materials: 철학과 핵심 재료는 거의 확정, 월드젠/최종 수치 미완.
- Equipment: 구조 확정, T2/T4 roster와 일부 balance 미완.
- Coating: 설계 거의 LOCKED, template economy와 수치 미완.
- Utility Equipment: 기능 대부분 확정, recipe/수치 미완.
- Fishing: 핵심 시스템 확정, species/확률/보상/recipe 미완.
- Alchemy: 구조는 존재, 최종 콘텐츠 roster 미완.
- Mobs: 아이디어/reference 다수, 최종 roster와 spawn/drop 체계 미완.
- Boss: prototype/reference 존재, 정식 roster/progression 미완.
- World/Exploration: 과거 reference 존재, 무엇을 살릴지부터 재확정 필요.

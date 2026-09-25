# MoreMinecrafts 콘텐츠 최종 확정 체크리스트

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
- [LOCKED] T2 Elemental roster: 불꽃검 / 흐르는 물의 검 / 바람을 가르는 검 / 바람궁수의 활 / 대지의 철퇴 / 번개의 검 / 마기가 깃든 지팡이. Ice T2 장비는 없음.
- [LOCKED] T2 기본 타입: Fire=Sword, Water=Sword, Wind=Sword+Bow, Earth=Mace, Lightning=Sword, Ice=없음, Dark Energy=Staff.
- [PROVISIONAL] T2 각 무기는 고유 액티브 스킬 1개 + 패시브/기타 고유 능력 구조를 목표로 한다.
- [PROVISIONAL] 모든 T2 원소검 기본 공격에는 해당 원소를 식별하는 전용 VFX를 적용한다. 연출 규모는 기본 공격 < 패시브/누적 발동 < 액티브 순으로 둔다.
- [PROVISIONAL] 불꽃검: 기본 화염 VFX, 10회 공격 적중마다 가로 화염 휩쓸기 범위공격, 우클릭 30초 쿨다운의 강력한 사선 화염 베기.
- [PROVISIONAL] 흐르는 물의 검: 빠른 공격속도 + 처치 시 소량 회복, 우클릭 시 상승 후 시선 방향으로 내려찍고 약 2블록 앞에 강력한 물기둥 소환.
- [PROVISIONAL] 바람을 가르는 검: 빠른 공격속도 + 기본 바람 VFX, 10회 공격 적중마다 넓은 바람 범위공격과 범위 내 적별 Sweep Attack 연출. 최종 액티브 1개는 미확정.
- [PROVISIONAL] 번개의 검: 기본 전기 VFX, 연속 타격 전하 누적 후 추가 번개 피해, 액티브 뇌격참(짧은 전진/돌진성 베기 + 순간 번개 burst). 세부 수치/쿨다운은 미확정.
- [NEEDS_DESIGN_DECISION] 바람궁수의 활과 대지의 철퇴는 Paper identity를 기준으로 패시브/액티브 구조를 추가 확정해야 한다.
- [DEFERRED] 마기가 깃든 지팡이 설계는 보류.
- [IMPLEMENTATION] T2는 Paper 능력의 핵심 컨셉을 계승하되, 모델링/VFX/SFX/정확한 능력치/쿨다운/범위/키 입력/세부 판정은 Fabric 구현 단계에서 재설계.
- [LOCKED] T4 Endgame baseline = 무극검 / 천궁 / 천서 3종. 세계수 활은 baseline 밖의 OPTIONAL 후보이며 필요 시 T3로 재분류 가능.
- [LOCKED] T1/T2/T3/T4는 서로 독립적인 설계 카테고리이며 진화/승급 트리가 아니다. 특정 recipe/해금 조건에서 다른 Tier 장비를 요구할 수는 있으나 장비 상속은 아니다.
- [LOCKED] T3 Special 7종 전부 유지: 타나토스의 철퇴 / 포세이돈의 창 / 화염 도끼 / 뇌신의 도끼 / 성검 · 솔라리스 / 월야잔광 / 빙결검. 핵심 정체성은 유지하고 Fabric 표현·구현은 재해석.
- [LOCKED] Enhancement 2.0: T1/T2 ordinary 상한 +20, +5/+10/+15/+20 결정론적 milestone. T3/T4 일반 강화 불가.
- [LOCKED] 성공률 100%; 실패/하락/파괴/pity/reroll/Promotion/Coin 없음. milestone은 단일 enhancement level에서 파생.
- [PROVISIONAL] 곡선, milestone bonus, 재료/XP 비용, 도구 utility, ranged charge speed, Mace smash 공식, armor 산술(풀세트 약 8% 이하)은 조정 가능. Anvil은 현재 임시 adapter이다.
- [DEFERRED] T3 별도 progression과 T4 kill-point Grade/Mastery 구현 및 monster scaling 결정.
- [PROVISIONAL] 장비 획득 밸런스는 실제 재료 recipe를 핵심으로 한다. T4는 희귀재료 제작 중심이며 탐험 상자 완제품 보상을 핵심 획득축으로 삼지 않는다. 정확한 재료 수량은 후속 공급량/플레이테스트와 함께 조정.
- [REVIEW-COMPLETE] Paper Special Equipment vs Content Freeze v3 대조 완료. Freeze v3에는 개별 T2/T3/T4 weapon roster/능력 명세가 없고 `02_EQUIPMENT.md`는 Utility Equipment 문서이므로 충돌하는 개별 Special Equipment 설계 없음.
- [LOCKED] 상세 authoritative record: `docs/EQUIPMENT_PROGRESSION_FINALIZATION.md`.

---

# 3. Stage 10 current status overlay

> This checklist began as a pre-implementation decision board. Stage 10's item-level statuses, unresolved evidence and source precedence are in [STAGE_10_DRAFT_CONTENT_AUDIT.md](STAGE_10_DRAFT_CONTENT_AUDIT.md). Older entries in sections 1-2 remain historical design records; current runtime status comes from Fabric. Stage 10 is Draft Content Completion / Design Audit, **not** World / Structure / Exploration implementation.

## Coating
- [LOCKED_NOT_IMPLEMENTED] Armor only. Functional progression distinct from Enhancement/Enchantment. Dedicated coating items and recipes in a smithing-template-style native flow; existing project materials; one shared placeholder visual pattern. No weapons/tools design.
- [LOCKED] One armor ItemStack: 0 → 1 → 2 → 3 coatings, then locked. Current removal/replacement is unavailable.
- [DEFERRED] Future replacement via separately specified removal item/mechanic.
- [NEEDS_DESIGN_DECISION] Full effect matrix must be recovered from authoritative Freeze v3 before an individual slice. Only two partial effects are quoted in the Stage 10 audit; do not invent the rest. Template acquisition/consumption, exact effects and balance unresolved.

## Utility Equipment
- [LOCKED_NOT_IMPLEMENTED] Retained independent roster: Miner Lantern, Sleeping Bag, Oxygen Tank, Jet Boots, Flare, Return Crystal, Portable Furnace, Mining Drill, Glider, Grappling Hook. None implies standalone Exploration progression.
- [NEEDS_DESIGN_DECISION] Item-specific mechanics, recipes and balance are tabulated in the Stage 10 audit. Grappling Hook is retained but deferred pending mechanics.
- [STALE_DOC] Older additional Utility entries and the previous optional treatment of Grappling Hook are not the current ten-item baseline.

## Fishing
- [IMPLEMENTED] Stage 7 fishing vertical slice exists; [LIVE PENDING] client/server gameplay checks remain in LIVE_VALIDATION_BACKLOG.md. Source and STAGE_7_FISHING.md define implemented species, grades, module behavior, recipes and timing. Earlier [DECIDE] markers for these implemented details were stale.
- [NEEDS_DESIGN_DECISION] Fisherman exchange/final reward economy remains open. Earlier left-click UI wording differs from Stage 7's shift-right-click implementation; changing the intended control requires an explicit decision.

## Alchemy
- [RECOVERED_PAPER_DESIGN] Eight enabled production potion/effect identities and ten enabled catalysts remain in final Paper backend. Historical effect handlers and catalyst functions are documented in the Stage 10 audit; their Paper implementation is **not** Fabric implementation.
- [RECOVERED_HISTORICAL_RECIPE] Eight older simultaneous multi-material ingredient lists were found before Farming/Cooking removal. Their Farming-derived ingredients and custom crafting frontend are historical references, not current prerequisites.
- [RECOVERED_FINAL_BREWING_MAPPING] None found: the September 21 Brewing Stand migration replaced recipe v1 with an empty transition table without retiring the potion/effect identities.
- [NEW_STAGE_10_BREWING_MAPPING] Eight one-ingredient Thick Potion transitions are specified and individually labeled in the audit. This is the only newly decided gameplay design in this Work pass, under the explicit recipe-mapping exception.
- [LOCKED_NOT_IMPLEMENTED / READY_FOR_VERTICAL_SLICE] The bounded Shock base potion (Thick Potion + Copper Ingot) has a recovered effect contract and Stage 10 mapping. No Fabric Alchemy implementation exists; no catalysts belong to that first slice.
- [NEEDS_DESIGN_DECISION] Final balance, other individual slice boundaries and catalyst compatibility remain separate. REDSTONE's Paper duration multiplier 100% is a provisional no-op, not a finalized duration increase. Retired Farming and custom crafting are not revived. See Stage 10 audit.

## Mobs / Bosses
- [IMPLEMENTED] Bomb Zombie, Leap Zombie, Mining Giant (Stage 8) and Skeleton King (Stage 9). [LIVE PENDING] human checks are in the validation backlog.
- [NEEDS_DESIGN_DECISION] Other Paper mob/boss candidates and survival progression remain individually undecided. Do not treat old global [DECIDE] as negating implemented content.

## Retired scope
- [RETIRED] Standalone Exploration progression, Paper Exploration event engine, mandatory structures, dungeon progression, exploration-specific economy, Coin economy, job levels, Promotion, Fragment progression, giant custom crafting GUI and generic RPG world progression.
- [STALE_DOC] The former World / Structures / Exploration stage and the old linear content-lock order are superseded. Use vertical slices after user review of an item-specific Work specification. This checklist does not authorize feature implementation.

## Implementation status reminder
- [IMPLEMENTED] Stage 6 material crafting/ore worldgen; Stage 7 fishing; Stage 8 three zombie variants; Stage 9 Skeleton King; Flame Sword, Wind Sword and Enhancement foundation.
- [LIVE PENDING] is validation work, never shorthand for missing implementation.

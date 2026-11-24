# 인게임 에셋·UI 전수 감사 — 2026-11-24

- **성격:** 읽기 중심 감사 보고서. 에셋, 코드, 설계 권한을 바꾸지 않는다. 이 문서는 **설계 권한이 아니며** 승인된 디자인·아트 변경을 확정하지 않는다. 모든 "개선안"은 제안이고, 승인된 항목에 대한 제안은 오너 결정이 필요하다.
- **기준:** `checkpoint/grand-unified-20261123` @ `fe9d754` (Merge PR #320).
- **권한 경로:** `AGENTS.md` → `docs/CONTENT_AUTHORITY_INDEX.md`, `docs/STRUCTURE_AUTHORITY_INDEX.md` → 해당 항목의 설계 문서(아래 각 표에 인용).
- **부록:** [`ASSET_AND_UI_AUDIT_2026-11-24_inventory.tsv`](ASSET_AND_UI_AUDIT_2026-11-24_inventory.tsv) — 아이템 141, 블록 아이템 포함, 엔티티 65, 미참조 텍스처 3행. 열: 카테고리, ID, 종류, 모델/렌더러, 텍스처, 크기, 상태, en_us, ko_kr.

## 0. 방법과 근거

| 단계 | 방법 | 근거 |
|---|---|---|
| 등록 인벤토리 | `registry/ModItems·ModBlocks·ModBlockEntities·ModMenuTypes`, `AccessoryDefinition`, `FishSpecies`, `WeaponBalance`, `CustomMobCatalog`, `PyramidMobs`, `SpiderColonyEntities`, `SkyNormalMobs`, `SkyVariantMobs`, `TombGuardianEntities`, `SecretEntranceEntities`, `GladiatorEntities` 정적 판독 | 소스 |
| 자산 대조 | `items/*.json` → `models/` → `textures/` 해석, PNG 크기와 MD5 중복, `PLACEHOLDER_T2_TEXTURES.txt` 목록, `lang/en_us·ko_kr` 키 대조(스크립트) | 부록 TSV |
| 엔티티 렌더 | `src/client` 렌더러 등록과 텍스처 경로 대조 | `MoreMinecraftsClient.java:184-283` 외 |
| 화면 | 화면 코드 판독과 최근 CI 아티팩트 스크린샷 판독 | 아래 아티팩트 표 |

**사용한 CI 아티팩트** (모두 `checkpoint/grand-unified-20261123`, 내려받아 직접 열어 봄):

| 아티팩트 | ID | 내용 |
|---|---|---|
| `minecraft-client-visual-validation-45182156196` | 14822383100 | 진행 화면 4세트(GUI 2/3 × en_us/ko_kr, 1280×720), 세트당 70장 |
| `accessory-ui-evidence-45359745542` | 14905245156 | `ui-contact-sheet.png`, `layout-verification-report.txt` |
| `accessory-integration-contact-sheet-45359745542` | 14903903891 | 액세서리·모루·인챈트 통합 시트, `anvil-functional-assertions.txt`, `enchanting-gui-scale-diagnostic.txt` |
| `accessory-visual-evidence-45359745542` | 14904193340 | 액세서리 개별 캡처(시트로 대신 확인) |
| `piglin-trade-client-validation-45359746246` | 14904510899 | `piglin-trade.png`, 로그 |
| `sky-bbmodel-captures` | 14903444127 | 크리스탈 벌레·스카이가드 궁수·폴른 센티넬 4방향 |
| `st03-natural-client-captures` | 14886061812 | 로그 확인용으로만 사용(ST-03는 다른 세션 범위, 화면 평가 안 함) |

워크플로를 새로 실행하지 않았다. 기존 아티팩트로 충분했다.

### 심각도와 크기 표기

- 심각도: **깨짐**(누락 모델, 화면 밖으로 나가는 UI, 원시 키 노출 등 명백한 오류) / **결함**(눈에 띄는 결함: 임시·재사용 자산이 플레이어에게 보이거나 상태 표시가 틀림) / **권장**(개선 권장)
- 크기: **소**(JSON/lang/수 줄), **중**(화면 하나의 레이아웃 수정, 텍스처 몇 장, Blockbench 단순 모델), **대**(새 3D 모델과 애니메이션, 여러 화면 재설계)
- 상태: **승인**(권한 문서가 승인한 자산) / **임시**(코드·문서가 임시로 표기) / **미정**(승인 기록을 찾지 못함)

---

## 1. 상위 발견 (효과 큰 순)

| # | ID | 발견 | 심각도 | 크기 | 근거 |
|---|---|---|---|---|---|
| 1 | A-01 | 유물 코어 5종(`*_relic_core`)과 `relic_catalyst`에 `items/*.json`이 없다. 26.x에서는 누락 모델(마젠타/검정) 아이콘으로 나온다. 코어는 실제 드롭되고(`ArtifactCoreDrops`), 촉매는 모루 슬롯에 들어간다. | 깨짐 | 소 | 부록 TSV `NO_ITEM_DEF`. `models/item/*_relic_core.json`은 있는데 연결이 안 됨. `relic_catalyst`는 모델도 없음. 인게임 화면으로는 확인하지 못함(정적 판정). |
| 2 | C-01 (정정 §8 참조) | 인챈트 진행 화면(330×360)이 GUI 4(1920×1080 → 480×270)에서 화면을 벗어난다. 탭 2개가 y=-37에 그려지고 슬롯 28개가 화면 밖이다. GUI 3에서는 패널이 y=0에 붙어 여백이 없다. | 깨짐 | 중 | `enchanting-gui-scale-diagnostic.txt`(`panelFits=false`, `widgetsOffScreen=[Enchantment@83,-37, Book Option@243,-37]`), 시트 `enchant-scale-3-tree`, `enchant-scale-4-tree`, `enchant-scale-4-book-options` |
| 3 | A-03 | T2 원소 무기 5종(물의 검, 바람의 검, 바람 활, 대지의 철퇴, 번개의 검)과 암흑 에너지 지팡이가 모두 바닐라 `diamond_sword` 텍스처를 쓴다. 활은 당김 단계가 없고, 철퇴도 검으로 보인다. | 결함 | 중(무기당) | `models/item/{wind_sword,…}.json` `layer0: minecraft:item/diamond_sword`. 권한: `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §1 "무기는 … 고유 텍스처/모델을 갖는다". 지팡이는 권한상 deferred/placeholder. |
| 4 | A-07 | ko_kr 키 누락 71개(아이템·블록 이름 65, 컨테이너·UI 6). 한국어 클라이언트에서 영어로 나온다. 대상: 광물 전체, 강화석, 바람의 검·T2 무기, 유틸 장비, 단검·대검 14종, 백팩 제목 등. `auto_farm` 블록 이름은 en/ko 모두 없어 원시 키가 나온다. | 결함(auto_farm은 깨짐) | 소~중 | 부록 TSV `NO_LANG_*`. 정적 키 검사: 코드의 리터럴 키 중 en 누락 0, ko 누락 6(`container.moreminecrafts.{backpack,fish_trap,mining_drill,portable_furnace,potion_injector}`, `item.moreminecrafts.enhancement_level`). 한국어 명칭이 권한 문서에 없는 항목은 명칭 결정이 필요하다. |
| 5 | C-04 (정정 §8 참조) | 연구 툴팁에 내부 식별자와 미완성 문구가 그대로 나온다. 예: "Prerequisite research: Production Engineering, V3 Research 08", "Tier: Unspecified"/"단계: 분류 미확정", "Next level effect description". 비용, 재료, 구매 상태 줄에 충족/부족 색이 없다. | 결함 | 소~중 | `gui-scale-3-en_us/research-node-tooltip.png`, `gui-scale-3-ko_kr/research-unavailable-tooltip.png`, `ProgressionTreeCanvas.java:1036-1090`(비용·재료·상태 줄에 스타일 없음). 권한: 2026-10-12 정정은 "localized item-rich tooltips"를 요구함 |
| 6 | C-06 | T1 모루 강화 모드에서 재료가 부족한 상태가 충분한 상태와 똑같아 보인다. 비용 줄 색이 `0xFF303030` 하나로 고정이고, 비활성 버튼도 겉모습 차이가 거의 없다. 바닐라 모루는 비용이 모자라면 빨간색으로 표시한다. | 결함 | 소 | 시트 `anvil-scale-3-accessory-insufficient`와 `…-4-to-5-before` 비교, `T1AnvilScreen.java:173-193`. 기능은 정상(`ANVIL_INSUFFICIENT_REJECTED`). |
| 7 | A-04 | 임시 텍스처 12장(T2 재료 11 + 깃털 부적)이 마젠타 체커 표식이 있는 공용 보석 모양이다. 대문자 파일 `textures/item/PLACEHOLDER_T2_TEXTURES.txt` 때문에 실행할 때마다 `ERROR Invalid path in mod resource-pack` 로그가 나온다. | 결함 | 소(로그) / 중(아트) | 모든 클라이언트 로그, 예: `sky-bbmodel/client.log` `[Worker-Main-3/ERROR] (ModNioPackResources) Invalid path … PLACEHOLDER_T2_TEXTURES.txt`. 메모 파일을 `assets/` 밖으로 옮기면 로그는 사라진다. 아트는 오너 결정이 필요하다. |
| 8 | A-06 | 단검·대검 14종이 모든 티어에서 바닐라 `iron_sword` 모델을 쓴다. 인벤토리에서 티어와 종류(단검/대검/바닐라 철 검)를 구분할 수 없다. | 결함 | 중 | `items/*_dagger.json`, `items/*_greatsword.json`. 권한(`EARLY_T1_T2…CHECKPOINT`)은 "vanilla-style" 확장이라고만 하며 아이콘은 정하지 않음 → 미정 |
| 9 | C-02/C-03 | 낚싯대 화면(330×270)과 계약 화면(260×240)은 GUI 높이가 270/240 미만이면 넘친다(1280×720에서 GUI 3, 1920×1080에서 GUI 4는 여백 0). 계약 목표 줄은 줄바꿈이나 말줄임이 없다. | 결함(추정) | 중 | `FishingRodScreen.java:25`, `ContractScreen.java:27-28,98-126`. **스크린샷 없음, 실행 확인 안 함** |
| 10 | B-01 | 3D 리소스가 필요한데 아직 없는 승인 항목: 그룹 A 비행 몹 4종(특히 천공의 비룡 전용 애니메이션, 부유 해파리), 불 보스·바다 보스(3D 파츠), 폴른 센티넬과 스카이가드 궁수의 무기. | 결함(콘텐츠 공백) | 대 | §3 표 |

---

## 2. 등록 콘텐츠 인벤토리 요약

전체 행은 부록 TSV에 있다.

| 범주 | 수 | 자산 상태 요약 |
|---|---|---|
| 아이템(블록 아이템 제외) | 125 | 전용 텍스처 + 생성 모델 44, 3D 요소 모델 23(낚시·물고기·불꽃검), 바닐라 모델 그대로 32, 바닐라 텍스처 재지정 16(T2 무기 6, T1 액세서리 7, 구형 장비 4·5티어 일부, `colony_crystal`), `items` 정의 없음 7 |
| 블록(+아이템) | 16 | 광물·블록 11은 승인 텍스처(`APPROVED_MATERIAL_TEXTURES.md`). 침낭·통발·자동 농장·거미 알주머니·매달린 고치 5종은 바닐라 텍스처나 모델 재사용 |
| 블록 엔티티 | 2 | `fish_trap`, `auto_farm` — 둘 다 바닐라 큐브 텍스처 |
| 메뉴 타입 | 3 | `fishing_rod`, `enchanting_progression`, `auto_farm` |
| 엔티티 | 65 | 일반 커스텀 몹 44(바닐라 모델 + 전용 텍스처, 설계상 의도), 피라미드 4, 거미 군락 2, 천계 일반 몹 5(전용 모델), 하늘섬 변종 5(텍스처만), 왕릉 수호자 + 기술 엔티티 2, 금 안내 골렘, 불멸의 검투사 |
| 커스텀 화면 | 11 | 진행 선택(4카드), 7섹터 선택, 연구·지식·XP 퀘스트 트리(`ProgressionBrowseScreen`/`ProgressionTreeCanvas`), 구조 미리보기, 인챈트 진행, T1 모루(강화/모루/책 합치기), 액세서리 인벤토리(믹스인), 계약, 낚싯대, 자동 농장 |
| 바닐라 화면 재사용 | 5+ | 백팩(ChestMenu 9×1/9×3/9×6), 휴대 화로, 채굴 드릴, 포션 주입기, 통발, 피글린 교역(상인 화면) |

등록 코드와 자산의 불일치:
- 등록되었으나 `items` 정의 없음: `earthen/abyssal/glacial/tidal/blazing_relic_core`, `relic_catalyst`, `foundation_probe`(디버그, 크리에이티브 탭 아님).
- 자산은 있으나 미참조: `textures/entity/crystal_bug.png`, `fallen_sentinel.png`, `skyguard_archer.png`(Blockbench 교체 뒤 `bb_*`로 대체됨). `bbmodel/fallen_sentinel_weapon.json`, `bbmodel/skyguard_archer_weapon.json`은 코드 참조가 없다. 삭제는 제안하지 않으며 정리 여부는 오너가 정한다.
- 완전히 같은 PNG(MD5)는 없다. 대신 **같은 바닐라 모델/텍스처를 가리키는 재사용**은 많다(§3.2).

---

## 3. 리소스 부족·임시 자산

### 3.1 누락과 오류

| ID | 대상 | 증상 | 상태 | 심각도 | 크기 | 제안 |
|---|---|---|---|---|---|---|
| A-01 | 유물 코어 5종 | `items/` 정의 없음 → 누락 모델. `models/item/*_relic_core.json`(echo_shard 재지정)은 있음 | 임시 | 깨짐 | 소 | `items/<id>.json` 추가. 전용 아트는 별도 결정 |
| A-01 | `relic_catalyst` | `items/`와 모델 모두 없음. 이름도 "(provisional)" | 임시. 아트는 권한상 미정(`accessory_sync_2026-11-13/…유물촉매…md` "아트/텍스처 미정") | 깨짐 | 소 | 임시 모델이라도 연결 |
| A-02 | `auto_farm` 블록 | `block.moreminecrafts.auto_farm` 키가 en/ko 모두 없음 | — | 깨짐 | 소 | 키 추가(`container.moreminecrafts.auto_farm` "Auto Farm"과 맞춤) |
| A-04 | `PLACEHOLDER_T2_TEXTURES.txt` | 리소스팩 경로 규칙(소문자) 위반으로 매 실행 ERROR 로그 | — | 결함 | 소 | 메모를 `docs/`로 옮기거나 소문자 이름으로 바꿈 |
| A-07 | ko_kr 71키 | 한국어 클라이언트에서 영어 표시 | — | 결함 | 소~중 | 권한 문서에 한국어 명칭이 있으면 그대로 쓰고, 없으면 명칭 결정을 요청 |

### 3.2 임시·재사용 자산

| ID | 대상 | 현재 표시 | 상태 | 심각도 | 크기 |
|---|---|---|---|---|---|
| A-03 | 물의 검, 바람의 검, 바람 활, 대지의 철퇴, 번개의 검 | 바닐라 다이아몬드 검 텍스처(handheld) | 임시. 권한은 고유 텍스처/모델 요구(SKY §1) | 결함 | 중×5 |
| A-03 | 암흑 에너지 지팡이 | 다이아몬드 검 | 권한상 deferred/placeholder | 권장 | 중 |
| A-04 | `skeleton_king_chain`, `gladiator_blood_crest`, `ancient_clock_spring`, `ancient_world_tree_sap`, `lava_heartstone`, `permafrost_crystal`, `abyssal_pearl_core`, `mermaid_scale`, `void_star_shard`, `void_gravity_core`, `shimmering_soul_glass`, `feather_charm` | 마젠타 체커 표식이 있는 공용 보석 32×32 | 임시(`PLACEHOLDER_T2_TEXTURES.txt`, `ModItems.java` 주석) | 결함 | 중(12장) |
| A-05 | T1 액세서리 7종 `t1_combo`, `t1_crisis_escape`, `t1_heavy_buffer`, `t1_status_resilience`, `t1_sustained_recovery`, `t1_sprint_acceleration`, `t1_double_jump` | 바닐라 철 검, 불사의 토템, 철 흉갑, 우유 양동이, 반짝이는 수박, 설탕, 깃털. 이름에 "(T1, provisional)" 표기 | 임시. 승인된 시각 정체성(`ACCESSORY_VISUAL_CONCEPT_2026-10-22.md`)은 당시 등록된 12종만 다룸 | 결함 | 중(아트 7장 + 명칭) |
| A-06 | 단검 7 + 대검 7 | 모두 바닐라 `iron_sword` 모델 | 미정 | 결함 | 중 |
| A-06 | 원소 코어 5종 | ender_eye, heart_of_the_sea ×2(천둥의 심장·해류의 심장이 **같은 아이콘**), fire_charge, echo_shard | 임시(`CUSTOM_MOB_RESOURCE_BACKLOG.md` "temporary display references") | 결함 | 중 |
| A-06 | 계약서 2종 | paper / map | 미정 | 권장 | 소 |
| A-06 | 유틸: 포획 도구(brush), 포획 알(egg), 낙하산(feather), 광부 랜턴(lantern), 포션 주입기(potion), 휴대 화로(block/furnace), 채굴 드릴(netherite_pickaxe), 백팩 3종(bundle/bundle/shulker_box), 건틀릿(iron_ingot) | 바닐라 아이콘 | 미정(`UTILITY_EQUIPMENT_FINALIZATION.md`는 기능 계약 중심) | 결함(바닐라 아이템과 혼동) | 중 |
| A-06 | `colony_crystal` | 바닐라 자수정 조각 텍스처 | 미정 | 권장 | 소 |
| A-08 | 탐험 침낭 | `cube_all` 초록 양털 정육면체(BedBlock인데 블록 엔티티 없음). 실제 렌더 결과는 확인 못 함 | 미정 | 결함 | 중 |
| A-08 | 통발 / 자동 농장 | 통 옆면 / 관측기 앞면 큐브 | 미정 | 결함 | 중 |
| A-08 | 거미 알주머니 / 매달린 고치 | 바닐라 거북 알 모델 / 거미줄 텍스처 상자 | 임시(거미 군락 "textures provisional") | 권장 | 소 |
| A-09 | 폴른 센티넬 무기 | 렌더 전용 바닐라 철 검(`FallenSentinelRenderer.java` 주석 "temporary stand-in") | 임시 | 결함 | 중 |
| A-09 | 스카이가드 궁수 무기 | 바닐라 활(실제 장비) | 임시(렌더러 주석 "no weapon of its own") | 권장 | 중 |
| A-12 | 어린 거미(`juvenile_spider`) | 바닐라 거미 모델과 바닐라 텍스처 그대로 | 미정 | 결함(바닐라 거미와 구분 불가) | 소 |
| A-12 | 어미 거미 | 바닐라 거미 모델 + 임시 텍스처·임시 크기(코드 주석) | 임시 | 권장 | 소 |
| A-13 | 구형 물고기 10종, 구형 장비 4·5티어 6종 | cod/iron_nugget/tropical_fish, `crankbait_4/5`는 `crankbait_3` 재사용 | 저장 호환용(크리에이티브 미노출) | 권장 | 소 |

### 3.3 해상도·스타일 불일치 (승인 자산 포함 — 변경 제안 아님)

| 자산 | 크기 | 상태 | 비고 |
|---|---|---|---|
| `flame_sword.png` | 1263×1246 + 3D 요소 | **승인, 불변**(Flame Sword C1-A/C1-B) | 다른 무기(16/32px)와 밀도가 크게 다르다. 기록만 하고 변경은 제안하지 않음 |
| 물고기 12종 | 512×512 + 3D 요소 | 승인 매니페스트(`FISHING_RESOURCE_D_APPROVED_MANIFEST.json`) | 기록만 함 |
| 릴·크랭크베이트·미노우 1~3 | 280~327 × 173~250, 정사각형 아님 | 승인 매니페스트로 추정(개별 승인은 대조 못 함) | 확인 필요 |
| 광물·재료·액세서리 | 32×32 | 승인(광물, 액세서리 12종). `gale_shard`, `etherium_shard`, `etherium_ingot`, `sky_feather`는 승인 기록을 찾지 못함 | 바닐라 16px 대비 2배 밀도 |
| `bb_fallen_sentinel.png`, `bb_skyguard_archer.png` | 512×512 | Blockbench 교체본 | 다른 몹(64×64)보다 픽셀 밀도가 8배. 오너 외형 승인 여부는 문서에서 확인 못 함 |
| 진행 선택 카드 아트(`gui/selector/approved_full_panel.png`) | 고해상도 회화풍 | 승인(파일명, 4카드 승인 UI) | 바닐라 픽셀 스타일과 다르지만 승인 자산이라 기록만 함 |

---

## 4. 3D 리소스 필요 목록

난이도: **T** = 바닐라 템플릿 모델에 텍스처만 / **BB-S** = Blockbench 단순 정적 모델 / **BB-A** = Blockbench 모델 + 전용 애니메이션 / **CODE** = 코드 지오메트리(현 구현 방식).

### 4.1 몹·보스

| 대상 | 현재 | 문서상 모델 요구 | 필요 작업 | 난이도 | 구현 |
|---|---|---|---|---|---|
| 하늘의 정령 | 없음 | "알레이 모델 기반 … 초기에는 바닐라 모델/텍스처를 재사용하고 추후 리소스 교체 가능"(SKY §3) | 처음에는 알레이 템플릿 + 텍스처 | T | 미구현(그룹 A 보류, SKY §20) |
| 폭풍의 사냥꾼 | 없음 | "팬텀 모델 기반 … 별도 전용 애니메이션 없음"(SKY §3) | 팬텀 템플릿 + 텍스처 | T | 미구현 |
| 천공의 비룡 | 없음 | "축소된 엔더 드래곤 모델 기반 … 비룡에만 전용 애니메이션 … 필요 시 전용 모델 계층/컨트롤러 도입 가능"(SKY §3) | 드래곤 재사용 범위를 기술 검증한 뒤 전용 모델·애니메이션(날갯짓, 브레스, 돌진, 날개 휘두르기, 피격) | BB-A | 미구현 |
| 부유 해파리 | 없음 | "반투명 몸체와 촉수, 공중에서 느리게 부유"(SKY §5) | 대응하는 바닐라 템플릿 없음 → 전용 모델 + 반투명 렌더 + 촉수 흔들림 | BB-A | 미구현 |
| 용암 대장장이 군주(불 보스) | 없음 | 정체·스킬 확정, "불기둥(개별 전용 모델링 연출)"(`boss_sync_2026-11-15/…불보스_바다보스…md`) | 보스 모델 + 불기둥 연출 모델 | BB-A | 미구현(스폰 구조물 deferred) |
| 심해의 해신(바다 보스) | 없음 | "일반 스킨 휴머노이드 기반 + 프리즈머린 왕관·견갑·발광 수염·삼지창 3D 파츠"(같은 문서) | 휴머노이드 템플릿 + 3D 파츠(검투사 방식 CODE 가능) | BB-S~CODE | 미구현 |
| 설원 자연 스폰 보스 | 없음 | 정체·전투 deferred | 설계 결정 전에는 산정 불가 | — | 미구현 |
| 왕릉 수호자 | 워든 골격 + 코드 사암 지오메트리 + 256px 텍스처·발광 | "(A) 모델 교체 + 일부 공격 키프레임 보정", 외형 1안 사암 거신(`st01_tomb_guardian/…설계_확정…md` §3, 레퍼런스 PNG 동봉) | 레퍼런스와 맞는지 실화면 검토. 반복 수정을 쉽게 하려면 Blockbench로 옮기는 것도 검토 | CODE(현재) / BB-A | 인덱스: "temporary dummy, code freeze, live pending" |
| 불멸의 검투사(ST-05) | 휴머노이드 + 코드 갑옷 파츠, 1.9배 | "전용 커스텀 모델과 애니메이션 선호, 약 1.8–2.0배 … GeckoLib 미정"(`ST05_…CURRENT_AUTHORITY…md:100`) | 현재 방식이 권한의 "선호"를 충족하는지 오너가 판단. 전용 공격 애니메이션은 없음 | CODE / BB-A | 구현(라이브 미검증) |
| 폴른 센티넬 무기 | 바닐라 철 검(렌더 전용) | 렌더러 주석 "until a custom sword model is attached" | 미참조 `bbmodel/fallen_sentinel_weapon.json` 연결 검토 | BB-S | 임시 |
| 스카이가드 궁수 무기 | 바닐라 활 | 렌더러 주석 "no weapon of its own" | 미참조 `bbmodel/skyguard_archer_weapon.json` 연결 검토(당김 자세와 맞출 것) | BB-S | 임시 |
| 클라우드 레이스, 클라우드 슬라임, 크리스탈 벌레, 폴른 센티넬, 스카이가드 궁수(본체) | 전용 모델 있음 | SKY-MOB §B1/C1/D1. D1(크리스탈 벌레)은 승인 기록 있음 | 추가 3D 작업 없음. 라이브 검증만 남음 | — | 구현 |
| 금 안내 골렘 | 철 골렘 템플릿 + 금 텍스처 | 렌더러 주석 "reuses the vanilla iron golem model" | 없음(템플릿 방식이 의도) | T | 구현 |
| 어미 거미 / 어린 거미 | 바닐라 거미 템플릿 | 거미 군락 설계: 텍스처 provisional | 어린 거미 전용 텍스처, 어미 거미 텍스처 확정 | T | 구현(월드젠 미연결) |
| 일반 커스텀 몹 44종, 하늘섬 변종 5종, 피라미드 4종 | 바닐라 모델 + 전용 텍스처 | "No custom 3D model is required … Only after explicit later approval"(`VANILLA_MODEL_CUSTOM_MOB_ROSTER.md:12,121`), SKY §20 "바닐라 모델·UV 그대로" | **3D 불필요(설계 의도)**. 텍스처 시각 QA만 대기 | T | 구현 |

### 4.2 무기·장비·장치

| 대상 | 현재 | 문서 근거 | 제안 | 난이도 |
|---|---|---|---|---|
| T2 원소 무기 5종 | 다이아몬드 검 | SKY §1 "고유 텍스처/모델" | 최소안: 32×32 handheld 텍스처. 불꽃검처럼 3D 요소를 쓰려면 `RESOURCE_ASSET_PRESENTATION_CONTRACT.md` 규칙을 따름. **바람 활**은 `pulling_0~2` 상태 분기가 필요하고, **대지의 철퇴**는 바닐라 철퇴 템플릿 사용 가능 | T~BB-S, 바람 활은 중 |
| 암흑 에너지 지팡이 | 다이아몬드 검 | deferred | 설계 결정 후 | — |
| 단검·대검 | 철 검 | 미정 | 티어별 템플릿 텍스처(대검은 크기를 키운 handheld 모델) | T |
| 통발(블록 엔티티) | 통 큐브 | 미정 | 바구니/통발 형태 블록 모델 | BB-S |
| 자동 농장(블록 엔티티) | 관측기 큐브 | `automation-device-ui-approved-2026-10-10.md`는 동작·UI만 다룸 | 장치 블록 모델/텍스처 | T~BB-S |
| 탐험 침낭 | 양털 큐브 | 미정 | 침대형 낮은 모델. BedBlock 렌더 경로 확인이 먼저 | BB-S |
| 채굴 드릴, 휴대 화로, 포션 주입기, 낙하산, 백팩 | 바닐라 아이콘 | 미정 | 우선 2D 아이콘. 착용·전개 3D(낙하산 펼침, 등에 멘 백팩)는 권한에 없으므로 **제안만** | T / BB-S |

---

## 5. UI 디자인 감사 (화면별)

공통 관찰:
- **C-14 시각 검증 한계:** `client-visual-validation`의 70×4개 `*-validation.json`은 모두 통과지만, 검사 내용은 PNG 유효성과 색 분산뿐이고 `"visual_content": "NEEDS_MANUAL_REVIEW"`이다. 진행 화면은 **GUI 2/3만** 캡처하고 GUI 4나 좁은 창은 없다(`structure-preview-regression-keyboard-resized`만 예외). 액세서리와 인챈트 쪽에만 GUI 4 진단이 있다. (권장, 중)
- **C-15 토스트 가림:** 캡처에 "대화 메시지를 검증할 수 없습니다", 튜토리얼, 소셜, 레시피 토스트가 남아 제목과 결과 슬롯을 가린다(`progression-selection.png`, `piglin-trade.png`, 시트 `inventory-regression-*`). 하네스에서 토스트를 닫으면 증거 품질이 좋아진다. (권장, 소)

| ID | 화면 | 발견 | 심각도 | 크기 | 근거 | 개선안 |
|---|---|---|---|---|---|---|
| C-01 (정정 §8 참조) | 인챈트 진행(`EnchantingProgressionScreen`, 330×360) | GUI 4(480×270)에서 탭이 y=-37, 슬롯 28개가 화면 밖. GUI 3은 위 여백 0이라 탭 윗부분이 잘림 | 깨짐 | 중 | `enchanting-gui-scale-diagnostic.txt`, 시트 `enchant-scale-3/4-*` | 높이를 270 이하로 줄이거나(인벤토리 줄 압축, 탭을 패널 안으로), 넘칠 때 스크롤 영역. 바닐라 모루·인챈트대(166 높이)와 맞추는 것이 기준 |
| C-02 | 낚싯대(`FishingRodScreen`, 330×270) | 높이 270 고정. 1280×720 GUI 3(240)에서 30px 넘칠 것으로 계산됨. 커스텀 어두운 테마(`0xEE17212C`)라 바닐라와 톤이 다름 | 결함(추정) | 중 | `FishingRodScreen.java:25,51` | 가용 높이에 맞춘 축소 배치. **NOT RUN: 스크린샷 없음** |
| C-03 | 계약(`ContractScreen`, 260×240) | 목표·보유·보상 줄에 폭 제한이나 줄바꿈이 없어 긴 대상 이름(한국어 구조물명 등)이 패널 밖으로 나갈 수 있음. 완료(초록)/부족(진홍) 색 구분은 있음(좋음) | 결함(추정) | 소~중 | `ContractScreen.java:98-136` | `font.split`으로 줄바꿈하거나 말줄임 + 툴팁. 패널 높이를 가용 높이에 맞춤. **NOT RUN** |
| C-04 | 연구·지식 툴팁 | 내부 ID 노출("V3 Research 08"), "Tier: Unspecified/분류 미확정", 라벨형 문구("Next level effect description/다음 레벨 효과 설명"). 비용·재료·상태 줄이 무색이라 가능/불가/부족을 한눈에 알 수 없음. GUI 3에서 툴팁이 캔버스의 약 70%를 덮고 화면 제목과 겹침 | 결함 | 소~중 | `gui-scale-3-en_us/research-node-tooltip.png`, `gui-scale-3-ko_kr/research-unavailable-tooltip.png`, `ProgressionTreeCanvas.java:1040-1090` | 선행 연구는 현지화 이름으로 표시(이름이 없으면 숨김). 미확정 단계 줄은 숨김. 충족=초록·부족=빨강(바닐라 관례). 상태 줄 색 구분. 툴팁 최대 폭과 높이 제한 |
| C-05 | 연구 트리 캔버스 | 가지 헤더 바가 뷰포트 왼쪽에서 잘려 라벨이 안 보임(`research-screen.png` 왼쪽 빈 바, `research-unavailable-tooltip.png`의 "전" 잘림). 위쪽 1/3이 빈 공간. 구매 가능/보유/잠김이 미세한 테두리 색 차이뿐이고 범례 없음 | 권장 | 소 | `gui-scale-3-ko_kr/research-screen.png`, `gui-scale-2-en_us/research-purchasable-entry.png` | 헤더 라벨을 뷰포트 안에 고정(sticky). 첫 화면을 첫 노드 쪽으로 맞춤. 노드 상태 범례 또는 아이콘 배지. 승인된 "vanilla advancement visuals" 범위 안에서 |
| C-06 | T1 모루 강화 모드 | 부족 상태와 충분 상태의 겉모습이 같음(비용 줄 단색, 버튼 비활성 차이 미미). 정보 줄 "Raw XP: 40 Stone: 2 Catalyst: 1"이 촘촘함 | 결함 | 소 | 시트 `anvil-scale-3-accessory-insufficient` vs `…-4-to-5-before`, `T1AnvilScreen.java:173-193` | 부족한 항목만 빨강. 보유량을 함께 표시("Stone 1/2"). 기능 단언은 모두 통과(`anvil-functional-assertions.txt`) |
| C-07 | 자동 농장(`AutoFarmScreen`) | 컨테이너 텍스처가 아닌 단색 fill 패널. "보관함" 라벨(y=116)이 입력 격자 아래 테두리(y=122)와 겹침. 제목이 어두운 글자 + 그림자라 번져 보임(바닐라 컨테이너 제목은 그림자 없음). "재파종 재료/수확물" 라벨(y=20)이 제목 띠(y≤26)와 겹침 | 결함 | 소 | `gui-scale-3-ko_kr/auto-farm-screen.png`, `AutoFarmScreen.java:13-38` | 라벨 y 조정(inventoryLabelY≈124), 제목 그림자 제거. 승인 문서는 "UI 컨트롤을 임의로 추가하지 말 것"을 요구하므로 상태 표시기는 **오너 결정 사항** |
| C-08 | 진행 선택(4카드) | 승인 UI. 한국어 카드 설명은 3줄 안에 들어감. 제목은 토스트에 가려 확인 못 함 | — | — | `gui-scale-3-ko_kr/progression-selection.png` | 변경 제안 없음. 토스트 없이 재캡처 권장 |
| C-09 | 7섹터 선택 | 선택된 섹터 이름만 중앙에 나오고 나머지 6개는 아이콘뿐. 진행도(해금 수) 표시 없음 | 권장 | 소 | `gui-scale-2-ko_kr/research-sector-selection.png` | 호버 툴팁이나 섹터별 "n/m" 소형 표기. 승인된 7섹터 구조는 유지 |
| C-10 | XP 퀘스트 트리 | 압축 레이아웃은 깔끔함. 미수령 보상 합계나 보유 XP 요약이 화면에 없음 | 권장 | 소 | `gui-scale-3-ko_kr/xp-quest-tree-compact.png` | 헤더 요약 줄(오너 승인 필요) |
| C-11 | 액세서리 인벤토리 | 승인 레이아웃. GUI 2/3/4 모두 정상, 좁은 GUI 4(340×200)도 정상(`layout-verification-report.txt` 전 항목 ok). 빈 슬롯에 실루엣이 없어 슬롯 종류를 툴팁으로만 알 수 있음 | 권장(승인 범위) | 소 | `ui-contact-sheet.png`, 시트 `accessory-inventory-*` | 승인안이 "permanent labels 없음, tooltips only"이므로 빈 슬롯 실루엣 아이콘은 **오너 결정 사항**으로만 제안 |
| C-12 | 피글린 교역 | 바닐라 상인 화면 재사용. 머리글 "Trades"만 있고 상인 이름이나 화폐 안내가 없음. 결과 슬롯이 토스트에 가려 확인 못 함 | 권장 | 소 | `piglin-trade.png` | 권한(`UI_PIGLIN_TRADE_MOB_TRACKER_APPROVED_2026-10-10.md`)이 "PENDING currency confirmation"이므로 문구는 확인 후 결정 |
| C-13 | 백팩 | 바닐라 ChestMenu라 일관됨. 제목 키 `container.moreminecrafts.backpack`이 ko에 없어 "Backpack"으로 나옴 | 결함 | 소 | 시트 `inventory-regression-backpack-open` | ko 키 추가 |
| C-16 | 한·영 문구 길이 | 연구 툴팁 한국어는 폭 안에서 줄바꿈됨(정상). 구조 미리보기 긴 라벨 검증 JSON 통과. 계약·낚싯대·자동 농장은 줄바꿈 처리가 없음 | 권장 | 소 | 위 근거 | 고정폭 라벨에 `font.split` 적용 |
| C-17 | 입력 일관성 | 연구는 한 번 클릭 구매(승인된 2026-10-12 정정과 일치), 키보드 탐색 캡처 있음(`structure-preview-keyboard-navigation`, `selector-xp-quest-opened-by-keyboard`). Shift·휠 동작은 스크린샷으로 판정할 수 없음 | — | — | — | 라이브 확인 대상 |

---

## 6. 확인하지 못한 항목

| 항목 | 이유 |
|---|---|
| 계약 화면, 낚싯대 화면과 미니게임, 휴대 화로·채굴 드릴·포션 주입기·통발 컨테이너 | 해당 화면 스크린샷 아티팩트가 없음. 코드로만 평가함(**NOT RUN**) |
| 몹 트래커, 상자 정렬, 호퍼 필터(UI_SYSTEM_FULL_AUDIT 항목) | 클라이언트 코드에서 전용 화면을 찾지 못함. 캡처 없음 |
| 누락 모델 아이콘의 실제 모습(A-01) | 정적 판정. 해당 아이템을 보여 주는 스크린샷이 없음 |
| 침낭(BedBlock) 실제 렌더 | 캡처 없음. GameTest 배치는 통과(`St03WorldgenAndGameplayGameTests`) |
| 진행 화면의 GUI 4와 좁은 창 | 아티팩트가 GUI 2/3만 있음 |
| 지식 화면, 구조 미리보기 전체 | 일부만 표본으로 열어 봄. 70장 전부는 보지 않음 |
| 천계 몹 3모델 영상(`sky-mob-three-video`, 약 206MB), 크리스탈 가디언·피라미드 몹 캡처 | 용량 문제나 목록 범위 밖이라 내려받지 않음. Blockbench 4방향 캡처는 확인함 |
| ST-03 캡처 | 다른 세션이 진행 중인 범위라 화면 평가는 하지 않음(로그만 확인) |
| 2D/3D 아트 품질과 체감 | **LIVE VALIDATION PENDING**(사람 판단 영역) |

## 7. 이번 감사에서 하지 않은 것

- 에셋·코드·권한 문서는 바꾸지 않았다. 워크플로를 실행하거나 추가하지 않았다.
- 승인 자산(불꽃검, 물고기·낚시 장비, 광물, 액세서리 12종, 선택 카드 아트, 액세서리 인벤토리 배치)은 기록만 하고 변경을 제안하지 않았다.

## 8. 정정 (수정 작업 중 확인한 내용)

수정 작업(`claude/asset-ui-fixes-20261124`)에서 코드를 더 읽고 발견한 정정이다. 위 본문은 감사 시점 기록으로 두고 여기서 바로잡는다.

| 항목 | 정정 |
|---|---|
| C-01 인챈트 화면 | `EnchantingProgressionScreen.init()`은 이미 `ViewportScale.fit(…, padding 8)`로 창에 맞춰 축소한다. 진단 로그의 `panelFits=false`와 `widgetsOffScreen`은 축소 전 좌표를 재므로 **실제 깨짐의 증거가 아니다**. 다만 통합 시트의 GUI 3/4 타일은 위아래가 잘려 보여 축소가 반영되었는지 확신할 수 없다. PR #323 CI 시트(`accessory-integration-contact-sheet-45439136922`)로 재확인: GUI 3/4 타일에서 패널 너비가 각각 330·440 타일px로 측정되어 **축소가 적용되지 않은 크기**와 일치한다(GUI 4에서 `ViewportScale.fit`이 적용됐다면 약 310). 즉 실제로 잘리는 것으로 보이며, 코드에 축소가 있는데도 반영되지 않는 원인은 아직 모른다(캡처 하네스의 해상도 변경 방식 때문일 수도 있음). 코드는 바꾸지 않았고, 실클라이언트 확인 후 원인을 정한다. |
| C-04 연구 툴팁의 "V3 Research 08" | 코드가 내부 ID를 만드는 것이 아니라 `en_us.json`의 연구 제목 60개가 임시 문구 "V3 Research NN"이다. 한국어는 실제 이름이 있다. 영어 제목은 명칭 결정이므로 **C(오너 결정)**로 옮긴다. 코드에서는 미확정 단계 줄 숨김과 충족·부족 색만 처리했다. |
| A-07 ko 누락 키 | 한국어 이름은 권한 문서에 없는 항목이 대부분이다(티타늄, 미스릴, 유틸 장비 등). 기존 용어로 확정되는 `block.moreminecrafts.auto_farm`("자동 농장")만 추가했다. 나머지는 **C(명칭 결정)**. |
| 검증 한계 | 클라이언트 캡처 워크플로(`client-visual-validation`, `accessory-client-acceptance`)는 수동 실행 트리거가 없고 특정 브랜치·PR에서만 돈다. 수정 브랜치의 화면 변경은 **NOT RUN**이다. |

**C-01 결론:** 원인은 `extractBackground`(패널·슬롯 틀)가 화면 축소 변환 밖에서 그려져 위젯만 줄어들던 것이었다. 진단 로그에서 축소 배율은 정상(GUI 3 약 0.956, GUI 4 약 0.706)이었다. `0adf20d`에서 배경에도 같은 변환을 적용했고, CI 캡처에서 GUI 4 패널 폭이 433에서 301 타일px로 줄어 하단 테두리와 인벤토리·핫바가 모두 보임을 확인했다. 통합 시트 타일은 위쪽 26px을 제목 막대가 덮으므로 탭이 잘려 보이는 것은 시트의 착시다. 위 정정 표의 C-01 행과 §1의 "깨짐" 판정은 이 결론으로 대체한다.

# 천국 하늘섬 P2.5 — 게임플레이 정책 검증 · P3 준비 보고서 (2026-11-20)

- 기준: `claude/heaven-sky-island-p2-20261120` @ 4800bf9 (P2 V5 외형·자원 구조)
- 작업 브랜치: `claude/heaven-sky-island-p2-5-20261120`
- 성격: **정책 검증 · 명세 · 최소 기술 검증 전용**. 승인되지 않은 게임플레이 변경은 코드에 적용하지 않았다.
- 변경 범위: 테스트 소스셋 프로브 1개, 분석 도구 1개, 문서·데이터. 제품(main 소스셋) 코드·자원·NBT·worldgen·구조물 ID 변경 없음. P1/P2 프로토타입 무변경.

## 0. 요약

| 항목 | 결과 |
|---|---|
| 현재 천국 자연 블록 | 장식용 일반 `Block`. 번짐·소멸·길·경작·뼛가루·눈 덮임 **모두 없음**. 바닐라 식물 **전부 생존 불가** |
| 핑크 꽃잎 소멸 원인 | `VegetationBlock.mayPlaceOn` = `#minecraft:supports_vegetation` 검사. 천국 흙·잔디가 이 태그에 없음 (P2 원인 확정, 프로브로 재확인) |
| 태그만으로 해결되는가 | **아니오**. 생존(꽃·묘목·풀)은 태그로 해결되지만 번짐·소멸·뼛가루·눈·길·경작은 각각 클래스/데이터 레지스트리 작업 필요. 농작물·버섯·사탕수수는 추가 조건 |
| 거대 꽃 연결성 | 꽃 3종 각각 단일 연결 성분(121/115/105 블록). 전체 붕괴(B안)·단계 붕괴(C안) 모두 유한하게 구현 가능 |
| 직소 | 19개 템플릿 × 6테마 = 128개 직소. `final_state` 바닐라 지형 30개, 석재 8개, 공기 90개 |
| 19 템플릿 사전 검증 | 114개(6테마×19) 전수 조사 완료. 거대 꽃 권장 수: 대형 23–56, 중형 2–3, 소형·위성 1, 탑 0 |
| 회귀 | GameTest **726/726**(기존 721 + 프로브 5), JUnit **791/791**, `./gradlew test build` 성공 |

---

## A. 자연 블록 정책 결정표

대상: `heaven_grass_block`, `heaven_loam`, `heaven_cream_stone`(+`_pale`). "현재" 열은 프로브 `HeavenP25PolicyProbes` 실측값 (`data/probe_results.txt`).

| # | 항목 | 현재 P2 동작 (실측) | 바닐라 동작 (26.3) | 권장안 | 대안 | 구현 방법 | 영향 | 승인 |
|---|---|---|---|---|---|---|---|---|
| A1 | 잔디 번짐 | 없음. 천국 잔디 옆 천국 흙·바닐라 흙 400 랜덤틱 후 변화 없음 | 잔디→흙(`#dirt` 기준 블록) 번짐, 빛 9 이상 | **천국 잔디는 천국 흙으로만 번짐**. 바닐라 흙으로는 번지지 않음 | (a) 번짐 없음 유지 (b) 천국·바닐라 흙 모두 | `SpreadingSnowyBlock` 하위 클래스(protected 생성자, `baseBlock`=`heaven_loam` 키). 번짐/소멸 대상이 `baseBlock`으로 결정됨 | 랜덤틱 부하 소량. 흙 길이 다시 잔디가 됨 | 필요 |
| A2 | 저조도 소멸 | 없음. 덮여도 천국 잔디 유지 | 위가 불투명/물이면 흙으로 | **덮이면 천국 흙으로 소멸** (A1과 한 쌍) | 소멸 없음(장식 우선) | A1과 같은 클래스에서 자동 (`canBeGrass`→`baseBlock`) | 건축 시 잔디 아래가 흙으로 바뀜. 섬 하부 외형 영향 없음(하부는 바위) | 필요 |
| A3 | 삽 → 길 | 없음 | `block_transformer/shovel.json`: `#turns_into_dirt_path` + 위 공기 → `dirt_path` | **바닐라 `dirt_path` 로 변환** (천국 전용 길 없음) | (b) 천국 전용 길 블록 신규 (c) 불가 유지 | 태그 `minecraft:turns_into_dirt_path` 에 천국 잔디·흙 추가 (데이터만) | 길은 바닐라 색. 길 → 잔디 복귀 시 바닐라 흙이 됨 | 필요 |
| A4 | 괭이 → 경작 | 없음 | `hoe.json`: `#turns_into_farmland` → `farmland[moisture=0]` | **바닐라 `farmland` 로 변환** | 천국 전용 경작지(블록 + `block_transformer` 덮어쓰기, 호환 위험 큼) | 태그 `minecraft:turns_into_farmland` 추가. 경작지가 마르면 바닐라 흙 | 하늘섬 농사 가능. 경작지 소멸 시 바닐라 흙이 남음 | 필요 |
| A5 | 식물 생존 | 바닐라 식물 14종 **전부 false** (천국 잔디·흙·석재). 천국 식물은 어디서나 true(규칙 없음) | 꽃·풀·묘목·꽃잎·베리: `#supports_vegetation`. 진달래 `#supports_azalea`, 농작물 `farmland`, 마른 식물 `#supports_dry_vegetation`, 버섯 빛+단단함, 사탕수수 물 인접 | **천국 잔디·흙을 `#minecraft:dirt`(또는 `#grass_blocks`)에 추가** → 꽃·풀·묘목·꽃잎·베리·진달래·마른 덤불·대나무 생존 | (b) `#supports_vegetation` 에만 직접 추가(부작용 최소, 대신 대나무·사탕수수·진달래는 개별 태그) (c) 불가 유지 | 데이터 태그. 상세 C 절 | `#dirt` 경유 시 부작용: 스컬크 대체, 엔더맨 집기, 나무 줄기 아래 교체 금지, 사탕수수(물 필요), 동굴 덩굴 등. **(b) 권장 근거** 아래 참조 | 필요 |
| A6 | 뼛가루 | false | 잔디: `BonemealableBlock` → 주변에 풀·꽃 배치. 흙: 없음 | **천국 잔디에 뼛가루 적용 → 천국 짧은 풀·천국 꽃 배치** | 바닐라 꽃 배치 / 불가 유지 | `BonemealableBlock` 구현 (`performBonemeal`). 배치 목록은 데이터(placed_feature 또는 자체 JSON) | 천국 식생이 플레이어 손으로 퍼짐. 꽃 아이템 획득 경로와 연동 필요(B 절) | 필요 |
| A7 | 눈 덮임 | `snowy` 속성 없음 | `SnowyBlock.snowy` 위가 `#snow` 일 때 true, 측면 텍스처 변경 | **도입 안 함** (천국은 눈 없음. 설원 테마는 바닐라 눈 블록 유지 가정) | A1 클래스 채택 시 자동으로 생기므로 눈 텍스처 추가 | A1 채택 시 `snowy` 상태가 생김 → 블록스테이트 JSON에 `snowy=true` 모델(기존 모델 재사용 가능) | 모델 1개 재사용, 신규 텍스처 0 | 필요 (A1과 연동) |
| A8 | 경도·도구 | 잔디 0.6 삽 / 흙 0.5 삽 / 석재 1.5 곡괭이·도구 필수 (폭발 저항 잔디 0.6, 석재 6) | 잔디 0.6, 흙 0.5, 돌 1.5/6 | **현행 유지** (바닐라와 동일) | 천국 돌 경도 상향(희소성) | 변경 없음 | 없음 | 필요 (확정 승인만) |
| A9 | 드롭·섬세한 손길 | 잔디 → 천국 흙, 섬손 → 잔디. 흙 → 흙. 석재 → **석재 그대로**(섬손 무관) | 잔디 → 흙, 섬손 → 잔디. 돌 → 조약돌 | 잔디·흙 **현행 유지**. 석재는 **현행(자기 자신)** 유지 권장 | 석재 → "천국 조약돌" 신규(블록·텍스처·레시피 추가) | 루트 테이블 변경 없음 | 조약돌 단계 없음 → 건축재로 바로 사용 | 필요 |
| A10 | 태그 | `mineable/shovel`(잔디·흙), `mineable/pickaxe`(석재)만 | — | `mineable/*` 유지 + 승인 항목에 따라 `supports_vegetation`, `turns_into_dirt_path`, `turns_into_farmland`, `animals_spawnable_on`, `enderman_holdable`(선택) | `#dirt`/`#grass_blocks` 에 넣어 바닐라 동작 일괄 상속 | 데이터 태그만 | `animals_spawnable_on` 미포함 시 바닐라 동물 자연 스폰 불가(현재 상태). `SkyIslandHabitat` 은 팔레트 `isSolid` 기반이라 태그와 무관 | 필요 |

### A5 보충 — 태그로 해결되는 것 / 안 되는 것

| 식물 | 생존 검사 (26.3 바이트코드) | `#supports_vegetation` 추가만으로 | 추가 조건 |
|---|---|---|---|
| 꽃·풀·고사리·묘목·핑크 꽃잎·베리 덤불 | `VegetationBlock.mayPlaceOn` = `#supports_vegetation` | **해결** | — |
| 진달래 | `#supports_azalea` = [#supports_vegetation, clay] | **해결** | — |
| 마른 덤불 | `#supports_dry_vegetation` ⊃ #supports_vegetation | **해결** | — |
| 대나무 | `#supports_bamboo` | 별도 태그 필요 | — |
| 사탕수수 | `#supports_sugar_cane` + 인접 물 | 별도 태그 필요 | 물 인접 |
| 밀 등 농작물 | `farmland` 만 | 해결 안 됨 | A4(경작) 승인 필요 |
| 버섯 | 빛 13 미만 + `isSolidRender` (또는 `#mushroom_grow_block`) | 무관 | 어두운 곳 |
| 나무 생장 | 묘목 생존 + 줄기 아래를 흙으로 교체 (`#cannot_replace_below_tree_trunk` = #dirt·#mud·#moss·podzol) | 생존은 해결 | 태그 미포함이면 **천국 잔디가 바닐라 흙으로 교체됨** → 나무를 기른다면 이 태그에도 추가 필요 |

권장 근거: `#dirt` 로 넣으면 한 줄로 끝나지만 `#enderman_holdable`, `#cannot_replace_below_tree_trunk` 및 `#substrate_overworld` 경유 `#sculk_replaceable`·`#azalea_root_replaceable` 등 바닐라·타 모드가 `#dirt` 를 참조하는 모든 동작을 상속해 범위를 통제할 수 없다. 개별 태그 열거(b)가 검증 가능하다.

### A — 블록 ID 후보 (확정 아님)

| 현재 (P1, 프로토타입) | 후보 1 (현행 유지, 권장) | 후보 2 (바닐라식) | 후보 3 (지명) |
|---|---|---|---|
| `heaven_grass_block` | 유지 | `heaven_grass_block` | `celestial_grass_block` |
| `heaven_loam` | 유지 | `heaven_dirt` | `celestial_loam` |
| `heaven_cream_stone` | 유지 | `heaven_stone` | `celestial_stone` |
| `heaven_cream_stone_pale` | 유지 | `pale_heaven_stone` | `celestial_stone_pale` |

ID 변경 시 저장 데이터 호환성: C 절.

---

## B. 식생 정책 결정표

대상: P2 식생 18종(`heaven_giant_stem/leaf/bloom`, `heaven_lavender/hydrangea/phlox`, `heaven_flower_{red,yellow,white,pink,blue,violet}`, `heaven_short_grass`, `heaven_tall_grass`, `heaven_vine`, `heaven_wisteria`, `heaven_petals`, `heaven_white_blossom_leaves`) + 재사용 바닐라 `oak_log`·`cherry_leaves` + 거대 꽃 구조 3종.

### B-1. 거대 꽃 파괴 방식 비교 (결정하지 않음)

실측 연결성 (`data/giant_connectivity.json`, P2 섬): 6·18·26 연결 모두 3개 성분.

| 거대 꽃 | 블록 수 | 구성 | 밑동부터 붕괴 파동 수 |
|---|---:|---|---:|
| starlotus | 121 | 꽃 71, 잎 26, 줄기 24 | 9 |
| lily | 115 | 꽃 68, 잎 28, 줄기 19 | 11 |
| daffodil | 105 | 잎 54, 꽃 35, 줄기 16 | 10 |

| 안 | 동작 | 장점 | 단점 | 구현 | 난이도 |
|---|---|---|---|---|---|
| **A 독립 부품** | 각 블록 개별 파괴. 줄기를 부숴도 위는 공중에 남음 (현행) | 구현 0, 건축 장식으로 자유 | 공중 부유 꽃 대량 발생, 바닐라 직관과 다름 | 없음 | 하 |
| **B 핵심 줄기 제거 시 전체 제거** | 줄기 파괴 → 연결된 같은 꽃 부품을 한 틱에 flood fill 제거 (≤121 블록) | 깔끔, 바닐라 대나무·사탕수수와 유사한 직관 | 드롭 폭증 가능(드롭 규칙 필요), 인접한 다른 꽃과 접촉 시 오인 → 성분 ID 또는 거리 제한 필요 | `HeavenPartBlock.playerDestroy`/`affectNeighborsAfterRemoval` 에서 BFS, 상한 256 | 중 |
| **C 단계 붕괴** | 지지 잃은 부품이 틱마다 한 겹씩 사라짐 (9–11 틱, 코러스 식물식) | 시각적으로 자연스러움, 서버 부하 분산 | 예약 틱 다수, 붕괴 중 플레이어 개입 시 예외 상황 | `updateShape` + `scheduleTick` 으로 지지 검사 | 중상 |

### B-2. 항목별

| # | 항목 | 현재 P2 (실측) | 권장안 | 대안 | 승인 |
|---|---|---|---|---|---|
| B1 | 충돌 | 거대 줄기만 충돌(solid). 꽃·잎·소형 식물 없음 | 현행 유지 (꽃 위 걷기 불가 = 의도) | 거대 꽃잎/잎 상면 충돌(올라탈 수 있게) | 필요 |
| B2 | 거대 꽃 파괴 | A안(독립) | 미결정 — B-1 비교 | B / C | **필요 (핵심)** |
| B3 | 연쇄 | 없음 | B2 결정에 종속 | — | 필요 |
| B4 | 채굴 속도·도구 | 거대 0.4, 나머지 즉시 파괴, 도구 무관 | 거대 → `mineable/axe` 추가 (도끼 가속), 나머지 유지 | 괭이 | 필요 |
| B5 | 드롭 | **없음** (아이템 미등록, 루트 없음) | 소형 꽃·풀 → 자기 자신(아이템 신규), 거대 꽃 → 씨앗/꽃잎 소량 (전체 블록 드롭 아님) | 드롭 없음 유지(장식 전용) | 필요 |
| B6 | 섬세한 손길·가위 | 없음 | 풀·덩굴·등나무: 가위/섬손일 때만 드롭 (바닐라 풀과 동일) | — | 필요 |
| B7 | 뼛가루 | 없음 | 천국 꽃 6종 → 복제(바닐라 꽃과 동일). 거대 꽃 → 미적용 | 거대 꽃 묘목에서 성장 | 필요 |
| B8 | 성장·재생 | 없음 | P3에서는 없음 (월드젠 배치만). 재생은 A6 뼛가루로 대체 | 거대 꽃 "구근" 블록 → 구조물 NBT 성장 | 필요 |
| B9 | 불·폭발·물 | 불 0/0 (참고: 벚꽃잎 30/60, 참나무 5/5, 민들레 60/100). 폭발 저항 0–0.4. 물: 비고체 부품은 물에 대체됨, 줄기·흰 꽃잎 블록은 아님 | 불: 바닐라 꽃·잎 수준 등록 (`FlammableBlockRegistry`) 또는 **천국은 불연** 유지(권장) | 가연 | 필요 |
| B10 | 지지 제거 | 천국 꽃·풀은 **공중에 남음** (민들레는 즉시 사라짐) | 소형 식물: 지지 제거 시 사라짐 (바닐라 동일). 거대: B2 종속 | 현행 | 필요 |
| B11 | 생존 대상 | 규칙 없음 → 석재·공기 위 어디든 | 소형: 천국 잔디·흙 + `#supports_vegetation`. 꽃잎·덩굴·등나무: 덩굴은 측면 부착, 등나무는 위 지지 | — | 필요 |

---

## C. 블록 ID · 호환성 보고서

### C-1. 현재 등록 ID

- P1: `heaven_grass_block`, `heaven_loam`, `heaven_cream_stone`, `heaven_cream_stone_pale`, `heaven_flora_prototype`
- P2: 위 B 절 18종

모두 아이템 없음(P1 자연 블록 4종은 루트만), 월드젠 미배치. 존재하는 경로는 `/place template` 프로토타입뿐 → **현재는 플레이어 월드에 저장된 적이 없다**.

### C-2. ID 변경 시 저장 데이터 영향

| 사실 | 근거 |
|---|---|
| 바닐라 26.3 `MappedRegistry` 에 alias 기능 없음 | 바이트코드 확인 |
| Fabric registry sync 는 누락 엔트리를 보고만 함 (`RegistryCustomContentState$Missing`) | Fabric API 0.161.0 |
| 결과: 이름이 바뀐 블록은 기존 월드에서 **공기로 소실**, 아이템은 삭제 | — |
| 회피: DataFixer(바닐라 스키마 확장, 무겁다) 또는 청크 로드 시 자체 리맵 믹스인 | 둘 다 유지비 큼 |

**결론**: ID 는 P3 월드젠 배치·아이템 등록 **이전에** 확정해야 비용 0. 확정 후 변경은 마이그레이션 코드가 필요하다.

### C-3. 바닐라 블록 재사용 영향

`oak_log`, `cherry_leaves` 재사용: 바닐라 드롭·가연·잎 소멸(로그 거리 7) 적용. 단 구조물 배치 시 `persistent=true` 여부 확인 필요 — P2 NBT 는 `persistent=true` 로 저장됨(P2 보고서). P3 에서도 유지해야 거대 나무 잎이 소멸하지 않는다.

---

## D. 직소 복원 명세 (worldgen 변경 없음, 명세만)

데이터: `data/jigsaw_inventory.json`.

### D-1. 현황

| 구분 | 수 | `final_state` |
|---|---:|---|
| 위성 연결 (`sky_link`) | 90 | `minecraft:air` |
| 테마 지형 | 30 | grass_block 6, snow_block 5, dirt 4, red_sand 3, sand 3, gravel 3, red_sandstone 2, sandstone 2, stone 2 |
| 석재 | 8 | stone_bricks 7, smooth_quartz 1 |

풀(pool): `<theme>/satellite` (medium a–e, satellite a/b, ruins), `<theme>/tower_top` (large_d/e, `dirt`), `ruins_top` (`grass_block`), `temple_top` (`grass_block`), tower 내부 `minecraft:empty` (`grass_block`).

### D-2. 변환 규칙

1. 직소 블록 NBT(`name`, `target`, `pool`, `joint`, `orientation`, `selection_priority`, `placement_priority`)는 **원본 그대로 복사**. 좌표·방향 변경 금지.
2. `final_state` 만 테마 슬롯 맵으로 변환: 표면 → 천국 표면, 하층 → 천국 흙, 암반 → 천국 석재. `air` 와 석재 구조물(`stone_bricks`, `smooth_quartz`)은 **유지**.
3. 붙는 템플릿(`tower_top`, `ruins_top`, `temple_top`, `satellite`)도 같은 팔레트 맵으로 변환 (붙은 쪽에 바닐라 지형이 남으면 경계가 드러남).
4. 직소 셀 + 반경 4(측면)/6(내부) 기둥에는 식생 배치 금지 (D-3 검사).
5. 구조물 ID·구조물 세트·확률은 변경하지 않는다. 천국판은 **별도 ID** 로 추가하고 기존 ID 는 보존.

### D-3. 자동 검사 설계 (P3 JUnit, 미구현)

| 검사 | 내용 |
|---|---|
| 직소 동일성 | 천국 템플릿의 모든 직소가 원본과 좌표·NBT 동일 (매핑된 `final_state` 제외) |
| 잔류 바닐라 지형 | `final_state`·팔레트에 테마 지형 바닐라 블록 0 (석재 구조물 화이트리스트 제외) |
| 식생 충돌 | 직소 셀 및 반경 기둥에 식생·거대 꽃 블록 0 |
| 입구·통로 | 구조물 마스크(구조물 블록 +2) 내 식생 0 |
| 서식지 | `SkyIslandHabitat.solids()` 결과가 원본 대비 표면 기둥 수 감소 ≤ 거대 줄기 점유분 |

`tools/heaven-p25/analyze.py` 의 직소·마스크 로직을 그대로 JUnit 으로 옮길 수 있다.

### D-4. 주의

`SkyIslandHabitat` 은 `sky_islands/` 템플릿 팔레트와 직소 `final_state` 의 `isSolid()` 로 스폰 표면을 계산한다. 천국 블록은 모두 solid(실측) → 지형 변환 자체는 스폰에 중립. 단 거대 줄기·흰 꽃잎 블록도 solid 라 그 위가 스폰 후보가 될 수 있다 → P3 에서 식생 블록 제외 규칙 필요.

---

## E. P3 템플릿 확장 계획

데이터: `data/template_survey.json` (114개 전수). 아래는 meadow 19개. 다른 5테마는 같은 형태(크기·직소 동일, 지형 블록만 다름) — 형태 보존·병합 금지.

| 템플릿 | 크기 | 지형 블록 | 식재 가능 기둥 | 연못 기둥 | 거대 꽃 가능/권장 | 추정 식생 블록 | 직소 |
|---|---|---:|---:|---:|---|---:|---|
| large_a | 97×67×97 | 96,572 | 6,338 | 208 | 60 / 23 | 9,676 | — |
| large_b | 109×69×99 | 121,238 | 7,637 | 54 | 72 / 28 | 11,659 | — |
| large_c | 125×71×113 | 170,828 | 9,908 | 259 | 96 / 37 | 15,126 | — |
| large_d | 145×101×129 | 130,036 | 13,458 | 0 | 136 / 50 | 20,546 | tower_top |
| large_e | 155×101×137 | 148,440 | 15,129 | 154 | 155 / 56 | 23,097 | tower_top |
| plain_medium_a | 45×43×35 | 6,004 | 801 | 0 | 6 / 3 | 1,223 | satellite ×2 |
| plain_medium_b | 43×43×35 | 5,641 | 724 | 30 | 4 / 3 | 1,105 | satellite ×2 |
| plain_medium_c | 47×44×33 | 6,276 | 734 | 68 | 4 / 3 | 1,121 | satellite ×2 |
| plain_medium_d | 45×43×37 | 6,272 | 807 | 52 | 5 / 3 | 1,232 | satellite ×2 (P2 원형) |
| plain_medium_e | 43×42×33 | 4,660 | 639 | 59 | 3 / 2 | 976 | satellite ×2 |
| plain_small_a | 25×23×25 | 1,702 | 318 | 0 | 1 / 1 | 485 | — |
| plain_small_b | 27×24×23 | 1,871 | 315 | 0 | 1 / 1 | 481 | — |
| plain_small_c | 25×23×27 | 1,800 | 347 | 0 | 1 / 1 | 530 | — |
| plain_small_d | 27×24×25 | 2,064 | 348 | 0 | 1 / 1 | 531 | — |
| ruins | 36×27×33 | 4,049 | 506 | 0 | 3 / 2 | 772 | ruins_top, satellite |
| satellite_a | 27×22×19 | 853 | 175 | 0 | 1 / 1 | 267 | satellite ×2 |
| satellite_b | 25×21×21 | 803 | 178 | 0 | 1 / 1 | 272 | satellite ×2 |
| temple | 81×45×57 | 32,897 | 2,997 | 0 | 28 / 11 | 4,575 | temple_top |
| tower | 58×97×49 | 13,490 | 87 | 0 | 1 / 0 | 133 | inner `minecraft:empty` — 구조물 29,056, 기능 블록 sea_lantern 29·vault 8·trial_spawner 16·oxidized_cut_copper 42 |

테마별 권장 거대 꽃 합계: meadow 227, forest 228, snowy 227, desert 225, badlands 226, rocky 152.

### E-1. 테마 역할 (평균 깊이 기준, 변환 슬롯)

| 테마 | 표면 | 하층 | 암반 |
|---|---|---|---|
| meadow | grass_block | dirt, coarse_dirt | stone, andesite, tuff |
| forest | grass_block | dirt, podzol | stone, andesite, mossy_cobblestone |
| snowy | snow_block | (없음) | stone, packed_ice, cobblestone, andesite |
| desert | sand | — | sandstone, smooth/cut sandstone |
| badlands | red_sand | red_sandstone | terracotta 계열 |
| rocky | gravel | — | stone, andesite, cobblestone, tuff |

비-meadow 테마를 천국 팔레트로 바꿀지(=6테마 정체성 상실) 또는 meadow/forest 만 천국화할지는 **승인 사항 F5**.

### E-2. 배치 규칙 (V5 좌표 복사 금지 — 템플릿마다 재계산)

- 거대 꽃: 가장자리 ≥ 5, 연못 ≥ 4, 상호 간격 ≥ 9, 밑동 3×3 식재 가능. 권장 수 = min(가능, round(식재 면적/270)), 면적 150 미만은 0.
- 금지 마스크: 구조물 블록 +2, 내부 직소 반경 6, 측면 직소 반경 4, 입구·통로·계단·기능 블록(vault, trial_spawner) 주변.
- 소형·위성 섬: 거대 꽃 최대 1. 탑: 0 (식재 87기둥, 구조물 우선).
- 식생 밀도: P2 plain_medium_d 비율(식생 1,232 / 식재 807 ≈ 1.53 블록/기둥) 기준.
- 연못·물 주변: 물에 대체되는 비고체 식생 배치 금지(B9).

### E-3. 자원·작업량

- 신규 모델·텍스처: **0** (P2 재사용 구조로 충분)
- NBT 팔레트 증가: 템플릿당 +18 식생 상태 이하
- 작업량: 테마 1개(19개) 자동 생성 + 검사 + 대표 4개 실클라이언트 촬영 ≈ P2 의 3–4배. 6테마(114개)는 그 6배.
- 서식지: 식재로 표면 기둥 감소는 거대 줄기 3×3 미만 점유 → 섬당 1% 미만 예상 (D-4 검사로 확인).

---

## F. 소유자 승인 요청서

각 항목은 독립적으로 결정 가능. 권장안은 ★.

| # | 결정 | 선택지 | 권장 | 장점 | 단점 | 게임플레이 영향 | 난이도 |
|---|---|---|---|---|---|---|---|
| F1 | 천국 잔디 번짐·소멸 (A1·A2·A7) | ① 없음(장식) ② 천국 흙으로만 ★ ③ 바닐라 흙 포함 | ② | 섬 생태 일관, 바닐라 오염 없음 | 클래스 1개·`snowy` 상태 추가 | 플레이어가 천국 잔디 복원 가능 | 중 |
| F2 | 바닐라 식물 생존 (A5) | ① 불가 유지 ② 개별 태그 열거 ★ ③ `#dirt` 편입 | ② | 핑크 꽃잎·묘목 문제 해결, 부작용 통제 | 태그 6–8개 관리 | 하늘섬에서 바닐라 꽃·나무 재배 | 하 |
| F3 | 삽 길·괭이 경작 (A3·A4) | ① 불가 ② 바닐라 길·경작지로 ★ ③ 천국 전용 블록 | ② | 데이터 태그만 | 길·경작지가 바닐라 색 | 농사 가능 | 하(②) / 상(③) |
| F4 | 천국 잔디 뼛가루 (A6) | ① 없음 ② 천국 식생 배치 ★ ③ 바닐라 꽃 배치 | ② | 천국 식생 재생 수단 | B5 드롭 정책과 연동 | 식생 확산 | 중 |
| F5 | P3 적용 테마 범위 | ① meadow 19개 ★ ② meadow+forest 38개 ③ 6테마 114개 | ① | 검증 범위 작음, 형태 보존 | 다른 테마는 바닐라 유지 | 천국은 초원 섬 한정 | ①하 ②중 ③상 |
| F6 | 거대 꽃 파괴 (B2) | A 독립 / B 전체 제거 ★ / C 단계 붕괴 | B | 직관적, 부유물 없음, ≤121 블록 유한 | 드롭 규칙 필요 | 거대 꽃 채집 방식 | 중 |
| F7 | 식생 드롭·아이템 (B5·B6) | ① 없음 ② 소형 자기 자신 + 거대 소량 ★ ③ 전부 자기 자신 | ② | 수집·장식 가능, 폭증 방지 | 아이템 18종 이하 신규 등록 → **ID 확정 필수(F9)** | 장식 블록 획득 | 중 |
| F8 | 식생 불·지지 (B9·B10) | 불: 불연 ★ / 가연. 지지: 바닐라처럼 사라짐 ★ / 현행 | 불연·사라짐 | 천국 분위기, 바닐라 직관 | — | 화재 피해 없음 | 하 |
| F9 | 블록 ID 확정 (C) | 후보 1 현행 ★ / 2 바닐라식 / 3 celestial | 1 | 변경 비용 0 | — | 없음 (월드젠 전이면) | 하 |
| F10 | 거대 꽃 수 (E) | 권장 공식(대형 23–56) ★ / 대형 상한 20 / 사용자 지정 | 공식 | 밀도 일관 | 대형 섬 서버 부하·시야 혼잡 가능 | 탐험 경관 | 하 |
| F11 | 천국 석재 드롭 (A9) | 자기 자신 ★ / 천국 조약돌 신규 | 자기 자신 | 자원 0 | 바닐라(돌→조약돌)와 다름 | 건축재 즉시 | 하 |

승인 전까지 위 항목은 모두 **미결정**이며 코드에 반영하지 않았다.

---

## 검증 · 기술 확인 (P2.5-F)

| 항목 | 방법 | 결과 |
|---|---|---|
| 정책 특성 프로브 | `src/gametest/.../HeavenP25PolicyProbes.java` (테스트 소스셋 전용, 제품 동작 무변경. 바닐라 대조군만 assert) | 5/5 통과, 결과 `data/probe_results.txt` |
| 생존·도구 실측 코드 | 26.3 바이트코드 javap (`VegetationBlock`, `SpreadingSnowyBlock`, `GrassBlock`, `FarmlandBlock`, block_transformer JSON, 태그 JSON) | 위 표에 반영 |
| 연결성·직소·템플릿 | `tools/heaven-p25/analyze.py` (약 5.5분) | `data/*.json` |
| 회귀 | GameTest 월드 삭제 후 `./gradlew test build` | GameTest 726/726, JUnit 791/791, BUILD SUCCESSFUL |

금지 사항 준수: 19 템플릿·worldgen·구조물 ID·P1/P2 프로토타입 무변경, PR·병합 없음.

## 미해결 문제

1. ruins·temple 의 구조물 블록이 분석상 0으로 집계됨 (석재 벽돌 등이 지형 팔레트로 분류) → P3 에서 구조물 마스크를 테마별 화이트리스트로 보정 필요.
2. 거대 줄기·흰 꽃잎 블록이 solid → `SkyIslandHabitat` 스폰 후보 포함 가능 (D-4).
3. `cherry_leaves` 재사용분의 `persistent` 유지를 P3 생성기에서 강제해야 함.
4. 천국 식생은 생존 규칙이 없어 현재 석재·공중에서도 유지됨 (F2/F8 승인 후 처리).
5. 비-meadow 테마 천국화 여부(F5)에 따라 P3 작업량이 1–6배로 달라짐.

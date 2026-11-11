# QUEST 200 AUDIT MATRIX

기준: PR #112 @ `ee37afe`. A 문서에서 직접 파싱(200행). 증거 수준: VERIFIED=소스/바이트코드 확인, INFERRED=API 존재로 추정, UNKNOWN=미확인. **NOW\*** = 콘텐츠는 서바이벌 달성 가능하나 결함(D-xx) 수정 전 등록 불가.

공통 규칙(전 행 적용): 귀속 = 행위를 한 서버 플레이어(킬은 `getKillCredit`, 전리품은 개봉 플레이어). 진행도 = `quest.progress.<id>` 단조 증가, 완료 = 목표 도달 틱에 `quest.complete`. 중복 = `quest.claimed` 1회. 필요한 테스트 = 각 행 Unit(조건 판정) + GameTest(트리거 1회·완료 후 재트리거 무변화). 이하 표는 행별 차이만 기록.

| # | ID | 명칭 | 현재 | XP | 서버 판정 근거 | 증거 | 소급 | 악용/멀티 | 결함 | 권장 | 비고 |
|---:|---|---|---|---:|---|---|---|---|---|---|---|
| 1 | `xpq.exp.first_structure_loot` | 첫 보물상자 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 2 | `xpq.exp.mineshaft_loot` | 폐광의 흔적 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 3 | `xpq.exp.dungeon_loot` | 지하 감옥 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 4 | `xpq.exp.walk_10k` | 여행자 | NOW | 250 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 원형 보행(의도) | - | NOW | - |
| 5 | `xpq.exp.temples` | 사원 도굴꾼 | NOW | 350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 6 | `xpq.exp.ocean_relics` | 난파선의 기록 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 7 | `xpq.exp.explorer_maps` | 지도 수집가 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 8 | `xpq.exp.biomes_25` | 대지 순례 | NOW | 500 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 9 | `xpq.exp.rare_biomes` | 희귀 경관 | NOW | 600 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 10 | `xpq.exp.villages_5` | 다섯 마을 | NOW | 550 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 11 | `xpq.exp.height_extremes` | 하늘과 심연 | NOW | 400 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 12 | `xpq.exp.archaeology_30` | 발굴 조사단 | NOW | 500 | Knowledge 카운터 `condition.*` 재사용 불가(archaeology_items_brushed≤10). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 13 | `xpq.exp.sleeping_bag_biomes` | 노숙의 달인 | VERIFY | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 14 | `xpq.exp.parachute_landing` | 무사 착지 | VERIFY | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 15 | `xpq.exp.walk_50k` | 대륙 횡단 | NOW | 1,150 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 동일 | - | NOW | - |
| 16 | `xpq.exp.biomes_40` | 세계 지리학자 | NOW | 1,150 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 17 | `xpq.exp.structures_15` | 원정대장 | NOW | 1,150 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 18 | `xpq.exp.grand_expedition` | 대원정 | NOW | 1,450 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 19 | `xpq.exp.sherds_15` | 파편의 역사 | NOW | 1,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 20 | `xpq.exp.sail_20k` | 대양 항해사 | NOW | 1,150 | Knowledge 카운터는 상한, 바닐라 Stats.CUSTOM.BOAT_ONE_CM 직접 조회로 대체 | VERIFIED | 가능(바닐라 통계) | 특이사항 없음 | D-02 | NOW* | 결함 수정 후 등록 |
| 21 | `xpq.exp.ride_10k` | 기수 | NOW | 1,000 | Knowledge 카운터는 상한, 바닐라 Stats.CUSTOM.HORSE_ONE_CM 직접 조회로 대체 | VERIFIED | 가능(바닐라 통계) | 특이사항 없음 | D-02 | NOW* | 결함 수정 후 등록 |
| 22 | `xpq.exp.elytra_25k` | 창공 | NOW | 1,100 | Knowledge 카운터는 상한, 바닐라 Stats.CUSTOM.AVIATE_ONE_CM 직접 조회로 대체 | VERIFIED | 가능(바닐라 통계) | 특이사항 없음 | D-02 | NOW* | 결함 수정 후 등록 |
| 23 | `xpq.exp.travel_250k` | 세계의 끝까지 | NOW | 3,150 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 얼음 보트 고속 이동 | - | NOW | - |
| 24 | `xpq.exp.biomes_all_overworld` | 완전한 지도 | NOW | 3,150 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-06 | VERIFY | 대상 목록 미정의 |
| 25 | `xpq.dim.fortress_loot` | 요새 약탈 | NOW | 350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 26 | `xpq.dim.blaze_rods_32` | 불꽃 막대 비축 | NOW | 400 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 27 | `xpq.dim.trial_vaults_5` | 시련 도전자 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-13 | NOW* | 결함 수정 후 등록 |
| 28 | `xpq.dim.ancient_city_loot` | 고대 도시의 유물 | NOW | 900 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09, D-13 | NOW* | 결함 수정 후 등록 |
| 29 | `xpq.dim.bastion_all` | 보루 정복 | NOW | 1,350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09 | NOW* | 결함 수정 후 등록 |
| 30 | `xpq.dim.ominous_vaults_5` | 불길한 시련 | NOW | 1,350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-13 | NOW* | 결함 수정 후 등록 |
| 31 | `xpq.dim.end_cities_3` | 엔드 도시 순례 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-13 | NOW* | 결함 수정 후 등록 |
| 32 | `xpq.dim.shulker_boxes` | 공허의 상자 | NOW | 1,000 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 33 | `xpq.dim.end_city_loot_10` | 엔드의 약탈자 | NOW | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-09, D-13 | NOW* | 결함 수정 후 등록 |
| 34 | `xpq.dim.nether_travel_50k` | 네더 회랑 | NOW | 2,500 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 네더 얼음 보트 | D-05 | NOW* | 신규 차원별 이동 추적 명세 후 |
| 35 | `xpq.cmb.hostile_100` | 파수꾼 | NOW | 250 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 36 | `xpq.cmb.mm_arms` | 새로운 무기 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-18 | NOW* | 결함 수정 후 등록 |
| 37 | `xpq.cmb.bestiary_5` | 현지 조사 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-15 | NOW* | 결함 수정 후 등록 |
| 38 | `xpq.cmb.shield_500` | 철벽 | NOW | 450 | Knowledge 카운터 `condition.*` 재사용 불가(shield_blocks≤50). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 39 | `xpq.cmb.hostile_500` | 숙련 전사 | NOW | 500 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 몹팜 | - | NOW | - |
| 40 | `xpq.cmb.vanilla_species_20` | 전장의 다양성 | NOW | 500 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 없음 | - | NOW | - |
| 41 | `xpq.cmb.dagger_100` | 그림자 칼날 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-18 | NOW* | 결함 수정 후 등록 |
| 42 | `xpq.cmb.greatsword_100` | 거인의 일격 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-18 | NOW* | 결함 수정 후 등록 |
| 43 | `xpq.cmb.bestiary_10` | 현장 도감 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-15 | NOW* | 결함 수정 후 등록 |
| 44 | `xpq.cmb.bandits_20` | 산적 토벌 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 45 | `xpq.cmb.cave_dwellers` | 동굴 소탕 | VERIFY | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-12 | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 46 | `xpq.cmb.trident_50` | 바다의 창 | NOW | 450 | Knowledge 카운터 `condition.*` 재사용 불가(trident_hostile_kills≤10). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 47 | `xpq.cmb.breeze_10` | 바람 사냥 | NOW | 450 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 48 | `xpq.cmb.guardians_30` | 해저 파수꾼 | NOW | 450 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 49 | `xpq.cmb.long_shot_25` | 명사수 | NOW | 850 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 50 | `xpq.cmb.mace_smash_10` | 낙하 강타 | NOW | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 51 | `xpq.cmb.lesser_wither_10` | 시든 하수인 | NOW | 700 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 52 | `xpq.cmb.hostile_2000` | 백전노장 | NOW | 1,100 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 몹팜 | - | NOW | - |
| 53 | `xpq.cmb.bestiary_25` | 완전 도감 | VERIFY | 1,350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-15 | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 54 | `xpq.cmb.habitats_5` | 서식지 사냥꾼 | VERIFY | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-15 | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 55 | `xpq.cmb.netherite_blades` | 검은 강철 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-18 | NOW* | 결함 수정 후 등록 |
| 56 | `xpq.cmb.ravagers_5` | 파괴수 사냥 | NOW | 1,000 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 57 | `xpq.cmb.brutes_3` | 돌격대 섬멸 | NOW | 1,000 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 58 | `xpq.cmb.wither_skulls_3` | 세 개의 해골 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 59 | `xpq.cmb.raids_3` | 마을의 수호자 | NOW | 1,350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 60 | `xpq.cmb.hostile_5000` | 전설의 전사 | NOW | 1,900 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 몹팜 | - | NOW | - |
| 61 | `xpq.cmb.tactical_ammo` | 전술 사격 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 62 | `xpq.cmb.gauntlet_50` | 건틀릿 격투 | FUTURE | 750 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 63 | `xpq.cmb.elemental_100` | 원소 전사 | FUTURE | 1,100 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 64 | `xpq.cmb.elemental_skills` | 원소 숙련 | FUTURE | 1,250 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 65 | `xpq.boss.elder_guardian` | 심해의 장로 | NOW | 750 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 66 | `xpq.boss.monument_clear` | 해저 신전 공략 | NOW | 1,450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-08 | VERIFY | 귀속 메커니즘 부재 |
| 67 | `xpq.boss.warden` | 심연의 감시자 | NOW | 1,700 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 68 | `xpq.boss.wither` | 시듦의 종언 | NOW | 1,450 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 69 | `xpq.boss.twin_bosses` | 두 개의 왕관 | NOW | 3,150 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 70 | `xpq.boss.dragon_rematch` | 재림한 용 | NOW | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-14 | NOW* | 결함 수정 후 등록 |
| 71 | `xpq.boss.omen_raid` | 불길한 전쟁 | NOW | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-19 | NOW* | 결함 수정 후 등록 |
| 72 | `xpq.boss.monuments_3` | 세 신전의 정복자 | NOW | 3,450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-08 | VERIFY | 귀속 메커니즘 부재 |
| 73 | `xpq.boss.warden_3` | 침묵의 사냥꾼 | NOW | 3,150 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 특이사항 없음 | - | NOW | - |
| 74 | `xpq.min.first_custom_ore` | 낯선 광석 | NOW | 250 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 75 | `xpq.min.custom_ore_survey` | 심층 탐사 | NOW | 350 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 76 | `xpq.min.obsidian_64` | 흑요석 채석장 | NOW | 300 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 흑요석 설치·재채굴 | D-03 | NOW* | 결함 수정 후 등록 |
| 77 | `xpq.min.ore_1000` | 광부 | NOW | 500 | Knowledge 카운터 `condition.*` 재사용 불가(ore_items_mined≤256). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 실크터치 광석 설치·재채굴(설치 광석 미구분) | D-01, D-03 | NOW* | 결함 수정 후 등록 |
| 78 | `xpq.min.deepslate_custom` | 심층암 광맥 | NOW | 500 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 79 | `xpq.min.stone_10000` | 채석장 주인 | NOW | 500 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 조약돌 설치·재채굴 | D-03 | NOW* | 결함 수정 후 등록 |
| 80 | `xpq.min.logs_1000` | 벌목꾼 | NOW | 500 | Knowledge 카운터 `condition.*` 재사용 불가(natural_vanilla_logs_mined≤200). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 자연 원목 마커 덕분에 설치 원목 재채굴 불가(K5 provenance) — 단, 소급 불가 | D-01 | NOW* | 결함 수정 후 등록 |
| 81 | `xpq.min.amethyst_spyglass` | 수정 동굴 | NOW | 400 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 82 | `xpq.min.adamantium_debris` | 황무지 아래 | NOW | 750 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 83 | `xpq.min.all_wood` | 수목원 | NOW | 750 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 84 | `xpq.min.pickaxe_5000` | 곡괭이의 생애 | NOW | 700 | Knowledge 카운터 `condition.*` 재사용 불가(pickaxe_durability_used≤500). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 반복 채굴(의도된 반복) | D-01 | NOW* | 결함 수정 후 등록 |
| 85 | `xpq.min.vein_500` | 광맥 연쇄 | VERIFY | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 86 | `xpq.min.lumberjack_200` | 한 번에 쓰러뜨리기 | VERIFY | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 87 | `xpq.min.ore_5000` | 대광부 | NOW | 1,100 | Knowledge 카운터 `condition.*` 재사용 불가(ore_items_mined≤256). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 동일 | D-01, D-03 | NOW* | 결함 수정 후 등록 |
| 88 | `xpq.min.diamond_ore_64` | 다이아몬드 광맥 | NOW | 1,100 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | BLOCK_MINED: 실크터치 다이아 광석 반복 | D-03 | NOW* | 결함 수정 후 등록 |
| 89 | `xpq.min.adamantium_10` | 아다만티움 채굴단 | NOW | 1,250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 잔해 설치·재채굴 | D-03 | NOW* | 결함 수정 후 등록 |
| 90 | `xpq.min.debris_16` | 고대의 잔해 | NOW | 1,100 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 고대 잔해는 항상 자기 자신 드롭 → 설치·재채굴 무한 | D-03 | NOW* | 결함 수정 후 등록 |
| 91 | `xpq.min.ore_20000` | 심연의 광부 | NOW | 3,150 | Knowledge 카운터 `condition.*` 재사용 불가(ore_items_mined≤256). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 동일 | D-01, D-03 | NOW* | 결함 수정 후 등록 |
| 92 | `xpq.min.drill_1000` | 굴착기 | FUTURE | 1,100 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 93 | `xpq.frm.harvest_all_crops` | 풍성한 수확 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 94 | `xpq.frm.composite_foods` | 요리사 | NOW | 200 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 95 | `xpq.frm.tame_5` | 동물의 친구 | NOW | 350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | D-07 | NOW* | 결함 수정 후 등록 |
| 96 | `xpq.frm.harvest_1000` | 농부 | NOW | 500 | Knowledge 카운터 `condition.*` 재사용 불가(crops_harvested≤100). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 97 | `xpq.frm.till_1000` | 개간 | NOW | 400 | Knowledge 카운터 `condition.*` 재사용 불가(farmland_tilled≤100). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 경작지→흙 복원 후 재경작 | D-01 | NOW* | 결함 수정 후 등록 |
| 98 | `xpq.frm.breed_200` | 목장주 | NOW | 500 | Knowledge 카운터는 상한, 바닐라 Stats.CUSTOM.ANIMALS_BRED 직접 조회로 대체 | VERIFIED | 가능(바닐라 통계) | 번식 반복(의도) | D-02 | NOW* | 결함 수정 후 등록 |
| 99 | `xpq.frm.honey_32` | 양봉가 | NOW | 450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 100 | `xpq.frm.master_trade` | 대가와의 거래 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 101 | `xpq.frm.mending_book` | 수선의 비밀 | NOW | 450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 102 | `xpq.frm.ancient_harvest` | 고대 식물원 | NOW | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 103 | `xpq.frm.professions_10` | 마을 경제 | NOW | 850 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 104 | `xpq.frm.villager_discount_20` | 단골 손님 | VERIFY | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 105 | `xpq.frm.harvest_5000` | 대농장 | NOW | 1,100 | Knowledge 카운터 `condition.*` 재사용 불가(crops_harvested≤100). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 자동 농장 없이도 수확 반복(의도) | D-01 | NOW* | 결함 수정 후 등록 |
| 106 | `xpq.frm.trades_200` | 상인 연합 | NOW | 1,100 | Knowledge 카운터는 상한, 바닐라 Stats.CUSTOM.TRADED_WITH_VILLAGER 직접 조회로 대체 | VERIFIED | 가능(바닐라 통계) | 동일 주민 반복 거래(의도된 반복) | D-02 | NOW* | 결함 수정 후 등록 |
| 107 | `xpq.frm.barter_200` | 금의 외교 | NOW | 1,100 | Knowledge 카운터 `condition.*` 재사용 불가(piglin_barters≤20). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 금괴 투척 자동화 | D-01 | NOW* | 결함 수정 후 등록 |
| 108 | `xpq.frm.soil_study_harvest` | 토양학자 | VERIFY | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 109 | `xpq.frm.first_capture` | 포획 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 110 | `xpq.frm.auto_farm` | 자동 농장 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 111 | `xpq.frm.capture_10` | 이동 동물원 | FUTURE | 1,100 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 112 | `xpq.fsh.first_sale` | 장날 | NOW | 250 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 113 | `xpq.fsh.freshwater_4` | 민물 낚시꾼 | NOW | 250 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 114 | `xpq.fsh.first_module` | 첫 채비 | NOW | 250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 115 | `xpq.fsh.grade_four` | 상급 어획 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 116 | `xpq.fsh.sale_64` | 어시장 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 의도 | - | NOW | - |
| 117 | `xpq.fsh.tier3_rig` | 명인의 채비 | NOW | 500 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 118 | `xpq.fsh.water_biomes_8` | 물길 탐사 | NOW | 500 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 119 | `xpq.fsh.multi_catch` | 두 마리 한 번에 | NOW | 450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 120 | `xpq.fsh.grade_five` | 완벽한 어획 | NOW | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 121 | `xpq.fsh.ocean_trio` | 원양 어업 | NOW | 850 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 122 | `xpq.fsh.catch_500` | 어업 조합장 | NOW | 1,100 | Knowledge 카운터 `condition.*` 재사용 불가(fishing_successes≤100). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 123 | `xpq.fsh.full_catalogue` | 어류 대도감 | NOW | 1,350 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 124 | `xpq.fsh.emerald_500` | 바다의 상인 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 의도 | - | NOW | - |
| 125 | `xpq.fsh.giant_catch` | 전설의 월척 | VERIFY | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 126 | `xpq.fsh.grade_five_5` | 명장 | NOW | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 127 | `xpq.fsh.fish_trap` | 통발 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 128 | `xpq.cft.sleeping_bag` | 야영 준비 | NOW | 150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 129 | `xpq.cft.enchanting_30` | 깊은 지혜 | NOW | 350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 130 | `xpq.cft.smelt_1000` | 용광로 장인 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 자동 화로(호퍼) 산출 수거 | - | NOW | - |
| 131 | `xpq.cft.all_daggers` | 단검 수집가 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 132 | `xpq.cft.anvil_50` | 대장장이 | NOW | 450 | Knowledge 카운터 `condition.*` 재사용 불가(anvil_repairs≤10). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 133 | `xpq.cft.redstone_100` | 공학자 | NOW | 450 | Knowledge 카운터 `condition.*` 재사용 불가(redstone_components_placed≤20). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 134 | `xpq.cft.parachute` | 하늘의 안전장치 | NOW | 400 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 135 | `xpq.cft.conduit` | 바다의 심장 | NOW | 850 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 136 | `xpq.cft.recipes_200` | 만능 장인 | NOW | 750 | Knowledge 카운터 `condition.*` 재사용 불가(recipes_completed≤50 (또한 횟수형이라 "서로 다른 200종"과 의미 불일치)). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01, D-11 | NOW* | 결함 수정 후 등록 |
| 137 | `xpq.cft.mm_metallurgy` | 다섯 금속 | NOW | 1,350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 138 | `xpq.cft.all_greatswords` | 대검 수집가 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 139 | `xpq.cft.netherite_tools` | 검은 공구함 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 140 | `xpq.cft.mm_blocks` | 금속 보관고 | NOW | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 141 | `xpq.cft.backpack_large` | 짐꾼 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 142 | `xpq.cft.portable_furnace` | 휴대 제련 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 143 | `xpq.bld.place_1000` | 첫 집 | NOW | 250 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 설치·파괴 반복 | D-04 | NOW* | 결함 수정 후 등록 |
| 144 | `xpq.bld.variety_50` | 건축 재료상 | NOW | 350 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 저가 블록 1개씩 | - | NOW | - |
| 145 | `xpq.bld.place_10000` | 건축가 | NOW | 500 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 설치·파괴 반복 | D-04 | NOW* | 결함 수정 후 등록 |
| 146 | `xpq.bld.storage_50` | 창고 관리인 | NOW | 400 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 147 | `xpq.bld.copper_ages` | 청동기 시대 | NOW | 450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 148 | `xpq.bld.variety_150` | 건축 백과 | NOW | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 동일 | - | NOW | - |
| 149 | `xpq.bld.place_50000` | 도시 계획가 | NOW | 1,100 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 설치·파괴 반복 | D-04 | NOW* | 결함 수정 후 등록 |
| 150 | `xpq.bld.place_200000` | 세계 건설자 | NOW | 3,150 | 바닐라 Stats(ENTITY_KILLED/BLOCK_MINED/ITEM_USED/CUSTOM) 합산 | VERIFIED(통계 존재) | 가능 | 설치·파괴 반복(무의미 반복으로 엔드게임 보상) | D-04 | NOW* | 결함 수정 후 등록 |
| 151 | `xpq.alc.shock_potion` | 병 속의 번개 | VERIFY | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | 26.3 BrewingRecipe + recipe/brewing/shock.json 확인 |
| 152 | `xpq.alc.types_12` | 물약 연구가 | NOW | 500 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 153 | `xpq.alc.splash_lingering` | 투척 연금술 | NOW | 500 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 154 | `xpq.alc.ingredients_all` | 재료 감정사 | NOW | 450 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | D-16 | NOW* | 결함 수정 후 등록 |
| 155 | `xpq.alc.effects_16` | 체험주의자 | NOW | 750 | Knowledge 고유 관측 키 `condition.<cond>.seen.<id>`(상한 없음, recordDistinctCondition) 읽기 | VERIFIED | 가능(관측 키 존재 시) | 특이사항 없음 | - | NOW | - |
| 156 | `xpq.alc.enhanced_3` | 정제와 연장 | NOW | 750 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | NOW | - |
| 157 | `xpq.alc.shock_10` | 전기 충격 | VERIFY | 700 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 158 | `xpq.alc.potions_250` | 대연금술사 | NOW | 1,100 | Knowledge 카운터 `condition.*` 재사용 불가(potions_brewed≤50). 신규 무상한 퀘스트 카운터 필요 | VERIFIED(결함) | 불가(상한 이상 이력 없음) | 특이사항 없음 | D-01 | NOW* | 결함 수정 후 등록 |
| 159 | `xpq.alc.injector` | 주사기 | FUTURE | 750 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 160 | `xpq.eqp.enhance_5` | 첫 이정표 | NOW | 250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 161 | `xpq.eqp.custom_enchant` | 비전 공예 | VERIFY | 350 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | EnchantmentTrees에 11종 모두 노드 존재(INFERRED) |
| 162 | `xpq.eqp.enhance_10` | 단련 | NOW | 500 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 163 | `xpq.eqp.enhance_trio` | 삼위 단련 | NOW | 850 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 164 | `xpq.eqp.enhance_15` | 숙련의 경지 | NOW | 750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 165 | `xpq.eqp.custom_enchant_5` | 비전 수집가 | VERIFY | 1,100 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 166 | `xpq.eqp.enhance_20` | 완성 | NOW | 1,250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 167 | `xpq.eqp.first_t2_core` | 원소의 심장 | NOW | 1,250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 168 | `xpq.eqp.t2_cores_3` | 세 개의 심장 | NOW | 1,250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 169 | `xpq.eqp.enhance_20_armor` | 불멸의 갑주 | NOW | 3,750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-17 | NOW* | 결함 수정 후 등록 |
| 170 | `xpq.eqp.custom_enchant_all` | 비전의 정점 | VERIFY | 3,150 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 171 | `xpq.eqp.all_t2_cores` | 다섯 심장 | NOW | 3,750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 172 | `xpq.eqp.accessory_first` | 첫 장신구 | FUTURE | 500 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 173 | `xpq.eqp.first_t2_weapon` | 원소의 무기 | FUTURE | 1,350 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 174 | `xpq.eqp.accessory_full` | 다섯 슬롯 | FUTURE | 1,250 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 175 | `xpq.eqp.t3_special` | 특수 장비 | FUTURE | 1,350 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 176 | `xpq.eqp.t2_arsenal` | 원소 무기고 | FUTURE | 4,050 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 177 | `xpq.eqp.t2_plus20` | 원소의 완성 | FUTURE | 3,750 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 178 | `xpq.eqp.accessory_all` | 유물 수집가 | FUTURE | 3,750 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 179 | `xpq.eqp.t3_set` | 특수 장비 수집가 | FUTURE | 4,050 | 획득 경로 구현 후 정의 | VERIFIED(경로 부재) | - | 특이사항 없음 | - | FUTURE | 생존 획득 경로 없음(레시피·루트 부재 확인) |
| 180 | `xpq.grw.knowledge_10` | 학도 | NOW | 250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 181 | `xpq.grw.first_research` | 첫 실험 | NOW | 250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 182 | `xpq.grw.seven_roots` | 일곱 기초 | NOW | 350 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 183 | `xpq.grw.knowledge_20` | 탐구자 | NOW | 350 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 184 | `xpq.grw.research_10` | 연구원 | NOW | 500 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 185 | `xpq.grw.knowledge_35` | 학자 | NOW | 500 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 186 | `xpq.grw.first_t2` | 전문 연구 | NOW | 500 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 187 | `xpq.grw.climbing_100` | 등반가 | VERIFY | 450 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 188 | `xpq.grw.discipline_breadth` | 박학다식 | NOW | 850 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 189 | `xpq.grw.spent_10k` | 투자자 | NOW | 750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-10 | NOW* | 결함 수정 후 등록 |
| 190 | `xpq.grw.wall_jump_50` | 벽차기 | VERIFY | 700 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 191 | `xpq.grw.fall_roll_20` | 낙법 | VERIFY | 700 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 192 | `xpq.grw.throwable_pickaxe` | 투척 곡괭이 | VERIFY | 700 | 신규 서버 이벤트 훅 필요(설치 이후) | INFERRED | 불가 | 특이사항 없음 | - | VERIFY | 메커니즘/감지 검증 전 미등록 |
| 193 | `xpq.grw.knowledge_50` | 석학 | NOW | 1,100 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 194 | `xpq.grw.research_25` | 수석 연구원 | NOW | 1,100 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 195 | `xpq.grw.first_t3` | 돌파구 | NOW | 1,250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 196 | `xpq.grw.spent_50k` | 후원자 | NOW | 1,250 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | D-10 | NOW* | 결함 수정 후 등록 |
| 197 | `xpq.grw.knowledge_complete` | 현자 | NOW | 3,750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 198 | `xpq.grw.research_50` | 연구소장 | NOW | 3,450 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 199 | `xpq.grw.t3_5` | 최첨단 | NOW | 3,150 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |
| 200 | `xpq.grw.discipline_complete` | 분야의 대가 | NOW | 3,750 | 연구/Knowledge/강화 상태 질의(ResearchProgressState, ENHANCEMENT_LEVEL) | INFERRED | 가능 | 특이사항 없음 | - | NOW | - |

## 집계

검사 행 수: **200** (누락·중복 0)

| 권장 분류 | 개수 | XP |
|---|---:|---:|
| NOW | 98 | 95,300 |
| NOW* | 62 | 60,750 |
| VERIFY | 20 | 23,950 |
| FUTURE | 20 | 29,100 |
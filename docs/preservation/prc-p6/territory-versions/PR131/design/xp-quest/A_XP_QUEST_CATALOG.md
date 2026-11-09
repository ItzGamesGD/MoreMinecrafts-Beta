# A. XP Quest 확장 카탈로그 (제안, 2026-10-17)

상태: **제안 — 권한 문서 아님, 미구현.** **기준 시나리오 D(활성 퀘스트 합계 ≈180,000XP, 사용자 확정 2026-10-17)**. 보상 = 단계 기준값 × 개별 가중치 × 정규화 계수(1단위=223XP), 50 단위 반올림 후 활성 합계를 정확히 180,000으로 맞추기 위해 반올림 손실이 큰 순서(동률 시 카탈로그 순서)로 +50씩 최대잉여 재분배. FUTURE 보상은 추가분(삭감 없음).

단계 가중 곡선(단위): 초반 1, 초중반 1.6, 중반 2.3, 중후반 3.4, 후반 5, 엔드게임 14 — 같은 곡선을 시나리오 총량에 맞춰 정규화한다. 표에는 기준 D(180K)와 참고용 B(130K) 값을 함께 표기.

2026-10-17 최종 설계 통합 반영: 감사(`audit/`) 결함 정정과 소유자 확정 정책 6개를 조건·추적·상태에 적용했고 개별 XP는 변경하지 않음. 퀘스트별 구현 명세는 `XP_QUEST_IMPLEMENTATION_HANDOFF.md`.

범례 — 추적: **R** 신뢰 가능한 영속 증거로 완전 소급 · **R부분** Knowledge 상한 카운터 값까지만 소급(그 이상 이력은 없음) · **S** 바닐라 통계 소급(크리에이티브 누적분 포함 위험, D-통계) · **I** 퀘스트 설치 이후 추적. 상태: **NOW** 현 생존 달성 가능 + 서버 판정 명세 완료(구현·테스트 후 등록) · **VERIFY** 메커니즘은 있으나 생존 획득·감지 검증 필요(검증 전 등록 보류) · **FUTURE** SURVIVAL-LOCKED, 획득 경로 구현 전 등록 금지. 선행: 시각적 연결만 표기, **실제 잠금 선행 조건은 전 퀘스트 없음**.

## 1. 요약

- 전체 퀘스트: **200** (활성 NOW+VERIFY 180, FUTURE 20)
- NOW 160개 = 156,050XP · VERIFY 20개 = 23,950XP · **활성 합계 180,000XP** · FUTURE 합계 29,100XP · 전체 209,100XP
- 초기 제안(28개/17,620XP) 대비: 수량 ×7.1, 활성 보상 ×10.2

### 탭별

| 탭 | NOW | VERIFY | FUTURE | 활성 XP | FUTURE XP |
|---|---:|---:|---:|---:|---:|
| 탐험 | 21 | 3 | 0 | 21,500 | 0 |
| 차원·구조물 | 10 | 0 | 0 | 12,600 | 0 |
| 전투 | 23 | 3 | 4 | 19,350 | 3,600 |
| 보스 | 7 | 2 | 0 | 21,400 | 0 |
| 채굴·자원 | 16 | 2 | 1 | 14,700 | 1,100 |
| 농업·생태·교역 | 14 | 2 | 3 | 10,350 | 2,100 |
| 낚시 | 14 | 1 | 1 | 12,600 | 500 |
| 제작·생산 | 13 | 0 | 2 | 11,100 | 1,000 |
| 건축 | 8 | 0 | 0 | 7,300 | 0 |
| 연금 | 7 | 1 | 1 | 5,250 | 750 |
| 장비 | 10 | 2 | 8 | 18,200 | 20,050 |
| 성장(Knowledge·연구) | 17 | 4 | 0 | 25,650 | 0 |

### 진행 단계별 (활성 퀘스트)

| 단계 | 개수 | XP | 누적 XP | 누적 비율 |
|---|---:|---:|---:|---:|
| 초반 | 18 | 4,350 | 4,350 | 2% |
| 초중반 | 10 | 3,450 | 7,800 | 4% |
| 중반 | 55 | 26,450 | 34,250 | 19% |
| 중후반 | 27 | 20,700 | 54,950 | 31% |
| 후반 | 49 | 57,800 | 112,750 | 63% |
| 엔드게임 | 21 | 67,250 | 180,000 | 100% |

### 탭 × 단계 분포 (활성)

| 탭 | 초반 | 초중반 | 중반 | 중후반 | 후반 | 엔드게임 |
|---|---:|---:|---:|---:|---:|---:|
| 탐험 | 4 | 1 | 9 | 0 | 8 | 2 |
| 차원·구조물 | 0 | 1 | 2 | 1 | 4 | 2 |
| 전투 | 3 | 0 | 11 | 3 | 8 | 1 |
| 보스 | 0 | 0 | 0 | 1 | 3 | 5 |
| 채굴·자원 | 1 | 2 | 5 | 5 | 4 | 1 |
| 농업·생태·교역 | 2 | 1 | 6 | 3 | 4 | 0 |
| 낚시 | 3 | 0 | 5 | 2 | 4 | 1 |
| 제작·생산 | 1 | 1 | 5 | 2 | 3 | 1 |
| 건축 | 1 | 1 | 3 | 0 | 2 | 1 |
| 연금 | 0 | 0 | 4 | 3 | 1 | 0 |
| 장비 | 1 | 1 | 1 | 2 | 4 | 3 |
| 성장(Knowledge·연구) | 2 | 2 | 4 | 5 | 4 | 4 |

## 2. 전체 카탈로그

### 탐험 (`exploration`) — 24개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.exp.first_structure_loot` | 첫 보물상자 | 초반 | 생성 구조물 전리품 상자(loot table 경로 chests/*)를 처음 열어 전리품 생성 | 250 | 150 | - | I | 없음 | NOW |
| `xpq.exp.mineshaft_loot` | 폐광의 흔적 | 초반 | chests/abandoned_mineshaft 상자 전리품 생성 1회 | 250 | 150 | first_structure_loot | I | 없음 | NOW |
| `xpq.exp.dungeon_loot` | 지하 감옥 | 초반 | chests/simple_dungeon 상자 전리품 생성 1회 | 250 | 150 | first_structure_loot | I | 없음 | NOW |
| `xpq.exp.walk_10k` | 여행자 | 초반 | 통계 WALK+SPRINT+CROUCH 합계 10,000블록 | 250 | 150 | - | S | 없음 | NOW |
| `xpq.exp.temples` | 사원 도굴꾼 | 초중반 | chests/desert_pyramid 와 chests/jungle_temple 전리품 각 1회 | 350 | 250 | dungeon_loot | I | 없음(salvage_sherd는 고고학 경로) | NOW |
| `xpq.exp.ocean_relics` | 난파선의 기록 | 중반 | shipwreck(supply/map/treasure 중 1), underwater_ruin(1), buried_treasure 전리품 각 1회 | 500 | 350 | temples | I | 없음 | NOW |
| `xpq.exp.explorer_maps` | 지도 수집가 | 중반 | 바다·삼림·시련 탐험 지도 3종 각각 인벤토리 획득 | 500 | 350 | ocean_relics | I | 없음 | NOW |
| `xpq.exp.biomes_25` | 대지 순례 | 중반 | 서로 다른 생물군계 25종 방문 | 500 | 350 | walk_10k | R | 수량증가: K53(15) | NOW |
| `xpq.exp.rare_biomes` | 희귀 경관 | 중반 | mushroom_fields, ice_spikes, eroded_badlands, bamboo_jungle, cherry_grove 5종 방문 | 600 | 450 | biomes_25 | R | 부분: adventuring_time(56 전부) | NOW |
| `xpq.exp.villages_5` | 다섯 마을 | 중반 | village_plains/desert/savanna/snowy/taiga 구조물 5종 진입 | 550 | 400 | biomes_25 | R | 부분: K60/K62(임의 구조물 수) | NOW |
| `xpq.exp.height_extremes` | 하늘과 심연 | 중반 | Y≥300 지점과 Y≤-60 지점에 각각 서기(오버월드) | 400 | 300 | walk_10k | I | 부분: fall_from_world_height(낙하 379) | NOW |
| `xpq.exp.archaeology_30` | 발굴 조사단 | 중반 | 고고학 솔질로 아이템 30개 획득 | 500 | 350 | temples | R부분 | 수량증가: K59(10) | NOW |
| `xpq.exp.sleeping_bag_biomes` | 노숙의 달인 | 중반 | 탐험용 침낭으로 서로 다른 생물군계 5곳에서 수면(밤 넘김) | 500 | 350 | walk_10k | I | 부분: sleep_in_bed(침대 1회) + 생물군계 제약 | VERIFY |
| `xpq.exp.parachute_landing` | 무사 착지 | 중반 | 낙하산을 펼친 상태로 60블록 이상 낙하 후 피해 없이 착지 | 500 | 350 | height_extremes | I | 없음(K57은 겉날개 거리) | VERIFY |
| `xpq.exp.walk_50k` | 대륙 횡단 | 후반 | 통계 WALK+SPRINT+CROUCH 합계 50,000블록 | 1,150 | 800 | walk_10k | S | 없음 | NOW |
| `xpq.exp.biomes_40` | 세계 지리학자 | 후반 | 생물군계 40종 방문 | 1,150 | 800 | biomes_25 | R | 수량증가: K53 / 부분: adventuring_time | NOW |
| `xpq.exp.structures_15` | 원정대장 | 후반 | 서로 다른 구조물 15종 진입 | 1,150 | 800 | villages_5 | R | 수량증가: K62(10) | NOW |
| `xpq.exp.grand_expedition` | 대원정 | 후반 | stronghold, ancient_city, monument, mansion, trial_chambers 5곳 진입 | 1,450 | 1,050 | structures_15 | R | 부분: follow_ender_eye, minecraft_trials_edition | NOW |
| `xpq.exp.sherds_15` | 파편의 역사 | 후반 | 서로 다른 도자기 조각 15종 획득 | 1,150 | 800 | archaeology_30 | I | 부분: salvage_sherd(임의 1) | NOW |
| `xpq.exp.sail_20k` | 대양 항해사 | 후반 | 보트 이동 거리 20,000블록 | 1,150 | 800 | ocean_relics | S | 수량증가: K55(5,000) | NOW |
| `xpq.exp.ride_10k` | 기수 | 후반 | 말 이동 거리 10,000블록 | 1,000 | 750 | walk_10k | S | 수량증가: K56(2,000) | NOW |
| `xpq.exp.elytra_25k` | 창공 | 후반 | 겉날개 비행 25,000블록 | 1,100 | 800 | - | S | 수량증가: K57(2,000) | NOW |
| `xpq.exp.travel_250k` | 세계의 끝까지 | 엔드게임 | 모든 이동 통계(걷기·달리기·보트·말·겉날개·헤엄) 합계 250,000블록 | 3,150 | 2,250 | walk_50k | S | 없음 | NOW |
| `xpq.exp.biomes_all_overworld` | 완전한 지도 | 엔드게임 | 고정 목록(26.3 바닐라 자연 생성 생물군계 — 오버월드·네더·엔드, the_void 제외) 전부 방문. 목록 확정 전 미등록 | 3,150 | 2,250 | biomes_40 | R | 부분: adventuring_time(오버월드), explore_nether(네더 5) — 3차원 전부는 신규 | VERIFY |

### 차원·구조물 (`dimension`) — 10개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.dim.fortress_loot` | 요새 약탈 | 초중반 | chests/nether_bridge 전리품 1회 | 350 | 250 | - | I | 부분: find_fortress(진입) | NOW |
| `xpq.dim.blaze_rods_32` | 불꽃 막대 비축 | 중반 | 블레이즈 막대 누적 32개 획득(최초 획득 시점 이후 증가분) | 400 | 300 | fortress_loot | I | 수량증가: obtain_blaze_rod(1) | NOW |
| `xpq.dim.trial_vaults_5` | 시련 도전자 | 중반 | 일반 금고(vault, ominous=false) 서로 다른 5개 개방 | 500 | 350 | - | I | 수량증가: under_lock_and_key(1) | NOW |
| `xpq.dim.ancient_city_loot` | 고대 도시의 유물 | 중후반 | chests/ancient_city 전리품 3회(서로 다른 상자) | 900 | 650 | trial_vaults_5 | I | 없음 | NOW |
| `xpq.dim.bastion_all` | 보루 정복 | 후반 | 보루 상자 4종(bridge/hoglin_stable/other/treasure) 전부 전리품 생성 | 1,350 | 950 | fortress_loot | I | 부분: loot_bastion(4종 중 1) | NOW |
| `xpq.dim.ominous_vaults_5` | 불길한 시련 | 후반 | 불길한 금고 서로 다른 5개 개방 | 1,350 | 950 | trial_vaults_5 | I | 수량증가: revaulting(1) | NOW |
| `xpq.dim.end_cities_3` | 엔드 도시 순례 | 후반 | 서로 다른 엔드 도시 구조물(시작 청크 기준) 3곳 진입 | 1,100 | 800 | - | I | 부분: find_end_city(1) | NOW |
| `xpq.dim.shulker_boxes` | 공허의 상자 | 후반 | 셜커 상자 서로 다른 색 4종 제작 | 1,000 | 750 | end_cities_3 | I | 없음 | NOW |
| `xpq.dim.end_city_loot_10` | 엔드의 약탈자 | 엔드게임 | chests/end_city_treasure 서로 다른 상자 10개 전리품 생성 | 3,150 | 2,250 | end_cities_3 | I | 없음 | NOW |
| `xpq.dim.nether_travel_50k` | 네더 회랑 | 엔드게임 | 네더 차원에서 지급된 이동 통계(cm) 합계 50,000블록 | 2,500 | 1,800 | bastion_all | I | 부분: fast_travel(7,000 단일 이동) | NOW |

### 전투 (`combat`) — 30개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.cmb.hostile_100` | 파수꾼 | 초반 | 적대 몹(Monster) 100마리 처치 | 250 | 150 | - | S | 수량증가: kill_a_mob(1) | NOW |
| `xpq.cmb.mm_arms` | 새로운 무기 | 초반 | 단검과 대검으로 각각 적대 몹 1마리 처치 | 250 | 150 | hostile_100 | I | 부분: kill_a_mob | NOW |
| `xpq.cmb.bestiary_5` | 현지 조사 | 초반 | MoreMinecrafts 적대 몹 5종 처치 | 250 | 150 | hostile_100 | S | 없음 | NOW |
| `xpq.cmb.shield_500` | 철벽 | 중반 | 방패로 공격 500회 막기 | 450 | 350 | hostile_100 | R부분 | 수량증가: K36(50) | NOW |
| `xpq.cmb.hostile_500` | 숙련 전사 | 중반 | 적대 몹 500마리 처치 | 500 | 350 | hostile_100 | S | 수량증가 | NOW |
| `xpq.cmb.vanilla_species_20` | 전장의 다양성 | 중반 | 서로 다른 바닐라 적대 몹 20종 처치 | 500 | 350 | hostile_500 | S | 부분: kill_all_mobs(전종) | NOW |
| `xpq.cmb.dagger_100` | 그림자 칼날 | 중반 | 단검으로 적대 몹 100마리 처치 | 500 | 350 | mm_arms | I | 없음 | NOW |
| `xpq.cmb.greatsword_100` | 거인의 일격 | 중반 | 대검으로 적대 몹 100마리 처치 | 500 | 350 | mm_arms | I | 없음 | NOW |
| `xpq.cmb.bestiary_10` | 현장 도감 | 중반 | MoreMinecrafts 적대 몹 10종 처치 | 500 | 350 | bestiary_5 | S | 없음 | NOW |
| `xpq.cmb.bandits_20` | 산적 토벌 | 중반 | bandit, bandit_archer, bandit_warrior 합계 20마리 처치(3종 각 1 이상) | 500 | 350 | bestiary_5 | I | 없음 | NOW |
| `xpq.cmb.cave_dwellers` | 동굴 소탕 | 중반 | cave_zombie, cave_skeleton, cave_creeper, small_cave_spider, swamp_cave_spider, cave_bat 처치 | 500 | 350 | bestiary_5 | I | 없음 | VERIFY |
| `xpq.cmb.trident_50` | 바다의 창 | 중반 | 삼지창 투척으로 적대 몹 50마리 처치 | 450 | 350 | hostile_500 | R부분 | 수량증가: K38(10) | NOW |
| `xpq.cmb.breeze_10` | 바람 사냥 | 중반 | 브리즈 10마리 처치 | 450 | 350 | vanilla_species_20 | S | 부분: blowback(풍력구 처치 1) | NOW |
| `xpq.cmb.guardians_30` | 해저 파수꾼 | 중반 | 가디언 30마리 처치 | 450 | 350 | vanilla_species_20 | S | 수량증가: K50(드라운드+가디언 2) | NOW |
| `xpq.cmb.long_shot_25` | 명사수 | 중후반 | 30블록 이상 거리에서 활/석궁으로 적대 몹 25마리 처치 | 850 | 600 | hostile_500 | I | 부분: sniper_duel(스켈레톤 50블록 1) | NOW |
| `xpq.cmb.mace_smash_10` | 낙하 강타 | 중후반 | 철퇴 낙하 공격(smash)으로 적대 몹 10마리 처치 | 750 | 550 | hostile_500 | I | 부분: overoverkill(단일 50 피해) | NOW |
| `xpq.cmb.lesser_wither_10` | 시든 하수인 | 중후반 | lesser_wither_skeleton 10마리 처치 | 700 | 500 | bestiary_10 | I | 없음 | NOW |
| `xpq.cmb.hostile_2000` | 백전노장 | 후반 | 적대 몹 2,000마리 처치 | 1,100 | 800 | hostile_500 | S | 수량증가 | NOW |
| `xpq.cmb.bestiary_25` | 완전 도감 | 후반 | MoreMinecrafts 적대 몹 25종 처치 | 1,350 | 950 | bestiary_10 | S | 없음 | VERIFY |
| `xpq.cmb.habitats_5` | 서식지 사냥꾼 | 후반 | 사막·늪·설원·균류·야생 슬라임 서식지 MM 몹 각 1종 이상 처치 | 1,100 | 800 | bestiary_10 | S | 없음 | VERIFY |
| `xpq.cmb.netherite_blades` | 검은 강철 | 후반 | 네더라이트 단검과 네더라이트 대검으로 각각 적대 몹 처치 | 1,100 | 800 | dagger_100 | I | 없음 | NOW |
| `xpq.cmb.ravagers_5` | 파괴수 사냥 | 후반 | 파괴수 5마리 처치 | 1,000 | 750 | vanilla_species_20 | S | 부분: kill_a_mob | NOW |
| `xpq.cmb.brutes_3` | 돌격대 섬멸 | 후반 | 피글린 야수 3마리 처치 | 1,000 | 750 | vanilla_species_20 | S | 부분: kill_a_mob | NOW |
| `xpq.cmb.wither_skulls_3` | 세 개의 해골 | 후반 | 위더 스켈레톤 해골 누적 3개 획득 | 1,100 | 800 | lesser_wither_10 | I | 수량증가: get_wither_skull(1) | NOW |
| `xpq.cmb.raids_3` | 마을의 수호자 | 후반 | 습격 3회 승리 | 1,350 | 950 | hostile_2000 | I | 수량증가: hero_of_the_village(1) | NOW |
| `xpq.cmb.hostile_5000` | 전설의 전사 | 엔드게임 | 적대 몹 5,000마리 처치 | 1,900 | 1,350 | hostile_2000 | S | 수량증가 (몹팜 가속 가능 — 보상 상한 검토) | NOW |
| `xpq.cmb.tactical_ammo` | 전술 사격 | 중반 | 특수 탄약으로 적대 몹 처치 | 500 | 350 | - | I | 없음 | FUTURE |
| `xpq.cmb.gauntlet_50` | 건틀릿 격투 | 중후반 | 건틀릿으로 적대 몹 50마리 처치 | 750 | 550 | hostile_500 | I | 없음 | FUTURE |
| `xpq.cmb.elemental_100` | 원소 전사 | 후반 | T2 무기로 적대 몹 100마리 처치 | 1,100 | 800 | hostile_500 | I | 없음 | FUTURE |
| `xpq.cmb.elemental_skills` | 원소 숙련 | 후반 | T2 액티브 스킬 5종 각각 적중 | 1,250 | 900 | elemental_100 | I | 없음 | FUTURE |

### 보스 (`boss`) — 9개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.boss.elder_guardian` | 심해의 장로 | 중후반 | 엘더 가디언 처치 | 750 | 550 | - | S | 부분: kill_a_mob | NOW |
| `xpq.boss.monument_clear` | 해저 신전 공략 | 후반 | 해저 신전 1곳 공략 완료(엘더 가디언 전멸) 시 검증된 참여자 전원 — 엘더 가디언 피해 또는 신전 내 가디언 전투 증거 필요, 단순 체류 불인정 | 1,450 | 1,050 | elder_guardian | I | 부분: Heart of Currents 획득 경로와 연결 | VERIFY |
| `xpq.boss.warden` | 심연의 감시자 | 후반 | 워든 처치 | 1,700 | 1,200 | - | S | 부분: kill_a_mob | NOW |
| `xpq.boss.wither` | 시듦의 종언 | 후반 | 위더 처치 | 1,450 | 1,050 | - | S | 부분: K52(드래곤 또는 위더) | NOW |
| `xpq.boss.twin_bosses` | 두 개의 왕관 | 엔드게임 | 엔더 드래곤과 위더 모두 처치 | 3,150 | 2,250 | wither | R | 부분: kill_dragon + K52 | NOW |
| `xpq.boss.dragon_rematch` | 재림한 용 | 엔드게임 | 재소환된 엔더 드래곤 처치(본인 소환 여부 무관, 두 번째 이후 드래곤) | 3,150 | 2,250 | twin_bosses | I | 부분: respawn_dragon(소환) | NOW |
| `xpq.boss.omen_raid` | 불길한 전쟁 | 엔드게임 | 흉조 레벨 III 이상으로 시작된 습격 승리 | 3,150 | 2,250 | - | I | 부분: hero_of_the_village | NOW |
| `xpq.boss.monuments_3` | 세 신전의 정복자 | 엔드게임 | 서로 다른 해저 신전(차원+구조물 시작 청크) 3곳에서 참여자로 공략 완료 인정 | 3,450 | 2,500 | monument_clear | I | 없음 | VERIFY |
| `xpq.boss.warden_3` | 침묵의 사냥꾼 | 엔드게임 | 워든 3마리 처치 | 3,150 | 2,250 | warden | S | 수량증가 | NOW |

### 채굴·자원 (`mining`) — 19개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.min.first_custom_ore` | 낯선 광석 | 초반 | MoreMinecrafts 광석 블록 1개 채굴 | 250 | 150 | - | I | 부분: K1 | NOW |
| `xpq.min.custom_ore_survey` | 심층 탐사 | 초중반 | 티타늄·텅스텐·미스릴·에테리움 광석 4종 채굴 | 350 | 250 | first_custom_ore | I | 부분: K1/K2 | NOW |
| `xpq.min.obsidian_64` | 흑요석 채석장 | 초중반 | 흑요석 64개 채굴 | 300 | 200 | - | S | 부분: form_obsidian(획득) | NOW |
| `xpq.min.ore_1000` | 광부 | 중반 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 1,000개 채굴 | 500 | 350 | first_custom_ore | I | 수량증가: K9(256) | NOW |
| `xpq.min.deepslate_custom` | 심층암 광맥 | 중반 | 네 MM 광석의 심층암 변종 4종 채굴 | 500 | 350 | custom_ore_survey | I | 부분: K2 | NOW |
| `xpq.min.stone_10000` | 채석장 주인 | 중반 | 돌 계열 블록 10,000개 채굴 | 500 | 350 | - | S | 부분: K8(암석 종류) | NOW |
| `xpq.min.logs_1000` | 벌목꾼 | 중반 | 자연 생성 원목 1,000개 채굴 | 500 | 350 | - | R부분 | 수량증가: K5(200) | NOW |
| `xpq.min.amethyst_spyglass` | 수정 동굴 | 중반 | 자수정 조각 32개 획득 후 망원경 제작 | 400 | 300 | - | I | 부분: spyglass 관련 발전과제(관찰) | NOW |
| `xpq.min.adamantium_debris` | 황무지 아래 | 중후반 | 아다만티움 잔해 획득 | 750 | 550 | deepslate_custom | R | 없음 | NOW |
| `xpq.min.all_wood` | 수목원 | 중후반 | 원목/줄기 12종(참나무~창백한 참나무, 진홍·뒤틀린 줄기) 획득 | 750 | 550 | logs_1000 | R | 수량증가: K4(6) | NOW |
| `xpq.min.pickaxe_5000` | 곡괭이의 생애 | 중후반 | 곡괭이 내구도 5,000 사용 | 700 | 500 | ore_1000 | R부분 | 수량증가: K10(500) | NOW |
| `xpq.min.vein_500` | 광맥 연쇄 | 중후반 | Vein Mining 인챈트로 추가 파괴된 블록 500개 | 750 | 550 | ore_1000 | I | 없음 | VERIFY |
| `xpq.min.lumberjack_200` | 한 번에 쓰러뜨리기 | 중후반 | Lumberjack 인챈트로 추가 파괴된 원목 200개 | 750 | 550 | logs_1000 | I | 없음 | VERIFY |
| `xpq.min.ore_5000` | 대광부 | 후반 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 5,000개 채굴 | 1,100 | 800 | ore_1000 | I | 수량증가: K9 | NOW |
| `xpq.min.diamond_ore_64` | 다이아몬드 광맥 | 후반 | 자연 생성 다이아몬드 광석(일반+심층암) 64개 채굴(설치 광석 제외) | 1,100 | 800 | ore_1000 | I | 부분: mine_diamond(획득 1) | NOW |
| `xpq.min.adamantium_10` | 아다만티움 채굴단 | 후반 | 자연 생성 아다만티움 잔해 10개 채굴(설치 잔해 제외) | 1,250 | 900 | adamantium_debris | I | 수량증가 | NOW |
| `xpq.min.debris_16` | 고대의 잔해 | 후반 | 자연 생성 고대 잔해 16개 채굴(설치 잔해 제외) | 1,100 | 800 | adamantium_debris | I | 부분: obtain_ancient_debris(1) | NOW |
| `xpq.min.ore_20000` | 심연의 광부 | 엔드게임 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 20,000개 채굴 | 3,150 | 2,250 | ore_5000 | I | 수량증가 | NOW |
| `xpq.min.drill_1000` | 굴착기 | 후반 | 채굴 드릴로 블록 1,000개 파괴 | 1,100 | 800 | ore_1000 | I | 없음 | FUTURE |

### 농업·생태·교역 (`farming`) — 19개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.frm.harvest_all_crops` | 풍성한 수확 | 초반 | 다 자란 밀·당근·감자·비트·수박·호박·네더 사마귀·코코아·달콤한 열매 수확 | 250 | 150 | - | I | 부분: plant_seed, K20, K21 | NOW |
| `xpq.frm.composite_foods` | 요리사 | 초반 | 케이크·호박 파이·쿠키·빵·토끼 스튜 제작 | 200 | 150 | harvest_all_crops | I | 부분: K30(임의 10종) | NOW |
| `xpq.frm.tame_5` | 동물의 친구 | 초중반 | 늑대·고양이·앵무새·말·라마 5종 모두 길들이기(여우는 길들일 수 없어 제외) | 350 | 250 | - | I | 부분: tame_an_animal(1), complete_catalogue(고양이 변종) | NOW |
| `xpq.frm.harvest_1000` | 농부 | 중반 | 작물 1,000회 수확 | 500 | 350 | harvest_all_crops | R부분 | 수량증가: K20(100) | NOW |
| `xpq.frm.till_1000` | 개간 | 중반 | 경작지 1,000칸 만들기 | 400 | 300 | harvest_all_crops | R부분 | 수량증가: K22(100) | NOW |
| `xpq.frm.breed_200` | 목장주 | 중반 | 동물 200회 번식 | 500 | 350 | tame_5 | S | 수량증가: K26(30) | NOW |
| `xpq.frm.honey_32` | 양봉가 | 중반 | 꿀병 32개 수집 | 450 | 350 | - | I | 부분: safely_harvest_honey(1) | NOW |
| `xpq.frm.master_trade` | 대가와의 거래 | 중반 | 숙련도 Master 주민과 거래 | 500 | 350 | - | I | 부분: trade(임의) | NOW |
| `xpq.frm.mending_book` | 수선의 비밀 | 중반 | 사서 주민 거래로 수선 마법부여 책 획득 | 450 | 350 | master_trade | I | 없음 | NOW |
| `xpq.frm.ancient_harvest` | 고대 식물원 | 중후반 | 횃불꽃과 투구꽃(벌레잡이) 작물을 다 자란 상태로 수확 | 750 | 550 | harvest_all_crops | I | 부분: plant_any_sniffer_seed(심기) | NOW |
| `xpq.frm.professions_10` | 마을 경제 | 중후반 | 서로 다른 직업 주민 10종과 거래 | 850 | 600 | master_trade | I | 부분: K61(거래 수) | NOW |
| `xpq.frm.villager_discount_20` | 단골 손님 | 중후반 | Villager Trading 연구 할인이 적용된 거래 20회 | 750 | 550 | master_trade | I | 없음 | VERIFY |
| `xpq.frm.harvest_5000` | 대농장 | 후반 | 작물 5,000회 수확 | 1,100 | 800 | harvest_1000 | R부분 | 수량증가 | NOW |
| `xpq.frm.trades_200` | 상인 연합 | 후반 | 주민 거래 200회 | 1,100 | 800 | professions_10 | S | 수량증가: K61(30) | NOW |
| `xpq.frm.barter_200` | 금의 외교 | 후반 | 피글린 물물교환 200회 | 1,100 | 800 | - | R부분 | 수량증가: K48(20) | NOW |
| `xpq.frm.soil_study_harvest` | 토양학자 | 후반 | Soil Study 연구 효과가 적용된 수확 500회 | 1,100 | 800 | harvest_1000 | I | 없음 | VERIFY |
| `xpq.frm.first_capture` | 포획 | 중반 | 포획 도구로 몹 포획 | 500 | 350 | - | I | 없음 | FUTURE |
| `xpq.frm.auto_farm` | 자동 농장 | 중반 | 자동 농장 설치·가동(산출 1회) | 500 | 350 | harvest_1000 | I | 없음 | FUTURE |
| `xpq.frm.capture_10` | 이동 동물원 | 후반 | 서로 다른 몹 10종 포획 | 1,100 | 800 | first_capture | I | 없음 | FUTURE |

### 낚시 (`fishing`) — 16개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.fsh.first_sale` | 장날 | 초반 | 어부 주민에게 물고기 1마리 판매 | 250 | 150 | - | I | 없음 | NOW |
| `xpq.fsh.freshwater_4` | 민물 낚시꾼 | 초반 | FRESHWATER 생태 어종 4종 모두 낚기(붕어·잉어·메기·배스) | 250 | 150 | - | R | 부분: K27(임의 5) | NOW |
| `xpq.fsh.first_module` | 첫 채비 | 초반 | 낚싯대에 Reel/Minnow/Crankbait 중 하나라도 II 단계 장착 | 250 | 150 | first_sale | R | 없음 | NOW |
| `xpq.fsh.grade_four` | 상급 어획 | 중반 | 등급 IV 이상 물고기 낚기 | 500 | 350 | first_module | I | 부분: fishy_business | NOW |
| `xpq.fsh.sale_64` | 어시장 | 중반 | 물고기 누적 64마리 판매 | 500 | 350 | first_sale | I | 없음 | NOW |
| `xpq.fsh.tier3_rig` | 명인의 채비 | 중반 | Reel III·Minnow III·Crankbait III 동시 장착 낚싯대 보유 | 500 | 350 | first_module | R | 없음 | NOW |
| `xpq.fsh.water_biomes_8` | 물길 탐사 | 중반 | 서로 다른 수역 생물군계 8곳에서 낚기 성공 | 500 | 350 | freshwater_4 | R | 수량증가: K28(2) | NOW |
| `xpq.fsh.multi_catch` | 두 마리 한 번에 | 중반 | Multi Catch로 한 번의 챌린지에서 2마리 이상 획득 | 450 | 350 | first_module | I | 없음 | NOW |
| `xpq.fsh.grade_five` | 완벽한 어획 | 중후반 | 등급 V 물고기 낚기 | 750 | 550 | grade_four | I | 부분: first_custom_fish | NOW |
| `xpq.fsh.ocean_trio` | 원양 어업 | 중후반 | 참다랑어·황새치·청새치 낚기 | 850 | 600 | tier3_rig | R | 부분: K27 | NOW |
| `xpq.fsh.catch_500` | 어업 조합장 | 후반 | 낚시 성공 500회 | 1,100 | 800 | sale_64 | R부분 | 수량증가: K29(100) | NOW |
| `xpq.fsh.full_catalogue` | 어류 대도감 | 후반 | 12종 전부 낚기 | 1,350 | 950 | ocean_trio | R | 부분: K27(5), first_custom_fish | NOW |
| `xpq.fsh.emerald_500` | 바다의 상인 | 후반 | 어류 판매로 에메랄드 누적 500개 획득 | 1,100 | 800 | sale_64 | I | 없음 | NOW |
| `xpq.fsh.giant_catch` | 전설의 월척 | 후반 | 어종 최대 길이의 90% 이상인 물고기 낚기 | 1,100 | 800 | grade_five | I | 없음 | VERIFY |
| `xpq.fsh.grade_five_5` | 명장 | 엔드게임 | 서로 다른 5종에서 등급 V 낚기 | 3,150 | 2,250 | grade_five | I | 없음 | NOW |
| `xpq.fsh.fish_trap` | 통발 | 중반 | 통발로 물고기 획득 | 500 | 350 | - | I | 없음 | FUTURE |

### 제작·생산 (`crafting`) — 15개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.cft.sleeping_bag` | 야영 준비 | 초반 | 탐험용 침낭 제작 | 150 | 100 | - | I | 없음 | NOW |
| `xpq.cft.enchanting_30` | 깊은 지혜 | 초중반 | 책장 15개 이상 배치된 마법부여대에서 30레벨 마법부여 | 350 | 250 | - | I | 부분: enchant_item(임의) | NOW |
| `xpq.cft.smelt_1000` | 용광로 장인 | 중반 | 화로/용광로/훈연기 산출물 1,000개 수거 | 500 | 350 | - | I | 부분: K11/K12 | NOW |
| `xpq.cft.all_daggers` | 단검 수집가 | 중반 | 나무·돌·구리·철·금·다이아몬드·네더라이트 단검 7종 제작 | 500 | 350 | - | I | 없음 | NOW |
| `xpq.cft.anvil_50` | 대장장이 | 중반 | 모루 수리 50회 | 450 | 350 | - | R부분 | 수량증가: K16(10) | NOW |
| `xpq.cft.redstone_100` | 공학자 | 중반 | 레드스톤 부품 100개 설치 | 450 | 350 | - | R부분 | 수량증가: K19(20) | NOW |
| `xpq.cft.parachute` | 하늘의 안전장치 | 중반 | 낙하산 제작 | 400 | 300 | - | I | 없음(K57은 레시피 해금) | NOW |
| `xpq.cft.conduit` | 바다의 심장 | 중후반 | 전달체 제작 후 활성화(프리즈머린 구조 완성) | 850 | 600 | - | I | 없음 | NOW |
| `xpq.cft.recipes_200` | 만능 장인 | 중후반 | 서로 다른 레시피 ID 200종 제작(결과 수거 기준) | 750 | 550 | smelt_1000 | I | 수량증가: K15(50) | NOW |
| `xpq.cft.mm_metallurgy` | 다섯 금속 | 후반 | 티타늄 주괴·텅스텐 주괴·미스릴·에테리움·아다만티움 주괴 각 획득 | 1,350 | 950 | smelt_1000 | I | 부분: K11 | NOW |
| `xpq.cft.all_greatswords` | 대검 수집가 | 후반 | 대검 7종 전부 제작 | 1,100 | 800 | all_daggers | I | 없음 | NOW |
| `xpq.cft.netherite_tools` | 검은 공구함 | 후반 | 네더라이트 검·곡괭이·도끼·삽·괭이 모두 보유 | 1,100 | 800 | anvil_50 | I | 부분: obtain_netherite_hoe(괭이) | NOW |
| `xpq.cft.mm_blocks` | 금속 보관고 | 엔드게임 | MM 금속 블록 5종 각 1개 이상 제작 | 3,150 | 2,250 | mm_metallurgy | I | 없음 | NOW |
| `xpq.cft.backpack_large` | 짐꾼 | 중반 | 대형 배낭 획득 | 500 | 350 | - | I | 없음 | FUTURE |
| `xpq.cft.portable_furnace` | 휴대 제련 | 중반 | 휴대용 화로로 아이템 64개 제련 | 500 | 350 | smelt_1000 | I | 없음 | FUTURE |

### 건축 (`building`) — 8개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.bld.place_1000` | 첫 집 | 초반 | 블록 1,000개 설치 | 250 | 150 | - | S | 없음 | NOW |
| `xpq.bld.variety_50` | 건축 재료상 | 초중반 | 서로 다른 블록 50종 설치 | 350 | 250 | place_1000 | I | 없음 | NOW |
| `xpq.bld.place_10000` | 건축가 | 중반 | 블록 10,000개 설치 | 500 | 350 | place_1000 | S | 없음 | NOW |
| `xpq.bld.storage_50` | 창고 관리인 | 중반 | 상자·통·셜커 상자 합계 50개 설치 | 400 | 300 | place_1000 | I | 부분: K17(상자 제작) | NOW |
| `xpq.bld.copper_ages` | 청동기 시대 | 중반 | 구리 블록 4단계 산화 상태(일반·풍화 시작·풍화·산화) 각각 설치 | 450 | 350 | variety_50 | I | 부분: wax_on/wax_off | NOW |
| `xpq.bld.variety_150` | 건축 백과 | 후반 | 서로 다른 블록 150종 설치 | 1,100 | 800 | variety_50 | I | 없음 | NOW |
| `xpq.bld.place_50000` | 도시 계획가 | 후반 | 블록 50,000개 설치 | 1,100 | 800 | place_10000 | S | 없음 | NOW |
| `xpq.bld.place_200000` | 세계 건설자 | 엔드게임 | 블록 200,000개 설치 | 3,150 | 2,250 | place_50000 | S | 없음 | NOW |

### 연금 (`alchemy`) — 9개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.alc.shock_potion` | 병 속의 번개 | 중반 | Shock 물약 양조 | 500 | 350 | - | I | 부분: brew_potion, K63 | NOW |
| `xpq.alc.types_12` | 물약 연구가 | 중반 | 서로 다른 물약 종류 12종 양조 | 500 | 350 | - | R | 수량증가: K64(5) | NOW |
| `xpq.alc.splash_lingering` | 투척 연금술 | 중반 | 투척 물약 3종과 잔류 물약 3종 양조 | 500 | 350 | types_12 | I | 부분: K65/K66(각 1) | NOW |
| `xpq.alc.ingredients_all` | 재료 감정사 | 중반 | 양조 재료 10종(네더 사마귀·블레이즈 가루·레드스톤·발광석 가루·가스트 눈물·마그마 크림·거미 눈·복어·황금 당근·토끼 발) 획득 | 450 | 350 | - | R | 수량증가: K68(8) | NOW |
| `xpq.alc.effects_16` | 체험주의자 | 중후반 | 서로 다른 상태 효과 16종 경험 | 750 | 550 | types_12 | R | 수량증가: K67(8) / 부분: all_effects | NOW |
| `xpq.alc.enhanced_3` | 정제와 연장 | 중후반 | 물약 3종에 대해 강화(II)판과 연장판을 모두 양조 | 750 | 550 | types_12 | I | 부분: K70 | NOW |
| `xpq.alc.shock_10` | 전기 충격 | 중후반 | Shock 효과를 몹 10마리에게 부여 | 700 | 500 | shock_potion | I | 없음 | VERIFY |
| `xpq.alc.potions_250` | 대연금술사 | 후반 | 물약 250개 양조 | 1,100 | 800 | splash_lingering | R부분 | 수량증가: K69(50) | NOW |
| `xpq.alc.injector` | 주사기 | 중후반 | 포션 주입기로 효과 적용 10회 | 750 | 550 | shock_potion | I | 없음 | FUTURE |

### 장비 (`equipment`) — 20개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.eqp.enhance_5` | 첫 이정표 | 초반 | 강화 +5 이상 장비를 인벤토리·착용 슬롯에 보유(강화 수행자 무관) | 250 | 150 | - | R | 없음 | NOW |
| `xpq.eqp.custom_enchant` | 비전 공예 | 초중반 | MoreMinecrafts 인챈트가 붙은 장비 보유 | 350 | 250 | enhance_5 | R | 부분: enchant_item | NOW |
| `xpq.eqp.enhance_10` | 단련 | 중반 | 강화 +10 이상 장비 보유 | 500 | 350 | enhance_5 | R | 없음 | NOW |
| `xpq.eqp.enhance_trio` | 삼위 단련 | 중후반 | 무기·도구·방어구 각 +10 이상을 동시에 보유 | 850 | 600 | enhance_10 | R | 없음 | NOW |
| `xpq.eqp.enhance_15` | 숙련의 경지 | 중후반 | 강화 +15 이상 장비 보유 | 750 | 550 | enhance_10 | R | 없음 | NOW |
| `xpq.eqp.custom_enchant_5` | 비전 수집가 | 후반 | 서로 다른 MM 인챈트 5종 보유 이력 | 1,100 | 800 | custom_enchant | I | 없음 | VERIFY |
| `xpq.eqp.enhance_20` | 완성 | 후반 | 강화 +20 장비 보유 | 1,250 | 900 | enhance_15 | R | 없음 | NOW |
| `xpq.eqp.first_t2_core` | 원소의 심장 | 후반 | T2 코어 아무거나 1종 획득 | 1,250 | 900 | - | R | 없음 | NOW |
| `xpq.eqp.t2_cores_3` | 세 개의 심장 | 후반 | T2 코어 서로 다른 3종 획득 | 1,250 | 900 | first_t2_core | R | 없음 | NOW |
| `xpq.eqp.enhance_20_armor` | 불멸의 갑주 | 엔드게임 | 방어구 4부위 각 +20을 동시에 보유(착용 또는 인벤토리) | 3,750 | 2,700 | enhance_20 | R | 없음 | NOW |
| `xpq.eqp.custom_enchant_all` | 비전의 정점 | 엔드게임 | MM 인챈트 11종 전부 보유 이력 | 3,150 | 2,250 | custom_enchant_5 | I | 없음 | VERIFY |
| `xpq.eqp.all_t2_cores` | 다섯 심장 | 엔드게임 | T2 코어 5종 전부 획득 | 3,750 | 2,700 | t2_cores_3 | R | 없음 | NOW |
| `xpq.eqp.accessory_first` | 첫 장신구 | 중반 | 장신구 1개 장착 | 500 | 350 | - | I | 없음 | FUTURE |
| `xpq.eqp.first_t2_weapon` | 원소의 무기 | 후반 | T2 무기 7종 중 1종 획득 | 1,350 | 950 | first_t2_core | I | 없음 | FUTURE |
| `xpq.eqp.accessory_full` | 다섯 슬롯 | 후반 | 장신구 슬롯 5개 모두 장착 | 1,250 | 900 | accessory_first | I | 없음 | FUTURE |
| `xpq.eqp.t3_special` | 특수 장비 | 후반 | LOCKED T3 Special 7종 중 1종 획득 | 1,350 | 950 | - | I | 없음 | FUTURE |
| `xpq.eqp.t2_arsenal` | 원소 무기고 | 엔드게임 | T2 무기 7종 전부 획득 | 4,050 | 2,950 | first_t2_weapon | I | 없음 | FUTURE |
| `xpq.eqp.t2_plus20` | 원소의 완성 | 엔드게임 | T2 무기 +20 | 3,750 | 2,700 | first_t2_weapon | I | 없음 | FUTURE |
| `xpq.eqp.accessory_all` | 유물 수집가 | 엔드게임 | 장신구 12종 전부 획득 | 3,750 | 2,700 | accessory_full | I | 없음 | FUTURE |
| `xpq.eqp.t3_set` | 특수 장비 수집가 | 엔드게임 | LOCKED T3 Special 7종 중 5종 획득 | 4,050 | 2,950 | t3_special | I | 없음 | FUTURE |

### 성장(Knowledge·연구) (`growth`) — 21개

| ID | 이름 | 단계 | 달성 조건 | XP(D·기준) | XP(B·참고) | 시각 선행 | 추적 | 중복 검토 | 상태 |
|---|---|---|---|---:|---:|---|---|---|---|
| `xpq.grw.knowledge_10` | 학도 | 초반 | Knowledge 10개 해금 | 250 | 150 | - | R | 없음 | NOW |
| `xpq.grw.first_research` | 첫 실험 | 초반 | 루트가 아닌 연구 1레벨 구매 | 250 | 150 | - | R | 없음 | NOW |
| `xpq.grw.seven_roots` | 일곱 기초 | 초중반 | 7개 분야 루트 연구 전부 구매 | 350 | 250 | first_research | R | 없음 | NOW |
| `xpq.grw.knowledge_20` | 탐구자 | 초중반 | Knowledge 20개 해금 | 350 | 250 | knowledge_10 | R | 없음 | NOW |
| `xpq.grw.research_10` | 연구원 | 중반 | 서로 다른 연구 노드 10개 보유 | 500 | 350 | seven_roots | R | 없음 | NOW |
| `xpq.grw.knowledge_35` | 학자 | 중반 | Knowledge 35개 해금 | 500 | 350 | knowledge_20 | R | 없음 | NOW |
| `xpq.grw.first_t2` | 전문 연구 | 중반 | T2 연구 1레벨 구매 | 500 | 350 | research_10 | R | 없음 | NOW |
| `xpq.grw.climbing_100` | 등반가 | 중반 | Climbing 연구 효과로 벽 100블록 등반 | 450 | 350 | first_research | I | 없음 | VERIFY |
| `xpq.grw.discipline_breadth` | 박학다식 | 중후반 | 7개 분야 각각 루트 외 연구 1개 이상 보유 | 850 | 600 | research_10 | R | 없음 | NOW |
| `xpq.grw.spent_10k` | 투자자 | 중후반 | 보유 연구의 승인 원장 원시 XP 합계(보유 연구 가치) 10,000 이상 | 750 | 550 | first_t2 | R | 없음(지출 1회성, 반복 불가) | NOW |
| `xpq.grw.wall_jump_50` | 벽차기 | 중후반 | Wall Jump 50회 | 700 | 500 | climbing_100 | I | 없음 | VERIFY |
| `xpq.grw.fall_roll_20` | 낙법 | 중후반 | Fall Roll로 낙하 피해 경감 20회 | 700 | 500 | climbing_100 | I | 없음 | VERIFY |
| `xpq.grw.throwable_pickaxe` | 투척 곡괭이 | 중후반 | Throwable Pickaxe로 원거리 블록 100개 파괴 | 700 | 500 | research_10 | I | 없음 | VERIFY |
| `xpq.grw.knowledge_50` | 석학 | 후반 | Knowledge 50개 해금 | 1,100 | 800 | knowledge_35 | R | 없음 | NOW |
| `xpq.grw.research_25` | 수석 연구원 | 후반 | 연구 노드 25개 보유 | 1,100 | 800 | research_10 | R | 없음 | NOW |
| `xpq.grw.first_t3` | 돌파구 | 후반 | T3 연구 1레벨 구매 | 1,250 | 900 | first_t2 | R | 없음 | NOW |
| `xpq.grw.spent_50k` | 후원자 | 후반 | 보유 연구 가치 50,000 이상 | 1,250 | 900 | spent_10k | R | 없음 | NOW |
| `xpq.grw.knowledge_complete` | 현자 | 엔드게임 | 미해결(unresolved) 제외 Knowledge 전부 해금 | 3,750 | 2,700 | knowledge_50 | R | 없음 | NOW |
| `xpq.grw.research_50` | 연구소장 | 엔드게임 | 연구 노드 50개 보유 | 3,450 | 2,500 | research_25 | R | 없음 | NOW |
| `xpq.grw.t3_5` | 최첨단 | 엔드게임 | T3 연구 5개 보유 | 3,150 | 2,250 | first_t3 | R | 없음 | NOW |
| `xpq.grw.discipline_complete` | 분야의 대가 | 엔드게임 | 한 분야의 모든 연구를 최대 레벨 | 3,750 | 2,700 | t3_5 | R | 없음 | NOW |

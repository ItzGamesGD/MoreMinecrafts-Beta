# 아이템·블록·몹 에셋 구현 요구서 (2026-11-24)

**상태: 오너 전체 승인 (시안 기준).** 이 문서는 승인된 시안표를 구현 요구로 옮긴 것입니다. 구현 자체는 이 문서를 근거로 별도 작업에서 진행합니다.

## 공통 기준
- 기존 승인 32×32 아이템 픽셀 아트 스타일: 명확한 1px 다크 아웃라인, 3~4단 명암, 고대비 실루엣.
- 승인된 자산은 제외: 불꽃검(1263px), 물고기 12종·낚시 장비 1~3, 광물·블록 11종, 기존 액세서리 12종, 선택 카드 아트.
- 모든 아이템 텍스처는 `assets/moreminecrafts/textures/item/<id>.png`, 모델은 `models/item/<id>.json`.

## 1. T2 무기 5종 + 지팡이
| id | 크기 | 모델 | 이름 (en / ko) | 수용 기준 |
|---|---|---|---|---|
| flowing_water_sword | 32×32 | handheld | Flowing Water Sword / 유수의 검 | 물결 곡선 날(청록~코발트 그라데이션), 물방울 가드. 불꽃검과 겹치지 않는 유선형 검신, 외곽선 번짐 없음 |
| wind_sword | 32×32 | handheld | Wind Sword / 질풍의 검 | 얇은 세검 비율, 날 주변 소용돌이 기류 하이라이트(민트·백색), 날 끝 1px 예리도 유지 |
| wind_archers_bow | 32×32 | bow + pulling overrides | Wind Archer's Bow / 바람 궁수의 활 | 날개형 상·하 림, 당김 3단계(`_0`, `_1` pull 0.65, `_2` pull 0.9)별 시위 장력과 바람 에너지. 4프레임 간 손잡이 위치 고정 |
| earth_mace | 32×32 | handheld | Earth Mace / 대지의 철퇴 | 육중한 암석·청동 다면체 헤드, 균열 틈 황토빛 발광 코어. 기존 철퇴·도끼와 헤드 부피감으로 구분 |
| lightning_sword | 32×32 | handheld | Lightning Sword / 번개의 검 | 지그재그 번개 톱니 검신(황금·전격 시안), 전류 스파크. flowing_water_sword와 색상·직선 각도 대비 명확 |
| dark_energy_staff | 32×32 | handheld | Dark Energy Staff / 암흑 에너지 지팡이 | **보류.** 오너가 별도 해제하기 전까지 구현 제외 |

## 2. 임시 보석 텍스처 12종 교체
| id | 이름 (en / ko) | 수용 기준 |
|---|---|---|
| skeleton_king_chain | Skeleton King's Chain / 해골 왕의 사슬 | 백골·은빛 고리 사슬, 왕관 문장 펜던트. 범용 보석 실루엣 제거 |
| gladiator_blood_crest | Gladiator's Blood Crest / 검투사의 핏빛 문장 | 방패형 청동·강철 문장, 중앙 진홍 루비·혈흔 |
| ancient_clock_spring | Ancient Clock Spring / 고대 시계의 태엽 | 황동 나선 태엽, 톱니 끝 청록 파티나. 나선 간격 2px 이상 |
| ancient_world_tree_sap | Ancient World Tree Sap / 고대 세계수의 수액 | 호박색 투명 방울, 연녹 잎맥·생명 코어. 꿀병·슬라임볼과 구분 |
| lava_heartstone | Lava Heartstone / 용암 심장석 | 검붉은 현무암 외피 사이 맥동하는 마그마 심장. lava_core와 외피 비율로 구분 |
| permafrost_crystal | Permafrost Crystal / 영구동토 결정 | 육각 비대칭 서리 결정 다발, 빙청·백색 하이라이트 |
| abyssal_pearl_core | Abyssal Pearl Core / 심연 진주 핵 | 남보라·청록 진주 구체, 심해 산호 링. 엔더 진주와 색상·외곽 링으로 분리 |
| mermaid_scale | Mermaid Scale / 인어의 비늘 | 부채꼴 무지갯빛 비늘(에메랄드·라벤더 이리데슨스). 프리즘린 조각과 곡선 외형 차별 |
| void_star_shard | Void Star Shard / 공허 별 조각 | 4각 별 파편, 칠흑 내부 마젠타·별빛 성운 점묘. 네더의 별과 비대칭 실루엣 차별 |
| void_gravity_core | Void Gravity Core / 공허 중력 핵 | 검은 특이점 구체 + 기울어진 보라 궤도 링 2겹. void_star_shard와 궤도 고리로 식별 |
| shimmering_soul_glass | Shimmering Soul Glass / 일렁이는 영혼 유리 | 반투명 유리 파편, 영혼불 청록 잔상, 모서리 반사광 |
| feather_charm | Feather Charm / 깃털 부적 | 금빛 고리 + 순백·하늘색 깃털 + 매듭. **제작 대상 포함(오너 결정, 2026-11-24): 기존 액세서리 제외 규칙의 예외.** 현재 텍스처를 새 아트로 교체 |

## 3. T1 액세서리 7종 (바닐라 아이콘 교체)
| id | 이름 (en / ko) | 수용 기준 |
|---|---|---|
| t1_combo | Combo Crest (T1) / 연속 타격 휘장 (T1) | 3중 참격 궤적 + 구리·강철 T1 테두리 (승인된 12종 액세서리 톤 준수) |
| t1_crisis_escape | Crisis Escape Charm (T1) / 위기 이탈 부적 (T1) | 금 간 방패 뒤로 빠져나가는 잔상·연기 실루엣 |
| t1_heavy_buffer | Heavy Buffer Plate (T1) / 중장 완충판 (T1) | 리벳 박힌 두꺼운 철제 완충 흉갑 |
| t1_status_resilience | Status Resilience Brooch (T1) / 상태 저항 브로치 (T1) | 맹독(녹)·화염(주황) 방울을 튕겨내는 은빛 브로치 |
| t1_sustained_recovery | Sustained Recovery Pendant (T1) / 지속 회복 펜던트 (T1) | 맥동하는 에메랄드·선홍 결정 펜던트, 상승 십자 파동 |
| t1_sprint_acceleration | Sprint Acceleration Spur (T1) / 질주 가속 박차 (T1) | 날개 달린 경량 부츠 박차, 뒤로 뻗는 속도선 2줄 |
| t1_double_jump | Double Jump Feather-Ring (T1) / 이단 도약 깃털 고리 (T1) | 2단 파동 고리 위의 쌍깃털. feather_charm과 2중 고리 구조로 분리 |

**확정(오너, 2026-11-24):** 이 7종은 임시가 아닌 최종 아트입니다. 현재 로그의 "provisional" 표기는 구현 시 제거 대상입니다.

## 4. 단검 7종 · 대검 7종
| 단검 (id / en / ko) | 대검 (id / en / ko) | 공통 수용 기준 |
|---|---|---|
| wood_dagger / Wooden Dagger / 나무 단검 | wood_greatsword / Wooden Greatsword / 나무 대검 | 단검: 32×32 중하단의 짧은 단검 실루엣, 참나무 결. 대검: 대각선 전체를 채우는 광폭 양손검 + 철제 보강 띠 |
| stone_dagger / Stone Dagger / 돌 단검 | stone_greatsword / Stone Greatsword / 돌 대검 | 거친 조약돌·안산암 칩 날; 육중한 석재 판형, 두꺼운 크로스가드 |
| copper_dagger / Copper Dagger / 구리 단검 | copper_greatsword / Copper Greatsword / 구리 대검 | 적동 광택 날, 청록 산화 포인트 |
| iron_dagger / Iron Dagger / 철 단검 | iron_greatsword / Iron Greatsword / 철 대검 | 풀러(혈조) 홈이 있는 정제 강철; 대검은 리카소·이중 풀러 |
| gold_dagger / Golden Dagger / 금 단검 | gold_greatsword / Golden Greatsword / 금 대검 | 의식용 곡선 가드, 황금 날; 대검은 왕실 의장풍 날개형 가드 |
| diamond_dagger / Diamond Dagger / 다이아몬드 단검 | diamond_greatsword / Diamond Greatsword / 다이아몬드 대검 | 다면 커팅 시안 날, 짙은 감청 자루; 대검은 능선 하이라이트 |
| netherite_dagger / Netherite Dagger / 네더라이트 단검 | netherite_greatsword / Netherite Greatsword / 네더라이트 대검 | 흑자색 합금 날, 금빛 상감 리벳; 대검은 흑철 톱니 숄더와 용암빛 상감 |

**참고:** 현재 저장소에서 이 14종의 등록 코드를 찾지 못했습니다. 구현 전에 등록 방식을 먼저 확인해야 합니다.

## 5. 원소 코어 5종 · 유물 코어 5종 · 유물 촉매
| id | 이름 (en / ko) | 수용 기준 |
|---|---|---|
| flame_core | Flame Core / 화염의 심장 | 3갈래 불꽃 왕관 외곽 + 백황색 핵 |
| current_core | Current Core / 해류의 심장 | 소용돌이 파도 링(딥블루·아쿠아) + 물방울 진주 핵. thunder_core와 분리 |
| wind_core | Wind Core / 질풍의 심장 | S자 회전 기류 날개 2매 + 부유 공기 핵 |
| earth_core | Earth Core / 대지의 심장 | 암반 파편 4개 결속, 황토·이끼빛 |
| thunder_core | Thunder Core / 천둥의 심장 | 4방향 전격 마름모 프레임 + 번개 스파크 핵 |
| royal_tomb_relic_core | Royal Tomb Relic Core / 왕릉 유물 코어 | 고대 금관·청동 부조 케이싱, 영혼 청록 구슬 |
| forge_lord_relic_core | Forge Lord Relic Core / 용암 대장장이 유물 코어 | 흑철 모루·톱니 케이싱, 백열 용암 노심 |
| abyssal_relic_core | Abyssal Relic Core / 심해 유물 코어 | 프리즘린·해구 케이싱, 발광 심해 핵 |
| verdant_relic_core | Verdant Relic Core / 고대수 유물 코어 | 뿌리·덩굴 금속 세공, 에메랄드 세계수 핵 |
| void_relic_core | Void Relic Core / 공허 유물 코어 | 부유 흑요석 룬 케이싱, 엔더 자수정 공허 핵 |
| relic_catalyst | Relic Catalyst / 유물 촉매 | 삼각 연금술 아스트롤라베 고리 중앙의 무지갯빛·금빛 결정 |

**참고:** 실제 코어 id가 다른 경우(예: fire_core, heart_of_flame) 구현 단계에서 파일명을 매핑합니다.

## 6. 유틸 · 기타 14종
mob_capture_tool(몹 포획기), captured_mob_egg(포획된 몹 알), parachute(낙하산), miner_lantern(광부의 랜턴), potion_injector(물약 주입기), portable_furnace(휴대용 화로), mining_drill(채광 드릴), small_backpack(소형 배낭), backpack(모험가 배낭), large_backpack(대형 원정 배낭), gauntlet(전투 건틀릿), bounty_contract(현상수배 의뢰서), special_contract(특수 현상수배 의뢰서), colony_crystal(개척지 수정). 각 항목의 수용 기준은 시안표 6번과 같습니다. 배낭 3종은 본체 비율로 크기 위계를 구분합니다.

## 7. 블록 텍스처 5종
| id | 대상 | 수용 기준 |
|---|---|---|
| exploration_sleeping_bag | `textures/block/exploration_sleeping_bag_{head,foot,side}.png`, `textures/item/exploration_sleeping_bag.png` (16×16 블록, 32×32 아이템) | 올리브·황토 누빔 원단, 가죽 베개 롤. 블록 이음새 타일링 일치 |
| fish_trap | `textures/block/fish_trap_{top,side}.png`, `models/block/fish_trap.json` | 대나무·목재 격자 바구니, 수중 배치 시 위화감 없는 톤 |
| auto_farm | `textures/block/auto_farm_{front,side,top}.png`, `models/block/auto_farm.json` | 참나무·구리 프레임, 전면 투입구와 램프 |
| spider_egg_sac | `textures/block/spider_egg_sac.png`, `models/block/spider_egg_sac.json` | 반투명 거미줄 막 속 알 실루엣 |
| hanging_cocoon | `textures/block/hanging_cocoon.png`, `models/block/hanging_cocoon.json` | 천장 부착형 방추 고치, 거미줄 실 연결 |

## 8. 몹 텍스처 2종
| id | 크기 | 수용 기준 |
|---|---|---|
| juvenile_spider | 64×32 | 연갈색·회갈색 키틴 외피, 등줄기 연반점, 작은 선홍 홑눈. 바닐라 거미와 구별 |
| mother_spider | 64×32 (또는 128×64) | 흑자색 갑각, 복부 양측 맥동 알주머니 문양, 8안 발광 레이어 |

## 미결 항목 (구현 전 오너 확인)
1. **`feather_charm`**: 확정. 제작 대상에 포함하며 기존 액세서리 제외 규칙의 예외로 둡니다.
2. **T1 액세서리 7종**: 확정. 최종 아트로 제작합니다.
3. **단검·대검 14종**: 확정. 현재 코드에 등록이 없으므로 아이템 등록(id, 언어 키, 모델)부터 구현 범위에 포함합니다.
4. **`dark_energy_staff`**: 보류, 구현 제외.
5. **9번 명칭(ko_kr 누락 65개, en_us 연구 제목 60개)**: 키 목록 대조 전이라 이 문서에 포함하지 않았습니다.

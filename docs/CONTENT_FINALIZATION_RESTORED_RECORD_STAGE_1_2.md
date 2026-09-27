# MoreMinecrafts Content Finalization 복원 기록 - Stage 1-2

상태 주의: 이 복원 기록의 Enhancement +30/+40은 Stage 10 Enhancement 2.0 재설계 및 Stage 2에서 명시적으로 superseded 처리된 역사적 결정이다. 나머지 복원 내용은 해당 결정의 증거 기록으로 보존한다.

상태: RECOVERED DECISION RECORD
복원 기준점: GitHub 최초 체크리스트 생성 커밋 5e697f6683c1e86915f121883d3fd6f21f60c2cc.
복원 범위: Content Finalization Checklist의 1. Resource / Mining / Materials 시작부터 2. Equipment Progression 종료까지.
제외 범위: 체크리스트 생성 이전의 Paper 개발사와 당시 이미 폐기되어 있던 Promotion/승급, Coin/Fragment 성장 등 구형 시스템.
목적: 체크리스트의 상태표시만 보존하는 것이 아니라, 체크리스트 시작 이후 실제로 무엇을 결정했고 무엇을 보류·폐기했는지를 복원한다.


상태 표기
[LOCKED] 현재 콘텐츠 설계 기준.
[PROVISIONAL] 현재 기준값이지만 구현/플레이테스트에서 조정 가능.
[IMPLEMENTATION] 콘텐츠 정체성은 정해졌으나 Fabric 구현 세부는 후속 단계.
[OPTIONAL] 후보로 보존하되 현재 baseline을 막지 않음.
[DISCARDED] 체크리스트 시작 이후 검토했으나 현재 설계에서는 폐기.
[REVIEW-COMPLETE] 기존 자료와 대조 완료.


1. 체크리스트 시작점


Content Finalization은 Mod migration과 분리된 작업으로 시작했다.
목표는 최종 Paper와 기존 설계 위에서 MoreMinecrafts가 실제로 무엇을 만들 것인지 먼저 확정하고, Fabric 구현 방식은 나중에 다시 설계하는 것이었다.
진행 순서는 Resource → Equipment → Coating → Utility → Fishing → Alchemy → Mobs → Boss → World.
각 항목을 하나씩 닫고 체크리스트 상태를 갱신하는 방식으로 진행했다.


체크리스트 최초 생성 시점부터 이미 다음은 폐기 상태였다:
- Promotion/승급 시스템.
- Coin/Fragment 기반 장비 성장.
따라서 이 둘의 과거 설계는 이번 복원 범위에 포함하지 않는다.


2. Stage 1 - Resource / Mining / Materials 복원


2.1 최초 열린 질문
체크리스트 시작 시 Resource에서 열려 있던 핵심 질문은 최종 광석 roster, 차원, 높이/분포, 희귀도, 광맥/노출, Silk Touch/Fortune, 가공 단계, Mythril 월드젠, Adamantium survival 획득, fantasy mineral 획득 방식, recipe 수량, 실제 체감 공급량이었다.
초기 제안 중 Adamantium을 일반 월드젠에서 제외하는 안도 있었지만 이 단계에서 확정되지 않았고, 이후 Nether 자연 월드젠으로 변경·확정되었다.


2.2 최종 광석 roster
[LOCKED]
- Titanium
- Tungsten
- Mythril
- Adamantium
- Etherium


[LOCKED] 다섯 재료 모두 지하 자연 월드젠으로 survival 획득한다.
[LOCKED] Aluminum과 Aquamarine은 이 최종 광석 roster에 포함하지 않는다.
[LOCKED] fantasy mineral도 보스/구조물 전용 획득으로 잠그지 않고 기본적으로 채굴 가능한 자원으로 둔다.


2.3 차원과 높이
[LOCKED] 차원:
- Titanium / Tungsten / Mythril / Etherium = Overworld.
- Adamantium = Nether.


[PROVISIONAL] 생성 분포 baseline:
- Titanium: Y -16~80, triangular peak Y 32.
- Tungsten: Y -48~32, triangular peak Y -16.
- Mythril: Y -64~0, triangular peak Y -48.
- Etherium: Y -32~64, triangular peak Y 16.
- Adamantium: Ancient Debris-like worldgen draft.


[LOCKED] Titanium/Tungsten은 MoreMinecrafts 고유 높이/분포를 유지한다.
[PROVISIONAL] 두 광물의 vein size/placement 관련 출발 규칙은 Iron 계열을 기준으로 동기화한다. Iron의 전체 worldgen을 그대로 복제한다는 뜻은 아니다.


2.4 희귀도와 광맥
[LOCKED-CONCEPT] Titanium/Tungsten은 현실계의 비교적 흔한 금속으로 취급하며 체감 공급 목표는 Iron보다 흔하고 Copper보다 희귀한 구간이다.
[LOCKED-CONCEPT] Mythril/Etherium은 Diamond보다 희귀해야 한다.
[PROVISIONAL] Mythril/Etherium placement attempt/frequency baseline = 대응 Diamond 값 × 0.67.
[PROVISIONAL] Mythril/Etherium max vein = 6 blocks.
[PROVISIONAL] Adamantium은 Ancient Debris와 비슷한 희귀도/배치 감각을 출발점으로 한다.


중요한 밸런스 메모:
Titanium/Tungsten의 Iron-like vein/placement 출발 규칙과 “Iron보다 흔한 체감 목표” 사이에는 실제 구현 결과에 따라 조정이 필요할 수 있다. 따라서 배치 상수는 최종 경제 수치가 아니라 플레이테스트 가능한 baseline으로 남겼다.


2.5 채굴 드롭과 Fortune/Silk Touch
[LOCKED] Silk Touch는 신규 광물 전체에 같은 원칙을 적용하여 ore block 자체를 획득한다.


[LOCKED] Titanium/Tungsten:
- 현대 Iron/Gold형 획득 구조.
- 일반 채굴 → raw material.
- Fortune 적용.
- raw material → smelting → ingot.


[LOCKED] Mythril/Etherium:
- Diamond형 획득 구조.
- 일반 채굴 → 완성 material 직접 드롭.
- Fortune 적용.
- Diamond 계열 XP drop.
- 별도 smelting 없음.


[LOCKED] Adamantium:
- Ancient Debris형 획득 구조.
- Fortune으로 수량 증가하지 않음.
- 채굴한 debris/ore-type block을 제련하여 사용 material 획득.
- 정확한 중간재 이름과 one-step/scrap-like 세부는 recipe 설계 때 확정.


2.6 가공 chain
[LOCKED]
- Titanium Ore → Raw Titanium → Titanium Ingot.
- Tungsten Ore → Raw Tungsten → Tungsten Ingot.
- Mythril Ore → Mythril material.
- Etherium Ore → Etherium material.
- Adamantium ore/debris-type block → smelted Adamantium material.


2.7 Resource 단계에서 의도적으로 닫지 않은 숫자
[PROVISIONAL] “재료별 장비 recipe 수량”은 실제 장비/Utility recipe가 아직 닫히지 않았으므로 숫자를 만들어 넣지 않는다.
[PROVISIONAL] 실제 체감 공급 balance는 상대 목표까지 닫고 절대 생성량은 구현 후 검증한다.
검증 대상은 chunk당 생성량, 탐색 시간, 노출 빈도, Fortune 후 시간당 획득량, recipe 소비량과 공급량 충돌, 특정 Y대 과밀 여부다.


Resource 단계 결론:
콘텐츠 구조와 상대 공급 목표는 닫혔다. recipe 소비량과 실제 월드에서의 생성 체감은 후속 밸런스 대상으로 이관했다.


3. Stage 2 - Equipment Progression 복원


3.1 Tier 구조 해석
[LOCKED] T1 Vanilla / T2 Elemental / T3 Special / T4 Endgame.
[LOCKED] 이 표기는 진행 트리나 진화 체계가 아니라 서로 독립적인 설계 카테고리다.
[LOCKED] T1→T2→T3→T4 장비 상속/진화를 기본 구조로 사용하지 않는다.
[LOCKED] 특정 T4 recipe 또는 unlock이 T3 장비를 재료/조건으로 요구할 수는 있다. 이것은 recipe/condition dependency이며 T3→T4 진화가 아니다.
[LOCKED] Enhancement = 주로 수치 성장, Coating = 기능적 성장, Enchantment = Minecraft-native enchant 체계 확장.


3.2 T2 Elemental
[LOCKED] 최종 roster는 6종:
- Fire: 불꽃검 / Sword.
- Water: 흐르는 물의 검 / Sword.
- Wind: 바람을 가르는 검 / Sword.
- Wind: 바람궁수의 활 / Bow.
- Earth: 대지의 철퇴 / Mace.
- Dark Energy: 마기가 깃든 지팡이 / Staff.
- Ice: T2 장비 없음.


[LOCKED] Paper 능력은 코드나 수치를 복제하는 것이 아니라 각 장비의 핵심 gameplay identity/ability concept을 계승한다.
[IMPLEMENTATION] 모델, texture, animation, VFX/SFX, 정확한 damage/range/cooldown/duration, key input, hit judgement는 Fabric 단계에서 재설계한다.
[IMPLEMENTATION] Earth와 Dark Energy는 carrier weapon type 변경에 맞춰 능력 정체성을 새 무기 타입에 맞게 재구성한다.


3.3 T3 Special
[LOCKED] “T3 전부 유지” 결정으로 7종을 보존한다:
- 타나토스의 철퇴.
- 포세이돈의 창.
- 화염 도끼.
- 뇌신의 도끼.
- 성검 · 솔라리스.
- 월야잔광.
- 빙결검.


[LOCKED] 빙결검은 삭제된 T2 Ice와 별개의 T3 Special weapon이다.
[LOCKED] T3도 핵심 identity/ability concept은 유지하고 Fabric에서 표현·세부 구현을 재해석한다.
[IMPLEMENTATION] 모델/VFX/SFX/정확한 수치/input/hit detection은 이 단계에서 동결하지 않는다.


3.4 T4를 정하는 과정에서 생긴 기준
T4 후보를 여러 차례 검토했지만 많은 안이 “T3에 더 복잡한 능력과 VFX를 얹은 것”처럼 느껴진다는 이유로 채택되지 않았다.
[LOCKED] T4는 장비 하나가 독립적인 전투 시스템 또는 완성된 무기군에 가까운 개념적 무게를 가져야 한다.
[LOCKED] Switching/Multi-form은 강력한 T4 설계 언어지만 모든 T4에 의무적으로 적용하지 않는다.


[DISCARDED] 현재 T4 authoritative roster가 아닌 검토안:
공간 절단검, 거울/잔상 무기, 낙성창, 공허 사슬, 궤도 칼날, 지형 각성 망치, 바다 가르는 삼지창, 태양 대검, 시간 낫, 성좌 활, 거대/중량 망치, 하늘/중력 창, End/Genesis Pickaxe, Portal Staff, Fortress Shield, 변형 무기, Resonance/Gravity 계열, “무명” 적응형 무기, Return-point 장비.
이 목록은 다시 후보로 자동 부활시키지 않는다.


3.5 T4 baseline
[LOCKED] Equipment 단계 마감 baseline = 무극검 / 천궁 / 천서 3종.
[OPTIONAL] 세계수 활은 별도 후보로 보존하며 필요하면 T3로 내릴 수 있다.


3.6 무극검
[LOCKED] 소켓 기반 T4 sword.
[LOCKED] 무소켓 기본 상태도 유효한 균형형 상태.
[LOCKED] UI를 열어 socket을 장착/관리한다.
[LOCKED] 조건을 갖추면 두 원소를 동시에 다루는 연출/운용 가능성을 허용한다.
[LOCKED] 5 forms = Fire / Water / Wind / Earth / Lightning.
[LOCKED] 공통 비교축 = 공격력 / 공격속도 / 공격 범위 / 기동성 / 지속력.
[LOCKED] 사거리는 공통 비교축에서 제외. 충격력은 Earth 같은 형태의 고유 mechanic으로 사용.


Fire:
[LOCKED-CONCEPT] 공격력과 공격 범위가 매우 높고 공격속도/기동성/지속력을 희생. 바닐라 sword 전후의 속도감으로, 과도하게 느린 대검형은 아님. Splash Damage가 핵심 passive.


Water:
[LOCKED-CONCEPT] 높은 공격속도와 기동성, 낮은 per-hit damage, 높은 sustain. “싸움을 멈추지 않는 장기전 specialist”가 정체성.
[PROVISIONAL] 연속 hit/movement로 Flow를 쌓아 mobility와 회복으로 연결하는 아이디어는 후보일 뿐 고정하지 않음.


Wind:
[LOCKED-CONCEPT] 전방 sword wave. 공격력/범위/속도가 모두 좋지만 specialist보다 최고점은 낮은 balanced generalist. spacing/거리 제어 담당.
[PROVISIONAL] double-hit sweet spot 또는 piercing sword wave 같은 세부안은 미확정.


Earth:
[LOCKED-CONCEPT] 매우 높은 impact와 공격 범위, 명중 시 큰 damage, 느린 속도, miss penalty, hunger 소비. “한 번의 공격이 사건”인 high-risk huge-hit form.
[PROVISIONAL] 정확한 whiff recovery, 이동 제한 등은 후속 조정.


Lightning:
[LOCKED-CONCEPT] 매우 빠른 속도/기동성과 높은 damage를 짧은 burst에 집중하고 sustain은 낮음.
[PROVISIONAL] charge → burst engage → disengage → recharge 구조는 후보이며 미확정.


3.7 천궁
[LOCKED] 하나의 bow가 firing mode를 전환한다.
[LOCKED] mode = 속사형 / 강궁형 / 산탄형 / 곡사형.
[LOCKED] 전환 방식은 무기 자체 설계에서 이미 결정되었으며 좌클릭 cycle, 우클릭 draw/fire 구조.


속사형:
[LOCKED-CONCEPT] 빠른 연속 사격.


강궁형:
[LOCKED-CONCEPT] 화살 모델 자체를 크게 하고 실제 공격 판정 영역도 크게 느껴지는 high-impact projectile.
[PROVISIONAL] piercing은 선택 가능성이 있었으나 확정하지 않음.
[PROVISIONAL] 용 머리는 알아볼 수 있게 모델링하고 몸통은 불꽃/먹선처럼 거친 energy trail로 만드는 VFX 아이디어를 보존한다. 완전한 관절형 용 entity를 만드는 설계는 아님.


산탄형:
[LOCKED-CONCEPT] 여러 화살을 spread 형태로 발사.


곡사형:
[LOCKED-CONCEPT] 직사 projectile이 아님. draw 중 camera/view direction을 기준으로 지면에 magic-circle-like predicted impact area를 표시하고, release 시 해당 영역에 위에서 “천궁 화살비”가 떨어진다.


3.8 천서
[LOCKED] 원소를 바꾸는 무기가 아니라 magic output form 자체를 전환하는 T4 magic implement.
[LOCKED] forms = 광선 / 마탄 / 영역 / 구조체.
[LOCKED] 네 form의 역할 언어 = 지속 / 축적 / 설치 / 구축.
[LOCKED-CONCEPT] 광선 = continuous precise beam.
[LOCKED-CONCEPT] 마탄 = 주변/후방에 여러 orb를 생성·축적 후 동시 방출.
[LOCKED-CONCEPT] 영역 = 지면/공간에 지속형 control field 설치.
[LOCKED-CONCEPT] 구조체 = 하나의 큰 magical weapon/structure를 구축하여 강한 단발 효과.
[LOCKED] visual identity는 천궁의 거칠고 신화적인 마법과 반대로 정밀한 circle/line/triangle/glyph 기반의 “정교하고 초월적인 마법공학”.
[PROVISIONAL] 영역의 압축장, 구조체의 거대 천창 등 구체안은 후보이며 최종 mechanic은 아님.
[IMPLEMENTATION] 정확한 input과 판정/VFX/수치는 후속 구현.


3.9 세계수 활
[OPTIONAL] 현재는 T4 후보지만 필요하면 T3로 내릴 수 있다.
[LOCKED-CONCEPT] switching weapon으로 강제하지 않는다.
[LOCKED-CONCEPT] 화살 착탄 지점에서 생명/세계수 계열 growth를 만들고 전장/환경 자체를 변화시키는 것이 핵심.
[PROVISIONAL] seed → growth → world tree → absorb/reclaim 순환은 후보.


3.10 T4 획득 원칙
[LOCKED] T4 획득의 중심은 rare material recipe.
[LOCKED] exploration chest/탐험 보상으로 완제품을 얻는 방식을 중심축으로 삼지 않는다. 탐험 보상 중심으로 만들면 다시 “탐험 지옥”이 되는 문제를 피한다.
[LOCKED] boss-linked material, special unlock/requirement 등은 recipe 접근 조건으로 열어둘 수 있다.
[PROVISIONAL] 정확한 material과 수량은 Resource 공급량 및 후속 콘텐츠가 닫힌 뒤 조정한다.


3.11 Enhancement
[LOCKED]
- [SUPERSEDED / HISTORICAL] Vanilla/T1 max +30.
- [SUPERSEDED / HISTORICAL] T2 Elemental max +40.
- T3 Special 일반 Enhancement 불가.
- T4 Endgame 일반 Enhancement 불가.
- Anvil + Upgrade Stone + Minecraft XP.
- 성공률 100%.
- 실패 없음.
- downgrade/destruction 없음.
- 실패 누적 correction/pity 없음.


[PROVISIONAL] 현재 Paper cost baseline:
- +1~5: Upgrade Stone 1 + XP 1 level.
- +6~10: Upgrade Stone 1 + XP 2 levels.
- +11~20: Upgrade Stone 1 + XP 3 levels.
- +21~30: Upgrade Stone 1 + XP 4 levels.
- +31~40: Upgrade Stone 1 + XP 5 levels.


비용을 provisional로 둔 이유:
장비 획득 밸런스의 가장 큰 축을 recipe/material cost로 두기로 했으므로 지금 XP/stone 비용을 과도하게 최적화할 필요가 없다.


3.12 Paper Special Equipment vs Content Freeze v3
[REVIEW-COMPLETE] v3 ZIP을 재검증한 결과 02_EQUIPMENT.md는 T2/T3/T4 weapon spec이 아니라 Utility Equipment 문서.
[REVIEW-COMPLETE] 01_CORE_PRINCIPLES.md가 보존하는 무기 원칙은 T1 Vanilla / T2 Elemental / T3 Special / T4 Endgame과 Enhancement/Coating이 다음 Tier 진입 필수조건이 아니라는 점.
[REVIEW-COMPLETE] Freeze v3에는 타나토스/포세이돈/솔라리스 등 개별 Special roster/ability spec이 없다.
[LOCKED] 따라서 개별 T3 source of truth는 최종 Paper special-equipment 자료와 이번 Equipment finalization 결정. Freeze v3와 충돌하는 개별 Special weapon 설계는 없음.


4. Stage 1-2 마감 상태


Stage 1 Resource:
- 콘텐츠 구조와 상대 공급 기준 복원 완료.
- recipe 수량과 실제 생성 체감은 PROVISIONAL/후속 balance.


Stage 2 Equipment:
- Tier 해석, T2 roster, T3 roster, T4 baseline, Enhancement 구조, 획득 원칙, Freeze v3 review 복원 완료.
- 모델/VFX/input/세부 전투 수치와 recipe 수량은 후속 IMPLEMENTATION/PROVISIONAL.


이 기록은 체크리스트를 대체하지 않는다.
체크리스트는 진행 상태판이며, 이 문서는 체크리스트 시작 이후 Stage 1-2에서 실제로 확정·보류·폐기된 내용을 복원한 decision record다.
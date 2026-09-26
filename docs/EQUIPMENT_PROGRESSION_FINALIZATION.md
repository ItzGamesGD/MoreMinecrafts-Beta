# MoreMinecrafts Equipment Progression 확정안

> **상태:** 콘텐츠 설계 단계 마감 / Equipment Progression Freeze.  
> 이 문서는 체크리스트가 아니라 Equipment 단계에서 실제로 확정한 설계 내용을 보존하는 authoritative design record다.  
> 정확한 수치, VFX, 입력 구현, 제작 수량처럼 플레이테스트와 Fabric 구현에 종속되는 값은 **[PROVISIONAL]** 또는 **[IMPLEMENTATION]**으로 분리한다.

## 1. 장비 티어 구조

- **[LOCKED]** 무기 분류는 **T1 Vanilla / T2 Elemental / T3 Special / T4 Endgame**.
- **[LOCKED]** 네 Tier는 설계 카테고리이며 T1→T2→T3→T4의 진화·승급 트리가 아니다.
- **[LOCKED]** 서로 다른 Tier 사이의 장비 상속/진화는 기본적으로 금지한다.
- **[LOCKED]** 특정 T4의 제작 재료 또는 해금 조건으로 T3 장비가 요구될 수는 있으나, 이는 recipe/condition dependency일 뿐 T3가 T4로 진화하는 구조가 아니다.
- **[LOCKED]** Enhancement는 주로 수치 성장, Coating은 기능적 성장, Enchantment는 Minecraft native enchant 체계 중심 확장이다.
- **[LOCKED]** Promotion, Coin, Fragment 기반 장비 성장 체계는 사용하지 않는다.
- **[LOCKED]** 갑옷은 무기보다 적은 수의 큰 Tier를 사용한다.

## 2. T2 Elemental 최종 roster

| Element | Equipment | Base type |
|---|---|---|
| Fire | 불꽃검 | Sword |
| Water | 흐르는 물의 검 | Sword |
| Wind | 바람을 가르는 검 | Sword |
| Wind | 바람궁수의 활 | Bow |
| Earth | 대지의 철퇴 | Mace |
| Lightning | 번개의 검 | Sword |
| Dark Energy | 마기가 깃든 지팡이 | Staff |
| Ice | 없음 | 없음 |

- **[LOCKED]** 총 7종, 6개 속성 계열.
- **[LOCKED]** T2는 Paper에서 정립된 각 장비의 핵심 gameplay identity와 능력 컨셉을 계승하되, 최종 무기 타입과 전투 구조는 Fabric 설계에서 재구성할 수 있다.
- **[PROVISIONAL]** T2 각 무기는 기본적으로 **고유 액티브 스킬 1개 + 패시브/기타 고유 능력** 구조를 목표로 한다. 정확한 발동 조건과 수치는 후속 설계/플레이테스트에서 조정 가능하다.
- **[PROVISIONAL]** 모든 T2 원소검은 기본 공격 자체에 해당 원소를 식별할 수 있는 전용 VFX를 가진다. 기본 타격 VFX는 작게, 패시브 발동과 액티브 스킬은 단계적으로 더 큰 연출을 사용한다.
- **[IMPLEMENTATION]** Paper 코드를 그대로 이식하지 않는다. 모델, 텍스처, 애니메이션, VFX/SFX, 정확한 공격력·범위·쿨다운·지속시간, 입력 키, 판정 방식은 Fabric 구현 단계에서 재설계한다.
- **[IMPLEMENTATION]** Earth와 Dark Energy는 carrier weapon type이 바뀌었으므로 기존 능력 정체성을 새 무기 타입에 맞게 재구성한다.

### 2.1 T2 원소검 전투 설계 — 임시 확정

#### Fire — 불꽃검
- **[PROVISIONAL]** 기본 공격 적중 시 화염 계열 VFX가 발생한다.
- **[PROVISIONAL]** 별도 쿨다운 없이 **10회 공격 적중마다** 불꽃이 가로로 크게 휩쓰는 연출과 함께 범위 공격이 발동한다.
- **[PROVISIONAL]** 우클릭 액티브는 **30초 쿨다운**의 강력한 **사선 화염 베기**다. 일반 10타 패시브보다 명확히 강한 일격/범위 공격으로 설계한다.
- **[SUPERSEDED-PROTOTYPE]** 현재 Fabric의 Flame Sword splash/Flame Cleave 구현은 기반 프로토타입이며, 위 설계에 맞춰 후속 구현에서 재조정한다.

#### Water — 흐르는 물의 검
- **[PROVISIONAL]** 기본 공격속도가 빠르며, 기본 공격에 물/수류 계열 VFX가 발생한다.
- **[PROVISIONAL]** 적 처치 시 체력을 소량 회복한다.
- **[PROVISIONAL]** 우클릭 시 먼저 위로 튀어오른 뒤 바라보는 방향으로 내려찍고, 착지/타격 지점 기준 약 **2블록 앞에 강력한 물기둥**을 소환한다.
- **[BALANCE]** 액티브 쿨다운, 이동량, 물기둥 피해량/범위는 후속 밸런싱 대상이다.

#### Wind — 바람을 가르는 검
- **[PROVISIONAL]** 기본 공격속도가 빠르며, 기본 공격에 바람 계열 VFX가 발생한다.
- **[PROVISIONAL]** **10회 공격 적중마다** 바람이 넓게 휘날리는 범위 공격이 발동한다.
- **[PROVISIONAL]** 범위에 닿은 적 각각에 Sweep Attack 계열 효과를 소환하는 방식으로 빠른 다중 참격 느낌을 표현한다.
- **[IMPLEMENTED / PROVISIONAL VALUES]** 최종 단일 액티브는 우클릭 **Wind Blade**다. Wind Step은 최종 T2 kit에서 제거되었다.

#### Lightning — 번개의 검
- **[PROVISIONAL]** 기본 공격에 전기/스파크 계열 VFX가 발생한다.
- **[PROVISIONAL]** 연속 공격 적중으로 전하를 누적하고, 일정 누적치에 도달하면 추가 번개 피해가 발생한다.
- **[PROVISIONAL]** 고유 액티브는 **뇌격참**이다. 짧은 전진/돌진성 베기와 함께 전방에 순간적인 번개 피해를 집중하는 burst형 스킬로 설계한다.
- **[LOCKED]** 전하 임계치는 성공한 5타이며 시간 경과로 감소하지 않고 발동 후 0으로 초기화한다.
- **[BALANCE]** 추가 피해, 액티브 쿨다운과 이동량은 후속 밸런싱 대상이다.

#### T2 검 공통 VFX 원칙
- **[PROVISIONAL]** 기본 공격 VFX는 원소 정체성을 보여주되 시야를 가리지 않는 소규모 연출로 제한한다.
- **[PROVISIONAL]** 연출 위계는 **기본 공격 < 패시브/누적 발동 < 액티브 스킬** 순으로 커진다.
- **[IMPLEMENTATION]** 실제 particle 종류, trail/mesh/model, sound, 색상과 성능 예산은 Fabric 구현 단계에서 확정한다.

#### 아직 별도 설계가 필요한 T2
- **[INTENTIONALLY OMITTED]** 바람궁수의 활: Checkpoint 12에서는 homing/steering을 의도적으로 구현하지 않는다.
- **[IMPLEMENTED / PROVISIONAL VALUES]** 대지의 철퇴: Smash 성공 시 좁은 전방 균열(피해+둔화), 우클릭 시 넓은 전방 Earth Shatter(피해+변위)를 사용하며 블록을 파괴하지 않는다.
- **[DEFERRED]** 마기가 깃든 지팡이: Paper의 약화/흡혈/Wither identity를 참고하되 현재 설계는 보류한다.

## 3. T3 Special 최종 roster

**[LOCKED] T3 Special 7종 전부 유지**

1. 타나토스의 철퇴
2. 포세이돈의 창
3. 화염 도끼
4. 뇌신의 도끼
5. 성검 · 솔라리스
6. 월야잔광
7. 빙결검

- **[LOCKED]** 빙결검은 삭제된 T2 Ice 장비와 별개인 강한 존재감의 T3 Special weapon이다.
- **[LOCKED]** T3는 고유 정체성과 능력의 핵심 컨셉을 유지하되 Fabric에서 표현과 구현을 재해석한다.
- **[IMPLEMENTATION]** 모델/VFX/SFX/세부 수치/입력/히트 판정은 현 단계에서 동결하지 않는다.

## 4. T4 Endgame 최종 baseline roster

**[LOCKED] Equipment 단계 마감 기준 T4 baseline = 3종**

1. **무극검**
2. **천궁**
3. **천서**

- **[LOCKED]** T4는 T2 또는 T3의 업그레이드 버전이 아니라 독립적인 Endgame equipment category다.
- **[LOCKED]** T4는 단순히 T3에 더 화려한 VFX를 붙인 장비가 아니라, 장비 하나가 독립적인 전투 시스템 또는 완성된 전투군을 제공할 정도의 개념적 무게를 가져야 한다.
- **[LOCKED]** Switching/Multi-form은 T4에서 중요한 설계 언어가 될 수 있지만 모든 T4에 강제되는 규칙은 아니다.

### 4.1 무극검

- **[LOCKED]** 소켓 기반 Endgame sword.
- **[LOCKED]** 기본 무소켓 상태도 균형형으로 유효한 완성 빌드.
- **[LOCKED]** UI에서 소켓을 장착해 전투 타입을 변경.
- **[LOCKED]** 조건 충족 시 두 원소를 동시에 운용하는 형태/연출 허용.
- **[LOCKED]** 형태: **Fire / Water / Wind / Earth / Lightning**.
- **[LOCKED]** 공통 비교축: **공격력 / 공격속도 / 공격 범위 / 기동성 / 지속력**. 사거리는 공통 축에서 제외. 충격력은 Earth 같은 특정 형태의 고유 특성.

**Fire**
- 높은 공격력과 넓은 공격 범위.
- Splash Damage가 핵심 passive.
- 공격속도·기동성·지속력을 희생하되 터무니없이 느린 대검형은 아님. 바닐라 검 전후의 속도감을 기준으로 한다.

**Water**
- 높은 공격속도와 기동성.
- 낮은 1타 화력, 매우 높은 지속전 능력.
- 회복/이동속도 계열 정체성.
- 장기전과 끊기지 않는 전투 흐름이 핵심.

**Wind**
- 전방 검기 발사.
- 공격력·공격속도·공격 범위가 모두 준수하지만 각 전문 형태보다 최고점은 낮은 균형형.
- 거리와 공간을 다루는 형태.

**Earth**
- 충격력과 공격 범위가 압도적이며 명중 시 매우 큰 피해.
- 느린 공격속도.
- 빗나갔을 때도 큰 대가.
- 허기 소비.
- 한 번의 공격 자체가 사건이 되는 고위험·고위력 형태.

**Lightning**
- 매우 빠른 공격속도·기동성과 높은 공격력을 짧은 순간에 집중.
- 지속력이 약한 burst 형태.

**[PROVISIONAL]** 각 형태의 정확한 수치, 세부 passive/active, charge/flow 등 보조 메커니즘, VFX와 입력 세부는 구현·전투 폴리시 단계에서 확정한다.

### 4.2 천궁

- **[LOCKED]** 하나의 활이 발사 방식을 전환하는 T4 switching bow.
- **[LOCKED]** 4개 모드: **속사형 / 강궁형 / 산탄형 / 곡사형**.
- **[LOCKED]** 좌클릭으로 모드를 순환하고 우클릭 draw/fire 흐름을 공통으로 사용하는 단순한 조작 구조.

**속사형:** 짧은 간격으로 빠르게 연속 사격.  
**강궁형:** 큰 화살 자체가 시각적으로 커지고 실제 공격 판정 영역도 크게 느껴지는 고충격 projectile.  
**산탄형:** 여러 화살을 넓게 분산 발사.  
**곡사형:** draw 중 시선 방향을 기준으로 지면에 예상 타격 영역을 표시하고, release 시 해당 영역에 위에서 화살비가 쏟아진다.

- **[PROVISIONAL]** 전체 VFX는 강한 마법적/신화적 인상을 지향한다.
- **[PROVISIONAL]** 강궁형에 “용 머리는 알아볼 수 있게, 몸통은 불꽃·붓질처럼 거칠게 휘갈긴 에너지 trail”을 붙이는 아이디어를 보존한다. 확정 VFX는 아니며 완전한 관절형 용 entity를 요구하지 않는다.
- **[PROVISIONAL]** passive skill, mode 연계 보너스, projectile physics, 정확한 수치는 후속 구현에서 결정한다.

### 4.3 천서

- **[LOCKED]** 마법 속성이 아니라 **마법 출력 방식** 자체를 전환하는 T4 magic implement.
- **[LOCKED]** 4개 출력 형태: **광선 / 마탄 / 영역 / 구조체**.
- **광선:** 정밀한 지속형 beam 계열.
- **마탄:** 다수의 마력탄을 생성·축적한 뒤 방출하는 계열.
- **영역:** 지면/공간에 마법 영역을 설치하여 지속적으로 공간을 제어하는 계열.
- **구조체:** 하나의 큰 마법 무기 또는 구조물을 구축해 강한 단발 효과를 만드는 계열.
- **[LOCKED]** 네 형태는 **지속 / 축적 / 설치 / 구축**이라는 서로 다른 마법 사용 방식을 담당한다.
- **[LOCKED]** 시각적 정체성은 천궁의 거친 신화적 마법과 구별되는 정밀하고 기하학적인 초월적 마법공학 방향.
- **[IMPLEMENTATION]** 정확한 입력, 구조체 최종 형상, 수치, VFX 세부와 전투 판정은 Fabric 구현 단계에서 확정한다.

### 4.4 세계수 활

- **[OPTIONAL]** T4 baseline 3종에는 포함하지 않는다.
- **[OPTIONAL]** 강한 단일 특수무기 컨셉으로 보존하며, 향후 T4 추가 후보 또는 필요 시 T3로 재분류 가능.
- **[LOCKED-CONCEPT]** switching weapon으로 강제하지 않는다. 화살이 착탄한 위치에 생명/세계수 계열 성장을 만들어 실제 전장·환경을 변화시키는 것이 핵심.
- **[PROVISIONAL]** seed → growth → world tree → absorb/reclaim 순환 구조는 후보이며 세부 메커니즘은 미확정.

## 5. T4 아이디어 정리 원칙

- **[LOCKED]** “T3보다 화려한 특수기 하나” 수준의 아이디어는 T4로 채택하지 않는다.
- **[DISCARDED]** 이전 브레인스토밍의 공간 절단검, 거울/잔상 무기, 낙성창, 공허 사슬, 궤도 칼날, 지형 각성 망치, 바다 가르는 삼지창, 태양 대검, 시간 낫, 성좌 활, 거대/중량 망치, 하늘/중력 창, End/Genesis Pickaxe, Portal Staff, Fortress Shield, 변형 무기, Resonance/Gravity 계열, “무명” 적응형 무기, Return-point 장비 등은 현재 authoritative T4 roster가 아니다.

## 6. Enhancement 2.0 구조

- **[LOCKED]** T1 Vanilla와 T2 Elemental ordinary Enhancement는 모두 **+20**까지 성장한다.
- **[LOCKED]** 연속 성장과 +5/+10/+15/+20의 결정론적 milestone을 사용한다. Promotion/별/랜덤 옵션은 복원하지 않는다.
- **[LOCKED]** 성공률은 100%이며 실패, 하락, 파괴, reroll, pity, Coin 비용은 없다.
- **[LOCKED]** T3는 향후 별도 progression을 사용하고 T4는 처치 point 기반 Grade/Mastery를 사용하므로 ordinary Enhancement 대상이 아니다.
- **[LOCKED]** Anvil은 현재 임시 adapter이며 core transaction policy의 일부가 아니다.
- **[PROVISIONAL]** 공격력 곡선, milestone bonus, 도구 보조 효과, armor 산술(풀세트 약 8% 이하), 재료/XP 비용은 플레이테스트 대상이다.
- **[DEFERRED]** Bow draw speed, Crossbow reload speed와 Mace smash 공식은 26.3 integration 검증 및 balance 결정 후 구현한다.

현재 임시 비용은 일반 단계 Stone 1/XP 0, milestone 단계 Stone 2/XP 1로 중앙 policy에 격리되어 있다. 최종 economy가 아니다.

## 7. 장비 제작 / 획득 비용 원칙

- **[LOCKED]** 장비 획득 밸런스의 핵심은 Coin이나 실패 확률이 아니라 실제 재료를 사용하는 제작 recipe다.
- **[LOCKED]** T4는 탐험 상자에서 완제품을 단순 보상으로 지급하는 방식을 핵심 획득축으로 삼지 않는다.
- **[LOCKED]** T4는 희귀 재료를 사용하는 제작 recipe를 핵심 획득축으로 둔다.
- **[LOCKED]** 특정 boss drop 또는 특수 해금 조건이 recipe/제작 접근 조건으로 연결될 가능성은 열어둔다.
- **[PROVISIONAL]** T2/T3/T4의 정확한 재료 수량과 recipe cost 숫자는 지금 고정하지 않는다. Resource 공급량과 후속 콘텐츠의 실제 획득 속도가 닫힌 뒤 밸런싱한다.

## 8. Paper Special Equipment vs Content Freeze v3 대조

- **[REVIEW-COMPLETE]** Content Freeze v3의 `02_EQUIPMENT.md`는 T2/T3/T4 개별 무기 명세가 아니라 Utility Equipment 설계 문서다.
- **[REVIEW-COMPLETE]** Freeze v3의 `01_CORE_PRINCIPLES.md`에서 무기 관련 authoritative 구조는 T1 Vanilla / T2 Elemental / T3 Special / T4 Endgame 및 Enhancement/Coating이 다음 Tier 진입의 필수 선행조건이 아니라는 원칙이다.
- **[REVIEW-COMPLETE]** Freeze v3에는 타나토스, 포세이돈, 솔라리스 등 개별 Paper Special Equipment roster/능력 명세가 없다.
- **[LOCKED]** 따라서 개별 T3의 source of truth는 최종 Paper special-equipment 자료와 이번 Equipment finalization 결정이며, Freeze v3와 충돌하는 개별 Special Equipment 설계는 없다.

## 9. 구현 단계로 이관하는 항목

- **[IMPLEMENTATION]** 모델, 텍스처, 애니메이션, VFX/SFX.
- **[IMPLEMENTATION]** 정확한 공격력, 공격속도, 범위, 지속시간, cooldown.
- **[IMPLEMENTATION]** 입력 시스템과 key mapping.
- **[IMPLEMENTATION]** hit detection, projectile physics, 상태효과 수치.
- **[IMPLEMENTATION]** T2/T3 Paper 능력의 Fabric-native 재구현.
- **[IMPLEMENTATION]** T4 각 모드의 세부 combat feel.
- **[PROVISIONAL]** Enhancement XP 비용과 모든 equipment recipe의 정확한 재료 수량.

## 10. 단계 마감 판정

- **[LOCKED]** T2 roster와 base type 확정.
- **[LOCKED]** T3 7종 유지/재해석 원칙 확정.
- **[LOCKED]** T4 baseline 3종과 각 핵심 형태 확정.
- **[LOCKED]** Enhancement 상한/적용 대상/성공·실패 구조 확정.
- **[PROVISIONAL]** Enhancement 비용 수치와 recipe 수량은 후속 밸런스 조정 가능.
- **[REVIEW-COMPLETE]** Paper Special Equipment와 Freeze v3 대조 완료.

**Equipment Progression 콘텐츠 설계 단계는 이 문서를 기준으로 마감한다.**

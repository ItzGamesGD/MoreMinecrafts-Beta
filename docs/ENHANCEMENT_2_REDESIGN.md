# MoreMinecrafts Enhancement 2.0 재설계안

> **Status:** Stage 10 equipment-growth redesign baseline  
> **Authority:** 이 문서는 기존 `+30 Vanilla / +40 Elemental / Anvil+XP 고정` Enhancement 설계를 대체하는 새 기준 기록이다.  
> 충돌 시 이 문서의 Enhancement 관련 결정이 기존 `EQUIPMENT_PROGRESSION_FINALIZATION.md` 및 recovery 문서의 구 Enhancement 절보다 우선한다.  
> 장비 roster, T2/T3/T4 개별 정체성 등 Enhancement 이외의 기존 확정사항은 그대로 유지한다.

## 1. 재설계 목적

Enhancement의 목적을 단순한 미세 수치 누적에서 **장비를 실제로 키우는 성장 루프**로 변경한다.

핵심 목표는 다음 네 가지다.

1. 한 번의 강화가 너무 무의미하지 않을 것.
2. 장기적으로 장비를 키우는 재미가 있을 것.
3. 전투력 인플레이션을 통제할 것.
4. T1 → T2 → T3 → T4의 장비 정체성과 교체 이유를 훼손하지 않을 것.

기존 Promotion의 유용한 역할은 Enhancement milestone으로 흡수하되 Promotion 자체는 복원하지 않는다.

## 2. 상태 표기

- **[LOCKED]** 구조적으로 확정된 방향.
- **[PROVISIONAL]** 현재 기준값이지만 플레이테스트 및 구현 검증 후 조정 가능.
- **[DEFERRED]** 의도적으로 다음 설계 단계로 넘긴 항목.
- **[REMOVED]** 기존 구조에서 명시적으로 폐기된 항목.

## 3. Ordinary Enhancement의 새 공통 구조

### 3.1 최대 강화

- **[LOCKED]** T1 Vanilla ordinary Enhancement 최대치는 현재 **+20**.
- **[LOCKED]** T2 Elemental ordinary Enhancement 최대치도 현재 **+20**.
- **[LOCKED]** T2가 T1보다 강한 이유를 추가 강화 레벨 수로 만들지 않는다.
- **[LOCKED]** T1/T2 차이는 기본 스탯, 고유 능력, 장비 정체성에서 만든다.

기존의 Vanilla +30 / Elemental +40은 더 이상 authoritative rule이 아니다.

### 3.2 Milestone

- **[LOCKED]** milestone은 **+5 / +10 / +15 / +20**.
- **[LOCKED]** milestone 효과는 확정형이며 랜덤 옵션이 아니다.
- **[LOCKED]** 모든 장비가 정확히 옵션 2개를 가져야 하는 것은 아니다.
- **[LOCKED]** 장비군에 따라 의미 있는 보조 성장축은 1개, 2개, 필요한 경우 최대 3개까지 허용한다.
- **[LOCKED]** milestone 상태는 가능한 한 `enhancement_level`에서 파생한다.
- **[REMOVED]** 별도 Promotion level, 별, 랜덤 옵션 데이터는 복원하지 않는다.

개념 구조:

`+1 +2 +3 +4 [★+5] +6 +7 +8 +9 [★+10] +11 +12 +13 +14 [★+15] +16 +17 +18 +19 [★+20]`

## 4. 확률 및 실패 정책

다음은 ordinary Enhancement에서 사용하지 않는다.

- **[REMOVED]** 성공 확률
- **[REMOVED]** 실패
- **[REMOVED]** 강화 하락
- **[REMOVED]** 장비 파괴
- **[REMOVED]** 실패 누적 보정 / pity
- **[REMOVED]** 랜덤 옵션
- **[REMOVED]** 옵션 reroll
- **[REMOVED]** Promotion Stone
- **[REMOVED]** Coin 비용

ordinary Enhancement는 계속 **결정론적 progression**으로 유지한다.

## 5. 장비군별 성장 방향

### 5.1 Sword

- **[LOCKED]** 주 성장축은 공격 피해.
- **[LOCKED]** milestone 보조 효과는 1~2개 정도의 단순한 확정 성장으로 제한한다.
- **[LOCKED]** multiplicative DPS를 여러 축에서 동시에 과도하게 키우지 않는다.

현재 검토 중인 연속 공격력 곡선:

| 구간 | 단계당 공격 피해 | 구간 종료 누적 |
|---|---:|---:|
| +1~+5 | +0.10 | +0.50 |
| +6~+10 | +0.12 | +1.10 |
| +11~+15 | +0.14 | +1.80 |
| +16~+20 | +0.16 | +2.60 |

- **[PROVISIONAL]** 위 수치는 밸런스 기준점이며 최종 고정 수치가 아니다.
- **[PROVISIONAL]** 공격속도 등 milestone 후보는 실전 DPS와 손맛을 함께 검증한 뒤 확정한다.

### 5.2 Mace

- **[LOCKED]** 철퇴는 generic melee의 평타 공격력 증가만으로 성장시키지 않는다.
- **[LOCKED]** 핵심 성장 방향은 **낙하 / smash damage 공식 자체의 강화**.
- **[LOCKED]** 장비 특성상 milestone 보조축이 반드시 2개일 필요는 없으며, 낙하 공격 성장 하나가 충분히 의미 있다면 단일 특성 구조를 허용한다.
- **[DEFERRED]** Minecraft 26.3 실제 Mace 낙하 피해 계산식을 확인한 뒤 정확한 계수와 formula를 확정한다.

### 5.3 Bow / Crossbow

- **[LOCKED]** Bow와 Crossbow는 동일한 ranged profile로만 취급하지 않고 사용감 차이를 유지한다.
- **[PROVISIONAL]** Bow milestone 후보: draw speed.
- **[PROVISIONAL]** Crossbow milestone 후보: charge / reload speed.
- **[DEFERRED]** Fabric 26.3에서 실제 charge 계산 경로와 안전한 mixin 지점을 확인한 뒤 구현 여부와 수치를 확정한다.
- **[LOCKED]** Crossbow firework의 별도 폭발 semantics는 불필요하게 변경하지 않는다.

### 5.4 Tools

도구는 일반 무기보다 성장축을 넓게 허용한다.

- **[LOCKED]** 최대 강화 + vanilla Efficiency V에서도 모든 일반 광물/블록이 무차별 instant-break가 되어서는 안 된다.
- **[PROVISIONAL TARGET]** 최종 체감은 블록/재질에 따라 대략 Efficiency VI~VIII 부근의 추가 성능 범위를 목표로 한다.
- **[LOCKED]** 단순히 mining speed 하나만 크게 올리는 구조를 사용하지 않는다.
- **[LOCKED]** 도구는 필요하면 2~3개의 성장축을 사용할 수 있다.

후보 축:
1. 제한적인 채굴속도 증가
2. 확정적 내구성 성장
3. 도구 종류별 utility milestone

- **[LOCKED]** 현재처럼 해당 도구가 원래 효과적인 블록에만 강화 채굴속도를 적용하는 안전장치는 유지하는 방향.
- **[DEFERRED]** pickaxe / shovel / axe / hoe 각각의 utility 효과와 정확한 속도 곡선은 실제 break-time 계산 후 확정한다.

### 5.5 Armor

방어구는 가장 민감한 밸런스 영역으로 취급한다.

- **[LOCKED]** 기존 old +30 기준 풀세트 추가 피해 감소 12%는 새 목표로 사용하지 않는다.
- **[PROVISIONAL TARGET]** 최대 강화 풀세트의 Enhancement 기반 추가 피해 감소는 실전 기준 **약 8% 이하**를 목표로 한다.
- 균등 배분이라면 부위당 약 2%가 기준점이지만 이는 최종 formula가 아니다.
- **[LOCKED]** vanilla armor, toughness, Protection enchantment, damage type, BYPASSES_ARMOR와의 상호작용을 포함해 계산한다.
- **[DEFERRED]** enhancement reduction을 vanilla armor 계산 전/후 어디에 적용할지는 실제 수학 모델 및 테스트 후 확정한다.
- **[PROVISIONAL]** 나머지 성장 예산은 내구성, 넉백 저항 등 비-DPS/비-DR 축에 배분할 수 있다.

## 6. 티어별 성장 시스템

### T1 Vanilla

- **[LOCKED]** ordinary Enhancement +20.

### T2 Elemental

- **[LOCKED]** ordinary Enhancement +20.
- **[LOCKED]** 현재 구현된 Flame Sword와 Wind Sword는 Elemental category로 연결해야 한다.
- **[LOCKED]** T2의 우위는 +40 같은 추가 강화 횟수보다 기본 성능과 고유 능력에서 만든다.

### T3 Special

- **[LOCKED]** T3는 향후 **별도의 성장/강화 시스템**을 사용한다.
- **[LOCKED]** 현재 ordinary Enhancement 대상이 아니다.
- **[DEFERRED]** T3 전용 성장 구조는 T3 개발 단계에서 설계한다.

### T4 Endgame

- **[LOCKED]** T4는 ordinary Enhancement 대상이 아니다.
- **[LOCKED]** T4에는 별도의 **등급창 / Grade-Mastery progression**을 도입한다.
- **[LOCKED]** T4 장비로 몬스터를 처치하면 progression point를 얻고, 이를 통해 장비 등급을 올리는 구조를 사용한다.
- **[DEFERRED]** point 획득량, grade 수, grade별 보상, UI 세부는 T4 개발 단계에서 확정한다.

## 7. 인접 티어의 파워 겹침

- **[LOCKED]** 완성된 T1 장비가 갓 획득한 T2 +0보다 무조건 즉시 폐기되는 구조를 피한다.
- **[LOCKED]** T1 +20과 T2 +0 사이에는 일부 전투력 overlap을 허용한다.
- **[LOCKED]** T2를 일정 수준 강화하면 T1 완성 장비를 자연스럽게 추월하도록 설계한다.

정확한 전투력 지수는 실제 base stat과 ability가 확정된 뒤 역산한다.

## 8. 비용 및 강화 인터페이스

### 8.1 비용

- **[LOCKED]** 기존의 “매 단계 XP를 지불” 구조는 더 이상 고정 규칙이 아니다.
- **[PROVISIONAL]** 일반 단계는 재료 중심, milestone 단계는 더 높은 재료 비용 및/또는 XP 비용을 사용하는 리듬을 우선 검토한다.
- **[DEFERRED]** 정확한 Upgrade Stone 수량과 XP 비용표는 재료 공급량과 플레이테스트 후 확정한다.

### 8.2 UI / Workstation

- **[LOCKED]** Anvil은 현재 playable adapter일 뿐 Enhancement의 영구 인터페이스가 아니다.
- **[LOCKED]** core enhancement logic은 AnvilMenu, Smithing Table, custom GUI, NPC, 전용 workstation을 알지 않아야 한다.
- **[LOCKED]** `EnhancementOperation` 계층의 UI-independent 구조를 유지하고 강화한다.
- **[DEFERRED]** 최종 interaction surface는 추후 UX/콘텐츠 단계에서 변경 가능.

## 9. 데이터 모델 원칙

ordinary Enhancement의 기본 저장 상태는 가능한 한 하나만 사용한다.

`enhancement_level = N`

여기서 milestone을 파생한다.

예:
- +4 → milestone count 0
- +5 → 1
- +10 → 2
- +15 → 3
- +20 → 4

별도 Promotion/Star 상태를 저장하지 않는다.

## 10. 몬스터 스케일링

- **[DEFERRED]** 몬스터 스탯 및 progression scaling은 이번 Enhancement 설계에서 확정하지 않는다.
- player-relative 실시간 scaling, 지역 기반 scaling, 구조물/던전 기반 scaling, 진행도에 따른 변종 몬스터 구성 변화 모두 후보로 남긴다.
- 구조물 모드 의존성이 생길 가능성이 있으며, vanilla-only 환경에서는 위험도 구획이 생각보다 어렵다는 점을 고려한다.
- 따라서 이 문제는 **향후 Mob Development / Balance 단계**에서 별도로 다룬다.

이번 Enhancement 개편은 특정 monster-scaling 방식에 의존하지 않게 설계한다.

## 11. 현재 코드에 필요한 핵심 변화

현재 Stage 10 코드 기준으로 다음 legacy assumptions가 남아 있다.

- `EnhancementCategory.VANILLA = +30`
- `EnhancementCategory.ELEMENTAL = +40`
- `EnhancementRules`의 +40 XP band
- Paper의 flat linear profile values
- `EnhancementCatalog`의 category hardcode = VANILLA
- Flame Sword / Wind Sword가 Elemental Enhancement catalog에 연결되지 않음
- Anvil adapter는 존재하지만 최종 UI로 오해될 수 있는 문서 표현

새 구현에서는 다음을 지향한다.

1. T1/T2 +20 cap.
2. +5 단위 milestone 정책.
3. continuous growth와 milestone effect 분리.
4. category 명시 등록.
5. Flame Sword / Wind Sword → ELEMENTAL mapping.
6. 비용 계산을 milestone-aware policy로 분리.
7. Anvil을 thin adapter로 유지.
8. balance 수치를 mixin에 흩뿌리지 않고 중앙 policy에서 관리.

## 12. 구현 전 검증이 필요한 항목

다음은 코드를 먼저 읽고 확인한 뒤 확정한다.

1. Minecraft 26.3 Mace smash/fall damage integration point.
2. Bow draw speed 조정 가능 지점.
3. Crossbow charge/reload speed 조정 가능 지점.
4. Efficiency V + Enhancement 적용 시 실제 block break-time curve.
5. Armor/Toughness/Protection와 Enhancement DR 계산 순서.
6. custom Elemental item 등록과 EnhancementCatalog 사이의 안전한 registry initialization 방식.

## 13. 이번 문서에서 의도적으로 확정하지 않는 것

- 검 최종 단계당 공격력 수치
- 검 milestone 최종 효과
- 철퇴 smash 최종 공식
- 활/쇠뇌 charge speed 수치
- 도구 utility 종류
- 도구 최종 mining speed curve
- armor 정확한 per-level 감소율 및 계산 순서
- Upgrade Stone / XP 최종 비용표
- 최종 Enhancement UI/workstation
- monster scaling 방식
- T3 전용 성장 시스템 세부
- T4 Grade/Mastery 수치 및 UI 세부

이 항목들은 구조를 먼저 구현하고 실제 Minecraft 26.3 mechanics 및 플레이테스트 데이터를 확인한 뒤 닫는다.

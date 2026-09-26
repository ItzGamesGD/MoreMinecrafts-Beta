# MoreMinecrafts Resource / Mining / Materials 확정안

> **상태:** 콘텐츠 설계 단계의 임시 확정(Provisional Freeze). 구현 및 플레이테스트에서 수치 미세조정은 허용하되, 아래 역할 구조는 기준안으로 사용한다.

## 1. 최종 추가 광석 roster

- Titanium
- Tungsten
- Mythril
- Adamantium
- Etherium

5종 모두 실제 지하 월드젠 광석으로 존재한다.

## 2. 차원 및 생성 높이

| 광석 | 차원 | 현재 생성 범위 | 중심 피크 |
|---|---|---:|---:|
| Titanium | Overworld | Y -16 - 80 | Y 32 |
| Tungsten | Overworld | Y -48 - 32 | Y -16 |
| Mythril | Overworld | Y -64 - 0 | Y -48 |
| Etherium | Overworld | Y -32 - 64 | Y 16 |
| Adamantium | Nether | Ancient Debris 유사 초안 | Ancient Debris 유사 |

Titanium과 Tungsten은 높이와 분포 위치는 MoreMinecrafts 고유 값을 사용하고, 광맥/placement 관련 출발 규칙은 바닐라 Iron 계열을 기준으로 동기화한다.

## 2.1 돌 / 심층암 자동 전환

Overworld의 Titanium, Tungsten, Mythril, Etherium은 각각 일반 광석 블록과 심층암 광석 블록을 가진다.

- 일반 암석 대상: `minecraft:stone_ore_replaceables` → 일반 `*_ore`
- 심층암 대상: `minecraft:deepslate_ore_replaceables` → `deepslate_*_ore`
- 전환은 고정 Y 임계값으로 강제하지 않는다. 실제 월드젠 시 대체 대상(host block)이 돌인지 심층암인지에 따라 같은 configured feature 안에서 자동으로 선택한다.
- 따라서 하나의 광맥이 돌-심층암 경계를 가로지르면 경계에 맞춰 일반형과 심층암형이 자연스럽게 섞일 수 있다.
- Titanium/Tungsten/Mythril/Etherium의 기존 높이 범위와 희귀도 수치는 그대로 유지한다. 이번 변경은 host-rock 표현과 블록 변형을 보완하는 것이며 분포 밸런스를 재설계하지 않는다.
- Adamantium은 Nether 전용 Ancient-Debris 계열이므로 심층암 블록을 등록하지 않는다. 승인된 `adamantium_debris_deepslate.png`는 현재 reserve asset으로만 유지한다.

## 3. 희귀도 및 공급 목표

- **Titanium / Tungsten:** 비교적 흔한 현실계 금속. 체감 공급 목표는 Copper보다 희귀하고 Iron보다 흔한 구간.
- **Mythril / Etherium:** 둘 다 Diamond보다 희귀.
- **Mythril / Etherium 생성 시도:** 바닐라 Diamond 대응 시도값 × 0.67.
- **Adamantium:** 현재 초안은 Ancient Debris와 비슷한 희귀도 감각.

이 수치는 최종 경제 밸런스가 아니라 콘텐츠 설계용 기준값이다. 실제 체감 공급량은 구현 후 플레이테스트에서 조정할 수 있다.

## 4. 광맥 크기 및 노출

- **Mythril / Etherium:** 한 광맥에서 최대 6블록 연결.
- **Titanium / Tungsten:** 높이 범위를 제외한 광맥 관련 출발 규칙은 Iron 기준 동기화.
- **Adamantium:** Ancient Debris 유사 구조를 임시 기준으로 사용.
- 노출 빈도의 독자 세부 수치는 현 단계에서 별도 최적화하지 않고 대응 바닐라 월드젠을 기준점으로 둔다.

## 5. Silk Touch / Fortune / 드롭 규칙

### 공통 Silk Touch
신규 광물 전체에 동일한 원칙을 적용하여 광석 블록 자체를 획득한다.

### Titanium / Tungsten
- 현대 바닐라 Iron / Gold와 같은 획득 구조.
- 일반 채굴 시 raw material 드롭.
- Fortune 적용.
- 기본 드롭 개수와 Fortune 동작도 Iron / Gold 대응 규칙 사용.
- 완제품 금속 획득에는 제련 필요.

### Mythril / Etherium
- 바닐라 Diamond와 같은 획득 구조.
- 일반 채굴 시 완성된 재료 직접 드롭.
- Fortune 적용.
- Diamond 계열 경험치 드롭 규칙 사용.
- 별도 제련 단계 없음.

### Adamantium
- 바닐라 Ancient Debris와 대응되는 획득 구조.
- Fortune 미적용.
- 채굴 후 제련 필요.
- 제련 결과물의 정확한 아이템 명칭과 중간재 형태는 실제 recipe 설계 시 확정.

## 6. 가공 단계

- Titanium Ore → Raw Titanium → Titanium Ingot
- Tungsten Ore → Raw Tungsten → Tungsten Ingot
- Mythril Ore → Mythril material
- Etherium Ore → Etherium material
- Adamantium ore/debris-type block → smelted Adamantium material  
  정확한 중간재 이름은 추후 확정.

## 7. Survival 획득

- Mythril: Overworld 자연 월드젠.
- Etherium: Overworld 자연 월드젠.
- Adamantium: Nether 자연 월드젠.

따라서 현재 확정된 fantasy mineral도 구조물/보스 전용 재료가 아니라 기본적으로 채굴을 통해 survival 획득 가능하다.

## 8. 체크리스트 해석

- **재료별 recipe 수량:** 아직 실제 recipe가 존재하지 않으므로 현 단계에서는 확정하지 않는다. Equipment / Utility 등 실제 recipe 설계 시 공급량과 함께 결정한다.
- **실제 체감 공급량 밸런스:** 세부 수치가 아니라 상대 목표를 확정한 것으로 처리한다. Titanium/Tungsten은 Copper와 Iron 사이, Mythril/Etherium은 Diamond보다 희귀, Adamantium은 Ancient Debris 유사 수준.

## 9. 구현 시 재검증 대상

- 실제 chunk당 생성량과 탐색 시간
- 광맥 노출 빈도
- Fortune 적용 후 시간당 획득량
- 향후 recipe 소비량과 공급량의 충돌 여부
- 플레이테스트에서 특정 높이에 광석이 과밀하게 겹치는지 여부

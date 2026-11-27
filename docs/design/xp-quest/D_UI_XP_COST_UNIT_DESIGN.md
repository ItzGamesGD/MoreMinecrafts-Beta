# D. XP Quest UI 재사용 · 180K 경제 · 원시 XP 비용 단위 설계 (제안, 2026-10-17)

> **최신 결정 반영 (2026-10-17):** §1 클라이언트 합성 UI, 완료 토스트 + 클릭 수령, 강화 원시 XP 비용표(40/70/110/160, +20 총 1,900), T1·T2 동일 비용, 미수령 보관 허용, 지급·차감 모두 정확 계산(+`totalExperience` 동시 갱신), 강화 거래 롤백 안전성, 시나리오 D가 **승인**되었다. 보상 범위: 활성 180개 = 정확히 180,000XP + FUTURE 29,100XP = 209,100XP. 승인된 결정의 정본은 `docs/UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md`(XP Quest amendment)와 `docs/STAGE_2_GAMEPLAY_RECONCILIATION.md`(Enhancement)이다. 이 설계 문서는 권한 문서가 아니다.


상태: **설계 제안 — 미구현, authority 미반영.** 프로덕션 코드와 기존 PR은 수정하지 않았다.

검증 출처:
- Mojang 공식 26.3 client/server jar(piston-meta 매니페스트). 26.3 jar는 비난독화 상태라 클래스·메서드 이름을 `javap`로 직접 확인했다.
- 저장소 `claude/stage23a-economy-soil-20261017` @ `9ec91dc`.

표기: **[확인]** 소스·바이트코드 확인 · **[제안]** 새 설계 · **[검증 필요]** 라이브/GameTest 확인 필요

---

## 1. UI — 바닐라 발전과제 화면 직접 재사용

### 1.1 26.3 API 확인 결과 [확인]

| 요소 | 26.3 시그니처 | 재사용 가능성 |
|---|---|---|
| 화면 | `public AdvancementsScreen(ClientAdvancements, Screen lastScreen)` | 데이터 원천(`ClientAdvancements`)을 생성자로 주입받는다. **별도 인스턴스를 넘기면 화면 전체(창·탭·스크롤·드래그·툴팁·노드 연결선)를 그대로 쓸 수 있다.** |
| 데이터 | `public class ClientAdvancements(Minecraft, WorldSessionTelemetryManager)` — `update(ClientboundUpdateAdvancementsPacket)`, `tree()`, `progress()`, `setListener`, `setSelectedTab(holder, boolean)` | final이 아니므로 **서브클래스 가능** |
| 패킷 | `public ClientboundUpdateAdvancementsPacket(boolean reset, List<PositionedAdvancement>, Set<Identifier> removed, Map<Identifier, AdvancementProgress>, boolean show)` | 클라이언트에서 직접 생성해 `update`에 넣을 수 있다. 노드 좌표는 `PositionedAdvancement(x, y)`로 지정한다. |
| 노드 데이터 | `Advancement(Optional parent, Optional<DisplayInfo>, AdvancementRewards, Map criteria, AdvancementRequirements, boolean)`, `AdvancementHolder(Identifier, Advancement)`, `DisplayInfo(ItemStackTemplate icon, title, description, Optional background, AdvancementType frame, showToast, announceChat, hidden)` | 모두 public 레코드/생성자 → 퀘스트 카탈로그에서 합성 가능 |
| 진행도 | `AdvancementProgress()`, `update(requirements)`, `grantProgress(String)` | 공개 API |
| 노드 표시 | `AdvancementWidgetType`: **OBTAINED / UNOBTAINED 2종뿐**. 프레임 종류는 `AdvancementType`(task/goal/challenge) | 3상태 구분에는 오버레이 확장이 필요(§1.3) |
| 탭 수 | `AdvancementTabType` ABOVE max 8 (+BELOW·LEFT·RIGHT). 한계를 넘으면 `AdvancementTab.create`가 null 반환 | 12탭 = 위 8 + 아래 4로 수용 가능 |
| 서버 통신 | 화면이 `ServerboundSeenAdvancementsPacket.openedTab/closedScreen`을 전송한다. 서버 `handleSeenAdvancements`는 `getAdvancements().get(id)`의 **null을 검사한다** | 미등록 퀘스트 탭 ID를 보내도 서버 오류는 없다. 그래도 억제를 권장(§1.2). |

### 1.2 구현 방식 [제안]

선택지 비교:
- **A안(권장): 클라이언트 합성.** 서버는 지금 그대로 `ResearchProgressState`의 `quest.*` 키로 진행·완료·수령을 저장하고 동기화한다. 클라이언트는 그 상태로 가짜 발전과제 트리를 만들어 바닐라 화면에 띄운다.
- **B안(비권장): 퀘스트를 실제 바닐라 발전과제(데이터팩)로 등록.**
  - 장점: 진행 저장을 바닐라에 맡긴다.
  - 단점: 퀘스트가 '발전 과제' 카드(바닐라 화면)와 채팅 알림에 섞여 나온다. 수령 상태는 어차피 별도 저장이 필요하다. 200개 조건 대부분이 바닐라 트리거로 표현되지 않는다(커스텀 `CriterionTrigger` 다수 필요).

A안 구성:
1. `QuestClientAdvancements extends ClientAdvancements`
   - `update`를 오버라이드해 tree·progress만 채운다.
   - 기본 `update`는 완료 시 `AdvancementToast`와 `telemetryManager.onAdvancementDone`을 호출한다 [확인]. 가짜 발전과제가 바닐라 텔레메트리로 보고되거나 이중 토스트가 뜨는 것을 막기 위해 둘 다 호출하지 않는다.
   - `setSelectedTab(holder, false)`로 서버 전송을 막는다.
2. 동기화가 바뀔 때마다(`ProgressionSyncRuntime` 스냅샷/델타 수신) `reset=true` 패킷을 다시 만든다. 200노드 재생성 비용은 무시할 수준이다 [검증 필요: 열린 화면의 스크롤 유지 — `AdvancementTab.copyPosition` 존재 확인].
3. 탭 = 12개 루트 노드(`exploration`…`growth`), 아이콘과 배경은 기존 바닐라 리소스를 사용한다. 노드 = 퀘스트, 부모 = 시각적 선행(잠금 아님). 좌표 = `node_layout.json` 그리드.
4. 선택기의 `xp_quest` 카드에서 `new AdvancementsScreen(questAdvancements, selector)`를 연다. 카드 활성 이미지는 별도 결정이다.
5. 화면 제목은 static `TITLE`("발전 과제")로 고정되어 있다. 퀘스트 인스턴스일 때만 mixin(`HeaderAndFooterLayout.addTitleHeader` 인자 교체)으로 "XP 퀘스트"로 바꾼다. 퀘스트 화면에서는 `ServerboundSeenAdvancementsPacket` 전송을 mixin으로 억제한다.

### 1.3 세 상태 표현 [제안]

| 상태 | 서버 키 | 합성 진행도 | 프레임 | 추가 표시 |
|---|---|---|---|---|
| 미달성 | `quest.complete=0` | 미완료 | UNOBTAINED | 설명 마지막 줄에 `진행 n / m · 보상 XXX XP` |
| 달성·미수령 | `complete=1, claimed=0` | 완료 | OBTAINED(금색) | 위젯 렌더 TAIL mixin으로 맥동 테두리 + "!" 배지. 툴팁 "클릭하여 XXX XP 수령" |
| 수령 완료 | `claimed=1` | 완료 | OBTAINED | 체크 배지, 툴팁 "수령 완료" |

- 진행 표시: 바닐라 `getProgressText`는 criteria 개수 기반이다. 목표가 250,000처럼 크면 criteria로 표현할 수 없으므로 진행 텍스트는 설명에 넣는다. 위젯 툴팁 너비 계산(`findOptimalLines`)이 그대로 적용된다.
- 클릭(유일한 핵심 확장):
  - `AdvancementsScreen.mouseClicked` HEAD에 mixin을 건다. 퀘스트 인스턴스이고, 선택 탭의 hovered 위젯(`AdvancementTab.hovered`는 private → accessor)이 '달성·미수령'이면 `QuestClaimRequestPayload(id)`를 전송하고 이벤트를 소비한다.
  - 드래그와 구분하기 위해 마우스 다운·업 위치가 같을 때만 처리한다.
- 클라이언트 중복 클릭 방지:
  - 전송한 id를 `pending` 집합에 넣고, 서버 결과(`ProgressionActionResultPayload "quest:<id>"`) 또는 `claimed` 델타가 올 때까지 재전송을 막는다. 대기 중에는 배지를 회색 모래시계로 표시한다.
  - 클라이언트는 XP를 낙관적으로 표시하지 않는다.

### 1.4 중복 수령 · 저장 · 재접속 [확인 + 제안]

- **[확인] 서버 처리는 이미 원자적이다.** 수신기는 `server.execute`로 메인 스레드에서 실행된다. `QuestProgressService.claim` 순서: `claimed` 검사 → `claimed` 기록 → `giveExperiencePoints`. 같은 틱에 두 패킷이 와도 두 번째는 `ALREADY_CLAIMED`.
- **[확인] 영속:** `KnowledgeRuntime.STATE`가 `.persistent(CODEC).copyOnDeath()`라 사망·재접속·재시작에서 유지된다. 접속 시 전체 스냅샷이 전송된다.
- **[제안] 보강:**
  - (a) 사망·접속 해제 중 수령 거부 → 결과 `UNAVAILABLE`(enum 끝에 추가).
  - (b) 수령 직후 플레이어 저장 데이터가 디스크에 기록되기 전에 서버가 비정상 종료되는 경우를 검토했다. `claimed` 기록과 XP 지급이 같은 플레이어 데이터(어태치먼트 + 경험치 필드)에 들어가므로, 둘 다 저장되거나 둘 다 롤백된다. 결과적으로 중복 수령도 XP 손실도 없다. GameTest로 확인한다.
  - (c) 크리에이티브·관전 모드에서는 진행만 무시하고 수령은 허용한다(기존 권고 유지).

---

## 2. 원시 XP 차감·지급 방식 [확인 + 제안]

- **[확인]** `Player.giveExperiencePoints(int)`:
  - `experienceProgress += xp / getXpNeededForNextLevel()` 후 while 루프로 레벨을 올리거나 내린다.
  - 내부에서 `increaseScore(xp)`를 호출하므로, 음수 지급으로 차감하면 점수도 줄어든다.
  - 진행도는 **float**이고 한 번에 여러 레벨을 넘으면 매번 다시 나눈다. 따라서 큰 차감(수천 XP)에서 원시 XP가 몇 포인트 어긋날 수 있다.
- **[확인]** `ServerPlayer.setExperienceLevels(int)`와 `setExperiencePoints(int)`가 public이다.
- **[확인]** 현재 MoreMinecrafts 원시 XP 결제는 `RawXp.pay → giveExperiencePoints(-cost)`다. `ResearchPurchaseService`는 실패 시 레벨·진행·total을 직접 되돌린다.
- **[제안] `RawXp.payExact(ServerPlayer, cost)`로 통일:**
  - 순서: `before = RawXp.available(p)` → 부족하면 거부 → `after = before - cost` → 역함수로 레벨 L과 잔여 포인트 r을 계산 → `setExperienceLevels(L)` + `setExperiencePoints(r)` → `totalExperience` 보정.
  - 결과가 정수로 정확하고, 점수(`increaseScore`) 감소도 생기지 않는다.
  - ~~지급(퀘스트 보상)은 `giveExperiencePoints(+n)`를 유지한다.~~ **[대체됨 — 승인: 지급·차감 모두 정확 계산, `totalExperience` 동시 갱신]**
  - 기존 `ResearchXpAcquisitionMixin`은 구슬 습득에만 걸리므로 두 방식 모두 연구 배율·수선과 무관하다 [확인].

---

## 3. MoreMinecrafts XP 소비처 조사 [확인]

`experienceLevel`, `giveExperience*`, `RawXp`, `XpLevels` 전수 검색 결과:

| 소비처 | 코드 | 현재 단위 | 비용 |
|---|---|---|---|
| 연구 구매 | `ResearchPurchaseService` | **원시 XP** | V3·V4 원장 (총 147,670) |
| T1 모루 결합(인챈트 진행) | `AnvilMenuMixin` mode 2 + `T1Operations.combineCost` | **원시 XP** | 목표 레벨당 100 |
| T1 인챈트 책 옵션 | `T1Operations` / `EnchantingProgressionScreen` | **원시 XP** | 200 |
| **강화 (T1 Vanilla / T2 Elemental 공통)** | `EnhancementOperation` → `XpLevels.pay` / `T1AnvilScreen` | **레벨** | 단계당 1레벨 (+1 강화석, 이정표 +5/+10/+15/+20은 2개) |
| 바닐라 마법부여대·모루 | 바닐라 | 레벨 | **변경 대상 아님** |
| 그 외 (포획·낚시·장비·주민 할인 등) | — | XP 비용 없음 | — |

→ **레벨 단위인 MoreMinecrafts 소비처는 강화 하나뿐이다.** 통일 대상은 강화다.

### 3.1 현재 강화 비용의 실제 원시 XP 환산 [확인, 바닐라 레벨 곡선]

1레벨의 원시 XP 가치(L−1 → L 구간): L1 7 · L5 15 · L10 25 · L15 35 · L20 57 · L25 82 · L30 107 · L35 148 · L40 193 · L50 283

| 지불 시 레벨 구간 | +0→+20 (20레벨) 실비용 |
|---|---:|
| 20 → 0 | 550 |
| 30 → 10 | 1,235 |
| 40 → 20 | 2,370 |
| 50 → 30 | 3,950 |

**같은 +20 강화가 레벨에 따라 최대 약 7배 차이 난다.** 0레벨 근처에서 1레벨씩 결제하면 단계당 약 7 XP까지 내려간다. 180K 퀘스트 XP가 들어오면 플레이어는 고레벨에 머물기 쉬워서, 이 편차는 오히려 커진다.

### 3.2 새 고정 원시 XP 비용 [제안]

원칙:
- 구간별 대표 지불 레벨을 가정해 현재 실비용과 비교한 뒤 고정값을 정한다.
- 연구 T1 L1 중앙값 245, 책 옵션 200, 결합 레벨당 100과 같은 척도 안에 둔다.
- 재료(강화석, 이정표 2개) 구조는 유지한다.

| 목표 단계 | 대표 지불 레벨(가정) | 현재 실비용/단계 | **제안 고정/단계** | 구간 합계(5단계) |
|---|---|---:|---:|---:|
| +1 ~ +5 | 10~15 (초반) | 25~35 | **40** | 200 |
| +6 ~ +10 | 20~25 | 57~82 | **70** | 350 |
| +11 ~ +15 | 25~30 | 82~107 | **110** | 550 |
| +16 ~ +20 | 30~40 (후반) | 107~193 | **160** | 800 |
| **+0 → +20 합계** | | 현재 550~3,950 | | **1,900** |

- 1,900은 대략 '35레벨 근처에서 지불할 때'의 현재 실비용에 해당한다. 현재 비용을 그대로 옮긴 값이 아니라 구간 대표값을 비교해 정한 제안이다.
- 이정표 단계에 XP를 추가로 붙이는 안은 채택하지 않았다. 현재 이정표의 추가 비용은 재료(강화석 2개)뿐이므로 그 구조를 보존한다(D-X2).
- T2 Elemental은 같은 `EnhancementRules.costFor`를 공유하므로 동일 표를 적용한다. T2만 차등할지는 별도 결정이다(D-X3).
- 장비 10개(무기·도구·방어구 4)를 +20까지 올리면 19,000 XP가 든다. 180K 공급 대비 약 11%다.
- UI 표기: `T1AnvilScreen`의 "N 레벨"을 "N XP"로 바꾸고, 가능 여부는 `RawXp.canPay`로 판정한다. 다른 원시 XP 화면과 같은 방식이다.
- 마이그레이션: 비용 함수만 바뀌고 저장 데이터는 없다. 이미 강화된 아이템은 영향받지 않는다.

---

## 4. 180,000XP 공급의 영향 [확인 수치 + 가정]

### 4.1 공급 규모 (시나리오 D, 활성 180개 = 180,000, 반올림 손실 재분배 후)

| 단계 | 퀘스트 대표 보상 | 0레벨에서 받으면 |
|---|---:|---|
| 초반 | 200 | → L11 |
| 초중반 | 350 | → L15 |
| 중반 | 500 | → L19 |
| 중후반 | 750 | → L22 |
| 후반 | 1,100 | → L27 |
| 엔드게임 | 3,150 | → L41 |

MoreMinecrafts 원시 XP 수요: 연구 147,670 + 강화(제안 기준, 장비 10개) ≈19,000 + 책 옵션·결합 가변 → **≈170~185K로 공급과 거의 균형**이다.

### 4.2 바닐라 시스템 영향 (바닐라 비용 체계는 변경하지 않음)

- **마법부여대:** 30레벨 보유가 조건이고 실제 소모는 1~3레벨(30레벨 기준 3레벨 ≈ 306 XP)이다. 후반 퀘스트 1~2개 수령으로 30레벨 도달이 쉬워진다. 결과적으로 바닐라 인챈트의 진입 장벽이 크게 낮아진다. 의도된 효과(몹팜 의존 완화)로 판단하지만, 체감 검증이 필요하다.
- **모루:** 레벨 비용과 "너무 비쌈"(40) 상한은 그대로다. 플레이어가 고레벨을 유지하면 같은 모루 작업의 원시 XP 실비용이 커진다. 이것은 바닐라 고유 특성이며 변경하지 않는다.
- **수선(Mending):** 퀘스트 XP는 구슬이 아닌 직접 지급이라 **수선 수리에 쓰이지 않는다** [확인]. 수선 장비 유지는 여전히 일반 플레이의 구슬 수급에 의존한다. 퀘스트 XP가 수선을 무력화하지 않는다는 장점이 있지만, 몹팜 의존을 완전히 없애지도 못한다. 수령 시 구슬을 생성하는 방식은 연구 배율 비적용 요구와 충돌하므로 채택하지 않는다.
- **사망 손실:** 바닐라 사망 시 경험치 드롭은 `7 × 레벨`, 최대 100까지만이고 나머지는 소실된다. 큰 보상을 받은 뒤 사망하면 손실이 크다. 그런데 **수동 수령이라 미수령 퀘스트가 사실상 안전한 XP 보관소 역할을 한다.** 플레이어가 소비 직전에 수령하는 패턴이 생길 것이다. 수용 가능하다고 판단하며(별도 화폐 도입 없이 생기는 자연스러운 완충), 결정은 D-X4.
- **점수:** 수령은 `increaseScore`로 사망 화면 점수를 올린다. 영향은 미미하다.
- **연구 전용 화폐:** 도입하지 않는다(지시 사항). 모든 MoreMinecrafts 비용을 원시 XP로 통일하면, 이후 별도 화폐로 전환할 때 환산 기준이 명확해진다.

### 4.3 몹팜 없는 진행의 병목 (갱신)

- 몹팜 없는 일반 플레이 수입을 시간당 300~800 XP로 가정하면 [가정], D 공급만으로 MoreMinecrafts 수요의 거의 전부를 충당한다. 일반 플레이 수입은 바닐라 인챈트·모루·수선에 쓰이게 된다.
- 남는 병목: 시나리오 곡선상 엔드게임 비중이 37%라 중반 T2 연구(L1 중앙값 900)에서 정체 구간이 생길 수 있다. 곡선 조정은 D-E4로 유지한다.

---

## 5. 구현 계획 영향 (기존 Q1~Q5 갱신)

| PR | 변경 |
|---|---|
| Q3 보상 | `UNAVAILABLE` 결과, 수령 원자성 GameTest |
| **Q3b 비용 단위 (신규, 독립)** | `RawXp.payExact` 도입. `RawXp.pay`·`ResearchPurchaseService`를 정확 차감으로 이관. `EnhancementRules.Cost`를 `rawXpCost`로 교체(§3.2 표). `XpLevels`는 사용처가 없어지면 제거. `T1AnvilScreen` 표기 변경 |
| Q4 UI | `QuestClientAdvancements`, 합성 패킷 빌더. mixin 3종: 화면 제목, mouseClicked 수령, 위젯 상태 배지·SeenAdvancements 억제. `AdvancementTab.hovered` accessor. 선택기 카드 활성 |
| Q5 테스트 | 아래 §6 |

## 6. 검증 계획

- **GameTest:**
  - 같은 틱 이중 수령 시 1회만 지급.
  - 사망·재접속·월드 재시작 후 3상태 유지.
  - `payExact`가 0·경계 레벨(15/16, 30/31)·대량(10,000) 차감에서 정확히 1의 오차도 없음.
  - 강화 비용이 레벨과 무관하게 동일.
  - 퀘스트 지급분에 연구 배율·수선 미적용.
- **클라이언트(LIVE VALIDATION PENDING):**
  - 12탭 배치, 맥동 배지 가독성, 드래그와 클릭 구분.
  - 툴팁 진행 텍스트의 한국어 줄바꿈.
  - 토스트·텔레메트리 미발생 확인.
  - 수령 중 화면 재생성 시 스크롤 유지.

## 7. 결정 필요

| ID | 결정 | 권장 |
|---|---|---|
| D-UI1 | A안(클라이언트 합성) | **결정됨: 채택** |
| D-UI2 | 완료 토스트 | **결정됨: 바닐라 스타일 토스트, 완료 델타(0→1)에서만 1회 — 스냅샷에서는 미표시(handoff §4)** |
| D-X1 | 보상 지급도 정확 계산 방식으로 할지 | **결정됨: 지급·차감 모두 정확 계산** |
| D-X2 | 강화 고정 비용 표 | **결정됨: 40/70/110/160, +20 총 1,900(잠정)** |
| D-X3 | T2 강화 비용 차등 | **결정됨: T1·T2 동일** |
| D-X4 | 미수령 퀘스트 XP 보관 | **결정됨: 허용** |

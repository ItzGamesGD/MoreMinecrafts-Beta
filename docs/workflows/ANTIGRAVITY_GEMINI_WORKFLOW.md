# Antigravity ↔ Gemini 독립 기획 워크플로 규약 (Operational Protocol)

## 1. 개요 및 목적

본 문서는 **GitHub를 단일 권위 저장소(Single Source of Truth)**로 유지하며, ChatGPT, Google Drive 동기화, 외부 중계 AI 없이 **Antigravity(조사·검증·구현 엔진)**와 **Gemini(브레인스토밍·설계 파트너)**가 신규 콘텐츠 기획부터 구현 준비까지 정밀하게 협업하기 위한 재사용 가능한 운영 규약이다.

### 핵심 원칙
1. **WHAT = Human, HOW = Agent:** 기획 내용과 설계 확정은 인간 사용자가 결정하며, 에이전트는 기획 근거 추출, 충돌 분석, 구현 검증을 담당한다.
2. **무손실 전달 & 최소 토큰:** 불필요한 전체 저장소 스캔을 지양하고 의존성 기반 단계적 확장을 통해 핵심 원본 근거와 제약을 전달한다.
3. **엄격한 다차원 상태 분리:** 구현 상태, 자동 검증 상태, 실사용 검증 상태를 완전히 독립된 축으로 관리한다.
4. **저장소 오염 방지:** 기획 중간 과정의 초안은 비추적 작업 자료(Working Artifacts)로 관리하며, 사용자가 최종 승인한 설계만 기존 권위 문서 체계에 맞추어 공식 반영한다.

---

## 2. 전체 협업 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            GitHub Single Source of Truth                    │
│   (docs/CONTENT_AUTHORITY_INDEX.md, FEATURE_STATUS_REGISTRY, Java/JSON Code)│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                1. 사용자 기획 주제 지정 (User -> Antigravity)
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           [Step 1] Antigravity                              │
│  - origin/<branch> 최신 원격 SHA 확인 및 조상 관계 검증                      │
│  - 권위 문서 및 코드 탐색 (구현 / 자동 검증 / 실사용 검증 3개 축 매핑)     │
│  - 영향도, 의존성 매핑, 기존 불변선 및 변경 검토점 식별                     │
│  - 산출물: TASK_CONTEXT (임시 작업 자료)                                    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                2. TASK_CONTEXT 전달 (User -> Gemini)
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           [Step 2] Gemini (기획 창구)                       │
│  - 자유 브레인스토밍 및 대안 제시                                          │
│  - 3중 태깅: [기존확정], [신규제안], [변경제안] (기존설계 변경 제안 허용)   │
│  - 미해결 질문 분리: [BLOCKER] vs [NON-BLOCKER]                             │
│  - 각 승인 항목별 결정 ID(Decision ID) 및 사용자 승인 근거 기록             │
│  - 산출물: APPROVED_DESIGN (임시 작업 자료)                                 │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                3. APPROVED_DESIGN 전달 (User -> Antigravity)
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           [Step 3] Antigravity                              │
│  - 원격 대상 브랜치 fetch 후 조상 관계 및 의미적 충돌(Semantic Conflict) 감사│
│  - 권위 문서 충돌 및 상위 호환성 감사                                       │
│  - [BLOCKER] 잔존 여부 확인 (잔존 시 STOP & ASK)                            │
│  - 통과 시 사용자 승인 하에 권위 문서에만 최종 동기화                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 다차원 상태 분류 체계 (3-Axis Status Model)

구현 여부와 검증 여부를 혼동하지 않기 위해 모든 기능과 컴포넌트는 다음 3개 축을 동시에 평가하여 기록한다.

### 1) 구현 상태 (Implementation State)
- **`UNIMPLEMENTED` (미구현):** 코드 및 데이터팩이 전혀 존재하지 않음.
- **`PARTIAL` (부분 구현):** 골격이나 일부 메서드/데이터만 존재하고 핵심 로직이 미완성됨.
- **`IMPLEMENTED` (구현 완료):** 명세된 기능의 프로덕션 코드 작성이 완료됨.
- **`SUPERSEDED / RETIRED` (대체/폐기):** 과거 구현체가 있으나 설계 변경으로 인해 비활성화되거나 교체 대상임.

### 2) 자동 검증 상태 (Automated Verification State)
- **`NOT_RUN` (미실행):** 자동화 테스트가 수행되지 않음.
- **`PASS` (통과):** JUnit 단위 테스트, 컴파일, Fabric GameTest(CI)가 오류 없이 통과됨.
- **`FAIL` (실패):** 컴파일 에러, 단위 테스트 실패, 게임테스트 타임아웃/오류 발생.

### 3) 실사용 검증 상태 (Live Acceptance State)
- **`NOT_RUN` (미실행):** 인게임 플레이 환경 검증을 시작하지 않음.
- **`PENDING` (대기 중):** 조작감, 타격감, VFX 연출, 네트워크 체감 밸런스 등 인게임 플레이어 확인이 필요한 상태.
- **`ACCEPTED` (승인 완료):** 실제 마인크래프트 클라이언트 환경에서 플레이어가 직접 플레이하고 합격 판정을 내림.

> **표기 예시:**  
> `Fire Sword 우클릭 스킬`: 구현 상태 `IMPLEMENTED` / 자동 검증 `PASS` / 실사용 검증 `PENDING`

---

## 4. Antigravity 조사 프로토콜 (Investigation Protocol)

사용자가 기획 주제를 지정하면 Antigravity는 아래 5단계로 조사를 진행한다.

1. **원격 최신성 및 조상 관계 확인:**
   - `git fetch origin <target_branch>` 실행.
   - 대상 브랜치의 최신 원격 SHA를 확인하고 기록.
2. **권위 문서 라우팅 (Authority Resolution):**
   - `docs/CONTENT_AUTHORITY_INDEX.md`를 시작점으로 삼아 해당 도메인의 최신 권위 문서 확인.
   - 오래된 문서의 폐기(Superseded)된 항목과 현행 권위를 구분.
3. **코드 및 리소스 매핑 (Implementation Reality Check):**
   - Registry, Runtime, Event Handler, Mixin, Data Component, JSON, Lang 파일 조사.
   - 3축 상태 모델(구현, 자동 검증, 실사용 검증)로 분류.
4. **불변 제약 및 변경 검토점 식별:**
   - 시스템 전역 불변선(예: AGENTS.md 1원칙, 바닐라 호환성) 추출.
   - 기존 권위 문서 중 합당한 근거가 있다면 Gemini가 변경을 제안할 수 있는 항목 식별.
5. **삼각 검증 및 TASK_CONTEXT 생성:**
   - `[FACT]`(확인된 사실), `[INFERENCE]`(정황 추론), `[CONFLICT/UNCERTAIN]`(불확실 사항)으로 구분.
   - Gemini가 별도 조사 없이 기획할 수 있도록 원본 코드 및 데이터 발췌본을 포함하여 생성.

---

## 5. Gemini 기획 프로토콜 (Gemini Planning Protocol)

Gemini는 사용자와 대화하며 아래 규칙에 따라 브레인스토밍을 진행한다.

1. **3중 태깅 규칙 (Tagging Rule):**
   - `[기존확정]`: `TASK_CONTEXT`에 명시된 기존 권위 및 구현값을 그대로 인용할 때 사용.
   - `[신규제안]`: 기존에 없던 새로운 메커니즘, 수치, 아이디어를 제안할 때 사용.
   - `[변경제안]`: 기존 권위나 구현으로 확정되어 있던 사항의 수정을 제안할 때 사용. (반드시 변경 사유, 이점, 잠재 위험 명시)
2. **기존 권위 유지 원칙:**
   - `[변경제안]`은 자유롭게 제안할 수 있으나, 사용자의 명시적 승인이 있기 전까지는 기존 권위가 유효한 것으로 전제한다.
3. **미해결 질문(Open Questions) 2분법:**
   - `[BLOCKER]`: 해당 결정 없이는 구현을 착수할 수 없는 핵심 아키텍처/데이터 구조 누락.
   - `[NON-BLOCKER / LIVE-TUNING]`: 구현 착수는 가능하며, 수치 미세조정이나 인게임 플레이를 통해 후속 튜닝이 가능한 항목.
4. **결정 ID 및 승인 근거 기록 (Traceability):**
   - 사용자가 합의하거나 승인한 각 항목에 대해 고유한 결정 ID(`DEC-01`, `DEC-02` 등)를 부여하고, 사용자가 어떠한 맥락/발언으로 승인했는지 기록한다.
5. **APPROVED_DESIGN 작성:**
   - 기획 종료 시 오직 사용자 승인을 받은 내용만 정리하여 템플릿에 맞추어 출력한다.

---

## 6. 원격 최신성 및 의미적 충돌 검증 프로토콜 (Remote Semantic Conflict Audit)

Antigravity는 `APPROVED_DESIGN`을 반환받았을 때 코드 작성에 들어가기 전 다음 절차로 검증한다.

```
[APPROVED_DESIGN 수신]
         │
         ▼
[Step 1: Remote Ancestry & Drift Audit]
 git fetch origin <target_branch>
 remote_head = git rev-parse origin/<target_branch>
 baseline_sha = <APPROVED_DESIGN.Baseline_SHA>
 
 1. 조상 관계 검증: git merge-base --is-ancestor baseline_sha remote_head
    (조상 관계가 깨진 경우 즉시 보고)
 2. 변경 로그 확인: git log --oneline baseline_sha..remote_head
 3. 변경 파일 확인: git diff --name-status baseline_sha..remote_head
         │
         ▼
[Step 2: Semantic Conflict Evaluation (의미적 충돌 판단)]
 * 변경 발생 자체만으로 충돌로 단정하지 않는다.
 * 기획 대상 영역과 원격의 변경점이 겹치는지 분석:
   - 기획과 무관한 파일(예: 문서 린트, 다른 도메인 기능) 수정 -> NON-CONFLICT (통과)
   - 기획 대상과 동일한 코드/권위 문서 수정 -> 변경 내용을 검토하여 의미적 충돌 여부 판정
   - 실제 의미적 충돌(Semantic Conflict) 발생 시 -> [STOP & ASK] 충돌 세부사항 보고 후 중단
         │
         ▼
[Step 3: Blocker Audit]
 - 미해결 항목에 [BLOCKER]가 남아있는지 확인.
   - [BLOCKER] 존재 시 -> [STOP & ASK] 필수 설계 누락으로 진행 중단
   - [NON-BLOCKER]만 존재 시 -> LIVE_PENDING으로 분류하고 진행
         │
         ▼
[Step 4: Implementation Feasibility & Acceptance Criteria Audit]
 - 현재 저장소의 Minecraft/Fabric 대상 버전(빌드 설정에서 확인) 및 현 아키텍처 지원 여부 확인. 과거 버전(예: 1.21.x)을 고정 가정하지 않는다.
 - AC(수용 기준) 및 검증 계획이 명확한지 확인.
         │
         ▼
[Step 5: Completion Report & Authoritative Sync Approval]
 - 모든 검증 통과 보고.
 - 사용자에게 "승인된 설계를 docs/ 권위 문서 체계에 반영하시겠습니까?" 요청.
```

---

## 7. 템플릿

### A. TASK_CONTEXT 템플릿

~~~~markdown
# TASK_CONTEXT: [작업 명칭]

## 1. 메타데이터 및 기준선 (Baseline)
- Task ID: `TC-[영역]-[날짜]-[순번]`
- 대상 브랜치: `[원격 대상 브랜치명 (예: work/grand-unified-20261112)]`
- 기준 원격 HEAD SHA: `[git fetch origin 후 확인한 정확한 40자리 commit hash]`
- 로컬 HEAD SHA: `[로컬 commit hash]`
- 조사 시점: `YYYY-MM-DD HH:MM:SS UTC`

## 2. 목표 및 기획 범위 (Scope)
- 작업 목적: [한 문장으로 요약]
- 포함 범위 (In-Scope): [Gemini가 다루어야 할 구체적 항목]
- 명시적 제외 범위 (Out-of-Scope): [이번 논의에서 다루지 않을 항목]

## 3. 권위 문서 및 우선순위 (Authority Routing)
1. 최상위 권위: `docs/[문서명].md` (docs/CONTENT_AUTHORITY_INDEX.md 기준)
2. 보조/참조 권위: `docs/[문서명].md`
> **주의:** 과거 문서 `[문서명]`의 [X] 항목은 최신 권위 문서에 의해 폐기(Superseded)되었으므로 참고하지 말 것.

## 4. 기존 구현 및 검증 현황 (3축 매트릭스)
| 시스템 / 컴포넌트 | 구현 상태 | 자동 검증 | 실사용 검증 | 근거 코드 / 문서 위치 | 비고 |
|---|---|---|---|---|---|
| 예: 화염검 우클릭 스킬 | IMPLEMENTED | PASS | PENDING | `FireSwordItem.java` | 자동화 CI 통과, 조작감 미검증 |
| 예: T2 코어 조합법 | UNIMPLEMENTED | NOT_RUN | NOT_RUN | `docs/CONTENT_...md` | 획득처만 존재, 레시피 없음 |

- **기존 문서와 코드 간의 불일치 사항:**
  - [불일치 내용: 코드는 A 방식이나 문서는 B로 기술되어 있음 등]

## 5. 불변 제약 및 변경 검토점 (Constraints & Review Points)
- **불변 원칙 (Hard Constraints):**
  - [예: 바닐라 레시피는 덮어쓰지 않고 대체 레시피로만 제공]
  - [예: 서버 권위 패킷 처리 필수, 클라이언트 임의 처리 불가]
- **기존 확정 사항 중 변경 검토 가능 영역:**
  - [예: 기존에 정의된 쿨다운 12초 규칙은 합당한 근거가 있다면 [변경제안] 가능]

## 6. 기획 허용 범위 (Design Freedoms)
- Gemini가 신규 제안을 완전히 자유롭게 제시할 수 있는 항목:
  - [자유 항목 1: 신규 아이템 명칭, 파티클 연출 콘셉트, 경험치 소모량 등]

## 7. 의존성 및 시스템 충돌 주의점 (Dependencies & Risks)
- 경제 밸런스 영향: [XP 공급원, 바닐라 주민 거래와의 연계]
- 다른 서브시스템 연계: [발전과제, 인벤토리 UI, 인챈트 호환성]

## 8. 확인된 사실 vs 추론 vs 불확실 사항 (Triangulation)
- **[FACT]**: [코드와 권위 문서가 모두 일치하며 검증된 사실]
- **[INFERENCE]**: [기존 구현 관례상 유력하나 명문화된 규칙은 없는 사항]
- **[CONFLICT/UNCERTAIN]**: [서로 다른 문서가 충돌하거나 확인이 필요한 사항]

## 9. 원본 코드 및 데이터 근거 발췌 (Raw Evidence)
```[언어]
// 기획에 반드시 필요한 원본 코드 스니펫 및 JSON 발췌
```
~~~~

---

### B. APPROVED_DESIGN 템플릿

~~~~markdown
# APPROVED_DESIGN: [작업 명칭]

## 1. 메타데이터 (Metadata)
- Task ID: `TC-[영역]-[날짜]-[순번]` (TASK_CONTEXT와 동일)
- Design Baseline Remote SHA: `[TASK_CONTEXT의 원격 기준 SHA]`
- 기획 승인 일시: `YYYY-MM-DD`
- 승인자: `User (Human)`

## 2. 사용자 승인 완료 명세 및 결정 근거 (Approved Specs & Decision Log)
### 2.1 결정 목록 및 사용자 승인 근거
| 결정 ID | 구분 | 결정 내용 요약 | 사용자 승인 근거 / 코멘트 |
|---|---|---|---|
| DEC-01 | [신규제안 승인] | 스킬 쿨타임 10초 설정 | "10초가 적당합니다. 확정합시다" 승인 |
| DEC-02 | [변경제안 승인] | 기존 마나 소모 방식에서 내구도 소모 방식으로 변경 | "마나 시스템 복잡도를 줄이기 위해 내구도 2 소모로 변경" 승인 |

### 2.2 세부 동작 메커니즘
- [동작 1: 트리거 조건, 수식, 결과]
- [동작 2: 상태 이상, 쿨다운, 범위 등]

### 2.3 확정 파라미터 및 수치 테이블
| 항목 | 값 | 단위 / 기준 | 연관 결정 ID | 비고 |
|---|---|---|---|---|
| 예: 기본 쿨타임 | 10 | 초 (200 ticks) | DEC-01 | 서버 틱 기준 |
| 예: 타격 범위 | 3.5 | 블록 반경 | DEC-03 | 원형 범위 |

### 2.4 데이터 / 리소스 요구사항
- 아이템/블록 ID: `moreminecrafts:[id]`
- Data Component 명세: `...`
- 번역 키: `item.moreminecrafts.[id]`

## 3. 기존 설계 변경점 (Deltas from Existing Authority)
- 변경 대상 권위 문서: `docs/[문서명].md`
- 연관 결정 ID: `DEC-02`
- 기존 명세: [이전 내용]
- 변경된 명세: [새로 합의된 내용]
- 변경 사유: [사용자 승인 사유]

## 4. 기각 및 보류된 아이디어 (Rejected / Deferred)
- `[아이디어 1]`: 기각 사유
- `[아이디어 2]`: 보류 사유 (향후 고려)

## 5. 미해결 항목 (Open Questions)
- **[BLOCKER]**: 없음 (BLOCKER 잔존 시 기획 완료 불가)
- **[NON-BLOCKER / LIVE-TUNING]**:
  - [항목 1: 예 - 파티클 밀도 및 사운드 볼륨 (인게임 확인 후 조정)]
  - [항목 2: 예 - 희귀 드롭율 1% vs 1.5% 미세 조정 (실제 사냥 테스트 후 확정)]

## 6. 구현 수용 기준 (Acceptance Criteria)
- [ ] AC 1: [구체적인 동작 조건]
- [ ] AC 2: [예외 상황 및 엣지 케이스 처리]
- [ ] AC 3: [멀티플레이 서버/클라이언트 동기화 검증]

## 7. 검증 계획 (Verification Plan)
- **단위 테스트 (JUnit):** [수식, 상태 변화, 태그 검증]
- **게임테스트 (Fabric GameTest):** [블록 상호작용, 엔티티 반응]
- **실사용 검증 (Live Acceptance):** [타격감, 조작 반응성, 시각 효과 등 PENDING 항목 검증 기준]
~~~~

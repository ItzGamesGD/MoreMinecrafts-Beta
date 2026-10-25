# CLASSIFICATION RECONCILIATION

개별 보상은 변경하지 않았다. 승인 원장과 실제 등록 가능 공급량을 분리해 보고한다.

## 원장 독립 재계산 (커밋된 A 문서 파싱)

| 항목 | 문서 주장 | 재계산 | 판정 |
|---|---:|---:|---|
| NOW 161개 | 163,250 | 163,250 | PASS |
| VERIFY 19개 | 16,750 | 16,750 | PASS |
| NOW+VERIFY | 180,000 | 180,000 | PASS |
| FUTURE 20개 | 29,100 | 29,100 | PASS |
| 전체 200개 | 209,100 | 209,100 | PASS |

중복 ID 0, 누락 0(200행), 50XP 단위 위반 0, 0 이하 보상 0, A 표 열 수 오류 0. **원장 산술은 PASS이지만, 분류(NOW)의 정당성은 FAIL이다(아래).**

## 권장 재분류

| 권장 | 의미 | 개수 | XP |
|---|---|---:|---:|
| NOW | 즉시 등록 가능(서버 판정 근거 확보) | 98 | 95,300 |
| NOW* | 서바이벌 달성 가능, 결함 수정 후 등록 | 62 | 60,750 |
| VERIFY | 검증·결정 전 미등록 | 20 | 23,950 |
| FUTURE | SURVIVAL-LOCKED | 20 | 29,100 |

분류 이동(현재→권장):

| 퀘스트 | 현재 | 권장 | XP | 사유 |
|---|---|---|---:|---|
| `xpq.exp.biomes_all_overworld` | NOW | VERIFY | 3,150 | D-06 목록 미정의 |
| `xpq.boss.monument_clear` | NOW | VERIFY | 1,450 | D-08 귀속 수단 없음 |
| `xpq.boss.monuments_3` | NOW | VERIFY | 3,450 | D-08 |
| `xpq.alc.shock_potion` | VERIFY | NOW | 500 | 26.3 BrewingRecipe 존재 + recipe/brewing/shock.json |
| `xpq.eqp.custom_enchant` | VERIFY | NOW | 350 | EnchantmentTrees에 MM 인챈트 11종 노드 존재(INFERRED) |

## 공급량 영향

- 승인 원장: 활성 180,000 / 전체 209,100 — **변동 없음**(보상 미수정).
- 즉시 등록 가능(NOW): **95,300XP / 98개**
- 결함 수정 후 등록 가능(NOW\*): +60,750XP / 62개 → 합 156,050XP
- VERIFY(통과 전 미등록): 23,950XP / 20개 (기존 19 → 20)
- FUTURE: 29,100XP / 20개. 재분류 없음 — 20개 모두 레시피·루트 부재를 확인했다. NOW로 옮길 수 있는 항목 없음.
- **충돌:** 승인안은 '활성(NOW+VERIFY) = 정확히 180,000'이다. 재분류는 활성 합계를 바꾸지 않지만, VERIFY↔NOW 이동은 '지금 등록되는 공급'을 바꾼다. 재정규화 금지(삭감 금지 원칙)를 유지하면 원장은 그대로 두고 등록 공급만 따로 관리하는 것이 맞다(AUTHORITY_CONFLICTS C-04).
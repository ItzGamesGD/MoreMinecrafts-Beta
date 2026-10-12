# Stage 2-B Knowledge acquisition condition corrections

Baseline: Stage 2-A PR #79 head `04fd5780658f847f74be6beda76754e08a0b5520`. This branch changes acquisition observation only. Knowledge rewards, Research prices, V3 prerequisite graphs, saved Knowledge flags, and unrelated progression behavior are out of scope.

The existing `ResearchProgressState` attachment remains the durable counter store. Distinct observations use the existing `condition.<key>.seen.<id>` keys, so repeat observations do not add progress and save/reload preserves both progress and deduplication. Previously unlocked Knowledge is never cleared. For K5 and K8, new observations are paused rather than derived from speculative taxonomies; historical progress and unlocks remain untouched.

| Node | Stage 2-B status | Event and evidence / limitation |
|---|---|---|
| K5 거목 생태 | PARTIAL — new progress paused | The approved wording requires 128 logs genuinely acquired from large trees, but supplies no taxonomy or provenance rule for what counts as a large tree. The old all-log statistic no longer advances K5. Saved counters and unlocks remain intact. No speculative tree classifier was added. |
| K8 암석 분류 | PARTIAL — new progress paused | The approved wording requires eight distinct eligible, survival-reachable rock types, but does not define the eligible roster. Broad base-stone tags are no longer counted. Saved counters and unlocks remain intact. |
| K12 고온 제련 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Requires a player's vanilla crafted-blast-furnace statistic and taking a non-empty product from a blast furnace output. Merely attempting to use a furnace, placing one, or crafting it without collected output does not qualify. The output-taking player must have a recorded blast-furnace craft. |
| K14 제작 기술 | PARTIAL — current authority interpretation retained | The approved wording says five tools crafted. The implementation retains cumulative crafted-item quantity rather than silently requiring five distinct tool IDs. The current finite list of counted tool items is retained because the authority does not define the complete tool roster; omissions require an explicit roster decision. |
| K18 물류 공학 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts successful server-side hopper item placements after the block-item placement reports success; item-use statistics and failed placement attempts do not count. Each successful placement is one event. |
| K19 자동화 원리 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts successful placements of the existing eligible set: redstone dust, repeater, comparator, observer, piston, and sticky piston. Redstone dust has its own placement hook. Other components are not added without authority. |
| K22 토양 관리 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts successful server-side hoe actions only when the clicked block is farmland after the action. Failed or no-op interactions do not count. |
| K23 관개 기술 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts a successful server-side water-bucket action only when it places source water within vanilla farmland hydration reach (horizontal Manhattan distance four, farmland at the same level or one block below the water). The condition target remains one. |
| K25 가축 사육 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts a species after an Animal interaction succeeds, consumes offered breeding food, and the animal accepts that food. The entity registry ID is the persistent distinct token. Pre-interaction callback attempts do not count. |
| K30 식품 가공 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Counts distinct food item IDs with actual crafted-item statistics; crafted result-slot output also records its item ID directly. Repeated crafting or cooking of the same type does not add progress. Cooking-stat behavior and every food-tag member remain CI/runtime validation items. |
| K34 궁술 이해 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Credits hostile deaths to the player owner of a direct arrow whose persisted weapon item is a bow. Holding a bow at death is irrelevant. |
| K35 석궁 구조 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Credits hostile deaths to the player owner of a direct arrow whose persisted weapon item is a crossbow, including arrow variants. Crossbow fireworks use temporary launch attribution because their projectile has no native weapon field; this path is not reload-persistent and remains unverified. |
| K38 투척 무기 | IMPLEMENTED_NOT_RUNTIME_VERIFIED | Credits a hostile death only when the direct damage entity is a thrown trident whose stored weapon is a trident and whose owner is a ServerPlayer. Holding a trident is irrelevant. |

## Validation limits

`KnowledgeConditionSemanticsTest` covers success versus failure predicates, item consumption, blast furnace craft-plus-output conjunction, bow/crossbow key distinction, threshold boundaries, persistent attachment codec round-trip, and distinct food-key deduplication. These are focused unit checks, not proof that Fabric mixins execute in a live server.

No Stage 2-B action-level GameTest has yet demonstrated these hooks. In particular, K12 crafting/output attribution, K18/K19 placement callbacks, K22/K23 world transitions, K25 species attribution, K30 cooking statistics, and K34–K38 real damage-source attribution remain **IMPLEMENTED_NOT_RUNTIME_VERIFIED** pending CI/runtime evidence.

## Stage boundary and integration

`KnowledgeRuntime.java` is shared with Stage 2-A: Stage 2-B edits its event-registration and statistic-refresh regions; PR #79 edits its central Knowledge unlock announcement and reconciliation behavior. These are separate logical sections, but a later integration may conflict near imports, initialization, or death-event registration. Resolve only those overlaps while preserving both the Stage 2-A announcement/history behavior and the Stage 2-B condition observers. No Stage 2-A code or branch was modified here.

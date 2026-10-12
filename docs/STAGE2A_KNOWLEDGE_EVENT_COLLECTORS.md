# Stage 2-A Knowledge collector implementation status

This change adds observers for the approved Stage 2-A conditions without changing catalog targets, rewards, purchase rules, or progression storage. All progress is written through `KnowledgeRuntime` into the existing persistent `ResearchProgressState` attachment; its existing condition update path unlocks entries and syncs deltas.

Status reflects evidence available at this commit: **IMPLEMENTED_NOT_VERIFIED** means a runtime hook is present, but compilation and actual hook execution have not yet been demonstrated. The local Gradle process cannot start in this environment because wildcard network-interface enumeration fails with `SocketException: Operation not permitted`.

| ID / name | Condition and target | Event / successful action | Collector and attribution / deduplication | Current status |
|---|---|---|---|---|
| K11 제련 원리 | `ore_types_smelted`, 5 | A recipe-backed cooking result recorded by a furnace, blast furnace, or smoker and awarded when a player collects furnace output. Only ore-family inputs recognized by `KnowledgeEventRules` count. | `KnowledgeFurnaceMixin` → `KnowledgeEventCollectors.smeltingCollected`; distinct input family tokens use `recordDistinctCondition`. Vanilla XP and recipe award behavior is left intact. | IMPLEMENTED_NOT_VERIFIED |
| K36 방패 방어 | `shield_blocks`, 50 | A server-side shield block that reduces positive incoming damage. | `KnowledgeShieldBlockMixin` observes `LivingEntity.blockedByItem`; only a `ServerPlayer` holding a `ShieldItem` receives progress. One invocation represents one blocked hit. | IMPLEMENTED_NOT_VERIFIED |
| K40 특수 탄약 | `special_ammunition_used`, 1 | A bow/crossbow projectile launch using a tipped arrow or firework rocket. Ordinary and spectral arrows are excluded. | `KnowledgeProjectileWeaponMixin` observes `shootProjectile` after launch; the shooter is credited. Counter is capped by the existing condition target. | IMPLEMENTED_NOT_VERIFIED |
| K48 피글린 사회 | `piglin_barters`, 20 | Piglin barter branch completes with a non-empty response list. Offering or consuming gold without a response does not count. | `KnowledgePiglinBarterMixin` carries the direct player or dropped gold owner's identity through admiration, then records at the barter response call. Pending attribution is removed at completion/cancellation. | IMPLEMENTED_NOT_VERIFIED |
| K59 고고학 | `archaeology_items_brushed`, 10 | A brushable block successfully spawns an archaeology item for a server player. Ordinary brush swings and failed entity spawns do not count. | `KnowledgeArchaeologyMixin` wraps the actual `addFreshEntity` result; the brusher is credited, and spawned stack count is counted once per successful spawn. | IMPLEMENTED_NOT_VERIFIED |
| K63 양조 기초 | `potions_brewed`, 1 | Player takes a potion item from one of a brewing stand's three output slots. | `KnowledgeBrewingOutputMixin` observes `Slot.onTake`; shared potion collector records output count. | IMPLEMENTED_NOT_VERIFIED |
| K64 물약 성질 | `potion_types_brewed`, 5 | Player takes five distinct registered potion types from brewing stand outputs. | Shared potion collector records the potion registry key through `recordDistinctCondition`, which ignores repeats. | IMPLEMENTED_NOT_VERIFIED |
| K65 투척 물약 | `splash_potions_brewed`, 1 | Player takes a splash potion from a brewing stand output. | Shared potion collector distinguishes `SplashPotionItem` from regular and lingering potions. | IMPLEMENTED_NOT_VERIFIED |
| K66 잔류 물약 | `lingering_potions_brewed`, 1 | Player takes a lingering potion from a brewing stand output. | Shared potion collector distinguishes `LingeringPotionItem`. | IMPLEMENTED_NOT_VERIFIED |
| K69 고급 양조 | `potions_brewed`, 50 | Player takes 50 potion bottles from brewing stand outputs. | Shared cumulative counter with K63; existing catalog thresholds independently unlock K63 and K69. | IMPLEMENTED_NOT_VERIFIED |
| K70 물약 농축 | `strengthened_potions_brewed`, 5 | Player takes registered vanilla strengthened potion types (`strong_*`) from a brewing stand output. | Shared potion collector checks the registered potion ID and increments only for `strong_*`; ordinary brews do not count. This uses the approved “강화 물약 5개 양조” mechanic and adds no custom concentration mechanic. | IMPLEMENTED_NOT_VERIFIED |

K51 remains excluded and unchanged. Reward effects remain separate from acquisition: this collector work does not add or claim consumers for metadata-only rewards.

## Tests and evidence

- `KnowledgeEventRulesTest` covers ore-family normalization, non-ore exclusion, registered strengthened versus ordinary potion distinction, unique potion IDs, and approved versus ordinary ammunition identification.
- These are predicate tests only. They do not establish runtime mixin application or prove that an action reached a hook in a running server.
- Full project validation requested for this stage could not start locally due to the container's denied wildcard socket enumeration. CI is required to establish compilation and existing test results; targeted GameTests for each event hook remain outstanding unless separately added and run.

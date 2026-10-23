# Q1-A / Q1-A2 parallel interface contract (2026-10-17)

Both branches start from the same PR #112 commit, the one that adds this file. Neither branch may depend on code that exists only in the other branch.

## Shared surface — both branches use it, neither branch changes it

| Symbol | Contract |
|---|---|
| `ResearchProgressState` | Existing public methods (`get`, `with`, `questProgress`, `questComplete`, `questClaimed`, `withQuestProgress`, `completeQuest`, `claimQuest`) and the flat `Map<String,Integer>` codec stay unchanged. New keys are added only by Q1-A. |
| `QuestDefinition` | The 7-argument canonical constructor `(id, category, titleKey, descriptionKey, objectiveKey, objectiveTarget, rewardRawXp)` is preserved. |
| `QuestCatalog` | `register`, `byId` and `entries` keep their signatures. |
| `KnowledgeRuntime` | `state(player)` and `updateState(player, state)` keep their signatures. `updateState` stays the single write path that bumps the revision and sends the delta. |
| Key namespace | `quest.progress.<id>`, `quest.complete.<id>`, `quest.claimed.<id>`: meaning unchanged. Q1-A adds `quest.counter.<key>`, `quest.seen.<questId>.<entry>` and `quest.schema`. Q1-A2 reads only `quest.complete.*` and `quest.claimed.*`. |

## File ownership

| Branch | Owns (may modify) | Must not modify |
|---|---|---|
| **Q1-A** `claude/xp-quest-q1a-core` | `QuestRules` (additive only), `QuestCatalog` (additive only); new `progression/XpQuest*` / `Quest*` tracking classes; catalog data resource `data/moreminecrafts/xp_quest/catalog.json`; one initializer line in `ModRegistries`; new unit tests and GameTests; gametest `fabric.mod.json`, appending entries at the **end** of the list | `RawXp`, `XpLevels`, `QuestProgressService`, `Enhancement*`, `ResearchPurchaseService`, `T1Operations`, `AnvilMenuMixin`, `T1AnvilScreen` |
| **Q1-A2** `claude/xp-quest-q1a2-exact-xp` | `RawXp`, `XpLevels`, new `XpMath`; `QuestProgressService` (claim transaction); `EnhancementRules`, `EnhancementOperation`; `T1AnvilScreen` (cost display); `ResearchPurchaseService` (XP deduction path only); `QuestProgressGameTests`; new XP / Enhancement tests; gametest `fabric.mod.json`, inserting its entries **directly after** the existing `QuestProgressGameTests` line | `ResearchProgressState`, `QuestRules`, `QuestCatalog`, `QuestDefinition`, `KnowledgeRuntime`, the quest key layout, the catalog data resource |

## Behavioural contract

- **Q1-A:**
  - Completes quests only through `ResearchProgressState.completeQuest`, written via `KnowledgeRuntime.updateState`.
  - Never grants XP or calls `QuestProgressService.claim`.
  - Never sets `quest.claimed`.
- **Q1-A2:**
  - Never sets `quest.complete` or `quest.progress`.
  - The claim succeeds only for an already-complete, unclaimed quest. A grant failure rolls back the claimed flag and the XP fields (approved authority, 2026-10-17).
- **Registration:**
  - No production quest is registered by either branch. Each of the 160 NOW quests is registered only after its own automated test passes (authority). The trigger hooks for most quests arrive in Q1-B and later.
  - GameTests register their own test-only quests.

## Merge order

The two branches can merge in either order. Expected overlap:
- the gametest `fabric.mod.json` list, in separated hunks;
- nothing else.

After both merge, the existing `QuestProgressGameTests` and both new suites must pass together.

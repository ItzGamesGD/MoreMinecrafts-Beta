# Phase 3 — Integration readiness plan

**Status: AUDIT / PROPOSAL ONLY. STOP AFTER THIS AUDIT.** No implementation, merge, closure, rebase or production-branch change is authorized. Owner approval and unresolved WHAT decisions precede any integration task.

Snapshot: 2026-10-23T21:45:28+09:00. Documentation-only audit; no integration, closure, deletion, rebase, force push, authority or gameplay change.

Main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3`; protected accessory+bounty `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`; Batch A #156 `05a378211a6e4576609416096d3515a6b1dee36f`. None of #156 or the protected integration is in main (Git ancestry checked).


## Goal, source discipline and architecture

Prepare one new future integration candidate from protected `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`, retaining its accessory/bounty tree while selectively composing the already-recovered #156 foundation. Reuse existing code/tests, not a replay of historical merges. Keep implementation, individual objective approval, automated verification and final live acceptance separate. The audit itself is based on main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3` and changes only four documentation artifacts.

This is a readiness plan, not a low-level implementation specification for unfinished features. Where an owner requirement or receipt protocol is incomplete, the batch stops; it does not invent gameplay or treat an implementation choice as an approved design.

## Strategy comparison

| Strategy | Semantic preservation risk | Conflict probability | Verification effort | Regression risk | Reviewability | Future parallel maintenance |
|---|---|---|---|---|---|---|
| A — deepest linear chain + sibling restoration | High if #128/#130 is chosen: inherits unfinished skull/76th registration and P1 replay defect. #125 is safer but #156 already recovers it. | High for older complete-file replay; overlaps shared progression/anvil/network/locale code. | Apparently low initially, high for proving absence of lost sibling tests and reverted fixes. | High across whole cumulative tree and registration changes. | Poor if historical PRs are merged wholesale. | Repeated stale-base merges and difficult provenance. |
| B — independent functional batches on existing baselines | Medium when exact manifests are maintained; risks omitting shared state/event hooks. | Medium per batch, with cross-batch event/registration conflicts. | Higher upfront invariant/manifest checks; bounded failures. | Lower with catalog/state/Research/UI gates. | Good, but many separate branches could drift. | Manageable with pinned interfaces, otherwise repeated reconciliation. |
| C — new clean integration candidate from protected exact SHA, using B’s batch boundaries | Lowest controllable risk: preserves already-tested accessory/bounty and imports known #156 endpoints once; unique donors remain explicit. | Known: 13 common changed paths, 4 per-file textual conflicts; semantic review also required on cleanly composed shared files. | Significant once, reusing #156 tests/manifests and existing JDK25 workflows. | Medium until shared XP/anvil and saved-state tests pass; rollback can stop at each accepted checkpoint. | Best: source-pinned per-batch deltas, no old branch rewriting. | Best: existing agents retain branches; new work targets accepted checkpoint only after review. |

**Recommendation: C, executed with B’s functional batches.** Do not choose #130 as the deepest donor or create/rebase a new branch during this audit. #156 is a donor, not an approved release. Integration must retain current main security protections too; main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3` and both candidates descend from `5f92307555ce5c486a2e0e8091f956bcbb6c6646`, so inspect the current-main security delta explicitly rather than assuming it is already in the protected SHA.

## Immutable donor ledger

| Role | Exact source |
|---|---|
| Protected candidate starting point | `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac` |
| Current main/security baseline | `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3` |
| Recovered cumulative foundation | #156 `05a378211a6e4576609416096d3515a6b1dee36f` |
| Original cumulative recovery/provenance reference | #125 `079d36ac3bd2d8950118e30647d5f2efad19f947` |
| Missing validation donor | #93 `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955` |
| Correct pricing descriptions | #116 `8f33e72c8a71d29e132050457535ac8a0092a7b0` |
| Optional client QA design/harness | #106 `9b66f49657c084c29873990c0ff8b92d6a0ab620`; #107 `dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6` |
| UI/presentation donors (already adapted in #156) | #126 `a9b89755f7c4e924243fa4229a90177433712be4`; #129 `2176935929c990a210fae64582d524d30993bc8a`; #135 `9a246f1e0cef5e3f39c4bf47b34df6e63877df30` |
| Current owner-decision/content record | #140 `3e66cfbdfa6a12576ecf0162108250691318c8db` |
| Skull work, explicitly withheld | #127 `334997d2f81d6b324b0cccd3f40b538c921edadc`; #128 `6ba15ac2cb40cc919dfee41b24f7813231d11efb`; #130 `9c9f22577370cd0160dd2b43b0e92caa7832f420` |
| Protected subsystem references | #151 `5ad001b091ea24705834a1caab94335a6abcc445`; #161 `0e9ccc5af9313d3648d4bce98a8ee33b1f2dba82` |

All source PRs’ exact HEADs are additionally listed in the graph and JSON. Do not fetch a branch name later and silently substitute a new HEAD. A changed source cancels that batch pending re-audit.

## Global constraints and review focus

- Preserve main, protected integration, all source feature branches, all existing PR state/base settings and trusted checkpoints.
- No cherry-pick/merge merely to make ancestry look complete; the accepted unit is a verified subsystem/file/test manifest.
- Preserve 200 stable quest IDs and existing save/claimed records. Do not activate 125 unregistered slots or the skull 76th registration. The historical 75 contain fourteen later-content holds, so owner release/migration policy is a gate, not an inferred approval.
- Preserve positive ore v2, reliable forward-only structure entry, per-player attribution, exact raw-XP and claim-once/rollback behavior; drawn presentation edges never become gameplay prerequisites.
- Resolve custom XP canvas versus routed vanilla AdvancementsScreen authority before accepting the UI. No WHAT decision is changed by this plan.
- Build/test success is not final live/physical input acceptance. Clean restart is not crash/power-loss durability proof.
- Sources remain recoverable at immutable SHA; any future branch/output SHA must be recorded before the next batch begins.

Review the five most failure-prone compositions: mixed anvil/accessory raw-XP charging; stale/duplicate/reordered claim packets; old saves with claimed/unclaimed and ore v1 data; multiplayer event recipients/merchant/loot attribution; GUI scale/resize/keyboard/drag-versus-claim behavior. Each is assigned below.

## Ordered batches

**Base-commit convention:** B0 has exact target `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac` and no code output. B1 starts at that exact commit. B2–B7 depend on the previous accepted candidate output, still rooted in `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`. Those future SHAs do not yet exist; recording the actual output SHA and source/ref recheck is a mandatory start condition. This plan deliberately does not invent future commit hashes or authorize starting a batch without them. Each rollback means restoring the isolated candidate to its recorded pre-batch checkpoint/reverting that batch after coordination; never force-push an existing branch or downgrade a valuable save.

### B0 — owner scope, authority, preservation and checkpoint gate

- **Sources:** protected `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`, main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3`, #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #140 `3e66cfbdfa6a12576ecf0162108250691318c8db`; #154 `84550ad4ef0a09248934641e26dab61c4f2cd13b`; #139 `526416b16a590abf558534a8426625508c23ad99`.
- **Base:** exact protected SHA; no integration branch created by the audit.
- **Files/subsystems:** `docs/CONTENT_AUTHORITY_INDEX.md`, approved UI contract, #156 `SOURCE_MANIFEST.json`, `QUEST_RETENTION.json`, #140 owner-decision log, current main security delta.
- **Dependencies/conflicts:** resolve custom screen contract and fourteen affected historical registrations, old-save/migration treatment, release/translation scope, allowed visual deferrals. Preserve all 200 slots and provisional budget unless separately changed by owner.
- **Checks/acceptance:** inventory and source SHAs agree; every shared state key/hook has one owner; approved release scope recorded durably by a separately authorized task. No gameplay mutation now. Unit/GameTests/smoke: NOT RUN for this non-code gate.
- **Rollback:** no production state change; retain current refs. **Potential closure:** none.

### B1 — shared progression state, exact XP and claim core

- **Sources:** #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #118 `c40ee6b1656800f016644893f5bac7a9c90827b2`; #115 `f30d263c93b89b1bab844893bdb82084bfe209bd`; #114 `78d1495402de6f3d7328fcd3117a4996137a05e2`; #112 `63107098fcde6742704a07394439e5cdf9c6ad49`; original sources are references, #156 is the code donor.
- **Base:** exact `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac` after B0 approval; also preserve current-main security protections after a reviewed diff.
- **Files/subsystems:** `ResearchProgressState`, `KnowledgeRuntime`, `RawXp`, `XpMath`, `QuestCatalog/Rules/ProgressService`, `XpQuestCatalogData`, catalog JSON, shared progression payloads/client API and initialization.
- **Dependencies/conflicts:** union `ModDataComponents`, `ModRegistries`, `ModNetworking`, mixin/GameTest metadata with bounty; keep its item-bound contracts independent of claim-once Quest raw-XP. Compose common `AnvilMenuMixin` with accessory catalyst logic; never replace it with an older whole file.
- **Required tests:** `ExactXpTransactionGameTests`, `QuestProgressGameTests`, `XpQuestCatalogDataTest`, `QuestClaimGateTest`, `ResearchProgressState`/catalog/legacy decode cases; duplicate/stale/off-thread/dead/disconnected claims, injected grant rollback, XP total/level/progress/score, insufficient costs, normal anvil and accessory +5/+10 costs. Compare exact registered set and all 200 rewards to approved B0 manifest.
- **Smoke/acceptance:** existing Fabric CI JDK25 build + server/client smoke on actual candidate SHA. Completion grants no Quest XP until claim; duplicate cannot pay twice; held claims survive saved-player reload. No power-loss atomicity claim.
- **Rollback:** restore pre-B1 isolated checkpoint and copied test save; never erase real claimed IDs/XP. **Potential closure after complete target verification:** #115/#112, then other core PRs only after their evolved catalog/tracker scope is accounted for.

### B2 — Research roots, Stage23 effects, furnace, fishing, economy

- **Sources:** #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #125 `079d36ac3bd2d8950118e30647d5f2efad19f947`; #116 `8f33e72c8a71d29e132050457535ac8a0092a7b0`; #93 `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955`; #88–#110/#123/#124 are preserved reference envelopes, not independent whole-PR imports.
- **Base:** accepted B1 output (pin before execution), ancestry rooted at `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`.
- **Files/subsystems:** `ResearchPurchaseService/Rules/Rewards`, root/passive adapters, farming/mining/movement/XP/fishing hooks, furnace attribution/state/portable components, `ResearchVillagerDiscount`, Soil state, corresponding tests/authority routes.
- **Dependencies/conflicts:** preserve one canonical handler per effect; keep final-drop Ore Mining and pre-enchantment Lumbering ordering distinct. Repair only stale pricing descriptions from #116. Compose furnace components and registry entries with protected accessories/contracts. Do not implement DATA_ONLY effects or current/max stock display merely as integration work.
- **Required tests:** `ResearchPurchasePrerequisiteTest`, research ledger/root migration/passive tests; `Research23AEffectRulesTest` all 24 Fisheries tables plus codec/ties/sum100; actual catching-player/.23 payload tests; `FurnaceResearchInfrastructureGameTests`, `PortableFurnaceResearchGameTests` and restart fixtures; `VillagerTradingGameTests`, `SoilStudyGameTests`, ceiling rounding/capped demand/fixed books/rebuild idempotence, crop owner/harvester/fallback; paid-only and multiplayer isolation.
- **Smoke/acceptance:** Fabric build/server/client smoke; existing K5 mapped/restart/crafting probes on actual candidate. Retain the explicit 43/42/2 effect-status limits; no silent runtime duplicates. Missing #93 validation is adapted in B5 after equivalent-coverage review.
- **Rollback:** revert B2 only on isolated candidate; restore copied furnace/player saves, no source branch edits. **Potential closure:** #123/#124/#125 are narrow exact-endpoint candidates; older effect PRs require all unique tests/docs and content gates checked first.

### B3 — XP event tracking and evidence reconciliation

- **Sources:** #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #121 `17e980912d636b0572a3f25b24995ffde31c754c`; #120 `fd4aa831139054f6c736edf58fa6fe5f8cc77814`; #119 `c7fe7fabc5a3bc486e2be0aed8f793628f42ec72`; #128/#130 excluded.
- **Base:** accepted B2 output (pin before execution).
- **Files/subsystems:** `XpQuestTracking/Hooks/Evaluator`, positive `QuestOreProvenance`, structure loot/entry/vault, combat/dragon/raid/monument helpers/mixins, persistent evidence and dedicated restart fixtures.
- **Dependencies/conflicts:** shared `KnowledgeRuntime`, drop/item/player mixins and initialization now coexist with bounty progress hooks; a single action may correctly progress both systems while keeping evidence and rewards separate. Never use contract progress as Quest evidence. No proximity-to-entry, distance-to-new-objective, or species-to-biome inference.
- **Required tests:** `XpQuestEventTrackingGameTests`, ore generation/placed/unknown/piston/legacy negative cases and `QuestOreRestartGameTests`; `QuestStructureLootGameTests`, loot/vault/entry restart, `QuestCombatGameTests`, `QuestDragonRematchGameTests`, `QuestRaidGameTests`, `QuestMonumentGameTests` plus restart suites. Test exact recipients, repeat callbacks, disconnect/rejoin/death and clean-JVM world reload; do not register the 76th skull quest.
- **Smoke/acceptance:** full existing CI + smoke, catalog roster unchanged from approved B0 manifest; no false retroactive grants and no per-player leakage. Real multiplayer/natural survival acceptance remains a separate gate.
- **Rollback:** previous candidate checkpoint and consistent copied player/chunk saves; do not drop live evidence keys to claim success. **Potential closure:** #120/#121/#119 only after all original validation/documentation scope is explicitly resolved; no automatic ancestor closures.

### B4 — approved UI/presentation composition

- **Sources:** #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #135 `9a246f1e0cef5e3f39c4bf47b34df6e63877df30`; #129 `2176935929c990a210fae64582d524d30993bc8a`; #126 `a9b89755f7c4e924243fa4229a90177433712be4`.
- **Base:** accepted B3 output; B0 UI authority decision is mandatory.
- **Files/subsystems:** selector/seven-sector routes, `ProgressionBrowseScreen/TreeCanvas/ClientApi`, Quest layout/structure/zoom/tooltip/icon data, native completion toast and claim indicators; associated localization and client fixtures.
- **Dependencies/conflicts:** `T1AnvilScreen` is one of the four proven textual conflicts; preserve protected catalyst slot/cost layout while composing exact XP. Union both locale files, do not lose bounty or accessory keys. #129 preview edges/candidates remain non-gameplay and cannot claim.
- **Required tests:** `QuestTreeLayoutTest`, `QuestPresentationDataTest`, `QuestPreviewIsolationTest`, `QuestClaimGateTest`, tooltip/zoom/LOD and selector input tests. Client checks must cover centers/borders/resize/GUI scales1–4, mouse/Tab/Shift+Tab/arrows/Enter/Space, drag rejection, complete-unclaimed/claimed states, notification replay suppression and disconnect reset.
- **Smoke/acceptance:** generic smoke plus existing visual/client fixtures on actual candidate; physical owner input and final authority compliance required. English fallback remains a stated limitation until accepted/translated. No new quest activation via UI.
- **Rollback:** pre-B4 UI checkpoint without reverting server claimed state; restore test saves only. **Potential closure:** #126/#129/#135 after authority and physical UI acceptance; not currently CLOSE_READY.

### B5 — missing validation and optional QA restoration

- **Sources:** #93 `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955`; #116 `8f33e72c8a71d29e132050457535ac8a0092a7b0`; #107 `dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6`; #106 `9b66f49657c084c29873990c0ff8b92d6a0ab620` plus current candidate.
- **Base:** accepted B4 output (pin before execution).
- **Files/subsystems:** selectively adapt #93 movement/orb/boundary validation; retain #116 prose corrections; optional separate clientQa tooling and verifier/build source-set changes only if owner chooses that infrastructure.
- **Dependencies/conflicts:** canonical runtime stays unchanged unless a separately scoped confirmed defect requires repair. Existing workflow `client-live-qa.yml` and `build.gradle` must compose with protected accessory resource validation; QA mod must be absent from distribution. Do not duplicate a test merely because its original method name disappeared.
- **Required tests:** reconcile eight named #93 methods against current collection/movement tests; retain missing exact 4/6-block inside/on/outside, two-player/Mending/exact XP and actual movement-event cases. For QA harness, run its load verifier and inspect distributable exclusions; label OS input, callbacks, fixture state and human verdict separately.
- **Smoke/acceptance:** existing JDK25 workflows + smoke; focused suites run on final candidate SHA, not donor HEAD. A screenshot or pixel statistic is not a visual PASS.
- **Rollback:** revert validation/tooling separately; keep gameplay snapshot. **Potential closure:** #93/#116/#106/#107 only if every unique original validation/report path is retained or explicitly disposed of and agent/child dependencies are rechecked.

### B6 — skull provenance: blocked, separately authorized follow-up

- **Sources:** #127 `334997d2f81d6b324b0cccd3f40b538c921edadc`; #128 `6ba15ac2cb40cc919dfee41b24f7813231d11efb`; #130 `9c9f22577370cd0160dd2b43b0e92caa7832f420`.
- **Base:** future accepted B5 output only after an explicit new task/receipt contract; **do not execute as an ordinary integration batch**.
- **Files/subsystems:** skull issuance/pickup/merge/hopper/drop/persistence evidence and catalog registration; #130 P2 actual-inventory-gain logic is a candidate donor, not a standalone fix for the whole system.
- **Dependencies/conflicts:** current target has no skull implementation and remains at75; introducing #128 would enable76. P1 intent/receipt/journal ownership and failure policy must be resolved before implementation; never silently accept Boolean/entity UUID replay or guess a durable receipt design.
- **Required tests:** reproduce current P2/P1 evidence in disposable worlds; actual creative full/partial inventory, split/merge/mismatched components, hoppers/drop/portal/unload, two recipients. Require independent two-JVM write/halt/read P1 GREEN on the same entity with A=1,B=0; consistent saved-player projection, tokenless data, I/O/corruption cases need a separately approved durable model. Default suite PASS cannot replace opt-in forced-halt test.
- **Smoke/acceptance:** full build + server/client smoke after a real fix, catalog exact set and owner live fortress/multiplayer/restart acceptance. Process-kill tests do not prove power-loss guarantees.
- **Rollback:** maintain75 target until approved safe implementation; preserve original #128/#130 and fixture evidence. **Potential closure:** none while P1 is blocked.

### B7 — final combined-candidate acceptance and later cleanup review

- **Sources:** accepted B1–B5 outputs, optional B6 only if separately approved, protected `63efc3ca0a71d369d9e7aa25b9e5231e6a7b29ac`, main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3`, #156 `05a378211a6e4576609416096d3515a6b1dee36f`; #151 `5ad001b091ea24705834a1caab94335a6abcc445`; #161 `0e9ccc5af9313d3648d4bce98a8ee33b1f2dba82`.
- **Base:** exact last accepted candidate SHA, pinned before tests. No merge to main is part of this plan.
- **Files/subsystems:** full source/authority/test registration manifest; no unrelated implementation.
- **Dependencies/conflicts:** all thirteen common paths, plus every migrated state/component/event boundary. Recheck main/security delta and source refs against frozen audit inventory.
- **Required verification:** existing `./gradlew clean test build compileGametestJava runGameTest` on Java25, server/client smoke, K5 restart/mapped/crafting/isolation and relevant existing client/resource checks. Run targeted exact-XP/anvil/accessory catalyst, reward overflow, claim and contract duplicate/stale packets, Research purchase, progression save/reload and two-player isolation. Run existing history scan without new suppressions.
- **Acceptance:** all required exact-candidate CI green; owner-approved screen/content scope; declared final real-play/physical/old-save gates complete or explicitly deferred by owner. #39936070097 and #156 donor green checks are references only, never this combined candidate’s result.
- **Rollback:** keep recorded pre-batch source refs and copied consistent saves; abandon/revert the new isolated candidate, not existing branches. **Potential closure:** re-audit narrow exact candidates #123/#124/#125/#140 first; then source-specific ancestors/donors after their unique test/document/content gates. No CLOSE_READY, PR closure or branch deletion is authorized by this audit.

## Exact conflict checklist

Textually conflicting: `src/client/java/dev/itzgamesgd/moreminecrafts/client/T1AnvilScreen.java`, `src/gametest/resources/fabric.mod.json`, `src/main/resources/assets/moreminecrafts/lang/en_us.json`, `src/main/resources/assets/moreminecrafts/lang/ko_kr.json`.

Shared but textually clean: `.github/workflows/k5-production-restart.yml`, `build.gradle`, `docs/CONTENT_AUTHORITY_INDEX.md`, `src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java`, `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AnvilMenuMixin.java`, `src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModDataComponents.java`, `src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModRegistries.java`, `src/main/resources/moreminecrafts.mixins.json`. `docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md` is identical. Preserve both lists’ semantic responsibilities; no replacement-by-older-file shortcut.

## Verification performed by this audit

Current remote PR/branch/API data and non-shallow local Git objects were reconciled. Changed-path endpoint blobs, actual ancestry, branch/base drift, exact witness sets, catalog totals and per-file three-way text composition were computed. Existing audit reports, critical implementation/test source and current exact-head CI records were read. The JSON/Markdown scope, identifiers, evidence paths and four-file documentation-only diff are checked before publishing.

Gradle builds, GameTests, server/client/physical play and forced-halt tests: **NOT RUN by this audit**. No code was modified; prior results are labeled by source SHA. Ownership of private agent sessions remains UNKNOWN. No active work was interrupted.

## Publication and stop condition

Only audit branch `audit/phase3-stage23-xpquest-readiness-20261023` and a new Draft PR against main are created, as explicitly requested. Existing PRs and production/feature/integration branch refs must be byte-for-byte unchanged on final remote comparison. Publishing adds one open Draft audit PR: snapshot63 → expected64, absent concurrent changes. Stop after audit publication; wait for owner approval before creating any feature integration candidate or applying any batch.

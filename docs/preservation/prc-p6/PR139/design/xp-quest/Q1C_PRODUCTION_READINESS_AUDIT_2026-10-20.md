# Q1-C Production Integration Readiness Audit — 2026-10-20

**Status:** Audit complete; no gameplay or UI implementation authorized.
**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Audit branch:** `codex/q1c-production-readiness-audit-20261020`  
**Verified baseline:** `main` / `5f92307555ce5c486a2e0e8091f956bcbb6c6646`  
**Previous UI audit:** [checkpoint](INTEGRATION_CHECKPOINT_2026-10-20.md)

## Continuation checkpoint

Completed:
- Read `AGENTS.md` and the prior UI integration checkpoint.
- Reauthenticated GitHub. Authenticated account `ItzGamesGD`; repository metadata grants push permission.
- Fetched `main`; confirmed SHA `5f92307555ce5c486a2e0e8091f956bcbb6c6646`.
- Created this dedicated audit branch from that exact `main` commit; remote branch creation succeeded.
- Refreshed PR metadata, file lists, review submissions and workflow runs for the Q1-C dependency chain through #135.
- Confirmed Q1-C `17e980912d636b0572a3f25b24995ffde31c754c` is 139 commits / 376 files ahead of `main`, with `main` as merge-base.
- Confirmed `integration/stage23-final-reconciliation-20261016` is 76 commits / 231 files ahead of `main`. PR #104 merged to that integration line only, not to `main`.

Completed publication: Draft audit PR [#139](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/139), targeting main. The shell git push attempt was blocked by the unavailable network proxy; the complete audit document was synchronized to the dedicated GitHub branch with the authenticated GitHub contents API. GitHub PR metadata is the authority for the current remote head and Draft state. No uncommitted work remains. Local commits f6b3403 and f2dca0b are equivalent local copies of the audit document; the GitHub API created the authoritative remote commits.

## Phase A — verified dependency inventory

PR refs and workflow state below were refreshed from GitHub on 2026-10-20. Unless called out, PRs are open Draft and have no submitted review records. `mergeable=true` is not approval. CI refers only to workflow runs returned for the exact head SHA.

| PR / purpose | Base → head (exact head SHA) | State, reviews, exact-head CI | Changed-file scope and XP Quest relevance |
|---|---|---|---|
| #104 — mob resources/accessory consolidation | `integration/stage23-final-reconciliation-20261016` → `integration/stage23-mobs-accessories-20261016` (`b0de6b754859e090427ff310887ca04188310a8a`) | Closed, merged 2026-10-16 into integration branch; review list empty; Fabric and 3 K5 workflows passed. | Build config, mob catalog/renderers/assets, accessory design/tests. Independent; not needed for XP Quest. |
| #105 — portable-furnace persistence live fix | integration line → `codex/stage23-livefix-b-20261016` (`5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`) | Open Draft; no reviews; 5 workflows passed. | Furnace persistence/content authority/tests, plus client validation harness. Independent gameplay; not needed. |
| #108 — Stage 23 expanded batch 2 and portable-furnace research | #105 → `codex/stage23a-expanded-batch2-20261017` (`2c8e984314bb079378cffc95c4f48982a962173b`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Research rules/runtime/mixins, furnace/mob support and tests. Independent; not needed. |
| #109 — Stage 23-A remaining research campaign | #108 → `codex/stage23a-remaining-research-20261017` (`9376777264136b37e10071adf8ed59c08da9b6ee`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Research movement/riding/effects, mob capture, tests/manifest. Independent; not needed. |
| #110 — villager discount and Soil Study | #109 → `claude/stage23a-economy-soil-20261017` (`9ec91dc53512796748a6cfc0980cda0b65e764e6`) | Open Draft; no reviews; exact-head Fabric, K5 restart, K5 crafting and K5 mapped-source workflows passed. | Villager pricing, Soil Study runtime/mixins, locales/tests. Independent; not needed. #116 later documents historical P1 fixes, stock-display gap and manual QA. |
| #112 — XP Quest authority and design docs A–D | #110 → `claude/xp-quest-authority-20261017` (`63107098fcde6742704a07394439e5cdf9c6ad49`) | Open Draft; no reviews; Fabric, visual, K5 and secret-audit workflows passed. | Authority/docs and reconciliation. Needed to interpret Q1, but stacked on unrelated Stage 23 work. |
| #118 — Q1 core + exact XP integration validation | #112 → `claude/xp-quest-q1-integration` (`c40ee6b1656800f016644893f5bac7a9c90827b2`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Q1-A catalog/evaluator/rules, XP math/transactions, enhancement transaction code and tests. Core gameplay dependency with additional XP/enhancement service integration. |
| #119 — Q1-B tracking and ore provenance | #118 → `claude/xp-quest-q1b-tracking` (`c7fe7fabc5a3bc486e2be0aed8f793628f42ec72`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Tracker hooks/mixins, ore provenance, catalog/evaluator, restart/tracking tests. Needed for B quests; source attribution and acceptance gates apply. |
| #120 — positive natural-ore provenance correction | #119 → `codex/xp-quest-q1b-correction` (`fd4aa831139054f6c736edf58fa6fe5f8cc77814`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Ore hooks/catalog corrections, independent audit and tests. Needed for safe ore tracking; audit migration/edge semantics. |
| #121 — Q1-C player-attributed structure-loot evidence | #120 → `codex/xp-quest-q1c` (`17e980912d636b0572a3f25b24995ffde31c754c`) | Open Draft; no reviews; Fabric, visual, K5 restart/mapped/crafting and fixture-isolation workflows passed. | Structure loot, raid/vault/dragon/monument trackers/tests, catalog updates; also UI/harness, knowledge collectors, mob tests, mixins, locales/tags. Current Q1-C dependency; body records unfinished/owner-gated content. |
| #116 — independent review/repair of #110 | #110 → `codex/review-claude-pr110-20261017` (`8f33e72c8a71d29e132050457535ac8a0092a7b0`) | Open Draft; no submitted review records. Body documents owner-approved discount policy and local test evidence, not exact-head CI here. | Villager/Soil Study review repairs/tests plus audit docs. Independent feature. |
| #123 — selected #116 reconciliation onto Q1-B | #120 → `integration/q1b-villager-discount-pr116-20261019` (`4fbb036e0158098f4b9d6733c68ddd73b87db826`) | Open Draft; no reviews; body requests CI/restart checks and says local validation NOT RUN. | Four villager/Soil Study files; independent. |
| #124 — selected #116 reconciliation onto Q1-C | #121 → `integration/q1c-pr116-discount-20261019` (`5ea5f54614943c8fd00f047b7f8379f385d6e5e8`) | Open Draft; no reviews; exact-head Fabric and 3 K5 workflows passed. | Same four villager/Soil Study files; not needed for XP Quest. |
| #125 — Stage 23 PR #92/#93 reconciliation | #124 → `audit/stage23-pr92-pr93-reconciliation-20261019` (`079d36ac3bd2d8950118e30647d5f2efad19f947`) | Open Draft; no reviews; Fabric CI passed. | Stage 23 mapping doc and Fisheries Study regression test; independent. |
| #126 — four-card XP Quest selector | #125 → `design/xp-quest-selector-redesign-20261019` (`a9b89755f7c4e924243fa4229a90177433712be4`) | Open Draft; no reviews; Fabric, visual and K5 workflows passed. | Selector/canvas/layout/locales/texture/visual harness/tests. UI only; excluded from gameplay implementation. |
| #129 — 200-quest presentation structure | #126 → `design/xp-quest-structure-200-20261019` (`2176935929c990a210fae64582d524d30993bc8a`) | Open Draft; no reviews; Fabric and visual workflows passed. | Presentation mapping/docs/previews/scripts/stress tests; proposed edges are presentation-only. UI dependency. |
| #135 — D1–D5 usability | #129 → `design/xp-quest-usability-d1-d5-20261019` (`9a246f1e0cef5e3f39c4bf47b34df6e63877df30`) | Open Draft; no reviews; Fabric and visual workflows passed (K5 workflows also green). | Zoom/labels/sub-branches/icons/tooltips and regression tests. UI dependency. |

### Transitive ancestry to production baseline

Q1-C PR path is #121 → #120 → #119 → #118 → #112 → #110 → #109 → #108 → #105 → `integration/stage23-final-reconciliation-20261016`. PR #104 merged into a separate mob/accessory integration branch. The Stage 23 integration line is 76 commits / 231 files ahead of `main`; the Q1-C head is 139 commits / 376 files ahead. Both share `main` as merge base. No staged commit is on the production baseline solely because #104 was merged into an integration branch.

## Phase B — Q1-C delta classification

The main...Q1-C diff is 376 changed paths. A path-level scan groups them as 129 research/knowledge/soil/fisheries paths, 82 assets/locales, 53 shared/other, 52 quest-named, 27 documentation, 19 test/QA paths, 13 build/config/data paths and one villager/economy path. These buckets overlap by function and are a scope summary, not a dependency proof. The narrower #112 authority head to Q1-C head changes 97 paths (41 Java, 7 resources, 8 unit-test and 29 GameTest paths, with the rest docs/config/assets). The actual extraction manifest is below; branch ancestry alone is not used to mark dependencies.

| Subsystem | Evidence and classification against 75-quest foundation |
|---|---|
| Catalog and production registrations | src/main/resources/data/moreminecrafts/xp_quest/catalog.json; XpQuestCatalogData.java:17-27, 55-79, 81-122; XpQuestTracking.java:27-48. Required. Exactly 200 ledger entries, of which the catalog marks 75 registered. |
| Tracking, persistence, rewards and prerequisites | XpQuestTracking.java:27-40, 53-88, 119-124; ResearchProgressState.java:7-17, 24-49; QuestProgressService.java:21-47; QuestRules.java:6-23, 51-56; RawXp.java:22-57. Required tracking/state/claim path. Q1-C UI prerequisite edges are display metadata only: no gameplay prerequisite evaluator is evidenced in QuestRules. |
| Q1-C source attribution | QuestStructureEntry, QuestCombatKills, QuestDragonRematch, QuestRaidVictoryMixin, vault/loot hooks, QuestMonumentParticipation, and their C1-C6 tests in src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/; implementation report docs/design/xp-quest/Q1C_PROGRESS.md. Required only for the subset of the 75 whose trackers use these events. |
| Q1-B ore provenance | QuestOreProvenance and ore hook/tests from #119/#120. Required only for ore-provenance quests in the 75; source verification and the positive-only v2 migration policy must be retained. Old v1 negative evidence is not imported. |
| Q1-A exact XP and claims | RawXp, XpMath, QuestProgressService, serverbound claim receiver, and transaction tests from #118. Required to deliver quest rewards. Enhancement unlock/purchase transaction code in #118 is independent and should not enter a minimal quest extraction unless a direct compile/runtime dependency is proven. |
| Shared progression / Knowledge attachment | ResearchProgressState flat-map codec and KnowledgeRuntime attachment/network synchronization are the existing shared host for quest keys and state. Shared infrastructure with an actual dependency; preserve V4 save codec, copy-on-death and server-authored updates. Knowledge collectors and complete research tree are not thereby required. |
| Research and knowledge systems | Most broad research nodes, collector rules, knowledge UI and economy are independent gameplay/features. Keep only the shared state/attachment/network classes or narrowly extracted hooks that quest code actually calls. Q1-C references an existing Knowledge tracker for some catalog quests; verify per-quest evaluator/data requirements during extraction. |
| Villager discounts / economy | #110/#116 and selected four-file #123/#124 reconciliation; unrelated to quest gameplay. #124 is not needed for 75 registrations. Owner statements in #116/#123 are not GitHub review approvals. |
| Fisheries / Soil Study | #110/#123/#125 work and tests; unrelated. |
| Shared mob/content infrastructure | #104 expands mob/accessory behavior, resources and tests. main already has CustomMobCatalog and several entity definitions, so broad #104 is not proven prerequisite. If a registered quest depends on an entity that cannot naturally spawn on main, include only the exact missing spawn/content dependency after checking main; do not import the whole PR by ancestry. |
| Tests and QA harnesses | XP-specific unit/GameTests and K5 restart fixtures are needed as acceptance evidence. Generic Stage 23 and unrelated research/economy tests are independent. Preserve restart tests, fixtures and CI workflows that exercise selected quest persistence. |
| Documentation | Q1 authority, roster, migration and implementation docs are necessary review/evidence artifacts. Stage23 reconciliation and unrelated feature docs are not gameplay dependencies. Q1C progress doc is internally stale (see Phase E). |
| Build config/assets/locales | Include compile/resource IDs and localized strings directly referenced by the extracted 75 quests, their server packets, and UI eventually. Do not bring unrelated art, locale keys, or build changes by ancestry. |

### Minimal extraction dependency manifest (not implemented)

Start from verified main at 5f92307555ce5c486a2e0e8091f956bcbb6c6646, not the Stage23 stack. A future extraction branch should port and reconcile, in this order:

1. XpQuestCatalogData plus the 200-entry JSON and catalog invariant tests; verify the exact registered set remains 75 at the selected source head.
2. Quest definition/catalog/rules/evaluator and the 75 registrations; add only tracker evaluator cases/data actually referenced by those entries.
3. Quest state keys on the existing ResearchProgressState V4 codec and the minimum KnowledgeRuntime server save/sync/attachment integration; no blanket migration or replacement of that attachment.
4. Raw XP math and claim transaction/packet path needed to award the catalog rewards; exclude enhancement purchase code absent a direct dependency.
5. Q1-B source baselines and positive provenance hooks required by registered ore quests; include #120 correction and its negative/legacy tests.
6. Per-quest event hooks and source attribution for the 75 registrations: C1 loot, C2 vault/structure entry, C3 kills, C4 dragon, C5 raid, C6 monument, and other selected events. Retain applicable dedicated restart fixtures.
7. Only the exact resource IDs, localization entries, registration wiring, mixin declarations and build metadata required by items 1–6.
8. Tests: exact registered roster/catalog totals; positive, negative and player-attribution tests per selected tracker; claim-once/failure rollback; old V4 player-state decode; monument legacy decode if C6 retained; clean two-process restart for durable state; client/server packet boundaries.

Scope estimate: Q1-C's delta from #112 is 97 paths, including all Q1-C implementation plus unrelated code/assets/docs. A likely isolated deliverable is approximately 35–55 production Java/resource/config paths and 20–35 focused unit/GameTest/QA paths, with actual count undetermined until a non-destructive extraction diff against main removes duplicate facilities and unrelated code. The exact path list must be derived from the 75 catalog tracker definitions, not assumed from the entire PR diff. No extraction was performed in this audit.

## Phase C — gameplay foundation evidence

### Data and registration

Read-only parsing of the current Q1-C catalog.json found schema version 1; 200 entries; 200 unique IDs; 75 registered=true; all 75 registered entries have status NOW; aggregate status counts are NOW 163, VERIFY 17, FUTURE 20. The catalog's registered ID set is unit-asserted against the intended Q1-B/Q1-C roster in XpQuestCatalogDataTest (assertion lines 29–74). XpQuestCatalogData.validate enforces 200 entries, ID shape/uniqueness, status/tier, reward multiples of 50, positive targets, registered-only-NOW, evaluable registered trackers, and ledger totals (lines 81–122). Proven from current head source/data; the project test suite was not run in this audit.

### Runtime behavior and evidence strength

| Claim | Assessment | Evidence / limit |
|---|---|---|
| Catalog loading and validation | Proven in source; exact-head CI green | Lazy resource load and parse/validate fail fast (XpQuestCatalogData.java:55-79); schema and ledger invariants above. CI does not independently prove design approval. |
| 75 intended registrations | Proven in current JSON and test source; execution not rerun here | registered is authoritative gate in XpQuestTracking.initialize; register rejects non-NOW or non-evaluable kinds (XpQuestTracking.java:27-48). Unit test exact-set assertion. |
| Tracker registration/reconciliation | Partially proven | Hooks initialize before join; joins and each 100 ticks reconcile, each tracker failure is caught and fails closed for that quest (XpQuestTracking.java:27-40, 67-88). Coverage is per the Q1C progress document; not a live all-75 player session. |
| Progress identity and monotonicity | Proven in source/unit tests; CI green | IDs are unique and persisted in stable string keys; progress is monotonic and capped; completion does not revoke (QuestRules.java:13-23). Catalog IDs are namespaced stable IDs. No independent owner signoff of identity/version policy was found. |
| Reward granting/idempotence | Strongly tested in source report; live acceptance pending | Server thread, alive/connected checks; claimed flag before grant; exact XP snapshot and rollback on runtime failure; duplicate claim returns ALREADY_CLAIMED (QuestProgressService.java:21-47; RawXp.java:22-57). Current branch CI green. No production live test in this audit. |
| Prerequisites | No gameplay prerequisite behavior proven or required by the 75 registrations | #129 edge graph is expressly a presentation map; no prerequisite graph check in QuestRules. Do not treat UI display relationships as gating gameplay. |
| Persistence / clean restart | Automated evidence reported, not independently rerun | Quest state occupies existing flat string-to-int V4 player attachment. Q1C_PROGRESS reports clean two-process restart for C1-C6 event evidence and 18/18 K5 restart phases locally. CI K5 restart green at exact head. Forced JVM kill is NOT RUN. |
| Existing-world/player-save compatibility | Partially proven | Flat V4 codec retained and absent keys default to 0. Schema 0→1 only adds marker; no transform. Q1-C intentionally does not infer ore completion from old v1 negative provenance or proximity-based Knowledge structure references, so no false retroactive credit but some legacy evidence receives no credit. Monument SavedData codec retains legacy claimed-ID list. These adapters have tests per branch documents; no real-world old-save migration trial reported. |
| Client/server separation | Partially proven | Claim executes on server thread and uses serverbound request flow; tracking source hooks update server player state. Client visual CI is not proof of multiplayer exploit resistance or physical input. Dedicated live multiplayer attribution remains pending. |
| Existing world / natural attainability | Partially proven | Structure piece-level and event tests use real StructureStart/pieces, but report says natural generation and naturally occurring quest path have not been validated in production worlds. No evidence of full balance or natural attainability for every quest. |
| Live acceptance | Blocked / pending | Q1C_PROGRESS: actual multiplayer, live acceptance, full dragon crystal respawn sequence, live omen raid, several-player monument combat, JVM-kill durability and naturally generated structures pending. |

### Exact-head CI, approval and live acceptance are separate

The listed prerequisite PR exact-head Fabric/K5/visual checks were green when queried on 2026-10-20 (details in Phase A). For #121, current exact SHA 17e980912d636b0572a3f25b24995ffde31c754c had Fabric, client visual, K5 Production Restart, K5 Fixture Isolation Regression, K5 Mapped Sources Probe, and Stage 23-A Batch 3 Crafting Probe all completed successfully. The UI checks are noted only as CI state; UI is not part of this gameplay verdict. No submitted GitHub review records were present for the open PRs in the chain. CI establishes tested technical signals only; it does not establish formal approval, live acceptance or authorization to integrate gameplay.

## Phase D — integration strategy comparison

| Strategy | Risk | Complexity / reviewability | Reversibility | Assessment |
|---|---|---|---|---|
| A. Full Q1-C integration | High: imports 139 commits/376 paths from main base, including unapproved Stage23 gameplay, research/economy, mob, fisheries/Soil Study and UI ancestry. Carries shared-code conflicts, broad migration surface, stale docs and incomplete owner/live gates. | Easiest mechanically if merging the branch, hardest to review causality and blast radius. It requires explicit review/approval of each included gameplay feature and full acceptance plan. | Poorer: wide rollback could remove unrelated accepted features or leave coupled state. | Do not proceed now. Would require reconciliation to production, complete exact-head tests, all required owner approvals, review and live acceptance gates. |
| B. Minimal XP Quest gameplay extraction | Medium until isolated diff is proven: shared ResearchProgressState/KnowledgeRuntime, network sync, hooks/mixins and IDs can conflict with main. Extraction can accidentally omit tracker sources or portability safeguards. | Highest upfront mapping/port effort, but resulting review is bounded by the 75 registered quest ledger and can use per-feature test gates. Estimated 35–55 production and 20–35 test/QA paths; exact count awaits extraction dry-run. | Best: separate commits per catalog/core, B provenance, C tracker families; each can revert without unrelated features. | Preferred destination, conditionally feasible. First produce a non-production extraction branch and dependency diff, then review/validate before any gameplay integration. |
| C. Staged prerequisite integration | Medium-high if importing PRs whole: #105/#108–#110 are independent Stage23 features and #104 is a separate mob/accessory line. Low-medium if stages are re-cut to narrow quest dependencies and each has isolated acceptance gates. | Allows incremental review, but current PR stack is not a clean dependency DAG to main. #112 authority is stacked after #110, and #121 base includes the entire Stage23 chain. Must rebase/reconcile to verified main or create new narrow branches. | Good when each narrow unit has its own rollback boundary; poor if existing wide PRs are merged sequentially wholesale. | Use as execution method for B, not as reason to merge #104–#110 wholesale. Suggested order: decisions/authority → shared player state & core catalog/claims → B provenance → C event tracker batches, with a gate and rollback point after each. |

## Phase E — readiness, blockers and owner decisions

### Risk and blocker register

| ID | Sev | Evidence-based blocker/risk | Smallest resolving action |
|---|---|---|---|
| R1 | P0 | No production authorization/approval evidence: every relevant open gameplay PR is Draft and no submitted review record was returned; #104 merged only into an integration branch. | Owner/reviewers explicitly decide approved scope and record formal reviews on exact heads; never infer approval from branch prose or CI. |
| R2 | P1 | Full-chain scope is 139 commits/376 files from main and imports unrelated gameplay; #121 is based on #120, itself atop #119/#118/#112/#110/#109/#108/#105. | Do not merge full ancestry. Prepare a narrow extraction manifest/diff based on the registered quest list. |
| R3 | P1 | Live acceptance absent for multiplayer attribution, natural-world attainability/feel, dragon rematch, omen raid, multi-player monument fight and JVM kill recovery. | Run owner-approved acceptance checklist on a dedicated server and representative existing world; record exact build and results. |
| R4 | P1 | Existing-save behavior has limited compatibility evidence; v1 ore evidence and old proximity structure keys intentionally do not grant retroactive credit; forced termination is untested. | Owner decides retroactive-credit policy; run tests against representative copied saves and forced-stop/restart before release. |
| R5 | P1 | Q1C_PROGRESS and #121 body conflict with current Q1-C head: PR body claims nine C1 quests and unfinished C3-C6; progress doc says 72/160 NOW early, final checkpoint says 75/163 NOW, and later cave/ecology audit again says three ecology quests unregistered. Current catalog/test say 75/163 NOW. | Update authoritative progress/PR descriptions and reconcile contradictory registration record on a reviewed branch; preserve historical text as dated history if needed. |
| R6 | P1 | #118 documentation records unresolved vanilla XP scoreboard semantics for spending; RawXp says grants increase score, deductions leave score alone. This may be deliberate but is an owner-visible gameplay contract. | Owner explicitly accepts exact score semantics for grants, spending, creative/free behavior and anvil-derived balance; add/retain tests for that contract. |
| R7 | P2 | main has no XP Quest catalog/tracker; integration requires careful overlap handling with existing Knowledge attachment/runtime, hooks and networking. | In extraction dry-run, map every changed class against main and prove one owner for each mixin/event/state key. |
| R8 | P2 | PR #121's current tests and Q1C progress report represent different checkpoints; current PR CI is green, but docs cite 344/294 while an earlier block cites 344/292 and previous historical 344/287 etc. | Refresh a single exact-head validation report and associate it with SHA; label older counts historical. |
| R9 | P2 | Exact-head green checks are technical evidence, not approvals; live acceptance and formal review are absent. | Require both exact-head CI and explicit review plus live acceptance before any production integration. |
| R10 | P3 | #124 adds four villager/Soil Study files; it is not needed by 75 quests and is an unrelated duplicate reconciliation path. | Exclude #123/#124 from XP Quest extraction unless a direct dependency is demonstrated. |

No additional unresolved gameplay defect is asserted here. Q1C_PROGRESS reports repaired P1 issues: incorrect monument registry ID (e3e61ff), proximity semantics in Knowledge structure references replaced by piece-level entry tracking, and nullable projectile weapon item handling (e3cebb9). It reports K5 harness/restart fixture repairs (61a13b0, 687cf88, f624bf4). This audit did not independently reproduce those historical failures/fixes; current exact-head CI was green.

### Decisions required from feature owner/reviewers

1. Approve the exact 75-quest roster and identity/reward ledger as a production scope, or identify changes; separate status approvals from the 200-entry future ledger.
2. Accept the XP score semantics in #118 and whether spending should preserve vanilla scoreboard semantics.
3. Decide retroactive credit for historical Knowledge structure proximity and v1 ore evidence; current implementation fails closed and does not grant it.
4. Approve the three ecology promotions represented in the current catalog, resolving stale contradictory docs; decide whether the two monument quests stay VERIFY/unregistered.
5. Accept the live validation matrix: natural generation, dragon rematch, omen raid, multi-player attribution/monument play, representative old-world load, client/server behavior and forced termination recovery.
6. Decide whether an isolated extraction is the accepted integration vehicle and explicitly exclude unrelated #104–#110, #116/#123/#124/#125 and UI PRs unless a specific dependency is shown.
7. Confirm rollback boundaries and any release/data-version policy for existing saves.

### Recommended integration sequence

1. Correct stale docs and record the owner's decisions on a reviewed, non-production branch.
2. Build and review an extraction diff directly against the verified main SHA; no cherry-picking assumptions from branch ancestry.
3. Integrate core state/catalog/rules/claim path in a narrow review unit; require catalog exact-set tests, claim rollback/idempotence tests, save decode tests and client/server checks.
4. Integrate Q1-B provenance and Q1-C event families in separate units, retaining positive/negative attribution and durable restart tests for each family.
5. Run exact-head CI and acceptance on representative existing worlds/dedicated server; record live tests separately from synthetic fixtures.
6. Require formal approvals and owner acceptance for each gameplay unit; then accepted UI work (#126/#129/#135) can be reviewed against the stable gameplay contract in its own later task. No merge is authorized by this audit.

## Phase F — reproduction, final verdict and continuation

### Reproduction instructions

These commands reproduce the read-only ref/scope checks from a clone with authenticated GitHub access. This audit did not run project tests.

```sh
git fetch origin main codex/xp-quest-q1c integration/stage23-final-reconciliation-20261016
git rev-parse origin/main origin/codex/xp-quest-q1c origin/integration/stage23-final-reconciliation-20261016
git merge-base origin/main origin/codex/xp-quest-q1c
git rev-list --count origin/main..origin/codex/xp-quest-q1c
git diff --stat origin/main...origin/codex/xp-quest-q1c
git diff --name-only origin/main...origin/codex/xp-quest-q1c
git show origin/codex/xp-quest-q1c:src/main/resources/data/moreminecrafts/xp_quest/catalog.json
```

Then inspect source files in Phases B–C and linked exact-head GitHub workflow runs/review records. Use GitHub PR head.sha, not branch labels, when comparing CI or review state. Do not infer owner approval from checks.

**Q1-C full integration: NO-GO.** The scope is broad and unapproved, upstream gameplay PRs remain Draft/unreviewed, unrelated changes dominate the ancestry, docs conflict at current head, and live acceptance is pending.

**Minimal XP Quest gameplay extraction: CONDITIONALLY FEASIBLE.** Current source and test structure provide an identifiable catalog/tracking/reward foundation, and main is a verified baseline. Feasibility is conditional on a dry-run extraction proving shared runtime dependencies, narrowing the manifest without losing any selected tracker hooks, resolving owner decisions, and passing validation against existing saves and live multiplayer scenarios. Extraction was not implemented here.

Recommended path: B as the bounded target, delivered through narrow staged units (C method). Do not integrate full Q1-C (A) as currently stacked.

### Continuation checkpoint

Audit analysis and doc are complete; no production gameplay/UI source was changed, no project tests were run, and no prerequisite PR was approved or merged. Audit PR: https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/139 (Draft, base main). The exact remote head is reported by GitHub PR metadata. GitHub contents API synchronization is confirmed; direct shell push was blocked by proxy connectivity. If continuing in a new session, read this document and AGENTS.md, fetch and verify current refs, then resume only from an explicitly approved next task.

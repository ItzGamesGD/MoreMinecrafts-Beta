# Q1-C Production Integration Readiness Audit — 2026-10-20

**Status:** Audit in progress; no gameplay or UI implementation authorized.  
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

Next: push this checkpoint; inspect Q1-C authority, source, data and tests; classify its diff by subsystem; verify the 75 registrations and tracker execution/persistence evidence; compare strategies A/B/C; finish readiness and risk findings; create a Draft audit PR without changing existing PRs.

## Phase A — verified dependency inventory

PR refs and workflow state below were refreshed from GitHub on 2026-10-20. Unless called out, PRs are open Draft and have no submitted review records. `mergeable=true` is not approval. CI refers only to workflow runs returned for the exact head SHA.

| PR / purpose | Base → head (exact head SHA) | State, reviews, exact-head CI | Changed-file scope and XP Quest relevance |
|---|---|---|---|
| #104 — mob resources/accessory consolidation | `integration/stage23-final-reconciliation-20261016` → `integration/stage23-mobs-accessories-20261016` (`b0de6b754859e090427ff310887ca04188310a8a`) | Closed, merged 2026-10-16 into integration branch; review list empty; Fabric and 3 K5 workflows passed. | Build config, mob catalog/renderers/assets, accessory design/tests. Independent; not needed for XP Quest. |
| #105 — portable-furnace persistence live fix | integration line → `codex/stage23-livefix-b-20261016` (`5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`) | Open Draft; no reviews; 5 workflows passed. | Furnace persistence/content authority/tests, plus client validation harness. Independent gameplay; not needed. |
| #108 — Stage 23 expanded batch 2 and portable-furnace research | #105 → `codex/stage23a-expanded-batch2-20261017` (`2c8e984314bb079378cffc95c4f48982a962173b`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Research rules/runtime/mixins, furnace/mob support and tests. Independent; not needed. |
| #109 — Stage 23-A remaining research campaign | #108 → `codex/stage23a-remaining-research-20261017` (`9376777264136b37e10071adf8ed59c08da9b6ee`) | Open Draft; no reviews; Fabric and K5 workflows passed. | Research movement/riding/effects, mob capture, tests/manifest. Independent; not needed. |
| #110 — villager discount and Soil Study | #109 → `claude/stage23a-economy-soil-20261017` (`9ec91dc53512796748a6cfc0980cda0b65e764e6`) | Open Draft; no reviews; no PR-triggered workflow run found at exact head. | Villager pricing, Soil Study runtime/mixins, locales/tests. Independent; not needed. #116 later documents historical P1 fixes, stock-display gap and manual QA. |
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

## Remaining audit sections

- Phase B: classify Q1-C delta by subsystem, with source/file/commit evidence.
- Phase C: verify catalog/75 registrations and review runtime, tests, live acceptance, persistence, migration, idempotence and server/client boundaries.
- Phase D: compare full integration, minimal extraction and staged integration.
- Phase E: risk register and owner decisions.
- Phase F: final recommendation, reproduction steps, completed checkpoint and Draft audit PR.

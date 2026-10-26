# XP Quest UI integration checkpoint — 2026-10-20

Repository: `ItzGamesGD/MoreMinecrafts-Beta`
Task branch: `codex/xp-quest-ui-integration-20261020`
Starting commit: `5f92307555ce5c486a2e0e8091f956bcbb6c6646` (`main` at fetch)
Remote task branch: created from `main` at the same starting commit.

## Phase A audit

Authenticated GitHub repository access succeeded. The authenticated account is `ItzGamesGD`; repository metadata grants `push: true`. The dedicated remote task branch was created successfully. Git fetch succeeded for `main`, the task branch, and all five handoff refs. Local worktree was clean before this checkpoint.

Current PR state and exact refs:

| PR | Base | Head | State | Approval / CI evidence |
|---|---|---|---|---|
| #124 | `codex/xp-quest-q1c` (`17e980912d636b0572a3f25b24995ffde31c754c`) | `integration/q1c-pr116-discount-20261019` (`5ea5f54614943c8fd00f047b7f8379f385d6e5e8`) | Open, Draft, unmerged, mergeable | No submitted reviews; no workflow runs returned for head |
| #125 | `integration/q1c-pr116-discount-20261019` | `audit/stage23-pr92-pr93-reconciliation-20261019` (`079d36ac3bd2d8950118e30647d5f2efad19f947`) | Open, Draft, unmerged, mergeable | No submitted reviews; Fabric CI run 39185666213 succeeded |
| #126 | `audit/stage23-pr92-pr93-reconciliation-20261019` | `design/xp-quest-selector-redesign-20261019` (`a9b89755f7c4e924243fa4229a90177433712be4`) | Open, Draft, unmerged, mergeable | No submitted reviews; Fabric CI 39202819310 and visual validation 39202819308 succeeded |
| #129 | `design/xp-quest-selector-redesign-20261019` | `design/xp-quest-structure-200-20261019` (`2176935929c990a210fae64582d524d30993bc8a`) | Open, Draft, unmerged, mergeable | No submitted reviews; Fabric CI 39222428205 and visual validation 39222428500 succeeded |
| #135 | `design/xp-quest-structure-200-20261019` | `design/xp-quest-usability-d1-d5-20261019` (`9a246f1e0cef5e3f39c4bf47b34df6e63877df30`) | Open, Draft, unmerged, mergeable | No submitted reviews; Fabric CI and visual validation 39407176598 and 39407176860 succeeded |

PR descriptions confirm the dependency order #135 → #129 → #126 → #125 → #124 → `codex/xp-quest-q1c`. PRs #124–#135 are Draft; their mergeability is not approval. The PR descriptions also state that #126 awaits owner visual acceptance and #129 awaits owner review of organization. The task handoff independently authorizes proceeding with accepted D1–D5 UI work subject to safety gates.

The #124 base `codex/xp-quest-q1c` is not an approved stable integration target. Its head is 139 commits ahead of current `main` and its diff spans 376 files (24,195 insertions, 1,343 deletions). `main` is its merge base and is not ahead. That chain includes substantial XP quest/gameplay and unrelated research work. PR #124 itself adds four villager discount/restart-test files from the Q1-C integration line. PR #125 adds a Stage 23 reconciliation document and Fisheries Study test. Those are outside this task and must not be imported.

PR #126 changes the four-card selector and supporting quest canvas/layout, localization, texture, workflow and tests (13 paths). PR #129 adds the 200-entry presentation mapping, preview tooling/assets, chapter/sub-branch proposal, and tests (21 paths). PR #135 adds D1–D5 semantic zoom, labels, sub-branch layout, icon catalog/generator, tooltip behavior, visual harness updates and tests (18 paths). Historical CI is green at each of those exact heads, including #135's client visual validation, but it is not evidence for a newly isolated commit.

## Current decision and next action

Do not use bottom-up stacked merging: the ancestors are unapproved Drafts and include unrelated Q1-C, villager and Stage 23 changes. Evaluate isolated integration on `main`, as Strategy B. Before changing source, inspect current `main` quest/UI APIs, determine whether the required #126/#129/#135 presentation changes can be isolated without importing gameplay changes, and record the result here. If safe, implement only the minimum adapted UI delta; then run required validation and open a Draft integration PR. If not safe, document the precise incompatibility and stop.

### Strategy B result: blocked

The compatibility check on target `main` (`5f92307555ce5c486a2e0e8091f956bcbb6c6646`) found that it has no `src/main/resources/data/moreminecrafts/xp_quest/catalog.json`, no `XpQuestCatalogData`/`XpQuestTracking` production loader, and no production `QuestCatalog.register` call. Thus the target does not contain the 75 registered quests that the task requires preserving. The catalog on `codex/xp-quest-q1c` has 200 entries and 75 registered NOW quests, but this only exists on the unmerged Q1-C line.

The dependency chain beyond PR #124 is also unapproved: #121 (Q1-C) is open Draft and based on #120; #120 is open Draft on #119; #119 is open Draft on #118; #118 is open Draft on #112; #112 is open Draft on #110; #110 is open Draft on #109; #109 is open Draft on #108; #108 is open Draft on #105. PR #104 is merged into the `integration/stage23-final-reconciliation` branch, not into `main`. The Q1-C branch's merge base is exactly `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; it is 139 commits ahead (376 files changed). This confirms the dependency chain does not reach an approved production baseline.

Importing Q1-C catalog/tracking/gameplay to satisfy the 75-quest invariant would violate the explicit prohibition on unapproved gameplay/prerequisite integration. Integrating #126/#129/#135 UI alone onto `main` would not preserve or present those 75 quests. Therefore neither bottom-up integration nor safe isolation onto the available stable target satisfies the task gates. No source changes were made and no integration PR was opened. The correct next action is to wait until the XP Quest gameplay prerequisite chain is approved/integrated into an authorized stable target, or for the owner to authorize a different target and scope.

## Progress

- [x] Verify repository/authenticated write access and create dedicated task branch.
- [x] Fetch current `main` and all five dependency refs; record exact starting and PR heads.
- [x] Inspect repository instructions, PR descriptions, changed-file lists, review submissions, and exact-head workflow results.
- [x] Commit and push this Phase A audit checkpoint before implementation work.
- [x] Audit `main` compatibility and determine whether safe isolation is feasible; neither strategy passes the safety gate.
- [ ] Resume after the gameplay dependency chain is approved/integrated, or after the owner authorizes a different stable target/scope.
- [ ] Then implement, validate, commit and push the isolated UI changes; create a Draft integration PR and confirm its remote head matches the verified commit.

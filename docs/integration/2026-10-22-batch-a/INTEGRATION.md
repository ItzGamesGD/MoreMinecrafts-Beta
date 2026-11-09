# Batch A implementation recovery — Draft review candidate

This branch preserves existing implementations; it is not a redesign or release approval. Main and all source PRs/branches are untouched. The custom XP canvas is retained for owner review under the current task, **without amending the approved vanilla `AdvancementsScreen` contract**. Complete Batch A acceptance remains blocked by that conflict, later content decisions, and pending live checks.

## Verified sources and ancestry

Primary map: [Draft audit PR #154](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/154), exact `84550ad4ef0a09248934641e26dab61c4f2cd13b`, all five reports read. All 65 inventory branch heads and merge bases were independently checked against fetched Git objects; no feature was inferred from a PR title. [SOURCE_MANIFEST.json](SOURCE_MANIFEST.json) records exact source commits/blobs and all 377 cumulative paths, 36 UI donor paths, and 8 content-review records. Its classifications distinguish runtime/resources, validation, build, and authority/history. It is the exhaustive reused-file list, including dependencies already in the cumulative baseline.

| Source | Exact SHA | Integration treatment |
|---|---|---|
| main / Knowledge #87 | `5f92307555ce5c486a2e0e8091f956bcbb6c6646` | Unmodified ancestor; Knowledge already present |
| cumulative #125 | `079d36ac3bd2d8950118e30647d5f2efad19f947` | New isolated worktree/branch starts here; 145 commits / 377 paths beyond main retained |
| selector #126 | `a9b89755f7c4e924243fa4229a90177433712be4` | Verified ancestor of #135; its assets/input retained from final donor blobs |
| structure #129 | `2176935929c990a210fae64582d524d30993bc8a` | Verified #126 descendant / #135 ancestor; preview remains test-only |
| UI #135 | `9a246f1e0cef5e3f39c4bf47b34df6e63877df30` | Exact bounded #125→#135 file delta recovered; no PR merge |
| Q1-A #114 | `78d1495402de6f3d7328fcd3117a4996137a05e2` | Catalog/evaluator/evidence source already incorporated through #118 |
| Q1-A2 #115 | `f30d263c93b89b1bab844893bdb82084bfe209bd` | Exact-XP/claim rollback already incorporated through #118 |
| Q1 integration #118 | `c40ee6b1656800f016644893f5bac7a9c90827b2` | Actual historical merge ancestry independently verified |
| Q1-B #119 | `c7fe7fabc5a3bc486e2be0aed8f793628f42ec72` | Server event hooks retained |
| Q1-B correction #120 | `fd4aa831139054f6c736edf58fa6fe5f8cc77814` | Positive ore provenance retained; unknown/placed ores remain ineligible |
| Q1-C #121 | `17e980912d636b0572a3f25b24995ffde31c754c` | Structure/combat/raid/monument tracking and historical 75 registrations retained |
| latest content #140 | `3e66cfbdfa6a12576ecf0162108250691318c8db` | Eight previously absent review records imported byte-for-byte; no runtime objectives invented |

Original source identification before repairs:

- `55b4510073a6b0ae5d823460388514d0dd79aad5`: playable Knowledge/Research screens; `0647b1815dc76e8701df811650ed007419998e98`: vanilla-aligned seven-sector navigation. Knowledge is not rewritten.
- `855ddfb23c745a86086a76d835eebd298dd73a36`: shared quest claim service; `f1ad683eb0d9861a5fc3067cc2a7431d4be12253`: Claude exact-XP transaction and rollback.
- `b8f86fe20019623b3ff6e201a12daf29c704bd4b`: Q1-C XP activation and custom Ecology canvas; `b8d3b9005f47dc82b22513dc5d4b7ec0babacc7b` and the verified #126/#135 descendants supply the retained texture, keyboard, tooltip, layout and zoom improvements.
- #125 already contains selected #92/#93 effects, #97 exact-fifths arithmetic, #99 model/viewport repair, #104 mob assets, #105 furnace persistence, #116/#124 pricing repair, and #125 codec test. These are retained once; older sibling runtime hooks are not replayed.
- The downloaded `codex/xp-quest-ui-integration-20261020` at `a124ccfc2e7e3bc826c04705fa98f69a102a2636` changes only a checkpoint document. It is not a runtime donor.
- #130 skull, accessories, bounty/contracts, arena/structures and territory are excluded as new integration work. Territory stays shelved. Existing cumulative dependency files are preserved without unrelated balancing.

## Reused implementation files and minimal repairs

The exhaustive list and exact original blobs are in SOURCE_MANIFEST.json. Key reused files are `ProgressionSelectionScreen`, `ProgressionDisciplineSelectionScreen`, `ProgressionBrowseScreen`, `ProgressionTreeCanvas`, `QuestTreeStructure/Layout`, `QuestLevelOfDetail`, `QuestTooltipPlacement`, `ProgressionClientApi`, `ProgressionScreenData`, `QuestCatalog`, `QuestProgressService`, `RawXp`, `XpQuestCatalogData/Evaluator/Tracking/Hooks`, all Q1 event helpers/mixins, `KnowledgeRuntime`, `ResearchPurchaseService`, progression synchronization/payloads, selector textures, localization, and existing unit/GameTest/restart fixtures.

| Defect | Cause established before repair | Minimal action |
|---|---|---|
| XP selector disabled / cannot open on downloaded branch | Missing integration and disabled main placeholder; working source exists in Q1-C/#126/#135 | Recover source activation, dispatch, card texture, hover and keyboard code |
| Knowledge inaccessible report | No disabled/missing source route found; screen is directly constructed, no screen-registry binding needed | Preserve route and data; verify actual clicks; do not guess a replacement implementation |
| Only three of 75 registrations reachable in tree | Hard-coded Ecology presentation filter, rather than missing server tracking | Reuse existing chapter/sub-branch/icon assignments for historical registered IDs only; no edges/gameplay candidates enabled |
| No completed-node claim | Production mouse/keyboard path never called existing `ProgressionClientApi.claim` | Bind click release / focused confirmation to existing payload; exclude synthetic preview; preserve drag rejection |
| No three-state claim presentation or completion notification | Genuinely missing client behavior | Add unclaimed/claimed indicators, pending/claim tooltip, and vanilla `SystemToast` on ordered completion delta only |
| Pending/repeated request lifecycle | Existing raw claim sender had no client suppression | Bounded five-second gate; clear on result, claimed sync, reopen and disconnect; server remains ultimate claim-once authority |
| Missing normal-play copy outside three nodes | Existing Korean catalog copy was never connected to runtime keys | Recover exact existing Korean titles/objectives. English falls back to recovered Korean for these entries; translation remains pending |

The original three-node tooltip regression remains an explicitly gated fixture using real server rows. Normal selector navigation first verifies all 75 nodes, then the legacy harness opens that fixture for its existing tooltip/edge checks. The 200-node synthetic preview cannot claim. `BatchAClientProbe` is opt-in observation only for external mouse/keyboard tests: it does not change screens, dispatch input, or grant progress/XP.

## Catalog, authority conflicts and owner decisions

[QUEST_RETENTION.json](QUEST_RETENTION.json) lists every one of the 200 stable slots, existing flags, tracker, reward, target, and identified release blocker. **75 historical registrations / 73,850 raw XP remain; 125 stay unregistered.** The catalog JSON, registered flags, server predicates, claim service, save keys, reward ledger and Research balancing are unchanged. No extra quest is activated by presentation. Existing progress/claims are not erased or reinterpreted.

This is a recovery review candidate, not approval to release all 75 historical objectives. Fourteen registered rows have explicit later-content/fishing holds: cave roster, habitat biome kills, shield target, ravager target, till→harvest replacement, four weapon slots, repeated-Warden slot, tool-wear slot, and three frozen fishing rows. Additional unregistered replacements/predicates remain in the imported [owner decisions](../../design/xp-quest/audit-200/OWNER_DECISIONS_2026-10-21.md). Section N supersedes deletion without replacement; all 200 slots and provisional budgets remain. No habitat tags, harvest quotas, replacement goals, migrations or new recipes are invented here. Owner must settle release/migration treatment before these affected historical objectives can be accepted.

The custom canvas conflicts with the Oct17 vanilla-screen contract in [approved UI authority](../../UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md). This task explicitly requires preservation for review, not an authority amendment. Owner must approve the presentation direction durably before final acceptance. Knowledge remains free/non-purchase; Research retains the seven-sector route, one-click server-authoritative purchase, materials, raw XP, prerequisites, migration and effects.

Research's preexisting partial/data-only effects remain partial/data-only; navigation recovery does not implement all research effects. Large Backpack rendering has historical negative evidence and is not repaired here. Skull crash-safety work remains excluded. No new security exemptions or history rewriting are introduced.

## Validation ledger

Validated code commit: `e173f1c7db50bcf031fab6c459ed2c0fc14f7740`; subsequent report/capture changes do not alter executable sources. Linux, Temurin Java25.0.5+8, Gradle9.6.0, Minecraft26.3, Fabric Loader0.19.5 / API0.161.0+26.3, Xvfb1280×720, Mesa25.0.7 llvmpipe Vulkan1.4.305. The runtime used the development classpath; clean distributable JAR SHA256: `0df4f200c82c2a01ef8550d71c42862f025be910b002e35992772309554dd032`.

| Check | Result | Actual evidence / limits |
|---|---|---|
| Clean `./gradlew clean test build compileGametestJava runGameTest` | PASS | Final fresh-world run: BUILD SUCCESSFUL, 3m23s |
| JUnit | PASS | 391 tests, zero failures/errors; includes new pending/timeout/reset/completion-notification and 75-node registration isolation tests |
| GameTests | PASS | All 297 required tests; existing exact-XP, duplicate claim, injected grant rollback, saved-player records, quest attribution, Knowledge and Research regressions retained |
| Dedicated server smoke | PASS | Registry/common initialization and `Done (11.021s)!`; production datapacks loaded; actual client joined |
| Client smoke/resources | PASS | Vulkan backend, item atlas, world load, client/common init; audio unavailable and online-services lookup warnings on offline fixture |
| Real-client Knowledge/Research navigation | PASS — Screen callbacks | Actual constructed/rendered screens; all seven sectors in each, populated tree, and Back. Physical OS input is NOT RUN |
| Real-client XP tree / completion / claim / duplicate packet | PASS — Screen callbacks + server events | 75 actual registered nodes; 100 spawned zombies killed by player-attributed server damage; hostile_100 completed; node click paid exactly250 XP; raw balance10→260; raw duplicate packet returned ALREADY_CLAIMED, no second payout |
| Completion toast and claim indicators | PASS — native capture | Completed/unclaimed and green claimed indicator; vanilla SystemToast observed. Source/JUnit ensure ordered deltas only, no snapshot replay |
| Claim/save/reload/reconnect | PASS — clean restart | Save-all flush, stop dedicated JVM and client, restart both, same player reconnects: hostile_100 remains complete/claimed at100, XP260, revision42. Pre/post-claim record GameTests also pass; physical death/dimension/unclaimed live-client scenarios remain pending |
| Physical mouse/keyboard / GUI scales1–4 | NOT RUN | SDL reports x11, but Xvfb exposes only an unmapped1×1 helper window; window focus/input cannot reach Minecraft. No Screen callback is mislabeled physical |
| Windows `.\gradlew runClient`, owner visual/feel acceptance, natural survival pacing, two real clients | NOT RUN / LIVE VALIDATION PENDING | Linux fixture and mock-player server tests do not establish these |
| Full-history secret audit | FAIL — existing historical findings | Pinned Gitleaks8.30.1, checksum verified, all branch/tag/PR refs fetched, `--all --full-history --redact=100`; two findings outside cumulative baseline ancestry. Values/reports withheld; no exemption, workflow change or suppression |
| New Draft PR automation | PENDING | Inspect exact final HEAD checks on the PR; local passes do not imply remote CI success |

The 10 XP before claiming is the unchanged `activities/first_hostile.json` reward for First Zombie; it is not quest XP. The fixture wrote no quest completion/progress/save keys. The recorded quest reward was250 only at claim. [Native captures](captures/) include selector, Knowledge, Research, full tree viewport, completed/unclaimed and claimed states. Captures include startup/vanilla notifications; they are evidence, not art acceptance.

A non-clean repeat GameTest run against the already-used test world failed K5/monument fixture-state assumptions. The final clean run restored a fresh test world and passed all297 without production changes or disabled fixtures. Earlier overlapping compilation/runtime exploration was not used as final evidence.

Presentation grouping from #129 remains a proposal retained for review; no candidate prerequisite, unavailable quest or unverified registration is enabled. **This branch is ready for bounded owner recovery/visual review, but is NOT ready for complete Batch A visual acceptance, release, Ready or merge.** Resolve the custom-screen contract, affected objectives/migrations, full-history security gate, physical Windows checks and final owner verdict first.

[EVIDENCE.json](EVIDENCE.json) records the exact local metrics and restart observation.

## Windows live acceptance

Use a disposable/copied world; do not downgrade a valuable migrated save. Check out this PR's exact HEAD and use Java25:

```powershell
.\gradlew --stop
.\gradlew clean test build compileGametestJava runGameTest
.\gradlew runClient
```

- [ ] Record HEAD, Java version, loaded mod/JAR SHA256, GUI scale, language, OS/GPU and save identity. Remove stale duplicate mod JARs from the test profile.
- [ ] Physical advancement key → four cards. Test centers/edges, scale1–4, resize, hover, Tab/Shift+Tab, arrows, Enter/Space and Close.
- [ ] Mouse Knowledge → each of seven sectors → actual populated tree → Back → sector picker → Back → selector. Knowledge never spends items/XP.
- [ ] Mouse Research → each sector → correct tree. Acquire legitimate prerequisites/materials/XP, purchase once; check exact debit, max-level/insufficient rejection, repeat clicks, Back and Escape.
- [ ] XP → chapter/index/tree → an unaffected legitimately completed quest (e.g. hostile_100 or structure-loot). Completion displays unclaimed/toast and grants zero quest XP. Click its node: exact reward once; claimed indicator and updated XP. Repeated click, double click, Enter and stale/duplicate requests cannot repay. Drag does not claim.
- [ ] Hold a completed reward unclaimed; die/respawn, change dimension, reconnect and restart. Unclaimed stays available; claimed stays claimed and never repays. Verify level/progress/totalExperience consistency and two-player isolation.
- [ ] Check Korean/English copy, long/scrolling tooltips, chapter counts, zoom, frame hit bounds and resource reload. English fallback, custom-screen authority and affected historical objectives are explicitly review limitations.
- [ ] Record owner screenshots/verdict. No merge or Ready action is authorized.

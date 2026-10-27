# Repository functional provenance — Wave 12 audit (2026-10-27)

Status: audit record. It assigns no design authority; current authority is routed by `docs/CONTENT_AUTHORITY_INDEX.md`.

## Baseline

- Observed: 2026-10-27 00:30–00:40 UTC, after `git fetch --unshallow` of every remote head and every `refs/pull/*/head` (the session clone was shallow; shallow ancestry results were discarded).
- Default branch `main`: `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3` (PR #155 merge, 2026-10-23). Wave 11 is 79 commits ahead of `main` and 0 behind.
- Reference checkpoint `checkpoint/wave11-unified-20261027`: `c47c69f3e3553819432905197599443288669bb5` (head of open PR #192; all 15 PR checks green, run 40414013333 et al.).
- Wave 11 contains `integration/wave10-unified-20261025` (`13c190d1`, PR #182 head) as an ancestor. `checkpoint/wave10-unified-20261026` (`73dcc8f4`) is **not** an ancestor: its three post-PR #191 commits were cherry-picked into Wave 11 (patch-id equivalent: `17a7cdd`→`d44a83c`, `e28ce2f`→`7ff1d14`, `73dcc8f`→`45fd25f`).
- `release/wave9-rc-20261025` (`268ec65b`, PR #181) is not an ancestor; every file it changed is byte-identical in Wave 11 or differs only in lines Wave 11 later revised (0 branch-only lines).
- Remote branches audited: **139**. Pull requests audited: **191** (numbers 1–192; #50 is not a pull request). States: merged 66, closed 110, open 15.
- PR head reachability: 106 heads are ancestors of Wave 11; 70 are reachable only from another remote branch; **15 are reachable only through `refs/pull/N/head`** because their branches were deleted. Those 15 were audited separately (section below). GitHub keeps `refs/pull/N/head` after branch deletion, so PR-backed history survives branch cleanup.
- Exact SHAs for every branch and PR: [`wave12-20261027/BASELINE_MANIFEST.json`](wave12-20261027/BASELINE_MANIFEST.json).

**Concurrent change check.** The 2026-10-26 Wave 11 inventory listed 144 branches besides Wave 11. Seven of them no longer exist: `audit/prc-p4-research-preservation-20261026` (an ancestor of Wave 11) and six branches whose heads survive as PR refs (#139, #154, #157, #158, #159, #163; audited below). One branch is new since then (`docs/sky-islands-design-context-20261027`). No audited branch head moved during this audit; this was re-verified by re-fetching before publishing.

## Method

1. **Ancestry**: `git rev-list --count W11..B` / `B..W11`, merge-base per branch.
2. **Patch equivalence**: `git cherry W11 B` (patch-id) — identifies rebased/cherry-picked commits.
3. **Per-file state** for every path changed in `merge-base..B`: IDENTICAL blob, PRESERVED_ELSEWHERE (same blob at another Wave 11 path), DIVERGENT, MISSING, DELETED_ON_BRANCH.
4. **Branch-only lines**: for DIVERGENT/MISSING files, the added lines of `merge-base..B` that appear nowhere in the Wave 11 file. Used only to direct inspection; refactors produce false positives.
5. **Semantic inventory** of every ref (Java classes, `@Test`/`@GameTest` methods, mixin registrations, entrypoints, lang keys, data/asset resources, research IDs referenced by runtime code, XP Quest catalog registrations, workflows) and set difference against Wave 11. 27 classes, 94 tests, 7 mixins, 14 data resources, 8 assets, 10 quest registrations, 4 entrypoints and 4 workflows exist on some branch but not in Wave 11; each was traced to a decision below. Zero lang keys and zero research IDs were missing. The same inventory and per-file analysis were run on the 15 PR heads reachable only through `refs/pull/N/head`.
6. **Source and authority inspection** of every remaining candidate (git history, PR body, authority index routes).

Tools and raw outputs are reproducible from the commands above; the classification table is generated from them plus the manual evidence column.

## Classification summary

| Provenance class | Branches |
|---|---:|
| INTEGRATED (ancestor of Wave 11) | 56 |
| SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | 45 |
| PRESERVED AS DOCUMENTATION | 13 |
| INTENTIONALLY ABANDONED / REJECTED | 10 |
| INTEGRATED — EQUIVALENT COMMITS | 8 |
| VALIDATION TOOLING — NOT PRODUCT FUNCTIONALITY | 3 |
| UNRESOLVED — BLOCKED (owner decision / design) | 2 |
| RECOVERED IN WAVE 12 | 2 |

No branch remains classified *Missing from the current integration baseline* or *Uncertain*: the two branches with missing approved, independently resolvable content were recovered in Wave 12, and the two blocked branches carry an explicit owner-decision blocker.

## PR dependency structure (stacks)

- **Foundation (Stages 1–20)**: #1–#69 merged through stage branches into `main`/stage checkpoints; all ancestors of Wave 11. #39–#49, #52, #58–#60, #65 closed; their fixes reached the recovered cumulative line (#56/#64/#66/#68/#69).
- **Knowledge/Research V4**: #74→#75→#76→#78→#79→#80→#81/#82→#84→#86 (closed drafts) consolidated by **#87** (merged to `main`).
- **Stage 22/23-A research**: #88 ← #89 ← {#90 ← #91 ← {#93, #94 ← {#95 ← #96 ← #98, #97, #99}}, #92}; #100 (linear chain) ← {#101, #102, #103 ← {#104 (merged), #105 ← {#106, #107, #108 ← #109 ← #110 ← {#112 ← {#114, #115, #118 ← #119 ← #120 ← #121 ← {#123, #124 ← #125 ← {#126 ← #129 ← #135, #127 ← #128 ← #130}}}, #113 ← #122, #116}}}, #117}. Tip consolidated by **#156 Batch A** (from #125 + #135), then **#165 Wave 5R** (composition `a13d9777`).
- **Territory design**: #131 ← #132 ← #133 ← #134 ← #136 ← #137 ← #138 (shelved).
- **Accessories**: #142 ← #143 ← #144 ← #145 ← #146 ← #149 ← {#152, #153, #160, #161}; #161 ← {#171, #173, #174 ← #175}.
- **Bounty**: #150 (audit), #151 → #162 (merged into accessory baseline) ← #177 ← #178 ← #180.
- **Waves**: #165 (5R) ← {#166, #167, #168, #169}; #172 (Wave 8) ; #176 (Wave 9) ← #179 (merged); #181 (RC) ← {#183, #185, #186, #187 merged; #188 closed}; **#182 Wave 10** = #176 + #180 + #170 + fixes; #190/#191 merged into Wave 10 checkpoint; **#192 Wave 11**.

## Branch-by-branch provenance

Columns: ahead/behind Wave 11; patch-unique / patch-equivalent commits (`git cherry`).

| Branch | HEAD | PR(s) | +/− | pu/pe | Classification | Evidence |
|---|---|---|---|---|---|---|
| `audit/bc01-bc02-20261022` | `1269cdbacc` | #150 | +0/−188 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `audit/prc-p5-pre-research-preservation-20261026` | `e00d65cbd8` | — | +0/−9 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `audit/prc-p6-pre-preservation-20261027` | `13c190d1ab` | — | +0/−7 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `audit/prc-p6-remaining-preservation-20261027` | `73dcc8f4d1` | #191 | +3/−7 | 0/3 | INTEGRATED — EQUIVALENT COMMITS | PR #191 (merged into Wave 10). All 3 commits patch-equivalent to Wave 11 d44a83c/7ff1d14/45fd25f; 90 files blob-identical. |
| `audit/stage23-pr92-pr93-reconciliation-20261019` | `079d36ac3b` | #125 | +145/−195 | 142/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #125 (closed) — base of open PRs #126/#127. Cumulative donor for Batch A; Stage 22/23-A research chain. Its retained runtime reached Wave 11 through #125 → Batch A #156 → Wave 5R composition a13d9777; every research effect it implemented has a live handler in the checkpoint (ResearchRewards + mixins, verified per ID) and renamed regression tests (Research23AEffectRulesTest, Research23AXpCollectionGameTests, Research23AMovementExhaustionGameTests, ResearchContentGameTests). Branch-only lines are older test fixtures/keys repaired by #166. |
| `checkpoint/wave10-unified-20261026` | `73dcc8f4d1` | — | +3/−7 | 0/3 | INTEGRATED — EQUIVALENT COMMITS | Wave 10 checkpoint. Its 3 post-#191 commits are patch-equivalent in Wave 11; trusted checkpoint ref. |
| `checkpoint/wave11-unified-20261027` | `c47c69f3e3` | #192 | +0/−0 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `claude/q1c-skull-provenance-20261019` | `6ba15ac2cb` | #128 | +147/−195 | 144/0 | UNRESOLVED — BLOCKED (owner decision / design) | PR #128 (open draft). Wither-skull provenance (4 mixins, QuestWitherSkullProvenance, 13 tests) and registration of xpq.cmb.wither_skulls_3. Owner F-1: rule approved but #128/#130 NOT release-ready; crash-consistency defect must be fixed before registration. |
| `claude/stage23a-economy-soil-20261017` | `9ec91dc535` | #110 | +87/−195 | 86/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #110. .53 Villager Trading + .16 Soil Study. Wave 11 has both (ResearchVillagerDiscount with owner-approved ceil rounding 2026-10-26; SoilStudyRuntime + 14 Soil tests). Branch's round-to-nearest test superseded. |
| `claude/wave-7b-decisions-reconciliation-xjdmva` | `0fb34bc512` | #175 | +0/−90 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `claude/xp-quest-authority-20261017` | `63107098fc` | #111, #112 | +96/−195 | 95/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #111/#112 (closed) — base of open PR #118. XP Quest authority docs present in Wave 11 (UI_ADVANCEMENT_INTEGRATION amendment, docs/design/xp-quest/*). |
| `claude/xp-quest-q1-integration` | `c40ee6b165` | #118 | +101/−195 | 98/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #118 (open draft) — base of open #119. XP Quest Q1 chain. Wave 11 carries the Q1-C/Batch A catalog (200 ids, 75 registered), exact raw-XP claim service (RawXp/QuestProgressService) and all Q1 hooks; #120 intentionally unregistered nine Q1-B quests lacking attribution tests (authority: register only after own test passes) and replaced the chunk-generator ore mixin with positive provenance. |
| `claude/xp-quest-q1a-core` | `78d1495402` | #114 | +96/−195 | 95/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #114. Catalog ledger/tracking reached Wave 11 via #118→#121→#156. XpLevels replaced by exact RawXp (authority: exact raw XP). XP Quest Q1 chain. Wave 11 carries the Q1-C/Batch A catalog (200 ids, 75 registered), exact raw-XP claim service (RawXp/QuestProgressService) and all Q1 hooks; #120 intentionally unregistered nine Q1-B quests lacking attribution tests (authority: register only after own test passes) and replaced the chunk-generator ore mixin with positive provenance. |
| `claude/xp-quest-q1a2-exact-xp` | `f30d263c93` | #115 | +97/−195 | 96/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #115. Exact raw-XP transactions + claim rollback present (RawXp, ExactXpTransactionGameTests, EnhancementTransactionGameTests). |
| `claude/xp-quest-q1b-tracking` | `c7fe7fabc5` | #119 | +103/−195 | 100/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #119 (open draft). XP Quest Q1 chain. Wave 11 carries the Q1-C/Batch A catalog (200 ids, 75 registered), exact raw-XP claim service (RawXp/QuestProgressService) and all Q1 hooks; #120 intentionally unregistered nine Q1-B quests lacking attribution tests (authority: register only after own test passes) and replaced the chunk-generator ore mixin with positive provenance. |
| `codex/accessory-client-acceptance-20261021` | `d7e8effd40` | #145 | +0/−157 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-client-lifecycle-final-20261022` | `6d37a73f11` | #149 | +0/−128 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-client-repair-20261022` | `b7f72fdae4` | #146 | +0/−148 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-enhancement-20261022` | `f9a31c7117` | #153 | +0/−117 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-infrastructure-20261021` | `9b79f9be03` | #142 | +0/−194 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-inventory-ui-redesign-20261023` | `ed4cc92aa5` | #160 | +0/−123 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-lifecycle-qa-20261021` | `2083cf37d1` | #144 | +0/−183 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-persistence-inventory-20261021` | `f2e6e87fe9` | #143 | +0/−192 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/accessory-visual-assets-20261022` | `9f6fba5fcc` | #152 | +0/−121 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/ancient-city-arena-prototype-20261013` | `e7dc53f9f4` | #83 | +5/−567 | 5/0 | RECOVERED IN WAVE 12 | PR #83 (open draft). Owner-approved first Ancient City Arena prototype (ANCIENT_CITY_STRUCTURE_EXPANSION §2-A/§4). Ported in Wave 12 commits 6bb483e1…209c251e; 2 GameTests + real worldgen re-verified. |
| `codex/bc03-bounty-contract-20261022` | `5ad001b091` | #151, #162 | +0/−177 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `codex/q1c-pr128-p1-p2-repair-20261019` | `9c9f225773` | #130 | +154/−195 | 151/0 | UNRESOLVED — BLOCKED (owner decision / design) | PR #130 (open draft). P2 pickup accounting fixed; P1 forced-halt receipt protocol still RED by its own acceptance test (A=1,B=1) and needs a durability-protocol design. Not integrated. |
| `codex/q1c-recovery-20261019-079d36a` | `334997d2f8` | #127 | +146/−195 | 143/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #127 (open draft) — base of #128. Skull decision recorded in Phase 2 F-1; build blocker report historical. |
| `codex/review-claude-pr110-20261017` | `8f33e72c8a` | #116 | +88/−195 | 87/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #116. Villager pricing repair carried by #123/#124 into #125; Wave 11 test villagerDiscountRoundsReductionUp… matches 2026-10-26 ceil authority. |
| `codex/stage22-research-implementation-20261015` | `53c2374e84` | #89 | +22/−195 | 22/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #89. Roots/passives/C-Lite present (ResearchRewards root effects, ResearchCatalog). Stage 22/23-A research chain. Its retained runtime reached Wave 11 through #125 → Batch A #156 → Wave 5R composition a13d9777; every research effect it implemented has a live handler in the checkpoint (ResearchRewards + mixins, verified per ID) and renamed regression tests (Research23AEffectRulesTest, Research23AXpCollectionGameTests, Research23AMovementExhaustionGameTests, ResearchContentGameTests). Branch-only lines are older test fixtures/keys repaired by #166. |
| `codex/stage23-livefix-a-20261016` | `eb80a14806` | #99 | +48/−195 | 48/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #99. Model/viewport repair retained via #125 (Batch A manifest). |
| `codex/stage23-livefix-b-20261016` | `5682be1071` | #105 | +82/−195 | 81/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #105 — base of open PR #107. Portable furnace persistence present (PortableFurnaceContents, PortableFurnaceRestartFixture; authority amendment 2026-10-16). |
| `codex/stage23-pr105-bundle-export-20261016` | `c035946fb8` | — | +83/−195 | 82/0 | VALIDATION TOOLING — NOT PRODUCT FUNCTIONALITY | No PR. One-off CI workflow (c035946f) that exported a PR #105 git bundle; no product code or authority. The workflow file exists only on this branch (no PR ref keeps it), so deletion would discard it; owner discard needed. |
| `codex/stage23a-batch2-20261015` | `68a7fafb4e` | #91 | +36/−195 | 36/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #91. .17/.28/.64 + wall_jump/fall_roll present (ResearchLivestockRuntime, ResearchAxeCombatRuntime, ResearchXpAcquisition, ResearchMovementRuntime). |
| `codex/stage23a-batch3-20261015` | `c3683de733` | #95 | +60/−195 | 60/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #95. Precision crafting .08 present (PrecisionCraftingRulesTest, k5-stage23a-batch3-crafting-probe workflow). |
| `codex/stage23a-expanded-batch2-20261017` | `2c8e984314` | #108 | +83/−195 | 82/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #108. .38/.39/.40/.51/.62 present (ResearchCreeperMixin, ResearchSpiderLeapMixin, ResearchEndermanMixin, ResearchBatch2Runtime, ResearchStatusEffectMixin). |
| `codex/stage23a-expanded-research-20261015` | `7231c7aad5` | #98 | +67/−195 | 67/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #98. .06/.07/.11/.44/.45/.47 on furnace infrastructure present (FurnaceResearchRules, Research23AExpandedGameTests). |
| `codex/stage23a-fisheries-hotfix-20261015` | `df960d6511` | #97 | +42/−195 | 42/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #97. Exact-fifths/largest-remainder Fisheries arithmetic present (Research23AEffectRulesTest.fisheriesUsesApprovedLargestRemainderTables…). |
| `codex/stage23a-furnace-infrastructure-20261015` | `f28658e5e0` | #96 | +66/−195 | 66/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #96. Furnace attribution present; branch's 'latest open wins' rule superseded by authority 'actual interaction outranks opening' (RESEARCH_KNOWLEDGE_TIER Batch 3 amendment) as implemented in Wave 11. |
| `codex/stage23a-integrated-research-20261015` | `5e48deacbe` | #94 | +41/−195 | 41/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #94. Selective #92 import (.19/.55/.67) present. |
| `codex/stage23a-reconcile-20261015` | `803d2e5df5` | #93 | +51/−195 | 51/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #93. ResearchParallelBranchRules/ResearchMovementExhaustion folded into Research23AEffectRules + ResearchMovementExhaustionMixin; manifest preserved (docs/preservation/PR93_…). Its RESEARCH 67 authority text restored in Wave 12 (0d035c84). |
| `codex/stage23a-remaining-research-20261017` | `9376777264` | #109 | +86/−195 | 85/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #109. .33 sweeps, .61 potions, .50 Riding, mob_capture gate present (ResearchConsecutiveSweep, ResearchSplash/LingeringPotionMixin, ResearchRidingRuntime, MobCaptureRuntime). |
| `codex/stage23a-research-content-20261015` | `1edbdb6bc6` | #90 | +30/−195 | 30/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #90. Batch 1 effects .02/.03/.18/.23 present (ResearchContentRuntime, ItemStackMixin, ResearchBeehiveMixin, FishingChallenges). |
| `codex/stage23a-research-implementation-20261015` | `f632f4f521` | #92 | +33/−195 | 33/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #92. Six effects present through evolved paths (ResearchBeehiveHarvestMixin → ResearchBeehiveMixin+ResearchBeehiveHarvestGuard). Manifest preserved (docs/preservation/PR92_…). RESEARCH 67 authority text restored in Wave 12. |
| `codex/stage23b-cloud-client-qa-design` | `9b66f49657` | #106 | +84/−195 | 83/0 | PRESERVED AS DOCUMENTATION | PR #106. QA design preserved in docs/preservation/prc-p6/PR106 (Wave 11 45fd25f). |
| `codex/stage23b-cloud-live-client-qa` | `dad054c7cb` | #107 | +137/−195 | 136/0 | VALIDATION TOOLING — NOT PRODUCT FUNCTIONALITY | PR #107 (open draft). Test-only clientQa source set + workflow; approved as validation tooling, not gameplay. Not integrated (would add a new source set/CI lane; real-client coverage in Wave 11 comes from client-visual-validation + Wave 7 A/E SDL input + accessory client gametests). |
| `codex/xp-quest-200-content-audit-20261020` | `3e66cfbdfa` | #140 | +26/−195 | 26/0 | PRESERVED AS DOCUMENTATION | PR #140. All 8 review records imported byte-for-byte by Batch A (docs/design/xp-quest/audit-200). |
| `codex/xp-quest-q1b-correction` | `fd4aa83113` | #120 | +108/−195 | 105/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #120. XP Quest Q1 chain. Wave 11 carries the Q1-C/Batch A catalog (200 ids, 75 registered), exact raw-XP claim service (RawXp/QuestProgressService) and all Q1 hooks; #120 intentionally unregistered nine Q1-B quests lacking attribution tests (authority: register only after own test passes) and replaced the chunk-generator ore mixin with positive provenance. |
| `codex/xp-quest-q1c` | `17e980912d` | #121 | +139/−195 | 136/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #121. Structure/combat/raid/monument tracking and 75 registrations present. |
| `codex/xp-quest-ui-integration-20261020` | `a124ccfc2e` | — | +2/−195 | 2/0 | PRESERVED AS DOCUMENTATION | No PR. Only a checkpoint doc, preserved blob-identical in docs/audit/BRANCH-W11-20261027/preserved/92c8a0f9….md. |
| `design/bounty-contract-approved-20261022` | `b7cedf199a` | #148 | +0/−190 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `design/pillager-headquarters-concept-20261022` | `55df673a83` | #147 | +15/−195 | 15/0 | PRESERVED AS DOCUMENTATION | PR #147. PILLAGER_HEADQUARTERS_DESIGN blob 904d16c4 identical in Wave 11; pyramid prep superseded by #181 version. |
| `design/pyramid-mob-roster-20261026` | `a86c6d5beb` | — | +1/−193 | 1/0 | PRESERVED AS DOCUMENTATION | No PR. STRUCTURE_AUTHORITY_INDEX routes ST-01 roster to this branch as an active design branch (not reviewed into authority). Copy preserved in docs/audit/BRANCH-W11-20261027/preserved/ad8d2e31….md. |
| `design/spider-colony-biome-20261026` | `f75638670f` | — | +39/−193 | 39/0 | PRESERVED AS DOCUMENTATION | No PR. STRUCTURE_AUTHORITY_INDEX routes ST-04 and ST-02 cancellation to this branch (active design). Copy preserved (fb5611f5….md). |
| `design/stage23a-remaining-approval-20261017` | `67d6c1fdd1` | — | +88/−195 | 87/0 | PRESERVED AS DOCUMENTATION | No PR. RESEARCH_STAGE23A_REMAINING_USER_DECISIONS preserved at docs/preservation/recovered/stage23a-remaining-approval/ and routed by the index. |
| `design/territory-d79-d80-reconcile-20261020` | `d641075f20` | #136 | +39/−195 | 39/0 | INTENTIONALLY ABANDONED / REJECTED | PR #136. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. |
| `design/territory-d81-d84-facility-prep-20261021` | `f2cc4f06db` | #137 | +41/−195 | 41/0 | INTENTIONALLY ABANDONED / REJECTED | PR #137. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. |
| `design/territory-economy-bounty-approved-20261019` | `befa4c8a44` | #131 | +25/−195 | 25/0 | INTENTIONALLY ABANDONED / REJECTED | PR #131. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. Commission board is historical-only (Q1). |
| `design/territory-owner-decisions-d01-d78-20261020` | `09b33f39f6` | #133 | +27/−195 | 27/0 | INTENTIONALLY ABANDONED / REJECTED | PR #133. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. |
| `design/territory-owner-decisions-reconcile-20261020` | `e0428763cf` | #134 | +28/−195 | 28/0 | INTENTIONALLY ABANDONED / REJECTED | PR #134. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. |
| `design/territory-registration-recovery-d2a-20261019` | `a1e0aeb0ab` | #132 | +26/−195 | 26/0 | INTENTIONALLY ABANDONED / REJECTED | PR #132. Territory design chain. Territory is SHELVED (TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21); every doc is preserved under docs/preservation/recovered/territory-chain or prc-p6/territory-versions (blob-verified). No code. |
| `design/territory-shelved-independent-content-20261021` | `284ce62ff5` | #138 | +44/−195 | 44/0 | PRESERVED AS DOCUMENTATION | PR #138. Shelving decision preserved and routed as current authority (docs/preservation/recovered/territory-chain/TERRITORY_SHELVED_…). |
| `design/xp-quest-selector-redesign-20261019` | `a9b89755f7` | #126 | +156/−195 | 153/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #126 (open draft) — base of open #129. XP Quest UI stack (#126 → #129 → #135). Batch A #156 recovered the exact #125→#135 file delta; Wave 4B custom screen authority (2026-10-24) and Wave 7 A/D input/contrast fixes then evolved the same files. Branch-only lines are pre-Wave-7 click/contrast code; QuestStructurePreview/QuestTreeLayout/Structure are present in Wave 11. |
| `design/xp-quest-structure-200-20261019` | `2176935929` | #129 | +160/−195 | 157/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #129 (open draft). XP Quest UI stack (#126 → #129 → #135). Batch A #156 recovered the exact #125→#135 file delta; Wave 4B custom screen authority (2026-10-24) and Wave 7 A/D input/contrast fixes then evolved the same files. Branch-only lines are pre-Wave-7 click/contrast code; QuestStructurePreview/QuestTreeLayout/Structure are present in Wave 11. |
| `design/xp-quest-usability-d1-d5-20261019` | `9a246f1e0c` | #135 | +172/−195 | 169/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #135. XP Quest UI stack (#126 → #129 → #135). Batch A #156 recovered the exact #125→#135 file delta; Wave 4B custom screen authority (2026-10-24) and Wave 7 A/D input/contrast fixes then evolved the same files. Branch-only lines are pre-Wave-7 click/contrast code; QuestStructurePreview/QuestTreeLayout/Structure are present in Wave 11. |
| `docs/2026-10-07-content-first-axis-review` | `8bf157f798` | #71 | +15/−815 | 15/0 | PRESERVED AS DOCUMENTATION | PR #71 (merged into main). Old pre-recovery lineage; its two unique docs preserved in docs/preservation/recovered/content-first-axis-review. Villager −2/−4/−6% line superseded by 5/10/15%. Old-lineage code (F/Shift+F, Wind Step, +30/+40 enhancement, Tier IV–V fishing) superseded by current authority. |
| `docs/accessory-enhancement-approved-20261017` | `32cbca0a3f` | #113 | +88/−195 | 87/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #113. ACCESSORY_ENHANCEMENT_DECISIONS present; implementation present (AccessoryEnhancement*). |
| `docs/alchemy-plot-notes` | `cc1a96b062` | — | +1/−698 | 1/0 | PRESERVED AS DOCUMENTATION | No PR. Single idea note preserved blob-identical (preserved/020ce8e8….md). |
| `docs/phase2-preservation-decisions-20261026` | `6f9c3ac2ad` | #189 | +5/−193 | 0/5 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #189 (open draft). All 5 commits patch-equivalent in Wave 11; Wave 11 then applied owner Q1–Q4 (which changed Q3 Feather Boots wording) — branch-only lines are the superseded pre-Q3 text. |
| `docs/ring-life-authority-option-a-20261025` | `bacb9546c9` | #183 | +10/−43 | 3/5 | INTEGRATED — EQUIVALENT COMMITS | PR #183 (merged into #181). Ring of Life Option A present in ACCESSORY_COOLDOWN_DECISION + regression test; all files identical/zero unique lines. |
| `docs/sky-islands-design-context-20261027` | `8ceff945ab` | — | +1/−193 | 1/0 | RECOVERED IN WAVE 12 | No PR; created 2026-10-27 00:26 UTC. Design-in-progress record copied byte-identical into Wave 12 (0d035c84). Not routed as authority; owner continues design on this branch. |
| `docs/stage23-accessories-approved-20261016` | `e532fa3034` | #101 | +68/−195 | 68/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #101. 12-accessory design present (ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16). |
| `docs/ui-piglin-mob-tracker-20261010` | `6fe1a308c9` | — | +1/−534 | 1/0 | PRESERVED AS DOCUMENTATION | No PR. Piglin trade/mob tracker UI record preserved (preserved/ba6dbd84….md); summarized in UI_SYSTEM_FULL_AUDIT. |
| `docs/wave4b-custom-quest-ui-authority-20261024` | `2e847d4956` | #164 | +3/−193 | 3/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #164. Custom Quest UI authority present as 'XP Quest UI architecture (2026-10-24)' in the index and the UI amendment. |
| `docs/world-events-patrol-design-20261018` | `ad77805971` | #122 | +93/−195 | 92/0 | PRESERVED AS DOCUMENTATION | PR #122. Patrol design preserved (docs/preservation/recovered/territory-chain/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md). Design only; no implementation exists on any branch. |
| `experiment/cloud-free-model-delegation-phase0` | `aa376713ae` | #141 | +1/−195 | 1/0 | INTENTIONALLY ABANDONED / REJECTED | PR #141 (open draft). Delegation experiment (OpenRouter scripts/workflow); explicitly excluded from production (PR #154 plan). |
| `experiment/ponytail-skill-2-20261021` | `f864df45ef` | — | +4/−195 | 4/0 | INTENTIONALLY ABANDONED / REJECTED | No PR. Experimental agent-skill files only; excluded from production (PR #154 plan). Files exist only on this branch (no PR ref). |
| `experiment/underground-kingdom-worldgen-probe` | `991b20c8a0` | #117 | +77/−195 | 76/0 | VALIDATION TOOLING — NOT PRODUCT FUNCTIONALITY | PR #117 (open draft). Feasibility probe; STRUCTURE_AUTHORITY_INDEX: 'experiment evidence only'. |
| `feature/bc03-approved-reconciliation-20261025` | `35c97ff413` | #178 | +0/−174 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `feature/wave7a-star-covenant-20261024` | `b6a03f4e5a` | #171 | +0/−92 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `feature/wave7b-accessory-abilities-20261025` | `91faed1092` | #174 | +0/−91 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `feature/wave7b-snow-charm-20261025` | `e72be04070` | #173 | +0/−92 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `fix/bc04-contract-structure-names-20261025` | `f3604c4593` | #180 | +0/−173 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `fix/exposure-audit-parallel-20261025` | `6d456e6fca` | #184 | +1/−193 | 0/1 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #184 (closed). Parallel downloads superseded by #186 paced audit (merged). |
| `fix/qm01-quest-monument-test-isolation` | `baa06dc4ae` | #179 | +0/−47 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `fix/wave9-live-input-harness-20261026` | `eb88cf8147` | #188 | +25/−41 | 9/9 | INTEGRATED — EQUIVALENT COMMITS | PR #188. Patch-equivalent 295634f in Wave 11; remaining diff is the superseded parallel-audit variant. |
| `integration/accessory-bounty-20261023` | `63efc3ca0a` | — | +0/−74 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `integration/accessory-system-20261023` | `0e9ccc5af9` | #161 | +0/−93 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `integration/batch-a-recovery-20261022` | `05a378211a` | #156 | +149/−195 | 146/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #156. Composed into Wave 11 by a13d9777; branch-only lines are pre-#166 test keys and pre-Wave-7 input code. |
| `integration/q1b-villager-discount-pr116-20261019` | `4fbb036e01` | #123 | +112/−195 | 109/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #123. Pricing repair retained via #124/#125. |
| `integration/q1c-pr116-discount-20261019` | `5ea5f54614` | #124 | +143/−195 | 140/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #124. Pricing repair retained via #125. |
| `integration/stage23-final-reconciliation-20261016` | `bdffa599e1` | #103 | +76/−195 | 75/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #103 — base of open PR #117. Sibling fixes retained via #125. |
| `integration/stage23-linear-chain-20261016` | `7231c7aad5` | #100 | +67/−195 | 67/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #100. Linear chain retained via #103/#125. |
| `integration/stage23-mobs-accessories-20261016` | `b0de6b7548` | #104 | +75/−195 | 75/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #104 (merged into #103). 40-mob resources present (CustomMobRenderers, textures). |
| `integration/stage23-reconciliation-audit-20261016` | `601cc28b9d` | #102 | +69/−195 | 69/0 | PRESERVED AS DOCUMENTATION | PR #102. Audit preserved in docs/preservation/prc-p6/PR102. |
| `integration/wave10-unified-20261025` | `13c190d1ab` | #182 | +0/−7 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `integration/wave5r-combined-20261024` | `a33d03ba1c` | #165 | +0/−72 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `integration/wave8-wave7-repairs-20261025` | `cb6412e204` | #172 | +0/−59 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `integration/wave9-accessory-abilities-20261025` | `decb3efa19` | #176 | +0/−46 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `main` | `cde842dd2c` | — | +0/−193 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `prototype/stage20-vanilla-sweep-texture` | `b1830ce606` | — | +1/−567 | 1/0 | INTENTIONALLY ABANDONED / REJECTED | No PR. Global vanilla sweep-texture override prototype; T2 VFX deferred and override excluded by PR #154 plan. Its sprites exist only on this branch (no PR ref). |
| `prototype/vanilla-sweep-texture` | `fe9d441f57` | — | +2/−814 | 2/0 | INTENTIONALLY ABANDONED / REJECTED | No PR. Same sweep prototype on older lineage; sprites exist only on this branch. |
| `recovery/vanilla-model-custom-mob-roster-base` | `cf4360b228` | — | +1/−590 | 1/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | No PR. One commit cf4360b2 (roster approval doc) whose content reached Wave 11 through #69; only the 'Baseline:' line differs. Pre-#69 test name superseded by portableFurnaceRetainsItsApprovedProcessingContract. Loss on deletion: one superseded doc revision. |
| `release/wave9-rc-20261025` | `268ec65bb8` | #181 | +44/−41 | 23/11 | INTEGRATED — EQUIVALENT COMMITS | PR #181 (open draft). All files identical in Wave 11 (pyramid docs, #183/#185/#186/#187); release candidate ref pinned by STRUCTURE_AUTHORITY_INDEX. |
| `resources/40-custom-mob-production-textures` | `0cc0a9eaf5` | #72 | +4/−567 | 4/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #72 (closed). Textures integrated via #104; remaining diffs are formatting-only. |
| `security/exposure-audit-pacing-20261025` | `97797b5deb` | #186 | +35/−41 | 15/11 | INTEGRATED — EQUIVALENT COMMITS | PR #186 (merged into #181). Content identical in Wave 11. |
| `security/gitleaks-historical-findings-20261022` | `b2f2f8f9bf` | #155 | +0/−194 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-1/fabric-foundation` | `ab5e2a5790` | #1 | +0/−920 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-10/draft-content` | `a2acc715ad` | — | +0/−797 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-11/checkpoint` | `3cb27187e3` | — | +0/−783 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-12/checkpoint` | `a8b6299f16` | — | +7/−817 | 6/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | Pre-recovery checkpoint (#36). Fishing core superseded by Stage 3 reconciliation; all 36 approved PNGs blob-identical in Wave 11. |
| `stage-13/checkpoint` | `94a264c2c6` | — | +20/−817 | 16/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | Pre-recovery checkpoint (#37/#38). Flame Sword C1-A/B: approved PNG identical; two-face model superseded by Stage 18 six-face contract. |
| `stage-14/checkpoint` | `94a264c2c6` | — | +20/−817 | 16/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | Pre-recovery checkpoint (#39 base). Same content as stage-13. |
| `stage-15/checkpoint` | `c39313e7d1` | — | +0/−758 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-16/checkpoint` | `70e470deb2` | — | +0/−729 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-17/checkpoint` | `d8493c62bb` | — | +0/−709 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-18/checkpoint` | `93d264a8f7` | — | +0/−624 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-18/integration` | `e4e99c36c1` | — | +0/−701 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-19/checkpoint` | `984418d302` | — | +0/−614 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-2/enhancement-stone` | `6ae717c92d` | #3 | +0/−916 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-20/checkpoint` | `20121ca7cc` | — | +0/−567 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-21/knowledge-checkpoint` | `c0ce63cb73` | — | +0/−196 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-22/research-restructure` | `d65f590593` | #88 | +7/−195 | 7/0 | SUPERSEDED / REIMPLEMENTED IN CHECKPOINT | PR #88 (closed). V3-inclusive prerequisite validation (purchasedResearch over allCatalogEntries) present in Wave 11 ResearchPurchaseService; docs present. |
| `stage-3/enhancement-vertical-slice` | `1f05e53fba` | #5 | +0/−910 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-4/equipment-foundation` | `8a3159c5a3` | #11 | +0/−878 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-5/special-equipment` | `e66bf7ecb3` | #14 | +0/−866 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-6/materials-crafting-progression` | `c8705f4c39` | #16 | +0/−851 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-7/life-content` | `a62a65e10c` | #18 | +0/−844 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-8/entity-mob` | `5541ad505a` | #21 | +0/−837 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-9/skeleton-king` | `ec1b6ce918` | #23 | +0/−827 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `stage-ci/accessory-bounty-20261023` | `63efc3ca0a` | — | +0/−74 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `test/bc03-validation-hardening-20261025` | `cc058692ca` | #177 | +0/−176 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `test/k5-reused-world-determinism-20261025` | `9f565a664a` | #187 | +22/−41 | 8/8 | INTEGRATED — EQUIVALENT COMMITS | PR #187 (merged into #181). Equivalent commits in Wave 11 (f4a49bd, ff249bf). |
| `test/ring-life-option-a-regression-20261025` | `efc77967c9` | #185 | +13/−43 | 4/6 | INTEGRATED — EQUIVALENT COMMITS | PR #185 (merged into #181). Regression test present (5622585). |
| `tooling/integration-gametest-coverage-20261024` | `741a1be900` | #170 | +0/−192 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `wave7/a-e-real-mouse-input` | `0f466b96df` | #168 | +0/−67 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `wave7/b-gametest-recovery` | `e5982f3c2f` | #166 | +0/−71 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `wave7/c-accessory-raw-xp` | `06be9f53ff` | #167 | +0/−71 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |
| `wave7/d-ui-cleanup` | `8f53b48816` | #169 | +0/−70 | 0/0 | INTEGRATED (ancestor of Wave 11) | Every commit is an ancestor of Wave 11 (git merge-base --is-ancestor). |

## PR heads reachable only through refs/pull (branch deleted)

Semantic inventory and per-file analysis were run on each `refs/pull/N/head`.

| PR | Head | Classification | Evidence |
|---:|---|---|---|
| #34 | `3e19227704` | SUPERSEDED | Resource material textures pass (merged into `main`); deepslate-variant note superseded by #35; old-lineage F/Shift+F, Wind Step, +30/+40, Tier IV–V content superseded (Stage 2/3 reconciliations). |
| #39 | `2a327923ed` | SUPERSEDED | Closed audit repair on stage-14; worldgen/CI guardrails reached the recovered line (#56/#64); same old-lineage code as above. |
| #65 | `9ba8f422ae` | RECOVERED IN WAVE 12 (tests) | Closed lower-equipment live repair. Fixes present in Wave 11 in evolved form (FilteredChestMenu, drill `ownsSource`, `GreatswordPlayerInteractionMixin`, vanilla fuel providers); its regression GameTests were missing — 15 ported unchanged + 1 adapted in `LowerEquipmentRegressionGameTests` (7ef97c4c), all pass. Close-and-drop and left-of-helmet socket tests superseded (2026-10-16 persistence, 2026-10-23 layout). Old Earth Mace fracture/shatter math superseded by T2 seismic finalization. |
| #70 | `7c164fe368` | SUPERSEDED | Oct 7 decisions doc; merged into `main` as 03fefe14 and rewritten by the owner in 1b600b77 (current text in Wave 11). |
| #73 | `3a0cd8cc27` | SUPERSEDED | Client visual validation prototype; evolved into `client-visual-validation.yml` in Wave 11. |
| #74 | `a5a217e101` | PRESERVED AS DOCUMENTATION | Oct 10 artifact/research decisions; the decision record is preserved (lineage-rebased copy) at docs/preservation/recovered/content-first-axis-review/DESIGN_IDEA_DECISIONS_2026-10-10.md; Q3 governs Feather Boots. |
| #78 | `d90eb70b10` | PRESERVED VIA PR REF | Stage 1 research/knowledge runtime audit (audit/stage1/*); historical, superseded by the 87-node audit; content kept by refs/pull/78/head. |
| #81 | `be0b6c7874` | SUPERSEDED | K5 redesign doc; superseded by K5 production integration (#84 → #86 → #87). |
| #85 | `bc092d8c30` | INTENTIONALLY ABANDONED / REJECTED | Titled "WRONG REPOSITORY — DO NOT MERGE"; whole-city variants are concepts with unconfirmed details and excluded from the arena prototype (STRUCTURE_AUTHORITY_INDEX). |
| #139 | `526416b16a` | PRESERVED AS DOCUMENTATION | Q1-C readiness audit; preserved in docs/preservation/prc-p6/PR139. |
| #154 | `84550ad4ef` | PRESERVED AS DOCUMENTATION | Repository PR reconciliation plan; preserved in docs/preservation/prc-p6/PR154. |
| #157 | `cce99b6cc7` | SUPERSEDED | Gitleaks remediation variant; superseded by merged #155. |
| #158 | `c9e29c5680` | PRESERVED AS DOCUMENTATION | Cleanup safety audit; preserved in docs/preservation/prc-p6/PR158. |
| #159 | `5a0ede4051` | PRESERVED AS DOCUMENTATION | Cleanup phase 2B audit; preserved in docs/preservation/prc-p6/PR159. |
| #163 | `01a36b0caf` | PRESERVED AS DOCUMENTATION | Phase 3 readiness audit; preserved in docs/preservation/prc-p6/PR163. |

## Pull requests

| PR | State | Head → base | Head SHA | Head reachable from |
|---:|---|---|---|---|
| #1 | merged | `stage-1/fabric-foundation` → `main` | `ab5e2a5790` | Wave 11 ancestry |
| #2 | merged | `codex/implement-enhancement_stone-as-custom-item` → `stage-2/enhancement-stone` | `f9389ed2c1` | Wave 11 ancestry |
| #3 | merged | `stage-2/enhancement-stone` → `main` | `6ae717c92d` | Wave 11 ancestry |
| #4 | merged | `codex/implement-stage-3-enhancement-vertical-slice` → `stage-3/enhancement-vertical-slice` | `04afc218b7` | Wave 11 ancestry |
| #5 | merged | `stage-3/enhancement-vertical-slice` → `main` | `1f05e53fba` | Wave 11 ancestry |
| #6 | merged | `codex/generalize-enhancement-foundation-for-stage-4a` → `stage-4/equipment-foundation` | `a14b476d00` | Wave 11 ancestry |
| #7 | merged | `codex/implement-stage-4b-1-melee-and-tool-effects` → `stage-4/equipment-foundation` | `275e226101` | Wave 11 ancestry |
| #8 | merged | `codex/implement-stage-4b-2-ranged-effects` → `stage-4/equipment-foundation` | `e8ea4221a8` | Wave 11 ancestry |
| #9 | merged | `codex/implement-stage-4b-3-armor-enhancement-effects` → `stage-4/equipment-foundation` | `af696580d8` | Wave 11 ancestry |
| #10 | merged | `codex/implement-stage-4c-ability-cooldown-foundation` → `stage-4/equipment-foundation` | `94944909a1` | Wave 11 ancestry |
| #11 | merged | `stage-4/equipment-foundation` → `main` | `8a3159c5a3` | Wave 11 ancestry |
| #12 | merged | `codex/implement-flame-sword-for-stage-5a` → `stage-5/special-equipment` | `866a71f0c5` | Wave 11 ancestry |
| #13 | merged | `codex/implement-stage-5b-special-equipment-system` → `stage-5/special-equipment` | `96944bd836` | Wave 11 ancestry |
| #14 | merged | `stage-5/special-equipment` → `main` | `e66bf7ecb3` | Wave 11 ancestry |
| #15 | merged | `codex/implement-stage-6-materials-and-crafting` → `stage-6/materials-crafting-progression` | `19b23d77d6` | Wave 11 ancestry |
| #16 | merged | `stage-6/materials-crafting-progression` → `main` | `c8705f4c39` | Wave 11 ancestry |
| #17 | merged | `codex/implement-stage-7-fishing-life-content-t94ezj` → `stage-7/life-content` | `8b5a14921d` | Wave 11 ancestry |
| #18 | merged | `stage-7/life-content` → `main` | `a62a65e10c` | Wave 11 ancestry |
| #19 | merged | `codex/implement-stage-8a-zombie-variants` → `stage-8/entity-mob` | `9c1a291c44` | Wave 11 ancestry |
| #20 | merged | `codex/implement-mining-giant-for-stage-8` → `stage-8/entity-mob` | `8d83cfc568` | Wave 11 ancestry |
| #21 | merged | `stage-8/entity-mob` → `main` | `5541ad505a` | Wave 11 ancestry |
| #22 | merged | `codex/implement-stage-9a-skeleton-king-boss` → `stage-9/skeleton-king` | `adc7f780ed` | Wave 11 ancestry |
| #23 | merged | `stage-9/skeleton-king` → `main` | `ec1b6ce918` | Wave 11 ancestry |
| #24 | merged | `work/stage-10-draft-audit` → `stage-10/draft-content` | `e675e72589` | Wave 11 ancestry |
| #25 | merged | `work/stage-10-full-content-recovery` → `stage-10/draft-content` | `58dbcc39b3` | Wave 11 ancestry |
| #26 | merged | `codex/add-stage-10-live-test-command` → `stage-10/draft-content` | `e381dffd04` | Wave 11 ancestry |
| #27 | merged | `codex/repair-stage-6-mineral-worldgen-for-minecraft-26.3` → `stage-10/draft-content` | `795cb44ae7` | Wave 11 ancestry |
| #28 | merged | `fix/mc26.3-feature-registry-migration` → `stage-10/draft-content` | `7a672e3cab` | Wave 11 ancestry |
| #29 | merged | `codex/implement-shock-potion-for-stage-10` → `stage-10/draft-content` | `0ede6657df` | Wave 11 ancestry |
| #30 | merged | `codex/continue-development-of-moreminecrafts-project` → `stage-10/draft-content` | `f58356ae59` | Wave 11 ancestry |
| #31 | merged | `codex/continue-moreminecrafts-t2-elemental-work` → `stage-11/checkpoint` | `ced23001bf` | Wave 11 ancestry |
| #32 | merged | `codex/continue-development-of-moreminecrafts-fabric` → `stage-11/checkpoint` | `dad4e559e6` | Wave 11 ancestry |
| #33 | merged | `codex/implement-checkpoint-12-for-t2-equipment` → `stage-11/checkpoint` | `90b7c3a0bd` | Wave 11 ancestry |
| #34 | merged | `resource/material-textures-pass-1` → `main` | `3e19227704` | refs/pull only |
| #35 | merged | `worldgen/deepslate-ore-variants` → `main` | `922ac0252b` | Wave 11 ancestry |
| #36 | merged | `codex/implement-first-code-reconciliation-pass` → `stage-12/checkpoint` | `426ea7bfe8` | remote branch |
| #37 | merged | `resource-c1/flame-sword-render-foundation` → `stage-13/checkpoint` | `b49e6dd6b7` | remote branch |
| #38 | merged | `resource-c1b/flame-sword-production-texture` → `stage-13/checkpoint` | `d73f19701e` | remote branch |
| #39 | closed | `audit/local-worldgen-ci-repair` → `stage-14/checkpoint` | `2a327923ed` | refs/pull only |
| #40 | closed | `recovery/post-stage8-cumulative` → `main` | `0264b94cbb` | Wave 11 ancestry |
| #41 | closed | `recovery/stage-2-gameplay-reconciliation` → `recovery/post-stage8-cumulative` | `fde245ab36` | Wave 11 ancestry |
| #42 | closed | `recovery/stage-3-fishing-resource-reconciliation` → `recovery/stage-2-gameplay-reconciliation` | `8de9efdd74` | Wave 11 ancestry |
| #43 | closed | `recovery/stage-4-final-integration` → `recovery/stage-3-fishing-resource-reconciliation` | `c39313e7d1` | Wave 11 ancestry |
| #44 | closed | `fix/batch-1-t2-runtime-correctness` → `stage-15/checkpoint` | `4132c0e50f` | Wave 11 ancestry |
| #45 | closed | `fix/batch-2-water-dive-lifecycle` → `fix/batch-1-t2-runtime-correctness` | `5979ccd89c` | Wave 11 ancestry |
| #46 | closed | `fix/batch-3-fishing-component-mutation` → `fix/batch-2-water-dive-lifecycle` | `d372a82294` | Wave 11 ancestry |
| #47 | closed | `fix/batch-4-validation-regression-protection` → `fix/batch-3-fishing-component-mutation` | `983744e349` | Wave 11 ancestry |
| #48 | closed | `fix/batch-5-final-defect-and-validation` → `fix/batch-4-validation-regression-protection` | `f167094fa8` | Wave 11 ancestry |
| #49 | closed | `fix/def-003-dimension-transfer` → `fix/batch-5-final-defect-and-validation` | `a153db7253` | Wave 11 ancestry |
| #51 | merged | `codex/implement-enchantment-vertical-for-stage-17` → `stage-16/checkpoint` | `0ec56186fa` | Wave 11 ancestry |
| #52 | closed | `codex/fix-enchantment-vertical-defects-and-validate` → `codex/implement-enchantment-vertical-for-stage-17` | `9b1a02f77e` | Wave 11 ancestry |
| #53 | merged | `codex/perform-final-runtime-repair-for-stage-17` → `codex/fix-enchantment-vertical-defects-and-validate` | `40d51e7ef5` | Wave 11 ancestry |
| #54 | merged | `governance/ci-acceptance-lifecycle` → `stage-16/checkpoint` | `aaed797e25` | Wave 11 ancestry |
| #55 | merged | `governance/design-authority-sync` → `stage-16/checkpoint` | `acbe041c4b` | Wave 11 ancestry |
| #56 | merged | `reconcile/stage-17-checkpoint` → `stage-16/checkpoint` | `4c5df63908` | Wave 11 ancestry |
| #57 | merged | `codex/repair-titanium-tungsten-block-texture-scale` → `stage-17/checkpoint` | `bc24d1f1af` | Wave 11 ancestry |
| #58 | closed | `codex/post-live-flame-sword-mineral-repair` → `stage-17/checkpoint` | `96868acbb8` | Wave 11 ancestry |
| #59 | closed | `codex/finalize-cumulative-live-test-defect-repairs` → `codex/post-live-flame-sword-mineral-repair` | `b2dec4e85e` | Wave 11 ancestry |
| #60 | closed | `codex/repair-fishing-equipment-panel-layout` → `stage-17/checkpoint` | `83b5ba7696` | Wave 11 ancestry |
| #61 | merged | `docs/stage-18-flame-sword-authority-sync` → `stage-18/checkpoint` | `42de45c052` | Wave 11 ancestry |
| #62 | merged | `codex/implement-lower-equipment-gameplay-systems` → `codex/provisional-equipment-resources` | `bc11edf3b9` | Wave 11 ancestry |
| #63 | merged | `work/pr62-acceptance-repair` → `codex/implement-lower-equipment-gameplay-systems` | `9f1c6aa920` | Wave 11 ancestry |
| #64 | merged | `codex/provisional-equipment-resources` → `stage-18/checkpoint` | `60ce2c3b4b` | Wave 11 ancestry |
| #65 | closed | `codex/post-stage18-lower-equipment-live-repair` → `stage-18/checkpoint` | `9ba8f422ae` | refs/pull only |
| #66 | merged | `implementation/t2-elemental-gameplay-finalization` → `stage-19/checkpoint` | `7e5dbd730e` | Wave 11 ancestry |
| #67 | merged (draft) | `design/t1-enchantment-skill-tree` → `stage-19/checkpoint` | `233e2e243e` | Wave 11 ancestry |
| #68 | merged | `implementation/t1-enchantment-progression` → `stage-19/checkpoint` | `cbc3fa1ab0` | Wave 11 ancestry |
| #69 | merged | `recovery/vanilla-model-custom-mobs-7730b10` → `stage-20/checkpoint` | `00542ce999` | Wave 11 ancestry |
| #70 | merged | `design/end-boss-continuation-2026-10-07` → `main` | `7c164fe368` | refs/pull only |
| #71 | merged | `docs/2026-10-07-content-first-axis-review` → `main` | `1b600b7755` | Wave 11 ancestry |
| #72 | closed | `resources/40-custom-mob-production-textures` → `stage-20/checkpoint` | `0cc0a9eaf5` | remote branch |
| #73 | closed (draft) | `codex/prototype-client-visual-validation-20261009` → `main` | `3a0cd8cc27` | refs/pull only |
| #74 | closed (draft) | `docs/2026-10-10-artifact-research-decisions` → `stage-20/checkpoint` | `a5a217e101` | refs/pull only |
| #75 | closed (draft) | `feat/research-v4-a-c-20261010` → `stage-20/checkpoint` | `d1f113720c` | Wave 11 ancestry |
| #76 | closed (draft) | `feat/research-knowledge-ui-integration` → `feat/research-v4-a-c-20261010` | `5dad54adc6` | Wave 11 ancestry |
| #77 | merged | `security/pre-public-hardening-20261012` → `main` | `9fadf2d703` | Wave 11 ancestry |
| #78 | closed | `codex/stage1-runtime-audit-20261012` → `feat/research-knowledge-ui-integration` | `d90eb70b10` | refs/pull only |
| #79 | closed (draft) | `codex/stage2a-knowledge-collectors-20261012` → `feat/research-knowledge-ui-integration` | `204f706cb2` | Wave 11 ancestry |
| #80 | closed (draft) | `codex/stage2b-knowledge-conditions-20261012` → `codex/stage2a-knowledge-collectors-20261012` | `1a17f8e777` | Wave 11 ancestry |
| #81 | closed (draft) | `codex/k5-natural-tree-provenance-design-20261013` → `codex/stage2b-knowledge-conditions-20261012` | `be0b6c7874` | refs/pull only |
| #82 | closed (draft) | `codex/k5-natural-tree-mining-probe-20261013` → `codex/stage2b-knowledge-conditions-20261012` | `080a3f05b7` | Wave 11 ancestry |
| #83 | open (draft) | `codex/ancient-city-arena-prototype-20261013` → `stage-20/checkpoint` | `e7dc53f9f4` | remote branch |
| #84 | closed (draft) | `codex/k5-production-integration-20261013` → `codex/k5-natural-tree-mining-probe-20261013` | `bd816aef9c` | Wave 11 ancestry |
| #85 | closed (draft) | `codex/ancient-city-whole-variant-probe-20261014` → `stage-20/checkpoint` | `bc092d8c30` | refs/pull only |
| #86 | closed (draft) | `codex/knowledge-consolidation-review-20261014` → `codex/k5-production-integration-20261013` | `ca8478f8ba` | Wave 11 ancestry |
| #87 | merged | `codex/knowledge-stack-integration-20261014` → `main` | `c0ce63cb73` | Wave 11 ancestry |
| #88 | closed | `stage-22/research-restructure` → `main` | `d65f590593` | remote branch |
| #89 | closed (draft) | `codex/stage22-research-implementation-20261015` → `stage-22/research-restructure` | `53c2374e84` | remote branch |
| #90 | closed (draft) | `codex/stage23a-research-content-20261015` → `codex/stage22-research-implementation-20261015` | `1edbdb6bc6` | remote branch |
| #91 | closed (draft) | `codex/stage23a-batch2-20261015` → `codex/stage23a-research-content-20261015` | `68a7fafb4e` | remote branch |
| #92 | closed (draft) | `codex/stage23a-research-implementation-20261015` → `codex/stage22-research-implementation-20261015` | `f632f4f521` | remote branch |
| #93 | closed (draft) | `codex/stage23a-reconcile-20261015` → `codex/stage23a-batch2-20261015` | `803d2e5df5` | remote branch |
| #94 | closed (draft) | `codex/stage23a-integrated-research-20261015` → `codex/stage23a-batch2-20261015` | `5e48deacbe` | remote branch |
| #95 | closed (draft) | `codex/stage23a-batch3-20261015` → `codex/stage23a-integrated-research-20261015` | `c3683de733` | remote branch |
| #96 | closed (draft) | `codex/stage23a-furnace-infrastructure-20261015` → `codex/stage23a-batch3-20261015` | `f28658e5e0` | remote branch |
| #97 | closed (draft) | `codex/stage23a-fisheries-hotfix-20261015` → `codex/stage23a-integrated-research-20261015` | `df960d6511` | remote branch |
| #98 | closed (draft) | `codex/stage23a-expanded-research-20261015` → `codex/stage23a-furnace-infrastructure-20261015` | `7231c7aad5` | remote branch |
| #99 | closed (draft) | `codex/stage23-livefix-a-20261016` → `codex/stage23a-integrated-research-20261015` | `eb80a14806` | remote branch |
| #100 | closed (draft) | `integration/stage23-linear-chain-20261016` → `codex/stage23a-batch2-20261015` | `7231c7aad5` | remote branch |
| #101 | closed (draft) | `docs/stage23-accessories-approved-20261016` → `integration/stage23-linear-chain-20261016` | `e532fa3034` | remote branch |
| #102 | closed (draft) | `integration/stage23-reconciliation-audit-20261016` → `integration/stage23-linear-chain-20261016` | `601cc28b9d` | remote branch |
| #103 | closed (draft) | `integration/stage23-final-reconciliation-20261016` → `integration/stage23-linear-chain-20261016` | `bdffa599e1` | remote branch |
| #104 | merged | `integration/stage23-mobs-accessories-20261016` → `integration/stage23-final-reconciliation-20261016` | `b0de6b7548` | remote branch |
| #105 | closed (draft) | `codex/stage23-livefix-b-20261016` → `integration/stage23-final-reconciliation-20261016` | `5682be1071` | remote branch |
| #106 | closed (draft) | `codex/stage23b-cloud-client-qa-design` → `codex/stage23-livefix-b-20261016` | `9b66f49657` | remote branch |
| #107 | open (draft) | `codex/stage23b-cloud-live-client-qa` → `codex/stage23-livefix-b-20261016` | `dad054c7cb` | remote branch |
| #108 | closed (draft) | `codex/stage23a-expanded-batch2-20261017` → `codex/stage23-livefix-b-20261016` | `2c8e984314` | remote branch |
| #109 | closed (draft) | `codex/stage23a-remaining-research-20261017` → `codex/stage23a-expanded-batch2-20261017` | `9376777264` | remote branch |
| #110 | closed (draft) | `claude/stage23a-economy-soil-20261017` → `codex/stage23a-remaining-research-20261017` | `9ec91dc535` | remote branch |
| #111 | closed | `claude/xp-quest-authority-20261017` → `main` | `63107098fc` | remote branch |
| #112 | closed (draft) | `claude/xp-quest-authority-20261017` → `claude/stage23a-economy-soil-20261017` | `63107098fc` | remote branch |
| #113 | closed (draft) | `docs/accessory-enhancement-approved-20261017` → `claude/stage23a-economy-soil-20261017` | `32cbca0a3f` | remote branch |
| #114 | closed (draft) | `claude/xp-quest-q1a-core` → `claude/xp-quest-authority-20261017` | `78d1495402` | remote branch |
| #115 | closed (draft) | `claude/xp-quest-q1a2-exact-xp` → `claude/xp-quest-authority-20261017` | `f30d263c93` | remote branch |
| #116 | closed (draft) | `codex/review-claude-pr110-20261017` → `claude/stage23a-economy-soil-20261017` | `8f33e72c8a` | remote branch |
| #117 | open (draft) | `experiment/underground-kingdom-worldgen-probe` → `integration/stage23-final-reconciliation-20261016` | `991b20c8a0` | remote branch |
| #118 | open (draft) | `claude/xp-quest-q1-integration` → `claude/xp-quest-authority-20261017` | `c40ee6b165` | remote branch |
| #119 | open (draft) | `claude/xp-quest-q1b-tracking` → `claude/xp-quest-q1-integration` | `c7fe7fabc5` | remote branch |
| #120 | closed (draft) | `codex/xp-quest-q1b-correction` → `claude/xp-quest-q1b-tracking` | `fd4aa83113` | remote branch |
| #121 | closed (draft) | `codex/xp-quest-q1c` → `codex/xp-quest-q1b-correction` | `17e980912d` | remote branch |
| #122 | closed (draft) | `docs/world-events-patrol-design-20261018` → `docs/accessory-enhancement-approved-20261017` | `ad77805971` | remote branch |
| #123 | closed (draft) | `integration/q1b-villager-discount-pr116-20261019` → `codex/xp-quest-q1b-correction` | `4fbb036e01` | remote branch |
| #124 | closed (draft) | `integration/q1c-pr116-discount-20261019` → `codex/xp-quest-q1c` | `5ea5f54614` | remote branch |
| #125 | closed (draft) | `audit/stage23-pr92-pr93-reconciliation-20261019` → `integration/q1c-pr116-discount-20261019` | `079d36ac3b` | remote branch |
| #126 | open (draft) | `design/xp-quest-selector-redesign-20261019` → `audit/stage23-pr92-pr93-reconciliation-20261019` | `a9b89755f7` | remote branch |
| #127 | open (draft) | `codex/q1c-recovery-20261019-079d36a` → `audit/stage23-pr92-pr93-reconciliation-20261019` | `334997d2f8` | remote branch |
| #128 | open (draft) | `claude/q1c-skull-provenance-20261019` → `codex/q1c-recovery-20261019-079d36a` | `6ba15ac2cb` | remote branch |
| #129 | open (draft) | `design/xp-quest-structure-200-20261019` → `design/xp-quest-selector-redesign-20261019` | `2176935929` | remote branch |
| #130 | open (draft) | `codex/q1c-pr128-p1-p2-repair-20261019` → `claude/q1c-skull-provenance-20261019` | `9c9f225773` | remote branch |
| #131 | closed (draft) | `design/territory-economy-bounty-approved-20261019` → `main` | `befa4c8a44` | remote branch |
| #132 | closed (draft) | `design/territory-registration-recovery-d2a-20261019` → `design/territory-economy-bounty-approved-20261019` | `a1e0aeb0ab` | remote branch |
| #133 | closed (draft) | `design/territory-owner-decisions-d01-d78-20261020` → `design/territory-registration-recovery-d2a-20261019` | `09b33f39f6` | remote branch |
| #134 | closed (draft) | `design/territory-owner-decisions-reconcile-20261020` → `design/territory-owner-decisions-d01-d78-20261020` | `e0428763cf` | remote branch |
| #135 | closed (draft) | `design/xp-quest-usability-d1-d5-20261019` → `design/xp-quest-structure-200-20261019` | `9a246f1e0c` | remote branch |
| #136 | closed (draft) | `design/territory-d79-d80-reconcile-20261020` → `design/territory-owner-decisions-reconcile-20261020` | `d641075f20` | remote branch |
| #137 | closed (draft) | `design/territory-d81-d84-facility-prep-20261021` → `design/territory-d79-d80-reconcile-20261020` | `f2cc4f06db` | remote branch |
| #138 | closed (draft) | `design/territory-shelved-independent-content-20261021` → `design/territory-d81-d84-facility-prep-20261021` | `284ce62ff5` | remote branch |
| #139 | closed (draft) | `codex/q1c-production-readiness-audit-20261020` → `main` | `526416b16a` | refs/pull only |
| #140 | closed (draft) | `codex/xp-quest-200-content-audit-20261020` → `main` | `3e66cfbdfa` | remote branch |
| #141 | open (draft) | `experiment/cloud-free-model-delegation-phase0` → `main` | `aa376713ae` | remote branch |
| #142 | closed (draft) | `codex/accessory-infrastructure-20261021` → `main` | `9b79f9be03` | Wave 11 ancestry |
| #143 | closed (draft) | `codex/accessory-persistence-inventory-20261021` → `codex/accessory-infrastructure-20261021` | `f2e6e87fe9` | Wave 11 ancestry |
| #144 | closed (draft) | `codex/accessory-lifecycle-qa-20261021` → `codex/accessory-persistence-inventory-20261021` | `2083cf37d1` | Wave 11 ancestry |
| #145 | closed (draft) | `codex/accessory-client-acceptance-20261021` → `codex/accessory-lifecycle-qa-20261021` | `d7e8effd40` | Wave 11 ancestry |
| #146 | closed (draft) | `codex/accessory-client-repair-20261022` → `codex/accessory-client-acceptance-20261021` | `b7f72fdae4` | Wave 11 ancestry |
| #147 | closed (draft) | `design/pillager-headquarters-concept-20261022` → `main` | `55df673a83` | remote branch |
| #148 | closed (draft) | `design/bounty-contract-approved-20261022` → `main` | `b7cedf199a` | Wave 11 ancestry |
| #149 | closed (draft) | `codex/accessory-client-lifecycle-final-20261022` → `codex/accessory-client-repair-20261022` | `6d37a73f11` | Wave 11 ancestry |
| #150 | closed (draft) | `audit/bc01-bc02-20261022` → `main` | `1269cdbacc` | Wave 11 ancestry |
| #151 | closed (draft) | `codex/bc03-bounty-contract-20261022` → `main` | `5ad001b091` | Wave 11 ancestry |
| #152 | closed (draft) | `codex/accessory-visual-assets-20261022` → `codex/accessory-client-lifecycle-final-20261022` | `9f6fba5fcc` | Wave 11 ancestry |
| #153 | closed (draft) | `codex/accessory-enhancement-20261022` → `codex/accessory-client-lifecycle-final-20261022` | `f9a31c7117` | Wave 11 ancestry |
| #154 | closed (draft) | `audit/repository-pr-reconciliation-20261022` → `main` | `84550ad4ef` | refs/pull only |
| #155 | merged | `security/gitleaks-historical-findings-20261022` → `main` | `b2f2f8f9bf` | Wave 11 ancestry |
| #156 | closed (draft) | `integration/batch-a-recovery-20261022` → `main` | `05a378211a` | remote branch |
| #157 | closed (draft) | `security/gitleaks-remediation-20261022-codex-725159` → `main` | `cce99b6cc7` | refs/pull only |
| #158 | closed (draft) | `audit/repository-cleanup-phase2-20261023` → `main` | `c9e29c5680` | refs/pull only |
| #159 | closed (draft) | `audit/repository-cleanup-phase2b-20261023` → `main` | `5a0ede4051` | refs/pull only |
| #160 | closed (draft) | `codex/accessory-inventory-ui-redesign-20261023` → `codex/accessory-client-lifecycle-final-20261022` | `ed4cc92aa5` | Wave 11 ancestry |
| #161 | closed (draft) | `integration/accessory-system-20261023` → `codex/accessory-client-lifecycle-final-20261022` | `0e9ccc5af9` | Wave 11 ancestry |
| #162 | merged (draft) | `codex/bc03-bounty-contract-20261022` → `integration/accessory-bounty-20261023` | `5ad001b091` | Wave 11 ancestry |
| #163 | closed (draft) | `audit/phase3-stage23-xpquest-readiness-20261023` → `main` | `01a36b0caf` | refs/pull only |
| #164 | closed (draft) | `docs/wave4b-custom-quest-ui-authority-20261024` → `main` | `2e847d4956` | remote branch |
| #165 | closed (draft) | `integration/wave5r-combined-20261024` → `main` | `a33d03ba1c` | Wave 11 ancestry |
| #166 | closed (draft) | `wave7/b-gametest-recovery` → `integration/wave5r-combined-20261024` | `e5982f3c2f` | Wave 11 ancestry |
| #167 | closed (draft) | `wave7/c-accessory-raw-xp` → `integration/wave5r-combined-20261024` | `06be9f53ff` | Wave 11 ancestry |
| #168 | closed (draft) | `wave7/a-e-real-mouse-input` → `integration/wave5r-combined-20261024` | `0f466b96df` | Wave 11 ancestry |
| #169 | closed (draft) | `wave7/d-ui-cleanup` → `integration/wave5r-combined-20261024` | `8f53b48816` | Wave 11 ancestry |
| #170 | closed (draft) | `tooling/integration-gametest-coverage-20261024` → `main` | `741a1be900` | Wave 11 ancestry |
| #171 | closed (draft) | `feature/wave7a-star-covenant-20261024` → `integration/accessory-system-20261023` | `b6a03f4e5a` | Wave 11 ancestry |
| #172 | closed (draft) | `integration/wave8-wave7-repairs-20261025` → `main` | `cb6412e204` | Wave 11 ancestry |
| #173 | closed (draft) | `feature/wave7b-snow-charm-20261025` → `integration/accessory-system-20261023` | `e72be04070` | Wave 11 ancestry |
| #174 | closed (draft) | `feature/wave7b-accessory-abilities-20261025` → `integration/accessory-system-20261023` | `91faed1092` | Wave 11 ancestry |
| #175 | closed (draft) | `claude/wave-7b-decisions-reconciliation-xjdmva` → `feature/wave7b-accessory-abilities-20261025` | `0fb34bc512` | Wave 11 ancestry |
| #176 | closed (draft) | `integration/wave9-accessory-abilities-20261025` → `main` | `decb3efa19` | Wave 11 ancestry |
| #177 | closed (draft) | `test/bc03-validation-hardening-20261025` → `codex/bc03-bounty-contract-20261022` | `cc058692ca` | Wave 11 ancestry |
| #178 | closed (draft) | `feature/bc03-approved-reconciliation-20261025` → `test/bc03-validation-hardening-20261025` | `35c97ff413` | Wave 11 ancestry |
| #179 | merged (draft) | `fix/qm01-quest-monument-test-isolation` → `integration/wave9-accessory-abilities-20261025` | `baa06dc4ae` | Wave 11 ancestry |
| #180 | closed (draft) | `fix/bc04-contract-structure-names-20261025` → `feature/bc03-approved-reconciliation-20261025` | `f3604c4593` | Wave 11 ancestry |
| #181 | open (draft) | `release/wave9-rc-20261025` → `main` | `268ec65bb8` | remote branch |
| #182 | open (draft) | `integration/wave10-unified-20261025` → `main` | `13c190d1ab` | Wave 11 ancestry |
| #183 | merged | `docs/ring-life-authority-option-a-20261025` → `release/wave9-rc-20261025` | `bacb9546c9` | remote branch |
| #184 | closed (draft) | `fix/exposure-audit-parallel-20261025` → `main` | `6d456e6fca` | remote branch |
| #185 | merged | `test/ring-life-option-a-regression-20261025` → `release/wave9-rc-20261025` | `efc77967c9` | remote branch |
| #186 | merged (draft) | `security/exposure-audit-pacing-20261025` → `release/wave9-rc-20261025` | `97797b5deb` | remote branch |
| #187 | merged | `test/k5-reused-world-determinism-20261025` → `release/wave9-rc-20261025` | `9f565a664a` | remote branch |
| #188 | closed (draft) | `fix/wave9-live-input-harness-20261026` → `release/wave9-rc-20261025` | `eb88cf8147` | remote branch |
| #189 | open (draft) | `docs/phase2-preservation-decisions-20261026` → `main` | `6f9c3ac2ad` | remote branch |
| #190 | merged (draft) | `audit/prc-p4-research-preservation-20261026` → `checkpoint/wave10-unified-20261026` | `13c190d1ab` | Wave 11 ancestry |
| #191 | merged (draft) | `audit/prc-p6-remaining-preservation-20261027` → `checkpoint/wave10-unified-20261026` | `73dcc8f4d1` | remote branch |
| #192 | open (draft) | `checkpoint/wave11-unified-20261027` → `main` | `c47c69f3e3` | Wave 11 ancestry |

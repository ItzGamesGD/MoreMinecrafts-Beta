# BRANCH-W11-20261027 — Repository-Wide Branch Audit & Wave 11 Checkpoint

## Basis

- Audited remote branches (excluding Wave 11): **144**; total remote branches at refresh: **145**; PRs: **191**, open: **15**.
- Wave 10 at branch creation and PR #182 head: `13c190d1abf0a9dc6936b8dbc5eac50be4e63319`.
- Wave 10 moved externally through PR #191 to `73dcc8f4d139c1688352068b397dab040208253b` during this audit. Wave 11 copied its 90 documentation files by cherry-pick; the existing Wave 10 ref was not modified by this agent. The protected-branch invariance gate therefore cannot be claimed.
- Branches refreshed from `origin` on 2026-10-26 UTC after PR #191 merged. Full machine-readable commit, path, patch-id, blob and PR evidence: `branch_inventory.json`.
- `docs/CONTENT_AUTHORITY_INDEX.md` routes active authority. Historical branch content was not promoted merely because it exists.

## Classification and cleanup

- Classes: {'A': 45, 'B': 1, 'D': 66, 'F': 3, 'G': 29}.
- Cleanup: {'ARCHIVED_IN_CHECKPOINT': 1, 'RETAIN_ACTIVE': 13, 'RETAIN_PROTECTED': 16, 'RETAIN_UNIQUE': 66, 'REVIEW_REQUIRED': 3, 'SAFE_TO_DELETE_CANDIDATE': 45}. No branches deleted.
- D means unique or divergent material remains without sufficient semantic and design verification. F branches remain on their original refs.

## Branch-by-branch classification

| Branch | HEAD | PR | MB | ± Wave 10 | Patch eq/unique | Blob eq/divergent | Class | Cleanup | Evidence |
|---|---|---:|---|---:|---:|---:|---|---|---|
| `audit/bc01-bc02-20261022` | `1269cdbacc` | #150 closed | `1269cdbacc` | +0/−184 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `audit/phase3-stage23-xpquest-readiness-20261023` | `01a36b0caf` | #163 closed | `cde842dd2c` | +1/−189 | 0/1 | 0/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `audit/prc-p4-research-preservation-20261026` | `13c190d1ab` | #190 closed | `13c190d1ab` | +0/−3 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `audit/prc-p5-pre-research-preservation-20261026` | `e00d65cbd8` | — | `e00d65cbd8` | +0/−5 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `audit/prc-p6-pre-preservation-20261027` | `13c190d1ab` | — | `13c190d1ab` | +0/−3 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `audit/prc-p6-remaining-preservation-20261027` | `73dcc8f4d1` | #191 closed | `73dcc8f4d1` | +0/−0 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `audit/repository-cleanup-phase2-20261023` | `c9e29c5680` | #158 closed | `cde842dd2c` | +6/−189 | 0/6 | 0/6 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `audit/repository-cleanup-phase2b-20261023` | `5a0ede4051` | #159 closed | `cde842dd2c` | +6/−189 | 0/6 | 0/6 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `audit/repository-pr-reconciliation-20261022` | `84550ad4ef` | #154 closed | `5f92307555` | +1/−191 | 0/1 | 0/5 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `audit/stage23-pr92-pr93-reconciliation-20261019` | `079d36ac3b` | #125 closed | `5f92307555` | +145/−191 | 0/142 | 350/27 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `checkpoint/wave10-unified-20261026` | `73dcc8f4d1` | — | `73dcc8f4d1` | +0/−0 | 0/0 | 0/0 | G | RETAIN_PROTECTED | ancestor |
| `claude/q1c-skull-provenance-20261019` | `6ba15ac2cb` | #128 open | `5f92307555` | +147/−191 | 0/144 | 343/42 | G | RETAIN_PROTECTED | open/baseline |
| `claude/stage23a-economy-soil-20261017` | `9ec91dc535` | #110 closed | `5f92307555` | +87/−191 | 0/86 | 250/28 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `claude/wave-7b-decisions-reconciliation-xjdmva` | `0fb34bc512` | #175 closed | `0fb34bc512` | +0/−86 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `claude/xp-quest-authority-20261017` | `63107098fc` | #112 closed,#111 closed | `5f92307555` | +96/−191 | 0/95 | 263/31 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `claude/xp-quest-q1-integration` | `c40ee6b165` | #118 open | `5f92307555` | +101/−191 | 0/98 | 279/38 | G | RETAIN_PROTECTED | open/baseline |
| `claude/xp-quest-q1a-core` | `78d1495402` | #114 closed | `5f92307555` | +96/−191 | 0/95 | 266/38 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `claude/xp-quest-q1a2-exact-xp` | `f30d263c93` | #115 closed | `5f92307555` | +97/−191 | 0/96 | 276/31 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `claude/xp-quest-q1b-tracking` | `c7fe7fabc5` | #119 open | `5f92307555` | +103/−191 | 0/100 | 287/44 | G | RETAIN_PROTECTED | open/baseline |
| `codex/accessory-client-acceptance-20261021` | `d7e8effd40` | #145 closed | `d7e8effd40` | +0/−153 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-client-lifecycle-final-20261022` | `6d37a73f11` | #149 closed | `6d37a73f11` | +0/−124 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-client-repair-20261022` | `b7f72fdae4` | #146 closed | `b7f72fdae4` | +0/−144 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-enhancement-20261022` | `f9a31c7117` | #153 closed | `f9a31c7117` | +0/−113 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-infrastructure-20261021` | `9b79f9be03` | #142 closed | `9b79f9be03` | +0/−190 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-inventory-ui-redesign-20261023` | `ed4cc92aa5` | #160 closed | `ed4cc92aa5` | +0/−119 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-lifecycle-qa-20261021` | `2083cf37d1` | #144 closed | `2083cf37d1` | +0/−179 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-persistence-inventory-20261021` | `f2e6e87fe9` | #143 closed | `f2e6e87fe9` | +0/−188 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/accessory-visual-assets-20261022` | `9f6fba5fcc` | #152 closed | `9f6fba5fcc` | +0/−117 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/ancient-city-arena-prototype-20261013` | `e7dc53f9f4` | #83 open | `20121ca7cc` | +5/−563 | 0/5 | 0/22 | G | RETAIN_PROTECTED | open/baseline |
| `codex/bc03-bounty-contract-20261022` | `5ad001b091` | #162 closed,#151 closed | `5ad001b091` | +0/−173 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `codex/q1c-pr128-p1-p2-repair-20261019` | `9c9f225773` | #130 open | `5f92307555` | +154/−191 | 0/151 | 343/43 | G | RETAIN_PROTECTED | open/baseline |
| `codex/q1c-production-readiness-audit-20261020` | `526416b16a` | #139 closed | `5f92307555` | +4/−191 | 0/4 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/q1c-recovery-20261019-079d36a` | `334997d2f8` | #127 open | `5f92307555` | +146/−191 | 0/143 | 349/28 | G | RETAIN_PROTECTED | open/baseline |
| `codex/review-claude-pr110-20261017` | `8f33e72c8a` | #116 closed | `5f92307555` | +88/−191 | 0/87 | 253/25 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage22-research-implementation-20261015` | `53c2374e84` | #89 closed | `5f92307555` | +22/−191 | 0/22 | 25/17 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23-livefix-a-20261016` | `eb80a14806` | #99 closed | `5f92307555` | +48/−191 | 0/48 | 68/27 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23-livefix-b-20261016` | `5682be1071` | #105 closed | `5f92307555` | +82/−191 | 0/81 | 207/31 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23-pr105-bundle-export-20261016` | `c035946fb8` | — | `5f92307555` | +83/−191 | 0/82 | 207/32 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-batch2-20261015` | `68a7fafb4e` | #91 closed | `5f92307555` | +36/−191 | 0/36 | 52/26 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-batch3-20261015` | `c3683de733` | #95 closed | `5f92307555` | +60/−191 | 0/60 | 73/25 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-expanded-batch2-20261017` | `2c8e984314` | #108 closed | `5f92307555` | +83/−191 | 0/82 | 224/29 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-expanded-research-20261015` | `7231c7aad5` | #98 closed | `5f92307555` | +67/−191 | 0/67 | 94/30 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-fisheries-hotfix-20261015` | `df960d6511` | #97 closed | `5f92307555` | +42/−191 | 0/42 | 60/25 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-furnace-infrastructure-20261015` | `f28658e5e0` | #96 closed | `5f92307555` | +66/−191 | 0/66 | 77/34 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-integrated-research-20261015` | `5e48deacbe` | #94 closed | `5f92307555` | +41/−191 | 0/41 | 59/26 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-reconcile-20261015` | `803d2e5df5` | #93 closed | `5f92307555` | +51/−191 | 0/51 | 51/36 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-remaining-research-20261017` | `9376777264` | #109 closed | `5f92307555` | +86/−191 | 0/85 | 237/27 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-research-content-20261015` | `1edbdb6bc6` | #90 closed | `5f92307555` | +30/−191 | 0/30 | 38/24 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23a-research-implementation-20261015` | `f632f4f521` | #92 closed | `5f92307555` | +33/−191 | 0/33 | 25/28 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23b-cloud-client-qa-design` | `9b66f49657` | #106 closed | `5f92307555` | +84/−191 | 0/83 | 207/32 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/stage23b-cloud-live-client-qa` | `dad054c7cb` | #107 open | `5f92307555` | +137/−191 | 0/136 | 207/36 | G | RETAIN_PROTECTED | open/baseline |
| `codex/xp-quest-200-content-audit-20261020` | `3e66cfbdfa` | #140 closed | `5f92307555` | +26/−191 | 0/26 | 8/0 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/xp-quest-q1b-correction` | `fd4aa83113` | #120 closed | `5f92307555` | +108/−191 | 0/105 | 296/38 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/xp-quest-q1c` | `17e980912d` | #121 closed | `5f92307555` | +139/−191 | 0/136 | 344/32 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `codex/xp-quest-ui-integration-20261020` | `a124ccfc2e` | — | `5f92307555` | +2/−191 | 0/2 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/bounty-contract-approved-20261022` | `b7cedf199a` | #148 closed | `b7cedf199a` | +0/−186 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `design/pillager-headquarters-concept-20261022` | `55df673a83` | #147 closed | `5f92307555` | +15/−191 | 0/15 | 2/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/pyramid-mob-roster-20261026` | `a86c6d5beb` | — | `cde842dd2c` | +1/−189 | 0/1 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/spider-colony-biome-20261026` | `f75638670f` | — | `cde842dd2c` | +39/−189 | 0/39 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/stage23a-remaining-approval-20261017` | `67d6c1fdd1` | — | `5f92307555` | +88/−191 | 0/87 | 237/28 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-d79-d80-reconcile-20261020` | `d641075f20` | #136 closed | `5f92307555` | +39/−191 | 0/39 | 14/19 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-d81-d84-facility-prep-20261021` | `f2cc4f06db` | #137 closed | `5f92307555` | +41/−191 | 0/41 | 14/20 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-economy-bounty-approved-20261019` | `befa4c8a44` | #131 closed | `5f92307555` | +25/−191 | 0/25 | 14/15 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-owner-decisions-d01-d78-20261020` | `09b33f39f6` | #133 closed | `5f92307555` | +27/−191 | 0/27 | 14/19 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-owner-decisions-reconcile-20261020` | `e0428763cf` | #134 closed | `5f92307555` | +28/−191 | 0/28 | 14/19 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-registration-recovery-d2a-20261019` | `a1e0aeb0ab` | #132 closed | `5f92307555` | +26/−191 | 0/26 | 14/18 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/territory-shelved-independent-content-20261021` | `284ce62ff5` | #138 closed | `5f92307555` | +44/−191 | 0/44 | 14/21 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `design/xp-quest-selector-redesign-20261019` | `a9b89755f7` | #126 open | `5f92307555` | +156/−191 | 0/153 | 352/30 | G | RETAIN_PROTECTED | open/baseline |
| `design/xp-quest-structure-200-20261019` | `2176935929` | #129 open | `5f92307555` | +160/−191 | 0/157 | 364/33 | G | RETAIN_PROTECTED | open/baseline |
| `design/xp-quest-usability-d1-d5-20261019` | `9a246f1e0c` | #135 closed | `5f92307555` | +172/−191 | 0/169 | 377/28 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/2026-10-07-content-first-axis-review` | `8bf157f798` | #71 closed | `1b600b7755` | +15/−811 | 0/15 | 0/3 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/accessory-enhancement-approved-20261017` | `32cbca0a3f` | #113 closed | `5f92307555` | +88/−191 | 0/87 | 250/29 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/alchemy-plot-notes` | `cc1a96b062` | — | `327b7ef1bf` | +1/−694 | 0/1 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/phase2-preservation-decisions-20261026` | `6f9c3ac2ad` | #189 open | `cde842dd2c` | +5/−189 | 5/0 | 24/4 | G | RETAIN_PROTECTED | open/baseline |
| `docs/ring-life-authority-option-a-20261025` | `bacb9546c9` | #183 closed | `decb3efa19` | +10/−39 | 5/3 | 15/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/stage23-accessories-approved-20261016` | `e532fa3034` | #101 closed | `5f92307555` | +68/−191 | 0/68 | 95/30 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/ui-piglin-mob-tracker-20261010` | `6fe1a308c9` | — | `e1d3cb3538` | +1/−530 | 0/1 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/wave4b-custom-quest-ui-authority-20261024` | `2e847d4956` | #164 closed | `cde842dd2c` | +3/−189 | 0/3 | 2/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `docs/world-events-patrol-design-20261018` | `ad77805971` | #122 closed | `5f92307555` | +93/−191 | 0/92 | 250/32 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `experiment/cloud-free-model-delegation-phase0` | `aa376713ae` | #141 open | `5f92307555` | +1/−191 | 0/1 | 0/5 | G | RETAIN_PROTECTED | open/baseline |
| `experiment/ponytail-skill-2-20261021` | `f864df45ef` | — | `5f92307555` | +4/−191 | 0/4 | 0/4 | F | REVIEW_REQUIRED | prototype |
| `experiment/underground-kingdom-worldgen-probe` | `991b20c8a0` | #117 open | `5f92307555` | +77/−191 | 0/76 | 202/44 | G | RETAIN_PROTECTED | open/baseline |
| `feature/bc03-approved-reconciliation-20261025` | `35c97ff413` | #178 closed | `35c97ff413` | +0/−170 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `feature/wave7a-star-covenant-20261024` | `b6a03f4e5a` | #171 closed | `b6a03f4e5a` | +0/−88 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `feature/wave7b-accessory-abilities-20261025` | `91faed1092` | #174 closed | `91faed1092` | +0/−87 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `feature/wave7b-snow-charm-20261025` | `e72be04070` | #173 closed | `e72be04070` | +0/−88 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `fix/bc04-contract-structure-names-20261025` | `f3604c4593` | #180 closed | `f3604c4593` | +0/−169 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `fix/exposure-audit-parallel-20261025` | `6d456e6fca` | #184 closed | `cde842dd2c` | +1/−189 | 1/0 | 0/1 | B | ARCHIVED_IN_CHECKPOINT | PR #182 verified equivalent |
| `fix/qm01-quest-monument-test-isolation` | `baa06dc4ae` | #179 closed | `baa06dc4ae` | +0/−43 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `fix/wave9-live-input-harness-20261026` | `eb88cf8147` | #188 closed | `decb3efa19` | +25/−37 | 9/9 | 19/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/accessory-bounty-20261023` | `63efc3ca0a` | — | `63efc3ca0a` | +0/−70 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `integration/accessory-system-20261023` | `0e9ccc5af9` | #161 closed | `0e9ccc5af9` | +0/−89 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `integration/batch-a-recovery-20261022` | `05a378211a` | #156 closed | `5f92307555` | +149/−191 | 0/146 | 405/27 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/q1b-villager-discount-pr116-20261019` | `4fbb036e01` | #123 closed | `5f92307555` | +112/−191 | 0/109 | 300/34 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/q1c-pr116-discount-20261019` | `5ea5f54614` | #124 closed | `5f92307555` | +143/−191 | 0/140 | 348/28 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/stage23-final-reconciliation-20261016` | `bdffa599e1` | #103 closed | `5f92307555` | +76/−191 | 0/75 | 202/29 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/stage23-linear-chain-20261016` | `7231c7aad5` | #100 closed | `5f92307555` | +67/−191 | 0/67 | 94/30 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/stage23-mobs-accessories-20261016` | `b0de6b7548` | #104 closed | `5f92307555` | +75/−191 | 0/75 | 202/29 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/stage23-reconciliation-audit-20261016` | `601cc28b9d` | #102 closed | `5f92307555` | +69/−191 | 0/69 | 94/31 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `integration/wave10-unified-20261025` | `13c190d1ab` | #182 open | `13c190d1ab` | +0/−3 | 0/0 | 0/0 | G | RETAIN_PROTECTED | ancestor |
| `integration/wave5r-combined-20261024` | `a33d03ba1c` | #165 closed | `a33d03ba1c` | +0/−68 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `integration/wave8-wave7-repairs-20261025` | `cb6412e204` | #172 closed | `cb6412e204` | +0/−55 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `integration/wave9-accessory-abilities-20261025` | `decb3efa19` | #176 closed | `decb3efa19` | +0/−42 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `main` | `cde842dd2c` | — | `cde842dd2c` | +0/−189 | 0/0 | 0/0 | G | RETAIN_PROTECTED | ancestor |
| `prototype/stage20-vanilla-sweep-texture` | `b1830ce606` | — | `20121ca7cc` | +1/−563 | 0/1 | 0/9 | F | REVIEW_REQUIRED | prototype |
| `prototype/vanilla-sweep-texture` | `fe9d441f57` | — | `6cd671bb79` | +2/−810 | 0/2 | 0/9 | F | REVIEW_REQUIRED | prototype |
| `recovery/vanilla-model-custom-mob-roster-base` | `cf4360b228` | — | `25eede9fce` | +1/−586 | 0/1 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `release/wave9-rc-20261025` | `268ec65bb8` | #181 open | `decb3efa19` | +44/−37 | 11/23 | 20/2 | G | RETAIN_PROTECTED | open/baseline |
| `resources/40-custom-mob-production-textures` | `0cc0a9eaf5` | #72 closed | `20121ca7cc` | +4/−563 | 0/4 | 90/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `security/exposure-audit-pacing-20261025` | `97797b5deb` | #186 closed | `decb3efa19` | +35/−37 | 11/15 | 19/3 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `security/gitleaks-historical-findings-20261022` | `b2f2f8f9bf` | #155 closed | `b2f2f8f9bf` | +0/−190 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `security/gitleaks-remediation-20261022-codex-725159` | `cce99b6cc7` | #157 closed | `cde842dd2c` | +4/−189 | 0/3 | 0/1 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `stage-1/fabric-foundation` | `ab5e2a5790` | #1 closed | `ab5e2a5790` | +0/−916 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-10/draft-content` | `a2acc715ad` | — | `a2acc715ad` | +0/−793 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-11/checkpoint` | `3cb27187e3` | — | `3cb27187e3` | +0/−779 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-12/checkpoint` | `a8b6299f16` | — | `50b142206c` | +7/−813 | 0/6 | 63/74 | G | RETAIN_ACTIVE | open/baseline |
| `stage-13/checkpoint` | `94a264c2c6` | — | `50b142206c` | +20/−813 | 0/16 | 66/78 | G | RETAIN_ACTIVE | open/baseline |
| `stage-14/checkpoint` | `94a264c2c6` | — | `50b142206c` | +20/−813 | 0/16 | 66/78 | G | RETAIN_ACTIVE | open/baseline |
| `stage-15/checkpoint` | `c39313e7d1` | — | `c39313e7d1` | +0/−754 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-16/checkpoint` | `70e470deb2` | — | `70e470deb2` | +0/−725 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-17/checkpoint` | `d8493c62bb` | — | `d8493c62bb` | +0/−705 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-18/checkpoint` | `93d264a8f7` | — | `93d264a8f7` | +0/−620 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-18/integration` | `e4e99c36c1` | — | `e4e99c36c1` | +0/−697 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-19/checkpoint` | `984418d302` | — | `984418d302` | +0/−610 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-2/enhancement-stone` | `6ae717c92d` | #3 closed | `6ae717c92d` | +0/−912 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-20/checkpoint` | `20121ca7cc` | — | `20121ca7cc` | +0/−563 | 0/0 | 0/0 | G | RETAIN_ACTIVE | ancestor |
| `stage-21/knowledge-checkpoint` | `c0ce63cb73` | — | `c0ce63cb73` | +0/−192 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-22/research-restructure` | `d65f590593` | #88 closed | `5f92307555` | +7/−191 | 0/7 | 2/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `stage-3/enhancement-vertical-slice` | `1f05e53fba` | #5 closed | `1f05e53fba` | +0/−906 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-4/equipment-foundation` | `8a3159c5a3` | #11 closed | `8a3159c5a3` | +0/−874 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-5/special-equipment` | `e66bf7ecb3` | #14 closed | `e66bf7ecb3` | +0/−862 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-6/materials-crafting-progression` | `c8705f4c39` | #16 closed | `c8705f4c39` | +0/−847 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-7/life-content` | `a62a65e10c` | #18 closed | `a62a65e10c` | +0/−840 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-8/entity-mob` | `5541ad505a` | #21 closed | `5541ad505a` | +0/−833 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-9/skeleton-king` | `ec1b6ce918` | #23 closed | `ec1b6ce918` | +0/−823 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `stage-ci/accessory-bounty-20261023` | `63efc3ca0a` | — | `63efc3ca0a` | +0/−70 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `test/bc03-validation-hardening-20261025` | `cc058692ca` | #177 closed | `cc058692ca` | +0/−172 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `test/k5-reused-world-determinism-20261025` | `9f565a664a` | #187 closed | `decb3efa19` | +22/−37 | 8/8 | 17/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `test/ring-life-option-a-regression-20261025` | `efc77967c9` | #185 closed | `decb3efa19` | +13/−39 | 6/4 | 16/4 | D | RETAIN_UNIQUE | unique/divergent; hold |
| `tooling/integration-gametest-coverage-20261024` | `741a1be900` | #170 closed | `741a1be900` | +0/−188 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `wave7/a-e-real-mouse-input` | `0f466b96df` | #168 closed | `0f466b96df` | +0/−63 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `wave7/b-gametest-recovery` | `e5982f3c2f` | #166 closed | `e5982f3c2f` | +0/−67 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `wave7/c-accessory-raw-xp` | `06be9f53ff` | #167 closed | `06be9f53ff` | +0/−67 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |
| `wave7/d-ui-cleanup` | `8f53b48816` | #169 closed | `8f53b48816` | +0/−66 | 0/0 | 0/0 | A | SAFE_TO_DELETE_CANDIDATE | ancestor |

## Unique changes and dependencies
- Every branch’s full unique commit list, changed path list, differing blob paths and stable patch IDs are in `branch_inventory.json`.
- Parent edges below are inferred only when a named branch HEAD is an ancestor of the child HEAD; branches may also share an unnamed merge-base. No cycle exists among ancestry edges.
- `security/gitleaks-historical-findings-20261022` → `audit/phase3-stage23-xpquest-readiness-20261023`
- `security/gitleaks-historical-findings-20261022` → `audit/repository-cleanup-phase2-20261023`
- `security/gitleaks-historical-findings-20261022` → `audit/repository-cleanup-phase2b-20261023`
- `stage-21/knowledge-checkpoint` → `audit/repository-pr-reconciliation-20261022`
- `integration/q1c-pr116-discount-20261019` → `audit/stage23-pr92-pr93-reconciliation-20261019`
- `codex/q1c-recovery-20261019-079d36a` → `claude/q1c-skull-provenance-20261019`
- `codex/stage23a-remaining-research-20261017` → `claude/stage23a-economy-soil-20261017`
- `claude/stage23a-economy-soil-20261017` → `claude/xp-quest-authority-20261017`
- `claude/xp-quest-q1a2-exact-xp` → `claude/xp-quest-q1-integration`
- `claude/stage23a-economy-soil-20261017` → `claude/xp-quest-q1a-core`
- `claude/stage23a-economy-soil-20261017` → `claude/xp-quest-q1a2-exact-xp`
- `claude/xp-quest-q1-integration` → `claude/xp-quest-q1b-tracking`
- `stage-20/checkpoint` → `codex/ancient-city-arena-prototype-20261013`
- `claude/q1c-skull-provenance-20261019` → `codex/q1c-pr128-p1-p2-repair-20261019`
- `stage-21/knowledge-checkpoint` → `codex/q1c-production-readiness-audit-20261020`
- `codex/xp-quest-q1c` → `codex/q1c-recovery-20261019-079d36a`
- `claude/stage23a-economy-soil-20261017` → `codex/review-claude-pr110-20261017`
- `stage-22/research-restructure` → `codex/stage22-research-implementation-20261015`
- `codex/stage23a-integrated-research-20261015` → `codex/stage23-livefix-a-20261016`
- `codex/stage23a-expanded-research-20261015, integration/stage23-linear-chain-20261016` → `codex/stage23-livefix-b-20261016`
- `codex/stage23-livefix-b-20261016` → `codex/stage23-pr105-bundle-export-20261016`
- `codex/stage23a-research-content-20261015` → `codex/stage23a-batch2-20261015`
- `codex/stage23a-integrated-research-20261015` → `codex/stage23a-batch3-20261015`
- `codex/stage23-livefix-b-20261016` → `codex/stage23a-expanded-batch2-20261017`
- `codex/stage23a-furnace-infrastructure-20261015` → `codex/stage23a-expanded-research-20261015`
- `codex/stage23a-integrated-research-20261015` → `codex/stage23a-fisheries-hotfix-20261015`
- `codex/stage23a-batch3-20261015` → `codex/stage23a-furnace-infrastructure-20261015`
- `stage-21/knowledge-checkpoint` → `codex/stage23a-integrated-research-20261015`
- `stage-21/knowledge-checkpoint` → `codex/stage23a-reconcile-20261015`
- `codex/stage23a-expanded-batch2-20261017` → `codex/stage23a-remaining-research-20261017`
- `codex/stage22-research-implementation-20261015` → `codex/stage23a-research-content-20261015`
- `stage-21/knowledge-checkpoint` → `codex/stage23a-research-implementation-20261015`
- `codex/stage23-livefix-b-20261016` → `codex/stage23b-cloud-client-qa-design`
- `codex/stage23-livefix-b-20261016` → `codex/stage23b-cloud-live-client-qa`
- `stage-21/knowledge-checkpoint` → `codex/xp-quest-200-content-audit-20261020`
- `claude/xp-quest-q1b-tracking` → `codex/xp-quest-q1b-correction`
- `codex/xp-quest-q1b-correction` → `codex/xp-quest-q1c`
- `stage-21/knowledge-checkpoint` → `codex/xp-quest-ui-integration-20261020`
- `stage-21/knowledge-checkpoint` → `design/pillager-headquarters-concept-20261022`
- `security/gitleaks-historical-findings-20261022` → `design/pyramid-mob-roster-20261026`
- `security/gitleaks-historical-findings-20261022` → `design/spider-colony-biome-20261026`
- `codex/stage23a-remaining-research-20261017` → `design/stage23a-remaining-approval-20261017`
- `design/territory-owner-decisions-reconcile-20261020` → `design/territory-d79-d80-reconcile-20261020`
- `design/territory-d79-d80-reconcile-20261020` → `design/territory-d81-d84-facility-prep-20261021`
- `stage-21/knowledge-checkpoint` → `design/territory-economy-bounty-approved-20261019`
- `design/territory-registration-recovery-d2a-20261019` → `design/territory-owner-decisions-d01-d78-20261020`
- `design/territory-registration-recovery-d2a-20261019` → `design/territory-owner-decisions-reconcile-20261020`
- `design/territory-economy-bounty-approved-20261019` → `design/territory-registration-recovery-d2a-20261019`
- `design/territory-d81-d84-facility-prep-20261021` → `design/territory-shelved-independent-content-20261021`
- `codex/xp-quest-q1c` → `design/xp-quest-selector-redesign-20261019`
- `design/xp-quest-selector-redesign-20261019` → `design/xp-quest-structure-200-20261019`
- `design/xp-quest-structure-200-20261019` → `design/xp-quest-usability-d1-d5-20261019`
- `stage-9/skeleton-king` → `docs/2026-10-07-content-first-axis-review`
- `claude/stage23a-economy-soil-20261017` → `docs/accessory-enhancement-approved-20261017`
- `stage-18/integration` → `docs/alchemy-plot-notes`
- `security/gitleaks-historical-findings-20261022` → `docs/phase2-preservation-decisions-20261026`
- `integration/wave9-accessory-abilities-20261025` → `docs/ring-life-authority-option-a-20261025`
- `codex/stage23a-expanded-research-20261015, integration/stage23-linear-chain-20261016` → `docs/stage23-accessories-approved-20261016`
- `stage-20/checkpoint` → `docs/ui-piglin-mob-tracker-20261010`
- `security/gitleaks-historical-findings-20261022` → `docs/wave4b-custom-quest-ui-authority-20261024`
- `docs/accessory-enhancement-approved-20261017` → `docs/world-events-patrol-design-20261018`
- `stage-21/knowledge-checkpoint` → `experiment/cloud-free-model-delegation-phase0`
- `stage-21/knowledge-checkpoint` → `experiment/ponytail-skill-2-20261021`
- `integration/stage23-final-reconciliation-20261016` → `experiment/underground-kingdom-worldgen-probe`
- `security/gitleaks-historical-findings-20261022` → `fix/exposure-audit-parallel-20261025`
- `test/k5-reused-world-determinism-20261025` → `fix/wave9-live-input-harness-20261026`
- `audit/stage23-pr92-pr93-reconciliation-20261019` → `integration/batch-a-recovery-20261022`
- `codex/xp-quest-q1b-correction` → `integration/q1b-villager-discount-pr116-20261019`
- `codex/xp-quest-q1c` → `integration/q1c-pr116-discount-20261019`
- `integration/stage23-mobs-accessories-20261016` → `integration/stage23-final-reconciliation-20261016`
- `codex/stage23a-furnace-infrastructure-20261015` → `integration/stage23-linear-chain-20261016`
- `codex/stage23a-expanded-research-20261015, integration/stage23-linear-chain-20261016` → `integration/stage23-mobs-accessories-20261016`
- `codex/stage23a-expanded-research-20261015, integration/stage23-linear-chain-20261016` → `integration/stage23-reconciliation-audit-20261016`
- `stage-20/checkpoint` → `prototype/stage20-vanilla-sweep-texture`
- `stage-9/skeleton-king` → `prototype/vanilla-sweep-texture`
- `stage-19/checkpoint` → `recovery/vanilla-model-custom-mob-roster-base`
- `security/exposure-audit-pacing-20261025` → `release/wave9-rc-20261025`
- `stage-20/checkpoint` → `resources/40-custom-mob-production-textures`
- `test/k5-reused-world-determinism-20261025` → `security/exposure-audit-pacing-20261025`
- `security/gitleaks-historical-findings-20261022` → `security/gitleaks-remediation-20261022-codex-725159`
- `stage-9/skeleton-king` → `stage-12/checkpoint`
- `stage-12/checkpoint` → `stage-13/checkpoint`
- `stage-12/checkpoint` → `stage-14/checkpoint`
- `stage-21/knowledge-checkpoint` → `stage-22/research-restructure`
- `test/ring-life-option-a-regression-20261025` → `test/k5-reused-world-determinism-20261025`
- `integration/wave9-accessory-abilities-20261025` → `test/ring-life-option-a-regression-20261025`

## Semantic comparison and holds

- Exact ancestry proves class A. `fix/exposure-audit-parallel-20261025` has no patch-unique commit; PR #182 documents its inclusion through the exposure audit chain.
- D and F branches retain their original refs. Many older research/XP Quest/economy branches diverge from Wave 10 on source files and require per-feature review against current authority; they are not deletion candidates.
- Open PR heads, main, release candidates and checkpoints are G. PR #181, #182 and #189 remain open. PR #191 merged into Wave 10; its branch and its pre-preservation parent remain retained.
- Unique source tests were not copied into runtime because API compatibility and behavior still require review.

| Area reviewed | Authority or evidence | Wave 11 disposition |
|---|---|---|
| Research, knowledge, XP Quest | `CONTENT_AUTHORITY_INDEX.md` V4 and UI routes; Q1C and Stage 23 branches diverge | Wave 10 runtime retained; unique branch code held |
| Accessories, artifacts, bounty, economy | Current index and PR #182 integration record; territory designs on separate branches | Wave 10 runtime retained; unlinked designs historical |
| Dungeons, structures, mobs, world generation | `STRUCTURE_AUTHORITY_INDEX.md` and explicit prototype branches | Prototype code held; documents archived |
| Inventory, GUI, saving, death and synchronization | Current UI and utility authority routes; branch inventory paths | No unreviewed runtime replay |
| Client/server GameTests, security, build and CI | Source test paths, workflow paths and PR #182 checks | Missing tests archived as historical; existing build and CI retained |

There is no evidence sufficient to assign any whole divergent branch class C or E. This conservative result is a hold for later per-feature review, not a claim that every divergent feature is obsolete.

## Preserved documents and tests

- `preservation_manifest.json` records **82** verbatim document blob variants across **62** paths and **7** test blob variants across **6** paths, each with original branch, HEAD, path, blob SHA, reason, implementation relationship, historical status and SHA-256. Raw files live under `preserved/`. Historical preservation grants no gameplay or design approval.
- Unique source tests remain reachable on their original branches and are archived as raw evidence. Their API compatibility has not been validated; none was claimed as a passing Wave 11 runtime test.
- PR #191 merged 90 documentation files into Wave 10. The same endpoint blobs were copied into Wave 11 without changing current gameplay authority.

## Integration and verification

- Integration: document/test preservation, audit record, PR #191’s three documentation commits (90 files), and exact `.gitleaksignore` fingerprints for an already reviewed historical prose false positive recreated by archival. No gameplay source branch was replayed.
- Final verification results and Wave 11 SHA are recorded in the PR #182 appendage after execution.

## Follow-up

- Review D branches by subsystem against routed authority; inspect sibling implementations and source tests before integrating code.
- Resolve F experiments independently if an owner selects a proposal.
- Reassess deletion candidates only after explicit owner review; this audit deletes nothing.

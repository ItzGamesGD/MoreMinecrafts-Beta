# XP Quest 200-entry content-design audit — 2026-10-20

**Status:** Independent design review proposal. No catalog, gameplay, UI, localization or approved reward was changed. This document and its companion CSV are evidence for owner decisions, not authority to activate quests.

## Read this audit

- [Complete 200-row inventory](quest_inventory.csv): exact current catalog fields, A-proposal Korean names/objectives, available runtime copy, tracker payload, dependency, per-row attainability/pacing/repetition/reward/retroactivity notes, labels, approval provenance and recommendation.
- [Registered 75 attention list](REGISTERED_ATTENTION.md): focused source/design mismatches and live gates.
- [Unregistered 125 readiness register](UNREGISTERED_READINESS.md): every ID classified with a reason and next action.
- [Economy, pacing, redundancy and dependencies](ECONOMY_DEPENDENCIES.md): ledger totals, tracker coverage, overlap clusters and UI cross-check.
- [Read-only inventory validator](validate_inventory.py): compares CSV against the pinned Q1-C catalog.
- [Continuation checkpoint](../XP_QUEST_200_CONTENT_AUDIT_CHECKPOINT_2026-10-20.md): branch and publication state.

## Evidence hierarchy and exact refs

| Source | Ref / blob | Role and limit |
|---|---|---|
| `main` | `5f92307555ce5c486a2e0e8091f956bcbb6c6646` | Verified production baseline. It does not contain the Q1-C gameplay catalog. |
| Q1-C branch / PR #121 | `17e980912d636b0572a3f25b24995ffde31c754c` | Current implementation source; 139 commits ahead of main, merge-base main. PR open Draft. |
| Q1-C `catalog.json` | Git blob `95ddc0bf15d381e6694dd49ec774f4d04650e48c` | Authoritative **current data snapshot** for this audit: all 200 IDs/statuses/trackers/targets/rewards. Current data is not individual owner approval. |
| Authority index on Q1-C | blob `afe727e77289068612dc77358422e6eb87977a26` | Routes approved XP Quest system behavior and aggregate reward scope. |
| UI XP Quest amendment / PR #112 | PR head `63107098fcde6742704a07394439e5cdf9c6ad49`; blob `c3b4c3ba922c6c0485feda2c0cf4485d49580875` | Owner-approved claim, exact XP, 180,000 active + 29,100 FUTURE target, VERIFY registration gate and policies. PR open Draft. |
| Catalog A on Q1-C | blob `7a490801087a28f91d1ce2116de210a819e656f2` | Korean **proposal** titles/objectives, visual antecedents and retroactivity claims. It explicitly says individual quests await review. Its status summary is stale for three ecology entries. |
| XP Quest handoff on Q1-C | blob `9553550170c9388c1e0fad4992467723c835705d` | Detailed owner-locked tracking policies, implementation plan and older 160/20 status snapshot; not proof of current registration. |
| Q1C_PROGRESS on Q1-C | blob `ae7bdd25cb4d467fde5a68e7ea9238f035d89c27` | Reported tests/source behavior and ecology final checkpoint; internally inconsistent historical paragraphs. |
| Q1-C evaluator/tracking | blobs `ba046b386676518d0566915365ea6ab605aae3b7`, `a7c9a1dacd2eaa55dea96996380bdefc81648d02` | Implemented reconciliation and registration gate. Source review is distinct from live testing. |
| Q1-C catalog test | blob `b99e9fcb3ba219ab99de9af10765f63556f91f32` | Exact ledger/roster assertions in source; this audit did not rerun the project suite. |
| PR #139 readiness audit | head `526416b16a590abf558534a8426625508c23ad99`; report blob `b7c2b41b2b53ea63fbb202eb516ca83f25f42f7a` | Independent technical readiness, CI and live-gap inventory. PR open Draft. |
| PR #126 selector | head `a9b89755f7c4e924243fa4229a90177433712be4` | Draft UI card/navigation; visual owner acceptance pending. |
| PR #129 structure | head `2176935929c990a210fae64582d524d30993bc8a`; mapping blob `bc53b3013fe8f7bdbe6809ff6459a9894f55dc7b` | Proposed 200-node/15-chapter/52-sub-branch presentation only. Its 24 presentation edges and 2 disabled gameplay candidates are not prerequisites. |
| PR #135 D1–D5 | head `9a246f1e0cef5e3f39c4bf47b34df6e63877df30` | Draft zoom, labels, layout, icons, input behavior; does not validate quest content. |
| Q1-C EN/KO lang | blobs `adcf1ff16fb67ceb7e802a9e9c4f531422407622`, `2a71232eb9adc71603834e2d9ebeac1a18b4ebc7` | Only three quests have all title/description/objective fields in each language. The other 197 are marked MISSING in the CSV. |

PR #112, #126, #129, #135 and #139 were refreshed as **open Draft** on 2026-10-20. PR #121 is also open Draft. Draft state, green CI, catalog NOW, and `registered=true` do not establish individual design approval or live acceptance.

## Inventory result and design assessment

The exact Q1-C JSON yields **200 distinct IDs**: NOW **163** (159,000 XP), VERIFY **17** (21,000 XP), FUTURE **20** (29,100 XP); registered **75** (73,850 XP), unregistered **125** (135,250 XP). Active NOW+VERIFY is **180 quests and 180,000 XP**; the complete ledger is **209,100 XP**. Of the NOW rows, **88 remain unregistered**. All registered rows are NOW and use non-pending trackers. All **77 pending** trackers are unregistered. These totals reconcile to the CSV and the catalog's aggregate target.

The distribution is late weighted: ENDGAME contains 25 quests and 82,850 XP (39.6% of all raw XP), while EARLY plus EARLY_MID contains 28 quests and 7,800 XP (3.7%). This can support an escalating curve, but the practical balance is not established: a 3,750 XP quest is nearly twice the approved 1,900 XP +0→+20 Enhancement cost. Growth and equipment together hold 41 quests/63,900 XP with **zero registered**, so the currently playable 75-quest mix is narrower than the 200-entry plan. Farming, fishing, crafting and alchemy likewise have only 3, 3, 2 and 1 registrations.

The catalog promotes exploration, combat, progression, gathering and production, but several count ladders share a single source and can auto-complete together. The exact clusters and incentives are in the economy/dependency map. No measured completion times exist; high-count targets are *risk hypotheses*, not measured defects. Full survival access remains unverified for all live worlds. Existing-world retroactivity is particularly important where the source is a vanilla stat, an old Knowledge observation, or an install-forward event.

## Contradictions and gaps requiring explicit reconciliation

1. **Ecology status/document conflict:** `xpq.cmb.cave_dwellers`, `xpq.cmb.bestiary_25`, `xpq.cmb.habitats_5` are NOW/registered in current JSON, yet catalog A and parts of Q1C_PROGRESS still say VERIFY/unregistered. The progress document's ecology final checkpoint claims owner approval and 75 registrations, while its later audit paragraph again says no promotion. Keep all claims attributed; obtain an exact owner confirmation before editing authority or gameplay.
2. **Structure retroactivity mismatch:** `xpq.exp.villages_5`, `xpq.exp.structures_15`, `xpq.exp.grand_expedition` are `R` in catalog A, but Q1-C safely uses forward-only piece entry because old Knowledge keys recorded proximity. This implements the reliable-evidence policy, yet the catalog's player expectation is stale.
3. **Biome title/objective mismatch:** `xpq.exp.biomes_all_overworld` says Overworld in the ID/title while catalog A describes all naturally generated Overworld, Nether and End biomes. The exact 26.3 roster is not frozen in the current tracker (`pending`). Keep VERIFY.
4. **Presentation versus gameplay:** #129 maps all IDs without field drift, but its two gameplay candidates are disabled. #135 preview usability does not make 197 missing localized quests or 125 unregistered objectives playable.
5. **Approval scope:** PR #112 explicitly approves the **aggregate reward budget** and system behavior; catalog A explicitly describes individual content as pending review. No per-quest reward approval is inferred from the ledger, automated tests or catalog flags.

## Prioritized owner decision queue

Each proposed change is a recommendation only; **explicit owner approval is required** for all changes to content, status, tracker semantics, reward or authority. No row below has been executed.

| Priority / type | Quest ID | Current value or behavior | Evidence and observed problem | Proposed alternative and expected play impact | Implementation implication | Recommendation |
|---|---|---|---|---|---|---|
| P0 critical contradiction | `xpq.cmb.cave_dwellers`, `xpq.cmb.bestiary_25`, `xpq.cmb.habitats_5` | NOW, registered; A says VERIFY | Q1C_PROGRESS has conflicting snapshots and an owner-approval claim. | Confirm exact ecology roster/status and five habitat groups; align history and player copy. Clarifies what the 75 baseline means. | Documentation/authority reconciliation first; gameplay only if owner changes design. | KEEP pending decision |
| P1 critical contradiction | `xpq.exp.villages_5`, `xpq.exp.structures_15`, `xpq.exp.grand_expedition` | A says full retroactivity; code is forward-only | Proximity keys could falsely award entry. | Retain forward-only and disclose install-forward credit; avoids false completion. | Copy/documentation and old-save acceptance; any import would need a new migration design. | KEEP |
| P1 important progression | `xpq.exp.biomes_all_overworld` | VERIFY; target 1 pending; name implies Overworld | A objective spans three dimensions; no fixed set in tracker. | Approve exact biome roster and accurate name, then verify natural obtainability; reduces hidden requirements. | Frozen versioned set, evaluator, tests, EN/KO copy. | MODIFY / DEFER |
| P1 important progression | `xpq.boss.monument_clear`, `xpq.boss.monuments_3` | VERIFY/unregistered despite automated monument tests | Participation and distinct-site behavior need natural-world multiplayer acceptance. | Keep gate until owner accepts live evidence and reward/tier timing; protects co-op fairness. | Dedicated-server and old-save tests; no activation here. | DEFER |
| P1 important balance | `xpq.cmb.hostile_5000`, `xpq.min.ore_20000`, `xpq.frm.harvest_5000`, `xpq.bld.place_10000` | Large count ladders, current rewards unchanged | Same actions can be farmed repeatedly; natural timing unmeasured. | Measure ordinary survival progress and farming rate; owner then keeps targets or requests a distinct objective. | Telemetry/test protocol and possible future tracker design. | KEEP pending evidence |
| P1 important economy | `xpq.eqp.enhance_20_armor`, `xpq.eqp.all_t2_cores`, `xpq.grw.knowledge_complete` | 3,750 XP each; unregistered | Different effort/cost paths pay the same high amount; 3,750 exceeds full +20 enhancement cost. | Model net XP and claim timing under scenario D before changing values. | Economy model only now; later catalog edit requires owner approval and exact total reconciliation. | DEFER |
| P2 optional | `xpq.exp.rare_biomes`, `xpq.exp.travel_250k` | NOW/unregistered, rare-biome set and 250,000 travel target | World seed/travel variance and repetition may be high. | Verify ordinary survival routes and progress feedback; owner may retain if endgame stretch goals. | Natural-world sampling and copy. | KEEP pending evidence |
| P2 optional | `xpq.grw.knowledge_complete`, `xpq.grw.discipline_complete` | ENDGAME, unregistered | Completion depends on Knowledge exclusions and Research maximal-level roster remaining stable. | Freeze exact eligible node sets and show denominator; makes goal legible. | Versioned roster/evaluator and localization. | MODIFY / DEFER |
| P2 missing information | `xpq.exp.explorer_maps`, `xpq.cft.recipes_200`, `xpq.frm.mending_book` | NOW with `pending` trackers | Acquisition/provenance or recipe-ID event is unspecified in executable data. | Specify server evidence and retroactivity; avoids false or missed credit. | New hooks, positive/negative attribution and restart tests. | DEFER |
| P2 missing information | `xpq.fsh.giant_catch`, `xpq.min.vein_500`, `xpq.grw.throwable_pickaxe` | VERIFY, `pending` | Feature/event and threshold semantics are unverified. | Resolve mechanics and survival access before revisiting status. | Feature-specific probes/tests; no roster promotion. | DEFER |

No replacement quests are proposed to preserve the number 200. Content-dependent FUTURE rows stay FUTURE.

## Suggested design-review sequence

1. Owner reviews this CSV by the 12 tabs, beginning with the 75 baseline. Confirm the three ecology statuses and the safe structure-entry retroactivity policy; record decisions in routed authority, not only this audit.
2. Review the 88 unregistered NOW entries by readiness class. Resolve pending tracker evidence before any implementation schedule; confirm exact objective, source attribution, tier and raw XP per ID.
3. Review 17 VERIFY entries by factual/game-mechanics question. Keep the registration gate until evidence and explicit owner decision exist.
4. Review 20 FUTURE entries against feature roadmaps and natural Survival access. Do not promise or activate unavailable content.
5. Run a natural-world and existing-save playtest on the accepted design candidates, including multiplayer credit and claim timing. Compare actual effort and XP inflow to scenario D, then decide any target/reward changes with aggregate-budget reconciliation.
6. Review #129/#135 presentation and the missing 197 EN/KO quest copies after objective text is accepted. Keep presentation links separate from gameplay prerequisites.

## Validation and limits

Read-only parsing checked 200 unique IDs and row-level equality for ID, tab, tier, status, registered, tracker kind/payload, target and raw XP. The 200 A-catalog XP values match current JSON exactly; its three ecology statuses do not. PR #129 mapping also matches all 200 current category/tier/status/registration/XP fields. The inventory has explicit `MISSING` runtime copy for 197 quests in both languages. Every issue and owner decision in this report names exact quest IDs. The published validator permits replay against a checkout of the pinned Q1-C catalog.

Project tests: **NOT RUN** (documentation-only audit; execution environment unavailable). Live gameplay verification: **PENDING**. The prior source/CI findings are cited as historical evidence, not tests performed by this audit.

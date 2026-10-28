# Staged integration plan — approval required before execution

This is a proposal, not an implemented integration. Main remains `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. No existing PR/branch was merged, closed, deleted, rebased or force-pushed. This audit adds reports only. The owner must authorize the next task before any production extraction, repair, authority amendment or integration begins.

## Recommended first batch and minimum branch set

Create **one new playable-client candidate branch** from pinned main after approval, proposed name `integration/playable-progression-20261022`. Use the verified cumulative #125 core as the foundation, selected #126/#135 presentation material as donors, current #140 decisions as constraints, and complete missing already-approved navigation/claim behavior. Do not call the candidate playable or accepted until the real interaction and persistence gates below pass.

This is substantial: `main..#125` changes **377 paths**, 145 head-only commits; #135 is 172 commits ahead of main. An “XP UI-only merge” would disguise Research, furnace, crafting, mob-resource and Quest dependencies. Approval must explicitly include that cumulative scope. Using one known cumulative donor avoids replaying every historical PR; it does not dispense with scope review, unresolved content gates or semantic validation.

A second branch, proposed `integration/approved-content-followup-20261022`, is optional **after** the first candidate is accepted. It would add accessories, then independently gated contracts/arena batches as separate reversible commits. Its baseline must be the actual accepted first-candidate SHA, recorded before creation; that SHA does not yet exist and is not invented here. Do not create separate integration branches per historical PR or run all optional batches automatically. Existing source PRs remain intact as evidence/rollback sources.

No production branch is created during this audit. The separate Draft audit PR contains only these five reports.

## Exact donor ledger

All SHAs below are immutable planning inputs; future execution must re-fetch current state and stop to re-audit any changed donor. A proposed future result SHA is not available until execution and must be pinned at each gate.

| Source | Exact HEAD | Intended use |
|---|---|---|
| main | `5f92307555ce5c486a2e0e8091f956bcbb6c6646` | Initial target baseline, already includes Knowledge #87 |
| #125 | `079d36ac3bd2d8950118e30647d5f2efad19f947` | Cumulative Research/Live-Fix/Q1-C/pricing/codec foundation |
| #126 | `a9b89755f7c4e924243fa4229a90177433712be4` | Selector redesign, focus/keyboard, bounded tree layout donor |
| #135 | `9a246f1e0cef5e3f39c4bf47b34df6e63877df30` | Selected presentation fixes and regression tests, never blanket 200 activation |
| #140 | `3e66cfbdfa6a12576ecf0162108250691318c8db` | Latest content/recipe decisions, especially A/M/N/O/P; documentation only |
| #93 | `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955` | Missing event-boundary test donor only |
| #116 | `8f33e72c8a71d29e132050457535ac8a0092a7b0` | Correct pricing-description source; four code/test blobs already in #125 |
| #107 | `dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6` | Optional reusable rendered-client QA infrastructure, six own paths |
| #130 | `9c9f22577370cd0160dd2b43b0e92caa7832f420` | Optional skull/P2 repair; P1 crash acceptance remains BLOCKED |
| #153 | `f9a31c7117198550c0d5b984b4019af6f164cd94` | Cumulative accessory equipment/QA/enhancement donor |
| #152 | `f0dfef9e88b3a284aee9ba751dd71e9847c92271` | Approved-identity twelve accessory asset sets, validator/workflow, separate sibling of #153 |
| #151 | `30b5c076554111bf660fe3dca67cde4be735776a` | Cumulative contracts authority/audit/runtime/crash tests, currently blocked |
| #83 | `e7dc53f9f48dfece8a5723debd5526dc061107d1` | Optional additive arena prototype |
| #138 | `284ce62ff54247cc0a4e96693c34c190b49c4bbf` | Preserve SHELVED territory scope only |
| #122 | `ad778059718b0a1b911264b72818bbaca8026fcd` | Patrol design reference only |
| #147 | `849c7393e92eb18822b19426aab51ae875c00f32` | Latest HQ design reference only |

#72 is not an additional donor: #104's `b0de6b754859e090427ff310887ca04188310a8a`, actually merged in #103, already retains its production resources in #125/#135. #98 and #100 have the same HEAD. #139 and `a124ccfc2e7e3bc826c04705fa98f69a102a2636` are audit checkpoints, not runtime sources.

## Batch A — progression candidate, one branch with reviewable substeps

### A0: freeze scope and authority manifest

Target baseline: exact main above. Sources: #125, #135, #140 and #138 above; read main/donor authority indexes and routed documents before extracting anything. Write the execution task's per-feature/per-quest retention manifest as part of that authorized task, pinned to these sources. These audit reports are findings, not new design authority.

Keep existing Knowledge/Research rules, IDs, costs, prerequisites and save keys. Carry current explicit XP activation/exact-raw-XP/claim rules. Preserve #140's 200-slot replacement policy, frozen fishing review and approved utility recipe decisions as design status; do not implement missing recipes, replacement objectives or FUTURE activation merely by copying those documents. Preserve territory shelving and make any retained older active-looking routing unambiguous. Do not reopen its D01–D84 design.

Default UI plan follows the already-approved synthesized vanilla `AdvancementsScreen` contract. The custom #135 canvas is a donor for separable presentation fixes/tests, not automatically accepted as the final screen. If the owner wants it retained, obtain an explicit authority amendment before implementing that choice. Confirm the initial release roster and any exact event predicates still missing from #140 before enabling affected quests. Source catalog count75 does not constitute content approval.

Required checks: retained authority routes have no conflict or accidental promotion; list all 377 cumulative paths and identify production, tooling, approved authority and proposal/test-only content before the first extraction commit. Rollback: abandon the new candidate only; source branches and main untouched. Gate: owner-approved scope and resolved in-scope authority requirements.

### A1: retain the verified cumulative implementation foundation

Initial target: main, then A0's pinned candidate commit. Source: **#125 exact HEAD**, not a sequence of historical PR merges. Retain the cumulative approved Research/Knowledge/Quest implementation dependency set and its associated tests/resources. This includes seven-root graph/migration, Stage23 selected effects, current furnace persistence, K5/K14 semantics, XP catalog/evidence/raw-XP/claim endpoint, #124 pricing repairs, #125 codec test, #104 mob resources and Live-Fix A model/validator/test. Keep current main history and security/workflow protections.

Primary retained paths/domains: `progression/`, associated research/Quest hooks under `mixin/`, `network/`, `registry/ModRegistries.java`, Knowledge/Research client routing/canvas/data, furnace/crafting/fishing/equipment adapters required by this cumulative tree, catalog/layout/localization data, custom-mob renderer/state/texture resources, GameTest/test registrations and validators. This is a cumulative foundation, not a claim every research node has a runtime handler; the 43 DATA_ONLY /42 PARTIAL /2 BLOCKED audit remains explicit.

Omit superseded alternate #92/#93 runtime helpers/mixins; omit replays of #97/#99/#116/#123/#124/#72 and their older shared-file versions; omit #107 QA unless selected in A3; omit skull #127/#128/#130; omit territory, contracts, accessory runtime, kingdom/sweep/delegation experiments and unapproved equipment expansion. An intermediate custom Quest route may exist in donor history, but must not be promoted as final acceptance before A2 resolves the approved screen contract. No unregistered Quest becomes eligible.

Expected conflicts: main is an ancestor of #125 at the snapshot, so there is no divergent main/donor history to infer a textual conflict from. Report-only audit commits may be applied separately. Selective extraction/omission still needs semantic registration review: shared `MoreMinecraftsClient`, progression attachment/sync, mixin JSON, catalog loader, localization, GameTest metadata and RawXp/anvil boundaries. Never replace these with older whole files merely because a patch applies cleanly.

Required tests: Java25 clean build/unit/GameTests; exact catalog+registration invariants; graph/save migration; K5/K14; Fisheries exact-fifths/codec; furnace owner/content/fuel/progress/restart; trading/Soil; ore positive provenance; shared XP transactions; common/client init and server readiness. Live criteria: Knowledge/Research routes function, actual purchases charge once under correct rules, no new model/resource errors, preexisting progression survives. Rollback: revert/disable the candidate's A1 commit series only before promotion; restore disposable copied saves from the pre-batch backup if migration was exercised. Never downgrade a valuable mutated save in place.

### A2: finish approved screen navigation and XP interaction

Target: pinned successful A1 candidate commit. Donor heads: #126/#135 above; compare bounded #125..#126 and #129..#135 changes rather than merging #135's full ancestry again. #129 exact HEAD is `2176935929c990a210fae64582d524d30993bc8a`; its proposal/preview fixtures are not production content authority.

Retain #126 selector card assets, normalized hit testing, focus/keyboard traversal and Back/Close routes where compatible with approved visuals. Knowledge and Research continue through the existing seven-sector picker and branch direct-tree navigation. Reuse #135 applicable tooltip clipping/placement, focus/zoom regression knowledge and icons only within the authorized final screen design. If vanilla remains the contract, adapt applicable work and omit the custom Quest canvas replacement; this is additional engineering, not a merge-ready donor.

Complete the already-approved behavior absent from donors: click completed/unclaimed node dispatches the existing server claim request; waiting/action-result state cannot send duplicate payouts; unachieved/completed-unclaimed/claimed indicators; completion toast without XP; authoritative exact-XP receipt/rollback; UI refresh after revisioned response; appropriate Back/Close/focus behavior. This work is **not started by this audit** and needs next-task execution authorization, not a new gameplay invention.

Do not activate the 200-node synthetic preview in normal play, invent localization/objectives, enable candidate prerequisite edges or display unavailable states without approval. Do not silently ship the stale three Ecology definitions: #140 section A requires cave_bug, actual biome-qualified habitat kills and bestiary natural-availability checks. Use an explicitly approved initial roster; any predicates requiring materially different gameplay outcomes remain owner gates. Maintain registration/claim rejection for unregistered IDs and preserve credible save evidence without reinterpreting species/distance/proximity counters as new objectives.

Expected conflicts: `ProgressionSelectionScreen`, `ProgressionBrowseScreen`, `ProgressionTreeCanvas`, `QuestTreeStructure/Layout`, client progression API/network refresh, layout/localization/tests. #135 changes36 paths cumulatively relative to #125; only the selected compatible subset is retained. The existing claim service is not replaced with automatic XP-on-completion. Neither the three-node production view nor a 200-node screenshot is sufficient acceptance.

Required tests: actual screen class transitions by mouse and keyboard, pointer scale/frame-edge hit bounds, vanilla-screen data isolation from ordinary Advancements, duplicate/stale claim packets, registration denial, failure rollback, exact totalExperience/level/points, two players, completion vs claim vs repeat, relog/death/dimension/restart. Live criteria: all four cards open their intended screens through real clicks, Knowledge remains free/non-purchase, Research purchases once, a legitimately completed released Quest shows unclaimed, grants exactly its XP once, then remains claimed after restart. Rollback: keep A1 pinned; revert A2 only on the candidate, retain backup saves and do not erase granted/claimed records to simulate success.

### A3: restore unique validation and inspect resources

Target: successful A2 pinned candidate. Sources: #93 test methods, #116 corrected descriptions and optionally #107 QA harness. Restore only #93 actual movement/orb boundary cases (`051c1a7869a588fad8a72ebfbb7d8942fae6eab3`, `e72e598800106026e6c1b26e2f7204cbc2130d62`, `1d9081f1dda9c59fde7c36b17b78972931e59100`); adapt to chosen helpers rather than restoring duplicate runtime hooks. Preserve #125 codec case.

Synchronize just the stale pricing sentences in `RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md` and `RESEARCH_STAGE23A_REMAINING_CAMPAIGN_MANIFEST_2026-10-17.md` against actual ceiling-discount code; source historical test statements by their original SHA rather than claiming a new result. #107 own paths are QA workflow, `build.gradle`, clientQa source/mod metadata, verifier script and QA plan. Its shared baseline with #135 has no two-sided divergent-path overlap at this snapshot, but later accessory build changes will require composition. Re-run on the actual combined candidate; do not inherit old acceptance.

Keep Flame Sword corrected model/validator/test together and immutable PNG unchanged. Preserve mob renderer and all companion-layer hooks with the 78 PNGs. Carry Large Backpack icon FAIL as an unresolved visual defect; source history contains no transparency fix to cherry-pick. Rendering repair, if added to the next-task scope, requires independent diagnosis and visual acceptance; do not substitute passing storage tests.

Required tests/live criteria: sprint/swim hook, exact 4/6-block collection boundaries, Fortune/Silk Touch/Auto Smelt/Lumbering/Soil/beehive/fishing interactions, enchanting viewport, Greatsword hit/miss sweep, Flame Sword front/back/first/third-person/ground views, large Backpack icon at all GUI scales and storage conservation, resource reload and missing models, 40-mob/state-layer review. Rollback: revert QA adaptation separately; keep production model changes reversible as a matched model/validator/test unit, with approved art immutable.

**Promotion gate for Batch A:** all required automation succeeds on the final candidate SHA; physical navigation/claim/restart acceptance is recorded; any allowed visual deferral is explicitly named, including whether the unresolved Backpack defect blocks this candidate. Main merge remains a separate owner action even after these gates pass. A candidate with missing claim interaction must never be labeled the integrated playable solution.

## Batch B — optional accessory equipment/enhancement

Target baseline: accepted Batch A result, exact SHA to be recorded before creating the second branch. Source #153 above already contains #142→#143→#144→#145→#146→#149 and the exact #113/#101 authority blobs. Do not replay those PRs or merge the old #113 research branch merely for a document. #152 now supplies the twelve approved-identity asset sets and must be considered as a separate resource donor. Its docs-only title/body are stale; identities and 32x32 size are approved, final rendered appearance remains pending.

Retain five full-stack equipment slots, persistent/synced attachment, menu/death behavior, real lifecycle/restart/client fixtures, growth arrays and component-only deterministic enhancement/catalyst slot transactions. Retain #152 item definitions/models/textures and resource validator/build check with its current authority route; preserve deferred effect/activation/catalyst-art/acquisition scope; do not invent passive adapters or treat `valueAt` arrays as functioning effects. Synchronize durable task-approved slot/death/layout authority when authorized, without changing the approved rules. Compose ordinary + accessory anvil charges with Batch A's exact-raw-XP semantics.

Before acceptance diagnose/test Creative full-inventory catalyst close conservation, rollback at every mutation boundary and actual catalyst-slot UI; do not call the likely loss repaired until reproduced and verified. Accessory models/art are present on #152 and absent on #153; compose them and validate the rendered result. Catalyst models/art remain absent and a visual gate. Owner must choose whether a deliberately incomplete visual/effect slice is an acceptable test candidate; no production completeness claim is proposed.

Expected conflicts: pinned #135/#153 have **11 shared divergent paths**, including `build.gradle`, K5 restart workflow, authority index/relic doc, `T1AnvilScreen`, `AnvilMenuMixin`, `ModRegistries`, GameTest metadata, mixin JSON and both languages. Also compose the #152 sibling asset validator/build hook, authority route and client workflow; no gameplay effect is added by its art. Conflict counts are changed-path intersections since merge base, not executed merge results. Compose registrations and transactions, not whole-file replacements. `ModItems` also needs union with later bounty registry work.

Required tests/live criteria: all unit/GameTests + three-process persistence; equip/unequip/shift transfer, metadata/enchant/durability/component preservation, death/keepInventory, dimensions/reconnect/two players, rejected category/duplicate equipment, GUI scale/recipe book/Backpack regressions, real burst input under latency, +0→+10 boundaries, insufficient materials/XP, duplicate clicks, ordinary equipment costs and Creative/full-inventory close. Historical #149 per-click barrier tests are useful but do not replace burst testing. Rollback: revert B commits on optional branch; restore copied saves if testing introduced attachment/component versions; no main/source mutation.

## Batch C — optional bounty/contracts, independently blocked

Target: successful accepted A+B result if both chosen, otherwise accepted A with explicit absence of accessory dependency; pin that target SHA before work. Source #151 above includes #148 authority and #150 audit/simulation. Do not replay their main-target PRs separately. Source payout simulations remain non-authoritative.

Retain approved physical contract data/ledger/actions, source materialization, combat/structure/delivery hooks, trade/loot acquisition, independent emerald/material economy, crash matrix and regressions. Repair confirmed exploration and fallback kill-band defects under existing rules. Establish owner-approved profession weighting, effort/value validation, provisional distribution/restock choices needed for release; do not fill them from a simulation or silently change reward scope. Assess the documented overflow receipt crash gap; retain explicit process-halt versus power-loss limits. A mailbox is not preselected.

Expected conflicts: #135/#151 share **8 divergent paths**: authority index, client initialization, GameTest metadata, `ModDataComponents`, `ModRegistries`, mixin JSON and both languages. #153/#151 share **7**, including `ModItems`. Also inspect semantic composition of `ModNetworking` and XP hooks even where only one side edits the file. Keep contracts repeatable/physical and independent of one-time Quest raw-XP rewards; do not introduce accessory/research rewards or territory dependencies.

Required tests/live criteria: Advanced/Special exploration 2–3; End fallback target count by actual strength; objective source/profession/economic invariants; accept/abandon/complete with slot+ID revalidation, stale/duplicate packets, all matching contracts, assistance attribution, true structure events, delivery costs/overflow, seller stock/restock, multiplayer handoff/death/restart; re-run actual two-JVM crash matrix in disposable worlds plus a real rendered-client contract UI/physical mouse pass. Full-history check failure must be understood/cleared by the appropriate security work, without unreviewed ignore-list suppression. Rollback: revert only contracts batch and restore disposable pre-contract worlds/ledgers; do not orphan redeemed IDs by dropping ledger state in a valuable save.

## Batch D — optional arena prototype promotion

Target: explicitly selected accepted candidate SHA, recorded first. Source #83 above only, using its own delta from stage20 rather than a whole old baseline. Retain arena NBT `4620400d63d81aeb2d2da6b7c773590874037216`, additive pool definition/mixin, generator/validator, narrow registration hooks and opt-in probe/capture tests. Preserve vanilla pool entries/fallback and placement. Do not add kingdom, HQ/patrol, territory, Ancient Heart/Warden or loot/progression redesigns. Weight1 alongside 46 is connector-candidate context, not a per-city2.13% guarantee.

Expected conflicts: #135/#83 share four divergent paths: common/client initializers, GameTest mod metadata, mixin JSON. Preserve all existing registrations while adding the narrow pool contribution. Required tests: NBT reproducibility/connectivity/bounds/entrances, actual eligible generation seeds, additive pool probabilities, cities both with/without arena, no terrain edits/forced placement in acceptance; native visual review of floor/seating/columns/water/terrain. Rollback: remove candidate's additive pool/code in a revert; existing generated terrain is not undone by a code revert, so use disposable worlds or backups and disclose that limit. No prototype promotion is authorized now.

## Work explicitly omitted

- No blind merge sweep, PR closure/deletion, force push, rebase or trusted-checkpoint movement.
- No territory implementation/reactivation, including registration/protection/facility tiers/material deposits. #138 is decisive despite stale historical routing.
- No kingdom feasibility probe, global vanilla sweep-texture override, free-model delegation or experimental skill installation in production.
- No automatic activation of 200 Quest rows, candidate gameplay prerequisites, all fishing quests, new utility recipes/FUTURE quests, replacement objectives, accessory effects/catalyst art, patrols or HQ generator. #152 accessory art itself has approved identities and is a selected optional resource donor, with final appearance pending. Existing approval and implementation are separate gates.
- No skull #130 in first batch; default green tests do not clear its deliberately opt-in forced-halt failure. A later approved repair task must prove the accepted receipt/provenance guarantee without shipping an unverified journal.
- No independent #72/#92/#94/#97/#99/#100/#103/#116/#123/etc replay after selecting their retained cumulative content. Dispositions are recommendations only.

## Final verification protocol for any authorized candidate

Record candidate SHA, Java25/toolchain, Minecraft/Fabric versions, loaded mod/JAR SHA256, OS/GPU, test-world identity, single/dedicated server and artifact paths. Do not trust branch names or an old `build/libs` artifact. Use clean build output, check the actual loaded mod, and preserve the owner-reproduced Windows path as an acceptance platform.

| Gate | Required execution/evidence | Current result in this audit |
|---|---|---|
| Clean build/unit suite | Java25 `./gradlew clean test build compileGametestJava runGameTest` under normal dependencies; inspect test failures/counts, not just process startup | NOT RUN locally; donor exact-head remote runs are linked in inventory |
| GameTests | All required tests + selected regression/event/transaction tests with fixtures active and fresh world | NOT RUN on combined tree |
| Dedicated server smoke | Registry/common initialization, readiness, no datapack/registry errors, join from actual candidate client | NOT RUN |
| Client smoke/resources | Client initialization, resource reload/item atlas, no missing texture/model/decode errors, actual world load | NOT RUN |
| Physical navigation | Advancement key→four cards→correct screen/sector/tree; mouse press/release at center/edge/disabled zone, GUI scales1–4, resize, Back/Esc; keyboard traversal | NOT RUN / VISUAL PENDING |
| XP end-to-end | Legitimate objective, unclaimed state+toast, zero XP on completion, exact reward on one click, repeat/duplicate/stale rejection, two-player isolation | BLOCKED by known missing donor UI; NOT RUN |
| Persistence/sync | Complete/unclaimed survive death/relog/restart; claimed survives and cannot repay; revision-gap recovery, dimensions, disconnect/reconnect | NOT RUN |
| Research/Knowledge | Current seven sectors/branches, free Knowledge, one-click paid Research, material/raw-XP/prerequisite revalidation, K5/K14/graph migration | NOT RUN on combined tree |
| Rendering | Flame model faces and immutable PNG, mob companion layers, Backpack opacity, selector/tree hit bounds/readability, accessory/catalyst resources if selected | VISUAL PENDING; Backpack historical FAIL remains |
| Accessory regression | Full-stack equipment/sync/death/restart, anvil rollback/conservation, physical menu interactions if B chosen | NOT RUN on combined tree |
| Contract/arena optional | Applicable C/D tests and real interaction/generation gates only if chosen | BLOCKED / NOT RUN |
| Final remote CI | All relevant workflows on final exact candidate SHA, not only its parent; investigate independent failed checks | NOT RUN; candidate does not exist |

Use CODE VERIFIED for source or specified automation evidence, LIVE VERIFIED only for actual recorded runtime behavior, VISUAL PENDING for unreviewed appearance/input, BLOCKED for unmet gates, NOT RUN for unexecuted checks. Never turn donor CI success into live acceptance, disable a failing required fixture to claim success, or use direct Screen callbacks as proof of physical mouse interaction. No Java/build dependency repair or gameplay fix was attempted in this audit.

After each accepted substep, retain its commit as the rollback point, not a moved trusted checkpoint. Before any later main promotion, the owner reviews the final feature manifest, exact SHA, CI and live ledger. Source PR closure/deletion needs its own explicit authorization; it is not implied by approving an integration task.

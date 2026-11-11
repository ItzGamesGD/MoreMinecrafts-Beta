# Grand unified integration manifest — 2026-11-11

**Status: CANDIDATE** (reproducible combined tree; not VERIFIED). Open items are listed under *Remaining blockers*.

This manifest extends [UNIFIED_20261111_MANIFEST.md](UNIFIED_20261111_MANIFEST.md) (the earlier candidate on `integration/unified-20261111`). It records the batches added in the grand-integration pass, the audit of every remote branch, and what was preserved or deliberately left out. Full branch classification: [GRAND_UNIFIED_20261111_BRANCH_INVENTORY.tsv](GRAND_UNIFIED_20261111_BRANCH_INVENTORY.tsv) (151 remote branches, computed against integration head `f8bd02e7`, before this manifest commit).

## Refs

| Ref | SHA | Notes |
|---|---|---|
| `integration/unified-20261111` | see *Integration head* below | the one continuing integration branch; fast-forward only |
| `checkpoint/wave16-unified-20261104` | `65072bca` | integration base (unchanged) |
| `checkpoint/unified-20261111-candidate-1` | `0743aa74` | previous candidate, unchanged |
| `checkpoint/unified-20261111-candidate-2` | `11946339` | previous candidate, unchanged |
| `checkpoint/grand-unified-20261111-candidate-1` | integration head after this manifest | new immutable candidate ref (created in this pass) |
| `main` | `cde842dd` | not modified |

PRs: #273 (integration → `main`, Draft, not merged), #274 (ST-01 P8-C3-A research → integration, Draft, docs only, not merged).

## Batches added in this pass (in order)

Each batch is a `--no-ff` merge on `integration/unified-20261111`. No history was rewritten.

| # | Source branch @ tip | Result | Classification | Conflicts and resolution |
|---|---|---|---|---|
| G1 | `feat/st03-implement-01-20261105` @ `7c9520a1` | `3242170e` | DOCUMENTATION_ONLY (ST-03 Q01–Q42 ledger and Q106–Q108 status snapshot) | none |
| G2 | `design/t1-accessories-d01-owner-decisions-20261030` @ `59ad0bbb` | `0294a13d` | DOCUMENTATION_ONLY (D01 Q1–Q108 record) | add/add on `docs/planning/T1_ACCESSORIES_D01_OWNER_DECISIONS_2026-10-30.md`. The two versions differ only by one trailing space on line 20. Kept the integration version. |
| G3 | `docs/content-design-roadmap-wave15-20261030` @ `fdaeac6c` | `e5e8110a` | DOCUMENTATION_ONLY (roadmap and D13 Q-records; keeps the commit pinned by the D13 provenance links reachable) | none |
| G4 | `claude/sky-tower-phase-2-validation-twragg` @ `d12a6e11` (includes `validation/sky-tower-mountain-height` @ `038eaef6`) | `2c764e02` | DOCUMENTATION (Sky Tower mountain-height audit evidence) **plus a diagnostic GameTest probe** `SkyTowerPhase2Probe`. Not docs-only as first assumed. | `src/gametest/resources/fabric.mod.json`: union of entrypoints (both sides kept). |
| G5 | `audit/st01-p8c2b-counterfactual-rarity-20261111` @ `569fc789` | merged as `2a1c23a0`; **reverted by `01bf033d`** | REQUIRES_ISOLATION | `fabric.mod.json`: union, de-duplicated. The merge failed to compile: `St01CounterfactualProbe` uses `St01PlacementConfig.FLUID_MARGIN`, `UNDERGROUND_VOLUME`, `ENTRANCE_HALF_WIDTH` and `St01Eligibility.Volume`, which the Policy D tree does not have. The probe was not rewritten: it is historical measurement evidence. The report doc was already present in integration. The probe, tools and workflow remain only on the source branch. |
| — | routing commit | `6474a024` | docs only | routes G1–G4 and the paused list in `CONTENT_AUTHORITY_INDEX.md` (no implementation claimed) |
| G6 | `implement/d13-01-relic-catalyst-20261104` @ `23e9be7e` | `f8bd02e7` | ALREADY_INTEGRATED (was PARTIAL_BUT_INTEGRABLE) | clean automatic merge. Adds the relic_catalyst recipe, `tags/item/t1_accessories.json`, `RelicCatalystRecipeResourceTest` and `RelicCatalystRecipeGameTests`. This is the catalyst implementation the authority index says to preserve (PR #228 line). |

Integration head after G6: `f8bd02e7`. The routing commit `6474a024` and the revert `01bf033d` sit before it.

## Validation of the integration head (`f8bd02e7`)

Command: `JAVA_HOME=/opt/jdk25/jdk-25.0.5+8 ./gradlew clean test build compileGametestJava runGameTest --no-daemon`

| Step | Result |
|---|---|
| `compileJava`, `compileClientJava`, `compileTestJava`, `compileGametestJava` | PASS |
| `test` (unit) | **PASS**: 685 tests, 0 failures, 0 errors, 0 skipped (from `build/test-results/test/*.xml`) |
| `build` | **PASS**, BUILD SUCCESSFUL in 5m 11s |
| `runGameTest` (server GameTests) | **PASS**: "All 580 required tests passed" (the earlier candidate ran 576; the +4 are the D13-01 recipe tests) |
| Earlier failure on the counterfactual merge (`2a1c23a0`) | FAIL, compile error (see G5). Fixed by revert, **not** by changing the probe. |

Not run in this pass: client GameTests (`runClientGameTest`), the `st01-p8c2c-policy-d` 20-seed matrix, the `st01-p8c2a-assembly` workflow, the Policy D validation script, and the remote CI. Remote CI runs on the pushed head and is recorded below when available.

## Remote CI on the pushed head

*Pending at the time this manifest was written.* Record the CI run IDs here after the push. Expected carry-overs from the earlier candidate, not yet re-observed on this head:

* `client-visual-validation`: bestiary count 37 hard-coded; wave16 registers the Tomb Guardian and Gladiator. Inherited baseline defect, unchanged.
* `full-history-secret-scan`: gitleaks flags commit `cec8bff8`, reachable only from `audit/st05-po25-review-package-20261102` and `probe/st05-p0-probe-01`. Repository-wide, unchanged.

## Included design records (routed, not implemented)

Routing in [CONTENT_AUTHORITY_INDEX.md](../CONTENT_AUTHORITY_INDEX.md) section *Grand integration 2026-11-11*:

* ST-03 Q01–Q42 owner-decision ledger (implementation coordinates named in the record as an external repository; owner to confirm authority status).
* D01 Q1–Q108 accessory record (partial design; Q39 deferred).
* Wave15 content inventory and roadmap (a proposal, not an approval).
* Sky Tower mountain worldgen height audit (evidence).

## Preserved, not integrated (with reasons)

Every branch below keeps its ref; nothing was deleted, closed or force-pushed.

**PAUSED_BY_OWNER — mob track (9 branches). Mob development is not resumed.**

| Branch | Tip |
|---|---|
| `docs/sky-mob-p0-authority-20261106` | `ec026c05` |
| `implement/sky-mob-p1-entities-20261106` | `ccc57547` |
| `claude/sky-mob-gametest-timing-nn7o1h` | `9a54ae0b` |
| `feature/sky-mob-p2b-cloud-wraith` | `58efa1ff` |
| `claude/sky-mob-p2b-p2c-vwuur8` | `55884b22` |
| `claude/sky-mob-p2d-crystal-bug` | `1683772e` |
| `claude/sky-mob-p2e1-r2-cloud-creature` | `8893c1ff` |
| `claude/sky-mob-p2f1-blood-seraph` | `dcd93ba5` |
| `work/sky-mob-tower-integration-20261111` | `bd5ba43f` |

**BLOCKED — ST-01 P7–P11 and O-14 line (10 branches).** A separate secret-entrance and gold-guide-golem runtime, branched from the P6 tree, with its own design authority under `docs/design/st01-p7`–`p11`. Its runtime is not reconciled with Policy D, and integrating it would add gameplay not routed in the authority index. Owner decision needed on whether it is to be integrated, and under which authority.

| Branch | Tip |
|---|---|
| `work/st01-p7-secret-entrance-state-20261107` | `5610683d` |
| `work/st01-p8-dummy-prototype-20261107` | `633005e8` |
| `work/st01-p9-victory-split-20261108` | `eecf09b5` |
| `work/st01-p10-real-tomb-golem-20261108` | `1449ce50` |
| `work/st01-p11-prod-config-o9-audit-20261108` | `c1f63fa3` |
| `work/st01-p11r-o9-gametest-fix-20261109` | `6d7856b4` |
| `study/st01-o14-worldgen-feasibility-20261109` | `58ee24b6` |
| `work/st01-o14-phase1-phase2-20261109` | `d432a9c5` |
| `work/st01-o14-phase3-1-validation-20261109` | `7e37b786` |
| `work/st01-o14-phase3-calibration-20261109` | `d6753339` |

**REQUIRES_ISOLATION — ST-05 and pre-wave-16 lines (base not in integration).** Rebasing onto the wave16 lineage or an owner decision is needed first. Includes the PRC security audits (`audit/prc-p7*`, base `d341a065`), the ST-05 design, probe and validation branches (`*st05*`), and the counterfactual audit probe (`audit/st01-p8c2b-counterfactual-rarity-20261111` @ `569fc789`, G5).

**SUPERSEDED_BUT_PRESERVED**

* `implement/d13-03-i04a-star-covenant-vault-logic-20261106` (`df673dfe`): logic and test already in integration; only comments differ.
* `claude/colosseum-deterministic-clock-20261107`: superseded by the D13 deterministic-clock `ColosseumGameTests` chosen in conflict C1 of the earlier candidate.
* `claude/st01-pyramid-phase4-worldgen-roke2l` (`ada10ba3`): Phase 4 worldgen registration superseded by the Policy D production pipeline. Not integrated, to avoid a second worldgen registration for the same structure.
* 30 branches in total in this class, including the main-lineage branches (no common ancestor with the wave16 checkpoint lineage: `main`, `stage-*`, `release/wave9-rc-*`, `design/spider-colony-biome-*`, and similar). They are historical and kept unchanged.

**HISTORICAL_CHECKPOINT** — 37 wave, stage and integration checkpoint refs. Kept as they are. None is moved.

**DOCUMENTATION_ONLY, not merged** — `research/st01-p8c3a-terrain-adaptation-20261111` @ `357fc936` (PR #274). Awaits owner decision on merging it into integration.

## Remaining blockers and owner decisions

1. **Remote CI on the new head** is pending at the time of writing (see above). The two earlier red checks are still expected.
2. **Bestiary count in `client-visual-validation`.** The harness hard-codes 37 species and bosses are registered. Changing the gameplay definition of a bestiary species needs owner approval; not changed.
3. **Secret scan (`cec8bff8`).** Remediation options are for the owner: delete the two source branches (not done), rewrite them (not done, and forbidden by policy), or accept the finding for the scope. Not changed.
4. **ST-01 P7–P11 / O-14 secret-entrance line.** Owner decision on integration and authority (see BLOCKED).
5. **Counterfactual audit probe.** Needs re-basing on the Policy D API if it is to be re-run. Owner or follow-up task.
6. **ST-03 and D01 authority status.** Owner to confirm the ledgers are current authority, not merely recorded design (see routing).
7. **Policy B (ST-01 P8-C3-A).** CONDITIONAL_GO; gates G1–G7 open; not implemented (see [PR #274](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/274)).
8. **Royal-chest GameTest timing.** One intermittent failure earlier, passed on rerun and locally; root cause not investigated.

## Main merge readiness

**NOT READY.** Blockers 1–3 above, plus the absence of an owner merge authorization. `main` shares no ancestor with this lineage, so PR #273 shows the whole tree as its diff. Merging needs an explicit owner decision.

## Google Drive snapshot plan (not executed)

Not uploaded (`GOOGLE_DRIVE_UPLOADED: NO`). Plan for later: after an owner-approved merge into `main` and a verification run, export one archive of the tree at a single commit, record the commit SHA in the archive's name and in a `SNAPSHOT.txt`, and verify the archive's SHA-256. GitHub remains the authoritative history source. A Git bundle of the relevant refs is the only way to keep offline history, and it should be evaluated separately.

## Post-integration development backlog (not started)

* Resolve blockers 1–8 above, in that order of priority.
* Decide on the 9 paused mob branches (preservation only; no resumption without the owner).
* Decide on the ST-01 P7–P11 line and whether to re-base the counterfactual audit probe.
* ST-05 and PRC security branches: rebase or isolate under an owner decision.
* Policy B: close gates G1–G7 before any implementation.

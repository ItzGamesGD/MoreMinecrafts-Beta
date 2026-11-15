# Remediation register

As of 2026-11-10 (inventory `2026-11-10T20:24:58Z`). Machine-readable twin: [`REMEDIATION_REGISTER.json`](REMEDIATION_REGISTER.json).

Status vocabulary: **FIXED** (defect corrected *and* verified) · **DOCUMENTED** (recorded, nothing else changed) · **BLOCKED** · **OWNER DECISION REQUIRED** · **UNVERIFIED**. A finding is never FIXED merely because it is documented.

| Status | Count |
|---|---:|
| BLOCKED | 1 |
| DOCUMENTED | 10 |
| FIXED | 11 |
| OWNER DECISION REQUIRED | 8 |
| UNVERIFIED | 1 |

## Summary

| ID | Title | Severity | Status | Decision |
|---|---|---|---|---|
| F-01 | No document states that main is stale, that checkpoint/wave16 is the latest unified line, or what the unmerged stacks are | high (orientation) | **DOCUMENTED** | D-1 |
| F-02 | main's authority indexes are 2026-10-22; STRUCTURE_AUTHORITY_INDEX.md does not exist on main | medium | **OWNER DECISION REQUIRED** | D-1, D-7 |
| F-03 | STRUCTURE_AUTHORITY_INDEX cites 'Draft PR #219' as the ST-05 implementation | low | **DOCUMENTED** | D-7 |
| F-04 | Research node counts: index says 45/40/2, audit table says 43/42/2 | low | **DOCUMENTED** | D-7 |
| F-05 | Index routes the Ancient City arena to 'Draft PR #83' / branch codex/ancient-city-arena-prototype-20261013 | low | **DOCUMENTED** | D-5, D-7 |
| F-06 | Index calls PR #181 a Draft | low | **DOCUMENTED** | D-7 |
| F-07 | Structure index says ST-01 'Implementation is not authorized' while ST-01 branches hold approval records and runtime code | medium | **OWNER DECISION REQUIRED** | D-3 |
| F-08 | Royal chest implemented twice (PR #245 D13-03 stack vs PR #254 ST-01 stack) | high | **OWNER DECISION REQUIRED** | D-2 |
| F-09 | CI repairs (#240, #242) merged only into sibling stacks; ST-01 stack fails the same GameTest | medium | **OWNER DECISION REQUIRED** | D-8 |
| F-10 | D13-03 I03/I04a fail the Tomb Guardian loot GameTest after the Heart of Gravity drop | medium | **OWNER DECISION REQUIRED** | D-8 |
| F-11 | PR descriptions report CI honestly | informational | **DOCUMENTED** |  |
| F-12 | Secret-scan failures on #249/#251/#252 (CORRECTED) | medium (security gate) / high (merge risk) | **OWNER DECISION REQUIRED** | D-4, D-6 |
| F-13 | Sky-tower stack and feat/st03-implement-01 have no GitHub Actions run on any head | medium | **UNVERIFIED** |  |
| F-14 | Shallow clone mis-reports branch relationships | low | **FIXED** |  |
| F-15 | CI numbers cited in CONTENT_AUTHORITY_INDEX are accurate | informational | **DOCUMENTED** |  |
| F-16 | AGENTS.md is identical across branches | informational | **DOCUMENTED** |  |
| N-01 | Retraction: commit id in F-12 was wrong | medium (accuracy of a security statement) | **FIXED** |  |
| N-02 | Allowlist asymmetry: main has 2 .gitleaksignore entries, wave16 and later have 11 | medium | **OWNER DECISION REQUIRED** | D-6 |
| N-03 | PR-only refs were outside the default mirror scope | medium | **FIXED** |  |
| N-04 | A mirrored PR snapshot would be retired when a branch later contains its commit | medium | **FIXED** |  |
| N-05 | Deletion confirmation could not distinguish a missing ref from an inaccessible one | medium | **FIXED** |  |
| N-06 | Serial Drive folder walks would take hours against real Drive; per-run full verify infeasible | medium | **FIXED** |  |
| N-07 | A cron-selected daily reconcile can be starved by Actions concurrency | medium | **FIXED** |  |
| N-08 | Credential-bearing job used mutable action tags | low-medium | **FIXED** |  |
| N-09 | No pre-upload secret gate | high | **FIXED** |  |
| N-10 | Resumable upload retry mishandled 409 fileIdInUse | medium | **FIXED** |  |
| N-11 | Full Git history is not mirrored | medium | **OWNER DECISION REQUIRED** | D-9 |
| N-12 | Live Google Drive verification not possible in this environment | high (for readiness) | **BLOCKED** | D-10 |
| N-13 | Scheduled workflows are disabled by GitHub after 60 days of repository inactivity | low-medium | **DOCUMENTED** |  |
| N-14 | Remote moved during the audit | informational | **DOCUMENTED** |  |
| N-15 | My first control GameTest run was contaminated | informational | **FIXED** |  |

## Detail

### F-01 — No document states that main is stale, that checkpoint/wave16 is the latest unified line, or what the unmerged stacks are

- **Status:** DOCUMENTED (D-1)
- **Original:** DOCUMENTATION MISSING. Search of *.md for 'wave16' found nothing on main, wave16 or the D13/sky-mob/sky-tower tips; one ST-01 doc names it as its own base only.
- **Current evidence:** Recomputed 2026-11-10 from commit ancestry: main cde842dd (last PR merge 2026-10-22) is 335 commits behind checkpoint/wave16-unified-20261104 65072bca; the computed index lists 7 stack roots on wave16 (D13, ST-01, sky-tower, sky-mob, ST-03 and two single-branch docs); 27 merged PRs' work is not in main.
- **Affected:** branches ['main', 'checkpoint/wave16-unified-20261104', 'all stack tips']; files ['docs/CURRENT_STATE.json', 'docs/audit/REPO-STATE-20261110/REPORT.md']
- **Severity / impact:** high (orientation). An agent or reviewer reading main or any single branch gets a wrong picture of the project state.
- **Required remediation:** Provide a generated, dated, machine-readable current-state index and keep the audit report; baseline selection is the owner's.
- **Verification evidence:** docs/CURRENT_STATE.json generated by `drive_mirror current-state`; byte-reproducible from identical inputs (test_lineage_and_current_state_are_reproducible, OK). The index states integrated_project_state = NOT ESTABLISHED.
- **Residual:** The integration baseline itself is an owner decision; no document on any protected branch was changed.

### F-02 — main's authority indexes are 2026-10-22; STRUCTURE_AUTHORITY_INDEX.md does not exist on main

- **Status:** OWNER DECISION REQUIRED (D-1, D-7)
- **Original:** DOCUMENTATION OUTDATED. git diff --stat main checkpoint/wave16 -- docs: 447 files.
- **Current evidence:** Unchanged: main is still cde842dd; AGENTS.md routes agents on main to an index that predates accessory, bounty, ST-05, structure-index and later routing.
- **Affected:** branches ['main']; files ['docs/CONTENT_AUTHORITY_INDEX.md', 'docs/STRUCTURE_AUTHORITY_INDEX.md (absent)']
- **Severity / impact:** medium. Agents resolve authority from stale routing if they start on main.
- **Required remediation:** Owner decides whether main is updated from the checkpoint (D-1) and whether the prepared corrections are applied (D-7). Modifying main is forbidden here.
- **Verification evidence:** Not modifiable under AGENTS.md section 3 and this task's constraints; current-state index warns readers.

### F-03 — STRUCTURE_AUTHORITY_INDEX cites 'Draft PR #219' as the ST-05 implementation

- **Status:** DOCUMENTED (D-7)
- **Original:** DOCUMENTATION OUTDATED. PR API: #219 closed, unmerged.
- **Current evidence:** Re-verified: #219 closed; base branch probe/st05-stage0-a02-a03-20261030 and head branch claude/st05-stage1a-integration-ydvo8d no longer exist; head 8a1ae3d1 IS contained in checkpoint/wave16 (head_in_latest_checkpoint = true) and is not in main.
- **Affected:** branches ['checkpoint/wave16-unified-20261104 and every stack']; files ['docs/STRUCTURE_AUTHORITY_INDEX.md']
- **Severity / impact:** low. A reader looks for a PR that is closed; the code is integrated in wave16.
- **Required remediation:** Append a dated correction (history preserved).
- **Verification evidence:** BRANCH_LINEAGE.json pull_requests[#219]; patches/docs/0001 prepared and `git apply --check` clean on 6 of 7 tips (3-way on the 7th).
- **Residual:** Patch not applied: the file lives on protected checkpoint/stack branches.

### F-04 — Research node counts: index says 45/40/2, audit table says 43/42/2

- **Status:** DOCUMENTED (D-7)
- **Original:** DOCUMENTATION OUTDATED. Index 'now records 45 DATA_ONLY, 40 PARTIAL and 2 BLOCKED'; audit totals table 43/42/2.
- **Current evidence:** Independent recount of the 87-row node table at wave16: 43 DATA_ONLY + 42 PARTIAL + 2 BLOCKED = 87 (87 unique ids), equal to the audit's totals table. The index sentence is a 2026-10-17 record (document blob 22ba8747 on design/stage23a-remaining-approval-20261017 has exactly 45/40/2): history, not a live conflict. No descendant of wave16 touches research source or docs, so 43/42/2 is current. 31/42 PARTIAL rows cite an existing non-catalog Java class; 11 cite vanilla/mixin methods and were not mechanically checkable.
- **Affected:** branches ['checkpoint/wave16-unified-20261104']; files ['docs/CONTENT_AUTHORITY_INDEX.md', 'docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md']
- **Severity / impact:** low. Misleading status for research implementation progress.
- **Required remediation:** Annotate the index sentence as historical and point at the current count.
- **Verification evidence:** Row recount script output (87 rows / 87 unique ids / 43-42-2); patches/docs/0002 prepared (applies on all 7 tips tested). Sub-item UNVERIFIED: behaviour of 11 PARTIAL rows.
- **Residual:** Behaviour-level claims for the 11 rows remain UNVERIFIED (U-RES).

### F-05 — Index routes the Ancient City arena to 'Draft PR #83' / branch codex/ancient-city-arena-prototype-20261013

- **Status:** DOCUMENTED (D-5, D-7)
- **Original:** DOCUMENTATION OUTDATED. PR #83 closed; branch gone; head only at refs/pull/83/head.
- **Current evidence:** Re-verified: #83 closed, head branch absent, base stage-20/checkpoint present; head e7dc53f9f4 contained in no branch, main or wave16. It is one of the 50 PR-only heads, which the mirror now includes by default under pulls/83.
- **Affected:** branches ['refs/pull/83/head']; files ['docs/STRUCTURE_AUTHORITY_INDEX.md']
- **Severity / impact:** low. Prototype history reachable only through a PR ref; could be lost if GitHub prunes it.
- **Required remediation:** Append a dated correction; preserve the commit (mirror).
- **Verification evidence:** inventory pull_heads_not_in_any_branch (50, unchanged); patches/docs/0001; mirror PR-only scope tested (test_pr_only_refs_are_in_default_scope...). Live preservation requires a live sync (BLOCKED).

### F-06 — Index calls PR #181 a Draft

- **Status:** DOCUMENTED (D-7)
- **Original:** DOCUMENTATION OUTDATED. #181 closed.
- **Current evidence:** Re-verified: closed; branch release/wave9-rc-20261025 exists and equals the PR head 268ec65b; commit is in neither main nor wave16. Pins in the index stay valid while the branch exists.
- **Affected:** branches ['release/wave9-rc-20261025']; files ['docs/STRUCTURE_AUTHORITY_INDEX.md']
- **Severity / impact:** low. Pins would dangle if the branch were deleted.
- **Required remediation:** Append a dated correction.
- **Verification evidence:** BRANCH_LINEAGE.json pull_requests[#181]; patches/docs/0001.

### F-07 — Structure index says ST-01 'Implementation is not authorized' while ST-01 branches hold approval records and runtime code

- **Status:** OWNER DECISION REQUIRED (D-3)
- **Original:** CONFLICTING EVIDENCE. Index row on the ST-01 tip vs docs/design/st01-p1/p2 conditional-approval records and src/ code.
- **Current evidence:** Still true on the newest ST-01 tips (p8c2a, phase4-worldgen, g0-r1): same index blob 2ab2dbe0, row unchanged.
- **Affected:** branches ['work/st01-*', 'claude/st01-*']; files ['docs/STRUCTURE_AUTHORITY_INDEX.md']
- **Severity / impact:** medium. Authority for ST-01 implementation is ambiguous; AGENTS.md says stop and ask.
- **Required remediation:** Owner states whether the conditional approvals authorize implementation and amends the index row accordingly.
- **Verification evidence:** Conflict re-verified on three current tips; not resolved here (design decision).

### F-08 — Royal chest implemented twice (PR #245 D13-03 stack vs PR #254 ST-01 stack)

- **Status:** OWNER DECISION REQUIRED (D-2)
- **Original:** CONFLICTING EVIDENCE. Trial merge of 6c7923e0 and 03685656: 8 conflicted files.
- **Current evidence:** Re-run on current tips: same 8 files (6 add/add + 2 content), merge-base = wave16. Side B is a superset in the persistence layer; each side carries authority documents the other lacks. Full analysis, strategies and required verification: CURRENT_STATE facts C-1.
- **Affected:** branches ['implement/d13-03-i06-p4-st01-integration-20261108', 'work/st01-p8b2-royal-chest-runtime-20261110']; files ['RoyalChest{Container,Data,Menu,RewardModel,Role,Runtime}.java', 'PyramidRoyalChestRewards.java', 'MoreMinecrafts.java', 'fabric.mod.json (gametest)']
- **Severity / impact:** high. The two stacks cannot both be merged; the reward-system semantics differ by authority source.
- **Required remediation:** Owner chooses the governing authority and integration strategy; then reconcile once and run the listed verification.
- **Verification evidence:** git merge-tree --write-tree reproduced (exit 1, 8 paths); royal_chest_conflict.json in evidence/.

### F-09 — CI repairs (#240, #242) merged only into sibling stacks; ST-01 stack fails the same GameTest

- **Status:** OWNER DECISION REQUIRED (D-8)
- **Original:** CONFIRMED. Fabric CI logs of #254 and #249.
- **Current evidence:** 15 current branch heads fail Fabric CI on colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise (log-extracted; 2 other failing heads fail different tests). All 11 wave16 descendants containing #240 or #242 pass; 15 without either fail; 7 without either pass (final snapshot). #242 contains the same two Colosseum commits as #240 plus a Tomb Guardian test alignment. #240/#242 are merged into their stacks' base branches only: neither is in main or wave16.
- **Affected:** branches ['work/st01-* (incl. p8c2a, g0-*, o14-*)', 'claude/st01-pyramid-phase4-worldgen-roke2l', 'implement/d13-01..i04a']; files ['src/main/.../colosseum/ArenaController.java', 'ColosseumRuntime.java', 'src/gametest/.../colosseum/*']
- **Severity / impact:** medium. ST-01 and D13 heads cannot reach green CI; masks real regressions.
- **Required remediation:** Apply the existing two-commit fix to the affected stacks (technical, behaviour-preserving). Prepared as a branch; not applied to any existing branch.
- **Verification evidence:** Branch remediation/st01-colosseum-clock-seam-20261110 (f2c0a6c8) = work/st01-p8c2a-complete-assembly-20261110-r1 (c25f9d44) + the two #240 commits; 6 files, all under colosseum/. Local, JDK 25.0.5, Gradle 9.6: `./gradlew test compileGametestJava` BUILD SUCCESSFUL, 594 JUnit tests / 0 failures (incl. 2 new ArenaControllerClockTest); `runGameTest` on a fresh worktree: all 551 required tests passed. Clean control (fresh worktree, unmodified c25f9d44): 1 required test failed = the absence-grace test, 'grace running on tick 49', identical to CI. n=1 each; causation supported by this before/after plus the CI cross-tabulation. See evidence/colosseum_gametest_verification.md.
- **Residual:** A first control run reused a worktree and was contaminated by stale saved-world state (18 spurious failures); it is excluded from the evidence and replaced by the clean control.

### F-10 — D13-03 I03/I04a fail the Tomb Guardian loot GameTest after the Heart of Gravity drop

- **Status:** OWNER DECISION REQUIRED (D-8)
- **Original:** CONFIRMED. Fabric CI run 42136101897.
- **Current evidence:** Still failing on implement/d13-03-i03-heart-of-gravity, i04a and docs/d13-03-i06-royal-chest-authority (two failing tests each: colosseum grace + tomb_guardian kill loot). The repair in #242 (commit 1ec7610f) edits the test's expected pool to match D13-03 Q21-Q24, i.e. it encodes an authority decision.
- **Affected:** branches ['implement/d13-03-i03-...', 'implement/d13-03-i04a-...', 'docs/d13-03-i06-royal-chest-authority']; files ['src/gametest/.../boss/TombGuardianGameTests.java']
- **Severity / impact:** medium. D13 stack heads red; fix is design-linked so not purely technical.
- **Required remediation:** Owner confirms applying the test alignment to I03/I04a (stack ordering) or re-bases the stack.
- **Verification evidence:** ci_failures.json (extracted failing test names per head). No patch prepared by design (STOP & ASK: encodes a design decision).

### F-11 — PR descriptions report CI honestly

- **Status:** DOCUMENTED
- **Original:** CONFIRMED. #228 and #254 report failures; #233-#236 report GameTest NOT RUN.
- **Current evidence:** Not re-audited for the 8 PRs created since; no counter-example found in the 10 PRs examined.
- **Affected:** branches —; files —
- **Severity / impact:** informational. None.
- **Required remediation:** None.
- **Verification evidence:** Observation only; no action.

### F-12 — Secret-scan failures on #249/#251/#252 (CORRECTED)

- **Status:** OWNER DECISION REQUIRED (D-4, D-6)
- **Original:** CONFIRMED (risk) -- original text contained an error. Original report named commit 54898b74 as the flagged commit and as unreachable. That was wrong (wrong log line).
- **Current evidence:** Corrected: the single finding in all three runs is commit cec8bff8, tools/st05-p0-probe-01/freeze_integrity.py:23, rule generic-api-key; reachable only from probe/st05-p0-probe-01 and audit/st05-po25-review-package-20261102, in no PR head. Classified scanner false positive (proposed, awaiting owner). The risk that #249/#251/#252 would land the whole stack on main is real: they are 365/369/371 commits ahead of main.
- **Affected:** branches ['probe/st05-p0-probe-01', 'audit/st05-po25-review-package-20261102', 'work/st01-o14-phase1-phase2-20261109', 'work/st01-o14-phase3-calibration-20261109', 'work/st01-o14-phase3-1-validation-20261109']; files ['tools/st05-p0-probe-01/freeze_integrity.py']
- **Severity / impact:** medium (security gate) / high (merge risk). Every PR into main fails the pre-public scan; live Drive sync of the two affected branches is held by the gate.
- **Required remediation:** Owner decides the finding (D-6) and whether #249/#251/#252 should keep main as base (D-4).
- **Verification evidence:** SECRET_SCAN_INVESTIGATION.md; pinned gitleaks 8.30.1 reproduces CI exactly (1 residual with #252's allowlist, 10 with main's); gate run on the real repo: 12 findings, 11 approved, 1 proposed, 2 refs quarantined.

### F-13 — Sky-tower stack and feat/st03-implement-01 have no GitHub Actions run on any head

- **Status:** UNVERIFIED
- **Original:** UNVERIFIED. Runs API by head_sha: none.
- **Current evidence:** Still none: 8 of the 9 sky-tower branches (incl. the new implementation/sky-tower-mountain-adaptive-b and claude/sky-tower-phase-2-validation-twragg) have no CI run; only docs/sky-tower-design-q01-q41 has one.
- **Affected:** branches ['feature/sky-tower-*', 'claude/sky-tower-*', 'audit/sky-tower-completeness', 'validation/sky-tower-mountain-height', 'implementation/sky-tower-mountain-adaptive-b']; files —
- **Severity / impact:** medium. Documented validation of the sky-tower work cannot be checked from CI evidence.
- **Required remediation:** Run the relevant workflows on the stack tip (owner/author).
- **Verification evidence:** Computed from the runs API per head (BRANCH_LINEAGE.json ci_on_head empty).

### F-14 — Shallow clone mis-reports branch relationships

- **Status:** FIXED
- **Original:** CONFIRMED (tooling). git rev-parse --is-shallow-repository = true gave 749 'behind'.
- **Current evidence:** The audit tooling builds its own bare cache from a full fetch and never uses the working clone for ancestry; lineage was computed on that cache.
- **Affected:** branches —; files ['tools/drive_mirror/gitsrc.py']
- **Severity / impact:** low. Wrong lineage numbers if computed in a shallow clone.
- **Required remediation:** Always compute ancestry from the full cache.
- **Verification evidence:** `git --git-dir=<cache> rev-parse --is-shallow-repository` = false (cache created by git init --bare + full fetch); lineage reproduced twice with identical bytes.

### F-15 — CI numbers cited in CONTENT_AUTHORITY_INDEX are accurate

- **Status:** DOCUMENTED
- **Original:** CONFIRMED. Fabric CI #754, K5 probe #179, K5 restart #87 on 227749de; #291 on PR #66.
- **Current evidence:** Not changed; nothing in this task invalidates them.
- **Affected:** branches —; files —
- **Severity / impact:** informational. None.
- **Required remediation:** None.
- **Verification evidence:** Observation only.

### F-16 — AGENTS.md is identical across branches

- **Status:** DOCUMENTED
- **Original:** CONFIRMED. blob 05abb64c on main, wave16 and the tips checked.
- **Current evidence:** Re-checked on all 133 branches: 116 have blob 05abb64c; 17 have no AGENTS.md (stage-1..15 checkpoints and two early prototype/docs branches).
- **Affected:** branches ['17 early branches without the file']; files ['AGENTS.md']
- **Severity / impact:** informational. None.
- **Required remediation:** None.
- **Verification evidence:** git cat-file -e <branch>:AGENTS.md over all 133 branches.

### N-01 — Retraction: commit id in F-12 was wrong

- **Status:** FIXED
- **Original:** NEW (self-correction). -
- **Current evidence:** See F-12 and SECRET_SCAN_INVESTIGATION.md section 1.
- **Affected:** branches —; files ['docs/audit/REPO-STATE-20261110/REPORT.md']
- **Severity / impact:** medium (accuracy of a security statement). A reader would search for a nonexistent commit.
- **Required remediation:** Correct the report in place; record the retraction.
- **Verification evidence:** REPORT.md F-12 row and section 6 corrected; SECRET_SCAN_INVESTIGATION.md section 1.

### N-02 — Allowlist asymmetry: main has 2 .gitleaksignore entries, wave16 and later have 11

- **Status:** OWNER DECISION REQUIRED (D-6)
- **Original:** NEW. -
- **Current evidence:** A push to main would report 10 findings today; a PR into main reports 1 (it uses the merged tree's allowlist).
- **Affected:** branches ['main']; files ['.gitleaksignore']
- **Severity / impact:** medium. main's own security scan is red as soon as it runs again.
- **Required remediation:** Owner decides how the reviewed entries reach main (together with finding 1).
- **Verification evidence:** gitleaks with each tip's allowlist (see SECRET_SCAN_INVESTIGATION.md section 4).

### N-03 — PR-only refs were outside the default mirror scope

- **Status:** FIXED
- **Original:** NEW. Default --pull-heads was none.
- **Current evidence:** 50 PR heads (48 closed, 2 merged) are in no branch (49,080 files, 0.82 GB logical, 223 new blobs / 4.8 MB, ~57k Drive items).
- **Affected:** branches ['refs/pull/*/head not in any branch']; files ['tools/drive_mirror/__main__.py', 'sync.py', '.github/workflows/drive-mirror.yml']
- **Severity / impact:** medium. Closed-PR work reachable only through refs/pull/* would not be on Drive.
- **Required remediation:** Default to orphaned; namespaces under pulls/, separate from branches/.
- **Verification evidence:** test_pr_only_refs_are_in_default_scope_with_exact_manifests_and_isolated_namespaces; full repository emulator run (MIRROR_COMPLETENESS.md).

### N-04 — A mirrored PR snapshot would be retired when a branch later contains its commit

- **Status:** FIXED
- **Original:** NEW (defect found by code review). -
- **Current evidence:** Reproduced in design review: under orphaned scope the ref leaves the desired set and _retire_missing moved the namespace.
- **Affected:** branches —; files ['tools/drive_mirror/sync.py']
- **Severity / impact:** medium. Silent loss of a PR snapshot from the active mirror.
- **Required remediation:** Keep a mirrored PR namespace while the remote still advertises the ref.
- **Verification evidence:** test_pr_namespace_is_sticky_when_a_branch_later_contains_the_head.

### N-05 — Deletion confirmation could not distinguish a missing ref from an inaccessible one

- **Status:** FIXED
- **Original:** NEW (defect). -
- **Current evidence:** Two full listings are not independent evidence of deletion.
- **Affected:** branches —; files ['tools/drive_mirror/sync.py', 'gitsrc.py']
- **Severity / impact:** medium. A hidden/denied/truncated listing could retire live namespaces.
- **Required remediation:** Require a targeted successful ls-remote that finds nothing, and (for PR heads) the GitHub API; otherwise keep, mark unconfirmed, report.
- **Verification evidence:** test_pr_ref_disappearance_distinguishes_inaccessible_from_deleted, test_branch_probe_failure_is_inaccessible_not_deleted; mass-retire guard re-tested (test_mass_retire_guard_and_dry_run).

### N-06 — Serial Drive folder walks would take hours against real Drive; per-run full verify infeasible

- **Status:** FIXED
- **Original:** NEW (defect, invisible on the emulator). -
- **Current evidence:** ~32,000 namespace folders (+7,800 for PR-only) walked one request at a time.
- **Affected:** branches —; files ['sync.py', 'verify.py', '.github/workflows/drive-mirror.yml']
- **Severity / impact:** medium. Hourly runs could exceed the 6 h job limit.
- **Required remediation:** Parallel crawl; incremental runs verify bytes only for changed namespaces; reconcile verifies in full.
- **Verification evidence:** test_scoped_verify_reads_far_less_and_full_verify_still_catches_drift; whole-repo emulator timings.

### N-07 — A cron-selected daily reconcile can be starved by Actions concurrency

- **Status:** FIXED
- **Original:** NEW (defect). -
- **Current evidence:** A newer pending run replaces an older pending one in the same group.
- **Affected:** branches —; files ['.github/workflows/drive-mirror.yml', 'sync.py']
- **Severity / impact:** medium. Drift repair might never run on a busy mirror.
- **Required remediation:** Decide reconcile from persisted last_reconcile_at; one hourly cron.
- **Verification evidence:** test_incremental_run_upgrades_itself_to_reconcile_from_persisted_state.

### N-08 — Credential-bearing job used mutable action tags

- **Status:** FIXED
- **Original:** NEW. -
- **Current evidence:** actions/checkout@v4 etc.
- **Affected:** branches —; files ['.github/workflows/drive-mirror.yml']
- **Severity / impact:** low-medium. Supply-chain exposure of Drive credentials.
- **Required remediation:** Pin to commit SHAs.
- **Verification evidence:** actionlint clean; all 4 third-party actions pinned to 40-hex SHAs (checked by script).

### N-09 — No pre-upload secret gate

- **Status:** FIXED
- **Original:** NEW. -
- **Current evidence:** Upload happened before any scan.
- **Affected:** branches —; files ['tools/drive_mirror/security.py', 'security/approved_findings.json', '.github/workflows/drive-mirror.yml']
- **Severity / impact:** high. Flagged content could reach Drive.
- **Required remediation:** Fail-closed gate with per-ref quarantine; never auto-suppresses.
- **Verification evidence:** 5 gate tests + real-gitleaks end-to-end test; run on the real repo (2 refs quarantined).

### N-10 — Resumable upload retry mishandled 409 fileIdInUse

- **Status:** FIXED
- **Original:** NEW (latent defect, found by a flaky regression). -
- **Current evidence:** Lost response on the final PUT of a >5 MB upload, retried with the same generated id.
- **Affected:** branches —; files ['tools/drive_mirror/drive.py']
- **Severity / impact:** medium. Large-file sync could fail and need a rerun.
- **Required remediation:** Treat 409/fileIdInUse on the PUT as already created.
- **Verification evidence:** test_lost_response_on_resumable_put_does_not_duplicate_or_fail (deterministic); test_transient_errors_and_rate_limits_are_retried.

### N-11 — Full Git history is not mirrored

- **Status:** OWNER DECISION REQUIRED (D-9)
- **Original:** NEW (scope). -
- **Current evidence:** Measured: all refs bundle 546 MB / 391 refs, restore 8.6 s; commit-graph metadata 0.50 MB (0.13 MB gz), 2,135 commits.
- **Affected:** branches —; files ['tools/drive_mirror/MIRROR_COMPLETENESS.md']
- **Severity / impact:** medium. Gemini sees tip states, not what changed when.
- **Required remediation:** Approve and add a history archive (bundle and/or commit-graph metadata) as a separate extension.
- **Verification evidence:** MIRROR_COMPLETENESS.md section 3 (measured).

### N-12 — Live Google Drive verification not possible in this environment

- **Status:** BLOCKED (D-10)
- **Original:** NEW. -
- **Current evidence:** No credentials, connector or destination; Google API answers 401 unauthenticated.
- **Affected:** branches —; files —
- **Severity / impact:** high (for readiness). Production readiness cannot be claimed.
- **Required remediation:** Provide destination + credentials; run the controlled live test.
- **Verification evidence:** drive_mirror preflight -> BLOCKED exit 4; test_blocked_without_credentials.

### N-13 — Scheduled workflows are disabled by GitHub after 60 days of repository inactivity

- **Status:** DOCUMENTED
- **Original:** NEW (operational risk). -
- **Current evidence:** GitHub documented behaviour for public repositories.
- **Affected:** branches —; files ['.github/workflows/drive-mirror.yml']
- **Severity / impact:** low-medium. The mirror could go silently stale.
- **Required remediation:** External freshness monitor on state.json last_complete_run.
- **Verification evidence:** README limitations; workflow header comment.

### N-14 — Remote moved during the audit

- **Status:** DOCUMENTED
- **Original:** NEW (observation). -
- **Current evidence:** Between the first audit snapshot (02:51 UTC) and the final snapshot of this work (11:33 UTC): 126 -> 135 branches (this audit's branch, its remediation branch and 7 others), 252 -> 259 PRs, 289 -> 301 PR refs, 4 existing heads advanced. New ST-01 heads (p8c2a, g0-*, phase4-worldgen) also fail the grace test.
- **Affected:** branches ['work/st01-g0-*', 'work/st01-p8c2a-*', 'claude/st01-pyramid-phase4-worldgen-roke2l', 'implementation/sky-tower-mountain-adaptive-b', 'claude/sky-tower-phase-2-validation-twragg']; files —
- **Severity / impact:** informational. Any SHA in this register is a point-in-time value.
- **Required remediation:** Regenerate before relying on a SHA.
- **Verification evidence:** INVENTORY.json generated_at 2026-11-10T20:24:58Z.

### N-15 — My first control GameTest run was contaminated

- **Status:** FIXED
- **Original:** NEW (self-correction). -
- **Current evidence:** Reused a worktree; stale saved-world state caused 18 spurious Colosseum failures that CI never shows.
- **Affected:** branches —; files —
- **Severity / impact:** informational. A wrong control would have misrepresented the fix.
- **Required remediation:** Redo in a fresh worktree; exclude the invalid run.
- **Verification evidence:** evidence/colosseum_gametest_verification.md records both runs and why one is excluded.


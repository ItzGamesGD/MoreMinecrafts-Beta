# Drive mirror: test evidence (updated 2026-11-10, post-audit remediation)

This supersedes the first version of this file. Validation types are kept apart on purpose.
**Emulator success is never evidence of live Google Drive behaviour.**

## Verdicts

| Validation type | Verdict | Basis |
|---|---|---|
| Static validation | **PASS** | `pyflakes tools/drive_mirror` clean; `actionlint .github/workflows/drive-mirror.yml` clean; all 4 third-party actions pinned to 40-hex commit SHAs (checked by script); no gitleaks finding and no e-mail address in any added file |
| Unit / scenario tests (Drive API emulator) | **PASS** | 29 / 29, run from a fresh checkout of commit `c785a8df` (below) |
| Whole-repository emulator run | **PASS** | frozen copy of the live remote (136 heads, 259 PR heads): §3 |
| Project tests for the CI remediation branch | **PASS** (before/after) | JUnit 594 / 0 failures; `runGameTest` 551 / 551 on the fix; clean control reproduces the CI failure: `docs/audit/REPO-STATE-20261110/evidence/colosseum_gametest_verification.md` |
| Live GitHub verification | **DONE** (read-only) | §4 |
| Live Google Drive verification | **BLOCKED** | no credentials, connector or destination. `drive_mirror preflight` → `{"result":"BLOCKED","reason":"no Drive credentials configured"}`, exit 4; unauthenticated `GET https://www.googleapis.com/drive/v3/about` → HTTP 401. **Not run.** |
| Live sync, controlled live test, workflow execution | **NOT RUN** | `DRIVE_MIRROR_ENABLED` is unset; the workflow is not on `main`. Both jobs were skipped when the branch was pushed (observed on GitHub) |

## Commits

| Purpose | SHA |
|---|---|
| Baseline reported by the first audit (verified: ancestor chain, 0 files outside audit/mirror scope) | `a98961eb` (docs), `280b0007` (mirror), both on `claude/moreminecrafts-beta-audit-mirror-7id36x` |
| 1. Audit and documentation corrections | `53303a32` |
| 2. Mirror completeness (+ gate engine) | `cde833c4` |
| 3. Security and workflow hardening | `dcfc261a` |
| 4a. Tests | `c785a8df` (the suite below ran on this commit) |
| 4b. Test evidence | the commit that adds this file |
| Remediation branch (not merged, no PR) | `remediation/st01-colosseum-clock-seam-20261110` @ `f2c0a6c8` |

## 1. Static validation

```
python3 -m pyflakes tools/drive_mirror                       → no output
actionlint .github/workflows/drive-mirror.yml                → no output
gitleaks dir tools docs/audit docs/CURRENT_STATE.json .github/workflows/drive-mirror.yml README.md → "no leaks found"
```

## 2. Scenario suite (emulator)

```
GITLEAKS_BIN=<gitleaks 8.30.1> PYTHONPATH=tools python3 -W error::ResourceWarning -m unittest discover -s tools/drive_mirror/tests -v
→ Ran 29 tests in 147.2 s — OK   (ResourceWarnings promoted to errors; none skipped)
```

13 original scenarios (all pass after the engine changes; the retry test was repeated 6× after its fix: 6/6) and 16 new:

| Required check | Test |
|---|---|
| 1. PR-only ref discovery and mirroring (default scope) | `test_pr_only_refs_are_in_default_scope_with_exact_manifests_and_isolated_namespaces`, `test_pr_metadata_is_recorded_in_pr_manifest` |
| 2. PR-only ref disappearance | `test_pr_ref_disappearance_distinguishes_inaccessible_from_deleted`, `test_branch_probe_failure_is_inaccessible_not_deleted`, `test_pr_namespace_is_sticky_when_a_branch_later_contains_the_head` |
| 3. Exact ref-to-manifest mapping | same first test (6 namespaces, 3 kinds, commit sha vs `ls-remote`), `test_verifier_detects_manifest_ref_mismatch_and_orphan_manifest` |
| 4. Branch / PR namespace isolation | first test: branch `pull/7/head` and `refs/pull/7/head` coexist with different content |
| 5. Documentation index reproducibility | `test_lineage_and_current_state_are_reproducible` (inventory equal except timestamp; lineage and CURRENT_STATE byte-identical) |
| 6. Security gate | `test_gate_blocks_unreviewed_and_proposed_and_quarantines_only_affected_tips`, `test_gate_passes_only_for_approved_status`, `test_gate_fails_closed_on_unreadable_or_malformed_input`, `test_quarantined_refs_are_not_uploaded_and_never_silent`, `test_content_uploaded_before_the_gate_fired_is_reported`, `test_real_gitleaks_report_end_to_end` (the planted secret value is asserted absent from every output) |
| 7. Full repository emulator synchronization | §3 |
| 8. Independent checksum verification | `verify` (server SHA-256 + size; deep re-download re-hashing) in `test_full_lifecycle`, `test_drift_detected_and_reconciled`, §3 |
| 9. Idempotent repeat synchronization | `test_full_lifecycle` (identical Drive snapshot), §3 (0 items changed) |
| 10. Interrupted-run recovery | `test_interrupted_sync_recovers` (SIGKILL mid-run), `test_transient_errors_and_rate_limits_are_retried`, `test_lost_response_on_resumable_put_does_not_duplicate_or_fail` |
| New behaviour | `test_incremental_run_upgrades_itself_to_reconcile_from_persisted_state`, `test_scoped_verify_reads_far_less_and_full_verify_still_catches_drift` |

Defects found by this testing and fixed: a duplicated CLI option that broke every invocation (caught by the first run); a resumable-upload
retry that surfaced `409 fileIdInUse` (appeared as one flaky failure in the full suite, then reproduced deterministically with a new emulator fault).

## 3. Whole-repository run (emulator, frozen remote)

Source: `tests/full_repo_emulator.py` on a frozen local copy of `https://github.com/ItzGamesGD/MoreMinecrafts-Beta` taken 2026-11-10T20:36Z
(136 heads, 259 PR heads), after the security gate (2 refs quarantined). Raw summary: `docs/audit/REPO-STATE-20261110/evidence/full_repo_emulator_summary.json`.

| Step | Exit | Result |
|---|---|---|
| initial sync | 0 | 186 namespaces: 184 complete, 2 quarantined; 136 branch and 50 PR-only namespaces; 5,242 uploads (437 MB), 281,208 copies, 43,615 folders |
| verify, deep sample 500 | 0 | **PASS**; 286,450 / 286,450 files (14.60 GB) matched by size and server SHA-256; 500 / 500 downloads matched their git blob ids |
| sync again | 0 | 184 no-ops, **0** items changed, 3 writes (lease, state) |
| sync `--mode reconcile` | 0 | full crawl, **0** items changed |
| verify | 0 | **PASS**, 286,450 files |

An earlier run against the *live* remote reported `namespace_missing` for a branch created while it ran, then synced it in the next step.
That is the verifier working, and the run is not used as idempotency evidence. Another attempt was discarded because my freeze script mis-mapped PR refs
(0 PR heads); it is not used either.

## 4. Live GitHub verification (read-only)

Performed against the real repository on 2026-11-10: `git ls-remote` and full fetch of all heads and `refs/pull/*/head`; REST API for PRs
(259), Actions workflows and runs per branch head, job logs for 17 failing heads and for the three secret-scan runs; pinned gitleaks 8.30.1 over
2,008 commits; `git merge-tree` of PR #245 and #254; ancestry computations for every branch and PR. No write to GitHub other than pushing
this audit branch and the remediation branch (no PR, no merge, no comment).

## 5. Not verified

Real Google Drive (quotas, rate limits, error codes, checksum availability, search consistency); the workflow on a runner; live (in-game) behaviour of
the Colosseum change; the cause of the royal_tomb_flow follower-west GameTest failure on #251/#252.

## 6. Deployment-readiness verification (2026-11-11)

Branch `claude/moreminecrafts-mirror-readiness-20261111`, created from the verified baseline `6d4beed1`. Emulator and static evidence only: **live Google Drive: NOT TESTED**.

| Check | Result | Basis |
|---|---|---|
| Baseline re-run without gitleaks (`6d4beed1`) | 29 run, 28 passed, 0 failed, **1 skipped** | `test_real_gitleaks_report_end_to_end` needs `GITLEAKS_BIN`; this was the gap in the earlier "29/29" claim |
| gitleaks binary | **verified** | v8.30.1 `linux_x64` archive from the gitleaks GitHub release, SHA-256 `551f6fc8…2470eb` = the release's `checksums.txt` = both workflow pins; `gitleaks version` → `8.30.1` |
| `test_real_gitleaks_report_end_to_end` with `GITLEAKS_BIN` | **passed, not skipped** | also in the full run below |
| Full suite on the final code (commit `f69378d`; Python and workflow identical to `7871afc`, later commits touch documentation only) | **39 run, 39 passed, 0 failed, 0 skipped** | `GITLEAKS_BIN=<8.30.1> PYTHONPATH=tools python3 -W error::ResourceWarning -m unittest discover -s tools/drive_mirror/tests -v` → `Ran 39 tests in 155.6 s — OK` |
| New tests | 10 (`test_workflow.py`): 9 static checks of the workflow, 1 CLI dry-run check | the verify-after-dry-run check **fails on the previous workflow** (reproduced) |
| `python3 -m pyflakes tools/drive_mirror` (pyflakes 4.1.0) | clean | |
| `actionlint .github/workflows/drive-mirror.yml` (1.7.7, archive checksum verified) | clean | |
| Idempotency and checksums | passed inside the suite (`test_full_lifecycle`, `test_drift_detected_and_reconciled`, `test_interrupted_sync_recovers`, `test_lost_response_on_resumable_put_does_not_duplicate_or_fail`, …) | emulator |
| Runbook rehearsal | passed | every command form of RUNBOOK Parts 1, 3a, 4–6 run against the emulator on a scratch repository: preflight with write probe PASS; dry run writes nothing; sync, verify (6/6 files), repeat sync `noop`, `reconcile` (`updated`, 0 content writes), `status` PASS; then rename, branch deletion and force-push converge and verify PASS |
| Diff scope `6d4beed1..HEAD` | `.github/workflows/drive-mirror.yml` and `tools/drive_mirror/` only; no engine (`*.py` other than the new test) change | |

**Defect found and fixed.** The workflow's `Independent verification` step ran after a successful `dry-run`; a dry run writes nothing, so on a never-synced namespace (the documented
first step of the controlled live test) `verify` fails with `mirror_root` and the run goes red. The step is now skipped for `dry-run`.

**Historical (not re-run): whole-repository emulator run.** The 286,450 / 286,450-file result in §3 comes from the frozen remote copy taken 2026-11-10T20:36Z
(136 heads, 259 PR heads) with the code of commit `c785a8df`. The Python in `6d4beed1` and in this branch is identical to `c785a8df` (`git diff c785a8df 6d4beed1` touches only `.gitignore` and this file;
this branch changes no engine file), so the result still describes the engine, but it describes **that snapshot**, not the repository today. It was not re-run: no engine change and no unresolved integrity concern.

**Security.** Finding `cec8bff8` re-examined (structure only, no value printed): reproduced by the scanner on the blob alone (line 23, entropy 3.63); line 23 holds two identifier-style literals; all 9 reconstructed
paths of the enclosing list exist in the commit's tree. Classified as a demonstrable false positive, **not approved**; `approved_findings.json` is unchanged and still `proposed`; the 2 refs stay quarantined.
See `security/D-6_APPROVAL_PROPOSAL.md`.

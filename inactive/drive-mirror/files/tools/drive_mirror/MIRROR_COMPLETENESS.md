# Mirror completeness report (2026-11-10)

Scope questions answered here: what the mirror covers, what it deliberately does not, how completeness and integrity are
independently checked, and how a full Git history archive compares. All numbers are measured on the repository as of
2026-11-10; "emulator" means the local Drive API emulator, **not Google Drive**. Live Google Drive verification: **BLOCKED**
(no credentials or destination). Nothing here is a production-readiness claim.

## 1. Scope

| Included | Namespace | Default | Notes |
|---|---|---|---|
| Every branch (`refs/heads/*`) | `branches/<encoded name>/` | yes | 136 at the final snapshot |
| Every tag (`refs/tags/*`, peeled to a commit) | `tags/…` | yes | the repository has none |
| PR heads that **no branch contains** | `pulls/<N>/` | **yes (new)** | 50 at the final snapshot: 48 closed, 2 merged |
| PR heads that a branch contains | metadata only (`refs.json`, `pulls.json`) | n/a | `--pull-heads all` mirrors them too |
| Ref list, HEAD symref, PR metadata, commit metadata, per-namespace manifests | `_mirror/` | yes | source ref, commit sha and sync time in every manifest |
| Refs flagged by the security gate | **quarantined: not uploaded, listed everywhere** | yes | 2 today (§4) |

Not included (explicit, not silent): Git history other than ref heads; non-file tree entries (submodule pointers: the repository
has none; symlinks **are** stored, as the link target with `mm_mode=120000`); empty directories (Git has none).

PR-only namespaces cannot be confused with branches: separate container folders, separate manifest names (`pulls__83.json` vs
`branches__…`), and `source.kind = "pull_head"` with the PR number and state. A branch literally named `pull/7/head` is tested
to coexist with PR 7 (`branches/pull%2F7%2Fhead` vs `pulls/7`).

## 2. Measured size and cost of each scope (final snapshot, emulator)

| | Branch heads (134 mirrored) | + PR-only heads (50) | Total |
|---|---:|---:|---:|
| namespaces | 134 (+2 quarantined) | 50 | 184 uploaded, 186 expected |
| files | ≈ 237,000 | 49,080 | **286,450** |
| logical bytes | ≈ 13.8 GB | 0.82 GB | **14.60 GB** |
| bytes actually uploaded | | 223 new blobs = 4.8 MB | **437 MB** (5,242 uploads) |
| server-side copies | | | 281,208 |
| Drive items (files + folders + metadata) | | ≈ +57,000 | **330,445** |
| HTTP writes, initial sync | | | 330,814 (emulator: 498 s) |

A shared drive is limited to **500,000 items**: the mirror already uses 66 %, of which PR-only refs account for ~11 points. Growth is driven by branch
count (each branch is a full copy because the requirement is exact per-branch paths), not by repository size.

## 3. Snapshot vs PR-only vs full-history archive

| | A. Branch-head file snapshots | B. PR-only head snapshots | C. Full Git history archive (**not implemented**) |
|---|---|---|---|
| What it preserves | the tree of every branch tip, browsable path by path | the tree of 50 closed/merged PR heads that no branch holds | every commit, tree and blob reachable from every ref (heads + PR heads) |
| Measured size | 13.8 GB logical, ~0.4 GB uploaded, ~273k items | 0.82 GB logical, 4.8 MB new, ~57k items | **bundle: 546 MB, one file** (391 refs); commit-graph metadata alone: 0.50 MB (0.13 MB gzip), 2,135 commits |
| Sync cost | incremental per changed file; new branch ≈ one copy per file | same | bundle: regenerated per run (2.8 s here), one 546 MB upload; metadata: one tiny file |
| Recovery | read any file at any branch tip; **cannot** rebuild a repository (no history) | same, for PR heads | `git clone` from the bundle: 8.6 s here; `git bundle verify` passes; full history and all refs |
| Answers "what changed when / who / why" | no | no | yes (needs git; the bundle is opaque to a reader that cannot run git) |
| Security surface | tip trees only; quarantine is per ref | tip trees only | **all history**, including deleted files; a flagged historical commit cannot be quarantined without dropping its refs |
| Needs new approval | no (approved scope) | no (approved scope: "all remote branches") | **yes** |

**Is history necessary for the stated goal** (an AI understanding the repository's current *and historical* development state)?
Snapshots give the current state of every line of work and `pulls.json`/`refs.json` give PR and ref history, but *how* a branch got
there is not recoverable. The smallest sufficient addition is **commit-graph metadata** (sha, parents, author name, date, subject for all
2,135 commits, 0.13 MB gzip; no content, no e-mail addresses). A **git bundle** is the only way to recover real history, at 546 MB and
with the exposure noted above (it would also have to exclude, or be gated on, the refs containing the proposed secret-scan finding).

**Decision: not implemented.** Neither was in the approved scope, the bundle widens the secret-exposure surface, and the gate would need
a history-level mode. Reported as separately approvable extension **D-9**, with commit-graph metadata as the suggested first step.

## 4. Integrity and recovery: what is checked, and by what

The verifier (`verify.py`) shares no planning code with the synchroniser: it re-fetches the remote, re-reads every tree, hashes blobs
itself, walks Drive itself, and compares to Drive's **server-computed** checksums.

| Requirement | Check | Evidence |
|---|---|---|
| Every included ref has a manifest | `manifest_missing`; `manifest_without_namespace` for the reverse | whole-repo run PASS; `test_verifier_detects_manifest_ref_mismatch_and_orphan_manifest` |
| Manifest maps to the right ref and commit | `manifest_ref_mismatch`, `manifest_kind_mismatch`, `manifest_stale_commit` | `test_pr_only_refs_are_in_default_scope_with_exact_manifests_and_isolated_namespaces` (all 6 namespaces of 3 kinds) |
| Every tracked file present with exact bytes | size + SHA-256 per file; `file_missing`, `extra_file`, `duplicate_file`, `converted_to_google_format`; deep sample re-downloads and re-hashes | whole-repo run: **286,450 / 286,450** files, **500 / 500** deep downloads |
| Additions, modifications, renames, deletions converge | diff against Drive; rename keeps the file id | `test_full_lifecycle` |
| Force-push / branch deletion leave no active stale content | forced-update detection; retired namespaces moved to `_retired/` | `test_full_lifecycle`; deletion confirmation tests |
| Missing vs inaccessible ref | targeted `ls-remote` must succeed and return nothing; PR heads also need the GitHub API | `test_pr_ref_disappearance_distinguishes_inaccessible_from_deleted`, `test_branch_probe_failure_is_inaccessible_not_deleted` |
| PR snapshot survives a branch later containing it | sticky namespaces | `test_pr_namespace_is_sticky_when_a_branch_later_contains_the_head` |
| Interrupted run recovers | state `in_progress` → crawl and converge; lost-response creates are idempotent (generated ids, 409 recovery, including resumable uploads) | `test_interrupted_sync_recovers`, `test_transient_errors_and_rate_limits_are_retried` (6/6 repeats), `test_lost_response_on_resumable_put_does_not_duplicate_or_fail` |
| Repeated sync is idempotent | no writes except lease and state | whole-repo run: second sync **0 items changed, 3 writes**; reconcile **0 changed** |
| Listing delays cannot corrupt the mirror | `incompleteSearch` is an error; verify confirms by direct GET; deletions never come from a partial listing | `test_listing_lag_is_tolerated_by_verify` |
| Mass-retirement safeguard | more than max(5, 20 %) at once → `SAFETY_STOP` | `test_mass_retire_guard_and_dry_run` |
| Flagged content never uploaded, never silent | quarantine; `verify` fails without the same quarantine file; `quarantined_content_on_drive` if uploaded earlier | 5 gate tests + whole-repo run (2 quarantined) |

Scoped verification (changed namespaces only) is cheaper and is used after incremental syncs; it cannot see drift in a namespace the sync did
not touch. The full verification (reconcile, or `verify` with no `--sync-report`) is the authority:
`test_scoped_verify_reads_far_less_and_full_verify_still_catches_drift`.

## 5. Whole-repository emulator run (frozen copy of the live remote)

The live remote moved while a first run was in progress (a new branch appeared between sync and verify), which the verifier correctly
reported as `namespace_missing`. That run is not used for idempotency evidence. The reported run uses a frozen local copy of the remote taken at
2026-11-10T20:36Z (136 heads, 259 PR heads), scanned and gated first.

| Step | Exit | Time | Writes / reads | Result |
|---|---|---:|---:|---|
| 1. initial sync | 0 | 498 s | 330,814 / 546 | 186 namespaces: 184 complete, 2 quarantined; 5,242 uploads (437 MB), 281,208 copies, 43,615 folders |
| 2. verify, deep sample 500 | 0 | 316 s | 0 / 44,491 | **PASS**: 286,450 / 286,450 files, 500 / 500 downloads, no problems |
| 3. sync again | 0 | 6 s | 3 / 8 | 184 no-ops; **0** namespace items changed |
| 4. sync `--mode reconcile` | 0 | 469 s | 556 / 44,044 | full crawl of every namespace; **0** items changed |
| 5. verify | 0 | 332 s | 0 / 43,991 | **PASS**, 286,450 files |

Reads are Drive listing calls: against real Drive the walk is the dominant cost, which is why walks are parallel and incremental runs
verify only what they changed. Real latency and quotas were not measured.

## 6. Residual limitations

* Live Google Drive behaviour unverified (quotas, error codes, checksum availability): BLOCKED.
* The 2 quarantined refs (`probe/st05-p0-probe-01`, `audit/st05-po25-review-package-20261102`) are absent from the mirror until owner decision D-6.
* Item-count headroom on a shared drive is 34 %; more branches or PR-only refs shrink it.
* Full history, and anything not a branch/tag/PR head, is out of scope (D-9).

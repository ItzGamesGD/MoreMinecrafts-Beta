# Drive mirror: deployment-readiness report (2026-11-11)

**Final status: OFFLINE READY — LIVE GOOGLE DRIVE VERIFICATION PENDING.** Not production-ready. Nothing was uploaded to Google Drive, no Drive file was read or written,
no repository variable or secret was changed, nothing was merged, and `DRIVE_MIRROR_ENABLED` was not touched.

Baseline: `claude/moreminecrafts-beta-audit-mirror-7id36x` @ `6d4beed16251874ad23c0c4b209591e6537089e0` (verified by fetch). This branch: `claude/moreminecrafts-mirror-readiness-20261111`.
Evidence: [`TEST_EVIDENCE.md`](TEST_EVIDENCE.md) §6. Operating procedure: [`RUNBOOK.md`](RUNBOOK.md).

## Findings

| # | Finding | Severity | Status |
|---|---|---|---|
| R-1 | Workflow ran `verify` after a `dry-run`; on a never-synced test namespace this fails (`mirror_root`), so the documented first step of the controlled test was red | deployment-blocking for the documented procedure | **FIXED** (`7871afc`), regression test fails on the old workflow |
| R-2 | Earlier "29/29" was 28 passed + 1 skipped without gitleaks | evidence accuracy | **FIXED** (reproduced with verified gitleaks 8.30.1: 39/39, 0 skipped) |
| R-3 | README's controlled live test would build a second ~330,000-item copy (no ref filter) and `workflow_dispatch` requires `DRIVE_MIRROR_ENABLED=true`, which also enables schedule and relays against `DRIVE_MIRROR_NAME` | procedural trap (capacity, accidental activation) | **DOCUMENTED** (README caution, RUNBOOK 3 and 9); no code change |
| R-4 | README omitted `DRIVE_MIRROR_TOOLING_REF` | documentation | **FIXED** |
| R-5 | Gate/sync race: a ref created or advanced between the scan and `sync`'s snapshot is uploaded unscanned until the next run | low-medium (content already public; next hourly run re-scans) | **OPEN, DOCUMENTED**; hardening described in README, not implemented (engine + quarantine contract change) |
| R-6 | No automated item-count (500,000) or shared-drive quota warning | medium for operations | **OPEN, DOCUMENTED**; manual procedure in RUNBOOK 11 |
| R-7 | Finding `cec8bff8` | security | **UNAPPROVED**; classified a demonstrable false positive; owner decision D-6 |

## Acceptance report

1. **Branch / HEAD:** `claude/moreminecrafts-mirror-readiness-20261111` (created from `6d4beed1`); HEAD in the final message.
2. **Files changed vs `6d4beed1`:** `.github/workflows/drive-mirror.yml` (+4/−1), `tools/drive_mirror/{README.md, RUNBOOK.md, TEST_EVIDENCE.md, READINESS_REPORT.md}`, `tools/drive_mirror/security/D-6_APPROVAL_PROPOSAL.md`, `tools/drive_mirror/tests/test_workflow.py`. No engine file changed. Commits: `7871afc` (fix + tests), `f69378d` (docs), then the evidence/report commit.
3. **Security finding `cec8bff8`:** UNAPPROVED, `proposed` in `approved_findings.json`, quarantine of `probe/st05-p0-probe-01` and `audit/st05-po25-review-package-20261102` unchanged. Re-verified as a structural false positive (scanner reproduces line 23; 9 of 9 reconstructed paths exist); narrow approval proposal in `security/D-6_APPROVAL_PROPOSAL.md`.
4. **Gitleaks test:** binary v8.30.1 verified (release checksums = workflow pins). `test_real_gitleaks_report_end_to_end`: passed. Full suite: **39 run / 39 passed / 0 failed / 0 skipped**. The unmodified baseline without the binary: 29 run / 28 passed / 0 failed / 1 skipped.
5. **Manual mirror:** implemented and verified on the emulator for branches, PR-only heads, separate namespaces, checksums, incremental and idempotent runs, resumable uploads, reconcile, deleted-vs-inaccessible refs and recovery. Not verified live. Whole-repository result is historical (snapshot 2026-11-10T20:36Z, code `c785a8df`, engine unchanged).
6. **GitHub Actions:** reviewed; one defect fixed (R-1); permissions minimal, credentials only in the `mirror` job, all four actions pinned to commit SHAs, gate before sync and fail-closed, one writer. Never executed on a runner with the feature enabled. `DRIVE_MIRROR_ENABLED` unchanged.
7. **Offline tests:** pyflakes clean; actionlint clean; 39/39; diff scope limited to `tools/drive_mirror/` and `drive-mirror.yml`.
8. **Live Google Drive:** **NOT TESTED** (no credentials or destination in this environment; the connector was not used).
9. **Remaining owner steps:** RUNBOOK Part 0 (destination, auth, environment, secrets, variables); decide D-6; run Parts 1–8 manually; then Part 9 only if satisfied.
10. **Exact next step for a controlled live test:** configure credentials and a destination, then RUNBOOK Part 1 (`preflight`, read-only first, then `--write-probe`), followed by Part 3a (scratch repository) on the real Drive.

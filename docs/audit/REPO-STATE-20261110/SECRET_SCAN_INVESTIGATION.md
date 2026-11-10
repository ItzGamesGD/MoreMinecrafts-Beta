# Secret-scan investigation (2026-11-10)

**Verdict: no confirmed exposed credential. Security readiness for live Google Drive synchronization: NOT READY**
until the repository owner decides finding `cec8bff8…` (decision D-6). Until then the mirror's security gate
quarantines the two branches that contain it and mirrors everything else. No secret value appears in this document,
in any report produced by this work, or in any log it generated.

## 1. Correction to the earlier audit

The first audit report (finding **F-12**) said the failing pre-public scan on PRs #249, #251 and #252 reported a finding in
commit `54898b74`, "not reachable from any advertised ref". **That was wrong.** I had taken a commit id from the wrong
line of the run log. The scanner's own output for all three runs is identical:

```
rule=generic-api-key  file=tools/st05-p0-probe-01/freeze_integrity.py  commit=cec8bff8edc34bfbd54aebe635260f2f0761f729
```

Run ids: 42604036681 (#249), 42615115265 (#251), 42721090327 (#252). Each reports exactly one finding. F-12 in the audit
report is corrected in place and in `REMEDIATION_REGISTER.json` (N-01).

## 2. Method

* Scanner: gitleaks **8.30.1**, the version pinned by `.github/workflows/pre-public-security-scan.yml`; the release archive was
  verified against that workflow's SHA-256 (`551f6fc8…2470eb`) before use.
* Scope: `gitleaks git --redact=100 --log-opts='--all --full-history'` over a bare cache holding **every branch (133) and every
  `refs/pull/*/head` (258)**: 2,008 commits, 160 MB, 15 s. This is the same scope as the repository's own scan.
* Reproduction of CI: the scan reads the `.gitleaksignore` of the checked-out tree. Applying the allowlist from the head of
  #252 (11 entries) leaves exactly **1** finding, the one CI reports. Applying `main`'s allowlist (2 entries) leaves **10**.
* Values: the report's `Secret` and `Match` fields are `REDACTED`; the audit tooling discards them on read. Classification used
  only structure (line shape, literal lengths and character classes, hashes of lines, existence of paths), never the value.

## 3. Findings (12)

| # | Rule / file | Commit | Reachable from | In owner allowlist | Classification | Basis |
|---|---|---|---|---|---|---|
| 1 | `generic-api-key` `tools/st05-p0-probe-01/freeze_integrity.py:23` | `cec8bff8` (2026-11-01) | `probe/st05-p0-probe-01`, `audit/st05-po25-review-package-20261102`; **no PR head** | **no** | **scanner false positive (proposed, awaiting owner)** | line 23 is inside a list of 9 identifier-like literals concatenated as `<12-char prefix> + name + '.md'`; all 9 reconstructed paths exist in that commit's tree; every name matches `[A-Za-z0-9_.-]+`; entropy 3.63 |
| 2–10 | `generic-api-key` `TERRITORY_REGISTRATION…` copies under `docs/preservation/…` and `docs/audit/BRANCH-W11-…/preserved/…` (9 fingerprints, commits `7ff1d141`, `e28ce2fb`, `31c5a52f`, `0eba0d11`, `024fc75a`) | various, 2026-10-26 | 81–83 branches / 67–71 PR heads (one only via a PR head) | yes, in wave16's `.gitleaksignore` (security commit `b9cab714`) | false positive, **byte-identical copies** | each flagged line has SHA-256 `6808756b…` = the line the owner reviewed (re-verified here by hashing every one) |
| 11 | `generic-api-key` `scripts/free_model_delegation/test_delegation.py:32` | `aa376713` | `experiment/cloud-free-model-delegation-phase0` (PR #141) | yes, on `main` (`b2f2f8f9`, reviewed 2026-10-22) | synthetic test fixture | line is `FAKE_KEY = "<28-char literal>"` in a mocked unit-test module whose docstring says no key is needed |
| 12 | `generic-api-key` `docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md:15` | `a1e0aeb0` | PR heads only | yes, on `main` | false positive (prose of links to public Fabric sources) | the original reviewed line (entropy 3.54) |

Counts: **0 confirmed credentials · 1 proposed false positive · 11 owner-reviewed (1 fixture, 10 prose false positives)**.

## 4. Why PRs #249, #251 and #252 fail

* None of the three branches contains `cec8bff8` (`git for-each-ref --contains` lists only the two branches above). Their own
  content is not the cause.
* The workflow scans *all* refs on every run, so any PR into `main` created after `probe/st05-p0-probe-01` appeared
  (2026-11-01) fails the same way; these three are simply the only PRs targeting `main` since then.
* Allowlist asymmetry (register N-02): wave16 and all later stacks carry an 11-entry `.gitleaksignore`; `main` carries 2. A **push to
  `main`** would report 10 findings today. Merging any of the stacks' allowlist to `main` without a decision on finding 1 would still fail.

## 5. Risk assessment of finding 1

* Not a credential by structure (see §3), so **no revocation or rotation is required**.
* Limits of that conclusion: a pattern scanner cannot prove absence; this assessment covers text content reachable from the refs
  above at 2026-11-10 11:34 UTC. It does not cover GitHub's own secret scanning/push protection settings (not inspected),
  binary files (NBT, images) beyond what gitleaks decodes, or refs created after the scan.
* Suppressing the finding changes a security control, so I did **not** add it to any allowlist. A patch is prepared and clearly marked
  *PROPOSED, NOT APPROVED*: `patches/security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch`.

## 6. Security gate behaviour for the Drive mirror

`python -m drive_mirror secgate` (see `tools/drive_mirror/README.md → Security gate`) was run against the real repository:

```
findings 12 · approved 11 · proposed (awaiting owner) 1 · unreviewed 0 · refs examined 394 · refs quarantined 2
quarantined: refs/heads/probe/st05-p0-probe-01, refs/heads/audit/st05-po25-review-package-20261102
```

Both tips still contain the flagged file, so both are held back; the other 131 branches and the PR-only heads are mirrored. The gate's
approved list (`tools/drive_mirror/security/approved_findings.json`) marks the 11 owner-reviewed fingerprints `approved` and finding 1
`proposed`; only `approved` passes. **Approving finding 1 is a one-line status change in a reviewed commit.**

## 7. Prerequisites to lift the block

1. Owner decision D-6 on finding 1 (approve → set its status to `approved` and add the fingerprint to the repository `.gitleaksignore`;
   reject → remove or rewrite the file on both branches, or keep them quarantined).
2. Decide where the allowlist lives for `main` (its 2-entry list no longer matches the branch set).
3. Re-run the gate immediately before the first live sync: the verdict is a function of the refs at that moment.

## 8. Reproducing

```sh
PYTHONPATH=tools python3 -m drive_mirror prepare --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --work-dir /tmp/mm.git --pull-heads all
cd /tmp/mm.git && gitleaks git --no-banner --no-color --redact=100 --log-opts='--all --full-history' --report-format json --report-path /tmp/gl.json . ; cd -
PYTHONPATH=tools python3 -m drive_mirror secgate --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --work-dir /tmp/mm.git \
    --scan-report /tmp/gl.json --quarantine-out /tmp/quarantine.json
```

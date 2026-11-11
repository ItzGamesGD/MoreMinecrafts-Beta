# D-6: proposal to approve secret-scan finding `cec8bff8` (NOT APPROVED)

**Status: PROPOSED. Nothing in this file approves, allowlists or un-quarantines anything.** `approved_findings.json` still says
`"status": "proposed"` for this fingerprint, so the gate still blocks it and still quarantines the two refs below. Only the repository
owner can approve it, in a reviewed commit.

No secret value appears here. The analysis used only structure: literal lengths and character classes, hashes of lines, and whether
paths exist. Independent re-verification: 2026-11-11, gitleaks 8.30.1 (archive SHA-256 `551f6fc8…2470eb`, equal to the release's `checksums.txt` and to both workflow pins).

## The finding

| | |
|---|---|
| Fingerprint | `cec8bff8edc34bfbd54aebe635260f2f0761f729:tools/st05-p0-probe-01/freeze_integrity.py:generic-api-key:23` |
| Rule / entropy | `generic-api-key`, 3.63 |
| Commit | `cec8bff8`, 2026-11-01, subject `fix(st05): correct B1-B3 contracts and prepare G4 integrity controls [skip ci]` |
| Reachable from | `probe/st05-p0-probe-01`, `audit/st05-po25-review-package-20261102`; no PR head |
| Effect today | the gate quarantines those two branches (not uploaded); every PR into `main` fails the pre-public scan |

## Classification: demonstrable false positive (structural evidence), awaiting owner approval

Re-verified independently of the earlier audit (scripts read the blob from the flagged commit; only booleans, lengths and hashes were printed):

1. The scanner reproduces the finding on that blob alone: rule `generic-api-key`, line 23, entropy 3.63. It is the only finding in the file.
2. Line 23 (SHA-256 prefix `4c70d31b208a`) holds **two** string literals (26 and 31 characters) inside one parenthesised tuple that feeds the list comprehension on line 21:
   `[<12-character prefix> + name + '.md' for name in (…9 names…)]`.
3. All 9 names match `[A-Za-z0-9_.-]+` (identifier-style, none hex-only).
4. For all 9 names, `prefix + name + '.md'` **is a path that exists in that commit's tree** (9 of 9; both names on line 23 included). A credential would not be a file name that exists in the repository.
5. The surrounding lines (19–31) are the same shape: lists of repository paths and path fragments assembled with `BASE + …`.

Limits: a structural argument cannot prove the absence of a secret. It covers the text of this blob and this commit; it does not cover GitHub's own secret-scanning settings
(not inspected), binary files, or refs created after the scan.

## Narrowly scoped approval, if you agree

Exactly one fingerprint; no rule, path or regex is relaxed; no other finding is touched.

1. `tools/drive_mirror/security/approved_findings.json`: set this one entry's `"status"` from `"proposed"` to `"approved"` (leave its `basis`).
2. `.gitleaksignore` (on the branch the pre-public scan reads): add the single line
   `cec8bff8edc34bfbd54aebe635260f2f0761f729:tools/st05-p0-probe-01/freeze_integrity.py:generic-api-key:23`
   (the prepared patch is `docs/audit/REPO-STATE-20261110/patches/security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch`; it is not applied anywhere).
3. Re-run Part 2 of [`../RUNBOOK.md`](../RUNBOOK.md): the gate should then report verdict `PASS` and zero quarantined refs. The quarantine lifts only through that re-run; nobody edits `quarantine.json` by hand.

**Alternative (reject):** rewrite or remove the file on both branches, or keep them quarantined; the mirror then stays knowingly incomplete by those two refs (RUNBOOK Part 2 and 8).

## What this does not decide

`main`'s `.gitleaksignore` has 2 entries versus 11 on the later stacks (register N-02, decision D-6 part 2): where the allowlist lives is a separate owner decision.

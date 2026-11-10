# Prepared patches (none applied)

These are corrections for branches this audit is not allowed to modify (protected checkpoints, other stacks).
Each patch is **prepared and checked, not applied**. Applying any of them is an owner decision (see
`REMEDIATION_REGISTER.json`, decisions D-6, D-7, D-8).

| Patch | Purpose | Generated against | Applies cleanly to (checked 2026-11-10) | Status |
|---|---|---|---|---|
| `docs/0001-structure-index-status-corrections.patch` | Appends dated corrections for PR #219, #83, #181 to `docs/STRUCTURE_AUTHORITY_INDEX.md`; historical text untouched | `checkpoint/wave16-unified-20261104` | wave16, ST-01 tips (`p8c2a`, `phase4-worldgen`, `p8b2`), `sky-mob-p2d`, `sky-tower-integration-02`; needs `git apply --3way` on `implement/d13-03-i06-p4-st01-integration-20261108` | D-7 |
| `docs/0002-content-index-research-count-correction.patch` | Appends a dated note that the 45/40/2 research counts are the 2026-10-17 record and that the current count is 43/42/2 | same | all 7 tips tested | D-7 |
| `ci/0001-…`, `ci/0002-…` | The two Colosseum commits of PR #240 (per-controller clock seam + manual-clock GameTests), cherry-picked onto `work/st01-p8c2a-complete-assembly-20261110-r1`. Also pushed as branch `remediation/st01-colosseum-clock-seam-20261110` (`f2c0a6c8`) | `c25f9d44` | that tip (identical tree to the verified run) | D-8 |
| `security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch` | Adds the fingerprint of the one residual secret-scan finding to `.gitleaksignore` | `checkpoint/wave16-unified-20261104` | wave16 | **PROPOSED, not approved: D-6** |

Check before applying: `git apply --check <patch>` (add `--3way` if it reports a context mismatch).

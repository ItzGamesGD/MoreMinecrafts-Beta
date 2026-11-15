# inactive/ — consolidated but NOT active development assets

Nothing here is compiled (Gradle source sets are `src/*`), registered, loaded as a Minecraft resource or executed by CI (workflows are read only from `.github/workflows/`; stored workflow files are data). Files are byte-identical copies of the named source refs, with two documented exceptions: one reversibly transformed file and five regenerable artifacts (see `docs/integration/RECOVERY_RUNBOOK_20261112.md`).

* `<family>/files/`, `<family>/variants/` — assets first consolidated in Phase 1.
* `<family>/state-blobs/<label>/` — additional blobs stored in Phase 2 so each recorded head state can be rebuilt (modified shared files, old-line versions, tooling).
* `RECOVERY_MANIFEST.tsv` — per state: original head SHA/tree, merge base, A/M/D delta, modes, blobs. `MANIFEST.tsv` — every stored file with blob hash and source ref/path.
* `RECIPES.json`, `TRANSFORMS.json`, `OMITTED.json` — regeneration recipes, reversible transforms, files deliberately not copied.
* `shared-file-hunks.patch` (Phase 1) — historical diffs of shared files; superseded for recovery by the full blobs but kept for readability.

Rebuild any state without the original branches: `python3 -I scripts/integration/recover_state.py --family <family> --label <label> --out <dir>`. Verify the whole tree: `python3 -I scripts/integration/check_grand_unified_isolation.py --verify-source`.

| Family | State | Original head | Head tree | Delta entries | Stored files | Stored bytes |
|---|---|---|---|---|---|---|
| client-qa-stage23b | primary | dad054c7 | bcc1fc90 | 243 | 29 | 585551 |
| docs-alchemy-plot-notes | primary | cc1a96b0 | 6b4ef3cb | 1 | 0 | 0 |
| docs-content-first-axis-review | primary | 8bf157f7 | ee336b0f | 3 | 1 | 115614 |
| docs-pyramid-mob-roster | primary | a86c6d5b | bd2e7cd7 | 1 | 0 | 0 |
| docs-sky-islands-design-context | primary | 2fba754b | 63f1ffdb | 2 | 2 | 61339 |
| docs-spider-colony-biome | primary | 4aabb33e | e648880e | 1 | 0 | 0 |
| docs-ui-piglin-mob-tracker | primary | 6fe1a308 | 98618086 | 1 | 0 | 0 |
| docs-vanilla-roster-recovery | primary | cf4360b2 | 84084384 | 1 | 1 | 5658 |
| docs-xp-quest-ui-checkpoint | primary | a124ccfc | ba38acb9 | 1 | 0 | 0 |
| drive-mirror | primary | 79f10059 | aab85230 | 59 | 55 | 1507675 |
| prc-p7-security-audit | primary | 20eb1d8e | 474eb651 | 4 | 3 | 23080 |
| q1c-wither-skull | primary | 9c9f2257 | 54b0bf99 | 386 | 48 | 1068329 |
| sky-mob | primary | bd5ba43f | 0f56209a | 80 | 88 | 938632 |
| sky-mob | variant-gametest-timing | 9a54ae0b | 36ef80d6 | 41 | 88 | 938632 |
| sky-mob | variant-p2b-p2c | 55884b22 | ae614be1 | 74 | 88 | 938632 |
| st01-o14-p7-p11 | primary | 7e37b786 | 3f2b7071 | 170 | 170 | 3243577 |
| st01-phase4 | primary | ada10ba3 | 720086eb | 23 | 21 | 217125 |
| st01-terrain-adaptation | primary | 357fc936 | af4b323d | 168 | 168 | 6479193 |
| st05-controller-structure-mask | primary | 1ee08d44 | 99eca1a6 | 41 | 41 | 396495 |
| st05-owner-decisions-2026-10-28 | primary | dd193d78 | b08d376c | 2 | 1 | 6335 |
| star-covenant-i04a | primary | df673dfe | 34cfa38d | 2 | 1 | 4739 |
| validation-st05-p0-probe | primary | 11c09064 | 8e5871c7 | 57 | 52 | 834103 |
| validation-st05-stage0-probes | primary | 838e1366 | 37b78134 | 47 | 47 | 279618 |
| validation-underground-kingdom-probe | primary | 991b20c8 | edd42ec3 | 246 | 22 | 449286 |

## Deliberately not copied (kept on their source refs)

| Family | Path | Reason |
|---|---|---|
| prc-p7-security-audit | `docs/audits/prc-p7d/PRC-P7D-SECURITY-REMEDIATION-VERIFICATION-20261028.md` | contains complete gitleaks fingerprint / ignore-entry proposal |
| drive-mirror | `docs/audit/REPO-STATE-20261110/evidence/secgate_result.json` | contains complete gitleaks fingerprint / ignore-entry proposal |
| drive-mirror | `docs/audit/REPO-STATE-20261110/patches/security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch` | contains complete gitleaks fingerprint / ignore-entry proposal |
| drive-mirror | `tools/drive_mirror/security/D-6_APPROVAL_PROPOSAL.md` | contains complete gitleaks fingerprint / ignore-entry proposal |
| drive-mirror | `tools/drive_mirror/security/approved_findings.json` | contains complete gitleaks fingerprint / ignore-entry proposal |

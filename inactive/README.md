# inactive/ — consolidated but NOT active development assets

Nothing in this directory is compiled (Gradle source sets are `src/*`), registered, loaded as a Minecraft resource, or executed by CI (workflows are read only from `.github/workflows/`). Files are byte-identical copies of the named source refs, at their original relative paths under `files/`. `MANIFEST.tsv` lists every file with its blob hash and source ref. See `../docs/integration/FEATURE_STATUS_REGISTRY_20261112.md` for status, authority and blockers.

`shared-file-hunks.patch` records the source branch's own changes to files that also exist in the baseline (registries, tags, lang files, indexes). They are historical hunks, not a ready-to-apply patch: the baseline has often evolved since the merge base. They are the exact places where activation would touch active code.

| Family | Primary source ref | SHA | Files | Bytes |
|---|---|---|---|---|
| sky-mob | work/sky-mob-tower-integration-20261111 | bd5ba43f | 55 | 295133 |
| st01-o14-p7-p11 | work/st01-o14-phase3-1-validation-20261109 | 7e37b786 | 158 | 2925230 |
| st01-phase4 | claude/st01-pyramid-phase4-worldgen-roke2l | ada10ba3 | 11 | 147225 |
| st01-terrain-adaptation | research/st01-p8c3a-terrain-adaptation-20261111 | 357fc936 | 167 | 6432777 |
| st05-controller-structure-mask | feat/st05-c4-structure-mask-20261030 | 1ee08d44 | 35 | 332239 |
| q1c-wither-skull | codex/q1c-pr128-p1-p2-repair-20261019 | 9c9f2257 | 9 | 57294 |
| client-qa-stage23b | codex/stage23b-cloud-live-client-qa | dad054c7 | 5 | 71191 |
| validation-underground-kingdom-probe | experiment/underground-kingdom-worldgen-probe | 991b20c8 | 15 | 380672 |
| validation-st05-stage0-probes | validation/st05-stage0-a02-a03-20261030 | 838e1366 | 45 | 251577 |
| validation-st05-p0-probe | audit/st05-po25-review-package-20261102 | 11c09064 | 8 | 460192 |
| drive-mirror | claude/moreminecrafts-mirror-gate-race-20261111 | 79f10059 | 45 | 1178232 |

Variants (Sky Mob): `claude/sky-mob-p2b-p2c-vwuur8@55884b22` (8 files), `claude/sky-mob-gametest-timing-nn7o1h@9a54ae0b` (3 files)

## Excluded from the tree (kept on their source refs)

| Family | Path | Reason |
|---|---|---|
| validation-st05-p0-probe | `tools/st05-p0-probe-01/freeze_integrity.py` | gitleaks-fingerprinted-file |
| drive-mirror | `docs/CURRENT_STATE.json` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/REMEDIATION_REGISTER.json` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/REMEDIATION_REGISTER.md` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/REPORT.md` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/SECRET_SCAN_INVESTIGATION.md` | sensitive-filename |
| drive-mirror | `docs/audit/REPO-STATE-20261110/current_state_facts.json` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/evidence/secgate_result.json` | pattern:generic-api-key |
| drive-mirror | `docs/audit/REPO-STATE-20261110/evidence/secret_scan_findings_metadata.json` | sensitive-filename |
| drive-mirror | `docs/audit/REPO-STATE-20261110/patches/security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch` | sensitive-filename |
| drive-mirror | `tools/drive_mirror/security/D-6_APPROVAL_PROPOSAL.md` | pattern:generic-api-key |
| drive-mirror | `tools/drive_mirror/security/approved_findings.json` | pattern:generic-api-key |
| drive-mirror | `tools/drive_mirror/tests/test_gate_race.py` | pattern:generic-api-key |
| drive-mirror | `tools/drive_mirror/tests/test_hardening.py` | pattern:generic-api-key |
| validation-st05-p0-probe | 48 files under `tools/st05-p0-probe-01/` and one workflow | external: ~37 MB probe inputs/oracle, REQUIRES_ISOLATION (EXT-01) |

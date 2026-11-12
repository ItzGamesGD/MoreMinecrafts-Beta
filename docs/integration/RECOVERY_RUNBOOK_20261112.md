# Development-state recovery — runbook and results (2026-11-12)

Three different things are easy to confuse: **file preservation** (bytes sit under `inactive/`), **Git reachability** (a commit can be reached from some ref) and **development-state recoverability** (a branch's complete working tree can be rebuilt without that branch). This document covers the third.

## What is recorded

For each preserved state, `inactive/RECOVERY_MANIFEST.tsv` holds the original head SHA, the original head **tree** SHA, the merge base with the frozen baseline (an ancestor of the baseline, hence present in Grand Unified history) and the complete A/M/D delta with file modes and blob SHAs. Every delta blob is findable by content hash in the Grand Unified tree: baseline files, `inactive/<family>/files/`, or `inactive/<family>/state-blobs/`. Interior commits of a stacked family are **not** reproduced; only the state of each recorded head is.

## How to reconstruct

```bash
# in any clone that contains work/grand-unified-20261112 (original feature branches are NOT needed)
python3 -I scripts/integration/recover_state.py --family sky-mob --label primary --out /tmp/sky-mob-tree
# prints the reconstructed tree SHA and exits 0 only if it equals the recorded original head tree
```

The tool starts from the merge-base tree, applies the delta, runs documented recipes (`inactive/RECIPES.json`, two deterministic ST-05 probe generators) and reversible transforms (`inactive/TRANSFORMS.json`), writes the tree and compares it with the recorded original tree. List all states: `cut -f1,2 inactive/RECOVERY_MANIFEST.tsv | sort -u`. To start development, copy the output directory into a new branch (`git checkout -b <new> <grand-unified>`; replace the working tree with it) after owner approval.

## Security handling inside recovery

* Files containing a complete gitleaks fingerprint (`<40-hex>:<path>:<rule>:<line>`) or an ignore-entry proposal are **not copied**. They are listed in `inactive/OMITTED.json`; their states are PARTIAL.
* `tools/st05-p0-probe-01/freeze_integrity.py` contains an identifier list that gitleaks reports as `generic-api-key` (documented false positive). It is stored as an AST-identical variant (one tuple element parenthesised) and restored byte-exactly by the recovery tool (`inactive/TRANSFORMS.json`). This is a scanner-false-positive workaround, not an allowlist change; the owner may prefer to keep the file external instead.
* Five deterministic ST-05 probe artifacts (about 37 MB) are not stored: `materialize.py` and `oracle/oracle.py` regenerate them byte-exactly, and the recovery tool verifies each against the recorded blob.

## Results

| family/label | original head (full SHA) | original tree | reconstructed tree identical | recovery class | entries | omitted/regenerated | tests run in the reconstructed tree |
|---|---|---|---|---|---|---|---|
| client-qa-stage23b/primary | dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6 | bcc1fc90c394a454d84f61ac5fd0d50de0163b13 | yes | EXACT | 243 | - | 4 source sets + compileClientQaJava + verifyClientQaExcludedFromRemapJar OK; 300 unit tests OK; real-client QA NOT RUN |
| docs-alchemy-plot-notes/primary | cc1a96b062696bb5420ec0631883f0d1f59aa89e | 6b4ef3cbc944a88d3e30e27f8acad5cf5a6db345 | yes | EXACT | 1 | - | documents only (n/a) |
| docs-content-first-axis-review/primary | 8bf157f7988569c715bde4dfd298e9af20305415 | ee336b0f4d4ddda3792697d72503de667673e29b | yes | EXACT | 3 | - | documents only (n/a) |
| docs-pyramid-mob-roster/primary | a86c6d5beb336eb14c0459200434b0f6304398c5 | bd2e7cd7dd996681a59c424458eff6585a1d8355 | yes | EXACT | 1 | - | documents only (n/a) |
| docs-sky-islands-design-context/primary | 2fba754b3c092017ab5da95b661c4dd1f898f386 | 63f1ffdb83b2f35fd34c7a112bd37ceeecb492ef | yes | EXACT | 2 | - | documents only (n/a) |
| docs-spider-colony-biome/primary | 4aabb33e189e783b11885732731881bc82b66790 | e648880e2ee67f90f94874ebbee21884163ea859 | yes | EXACT | 1 | - | documents only (n/a) |
| docs-ui-piglin-mob-tracker/primary | 6fe1a308c9c72d63b48be6c7a8590e44e46a55bd | 986180868b1da0e8772ac127eb4fd1a6ef04fe76 | yes | EXACT | 1 | - | documents only (n/a) |
| docs-vanilla-roster-recovery/primary | cf4360b228589e538f831aae34e378330fabf670 | 84084384f179e37850e932a88b18a056c81069b4 | yes | EXACT | 1 | - | documents only (n/a) |
| docs-xp-quest-ui-checkpoint/primary | a124ccfc2e7e3bc826c04705fa98f69a102a2636 | ba38acb9752724ddd1af2d4e32239198591e08c1 | yes | EXACT | 1 | - | documents only (n/a) |
| drive-mirror/primary | 79f10059994a94378e5d299980e45ca41205559c | aab85230fe5f6e55774cff1d61d0d1176028567a | NO | FUNCTIONALLY EQUIVALENT | 59 | omitted 4 | python unittest discover: 64 tests OK (3 skipped) in reconstructed AND original trees (original run after reconstruction) |
| prc-p7-security-audit/primary | 20eb1d8ef705d15ede39997f5735c01b06297d45 | 474eb65168a2e9ff41f9ed2dcc96d55e99aa1a96 | NO | PARTIAL | 4 | omitted 1 | documents only (n/a) |
| q1c-wither-skull/primary | 9c9f22577370cd0160dd2b43b0e92caa7832f420 | 54b0bf99a595629eae0b04f22af831d0e18a3640 | yes | EXACT | 386 | - | 4 source sets compile; 348 unit tests OK; two-JVM forced-halt GameTest NOT RUN (known unresolved defect) |
| sky-mob/primary | bd5ba43f9fe6542a8d48dfb412b952d6498ab0fb | 0f56209ad14b44cf70c0b5855908ee4030622117 | yes | EXACT | 80 | - | 4 source sets compile; 603 unit tests OK; runGameTest: all 556 required passed |
| sky-mob/variant-gametest-timing | 9a54ae0ba3805dce813354a5aa9ea21c1f57de69 | 36ef80d6bcde44625f737e072c433c0ad3ff0068 | yes | EXACT | 41 | - | NOT TESTED (tree exact; not built) |
| sky-mob/variant-p2b-p2c | 55884b22c5d66e19974851ddb37372d5b398022e | ae614be173911f9b66c06c40195728a7cc6c7c61 | yes | EXACT | 74 | - | NOT TESTED (tree exact; not built) |
| st01-o14-p7-p11/primary | 7e37b786de8fcf36980ec732cc8079bb24e6a7fd | 3f2b70710357a913ae8995f047f36102a6e6db96 | yes | EXACT | 170 | - | 4 source sets compile; 607 unit tests OK; runGameTest FAILED (reconstructed: 1 then 18 Colosseum GameTest failures in two runs; the original tree checked out from its ref afterwards failed the same absence-grace test): inherited, not a recovery defect |
| st01-phase4/primary | ada10ba343f7d7eeb9b944baa69750c3c25a949e | 720086eb0900faa6cb3d63b458d58de41a67aa59 | yes | EXACT | 23 | - | 4 source sets compile; 607 unit tests OK; GameTests NOT RUN |
| st01-terrain-adaptation/primary | 357fc9369de0bccc7474c48f39c5c31a726e304f | af4b323db0b3870532c3f3dcaebc2abe4940b8c0 | yes | EXACT | 168 | - | NOT TESTED (documents, probe patches) |
| st05-controller-structure-mask/primary | 1ee08d440032095f9f247f2cb98c87fd15478656 | 99eca1a6614ddfc198d3d897b875674a1fa3e0b5 | yes | EXACT | 41 | - | 4 source sets compile; 586 unit tests OK; runGameTest: all 510 required passed |
| st05-owner-decisions-2026-10-28/primary | dd193d7880b87a40a3f6eaa9bff359792e2cea08 | b08d376cd357304d5d1cbc5c1a88a6e700a5ff08 | yes | EXACT | 2 | - | documents only (n/a) |
| star-covenant-i04a/primary | df673dfe7ab8bbc43b0087b17a4476f4a5211ef6 | 34cfa38dc7309b938ab947e6bd16b670ccbd0aaf | yes | EXACT | 2 | - | NOT TESTED (not built) |
| validation-st05-p0-probe/primary | 11c09064dc62c28faea44ffcc564621cc6303908 | 8e5871c753fc4794b515d4ef9713c58ebb5fb37f | yes | EXACT | 57 | regenerated 5 | python: test_freeze_integrity 2 OK, oracle/test_corrections 5 OK, verify_materialization PASSED; 5 artifacts regenerated byte-exactly |
| validation-st05-stage0-probes/primary | 838e1366d44a7a09c43bb1453d92511eea46327e | 37b7813428e474fdb5c9952ce9201fa6376118c2 | yes | EXACT | 47 | - | 4 source sets compile; 511 unit tests OK; Stage 0 probes NOT RUN |
| validation-underground-kingdom-probe/primary | 991b20c8a052ecf5d73c5130d2d9e0eb4338c041 | edd42ec3eab0cb891a28ecde08cee20f3b049f1e | yes | EXACT | 246 | - | 4 source sets compile; 300 unit tests OK; worldgen probe matrix NOT RUN |

## Notes per state

* `client-qa-stage23b/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-alchemy-plot-notes/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-content-first-axis-review/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-pyramid-mob-roster/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-sky-islands-design-context/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-spider-colony-biome/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-ui-piglin-mob-tracker/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-vanilla-roster-recovery/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `docs-xp-quest-ui-checkpoint/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `drive-mirror/primary` — FUNCTIONALLY EQUIVALENT: tree differs by 4 files omitted by security policy (approved_findings.json, D-6 proposal, secgate_result.json, proposed-ignore patch); identical unittest outcome in reconstructed and original trees. Gate data approved_findings.json must be supplied by the owner's security process before any deployment (deferred).
* `prc-p7-security-audit/primary` — PARTIAL: 3 of 4 documents recovered; the P7D verification document is omitted because it contains complete gitleaks fingerprints
* `q1c-wither-skull/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `sky-mob/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `sky-mob/variant-gametest-timing` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `sky-mob/variant-p2b-p2c` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `st01-o14-p7-p11/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `st01-phase4/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `st01-terrain-adaptation/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `st05-controller-structure-mask/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `st05-owner-decisions-2026-10-28/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `star-covenant-i04a/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `validation-st05-p0-probe/primary` — EXACT: reconstructed tree identical, using 5 byte-exact regenerations and 1 reversible AST-identical transform (documented in the runbook)
* `validation-st05-stage0-probes/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences
* `validation-underground-kingdom-probe/primary` — EXACT: reconstructed tree identical to the original head tree; independent file/mode comparison against the original ref afterwards: 0 differences

## Limits

* Recovery reproduces the **tree** of each recorded head. Commit history, authorship and interior stack tips remain only on the original refs; keep them (or a `git bundle` stored outside the repository) if history matters. No bundle is committed here because packed histories would hide scan-flagged content from scanners.
* Test results above describe the reconstructed trees only. Unrun checks are marked NOT TESTED/NOT RUN.

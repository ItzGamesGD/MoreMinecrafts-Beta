# PRC-P7D security remediation verification

**Verdict: BLOCKED_CI.** The seven proposed exceptions are independently verified as exact historical prose false positives. The existing security-only Draft PR #200 is the minimal integration candidate and its exact HEAD passed Fabric CI. Its exact HEAD has no completed repository full-history security workflow run; the local Gitleaks run passes. PR #182 remains at its old, failing HEAD. No protected ref was moved.

Review performed 2026-10-27 UTC. The report filename follows the requested 20261028 artifact name; it does not assert that the review took place on October 28.

## Authority and current baseline

Read `AGENTS.md`, `docs/CONTENT_AUTHORITY_INDEX.md`, `.gitleaksignore`, `.github/workflows/pre-public-security-scan.yml`, PRs #182, #197, #199, #200, #201, the P7B and P7C reports, and the historical `docs/security/GITLEAKS_REMEDIATION_2026-10-22.md` at PR #157 HEAD `cce99b6cc7bb84c03fd8c4d7fe2a58c309ec4b43`. The repository has no custom `.gitleaks.toml`; the workflow uses Gitleaks defaults plus `.gitleaksignore`.

| PR | State and relationship at review | Exact HEAD |
| --- | --- | --- |
| #182 | Open Draft; base `main`; source `integration/wave10-unified-20261025` | `73dcc8f4d139c1688352068b397dab040208253b` |
| #197 | Open Draft; audit based on Wave 12 checkpoint | `04c09ea121891834b9166c1804de2d5a16464fd7` |
| #199 | Open Draft; P7B audit based on #197's source | `66d2b52995bd6da909cb1ce9539db5cb175f6d15` |
| #200 | Open Draft; security remediation based on #182's source branch | `d4ca6a4956388805b49c38a646cd5cb22f857eb6` |
| #201 | Open Draft; P7C audit based on #199's source | `84c3eadcdde4aac6ef981870788b66ddf16d3e8e` |

Remote Git refs and GitHub PR metadata agreed for #182 and #200. GitHub comparison of `73dcc8f4...d` to `d4ca6a49...b6` returned one commit ahead, zero behind, merge base exactly #182's HEAD, and only `.gitleaksignore` changed. #200 is already the equivalent Draft PR targeting the correct source branch; no duplicate remediation PR was created and #200 was not modified.

## Seven-finding verification matrix

All seven entries have rule `generic-api-key`, the exact `commit:path:rule:line` fingerprint shown, a source-line SHA-256 of `6808756b4fb7291f778a23ccd2771325f1ee1f7461c900d3bc8392115766c242`, historical classification **non-secret technical prose copied from the reviewed territory design document**, exact-finding-only scope, and verdict **VERIFIED**. F01–F07 are stable redacted IDs; matched candidate strings are not recorded here.

| ID | Commit | Exact historical file | Line | Exact fingerprint | Verdict |
| --- | --- | --- | ---: | --- | --- |
| F01 | `7ff1d14156730373d28973665d269bcb5f9ae39d` | `docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 | `7ff1d14156730373d28973665d269bcb5f9ae39d:docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md:generic-api-key:15` | VERIFIED |
| F02 | `7ff1d14156730373d28973665d269bcb5f9ae39d` | `docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 | `7ff1d14156730373d28973665d269bcb5f9ae39d:docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md:generic-api-key:15` | VERIFIED |
| F03 | `e28ce2fb8491dc7b7aade3ac51b52ab0a37542d7` | `docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 | `e28ce2fb8491dc7b7aade3ac51b52ab0a37542d7:docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md:generic-api-key:15` | VERIFIED |
| F04 | `e28ce2fb8491dc7b7aade3ac51b52ab0a37542d7` | `docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 | `e28ce2fb8491dc7b7aade3ac51b52ab0a37542d7:docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md:generic-api-key:15` | VERIFIED |
| F05 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a` | `docs/audit/BRANCH-W11-20261027/preserved/05a8cab78753a5dae3080d2c54bfc09ea8d5b63b.md` | 15 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a:docs/audit/BRANCH-W11-20261027/preserved/05a8cab78753a5dae3080d2c54bfc09ea8d5b63b.md:generic-api-key:15` | VERIFIED |
| F06 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a` | `docs/audit/BRANCH-W11-20261027/preserved/04206d024522669763567035f2d00e9590501ed3.md` | 15 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a:docs/audit/BRANCH-W11-20261027/preserved/04206d024522669763567035f2d00e9590501ed3.md:generic-api-key:15` | VERIFIED |
| F07 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a` | `docs/audit/BRANCH-W11-20261027/preserved/f2898e2baacd03f4ae045c7f77dd91af190c600d.md` | 17 | `31c5a52f9aca386c64467c81c8f83a2cc1c6579a:docs/audit/BRANCH-W11-20261027/preserved/f2898e2baacd03f4ae045c7f77dd91af190c600d.md:generic-api-key:17` | VERIFIED |

The original source is `a1e0aeb0ab8ba90419b714a42502cbe8db9d770c:docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md:15`. Independently reading each historical Git object showed all eight complete source lines, including the original, byte-identical (516 bytes; same SHA-256). The original historical security review classifies the detected 19-character natural-language compound as non-secret prose, not a URL, code literal, token, or credential; it appears in a technical paragraph referencing public Fabric source material. The surrounding document is a proposed territory specification. Neither the line nor its copied instances serve as authentication material. External service-side usage records were unavailable; the non-credential classification rests on the text's structure, context, complete byte identity, and prior source review, not on an unverified claim about account activity. No potentially real credential was identified.

## Security diff and scan scope

The isolated test worktree was created from exact #182 HEAD with `git worktree add -b security/prc-p7d-review-20261027 /tmp/prc-p7d-review 73dcc8f4d139c1688352068b397dab040208253b`. The original `.gitleaksignore` SHA-256 was `9a7cec214d3eaba9e6a09d69b830b1cabe7277b7fde31f821578d75deb900b75`; its four existing fingerprints were preserved. The tested file was transferred directly from `d4ca6a4956388805b49c38a646cd5cb22f857eb6:.gitleaksignore`; proposed SHA-256 `6ae4d32caecb1070d40e11064d7d9e0f853f10702ca9b78335368889d3b1dd52`. Diff: 11 additions, zero deletions, seven exact fingerprints and provenance comments, with no broad rule, path, regex, or global exclusion. The existing security workflow SHA-256 stayed `3066ba02031c32fab495de7ee96b8698930817b3aeff4481dc0a22ba8f07e549`. No security workflow, gameplay file, history, or protected ref was modified. The exact seven added entries equal the baseline scan fingerprint set, with no additional entry.

The workflow fetches every branch, tag and PR head, then runs `gitleaks git --no-banner --no-color --redact=100 --log-opts='--all --full-history' --report-format json --report-path "$report" .`. This local reproduction fetched 150 remote branch refs, 201 PR head refs, zero tags, and covered 1,626 reachable commits. Gitleaks archive `gitleaks_8.30.1_linux_x64.tar.gz` passed the workflow's SHA-256 check `551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`; `gitleaks version` returned `8.30.1`.

| Evidence lane | Exact command or method | Result |
| --- | --- | --- |
| LOCAL_SECURITY_SCAN baseline | Above Gitleaks command on #182 worktree with original ignore file | Exit 1; exactly F01–F07, all `generic-api-key`; no other findings. |
| LOCAL_SECURITY_SCAN remediation | Same command, same refs and worktree, with only #200's ignore file substituted | Exit 0; zero findings. |
| Exact fingerprint isolation | Remove each proposed line individually, rerun the same full-history scan, restore file | Seven of seven removals each restored exactly one finding: its own fingerprint; each run exited 1. |
| Negative control | In a separate temporary Git repository, commit an unrelated randomly generated synthetic `api_key` fixture plus the proposed ignore file, then run the same pinned scanner with `--all --full-history` | Exit 1; exactly one `generic-api-key` finding in the synthetic fixture. The synthetic string is neither published nor stored in this report. |
| Configuration comparison | Compare original and proposed `.gitleaksignore` entries and workflow hash; `git diff --check` | Four prior entries preserved; precisely seven added exact entries; no workflow change; diff check passed. |
| Gameplay scope | GitHub exact-commit comparison #182 → #200 | One changed file: `.gitleaksignore`; 11 additions, zero deletions. |

The local full-history report and logs are kept outside the repository under `/tmp/p7d-*`; matched values are not included in this report or any published output. The negative control establishes that an unrelated generic finding remains detectable. The seven removal runs establish that each exception has only its intended observed effect in this history snapshot.

## Exact-HEAD CI and integration gate

| Lane | HEAD | Run and jobs | Result |
| --- | --- | --- | --- |
| REMEDIATION_BRANCH_CI, Fabric | #200 `d4ca6a4956388805b49c38a646cd5cb22f857eb6` | Run `40516088040`: build `122230273722`, server-smoke `122231518951`, client-smoke `122232865026` | Completed success on the final #200 HEAD. |
| REMEDIATION_BRANCH_CI, full-history security workflow | #200 `d4ca6a4956388805b49c38a646cd5cb22f857eb6` | No run returned for this SHA | **NOT RUN**. The workflow's PR trigger targets `main`, while #200 targets #182's integration branch; the push trigger also excludes #200's branch. Local scan does not substitute for an Actions result. |
| PR182_CURRENT_HEAD_CI, Fabric | #182 `73dcc8f4d139c1688352068b397dab040208253b` | Run `40480578141`: build `122109560560`, server-smoke `122110657753`, client-smoke `122111827415` | Completed success on #182's unchanged HEAD. |
| PR182_CURRENT_HEAD_CI, full-history security workflow | #182 `73dcc8f4d139c1688352068b397dab040208253b` | Run `40480578071`, scan job `122109560906` | Completed failure on scan step; historical seven findings. |

There is no later commit on either reviewed source branch in the final remote HEAD recheck. No #182 security pass is claimed. The exact proposed protected-ref update, **only after separate explicit owner approval**, is a non-force, expected-old-SHA fast-forward of `integration/wave10-unified-20261025` from `73dcc8f4d139c1688352068b397dab040208253b` to the existing #200 commit `d4ca6a4956388805b49c38a646cd5cb22f857eb6`, after re-fetching both refs and verifying the same one-file diff. That update would trigger #182's applicable exact-HEAD security workflow; its result must be observed before claiming #182 gate recovery. Do not merge #200, merge #182, modify checkpoints, or move #182 based solely on this report.

**Owner decision required:** review this evidence and decide separately whether to authorize that exact expected-SHA fast-forward. Because the remediation branch lacks its own completed security workflow run, the present verdict is **BLOCKED_CI**, not `READY_FOR_OWNER_INTEGRATION_APPROVAL`. A completed exact-HEAD security run on #200, or another owner-approved CI path that preserves the workflow and scan scope, is needed to clear this pre-integration gap. The later #182 run remains a separate release gate even if this candidate becomes ready.

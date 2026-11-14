# PRC-P7B-TARGETED-CLOSURE-AND-SECURITY-EVIDENCE-20261027

Audit-only descendant of PR #197 (`04c09ea121891834b9166c1804de2d5a16464fd7`). No PR was closed, no checkpoint moved, and no security control changed.

## Current baseline

Fetched GitHub PR and branch refs. #83 is OPEN at `e7dc53f9f48dfece8a5723debd5526dc061107d1` (base `stage-20/checkpoint`); #182 OPEN at `73dcc8f4d139c1688352068b397dab040208253b`; #192 OPEN at `c47c69f3e3553819432905197599443288669bb5` (base `main`); #193 OPEN and `checkpoint/wave12-unified-20261027` both at `d341a0656450f74ae16d78c75f7a981002a54692`; #197 OPEN at `04c09ea121891834b9166c1804de2d5a16464fd7`. New #198 at `f17188973d1c567c4505bd00f64e1829841d44f9` concerns T2 Wave AB, not either source branch. An open-PR base query found no child based on #83's or #192's head branch. This is the latest fetched state, not a closure-time recheck.

## Track A — #83: READY_TO_CLOSE_CANDIDATE

| Evidence | Verified result |
|---|---|
| Source/checkpoint | #83 `e7dc53f9`; Wave 12 `d341a065` |
| Ancestry | #83's five commits (`5d4e5196`, `04c2986a`, `41c429e8`, `64bfacec`, `e7dc53f9`) were cherry-picked with `-x` as Wave 12 `6bb483e1`, `64ffec31`, `d8a8f386`, `ef3e7054`, `209c251e`; source head is not assumed an ancestor. |
| Production | Exact blob matches for `StructureTemplatePoolMixin.java` (`9dec9603`), `ArenaWorldgenProbe.java` (`15b367b6`), `StructurePoolContributions.java` (`0f0993f3`), `StructurePoolExtension.java` (`c68ddf02`), arena template NBT (`4620400d`), and template-pool JSON (`d848dd99`). The four changed registration files differ as whole blobs due to later additions; targeted token counts match source and checkpoint: client harness 1, server GameTest 1, pool initializer 2, mixin 1. The source contribution adds no separate loot table, trigger, or entity file; the identical template and pool code are the scoped generation behavior. |
| Tests/tools | `AncientCityArenaGameTests.java` (`9a9c54b8`), visual harness (`dfa41766`), capture/check scripts, generator, validator, and requirements all blob-identical. Workflow differs only by adding Wave 12 integration/checkpoint branches to the push filter; test steps remain source-equivalent. |
| Documents/visual evidence | Prototype document (`07f2d5f6`) and both original PNGs (`5cd84821`, `d1e848c6`) are blob-identical. Wave 12 adds independent real-worldgen logs and two native capture derivatives under `docs/audits/wave12-20261027/arena-local/`. Original paths remain preserved. |
| Authority | `STRUCTURE_AUTHORITY_INDEX.md` routes the user-approved first Ancient City arena prototype to #83 and keeps it separate from ST-05. Current checkpoint uses the same weight-1, rigid, vanilla-degradation template behavior. No conflict identified. |
| Dependencies | No open PR targets #83's source branch as base in the current query. |
| Targeted validation | NOT RUN in P7B: no code changed; source and checkpoint test blobs are identical. Wave 12's recorded validation is 425/425 server GameTests, arena selection and real-terrain/native-client capture. This is historical evidence on the checkpoint, not a new P7B run. Human visual/feel acceptance remains pending and is separate from whether this prototype source PR has unique work. |
| Preservation gaps | None among 22 source-changed paths. No source-only gameplay or evidence file identified. |

Smallest action: no preservation change. Owner may approve closing #83 unmerged after a fresh HEAD/state/dependency check and a comment identifying Wave 12 and the original blob evidence. Retain the source ref; branch deletion is a separate decision. Closure does not assert final visual/feel acceptance.

## Track B — #192: READY_TO_CLOSE_CANDIDATE

| Evidence | Verified result |
|---|---|
| Source/checkpoint | #192 `c47c69f3`; #193 and Wave 12 `d341a065` |
| Ancestry | `git merge-base --is-ancestor c47c69f3 d341a065` returned success. Wave 12 has 13 commits after #192. |
| Production | Ancestry retains all #192 code and commits. Wave 12 adds Arena work and regression tests; it does not remove the #192 commit history. This is commit preservation, not a claim that #192 itself integrated every historical source. |
| Tests/tools | All #192 files remain reachable by ancestry. No `docs/audit` or `docs/preservation` path changed between the two trees. |
| Documents/evidence | Exhaustive path-and-blob comparison of the 208 #192 files under `docs/audit` and `docs/preservation` against Wave 12 found **0 mismatches**. This includes `docs/audit/BRANCH-W11-20261027/REPORT.md`, `branch_inventory.json` (144 entries), `preservation_manifest.json` (89 entries), preserved payloads, PRC-P6 provenance and `.gitleaksignore` through ancestry. Thus the original Git blobs remain addressable by path and SHA. |
| Authority | #192 is an audit/checkpoint, not new gameplay authority. Current index and Phase 2 owner records are retained by ancestry. |
| Dependencies | No open PR targets `checkpoint/wave11-unified-20261027` as base in the current query. The branch is a protected historical checkpoint and should remain even if its PR is closed. |
| Targeted validation | PASS: Git ancestry and 208 exact blob comparisons. Runtime tests NOT RUN because this audit changes no runtime code. |
| Preservation gaps | None found in the historical audit/preservation tree. |

Smallest action: no preservation change. Owner may approve closing #192 unmerged after a fresh HEAD/state/dependency check and a comment linking Wave 12 plus the 144-entry inventory and preserved manifest. **Do not delete or move the Wave 11 checkpoint branch.**

## Track C — #182 seven-finding recovery

The Actions log redirect returned `Forbidden`; the check-run output had no summary, and check-run annotations exposed only generic workflow messages. Run `40480578071` listed no downloadable finding artifact. Job `122109560906` confirms that checkout, full-ref fetch and pinned Gitleaks installation passed; the scan step failed. No log endpoint was retried after the permission failure.

Independent reproduction succeeded. The repository workflow pins Gitleaks **8.30.1** with archive SHA-256 `551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`. The same verified binary, `--all --full-history`, and #182's `.gitleaksignore` at the exact #182 checkout returned **7** `generic-api-key` findings. The 7 finding identities, without values, are:

| Commit | Historical path | Line |
|---|---|---:|
| `7ff1d141` | `docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 |
| `7ff1d141` | `docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 |
| `e28ce2fb` | `docs/preservation/prc-p6/territory-versions/PR132/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 |
| `e28ce2fb` | `docs/preservation/prc-p6/territory-versions/PR134/TERRITORY_REGISTRATION_FABRIC_SPEC.md` | 15 |
| `31c5a52f` | `docs/audit/BRANCH-W11-20261027/preserved/05a8cab78753a5dae3080d2c54bfc09ea8d5b63b.md` | 15 |
| `31c5a52f` | `docs/audit/BRANCH-W11-20261027/preserved/04206d024522669763567035f2d00e9590501ed3.md` | 15 |
| `31c5a52f` | `docs/audit/BRANCH-W11-20261027/preserved/f2898e2baacd03f4ae045c7f77dd91af190c600d.md` | 17 |

For each of the seven, the matched line SHA-256 is `6808756b4fb7291f778a23ccd2771325f1ee1f7461c900d3bc8392115766c242`, exactly the same as the original reviewed line in commit `a1e0aeb0`, `docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md:15`. #192's `.gitleaksignore` documents the original as a design-prose false positive and already contains **these exact seven fingerprints**; no entire rule or directory is suppressed. This supports classifying each of the seven as the same benign historical prose false positive. The original review says no external credential use was found; P7B did not obtain independent service-side usage records, so that external-use assertion is not independently proved.

A second full-history scan from the P7B checkout, using the Wave 11/Wave 12 exact-fingerprint ignore file, returned **0 findings** over 1,510 scanned commits. This is local validation on the P7B checkout, **not** a CI pass on a #182 remediation HEAD. It demonstrates the narrow historical exceptions work while leaving new fingerprints detectable. No remediation was applied to #182. The smallest proposed change is a separate Draft PR based on #182 carrying only the seven verified fingerprint lines and their provenance comments, followed by a full-history scan and CI on that new HEAD. The protected #182 branch remains untouched pending owner authorization.

## Decisions and statuses

- Track A COMPLETE: #83 READY_TO_CLOSE_CANDIDATE. Owner approval required to close; re-fetch immediately before action.
- Track B COMPLETE: #192 READY_TO_CLOSE_CANDIDATE. Owner approval required to close; keep checkpoint branch.
- Track C EVIDENCE RECOVERED LOCALLY: seven exact findings reproduced and matched to reviewed historical prose; no #182 remediation HEAD or CI result exists. Security release gate remains pending remediation and exact-HEAD CI.
- Owner decisions: (1) whether to close #83 unmerged; (2) whether to close #192 unmerged while retaining its branch; (3) whether to authorize a separate #182 security-remediation Draft PR and later integration. No approval is inferred from this report.

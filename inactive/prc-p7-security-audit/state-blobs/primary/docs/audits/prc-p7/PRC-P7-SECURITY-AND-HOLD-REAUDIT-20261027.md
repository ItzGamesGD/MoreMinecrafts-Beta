# PRC-P7-SECURITY-AND-HOLD-REAUDIT-20261027

Status: PARTIAL. Audit branch only. No closure, merge, security suppression, or protected-ref change is authorized by this record.

## SECURITY

PR #182 is OPEN at `73dcc8f4d139c1688352068b397dab040208253b`. The exact-head `full-history-secret-scan` failed in run `40480578071`, job `122109560906`; Fabric CI succeeded in run `40480578141`. GitHub's job-log redirect returned `Forbidden`, and the job annotations exposed only generic workflow messages. Thus the seven P6-reported findings are **SECURITY_EVIDENCE_BLOCKED**, individually unclassified. Neither their exact fingerprints nor credential reality was verified in P7. No new exception was added. The existing #182 `.gitleaksignore` contains four narrow historical fingerprints with explanatory text; their validity for the seven current findings remains unproved. Alternative access checked: Actions job-log API and check-run annotations. Next: obtain the scan artifact or job logs through authorized access; review each finding without publishing values; check credential use; propose only verified exact-fingerprint controls on a separate remediation Draft PR; run the full-history scan on its exact HEAD. Release security gate remains blocked, independently of the source audit.

## CHECKPOINT

Fetched current GitHub PR refs and branch refs. `checkpoint/wave12-unified-20261027` and PR #193 both point to `d341a0656450f74ae16d78c75f7a981002a54692`. PR #192 (`c47c69f3e3553819432905197599443288669bb5`) is its ancestor. PR #182 is not its ancestor; Wave 12 integrated selected work rather than the whole Wave 10 head. The latest integrated gameplay checkpoint is Wave 12. #194 is evidence tooling based on Wave 12; #195 is audit documents based on Wave 12; #196 is F-3 implementation and P4 verification based on #194. None is automatically an integrated gameplay checkpoint. No open PR newer than #196 appeared in the current open-PR query. Historical CI in Wave 12 reports 463 JUnit and 425 server GameTests; P7 did not rerun these on its audit-only branch.

Relevant current open heads: #83 `e7dc53f9`, #107 `dad054c7`, #117 `991b20c8`, #118 `c40ee6b1`, #119 `c7fe7fab`, #126 `a9b89755`, #127 `334997d2`, #128 `6ba15ac2`, #129 `21769359`, #130 `9c9f2257`, #141 `aa376713`, #181 `268ec65b`, #182 `73dcc8f4`, #189 `6f9c3ac2`, #192 `c47c69f3`, #193 `d341a065`, #194 `4e6901c0`, #195 `87fa0133`, #196 `bbd69182`. All were OPEN when queried. Full SHAs remain in fetched refs; abbreviations here are identifiers, not closure preflight checks.

## HOLD AUDIT

Evidence basis: current fetched heads; `docs/CONTENT_AUTHORITY_INDEX.md`; Phase 2 owner decisions; Wave 12 `INTEGRATION_GAPS_AND_RESOLUTIONS`, `FINAL_CHECKPOINT_ACCEPTANCE`, and Wave 13 cleanup manifest. Historical report claims are marked as such where P7 did not independently rerun runtime tests. Broad PR-to-checkpoint tree diffs are misleading because old PRs branch from earlier cumulative baselines; the findings below use contribution and authority scope. No row is a closure authorization.

| PR | P7 classification | Verified contribution and precise release condition |
|---|---|---|
| #83 | READY_TO_CLOSE_CANDIDATE, live acceptance pending | Five source commits were cherry-picked with `-x` into Wave 12. Its arena template, weight-1 pool entry, mixin, two GameTests, generator, validator, and screenshots have Wave 12 counterparts. Wave 12 records 425 GameTests and real-terrain/native-image checks. Confirm exact current blob and patch equivalence, preserve original image provenance, and obtain required visual/feel acceptance before closure approval. |
| #107 | ACTIVE_UNIQUE tooling | Separate `clientQa` source set and workflow are absent from checkpoint. Current real-client visual, SDL input, accessory client and restart lanes overlap, but a capability-by-capability comparison of the old harness remains needed before retiring it. No gameplay implementation is at stake. |
| #117 | ACTIVE_UNIQUE experimental evidence | Underground Kingdom is an experiment-only worldgen probe per structure authority; no approved feature implementation is inferred. Preserve probe inputs/results and decide whether its tooling has continuing value before closure. |
| #118 | BLOCKED_DEPENDENCY | XP Quest parent of open #119. Wave 12 has the 200-ID catalog, 75 registrations, RawXp claim path and current hooks according to its audit. Recheck parent-only evidence, then resolve #119 first. |
| #119 | SEMANTIC_REVIEW | Historical chunk-generator ore attribution and nine registrations differ from approved positive provenance and per-quest test gate (#120 successor). Prove current attribution and duplicate prevention against executable tests before treating its semantics as preserved. Do not register the nine quests through cleanup. |
| #126 | BLOCKED_DEPENDENCY | Parent of #129. Old four-card/selector work evolved through #135 and later Wave 4B and Wave 7 input fixes. Preserve still-valid navigation separately; do not restore obsolete UI. Resolve #129 first. |
| #127 | BLOCKED_DEPENDENCY | Parent of #128; contains decision/build history. Phase 2 F-1 decision is in authority. Retain until #128/#130 provenance and evidence chain is settled. |
| #128 | BLOCKED_DEPENDENCY | Parent of #130; adds skull provenance and premature `xpq.cmb.wither_skulls_3` registration. F-1 explicitly keeps quest unregistered pending real durability proof. |
| #129 | SEMANTIC_REVIEW | 200 stable IDs are distinct from 75 registered quests; checkpoint audit says both are present. Verify navigation and catalog blob/semantic differences, preserve design evidence, and leave registration count unchanged. |
| #130 | OWNER_DECISION_REQUIRED | P2 pickup accounting repair exists, but its own two-JVM forced-halt acceptance historically credited both players for one skull. #196 supplies a P4 protocol model and assumption checks, not real forced-halt proof. Need approved durable receipt design, real crash/restart test, skull conservation, duplicate-credit invariant, and registration gate before closure. |
| #141 | ACTIVE_UNIQUE tooling | Five files add a manual OpenRouter delegation script, workflow and mocked tests; excluded from production by earlier plan. Preserve as experimental evidence or retain as isolated tooling; do not enable paid external services. |
| #192 | READY_TO_CLOSE_CANDIDATE | Its exact HEAD is ancestor of Wave 12. Before closure approval, verify all unique Wave 11 audit/preservation records and blobs remain reachable in Wave 12; retain checkpoint ref. |

## ACTIVE DEVELOPMENT

| PR | HEAD | Relationship and guard |
|---|---|---|
| #193 | `d341a065` | Wave 12 integrated checkpoint, current gameplay baseline; retain active. |
| #194 | `4e6901c0` | Adds F-3 sampling and skull protocol-model tools on Wave 12; retain active. |
| #195 | `87fa0133` | Adds Wave 13 analysis, decisions and cleanup manifest on Wave 12; evidence-only, retain active. |
| #196 | `bbd69182` | Adds approved Adamantium F-3 data and P4 assumption verification on #194; retain active. P4 model is not real crash-safety acceptance and does not authorize quest registration. |

## CLOSURE READINESS

No PR is READY_TO_CLOSE. Candidate order after evidence and owner approval: #83 and #192 independently; #119 before #118; #129 before #126; #130 before #128 before #127 only after F-1 real implementation and acceptance. #107, #117 and #141 require explicit tooling/evidence disposition and source preservation. Re-fetch each source HEAD/state immediately before any approved closure; comment with successor, original commit/path/blob and verified preserved payload. P7 performed no closure preflight or focused tests because it changed no production or test code.

Dimensions: SECURITY BLOCKED (seven findings unclassified); HOLD AUDIT PARTIAL (source and authority comparison done, listed semantic and payload checks outstanding); CHECKPOINT CONDITIONAL_GO (Wave 12 identified, no new integration authorized); CLOSURE READINESS BLOCKED (no closures approved). Overall PARTIAL. Security does not block continued HOLD evidence work, but it blocks release security acceptance.

# Final checkpoint acceptance — Wave 12 (2026-10-27)

> **Historical record, baseline-dated (Wave 13 gate, 2026-10-28).** This file describes the Wave 12 checkpoint as validated at `3c8186b7`. The checkpoint has since moved to `6ead3fcd` (PR #204, T2 equipment Waves AB/C/D). The CI table below is for `3c8186b7` only and is **not** the result for `6ead3fcd` or for the Wave 13 branch. The "Adamantium does not match F-3" rows describe `3c8186b7`; F-3 candidate E is integrated on `integration/wave13-adamantium-f3-20261028` after the owner's 2026-10-28 amendment (see `docs/preservation/PHASE2_OWNER_DECISIONS_2026-10-26.md`). Wither-skull F-1 is still blocked.


## Identity

| Item | Value |
|---|---|
| Final checkpoint branch | `checkpoint/wave12-unified-20261027` (same commit as `integration/wave12-functional-preservation-20261027`) |
| Baseline | `checkpoint/wave11-unified-20261027` @ `c47c69f3e3553819432905197599443288669bb5` (unmodified ancestor) |
| Last code-bearing commit | `7ef97c4c` (GameTest-only addition); `0333a5d2` is the last production-code commit |
| Draft PR | [#193](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/193) → `main` (not for merge) |
| Audit scope | 139 remote branches, 191 pull requests; full unshallowed history; remote state re-verified unchanged before publishing |

Commits on top of Wave 11:

| Commit | Content |
|---|---|
| `6bb483e1`, `64ffec31`, `d8a8f386`, `ef3e7054`, `209c251e` | Ancient City Arena prototype (PR #83, cherry-picked with `-x`) |
| `0333a5d2` | Arena validation workflow push filter includes the Wave 12 refs |
| `0d035c84` | XP Control (`v3.research.67`) approved amendment restored; Sky Islands design record preserved |
| `35a0ea6b` | Audit deliverables and evidence |
| `7ef97c4c` | 16 lower-equipment regression GameTests restored from closed PR #65 |
| `3c8186b7` | Audit of PR-ref-only heads and the #65 recovery record (this is the validated head) |

## Automated validation on the code tree

> **Superseding record (2026-10-27, Wave 13 preparation).** The copy of this file on `checkpoint/wave12-unified-20261027` is frozen at `3c8186b7` and still says CI was pending. That checkpoint is deliberately not rewritten. The verified result is recorded here, on `docs/wave13-prep-audit-20261027`.

CI on PR #193, independently re-queried from GitHub on 2026-10-27 against the exact head `3c8186b7af3e9fc771feaf427214ed448366dd81` (the remote `checkpoint/wave12-unified-20261027` and `integration/wave12-functional-preservation-20261027` both point at it; `main` is unchanged at `cde842dd`).

| # | Workflow (PR event) | Run | Attempt | Conclusion |
|---:|---|---|---:|---|
| 1 | Fabric CI (`build`, `server-smoke`, `client-smoke`) | [40431027337](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027337) | **2** | success |
| 2 | Accessory Client GameTest Acceptance | [40431027392](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027392) | 1 | success |
| 3 | Minecraft Client Visual Validation | [40431027406](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027406) | 1 | success |
| 4 | K5 Production Restart (two-process and three-process persistence) | [40431027320](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027320) | 1 | success |
| 5 | K5 Fixture Isolation Regression | [40431027291](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027291) | 1 | success |
| 6 | K5 Mapped Sources Probe | [40431027380](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027380) | 1 | success |
| 7 | K5 Stage 23-A Batch 3 Crafting Probe | [40431027267](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027267) | 1 | success |
| 8 | Accessory cooldown restart persistence | [40431027292](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027292) | 1 | success |
| 9 | Pre-public full-history secret audit | [40431027391](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027391) | 1 | success (`full-history-secret-scan`); `actions-exposure-audit` and `actions-artifact-inventory` skipped by design, they run only on `workflow_dispatch` |
| 10 | Tooling readiness | [40431027309](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431027309) | 1 | success |

Push-triggered Ancient City Arena Validation (real terrain generation and native client capture): [40431021779](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431021779) on `checkpoint/wave12-unified-20261027` and [40431019466](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/40431019466) on the integration branch, both success on `3c8186b7`.

All 17 check runs on the head are `success` or the 2 documented skips; none is failed, cancelled or pending.

**Disclosure — Fabric CI needed one re-run.** On attempt 1 the `client-smoke` job was **cancelled** by its own `timeout-minutes: 8`, because the runner spent about 7 minutes in the `apt-get install` step before the client even started (job `121950319338`). The same cancellation occurred on the earlier head `35a0ea6b` (run `40430312736`, job `121947893698`), which was then superseded. Attempt 2 of run `40431027337` re-ran only the cancelled job, and `client-smoke` passed (job `121953700111`, 01:27:00–01:30:47Z). `build` and `server-smoke` passed on attempt 1 and were not re-run. The cancellation is an infrastructure timeout, not a test failure: the log shows no test output, and no product code or workflow step in the diff touches that path. It is also a latent fragility: any slow apt mirror can cancel `client-smoke` again, and the 8-minute budget is shared with that install step. Changing it is outside this record.

The earlier-head runs on `35a0ea6b` are historical and not part of this acceptance.

Local runs are listed in [CROSS_SYSTEM_VALIDATION.md](CROSS_SYSTEM_VALIDATION.md): JUnit 463/463 and server GameTests 441/441 on `7ef97c4c` (425/425 on the earlier `209c251e`), real-client GameTests PASS, arena real worldgen + native capture PASS, static wiring checks PASS, independent XP Quest catalog recount PASS.

## Acceptance check of the final tree against task acceptance and current authority

| Criterion | Result |
|---|---|
| Every in-scope approved contribution found on any branch is present or explicitly blocked/excluded with evidence | PASS. Two gaps were recovered (arena; XP Control authority text) and one design record was preserved. One approved feature is blocked by owner decision F-1 (skull provenance). Exclusions are cited in INTEGRATION_GAPS_AND_RESOLUTIONS.md |
| No newer implementation replaced by an older one | PASS: `git diff c47c69f3..HEAD` over `src/` removes no line except two JSON list terminators that gained a trailing comma. Four existing files received appended lines (arena initializer calls, client harness registration, mixin and GameTest list entries); everything else is new files |
| No gameplay value, progression rule or owner decision silently changed | PASS: the arena values are PR #83's (weight 1, rigid, vanilla degradation processor); the authority restoration is verbatim |
| Rejected/shelved designs not reactivated | PASS: territory, commission board, Feather Boots redesign, sweep override and experiments are not integrated |
| `main`, trusted checkpoints and other branches untouched; no deletion or closure | PASS: verified by re-fetch (139 audited refs unchanged; only the two Wave 12 refs are new) |
| Current authority matches implementation | **NOT FULLY**: Adamantium worldgen still uses the pre-F-3 values (U-2) |
| Required live validation complete | **NO**: LIVE VALIDATION PENDING (arena look and feel, combat/ability feel, physical UI input, two real clients, Windows client) |

## Outcome

**PARTIAL INTEGRATION.**

- Every independently resolvable, approved contribution in the repository is integrated, and automated validation passes.
- Approved functionality still missing or blocked:
  1. Wither-skull provenance quest: BLOCKED. F-1 rules #128/#130 not release-ready, and #130's P1 forced-halt acceptance is still red; a receipt/durability protocol must be decided by the owner.
  2. Adamantium generation does not yet match F-3. Implementing it needs an authorized task, because the parameter choice changes abundance materially.
- Live validation is pending in any case, so FULL INTEGRATION VERIFIED cannot be claimed even after those two are resolved.

The checkpoint is a trustworthy integration baseline. Every exclusion is traceable, no regression was found by automated checks, and the remaining gaps are explicit owner decisions rather than lost work.

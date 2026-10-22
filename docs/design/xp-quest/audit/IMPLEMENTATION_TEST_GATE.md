# IMPLEMENTATION TEST GATE

Q1 implementation is **forbidden until the user explicitly approves it.** The gates below must be met before then.

## A. Design gates (must be resolved before Q1 starts)

| Gate | Related | Status |
|---|---|---|
| G1: Replace the 17 Knowledge capped-counter quests with dedicated counters and correct their retroactivity | D-01 | FAIL |
| G2: Move the 5 stat-backed quests to direct vanilla-statistic lookups | D-02 | FAIL |
| G3: Design a placed-block exclusion, or redesign the mining and placement quests | D-03, D-04 | FAIL |
| G4: Decide how retroactive payout works for existing worlds | D-32 | FAIL |
| G5: Fix the definitions for tame_5, biomes_all, recipes_200, cave_dwellers and ingredients_all | D-06, D-07, D-11, D-12, D-16 | FAIL |
| G6: Specify monument attribution (last killer only vs all participants) | D-08 | FAIL |
| G7: Specify the loot hook and uniqueness keys | D-09, D-13 | FAIL |
| G8: Specify the UI toast and pending lifecycle | D-20, D-21 | FAIL |
| G9: Decide the selector card art | D-30 | FAIL |
| G10: Mark the superseded authority lines inline | D-29 | FAIL |

## B. Required tests (all NOT RUN — no implementation exists)

| ID | Type | Content |
|---|---|---|
| T-01 | Unit | `QuestRules`: capped at target; no change after claim; non-positive input ignored |
| T-02 | Unit | Catalog invariants: 200 unique IDs, multiples of 50, totals 180,000 and 209,100, VERIFY/FUTURE unregistered |
| T-03 | Unit | Exact XP conversion: total ↔ (level, points) round-trip over the full range 0–1,000,000 |
| T-04 | GameTest | Exact deduction and grant at balance 0, exactly equal to the cost, 1 XP short, the 15/16 and 30/31 boundaries, and ±100,000. Assert `RawXp.available` and `totalExperience` both match exactly. |
| T-05 | GameTest | Two claim packets in the same tick pay once; replayed packets and random IDs are refused with no state change |
| T-06 | GameTest | Exception injected during the grant → claimed flag and XP fully rolled back (or the design defines an atomic order) |
| T-07 | GameTest/Integration | Claim then forced shutdown → after restart, verify no duplicate and no loss (D-23) |
| T-08 | GameTest | Death, respawn, relog and world reload keep all three states |
| T-09 | GameTest | **Enhancement transaction rollback** (authority requirement): forced failure after XP deduction, after material removal, and after item modification → XP (level, progress, `totalExperience`), inventory and equipment components fully restored |
| T-10 | GameTest | Enhancement rejection when XP or materials are short, with no state change; same cost regardless of level |
| T-11 | GameTest | Quest XP is not affected by the Research XP multiplier or Mending |
| T-12 | GameTest | Per-quest triggers: completes once; no change on re-trigger after completion; no credit for placed-block re-mining (after D-03 is fixed) |
| T-13 | GameTest | Retroactive login evaluation (per the D-32 decision) |
| T-14 | GameTest | Attribution: kill credit; loot opener; monument completion after chunk unload (D-08) |
| T-15 | Integration (client) | No toast from snapshots, exactly one toast from deltas; pending cleared on timeout and on reopen |
| T-16 | Live | 12-tab layout, three-state badges, telling click from drag, Korean tooltips, scroll kept across rebuilds, no vanilla advancement/telemetry contamination |

## C. Tests run in this audit

| Test | Result |
|---|---|
| Ledger and table-format script (independent of A) | **PASS** (ledger) / **FAIL** (B document table, D-25) |
| Research and Enhancement totals recomputed | PASS |
| 26.3 API / bytecode checks (javap) | Run. Results in DEFECT_REGISTER |
| `./gradlew test` (existing unit tests) | **NOT RUN** — compile could not start locally: `release version 25 not supported` (this environment has no JDK 25) |
| GameTests | **NOT RUN** (the same JDK limitation, and there is no implementation) |
| Live client | **NOT RUN / LIVE PENDING** |

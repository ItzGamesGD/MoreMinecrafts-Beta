# Wave 13b — provisional owner decisions: reconciliation and verification (2026-10-27)

Status: **audit record plus one data change (F-3).** It creates no design authority. The four decisions below are **provisional** (conveyed in the task, not yet written into
`docs/preservation/PHASE2_*`); nothing here promotes them. Wave 12 (`checkpoint/wave12-unified-20261027`), PRs #193, #194 and #195 are untouched; this work is on
`feat/wave13-adamantium-f3-impl-20261027`, stacked on #194's branch because it reuses its sampling tooling.

## 1. Reconciliation with the existing approvals

| Provisional decision | Existing approval it touches | Result |
|---|---|---|
| **D-F3-1** Y 8–22 bounds **every generated block** | F-3: "Height Y 8–22, biased toward Y 15" (PHASE2_OWNER_DECISIONS, RECONCILIATION §F-3) | **Consistent.** F-3 does not say origin vs block; this is the stricter reading and satisfies both. Candidate E (origin 9–21) |
| **D-F3-2** attempts per chunk "follow vanilla Ancient Debris" | None defines an attempt structure. The 2026-10-18 design says only "slightly rarer … ~80%" | **Resolved from vanilla data, not chosen.** Both vanilla debris features (`ore_ancient_debris_large`, `ore_debris_small`; 26.3 `minecraft-common.jar`) have **no count modifier = exactly one attempt per chunk each**, so the attempt count is identical for large and small: **1**. Large and small differ in *size* (3 vs 2) and *height* (trapezoid 8–24 vs uniform 8…top−8), and F-3 fixes both of those (size 1–2, Y 8–22 biased to 15), so no remaining choice depends on large-vs-small. **Open item:** vanilla has **two** debris features per chunk; Adamantium keeps **one** feature, because F-3 describes one vein definition. Mirroring both features would be a second Adamantium feature and is not in any record (see §5, O-1) |
| **D-F3-3** abundance change allowed; must stay below Ancient Debris | F-3 binding rule; "~80%" is a tuning target only | **Consistent.** Measured 50.0% (§2) |
| **D-F1-1** proof must prevent double counting **and** preserve the physical skull | F-1: first pickup only, #128/#130 not release-ready, crash-consistency defect to be corrected and tested | **No conflict, but new and under-specified.** F-1 never required physical conservation. "Preserve" needs a scope; vanilla cannot preserve an item forever (void, lava, chest loss). §3 proves one scope and states the limit |

No historical-versus-current conflict was found that needs escalation.

## 2. F-3 — implemented and measured (candidate E)

Change (two data files, one build-validation line, tests): `feature/ore_adamantium.json` → `minecraft:scattered_ore`, size 2, discard 1.0; `placed_feature/ore_adamantium.json` → count 1, `in_square`,
**trapezoid 9–21 (no plateau)**, biome filter. `build.gradle`'s `validateStage6Data` asserted the pre-F-3 `minecraft:ore` type and failed on the change; it now expects `scattered_ore` for Adamantium only.
Nether-only registration is unchanged. Nothing else (items, recipes, equipment) was touched.

Why 9–21: `scattered_ore` moves a block one block vertically from the vein origin (measured earlier: an 8–22 origin range produced blocks at Y 7 and 23). An origin range of 9–21 therefore confines every block to 8–22 (D-F3-1), and still peaks at Y 15.

**Measurement** (shipped data, no datapack override): real vanilla Nether generator, **8 seeds × 1,296 interior chunks = 10,368 chunks**, Ancient Debris and Adamantium counted in the same chunks, every block Y 0–255 scanned, biome mix and method as in
[ADAMANTIUM_F3_ANALYSIS.md](../wave13/ADAMANTIUM_F3_ANALYSIS.md) §3 (on PR #195). Raw data: [`adamantium/results/`](adamantium/results/), table: [`adamantium/shipped_summary.md`](adamantium/shipped_summary.md).

| Check | Result |
|---|---|
| Fewer blocks than Ancient Debris (binding), **every** seed | **PASS.** 7,954 vs 15,919 = **50.0 %**, per seed 47.7–52.2 % |
| Any Adamantium block outside Y 8–22 (D-F3-1) | **0 of 7,954.** Observed Y range 8–22 on every seed |
| Peak | Y 15; 60 % of blocks in Y 13–17 |
| Vein size | 4,457 single-block, 1,744 two-block, 3 clusters of 3 (two touching veins; one placement cannot exceed 2) |
| Seeds 1–3 vs the earlier candidate-E run | identical counts (996/1907, 1014/2081, 998/2080), so the shipped files are candidate E |

Disclosure: dimension `minecraft:the_nether`; biomes nether_wastes, soul_sand_valley, basalt_deltas, crimson_forest, warped_forest (chunk centre); height method = per-block Y scan. About **17 % of individual chunks hold more Adamantium than Ancient Debris**
(206–241 of 1,296 per seed); the F-3 rule is stated for matched *samples*, and the sample total is what passes. This is disclosed, not a failure of the stated criterion. Eight seeds is a bounded sample, not a proof of the long-run rate; no live play was done.

Tests: `AdamantiumF3WorldgenTest` (9 tests) now asserts shipped data equals candidate E, the origin range confines blocks to 8–22, one attempt per chunk, and the real `TrapezoidHeight` over the shipped range (support 9–21, mode 15).
Mutation check: origin floor 8, origin ceiling 22, size 3, count 2 and uniform height each fail a test. Full `./gradlew build` (unit tests, data validation, 443 game tests) passes locally.

## 3. F-1 — can duplicate-counting prevention and physical skull preservation both hold?

Prior finding: P3 stops double credit but, in 8 of 24 crash states, leaves a ground duplicate and in 2 loses the skull. The model now has **P4**:

1. ledger `PENDING(token, owner)` **plus the item and its location**, flushed synchronously, before any other write;
2. remove the ground entity; owner's file receives skull + credit + receipt in one file; player save is the commit point; ledger `COMMITTED`; clear receipt;
3. recovery from durable state only: `PENDING` + owner's receipt → roll forward and **discard** any stale ground copy; `PENDING` without receipt → roll back and **restore** the skull entity under its original UUID, flushing the chunk before the ledger entry is cleared; `COMMITTED` + stale ground copy → discard.

Exhaustive result ([`skull/protocol-model-results.json`](skull/protocol-model-results.json)): every crash point × every independent persistence outcome of chunk / player / ledger, **plus a second crash at every step of recovery itself** — **138 states, 0 violations** of INV1 (double credit), INV2/INV3 (credit ⇔ held skull), INV4 (no loss), INV5 (no duplicate); physical outcome **138 exact, 0 lost, 0 duplicated** (P3: 14 exact, 2 lost, 8 duplicated).
Mutation check ([`skull/p4-mutants.txt`](skull/p4-mutants.txt)): removing the PENDING flush, the owner-file flush, the restore, the stale-copy discard, or clearing the ledger before the restore is durable each produces violations — five of five caught.

Vanilla assumptions, checked on real 26.3: a second entity with an existing UUID is **refused** (`SkullProtocolAssumptionGameTests.secondEntityWithTheSameUuidIsRefused`, so a repeated restore is idempotent) and per-chunk entity-load status is queryable (`areEntitiesLoaded`; same class). Existing proof: the ledger/receipt/player-save pattern survives the 13-scenario two-JVM matrix in `ContractCrashGameTests`. `ServerChunkCache.save(boolean)` exists for the flush in the rollback path.

**Verdict.** Feasible **within a defined scope**: from the start of a pickup until its transaction is committed and the receipt cleared, the skull is neither lost nor duplicated and is credited exactly once, in the model, under the stated persistence semantics.
**Not achievable, by any protocol on vanilla storage:** conservation *after* commit (the skull is then an ordinary item: dropped into lava or the void, destroyed, lost with a chest or a deleted player file), and any guarantee against loss of the ledger file or of an already-flushed domain. Power-loss durability is not claimed (process-halt only).
**Not proven:** the Minecraft hooks themselves. There is no skull feature on this checkpoint, so the real two-JVM halt matrix (before/after prepare, after chunk removal, after player save, after ledger commit, chunk-saved-before-player, plus a crash inside recovery) can only be run once the protocol is implemented. `xpq.cmb.wither_skulls_3` stays unregistered.

## 4. Not done, on purpose
Nothing implements or registers the skull quest. P4 adds behaviours no record describes (a restored skull reappearing at its pickup location after a crash; a stale duplicate being deleted), so implementation waits for approval of §5 O-2.

## 5. Remaining decisions (owner)

| ID | Decision | Why it is not mine |
|---|---|---|
| O-1 | F-3: one Adamantium feature (shipped) vs mirroring both vanilla debris features? | Doubles attempts and adds a second vein definition; no record covers it. Without an answer E stands (it satisfies every recorded rule) |
| O-2 | F-1: approve P4's scope — "no double credit and no loss/duplication during the pickup transaction; after commit the skull is an ordinary item" — and its two recovery behaviours (restore at the original location after a rolled-back pickup; delete a stale duplicate of a committed skull) | New observable behaviour; "preserve" has no scope in the records. If the owner needs conservation after commit, it is not possible and the quest should stay unregistered |
| O-3 | Write the four decisions into the PHASE2 authority records if they become final | Per AGENTS.md they are provisional until approved |

Whether implementation can proceed: **F-3 is implemented and validated** (pending your approval of O-1 as a non-issue, and CI). **F-1 implementation is blocked only on O-2.**

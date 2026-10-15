# Stage 23-A Parallel Branch Reconciliation Manifest

Date: 2026-10-15

Branch: `codex/stage23a-reconcile-20261015`

Base and target: PR #91 head `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`, branch `codex/stage23a-batch2-20261015`
Parallel source: PR #92 head `f632f4f521c782c9614a3bbb71f1ab5579a3358d`, code-tested head `f0f9c2d07f4e74fae63cb8c5b704c558917fc70b`, based on PR #89 head `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`.

## Ancestry and reconciliation rules

PR #92 is an authorized sibling implementation from PR #89, not a descendant of PR #90 or #91. This branch keeps PR #91 as its cumulative base. PR #92 remains open, Draft, and unchanged. Remote PR heads and changed-file lists for #89–#92 were fetched before worktree creation; the original dirty checkout was preserved.

Only one runtime path remains for each duplicate ID. Where PR #90’s behavior is already correct, retain it. Carry a correction only when it has an observable safety or correctness benefit. The same paid research level and one server event must not activate two handlers.

## Effect decisions

### Fisheries Study approval — 2026-10-15

Before the reconciliation implementation was delivered, the user explicitly approved the `.19` residual-allocation policy. This decision is now recorded in the current design authority and supersedes the earlier unresolved question and inactive-candidate status. For each crankbait tier and paid `.19` level 1–5, linearly interpolate each grade probability between the approved baseline and level-5 tables, floor the values, and assign remaining percentage points by largest fractional remainder; ties go to the lower grade index. Every distribution totals exactly 100%. Level 0/unpurchased behavior uses the baseline. Preserve species, length, weight, fish components/payload, and `.23` timing behavior.

The first documentation checkpoint recorded approval before gameplay code. The implementation now uses the approved behavior through PR #93's existing fishing path. PR #92 supplies reference code, but this branch ports only the approved Fisheries behavior, not its duplicate `.02`, `.03`, or `.18` handlers.

| Crankbait tier | Approved baseline Grade I–V | Approved level-5 Grade I–V |
|---|---|---|
| 0 | 45/30/16/7/2% | 40/32/17/8/3% |
| 1 | 30/32/22/12/4% | 25/33/23/13/6% |
| 2 | 10/25/32/24/9% | 7/24/33/24/12% |
| 3 | 0/8/34/38/20% | 0/6/33/37/24% |

PR #92's reference code compares floating-point remainders. Its candidate table differs from the newly approved lower-index tie rule in three mathematically tied cases. PR #93 uses exact fifths (integer numerator/remainder) and produces the approved lower-index outcomes: tier 0 level 2 = `43/31/17/7/2`, tier 0 level 3 = `42/31/17/8/2`, and tier 1 level 2 = `28/33/22/12/5` (Grade I–V). These cases are explicit regression tests.

Actual hook: the successful server-side `FishingChallenges.awardIfPermitted` path obtains only that `ServerPlayer`'s purchased `.19` level through `ResearchRewards` and passes it with the rod's existing crankbait tier into `FishingLogic`. Exact integer fifths make remainder ties deterministic. V3 catalog, purchase costs/prerequisites, and progression serialization stay unchanged. The paid level is already player-scoped and saved; `.19` adds no state. A valid catch still produces only the existing fish result(s), with existing equipment wear and delivery behavior. No global/shared odds, extra fish, bonus wear, XP, or alternate trap reward is added.

| ID | Comparison / authority | Reconciliation |
|---|---|---|
| `v3.research.02` Lumbering | Both implementations use the approved L1–L5 10/20/20/30/30% chance and 1/1/2/2/3 extra-log cap, with a one-pass log-only extra drop. PR #90 processes already-generated loot, honoring custom log loot and the player’s drop event; PR #92 uses the broken block’s default item and explicitly Auto-Smelt-processes the extra. | Retain PR #90’s single handler and ownership gate. Move the one-pass research drop augmentation before the existing Auto Smelt/Replant processing so bonus drops receive the same enchantment processing once. No tree scan, extra block break, or recursive drop call. |
| `v3.research.03` Mineral Conservation | Both use an independent 5% per paid level (max 25%) prevention roll for mining durability; PR #90’s `ResearchMiningContext` captures the exact tool and requires it to be effective against the broken state. PR #92’s player-only context and pickaxe-tag gate is broader and can include a different/non-effective pickaxe. | Retain PR #90’s stricter context and its one-point prevention behavior. Keep shield durability handling separate. |
| `v3.research.18` Beekeeping | Both use +1 honeycomb at 20/40/60% only for a full-hive shears harvest. PR #90 checks that vanilla consumed the action and lowered honey to zero. PR #92 additionally checks that the same block still occupies the position, avoiding an unsafe property read if the block changed. | Keep PR #90’s explicit success and server/player checks; add PR #92’s same-block postcondition; use the normal block resource-drop helper. Retain PR #90’s level-3 full-hive GameTest and add failed/unowned coverage. |
| `v3.research.55` Survival & Expedition | Current authority approves sprint/swim exhaustion reductions of 8/16/24%. PR #92 scales only the mapped swim and sprint exhaustion call sites in `ServerPlayer.checkMovementStatistics`. | Integrate the two scoped call-site modifiers, paid-level gated, and test that unrelated movement-research ownership does not scale exhaustion. Other exhaustion causes remain unchanged. |
| `v3.research.67` XP Control | Current authority and the parallel approval authorize immediate server-side pickup within a 4-block L1 / 6-block L2 Euclidean radius, with no duplicate claim. PR #92 calls vanilla `ExperienceOrb.playerTouch` once for the nearest eligible player. | Add the scan to the existing Batch 2 `ResearchXpAcquisitionMixin` tick hook. The same vanilla pickup call passes through the existing `.64` recipient XP redirect once, so `.67` changes pickup distance only; `.64` applies its fractional bonus once to the actual collector. No orb value estimation or new saved state. |
| `v3.research.19` Fisheries Study | Current authority now explicitly approves linear interpolation of the existing baseline and L5 tables, largest-remainder allocation, lower-grade-index tie-breaking, and exact 100% totals for levels 0–5 and crankbait tiers 0–3. | `ResearchRewards` exposes the five paid levels; `FishingChallenges.awardIfPermitted` resolves the collecting player's level and passes it to `FishingLogic` for the existing fish grade roll. Fish species, length, weight, item components, `.23` timing, and current wear/delivery behavior are preserved. No new mixin or persistent state is added. `.19` remains PARTIAL pending complete CI and live fishing validation. |

## Tests and persistence

- Keep the existing PR #90 event-level mining durability and full-hive harvest GameTests and existing Batch 2 `.64` recipient-isolation GameTest.
- Add `.18` failed/unpurchased attribution GameTests.
- Add `.55` scaling tests for levels 0–3, unpurchased players, unrelated wall-jump/fall-roll ownership, and independent players. The mapped-source probe verifies only the swim/sprint call sites are modified.
- Add `.67` GameTests for `.64` only, `.67` only at the inclusive 4- and 6-block boundaries, just outside the level-two radius, both effects, neither, nearest eligible among multiple players, exact vanilla XP conservation, and one-time orb removal.
- Serialize/reload `.64` fractional state and `.67` paid level through the existing player progression codec; `.67` adds no state.
- Fisheries tests cover all six levels and all four crankbait tiers, exact baseline/L5 endpoints, largest-remainder allocation, lower-grade-index ties, exact 100% totals, unpurchased gating, unchanged species/length/weight and item components, per-player isolation in GameTest, and coexistence with `.23` timing rules.

## Shared files

Shared with PR #90/#91: `ResearchRewards.java`, `ResearchMiningContext.java`, `BlockMixin.java`, `ItemStackMixin.java`, `ServerPlayerGameModeMixin.java`, `ResearchXpAcquisitionMixin.java`, `ResearchContentGameTests.java`, `ResearchXpAcquisitionRulesTest.java`, `moreminecrafts.mixins.json`, the mapped-source workflow, the research authority, and the 87-node audit. New Fisheries integration files are `FishingChallenges.java`, `FishingLogic.java`, `FisheriesStudyGameTests.java`, `FishingLogicTest.java`, `FishingTimingTest.java`, and `ResearchParallelBranchRulesTest.java`. Changes are made by adding or narrowly adjusting behavior; no full-file replacement from PR #92 is used.

At the reconciliation checkpoint before Fisheries Study implementation, `.55` and `.67` had moved from DATA_ONLY to PARTIAL after adding runtime and focused automated tests. The duplicate `.02`, `.03`, `.18` remain PARTIAL; `.19` remains DATA_ONLY until its implementation and tests are present. The checkpoint total is 61 DATA_ONLY / 24 PARTIAL / 2 BLOCKED. Recount the individual rows after implementation. No row becomes COMPLETE from CI alone.

No Stage 23-B handlers, IDs, prices, prerequisites, or catalog topology change. No other persistent state is added. Manual movement, fishing, and multiplayer gameplay checks remain pending.

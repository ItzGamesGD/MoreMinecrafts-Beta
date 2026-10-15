# Stage 23-A Parallel Branch Integration Manifest

**Integration branch:** `codex/stage23a-integrated-research-20261015`  
**Verified base:** PR #91 head `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`, branch `codex/stage23a-batch2-20261015`  
**Integration PR target:** PR #91 implementation branch `codex/stage23a-batch2-20261015`  
**Selective source:** PR #92 head `f632f4f521c782c9614a3bbb71f1ab5579a3358d`, branch `codex/stage23a-research-implementation-20261015`  
**PR state at manifest creation:** PRs #88–#92 are open and unmerged. PR #91 descends from #90; #92 is a sibling line from #89. The local integration branch is exactly at the verified PR #91 head. No integration gameplay code has been changed at this manifest revision.

## Scope and ID ownership

Import only the unique approved behavior for:

| Research ID | Imported behavior | Source implementation ownership | Integrated code ownership |
|---|---|---|---|
| `v3.research.19` — Fisheries Study | Preserve approved baseline and level-5 grade distributions; interpolate purchased levels 1–4; largest-remainder integer allocation with ties to the lower grade index; exact sum 100%. Preserve fish identity/size/weight and fishing payloads. | PR #92, `Research23AEffectRules`, `FishingLogic` | Existing PR #90 fishing-grade and `.23` fishing-timing paths remain; add only grade weights and paid-level lookup needed for `.19`. Keep `.23` timing, payload, and client HUD behavior intact. |
| `v3.research.55` — Survival & Expedition | Sprinting and swimming exhaustion reduction: 8%, 16%, 24% at levels 1–3; other exhaustion causes unchanged. | PR #92, `ResearchMovementExhaustionMixin` | Add the narrowly scoped sprint/swim runtime adapter and mixin registration. Do not change PR #91 movement research handlers. |
| `v3.research.67` — XP Control | Server-side instant collection at 4 blocks for level 1 and 6 blocks for level 2; nearest eligible player, once through vanilla `ExperienceOrb.playerTouch`, exact orb XP conserved, no persistent state. | PR #92, `ResearchExperienceOrbMixin` and `Research23AEffectRules` | Extend the PR #91 `ResearchExperienceOrbMixin` while preserving Stage 22 attribution and the separate `.64` pickup award hook. Add the selected research reward mapping and focused combined `.64`/`.67` coverage. |

## Preserved IDs and duplicate exclusions

- Preserve all PR #90 behavior and tests for `v3.research.01`, `.02`, `.03`, `.09`, `.10`, `.15`, `.18`, and `.23`.
- Preserve PR #91 behavior and tests for `v3.research.17`, `.28`, and `.64`, plus the `wall_jump` and `fall_roll` verification additions.
- Do not import PR #92 duplicate handlers or duplicate runtime tests for `v3.research.02`, `.03`, or `.18`. PR #90 remains canonical for extra-log drops, one-point mining-durability prevention, and successful-harvest extra honeycomb.
- Do not alter shared catalog, price, prerequisite, progression schema, purchase gating, or save migration.
- Exclude all Stage 23-B reserved IDs: `shield_counter`, `ballistic_control`, `bow_charge_mobility`, `falling_axe_smash`, `weapon_swap_handling`, `mob_aggro_redirection`, and `hopper_filter`.

## Authority

- Current design routes through `docs/CONTENT_AUTHORITY_INDEX.md` to `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md` and the Stage 22 authority addendum.
- The user-approved `v3.research.19` residual rule is recorded in the current research authority before gameplay changes: largest remainder, lower grade index on ties, exact 100% totals. The existing baseline/L5 distributions, species/size/weight behavior, payloads, server-authoritative selection, and independent `.23` timing effect remain unchanged.
- `.55` and `.67` values and boundaries follow the existing current authority and the user-approved integration scope above.

## File ownership and planned changes

The PR #91 tree is the starting point for every shared file. Only the additions needed for `.19`, `.55`, and `.67` may be made:

| File | Planned ownership/change |
|---|---|
| `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java` | Add canonical purchase-gated mappings for `.19`, `.55`, `.67`; preserve PR #90 keys `.02`, `.03`, `.18`, `.23` and PR #91 keys `.17`, `.28`, `.64`. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java` | Add only pure grade-interpolation, exhaustion-multiplier, and XP-radius rules for imported IDs. Do not import duplicate `.02`, `.03`, `.18` implementations. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogic.java` | Route current grade selection through the approved `.19` table while preserving species, length, weight, deterministic server selection, and `.23` success timing. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMovementExhaustionMixin.java` | Add a narrow adapter for only sprint and swim exhaustion call sites. |
| `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchExperienceOrbMixin.java` | Add `.67` collection to the existing server orb lifecycle, preserving root XP attribution and vanilla pickup semantics. |
| `src/main/resources/moreminecrafts.mixins.json` | Register the `.55` adapter once; retain all PR #90/#91 registrations, especially `.64` XP acquisition. |
| `src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java` | Add unique `.19`, `.55`, and `.67` rule/purchase-gate tests only; do not copy `.02`, `.03`, `.18` tests from PR #92. |
| `src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AXpCollectionGameTests.java` | Add focused server tests for `.67`, including one-time vanilla pickup, nearest recipient, competing players, unpurchased players, and `.64` bonus composition. |
| `src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentGameTests.java` | Preserve all PR #90/#91 test methods. Add only nonduplicative fisheries/exhaustion integration checks if feasible in the existing class. |
| `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md` | Recount the canonical 87-row roster from the final integrated tree; upgrade only rows with concrete automated evidence; distinguish automated status from pending live checks. |
| `docs/CONTENT_AUTHORITY_INDEX.md`, `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md` | Record this manifest/current authority amendment only; avoid unrelated authority edits. |

No PR #92 wholesale merge, cherry-pick, branch retarget, or replacement of PR #90/#91 source files is permitted. Do not import PR #92 `ResearchMiningContext`, duplicate `BlockMixin`/`ItemStackMixin` logic, `ResearchBeehiveHarvestMixin`, or duplicate `.02`/`.03`/`.18` test cases.

## Required tests and validation

- Preserve PR #90 `ResearchContentRulesTest`, `ResearchContentGameTests`, and `FishingTimingTest`; preserve PR #91 `ResearchAxeCombatRulesTest`, `ResearchLivestockRulesTest`, `ResearchXpAcquisitionRulesTest`, `ResearchMovementRulesTest`, and all added `ResearchContentGameTests` methods.
- Add deterministic grade interpolation expectations for every purchased level and crankbait tier, largest-remainder allocation/tie-breaking, 100% totals, `.19` with `.23`, and unchanged fish species/length/weight.
- Add `.55` tests for sprint and swim scaling at levels 1–3 and unchanged unrelated exhaustion sources.
- Add `.64` + `.67` combined XP tests: exactly one vanilla pickup path, conserved original XP, correct once-only bonus and per-player fractions across save/reload, nearest eligible recipient, no award to competing/unpurchased players, and vanilla Mending/pickup behavior where automatable.
- Required command: `./gradlew clean test build compileGametestJava runGameTest` using pinned Java 25. Also run server/client smoke, mapped-source checks, two-process restart validation, focused integrated tests, `git diff --check`, mixin JSON/registration checks, and a canonical 87-ID/status recount.

## Manual validation obligations

Automated tests and CI do not establish live gameplay acceptance. Keep merge readiness **NOT READY** pending gameplay checks for all three imported effects: Fisheries Study grades for each paid level and crankbait tier alongside `.23`; sprint/swim exhaustion and unchanged other exhaustion causes; exact `.67` radius, competing players, `.64` composition, Mending, ordinary pickup, and real restart persistence of existing `.64` fractional state. Do not claim live verification unless it occurs.

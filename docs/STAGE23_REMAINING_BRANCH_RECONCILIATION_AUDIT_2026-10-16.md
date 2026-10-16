# Stage 23 — Remaining Branches: Source-Code Review and Integration Readiness

**Reviewed:** 2026-10-16. **Status: REVIEW COMPLETE / INTEGRATION PREPARED / CODE NOT YET MERGED.**  
**Protected staging baseline:** PR #100, `integration/stage23-linear-chain-20261016` at `7231c7aad598e9cada152537dfece7cfad2756dc`.  
**Review branch / PR:** `integration/stage23-reconciliation-audit-20261016`, Draft PR #102.  
**No modification to existing feature PRs or main; no existing branch reset or merge.**

## Source heads and ownership

- PR #92: `f632f4f521c782c9614a3bbb71f1ab5579a3358d`, sibling implementation from #89.
- PR #93: `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955`, sibling of #94 from #91.
- PR #94: `5e48deacbeed95783902f2b71e04e2ae25005606`, chosen research integration from #91.
- PR #97: `df960d6511fbf9bbe0a649d8f68de6c12362d458`, hotfix from #94.
- PR #99: `eb80a148065c29394f7a46650c05ab72dda9140d`, Live-Fix A from #94.
- PR #100 is the verified straight-line #94 → #95 → #96 → #98 lineage, with #88 → #89 → #90 → #91 as its earlier ancestors.

The PR #93 and #94 histories diverge. PR #92 is an alternate source, not a safe merge parent. Do **not** merge or cherry-pick #92/#93 wholesale.

## Actual source review: #92/#93 versus PR #100

### A. Research .02 Lumbering vs .01 Ore Mining — CONDITIONAL PORT REQUIRED

**Observed files:** `src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java`, `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentRuntime.java`, `src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentGameTests.java`.

- PR #100 invokes `applyOreAndLogResearch` **after** Auto Smelt/Replant. This preserves the approved Ore Mining `.01` behavior of duplicating the final processed ore drop (explicitly asserted in `dropResearchIsPurchaseGatedAndUsesTheProcessedDrop`), but a Lumbering `.02` bonus log is generated too late to pass through Auto Smelt.
- PR #93 moves the **combined ore-and-log method** before Auto Smelt. This fixes the Lumbering extra-log processing issue, but would change `.01` semantics by duplicating the pre-smelt ore rather than the processed result. Therefore **do not copy PR #93's BlockMixin ordering verbatim**.
- Selective implementation requirement: apply only Lumbering bonus generation before the one enchantment processing pass, and apply Ore Mining bonus generation to the processed result afterward. Keep the original ownership/creative/event gates, preserve Fortune/Silk Touch and one-pass smelting, and do not roll twice.
- Must add event-level regression tests: .02 bonus receives smelting exactly once; .01 copies the final Auto-Smelt output, not a pre-smelt ore; ordinary/unowned logs and ores unchanged; no double drops or duplicated enchantment processing.

### B. Research .18 Beekeeping — SELECTIVE PORT REQUIRED

**Observed:** `ResearchBeehiveMixin.java`. PR #100 reads `serverLevel.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL)` directly after vanilla interaction. If the block has been replaced, the target blockstate might not have the property and this access is unsafe.

PR #93 obtains `BlockState after`, checks `after.is(before.getBlock())` **before** `after.getValue(HONEY_LEVEL)`, and uses `Block.popResource` for the bonus. Port these guarded postconditions while retaining the existing successful-full-hive shears, server-side/player purchase checks and *single* handler. Add tests for replaced block, failed/no-consume action, and unowned player. Do not import #92's separate `ResearchBeehiveHarvestMixin`.

### C. Research .19 Fisheries Study — TAKE #97, NOT DUPLICATE #93

PR #93 computes exact fifths within `FishingLogic`; PR #100 delegates to `Research23AEffectRules` using floating-point remainders. PR #97 fixes the **same approved tie-breaking bug** in the PR #100 architecture using long-integer fifths. The three incorrectly rounded table rows on #100 are L2/tier0, L2/tier1 and L3/tier0. PR #97 corrects them; all 24 level/tier rows must sum to 100.

The two changed #97 files — `Research23AEffectRules.java` and `Research23AEffectRulesTest.java` — have **identical Git blob SHAs at #94 and #100**. A direct cherry-pick of #97's single fix commit is therefore a good candidate, without overwriting later #95/#96/#98 effects. Do not import PR #93's alternate complete `FishingLogic.java` or drop the PR #100 overloads and other current fishing features.

PR #93 has a **unique, absent** `FisheriesStudyGameTests.java` checking server-side player scoping, species/length/weight/components, and coexistence with research .23. Consider a narrowly adapted port of that test and its required helper methods from `FishingChallenges` (or test the same contract without changing production architecture). **Append** a GameTest registration in `src/gametest/resources/fabric.mod.json` rather than overwriting the current registration, which includes furnace and expanded research tests. Keep the grade/payload/rarity semantics independent of fish equipment wear.

### D. Research .55 / .67 and .03 — ALREADY OWNED, DO NOT DUPLICATE

- .55 sprint/swim exhaustion: #93 and PR #100 use the same two scoped `ServerPlayer.checkMovementStatistics` ordinal call sites; helper classes differ. Keep PR #100 `Research23AEffectRules` and the single `ResearchMovementExhaustionMixin`.
- .67 XP collection: #93 adds an orb `tick` handler in `ResearchXpAcquisitionMixin`; PR #100 has it in `ResearchExperienceOrbMixin`. Both invoke vanilla `playerTouch`. **Keep only the existing PR #100 orb-tick handler**; preserve .64 recipient-XP redirect separately. Target has focused XP GameTests for nearest player, one-time award, fractional XP, Mending and out-of-radius/unpurchased cases.
- .03 mining-durability context: #100 retains the stricter effective-tool and exact-block context from #90; do not import #92's broader player/pickaxe-only context.
- Extra #93 rules-only tests duplicate many target assertions; retain only genuinely missing regression cases. No extra mixin registration should be introduced for these IDs.

## PR #97 Fisheries hotfix — FILE-CLEAN PICKUP CANDIDATE

Exact file SHAs of its two targets in #100 match #94 (verified). One commit, two files; no file-level collision with PR #98 or #99. Apply PR #97 commit `df960d6511fbf9bbe0a649d8f68de6c12362d458` **once** after pinning the integration base. Verify the three tied outcomes, 24 total distributions, .23 timing, fish component data, and per-player research levels.

## PR #99 Live-Fix A — FILE-CLEAN PICKUP CANDIDATE

Five PR #99 **modified** files are unchanged on PR #100 relative to #94, based on Git blob SHA comparison:
1. `gradle/validate-flame-sword-production.gradle`
2. `src/client/java/dev/itzgamesgd/moreminecrafts/client/EnchantingProgressionScreen.java`
3. `src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java`
4. `src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordItem.java`
5. `src/main/resources/assets/moreminecrafts/models/item/flame_sword.json`

Five PR #99 **new** files are absent at PR #100 HEAD: `GreatswordSweepVfx.java`, `ViewportScale.java`, `FlameSwordModelTest.java`, `GreatswordSweepVfxTest.java`, `ViewportScaleTest.java` (under their corresponding src/main/src/test folders).

Therefore its seven commits / ten file changes appear file-level collision-free for application onto PR #100. Still require a combined build/tests and actual GUI/asset visual acceptance.

**Actual LF status:** LF-03 Flame Sword model UV correction, LF-04 Greatsword particle, LF-06 scaled Enchanting GUI are implemented with automated coverage but real-client visual checks pending. LF-01 adds opt-in selector dispatch logging but **does not establish the reported defect is fixed**. LF-02 backpack was inspected without a confirmed defect or code fix. LF-05 remains deferred. Do not report all six items as completed.

## CI evidence and limits

GitHub reported successful Java 25 Fabric CI, mapped-source and production-restart workflows for the exact PR #100/PR #98 HEAD `7231c7aad5`, PR #97 HEAD `df960d6511`, and PR #99 HEAD `eb80a14806` (individual source HEAD checks). **No combined result has been built or tested**. No live visual PASS is implied by those source CI results.

## Integration execution plan — authorization/implementation pending

1. Lock the Git HEADs and check that PR #100/PR #97/PR #99 are unchanged. Create a **new** code integration branch from PR #100 HEAD, distinct from this docs-only review PR #102.
2. Apply #97's one commit, preserving target source architecture; run focused Fisheries exact-fifths tests.
3. Selectively implement #93's .02-only pre-smelt generation while retaining .01 post-smelt, and .18 blockstate guard. Add new event-level regression tests; selectively bring .19 player-scoped/payload GameTest coverage while retaining all existing entries and helpers.
4. Apply the seven #99 commits (or equivalent identical targeted changes) without full-file replacement outside its ten scoped files. Run targeted GUI transform, Flame Sword UV and Greatsword particle tests; preserve LF-01/02 investigative and LF-05 deferred states.
5. Run Java 25 `./gradlew clean test build compileGametestJava runGameTest`, mapped-source probe, two-process persistence, server/client smoke, mixin JSON/registration checks, `git diff --check`, and full research-ownership/87-ID recount; verify no double invocation in server XP/harvest/mob paths.
6. Submit **Draft / NOT READY** code PR targeting #100 only when the combined code is testable. Record precise CI HEAD, unresolved manual/live checks, merge-bases, and traceability for #92/#93/#97/#99. Do not close, retarget, merge, or delete any existing PR automatically.
7. When all accepted, decide final promotion/rebase with explicit confirmation. The separately recorded 12-accessory spec remains PR #101 and is **not** silently added to Stage 23 gameplay code.

**Final review finding:** Two selective #93 behavioral fixes are needed (but .02 must be separated from .01); #97 and #99 are clean file-level pickup candidates; most of #92/#93's .55/.67/.03 work is already represented. Code integration, combined CI, and live visual checks are pending.

# Stage 23 remaining-branches reconciliation audit — 2026-10-16

**STATUS: PREPARATION ONLY / NOT MERGED.** Baseline is PR #100 branch `integration/stage23-linear-chain-20261016`, pinned to PR #98 HEAD `7231c7aad598e9cada152537dfece7cfad2756dc`. No existing PR branch was modified.

## Confirmed ancestry
- #88 → #89 → #90 → #91; #91 → #94 → #95 → #96 → #98. PR #100 stages this linear history.
- #92 branches from #89; #93 and #94 are siblings from #91; #97 and #99 are siblings from #94.
- #93 vs #94 compare: diverged, #93 has 15 unique commits, #94 has 5 unique commits, merge base #91 HEAD `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`.
- #98 vs #97: diverged (1 unique hotfix commit vs 26 commits on #98). #98 vs #99: diverged (7 unique Live-Fix commits vs 26 on #98).

## #92 and #93 — do not wholesale merge

#93 manifest `docs/RESEARCH_STAGE23A_PARALLEL_RECONCILIATION_MANIFEST_2026-10-15.md` describes unique reconciliation of research IDs .02 (extra log processing), .03 (mining durability context), .18 (beehive same-block postcondition), .55 (movement exhaustion), .67 (XP radius), and .19 (Fisheries). #94 manifest `docs/RESEARCH_STAGE23A_INTEGRATION_MANIFEST_2026-10-15.md` explicitly adopts only unique .19/.55/.67 effects from #92, excludes duplicate .02/.03/.18 handlers. Both branches implement overlapping research/fishing/mixin/authority files, so a wholesale merge risks duplicate handlers or replacing canonical behavior.

**Required follow-up:** compare actual current source behavior (not just manifest prose) for #93's .02 extra-log processing order, .18 same-block guard, .03 ownership gate, .55/.67 paths, and .19 exact tie allocation against PR #100. Selectively port only confirmed missing correctness fixes with focused tests. Do not duplicate handlers or rewrite current authority.

## #97 — exact Fisheries Study hotfix

#97 unique commit `df960d6511fbf9bbe0a649d8f68de6c12362d458`. Two files:
- `src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java`
- `src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java`

**Conflict risk:** PR #98 also changes research effect rules/tests. Do not overwrite #98's furnace/combat research effects with #97's older versions. Port only exact-fifths largest-remainder tie fix and assertions after examining the current target functions; run full deterministic Fisheries tests.

## #99 — Live-Fix A

#99 HEAD `eb80a148065c29394f7a46650c05ab72dda9140d`, 7 unique commits from #94. Compared with PR #98, its 10 changed paths are:
- `gradle/validate-flame-sword-production.gradle`
- `src/client/java/dev/itzgamesgd/moreminecrafts/client/EnchantingProgressionScreen.java`
- `src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordItem.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfx.java`
- `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScale.java`
- `src/main/resources/assets/moreminecrafts/models/item/flame_sword.json`
- `src/test/java/dev/itzgamesgd/moreminecrafts/equipment/FlameSwordModelTest.java`
- `src/test/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfxTest.java`
- `src/test/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScaleTest.java`

This set does not overlap #97's two paths. **Do not infer conflict-free integration from filenames alone**; check dependencies and compiled behavior. Three GitHub workflows at #99 HEAD reported success: Fabric CI, K5 Mapped Sources Probe, K5 Production Restart. Actual real-client visual acceptance is pending; LF-05 remains deferred and LF-01/LF-02 remain investigative.

## Proposed safe implementation order

1. Pin and verify target PR #100 HEAD; stop if changed unexpectedly.
2. Inspect #93 vs #100 at function level; selectively port missing unique corrections, with tests. Treat #92 as a reference only.
3. Port #97's exact-fifths correction into the current target implementation, preserving #98 effect rules.
4. Port #99's isolated Live-Fix A commits/patches; resolve compile/runtime dependencies and keep visual checks pending.
5. Run Java 25 `./gradlew clean test build compileGametestJava runGameTest`, mapped source, persistence, server/client smoke, `git diff --check`, research ID recount, and manual gameplay acceptance as applicable.
6. Create a new integration Draft PR for verified code; do not change, close, merge, retarget, or delete existing PRs without explicit instruction. No final checkpoint promotion before CI and conflict review.

**No code reconciliation or final integration has yet occurred.**

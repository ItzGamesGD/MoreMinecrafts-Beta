# SKY-MOB P1 implementation and verification audit (2026-11-06)

This is an implementation audit for the P1 branch. Issue #230 Q01–Q87 and approved D1–D6 remain the design authority; this report records code behavior and evidence without changing those decisions. P1-Q01 B authorizes use of Minecraft 26.3's native Looting loot condition while retaining Q85's exact 5% unenchanted emerald chance. The level-specific bonus values below describe the selected implementation curve only; they are not new approved balance decisions.

## Scope and state

- Base: `docs/sky-mob-p0-authority-20261106` at P0 commit `90ca11bc54ee85b3a9267d600668edc0cb052a3c`.
- Head at audit start: `0002278574e86dd6f2ab0c74e3f8339bd2c2a949` (`implement/sky-mob-p1-entities-20261106`).
- Existing P1 commits: `f33f8dc` entity implementation; `f17ff24` Crystal Bug collision/loadout and test corrections; `0002278` direct Cloud Slime attack test.
- Existing checkout was clean after selecting the requested branch. No GameTest process or prior local GameTest output was found.
- A prior GitHub Actions run for `0002278` is the only reusable full-suite evidence found. Its log is at [Fabric CI run 42156604064](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/42156604064).

## Entity audit

| Entity / ID | Registration, parent and fixed data | Audit result |
|---|---|---|
| 구름 망령 / `moreminecrafts:cloud_wraith` | `Zombie` through `SkyZombieBase`; zombie melee goals, target selector and attributes; 30 HP, attack 5, speed 0.23; 0.6×1.95; MONSTER. Q48 disables Drowned conversion. | **PASS** for registration, stats, inherited melee, fixed equipment and tested Drowned-conversion exception. **UNVERIFIED** for Q47/Q67 water-breathing parity until new vanilla-counterpart comparison runs. |
| 천궁 사수 / `moreminecrafts:skyguard_archer` | `AbstractSkeleton`; skeleton ranged goals, bow cadence, movement, range and target selector; 30 HP, skeleton speed, 0.6×1.99; arrow base damage multiplied by 1.4; fixed plain bow. It does not inherit `Skeleton`'s powder-snow Stray conversion path (D4). | **PASS** for inspected inheritance and direct arrow-damage test in prior CI; the Stray conversion GameTest also ran without a Sky-Mob failure. **UNVERIFIED** for exact water-breathing parity after Q51 classification and for untagged vanilla `#skeletons` target-selection effects (V5 remains pending). |
| 수정 벌레 / `moreminecrafts:crystal_bug` | `Silverfish`; 24 HP, attack 4, vanilla Silverfish speed; scale 2.5 with matching 2.5× collision dimensions; arthropod and bane-of-arthropods tags. `registerGoals` removes `SilverfishWakeUpFriendsGoal` and `SilverfishMergeWithStoneGoal`; the ordinary melee/retaliation goals remain. | **PASS** for the two goal removals, block/support prevention covered by its GameTest, type, scale, category and stats in prior CI. **UNVERIFIED** for all indirect Silverfish alert/support routes outside those goals until the full fixture rerun. |
| 구름 슬라임 / `moreminecrafts:cloud_slime` | `Slime`; size health 4/12/32, vanilla speed, 2.5× damage on damaging sizes, zero small-size damage, 2–4 same-type split children, final-small-only Wind Charge table. D5-a remains a P4 natural-spawn constraint. | Initial code **FAIL**: repeated `setSize` calls compounded the attack multiplier. Fixed by using one stable permanent 50% attack modifier and removing it for the tiny size. The expanded resize-sequence and full GameTests are **UNVERIFIED** until rerun. Prior CI checked size, tiny damage, split counts/types, loot and save/restore without a Sky-Mob failure. |
| 타락한 파수병 / `moreminecrafts:fallen_sentinel` | `Zombie`, reproducing Husk's shared Zombie movement/melee AI; 40 HP, attack 6, speed 0.23; undead-only classification; Husklike sounds, sun immunity and water-to-Zombie conversion. A successful melee hit rolls 40% Weakness I for 60 ticks. | **PASS** for direct effect-rate and water-conversion checks in prior CI; fixed equipment is tested. Husk-vs-Zombie differences are tabulated below. Reinforcement composition remains **UNVERIFIED** under V6. |

Common code and test coverage found: unique registry IDs, hostile category/cap, fixed loadouts, no pickup, no equipment drops, adult-only behavior, peaceful creation/removal, persistence, daylight/fire/fall/freeze/lightning immunity, and ordinary/explosion/suffocation damage. Design and code do not add natural-spawn or Trial Spawner hookups.

The P1 test class has 19 `@GameTest` methods at the audited head. It checks the entities at runtime; compile success alone is not treated as design compliance.

## Inheritance and vanilla behavior notes

| Choice | Behavior kept | Behavior changed or omitted |
|---|---|---|
| Cloud Wraith extends `Zombie` | Zombie melee path, target selector, adult collision and zombie oxygen/drowning code; no custom flight or AI. | Drowned conversion is explicitly disabled by Q48. Q51 classification is separate from vanilla counterpart water-breathing and is now asserted by a dedicated parity test; that assertion awaits execution. |
| Skyguard Archer extends `AbstractSkeleton` | Shared AbstractSkeleton bow attack implementation, ranged combat goal, skeleton cadence/movement/range and targeting. `getArrow` applies 1.4× vanilla base damage. | Avoids `Skeleton`'s Stray conversion implementation to satisfy D4. Class-based skeleton targeting remains, while `#skeletons` tag membership is still V5-pending and was not invented. |
| Fallen Sentinel extends `Zombie`, not `Husk` | Husk inherits the Zombie melee/movement implementation, so its core ground combat is shared. Sentinel copies Husk sounds and water-to-Zombie conversion. | It replaces Husk's Hunger-on-hit with the approved Weakness effect. It bypasses Husk-specific `finalizeSpawn` extras such as jockey/passenger setup; D1/Q55 require adult, fixed-loadout entities. It keeps only explicit Husk-like behavior rather than inheriting every present/future Husk override. Zombie reinforcement behavior can differ from Husk; V6 is unresolved and no custom exception was added. |

## P1-Q01 B loot function and probabilities

Crystal Bug uses `minecraft:random_chance_with_enchanted_bonus` on the emerald pool, with `unenchanted_chance: 0.05`, the Looting enchantment, and a `minecraft:linear` enchanted chance (`base: 0.06`, `per_level_above_first: 0.01`). This keeps the exact Q85 no-Looting chance at 5% and lets Minecraft 26.3 evaluate the killer's Looting level through its native loot context (Q64).

| Looting level | Function-derived chance in this implementation | Status of value |
|---:|---:|---|
| None | 5% | Q85-approved base |
| I | 6% | Implementation curve; not separately approved |
| II | 7% | Implementation curve; not separately approved |
| III | 8% | Implementation curve; not separately approved |

The original P1 GameTest sampled 4,000 seeds and logged 4.975% at level 0 and 8.0% at Looting III. The audit adds 20,000-seed checks at levels 0–III. These are statistical checks, not a single-drop assertion; the exact probabilities also follow from the JSON curve passed to the native condition. The 6/7/8% curve is not declared a new design authority; the still-open emerald economy/balance review remains in Q85's follow-up list.

## Existing verification evidence

### GitHub Actions at `0002278`

- Java 25 `compileJava`, `compileClientJava`, `compileGametestJava`, `test`, and `build`: **PASS**.
- `runGameTest`: **FAIL**. The log reports one required failure: `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise`, assertion “return resumes the same attempt on tick 74”. It is outside the P1 changed-file list and is classified as an existing unrelated regression/flaky test pending baseline confirmation; the remainder of the runner emitted no required-test failures.
- Sky-Mob log markers were present for loot (4.975% base, 8% Looting III), Husk water behavior and slime splitting; no Sky-Mob failure was reported. All 19 Sky-Mob tests are treated as passed in that run because the sole reported failed test is the Colosseum test.
- Server and client smoke jobs were **SKIPPED** because the build job failed at `runGameTest`. This is not a smoke-test pass.
- Additional workflow runs on that SHA: Sky Islands Validation, K5 Mapped Sources Probe, K5 Stage 23-A Batch 3 Crafting Probe, Tooling Readiness, and K5 Production Restart all completed successfully. They are not substitutes for this branch's build, GameTest or smoke checks.

### Local audit environment

- Java 25.0.4.1 is installed and selected. Gradle wrapper 9.6.0 is present.
- Local `compileJava compileClientJava compileGametestJava test build` did not reach project configuration: Gradle could not resolve Fabric Loom 1.17.21 because the Java process could not reach Maven (`Network is unreachable`). Direct `curl` to the Fabric Maven artifact returned HTTP 200, so this is recorded as a local Gradle network/proxy limitation, not a source compile failure.
- The new resize, water-breathing parity, and Looting I–III checks have not yet run. Final P1 status remains **UNVERIFIED** until the updated branch CI result is available.

## Scope boundary and open findings

**OUT_OF_SCOPE for P1:** natural-spawn module and weights (including D5-a/D5-b implementation), Trial Spawner wiring and 2–3 mob composition, C2, C4, C5, combat-combination table, safe-fall pursuit, final models/textures/animation, and live server/client acceptance beyond available smoke workflows.

**UNVERIFIED:** updated GameTest suite and total suite count; water-breathing parity against each 26.3 vanilla counterpart; exact 2.5× Crystal Bug collision in world placement contexts; indirect Silverfish support/infestation paths beyond the removed goals; V5 class/tag target-selection consequences; V6 Zombie/Husk reinforcement mix; client and server smoke.

**FAIL at prior CI head:** one unrelated Colosseum GameTest, documented above. No new design exception was introduced to clear it.

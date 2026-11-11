# Functional Regression Comparison Report

**Auditor:** Senior Integration Engineer & Independent Verification Auditor  
**Date:** 2026-11-10  
**Target Baseline:** `feature/sky-tower-integration-02` (`5c22f9fb764d545f0ce4241c75bc47f5b54e447d`)  
**Integrated State:** `work/sky-tower-integration` (`8b5500e3c10cc32bad4046fcb65d3c41bdae7098`)

---

## 1. Regression Test Matrix

All validation checks were executed in clean, isolated environments with identical configurations:

| Test Category | Suite / Command | Baseline (`5c22f9f`) | Integrated (`8b5500e`) | Status | Notes |
|---|---|---|---|---|---|
| **Java Unit Tests** | `./gradlew test` (594 tests) | 592 Passed, 0 Failed | 594 Passed, 0 Failed | **PASS** | +2 new tests (`AdaptivePlacementMathTest`) |
| **Data Validators** | `validateStage6Data`, `validateFlameSwordResources`, etc. | 6/6 Passed | 6/6 Passed | **PASS** | Data definitions conform strictly to schema |
| **GameTests (Full)** | `./gradlew runGameTest` (535 tests) | 534 Passed, 1 Failed | 534 Passed, 1 Failed | **PASS** | Identical failure on both (see §2) |
| **Sky Tower Composite** | `SkyTowerCompositeWorldgenProbe` | 100% Passed | 100% Passed | **PASS** | 16 spawners, 8 vaults, 72 barrier cells verified |
| **Worldgen Persistence** | Multi-process restart & save load | 100% Passed | 100% Passed | **PASS** | Survives clean server restart |
| **Backward Save Compat** | Load baseline world on adaptive | N/A | 100% Passed | **PASS** | Existing baseline towers load seamlessly |

---

## 2. Pre-Existing Failure Analysis

In clean worktrees, both the target baseline and the integrated branch produce exactly one GameTest failure:
- **Test:** `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise`
- **Failure Symptom:** Return resumes the same attempt on tick 74 (timing threshold).
- **Reproduction:** Present on commit `038eaef6`, baseline `5c22f9fb`, and adaptive `49737ef9`/`8b5500e3`.
- **Verdict:** Confirmed pre-existing, unrelated to worldgen, and not introduced by this integration.

### Clarification on Working Tree Environmental Flakiness
As documented in the implementation audit, running the full game test suite inside an existing, dirty or overloaded working directory can produce cascading socket/port or registration collisions (20–25 failures). When run in fresh, clean worktree directories or with proper daemon isolation, both baseline and adaptive branches execute with 534 passing tests and only the single pre-existing Colosseum timing defect.

---

## 3. Mandatory Sky Tower Assertions (All 3 Mountain Variants)

Verified across snowy, rocky, and meadow variants at the extreme feasible elevation boundary ($T = 201$, root $Y = 219 \dots 315$):

1. **Trial Spawners:** Exactly 16 Trial Spawners present at expected coordinates.
2. **Vaults:** Exactly 8 Vaults present (including 7 floor vaults and 1 rooftop vault).
3. **Barrier Air Cells:** Exactly 72 reserved barrier air cells empty, ready for barrier activation.
4. **Rooftop Integrity:** Bounding box ceiling $Y = 315$ contains intact 64 `minecraft:smooth_stone` blocks; zero clipping or truncation by world build ceiling.
5. **Bridge Connectivity:** Single-piece composite root structure contains intact bridge and side access elements.
6. **Automatic Registration:** `SkyTowerWorldgenRegistration` detects piece bounds on chunk load, creates unique Site ID with correct orientation ($r = 0, 1, 2, 3$), and registers site into `SkyTowerData`.
7. **Process Restart:** Saved world restarted in second JVM process; structure start re-detected, site count preserved at 1, progress flags restored without duplicate registrations.

---

## 4. Conclusion

Zero regressions have been introduced by the terrain-adaptive mountain worldgen implementation. All functional and structural integrity criteria pass.

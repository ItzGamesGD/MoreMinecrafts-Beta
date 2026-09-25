# Checkpoint 11

> **Status:** READY FOR NEXT WORK  
> **Integration branch:** `stage-11/checkpoint`  
> **Source checkpoint:** `stage-10/draft-content` @ `a2acc715ad2ee9e7af06d84dab7d25e7b84e31c8`

## 1. Promotion basis

Checkpoint 11 starts from the completed Stage 10 integration state after PR #30:

- PR #30: **Stage 10B: Establish Enhancement 2.0 foundation**
- PR head: `f58356ae59e88d589d0b952f82fac4d925ef369f`
- Merge commit: `a2acc715ad2ee9e7af06d84dab7d25e7b84e31c8`
- Fabric CI run #140: **SUCCESS**
- Stage 10B automated validation is accepted as the implementation checkpoint.
- Manual/live gameplay validation is intentionally NOT claimed complete.

## 2. Enhancement 2.0 carry-forward state

### Implemented / automated checkpoint

- T1/T2 ordinary Enhancement cap = +20.
- Deterministic milestones = +5 / +10 / +15 / +20.
- Promotion/random reroll/failure/destruction/pity/Coin remain removed.
- Continuous growth curves and milestone descriptors are separated.
- Flame Sword and Wind Sword resolve as Elemental enhancement equipment.
- Anvil remains a thin temporary adapter over UI-independent enhancement logic.
- Tool effective-block safeguard remains present.
- Current armor provisional runtime target is bounded around an 8% full-set enhancement contribution.
- T3/T4 ordinary Enhancement remains disabled.

### LIVE PENDING

The following are deliberately carried forward without claiming human verification:

- Full +0 -> +20 progression and milestone transitions.
- Flame Sword / Wind Sword state and ability preservation through enhancement.
- Provisional sword combat curve.
- Bow / Crossbow projectile regression behavior.
- Trident melee / thrown / Loyalty / persistence behavior.
- Four-piece +20 armor live damage behavior.
- Enhanced tool + Efficiency V break-time behavior across representative blocks.

The authoritative current list is:
`docs/LIVE_VALIDATION_BACKLOG.md` → **Stage 10B - Enhancement 2.0 LIVE PENDING**.

Older Stage 3/4 enhancement checks remain historical records only where they conflict with Enhancement 2.0.

## 3. Authoritative design records

Use these before future equipment-growth work:

- `docs/ENHANCEMENT_2_REDESIGN.md`
- `docs/EQUIPMENT_PROGRESSION_FINALIZATION.md`
- `docs/CONTENT_FINALIZATION_CHECKLIST.md`
- `docs/FULL_CONTENT_RECOVERY_RECONCILIATION.md`
- `docs/LIVE_VALIDATION_BACKLOG.md`
- `docs/T2_ELEMENTAL_RECONCILIATION.md` (Stage 11 Paper/Fabric mapping and prototype delta)

## 4. Deferred by design

Do not silently resolve these as part of unrelated Stage 11 work:

- final Mace smash/fall-damage formula
- final Bow draw-speed / Crossbow charge-speed tuning
- final tool utility milestones and mining-speed curve
- final armor arithmetic/order beyond the current provisional bound
- final Enhancement material/XP economy
- final Enhancement workstation/UI
- T3 separate progression system
- T4 kill-point Grade/Mastery details
- monster/player/world scaling model

Monster scaling is explicitly deferred to the later mob/balance stage, especially because structure-mod dependency and vanilla-only world segmentation are still unresolved.

## 5. Stage 11 workflow

New feature work should:

1. branch from `stage-11/checkpoint`;
2. use one bounded vertical slice per PR;
3. target PRs back to `stage-11/checkpoint`;
4. keep CI/build/runtime smoke separate from human LIVE validation;
5. append new human checks to `docs/LIVE_VALIDATION_BACKLOG.md`;
6. never mark LIVE PENDING items complete solely because CI passes.

Checkpoint 11 itself introduces no new gameplay feature.

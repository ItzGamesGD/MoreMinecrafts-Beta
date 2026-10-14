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

## Runtime and design stabilization follow-up

### AUTHORITATIVE contracts implemented

- Minecraft 26.3 ore feature targets use the string block-state codec; validation rejects legacy
  object-shaped `Name`/`Properties` states. Server smoke now requires the post-datapack `Done (...)`
  marker and rejects fatal registry/datapack messages.
- Ordinary T1/T2 enhancement remains deterministic through +20 with milestones at +5/+10/+15/+20.
  Vanilla Mace enhancement scales the fall distance consumed by vanilla smash damage, never its
  generic melee attribute. Mace milestones represent impact radius and secondary-target capacity.
- Reel unlocks species, Minnow weights rarer *eligible* species, and Crankbait weights Grade. Species
  rarity, Grade, and Length remain separate catch dimensions.
- The seven-item T2 roster is registered and available from the Combat creative tab. Flame and Wind
  trigger their sweep on every tenth server-confirmed successful hit. Their sole active input is right
  click; Wind Step is no longer bound. Water, Lightning, Earth, Wind Bow, and Staff have live-testable
  carriers/abilities while final balance remains open.

### PROVISIONAL live-test values (not final balance)

- Every current enhancement transaction costs a minimum of 1 XP level. The long-term enhancement XP
  economy remains subject to balancing.
- Mace effective fall distance is `actual × (1 + 0.025 × level)`; +5/+10/+15/+20 radii are
  1.5/2.0/2.5/3.0 blocks, target caps are 1/2/3/4, and secondary damage is 35% when wired.
- Sword attack-speed/sweeping and Bow/Crossbow draw/reload/velocity milestone magnitudes remain
  provisional and require a later tuning slice. Tool/Axe durability/utility, Armor milestone utility,
  and Trident special milestones remain explicitly deferred.
- Fishing exposes exactly Bluegill/Trout/Pike and Grades I/II/III, with each species' existing random
  length range persisted on the stack. All weights and length ranges are live-test data.
- Water heal (2), movement, 7-damage/1.5-radius column and 12-second cooldown; Lightning 8-damage slash
  and 10-second cooldown; Earth 4-damage/3-radius shockwave and 15-second cooldown; Staff 7-damage,
  12-block beam and 8-second cooldown; Flame active 10 damage; Wind passive 3 damage are PROVISIONAL.
  Wind Bow currently preserves ordinary vanilla Bow behavior as its minimum usable live-test carrier;
  its documented mild steering identity remains a warning/open implementation item.

### Validation status

Local JSON validation and Java compilation pass. A complete local dedicated-server run is currently
blocked by this environment returning HTTP 403 for missing Fabric runtime artifacts; runtime readiness
must therefore be confirmed by CI and its uploaded/full log before this slice can be treated as ready.
The client job is deliberately named **client-initialization** and does not claim graphical readiness.


## Resource asset work policy

Resource completeness is now a first-class tracked backlog and must be carried in both the repository
documentation and the mirrored Google Drive project records.

Resource work is intentionally separated from gameplay/runtime implementation after this stabilization
slice. Gameplay code may use explicit placeholder vanilla assets to keep vertical slices testable, but a
feature is not considered visually complete until its resource backlog is closed.

### Current resource audit

- Titanium Ore: blockstate/model wired; currently reuses vanilla Iron Ore texture.
- Tungsten Ore: blockstate/model wired; currently reuses vanilla Deepslate Iron Ore texture.
- Mythril Ore: blockstate/model wired; currently reuses vanilla Diamond Ore texture.
- Etherium Ore: blockstate/model wired; currently reuses vanilla Emerald Ore texture.
- Adamantium: gameplay/worldgen material exists, but dedicated blockstate/model asset coverage is still
  missing and must be completed in the resource slice.
- Raw/ingot/material item models currently reuse vanilla item textures where present.
- T2 equipment carriers may use placeholders during gameplay verification; final unique
  models/textures/animation/VFX/SFX remain resource backlog items unless explicitly completed.

### Next resource slice

Do not mix broad visual production into runtime bug-fix/gameplay PRs. The next dedicated resource stage
should audit the complete implemented-content asset matrix and then add, at minimum:

1. unique vanilla-style 16x16 textures for the five material families where applicable;
2. missing blockstate/block/item model wiring, including Adamantium;
3. raw/material/ingot item textures and model links;
4. T2 equipment model/texture coverage and explicit placeholders for anything still awaiting final art;
5. an asset checklist that distinguishes MISSING / PLACEHOLDER / IMPLEMENTED / FINAL;
6. client-side live verification for missing-texture/model fallbacks.

Gameplay behavior and balance remain separate from final art polish. A later model/VFX stage may replace
these baseline assets without reopening gameplay design.

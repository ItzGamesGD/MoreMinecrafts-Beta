# Current content authority and deferred design debt

This file is the compact authority map for the recovered cumulative line.

It does not replace the detailed design documents. When an older document conflicts with a newer reconciliation document listed here, the newer reconciliation is authoritative for implementation.

## Current authority

### Cumulative recovery foundation

Use the Stage 1 cumulative recovery line as the trusted implementation base. Historical checkpoint branches remain forensic history and are not implementation authority when they conflict with the recovered cumulative tree.

### Enhancement and T2 gameplay

Current contract: [Stage 2 gameplay and progression reconciliation](STAGE_2_GAMEPLAY_RECONCILIATION.md).

Authoritative ordinary Enhancement rules:

- T1 Vanilla: maximum +20
- T2 Elemental: maximum +20
- deterministic milestones at +5 / +10 / +15 / +20
- no ordinary T3/T4 Enhancement
- historical +30 / +40 rules are superseded

Current T2 input/gameplay rules include the seven-item final roster, right-click active skills, cumulative Lightning charge semantics, and the removal of Wind Step and the old F / Shift+F T2 path.

### Fishing

Current Fishing implementation and runtime/resource contract: [Stage 3 Fishing runtime/resource/UX reconciliation](STAGE_3_FISHING_RESOURCE_RECONCILIATION.md).

Older Stage 7 and content-finalization sections are historical where they conflict with Stage 3.

Current Fishing authority:

- final 12-species roster
- Reel / Minnow / Crankbait equipment Tier I–III only
- Fish Grade I–V
- species first, minigame, Minnow capture permission, Grade, Length, then Length-dependent Weight
- approved Resource D master PNGs remain immutable
- runtime item presentation is a separate model contract and must not default to unsafe generated alpha-extrusion geometry

Tier I–V modules, independent Length/Weight rolls, late-only Minnow notice, retired fish, and generated-item geometry are superseded where they conflict with Stage 3.

### Approved resource assets

Runtime presentation rules: [Resource asset presentation contract](RESOURCE_ASSET_PRESENTATION_CONTRACT.md).

An approved master image and its Minecraft runtime model are separate concerns.

Human-approved production PNGs remain immutable unless an explicit later art task changes them. Runtime model geometry, display mode, and performance constraints may be changed without altering the approved master asset.

Flame Sword C1-A/C1-B remains separately authoritative; its approved production PNG and bounded two-face handheld model are unchanged.

## Explicitly deferred design debt

The following items are known design questions. They are not missing recovery work and must not be silently "fixed" by unrelated stages.

### T2 elemental identity

**Fire / Wind passive identity overlap — DEFERRED**

Both currently use hit-count-based broad AoE passives. Their mechanical identity may need stronger separation after live combat testing.

Handle during a dedicated T2 combat/VFX/balance pass. Do not redesign them during unrelated infrastructure or resource work.

### Fishing environment eligibility

**Binary substantial-water eligibility — DEFERRED**

The current tolerant substantial-water threshold is accepted for the recovered vertical slice. Similar-looking bodies of water can still fall on opposite sides of the custom-fishing threshold.

Revisit only during a future ecology/environment pass. Do not block the current Fishing implementation on this issue.

### Materials and resource economy

**Material sink identity — DEFERRED**

Titanium, Tungsten, Mythril, Etherium, and Adamantium should each have a meaningful anchor use before final spawn rarity and long-term material economy are considered fully settled.

Resolve during future crafting/equipment/material-economy design rather than by changing worldgen opportunistically.

### Utility progression

**Movement utility overlap — DEFERRED**

Jet Boots, Glider, Grappling Hook, and Return Crystal may collectively trivialize vanilla terrain/travel if acquisition timing and roles are not separated.

Resolve during the Utility implementation/design pass, especially relative to Elytra progression.

### Alchemy discoverability

**Brewing discovery UX — DEFERRED**

Alchemy remains a Brewing Stand extension, but the custom effects/catalysts need discoverability support such as advancement hints, ingredient tooltips, recipe hints, or a compact discovery record.

Avoid turning this into a full combinatorial catalyst matrix. Resolve during the Alchemy UX/content pass.

### Boss and higher-tier dependency order

**Boss -> T3/T4 reward dependency — DEFERRED**

Do not hard-lock T3/T4 recipes around undefined boss drops.

Preferred order for later design:

1. boss role and encounter identity
2. reward identity
3. rare material/drop
4. T3/T4 recipe/progression connection

### T4 progression

**Generic kill-to-Mastery farming risk — DEFERRED**

Do not implement unrestricted generic monster-kill XP as the final T4 progression contract without a dedicated design review.

If T4 Mastery is retained later, its progression source should avoid making weak-mob farming the dominant strategy.

### Progression-system proliferation

**T3 separate growth system — REVIEW BEFORE IMPLEMENTATION**

Ordinary Enhancement already covers T1/T2. T3 should not automatically receive another independent grind system merely because an older document proposed one.

Re-evaluate whether T3 needs any separate growth axis before implementation.

### Coating

**Coating — TEMPORARILY ON HOLD**

Coating is not deleted from project history, but it is not part of the active progression implementation.

Do not implement, rebalance, expand, or connect Coating to T3/T4 until higher-tier armor/progression is reconsidered. The historical coating matrix is reference material only.

## Idea parking lot

The following concepts are intentionally preserved only as ideas. They are not current design authority and are not implementation-ready.

### New gameplay-axis concepts

**New gameplay-axis design — DEFERRED BY DEFAULT**

New standalone or cross-system gameplay axes that are not already part of current authority are intentionally deferred.

Examples include:

- Knowledge / Research,
- Trading as a new independent economy/exchange axis,
- other newly proposed progression, economy, research, social, or cross-system axes.

Preserve promising concepts here at idea level only. Do not expand them into detailed design, promote them into current authority, or implement them unless a later human-approved design task explicitly selects that concept.

### Knowledge / Research

**Knowledge / Research — IDEA / DEFERRED**

Potential new horizontal progression axis.

Possible direction:

- spend vanilla XP to acquire persistent knowledge,
- provide small benefits across multiple gameplay systems,
- do not gate recipes or ordinary content access.

Exact categories, effects, progression structure, balance, and UI are intentionally undefined.

Do not implement or infer missing design from this idea entry. A dedicated design decision is required before promotion into current authority or implementation scope.

## Live validation

Items marked LIVE PENDING in the Stage 2/Stage 3 reconciliation documents remain live-test work rather than documentation defects.

Headless CI must not be treated as proof of combat feel, visual presentation, interaction feel, or economy balance.

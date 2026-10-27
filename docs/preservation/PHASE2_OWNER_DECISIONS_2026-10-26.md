# Phase 2 preservation — owner decisions (2026-10-26)

Status: **OWNER APPROVED DECISIONS / IMPLEMENTATION PENDING**.
This is a preservation record, not evidence that gameplay code has been implemented or validated. It does not replace existing detailed approved design documents until their reconciliation is reviewed. Source: owner decisions in the Phase 2 preservation-audit follow-up. Do not promote historical shelved territory mechanics.

## F-1 — Wither-skull provenance
Only the **first pickup** of a skull genuinely dropped by a Wither Skeleton counts. Repeated pickup of the same skull does not count again. The 2026-10-18 repeat-pickup rule is superseded by the later owner decision recorded in PR #127. Preserve the stable XP Quest ID, target of three and reward of 1100 XP. PRs #128/#130 are not accepted as release-ready; the reported crash-consistency defect must be corrected and tested before registration.

## F-2 — Earth Mace and Ancient Heart
The Tomb Guardian's boss-exclusive drop is **Ancient Heart**, not a separate Tomb Guardian Core. Keep the most recently approved Earth Mace crafting recipe and its layout/other ingredients, replacing only the Tomb Guardian Core ingredient with Ancient Heart. Preserve the existing Ancient City acquisition route for Ancient Heart. Do not introduce a new distinct core item. This resolves the Oct 7 versus pyramid material naming conflict; implementation and recipe availability remain pending.

## F-3 — Adamantium world generation
Approved: Y **8–22**, ore veins **1–2**, generation biased toward **Y 15**. Adamantium must yield **fewer generated ore blocks per comparable newly generated chunk sample than Ancient Debris**. Validate with matched sampling and disclose dimension/biome/height methodology; do not equate vein size alone with rarity. Current Y 8–24/size 3 implementation is not authoritative.

## F-5 — Rare caravan and special contracts
Rare caravan merchants **must not directly sell special contracts**. Keep previously approved ordinary contract acquisition rules and the existing **5% advanced-to-special conversion**. No change to other approved special-contract sources is authorized.

## F-6 — Colosseum identities
The arena **inside the Ancient City** and **ST-05 standalone Colosseum** are **separate structures**, not aliases. Keep separate identities and implementation/design tracks. Do not infer that a separate repository automatically implements either structure.

## Mercenaries — candidate only
Preserve the mercenary concept as a **territory-independent future candidate**, status **CANDIDATE / DEFERRED / NOT AUTHORIZED FOR IMPLEMENTATION**. Territory system remains shelved. Historical territory-dependent mercenary mechanics are not revived.

## Preservation and implementation constraints
- Preserve the approved 2026-10-17 Stage 23-A research decision record and other uniquely approved documents with exact source commit provenance; catalog entries do not prove behavior implementation.
- Preserve independent territory-chain concepts (Adamantium equipment, pillager patrol, rare caravan/runes), with original approval status and dependencies accurately labeled.
- Preserve the latest pyramid and pillager HQ versions; do not overwrite newer pyramid material with PR #147's older copy.
- Structure authority index routes should be added only after verifying paths and approval statuses.
- No gameplay, worldgen, loot, recipes, research effects, XP Quest registration, or live test results are changed by this document.
- PR #181 and main must not be mutated by this preservation work.

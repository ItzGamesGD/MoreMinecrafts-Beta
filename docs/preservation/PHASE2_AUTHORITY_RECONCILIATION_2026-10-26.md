# Phase 2 authority reconciliation (2026-10-26)

Status: **CURRENT AUTHORITY OVERLAY — DESIGN ONLY / IMPLEMENTATION PENDING.**
Source decisions: [PHASE2_OWNER_DECISIONS_2026-10-26.md](PHASE2_OWNER_DECISIONS_2026-10-26.md) (owner-approved F-1, F-2, F-3, F-5, F-6 and mercenaries). Recovered records and their provenance: [PROVENANCE_MANIFEST_2026-10-26.md](PROVENANCE_MANIFEST_2026-10-26.md).

This overlay applies those six decisions to the design documents they affect. Where a named document says something different on the **same point**, this overlay wins for that point only. Every other approved rule in those documents stays exactly as written. No gameplay code, worldgen data, loot table, recipe, research effect, XP Quest registration or test result is changed or claimed.

**Why this is an overlay.** The affected documents began on the Wave 9 release branch, PR #181. The pyramid design has since been carried byte-identically into PR #182; the source version cited below remains the pinned PR #181 blob at `268ec65bb8827c3f627d076188c7924225b8afa0`. PR #181 is release-frozen. Preserve the original documents and their provenance; add short in-place pointers after integration if needed. This overlay and [CONTENT_AUTHORITY_INDEX.md](../CONTENT_AUTHORITY_INDEX.md) carry the approved decisions now. Do not rebase or squash commits with SHA-dependent Gitleaks fingerprints merely to synchronize documentation.

## F-1 — Wither-skull pickup provenance

Affected authority: `docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md` on PR #181 (blob `9553550170c9388c1e0fad4992467723c835705d`), section "Owner decision (2026-10-18): `xpq.cmb.wither_skulls_3` acquisition semantics". Catalog row `xpq.cmb.wither_skulls_3` in `docs/design/xp-quest/A_XP_QUEST_CATALOG.md` on PR #181.

Resulting rule:

- Progress counts only the **first successful pickup of a skull that was genuinely dropped by a Wither Skeleton**. Picking up the same skull again after it has been dropped, moved or traded does **not** count again.
- **Superseded:** the 2026-10-18 sentence "Re-picking up the same physical skull after dropping it **does count again** … no deduplication or origin tracking is authorized." The later owner decision recorded in PR #127 replaces it. Origin tracking of genuine drops is now required.
- **Unchanged from 2026-10-18:** quest ID `xpq.cmb.wither_skulls_3`; target **3**; reward **1100** raw XP; server-authoritative attribution to the receiving player; only transferred quantity counts; failed or zero-transfer pickups grant nothing; the `get_wither_skull` advancement seeds at most the existing historical lower bound of 1, with no double count.
- **Registration hold:** keep `registered=false` and `tracker.kind=pending`. PRs #128 and #130 are **not release-ready**. Their reported crash-consistency defect must be fixed and tested before the quest is registered. Do not register the incomplete implementation.

## F-2 — Ancient Heart replaces Tomb Guardian Core

Affected authority: `docs/PYRAMID_DESIGN_PREP_2026-10-25.md` on PR #181 (blob `52cc81573ee34a65b9201fd86125bd4c2a6a0772`), sections from "Owner direction — Tomb Guardian Core as Earth Mace crafting ingredient" through the locked boss summary. Also the Oct 7 core authority in [CONTENT_FINALIZATION_CONTINUATION_2026-10-07.md](../CONTENT_FINALIZATION_CONTINUATION_2026-10-07.md) and [CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md](../CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md).

Resulting rule:

- The Tomb Guardian's boss signature drop is the existing T2 Earth core **Ancient Heart / 고대의 심장** (`moreminecrafts:ancient_heart`). No separate "Tomb Guardian Core" (왕릉 수호자의 핵) item exists. Do not register a new core item.
- The approved shaped Earth Mace recipe keeps its layout and other ingredients. Only the centre ingredient changes:

  ```
  Diamond Block | Heavy Core      | Diamond Block
  (empty)       | Ancient Heart   | (empty)
  (empty)       | Netherite Ingot | (empty)
  ```

  Output **Earth Mace ×1**. The recipe is shaped (positions matter) and uses Diamond Block ×2, Heavy Core ×1, Ancient Heart ×1 and Netherite Ingot ×1.
- Every pyramid rule that names the Tomb Guardian Core applies to Ancient Heart **as the guardian's drop**. In particular, it is obtained by killing the Tomb Guardian rather than from the royal chest, with no duplication. The open questions in the pyramid document (quantity per kill, per-player ownership, inventory versus world drop) stay open.
- **Ancient City remains an Ancient Heart source.** The Oct 7 acquisition route (Ancient City chest table, provisional 3% chance, quantity 1) is unchanged. As a result, Ancient Heart is the guardian's signature reward but is not obtainable *only* from the guardian.
- This resolves the naming conflict between the Oct 7 T2 core identities and the pyramid material. It also settles the pyramid document's "authority conflict to resolve before implementation" paragraph: the existing T2 core is the ingredient, so no new mandatory component is introduced.
- Implementation status: no Earth Mace crafting recipe exists on `main` or on PR #181 today. Recipe availability, Tomb Guardian implementation and live validation remain **pending**.

## F-3 — Adamantium world generation

Affected authority: [RESOURCE_MINING_MATERIALS_FINALIZATION.md](../RESOURCE_MINING_MATERIALS_FINALIZATION.md) (Adamantium row "Ancient Debris 유사 초안"), and the recovered [ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md](recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md), section "Ore generation and refining". The amendment is also recorded in place in `RESOURCE_MINING_MATERIALS_FINALIZATION.md`.

Resulting rule:

- Nether only. Height **Y 8–22**, biased toward **Y 15**. Vein size **1–2** ore blocks.
- Rarity acceptance: Adamantium must produce **fewer generated ore blocks than Ancient Debris** in matched samples of comparable newly generated chunks. Validation must disclose its dimension, biome and height-sampling method. Vein size alone is not evidence of rarity.
- The 2026-10-18 "approximately 80% of ancient debris" operational target is consistent with this rule and stays as a tuning target. The binding acceptance criterion is "fewer blocks than Ancient Debris under matched sampling".
- **Not authoritative:** the current data (`worldgen/placed_feature/ore_adamantium.json` uniform Y 8–24, `worldgen/feature/ore_adamantium.json` size 3). These files are not changed by this PR. Correcting them is a separate implementation task.
- Mining tier, smelting, upgrade material, equipment statistics and template sources in the Adamantium design are unchanged.

## F-5 — Rare caravan does not sell special contracts

Affected authority: `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` on PR #181 (blob `a26240835dfab8a7ee94ae1dbe851403dbfef7b8`). The recovered [BOUNTY_COMMISSION_APPROVED_2026-10-19.md](recovered/territory-chain/BOUNTY_COMMISSION_APPROVED_2026-10-19.md) §1, the recovered [ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md](recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) caravan merchandise groups, and [DESIGN_CONFLICT_REGISTER.md](recovered/territory-chain/DESIGN_CONFLICT_REGISTER.md) rows F08 and F19.

Resulting rule:

- Rare caravan merchants **must not directly sell special contracts**.
- **Superseded:** "Special bounty contract items are obtained from the already-designed rare merchant caravan" (2026-10-19 commission design §1). Also superseded: register row F19 ("Caravan can list special contracts"), and "bounty contracts" or "rare dungeon maps/contracts" as caravan exclusive merchandise, insofar as those cover special contracts.
- **Unchanged:** the approved ordinary contract acquisition rules in `BOUNTY_CONTRACT_APPROVED_2026-10-22.md` (villager seller stock, eligible structure containers at Easy 5% / Medium 10% / Hard 18%) and the **5% Advanced-to-Special conversion**, together with its approved special economy (Advanced price 12–20, payout 25–50, rarity 20/45/30/5). Direct special drops from structures or events remain DEFERRED exactly as recorded there. No other special-contract source is added or removed.
- Whether the caravan may sell **ordinary** contracts is not decided by F-5. See remaining conflicts.

## F-6 — Ancient City arena and ST-05 are separate structures

Affected authority: the recovered [ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md](recovered/content-first-axis-review/ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md) ("고대도시 경기장 / 콜로세움") and `docs/STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md` on PR #181 (blob `dafe1d5dd31926779bb554ca9bfdc550eeec43b5`), row ST-05.

Resulting rule:

- The **arena inside the Ancient City** (an independent landmark added to Ancient City generation; first prototype PR #83) and **ST-05 Ancient Colosseum** (a standalone underground arena found only in an enormous natural cave) are **two separate structures**, not aliases. Keep separate identities, IDs and design or implementation tracks.
- Do not infer that a separate repository (for example `MoreMinecrafts-Structure-Generator`) implements either one.
- Routing: [STRUCTURE_AUTHORITY_INDEX.md](../STRUCTURE_AUTHORITY_INDEX.md).

## Mercenaries — territory-independent candidate only

Affected record: the recovered [DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md](recovered/territory-chain/DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md) "Mercenary hiring [APPROVED]", and the "Guards / mercenaries" row of [TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md](recovered/territory-chain/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md).

Resulting status: **CANDIDATE / DEFERRED / NOT AUTHORIZED FOR IMPLEMENTATION**, as a territory-independent future concept. The 2026-10-19 mercenary details (jobs, grades, fees, term, equipment, healing) are preserved as historical design input only. The "Unlock at territory T3" dependency and any other territory coupling are **not** revived. Adopting mercenaries requires a new owner decision. The territory system stays shelved.

## Routing added for previously unrouted records

| Area | Route | Status |
|---|---|---|
| Stage 23-A remaining research | [RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md](recovered/stage23a-remaining-approval/RESEARCH_STAGE23A_REMAINING_USER_DECISIONS_2026-10-17.md) | APPROVED design authority for the topics it names; its listed open items stay open |
| Territory shelving | [TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md](recovered/territory-chain/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md) | Current scope authority: territory SHELVED; released ideas are CANDIDATE |
| World event: pillager patrols | [WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md](recovered/territory-chain/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md) | DESIGN FINALIZED, not implemented |
| Equipment: Adamantium | [ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md](recovered/territory-chain/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) + F-3 | Approved v1, not implemented |
| Rare caravan and runes | [RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md](recovered/territory-chain/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md) + register F20/F21 + F-5 | User-confirmed, not implemented; open items listed in the source |
| Structures (pyramid, pillager HQ, candidates, Ancient City) | [STRUCTURE_AUTHORITY_INDEX.md](../STRUCTURE_AUTHORITY_INDEX.md) | See index |


## Owner follow-up decisions — Q1–Q4 (2026-10-26)

These decisions supersede the corresponding **open questions** in the historical "Remaining conflicts" section below. That section is retained as an audit trail, not as a current list of unresolved owner choices. These decisions are **approved design authority**, not evidence of implementation.

### Q1 — 2026-10-22 contracts supersede 2026-10-19 commission board

The approved **2026-10-22 contract-item system is the sole current bounty/contract authority**. The 2026-10-19 commission-board design is **historical reference only**. Do not implement or restore its rotating board, UI entry, independent active-contract limits, or separate payout rules. Preserve the original recovered document unmodified for provenance. This does not revoke approved mechanics of the 2026-10-22 system.

### Q2 — Rare caravan may sell ordinary contracts

Rare caravan merchants **may directly sell Beginner, Intermediate and Advanced ordinary contract items**, in addition to previously approved acquisition sources. They **may not directly sell Special contracts**. A contract sold as ordinary remains an ordinary sale item; do not silently convert a merchant's stock into a direct Special offering. Preserve the approved 5% Advanced-to-Special conversion for its applicable existing generation/acquisition flow; clarify merchant-purchase conversion behavior before implementation rather than treating the permission to sell ordinary stock as permission to sell Special contracts. Caravan prices, stock weights, quantity limits and exact implementation remain **OPEN FOR BALANCE DESIGN**.

### Q3 — Feather Boots and Hot Step deferred

The **existing implemented T1 Feather Boots enchantment is approved to remain**. Its current skill-tree placement and retained gameplay semantics are governed by [T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md](../T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md) and [ENCHANTMENT_BRANCH_SKELETON.md](../ENCHANTMENT_BRANCH_SKELETON.md). Do not remove or disable it. Only the proposed **2026-10-10 Feather Boots redesign** (double jump and full fall-damage immunity in place of the existing dash) remains **CANDIDATE / DEFERRED / NOT AUTHORIZED FOR IMPLEMENTATION**. **Hot Step (뜨거운 걸음)** also remains CANDIDATE / DEFERRED / NOT AUTHORIZED FOR IMPLEMENTATION. Preserve the historical idea record without promoting its proposed effects.

### Q4 — Tomb Guardian rewards

The Tomb Guardian's **core signature drop is Ancient Heart**, which also retains its existing Ancient City acquisition source. **Do not add a guardian-exclusive new crafting material or resurrect Tomb Guardian Core**. As a boss, the guardian should also provide **a varied additional reward pool of rare minerals and existing higher-tier crafting materials**. Specific items, drop quantities, rates and guaranteed-versus-random allocation beyond the approved core reward are **NOT YET APPROVED** and require balance review against the latest pyramid design and progression tiers. No exclusive new equipment/artifact is authorized by this decision.

### Follow-up integration notes

After PR #181 reaches main, place short links to this reconciliation in the live pyramid, 2026-10-22 bounty and related handoff documents without rewriting recovered historical copies. Validate that no conflict with the latest release text is introduced. Keep PR #182 Draft until its integration gates pass; do not mutate PR #181 for routine design changes.

## Remaining conflicts that need owner decisions

**Historical audit snapshot:** questions 1–4 below were resolved by the Q1–Q4 owner follow-up above; question 5 remains a release-verification concern. Do not treat the resolved questions as open.

1. **Commission board (2026-10-19) versus bounty contracts (2026-10-22).** The recovered commission design (a Commissions entry in the progression UI, a world-wide six-offer board of 2 kill / 2 delivery / 2 exploration rotating every three days, active limits 3 + 1, emerald-block payout bands) and the later `BOUNTY_CONTRACT_APPROVED_2026-10-22.md` (villager-sold and loot-found contract items, Beginner/Intermediate/Advanced tiers, emerald payouts) describe overlapping systems. The 2026-10-22 document does not say it supersedes the 2026-10-19 design. Decide whether the commission board is superseded, retained as a separate system, or merged.
2. **Ordinary contracts in the caravan.** F-5 forbids direct sale of *special* contracts only. The 2026-10-18 caravan merchandise group "bounty contracts" and register row F08 ("rare dungeon maps/contracts … stay caravan") leave open whether the caravan may sell **ordinary** contracts. The 2026-10-22 bounty authority lists villagers and structure containers as the sources.
3. **Feather Boots.** The recovered 2026-10-10 decision record replaces Feather Boots' Shift+Jump dash with double jump plus full fall-damage immunity. Current `ENCHANTMENT_BRANCH_SKELETON.md` still defines the dash, the Jet Boots mutual exclusion and the Elytra Launch input priority. The record itself says the reconciliation was not approved. The same record's "뜨거운 걸음" (Hot Step) enchantment is likewise unrouted in current authority.
4. **Ancient Heart uniqueness wording.** The pyramid document says the guardian has "exactly one unique crafting-material item type as its boss-exclusive signature reward". Under F-2 that item is Ancient Heart, which also drops in Ancient City chests. The owner decision explicitly keeps both sources. Confirm that no guardian-*exclusive* material is still wanted, so that later pyramid edits do not reintroduce one.
5. **Pyramid document changes after the release freeze.** See the release-verification note in PR #189. PR #181 advanced from `a301261` to `268ec65` through three pyramid-documentation commits. Confirm that design commits on the release branch are intended.

## Verification for this overlay

Checked against these source blobs: PR #181 HEAD `268ec65b` for the pyramid, XP Quest handoff, bounty and structure-candidate documents; `main` `cde842dd` for the Oct 7 core, mining-material, Adamantium worldgen and recipe data; and recovered copies per the manifest. No approved rule outside the six decisions above was changed.

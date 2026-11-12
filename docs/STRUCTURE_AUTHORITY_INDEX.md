# Structure authority index (2026-10-26)

Status: **ROUTING INDEX ONLY.** This file approves no structure, dimension, mob, loot or generation setting. It points to where each structure decision is recorded and states that record's status. Statuses are copied from the source records. A route to a candidate or deferred record does not promote it.

Several structure records currently live only on the Wave 9 release branch (Draft PR #181) or on active design branches. Those records are pinned below to an exact commit and blob instead of being copied into this branch, so that no competing, older copy is created. The latest pyramid document in particular must be taken from PR #181, never from PR #147's older copy (blob `b9cd5b9d…`, 26 lines).

Pinned release-branch commit: `268ec65bb8827c3f627d076188c7924225b8afa0` (`release/wave9-rc-20261025`, PR #181 HEAD at 2026-10-26).

On the Wave 10 integration branch (PR #182) the three pinned records are also present as byte-identical copies of those blobs: [PYRAMID_DESIGN_PREP_2026-10-25.md](PYRAMID_DESIGN_PREP_2026-10-25.md) (`52cc8157`), [PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md](PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md) (`904d16c4`) and [STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md](STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md) (`dafe1d5d`). A later edit on either branch makes the blobs differ; the newest owner-approved version then wins, and the pins above stay as provenance.

## Structure portfolio and cross-cutting rules

| Record | Location | Blob | Status |
|---|---|---|---|
| Structure candidate register (ST-01…ST-10) | [`docs/STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md` @ 268ec65](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/268ec65bb8827c3f627d076188c7924225b8afa0/docs/STRUCTURE_CANDIDATE_AUTHORITY_2026-10-25.md) (identical on PR #147 `55df673a`) | `dafe1d5dd31926779bb554ca9bfdc550eeec43b5` | Design inventory; not an implementation authorization. Most entries are concept only. |
| Phase 2 owner decisions for structures (F-2, F-6) | [PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md](preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md) | this branch | Current overlay. |
| Territory shelving | [TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md](preservation/recovered/territory-chain/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md) | `663c55ef…` | Territory buildings and facilities are SHELVED. Village and settlement ideas are CANDIDATE tracks only. |

## Individual structures

| ID / name | Authority route | Status |
|---|---|---|
| **ST-01 Colossal Pyramid** — design | [`docs/PYRAMID_DESIGN_PREP_2026-10-25.md` @ 268ec65](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/268ec65bb8827c3f627d076188c7924225b8afa0/docs/PYRAMID_DESIGN_PREP_2026-10-25.md), blob `52cc81573ee34a65b9201fd86125bd4c2a6a0772` (266 lines; latest version) | Mixed. The document separates its OWNER APPROVED items (Tomb Guardian boss identity, boss attacks, Earth Mace shaped recipe, kill-drop) from PROPOSAL and OPEN items. **F-2 overlay:** read every "Tomb Guardian Core" as **Ancient Heart**. Implementation is not authorized. |
| ST-01 Colossal Pyramid — mob roster | `docs/PYRAMID_MOB_ROSTER_DESIGN_2026-10-26.md` on branch `design/pyramid-mob-roster-20261026` @ `a86c6d5beb336eb14c0459200434b0f6304398c5` | The source states "OWNER-APPROVED DESIGN, implementation DEFERRED". This is an active design branch not yet reviewed into the pyramid authority; its per-room counts are OPEN. |
| ST-02 Sun Temple | candidate register; cancellation on branch `design/spider-colony-biome-20261026` @ `c129a5c9d8fd60d104f2ed969ae77af9b4ae5d41` | The register says concept only. The 2026-10-26 branch records an owner **cancellation** (ID retained). It is unmerged; treat ST-02 as not active. |
| **ST-03 Pillager Headquarters** | [`docs/PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md` @ 268ec65](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/268ec65bb8827c3f627d076188c7924225b8afa0/docs/PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md), blob `904d16c426e3cf3cd6e048c86130f51692987961` (identical on PR #147 `55df673a`) | Detailed source of truth. Its latest approved-decision index is at the top; item pools are deferred. Not implemented. |
| ST-04 Spider Colony Cave | `docs/SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md` on branch `design/spider-colony-biome-20261026` @ `c129a5c9d8fd60d104f2ed969ae77af9b4ae5d41` | The source states "OWNER APPROVED concept, DESIGN ONLY". This is an active design branch; generation rules remain open. |
| **ST-05 Ancient Colosseum** | [Current Q1–Q118 authority](ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md); [original decision history](ST05_ANCIENT_COLOSSEUM_DECISION_HISTORY_2026-10-29.md); candidate register row ST-05 | **OWNER-APPROVED DESIGN ONLY.** [ST-05 WG-Q01–Q45 worldgen/architecture overlay](design/ST05_WORLDGEN_OWNER_DECISIONS_Q01_Q18_2026-10-30.md) supersedes conflicting *structure and worldgen* clauses in the older Q1–Q118 authority (including stands and the huge-cave requirement); it does **not** supersede combat, trial lifecycle, persistence or rewards. [Preimplementation audit](design/ST05_WORLDGEN_PREIMPLEMENTATION_AUDIT_2026-10-30.md) records unresolved 75% probability semantics, cavern identity and cross-chunk determinism gates; implementation remains on hold. Q1–Q118 current rules include the provisional Q109 15-member roster, eight inactive conditional candidates, Q110's 60-active-second blocked-spawn failure, the R3 role classification, Q111–Q115 persistence and reward-record rules, Q116's 65-minute trial time limit, Q117's boss vanishing on time-out, and Q118-A's never-overwrite rule for boss-destroyed spots. Neither implementation nor live balance/acceptance is approved. Separate from the Ancient City arena (F-6). |
| ST-06 … ST-10 (End remakes and End structures) | candidate register | Concept only. |
| **Ancient City arena** (inside the Ancient City) | [ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md](preservation/recovered/content-first-axis-review/ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md) §2-A, §4 | User-approved design record. The first prototype is Draft PR #83 (`codex/ancient-city-arena-prototype-20261013`). **Not ST-05 (F-6).** |
| Ancient City whole-city variants (death/ruin, Nether-eroded, golden age) | same record, §2-B | Adopted concepts; details unconfirmed; excluded from the arena prototype. |
| Ancient City remodeling in `MoreMinecrafts-Structure-Generator` | candidate register, "Related but not counted" | Separate repository work. Do not infer that it implements the arena or ST-05. |
| Underground kingdom worldgen probe | Draft PR #117 (`experiment/underground-kingdom-worldgen-probe`) | Experiment evidence only. |

## ST-05 current lifecycle override (2026-11-02)

**CURRENT OVERRIDE:** [ST-05 Stage 1 lifecycle authority reconciliation](st05/ST05_STAGE1_LIFECYCLE_AUTHORITY_RECONCILIATION_2026-11-02.md) routes the later owner-approved 180-second grace, 65-minute wall-clock attempt cap, verified-outage exception, 78,000-tick online cooldown, and per-round admission. The older ST-05 row above is historical for conflicting clauses; its 'design only / implementation on hold' status is also superseded by the reported implementation on Draft PR #219 (live-client acceptance pending). This index update does not grant merge or checkpoint approval.

## Acquisition constraint carried by structure work

The Ancient City remains an **Ancient Heart** source, as recorded in [CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md](CONTENT_FINALIZATION_IMPLEMENTATION_2026-10-07.md). Neither the Ancient City expansion nor the pyramid may remove that route (F-2).

## ST-01 royal reward acquisition overlay — D13-03 (2026-11-06)

For **ST-01 Colossal Pyramid** royal reward acquisition, see [D13-03-Q25–Q41 and Q42–Q47](planning/D13_03_DECISIONS_Q01-Q47.md): exactly two designated royal reward chests, independent 20% accessory-pool rolls per chest, and staged preimplementation/integration verification. This supplements the previously OPEN chest count only; it does **not** replace the [pyramid design](PYRAMID_DESIGN_PREP_2026-10-25.md), the main royal chest's encounter/unlock gate, or existing loot/catalyst rules. The second chest's precise location, role and unlock conditions and the approved base royal loot table remain unconfirmed here. Design registration does not imply structure implementation, merge, or live validation.


## ST-01 royal chest I06 Q01–Q45 authority overlay (2026-11-07)

The current owner-approved two-royal-chest reward and access contract is [D13-03-I06 Royal Chest Reward Authority](planning/D13_03_I06_ROYAL_CHEST_REWARDS_AUTHORITY_2026-11-07.md). It resolves the earlier D13-03 Q25–Q47 row's unspecified royal base loot, two-chest access and inventory conditions **for I06 only**: two 54-slot chests, first direct player open after Tomb Guardian defeat, permanent no-insertion/no-automation/no-destruction, independent ordinary and accessory rewards, and one structure-wide catalyst assignment. Q45 specifies titanium/tungsten ingots. ST-01 P4 **B** is the selected design candidate; orientation and final in-world placement remain separate/unverified. The third secret treasury chest is excluded. This is **OWNER-APPROVED DESIGN ONLY** and does not claim implementation, merge, or live validation.

## ST-01 single primary royal chest override (2026-11-12)

**CURRENT OVERRIDE:** ST-01 Colossal Pyramid operates under a single `PRIMARY` royal reward chest system (a single double chest in the royal hall). The unplaced `SECONDARY` royal chest is discontinued.
1. **Single Chest Architecture:** Only the `PRIMARY` double chest (54 slots) is placed and registered in ST-01. The unplaced `SECONDARY` chest is removed from runtime requirements and active reward distribution.
2. **5% Relic Catalyst Unification:** The 5% structure-level Relic Catalyst roll (`CATALYST_CHANCE = 0.05D`) is fully unified onto the single `PRIMARY` chest (100% of successful catalyst rolls are assigned to `PRIMARY`; the superseded 50:50 split with `SECONDARY` is eliminated, preventing unallocated/missing catalysts).
3. **20% T2 Accessory Unification:** The single `PRIMARY` chest conducts the 20% T2 accessory roll (Seed of Life / Ring of Time 50:50).
4. **Backward Compatibility:** Existing world saves (`SavedData`) retaining legacy `SECONDARY` positions or `catalyst: "SECONDARY"` entitlements continue to decode without migration errors; uncollected catalyst entitlements decode safely and are honored at the primary chest or preserved.


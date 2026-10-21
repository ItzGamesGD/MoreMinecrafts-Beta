# XP Quest owner decisions — 2026-10-21 (content review checkpoint)

**Authority:** explicit owner decisions in the XP Quest 200-entry design review (2026-10-21). This is a **design-decision log**, not evidence that gameplay code, catalog JSON, localization, tests or live play were changed. Q1-C snapshot remains pinned to `17e980912d636b0572a3f25b24995ffde31c754c`. Do not retroactively treat the catalog A proposals as owner approvals.

**Scope:** modify content in the future implementation branch after full 200-quest design review; preserve all 200 quest slots where possible. All raw-XP rewards are **provisional, unchanged** pending a separate cross-quest economy pass. All status/registration and test gates remain as currently implemented unless explicitly stated otherwise. Do not merge any PR or mark Ready based on this document.

## A. Combat ecology — 3 reviewed quests

- `xpq.cmb.cave_dwellers` (MID, 500 XP): target remains six distinct kills, one of each: `cave_zombie`, `cave_skeleton`, `cave_creeper`, `small_spider`, `cave_bat`, **`cave_bug`**. Replace the non-cave `swamp_spider`. These are the six cave-spawn roster variants. Preserve reliable server-side kill attribution; live verification still required.
- `xpq.cmb.bestiary_25` (LATE, 1,350 XP): keep 25 **distinct** MoreMinecrafts monster types killed; a long-running collection objective. Verify that 25 eligible types naturally spawn and their kills actually count. `cave_bat` is AMBIENT, so do not silently assume it is part of a monster-only count.
- `xpq.cmb.habitats_5` (LATE, 1,100 XP): **actual habitat exploration plus combat**, not simply checking which mob species was killed anywhere. Five progress groups, each requires a kill **in the designated biome**: (1) desert/desert spider or sand slime; (2) swamp or mangrove swamp/swamp zombie, swamp spider or mud slime; (3) snowy habitat/frozen zombie or ice spider; (4) fungal habitat **restricted to mushroom fields or lush caves**/fungal zombie (a fungal zombie killed in a swamp does not count); (5) forest habitat/wild slime (a wild slime killed in a swamp does not count). Confirm precise valid biome tags/IDs at implementation and test every negative case. Each completed group can be credited once. Current `stat_killed_groups_each` tracker is insufficient without kill-position biome evidence and likely needs a new evidence path. No retroactive credit from species-only kill statistics.

All three are reviewed content candidates for NOW/registered, but **design approval does not equal technical/live acceptance**; reconcile stale ecology documentation with current JSON after implementation review.

## B. Exploration — 24 existing quest slots reviewed

### Replace all six distance-count quests with actual exploration objectives

Keep existing IDs only as stable migration keys if safe; player-facing names/objectives become:

| Current ID | New Korean title | Tier | Provisional XP | Required objective |
|---|---|---|---:|---|
| `xpq.exp.walk_10k` | 지하 탐험가 | EARLY | 250 | Enter both a naturally generated lush cave and dripstone cave underground. |
| `xpq.exp.walk_50k` | 지하 세계의 비밀 | LATE | 1,150 | Enter Deep Dark biome, Ancient City interior and naturally generated Mineshaft interior. |
| `xpq.exp.sail_20k` | 일곱 바다의 탐험가 | LATE | 1,150 | Visit a warm-ocean coral reef and a frozen-ocean iceberg area; explore a deep-ocean underwater site and underwater ruins. Exact feature predicates still require implementation specification. |
| `xpq.exp.ride_10k` | 육로의 개척자 | LATE | 1,000 | While riding a land mount (e.g. horse/camel), enter desert, badlands, savanna and snowy biome groups. |
| `xpq.exp.elytra_25k` | 하늘에서 본 세계 | LATE | 1,100 | While actually gliding with elytra above mountain peaks, jungle, cherry grove and badlands, satisfy each region's height/flight predicate. Freeze safe height thresholds at implementation. |
| `xpq.exp.travel_250k` | 세계의 끝을 향하여 | ENDGAME | 3,150 | Enter the interiors of Ancient City, Woodland Mansion, Nether Fortress, Bastion Remnant and End City. |

Distance alone must never complete these objectives. Confirm natural-world predicates and any vanilla ID/structure validity. New trackers, server-side observations and negative tests are necessary. Existing distance stats must not be imported as evidence for the replacement objectives. Review overlap with `structures_15`, `grand_expedition` and other exploration goals; **overlap is allowed**, but objectives should remain distinct.

### Structure entry — approved attribution and overlap

- `xpq.exp.villages_5`: player-facing title **다섯 문화의 마을**, with a separate required visit to each of five **village types**: plains, desert, savanna, snowy, taiga; not five arbitrary village instances. Existing tier MID, 550 XP provisional.
- `xpq.exp.structures_15`, `xpq.exp.grand_expedition`, `xpq.exp.villages_5`: credit **actual entry inside generated structure pieces**, not proximity. Old Knowledge proximity observations cannot be retroactively interpreted as entry and must not award progress. Correct the stale catalog A `R` claim and player copy, without rewriting old save evidence.
- **Shared progress:** one valid structure entry may advance multiple quests in parallel; players must not re-enter the same place solely because a different quest tracks it. Each quest grants its reward only upon its own completion. Valid forward-time structure-entry records can be shared across quest evaluations.

### Biome milestones — approved

- `xpq.exp.biomes_25` (MID 500 XP) and `xpq.exp.biomes_40` (LATE 1,150 XP): retain unique biome discovery milestones and share observed biome evidence.
- `xpq.exp.rare_biomes`: player-facing name **희귀 경관**; **all five** of `mushroom_fields`, `ice_spikes`, `eroded_badlands`, `bamboo_jungle`, `cherry_grove` required; change tier **MID -> LATE**. 600 XP provisional. Concurrent milestone progress allowed.
- `xpq.exp.biomes_all_overworld`: player-facing name **완전한 지도**; complete the frozen, versioned roster of **all naturally generated survival-accessible biomes across Overworld, Nether and End**, excluding `the_void` and non-survival biomes. Show denominator, discovered count and missing list. Trustworthy persisted Knowledge visit evidence may count retroactively (unlike structure proximity evidence). Keep **VERIFY/unregistered** until version-specific exact roster, evaluator and live availability are validated. Existing `overworld` identifier conflicts with three-dimension objective; audit references/migration before renaming stored ID.

### Other exploration content — disposition

- **Retain current objectives** (existing tiers/rewards provisional): `first_structure_loot`, `mineshaft_loot`, `dungeon_loot`, `temples`, `ocean_relics`, `height_extremes`, `archaeology_30`, `sherds_15`.
- `xpq.exp.sleeping_bag_biomes`, `xpq.exp.parachute_landing`: retain proposed objective but keep **VERIFY/unregistered** until feature/event detection and survival attainability are verified.
- `xpq.exp.explorer_maps`: rename **지도 제작자의 발자취**, promote **MID -> LATE**, keep original **500 XP provisional**. Collect each of three explorer maps (ocean, woodland, trial) and **enter the exact corresponding destination structure targeted by that map**, in order (map acquisition -> matching actual visit). A preexisting visit without obtaining the matching map cannot satisfy this map-specific quest. Record each map's actual target identity and server-side entry; don't grant completion simply for visiting any monument/mansion/trial chamber. Keep **NOW/unregistered**, as current tracker is pending and reliable evidence implementation/tests are required.

## C. Shared rules, rollout constraints, and remaining work

1. **Quest overlap is intentional:** the same credible discovery or structure-entry event can advance multiple quests; each quest tracks its distinct own completion and payout.
2. **No fake retroactivity:** old structure proximity and mere species kill stats cannot establish structure entry or biome-qualified kill. Previously persisted *credible* biome discovery can be credited where reliable and appropriate.
3. No XP economy retune yet: retain stated raw XP amounts temporarily, even after tier promotions and replaced objectives. The prior aggregate 200-row totals will be revisited as a separate explicit owner decision, not silently changed.
4. All 24 exploration slots have undergone **first-pass design review**, but detailed predicates, event availability, migrations, negative tests and live acceptance remain open; 200-entry review is not complete.
5. **Implementation is not authorized by this document alone.** No registered/status flips, runtime copy changes or catalog mutation happened in this PR. The Q1-C/catalog and separate code PR branches remain unchanged.
6. Next review queue: the remaining **combat tab** entries excluding the three ecology quests already decided; inspect high-count kill ladders, mob types and distinctive fights against real code and pacing.

## Source of truth distinction

This file records **owner-approved design intent from conversation**, not an implementation claim. The existing `quest_inventory.csv` retains pinned Q1-C data for audit reproducibility; do **not** rewrite its snapshot rows to show future approved designs as though they were implemented.


## D. Combat follow-up — weapon quest removals and block target (2026-10-21)

Owner explicitly **rejects and orders removal** of the two repetitive, weapon-specific 100-kill quest slots:
- `xpq.cmb.dagger_100` / 그림자 칼날 — **DELETE** (not defer).
- `xpq.cmb.greatsword_100` / 거인의 일격 — **DELETE** (not defer).

Rationale: Dagger/greatsword progression roles and distinctive gameplay are not approved; forcing 100 kills with inventory-registered weapons is not meaningful content. Do not replace these entries with invented content merely to preserve the 200-entry count. Before implementation, reconcile roster cardinality and the approved XP ledger via an explicit decision; do not silently redistribute the 1,000 XP combined awards. `xpq.cmb.mm_arms` (one kill with each weapon) is a separate quest and **has not been explicitly removed** in this decision; flag it for later re-review.

Also confirmed by owner: `xpq.cmb.shield_500` / 철벽 goal **500 -> 100 successful shield blocks**, pending implementation and reward review. Ordinary hostile-kill ladder 100/500/2,000/5,000 remains approved as-is, including farmable cumulative progression. This is documentation-only owner intent, not a catalog edit or a registered-quest migration.

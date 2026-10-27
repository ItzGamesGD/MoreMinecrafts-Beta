# Integration gaps and resolutions — Wave 12 (2026-10-27)

Status: audit record. Nothing here creates or changes design authority except the verbatim restoration of an already owner-approved amendment (G-2), which is a synchronization required by `AGENTS.md` ("Design finalization and authority synchronization").

Baseline `checkpoint/wave11-unified-20261027` @ `c47c69f3`; integration branch `integration/wave12-functional-preservation-20261027`; final checkpoint `checkpoint/wave12-unified-20261027`.

## How gaps were found

1. Semantic set-difference of every remote branch against Wave 11 (classes, tests, mixins, entrypoints, resources, assets, lang keys, research IDs, quest registrations, workflows): 168 branch-only items (27 classes, 94 test methods, 7 mixins, 14 data resources, 8 assets, 10 quest registrations, 4 entrypoints, 4 workflows).
2. Per-file branch-only line analysis on all 83 branches that have commits outside Wave 11.
3. Every candidate traced to source history, the PR body, and the authority routes. Divergent history was not treated as missing functionality, and an empty diff was not treated as proof of equivalence: each research ID, quest registration and mixin was checked for a live runtime caller in Wave 11.

Of the 168 branch-only items, 121 trace to superseded, rejected or tooling-only work (evidence in [REPOSITORY_FUNCTIONAL_PROVENANCE.md](REPOSITORY_FUNCTIONAL_PROVENANCE.md)), 13 belong to the Ancient City Arena (G-1, recovered) and 34 to the wither-skull provenance work (U-1, blocked). Line-level and authority analysis found two further gaps with no code footprint (G-2, G-3). Auditing the 15 PR heads that survive only as `refs/pull/N/head` found one more: missing regression coverage (G-4).

## Resolved gaps

### G-1 — Ancient City Arena prototype missing from the integration line — RECOVERED

- **Source**: PR #83 (`codex/ancient-city-arena-prototype-20261013` @ `e7dc53f9`, open draft), based on Stage 20.
- **Authority**: [ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md](../preservation/recovered/content-first-axis-review/ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md) §2-A and §4 approve "the first prototype: the Ancient City arena only", with random (not forced) generation through the vanilla Ancient City structure system and a documented provisional weight. STRUCTURE_AUTHORITY_INDEX routes the arena to PR #83; F-6 keeps it separate from ST-05.
- **Why missing**: Batch A (#156) and every later wave excluded "arena/structures" as out of their scope; no branch rejected it.
- **Method**: cherry-pick with `-x` of all five commits (smallest coherent set: template, pool contribution, mixin, tests, validation tooling, evidence). Three registration-list conflicts resolved by keeping every Wave 11 entry and appending the arena entries (`MoreMinecraftsClient` harness registration, `StructureTemplatePoolMixin` in the mixin list, `AncientCityArenaGameTests` in the **server** `fabric-gametest` list, where the textual merge had initially placed it in the client list). No Wave 11 code was replaced.
- **Commits**: `6bb483e1`, `64ffec31`, `d8a8f386`, `ef3e7054`, `209c251e` (cherry-picks of `5d4e5196`, `04c2986a`, `41c429e8`, `64bfacec`, `e7dc53f9`), `0333a5d2` (adds the Wave 12 refs to the arena workflow's push filter; no step changed).
- **Gameplay values unchanged**: production weight 1 against 46 vanilla weight slots in `minecraft:ancient_city/structures`, `minecraft:ancient_city_generic_degradation` processor, rigid projection, the same 37×13×37 template (blob `4620400d`).
- **Verification**:
  - AUTOMATED: `AncientCityArenaGameTests` (2) pass inside the full 425-test server GameTest run; selection log `ARENA_SELECTION_VERIFIED seeds=4096 configured_weight=1 total_weight=47 random_arena=88 shuffled_first_arena=106 vanilla=3399` (identical to PR #83's evidence).
  - Second path (independent of the author's GameTests): local dedicated server with normal terrain generation, seed 2632027. It reproduced PR #83's result exactly: arena at (7824,−51,−5616)…(7860,−39,−5580), city chunk (485,−348), 72 vanilla pieces, 5/5 voxel probes on arena blocks, plus an ordinary Ancient City without the arena at chunk (194,−423). Two native 1280×720 client captures (Xvfb + Mesa llvmpipe Vulkan) were taken and inspected; they show the ring, seating and sculk inside the city. Evidence: [wave12-20261027/arena-local/](wave12-20261027/arena-local/).
  - Template: `validate_ancient_city_arena.py` PASS (3,004 non-air blocks, one jigsaw). `generate_ancient_city_arena.py --check` regenerates byte-identical NBT content; the only difference locally is the gzip header OS byte (Python 3.13 writes `0xff`, the committed file has `0x03`). This is environment-specific; CI's Python 3.12 runner is the authoritative check.
- **Remaining**: owner visual/feel acceptance — LIVE VALIDATION PENDING. The prototype weight remains provisional, as the authority allows.

### G-2 — Owner-approved XP Control (`v3.research.67`) amendment missing from current authority — RESTORED

- **Source**: "RESEARCH 67 — INSTANT XP ORB COLLECTION CLARIFICATION — 2026-10-15", approved in commit `35acb625` (PR #92 branch) and reconciled in `e2acd57f` (PR #93 branch). Both commits are authored by the repository owner.
- **Gap**: Wave 11's `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md` keeps only the older one-line "XP pickup radius 4 / 6 blocks". That wording does not distinguish remote instant collection from increased orb attraction, and it omits the once-per-orb and `.64` composition rules. The implementation already follows the approved semantics (`ResearchExperienceOrbMixin` sorts eligible players by distance and calls vanilla `ExperienceOrb.playerTouch` once). Only the authority record was lost.
- **Resolution**: both approved texts appended verbatim with provenance (`0d035c84`). No value or rule changed; nothing was promoted.

### G-3 — New design record existing only on an unmerged branch — PRESERVED

- `docs/sky-islands-design-context-20261027` @ `8ceff945` (created 2026-10-27 00:26 UTC, no PR) holds the only copy of `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md`. It was copied byte-identically (blob `a91e385d`) in `0d035c84`. The document's own header marks it as design in progress and not an implementation instruction, so it is **not** routed as authority.

### G-4 — Lower-equipment regression coverage from closed PR #65 — RESTORED

- **Source**: PR #65 (`9ba8f422`, closed, branch deleted; reachable only via `refs/pull/65/head`). It fixed Mining Drill ghost-slot item loss, fuel-merge persistence and detached-session fuel duplication; Portable Furnace excess transfer and 26.3 fuel-provider resolution; Greatsword entity right-click sweep ordering; and Potion Injector filtering.
- **Finding**: the fixes are present in Wave 11 in evolved form, carried by Stage 20 / PR #69 `02fd7545`: `FilteredChestMenu`, the drill's `ownsSource` guard, `GreatswordPlayerInteractionMixin` → `GreatswordItem.useSweep`, and `PortableFurnaceMenu.vanillaFuelTicks`. However, 16 of #65's 17 regression GameTests were not carried over, so most of these defects had no regression protection.
- **Resolution** (`7ef97c4c`): the 15 still-applicable tests are ported unchanged into `LowerEquipmentRegressionGameTests`, plus one adapted lantern-socket equip/clear test without the superseded position assertion. Not ported, because current authority supersedes them: furnace close-and-drop (2026-10-16 persistence amendment) and the socket "left of helmet" position (2026-10-23 accessory inventory layout). All 16 pass against Wave 11's evolved code. This independently confirms that #65's fixed behaviors survived the refactors.

### PR heads whose branches were deleted

15 PR heads are reachable only through `refs/pull/N/head`: #34, #39, #65, #70, #73, #74, #78, #81, #85, #139, #154, #157, #158, #159, #163. They were audited with the same semantic inventory. Apart from G-4, their unique content is old-lineage code that the Stage 2/3 reconciliations supersede, documents preserved in `docs/preservation`, or a probe marked "WRONG REPOSITORY — DO NOT MERGE" (#85). Details are in the provenance record.

## Unresolved — owner decision required

### U-1 — Wither-skull provenance quest (`xpq.cmb.wither_skulls_3`)

- **Sources**: #127 → #128 (`claude/q1c-skull-provenance-20261019`) → #130 (`codex/q1c-pr128-p1-p2-repair-20261019`): `QuestWitherSkullProvenance`, four item-entity mixins (death loot, merge, pickup, spawn), 17 GameTests and 3 JUnit tests, and catalog registration.
- **Authority**: Phase 2 F-1 approves the rule (first pickup of a genuinely dropped skull; target 3; 1,100 XP), and says: "PRs #128/#130 are not accepted as release-ready; the reported crash-consistency defect must be corrected and tested before registration." The authority index adds: "stays unregistered".
- **State**: #130's own opt-in two-JVM acceptance test (player-only save, then `Runtime.halt`) still credits two players for one skull (A=1, B=1). Fixing it needs a durable receipt/journal protocol that #130 explicitly lists as unimplemented and awaiting design review.
- **Why not integrated**: the quest must stay unregistered. Shipping the tracking mixins on the core item-pickup path with a known P1 defect and no design for the fix would add risk without enabling any approved behavior, and choosing the receipt protocol is a design decision. Wave 12 keeps the catalog entry exactly as approved (unregistered, target 3, 1,100 XP). Its implementation and tests remain on the PR #128/#130 refs and are also preserved in `docs/audit/BRANCH-W11-20261027/preserved/`.
- **Decision needed**: approve a receipt/durability protocol (or accept a documented weaker guarantee). A repair task can then integrate #130 behind the existing registration gate.

### U-2 — Adamantium ore generation differs from approved F-3 values

- **Authority**: F-3 (2026-10-26): Nether Adamantium at Y 8–22, biased to Y 15, veins of 1–2, and fewer generated blocks than Ancient Debris in matched sampling. "Current Y 8–24/size 3 implementation is not authoritative."
- **Checkpoint**: `worldgen/placed_feature/ore_adamantium.json` uses count 1, uniform Y 8–24; `worldgen/feature/ore_adamantium.json` uses size 3.
- **Why not changed here**: no branch contains an implementation of F-3, so this is new implementation work rather than recovery. It also needs a choice of count/rarity and height distribution parameters that satisfy the comparative constraint, validated by a matched-sampling harness. Several parameter sets satisfy F-3, with materially different abundance, so per `AGENTS.md` STOP & ASK the parameters are not chosen in this integration task.

## Approved designs never implemented on any branch (not integration gaps)

These are listed so they are not mistaken for lost work. No branch or PR contains an implementation:

- Adamantium equipment, templates and refining (approved v1 design, 2026-10-18).
- Rare caravan, runes and caravan contract sales (Q2/F-5).
- Pillager patrol world event (design finalized 2026-10-18).
- Earth Mace shaped recipe with Ancient Heart (F-2). STRUCTURE_AUTHORITY_INDEX says pyramid implementation "is not authorized", and the index states the recipe "is not implemented".
- Pyramid, Pillager HQ, Spider Colony, ST-05…ST-10 (design records; implementation not authorized).

## Excluded with evidence (summary)

| Contribution | Reason |
|---|---|
| Territory chain (#131–#138) | SHELVED by owner; docs preserved and historical only |
| 2026-10-19 commission board | Q1: historical only, must not be implemented |
| Feather Boots redesign / Hot Step | Q3: candidate/deferred; existing enchantment kept |
| #119 chunk-generator ore mixin and nine #119 registrations | Replaced by #120 positive provenance and test-gated registration |
| Old-lineage F/Shift+F, Wind Step, +30/+40 Enhancement, Tier IV–V fishing, independent length/weight rolls | Superseded by Stage 2 / Stage 3 reconciliations |
| Furnace "latest open wins" (#96) | Superseded by the Batch 3 attribution amendment |
| Villager discount round-to-nearest (#110 test) | Superseded by the 2026-10-26 ceil rounding |
| Wild Piglin vanilla zombification (main test) | Superseded by the 2026-10-08 identity rule |
| Global vanilla sweep-texture override prototypes | T2 VFX deferred; excluded from production by the PR #154 plan |
| Free-model delegation (#141), experimental skills | Not product; excluded from production |
| Stage 23-B `clientQa` harness (#107) | Validation tooling with its own source set; not product functionality |
| Underground kingdom probe (#117) | Experiment evidence only (STRUCTURE_AUTHORITY_INDEX) |
| Parallel exposure-audit downloads (#184) | Superseded by paced audit #186 |

## Pre-existing findings outside integration scope (not changed)

- `ko_kr.json` lacks 81 keys present in `en_us.json` (mostly ore/material blocks). No branch had them, so this is not a regression.
- `relic_catalyst` is a registered item without an item model definition; its art is explicitly DEFERRED by the accessory catalyst authority.
- The XP Quest documents disagree on the NOW count: the 2026-10-17 handoff says 160, while the catalog and later audits say 163. Totals (180,000 active / 209,100) and the 75 registrations are consistent; the reclassification is already recorded in `CHAPTER_DESIGN.md` and the 2026-10-20 checkpoint.
- Large Backpack icon visual defect: carried forward from earlier audits; no fix exists in history.

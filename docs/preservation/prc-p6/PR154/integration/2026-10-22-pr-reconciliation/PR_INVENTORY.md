# Repository-wide PR inventory — 2026-10-22

Repository: [ItzGamesGD/MoreMinecrafts-Beta](https://github.com/ItzGamesGD/MoreMinecrafts-Beta). GitHub snapshot collected 11:52 UTC on 2026-10-22 and refreshed after #152/#153 advanced, approximately 12:10 UTC; final pre-publication head check is recorded in the audit PR. Baseline `main`: `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. All 65 open PRs are included. This audit PR itself is excluded from that count.

## Findings and counting rules

- **65 open PRs audited; 6 distinct production/runtime donor lines:** #83 arena prototype, #130 skull/provenance, #135 cumulative research/XP/UI, #151 contracts, #152 accessory art resources, #153 accessory equipment/enhancement. Five are active system/resource frontiers; the sixth is an isolated optional prototype. This counts independent retained runtime payloads, not every historical implementation PR. It does not mean any is ready to merge.
- **46 redundant or superseded PR envelopes:** 44 SUPERSEDE + 2 CLOSE-CANDIDATE. Their relevant runtime work is already represented or duplicated in selected descendants/copies. Unique historical documents and alternative tests may still exist; these recommendations do not authorize closure or deletion.
- Separate valuable validation donors: #93 event tests and #107 client QA harness. #72 contributes no additional production payload beyond the copy already in #135. #117/#141 and the un-PR'd Ponytail branch remain experiments. Territory is **SHELVED** under #138 and the owner's current instruction.
- All 65 heads/bases were fetched and compared with Git. REST paginated changed-file sets match `git diff --name-status <merge-base> <head>` for all 65. PR numbers/titles/bases were never used as ancestry proof. Exact graph, merge bases and preservation exceptions: [dependency graph](PR_DEPENDENCY_GRAPH.md).
- Fresh exact-head Actions/check-run metadata is linked below. Status API contexts are empty across the snapshot: its nominal `pending` state with zero contexts is not a failed or pending build. Actions/check runs carry the actual evidence. Earlier failed retries are not substituted for the latest run of the same workflow. Other workflow failures remain visible even when Fabric succeeds.
- There are **no submitted APPROVED reviews** across the 65 PRs; #72 has one bot COMMENTED review. Design approval recorded in authority documents is a separate question from GitHub review state. Most PRs are Draft; state is listed individually.

## Classification and evidence rules

Every entry has one primary class from the requested taxonomy; redundancy is a second classification expressed by SUPERSEDE/CLOSE-CANDIDATE. KEEP means preserve as a source/reference, INTEGRATE means retain the specified narrow content in a future approved batch, BLOCKED means unresolved acceptance/authority/engineering prevents promotion. Nothing was merged, closed, deleted, rebased or force-pushed.

**CODE VERIFIED** below means this audit verified committed source/tree/graph or fetched exact-head automation metadata. It does not mean this audit executed Java tests. **LIVE VERIFIED** requires named actual client/server behavior and exact provenance. No fresh gameplay was run in this audit. Historical automated rendered-client evidence is identified as such, not upgraded to physical mouse or owner acceptance. Full-history security-check failures are recorded as failures without copying potential secret findings.

| PR | Primary class | Disposition | Incremental paths | Review | Draft |
|---|---|---|---:|---|---|
| [#72](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/72) | Production implementation | **SUPERSEDE** | 94 | COMMENTED only | No |
| [#83](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/83) | Experiment | **KEEP** | 22 | None | Yes |
| [#88](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/88) | Bug fix | **SUPERSEDE** | 6 | None | No |
| [#89](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/89) | Production implementation | **SUPERSEDE** | 38 | None | Yes |
| [#90](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/90) | Production implementation | **SUPERSEDE** | 29 | None | Yes |
| [#91](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/91) | Production implementation | **SUPERSEDE** | 24 | None | Yes |
| [#92](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/92) | Production implementation | **SUPERSEDE** | 18 | None | Yes |
| [#93](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/93) | Integration or reconciliation | **INTEGRATE** | 22 | None | Yes |
| [#94](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/94) | Integration or reconciliation | **SUPERSEDE** | 16 | None | Yes |
| [#95](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/95) | Production implementation | **SUPERSEDE** | 16 | None | Yes |
| [#96](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/96) | Production implementation | **SUPERSEDE** | 15 | None | Yes |
| [#97](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/97) | Bug fix | **SUPERSEDE** | 2 | None | Yes |
| [#98](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/98) | Production implementation | **SUPERSEDE** | 24 | None | Yes |
| [#99](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/99) | Bug fix | **SUPERSEDE** | 10 | None | Yes |
| [#100](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/100) | Integration or reconciliation | **CLOSE-CANDIDATE** | 58 | None | Yes |
| [#102](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/102) | Documentation-only audit | **KEEP** | 1 | None | Yes |
| [#103](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/103) | Integration or reconciliation | **SUPERSEDE** | 116 | None | Yes |
| [#105](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/105) | Bug fix | **SUPERSEDE** | 12 | None | Yes |
| [#106](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/106) | Approved design or authority | **KEEP** | 1 | None | Yes |
| [#107](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/107) | Tests and validation | **INTEGRATE** | 6 | None | Yes |
| [#108](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/108) | Production implementation | **SUPERSEDE** | 22 | None | Yes |
| [#109](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/109) | Production implementation | **SUPERSEDE** | 18 | None | Yes |
| [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110) | Production implementation | **SUPERSEDE** | 24 | None | Yes |
| [#112](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/112) | Approved design or authority | **KEEP** | 17 | None | Yes |
| [#113](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/113) | Approved design or authority | **SUPERSEDE** | 1 | None | Yes |
| [#114](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/114) | Production implementation | **SUPERSEDE** | 11 | None | Yes |
| [#115](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/115) | Bug fix | **SUPERSEDE** | 15 | None | Yes |
| [#116](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/116) | Bug fix | **SUPERSEDE** | 6 | None | Yes |
| [#117](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/117) | Experiment | **KEEP** | 17 | None | Yes |
| [#118](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/118) | Integration or reconciliation | **SUPERSEDE** | 25 | None | Yes |
| [#119](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/119) | Production implementation | **SUPERSEDE** | 24 | None | Yes |
| [#120](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/120) | Bug fix | **SUPERSEDE** | 15 | None | Yes |
| [#121](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/121) | Production implementation | **SUPERSEDE** | 62 | None | Yes |
| [#122](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/122) | Approved design or authority | **KEEP** | 3 | None | Yes |
| [#123](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/123) | Integration or reconciliation | **CLOSE-CANDIDATE** | 4 | None | Yes |
| [#124](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/124) | Integration or reconciliation | **SUPERSEDE** | 4 | None | Yes |
| [#125](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/125) | Tests and validation | **SUPERSEDE** | 2 | None | Yes |
| [#126](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/126) | Production implementation | **SUPERSEDE** | 13 | None | Yes |
| [#127](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/127) | Approved design or authority | **SUPERSEDE** | 1 | None | Yes |
| [#128](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/128) | Production implementation | **SUPERSEDE** | 17 | None | Yes |
| [#129](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/129) | Unapproved proposal | **KEEP** | 20 | None | Yes |
| [#130](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/130) | Bug fix | **BLOCKED** | 3 | None | Yes |
| [#131](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/131) | Approved design or authority | **SUPERSEDE** | 29 | None | Yes |
| [#132](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/132) | Approved design or authority | **SUPERSEDE** | 6 | None | Yes |
| [#133](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/133) | Approved design or authority | **SUPERSEDE** | 1 | None | Yes |
| [#134](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/134) | Approved design or authority | **SUPERSEDE** | 8 | None | Yes |
| [#135](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/135) | Production implementation | **BLOCKED** | 18 | None | Yes |
| [#136](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/136) | Approved design or authority | **SUPERSEDE** | 8 | None | Yes |
| [#137](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/137) | Approved design or authority | **SUPERSEDE** | 2 | None | Yes |
| [#138](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/138) | Approved design or authority | **INTEGRATE** | 3 | None | Yes |
| [#139](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/139) | Documentation-only audit | **KEEP** | 1 | None | Yes |
| [#140](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/140) | Approved design or authority | **INTEGRATE** | 8 | None | Yes |
| [#141](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/141) | Experiment | **BLOCKED** | 5 | None | Yes |
| [#142](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/142) | Production implementation | **SUPERSEDE** | 8 | None | Yes |
| [#143](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/143) | Production implementation | **SUPERSEDE** | 16 | None | Yes |
| [#144](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/144) | Tests and validation | **SUPERSEDE** | 4 | None | Yes |
| [#145](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/145) | Tests and validation | **SUPERSEDE** | 6 | None | Yes |
| [#146](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/146) | Tests and validation | **SUPERSEDE** | 5 | None | Yes |
| [#147](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/147) | Approved design or authority | **KEEP** | 1 | None | Yes |
| [#148](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/148) | Approved design or authority | **SUPERSEDE** | 2 | None | Yes |
| [#149](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/149) | Tests and validation | **SUPERSEDE** | 4 | None | Yes |
| [#150](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/150) | Tests and validation | **SUPERSEDE** | 11 | None | Yes |
| [#151](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/151) | Production implementation | **BLOCKED** | 62 | None | Yes |
| [#152](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/152) | Production implementation | **INTEGRATE** | 41 | None | Yes |
| [#153](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/153) | Production implementation | **BLOCKED** | 20 | None | Yes |

## Complete per-PR records

Changed files below are the complete paginated GitHub set, cross-checked against pinned Git trees. “Exclusive endpoint blobs” counts changed-path final blobs found in no other open-PR head; it is an exact tree comparison, not a semantic uniqueness or acceptance claim. An older blob replaced by a successor can be exclusive while its feature is superseded. Deletions, authorship and commits must be assessed with the accompanying notes/graph.

### #72 — Add production textures for all 40 custom mobs

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `resources/40-custom-mob-production-textures` at `0cc0a9eaf58290bbde2a4b45f68df101f8f7c6d2`; tree `292e5417ba8c14df59b610735d88513f89b3bf3f`.
- **Declared base:** `stage-20/checkpoint` at `20121ca7cceae03602038de31076bd9ace115208`; merge base `20121ca7cceae03602038de31076bd9ace115208`. State OPEN, not Draft; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Production texture/renderer payload already selectively retained by #104→#103→#135. No additional production donor needed; retain human art acceptance gate. Exact exclusive endpoint blobs: 3; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/37804219020): **success**.
- **Review status:** chatgpt-codex-connector[bot]: COMMENTED.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 94 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	build.gradle
added	docs/40_MOB_SPAWN_ECOLOGY_AUDIT_2026-10-08.md
modified	docs/CUSTOM_MOB_RESOURCE_BACKLOG.md
added	docs/CUSTOM_MOB_TEXTURE_PRODUCTION_MANIFEST.md
modified	docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md
modified	docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md
added	src/client/java/dev/itzgamesgd/moreminecrafts/client/CustomMobRenderers.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobGameTests.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/WolfMixinAccessors.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/FeralWolf.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mob/GiantBeeEntity.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mob/WildPiglin.java
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/armored_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ashen_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit_archer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit_warrior.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_bat.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_creeper.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_creeper_armor.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/end_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_angry.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_angry_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_tame.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_tame_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/forest_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/forest_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/frozen_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/frozen_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/fungal_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/fungal_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_nectar.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_nectar_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_nectar.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_nectar_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ice_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ice_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_magma_cube.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_phantom.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_phantom_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_wither_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mossy_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mossy_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mountain_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mountain_skeleton_overlay.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mud_slime.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/rotten_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/sand_slime.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/small_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/small_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wandering_raider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_piglin.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_piglin_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_slime.png
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobRosterDefinitionTest.java
```

</details>

### #83 — Prototype: additive Ancient City Arena with verified Minecraft screenshots

- **Class / disposition:** Experiment / **KEEP**.
- **Head:** `codex/ancient-city-arena-prototype-20261013` at `e7dc53f9f48dfece8a5723debd5526dc061107d1`; tree `be8508b068ac3c44eb84174bfc1bb586d5b4b735`.
- **Declared base:** `stage-20/checkpoint` at `20121ca7cceae03602038de31076bd9ace115208`; merge base `20121ca7cceae03602038de31076bd9ace115208`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique approved arena prototype: NBT, additive pool/mixin, probe and capture harness. Isolated optional worldgen batch; no implied production promotion. Exact exclusive endpoint blobs: 22; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38356029596): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner approval before promoting optional prototype. Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 22 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	.github/workflows/ancient-city-arena-validation.yml
added	docs/ANCIENT_CITY_ARENA_PROTOTYPE_2026-10-13.md
added	docs/images/ancient-city-arena/arena-camera-0.png
added	docs/images/ancient-city-arena/arena-camera-1.png
added	scripts/arena_validation/capture_client.sh
added	scripts/arena_validation/check_screenshots.py
added	scripts/arena_validation/rcon.py
added	scripts/structures/generate_ancient_city_arena.py
added	scripts/structures/requirements.txt
added	scripts/structures/validate_ancient_city_arena.py
added	src/client/java/dev/itzgamesgd/moreminecrafts/client/ArenaVisualValidationHarness.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgen/AncientCityArenaGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/StructureTemplatePoolMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/ArenaWorldgenProbe.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/StructurePoolContributions.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/worldgen/structure/StructurePoolExtension.java
added	src/main/resources/data/moreminecrafts/structure/ancient_city/arena.nbt
added	src/main/resources/data/moreminecrafts/worldgen/template_pool/ancient_city/arena_additions.json
modified	src/main/resources/moreminecrafts.mixins.json
```

</details>

### #88 — Stage 22: repair Research prerequisite purchase validation

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `stage-22/research-restructure` at `d65f59059314b6e7b4e7ae4f534a12a7342d4171`; tree `89dca2144f20134e624369feda6e78c9aaa2e085`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, not Draft; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** V3 prerequisite lookup repair retained throughout research→XP line. Exact exclusive endpoint blobs: 2; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38662367518): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38662367502): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38662367520): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38662367513): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 6 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md
added	docs/STAGE22_RESEARCH_DESIGN_GAP_AUDIT_DRAFT.md
added	docs/STAGE22_RESEARCH_IMPLEMENTATION_HANDOFF_2026-10-15.md
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPurchaseService.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPurchasePrerequisiteTest.java
```

</details>

### #89 — Draft: Stage 22 research roots, passives, and C-Lite

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage22-research-implementation-20261015` at `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`; tree `27891cafcda10a2487ac91df4d7a6bba1509e586`.
- **Declared base:** `stage-22/research-restructure` at `d65f59059314b6e7b4e7ae4f534a12a7342d4171`; merge base `d65f59059314b6e7b4e7ae4f534a12a7342d4171`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#88](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/88). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Seven roots, migration and initial passives retained in #135 ancestry; effect-level live coverage incomplete. Exact exclusive endpoint blobs: 6; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38671285561): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38671285546): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38671285571): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 38 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
added	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
modified	docs/RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md
modified	docs/superpowers/specs/2026-10-11-research-knowledge-node-tree-manifest.json
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionPurchaseGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ItemStackMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/LivingEntityMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchCropGrowthMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchExperienceOrbMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchFurnaceResultSlotMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMobXpMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchPotionDurationMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchThrownProjectileMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ServerPlayerGameModeMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRuntime.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionNodeLayoutCatalog.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchCatalog.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPassiveMath.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRootMigration.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchXpOrbContext.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
modified	src/main/resources/assets/moreminecrafts/progression/node_layout.json
modified	src/main/resources/data/moreminecrafts/research/research_v3.json
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionCoreTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionNodeLayoutCatalogTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionScreenDataTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPassiveMathTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPurchasePrerequisiteTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRootEffectsTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRootMigrationTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchV3CatalogTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchV3LedgerTest.java
```

</details>

### #90 — Stage 23-A: implement research content batch 1

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-research-content-20261015` at `1edbdb6bc6bbd82c22f5e0865c6dd0d8c70a2469`; tree `c341fca0d8331ca3ddb06b65169beecafc7040b0`.
- **Declared base:** `codex/stage22-research-implementation-20261015` at `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`; merge base `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#89](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/89). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Eight research effects retained through #91/#94 onward; obsolete shared hook files must not replace newer handlers. Exact exclusive endpoint blobs: 6; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38681042713): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38681042894): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38681042719): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 29 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/RESEARCH_COMBAT_ECOLOGY_CONVENIENCE_APPROVAL_2026-10-15.md
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_BATCH1_MANIFEST_2026-10-15.md
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingTiming.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AnvilMenuMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ItemStackMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeCraftingMenuMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchBeehiveMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ServerPlayerGameModeMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/network/FishingChallengePayload.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchCraftingRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMiningContext.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
added	src/main/resources/data/moreminecrafts/tags/block/research_ores.json
added	src/main/resources/data/moreminecrafts/tags/item/research_seeds.json
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/fishing/FishingTimingTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentRulesTest.java
```

</details>

### #91 — Stage 23-A: implement research content batch 2

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-batch2-20261015` at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; tree `76d7a18d53606688876bec5757f0799096977e36`.
- **Declared base:** `codex/stage23a-research-content-20261015` at `1edbdb6bc6bbd82c22f5e0865c6dd0d8c70a2469`; merge base `1edbdb6bc6bbd82c22f5e0865c6dd0d8c70a2469`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#90](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/90). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Livestock, Axe Combat, XP acquisition and movement evidence retained in cumulative research/XP source. Exact exclusive endpoint blobs: 5; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38689829485): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38689829569): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38689829634): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 24 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_BATCH2_MANIFEST_2026-10-15.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/LivingEntityMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchAxeAttackMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchLivestockMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchXpAcquisitionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchAxeCombatRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchAxeCombatRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchLivestockRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchLivestockRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRuntime.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchXpAcquisition.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchXpAcquisitionRules.java
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchAxeCombatRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchLivestockRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchXpAcquisitionRulesTest.java
```

</details>

### #92 — Stage 23-A Batch 1 research effects

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-research-implementation-20261015` at `f632f4f521c782c9614a3bbb71f1ab5579a3358d`; tree `e22d5f1e4a8865f5c8c0071a5e58f0170394b702`.
- **Declared base:** `codex/stage22-research-implementation-20261015` at `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`; merge base `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#89](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/89). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Alternate six-effect implementation; selected runtime equivalents are in #94/#103. Whole replay would duplicate or regress mining/beehive/orb handlers. Exact exclusive endpoint blobs: 18; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38691075067): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38691075057): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38691075091): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 18 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_IMPLEMENTATION_MANIFEST_2026-10-15.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AXpCollectionGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogic.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ItemStackMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchBeehiveHarvestMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchExperienceOrbMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMovementExhaustionMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ServerPlayerGameModeMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMiningContext.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
```

</details>

### #93 — Draft: Stage 23-A parallel branch reconciliation

- **Class / disposition:** Integration or reconciliation / **INTEGRATE**.
- **Head:** `codex/stage23a-reconcile-20261015` at `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955`; tree `37d2c80bac22dd2a6626c252769504eb83cbb9e9`.
- **Declared base:** `codex/stage23a-batch2-20261015` at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; merge base `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#91](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/91). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Runtime equivalents are reconciled in #103; retain only missing actual sprint/swim hook and inclusive 4/6-block orb boundary GameTests. Unique test donor, not another runtime implementation. Exact exclusive endpoint blobs: 20; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38706241965): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38706241708): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38706241877): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 22 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_PARALLEL_RECONCILIATION_MANIFEST_2026-10-15.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/fishing/FisheriesStudyGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogic.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchBeehiveMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMovementExhaustionMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchXpAcquisitionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementExhaustion.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchParallelBranchRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogicTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/fishing/FishingTimingTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchParallelBranchRulesTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchXpAcquisitionRulesTest.java
```

</details>

### #94 — Stage 23-A: selectively integrate unique research effects

- **Class / disposition:** Integration or reconciliation / **SUPERSEDE**.
- **Head:** `codex/stage23a-integrated-research-20261015` at `5e48deacbeed95783902f2b71e04e2ae25005606`; tree `f5ecc119f57c89513bdeb9bee47f70d47a9a05a5`.
- **Declared base:** `codex/stage23a-batch2-20261015` at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; merge base `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#91](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/91). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Selected unique research effects retained in #95→#135; do not replay alternative sibling architecture. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38697865742): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38697865744): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38697865726): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 16 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_INTEGRATION_MANIFEST_2026-10-15.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AMovementExhaustionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AXpCollectionGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogic.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchExperienceOrbMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMovementExhaustionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
```

</details>

### #95 — Draft: Stage 23-A Batch 3 precision crafting; furnace nodes deferred

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-batch3-20261015` at `c3683de7338a40143ee45dbcc76918c9cb363390`; tree `4b8baa7ff01119f35d2b8010e7f4ba4ca8c744af`.
- **Declared base:** `codex/stage23a-integrated-research-20261015` at `5e48deacbeed95783902f2b71e04e2ae25005606`; merge base `5e48deacbeed95783902f2b71e04e2ae25005606`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#94](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/94). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Precision Crafting retained through furnace/cumulative line; initial furnace deferral superseded by #96. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38727817262): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38727817209): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38727817213): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38727817228): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 16 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	.github/workflows/k5-stage23a-batch3-crafting-probe.yml
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_BATCH3_MANIFEST_2026-10-15.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeAcquisitionGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeCraftingMenuMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeRecipeCraftCountMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRuntime.java
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/compressed_tokens.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/ordinary.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/precious.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/processed.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/rare.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/unique.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRulesTest.java
```

</details>

### #96 — Draft: Stage 23-A furnace research infrastructure

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-furnace-infrastructure-20261015` at `f28658e5e03ae2e78a44af8e921d06b6f2cdbb47`; tree `5391cb7437eace36805d2c10bd6a6a5d42e44903`.
- **Declared base:** `codex/stage23a-batch3-20261015` at `c3683de7338a40143ee45dbcc76918c9cb363390`; merge base `c3683de7338a40143ee45dbcc76918c9cb363390`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#95](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/95). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Furnace attribution/infrastructure retained; later #105 persistence semantics supersede early close/session assumptions. Exact exclusive endpoint blobs: 8; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38747876971): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38747875991): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38747876096): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38747875965): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 15 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/STAGE23A_FURNACE_INFRASTRUCTURE_2026-10-15.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchInfrastructureGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceItem.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceMenu.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchAttribution.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchState.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchStateAccess.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/FurnaceBlockInteractionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/FurnaceMenuInteractionMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeFurnaceMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchProgressLookup.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModDataComponents.java
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchStateTest.java
```

</details>

### #97 — Draft: Stage 23-A Fisheries Study exact-allocation hotfix

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `codex/stage23a-fisheries-hotfix-20261015` at `df960d6511fbf9bbe0a649d8f68de6c12362d458`; tree `817a051ac74ab2a9f35c13c09b91fa39d713f55b`.
- **Declared base:** `codex/stage23a-integrated-research-20261015` at `5e48deacbeed95783902f2b71e04e2ae25005606`; merge base `5e48deacbeed95783902f2b71e04e2ae25005606`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#94](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/94). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Exact-fifths Fisheries arithmetic and tests copied by fa903d6 into #103 and retained in #135. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38751593376): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38751593221): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38751593208): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 2 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
```

</details>

### #98 — Draft: Stage 23-A expanded research effects on furnace infrastructure

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-expanded-research-20261015` at `7231c7aad598e9cada152537dfece7cfad2756dc`; tree `4e7849b60e94aaa689710e825791b56631e8cfd9`.
- **Declared base:** `codex/stage23a-furnace-infrastructure-20261015` at `f28658e5e03ae2e78a44af8e921d06b6f2cdbb47`; merge base `f28658e5e03ae2e78a44af8e921d06b6f2cdbb47`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#96](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/96). Identical HEAD envelopes: [#100](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/100). Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Six research effects retained; exact same HEAD as #100. Prefer later cumulative source. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237516): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237344): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237400): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237442): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 24 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_EXPANDED_BATCH_MANIFEST_2026-10-15.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchInfrastructureGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AExpandedGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceItem.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceMenu.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchAttribution.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRuntime.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchState.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeFurnaceMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/LivingEntityMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchEcologyDamage.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchProgressLookup.java
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/aquatic_hostiles.json
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/bosses.json
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/hoglins.json
added	src/main/resources/data/moreminecrafts/tags/item/research/mineral_smelting_inputs.json
added	src/main/resources/data/moreminecrafts/tags/item/research/mineral_smelting_outputs.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRulesTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchStateTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchEcologyDamageTest.java
```

</details>

### #99 — Draft: Stage Live-Fix A repairs

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `codex/stage23-livefix-a-20261016` at `eb80a148065c29394f7a46650c05ab72dda9140d`; tree `00f66d1e320f264171f3467a12c730b201abd31a`.
- **Declared base:** `codex/stage23a-integrated-research-20261015` at `5e48deacbeed95783902f2b71e04e2ae25005606`; merge base `5e48deacbeed95783902f2b71e04e2ae25005606`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#94](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/94). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Live-Fix A final blobs copied into #103. Flame UV/sweep/viewport fixes retained by #135 but absent main; selector evolved later. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38828745962): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38828745932): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38828745935): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 10 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	gradle/validate-flame-sword-production.gradle
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/EnchantingProgressionScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordItem.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfx.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScale.java
modified	src/main/resources/assets/moreminecrafts/models/item/flame_sword.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/equipment/FlameSwordModelTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfxTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScaleTest.java
```

</details>

### #100 — Draft: Stage 23 linear-chain integration (#94 → #95 → #96 → #98)

- **Class / disposition:** Integration or reconciliation / **CLOSE-CANDIDATE**.
- **Head:** `integration/stage23-linear-chain-20261016` at `7231c7aad598e9cada152537dfece7cfad2756dc`; tree `4e7849b60e94aaa689710e825791b56631e8cfd9`.
- **Declared base:** `codex/stage23a-batch2-20261015` at `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`; merge base `68a7fafb4ed5f2f0fd52211a22a6785f9b5e4fb3`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#96](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/96). Identical HEAD envelopes: [#98](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/98). Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Exact duplicate HEAD of #98 with a broader declared base; an integration envelope, not additional implementation. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237516): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237344): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237400): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38831237442): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 58 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-mapped-sources-probe.yml
added	.github/workflows/k5-stage23a-batch3-crafting-probe.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_BATCH3_MANIFEST_2026-10-15.md
added	docs/RESEARCH_STAGE23A_EXPANDED_BATCH_MANIFEST_2026-10-15.md
added	docs/RESEARCH_STAGE23A_INTEGRATION_MANIFEST_2026-10-15.md
added	docs/STAGE23A_FURNACE_INFRASTRUCTURE_2026-10-15.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchInfrastructureGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeAcquisitionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AExpandedGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AMovementExhaustionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AXpCollectionGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceItem.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceMenu.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingLogic.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchAttribution.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchState.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchStateAccess.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/FurnaceBlockInteractionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/FurnaceMenuInteractionMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeCraftingMenuMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeFurnaceMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeRecipeCraftCountMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/LivingEntityMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchExperienceOrbMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchMovementExhaustionMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchEcologyDamage.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchProgressLookup.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRewards.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModDataComponents.java
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/aquatic_hostiles.json
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/bosses.json
added	src/main/resources/data/moreminecrafts/tags/entity_type/research/ecology/hoglins.json
added	src/main/resources/data/moreminecrafts/tags/item/research/mineral_smelting_inputs.json
added	src/main/resources/data/moreminecrafts/tags/item/research/mineral_smelting_outputs.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/compressed_tokens.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/ordinary.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/precious.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/processed.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/rare.json
added	src/main/resources/data/moreminecrafts/tags/item/research/precision_crafting/unique.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchStateTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/PrecisionCraftingRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchEcologyDamageTest.java
```

</details>

### #102 — Draft: Stage 23 remaining branch reconciliation audit (#92/#93/#97/#99)

- **Class / disposition:** Documentation-only audit / **KEEP**.
- **Head:** `integration/stage23-reconciliation-audit-20261016` at `601cc28b9d078b01f1d01451fd07f278839eb4b0`; tree `748eb3b05c4b7836e92d609fbf5da35c5bcdf0b3`.
- **Declared base:** `integration/stage23-linear-chain-20261016` at `7231c7aad598e9cada152537dfece7cfad2756dc`; merge base `7231c7aad598e9cada152537dfece7cfad2756dc`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#100](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/100), [#98](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/98). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Historical sibling-reconciliation analysis only; unique document, no production integration. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38832769569): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/STAGE23_REMAINING_BRANCH_RECONCILIATION_AUDIT_2026-10-16.md
```

</details>

### #103 — Draft: integrate Stage 23 sibling fixes (#93/#97/#99) and regression tests

- **Class / disposition:** Integration or reconciliation / **SUPERSEDE**.
- **Head:** `integration/stage23-final-reconciliation-20261016` at `bdffa599e1197e66d60a99064dd17a0c8209b9ef`; tree `4e523f72935eb886f13227558c29cad0bd34ddda`.
- **Declared base:** `integration/stage23-linear-chain-20261016` at `7231c7aad598e9cada152537dfece7cfad2756dc`; merge base `7231c7aad598e9cada152537dfece7cfad2756dc`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#100](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/100), [#98](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/98). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Current head includes merged #104 resources/specification plus selective #93/#97/#99 runtime fixes. Retained in #135; body references an older head. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38847380456): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38847380442): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38847380449): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38847380427): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 116 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	build.gradle
added	docs/40_MOB_SPAWN_ECOLOGY_AUDIT_2026-10-08.md
added	docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md
modified	docs/CUSTOM_MOB_RESOURCE_BACKLOG.md
added	docs/CUSTOM_MOB_TEXTURE_PRODUCTION_MANIFEST.md
modified	docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md
modified	docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER_IMPLEMENTATION.md
modified	gradle/validate-flame-sword-production.gradle
added	src/client/java/dev/itzgamesgd/moreminecrafts/client/CustomMobRenderers.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/EnchantingProgressionScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/fishing/FisheriesStudyGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23AReconciliationGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordItem.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfx.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishingChallenges.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchBeehiveMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/WolfMixinAccessors.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobCatalog.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/FeralWolf.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mob/GiantBeeEntity.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mob/WildPiglin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchBeehiveHarvestGuard.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchContentRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScale.java
modified	src/main/resources/assets/moreminecrafts/models/item/flame_sword.json
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/armored_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ashen_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit_archer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/bandit_warrior.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_bat.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_creeper.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_creeper_armor.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/cave_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/deep_sea_drowned_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/desert_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/drowned_sailor_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/end_bug.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_angry.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_angry_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_tame.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/feral_wolf_tame_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/forest_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/forest_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/frozen_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/frozen_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/fungal_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/fungal_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_nectar.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_angry_nectar_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_nectar.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/giant_bee_nectar_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ice_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/ice_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/large_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_magma_cube.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_phantom.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_phantom_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/lesser_wither_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/miner_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mossy_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mossy_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mountain_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mountain_skeleton_overlay.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/mud_slime.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_outer_layer.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/river_drowned_outer_layer_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/rotten_skeleton.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/sand_slime.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/small_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/small_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_spider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_spider_eyes.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_zombie.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/swamp_zombie_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wandering_raider.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_piglin.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_piglin_baby.png
added	src/main/resources/assets/moreminecrafts/textures/entity/mobs/wild_slime.png
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/equipment/FlameSwordModelTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/equipment/GreatswordSweepVfxTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobRosterDefinitionTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchBeehiveHarvestGuardTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ViewportScaleTest.java
```

</details>

### #105 — Draft: Stage 23 Live-Fix B portable furnace persistence

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `codex/stage23-livefix-b-20261016` at `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`; tree `4b5516194223ce901faa8cdcfba1eccb29ad5a36`.
- **Declared base:** `integration/stage23-final-reconciliation-20261016` at `bdffa599e1197e66d60a99064dd17a0c8209b9ef`; merge base `bdffa599e1197e66d60a99064dd17a0c8209b9ef`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#103](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/103). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Portable furnace contents/fuel/progress persistence retained through #135; earlier visual fixtures do not prove physical clicks. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38850688575): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38850688545): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38850688556): **success**; [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38850688546): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38850688549): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 12 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/client-visual-validation.yml
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/UTILITY_EQUIPMENT_FINALIZATION.md
modified	scripts/visual_validation/capture_client.sh
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchInfrastructureGameTests.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceContents.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceItem.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceMenu.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchDebugCommands.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModDataComponents.java
```

</details>

### #106 — Draft: Stage 23-B Cloud Live Client QA design

- **Class / disposition:** Approved design or authority / **KEEP**.
- **Head:** `codex/stage23b-cloud-client-qa-design` at `9b66f49657c084c29873990c0ff8b92d6a0ab620`; tree `b7fd75a4333bb7fae8d7ab758c0ac2d1be72eaf4`.
- **Declared base:** `codex/stage23-livefix-b-20261016` at `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`; merge base `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#105](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/105). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Cloud Live QA specification; sibling of harness #107, not ancestor. Preserve as validation scope, not gameplay. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38856037561): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/superpowers/specs/2026-10-16-stage23b-cloud-live-client-qa-design.md
```

</details>

### #107 — Draft: Stage 23-B Cloud Live Client QA harness

- **Class / disposition:** Tests and validation / **INTEGRATE**.
- **Head:** `codex/stage23b-cloud-live-client-qa` at `dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6`; tree `bcc1fc90c394a454d84f61ac5fd0d50de0163b13`.
- **Declared base:** `codex/stage23-livefix-b-20261016` at `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`; merge base `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#105](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/105). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique six-path clientQa/workflow/build harness absent later cumulative branches. Adapt narrowly; historical transparent backpack result remains FAIL. Exact exclusive endpoint blobs: 6; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885409848): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885409756): **success**; [Minecraft Client Live QA](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885410011): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885409850): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885409786): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 6 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	.github/workflows/client-live-qa.yml
modified	build.gradle
added	docs/superpowers/plans/2026-10-16-stage23b-cloud-live-client-qa.md
added	scripts/client_live_qa/verify_qa_mod_load.sh
added	src/clientQa/java/dev/itzgamesgd/moreminecrafts/clientqa/Stage23BClientQa.java
added	src/clientQa/resources/fabric.mod.json
```

</details>

### #108 — Draft: Stage 23-A expanded batch 2 and portable furnace research

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-expanded-batch2-20261017` at `2c8e984314bb079378cffc95c4f48982a962173b`; tree `4a5cacc2599c54599b9f09c0f50ef264dcf644e5`.
- **Declared base:** `codex/stage23-livefix-b-20261016` at `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`; merge base `5682be107145b9e4f3f7c4acc6e6dd8835ebe01b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#105](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/105). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Expanded research/portable-furnace effects retained through #109→#135. Exact exclusive endpoint blobs: 4; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38929113153): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38929113226): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38929113268): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38929113515): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 22 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_EXPANDED_BATCH2_MANIFEST_2026-10-17.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/PortableFurnaceResearchGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/furnace/PortableFurnaceRestartFixture.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/Research23ABatch2GameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/PortableFurnaceMenu.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/furnace/FurnaceResearchRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchCreeperMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchEffectDurationAccessor.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchEndermanMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchFlightMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchSpiderLeapMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchStatusEffectMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/SmallCaveSpider.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mob/SwampCaveSpider.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchBatch2Rules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchBatch2Runtime.java
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchBatch2RulesTest.java
```

</details>

### #109 — Draft: Stage 23-A remaining research campaign

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/stage23a-remaining-research-20261017` at `9376777264136b37e10071adf8ed59c08da9b6ee`; tree `c1a37eee56fd4552de317171bcdacfd02143dfe2`.
- **Declared base:** `codex/stage23a-expanded-batch2-20261017` at `2c8e984314bb079378cffc95c4f48982a962173b`; merge base `2c8e984314bb079378cffc95c4f48982a962173b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#108](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/108). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Remaining sword/potion/horse/capture/climb campaign retained; many catalog nodes still DATA_ONLY or PARTIAL. Exact exclusive endpoint blobs: 5; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38996974903): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38996974781): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38996974782): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38996974837): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 18 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
added	docs/RESEARCH_STAGE23A_REMAINING_CAMPAIGN_MANIFEST_2026-10-17.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRemainingGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/MobCaptureRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchConsecutiveSweepMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchHorseRidingMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchLingeringPotionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchSplashPotionMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchConsecutiveSweep.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRemainingRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRidingRuntime.java
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchMovementRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchRemainingRulesTest.java
```

</details>

### #110 — Draft: Villager Trading discount and Soil Study (V3 .53 / .16)

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `claude/stage23a-economy-soil-20261017` at `9ec91dc53512796748a6cfc0980cda0b65e764e6`; tree `4e6e34a125a93ff2047eb345bedf1021052ff435`.
- **Declared base:** `codex/stage23a-remaining-research-20261017` at `9376777264136b37e10071adf8ed59c08da9b6ee`; merge base `9376777264136b37e10071adf8ed59c08da9b6ee`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#109](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/109). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Soil/Villager implementation retained, but old discount arithmetic superseded by selectively copied #116. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39018766925): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39018767056): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39018766944): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39018767041): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 24 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
modified	docs/RESEARCH_STAGE23A_REMAINING_CAMPAIGN_MANIFEST_2026-10-17.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/VillagerTradingGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/BlockMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/KnowledgeItemUseMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchCropGrowthMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ResearchVillagerDiscountMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/SoilStudyFarmlandMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/SoilStudyFarmlandMutationMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/SoilStudyStemGrowthMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/FarmlandCultivatorData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/FarmlandCultivators.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscount.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyRuntime.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscountTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyRulesTest.java
```

</details>

### #112 — Draft: XP Quest authority decisions and design documents A–D

- **Class / disposition:** Approved design or authority / **KEEP**.
- **Head:** `claude/xp-quest-authority-20261017` at `63107098fcde6742704a07394439e5cdf9c6ad49`; tree `8391397f00abeaba78a2c11c03411d757096cdf8`.
- **Declared base:** `claude/stage23a-economy-soil-20261017` at `9ec91dc53512796748a6cfc0980cda0b65e764e6`; merge base `9ec91dc53512796748a6cfc0980cda0b65e764e6`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Approved XP activation, exact-XP and claim transaction authority retained via actual #118 merge; proposals inside A–D remain proposals. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837518): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837326): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837519): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837513): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837529): **success**; [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044837494): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 17 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/STAGE_2_GAMEPLAY_RECONCILIATION.md
modified	docs/UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md
added	docs/design/xp-quest/A_XP_QUEST_CATALOG.md
added	docs/design/xp-quest/B_XP_ECONOMY_SCENARIOS.md
added	docs/design/xp-quest/C_EQUIPMENT_EXPANSION_PROPOSAL.md
added	docs/design/xp-quest/D_UI_XP_COST_UNIT_DESIGN.md
added	docs/design/xp-quest/Q1_PARALLEL_INTERFACE_CONTRACT.md
added	docs/design/xp-quest/README.md
added	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
added	docs/design/xp-quest/audit/AUTHORITY_CONFLICTS.md
added	docs/design/xp-quest/audit/CLASSIFICATION_RECONCILIATION.md
added	docs/design/xp-quest/audit/DEFECT_REGISTER.md
added	docs/design/xp-quest/audit/FINAL_AUDIT_REPORT.md
added	docs/design/xp-quest/audit/IMPLEMENTATION_TEST_GATE.md
added	docs/design/xp-quest/audit/QUEST_200_AUDIT_MATRIX.md
added	docs/design/xp-quest/audit/XP_ECONOMY_ADVERSARIAL_REVIEW.md
```

</details>

### #113 — docs: finalize accessory +10 enhancement and anvil catalyst design

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `docs/accessory-enhancement-approved-20261017` at `32cbca0a3fe57db86262bca7a865573071763640`; tree `cfb5b8d0775076e111253017050ebdbb1f71aa2b`.
- **Declared base:** `claude/stage23a-economy-soil-20261017` at `9ec91dc53512796748a6cfc0980cda0b65e764e6`; merge base `9ec91dc53512796748a6cfc0980cda0b65e764e6`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Approved accessory enhancement document copied byte-for-byte into #153; merging its research ancestry is unnecessary. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39035912127): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md
```

</details>

### #114 — Draft: Q1-A XP Quest catalog ledger, persistent tracking and retroactive reconciliation

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `claude/xp-quest-q1a-core` at `78d1495402de6f3d7328fcd3117a4996137a05e2`; tree `267a64c5a446569d77e26070ea514d04f2db9c1f`.
- **Declared base:** `claude/xp-quest-authority-20261017` at `7e21fc0c66c51c731aba58fd9b08b79bf5175cec`; merge base `7e21fc0c66c51c731aba58fd9b08b79bf5175cec`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Quest catalog/evidence core incorporated by actual #118 merge and descendants. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042219677): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042219699): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042219695): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042219729): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 11 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTrackingGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestRules.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEvaluator.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestResearchValue.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTracking.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModRegistries.java
added	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestRulesTest.java
```

</details>

### #115 — Draft: Q1-A2 exact raw-XP transactions, claim rollback, Enhancement raw-XP costs

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `claude/xp-quest-q1a2-exact-xp` at `f30d263c93b89b1bab844893bdb82084bfe209bd`; tree `7b0322b5a232b9a41417b941eb7f7c9744f0b19b`.
- **Declared base:** `claude/xp-quest-authority-20261017` at `7e21fc0c66c51c731aba58fd9b08b79bf5175cec`; merge base `7e21fc0c66c51c731aba58fd9b08b79bf5175cec`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Exact raw-XP, rollback and enhancement debit changes retained through #118→#135; reconcile with main-based #153 ordinary enhancement level semantics. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044812203): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044812202): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044812239): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39044812195): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 15 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/T1AnvilScreen.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementTransactionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ExactXpTransactionGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/T1ProgressionGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementOperation.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestProgressService.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/RawXp.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPurchaseService.java
removed	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpLevels.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpMath.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementOperationTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpMathTest.java
```

</details>

### #116 — Independent review and repair for Claude PR #110

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `codex/review-claude-pr110-20261017` at `8f33e72c8a71d29e132050457535ac8a0092a7b0`; tree `e789ec8c2b3820b70db1cf0b1bd11454f27238bd`.
- **Declared base:** `claude/stage23a-economy-soil-20261017` at `9ec91dc53512796748a6cfc0980cda0b65e764e6`; merge base `9ec91dc53512796748a6cfc0980cda0b65e764e6`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#110](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/110). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Four pricing/test files copied independently by #123/#124. Two corrected docs intentionally omitted; retain narrow wording synchronization. Exact exclusive endpoint blobs: 2; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042273176): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042273204): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042273236): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39042273216): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 6 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md
modified	docs/RESEARCH_STAGE23A_REMAINING_CAMPAIGN_MANIFEST_2026-10-17.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/VillagerTradingGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscount.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscountTest.java
```

</details>

### #117 — Draft: experiment — underground kingdom worldgen feasibility probe (MC 26.3)

- **Class / disposition:** Experiment / **KEEP**.
- **Head:** `experiment/underground-kingdom-worldgen-probe` at `991b20c8a052ecf5d73c5130d2d9e0eb4338c041`; tree `edd42ec3eab0cb891a28ecde08cee20f3b049f1e`.
- **Declared base:** `integration/stage23-final-reconciliation-20261016` at `bdffa599e1197e66d60a99064dd17a0c8209b9ef`; merge base `bdffa599e1197e66d60a99064dd17a0c8209b9ef`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#103](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/103). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Isolated opt-in underground-kingdom feasibility source set; no production feature or approved generator promotion. Exact exclusive endpoint blobs: 17; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39043907402): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39043907382): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39043907430): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39043907357): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 17 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	build.gradle
added	docs/UNDERGROUND_KINGDOM_WORLDGEN_PROBE.md
added	docs/underground-kingdom-probe/results.md
added	docs/underground-kingdom-probe/samples.csv
added	docs/underground-kingdom-probe/slices_bare_encapsulate_mid_seed1.txt
added	docs/underground-kingdom-probe/slices_bare_none_mid_seed1.txt
added	docs/underground-kingdom-probe/slices_built_encapsulate_mid_seed1.txt
added	docs/underground-kingdom-probe/slices_built_none_mid_seed1.txt
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgenprobe/WorldgenProbe.java
modified	src/gametest/resources/fabric.mod.json
added	tools/worldgen-probe/aggregate.py
added	tools/worldgen-probe/build_probe_pack.py
added	tools/worldgen-probe/export_csv.py
added	tools/worldgen-probe/nbt.py
added	tools/worldgen-probe/run_matrix.sh
added	tools/worldgen-probe/run_probe.sh
added	tools/worldgen-probe/selftest.py
```

</details>

### #118 — Draft: Q1 integration validation — XP Quest core + exact XP

- **Class / disposition:** Integration or reconciliation / **SUPERSEDE**.
- **Head:** `claude/xp-quest-q1-integration` at `c40ee6b1656800f016644893f5bac7a9c90827b2`; tree `89206fc71c698232ea404025068d4c607e8ec053`.
- **Declared base:** `claude/xp-quest-authority-20261017` at `63107098fcde6742704a07394439e5cdf9c6ad49`; merge base `63107098fcde6742704a07394439e5cdf9c6ad49`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#115](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/115), [#114](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/114), [#112](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/112). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Actual Q1-A/Q1-A2/authority merge retained through #119→#135. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39049400838): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39049400908): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39049400780): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39049400825): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 25 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/T1AnvilScreen.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementTransactionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/ExactXpTransactionGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/T1ProgressionGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTrackingGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementOperation.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestProgressService.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/RawXp.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchPurchaseService.java
removed	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpLevels.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpMath.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEvaluator.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestResearchValue.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTracking.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModRegistries.java
added	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementOperationTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/enhancement/EnhancementRulesTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpMathTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestRulesTest.java
```

</details>

### #119 — XP Quest Q1-B: live event tracking, ore provenance, 39 registered quests

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `claude/xp-quest-q1b-tracking` at `c7fe7fabc5a3bc486e2be0aed8f793628f42ec72`; tree `6e27bfb1924a9d6c313e7ede9579260d40c56145`.
- **Declared base:** `claude/xp-quest-q1-integration` at `c40ee6b1656800f016644893f5bac7a9c90827b2`; merge base `c40ee6b1656800f016644893f5bac7a9c90827b2`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#118](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/118). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Initial 39 registrations/live hooks; #120 reduces registrations to 30 under corrected positive-natural-ore provenance. Exact exclusive endpoint blobs: 11; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39055788688): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39055788695): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39055788670): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39055788803): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 24 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-production-restart.yml
modified	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreFixture.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEventTrackingGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTrackingGameTests.java
modified	src/gametest/resources/fabric.mod.json
modified	src/main/java/dev/itzgamesgd/moreminecrafts/fishing/FishermanFishSale.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestBlockPlacementMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestMovementStatMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestOreChunkGeneratorMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestOreLevelSetBlockMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestTameAnimalTriggerMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRuntime.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreProvenance.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogData.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEvaluator.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestHooks.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTracking.java
added	src/main/resources/data/moreminecrafts/tags/block/quest_tracked_ores.json
modified	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreProvenanceTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
```

</details>

### #120 — Q1-B: require positive natural ore provenance

- **Class / disposition:** Bug fix / **SUPERSEDE**.
- **Head:** `codex/xp-quest-q1b-correction` at `fd4aa831139054f6c736edf58fa6fe5f8cc77814`; tree `ddec9d101092f9b84e80ac8482e689e7280176c8`.
- **Declared base:** `claude/xp-quest-q1b-tracking` at `c7fe7fabc5a3bc486e2be0aed8f793628f42ec72`; merge base `c7fe7fabc5a3bc486e2be0aed8f793628f42ec72`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#119](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/119). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Positive natural-ore provenance and conservative registrations retained through Q1-C and both XP frontier lines. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39065408655): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39065408613): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39065408596): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39065408730): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 15 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/design/xp-quest/Q1B_INDEPENDENT_AUDIT.md
modified	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreRestartGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEventTrackingGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/K5WorldGenRegionBlockMutationMixin.java
removed	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestOreChunkGeneratorMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestOreFeatureMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestOreLevelSetBlockMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestScatteredOreFeatureMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreProvenance.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestHooks.java
modified	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestOreProvenanceTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
```

</details>

### #121 — Q1-C WIP: player-attributed structure loot evidence

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/xp-quest-q1c` at `17e980912d636b0572a3f25b24995ffde31c754c`; tree `5cdaa2619aeb6122d706a70942b501f98c434e76`.
- **Declared base:** `codex/xp-quest-q1b-correction` at `fd4aa831139054f6c736edf58fa6fe5f8cc77814`; merge base `fd4aa831139054f6c736edf58fa6fe5f8cc77814`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#120](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/120). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Q1-C 75 registrations/structure attribution retained by #124→#135; this head lacks #116 pricing repair. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113040991): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113040956): **success**; [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113041035): **success**; [K5 Fixture Isolation Regression](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113041043): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113040948): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39113040953): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 62 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/client-visual-validation.yml
added	.github/workflows/k5-fixture-isolation.yml
modified	.github/workflows/k5-production-restart.yml
modified	docs/design/xp-quest/A_XP_QUEST_CATALOG.md
added	docs/design/xp-quest/Q1C_PROGRESS.md
modified	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbe.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/K5NaturalTreeProbeGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/mob/CustomMobGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeRemainingCollectorsGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestC3ThresholdGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestCombatGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestCombatRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestDragonRematchGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestLootRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestMonumentGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestMonumentRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestPersistentPlayers.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestRaidGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureEntryGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureFixture.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureLootGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestVaultFixture.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestVaultRestartGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestVaultRewardGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEventTrackingGameTests.java
added	src/gametest/resources/data/moreminecrafts/loot_table/chests/simple_dungeon.json
modified	src/gametest/resources/fabric.mod.json
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestBlockLootGenerationMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestDragonFightMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestEntityLootGenerationMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestRaidVictoryMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestVaultRewardMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/KnowledgeEventCollectors.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/MonumentRewardData.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionScreenData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestCombatKills.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestDragonRematch.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestDragonRematchData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestMonumentParticipation.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestRaidVictory.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureEntry.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureLoot.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestVaultRewards.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogData.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEvaluator.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTracking.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
added	src/main/resources/data/moreminecrafts/tags/item/daggers.json
added	src/main/resources/data/moreminecrafts/tags/item/greatswords.json
modified	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/MonumentRewardDataTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestDragonRematchDataTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
```

</details>

### #122 — docs: finalize 5 pillager patrol variants and custom captain

- **Class / disposition:** Approved design or authority / **KEEP**.
- **Head:** `docs/world-events-patrol-design-20261018` at `ad778059718b0a1b911264b72818bbaca8026fcd`; tree `2357c1324d1a3b65d4889aa690d4fe4b60b4d640`.
- **Declared base:** `docs/accessory-enhancement-approved-20261017` at `32cbca0a3fe57db86262bca7a865573071763640`; merge base `32cbca0a3fe57db86262bca7a865573071763640`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#113](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/113). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Approved five pillager patrol variants/captain design only; no patrol runtime source. Includes copied accessory docs, not new equipment code. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39108814243): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 3 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md
added	docs/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md
added	docs/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md
```

</details>

### #123 — reconcile: apply approved PR #116 villager pricing and regression tests onto Q1-B

- **Class / disposition:** Integration or reconciliation / **CLOSE-CANDIDATE**.
- **Head:** `integration/q1b-villager-discount-pr116-20261019` at `4fbb036e0158098f4b9d6733c68ddd73b87db826`; tree `e82e027b35ed7339a9d003f2110f017c48f799ca`.
- **Declared base:** `codex/xp-quest-q1b-correction` at `fd4aa831139054f6c736edf58fa6fe5f8cc77814`; merge base `fd4aa831139054f6c736edf58fa6fe5f8cc77814`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#120](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/120). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Q1-B copy of four #116 fixes; #124 applies same blobs on richer Q1-C line. No second additive fix to merge. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39182316631): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39182316652): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39182316633): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39182316642): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 4 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/VillagerTradingGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscount.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscountTest.java
```

</details>

### #124 — reconcile(Q1-C): integrate approved #116 villager discount fixes into #121 lineage

- **Class / disposition:** Integration or reconciliation / **SUPERSEDE**.
- **Head:** `integration/q1c-pr116-discount-20261019` at `5ea5f54614943c8fd00f047b7f8379f385d6e5e8`; tree `f6110dfc5ac970d84f0bd607216e92ee881749eb`.
- **Declared base:** `codex/xp-quest-q1c` at `17e980912d636b0572a3f25b24995ffde31c754c`; merge base `17e980912d636b0572a3f25b24995ffde31c754c`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#121](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/121). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Four-file Q1-C pricing/test repair retained by #125 and both XP frontier lines. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39183978575): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39183978752): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39183978748): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39183978699): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 4 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/SoilStudyGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/VillagerTradingGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscount.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/ResearchVillagerDiscountTest.java
```

</details>

### #125 — audit(Stage23): reconcile PR #92/#93 successors and restore missing fisheries codec test

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `audit/stage23-pr92-pr93-reconciliation-20261019` at `079d36ac3bd2d8950118e30647d5f2efad19f947`; tree `1720216bd17020bbb6384b145f049b30eac1daf8`.
- **Declared base:** `integration/q1c-pr116-discount-20261019` at `5ea5f54614943c8fd00f047b7f8379f385d6e5e8`; merge base `5ea5f54614943c8fd00f047b7f8379f385d6e5e8`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#124](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/124). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Restored Fisheries codec regression retained in #126/#130/#135; additional #93 event cases still absent. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39185666213): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 2 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/STAGE23_PR92_PR93_RECONCILIATION_2026-10-19.md
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/Research23AEffectRulesTest.java
```

</details>

### #126 — ui(selector): XP Quest card redesign + four-card keyboard navigation

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `design/xp-quest-selector-redesign-20261019` at `a9b89755f7c4e924243fa4229a90177433712be4`; tree `f11a9bc9731993e5848f9fabf0745b53ed42c4ec`.
- **Declared base:** `audit/stage23-pr92-pr93-reconciliation-20261019` at `079d36ac3bd2d8950118e30647d5f2efad19f947`; merge base `079d36ac3bd2d8950118e30647d5f2efad19f947`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#125](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/125). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Redesigns the already-enabled Q1-C XP card and adds four-card keyboard/focus behavior plus chapter-lane tree types; retained by #135, absent skull sibling. Exact exclusive endpoint blobs: 5; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39202819204): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39202819310): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39202819203): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39202819236): **success**; [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39202819308): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 13 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/client-visual-validation.yml
modified	scripts/visual_validation/capture_client.sh
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionSelectionScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeLayout.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeStructure.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
added	src/main/resources/assets/moreminecrafts/textures/gui/selector/xp_quest_card.png
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestLocalizationTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeLayoutTest.java
```

</details>

### #127 — draft(q1c): recover skull provenance decision and record build blocker

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `codex/q1c-recovery-20261019-079d36a` at `334997d2f81d6b324b0cccd3f40b538c921edadc`; tree `1c873c5ee83318a68c842d59e83e3da67ed00c8f`.
- **Declared base:** `audit/stage23-pr92-pr93-reconciliation-20261019` at `079d36ac3bd2d8950118e30647d5f2efad19f947`; merge base `079d36ac3bd2d8950118e30647d5f2efad19f947`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#125](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/125). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Recovered approved skull-provenance decision inherited by #128/#130; blocker log is historical, not current failing build evidence. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39193136083): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
```

</details>

### #128 — feat(q1c): Wither Skeleton skull provenance + register xpq.cmb.wither_skulls_3

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `claude/q1c-skull-provenance-20261019` at `6ba15ac2cb40cc919dfee41b24f7813231d11efb`; tree `aec7b65266c7b7e3c353cb5c20f202507c445cab`.
- **Declared base:** `codex/q1c-recovery-20261019-079d36a` at `334997d2f81d6b324b0cccd3f40b538c921edadc`; merge base `334997d2f81d6b324b0cccd3f40b538c921edadc`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#127](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/127). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** 76th skull quest/provenance runtime and tests retained in #130, with corrected pickup accounting there; forced-halt limitation remains. Exact exclusive endpoint blobs: 2; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39196289996): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39196289956): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39196290012): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39196290053): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 17 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/design/xp-quest/Q1C_PROGRESS.md
modified	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestC3ThresholdGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestWitherSkullGameTests.java
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestEventTrackingGameTests.java
added	src/gametest/resources/data/moreminecrafts-gametest/loot_table/wither_skull_always.json
modified	src/gametest/resources/fabric.mod.json
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestWitherSkullDeathLootMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestWitherSkullMergeMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestWitherSkullPickupMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestWitherSkullSpawnMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestWitherSkullProvenance.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestTracking.java
modified	src/main/resources/data/moreminecrafts/xp_quest/catalog.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestWitherSkullQuestTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/XpQuestCatalogDataTest.java
```

</details>

### #129 — design(xp-quest): 200-quest structure proposal (presentation-only, draft)

- **Class / disposition:** Unapproved proposal / **KEEP**.
- **Head:** `design/xp-quest-structure-200-20261019` at `2176935929c990a210fae64582d524d30993bc8a`; tree `7933b4565c76ccc3885eba01ffa9202329219378`.
- **Declared base:** `design/xp-quest-selector-redesign-20261019` at `a9b89755f7c4e924243fa4229a90177433712be4`; merge base `a9b89755f7c4e924243fa4229a90177433712be4`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#126](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/126). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** 200-node presentation proposal plus isolated preview code; no production registration expansion. Do not approve locked states/gameplay edges by importing previews. Exact exclusive endpoint blobs: 7; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39222428500): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39222428205): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner approval for unapproved structure/locked-state/gameplay-edge proposals; D1–D5 work does not settle the vanilla-screen conflict. Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 20 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/client-visual-validation.yml
added	docs/design/xp-quest/structure/CHAPTER_DESIGN.md
added	docs/design/xp-quest/structure/edge_ledger.json
added	docs/design/xp-quest/structure/localization_coverage.json
added	docs/design/xp-quest/structure/previews/01-overview-all-chapters.png
added	docs/design/xp-quest/structure/previews/02-dense-exploration.png
added	docs/design/xp-quest/structure/previews/03-sparse-ecology.png
added	docs/design/xp-quest/structure/previews/04-cross-chapter-links.png
added	docs/design/xp-quest/structure/previews/05-zoomed-node.png
added	docs/design/xp-quest/structure/quest_structure_mapping.json
modified	scripts/visual_validation/capture_client.sh
added	scripts/xp_quest/generate_structure_proposal.py
added	scripts/xp_quest/render_structure_previews.py
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java
added	src/client/java/dev/itzgamesgd/moreminecrafts/client/QuestStructurePreview.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestPreviewIsolationTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestStructureProposalTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeStressTest.java
```

</details>

### #130 — WIP Q1-C #128: fix P2 pickup accounting; preserve P1 forced-halt blocker

- **Class / disposition:** Bug fix / **BLOCKED**.
- **Head:** `codex/q1c-pr128-p1-p2-repair-20261019` at `9c9f22577370cd0160dd2b43b0e92caa7832f420`; tree `54b0bf99a595629eae0b04f22af831d0e18a3640`.
- **Declared base:** `claude/q1c-skull-provenance-20261019` at `6ba15ac2cb40cc919dfee41b24f7813231d11efb`; merge base `6ba15ac2cb40cc919dfee41b24f7813231d11efb`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#128](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/128). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique skull-line pickup-accounting repair. Not in #135. Preserve optional donor; forced-halt provenance persistence remains unresolved. Exact exclusive endpoint blobs: 3; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39208325841): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39208325828): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39208325820): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39208325823): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner approval of a separate skull batch; no fresh gameplay decision needed for approved provenance repair. Execution/closure approval required under this mission; no action taken.

<details><summary>All 3 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/Q1C_PR128_REPAIR_DESIGN.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/progression/QuestWitherSkullGameTests.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/QuestWitherSkullPickupMixin.java
```

</details>

### #131 — docs: consolidate approved territory, economy, bounty and scattered design records

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-economy-bounty-approved-20261019` at `befa4c8a44c8e3556be2dca9af502d36b0cf11ad`; tree `b0529bbd57f10bffb4e0564b051928b4fd3cb1bf`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Consolidated scattered design records; territory portions superseded by explicit shelving #138. Independent content stays design-only. Exact exclusive endpoint blobs: 3; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39212785965): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39212785953): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 29 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md
added	docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md
added	docs/ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md
added	docs/BOUNTY_COMMISSION_APPROVED_2026-10-19.md
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md
added	docs/DESIGN_AUTHORITY_MATRIX.md
added	docs/DESIGN_BRANCH_COVERAGE.md
added	docs/DESIGN_CONFLICT_REGISTER.md
added	docs/DISTRIBUTED_DESIGN_CONSOLIDATION_INDEX_2026-10-19.md
added	docs/IMPLEMENTATION_PHASE_PLAN.md
added	docs/RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md
added	docs/TERRITORY_ECONOMY_APPROVED_2026-10-19.md
added	docs/WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md
added	docs/design/xp-quest/A_XP_QUEST_CATALOG.md
added	docs/design/xp-quest/B_XP_ECONOMY_SCENARIOS.md
added	docs/design/xp-quest/C_EQUIPMENT_EXPANSION_PROPOSAL.md
added	docs/design/xp-quest/D_UI_XP_COST_UNIT_DESIGN.md
added	docs/design/xp-quest/Q1_PARALLEL_INTERFACE_CONTRACT.md
added	docs/design/xp-quest/README.md
added	docs/design/xp-quest/XP_QUEST_IMPLEMENTATION_HANDOFF.md
added	docs/design/xp-quest/audit/AUTHORITY_CONFLICTS.md
added	docs/design/xp-quest/audit/CLASSIFICATION_RECONCILIATION.md
added	docs/design/xp-quest/audit/DEFECT_REGISTER.md
added	docs/design/xp-quest/audit/FINAL_AUDIT_REPORT.md
added	docs/design/xp-quest/audit/IMPLEMENTATION_TEST_GATE.md
added	docs/design/xp-quest/audit/QUEST_200_AUDIT_MATRIX.md
added	docs/design/xp-quest/audit/XP_ECONOMY_ADVERSARIAL_REVIEW.md
added	docs/design/xp-quest/structure/CHAPTER_DESIGN.md
```

</details>

### #132 — docs(D2-A): recover Paper territory registration and Fabric ownership design

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-registration-recovery-d2a-20261019` at `a1e0aeb0ab8ba90419b714a42502cbe8db9d770c`; tree `4ab059ea255ffe0fcdabdea92d70d04d6a979d21`.
- **Declared base:** `design/territory-economy-bounty-approved-20261019` at `befa4c8a44c8e3556be2dca9af502d36b0cf11ad`; merge base `befa4c8a44c8e3556be2dca9af502d36b0cf11ad`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#131](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/131). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Territory registration archaeology/design; superseded by later owner decisions and shelving, not active implementation. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39225314324): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 6 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/DESIGN_AUTHORITY_MATRIX.md
modified	docs/DESIGN_CONFLICT_REGISTER.md
modified	docs/IMPLEMENTATION_PHASE_PLAN.md
added	docs/TERRITORY_OWNERSHIP_CONFLICTS.md
added	docs/TERRITORY_PAPER_RECOVERY_2026-10-19.md
added	docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md
```

</details>

### #133 — docs: approve territory D01–D66; withdraw D67–D78 overengineering

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-owner-decisions-d01-d78-20261020` at `09b33f39f6c0d75f6d1ef837edcdc6d8d8753776`; tree `9ed022a023355f3fbbc9e2f3189df6b260a756e5`.
- **Declared base:** `design/territory-registration-recovery-d2a-20261019` at `a1e0aeb0ab8ba90419b714a42502cbe8db9d770c`; merge base `a1e0aeb0ab8ba90419b714a42502cbe8db9d770c`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#132](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/132). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Historical D01–D66 authority and withdrawn D67–D78; preserve history under #138 shelving. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39330004288): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/TERRITORY_OWNER_DECISIONS_2026-10-20.md
```

</details>

### #134 — docs: reconcile territory docs with owner D01–D66 (D67–D78 withdrawn)

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-owner-decisions-reconcile-20261020` at `e0428763cfd1f720a4da20e1bcc4d561fd153261`; tree `d0e76e84004bfec919b237950d63b2f005d65188`.
- **Declared base:** `design/territory-owner-decisions-d01-d78-20261020` at `09b33f39f6c0d75f6d1ef837edcdc6d8d8753776`; merge base `09b33f39f6c0d75f6d1ef837edcdc6d8d8753776`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#133](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/133). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Territory authority reconciliation inherited by #138; no runtime source. Exact exclusive endpoint blobs: 8; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39330774591): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 8 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/DESIGN_AUTHORITY_MATRIX.md
modified	docs/DESIGN_CONFLICT_REGISTER.md
modified	docs/IMPLEMENTATION_PHASE_PLAN.md
modified	docs/TERRITORY_ECONOMY_APPROVED_2026-10-19.md
modified	docs/TERRITORY_OWNERSHIP_CONFLICTS.md
modified	docs/TERRITORY_OWNER_DECISIONS_2026-10-20.md
modified	docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md
```

</details>

### #135 — feat(xp-quest): semantic zoom, sticky chapter labels, sub-branch rows, per-quest icons, tooltip/keyboard fixes (D1-D5)

- **Class / disposition:** Production implementation / **BLOCKED**.
- **Head:** `design/xp-quest-usability-d1-d5-20261019` at `9a246f1e0cef5e3f39c4bf47b34df6e63877df30`; tree `efcf0ee98d9fef6cf227d9bd6fffe9d9ca87da46`.
- **Declared base:** `design/xp-quest-structure-200-20261019` at `2176935929c990a210fae64582d524d30993bc8a`; merge base `2176935929c990a210fae64582d524d30993bc8a`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#129](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/129). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique XP presentation runtime frontier retaining cumulative research/XP. Only three normal-play nodes; no claim call/claimed indicator/toast, custom-vs-vanilla authority conflict. Exact exclusive endpoint blobs: 18; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39407176860): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39407176598): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39407176558): **success**; [K5 Stage 23-A Batch 3 Crafting Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39407176481): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39407176585): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner resolution of vanilla screen vs custom canvas plus staged integration authorization. Execution/closure approval required under this mission; no action taken.

<details><summary>All 18 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/client-visual-validation.yml
added	docs/design/xp-quest/structure/quest_icons.json
modified	scripts/visual_validation/capture_client.sh
added	scripts/xp_quest/generate_quest_icons.py
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/QuestStructurePreview.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestLevelOfDetail.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestTooltipPlacement.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeLayout.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeStructure.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestDefectRegressionGuardTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestLargeLayoutTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestLevelOfDetailTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestTooltipPlacementTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeLayoutTest.java
modified	src/test/java/dev/itzgamesgd/moreminecrafts/progression/QuestTreeStressTest.java
```

</details>

### #136 — docs(territory): approve D79 registration recipe and D80 immediate T1

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-d79-d80-reconcile-20261020` at `d641075f2078697e432f123d61178a73d26fbdf8`; tree `190b6e711cded6c42009ec89051e21d2731fe09d`.
- **Declared base:** `design/territory-owner-decisions-reconcile-20261020` at `e0428763cfd1f720a4da20e1bcc4d561fd153261`; merge base `e0428763cfd1f720a4da20e1bcc4d561fd153261`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#134](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/134). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** D79/D80 territory decisions inherited by #138; shelved, no active registration task. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39409899197): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 8 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	docs/DESIGN_AUTHORITY_MATRIX.md
modified	docs/DESIGN_CONFLICT_REGISTER.md
modified	docs/IMPLEMENTATION_PHASE_PLAN.md
modified	docs/TERRITORY_ECONOMY_APPROVED_2026-10-19.md
modified	docs/TERRITORY_OWNERSHIP_CONFLICTS.md
modified	docs/TERRITORY_OWNER_DECISIONS_2026-10-20.md
modified	docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md
```

</details>

### #137 — docs(territory): approve D81–D84 and prepare facility design

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/territory-d81-d84-facility-prep-20261021` at `f2cc4f06db44f4125b3987c4b92d2ba8711df06b`; tree `70b3e7d4b1ff58927ea76d2fb45cd1228e38bb25`.
- **Declared base:** `design/territory-d79-d80-reconcile-20261020` at `d641075f2078697e432f123d61178a73d26fbdf8`; merge base `d641075f2078697e432f123d61178a73d26fbdf8`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#136](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/136). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** D81–D84/facility preparation inherited by #138; shelved, not production. Exact exclusive endpoint blobs: 2; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39451124866): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 2 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/TERRITORY_OWNER_DECISIONS_2026-10-20.md
added	docs/TERRITORY_OWNER_DECISIONS_D81_D84_2026-10-21.md
```

</details>

### #138 — docs(scope): shelve territory system and release independent content ideas

- **Class / disposition:** Approved design or authority / **INTEGRATE**.
- **Head:** `design/territory-shelved-independent-content-20261021` at `284ce62ff54247cc0a4e96693c34c190b49c4bbf`; tree `f8a4b480dfe7e1681b5e0fb51a26c6149df91da3`.
- **Declared base:** `design/territory-d81-d84-facility-prep-20261021` at `f2cc4f06db44f4125b3987c4b92d2ba8711df06b`; merge base `f2cc4f06db44f4125b3987c4b92d2ba8711df06b`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#137](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/137). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Preserve explicit SHELVED territory scope and independent-idea status only. Do not revive territory or auto-authorize released ideas. Exact exclusive endpoint blobs: 3; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39453396122): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 3 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	docs/TERRITORY_OWNER_DECISIONS_2026-10-20.md
modified	docs/TERRITORY_OWNER_DECISIONS_D81_D84_2026-10-21.md
added	docs/TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md
```

</details>

### #139 — Audit Q1-C XP Quest production integration readiness

- **Class / disposition:** Documentation-only audit / **KEEP**.
- **Head:** `codex/q1c-production-readiness-audit-20261020` at `526416b16a590abf558534a8426625508c23ad99`; tree `340cd90f8665437448742a9fc55a4ef5dd0b7c0f`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Readiness audit only, one added Markdown vs main. No XP production integration; current full-history check fails. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39454909253): **failure**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39454909289): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/design/xp-quest/Q1C_PRODUCTION_READINESS_AUDIT_2026-10-20.md
```

</details>

### #140 — Draft: XP Quest 200-entry content-design audit

- **Class / disposition:** Approved design or authority / **INTEGRATE**.
- **Head:** `codex/xp-quest-200-content-audit-20261020` at `3e66cfbdfa6a12576ecf0162108250691318c8db`; tree `d5de19d2affff51411074729dc2ba41e0df3f7c1`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Mixed 200-entry audit plus later owner-approved content/recipe decisions. Preserve latest N/O/P amendments; no runtime changes or blanket approval of all rows. Exact exclusive endpoint blobs: 8; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39599484392): **failure**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39599484399): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Only unresolved replacements/predicates/quotas remain owner design gates; N/O/P decisions already settled. Execution/closure approval required under this mission; no action taken.

<details><summary>All 8 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/design/xp-quest/XP_QUEST_200_CONTENT_AUDIT_CHECKPOINT_2026-10-20.md
added	docs/design/xp-quest/audit-200/CONTENT_AUDIT_REPORT.md
added	docs/design/xp-quest/audit-200/ECONOMY_DEPENDENCIES.md
added	docs/design/xp-quest/audit-200/OWNER_DECISIONS_2026-10-21.md
added	docs/design/xp-quest/audit-200/REGISTERED_ATTENTION.md
added	docs/design/xp-quest/audit-200/UNREGISTERED_READINESS.md
added	docs/design/xp-quest/audit-200/quest_inventory.csv
added	docs/design/xp-quest/audit-200/validate_inventory.py
```

</details>

### #141 — Phase 0: free-model delegation experiment (manual workflow, mocked tests)

- **Class / disposition:** Experiment / **BLOCKED**.
- **Head:** `experiment/cloud-free-model-delegation-phase0` at `aa376713aedcd7fecd769af39e18f8532b9e147d`; tree `9e542de0b241775385b06355d68a7e43528105a6`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Manual free-model delegation/tooling experiment; Fabric and full-history checks fail. No gameplay source; omit from integration. Exact exclusive endpoint blobs: 5; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39593158053): **failure**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39593158033): **failure**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 5 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	.github/workflows/free-model-delegation.yml
added	scripts/free_model_delegation/README.md
added	scripts/free_model_delegation/openrouter_client.py
added	scripts/free_model_delegation/run_delegation.py
added	scripts/free_model_delegation/test_delegation.py
```

</details>

### #142 — Draft: add accessory registry and category infrastructure

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/accessory-infrastructure-20261021` at `9b79f9be03d3227f1e295dda36ddc2dbc09ccabb`; tree `f6e4eadddf911e12265c5678364f7d6cb345495c`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Accessory registry/category foundation retained in #153. Standalone full-history check fails; inherited authority route repaired later. Exact exclusive endpoint blobs: 4; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39608722001): **failure**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39608722221): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39608722064): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39608722053): **success**; [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39608721982): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 8 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryCategory.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryDefinition.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryItem.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryLoadout.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModItems.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryCatalogTest.java
```

</details>

### #143 — Draft: add persistent accessory equipment and inventory integration

- **Class / disposition:** Production implementation / **SUPERSEDE**.
- **Head:** `codex/accessory-persistence-inventory-20261021` at `f2e6e87fe95ee4454f4b365c569ecfefae619e9e`; tree `0a05986578cdcd8a0bba0bc21858304a49373463`.
- **Declared base:** `codex/accessory-infrastructure-20261021` at `9b79f9be03d3227f1e295dda36ddc2dbc09ccabb`; merge base `9b79f9be03d3227f1e295dda36ddc2dbc09ccabb`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#142](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/142). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Five full-stack persisted accessory slots/menu/death/sync retained in #153; no accessory effects. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39635565357): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39635565382): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39635565368): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 16 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	src/client/java/dev/itzgamesgd/moreminecrafts/mixin/AbstractContainerScreenAccessor.java
added	src/client/java/dev/itzgamesgd/moreminecrafts/mixin/AccessoryInventoryScreenMixin.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEquipmentGameTests.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryDeathDrops.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEquipmentContainer.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEquipmentRuntime.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryLoadout.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessorySlotRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/equipment/EquipmentAttachments.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AccessoryDeathMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/InventoryMenuMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModRegistries.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
modified	src/main/resources/moreminecrafts.mixins.json
modified	src/test/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryCatalogTest.java
```

</details>

### #144 — Draft: verify accessory lifecycle and restart persistence

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `codex/accessory-lifecycle-qa-20261021` at `2083cf37d163398c953995223be35f1d3ba50f02`; tree `0b1604055948d1374eccfd1eea09104cf7a02908`.
- **Declared base:** `codex/accessory-persistence-inventory-20261021` at `f2e6e87fe95ee4454f4b365c569ecfefae619e9e`; merge base `f2e6e87fe95ee4454f4b365c569ecfefae619e9e`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#143](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/143). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Lifecycle and multi-process restart QA retained in #153; dedicated-server results are not physical client acceptance. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39685248872): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39685248856): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39685248850): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 4 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/k5-production-restart.yml
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEquipmentGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryLifecycleGameTests.java
modified	src/gametest/resources/fabric.mod.json
```

</details>

### #145 — Draft: accessory client validation — shift-click blocked

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `codex/accessory-client-acceptance-20261021` at `d7e8effd4052313e9965ff417001e5c2ad489c06`; tree `6269695b0374d065cdac7c07ebc4c28fd09e9fb1`.
- **Declared base:** `codex/accessory-lifecycle-qa-20261021` at `2083cf37d163398c953995223be35f1d3ba50f02`; merge base `2083cf37d163398c953995223be35f1d3ba50f02`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#144](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/144). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Client fixture/lifecycle setup retained later. Exact-head acceptance fails; #146 fixes input fixture, not a demonstrated production shift-transfer bug. Exact exclusive endpoint blobs: 2; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697867931): **success**; [Accessory Client GameTest Acceptance](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697867701): **failure**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697867750): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697867665): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 6 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	.github/workflows/accessory-client-acceptance.yml
modified	build.gradle
added	docs/validation/ACCESSORY_CLIENT_ACCEPTANCE_2026-10-21.md
modified	src/client/java/dev/itzgamesgd/moreminecrafts/mixin/AbstractContainerScreenAccessor.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryInventoryClientGameTest.java
modified	src/gametest/resources/fabric.mod.json
```

</details>

### #146 — Draft: repair accessory client Shift-click validation fixture

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `codex/accessory-client-repair-20261022` at `b7f72fdae46428110079641c681eabdde175e79c`; tree `190ad9d90e9d9609c264e8fcbfc56f76020f312b`.
- **Declared base:** `codex/accessory-client-acceptance-20261021` at `d7e8effd4052313e9965ff417001e5c2ad489c06`; merge base `d7e8effd4052313e9965ff417001e5c2ad489c06`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#145](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/145). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** SDL/Shift fixture correction retained in #149/#153; latest exact-head client suite succeeds. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39715575590): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39715575562): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39715575576): **success**; [Accessory Client GameTest Acceptance](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39715575588): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 5 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/accessory-client-acceptance.yml
added	docs/validation/ACCESSORY_CLIENT_REPAIR_2026-10-22.md
modified	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryInventoryClientGameTest.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/k5probe/mixin/AccessoryMenuInputProbeMixin.java
modified	src/gametest/resources/moreminecrafts-k5probe.mixins.json
```

</details>

### #147 — docs(structures): square-wall pillager HQ and flat plains/forest placement

- **Class / disposition:** Approved design or authority / **KEEP**.
- **Head:** `design/pillager-headquarters-concept-20261022` at `849c7393e92eb18822b19426aab51ae875c00f32`; tree `f0c5990fe9f0bd8040728fc66e650c1b6e143ad0`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique current pillager-HQ authority (footprint/walls/garrison/placement); exact spacing/modeling/loot details remain deferred. No structure runtime. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39726876897): **failure**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39726876892): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Only deferred spacing/loot/modeling questions require future decisions, outside first batch. Retention/read-only review needs no approval; future integration or closure does.

<details><summary>All 1 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md
```

</details>

### #148 — docs: approved bounty and contract design (Q1-Q26)

- **Class / disposition:** Approved design or authority / **SUPERSEDE**.
- **Head:** `design/bounty-contract-approved-20261022` at `b7cedf199a5f1eda341ce3bd27eabb22eeb309e1`; tree `c9f01a2d6ed0691be9b0205e5bc8773062497f86`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** None among open heads. Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Approved bounty authority retained in actual #150→#151 chain despite main PR base labels. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39716206134): **success**; [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39716206000): **failure**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 2 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md
modified	docs/CONTENT_AUTHORITY_INDEX.md
```

</details>

### #149 — Draft: verify accessory client lifecycle and inventory regressions

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `codex/accessory-client-lifecycle-final-20261022` at `6d37a73f11befea3288e028590c540dc31deda61`; tree `eed55a528f26b4cc1d9ccd4a2c1d8290434bab19`.
- **Declared base:** `codex/accessory-client-repair-20261022` at `b7f72fdae46428110079641c681eabdde175e79c`; merge base `b7f72fdae46428110079641c681eabdde175e79c`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#146](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/146). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Rendered-client lifecycle/inventory fixtures retained in #153; rapid clicks are synchronized per operation, not adverse-network burst verification. Exact exclusive endpoint blobs: 1; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39728005469): **success**; [Accessory Client GameTest Acceptance](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39728005477): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39728005466): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39728005455): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 4 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/accessory-client-acceptance.yml
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryClientLifecycleGameTest.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/equipment/InventoryRegressionClientGameTest.java
modified	src/gametest/resources/fabric.mod.json
```

</details>

### #150 — BC-01 and BC-02 bounty registry audit and economy simulation

- **Class / disposition:** Tests and validation / **SUPERSEDE**.
- **Head:** `audit/bc01-bc02-20261022` at `1269cdbacc7c6b75d8aa3cce1352de9432a61914`; tree `d42bc51f90e3d7b9c9eff74d6b32a03bc5f45a61`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#148](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/148). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Registry audit/economy simulation retained in #151. Simulation values are not approved economic rules. Exact exclusive endpoint blobs: 0; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39718203527): **success**; [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39718203554): **failure**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** No new gameplay approval to preserve the approved portion.  Execution/closure approval required under this mission; no action taken.

<details><summary>All 11 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/bounty_contract/BC-01_DELIVERY_CATALOG.md
added	docs/bounty_contract/BC-01_EXPLORATION_CATALOG.md
added	docs/bounty_contract/BC-01_MOB_CATALOG.md
added	docs/bounty_contract/BC-01_REGISTRY_AUDIT.md
added	docs/bounty_contract/BC-02_BALANCE_RECOMMENDATIONS.md
added	docs/bounty_contract/BC-02_ECONOMY_AUDIT.md
added	docs/bounty_contract/BC-02_SIMULATION_RESULTS.md
added	scripts/bounty_contract_balance/simulate.py
added	scripts/bounty_contract_balance/test_simulate.py
```

</details>

### #151 — BC-03 bounty contracts implementation (in progress)

- **Class / disposition:** Production implementation / **BLOCKED**.
- **Head:** `codex/bc03-bounty-contract-20261022` at `30b5c076554111bf660fe3dca67cde4be735776a`; tree `037dbe2288607901923c8f438b85688b4bde1907`.
- **Declared base:** `main` at `5f92307555ce5c486a2e0e8091f956bcbb6c6646`; merge base `5f92307555ce5c486a2e0e8091f956bcbb6c6646`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#150](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/150). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique bounty/contracts runtime frontier. Exploration/fallback kill count defects, missing profession/economic semantics, partial crash guarantee; current full-history check fails. Exact exclusive endpoint blobs: 53; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [Minecraft Client Visual Validation](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39755218476): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39755218552): **success**; [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39755218719): **success**; [Pre-public full-history secret audit](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39755218455): **failure**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39755218561): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner economic/profession/restock choices remain; approved count repairs are engineering. Execution/closure approval required under this mission; no action taken.

<details><summary>All 62 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
added	docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/bounty_contract/BC-01_DELIVERY_CATALOG.md
added	docs/bounty_contract/BC-01_EXPLORATION_CATALOG.md
added	docs/bounty_contract/BC-01_MOB_CATALOG.md
added	docs/bounty_contract/BC-01_REGISTRY_AUDIT.md
added	docs/bounty_contract/BC-02_BALANCE_RECOMMENDATIONS.md
added	docs/bounty_contract/BC-02_ECONOMY_AUDIT.md
added	docs/bounty_contract/BC-02_SIMULATION_RESULTS.md
added	docs/bounty_contract/BC-03_IMPLEMENTATION.md
added	scripts/bounty_contract_balance/simulate.py
added	scripts/bounty_contract_balance/test_simulate.py
added	scripts/contract_crash/run_matrix.sh
added	src/client/java/dev/itzgamesgd/moreminecrafts/client/ContractScreen.java
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/MoreMinecraftsClient.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractCatalogGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractCrashGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractGeneratorGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractIntegrationGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractRuntimeGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractTradeGameTests.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/bounty/ContractTransactionGameTests.java
modified	src/gametest/resources/fabric.mod.json
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractAttachments.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractBalance.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractCatalog.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractData.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractEconomy.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractGenerator.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractItem.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractLedger.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractLootIntegration.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractMaterializer.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractObjectives.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractRewards.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractService.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractStacks.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractTrades.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractTransactions.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractUiBridge.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractVendorPolicy.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/bounty/DamageCredit.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/ContractTradeStockMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/PlayerListAccessor.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/network/ContractActionPayload.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/network/ModNetworking.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModDataComponents.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModItems.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModRegistries.java
added	src/main/resources/assets/moreminecrafts/items/bounty_contract.json
added	src/main/resources/assets/moreminecrafts/items/special_contract.json
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractBalanceTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractDataTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractEconomyTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractLedgerTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractRewardsTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/ContractVendorPolicyTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/bounty/DamageCreditTest.java
```

</details>

### #152 — Accessory visual assets: concept plan for owner approval (no textures yet)

- **Class / disposition:** Production implementation / **INTEGRATE**.
- **Head:** `codex/accessory-visual-assets-20261022` at `f0dfef9e88b3a284aee9ba751dd71e9847c92271`; tree `ae2b403d46beefbd32f16f77d153539e5eff3f1a`.
- **Declared base:** `codex/accessory-client-lifecycle-final-20261022` at `6d37a73f11befea3288e028590c540dc31deda61`; merge base `6d37a73f11befea3288e028590c540dc31deda61`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#149](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/149). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Latest head adds 12 approved-identity 32x32 texture/model/item-definition sets, resource validator and workflow wiring. Separate sibling of #153; body still falsely says docs only. Human rendered-art acceptance pending. Exact exclusive endpoint blobs: 41; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775414588): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775414539): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775414866): **in_progress**; [Accessory Client GameTest Acceptance](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775405212): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Concept identities and 32x32 size are now approved in current authority; final rendered-art acceptance remains pending. Execution/closure approval required under this mission; no action taken.

<details><summary>All 41 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/accessory-client-acceptance.yml
modified	build.gradle
modified	docs/CONTENT_AUTHORITY_INDEX.md
added	docs/validation/ACCESSORY_VISUAL_CONCEPT_2026-10-22.md
added	gradle/validate-accessory-resources.gradle
added	src/main/resources/assets/moreminecrafts/items/blood_covenant.json
added	src/main/resources/assets/moreminecrafts/items/deep_sea_pearl.json
added	src/main/resources/assets/moreminecrafts/items/ember_ring.json
added	src/main/resources/assets/moreminecrafts/items/heart_of_gravity.json
added	src/main/resources/assets/moreminecrafts/items/phantom_mirror.json
added	src/main/resources/assets/moreminecrafts/items/ring_of_life.json
added	src/main/resources/assets/moreminecrafts/items/ring_of_time.json
added	src/main/resources/assets/moreminecrafts/items/seed_of_life.json
added	src/main/resources/assets/moreminecrafts/items/snow_charm.json
added	src/main/resources/assets/moreminecrafts/items/soul_chain.json
added	src/main/resources/assets/moreminecrafts/items/star_covenant.json
added	src/main/resources/assets/moreminecrafts/items/whisper_of_the_sea.json
added	src/main/resources/assets/moreminecrafts/models/item/blood_covenant.json
added	src/main/resources/assets/moreminecrafts/models/item/deep_sea_pearl.json
added	src/main/resources/assets/moreminecrafts/models/item/ember_ring.json
added	src/main/resources/assets/moreminecrafts/models/item/heart_of_gravity.json
added	src/main/resources/assets/moreminecrafts/models/item/phantom_mirror.json
added	src/main/resources/assets/moreminecrafts/models/item/ring_of_life.json
added	src/main/resources/assets/moreminecrafts/models/item/ring_of_time.json
added	src/main/resources/assets/moreminecrafts/models/item/seed_of_life.json
added	src/main/resources/assets/moreminecrafts/models/item/snow_charm.json
added	src/main/resources/assets/moreminecrafts/models/item/soul_chain.json
added	src/main/resources/assets/moreminecrafts/models/item/star_covenant.json
added	src/main/resources/assets/moreminecrafts/models/item/whisper_of_the_sea.json
added	src/main/resources/assets/moreminecrafts/textures/item/blood_covenant.png
added	src/main/resources/assets/moreminecrafts/textures/item/deep_sea_pearl.png
added	src/main/resources/assets/moreminecrafts/textures/item/ember_ring.png
added	src/main/resources/assets/moreminecrafts/textures/item/heart_of_gravity.png
added	src/main/resources/assets/moreminecrafts/textures/item/phantom_mirror.png
added	src/main/resources/assets/moreminecrafts/textures/item/ring_of_life.png
added	src/main/resources/assets/moreminecrafts/textures/item/ring_of_time.png
added	src/main/resources/assets/moreminecrafts/textures/item/seed_of_life.png
added	src/main/resources/assets/moreminecrafts/textures/item/snow_charm.png
added	src/main/resources/assets/moreminecrafts/textures/item/soul_chain.png
added	src/main/resources/assets/moreminecrafts/textures/item/star_covenant.png
added	src/main/resources/assets/moreminecrafts/textures/item/whisper_of_the_sea.png
```

</details>

### #153 — Draft: implement approved accessory enhancement

- **Class / disposition:** Production implementation / **BLOCKED**.
- **Head:** `codex/accessory-enhancement-20261022` at `f9a31c7117198550c0d5b984b4019af6f164cd94`; tree `862d270e522dc22489c6ec947d0ebfd5d3f6a185`.
- **Declared base:** `codex/accessory-client-lifecycle-final-20261022` at `6d37a73f11befea3288e028590c540dc31deda61`; merge base `6d37a73f11befea3288e028590c540dc31deda61`. State OPEN, DRAFT; GitHub mergeability at metadata fetch: True. This is not an approval.
- **Verified nearest open ancestors:** [#149](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/149). Identical HEAD envelopes: None. Recursive dependency chain is in [the graph](PR_DEPENDENCY_GRAPH.md#verified-nearest-open-head-relationships).
- **Unique / retained / superseded content and overlap:** Unique cumulative accessory runtime/enhancement frontier. Latest added atomic/rejection tests do not repair Creative catalyst return risk; growth tables are not effects. Asset donor #152 is separate; XP composition and final automation/visual gates remain. Exact exclusive endpoint blobs: 18; complete source delta below. Shared domains and semantic conflicts are in [coverage](FEATURE_COVERAGE_MATRIX.md) and [the plan](INTEGRATION_PLAN.md).
- **CI at exact HEAD:** [K5 Production Restart](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775621641): **success**; [K5 Mapped Sources Probe](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775621705): **success**; [Fabric CI](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775621637): **in_progress**; [Accessory Client GameTest Acceptance](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39775613436): **success**.
- **Review status:** No submitted reviews; no formal approval.
- **Verification:** CODE VERIFIED for graph/source/metadata; local Java/unit/GameTest/client/server execution NOT RUN; fresh visual and physical gameplay acceptance VISUAL PENDING. Additional failed workflows above remain BLOCKED where relevant.
- **Owner approval:** Owner durability of slot/death authority and deferred effect/art/catalyst scope; XP and conservation repairs are engineering. Execution/closure approval required under this mission; no action taken.

<details><summary>All 20 changed paths (status from GitHub; merge-base comparison verified)</summary>

```text
modified	.github/workflows/accessory-client-acceptance.yml
added	docs/ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md
added	docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md
modified	docs/CONTENT_AUTHORITY_INDEX.md
modified	src/client/java/dev/itzgamesgd/moreminecrafts/client/T1AnvilScreen.java
added	src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementGameTests.java
modified	src/gametest/resources/fabric.mod.json
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementCatalog.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementOperation.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementRules.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AnvilButtonsMixin.java
added	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AnvilCatalystQuickMoveMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/mixin/AnvilMenuMixin.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/progression/T1AnvilModes.java
modified	src/main/java/dev/itzgamesgd/moreminecrafts/registry/ModItems.java
modified	src/main/resources/assets/moreminecrafts/lang/en_us.json
modified	src/main/resources/assets/moreminecrafts/lang/ko_kr.json
modified	src/main/resources/moreminecrafts.mixins.json
added	src/test/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementCatalogTest.java
added	src/test/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryEnhancementRulesTest.java
```

</details>

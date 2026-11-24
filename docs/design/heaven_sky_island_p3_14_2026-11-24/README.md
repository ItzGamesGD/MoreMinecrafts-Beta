# P3.14 — integration readiness of the heaven sky-island production transition

Work branch `claude/heaven-sky-island-p3-14-integration-readiness-20261124` (from P3.13 `fde09f5`, which is untouched). Validation-only simulation branch `sim/p314-merge-1123` (merge of the newest checkpoint into P3.13, never intended for integration). No product code, pool, NBT, texture, spawn rule changed in this step. All worlds are isolated test worlds under `build/`. No PR, no merge into any existing branch, `main` and `work/grand-unified-20261112` untouched.

## A. Baseline
| item | observation |
|---|---|
| P3.13 | `claude/heaven-sky-island-p3-13-final-verification-20261122` = `fde09f5` (remote == local) |
| P3.13 vs P3.12 | `src/main` diff empty (only docs / tools / tests changed); the 6 production pools == `heaven_worldgen_pending` (6/6 equal); only those 6 of the 58 baseline worldgen/tag files differ from the pre-transition baseline (52 identical incl. all other themes' pools, structure sets, structures, biome tags) |
| P3.13 evidence files | README, worldgen_results (13), client_screenshots (25), contact_sheets (8), nbt_samples_fresh_generation.txt all present at `fde09f5` |
| `work/grand-unified-20261112` | `fcac415a` (2026-11-15). It is an **ancestor** of P3.13 (P3.13 is 101 commits ahead, 0 behind; fast-forward, 0 overlapping files). It is **not the newest integration line**: `origin/checkpoint/grand-unified-20261123` = `0e91e290` (2026-11-24) contains it, contains `origin/main` (`b144365c`, 2026-11-23) and is 40 commits ahead of the P3 fork point. Candidate for the current base: `checkpoint/grand-unified-20261123` (owner to confirm; not substituted silently). |
| heaven series fork point | `4d36dd91` (PR #304, 2026-11-17); 77 commits since; first one (`610e3d6f`, "small/medium/large island families, tower only on large islands") adds the 19-template family set, new start pools and `tower_top` pools for ALL 6 themes (the "114 original NBTs") - **this overhaul exists only on the P3 line, not in checkpoint 1123 / main / grand-unified-20261112** |
Independent re-verification in this step vs inherited from P3.13: file/Git identity checks above, the 765/765 integrated regression, the all-theme world matrix with two seeds and the other-theme client frames are NEW observations of P3.14; the P3.13 verdicts (meadow / mountain_meadow / forest worldgen, client frames, block entities) are quoted, not repeated, except where the matrix below re-observes them.

## B. Integration compatibility
Git (merge simulation of P3.13 + `checkpoint/grand-unified-20261123`, 94 files on the checkpoint side, 6 overlapping with the P3 side):
| file | result | resolution used in the simulation (`integration_simulation/conflict_resolution_vs_checkpoint_1123.patch`) |
|---|---|---|
| `src/gametest/resources/fabric.mod.json` | TEXT CONFLICT (both sides appended gametest entrypoints) | keep both lists (add a comma); JSON valid |
| `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` | TEXT CONFLICT (see semantic) | both sections kept verbatim |
| `docs/CONTENT_AUTHORITY_INDEX.md`, `ModRegistries.java`, `lang/en_us.json`, `lang/ko_kr.json` | auto-merged | heaven and Piglin registrations both present |
Merge into `work/grand-unified-20261112`: no conflict (ancestor). Merge into `main`: 12 commits behind, 15 overlapping files (not simulated further; the checkpoint line already contains main).
Semantic findings:
1. **Authority numbering collision (needs owner decision):** both lines own a section `## 23` in the authority document - checkpoint: "2026-11-17 accessory T2 decisions"; P3: "2026-11-21 heaven sky island policies" with sub-sections 23.1-23.6 that tests look up by text (`### 23.5`, ...). Keeping both verbatim gives two `## 23` headings and passes all tests; renumbering is an authority-document change reserved for the owner.
2. **The P3 line carries more than heaven:** integration brings the all-theme island family overhaul (`610e3d6f`: snowy / desert / badlands / rocky + meadow / forest families, new start / tower_top pools) that is absent from the checkpoint line; its approval status is outside what was verified here.
3. No other feature references the heaven pools: the only Java coupling is block registration (`ModRegistries`); `SkyIslandHabitat` / `SkyIslandRegion` / `SkySpawnRules` are only used by the sky-island mob code; no worldgen JSON outside the sky-island files refers to `sky_islands/` pools. The checkpoint side changes only: Piglin trading, accessories (Feather Charm etc.), sky-mob models (Crystal Bug, Skyguard Archer, Fallen Sentinel - client / model files), CI workflows, 23 piglin offers. Nothing there touches worldgen, structures or templates.
4. Registration order / data-pack loading: the integrated build starts and generates worlds (765 GameTests, all world matrix runs) - no registry conflict observed.

## C. All-theme verification (integrated tree = simulation branch; 8 island structures: meadow, mountain_meadow, forest, snowy, desert, badlands, mountain_snowy, mountain_rocky; there is no plain `rocky` structure)
World matrix (`tools/heaven-p39/analyze_matrix.py`, dumps taken AFTER block-entity access; chunk-map counts and the faulty `inMap` flag are NOT used):
| run | unique natural sites | unresolved jigsaw | block-entity dump (position + block + NBT hash) vs single-stage |
|---|---|---|---|
| seed 42 single stage | 21 (meadow 5, mm 2, forest 4, snowy 1, desert 3, badlands 3, mountain_snowy 2, mountain_rocky 1) | 0 | reference |
| seed 42 reload (second start) | same 21 | 0 | 21 / 21 equal |
| seed 42 two-stage with restart (west half, map-only stage, full stage) | same 21 | 0 | 21 / 21 equal |
| seed 1337 single / reload | 23 (meadow 4, mm 3, forest 4, snowy 1, desert 3, badlands 1, mountain_snowy 3, mountain_rocky 4) | 0 | 23 / 23 equal |
(Sites listed as "not generated" by the tool are the probe's `duplicate of an earlier site` skips, not failures.) Loot containers and vault / trial-spawner block entities exist only on some sites (badlands, snowy, meadow, forest, mountain_meadow, mountain_snowy, mountain_rocky); all identical across stages. Habitat support: >= 95 % of standable columns on every theme except mountain_snowy 440 / 673 and 341 / 432 (partly natural terrain, same method as P3.12). Chunk-loading caveat of P3.13 respected (dump after access; observation window).
Structure pools: the pools of snowy / desert / badlands / mountain_* are byte-identical to the baseline hashes (checked); transitioned pools meadow / forest only. Real client (`other_themes_screenshots/`, `contact_sheets/`; 5 sites x day / opposite / night; every frame `renderedAll=true`): snowy (white temple ruin with spruces), desert (sand islands with cacti), badlands (terracotta islands), mountain_snowy and mountain_rocky (floating islands over natural mountains) render with textures, no missing-texture checkerboard seen; design not judged (no spec used). meadow / mountain_meadow / forest client frames: P3.13 (not repeated). Software renderer; night frames dark by nature.
Authority / spec of the 4 other themes was not re-derived; the checks above are integrity checks (generation, jigsaw, block entities, save / reload / restart, pools unchanged), not a spec compliance review.

## D. Regression
Integrated tree (P3.13 + checkpoint 1123, simulation branch), `rm -rf build/run/gameTest/world && ./gradlew --no-daemon test build`, 6 GB game-test heap: **GameTest 765 / 765, JUnit all green, BUILD SUCCESSFUL (12 min 18 s)** (`integration_simulation/sim_regression_summary.txt`). Count change vs P3.13 (749): +16 GameTests, all from checkpoint-side test files (`PiglinTradeGameTests`, `FeatherCharmGameTests`, `RoyalChestGameTests`, ...): `@GameTest` annotations 748 -> 764; no test lost. Standalone P3.13 tree: 749 / 749 (P3.13 record).
Other systems: affected-by-change analysis (B.3) shows no reference path from ST-01 pyramid, spider colony (ST-04), colosseum (ST-05), Sky Tower or the accessory / Piglin / sky-mob changes to the heaven pools; their GameTests are part of the 765 and passed. **ST-03 (raider stronghold) is not implemented on this tree** (3 resource files, no Java; separate branch `work/st03-transfer-20261114`) - not assessed, not assumed. F7 / G2: no code exists (deferred), unchanged.
Not done: no merge into the real integration branch; other mods / data packs not tested; seeds 42 and 1337 only; a stricter sample of rare themes (snowy / badlands / mountain_rocky had 1 site in one seed).

## E. Verdict: **CONDITIONAL**
Observed basis: no blocking defect (no conflict that changes behaviour, 765 / 765, 44 unique natural sites over two seeds with 0 jigsaw leftovers and 100 % identical block-entity dumps across single / reload / two-stage). Conditions needing explicit owner confirmation before integrating:
1. **Base:** `work/grand-unified-20261112` (the branch named in the task) is stale - confirm `checkpoint/grand-unified-20261123` as the integration base. P3.13 fast-forwards the old one; against the new one it needs the two conflict resolutions above.
2. **Authority numbering:** two `## 23` sections (accessory T2 vs heaven) - owner decides the numbering (tests currently reference the heaven `### 23.x` texts).
3. **Scope awareness:** integration also brings the all-theme island-family overhaul (`610e3d6f`) that the checkpoint line does not have; confirm it is approved content for that line.
4. ST-03 cannot be verified on this tree.
Recommended next task: owner decision on 1-3, then a real (non-simulated) integration branch created from the confirmed base with the same resolutions and a repeat of the regression.

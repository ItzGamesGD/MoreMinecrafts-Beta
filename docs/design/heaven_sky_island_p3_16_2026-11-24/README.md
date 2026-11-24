# P3.16 — residual-risk verification before the final integration approval

Verification branch `claude/heaven-sky-island-p3-16-final-risk-20261124` (from the candidate). Candidate `work/heaven-integration-20261124` stays at `2b182844` (remote verified); checkpoint `83e7593d` and `main` untouched. No PR, no merge. Isolated test worlds only. **Final verdict: CONDITIONAL.**

## 0. Baseline
Candidate `2b182844` == remote; contains checkpoint `83e7593d` (81 commits ahead). The checkpoint branch has since advanced to `c10762e6` (one CI-only commit touching 4 workflow files; `git merge-tree` against the candidate: no conflict, no overlapping file). Earlier record: GameTest 772 / 772, JUnit PASS, BUILD SUCCESSFUL.

## 1. World generation with ST-03 present (same conditions as P3.14)
Same probe, 8 island structures + `st03_pillager_headquarters` in the same worlds, seeds 42 / 1337, stages: single, reload (second start), two-stage with restart (seed 42). Block-entity dumps are taken after access (chunk-map counts and the faulty `inMap` flag not used).
| | P3.14 baseline | now (ST-03 in the tree) |
|---|---|---|
| seed 42 sky-island sites | 21 | **21** - all at identical coordinates |
| seed 1337 sky-island sites | 23 | **23** - all at identical coordinates |
| unresolved jigsaw | 0 | **0** (all stages, all targets incl. ST-03) |
| block-entity dump vs single stage | 100 % | **100 %** (reload 21/21 and 23/23, two-stage 21/21; ST-03: 3 + 4 sites, 960 / 1280 block entities, reload equal, two-stage equal) |
| box size vs P3.14 | - | equal 21/21 and 23/23 |
| block fingerprint vs P3.14 | - | equal 18/21 and 21/23; differing sites 1 400 - 3 700 blocks away from any ST-03 start, +-1..141 blocks (terrain / leaf noise of the size already seen in P3.11), so no ST-03 influence is indicated |
ST-03 and sky islands were generated in the same worlds (3 + 4 ST-03 starts); the nearest start-to-start distance was 179 - 205 blocks. Box intersection was not computed (not observed, not claimed absent). Differences to P3.14: none attributable to ST-03 or to other checkpoint changes. Files: `worldgen_results/`.

## 2. Baseline-test exception (`HeavenP38ResourceTest.liveWorldgenIsByteIdentical...`)
The candidate's exception removes two exact ST-03 paths from the comparison **without looking at their content** (and tolerates their absence). Mutation experiments (isolated edits injected one at a time into the real tree, JUnit run, edit reverted; `baseline_test_mutations/`):
| injected change | candidate test | patched test (this branch) |
|---|---|---|
| control (no change) | PASS | PASS |
| other theme pool (snowy/start.json) edited | FAIL (detected) | FAIL |
| approved heaven pool (meadow/start.json) edited | FAIL (detected) | - |
| new unapproved structure file | FAIL (detected) | - |
| new `st03_extra.json` (unapproved name) | FAIL (detected) | FAIL |
| ST-03 file content edited, harmless | PASS | PASS (intended) |
| ST-03 file edited to mention `sky_island_meadow` | **PASS - not detected** | **FAIL (detected)** |
| ST-03 file deleted | PASS | PASS (intended; ST-03 may change independently) |
Finding: the exception covered exactly the two approved paths (items 1, 3, 4 hold), but it did not constrain their content (item 2 weak). Minimal fix (committed here, test file only): exact paths, valid JSON, must not contain `sky_island` / `heaven`; everything else unchanged. Heaven-file detection is not weakened (only the ST-03 handling changed).

## 3. CI workflow `.github/workflows/sky-island-sizes.yml` (not modified)
* Origin: commits `610e3d6f` (introduced with the island-family overhaul, 2026-11-17) and `68c0d5bc`; branch `claude/sky-island-sizes-20261117` still exists on the remote (last commit "captures from run 4").
* Triggers: `push` to that one branch and `workflow_dispatch`. On the candidate / `main` it runs only by manual dispatch.
* Jobs: static generator checks; natural-generation probe on 3 seeds (90 min timeout each); probe check; full `runGameTest` (45 min); client capture (60 min, software GL) that **commits screenshots back to the sizes branch** (`permissions: contents: write`, `git push`).
* External actions: `checkout@v4`, `setup-python@v5`, `setup-java@v5`, `setup-gradle@v4`, `upload/download-artifact@v4` - the same tag-pinned set as the other 50+ workflows (no new dependency).
* Overlap: `static` and `worldgen-probe` duplicate the existing `sky-islands-validation.yml` (which also runs on pull requests touching data / scripts / gametest); `gametest` duplicates `build.yml`. Unique parts: tower-only-on-large-islands probe check, size-family counts, the capture / commit-back job.
* Present state on the candidate: its `static` job (`generate.py --check`, `validate.py`) FAILS until the scripts know the production transition (see 4). Its probe-check logic PASSES on the 3 seeds (family counts small 39 / medium 32 / large 44, towers only on large_d / large_e, 0 violations).
* Classification: **UNDETERMINED (evidence leans MODIFY)** - it is inert on the integrated line (branch-specific trigger, heavy, duplicates two other workflows) but contains unique checks and a commit-back step whose fate depends on whether the sizes branch stays alive; that is an owner decision. Suggested change if kept: drop the branch push trigger and the commit-back step / `contents: write`, keep dispatch.

## 4. Existing CI on the integrated line - two failures found and one pre-existing
`sky-islands-validation.yml` runs on every pull request that touches data / scripts / gametest, i.e. it WILL run on the integration PR. Run locally on the candidate:
| check | candidate | with the scripts patched on this branch |
|---|---|---|
| `generate.py --check` | **FAIL** - the 6 production pools differ from generator output (generator still emits vanilla locations; running `generate.py` would also silently undo the transition) | PASS (`HEAVEN_POOL_THEMES = (meadow, forest)` emit `sky_islands/heaven/<theme>/`) |
| `validate.py` | **FAIL** - treats the new `structure/sky_islands/heaven/` directory as a theme (FileNotFoundError) and expects the vanilla `tower` location in `tower_top` pools | PASS (heaven dir excluded; transitioned themes expect the heaven tower) |
| `test_tower_composite.py` | PASS (5 tests) | PASS |
| coverage job (3 CI seeds 2632028-30) | **FAIL**: `ruins_b` never generated in the union of the 3 seeds (seen ruins_a, c, d, temple; physical checks all pass; composite tower starts 10) | unchanged |
| same coverage check with the 6 pools reverted to vanilla (control, same seeds) | identical probe results (same variant sets) | -> **pre-existing / seed-dependent, not caused by the transition** |
Patches are on this branch only (`scripts/structures/sky_islands/generate.py`, `validate.py`); not applied to the candidate. The coverage failure needs an owner decision (seed set or expectation); it is independent of the heaven work.

## 5. Final integration integrity
* Authority numbering: `## 22`, `## 23` (accessory T2), `## 24` (heaven) unique; `### 24.1 - 24.6` present; index anchors `#24-...`, `#245-...` resolve to existing headings; one index anchor (`#20-2026-11-14-...`) points to no heading - **pre-existing on the checkpoint**, out of scope.
* Checkpoint features preserved: 0 files of Piglin / accessory / ST-01 / ST-03 / ST-05 / spider colony / sky-tower / trade / progression / sky-mob-model areas changed by the candidate; lang files: 0 checkpoint keys changed or removed, 23 heaven keys added; `ModRegistries` diff has no removed line.
* Unintended changes: 2 714 files differ from the checkpoint; all belong to heaven models / textures / blockstates (1 862), heaven structures (154 incl. `heaven_flora`, `heaven_prototype`), pools + pending (26), heaven code (6 new classes + `SkyIslandHabitat` + `ModRegistries`), tags / loot / recipes (34), lang (11), tests / probes (110), tools (32), docs (447), CI / build (2). Nothing outside these groups.
* ST-03 vs heaven registration / resources: no collision observed (worlds start, both structures generate, 772 GameTests incl. ST-03 tests passed on the candidate).
* Re-run after this branch's changes: `./gradlew test` (JUnit, includes the changed baseline test): BUILD SUCCESSFUL (1 min 45 s). GameTest not re-run: this branch changes only a JUnit test and two Python scripts (no product, resource or GameTest file); candidate result 772 / 772 stands.

## 6. Residual risks
1. Integration PR CI: `generate.py --check` and `validate.py` fail until the script patches (or equivalent) are applied; `coverage` (ruins_b) fails independently of the transition.
2. The baseline test exception on the candidate is weaker than intended (patched here).
3. `sky-island-sizes.yml` role undecided.
4. Not done: real PR CI run, other mods / data packs, ST-03 / sky-island box intersection, more than 2 seeds, GPU rendering; the checkpoint advanced by one CI-only commit (no conflict predicted).

## 7. Verdict: **CONDITIONAL**
No product regression or integration defect found: world generation, block entities, save / reload / restart, ST-03 co-existence, checkpoint feature preservation and numbering are intact. Conditions before the final integration approval: (a) adopt the script patches and the test-exception fix from this branch (or equivalent) into the integration candidate; (b) owner decision on the `ruins_b` coverage expectation / seeds; (c) owner decision on `sky-island-sizes.yml`.

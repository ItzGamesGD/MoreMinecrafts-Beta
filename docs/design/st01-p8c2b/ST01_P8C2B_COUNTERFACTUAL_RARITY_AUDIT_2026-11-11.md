# ST-01 P8-C2-B — Post-Integration Counterfactual Rarity Audit

Date: 2026-11-11 · Mode: read-only production audit + isolated diagnostic instrumentation · **No production policy was changed.**

Status: `AUDIT_STATUS: COMPLETE` (see §15 for the exact status block and its caveats).

Evidence: CI run <https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/42933161338> (22/22 jobs green) on commit `3ff2db10`.
Machine-readable data: [`counterfactual-audit/`](counterfactual-audit/) (`candidates.csv`, `candidates.json`, `summary.json`, `fluid_cells.json`, `tables.md`, `seeds.json`).

## 0. Headline

| | Eligible candidates (20 seeds, 8,388.608 km²) | Per 100 km² | vs production |
|---|---|---|---|
| **A** production (entrance ON, fluid ON) | 4 (real, generated) | 0.048 | 1.00× |
| **B** entrance OFF, fluid ON | 6 | 0.072 | 1.50× |
| **C** entrance ON, fluid OFF | 25 | 0.298 | 6.25× |
| **D** both OFF | 46 | 0.548 | 11.5× |

* **The fluid filter is the dominant cause of rarity; the entrance filter is minor.** Removing the entrance filter alone would add 2 candidates; removing the fluid filter alone would add 21; removing both gives all 46 terrain-qualified candidates.
* 40 of 46 terrain-qualified candidates (87 %) contain fluid in the inspected volume; 21 of 46 (46 %) fail the entrance test. The two filters are statistically unrelated in this data (φ = 0.10, Fisher p = 0.67). The production order hid 19 candidates that fail both.
* All fluid is **water** (107,429 blocks; no lava). The one-block margin is **never** the deciding factor (0 of 40 would pass with margin 0).
* B, C and D are **COUNTERFACTUAL_ELIGIBLE** (eligibility evaluation only). Nobody has generated or verified a pyramid for them. Only A is VERIFIED_GENERATED.
* Statistical precision is low (4 production starts, 20 seeds): see §10.

## 1. Verified baseline and dependency graph

| Reference | Verified |
|---|---|
| P8-C2-B final `9e4379c7`, last tested code `58646bef` | exist on `claude/st01-p8c2b-natural-worldgen-isauq8`; `58646bef` is an ancestor of `9e4379c7` |
| P8-C2-A final `c25f9d44` | ancestor of the P8-C2-B branch |
| Phase 4 `ada10ba3` | **not an ancestor**: a separate branch (PR #258) whose code P8-C2-B imported by adaptation, not by merge |
| P8-C2-B CI `42899510519` | reported in the earlier report; not re-opened here. The audit reproduces its numbers independently (§3) |

Graph: `c26ea0f8` (approved G0-R1 blueprint) → `be6272cf`/`c25f9d44` (P8-C2-A assembly) → `315c29d2`…`9e4379c7` (P8-C2-B natural worldgen, PR #262, Draft) → `3ff2db10` (this audit, new branch `audit/st01-p8c2b-counterfactual-rarity-20261111`, base `9e4379c7`).

Authority: `docs/design/st01-p8c2b/AUTHORITY-AMENDMENT-O14-MODERATE.md` (16/10/8 for worldgen terrain). The owner decision to retain the current entrance and fluid policies is taken from the task brief; this audit does not alter either.

## 2. Locked configuration (read from source, asserted by the analysis)

`analyze.py` parses `St01PlacementConfig.java` and aborts if any value differs: footprint half-width 40 (81×81), height variation ≤ 16, cut ≤ 10, skirt ≤ 8, entrance step-up 1, fluid margin 1, volume (−40..40, −32..−1, −40..40), spacing 32, separation 8, salt 91401. Triangular spread and the four-rotation / 50:50 route policies are production code that the probe calls unmodified.

## 3. Exact 20-seed dataset and baseline reproduction

Seeds (verbatim from `.github/workflows/st01-p8c2b-worldgen.yml` @ `58646bef`, also `counterfactual-audit/seeds.json`):
`1001, 1002, 1003, 1004, 1005, 527213223, 1034165336, 1541117449, 2048069562, 407538027, 914490140, 1421442253, 1928394366, 287862831, 794814944, 119599970, 218938829, 318277688, 417616547, 516955406`.
The two derived batches were re-checked against their stated formulas (`i·0x9E3779B1+20261110`, `j·0x85EBCA6B+20261111`, mod 2³¹). Search area per seed: square [−10240, 10240]² (the same 40×40 region grid → 1,600 potential anchors).

`compare_baseline.py` checks the recomputation against the previously **reported** values (they are not hard-coded in the analysis):

| Check | Reproduced | Reported |
|---|---|---|
| Potential anchors | 32,000 | 32,000 |
| Full Desert | 221 | 221 |
| Terrain-qualified | 46 | 46 |
| Entrance-qualified (sequential) | 25 | 25 |
| Complete (production eligible) = real server starts | 4 = 4 | 4 |
| Rejections Δh / cut / skirt / entrance / fluid | 160 / 5 / 10 / 21 / 21 | same |
| Four starts (seed, chunk, ground, orientation, route) | identical | — |
| Density | 0.0477 → 0.048 per 100 km² | 0.048 |

`BASELINE_REPRODUCTION: PASS`. No mismatch to investigate. Also checked: the probe's `actual_valid_starts` come from the real chunk generator's structure starts (4), equal to scenario A, and no start exists outside A.

## 4. Method

`St01CounterfactualProbe` (new, `src/gametest`, diagnostic only, inert without `-Dmm.st01.cf.out`) enumerates the exact P8-C2-B anchors. For every candidate it (1) calls the **unmodified** production `St01Candidate.decide`, and (2) records one immutable raw observation: the full 81×81 height grid, the 25 entrance-approach heights (production row), every fluid cell of the production inspection region (run-length per column, water/lava), the hypothetical route, and structure overlaps. Entrance and fluid are observed independently of each other and of the production stage order.

Same generator state as production: heights from `getBaseHeight(WORLD_SURFACE_WG)` and fluid from `getBaseColumn` (noise terrain before carvers, features and structures), over the same inspection region (x,z ∈ anchor ±41; y ∈ [yf−33, yf]). No later fully generated world state is substituted.

Two analysis paths share no code: `analyze.py` (records, scenarios, statistics, geometry classification) and `verify_independent.py` (SQL over `candidates.csv`, different Poisson-interval algorithm). `determinism.py` compares two independent server runs per seed.

Definitions: *obstruction* = entrance-row first-free-Y − foundation level `yf` (production fails when > 1); *excess* = obstruction − 1. *Units*: blocks, world coordinates (x, y, z), fluid y relative to `yf`. *Timing*: pre-carver noise terrain, at structure-start evaluation time.

## 5. 2×2 contingency (46 terrain-qualified candidates)

| | Fluid PASS | Fluid FAIL | Row |
|---|---|---|---|
| **Entrance PASS** | n11 = **4** | n10 = **21** | 25 |
| **Entrance FAIL** | n01 = **2** | n00 = **19** | 21 |
| Column | 6 | 40 | 46 |

Sum 46 ✓; n11 = 4, n11+n10 = 25, n10 = 21 all reproduce. **n01 and n00 are measured independently** (2 and 19), not inferred.

Scenario algebra (verified by SQL): A = n11 = 4; B = n11+n01 = 6; C = n11+n10 = 25; D = 46.

| Interaction | Value |
|---|---|
| P(fluid fail \| entrance pass) / \| entrance fail | 0.840 / 0.905 |
| P(entrance fail \| fluid pass) / \| fluid fail | 0.333 / 0.475 |
| Expected n00 if independent | 18.3 (observed 19) |
| Odds ratio, φ, Fisher exact p | 1.81, 0.096, 0.673 |

The filters act almost independently; the high fluid-fail rate makes the sequential production order hide most of the entrance filter's overlap.

## 6. Four-scenario comparison

| Scenario | Eligible | Per 100 km² | Poisson 95 % | Seed-cluster bootstrap 95 % | vs A | Abs. diff | Seeds with 0 | Mean / median per seed |
|---|---|---|---|---|---|---|---|---|
| A | 4 | 0.048 | 0.013–0.122 | 0.000–0.107 | 1.00 | 0 | 17 | 0.20 / 0 |
| B | 6 | 0.072 | 0.026–0.156 | 0.012–0.143 | 1.50 | +0.024 | 16 | 0.30 / 0 |
| C | 25 | 0.298 | 0.193–0.440 | 0.203–0.405 | 6.25 | +0.250 | 5 | 1.25 / 1 |
| D | 46 | 0.548 | 0.401–0.731 | 0.393–0.703 | 11.5 | +0.501 | 2 | 2.30 / 2 |

Denominator for every scenario: 20 seeds × 419.4304 km² = 8,388.608 km². Check: 4 / 8,388.608 × 100 = 0.0477 ✓ consistent with the reported 0.048. Paired bootstrap ratios (resamples with A > 0 only, 19,207 of 20,000): B/A 1–3, C/A 2.9–26, D/A 5–49; these are conditional on A > 0 and very wide.

For context only (different definitions and fixtures, not like-for-like): Phase 4 fixture 0.254, Phase 3.1 Mansion reference 0.715 per 100 km².

Per-seed table and all 46 candidates with coordinates: [`counterfactual-audit/tables.md`](counterfactual-audit/tables.md).

## 7. Entrance diagnostics (21 failing candidates)

* Max obstruction above walking level: values 2×7, 3×7, 4×2, 5×3, 7×2 (max **+7**). Excess over the 1-block threshold: **exactly 1: 7**; **2–4: 12**; **5–10: 2**; **> 10 (and so > the approved cut limit): 0**.
* Columns over the threshold per failing row: 4–25 of 25 (7 candidates have all 25 columns over).
* **Origin:** the approach row is authored z = +41, i.e. one block **outside** the 81×81 footprint for all 46 candidates (measured from the exported geometry), so the terrain-correction piece can never act on it. In all 21 failing candidates the in-footprint edge row (z = +40) rises 2–6 above `yf` as well: the obstruction is a terrain slope that continues from inside the footprint across the boundary, not an isolated outside feature.
* Diagnostic only: nothing was excavated and the entrance rule was not changed.

## 8. Fluid diagnostics (40 failing candidates)

* **Composition:** 107,429 fluid blocks, all water, 0 lava. 98,649 in the 81×32×81 volume, 8,780 in the one-block margin shell.
* **Size:** per-candidate in-volume blocks range 1 to 14,735; 8 candidates have ≤ 74 blocks.
* **Vertical distribution** (y relative to `yf`, volume −32…−1 plus margin −33 and 0): 8-block bands −40…−33: 1,670; −32…−25: 20,725; −24…−17: 38,474; −16…−9: 30,800; −8…−1: 15,760 (`tables.md` has the per-level histogram). Peak around −19.
* **Margin:** `would_pass_without_margin` = **0**. The margin never decides a rejection.

### Geometry classification (exact transformed authored grid per rotation/route)

Every in-volume cell was looked up in the authored P8-C2-A design (rotated with the exported unit vectors):

| Cell class | Fluid cells |
|---|---|
| Inside volume, authored **solid** block | 88,143 |
| Inside volume, authored **cavity (air)** | 10,506 |
| Inside volume, unwritten (structure void) | 0 |
| Margin ring (outside the authored frame) | 8,780 |

Candidates: 24 have water in authored cavity cells; 16 only in authored solid cells; 0 in unwritten cells; **0** have margin fluid face-adjacent to an authored cavity cell (no opening through the boundary touches outside water); 37 have fluid in outermost shell cells of the volume. Nothing was UNKNOWN.

Interpretation, as far as it can be supported: every in-volume cell is overwritten by the assembly, and no outside water touches an authored passable cell, so the full-volume rule rejects mainly on **inspection coverage** rather than on an observed boundary opening. This is a geometric statement, **not** a placement test: no counterfactual candidate was generated, so residual risks (e.g. water behaviour at placement, seams at chunk borders, aquifer interaction with the cut/skirt correction) are unmeasured. Fluid policy is not modified and no recommendation is made here.

## 9. Vanilla structure overlaps (diagnostic only)

14 of 46 terrain-qualified candidates overlap a vanilla structure box (mineshaft ×8, desert village ×7, plains village ×1 structure instances). By scenario, eligible-with-overlap: A 2 of 4, B 2 of 6, C 8 of 25, D 14 of 46. Cross-check: the probe's overlap detection for the two A candidates that P8-C2-B reported (seed 1004 mineshaft, 6 pieces; seed 1541117449 mineshaft, 5 pieces) matches. Fluid and height measurements are taken on noise terrain before any structure exists, so overlaps cannot alter them by construction (overlap rows list piece counts touching the fluid region and entrance columns for transparency). No exclusion, suppression or priority change was implemented, and overlap was not treated as a rejection criterion. Note the desert villages: if an overlap policy were ever adopted, it would remove several of the C/D candidates.

## 10. Statistical uncertainty and limitations

* 20 seeds, 4 production starts, 46 candidates: precision is low. A's Poisson interval is 0.013–0.122; its seed-cluster bootstrap interval touches 0 (17 of 20 seeds have no start).
* Candidates are not independent (clustered by seed and by shared terrain/biome regions); the Poisson interval assumes independence and is shown only as a reference. The cluster bootstrap (20,000 resamples, fixed RNG seed 20261111) is the preferred interval; with 20 clusters it is itself approximate.
* Ratio intervals are conditional on A > 0 in the resample and should be read as order-of-magnitude.
* B, C, D densities are **candidate-eligibility densities** and not generation densities. Other constraints (e.g. vanilla structure interaction, the final placement outcome) were not simulated.
* Single dimension (Overworld), radius 10,240 blocks per seed. Results describe these 20 seeds.
* The local full-seed run was lost to a container restart; the evidence is the CI run (two independent probe runs per seed, identical). A local two-run check on seed 1004 was also identical.

## 11. Production non-regression evidence

* `src/main` is byte-identical to `9e4379c7` (`nonregression.sh`: diff plus `git status`); the four NBT hashes are unchanged (`fe5afb19…`, `55cb285c…`, `7bdc1d09…`, `72f32c06…`); no bypass/switch/counterfactual token exists in production pyramid code; the probe exists only in `src/gametest`.
* Files changed outside production: `build.gradle` (one diagnostic run configuration, verified to be the only change), `src/gametest/resources/fabric.mod.json` (probe registration), the probe, tools, workflow and this documentation.
* CI job `nonregression` green: static checks, `export_assembly.py --check`, `./gradlew build` with all unit tests, and the natural-worldgen piece GameTests (normal/reverse/random chunk order).
* The audit's real-server production decisions (4 valid starts, identical coordinates/rotations/routes) equal the baseline.
* **Not re-run:** the full P8-C2-B natural generation/Guardian/reward probe (unchanged production code; its CI evidence stands), the full GameTest suite, and the ST-05 colosseum GameTests.

## 12. Independent calculation verification

`verify_independent.py` recomputed from `candidates.csv` alone (SQL over raw columns): 2×2 table, scenario counts (algebra and direct conjunction), per-seed counts, uniqueness (no duplicate (seed, chunk) records), production-verdict agreement with the logic implied by the raw observations (0 disagreements out of 221), area, densities, Poisson intervals and ratios: **31 checks PASS, 0 failures**. Determinism: observations from two server runs per seed identical for all 20 seeds. The CI analysis was re-checked locally from the downloaded artifact with the same result.

## 13. Reproduction

```
tools/st01-p8c2b-audit/run_audit.sh <seed> 10240 2   # per seed (JDK 25)
python3 -I tools/st01-p8c2b-audit/analyze.py results analysis
python3 -I tools/st01-p8c2b-audit/verify_independent.py analysis
python3 -I tools/st01-p8c2b-audit/compare_baseline.py analysis
python3 -I tools/st01-p8c2b-audit/determinism.py results
python3 -I tools/st01-p8c2b-audit/make_tables.py analysis
```
Raw per-seed observations (2 runs each) are the `st01-p8c2b-audit-seed-<seed>` CI artifacts (30-day retention); the analysis output is `st01-p8c2b-audit-analysis` (90 days) and committed here. Record schema: see `candidates.csv` header (`seed … eligible_D`); `candidates.json` adds fluid class, per-class cell counts and overlap rows.

## 14. Owner decision options for a future task (none implemented)

1. **Fluid policy** (dominant): keep as is; or narrow the inspected region/semantic (e.g. to what the placement can actually leave behind) — requires a placement test of counterfactual candidates first; or keep and accept ~0.05/100 km².
2. **Entrance policy** (minor, +2 candidates alone): keep; or relax the threshold; or allow approach shaping outside the footprint. Obstructions are modest (≤ +7, none above the cut limit).
3. **Overlap policy** (separate decision; 14 of 46 overlap, desert villages included).
4. **Rarity target:** which density is acceptable? Neither relaxing filter alone reaches the Mansion-class figure; relaxing both reaches 0.55 (candidate level).

## 15. Final status

```
AUDIT_STATUS: COMPLETE
BASELINE_REPRODUCTION: PASS
INDEPENDENT_ENTRANCE_EVALUATION: PASS
INDEPENDENT_FLUID_EVALUATION: PASS
COUNTERFACTUAL_MATRIX: PASS
STATISTICAL_VALIDATION: PASS   (limited precision; see §10)
PRODUCTION_NON_REGRESSION: PASS (see §11 for what was not re-run)
```

Recommended next experiment (needs owner go-ahead): generate the 21 fluid-only counterfactual candidates (scenario C) in an isolated test world with a diagnostic-only fluid relaxation, and run the P8-C2-A verifier plus a fluid-leak check on each, to find out whether any of them is actually unsafe. That would turn the geometric observation in §8 into evidence, and it is a prerequisite for any fluid-policy decision.

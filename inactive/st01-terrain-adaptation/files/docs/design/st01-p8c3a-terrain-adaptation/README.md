# ST-01 P8-C3-A — Policy B terrain adaptation research (2026-11-11)

**Documentation and evidence only. No decision, no implementation, no production code change.** Policy D (16 / 10 / 8, median plane) remains the current authority ([OWNER-DECISION-POLICY-D](../st01-p8c2c/OWNER-DECISION-POLICY-D.md)). The numbers 40 / 24 / 32 are the owner's design candidate, investigated here as requested.

* **Base:** `checkpoint/unified-20261111-candidate-2` (`11946339`) on `integration/unified-20261111`. That integration checkpoint is a **CANDIDATE**, not VERIFIED; see the integration manifest for its two pre-existing CI failures. The research is based on that integrated project, and its code tree is identical to `0743aa74`.
* **Isolation:** all experimental code ran in disposable worktrees (`/tmp/p3/wt`: probe; `/tmp/p3/wtb`: probe plus the three constants set to 40/24/32). The patches are archived under [probe/](probe/) as text; nothing experimental is committed to any source path.

## Verdict

**POLICY_B_FEASIBILITY: CONDITIONAL_GO.** The policy is internally consistent and was demonstrated on real servers. It raises eligible starts 46 → 172 (0.548 → 2.050 per 100 km²) with deterministic, chunk-order-independent generation. The owner gates G1–G7 in [09](09-implementation-feasibility-decision.md) must be decided first: limits, plane, entrance drop, edge treatment, overlap consequences at ×4.3, test contract and visual acceptance.

## Deliverables

| # | Document | Content |
|---|---|---|
| 1 | [01-baseline-and-policy-comparison.md](01-baseline-and-policy-comparison.md) | exact baseline reproduction; variants A / B / C / D; internal feasibility of 40 / 24 / 32 |
| 2 | [02-construction-plane-analysis.md](02-construction-plane-analysis.md) | median, mean, trimmed mean, percentiles, midrange, window-clamped median |
| 3 | [03-terrain-correction-architecture.md](03-terrain-correction-architecture.md) | excavation, foundation, edges and entrance, ravines and caves, fluids, masks |
| 4 | [04-structure-overlap-safety-matrix.md](04-structure-overlap-safety-matrix.md) | bounding-box and real-block intersections, damage categories, protective options |
| 5 | [05-coverage-simulation.md](05-coverage-simulation.md) | 20 seeds × 1,600 anchors, density, loss funnel |
| 6 | [06-search-and-worldgen-performance.md](06-search-and-worldgen-performance.md) | search latency A / B / C, height samples, main-thread blocking, correction cost, chunks |
| 7 | [07-determinism-and-chunk-safety.md](07-determinism-and-chunk-safety.md) | real-pipeline experiment under B: NORMAL / REVERSE / RANDOM / restart |
| 8 | [08-visual-and-geometric-evidence.md](08-visual-and-geometric-evidence.md) | cut/fill maps, true-scale sections, solid-face evidence |
| 9 | [09-implementation-feasibility-decision.md](09-implementation-feasibility-decision.md) | verdict, owner gates, status lines |

## Probe source and raw results

| Path | What |
|---|---|
| [probe/St01C3AProbe.java.txt](probe/St01C3AProbe.java.txt) | the probe (gametest source set; modes `coverage`, `search`, `correction`). The coverage and search runs used a build whose code is byte-identical to this file up to `correction()`; only `correction()` was extended afterwards, before any correction run |
| [probe/worktree-wiring.patch](probe/worktree-wiring.patch) | registers the probe as a main entrypoint in the gametest `fabric.mod.json` (worktree only) |
| [probe/policyB-experiment-worktree.patch](probe/policyB-experiment-worktree.patch) | the three-constant change used **only** in the disposable experiment worktree |
| [probe/run_coverage_and_search.sh](probe/run_coverage_and_search.sh), [probe/run_correction.sh](probe/run_correction.sh), [probe/run_policyB_pipeline_experiment.sh](probe/run_policyB_pipeline_experiment.sh), [probe/run_actual_generation_overlap.sh](probe/run_actual_generation_overlap.sh) | runners (use [../st01-p8d2-r1/probe/run_r1.sh](../st01-p8d2-r1/probe/run_r1.sh) and `tools/st01-p8c2b/run_validation.sh`; need JAVA_HOME = JDK 25) |
| [probe/analyze.py](probe/analyze.py), [probe/overlap_split.py](probe/overlap_split.py), [probe/outer_faces.py](probe/outer_faces.py), [probe/figures.py](probe/figures.py) | offline analysis; `python3 -I probe/analyze.py results/coverage results/search` regenerates [results/analysis.md](results/analysis.md) exactly |
| [results/coverage/](results/coverage) | 20 × `cov-<seed>.json`: every full-Desert anchor, all planes × both policies, overlap, and the 81×81 height grid (int16 LE, base64) for every B-relevant anchor |
| [results/search/](results/search) | 6 × `search-<seed>-r20.json` |
| [results/correction/](results/correction) | production `correct()` on real FULL chunks: cost, fluids, caves, vanilla blocks in the footprint (production world) |
| [results/actual-generation-overlap/](results/actual-generation-overlap) | the same scan after real Policy B generation (experiment worktree) |
| [results/policyB-pipeline-experiment/](results/policyB-pipeline-experiment) | `run_validation.sh` output for seeds 1004 and 1001 under B: phase JSONs, verifier result, world dumps, [summary.md](results/policyB-pipeline-experiment/summary.md) |
| [results/assembly-outer-faces.json](results/assembly-outer-faces.json) | outer-face composition of both route templates from Y−32 to Y0 |
| [figures/](figures) | PNG cut/fill maps and SVG sections |

## Reproduction

```
git worktree add --detach /tmp/p3/wt <this commit>
mkdir -p /tmp/p3/wt/src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgenprobe/st01c3a
cp probe/St01C3AProbe.java.txt /tmp/p3/wt/src/gametest/java/dev/itzgamesgd/moreminecrafts/worldgenprobe/st01c3a/St01C3AProbe.java
git -C /tmp/p3/wt apply $PWD/probe/worktree-wiring.patch
JAVA_HOME=<JDK 25> probe/run_coverage_and_search.sh        # ≈ 25 min on 4 vCPU
JAVA_HOME=<JDK 25> probe/run_correction.sh                 # ≈ 15 min
# experiment worktree: a second worktree + worktree-wiring.patch + policyB-experiment-worktree.patch, then
probe/run_policyB_pipeline_experiment.sh; probe/run_actual_generation_overlap.sh
```

The runners write to `/tmp/p3/out`; edit the `O=` and `W=` variables to relocate.

# 07 — Determinism and chunk safety

Status: **measured on real servers in a disposable worktree.** The production pipeline was used unchanged, with only the three `St01PlacementConfig` limits set to 40/24/32 ([probe/policyB-experiment-worktree.patch](probe/policyB-experiment-worktree.patch)). That change was **never committed** to any branch.

## Why B does not change the determinism argument

The correction piece reads only (a) the generator base height, a pure function of seed and position, and (b) the chunk's own blocks. It writes only inside the intersection of its 81×81 box with the chunk being post-processed. The assembly writes only owned cells. Policy B changes three integer comparisons in eligibility. It changes no input, no write region and no ordering. Each start still spans exactly 6 × 6 chunks (36; min = max over all 172 B starts), and nothing is written outside the footprint.

## Real-pipeline experiment (production code, limits 40/24/32)

Runner: [probe/run_policyB_pipeline_experiment.sh](probe/run_policyB_pipeline_experiment.sh), which calls `tools/st01-p8c2b/run_validation.sh <seed> 10240` exactly as the Policy D CI does. Its phases:

* **scan**: fresh world, staged prediction vs actual starts, FULL generation in NORMAL chunk order, completion handoff, Guardian/reward lifecycle, per-start hashes and world dumps;
* **reverse**: fresh world, anchors and chunks in REVERSE order;
* **random**: fresh world, seeded RANDOM order;
* **reload**: the scan world restarted.

The P8-C2-A verifier then checks every natural start. Results: [results/policyB-pipeline-experiment](results/policyB-pipeline-experiment).

| Check (21 starts: seed 1004 ×7, seed 1001 ×14) | Result |
|---|---|
| Predicted vs actual starts | **0 mismatches** (7/7, 14/14) |
| Anchor, orientation, route, canonical id identical across scan / reverse / random / reload | **21/21** |
| Owned-cell hash identical across all four phases | **21/21** |
| Completion-box hash identical across all four phases | **21/21** |
| Registered in every phase; reward state persisted exactly through restart | **21/21** |
| Owned-cell mismatches vs the exported templates | **0** on every start |
| Structure-void cells non-air / fluid inside the completion box | **0 / 0** on every start |
| P8-C2-A structural verifier failures | **none** |
| No premature registration; repeat completion idempotent | 21/21 |
| `moderate_limits_respected` (asserts the **16/10/8** limits) | false on the **14 B-only starts** (4 + 10), true on the 7 D-eligible starts. **Expected**: the assertion encodes Policy D. It is the only failing check, so the runner's overall status is FAIL. |

Per-start values are in [results/policyB-pipeline-experiment/summary.md](results/policyB-pipeline-experiment/summary.md): terrain, edge rise/drop, entrance approach and shell fluids.

**Interpretation.** Under Policy B the natural-generation pipeline is deterministic across chunk orders and restart, places every owned cell exactly, and keeps chunk writes inside the footprint, for every start in two seeds. A production adoption of B would have to update the `moderate_limits_respected` assertion together with the constants. That is test maintenance that follows an owner decision, not a defect.

## Chunk-border notes

* The excavation band is computed per column from the base height. Columns on either side of a chunk border therefore agree whichever chunk post-processes first. This is now verified under B by the reverse/random hash equality.
* Fluids: natural water on the outside ring is not touched by ST-01 generation, so any flow after generation is a vanilla fluid tick, outside generation determinism. No leak faces were found on 8 real targets ([03](03-terrain-correction-architecture.md)).
* Edge blending outside the footprint, if the owner ever approves it, would write into chunks that may already be decorated. It would need a new determinism argument: a larger piece box and blending that reads only seed-pure inputs. Not analysed further, because it is not approved.

## Not run

* The 18 other seeds of the matrix under B: **NOT RUN** in the real pipeline because of local time limits; the coverage probe covers them for eligibility only.
* Client-rendered captures under B: **NOT RUN** ([08](08-visual-and-geometric-evidence.md)).

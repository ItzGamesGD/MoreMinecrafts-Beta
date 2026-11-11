# ST-01 P8-D2-R1 — Structure-search algorithm audit and controlled re-benchmark

Documentation and evidence only; **no production code changed**. Base: P8-D2 branch `04a44ffa` (PR #270, unmerged; PR #270 not modified).
Disposable worktrees (`/tmp/r1/wtA`, `/tmp/r1/wtB`) held the instrumentation and probes; they are not part of this branch.

| File | Content |
|---|---|
| [PREVIOUS_ALGORITHM_RECONSTRUCTION.md](PREVIOUS_ALGORITHM_RECONSTRUCTION.md) | exactly what P8-D2 measured, which probe revision |
| [VANILLA_LOCATOR_CALL_GRAPH.md](VANILLA_LOCATOR_CALL_GRAPH.md) | bytecode-backed call graph with line references |
| [ST01_FILTER_EXECUTION_ORDER.md](ST01_FILTER_EXECUTION_ORDER.md) | the real order of ST-01's checks |
| [INSTRUMENTATION_RESULTS.md](INSTRUMENTATION_RESULTS.md) | per-stage counts and costs |
| [CONTROLLED_REBENCHMARK.md](CONTROLLED_REBENCHMARK.md) | reproduction, instrumented baseline, cheap-first locator |
| [CHEAP_FIRST_HYPOTHESIS.md](CHEAP_FIRST_HYPOTHESIS.md) | the owner's hypothesis, safety proof, 9,600-anchor equivalence test |
| [ROOT_CAUSE_ANALYSIS.md](ROOT_CAUSE_ANALYSIS.md) | H1–H6 |
| [OPTIMIZATION_DECISION_GATE.md](OPTIMIZATION_DECISION_GATE.md) | R1–R5 recommendation |
| [probe/](probe/), [evidence/](evidence/) | probe source (text), instrumentation patch, runners, raw JSON |

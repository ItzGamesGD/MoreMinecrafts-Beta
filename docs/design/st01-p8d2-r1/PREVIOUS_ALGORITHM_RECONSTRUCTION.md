# ST-01 P8-D2-R1 — Reconstruction of the algorithm P8-D2 actually benchmarked

## 1. Verified references
| Reference | Verified |
|---|---|
| P8-D2 PR #270, head `04a44ffa` (`research/st01-p8d2-explorer-map-feasibility-20261111`) | yes (branch tip); unmerged |
| P8-D1 PR #268, `a598054b` | yes; base of #270; unmerged |
| P8-C2-C PR #266, report `c376488d` | yes; base of #268; unmerged |
| Production ST-01 code at `04a44ffa` vs `c376488d` | **identical** (`git diff --stat c376488d 04a44ffa -- src` is empty) |
| `main` | `cde842dd`; contains none of the above |

## 2. Which probe code produced which benchmark
The P8-D2 benchmark numbers (`STRUCTURE_SEARCH_BENCHMARK.md`, `evidence/bench-*.json`) were all produced by `run_all.sh` **before** any probe patch was applied.
The later probe revisions (applied by `patch_probe.py`, `patch2.py` and three inline edits; archived in `probe/patches/`) changed only the `fixture` and `incr` modes:
`patch_probe.py` replaces text between the `fixture` reload branch and the `// --- bench` marker and inserts a helper *before* that marker; its other
replacements and all later edits target `incr`/`fixture` strings only. Therefore the `bench` method in the archived final source
(`docs/design/st01-p8d2/probe/St01D2Probe.java.txt`, method `bench`, lines 331-388) **is the code that ran**. Verified by reading the patch scripts (no replacement
touches lines of `bench`). Status: **PASS**.

## 3. The benchmarked call (exact)
`St01D2Probe.bench` (`St01D2Probe.java.txt:355`):
```java
BlockPos found = level.findNearestMapStructure(HolderSet.direct(st01), new BlockPos(ox, 64, oz), radius, /*skipKnownStructures*/ false);
```
- **API:** `ServerLevel.findNearestMapStructure(HolderSet<Structure>, BlockPos, int, boolean)` → `ChunkGenerator.findNearestMapStructure(ServerLevel, HolderSet, BlockPos, int, boolean)` (26.3 `ServerLevel` line 1403). No ST-01-specific search; **vanilla locator, used directly.**
- **Arguments:** structure set = the ST-01 holder only; origin `(0,64,0)` cold, then `(6000,64,−6000)`; radius 5/10/20/40 (and 100 for two seeds); `skipKnownStructures = false`.
- **Radius interpretation:** structure-placement regions (verified from bytecode and counts; `VANILLA_LOCATOR_CALL_GRAPH.md`).
- **Origin used by vanilla:** `SectionPos.blockToSectionCoord(x|z)` = the origin's **chunk** coordinates (ChunkGenerator L207-208).
- **Validation path:** vanilla's own (`checkStructurePresence` → ST-01 `findGenerationPoint`; then `getChunk(STRUCTURE_STARTS)` → `isValid()`), i.e. **ST-01's production eligibility was used**, unmodified.
- **Threads:** the call ran on the server thread inside `SERVER_STARTED`; chunk generation for `STRUCTURE_STARTS` ran on worldgen workers while the server thread waited (`managedBlock`).
- **Caches:** fresh world per (seed, radius); in-process caches (StructureCheck `featureChecks`/`loadedChunks`, chunk holders) start empty; the "later origin" and "warm repeat" rows share them.
- **Termination:** first ring that yields a valid start (or radius exhausted).
- **Measured fields:** wall time (= server-thread blocked time, synchronous), calling-thread CPU, heap peaks, GC, the production counter `St01PyramidStructure.VERDICT_COUNTS` (counts *every* `findGenerationPoint` call, on any thread), loaded-chunk delta.

## 4. A measurement-interpretation correction for P8-D2
P8-D2 reported "ST-01 start evaluations" from `VERDICT_COUNTS` as if each were a distinct candidate. They are not: the counter is incremented by **both** evaluations of a successful candidate (once in `StructureCheck`, once again in the chunk pipeline), so for a found result it overstates distinct candidates by one, and the reported per-evaluation averages mixed main-thread and worker work. This does not change the reported wall times. Details: `INSTRUMENTATION_RESULTS.md`.

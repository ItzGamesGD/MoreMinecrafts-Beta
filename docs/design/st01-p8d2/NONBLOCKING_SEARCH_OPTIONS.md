# ST-01 P8-D2 — Non-blocking structure search: constraints and options (evidence-backed)

**Verdict: PARTIAL, leaning negative.** A safe *fully* non-blocking search was **not demonstrated**. The one variant that returned correct results with tiny per-tick steps (N3b) showed tick gaps up to ≈ 0.9 s on a 4-vCPU container, was several times slower than the blocking call on the small case, and **did not finish within a 1,500 s budget on the one larger case (1001, radius 20, nearest pyramid at ring 14)**. Nothing unsafe was built or recommended.

## 1. Constraints read from the 26.3 code (`javap -c`)
| Question | Finding | Evidence |
|---|---|---|
| Is the locator thread-safe? | **No.** `StructureCheck` keeps `Long2ObjectOpenHashMap loadedChunks` and `Map featureChecks` (fastutil / plain maps, no synchronization) and `StructureManager`/`ChunkAccess.structureReferences` are mutated by the locator (`addReference`). It is designed for the server thread. | field types of `StructureCheck`, `StructureStart.addReference` |
| Does it touch mutable world/chunk state? | Yes: chunk starts and references, `StructureCheck` caches, the chunk system (tickets, holders). | `getNearestGeneratedStructure` bytecode |
| Does it need the main thread? | Called from a worker, `ServerChunkCache.getChunk(x,z,status,load)` does `supplyAsync(() -> getChunk(...), mainThreadProcessor).join()`: the work **runs on the main thread and the worker only blocks**. So "moving the call to a worker" gives no parallelism and still stalls the tick. | `ServerChunkCache.getChunk` bytecode |
| Is there an async chunk request? | Yes: `ServerChunkCache.getChunkFuture(x,z,status,load)`. **On the main thread it calls `getChunkFutureMainThread` and then `mainThreadProcessor.managedBlock(future::isDone)` — it blocks** (helping with other main-thread tasks). From a **non-main** thread it returns `supplyAsync(main).thenCompose(...)`, a future that does not block the caller. | bytecode of `getChunkFuture` |
| What keeps the requested chunk alive? | `getChunkFutureMainThread` adds a `Ticket(TicketType.UNKNOWN, level)` — a **short-lived** ticket. | bytecode of `getChunkFutureMainThread` |
| Where is the expensive work? | `StructureManager.checkStructurePresence` → `StructureCheck.checkStart` → `canCreateStructure` → the structure's own `findValidGenerationPoint` (for ST-01: the 81×81 Desert sweep and 6,561 height samples) executes **on the calling thread** for every candidate; `START_NOT_PRESENT` for 99.8 % of candidates (839 / 841 in the 1001 r20 run). The same evaluation also runs in the chunk pipeline's `STRUCTURE_STARTS` step on worldgen workers. | `StructureCheck` bytecode; probe counters |
| Can results be precomputed from immutable data only? | The structure start depends on the terrain generator, biome source and seed, which a worker cannot read safely outside the chunk pipeline; the supported way to compute it off-thread is the chunk pipeline itself. A pure re-implementation of ST-01 eligibility over a private noise sampler is not safe to assume (it would duplicate production logic and the generator state). | design analysis; not built |
| Cancellation? | The vanilla call has none. Futures can be dropped (and tickets removed), but generation already scheduled is not cancelled. | API surface |

## 2. Experiments (probe `St01D2Probe`, mode `incr`; same seeds/origins as the benchmark; **n = 1–3 runs per row**, one container)
Search = ring-ordered candidate walk identical in spirit to vanilla, per-tick budget 5 ms, polled each `END_SERVER_TICK`, compared with the vanilla result of the same call.

| Variant | Setup | Result correctness | Server-thread behavior | Notes |
|---|---|---|---|---|
| **N1** vanilla synchronous | `findNearestMapStructure` | correct | blocked 6–28 s (benchmark) | baseline |
| **N2** incremental on the main thread, `checkStructurePresence` per candidate, 5 ms budget | 6 runs (1004, 1001, 1003 × radius 5, 20) | correct in all 6 (matches vanilla or finds a different valid start; "none" where vanilla finds none) | budget **cannot hold**: max single step **5,845 / 5,941 / 5,940 / 2,999 ms** in the 4 runs that reached a Desert candidate (218 ms / 158 ms in the two that did not); total up to 39 s | one candidate evaluation can cost seconds on the main thread |
| **N3a** worker-thread `getChunkFuture`, no pre-check, no ticket | 6 runs: 1004, 1001, 1003 × r5, r20 (+ 2 diagnostic reruns with retries) | **wrong**: 0 pyramids found in the 3 runs where vanilla finds one (1004 r5, 1004 r20, 1001 r20); 22 % of requests (r5, 27 of 121) and 72–84 % of request attempts (r20, with up to 8 retries per candidate) returned `ERROR: Unloaded chunk`; retries did not help | **bounded**: max step 0.6–12.6 ms, max inter-tick gap 54–76 ms | the `UNKNOWN` ticket expires before the long (Desert) generation finishes, so exactly the expensive candidates are dropped — an unsafe approach |
| **N3b** worker-thread `getChunkFuture` + `TicketType.FORCED` ticket at the `STRUCTURE_STARTS` level (removed on completion) | 1004 r5, r20 completed; **1001 r20 INCOMPLETE** (§2.1) | **correct**: same chunk (−111,168) as vanilla; 0 request errors | max step ≤ 4.2 ms, but **max inter-tick gap 798 ms and 908 ms**; total 27.5 s and 29.7 s (vs ≈ 6 s blocking for the same 121 candidates) | worker generation competed with the tick on 4 vCPU; cause of the gaps not isolated |
| **N5** "prepare then vanilla" | after N2 or N3b, call vanilla for the same inputs | correct | the vanilla call then took **0.5–1.8 ms** (N2) and **0.6–0.7 ms** (N3b) | the prepared caches are the ones vanilla reads; with N3a it stayed cold (3 s / 21.8 s) because nothing was computed |

### 2.1 Incomplete run
`1001 r20` with N3b was started with the same settings and **hit the 1,500 s time budget without producing a result** (the process was stopped by the budget; no partial output). It was not retried: the cause (ticket churn, worker saturation of the 4 vCPUs, or per-candidate cost under tickets) was not isolated, so N3b's behavior for a nearest pyramid at ring ≥ 14 is **UNVERIFIED**. Both completed N3b runs had the pyramid at ring 5 (121 candidates).

## 3. The five approaches
| | N1 vanilla sync, strict bounds | N2 bounded incremental (main thread) | N3 async preparation + main-thread integration | N4 precomputed / persistent location cache | N5 hybrid |
|---|---|---|---|---|---|
| Technical feasibility | trivial; bound = a **small radius** | works but cannot bound a tick (§2) | demonstrated **only with an explicit ticket** (N3b); the naive version is incorrect (N3a) | feasible (a `SavedData` of valid starts) but the *fill* is N3 or lazy | N3b-style preparation, then a vanilla call that is a cache hit |
| Correctness risk | none (vanilla) | low; ring/"nearest" semantics duplicated | **dropped candidates** unless tickets are managed; must treat any error as "not decided" | stale entries if eligibility/placement changes (version-stamp it) | as N3 |
| Server-thread impact | seconds per cold call | per-step spikes up to ≈ 6 s | small steps, but gaps up to ≈ 0.9 s observed | none at lookup (0.2 ms) | none at lookup once prepared |
| Complexity | none | medium | high (tickets, futures, backpressure, errors) | medium–high | highest |
| Persistence | vanilla chunk data | none | none | new world data | cache optional |
| Cache invalidation | n/a | n/a | n/a | seed fixed; invalidate on structure-set/eligibility/version change; negative results per area need a lifetime | same |
| Unexplored chunks | yes (generates `STRUCTURE_STARTS`) | yes | yes | needs the prepare step to cover them | yes |
| Multiplayer | each call blocks everyone | same | requests can be queued/de-duplicated | shared by all | shared |

## 4. Safe and unsafe approaches (summary)
- **Unsafe / rejected:** calling the vanilla locator from a worker thread (it re-enters the main thread and blocks the worker; the shared caches are not thread-safe); naive worker-dispatched `getChunkFuture` without a ticket (silently drops expensive candidates — measured wrong results).
- **Safe but blocking:** the vanilla call with a small, owner-set radius (r=5 ≈ 0.1–6 s, finds a pyramid in 1/6 of the test seeds).
- **Demonstrated, with caveats:** worker-dispatched futures with an explicit `FORCED` ticket (correct, step times tiny, but tick gaps up to ≈ 0.9 s and slow). It needs hardening and a fresh measurement on target hardware before any commitment.
- **Not attempted:** a private re-implementation of the eligibility test on a worker; reflection into private chunk-system methods; any mixin into the locator.

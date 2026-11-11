# ST-01 P8-D1 — Architecture comparison (implementation alternatives, not gameplay chains)

A = vanilla-first, B = dedicated ST-01 location service, C = hybrid (vanilla mechanisms plus minimal custom logic). None of these is a
bounty-then-villager sequence: all three keep the channels independent.

| Criterion | A Vanilla-first | B Custom service | C Hybrid |
|---|---|---|---|
| What it is | Channel B: data-driven `exploration_map` trade; Channel A: new contract objective using `findNearestMapStructure` synchronously | One service enumerates candidates and resolves starts for both channels; maps/clues built from its output | Channel B explorer map via the vanilla data path; Channel A and any clue items use a thin non-blocking service (`getChunkFuture`) built on the same engine calls |
| Implementation complexity | lowest for B, medium for A | highest (new service, own caches, own item/clue) | medium |
| Source compatibility | uses only vanilla data format for B; A reuses the already-used `findNearestMapStructure` | depends on engine internals (`StructureManager`, `ChunkGenerator`, chunk futures) | same engine calls as B for the small piece it adds |
| Search correctness | locator accepts only valid starts (bytecode + probe 3/3); "nearest" is ring-approximate | same if it validates `isValid()` (must be implemented and tested) | same |
| Performance | **sync search on the server thread**: measured 6–9 s cold at radius 20; radius 100 unmeasured; applies at villager offer generation *and* contract resolution | can be budgeted/async | A's problem remains for the map trade unless its `search_radius` is small; the service removes it for Channel A |
| Server-thread safety | poor for cold calls | good if budgeted | good for A-side; B-side bounded by radius only |
| Multiplayer | per-villager target, no per-player copies (vanilla) | explicit | as A for villagers, explicit for contracts |
| Persistence | villager offer item (vanilla); contract needs new fields | contract/clue item stores result; service itself is stateless | as A + B |
| Testability | vanilla path testable by GameTest/probe; A-side needs a real structure in the test world (the existing contract test generates a vanilla start manually) | full control, more to test | medium |
| Existing-system reuse | high | medium | high |
| Maintenance burden | low | high | medium |
| Future structure support | other structures get the same vanilla trade for free | one service for all structures if generalized | service generalizes; vanilla trades stay data |
| Main risk | cold-call stalls; no control of icon/precision | over-building before the owner picks the format | two code paths |

## Recommendation: C (hybrid)
- Channel B starts as a vanilla data-driven explorer-map trade (smallest change, vanilla persistence/failure rule), after a short feasibility probe proves the additive tag/trade and a small bounded `search_radius`.
- Channel A needs a lead that the vanilla contract model cannot express and must not block the tick, so it uses a thin `St01LocationService` (non-blocking, §SHARED_LOCATION_ARCHITECTURE.4).
- The same service can later back a clue item (Channel B2) or replace the map trade's synchronous call if the cold-call cost proves unacceptable.
Evidence: the measured 6–9 s cold server-thread search (probe), the existing contract model's lack of position/level context (audit F-5), the engine's non-blocking `getChunkFuture`, and the data-driven trade format. The strongest remaining uncertainty is the real cost/throughput of `getChunkFuture` under load and the cost at radius 100, both **unmeasured**.

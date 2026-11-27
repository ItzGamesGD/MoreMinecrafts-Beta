# ST-01 P8-D1 — Proposed implementation roadmap and test matrix (not authorized)

Implementation authorization: NONE. Each phase needs owner approval of its decisions first (OWNER_DECISIONS_REQUIRED.md).

## Phases
| Phase | Content | Depends on | Output |
|---|---|---|---|
| P8-D2a Feasibility benchmark | Reproducible server probe (extend the P8-D1 probe): cold/warm locate at radius 5/10/20/50/100 over the 20 audit seeds; `getChunkFuture` throughput and chunk retention; additive-tag/data-trade merge check; custom structure tag + `exploration_map` trade in a throwaway datapack | D1 | numbers for radius caps and the budget |
| P8-D2b Channel B | structure tag, trade JSON + tag entry, map item/decoration, caps; GameTests for trade availability and stability | D1, D2, D4, D4b, D4c, 2a | villager discovery |
| P8-D2c Channel A | `St01LocationService`, objective extension (`optionalFieldOf`), resolver on accept/tick, UI text/translation, overlap-safe entry detection | D1, D3, D4, D7–D9, 2a | bounty discovery |
| P8-D2d Integration & balance | cross-channel tests, tuning of probabilities/prices/caps, rarity target | D2b, D2c | tuned release |
Phases 2b and 2c are independent and may run in either order; neither depends on the other.

## Benchmark proposal (reproducible; no numbers invented here)
Per seed in `tools/st01-p8c2b-audit/seeds.json` (the 20 audit seeds, radius 10,240), for each of N = 20 origins on a fixed grid: record, on a fresh world and a warm world,
wall time of `findNearestMapStructure` at radius {5,10,20,50,100}; ticks blocked; result chunk and whether it is a valid start; compare to the enumerated truth. Report median/p95/max on the CI runner
class used by the repo and on a developer machine. Pass criteria are set by the owner (e.g. an acceptable tick budget), not here.

## Future test matrix
**Location:** known generated pyramid; buried pyramid (the 21 buried-entrance P8-C2-C starts are ready fixtures); each of four rotations and both routes; no pyramid in range; several candidates (nearest ordering); unexplored chunks (cold) versus cached; a result is a valid start, never a potential anchor.
**Bounty (Channel A):** acquisition from each source; independent target assignment (no villager map involved); objective completion by entry after acceptance; overlap case (ST-01 box intersecting a village/mineshaft box: credit still granted); duplicate-completion prevention (existing ledger); contract transfer; death, logout, restart persistence; no-target failure path; deferred resolution does not block the tick.
**Villager (Channel B):** trade appears at the chosen profession/level; the map points at a valid start; target stable across restart and repeated UI opens; two players, one villager → same target; trade hidden when nothing found; `max_uses` cap; `normalizeTrades` unaffected.
**Cross-channel independence:** bounty-only player; villager-only player; both active; same target from both; different targets; neither channel reads the other's state (assert in code and test).
**Performance:** bounded search under the budget; repeated requests hit the cache; concurrent players; restart; memory/chunk retention after many requests.
Read-only probes needing no gameplay change may run now; everything else waits for approval.

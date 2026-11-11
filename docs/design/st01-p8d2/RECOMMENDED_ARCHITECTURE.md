# ST-01 P8-D2 — Recommended shared architecture (proposal; not approved, not implemented)

Applies Architecture 3 (hybrid): vanilla wherever it is correct; custom code only where the evidence shows a gap.

## 1. What the evidence says vanilla already does correctly (reuse as is)
| Need | Vanilla facility | Verified |
|---|---|---|
| Name the target set | structure tag (`data/<ns>/tags/worldgen/structure/…`) containing `moreminecrafts:st01_pyramid` | PASS (fixture) |
| Find a real pyramid | `findNearestMapStructure` (inside `ExplorationMapFunction` and villager trades) — returns only valid starts, chunk corner at Y 0 (11.3 blocks from the anchor, inside the box) | PASS (116/116 valid; map targets match enumerated starts) |
| Create the map and its icon | `ExplorationMapFunction` (+ existing decoration `target_x` / `desert_pyramid`): no new map-rendering system, no new asset | PASS at data level; client rendering UNVERIFIED |
| Offer it in trades | additive `villager_trade` entry in `#minecraft:cartographer/level_N` (vanilla trades preserved) | PASS (availability probabilistic) |
| Create it outside trading | the same function with any `LootContext` carrying `ORIGIN` | PASS |
| Persist it | map item + `map_id` + saved map data (restart check: see `CARTOGRAPHER_TRADE_FEASIBILITY.md` §5) | PASS (§5) |

## 2. Where vanilla is not enough (the only justified custom parts)
1. **Server-thread cost.** A cold call blocks the server thread for seconds (benchmark: median 6.3 s at radius 10, 10.7 s at 20, up to 27.7 s). Vanilla offers no budget, hook or cancel.
2. **Supply semantics.** `skip_existing_chunks` (default true) turns each pyramid into a one-time resource (3 pyramids → 3 maps → trade vanishes); `false` removes the limit but gives everyone the nearest pyramid.
3. **Overlap-safe visit detection** for contracts (the existing detector can miss ST-01).
4. **Contract-side durability** if a map is issued by the bounty system (idempotence with the contract ledger).
5. **Guaranteed availability** (optional) because an additive tag entry is only drawn with ≈ 22–39 % probability.

## 3. Recommended structure
```
data (vanilla format):  structure tag  moreminecrafts:on_st01_pyramid_maps
                        villager_trade JSON (B1)       -> uses ExplorationMapFunction
                        [optional] loot table for A4   -> uses ExplorationMapFunction
code (minimal):         St01MapLocator (server, main thread, no gameplay state)
                          - prepare(level, originXZ, radius, budget): background "warm-up" of the vanilla caches for the candidate chunks (N5)
                          - is_prepared(level, originXZ, radius): so a player-facing map creation only runs when the vanilla call is a cache hit (~0.2 ms)
                          - negative/in-flight cache, per-origin key, version-stamped, bounded memory
                        ContractObjectives fix: per-structure lookup (independent of A4/B1)
                        A4 glue (only the chosen acquisition): idempotent issuance bound to the contract id
```
- **No dedicated location "service" is required for correctness** (vanilla's locator is correct); what is required is a **preparation/guard layer** around it. It must be shared by both channels *only as infrastructure* and must never read the other channel's state.
- The preparation layer's technique is **not settled**: N3b (explicit `FORCED` ticket at `STRUCTURE_STARTS`) was correct in the two completed runs but showed tick gaps up to ≈ 0.9 s, was slower than blocking, and did not finish on the larger case; N2 cannot bound a tick; N1 with a small radius is safe but finds a pyramid in only 1 of 6 seeds at radius 5. The first implementation phase must therefore start with a **hardening benchmark**, not with gameplay.
- **Independence is preserved by construction:** both channels call the same vanilla function with the same tag; neither stores or reads a flag that the other sets; a map from one channel is just an item to the other. The only shared *resource* is the pyramid supply, and only if the owner chooses `skip_existing_chunks=true`.

## 4. Recommended policy defaults (for owner confirmation)
- `skip_existing_chunks: false` for both channels (no consumption, no competition, no exhaustion), accepting repeat targets.
- A search radius chosen **per channel from a benchmark on the target hardware**; the measured data say: radius 5 (≈ 2.6 km) is cheap but rarely finds a pyramid; ≥ 21 regions (≈ 11 km) were needed to find one in all six test seeds.
- Cap offers with `max_uses` and a conservative level; a map must not promise an entrance (Policy D allows buried entrances): the marker is a region lead (target within 11.3 blocks of the anchor).

## 5. Verified versus unverified
| Claim | Status |
|---|---|
| Vanilla map mechanism can target real ST-01 starts | **PASS** |
| Additive cartographer entry works, vanilla preserved | **PASS** (availability PARTIAL) |
| Search cost data | **PASS** (one container; indicative) |
| Safe non-blocking search | **PARTIAL, leaning negative** (N3b correct on a 121-candidate case with gaps ≈ 0.9 s; did not finish on a 736+-candidate case within 1,500 s) |
| Contract detector overlap fix feasible | **PASS** (specified; not implemented) |
| XP quest compatibility | **PASS** (audited) |
| Client rendering of the map/icon | **UNVERIFIED** |
| Restart persistence of an offer/map | **PASS** (one villager, seed 1004; map id, decoration and saved map data identical) |
| Multiplayer concurrency | **UNVERIFIED** (not tested) |

## 6. Proposed implementation boundaries (not authorized)
- **P8-D3 — Channel B1 (villager):** structure tag, additive trade (radius/level/price/`max_uses`/skip policy from the owner), the preparation/guard layer sized by a hardening benchmark, GameTests for availability, vanilla preservation, restart and failure behavior. Acceptance includes an owner-set tick-gap budget on target hardware. No bounty code.
- **P8-D4 — Channel A4 (bounty) and the detector fix:** the overlap-safe detector change **with its regression test first** (it is a correctness bug for any ST-01 exploration objective and is independent of A4/B1), then the owner-selected A4 acquisition option (`BOUNTY_MAP_INTEGRATION.md`) reusing the same map function and guard. No villager code.
D3 and D4 are independent and may be done in either order; neither may be a prerequisite of the other.

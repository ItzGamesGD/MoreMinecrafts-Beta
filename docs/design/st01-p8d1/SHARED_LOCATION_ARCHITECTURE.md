# ST-01 P8-D1 — Shared structure-location architecture (PROPOSAL; probe evidence is measured)

Both channels need "where is a real ST-01 Pyramid near here?". A shared **server-side** location service avoids two copies of that logic.
Sharing the *service* creates no shared *gameplay* prerequisite: it is a pure query; neither channel reads the other's state.

## 1. Concepts that must not be conflated

| State | Meaning | How it can be known | Cost / availability |
|---|---|---|---|
| Potential anchor | a candidate chunk of the structure set | `RandomSpreadStructurePlacement.getPotentialStructureChunk(seed, rx·32, rz·32)` — arithmetic, no world access | trivial |
| Eligible candidate | passes Desert/terrain at that chunk | needs `findGenerationPoint` (81×81 Desert sweep + 6,561 height samples) | expensive, only after a cheap biome pre-filter |
| **Valid start** | `StructureStart.isValid()` in the chunk at `STRUCTURE_STARTS` | `getChunk(x,z,STRUCTURE_STARTS).getStartForStructure(st01)` | cached after first computation (probe: 0.008 ms) |
| Generated pyramid | all chunks of its box generated, blocks placed | `St01PyramidCompletion.data(level).entry(startChunk)`, state REGISTERED (`St01PyramidCompletion.java:207`) | only once the box is generated |
| Visited | a player entered it | **no world state exists**; only per-contract `visited` lists (audit §2 #7) | absent |
| Guardian defeated / chest sealed | encounter state | `TombGuardianEncounters.isRoyalChestSealed(level, anchor)` (`boss/TombGuardianEncounters.java:65`); needs the registered instance's anchor | after generation |
| Reward generated | PRIMARY chest contents exist | `RoyalChestData.isGenerated(id, PRIMARY)` (`pyramid/RoyalChestData.java:104`) | after generation |
| Reward claimed | items taken | **not tracked** as a flag (chest contents only) — UNVERIFIED | absent |

An *eligible candidate is not a generated structure*, and a *valid start is not a generated structure either*: the start exists as data at
`STRUCTURE_STARTS`; the blocks appear only when its chunks reach FULL. A location result must say which of these it is. A tool may
report a **valid start** as "pyramid exists at this position" because production determinism makes the start authoritative (P8-C2-C:
46/46 predicted starts became real, complete pyramids, identical across chunk orders and restart) — but it must not report a *potential* or
*eligible-only* position as a structure.

## 2. Measured behavior of the vanilla locator (probe, seed 1004, radius parameter 20; raw data in `probe/`)

| Origin | Result | Chunk is an actual valid start? | True nearest (enumerated ±10,240) | Cold time | Repeat |
|---|---|---|---|---|---|
| (0,0) | (-1776, 0, 2688) | **yes** (-111,168) | same | 6,216 ms | 0.23 ms |
| (-8000,8000) | (-13088, 0, 5328) | outside the enumerated area (|x|>10,240): not verifiable by this probe | the enumerated start (-146,522) is 5.68 km away, the returned one 5.75 km | 6,113 ms | 0.27 ms |
| (4000,-4000) | (-1776, 0, 2688) | **yes** | same (8.84 km) | 9,137 ms | 0.32 ms |
| (-1700,2600) | (-1776, 0, 2688) | **yes** | same (0.12 km) | 0.008 ms (already cached) | 0.004 ms |

Reading, with limits (one seed, one container, single cold runs, no warm-up control):
1. The locator returned only real valid starts where it could be checked (3 of 3), consistent with the bytecode (it requires `isValid()`).
2. It returns the **chunk's minimum corner at Y=0**, i.e. (anchor − 8, 0, anchor − 8): a pyramid anchor is the chunk centre and the ground Y is elsewhere. Good enough for a region lead; not an entrance.
3. "Nearest" is ring-order approximate, not exact (second row).
4. A cold search at radius 20 (≈ 10 km) blocked the server thread for **6–9 s** here (≈ 120–180 ticks); a repeat or a nearby origin costs well under a millisecond because the start/reference is cached.
5. Radius parameter units are structure-placement regions (20 regions of 32 chunks ≈ 10 km). The vanilla explorer-map value is 100 (≈ 51 km): **not measured**; expect it to be far worse than radius 20 in a Desert-poor area and must not be assumed safe.
6. Corroborating measurement from the P8-C2-C CI evidence: evaluating all 1,600 anchors of a 20.48 km square (`scan_ms`, includes the `STRUCTURE_STARTS` requests, shared CI runners) took 10.6–204 s per seed (median ≈ 64 s) — dominated by the few Desert candidates; 99.3 % of anchors are rejected on biome.

## 3. Engine facilities usable for a non-blocking design (bytecode of 26.3; behavior in practice UNVERIFIED)
- `StructureManager.checkStructurePresence(chunk, structure, placement, skipKnown)` → START_PRESENT / START_NOT_PRESENT / CHUNK_LOAD_NEEDED; its implementation calls `StructureCheck.canCreateStructure` first (biome pre-filter), so non-Desert candidates cost a lookup.
- `ServerChunkCache.getChunkFuture(x, z, ChunkStatus.STRUCTURE_STARTS, load)` returns a `CompletableFuture<ChunkResult<ChunkAccess>>`: an asynchronous request for exactly the step the locator performs synchronously. Whether the resulting chunk is retained (tickets/memory) and its throughput under many requests are **not measured**.
- Persistent knowledge: the start is saved in chunk data once computed; the world seed, structure set and Policy D are fixed, so a valid start is stable across restarts (P8-C2-C: restart reload identical).

## 4. Proposed service (Architecture C, §ARCHITECTURE_COMPARISON): `St01LocationService`
A thin Overworld-only server service; no gameplay logic, no player state.

```
request(level, originXZ, maxRegions, budget) -> Ticket
poll(Ticket) -> PENDING | FOUND{startChunk, anchorXZ, state flags} | NONE_IN_RANGE | UNAVAILABLE
```
- **Enumeration:** ring order over regions via `getPotentialStructureChunk` (pure math), exactly like vanilla, but resumable.
- **Per candidate:** (1) `checkStructurePresence` (cheap, includes biome); (2) only for CHUNK_LOAD_NEEDED, issue `getChunkFuture(..., STRUCTURE_STARTS, true)`; (3) accept only `StructureStart.isValid()`.
- **Budget:** a hard cap on candidates started per tick and on in-flight futures; a cap on total regions (`maxRegions`, e.g. a small multiple, an owner/engineering value to be set from a benchmark, not guessed here); a cancel on disconnect.
- **Cache:** an in-memory map (level, start chunk) → result, rebuilt from the world on demand (no new persistent state needed because the start is in chunk data); a negative cache per (region, query) with an explicit lifetime is optional and **not recommended initially** (stale-world risk is nil, memory is the issue).
- **Never on the server thread synchronously for a player action;** the player-facing step (accept contract / open trade) must be able to show "resolving…" or decide without a lead.
- **Dimension/seed:** Overworld only; seed read from the level; ST-01 does not exist elsewhere.
- **Independence from rotation/route:** location depends only on the start chunk and anchor; rotation (four) and EAST/WEST route are internal to the assembly and irrelevant.
- **Buried pyramids:** the start/box is independent of the surface, so a buried pyramid is located like any other. What it returns is a **region lead**, not an entrance; the entrance lies on the rotated side of the footprint (`St01PyramidCompletion.metaOf(start)` exposes ground, orientation, route for a later, owner-approved "which side" hint).
- **Multiplayer:** many requests share the cache; in-flight futures are de-duplicated per (chunk); ordering by request time; per-player rate limits are an owner/engineering knob.
- **Interruption/restart:** a ticket is in memory only; on restart it is simply re-requested (cheap when chunk data exists, as the probe's cached row shows).
- **Failure:** no start in range → `NONE_IN_RANGE` (a normal outcome in a Desert-poor area; 2 of 20 seeds had no pyramid in 420 km²); chunk failure → `UNAVAILABLE`, retried later.

## 5. What to prefer / avoid (location semantics, owner decisions)
Whether to prefer unvisited, un-defeated or un-looted pyramids is **not decidable from today's state** (visited is not tracked, claim is not tracked). Guardian-defeated and chest-sealed are queryable but only for *registered* (generated) instances. See OWNER_DECISIONS_REQUIRED.md D6.

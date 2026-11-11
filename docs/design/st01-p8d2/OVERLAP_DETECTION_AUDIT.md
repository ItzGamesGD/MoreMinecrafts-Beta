# ST-01 P8-D2 — Overlapping-structure detection audit (can a vanilla structure mask an ST-01 visit?)

**Verdict: FAIL for the current bounty entry detector (masking is real, and non-deterministic across JVM runs). PASS for a structure-specific lookup.**
No production code was changed; a concrete regression test is specified in §5.

## 1. The code under test
`ContractObjectives.detectStructure` (`bounty/ContractObjectives.java:57-72`) calls
`level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> true)` and credits **only the one structure it returns**.
Bytecode of 26.3 (`StructureManager`): `getStructureWithPieceAt(pos, predicate)` takes `startsForStructure(chunk, predicate)` — which iterates
`ChunkAccess.getAllReferences()`, a `Map<Structure, LongSet>` (field `structureReferences`) — and **returns the first start that has a piece at `pos`**.
`Structure` does not define a value-based hash, so the iteration order of that map is hash-order of object identity: unspecified and able to change from one JVM run to the next.

## 2. Experiment (real data, seed-by-seed, one fresh JVM per seed)
`St01D2Probe` mode `overlap` (source in `probe/`): enumerate the real ST-01 starts of the seed (±10,240 blocks), load the references of every chunk of each start's box,
find every valid vanilla start whose box intersects it (the P8-C2-C overlap set), and for **every block cell that lies inside a piece of both structures** call
`getStructureWithPieceAt(pos, h -> true)`, and also the ST-01-only lookup `getStructureWithPieceAt(pos, h -> h.value() == st01)`.

| Seed (first run) | ST-01 start chunk | Vanilla structure | Cells in pieces of **both** | `getStructureWithPieceAt(.., any)` returned | ST-01 masked? | ST-01-only lookup |
|---|---|---|---|---|---|---|
| 1001 | 48,-434 | village_desert | 18,686 | ST-01 ×18,686 | no | 18,686 / 18,686 |
| 119599970 | 355,-15 | village_desert | 13,038 | **village_desert ×13,038** | **yes, all** | 13,038 / 13,038 |
| 119599970 | -311,559 | mineshaft | 0 | – | – | – |
| 1541117449 | 109,-319 | village_desert | 9,720 | ST-01 ×9,720 | no | 9,720 / 9,720 |
| 1541117449 | 328,176 | mineshaft | 486 | ST-01 ×486 | no | 486 / 486 |
| 914490140 | -468,489 | village_desert | 4,674 | ST-01 ×4,674 | no | 4,674 / 4,674 |
| 1004 | -571,-501 | mineshaft | 535 | **mineshaft ×535** | **yes, all** | 535 / 535 |
| 1034165336 | 179,-213 | village_desert | 2,004 | **village_desert ×2,004** | **yes, all** | 2,004 / 2,004 |

(Six seeds were run, each in its own fresh JVM: the six seeds with overlaps in the P8-C2-C evidence that were practical to re-run. One further case, 119599970 / -311,559, has overlapping boxes but no cell inside both pieces. 7 of the 8 rows have shared cells.)

## 3. Findings
1. **Masking occurs.** In 3 of the 7 cases with shared cells the any-structure lookup returned the vanilla structure for **every** shared cell, so the bounty exploration detector would credit the village/mineshaft and **never credit ST-01** there.
2. **It is all-or-nothing within a run, and it changes between runs of the same seed.** Repeating the identical experiment in fresh JVMs (same seed, same cells):

   | Seed / pair | run 1 | run 2 | run 3 | `getStructureWithPieceAt(any)` returned |
   |---|---|---|---|---|
   | 1001, ST-01 + desert village (18,686 shared cells) | ST-01 | **village** | ST-01 | flips |
   | 1004, ST-01 + mineshaft (535 cells) | **mineshaft** | ST-01 | **mineshaft** | flips |
   | 119599970, ST-01 + desert village (13,038 cells) | **village** | **village** | **village** | stable in 3 runs |

   In 9 runs over three seeds the vanilla structure masked ST-01 in 6 and ST-01 won in 3; within each run all shared cells of a pair agreed. This matches an iteration order over a `Map<Structure, LongSet>` keyed by objects without a value-based hash (identity hash), so which structure is credited is not reproducible, not merely "sometimes missed". (The order rule itself is not documented by the game; observed, not proven.)
3. **A structure-specific lookup is available and correct**: `getStructureWithPieceAt(pos, holder -> holder.value() == st01)` found ST-01 in 100 % of the shared cells in every run (all 12 runs) (and in 100 % of the cells where the any-structure call masked it). A fix can therefore be **limited to the exploration detector**.
4. **The repo already has the safe pattern.** `progression/QuestStructureEntry.observe` (`:34-57`) iterates `getAllStructuresAt(pos).keySet()` and calls `getStructureWithPieceAt(x, y, z, structure)` per structure; it is overlap-correct and records each structure type entered.
5. **Cost:** the detector runs every server tick for each online player only while an ACCEPTED contract has an unfinished exploration objective. A per-target lookup (`getStructureWithPieceAt` restricted to the objective targets, or a loop over `getAllStructuresAt(pos)`) touches the same chunk references already read by the current call; it replaces one scan by a small constant number (number of distinct exploration targets held, at most a handful). No measurement was taken; no meaningful cost increase is expected, but it is **UNVERIFIED**.
6. P8-C2-C overlap totals for context: 16 vanilla overlaps over 46 pyramids (mineshaft ×8, desert village ×7, plains village ×1); only a subset share *pieces*, which is what matters here.

## 4. What this does and does not prove
- Proven (runtime, real worlds): masking of ST-01 by the any-structure lookup, and success of the structure-specific lookup, on the cases above.
- Not proven: the exact iteration order rule; behavior with three or more overlapping structures; behavior at chunk borders beyond those cells; real player movement (the probe evaluates the same API call for block positions, not an actual `ServerPlayer`).
- Not exercised: the contract credit path itself (`ContractService.enterStructure`) — unchanged, and independent of which structure the detector reports.

## 5. Regression test for the eventual fix (specification, not implemented)
GameTest `ContractExplorationOverlapGameTests` (+ pure unit test for the selection helper):
1. Fixture: a flat test world with two synthetic valid `StructureStart`s registered in the chunk references exactly as world generation does (the existing test
   `ContractRuntimeGameTests.generatedVanillaStructureEntryIsDetectedOnTheServerAfterAcceptance` shows how: `Structure.generate` + reference registration), one with
   `moreminecrafts:st01_pyramid` pieces and one with `minecraft:desert_pyramid`/`mineshaft` pieces overlapping the same cells.
2. A mock player with two ACCEPTED contracts, one exploring `moreminecrafts:st01_pyramid`, one exploring the vanilla target.
3. Stand in a cell inside both piece sets; run `ContractObjectives.detectStructure`.
4. **Assert both contracts advance by one distinct instance**, and that repeated ticks do not double count.
5. Order independence: repeat the whole test with the two starts registered in both insertion orders and with the references map rebuilt (or, if the order cannot be forced, run the
   assertion through the pure helper that takes an explicit list of starts in both orders, since the production order is not controllable).
6. Negative checks: a player in only the vanilla piece does not advance the ST-01 contract; a player outside every piece advances nothing; an OFFERED contract advances nothing.

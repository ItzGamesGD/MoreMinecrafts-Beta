# ST-01 P8-D1 — Existing system audit (read-only)

Status legend: IMPLEMENTED · PARTIAL · DESIGN_ONLY · ABSENT · UNVERIFIED. "Tested" is stated separately. Nothing here is a proposal.
Inspected at branch `docs/st01-p8d1-discovery-design-20261111`, which starts from `c376488d` (P8-C2-C). No production file was changed.

## 1. Baseline and dependency chain (verified, not assumed)

| Reference | Verified | Notes |
|---|---|---|
| P8-C2-A `c25f9d44` (PR #259) | commit exists; ancestor of P8-C2-B | construction + verification |
| P8-C2-B `9e4379c7` (PR #262) | commit exists; branch `claude/st01-p8c2b-natural-worldgen-isauq8` tip; ancestor of P8-C2-C | natural worldgen (entrance/fluid rejection, superseded by Policy D) |
| Audit report `569fc789` (PR #264) | commit exists; branch tip; **not** an ancestor of P8-C2-C (its docs were imported by file copy) | counterfactual rarity audit |
| P8-C2-C `f3ddfbea` / `c376488d` (PR #266) | commits exist; `c376488d` is the tip of `work/st01-p8c2c-policy-d-20261111` | Policy D |
| CI `42943983078` | recorded in the P8-C2-C report (24/24 jobs green at `f3ddfbea`); re-observed this session only as the stored artifact numbers | |

**None of these is on `main`.** `origin/main` is `cde842dd` (2026-10-23) and contains neither the ST-01 worldgen
(`pyramid/worldgen`, 0 files) nor the bounty system (`bounty/Contract*`, 0 files). The bounty system exists on the unified checkpoint
lineage (`checkpoint/wave10…wave16-unified-*`, introduced around 2026-10-25) and, through it, on the P8 branches. Everything below
describes the P8-C2-C branch. PRs #259/#262/#264/#266 are unmerged; the PR #266 head is the implementation baseline for P8-D.

## 2. Bounty / contract audit (source: `src/main/java/dev/itzgamesgd/moreminecrafts/bounty/`)

| # | Question | Status | Evidence |
|---|---|---|---|
| 1 | Generation | IMPLEMENTED | `ContractGenerator.generate` (:35) rolls objectives + reward once; `acquire` (:22) applies the 5 % Advanced→Special conversion. Per objective slot a uniform category draw `random.nextInt(3)` (combat/delivery/exploration), `tryObjectives` (:50). Unit tests: `ContractExplorationTest`, `ContractDistributionTest`. |
| 2 | Acquisition | IMPLEMENTED | (a) villager/wandering-trader offers: `ContractTrades` + `mixin/ContractTradeStockMixin` (after `getOffers`); (b) six vanilla chest loot tables: `ContractLootIntegration` (:85-91: abandoned mineshaft, desert pyramid, jungle temple, trial chambers reward, ancient city, bastion treasure). A trade/loot item is a *placeholder* (`PENDING_CONTRACT` tier); `ContractMaterializer.materialize` (:53-66) rolls it into a real contract on the player's tick. **ST-01 royal chest is not a contract source.** |
| 3 | Identity/progress storage | IMPLEMENTED | Item-bound data component `ModDataComponents.CONTRACT` = `ContractData(id UUID, tier, status OFFERED/ACCEPTED, objectives, reward)`; unstackable `ContractItem`; `ContractStacks.read/create`. |
| 4 | Exploration objective | IMPLEMENTED | `ContractData.Objective(category EXPLORATION, target = structure registry id, required, progress, visited list of instance keys)` (`ContractData.java:51` `enter`). Count bands per D2: Beginner 1, Intermediate 1–2, Advanced/Special 2–3 (`ContractGenerator.explorationBand` :180). |
| 5 | Target selection | IMPLEMENTED (no location) | Uniform pick from a hard-coded list `ContractCatalog.EXPLORATION_STRUCTURES` (:122-125: village_plains, desert_pyramid, jungle_pyramid, mineshaft, stronghold, mansion, trial_chambers). **The contract stores a structure *type*, never a position, and no search is run.** The list is marked PROVISIONAL in code and in the approved authority. |
| 6 | Visit validation | IMPLEMENTED | `ContractObjectives.detectStructure` (:57-72), called every server tick for every online player (:45-52): only if the player holds an ACCEPTED contract with an unfinished exploration objective, `level.structureManager().getStructureWithPieceAt(pos, h -> true)`; instance key = `dimension:startChunkPacked`. Credit goes to every ACCEPTED contract in the inventory via `ContractService.enterStructure` (:84-93). Tested for a vanilla desert pyramid by `ContractRuntimeGameTests.generatedVanillaStructureEntryIsDetectedOnTheServerAfterAcceptance` (:118) and `…unacceptedContractDoesNotCreditAndAcceptedOneDoesNotDoubleCountAnInstance` (:93). |
| 7 | Previously visited structures | IMPLEMENTED (by construction) | Only entries **after acceptance** count (`status == ACCEPTED`), a structure visited before acceptance qualifies on re-entry; the `visited` list de-duplicates per objective. No global "visited" state exists. |
| 8 | Death / transfer / logout / restart | IMPLEMENTED (tested) | Progress lives on the item; `ContractAttachments.RECEIPTS` is `copyOnDeath` and persistent (:54). Tests: `ContractRuntimeGameTests.deathDropKeepsContractIdentityAndProgress` (:30). Transfers: the item travels. |
| 9 | Simultaneous contracts | IMPLEMENTED | Every accepted contract in the inventory advances independently (`ContractService.advance/enterStructure` loop all slots). |
| 10 | Completion and rewards | IMPLEMENTED | `ContractService.act(COMPLETE)` → `ContractTransactions.redeem` (write-ahead, receipts, overflow drop, join-time `recover`). |
| 11 | Duplicate completion | IMPLEMENTED (tested) | `ContractLedger` two-phase PENDING/COMMITTED world ledger blocks every copy of an ID; test `concurrentSubmissionsAreSerializedAndPayOnce` (:54). |
| 12 | Professions selling contracts | IMPLEMENTED | `ContractVendorPolicy.ELIGIBLE_PROFESSIONS` (:7-9): farmer, fisherman, fletcher, weaponsmith, toolsmith, **cartographer**, cleric; Beginner at level ≥2, Intermediate ≥3, Advanced ≥4; stock 2/1/1 per villager; wandering trader 1–2 / 0–1 / 0–1. Price = band midpoint 3/8/16 emeralds (PROVISIONAL). |
| 13 | Custom structure types | PARTIAL | The objective target is a free string compared with the registry id of the structure found at the player's position, so `moreminecrafts:st01_pyramid` would match with **no detector change**. But the pool does not contain it, and the contract screen resolves names through `ContractCatalog.structureNameKey` (translation key `moreminecrafts.contract.structure.<ns>.<path>`; a key for ST-01 does not exist — UNVERIFIED whether a missing key breaks the screen; `ContractStructureNamesTest` exists). |
| 14 | Structure-specific exploration contracts | ABSENT | No contract is tied to a given structure instance or position; no ST-01 reference anywhere in `bounty/` (searched). |

Design-document status: `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` (Q62 + BC-03 overlay) approves exploration objectives as "structure entry after acceptance"
and states "Map ownership, proximity … do not credit". `docs/bounty_contract/BC-01_EXPLORATION_CATALOG.md` is **stale** ("0 verified MM structures").

### Bounty findings
- **F-1 No location.** Contracts never tell the player where anything is. An ST-01 target in the current pool would be a pure "find one yourself" objective.
- **F-2 Rarity mismatch.** Required counts are 1–3 *distinct* instances. At the measured Policy D density (0.548 / 100 km², P8-C2-C evidence), a Poisson approximation gives a mean nearest-neighbour distance of about 0.5/√λ ≈ 6.8 km between pyramids (derived, not measured); 2–3 distinct pyramids without location help is a very large trip, and 2 of 20 seeds had none in 420 km².
- **F-3 Overlap masking (code-derived, UNVERIFIED by test).** `StructureManager.getStructureWithPieceAt(pos, predicate)` returns the **first** matching start whose piece contains the position (bytecode of the 26.3 jar: iterate `startsForStructure(...)`, return at first `structureHasPieceAt`). Where a pyramid overlaps a village or mineshaft, `detectStructure` credits only whichever start the API yields first. With overlap now permitted, an ST-01 visit could be missed (or a vanilla visit masked). The credited target must be resolved per structure (e.g. query `getAllStructuresAt` / the contract's own target), a later-phase requirement.
- **F-4 Piece-box semantics.** "Entry" = standing inside any piece bounding box (`structureHasPieceAt`). For ST-01 the boxes cover the buried body, so a player excavating counts without reaching the chamber; whether that is the desired "visit" is an owner decision.
- **F-5 Materialization has no position context.** `ContractMaterializer.materialize` calls `ContractGenerator.acquire(pending, player.getRandom())` — no level, position or dimension. Any location-bearing objective needs a new, asynchronous or deferred resolution step.
- **F-6 Item-bound state is small.** Objectives are a fixed codec; adding fields must use `optionalFieldOf` to keep saved contracts readable.

## 3. Villager trading audit (26.3 APIs, jar `minecraft-common-deobf-26.3`)

| # | Question | Status | Evidence |
|---|---|---|---|
| 1 | Supported professions | IMPLEMENTED (vanilla) + contract overlay | All vanilla professions; the mod modifies offers post-hoc by mixin (`T1VillagerMixin` → `T1Supply.normalizeTrades`; `ContractTradeStockMixin`). |
| 2 | Profession-level trade registration | ABSENT (in mod) | No `data/**/villager_trade` or trade-set file and no Fabric trade API call in the mod (searched `src/main`); the mod only post-processes `getOffers()`. |
| 3 | Cartographer custom maps | ABSENT | Only `ContractVendorPolicy` mentions the cartographer (as a contract seller). |
| 4 | Custom explorer maps supported by the game | IMPLEMENTED in vanilla, data-driven | In 26.3 trades are datapack registries (`Registries.VILLAGER_TRADE`, `TRADE_SET`): `data/minecraft/villager_trade/cartographer/<level>/*.json`, grouped by `data/minecraft/tags/villager_trade/cartographer/level_N.json` and `trade_set/cartographer/level_N.json` (`amount: 2`). An explorer map is `given_item_modifier: [minecraft:exploration_map {decoration, destination: structure tag, search_radius: 100, skip_existing_chunks?}, filtered(map_id → discard on fail)]`, `gives: <map item>`, `wants` emerald (+compass), `max_uses` 12, `merchant_predicate` on villager variant. Vanilla destination tags live in `data/minecraft/tags/worldgen/structure/on_*_maps.json`. A mod *could* add a trade by shipping its own `villager_trade` JSON, an additive entry in the level tag, and a structure tag — **UNVERIFIED in this repo** (never done here). |
| 5 | Custom structure explorer map | PARTIAL | `ExplorationMapFunction` takes any structure tag/holder set; map decoration types are a static registry (`BuiltInRegistries.MAP_DECORATION_TYPE`; no datapack JSON) so a *new icon* needs code, reusing an existing decoration needs none. Per-structure map items in vanilla (`JUNGLE_PYRAMID_MAP`, …) are vanilla items; an ST-01 map would use `filled_map` or a new mod item. |
| 6 | Offer generation and persistence | IMPLEMENTED (vanilla) | Offers are generated when the villager first needs them / on level-up; the result `ItemStack` (a filled map with `map_id`, decorations, name) is stored in the villager's saved offers, so the target is stable across restarts. `max_uses` + restock reset uses; the same map can be re-bought (repeatable supply; cf. the villager repeatability constraint in the villager/book checkpoint §8). |
| 7 | Location search at offer generation | IMPLEMENTED (vanilla) | The `exploration_map` modifier calls `findNearestMapStructure` **synchronously on the server thread when the offer is created**; there is no async path. |
| 8 | Unavailable structures | IMPLEMENTED (vanilla) | The `filtered(map_id)` modifier discards the trade when no structure was found (the offer silently does not appear). |
| 9 | Stability after restarts | IMPLEMENTED (vanilla) | See 6. Not tested for ST-01. |
| 10 | Multiplayer target assignment | IMPLEMENTED (vanilla) | Offers belong to the villager, not the player; every buyer gets the same target; locating is relative to the villager's position. |
| 11 | Existing ST-01 targeting | ABSENT | No tag, trade, item or locator references `st01_pyramid` for discovery. |

A related, **unrelated-to-ST-01** item: the XP quest catalog has an entry `xpq.exp.explorer_maps` (`data/moreminecrafts/xp_quest/catalog.json:100`, tracker "pending") that would interact with any new map item; its semantics were not audited here (UNVERIFIED).

## 4. Minecraft 26.3 structure-location API facts (bytecode of the 26.3 jar)

- `ServerLevel.findNearestMapStructure(HolderSet|TagKey, BlockPos, int radius, boolean skipKnownStructures)` → `ChunkGenerator.findNearestMapStructure(ServerLevel, HolderSet, BlockPos, int, boolean)` → `Pair<BlockPos, Holder<Structure>>`; already used once in this repo for a vanilla structure: `worldgen/structure/ArenaWorldgenProbe.java:67`.
- For `random_spread` placements the search walks rings of potential chunks (`RandomSpreadStructurePlacement.getPotentialStructureChunk`), asks `StructureManager.checkStructurePresence` (START_PRESENT / START_NOT_PRESENT / needs load), and for undecided chunks calls **`getChunk(x, z, ChunkStatus.STRUCTURE_STARTS)`** then requires `StructureStart.isValid()`. Therefore the vanilla locator **does** distinguish an eligible candidate from a real start: it forces the candidate through the production `findGenerationPoint` and only returns a position when a valid start results. The returned position is `StructurePlacement.getLocatePos(chunk)` (chunk-based, not the pyramid anchor — see the probe results in SHARED_LOCATION_ARCHITECTURE.md §2).
- The cost of an undecided candidate is a full `findGenerationPoint`; for ST-01 that is the 81×81 Desert sweep plus 6,561 height samples, done only for Desert candidates.
- `getStructureWithPieceAt(pos, …)` returns the first match (F-3).

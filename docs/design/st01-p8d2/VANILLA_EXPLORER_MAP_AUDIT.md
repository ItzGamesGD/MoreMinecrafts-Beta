# ST-01 P8-D2 — Vanilla explorer-map audit (26.3 bytecode + isolated runtime fixtures)

Method: `javap -c` on the 26.3 jar (`minecraft-common-deobf-26.3`) plus a disposable datapack fixture and `St01D2Probe` (`probe/`), executed on real dedicated
servers (seed 1004, which has exactly three real ST-01 starts inside ±10,240 blocks: chunks (−571,−501), (−146,522), (−111,168)). Nothing from the fixture lives in
`src/main` or any production resource path.

## 1. How the mechanism works (answers to brief §3)
| # | Question | Finding | Evidence |
|---|---|---|---|
| 1 | `exploration_map` modifier | A loot-item function (`ExplorationMapFunction`). `run(stack, ctx)`: if the stack is empty or `ORIGIN` is absent it returns the stack untouched; otherwise `if (skipKnownStructures) claimStructureAt(level, origin)`, then `level.findNearestMapStructure(destination, origin, searchRadius, skipKnownStructures)`; if null → return the stack **unchanged (no `map_id`)**; else `MapItem.applyNewSavedData(level, stack, x, z, zoom, …)`, `MapItem.renderBiomePreviewMap(level, stack)`, `MapItemSavedData.addTargetDecoration(stack, pos, "+", decoration)`. | bytecode of `run` / `claimStructureAt` |
| 2 | How a structure tag is selected | JSON `destination` is a structure **tag** (`"#ns:name"`, a `HolderSet<Structure>`), tag file at `data/<ns>/tags/worldgen/structure/<name>.json`. | vanilla `data/minecraft/tags/worldgen/structure/on_*_maps.json`; fixture tag below |
| 3 | How the target is located | `ServerLevel.findNearestMapStructure` → `ChunkGenerator.findNearestMapStructure`: ring search over `RandomSpreadStructurePlacement.getPotentialStructureChunk`, `StructureManager.checkStructurePresence`, then a `getChunk(…, STRUCTURE_STARTS)` and `StructureStart.isValid()` check; returns `StructurePlacement.getLocatePos(chunk)`. | bytecode (P8-D1) + benchmark |
| 4 | Is the result an **actual valid** ST-01 start? | **Yes.** In every fixture map, the target decoration chunk equals a chunk of the enumerated real starts, and `getStartForStructure(st01).isValid()` is true there; **116 / 116** benchmark results were valid starts. The returned position is the **chunk's minimum corner at Y 0**, which is (−8, −8) from the pyramid anchor (**11.3 blocks**) and lies **inside the start's bounding box** (map "−9136,−8016" vs box "−9168,−8048..−9088,−7968"). | `fixture-noskip-create.json` → `targeting_check_next_to_first_start`; `STRUCTURE_SEARCH_BENCHMARK.md` |
| 5 | Map centre coordinates | The centre is **not** the target. `applyNewSavedData` snaps the centre to the zoom grid: located (−9136, −8016) → centre (−9024, −8000), scale 2 (a 512 × 512-block view; target 112 / 16 blocks from the centre, inside the view). The target is only the **decoration**. | map data in all fixture outputs |
| 6 | Map data creation and persistence | `applyNewSavedData` allocates a new `MapId` and `MapItemSavedData` in the world data storage and writes `map_id` (+ decorations) onto the stack. Offers keep the whole stack. **Every generated offer creates a new map id** (300 sampled villagers → ids up to 475). Restart persistence: PASS (§6). Maps are stored as `data/minecraft/maps/<id>.dat` (≈ 1.4 KB each). | fixtures; `MerchantOffers.CODEC` JSON of the offer shows `minecraft:map_id` and `minecraft:map_decorations` |
| 7 | Decorations and icons | The decoration is a holder of the **static** registry `BuiltInRegistries.MAP_DECORATION_TYPE` (not a datapack registry). about 40 vanilla types exist (the `MapDecorationTypes` constants), including `target_x`, `red_x`, `desert_pyramid`, `mineshaft`, `desert_village`. The fixture used `minecraft:target_x`. | `MapDecorationTypes`; fixture `decoration_types_present` |
| 8 | Custom structure tag can target ST-01 | **Yes**: `st01d2:on_st01_pyramid_maps` = `[moreminecrafts:st01_pyramid]` resolved from a datapack and the locator returned real pyramids. | fixture `structure_tag_values` |
| 9 | Custom icon necessary? | **No** for a vanilla-style map: existing decorations (`target_x`, `red_x`, `desert_pyramid`) need no new asset. A *new* icon would need a code-registered `MapDecorationType` plus a sprite asset (not tested). Rendering of these icons on a real client was **not verified** (no client run). | `MapDecorationTypes`; no client test |
| 10 | Reuse a vanilla icon without assets | Yes at the data level (resolves; used in the fixture). Visual correctness: UNVERIFIED. | – |
| 11 | Failed search | The function returns the stack without `map_id`; the vanilla `filtered(map_id → discard)` modifier discards the trade (§3 of `CARTOGRAPHER_TRADE_FEASIBILITY.md`). A radius-2 trade at the origin returned no offer in 38–45 ms. | fixture `direct_trade_at_origin.map_r2` |
| 12 | Outside villager trading | **Yes.** `ExplorationMapFunction.makeExplorationMap(HolderSet).setSearchRadius(20)….build()` applied to a plain `minecraft:map` with a **chest** loot context carrying only `ORIGIN` produced a valid map with the same target (99–113 ms warm). The function needs only a `LootContext` with `ORIGIN`. | fixture `exploration_map_function_applied_outside_trading_with_chest_context` |

## 2. Fixture (disposable; not committed to production paths)
A world datapack `file/st01d2` (enabled with `initial-enabled-packs`), `pack.mcmeta` `min_format = max_format = [121,0]` (matches `version.json` `data_major` 121):
- `data/st01d2/tags/worldgen/structure/on_st01_pyramid_maps.json` → `["moreminecrafts:st01_pyramid"]`
- `data/st01d2/villager_trade/map_r2|r5|r20.json` and `map_r20_noskip.json`: `wants` 8 emeralds, `additional_wants` compass, `gives` `minecraft:filled_map`, `given_item_modifier` = `exploration_map` (`decoration` `minecraft:target_x`, `destination` `#st01d2:on_st01_pyramid_maps`, `search_radius` 2/5/20) then `filtered(map_id)` → discard, `max_uses` 12, `xp` 5; the `noskip` variant adds `"skip_existing_chunks": false`.
- `data/minecraft/tags/villager_trade/cartographer/level_2.json` = `{"replace": false, "values": ["st01d2:map_r20"]}` (or the `noskip` trade).
The files are reproduced in `probe/fixture-pack/` for reproducibility.

## 3. The decisive finding: `skip_existing_chunks` makes each pyramid single-use
The vanilla function's default is `skip_existing_chunks = true` (field default of `ExplorationMapFunction`; vanilla trades omit the key). In `findNearestMapStructure`, a start found while `skipKnown` is set receives a **reference** (`tryAddReference` → `StructureStart.addReference`), and `StructureStart.canBeReferenced()` is `references < getMaxReferences()`; the next skip-known search treats that start as already taken. Observed (seed 1004, which has 3 pyramids in range, trade radius 20, default skip):

| Maker | Map id | Target chunk |
|---|---|---|
| direct trade call (radius 5) | 0 | (−111,168) |
| direct trade call (radius 20) | 1 | (−146,522) |
| first sampled villager (map id 2 was the unrelated `skip_existing_chunks:false` call) | 3 | (−571,−501) |
| every later villager (299 of 300) | – | **no map; trade discarded and replaced** |

So with the vanilla default, **only 3 maps ever exist for 3 pyramids** and 299 of 300 sampled cartographers never got an ST-01 offer; the sample reproduced identically in two runs. With `"skip_existing_chunks": false` the same fixture gave an ST-01 offer to **117 of 300** villagers and any number of maps pointing at the same nearest pyramid.

Consequences (owner decisions): (a) default skip means a **finite, per-world supply** (≈ 0.55 pyramids / 100 km²) shared by every map issued with skip on, so **channels A4 and B1 would compete** for the same pyramids if both used it (a shared-resource coupling, not a prerequisite); (b) `false` removes the competition and the exhaustion but lets many players receive the same target and points at already-explored pyramids; (c) a "claimed" bit exists only as the vanilla reference count, which is not a gameplay state (it is set by *generating a map*, not by visiting).

## 4. Other behavior observed
- Level-1 cartographers: 2 vanilla offers (`emerald` for paper, `map`), unchanged by the fixture. After leveling to 2 (`updateTrades` as `increaseMerchantCareer` does): 2 vanilla level-2 offers (+ 2 contract offers from `ContractTradeStockMixin`) = 6 in all 300 villagers, both with and without the pack (`fixture-control-nopack.json`).
- `ExplorationMapFunction` and the trade modifier run **synchronously**; the first call in a fresh world cost 6.3 s (radius 5) and 18.5–19.2 s (radius 20, after the radius-5 call had marked the nearest pyramid), see the benchmark.
- Offer generation creates the map data at generation time for each candidate that is chosen, so the world data grows by one map per generated offer.

## 5. Probe defects found and fixed (disclosed)
1. `LootContext` for `VILLAGER_TRADE` needs `ADDITIONAL_COST_COMPONENT_ALLOWED` (a `Unit`): added.
2. The first targeting check measured the quantized map centre instead of the decoration, and reported "not a valid start" incorrectly; it now checks the decoration position (result in §1 row 4).
3. The first restart check found no villager because the entity was added to a chunk that was not loaded/forced, so it was never saved; fixed and rerun (§6).

## 6. Restart persistence
PASS: after a full server restart the persisted villager's map offer and the saved map data were identical (details in `CARTOGRAPHER_TRADE_FEASIBILITY.md` §5).

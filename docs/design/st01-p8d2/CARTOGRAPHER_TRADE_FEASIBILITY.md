# ST-01 P8-D2 — Cartographer trade feasibility (disposable datapack fixture; additive merge tested)

The fixture (see `VANILLA_EXPLORER_MAP_AUDIT.md` §2, files in `probe/fixture-pack/`) was loaded as a world datapack into a real 26.3 dedicated server running the P8-C2-C
branch. Nothing was added to `src/main/resources`.

## 1. Schema and registration — PASS
- `villager_trade` JSON (`data/<ns>/villager_trade/<name>.json`) accepted as written (keys `wants`, `additional_wants`, `gives`, `given_item_modifier` = [`exploration_map`, `filtered(map_id)`], `max_uses`, `xp`, `reputation_discount`). All four fixture trades appeared in `Registries.VILLAGER_TRADE`.
- **Profession registration is not needed.** The cartographer is a vanilla profession whose `VillagerProfession.tradeSetsByLevel` maps level 1 and 2 to `minecraft:trade_set/cartographer/level_1|2`; each trade set is `{amount: 2, trades: "#minecraft:cartographer/level_N"}`. The mod only supplies trades.
- **Level assignment is by tag membership**: the trade is a member of `#minecraft:cartographer/level_2`, so it is offered when a cartographer reaches level 2.
- The `pack.mcmeta` for 26.3 uses `min_format`/`max_format` `[121,0]` (the `data_major` of `version.json`).

## 2. Additive versus replacement — PASS (additive)
`data/minecraft/tags/villager_trade/cartographer/level_2.json` = `{"replace": false, "values": ["st01d2:map_r20"]}` **merged** with the vanilla tag: the resolved tag contained the **8 vanilla entries plus the fixture trade** (9). The level-1 tag was untouched (2 entries). No vanilla file was replaced.

## 3. Trade availability — PARTIAL (works; **not guaranteed**)
Because the trade set draws `amount: 2` offers *at random* from the tag, an additive entry is only one candidate among nine; discarded candidates (failed map searches) are replaced by other candidates.
300 simulated cartographers (level 1 created, then leveled to 2 exactly as `Villager.increaseMerchantCareer` does — `updateTrades` after `withLevel(2)`), at the world origin of seed 1004:

| Configuration | villagers with an ST-01 map offer | offers per villager | notes |
|---|---|---|---|
| no pack (control) | **0 / 300** | 6 | vanilla: 2 level-1 + 2 level-2 + 2 contract offers |
| pack, `skip_existing_chunks` default (true) | **1 / 300** | 6 | supply exhausted after 3 maps (3 pyramids in range) |
| pack, `skip_existing_chunks:false` | **117 / 300 (39 %)** | 6 | more than 2/9 = 22 % because failed vanilla map candidates are replaced by the next draw; position/world dependent |

- Level-1 offers were identical in all configurations (2 per villager: emerald for paper, `minecraft:map`), and **all level-2 vanilla results remain** (`jungle_pyramid_map`, `plains_village_map`, `savanna_village_map`, emeralds); the **mod's contract stock** (+2 offers, `ContractTradeStockMixin`) still appears. **Vanilla trade preservation: PASS.**
- A guaranteed offer needs something beyond an additive tag entry: e.g. a dedicated trade-set override (replaces a vanilla data file, fragile with other mods), or a deterministic top-up like the existing `ContractTradeStockMixin` (precedent in this repo), or accepting probabilistic availability. Owner decision.

## 4. Missing-target behavior — PASS (understood)
A trade whose search finds nothing yields **no offer** (`getOffer` returns null in 38–45 ms at radius 2) and the trade set silently draws another candidate, so a cartographer never shows a broken map and the offer count stays 6. The search cost of a *found* target is large (benchmark): first radius-5 trade 6.2–6.3 s, first radius-20 trade 18.5–19.2 s in the fixture.

## 5. Persistence after restart — PASS (single seed, single villager)
A persistent villager (its chunk loaded and force-loaded) was given one fixture map offer (`noskip` trade, map id 474, decoration `target_x` at (−9136, −8016) = real start chunk (−571,−501)), the world was saved and the server **stopped and started again** on the same world with the datapack. After 80 ticks the villager was found and its offer was **identical**: same `map_id` 474, same decoration position and `rotation`, same prices/`maxUses`/`xp`, and `MapItem.getSavedData` returned the same saved map (centre (−9024, −8000), scale 2) — the map data persisted in `data/minecraft/maps/474.dat`. After restart `getOffers()` also re-added the mod's contract stock (offer count 3 = 1 map + 2 contract offers), which is the existing `ContractTradeStockMixin` behavior and not a persistence fault.
Map storage cost: 476 maps (the 300 sampled villagers plus fixture calls) occupied **647,333 bytes** in `world/data` (≈ 1.4 KB per generated map offer).
An earlier restart check in this phase failed ("villager not found") because of a probe defect (the villager's chunk was not loaded/forced when the entity was added, so it was never saved); fixed and rerun as above.

## 6. Map generation — PASS
Every created offer holds a real map (`filled_map`, `map_id`, scale 2, target decoration at the located chunk corner). Targets were verified against the enumerated real starts; see `VANILLA_EXPLORER_MAP_AUDIT.md` §1. Map data is allocated per generated offer (ids up to 475 for 300 sampled villagers).

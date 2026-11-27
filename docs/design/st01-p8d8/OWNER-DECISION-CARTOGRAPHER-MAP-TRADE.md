# ST-01 P8-D8 — Owner decision: cartographer ST-01 explorer-map trade (Channel B1)

Date: 2026-11-13 · Source: P8-D8 brief §2-A/B/C · Implements Channel B1 of
[OWNER_DECISION_A4_B1_HYBRID.md](../st01-p8d2/OWNER_DECISION_A4_B1_HYBRID.md).

## Approved by the owner (P8-D8 brief)
| Item | Decision |
|---|---|
| Seller | Cartographer |
| Level | 4 |
| Price | 16 emeralds + 1 compass |
| Appearance | 50 % |
| Not prepared yet | an unpurchasable placeholder offer is shown and replaced, in the same slot, by the real ST-01 explorer-map offer once ready; slot count and order of existing offers preserved; works across save/restart and with the trading screen open |
| Preparation failure | retried with an interval and a limit |
| No ST-01 found | the slot receives another normal trade; no endless waiting |
| Preload | the target chunk is preloaded (non-persistent, self-expiring ticket) only for a pending placeholder with a known target; "ready" includes that chunk having reached the required generation step; no synchronous search on the server thread while not ready |

## How "50 %" is applied (implementation)
The ST-01 trade is **not** added to the vanilla level-4 trade pool (which would make its chance depend on the pool size and
displace a vanilla trade). When a cartographer's level-4 trades are generated, the villager rolls once (persisted), and on
success the map offer is added **after** the vanilla level-4 offers. Vanilla level-4 offers, their number and their order are
unchanged.

## Items still provisional in the B1 record — values used, following vanilla explorer-map trades
These are not new owner decisions; they copy what vanilla's own cartographer explorer-map trades use and may be revised:

| Item | Value | Vanilla reference |
|---|---|---|
| `search_radius` | 100 | all vanilla cartographer explorer-map trades |
| `skip_existing_chunks` | vanilla default (true) | all vanilla explorer-map trades omit it |
| Stock (`max_uses`) / refresh | 12, vanilla restock | vanilla explorer-map trades |
| Reputation discount | 0.2 | vanilla explorer-map trades |
| Trade XP | 15 | vanilla cartographer level-4 trades |
| Map decoration | `minecraft:target_x` | no vanilla pyramid icon; P8-D2 fixture |
| Item | `filled_map` named "Colossal Pyramid Map" | vanilla explorer maps are named filled maps |

Data: `data/moreminecrafts/villager_trade/cartographer/4/emerald_and_compass_st01_pyramid_map.json`,
`data/moreminecrafts/tags/worldgen/structure/on_st01_pyramid_maps.json`. Code: `pyramid/maptrade`.

Unchanged: ST-01 placement, rarity, terrain policy (Policy D, MODERATE), structure id, assets, other structures, Channel A4.

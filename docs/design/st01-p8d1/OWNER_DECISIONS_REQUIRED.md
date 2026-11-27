# ST-01 P8-D1 — Owner decisions required (nothing here is approved)

Default principle: the discovery channels stay independent unless the owner approves a specific interaction. Each decision lists the
options, the recommendation (opinion), and what it blocks.

## Technical direction
- **D1 Architecture.** A vanilla-first / B custom service / **C hybrid (recommended)**. Blocks every implementation phase.
- **D2 Channel B information format.** B1 explorer map (recommended first, coarse) / B2 written clue / B3 coordinates / B4 tracker item. Blocks P8-D2b.
- **D3 Channel A lead.** A1 no lead / A2 exact located contract / **A3 regional lead (recommended)**; completion stays physical entry (A4). Blocks P8-D2c.

## Gameplay values (all currently unapproved)
- **D4 Acquisition and availability.** Which professions/levels sell the ST-01 map (candidate: cartographer, level to choose); whether contracts that lead to ST-01 come from existing sources, from specific professions, or from structure loot (the royal chest is not a contract source today); price; `max_uses`/restock cap; whether the mod may add ST-01 to `ContractCatalog.EXPLORATION_STRUCTURES` and with which count (D2 bands 1–3 vs 1).
- **D4b Location precision.** Exact marker / chunk-level / coarse region / bearing+distance band; whether to promise an entrance (Policy D allows buried entrances; recommended: never promise an entrance).
- **D4c Search limits.** Maximum search distance for each channel (must come from a benchmark, see roadmap P8-D2a; the vanilla value 100 is unmeasured and likely too large).
- **D4d Rewards.** None proposed; existing contract rewards and the ST-01 royal chest are unchanged.

## Cross-channel coexistence (defaults proposed; confirm or change)
| Question | Proposed default |
|---|---|
| 1. May both channels target the same pyramid? | Yes — no coordination, both are nearest-valid-start queries |
| 2. May a player use both at once? | Yes |
| 3. Does completing a bounty affect villager information? | No |
| 4. Does buying a map affect bounty progress? | No (the approved rule already says map ownership does not credit) |
| 5. Are already-explored pyramids eligible targets? | Owner decision (D6); not decidable from today's state (visited is not tracked) |
| 6. Are looted pyramids eligible? | Owner decision (D6); Guardian-defeated/chest-sealed are queryable, claimed is not |
| 7. Does one player's completion affect another's discovery? | No (no per-player copies of structures or rewards) |
| 8. Can target caching be shared without coupling progression? | Yes — the location service is a stateless query with a cache of *world facts*, never of per-player progress |

## Exploration rules
- **D5 Multiplayer.** Confirm "same pyramid for many players" (no per-player copies; first-come loot is the existing royal-chest behavior).
- **D6 Prefer unexplored/undefeated pyramids?** Options: no preference (recommended for the first release: nearest valid start); prefer not-yet-defeated (queryable only after generation); add a world-level "discovered" record (new persistent state, **new design**).
- **D7 What counts as a visit for a contract.** Today: standing inside any piece box (audit F-4). Options: keep; require the entrance side/royal hall; require the Guardian. Affects excavation play.
- **D8 Failure behavior.** No pyramid in range: contract refused / contract valid with no lead / retry later; map: trade hidden (vanilla) vs visible fallback clue.
- **D9 Overlap handling in detection.** Fix the bounty detector so overlap with villages/mineshafts cannot mask ST-01 credit (audit F-3). Recommended as a correctness requirement, not a design choice; the owner may just confirm.

## Items outside P8-D1
Vanilla overlap is already decided (AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED.md). Visual acceptance of the Policy D pyramids is still UNVERIFIED from P8-C2-C.

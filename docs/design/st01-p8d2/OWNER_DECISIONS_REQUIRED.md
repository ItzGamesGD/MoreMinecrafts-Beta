# ST-01 P8-D2 — Owner decisions required (nothing approved by this document)

Approved already: A4, B1, hybrid architecture, independence (`OWNER_DECISION_A4_B1_HYBRID.md`). Open items, each with evidence and a recommendation (opinion).

| # | Decision | Options | Evidence / consequence | Recommendation |
|---|---|---|---|---|
| E1 | **Map supply policy** (`skip_existing_chunks`) | true (vanilla default): each pyramid issues one map per world; false: unlimited maps to the nearest pyramid | with true: 3 pyramids → 3 maps then no trade; A4 and B1 would **compete** for the same pyramids; with false: no limit, repeated targets (`VANILLA_EXPLORER_MAP_AUDIT.md` §3) | false for both channels (preserves independence), unless scarcity of maps is intended |
| E2 | **Search radius per channel** and what happens on a miss | small (cheap, rarely finds) … ≥ 21 regions (≈ 11 km; finds in 6/6 seeds, costs 6–28 s cold) | benchmark: radius 5 median 0.1 s but 1/6 found; radius 20: 10.7 s median, 5/6 found; failed search costs the full area | decide after the hardening benchmark on target hardware; do not use vanilla's 100 without a guard |
| E3 | **Acceptable server stall** (tick budget) for map creation | e.g. block (vanilla behavior), or require the prepared/guarded path | N1: 6–28 s; N3b: gaps ≈ 0.9 s (partial result); N2 cannot bound | owner sets the budget; implementation must meet it on target hardware |
| E4 | **Guaranteed vs probabilistic cartographer offer** | additive tag entry only (22–39 % of cartographers get it); deterministic top-up (precedent: `ContractTradeStockMixin`); trade-set override | `CARTOGRAPHER_TRADE_FEASIBILITY.md` §3 | owner choice; the additive entry is safest for compatibility |
| E5 | **Profession, level, price, `max_uses`, restock** | cartographer level 2 is where vanilla explorer maps are | repeatability of villager supply is a known constraint (villager checkpoint §8) | owner |
| E6 | **Map item and icon** | `filled_map` + existing icon (`target_x`, `desert_pyramid`) / new item / new icon (code + asset) | no new asset needed for the first two; client rendering unverified | existing icon, verify on a real client first |
| E7 | **A4 acquisition option** | A4-1 on accept, A4-2 on completion of a separate contract, A4-3 exchange, A4-4 info-in-contract (only if the owner relaxes the map requirement) | `BOUNTY_MAP_INTEGRATION.md` | decide after E1–E3; A4-2 reuses the ledger/receipt machinery the most |
| E8 | **No-target fallback** | refuse, valid contract without a map, pay an alternative | needed for A4 and for any non-trade creation | owner |
| E9 | **Visit definition** for contracts | piece-box entry (current) / entrance side / hall | P8-D1 D7; buried entrances allowed by Policy D | owner |
| E10 | **Detector fix** | per-structure lookup limited to the exploration detector | overlap masking verified, order flips between JVM runs (`OVERLAP_DETECTION_AUDIT.md`) | treat as a correctness requirement; confirm |
| E11 | **XP quest guard** | require that a future `xpq.exp.explorer_maps` hook identify maps by target structure / vanilla map item, and that ST-01 maps use neither | `XP_QUEST_COMPATIBILITY.md` | confirm as an implementation rule |
| E12 | **Concurrency target** | how many simultaneous map requests must be supported | not tested | owner / later benchmark |

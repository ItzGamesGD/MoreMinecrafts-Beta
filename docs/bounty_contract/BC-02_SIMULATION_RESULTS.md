# BC-02 Simulation Results

Reproduce: `python scripts/bounty_contract_balance/simulate.py`; test: `python -m unittest discover -s scripts/bounty_contract_balance -p "test_*.py"`. Python standard library only. Seed `20261022`; 10,000 independent contracts per scenario and sensitivity case. Script overwrites this Markdown deterministically.

**Model inputs:** Target IDs are read from the BC-01 catalogs and checked for membership. Price/payout, rarity and quantity bands come from approved authority. Sampled price/payout are uniform *within* approved bands solely for sensitivity; real effort adjustment is OPEN. Reward unit emerald equivalents are assumptions: low `(0.02, 0.08, 0.4, 2)`, mid `(0.08, 0.3, 2, 12)`, high `(0.2, 1, 8, 48)` for common/high-grade/rare/legendary. They are not verified prices or reward-pool contents. Effort units and delivery opportunity costs are scenario hypotheses, not observed gameplay facts.

| Scenario / BC-01 ID | Tier | Cost assumption | Effort assumption | Mid net mean | Mid p05–p95 | Low–high mean | Mid net / effort | Legendary sampled |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| beginner combat `moreminecrafts:swamp_zombie` | beginner | 0 | 12 | 7.7 | 3.84–11.9 | 5.3–13.71 | 0.642 | 0.21% |
| intermediate combat `moreminecrafts:bandit_archer` | intermediate | 0 | 45 | 13.98 | 6.44–22 | 10.93–22.76 | 0.311 | 0.97% |
| advanced combat `moreminecrafts:lesser_magma_cube` | advanced | 0 | 120 | 26.7 | 12.68–41 | 22.74–39.33 | 0.222 | 2.90% |
| common delivery `minecraft:wheat` | beginner | 0.96 | 8 | 6.71 | 2.88–10.84 | 4.33–12.6 | 0.839 | 0.21% |
| rare delivery `minecraft:diamond` | advanced | 24 | 80 | 2.71 | -11.4–17 | -1.26–15.38 | 0.034 | 2.94% |
| MM material delivery `moreminecrafts:adamantium_ingot` | advanced | 96 | 160 | -69.35 | -83–-55 | -73.41–-56.35 | -0.433 | 2.94% |
| exploration `minecraft:mineshaft` | intermediate | 0 | 35 | 14.04 | 6.52–22.0 | 10.95–22.99 | 0.401 | 1.02% |
| special trial `minecraft:trial_chambers` | advanced | 0 | 120 | 28.21 | 13.8–43 | 23.07–45.83 | 0.235 | 4.99% |

Net = emerald payout − purchase price − assumed material opportunity cost + assumed extra reward value. Material cost is a hypothetical conversion to emerald equivalents. Negative net means economic loss under that assumption. Completion effort is a normalized hypothesis; no measured spawn, catch or travel rates were available.

## Compatible shared combat stacking

| Contracts | Shared qualifying kills | Mean purchase | Mean emerald payout | Mean emerald net | Net / kill |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 12 | 3 | 7.5 | 4.5 | 0.375 |
| 2 | 12 | 6 | 15.0 | 9.0 | 0.75 |
| 5 | 12 | 15 | 37.5 | 22.5 | 1.875 |
| 10 | 12 | 30 | 75.0 | 45.0 | 3.75 |

One kill advances all matching accepted contracts, including both entries in one contract. Duplicate requirements 8 and 16 require 16 kills total, with counters `(8,8)` after 8 and `(8,16)` after 16. For two delivery contracts each requiring 32 of an item, starting inventory 32 completes the first and leaves the second incomplete with inventory 0. It must acquire another 32.

## Reward distribution and tail

Approved rarity totals: Beginner 100%, Intermediate 100%, Advanced 100%, Special 100%. Approved quantity bands: Common 16–48, High-grade 8–24, Rare 2–8, Legendary 1–2. Sampled legendary frequencies appear above; binomial sampling error is approximately 0.14 percentage points at 0.2% and 0.43 points at 5%. A legendary reward worth 48 emerald equivalents per unit in the high sensitivity case can add 48–96 to one contract, versus a 2–20 emerald purchase. No real reward item is asserted to have this value.

## Limits

Special trial availability and direct special acquisition probability are not validated; the special scenario is conditional on obtaining a special contract and uses only a vanilla trial chamber target. Structure entry detection, completion times, market values, stock refresh, vendor encounter rate, and actual chosen reward items are UNKNOWN. No MM structure or unregistered item/entity enters any scenario.

# BC-02 Evidence-Based Economy Audit

**Status:** analytical simulation, not approved balance. BC-01 catalogs are the target source; the approved Q1–Q62 rules in `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` remain unchanged. Reproducible script and scenario table: [results](BC-02_SIMULATION_RESULTS.md).

## Model and coverage

The simulator samples 10,000 contracts per scenario with seed 20261022 and three hypothetical reward-unit valuations. It covers beginner, intermediate, advanced combat; common and rare delivery; a high-value MM material; exploration; a conditional special; 1/2/5/10 compatible contracts; duplicate kill entries; shared kill progress; inventory competition between delivery contracts; and the legendary reward tail. All simulated MM objective IDs belong to BC-01 verified registrations. The catalog does not verify real spawn frequency, structure visit time, item market prices, or reward item identities. The simulation's effort units and values are assumptions and cannot be treated as gameplay measurements.

## Approved arithmetic versus assumptions

| Rule | Status |
| --- | --- |
| Beginner purchase 2–4, payout 5–10; Intermediate 6–10, 12–24; Advanced 12–20, 25–50 | APPROVED |
| Standard rarity 80/17/2.8/0.2, 60/30/9/1, 40/40/17/3 percent | APPROVED |
| Special rarity 20/45/30/5 percent | APPROVED |
| Reward quantities 16–48, 8–24, 2–8, 1–2, subject to item-specific caps | APPROVED |
| One qualifying kill credits duplicate entries and all matching accepted contracts | APPROVED |
| Delivery materials are revalidated and consumed per completed contract | APPROVED |
| Uniform sampling within price/payout bands, emerald-equivalent item values, effort units | MODEL ASSUMPTION |
| Exact item caps, target/effort weights and eligibility filters | PROPOSED / OPEN |

The approved emerald-only net ranges before material opportunity cost and extra rewards are beginner +1 to +8, intermediate +2 to +18, advanced +5 to +38. At the uniform midpoint, they are +4.5, +10, +21.5 respectively. The economy is structurally positive on completed combat/exploration contracts. Travel, killing and purchase availability constrain realized income; their rates are UNKNOWN.

## Ranked economic risks

| Rank | Mechanism | Evidence / modeled consequence | Needed decision |
| ---: | --- | --- | --- |
| 1 | High-value delivery above payout capacity | Eight hypothetical adamantium ingots at 12 emerald equivalents each cost 96; advanced mid net is −69.35 even including modeled extra reward. | Item cap or reject this objective; determine value evidence. |
| 2 | Shared combat credit and duplicate entries | Ten beginner contracts can share twelve kills and yield a modeled 45 emerald-only net, or 3.75 per kill; unlimited acceptance is approved. | Set contract stock/selection and effort weights, then live-measure acquisition rate; do not remove shared credit. |
| 3 | Legendary reward tails | High sensitivity 1–2 items at 48 each can add 48–96 emerald equivalents to one contract; real reward pool/value UNKNOWN. | Select reward items and item-specific caps. |
| 4 | Renewable low-effort delivery | Hypothetical 24 wheat costs 0.96 and beginner mid net is +6.71; automatic farming could make effort near zero. | Weight farmables by measured acquisition effort. |
| 5 | Revisited exploration locations | Revisit after acceptance is approved; a known nearby structure could be repeatedly used if offers are obtainable. | Define entry detection and travel/visit criteria, then test. |

Special objectives for MM structures and meteors remain unavailable; the modeled special is conditional on a vanilla trial chamber visit. The approved 5% advanced-to-special conversion is not treated as a generic special drop rate. Direct special reward probability and structure tier matrices remain OPEN.

## Limits and engineering debt

Static spawn registration does not prove actual survival frequency. Worldgen can be disabled or modified by world settings. Vanilla structure identifiers require registry/runtime cross-check. Validate duplicate-progress serialization, damage contribution attribution, transfer/death persistence, competing inventory completion, double redemption and crash-safe atomic completion during implementation. These are not claimed by this model.

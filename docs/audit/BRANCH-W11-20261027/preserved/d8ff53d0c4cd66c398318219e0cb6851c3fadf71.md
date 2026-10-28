# MM Territories & Economy — Approved Design Checkpoint (2026-10-19)

**Authority:** User-approved conversation decisions through 2026-10-19. **Scope:** design-only, not implementation, not live acceptance. This is a *later approval overlay* over the older 2026-10-07 content-axis review's economy/settlement deferrals. Never rewrite older history to imply it was approved at the time. Companion docs: `BOUNTY_COMMISSION_APPROVED_2026-10-19.md` and `DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md`.

## 1. Principles & exclusions [APPROVED]
- Optional village **territory/estate (영지)** progression. Vanilla village use remains possible without joining this system.
- Five village biome families: **plains, desert, savanna, taiga, snowy**. No obligation to invent a unique artifact for every biome.
- **One global tier per territory**: T0 ordinary village → T1 settlement → T2 developed → T3 prosperous. Upgrading advances the whole territory simultaneously; do **not** add independently leveled facilities.
- Upgrade costs may use resources, but **no recurring upkeep, taxes, happiness, recruitment/population micromanagement, market-demand simulation, or regression on raid failure**.
- Visual development matters: more villagers and friendly guards, upgrading existing buildings and constructing additional homes/commercial buildings. Population should respect available beds/workstations; grow after facilities are available.
- Territory-managed buildings/protected blocks must resist player breaking/modification so expansions cannot overwrite resident edits. **Do not blanket-protect every player-placed block.** Future placement must check terrain slopes, structures, collisions, roads, and existing player construction. Implementation/testing may be delegated to structure-generator; placement algorithms are **not** already validated.
- Keep vanilla-style trading **simple to operate**. Internal rules may be sophisticated, but player interactions should be immediate, without long contract/queue management.
- Reuse emeralds/emerald blocks and material barter; **no new currency**.
- **Explicit rejection:** economy-wide bulk procurement contracts, additional repeating sales missions, complex demand/supply/price simulation. These must not sneak back in through T1/T3. They are distinct from the accepted *bounty commission system*.

## 2. Territory growth & guards [APPROVED DIRECTION]
Original village size matters. The approved recommended T3 *cumulative growth targets* are:

| Village starting population | Extra settlers by T3 | T3 guard total target | Approx. newly constructed buildings |
|---|---:|---:|---:|
| Small (1–5) | +7 | 4 | ~5 |
| Medium (6–10) | +8 | 6 | ~6 |
| Large (11+) | +10 | 8 | ~7 |

These are size-sensitive design targets, not a command to spawn everyone immediately. Tier-by-tier placement and staffing must remain conditional on valid beds/workstations/space; do not silently substitute fixed totals.

**Guard contract:**
- Guards are friendly to their territory's player; they **never retaliate/attack the player on accidental hits**. Ordinary village reputation consequences may still apply.
- Guard stays to defend territory and villagers; **does not follow on adventures** (separate from hireable mercenaries).
- Recover HP **once at morning**, not continuously: T1 **35%**, T2 **50%**, T3 **70%** of maximum health.
- Missing guards replenish **from the next morning, at most 2 per Minecraft day**; no money and no immediate mid-combat replacement.
- Earlier *provisional* baseline guard stats: T1 24 HP / 4 attack / 2 armor; T2 32 / 5 / 4; T3 40 / 6 / 6. Treat these as subject to vanilla combat/playtest reconciliation rather than unreviewed hardcoded final balance.
- A failed raid should not reduce town tier or invoke upkeep/regression.

## 3. Trading progression [APPROVED]
### Common: villagers and transaction convenience
- Extend **existing vanilla villager trades/UI** rather than forcing a new standalone market economy. Keep the ordinary vanilla profession/stock/restock paths working.
- **Bulk trade is QoL only** (e.g. quantity 1 / 8 / maximum), not a new logistics/procurement mechanic. Maintain correct inventory validation and atomic transactions.
- **Special profession trades** acceptable conditionally; discard any individual profession addition if it adds excessive complexity or duplicates vanilla options. **No full profession recipe matrix is approved yet.**

### T1 — profession-specific barter
- **Material-for-material trade with no emerald payment**. Transactions are immediate.
- New barter offer stock around **4–12 uses per offer**; refresh **once per in-game day**.
- Vanilla trades retain their existing stock/restock rules; don't globally change them.
- Exact offers across the 13 villager professions were **NOT APPROVED**. Audit vanilla overlap and arbitrage loops before drafting individual recipes; do not promote an old candidate list to final authority.
- **No recurring bulk contracts**.

### T2 — exactly three profession services
These are instant purchases/orders by the **player**, not quest-like missions or timed delivery:
1. **Equipment dismantling/recycling** at toolsmith/weaponsmith/armorer: submit used armor/tools/weapons to reclaim a *portion* of eligible raw materials, plus an appropriate fee. Durability, enchantments, tier exclusions and rounding must be exploit-audited; **no return percentages/fees approved**.
2. **Special prepared food orders** at relevant food professions (farmer/butcher). Differentiated expedition-use food, not mandatory new food subtrees and not duplicates of permanent food already in MM. **Exact recipes/effects are not approved.**
3. **MM exploration map orders** at cartographers, for *ordinary* MM-added structures. Validate an actual destination **before** charging the player; maps to special rare dungeons remain rare caravan territory. No wait queue.
**Rejected T2 additions:** fishing gear orders, brewing/alchemy orders, cosmetic equipment modifications, special building block production.

### T3 — hybrid premium market [APPROVED]
- All T3 territories offer a compact shared high-end exchange/service set, **plus a small number of biome-specialized products/services**; don't create five separate complex economies.
- **Plains:** agriculture/food; **Bountiful Lunchbox (풍요의 도시락)** is selected as its signature T3 item. Earlier suggested effect (slower hunger drain etc.) has **not separately been approved as a precise contract**.
- **Desert:** **ancient artifacts/treasure market** using the *existing wearable artifact system*, not decorative collectibles or a parallel treasure-slot/museum system. Detailed roster and approval caveats in companion artifact document.
- **Savanna:** exploration/structure loot submitted in exchanges.
- **Taiga:** pillager/raid-related loot submitted in exchanges. This **supersedes the earlier savanna=pillager, taiga=exploration assignment**.
- **Snowy:** alchemy and scarce Nether/End material exchange *direction*; exact offerings remain open.
- Biome identity is principally about **trade inputs and availability**, not mandatory unique gear per biome.
- Premium trade recipes, stock intervals, prices, discounts and arbitrage balance **remain open**. Earlier sample offers such as diamond+gold→netherite scraps, 4–6 offers per biome, 7-day rare restock are illustrative proposals, **NOT user-approved requirements**.
- Rare merchant caravan remains a separately designed content axis; do not duplicate its exclusive maps, very rare upgrade items, or enchantment/rune offers in normal T3 trades without specific approval.

## 4. Design/implementation gates
- The T3 mercenary rules are in companion document; guards and mercenaries MUST remain distinct.
- New trading UI may extend existing villager experience but must not accidentally bypass vanilla economics or research entitlements.
- All actions server-authoritative; persistence and multiplayer ownership need implementation validation.
- Explicitly audit barter cycles / infinite conversion / self-repurchase, invalid map locations, equipment-recycling duplication, discounts at floor prices, and stock refill timing.
- This document records **design authority**, not executed work, CI PASS, or live acceptance. No source code was changed by this checkpoint.

## 5. Open decisions / not approved
- Territory ownership claim/loss, per-player territory count, village liberation and detailed claiming UI need re-confirmation rather than assumption from old proposals.
- Individual profession trades, T2 food recipes, dismantle coefficients and excluded tiers.
- T3 detailed trade roster/prices/stock and desert artifact exact effects/rarity (except preexisting Sands of Time mechanics).
- Merchant placement mechanics, pathing/expansion validation, guard balancing under actual combat.

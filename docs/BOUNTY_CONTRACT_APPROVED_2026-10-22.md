# Bounty & Contract System — Approved Design (2026-10-22)

Status: **APPROVED DESIGN; IMPLEMENTATION PENDING**. This authority records approved decisions Q1-Q62. Design approval does not claim gameplay implementation or verification. Registry identities remain subject to repository reconciliation; no unsupported MM mob or item IDs are asserted.

## BC-03 owner-approved implementation overlay (2026-10-22)

These decisions supersede the remaining OPEN statements below where they address the same question. This section records owner decisions; it does not assert runtime validation.

- Ordinary combat targets are **hostile mobs only**. Passive, neutral, tameable, and friendly mobs are excluded. Boss or special targets need explicit eligibility and currently available content. The 40 registered MM mobs are a classification roster, not an automatic combat pool.
- High-value delivery quantity exceptions are authorized as **PROVISIONAL BALANCE**: `minecraft:diamond` 2–4 and `moreminecrafts:adamantium_ingot` 1. No other high-value exception is approved. These may override general delivery minima.
- Bonus rewards use primarily vanilla resources and materials and selected verified MM materials. Ordinary equipment, relics, accessories, and exclusive dungeon or boss rewards are excluded. Exact item identities, rarity assignment, and item-specific counts must be explicit and auditable; BC-02 hypothetical emerald equivalents are not real prices. Any unresolved exact pool entries are deferred, not invented.
- Exploration credits entry into an actual registered structure boundary **after acceptance**, including revisiting a previously visited structure. Map ownership, proximity, and unrelated structures do not count. Detection is server-authoritative; disable an objective if reliable boundary detection is unavailable.
- Eligible structure loot sources have separate contract appearance rates of Easy 5%, Medium 10%, Hard 18%. Conditional tier distributions are Easy 75/22/3%, Medium 45/40/15%, Hard 20/45/35% for Beginner/Intermediate/Advanced. These rates apply only to explicitly eligible containers.
- Advanced acquisition converts to a distinct special contract 5% of the time. Direct special-contract drops from structures or events are **DEFERRED**; no direct-drop rate is approved. Objectives for unavailable MM structures, meteors, and disabled events are excluded.

### Owner decisions — BC-03C reward pool, loot sources, and special economy (2026-10-22, APPROVED)

These decisions were approved by the owner in the BC-03 continuation request and are recorded here as current authority. They refine Q27–Q35 and Q41–Q42 and do not change them otherwise.

- **Additional reward pool (explicit).** Each additional reward is one item drawn uniformly from its rarity's list, then sized by the Q31/Q35 quantity band for that rarity, clamped by the item cap below:

  | Rarity | Items |
  | --- | --- |
  | Common | `minecraft:coal`, `minecraft:copper_ingot` |
  | High-grade | `minecraft:iron_ingot`, `minecraft:gold_ingot` |
  | Rare | `minecraft:diamond`, `moreminecrafts:titanium_ingot` |
  | Legendary | `moreminecrafts:adamantium_ingot` |

  Item-specific reward caps (PROVISIONAL BALANCE): `minecraft:diamond` maximum 2; `moreminecrafts:titanium_ingot` maximum 4; `moreminecrafts:adamantium_ingot` maximum 1. Caps apply to reward quantity only and do not change the delivery exceptions above. Both MM item IDs are registered in `ModItems`; no other MM item is in the reward pool.
- **Eligible vanilla loot sources (explicit, verified IDs required before wiring).** Easy: mineshaft, desert pyramid. Medium: jungle temple, trial chamber reward. Hard: ancient city, bastion treasure. Contract appearance remains Easy 5%, Medium 10%, Hard 18%, with the conditional Beginner/Intermediate/Advanced distributions already recorded in Q41 and the Oct 22 overlay. Integration must add to these sources without replacing or reducing their existing vanilla loot.
- **Special contract economy (explicit).** Special contracts produced by the 5% advanced-to-special conversion keep the Advanced purchase price 12–20 emeralds, Advanced emerald payout 25–50 emeralds, and special bonus rarity distribution 20/45/30/5 (Common/High-grade/Rare/Legendary). No direct special drops are introduced from structures or events.

### Owner decisions — BC-03 final continuation (2026-10-22, APPROVED)

These decisions refine the sections above. They do not change the approved Q1–Q62 rules except where stated.

**Decision A — Combat objective pools.**
- Ordinary combat targets are **hostile** mobs only. Passive, neutral, tameable, friendly, boss, and event-only entities are excluded from ordinary pools.
- Hostility is verified from entity behavior (`Enemy` interface, and not `NeutralMob` or `TamableAnimal`), not from name or spawn configuration.
- Approved initial **strong MM roster**: `moreminecrafts:large_zombie`, `moreminecrafts:armored_skeleton`, `moreminecrafts:lesser_wither_skeleton`, `moreminecrafts:large_spider`, `moreminecrafts:swamp_spider`, `moreminecrafts:lesser_magma_cube`, `moreminecrafts:deep_sea_drowned`, `moreminecrafts:bandit_warrior`, `moreminecrafts:cave_creeper`.
- Other verified hostile MM mobs enter the ordinary pool. Explicitly excluded: `moreminecrafts:feral_wolf`, `moreminecrafts:cave_bat`, `moreminecrafts:giant_bee`. `moreminecrafts:wild_piglin` is included only if verified hostile at runtime.
- Vanilla strong/ordinary split (implementation rule, PROVISIONAL): a non-boss hostile vanilla mob is **strong** when its `MAX_HEALTH` attribute is at least 40; otherwise ordinary. Vanilla mobs that spawn only through raids, conversion, or summoning are excluded from ordinary pools. The full registry-ID classification is in `src/main/java/dev/itzgamesgd/moreminecrafts/bounty/ContractCatalog.java`.

**Decision B — Combat target weighting (PROVISIONAL BALANCE) — weights SUPERSEDED 2026-10-23 by the combat roster decisions below.** After a combat objective is selected, its ordinary/strong target is a conditional draw:

| Tier | Ordinary | Strong |
| --- | ---: | ---: |
| Beginner | 90% | 10% |
| Intermediate | 70% | 30% |
| Advanced | 50% | 50% |

Quantity bands are unchanged. Targets are filtered by dimension and accessibility. Mixed-dimension combat bundles are not generated: all combat objectives in one contract share one dimension (Beginner: Overworld; Intermediate: Overworld or Nether; Advanced: Overworld, Nether, or End; uniform among allowed dimensions — PROVISIONAL IMPLEMENTATION CHOICE). An empty filtered pool yields a valid alternative, never an invalid objective.

### Owner decisions — BC-03 combat roster (2026-10-23, APPROVED)

These decisions supersede the Decision B weight table and refine Decision A. Quantity bands, hostile-only eligibility, availability and dimension safeguards, and shared progress are unchanged.

- **Combat-class weights:** Beginner ordinary 100% / strong 0%; Intermediate 75% / 25%; Advanced 50% / 50%.
- **Strong MM roster:** the nine mobs approved in Decision A are preserved.
- **`moreminecrafts:wild_piglin`:** excluded from generated objectives pending live aggression verification.
- **Special-only candidates:** `minecraft:elder_guardian`, `minecraft:warden`, `minecraft:ghast`. They are not generated by ordinary combat objectives and stay unavailable until an appropriate special objective is implemented and validated.
- **Minimum tier:** `guardian`, `breeze`, `silverfish`, `phantom`, `moreminecrafts:lesser_phantom` and `vindicator` require Intermediate or higher. `shulker` requires Advanced. All End combat targets require Advanced.
- **Missing strong candidates:** when a strong draw has no valid candidate for the contract's tier and dimension, an ordinary target is never silently substituted. The objective set is rejected and regenerated.
- **Metadata corrections:** `minecraft:piglin_brute` is always hostile; `minecraft:zombie_villager` can spawn naturally. Neither has an owner-approved combat classification, so neither is pooled.

**Decision C — Abandonment.**
- Abandonment is allowed in both OFFERED and ACCEPTED states, after explicit UI confirmation.
- Successful abandonment destroys exactly the selected contract item (validated by contract ID and inventory slot). It awards no reward and records no completion.
- Abandonment of an already-redeemed contract is refused.

**Implementation choices recorded for owner review (not new design):**
- Objective category selection among eligible categories (Combat, Delivery, Exploration) is uniform (PROVISIONAL IMPLEMENTATION CHOICE; no category weights were approved).
- Exploration objectives use the accessible Overworld subset of the BC-01 structure catalog for every tier (PROVISIONAL; the tier matrix remains OPEN).
- Delivery objectives draw from the BC-01 vanilla delivery catalog with approved count bands. Diamond delivery is fixed at 2–4 and adamantium ingot delivery at 1, per the Oct 22 exceptions. At most one high-value delivery (diamond or adamantium) per contract (PROVISIONAL economic feasibility rule).
- Purchase prices in villager and wandering-trader offers are fixed per offer entry at the approved band midpoint (PROVISIONAL). Emerald payouts are rolled within the approved band on acquisition.
- Contracts from trades and loot are created as pending items carrying only their tier; objectives and rewards are rolled on the first server-side inventory pickup.
- Structure contract appearance (5% / 10% / 18%) and tier distributions apply as approved to the six loot tables. The Trial chamber source is `minecraft:chests/trial_chambers/reward` only; the ominous and unique variants are not included.
- **Seller stock is enforced exactly (supersedes the earlier "approximated" note).** One stock unit is one contract offer with `maxUses = 1`, so a seller shows exactly its approved number of contract offers. Villagers (Farmer, Fisherman, Fletcher, Weaponsmith, Toolsmith, Cartographer, Cleric) show Beginner 2 from Apprentice, plus Intermediate 1 from Journeyman, plus Advanced 1 from Expert, and keep them at Master. A wandering trader draws Beginner 1–2, Intermediate 0–1, and Advanced 0–1 once per trader, uniformly inside each approved range (PROVISIONAL IMPLEMENTATION CHOICE: the owner approved ranges, not a distribution). Vanilla offers are untouched. Contract offers have xp 0 and a zero price multiplier, so they neither level a villager nor drift in price.
- **OPEN OWNER DECISION: restock.** Vanilla `Villager.restock()` resets the uses of every offer, so contract stock currently replenishes with vanilla restocking (up to twice a day). The approved stock quantities do not say whether sold contract stock should refill. Preventing a refill would be a new gameplay rule, so it is not implemented. Wandering traders never restock.
- **Completion is a durable transaction (implementation constraint, not a design change).** Redemption writes a pending ledger entry, applies the change to the player's data with a receipt, saves the player file as the commit point, and then finalizes the ledger; recovery on player join resolves any unfinished transaction from the player's own receipt. Q53 overflow behavior is unchanged: the remainder is dropped at the player's location.

## Acquisition and item lifecycle
- Contracts are physical items. Wandering traders are the main vendor; selected ordinary villagers also sell tiered contracts. Structure chests, trial rewards and world events may award contract items.
- Vendor listing identifies contract tier; an individually rolled objective set and reward set are fixed **when the purchased item is acquired**. Loot-generated contracts likewise resolve on acquisition. The seller does not reveal the particular rolled objectives beforehand.
- Right-click a contract to open a **single-screen UI**, including after acceptance. Show objectives, counters, required inventory items, fully determined rewards, status and appropriate accept/complete/abandon controls.
- Accepting does **not** consume the item. Unlimited accepted contracts can coexist. The contract item carries identity and progress; transfer to another player preserves its progress.
- Completion is **manual**, via the UI button after authoritative validation of recorded counters and inventory quantities. On successful completion, consume required delivery items and the contract, and grant the precommitted reward **exactly once**. Abandonment destroys the contract.
- Direct kills count, as do qualifying assists under Q48/Q51/Q58/Q59: the player dealt at least 20% of the mob’s maximum health as qualifying damage during the 30 seconds before death. Attribution is server-authoritative. Healing, damage caps/sources, and despawning edge cases remain engineering verification items. Explore targets count on visits **after acceptance**, including revisits to previously discovered places. A qualifying action advances every applicable active contract, including duplicates.
- Implementation must prevent double redemption, item duplication exploits and cross-player completion races. How item-bound progress updates and event attribution are persisted is an engineering decision constrained by these rules.

## Tiers and categories
- Three contract tiers: beginner, intermediate, advanced. Four objective categories: combat, delivery, exploration, special. Special is a **distinct contract item** rather than a fourth difficulty tier.
- Objective slots: beginner 1; intermediate 1-2; advanced 2-3. Generate objectives from **tier-weighted random pools**. Keep individual counts and total effort separate; no arbitrarily impossible mixed bundles.
- Approved count bands, per objective:
| Objective | Beginner | Intermediate | Advanced |
| --- | --- | --- | --- |
| Ordinary mob kills | 8-16 | 20-40 | 40-80 |
| Strong mob kills | 2-4 | 5-10 | 10-20 |
| Ordinary item delivery | 16-32 | 32-64 | 64-128 |
| Rare item delivery | 2-4 | 4-12 | 8-24 |
| Exploration locations | 1 | 1-2 | 2-3 |
- Tier access: beginner predominantly Overworld; higher tiers may include Nether/End and harder structures. Special targets integrate world events, trials and new structures. Special contracts may roll rarely from advanced contract acquisition or appear as structure/event rewards.
- Delivery requires current inventory quantity at completion, then deducts it. Kill and exploration counters are recorded after acceptance.

## Expanded target pools (approved candidate categories, actual registry reconciliation required)
- **Combat:** include eligible vanilla mobs and **all 40 MM-added mobs** in the classification pass. Ordinary and strong mobs enter tier-weighted random combinations; exceptional, boss, event-only or uniquely summoned mobs are excluded from ordinary random combat and reserved for special objectives as appropriate. Classify by combat difficulty, spawning availability and access dimension. Duplicate objective entries are allowed, including repeated mob or item targets. A qualifying event advances every matching entry simultaneously; each keeps its own requirement and counter. One qualifying event may also advance multiple accepted contracts. Avoid impossible cross-dimension bundles. Do not invent a list of the 40 MM mobs: reconcile against actual registry before implementation.
- **Delivery:** farming: wheat, potatoes, carrots, beetroot, sugar cane, pumpkins, melon slices, cocoa beans, sweet berries, glow berries, cactus, hay bales; forestry: logs by type, bamboo, saplings, leaves, sticks; mining: cobblestone, dirt, coal, iron/copper/gold, redstone, lapis, quartz, diamond, emerald, amethyst shards, flint, deepslate, obsidian; mob drops: bones, string, gunpowder, ender pearls, blaze rods, rotten flesh, spider eyes, slimeballs, magma cream, leather, feathers; aquatic: fish, ink sacs, prismarine shards, seagrass, kelp, nautilus shells; crafted: glass, bricks, paper, books, copper blocks, torches, chains, iron bars, flower pots. MM fish/materials are candidate extensions pending verified item registries. Respect economic value; rare expensive items must not automatically inherit the maximum count.
- **Exploration:** eligible Overworld/Nether/End biomes and vanilla structures (villages, temples, mineshafts, strongholds, monuments, mansions, trial chambers, ancient/end cities, fortresses, bastions), plus verified new MM structures. Require verifiable entry rather than mere map ownership.
- **Special:** elite pillager/event pursuit, meteor investigation, trial encounters and new-structure challenges; never generate a contract for an unimplemented or unverifiable event/structure.

## Rewards and trade
- Mixed reward: emeralds plus materials/consumables plus tier-weighted rarity roll. Four extra-reward rarities **common, uncommon/high-grade, rare, legendary** (Korean: 일반/고급/희귀/전설). Objective and reward are finalized at acquisition and shown before acceptance. After purchase, the full objective list, quantities, and fixed rewards are visible; the wandering trader does not reveal rolled contents before purchase.
- **Approved purchase-price / emerald-reward ranges:** beginner 2–4 / 5–10; intermediate 6–10 / 12–24; advanced 12–20 / 25–50. These are approved limits. Apply effort and delivery-cost adjustment within each tier’s range; reject or rebalance delivery combinations that cannot be economically reasonable within the tier payout limit.
- Q27–Q62 decisions below approve rarity probabilities, additional-item quantities, trader stock, eligible villager professions and unlock levels, purchase/reward ranges, special-contract conversion chance, structure-container probabilities, combat contribution threshold, duplicate handling, UI information, and delivery semantics. Still-open details are called out explicitly below: exact registry identities and item IDs, item-specific value caps, direct special-reward probabilities, structure tier matrices, remaining economic selections, and engineering edge cases.
- Balance explicitly against delivery opportunity cost, expected playtime and unlimited simultaneous contract stacking. Do not silently change the approved multi-contract shared-credit rule to mitigate farming.

## Remaining design and engineering work
1. Reconcile the exact 40 MM mob registry entries and classify each ordinary/strong/special, access, and spawn conditions without inventing identities.
2. Establish final eligible item IDs and item-specific value caps, including any MM fishing/material additions.
3. Decide direct special-reward probabilities, exact structure difficulty-to-tier matrices, and any remaining numeric objective weights or effort rules.
4. Verify combat edge cases (healing, damage caps/sources, despawning), item progress synchronization across transfers, duplicate-ID prevention, simultaneous redemption, and crash-safe completion.
5. Implement and validate the approved single-screen UI and all gameplay rules in a separately authorized implementation task. No gameplay implementation is authorized or claimed by this design document.


## Approved decisions Q27–Q62

### Q27–Q35: rewards, pricing, and stock

- **Q27 — Additional-reward rarity probabilities.** These probabilities apply to the additional item reward rarity roll:

  | Tier | Common | High-grade | Rare | Legendary |
  | --- | ---: | ---: | ---: | ---: |
  | Beginner | 80% | 17% | 2.8% | 0.2% |
  | Intermediate | 60% | 30% | 9% | 1% |
  | Advanced | 40% | 40% | 17% | 3% |

  Each tier row totals 100%.

- **Q28 — Reward composition.** Additional rewards primarily consist of resources and materials. Equipment is excluded from the normal additional-reward pool. Preserve individual caps for exceptionally valuable items. Exclusive dungeon, boss, relic, and accessory rewards are not silently included.
- **Q29 — Pricing model.** Contract prices and emerald payouts are set within approved tier-specific ranges, considering objective difficulty, expected effort, and delivery-material cost.
- **Q30 — Special contract rewards.** Special contracts use an increased rarity distribution; they do not require an exclusive reward-item pool.
- **Q31/Q35 — Additional-item quantities.** Baseline quantities are Common 16–48, High-grade 8–24, Rare 2–8, Legendary 1–2. Item-specific value caps override these ranges when needed. Exact item selections and numeric caps remain open.
- **Q32 — Special-contract rarity.** Common 20%, High-grade 45%, Rare 30%, Legendary 5% (100% total).
- **Q33 — Wandering-trader stock per trader.** Beginner 1–2, Intermediate 0–1, Advanced 0–1.
- **Q34 — Purchase prices and emerald rewards.** The approved ranges are:

  | Tier | Purchase price | Emerald reward |
  | --- | ---: | ---: |
  | Beginner | 2–4 | 5–10 |
  | Intermediate | 6–10 | 12–24 |
  | Advanced | 12–20 | 25–50 |

  Adjust for effort and delivery cost only within these ranges. Reject or rebalance an economically unreasonable delivery objective combination rather than exceeding its tier’s payout range.

### Q36–Q45: sellers and sources

- **Q36/Q39 — Eligible villager professions:** Farmer, Fisherman, Fletcher, Weaponsmith, Toolsmith, Cartographer, and Cleric. Profession-relevant objectives receive appropriate selection weights.
- **Q40 — Villager trade unlocks:** Beginner at Apprentice; Intermediate at Journeyman; Advanced at Expert or higher.
- **Q41 — Eligible structure-container contract probabilities:** Easy 5%, Medium 10%, Hard 18%. These apply only to explicitly eligible loot containers, not every chest in the world.
- **Q42 — Special-contract acquisition:** 5% conversion chance when acquiring an Advanced contract, plus additional direct rewards from eligible high-difficulty structures and world events. Direct-reward probabilities remain OPEN unless separately approved.
- **Q43 — Ordinary villager stock:** Beginner 2, Intermediate 1, Advanced 1. Stock applies to unlocked eligible trade offers; it does not require every villager to offer every tier irrespective of unlock level.
- **Q44 — Structure difficulty weighting:** Tier distributions are weighted by structure difficulty: easy structures favor Beginner and difficult structures favor Advanced. Exact tier probability matrices remain OPEN.
- **Q45 — Special objective availability:** Generate only objectives for content implemented, enabled, and actually generatable or completable in the current world. Lack of prior player discovery does not by itself disqualify an otherwise valid structure.

### Q46–Q59: duplicates, combat credit, and item lifecycle

- **Q46 — Duplicate objectives:** Duplicate objective entries are allowed, including the same mob or item target multiple times in one contract. This supersedes the earlier duplicate-target prohibition.
- **Q47 — Duplicate progress:** One qualifying event advances all matching objective entries simultaneously, including duplicates in one contract. Each entry retains its own requirement and counter. Preserve shared advancement across multiple accepted contracts.
- **Q48/Q51/Q58/Q59 — Combat contribution:** A kill counts when the player lands the final blow, or when the player dealt at least 20% of the mob’s maximum health as qualifying damage during the final 30 seconds before death. Do not use damage dealt by other players in that player’s percentage. Attribution is server-authoritative. Healing, damage caps, qualifying damage sources, and mob despawning edge cases remain engineering verification items.
- **Q49 — Transfer:** Progress is stored on the contract item and persists when transferred between players.
- **Q52 — Death:** Contracts behave as ordinary inventory items on death. Dropped items retain identity and progress.
- **Q53 — Full inventory:** On completion, insert as much reward as possible into the inventory, then drop the remainder at the player’s location. Overflow must not trigger duplicate payout.
- **Q57 — Abandonment:** Require a confirmation dialog before destroying the contract item.
- **Q60/Q61 — Delivery:** Materials already owned before acceptance are eligible. Show current inventory quantities as live delivery availability. Consume required materials only on successful manual completion and revalidate inventory on the server at completion. A previously observed quantity is not permanently banked progress.

### Q50/Q62: completion integrity

Completion processing is server-authoritative and atomic at the design level. Persist completed contract IDs to prevent repeated redemption; validate objective counters and required inventory; consume delivery materials and the contract item; and grant precommitted rewards exactly once. Handle simultaneous requests and server interruption safely. **Crash-safe atomicity is a requirement, not an implementation claim or a tested result.**

### Q54–Q56: contract information and controls

- **Q54/Q55 — UI:** Use one vertically arranged screen in this order: (1) contract tier and status; (2) objective list and progress; (3) precommitted rewards; (4) context-appropriate accept, complete, and abandon controls.
- **Q56 — Pre-acceptance information:** Once acquired, show the complete objective list, required quantities, and fixed rewards before acceptance. This does not require a wandering trader to disclose rolled contents before purchase.

## Design integrity review

Statuses distinguish settled rules from engineering proof still required. **DESIGN CONSISTENT** means the approved rules give a coherent outcome; it does not claim implementation. **OPEN ENGINEERING VALIDATION** means implementation behavior needs verification. No case below is a conflict requiring an owner decision.

| Case | Status | Design result / remaining check |
| --- | --- | --- |
| Two duplicate combat objectives inside one contract | DESIGN CONSISTENT | One qualifying kill advances both matching entries; each counter and requirement remains independent. |
| Multiple contracts share one qualifying kill | DESIGN CONSISTENT | One qualifying event advances every applicable accepted contract. |
| Two delivery contracts require the same inventory materials | DESIGN CONSISTENT | Each checks current inventory at completion; materials are available to both until one completion consumes them. |
| First delivery completion temporarily unsatisfies the second | DESIGN CONSISTENT | The second contract remains incomplete until its materials are again present; live availability updates and server revalidation govern. |
| Contract transfer while progress updates | OPEN ENGINEERING VALIDATION | Item-bound identity/progress must survive transfer without a lost or duplicated update; synchronize the update server-authoritatively. |
| Death and item drops preserve contract identity | DESIGN CONSISTENT | Dropped contract item retains its identity and progress, like an ordinary inventory item. |
| Duplicate contract IDs attempt redemption | OPEN ENGINEERING VALIDATION | Persisted completed IDs and server-authoritative validation must prevent repeated rewards across copied items. |
| Two simultaneous completion requests for one contract | OPEN ENGINEERING VALIDATION | Atomic completion and completed-ID persistence must serialize/reject the duplicate request. |
| Reward overflow with full inventory | DESIGN CONSISTENT | Fill available inventory space, drop the remainder at the player, and prevent a second payout. |
| Server interruption during completion | OPEN ENGINEERING VALIDATION | Recovery must preserve exactly-once completion; crash-safe atomic behavior has not been proven. |
| Special objective references disabled or unimplemented content | DESIGN CONSISTENT | Such objectives are not generated; valid undiscovered structures remain eligible. |
| High-value delivery items exceed reasonable tier payout limits | DESIGN CONSISTENT | Apply item-specific value caps and reject or rebalance the objective combination within approved price/reward ranges. Exact caps and balancing remain OPEN. |

## Remaining OPEN decisions and IMPLEMENTATION PENDING

- Exact eligible MM mob and item registry identities, final objective pool entries, and implementation/content availability reconciliation.
- Exact item-specific high-value caps and the concrete eligible additional reward-item pool.
- Direct special-reward probabilities; exact structure difficulty-to-tier probability matrices.
- Any remaining objective selection weights and numerical difficulty/effort rules not set above.
- Qualifying damage edge cases for healing, damage caps, damage sources, and despawn.
- Server-authoritative persistence and transfer synchronization, completed-ID storage, simultaneous completion handling, and crash-safe atomic completion.
- All gameplay implementation and implementation validation. This authority document records design only.

# Bounty & Contract System — Approved Design (2026-10-22)

Status: **APPROVED DESIGN, NOT IMPLEMENTATION**. This document captures owner decisions Q1-Q26 and subsequent roster/algorithm approval. Any illustrative numbers or unverified MM mob names are not promoted to authority.

## Acquisition and item lifecycle
- Contracts are physical items. Wandering traders are the main vendor; selected ordinary villagers also sell tiered contracts. Structure chests, trial rewards and world events may award contract items.
- Vendor listing identifies contract tier; an individually rolled objective set and reward set are fixed **when the purchased item is acquired**. Loot-generated contracts likewise resolve on acquisition. The seller does not reveal the particular rolled objectives beforehand.
- Right-click a contract to open a **single-screen UI**, including after acceptance. Show objectives, counters, required inventory items, fully determined rewards, status and appropriate accept/complete/abandon controls.
- Accepting does **not** consume the item. Unlimited accepted contracts can coexist. The contract item carries identity and progress; transfer to another player preserves its progress.
- Completion is **manual**, via the UI button after authoritative validation of recorded counters and inventory quantities. On successful completion, consume required delivery items and the contract, and grant the precommitted reward **exactly once**. Abandonment destroys the contract.
- Direct kills and sufficiently credited assists count; exact assist threshold remains OPEN. Explore targets count on visits **after acceptance**, including revisits to previously discovered places. A qualifying action advances every applicable active contract, including duplicates.
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
- **Combat:** include eligible vanilla mobs and **all 40 MM-added mobs** in the classification pass. Ordinary and strong mobs enter tier-weighted random combinations; exceptional, boss, event-only or uniquely summoned mobs are excluded from ordinary random combat and reserved for special objectives as appropriate. Classify by combat difficulty, spawning availability and access dimension. Avoid duplicate entries for the same mob in a single contract; avoid impossible cross-dimension bundles. Do not invent a list of the 40 MM mobs: reconcile against actual registry before implementation.
- **Delivery:** farming: wheat, potatoes, carrots, beetroot, sugar cane, pumpkins, melon slices, cocoa beans, sweet berries, glow berries, cactus, hay bales; forestry: logs by type, bamboo, saplings, leaves, sticks; mining: cobblestone, dirt, coal, iron/copper/gold, redstone, lapis, quartz, diamond, emerald, amethyst shards, flint, deepslate, obsidian; mob drops: bones, string, gunpowder, ender pearls, blaze rods, rotten flesh, spider eyes, slimeballs, magma cream, leather, feathers; aquatic: fish, ink sacs, prismarine shards, seagrass, kelp, nautilus shells; crafted: glass, bricks, paper, books, copper blocks, torches, chains, iron bars, flower pots. MM fish/materials are candidate extensions pending verified item registries. Respect economic value; rare expensive items must not automatically inherit the maximum count.
- **Exploration:** eligible Overworld/Nether/End biomes and vanilla structures (villages, temples, mineshafts, strongholds, monuments, mansions, trial chambers, ancient/end cities, fortresses, bastions), plus verified new MM structures. Require verifiable entry rather than mere map ownership.
- **Special:** elite pillager/event pursuit, meteor investigation, trial encounters and new-structure challenges; never generate a contract for an unimplemented or unverifiable event/structure.

## Rewards and trade
- Mixed reward: emeralds plus materials/consumables plus tier-weighted rarity roll. Four extra-reward rarities **common, uncommon/high-grade, rare, legendary** (Korean: 일반/고급/희귀/전설). Objective and reward are both finalized at item acquisition and displayed before acceptance.
- Previously discussed **illustrative, NOT approved** economic bands: beginner contract cost 2-4 emeralds / emerald reward 5-10; intermediate 6-10 / 12-24; advanced 12-20 / 25-50. These are placeholders pending balancing, not fixed economy rules.
- Exact rarity probabilities, item pools, item quantities, trader stock, villager professions, special-contract chance, contribution threshold, structure loot rates and final UI art are **OPEN**.
- Balance explicitly against delivery opportunity cost, expected playtime and unlimited simultaneous contract stacking. Do not silently change the approved multi-contract shared-credit rule to mitigate farming.

## Acceptance and next design work
1. Verify the exact 40 MM mob registry entries and classify each ordinary/strong/special, access and spawn conditions.
2. Establish final eligible item IDs and delivery-specific cost caps, including MM fishing/material additions.
3. Decide four-tier reward probability matrix and concrete reward pools, prices, stock and source loot weights.
4. Resolve kill-assist credit threshold, objective combination effort cap, multiplayer item-progress synchronization and completion atomicity.
5. Design single-screen UI layout and validation tests. No gameplay implementation authorized by this document alone.

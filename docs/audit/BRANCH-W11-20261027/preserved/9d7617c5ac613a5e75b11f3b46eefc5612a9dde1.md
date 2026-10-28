# Stage 23-A remaining research — user decision checkpoint (2026-10-17)

**Status: user-approved design checkpoint; amended topics remain OPEN for follow-up details. DESIGN ONLY; no implementation or merge authorization.**

Parent: Draft PR #109 at exact HEAD `9376777264136b37e10071adf8ed59c08da9b6ee` (PR #108 base). Do not modify PR #105, #107, #108, or #109. This separate branch is a design record. Existing authoritative documents retain precedence for all previously locked numbers, purchase gates, IDs, costs, and topology unless the user's specific amendment below overrides them.

## Scope and approval

The user explicitly approved **all proposals in the immediately preceding 47-item remaining-research design response EXCEPT the topics they named for amendment or further explanation**. This covers the 45 DATA_ONLY nodes and the 2 formerly BLOCKED nodes as design topics, not implementation completion. The user's amendments below supersede the corresponding earlier assistant proposals. An assistant-suggested detail is NOT automatically user-approved where this record labels it pending.

Preserve existing Stage 22 design authority, including approved furnace/boat/brewing/automation architecture and transactional rules. Do not change research audit statuses merely because a design was approved: latest PR #109 audit is 45 DATA_ONLY, 40 PARTIAL, 2 BLOCKED, 0 COMPLETE. Preserve Draft status and live verification gates.

## Explicit amendments from the user

1. **V3 .05 Auto Lumber — revised processing and animation (further details pending).** Add an autonomous startup animation when work begins. First select ONE log/tree target, then mine connected logs sequentially at **one block per tick**, faster only if safely feasible and measurably better for server load. After that log/tree's log blocks have all been removed, process associated leaves automatically into their normal drops (apples, sticks, saplings, etc.) preserving vanilla-compatible loot. Then search for another log/tree; insert a short tick delay before the next mining cycle. Favor bounded scans, chunk-local loaded-only work, no force-loading, no block duplication, and minimal server cost. This overrides the previous 4-tick-per-log cadence. Exact definition of one target/tree, leaf association and delay remain to be specified.
2. **V3 .12 High-speed/high-efficiency furnaces — detailed design requested.** Prior authority mainly unlocked recipes for new 3x/5x/7x furnaces; user requests concrete recipes, tier progression, fuel and cooking accounting, ownership, and UI/lifecycle rules. Do not treat assistant's earlier one-line description as final detail.
3. **V3 .16 Soil Study L3 — REPLACE old weak 5-minute post-water hydration retention.** New L3 must extend the irrigation/water coverage of farmland and increase growth speed of crops on that farmland. Preserve L1/L2 unless contradicted. Exact water radius and growth bonus to specify; old 5-minute effect is superseded.
4. **V3 .22 Throw Feed/Breeding — add animal AI.** Animals must notice the thrown feed and approach it appropriately, with vanilla breeding/growth/cooldown constraints. Specify targeting priority, pathfinding, consumption and multiplayer ownership without excessive scans.
5. **V3 .46 Rare Mob — explicitly include rare rider/mount combinations.** User positively approves treating rare mounted combinations (including skeleton horse/zombie horse and spear-bearing or camel rider combinations where they actually exist) as candidates for explicit taxonomy. Use versioned, verified entity tags; do not invent nonexistent mob entities. Audit actual 26.3/custom entity registrations.
6. **V3 .53 Villager Trading — CHANGE discount to L1/L2/L3 = 5%/10%/15%**, replacing 2%/4%/6%. Preserve stock UI, player-specific discount and minimum price 1; update design, tooltip, tests and any applicable authoritative data only when implementing.
7. **V3 .54 Piglin Bartering — user clarifies a villager-like trading GUI**, not just a silent loot-table rarity modifier. Need reconcile which specific research/UI this refers to, with existing V3 .54 and V4 `piglin_barter`, before fixing the exact mechanics. Do not silently overwrite existing reward-weight rules or claim the GUI is already specified.
8. **V4 `throwable_pickaxe` — detailed design required.** Mandatory automatic return, visible spin while airborne, enchantment interaction and vein-mining behavior, with item identity, durability, drop handling, owner disconnect, inventory-full and unloaded chunk safeguards. No duplicate items.
9. **V4 `ballistic_control` — trajectory preferred.** Show actual predicted trajectory if technically feasible; otherwise show a vertical impact-location line visible **only to the local player**. No auto-aim and no global effects.
10. **V4 `falling_axe_smash` — reuse mace mechanics.** Axe falling smash should reuse the existing mace-like smash calculation/feel, while the attacker's own fall damage is reduced by approximately **30%**, NOT fully cancelled. Add a **subsequent research level** unlocking an anvil enhancement that combines an axe and mace. Apply simple vanilla anvil prior-work/repair-cost count rules. Exact bonus and upgrade level/price/knowledge topology, plus how this interacts with the existing one-level V4 catalog, require explicit reconciliation before implementation. Do not invent a new research ID or silently mutate purchase ledger.
11. **V3 .35 Tactical Shooting — proceed with design/implementation instead of deferring indefinitely.** User likes the proposed smoke/splitting/illumination arrow directions; detailed numbers, recipes and interactions may be designed later, but do not treat indefinite deferral as the user's intent. Existing BLOCKED status remains until complete spec/implementation and validation.

## Previously approved topics not reopened

The user explicitly stated: **all prior proposals NOT mentioned in their amendment message are unconditionally approved**. That includes the previously proposed shared owner/transaction/chunk/sync policies and unmentioned research rows. Existing higher-priority approved specifications prevail over an assistant summary where they differ. No implied approval of extra costs, item recipes, registry identities or values absent from those proposals. In particular V3 .58 cross-dimensional item transport keeps its existing durable exactly-once queue and crash-recovery requirements.

## Next design pass required

Before asking for implementation, fill only remaining gaps, starting with .05 tree/leaf processing and cadence, .12 tiered furnace recipes and progression, .16 water radius/growth formula, .22 feed-attraction AI, .54 V3/V4 piglin trading GUI semantics, throwable pickaxe return/vein-mining/enchanting, and falling-axe-smash level-2 purchase/anvil mechanics. Reconcile with exact catalog and current game entities; produce a fully enumerated, non-contradictory authority update. Do not claim design closure until user approves these amendments. Keep existing PRs untouched.


---

# FINAL USER APPROVAL — 2026-10-17 (supersedes OPEN follow-up design notes above)

**STATUS: APPROVED DESIGN AUTHORITY.** The user explicitly approved the previously unmentioned 47-topic design proposals and all the recommendations discussed in the subsequent eight-item clarification, with the precise overrides below. The earlier OPEN/pending notes are historical and superseded wherever this section gives a rule. This is DESIGN APPROVAL ONLY, not proof of implementation or live acceptance. Keep existing research costs/IDs/levels except the explicitly approved new second level for `falling_axe_smash`, which requires a catalog migration and explicit price definition before purchase activation. Do not change PR #105/#107/#108/#109, merge, or promote Draft.

## 1. V3 .05 Auto Lumber — final operation

- Select a starting log; tree identification must not rely on logs touching each other: logs may be disconnected while **leaves are connected**. Search a bounded, loaded-only tree canopy/log graph using verified leaf association and natural-tree heuristics, avoiding adjacent unrelated trees and player structures as far as safely identifiable. No force-loading.
- One log block is mined per tick during the active cutting phase; optimize to faster only if safe and server-load justified. When the entire identified tree's logs are gone, process its associated leaves and emit vanilla-compatible loot (saplings, apples, sticks, etc.) once, without double drops. Then find the next tree and wait **10 ticks** before cutting again.
- Add startup/work animations and completion transition; visual effects must not require excessive entity spawns, chunk scans or packet spam.
- Preserve previously approved fuel 1 coal per 8 logs, tool wear 1 per 3 logs, no inventory, output drop in place, atomic/persistent progress and loaded-chunk safeguards. Tree identification and leaf loot must be tested against disconnected logs and connected foliage.

## 2. V3 .12 3x/5x/7x furnace — EXACT CRAFTING RECIPES

3x furnace (crafting grid indices 1..9, row-major):
```
iron_ingot | furnace    | iron_ingot
furnace    | iron_block | furnace
iron_ingot | furnace    | iron_ingot
```
5x furnace:
```
iron_block | diamond    | iron_block
diamond    | 3x_furnace | diamond
iron_block | diamond    | iron_block
```
7x furnace:
```
gold_ingot       | netherite_ingot | gold_ingot
netherite_ingot  | 5x_furnace      | netherite_ingot
gold_ingot       | netherite_ingot | gold_ingot
```
Each produces ONE next-tier furnace. The 3x/5x/7x multipliers apply to **both processing speed and fuel efficiency**. The furnace family supports ordinary furnace recipe types (not automatically blast/smoker specialized recipes). Existing .06/.07/.11 research effects compose through the shared research accounting layer without duplicated bonuses, overflow or item loss. Server-authoritative fuel fractions, progress, output reservations and persistence; GUI, block break, hopper, unload/restart and zero-fuel behavior must preserve all items. These recipes override any incompatible earlier proposed recipe.

## 3. V3 .16 Soil Study — final L3

- Replace the old 5-minute residual hydration rule completely.
- At L3, valid farmland water/hydration coverage extends from vanilla horizontal distance 4 to **6 blocks**, and natural crop-growth probability on qualifying farmland gains **+20% relative** (multiply by 1.20, capped at 100%; not an unconditional growth-stage jump).
- Preserve L1 trample prevention and L2 +30% eligible seed yield. Attribute to the recorded farmland cultivator; handle unknown-owner farmland with the previously proposed nearby 16-block qualifying researcher fallback. Do not stack nearby players, force-load chunks or duplicate the Living Root +1% growth roll; compose independent bonuses once.

## 4. V3 .22 Thrown Feed — final animal AI

- Animal detection region is **5×5 horizontal footprint** centered on the thrown food, not 12-block radius; use vanilla-compatible vertical reach/pathfinding and loaded entities only.
- No artificial cap on attracted animal count; ordinary entity crowding/collision and the user's cited 23-entity cramming behavior are the only limiting mechanics (do not hardcode a new cap or change the world's entity-cramming gamerule).
- Animals currently unable to breed/eat due to vanilla eligibility/cooldown **must not aggro or path toward** the feed. Eligible animals path toward the feed and consume only actual available food items in deterministic nearest-first order. Keep vanilla breeding, growth and cooldown rules, and thrower purchase ownership. Avoid costly per-tick world scans.

## 5. V3 .54 and V4 piglin_barter — final functional split

- V3 .54 retains the approved rare-barter reward weight +5/+10/+15% across L1–3.
- V4 `piglin_barter` (one existing purchased level) unlocks a **villager-like trading GUI** for eligible piglins; ordinary gold-ingot throw bartering stays available. Keep server-authoritative offers, inventory transactions and no dupes.
- The previously proposed V4 extra +5% rarity modifier is **replaced** by GUI unlock: do not silently apply that bonus in addition to V3 .54.
- Exact trade stock/restock and offers must be derived from the existing piglin barter reward table where possible; do not invent guaranteed rare rewards or an economy exploit. Any implementation-specific offer-table values not covered by existing authority must be surfaced for a separate content review, not guessed.

## 6. V4 throwable_pickaxe — final operation

- Throw distance max **24 blocks**; visible spinning flight and **automatic return** to its owner. On valid block hit, mine the impact block and a connected ore vein up to **16 blocks**, with normal tool eligibility, drop rules, protection and loaded-only checks. Prevent infinite chain, duplicate tool entities and item duplication.
- Preserve applicable Efficiency, Fortune, Silk Touch, Unbreaking and Mending semantics; no arbitrary enchantment stripping or recursive drops. Damage/tool consumption per successfully mined block under normal durability rules unless an already approved specific item rule overrides.
- Owner logout/death, inventory-full return, failed hit and unloaded-area crossing must resolve via durable single-tool ownership/recovery rather than spawning duplicates or deleting the tool. Returning animation is cosmetic; server owns the unique tool identity.

## 7. V4 ballistic_control — final presentation

- Prefer a local-only, non-authoritative **predicted projectile trajectory** matching supported bow/crossbow physics. If reliable trajectory rendering is technically unavailable, show a **vertical impact-position line visible only to the researching player**. No global particles, aim correction, auto-aim or change to server projectile physics. Bound trajectory prediction work and marker lifespan.

## 8. V4 falling_axe_smash — final L1/L2 direction

- **L1:** axe smash reuses vanilla mace-like falling smash mechanics/feel; successful falling-axe attack reduces the user's own corresponding fall damage by **30%**, not total immunity. Do not stack another invented +20% damage formula on top of mace-style calculation. Preserve vanilla landing/critical/knockback compatibility as far as practical.
- **L2:** unlock an anvil **enhancement of the axe**, consuming a compatible axe plus mace; result remains an upgraded axe, not a new unrelated weapon. Use vanilla anvil prior-work/repair-cost-count logic for the combine operation, not a custom XP price formula.
- The V4 catalog currently defines `falling_axe_smash` as a single-level node; user explicitly approved adding L2, but its research purchase cost/materials are **not specified**. Do not activate a silently invented price or change global catalog totals until a specific cost is approved. Preserve existing purchased L1 entitlements and migrate idempotently. Enchantment conflict/attribute merge, resulting axe components, and one-time combination semantics must be explicitly tested.

## 9. V3 .35 Tactical Shooting — final direction

- Three arrow families approved: **smoke, splitting, illumination**. Support both bow and crossbow. Smoke interferes with hostile mob target acquisition; splitting hits up to **3 additional valid targets** without recursive split; illumination applies a temporary Glowing effect to valid targets in an area.
- Recipes, exact radii, damage and duration values remain to be authored in a bounded follow-up specification; this is no longer indefinitely deferred by design. Keep current BLOCKED implementation audit classification until the production content and acceptance tests exist.

## Other accepted decisions and verification

- V3 .46 rare-mob taxonomy explicitly covers verified rare rider/mount/spear/camel combinations, skeleton horses and zombie horses where actually registered. Versioned tags; no invented nonexistent entity types.
- V3 .53 villager discount is **5%/10%/15%** for L1/L2/L3, not 2/4/6; player-specific, minimum cost one. Ensure tooltips and tests use new values.
- The previously recommended detailed behavior for all other unmentioned research rows is approved; preserve prior higher-authority approved constraints when they conflict with an assistant's brief summary.
- This decision checkpoint does not mark any node COMPLETE or change the 45 DATA_ONLY / 40 PARTIAL / 2 BLOCKED audit baseline. Exact current PR #109 HEAD remains `9376777264136b37e10071adf8ed59c08da9b6ee`.

**Implementation readiness:** design direction is approved. Do not silently guess unresolved new content tables or L2 research purchase price. Implement all fully specified portions and isolate truly unpriced/unlisted content for explicit follow-up, while preserving existing stable PRs.

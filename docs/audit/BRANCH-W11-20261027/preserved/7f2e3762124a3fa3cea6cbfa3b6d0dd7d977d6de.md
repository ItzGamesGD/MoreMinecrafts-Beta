# T3 Desert Artifacts and Mercenary Hiring — Approved Checkpoint, 2026-10-19

Design-only approval overlay. No implementation, CI, or live validation is claimed.

## Mercenary hiring [APPROVED]
- Unlock at territory **T3**. Three jobs: **swordsman (검사), archer (궁수), crossbowman (석궁병)**. Two grades: **budget (보급형)** and **elite (정예형)**, six hire offerings.
- Up to **four simultaneous hires per player**; any job/grade combination, including four copies of one job, is valid. Hiring term **five Minecraft days**; how sleeping and time commands affect expiry requires precise game-clock design.
- Mercenaries follow and defend the hiring player, including **cross-dimensional travel**, with no hunger. On death or contract expiry they **despawn**, drop none of their gear, vacate the slot and can be rehired. Must persist ownership, contract and live entity across restarts/load/unload without duplication.
- They are not village-defense guards. Never attack hiring player, allied mercenaries or allied territorial guards; do not credit their kills for player-direct-kill bounty contracts.
- **Vanilla mannequin/player-form visual intent**: wolf-inspired following/owner protection; skeleton-inspired bow AI; pillager-inspired crossbow AI; separate simple melee attack logic. Do not assert mannequin is itself a ready-made pathfinding/Goal host: compare extending mannequin versus an AI-capable mob with mannequin-like renderer in actual Minecraft **26.3** source.
- **No custom RPG combat stat tree, class level or talent system**: gameplay damage and armor depend on real equipped gear and actual vanilla combat behavior. Stable entity base attributes/health still need implementation defaults; don't misrepresent them as already approved stats.

### Equipment
| Attribute | Budget | Elite |
|---|---|---|
| Armor | Full unenchanted iron set | Full iron armor with Protection II on **each piece** |
| Weapon | Swordsman iron sword; archer bow; crossbowman crossbow | Same profession weapon |
| Healing inventory | **One Potion of Regeneration I** | **Three ordinary golden apples** |
| Use | **Automatically when HP ≤ 50%**, consumed, no replenishment | **Automatically when HP ≤ 50%**, consumed, no replenishment |
| Hunger | None | None |
| Term | Five Minecraft days | Five Minecraft days |

Do not substitute enchanted golden apples or infinite passive potion regeneration. A recovery use should deliver vanilla-like effect and be safe under combat concurrency.

### Approved exact hire fees (emerald **blocks**)
Costs depend on **number already actively hired**, not a permanent hiring slot, player lifetime purchase count, or job. The table, not an approximate continuous formula, is authoritative.

| Active hires before hiring | Budget | Elite |
|---|---:|---:|
| 0 | **3** | **5** |
| 1 | **4** | **7** |
| 2 | **5** | **8** |
| 3 | **6** | **10** |

Four consecutive budget hires cost **18 blocks**, four elites **30**. Upon death/expiry the next price adjusts to *current* active count. The fee scale reflects the user-approved **two-thirds reduction** from a previous draft plus budget pricing.

### Technical checks still required
Exact expiry clock under sleep, relog and server restart; safe dimension follow, chunk-unloaded companions, prevent duplicates and drops; combat AI ownership and target filters; armor enchantment preservation; healing trigger and item decrement exactly once; vanilla mob goal API compatibility; payments and shared 4-slot cap atomicity. No specific follow/stay command was explicitly finalized.

## T3 desert artifact offerings [APPROVED DIRECTION]
**The desert specializes in equippable artifacts within MM's existing accessory system, NOT inert decorative collectibles.** Reuse its **five category-restricted slots and +0 to +10 enhancement** design; the latest existing authority is `ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md`, copied into this branch. New artifacts must be assigned valid slot categories; any combat-active ability must obey the one combat slot/activation rules.

User accepted continuation of the proposed desert **eight-name market set**, but the **seven newly suggested mechanics still need overlap/balance review**. One candidate, **시간의 모래 / Sands of Time**, is explicitly **already designed** per the user: **DO NOT register a duplicate** or silently replace its established mechanic. The earlier chat describes a +3-second beneficial-effect extension on expiry / 180-second cooldown / no recursive re-extension, but its original authoritative document was **not found in the inspected main/accessory design docs**; verify against its true prior source before encoding those numbers.

| Type | Name | Status of effect |
|---|---|---|
| Common candidate | 황금 풍뎅이 (Golden Scarab) | Draft: +5% experience gained; research overlap check |
| Common candidate | 사막의 부적 (Desert Charm) | Draft: -8% fire damage; existing ember ring overlap check |
| Common candidate | 유목민의 발찌 (Nomad Anklet) | Draft: -10% hunger drain while sprinting; mobility/food overlap check |
| Common candidate | 청금석 목걸이 (Lapis Necklace) | Draft: slightly lower enchantment costs; skill tree economics check |
| Common candidate | 고대 동전 (Ancient Coin) | Draft: -3% villager trade cost; discount stacking/rounding check |
| Premium candidate | 불멸의 풍뎅이 (Undying Scarab) | Draft: fatal-hit survival with long cooldown; Totem interaction check |
| Premium candidate | 태양의 심장 (Heart of the Sun) | Draft: daylight combat buff plus recovery; exact effect open |
| **Existing, reuse** | **시간의 모래 (Sands of Time)** | **Already designed artifact**, not new; find original authoritative record before implementation |

Provisional 5 common + 2 premium new items + 1 preexisting Sands of Time. Exact prices, quantities, refresh frequencies, category assignments, loot sources and seven new effect values **not finally approved**. Do not turn the rejected old proposal of time-slowing AoE "Sands of Time" into its effect. Avoid duplicating existing `시간의 고리` (Time Ring) already found in the approved accessory roster; these are **not automatically the same item**.

## Rejected / out of scope
- All four proposed T3 battle consumables: shockwave flask, hunter's mark, guardian seal, binding orb — **rejected**, must not be implemented.
- Separate decorative treasure collection/museum systems — misunderstood idea, **rejected interpretation**.
- Old suggested elaborate mercenary leveling, skill trees, inventory management and ongoing upkeep — not approved.

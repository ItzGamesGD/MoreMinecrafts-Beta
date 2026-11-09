# MoreMinecrafts — 장신구·유물 12종 확정안 (2026-10-16)

Status: **Approved roster and behavioral direction; numeric proposals explicitly distinguished.** Documentation only, not gameplay implementation. This applies to Fabric MoreMinecrafts, not historical Paper designs.

## System contract

- **Five accessory slots**, with no finalized sub-slot layout.
- Primary acquisition through exploration, dungeons, and bosses; specific loot tables are pending.
- Twelve adopted accessories: six existing and six new.
- Effects remain accessories until a separate explicit decision authorizes migration to research, enchantments, special arrows, or other systems.
- Implementation requires server-authoritative behavior and separate CODE VERIFIED / VISUAL PENDING / BLOCKED states.

## Previously adopted six

| Accessory | Effect |
|---|---|
| 설원의 부적 | Powder snow sinking speed -70%; reduce slipping on snow/ice. |
| 심해의 진주 | Crouch motionless underwater for 2 sec → water breathing 8 sec; cooldown 45 sec. |
| 잿불 반지 | After fire damage, reduce fire damage 40% for 3 sec; cooldown 30 sec. |
| 생명의 고리 | At health ≤25%, Regeneration I for 4 sec; cooldown 120 sec. |
| 바다의 속삭임 | Fishing minigame success zone +10%; no changes to rarity, size, or weight. |
| 별의 계약 | Melee damage +8% while health ≤50%. |

## Newly adopted six

### 7. 환영의 거울 (Illusion Mirror)
**Approved:** Summon a player-looking combat clone with visually copied armor. Give it **only a sword and simple melee combat**; no inventory, crafting, mining, block interactions, item use, elemental/T3 skills, or item drops on despawn. A mannequin-like rendering base was discussed but not validated.
**Proposed numbers, not individually confirmed:** 12-sec lifetime, 120-sec cooldown, 40% melee damage, 50% player health.

### 8. 시간의 고리 (Time Ring)
**Approved:** Slow nearby mobs and projectiles while wearer stays at normal speed/tick rate. Review candidate open-source code and license/version compatibility; implement independently if unsuitable. Do not assume a particular dependency is compatible.
**Proposed numbers, not individually confirmed:** affected entities ~4 TPS-equivalent, 3-sec duration, 180-sec cooldown. Multiplayer and boss scope unresolved.

### 9. 피의 계약 (Blood Pact)
**Approved:** Spend 30% of current HP as non-mitigable activation cost; +35% damage for 12 sec; natural regeneration disabled; cooldown 90 sec.

### 10. 생명의 씨앗 (Seed of Life)
**Approved:** Area healing for allies, with user-requested stronger effect.
**Proposed numbers, not individually confirmed:** 12 sec, radius 5 blocks, heal 3 HP every 2 sec, one-time 4 absorption HP upon entry, allies and tamed animals, 150-sec cooldown, no stacking.

### 11. 중력의 심장 (Gravity Heart)
**Approved:** Pull enemies toward center.
**Proposed numbers, not individually confirmed:** radius 5 blocks, 4 sec, max 12 ordinary mobs, 100-sec cooldown, bosses immune, no terrain damage.

### 12. 영혼의 사슬 (Soul Chain)
**Approved:** Link multiple enemies so some direct damage propagates to other linked targets; user requested expanded link cap and suggested potential research progression. No recursion, no propagated-damage re-trigger, no AOE double counting.
**Proposed numbers, not individually confirmed:** 3 linked targets base, 8-block reach, 8-sec duration, 25% direct damage shared to each other target, 90-sec cooldown. Potential research caps 4/5/6 at I/II/III **not authorized as actual research tree changes**.

## Rejected / not adopted

공간의 반지 (portal), 천상의 깃털 (midair platforms), 토끼의 발걸음 (Bunny Hoppers overlap), 그림자 펜던트 (Echo Pendant overlap), 풍요의 반지 (farming research), 광맥의 눈 (natural-block provenance unresolved), 감시자의 눈 (mob detector overlap), 시간의 모래 (potion extension). Echo Pendant remains an enchantment; Crystal Heart an armor upgrade; Glowshroom Charm a prospective detection device.

## Future content axes

Accessories/relics; dungeons/trials; world events; bounties/commissions; economy/trade. Equipment cosmetics deferred.

## Implementation gates

Finalize proposed balance values, slot UI/activation, cooldown persistence, stacking, death/drop, multiplayer target rules, loot, IDs and resources. Validate Time Ring license and Minecraft/Fabric compatibility before choosing dependency. Implement from a separately verified integration checkpoint, with Java 25 build, unit tests, GameTests, smoke tests, and actual client visual checks. **This document adds no gameplay code and does not authorize edits to existing research nodes or parallel PRs.**

# Cumulative live validation backlog

Automated build and smoke coverage does not replace the following human gameplay checks. Entries
remain **LIVE PENDING** until a human records an observed result.

## Stage 3 - LIVE PENDING

- [ ] **LIVE PENDING (Stage 3):** Create the actual Iron Sword enhancement result in an anvil.
- [ ] **LIVE PENDING (Stage 3):** Taking the result consumes one Enhancement Stone and one XP level.
- [ ] **LIVE PENDING (Stage 3):** The `Enhancement +1` tooltip and Iron Sword attack bonus appear.
- [ ] **LIVE PENDING (Stage 3):** Unrelated ItemStack state survives enhancement.
- [ ] **LIVE PENDING (Stage 3):** Vanilla rename, repair, and enchanted-book operations coexist unchanged.
- [ ] **LIVE PENDING (Stage 3):** Inventory/container movement preserves enhancement state.
- [ ] **LIVE PENDING (Stage 3):** Reconnect and server restart preserve the level, tooltip, and bonus.

The detailed original procedure remains in `docs/STAGE_3_VALIDATION.md`.

## Stage 4A - LIVE PENDING

- [ ] **LIVE PENDING (Stage 4A):** Enhance a representative Sword through multiple sequential levels.
- [ ] **LIVE PENDING (Stage 4A):** Observe exact XP-cost transitions at targets +1, +6, +11, and +21.
- [ ] **LIVE PENDING (Stage 4A):** Confirm Vanilla enhancement succeeds at +30 and exposes no +31 result.
- [ ] **LIVE PENDING (Stage 4A):** Confirm unsupported items and Shield expose no enhancement result.
- [ ] **LIVE PENDING (Stage 4A):** Obtain a valid preview for every supported vanilla equipment family.
- [ ] **LIVE PENDING (Stage 4A):** Elytra carries enhancement levels but gains no enhancement stat effect.
- [ ] **LIVE PENDING (Stage 4A):** Taking a result from a stacked material input consumes exactly one stone.
- [ ] **LIVE PENDING (Stage 4A):** Vanilla anvil operations without an Enhancement Stone remain intact.
- [ ] **LIVE PENDING (Stage 4A):** Existing name, durability, enchantments, and unrelated components survive.
- [ ] **LIVE PENDING (Stage 4A):** Save, reconnect, and restart preserve multi-level enhancement state.

## Stage 4B-1 - LIVE PENDING

- [ ] **LIVE PENDING (Stage 4B-1):** Sword enhancement increases actual melee damage by +0.08 per level.
- [ ] **LIVE PENDING (Stage 4B-1):** Mace enhancement increases actual melee damage by +0.10 per level.
- [ ] **LIVE PENDING (Stage 4B-1):** Trident enhancement increases melee damage by +0.09 per level without changing thrown damage.
- [ ] **LIVE PENDING (Stage 4B-1):** Axe enhancement increases actual melee damage by +0.10 per level.
- [ ] **LIVE PENDING (Stage 4B-1):** Pickaxe mining speed increases by +0.65 per level on effective blocks.
- [ ] **LIVE PENDING (Stage 4B-1):** Shovel mining speed increases by +0.65 per level on effective blocks.
- [ ] **LIVE PENDING (Stage 4B-1):** Hoe mining speed increases by +0.65 per level on effective blocks.
- [ ] **LIVE PENDING (Stage 4B-1):** Axe receives both its mining-efficiency and melee-attack bonuses.
- [ ] **LIVE PENDING (Stage 4B-1):** Unenhanced supported equipment remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-1):** Unsupported equipment remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-1):** Enhancement state and effects survive inventory movement, reconnect, and restart through native ItemStack persistence.
- [ ] **LIVE PENDING (Stage 4B-1):** No duplicate attack-damage modifier appears after repeated slot movement and equip cycles.

## Stage 4B-2 - LIVE PENDING

- [ ] **LIVE PENDING (Stage 4B-2):** Bow +10 deals vanilla projectile damage +0.80.
- [ ] **LIVE PENDING (Stage 4B-2):** Crossbow +10 deals vanilla projectile damage +0.80.
- [ ] **LIVE PENDING (Stage 4B-2):** Thrown Trident +10 deals vanilla thrown damage +0.90.
- [ ] **LIVE PENDING (Stage 4B-2):** Bow +0 remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-2):** Crossbow +0 remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-2):** Trident +0 remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-2):** Firing a projectile and then switching held items does not change its enhancement bonus.
- [ ] **LIVE PENDING (Stage 4B-2):** Bow enhancement is applied exactly once.
- [ ] **LIVE PENDING (Stage 4B-2):** Crossbow enhancement is applied exactly once.
- [ ] **LIVE PENDING (Stage 4B-2):** Each Multishot projectile independently receives one enhancement bonus.
- [ ] **LIVE PENDING (Stage 4B-2):** Thrown Trident enhancement is applied exactly once.
- [ ] **LIVE PENDING (Stage 4B-2):** Trident melee behavior from Stage 4B-1 remains unchanged.
- [ ] **LIVE PENDING (Stage 4B-2):** Power, Piercing, Multishot, Loyalty, Channeling, and Impaling interactions remain vanilla-compatible.
- [ ] **LIVE PENDING (Stage 4B-2):** Fired projectiles do not acquire enhancement state from a weapon held after firing.
- [ ] **LIVE PENDING (Stage 4B-2):** A returned or picked-up Trident retains its `enhancement_level`.
- [ ] **LIVE PENDING (Stage 4B-2):** Save/reload or chunk unload does not duplicate or erase the projectile enhancement contribution where applicable.
- [ ] **LIVE PENDING (Stage 4B-2):** Crossbow-fired fireworks remain vanilla because their explosion damage does not use the ordinary arrow base-damage path.

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

## Stage 4B-3 - LIVE PENDING

- [ ] **LIVE PENDING (Stage 4B-3):** One +10 armor piece reduces incoming pre-armor damage by 1%.
- [ ] **LIVE PENDING (Stage 4B-3):** One +30 armor piece reduces incoming pre-armor damage by 3%.
- [ ] **LIVE PENDING (Stage 4B-3):** Four +30 armor pieces produce 12% enhancement reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Enhancement reduction is applied before vanilla armor/toughness.
- [ ] **LIVE PENDING (Stage 4B-3):** Vanilla Protection and Resistance still stack through their native paths.
- [ ] **LIVE PENDING (Stage 4B-3):** Unenhanced armor remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-3):** Unsupported equipment remains vanilla-equivalent.
- [ ] **LIVE PENDING (Stage 4B-3):** Enhanced Elytra produces zero damage reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Elytra retains its enhancement level and tooltip normally.
- [ ] **LIVE PENDING (Stage 4B-3):** Holding enhanced armor in the main hand does not grant reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Holding enhanced armor in the off hand does not grant reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Armor-bypassing damage bypasses enhancement armor reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Removing or equipping armor immediately updates reduction.
- [ ] **LIVE PENDING (Stage 4B-3):** Inventory movement, reconnect, and restart preserve enhancement state.
- [ ] **LIVE PENDING (Stage 4B-3):** Existing Stage 4B-1 melee/tool behavior is unchanged.
- [ ] **LIVE PENDING (Stage 4B-3):** Existing Stage 4B-2 projectile behavior is unchanged.

## Stage 4C - LIVE PENDING

- [ ] **LIVE PENDING (Stage 4C):** `foundation_probe` activates successfully when ready.
- [ ] **LIVE PENDING (Stage 4C):** Successful activation starts its cooldown.
- [ ] **LIVE PENDING (Stage 4C):** Immediate repeated use is blocked.
- [ ] **LIVE PENDING (Stage 4C):** Swapping between duplicate probes does not bypass the player-owned cooldown.
- [ ] **LIVE PENDING (Stage 4C):** Cooldown expires at the expected server game tick.
- [ ] **LIVE PENDING (Stage 4C):** Separate ability IDs do not share cooldowns.
- [ ] **LIVE PENDING (Stage 4C):** Logout and reconnect preserve an active cooldown.
- [ ] **LIVE PENDING (Stage 4C):** Server restart preserves an active cooldown.
- [ ] **LIVE PENDING (Stage 4C):** Inventory movement does not alter cooldown state.
- [ ] **LIVE PENDING (Stage 4C):** Dropping and reacquiring the probe does not reset cooldown state.
- [ ] **LIVE PENDING (Stage 4C):** Another player has an independent cooldown.
- [ ] **LIVE PENDING (Stage 4C):** No custom gameplay packet is required.
- [ ] **LIVE PENDING (Stage 4C):** Activation remains server-authoritative.
- [ ] **LIVE PENDING (Stage 4C):** Stage 4A enhancement behavior is unchanged.
- [ ] **LIVE PENDING (Stage 4C):** Stage 4B-1 melee/tool behavior is unchanged.
- [ ] **LIVE PENDING (Stage 4C):** Stage 4B-2 projectile behavior is unchanged.
- [ ] **LIVE PENDING (Stage 4C):** Stage 4B-3 armor behavior is unchanged.

## Stage 5A - Flame Sword - LIVE PENDING

### Flame Sword
- [ ] **LIVE PENDING (Stage 5A):** `flame_sword` exists and can be obtained.
- [ ] **LIVE PENDING (Stage 5A):** A normal melee attack works.
- [ ] **LIVE PENDING (Stage 5A):** Expected attack damage and attack speed attributes apply.

### Passive splash
- [ ] **LIVE PENDING (Stage 5A):** A melee hit triggers damage on nearby valid entities.
- [ ] **LIVE PENDING (Stage 5A):** The primary target is not double-hit by splash.
- [ ] **LIVE PENDING (Stage 5A):** The attacker is excluded.
- [ ] **LIVE PENDING (Stage 5A):** Nearby valid entities are damaged exactly once.
- [ ] **LIVE PENDING (Stage 5A):** Entities outside the 2.5-block radius are unaffected.
- [ ] **LIVE PENDING (Stage 5A):** Splash cannot recursively trigger itself.
- [ ] **LIVE PENDING (Stage 5A):** Flame particles appear around affected entities.

### Flame Cleave
- [ ] **LIVE PENDING (Stage 5A):** Right-click activates Flame Cleave.
- [ ] **LIVE PENDING (Stage 5A):** Frontal targets are hit.
- [ ] **LIVE PENDING (Stage 5A):** Targets behind the player are not hit.
- [ ] **LIVE PENDING (Stage 5A):** Targets outside the 90-degree cone are not hit.
- [ ] **LIVE PENDING (Stage 5A):** Targets outside the 4-block range are not hit.
- [ ] **LIVE PENDING (Stage 5A):** Each entity is hit at most once per activation.
- [ ] **LIVE PENDING (Stage 5A):** Affected entities ignite for approximately three seconds.
- [ ] **LIVE PENDING (Stage 5A):** Terrain remains unchanged.
- [ ] **LIVE PENDING (Stage 5A):** Flame particles and vanilla fire sound appear.
- [ ] **LIVE PENDING (Stage 5A):** An empty cast still starts cooldown.

### Cooldown
- [ ] **LIVE PENDING (Stage 5A):** Flame Cleave cooldown lasts 100 ticks.
- [ ] **LIVE PENDING (Stage 5A):** Immediate reuse is blocked.
- [ ] **LIVE PENDING (Stage 5A):** Reuse after expiry works.
- [ ] **LIVE PENDING (Stage 5A):** Swapping duplicate Flame Swords cannot bypass cooldown.
- [ ] **LIVE PENDING (Stage 5A):** Logout and reconnect preserve cooldown.
- [ ] **LIVE PENDING (Stage 5A):** Server restart preserves cooldown.
- [ ] **LIVE PENDING (Stage 5A):** Separate players have independent cooldowns.

### Regression
- [ ] **LIVE PENDING (Stage 5A):** `foundation_probe` still works.
- [ ] **LIVE PENDING (Stage 5A):** Stage 4 enhancement behavior still works.
- [ ] **LIVE PENDING (Stage 5A):** Vanilla swords remain unaffected.
- [ ] **LIVE PENDING (Stage 5A):** Unrelated items retain their vanilla right-click behavior.

## Stage 5B - Special Equipment System - LIVE PENDING

### Input
- [ ] **LIVE PENDING (Stage 5B):** F is detected in the client and sends one PRIMARY request per press.
- [ ] **LIVE PENDING (Stage 5B):** Shift+F is detected distinctly and sends one SECONDARY request per press.
- [ ] **LIVE PENDING (Stage 5B):** Unrelated keys are unaffected.
- [ ] **LIVE PENDING (Stage 5B):** Holding an unsupported vanilla item safely does nothing.
- [ ] **LIVE PENDING (Stage 5B):** Dedicated-server startup remains client-class safe.

### Server authority
- [ ] **LIVE PENDING (Stage 5B):** The server resolves the actual main-hand item.
- [ ] **LIVE PENDING (Stage 5B):** Invalid and unbound activation requests are ignored safely.
- [ ] **LIVE PENDING (Stage 5B):** A client cannot select an arbitrary ability, item, target, damage, cooldown, or movement vector.
- [ ] **LIVE PENDING (Stage 5B):** One key press does not cause duplicate activation.

### Flame Sword
- [ ] **LIVE PENDING (Stage 5B):** Normal melee and the Stage 5A splash passive still work.
- [ ] **LIVE PENDING (Stage 5B):** F activates Flame Cleave; right-click no longer does.
- [ ] **LIVE PENDING (Stage 5B):** Shift+F does nothing with Flame Sword.
- [ ] **LIVE PENDING (Stage 5B):** Flame Cleave remains a 100-tick cooldown and an empty cast consumes it.
- [ ] **LIVE PENDING (Stage 5B):** Swapping duplicate Flame Swords cannot bypass cooldown.

### Wind Sword
- [ ] **LIVE PENDING (Stage 5B):** `wind_sword` is obtainable, performs normal melee, and has expected native attributes.

### Wind Blade
- [ ] **LIVE PENDING (Stage 5B):** F launches the visible forward attack and plays its sound.
- [ ] **LIVE PENDING (Stage 5B):** A forward target is hit; off-path and beyond-range targets are unaffected.
- [ ] **LIVE PENDING (Stage 5B):** The caster is unaffected and one cast cannot repeatedly damage the same target.
- [ ] **LIVE PENDING (Stage 5B):** Terrain is unaffected and solid terrain stops the attack path.
- [ ] **LIVE PENDING (Stage 5B):** Wind Blade has a 60-tick cooldown; duplicate sword swapping cannot bypass it.

### Wind Step
- [ ] **LIVE PENDING (Stage 5B):** Shift+F starts a short forward movement burst with visible particles and sound.
- [ ] **LIVE PENDING (Stage 5B):** A solid wall is not bypassed, collision remains sane, and vertical velocity behaves sanely.
- [ ] **LIVE PENDING (Stage 5B):** A rejected Wind Step does not consume cooldown.
- [ ] **LIVE PENDING (Stage 5B):** Wind Step has an 80-tick cooldown; duplicate sword swapping cannot bypass it.

### Persistence
- [ ] **LIVE PENDING (Stage 5B):** Cooldowns survive inventory movement and dropping/reacquiring equipment.
- [ ] **LIVE PENDING (Stage 5B):** Cooldowns survive logout/reconnect and server restart.
- [ ] **LIVE PENDING (Stage 5B):** Separate players retain independent cooldown state.

### Regression
- [ ] **LIVE PENDING (Stage 5B):** `foundation_probe` remains valid.
- [ ] **LIVE PENDING (Stage 5B):** Stage 4 enhancement behavior remains valid.
- [ ] **LIVE PENDING (Stage 5B):** Vanilla equipment and vanilla right-click behavior remain unaffected.
- [ ] **LIVE PENDING (Stage 5B):** Flame Sword Stage 5A passive behavior remains unaffected.
- [ ] **LIVE PENDING (Stage 5B):** Dedicated server and client startup complete cleanly.

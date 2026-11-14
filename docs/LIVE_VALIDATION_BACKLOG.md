# Cumulative live validation backlog

> Historical status: the Stage 5 Flame prototype, F/Shift+F routing, 100-tick Flame cooldown, and Wind Step entries below are stale and superseded. Current behavior is documented in `STAGE_2_GAMEPLAY_RECONCILIATION.md`; retain the old entries only as historical test intent. Unrelated live checks remain pending until observed.

Automated build and smoke coverage does not replace the following human gameplay checks. Entries
remain **LIVE PENDING** until a human records an observed result.

## Oct 7 scoped follow-up — LIVE PENDING

- [ ] **LIVE PENDING (T2 cores):** Generate new Bastion treasure and Ancient City chest loot; verify one-time contents persist after closing, chunk unload, reconnect, and world re-entry.
- [ ] **LIVE PENDING (Trial cores):** (Sky Tower vaults, rooftop vault and Star Covenant roll are checked separately in S-01…S-10 at the end of this file.) Open normal and ominous Trial Vaults and verify the Eye of the Typhoon / Heart of Thunder source mapping, chance behavior, per-player key consumption, and no repeat reward from the same vault/player.
- [ ] **LIVE PENDING (Ocean Monument):** Defeat all three Elder Guardians across rooms/chunk boundaries; verify exactly one Heart of Currents drops, pickup works in multiplayer, and reconnect/restart does not grant a second core.
- [ ] **LIVE PENDING (Multi Catch):** Verify two independently generated fish with potentially different species, grade, length, and weight after success; verify no fish on minigame failure or missing Minnow permission.
- [ ] **LIVE PENDING (Multi Catch durability):** Verify one Reel charge per valid attempt and one Minnow/Crankbait charge per permitted fish, including a module that becomes broken after the first result.
- [ ] **LIVE PENDING (Fishing cancellation):** Verify logout, death, hook removal, rod removal, dimension travel, and timeout cancel both rewards while retaining the already-spent Reel charge.
- [ ] **LIVE PENDING (Fishing regression):** Verify vanilla rod casting/retrieval outside eligible custom water and the eligible custom fishing loop/HUD remain functional.
- [ ] **LIVE PENDING (Fisherman sale):** Shift-right-click sells exactly one data-bearing fish at the component-derived value, handles a full inventory, supports repeat sales, and leaves ordinary vanilla trades/restock unchanged.
- [ ] **LIVE PENDING (Fisherman multiplayer):** Two players selling to the same Fisherman each receive only their own server-calculated payment; inventory movement and reconnect preserve unsold fish data.
- [ ] **LIVE PENDING (Activity rewards):** Verify the five advancement milestones appear once per player, toast correctly in Korean/English, award exactly 60 XP total, and remain complete after reconnect/restart.
- [ ] **LIVE PENDING (PR #69 mobs):** Confirm Peaceful difficulty removes/blocks Feral Wolf and Cave Bat while their Creature/Ambient category caps and ordinary difficulty behaviors remain correct.
- [ ] **LIVE PENDING (PR #65 equipment):** Click Armor Stands with the Greatsword, move/remove a Mining Drill during its menu, test all portable-furnace fuel families, and visually inspect Injector/Drill/furnace slot placement and filters.
- [ ] **LIVE PENDING (resources):** Review all 40 custom mob textures after production art is connected; inspect vanilla-model silhouettes, equipment, scale, held items, first/third person and GUI. The five core icons currently use vanilla model references and are not final artwork.

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

### Flame Sword (historical F-key checks; current active uses right-click)
- [ ] **LIVE PENDING (Stage 5A):** `flame_sword` exists and can be obtained.
- [ ] **LIVE PENDING (Stage 5A):** A normal melee attack works.
- [ ] **LIVE PENDING (Stage 5A):** Expected attack damage and attack speed attributes apply.

### Passive splash (superseded Stage 5 prototype; stale)
- [ ] **LIVE PENDING (Stage 5A):** A melee hit triggers damage on nearby valid entities.
- [ ] **LIVE PENDING (Stage 5A):** The primary target is not double-hit by splash.
- [ ] **LIVE PENDING (Stage 5A):** The attacker is excluded.
- [ ] **LIVE PENDING (Stage 5A):** Nearby valid entities are damaged exactly once.
- [ ] **LIVE PENDING (Stage 5A):** Entities outside the 2.5-block radius are unaffected.
- [SUPERSEDED / STALE] Stage 5A recursive splash assertion; current passive is every tenth successful hit.
- [SUPERSEDED / STALE] Stage 5A splash VFX assertion.

### Flame Cleave (historical Stage 5 checks; right-click and 600-tick cooldown are current)
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
- [SUPERSEDED / STALE] Stage 5A recorded a 100-tick Flame Cleave cooldown; current cooldown is 600 ticks.
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

### Input (superseded F / Shift+F route; T2 actives use item right-click)
- [SUPERSEDED / STALE] Stage 5B F-key PRIMARY route; T2 uses item right-click.
- [SUPERSEDED / STALE] Stage 5B Shift+F SECONDARY route and Wind Step input.
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
- [SUPERSEDED / STALE] Stage 5B F-key Flame Cleave route; current active uses right-click.
- [SUPERSEDED / STALE] Stage 5B Shift+F Flame Sword assertion.
- [SUPERSEDED / STALE] F-key-era 100-tick Flame cooldown and empty-cast assertion.
- [ ] **LIVE PENDING (Stage 5B):** Swapping duplicate Flame Swords cannot bypass cooldown.

### Wind Sword
- [ ] **LIVE PENDING (Stage 5B):** `wind_sword` is obtainable, performs normal melee, and has expected native attributes.

### Wind Blade (historical F-key route; current active uses item right-click)
- [SUPERSEDED / STALE] Stage 5B F-key Wind Blade route; current active uses right-click.
- [ ] **LIVE PENDING (Stage 5B):** A forward target is hit; off-path and beyond-range targets are unaffected.
- [ ] **LIVE PENDING (Stage 5B):** The caster is unaffected and one cast cannot repeatedly damage the same target.
- [ ] **LIVE PENDING (Stage 5B):** Terrain is unaffected and solid terrain stops the attack path.
- [ ] **LIVE PENDING (Stage 5B):** Wind Blade has a 60-tick cooldown; duplicate sword swapping cannot bypass it.

### Wind Step (removed from current design; stale historical checklist)
- [ ] **LIVE PENDING (Stage 5B):** Shift+F starts a short forward movement burst with visible particles and sound.
- [SUPERSEDED / STALE] Wind Step collision assertion.
- [SUPERSEDED / STALE] Wind Step rejection assertion.
- [SUPERSEDED / STALE] Wind Step cooldown assertion.

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

## Stage 6 - Materials, Crafting, and Progression - LIVE PENDING

### Registration
- [ ] **LIVE PENDING (Stage 6):** All five ore/debris blocks, materials, and storage blocks exist.
- [ ] **LIVE PENDING (Stage 6):** No models or textures are missing.
- [ ] **LIVE PENDING (Stage 6):** Materials and blocks are discoverable in the intended creative tabs.

### Approved material texture integration

The final 23 human-approved 32x32 PNG files from the task attachment are copied byte-for-byte into the native Minecraft resource namespace. See [APPROVED_MATERIAL_TEXTURES.md](APPROVED_MATERIAL_TEXTURES.md) for source checksums, destination paths, registration audit, and unused deepslate variants.

- IMPLEMENTED (file-level): Titanium, Tungsten, Mythril, Etherium, Adamantium, and Enhancement Stone approved textures.
- IMPLEMENTED (file-level): Existing registered blocks, block item definitions, and flat item models resolve through the `moreminecrafts` namespace.
- PRESERVED: Adamantium's existing registered item ID remains `adamantium_ingot`; its item model references the approved `adamantium.png` texture.
- INCLUDED, CURRENTLY UNUSED: Five deepslate/debris-deepslate textures because no matching deepslate blocks are registered.
- LIVE PENDING: Inspect all material block faces and item icons in a running Minecraft client; confirm no missing texture or UV issues.

### World generation
- [ ] **LIVE PENDING (Stage 6):** Titanium generates in the Overworld from Y -16 through 80 near its intended distribution.
- [ ] **LIVE PENDING (Stage 6):** Tungsten generates in the Overworld from Y -48 through 32 near its intended distribution.
- [ ] **LIVE PENDING (Stage 6):** Mythril generates in the Overworld from Y -64 through 0 and remains rarer than Diamond.
- [ ] **LIVE PENDING (Stage 6):** Etherium generates in the Overworld from Y -32 through 64 and remains rarer than Diamond.
- [ ] **LIVE PENDING (Stage 6):** Adamantium generates only in Nether biomes with debris-like rarity and exposure.
- [ ] **LIVE PENDING (Stage 6):** No new mineral has obvious generation spam or surface spam.
- [ ] **LIVE PENDING (Stage 6):** Vanilla ore generation remains unaffected.

### Mining
- [ ] **LIVE PENDING (Stage 6):** Every mineral requires a pickaxe and its documented mining tier.
- [ ] **LIVE PENDING (Stage 6):** Wrong tools and insufficient-tier pickaxes do not produce drops.
- [ ] **LIVE PENDING (Stage 6):** Silk Touch returns each of the five ore/debris blocks.
- [ ] **LIVE PENDING (Stage 6):** Fortune multiplies Titanium, Tungsten, Mythril, and Etherium drops.
- [ ] **LIVE PENDING (Stage 6):** Fortune does not multiply Adamantium Debris.

### Drops and XP
- [ ] **LIVE PENDING (Stage 6):** Titanium drops Raw Titanium and Tungsten drops Raw Tungsten.
- [ ] **LIVE PENDING (Stage 6):** Mythril and Etherium drop their direct materials.
- [ ] **LIVE PENDING (Stage 6):** Adamantium Debris drops itself.
- [ ] **LIVE PENDING (Stage 6):** Mythril and Etherium award diamond-like XP without Silk Touch.
- [ ] **LIVE PENDING (Stage 6):** None of the five blocks produces duplicate drops.

### Processing
- [ ] **LIVE PENDING (Stage 6):** Raw Titanium works in both furnace and blast furnace.
- [ ] **LIVE PENDING (Stage 6):** Raw Tungsten works in both furnace and blast furnace.
- [ ] **LIVE PENDING (Stage 6):** Adamantium Debris works in both furnace and blast furnace.
- [ ] **LIVE PENDING (Stage 6):** Mythril and Etherium cannot be incorrectly smelted.

### Crafting
- [ ] **LIVE PENDING (Stage 6):** All five 9-material storage recipes work and consume the correct count.
- [ ] **LIVE PENDING (Stage 6):** All five storage blocks unpack to exactly nine materials.

### Regression
- [ ] **LIVE PENDING (Stage 6):** Enhancement Stone and Stage 4 enhancement still work.
- [ ] **LIVE PENDING (Stage 6):** Flame Sword and Wind Sword skills still work.
- [ ] **LIVE PENDING (Stage 6):** Dedicated server and client startup complete cleanly.
- [ ] **LIVE PENDING (Stage 6):** Unrelated vanilla mining and world generation remain unaffected.

- [ ] **LIVE PENDING (Stage 6):** Sample generated chunks and measure actual Y distributions, especially Tungsten near -16 and Mythril near -48; confirm the corrected core peaks and sparse upper tails in newly generated chunks.
- [ ] **LIVE PENDING (Stage 6):** Verify stone and deepslate replacement in each Overworld ore's full allowed Y range after adding both replacement targets.

## Stage 7 — Fishing Life Content

All checks below are **LIVE PENDING** until exercised in a Minecraft 26.3 client or dedicated server.

### Vanilla flow
- **LIVE PENDING** — Vanilla Fishing Rod casts normally.
- **LIVE PENDING** — Vanilla FishingHook movement and water behavior work.
- **LIVE PENDING** — Vanilla waiting and bite behavior work.
- **LIVE PENDING** — Lure still affects wait time.
- **LIVE PENDING** — reel/catch use consumes normal rod durability.

### Equipment UI and persistence
- **LIVE PENDING** — Shift-right-click opens the Fishing Equipment UI; normal right-click still casts.
- **LIVE PENDING** — Reel, Crankbait, and Minnow slots are visible and each accepts/removes only its correct family.
- **LIVE PENDING** — Invalid items and wrong-family equipment are rejected.
- **LIVE PENDING** — closing normally or unexpectedly, shift-clicking, hotbar swap, and attempted movement of the source rod neither duplicate nor lose equipment.
- **LIVE PENDING** — equipped rods retain loadouts through inventory/chest movement, drop/reacquire, transfer, reconnect, and server restart.
- **LIVE PENDING** — different Fishing Rod ItemStacks retain independent loadouts.

### Reel, Minnow, and Crankbait roles
- **LIVE PENDING** — shallow water works and progressively deeper water/candidates require stronger Reels.
- **LIVE PENDING** — Reel unlocks depth candidates without directly increasing rarity.
- **LIVE PENDING** — insufficient Minnow fully excludes required species; sufficient Minnow unlocks them.
- **LIVE PENDING** — higher Minnow does not act as a generic rarity multiplier.
- **LIVE PENDING** — repeated catches demonstrate Crankbait's upper-grade probability shift.
- **LIVE PENDING** — Crankbait neither guarantees rare results nor unlocks depth/Minnow-inaccessible species.

### Timing minigame
- **LIVE PENDING** — overlay starts at a valid vanilla bite/reel and marker moves automatically at constant speed with no steering.
- **LIVE PENDING** — one center click succeeds; an outside click and timeout fail; the HUD remains aligned to server timing under latency.
- **LIVE PENDING** — success, failure, timeout, logout, death, hook removal, dimension transition, and reconnect clear the UI/state without leaving it stuck.

### Fish items
- **LIVE PENDING** — catches have the selected species, Grade I–V, bounded centimeter length, deterministic quality, and correct tooltip.
- **LIVE PENDING** — grade/length persist through inventory movement, drop/reacquire, logout/reconnect, and restart.
- **LIVE PENDING** — heterogeneous fish do not merge incorrectly.

### Crafting and regression
- **LIVE PENDING** — all 15 equipment recipes load in a normal crafting table and consume their specified vanilla/Stage 6 materials.
- **LIVE PENDING** — enhancement system, Flame Sword, Wind Sword, and Stage 6 ore/worldgen behavior remain intact.
- **LIVE PENDING** — dedicated server starts and a client starts/connects without mixin, packet, component, screen, model, or translation errors.

## Stage 8A — Zombie variants

Variant identity is persisted in each Zombie's save data. Warning, cooldown, leap,
fall-immunity, recovery, and pending explosion countdown state is intentionally
runtime-only and resets across a server restart.

### Bomb Zombie

- **LIVE PENDING:** Naturally appears at the expected approximate frequency.
- **LIVE PENDING:** TNT helmet is visible and does not drop.
- **LIVE PENDING:** Normal Zombie navigation and combat remain intact.
- **LIVE PENDING:** Death warning sequence occurs and explosion follows at approximately 30 ticks.
- **LIVE PENDING:** Player damage has distance falloff and applies knockback.
- **LIVE PENDING:** Ordinary mobs are not damaged.
- **LIVE PENDING:** Explosion destroys no blocks and starts no fire.
- **LIVE PENDING:** Save/reload and chunk unload/reload preserve the Bomb variant.

### Leap Zombie

- **LIVE PENDING:** Naturally appears and ordinary Zombie navigation works between leaps.
- **LIVE PENDING:** A player target at the configured range triggers a readable warning.
- **LIVE PENDING:** Physical leap works and retains normal world collision.
- **LIVE PENDING:** A leap interrupted by water, lava, a passenger state, or a blocked launch recovers without becoming stuck; a long airborne leap times out.
- **LIVE PENDING:** The Leap Zombie takes no self fall damage from its leap.
- **LIVE PENDING:** Landing AoE damages and knocks back players but not ordinary mobs.
- **LIVE PENDING:** Cooldown prevents repeated leaps for 120 ticks.
- **LIVE PENDING:** Save/reload and chunk unload/reload preserve the Leap variant.

### Regression

- **LIVE PENDING:** Ordinary Zombies remain ordinary and vanilla Zombie spawning remains functional.
- **LIVE PENDING:** Unrelated mobs are unaffected.
- **LIVE PENDING:** Dedicated server starts.
- **LIVE PENDING:** Client starts.
- **LIVE PENDING:** Stage 1–7 functionality still initializes.

## Stage 8B — Mining Giant

The Mining Giant is a persistent Zombie identity; its Tunnel Breaker warning,
locked direction, and cooldown are intentionally transient and reset when the entity reloads.

### Identity and survival spawning

- **LIVE PENDING:** Rare survival-native Zombie spawning at Y 32 or below produces Mining Giants without dominating underground play.
- **LIVE PENDING:** The approximately 2.0-scale Zombie appearance is correct and remains readable in caves.
- **LIVE PENDING:** The Diamond Pickaxe appears in the main hand and never drops.
- **LIVE PENDING:** 140 health, 8 armor, and 12-damage melee behavior work as configured.
- **LIVE PENDING:** 0.35 knockback resistance is noticeable without making displacement impossible.
- **LIVE PENDING:** Normal Zombie navigation and pathfinding work at the larger scale.
- **LIVE PENDING:** Normal hostile player targeting works.
- **LIVE PENDING:** Save/reload preserves Mining Giant identity, equipment, attributes, and current health.
- **LIVE PENDING:** Chunk unload/reload preserves Mining Giant identity.

### Tunnel Breaker special ability

- **LIVE PENDING:** A living player target between 2.5 and 7 blocks triggers Tunnel Breaker only while the giant is grounded and ready.
- **LIVE PENDING:** The 24-tick planted stance, pickaxe swings, restrained smoke, and low anvil sound form a clearly readable telegraph.
- **LIVE PENDING:** The locked attack direction gives players meaningful sideways or backward counterplay.
- **LIVE PENDING:** The seven-block ground corridor damages and strongly knocks back players inside it, while sparing players outside it.
- **LIVE PENDING:** The 200-tick cooldown prevents repeated Tunnel Breakers.
- **LIVE PENDING:** Impact particles and sounds are satisfying but not excessive in enclosed combat.
- **LIVE PENDING:** Completion and invalid/dead/removed interruption leave no stale warning, target, or direction state.
- **LIVE PENDING:** Changing targets, losing ground, entering fluid, or becoming a passenger during the warning cancels the cleave.
- **LIVE PENDING:** Death or removal during the telegraph cleans up correctly.
- **LIVE PENDING:** Tunnel Breaker causes no permanent or temporary terrain damage.
- **LIVE PENDING:** Multiplayer targeting and corridor hits behave sensibly with multiple nearby players.

### Regression

- **LIVE PENDING:** Bomb Zombie still spawns, persists, warns, and explodes correctly.
- **LIVE PENDING:** Leap Zombie still spawns, persists, leaps, lands, and recovers correctly.
- **LIVE PENDING:** Ordinary Zombies remain unaffected.
- **LIVE PENDING:** Unrelated mobs remain unaffected.
- **LIVE PENDING:** Dedicated server starts.
- **LIVE PENDING:** Client starts.
- **LIVE PENDING:** Previous Stage functionality still initializes.

## Stage 9A — Skeleton King

The operator-only `/summonskeletonking` command creates this intentionally concrete encounter.
Persistent entity save data records the King and guard-owner identities; ability telegraphs,
cooldowns, projectile references, boss-bar viewers, and the active schedule are transient.

### Boss

- **LIVE PENDING:** The test summon works and creates exactly one Skeleton King.
- **LIVE PENDING:** The approximately 1.25 scale, gold helmet, name, and vanilla Skeleton silhouette are readable.
- **LIVE PENDING:** Maximum health is 200 and current health begins full.
- **LIVE PENDING:** Armor is 8 and movement speed is approximately 0.22.
- **LIVE PENDING:** Normal Skeleton navigation remains intact.
- **LIVE PENDING:** Normal Skeleton targeting and combat remain intact between telegraphs.
- **LIVE PENDING:** The native Skeleton King boss bar appears for appropriate nearby players.
- **LIVE PENDING:** Boss-bar health stays synchronized with actual server-side health.
- **LIVE PENDING:** Boss-bar viewers are added in range and removed on leaving range or disconnecting.
- **LIVE PENDING:** Save/reload preserves the Skeleton King identity, attributes, equipment, name, current health (including partially depleted health), and boss bar restoration.
- **LIVE PENDING:** Chunk unload/reload preserves the Skeleton King and restores its encounter UI safely.

### Bone Barrage

- **LIVE PENDING:** The short crit-particle and Skeleton-sound telegraph is readable.
- **LIVE PENDING:** Exactly eight real vanilla arrows appear from a sensible eye-height origin.
- **LIVE PENDING:** The approximately 30-degree total spread feels correct.
- **LIVE PENDING:** Vanilla block and entity collision works.
- **LIVE PENDING:** The 120-tick cooldown works.
- **LIVE PENDING:** Death, removal, unload, or target loss interrupts cleanly and tracked arrows do not leak.

### Summon Guards

- **LIVE PENDING:** Three Skeleton Guards spawn per successful cast.
- **LIVE PENDING:** Spawn positions approximately four blocks around the King are sensible in normal terrain.
- **LIVE PENDING:** Guards acquire and participate against the King's player target.
- **LIVE PENDING:** Repeated casts remain bounded at six living owned guards.
- **LIVE PENDING:** Guards stay ordinary Skeleton-derived minions and never become bosses or gain boss bars.
- **LIVE PENDING:** Guards in a neighboring loaded chunk survive a temporary King chunk unload/reload.
- **LIVE PENDING:** Owned guards clean up when the encounter ends, including guards unloaded when their owner is removed; a reloaded orphan guard gets at most the 200-tick owner-missing grace before self-removal.
- **LIVE PENDING:** If unloaded guards return while the King is active, the loaded owned-guard population is reconciled back to the six-guard cap.

### Weakness Curse

- **LIVE PENDING:** Witch particles and spell audio make the telegraph readable.
- **LIVE PENDING:** Nearby valid participating players receive Weakness.
- **LIVE PENDING:** Weakness lasts 100 ticks with amplifier 0.
- **LIVE PENDING:** Unrelated entities are unaffected.
- **LIVE PENDING:** The 160-tick cooldown works.

### Lifecycle

- **LIVE PENDING:** Boss death removes guards, tracked barrage arrows, pending telegraph, and boss-bar viewers.
- **LIVE PENDING:** Explicit boss removal performs the same cleanup.
- **LIVE PENDING:** Player disconnect during participation or a telegraph leaves no stale viewer or target.
- **LIVE PENDING:** A player leaving range loses the bar and invalidates an active targeted telegraph safely.
- **LIVE PENDING:** Changing the King's target during a telegraph cancels that ability.
- **LIVE PENDING:** Server shutdown/restart preserves identity while restarting transient cooldown timing safely.
- **LIVE PENDING:** No stale boss bars remain after death, removal, unload, disconnect, or restart.
- **LIVE PENDING:** No stale transient ability state remains after interruption or restart.

### Regression

- **LIVE PENDING:** Stage 8 Bomb Zombies, Leap Zombies, and Mining Giants still work.
- **LIVE PENDING:** Ordinary Skeletons remain unaffected.
- **LIVE PENDING:** Ordinary Zombies remain unaffected.
- **LIVE PENDING:** Dedicated server starts.
- **LIVE PENDING:** Client starts and connects.
- **LIVE PENDING:** Previous Stage systems still initialize.

---

# Grand Unified 2026-11-12 — in-game checklist for ACT-03 / ACT-04 / ACT-05 — LIVE VALIDATION PENDING

Scope: registry rows ACT-03 (ST-01 royal chest runtime), ACT-04 (accessory acquisition, slots, T1 effects, cooldowns) and ACT-05 (Sky Tower vaults, trial key, Star Covenant) in
[`integration/FEATURE_STATUS_REGISTRY_20261112.md`](integration/FEATURE_STATUS_REGISTRY_20261112.md), base `work/grand-unified-20261112` @ `98748003efd494f73e41e035650493c272cc9123`.
The owner plays this through manually. Every check below is **LIVE VALIDATION PENDING** until a human fills in its Result line.

| Layer | ACT-03 / ACT-04 / ACT-05 status on this base |
|---|---|
| Implementation | Present in the baseline tree (registry: ACTIVE). Gaps found while writing this list are flagged **[GAP]** in the checks. |
| Automated validation | Registry cites CI on tree-identical `f5777244`; not re-run for this documentation-only change. |
| Live validation | **LIVE VALIDATION PENDING** — nothing below has been observed by a human. |

How to read a check: **Setup** (commands) → **Steps** → **Expected** (authority cited as `file Lnn` / decision ID) → **Result** (fill in: tick PASS or FAIL, add notes).
A check whose expected value has no approved authority says **RECORD ONLY** — write down what you see; do not mark FAIL against it. Probabilistic outcomes (a roll that may legitimately miss) are marked **STAT** — record counts, do not fail a single miss.

Authority short names used below:

| Short | File |
|---|---|
| I06 | `docs/planning/D13_03_I06_ROYAL_CHEST_REWARDS_AUTHORITY_2026-11-07.md` |
| D13Q | `docs/planning/D13_03_DECISIONS_Q01-Q47.md` (decision IDs `D13-03-Qnn`) |
| UI | `docs/ACCESSORY_INVENTORY_UI_APPROVED_2026-10-23.md` |
| CD | `docs/ACCESSORY_COOLDOWN_DECISION_2026-10-25.md` |
| D01 | `docs/planning/T1_ACCESSORIES_D01_OWNER_DECISIONS_2026-10-30.md` (decision IDs `D01-Qnn` / `Qnn`) |
| P4 | `docs/implementation/sky_tower_p4_vault_rewards/OPEN_DESIGN_DECISIONS.md` (rows 1–9) |
| REW | `docs/implementation/sky_tower_p4_vault_rewards/AUTHORITY_MATRIX.md` / `REWARD_TABLES.md` |
| STQ | `docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` |

Command reference for the debug helpers used here (`/mmtomb`, `/mmskytower`, vanilla `/locate`, `/place`, `/loot`, `/damage`) is in [`LIVE_TEST_COMMANDS.md`](LIVE_TEST_COMMANDS.md) § "Grand Unified 2026-11-12 helpers".
Existing, related backlog items are **not** duplicated: the vanilla/T2-core Trial Vault item under "Oct 7 scoped follow-up" (Eye of the Typhoon / Heart of Thunder) stays as is; Sky Tower vault behaviour is checked here (S-xx).

## Common preflight (do once)

- Launch per [`MOD_DEVELOPMENT_RULES.md`](MOD_DEVELOPMENT_RULES.md) § C (`runClient` / `runServer`, JDK 25). Two-player checks need a dedicated or LAN server and a second client/account. Persistence checks need a real **server stop → start** (singleplayer "Save and Quit" → reopen counts as a restart).
- Use an operator account. `/gamerule` names below are the 26.x snake_case ids as understood by the writer; if tab-completion offers a different spelling, use that and note it in the Result.
- Handy rules: `/gamerule keep_inventory true|false`, `/gamerule natural_health_regeneration false` (stops regen from masking heal tests), `/difficulty normal`.
- Record the world seed, build/commit, and client/server used at the top of each run: seed `______` build `______` side `SP / dedicated` date `______`.

---

## ACT-03 — ST-01 pyramid royal chest runtime

Facts from the code that shape the checklist (verified in `src/main`):

- Structure id `moreminecrafts:st01_pyramid` (desert only, `random_spread` spacing 32 / separation 8, salt 91401). Natural generation is the only live path that registers the royal chest (`St01PyramidCompletion.tryComplete` → `St01RoyalChestPlacement.registerPlaced`).
- Only **PRIMARY** (a double chest) is registered. `registerPlaced` passes `Optional.empty()` for SECONDARY, matching the "absent SECONDARY" statement in the G0-R1 authority. **[GAP] SECONDARY cannot be live-tested on this base** (R-12).
- The royal hall sits ≈20 blocks below the pyramid foundation behind the closed secret complex; no unlock mechanism exists (`docs/design/st01-p8c2a/README.md`: "Open cover … No unlock mechanism exists"). Reach it by digging / creative flight, then use survival for the chest checks.
- Rewards are a pure function of (world seed, structure instance id, role), so they are fixed at the moment the instance is registered; a first-open cannot be re-rolled in the same world.

### Locating the PRIMARY chest

```
/locate structure moreminecrafts:st01_pyramid
/tp @s <X> 120 <Z>            # fly there; X/Z from /locate
/forceload add <X-48> <Z-48> <X+48> <Z+48>   # load the whole 81x81 footprint so completion/registration can run
```

Ground anchor `G` = centre of the start chunk: `Gx = floor(X/16)*16+8`, `Gz = floor(Z/16)*16+8`; `Gy` = Y of the pyramid foundation (read it from F3 at the pyramid base). The pyramid's orientation is not shown anywhere, so try the four candidates; the right one has a **double chest** in a large hall. Offsets are `world = G + R(authored)` (authored PRIMARY left (−21,−20,33), right (−20,−20,33), guardian emergence point (−21,−22,26)):

| Orientation | PRIMARY left = tomb anchor | PRIMARY right | Guardian emergence point |
|---|---|---|---|
| SOUTH (rotation none) | G + (−21, −20, +33) | G + (−20, −20, +33) | G + (−21, −22, +26) |
| WEST (clockwise_90) | G + (−33, −20, −21) | G + (−33, −20, −20) | G + (−26, −22, −21) |
| NORTH (180) | G + (+21, −20, −33) | G + (+20, −20, −33) | G + (+21, −22, −26) |
| EAST (counterclockwise_90) | G + (+33, −20, +21) | G + (+33, −20, +20) | G + (+26, −22, +21) |

Call the confirmed left-half position `<AX> <AY> <AZ>` (it is the encounter anchor used by `/mmtomb`).

#### R-01 — Pyramid exists and the PRIMARY royal chest is where the code puts it
- **Status:** LIVE VALIDATION PENDING
- **Setup:** commands above; creative, flying. Dig down at a candidate (Gx+dx, Gy−20, Gz+dz) or `/tp @s <candidate>` only after confirming air there (spectator mode can pass through blocks; switch back to survival/creative before interacting — spectators and fake players cannot open royal chests).
- **Steps:** find the hall; confirm exactly one double chest ("PRIMARY" pair) at the table position; note orientation, `<AX> <AY> <AZ>`.
- **Expected:** a complete pyramid (natural generation, Policy D) with a double chest at the tabled offset. Treasury and secret areas are separate. *(Registry ACT-01/02/03; I06 L7, L11)*
- **Result:** ☐ PASS ☐ FAIL — orientation `____` anchor `____` notes: ____________

#### R-02 — Sealed chest starts the Tomb Guardian encounter; no loot yet
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival, full armor/food, a sword; stand in the hall. `/mmtomb status <AX> <AY> <AZ>`.
- **Steps:** (1) status. (2) Right-click either half of the PRIMARY chest. (3) Right-click again while the guardian is alive. (4) `/mmtomb status <AX> <AY> <AZ>`.
- **Expected:** (1) `DORMANT sealed=true`. (2) **No chest GUI opens**; a Tomb Guardian (`moreminecrafts:tomb_guardian`) emerges at the emergence point. (3) Still no GUI and **no second guardian**. (4) `ACTIVE …`, `sealed=true`. If a normal empty double-chest GUI opens instead, the chest was **not registered** — FAIL and note (chunks not fully completed/loaded). *(I06 L14 "sealed until normal defeat … preserve PRIMARY interaction trigger"; `TombGuardianEncounters` StartResult STARTED / ALREADY_ACTIVE)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-03 — Interrupted fight is not victory (retry); no refarm
- **Status:** LIVE VALIDATION PENDING
- **Setup:** an ACTIVE encounter from R-02.
- **Steps:** (1) `/mmtomb end <AX> <AY> <AZ>` (debug stand-in for "guardian left without dying"). (2) `/mmtomb status …`. (3) Right-click the chest again. (4) Optional natural variant: log out mid-fight, rejoin; record whether the guardian is still there and whether the chest still seals.
- **Expected:** (1) `End encounter …: true`, guardian removed. (2) `DORMANT sealed=true`. (3) A fresh guardian spawns (RESTARTED); chest still gives no loot. (4) Never `DEFEATED` unless the guardian actually died. *(I06 L14 "Boss absence, unload, or interruption is not victory")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-04 — Sealed-state protections (before defeat)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival, `minecraft:diamond_pickaxe` (`/give @s minecraft:diamond_pickaxe`), chest still sealed (R-02/R-03 state, guardian absent or alive).
- **Steps:** (1) Try to mine a chest half. (2) `/summon minecraft:tnt <x> <y> <z> {fuse:40s}` next to the chest, stay clear. (3) Place a hopper beside the chest facing into it and one on top of it: `/setblock <hx> <hy> <hz> minecraft:hopper[facing=<dir toward chest>]` then `/item replace block <hx> <hy> <hz> container.0 with minecraft:dirt 5`; for the top hopper `/setblock <cx> <cy+1> <cz> minecraft:hopper[facing=down]`. Wait 10 s.
- **Expected:** chest cannot be mined; TNT does not destroy the chest (nearby hall blocks may); no item enters the chest from the side hopper and nothing leaves via the top hopper. *(I06 L17 "Block automated insertion and extraction … permanently, before and after defeat. Block player mining and explosion destruction permanently")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-05 — Normal defeat unlocks the chest (and only a defeat does)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival, good gear; encounter ACTIVE (right-click chest).
- **Steps:** defeat the Tomb Guardian by real combat. `/mmtomb status <AX> <AY> <AZ>`. Do **not** open the chest yet. (Shortcut for later repeat runs only: `/kill @e[type=moreminecrafts:tomb_guardian,limit=1,sort=nearest]` goes through the same death path but is not a normal-combat PASS.)
- **Expected:** `DEFEATED sealed=false`. Guardian drop is existing behaviour (Ancient Heart 50% via `moreminecrafts:entities/tomb_guardian`) — record only. *(I06 L14 "valid defeat is persisted"; `TombGuardianEncounters.guardianDied`)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-06 — First direct opening generates PRIMARY rewards (54 slots, withdraw-only UI)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** state from R-05; empty inventory space (≥ 12 free slots); `/gamerule keep_inventory true`.
- **Steps:** right-click the chest once. Count every item (sum across stacks). Write them in the table. Do not take anything yet.
- **Expected:** a 9×6 (54-slot) GUI titled **"Royal Chest (Primary)"**; no chat/title notification. Contents obey I06 L27–32, L36–45, L64, L66:

| Item (id) | Rule | Observed total |
|---|---|---|
| `minecraft:gold_ingot` | always present, 64–128 | ____ |
| `minecraft:diamond` | 85% — if present 10–18 | ____ |
| `minecraft:emerald` | 75% — if present 12–24 | ____ |
| `moreminecrafts:mythril` | 25% — if present 2–3 | ____ |
| rare pool (≤ ONE of): `moreminecrafts:adamantium_ingot` 1 / `minecraft:netherite_ingot` 1 / `minecraft:netherite_scrap` 1–2 / `minecraft:ancient_debris` 1–2 | 10% gate; never two rare types | ____ |
| T2 accessory: `moreminecrafts:seed_of_life` or `moreminecrafts:ring_of_time` ×1 | 20% **STAT** | ____ |
| `moreminecrafts:relic_catalyst` ×1 | 5% per instance, 50% of those to this chest **STAT** | ____ |

  Nothing else may appear (no titanium/tungsten/enhancement stone — those are SECONDARY; no raw ores; no other accessories). Quantities above 64 are split across stacks with nothing truncated (I06 L76). If an accessory appeared (first generation of this chest), expect a short **END_ROD particle burst + amethyst chime** at the chest, visible to players within 16 blocks, with **no chat or title** (D13-03 Q39–Q41; implemented in `RoyalChestRevealFx`). No repeat on reopen, item withdrawal, relog or restart.
- **Result:** ☐ PASS ☐ FAIL — notes/total slots used: ____________

#### R-07 — Withdraw-only: nothing can be inserted
- **Status:** LIVE VALIDATION PENDING
- **Setup:** chest GUI open (R-06). Put `/give @s minecraft:dirt 32` and `/give @s minecraft:diamond 4` in inventory/hotbar.
- **Steps:** try every insertion path: drag a stack onto an empty chest slot; click-drop onto a slot; shift-click from inventory; number-key hotbar swap with a chest slot; double-click gather; drag-split across several chest slots. Then withdraw normally: click-pick, shift-click out, drag-split out.
- **Expected:** no insertion succeeds by any path (items snap back / do nothing); withdrawing works with ordinary slot synchronisation. *(I06 L16 "Players may withdraw only … including shift-click, drag, hotbar swap, or other insertion paths")*
- **Result:** ☐ PASS ☐ FAIL — path that leaked (if any): ____________

#### R-08 — Remaining items persist; loot is permanently depleted
- **Status:** LIVE VALIDATION PENDING
- **Setup:** R-06 contents known.
- **Steps:** (1) Take ~half (note exactly what remains). Close. Reopen. (2) Log out/in; reopen. (3) `/tp` 500+ blocks away, wait 30 s, return; reopen. (4) Server restart; reopen. (5) Take everything; reopen (empty); `/time add 24000`; reopen; restart again; reopen.
- **Expected:** after (1)–(4) the remaining items are exactly what you left; **no regeneration, no reroll**. After (5) the chest stays empty forever — no restock/refarm. `/mmtomb status` still `DEFEATED`. *(I06 L15, L18, L19, L20)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-09 — Protections after defeat and after depletion
- **Status:** LIVE VALIDATION PENDING
- **Setup:** do part A while the chest still holds items (between R-06 and R-08 step 5); part B after depletion. Survival, diamond pickaxe, TNT/hopper commands from R-04.
- **Steps:** A: repeat R-04 steps 3 with items inside (top hopper must not pull; side hopper must not insert); also place a hopper minecart (`/give @s minecraft:hopper_minecart`) on a rail under/next to it. B: with the chest empty, repeat mining and TNT (R-04 steps 1–2).
- **Expected:** no extraction/insertion by hopper or hopper minecart; chest cannot be mined or exploded even when empty. *(I06 L17)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-10 — Two players: one shared inventory, no per-player reroll
- **Status:** LIVE VALIDATION PENDING
- **Setup:** two players (P1, P2) on one server, both ≥ 8 free slots. Use a **second, undisturbed pyramid** (or a fresh seed) so the chest is not yet generated; defeat the guardian with P1 (P2 may help).
- **Steps:** (1) P1 opens the chest first (generation). (2) With P1's GUI still open, P2 opens the same chest. (3) P1 takes the diamonds; P2 watches the slot update. (4) P2 takes everything left. (5) P1 re-opens. (6) Optional: have P2 open it before P1 in a third pyramid.
- **Expected:** both see the **same** contents (shared inventory); whoever opens first triggers the single generation; a stack taken by P1 disappears for P2 (normal chest semantics, no duplication); P2 gets no separate roll; after P2 empties it, P1 sees it empty. *(I06 L16 "One shared inventory per chest for all players"; D13Q Q31 "공유 월드 전리품; 플레이어별 재추첨 없음")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-11 — Defeat persists across restart; unsealed-but-unopened chest survives restart
- **Status:** LIVE VALIDATION PENDING
- **Setup:** a pyramid whose guardian is defeated but whose chest has **not** been opened (repeat R-05 on a fresh pyramid; do not right-click the chest afterwards).
- **Steps:** server restart. `/mmtomb status <AX> <AY> <AZ>`. Right-click the chest.
- **Expected:** `DEFEATED sealed=false`; no guardian ever spawns again; the chest opens and **generates now** (first valid opening after defeat). *(I06 L14 "a valid defeat is persisted and prevents refarming"; L15)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### R-12 — SECONDARY chest and treasury chest **[GAP / RECORD ONLY]**
- **Status:** LIVE VALIDATION PENDING (SECONDARY: **BLOCKED — not placed on this base**)
- **Setup:** explore the whole pyramid, including the treasury and crypt.
- **Steps:** (1) Look for a second royal reward chest. (2) Find the separate treasury chest (authored at G + (25, −26, 26), rotated like R-01) and right-click it.
- **Expected:** (1) none exists on this base — I06 describes two chests, but the G0-R1 record keeps "absent SECONDARY" and `St01RoyalChestPlacement.registerPlaced` registers none; **do not mark PASS**; if you do find a second chest, record where and whether it seals. (2) The treasury UI chest is an ordinary vanilla chest — no seal, no 54-slot royal menu. *(I06 L7 "separate large treasury chest … excluded", L75; `St01PyramidAssembly.TREASURY_UI_ANCHOR` comment)*
- **Result:** ☐ SECONDARY BLOCKED confirmed ☐ treasury ordinary ☐ FAIL — notes: ____________

---

## ACT-04 — accessories: acquisition, five slots, T1 wearing effects, cooldowns

Facts from the code that shape the checklist:

- Registered accessory item ids (all `moreminecrafts:`): T2 `snow_charm`, `deep_sea_pearl`, `ember_ring`, `ring_of_life`, `whisper_of_the_sea`, `star_covenant`, `phantom_mirror`, `ring_of_time`, `blood_covenant`, `seed_of_life`, `heart_of_gravity`, `soul_chain`; T1 `t1_combo`, `t1_crisis_escape`, `t1_heavy_buffer`, `t1_status_resilience`, `t1_sustained_recovery`, `t1_sprint_acceleration`, `t1_double_jump` (`AccessoryDefinition`, tag `moreminecrafts:t1_accessories`).
- Slot index = category order: **0 Combat, 1 Defense / Survival, 2 Support / Recovery, 3 Exploration / Mobility, 4 Lifestyle / Gathering** (`AccessorySlotRules`: one item, stack size 1, category must match). Category of each item: Combat — `star_covenant`, `phantom_mirror`, `ring_of_time`, `blood_covenant`, `heart_of_gravity`, `soul_chain`, `t1_combo`, `t1_crisis_escape`; Defense/Survival — `ember_ring`, `ring_of_life`, `t1_heavy_buffer`, `t1_status_resilience`; Support/Recovery — `seed_of_life`, `t1_sustained_recovery`; Exploration/Mobility — `snow_charm`, `deep_sea_pearl`, `t1_sprint_acceleration`, `t1_double_jump`; Lifestyle/Gathering — `whisper_of_the_sea`.
- **[GAP] T1 accessories have no acquisition path in code or authority**: D01 Q4/Q40 call acquisition "mixed craft + exploration", Q41/Q106 defer every recipe, and there is no T1 loot injection. The only structure-chest acquisition that exists is the **T2** pools in `T2StructureChestAccessories` plus the pyramid royal chest (ACT-03). T1 items are therefore obtained with `/give` on this base (A-01 records this).
- T1 cooldowns are server-side online-tick timers; T1 timers for Crisis Escape / Heavy Buffer / Double Jump are **reset on death** (D01 Q75, Q96), while T2 cooldowns **survive death** (CD L7).

Give kit (one command per line; use `/clear @s` between sections if inventory is full):

```
/give @s moreminecrafts:t1_combo
/give @s moreminecrafts:t1_crisis_escape
/give @s moreminecrafts:t1_heavy_buffer
/give @s moreminecrafts:t1_status_resilience
/give @s moreminecrafts:t1_sustained_recovery
/give @s moreminecrafts:t1_sprint_acceleration
/give @s moreminecrafts:t1_double_jump
/give @s moreminecrafts:ring_of_life
/give @s moreminecrafts:seed_of_life
/give @s moreminecrafts:ring_of_time
/give @s moreminecrafts:whisper_of_the_sea
```

### Acquisition from structure chests

#### A-01 — T1 acquisition path audit **[GAP / RECORD ONLY]**
- **Status:** LIVE VALIDATION PENDING
- **Steps:** in a survival world look for any way to obtain a T1 accessory other than `/give`: creative inventory tab, crafting table / smithing / anvil recipe search (recipe book), the chests opened in A-02/A-03, the royal chest (R-06).
- **Expected:** none. Acquisition and recipes are deferred by authority (D01 Q4, Q40, Q41, Q106); finding one is a RECORD ONLY note, finding none is consistent. *(D01 "후속 설계 공백" item 4)*
- **Result:** ☐ none found ☐ found: ____ notes: ____________

#### A-02 — T2 chest pools are wired into the four vanilla structure tables (loot-command pass)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival or creative, big empty inventory. Run each command repeatedly (↑ + Enter), counting runs and accessory hits. Roughly 20 runs for the 15% table and 60 runs for each 5% table give ≈95% chance of at least one hit.

```
/loot give @s loot minecraft:chests/nether_bridge        # 5%: ember_ring or soul_chain, 1:1  (2.5% each)
/loot give @s loot minecraft:chests/shipwreck_treasure   # 5%: whisper_of_the_sea
/loot give @s loot minecraft:chests/ancient_city         # 5%: ring_of_life or phantom_mirror, 1:1
/loot give @s loot minecraft:chests/bastion_treasure     # 15%: blood_covenant
```

- **Expected:** every extra accessory is **only** the id listed for that table; never two accessories from one table in one run; the ordinary vanilla loot of the table still appears alongside (pools are additive, nothing replaced). Zero hits in N runs is **STAT-inconclusive**, not FAIL; a wrong-table accessory is FAIL. *(D13Q Q01–Q04 nether bridge; Q05 shipwreck treasure only; Q06–Q08 ancient city (Ancient Heart and Relic Catalyst pools preserved); Q09–Q10 bastion treasure 15%)*
- **Result:** ☐ PASS ☐ FAIL — runs / hits per table: bridge ___/___ shipwreck ___/___ city ___/___ bastion ___/___ notes: ____

#### A-03 — Real chest block entity path (first open generates once)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** flat creative world.

```
/setblock ~ ~ ~2 minecraft:chest{LootTable:"minecraft:chests/bastion_treasure"}
/setblock ~2 ~ ~2 minecraft:chest{LootTable:"minecraft:chests/ancient_city"}
/setblock ~4 ~ ~2 minecraft:chest{LootTable:"minecraft:chests/nether_bridge"}
/setblock ~6 ~ ~2 minecraft:chest{LootTable:"minecraft:chests/shipwreck_treasure"}
```

- **Steps:** open each chest once, leave the contents, close, reopen; relog; reopen. Repeat with fresh chests until a bastion accessory shows (≈7 chests on average) and note it. Optional: open one chest in a naturally generated nether fortress / bastion treasure room / ancient city / shipwreck treasure chest.
- **Expected:** no errors in the log; contents are generated on first open and **do not change** on reopen/relog (no re-roll); accessory, if any, is the table's own id. Chests whose loot was already generated before the pools existed are unchanged (D13Q Q36 — only ungenerated loot). *(D13Q Q02/Q05/Q06/Q10, Q36)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

### The five accessory slots

#### A-04 — Layout and tooltips
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival, GUI scale auto, window ≥ 760 px wide (so GUI width ≥ 379, not vanilla narrow mode). Items from the give kit.
- **Steps:** (1) Press E. (2) Open the recipe book. (3) Hover each of the five empty slots, nothing on cursor. (4) Put an accessory in its slot and hover it. (5) Shrink the window until the GUI is narrow (< 379 GUI px) and look again.
- **Expected:** (1) five ordinary-looking slots stacked vertically immediately **left of the armor slots**, no side panel, no heading, no permanent labels, no category icons. (2) recipe book order `recipe book | 5 accessory slots | armor | main inventory`; the book covers no slot or control. (3) hovering an empty slot shows its category name. (4) occupied item tooltip has one `Category: …` line. (5) narrow mode: book overlays the inventory as in vanilla and the accessory cells are inert — documented limitation, RECORD ONLY. *(UI L9, L11, L13, L14, L16 and "Remaining limitations")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### A-05 — Slot rules (category restriction, order, shift-click)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** give kit in inventory plus `/give @s moreminecrafts:t1_combo 2` (a stack of 2).
- **Steps:** (1) Drag each accessory onto every one of the five slots. (2) Shift-click each accessory from the inventory. (3) Shift-click an equipped accessory back. (4) Try the stack of 2 (drag and shift-click). (5) With `t1_combo` equipped try to also equip `t1_crisis_escape` (both Combat). (6) Same with `t1_sprint_acceleration` + `t1_double_jump` (both Exploration).
- **Expected:** an accessory is accepted **only** by its own category slot (order top→bottom: Combat, Defense/Survival, Support/Recovery, Exploration/Mobility, Lifestyle/Gathering); shift-click routes it to the right slot; one item per slot (a stack of 2 is not accepted as a stack — **RECORD** exactly what happens); the second same-category item cannot be equipped while the slot is full (one slot per category). *(UI L11 "keep … all behavior (drag, click, shift-click, restrictions, persistence)"; D01 Q3 "카테고리별 슬롯 1개 규칙 유지")*
- **Result:** ☐ PASS ☐ FAIL — notes (what happened with the stack of 2): ____________

#### A-06 — Equipment persistence: relog, restart, dimensions, death
- **Status:** LIVE VALIDATION PENDING
- **Setup:** equip one accessory per category (suggest `t1_combo`, `t1_heavy_buffer`, `t1_sustained_recovery`, `t1_double_jump`, `whisper_of_the_sea`).
- **Steps:** (1) Relog. (2) Server restart. (3) `/execute in minecraft:the_nether run tp @s 0 100 0`, then `/execute in minecraft:overworld run tp @s ~ ~ ~` back. (4) `/gamerule keep_inventory true`, `/kill @s`, respawn. (5) `/gamerule keep_inventory false`, `/kill @s`; look at the drops; pick them up.
- **Expected:** (1)–(3) all five slots keep their items and their effects keep working. (4) with keepInventory all five are retained. (5) **RECORD ONLY**: on this base equipped accessories drop as items with the death drops (`AccessoryDeathDrops`); no approved authority fixes accessory death/drop behaviour (`ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md` L59 lists "death/drop" among unfinalised items). *(UI L11 "persistence")*
- **Result:** ☐ PASS(1–4) ☐ FAIL — (5) observed: ____________

### T1 wearing effects

Common: survival, `/gamerule natural_health_regeneration false` where HP matters, `/difficulty normal`. Max HP 20 ⇒ 30% = 6 HP, 40% = 8 damage, 25% = 5 HP. `/damage` bypasses armor, so use an **unarmored** player; wait ≥ 1 s between `/damage` calls (vanilla invulnerability frames). Restore HP with `/effect give @s minecraft:instant_health 1 3`. All T1 are +0 (no enhancement) unless noted.

#### A-07 — `t1_combo` (Combat) — extra hit on every 4th valid melee hit
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_combo`, `/give @s minecraft:wooden_sword`; equip the accessory; target: `/summon minecraft:iron_golem ~ ~ ~3 {NoAI:1b}` (100 HP, neutral, does not fight back). Read HP with `/data get entity @e[type=minecraft:iron_golem,limit=1,sort=nearest] Health`.
- **Steps:** (1) Hit 4 times at full attack charge (wait for the cooldown icon to refill ≥ 90%), reading HP after each hit. (2) Hit 3 times, wait 5 s, hit once more. (3) Hit once fully charged, then swing rapidly 5 times (uncharged), then 3 more fully charged hits. (4) Hit 3 charged, unequip and re-equip the accessory, 1 more hit. (5) Hit 3 charged, relog, 1 more hit. (6) Shoot the golem 4 times with a bow (`/give @s minecraft:bow` + `/give @s minecraft:arrow 16`). (7) Hit 3 charged, take damage from a zombie, hit once more.
- **Expected:** (1) hits 1–3 remove the weapon's damage D each (wooden sword D = 4 ⇒ HP 96, 92, 88; if your D differs use your own hit-1 delta); hit 4 removes D + 35% of D ⇒ **≈ 5.4** (HP ≈ 82.6), one single damage event; then the count restarts. (2) the 5-second gap (> 4 s since the last valid hit) resets the count: hit 4 is **not** boosted. (3) the uncharged swings neither count nor refresh the timer, so the boost lands on the **4th charged** hit, not earlier. (4) unequip resets the count. (5) relog resets the count. (6) arrows never count. (7) taking damage does not reset the count. The extra part is not crit-amplified and ignores Sharpness/potions. *(D01 Q9, Q10, Q11, Q47, Q48, Q49, Q50, Q51, Q52, Q53, Q57, Q59, Q60, Q61)*
- **Result:** ☐ PASS ☐ FAIL — HP readings: ____________

#### A-08 — `t1_crisis_escape` (Combat) — shockwave + heal 4 below 30% HP, 90 s cooldown
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_crisis_escape` (equip in the Combat slot instead of `t1_combo`); `/summon minecraft:zombie ~2 ~ ~ {PersistenceRequired:1b}`, `/summon minecraft:zombie ~3 ~ ~ {PersistenceRequired:1b}`, `/summon minecraft:zombie ~6 ~ ~ {PersistenceRequired:1b}` (inside / just outside the 4-block radius), and one zombie behind a solid wall within 3 blocks.
- **Steps:** (1) `/damage @s 14` (HP 6 = exactly 30%). (2) `/damage @s 1` (HP 5). (3) At once `/damage @s 15` again within the next minute; note HP. (4) With the accessory **unequipped** `/damage @s 14`…`/damage @s 1`, then **equip** it (already < 30% and ready). (5) Cooldown/relog: after a trigger stay online 30 s, log out, wait 2 minutes offline, rejoin, restore HP, `/damage @s 15`; then wait until 95 s of **online** time since the trigger and repeat. (6) After a trigger `/kill @s`, respawn, repeat `/damage @s 15`.
- **Expected:** (1) no trigger. (2) triggers: heal **+4 HP**, cloud ring ≈ 4 blocks, zombies within 4 blocks (line of sight) are knocked outward/slightly up (≈ 2.5 blocks, not guaranteed), the far zombie and the one behind the wall are not; no direct damage to anything. (3) no second trigger during the 90 s cooldown. (4) equipping while already < 30% and ready triggers immediately. (5) offline time does not reduce the cooldown (still blocked after 2 min offline); it becomes ready after 90 s of **online** time. (6) death resets the cooldown (ready after respawn). Healing never exceeds max HP. *(D01 Q13, Q14, Q15, Q62, Q63, Q64, Q67, Q68, Q71, Q72, Q74, Q75, Q76, Q81)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### A-09 — `t1_heavy_buffer` (Defense/Survival) — 40% reduction of a ≥ 40%-max-HP hit, 60 s cooldown
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_heavy_buffer`, equip; full HP, no armor.
- **Steps:** (1) `/damage @s 7`. (2) restore HP, `/damage @s 8`. (3) restore HP, wait 65 s, `/damage @s 10`. (4) within the next 60 s `/damage @s 10`. (5) relog after a trigger and repeat (4) offline-pause style as in A-08 (5). (6) trigger, `/kill @s`, respawn, `/damage @s 10`.
- **Expected:** (1) full 7 damage, no cooldown started. (2) exactly the threshold triggers: 8 → **4.8** damage. (3) 10 → **6** damage. (4) full 10 damage while on cooldown. (5) offline time does not count down the 60 s. (6) death resets it (reduced again). *(D01 Q18, Q19, Q20, Q21 and the Q74/Q75 lifecycle used by T1 timers)*
- **Result:** ☐ PASS ☐ FAIL — HP deltas: ____________

#### A-10 — `t1_status_resilience` (Defense/Survival) — harmful effect duration −20%
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_status_resilience`; compare unequipped vs equipped.
- **Steps:** (1) unequipped: `/effect give @s minecraft:poison 20 0`, read the remaining time in the effect HUD. (2) equipped: same; also `minecraft:slowness 20 0`, `minecraft:weakness 20 0`. (3) `/effect give @s minecraft:instant_damage 1 0`. (4) beneficial: `/effect give @s minecraft:speed 20 0`. (5) re-apply poison while it is running.
- **Expected:** (1) 0:20. (2) harmful timed effects start at **≈ 0:16** (20% shorter). (3) instantaneous effects are not shortened. (4) beneficial effects unchanged (0:20). (5) record: applying again must not stack the reduction twice (undefined by authority — RECORD ONLY). *(D01 Q23, Q24, Q26; "상태이상 갱신/재부여 시 중복 감소" is listed as undecided)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### A-11 — `t1_sustained_recovery` (Support/Recovery) — heal after 10 s without damage, then 1 HP / 3 s
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_sustained_recovery`, equip; `/gamerule natural_health_regeneration false`; `/damage @s 10` (HP 10).
- **Steps:** (1) Do nothing for ~12 s with a stopwatch from the damage; then watch ~10 s more. (2) During the heal phase `/damage @s 2`; keep waiting. (3) Unequip and re-equip; wait. (4) Relog; wait (record only).
- **Expected:** (1) no heal for 10 s (200 ticks) after the last damage; then +1 HP and +1 HP every **3 s** (60 ticks). (2) taking damage cancels the heal and restarts the 10 s wait. (3) re-equipping restarts the 10 s wait. (4) RECORD ONLY (timer is not persisted; authority leaves it to later spec). *(D01 Q27, Q28, Q29; "재장착/재접속 시 타이머 처리 … 후속")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### A-12 — `t1_sprint_acceleration` (Exploration/Mobility) — +15% after 5 s continuous sprint
- **Status:** LIVE VALIDATION PENDING
- **Setup:** a flat, straight, obstacle-free 120-block runway (e.g. a stone line in a superflat world); `/give @s moreminecrafts:t1_sprint_acceleration`; stopwatch or screen recording.
- **Steps:** (1) accessory **unequipped**: sprint 120 blocks, time it. (2) equipped: sprint the same 120 blocks. (3) Sprint 3 s, release, sprint again; compare. (4) Equipped, sprint > 5 s then unequip mid-run.
- **Expected:** (1) ≈ 21.4 s (vanilla sprint ≈ 5.6 b/s). (2) ≈ 19.2 s: the first 5 s at normal speed, then **+15%** (≈ 2 s faster overall). (3) any sprint interruption drops the bonus at once and restarts the 5 s count. (4) unequipping removes the bonus immediately. Values are approximate by design — large deviations (no change, or a bonus before 5 s) are the FAIL signal. *(D01 Q33, Q34, Q99, Q101, Q103)*
- **Result:** ☐ PASS ☐ FAIL — times: unequipped ___ s equipped ___ s notes: ____

#### A-13 — `t1_double_jump` (Exploration/Mobility) — one mid-air jump, 30 s cooldown
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:t1_double_jump` (remove `t1_sprint_acceleration` first); `/give @s minecraft:elytra`; water and a ladder nearby; stopwatch.
- **Steps:** (1) Jump, press jump again in mid-air (rising and falling). (2) Land and immediately jump + press again. (3) After ≥ 30 s of online time repeat. (4) Fall from ~10 blocks (`/tp @s ~ ~10 ~`), press jump again ~3 blocks above the ground. (5) Try in water, on a ladder, riding a boat, and while gliding with the elytra. (6) Use it, log out after 10 s, wait 1 min offline, rejoin, try; then wait until 30 s of **online** time since use. (7) Use it, `/kill @s`, respawn, try. (8) Unequip/re-equip right after use and try.
- **Expected:** (1) one extra lift of about **1.5 blocks** (target, not a fixed velocity), works rising or falling, once per air time. (2) denied: cooldown still running even though you touched the ground. (3) works again after 30 s. (4) fall damage is counted from the double-jump point, so far lower than a plain 10-block fall (≈ 7). (5) not usable in water/ladder/vehicle/elytra. (6) offline time does not reduce the cooldown. (7) death resets it. (8) unequipping does not reset it (keeps counting online). Particles/sound on use. *(D01 Q36, Q37, Q89, Q90, Q91, Q92, Q93, Q94, Q95, Q96, Q97, Q98; D01-Q107 particles/sound for activations)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

### Cooldown lifecycle (T2, policy B) and multiplayer

#### A-14 — Ring of Life cooldown lifecycle: unequip, relog, offline, death, re-arm
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/give @s moreminecrafts:ring_of_life` (+0: Regeneration I for 4 s, **120 s** cooldown, activates at ≤ 25% HP = ≤ 5 of 20); equip in Defense/Survival (remove `t1_heavy_buffer`); `/gamerule natural_health_regeneration false`.
- **Steps:** (1) `/damage @s 14` (HP 6), then `/damage @s 1` (HP 5). (2) Restore HP to full, `/damage @s 15` within 120 s. (3) Unequip immediately after a trigger, wait 60 s, re-equip, restore HP, `/damage @s 15`. (4) Trigger, stay online 30 s, log out, wait 3 minutes real time, rejoin, restore HP, `/damage @s 15`. (5) Trigger, `/kill @s`, respawn, restore HP, `/damage @s 15`. (6) Trigger; restore HP above 25% (re-arms the ring); then `/damage @s 15` (HP 5, ring armed, cooldown still running) and keep HP ≤ 5 (re-apply `/damage` after any regeneration) until the 120 s cooldown expires — watch for one activation at expiry. (7) Trigger, server restart, repeat (2).
- **Expected:** (1) 6 HP does not trigger; 5 HP (exactly 25%) does: regeneration for 4 s. (2) blocked — still on cooldown. (3) the cooldown kept running while unequipped (≈ 60 s left, so blocked) and the timer is **not reset by unequip**. (4) offline time does not reduce it: still blocked after 3 min offline. (5) the remaining cooldown **survives death and respawn** (blocked), unlike T1 timers. (6) activates **once** at expiry if HP is still ≤ 25% and the ring is armed; expiry alone never re-arms it; it does not re-fire while HP stays low. (7) remaining ticks survive a server restart. *(CD L5–L8 policy B; CD L19 re-arm; CD L22 Option A "ALLOW ONE ACTIVATION")*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### A-15 — Two players: independent slots, independent cooldowns, PvP-gated shockwave
- **Status:** LIVE VALIDATION PENDING
- **Setup:** P1 and P2 on one server, both unarmored. P1 wears `t1_heavy_buffer` + `t1_crisis_escape`; P2 wears `t1_heavy_buffer` only. `pvp` on (`server.properties pvp=true`).
- **Steps:** (1) `/damage @s 10` on P1 (trigger), then immediately on P2. (2) P1 `/damage @s 15` with P2 standing 2 blocks away; then repeat with PvP off (set `pvp=false` / `/gamerule pvp false` if the rule exists, else restart with `pvp=false`) after the cooldown. (3) P1 relogs; P2 stays online.
- **Expected:** (1) both get the 60% damage — one player's cooldown does not block the other (cooldowns are per player and per ability). (2) with PvP on, P2 (not P1 itself) is pushed away by P1's shockwave regardless of team; with PvP off P2 is not affected. P2's slots/items are untouched by P1. (3) P2's loadout and timers are unaffected by P1's relog. *(CD L5 "per player"; D01 Q73, Q79)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

*Out of scope for this list:* the numeric ability values of the twelve T2 accessories (October 16/17 records) and the earlier accessory UI/anvil checks are not re-listed here; ACT-04 in this list covers acquisition, the five slots, T1 effects and cooldown lifecycle only.

---

## ACT-05 — Sky Tower: rooftop vault, eight normal vaults, trial key, Star Covenant

Facts from the code that shape the checklist (verified in `src/main`):

- A tower is the template `moreminecrafts:sky_tower/full_01` (≈ 50×80×49 blocks), a piece of the sky-island structures (`moreminecrafts:sky_island_snowy|meadow|forest|desert|badlands` and the mountain variants `sky_island_mountain_snowy|rocky|meadow`; the tower is one of several island elements, so not every island has one). Towers register themselves on chunk load (`SkyTowerWorldgenRegistration`); `/mmskytower status` lists them with ids `minecraft:overworld|X,Y,Z|R` (origin X,Y,Z and rotation R 0–3).
- **Controlled placement** (deterministic, not natural worldgen): `/place template moreminecrafts:sky_tower/full_01 X Y Z [rotation]` then `/mmskytower register X Y Z R` (absolute integers — `~` is not accepted). Registration configures the 8 normal vaults and places the rooftop vault if its cell is air.
- Vault cells (offsets from the registered origin `X Y Z`, rotation R). Normal vaults index 0–7 are two per floor on floors 1–4; the rooftop vault is `at(24, 65, 11)`:

| R (place rotation) | Rooftop vault | Rooftop `facing` | Normal vaults 0 / 1 (floor 1, Y+11) | 2 / 3 (Y+22) | 4 / 5 (Y+34) | 6 / 7 (Y+48) |
|---|---|---|---|---|---|---|
| 0 (`none`) | X+24, Y+65, Z+11 | north | X+16, Z+15 / X+16, Z+33 | same X/Z | same X/Z | same X/Z |
| 1 (`clockwise_90`) | X−11, Y+65, Z+24 | east | X−15, Z+16 / X−33, Z+16 | same X/Z | same X/Z | same X/Z |
| 2 (`180`) | X−24, Y+65, Z−11 | south | X−16, Z−15 / X−16, Z−33 | same X/Z | same X/Z | same X/Z |
| 3 (`counterclockwise_90`) | X+11, Y+65, Z−24 | west | X+15, Z−16 / X+33, Z−16 | same X/Z | same X/Z | same X/Z |

  (Even index = first X/Z of the pair, odd index = second; Y offsets 11 / 22 / 34 / 48 for index pairs 0–1 / 2–3 / 4–5 / 6–7.) Rooftop rows are exactly the P4 §3 mapping; normal vaults are `SkyTowerLayout.Site.vault(i)`.
- Keys: normal and rooftop vaults both use the vanilla `minecraft:trial_key` (P4 row 4). Loot tables: normal `moreminecrafts:chests/sky_islands/vault`, rooftop `moreminecrafts:chests/sky_islands/vault_tower_top` (P4 row 5), both include `moreminecrafts:sky_feather` 15% and `moreminecrafts:gale_shard` 5%.
- The Star Covenant roll is keyed by (player, tower id, `"rooftop"`), is **claimed before it is rolled**, and a miss is just as final as a hit (`StarCovenantVaultRewards.resolve`). A vault can't be unlocked twice by the same player (vanilla), so a **single player gets exactly one 10% draw per tower**; use several towers or several accounts to see a hit.

Setup (controlled placement; repeat per tower, spacing ≥ 64 blocks apart on X):

```
/gamemode creative
/tp @s <X> <Y+1> <Z>
/forceload add <X-60> <Z-60> <X+60> <Z+60>
/place template moreminecrafts:sky_tower/full_01 <X> <Y> <Z>              # rotation 0; add clockwise_90 | 180 | counterclockwise_90 for R = 1|2|3
/mmskytower register <X> <Y> <Z> <R>
/mmskytower status
/give @s minecraft:trial_key 16
```

#### S-01 — Natural sky-island towers register themselves
- **Status:** LIVE VALIDATION PENDING
- **Setup:** new world; `/locate structure moreminecrafts:sky_island_snowy` (repeat for `…_meadow`, `…_forest`, `…_desert`, `…_badlands`, `…_mountain_snowy`, `…_mountain_rocky`, `…_mountain_meadow`); `/tp` near; fly around the island.
- **Steps:** find a tower (a circular multi-floor temple tower with an open rooftop sanctuary). `/mmskytower status`. Check the server log for `Registered generated Sky Tower …`.
- **Expected:** a tower exists on at least one located island; `status` lists its site id `minecraft:overworld|X,Y,Z|R` (use those numbers for the tables above). Tower frequency is not specified by authority — RECORD ONLY how many islands you checked before finding one. *(P4 row 7 "Runtime placement via registration"; ACT-05 registry row)*
- **Result:** ☐ PASS ☐ FAIL — islands checked ___ site id ____ notes: ____

#### S-02 — Eight normal vaults and the rooftop vault exist and are configured
- **Status:** LIVE VALIDATION PENDING
- **Setup:** a registered tower (S-01 natural, or controlled placement above). Use the offset table.
- **Steps:** for each of the 8 normal cells and the rooftop cell: `/tp @s <cell>`, look at the block (F3: `minecraft:vault`), then `/data get block <x> <y> <z> config`.
- **Expected:** 8 `minecraft:vault` blocks (2 per floor, floors 1–4, in the outer arch niches) with `config.loot_table = "moreminecrafts:chests/sky_islands/vault"`; **exactly one** rooftop `minecraft:vault` at the table position with `config.loot_table = "moreminecrafts:chests/sky_islands/vault_tower_top"`, key item `minecraft:trial_key`, `ominous=false`, `facing` per the table. The rooftop vault is open-air with clear sky above it. *(P4 rows 1, 2, 3, 4, 5, 6; STQ Q08, Q18; REW "Normal Vault Count")*
- **Result:** ☐ PASS ☐ FAIL — missing/extra vault: ____________

#### S-03 — All four rotations (controlled placement)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** four towers with R = 0, 1, 2, 3 (placement command rotation `none`, `clockwise_90`, `180`, `counterclockwise_90`; register with the same R).
- **Steps:** repeat S-02's rooftop check and one normal-vault check per tower.
- **Expected:** every tower's rooftop vault is at its tabled cell with the tabled `facing`; each tower still has all 8 normal vaults. *(P4 rows 1, 3 and §3 rotation table)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### S-04 — Normal vault unlock with a trial key (each of the 8)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival; `/give @s minecraft:trial_key 16`; a registered tower.
- **Steps:** for each of the 8 normal vaults: right-click it holding a trial key; note the items ejected and whether the key is consumed. Then right-click the same vault again with another key. Then hold `minecraft:ominous_trial_key` (`/give @s minecraft:ominous_trial_key 2`) on a vault you have not opened.
- **Expected:** a normal trial key opens the vault (unlock animation, rewards ejected upward, **1 key consumed**); the same player cannot open that vault again (vanilla: rejected, key kept); the ominous key does **not** open these (normal) vaults. Rewards come from the sky-island table: 1–3 draws from vanilla trial-chamber `reward_common` plus one primary draw (rare/common/sky materials), 25% `reward_unique`, **STAT** 15% `moreminecrafts:sky_feather`, **STAT** 5% `moreminecrafts:gale_shard`; sky materials are `mythril` / `titanium_ingot` / `tungsten_ingot` (1–2) / `adamantium_ingot` (1). Record what each vault produced: ____. No Star Covenant ever drops from a normal vault. *(P4 rows 4, 5, 6; REW normal-vault rows; D13Q Q11 "깃털 15%·돌풍 파편 5% 보존"; STQ Q07 vanilla trial/vault rules kept)*
- **Result:** ☐ PASS ☐ FAIL — per-vault loot: v0 ____ v1 ____ v2 ____ v3 ____ v4 ____ v5 ____ v6 ____ v7 ____

#### S-05 — Rooftop vault unlock and key rules
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival; fresh tower (this player has not opened its rooftop vault); `/give @s minecraft:trial_key 4`, `/give @s minecraft:ominous_trial_key 2`, `/give @s minecraft:stick 1`.
- **Steps:** (1) Right-click the rooftop vault with a stick, then with the **ominous** key. (2) Right-click with a normal `minecraft:trial_key`. (3) Right-click again with another normal key. (4) Look at the ejected items.
- **Expected:** (1) neither opens it; no key consumed; **no Star Covenant roll is spent** (a failed/empty unlock must not consume the claim). (2) opens; 1 key consumed; items ejected upward with vanilla timing/sound/particles. (3) rejected, key kept. (4) items are from `vault_tower_top` (same pools as S-04 but rooftop weights: rare 14 / common 2 / sky materials 6; `reward_unique` **50%**; Sky Feather 15% **STAT**, Gale Shard 5% **STAT**); **STAT**: one additional `moreminecrafts:star_covenant` with 10% chance. Record: ☐ Star Covenant HIT ☐ MISS. *(P4 rows 4, 5, 9; REW rooftop rows; D13Q Q11)*
- **Result:** ☐ PASS ☐ FAIL — loot: ____________

#### S-06 — Two players: independent unlocks and independent Star Covenant rolls
- **Status:** LIVE VALIDATION PENDING
- **Setup:** P1 and P2 on a server, a fresh tower, each with `/give @s minecraft:trial_key 4`.
- **Steps:** (1) P1 opens the rooftop vault. (2) P2 opens the same rooftop vault. (3) P2 opens one normal vault that P1 already opened; P1 tries it again. (4) Record both players' loot and each one's Star Covenant HIT/MISS. (5) P1 relogs, tries the rooftop vault again.
- **Expected:** vault eligibility and the 10% draw are **per player**: P2 can unlock what P1 already unlocked and gets their own roll, P1 cannot repeat; one player's result never changes the other's; a re-login does not grant a second draw. *(D13Q Q11 "플레이어별 10%", Q12 "플레이어별 금고당 1회, 다른 탑·금고 독립"; P4 row 9)*
- **Result:** ☐ PASS ☐ FAIL — P1 HIT/MISS ___ P2 HIT/MISS ___ notes: ____

#### S-07 — Star Covenant HIT path (STAT hunt) — optional but required to close ACT-05
- **Status:** LIVE VALIDATION PENDING
- **Setup:** creative superflat world, `/forceload` around each site; place and register N = 20–30 towers at ≥ 64-block X spacing (controlled placement above); `/give @s minecraft:trial_key 64`.
- **Steps:** survival; for each tower open its rooftop vault once; record HIT/MISS in the table (tower id ↔ result). Optionally use extra accounts (each gets its own draw per tower). Pick up and equip a dropped `moreminecrafts:star_covenant`.
- **Expected:** about 10% of draws drop one extra `moreminecrafts:star_covenant` item (it is a Combat accessory — it equips in the Combat slot). 20 towers give ≈ 88% and 30 towers ≈ 96% chance of ≥ 1 HIT; **0 hits is not a FAIL**, it leaves the HIT path LIVE VALIDATION PENDING. The observed rate is only a sanity check, not proof of exactly 10%. *(D13Q Q11; P4 row 9; `StarCovenantVaultRewards.CHANCE = 0.10`)*
- **Result:** ☐ HIT observed ☐ no HIT in ___ draws ☐ FAIL — hits ___ / draws ___ notes: ____

#### S-08 — Persistence: chunk unload, relog, server restart
- **Status:** LIVE VALIDATION PENDING
- **Setup:** a tower whose rooftop vault P1 has opened (S-05) and whose normal vaults P1 has partly opened.
- **Steps:** (1) `/tp` 1000 blocks away, wait 30 s, return. (2) P1 relogs. (3) Server restart. (4) After each, `/mmskytower status`, check the rooftop block (`/data get block <rooftop> config`), P1 tries the rooftop and the opened normal vaults again with a key, P2 (not yet opened) tries the rooftop.
- **Expected:** the tower stays registered (same id); exactly one rooftop vault with the same config (no duplicate/relocated vault); P1 still cannot re-unlock any vault already opened (key kept) and gets no extra Star Covenant; P2 can still unlock and gets exactly one draw. The saved claim ledger `moreminecrafts:star_covenant_vault_claims` is a world file — live play can only observe its effect (no second reward), so RECORD that. *(D13Q Q12 "필요 시 플레이어·금고 영속 상태"; P4 rows 7, 9)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

#### S-09 — Obtaining the trial key legitimately (observation)
- **Status:** LIVE VALIDATION PENDING
- **Setup:** survival, gear for a trial; a tower whose floor-1 trial spawners are untouched.
- **Steps:** complete a floor-1 trial spawner (defeat its waves); collect what it ejects; use a key you earned on a normal vault of the same tower. Note whether the next stair barrier opens after **all** that floor's spawners are done (`/mmskytower status` shows `completedSpawnerMask`).
- **Expected:** vanilla trial-spawner behaviour is unchanged (STQ Q07 "기존 trial spawner 규칙과 보상 유지; 커스텀 웨이브 관리자 추가 금지"). Where the key comes from is not specified beyond that — **RECORD ONLY** whether the spawner reward is a `minecraft:trial_key` and whether it opens these vaults. Floor completion permanently opens the next barrier for everyone and survives restart; barriers only block normal movement (bypass by block placement/ender pearl/flight allowed). *(STQ Q19, Q20, Q22)*
- **Result:** ☐ PASS ☐ FAIL ☐ RECORD ONLY — key came from: ____ notes: ____

#### S-10 — Regression: other vaults are untouched
- **Status:** LIVE VALIDATION PENDING
- **Setup:** `/locate structure minecraft:trial_chambers`, `/tp` there; `/give @s minecraft:trial_key 4`.
- **Steps:** open two vanilla Trial Chambers vaults; place one manual vault `/setblock ~ ~ ~ minecraft:vault` and open it.
- **Expected:** vanilla loot only — no `moreminecrafts:sky_feather`/`gale_shard` bonus pools, **never** `moreminecrafts:star_covenant`; vanilla key rules unchanged. *(P4 header "Scopes custom reward pools to Sky Tower vaults"; D13Q Q11 limits Star Covenant to the Sky Tower top vault)*
- **Result:** ☐ PASS ☐ FAIL — notes: ____________

---

## Roll-up (fill in after the run)

| Group | Checks | PASS | FAIL | BLOCKED / RECORD ONLY |
|---|---|---|---|---|
| ACT-03 royal chest | R-01 … R-12 | | | R-12 (SECONDARY) |
| ACT-04 accessories | A-01 … A-15 | | | A-01 (T1 acquisition) |
| ACT-05 Sky Tower | S-01 … S-10 | | | S-09, S-07 if no hit |

Overall: **LIVE VALIDATION PENDING** (no result recorded). When a group is finished, update `docs/integration/FEATURE_STATUS_REGISTRY_20261112.md` blockers in a separate change — this list does not change any registry status.

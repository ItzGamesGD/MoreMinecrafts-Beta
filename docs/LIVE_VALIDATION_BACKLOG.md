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

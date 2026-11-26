# Stage 23-B Research Effect Design Decisions (2026-11-24)

STATUS: USER-APPROVED DESIGN AUTHORITY. Design-only; implementation, CI, and live validation NOT claimed. This addendum resolves the 12 open design questions identified during the `checkpoint/grand-unified-20261123` research-handler audit and supplements [RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md](RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md).

## 1. Block Entity Research Attribution (`.26`, `.59`, `.60`, `.66`)
- For unattended block entities (Brewing Stand for `v3.research.59` / `.60` / `.66`, Fish Trap for `v3.research.26`), persist the UUID of the **last player who inserted ingredients or interacted** with the device, matching the existing `FurnaceResearchState` attribution pattern.

## 2. Brewing Ingredients & Catalysts (`.63`, `.65`)
- For `v3.research.63` (Magical Material Study) and `v3.research.65` (Catalyst Preservation), define eligible brewing ingredients and catalysts explicitly via data `TagKey` definitions covering standard vanilla brewing materials/catalysts (e.g., nether wart, glowstone dust, redstone, gunpowder, dragon's breath, fermented spider eye, and standard potion reagents) rather than implicit recipe scanning.

## 3. Piglin Barter vs. Piglin Trading (`.54`, `piglin_barter`)
- Keep `v3.research.54` (3 levels) as the vanilla gold-ingot piglin bartering rare-reward weight bonus (+5% / +10% / +15%).
- Define V4 `piglin_barter` (1 level) as a separate additive/special bartering bonus rather than duplicating `.54` or replacing `piglin_trading` / `piglin_trading_2`.

## 4. Automatic Tool Switch (`automatic_tool_switch`)
- On mining start, if the held main-hand tool is unsuitable for the target block, automatically switch to the highest-efficiency suitable tool in the player's hotbar.
- Do not auto-restore the previous slot after mining; apply a short cooldown between automatic switches.

## 5. Crop Cultivation (`.14`)
- Apply `v3.research.14` extra harvest amounts/chances exclusively to vanilla `CropBlock`-based staple food crops (wheat, carrots, potatoes, beetroots) to keep roles distinct from `v3.research.15` (Seed Study).

## 6. Soil Study L3 (`.16`)
- Confirm the current runtime L3 implementation in `SoilStudyRuntime` (hydration reach 6 + +20% relative natural crop growth bonus) as the authoritative L3 specification, superseding the older 5-minute post-water-loss hydration description.

## 7. Ecology Mob Attribution (`.36`, `.37`, `.42`, `.43`)
- Apply `v3.research.36` (Zombie), `v3.research.37` (Skeleton), `v3.research.42` (Illager), and `v3.research.43` (Piglin) based on the mob's current target or recent attacker (`ServerPlayer`), matching the existing `ResearchBatch2Runtime` pattern used by Creeper/Spider/Enderman.

## 8. Rare Mobs (`.46`)
- Define `v3.research.46` (+5% / +8% / +12% damage) targets via an explicit entity tag (`moreminecrafts:research/ecology/rare_mobs`) mapping eligible vanilla and custom rare mobs while preserving boss-exclusion rules.

## 9. Archaeology (`.52`)
- Apply brush-time reduction (-10% / -20% / -30%) to the brushing player.
- Apply structure-chest reward weight (+3% / +6% / +9%) and thematic extra roll (10% / 20% / 30%, using vanilla archaeology pools such as pottery sherds and armor trims) to the **first player who opens** the structure chest.

## 10. Shield Counter (`shield_counter`)
- After a successful shield block, open a 1.5-second (30-tick) counter window: the researcher's melee counterattack within this window deals **+20% damage** and triggers a **knockback shockwave**, with a **4-second (80-tick) cooldown**.

## 11. Falling Axe Smash (`falling_axe_smash`)
- Under vanilla critical-fall conditions (`fallDistance > 1.5`, airborne), an axe attack deals **+25% bonus damage** and triggers a small landing shockwave that disables shields, with a **3-second (60-tick) cooldown**.

## 12. Weapon Swap Handling (`weapon_swap_handling`)
- Immediately after switching the main-hand slot to a melee weapon (sword, axe, dagger, etc.), pre-fill **50% of the vanilla attack cooldown** (`attackStrengthTicker`) for the first strike, with a **2-second internal cooldown**.

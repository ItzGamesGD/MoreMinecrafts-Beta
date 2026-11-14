# ST-03 Pillager Headquarters — Owner-Approved Gameplay & Worldgen Measurement Authority (2026-11-14)

> **Status: OWNER-APPROVED DESIGN AUTHORITY (No runtime/worldgen implementation claimed).**  
> **Scope:** Replaces the unrecovered historical Q96–Q105 questions with newly approved gameplay decisions for ST-03 (Pillager Headquarters), assuming architectural modules are finalized (`MoreMinecrafts-Structure-Generator` PR #55 / Q01–Q95).  
> **Cross-references:**  
> - Concept & baseline rules: `docs/PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md` (PR #147)  
> - Architectural Q01–Q95 ledger: `docs/planning/ST03_OWNER_APPROVED_DESIGN_Q01_Q42_2026-11-04.md` (PR #225)  

---

## 1. Core Owner Principles & Overrides

1. **Q96–Q105 Non-Recovery:** Historical Q96–Q105 originals are intentionally not recovered; only required gameplay decisions are newly finalized here.
2. **Ravager Count:** Confirmed at **1 Ravager** (stationed at F10 Ravager Stable).
3. **Strict Vanilla Preservation (No Arbitrary Vanilla Stat Buffs):**  
   - **Do NOT arbitrarily increase vanilla mob attributes** (such as `follow_range`) during testing or implementation.  
   - *Audit note:* Raising Vindicator `follow_range` from vanilla `12` to `32` in the earlier Structure Generator F01 AI probe is explicitly rejected as an invalid deviation from vanilla behavior. Keep vanilla `follow_range` (`12`) and vanilla AI rules intact unless explicitly authorized otherwise.

---

## 2. Approved Gameplay Decisions

### 2.1 Garrison Spawn Trigger
- **Approved:** **Player-proximity one-time trigger**, maximizing acceptance of **vanilla rules**.
- When a player first enters the HQ proximity/bounding trigger, the finite initial garrison (35–45 total; nominal 40 baseline including 1 Ravager and 1 Commander) is initialized once per slot.
- Once spawned, entity AI, targeting, movement, and normal world interactions follow vanilla rules as closely as possible, while preserving the approved **no garrison respawn** and **persistent survivor** contract.

### 2.2 Commander Combat Rules & Deferred Boss Separation
- **Commander (Approved):**
  - Spawns in F01 Manor 2F war room.
  - **40 HP**, vanilla Vindicator melee AI (keep vanilla `follow_range` = `12`).
  - **+25% fixed melee attack damage** (midpoint of the approved +20–30% band) relative to an unmodified same-difficulty vanilla Vindicator, following standard vanilla difficulty scaling.
  - Visually distinct equipment/nametag (e.g., enchanted iron/diamond-tier axe appearance, commander nametag/banner) without changing vanilla melee mechanics.
- **Separate Dedicated Boss (Deferred Follow-up Design):**
  - A dedicated ST-03 Boss is **separated from the Commander** and recorded as a follow-up design task.
  - **Delegated Boss Design Direction:** Focus on **minimal movement, slow speed, and heavy/strong attacks** (low-mobility, high-impact combat identity).
  - **F01 2F Spatial Note:** F01 Manor 2F has `63` walkable feet cells (U-shaped gallery + rear war room, vs `122` on 1F and `109` on 3F). Low-mobility boss design should account for room clearance when its arena/location is decided.

### 2.3 Post-Clear Behavior (After All Illagers Are Eliminated)
- **Approved (Option D):**
  1. **Complete Hostile & Neutral Mob Spawn Suppression Inside HQ:** Once all stationed HQ illagers are eliminated, **no hostile monsters and no neutral mobs (e.g., Creepers, Endermen, etc.) may naturally spawn inside the HQ boundary**.
  2. **Village Conversion Condition:** Placing **5 or more beds** inside the HQ grants the compound **vanilla village status**.
  3. **Villager Spawning & Non-Respawn Rule:**
     - Once the 5-bed condition is met, starting at the **next morning**, the HQ evaluates villager spawning conditions **2 times per day** (deterministic condition check, not a random percentage roll), up to a **maximum of 15 villagers**. *(The mismatch between ≥5 beds and 15 max villagers is an intentional design rule.)*
     - Villagers do **not** bulk-spawn all at once.
     - Once villagers spawn, the area is treated as a **vanilla village**; if a spawned villager dies, it **never respawns** except through remaining unused daily spawn attempts.

### 2.4 Ordinary Loot Weights & Quantities
- **Approved (Option B + 5–10% Diamond Gear Pool):**
  - Because the compound is open in the center and enclosed by perimeter walls (making pure walk-out escape difficult once entered), provide a **5–10% Diamond equipment pool** in appropriate military/manor ordinary loot rolls.
  - All other ordinary loot (food, iron/gold ingots, crossbows, arrows, shields, materials) uses **Option B (abundant resource/supply pool)** with facility-specific separation (F05/F07 military, F08/kitchen food & general supplies, etc.).

### 2.5 Premium Loot Pool Weights
- **Approved (Option A — Original Conditional Category Weights):**
  - **F01 Manor (65% independent attempt):** `55%` MM Equipment / `30%` Combat Accessories / `15%` Real-Enchantment Runes.
  - **F05 Armory (35% independent attempt):** `75%` MM Equipment / `15%` Combat Accessories / `10%` Real-Enchantment Runes.
  - **Commander (10% independent special drop):** `50%` Combat Accessories / `40%` High-Tier Equipment / `10%` Real-Enchantment Runes.
  - *Fallback rule:* While the HQ-eligible rune catalog remains unfinalized, exclude the rune branch and **proportionally renormalize** weights over Equipment and Accessories.

### 2.6 Dedicated ST-03 Vanilla-Model Resource Variants & 40-Unit Garrison Roster
- **Hard Constraints:**
  - Zero custom 3D models or custom animations; 100% vanilla `Vindicator` / `Pillager` models, AI, and `64×64` UV skin textures (`VANILLA_MODEL_CUSTOM_MOB_ROSTER` pipeline, structure-exclusive without natural biome spawns).
  - Zero custom AI Goals, global alarms, synergy buffs, or reinforcement chains.
  - One-time persistent spawn (`PersistenceRequired: 1b`), no respawn, and no key/puzzle locks on chests.
- **4 Dedicated Variants (5 instances total):**
  1. `pillager_commander` (`Vindicator` base, F01 2F War Room ×1): `40.0` HP, `+25%` fixed melee damage, `iron_axe` (`unbreaking 1` glint).
  2. `pillager_beastmaster` (`Pillager` base, F10 Stable ×1): `24.0` HP, vanilla Pillager damage, `crossbow`.
  3. `armory_guard` (`Vindicator` base, F05 Armory ×2): `28.0` HP, vanilla Vindicator damage, `iron_sword` + `shield` (offhand).
  4. `pillager_drillmaster` (`Pillager` base, F06 Training Yard ×1): `30.0` HP, vanilla Pillager damage, `crossbow` (`quick_charge 1`).
- **Total Garrison (40 defenders + 2 non-combat Villager prisoners):**
  - `35` existing roster (`Bandit` ×9, `Bandit Warrior` ×8, `Bandit Archer` ×12, `Wandering Raider` ×5, `Ravager` ×1) + `5` dedicated variant instances = `40` defenders.
  - `F09` Prison additionally spawns `2` non-combat `Villager` prisoners outside the 40-defender elimination cap.

---

## 3. Worldgen Measurement Plan (Delegated Methodology)

Measurement methodology and statistical analysis are delegated to the engineering/agent pass before locking `spacing` and `separation` JSON parameters:

- **Seed Suite:** `10` deterministic seeds (`3` plains-heavy, `3` forest-heavy, `2` mountainous/high-relief control, `2` random).
- **Sample Area:** `4,000`-block radius from spawn per seed (`8,000 × 8,000` blocks / `64 km²` per seed).
- **Measured Metrics:**
  1. **112×112 Site Acceptance Rate:** Pass rate under `≤ 4–5` block natural-ground elevation variance (excluding forest canopy), plus rejection breakdown (slope, water, caves/voids, village/structure overlap).
  2. **Spatial Distribution:** Nearest-neighbor distance (mean, median, quartiles) between accepted HQ centers and count per `10,000 × 10,000` area.
  3. **Vanilla Benchmark Ratio:** Relative discovery frequency vs vanilla Pillager Outposts and Woodland Mansions on the same seeds, targeting **Rare Regional Landmark (B)**.

---

## 4. Open / Next Follow-Up Tasks

1. **Task 2 Brainstorming (Delegated Prompt Ready):**
   - Space-matched optional mobs (F09 Prison, F10 Stable, F05 Armory, F06 Training Yard) within vanilla-friendly rules.
   - Follow-up Boss design (**minimal movement, slow, strong attacks**).
2. **Worldgen Headless Probe Execution:** Run the 10-seed measurement plan and report spacing/separation recommendations.
3. **Runtime Implementation & Validation:** Implement spawn trigger, post-clear D-option village/spawn rules, loot tables, and verify in real Minecraft 26.3 without altering vanilla mob attributes.

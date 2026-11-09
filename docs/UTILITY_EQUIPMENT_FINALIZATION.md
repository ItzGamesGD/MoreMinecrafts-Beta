# MoreMinecrafts Utility Equipment Finalization

> **Status:** CURRENT AUTHORITY — active Utility Equipment roster and approved interaction contracts.
>
> This document records the human-approved Utility Equipment decisions made after the Stage 18 checkpoint. It defines gameplay identity and interaction semantics. Exact recipe material assignment, acquisition timing, implementation details, and visual-resource production remain separate unless explicitly locked below.

## 1. Active / deferred / deleted roster

### Active Utility Equipment

1. **Miner Lantern**
2. **Potion Injector**
3. **Exploration Sleeping Bag**
4. **Portable Furnace**
5. **Mining Drill**
6. **Backpack**

These six items are the current active Utility Equipment roster.

### Intentionally deferred

- **Glider**
- **Oxygen Tank**
- **Grappling Hook**

Deferred items are not deleted, but they are outside the active implementation-ready Utility roster until explicitly reactivated.

### Deleted / retired from Utility Equipment

- **Climbing Gloves**
- **equipment-form Jet Boots**
- **Flare**
- **Buoyancy Capsule**
- **Return Crystal**

The equipment-form Jet Boots must not be reintroduced. The separate Jet Boots enchantment remains governed by `ENCHANTMENT_BRANCH_SKELETON.md`.

---

## 2. Helmet socket UI

- The vanilla player inventory gains **one square socket slot immediately to the left of the existing helmet armor slot**.
- The slot uses the same basic square-slot visual grammar and dimensions as the neighboring vanilla equipment slot.
- It is reserved for approved helmet-socket items.
- The Miner Lantern is the currently approved item for this slot.
- This decision does **not** imply a larger accessory/jewelry system or additional socket count.
- Exact code layout, rendering hooks, and compatibility handling are implementation details.

---

## 3. Miner Lantern

### Role

A helmet-socket utility that provides moving visual illumination without placing persistent world blocks.

### Locked behavior

- Equipped through the helmet-adjacent socket defined above.
- While equipped, the player acts as a **dynamic light source at light level 15**.
- Dynamic lighting is implemented through **LambDynamicLights integration** rather than repeated placement/removal of `minecraft:light` blocks.
- The light follows the player/head position and uses normal dynamic-light occlusion/propagation behavior.
- It does not create persistent light blocks or otherwise modify the world block state.
- Removing the Lantern from the socket removes the dynamic-light effect.

### Not yet fixed

- Recipe/material cost.
- Whether the LambDynamicLights integration is packaged as required or optional compatibility is an implementation/dependency decision, not a gameplay redesign.

---

## 4. Potion Injector

### Role

A prepared-potion quick-use tool. It is not an automatic health trigger and does not choose effects for the player.

### Locked behavior

- Contains a **9-slot internal potion inventory**.
- **Left-click:** opens the Injector inventory.
- **Right-click:** uses the next stored potion in slot order **1 -> 9**.
- Empty slots do not create a separate selection mode; use proceeds in the fixed slot sequence.
- The consumed potion applies **immediately**, without the normal potion drinking time.
- The Injector is intended for general potion use rather than being restricted to healing or another single effect family.
- No health-threshold trigger, conditional automation, or separate potion-selection UI is part of the design.

### Not yet fixed

- Recipe/material cost.
- Final visual-resource implementation.

---

## 5. Exploration Sleeping Bag

### Locked recipe

A 2 x 2 recipe:

- top row: **2 Wool**
- bottom row: **2 Wood**

The exact wood-tag/item normalization is an implementation/data detail unless a later recipe pass requires a narrower material definition.

### Locked behavior

- Can be placed/used for sleeping.
- Successful sleep skips the night under the normal sleep flow.
- **Does not set or replace the player's respawn point.**
- No extra buff, fuel, durability system, or additional progression mechanic is attached to it.

---

## 6. Portable Furnace

### Role

A furnace opened directly from an item, using vanilla furnace interaction/presentation as the base rather than a new custom machine UI.

### Locked behavior

- Exists and is used as an **item**, not as a placed furnace block.
- Opens a furnace-style UI with the normal input / fuel / output structure.
- Smelting completes at **3x vanilla furnace speed**.
- Fuel is consumed at **3x the vanilla furnace rate**.
- Therefore the speed increase does not create a free fuel-efficiency bonus.
- Items whose normal maximum stack size is 64 are capped at **16 per Portable Furnace slot**.
- When transferring a larger stack, only the allowed amount enters the slot and the remainder stays with the cursor/source inventory as normal.
- **Stage 23-B amendment (2026-10-16):** closing the UI persists input, fuel, output, remaining burn time, and cooking progress on that Portable Furnace item. Reopening the same item restores those slots and timers.
- Portable Furnace contents follow the single-stack item when it is moved, dropped, or transferred after closing. The active opener item cannot be removed while its menu is open.
- This persistence amendment does not extend furnace research bonuses to Portable Furnaces. Existing item attribution metadata remains separate from the portable processing rules.

The Stage 23-B amendment supersedes the earlier close-and-drop behavior recorded in this section.

### Not yet fixed

- Recipe/material cost.
- Exact implementation details of the vanilla-furnace UI reuse.

---

## 7. Mining Drill

### Role

A very-high-speed **single-block pickaxe replacement** whose long-term cost is fuel rather than durability.

It is not a vein-mining or area-mining tool.

### Locked mining behavior

- No durability loss system.
- Mining speed is calibrated so that **Deepslate is effectively one-touch / near-instant** under the intended normal mining flow.
- The Drill mines one block at a time.
- A successfully mined block costs **1 Fuel Point (FP)**.
- **Silk Touch** and **Fortune** are supported.
- Vein/area mining is not part of the Drill; those mechanics remain separate from this item.

### Locked fuel behavior

The Drill uses a small furnace-like fuel interface.

- Fuel slot maximum: **64 items**.
- Accepted fuels:
  - **Coal:** 1 item = **8 FP**
  - **Block of Coal:** 1 item = **72 FP**
- A Coal Block therefore carries exactly the fuel value of 9 Coal.
- Fuel is consumed through the Drill's own fuel store; it does not automatically pull arbitrary Coal from the player's main inventory while mining.
- At 1 FP per successful block:
  - 64 Coal = 512 blocks
  - 64 Coal Blocks = 4,608 blocks

### Presentation

- Fuel/UI presentation should reuse vanilla furnace visual language where practical rather than inventing a separate art system.

### Not yet fixed

- Recipe/material cost.
- Exact internal tick/mining-speed constant used to realize the one-touch Deepslate target.

---

## 8. Backpack

### Role

A player-bound persistent secondary storage system, conceptually similar to a second Ender Chest rather than a collection of independent bag inventories.

### Locked storage behavior

- All Backpack items used by the same player access **one shared player-bound storage state**.
- Multiple Backpack items do not create multiple independent inventories.
- Stored contents are retained through player death and do not drop with ordinary inventory contents.
- Capacity depends on Backpack tier:
  - **Small Backpack:** 9 slots
  - **Backpack:** 27 slots
  - **Large Backpack:** 54 slots
- If a player later accesses the same shared storage with a lower-capacity Backpack, contents in currently inaccessible higher slots are preserved rather than deleted.

### Locked equipment interaction

- The Backpack attaches to **chest armor** through direct inventory interaction rather than an anvil-combination recipe.
- It can be detached again through the corresponding inventory interaction.
- Exact click/drag implementation may follow normal inventory semantics, but the result is attachment to the chest-armor equipment context rather than destruction/merging of either item.

### UI direction

The storage UI uses vanilla container grammar rather than a bespoke custom panel:

- 9 slots -> one-row container presentation
- 27 slots -> chest-sized presentation
- 54 slots -> double-chest-sized presentation

### Approved recipe progression direction

Exact counts and final structural-material assignments remain for the later material-economy pass, but the progression direction is retained:

- **Small Backpack:** Bundle as a key progression component plus leather/string-like structure.
- **Backpack:** upgrades from Small Backpack and includes an Ender Pearl plus a stronger iron/gold-like structural component.
- **Large Backpack:** upgrades from Backpack and includes a Shulker Shell plus a diamond-like structural component.

The terms above describe recipe progression/ingredient roles, not a final assignment of MoreMinecrafts minerals.

---

## 9. Material/economy boundary

- Do not invent mineral sinks merely to finish these recipes.
- Exact recipe quantities and unresolved structural-material assignments are determined after downstream equipment/content demand is aggregated.
- The fixed Sleeping Bag recipe is already an exception because its recipe was explicitly approved.
- The Backpack progression components above are approved direction; unspecified structural materials remain open.
- Utility design closure does not by itself finalize Titanium/Tungsten/Mythril/Etherium/Adamantium economy.

## 10. Implementation/resource boundary

- Vanilla-derived UI is preferred where explicitly stated for Backpack, Portable Furnace, and Mining Drill.
- Custom item models/textures remain resource work.
- The Miner Lantern and Potion Injector may use their separately approved/generated reference assets, but those visual assets do not alter the gameplay contracts in this document.

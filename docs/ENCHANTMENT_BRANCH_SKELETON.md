# Enchantment Branch Skeleton

Status: **CURRENT DESIGN SKELETON**

This document records the approved MoreMinecrafts enchantment direction after reviewing the legacy Paper enchantment catalogue.

## Design direction

The Paper enchantment catalogue was built while custom skills were being replaced by a vanilla-equipment + enchantment end state. Many legacy enchantments therefore behaved like active skills with dedicated key inputs.

That is **not** the target for MoreMinecrafts.

MoreMinecrafts enchantments should primarily extend normal vanilla actions and equipment behavior. Legacy Paper enchantments are idea/reference material only unless explicitly retained below. Removed concepts may remain as historical ideas, but they are not current implementation requirements.

Detailed recipes, acquisition, balance values, progression placement, resource dependencies, and unresolved cross-system behavior are outside this skeleton unless explicitly stated.

## Current retained roster

### Laser Arrow
- Bow enchantment.
- Replaces the normal projectile behavior with a laser projectile.
- Should be designed to coexist with ordinary compatible bow enchantments such as Power, Infinity, and Mending.
- Mutually exclusive with **Multi Shot** below.
- New MoreMinecrafts resources/presentation should be created rather than treating the Paper presentation as final.

### Multi Shot
- Bow enchantment.
- Uses vanilla Multishot-style behavior on bows.
- Level I fires **3 arrows**.
- Level II fires **5 arrows** and uses a **narrower spread** than Level I.
- Mutually exclusive with **Laser Arrow**.

### Crossbow Barrage
- Crossbow enchantment.
- One normal charge produces a **timed sequential 3-shot barrage**, not a simultaneous volley.
- The charge-speed behavior remains the same as the normal crossbow charging behavior; this enchantment does not add a separate faster charging rule.
- A full barrage consumes **3 ammunition items** and durability equivalent to **3 uses**.
- The exact interval between the three sequential shots is an implementation/live-tuning parameter.
- It extends normal crossbow operation; do not restore the old Paper dedicated-key implementation.

### Vein Mining
- **Pickaxe-only** mining enchantment.
- Mining one eligible ore block mines the connected vein.
- Connected-vein search is bounded to a maximum area of **4 chunks** for performance protection.
- Eligible ores include all vanilla ore/resource blocks such as diamond and other overworld ores, Nether Quartz Ore, Ancient Debris, and the ore/raw-resource families added by MoreMinecrafts.
- Level I charges durability for **every ore block actually mined** by the vein operation.
- Level II reduces the resulting vein-operation durability usage by **50%**.
- **Unbreaking does not apply to Vein Mining durability calculation.**
- This is connected-vein mining, not generic area excavation.

### Auto Replant
- Retained.
- **Hoe-only** enchantment.
- Applies to farmable crops planted on farmland, including ordinary seed crops, carrots, and potatoes; **Nether Wart is also included**. **Cocoa is excluded.**
- Immature crops are not harvested by the enchantment.
- When an eligible mature crop is harvested, replanting consumes **one real seed/planting item** from the harvested/available resources.
- If no valid planting item is available, the crop is simply harvested/destroyed and is **not replanted**.
- Farming is not a standalone MoreMinecrafts progression axis, but that does not remove this vanilla-action enchantment.

### Auto Smelt
- Retained.
- **Pickaxe-only** enchantment.
- Migrate the Paper passive concept directly: eligible mined ore yields its smelted result.
- Its eligible ore/resource coverage follows the same broad ore set as **Vein Mining**, including applicable MoreMinecrafts-added ore/raw-resource families and vanilla ores/resources such as Nether Quartz Ore and Ancient Debris.

### Lumberjack
- Retained.
- Axe enchantment that extends normal tree cutting into connected-tree felling.
- Activation is based on validating the **complete connected tree structure before chain-felling begins**. These structural limits are activation bounds, not partial-processing caps.
- The trunk may use a normal vanilla-style **1×1** footprint or a complete **2×2** footprint. The approved maximum trunk cross-section is **2×2**.
- The continuous trunk height must remain within the approved **3–34 block activation range**.
- Upper branch logs may extend laterally outside the trunk footprint. Branches do **not** enlarge the trunk cross-section envelope and must not cause an otherwise valid vanilla-style tree to be rejected merely because they extend beyond 2×2 horizontally.
- Structures with a trunk broader than 2×2, a trunk outside the 3–34 height range, broad/dense artificial log sheets, or additional trunk-like vertical columns are invalid for Lumberjack activation.
- If the complete connected structure is invalid, Lumberjack does **not** partially process the first valid-looking portion. Chain-felling does not activate and only the block normally mined by the player is broken.
- Leaves are **not** harvested.
- Unbreaking is not rolled independently for every destroyed log. Instead, calculate the total destroyed-log count and apply the corresponding Unbreaking durability-saving percentage arithmetically to determine the final durability cost.

### Multi Catch
- Retained concept, **Fishing integration unresolved**.
- Do not directly port the old Paper reward multiplication behavior.
- Final behavior must be designed against the current MoreMinecrafts Fishing pipeline, including fish instances, grade/species, length/weight, minigame outcome, equipment validation, consumable use, and XP/reward semantics.

### Elytra Launch
- Retained.
- Elytra enchantment providing self-launch/takeoff functionality.
- Activation input is **Shift + Jump**.
- If Elytra Launch and Feather Boots could both respond to the same Shift + Jump input, **Elytra Launch has priority** and Feather Boots does not also trigger.
- It is launch/takeoff functionality only; it does not restore Precision Flight or provide sustained propulsion.

### Explosive Mace
- Retained.
- Migrate the Paper concept onto the vanilla mace smash/impact action rather than a dedicated active-skill key.
- Use **one explosion/effect centered on the primary impact point**.
- Do not create a separate explosion centered on every affected mob.

### Feather Boots
- New/reworked boots enchantment replacing the old Rolling Landing direction.
- **Mutually exclusive with Jet Boots.**
- **Fall damage only is reduced by 100%.**
- This protection does not apply to elytra collision/kinetic damage, projectiles, tridents, explosions, entity attacks, or other non-fall damage sources.
- Also provides a **Shift + Jump** launch/jump behavior.
- Target movement is approximately **5 blocks horizontally** and **2 blocks vertically**; exact velocity constants are implementation/live-tuning parameters, not a new gameplay-design decision.

### Jet Boots
- New boots enchantment.
- **Mutually exclusive with Feather Boots.**
- Grants **air jumps**; it is not sustained flight and does not replace elytra propulsion.
- Level I: **1** air jump.
- Level II: **2** air jumps.
- Level III: **3** air jumps.
- Air-jump charges reset **only when the player touches the ground**.

## Removed from the current roster

The following legacy Paper concepts are **not current MoreMinecrafts enchantment requirements**:

- Blade Chain
- Light Greatsword
- Axe Heavy Strike
- Titan's Wrath
- Skill Protection
- Rolling Landing (superseded by Feather Boots)
- Wind Arrow
- Fire Arrow Rain
- Treasure Finder
- Precision Flight
- Area Excavation as a separate enchantment (vein behavior is represented by Vein Mining)

Other inactive/retired Paper custom enchantments and vanilla-copy serialization shims remain historical reference only and are not migration requirements.

Deletion here means removal from the current roster, **not erasure of the historical idea**.

## Related utility concept

Precision Flight is removed from the enchantment roster. The broader need for controllable elytra propulsion is moved to a **separate elytra propulsion Utility item concept**.

This document does not design that Utility item.

## Vanilla acquisition integration

All implemented custom enchantment books are intended to participate in the normal vanilla acquisition ecosystem, including appropriate **generated chest loot, fishing, villager trading, and other native enchanted-book/enchantment paths**.

Initial probability/weight values may be provisional implementation values. Final acquisition-rate balancing is a later tuning task; provisional weights do not make the integration itself incomplete.

Do not create a separate custom enchantment shop, currency, or parallel acquisition system.

## Open integration points

- **Laser Arrow** is intentionally separated into its own resource/VFX and implementation task.
- **Multi Catch** intentionally requires a dedicated cross-system design pass against the current MoreMinecrafts Fishing pipeline before implementation.

## Authority boundary

This is a branch-level design skeleton, not permission to implement unspecified details.

Do not infer:
- additional enchantment levels beyond those explicitly stated,
- final acquisition probability/weight balancing,
- compatibility not explicitly stated or naturally supplied by vanilla mechanics,
- resource/material costs,
- progression placement,
- Multi Catch's final Fishing behavior,
- Laser Arrow's implementation/resource details from this skeleton,
- elytra propulsion Utility behavior.

For the non-Laser, non-Multi-Catch retained roster, remaining low-level constants needed to realize the behavior above may be treated as implementation/live-tuning parameters when they do not change the approved gameplay model.

Missing details must be resolved by a later human-approved design task.

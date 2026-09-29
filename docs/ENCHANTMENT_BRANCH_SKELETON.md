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
- Fires multiple arrows in the style of vanilla crossbow Multishot, but for bows.
- Mutually exclusive with **Laser Arrow**.
- Exact projectile count/spread/balance remain TBD.

### Crossbow Barrage
- Crossbow enchantment.
- Retained concept: improves the weak vanilla crossbow through repeated/rapid firing behavior.
- Exact passive trigger, cadence, and balance remain TBD; do not restore the old Paper dedicated-key implementation by default.

### Vein Mining
- Mining enchantment.
- Mining one ore block mines the connected vein.
- Connected-vein search is bounded to a maximum area of **4 chunks** for performance protection.
- Exact eligible blocks, durability/tool-cost behavior, and edge cases remain TBD.

### Auto Replant
- Retained.
- Automatically replants eligible crops as part of normal farming interaction.
- Farming is not a standalone MoreMinecrafts progression axis, but that does not remove this vanilla-action enchantment.

### Auto Smelt
- Retained.
- Migrate the Paper passive concept directly: eligible mined ore yields its smelted result.
- MoreMinecrafts custom-material coverage and compatibility details may be connected later.

### Lumberjack
- Retained.
- Axe enchantment that extends normal tree cutting into connected-tree felling.
- Exact tree detection/performance bounds remain TBD.

### Multi Catch
- Retained concept, **Fishing integration unresolved**.
- Do not directly port the old Paper reward multiplication behavior.
- Final behavior must be designed against the current MoreMinecrafts Fishing pipeline, including fish instances, grade/species, length/weight, minigame outcome, equipment validation, consumable use, and XP/reward semantics.

### Elytra Launch
- Retained.
- Elytra enchantment providing self-launch/takeoff functionality.
- Exact vanilla-input interaction and launch parameters remain TBD.

### Explosive Mace
- Retained.
- Extends the vanilla mace smash/impact behavior with an explosion effect.
- It should remain tied to the vanilla mace action rather than a dedicated active-skill key.
- Exact explosion behavior and balance remain TBD.

### Feather Boots
- New/reworked boots enchantment replacing the old Rolling Landing direction.
- **Fall damage only is reduced by 100%.**
- This protection does not apply to elytra collision/kinetic damage, projectiles, tridents, explosions, entity attacks, or other non-fall damage sources.
- Also provides a Shift + Jump launch/jump behavior.
- Exact launch parameters remain TBD.

### Jet Boots
- New boots enchantment.
- Grants an **air jump** (double-jump style movement).
- It is not sustained flight and does not replace elytra propulsion.
- Exact air-jump count, reset conditions, compatibility, and movement values remain TBD.

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

## Open integration point

**Multi Catch** is the only retained enchantment in this skeleton that intentionally requires a dedicated cross-system design pass before its behavior can be considered finalized.

## Authority boundary

This is a branch-level design skeleton, not permission to implement unspecified details.

Do not infer:
- recipes or acquisition sources,
- enchantment rarity/levels unless stated,
- numeric balance,
- compatibility not explicitly stated,
- resource/material costs,
- progression placement,
- Multi Catch's final Fishing behavior,
- elytra propulsion Utility behavior.

Missing details must be resolved by a later human-approved design task.

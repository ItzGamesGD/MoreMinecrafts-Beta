# Adamantium equipment — approved design v1 (2026-10-18)

Status: **design approved; documentation only; not implemented or live-verified**.
Scope: Adamantium only. Preserve existing Mithril, T2 elemental equipment, research, options, enhancements and other established ore specifications. Do not substitute previously proposed titanium/tungsten upgrade branches.

## Upgrade chain and smithing template
- Diamond → Netherite → **Adamantium**. Separately, Diamond → Mithril (existing design; unchanged).
- Smithing upgrade: Netherite equipment + Adamantium upgrade material + Adamantium smithing template → Adamantium equipment.
- Existing compatible enchantments, options, enhancements and other equipment data must survive upgrading; audit actual item/data component APIs before implementing.
- Caravan's reserved '형판' category now includes Mithril and Adamantium upgrade templates; do not assume other new template SKUs.

## Ore generation and refining
- Dimension: Nether.
- Adamantium ore is slightly rarer than ancient debris. **v1 proposed operational target accepted by user:** approximately 80% of ancient debris generation rate, with actual obtained density to be checked in worldgen tests.
- Height: Y=8..22, biased toward Y≈15.
- Vein size: 1–2 ore blocks.
- Mining: diamond pickaxe or better.
- Smelting: 1 Adamantium ore → 1 Adamantium refined material ('아다만티움 재련물'). Exact registry naming and furnace integration subject to implementation audit.
- Upgrade material recipe: **4 Adamantium refined materials + 2 Netherite ingots → 1 Adamantium upgrade material**. Choose crafting layout and registry identifiers during implementation without altering quantities.

## Equipment statistics
- All Adamantium equipment: **no durability loss under ordinary usage**, not merely a very high max durability or a minimum durability floor. No XP repair needed. Confirm durability hooks for attack, mining, armor damage and special modded interactions.
- Sword: **10 attack damage**, **1.6 attack speed** (Netherite reference: 8 damage / 1.6 speed).
- Pickaxe: **11 mining speed** (Netherite reference: 9).
- Armor defense: same as Netherite; **armor toughness 4 per piece**; **knockback resistance 15% per piece**.
- Preserve existing enhancement, enchantment and option data on upgrade.
- Other tool and armor item stats not expressly specified here should follow the established Netherite baseline until separately approved; do not silently generalize sword attack damage or pickaxe mining speed to all items.

## Template acquisition and duplication
- Bastion Remnant **treasure room chest**: **8% chance per chest** for an Adamantium smithing template. Scope specifically treasure room loot; verify target loot table before implementing.
- **Top-tier rare caravan**: **20% chance** to list Adamantium template; trade price **8 emerald blocks**; unlimited stock once offered, consistent with caravan policy.
- Template duplication: **1 Adamantium template + 7 diamonds + 1 netherrack → 2 Adamantium templates** (net +1). Recipe shape can mirror vanilla duplication where compatible.
- Do not grant templates from all caravan tiers or invent a guaranteed listing.

## Rare caravan compatibility / other approved decisions
- User approved exactly three implicit, equally weighted caravan merchant tiers: common / advanced / top-tier (1/3 each).
- Only top-tier sells over-cap runes: **exactly 4 distinct runes**, chosen from 24 approved rune SKUs. Per-rune equal weights were suggested but not expressly finalized; do not encode as approved without confirmation.
- Last proposed caravan replacement rate **60%** was an assistant recommendation rather than explicit user approval; the prior explicitly approved 1:1 (50%) vanilla:caravan ratio should remain authoritative until user explicitly changes it.
- Three approved exclusive merchandise groups: exploration maps (future structure/dungeon linkage), bounty contracts (future commission/bounty system linkage), special enhanced equipment (design alongside T2 equipment). Do not implement unapproved detailed variants.
- Over-cap rune price proposal (24 SKUs) was provisionally accepted by user, but requires documentation and implementation review elsewhere.

## Verification required before code
Audit actual Minecraft 26.3 Fabric mappings, existing MM item registrations and equipment pipelines, worldgen/loot table behavior, smithing recipe, durability prevention across all hooks, enchant/option/enhancement persistence, merchant offers and save/reload persistence. No code or tests are claimed in this document.

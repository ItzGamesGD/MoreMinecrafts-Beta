# MoreMinecrafts — Knowledge / Research / Quest Design Snapshot (2026-10-08)

> Design-only backup. Research taxonomy and tier hierarchy are LOCKED; individual research rewards, level caps, values and dependencies are PROVISIONAL. This snapshot is separate from the mod/modpack inspiration-extraction workflow. Do not modify implementation or main branch.

## Locked: system principles
- Knowledge is persistent evidence/understanding earned from gameplay; it determines eligibility to research.
- Research consumes materials + Minecraft XP; rewards include passive capabilities, deterministic workflow improvements, and newly unlocked crafting recipes.
- Reverse-design knowledge prerequisites **after** defining valuable research outcomes.
- Do not lock existing vanilla recipes/features behind research; new improved recipes/devices may require it.
- Quests supply XP through one-time activities/challenges; no daily reroll/shop system decided.
- Keep compatible with existing T1 enchant skill trees, elemental weapons, custom fishing, portable furnace, mining drill, fish trap, and 40 custom mobs. Avoid duplicate capabilities and unchecked enchant stacking.

## Locked: seven research disciplines
1. Resource Studies (자원학): mining, lumbering, gathering.
2. Production Engineering (생산공학): crafting, smelting, processing.
3. Living Technologies (생활기술학): agriculture, livestock, fishing, daily-life automation; includes applicable machinery.
4. Combat Studies (전투학): weapon operation, defense, combat technique.
5. Ecology (생태학): creature traits, weaknesses, species-specific responses.
6. World Studies (세계학): exploration, trade, archaeology, mobility.
7. Alchemy & Magic (연금·마법학): brewing, magic, elemental applications.
Machinery/redstone topics may be allocated by the actual improvement to Production Engineering or Living Technologies; do not invent an eighth discipline without user approval.

## Locked: research tier hierarchy (separate from equipment T1–T4)
- T1 **basic ability**: small, often passive/probabilistic benefits during existing gameplay, e.g. a chance of extra ore drop. Not necessarily random.
- T2 **operational efficiency**: tangible, more reliable or deterministic intervention in how an action/tool operates; not merely larger T1 numbers. Example: harvest and replant in one operation.
- T3 **applied recipe**: unlock an optional new item/block/device/recipe with high situational value, e.g. 3x3 mining equipment or a 5x furnace. T3 is not necessarily a universally stronger passive.
- T3 unifies the earlier separate 'new recipe' and 'new device' reward categories.
- Branching skill-tree topology analogous to enchantment trees. One research may fork to several others, and multiple paths can connect; a rigid three-node ladder is not required.
- **Research tier vs level are separate.** A single research can progress T1-1, T1-2, ... T1-N (and where meaningful T2-1 ... T2-M), adding incremental numeric effects. **Maximum level is set per research**, not globally or per discipline. T3 generally single unlock, with distinct recipes as branches rather than arbitrary numerical leveling; this is a design recommendation, not an absolute ban.
- Each level is a research purchase using materials + XP. Precise costs, caps, numerical values, unlock prerequisites and whether previous research must be purchased remain open. Prefer knowledge-gated independent unlocks except justified prerequisites (recommendation, not locked).
- Examples such as 2% per level / max 5, +damage, 5x furnace speed, and 3x3 mining are illustrative candidates, not locked balance.

## Provisional content pool (88 ideas, not 88 approved implementations)
Resource R01–R08: ore vein analysis; improved wood conversion; resource sifting; precision gathering; geological guide; wide excavation; mineral refining; special block recovery.
Production R09–R20: 5x furnace; parallel smelter; advanced logistics; fuel-efficient furnace; stonecutting; woodworking; bulk crafting; advanced autocrafting; equipment servicing; fuel processing; redstone signal control; building material processing.
Living R21–R34: crop breeding; husbandry; fishing technique; soil enrichment; irrigation; beekeeping; mushroom cultivation; feed; automated breeding; aquaculture; fish trap improvement; food processing; food preservation; daily-life automation.
Combat R35–R46: sword technique; shield technique; axe combat; archery; crossbow; throwing weapons; mobile combat; underwater combat; siege; special weapons; ammunition; armor operation.
Ecology R47–R59: undead analysis; illager analysis; arthropods; explosive creatures; nether mobs; end mobs; aquatic hostiles; slime; loot analysis; behavior guide; rare mobs; bosses; mob-material crafting.
World R60–R75: terrain survey; cave exploration; sailing; riding; flight; dimensional exploration; archaeology; ruins; maps; survival; transport/storage; villager trade; piglin barter; village systems; trial chambers; dimensional ruins.
Alchemy R76–R88: advanced brewing; catalysts; concentrated potions; splash; lingering; status effects; magical materials; XP application; soul materials; elemental analysis; elemental synthesis; magic devices; enchantment support.

## Example tree sketches (provisional)
- Resource > ore: T1 bonus ore chance (individually levelable) -> T2 direct mining workflow change -> T3 3x3 excavation recipe (watch existing drill).
- Living > crops: T1 bonus harvest chance -> T2 harvest-and-replant -> T3 irrigation device.
- Living > fishing: T1 catch-related bonus -> T2 meaningful mini-game operational improvement -> T3 aquaculture recipe (protect existing fishing balance).
- Production > smelting: T1 chance to save fuel -> T2 improve how furnace fuel cycles are used -> T3 5x furnace / fuel-saving / parallel furnace recipes (different tradeoffs).
- Combat > weapon: T1 small conditional advantage -> T2 different weapon operation -> T3 specialized equipment recipe only if meaningful.
- Ecology > undead: species knowledge -> levelable undead damage bonus; account for Smite and overlapping mob taxonomies.

## Outstanding decisions / audit flags
- Convert the 88 brainstorm entries into real tiered, branching research; prune redundant or vague entries, do not force every idea into a tree.
- Distinguish T1 probabilistic baseline from T2 operational mechanics with concrete examples; avoid plain 5% -> 10% disguised as tiers.
- Assign research-specific level caps and XP/material costs later; calibrate quest XP supply.
- Design knowledge prerequisites last, using real gameplay achievements/discoveries.
- Decide multiplayer ownership and sharing of recipe unlocks and devices.
- Avoid vanilla obsolescence and clashes with Fortune/Efficiency/Smite, enchant skill tree, Crafter, portable furnace (3x throughput and 3x fuel), Mining Drill, Fish Trap.
- Audit underrepresented vanilla activities: building/decor, cooking, villagers, beacon, dimensions, archaeology, trial chambers, etc.
- No implementation changes authorized by this snapshot.

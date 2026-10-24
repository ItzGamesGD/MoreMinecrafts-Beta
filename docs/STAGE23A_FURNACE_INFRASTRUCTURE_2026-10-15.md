# Stage 23-A Furnace Research Infrastructure

This change adds attribution and save-state infrastructure for `v3.research.06` Fuel Saving,
`.07` Precision Smelting, and `.11` Faster Smelting. It does not activate any of those effects or
change research purchases.

## Persistence and authority

Permanent furnace, blast furnace, and smoker block entities carry a versioned state with the
attributed player UUID and two normalized fractional remainders. Existing saves without the new
versioned compound load as empty state. The block entity owns the fields, so normal chunk save,
unload, reload, server restart, and break/replacement behavior apply.

The online research source remains `KnowledgeRuntime.STATE`, the existing persistent
`ResearchProgressState` attachment. `ResearchProgressLookup` reads online state from the live
player. For an offline UUID, it synchronously calls Minecraft's existing `PlayerList.loadPlayerData`
on the server thread and decodes only `fabric:attachments` / `moreminecrafts:research_progress_v4`
with the attachment's existing codec. It does not create a synthetic `ServerPlayer` or a second
research store. Missing or malformed player data resolves to no state and research level zero.

Portable furnace attribution and fractional fields use a persistent data component on its existing
non-stackable opener item. Only those metadata fields persist; the three-slot session container,
fuel and cooking rates, and close-time item drops remain session-only and unchanged.

## Compatibility inventory

| Furnace family | Block/entity architecture | User interaction | Fuel path | Cooking path | Output path | Persistence | Integration risks |
|---|---|---|---|---|---|---|---|
| Vanilla furnace | Vanilla `FurnaceBlock` + `FurnaceBlockEntity`, which extends `AbstractFurnaceBlockEntity` | Vanilla block opens `FurnaceMenu`; block entity receives `startOpen`; server menu clicks commit inventory changes | Shared `AbstractFurnaceBlockEntity.serverTick`/`burn` | Shared recipe lookup and integer cook counters in `serverTick` | Shared `burn` into slot 2 | Vanilla block entity NBT; this change adds versioned `MoreMinecraftsFurnaceResearchState` | Shared hooks affect all three vanilla furnace variants; preserve recipe checks, fuel remainders, and result conservation when effects are added later |
| Blast furnace | Vanilla `BlastFurnaceBlock` + `BlastFurnaceBlockEntity`, same abstract furnace entity | Same `FurnaceMenu` path | Same shared path, with blast-furnace recipe/fuel predicates | Same shared counters; blast-furnace recipe type remains vanilla-controlled | Same slot 2 output path | Same vanilla block entity persistence and new state compound | Future effect eligibility must use recipe context; do not infer eligibility from output pickup |
| Smoker | Vanilla `SmokerBlock` + `SmokerBlockEntity`, same abstract furnace entity | Same `FurnaceMenu` path | Same shared path, with smoker recipe/fuel predicates | Same shared counters; smoker recipe type remains vanilla-controlled | Same slot 2 output path | Same vanilla block entity persistence and new state compound | Food recipes need explicit exclusion from Precision Smelting |
| Portable furnace | `PortableFurnaceItem` opens `PortableFurnaceMenu`; three-slot `PortableContainer`, no world block or block entity | Item `use` opens a fresh session; menu clicks are observed server-side | `PortableFurnaceMenu.consumeFuel` resolves vanilla fuel then applies the existing 3x fuel tick factor | `PortableFurnaceMenu.serverTick` uses its session-local integer `SimpleContainerData` cook count and existing 67 tick baseline | Recipe assembly writes directly to the session output slot | Existing contents and counters are not serialized; new attribution/remainders persist on the opener item only | Do not persist the session inventory or alter `removed` close/drop handling; the item may move while its menu is open, so server mutation must remain tied to the server-owned stack reference |
| Multiplier furnaces | None registered in this repository; no custom block/entity, processing hook, or multiplier-furnace recipe set exists | Not applicable | Not applicable | Not applicable | Not applicable | Not applicable | The V3 authority's future 3x/5x/7x furnaces remain a compatibility obligation if introduced; this change does not create them or claim support |

## Attribution events

Opening a GUI records that server player's UUID. A committed server menu click records a new UUID
only when the furnace input, fuel, or output inventory actually changes. The latest open or actual
change wins; closing does not clear it. Hopper changes bypass the menu-click observer and retain the
current UUID. With no UUID, future effects have no player to resolve.

No proximity, placement owner, output collector, client research level, or highest-level player is
consulted. Research effects remain inactive in this infrastructure change.

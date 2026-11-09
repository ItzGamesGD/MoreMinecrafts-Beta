# BC-01 Exploration Catalog

Static evidence: `src/main/resources/data/moreminecrafts/worldgen/` contains ore features only; no MM `structure`, `structure_set`, or biome definition JSON. `ModOreGeneration.java` adds ores, not structures. Therefore **0 verified MM structures and 0 MM biomes** are implementation-ready. Meteor and new-structure special targets are unavailable.

| Target | Dimension | Generation / survival | Server entry detection | Pool status |
| --- | --- | --- | --- | --- |
| `minecraft:village_plains` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:desert_pyramid` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:jungle_temple` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:mineshaft` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:stronghold` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:ocean_monument` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:woodland_mansion` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:trial_chambers` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:ancient_city` | Overworld | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:fortress` | Nether | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:bastion_remnant` | Nether | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |
| `minecraft:end_city` | End | vanilla generated; survival accessible with progression | structure piece/start lookup feasible; entrance semantics OPEN | candidate only |

Vanilla structure IDs above need runtime registry cross-check before implementation (notably village variants and fortress ID). Vanilla biome candidates: `minecraft:plains`, `minecraft:forest`, `minecraft:desert`, `minecraft:swamp`, `minecraft:snowy_plains`, `minecraft:nether_wastes`, `minecraft:crimson_forest`, `minecraft:warped_forest`, `minecraft:the_end`, `minecraft:end_highlands`. Biome-key lookup on server is feasible, but visit radius/dwell, repeated visits, and mixed dimension bundles need owner rules. Generation depends on world seed and settings; discovery time is UNKNOWN. Stronghold and End City require progression; ocean monument and trial chambers have encounter hazards. No new MM structure or event objective is ready.

# BC-01 Mob Catalog

Baseline: PR #148 `b7cedf199a5f1eda341ce3bd27eabb22eeb309e1`. Candidate classifications below are **PROPOSED**, not approved objective pools.

**Verified static count: 40 registered custom `EntityType`s and 40 spawn specifications.** The historical 40-mob roster reconciles 1:1 to these IDs. No extra Mire Shaman or Shard Golem registry entries exist. Evidence: `CustomMobCatalog.java:69-118,158-213`, `ModRegistries.java:22`; historical names: `docs/VANILLA_MODEL_CUSTOM_MOB_ROSTER.md`. Static source proves registration and spawn wiring, not observed survival spawn rates.

| ID | Display name | Spawn route / dimension | Access and evidence | Candidate difficulty / eligibility |
| --- | --- | --- | --- | --- |
| `moreminecrafts:swamp_zombie` | Swamp Zombie | natural; Overworld; `SWAMP_HABITAT`, `GROUND`, weight 4 | survival plausible; biome/position gate; registration L69, spawn L159 | ordinary (PROPOSED) |
| `moreminecrafts:cave_zombie` | Cave Zombie | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 3 | survival plausible; biome/position gate; registration L70, spawn L160 | ordinary (PROPOSED) |
| `moreminecrafts:large_zombie` | Large Zombie | natural; Overworld; `IS_OVERWORLD`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L71, spawn L161 | strong (PROPOSED) |
| `moreminecrafts:frozen_zombie` | Frozen Zombie | natural; Overworld; `SNOWY_HABITAT`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L72, spawn L162 | ordinary (PROPOSED) |
| `moreminecrafts:mossy_zombie` | Mossy Zombie | natural; Overworld; `IS_FOREST`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L73, spawn L163 | ordinary (PROPOSED) |
| `moreminecrafts:fungal_zombie` | Fungal Zombie | natural; Overworld; `FUNGAL_HABITAT`, `CAVE`, weight 2 | survival plausible; biome/position gate; registration L74, spawn L164 | ordinary (PROPOSED) |
| `moreminecrafts:miner_zombie` | Miner Zombie | natural, structure gated; Overworld; `HAS_MINESHAFT`, `CAVE`, weight 2 | survival plausible; structure gate; registration L75, spawn L165 | ordinary (PROPOSED) |
| `moreminecrafts:rotten_skeleton` | Rotten Skeleton | natural; Overworld; `IS_OVERWORLD`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L76, spawn L166 | ordinary (PROPOSED) |
| `moreminecrafts:cave_skeleton` | Cave Skeleton | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 3 | survival plausible; biome/position gate; registration L77, spawn L167 | ordinary (PROPOSED) |
| `moreminecrafts:armored_skeleton` | Armored Skeleton | natural; Overworld; `IS_MOUNTAIN`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L78, spawn L168 | strong (PROPOSED) |
| `moreminecrafts:ashen_skeleton` | Ashen Skeleton | natural; Nether; `IS_NETHER`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L79, spawn L169 | ordinary (PROPOSED) |
| `moreminecrafts:lesser_wither_skeleton` | Lesser Wither Skeleton | natural, structure gated; Nether; `HAS_NETHER_FORTRESS`, `NETHER`, weight 2 | survival plausible; structure gate; registration L80, spawn L170 | strong (PROPOSED) |
| `moreminecrafts:mountain_skeleton` | Mountain Skeleton | natural; Overworld; `IS_MOUNTAIN`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L81, spawn L171 | ordinary (PROPOSED) |
| `moreminecrafts:miner_skeleton` | Miner Skeleton | natural, structure gated; Overworld; `HAS_MINESHAFT`, `CAVE`, weight 2 | survival plausible; structure gate; registration L82, spawn L172 | ordinary (PROPOSED) |
| `moreminecrafts:small_spider` | Small Spider | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 2 | survival plausible; biome/position gate; registration L83, spawn L173 | ordinary (PROPOSED) |
| `moreminecrafts:large_spider` | Large Spider | natural; Overworld; `IS_OVERWORLD`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L84, spawn L174 | strong (PROPOSED) |
| `moreminecrafts:forest_spider` | Forest Spider | natural; Overworld; `IS_FOREST`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L85, spawn L175 | ordinary (PROPOSED) |
| `moreminecrafts:desert_spider` | Desert Spider | natural; Overworld; `DESERT_HABITAT`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L86, spawn L176 | ordinary (PROPOSED) |
| `moreminecrafts:swamp_spider` | Swamp Spider | natural; Overworld; `SWAMP_HABITAT`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L87, spawn L177 | strong (PROPOSED) |
| `moreminecrafts:ice_spider` | Ice Spider | natural; Overworld; `SNOWY_HABITAT`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L88, spawn L178 | ordinary (PROPOSED) |
| `moreminecrafts:cave_bug` | Cave Bug | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 3 | survival plausible; biome/position gate; registration L89, spawn L179 | ordinary (PROPOSED) |
| `moreminecrafts:desert_bug` | Desert Bug | natural, structure gated; Overworld; `HAS_DESERT_PYRAMID`, `CAVE`, weight 2 | survival plausible; structure gate; registration L90, spawn L180 | ordinary (PROPOSED) |
| `moreminecrafts:end_bug` | End Bug | natural; End; `IS_END`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L91, spawn L181 | ordinary (PROPOSED) |
| `moreminecrafts:wild_slime` | Wild Slime | natural; Overworld; `WILD_SLIME_HABITAT`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L92, spawn L182 | ordinary (PROPOSED) |
| `moreminecrafts:mud_slime` | Mud Slime | natural; Overworld; `SWAMP_HABITAT`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L95, spawn L183 | ordinary (PROPOSED) |
| `moreminecrafts:sand_slime` | Sand Slime | natural; Overworld; `DESERT_HABITAT`, `GROUND`, weight 3 | survival plausible; biome/position gate; registration L98, spawn L184 | ordinary (PROPOSED) |
| `moreminecrafts:lesser_magma_cube` | Lesser Magma Cube | natural; Nether; `IS_NETHER`, `NETHER`, weight 2 | survival plausible; biome/position gate; registration L101, spawn L185 | strong (PROPOSED) |
| `moreminecrafts:drowned_sailor` | Drowned Sailor | natural, structure gated; Overworld; `HAS_SHIPWRECK`, `WATER`, weight 1 | survival plausible; structure gate; registration L103, spawn L186 | ordinary (PROPOSED) |
| `moreminecrafts:deep_sea_drowned` | Deep-Sea Drowned | natural; Overworld; `IS_DEEP_OCEAN`, `WATER`, weight 3 | survival plausible; biome/position gate; registration L104, spawn L187 | strong (PROPOSED) |
| `moreminecrafts:river_drowned` | River Drowned | natural; Overworld; `IS_RIVER`, `WATER`, weight 2 | survival plausible; biome/position gate; registration L105, spawn L188 | ordinary (PROPOSED) |
| `moreminecrafts:wild_piglin` | Wild Piglin | natural; Nether; `IS_NETHER`, `NETHER`, weight 2 | survival plausible; biome/position gate; registration L106, spawn L189 | ordinary (PROPOSED) |
| `moreminecrafts:bandit` | Bandit | natural; Overworld; `IS_FOREST`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L107, spawn L190 | ordinary (PROPOSED) |
| `moreminecrafts:bandit_archer` | Bandit Archer | natural; Overworld; `IS_FOREST`, `GROUND`, weight 2 | survival plausible; biome/position gate; registration L108, spawn L191 | ordinary (PROPOSED) |
| `moreminecrafts:bandit_warrior` | Bandit Warrior | natural; Overworld; `IS_FOREST`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L109, spawn L192 | strong (PROPOSED) |
| `moreminecrafts:wandering_raider` | Wandering Raider | natural; Overworld; `IS_OVERWORLD`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L110, spawn L193 | ordinary (PROPOSED) |
| `moreminecrafts:feral_wolf` | Feral Wolf | natural; Overworld; `IS_FOREST`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L111, spawn L194 | ordinary; tame-state filter needed (PROPOSED) |
| `moreminecrafts:cave_bat` | Cave Bat | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 2 | survival plausible; biome/position gate; registration L113, spawn L195 | ordinary (PROPOSED) |
| `moreminecrafts:giant_bee` | Giant Bee | natural; Overworld; `IS_FOREST`, `GROUND`, weight 1 | survival plausible; biome/position gate; registration L115, spawn L196 | excluded pending hostile-target decision (PROPOSED) |
| `moreminecrafts:lesser_phantom` | Lesser Phantom | natural; Overworld; `IS_OVERWORLD`, `SKY`, weight 1 | survival plausible; biome/position gate; registration L116, spawn L197 | ordinary (PROPOSED) |
| `moreminecrafts:cave_creeper` | Cave Creeper | natural; Overworld; `IS_OVERWORLD`, `CAVE`, weight 2 | survival plausible; biome/position gate; registration L118, spawn L198 | strong (PROPOSED) |

## Limits and owner decisions

No registered mob is verified as event-only or summon-only in this 40-entry catalog. Structure gates use generated vanilla pieces; actual spawn yield is UNKNOWN and needs a survival server check. `giant_bee` is a neutral animal; decide whether combat contracts may target it. Decide handling of tameable `feral_wolf`, peaceful mode, split slimes, and biome-specific accessibility. Mob effort weights and tier placement remain owner decisions. All 40 are candidate classifications only.

## Owner-approved classification overlay (2026-10-23)

The PROPOSED labels above are superseded by the approved combat roster in `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` (BC-03 combat roster). All 40 `moreminecrafts` entities are accounted for exactly once:

| Classification | Count | IDs |
| --- | ---: | --- |
| Strong (approved) | 9 | `large_zombie`, `armored_skeleton`, `lesser_wither_skeleton`, `large_spider`, `swamp_spider`, `lesser_magma_cube`, `deep_sea_drowned`, `bandit_warrior`, `cave_creeper` |
| Excluded (not hostile combat targets) | 3 | `feral_wolf`, `cave_bat`, `giant_bee` |
| Withheld pending live aggression verification | 1 | `wild_piglin` |
| Ordinary, Beginner or higher | 25 | every other ordinary entry except those below |
| Ordinary, Intermediate or higher | 1 | `lesser_phantom` |
| Ordinary, Advanced only (End) | 1 | `end_bug` |

Vanilla special-only (not generated): `minecraft:elder_guardian`, `minecraft:warden`, `minecraft:ghast`. Vanilla Intermediate or higher: `guardian`, `breeze`, `silverfish`, `phantom`, `vindicator`. Vanilla Advanced only: `shulker`. Strong vanilla targets currently available: `hoglin` only. No strong target exists in the End, so an Advanced End contract whose combat slot draws strong is regenerated.

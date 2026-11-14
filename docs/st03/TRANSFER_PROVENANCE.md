# ST-03 Pillager Headquarters — structure transfer (provenance)

Transferred on branch `work/st03-transfer-20261114` (from Beta `b2c9257`). **Not merged to `main`.**

## What this is

- Source: `ItzGamesGD/MoreMinecrafts-Structure-Generator` `main` @ `7bd65cef95084f01f229d3365ecfe79a91e40a58` (read-only clone).
- Adopted by owner decisions on 2026-11-14: **D-1** wall F for the perimeter (`MW86` / `MW31`), **D-9** F06 grass-to-dirt fix.
- Evidence: `docs/st03/evidence/integration-d1-d9/` (copied from `experiments/st03-integration-d1-d9/`, README and results as recorded there on 2026-11-14; not re-run in Beta).
- Placement authority copy: `docs/st03/data/st03_placements.json` (25 modules, 34 placements; from `tools/seam01/data/st03_placements.json`).
- Structure templates: `src/main/resources/data/moreminecrafts/structure/pillager_headquarters/*.nbt` (25 files, byte-identical copies of the adopted sources).

## What this is NOT

- **No worldgen.** No `structure_set`, `template_pool`, placement or generation hook is added. Natural worldgen is UNTESTED (per the source README). Planned to begin after this transfer.
- **No gameplay.** Garrison spawns, loot tables and rewards are not implemented. Reward quantities and premium item lists remain OPEN in the ledger.
- **Authority not confirmed.** `docs/CONTENT_AUTHORITY_INDEX.md` still lists the Q01–Q42 ledger's authority status as "owner to confirm". This transfer does not change that.
- The batch-8 candidate walls and wall A/B/C/B2 prototypes are not transferred (history only in the source).

## Adopted files and hashes

| module | file (Beta) | size (B) | sha256 | source (Structure-Generator) |
|---|---|---:|---|---|
| MT | `st03_tower.nbt` | 12350 | `990bdd2336b603f8…` | `experiments/st03-batch9-towers-gate/out/st03_tower.nbt` |
| MW86 | `st03_wall_86.nbt` | 12938 | `dcad38a64c4eafa3…` | `experiments/st03-wall-redesign/out/f/st03_wall_86.nbt` |
| MW31 | `st03_wall_31.nbt` | 5148 | `6d4172a144ec1843…` | `experiments/st03-wall-redesign/out/f/st03_wall_31.nbt` |
| MG | `st03_gatehouse.nbt` | 17205 | `da9750f241e59167…` | `experiments/st03-batch9-towers-gate/out/st03_gatehouse.nbt` |
| MQ-NW | `st03_ground_nw.nbt` | 59331 | `96d2d26601239bd1…` | `experiments/st03-batch10-ground-roads/out/st03_ground_nw.nbt` |
| MQ-NE | `st03_ground_ne.nbt` | 58810 | `40e20d59af27b4c6…` | `experiments/st03-batch10-ground-roads/out/st03_ground_ne.nbt` |
| MQ-SW | `st03_ground_sw.nbt` | 59524 | `70185e7eed178e96…` | `experiments/st03-batch10-ground-roads/out/st03_ground_sw.nbt` |
| MQ-SE | `st03_ground_se.nbt` | 59279 | `92ed5615105317b8…` | `experiments/st03-batch10-ground-roads/out/st03_ground_se.nbt` |
| M-manor | `st03_manor.nbt` | 21947 | `f0e21133f6baab46…` | `experiments/st03-batch7-commander-manor/out/st03_manor.nbt` |
| M-barracks_a | `st03_barracks_a.nbt` | 6516 | `3702b103ff9a19fb…` | `experiments/st03-batch4-barracks/out/st03_barracks_a.nbt` |
| M-barracks_b | `st03_barracks_b.nbt` | 6478 | `0faa0ea8be5dd19b…` | `experiments/st03-batch4-barracks/out/st03_barracks_b.nbt` |
| M-barracks_c | `st03_barracks_c.nbt` | 6379 | `536352915e6581b9…` | `experiments/st03-batch4-barracks/out/st03_barracks_c.nbt` |
| M-armory | `st03_armory.nbt` | 9349 | `b597abcc9bfd339e…` | `experiments/st03-batch3-military/out/st03_armory.nbt` |
| M-yard | `st03_yard.nbt` | 8882 | `17e725dde1c621c3…` | `experiments/st03-batch3-military/out/st03_yard.nbt` |
| M-warehouse_a | `st03_warehouse_a.nbt` | 8246 | `8bb4ea0061284596…` | `experiments/st03-batch5-warehouses/out/st03_warehouse_a.nbt` |
| M-warehouse_b | `st03_warehouse_b.nbt` | 7707 | `503151fff5beb5de…` | `experiments/st03-batch5-warehouses/out/st03_warehouse_b.nbt` |
| M-prison | `st03_prison.nbt` | 4996 | `e3b9eb9f18add02f…` | `experiments/st03-batch6-prison-stable/out/st03_prison.nbt` |
| M-stable | `st03_stable.nbt` | 9278 | `14255f69671efa17…` | `experiments/st03-batch6-prison-stable/out/st03_stable.nbt` |
| M-house_a | `st03_house_a.nbt` | 2741 | `e27317c3864840f9…` | `experiments/st03-b5a-house-a/st03_house_a.nbt` |
| M-house_b | `st03_house_b.nbt` | 2727 | `ef2dd204f1736233…` | `experiments/st03-batch2-houses/out/st03_house_b.nbt` |
| M-house_c | `st03_house_c.nbt` | 2289 | `ca3f18be62206164…` | `experiments/st03-batch2-houses/out/st03_house_c.nbt` |
| M-house_d | `st03_house_d.nbt` | 2245 | `dd40b688144a642f…` | `experiments/st03-batch2-houses/out/st03_house_d.nbt` |
| M-kitchen | `st03_kitchen.nbt` | 3394 | `c97b622d4b0d7cee…` | `experiments/st03-batch1-support/out/st03_kitchen.nbt` |
| M-smithy | `st03_smithy.nbt` | 3353 | `5f19af5ecc64bbe3…` | `experiments/st03-batch1-support/out/st03_smithy.nbt` |
| M-cart_shed | `st03_cart_shed.nbt` | 2068 | `076d14251dabf904…` | `experiments/st03-batch1-support/out/st03_cart_shed.nbt` |

## Checks performed in Beta for this transfer

- Each copied file is byte-identical to its source (sha256 above).
- All 25 module files referenced by the 34 placements exist.
- Each `.nbt` is gzip-compressed and starts with a root compound tag.
- `./gradlew processResources`: BUILD SUCCESSFUL (2026-11-14).
- NOT RUN: full `./gradlew build` and GameTests (not required for resource-only transfer; not executed here).

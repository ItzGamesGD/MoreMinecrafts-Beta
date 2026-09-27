# Approved master asset and runtime presentation contract

Approved PNGs are immutable source assets: preserve bytes, dimensions, mode, alpha, padding, and source identity. Approval of art does not itself approve a geometry strategy. Resource D hashes are in [the manifest](FISHING_RESOURCE_D_APPROVED_MANIFEST.json) and checked by `validateFishingResources`.

| Family | Master | Runtime model | Constraints |
|---|---|---|---|
| Fish | Approved 512×512 RGBA PNG | Explicit native item plane | Two faces; 2 quads; source aspect retained; no alpha-derived extrusion |
| Reel / Minnow / Crankbait | Approved high-resolution RGBA PNG | Bounded flat-item presentation | Two faces; source aspect retained; explicit vanilla transforms |
| Flame Sword | Approved immutable PNG | Existing C1-A/C1-B `minecraft:item/handheld` | Preserve existing two-face model and validator |

Master PNGs remain unchanged by model edits. Fishing uses one texture per item across GUI, hand, ground, and fixed contexts.

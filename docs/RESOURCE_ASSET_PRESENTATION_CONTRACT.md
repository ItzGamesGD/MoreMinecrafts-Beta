# Approved master asset and runtime presentation contract

Approved PNGs are immutable source assets: preserve bytes, dimensions, mode, alpha, padding, and source identity. Approval of art does not itself approve a geometry strategy. Resource D hashes are in [the manifest](FISHING_RESOURCE_D_APPROVED_MANIFEST.json) and checked by `validateFishingResources`.

| Family | Master | Runtime model | Constraints |
|---|---|---|---|
| Fish | Approved 512×512 RGBA PNG | Explicit native item plane | Two faces; 2 quads; source aspect retained; no alpha-derived extrusion |
| Reel / Minnow / Crankbait | Approved high-resolution RGBA PNG | Bounded flat-item presentation | Two faces; source aspect retained; explicit vanilla transforms |
| Flame Sword | Approved immutable PNG | Bounded solid handheld model | Six connected faces forming a thin solid profile; approved front/back artwork preserved; bounded side geometry; explicit vanilla-aligned hand transforms; no pathological per-pixel alpha extrusion |

Master PNGs remain unchanged by model edits. Fishing uses one texture per item across GUI, hand, ground, and fixed contexts.

The Flame Sword two-face runtime model contract is superseded by the Stage 18 post-live-test correction. The approved Flame Sword PNG remains immutable; only its runtime presentation contract changed.

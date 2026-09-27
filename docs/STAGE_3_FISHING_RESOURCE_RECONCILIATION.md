# Stage 3 Fishing runtime/resource/UX reconciliation

Status: implementation foundation for the approved Fishing vertical slice, not an economy, progression, ecology, or visual redesign.

## Baseline
- Stage 2 base: `recovery/stage-2-gameplay-reconciliation` at `fde245ab364666cbff31b49ab85b89409b1d8c7b`.
- Branch: `recovery/stage-3-fishing-resource-reconciliation`.
- Modules remain Tier I–III, Grades I–V, final 12-species roster; fish are stack-size-one ItemStacks with Species / Grade / Length / Weight stored.
- Reel controls Species, Crankbait controls Grade, Minnow gates capture. Server authority and vanilla fallback remain.

## Length, Weight, value
Length uses its species distribution. Expected Weight follows a species-specific quadratic curve of normalized Length, then receives bounded individual variation of ±4% of species Weight range and is clamped. Longer fish trend heavier; same-Length individuals may differ. Grade and modules are not size-roll inputs.
Value architecture and Grade multipliers remain. Correlated Length/Weight percentiles are averaged into one bounded 0–75% size bonus instead of separately awarding the former +75% bonuses. This narrow normalization keeps Species Base Value and Grade distinct. Emerald conversion remains BALANCE PENDING.

## Minnow and cards
The server reveals the species and sends eligibility and required Minnow Tier; HUD shows the localized requirement before minigame completion. Server denial/wear remain authoritative.
Full card rectangles route click, drag and release through normal menu handling into server-owned slots. Standard ItemStack cursor/drag/swap is retained; no client-only inventory. Source rod lock and wrong-family rejection remain.

## Resource geometry
An alpha-run estimate counts an opposing pair of side quads for each contiguous opaque run across horizontal/vertical scans, plus two front/back quads. The 12 square 512×512 fish and 9 modules estimate 138,324 alpha-derived sides +42 broad faces =138,366 quads for 21 generated models. Examples: Blue Marlin 8,724; Minnow I 7,042; Reel I 5,934; Crankbait I 7,454.
All 21 runtime models use one bounded plane with north/south faces only: 2 quads each, 42 total, estimated 3,294× aggregate reduction. Plane dimensions preserve each master's aspect ratio within a centered 16-unit square. PNG bytes, alpha, padding and source UV remain untouched. Display transforms explicitly cover GUI, both hands, ground, head and fixed/item-frame contexts. Visual appearance is LIVE PENDING.
The approved Resource D manifest has hashes for 35 item/UI images; source and JAR copies are verified. The new validator is wired to `check`. Flame Sword C1 art/model/validators are unchanged.

## Preserved flow
custom environment eligibility → Reel-filtered Species → minigame → Minnow permission → Grade → Length → Weight → fish ItemStack. Reel wear on valid start, Minnow/Crankbait wear on permitted success, broken-at-1, minigame baselines, substantial-water tolerance, and vanilla fallback remain unchanged. No fish container or fisherman sale is added.

## Validation
Run `./gradlew build`, JUnit, `validateStage6Data`, `validateFlameSwordResources`, `validateFlameSwordProductionAsset`, `validateFishingResources`, distributable JAR validation, server ready/fatal-marker smoke and client/resource smoke. Headless reload does not verify appearance.

## LIVE PENDING
- GUI, scale/aspect, filtering, pixel density.
- First-person right/left, third-person, dropped, fixed/item-frame appearance.
- Minnow notice placement/readability.
- Card click/drag/equip/unequip/swap, wrong family, rapid input, full inventory, close with carried module, source rod move/drop/hotbar swap.
- Gauge seams/scaling, icons, tier/durability overlays, card alignment.
- Natural irregular water and full loop in a live session.

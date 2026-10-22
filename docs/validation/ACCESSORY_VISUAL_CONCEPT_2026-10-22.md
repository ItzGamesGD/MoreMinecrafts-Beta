# Accessory visual assets — concept plan (PROPOSED, awaiting owner approval)

**Status:** PROPOSAL ONLY. Nothing here is design authority and no production texture has been created. `docs/CONTENT_AUTHORITY_INDEX.md` routes to no approved accessory artwork, so per task rules the identities below need owner approval before textures are produced.

Base: Draft PR #149 HEAD `6d37a73f11befea3288e028590c540dc31deda61`.

## 1. Audit

- Registry: `AccessoryDefinition` registers 12 IDs, registered through `ModItems.ACCESSORIES` under `moreminecrafts:<id>`: `snow_charm`, `deep_sea_pearl`, `ember_ring`, `ring_of_life`, `whisper_of_the_sea`, `star_covenant`, `phantom_mirror`, `ring_of_time`, `blood_covenant`, `seed_of_life`, `heart_of_gravity`, `soul_chain`.
- Missing for all 12: `items/<id>.json`, `models/item/<id>.json`, `textures/item/<id>.png` (matches the parent audit).
- **Category indicators are not textures.** `AccessoryInventoryScreenMixin.drawCategory` draws text from `accessory.category.*` lang keys; no sprite or texture is referenced, so no indicator texture is missing. Adding icons would be a new UI design decision and is not proposed unless you request it.
- Lang names exist in `en_us.json`/`ko_kr.json`; unchanged.

## 2. Established conventions (inferred, not a stated rule)

- Newly authored items (`etherium`, `mythril`, `adamantium`, `enhancement_stone`, `titanium_ingot`) are native **32×32 RGBA** pixel art, transparent background, near-black outline, 4–6 shaded tones per hue (≈300–570 distinct colours each, many from soft shading).
- Fish/fishing-gear/Flame Sword are large approved masters with bespoke models; they are not the accessory precedent.
- Item wiring precedent: `items/<id>.json` → `minecraft:model` → `moreminecrafts:item/<id>`; `models/item/<id>.json` → parent `minecraft:item/generated`, `layer0` = `moreminecrafts:item/<id>`.

## 3. Proposed identities (all 32×32, dark outline, top-left light)

Category accent: Combat = crimson/steel, Defense/Survival = amber/rust, Support/Recovery = green/rose, Exploration/Mobility = cyan/ice, Lifestyle/Gathering = teal/gold.

| ID | Shape | Material | Palette | Distinguishing feature |
|---|---|---|---|---|
| `snow_charm` | hexagonal snowflake pendant on cord | frost-silver, pale ice | white, ice blue `#bfe6f5`, slate | six-armed crystalline star |
| `deep_sea_pearl` | round pearl in open shell-claw setting | pearl, dark blue-green metal | deep teal, navy, pearl white | glossy highlight, bubble sparkle |
| `ember_ring` | thick band ring | blackened iron | charcoal, orange, ember yellow | glowing cracks and a coal-red gem |
| `ring_of_life` | slim ring with leaf motif | gold with green inlay | gold, leaf green, cream | single heart-shaped leaf gem |
| `whisper_of_the_sea` | spiral conch pendant | coral shell, brass cap | coral pink, sand, aqua | spiral with faint wave line |
| `star_covenant` | eight-point star medallion | dark steel, silver trim | midnight blue, silver, star yellow | inset small star, pact-seal ring |
| `phantom_mirror` | oval hand-mirror charm | spectral glass, pale frame | violet-grey, white, pale cyan | cracked glass with ghost-face silhouette |
| `ring_of_time` | ring with clock face | bronze, glass | bronze, cream, teal | tiny clock hands, gear tooth edge |
| `blood_covenant` | angular sigil pendant | black iron, crimson | deep red, near-black, bone | cut droplet gem in a thorn frame |
| `seed_of_life` | sprouting seed in a cage | wood, living vine | brown, sprout green, soft gold | glowing sprout core |
| `heart_of_gravity` | dense faceted heart-stone with orbit ring | dark ore, violet ring | indigo, grey, violet glow | tilted orbit ring around a heavy core |
| `soul_chain` | looped chain pendant | dark iron links, spectral | slate, soul cyan, pale white | soul-fire flickering through a broken link |

Distinct silhouettes: rings (3), pendants/medallions (6), mirror, seed-cage, chain; no two share silhouette + dominant hue.

## 4. Resource plan after approval

Create per ID: `items/<id>.json`, `models/item/<id>.json`, `textures/item/<id>.png`, following the precedent in §2. No Java changes. Add a resource validator (all 12 definitions resolve, models → existing 32×32 textures) and extend the existing Xvfb/Mesa client workflow for GUI scales 2/3/4, recipe book open/closed, accessory slot positions, and vanilla armor/offhand/Backpack regression screenshots.

## 5. Limitation

No image-generation model is available here. Textures would be hand-specified pixel grids rendered programmatically; this can produce clean 32×32 art but is not equivalent to an artist's work, and is final only if you approve it after viewing it. No placeholder art has been, or will be, presented as final.

## 6. Decisions requested

1. Approve, amend or reject each identity in §3.
2. Confirm 32×32 native size.
3. Confirm category indicators stay as text (no new textures), or request icons and specify design.

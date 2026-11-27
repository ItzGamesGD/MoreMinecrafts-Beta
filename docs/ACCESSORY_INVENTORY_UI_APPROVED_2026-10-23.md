# Accessory inventory UI — owner-approved design (2026-10-23)

Current authority for the accessory slots' presentation in the vanilla survival inventory. It replaces the separate
"Accessories" side panel from the earlier client acceptance work. It does not change accessory identities, category
rules, equipment logic, persistence, synchronization, textures or models.

## Approved design

- Five accessory slots, vertically stacked, immediately LEFT of the vanilla armor slots. The separate Accessories
  panel, its heading and all permanently visible category labels are removed.
- The five category-restricted slots keep their existing order (Combat, Defense / Survival, Support / Recovery,
  Exploration / Mobility, Lifestyle / Gathering) and all behavior (drag, click, shift-click, restrictions, persistence).
- Each slot looks like an ordinary inventory slot. No category icons, decorative panels or explanatory text.
- Name, category and other details appear only in tooltips. An occupied slot's item tooltip carries one
  `Category: …` line; an empty hovered slot (nothing on the cursor) shows its category name.
- Preferred layout with the recipe book open: `recipe book | 5 accessory slots | armor slots | main inventory`.
  The recipe book never covers an accessory slot, armor slot, inventory slot or control.

## Implementation record (Minecraft 26.3; the request cited 1.21.1)

Geometry in vanilla slot coordinates (origin = top-left of the unchanged 176×166 inventory texture):

| | Before (side panel) | After |
|---|---|---|
| Image width | 284 (widened) | 176 (vanilla) |
| Accessory slot x / y | 178 / 26 + 27n | −18 / 8 + 18n (cells −19…−1) |
| Open book x at GUI width 480 | 80 (book 80–227) | 60 (book 60–207) |
| Inventory left at width 480, book open | 175 (book overlapped it by 52 px) | 229 (vanilla) |
| Gap, book right edge → accessory cells | −52 px (overlap) | 12 px |

Feasibility verdict: independent recipe-book positioning is feasible with one small client mixin. In 26.3,
`AbstractRecipeBookScreen.init()` sets `leftPos = RecipeBookComponent.updateScreenPosition(width, imageWidth)` and the
book's own x is the private `RecipeBookComponent.getXOrigin() = (width − 147) / 2 − xOffset` (xOffset 86 beside the
GUI, 0 in narrow mode). The old design failed because widening `imageWidth` moved `leftPos` but not the book. Now
`getXOrigin()` is reduced by 20 px for the survival inventory's book only, which leaves `leftPos` and every vanilla
position untouched.

Remaining limitations / trade-offs:

- Vanilla narrow mode (GUI width < 379, e.g. a small window at a large GUI scale): the book overlays the inventory as in
  vanilla and disables inventory slots while open. The accessory cells sit outside that overlay but are inert too.
- The cells sit against the left edge of the vanilla panel, separated from the armor slots by the panel margin (7 px)
  rather than touching them. Touching them would draw over the panel's frame; owner choice if preferred.
- Item art is out of scope here (see the separate artwork PR); missing-texture icons in screenshots are expected on
  this base.

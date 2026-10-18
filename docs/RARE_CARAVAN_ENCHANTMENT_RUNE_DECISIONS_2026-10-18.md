# MoreMinecrafts — 희귀 교역단 및 인챈트 룬 결정 (2026-10-18)

**Status:** User-confirmed design directions; documentation only. Merchant catalog, exact price, rarity, stock/restock and complete rune UI details remain to be designed. This document corrects the previous assistant misunderstanding of rune/option/enchantment relationships. Do not implement guessed rules.

## 1. Rare merchant caravan: confirmed

- **Five entities total:** caravan leader merchant (1), materials merchant (1), guard (1), pack llamas (2).
- **Spawn model A:** replace **some existing vanilla wandering trader appearances** with caravans. The exact replacement chance and any proposed slight total-frequency increase are NOT approved numerical values; retain vanilla game-rule compatibility and avoid a second independent patrol-like timer.
- **Merchandise model C:** mixture of unique functional goods and powerful equipment. Exact exclusive goods are deferred for later detailed design.
- **Implicit merchant tiers and combinations:** vary generated equipment and merchandise across merchant appearances, so distinct merchant/stock combinations imply different quality tiers **without requiring visible tier labels**. Actual weighted pools and tier thresholds TBD.
- **Purchase restrictions:** control purchases **through pricing**, not per-player purchase caps or arbitrary per-item transaction quotas. Exact shared stock, infinite stock, and restocking behavior are separate unresolved choices; do not silently reintroduce a purchase cap via stock policy.
- The **Sharpness VI and VII enchantment runes** are explicitly important prospective caravan merchandise. Pricing, availability and whether any other acquisition path exists TBD.

## 2. Critical system distinction: runes are real enchantments, books are options

**User correction (normative):** Enchantment runes **apply the actual enchantment** to equipment; they do **not** reroll or grant a separate option and are not merely a voucher to unlock an enchantment progression tier.

- **Enchantment Rune:** inserted **instead of an Enhancement Stone** in the relevant enhancement interface; performs an **actual enchantment application** to the target equipment. The resulting enchantment must be represented by the same enchantment data/logic used by the existing enchantment system, not a second custom stat modifier.
- **Vanilla Enchanted Book in the MoreMinecrafts system:** belongs to the **separate option grant/reroll workflow**, not the rune workflow. Maintain this distinction throughout UI, costs, data storage and tooltips.
- **Enhancement Stone:** the equipment enhancement resource; remains distinct from both enchantment runes and books.
- Sharpness VI and Sharpness VII are **real Sharpness levels 6 and 7**, part of the **existing enchantment table's level/progression structure**, not independent modifiers. No extra option slot consumed by applying a rune.
- Rune combination/fusion is **not allowed**. Do not assume combining enchanted books creates level VI/VII or that Sharpness V must be upgraded sequentially first.
- **A Sharpness VI rune can apply Sharpness VI directly** (no prerequisite Sharpness V). Similarly, rune application is not a mandatory VI-then-VII ladder; detailed downgrade prevention and level compatibility rules can be specified separately.
- **Critical UI behavior:** if an item already has **Sharpness VI**, placing it in the existing enchantment table/interface must automatically show **Sharpness levels I–V as completed** (and level VI as already applied), **without adding duplicate lower-level enchantment entries**. The player must still be able to work on **other compatible enchantments** normally. This is a state/UI interpretation of the item's actual enchantment level, not separate purchases of five levels.
- The same monotonic completed-lower-level display should generalize to level VII and other rune-based over-cap levels where appropriate; do not regress the current enchantment progression.
- The enchantment table's inclusion of Sharpness VI/VII in its progression **does not by itself authorize normal random vanilla enchanting to roll VI/VII**. Whether these levels are obtainable there without runes remains unresolved; avoid guessing.

## 3. Required future implementation checks

1. Audit existing MoreMinecrafts enchanting/options UI and code; preserve book-as-option behavior and distinguish enchantment-table workflow from anvil/equipment enhancement workflow where they are separate.
2. A rune transaction consumes exactly the appropriate rune and applies a true enchantment once; preserve unrelated enchantments, other options and enhancement levels.
3. Sharpness VI on sword => enchantment table marks I–V complete, VI applied, and unrelated compatible enchantments remain actionable; no duplicate enchantments or option rerolls.
4. Sharpness VII => corresponding lower-tier completion state; lower-level runes cannot silently downgrade a higher-level enchantment.
5. Enchantment compatibility, server-authoritative state, UI sync, inventory rollback and persistence after reconnect/restart.
6. Explicitly document **exactly which UI input slot** accepts a rune and how it coexists with accessory enhancement/catalyst UI, after checking existing screens. The statement 'instead of enhancement stone' is authoritative; UI geometry is not yet settled.
7. Check vanilla maximum-enchantment-level constraints, anvil/book combination routes, tooltip presentation and actual damage calculation so levels VI/VII work as real enchantments rather than visually-only values.
8. Merchant stock/price/tier distribution and the rune acquisition economy must be balanced later; do not claim these have been implemented or validated.

## 4. Supersession note

Any prior assistant proposal that runes are **upgrade unlock vouchers** requiring a Sharpness V item before applying VI, or requiring VI before VII, is **rejected**. Any prior conflation of enchanted books with applying real enchantments rather than the established option/reroll workflow is **rejected**. The confirmed user distinction in this document takes precedence over such proposals.

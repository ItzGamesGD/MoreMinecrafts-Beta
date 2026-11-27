# MoreMinecrafts — 희귀 교역단 및 인챈트 룬 결정 (2026-10-18)

**Status: user-confirmed design decisions; DOCUMENTATION ONLY / NOT IMPLEMENTED.** This revision supersedes earlier two-merchant, undecided-stock, all-tier rune quotas, and normal-level rune merchandise proposals. Specific rune prices, per-rune draw weights, exclusive custom merchandise and template identities are **NOT approved**; see section 7 for newly approved v1 spawn/generation baselines and vanilla merchandise prices.

## 1. Rare merchant caravan — confirmed

- **Exactly five entities: ONE merchant (1) + guard NPC (1) + pack llamas (3).** The previous merchant leader + materials merchant + guard + 2 llamas roster is rejected. All merchandise categories are handled by the single merchant.
- **Spawn mode A:** replace some existing vanilla wandering trader spawn appearances, preserving relevant vanilla gamerules. Exact replacement percentage and any change to total wandering-trader frequency are not approved.
- **Merchandise model C:** a broad specialty caravan carrying hard-to-obtain vanilla items, higher-level real-enchantment runes, enhanced/enchantment equipment, and later-designed exclusive functional gear. Do not populate generic food, logs, stone, ordinary ores/ingots or routine materials simply to fill a category.
- **Implicit merchant quality tier:** merchandise mix/quality may vary by spawn, but do not show explicit tier labels. Proposed 60/30/9/1 weights, fixed per-tier SKU counts and tier-dependent guaranteed rune slots are NOT approved.
- **Unlimited stock:** any offered trade can be repeated as long as the player can pay. **Price-only purchase control**: no per-player quotas, arbitrary cooldowns, depletion, restock throttles or mandatory limited quantities. Trade selection and pricing should persist across chunk reload/server restart; no reroll exploit.
- **Vanilla merchant trade has two payment input slots.** Accept emerald blocks (worth 9 emeralds each) plus emeralds where necessary, instead of assuming six stacks of emeralds are required for a 384-emerald price. For a price P, block count = floor(P/9), remainder = P mod 9, within vanilla input stack limits; validate actual trade/slot compatibility during implementation. Example: 384 = 42 blocks + 6 emeralds. No custom checkout UI is authorized solely by that price.
- **Not every tier must offer runes.** Do not force a rune minimum on normal or other tier merchants. Runes are rare specialty merchandise selected from the approved over-cap list only.

## 2. Approved vanilla rare merchandise: eleven groups

The user explicitly selected these eleven **candidate sale groups** as approved caravan inventory. Variant types within a group may expand the SKU pool.

1. Enchanted Golden Apple (마법이 부여된 황금 사과)
2. Echo Shard (메아리 조각)
3. Sniffer Egg (스니퍼 알)
4. Trident (삼지창)
5. Wither Skeleton Skull (위더 스켈레톤 해골)
6. Heart of the Sea (바다의 심장)
7. Totem of Undying (불사의 토템)
8. Shulker Shell (셜커 껍데기)
9. Breeze Rod (브리즈 막대)
10. Ominous Bottle levels III–V (불길한 병 III~V)
11. Rare Armor Trim Smithing Templates (희귀 갑옷 장식 형판); exact eligible template IDs still need approval.

**Not approved for this catalog:** generic everyday supplies, ordinary mineral/material bulk trade, elytra, wind charges, rare music discs, pottery sherds, and other unselected candidate goods. These are not silently added.

**Prior assistant-only pricing remains unapproved:** enchanted golden apple 108, echo shards 4 for 36, sniffer egg 72, trident 81, skull 90, heart 108, totem 72, shulker shells 2 for 81, breeze rods 4 for 36, ominous bottles III/IV/V 27/45/72, and trim templates by category. Do not implement these as confirmed economy settings. Trade quantity per purchase is also provisional.

## 3. Enchantment system distinction — authoritative

- **Rune = the role of a vanilla enchanted book for actual enchantment application.** It gives the equipment a **real enchantment** at the rune's level, not an unlock token, custom stat modifier or option reroll.
- **Vanilla enchanted books in MoreMinecrafts = separate option granting/rerolling**, not the actual-enchantment application path.
- **Enhancement stones** remain separate equipment enhancement resources. Earlier design placed rune in the relevant enhancement interface in lieu of a stone; exact UI placement and application transaction require an implementation audit. Do not invent a different mechanic merely because rune behaves like a vanilla enchanted book.
- **Normal enchantments obtainable by paying XP at the existing enchantment table have no reason to be sold as caravan runes.** Merchant rune inventory contains **only the specifically approved above-normal-table-cap levels** below.
- No rune fusion; no sequential lower-level prerequisite. A Sharpness VI rune can directly apply VI without acquiring V first; VII can directly apply VII.
- If a higher level is present, all lower levels appear completed in the existing enchantment-table progression, without duplicate enchantments; other compatible enchantments remain available. Do not silently downgrade a higher level.
- Enchantment incompatibility, genuine level effect, preservation of existing options/enchantments, persistence and server authority apply.

### Approved over-cap rune SKU catalog — 24 distinct level variants

| Enchantment (Korean) | Rune levels | SKU count |
| --- | --- | ---: |
| Sharpness (날카로움) | VI, VII | 2 |
| Smite (강타) | VI, VII | 2 |
| Looting (약탈) | IV, V | 2 |
| Efficiency (효율) | VI, VII, VIII | 3 |
| Fortune (행운) | IV, V | 2 |
| Protection (보호) | V, VI | 2 |
| Fire Protection (화염 보호) | V, VI | 2 |
| Blast Protection (폭발 보호) | V, VI | 2 |
| Projectile Protection (발사체 보호) | V, VI | 2 |
| Depth Strider (물갈퀴) | IV | 1 |
| Power (힘) | VI | 1 |
| Unbreaking (내구성) | IV, V | 2 |
| Mending (수선) | II | 1 |
| **TOTAL** | | **24** |

**Mending expansion:** base Mending I repairs 2 durability per 1 XP; new Mending II repairs 4 per 1 XP. The existing enchantment enhancement mechanic adds **+2 repair per XP**: enhanced I = 4/XP, enhanced II = 6/XP. Treat enchantment level and enhanced state as separate if the current implementation supports that distinction; audit actual mechanics rather than assuming how its flag is stored.

**Rune price proposals are NOT approved:** earlier 192 emeralds for Sharpness VI, 384 for Sharpness VII, 54 for Mending I and 162 for Mending II, and any generic level-squared pricing formula. In particular **Mending I is NOT a caravan rune SKU** under this design.

## 4. Merchant merchandise generation: confirmed vs pending

**Confirmed:** one merchant, varied implicit quality, eleven selected vanilla rare-goods groups, 24 above-cap rune SKUs, specialty/enhanced equipment category, future exclusive/custom items (design deferred), unlimited repeat purchase, price-only gating, and **no mandatory rune inventory in every merchant tier**.

**Pending approval:** exact tier distribution, chance to have any rune by tier, rune selection weights, number of trade entries, prices, per-trade item quantity, exact eligible rare armor trims, enhanced equipment level ranges, exclusive merchandise, trader replacement probability and despawn/lifecycle details. Do not treat previously suggested 16/21/26/32 trade counts, 60/30/9/1 tier weights or fixed rune slots as settled.

## 5. Future implementation and QA checks

1. Inspect actual Minecraft 26.3 mapped wandering trader spawning and trade input slots, and current MoreMinecrafts enchanting, options, enhancement, rune, and merchant source before editing gameplay.
2. Ensure only **one** merchant appears with one guard and three llamas; do not spawn the obsolete second merchant.
3. Unlimited repeat trades with correct server-side payment, emerald-block conversion, stable catalog after reload, and no duplicated output.
4. Verify all eleven vanilla groups and exactly 24 rune level SKUs resolve to real registered items/enchantments; keep unapproved rare trim IDs out until selected.
5. Applying runes yields true levels and expected mechanics (including Efficiency VIII and Mending II); never use vanilla books as rune substitutes or options as real enchantments.
6. Table/UI automatically marks lower levels completed for rune-applied higher enchantments while allowing other compatible enchants.
7. Confirm the existing enchantment enhancement rule yields Mending I 2→4 and II 4→6 durability per XP without double-applying bonus or incorrect XP consumption.
8. Distinguish **design documented** from CODE VERIFIED, LIVE VERIFIED and gameplay implementation. This update claims none of those validations.

## 6. Superseded proposals

Rejected: two merchants, two llamas, finite/undecided stock, per-tier guaranteed rune slots, low-level runes available from normal XP enchanting, routine materials/ores as filler, rune-as-unlock-voucher, books-as-real-enchantment-items, and sequential rune upgrades. Preserve the exact user decisions above over older proposals.


## 7. User-approved v1 spawn and generation baseline (2026-10-18)

This section supersedes prior language saying that the replacement chance, tier distribution, SKU counts, or rune-presence rates are wholly undecided. **These are accepted v1 design targets, NOT tested gameplay balance or implemented code.**

- **Vanilla wandering trader : rare caravan = 1:1.** On a *successful eligible vanilla wandering-trader appearance*, replace with caravan at **50%**, otherwise preserve vanilla trader. Do not double spawn or change vanilla attempt frequency, eligibility or gamerules. Verify precise mapped 26.3 spawn-hook behavior; avoid consuming a vanilla spawn on caravan-placement failure without an intentional fallback.
- **Implicit merchant tier distribution, conditional on caravan:** COMMON 55%, ADVANCED 30%, RARE 12%, LEGENDARY 3%; do not display tier labels.
- **Total distinct trade offers, by tier:** COMMON 12–16; ADVANCED 15–20; RARE 18–24; LEGENDARY 22–28. Trade entries are not purchase caps; all offered trades have unlimited repeat stock.
- **Chance a merchant offers at least one over-cap rune:** COMMON 0%, ADVANCED 15%, RARE 50%, LEGENDARY 90%. Proposed counts when runes are present: ADVANCED 1, RARE 1–2, LEGENDARY 2–4. The number of rune offers is a v1 design baseline, not a guaranteed quota across all tiers.
- **Rune level access:** highest-level runes are weighted toward LEGENDARY, but **RARE merchants may offer top-level runes at extremely low nonzero chance** (user selected probability-based option B). The exact per-rune weights are still unresolved. Do not hard-lock every top-level rune to LEGENDARY.
- **Remaining merchandise mix v1 suggestion:** after optional rune selection, draw ordinary rare vanilla goods, enhanced/enchantment equipment and (when designed) exclusive specialty goods using tier-specific weights: COMMON 65/30/5%; ADVANCED 55/35/10%; RARE 45/40/15%; LEGENDARY 35/45/20%. These percentages are recommended algorithm baselines; specialty items are still deferred and must not be fabricated or silently shipped as filler.
- Deduplicate identical offers; persist generated offer list and prices; no reload rerolls; no forced guaranteed rune in every caravan.

### 7.1 User-adjusted vanilla merchandise price baseline

These **user-adjusted** prices replace prior conflicting assistant-only prices. All blocks mean **emerald blocks (9 emeralds each)**. Other prices were accepted by user instruction '나머진 그대로' and retained from the immediately preceding proposal.

| Item | Output quantity | Price |
| --- | ---: | --- |
| Breeze Rod | 4 | 8 emeralds |
| Echo Shard | 4 | 4 emeralds |
| Sniffer Egg | 1 | 1 emerald block |
| Heart of the Sea | 1 | 4 emerald blocks |
| Wither Skeleton Skull | 1 | 15 emerald blocks |
| Shulker Shell | 2 | 9 emerald blocks |
| Trident | 1 | 9 emerald blocks |
| Totem of Undying | 1 | 8 emerald blocks |
| Enchanted Golden Apple | 1 | 12 emerald blocks |
| Ominous Bottle III | 1 | 3 emerald blocks |
| Ominous Bottle IV | 1 | 5 emerald blocks |
| Ominous Bottle V | 1 | 8 emerald blocks |

**Template placeholder:** record only **'형판' (template)** as a reserved merchandise group. Do not preselect vanilla armor-trim template IDs, prices or recipes. Later connect this placeholder to future equipment upgrade trees beyond netherite; design deferred.

**Unresolved:** specific 24 over-cap rune SKU prices, exact per-rune draw weights, caravan-exclusive custom merchandise and final merchant UI/persistence details. Earlier price suggestions for rune levels are not user-approved and must not be implemented as final.

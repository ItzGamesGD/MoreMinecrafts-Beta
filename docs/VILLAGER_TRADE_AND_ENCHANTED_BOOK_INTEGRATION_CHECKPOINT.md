# Villager Trade and Enchanted Book Integration Checkpoint

Status: **CURRENT DESIGN CHECKPOINT**

Baseline: `stage-19/checkpoint` @ `984418d302cf865b5654fac0a0dcb28449fb6118`

Working design line: `design/t1-enchantment-skill-tree`

This document records the human-approved cross-system decisions connecting the T1 XP enchantment redesign, Enchanted Books, vanilla-style UI surfaces, and vanilla villager trading.

Confirmed rules are authoritative. Items explicitly marked OPEN remain design work and must not be invented during implementation.

---

## 1. System boundary

The primary T1 enchantment progression is the XP-funded equipment skill tree defined by `T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md`.

Therefore:

- an Enchanted Book must **not** directly grant or bypass a skill-tree enchantment;
- the old `Enchanted Book + Anvil -> enchantment applied to equipment` progression path is retired;
- Enhancement `+0 -> +20`, the XP Enchantment Skill Tree, and Book Option are separate growth axes;
- Book Option is an **optional limit-pushing optimization layer**, not required for a practically complete/endgame T1 item.

Intended role split:

`Enhancement -> base-stat growth`

`XP Enchantment Skill Tree -> primary enchantment/function growth`

`Book Option -> optional final optimization beyond the primary tree`

A player who stops after Enhancement + Skill Tree must not feel that the equipment is incomplete merely because Book Option was not pursued.

---

## 2. Book Option application model

### 2.1 All listed numeric effects are bonuses

Every numeric Book Option value in this document is an **additional bonus layered on top of the ordinary enchantment effect**.

It must never be interpreted as a replacement value for the vanilla enchantment or as a rewrite of the vanilla enchantment's base formula unless a later authority explicitly says so.

Example: Mending retains its normal behavior and its Book Option improves the repair efficiency from 2 durability per XP to 4 durability per XP.

### 2.2 Option result tiers

A compatible retained book is consumed through the Book Option flow and produces one fixed result tier:

- Option I — lowest
- Option II
- Option III — highest

Once applied, the rolled option is fixed. It is not freely overwritten.

Higher source-book level improves the probability distribution; it does not directly guarantee the corresponding Option tier.

### 2.3 Common normalized probability distribution

All retained books that use randomized Option I/II/III quality use one normalized common rule rather than per-enchantment hand-authored probability tables.

Normalize source-book level as:

`t = (current book level - 1) / (maximum book level - 1)`

For maximum-level-V books, the approved anchor distribution is:

| Source book | Option I | Option II | Option III |
|---|---:|---:|---:|
| I/V | 70% | 25% | 5% |
| II/V | 55% | 35% | 10% |
| III/V | 40% | 40% | 20% |
| IV/V | 25% | 45% | 30% |
| V/V | 10% | 40% | 50% |

Other maximum book levels use the same normalized progression/interpolation rather than receiving unrelated probability tables.

Examples already accepted:

- I/III -> 70 / 25 / 5
- II/III -> 40 / 40 / 20
- III/III -> 10 / 40 / 50

The highest-level source book therefore still has a 10% chance to produce the lowest Option I. Low-level books retain a small chance to produce Option III.

This common probability mechanism is especially relevant to the damage-family retained books; do not invent bespoke probability curves without a later design change.

---

## 3. Retained Book Option roster and bonuses

The following Enchanted Book variants remain eligible for the Book Option system.

All values below are **additional bonuses** on top of the corresponding enchantment's normal effect.

### 3.1 Durability / repair

**Unbreaking / 내구성**
- each Option tier adds a further 5 percentage-point reduction to durability-consumption probability.

**Mending / 수선**
- Book Option improves repair efficiency from the normal 2 durability per 1 XP to 4 durability per 1 XP.

### 3.2 Melee damage family

**Sharpness / 날카로움**
- maximum Book Option tier: III;
- source-book level modifies the common Option-tier probability distribution.

**Smite / 강타**
- same Book Option model as Sharpness;
- maximum Option tier III;
- source-book level modifies the common Option-tier probability distribution.

**Bane of Arthropods / 살충**
- same Book Option model;
- maximum Option tier III;
- source-book level modifies the common Option-tier probability distribution.

Exact per-tier damage-family bonus values beyond the approved tier/probability structure remain OPEN unless separately recorded.

### 3.3 Sword utility

**Sweeping Edge / 휩쓸기**
- +3.5 percentage points of sweeping damage per Option tier, added on top of the normal enchantment effect.

**Fire Aspect / 발화**
- +2 seconds fire duration per Option tier.

**Knockback / 밀치기**
- retain as a Book Option;
- intended direction is an additional 0.5-step correction/bonus relative to the underlying knockback-speed calculation;
- exact internal formula/application point requires code/runtime verification before implementation and is OPEN.

**Looting / 약탈**
- Enchanted Book variant is removed from Book Option supply because the affected loot rules are too heterogeneous for the intended simple Option layer.

### 3.4 Bow

**Power / 힘**
- +5.5% damage per Option tier, additional to the normal Power effect.

### 3.5 Crossbow

**Quick Charge / 빠른 장전**
- -0.1 seconds additional charge time per Option tier.

**Piercing / 관통**
- +1 additional entity penetrated per Option tier.

### 3.6 Armor protection family

**Protection / 보호**
- +2.5% additional protection per Option tier.
- Use the game's protection/capping model as the basis.
- When the broader protection result would exceed the ordinary 80% region, the intended final high-end cap is 90%.
- This 90% final cap is intended to remain the ultimate cap for later endgame armor as well unless separately redesigned.

**Fire Protection / 화염으로부터 보호**
- +2.5% additional protection per Option tier;
- additionally reduce fire duration by 5% per Option tier.

**Projectile Protection / 발사체로부터 보호**
- +2.5% additional protection per Option tier.

**Blast Protection / 폭발로부터 보호**
- +2.5% additional protection per Option tier;
- +2.5% additional explosion-knockback reduction per Option tier.

---

## 4. Removed Enchanted Book variants

For the following enchantments, **the enchantment/function itself is not deleted merely by this rule**.

Instead, its itemized Enchanted Book variant is removed from survival/world supply. It must not naturally appear through loot, structure/chest rewards, villager trading, fishing, or any other ordinary world-generation/acquisition source.

The XP Skill Tree remains the owner of the enchantment/function where that enchantment remains part of current gameplay authority.

Removed Book variants include:

- Looting / 약탈
- Efficiency / 효율
- Fortune / 행운
- Silk Touch / 섬세한 손길
- Vein Mining / 광맥 채굴
- Auto Smelt / 자동 제련
- Auto Replant / 자동 재식재
- all Mace-related enchantment books
- Lunge / 돌진
- Punch / 밀어내기
- Flame / 화염
- Infinity / 무한
- Multi Shot / 다중 사격
- Laser Arrow / 레이저 화살
- Multishot / 다중 발사
- Crossbow Barrage / 쇠뇌 연사
- Respiration / 호흡
- Aqua Affinity / 친수성
- Thorns / 가시
- Swift Sneak / 신속한 잠행
- Feather Falling / 가벼운 착지
- Depth Strider / 물갈퀴
- Frost Walker / 차가운 걸음
- Soul Speed / 영혼 가속
- Feather Boots / 깃털 부츠
- Jet Boots / 제트 부츠
- and all remaining book variants below/outside the retained roster in Section 3, including Elytra/Trident/Fishing/custom functional books.

Implementation must treat the retained roster in Section 3 as the allow-list for Book Option/world-supplied Enchanted Books unless a later authority explicitly adds another book.

---

## 5. Enchanting Table and Anvil functional surfaces

### 5.1 UI language

Use vanilla Minecraft's common UI grammar and interaction language.

Do not design bespoke visual styling/assets yet. Exact layout, textures, icons, spacing, and final presentation are deferred to the implementation/UI design pass.

### 5.2 Enchanting Table

The old random enchanting function is removed.

The Enchanting Table becomes the surface for two selectable functions:

`Enchantment Skill Tree | Book Option Application`

- **Enchantment Skill Tree**: XP-funded primary enchantment progression.
- **Book Option Application**: consumes a compatible retained Enchanted Book to roll/apply its optional optimization result.

### 5.3 Anvil

The Anvil surface is organized around:

`Enhancement | Normal Anvil | Enchanted Book Combination`

- **Enhancement**: current +0 -> +20 Enhancement flow. It currently uses the anvil-style flow and may be separated as a functional tab/surface while retaining vanilla UI grammar.
- **Normal Anvil**: ordinary applicable anvil operations, but it must not restore direct Enchanted Book -> equipment enchantment application.
- **Enchanted Book Combination**: combines compatible books to raise book level.

Enchanted Book combination uses **XP only** as its additional cost. Do not introduce a new currency/material solely for book-level combination.

Exact XP amounts remain BALANCE/OPEN.

---

## 6. Book Option reset

Book Options are intentionally expensive to reset so that Option III optimization remains a high-end optional pursuit rather than mandatory routine rerolling.

Reset requires a combined material cost containing all of:

- Diamond
- Mythril
- Netherite
- Adamantium

Exact quantities remain OPEN until material acquisition/rarity is sufficiently finalized.

Reset semantics:

`locked Book Option -> consume reset materials -> clear Book Option -> item becomes eligible for another Book Option application`

Reset does not itself roll a replacement Option.

This creates a meaningful late-game material sink for Mythril and Adamantium in addition to Diamond and Netherite.

---

## 7. Economic intent

Book Option deliberately introduces a second optimization cost layer after ordinary equipment progression.

The intended player perception is:

- Enhancement + XP Skill Tree = a complete, strong T1 equipment progression;
- Book Option = optional investment for players who want to push an item toward its theoretical limit.

Do not balance ordinary T1 survival around the assumption that every player has Option III on every item.

High-level books remain valuable because they improve the common probability distribution, but even a maximum-level book does not guarantee Option III.

The combination of:

- XP cost to create/combine higher-level books,
- book consumption/application,
- probabilistic Option quality,
- and expensive Diamond + Mythril + Netherite + Adamantium reset

is intentional friction for limit optimization.

---

## 8. Villager repeatability constraint

Villager breeding, restocking, and trading can create effectively repeatable supply.

Therefore villager balance must not rely only on nominal rarity or low initial appearance rates.

The Librarian's final retained-book inventory, weights, restock behavior, and pricing remain OPEN, but it may only supply books from the retained Book Option roster once the final trade table is designed.

Removed Book variants in Section 4 must not remain available through Librarian trades.

Other profession status remains:

- Farmer: KEEP vanilla.
- Fisherman: separate custom-Fishing economy design; Vanilla / Custom Fishing presentation split remains an allowed direction.
- Fletcher: retain vanilla baseline; focused review later if needed.
- Cartographer: KEEP now; future Exploration/structure authority must explicitly review it.
- Cleric: KEEP now; future Brewing/Alchemy authority must explicitly review it.
- Armorer / Weaponsmith / Toolsmith: CORE OPEN PROBLEM because direct equipment sales may bypass/distort T1 equipment progression.
- Mason / Shepherd / Butcher / Leatherworker: KEEP vanilla unless a later concrete conflict appears.

---

## 9. Remaining OPEN items

Do not invent these during implementation:

- exact Book Option slot model/count per equipment item;
- exact XP costs for Enchanted Book Combination;
- exact XP/material application cost if Book Option application needs cost beyond consuming the book;
- exact reset quantities for Diamond / Mythril / Netherite / Adamantium;
- exact Knockback Book Option internal formula/application point;
- exact per-tier damage-family bonus values where not separately fixed;
- exact UI layout/art/design beyond the vanilla-common-grammar requirement;
- Librarian final retained-book trade inventory/weights/prices/restock behavior;
- Armorer / Weaponsmith / Toolsmith replacement model;
- Fisherman custom-economy values/final presentation;
- Fletcher exact trade changes;
- skill-tree persistence/binding and respec items tracked by the T1 enchantment authority.

---

## 10. Implementation stop conditions

Until the OPEN items above are separately finalized:

- do not restore direct book-to-enchantment application;
- do not make Enchanted Books an XP-skill-tree bypass;
- do not add world-supplied books outside the retained allow-list;
- do not reinterpret Book Option numbers as replacements for vanilla enchantment values;
- do not invent bespoke probability tables where the common normalized distribution applies;
- do not invent reset quantities;
- do not invent final UI art/layout;
- do not implement a final Librarian trade table;
- do not implement a final Armorer / Weaponsmith / Toolsmith solution.

This checkpoint preserves the Book Option core architecture while leaving only explicit balance, UI-detail, and remaining villager-integration decisions open.

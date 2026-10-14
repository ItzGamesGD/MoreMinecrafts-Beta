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

### 2.2 Option level follows the enchantment's vanilla level structure

Book Option does **not** use a universal I-III tier cap.

For each retained enchantment, the Book Option maximum level follows that enchantment's own vanilla maximum level.

Examples:

- Sharpness V -> Book Option (+I) through (+V)
- Protection IV -> (+I) through (+IV)
- Unbreaking III -> (+I) through (+III)
- Fire Aspect II -> (+I) through (+II)
- Mending I -> (+I)

A compatible retained book is consumed through the Book Option flow and rolls one level inside that enchantment-specific range.

Once applied, that Book Option level is fixed until the expensive reset flow is used.

All compatible retained Book Options on an item may coexist. There is no shared equipment-wide Book Option slot limit and no competition between different enchantments' Book Options. Each supported enchantment independently owns its own bonus level.

Tooltip/presentation direction is to display the bonus adjacent to the existing enchantment, for example:

`Sharpness V (+IV)`

Exact visual styling remains deferred.

### 2.3 Common binomial probability rule

Source-book level changes the probability of rolling higher Book Option levels. It does **not** cap the possible result at the source-book level.

Let:

- `M` = the enchantment's vanilla maximum level, which is also its maximum Book Option level;
- `L` = the source Enchanted Book's current level;
- `B` = the source book's maximum level.

For enchantments with `B > 1`, map source-book quality linearly from `p = 0.20` at the lowest book level to `p = 0.80` at the highest:

`p = 0.20 + 0.60 * ((L - 1) / (B - 1))`

Then roll:

`Book Option level = 1 + Binomial(M - 1, p)`

Consequences:

- every result from (+I) through (+M) remains possible for every source-book level;
- a low-level book can very rarely roll the maximum Book Option;
- a maximum-level book can still very rarely roll (+I);
- higher-level books shift the distribution toward stronger results rather than guaranteeing them;
- one formula handles enchantments with different vanilla maximum levels.

For a maximum-V enchantment, source-book p values are:

| Source book | p |
|---|---:|
| I/V | 0.20 |
| II/V | 0.35 |
| III/V | 0.50 |
| IV/V | 0.65 |
| V/V | 0.80 |

Example distributions for a maximum-V Book Option:

| Source book | (+I) | (+II) | (+III) | (+IV) | (+V) |
|---|---:|---:|---:|---:|---:|
| I/V | 40.96% | 40.96% | 15.36% | 2.56% | 0.16% |
| III/V | 6.25% | 25.00% | 37.50% | 25.00% | 6.25% |
| V/V | 0.16% | 2.56% | 15.36% | 40.96% | 40.96% |

Examples at other vanilla maximum levels:

- Unbreaking III from an Unbreaking III book: (+I) 4%, (+II) 32%, (+III) 64%.
- Protection IV from a Protection IV book: (+I) 0.8%, (+II) 9.6%, (+III) 38.4%, (+IV) 51.2%.
- Fire Aspect I book: (+I) 80%, (+II) 20%.
- Fire Aspect II book: (+I) 20%, (+II) 80%.

For a maximum-I enchantment such as Mending, the only valid Book Option level is (+I), so the result is deterministic.

Do not restore the superseded universal I/II/III probability table.

---

## 3. Retained Book Option roster and bonuses

The following Enchanted Book variants remain eligible for the Book Option system.

All values below are **additional bonuses** on top of the corresponding enchantment's normal effect.

### 3.1 Durability / repair

**Unbreaking / 내구성**
- each Option tier adds a further 5 percentage-point reduction to durability-consumption probability.
- this bonus applies to ordinary eligible durability use and authorized Lumberjack operations; it does not reduce Vein Mining's separately defined block-count / level-II half-cost calculation.

**Mending / 수선**
- Book Option improves repair efficiency from the normal 2 durability per 1 XP to 4 durability per 1 XP.

### 3.2 Melee damage family

**Sharpness / 날카로움**
- Book Option range follows Sharpness's vanilla maximum: (+I) through (+V);
- source-book level shifts the common binomial distribution;
- add +0.25 damage per Option level, on top of the ordinary Sharpness effect.

**Smite / 강타**
- Book Option range follows Smite's vanilla maximum: (+I) through (+V);
- source-book level shifts the common binomial distribution;
- add +0.25 damage per Option level, on top of the ordinary Smite effect.

**Bane of Arthropods / 살충**
- Book Option range follows Bane of Arthropods's vanilla maximum: (+I) through (+V);
- source-book level shifts the common binomial distribution;
- add +0.25 damage per Option level, on top of the ordinary Bane of Arthropods effect.

A maximum (+V) damage-family Option therefore adds +1.25 damage.

### 3.3 Sword utility

**Sweeping Edge / 휩쓸기**
- +3.5 percentage points of sweeping damage per Option tier, added on top of the normal enchantment effect.

**Fire Aspect / 발화**
- +2 seconds fire duration per Option tier.

**Knockback / 밀치기**
- retain as a Book Option;
- intended direction is an additional 0.5-step correction/bonus relative to the underlying knockback-speed calculation;
- the implementation applies +0.5 to vanilla's pre-halving helper output per Option level, equivalent to +0.25 to the final knockback-speed multiplier; this is an implementation default pending live combat tuning.

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

The UI is split by workstation. It is **not one shared four-mode menu**.

- Enchanting Table owns: `Enchantment | Book Option Application`.
- Anvil owns: `Enhancement | Normal Anvil | Enchanted Book Combination`.

Mode selection is exposed as new selectable tabs/buttons at the top of the corresponding workstation UI.

Do not introduce a modern web/mobile visual language. Preserve vanilla-style slots, panels, controls, inventory presentation, and interaction grammar. Exact texture/icon polish may be refined during implementation.

### 5.2 Enchanting Table

The old random enchanting function is removed.

The Enchanting Table becomes the surface for two selectable functions:

`Enchantment Skill Tree | Book Option Application`

- **Enchantment**: XP-funded primary enchantment progression. The previous working label "Skill Tree" is replaced by the player-facing label "Enchantment / 인챈트".
- **Book Option Application**: consumes a compatible retained Enchanted Book to roll/apply its optional optimization result.

Enchantment-mode direction:
- provide one dedicated equipment input slot;
- the enchantment tree is contextual to the equipment inserted into that slot;
- do not show unrelated equipment/category nodes mixed into one tree;
- inserting a sword shows the sword-compatible tree, inserting boots shows the boots-compatible tree, etc.;
- the already approved connected-node/tree direction remains valid.

Book Option Application direction:
- retain the previously approved simple equipment + compatible Enchanted Book -> resulting equipment presentation;
- show the relevant Book Option result/range and XP application cost;
- Book Option application consumes XP in addition to the compatible book;
- the equipment must already own the corresponding primary Enchantment through XP progression; a Book Option never grants its underlying enchantment.

### 5.3 Anvil

The Anvil surface is organized around:

`Enhancement | Normal Anvil | Enchanted Book Combination`

- **Enhancement**: current +0 -> +20 Enhancement flow. Keep this UI intentionally minimal: one equipment slot, one Enhancement Stone slot, and an Enhancement action/button area are sufficient. Enhancement uses Enhancement Stones; do not invent additional material slots for this mode.
- **Normal Anvil**: ordinary applicable repair/name operations, but it must not transfer Enchantments from a book or another equipment item. Enchantment progression belongs to the XP Enchantment system. Book renaming need not be supported.
- **Enchanted Book Combination**: combines compatible books to raise book level. Its interaction/layout should closely follow the familiar vanilla Anvil combination grammar rather than introducing a separate elaborate tree/menu.

Enchanted Book combination uses **XP only** as its additional cost. Do not introduce a new currency/material solely for book-level combination.

The current implementation defaults remain 200 raw XP per Book Option application and 100 raw XP times the resulting level for book combination. They are provisional balance values, not finalized authority.

---

## 6. Book Option reset

Book Options are intentionally expensive to reset so that maximum/high Book Option optimization remains a high-end optional pursuit rather than mandatory routine rerolling.

Reset requires a combined material cost containing all of:

- Diamond
- Mythril
- Netherite
- Adamantium

Current provisional reset quantities are fixed at one of each material:
- Diamond x1
- Mythril x1
- Netherite x1
- Adamantium x1

These quantities may be revisited only through an explicit later balance decision.

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

Do not balance ordinary T1 survival around the assumption that every player has maximum Book Options on every supported enchantment.

High-level books remain valuable because they improve the common binomial probability distribution, but even a maximum-level book does not guarantee the maximum Book Option.

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

The Librarian may supply retained-book candidates at a 1% retention/availability chance applied to an eligible retained Enchanted Book trade candidate, not to all Librarian offers. A surviving offer costs a fixed effective 64 Emeralds and a Book, has one use per restock, and receives no vanilla reputation, cure, Hero of the Village, demand, or other price reduction. Removed-book variants remain excluded.

Removed Book variants in Section 4 must not remain available through Librarian trades.

Other profession status remains:

- Farmer: KEEP vanilla.
- Fisherman: separate custom-Fishing economy design; Vanilla / Custom Fishing presentation split remains an allowed direction.
- Fletcher: retain vanilla baseline; focused review later if needed.
- Cartographer: KEEP now; future Exploration/structure authority must explicitly review it.
- Cleric: KEEP now; future Brewing/Alchemy authority must explicitly review it.
- Armorer / Weaponsmith / Toolsmith: convert their generated equipment-sale offers to corresponding intact equipment + Emeralds -> moderately enchanted corresponding equipment. Preserve each affected equipment identity and the submitted item's legitimate progression state; Emeralds alone must not supply the equipment base. The implementation's broad conversion of equipment-result offers is accepted and supersedes the earlier final-sale-only wording. Emerald price 32 is the current provisional implementation value.
- Mason / Shepherd / Butcher / Leatherworker: KEEP vanilla unless a later concrete conflict appears.

---

## 8.1 Armorer / Weaponsmith / Toolsmith equipment-trade rule

The three equipment-selling smith professions use one shared final rule:

`corresponding fully intact finished equipment + Emeralds -> corresponding moderately enchanted equipment`

Authoritative constraints:

- the submitted equipment must be the corresponding finished item and must have no durability damage;
- the villager does not provide the equipment base from Emeralds alone;
- the returned item is the corresponding equipment with a moderate enchantment specification appropriate to the trade;
- preserve each converted offer's equipment identity rather than redesigning the equipment itself;
- the purpose is to make the villager an upgrade route for equipment the player already acquired, not a bypass around material/equipment progression;
- 32 Emeralds is the current provisional implementation price;
- exact moderate enchantment rolls/specification should remain compatible with the T1 enchantment authority and must not become a shortcut to theoretical/maximized T1 equipment.

This closes the Armorer / Weaponsmith / Toolsmith progression-bypass problem without redesigning their broader professions.

---

### 8.2 Enchanted loot scarcity direction

Naturally/world-supplied enchanted equipment and retained Enchanted Books must be **very, very rare** relative to vanilla-style ordinary availability.

The confirmed candidate-retention probability is **0.5% (0.005)** for otherwise-generated enchanted equipment and retained Enchanted Books. This is a filter on eligible enchanted candidates, not a probability applied to every loot entry.

- enchanted equipment from loot/world sources should have extremely low appearance probability;
- retained Enchanted Books from loot/world sources should likewise have extremely low appearance probability;
- source-by-source base generation weights and loot-table composition remain outside this retention rule.

The purpose is to prevent random loot from routinely bypassing the XP Enchantment progression or trivializing Book Option acquisition.

---

## 9. Remaining OPEN items

These are balance defaults selected for the current implementation. Keep them centralized and easy to retune; remaining balance openness does not block implementation:

- Book Option application: initial **200 raw XP**;
- Enchanted Book Combination: initial **100 raw XP per resulting book level**;
- final UI texture/icon/spacing polish beyond the confirmed workstation split and interaction direction;
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
- keep the selected provisional XP costs tunable rather than treating them as final balance;
- do not use these open details to block or contradict the confirmed workstation and progression rules.

This checkpoint preserves the Book Option core architecture while leaving only explicit balance, UI-detail, and remaining villager-integration decisions open.

---

## 11. Post-audit confirmed clarifications (2026-10-04)

These human-approved rules supersede conflicting OPEN statements above:

- A Book Option is allowed only when the equipment already contains the corresponding enchantment from primary XP progression. A retained book cannot grant it.
- The source book level changes the common binomial distribution only; it is not the rolled Option level.
- The 1% Librarian candidate rule applies to eligible retained-book trade candidates. Maximum use is one per restock. The effective price is fixed at 64 Emeralds despite vanilla discount and demand modifiers.
- Enchanted-loot candidate retention is 0.5%.
- Normal Anvil mode cannot transfer enchantments either from books or by equipment-to-equipment merging. Ordinary applicable non-enchantment operations remain.
- Successful Enchanted Book Combination is an Anvil use and follows vanilla Anvil degradation/destruction chance. Its own raw XP payment and one-book-per-input consumption happen once.
- Smith upgrade offers use the broader generated equipment-offer conversion described in section 8.1; unchanged or downgraded equipment is not a valid paid result.
- Book Option application costs 200 raw XP as a conservative provisional constant. Enchanted Book Combination costs 100 raw XP per resulting book level as a conservative provisional constant. Both values remain tunable.
- Damage-family Book Options (Sharpness, Smite, and Bane of Arthropods) add 0.25 damage per Option level on top of the normal enchantment.
- The Enhancement Stone recipe is R-D-R / D-L-D / R-D-R (Redstone, Diamond, Lapis Lazuli) and returns 16 stones.
- A premium-bearing enchantment node presents total XP, base XP, and additional-family premium separately so the premium is not mistaken as a second charge beyond the total.

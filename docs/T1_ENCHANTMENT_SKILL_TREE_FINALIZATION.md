# T1 Enchantment Skill Tree Finalization

Status: **CURRENT DESIGN AUTHORITY**

Baseline: `stage-19/checkpoint` @ `984418d302cf865b5654fac0a0dcb28449fb6118`

This document records the human-approved T1 enchantment progression redesign finalized after the Stage 19 progression audit.

The broader early-game / T1 / T2 survival progression identity is governed by `EARLY_T1_T2_SURVIVAL_PROGRESSION_LOOP_CHECKPOINT.md`.

It supersedes the acquisition/progression portions of `ENCHANTMENT_BRANCH_SKELETON.md` where they conflict. The retained custom-enchantment gameplay semantics in that skeleton remain authoritative unless this document explicitly changes them.

Implementation is **not** authorized merely by this document. It records design authority.

---

## 1. T1 progression identity

T1 is the mature vanilla-equipment stage.

The ordinary enchanting-table / random-enchantment acquisition model is replaced for T1 equipment progression by an **XP-funded per-equipment enchantment skill tree**.

The intended T1 loop is:

`ordinary Minecraft play -> XP -> Enhancement and Enchantment growth -> mature T1 equipment`

T1 does **not** require a dedicated T1 boss.

Ordinary Enhancement remains a parallel growth axis at `+0 -> +20`.

Enhancement Stone is **not** a rare progression gate or exploration hook. Its intended role is an easy-to-craft material cost/sink using ordinary existing resources. Exact recipe composition is outside this document.

Quest-based XP/resource supply is intentionally **DEFERRED** to a later dedicated design pass because the quest combinations require broader cross-system design. Vanilla XP sources remain valid; XP farms are not prohibited by this design.

---

## 2. Global skill-tree rules

1. Nodes are purchased with **actual accumulated XP**, not nominal player-level count.
2. Levels of the same enchantment are sequential: e.g. `Sharpness I -> II -> III -> IV -> V`.
3. Ordinary unrelated enchantments should not receive artificial prerequisite chains merely to make the tree look hierarchical.
4. With enough XP, a player may ultimately obtain all mechanically compatible nodes on an item. This is not a hard build-point-cap system.
5. Respec/reset is allowed in principle. Exact reset interaction and reset cost are not finalized here.
6. The durability branch is globally:
   `Unbreaking I -> Unbreaking II -> Unbreaking III -> Mending`.
7. The durability branch is deliberately expensive; Mending is a capstone-cost node.
8. Strong custom functional enchantments may branch from a related vanilla enchantment when the relationship is natural. Do not mechanically force every custom enchantment behind an unrelated prerequisite.
9. Actual mechanical incompatibility remains a hard structural exclusion.
10. Vanilla incompatibility by itself is **not** sufficient reason to prohibit a combination.

---

## 3. Compatibility model

Compatibility is divided into three classes.

### 3.1 Free stacking

Combinations whose historical vanilla exclusion is not required by the MoreMinecrafts design may coexist without an extra conflict unlock.

Explicitly approved examples:

- **Mending + Infinity: allowed**
- **Crossbow Piercing + Multishot: allowed**
- Bow ordinary compatible enchantments may coexist with the custom bow path except where a structural exclusion below applies.
- Depth Strider + Frost Walker may coexist unless implementation evidence proves a true mechanical impossibility.

### 3.2 Premium stacking

Very powerful overlapping families may coexist, but additional overlapping families require a large XP premium.

Approved families include:

- Sword-style damage overlap such as **Sharpness + Smite**.
- Armor protection overlap such as **Protection + Blast Protection** and the other protection-family combinations.

For the armor protection family, the approved premium baseline is:

- first protection family: normal node costs
- second protection family unlock premium: **+500 XP**
- third protection family unlock premium: **+1,000 XP**
- fourth protection family unlock premium: **+2,000 XP**

The premium depends on already owning another family; it does not require completing the first family to level IV before starting the next.

The same premium-stacking principle applies to other very high-impact overlapping families. Exact per-family premium values beyond the armor baseline may be tuned without converting them back into blanket incompatibilities.

### 3.3 Structural exclusion

If two mechanics cannot coherently operate together, they remain mutually exclusive.

Explicitly approved:

- Bow **Multi Shot <-> Laser Arrow**
- Crossbow **Crossbow Barrage <-> Multishot**
- Trident/Spear-style mechanics that are actually impossible to execute together remain structurally exclusive rather than being forced to stack.

Existing custom-enchantment structural exclusions that were not redesigned in this pass remain governed by `ENCHANTMENT_BRANCH_SKELETON.md`.

---

## 4. XP cost model

The economy is intentionally designed on the expensive side. Later progression balancing should primarily tune **XP supply**, rather than repeatedly making the skill tree cheaper.

### 4.1 Cost tiers

Approved relative tier structure:

| Tier | Typical role | Baseline XP |
| --- | --- | ---: |
| Minor | low ordinary growth | 100 |
| Standard | meaningful intermediate node | 200 |
| Major | high-level / strong node | 400 |
| Capstone | Mending / strongest functional nodes | 800+ |

Relative design target: approximately **1 : 2 : 4 : 8**.

### 4.2 Levelled ordinary-enchantment baseline

Approved representative five-level curve:

- level I: 100 XP
- level II: 150 XP
- level III: 200 XP
- level IV: 300 XP
- level V: 400 XP

Representative total for a five-level branch: **1,150 XP**.

Shorter ordinary branches should follow the same expensive-growth direction rather than being flattened to negligible costs.

### 4.3 Durability baseline

- Unbreaking I: **200 XP**
- Unbreaking II: **300 XP**
- Unbreaking III: **500 XP**
- Mending: **1,000 XP**

Total durability branch baseline: **2,000 XP**.

### 4.4 Custom functional nodes

Strong custom functions belong broadly in Major/Capstone pricing.

Approved examples from the design pass:

- Vein Mining I: about **500 XP**
- Vein Mining II: about **800 XP**
- Lumberjack / Auto Replant-class major convenience functions: broadly **500-800 XP**
- Feather Boots / Elytra Launch-class major movement or protection functions: broadly **800-1,000 XP**

These ranges express the approved expensive cost class. Where an exact number is not explicitly fixed above, implementation must not silently invent a final economy value if that value materially affects progression; it may remain a balance constant until the balancing task.

### 4.5 Economy control principle

The project intentionally accepts a high total T1 XP demand. Full T1 completion across combat equipment, armor, and tools may require many thousands to tens of thousands of XP.

XP supply is the primary later balancing lever:

- vanilla XP remains valid,
- future fixed one-time quest/challenge rewards may provide generous XP/resources,
- the future quest system is intended to connect Mining / Fishing / Exploration / Combat / Brewing / progression experiences to XP supply,
- daily quests, rerolls, and a quest-shop economy are **not** approved by this document.

Quest design itself remains deferred.

---

## 5. Approved equipment trees

### 5.1 Sword

Independent branches from START unless noted:

- Sharpness I -> II -> III -> IV -> V
- Smite I -> II -> III -> IV -> V
- Bane of Arthropods I -> II -> III -> IV -> V
- Sweeping Edge I -> II -> III
- Fire Aspect I -> II
- Knockback I -> II
- Unbreaking I -> II -> III -> Mending

The high-impact damage-family overlap follows **premium stacking**, not blanket vanilla mutual exclusion.

**Looting note:** Looting was not explicitly placed during this design pass. Do not invent a prerequisite relationship for it. If implementation requires its exact tree placement, treat that placement as a small authority gap to resolve rather than silently altering the approved tree.

### 5.2 Greatsword

For the current T1 pass, Greatsword is synchronized to the approved Sword enchantment tree. No unique Greatsword enchantment branch is added now.

### 5.3 Dagger

For the current T1 pass, Dagger is synchronized to the approved Sword enchantment tree. No unique Dagger enchantment branch is added now.

### 5.4 Axe

- Efficiency I -> II -> III -> IV -> V
- Efficiency III -> Lumberjack
- Fortune I -> II -> III
- Silk Touch
- Sharpness I -> II -> III -> IV -> V
- Smite I -> II -> III -> IV -> V
- Bane of Arthropods I -> II -> III -> IV -> V
- Unbreaking I -> II -> III -> Mending

Lumberjack requires **Efficiency III**.

### 5.5 Pickaxe

- Efficiency I -> II -> III -> IV -> V
- Efficiency III -> Vein Mining I -> Vein Mining II
- Fortune I -> II -> III
- Silk Touch
- Auto Smelt directly from START
- Unbreaking I -> II -> III -> Mending

Auto Smelt has **no prerequisite**.

### 5.6 Shovel

- Efficiency I -> II -> III -> IV -> V
- Fortune I -> II -> III
- Silk Touch
- Unbreaking I -> II -> III -> Mending

No custom Shovel enchantment is added in this pass.

### 5.7 Hoe

- Efficiency I -> II -> III -> IV -> V
- Efficiency III -> Auto Replant
- Fortune I -> II -> III
- Silk Touch
- Unbreaking I -> II -> III -> Mending

Auto Replant is retained as a vanilla-farming convenience enchantment. This does **not** reactivate Farming RPG as a progression axis.

### 5.8 Mace

- Density I -> II -> III -> IV -> V
- Breach I -> II -> III -> IV
- Wind Burst I -> II -> III
- Wind Burst II -> Explosive Mace
- Unbreaking I -> II -> III -> Mending

Explosive Mace requires **Wind Burst II**.

### 5.9 Spear

Spear is treated as a current vanilla equipment category, not as a custom substitute for another weapon.

- Sharpness I -> II -> III -> IV -> V
- Smite I -> II -> III -> IV -> V
- Bane of Arthropods I -> II -> III -> IV -> V
- Fire Aspect I -> II
- Knockback I -> II
- Looting I -> II -> III
- Lunge I -> II -> III
- Unbreaking I -> II -> III -> Mending

Lunge is independent and has no artificial prerequisite.

Mechanically impossible Spear combinations remain structurally exclusive.

### 5.10 Trident

- Impaling I -> II -> III -> IV -> V
- Loyalty I -> II -> III
- Riptide I -> II -> III
- Channeling
- Unbreaking I -> II -> III -> Mending

Rules:

- Trident combinations that are mechanically impossible to execute together remain **structural exclusions**.
- Do not relax a true movement/return/channeling mechanical conflict merely because the global compatibility policy relaxes balance-only vanilla exclusions.
- Ordinary compatible Trident branches remain governed by the global free/premium/structural compatibility model.

### 5.11 Bow

- Power I -> II -> III -> IV -> V
- Power III -> Multi Shot I -> Multi Shot II
- Power III -> Laser Arrow
- Punch I -> II
- Flame
- Infinity
- Unbreaking I -> II -> III -> Mending

Rules:

- **Multi Shot and Laser Arrow are structurally mutually exclusive.**
- Mending + Infinity is allowed.
- Multi Shot retains its current authority: level I = 3 arrows; level II = 5 arrows with narrower spread.
- Laser Arrow's implementation/resource/VFX task remains deferred even though its skill-tree placement is now fixed.

### 5.12 Crossbow

- Quick Charge I -> II -> III
- Quick Charge III -> Crossbow Barrage
- Piercing I -> II -> III -> IV
- Multishot
- Unbreaking I -> II -> III -> Mending

Rules:

- Crossbow Barrage requires **Quick Charge III**.
- Crossbow Barrage and Multishot are structurally mutually exclusive.
- Piercing + Multishot is allowed.
- Piercing + Crossbow Barrage is allowed.
- Crossbow Barrage retains its current gameplay authority: one charge produces a timed sequential 3-shot barrage; a full barrage consumes 3 ammunition and durability equivalent to 3 uses.

### 5.13 Helmet

- Protection I -> II -> III -> IV
- Fire Protection I -> II -> III -> IV
- Blast Protection I -> II -> III -> IV
- Projectile Protection I -> II -> III -> IV
- Respiration I -> II -> III
- Aqua Affinity
- Unbreaking I -> II -> III -> Mending

Respiration and Aqua Affinity are independent.

Protection families use premium stacking.

### 5.14 Chestplate

- Protection I -> II -> III -> IV
- Fire Protection I -> II -> III -> IV
- Blast Protection I -> II -> III -> IV
- Projectile Protection I -> II -> III -> IV
- Unbreaking I -> II -> III -> Mending

No custom Chestplate enchantment is added in this pass.

Backpack/chest attachment remains a separate equipment/utility system and is not an enchantment-tree node.

### 5.15 Leggings

- Protection I -> II -> III -> IV
- Fire Protection I -> II -> III -> IV
- Blast Protection I -> II -> III -> IV
- Projectile Protection I -> II -> III -> IV
- Swift Sneak I -> II -> III
- Unbreaking I -> II -> III -> Mending

No custom Leggings enchantment is added in this pass.

### 5.16 Boots

- Protection I -> II -> III -> IV
- Fire Protection I -> II -> III -> IV
- Blast Protection I -> II -> III -> IV
- Projectile Protection I -> II -> III -> IV
- Feather Falling I -> II -> III -> IV
- Feather Falling IV -> Feather Boots
- Depth Strider I -> II -> III
- Frost Walker I -> II
- Soul Speed I -> II -> III
- Unbreaking I -> II -> III -> Mending

Rules:

- Feather Boots requires **Feather Falling IV**.
- Depth Strider + Frost Walker is allowed unless a true runtime/mechanical impossibility is demonstrated.
- Feather Boots retains the gameplay semantics defined by `ENCHANTMENT_BRANCH_SKELETON.md`.

**Jet Boots note:** Jet Boots remains a retained enchantment in the current skeleton, but its exact placement in the new XP skill tree was not explicitly finalized in this design pass. Do not silently delete it or invent its prerequisite/cost placement.

### 5.17 Elytra

- Unbreaking I -> II -> III -> Mending
- Elytra Launch as an independent high-cost special node from START

Elytra Launch has no prerequisite. Its strength is controlled by its high XP cost rather than by an artificial durability prerequisite.

Its Shift + Jump gameplay semantics and input priority remain governed by `ENCHANTMENT_BRANCH_SKELETON.md`.

### 5.18 Shield

- Unbreaking I -> II -> III -> Mending

No custom Shield enchantment is added in this pass.

### 5.19 Fishing Rod

- Luck of the Sea I -> II -> III
- Lure I -> II -> III
- Unbreaking I -> II -> III -> Mending

Luck of the Sea + Lure is allowed.

Fishing Reel / Minnow / Crankbait are **not enchantment-tree nodes**. They remain part of the separate Fishing equipment/module system.

---

## 6. Items not fully placed by this pass

The following are deliberately not invented here:

- Gauntlet enchantment-tree identity/placement.
- Multi Catch's final Fishing integration and therefore its exact skill-tree placement.
- Exact persistence/binding semantics for the skill tree where implementation needs to distinguish physical-item state from any player/category-wide state.
- Exact reset/respec interaction and cost.
- Final per-node numeric costs where only a cost class/range was approved.
- Future Quest catalogue and reward amounts.

The initial Sword Looting, Chestplate Thorns, and Jet Boots placements are recorded as provisional implementation defaults in §10. They are not final placement authority. Gauntlet remains deferred as described in §10.3.

---

## 7. Superseded acquisition rule and Enchanted Book boundary

The old rule in `ENCHANTMENT_BRANCH_SKELETON.md` that random/generated Enchanted Books are a primary route for acquiring T1 enchantments is **superseded**.

The current primary route is the XP-funded equipment skill tree defined here.

The old `Enchanted Book + Anvil -> directly apply the enchantment to equipment` progression path must not remain as a bypass around the XP tree.

Enchanted Book items are not necessarily deleted as a class. The approved current checkpoint is `VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md`:

- eligible retained books may become a small **Book Option** optimization layer rather than granting the primary enchantment;
- one application establishes a fixed base Option;
- higher-level books may add an additional level bonus on top of that same base Option;
- changing/removing the applied Option requires a dedicated reset resource;
- books that cannot support a meaningful Option may have their itemized Enchanted Book variant removed;
- see the current human-approved roster, probability model, bonus values, reset resource, and Librarian policy in `VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md`.

Do not implement a separate enchantment shop, custom enchantment currency, random-book progression, or book-based XP-tree bypass as a substitute.

---

## 8. Villager integration boundary

Targeted changes to vanilla villager trading are **not** the same as promoting Trading into a new independent gameplay axis.

Current villager-trade status is governed by `VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md`.

Armorer / Weaponsmith / Toolsmith and Librarian integration is recorded in `VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md`, including its post-audit clarifications.

---

## 9. Implementation boundary

This authority is sufficient to implement the approved XP-tree framework and the explicitly placed equipment/enchantment nodes, subject to the genuinely unresolved items in section 6. The Book Option/villager checkpoint supplies current implementation authority for its retained roster, probability model, bonuses, reset, and economy integration. Its selected provisional costs are recorded there and in `T1Balance`; they remain tunable without blocking implementation.

Implementation must preserve:

- existing custom-enchantment gameplay semantics,
- structural exclusions,
- premium-stacking behavior,
- expensive XP progression,
- the separate Enhancement +0 -> +20 axis,
- separation of Fishing modules and Utility equipment from enchantment nodes,
- no direct Enchanted Book/anvil bypass around the XP skill tree.

Do not use implementation work to invent Quest content, T2 acquisition, unresolved custom-enchantment behavior, genuinely missing tree placements, or unrelated economy redesign. For a required open balance detail, use a conservative centralized default and document it. Confirmed Book Option, reset, and villager rules below are implementation authority, not permission to defer them.

---

## 10. Post-audit implementation clarifications (2026-10-04)

These clarifications are current. They supersede conflicting OPEN statements above only where explicitly stated. The T1 skill-tree costs, prerequisites, and compatibility rules otherwise remain as written.

### 10.1 Shared equipment growth axes

Applicable T2 equipment may use the same Enhancement, XP Enchantment, and Book Option axes as T1. Equipment is assigned a shared category profile explicitly; item-name suffixes do not confer an enchantment tree. Current T2 Sword, Bow, and Mace identities use their respective shared trees. Dark Energy Staff remains deferred with no assigned tree while its gameplay/Enhancement profile is unresolved.

### 10.2 Initial tree placement defaults

The first T1 implementation used these conservative, independently reviewable placement defaults:

- Sword Looting is available from START.
- Chestplate Thorns is available from START.
- Jet Boots is a three-step Boots branch at 800 / 1,000 / 1,200 XP, available from START.

These defaults explain the existing implementation and are not a new general placement rule. A later dedicated balance/tree pass may revise them.

### 10.3 Gauntlet known implementation defect / deferred authority gap

The current implementation assigns Gauntlet the Sword tree. Since Gauntlet has no ordinary durability semantics, this exposes Unbreaking and its Mending prerequisite even though their normal durability function does not apply. This mismatch is intentionally deferred in PR #68: do not remove Gauntlet progression or invent a replacement tree here. A later dedicated decision must establish Gauntlet's enchantment identity and tree, including the meaning of durability-oriented nodes.

### 10.4 Confirmed repair clarifications

- Premium costs must be shown as one total with a separate base and additional-family premium breakdown. For example, a 100 XP node plus a 500 XP premium is communicated as **Total: 600 XP; Base: 100 XP; Additional-family premium: 500 XP**.
- A Book Option requires the equipment to already own the corresponding enchantment through primary XP progression. The source book level shifts the common binomial distribution; it never becomes the Option result level.
- Sharpness, Smite, and Bane of Arthropods Options each add **0.25 damage per Option level** on top of ordinary enchantment behavior.
- Normal Anvil mode does not transfer enchantments from books or between equipment. Ordinary non-enchantment Anvil operations remain available. Enchanted Book renaming is not required by this clarification.
- Vein Mining keeps its per-block Level I and half-cost Level II durability rules; the Unbreaking Book Option does not reduce Vein Mining durability. The option remains active for ordinary applicable durability behavior and authorized Lumberjack behavior.
- The retained-book and natural enchanted-loot supply rules and provisional application/combination XP amounts are specified in the current Book Option/villager checkpoint.

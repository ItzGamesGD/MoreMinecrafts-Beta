# Villager Trade and Enchanted Book Integration Checkpoint

Status: **CURRENT DESIGN CHECKPOINT**

Baseline: `stage-19/checkpoint` @ `984418d302cf865b5654fac0a0dcb28449fb6118`

Working design line: `design/t1-enchantment-skill-tree`

This document records the human-approved cross-system decisions connecting the T1 XP enchantment redesign, Enchanted Books, and vanilla villager trading.

It is intentionally a **checkpoint**, not a claim that every villager trade or every Enchanted Book has been finalized. Confirmed rules are authoritative. Items explicitly marked OPEN remain design work and must not be invented during implementation.

---

## 1. System boundary

The primary T1 enchantment progression is the XP-funded equipment skill tree defined by `T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md`.

Therefore:

- an Enchanted Book must **not** directly grant or bypass a skill-tree enchantment;
- the old `Enchanted Book + Anvil -> enchantment applied to equipment` progression path is retired for the redesigned T1 system;
- combining/applying books through the old anvil path must not remain a hidden bypass around XP progression;
- Enhancement `+0 -> +20`, the XP enchantment skill tree, and the new Book Option layer are separate growth axes.

The intended role split is:

`Enhancement -> base-stat growth`

`XP Enchantment Skill Tree -> primary enchantment/function growth`

`Enchanted Book Option -> small optional optimization beyond the primary tree`

The Book Option layer must remain secondary. It must not become another route that replaces the XP skill tree.

---

## 2. Enchanted Book Option model — confirmed

Some Enchanted Books may be retained and repurposed as **Option materials** instead of direct enchantment-granting items.

### 2.1 One application establishes a fixed option

When a compatible retained Enchanted Book is applied as an Option:

- the application establishes the option in one step;
- the option's **base effect is fixed** once applied;
- the base option value does **not** scale into a separate ladder merely because the source book has a higher enchantment level;
- this is not a second Sharpness-I-to-V-style progression tree.

### 2.2 Higher-level books still matter

Book level is not meaningless.

A higher-level retained book grants an **additional level bonus on top of the same fixed base option**.

Conceptually:

`lower-level book -> fixed base Option + smaller/zero level bonus`

`higher-level book -> same fixed base Option + larger level bonus`

The exact base value, bonus formula, per-level increments, caps, and affected statistics are **OPEN** and must not be invented here.

### 2.3 Option replacement requires reset currency

An applied Book Option is not freely overwritten or rerolled.

To clear/change it:

`applied Option -> consume dedicated reset currency/material -> cleared/reset Option -> apply another eligible book`

Confirmed:

- a dedicated reset resource/currency is required to reset the Book Option;
- free replacement is not the design.

Still OPEN:

- exact reset item/currency identity;
- acquisition source;
- quantity/cost;
- whether reset removes only the Book Option or has any broader interaction.

### 2.4 Not every Enchanted Book must survive

The project will **not** preserve every book merely for compatibility with the old system.

Books whose effects cannot be converted into a sensible small Option may be removed as itemized Enchanted Book variants, while their enchantment/function remains available through the XP skill tree where applicable.

The exact classification is **not finalized yet**.

Next book-design pass must classify existing books into at least:

1. retained as a meaningful Book Option;
2. requires special redesign;
3. Enchanted Book item variant removed.

Do not decide this list during implementation.

---

## 3. Villager repeatability constraint

Villager breeding, restocking, and trading can create effectively repeatable supply.

Therefore villager balance must **not rely only on nominal rarity or a low initial trade appearance rate** as protection against progression bypass.

Any retained/reworked Librarian Enchanted Book trade and any future equipment trade must remain safe under repeatable village infrastructure.

This constraint is one reason the primary enchantment itself remains XP-tree-owned rather than book-owned.

The exact future Librarian supply table is still OPEN because the retained/deleted Book Option roster has not yet been classified.

---

## 4. Villager profession audit checkpoint

### 4.1 Farmer — KEEP

No current redesign required.

Retain the normal vanilla livelihood/food/economy role unless a later content system creates a concrete conflict.

### 4.2 Fisherman — SEPARATE ECONOMY DESIGN

The Fisherman problem should be solved inside the dedicated Fishing economy design rather than by forcing it into the general villager redesign.

Confirmed direction:

- custom MoreMinecrafts Fishing economy may be separated from ordinary vanilla Fishing trades;
- a villager trade presentation that separates **Vanilla / Custom Fishing** is an allowed design direction.

Not yet fixed:

- exact Fisherman trade list;
- whether the separation is implemented as explicit UI tabs/sections or another presentation;
- custom-fish valuation and sale/reward formulas.

The current Fishing authority remains responsible for exact custom-Fishing economy design.

### 4.3 Fletcher — PARTIAL REVIEW

Retain the vanilla role as the baseline.

A later focused trade-economy pass may review high-throughput emerald or combat-supply interactions, but no new Fletcher redesign is authorized by this checkpoint.

### 4.4 Librarian — BOOK ROLE REWORK, NOT AUTOMATIC FULL PROFESSION REPLACEMENT

The Librarian is **not** declared for wholesale replacement merely because direct book enchanting is removed.

Instead:

- direct Enchanted Book -> equipment enchantment application is blocked by the T1 redesign;
- meaningful retained books are redesigned through the Book Option model;
- book variants that cannot support that role may be removed;
- the Librarian's exact future Enchanted Book inventory/supply must be revisited after the book classification pass.

Do not treat the current Librarian book table as implementation-ready.

### 4.5 Cartographer — KEEP NOW, FUTURE CONTENT REVIEW HOOK

No current trade redesign.

Whenever new Exploration / structure / navigation content is promoted into current authority, its authority/design task must explicitly review whether Cartographer trades should be extended or changed.

This is a future review hook, not permission to redesign Cartographer now.

### 4.6 Cleric — KEEP NOW, FUTURE CONTENT REVIEW HOOK

No current trade redesign.

Whenever new Brewing / Alchemy / relevant supernatural-resource content is promoted into current authority, its authority/design task must explicitly review whether Cleric trades should be extended or changed.

This is a future review hook, not permission to redesign Cleric now.

### 4.7 Armorer / Weaponsmith / Toolsmith — CORE OPEN PROBLEM

These three professions are the highest-priority unresolved villager-trade problem.

Their direct equipment sales can bypass or distort the new T1 equipment-growth structure in the same architectural class as the old direct Enchanted Book bypass.

Confirmed:

- they require a dedicated solution before the villager/equipment progression design can be considered closed;
- the solution should be designed together as a shared equipment-trade problem rather than three unrelated patches.

OPEN:

- whether high-tier completed equipment is removed, replaced, restricted, transformed, or otherwise recontextualized;
- exact retained trade tables;
- how purchased equipment participates in Enhancement / XP Enchantment growth.

Do not guess these answers during implementation.

### 4.8 Mason / Shepherd / Butcher / Leatherworker — KEEP

No current redesign required.

Their vanilla material/food/decorative economy roles do not presently create a direct conflict with the T1 enchantment redesign.

---

## 5. Explicit non-decisions

This checkpoint does **not** finalize:

- the retained/deleted Enchanted Book roster;
- per-book Option statistics;
- Option slot count or whether different equipment categories receive different slot layouts;
- level-bonus formula;
- Book Option UI;
- reset-resource identity or economy;
- Librarian final inventory, weights, restock behavior, or pricing;
- Armorer / Weaponsmith / Toolsmith replacement model;
- Fisherman custom-economy values or final UI;
- Fletcher exact changes;
- Quest reward design;
- T2/T3/T4 progression.

---

## 6. Implementation stop conditions

Until the OPEN items above are separately finalized:

- do not restore direct book-to-enchantment application;
- do not make Enchanted Books an XP-skill-tree bypass;
- do not invent the Book Option roster or numeric effects;
- do not invent the reset currency;
- do not redesign unrelated villager professions;
- do not implement a final Librarian trade table;
- do not implement a final Armorer / Weaponsmith / Toolsmith solution.

The purpose of this checkpoint is to preserve the confirmed architecture while allowing the next design pass to classify books and solve equipment-selling villagers deliberately.

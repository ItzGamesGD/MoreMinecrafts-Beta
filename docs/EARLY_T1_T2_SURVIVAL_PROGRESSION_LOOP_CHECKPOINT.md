# Early / T1 / T2 Survival Progression Loop Checkpoint

Status: **CURRENT DESIGN CHECKPOINT**

Baseline: `stage-19/checkpoint` @ `984418d302cf865b5654fac0a0dcb28449fb6118`

This document records the human-approved high-level survival progression direction established after the Stage 19 progression audit.

It is intentionally limited to the early game, T1, and the high-level T2 acquisition direction. T3 is **not designed here**.

Confirmed architecture is authoritative. Exact recipes, loot weights, encounter tuning, Quest catalogue entries, and T3+ progression remain separate work.

---

## 1. Why this checkpoint exists

The Stage 19 audit found many implemented systems but weak connective progression:

- custom minerals existed without enough meaningful sinks;
- Fishing had a substantial gameplay loop but weak downstream reward use;
- T2 equipment existed without a complete survival acquisition path;
- Enhancement had no proper survival material source;
- custom combat/exploration did not yet close into a coherent growth loop.

The correction is **not** to make Enhancement Stone itself into a rare progression hook.

The intended high-level loop is:

`experience/content -> XP + useful resources -> equipment growth -> broader/harder experiences`

Enhancement Stone is only a supporting material cost inside that loop.

---

## 2. Early-game identity — vanilla remains primary

Early survival remains recognizably Minecraft.

Custom resources/options may begin to appear lightly from approximately the iron-era onward, but:

- they are optional side opportunities;
- they do not form a mandatory separate early-game technology tree;
- the player should not be forced to learn a large custom material economy before ordinary vanilla progression works;
- vanilla iron -> diamond / Nether / normal exploration remains the primary early progression language.

Titanium/Tungsten or other custom materials may support side choices where existing content already justifies them, but their existence must not turn the early game into a parallel RPG progression ladder.

---

## 3. T1 identity — play Minecraft and grow

T1 is the **mature vanilla-equipment stage**.

Its identity is:

> play normal Minecraft content, accumulate XP/resources, and grow the equipment you already use.

T1 does **not** require a special T1 boss or a mandatory dedicated dungeon gate.

### 3.1 Two parallel equipment-growth axes

T1 ordinary equipment growth has two primary axes:

1. **Enhancement**: `+0 -> +20`
2. **XP Enchantment Skill Tree**: the current authority in `T1_ENCHANTMENT_SKILL_TREE_FINALIZATION.md`

Neither replaces the other.

The same three equipment-growth axes are available to applicable T2 equipment: Enhancement, XP Enchantment progression, and Book Option. T2 items use an explicitly assigned shared equipment profile (such as Sword, Bow, or Mace); registry-name resemblance does not establish a profile. Dark Energy Staff remains exempt while its gameplay and Enhancement profile are deferred.

### 3.2 Enhancement Stone role

Enhancement Stone is a **cheap supporting consumable/material sink**, not a rare progression currency.

Confirmed direction:

- it should be easy to craft from ordinary/existing resources;
- its purpose is to make Enhancement consume material value as well as XP;
- it should not be a boss-exclusive drop, rare exploration key, or artificial gate merely to make Enhancement feel important.

Confirmed recipe (R = Redstone, D = Diamond, L = Lapis Lazuli):

```text
R D R
D L D
R D R
```

This crafts 16 Enhancement Stones from four Redstone, four Diamonds, and one Lapis Lazuli. No new ore or material is introduced.

### 3.3 XP is the main T1 progression currency

XP is the main growth resource for the T1 enchantment tree and also participates in Enhancement demand.

The system is intentionally allowed to be expensive.

The preferred balance strategy is:

> keep meaningful XP costs, then tune **XP supply** to make the overall loop satisfying.

Do not repeatedly trivialize node costs merely because total T1 XP demand becomes large.

Vanilla XP remains valid. XP farms are not prohibited by this design.

---

## 4. Future Quest role — supply/connective glue, not a new economy

A future Quest system is intended primarily to improve XP/resource **supply** and connect otherwise separate activities.

High-level approved philosophy:

- quests are fixed, one-time challenge/achievement-like goals;
- they recognize normal vanilla + MoreMinecrafts experiences;
- completion can grant generous XP and useful resources;
- examples may eventually cover milestones such as first iron/diamond progression, Nether entry, combat, exploration, Fishing grades, Brewing, and similar experiences;
- the system should reward actually playing the game's content rather than becoming a separate repetitive chore loop.

Not approved:

- daily quest rotations;
- reroll loops;
- a Quest shop/currency economy.

**Important:** the Quest catalogue, exact combinations, rewards, ordering, and implementation are intentionally **DEFERRED**. This checkpoint preserves only its intended role in the progression architecture.

---

## 5. T2 identity — go out and earn stronger elemental equipment

T2 changes the progression behavior.

T1 is primarily passive/natural growth from playing Minecraft.

T2 should require the player to **actively seek exploration and challenge content**.

The identity is:

> leave the ordinary growth loop, find meaningful challenges, earn elemental materials/rewards, and use them to obtain T2 equipment.

High-level flow:

`Exploration / Challenge -> Elemental Material or equivalent T2 acquisition reward -> T2 equipment`

### 5.1 Approved foundation directions

The current preferred T2 acquisition foundations are vanilla-adjacent content such as:

- Pillager-related content;
- Trial Chambers;
- valuable exploration chests;
- selectable harder Trials / wave-style challenges.

Elemental acquisition may use a combination of:

- chance-based chest/exploration rewards;
- guaranteed or higher-value rewards for deliberately selecting/completing harder Trial/wave content.

The old wave-challenge idea remains useful in this T2 role.

### 5.2 What is not finalized

This checkpoint does **not** yet define:

- exact Elemental Material item identities if new items are needed;
- exact loot tables;
- chest probabilities;
- Trial/wave selection UI;
- wave counts;
- difficulty scaling;
- guaranteed-reward thresholds;
- per-element source mapping;
- T2 recipes/material quantities;
- whether every T2 item shares the same acquisition source.

Those require a dedicated T2 acquisition design pass.

---

## 6. Tier identity summary

### Early game

**Vanilla-first, with light optional custom side choices.**

### T1

**Play Minecraft and grow.**

Normal activities feed XP/resources into Enhancement and the XP Enchantment Skill Tree.

### T2

**Go out and earn/claim stronger elemental equipment.**

Exploration and deliberate challenge become the acquisition gate.

### T3

**Not designed by this checkpoint.**

Do not extrapolate T2's structure into T3 or invent T3 progression merely to continue the sequence.

---

## 7. Cross-system consequences

This progression direction means:

- Mining/resources should serve actual downstream demand rather than creating content solely to justify minerals;
- Fishing, Exploration, Combat, Brewing, and future activities can feed XP/resource supply without each requiring its own independent currency;
- Enhancement Stone must remain a supporting consumable, not the center of the progression graph;
- Quest design is a future connective layer, not a prerequisite for implementing the already-approved T1 XP-tree architecture;
- T2 acquisition must eventually close the currently missing survival path for elemental equipment.

---

## 8. Implementation stop conditions

Do not use this checkpoint to invent:

- the Enhancement Stone recipe;
- Quest entries/rewards;
- exact T2 loot tables;
- exact wave/Trial mechanics;
- T2 recipe quantities;
- T3 progression.

Those are still design tasks.

Implementation may rely on the confirmed tier identities and progression boundaries only where the relevant detailed system authority is otherwise complete.

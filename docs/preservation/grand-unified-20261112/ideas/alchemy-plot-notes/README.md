# Alchemy / Brewing — Plot Notes

> **Status:** IDEA / NON-AUTHORITY / PLOT-LEVEL ONLY
>
> This file is only a lightweight parking place for future Alchemy/Brewing ideas.
> It is **not** current gameplay authority, is **not** implementation-ready, and must **not** be used to infer missing mechanics.
>
> Do not route `CONTENT_AUTHORITY_INDEX.md` here.
> Any future implementation requires a separate human-approved design pass and synchronization into the appropriate current-authority document.

## 1. Current intent

The previously broader Alchemy direction is not being restored as a complete standalone system.

However, Brewing should no longer be treated as something that must remain permanently discarded.

A future design pass may keep vanilla Brewing as the base and add a **small, understandable extension** around it.

The goal is to preserve the vanilla Minecraft feel rather than replace the Brewing Stand with a separate RPG-style crafting system.

## 2. Plot-level future direction

Possible direction to revisit later:

- keep the vanilla Brewing Stand and ordinary potion ecosystem recognizable,
- allow a limited form of **potion synthesis / potion combination**,
- explore ways for existing potions to be combined, refined, or transformed without creating a huge combinatorial matrix,
- keep the system readable enough that players can understand what they are making,
- prefer a small number of meaningful new interactions over a large list of arbitrary catalysts or hidden recipes.

These are idea-level directions only, not approved mechanics.

## 3. Explicitly unresolved

The following are intentionally **not designed yet**:

- exact synthesis rules,
- which potions may be combined,
- whether combination means effect fusion, duration/intensity tradeoff, transformation, or another model,
- ingredient/catalyst roster,
- recipe discovery UX,
- failure rules,
- limits on effect count,
- balance values,
- progression or acquisition requirements,
- whether any new potion effects are needed at all.

Do not fill these gaps during unrelated implementation work.

## 4. Scope guardrails

This note does **not** authorize:

- reactivating the old full Alchemy system,
- implementing potion synthesis immediately,
- changing current Brewing behavior,
- inventing new catalysts or potion effects,
- creating a separate Alchemy progression/economy axis,
- changing the Potion Injector contract.

The current Potion Injector remains a separate Utility Equipment feature and accepts ordinary drinkable Potion items according to its own approved design.

## 5. Promotion rule

If this direction is selected later:

1. run a dedicated Alchemy/Brewing design task,
2. decide the retained gameplay identity and smallest useful mechanic set,
3. resolve the explicit open questions above,
4. synchronize approved decisions into current authority,
5. only then begin implementation.

Until then, this folder remains idea storage only.

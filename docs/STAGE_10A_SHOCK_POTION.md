# Stage 10A — Shock Potion vertical slice

## Implementation status

Shock Potion is the first implemented Stage 10 Alchemy content slice. The other seven recovered
potions and every catalyst remain unimplemented. This slice uses Minecraft's registered potion and
status-effect identities; it does not add a custom item, screen, brewing engine, or entity tracker.

The Minecraft 26.3 brewing system is data-driven. The `minecraft:brewing` recipe maps a normal
`minecraft:thick` potion plus `minecraft:copper_ingot` to the registered `moreminecrafts:shock`
potion. Consequently the ordinary Brewing Stand owns fuel consumption, its per-bottle-slot recipe
checks, and all unrelated vanilla recipes. No recipe currently competes for Thick Potion plus
Copper Ingot.

## Effect contract and lifecycle

- Duration: **1,200 ticks (60 seconds)**.
- Pulse interval: **320 ticks (16 seconds)**, with the first pulse after one complete interval.
- Each pulse: **1.0 server-authoritative magic damage**, followed (if the target survives) by a
  **20-tick root**.
- Stacking: **refresh only**. Vanilla's single status-effect instance for the registered identity
  replaces/refreshes its remaining duration and therefore restarts the duration-derived pulse clock.
- Persistence and cleanup: both Shock and its short root use vanilla status-effect instances. Their
  durations serialize through normal entity persistence and expire/remove with the entity; there is
  no global runtime state to orphan.

The root is a private, short-lived status effect with a native movement-speed attribute modifier of
`-1.0 ADD_MULTIPLIED_TOTAL`. Minecraft installs and removes that modifier with the effect lifecycle,
so unrelated entities are untouched and normal movement returns when the 20 ticks expire.

The visible Shock effect uses fixed RGB **`#87CEEB` (sky blue)**. Minecraft derives the normal potion
liquid tint from its registered effect, so no bottle model, texture, or renderer is introduced.

## Validation status

Automated source compilation and resource JSON validation completed locally. The full build/unit
test and runtime smoke commands were attempted but dependency resolution was blocked by HTTP 403
responses from the configured Maven endpoints in this environment. Automated results do not change
the gameplay status below: **LIVE PENDING**.

Use `/mmtest give shock` as an operator to receive three Thick Potions, Copper Ingots, and one ready
Shock Potion. The Thick Potions are intentionally included so the actual Brewing Stand path can be
tested independently of the ready-made direct-effect sample. `/mmtest give all` includes this kit.

### Shock Potion — LIVE PENDING

- [ ] Thick Potion + Copper Ingot brews Shock Potion.
- [ ] One valid bottle brews correctly.
- [ ] Two valid bottles brew correctly.
- [ ] Three valid bottles brew correctly.
- [ ] The potion has the sky-blue vanilla bottle presentation.
- [ ] The effect lasts 1,200 ticks / 60 seconds.
- [ ] A pulse occurs at the intended 320-tick interval.
- [ ] A pulse deals exactly 1.0 damage.
- [ ] A pulse roots movement for 20 ticks.
- [ ] Movement returns normally afterward.
- [ ] Reapplication refreshes rather than stacks.
- [ ] Death and entity removal leave no effect state behind.
- [ ] Normal save/reload preserves the active effect and its remaining-duration pulse clock.
- [ ] Unrelated vanilla Brewing Stand recipes still work.

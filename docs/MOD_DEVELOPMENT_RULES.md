# MoreMinecrafts Mod Development Rules

This document defines the default architecture and development/testing workflow for MoreMinecrafts-Beta.

## 1. Product shape: One Mod, Many Modules

MoreMinecrafts ships as one integrated Fabric mod JAR by default.

Logical content domains stay separated in code:

- core
- equipment
- enchantment
- fishing
- mobs
- alchemy
- world
- client
- dev

Do not split these into separate independently built mods unless there is a concrete reason later, such as independent installation, release cadence, or reuse.

## 2. Build strategy

### Foundation is built horizontally

Shared infrastructure comes first:

- registry and IDs
- data components
- networking
- keybind/input routing
- cooldown/state handling
- combat hooks
- save/sync
- common VFX hooks
- world hooks
- reusable content runtime

The goal is to establish a stable mod-native engine before mass content migration.

### Content is built vertically

After the shared foundation is stable, complete one content branch from start to finish before spreading work across many half-finished systems.

Preferred pattern:

1. register content
2. implement runtime logic
3. persistence/sync
4. interaction/input
5. automated tests
6. in-game validation
7. polish

Avoid keeping many content families at 10-20% completion for long periods.

## 3. Migration rule

The final Paper plugin is a reference implementation and behavior specification, not code to port line by line.

For each migrated feature:

1. identify the gameplay contract from Paper
2. remove Paper-specific assumptions
3. implement the contract using Fabric/Minecraft-native systems
4. validate behavior
5. only then add mod-only improvements

## 4. Placeholder asset rule

Gameplay implementation comes before final art.

During early implementation, it is acceptable and preferred to use:

- vanilla item models
- vanilla entity models
- placeholder textures/icons
- temporary names or simple presentation

Do not block gameplay implementation on finished 3D models, animation, VFX, sound, or UI polish.

Preferred order:

behavior -> validation -> custom model/animation/VFX/sound/UI polish

## 5. Development harness is a first-class module

Maintain a dedicated dev/test surface instead of relying only on normal progression.

Recommended logical area:

```
dev/
  commands/
  gametest/
  debug/
  test-world/
```

The dev layer may provide:

- test commands
- direct item/equipment grants
- mob spawning
- skill/effect triggering
- debug state inspection
- deterministic GameTests
- test-world helpers

Development utilities must be isolated so they can be disabled or excluded from production behavior if needed.

## 6. Testing hierarchy

Use the cheapest suitable test layer first.

### A. JUnit for pure logic

Use ordinary unit tests for logic that does not require a running Minecraft instance.

Examples:

- damage formulas
- cooldown math
- coating rules
- fishing probability/grade logic
- target selection
- state machines
- deterministic progression rules

### B. GameTest for Minecraft behavior

Use Minecraft/Fabric GameTest when behavior depends on an actual game environment.

Examples:

- entity spawning
- block interaction
- item use
- combat effects
- status application
- inventory behavior
- world interaction
- tick-dependent gameplay

Prefer deterministic automated tests over repeated manual reproduction.

### C. runClient/runServer for visual and integration validation

Use Loom development runs for:

- UI
- rendering
- particles/VFX
- animation
- sound
- input feel
- full integration
- manual gameplay validation

The development instance is the normal place for interactive testing. Do not copy built JARs into a normal launcher instance for every iteration.

## 7. HotSwap rule

When the development client/server is already running, use JVM/IDE HotSwap where practical for method-body changes.

Good candidates:

- constants
- formulas
- conditions
- particle counts
- AI calculations
- internal method logic

Do not assume HotSwap is valid for structural changes.

Restart the development process when changing things such as:

- registry entries
- new or removed fields/methods
- method signatures
- inheritance
- mixins
- initialization/bootstrap structure
- networking packet schemas
- other class-structure changes

## 8. Restart policy

Minecraft restarts are not the default test loop.

Preferred loop:

```
code change
  -> JUnit when possible
  -> GameTest when Minecraft behavior is required
  -> HotSwap into an existing dev run when safe
  -> restart runClient/runServer only for structural/bootstrap changes
```

The goal is to minimize full restarts while keeping validation reliable.

## 9. Initial vertical slice

Before mass migration, complete at least one representative gameplay feature end to end through the new mod architecture.

That slice should prove:

- registration
- runtime logic
- data/state
- persistence/sync where relevant
- input/interaction
- testing
- in-game validation

Only after this path is proven should migration scale outward.

## 10. Default priority

When there is tension between speed and presentation, prioritize in this order:

1. architecture correctness
2. gameplay behavior
3. automated validation
4. integration usability
5. art and polish

This document is the default rule set for Mod Stage development unless a later explicit design decision supersedes it.

# Owner decision — Territory system shelved, constituent ideas released (2026-10-21)

**Status: OWNER-APPROVED SCOPE CHANGE — TERRITORY SYSTEM INDEFINITELY SHELVED / NOT ON ACTIVE IMPLEMENTATION ROADMAP.**

This is the latest owner decision and supersedes the *implementation authorization/active priority* of territory decisions D01–D84, without erasing their historical design content. **Do not implement territory registration, ownership, tier progression, material deposits, automatic settlement retrofits, protection, or territory-linked facility unlocks unless the owner explicitly reactivates the system.** Earlier documents saying OWNER-APPROVED describe their historical design status, not an active instruction to build.

## Owner's rationale

1. MoreMinecrafts is a broad Minecraft content mod, not a village-centric mod.
2. Attractive independent features became an overly interconnected management and resource-payment burden when combined.
3. Territory upgrades offered too little directly perceptible gameplay change relative to the design and implementation complexity; trade unlocks alone do not justify the full system.
4. Automatic remodeling/placement near established player activity areas introduces high safety and implementation risk.

This is a **provisional suspension**, not a demand to delete historical decisions or ban villages from the mod.

## What is paused

- T0–T3 territory registration/ownership/progression, bell Text Display + Interaction payment and D79/D83 material recipes.
- Mandatory territory upgrades, territory-protection entitlement, territory-specific facility construction and grading, and territory-dependent unlock dependencies.
- The D01–D84 owner-decision chain as an *active delivery specification*; all remain archived reference material.
- Do not turn previously planned territory QA, code, or docs into an active prerequisite for unrelated MM content.

## Released ideas: independent candidate tracks (NOT blanket implementation approval)

| Idea from territory design | Independent future track | Status |
|---|---|---|
| Guards / mercenaries | Standalone NPC, guards or hireable companions without territory ownership prerequisite | CANDIDATE; existing independent approved mercenary decisions elsewhere remain subject to their own authority |
| Village appearance variety | Vanilla village visual improvements, preferably safe world-generation-time variants | CANDIDATE |
| Specialized workshops | Additional structures or points of interest with meaningful direct gameplay | CANDIDATE |
| Housing and settlement variety | New village structures and settlement generation | CANDIDATE |
| Special trading and economy | Independent trade/economy progression not gated by territory tiers | CANDIDATE |
| Biome-specific settlements | Separate new village/settlement structures rather than retrofitting player-occupied vanilla villages | CANDIDATE |
| Automated terrain preparation and road placement | Reuse only when a future structure design specifically needs it and has its own safety validation | DEFERRED TECHNIQUE |

**Explicit design separation:** (A) improvements to vanilla villages and (B) wholly new village/settlement structures are **different future design tracks**. Neither requires player ownership, territory registration, tier upgrades or a village-center hub.

These candidates do not inherit D01–D84 costs, protection, buildings-per-tier, guard-per-tier or other territory coupling. Re-adoption requires an independent design decision. Avoid automatically merging all of these into another monolithic village management system.

## Documentation and branch handling

- Preserve earlier Draft PRs #131–#137 and their docs as historical work; this decision is newer and takes precedence for active scope.
- Keep this PR Draft, docs-only, stacked on #137. Do not merge, close or mark Ready on earlier PRs; do not change main.
- Implementation/phase plans, authority matrices and task prompts that still list territory as active must be interpreted as **SHELVED** pending explicit roadmap reconciliation. Future implementers should check this file before working on territory.
- No gameplay code, worldgen assets, tests or workflows are authorized by this change.

## Re-entry criteria if the owner revisits the territory system

Require an explicit owner decision to reactivate. Before implementation, reassess (a) player-visible gameplay benefit, (b) independence of feature components, (c) placement/world integrity and multiplayer safety, and (d) total development/maintenance burden. Do not treat archived D01–D84 as automatic re-approval.

## Next planning step (not yet decided)

Consider a **structure-content roadmap** with two separate sections: vanilla village variants/improvements and brand-new settlement structures, while treating guard/mercenary, workshop and trade concepts as independent modules. Prioritize player-visible discovery and interaction rather than mandatory settlement management.

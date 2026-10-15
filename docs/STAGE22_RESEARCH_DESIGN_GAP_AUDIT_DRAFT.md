# Stage 22 — Research design gap audit (working draft, NOT authority)

Checked against `docs/CONTENT_AUTHORITY_INDEX.md`, current research V4 execution authority, and the V3 research ledger on `stage-22/research-restructure`.

## User-approved changes awaiting authority synchronization
- Seven disciplines have a **purchased branch-root research node**, each granting a small passive bonus. Existing combat/ecology roots remain.
- Add five roots: resources (mining XP +1%), production (smelting XP +1%), living (crop growth speed +1%), world (movement speed +1%), alchemy (potion duration +1%). These are agreed candidate effects; implementation semantics and rounding must be validated.
- Same baseline root price as existing two: 80 raw XP, two discipline-appropriate common materials of 16 each. Exact material identities for five new roots are **not approved**; do not invent them in implementation.
- Purchasing a discipline root is a prerequisite for all other paid research in that discipline. Other research-to-research edges only where meaningful; sibling branches must not require one another merely for ordering.
- Knowledge prerequisites: generally one, more than one permitted when justified. Preserve the documented knowledge/research thematic mapping.
- Derived *provisional* totals: 92 nodes, 200 level purchases, 147,670 raw XP, conditional on exactly five 80-XP single-level roots and no other ledger changes.
- **Do not treat this audit as authority or merge authorization.** Sync into current authority only after resolving missing exact design choices.

## Confirmed design gaps — require user decisions, not implementation guesses
1. **V3 prerequisite topology:** all 70 `research_v3.json` entries have `prerequisitesDefined=false`; the 70 knowledge mapping exists in the approved research document §8 but exact per-node knowledge sets and research edges have not been ratified.
2. **New root costs:** five pairs of 16 materials are unspecified. Root knowledge-entry conditions also need a coherent rule: prior combat/ecology roots used any-of discovery and grandfathering.
3. **Root bonus edge semantics:** mining XP and smelting XP may be zero or fractional, crop growth ticks are stochastic, potion duration rounds to ticks, movement speed interacts with other modifiers; no arbitrary gameplay outcomes or unapproved compensating buffs.
4. **Tactical Shooting T3:** special arrows/firework ammunition kinds, effects, recipes, and art are deferred; rope arrow also deferred by latest V4 authority.
5. **Counter-shield:** recipe still unresolved (core effect specified).
6. **Motorized Boat:** intermediate speed/fuel table and engine craft/attachment rules unresolved; endpoints are only proposed.
7. **Piglin Trading V4:** transaction offerings, prices and interactions with T1 bartering are unresolved.
8. **Cross-dimensional item transport:** crash/full-storage/missing-bed delivery policy and feasibility remain conditional; abandon rather than risk duplication.
9. **Potion Mixing/Concentration:** compatible potion whitelist, effect restrictions, and edge cases require audit.
10. **Mob taxonomy:** exact overlapping species tags for ecology bonuses and 40 custom mobs require reconciliation; knowledge bonuses must not double-count.
11. **Research-specific equipment and devices:** recipes and transaction safety remain unresolved in places (e.g. sorting precedence, auto farm exceptional crops/unload/full inventory, filter details, tool capture persistent state). Some items are implementation validation rather than missing gameplay design.
12. **Research economy:** V3 costs are approved/locked; do not mistake older historical temporary-cost notes for a current mandate to rebalance. New five roots add only explicitly agreed prices.

## Not design gaps
- T1/T2/T3 are labels, **not** mandatory linear purchase tiers (latest V4 override).
- The 70 V3 XP/material prices and 17 V4 XP/material prices are approved; no wholesale repricing.
- The 70 knowledge-to-research thematic mapping is documented (§8), although exact purchase dependency sets remain open.
- The latest V4 decisions specify aggro redirection 30%/12 blocks/200 ticks, mob capture 30 durability/nonboss individual-state persistence, wall movement input windows and fall roll; older PENDING notes are superseded.
- UI authority lives in the separate UI integration documents; old selector/UI statements in the research history are not current authority.

## Safe implementation sequence
1. Reconcile and approve missing root materials, root entry knowledge rules, exact 70-node prerequisite table, and high-impact content gaps.
2. Synchronize approved decisions into current authority (do not promote draft proposals).
3. Implement five roots, bonus behavior, complete prerequisites, server-side purchase gates, migration/grandfathering, localization and UI.
4. Run unit/GameTests, Gradle build, and client visual validation where available. Keep PR #88 open/unmerged until separately approved.

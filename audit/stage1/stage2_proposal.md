# Stage 2 proposal — ranked work packages

This is a proposal only. No Stage 2 work was started. Each boundary below is one suggested reviewable PR, built on current integration ancestry. Costs, IDs and baseline vanilla behavior remain fixed.

| Rank / PR boundary | Nodes | Required work and dependencies | Complexity / regression risk | Focused validation | Independence |
|---|---|---|---|---|---|
| 1. Knowledge collectors | K11, K36, K40, K48, K59, K63–66, K69–70 | Add server-observed smelting, shield, ammunition, barter, brushing and brewing events; retain current counter/persistence API. K51 stays design-blocked. | HIGH / MEDIUM | GameTests for each event, threshold, reload and sync | Independent of V3 graph |
| 2. Condition correctness | K5, K8, K12, K14, K18, K19, K22, K23, K25, K30, K34, K35, K38 | Correct distinctness, large-tree provenance, success-only interactions and projectile source; inspect stat-to-install semantics. | MEDIUM / MEDIUM | False-positive and real-survival event cases | Independent of rewards |
| 3. Knowledge rewards | K1–70 except unresolved K51 | Apply approved small bonuses and information unlocks; create shared ecology +5% matching once taxonomy is decided; no invented buffs for info rewards. | HIGH / HIGH | Reward-on/reward-off GameTests, stacking and reconnect | Depends on info/taxonomy decisions for relevant subsets |
| 4. Recipe gating | K4, K14, K15, K17, K18, K19, K22, K25, K30, K42, K44, K57, K60 | Validate all crafting grids and server output paths for 13 alternatives, while preserving vanilla recipes. | MEDIUM / HIGH | 2×2, 3×3, recipe book, automation, locked/unlocked cases | Independent of V3 graph |
| 5. V4 existing-system integration | `throwable_pickaxe`, `nearby_chest_ingredients`, `hopper_filter`, `mob_capture`, `piglin_barter` | Audit stand-alone systems and connect approved purchased-state gates or improvements; #51 blocks ordinary `mob_capture` access until defined. | HIGH / HIGH | Before/after purchase and denial GameTests, client for GUI/device actions | Each subfeature separable; capture depends on K51 |
| 6. V4 new effects | `ecology_root`, `automatic_tool_switch`, `shield_counter`, `ballistic_control`, `bow_charge_mobility`, `falling_axe_smash`, `weapon_swap_handling`, `mob_aggro_redirection` | Implement real state-gated consumers corresponding to approved behavior; reuse shared combat/mob handlers. | HIGH / HIGH | Direct effect and negative gating tests per node | Can split combat, ecology, production |
| 7. V4 effect verification | `combat_root`, `climbing`, `wall_jump`, `fall_roll` | Verify server authority, trigger semantics, client input and actual damage/movement outcomes. | MEDIUM / MEDIUM | GameTests plus real-client movement checks | Independent of V3 graph |
| 8. V3 design approval | V3 01–70 | Resolve prerequisite graph and any still incomplete effect specifications using latest design authority. Preserve `prerequisitesDefined=false` until approved. | HIGH design / HIGH risk if rushed | Design review; graph and cost consistency checks | Must precede V3 purchase enablement |
| 9. V3 effects and gates | V3 01–70 in discipline-sized PRs | Add level-aware consumers, reuse existing machines, fishing, equipment, brewing and combat code without duplicating baseline gameplay. Enable purchases only for approved defined edges. | HIGH / HIGH | Per-level effect and negative gate GameTests, economic regressions | Parallel by discipline after rank 8 |
| 10. Layout relationship pass | Affected Research branches | Adjust only after actual gameplay dependencies are approved; keep seven official disciplines and current screen behavior. | MEDIUM / MEDIUM | Layout coverage, UI traversal and GUI scales | Depends on rank 8 |

## Explicit user decisions before implementation

1. Approve the 70-node V3 Knowledge/Research prerequisite graph, including which nodes remain unavailable until later design. Confirm that branch order is visual only.
2. Set Knowledge #51's rare species list and ecology family membership/overlap for all 12 +5% damage bonuses.
3. Confirm the exact information/encyclopedia presentation and content for nonnumeric Knowledge rewards.
4. Resolve remaining V3 effect details marked follow-up in the latest design document, particularly mechanisms and edge cases; do not derive them from names.
5. Confirm any purchase gate relationship to existing stand-alone devices/functions where the design currently leaves baseline use ambiguous.

Recommended first coding PR: rank 1 event collectors, excluding K51, with direct threshold and persistence tests. This recommendation is not implementation authorization.

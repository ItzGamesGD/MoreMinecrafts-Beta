# Stage 23-A Research Implementation Manifest

Date: 2026-10-15  
Implementation branch: `codex/stage23a-research-implementation-20261015`  
Base: PR #89 head `53c2374e84356ff3b5ccaa84df26897dd7ce4b5b`  
Scope: publish this manifest before gameplay changes. Stage 22 PRs #88/#89 and the Stage 23-B design branch are outside this branch's write scope.

## Basis and authority

The source for implementation evidence is `docs/RESEARCH_STAGE22_87_NODE_IMPLEMENTATION_AUDIT_2026-10-15.md`. That audit reports 87 baseline nodes: 70 V3 nodes, two root nodes included in its roster, and 15 other V4 nodes. Its LOW/MEDIUM/HIGH remaining-work ratings are not the architectural classes below.

Current gameplay authority is `docs/RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, including the approved V3 effects and per-level scaling. The Stage 22 graph, paid-root gate, migration, and save rules are in `docs/RESEARCH_STAGE22_SEVEN_ROOT_COMPLETION_AUTHORITY_2026-10-15.md`. Selected nodes already have catalog entries, prices, level limits, prerequisites, and purchase handling on the base branch; this batch adds runtime effects and focused verification without changing those records.

## Architectural classification of the audited roster

M1 means an existing event/callback/mixin/handler extension without substantial new persistent state. M2 means a focused item, recipe, equipment modifier, or interaction without a large persistent subsystem. H1 means a stateful device, inventory, processing machine, or substantial persistent device state. H2 means advanced combat, transport, custom UI, or a multi-component system. BLOCKED means explicitly deferred or missing an essential approved gameplay decision.

| Class | Exact audited IDs |
|---|---|
| M1 | `combat_root`, `ecology_root`; `v3.research.01`, `.02`, `.03`, `.07`, `.14`, `.15`, `.17`, `.18`, `.19`, `.23`, `.27`, `.30`, `.31`, `.32`, `.36`, `.37`, `.38`, `.39`, `.40`, `.41`, `.42`, `.43`, `.44`, `.45`, `.46`, `.47`, `.50`, `.52`, `.54`, `.55`, `.59`, `.60`, `.62`, `.63`, `.64`, `.66`, `.67`; `climbing`, `ballistic_control`, `piglin_barter` |
| M2 | `v3.research.04`, `.09`, `.10`; `throwable_pickaxe`, `automatic_tool_switch` |
| H1 | `v3.research.05`, `.06`, `.11`, `.12`, `.16`, `.24`, `.25`, `.26`, `.69`; `hopper_filter` |
| H2 | `v3.research.08`, `.13`, `.20`, `.21`, `.22`, `.28`, `.29`, `.33`, `.34`, `.48`, `.49`, `.51`, `.53`, `.56`, `.57`, `.61`, `.68`, `.70`; `nearby_chest_ingredients`, `shield_counter`, `bow_charge_mobility`, `falling_axe_smash`, `weapon_swap_handling`, `mob_aggro_redirection`, `mob_capture`, `wall_jump`, `fall_roll` |
| BLOCKED | `v3.research.35` (Tactical Shooting is explicitly deferred), `v3.research.58` (cross-dimensional delivery requires the separately approved durable transactional system), `v3.research.65` (eligible catalyst set/meaning is not fully specified) |

Count check: 42 M1 + 5 M2 + 10 H1 + 27 H2 + 3 BLOCKED = 87 audited nodes. Existing partial handlers do not make a node a new Stage 23-A selection. The seven reserved Stage 23-B IDs are excluded from selection: `shield_counter`, `ballistic_control`, `bow_charge_mobility`, `falling_axe_smash`, `weapon_swap_handling`, `mob_aggro_redirection`, and `hopper_filter`.

## Selected implementation ownership

Six M1 nodes meet the scope and have complete approved rules with a focused server-side implementation path. Their purchase levels remain authoritative; handlers query paid level at the event and do not apply or persist a one-time bonus.

| Research ID | Approved effect | Hook and intended files | Authority | Focused verification |
|---|---|---|---|---|
| `v3.research.02` | Lumbering, levels 1–5: independent extra-log roll at 10/20/20/30/30%; maximum extra logs 1/1/2/2/3, uniformly selected from 1 through the level maximum. Extra drops only; no additional block breaks or duplicate chain logging. | Existing server block-drop path in `mixin/BlockMixin.java`; shared reward lookup in `progression/ResearchRewards.java`; pure chance/count rules in a Stage 23-A effect-rules helper. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, Resource Studies, Lumbering. | Test levels 0–5, chance boundaries and maximum counts; verify only log drops are eligible, no extra blocks are removed, and the chain-logging path does not recursively create bonus drops. Verify independent players and unpurchased gating. |
| `v3.research.03` | Mineral Conservation, levels 1–5: 5% per level (maximum 25%) independent prevention check for mining-tool durability consumption; it must not double-consume damage. | Bracket server block-break tool damage in existing `mixin/ServerPlayerGameModeMixin.java`; apply the paid-level check in existing `mixin/ItemStackMixin.java`; shared reward lookup and pure rule helper. The transient break context is thread-local and always cleared; no saved state. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, Resource Studies, Mineral Conservation. | Test 0–25% boundaries, zero/nonzero damage, mining-only scope, independent application alongside Unbreaking, and no change to combat/anvil/non-mining durability. Verify canceled/failed breaks clear context and unpurchased players are unchanged. |
| `v3.research.18` | Beekeeping, levels 1–3: add one honeycomb on a 20/40/60% chance at honeycomb harvest; one harvest activation only. | New server hook `mixin/ResearchBeehiveHarvestMixin.java`; shared reward lookup and pure rule helper; register the hook in `src/main/resources/moreminecrafts.mixins.json`. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, Living Technologies, Beekeeping. | Test 0–3 levels and chance boundaries; verify only honeycomb harvesting can award it, one activation yields at most one extra comb, failed/blocked harvesting consumes no extra item, and the recipient's paid level controls the effect. |
| `v3.research.55` | Survival & Expedition, levels 1–3: reduce sprinting and swimming hunger use by 8/16/24%; leave all other exhaustion sources unchanged. | New server-side exhaustion hook(s) at the existing sprint and swim exhaustion call sites, with a shared purchase-level/rule helper; register only any new mixin in `moreminecrafts.mixins.json`. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, World research effects, Survival & Expedition. | Test 0–3 levels at both sprint and swim call sites; verify the exact multiplier, no effect on other exhaustion causes, and separate-player/purchase gating. |
| `v3.research.59` | Brewing Basics, levels 1–3: brewing time reduced by 15/30/45%. | New server hook on vanilla `BrewingStandBlockEntity` brew-time processing; shared reward lookup and pure timer rule; register in `moreminecrafts.mixins.json`. No custom inventory or new persisted timer field. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, Alchemy research effects, Brewing Basics. | Test 0–3 levels against vanilla duration, including exact resulting tick counts, pause/resume, invalid/blocked brew conditions, and no acceleration for unpurchased players. Confirm existing brewing state remains readable. |
| `v3.research.67` | XP Control, levels 1–2: increase XP pickup radius to 4/6 blocks; no XP redistribution, splitting, or assignment. | Extend existing `mixin/ResearchExperienceOrbMixin.java` pickup handling; shared reward lookup and pure radius rule. Keep the existing orb-value/source attribution path separate. | `RESEARCH_KNOWLEDGE_TIER_DESIGN_2026-10-08.md`, Alchemy research effects, XP Control. | Test vanilla radius, 4- and 6-block boundaries, level gating, pickup without changing orb value, no redistribution, and two nearby players with different purchase levels. |

## File ownership and shared infrastructure

Stage 23-A owns the six effects above and their focused test additions. Expected shared files are `progression/ResearchRewards.java`, a small stateless rules helper under `progression/`, and `src/main/resources/moreminecrafts.mixins.json` only for new mixin registrations. Expected runtime adapters are `BlockMixin.java`, `ServerPlayerGameModeMixin.java`, `ItemStackMixin.java`, `ResearchExperienceOrbMixin.java`, and the new beehive, sprint/swim exhaustion, and brewing-time mixins described above. Expected tests are a focused Stage 23-A rules test plus node-specific hook/GameTests under existing test source sets.

No change is authorized to V3/V4 IDs, prices, prerequisites, catalog topology, progression attachment schema, root migration, or any unrelated authority document. No Stage 23-B research ID is selected. Shared mixin files must retain Stage 22 behavior and every existing purchase gate; all temporary context must be cleared on cancellation and exceptions. Stage 23-A owns its additions in shared source files; it does not claim ownership of Stage 23-B design or implementation files. Any discovered overlapping Stage 23-B file requirement is a stop-and-report condition before editing that file.

No gameplay files have been changed as part of preparing this manifest. Manifest publication and commit must precede all runtime or test-source changes.

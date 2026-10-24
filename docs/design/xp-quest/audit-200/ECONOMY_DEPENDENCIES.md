# Global pacing, XP economy, redundancy and dependency map

Source: Q1-C catalog at `17e980912d636b0572a3f25b24995ffde31c754c` (blob `95ddc0bf15d381e6694dd49ec774f4d04650e48c`). These are ledger sums, not earned-player XP forecasts. No completion times were measured.

## Ledger reconciliation

| Dimension | Group | Quests | Raw XP | Registered |
|---|---|---:|---:|---:|
| Tab | exploration | 24 | 21,500 | 9 |
| Tab | dimension | 10 | 12,600 | 8 |
| Tab | combat | 30 | 22,950 | 25 |
| Tab | boss | 9 | 21,400 | 7 |
| Tab | mining | 19 | 15,800 | 11 |
| Tab | farming | 19 | 12,450 | 3 |
| Tab | fishing | 16 | 13,100 | 3 |
| Tab | crafting | 15 | 12,100 | 2 |
| Tab | building | 8 | 7,300 | 6 |
| Tab | alchemy | 9 | 6,000 | 1 |
| Tab | equipment | 20 | 38,250 | 0 |
| Tab | growth | 21 | 25,650 | 0 |
| Tier | EARLY | 18 | 4,350 | 9 |
| Tier | EARLY_MID | 10 | 3,450 | 5 |
| Tier | MID | 62 | 29,950 | 23 |
| Tier | LATE | 56 | 66,300 | 23 |
| Tier | ENDGAME | 25 | 82,850 | 9 |
| Tier | MID_LATE | 29 | 22,200 | 6 |
| Status | NOW | 163 | 159,000 | 75 |
| Status | VERIFY | 17 | 21,000 | 0 |
| Status | FUTURE | 20 | 29,100 | 0 |
| Registration | true | 75 | 73,850 | 75 |
| Registration | false | 125 | 135,250 | 0 |

Total: **200 quests, 209,100 XP**. Active NOW+VERIFY: **180 quests, 180,000 XP**; FUTURE: **20, 29,100 XP**. Registered: **75 quests, 73,850 XP**; unregistered: **125, 135,250 XP**. Unregistered NOW: **88**. Global reward median: **750 XP**, range **150–4,050 XP**. Endgame holds 82,850 XP (39.6% of all catalog XP) in 25 quests. EARLY+EARLY_MID hold 7,800 XP in 28 quests (3.7%). These tier totals include FUTURE and VERIFY where present.

## Tracker coverage

| Kind | Quests | XP | Registered |
|---|---:|---:|---:|
| `distinct_set` | 20 | 20,150 | 18 |
| `stat_custom` | 8 | 9,400 | 0 |
| `distinct_groups` | 3 | 1,100 | 3 |
| `pending` | 77 | 81,950 | 0 |
| `knowledge_seen` | 10 | 6,900 | 0 |
| `counter` | 29 | 30,500 | 26 |
| `stat_killed_monsters` | 4 | 3,750 | 4 |
| `counter_each` | 2 | 1,350 | 2 |
| `stat_killed_monsters_distinct` | 4 | 2,600 | 4 |
| `counter_gated` | 1 | 500 | 1 |
| `stat_killed_each` | 2 | 3,650 | 2 |
| `stat_killed` | 9 | 10,650 | 9 |
| `stat_killed_groups_each` | 1 | 1,100 | 1 |
| `stat_mined` | 2 | 800 | 0 |
| `counter_sum` | 1 | 1,100 | 1 |
| `stat_crafted_distinct` | 6 | 5,500 | 0 |
| `placement` | 4 | 5,000 | 4 |
| `knowledge_count` | 5 | 5,950 | 0 |
| `research_nodes` | 4 | 5,300 | 0 |
| `research_roots` | 1 | 350 | 0 |
| `research_tier` | 3 | 4,900 | 0 |
| `research_breadth` | 1 | 850 | 0 |
| `research_value` | 2 | 2,000 | 0 |
| `research_discipline_complete` | 1 | 3,750 | 0 |

The `pending` kind covers **77 entries and 81,950 XP**; none is registered. It is a coverage gap, not an active gameplay defect. All 20 FUTURE entries are `pending`. The 75 registered quests use non-pending trackers. A non-pending unregistered tracker is still not authorization to activate its quest.

## Pacing and reward pressure

- `xpq.exp.walk_10k`, `xpq.exp.walk_50k`, `xpq.exp.travel_250k`: distance escalates from 10,000 to 50,000 to 250,000 blocks, with 250/1,150/3,150 XP. Test whether the final step rewards enjoyable exploration or prolonged looping; the current early quest is unregistered.
- `xpq.cmb.hostile_100`, `xpq.cmb.hostile_500`, `xpq.cmb.hostile_2000`, `xpq.cmb.hostile_5000`: a pure kill-count ladder can be farmed. The later 5,000 step pays 1,700 XP; assess risk, novelty and effort in natural play before changing any approved value.
- `xpq.frm.harvest_1000`, `xpq.frm.harvest_5000`: repeated harvests and large counts favor automated farms; the latter is not yet tracked. Assess whether the objective is distinct from ordinary farming and Knowledge unlocks.
- `xpq.bld.place_1000`, `xpq.bld.place_10000`: both count repeat successful placement under the approved building policy. The 10,000 target for 500 XP may encourage place/break loops; keep that policy unless owner chooses a design change.
- `xpq.eqp.enhance_20_armor` (3,750 XP), `xpq.eqp.all_t2_cores` (3,750 XP), `xpq.grw.knowledge_complete` (3,750 XP): high endgame rewards are attached to very different acquisition paths and ownership costs. Live effort and access need measurement before any rebalance.
- `xpq.boss.monuments_3` (3,450 XP): three distinct monuments compound world-generation/travel and multiplayer credit risks. Its VERIFY gate remains intact.

Within-tier reward medians from the catalog: EARLY 250; EARLY_MID 350; MID 500; MID_LATE 750; LATE 1,100; ENDGAME 3,150 raw XP. The endgame median is 2.86 times LATE; this is a reward cliff to review against actual player XP sinks, especially the approved +0→+20 Enhancement total of 1,900 XP. A 3,750 XP quest exceeds that full cost. One-time claim storage may affect the timing of this influx. No reward change is made here.

## Redundancy and automatic co-completion

| Cluster, exact IDs | Design observation | Decision test |
|---|---|---|
| `xpq.exp.biomes_25`, `xpq.exp.biomes_40`, `xpq.exp.rare_biomes`, `xpq.exp.biomes_all_overworld` | Same visited-biome evidence can advance several entries; the last title says Overworld while A requires all dimensions. | Keep a deliberate discovery ladder; correct name/description only with owner approval and an exact biome roster. |
| `xpq.cmb.bestiary_5`, `xpq.cmb.bestiary_10`, `xpq.cmb.bestiary_25`, `xpq.cmb.habitats_5` | Same kills can satisfy species and habitat breadth; 25 and habitats use explicit MM species semantics. | Keep if each step adds a new behavior; verify natural spawn access and show per-type progress. |
| `xpq.cmb.hostile_100`, `xpq.cmb.hostile_500`, `xpq.cmb.hostile_2000`, `xpq.cmb.hostile_5000` | One monotonic source pays multiple milestones; high thresholds invite a low-risk farm. | Test ordinary progression and consider alternate objectives only by owner decision. |
| `xpq.grw.knowledge_10`, `xpq.grw.knowledge_20`, `xpq.grw.knowledge_35`, `xpq.grw.knowledge_50`, `xpq.grw.knowledge_complete` | Knowledge discovery already unlocks another system; quest rewards may stack automatically. | Compare XP timing to Knowledge and Research costs; do not count this as independent play by default. |
| `xpq.grw.first_research`, `xpq.grw.research_10`, `xpq.grw.research_25`, `xpq.grw.research_50`, `xpq.grw.spent_10k`, `xpq.grw.spent_50k` | Research purchase, count and value can pay together; player is spending XP to gain some back. | Model net XP and claim timing under scenario D. |
| `xpq.eqp.enhance_5`, `xpq.eqp.enhance_10`, `xpq.eqp.enhance_15`, `xpq.eqp.enhance_20` | Same equipment path chains milestones; expensive end steps can be offset by quest claims. | Compare one-time reward to 1,900 XP enhancement cost and assess intended subsidy. |
| `xpq.boss.elder_guardian`, `xpq.boss.monument_clear`, `xpq.boss.monuments_3` | The same monument combat can satisfy a kill and clear, with cooperative ownership rules. | Verify every participant gets credit only through approved evidence; retain VERIFY for clears. |

These are overlap candidates, not findings of duplicate IDs or invalid progression. Catalog A names visual antecedents; PR #129 edges are **presentation only** and do not enforce gameplay prerequisites. Vanilla advancement overlap is clear for `xpq.boss.dragon_rematch`, `xpq.boss.wither`, `xpq.cft.conduit`, and `xpq.exp.biomes_all_overworld`; compare objectives and reward timing before treating overlap as filler.

## Dependency concentration and UI cross-check

Equipment has 20 quests / 38,250 XP and growth has 21 / 25,650 XP, but both have **zero registrations**. This leaves mid and late XP Quest play concentrated in exploration/combat while growth/equipment systems are catalog promises. Farming, fishing, crafting and alchemy have 3, 3, 2 and 1 registrations respectively. Each ecosystem needs event hooks or feature access before it can be an active quest route.

PR #129 mapping at `2176935929c990a210fae64582d524d30993bc8a` contains 200 unique IDs, 15 chapters and 52 sub-branches; all 200 mapped category/tier/status/registered/XP values equal the Q1-C catalog. Its 26 edges comprise 24 presentation-only edges and 2 **not-enabled** gameplay candidates. PR #135 D1–D5 changes semantic zoom, fixed chapter labels, sub-branch rows, icons and input behavior, while the production slice remains three ecology nodes. Both PRs are open Drafts. Only 3/200 quests have EN and KO runtime title/description/objective copy; proposal titles and preview labels are not approved localization. Visual/UI acceptance remains separate from content and gameplay acceptance.

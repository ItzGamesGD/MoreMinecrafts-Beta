# Unified integration 2026-11-11 — development-track inventory and dependency graph

Snapshot: `git fetch --prune` on 2026-11-11 (148 remote branches, 146 distinct tips, **45 frontier tips** = tips not contained in any other branch).
Classifications: READY, READY_WITH_DEPENDENCIES, PARTIAL, BLOCKED, PAUSED_BY_OWNER, SUPERSEDED, DOCUMENTATION_ONLY, UNVERIFIED.

## 1. Repository baseline (verified)
| Ref | SHA | Notes |
|---|---|---|
| `origin/main` | `cde842dd` (2026-10-23, merge of #155) | **No common ancestor with the checkpoint lineage.** `main` has root `0e1a626d`; every `checkpoint/wave*` branch descends from root `ab531c74` (2026-10-25). `git merge-base origin/main checkpoint/wave16` is empty. |
| `checkpoint/wave16-unified-20261104` | `65072bca` | last unified checkpoint; ancestor of **every** post-11-04 development line below |
| main vs wave16 tree | 1,690 files differ; only 1 file of `main` is absent in wave16 (`progression/XpLevels.java`, removed in the wave lineage); wave16 is a content superset of main | |
| Existing `integration/*` | `integration/stage23-final-reconciliation-20261016`, `integration/wave10-unified-20261025`, `integration/wave12-functional-preservation-20261027` | not reused; no `integration/unified-20261111` existed |
| Existing checkpoints | `checkpoint/wave10…wave16-unified-*`, `stage-11…stage-20/checkpoint`, `stage-21/knowledge-checkpoint`, `stage-18/integration` | none overwritten; no tags exist |
| Local working tree | clean (on `research/st01-p8d2-r1-search-audit-20261111` before starting) | |

## 2. Development tracks
| Track | Latest relevant branch @ commit | PRs | Implementation status | Test evidence | Parent dependencies | Conflicts with | Eligibility | Exclusion reason |
|---|---|---|---|---|---|---|---|---|
| **Sky Tower** | `feature/sky-tower-integration-02` @ `25e6ed6e` | #263, #267, #271 (merged into it), #229 docs (contained) | full temple, worldgen composites, terrain-adaptive mountain placement, persistent trial progress, P4 unified vault rewards, DEF-P3A-01/02 fixes | PR #271 checks **13/13 success** (build incl. `test build runGameTest`, server-smoke, client-smoke, 3 worldgen probes, 2 persistence jobs, crafting source, static, coverage) — HISTORICAL_PASS | wave16; contains `feature/sky-tower-integration-01` | D13 line: `ColosseumGameTests` (test only) | **READY** → integrated (B1) | – |
| Sky Tower phase-2 validation docs | `claude/sky-tower-phase-2-validation-twragg` @ `d12a6e11` | – | 2 docs-only audit commits on top of int-01 | – | int-01 | – | DOCUMENTATION_ONLY | likely superseded by int-02's adaptive placement (`49737ef9`); not merged pending owner confirmation |
| **ST-01 Pyramid** | `research/st01-p8d2-r1-search-audit-20261111` @ `d01ca8ac` | stack #231 → P4/P5/P6/P7 (#246) → P8-A (#247) → P8-A-D1 (#250) → P8-B2 (#254) → G0 (#255) → G0-R1 (#257) → P8-C2-A (#259) → P8-C2-B (#262) → P8-C2-C (#266) → P8-D1 (#268) → P8-D2 (#270) → P8-D2-R1 (#272) | complete assembly, royal chest runtime, Guardian lifecycle, natural worldgen with **Policy D** (81×81 Desert, 16/10/8, no entrance/fluid rejection, overlap permitted); P8-D1/D2/D2-R1 are **docs + evidence only** | P8-C2-C CI run 42943983078 **24/24** at `f3ddfbea` (src identical at `d01ca8ac`) — HISTORICAL_PASS | wave16 | D13 line: royal-chest files (add/add), `MoreMinecrafts.java`, `fabric.mod.json`, authority index | **READY_WITH_DEPENDENCIES** → integrated (B3) | – |
| ST-01 Phase 4 | `claude/st01-pyramid-phase4-worldgen-roke2l` @ `ada10ba3` | #258 | fixture worldgen | – | P8-B2 | P8-C2-B | SUPERSEDED | P8-C2-B adapted it |
| ST-01 counterfactual audit | `audit/st01-p8c2b-counterfactual-rarity-20261111` @ `569fc789` | #264 | diagnostic probe + docs | its own CI | P8-C2-B | – | DOCUMENTATION_ONLY | its report/data are already in the ST-01 line (imported in P8-C2-C); diagnostic probe code not needed in production |
| ST-01 O-14 studies | `work/st01-o14-phase1-phase2-…`, `…phase3-calibration…`, `…phase3-1-validation-20261109` @ `7e37b786` | #249, #251, #252 | statistical probes and reports | per-PR | wave16 | – | DOCUMENTATION_ONLY | research tooling; the decisions they informed are recorded in the ST-01 authority |
| ST-01 colosseum clock seam | `remediation/st01-colosseum-clock-seam-20261110` @ `f2c0a6c8` | – | 2 commits | – | P8-B2 | – | SUPERSEDED | **patch-identical** (`git patch-id`) to `53f99c2b`/`a550eda1` already in the D13 line |
| **D13 accessory acquisition** | `implement/d13-03-i06-p4-st01-integration-20261108` @ `6c7923e0` | #228 → #233 → #234 → #235 → #237 (+#241, #242, #243 merged) → #245 | D13-02 relic catalyst sources; I01 T2 accessory chest pools; I02 Deep Sea Pearl; I03 Heart of Gravity; I05/I06-P3 royal chest engine; colosseum time seam; D13-03 and I06 authority | PR #245 build + server-smoke + client-smoke **success** — HISTORICAL_PASS (earlier #235 build failure was fixed by #242 inside the line) | wave16 | ST-01 royal-chest files; Sky Tower `ColosseumGameTests` | **READY_WITH_DEPENDENCIES** → integrated (B2); its royal-chest runtime is superseded by the ST-01 port (`c9480752` "Selectively port P3 engine from 97afeb66 and I05 … from 18362bb9") | – |
| D13-01 relic catalyst recipe | `implement/d13-01-relic-catalyst-20261104` @ `23e9be7e` | #227 | 1 commit | **build check failed** (2026-11-04), not rerun | wave16 | – | UNVERIFIED | last CI build failed; not re-verified |
| D13-03 I04a Star Covenant vault | `implement/d13-03-i04a-star-covenant-vault-logic-20261106` @ `df673dfe` | #236 | vault reward logic | – | I03 | Sky Tower `StarCovenantVaultRewards.java` (different content) | SUPERSEDED (provisional) | Sky Tower P4 (`69899386`) implements `StarCovenantVaultRewards` itself; owner to confirm the I04a version is obsolete |
| D13-03 authority docs | `docs/d13-03-authority-q01-q47-20261106` @ `3ad9f283` | #232 | docs | – | wave16 | – | SUPERSEDED | the same decision document is in the D13 line (`eae086db`); index routing in the line is a superset |
| **Mob (SKY-MOB)** | `docs/sky-mob-p0-authority-20261106` @ `ec026c05` (P1 entities, P2-B/C/D/E models merged into it), `claude/sky-mob-p2f1-blood-seraph` @ `dcd93ba5` (#265), `claude/sky-mob-p2b-p2c-vwuur8` (#260 closed), `claude/sky-mob-gametest-timing-nn7o1h` | #238, #239, #244, #248, #253, #256, #261, #265, #260, #240 | partially implemented; visual approvals pending | per-PR | wave16 | – | **PAUSED_BY_OWNER** | owner paused the track; nothing merged; branches preserved |
| Mob + Sky Tower candidate | `work/sky-mob-tower-integration-20261111` @ `bd5ba43f` | #269 | mob + Sky Tower **int-01** | – | mob P0 + tower int-01 | – | PAUSED_BY_OWNER / SUPERSEDED for the tower part | contains mob work; its tower content is older than int-02 |
| ST-05 colosseum worldgen / controller | `design/st05-colosseum-owner-decisions-20261028` (#207, 113 docs commits), `feat/st05-c-controller-foundation-20261030` (#213), `feat/st05-c4-structure-mask-20261030` (#216), `validation/st05-stage0-a02-a03-20261030` (#218 "BLOCKED"), `audit/st05-po25-review-package-20261102` | #207, #213, #216, #218 | partial; based on wave12/wave15, not on wave16 | per-PR | wave12/wave15 | wave16 colosseum runtime | PARTIAL / BLOCKED | pre-wave16 bases; #218 self-declared BLOCKED; code would need reconciliation with the colosseum runtime in wave16 |
| Docs-only proposals (pre-wave16 base) | `docs/content-design-roadmap-wave15-20261030` (#215), `docs/st03-owner-decisions-q01-q42-20261104` (#225), `feat/st03-implement-01-20261105`, `design/t1-accessories-d01-owner-decisions-20261030`, PRC-P7 security chain (#197/#199/#201/#203) | as listed | documentation | – | wave12/wave15 | authority index | DOCUMENTATION_ONLY | not reviewed/merged by the owner; integrating them would promote unreviewed text into authority; kept for a later docs pass |
| Historical unrelated-history branches | `stage-*`, `codex/*`, `experiment/*`, `prototype/*`, `release/wave9-rc-20261025`, older `docs/*` (no merge base with wave16) | e.g. #107, #117, #127-#130, #141 | historical | – | old lineage | – | SUPERSEDED / UNVERIFIED | outside the checkpoint lineage |
| Equipment, loot, research/XP quests, bounty, fishing, UI/networking, resources | no development branch beyond wave16 except those above | – | in wave16 | wave16 history | – | – | in base | – |

## 3. Dependency graph (post-wave16)
```
checkpoint/wave16 (65072bca)
├── Sky Tower: int-01 ─▶ int-02 (25e6ed6e)  [#263 #267 #271 merged]              ── B1
├── D13: d13-02 ─▶ i01 ─▶ i02 ─▶ i03 ─▶ i05 ─▶ (#241 docs, #242 fix, #243 P3) ─▶ i06-p4 (6c7923e0)   ── B2
│        └─ i04a (sibling of i05; superseded by Sky Tower P4)      d13-01 (sibling, UNVERIFIED)
├── ST-01: build docs ─▶ P4 ─▶ P5 ─▶ P6 ─▶ P7 ─▶ P8-A ─▶ P8-A-D1 ─▶ P8-B2 (port of D13 I05/P3) ─▶ G0 ─▶ G0-R1 ─▶ P8-C2-A ─▶ P8-C2-B ─▶ P8-C2-C ─▶ P8-D1 ─▶ P8-D2 ─▶ P8-D2-R1 (d01ca8ac)   ── B3
│        ├─ Phase 4 (#258, superseded)   ├─ audit #264 (docs)   └─ clock seam (patch-equal to D13 commits)
├── Mob P0 ─▶ P1 ─▶ P2… (PAUSED)        └── #269 mob + tower int-01 (PAUSED)
└── O-14 studies (#249/#251/#252)
```
Overlaps (files changed by more than one line vs wave16): ST-01 ∩ D13 = 19 files (royal chest, registration); ST-01 ∩ Sky Tower = 3 (registration files); D13 ∩ Sky Tower = 4 (registration + `ColosseumGameTests`).
Patch equivalence: the colosseum time-seam commits appear in three lines with identical patch-ids (D13 `53f99c2b`/`a550eda1`, clock-seam branch, mob `f6c45ee3`); integrated once via D13. No other cherry-pick-equivalent commits between the integrated lines (`git cherry` checks).
Integration order: Sky Tower (independent, largest) → D13 (introduces the shared colosseum seam and the D13 content) → ST-01 (newest derivative of the royal chest engine, so its versions supersede D13's on conflict).

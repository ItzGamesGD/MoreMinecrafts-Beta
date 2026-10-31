# ST-05 Stage 0-FIX — Owner Decisions (2026-10-31)

**Status: OWNER-APPROVED POLICY ONLY. NO IMPLEMENTATION AUTHORIZATION.**

## A03-FIX-Q01 — B: protect player modifications first

The owner explicitly chooses **B (player-modification protection priority)** over automatic completion that can overwrite player changes, or a presently unproven guarantee of never exposing partial placement.

- On recovery of a partially placed arena, if player intervention is confirmed **or suspected**, **do not automatically overwrite** affected blocks; stop automatic restoration and route the site to a safe recovery/quarantine workflow.
- Any intervention-detection method must be independently proven on Minecraft 26.3. In particular, a player changing a block and restoring its original state may leave the same current block state as an untouched block; block-state comparison alone **cannot prove the absence of player intervention**.
- The precise evidence threshold, tracking mechanism, quarantine semantics, and recovery procedure are **NOT YET APPROVED**. Do not silently interpret uncertainty as permission to overwrite.
- This choice does **not** resolve A03: crash-time partial visibility and intervention detection remain technical blockers.
- Keep all WG-Q01–Q45 + FIX-01 A constraints, especially full-plan-before-write, deterministic worldgen, safe terrain/structure interactions, 75% fit-before-roll semantics, and no unauthorized architecture/combat changes.
- A02 (global cavern identity, spacing conflict precedence, denominator) remains **BLOCKED** and undecided.
- No implementation, production worldgen integration, merge, checkpoint promotion, or Stage 1 progression is authorized by this decision.

**Provenance:** Owner response approving ST-05-A03-FIX-Q01 B on 2026-10-31.

## A03-FIX-Q02 — B: quarantine whenever intervention cannot be excluded

The owner explicitly approves **B (quarantine on uncertainty)**.

- Automatic recovery may proceed **only for portions with sufficient, independently verifiable evidence that no player intervention occurred**. Define and validate the evidence standard before implementation; no current-state-only check can establish a complete modification history.
- Where nonintervention cannot be established, **preserve existing blocks and quarantine the uncertain portion** rather than overwrite it. Do not interpret missing logs, unavailable chunks, interrupted writes, or ambiguous evidence as absence of intervention.
- The exact quarantine boundary, behavior when uncertain and safe portions overlap, and how a quarantined structure is later completed or retired remain **unresolved owner decisions**. A03 remains BLOCKED until a feasible mechanism and tests establish safety.
- No implementation, integration, merge, checkpoint or Stage 1 approval. This decision preserves A03-FIX-Q01 B, WG-Q01–Q45 and FIX-01 A.

**Provenance:** Owner approval of ST-05-A03-FIX-Q02 B on 2026-10-31.

## A03-FIX-Q03 — B: chunk-level quarantine with dependency closure

The owner explicitly approves **B (chunk-level quarantine)**.

- If player intervention cannot be excluded in a chunk containing a partially generated arena, **quarantine that chunk plus any cross-chunk structural/connection dependencies affected by it**. Do not automatically overwrite blocks in the quarantine set.
- Automatic recovery may proceed **only in independent chunks for which nonintervention is sufficiently evidenced** and whose restoration cannot change, overwrite, invalidate or implicitly depend on quarantined portions.
- The dependency closure must be conservatively determined and validated across chunk boundaries, crash/restart, cave corridors, protected structures and arena completion state. If independence cannot be proven, expand quarantine; do not silently assume it.
- **Chunk quarantine does not imply an incomplete arena is usable or completed**. Trial activation, Vault rewards, successful-generation accounting and other completion-dependent behavior must remain disabled until the entire structure satisfies its acceptance gates.
- Exact dependency calculation, quarantine persistence and later recovery/retirement remain undecided. A03 remains BLOCKED pending feasibility proof.
- No implementation, production worldgen integration, Stage 1+, merge, checkpoint or owner-constraint changes authorized.

**Provenance:** Owner approval of ST-05-A03-FIX-Q03 B on 2026-10-31.

## A03-FIX-Q04 — B: conditional recovery; retire if safety cannot be established

The owner approves **B: conditional recovery plus non-destructive retirement if recovery cannot be demonstrated safe**.

- A quarantined arena or chunk may resume recovery **only when sufficient independently verifiable evidence establishes that recovery will not overwrite player modifications**, including cross-chunk dependencies as approved in A03-FIX-Q01–Q03. Missing evidence is not consent to overwrite.
- If safety cannot be established, **mark the affected arena generation as failed/retired and disable all completion-dependent trial, Vault, key, reward and success accounting behavior**. Retirement is a **logical state transition only**, not removal, replacement, rollback or forced deletion of existing world blocks (including player-built blocks).
- Preserve the actual world state and audit trail. Do not auto-retry the same placement or allocate another cavern/roll as a hidden substitute; retry/placement-accounting policy remains undecided and needs explicit owner approval.
- The precise evidence standard, retirement persistence, transitions, and reattempt policy still require technical validation or further owner decisions. A03 remains BLOCKED until demonstrated safe in Minecraft 26.3.
- This decision preserves A03-FIX-Q01–Q03 B, WG-Q01–Q45 and FIX-01 A. **No implementation, production worldgen integration, Stage 1+, merge, checkpoint change or constraint relaxation authorized.**

**Provenance:** Owner approval of ST-05-A03-FIX-Q04 B on 2026-10-31.

## A03-FIX-Q05 — B: protect retired site; allow only independent cavern candidates

The owner approves **B: preserve the retired arena site and existing player blocks; consider only candidates in other independent caverns that satisfy all original spacing and collision constraints**.

- A failed/retired arena is not automatically removed, reverted, or overwritten. Its occupied footprint and player modifications remain protected during future candidate checks.
- A candidate in a **different, demonstrably independent cavern** may be evaluated only if it satisfies the existing minimum separation, protected-structure collision checks, and every other WG-Q01–Q45 hard gate. No implicit exception for a retired arena and no relaxation of minimum spacing.
- **No second 75% roll and no fresh candidate search in the same cavern** as a substitute for the retired arena. Keep FIX-01 A's pre-roll fit evaluation and one-roll-per-eligible-cavern policy; never treat retirement as a new eligibility event.
- Deterministic cavern identity, the meaning of independent caverns under distant natural connections, cross-cavern conflict precedence and probability denominators remain **A02 BLOCKER / OWNER_DECISION_REQUIRED**. This decision does not define them.
- The exact footprint accounting and persistence of retired sites remain technical validation work under A03. Preserve A03-FIX-Q01–Q04 B and all prior approved ST-05 rules.
- **No implementation, Stage 1+, worldgen integration, merge, checkpoint promotion, or owner-constraint changes authorized.**

**Provenance:** Owner approval of ST-05-A03-FIX-Q05 B on 2026-10-31.

## A02-FIX-Q01 — B: independently qualifying large cavern spaces as probability units

The owner approves **B: independently qualifying large cavern spaces**, not the entire globally connected air-volume network and not arbitrary fixed geographic regions, are the conceptual units for the single 75% roll.

- Identify major natural cavern chambers that individually satisfy the already approved WG-Q09 qualification intent (representative width >=32, height >=18, traversable connected cave path >=64). A narrow/long connecting passage **alone does not merge** two otherwise independent major chambers into one probability unit.
- Each distinct qualifying large cavern space may be evaluated independently, subject to **WG-Q11 one placement per defined cavern unit**, the original minimum separation and collision rules, and **FIX-01 A** (up to eight deterministic candidate fit checks **before** one 75% roll for a fit-eligible unit).
- **Do not invent** a passage-width cutoff, chamber segmentation radius, sampling grid, minimum separation distance, global conflict priority, probability denominator exclusions, or cave-ID algorithm without owner approval where these would alter eligibility, density or approved constraints. In particular, broad connected open spaces may require a different rule from narrow connectors; the segmentation threshold is unresolved.
- Deterministic identification across chunk boundaries, generation order and restart, and correct treatment of overlapping chambers and neighboring candidate conflicts remain **A02 BLOCKER** until proved in Minecraft 26.3.
- Preserve A03-FIX-Q01–Q05 B, WG-Q01–Q45, FIX-01 A and existing combat/persistence/reward authority. **No implementation, Stage 1+, worldgen integration, merge or checkpoint promotion authorized.**

**Provenance:** Owner approval of ST-05-A02-FIX-Q01 B on 2026-10-31.

## A02-FIX-Q02 — B: relative cross-section and bottleneck analysis

The owner approves **B: distinguish independently qualifying large cavern spaces by relative cross-sectional narrowing / bottleneck analysis**, rather than only an absolute fixed passage-width cutoff or center-distance partition.

- Use geometry of large connected chambers and intervening narrow necks to propose segmentation; preserve WG-Q09's qualifying chamber dimensions and traversable cave requirements.
- No particular cross-section threshold, comparison window, scan resolution, computational budget, cavern-ID function, or minimum separation priority is approved by this choice. These remain technical design/validation questions, with owner approval required if they alter the number of roll-eligible cavern units or other approved semantics.
- Validate order-independent segmentation across chunks, restarts and irregular multi-branch caves, with deterministic operation budgets and counterexamples. **A02 remains BLOCKED** until proven.
- Preserve A02-FIX-Q01 B, A03-FIX-Q01–Q05 B, WG-Q01–Q45 and FIX-01 A. No implementation, Stage 1+, worldgen integration, merge or checkpoint changes authorized.

**Provenance:** Owner approved ST-05-A02-FIX-Q02 B on 2026-10-31.

## A02-FIX-Q03 — B: evidence-led bottleneck threshold proposal, owner approves final value

The owner approves **B: delegate empirical evaluation and proposal of cavern-bottleneck segmentation thresholds** using actual Minecraft 26.3 cave samples, but **reserve final threshold approval to the owner**.

- Compare candidate cross-sectional narrowing metrics, fixed-reference thresholds, boundary/branch cases, and observed qualifying cavern counts on real 26.3 seeds. Include examples where distant large chambers are linked by narrow tunnels and where one broad cave must not be spuriously split.
- Report seed/coordinates, deterministic work budget, cave IDs, candidate eligibility counts, spacing exclusions, and how each proposed threshold changes the **number of distinct fit-eligible cavern units** and therefore the effective 75% roll denominator.
- The implementer may **research, prototype in isolation, benchmark, and recommend** parameter values; may **not finalize or deploy** a threshold, silently change WG-Q09 qualification bounds, FIX-01 A's fit-before-one-roll semantics, or WG-Q11 separation constraints.
- Require owner approval of the proposed numerical threshold/metric before using it in integrated worldgen. A02 remains BLOCKED until the segmentation and stable-ID method is proved across chunk orders/restarts.
- Preserve A02-FIX-Q01–Q02 B, A03-FIX-Q01–Q05 B, WG-Q01–Q45, FIX-01 A. **No implementation of worldgen, Stage 1+, integration, merge or checkpoint change authorized.**

**Provenance:** Owner approval of ST-05-A02-FIX-Q03 B on 2026-10-31.

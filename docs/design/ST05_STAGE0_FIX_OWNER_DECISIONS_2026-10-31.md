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

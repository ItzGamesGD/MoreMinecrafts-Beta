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

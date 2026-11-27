# ST-05 Stage 0-F — Owner decisions and corridor/construction implementation contract

Status: **OWNER DECISIONS APPROVED — IMPLEMENTATION PENDING**
Base: Draft PR #217 @ `622d1c54865cf854a74bed1242c654d83d4e1d86`.
This document supersedes the open parameter-selection requests in the Stage 0-E PR description, but does not assert that the corresponding production code or construction tests exist.

## Binding owner decisions (2026-11-01)

1. B-prime chamber core extraction parameter **24×24×12**, width/height qualification **DISK32** (32-wide cylinder, height 18). Do not treat BOX32 or 10×10×6 / 16×16×8 as production alternatives.
2. Keep preflight's **zero-fluid tolerance provisionally** for the Stage 0-E probe. This is **not** a permanent gameplay prohibition on all fluid-adjacent builds. During actual construction, evaluate whether encountered water/lava can be sealed safely under the existing approved construction/safety constraints. Do not invent an arbitrary tolerated-fluid count or silently relax safety requirements.

## Next task: implementation first, not another design audit

- Reuse verified Stage 0-D identity/ownership/determinism/restart evidence and Stage 0-E real-terrain spacing/roll evidence; do not rerun those expensive scans solely to restate previous PASS results.
- Apply approved core and DISK32 parameters to the production candidate path. Ensure old parameter experiments remain clearly separated from production selection; preserve old experimental evidence.
- Implement the already-approved 32-block connection, corridor routing, 5×5 clearance, stairs/terrain repair and approved eight alternative placements, referencing the existing WG/FIX/RESOLVE authority ledgers for exact semantics. Do not invent or change gameplay parameters.
- Implement a deterministic construction plan and preflight that handles plan-time vs final-terrain mismatch: recheck the real terrain before destructive edits, validate structure/corridor footprints and sealable fluids, and fail closed on uncertainty. Treat ordinary excavatable blocks as excavatable rather than rejecting all non-air blocks.
- Persist reservation, construction state and recovery evidence separately; do not claim exactly-once multi-chunk construction without tests. Use conservative recovery/quarantine for ambiguous partial edits; do not reroll 75% or reassign spacing losers on post-roll failures.
- Write targeted tests for routing, alternative placement order, clearance, fluids (sealable vs unsealable), partial edits/restart and construction placement in a real Minecraft 26.3 server. On failure, fix code and rerun only affected checks. Update Draft PR #217 with implemented code, actual test evidence and remaining blockers.
- Do not implement Stage 1 combat/rewards; do not merge, move checkpoint, or delete branches.

Escalate only an actual contradiction between approved construction rules, or a genuinely missing gameplay decision that cannot be resolved as routine engineering. No new broad audit phase. Any future verification should inspect implemented behavior against approved requirements.

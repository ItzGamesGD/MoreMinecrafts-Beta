# ST-05 Stage 1 — activation owner decisions and implementation handoff (2026-11-02)

Status: OWNER-APPROVED ACTIVATION DECISIONS; STAGE 1 IMPLEMENTATION PENDING.
Source baseline: Draft PR #217, 1ac3b33808464b09ac8638148c39093dd087d1bb. This document supplements existing WG/FIX/RESOLVE approvals without rewriting their other gameplay rules.

## Owner-approved decisions

1. The centre crystal is **purely visual decoration**. It has no interaction, activation, round-control, reward, or Vault-unlock responsibility. Do not implement a crystal-click start mechanic.
2. **Entry is detected by a player crossing into the approved arena structure's interior.** Do not require a centre click, button, or manual activation device. Derive the authoritative interior volume from the constructed arena plan, not a generic cave radius or mere proximity.

## Critical integration distinction

An ENTRY event is not automatically a new roster entitlement or a repeat challenge trigger. Preserve the previously approved fixed-attempt roster, no late automatic additions, completion eligibility, return grace, wipe and restart rules. Before implementing the trigger, reconcile the exact attempt-creation/roster-snapshot moment with the original approved arena gameplay spec. If no approved rule resolves the simultaneous-entry boundary, do not invent a new gameplay rule: surface only that concrete owner decision. Engineering-level debounce, per-player enter/exit edge detection, persistent attempt IDs, idempotent event delivery, and unloaded-chunk safeguards may be implemented autonomously.

## Implementation work order — code first

1. Read existing authoritative ST-05 gameplay rules once, identify only direct contradictions with these two owner decisions. Preserve original round/reward parameters.
2. Integrate Stage 0-F/G constructed arena identity and verified interior geometry with a server-side entry detector. Exclude corridors, outside cave, and cutaway/camera-only terrain. Track inside/outside transitions deterministically.
3. Implement an arena attempt controller and roster snapshot at the approved start boundary. Avoid duplicate starts across movement ticks, re-entry, unload, logout and restart. Keep the crystal decoration-only.
4. Implement seven-round progression with the existing approved threat budgets, monster caps, timing, pause/return and 65-minute lifecycle, using durable identifiers and conservative quarantine on uncertain recovery.
5. Integrate approved key/Vault/reward access without letting vanilla Vault interactions bypass roster and entitlement rules. Implement the boss and reward subsystems in subsequent bounded implementation tasks, not a new pre-implementation audit.
6. After code exists, run targeted Minecraft 26.3 integration tests: crossing into interior, lingering at boundary, two-player simultaneous entry, late entry, return, wipe, restart, duplicate prevention, and Vault access. Fix failures and rerun affected tests. Report only actual design conflicts, not ordinary engineering choices.

Do not merge PR #217, move checkpoint, delete branches, or claim Stage 1 is complete from this planning document.

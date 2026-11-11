# ST-05 Stage 1 — owner-approved round-boundary join amendment (2026-11-02)

Status: OWNER APPROVED. This is an explicit amendment to the earlier immutable-attempt-roster interpretation; it is not an inferred engineering preference. Applies to Stage 1 implementation on Draft PR #217.

## Approved gameplay semantics

1. Entering the actual constructed arena interior is the trigger. The central crystal is decoration only, never an activation control.
2. The first eligible entry begins an attempt. Players entering before the first round's roster snapshot are included in that round. Avoid order-dependent treatment of multiple players entering in the same server tick; gather the tick's entries before committing that snapshot.
3. Players entering **after a round has begun** are pending joiners, not active participants in that round. They do not change its mob scaling, supply allocation, or participant entitlement. Do not retroactively change a round's locked parameters.
4. At the **next round boundary**, validate pending players still eligible/present, add them to the active participant roster, and recalculate the next round's scaling and supply. Their reward eligibility begins with that admission, subject to other approved completion-eligibility rules; no retroactive rewards.
5. Exits during a round do not retroactively lower that round's scaling. Preserve the approved monotonic maximum-active-player scaling floor and the M(n)=1+0.6(n-1) formula, unless a separate explicit owner amendment changes them.
6. A pending joiner is not counted as an active participant for the current round's wipe, grace, scaling, supply, or reward decisions. The original current-round participants retain their approved death, return, and completion rules.

## Explicit precedence / conflict reconciliation

This decision **supersedes** the 'roster fixed at attempt start; no late automatic join' interpretation in RESOLVE Q23 and its dependent immutable-roster assumptions in Q24-Q25 and Q33-Q37, **only to the extent necessary** to permit validated admission at round boundaries. In particular:
- 'fixed roster' now means a **snapshot locked within each round**, not permanently fixed for the entire attempt.
- Q34's round-start scaling lock is preserved; Q35's maximum-active-player floor is preserved.
- Q36's active-player count must use the **currently admitted** roster, never pending entrants.
- Completion reward eligibility continues to require being alive and inside the arena at completion, and being admitted by a round boundary. Pending players do not acquire rewards simply by being physically present.
- Retain all unrelated prior rules, including 65-minute cap, 180-second return grace, payout idempotence, and safe recovery/quarantine.

## Engineering boundaries to resolve without reopening approved design

Persist active roster, pending entrants, admission round, round snapshot, and entitlement IDs. Deduplicate entry and admission events. Handle re-entry of an existing admitted participant as return, not a new admission. Use a deterministic tick/round-boundary ordering. If a specific scenario is not determined by this amendment (e.g. whether an attempt can continue when all active members wipe before a pending entrant's next-round admission), follow the pre-existing failure rule rather than inventing a rescue exception: wipe of all current active participants ends the attempt immediately.

No new pre-implementation audit. Implement and test Stage 1-A on Minecraft 26.3; report actual conflicts only. No merge, checkpoint movement, or branch deletion.

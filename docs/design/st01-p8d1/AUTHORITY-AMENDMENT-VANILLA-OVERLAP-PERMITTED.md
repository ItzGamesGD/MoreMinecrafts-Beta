# ST-01 authority amendment — vanilla structure overlap is permitted

Date: 2026-11-11 · Status: **owner-confirmed** (P8-D1 brief §1.1) · Prospective; additive to the P8-C2-C records.

## Decision
Overlap between a naturally generated ST-01 Pyramid and vanilla structures (including villages and mineshafts) is **permitted**.
Vanilla overlap must **not** become a new generation rejection condition, an exclusion, a suppression or a relocation rule.

## What did not change
Policy D ([OWNER-DECISION-POLICY-D.md](../st01-p8c2c/OWNER-DECISION-POLICY-D.md)), the placement parameters (81×81 footprint, spacing 32,
separation 8, TRIANGULAR, salt 91401, full-footprint Desert, MODERATE terrain 16/10/8, four rotations, deterministic 50:50 EAST/WEST),
the assembly, royal chest, Guardian and reward behavior. Accepted irregularities still do not authorize crashes, corrupted block
entities, duplicate rewards or invalid structure registration.

## Historical records preserved (not rewritten)
The previously unresolved status ("does not approve a vanilla-structure overlap policy") remains in the P8-C2-B amendment record
and the authority index paragraph of 2026-11-10; P8-C2-B README §8 and the counterfactual audit (PR #264) list overlap as an open
owner decision. Those statements were correct when written and are superseded **only** on this point, prospectively, by this amendment.
Measured overlap at the time of Policy D verification: 16 vanilla overlaps over the 46 generated pyramids (evidence in
`docs/design/st01-p8c2c/evidence/aggregate.md`).

## Implementation note for later phases (not a design decision)
Overlap is permitted, but systems that look up "the structure at a position" must tolerate it. See EXISTING_SYSTEM_AUDIT.md
finding F-3 (the bounty entry detector returns only the first structure at a position).

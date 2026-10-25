# AUTHORITY CONFLICTS

| ID | Conflict | Location | Fix proposal | Needs decision |
|---|---|---|---|---|
| C-01 | Old lines saying "XP Quest disabled / DEFERRED" remain without an inline superseded mark | `CONTENT_AUTHORITY_INDEX.md` line 1 banner; `UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md` lines 18, 29, 39 | Add "superseded by 2026-10-17 amendment" to those lines | No (AUTO) |
| C-02 | The authority claim-order description ("check → record → grant") does not define a rollback once exact grants are added; meanwhile the D document asserts that saves are atomic | UI amendment, D §1.4 | Make the authority require a grant-exception rollback or a pre-validation order; remove the "guaranteed" wording | Yes |
| C-03 | Authority says "VERIFY unregistered, 161 NOW registered", but this audit shows 62 of the NOW quests cannot be registered until defects are fixed | UI amendment | Change to "only NOW quests that pass this audit's gates are registered" | Yes |
| C-04 | "Active = exactly 180,000" vs a changing set of registrable quests | UI amendment | Separate the approved ledger from the registered supply | Yes |
| C-05 | B §6 / §7 recommend scenario B, but D is approved | B document | Correct the body or mark it as history | No |
| C-06 | D §2 says "keep `giveExperiencePoints` for grants", but authority requires exact calculation for both grants and deductions | D document | Correct the body | No |
| C-07 | The exact-XP specification does not state whether the score (`increaseScore`) should change; vanilla grants raise the score | Authority | Decide whether to mirror the score | Yes (minor) |
| C-08 | C §5 and A FUTURE `t3_*` rest on unapproved equipment expansion (README states C is unapproved) | A, C | Redefine around the LOCKED T3 roster of 7 | No |
| C-09 | The README's priority statement (authority wins) is correct, but A carries no notice that its D-01 retroactivity claims are wrong | README, A | Add an audit notice in the next document revision | No |

This audit modified no authority document. All fixes above are proposals.

# Owner decisions requiring approval

This file does not reopen settled design. It identifies the execution authorization and unresolved choices that an agent cannot decide safely. No production change, merge, closure or deletion is authorized by this audit. The full concrete source/batch proposal is [INTEGRATION_PLAN.md](INTEGRATION_PLAN.md).

## Decision needed to start the next task

**Authorize Batch A on a new candidate branch from `5f92307555ce5c486a2e0e8091f956bcbb6c6646`, with the cumulative #125 core (`079d36ac3bd2d8950118e30647d5f2efad19f947`), bounded #126/#135 donors, latest #140 design constraints and required engineering/validation.** This explicitly includes the broad Research/furnace/crafting/fishing/mob-resource/Quest dependency foundation (377 changed paths), rather than treating it as a UI-only patch. No existing branch/PR will be merged, closed, deleted, rebased or force-pushed under that next task unless separately authorized.

Approval should specify the initial Quest release roster: a limited reviewed slice, or continuation of content review before broader activation. Recommended minimum candidate: the reviewed Ecology slice after its latest requirements/predicates/tests are satisfied, while preserving the 200-slot design budget and rejecting unregistered claims. Existing 75 registrations do not establish owner approval of all content; the remaining three-node presentation is not a substitute for a deliberate roster. The owner can approve the candidate scope while keeping main promotion separate.

## Choices that block their affected scope

| Choice | Why it requires the owner | Concrete recommended handling |
|---|---|---|
| Retain custom XP canvas instead of the approved vanilla `AdvancementsScreen`? | #135 diverges from the explicit Oct17 screen contract; its later code/preview documents do not durably amend that contract. An agent cannot silently replace approved behavior. | Default plan honors the existing vanilla contract and adapts applicable donor work. **No new design decision is needed for that default.** If the owner instead wants the custom canvas/semantic-zoom screen, explicitly amend the routed authority before implementation. |
| Exact predicates still undefined for the chosen initial roster | #140 requires habitat-specific kills and calls for valid biome tag/ID specification; its new exploration/geode/gliding/Mending objectives also leave some event thresholds/identity/eligibility details open. These can produce materially different gameplay. | Resolve only predicates needed by the approved initial roster; defer unrelated objectives. Do not silently substitute species stats or proximity/distance counters. Preserve existing credible evidence and stable save IDs. |
| Initial roster/content detail outside the reviewed slice | #140 section N requires replacements in nine existing slots; exact replacements are not approved. Farming quotas, Mending tab/slots/count and several predicates remain proposals. | Keep 200 slots/per-slot provisional budget and existing frozen snapshots; approve exact replacements/details during their content task. Do not invent goals, delete slots, redistribute XP or enable preview rows to fill the roster. |

These decisions are about release scope and undefined gameplay. Missing claim dispatch, claim-state indicators, toast, pricing wording, event tests and engineering under approved rules do **not** require fresh design approval; they require authorization to perform the next implementation task only.

## Only if optional batches are selected

| Optional scope | Owner decision genuinely needed |
|---|---|
| Accessory B | Authorize integration; durably confirm/synchronize previously task-approved slot/death/layout decisions where the routed authority still labels them gates. The latest #152 authority already approves all 12 identities and 32x32 size; do not ask for concept approval again. Final rendered pixel appearance still requires owner visual acceptance. Choose whether a deliberately partial equipment/enhancement test candidate is acceptable with effects/catalyst art deferred and #152 accessory art awaiting rendered acceptance; do not call it complete accessories. Deferred activation/stack/death/effect/acquisition/catalyst-finality design is needed only if that scope is added. |
| Contracts C | Authorize integration after count repairs/verification. Specify profession weighting and objective-dependent effort/delivery-cost/value rules, and resolve remaining explicitly provisional generation/distribution/restock policy before final release. Simulation outputs are recommendations, not authority. Clarify entry semantics only if the existing approved wording cannot resolve a targeted test. |
| Contract overflow redesign | A mailbox or materially different delivery/receipt guarantee changes the approved contract. Seek approval only if engineering investigation selects such a redesign; no mailbox is presumed necessary. Current documented residual crash limitation must remain explicit. |
| Arena D | Authorize promotion of the isolated approved prototype into the chosen client. It has not reached main, and prototype approval is not automatic production acceptance. |
| Skull #130 | Authorize a separate repair/integration task if desired. The required crash-safe provenance goal is already approved; selecting an engineering receipt/journal design does not itself need new gameplay approval. A weaker guarantee or changed behavior would. No P1 fix is ready to promote. |
| PR cleanup | After actual retained-content validation, authorize exact PR closures/branch deletions individually or by an explicit list. This audit's 46 redundant/superseded recommendations authorize neither. |

## Already settled; do not request again

- Territory is **SHELVED** under #138 and the owner's current instruction. Do not reactivate it, implement its registration/facilities/protection or reopen its decision list. Independent ideas remain candidates under their own authority.
- V4 Knowledge/Research is active. Knowledge is free/non-purchase; Research uses the approved raw-XP/material/prerequisite rules. Historical idea-parking-lot text is superseded.
- XP completion grants no XP; the approved completed/unclaimed click claims once, with server validation and rollback, exact raw-XP state and persisted claim keys. Implementing missing caller/state/toast is engineering, not a request to redesign rewards.
- #140 section N replaces earlier deletion-without-replacement wording: preserve 200 slots and per-slot provisional budget until approved adjustment. Fishing content review is on hold. Do not reinterpret the hold as approval to change flags or delete entries.
- #140 sections O/P approve all four Backpack/Portable Furnace recipes, including the chest-based normal Backpack upgrade. No new ingredient/layout question is needed. Implementation and FUTURE Quest activation remain separately gated.
- Latest #152 is no longer docs-only: twelve accessory PNG/model/definition sets exist; identities/32x32 size are already approved, final appearance pending.
- Approved PNG masters remain immutable; Flame Sword runtime model/validator repairs do not authorize changing artwork.
- Bounty exploration/actual-strength count fixes, #153 catalyst conservation investigation, shared exact-XP composition, missing #93 event tests and stale pricing text are engineering issues, not new gameplay choices.

The separate docs-only **Draft audit PR is already requested by this task and permitted by AGENTS.md**; it needs no additional conversational approval to create. It must remain Draft. After reporting, stop and await the owner's next authorization.

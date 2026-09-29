# AGENTS.md

## Purpose

This file defines how coding agents should behave while working in this repository.

It does not define gameplay design, balance values, content rosters, or implementation status.

For current design authority, start from:

`docs/CONTENT_AUTHORITY_INDEX.md`

---

## 1. Core Principle

**WHAT = Human**  
**HOW = Agent**

Humans decide what the project should do.

Agents may independently decide how to implement an already-defined requirement, provided the implementation remains within the current task scope and current authority.

Do not introduce new design decisions while solving implementation problems.

---

## 2. Authority

Before implementing gameplay or content behavior, consult:

`docs/CONTENT_AUTHORITY_INDEX.md`

Use the current authority or reconciliation document routed from that index.

Historical, superseded, checkpoint, checklist, audit, and recovery documents may provide context, but they must not override current authority.

If a historical document conflicts with current authority, follow current authority without escalation.

If `docs/CONTENT_AUTHORITY_INDEX.md` does not route to an authority that defines a design decision required by the task, or if the routed authority is incomplete on that decision:

1. identify the missing or incomplete requirement,
2. explain why it is required to continue,
3. stop and ask for a decision.

Do not invent missing gameplay behavior, content, parameters, balance values, progression rules, controls, or other design requirements.

Existing code behavior is not automatically design authority.

If two documents that both appear to be current authority conflict in a way that affects the task:

1. identify the conflicting requirements,
2. explain their effect on the current task,
3. stop and ask for a decision.

Do not resolve a genuine authority conflict by guessing, choosing the newest date, copying current code behavior, or selecting the interpretation that is easiest to implement.

---

## 3. Agent Authority

### AUTO

Agents may independently:

- choose implementation details,
- fix compilation errors,
- fix test failures caused by the current task,
- correct imports or API usage,
- fix clear implementation defects within task scope,
- perform formatting or lint corrections,
- make the minimum dependency repair required for the task to function.

These actions do not require approval if they do not change intended design.

### STOP & ASK

Stop and request a decision when:

- current authority conflicts,
- required design authority is missing or incomplete,
- requirements allow multiple materially different gameplay outcomes,
- completing the task requires expanding its intended scope,
- an existing approved design would need to change,
- acceptance criteria cannot be satisfied without making a new design decision.

### NEVER WITHOUT APPROVAL

Do not independently:

- change current design authority,
- change approved gameplay requirements,
- perform destructive or broad repository changes,
- perform unrelated large refactors,
- merge branches,
- move or modify trusted checkpoints,
- modify `main`,
- delete branches,
- perform other irreversible baseline changes.

---

## 4. Bounded Auto-Repair

When an ordinary implementation, compilation, or test failure occurs, attempt to diagnose and repair it autonomously.

Do not escalate ordinary engineering problems immediately.

For the same underlying failure, make at most **3 bounded repair attempts**.

If the problem remains after those attempts, stop and provide a handoff in this format:

### Repair Failure Handoff
- **Failure:** what is still failing
- **Evidence:** relevant error/test/build result
- **Attempts:** what was changed in each repair attempt
- **Current state:** what works and what remains broken
- **Next decision:** whether human input is required, and why

Include only the relevant error-log excerpt or summary. Do not dump large raw logs unless specifically requested.

Do not continue an unbounded repair loop.

---

## 5. Task Scope

Only make changes necessary to satisfy the current task and its acceptance criteria.

Do not opportunistically fix unrelated problems merely because they were discovered during the task.

If an out-of-scope defect directly prevents completion of the current task, the minimum necessary dependency repair is allowed.

Otherwise, leave the unrelated issue unchanged and report it as an out-of-scope finding.

Do not perform speculative cleanup or refactoring simply because it appears beneficial.

---

## 6. Validation and Completion

Run validation appropriate to the changed scope.

Examples include:

- compilation/build validation,
- relevant automated tests,
- registry or resource-reference validation,
- command/runtime checks where automatable,
- other checks directly required by the task.

A successful build alone does not prove that every kind of change is validated.

If a required validation could not be executed, report it as:

`NOT RUN`

and state why.

Do not report unexecuted validation as passed.

Human-perceptual or interaction-dependent checks such as gameplay feel, VFX quality, UI readability, or control feel may remain:

`LIVE VALIDATION PENDING`

Do not repeat previously completed validation without reason. Re-run it when the current changes could invalidate the earlier result.

Keep implementation and validation status distinct:

**Implemented does not automatically mean validated.**

Completion reports should normally contain only:

- Changed
- Validation
- Live validation
- Out-of-scope findings

Do not create new completion-report documents unless the task specifically requires one.

---

## 7. Git and Trusted Baselines

Agents may commit normally on a task/work branch when needed.

The following require explicit approval:

- modifying `main`,
- moving or modifying a trusted `stage-*/checkpoint`,
- merging,
- deleting branches,
- other changes to trusted repository baselines.

Task completion does not imply permission to merge.

Creating a PR should occur only when required by the task or explicitly approved.

---

## 8. Keep Governance Simple

Do not create additional authority, status, backlog, checkpoint, defect, or completion documents merely to describe work that can be represented by:

- current authority documents,
- the current task,
- Git history,
- CI/test results,
- the completion report.

Prefer the smallest durable source of truth necessary.

---
name: pr-first-minimal-reporting
description: >-
  Minimize conversation-token overhead in Claude, Codex and ChatGPT development
  tasks by recording substantive progress and final acceptance in the existing
  GitHub PR body and emitting only tiny chat acknowledgements.
  Use when a task has an authorized PR or a user explicitly requests PR-first reporting.
license: MIT
---

# PR-first minimal reporting

## Authority and safety
- This skill controls **report placement and verbosity only**, not coding, design, review depth, test coverage, or completion criteria.
- Follow repository `AGENTS.md`, `docs/CONTENT_AUTHORITY_INDEX.md` and the current task's authority, scope, STOP & ASK, and validation rules first. A Ponytail skill cannot override them.
- Do not change gameplay authority, alter existing review findings, bypass validation, merge, modify protected baselines, or create a PR without authorization.
- Never say `done`, `PASS`, or `sent` for an operation that has not actually succeeded. A pending CI run is not acceptance.

## Output routing
1. **Existing authorized PR is the reporting destination.** Put substantive intermediate progress, changed scope, evidence, tests, limitations, blockers and final acceptance in its **body**, not the chat. Preserve the original PR purpose, scope, links, decisions, reviewer-authored content and relevant historical context; update the owned reporting section in place rather than appending duplicate reports.
2. Keep the PR body useful for a future reader, but concise: use at most these headings when relevant: `Scope`, `Changed`, `Validation`, `Live validation`, `Blockers / Out of scope`. Prefer compact bullets or a small table **in the PR body only**. Record final HEAD, required CI state, concrete failure evidence and `NOT RUN` / `LIVE VALIDATION PENDING` accurately. Do not store raw logs or full diffs in the body; link to CI/commits.
3. **Intermediate chat**: normally **no update** unless the platform requires one. If an update is necessary, send one short signal only, e.g. `PR 본문 갱신.` or `PR 업데이트 중.` No status tables, step-by-step explanations, English progress narration, repeated test totals or implementation recaps.
4. **Final chat after PR body update succeeds**: only `PR 갱신 완료: <PR URL>.` (or `PR 생성 완료: <PR URL>.` if creating the PR was explicitly authorized). Do not reproduce the PR report in chat.
5. **Exceptions demanding action**: if a human decision is required, a required validation is blocked, or PR write failed, give the **minimum actionable** statement, e.g. `중단: 설계 A/B 충돌. 결정 필요. <PR URL>` or `PR 본문 갱신 실패: 권한 없음.` Never conceal the problem merely to save tokens.
6. **Without an accessible authorized PR**: do not create one solely for reporting. Keep a minimal factual completion/blocker reply that satisfies `AGENTS.md`; do not falsely claim the PR was updated. Link to an existing commit or task artifact where available.

## Operating pattern
- Start: identify the correct PR, branch, and current HEAD; read the PR body before editing.
- During work: batch meaningful state changes. Avoid rewriting the PR body for every command or test; write checkpoints when status materially changes or a handoff is needed.
- Before exit: re-read current PR metadata/body when possible; merge updates into the relevant section without erasing parallel agents' content; check updated body reflects the **actual final HEAD** and validation status.
- When multiple agents share a PR, prefer distinct clearly owned reporting sections; avoid racing overwrites. If safe concurrent edits cannot be established, report the conflict instead.
- A GitHub PR body is a durable report, **not** a substitute for running required tests or consulting authority.

## Example final replies
- `PR 갱신 완료: https://github.com/owner/repo/pull/123.`
- `CI 대기 중. PR 갱신: https://github.com/owner/repo/pull/123.`
- `중단: 권한 문서 충돌. 결정 필요: https://github.com/owner/repo/pull/123.`

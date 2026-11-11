# Stage 23-B Cloud Live Client QA Implementation Plan

**Base:** PR #105 HEAD 5682be107145b9e4f3f7c4acc6e6dd8835ebe01b
**Design:** docs/superpowers/specs/2026-10-16-stage23b-cloud-live-client-qa-design.md
**Execution:** inline implementation in a separate Draft PR; no subagents.

## Ordered steps

1. **Update the approved design.** Add the user's constraints for a Loom loading/package-boundary spike, visual PASS evidence, and four distinct interaction paths. Keep this update on the design-only PR.
2. **Create an implementation Draft PR.** Start a new branch from the verified PR #105 HEAD. Do not modify PR #105 or its branch.
3. **Prove the Loom QA boundary first.** Add the smallest separate QA mod/source set that logs its development client initialization. Run the existing Loom development client and assert the log appears. Build the production remap JAR and assert the QA mod ID, QA entrypoint, and QA package are absent. Do not proceed to scenarios until both checks pass.
4. **Add isolated scenario fixtures and result records.** Implement test-only client/server QA support, a scenario runner that isolates failures, structured status/input-path records, and before/after captures. Keep fixture setup distinct from Screen callbacks and OS events.
5. **Implement seven independent scenarios.** Add observable state/render checks for selector routing, large backpack icon and transfers, Flame Sword front/back views, Greatsword hit/miss and particles, Enchanting layouts, Portable Furnace item-backed progress, and Miner Lantern with its optional runtime.
6. **Add a separate CI workflow.** Reuse the existing deterministic client/server/Xvfb/llvmpipe setup and Gradle caches without editing the existing visual workflow. Pin and hash-check the optional LambDynamicLights test artifact. Ensure one blocked case does not stop independent cases.
7. **Run validation and inspect evidence.** Keep the 113 GameTests and all required CI. Run the seven cases, inspect artifacts for semantic visual evidence, and use LIVE PENDING where meaning cannot be verified. Do not repair product behavior if a defect reproduces.
8. **Report and stop.** Publish exact PR HEAD, CI URLs, evidence artifact paths, per-case status and interaction path, and all unverified work. Keep the PR Draft; do not merge or promote.

## Acceptance checks

- Minimal Loom dev-load and production-JAR exclusion checks pass before scenario implementation.
- No production product-code file or existing workflow file changes.
- OS XTest/xdotool, Screen callback, direct fixture, and human manual-play evidence remain separate.
- Screenshot writes, pixel statistics, and particle receipt logs alone never yield visual PASS.
- Missing X11 access or LambDynamicLights blocks only its affected path/case.
- Existing GameTest and mandatory CI results remain visible and are not weakened.

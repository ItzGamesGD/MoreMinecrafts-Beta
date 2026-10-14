# PR #76 Interaction Correction Implementation Plan

> **For agentic workers:** Execute inline on the existing PR #76 branch. Use test-first changes and leave PR #75 and PR #76 unmerged.

**Goal:** Correct the existing Research and Knowledge tree interactions, then validate the updated client screens at GUI scales 2, 3, and 4.

**Architecture:** Keep the existing canvas, catalog, and server purchase route. Put the exact frame rectangle math in pure shared geometry; track a click gesture separately from pan and purchase submission; render existing catalog descriptions, prerequisites, XP, materials, and knowledge progress in the tree tooltip. Reuse the existing client capture workflow.

**Tech Stack:** Java 25, Minecraft 26.3, Fabric, JUnit 5, existing GitHub Actions Xvfb/llvmpipe capture harness.

**Spec:** User request in `/workspace/attachments/1a2d4df0-76b5-44e2-97b3-c1cd9b1d9e58/Pasted text.txt`.

## Global Constraints

- Work on `feat/research-knowledge-ui-integration` from PR #76's re-fetched HEAD `2e32c1e1ba444711d6b038cde48d89a3fe47a36b`.
- Do not change PR #75, merge either PR, or alter progression data, costs, prerequisites, unlock rules, or server validation.
- Keep Research and Knowledge separate; Knowledge nodes never submit purchase requests.
- Keep Minecraft's vanilla advancement frames, icons, and connectors; preserve the existing right-expanding node layout.
- Do not report physical input verification unless the workflow exercises X11 input and records the result.

## Review Focus

- The drawn frame's outside pixel and adjacent-node gaps must not be clickable.
- A node gesture that starts or ends on different targets, crosses the drag threshold, or is doubled must not submit an unintended or duplicate purchase.
- Pending requests must clear through authoritative synchronization/action response, without client-side optimistic spending.
- Scale-4 top-left category controls and tooltips must remain inside the screen.
- Purchasable nodes must retain the unobtained vanilla frame; only completed nodes use the obtained frame.

---

### Task 1: Unify frame geometry and gesture purchase policy

**Files:** `ProgressionCanvasGeometry.java`, `ProgressionCanvasGeometryTest.java`, `ProgressionTreeCanvas.java`, `ProgressionBrowseScreen.java`, client interaction policy and tests if needed.

- [x] Add pure tests for rounded frame size and half-open screen bounds at min/default/middle/max zoom, 1-pixel outside points, gaps, and GUI logical coordinates.
- [x] Add pure tests for click target agreement, drag threshold, target mismatch, Knowledge no-purchase, and one pending Research purchase per ID/level.
- [x] Run focused tests and observe the expected failures.
- [x] Implement shared frame sizing/placement used by render and hit testing; preserve rounded center and use half-open bounds.
- [x] Implement single-submit purchase policy and clear pending request after the authoritative action result's revision is synchronized; call only the existing network request path after a clean left-click release.
- [x] Run focused tests and progression regressions.

### Task 2: Correct the browse screen controls and category navigation

**Files:** `ProgressionBrowseScreen.java`, locale JSON files, capture harness.

- [x] Verify catalog branch roster and first-node focus through the existing pure branch-order test plus client capture callbacks.
- [x] Replace the bottom cycling control with a top-left direct selector populated only by `ProgressionNodeLayoutCatalog.branches`.
- [x] Remove visible +, −, Fit All, Focus Selected, detail, and purchase controls; keep only the geometry fit helper covered by tests and selected-node focus used by tree navigation.
- [x] Audit all confirmation-screen call sites; remove the now-unused screen and raw client purchase helper.
- [x] Keep Back, wheel zoom, empty-space drag, and Knowledge unlock behavior.

### Task 3: Render full vanilla-style node tooltips and state distinction

**Files:** `ProgressionTreeCanvas.java`, `ProgressionScreenData.java` only if current records omit required real data, locale JSON files.

- [x] Verify tooltip source records through existing screen-data/catalog tests and client capture callback journeys for available, unavailable, and Knowledge entries.
- [x] Use localized catalog keys and registry-resolved `ItemStack`s with required/held counts; do not display internal IDs or fabricate undefined details.
- [x] Use the unobtained frame for purchasable nodes and obtained only for completed/unlocked nodes; retain restrained hover/selection outlines.
- [x] Use native adaptive wrapping and screen-edge positioning. Per-level effects for multi-level catalog entries are explicitly marked unspecified because current data has no per-level effect values.

### Task 4: Extend actual client interaction validation

**Files:** existing `VisualValidationHarness`, `capture_client.sh`, `.github/workflows/client-visual-validation.yml`.

- [x] Replace confirmation-flow journeys with one-click purchase and authoritative result checks; add geometry boundary tests and Knowledge/ineligible no-purchase assertions.
- [ ] Reuse Xvfb/X11 input (`xdotool` where available) for physical wheel, blank drag, inside/outside click, and one-click purchase; record each input path separately from callback validation. The existing runner documents that its SDL client has no discoverable visible X11 window; do not claim physical input until CI provides evidence.
- [ ] Capture original PNGs at 1280x720 for scales 2/3/4, including tooltip/materials, unavailable status, category selector, and post-purchase state.
- [x] Run `./gradlew clean build compileGametestJava runGameTest`: build and all 33 required GameTests passed. Pending CI capture and artifact image review; physical input remains unverified.

### Task 5: Update PR #76

- [ ] Review the final diff against this plan and the original acceptance criteria.
- [ ] Commit and push the correction to the existing PR #76 branch after tests/captures; verify remote HEAD and CI/artifact URLs.
- [ ] Update PR #76's description with final implementation and verification results. Do not merge it or PR #75.

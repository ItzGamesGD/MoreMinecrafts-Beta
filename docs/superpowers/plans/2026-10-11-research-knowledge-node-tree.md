# Research and Knowledge Node Tree Implementation Plan

**Goal:** Replace the paginated Research and Knowledge button lists with deterministic, seven-discipline pan-and-zoom node canvases, while preserving all catalog data, eligibility semantics, purchase behavior, and the approved selector.

**Architecture:** Keep existing catalog and purchase APIs authoritative. Add presentation-only manifest parsing and pure geometry/camera policy with headless tests; render those layouts through a client canvas component. Adapt the current browse and confirmation screens for responsive navigation and compact overlays. Validate interaction paths through existing JUnit coverage and the client visual validation harness.

**Tech Stack:** Java 25, Minecraft/NeoForge client GUI and item rendering APIs already used by the repository, JUnit 5, JSON resources, PNG GUI textures, Gradle, existing visual validation shell/CI harness.

**Spec:** [Research and Knowledge Node Tree Design](../specs/2026-10-11-research-knowledge-node-tree-design.md) and [node layout manifest](../specs/2026-10-11-research-knowledge-node-tree-manifest.json).

## Global Constraints

- Do not modify the approved selector screen, its texture, hitboxes, or routing.
- Do not change `KnowledgeCatalog`, `ResearchCatalog`, purchase rules, server validation, saved IDs, rewards, costs, caps, triggers, or balance to make the tree work.
- V3 `prerequisitesDefined=false` remains unresolved and unavailable. V4 knowledge requirements retain any-one (OR) semantics.
- Branches and connectors are presentation metadata only. Do not create purchasable roots or encode visual edges in purchase requests.
- Keep Knowledge and Research separate; preserve the existing confirmation and cancellation behavior.
- Render Korean text with Minecraft fonts and `Component`s; PNG assets contain only non-text visuals.
- Add automated checks and perform live screenshot review at GUI scales 2, 3, and 4 before claiming visual approval. Do not merge or edit `main` as part of this plan.

## Review Focus

- Layout and source records join one-to-one by stable IDs, and runtime purchase inputs remain sourced only from existing progression data.
- At 1280×720 / GUI Scale 4 (about 320×180 logical pixels), canvas-first mode exposes back, discipline navigation, canvas controls, details, close, confirm, and cancel without clipping.
- A large discipline opens with readable selected-node focus when full fit would shrink nodes below the specified minimum; Fit All remains explicit, and branch/node navigation reaches offscreen content.
- Icon IDs are resolved through the item registry; invalid IDs fall back to `minecraft:book` with a node-specific diagnostic.
- Selection, focus, zoom, thematic branch choice, and details never initiate purchase; confirm/cancel issue exactly the existing request behavior.

---

## Task 1: Add presentation layout catalog and integrity tests

**Files:**
- Create: `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionNodeLayoutCatalog.java`
- Create: `src/main/resources/assets/moreminecrafts/progression/node_layout.json`
- Create: `src/test/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionNodeLayoutCatalogTest.java`
- Reference: `docs/superpowers/specs/2026-10-11-research-knowledge-node-tree-manifest.json`
- Reference only: existing `ResearchCatalog`, `KnowledgeCatalog`, and discipline IDs

**Work:**
1. Write failing tests for exact 70 V3, 17 V4, and 70 knowledge roster coverage; uniqueness; valid discipline and branch references; valid V4 knowledge-reference IDs; required explicit icon IDs and `minecraft:book` fallback.
2. Include source parity checks for IDs and immutable gameplay fields (tier, level cap, costs/material quantities/compression, conditions/targets/reward keys, prerequisite values and V4 OR meaning). Assert V3 unresolved records remain unavailable.
3. Add a presentation-only JSON resource generated from the approved manifest. It contains node ID, screen/catalog kind, discipline, branch/order, fixed coordinate, icon ID, and fallback policy. It contains no purchase rules or unlock decisions.
4. Implement an immutable parser/resolver that fails clearly on duplicate/missing/extra IDs, invalid references, or malformed coordinates and returns stable ordered presentation records. It must not produce or mutate catalog/purchase data.
5. Run the focused JUnit test and confirm it passes; run the existing progression tests.

**Verification:**
- `./gradlew test --tests 'dev.itzgamesgd.moreminecrafts.progression.ProgressionNodeLayoutCatalogTest'` — expected: all roster, reference, icon, and parity assertions pass.
- `./gradlew test --tests 'dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenDataTest'` — expected: existing screen-data behavior remains unchanged.

## Task 2: Implement responsive canvas geometry and camera policy

**Files:**
- Create: `src/main/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionCanvasGeometry.java`
- Create: `src/test/java/dev/itzgamesgd/moreminecrafts/progression/ProgressionCanvasGeometryTest.java`
- Reference: manifest coordinate/bounds specification

**Work:**
1. Write failing pure tests for deterministic bounds, padded collision rejection, scale-aware logical viewport sizing, canvas-first breakpoint at 640×360, readable initial fit threshold 0.75, selected focus, 0.1–1.5 zoom clamp, cursor-anchored zoom, pan limits, world/screen inverse mapping, and viewport clipping bounds.
2. Implement immutable bounds and camera-transform helpers with no Minecraft client dependencies so the policy can be tested headlessly.
3. Encode initial camera policy: use full fit when node size remains at least 27 logical pixels for research / 24 for knowledge; otherwise center the selected node at 0.75×. Explicit Fit All may use a smaller overview scale. Focus Selected centers at at least 0.75×.
4. Cover navigation into large content: branch-first selection, nearest directional node selection, previous/next order and focus recenter. Do not derive purchase state or connections from geometry.
5. Run focused geometry tests and existing progression tests.

**Verification:**
- `./gradlew test --tests 'dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometryTest'` — expected: all layout, camera, and responsive-policy cases pass, including a tree whose full fit is below 0.75×.
- `./gradlew test` — expected: all existing and new common-source tests pass.

## Task 3: Build the clipped node canvas and node rendering

**Files:**
- Create: `src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionTreeCanvas.java`
- Modify: `src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java`
- Create: `src/main/resources/assets/moreminecrafts/textures/gui/progression/tree_background.png`
- Create: `src/main/resources/assets/moreminecrafts/textures/gui/progression/node_borders.png`
- Create if GUI primitives are insufficient: `src/main/resources/assets/moreminecrafts/textures/gui/progression/branch_line.png`

**Work:**
1. Introduce a canvas component that accepts the already-resolved layout and current existing screen rows, keeping camera, selection, hover, and branch selection as view state only.
2. Render branch labels and subtle spine/spur paths from the layout. Do not draw speculative node-to-node prerequisite edges.
3. Resolve each configured icon ID through the item registry. Render the item sprite; for missing/unrenderable entries render `minecraft:book` and log the node ID and invalid configured ID once per node.
4. Draw individual research/knowledge frames and states, Minecraft-rendered tooltip/title text, clipped canvas content, and connector/node hover regions. Keep canvas controls outside the scissor and restore scissor state even on render failures.
5. Implement input handling: select-on-click, drag empty canvas to pan, node drag does not pan, middle drag alternate pan, cursor-centered wheel zoom, keyboard direction focus, Enter selection, branch picker, Fit All, Focus Selected, and discipline transition reset.
6. Keep purchase and knowledge unlock decisions read from existing rows/state; node renderer does not calculate eligibility.

**Verification:**
- `./gradlew compileJava compileClientJava` — expected: common layout and client canvas compile against repository Minecraft APIs.
- Add/extend pure interaction tests in Task 2 where behavior can be isolated; expected: node-vs-background drag and selection/focus state transitions pass without initiating purchase.

## Task 4: Integrate discipline screens, responsive details, and purchase confirmation

**Files:**
- Modify: `src/client/java/dev/itzgamesgd/moreminecrafts/client/ProgressionBrowseScreen.java`
- Modify: `src/client/java/dev/itzgamesgd/moreminecrafts/client/ResearchPurchaseConfirmScreen.java`
- Modify: `src/main/resources/assets/moreminecrafts/lang/en_us.json`
- Modify: `src/main/resources/assets/moreminecrafts/lang/ko_kr.json`
- Reference only: selector implementation and existing route

**Work:**
1. Replace paginated list composition with the canvas while keeping Knowledge and Research as separate screens and retaining the existing seven discipline IDs/order.
2. At normal logical sizes, show canvas and selected-node detail side by side. Below 640×360, remove the simultaneous split, give the canvas the content area, and show details as a scrollable full-screen overlay with pinned close/context action.
3. On compact screens, implement horizontally scrollable discipline tabs with previous/next controls, an active-discipline label, and 20×20 logical minimum controls outside the canvas scissor. Keep footer controls and back reachable at 320×180.
4. Render knowledge condition/progress/reward/unlocked detail and research tier/level/cap/purchase state/cost detail from current screen data. Render V3 unresolved state and V4 any-one-of-listed-knowledge wording exactly.
5. Feed research purchase into the existing confirmation screen and existing request path. Make the confirmation content scroll when compact while confirm/cancel remain pinned; preserve close/cancel no-request behavior and confirm one-request behavior.
6. Add all Korean and English translation keys for branch names, labels, states, details, legend, navigation, accessibility text, and compact controls using normal translatable components. Keep all text out of PNGs.

**Verification:**
- `./gradlew test` — expected: source-parity and geometry tests still pass.
- `./gradlew compileJava compileClientJava` — expected: both screens compile and localization resources load.
- Existing visual harness journeys for Knowledge, Research, confirmation cancel, and confirmation confirm — expected: existing purchase flow remains intact and selector route is unchanged.

## Task 5: Add frame assets, update visual validation, and capture scales 2–4

**Files:**
- Create: `src/main/resources/assets/moreminecrafts/textures/gui/progression/tree_frame.png`
- Create/refine: progression PNG assets from Tasks 3–4, non-text only
- Modify: `src/client/java/dev/itzgamesgd/moreminecrafts/client/VisualValidationHarness.java`
- Modify: `scripts/visual_validation/capture_client.sh`
- Modify if needed: `.github/workflows/client-visual-validation.yml`

**Work:**
1. Author non-text frame/background/border assets with slice margins and state variants documented in a nearby asset note or constants. Do not edit selector assets.
2. Extend the harness to cover seven tabs, keyboard focus, branch selection, Fit All, Focus Selected, drag/zoom clipping, compact details overlay, confirmation scroll, and pinned confirm/cancel controls.
3. Make the capture harness exercise Research and Knowledge at GUI scales 2, 3, and 4, including 1280×720 at scale 4, plus a large canvas whose overview zoom is below the legibility threshold.
4. Run automated validation, then launch the actual Minecraft client and capture screenshots at scales 2/3/4. Review clipping, responsive controls, node legibility, Korean font rendering, icon recognition, tooltip placement, and purchase/cancel screens.
5. Record screenshot paths and findings in the PR review materials. Do not label the visuals approved until actual Minecraft screenshots are inspected.

**Verification:**
- `./gradlew test` — expected: all roster, field-preservation, reference, collision, determinism, camera, and input policy tests pass.
- `./gradlew build` — expected: common/client compilation, resources, and existing project checks succeed.
- `scripts/visual_validation/capture_client.sh` with GUI scales 2, 3, and 4 — expected: actual Research and Knowledge captures exist for each scale, with compact Scale 4 controls reachable.
- Manually inspect generated Minecraft captures — expected: no visual approval claim until all required screens/scales and compact purchase flow are reviewed.

## Implementation stop points

- If a source catalog row does not match the approved manifest, stop and report the mismatch; do not “fix” it by changing gameplay data or layout authority.
- If a configured item ID is missing, use the specified book fallback and diagnostic. Do not create custom icons without a separate approved asset decision.
- If 320×180 cannot preserve reachable controls, keep canvas-first and adjust responsive composition or add an alternate details view; do not shrink controls below the specified minimum.
- V3 remains unresolved and non-purchasable throughout this work. Any request to change that status requires separate gameplay authority and is outside this plan.

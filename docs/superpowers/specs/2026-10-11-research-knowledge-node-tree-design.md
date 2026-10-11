# Research and Knowledge Node Tree Design

**Status:** User-approved architecture; specification prepared for review 2026-10-11
**Base:** Draft PR #75 current head `d1f113720cf7d71720450d4709b3834b137d43f4`
**Scope:** Replace the paginated knowledge/research button lists with vanilla-advancements-inspired node canvases. This is a presentation and navigation change. It does not change progression data or gameplay.

## 1. Authority and invariants

Use the catalogs and current runtime purchase path in PR #75 for identifiers, discipline membership, costs, levels, current eligibility and progress. The machine-readable inventory and proposed visual placement are in [the node layout manifest](2026-10-11-research-knowledge-node-tree-manifest.json). The manifest is client-side design metadata: it is not a progression catalog and must not be sent to, or read by, server purchase validation.

Preserve these invariants:

- Knowledge and research remain separate screens, reached through the existing selector. The approved selector screen, hitboxes, texture `assets/moreminecrafts/textures/gui/selector/approved_full_panel.png`, and selector routing are outside this change.
- Keep all seven discipline IDs and existing order: Resources (`resources`), Production Engineering (`production`), Living Skills (`living`), Combat (`combat`), Ecology (`ecology`), World Studies (`world`), and Alchemy and Magic (`alchemy`). Korean labels continue to come from Minecraft translation components.
- Preserve every node ID, knowledge counter/condition, reward key, research catalog version, tier, level cap, per-level price, material quantity and compression-unit value. This task adds no research, cost, level, effect, knowledge requirement, or purchase rule.
- V3 `prerequisitesDefined=false` means unresolved and non-purchasable. Empty V3 requirement arrays do not mean “no requirements.” Keep the unavailable status and show the unresolved state in details.
- V4 research eligibility is **any one of** its listed knowledge numbers (OR). `researchPrerequisites` is empty for all 17 V4 additions in the current catalog. Display the source requirement; do not reinterpret its set as AND.
- A thematic branch or connector is organizational metadata only. It never makes a node purchasable, blocks one, implies a tier gate, or participates in server validation. No artificial purchasable roots are added. `combat_root` and `ecology_root` are included because those exact paid entries already exist in V4.
- Preserve server-authoritative purchase validation/deduction, state synchronization, and the existing explicit confirmation/cancel flow. Selecting a node never purchases it.
- Korean text is rendered by Minecraft fonts from translatable `Component`s. PNGs contain non-text visuals only.

## 2. Current-source inventory

At the fetched PR head, source counts are:

| Catalog | Count | Source | Eligibility/reward status represented in the manifest |
|---|---:|---|---|
| Research V3 | 70 | `data/moreminecrafts/research/research_v3.json` via `ResearchCatalog` | All entries have `prerequisitesDefined=false`, empty requirement arrays and a source description stating purchase is disabled until requirements and reward links are resolved. All caps and every level cost are preserved. |
| Research V4 | 17 | `ResearchCatalog.java`, `ResearchRewards.java` | Every entry has a one-level cost, an any-of knowledge prerequisite set, and a mapped reward effect. |
| Knowledge | 70 | `KnowledgeCatalog.java` | Preserves number/ID, discipline, Korean name, condition type/key/target, reward key and unresolved flag. Progress values remain player state from the existing progression API. |

The V3 source ledger totals 178 level purchases and 130,360 raw XP. The 17 V4 additions total 17 purchases and 16,910 raw XP. The combined source inventory therefore contains 87 nodes, 195 level purchases and 147,270 raw XP. These are source-ledger checksums only; the UI task does not change them.

The manifest enumerates each record, not a sample. V3 costs retain each level’s raw XP and each material’s item ID, count, compressed units, and source material name. V4 costs and effect IDs/kinds/values come from their source catalog/reward table. Knowledge eligibility and rewards are descriptive node data; they are not paid purchases.

### Research thematic branch mapping

Branch order and node order are fixed in the manifest. These labels organize browsing only; node order is not a prerequisite sequence.

| Discipline | Branches in canvas order |
|---|---|
| Resources | Geology and quarrying; Forestry; Field mobility |
| Production Engineering | Smelting and fuels; Crafting and maintenance; Woodworking; Storage and logistics |
| Living Skills | Cultivation and soil; Animal care; Fisheries |
| Combat | Existing discipline foundation (`combat_root`); Melee; Ranged; Defense; Thrown weapons |
| Ecology | Existing discipline foundation (`ecology_root`); Overworld hostiles; Nether hostiles; Aquatic hostiles; Rare and boss mobs; Tracking and capture |
| World Studies | Travel and transport; Archaeology and expedition; Trading; Mobility |
| Alchemy and Magic | Brewing basics; Potion handling; Effects and catalysts; Experience; Advanced brewing |

### Knowledge thematic branch mapping

Knowledge uses separate branch assignments from research even when the topic overlaps. Every branch remains inside the node’s existing discipline.

| Discipline | Branches in canvas order |
|---|---|
| Resources | Geology; Forestry; Flora and soil; Rock classification; Extraction and tools |
| Production Engineering | Smelting and alloys; Crafting and maintenance; Storage and automation |
| Living Skills | Cultivation and soil; Livestock and bees; Fishing; Food |
| Combat | Melee; Ranged; Defense and thrown weapons; Movement and ammunition |
| Ecology | Overworld hostiles; Nether and aquatic; Rare and boss mobs |
| World Studies | Biomes and caves; Travel; Archaeology and structures; Villager trade; Expeditions |
| Alchemy and Magic | Brewing and potions; Effects and ingredients; Advanced brewing |

### Rewards and source gaps

The V3 runtime catalog does not define a reward field. Its current localized description says that reward links remain unresolved, and V3 purchases are disabled. The manifest records this as `rewardStatus: unresolved_in_runtime_source` rather than fabricating or borrowing a reward. V4 effects are listed explicitly. Knowledge rewards remain the source `rewardKey`; the existing detail presentation resolves that key as it does today.

This spec therefore preserves known values and exposes existing unresolved state. It does not authorize resolving V3 rewards or prerequisites. A future gameplay-authority task must settle those before a V3 node may be offered as purchasable.

## 3. Coordinate and connector specification

Each discipline tab owns an independent canvas for the selected mode (Knowledge or Research). Coordinates use unscaled canvas logical pixels and are independent of window size, monitor DPI, and GUI scale.

The manifest fixes branch order and node order. For branch lane `b` and node index `r` within that lane, the node center is:

```text
centerX = 64 + 192 * b
centerY = 64 + 72 * r
```

Research node bounds are 36×36 logical pixels; knowledge node bounds are 32×32. Their different size and icon treatment preserve the approved distinction between knowledge and paid research. The 192-unit lane spacing and 72-unit row spacing leave at least 156 horizontal and 36 vertical logical pixels between adjacent node bounds. Branch headers are centered on the lane at logical y=20, above the first node. Their labels use each manifest row's deterministic `branchTitleKey`; implementation supplies Korean and English translation entries. The largest lane determines scrollable canvas height; the last lane determines width. Canvas bounds start at `(0,0)` and end 64 logical pixels beyond the greatest node center plus that node's half-size. No coordinate is derived from localized text, hash iteration, current unlock state, or purchase state.

Visual grouping uses at most one muted orthogonal branch spine per lane at `spineX = centerX - 40`, with a short spur from the spine to each node's left edge. The spine runs from the first node center Y to the last node center Y. Single-node lanes may omit the spine. These paths connect nodes to a non-clickable thematic branch guide, not to other nodes. No speculative node-to-node or cross-branch edges are defined in the initial manifest. If a later visual review proposes such an edge, it must be tagged `THEMATIC_ONLY`, be visibly distinct from requirement status, and remain absent from gameplay data. Show a concise legend on the research screen: “Branch lines group related topics; they are not purchase requirements.” Render it in Korean/English using translation keys.

Node rectangles are collision-checked after padding by 8 logical pixels on every side. Branch labels, canvas controls, the details panel, and the viewport are separate layout regions and must not overlap. Reordering or adding a node requires an intentional manifest update; the same manifest must yield the same positions on every launch.

## 4. Screen architecture and interaction

### Screen composition

- Keep the existing selector and its approved image untouched.
- Keep Knowledge and Research as separate screens. Keep the seven existing discipline tabs, in current order, on both screens.
- Retain a vanilla advancements visual language: textured frame, restrained canvas backdrop, framed node states and connected branch presentation. Avoid a new dashboard style.
- Divide available GUI logical width between a clipped canvas (about 62%) and selected-node details (about 34%), with remaining space for gutters. Keep the title, discipline navigation, and pan/zoom/recenter controls outside the canvas scissor. Use responsive min/max bounds rather than assuming one physical resolution.
- The canvas initially frames its discipline’s node bounds at a fit scale capped by the zoom range. Camera transform is `screen = viewportOrigin + (world - pan) * zoom`; inverse transform is used for hit testing.

### Input and navigation

- Clicking a discipline tab switches the canvas, selects that discipline’s first node when present, and resets camera fit. Knowledge and Research screen navigation remains as currently routed.
- Left click on a node selects it; the click does not pan and does not buy. Hover shows a compact tooltip. Selected-node details persist while the pointer moves away.
- Left dragging empty canvas pans. A node drag does not start a pan. Middle-button drag may pan as an alternate mouse gesture. Recenter/fit control restores the initial framing.
- Mouse wheel zooms around the cursor using fixed 1.1 steps, clamped to 0.5×–1.5×. Zoom changes preserve the world point under the cursor. Panning is clamped so the canvas cannot be lost entirely.
- Scissor/clipping bounds use the canvas viewport in GUI logical coordinates after layout and camera calculations. Apply clipping to branch lines, nodes, and hover surfaces; restore clipping in a `finally` path. Do not clip tabs, controls, details, or Minecraft tooltips.
- Keyboard focus reaches discipline tabs, canvas nodes, recenter, back and purchase controls. Left/right changes discipline when tab-focused; arrow keys move node focus to the nearest node in that direction; Enter selects; Escape follows the existing screen close/back behavior. Focus movement never purchases.
- At GUI scales 2, 3, and 4, hit targets, clipping, tooltip placement and details remain usable. GUI-scale testing is live visual validation, not a claim made by code-level tests.

### Node states and details

Knowledge nodes show their individual icon, achieved/not-achieved state, and condition progress. The selected details show translated title, discovery condition, current progress/target, reward, and unlocked status. Knowledge remains free and auto-unlocks under the existing runtime; there is no purchase control.

Research nodes show individual icon, tier (including the explicit `UNSPECIFIED` value where supplied), current level / level cap, purchase status and next-level cost. Selected details show translated title/description, exact current eligibility status, known prerequisite display, next-level raw XP and material quantities, and purchase state. For V3, show unresolved eligibility/reward status; the purchase control remains disabled. For V4, show all qualifying knowledge IDs as OR (“any one”), plus the current known/unknown status of each. Do not change existing level progression or cost-compression display.

The research purchase control calls the existing confirmation screen with the selected row. Confirmation remains a distinct second step, and cancel, close, and confirm retain their present behavior and server request path. If live state changes, refresh displayed node availability from existing synchronized progression data; the server remains authoritative.

## 5. Rendering and assets

All text—including Korean labels, branch names, costs, conditions, statuses, legends and tooltips—uses Minecraft `Font` and translatable `Component` rendering. Do not bake localized copy into PNG files.

PNG textures are limited to non-text elements:

| Asset | Purpose |
|---|---|
| `textures/gui/progression/tree_frame.png` | Advancements-inspired outer frame, sliced/repeated by the renderer as needed. |
| `textures/gui/progression/tree_background.png` | Low-contrast tileable canvas backdrop. |
| `textures/gui/progression/node_borders.png` | Sprite sheet for knowledge/research node border and state treatments (locked, available/unlocked, selected, hovered, purchased/maxed, unresolved). No embedded labels. |
| Existing vanilla/mod item PNG textures through item sprites | Per-node non-text icons rendered as item stacks; icon-to-node mapping is presentation metadata. No Korean image labels. |
| Optional `textures/gui/progression/branch_line.png` | Subtle repeating connector segment if a vanilla-compatible line cannot be rendered cleanly with GUI primitives. No text. |

Do not add a new background, node-icon atlas, border variant, or art style to the approved selector. Individual node icon mappings are intentionally unresolved in the manifest; icon selection must use an existing representative item sprite and cannot change mechanics or imply eligibility.

## 6. Proposed client data boundary

Keep authoritative data in the existing `ProgressionScreenData`, `KnowledgeCatalog`, `ResearchCatalog`, `ResearchPurchaseRules`, synchronization payloads and purchase service. Add only client-side layout metadata and rendering responsibilities:

1. A layout manifest maps stable IDs to discipline, thematic branch, fixed order, center coordinates and visual connector membership.
2. A pure layout resolver combines that metadata with current screen rows. It validates one-to-one ID coverage and returns positioned nodes and connector paths; it never calculates purchase eligibility.
3. A canvas view owns camera pan/zoom, clipping and pointer-to-world conversion.
4. A node renderer draws textures/icons and state indications; click handling selects only.
5. The existing detail and confirmation flow consumes the selected existing row data.

The layout resolver must fail closed on a missing/duplicate ID or unknown branch rather than silently omit a node or assign a guessed location. It must not send branch, coordinate, connector, tier classification, or visual unlock state to the server.

## 7. Automated checks and acceptance

Add automated checks for:

1. **Roster integrity:** manifest has exactly 70 V3 IDs, 17 V4 IDs and 70 knowledge IDs; IDs are unique and equal the source catalogs exactly.
2. **Source preservation:** every manifest discipline, tier, cap, level count, cost, material count/compression value, condition/target/reward key and prerequisite field matches its source record. V4 prerequisite comparison asserts any-of semantics. V3 unresolved state stays false/disabled.
3. **Reference validity:** branch and discipline references exist; all V4 knowledge requirement IDs exist; no visual link is accepted as a gameplay requirement or sent in a purchase payload.
4. **Bounds and collision:** every node lies in its discipline canvas bounds; padded node rectangles do not overlap; no branch spine/spur crosses a node interior or escapes its viewport after clipping.
5. **Determinism:** repeated resolution from identical manifest/catalog input gives byte-for-byte identical ordered coordinates and connector paths; localized strings and state changes do not reorder nodes.
6. **Navigation and input:** discipline switching, node selection, arrow focus, enter, escape/back, pan, cursor-centered zoom, recenter, clipping and node-vs-background drag hit testing.
7. **Purchase flow regression:** selecting a node does not purchase; valid V4 selection opens the same confirmation; cancel/close send no request; confirm sends one request; V3 unresolved stays disabled; eligibility remains server-validated.

Live acceptance must capture Research and Knowledge screens at GUI scales 2, 3 and 4 in Minecraft. Review clipping, legibility, Korean font rendering, icon recognition, node density and pan/zoom at those scales. Until these captures are inspected, visual approval is pending.

## 8. Unresolved decisions

These are presentation/integration gaps, not permission to change gameplay:

- **Per-node icon mapping:** all inventory rows currently mark `iconMapping: UNRESOLVED`. Choose an existing item/ block sprite for each node, or document deliberate reuse. No custom text artwork.
- **PNG state treatment details:** exact pixel dimensions, palette and slice margins for frame/background/border assets need asset authoring and Minecraft captures. Keep assets non-text and consistent with vanilla advancements.
- **Fit and minimum responsive dimensions:** initial fit behavior is specified, but exact panel dimensions and minimum supported GUI window size need live testing at GUI scales 2–4.
- **V3 reward links:** runtime source explicitly leaves reward links unresolved and purchase disabled. This UI task must show that truth; a separate approved gameplay task is needed to resolve rewards and prerequisites.
- **V3 prerequisites:** remain unresolved and non-purchasable until authority is updated. The manifest’s empty arrays are retained as raw source data, not interpreted as “none.”
- **Knowledge entry 51:** its source `unresolved` flag remains true; preserve its unresolved condition/reward display.
- **Connector density:** initial design uses only subdued per-branch spines and node spurs; no inter-node edges. Any further connector proposal needs review against the non-prerequisite rule and captured screen readability.

## 9. Implementation readiness and exclusions

The screen architecture, source inventory, branch assignments, coordinate formula, no-overlap spacing, connector semantics, interaction model and test requirements are sufficiently specified for a later implementation plan. The icon mapping and exact asset geometry remain visual tasks; V3 eligibility/rewards remain gameplay-authority blockers only for making those nodes purchasable. This spec does not authorize resolving those blockers.

Excluded: gameplay or balance changes; changing any catalog/backend or saved identifier; inventing prerequisite edges; adding artificial purchasable roots; altering research tiers/costs/caps/rewards; changing knowledge triggers/rewards; selector edits; PR merge; main-branch edits; and visual-approval claims without Minecraft captures.

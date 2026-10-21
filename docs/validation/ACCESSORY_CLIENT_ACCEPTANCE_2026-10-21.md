# Accessory Client Acceptance Gate — 2026-10-21

## Target and CI baseline

- PR #144 is open and Draft at `2083cf37d163398c953995223be35f1d3ba50f02`; its base PR #143 remains at `f2e6e87fe95ee4454f4b365c569ecfefae619e9e`.
- Fabric CI #1012: PASS (build, unit tests, 107 GameTests, dedicated server smoke, generic client startup smoke).
- K5 Production Restart #282: PASS (existing two-process regression and accessory player persistence over three server processes).
- K5 Mapped Sources Probe #376: PASS.
- Generic client startup is not inventory-screen, pointer, or client synchronization evidence.

## Environment and available harness

The current workspace has Java 21 but no `DISPLAY`, `Xvfb`, `xvfb-run`, Minecraft launcher, or `xdotool`. There is no local real-client window to connect to or operate. I did not retry a client launch without a display.

The repository has `.github/workflows/client-visual-validation.yml` and `scripts/visual_validation/capture_client.sh`. The workflow installs Xvfb, Openbox, and xdotool, starts a dedicated server and real Fabric client, and captures native Minecraft render-target screenshots. Its existing journey targets progression screens; it does not open `InventoryScreen`. Its own script states the Minecraft 26.3 SDL window is not discoverable as a visible X11 window on that runner. Physical selector input is opt-in; the normal path uses synthetic in-client Screen callbacks. The workflow's current captures therefore cannot establish accessory pointer behavior or layout.

## Implementation inspected

- `AccessoryInventoryScreenMixin` sets inventory `imageWidth` to 284 and draws the added panel and five row backgrounds at fixed coordinates.
- `InventoryMenuMixin` adds five server menu slots at x=178 and y=26, 53, 80, 107, and 134, with category-filtered placement and shift-click routing.
- The accessory attachment uses a persistent codec and `syncWith(..., AttachmentSyncPredicate.all())`.
- These details are source inspection only. Without the InventoryScreen open in a real client, they do not prove that the recipe book, window sizes, GUI scaling, item rendering, or tooltips are correct.

No runtime code changes or defects are confirmed in this gate.

## Real-client scenarios

| Scenario | Result | Evidence boundary |
|---|---|---|
| Initial accessory state synchronization | BLOCKED | Attachment sync is configured; no real connected client assertion |
| Equip / unequip | BLOCKED | Server menu callbacks exist; no real client session |
| Cursor transfer / compatible swap | BLOCKED | Callback GameTests are not physical input |
| Shift-click in both directions | BLOCKED | Callback GameTests are not physical input |
| Reconnect synchronization | BLOCKED | Restart persistence test is not a network reconnect test |
| Nether / End transfer synchronization | BLOCKED | Server lifecycle GameTest only |
| Death / respawn synchronization | BLOCKED | Server lifecycle GameTest only |
| keepInventory true / false synchronization | BLOCKED | Server lifecycle GameTest only |
| Invalid category feedback | BLOCKED | Server rejection exists; no client feedback observed |
| Active-slot exclusivity | CODE VERIFIED | Server loadout policy test; client display/state not exercised |
| Rapid operations / replay / stale ghost slots | BLOCKED | No client packet replay or live synchronization test |
| Malformed client operation rejection | BLOCKED | No packet-level client test |

The PR #144 server-side checks remain useful CODE VERIFIED evidence, but are not promoted to real-client acceptance.

## Visual scenarios and screenshots

No accessory inventory screenshots were produced. All requested visual cases remain VISUAL PENDING:

1. Recipe book closed
2. Recipe book open
3. All five accessory slots occupied
4. Small GUI scale
5. Large GUI scale
6. Invalid placement feedback

No evidence is available to judge slot alignment, clipping, vanilla armor/offhand accessibility, crafting behavior, recipe-book overlap, tooltip placement, or GUI-scale behavior.

## Dedicated regression boundary

- Accessory item conservation in server menu callbacks: CODE VERIFIED by PR #144 GameTests.
- Backpack storage/death retention and vanilla armor, offhand, crafting, and recipe-book accessibility in a live inventory session: BLOCKED.
- The passing 107-test repository GameTest suite is not a substitute for those client regressions.

## Smallest practical next step

Reuse the existing client visual-validation runner, but add an accessory-specific fixture and native InventoryScreen capture stage for the six requested visual states. Keep screenshot capture distinct from pointer-input claims. The pointer and live synchronization gate needs a client environment where Minecraft's SDL window can be selected and driven, or an equivalent real-client harness with explicit network and physical-input evidence. Until then, mark the client scenarios BLOCKED and do not accept Batch 1 as fully client-verified.

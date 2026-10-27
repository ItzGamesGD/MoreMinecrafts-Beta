# Accessory Client Acceptance — 2026-10-22

## Target

- PR #145: https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/145
- Stacked on PR #144, verified base SHA `2083cf37d163398c953995223be35f1d3ba50f02`.
- Draft; not merged or marked Ready.
- Client fixture source: `src/gametest/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryInventoryClientGameTest.java`.
- Workflow: `.github/workflows/accessory-client-acceptance.yml`.

## Recovered client-validation precedent

The current workspace lacking a display was not a reason to stop: GitHub Actions provides a working software-rendered client environment.

| Historical work | Evidence | Findings |
|---|---|---|
| PR #68 | Commit `cbc3fa1ab028a967cb21b1de9801c35324eed91d`; checked-in screenshots in `docs/validation/t1-pr68/` | Native Minecraft screenshots were committed; CI passed. |
| PR #72 | Commit `0cc0a9eaf58290bbde2a4b45f68df101f8f7c6d2` | Xvfb/Mesa llvmpipe rendering approach; no separate Actions workflow was added there. |
| PR #73 | Commit `3a0cd8cc279c6419cf48e684a3d6df95765a4618`; [run 37965666797](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/37965666797), [artifact 11633831005](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/37965666797/artifacts/11633831005) | Successful Ubuntu client render and native screenshot capture using `.github/workflows/client-visual-validation.yml` and `scripts/visual_validation/capture_client.sh`. |
| PR #75 | Commit `d1f113720cf7d71720450d4709b3834b137d43f4`; successful earlier run [38056218131](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38056218131), scale-validation run [38102811374](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38102811374) | Workflow attempted GUI scales 2/3/4. Final scale-validation run failed; its scale loop is not counted as successful evidence. |
| PR #107 | Commit `dad054c7cb5e5d672a0ef96bfdeffc482e1c48f6`; [run 38885410011](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885410011), [artifact 12039442747](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/38885410011/artifacts/12039442747) | Successful native client screenshots with a connected test server. External XTest/xdotool pointer discovery was explicitly blocked because the Minecraft 26.3 SDL window was not discoverable on that runner. |

The proven runner used Temurin Java 25, Gradle Actions v4, Ubuntu, Xvfb (1280x720x24 in the earlier workflow), Openbox, Mesa DRI/GLX with `LIBGL_ALWAYS_SOFTWARE=1` and `MESA_LOADER_DRIVER_OVERRIDE=llvmpipe`, plus Mesa lavapipe Vulkan selection. It launched the real Fabric client and captured the Minecraft render target. The accessory workflow reuses this stack, sets Xvfb/client to 1920x1080 so GUI scale 4 is available, and invokes Loom's `runClientGameTest`.

Project versions: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.17.21, Java 25.

Fabric's `runClientGameTest`, `TestDedicatedServerContext`, `TestInput.setCursorPos`, `holdShift`, `holdMouse`/mouse input, and `context.takeScreenshot` are used. This is real Minecraft rendering and a real client-server test connection, with input simulated through Fabric's client test API. It is not external OS pointer input and not an independently launched dedicated-server executable.

## Current Actions evidence

At tested code SHA `afee564e3855e424eb7ed731d02881e97e20fe0a`:

- [Accessory Client GameTest Acceptance run 39697532507](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697532507): Java/client/GameTest compilation passed; Minecraft client launched, rendered screenshots, and ran the fixture; run failed at shift-click equip.
- [Artifact 12391104186](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697532507/artifacts/12391104186): native screenshots, client/server logs, and test world. Screenshot files include:
  - `0000_accessory-inventory-scale-2-recipe-book-closed.png`
  - `0001_accessory-inventory-scale-3-recipe-book-closed.png`
  - `0002_accessory-inventory-scale-4-recipe-book-closed.png`
  - `0004_accessory-inventory-recipe-book-open-all-five.png`
  - `0005_accessory-inventory-all-five-gui-scale-4.png`
  - `0006_accessory-inventory-invalid-category-placement.png`
- [K5 Production Restart run 39697532865](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697532865): PASS, including three-process accessory player persistence.
- [Fabric CI run 39697532989](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697532989): build PASS; server smoke was still running at time of this report.
- [K5 Mapped Sources Probe run 39697532720](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/39697532720): still running at time of this report.
- Earlier accessory render run 39697194264 also produced screenshots, but stopped on a fixture-only conservation assertion (the fixture intentionally contained one equipped and one inventory Star Covenant). This accounting error was corrected before run 39697532507; no product duplication was indicated.

## Scenario results

| Scenario | Result | Evidence |
|---|---|---|
| Actual Minecraft inventory rendering | PASS | Native render-target screenshots from run 39697532507. |
| Recipe book closed/open | PASS | Closed screenshots at scales 2/3/4; Client GameTest asserted the recipe component opened and closed; open screenshot captured. |
| Five accessory rows fit in the inventory screen | PASS, with art defect | The five row positions are visible and within bounds in screenshots. All accessory item sprites render missing-texture checkerboards because PR #144 has no accessory item models/textures. |
| GUI scales 2, 3, and 4 | PASS | Effective scale asserted and native screenshots captured; Xvfb/client resolution is 1920x1080. |
| Initial client equipment sync and stack metadata | PASS | Client attachment has all five accessories; custom name and enhancement component match server fixture values. |
| Cursor equip/unequip | PASS | TestInput clicks; client and server stacks checked with explicit item conservation. |
| Invalid-category placement | PASS | TestInput attempt is rejected; support slot stays empty, cursor retains the combat accessory, item count is conserved, and the rejection state is captured. |
| Shift-click equip | FAIL | With `holdShift` and simulated left click, the item moved to the cursor and did not equip. Log: `equipped=minecraft:air`, `cursor=moreminecrafts:ring_of_life`; test fails at this assertion. This is not claimed as a pass. Product quick-move behavior versus Client GameTest modifier delivery remains to be isolated. |
| Shift-click unequip | BLOCKED | The fixture stops on the failed equip assertion. |
| Client reconnect, dimension transfer, death/respawn sync, keepInventory sync | BLOCKED | Not exercised by this client fixture. The server-side three-process persistence workflow does not prove network resynchronization for those transitions. |
| Malformed/replayed packet resistance, rapid-click ghost state | BLOCKED | No packet replay harness was used. |
| Backpack, armor/offhand, crafting, recipe-book interactions | VISUAL PASS / interaction BLOCKED | Screenshots show the vanilla controls accessible and non-overlapping with the five rows. Physical interaction and dedicated backpack regression were not performed by this client fixture. |
| External OS-level pointer input | BLOCKED | No XTest/xdotool pointer claim. The historical runner evidence says the Minecraft SDL window is not discoverable for external pointer selection. |

No real-client screenshot is treated as proof of physical pointer input. `TestInput` interaction assertions are simulated in-process input delivered to the actual Minecraft client.

## Visual findings and acceptance

The recipe-book-open screenshot shows the full accessory panel without toast overlap after the fixture waits for transient notifications. The slot geometry, armor/offhand, crafting grid, and recipe-book area are visible. Accessory item sprites are magenta/black missing-texture checkerboards, confirmed in the native screenshots; artwork/model registration is a separate defect to resolve before visual acceptance. Tooltip placement was not verified.

**Batch 1 client acceptance: NOT READY.** Real client rendering, layout capture, initial synchronization, cursor movement, invalid-category rejection, and persistence CI are evidenced. Shift-click equip fails under the supported Client GameTest input attempt; shift-click unequip, external pointer input, reconnect/dimension/death client resynchronization, and accessory artwork remain open.

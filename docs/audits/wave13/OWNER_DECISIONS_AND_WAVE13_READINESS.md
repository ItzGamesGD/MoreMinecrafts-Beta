# Owner decisions and Wave 13 readiness (2026-10-27)

Status: audit record. It creates no design authority. Every item below is something the owner has to decide, or a limit on what the evidence shows.

## 1. Where things stand

| Area | State | Evidence |
|---|---|---|
| Wave 12 CI on `3c8186b7` | **Verified green.** All 10 PR workflows and the arena run succeeded; Fabric CI only on attempt 2 after a `client-smoke` timeout | [FINAL_CHECKPOINT_ACCEPTANCE.md](../FINAL_CHECKPOINT_ACCEPTANCE.md) |
| Adamantium F-3 | **Not implemented.** Shipped data violates F-3 on height, bias and vein size and meets only the binding "fewer than Ancient Debris" rule. Six bounded alternatives measured | [ADAMANTIUM_F3_ANALYSIS.md](ADAMANTIUM_F3_ANALYSIS.md) |
| Wither-skull F-1 | **Blocked, and the defect is reproduced.** The quest is unregistered on the checkpoint (`registered: false`, no skull code present, 75 registrations). A crash-consistent protocol is identified and needs one approval | [WITHER_SKULL_F1_ANALYSIS.md](WITHER_SKULL_F1_ANALYSIS.md) |
| Branch cleanup | 143 branches: 87 delete eligible (81 strictly safe), 56 retained. Nothing deleted | [BRANCH_CLEANUP_MANIFEST.md](BRANCH_CLEANUP_MANIFEST.md) |

## 2. Decisions needed

### Gameplay (block Wave 13 implementation)

| ID | Decision | Options | Recommendation |
|---|---|---|---|
| **D-F3-1** | Does "Y 8–22" bound the vein **origin** or **every generated block**? | origin → candidates A/B (8–22) or C/D (8–21). Every block → E/F (9–21), the only configurations with zero blocks outside Y 8–22 | Every block, if "no Adamantium outside Y 8–22" is the intent (E). Origin if the range was meant literally (A). Owner's reading decides |
| **D-F3-2** | Placements per chunk | 1 (≈49% of Ancient Debris), uniform 1–2 (≈73%), or another distribution (not sampled; about 1.6 average would approach the 80% tuning target) | 1, because it is the only value that adds no new numeric choice. Moving toward 80% is a balance call |
| **D-F3-3** | Accept that every F-3-compliant configuration raises abundance from the shipped **26%** of Ancient Debris | yes / no | Needed for any implementation; `scattered_ore` yields ~1.9× the blocks of the size-3 `ore` it must replace |
| **D-F1-1** | Approve the meaning of "crash-safe counting" for the skull quest: **never credited twice; a durably held skull is always credited; no credit without a durably held skull. A crash may still cost the skull and its credit, and may leave an uncredited duplicate item on the ground** | approve / require physical skull conservation (no protocol can deliver it on vanilla storage) | Approve. Implementation is then bounded and uses the existing contract-redemption pattern |

Defaults that need no separate answer unless the owner objects (F-1): genuine skull entities never merge; legacy and tokenless skulls never earn credit; two synchronous writes and one player save per credited pickup.

### Housekeeping

| ID | Decision | Notes |
|---|---|---|
| **D-H-1** | Policy for the **6 delete-eligible branches whose deletion would orphan commits** (`codex/xp-quest-ui-integration-20261020`, `design/stage23a-remaining-approval-20261017`, `docs/2026-10-07-content-first-axis-review`, `docs/alchemy-plot-notes`, `docs/ui-piglin-mob-tracker-20261010`, `recovery/vanilla-model-custom-mob-roster-base`) | Every byte they introduced exists in the checkpoint, except two lines I checked and found explicitly superseded (the old −2/−4/−6% villager discount; an old "Baseline:" line). Only commit history would be lost. Either accept that, or tag the tips first. The other **81** lose nothing: every commit stays reachable through another branch or a `refs/pull/N/head` |
| **D-H-2** | Retire or keep four tooling-only branches (`codex/stage23-pr105-bundle-export-20261016`, `experiment/ponytail-skill-2-20261021`, `prototype/stage20-vanilla-sweep-texture`, `prototype/vanilla-sweep-texture`) | They have no PR, so deleting them destroys their only copy. Classified RETAIN until decided |
| **D-H-3** | `integration/wave12-functional-preservation-20261027` | Same commit as the checkpoint; kept only because CI evidence ran on it. Safe to delete later |
| **D-H-4** | Sky Islands design branch | Still being edited by the owner (it moved after Wave 12 preserved an earlier copy). A current snapshot is preserved in `docs/preservation/snapshots/`. Not authority |

### Approved designs that were never implemented on any branch

These are not integration gaps and nothing was lost. Each needs an explicit "implement this" task: Adamantium equipment, templates and refining; the rare caravan and runes; pillager patrols; the Earth Mace recipe with Ancient Heart (F-2); the pyramid, Pillager HQ, Spider Colony and further structures (the structure index says implementation is not authorized).

## 3. Risks that are not owner decisions

- **CI fragility.** `client-smoke` has `timeout-minutes: 8`, and the runner can spend ~7 minutes in `apt-get install` first. It was cancelled on both Wave 12 heads and passed only on re-run. Raising the timeout or caching the display packages would remove this; I did not change a workflow outside the task.
- **Evidence is headless.** Everything here is Linux, software Vulkan or dedicated server. No live validation of any kind is claimed (arena look, combat/ability feel, physical UI input at GUI scales 1–4, two real clients, Windows client).
- **Three seeds.** The Adamantium ratios vary by seed (e.g. 67.5–78.3% for candidate B). They support "rarer than Ancient Debris on every seed", not a precise long-run rate.
- **The skull model is not the Minecraft hooks.** It proves protocol logic under stated persistence assumptions. An implementation must still pass a real two-JVM halt matrix including the skull-specific boundaries.

## 4. Can Wave 13 implementation proceed without further blockers?

**No for gameplay; yes for everything else.**

- **Adamantium F-3** cannot be implemented until D-F3-1, D-F3-2 and D-F3-3 are answered. Each is a one-line answer. After that it is a two-file data change, with the measurements here as acceptance evidence.
- **Wither-skull F-1** cannot be implemented until D-F1-1 is approved. After that it is a bounded task (a transaction class modelled on `ContractTransactions`, the item-entity hooks, and an extended halt matrix), and registration is a separate, later step.
- **Nothing else is blocked by a decision.** The Wave 12 checkpoint is verified, the cleanup manifest is complete, and the other approved-but-unimplemented designs are scope choices rather than blockers.

Not blocking, but worth doing alongside: the `client-smoke` timeout.

The minimum set of owner answers that unblocks both gameplay items is **four**: D-F3-1, D-F3-2, D-F3-3 and D-F1-1.

> **LATEST OWNER SCOPE CHANGE (2026-10-21): TERRITORY SHELVED.** This document is historical design reference, **not an active implementation specification**. See [territory shelving and independent-content decision](TERRITORY_SHELVED_INDEPENDENT_CONTENT_2026-10-21.md). Do not implement D01–D84 as a territory system without explicit owner reactivation.

# Territory owner decisions D81–D84 and facility design handoff — 2026-10-21

Status: **OWNER-APPROVED gameplay policy, documentation only**. Stacked on Draft PR #136 HEAD `d641075f2078697e432f123d61178a73d26fbdf8`. This supplements [D01–D80](TERRITORY_OWNER_DECISIONS_2026-10-20.md); it does not authorize gameplay code or merges.

## D81 — Bell-centered physical registration and staged visual/world changes

- A village must have a bell. No bell means registration is unavailable; no fallback anchor is required.
- When a player approaches the village-center bell, show a Text Display explaining that throwing the required materials into the nearby large Interaction area registers the village as their territory.
- Only accepted required items count toward the deposit. Non-matching thrown items behave as ordinary dropped items and must not be consumed or intercepted.
- When the complete D79 recipe is paid and the D01/D02/D03a/D04 eligibility/safety checks succeed, **T1 ownership and territory protection activate immediately**. No additional battle/quest/liberation challenge and no second T0→T1 payment are required. D03a's formerly open “liberation challenge” is fulfilled by the physical deposit itself.
- Physical village changes (new facilities, roads, guard placement) unfold sequentially after ownership/protection activation. This is not a delay to the T1 entitlement and does not waive safe prevalidation or persistence proof.
- Re-registration after relinquishment follows D57: the village returns to T0 and must complete the registration deposit to obtain T1 again.

## D82 — Accepted registration materials

D79 quantities are unchanged. Accepted variants in each category count together, including mixed stacks:

| Category | Count | Accepted |
|---|---:|---|
| Logs | 64 | Vanilla tree logs of any wood species, mix allowed |
| Cobblestone | 128 | Ordinary cobblestone only |
| Sticks | 32 | Ordinary sticks |
| Planks | 64 | Vanilla wood planks of any species, mix allowed |
| Coal / charcoal | 32 | Either coal or charcoal, mixed total allowed |
| Iron ingots | 16 | Ordinary iron ingots |

Implementation must define exact 26.3 item identifiers/tags consistent with these descriptions. Do not silently include stripped logs, wood blocks, nether stems, cobbled deepslate or modded variants without owner approval.

## D83 — Territory upgrade recipes (each transition paid once)

Upgrade deposits use the **same bell-centered Text Display + Interaction physical thrown-item interface**. Costs are per transition, not cumulative and not additional registration costs.

| Material | T1 → T2 | T2 → T3 |
|---|---:|---:|
| Logs | 256 | 512 |
| Cobblestone | 384 | 768 |
| Iron ingots | 64 | 128 |
| Copper ingots | 64 | — |
| Gold ingots | — | 64 |
| Emeralds | 16 | 64 |
| Diamonds | — | 16 |

Glass and stone bricks are excluded; avoid mandatory tedious processed construction materials. Mixed vanilla log species count together under D82. Upgrade recipe variants not covered by D82 remain subject to item-ID validation; do not invent other substitutions.

## D84 — Automatic site search, conservative clearing and refund on failure

- Before committing an upgrade, search for valid sites for **all mandatory new facilities and connections**, preferring existing road/building connectivity (D59–D65). The server may plan limited grading, tree/leaves removal and safe natural-obstacle clearing where reliably safe.
- **Protect all existing village buildings, managed structures, player builds, owner edits, containers and roads**. Vanilla block state alone cannot prove whether a natural-looking block was player-placed. Do not revive withdrawn D67–D78 block-history/provenance/journal/backup systems. If safety is uncertain, reroute or reject.
- Use D04 whole-layout prevalidation: no partial facility construction, no resource consumption on failed upgrade, no destructive speculative edits. A valid plan must account for planned grading and safe clearance before world mutation.
- If no safe whole-layout plan exists, **reject the upgrade, return all contributed upgrade materials, preserve the existing tier and world, and show an explicit “clear space” instruction**. No partial refund, no lost deposit, no deferred indefinite escrow.
- Exact delivery/refund implementation (full inventory, disconnect, cross-save restart, simultaneous players, dropped-item attribution and duplicate prevention) is an **unresolved technical gate**. Do not claim atomic/exactly-once payment before real durability tests.

## Compatibility / precedence

- D01–D66 remain approved; D67–D78 remain withdrawn; D79–D80 remain approved.
- D81 resolves the formerly open D03a challenge **at owner-policy level**: physical bell registration is the liberation action. Older text demanding an additional challenge is superseded.
- D82 resolves D79 material acceptance **at owner-policy level**, including charcoal; historical Paper source may still be SOURCE MISSING.
- D83 resolves the T1→T2 and T2→T3 price tables. Exact item matching and economic/live balance are separate implementation/test concerns.
- D84 refines D04 and D59–D66 without permitting player-build destruction. Refunds are an approved outcome, not proof of crash-safe implementation.
- Do not treat an item passing through the Interaction hitbox as proof of safe player attribution or completed durable payment.

## Next task: facility design (NOT YET APPROVED)

Design a tier-by-tier facility roster and upgrade order, including:
1. Which original village buildings remain ordinary, which become managed/protected, and how owner edits survive upgrades.
2. Mandatory vs optional facilities at T1/T2/T3, per small/medium/large village and cumulative D06 villager/guard/building targets.
3. Physical placement footprints, roads, bed/workstation requirements, guard spawn buildings, and staged construction/visual timing.
4. What happens if T1 registration is paid but the mandatory initial layout cannot pass safe prevalidation; distinguish registration failure/refund from post-registration staged construction.
5. Protection and non-owner interaction matrix; keep unresolved details explicitly OPEN rather than inventing approvals.

No code/assets/workflows changed; no builds, GameTests, or live multiplayer/crash tests performed. Keep all stacked PRs Draft and do not merge.

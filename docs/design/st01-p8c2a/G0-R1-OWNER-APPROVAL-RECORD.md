# ST-01 G0-R1 owner approval record — construction authority for P8-C2-A

Date: 2026-11-10. Source: owner's "ST-01 G0-R1 Approval → P8-C2-A Complete Pyramid Construction" instruction.

Approved artifact: branch `work/st01-g0-r1-blueprint-finalization-20261110`, commit
`c26ea0f871e788d1d331b40151779d86590469f2` (Draft PR #257), directory `docs/design/st01-g0-r1/`.
The approved machine-readable geometry is exactly the committed `generated/blueprint.json`
(SHA-256 `3e7106aea15d8f033cbdba4728d90e7ee50875c0e80fc724ad1585510fd78f5a`) and
`generated/d1-exceptions.json` (SHA-256 `9eeed14ca6809b12426f92adf910c8cf514ad52e8263682ada7f49f01f8bc849`),
which `tools/st01-g0-r1/verify.py` reproduces byte-for-byte.

The owner approved the three bundles of `docs/design/st01-g0-r1/OWNER-APPROVAL.md` by number. Each approval
covers exactly the content of that commit. None of them authorizes changes to adjacent geometry.

| Bundle | Status | Scope adopted (exactly as in blueprint.json at c26ea0f8) |
|---|---|---|
| 1 — Numerical architecture | **APPROVED** (complete package) | P1/P2/P4-B candidate geometry; candidate-C adjustment; three L1 tombs (T1–T3); three L2 stores (B1–B3); gallery support piers X−8..−6 and X6..8 / Y0..4 / Z−3..−1; altar pedestal X−3..3 / Y11..12 / Z−19..−17; antechamber statues centred X−27 and X−15, Z−4; one 6×9×5 annex X−38..−33 / Y−22..−18 / Z−11..−3 and its two-block port X−32..−31 / Y−22..−20 / Z−8..−6; secret crypt 15×13×7 X8..22 / Y−30..−24 / Z−7..5 with six niches; lowest structural layer Y = −32; the stepped-in ceiling and junction masks; all component coordinates, dimensions, origins, parents, placement order and construction profiles. |
| 2 — WEST access route | **APPROVED** | The five WEST segments (departure spine X−2..2 / Y−30..−27 / Z29..31; westbound X−15..−3 / Z29..31; north turn X−15..−13 / Z20..28; east return X−12..2 / Z20..22; destination join X0..2 / Z23..26, all Y−30..−27). The EAST direct segment CB1 is X−2..2 / Y−30..−25 / Z24..31. Shared cover, seven-tread FA descent and landing LA. Fixed royal hall, treasury and crypt. Deterministic 50:50 EAST/WEST selects the access route only. No whole-hall mirror, no duplicated secret chamber, no relocated PRIMARY or Guardian, four independent whole-pyramid rotations. The 48-cell WEST CB1/CC destination-junction overlap is the only permitted inter-component cavity overlap. |
| 3 — Visual and physical representation | **APPROVED FOR INITIAL CONSTRUCTION** | The 21-state palette and block-state assignments; furniture (sarcophagi, oak-plank shelves, flower-pot pottery on pedestals) and architectural decoration; the 94 wall-torch positions and states per variant; gold and diamond pile masks; the black-terracotta/gold/cyan block-scale treasury sculpture and lapis column accents; the approved block-entity representations (D1's two PRIMARY chest entities verbatim, plus the empty id-only vanilla chest UI anchor at (25, −26, 26), facing west, single). |

## Explicit limits of this approval

- **Treasure chest.** The existing PRIMARY double chest is preserved: coordinates, handedness, inventory and reward
  behavior. No new treasure-chest model, texture, opening animation or rendering system. The treasury UI anchor
  stays an empty vanilla chest with no loot table. SECONDARY stays physically absent. The final appearance of any
  future custom treasure chest is outside this approval. A custom sculpture model may replace the block-scale one
  later, as a separate visual task.
- **Not authorized.** New loot, reward mechanics, pile mineability, protection rules, secret-unlock behavior, natural
  worldgen, server-thread completion handoff, or route-selection runtime.
- **Unchanged.** 81×81 footprint; 40-block aboveground height; original D1 NBT bytes (SHA-256
  `7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423`); the EAST royal-hall position; PRIMARY coordinates
  and handedness; Guardian emergence position; canonical tomb identity; catalyst 5% probability and conditional 50:50
  PRIMARY/SECONDARY assignment; deferred SECONDARY entitlement; the approved P8-C1 worldgen policy.
- **D1 seam (R1-01, previously approved).** Exactly 12 cells become air: assembly X−8..−5 / Y−24 / Z33..35 (D1-local
  X32..35 / Y0 / Z31..33), per `generated/d1-exceptions.json`. The separately governed Y−23 cover cells keep their
  original rules. No other D1 change is authorized.

## Effect on authority status

The fields that `docs/design/st01-g0-r1/AUTHORITY-DELTA.md` listed as PROPOSED or CONDITIONALLY_APPROVED become
**APPROVED construction authority**, but only to the extent of the bundles above. Treasury pile and chest harvest and loot
behavior remain **UNSPECIFIED**. Construction against this record is P8-C2-A, documented in
[README.md](README.md).

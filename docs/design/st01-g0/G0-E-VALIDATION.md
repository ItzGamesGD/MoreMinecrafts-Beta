# G0-E — Validation and unresolved contradictions

Overall: G0 BLOCKED. A useful authority matrix and numerical adoption proposal exist, but the complete required geometry cannot be presented as coherent under all current constraints. No construction-ready claim.

## Reproduction

From repository root, Python 3, no third-party packages:

    python3 tools/st01-g0/audit.py --out work/st01-g0-recheck

Compare the three generated files with docs/design/st01-g0/generated. Outputs contain no timestamps and should match byte-for-byte. The script writes design JSON/Markdown only. A zero exit code means diagnostic execution succeeded, not that architecture passed. Inspect validation.json checks and overall status.

## Actual results

| Check | Result | Scope |
|---|---|---|
| D1 original SHA-256 | PASS | 7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423 |
| D1 format | INSPECTED | 39×20×39; 30,420 block records; DataVersion5023; zero entities |
| Registry box ordering | PASS | All 141 source records have ordered inclusive boxes |
| Registry duplicate IDs | PASS | 141 unique identifiers; does not imply every required solid exists |
| Rotation inverse | PASS | Integer transform algebra for representative extrema and anchor; not full block-state verification |
| D1 vs secret FA cavity | FAIL | Twelve D1 solid cells would need carving, excluding the twelve cover cells |
| Independent secret orientation | FAIL | Existing source EAST anchor compatible under four rotations; WEST incompatible under four rotations |
| Entire assembled envelope/reservation | NOT RUN | Full materialized candidate is incomplete |
| Entrance-to-hall navigation | NOT RUN | Source graph is recorded; no actual-asset connectivity claim |
| All overlaps / seam widths / headroom | NOT RUN | Isolated source bbox sanity does not prove assembly integrity |
| Secret concealment and access | NOT RUN | Existing source abstract checks do not settle D1 intersection |
| Complete block-state rotations | NOT RUN | No complete per-region material/state blueprint |

Exact conflicting cells: X−8..−5 × Y−24 × Z33..35. These are below the Y−23 cover plane; they are not the same twelve cells. Independent parser reads actual compressed NBT palette/block records and maps template local positions by (−40,−24,2). P4 FA boxes are interpreted as explicit air cavities, as defined in the source model. The audit does not infer walkability from a vague room bounding volume.

The WEST source anchor (+21,−20,33) differs by 42 blocks in X from the required anchor (−21,−20,33). Rotation preserves that nonzero displacement. This proves that the old full WEST model is not an independent secret variant around fixed D1; it does not prove that a newly designed WEST variant is impossible.

## Regression and runtime limits

No production code/assets were changed. No new Minecraft server/client tests or CI runs were triggered for this design packet; they are NOT RUN. Prior B2 evidence remains historical:

- Scoped PASS: https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/42742214241
- Full Fabric CI FAIL (ST-05 absence-grace): https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/42742214500
- Duplicate Guardian collision FAIL: https://github.com/ItzGamesGD/MoreMinecrafts-Beta/actions/runs/42742209085

Causality remains unresolved. Source preservation alone does not establish independence. Those failures are separate from the geometrically demonstrated conflicts here.

MM-SG main was verified at 61a09eebafa9beee0944eba266e7f05a9543eec5. Its tools/structure_export/convert.py uses version-pinned block-state reference data, supported block-entity handlers and .schem/TOML inputs. It has no inspected ST-01 export configuration that resolves these geometry decisions. No exporter was run or modified. No new NBT, schematic, runtime behavior or worldgen was generated.

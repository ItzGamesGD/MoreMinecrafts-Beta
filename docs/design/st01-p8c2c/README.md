# ST-01 P8-C2-C — Policy D natural worldgen (implementation and verification)

Branch `work/st01-p8c2c-policy-d-20261111` (based on P8-C2-B `9e4379c7`). Authority: [OWNER-DECISION-POLICY-D.md](OWNER-DECISION-POLICY-D.md).
Code commit verified by CI: `f3ddfbea` (run 42943983078, 24/24 jobs green). Merge: not authorised, not done.

## Verified baseline
P8-C2-B `claude/st01-p8c2b-natural-worldgen-isauq8` = `9e4379c7` (PR #262); audit branch `audit/st01-p8c2b-counterfactual-rarity-20261111` = `569fc789` (PR #264); P8-C2-A `c25f9d44` (PR #259). Only the audit report and `counterfactual-audit/` data were imported; no PR was modified.

## Production change (src/main only)
- `St01Eligibility`: removed the entrance stage, the fluid stage, `Terrain.hasFluid`, `Volume`, `REJECT_ENTRANCE`/`REJECT_FLUID`, `entranceQualified`.
- `St01Candidate`: removed `entranceApproach`; `decide` calls `evaluate(terrain, cx, cz)`.
- `St01PlacementConfig`: removed `FLUID_MARGIN`, `UNDERGROUND_VOLUME`, `ENTRANCE_*`.
- `St01PyramidStructure.GeneratorTerrain`: removed `hasFluid`.
- No flag restores the old policy. Unchanged: structure/structure-set JSON, pieces, NBT assets, completion, royal chest, Guardian, rewards.

## Real-world result (20 original seeds, 8,388.608 km²; evidence/policy_d.json)
| Measure | Count |
|---|---|
| potential anchors / full Desert / terrain-qualified | 32,000 / 221 / 46 |
| eligible candidates (Policy D) | 46 |
| actual structure starts | 46 (the set equals the audit's eligible_D set exactly) |
| fully generated and all in-game checks pass (4 phases) | 46 |
| complete assembly integrity (0 owned-cell mismatches, 0 leftover terrain in void) | 46 |
| royal chest registered / Guardian+reward lifecycle pass | 46 / 46 |
| buried-entrance starts (approach rise >1; audit obstruction) | 21, all pass |
| starts previously rejected for fluid | 40, all pass |
| approach row lies outside the structure box | 46 / 46 |

Density 0.548 /100 km² (Garwood 95 % 0.401–0.731; seed-cluster bootstrap 0.393–0.703); per seed: 2 seeds with zero, mean 2.3, median 2. Policy A 4 → B 6 → C 25 → D 46 (counterfactual) → real 46. EAST 22 / WEST 24; N18 E10 S9 W9. 42 starts are new relative to Policy A; the 4 original starts persist. 16 vanilla overlaps are recorded only (no overlap policy).

## Observations (accepted, not failures)
- Fluid inside the completion box: 0 starts (assembly cells replace it). Fluid in the one-block shell outside the box occurs and is untouched.
- 18 approach columns across all starts have air at the pre-generation noise surface. This cannot be attributed to the structure (provably no write outside the ±40 box, `terrain_adaptation: none`; vanilla carvers also open the surface) and is recorded only.
- A first CI run failed on a too-strict diagnostic I had added for this (air at the approach row); that was a probe defect, not production. It is now an observation plus a structural check (run 42941091419 failed; 42943983078 is the reference).

## Validation status
Unit tests (incl. Policy D tests) PASS; P8-C2-A eight-case placement and verifier PASS; natural-piece GameTests (8 cases × normal/reverse/random order) PASS; royal chest and Guardian GameTests PASS; 20-seed real-server scan, reverse, random, restart-reload PASS with identical owned/box hashes across phases. Not run: the full GameTest suite and the ST-05 colosseum tests (outside scope). Visual acceptance UNVERIFIED: client captures ran (seed 1004, 2 starts, neither buried nor fluid-affected) and have not been reviewed.

# Sky Tower — Regression & Gameplay-Constraint Assessment (Phase 2)

Scope: evaluation only. No production worldgen JSON, Java, NBT or reward file was changed (`git diff 038eaef -- src/main scripts` is empty; the only non-doc changes are the isolated probe, one added gametest `fabric.mod.json` entrypoint, and `tools/sky-tower-phase2/`).
Labels: `RUNTIME_CONFIRMED` · `SOURCE_CONFIRMED` · `MATHEMATICALLY_DERIVED` · `INFERRED` · `UNVERIFIED` · `NOT RUN` (AGENTS.md §7: not reported as passed).

Strategies (see `REMEDIATION_BENCHMARK.csv`): **E** no change · **A** narrower `start_height` range (data-only) · **B** terrain-aware dynamic height (needs a custom `Structure` type = Java) · **C** retry / substitute / relocate (Java) · **D** dedicated tower structure (new structure + structure set + pool edits).

## 1. Constraint × strategy matrix

| Constraint | E | A (e.g. 15..35) | B | C | D |
|---|---|---|---|---|---|
| **Approved tower geometry & NBT** unchanged | ✔ | ✔ (rigid translation; no template touched) | ✔ | ✔ | ✔ (reuses same NBT) |
| **Six visual variants** | ✔ | ✔ | ✔ | ✔ (substitute/relocate keep style) | ⚠ new structures must reference six style pools; pool weights of the six start pools change |
| **Trial Spawner / Vault counts** | ✔ (prior C-4) | ✔ **RUNTIME**: tower generated under 15..35 at `maxY=315`: 16 spawners, 8 vaults | NOT RUN | NOT RUN | NOT RUN |
| **Barrier progression & registration** | ✔ (prior C-4) | ✔ **RUNTIME**: `automatically_registered=true`, 72 runtime barrier-air cells, 0 blocked cells | NOT RUN (UNVERIFIED) | UNVERIFIED — relocation moves the start; registration keys on the structure box | NOT RUN |
| **Island-to-tower bridge integrity** | ✔ | `composite_piece_count 1`, `legacy_tower_child_count 0` **RUNTIME** (single rigid piece, bridge inside the NBT) | INFERRED same | INFERRED same | INFERRED same |
| **Natural terrain clearance** | 18..70 | **changes for every mountain island**: mean 25.1 (15..35) vs 43.3 | tower clearance mean 42.9, min 18 (18..70 kept; truncated only where physics forces it); islands other than towers unchanged except at T ≳ 200 | tower mean 43.1–43.6 | 1..20 ⇒ mean 10.4 |
| **Rarity intent** | −3.1 % mountain towers (alpine ≈ −20 % per peak biome) | +61 towers / 2,699 (paired) | +72 / +81 | retry +51..62; relocate +76 but 54 % change biome | decided by new placement density (**OWNER DECISION**) |
| **Biome distribution** | alpine towers under-represented | restores alpine share (frozen 4.1 → 4.8 %) | restores (4.9–5.0 %) | relocate **distorts** (alpine → grove) | by design |
| **Existing saves** | n/a | data-only; already-generated chunks unchanged, new chunks use the new range (`INFERRED`, mixed-era seams possible) | structure type/Java change: existing stored starts unaffected in principle, `UNVERIFIED` | `UNVERIFIED` | removing the tower from six pools alters future starts only (`INFERRED`) |
| **Reward system untouched** | ✔ | ✔ | ✔ | ✔ | ✔ (reward pools are loot tables, not referenced by placement) |

## 2. Evidence for the cells marked RUNTIME

* **Tower top blocks present:** 45/45 natural towers (maxY 220…315, 11 at exactly 315) have the reference 64 `smooth_stone` crown (`topblocks.csv`, see `RUNTIME_REPRODUCTION.md §4.4`).
* **Candidate A under the existing C-3 composite probe** (`tools/sky-tower-phase2/run_regression_probe.sh reg_A1535_jag190`, flat jagged_peaks, surface T=190, disposable datapack `start_height 15..35` for `sky_island_mountain_snowy`; production file untouched): snowy tower generated naturally at site `(-368, 236, 224)`, `piece_bounds` `minY=219, maxY=315`, **16 Trial Spawners, 8 Vaults**, `composite_piece_count 1`, `legacy_tower_child_count 0`, `automatically_registered true`, `runtime_barrier_air_cells 72`, `blocked_barrier_cells 0`, `pass true` for style `snowy`. The overall `pass false` of that report only reflects that the other five styles cannot occur in a single-biome world; the JVM also logged `OutOfMemoryError` at shutdown (the probe's 160,801-chunk scan, after the report was written). This is one sample of one style under candidate A; it does not cover B/C/D or the other styles. `RUNTIME_CONFIRMED` for that sample only.
* **Unchanged-asset proof:** every run reads the six tower templates through the engine: `58×97×49` for all six styles (`elements.csv`); the repository diff against `038eaef` contains no NBT.

## 3. Collateral risks found while evaluating

1. **A changes every mountain island, not only the tower.** The mountain structure JSONs share one `start_height`; a narrower range lowers *all* island types (mean clearance 43 → 25 for 15..35). That alters the approved "mixed altitude, near-peak to high above" ecology direction (`design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` § 생성 지역, 고도, 군집), which sets no numeric range — the numbers are an open tuning backlog — but is still a visible design change. Counter-view: relative altitude and "above the peak" are still satisfied (`clearance ≥ 15`).
2. **B changes only what physics forces**: below T ≈ 149 the tower's distribution is untouched; above, it is truncated. Non-tower islands are unaffected below T ≈ 200. But it requires a new registered structure type (Java) — a production change and a `STOP & ASK` item under AGENTS.md.
3. **Strategies cannot create room that does not exist:** 97-block pieces need `T ≤ 218` (clearance ≥ 1) / `T ≤ 201` (clearance ≥ 18). For T ≥ 219 every strategy is infeasible; relocation only escapes by leaving the peak (biome distortion).
4. **Other (out-of-scope) defects found** — not fixed: non-tower islands also fail at high terrain (temple at T ≥ 202, plains/ruins at T ≥ 220; 318/745 non-tower at flat T=240); general-set towers lose 0.8 % (T ≥ 110); 39 attempts rejected only by the sky-altitude biome test; Phase 1's element-height table and "100 % non-tower success" are wrong.

## 4. Not verified in Phase 2

`NOT RUN`: Trial Spawner/Vault counts and barrier registration for B, C and D; the other five tower styles under A; client rendering / LOD of lower islands (`LIVE VALIDATION PENDING`); restart persistence under A; interaction with the `sky_islands_general` exclusion zone when a mountain candidate is relocated or removed (`SOURCE_CONFIRMED` independent of generation success; not run).

# Sky Tower Mountain Worldgen — Phase 2 Verdict

Labels: `RUNTIME_CONFIRMED` · `SOURCE_CONFIRMED` · `MATHEMATICALLY_DERIVED` · `INFERRED` · `UNVERIFIED` · `BLOCKED`. Detail and sample sizes are in the other Phase 2 documents; Phase 1 documents are unchanged historical evidence.

## 1. Runtime reproduction status

**REPRODUCED in a real Minecraft 26.3 Fabric server (not BLOCKED).** The Sky Tower root-piece is rejected by `JigsawPlacement` when its bounding box reaches the padded build ceiling, and the whole structure start is then absent — `RUNTIME_CONFIRMED` on natural Overworld terrain (engine log line + 1,062 rejected attempts, natural pipeline agrees with the engine replay on all 85,154 compared chunks), on controlled flat worlds through the real pipeline, and on a 12-seed natural scan. Physical clipping does **not** occur naturally; it only occurs for forced placement at `maxY ≥ 320` (`RUNTIME_CONFIRMED`).

## 2. Corrected mathematical model (`CORRECTED_HEIGHT_MODEL.md`)

`root.minY = start_height + T` (T = top surface block), `root.maxY = minY + sizeY − 1`, rejected iff `maxY > getMaxY() − padding.top = 319 − 4 = 315`. Tower (97 blocks): **accepted ⇔ `start_height ≤ 219 − T`** (`= 220 − H`, H = heightmap first-free = T+1). Verified: 0 mismatches over 260,244 engine attempts; maxY=315 accepted (17 observed), maxY=316 rejected (19/19). `RUNTIME_CONFIRMED`.

## 3. Phase 1 claims — confirmed / rejected

* **CONFIRMED:** rejection (not clipping) is the natural-generation failure mode; the tower (97) is the dominant victim; forced placement clips at 320; terrain ≥ ~220 cannot host the tower under any configuration.
* **REJECTED / corrected:** limit is 315, not 316 (every Phase 1 per-roll failure rate is one roll low); the element-height table (16/17/21/19/27) is wrong — real sizes are 23/24/27/27/45; "non-tower islands 100 % to T=220" and "general variants immune ≤ 100" are wrong (general towers fail from T ≥ 110, temples from T ≥ 202); "100 % extinction on jagged peaks" is not what the natural world shows (131/166 jagged_peaks towers still generate); `mountain_meadow` is not affected (1,542 accepted, 0 padding rejections).
* **REJECTED (explicit): "15..35 restores > 95 % success."** Baseline undefined; on the failure range it is false: 23.8 % at T=200 (model), 56.7 % (34/60) of natural towers at T ≥ 180, 0 % at T ≥ 220. Globally the current config already accepts 96.7 % of mountain towers. No replacement percentage is offered.
* **UNVERIFIED:** hypothetical cases M-1/M-2 (seeds 42/1337), the 1,536-sample historical padding audit. Phase 1's "runtime evidence" was source-level simulation.

## 4. Candidate rejection mechanism

1. Structure set picks a chunk (`random_spread 14/5/0.5`) → weighted structure → `JigsawStructure.findGenerationPoint` samples `start_height` (first draw) → `JigsawPlacement.addPieces` draws rotation then **one** pool element (no retry) → root box moved to `minY = start_height + T` → `isStartTooCloseToWorldHeightLimits` → empty ⇒ `INVALID_START`. `SOURCE_CONFIRMED` + `RUNTIME_CONFIRMED`.
2. The set-level retry tries other *structures* of the set from the same seed: identical height/rotation/element (22,247/22,247) ⇒ identical rejection (and the other variants fail the biome test). `RUNTIME_CONFIRMED`.
3. A rejected mountain candidate still occupies its placement chunk; the general set's exclusion zone is computed from placement alone. `SOURCE_CONFIRMED`.
4. Secondary mechanism found: biome test at the sky-altitude stub (39 rejections). `RUNTIME_CONFIRMED`.

## 5. Comparative remediation results (`REMEDIATION_BENCHMARK.csv`, `RARITY_DISTRIBUTION.md`)

| Strategy | Feasible? | Result (paired, 8 natural seeds, 2,699 selected towers; controlled sweep for elevation) |
|---|---|---|
| **E no change** | — | loses 88 / 2,699 = 3.3 % of selected towers (121 / 3,850 = 3.1 % over 12 seeds; 0.45 % of all mountain islands); ≈ 20 % per alpine peak biome; 0 % above T≈201 |
| **A narrower range** (15..35, 18..35, 10..30, 5..20, 1..18) | yes, data-only | 15..35: 99.0 % overall, **but T ≥ 180 only 56.7 %**; always-OK up to T=184, partial to 204, impossible ≥ 205; **lowers clearance of every mountain island** (mean 43 → 25). 1..18 closes T ≤ 201 fully but mean clearance 9 |
| **B terrain-aware dynamic height** (prototype) | yes, needs Java structure type | 18..70 floor kept: 99.4 % overall, 75 % at T ≥ 180, **100 % wherever clearance ≥ 18 is physically possible (T ≤ 201)**, mean tower clearance 42.9 (vs 43.3); 1..70 floor: 99.7 %, 100 % up to T=218; infeasible above |
| **C retry / substitute / relocate** (prototype) | partly | retry ×8: 99.0 % but only 32 % at T=200; substitute: towers unchanged (keeps island count); relocate: 99.6 % but 54 % of rescued towers change surface biome and alpine biomes are not recovered |
| **D dedicated tower structure** | yes (data) but redefines tower placement density | with a 1..20 range 93.6 % at T 180–199, 65 % at 200–219; absolute rate is an owner design choice; pool weights/set change |

Not all five are equally good: C-relocate and C-substitute do not address the alpine failure; A-15..35 and D-18..70 leave most of T ≥ 180 uncovered; infeasible terrain is infeasible for every strategy.

## 6. Remaining impossible terrain (97-block tower, `MATHEMATICALLY_DERIVED`, runtime-consistent)

| Required clearance | Highest feasible terrain T | Impossible for |
|---|---|---|
| ≥ 1 | **218** | T ≥ 219 (incl. requested 220, 240, 260) |
| ≥ 10 | 209 | |
| **≥ 18 (current minimum)** | **201** | T ≥ 202 |
| ≥ 35 | 184 | T ≥ 185 |
| ≥ 70 | 149 | |

Of the requested terrains 80/120/140 are safe under the current config; 160/180/200 are partly lost (79.2 %/41.5 %/3.8 % accepted); 220/240/260 are infeasible for all strategies.

## 7. Effective rarity impact (`RARITY_DISTRIBUTION.md`)

Tower share in the start pool 14.16 % (nominal 14.29 %); conditional acceptance 96.86 % [96.3, 97.4] (≥ 99.9 % below T=140; 26.9 % at T 180–199; 0/15 at 200–219); absolute ≈ 485 accepted towers per 10,000 km² (mountain set); tower share of generated mountain islands 13.79 % (4.8 % at T 180–199). The impact is large per peak biome (frozen/jagged/stony ≈ −20 %) but small globally (−3.1 % of mountain towers). Samples at T ≥ 200 are small (22 towers); the report refuses to present those percentages as precise.

## 8. Recommended remediation (if justified)

**Justified, as an owner decision, but not Phase 1's 15..35.** Evidence: the defect is real and concentrated in the biomes the design singles out for high-altitude islands; the owner design leaves the numeric altitude/frequency as an open tuning backlog and asks for terrain-relative altitude (`design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md`), which points at **B** (terrain-aware truncation: tower height limited only where physics requires it, everything else unchanged; closes 100 % of the feasible range, preserves the 18..70 clearance elsewhere). If Java is not allowed, the data-only fallback is **A with the narrowest range the owner accepts** (a tower-friendly 5..20 or 1..18 closes the T ≤ 199–201 range but changes the clearance of all mountain islands; 18..35 preserves the 18 minimum but still loses 89 % at T=200). Both require owner decisions that Phase 2 does not make (AGENTS.md STOP & ASK): minimum clearance, acceptance that T ≥ 202/219 stays tower-free, and whether a new structure type is allowed.

No fix was implemented.

## 9. Delivery (see `CHECKPOINT.md` for exact commands and results)

Branch `claude/sky-tower-phase-2-validation-twragg`, based on `validation/sky-tower-mountain-height @ 038eaef6872e90961afdbf8ccab2581a275332a1`. Final commit SHA and remote verification are reported in the delivery message (a document cannot contain its own SHA). Not merged; `main` and the feature branch untouched.

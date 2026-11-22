# P3.11 — risk reduction before the production transition (production worldgen pools NOT changed)

Branch `claude/heaven-sky-island-p3-11-theme-rollout-20261122` (on P3.10). Unchanged: production pools, structure IDs / sets / probabilities / biomes / spawn rules, 114 original + 38 heaven NBTs, P3.6 T1 textures, other 4 themes, §4, F7 / G2 deferrals.

## 1. OOM in the game-test JVM
* **What happened:** in the first P3.10 regression `runGameTest` died with `java.lang.OutOfMemoryError: Java heap space` around the xpq / test-mock-player tests; an identical rerun passed (749/749).
* **Reproduction (instrumented run, `-Xlog:gc`, `-XX:+HeapDumpOnOutOfMemoryError`, live class histogram every 45 s, `oom_heap_histograms_run1.txt`):** OOM reproduced again at the same place (05:58, xpq/test-mock-player), this time without failing the suite (the OOM hit IO workers / one thread; 749/749 passed, but a 5.7 GB heap dump was written). Max heap of the game-test JVM = 3.4 GB (default 1/4 of 16 GB RAM); 128 full GCs during one run.
* **Heap / GC / accumulation vs leak:** the live set after full GC rose 0.8 GB -> 1.9 -> 2.7 -> 3.3 GB over the first ~6 minutes and then fell again to 2.5-2.6 GB (and to 2.6 GB at the end), i.e. it is accumulated *state of the shared test world and caches*, not an unbounded leak:
  * ~4.2 M `PalettedContainer` (+ same number of `ThreadingDetector` / `Semaphore` / `ReentrantLock`) = chunk sections of tens of thousands of chunks that ~750 tests touch at far-apart positions (1.2 M at 05:52 -> 3.4 M at 05:57 -> 4.3 M at 05:58);
  * 6.5 -> 10 M cached `StructureTemplate$StructureBlockInfo` + `BlockPos` (template manager cache, ~330 MB for the pair, plus map nodes), jumping when the sky-island tests start (05:54) and once more (+3 M) when `HabitatMaskGameTests` loads 160 templates at the very end (06:03, after the OOM);
  * 1.8-2.7 M `PlayerAdvancements$TriggerInstanceKey` (mock players' advancements), `HashMap$Node` 7-8 M.
  The P3.9 dense masks are 2.65 MB in total, so they are not the cause; the Habitat code holds nothing per tick.
* **Cause (assessed, not proven by a dominator tree):** heap pressure - the cumulative live set (~3.3 GB) touches the 3.4 GB default limit around the xpq tests; which side of the limit a run lands on depends on GC timing, hence the flake. No single leaking object was identified (the 5.7 GB dump was not analysed with a dominator tool; only histograms).
* **Minimal fix:** `build.gradle`: `runGameTest` gets `maxHeapSize = "6g"` (game-test server JVM only). Verified: heap cap 6 144 MB, 12 full GCs instead of 128, no OOM, 749/749, runGameTest 9 min instead of 13.
* **Stability:** see section 6 (two further complete `test build` runs).

## 2. meadow#1: 2 692 vanilla-theme blocks
`HeavenWorldgenProbe -Dmm.heaven.trace=true` (`worldgen_results/tr_trace.json`): the start has two pieces, `heaven/meadow/large_e` (box -1840,74,-794 .. -1704,174,-640) and `heaven/meadow/tower`. Every one of the 2 692 cells (dirt 1 151, grass_block 1 140, short_grass 364, oak_leaves 31, tall_grass 6) is **not a block of either template** (transformed with the piece rotation/position): 0 in-template, 2 263 below the natural noise surface of their column, 429 at/above it; all at y = 74..78 (natural surface 75-76). The island sits at ground level there and its piece boxes overlap the natural terrain. = **natural terrain, not vanilla template contamination**. No change needed; the same piece pair on mountain_meadow#9 (in the air) shows 0. Approved terrain / structure designs untouched.

## 3. forest-only transition (isolated worlds, seed 42, same 25 natural sites as P3.10)
Only the 3 forest pools enabled (`fo_forest_only.json`, restart, `fo_forest_only_reload.json`):
| | meadow (11) | forest (9) | mountain_meadow (5) |
|---|---|---|---|
| template family | vanilla 11/11 | **heaven 9/9** | vanilla 5/5 |
| block fingerprint vs vanilla control | equal 11/11 | n/a (heaven; equal to the P3.10 meadow+forest run 9/9) | equal 4/5 (1 = tick noise) |
| reload (second start, same world) fingerprint equal | 11/11 | 9/9 | 5/5 |
| unresolved jigsaw / vanilla-theme blocks | 0 / (vanilla island) | 0 / 0 | 0 / (vanilla island) |
| loot containers + spawners, island size vs vanilla | identical | identical | identical |
| Habitat support on standable columns | 8279/8772 (vanilla) | 2158/2158 | 2728/4179 (vanilla) |
Partial generation: vanilla west half -> forest pack enabled -> restart -> finish: 0 heaven pieces in 25/25 sites (saved starts keep their vanilla locations; meadow / mountain_meadow equal to control in 10/11 and 4/5, forest within noise: two vanilla runs already differ in 6/9 forest sites).
Real player, forest-only (`realplayer_forest_only.json`; spawned total / on Habitat; vanilla | meadow+forest | forest-only): meadow 21/1 | 30/0 | 29/6; meadow#2 77/11 | 61/4 | 53/6; meadow#3 315/27 | 335/24 | 226/24; forest 41/4 | 26/3 | 36/0; forest#1 60/1 | 78/1 | 71/3; forest#3 225/9 | 258/15 | 164/12; mountain_meadow 31/0 | 33/2 | 37/0. The same mob families spawn in every configuration (vanilla hostiles + sky mobs); values are random, so this is qualitative only (no statistical power claim), as in P3.10.

## 4. Per-theme tool (`tools/heaven-p38/pending_pools.py`)
`apply|revert <meadow|forest|all> [--dry-run]` (theme required; mountain_meadow follows meadow because it shares `sky_islands/meadow/start`). Guard: if any target pool is neither the vanilla text nor exactly the pending text, it refuses and changes nothing. `tools/heaven-p39/rollback_themes.py` (on a copy; `HEAVEN_P38_LIVE`): 20/20 checks PASS - copy == baseline (58 files); no/invalid theme refused; dry-run changes nothing; apply meadow / forest / all change exactly 3 / 3 / 6 expected files (the other theme untouched); apply and revert twice are byte-identical; revert returns to the baseline hashes; mixed order (apply meadow, apply forest, revert meadow -> only forest remains; revert forest -> baseline); a tampered pool makes apply and revert refuse with nothing changed (also for the untouched theme in the same call); production tree == baseline.
Scope (unchanged): rollback restores files / future generation only; chunks already generated and saved StructureStarts keep the pieces they have.

## 5. Open risks
Seed 42 only; spawn-rate equivalence qualitative only; the OOM cause is an assessed heap-pressure explanation (histograms, no dominator analysis of the 5.7 GB dump); production switch cannot be undone for already generated chunks; mountain_meadow cannot be switched independently of meadow (shared pool); other mods / datapacks that edit sky-island pools are untested.

## 6. Full regression
(see the final section appended after the runs)

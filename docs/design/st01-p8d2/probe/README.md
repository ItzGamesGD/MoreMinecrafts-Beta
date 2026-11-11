# P8-D2 probe (evidence tooling; not production, not compiled in this branch)

- `St01D2Probe.java.txt` — the final probe source (modes `fixture`, `bench`, `overlap`, `incr`; inert unless `-Dmm.st01.d2.out` is set).
- `run_probe.sh`, `run_all.sh`, `run_extra.sh`, `run_extra2.sh` — runners; `summarize_bench.py`; `bench-table-raw.md` (all benchmark calls); `fixture-pack/` (the disposable datapack).
- Raw outputs: `../evidence/*.json` (+ `fixture3-map-storage.txt`, `INCOMPLETE.txt`).

**Probe revisions.** The probe was extended while the evidence was gathered, so batches used slightly different revisions of the same file: the first batch (`run_all.sh`) used the original
(blocking-per-candidate `incr`, quantized-centre target check, no deferred reload check); `run_extra.sh` used the version with worker dispatch; the `incrw-debug`/`incrr`/`incrt` runs added
result histograms, retries and FORCED tickets; `run_extra2.sh` used the final file. Behavior relevant to a conclusion is attributed to the run that produced it in the documents.
`evidence/superseded-first-run-skipdefault-older-probe.json` is the first fixture run (older probe); its supply-exhaustion observation was reproduced by the final-probe run.
Runs were executed on one 4-vCPU cloud container; the 20-seed audit set was only sampled (6 seeds for the benchmark, 6 for overlap, 1 for the fixture).
Temporary wiring used: the probe class was listed in the gametest `fabric.mod.json` and run through the existing `runSt01C2BProbe` Gradle configuration; both were reverted (`git diff` of `src/`, `build.gradle`, `.github`, `tools` against the P8-D1 head is empty).

# C-4 Regression report

Baseline reported by C-3 (not evidence for C-4): JUnit 590, new GameTests 5/5. Everything below was re-run on this branch/container (JDK 25.0.5 Temurin, Gradle 9.6.0, Minecraft 26.3).

| Check | Command | Result | Kind |
|---|---|---|---|
| JUnit | `./gradlew test --no-daemon` (and again inside `build`) | **590 / 590 pass**, 0 failed/skipped | [OBSERVED] |
| Composite unit tests | `python3 -m unittest scripts.structures.sky_islands.test_tower_composite` | 5/5 OK (≈31 s) | [OBSERVED] |
| Structure/NBT validator | `python3 scripts/structures/sky_islands/validate.py` | 56 templates PASS | [OBSERVED] |
| Generator drift | `python3 scripts/structures/sky_islands/generate.py --check` | **FAILED on this machine before the fix** (all six `tower.nbt` "stale"); **PASS after fix** | see below |
| Sky Tower GameTests (C-3 baseline) | focused runGameTest | 5/5 pass on the base HEAD | [OBSERVED] |
| Sky Tower GameTests (final) | same | **8/8 pass** (5 + 3 new real-combat) on final sources | [OBSERVED] |
| Full GameTest suite | `./gradlew build --no-daemon` | 535 tests; **534 pass, 1 fail**: `colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise` ("return resumes the same attempt on tick 74") | [OBSERVED] |
| That test alone | `-Dmmskytower.testFilter=…absence_grace…` focused | **passes** (1/1) | [OBSERVED] |
| Worldgen registration | `SkyTowerWorldgenRegistrationTest` (in JUnit) + 12 natural-generation probes | PASS | [OBSERVED] |
| Save/reload | `SkyTowerDataTest`/`LifecycleTest` (JUnit), NBT codec round-trip in GameTest, 2 dedicated-server stop/start probes | PASS | [OBSERVED] |

The `build` task stops at the failing `runGameTest`, so the packaging steps after it (jar/remap) were **NOT RUN** in this phase after that failure.

## generate.py --check (defect, fixed)
`generate.py` byte-compared the gzip files of the six composites. Gzip output depends on the zlib build; here `zlib 1.3` produced different compressed bytes of identical length. Verified: decompressed payloads of all six committed `tower.nbt` equal the generator's output (`gzip.decompress(a)==gzip.decompress(b)` True ×6), so the assets are correct. The check now compares decompressed payloads (`scripts/structures/sky_islands/generate.py`). No asset rewritten; approved NBT SHA unchanged.

## Colosseum instability (documented separately, not suppressed)
Recurred: 1 failure in the full 535-test batch, the same test C-3 recorded (529/530 then), unrelated to Sky Tower code or my gametest-only changes (no `src/main` change). It passes alone, consistent with the load sensitivity in `REPRODUCE.md`. I did not modify Colosseum code, timings or the test; I did not check it against an older baseline commit **[UNVERIFIED]** beyond C-3's earlier record. Treat as out-of-scope finding.

## Other notes
- A first `./gradlew test` failed on a transient Maven Central 429; retry passed (environment, not code).
- The full build was started while I edited the probe source; the probe change is gametest-only, and the 8 Sky Tower tests were re-run afterwards on the final sources.

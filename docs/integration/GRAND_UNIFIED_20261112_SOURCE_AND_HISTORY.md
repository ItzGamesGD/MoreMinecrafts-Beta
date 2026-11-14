# Grand Unified working branch — source, preservation and history corrections (2026-11-12)

Branch: `work/grand-unified-20261112` (local working branch). Source baseline: `49747981ecc49f7f6b0a39fe62b23bd1a8d42d02` (`baseline/unified-20261111`, same SHA as `checkpoint/grand-unified-20261111-candidate-2`). Tree `ef6e65c18240f4426cffe8f39234accc882e9c43`.

## What this branch is

The baseline tree plus (1) approved or pending documents that were missing from it, (2) a feature registry, (3) an `inactive/` directory holding byte-identical copies of unfinished, paused or blocked development assets, and (4) an isolation check script. **No file under `src/`, `.github/`, `build.gradle`, `gradle/` or resource directories is modified.** Runtime behavior is unchanged by construction.

## Verified history facts (full history, repository unshallowed)

* The baseline commit has two parents: `f5777244` (`integration/unified-20261111`) and `cde842dd` (`main`). It used merge strategy `ours`, so its tree is identical to `f5777244`.
* `main` is an **ancestor of the integration line**: `git merge-base main integration/unified-20261111` is `cde842dd` (main's tip), and `integration/unified-20261111` is 502 commits ahead of `main`. The baseline's second parent is therefore redundant but harmless, and the `ours` merge discarded nothing from `main`. `stage-20/checkpoint` is an ancestor of the integration line. Everything in `main` is reachable from the baseline (`baseline..main` is empty; `main..baseline` is 503 commits). A future `main` update can be a fast-forward from the integration line or from this lineage.
* The tree difference between `main` and the baseline (3,246 files added, 125 modified, 1 deleted) is the integration line's own development. `XpLevels.java` was deleted on the integration line by `a13d9777` ("integration: compose Research Quest with accessory bounty", 2026-10-24) in favor of the exact raw-XP implementation.
* CI evidence: all check runs on `f5777244` (same tree) succeeded, including Fabric CI, client visual validation and the full-history secret scan. The baseline commit itself had no check runs.
* `checkpoint/grand-unified-20261111-candidate-1` (`904b8001`) is an older subset; it differs from the baseline in 2 files.
* Branch classification against the baseline, computed on full history: 85 contained, 5 patch-equivalent, 66 with unique patches (156 remote branches).

## Corrections to earlier statements

1. The baseline commit message says original `main` is preserved "by tag `archive/main-pre-baseline-20261111-cde842dd`". **No tag exists** (the remote has zero tags). The ref is a *branch* with that name at `cde842dd`, which is also the SHA of `main`.
2. An earlier revision of this file (Phase 1/2) stated that the integration and wave lines share no history with `main` and that Draft PR #273 has no merge base. **That was wrong.** It was an artifact of analysing a shallow clone: the clone's boundary commit `ab531c74` looked like a root. With full history the lines are connected as described above, and PR #273's head contains `main`. The same artifact had classified one branch (`stage-ci/accessory-bounty-20261023`) as having unique patches; it is contained in the baseline.

## Preservation references

History of every consolidated or external asset stays on its source ref; SHAs are in `docs/integration/FEATURE_STATUS_REGISTRY_20261112.tsv` and `inactive/MANIFEST.tsv`. The 38 refs that reach all 1,912 commits of the 156 audited remote branches are: the baseline, the 33 family heads and four patch-equivalent heads (`integration/wave10-unified-20261025`, `docs/d13-03-authority-q01-q47-20261106`, `remediation/st01-colosseum-clock-seam-20261110`, `stage-audit/wave15-candidate-20261028`). No branch or PR was changed by this work.

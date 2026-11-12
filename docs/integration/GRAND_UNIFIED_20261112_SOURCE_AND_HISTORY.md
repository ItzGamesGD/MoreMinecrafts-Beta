# Grand Unified working branch — source, preservation and history corrections (2026-11-12)

Branch: `work/grand-unified-20261112` (local working branch). Source baseline: `49747981ecc49f7f6b0a39fe62b23bd1a8d42d02` (`baseline/unified-20261111`, same SHA as `checkpoint/grand-unified-20261111-candidate-2`). Tree `ef6e65c18240f4426cffe8f39234accc882e9c43`.

## What this branch is

The baseline tree plus (1) approved or pending documents that were missing from it, (2) a feature registry, (3) an `inactive/` directory holding byte-identical copies of unfinished, paused or blocked development assets, and (4) an isolation check script. **No file under `src/`, `.github/`, `build.gradle`, `gradle/` or resource directories is modified.** Runtime behavior is unchanged by construction.

## Verified history facts

* The baseline commit has two parents: `f5777244` (`integration/unified-20261111`) and `cde842dd` (`main`). It used merge strategy `ours`, so its tree is identical to `f5777244`. Everything on `main` is an ancestor of the baseline (`git rev-list baseline..main` is empty); `main..baseline` is 346 commits. A future `main` update from this lineage can be a fast-forward.
* The integration and wave lines (`integration/unified-20261111`, `checkpoint/wave10..16-unified`) have **no common ancestor with `main`** (`git merge-base` finds none; roots `ab531c74` versus `0e1a626d`). `stage-20/checkpoint` is an ancestor of the baseline only through the `main` parent. Draft PR #273 (`integration/unified-20261111` to `main`) therefore has no merge base; the baseline commit is what connects the two histories.
* The `ours` merge kept none of `main`'s file versions: `main` and the baseline tree differ by 3,246 added, 125 modified and 1 deleted file (`XpLevels.java`, replaced by the exact raw-XP implementation). For the 105 modified files whose `main` version never occurs in the integration history, the integration's last change is not older than `main`'s (date comparison, not a semantic review).
* CI evidence: all check runs on `f5777244` (same tree) succeeded, including Fabric CI, client visual validation and the full-history secret scan. The baseline commit itself has no check runs.
* `checkpoint/grand-unified-20261111-candidate-1` (`904b8001`) is an older subset; it differs from the baseline in 2 files.

## Corrections to earlier statements

1. The baseline commit message says original `main` is preserved "by tag `archive/main-pre-baseline-20261111-cde842dd`". **No tag exists** (the remote has zero tags). The ref is a *branch* with that name at `cde842dd`, which is also the SHA of `main`. It protects nothing beyond `main` itself.
2. Statements that `integration/unified-20261111` is a branch "of" or directly mergeable into `main` are inaccurate: they share no history (see above). Use the baseline lineage.

## Preservation references

History of every consolidated or external asset stays on its source ref; SHAs are in `docs/integration/FEATURE_STATUS_REGISTRY_20261112.tsv` and `inactive/MANIFEST.tsv`. The 38 refs that reach all 1,912 commits of the 156 audited remote branches are: the baseline, the 33 family heads and four patch-equivalent heads (`integration/wave10-unified-20261025`, `docs/d13-03-authority-q01-q47-20261106`, `remediation/st01-colosseum-clock-seam-20261110`, `stage-audit/wave15-candidate-20261028`). No branch or PR was changed by this work.

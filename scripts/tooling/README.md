# Integration and GameTest checks

Run these from the repository root with Python 3 and Git. They use only the Python standard library. Both commands write deterministic JSON and Markdown files. Exit code 0 means no review item, 2 means a review item or incomplete analysis. The checkers never edit source or merge refs.

## Integration manifest

Use locally available, complete commits. The first argument must be an ancestor of both candidates; normally it is their merge base.

```sh
python3 scripts/tooling/integration_manifest.py BASE_SHA REF_A REF_B \
  --json-report /tmp/integration.json --markdown-report /tmp/integration.md
```

The checker reads recursive Git trees, including file mode and object IDs. It groups paths changed on one side, shared paths with identical endpoints, and shared paths with differing endpoints. Deletions, mode changes, and Git's detected renames or copies require review. A differing shared endpoint also requires semantic review. A zero exit does not prove gameplay compatibility; it means the structural inventory is complete and has no flagged hazard. Fetch missing commits or deepen a shallow clone before retrying an incomplete result.

Git rename detection is similarity based. Check deletion and addition pairs even when Git does not identify a rename. Matching endpoints show content and mode equality only. This tool does not perform a merge or detect runtime interactions between different paths.
Gitlink entries are compared by recorded commit ID; the checker does not fetch submodule repositories.

## GameTest coverage

```sh
python3 scripts/tooling/gametest_coverage.py \
  --json-report /tmp/gametest.json --markdown-report /tmp/gametest.md
```

The scanner finds Java methods annotated with Fabric's `@GameTest` in `src/gametest/java`, then compares their classes with `src/gametest/resources/fabric.mod.json`'s `fabric-gametest` entry points. An unregistered class, unresolved entry point, stale exclusion, or malformed configuration exits 2. The existing build workflow runs `runGameTest` for registered classes; the separate tooling workflow runs this static check on relevant PR changes.

To intentionally omit an annotated class, add its fully qualified name and a specific reason to `gametest_exclusions.json`. Review the change like code. An empty allowlist is valid; unknown classes are never excluded implicitly. Remove exclusions when they become registered or their source is removed.

This is a source scanner, not a Java parser or a runtime execution proof. It recognizes the repository's imported `@GameTest` method convention and ordinary Fabric class entry points. It does not count unannotated helper methods, resolve generated tests, or inspect Gradle runtime logs. An unusual Java annotation or entry-point form may need scanner support and a fixture before use.

## Self-tests

```sh
python3 -m unittest discover -s scripts/tooling -p 'test_*.py' -v
```

Tests create temporary Git and Java fixtures; they do not run the Minecraft build.

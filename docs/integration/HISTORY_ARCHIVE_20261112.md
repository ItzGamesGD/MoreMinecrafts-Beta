# History archive and recovery instructions (2026-11-12)

The Grand Unified tree preserves the *head state* of each development family. Commit histories, interior stack tips, closed-PR heads and historical checkpoints are preserved in a **local git bundle kept outside this repository**. It is not published and is not committed.

| Item | Value |
|---|---|
| File | `moreminecrafts-history-20261112.bundle` (outside the repository) |
| SHA-256 | `3512e072b9d4838d288a4215c3bbf66f2982035d34869a083dd4eb87c7f52fc3` |
| Size | 547,623,258 bytes |
| Refs | 431: 3 local branches, 156 remote-tracking branches (`refs/remotes/origin/*`), 272 pull-request heads (`refs/remotes/pr/*`) |
| Commits | 2,217 (full history; the source repository was unshallowed first) |
| Taken from | the repository state at `work/grand-unified-20261112` = `8fe71c63` plus all origin refs fetched on 2026-11-12 |

## Verification performed

1. `git bundle verify` succeeded.
2. A fresh empty repository fetched `+refs/*:refs/*` from the bundle and restored **all 431 refs with identical SHAs**; `git fsck --full` reported no errors; `git rev-list --all --count` returned 2,217 in both the source repository and the restored repository; the restored repository is not shallow.
3. Gitleaks 8.30.1 (the version pinned by `.github/workflows/pre-public-security-scan.yml`) scanned the restored full history: 12 findings without an allowlist, all 12 matching the existing `.gitleaksignore` fingerprints (documented historical false positives); 0 with the repository's allowlist. The 13th finding that appeared in a local, never-pushed commit was a documentation quote and was kept out of the pushed history.

## Security caveats

* The archive contains the 12 allowlisted historical false-positive commits, including commit `cec8bff8` (reachable only from the ST-05 probe branches) and the original `freeze_integrity.py` bytes. A bundle is a binary pack; scanners do not inspect it, so **do not publish it or treat it as scanned by virtue of being packed**. The scan above was run on a restored repository, not on the bundle file.
* Closed-PR heads (272 pull refs) are included; 299 commits are reachable only from PR heads or local refs, not from any remote branch.
* Session storage is ephemeral: **move the bundle and its `.sha256` to durable, access-controlled storage before this environment is discarded.** I could not store it durably from here.

## Recovery instructions

```bash
sha256sum -c moreminecrafts-history-20261112.bundle.sha256
git init restored && cd restored
git bundle verify ../moreminecrafts-history-20261112.bundle
git fetch ../moreminecrafts-history-20261112.bundle '+refs/*:refs/*'
git branch -a                       # local branches; remote-tracking branches are under refs/remotes/origin
git log refs/remotes/origin/<branch>
git update-ref refs/heads/<name> refs/remotes/origin/<branch>   # recreate a branch
```

Recreate a deleted remote branch with `git push origin refs/remotes/origin/<branch>:refs/heads/<branch>` after verifying the SHA against `docs/integration/GRAND_UNIFIED_20261111_BRANCH_INVENTORY.tsv` or the registry.

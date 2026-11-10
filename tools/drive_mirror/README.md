# Drive mirror

A full-fidelity, continuously synchronised Google Drive copy of **every branch** (and tag)
of this repository: exact relative paths, exact bytes, every tracked file type (source,
docs, textures, NBT, JSON, binaries), one namespace per ref, with Git metadata stored next
to it. It is driven by `.github/workflows/drive-mirror.yml`.

> **Live status: BLOCKED until configured.** No Google Drive credentials or destination
> existed when this was built, so the mirror has only been exercised against the local Drive
> API emulator (`tests/fake_drive.py`). Nothing in this directory claims a live Google run.
> Follow [Setup](#setup) and then [Controlled live test](#controlled-live-test).

The tool only **reads** from Git/GitHub. It never pushes, comments, labels or edits anything
in the source repository.

---

## Drive layout

```
<parent folder>/                                ← DRIVE_PARENT_FOLDER_ID (you create/share this)
└── MoreMinecrafts-Beta/                        ← mirror root (name = DRIVE_MIRROR_NAME)
    ├── branches/
    │   ├── main/                               ← exact tree of refs/heads/main
    │   │   ├── build.gradle
    │   │   ├── src/main/resources/…/structure.nbt
    │   │   └── …
    │   ├── work%2Fst01-p8b2-royal-chest-runtime-20261110/
    │   └── checkpoint%2Fwave16-unified-20261104/
    ├── tags/                                   ← one folder per tag (peeled to its commit)
    ├── pulls/                                  ← only with --pull-heads orphaned|all
    │   └── 83/                                 ← tree of refs/pull/83/head
    ├── _retired/                               ← namespaces of deleted refs, moved here (never trashed)
    │   └── branches__feature%2Fold@20261110T031500Z/
    └── _mirror/                                ← metadata Git semantics need but folders cannot express
        ├── state.json                          ← per-namespace status, commit, sync time, run history
        ├── refs.json                           ← full ls-remote: heads, tags (+peeled), refs/pull/*, HEAD symref
        ├── pulls.json                          ← PR metadata (number, state, draft, head/base ref+sha, merged_at…)
        ├── lock.json                           ← writer lease
        └── manifests/
            ├── branches__main.json             ← one manifest per namespace (see below)
            └── branches__work%2Fst01-….json
```

**Ref → folder name.** The ref short name with every character outside `[A-Za-z0-9._-]`
percent-encoded (`work/st01-p1` → `work%2Fst01-p1`). The encoding is reversible and
collision-free, keeps one flat folder per ref, and is safe for Drive for desktop on
Windows/macOS. (Git forbids `a` and `a/b` coexisting, but flat names make it obvious where a
branch ends.)

**Files.** Each tracked file is a regular Drive file whose name is the last path segment,
inside folders for its directories. Bytes are uploaded unmodified and the upload never asks
for conversion (target type is the file's own MIME type or `application/octet-stream`,
never `application/vnd.google-apps.*`). Every write is checked against Drive's
server-computed `sha256Checksum` and `size`.

Every mirrored item carries `appProperties` (private to the app, invisible in the UI):

| key | value |
|---|---|
| `mm_role` | `root`, `meta`, `container`, `ns`, `dir`, `file`, `metafile`, `retired` |
| `mm_mirror` | random token of this mirror root (scopes every query) |
| `mm_ns` | random token of one namespace *incarnation* (a re-created branch gets a new one) |
| `mm_blob` | Git blob object id |
| `mm_sha256` | SHA-256 of the bytes |
| `mm_mode` | Git mode: `100644`, `100755` (executable), `120000` (symlink) |

**Manifest** (`_mirror/manifests/<container>__<name>.json`): source ref, full commit metadata
(sha, tree, parents, author, committer, dates, subject), `synced_at`, run id, and for every
path `{id, blob, mode, size, sha256, mime}`, the folder map, and an `unsupported` list.

**Entries Drive cannot represent** are recorded, never dropped silently:

* **Symlinks** (`120000`) are stored as a file whose bytes are the link target — exactly
  Git's blob — with `mm_mode=120000`.
* **Executable bit** is kept in `mm_mode` and the manifest (Drive has no mode bits).
* **Submodules** (`160000` gitlinks) have no bytes in this repository; they are listed in
  the manifest's `unsupported` array with their commit id and reported by `verify`.
  (This repository currently has none — see `docs/audit/REPO-STATE-20261110/`.)
* Empty directories cannot exist in Git, so none exist in the mirror.

---

## How synchronisation works

Every run mirrors the **whole** remote state; the triggering event only decides *when*.

1. **Snapshot.** Fetch `refs/heads/*` (and tags, and optionally PR heads) into a bare
   cache, then `git ls-remote`; refetch until the two agree, so every namespace is synced
   from a value the remote really advertised.
2. **Per ref,** the desired state is `git ls-tree -r <commit>`. The current state is the
   namespace's manifest when its last sync completed, otherwise a full crawl of the folder.
3. **Diff and apply** (bounded thread pool):
   * unchanged → nothing; changed bytes → in-place content update (file id kept);
   * mode-only change → metadata update;
   * **rename** (path vanished, identical bytes appeared elsewhere) → move + rename, file id kept;
   * new file → **server-side copy** of an identical blob already on Drive when one is known
     (new branches cost almost no upload), otherwise upload (multipart, or resumable ≥ 5 MB);
   * deletions → Drive **trash** (recoverable 30 days), only after every desired file is in
     place and after re-checking the namespace folder's identity;
   * obsolete folders → trashed.
4. Write the manifest, mark the namespace `complete` in `state.json`.
5. **Deleted refs** are retired only if a second, independent `ls-remote` also lacks them;
   their folder is moved to `_retired/`. More than max(5, 20 %) at once stops with
   `SAFETY_STOP` unless `--allow-mass-retire`.

Properties that follow from diffing against Drive rather than Git history:

| Situation | Behaviour |
|---|---|
| Force-push / history rewrite | Same diff; stale files are trashed; `forced_update: true` in the report |
| Branch deleted then re-created | Old namespace retired; new incarnation (`mm_ns`) created |
| Interrupted run (crash, kill, timeout) | Namespace stays `in_progress`; next run crawls it and converges. Creates use ids from `files.generateIds`, so a create whose response was lost is recognised on retry (HTTP 409) instead of duplicated |
| Repeated run, nothing changed | Zero writes to any namespace (only `state.json` and the lease) |
| Someone edits the Drive copy | `incremental` trusts manifests and does not notice; the daily `reconcile` crawls every namespace, trashes extras, duplicates and converted Google-format copies, and restores bytes |
| Partial Drive listing (`incompleteSearch`) | Treated as an error; never used for deletions |
| Search index lag after writes | `verify` confirms such files by direct GET (strongly consistent) |

### Single writer

* The `mirror` job uses `concurrency: drive-mirror-<repo>` with `cancel-in-progress: false`:
  one sync at a time; a newer pending run replaces an older pending one (safe, because each
  run syncs everything).
* A Drive-side lease (`_mirror/lock.json`, 20 min TTL, renewed while working) stops manual
  runs from other machines from writing concurrently. `--break-lock` after a killed run.

### Modes and exit codes

`incremental` (every event, hourly) · `reconcile` (daily 03:41 UTC: crawl all, repair,
deep-verify 300 random files by download + git-blob hash) · `verify-only` · `dry-run`.

| exit | meaning |
|---|---|
| 0 | complete / PASS |
| 1 | failed (details in report) |
| 2 | stopped at `--time-budget-min`; resumable (the initial sync takes several runs) |
| 3 | lease held by another writer |
| 4 | BLOCKED — no credentials, destination missing, or access denied |
| 5 | safety stop (mass retirement, empty tree, ambiguous mirror root) |

`verify` re-fetches the remote, re-reads every tree, hashes the blobs itself, walks Drive
itself and compares against Drive's own checksums; it shares no planning code with `sync`.
`status` is a cheap freshness gate (state vs. `ls-remote`, no crawl) that fails when a
namespace has not converged for `DRIVE_MIRROR_MAX_AGE_HOURS` (default 26).

---

## Security model

* **GitHub side is read-only.** `contents: read` and `pull-requests: read`. Git access uses
  the run's token through environment-only config; it is never written to disk.
* **Credentials only on the trusted branch.** The `mirror` job runs only when the workflow
  executes in the context of `DRIVE_MIRROR_TRUSTED_BRANCH` (default `main`): pushes to
  `main`, schedules, dispatches, branch deletions and PRs targeting `main`. Events anywhere
  else run the credential-free `relay` job, whose only permission is `actions: write`, to
  request a `workflow_dispatch` on the trusted branch. A pushed branch therefore cannot
  change the code that runs with Drive access.
* **Keyless by default.** Workload Identity Federation: GitHub's OIDC token is exchanged for
  a short-lived Google token. No key file exists. Restrict the provider's attribute condition
  to this repository *and* `refs/heads/main`.
* **Public logs.** This repository is public, so workflow logs and artifacts are public.
  Destination ids are masked, reports are scrubbed of Drive object ids, and preflight
  reports only the account *type*, never its address.
* Repository content is treated as data: nothing from a mirrored tree is executed.

---

## Setup

### 1. Choose the destination

| | A. Google Workspace shared drive (recommended) | B. Personal Google account |
|---|---|---|
| Who owns files | the shared drive | the person |
| Credential | WIF → service account (keyless) | OAuth client + refresh token (long-lived secret) |
| Storage | pooled organisation quota | **15 GB free tier, shared with Gmail/Photos** — the mirror needs ~12 GB today |
| Item limit | **500,000 items per shared drive** — the mirror needs ~247,000 today | none documented for My Drive |

Service accounts cannot own files in a personal My Drive, which is why B needs OAuth.

### 2A. Shared drive + Workload Identity Federation

1. Create a Google Cloud project; enable the **Google Drive API**.
2. Create a service account (no keys).
3. Create a Workload Identity Pool + OIDC provider for `https://token.actions.githubusercontent.com`
   with attribute condition
   `assertion.repository == 'ItzGamesGD/MoreMinecrafts-Beta' && assertion.ref == 'refs/heads/main'`.
4. Grant `roles/iam.workloadIdentityUser` on the service account to
   `principalSet://iam.googleapis.com/<pool>/attribute.repository/ItzGamesGD/MoreMinecrafts-Beta`.
5. Create a shared drive (or a folder in one); add the service account as **Content manager**.

### 2B. Personal account + OAuth (drive.file)

1. Google Cloud project with the Drive API; OAuth consent screen **External**, publishing
   status **In production** (in *Testing* refresh tokens expire after 7 days). Scope:
   `https://www.googleapis.com/auth/drive.file` only — the app can then see only files it
   created, not the rest of the Drive. `drive.file` is non-sensitive; no verification needed.
2. Create a *Desktop* OAuth client; obtain a refresh token once on your own machine
   (e.g. `google-auth-oauthlib`'s installed-app flow) for that scope.
3. With `drive.file`, use `root` as the parent folder: the mirror creates its own root
   folder (it cannot see folders it did not create). You may move it afterwards.

### 3. GitHub configuration

Create environment **`drive-mirror`** (Settings → Environments), deployment branches:
**`main` only**. Then:

| kind | name | value |
|---|---|---|
| variable (repo) | `DRIVE_MIRROR_ENABLED` | `true` — nothing runs until this is set |
| variable | `DRIVE_AUTH_MODE` | `adc` (2A) or `oauth` (2B) |
| variable | `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT` | 2A only |
| variable | `DRIVE_MIRROR_NAME` | optional; default = repository name |
| variable | `DRIVE_MIRROR_PULL_HEADS` | optional: `none` (default), `orphaned`, `all` |
| variable | `DRIVE_MIRROR_TRUSTED_BRANCH` | optional; default `main` |
| variable | `DRIVE_MIRROR_MAX_AGE_HOURS`, `DRIVE_MIRROR_EXPECTED_BYTES` | optional |
| secret (env) | `DRIVE_PARENT_FOLDER_ID` | folder id, or `root` (2B) |
| secret (env) | `DRIVE_SHARED_DRIVE_ID` | 2A only |
| secret (env) | `DRIVE_OAUTH_CLIENT_ID`, `DRIVE_OAUTH_CLIENT_SECRET`, `DRIVE_OAUTH_REFRESH_TOKEN` | 2B only |

The workflow must be on `main` for schedules and dispatches to exist; merging it is an
owner decision.

### Controlled live test

Run against a throw-away namespace first, then the real one:

1. Actions → *Drive mirror* → *Run workflow*: `mode=dry-run`, `mirror_name=MMB-e2e-test`.
   Check the preflight table (auth, destination, quota, write probe) in the job summary.
2. `mode=incremental`, `mirror_name=MMB-e2e-test`, `time_budget_min=30`. Repeat until the
   summary says `complete` (exit 2 = resumable, expected for the first runs).
3. `mode=verify-only`, `mirror_name=MMB-e2e-test` → must be **PASS**.
4. Push a commit to a scratch branch, delete a scratch branch, then `incremental` +
   `verify-only` again. Re-run `incremental` with no changes and confirm `changed: 0`.
5. Only then unset the override; the scheduled runs build the real `MoreMinecrafts-Beta` root.

Local equivalent (any machine with credentials):

```sh
export PYTHONPATH=tools DRIVE_AUTH_MODE=oauth DRIVE_OAUTH_CLIENT_ID=… DRIVE_OAUTH_CLIENT_SECRET=… DRIVE_OAUTH_REFRESH_TOKEN=…
python3 -m drive_mirror preflight --parent-folder-id root --write-probe
python3 -m drive_mirror sync   --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --mirror-name MMB-e2e-test --parent-folder-id root
python3 -m drive_mirror verify --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --mirror-name MMB-e2e-test --parent-folder-id root --deep-sample 200
```

---

## Capacity and quotas (measured on 2026-11-10)

| | value |
|---|---|
| branches / tags | 126 / 0 |
| file entries across all branches | 214,364 (4,753 unique blobs, 339 MB unique) |
| bytes materialised across branches | 11.93 GB |
| Drive items incl. folders | 247,099 (214,364 files + 32,471 folders + metadata), measured in the emulator run |
| initial sync | 247,354 write requests, of which only 4,805 carry file bytes (the rest are server-side copies and folders). Google paces sustained writes per user; at a few writes per second this is on the order of a day, i.e. several 320-minute runs, each resuming where the last stopped. Not measured live |
| steady state | a push touching N files costs ≈ N writes; a new branch ≈ one copy per file and almost no upload |

Each branch is a full copy because the requirement is per-branch path fidelity; Drive has
no cross-folder hard links. Growth is roughly *(files per branch) × (number of branches)*.
Retiring stale branches on GitHub shrinks the live tree (retired folders stay in `_retired/`
until someone deletes them).

## Limitations and risks

* **Not yet run against Google.** Real behaviour of quotas, `sha256Checksum` availability,
  the exact error code for a reused generated id (handled as 409 or 400/`fileIdInUse`), and
  `addParents`/`removeParents` on retried moves are unverified.
* Free personal storage (15 GB) is nearly exhausted by today's mirror; a shared drive is
  bounded at 500k items. Both limits are reached by growth in branch count, not code.
* Drive search is eventually consistent; `verify` compensates with direct GETs, and
  `reconcile` may need a second pass if it ran seconds after a large write.
* PR heads not contained in any branch (50 today) are mirrored only with
  `--pull-heads orphaned`; otherwise they exist only in `refs.json`.
* Git history itself (commits other than ref heads) is not mirrored; `refs.json` and
  manifests record the head commits. A `git bundle` would be the right tool for that.
* `incremental` runs do not detect manual edits on Drive; the daily `reconcile` does.

## Tests

```sh
PYTHONPATH=tools python3 -m unittest discover -s tools/drive_mirror/tests -v      # ~2 min
PYTHONPATH=tools python3 tools/drive_mirror/tests/full_repo_emulator.py \
  --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --work-dir /tmp/mm.git --out-dir /tmp/mm-out
```

Results of the last run are in [`TEST_EVIDENCE.md`](TEST_EVIDENCE.md).

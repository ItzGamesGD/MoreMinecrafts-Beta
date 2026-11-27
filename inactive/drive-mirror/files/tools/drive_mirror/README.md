# Drive mirror

A full-fidelity, continuously synchronised Google Drive copy of **every branch** (and tag)
of this repository: exact relative paths, exact bytes, every tracked file type (source,
docs, textures, NBT, JSON, binaries), one namespace per ref, with Git metadata stored next
to it. It is driven by `.github/workflows/drive-mirror.yml`.

> **Live status: BLOCKED until configured.** No Google Drive credentials or destination
> existed when this was built, so the mirror has only been exercised against the local Drive
> API emulator (`tests/fake_drive.py`). Nothing in this directory claims a live Google run.
> Follow [Setup](#setup) and then [Controlled live test](#controlled-live-test).
>
> **Security gate:** live synchronization is additionally held back by the secret-scan gate
> (see [Security gate](#security-gate)): one finding is still awaiting an owner decision, and the
> gate quarantines the refs it touches. See `docs/audit/REPO-STATE-20261110/SECRET_SCAN_INVESTIGATION.md`.
> Scope and integrity claims: [`MIRROR_COMPLETENESS.md`](MIRROR_COMPLETENESS.md).
> **Operating it:** [`RUNBOOK.md`](RUNBOOK.md) (configuration checklist, ordered manual steps, enabling and disabling the workflow, recovery, capacity).
> Status: offline ready, live Google Drive verification pending.

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
    ├── pulls/                                  ← PR heads no branch contains (default; --pull-heads all|none)
    │   └── 83/                                 ← tree of refs/pull/83/head, manifest source.kind = "pull_head"
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

**Manifest** (`_mirror/manifests/<container>__<name>.json`): source ref, a `source` block
(`kind` = `branch` | `tag` | `pull_head`, the full ref, and for PR heads the PR number, state, head/base
refs and whether any branch contains the commit), full commit metadata (sha, tree, parents, author,
committer, dates, subject), `synced_at`, run id, and for every path
`{id, blob, mode, size, sha256, mime}`, the folder map, and an `unsupported` list. The verifier
checks that every manifest's `ref` and `source` match the namespace it describes, and that no
manifest exists without a namespace.

**PR-only refs.** A branch name can legally look like a PR ref (`refs/heads/pull/7/head`), so the
two live in different containers (`branches/pull%2F7%2Fhead` vs `pulls/7`). A PR head that no branch
contained when it was first mirrored stays mirrored for as long as the remote advertises it, even
after a branch later contains its commit (a PR snapshot is not the same tree as a branch tip).

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
5. **Deleted refs** are retired only when deletion is *confirmed*, never merely inferred:
   a second full `ls-remote` must lack the ref **and** a targeted `ls-remote <ref>` must succeed
   and return nothing. A failed, timed-out or denied query is "inaccessible": the namespace is
   kept, marked `unconfirmed` in `state.json`, listed under `retire_deferred`, and `verify`
   reports `ref_unconfirmed`. For `refs/pull/N/head` the GitHub API must also say the PR no longer
   exists (GitHub does not delete PRs, so a vanished PR head is treated as an anomaly until then).
   Retired folders are moved to `_retired/`. More than max(5, 20 %) at once stops with
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

`incremental` (every event, hourly) · `reconcile` (crawl every namespace, repair drift, deep-verify
300 random files by download + git-blob hash) · `verify-only` · `dry-run`.

**Reconcile cadence is state-driven.** An incremental run upgrades itself to a reconcile when
`state.json`'s `last_reconcile_at` is older than `DRIVE_MIRROR_RECONCILE_AFTER_HOURS` (default 24).
A dedicated daily cron could be starved: Actions keeps only one *pending* run per concurrency
group and replaces it with a newer one, so on a busy mirror a scheduled reconcile might never start.

**Verification cost.** After an incremental sync, `verify --sync-report` reads Drive and hashes
blobs only for the namespaces that sync changed (every namespace still gets the cheap structural
checks: inventory, state, manifest, commit match, file count). A reconcile, or any run that did not
complete, is verified in full. Both directory walks are parallel (`--crawl-workers`, default 8);
a serial walk of the ~32,000 folders would take hours against real Drive latency. A scoped
verification cannot see drift in a namespace the sync did not touch; the full verification can.

| exit | meaning |
|---|---|
| 0 | complete / PASS |
| 1 | failed (details in report) |
| 2 | stopped at `--time-budget-min`; resumable (the initial sync takes several runs) |
| 3 | lease held by another writer |
| 4 | BLOCKED — no credentials, destination missing, or access denied |
| 5 | safety stop (mass retirement, empty tree, ambiguous mirror root) |
| 6 | security gate: unapproved findings (affected refs quarantined) |
| 7 | security gate could not decide, **or the approved source snapshot is missing, invalid or no longer matches** (sync/verify): fail closed, nothing uploaded |

`verify` re-fetches the remote, re-reads every tree, hashes the blobs itself, walks Drive
itself and compares against Drive's own checksums; it shares no planning code with `sync`.
`status` is a cheap freshness gate (state vs. `ls-remote`, no crawl) that fails when a
namespace has not converged for `DRIVE_MIRROR_MAX_AGE_HOURS` (default 26).

---

## Security gate

**One frozen snapshot.** `python -m drive_mirror prepare --snapshot-out …` fetches every branch, tag and PR head once and
freezes `ref -> commit` (and tree ids). `secgate --source-snapshot … --run-scanner gitleaks` runs the same pinned,
checksum-verified gitleaks as `pre-public-security-scan.yml` over **exactly those commits** (`--redact=100`; only rule, file and
commit are ever printed), evaluates every ref of the snapshot, and writes an approval that embeds the snapshot. `sync` and `verify` take
their refs from that approval and never fetch, so every uploaded blob belongs to a commit the gate examined; anything pushed after the
snapshot is picked up, and gated, by the next run. `sync` refuses (exit 7) a missing, altered, mismatched, stale or unbound approval before
writing anything. Invariant, checks and limits: [`security/GATE_SYNC_HANDOFF.md`](security/GATE_SYNC_HANDOFF.md).

`secgate` compares each finding's fingerprint with `security/approved_findings.json`:

* `approved` — an owner-reviewed finding (the 11 entries already in the repository's
  `.gitleaksignore`, plus the re-verification recorded in each entry's `basis`): passes.
* `proposed` — analysed but awaiting an owner decision (today: `cec8bff8…:freeze_integrity.py:…:23`): **blocks**.
* anything else: **blocks**.

Blocking does not stop the mirror: it **quarantines exactly the refs whose tip tree would upload the
flagged content** (the same blob anywhere, or the flagged line at the same path). Quarantined refs are
never uploaded, are listed in the report, `state.json` and the job summary, and `verify` fails
(`namespace_missing`) unless it is given the same approval (`--quarantine-file`), so a quarantine can never be
mistaken for a complete mirror. If flagged content was uploaded *before* the gate fired, `verify`
reports `quarantined_content_on_drive`. An unreadable or missing scan report fails the job
(fail closed). The gate never edits an allowlist: approving a finding is a reviewed commit by the owner.

## Security model

* **GitHub side is read-only.** `contents: read` and `pull-requests: read`. Git access uses
  the run's token through environment-only config; it is never written to disk.
* **Credentials only on the trusted branch.** The `mirror` job runs only when the workflow
  executes in the context of `DRIVE_MIRROR_TRUSTED_BRANCH` (default `main`): pushes to
  `main`, schedules, dispatches, branch deletions and PRs targeting `main`. Events anywhere
  else run the credential-free `relay` job, whose only permission is `actions: write`, to
  request a `workflow_dispatch` on the trusted branch. A pushed branch therefore cannot
  change the code that runs with Drive access.
* **Pinned actions.** The credential-bearing job pins every third-party action to a commit SHA
  (`actions/checkout`, `actions/setup-python`, `google-github-actions/auth`, `actions/upload-artifact`).
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
| Storage | pooled organisation quota | **15 GB free tier, shared with Gmail/Photos** — the mirror needs ~14.6 GB logical today (assuming server-side copies are charged at full size; unverified live) |
| Item limit | **500,000 items per shared drive** — the mirror needs ~330,000 today | none documented for My Drive |

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
| variable | `DRIVE_MIRROR_PULL_HEADS` | optional: `orphaned` (default), `all`, `none` |
| variable | `DRIVE_MIRROR_TRUSTED_BRANCH` | optional; default `main` |
| variable | `DRIVE_MIRROR_TOOLING_REF` | optional; ref of the mirror tooling to check out; default = the run's own `github.sha` |
| variable | `DRIVE_MIRROR_MAX_AGE_HOURS`, `DRIVE_MIRROR_RECONCILE_AFTER_HOURS`, `DRIVE_MIRROR_EXPECTED_BYTES` | optional |
| secret (env) | `DRIVE_PARENT_FOLDER_ID` | folder id, or `root` (2B) |
| secret (env) | `DRIVE_SHARED_DRIVE_ID` | 2A only |
| secret (env) | `DRIVE_OAUTH_CLIENT_ID`, `DRIVE_OAUTH_CLIENT_SECRET`, `DRIVE_OAUTH_REFRESH_TOKEN` | 2B only |

The workflow must be on `main` for schedules and dispatches to exist; merging it is an
owner decision.

### Controlled live test

> **Read [`RUNBOOK.md`](RUNBOOK.md) first.** Two traps in the sequence below: (1) the tool has no ref filter, so a test namespace of *this* repository is a complete
> copy (~330,000 Drive items), and a test copy plus the real mirror does not fit in a 500,000-item shared drive; (2) `workflow_dispatch` only works once
> `DRIVE_MIRROR_ENABLED=true`, which also enables the hourly schedule and the event relays, and they build whatever `DRIVE_MIRROR_NAME` says. The runbook
> therefore does the controlled test manually (small scratch repository, or a throw-away destination) and enables the workflow only afterwards, on the verified real namespace.

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

## Workflow review (2026-11-10)

`actionlint` is clean. Each item of the readiness review, with what was found:

| Area | Finding | Action |
|---|---|---|
| Least privilege | top-level `contents: read`; `relay` has only `actions: write`; `mirror` adds `pull-requests: read` and `id-token: write` (OIDC for Google; grants nothing on GitHub) | none needed |
| Credential isolation | credentials exist only in the `mirror` job, which runs only in the trusted branch's context; secrets come from the `drive-mirror` environment (restrict it to `main`) | none needed |
| Supply chain | third-party actions used mutable tags in a job that can see Drive credentials | **fixed**: all four pinned to commit SHAs (`checkout` v4.4.0, `setup-python` v5.6.0, `google-github-actions/auth` v2.1.13, `upload-artifact` v4.6.2) |
| Branch / PR events | `pull_request_target` runs the base branch's workflow and never checks out PR code; other-branch events only relay a dispatch | none needed |
| Concurrency | `drive-mirror-<repo>` group, `cancel-in-progress: false`: one writer, one pending run | none needed; plus the Drive lease for manual runs |
| Scheduled reconcile | a dedicated cron could be starved (a pending run is replaced by a newer pending run) | **fixed**: reconcile is decided from persisted `last_reconcile_at`; one hourly cron |
| Manual dispatch | modes `incremental`, `reconcile`, `verify-only`, `dry-run`; test namespace via `mirror_name`; `break_lock`, `allow_mass_retire` | none needed |
| Retry / failure handling | one retry of a generic failure; lease-held, blocked and safety stops are final; each Drive request retries 429/5xx/rate-limit 403 with backoff and recovers lost-response creates | **fixed** a latent resumable-upload bug (409 `fileIdInUse` on a retried final `PUT`) |
| Execution limits | hosted jobs are hard-limited to 360 min; job timeout 350, sync budget 270, leaving time for scan, verify and reports; an unfinished run exits 2 and resumes | budget lowered from 320 to 270 |
| Verification cost | a full verify of ~330k Drive items every hour is infeasible against real Drive latency | **fixed**: incremental runs verify bytes only for changed namespaces; reconcile verifies in full; parallel walks |
| API quota | Drive per-user rate limits are handled by backoff; GitHub API calls use the run token | token now also passed to the inventory step |
| Storage / item count | Drive items ≈ 330k with PR-only refs; a shared drive allows 500,000 | `preflight` checks free quota against `DRIVE_MIRROR_EXPECTED_BYTES`; growth is by branch count (see `MIRROR_COMPLETENESS.md`) |
| Secret exposure | scanning happens after nothing is uploaded and before the first byte | **added**: security gate with per-ref quarantine, fail closed |
| Public logs / artifacts | repository is public: logs and artifacts are public | ids masked and scrubbed from reports; preflight prints account *type* only; the gate prints rule/file/commit only |
| Silent staleness | GitHub disables scheduled workflows after 60 days of repository inactivity | cannot be fixed inside the workflow: documented; use an external freshness check |
| Enabled state | nothing runs while `DRIVE_MIRROR_ENABLED` is unset (both jobs skipped, checked on a real push) | **left disabled**; enabling needs the approved controlled live test |

## Capacity and quotas (measured 2026-11-10, emulator run on a frozen copy of the remote)

| | value |
|---|---|
| branches mirrored / quarantined / PR-only namespaces / tags | 134 / 2 / 50 / 0 |
| files in the mirror | 286,450 (logical 14.60 GB; 6,304 unique blobs) |
| bytes actually uploaded on the first sync | 437 MB (5,242 uploads); 281,208 server-side copies |
| Drive items incl. folders and metadata | **330,445** (66 % of a shared drive's 500,000-item limit) |
| initial sync | 330,814 write requests (emulator: 498 s). Google paces sustained writes per user; at a few writes per second this is on the order of a day, i.e. several budgeted runs, each resuming where the last stopped. **Not measured live** |
| steady state | a push touching N files costs ≈ N writes; a new branch ≈ one copy per file and almost no upload; an unchanged run costs 3 writes |
| full verification | 44,491 listing reads (emulator: 316 s); parallel walks; hourly runs verify only changed namespaces |

Each branch is a full copy because the requirement is per-branch path fidelity; Drive has no cross-folder hard links. Growth is roughly
*(files per branch) × (number of branches)*. Retiring stale branches on GitHub shrinks the live tree (retired folders stay in `_retired/`
until someone deletes them). A free personal account (15 GB, shared with Gmail and Photos) would be exhausted if server-side copies are charged at full size (assumed;
not verified against real Drive), so a Workspace shared drive is the safer destination.

## Limitations and risks

* **Not yet run against Google.** Real behaviour of quotas, `sha256Checksum` availability,
  the exact error code for a reused generated id (handled as 409 or 400/`fileIdInUse`), and
  `addParents`/`removeParents` on retried moves are unverified.
* A free personal account (15 GB) is likely too small for today's ~14.6 GB logical mirror; a shared drive is
  bounded at 500k items and the mirror already uses ~330k (66 %). Both limits are approached by growth in branch
  count, not by code.
* Two refs are quarantined by the security gate until owner decision D-6 (see Security gate and `security/D-6_APPROVAL_PROPOSAL.md`).
* **Gate/sync race: fixed** by the frozen-snapshot handoff (above; `security/GATE_SYNC_HANDOFF.md`). Residual limits: the scanner's own coverage (content that exists only in a
  merge commit, and binary files, were not flagged by gitleaks 8.30.1 in a scratch test), `--allow-ungated` for scratch data, and the approval being tamper-evident rather than signed.
* **No automated capacity warning.** `preflight` checks free bytes on a personal drive only. Nothing counts Drive items against a shared drive's 500,000-item limit; see
  `RUNBOOK.md` Part 11 for the manual procedure.
* Drive search is eventually consistent; `verify` compensates with direct GETs, and
  `reconcile` may need a second pass if it ran seconds after a large write.
* PR heads not contained in any branch (50 today) are mirrored by default under `pulls/`
  (`--pull-heads none` opts out; they then exist only in `refs.json`).
* Git history itself (commits other than ref heads) is **not** mirrored; `refs.json` and
  manifests record the head commits. See `MIRROR_COMPLETENESS.md` for a measured comparison and
  why a history archive is left as a separately approvable extension.
* GitHub disables scheduled workflows in a public repository after 60 days without repository
  activity, which would silently stop the mirror. Monitor freshness from outside the workflow
  (for example, alert when `_mirror/state.json`'s `last_complete_run.finished_at` is older than a day).
* `incremental` runs do not detect manual edits on Drive; the daily `reconcile` does.

## Tests

```sh
GITLEAKS_BIN=/path/to/gitleaks-8.30.1 \
PYTHONPATH=tools python3 -m unittest discover -s tools/drive_mirror/tests -v      # ~2.5 min, 39 tests (29 mirror/hardening + 10 workflow/dry-run)
# (without GITLEAKS_BIN the one real-scanner test is skipped)
PYTHONPATH=tools python3 tools/drive_mirror/tests/full_repo_emulator.py \
  --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --work-dir /tmp/mm.git --out-dir /tmp/mm-out
```

Results of the last run are in [`TEST_EVIDENCE.md`](TEST_EVIDENCE.md).

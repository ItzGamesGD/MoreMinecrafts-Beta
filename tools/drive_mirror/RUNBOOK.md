# Drive mirror: operator runbook

For the repository owner. Every command below is an existing CLI command or workflow operation (`python -m drive_mirror …`,
`.github/workflows/drive-mirror.yml`); nothing here was run against Google Drive. Design and layout: [`README.md`](README.md).
Scope and integrity claims: [`MIRROR_COMPLETENESS.md`](MIRROR_COMPLETENESS.md). Offline evidence: [`TEST_EVIDENCE.md`](TEST_EVIDENCE.md).

**Status: OFFLINE READY, LIVE GOOGLE DRIVE VERIFICATION PENDING.** Nothing in this runbook has been executed live. Do not treat the
emulator results as evidence of Drive behaviour (quotas, rate limits, checksum availability, error codes, search consistency).

**Manual vs automatic.** Parts 1–8 are *manual*: you run the CLI yourself, one step at a time, and nothing happens unless you run it.
Part 9 turns on *automatic* operation (events, an hourly schedule, relays). Do not start Part 9 before Parts 1–8 pass.

## 0. Configuration checklist (do this first)

Names below are the ones the code and workflow read. There are no others.

### Authentication (pick one)

| | A. Shared drive, keyless (recommended) | B. Personal Google account |
|---|---|---|
| `DRIVE_AUTH_MODE` | `adc` (the workflow default) | `oauth` |
| Credential | Workload Identity Federation → service account; no key file | OAuth client + long-lived refresh token (secret) |
| API scope | `https://www.googleapis.com/auth/drive` (the `adc` default; overridable with `DRIVE_SCOPE` locally only) | `https://www.googleapis.com/auth/drive.file` |
| Google API | Google Drive API enabled in the Cloud project | same |
| Permission on destination | service account is **Content manager** of the shared drive (it must create, update, copy, move and trash) | the account owns everything it creates |
| `DRIVE_PARENT_FOLDER_ID` | folder id inside the shared drive | `root` (with `drive.file` the app can only see folders it created) |
| `DRIVE_SHARED_DRIVE_ID` | the shared drive id | leave unset |
| Capacity | 500,000 items per shared drive | 15 GB free tier shared with Gmail/Photos: **too small** for ~14.6 GB (assumes server-side copies count at full size; unverified) |

Local manual runs also accept `DRIVE_AUTH_MODE=static` with `DRIVE_ACCESS_TOKEN` (a short-lived token you mint yourself), or leave
`DRIVE_AUTH_MODE` unset and the first of `DRIVE_ACCESS_TOKEN`, `DRIVE_OAUTH_REFRESH_TOKEN`, `GOOGLE_APPLICATION_CREDENTIALS` decides.

WIF provider attribute condition (README §2A): `assertion.repository == 'ItzGamesGD/MoreMinecrafts-Beta' && assertion.ref == 'refs/heads/main'`.
The `mirror` job declares `environment: drive-mirror`, so GitHub's OIDC `sub` claim has the `…:environment:drive-mirror` form, not `…:ref:refs/heads/main`.
Do not restrict the provider on a `ref:`-style `sub`.

### GitHub configuration (Settings → Environments → `drive-mirror`, deployment branches: `main` only)

| Kind | Name | Needed for | Notes |
|---|---|---|---|
| secret | `DRIVE_PARENT_FOLDER_ID` | always | folder id, or `root` (mode B). Masked in logs by the workflow |
| secret | `DRIVE_SHARED_DRIVE_ID` | mode A | masked in logs |
| secret | `DRIVE_OAUTH_CLIENT_ID`, `DRIVE_OAUTH_CLIENT_SECRET`, `DRIVE_OAUTH_REFRESH_TOKEN` | mode B | |
| variable | `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT` | mode A | |
| variable | `DRIVE_AUTH_MODE` | optional | default `adc`; set `oauth` for mode B |
| variable | `DRIVE_MIRROR_ENABLED` | **the master switch** | `true` activates every job. Unset or anything else: both `relay` and `mirror` are skipped |
| variable | `DRIVE_MIRROR_NAME` | optional | mirror root folder name; default = repository name (`MoreMinecrafts-Beta`) |
| variable | `DRIVE_MIRROR_PULL_HEADS` | optional | `orphaned` (default), `all`, `none` |
| variable | `DRIVE_MIRROR_TRUSTED_BRANCH` | optional | default `main` |
| variable | `DRIVE_MIRROR_TOOLING_REF` | optional | ref of the mirror tooling to check out; default = the run's own `github.sha` |
| variable | `DRIVE_MIRROR_MAX_AGE_HOURS` (26), `DRIVE_MIRROR_RECONCILE_AFTER_HOURS` (24), `DRIVE_MIRROR_EXPECTED_BYTES` (14000000000) | optional | defaults in parentheses |

Workflow permissions (already in the file; do not widen): top level `contents: read`; `relay` job `actions: write`; `mirror` job
`contents: read`, `pull-requests: read`, `id-token: write`. The workflow never pushes, comments or labels. The file must be on `main`
for the schedule and for `workflow_dispatch` to exist. Merging it is your decision; this branch does not merge anything.

### Capacity and quota expectations (emulator measurements, 2026-11-10)

| | Value | Basis |
|---|---|---|
| Files / logical size | 286,450 / 14.60 GB | frozen snapshot, 184 namespaces mirrored + 2 quarantined |
| Drive items (files + folders + metadata) | 330,445 (66 % of 500,000) | ≈ **1.15 items per mirrored file** |
| Bytes actually uploaded, first sync | 437 MB (5,242 uploads) + 281,208 server-side copies | |
| Write requests, first sync | ≈ 330,800 | at real Drive pacing this is several budgeted runs; **not measured live** |
| Unchanged run | 3 writes (lease, state) | |

Both limits grow with **branch count** (each branch is a full copy), not with repository size. A destination needs room for the whole
mirror **once**: see the trap in step 3.

## 1. Credential preflight

Read-only first, then with the write probe (which creates and trashes one 7-byte file in the parent folder):

```sh
export PYTHONPATH=tools
export DRIVE_AUTH_MODE=adc            # or oauth + DRIVE_OAUTH_CLIENT_ID/_SECRET/_REFRESH_TOKEN
export DRIVE_PARENT_FOLDER_ID=…       # folder id, or root
export DRIVE_SHARED_DRIVE_ID=…        # mode A only
python3 -m drive_mirror preflight --parent-folder-id "$DRIVE_PARENT_FOLDER_ID" --drive-id "${DRIVE_SHARED_DRIVE_ID:-}" \
  --expected-bytes 14000000000 --report /var/tmp/preflight.json
python3 -m drive_mirror preflight --parent-folder-id "$DRIVE_PARENT_FOLDER_ID" --drive-id "${DRIVE_SHARED_DRIVE_ID:-}" \
  --expected-bytes 14000000000 --write-probe --report /var/tmp/preflight-write.json
```

Exit `0` PASS · `4` BLOCKED (no credentials, destination not visible, access denied) · `1` FAIL (e.g. less free quota than 110 % of
`--expected-bytes` on a personal drive, or the probe was not stored byte-for-byte). Look for `authentication`, `shared_drive`,
`destination` and `write_probe` all `ok: true`, and `server_sha256_available: true` (the verifier compares server-computed SHA-256;
if Drive does not return it, stop and report). Preflight prints the account *type* only, never the address.
It does **not** count existing items, so it cannot warn about the 500,000-item limit.

## 2. Fetch, scan, gate (before any upload)

```sh
REPO=https://github.com/ItzGamesGD/MoreMinecrafts-Beta
W=/var/tmp/mm-source.git
python3 -m drive_mirror prepare --repo-url "$REPO" --work-dir "$W" --pull-heads all
( cd "$W" && gitleaks git --no-banner --no-color --redact=100 --log-opts='--all --full-history' \
    --report-format json --report-path /var/tmp/gitleaks.json . )          # exit 1 = findings, expected
python3 -m drive_mirror secgate --repo-url "$REPO" --work-dir "$W" --pull-heads orphaned \
  --scan-report /var/tmp/gitleaks.json --quarantine-out /var/tmp/quarantine.json --report /var/tmp/secgate.json
```

Use gitleaks **8.30.1** verified against SHA-256 `551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb` (the value pinned in
`drive-mirror.yml`; it matches the release's `checksums.txt`). `secgate` exit `0` = PASS, `6` = unapproved findings: the refs that would
upload the flagged content are quarantined (today `probe/st05-p0-probe-01` and `audit/st05-po25-review-package-20261102`), `7` = could not decide
(**stop**). Pass `--quarantine-file /var/tmp/quarantine.json` to every `sync`, `verify` and `status` below.

**Quarantine and completeness.** A quarantined ref is never uploaded, is listed in the report, `state.json` and the job summary, and the
mirror is *knowingly incomplete* by exactly those refs. `verify` fails with `namespace_missing` for a quarantined ref unless it is given the same
quarantine file, so a quarantined mirror cannot be mistaken for a complete one. If flagged content reached Drive earlier, `verify` reports
`quarantined_content_on_drive`. Lifting a quarantine needs your decision D-6 (see `security/D-6_APPROVAL_PROPOSAL.md`); the tooling never edits the allowlist.

**Race window.** The gate examines the refs as they were when `prepare` ran. A ref created or advanced between the scan and `sync`'s own
snapshot is not covered by it (README *Limitations*). Re-run steps 2→3 back to back, and do not start `sync` from a stale `quarantine.json`.

## 3. Dry run, then a controlled test namespace

`sync --dry-run` reads Drive and Git, plans, and **writes nothing** (a namespace that does not exist yet is simply planned). Use `--quarantine-file` as above.

```sh
COMMON=(--repo-url "$REPO" --work-dir "$W" --parent-folder-id "$DRIVE_PARENT_FOLDER_ID" --drive-id "${DRIVE_SHARED_DRIVE_ID:-}"
        --quarantine-file /var/tmp/quarantine.json)
python3 -m drive_mirror sync "${COMMON[@]}" --mirror-name MMB-e2e-test --dry-run --github-repo ItzGamesGD/MoreMinecrafts-Beta --report /var/tmp/dry.json
```

Do **not** run `verify` after a dry run on a namespace that does not exist: it fails with `mirror_root` by design (the workflow now skips it).

> **Capacity trap.** The tool has no ref filter, so a test namespace of *this* repository is a complete 286,450-file / ~330,000-item copy.
> On a 500,000-item shared drive, a test copy **plus** the real mirror does not fit (and trashed items are, to the best of my knowledge, still
> counted until deleted permanently: verify this in Google's documentation before relying on it). Choose one:
>
> * **3a (recommended): a small scratch repository on the real Drive.** Exercises auth, quotas, checksums, rename/delete/force-push, retire and recovery at negligible size.
> * **3b: the real repository into a throw-away destination**: a separate shared drive (or folder) you delete entirely afterwards. Only then is the real root's capacity unaffected.
> * **3c: skip the full-size test and run the real mirror directly (Part 7)**, using the verification steps as the test. Reasonable because the tool only *reads* from GitHub, and a failed or odd first run is recoverable by `--mode reconcile`.

3a, concretely (local bare repo as the source; any `--repo-url` is accepted):

```sh
S=/var/tmp/mm-scratch; rm -rf "$S"; git init -q --bare -b main "$S/remote.git"; git init -q -b main "$S/work"
( cd "$S/work" && git config user.name t && git config user.email t@example.invalid \
  && mkdir src && printf 'a\n' > src/a.txt && head -c 6000000 /dev/urandom > big.bin && printf 'x' > run.sh && chmod +x run.sh \
  && git add -A && git commit -qm one && git remote add origin "$S/remote.git" && git push -q origin main \
  && git branch feature/x && git push -q origin feature/x )
python3 -m drive_mirror sync --repo-url "$S/remote.git" --work-dir /var/tmp/mm-scratch-cache.git --parent-folder-id "$DRIVE_PARENT_FOLDER_ID" \
  --drive-id "${DRIVE_SHARED_DRIVE_ID:-}" --mirror-name MMB-scratch --report /var/tmp/scratch-sync1.json
```

`big.bin` (> 5 MB) exercises the resumable-upload path against the real API. Then, on the scratch repo only, in this order, re-running `sync` then `verify` after each:
add a commit; rename a file; delete `feature/x`; force-push `main` (`git push -f`); each time expect `verify` PASS. These are the live counterparts of the
emulator scenarios in `TEST_EVIDENCE.md`; they are the first time any of them meets real Drive.

## 4. Checksum verification

```sh
python3 -m drive_mirror verify "${COMMON[@]}" --mirror-name MMB-e2e-test --deep-sample 200 --report /var/tmp/verify.json   # real run: see Part 8
```

Independent of `sync`: re-reads the remote, hashes blobs itself, walks Drive and compares sizes and Drive's server-computed SHA-256; `--deep-sample N`
downloads N files and re-hashes them. Exit `0` = PASS, `1` = FAIL (see `global.problems` and `failing_namespaces`). For 3a, drop `--quarantine-file`
and use the scratch `--repo-url`/`--work-dir`.

## 5–6. Repeat synchronization and idempotency

```sh
python3 -m drive_mirror sync "${COMMON[@]}" --mirror-name MMB-e2e-test --github-repo ItzGamesGD/MoreMinecrafts-Beta --report /var/tmp/sync2.json
```

The printed summary must show `"status": "complete"`, `"changed": 0`, `"errors": 0`, `"retired": 0`, and `drive_stats` with only the handful of writes for the lease and
`state.json` (the emulator showed 1 to 3). Then force a full crawl and verify again:

```sh
python3 -m drive_mirror sync "${COMMON[@]}" --mirror-name MMB-e2e-test --mode reconcile --github-repo ItzGamesGD/MoreMinecrafts-Beta --report /var/tmp/sync3.json
python3 -m drive_mirror verify "${COMMON[@]}" --mirror-name MMB-e2e-test --deep-sample 200 --report /var/tmp/verify2.json
```

**Idempotency criterion.** Check each sync report with:

```sh
python3 -c "import json,sys;r=json.load(open(sys.argv[1]));k=('uploads','copies','updates','moves','meta_updates','trashed','folders_created','folders_trashed');print(r['mode'],r['status'],'retired',len(r['retired']),'errors',len(r['errors']),'content-writes',sum(n.get(x,0) or 0 for n in r['namespaces'] for x in k),'actions',sorted({n['action'] for n in r['namespaces']}))" /var/tmp/sync2.json
```

* Repeat `incremental` with no source change: `content-writes 0`, every action `noop`.
* `reconcile` with no source or Drive change: `content-writes 0` and every action `updated` (a reconcile crawls each namespace and rewrites its manifest, so `changed` is *not* 0 there; the
  CLI's `changed` counts non-`noop` actions). In the emulator whole-repository run this left every namespace's Drive items unchanged.

Any non-zero content write with no source change is a defect or someone edited Drive; investigate before continuing.

## 7. Full manual synchronization

Use the real mirror name (default `MoreMinecrafts-Beta`; the name must be a single path segment). Time-box each invocation; exit `2` means "stopped at the budget, resume":

```sh
python3 -m drive_mirror sync "${COMMON[@]}" --mirror-name MoreMinecrafts-Beta --github-repo ItzGamesGD/MoreMinecrafts-Beta \
  --time-budget-min 270 --workers 4 --report /var/tmp/full-sync.json
echo "exit=$?"
```

Repeat the same command while the exit code is `2`; each run resumes (interrupted namespaces stay `in_progress` and are crawled and converged, creates are idempotent after a lost response).
Exit codes: `0` complete · `1` failed · `2` incomplete · `3` lease held (another writer; wait, or see Part 10) · `4` BLOCKED (credentials/destination) · `5` safety stop (mass retirement > max(5, 20 %), empty tree, ambiguous root: read `reason`, never override blindly) · `6`/`7` gate.

**If the source changes during a run.** Every run first fetches all refs, runs `git ls-remote`, and refetches (up to 4 times) until both agree, so each namespace is synced from a value
the remote really advertised; if the remote will not settle, the run fails (`remote refs kept changing during fetch`) and changes nothing. After that, pushes are **not** seen by the running sync:
a branch updated mid-run is picked up by the next run (hourly in automatic mode, your next command in manual mode); a branch created mid-run is simply absent until then. A `verify` run against a
remote that moved after the sync reports the new or advanced refs as `namespace_missing` / `manifest_stale_commit`: that is the verifier working. Re-sync, then verify again; use a **frozen**
source (a local bare clone passed as `--repo-url`) when you need a deterministic result. A deletion is only acted on after it is confirmed twice (full `ls-remote` plus a targeted probe); a failed
probe means "inaccessible", the namespace is kept and reported as `ref_unconfirmed`.

## 8. Final completeness verification

```sh
python3 -m drive_mirror verify "${COMMON[@]}" --mirror-name MoreMinecrafts-Beta --deep-sample 500 --report /var/tmp/final-verify.json
python3 -m drive_mirror status "${COMMON[@]}" --mirror-name MoreMinecrafts-Beta --max-age-hours 26 --report /var/tmp/final-status.json
```

Accept only if: `verify` exit `0` (no `--sync-report`, so it is the **full** verification); `global.files_verified == files_expected`; `expected_namespaces` equals
`found_namespaces` plus the quarantined refs; `deep_downloads` all matched; `status` PASS (`behind`, `missing`, `in_progress` empty). Record the quarantined refs and say so in the acceptance note:
the mirror is complete **except** for them until D-6 is decided. Then re-run Part 5–6 once against the real root to show the steady state is idempotent.

## 9. Enabling GitHub Actions (only after Parts 1–8 pass and you approve)

Order matters; an enabled workflow starts running on the next event or at minute 23 of the next hour.

1. Merge the workflow to `main` (owner decision; this branch does not).
2. Create environment `drive-mirror`, deployment branches `main` only; add the secrets/variables from Part 0. Leave `DRIVE_MIRROR_ENABLED` **unset**.
3. Leave `DRIVE_MIRROR_NAME` **unset** so automatic runs continue the namespace you already built and verified in Parts 7–8. Do **not** set it to a test name and then enable the workflow:
   the hourly schedule and every push would then build a second full-size namespace under that name.
4. Set `DRIVE_MIRROR_ENABLED=true`. Without it, `workflow_dispatch` only runs `not-configured`, which fails with BLOCKED by design, so a test dispatch cannot happen before this.
5. Actions → *Drive mirror* → *Run workflow*: `mode=dry-run` (writes nothing; preflight with write probe still runs), then `mode=verify-only` (must be PASS), then `mode=incremental` (expect `changed: 0` if nothing moved).
6. Read the job summary: preflight, gate verdict and quarantined refs, sync counts, verify, freshness.

**Automatic behaviour once enabled.** Events (`push`, `create`, `delete`, `pull_request_target`) on a branch other than the trusted branch run only the credential-free `relay` job, which asks GitHub to dispatch a sync on `main`.
Events in `main`'s context, the hourly schedule and manual dispatches run the `mirror` job, one at a time (`concurrency: drive-mirror-<repo>`, `cancel-in-progress: false`; a pending run is replaced by a newer pending run, which is safe because every run syncs everything).
Each run: preflight → fetch → pinned gitleaks → gate → sync → verify (scoped to changed namespaces after an incremental sync, full after a reconcile) → freshness check → summary + 30-day report artifact. A run upgrades itself to a reconcile when the last one is older than `DRIVE_MIRROR_RECONCILE_AFTER_HOURS`.
GitHub disables scheduled workflows after 60 days without repository activity: alert **outside** the workflow when `_mirror/state.json`'s `last_complete_run.finished_at` is older than a day.

## 10. Disable, and recover from failure

**Disable now:** set `DRIVE_MIRROR_ENABLED` to anything but `true` (or delete it), or disable the workflow in Actions. Both jobs are then skipped. A run already in progress finishes or is cancelled in the Actions UI; the lease expires after 20 minutes.

| Symptom | Meaning | Action |
|---|---|---|
| exit 2 / "INCOMPLETE" | time budget reached (expected during the first sync) | run again; it resumes |
| exit 3 / LOCKED | another writer holds `_mirror/lock.json` | check Actions and other machines for a running sync. Only if none exists (a killed run), wait out the 20-minute TTL or add `--break-lock` (workflow input `break_lock`) |
| exit 4 / BLOCKED | no credentials, destination missing, or 401/403/404 | `preflight`; check WIF condition, secrets, service-account membership, the folder id |
| exit 5 / SAFETY_STOP | mass retirement, empty tree or ambiguous root | read `reason`; if the retirement is intended: `--allow-mass-retire` (workflow input `allow_mass_retire`) |
| exit 6 | unapproved finding: refs quarantined | not a failure; see Part 2 and D-6 |
| exit 7 / job fails before sync | scanner or gate could not decide | fix the scanner/report; never bypass (fail closed) |
| `verify` FAIL: drift, `file_missing`, `extra_file`, converted copies | Drive was edited or an item lost | `sync --mode reconcile` (trashes extras/duplicates/converted copies, restores bytes), then full `verify` |
| a `refs/pull/N/head` namespace reported `ref_unconfirmed` | PR head vanished from the remote; GitHub does not delete PRs, so it is treated as an anomaly | investigate on GitHub; the namespace is kept until the API confirms |
| accidental deletion in Drive | the tool only trashes (30-day recovery per Drive's trash policy) and moves retired namespaces to `_retired/` | restore from trash/`_retired/`, then `reconcile` |
| wrong or abandoned namespace | | delete the root folder manually in Drive (and empty the trash if capacity matters) |

## 11. Monitoring capacity and quota

* **Item count (the binding limit on a shared drive).** There is no automated item-count warning in the tool or workflow. Estimate ≈ 1.15 × `files_verified` from the job summary (`Verify: … files N/N`).
  Suggested manual thresholds (derived from the 1.153 ratio, not measured live): review at ≈ 347,000 files (≈ 400,000 items); the hard limit is ≈ 433,000 files. Today: 286,450 files.
  Also read the drive's own item/storage figures in the Google Admin console or the shared drive's details.
* **Growth lever:** the number of branches. Retire stale branches on GitHub; their namespaces move to `_retired/` and still hold items until you delete them from Drive.
* **Bytes.** `preflight` reports free quota on personal drives (`my_drive_quota`); on a shared drive it is not checked.
* **API quota / rate limits.** The client retries 429 and rate-limit 403 responses with backoff, but real limits were never measured; watch the first full sync's duration and any `errors` in the report.
* **Freshness.** `status` (run after every sync by the workflow) fails when no complete run within `DRIVE_MIRROR_MAX_AGE_HOURS` (26) or namespaces stay unconverged; a red run is the signal.
* **Reports are public** on this public repository (ids are scrubbed; no tokens, e-mail addresses or matched values are written). Do not add secrets or private folder names to variables that are printed.

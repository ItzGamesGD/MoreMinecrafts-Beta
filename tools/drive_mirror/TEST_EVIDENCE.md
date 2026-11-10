# Drive mirror — test evidence (2026-11-10)

## Verdicts

| Area | Verdict | Basis |
|---|---|---|
| Live synchronisation to Google Drive | **BLOCKED** | No Google credentials, connector or destination in the build environment. `python3 -m drive_mirror preflight --parent-folder-id root` → `{"result": "BLOCKED", "reason": "no Drive credentials configured"}`, exit 4. Unauthenticated `GET https://www.googleapis.com/drive/v3/about` → HTTP 401 (API reachable, no identity). |
| Live verification against Google | **NOT RUN** | Depends on the above. |
| Engine, verifier and client against the Drive API emulator | **PASS** | Sections below. The emulator (`tests/fake_drive.py`) is *not* evidence about Google's service: real quotas, latency, rate limits and error codes are not modelled. |
| GitHub Actions workflow | Lint **PASS** (`actionlint`), execution **NOT RUN** | It can only run after it is on `main` and configured (owner decisions). |

## 1. Scenario suite — `tests/test_mirror.py`

Real `git` remote and clone, the real CLI started as subprocesses, the emulator over HTTP.
`PYTHONPATH=tools python3 -W error::ResourceWarning -m unittest discover -s tools/drive_mirror/tests -v`
→ **Ran 13 tests in 114.5 s — OK.**

| Requirement | Test | What is asserted |
|---|---|---|
| Every tracked file present, bytes and checksums match | `test_full_lifecycle` | text, random binary, gzip NBT, empty file, CRLF file, executable, symlink, unicode path with `#`, `%`, `'` and spaces, 7-level nesting, 6 MB file (resumable upload); `verify --deep-sample` re-downloads every file and recomputes its git blob id |
| Branch namespaces, tags | `test_full_lifecycle` | 2 branches + lightweight and annotated tag → 4 namespaces; `feature/sky-tower` → folder `feature%2Fsky-tower` |
| Add / modify / delete / rename / rename+modify / mode change / directory delete | `test_full_lifecycle` | applied from the manifest; the renamed file keeps its Drive file id; emptied folders removed |
| Branch creation | `test_full_lifecycle` | new branch built with server-side copies, **0 uploads** |
| Branch deletion | `test_full_lifecycle` | namespace moved to `_retired/` (not trashed); verify PASS |
| Force-push to unrelated history | `test_full_lifecycle` | `forced_update: true`; only the 2 files of the new tree remain |
| Re-created branch | `test_full_lifecycle` | fresh namespace incarnation, verify PASS |
| Repeated sync produces no changes | `test_full_lifecycle` | byte-for-byte identical Drive snapshot of all namespaces after the second run |
| Metadata references the right commit | `test_metadata_and_manifest` | `state.json`, `refs.json` (HEAD symref, annotated-tag peel), manifest commit sha, per-file blob ids |
| Interrupted sync recovers | `test_interrupted_sync_recovers` | sync `SIGKILL`ed after 150 writes; verify detects `state_incomplete`; next run crawls, completes; verify PASS with 411/411 files and no duplicates |
| Lost responses / retries | `test_transient_errors_and_rate_limits_are_retried` | injected 503 every 7th request, rate-limit 403 every 11th, and "write applied but response lost" every 5th write; retries > 10 and conflict recoveries > 0 (no duplicate files); verify PASS |
| Drift detection and repair | `test_drift_detected_and_reconciled` | tampered bytes, a missing file, an unmanaged extra file and a Google-Docs-type duplicate are all reported by verify; `--mode reconcile` repairs; deep verify PASS |
| Search-index lag | `test_listing_lag_is_tolerated_by_verify` | files written by the last sync hidden from listings; verify confirms them by direct GET and passes |
| Single writer | `test_lease_blocks_concurrent_writer` | a live lease held by another owner → exit 3 `LOCKED` |
| No destructive deletion before verification | `test_mass_retire_guard_and_dry_run` | dry run writes nothing; 8 of 12 namespaces disappearing → exit 5 `SAFETY_STOP`; allowed only with `--allow-mass-retire` |
| Resumable time budget | `test_time_budget_produces_resumable_incomplete_run` | exit 2 `incomplete`, next run completes, verify PASS |
| PR heads not in any branch | `test_pull_heads_orphaned` | `refs/pull/7/head` (orphaned) mirrored; `refs/pull/8/head` (contained in `main`) not |
| Credentials missing / wrong | `test_blocked_without_credentials` | exit 4 `BLOCKED` in both cases (never a pass) |
| Logs leak nothing | `test_preflight` | preflight PASS with write probe; output contains neither the account e-mail nor the folder id |

## 2. Whole real repository → emulator — `tests/full_repo_emulator.py`

Source: `https://github.com/ItzGamesGD/MoreMinecrafts-Beta`, fetched live on 2026-11-10 (~03:00–03:30 UTC).

| Step | Exit | Time | HTTP writes / reads | Result |
|---|---|---|---|---|
| 1. initial sync | 0 | 288 s | 247,354 / 461 | 126 / 126 namespaces `complete`; 4,805 uploads (423 MB) + 209,559 server-side copies; 32,471 folders |
| 2. verify `--deep-sample 500` | 0 | 78 s | 0 / 33,231 | **PASS** — 126 / 126 namespaces; **214,364 / 214,364** files verified by size + server SHA-256 (11,932,739,223 bytes); 500 / 500 downloads match their git blob id; no problems |
| 3. sync again (no source change) | 0 | 1.9 s | 3 / 8 | 126 no-ops; **0** namespace items changed (the 3 writes are lease acquire, `state.json`, lease release) |
| 4. sync `--mode reconcile` | 0 | 152 s | 381 / 32,774 | full crawl of every namespace; **0** namespace items changed (writes are manifests and state) |
| 5. verify after reconcile | 0 | 76 s | 0 / 32,731 | **PASS** — 214,364 files |

Emulator totals after the run: 247,099 Drive items (214,364 files + 32,471 folders + metadata).

## 3. Not covered here

* Behaviour of Google's real service (see README → Limitations and risks).
* The workflow itself on GitHub runners (needs `main` + configuration).
* Throughput: 247k writes took 288 s against a local emulator; Google enforces per-user write
  pacing, so the first live sync is expected to need several time-budgeted runs.

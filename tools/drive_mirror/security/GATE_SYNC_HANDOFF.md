# Gate/sync handoff: the frozen source snapshot

Applies to `tools/drive_mirror` from the commit that introduced `snapshot.py`. Emulator-verified only; **live Google Drive: NOT TESTED.**

## The invariant

> Every uploaded Git blob belongs to a commit of one immutable source snapshot that the security gate examined, and the uploader
> never expands that snapshot by resolving a moving ref again.

"Examined" is defined precisely below, and so are its limits (see *Remaining limitations*).

## What the gate operates on (verified, not assumed)

| Stage | Before | Now |
|---|---|---|
| Ref discovery | `prepare` fetched the remote | `prepare` fetches **once** and freezes `ref -> commit` (heads, tags + peeled commits, every `refs/pull/N/head`) plus each commit's **tree id**, and writes `source-snapshot.json` (`drive-mirror-source-snapshot/1`, self-digested) |
| Scan | `gitleaks --all` over *whatever refs the cache held when it ran* | `secgate --run-scanner gitleaks` runs gitleaks over **exactly the snapshot's commit ids** (`--log-opts=--full-history <sha> <sha> …`); never `--all` |
| Gate decision | `secgate` fetched again and decided over a *newer* ref set | `secgate` never fetches; it first requires the cache's `refs/mirror/*` and every commit/tree to equal the snapshot, and evaluates **every ref of the snapshot** (a flagged finding quarantines every ref, in any namespace, whose tip tree holds the flagged blob or the flagged line at the same path) |
| Output | `quarantine.json` listing only quarantined refs | the **approval** (`drive-mirror-quarantine/2`): verdict, quarantined refs, `gated_refs` (ref -> commit), scan record (binding, scanner version, commit-set digest), the embedded snapshot, `snapshot_digest`, `approval_digest` |
| Upload | `sync` fetched a **fourth** time and mirrored whatever tips it found | `sync` takes its ref map from the approval's snapshot and never fetches; file content comes from `git ls-tree`/blobs of those commits in the local object store |

**The scan is isolated from its surroundings.** gitleaks reads `./.gitleaksignore` from its working directory by default, honours `gitleaks:allow` comments inside scanned content and reads
`GITLEAKS_CONFIG*`. That would let the caller's directory or the content itself suppress findings outside `approved_findings.json` (observed on the real repository: a `.gitleaksignore` in the
caller's directory turned 12 findings into 10). `secgate --run-scanner` therefore runs the scanner from an empty directory that it also names as the ignore path, passes `--ignore-gitleaks-allow`,
scrubs `GITLEAKS_*` from the environment and uses absolute paths. Tests fail without this.

**Granularity.** The gate's findings are `(commit, file, line)` from a history scan; its *decision* is per ref and uses the tip tree's blob
ids (and the flagged line at the same path). Binding is by commit id (Git's object hash covers trees and blobs), and the tree id of every
commit is recorded and re-checked against the local store. It is **not** by ref name: two refs with the same name but different commits never
match.

## How `sync` consumes the approval (all checks happen before any Drive write unless noted)

1. `--quarantine-file` must be a v2 approval; otherwise `sync` exits **7** (GATE ERROR). A legacy v1 file, a missing, truncated or malformed
   file, an altered snapshot or quarantine list (digest mismatch), or a file whose SHA-256 differs from `--approval-sha256` all exit 7.
2. The approval's scan must be bound (`frozen-commits`, i.e. produced by `secgate --run-scanner`); an externally produced report needs the explicit
   `--allow-external-scan-report` and is recorded as `external-report`.
3. The snapshot's `repo_url` must equal `--repo-url`; every commit must exist in the cache with the recorded tree; `refs/mirror/*` must equal the
   snapshot (so `--contains` queries and the scan describe this state). If anything fetched after the gate, exit 7 with "run prepare and secgate again".
4. After reading Drive's `state.json` (read only; at most the empty mirror root is created on a brand-new drive): the snapshot must **not be older**
   than the last one applied (`state.approved_snapshot.created_at`). The *same* snapshot resumes freely; a newer one replaces it; an older one would
   roll branches back to commits the remote has moved past, so it exits 7.
5. Every ref `sync` would mirror must be in `gated_refs` with the same commit, and every namespace re-asserts its commit was examined
   (`GateError` is never downgraded to "one failed namespace"). Quarantined refs are skipped exactly as before.
6. `--allow-ungated` (scratch data only; the workflow never passes it) restores the old behaviour and stamps the report `gate: UNGATED`.

`verify --quarantine-file <approval>` checks Drive against the same frozen snapshot and never fetches; `status` stays a live freshness check.

## Concurrent changes on GitHub

| Event after the gate | Result |
|---|---|
| a branch advances / is force-pushed | the approved commit is mirrored; the new tip is picked up (and gated) by the next run |
| a branch or PR head is created | absent from this run; gated and mirrored by the next run |
| a ref (branch or PR head) is deleted | still mirrored this run from the approved snapshot (its objects are local); **retired by a later run only after two confirmations** (full `ls-remote` lacks it, targeted query succeeds and finds nothing). If the confirmation queries fail the namespace is kept: **inaccessible is never treated as deleted** |
| a quarantined commit appears under another name (alias branch, PR head) | not in the snapshot: not uploaded; the next gate quarantines the alias too (same tree) |
| the remote becomes unreachable | no effect on an approved run's *content*; only the deletion confirmation needs the remote |
| the source commit was garbage-collected on GitHub | irrelevant to a run that holds the object locally; a *new* `prepare` simply no longer lists the ref |

Because content is read only from the local cache that `prepare` filled, an approved run is deterministic and can be resumed for as long as the operator
keeps the cache untouched. Staleness is bounded by the operator re-running `prepare` + `secgate`, not by the tool.

## Retry, resume and reconcile

* The workflow's one retry re-runs `sync` with the same approval file: no new snapshot, no new fetch.
* A run interrupted at its time budget (exit 2) is resumed with the **same approval**; the persisted namespace state converges to the approved commits.
* `--mode reconcile` crawls Drive and restores bytes **from the approved snapshot's commits**, so it can neither import post-gate content nor un-quarantine a ref.
* A different snapshot is only accepted if it is newer (rule 4); a newer one must come from a new `prepare` and a new gate.

## Integrity of the handoff artifact

The approval is self-digested (`approval_digest`, `snapshot_digest`) and the workflow pins its SHA-256 in a step output (`steps.gate.outputs.approval_sha256`)
that a later step cannot change; `sync`, `verify` and `status` refuse a file that does not match. The files live in the job's temp directory and are published
as an artifact only at the end. A digest is *tamper evidence*, not a signature: a step that could rewrite both the file and its own step output is already
inside the trust boundary of this job (all steps are pinned and reviewed workflow steps).

## Recognising a gate/sync mismatch

* `sync` exit **7** and `"result": "GATE_ERROR"` in `sync.json`; the workflow prints `Approved snapshot missing, invalid or no longer matching the local cache (GATE MISMATCH)`.
  The job summary shows `Sync: GATE_ERROR`. Nothing was uploaded.
* Messages: `needs an approved source snapshot`, `digest mismatch`, `checksum`, `no longer matches the approved snapshot`, `older than the one this mirror last applied`,
  `not covered by the approved snapshot`, `was not examined by the security gate`.
* A report with `"gate": {"mode": "UNGATED"}` (or the summary's UNGATED warning) means the engine ran without a gate: treat as an incident in production.

**Recover:** run `prepare --snapshot-out`, `secgate --source-snapshot … --run-scanner …`, then `sync` with the new approval and its SHA-256. Never edit the approval, the
snapshot, or `refs/mirror/*` by hand, and do not run an un-approved `verify` (it fetches and moves the cache) between a gate and its sync.

## Remaining limitations (security)

1. **Scanner coverage is the scanner's.** Measured with gitleaks 8.30.1 on a scratch repository: content that exists *only in a merge commit* (an "evil merge")
   and secrets inside binary files (NUL bytes) were **not** flagged by the history scan; a >6 MB text file was. This is unchanged from the previous behaviour and is
   not caused by the handoff. `--log-opts=-m` would cover merges, but it re-reports already-approved content under merge-commit fingerprints (new, unreviewed
   findings, mass quarantine), so it is not applied here. The gate therefore guarantees "scanned by the pinned scanner as configured", not "contains no secret".
2. **Scan binding in the workflow is structural.** `--run-scanner` runs the scanner on exactly the snapshot's commits; a report produced elsewhere cannot be proven to be about
   the snapshot and is refused unless `--allow-external-scan-report` is given.
3. **Content already on Drive.** A ref that becomes quarantined after it was mirrored keeps its Drive copy; `verify` reports `quarantined_content_on_drive`. Removal is manual.
4. **Metadata is outside the invariant.** `refs.json`, `pulls.json` (GitHub API) and `state.json` carry ref names, shas and PR titles, not file content; `pulls.json` is read live.
5. **Digest, not signature.** See *Integrity*; an attacker who controls workflow steps controls the job anyway.
6. **A stale cache is operator-controlled.** An approval can be resumed as long as the cache is untouched; the tool cannot know that the remote has moved on, only that a *newer* approval exists.
7. **`--allow-ungated` exists** for scratch data and tests; it is never used by the workflow (a test enforces that), and every use is stamped in the report.
8. **Not live-tested.** Drive behaviour (retries on real 409/5xx, consistency) is unverified; this change does not touch the Drive client.

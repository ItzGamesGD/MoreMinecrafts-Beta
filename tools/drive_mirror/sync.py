"""Synchronisation engine: Git refs -> Drive namespaces.

Correctness model
-----------------
A namespace folder must equal `git ls-tree -r <commit>` for its ref: same set
of paths, same bytes (SHA-256 checked against Drive's server-side checksum),
nothing else. The engine reaches that state by diffing the desired tree against
what is on Drive — never against Git history — so force-pushes, rewrites and
interrupted runs converge exactly like ordinary updates.

"What is on Drive" comes from the namespace manifest when the previous sync of
that namespace completed, and from a full crawl of the folder otherwise
(state `in_progress`, missing/corrupt manifest, `--mode reconcile`). A crash
therefore never needs manual repair: the next run crawls and converges.

Safety rules
------------
* Deletions are Drive trash operations (recoverable for 30 days), never hard deletes.
* Before trashing anything in a namespace the folder's identity is re-read
  (mirror token, namespace token, container parent, not trashed).
* An empty desired tree never trashes files without --allow-empty-tree.
* Retiring namespaces of deleted refs requires a second ls-remote that still
  lacks the ref, and is capped by --max-retire-fraction unless overridden.
  Retired namespaces are moved to `_retired/`, not trashed.
"""
from __future__ import annotations

import concurrent.futures as cf
import datetime as dt
import hashlib
import json
import mimetypes
import os
import sys
import threading
import time
from dataclasses import dataclass, field

from . import SCHEMA_VERSION
from .drive import DriveClient
from .gitsrc import GitSource, RefSnapshot, TreeEntry
from .layout import (CONTAINERS, FOLDER_MIME, GOOGLE_APPS_PREFIX, LOCK_FILE, MANIFESTS_FOLDER, META_FOLDER,
                     P_BLOB, P_MIRROR, P_MODE, P_NS, P_REF, P_ROLE, P_SCHEMA, P_SHA256, PULLS_FILE,
                     REFS_FILE, RETIRED_FOLDER, STATE_FILE, fit_props, manifest_name, q_str, ref_namespace)
from .lock import DriveLease

FILE_MODES = {"100644", "100755", "120000"}
COPY_LOOKUP_MIN_BYTES = 256 * 1024


def utcnow() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def log(msg: str) -> None:
    print(f"[drive-mirror] {msg}", file=sys.stderr, flush=True)


class BudgetExceeded(RuntimeError):
    pass


class SafetyStop(RuntimeError):
    pass


@dataclass
class SyncConfig:
    mirror_name: str
    parent_folder_id: str
    repo_url: str
    mode: str = "incremental"            # incremental | reconcile
    include_tags: bool = True
    pull_heads: str = "orphaned"         # none | orphaned | all   (orphaned = PR heads no branch contains)
    dry_run: bool = False
    time_budget_s: float | None = None
    workers: int = 4
    max_retire_fraction: float = 0.2
    allow_mass_retire: bool = False
    allow_empty_tree: bool = False
    break_lock: bool = False
    lock_ttl_s: int = 1200
    lock_settle_s: float = 2.0
    run_id: str = field(default_factory=lambda: os.environ.get("GITHUB_RUN_ID", "local-" + os.urandom(4).hex()))
    github_repo: str | None = None
    quarantine: dict = field(default_factory=dict)   # ref -> reason; those refs are never uploaded
    reconcile_after_hours: float | None = None       # incremental runs upgrade themselves when the last reconcile is older
    crawl_workers: int = 8


def guess_mime(path: str) -> str:
    mime = mimetypes.guess_type(path, strict=False)[0]
    if not mime or mime.startswith(GOOGLE_APPS_PREFIX):
        return "application/octet-stream"
    return mime


def _age_hours(ts: str | None) -> float | None:
    if not ts:
        return None
    t = dt.datetime.fromisoformat(ts.replace("Z", "+00:00"))
    return (dt.datetime.now(dt.timezone.utc) - t).total_seconds() / 3600


def digest(obj) -> str:
    return hashlib.sha256(json.dumps(obj, sort_keys=True, ensure_ascii=False).encode()).hexdigest()


class Mirror:
    """Handle on one mirror root folder on Drive."""

    def __init__(self, drive: DriveClient, cfg: SyncConfig):
        self.drive = drive
        self.cfg = cfg
        self.root_id = None
        self.token = None
        self.folders: dict[str, str] = {}      # container / meta folder name -> id
        self.meta_files: dict[str, str] = {}   # file name in _mirror -> id
        self.manifest_files: dict[str, str] = {}

    # ------------------------------------------------------------------ bootstrap
    def open(self, create: bool) -> bool:
        if "/" in self.cfg.mirror_name or not self.cfg.mirror_name.strip():
            raise ValueError("mirror name must be a single non-empty folder name")
        roots = [f for f in self.drive.children(
            self.cfg.parent_folder_id, f"appProperties has {{ key='{P_ROLE}' and value='root' }}")
            if f["name"] == self.cfg.mirror_name and f["mimeType"] == FOLDER_MIME]
        if len(roots) > 1:
            raise SafetyStop(f"{len(roots)} mirror roots named {self.cfg.mirror_name!r}; refusing to guess")
        if not roots:
            if not create:
                return False
            self.token = os.urandom(8).hex()
            root = self.drive.create_folder(self.cfg.mirror_name, self.cfg.parent_folder_id,
                                            {P_ROLE: "root", P_MIRROR: self.token, P_SCHEMA: str(SCHEMA_VERSION)})
        else:
            root = roots[0]
            self.token = root["appProperties"][P_MIRROR]
        self.root_id = root["id"]
        kids = {f["name"]: f for f in self.drive.children(self.root_id) if f["mimeType"] == FOLDER_MIME}
        for name, role in [(META_FOLDER, "meta"), (RETIRED_FOLDER, "container"),
                           *((c, "container") for c in CONTAINERS.values())]:
            if name in kids and kids[name].get("appProperties", {}).get(P_MIRROR) == self.token:
                self.folders[name] = kids[name]["id"]
            elif create:
                self.folders[name] = self.drive.create_folder(
                    name, self.root_id, {P_ROLE: role, P_MIRROR: self.token})["id"]
        if META_FOLDER in self.folders:
            for f in self.drive.children(self.folders[META_FOLDER]):
                if f["mimeType"] == FOLDER_MIME and f["name"] == MANIFESTS_FOLDER:
                    self.folders[MANIFESTS_FOLDER] = f["id"]
                elif f["mimeType"] != FOLDER_MIME:
                    self.meta_files[f["name"]] = f["id"]
            if MANIFESTS_FOLDER not in self.folders and create:
                self.folders[MANIFESTS_FOLDER] = self.drive.create_folder(
                    MANIFESTS_FOLDER, self.folders[META_FOLDER], {P_ROLE: "meta", P_MIRROR: self.token})["id"]
        if MANIFESTS_FOLDER in self.folders:
            for f in self.drive.children(self.folders[MANIFESTS_FOLDER]):
                self.manifest_files[f["name"]] = f["id"]
        return True

    # ------------------------------------------------------------------ json files
    def _read_json(self, file_id: str | None):
        if not file_id:
            return None
        try:
            return json.loads(self.drive.download(file_id))
        except (ValueError, UnicodeDecodeError):
            return None

    def _write_json(self, table: dict, folder_key: str, name: str, obj) -> str:
        data = json.dumps(obj, indent=1, ensure_ascii=False, sort_keys=True).encode()
        props = {P_ROLE: "metafile", P_MIRROR: self.token}
        fid = table.get(name)
        if fid:
            self.drive.update_content(fid, data, "application/json", props)
        else:
            fid = self.drive.create_file(name, self.folders[folder_key], data, "application/json", props)["id"]
            table[name] = fid
        return fid

    def read_meta(self, name):
        return self._read_json(self.meta_files.get(name))

    def write_meta(self, name, obj):
        return self._write_json(self.meta_files, META_FOLDER, name, obj)

    def read_manifest(self, ref):
        return self._read_json(self.manifest_files.get(manifest_name(ref)))

    def write_manifest(self, ref, obj):
        return self._write_json(self.manifest_files, MANIFESTS_FOLDER, manifest_name(ref), obj)

    # ------------------------------------------------------------------ crawl
    def crawl(self, folder_id: str) -> tuple[dict[str, list[dict]], dict[str, str], list[dict]]:
        """Walk a namespace folder. Returns (path -> [files], dirpath -> folder id, non-file oddities)."""
        files: dict[str, list[dict]] = {}
        dirs: dict[str, str] = {"": folder_id}
        odd: list[dict] = []
        frontier = [("", folder_id)]
        # Level-parallel walk: one listing per folder, many folders in flight. Against real Drive
        # a serial walk of ~250 folders per namespace is the dominant cost of reconcile and verify.
        with cf.ThreadPoolExecutor(max_workers=max(1, self.cfg.crawl_workers)) as pool:
            while frontier:
                listings = list(pool.map(lambda pf: (pf[0], list(self.drive.children(pf[1]))), frontier))
                frontier = []
                for prefix, items in listings:
                    for item in items:
                        path = f"{prefix}/{item['name']}" if prefix else item["name"]
                        if item["mimeType"] == FOLDER_MIME:
                            if path in dirs:
                                # Duplicate folder name: keep the first as canonical and trash the other
                                # whole; any desired file inside it is re-created in the canonical folder.
                                odd.append({"kind": "duplicate_folder", "path": path, "id": item["id"]})
                                continue
                            dirs[path] = item["id"]
                            frontier.append((path, item["id"]))
                        else:
                            files.setdefault(path, []).append(item)
        return files, dirs, odd


class SyncRun:
    def __init__(self, drive: DriveClient, git: GitSource, cfg: SyncConfig):
        self.drive, self.git, self.cfg = drive, git, cfg
        self.mirror = Mirror(drive, cfg)
        self.started = time.time()
        self.report: dict = {"run_id": cfg.run_id, "mode": cfg.mode, "dry_run": cfg.dry_run,
                             "started_at": utcnow(), "namespaces": [], "retired": [], "errors": [],
                             "status": "running"}
        self.blob_index: dict[str, str] = {}
        self._index_lock = threading.Lock()
        self._indexed_manifests: set[str] = set()
        self.lease: DriveLease | None = None
        self.state: dict = {}
        self.pulls_meta: dict[int, dict] = {}

    # ------------------------------------------------------------------ helpers
    def _budget_left(self) -> bool:
        return self.cfg.time_budget_s is None or time.time() - self.started < self.cfg.time_budget_s

    def _check_budget(self) -> None:
        if not self._budget_left():
            raise BudgetExceeded("time budget exhausted")
        if self.lease:
            self.lease.renew()

    def _save_state(self) -> None:
        if self.cfg.dry_run:
            return
        self.state["updated_at"] = utcnow()
        self.mirror.write_meta(STATE_FILE, self.state)

    # ------------------------------------------------------------------ entry point
    def run(self) -> dict:
        cfg = self.cfg
        snap = self.git.snapshot(include_tags=cfg.include_tags, include_pulls=cfg.pull_heads != "none")
        if not snap.heads:
            raise SafetyStop("remote advertised no branches; refusing to treat that as 'everything deleted'")
        if not self.mirror.open(create=not cfg.dry_run):
            self.state = {}
        else:
            self.state = self.mirror.read_meta(STATE_FILE) or {}
        self._maybe_upgrade_to_reconcile()
        if not cfg.dry_run:
            self.lease = DriveLease(self.drive, self.mirror.folders[META_FOLDER], self.mirror.token,
                                    owner=f"{cfg.run_id}@{os.uname().nodename}", ttl_s=cfg.lock_ttl_s,
                                    settle_s=cfg.lock_settle_s, existing_id=self.mirror.meta_files.get(LOCK_FILE))
            self.lease.acquire(break_lock=cfg.break_lock)
            self.mirror.meta_files[LOCK_FILE] = self.lease.file_id
        try:
            self._run_locked(snap)
        finally:
            if self.lease:
                try:
                    self.lease.release()
                except Exception as e:  # pragma: no cover - best effort
                    log(f"lease release failed: {e}")
            self.git.close()
        return self.report

    def _maybe_upgrade_to_reconcile(self) -> None:
        """Reconcile cadence is decided from persisted state, not from the cron expression: a pending
        scheduled run can be replaced by a newer pending one (Actions concurrency), so a cron-selected
        reconcile could be starved forever on a busy mirror."""
        cfg = self.cfg
        self.report["mode_requested"] = cfg.mode
        if cfg.mode != "incremental" or not cfg.reconcile_after_hours:
            return
        last = self.state.get("last_reconcile_at")
        age = _age_hours(last)
        if last is None or age is None or age > cfg.reconcile_after_hours:
            cfg.mode = "reconcile"
            self.report["mode"] = "reconcile"
            self.report["mode_reason"] = (f"last reconcile {'never' if last is None else f'{age:.1f} h ago'} "
                                          f"(limit {cfg.reconcile_after_hours} h)")

    def _run_locked(self, snap: RefSnapshot) -> None:
        cfg = self.cfg
        st = self.state
        st.setdefault("schema", SCHEMA_VERSION)
        st.setdefault("namespaces", {})
        st.setdefault("retired", [])
        st["mirror"] = cfg.mirror_name
        st["mirror_token"] = self.mirror.token
        st["repo_url"] = cfg.repo_url
        st["generation"] = int(st.get("generation", 0)) + 1
        st["last_run"] = {"run_id": cfg.run_id, "started_at": self.report["started_at"], "status": "running",
                          "mode": cfg.mode}

        desired = self._desired_refs(snap)
        self.report["source"] = {"heads": len(snap.heads), "tags": len(snap.tags),
                                 "pull_heads_advertised": len(snap.pull_heads()),
                                 "pull_heads_mirrored": sum(1 for r in desired if r.startswith("refs/pull/")),
                                 "head_symref": snap.head_symref}
        st["quarantined"] = {r: {"reason": why, "since": (st.get("quarantined", {}).get(r) or {}).get("since")
                                 or utcnow()} for r, why in sorted(cfg.quarantine.items()) if r in desired}
        self.report["quarantined"] = sorted(st["quarantined"])
        self._write_refs(snap)
        if cfg.github_repo:
            self._write_pulls()

        # Default branch first, then most recently committed.
        def order(ref):
            return (ref != snap.head_symref, -self._commit_time(desired[ref]))
        incomplete = False
        for r in desired:
            st["namespaces"].get(r, {}).pop("unconfirmed", None)   # a ref that is advertised again is confirmed
        for ref in sorted(desired, key=order):
            if ref in st["quarantined"]:
                # Explicit, recorded exclusion (security gate). Never uploaded, never silent.
                self.report["namespaces"].append({"ref": ref, "commit": desired[ref], "action": "quarantined",
                                                  "status": "quarantined", "reason": st["quarantined"][ref]["reason"]})
                continue
            try:
                self._check_budget()
                res = self.sync_namespace(ref, desired[ref])
            except BudgetExceeded:
                incomplete = True
                self.report["namespaces"].append({"ref": ref, "action": "deferred", "status": "pending"})
                continue
            except Exception as e:  # keep going; failure is reported and the namespace stays in_progress
                log(f"{ref}: FAILED {type(e).__name__}: {e}")
                self.report["errors"].append({"ref": ref, "error": f"{type(e).__name__}: {e}"})
                res = {"ref": ref, "action": "failed", "status": "failed"}
            self.report["namespaces"].append(res)

        if not incomplete and not self.report["errors"]:
            self._retire_missing(desired, snap)
        elif any(r not in desired for r in st["namespaces"]):
            self.report["retire_skipped"] = "run incomplete or had errors; deleted refs are retired next run"

        status = "failed" if self.report["errors"] else ("incomplete" if incomplete else "complete")
        self.report["status"] = status
        self.report["finished_at"] = utcnow()
        self.report["drive_stats"] = dict(self.drive.stats)
        st["last_run"].update({"status": status, "finished_at": self.report["finished_at"],
                               "errors": len(self.report["errors"])})
        if status == "complete":
            st["last_complete_run"] = {"run_id": cfg.run_id, "finished_at": self.report["finished_at"],
                                       "refs": {r: desired[r] for r in sorted(desired) if r not in st["quarantined"]}}
            if cfg.mode == "reconcile":
                st["last_reconcile_at"] = self.report["finished_at"]
        self._save_state()

    def _source_block(self, ref: str) -> dict:
        """Where this namespace's tree came from, so a PR-only snapshot can never be mistaken for a branch."""
        if ref.startswith("refs/heads/"):
            return {"kind": "branch", "ref": ref}
        if ref.startswith("refs/tags/"):
            return {"kind": "tag", "ref": ref}
        n = int(ref.split("/")[2])
        p = self.pulls_meta.get(n)
        blk = {"kind": "pull_head", "ref": ref, "number": n,
               "contained_in_any_branch": bool(self.git.run(
                   "for-each-ref", "--contains", self.git.local_refs().get(ref, ""), "--count=1",
                   "--format=%(refname)", "refs/mirror/heads/", check=False).decode().strip())
               if self.git.local_refs().get(ref) else None}
        if p:
            blk["pr"] = {k: p.get(k) for k in ("state", "draft", "merged", "head_ref", "head_sha", "base_ref",
                                               "base_sha", "created_at", "closed_at", "merged_at", "title")}
        return blk

    def _commit_time(self, sha: str) -> float:
        try:
            return float(self.git.run("show", "-s", "--format=%ct", sha).decode().strip())
        except Exception:
            return 0.0

    def _desired_refs(self, snap: RefSnapshot) -> dict[str, str]:
        out = dict(snap.heads)
        if self.cfg.include_tags:
            out.update({t: self.git.peel_to_commit(s) for t, s in snap.tags.items()})
        if self.cfg.pull_heads == "all":
            out.update(snap.pull_heads())
        elif self.cfg.pull_heads == "orphaned":
            for ref, sha in snap.pull_heads().items():
                hit = self.git.run("for-each-ref", "--contains", sha, "--count=1", "--format=%(refname)",
                                   "refs/mirror/heads/").decode().strip()
                if not hit:
                    out[ref] = sha
            # Sticky: once a PR head has been mirrored it stays mirrored while the remote still advertises
            # it, even if a branch now contains its commit (a PR snapshot is not the same tree as the
            # branch tip). It is only ever retired after the ref itself is confirmed gone.
            sticky = [r for r in self.state.get("namespaces", {}) if r.startswith("refs/pull/")
                      and r in snap.pull_heads() and r not in out]
            for r in sticky:
                out[r] = snap.pull_heads()[r]
            self.report["pull_heads_sticky"] = sorted(sticky)
        return out

    # ------------------------------------------------------------------ metadata files
    def _write_refs(self, snap: RefSnapshot) -> None:
        body = {"head_symref": snap.head_symref, "heads": snap.heads, "tags": snap.tags,
                "tags_peeled": snap.tags_peeled, "pulls": snap.pulls, "other": snap.other,
                "repo_url": self.cfg.repo_url}
        d = digest(body)
        if self.state.get("refs_digest") != d and not self.cfg.dry_run:
            body["captured_at"] = utcnow()
            self.mirror.write_meta(REFS_FILE, body)
            self.state["refs_digest"] = d
            self.report["refs_file"] = "updated"
        else:
            self.report["refs_file"] = "unchanged"

    def _write_pulls(self) -> None:
        from .github_meta import GitHubMeta
        try:
            pulls = GitHubMeta(self.cfg.github_repo).pulls()
        except Exception as e:
            self.report["errors"].append({"ref": "(pull metadata)", "error": f"{type(e).__name__}: {e}"})
            return
        self.pulls_meta = {p["number"]: p for p in pulls}
        d = digest(pulls)
        if self.state.get("pulls_digest") != d and not self.cfg.dry_run:
            self.mirror.write_meta(PULLS_FILE, {"captured_at": utcnow(), "repo": self.cfg.github_repo,
                                                "pull_requests": pulls})
            self.state["pulls_digest"] = d
            self.report["pulls_file"] = "updated"
        else:
            self.report["pulls_file"] = "unchanged"

    # ------------------------------------------------------------------ one namespace
    def sync_namespace(self, ref: str, commit: str) -> dict:
        cfg, git, drive, mirror = self.cfg, self.git, self.drive, self.mirror
        container, folder_name = ref_namespace(ref)
        nss = self.state["namespaces"].get(ref)
        res = {"ref": ref, "commit": commit, "action": "noop", "uploads": 0, "copies": 0,
               "updates": 0, "moves": 0, "meta_updates": 0, "trashed": 0, "folders_created": 0,
               "folders_trashed": 0, "source": None, "forced_update": None, "planned": {}}

        if nss and nss.get("status") == "complete" and nss.get("commit") == commit and cfg.mode == "incremental":
            res["status"] = "complete"
            return res

        entries = git.tree_entries(commit)
        desired = {p: e for p, e in entries.items() if e.mode in FILE_MODES}
        unsupported = [{"path": e.path, "mode": e.mode, "oid": e.oid,
                        "reason": "gitlink (submodule commit pointer has no bytes in this repository)"
                        if e.mode == "160000" else f"unsupported mode {e.mode}"}
                       for e in entries.values() if e.mode not in FILE_MODES]
        if not desired and not cfg.allow_empty_tree and nss:
            raise SafetyStop(f"{ref}: desired tree is empty; refusing to empty the namespace")

        if nss and nss.get("commit") and nss.get("commit") != commit:
            anc = git.is_ancestor(nss["commit"], commit)
            res["forced_update"] = (anc is not True)   # missing old commit => not in new history => rewritten
            res["previous_commit"] = nss["commit"]

        # Resolve the namespace folder and what currently lives in it.
        folder = drive.get(nss["folder_id"]) if nss else None
        if folder and not self._folder_is_namespace(folder, nss, container):
            log(f"{ref}: namespace folder failed identity check; starting a fresh namespace")
            res["anomaly"] = "namespace folder identity mismatch"
            folder = None
        manifest = None
        if folder is None:
            if cfg.dry_run:
                current_files, dirs, odd = {}, {"": None}, []
                res["source"] = "new"
            else:
                nsid = os.urandom(8).hex()
                folder = drive.create_folder(folder_name, mirror.folders[container], fit_props(
                    {P_ROLE: "ns", P_MIRROR: mirror.token, P_NS: nsid, P_REF: ref}))
                nss = {"nsid": nsid, "folder_id": folder["id"], "created_at": utcnow()}
                self.state["namespaces"][ref] = nss
                current_files, dirs, odd = {}, {"": folder["id"]}, []
                res["source"] = "new"
                res["action"] = "created"
        else:
            if cfg.mode == "incremental" and nss.get("status") == "complete":
                manifest = mirror.read_manifest(ref)
                if not manifest or manifest.get("folder_id") != nss["folder_id"] or "files" not in manifest:
                    manifest = None
            if manifest:
                current_files = {p: [{"id": r["id"], "name": p.rsplit("/", 1)[-1],
                                      "appProperties": {P_BLOB: r["blob"], P_MODE: r["mode"],
                                                        P_SHA256: r["sha256"]},
                                      "_trusted": True, "size": str(r["size"]),
                                      "sha256Checksum": r["sha256"], "mimeType": r.get("mime", "")}]
                                 for p, r in manifest["files"].items()}
                dirs = dict(manifest["folders"])
                odd = []
                res["source"] = "manifest"
            else:
                current_files, dirs, odd = mirror.crawl(nss["folder_id"])
                res["source"] = "crawl"
            res["action"] = "updated"

        self._index_manifest(ref, manifest)
        if res["source"] in ("new", "crawl"):
            with self._index_lock:
                uncovered = any(e.oid not in self.blob_index for e in desired.values())
            if uncovered:
                self._seed_index_from_ancestor(ref, commit)

        plan = self._plan(desired, current_files, dirs, odd)
        res["planned"] = {k: len(v) for k, v in plan.items()}
        if cfg.dry_run:
            res["status"] = "planned"
            return res

        nss.update({"status": "in_progress", "target_commit": commit, "started_at": utcnow()})
        self._save_state()

        if plan["trash"] or plan["dir_trash"]:
            fresh = drive.get(nss["folder_id"])
            if not fresh or not self._folder_is_namespace(fresh, nss, container):
                raise SafetyStop(f"{ref}: namespace identity changed before deletions")

        new_manifest_files = self._apply(ref, plan, desired, dirs, res)
        manifest_obj = {
            "schema": SCHEMA_VERSION, "ref": ref, "namespace": f"{container}/{folder_name}",
            "source": self._source_block(ref), "nsid": nss["nsid"], "folder_id": nss["folder_id"],
            "mirror": cfg.mirror_name,
            "repo_url": cfg.repo_url, "commit": git.commit_meta(commit), "synced_at": utcnow(),
            "run_id": cfg.run_id, "file_count": len(new_manifest_files),
            "bytes": sum(r["size"] for r in new_manifest_files.values()),
            "files": dict(sorted(new_manifest_files.items())),
            "folders": dict(sorted(dirs.items())), "unsupported": unsupported,
        }
        nss["manifest_id"] = self.mirror.write_manifest(ref, manifest_obj)
        nss.update({"status": "complete", "commit": commit, "tree": manifest_obj["commit"]["tree"],
                    "synced_at": manifest_obj["synced_at"], "files": manifest_obj["file_count"],
                    "bytes": manifest_obj["bytes"], "unsupported": len(unsupported)})
        nss.pop("target_commit", None)
        self._save_state()
        res["status"] = "complete"
        res["unsupported"] = len(unsupported)
        log(f"{ref}: {res['action']} via {res['source']} — {res['uploads']} uploads, {res['copies']} copies, "
            f"{res['updates']} updates, {res['moves']} moves, {res['meta_updates']} meta, {res['trashed']} trashed")
        return res

    def _index_manifest(self, ref: str, manifest: dict | None) -> None:
        if not manifest or ref in self._indexed_manifests:
            return
        self._indexed_manifests.add(ref)
        with self._index_lock:
            for m in manifest.get("files", {}).values():
                self.blob_index.setdefault(m["blob"], m["id"])

    def _seed_index_from_ancestor(self, ref: str, commit: str) -> None:
        """Load the manifest of the closest already-mirrored ancestor so a new or re-crawled
        namespace copies unchanged files server-side instead of re-uploading them."""
        best, best_n = None, -1
        for other, nss in self.state["namespaces"].items():
            if other == ref or nss.get("status") != "complete" or not nss.get("commit"):
                continue
            if self.git.is_ancestor(nss["commit"], commit):
                n = int(self.git.run("rev-list", "--count", nss["commit"]).decode())
                if n > best_n:
                    best, best_n = other, n
        if best is None:
            best = next((r for r in self.state["namespaces"] if r.startswith("refs/heads/")
                         and self.state["namespaces"][r].get("status") == "complete"), None)
        if best and best not in self._indexed_manifests:
            self._index_manifest(best, self.mirror.read_manifest(best))

    def _folder_is_namespace(self, folder: dict, nss: dict, container: str) -> bool:
        props = folder.get("appProperties") or {}
        return (not folder.get("trashed") and props.get(P_MIRROR) == self.mirror.token
                and props.get(P_NS) == nss.get("nsid")
                and self.mirror.folders.get(container) in (folder.get("parents") or []))

    # ------------------------------------------------------------------ planning
    def _content_matches(self, item: dict, e: TreeEntry) -> bool:
        if item.get("mimeType", "").startswith(GOOGLE_APPS_PREFIX):
            return False
        props = item.get("appProperties") or {}
        if item.get("_trusted"):
            return props.get(P_BLOB) == e.oid
        # Crawled item: trust Drive's own checksum of the stored bytes, not our labels.
        if item.get("size") is not None and int(item["size"]) != e.size:
            return False
        server = item.get("sha256Checksum")
        if server:
            return server == self.git.blob_sha256(e.oid)
        if item.get("md5Checksum"):
            return item["md5Checksum"] == hashlib.md5(self.git.read_blob(e.oid)).hexdigest()
        return False

    def _plan(self, desired: dict[str, TreeEntry], current: dict[str, list[dict]], dirs: dict[str, str],
              odd: list[dict]) -> dict:
        plan = {"adds": [], "updates": [], "moves": [], "meta": [], "keep": [], "trash": [], "dir_trash": [],
                "dir_create": []}
        removable: dict[str, list[tuple[str, dict]]] = {}   # blob -> [(old path, item)] available for rename
        for path, items in current.items():
            e = desired.get(path)
            if e is None:
                for it in items:
                    blob = (it.get("appProperties") or {}).get(P_BLOB)
                    removable.setdefault(blob, []).append((path, it))
                continue
            good = next((it for it in items if self._content_matches(it, e)), None)
            if good is not None:
                if (good.get("appProperties") or {}).get(P_MODE) != e.mode:
                    plan["meta"].append((path, good))
                else:
                    plan["keep"].append((path, good))
                plan["trash"].extend(it for it in items if it is not good)
            else:
                reusable = next((it for it in items if not it.get("mimeType", "").startswith(GOOGLE_APPS_PREFIX)),
                                None)
                if reusable is not None:
                    plan["updates"].append((path, reusable))
                    plan["trash"].extend(it for it in items if it is not reusable)
                else:
                    plan["adds"].append(path)
                    plan["trash"].extend(items)
        for path in sorted(set(desired) - set(current)):
            plan["adds"].append(path)
        # Rename detection: reuse a file whose path disappeared and whose bytes are identical.
        remaining_adds = []
        for path in plan["adds"]:
            cands = removable.get(desired[path].oid) or []
            hit = next((c for c in cands if self._content_matches(c[1], desired[path])), None)
            if hit:
                cands.remove(hit)
                plan["moves"].append((path, hit[1], hit[0]))
            else:
                remaining_adds.append(path)
        plan["adds"] = remaining_adds
        for cands in removable.values():
            plan["trash"].extend(it for _, it in cands)
        want_dirs = {""}
        for path in desired:
            parts = path.split("/")[:-1]
            for i in range(1, len(parts) + 1):
                want_dirs.add("/".join(parts[:i]))
        plan["dir_create"] = sorted(want_dirs - set(dirs), key=lambda d: d.count("/"))
        obsolete = set(dirs) - want_dirs
        plan["dir_trash"] = sorted(d for d in obsolete
                                   if (d.rsplit("/", 1)[0] if "/" in d else "") not in obsolete)
        plan["trash"].extend({"id": o["id"]} for o in odd if o["kind"] == "duplicate_folder")
        return plan

    # ------------------------------------------------------------------ applying
    def _apply(self, ref: str, plan: dict, desired: dict[str, TreeEntry], dirs: dict[str, str],
               res: dict) -> dict[str, dict]:
        drive, git, mirror = self.drive, self.git, self.mirror
        nss = self.state["namespaces"][ref]
        manifest: dict[str, dict] = {}
        mlock = threading.Lock()

        def bump(key: str) -> None:
            with mlock:
                res[key] += 1

        def props_for(e: TreeEntry) -> dict:
            return {P_ROLE: "file", P_MIRROR: mirror.token, P_NS: nss["nsid"], P_BLOB: e.oid,
                    P_SHA256: git.blob_sha256(e.oid), P_MODE: e.mode}

        def record(path: str, e: TreeEntry, item: dict, verify: bool) -> None:
            if verify:
                self._verify_stored(item, e)
            with mlock:
                manifest[path] = {"id": item["id"], "blob": e.oid, "mode": e.mode, "size": e.size,
                                  "sha256": git.blob_sha256(e.oid), "mime": guess_mime(path)}
            with self._index_lock:
                self.blob_index[e.oid] = item["id"]

        for d in plan["dir_create"]:
            self._check_budget()
            parent = dirs[d.rsplit("/", 1)[0]] if "/" in d else dirs[""]
            f = drive.create_folder(d.rsplit("/", 1)[-1], parent,
                                    {P_ROLE: "dir", P_MIRROR: mirror.token, P_NS: nss["nsid"]})
            dirs[d] = f["id"]
            res["folders_created"] += 1

        def parent_of(path: str) -> str:
            return dirs[path.rsplit("/", 1)[0]] if "/" in path else dirs[""]

        def name_of(path: str) -> str:
            return path.rsplit("/", 1)[-1]

        def op_keep(path, item):
            record(path, desired[path], item, verify=False)

        def op_meta(path, item):
            e = desired[path]
            out = drive.update_meta(item["id"], props=props_for(e))
            record(path, e, out, verify=False)
            bump("meta_updates")

        def op_move(path, item, old_path):
            e = desired[path]
            old_parent = (item.get("parents") or [None])[0] or parent_of(old_path)
            new_parent = parent_of(path)
            same = old_parent == new_parent
            out = drive.update_meta(item["id"], name=name_of(path), add_parent=None if same else new_parent,
                                    remove_parent=None if same else old_parent, props=props_for(e))
            record(path, e, out, verify=True)
            bump("moves")

        def op_update(path, item):
            e = desired[path]
            out = drive.update_content(item["id"], git.read_blob(e.oid), guess_mime(path), props_for(e),
                                       name=name_of(path))
            record(path, e, out, verify=True)
            bump("updates")

        def op_add(path):
            e = desired[path]
            src = self._copy_source(e)
            if src:
                try:
                    out = drive.copy(src, name_of(path), parent_of(path), props_for(e))
                    self._verify_stored(out, e)
                    record(path, e, out, verify=False)
                    bump("copies")
                    return
                except Exception as ex:   # stale index / copy mismatch -> fall back to upload
                    log(f"{ref}: copy of {path} failed ({ex}); uploading instead")
            out = drive.create_file(name_of(path), parent_of(path), git.read_blob(e.oid), guess_mime(path),
                                    props_for(e))
            record(path, e, out, verify=True)
            bump("uploads")

        def op_trash(item):
            drive.trash(item["id"])
            bump("trashed")

        for path, item in plan["keep"]:
            op_keep(path, item)
        jobs = ([(op_meta, a) for a in plan["meta"]] + [(op_move, a) for a in plan["moves"]]
                + [(op_update, a) for a in plan["updates"]] + [(op_add, (p,)) for p in plan["adds"]])
        self._run_jobs(jobs)
        # Deletions only after every desired file is in place.
        self._run_jobs([(op_trash, (it,)) for it in plan["trash"]])
        for d in plan["dir_trash"]:
            self._check_budget()
            drive.trash(dirs.pop(d))
            for sub in [s for s in dirs if s.startswith(d + "/")]:
                dirs.pop(sub)
            res["folders_trashed"] += 1
        missing = set(desired) - set(manifest)
        if missing:
            raise RuntimeError(f"{len(missing)} desired files were not placed, e.g. {sorted(missing)[:3]}")
        return manifest

    def _run_jobs(self, jobs: list) -> None:
        if not jobs:
            return
        errors: list[Exception] = []
        workers = max(1, self.cfg.workers)
        submitted = 0
        stopped = False
        with cf.ThreadPoolExecutor(max_workers=workers) as pool:
            inflight: set = set()
            it = iter(jobs)
            while True:
                while len(inflight) < workers * 2 and not stopped:
                    if not self._budget_left():
                        stopped = True
                        break
                    nxt = next(it, None)
                    if nxt is None:
                        break
                    inflight.add(pool.submit(nxt[0], *nxt[1]))
                    submitted += 1
                if not inflight:
                    break
                done, inflight = cf.wait(inflight, return_when=cf.FIRST_COMPLETED)
                for f in done:
                    if f.exception() is not None:
                        errors.append(f.exception())
                if self.lease:
                    self.lease.renew()
                if len(errors) >= 25:
                    stopped = True   # stop feeding work after repeated failures
        if errors:
            raise RuntimeError(f"{len(errors)} Drive operations failed; first: {errors[0]}")
        if submitted < len(jobs):
            raise BudgetExceeded("time budget exhausted mid-namespace")

    def _copy_source(self, e: TreeEntry) -> str | None:
        with self._index_lock:
            src = self.blob_index.get(e.oid)
        if src or (e.size or 0) < COPY_LOOKUP_MIN_BYTES:
            return src
        q = (f"appProperties has {{ key='{P_BLOB}' and value={q_str(e.oid)} }} and "
             f"appProperties has {{ key='{P_MIRROR}' and value={q_str(self.mirror.token)} }} and trashed = false")
        for item in self.drive.list(q, page_size=1):
            return item["id"]
        return None

    def _verify_stored(self, item: dict, e: TreeEntry) -> None:
        if item.get("size") is not None and int(item["size"]) != e.size:
            raise RuntimeError(f"size mismatch after write for blob {e.oid}: {item['size']} != {e.size}")
        server = item.get("sha256Checksum")
        if server and server != self.git.blob_sha256(e.oid):
            raise RuntimeError(f"sha256 mismatch after write for blob {e.oid}")
        if item.get("mimeType", "").startswith(GOOGLE_APPS_PREFIX):
            raise RuntimeError(f"Drive converted blob {e.oid} to {item['mimeType']}")
        if item.get("trashed"):
            raise RuntimeError(f"write for blob {e.oid} produced a trashed file")

    # ------------------------------------------------------------------ deleted refs
    def _pr_still_exists(self, ref: str) -> bool | None:
        """True/False if the PR is known to exist/not exist; None when it cannot be checked."""
        if not self.cfg.github_repo:
            return None
        try:
            from .github_meta import GitHubMeta
            GitHubMeta(self.cfg.github_repo).get(f"/repos/{self.cfg.github_repo}/pulls/{ref.split('/')[2]}")
            return True
        except Exception as e:
            return False if getattr(e, "code", None) == 404 else None

    def _retire_missing(self, desired: dict[str, str], snap: RefSnapshot) -> None:
        gone = sorted(r for r in self.state["namespaces"] if r not in desired)
        if not gone:
            return
        # Second, independent observation of the remote before anything is retired. Anything still
        # advertised is never "deleted", whatever the current scope settings say.
        confirm = self.git.ls_remote()
        still = set(confirm.heads) | set(confirm.tags) | set(confirm.pull_heads())
        deferred: dict[str, str] = {}
        for r in list(gone):
            if r in still:
                deferred[r] = "still advertised by the remote (outside the current mirror scope or reappeared)"
                continue
            # A ref missing from a full listing is only *confirmed* deleted by a targeted query that
            # succeeds and returns nothing. An error, a timeout or a permissions problem is "inaccessible".
            exists = self.git.probe_ref(r)
            if exists is None:
                deferred[r] = "inaccessible: targeted ref query failed; deletion not confirmed"
            elif exists:
                deferred[r] = "targeted query found the ref although the full listing did not"
            elif r.startswith("refs/pull/") and self._pr_still_exists(r) is not False:
                deferred[r] = ("pull request still exists (or could not be checked) but its head ref is not "
                               "advertised: treated as inaccessible, not deleted")
        gone = [r for r in gone if r not in deferred]
        if deferred:
            self.report["retire_deferred"] = deferred
            for r, why in deferred.items():
                self.state["namespaces"][r]["unconfirmed"] = {"reason": why, "since":
                                                              (self.state["namespaces"][r].get("unconfirmed") or {}).get("since")
                                                              or utcnow()}
        for r in gone + [x for x in self.state["namespaces"] if x not in deferred]:
            self.state["namespaces"].get(r, {}).pop("unconfirmed", None)
        total = max(1, len(self.state["namespaces"]))
        if len(gone) > max(5, self.cfg.max_retire_fraction * total) and not self.cfg.allow_mass_retire:
            if self.cfg.dry_run:
                self.report["retire_guard_tripped"] = True
            else:
                raise SafetyStop(f"{len(gone)} of {total} namespaces would be retired at once; "
                             f"re-run with --allow-mass-retire after checking the remote")
        for ref in gone:
            nss = self.state["namespaces"][ref]
            container, folder_name = ref_namespace(ref)
            entry = {"ref": ref, "last_commit": nss.get("commit"), "nsid": nss.get("nsid"),
                     "retired_at": utcnow(), "reason": "ref no longer advertised by remote (confirmed twice)"}
            if self.cfg.dry_run:
                self.report["retired"].append({**entry, "dry_run": True})
                continue
            folder = self.drive.get(nss["folder_id"])
            if folder and self._folder_is_namespace(folder, nss, container):
                stamp = entry["retired_at"].replace(":", "").replace("-", "")
                new_name = f"{container}__{folder_name}@{stamp}"
                self.drive.update_meta(folder["id"], name=new_name, add_parent=self.mirror.folders[RETIRED_FOLDER],
                                       remove_parent=self.mirror.folders[container],
                                       props={P_ROLE: "retired"})
                entry["retired_folder"] = new_name
                entry["folder_id"] = folder["id"]
            else:
                entry["note"] = "namespace folder missing or failed identity check; nothing moved"
            mid = self.mirror.manifest_files.pop(manifest_name(ref), None)
            if mid:
                self.drive.update_meta(mid, name=f"retired__{manifest_name(ref)[:-5]}@{entry['retired_at']}.json")
                entry["manifest_id"] = mid
            self.state["retired"].append(entry)
            del self.state["namespaces"][ref]
            self.report["retired"].append(entry)
            log(f"{ref}: retired to {RETIRED_FOLDER}/")
        self._save_state()

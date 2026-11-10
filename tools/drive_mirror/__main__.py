"""CLI: PYTHONPATH=tools python3 -m drive_mirror {inventory,sync,verify,status} ...

Exit codes: 0 ok · 1 failed · 2 incomplete (time budget; resumable) · 3 lease held
            4 blocked (no Drive credentials / access) · 5 safety stop
"""
from __future__ import annotations

import argparse
import json
import os
import sys

from .auth import AuthUnavailable, from_env
from .drive import DriveClient, DriveError
from .gitsrc import GitSource
from .lock import LockHeld

EXIT_OK, EXIT_FAIL, EXIT_INCOMPLETE, EXIT_LOCKED, EXIT_BLOCKED, EXIT_SAFETY = 0, 1, 2, 3, 4, 5


def _common(p: argparse.ArgumentParser, drive: bool = True) -> None:
    p.add_argument("--repo-url", required=True, help="source repository (https URL or local path)")
    p.add_argument("--work-dir", default=os.path.join(os.environ.get("RUNNER_TEMP", "/tmp"), "drive-mirror-src.git"),
                   help="bare repository cache used for fetching")
    p.add_argument("--report", help="write the JSON report here")
    if drive:
        p.add_argument("--mirror-name", required=True, help="mirror root folder name (one path segment)")
        p.add_argument("--parent-folder-id", default=os.environ.get("DRIVE_PARENT_FOLDER_ID"),
                       help="Drive folder that holds the mirror root ('root' = My Drive)")
        p.add_argument("--drive-id", default=os.environ.get("DRIVE_SHARED_DRIVE_ID") or None,
                       help="shared drive id, when the parent folder is on a shared drive")
        p.add_argument("--api-base", default=os.environ.get("DRIVE_API_BASE", "https://www.googleapis.com"))
        p.add_argument("--no-tags", action="store_true", help="do not mirror tag trees (still recorded in refs.json)")
        p.add_argument("--pull-heads", choices=["none", "orphaned", "all"], default="none",
                       help="also mirror PR head trees: none, only heads no branch contains, or all")


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(prog="drive_mirror")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p_inv = sub.add_parser("inventory", help="machine-readable inventory of refs, trees, PRs and CI")
    _common(p_inv, drive=False)
    p_inv.add_argument("--github-repo")
    p_inv.add_argument("--files-out", help="also write a per-file JSONL inventory (path, mode, blob, sha256)")
    p_inv.add_argument("--no-ci", action="store_true")

    p_sync = sub.add_parser("sync", help="synchronise every ref into its Drive namespace")
    _common(p_sync)
    p_sync.add_argument("--mode", choices=["incremental", "reconcile"], default="incremental")
    p_sync.add_argument("--dry-run", action="store_true")
    p_sync.add_argument("--time-budget-min", type=float)
    p_sync.add_argument("--workers", type=int, default=4)
    p_sync.add_argument("--github-repo", help="also store PR metadata (pulls.json)")
    p_sync.add_argument("--max-retire-fraction", type=float, default=0.2)
    p_sync.add_argument("--allow-mass-retire", action="store_true")
    p_sync.add_argument("--allow-empty-tree", action="store_true")
    p_sync.add_argument("--break-lock", action="store_true")
    p_sync.add_argument("--lock-settle-s", type=float, default=2.0)

    p_ver = sub.add_parser("verify", help="independently verify the mirror against the remote")
    _common(p_ver)
    p_ver.add_argument("--deep-sample", type=int, default=0, help="download N files and re-hash them")
    p_ver.add_argument("--max-age-hours", type=float)

    p_stat = sub.add_parser("status", help="cheap staleness check of state.json against ls-remote")
    _common(p_stat)
    p_stat.add_argument("--max-age-hours", type=float, default=26.0)

    p_st = sub.add_parser("preflight", help="check Drive credentials, destination access and quota")
    p_st.add_argument("--parent-folder-id", default=os.environ.get("DRIVE_PARENT_FOLDER_ID"))
    p_st.add_argument("--drive-id", default=os.environ.get("DRIVE_SHARED_DRIVE_ID") or None)
    p_st.add_argument("--api-base", default=os.environ.get("DRIVE_API_BASE", "https://www.googleapis.com"))
    p_st.add_argument("--report")
    p_st.add_argument("--expected-bytes", type=int, default=0)
    p_st.add_argument("--write-probe", action="store_true", help="also create and trash a small probe file")

    a = ap.parse_args(argv)
    try:
        return _dispatch(a)
    except AuthUnavailable as e:
        _emit(a, {"result": "BLOCKED", "reason": str(e)})
        return EXIT_BLOCKED


def _drive(a) -> DriveClient:
    if not a.parent_folder_id:
        raise AuthUnavailable("no destination: --parent-folder-id / DRIVE_PARENT_FOLDER_ID is not set")
    return DriveClient(from_env(), base=a.api_base, drive_id=a.drive_id)


def _dispatch(a) -> int:
    if a.cmd == "inventory":
        from .github_meta import GitHubMeta
        from .inventory import build_inventory
        src = GitSource(a.work_dir, a.repo_url)
        fh = open(a.files_out, "w", encoding="utf-8") if a.files_out else None
        try:
            inv = build_inventory(src, GitHubMeta(a.github_repo) if a.github_repo else None,
                                  include_ci=not a.no_ci, files_out=fh)
        finally:
            if fh:
                fh.close()
            src.close()
        _emit(a, inv, summary=inv["counts"])
        return EXIT_OK

    if a.cmd == "preflight":
        from .preflight import preflight
        rep = preflight(_drive(a), a.parent_folder_id, a.drive_id, a.expected_bytes, a.write_probe)
        _emit(a, rep)
        return {"PASS": EXIT_OK, "BLOCKED": EXIT_BLOCKED}.get(rep["result"], EXIT_FAIL)

    drive = _drive(a)
    git = GitSource(a.work_dir, a.repo_url)
    if a.cmd == "sync":
        from .sync import SafetyStop, SyncConfig, SyncRun
        cfg = SyncConfig(mirror_name=a.mirror_name, parent_folder_id=a.parent_folder_id, repo_url=a.repo_url,
                         mode=a.mode, include_tags=not a.no_tags, pull_heads=a.pull_heads, dry_run=a.dry_run,
                         time_budget_s=a.time_budget_min * 60 if a.time_budget_min else None, workers=a.workers,
                         max_retire_fraction=a.max_retire_fraction, allow_mass_retire=a.allow_mass_retire,
                         allow_empty_tree=a.allow_empty_tree, break_lock=a.break_lock,
                         lock_settle_s=a.lock_settle_s, github_repo=a.github_repo)
        try:
            rep = SyncRun(drive, git, cfg).run()
        except LockHeld as e:
            _emit(a, {"result": "LOCKED", "reason": str(e)})
            return EXIT_LOCKED
        except SafetyStop as e:
            _emit(a, {"result": "SAFETY_STOP", "reason": str(e)})
            return EXIT_SAFETY
        except DriveError as e:
            if e.status in (401, 403, 404) and e.reason not in ("rateLimitExceeded", "userRateLimitExceeded"):
                _emit(a, {"result": "BLOCKED", "reason": str(e)})
                return EXIT_BLOCKED
            raise
        changed = [n for n in rep["namespaces"] if n.get("action") not in ("noop",)]
        _emit(a, rep, summary={"status": rep["status"], "namespaces": len(rep["namespaces"]),
                               "changed": len(changed), "retired": len(rep["retired"]),
                               "errors": len(rep["errors"]), "drive_stats": rep.get("drive_stats")})
        return {"complete": EXIT_OK, "incomplete": EXIT_INCOMPLETE}.get(rep["status"], EXIT_FAIL)

    if a.cmd == "status":
        from .status import status
        rep = status(drive, git, a.mirror_name, a.parent_folder_id, not a.no_tags, a.max_age_hours)
        _emit(a, rep)
        return EXIT_OK if rep["result"] == "PASS" else EXIT_FAIL

    if a.cmd == "verify":
        from .verify import verify
        rep = verify(drive, git, a.mirror_name, a.parent_folder_id, include_tags=not a.no_tags,
                     pull_heads=a.pull_heads, deep_sample=a.deep_sample, max_age_hours=a.max_age_hours)
        git.close()
        _emit(a, rep, summary={"result": rep["result"], "failing_namespaces": rep["failing_namespaces"][:20],
                               **{k: rep["global"].get(k) for k in ("expected_namespaces", "found_namespaces",
                                                                     "files_expected", "files_verified",
                                                                     "problems")}})
        return EXIT_OK if rep["result"] == "PASS" else EXIT_FAIL
    return EXIT_FAIL


ID_KEYS = {"folder_id", "manifest_id", "id"}


def _scrub(obj):
    """Reports become public workflow artifacts on a public repo: drop Drive object ids."""
    if isinstance(obj, dict):
        return {k: _scrub(v) for k, v in obj.items() if k not in ID_KEYS}
    if isinstance(obj, list):
        return [_scrub(v) for v in obj]
    return obj


def _emit(a, obj, summary=None) -> None:
    if getattr(a, "cmd", "") in ("sync", "verify", "status"):
        obj = _scrub(obj)
    if getattr(a, "report", None):
        with open(a.report, "w", encoding="utf-8") as fh:
            json.dump(obj, fh, indent=1, ensure_ascii=False)
    print(json.dumps(summary if summary is not None else obj, indent=1, ensure_ascii=False))


if __name__ == "__main__":
    sys.exit(main())

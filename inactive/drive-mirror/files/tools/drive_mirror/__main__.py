"""CLI: PYTHONPATH=tools python3 -m drive_mirror {inventory,sync,verify,status} ...

Exit codes: 0 ok · 1 failed · 2 incomplete (time budget; resumable) · 3 lease held
            4 blocked (no Drive credentials / access) · 5 safety stop
            6 security gate: unapproved findings (refs quarantined)
            7 gate could not decide, or the approved source snapshot is missing / invalid / no longer matches (fail closed)
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
EXIT_GATE_BLOCKED, EXIT_GATE_ERROR = 6, 7


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
        p.add_argument("--pull-heads", choices=["none", "orphaned", "all"], default="orphaned",
                       help="PR head trees to mirror under pulls/: orphaned (default: heads no branch contains), all, or none")
        p.add_argument("--quarantine-file", help="approval written by `secgate`: its quarantined refs are never uploaded; "
                                                 "`sync` (and `verify`) also take their frozen source snapshot from it")
        p.add_argument("--approval-sha256", help="sha256 of the approval file as recorded by the gate step; the file is "
                                                 "refused if it differs")


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
    p_sync.add_argument("--reconcile-after-hours", type=float,
                        help="an incremental run becomes a reconcile when the last one is older than this")
    p_sync.add_argument("--crawl-workers", type=int, default=8)
    p_sync.add_argument("--allow-ungated", action="store_true",
                        help="UNSAFE, scratch data only: sync without an approved source snapshot (re-resolves moving refs; "
                             "recorded in the report as gate=UNGATED). Never used by the workflow")
    p_sync.add_argument("--allow-external-scan-report", action="store_true",
                        help="accept an approval whose scan report was produced outside `secgate --run-scanner` "
                             "(not provably about the snapshot)")

    p_ver = sub.add_parser("verify", help="independently verify the mirror against the remote")
    _common(p_ver)
    p_ver.add_argument("--deep-sample", type=int, default=0, help="download N files and re-hash them")
    p_ver.add_argument("--max-age-hours", type=float)
    p_ver.add_argument("--sync-report", help="verify bytes only for namespaces this sync report changed "
                                             "(others get the cheap structural checks); omit for a full verification")

    p_stat = sub.add_parser("status", help="cheap staleness check of state.json against ls-remote")
    _common(p_stat)
    p_stat.add_argument("--max-age-hours", type=float, default=26.0)

    p_prep = sub.add_parser("prepare", help="fetch every ref into the cache (input for the secret scan)")
    _common(p_prep, drive=False)
    p_prep.add_argument("--pull-heads", choices=["none", "orphaned", "all"], default="all",
                        help="ignored: every ref is always frozen so the gate examines everything sync may upload")
    p_prep.add_argument("--snapshot-out", required=True,
                        help="write the frozen source snapshot (ref -> commit, tree ids) here")

    p_gate = sub.add_parser("secgate", help="decide which refs may be uploaded, from a gitleaks report")
    _common(p_gate, drive=False)
    p_gate.add_argument("--source-snapshot", required=True, help="frozen snapshot from `prepare --snapshot-out`")
    p_gate.add_argument("--scan-report", help="gitleaks JSON report (external; with --run-scanner: where to keep the report)")
    p_gate.add_argument("--run-scanner", metavar="GITLEAKS", help="run this gitleaks over exactly the snapshot's commits "
                                                                  "(binds the scan to the snapshot)")
    p_gate.add_argument("--approved", default=os.path.join(os.path.dirname(__file__), "security",
                                                           "approved_findings.json"))
    p_gate.add_argument("--pull-heads", choices=["none", "orphaned", "all"], default="orphaned",
                        help="ignored: every ref of the snapshot is examined")
    p_gate.add_argument("--no-tags", action="store_true", help="ignored: every ref of the snapshot is examined")
    p_gate.add_argument("--quarantine-out", required=True)

    p_lin = sub.add_parser("lineage", help="branch lineage and PR-state verification (JSON)")
    _common(p_lin, drive=False)
    p_lin.add_argument("--inventory", required=True, help="inventory JSON produced by `inventory`")

    p_cs = sub.add_parser("current-state", help="AI-readable CURRENT_STATE index from inventory + lineage")
    p_cs.add_argument("--inventory", required=True)
    p_cs.add_argument("--lineage", required=True)
    p_cs.add_argument("--facts", required=True, help="curated facts JSON (conflicts, owner decisions, findings)")
    p_cs.add_argument("--report", required=True)

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
    except Exception as e:
        from .security import GateError
        if isinstance(e, GateError):
            _emit(a, {"result": "GATE_ERROR", "reason": str(e)})
            return EXIT_GATE_ERROR
        raise


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

    if a.cmd == "prepare":
        from . import snapshot as frozen
        from .inventory import utcnow
        src = GitSource(a.work_dir, a.repo_url)
        snap = src.snapshot(include_tags=True, include_pulls=True)
        doc = frozen.build(src, snap, a.repo_url, utcnow())
        with open(a.snapshot_out, "w", encoding="utf-8") as fh:
            json.dump(doc, fh, indent=1, ensure_ascii=False)
        _emit(a, {"heads": len(snap.heads), "tags": len(snap.tags), "pull_heads": len(snap.pull_heads()),
                  "snapshot_digest": doc["digest"], "snapshot_created_at": doc["created_at"]})
        return EXIT_OK

    if a.cmd == "secgate":
        import tempfile
        from . import snapshot as frozen
        from .inventory import utcnow
        from .security import GateError, evaluate, external_scan_meta, run_scanner
        snapdoc = frozen.load(a.source_snapshot)
        src = GitSource(a.work_dir, a.repo_url)       # no fetch: the gate never looks at the remote
        frozen.check_local(src, snapdoc)
        if a.run_scanner:
            report = a.scan_report
            tmp = None
            if not report:
                tmp = tempfile.mkdtemp(prefix="drive-mirror-scan-")
                report = os.path.join(tmp, "gitleaks.json")
            try:
                scan = run_scanner(a.run_scanner, a.work_dir, sorted(set(frozen.commit_map(snapdoc).values())), report)
                doc = evaluate(src, report, a.approved, snapdoc, utcnow(), scan)
            finally:
                if tmp:
                    import shutil
                    shutil.rmtree(tmp, ignore_errors=True)
        else:
            if not a.scan_report:
                raise GateError("either --run-scanner or --scan-report is required")
            doc = evaluate(src, a.scan_report, a.approved, snapdoc, utcnow(), external_scan_meta(a.scan_report))
        src.close()
        with open(a.quarantine_out, "w", encoding="utf-8") as fh:
            json.dump(doc, fh, indent=1, ensure_ascii=False)
        _emit(a, doc, summary={"verdict": doc["verdict"], **doc["counts"], "scan_binding": doc["scan"]["binding"],
                               "snapshot_digest": doc["snapshot_digest"],
                               "approval_sha256": _sha256_file(a.quarantine_out),
                               "blocking_findings": [{k: f[k] for k in ("rule", "file", "line", "commit", "status")}
                                                     for f in doc["blocking_findings"]]})
        return EXIT_OK if doc["verdict"] == "PASS" else EXIT_GATE_BLOCKED

    if a.cmd == "lineage":
        from .lineage import build_lineage
        with open(a.inventory, encoding="utf-8") as fh:
            inv = json.load(fh)
        src = GitSource(a.work_dir, a.repo_url)
        src.ensure_repo()
        lin = build_lineage(src, inv)
        src.close()
        _emit(a, lin, summary={"branches": len(lin["branches"]), **lin["pr_summary"]})
        return EXIT_OK

    if a.cmd == "current-state":
        from .current_state import build_current_state
        with open(a.inventory, encoding="utf-8") as fh:
            inv = json.load(fh)
        with open(a.lineage, encoding="utf-8") as fh:
            lin = json.load(fh)
        with open(a.facts, encoding="utf-8") as fh:
            facts = json.load(fh)
        doc = build_current_state(inv, lin, facts)
        _emit(a, doc, summary={"schema": doc["schema"], "generated_at": doc["generated_at"],
                               "uncertainties": len(doc["unresolved_uncertainties"])})
        return EXIT_OK

    if a.cmd == "preflight":
        from .preflight import preflight
        rep = preflight(_drive(a), a.parent_folder_id, a.drive_id, a.expected_bytes, a.write_probe)
        _emit(a, rep)
        return {"PASS": EXIT_OK, "BLOCKED": EXIT_BLOCKED}.get(rep["result"], EXIT_FAIL)

    from .security import BINDING_TOOL, GateError, load_approval
    approval = load_approval(getattr(a, "quarantine_file", None), getattr(a, "approval_sha256", None))
    quarantine = approval.quarantine if approval else {}
    if a.cmd == "sync":
        # Fail closed BEFORE any Drive contact: sync uploads only what a gate approved.
        if approval is None or approval.snapshot is None:
            if not a.allow_ungated:
                raise GateError("sync needs an approved source snapshot: run `prepare --snapshot-out`, then `secgate "
                                "--source-snapshot ... --run-scanner ...`, and pass its output as --quarantine-file")
            approval = None
        elif approval.scan.get("binding") != BINDING_TOOL and not a.allow_external_scan_report:
            raise GateError("the approval's scan report was not produced by `secgate --run-scanner` on this snapshot")
    drive = _drive(a)
    git = GitSource(a.work_dir, a.repo_url)
    if a.cmd == "sync":
        from .sync import SafetyStop, SyncConfig, SyncRun
        cfg = SyncConfig(mirror_name=a.mirror_name, parent_folder_id=a.parent_folder_id, repo_url=a.repo_url,
                         mode=a.mode, include_tags=not a.no_tags, pull_heads=a.pull_heads, dry_run=a.dry_run,
                         time_budget_s=a.time_budget_min * 60 if a.time_budget_min else None, workers=a.workers,
                         max_retire_fraction=a.max_retire_fraction, allow_mass_retire=a.allow_mass_retire,
                         allow_empty_tree=a.allow_empty_tree, break_lock=a.break_lock,
                         lock_settle_s=a.lock_settle_s, github_repo=a.github_repo, quarantine=quarantine,
                         reconcile_after_hours=a.reconcile_after_hours, crawl_workers=a.crawl_workers,
                         approval=approval, allow_ungated=a.allow_ungated)
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
        rep = status(drive, git, a.mirror_name, a.parent_folder_id, not a.no_tags, a.max_age_hours, set(quarantine))
        _emit(a, rep)
        return EXIT_OK if rep["result"] == "PASS" else EXIT_FAIL

    if a.cmd == "verify":
        from .verify import verify
        only = None
        if a.sync_report:
            with open(a.sync_report, encoding="utf-8") as fh:
                srep = json.load(fh)
            only = {n["ref"] for n in srep.get("namespaces", []) if n.get("action") not in ("noop", "quarantined")}
            if srep.get("mode") == "reconcile" or srep.get("status") != "complete":
                only = None    # a reconcile (or an unfinished run) is verified in full
        frozen_snap = None
        if approval is not None and approval.snapshot is not None:
            from . import snapshot as frozen
            frozen.check_objects(git, approval.snapshot)      # verify the approved state; never fetch
            frozen_snap = frozen.to_ref_snapshot(approval.snapshot)
        rep = verify(drive, git, a.mirror_name, a.parent_folder_id, include_tags=not a.no_tags,
                     pull_heads=a.pull_heads, deep_sample=a.deep_sample, max_age_hours=a.max_age_hours,
                     only_refs=only, quarantine=quarantine, snapshot=frozen_snap)
        git.close()
        _emit(a, rep, summary={"result": rep["result"], "failing_namespaces": rep["failing_namespaces"][:20],
                               **{k: rep["global"].get(k) for k in ("expected_namespaces", "found_namespaces",
                                                                     "files_expected", "files_verified",
                                                                     "problems")}})
        return EXIT_OK if rep["result"] == "PASS" else EXIT_FAIL
    return EXIT_FAIL


def _sha256_file(path: str) -> str:
    import hashlib
    with open(path, "rb") as fh:
        return hashlib.sha256(fh.read()).hexdigest()


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

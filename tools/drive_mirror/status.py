"""Cheap staleness check: compares state.json with a fresh `git ls-remote`.

No crawl and no blob reads, so it is safe to run after every sync, including
partial ones, to detect a mirror that has stopped converging.
"""
from __future__ import annotations

import datetime as dt

from .gitsrc import GitSource
from .layout import STATE_FILE
from .sync import Mirror, SyncConfig, utcnow


def _age_h(ts: str | None) -> float | None:
    if not ts:
        return None
    t = dt.datetime.fromisoformat(ts.replace("Z", "+00:00"))
    return (dt.datetime.now(dt.timezone.utc) - t).total_seconds() / 3600


def status(drive, git: GitSource, mirror_name: str, parent_folder_id: str, include_tags: bool,
           max_age_hours: float) -> dict:
    rep = {"checked_at": utcnow(), "result": "PASS", "issues": []}
    m = Mirror(drive, SyncConfig(mirror_name=mirror_name, parent_folder_id=parent_folder_id, repo_url=git.repo_url))
    if not m.open(create=False):
        rep.update(result="FAIL", issues=["mirror root not found"])
        return rep
    state = m.read_meta(STATE_FILE) or {}
    git.ensure_repo()
    remote = git.ls_remote()
    want = dict(remote.heads)
    if include_tags:
        want.update(remote.tags_peeled)
    ns = state.get("namespaces", {})
    lc_age = _age_h((state.get("last_complete_run") or {}).get("finished_at"))
    rep["hours_since_last_complete_run"] = None if lc_age is None else round(lc_age, 2)
    rep["in_progress"] = sorted(r for r, v in ns.items() if v.get("status") != "complete")
    rep["missing"] = sorted(set(want) - set(ns))
    rep["behind"] = sorted(r for r in set(want) & set(ns) if ns[r].get("commit") != want[r])
    rep["extra"] = sorted(r for r in set(ns) - set(want) if not r.startswith("refs/pull/"))
    rep["last_run"] = state.get("last_run")
    if lc_age is None or lc_age > max_age_hours:
        rep["issues"].append(f"no complete run within {max_age_hours} h")
    stale = [r for r in rep["behind"] + rep["missing"] + rep["in_progress"]
             if (_age_h(ns.get(r, {}).get("synced_at")) or 1e9) > max_age_hours]
    if stale:
        rep["issues"].append(f"{len(stale)} namespaces not converged for more than {max_age_hours} h")
    rep["stale"] = stale[:50]
    if rep["issues"]:
        rep["result"] = "FAIL"
    return rep

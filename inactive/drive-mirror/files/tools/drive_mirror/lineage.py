"""Machine-readable branch lineage and PR-state verification.

Nothing here infers integration from PR labels, branch names or commit messages:
every relation is computed from commit ancestry in the fetched object database.
The only convention used is the *selection* of a "latest unified checkpoint"
(newest committer date among `checkpoint/wave*-unified-*`), which is recorded as
a convention and never promoted to an owner decision.

Output is deterministic for a given inventory + object database (sorted keys and
lists, no wall-clock values; `generated_at` is copied from the inventory).
"""
from __future__ import annotations

import datetime as dt
import re
import subprocess

from .gitsrc import GitSource

SCHEMA = "drive-mirror-lineage/1"
CHECKPOINT_RE = re.compile(r"^checkpoint/wave(\d+)-unified-\d+$")


def _count(src: GitSource, a: str, b: str) -> tuple[int, int]:
    left, right = src.run("rev-list", "--left-right", "--count", f"{a}...{b}").decode().split()
    return int(left), int(right)   # (only in a, only in b)


def _merge_base(src: GitSource, a: str, b: str) -> str | None:
    p = subprocess.run([src.git, "--git-dir", src.work_dir, "merge-base", a, b],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return p.stdout.decode().strip() or None


def _anc(src: GitSource, a: str | None, b: str | None) -> bool | None:
    if not a or not b:
        return None
    return src.is_ancestor(a, b)


def _ts(iso: str) -> float:
    """Committer dates carry their own UTC offsets; compare instants, not strings."""
    return dt.datetime.fromisoformat(iso.replace("Z", "+00:00")).timestamp()


def select_latest_checkpoint(inv: dict) -> dict | None:
    cands = [b for b in inv["branches"] if CHECKPOINT_RE.match(b["name"])]
    if not cands:
        return None
    return max(cands, key=lambda b: (_ts(b["commit"]["committed_at"]), b["name"]))


def build_lineage(src: GitSource, inv: dict) -> dict:
    branches = {b["name"]: b for b in inv["branches"]}
    default = inv["default_branch"].replace("refs/heads/", "")
    main_sha = inv["default_sha"]
    cp = select_latest_checkpoint(inv)
    cp_sha = cp["sha"] if cp else None
    head_of = {n: b["sha"] for n, b in branches.items()}

    out_branches = []
    for name in sorted(branches):
        b = branches[name]
        sha = b["sha"]
        mb_main = _merge_base(src, main_sha, sha)
        behind_main, ahead_main = _count(src, main_sha, sha)
        rec = {
            "name": name, "sha": sha, "committed_at": b["commit"]["committed_at"],
            "merge_base_with_main": mb_main,
            "ahead_of_main": ahead_main, "behind_main": behind_main,
            "contained_in_main": ahead_main == 0,
            "nearest_ancestor_branch": b.get("nearest_ancestor_branch"),
            "commits_over_nearest_ancestor": b.get("commits_over_nearest_ancestor"),
            "identical_head_branches": b.get("identical_head_branches", []),
            "descendant_branch_count": sum(1 for o in branches.values() if o["name"] != name and name in
                                           [x for x in [o.get("nearest_ancestor_branch")] if x]),
            "contained_in_branch_count": len(b.get("head_contained_in_branches", [])) if "head_contained_in_branches" in b
            else b.get("head_contained_in_branch_count"),
            "open_prs_as_head": sorted(b.get("open_prs_as_head", [])),
            "prs_as_head": sorted(b.get("prs_as_head", [])),
            "ci_on_head": {r["workflow"]: (r["conclusion"] or r["status"]) for r in (b.get("ci_runs_on_head") or [])},
        }
        if cp_sha:
            mb_cp = _merge_base(src, cp_sha, sha)
            behind_cp, ahead_cp = _count(src, cp_sha, sha)
            rec["relation_to_latest_checkpoint"] = (
                "same" if sha == cp_sha else
                "descendant" if behind_cp == 0 else
                "ancestor" if ahead_cp == 0 else "diverged")
            rec["merge_base_with_latest_checkpoint"] = mb_cp
            rec["ahead_of_latest_checkpoint"] = ahead_cp
            rec["behind_latest_checkpoint"] = behind_cp
        out_branches.append(rec)

    prs = []
    for p in sorted(inv.get("pull_requests", []), key=lambda p: p["number"]):
        state = "merged" if p["merged"] else p["state"]
        head_sha = p["head_sha"]
        base_ref = p["base_ref"]
        rec = {
            "number": p["number"], "state": state, "draft": p.get("draft"),
            "head_ref": p["head_ref"], "head_sha": head_sha, "base_ref": base_ref,
            "head_branch_exists": p["head_ref"] in branches,
            "head_branch_current_sha": head_of.get(p["head_ref"]),
            "head_branch_matches_pr_head": head_of.get(p["head_ref"]) == head_sha,
            "base_branch_exists": base_ref in branches,
            "base_branch_current_sha": head_of.get(base_ref),
            "merge_commit_sha": p.get("merge_commit_sha") if p["merged"] else None,
            "merged_at": p.get("merged_at"),
            "head_object_available": src.has_object(head_sha),
        }
        if rec["head_object_available"]:
            rec["head_in_main"] = _anc(src, head_sha, main_sha)
            rec["head_in_latest_checkpoint"] = _anc(src, head_sha, cp_sha)
            rec["head_in_base_branch_now"] = _anc(src, head_sha, head_of.get(base_ref))
        if p["merged"]:
            mc = p.get("merge_commit_sha")
            have = bool(mc) and src.has_object(mc)
            rec["merge_commit_object_available"] = have
            # A merge/squash commit is evidence the PR reached its base *at merge time*; whether it
            # is still part of main / the checkpoint / the base branch today is computed separately.
            rec["merge_commit_in_main"] = _anc(src, mc, main_sha) if have else None
            rec["merge_commit_in_latest_checkpoint"] = _anc(src, mc, cp_sha) if have else None
            rec["merge_commit_in_base_branch_now"] = _anc(src, mc, head_of.get(base_ref)) if have else None
            reached_main = rec.get("merge_commit_in_main") or rec.get("head_in_main")
            reached_cp = rec.get("merge_commit_in_latest_checkpoint") or rec.get("head_in_latest_checkpoint")
            rec["reached_main"] = (True if reached_main else (None if not (have or rec["head_object_available"])
                                                              else False))
            rec["reached_latest_checkpoint"] = (True if reached_cp else (None if not (have or rec["head_object_available"])
                                                                         else False))
        prs.append(rec)

    merged = [r for r in prs if r["state"] == "merged"]
    summary = {
        "pull_requests": len(prs),
        "by_state": _tally(r["state"] for r in prs),
        "merged_to_main_base": sum(1 for r in merged if r["base_ref"] == default),
        "merged_prs_whose_work_is_in_main": sum(1 for r in merged if r.get("reached_main") is True),
        "merged_prs_whose_work_is_not_in_main": sum(1 for r in merged if r.get("reached_main") is False),
        "merged_prs_unknown_reachability": sum(1 for r in merged if r.get("reached_main") is None),
        "merged_prs_in_latest_checkpoint": sum(1 for r in merged if r.get("reached_latest_checkpoint") is True),
        "open_prs_with_deleted_head_branch": sum(1 for r in prs if r["state"] == "open" and not r["head_branch_exists"]),
        "open_prs_with_deleted_base_branch": sum(1 for r in prs if r["state"] == "open" and not r["base_branch_exists"]),
        "open_prs_whose_head_moved_since_pr_head": sum(
            1 for r in prs if r["state"] == "open" and r["head_branch_exists"] and not r["head_branch_matches_pr_head"]),
    }
    return {
        "schema": SCHEMA,
        "generated_at": inv["generated_at"],
        "repo_url": inv["repo_url"],
        "sources": {
            "default_branch": default, "default_sha": main_sha,
            "latest_unified_checkpoint": ({"name": cp["name"], "sha": cp_sha, "committed_at": cp["commit"]["committed_at"],
                                           "selection": "newest committer date among checkpoint/wave*-unified-*; "
                                                        "a convention, NOT an owner declaration"} if cp else None),
            "inventory_generated_at": inv["generated_at"],
        },
        "checkpoints": sorted(
            ({"name": b["name"], "sha": b["sha"], "committed_at": b["commit"]["committed_at"]}
             for b in inv["branches"] if b["name"].startswith("checkpoint/")), key=lambda x: _ts(x["committed_at"])),
        "branches": out_branches,
        "pull_requests": prs,
        "pr_summary": summary,
    }


def _tally(it) -> dict:
    d: dict[str, int] = {}
    for x in it:
        d[x] = d.get(x, 0) + 1
    return dict(sorted(d.items()))

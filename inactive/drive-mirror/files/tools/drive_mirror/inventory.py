"""Machine-readable inventory of every remote ref, its tree, PRs and CI state."""
from __future__ import annotations

import collections
import datetime as dt
import json

from .gitsrc import GitSource
from .github_meta import GitHubMeta


def utcnow() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def build_inventory(src: GitSource, github: GitHubMeta | None, include_ci: bool = True,
                    files_out=None, analyse_pull_heads: bool = True) -> dict:
    snap = src.snapshot(include_tags=True)
    if analyse_pull_heads:
        # Objects only (never mirrored as namespaces by default); needed to find PR heads
        # whose commits no branch contains.
        src.run("fetch", "--quiet", "--no-tags", "origin", "+refs/pull/*/head:refs/inventory-pulls/*")
    default_ref = snap.head_symref or "refs/heads/main"
    default_sha = snap.heads.get(default_ref)
    limitations: list[str] = []

    pulls: list[dict] = []
    workflows: list[dict] = []
    if github:
        try:
            pulls = github.pulls()
            workflows = github.workflows()
        except Exception as e:  # recorded, never silently dropped
            limitations.append(f"GitHub API metadata unavailable: {e}")
            github = None
    else:
        limitations.append("GitHub API metadata not requested (no --github-repo)")

    prs_by_head = collections.defaultdict(list)
    prs_by_base = collections.defaultdict(list)
    for p in pulls:
        if p["head_repo"] == (github.repo if github else None):
            prs_by_head[p["head_ref"]].append(p["number"])
        prs_by_base[p["base_ref"]].append(p["number"])

    pr_numbers_open = {p["number"] for p in pulls if p["state"] == "open"}
    branches = []
    totals = collections.Counter()
    unique_blobs: set[str] = set()
    for ref, sha in sorted(snap.heads.items()):
        name = ref[len("refs/heads/"):]
        meta = src.commit_meta(sha)
        entries = src.tree_entries(sha)
        modes = collections.Counter(e.mode for e in entries.values())
        size = sum(e.size or 0 for e in entries.values())
        unique_blobs.update(e.oid for e in entries.values() if e.type == "blob")
        totals["files"] += len(entries)
        totals["bytes"] += size
        ahead = behind = None
        merged = None
        if default_sha:
            out = src.run("rev-list", "--left-right", "--count", f"{default_sha}...{sha}").decode().split()
            behind, ahead = int(out[0]), int(out[1])
            merged = ahead == 0
        history_size = int(src.run("rev-list", "--count", sha).decode())
        contained_in = [r[len("refs/mirror/heads/"):] for r in
                        src.run("for-each-ref", "--contains", sha, "--format=%(refname)",
                                "refs/mirror/heads/").decode().split()
                        if r != "refs/mirror/heads/" + name]
        rec = {
            "name": name, "ref": ref, "sha": sha, "tree": meta["tree"],
            "commit": {k: meta[k] for k in ("authored_at", "committed_at", "author", "subject", "parents")},
            "file_count": len(entries), "bytes": size, "modes": dict(modes),
            "gitlinks": [e.path for e in entries.values() if e.mode == "160000"],
            "symlinks": [e.path for e in entries.values() if e.mode == "120000"],
            "ahead_of_default": ahead, "behind_default": behind, "merged_into_default": merged,
            "history_commits": history_size,
            "head_contained_in_branches": contained_in,
            "prs_as_head": prs_by_head.get(name, []),
            "open_prs_as_head": [n for n in prs_by_head.get(name, []) if n in pr_numbers_open],
            "prs_as_base": prs_by_base.get(name, []),
        }
        if include_ci and github:
            try:
                runs = github.runs_for_sha(sha)
                latest = {}
                for r in sorted(runs, key=lambda r: r["created_at"]):
                    latest[r["workflow"]] = r
                rec["ci_runs_on_head"] = [{"workflow": w, "event": r["event"], "status": r["status"],
                                           "conclusion": r["conclusion"], "run_id": r["id"]}
                                          for w, r in sorted(latest.items())]
            except Exception as e:
                rec["ci_runs_on_head"] = None
                limitations.append(f"CI runs for {name} unavailable: {e}")
        branches.append(rec)
        if files_out is not None:
            for e in sorted(entries.values(), key=lambda e: e.path):
                files_out.write(json.dumps({
                    "branch": name, "path": e.path, "mode": e.mode, "type": e.type, "oid": e.oid,
                    "size": e.size, "sha256": src.blob_sha256(e.oid) if e.type == "blob" else None,
                }, ensure_ascii=False) + "\n")

    # Nearest ancestor branch: among branches whose head is an ancestor of this head
    # (and differs from it), the one with the largest history.
    by_name = {b["name"]: b for b in branches}
    for b in branches:
        cands = [a for a in branches if a["name"] != b["name"] and a["sha"] != b["sha"]
                 and b["name"] in a["head_contained_in_branches"]]
        best = max(cands, key=lambda a: a["history_commits"], default=None)
        b["nearest_ancestor_branch"] = best["name"] if best else None
        b["commits_over_nearest_ancestor"] = (b["history_commits"] - best["history_commits"]) if best else None
        b["identical_head_branches"] = [a["name"] for a in branches if a["name"] != b["name"] and a["sha"] == b["sha"]]
    del by_name

    pull_orphans = []
    if analyse_pull_heads:
        for ref, sha in sorted(snap.pulls.items(), key=lambda kv: int(kv[0].split("/")[2])):
            if not ref.endswith("/head"):
                continue
            hit = src.run("for-each-ref", "--contains", sha, "--count=1", "--format=%(refname)",
                          "refs/mirror/heads/").decode().strip()
            if not hit:
                n = int(ref.split("/")[2])
                p = next((x for x in pulls if x["number"] == n), {})
                pull_orphans.append({"number": n, "ref": ref, "sha": sha,
                                     "pr_state": ("merged" if p.get("merged") else p.get("state")),
                                     "head_ref": p.get("head_ref"), "title": p.get("title")})

    tags = [{"ref": t, "object": snap.tags[t], "commit": snap.tags_peeled[t],
             "annotated": snap.tags[t] != snap.tags_peeled[t]} for t in sorted(snap.tags)]

    return {
        "schema": "drive-mirror-inventory/1",
        "generated_at": utcnow(),
        "repo_url": src.repo_url,
        "default_branch": default_ref,
        "default_sha": default_sha,
        "counts": {
            "branches": len(snap.heads), "tags": len(snap.tags), "pull_refs": len(snap.pulls),
            "other_refs": len(snap.other), "pull_requests": len(pulls),
            "pull_requests_by_state": dict(collections.Counter(
                "merged" if p["merged"] else p["state"] for p in pulls)),
            "tracked_file_entries_all_branches": totals["files"],
            "tracked_bytes_all_branches": totals["bytes"],
            "unique_blobs": len(unique_blobs),
        },
        "branches": branches,
        "tags": tags,
        "pull_refs": dict(sorted(snap.pulls.items())),
        "pull_heads_not_in_any_branch": pull_orphans,
        "other_refs": snap.other,
        "pull_requests": pulls,
        "workflows_registered": workflows,
        "limitations": limitations,
    }

#!/usr/bin/env python3
"""Reproducible, SHA-backed branch cleanup manifest.

Every git fact is computed at run time from the local clone; only the provenance classification and its evidence text
come from docs/audits/wave13/branch_classification.json (written by hand after inspecting each branch).

usage: cleanup_manifest.py --checkpoint origin/checkpoint/wave12-unified-20261027 [--json out.json] [--md out.md]
Run `git fetch origin '+refs/heads/*:refs/remotes/origin/*' '+refs/pull/*/head:refs/remotes/pr/*' --prune` first.
Deletes nothing and never writes to the repository except the two output files.
"""
import argparse, collections, json, os, subprocess, sys

def git(*a):
    return subprocess.check_output(["git", *a], text=True).strip()

ap = argparse.ArgumentParser()
ap.add_argument("--checkpoint", required=True)
ap.add_argument("--classification", default="docs/audits/wave13/branch_classification.json")
ap.add_argument("--prs", default="docs/audits/wave13/pr_snapshot_2026-10-27.json")
ap.add_argument("--json"); ap.add_argument("--md")
ap.add_argument("--extra-retain", default="", help="comma list of new branch names to class as active dependency")
args = ap.parse_args()

cp_sha = git("rev-parse", args.checkpoint)
cls = json.load(open(args.classification))
prs = json.load(open(args.prs))
open_prs = [p for p in prs if p["state"] == "open"]
heads = collections.defaultdict(list)
for p in prs: heads[p["head"]].append(p["number"])

refs = {}
for line in git("for-each-ref", "--format=%(objectname) %(refname)", "refs/remotes/origin", "refs/remotes/pr").splitlines():
    sha, ref = line.split(" ", 1)
    if ref.endswith("/HEAD"): continue
    refs[ref] = sha
branches = {r[len("refs/remotes/origin/"):]: s for r, s in refs.items() if r.startswith("refs/remotes/origin/")}

CHECKPOINTISH = lambda b: b == "main" or b.startswith(("stage-", "checkpoint/", "release/"))
DEL, ACT, CPC, UNP, UNR, INC = ("DELETE ELIGIBLE", "RETAIN — ACTIVE DEPENDENCY", "RETAIN — CHECKPOINT",
                                "RETAIN — UNPRESERVED FUNCTIONALITY", "RETAIN — UNRESOLVED AUTHORITY", "RETAIN — AUDIT INCOMPLETE")
extra = {x for x in args.extra_retain.split(",") if x}

rows = []
for name, sha in sorted(branches.items()):
    own = "refs/remotes/origin/" + name
    others = [r for r in refs if r != own]
    orphan = git("rev-list", own, "--not", *others).split()
    ahead = int(git("rev-list", "--count", f"{args.checkpoint}..{own}"))
    behind = int(git("rev-list", "--count", f"{own}..{args.checkpoint}"))
    cherry = git("cherry", args.checkpoint, own).splitlines()
    pu = sum(1 for l in cherry if l.startswith("+")); pe = sum(1 for l in cherry if l.startswith("-"))
    roles = []
    for p in open_prs:
        if p["head"] == name: roles.append(f"head of open #{p['number']}")
        if p["base"] == name: roles.append(f"base of open #{p['number']}")
    c = cls.get(name)
    if c is None:
        provenance = "INTEGRATED (ancestor of checkpoint)" if ahead == 0 else "UNCLASSIFIED"
        evidence = "Every commit is an ancestor of the checkpoint." if ahead == 0 else "No classification on file."
        base = None
    else:
        provenance, evidence, base = c["provenance"], c["evidence"], c["class_wave12"]
    # Precedence: checkpoints first; an existing specific RETAIN class stands (the open-PR role is shown in its own column);
    # then any open-PR role or explicit extra makes it an active dependency; otherwise the recorded class, else derive.
    if CHECKPOINTISH(name) or name == "integration/wave10-unified-20261025" or sha == cp_sha: verdict = CPC
    elif base is not None and base.startswith("RETAIN") and base != ACT: verdict = base
    elif name in extra or roles: verdict = ACT
    elif base is not None: verdict = base
    elif ahead == 0: verdict = DEL
    else: verdict = INC
    # a DELETE ELIGIBLE branch must not carry unreconciled content
    if verdict == DEL and provenance == "UNCLASSIFIED": verdict = INC
    rows.append(dict(branch=name, sha=sha, prs=heads.get(name, []), ahead=ahead, behind=behind, patch_unique=pu,
                     patch_equivalent=pe, open_pr_roles=roles, orphaned_commits_if_deleted=len(orphan),
                     orphaned_commit_shas=orphan, provenance=provenance, verdict=verdict, evidence=evidence,
                     tier=("0 commits orphaned" if verdict == DEL and not orphan else
                           f"{len(orphan)} commit(s) orphaned; content preserved" if verdict == DEL else "")))

pr_only = []
for ref, sha in sorted(refs.items(), key=lambda kv: (len(kv[0]), kv[0])):
    if not ref.startswith("refs/remotes/pr/"): continue
    n = int(ref.rsplit("/", 1)[1])
    if any(r["sha"] == sha for r in rows) or subprocess.run(["git", "merge-base", "--is-ancestor", sha, cp_sha]).returncode == 0:
        continue
    on_branch = [b for b, s in branches.items() if subprocess.run(["git", "merge-base", "--is-ancestor", sha, s]).returncode == 0]
    if not on_branch: pr_only.append(dict(pr=n, sha=sha))

doc = dict(checkpoint=args.checkpoint, checkpoint_sha=cp_sha, branches=len(rows), pr_refs=sum(1 for r in refs if "/pr/" in r),
           summary=collections.Counter(r["verdict"] for r in rows),
           delete_eligible_zero_orphans=sum(1 for r in rows if r["verdict"] == DEL and not r["orphaned_commits_if_deleted"]),
           delete_eligible_with_orphans=sum(1 for r in rows if r["verdict"] == DEL and r["orphaned_commits_if_deleted"]),
           pr_heads_kept_only_by_refs_pull=pr_only, rows=rows)
if args.json: json.dump(doc, open(args.json, "w"), indent=1, ensure_ascii=False)
if args.md:
    L = [f"# Branch cleanup manifest — checkpoint `{cp_sha[:10]}`\n",
         f"Generated by `tools/branch-audit/cleanup_manifest.py` from `{args.checkpoint}`. Nothing was deleted or closed.\n",
         "Every row is read from the remote at generation time. The branch that carries this file necessarily moves when this file is committed, "
         "so its own row is always one commit behind; every other row can be re-verified with the script.\n",
         "| Disposition | Count |", "|---|---:|"]
    for k, v in doc["summary"].most_common(): L.append(f"| {k} | {v} |")
    L.append(f"| **Total branches** | **{len(rows)}** |\n")
    L.append(f"DELETE ELIGIBLE splits into **{doc['delete_eligible_zero_orphans']}** branches whose deletion orphans zero commits and "
             f"**{doc['delete_eligible_with_orphans']}** whose deletion would make some commits unreachable (content preserved elsewhere).\n")
    for v in (DEL, ACT, CPC, UNP, UNR, INC):
        sel = [r for r in rows if r["verdict"] == v]
        L.append(f"## {v} ({len(sel)})\n")
        if not sel: L.append("None.\n"); continue
        L.append("| Branch | HEAD | PR(s) | +/− vs checkpoint | Open-PR role | Orphans if deleted | Provenance | Evidence |")
        L.append("|---|---|---|---|---|---|---|---|")
        for r in sel:
            o = r["orphaned_commits_if_deleted"]
            L.append(f"| `{r['branch']}` | `{r['sha'][:10]}` | {', '.join('#%d' % x for x in r['prs']) or '—'} | +{r['ahead']}/−{r['behind']} | "
                     f"{'; '.join(r['open_pr_roles']) or '—'} | {o}{' (' + ', '.join(c[:8] for c in r['orphaned_commit_shas'][:4]) + ')' if o else ''} | "
                     f"{r['provenance']} | {r['evidence'].replace('|', '/')} |")
        L.append("")
    L.append("## PR heads that exist only as `refs/pull/N/head` (their branches are already gone)\n")
    L.append("| PR | Head SHA |"); L.append("|---:|---|")
    for x in pr_only: L.append(f"| #{x['pr']} | `{x['sha']}` |")
    open(args.md, "w").write("\n".join(L) + "\n")
print(json.dumps({k: doc[k] for k in ("checkpoint_sha", "branches", "summary", "delete_eligible_zero_orphans", "delete_eligible_with_orphans")}, default=dict))

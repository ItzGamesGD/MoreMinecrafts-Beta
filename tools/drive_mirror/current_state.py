"""Concise machine-readable CURRENT_STATE index for AI-assisted repository exploration.

Everything structural (branches, stacks, PR relations, CI, containment) is *computed* from the
inventory and lineage reports. Everything that needs judgement (conflicts, owner decisions,
named failures with their evidence) comes from a curated `facts` file whose every entry carries
evidence references. The generator adds no claims of its own.

Deterministic: same inputs -> byte-identical output (no wall clock, sorted keys and lists).
It never states that one branch is "the" integrated project state: that is reported as NOT
ESTABLISHED, with the candidates and the evidence that distinguishes them.
"""
from __future__ import annotations

SCHEMA = "moreminecrafts-current-state/1"

HOW_TO_USE = [
    "This file is a map, not design authority. Design authority is routed by docs/CONTENT_AUTHORITY_INDEX.md and "
    "docs/STRUCTURE_AUTHORITY_INDEX.md on the branch you are reading (see AGENTS.md).",
    "Every SHA below is a point-in-time value from `source_refs.inventory_generated_at`. Branch heads move; re-run "
    "`python3 -m drive_mirror inventory|lineage|current-state` before relying on one.",
    "A PR being merged, or a document existing, is not evidence that work reached main or the latest checkpoint; use "
    "`pull_requests.*.reached_*` fields, which are computed from commit ancestry.",
    "Treat `facts` entries as curated statements: each lists its evidence. Treat `computed` sections as derived.",
]


def _failing(ci: dict) -> dict:
    return {w: c for w, c in sorted(ci.items()) if c in ("failure", "cancelled", "timed_out", "startup_failure")}


def build_current_state(inv: dict, lin: dict, facts: dict) -> dict:
    default = lin["sources"]["default_branch"]
    cp = lin["sources"]["latest_unified_checkpoint"]
    branches = {b["name"]: b for b in lin["branches"]}

    on_cp = {n for n, b in branches.items() if b.get("relation_to_latest_checkpoint") in ("descendant", "same")}
    children: dict[str, list[str]] = {}
    for n in on_cp:
        parent = branches[n].get("nearest_ancestor_branch")
        children.setdefault(parent, []).append(n)
    roots = sorted(n for n in on_cp if n != (cp or {}).get("name")
                   and branches[n].get("nearest_ancestor_branch") not in on_cp - {(cp or {}).get("name")})

    def collect(n: str) -> list[str]:
        out = [n]
        for c in sorted(children.get(n, [])):
            out += collect(c)
        return out

    stacks = []
    for r in roots:
        members = collect(r)
        tips = sorted(m for m in members if not children.get(m))
        stacks.append({
            "root": r, "branch_count": len(members), "tips": tips,
            "members": sorted(members, key=lambda m: (branches[m]["committed_at"], m)),
            "open_prs": sorted({p for m in members for p in branches[m]["open_prs_as_head"]}),
            "tips_with_failing_ci": sorted(t for t in tips if _failing(branches[t]["ci_on_head"])),
            "tips_without_any_ci_run": sorted(t for t in tips if not branches[t]["ci_on_head"]),
        })
    stacks.sort(key=lambda s: s["root"])
    in_stacks = {m for s in stacks for m in s["members"]}

    branch_rows = []
    for n in sorted(branches):
        b = branches[n]
        branch_rows.append({
            "name": n, "sha": b["sha"], "committed_at": b["committed_at"],
            "relation_to_latest_checkpoint": b.get("relation_to_latest_checkpoint"),
            "ahead_of_main": b["ahead_of_main"], "contained_in_main": b["contained_in_main"],
            "nearest_ancestor_branch": b["nearest_ancestor_branch"],
            "open_prs": b["open_prs_as_head"], "prs": b["prs_as_head"],
            "failing_ci": _failing(b["ci_on_head"]), "ci_run_count": len(b["ci_on_head"]),
        })

    open_prs = [{k: p[k] for k in ("number", "head_ref", "head_sha", "base_ref", "draft", "head_in_main",
                                   "head_in_latest_checkpoint", "head_in_base_branch_now")}
                for p in lin["pull_requests"] if p["state"] == "open"]
    stale_refs = [{k: p[k] for k in ("number", "state", "head_ref", "base_ref", "head_branch_exists",
                                     "base_branch_exists")}
                  for p in lin["pull_requests"] if p["state"] != "open" and
                  (not p["head_branch_exists"] or not p["base_branch_exists"])]
    merged_not_in_main = sorted(p["number"] for p in lin["pull_requests"]
                                if p["state"] == "merged" and p.get("reached_main") is False)

    cp_rec = branches.get((cp or {}).get("name")) if cp else None
    main_rec = branches[default]
    computed = {
        "integration": {
            "main": {"sha": main_rec["sha"], "committed_at": main_rec["committed_at"],
                     "commits_behind_latest_checkpoint": main_rec.get("behind_latest_checkpoint"),
                     "latest_merge_into_main_via_pr": max(
                         (p["merged_at"] for p in lin["pull_requests"]
                          if p["state"] == "merged" and p["base_ref"] == default and p.get("merged_at")), default=None)},
            "latest_unified_checkpoint": ({**cp, "commits_ahead_of_main": cp_rec["ahead_of_main"],
                                           "ci_on_head": cp_rec["ci_on_head"]} if cp and cp_rec else None),
            "branches_descending_from_latest_checkpoint": len(on_cp) - 1,
            "branches_contained_in_main": sum(1 for b in branches.values() if b["contained_in_main"]),
            "integrated_project_state": "NOT ESTABLISHED: no single branch is verified to represent the whole "
                                        "project. main is stale relative to the checkpoint; the checkpoint predates "
                                        "the stacks listed under `stacks`. Owner decision D-1 selects the baseline.",
        },
        "stacks_on_latest_checkpoint": stacks,
        "branches_not_descending_from_latest_checkpoint_and_not_in_main": sorted(
            n for n, b in branches.items() if n not in on_cp and not b["contained_in_main"]),
        "pull_requests": {"summary": lin["pr_summary"], "open": sorted(open_prs, key=lambda p: p["number"]),
                          "closed_or_merged_with_missing_branch": sorted(stale_refs, key=lambda p: p["number"]),
                          "merged_but_not_in_main": merged_not_in_main},
        "pr_only_refs": {"heads_not_in_any_branch": [
            {k: x[k] for k in ("number", "sha", "pr_state", "head_ref")} for x in inv.get("pull_heads_not_in_any_branch", [])]},
        "heads_with_failing_ci": [{"branch": n, "sha": b["sha"], "failing": _failing(b["ci_on_head"])}
                                  for n, b in sorted(branches.items()) if _failing(b["ci_on_head"])],
        "branches": branch_rows,
        "unlisted_branch_count": len(set(branches) - in_stacks - {default}),
    }

    uncertainties = list(facts.get("unresolved_uncertainties", []))
    no_ci = sorted(n for n, b in branches.items() if not b["ci_on_head"] and n in in_stacks)
    if no_ci:
        uncertainties.append({"id": "U-CI", "statement": "Branches in the active stacks with no GitHub Actions run on "
                              "their head; their validation claims cannot be checked from CI evidence.",
                              "branches": no_ci})
    uncertainties.append({"id": "U-TIME", "statement": "Heads move continuously; this index is only valid for "
                          "`source_refs.inventory_generated_at`."})

    return {
        "schema": SCHEMA,
        "generated_at": inv["generated_at"],
        "repository": inv["repo_url"],
        "purpose": "Orientation map for AI-assisted exploration of the branch/PR landscape. Not design authority.",
        "how_to_use": HOW_TO_USE,
        "scope": {
            "covers": ["all refs/heads/* branches", "all pull requests (state, base/head, ancestry-derived "
                       "reachability)", "GitHub Actions conclusions on each branch head", "refs/pull/*/head commits "
                       "contained in no branch"],
            "does_not_cover": ["gameplay/design correctness", "live (in-game) validation", "contents of authority "
                               "documents", "refs other than branches and PR heads (the repository has no tags)",
                               "full commit history (only head commits and ancestry relations)"],
        },
        "source_refs": {
            "default_branch": {"name": default, "sha": main_rec["sha"]},
            "latest_unified_checkpoint": cp,
            "inventory_generated_at": inv["generated_at"], "lineage_generated_at": lin["generated_at"],
            "branch_count": len(branches), "pull_request_count": len(lin["pull_requests"]),
            "pull_ref_count": len(inv.get("pull_refs", {})),
        },
        "computed": computed,
        "facts": {k: facts[k] for k in ("conflicts", "known_failures", "owner_decisions_required",
                                        "security", "documentation_state", "mirror") if k in facts},
        "unresolved_uncertainties": uncertainties,
    }

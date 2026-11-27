#!/usr/bin/env python3
"""Compare two complete local Git trees against a supplied common base."""

import argparse
import json
import subprocess
import sys
from pathlib import Path


def git(*args):
    result = subprocess.run(["git", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise ValueError(result.stderr.decode(errors="replace").strip() or f"git {' '.join(args)} failed")
    return result.stdout


def tree(ref):
    # -z avoids Git's path quoting, and recursive traversal has no compare API cap.
    entries = {}
    for record in git("ls-tree", "-r", "-z", "--full-tree", ref).split(b"\0"):
        if not record:
            continue
        metadata, path = record.split(b"\t", 1)
        mode, kind, oid = metadata.decode("ascii").split(" ")
        entries[path.decode("utf-8", "surrogateescape")] = (mode, kind, oid)
    return entries


def verify_objects(*trees):
    objects = sorted({entry[2] for entries in trees for entry in entries.values() if entry[1] != "commit"})
    if not objects:
        return
    result = subprocess.run(["git", "cat-file", "--batch-check=%(objectname) %(objecttype)"],
                            input=("\n".join(objects) + "\n").encode("ascii"),
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise ValueError(result.stderr.decode(errors="replace").strip() or "Git object verification failed")
    missing = [line.decode("ascii") for line in result.stdout.splitlines() if line.endswith(b" missing")]
    if missing:
        raise ValueError(f"missing local objects: {', '.join(missing[:10])}")


def rename_hazards(base, ref, side):
    fields = git("diff", "--name-status", "-z", "-M", base, ref).split(b"\0")
    renames = []
    index = 0
    while index < len(fields) and fields[index]:
        status = fields[index].decode("ascii")
        index += 1
        if status.startswith(("R", "C")):
            old = fields[index].decode("utf-8", "surrogateescape")
            new = fields[index + 1].decode("utf-8", "surrogateescape")
            renames.append({"side": side, "from": old, "to": new, "status": status})
            index += 2
        else:
            index += 1
    return renames


def analyze(base, a, b):
    report = {"base": base, "a": a, "b": b, "a_only": [], "b_only": [],
              "identical_shared": [], "differing_shared": [],
              "structural_hazards": {"deletions": [], "mode_changes": [], "renames": []},
              "errors": []}
    try:
        if git("rev-parse", "--is-shallow-repository").strip() == b"true":
            raise ValueError("repository is shallow; complete history is unavailable")
        for ref in (base, a, b):
            git("cat-file", "-e", f"{ref}^{{commit}}")
        for ref in (a, b):
            git("merge-base", "--is-ancestor", base, ref)
        base_tree, a_tree, b_tree = (tree(ref) for ref in (base, a, b))
        verify_objects(base_tree, a_tree, b_tree)
        for path in sorted(set(base_tree) | set(a_tree) | set(b_tree)):
            before, left, right = (items.get(path) for items in (base_tree, a_tree, b_tree))
            changed_a, changed_b = left != before, right != before
            if not (changed_a or changed_b):
                continue
            if changed_a and changed_b:
                report["identical_shared" if left == right else "differing_shared"].append(path)
            else:
                report["a_only" if changed_a else "b_only"].append(path)
            if before and ((changed_a and left is None) or (changed_b and right is None)):
                report["structural_hazards"]["deletions"].append(path)
            if before and any(endpoint and endpoint[0] != before[0]
                              for endpoint, changed in ((left, changed_a), (right, changed_b)) if changed):
                report["structural_hazards"]["mode_changes"].append(path)
        report["structural_hazards"]["renames"] = sorted(
            rename_hazards(base, a, "a") + rename_hazards(base, b, "b"),
            key=lambda item: (item["side"], item["from"], item["to"]))
    except ValueError as exc:
        report["errors"].append(f"Incomplete local Git analysis: {exc}. Fetch complete commits and history, then retry.")
    report["requires_review"] = bool(report["differing_shared"] or
                                     any(report["structural_hazards"].values()) or report["errors"])
    return report


def markdown(report):
    lines = ["# Integration manifest", "", f"Base: `{report['base']}`  ",
             f"A: `{report['a']}`  ", f"B: `{report['b']}`", "",
             f"Review required: **{'yes' if report['requires_review'] else 'no'}**", ""]
    for key, title in (("a_only", "A-only changes"), ("b_only", "B-only changes"),
                       ("identical_shared", "Identical shared endpoints"),
                       ("differing_shared", "Differing shared endpoints; semantic review required")):
        lines.extend((f"## {title} ({len(report[key])})", ""))
        lines.extend(f"- `{path}`" for path in report[key])
        lines.append("")
    for key, title in (("deletions", "Deletions"), ("mode_changes", "Mode changes"),
                       ("renames", "Renames or copies")):
        values = report["structural_hazards"][key]
        lines.extend((f"## {title} ({len(values)})", ""))
        lines.extend(f"- `{v}`" if isinstance(v, str) else
                     f"- {v['side']}: `{v['from']}` → `{v['to']}` ({v['status']})" for v in values)
        lines.append("")
    lines.extend(("## Errors", ""))
    lines.extend(f"- {error}" for error in report["errors"])
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("base", "a", "b"):
        parser.add_argument(name, help="commit SHA or locally available commit ref")
    parser.add_argument("--json-report", type=Path, required=True)
    parser.add_argument("--markdown-report", type=Path, required=True)
    args = parser.parse_args()
    report = analyze(args.base, args.a, args.b)
    args.json_report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    args.markdown_report.write_text(markdown(report))
    return 2 if report["requires_review"] else 0


if __name__ == "__main__":
    sys.exit(main())

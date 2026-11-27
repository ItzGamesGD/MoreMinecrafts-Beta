#!/usr/bin/env python3
"""Check annotated Fabric GameTests against the declared entry points."""

import argparse
import json
import re
import sys
from pathlib import Path


PACKAGE = re.compile(r"\bpackage\s+([\w.]+)\s*;")
CLASS = re.compile(r"\bclass\s+(\w+)\b")
METHOD = re.compile(r"@GameTest\b(?:\s*\([^)]*\))?\s*(?:public|protected|private)?\s*(?:static\s+)?void\s+(\w+)\s*\(", re.S)


def without_comments(source):
    # Preserve quoted text while masking comments, including annotation-like examples.
    pattern = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
    return pattern.sub(lambda match: " " * len(match.group()) if match.group().startswith(("//", "/*")) else match.group(), source)


def discover(root, errors):
    classes = {}
    source_root = root / "src/gametest/java"
    if not source_root.is_dir():
        errors.append(f"Missing GameTest source directory: {source_root}")
        return classes
    for path in sorted(source_root.rglob("*.java")):
        source = without_comments(path.read_text())
        if not re.search(r"\bimport\s+net\.fabricmc\.fabric\.api\.gametest\.v1\.(?:GameTest|\*)\s*;", source):
            continue
        methods = sorted(set(METHOD.findall(source)))
        if not methods:
            continue
        package, name = PACKAGE.search(source), CLASS.search(source)
        if not package or not name:
            errors.append(f"Annotated methods lack package or class declaration: {path}")
            continue
        qualified = f"{package.group(1)}.{name.group(1)}"
        if qualified in classes:
            errors.append(f"Duplicate GameTest class: {qualified}")
        classes[qualified] = {"path": str(path.relative_to(root)), "methods": methods}
    return classes


def entrypoints(path, errors):
    try:
        data = json.loads(path.read_text())
        values = data["entrypoints"]["fabric-gametest"]
        if not isinstance(values, list) or not values:
            raise ValueError("fabric-gametest must be a nonempty list")
        names = []
        for value in values:
            if isinstance(value, dict):
                value = value.get("value")
            if not isinstance(value, str) or not re.fullmatch(r"[\w$]+(?:\.[\w$]+)+", value):
                raise ValueError("entry points must be Java class names or objects with a value")
            names.append(value)
        if len(names) != len(set(names)):
            errors.append("Duplicate fabric-gametest entry point")
        return sorted(set(names))
    except (OSError, ValueError, KeyError, TypeError) as exc:
        errors.append(f"Missing or malformed GameTest entry-point configuration at {path}: {exc}")
        return []


def exclusions(path, errors):
    try:
        data = json.loads(path.read_text())
        values = data["excluded_classes"]
        if not isinstance(values, list):
            raise ValueError("excluded_classes must be a list")
        names = []
        for value in values:
            if not isinstance(value, dict) or not isinstance(value.get("class"), str) or not isinstance(value.get("reason"), str) or not value["reason"].strip():
                raise ValueError("each exclusion needs a class and nonempty reason")
            names.append(value["class"])
        if len(names) != len(set(names)):
            errors.append("Duplicate GameTest exclusion")
        return sorted(set(names))
    except (OSError, ValueError, KeyError, TypeError) as exc:
        errors.append(f"Missing or malformed exclusion allowlist at {path}: {exc}")
        return []


def analyze(root, config, allowlist):
    errors = []
    classes = discover(root, errors)
    registered = entrypoints(config, errors)
    excluded = exclusions(allowlist, errors)
    found = set(classes)
    report = {
        "discovered_classes": classes,
        "discovered_methods": [f"{name}#{method}" for name in sorted(classes) for method in classes[name]["methods"]],
        "registered_classes": sorted(found & set(registered)),
        "excluded_classes": sorted((found - set(registered)) & set(excluded)),
        "unregistered_classes": sorted(found - set(registered) - set(excluded)),
        "stale_exclusions": sorted(set(excluded) - (found - set(registered))),
        "unresolved_entrypoints": sorted(set(registered) - found),
        "errors": errors,
    }
    report["requires_review"] = bool(report["unregistered_classes"] or report["stale_exclusions"] or
                                     report["unresolved_entrypoints"] or errors)
    return report


def markdown(report):
    lines = ["# GameTest coverage", "", f"Review required: **{'yes' if report['requires_review'] else 'no'}**", ""]
    for key, title in (("registered_classes", "Registered GameTest classes"),
                       ("unregistered_classes", "Unregistered GameTest classes"),
                       ("excluded_classes", "Intentionally excluded classes"),
                       ("stale_exclusions", "Stale allowlist entries"),
                       ("unresolved_entrypoints", "Entry points without annotated source"),
                       ("discovered_methods", "Discovered annotated methods")):
        lines.extend((f"## {title} ({len(report[key])})", ""))
        lines.extend(f"- `{item}`" for item in report[key])
        lines.append("")
    lines.extend(("## Errors", ""))
    lines.extend(f"- {error}" for error in report["errors"])
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--config", type=Path)
    parser.add_argument("--allowlist", type=Path)
    parser.add_argument("--json-report", type=Path, required=True)
    parser.add_argument("--markdown-report", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    report = analyze(root, args.config or root / "src/gametest/resources/fabric.mod.json",
                     args.allowlist or root / "scripts/tooling/gametest_exclusions.json")
    args.json_report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    args.markdown_report.write_text(markdown(report))
    return 2 if report["requires_review"] else 0


if __name__ == "__main__":
    sys.exit(main())

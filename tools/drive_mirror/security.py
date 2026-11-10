"""Security gate between a secret scan and the Drive upload.

Contract
--------
* Input: a gitleaks JSON report and a reviewed-findings list kept in this repository
  (`security/approved_findings.json`). Matched values (`Secret`, `Match`) are discarded the
  moment the report is parsed; nothing in this module can print them.
* A finding passes only if its fingerprint is listed with status `approved`. Status `proposed`
  means "analysed, awaiting an owner decision" and still blocks. Unknown findings block.
* Blocking findings do not stop the whole mirror. They **quarantine** exactly the refs whose
  *tip tree* would upload the flagged content (same blob anywhere, or the same flagged line at
  the same path). Quarantined refs are skipped by `sync`, recorded in state/report, and make
  `verify` report them explicitly, so nothing is ever omitted silently.
* Fail closed: an unreadable or missing report is an error (exit 7), never a pass.
* The gate never edits the approved list or any allowlist: suppressing a finding is an
  owner decision made in a reviewed commit.
"""
from __future__ import annotations

import json
import subprocess
from dataclasses import dataclass

from .gitsrc import GitSource

APPROVED_SCHEMA = "drive-mirror-approved-findings/1"
QUARANTINE_SCHEMA = "drive-mirror-quarantine/1"
STATUS_APPROVED, STATUS_PROPOSED = "approved", "proposed"


class GateError(RuntimeError):
    """The gate could not reach a verdict (fail closed)."""


@dataclass(frozen=True)
class Finding:
    rule: str
    file: str
    line: int
    commit: str
    fingerprint: str
    entropy: float | None = None


def load_findings(report_path: str) -> list[Finding]:
    try:
        with open(report_path, encoding="utf-8") as fh:
            raw = json.load(fh)
    except (OSError, ValueError) as e:
        raise GateError(f"scan report unreadable: {type(e).__name__}") from None
    if raw is None:
        raw = []
    if not isinstance(raw, list):
        raise GateError("scan report has an unexpected shape")
    out = []
    for x in raw:
        try:
            out.append(Finding(x["RuleID"], x["File"], int(x["StartLine"]), x["Commit"],
                               x.get("Fingerprint") or f'{x["Commit"]}:{x["File"]}:{x["RuleID"]}:{x["StartLine"]}',
                               x.get("Entropy")))
        except (KeyError, TypeError, ValueError):
            raise GateError("scan report contains a malformed finding") from None
        # Secret / Match are deliberately never read.
    return out


def load_approved(path: str) -> dict[str, dict]:
    try:
        with open(path, encoding="utf-8") as fh:
            doc = json.load(fh)
    except (OSError, ValueError) as e:
        raise GateError(f"approved-findings list unreadable: {type(e).__name__}") from None
    if doc.get("schema") != APPROVED_SCHEMA:
        raise GateError("approved-findings list has an unknown schema")
    return {e["fingerprint"]: e for e in doc.get("findings", [])}


def classify(findings: list[Finding], approved: dict[str, dict]) -> dict[str, list[Finding]]:
    res: dict[str, list[Finding]] = {"approved": [], "proposed": [], "unreviewed": []}
    for f in findings:
        e = approved.get(f.fingerprint)
        if e is None:
            res["unreviewed"].append(f)
        elif e.get("status") == STATUS_APPROVED:
            res["approved"].append(f)
        else:
            res["proposed"].append(f)
    return res


def _flagged_line(git: GitSource, f: Finding) -> bytes | None:
    try:
        blob = git.run("show", f"{f.commit}:{f.file}")
    except Exception:
        return None
    lines = blob.split(b"\n")
    return lines[f.line - 1] if 0 < f.line <= len(lines) else None


def affected_refs(git: GitSource, findings: list[Finding], refs: dict[str, str]) -> dict[str, list[str]]:
    """ref -> fingerprints whose flagged content its tip tree would upload."""
    out: dict[str, list[str]] = {}
    tips: dict[str, dict] = {}
    for f in findings:
        try:
            oid = git.run("rev-parse", f"{f.commit}:{f.file}").decode().strip()
        except Exception:
            oid = None
        line = _flagged_line(git, f)
        # Only refs whose history contains the finding's commit can hold the flagged content.
        contains = git.run("for-each-ref", "--contains", f.commit, "--format=%(refname)",
                           "refs/mirror/", check=False).decode().split()
        candidates = {"refs/" + c[len("refs/mirror/"):] for c in contains}
        for ref in sorted(candidates & set(refs)):
            entries = tips.get(ref)
            if entries is None:
                entries = tips[ref] = git.tree_entries(refs[ref])
            hit = oid is None                       # cannot resolve the flagged blob: be conservative
            if not hit:
                hit = any(e.oid == oid for e in entries.values())
            if not hit and line is not None:
                e = entries.get(f.file)
                if e is not None and e.type == "blob":
                    hit = line in git.read_blob(e.oid).split(b"\n")
            if hit:
                out.setdefault(ref, []).append(f.fingerprint)
    return out


def evaluate(git: GitSource, report_path: str, approved_path: str, refs: dict[str, str],
             generated_at: str) -> dict:
    findings = load_findings(report_path)
    approved = load_approved(approved_path)
    cls = classify(findings, approved)
    blocking = cls["proposed"] + cls["unreviewed"]
    affected = affected_refs(git, blocking, refs) if blocking else {}
    verdict = "PASS" if not blocking else "BLOCKED"
    return {
        "schema": QUARANTINE_SCHEMA, "generated_at": generated_at, "verdict": verdict,
        "counts": {"findings": len(findings), "approved": len(cls["approved"]),
                   "proposed_awaiting_owner": len(cls["proposed"]), "unreviewed": len(cls["unreviewed"]),
                   "refs_scanned_for_exposure": len(refs), "refs_quarantined": len(affected)},
        "blocking_findings": [
            {"fingerprint": f.fingerprint, "rule": f.rule, "file": f.file, "line": f.line, "commit": f.commit,
             "status": "proposed" if f in cls["proposed"] else "unreviewed"} for f in blocking],
        "refs": {ref: {"reason": "unapproved secret-scan finding in tip tree",
                       "fingerprints": sorted(fps)} for ref, fps in sorted(affected.items())},
        "blocking_findings_not_in_any_tip": [
            f.fingerprint for f in blocking if not any(f.fingerprint in v for v in affected.values())],
    }


def quarantine_map(doc: dict) -> dict[str, str]:
    """ref -> reason, the shape SyncConfig.quarantine expects."""
    return {ref: f'{v["reason"]} ({", ".join(fp.split(":")[0][:8] + ":" + fp.split(":")[1].split("/")[-1] for fp in v["fingerprints"])})'
            for ref, v in doc.get("refs", {}).items()}


def load_quarantine(path: str | None) -> dict[str, str]:
    if not path:
        return {}
    try:
        with open(path, encoding="utf-8") as fh:
            doc = json.load(fh)
    except (OSError, ValueError) as e:
        raise GateError(f"quarantine file unreadable: {type(e).__name__}") from None
    if doc.get("schema") != QUARANTINE_SCHEMA:
        raise GateError("quarantine file has an unknown schema")
    return quarantine_map(doc)


def git_available(git_bin: str = "git") -> bool:
    return subprocess.run([git_bin, "--version"], stdout=subprocess.DEVNULL).returncode == 0

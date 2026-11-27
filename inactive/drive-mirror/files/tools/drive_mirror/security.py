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
* The gate examines one *frozen source snapshot* (see `snapshot.py`): the exact ref -> commit map written by
  `prepare`. It never fetches. Its output (the "approval") embeds that snapshot, so `sync` uploads only
  commits the gate examined and never resolves a moving ref again.
* Fail closed: an unreadable or missing report is an error (exit 7), never a pass.
* The gate never edits the approved list or any allowlist: suppressing a finding is an
  owner decision made in a reviewed commit.
"""
from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
import tempfile
from dataclasses import dataclass, field

from .gitsrc import GitSource

APPROVED_SCHEMA = "drive-mirror-approved-findings/1"
QUARANTINE_SCHEMA = "drive-mirror-quarantine/2"            # carries the approved source snapshot
LEGACY_QUARANTINE_SCHEMA = "drive-mirror-quarantine/1"     # ref list only: accepted by verify/status, never by sync
BINDING_TOOL = "frozen-commits"        # the scanner was run by the gate on exactly the snapshot's commits
BINDING_EXTERNAL = "external-report"   # a report produced elsewhere: not provably about this snapshot
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


def run_scanner(binary: str, git_dir: str, commits: list[str], report_path: str) -> dict:
    """Run gitleaks over exactly `commits` (the frozen snapshot), never over whatever refs the cache holds.
    Returns the scan metadata recorded in the approval. Any failure to run the scanner is a GateError."""
    exe = shutil.which(binary) or (binary if os.path.isfile(binary) else None)
    if not exe:
        raise GateError("scanner executable not found")
    if not commits:
        raise GateError("snapshot has no commits to scan")
    try:
        ver = subprocess.run([exe, "version"], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=60)
        version = ver.stdout.decode(errors="replace").strip().splitlines()[0][:40] if ver.returncode == 0 else "unknown"
    except (OSError, subprocess.SubprocessError):
        version = "unknown"
    if os.path.exists(report_path):
        os.unlink(report_path)
    # Nothing but the frozen snapshot may influence what is reported: gitleaks reads a .gitleaksignore from its
    # working directory by default, honours `gitleaks:allow` comments in the scanned content, and reads a config from
    # GITLEAKS_CONFIG*. Run from an empty directory, name that directory as the ignore path, ignore inline allow
    # comments, and scrub the environment. Approving a finding is only ever done in approved_findings.json.
    neutral = tempfile.mkdtemp(prefix="drive-mirror-scan-cwd-")
    env = {k: v for k, v in os.environ.items() if not k.startswith("GITLEAKS_")}
    cmd = [exe, "git", "--no-banner", "--no-color", "--redact=100", "--report-format", "json",
           "--report-path", os.path.abspath(report_path), "--gitleaks-ignore-path", neutral, "--ignore-gitleaks-allow",
           "--log-opts=--full-history " + " ".join(commits), os.path.abspath(git_dir)]
    try:
        proc = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, cwd=neutral, env=env)
    except OSError as e:
        raise GateError(f"scanner could not be started: {type(e).__name__}") from None
    finally:
        shutil.rmtree(neutral, ignore_errors=True)
    if proc.returncode not in (0, 1):
        raise GateError(f"scanner failed (exit {proc.returncode}); refusing to decide")
    if not os.path.exists(report_path):
        if proc.returncode == 1:
            raise GateError("scanner reported findings but wrote no report")
        with open(report_path, "w", encoding="utf-8") as fh:
            fh.write("[]")
    return {"binding": BINDING_TOOL, "scanner": {"name": "gitleaks", "version": version},
            "commits_scanned": len(commits), "commits_digest": hashlib.sha256("\n".join(sorted(commits)).encode()).hexdigest()}


def external_scan_meta(report_path: str) -> dict:
    try:
        with open(report_path, "rb") as fh:
            h = hashlib.sha256(fh.read()).hexdigest()
    except OSError as e:
        raise GateError(f"scan report unreadable: {type(e).__name__}") from None
    return {"binding": BINDING_EXTERNAL, "report_sha256": h}


def evaluate(git: GitSource, report_path: str, approved_path: str, snapshot: dict, generated_at: str,
             scan: dict) -> dict:
    """Classify the scan's findings against the owner-approved list and decide, per ref of the frozen
    `snapshot`, whether its tip tree may be uploaded. Every ref the snapshot can ever upload is examined."""
    from .snapshot import canonical_digest, commit_map
    findings = load_findings(report_path)
    approved = load_approved(approved_path)
    cls = classify(findings, approved)
    blocking = cls["proposed"] + cls["unreviewed"]
    refs = commit_map(snapshot)
    affected = affected_refs(git, blocking, refs) if blocking else {}
    doc = {
        "schema": QUARANTINE_SCHEMA, "generated_at": generated_at, "verdict": "PASS" if not blocking else "BLOCKED",
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
        "scan": scan,
        "snapshot_digest": snapshot["digest"], "snapshot_created_at": snapshot["created_at"],
        "gated_refs": dict(sorted(refs.items())),
        "source_snapshot": snapshot,
    }
    doc["approval_digest"] = canonical_digest(doc, drop=("approval_digest",))
    return doc


def quarantine_map(doc: dict) -> dict[str, str]:
    """ref -> reason, the shape SyncConfig.quarantine expects."""
    return {ref: f'{v["reason"]} ({", ".join(fp.split(":")[0][:8] + ":" + fp.split(":")[1].split("/")[-1] for fp in v["fingerprints"])})'
            for ref, v in doc.get("refs", {}).items()}


@dataclass
class Approval:
    """A loaded gate output. `snapshot` is None for a legacy (v1) quarantine file, which `sync` refuses."""
    quarantine: dict[str, str]
    snapshot: dict | None = None
    gated_refs: dict[str, str] = field(default_factory=dict)
    scan: dict = field(default_factory=dict)
    verdict: str | None = None
    sha256: str = ""


def load_approval(path: str | None, expect_sha256: str | None = None) -> Approval | None:
    """Read and verify the gate output. Raises GateError on anything unexpected (fail closed)."""
    if not path:
        if expect_sha256 is not None:
            raise GateError("an approval checksum was given without an approval file")
        return None
    from .snapshot import canonical_digest, commit_map, validate
    try:
        with open(path, "rb") as fh:
            raw = fh.read()
        doc = json.loads(raw)
    except (OSError, ValueError) as e:
        raise GateError(f"quarantine file unreadable: {type(e).__name__}") from None
    sha = hashlib.sha256(raw).hexdigest()
    if expect_sha256 is not None and sha != expect_sha256.strip().lower():      # an empty value never matches
        raise GateError("approval file does not match the checksum recorded by the gate step (replaced after the gate?)")
    schema = doc.get("schema") if isinstance(doc, dict) else None
    if schema == LEGACY_QUARANTINE_SCHEMA:
        return Approval(quarantine=quarantine_map(doc), verdict=doc.get("verdict"), sha256=sha)
    if schema != QUARANTINE_SCHEMA:
        raise GateError("quarantine file has an unknown schema")
    if doc.get("approval_digest") != canonical_digest(doc, drop=("approval_digest",)):
        raise GateError("approval digest mismatch (document was altered)")
    snap = validate(doc.get("source_snapshot"))
    if doc.get("snapshot_digest") != snap["digest"]:
        raise GateError("approval does not belong to its embedded source snapshot")
    gated = doc.get("gated_refs")
    if not isinstance(gated, dict) or gated != dict(sorted(commit_map(snap).items())):
        raise GateError("approval's examined refs differ from its source snapshot")
    scan = doc.get("scan")
    if not isinstance(scan, dict) or scan.get("binding") not in (BINDING_TOOL, BINDING_EXTERNAL):
        raise GateError("approval carries no valid scan record")
    if not isinstance(doc.get("refs"), dict) or not set(doc["refs"]) <= set(gated):
        raise GateError("approval quarantines refs that are not in its snapshot")
    try:
        quarantine = quarantine_map(doc)
    except (KeyError, TypeError, IndexError, AttributeError):
        raise GateError("approval has a malformed quarantine entry") from None
    return Approval(quarantine=quarantine, snapshot=snap, gated_refs=gated, scan=scan,
                    verdict=doc.get("verdict"), sha256=sha)


def load_quarantine(path: str | None) -> dict[str, str]:
    """ref -> reason for verify/status (accepts the legacy and the current format)."""
    a = load_approval(path)
    return a.quarantine if a else {}


def git_available(git_bin: str = "git") -> bool:
    return subprocess.run([git_bin, "--version"], stdout=subprocess.DEVNULL).returncode == 0

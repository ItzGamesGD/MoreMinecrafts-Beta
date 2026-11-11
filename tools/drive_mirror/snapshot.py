"""Frozen source snapshot: the immutable object the security gate approves and the uploader consumes.

Invariant enforced here and in `sync`
-------------------------------------
Every uploaded Git blob belongs to a commit of one *frozen* snapshot that the security gate examined,
and the uploader never resolves a moving ref again: it reads only that snapshot (ref -> commit, plus
each commit's tree id) and objects that are already in the local cache.

* `prepare` fetches once, requires the fetch to agree with `git ls-remote`, and writes this document.
* `secgate` verifies that the local cache still holds exactly these refs (so the scan, which reads the
  cache, saw the same state), scans exactly these commits, and embeds the snapshot in the approval.
* `sync` and `verify` take the snapshot from the approval and never fetch.

Everything is identified by object ids: commit ids bind trees and blobs cryptographically (Git's own
object hash), and the tree id of every commit is recorded and re-checked against the local object store.
"""
from __future__ import annotations

import hashlib
import json
import re

from .gitsrc import GitSource, RefSnapshot
from .security import GateError

SNAPSHOT_SCHEMA = "drive-mirror-source-snapshot/1"
_OID = re.compile(r"^[0-9a-f]{40}$")


class SnapshotError(GateError):
    """The frozen snapshot is missing, malformed, tampered with, or no longer matches the local cache."""


def canonical_digest(doc: dict, drop: tuple[str, ...] = ("digest",)) -> str:
    body = {k: v for k, v in doc.items() if k not in drop}
    return hashlib.sha256(json.dumps(body, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode()).hexdigest()


def fetch_set(doc: dict) -> dict[str, str]:
    """ref -> object id exactly as `git fetch` stores it under refs/mirror/ (annotated tags: the tag object)."""
    out = dict(doc["heads"])
    out.update(doc["tags"])
    out.update({r: s for r, s in doc["pulls"].items() if r.endswith("/head")})
    return out


def commit_map(doc: dict) -> dict[str, str]:
    """ref -> commit id of every ref that can ever be uploaded (tags peeled to their commit)."""
    out = dict(doc["heads"])
    out.update({t: doc["tags_peeled"].get(t, s) for t, s in doc["tags"].items()})
    out.update({r: s for r, s in doc["pulls"].items() if r.endswith("/head")})
    return out


def build(git: GitSource, snap: RefSnapshot, repo_url: str, created_at: str) -> dict:
    doc = {"schema": SNAPSHOT_SCHEMA, "created_at": created_at, "repo_url": repo_url,
           "head_symref": snap.head_symref, "heads": dict(snap.heads), "tags": dict(snap.tags),
           "tags_peeled": dict(snap.tags_peeled), "pulls": dict(snap.pulls), "other": dict(snap.other)}
    commits = sorted(set(commit_map(doc).values()))
    doc["trees"] = _trees(git, commits)
    doc["digest"] = canonical_digest(doc)
    return doc


def _trees(git: GitSource, commits: list[str]) -> dict[str, str]:
    """commit -> tree id, read from the local object store in one call (a missing object is an error)."""
    if not commits:
        return {}
    out = git.run("cat-file", "--batch-check=%(objectname)", input="".join(f"{c}^{{tree}}\n" for c in commits).encode(),
                  check=False).decode().split("\n")
    trees = {}
    for c, line in zip(commits, out):
        if not _OID.match(line.strip()):
            raise SnapshotError(f"commit {c[:12]} is not available in the local cache")
        trees[c] = line.strip()
    return trees


def validate(doc) -> dict:
    """Structural and integrity validation. Returns the document; raises SnapshotError otherwise."""
    if not isinstance(doc, dict) or doc.get("schema") != SNAPSHOT_SCHEMA:
        raise SnapshotError("source snapshot has an unknown schema")
    for key in ("heads", "tags", "tags_peeled", "pulls", "other", "trees"):
        if not isinstance(doc.get(key), dict):
            raise SnapshotError(f"source snapshot lacks '{key}'")
    for key in ("heads", "tags", "tags_peeled", "pulls", "trees"):
        for k, v in doc[key].items():
            if not isinstance(k, str) or not isinstance(v, str) or not _OID.match(v):
                raise SnapshotError(f"source snapshot has a malformed entry in '{key}'")
    for r in doc["heads"]:
        if not r.startswith("refs/heads/"):
            raise SnapshotError("source snapshot has a head outside refs/heads/")
    if not isinstance(doc.get("created_at"), str) or not doc["created_at"]:
        raise SnapshotError("source snapshot has no creation time")
    if doc.get("digest") != canonical_digest(doc):
        raise SnapshotError("source snapshot digest mismatch (document was altered)")
    if set(doc["trees"]) != set(commit_map(doc).values()):
        raise SnapshotError("source snapshot tree map does not cover exactly its commits")
    return doc


def load(path: str) -> dict:
    try:
        with open(path, encoding="utf-8") as fh:
            return validate(json.load(fh))
    except (OSError, ValueError) as e:
        raise SnapshotError(f"source snapshot unreadable: {type(e).__name__}") from None


def to_ref_snapshot(doc: dict) -> RefSnapshot:
    return RefSnapshot(heads=dict(doc["heads"]), tags=dict(doc["tags"]), tags_peeled=dict(doc["tags_peeled"]),
                       pulls=dict(doc["pulls"]), other=dict(doc["other"]), head_symref=doc.get("head_symref"))


def check_objects(git: GitSource, doc: dict) -> None:
    """Every commit of the snapshot is in the local cache and has the recorded tree."""
    try:
        have = _trees(git, sorted(doc["trees"]))
    except SnapshotError:
        raise
    except Exception as e:
        raise SnapshotError(f"local cache unreadable: {type(e).__name__}") from None
    bad = [c[:12] for c, t in doc["trees"].items() if have.get(c) != t]
    if bad:
        raise SnapshotError(f"{len(bad)} commit(s) do not have the tree recorded in the snapshot")


def check_local(git: GitSource, doc: dict) -> None:
    """Objects as above, and the cache's refs/mirror/* equal the snapshot (so the scan saw this very state and
    ref-relative queries such as `--contains` answer for it)."""
    check_objects(git, doc)
    try:
        local = git.local_refs()
    except Exception as e:
        raise SnapshotError(f"local cache unreadable: {type(e).__name__}") from None
    want = fetch_set(doc)
    if local != want:
        diff = sorted(set(local.items()) ^ set(want.items()))
        raise SnapshotError(f"the local cache no longer matches the approved snapshot ({len(diff)} differing ref "
                            f"entries; first: {sorted({d[0] for d in diff})[:3]}). Something fetched after the gate; "
                            f"run prepare and secgate again")

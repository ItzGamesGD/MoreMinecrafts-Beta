"""Independent verification of a mirror against the live remote.

Deliberately shares no planning code with sync.py: it re-fetches the remote,
re-reads every tree with `git ls-tree`, hashes blob bytes itself, walks the
Drive folders itself, and compares against Drive's *server-computed*
checksums. The sync engine's own labels (appProperties, manifests, state) are
checked for consistency but never trusted as proof of content.
"""
from __future__ import annotations

import concurrent.futures as cf
import hashlib
import random

from .drive import DriveClient
from .gitsrc import GitSource, git_blob_sha1
from .layout import (CONTAINERS, FOLDER_MIME, GOOGLE_APPS_PREFIX, MANIFESTS_FOLDER, META_FOLDER, P_BLOB,
                     P_MIRROR, P_MODE, P_NS, P_ROLE, STATE_FILE, manifest_name, namespace_ref)
from .sync import FILE_MODES, utcnow

SAMPLE_LIMIT = 20  # examples kept per problem type


def _walk(drive: DriveClient, folder_id: str, workers: int = 8):
    files, dirs, dup_dirs = {}, {"": folder_id}, []
    frontier = [("", folder_id)]
    with cf.ThreadPoolExecutor(max_workers=max(1, workers)) as pool:
        while frontier:
            listings = list(pool.map(lambda pf: (pf[0], list(drive.children(pf[1]))), frontier))
            frontier = []
            for prefix, items in listings:
                for it in items:
                    path = f"{prefix}/{it['name']}" if prefix else it["name"]
                    if it["mimeType"] == FOLDER_MIME:
                        if path in dirs:
                            dup_dirs.append(path)
                            continue
                        dirs[path] = it["id"]
                        frontier.append((path, it["id"]))
                    else:
                        files.setdefault(path, []).append(it)
    return files, dirs, dup_dirs


class Problems:
    def __init__(self):
        self.counts: dict[str, int] = {}
        self.samples: dict[str, list] = {}

    def add(self, kind: str, detail) -> None:
        self.counts[kind] = self.counts.get(kind, 0) + 1
        lst = self.samples.setdefault(kind, [])
        if len(lst) < SAMPLE_LIMIT:
            lst.append(detail)

    def __bool__(self):
        return bool(self.counts)


def verify(drive: DriveClient, git: GitSource, mirror_name: str, parent_folder_id: str,
           include_tags: bool = True, pull_heads: str = "orphaned", deep_sample: int = 0,
           max_age_hours: float | None = None, seed: int = 0, only_refs: set[str] | None = None,
           quarantine: dict | None = None) -> dict:
    """`only_refs`: walk Drive and hash blobs only for these namespaces (those changed by the sync that just
    ran); every other namespace still gets the cheap global checks (inventory, state, manifest, commit match).
    A full verification (only_refs=None) is the authority and is what the daily reconcile runs."""
    report = {"started_at": utcnow(), "mirror": mirror_name, "namespaces": {}, "global": {}}
    glob = Problems()
    snap = git.snapshot(include_tags=include_tags, include_pulls=pull_heads != "none")
    quarantine = dict(quarantine or {})
    expected = dict(snap.heads)
    if include_tags:
        expected.update({t: git.peel_to_commit(s) for t, s in snap.tags.items()})
    orphaned_pulls: dict[str, str] = {}
    if pull_heads in ("orphaned", "all"):
        for r, s in snap.pull_heads().items():
            if pull_heads == "all" or not git.run("for-each-ref", "--contains", s, "--count=1",
                                                  "--format=%(refname)", "refs/mirror/heads/").decode().strip():
                orphaned_pulls[r] = s
    expected.update(orphaned_pulls)

    roots = [f for f in drive.children(parent_folder_id, f"appProperties has {{ key='{P_ROLE}' and value='root' }}")
             if f["name"] == mirror_name and f["mimeType"] == FOLDER_MIME]
    if len(roots) != 1:
        glob.add("mirror_root", f"expected exactly one root named {mirror_name!r}, found {len(roots)}")
        return _finish(report, glob)
    root = roots[0]
    token = root["appProperties"][P_MIRROR]
    top = {f["name"]: f for f in drive.children(root["id"]) if f["mimeType"] == FOLDER_MIME}
    meta_files, manifests = {}, {}
    if META_FOLDER in top:
        for f in drive.children(top[META_FOLDER]["id"]):
            if f["mimeType"] == FOLDER_MIME and f["name"] == MANIFESTS_FOLDER:
                manifests = {m["name"]: m for m in drive.children(f["id"])}
            else:
                meta_files[f["name"]] = f
    state = _json(drive, meta_files.get(STATE_FILE))
    if state is None:
        glob.add("state", "state.json missing or unreadable")
        state = {}

    if pull_heads != "none":   # sticky PR namespaces (see sync._desired_refs)
        for r in state.get("namespaces", {}):
            if r.startswith("refs/pull/") and r in snap.pull_heads() and r not in expected:
                expected[r] = snap.pull_heads()[r]
    for r, why in state.get("namespaces", {}).items():
        if why.get("unconfirmed"):
            glob.add("ref_unconfirmed", {"ref": r, **why["unconfirmed"]})

    # Namespace inventory on Drive vs. remote refs.
    found: dict[str, dict] = {}
    for kind in ("heads", "tags", "pull"):
        cname = CONTAINERS[kind]
        if cname not in top:
            if (kind == "heads") or (kind == "tags" and include_tags and snap.tags):
                glob.add("container_missing", cname)
            continue
        for f in drive.children(top[cname]["id"]):
            if f["mimeType"] != FOLDER_MIME:
                glob.add("stray_item_in_container", f"{cname}/{f['name']}")
                continue
            ref = namespace_ref(cname, f["name"])
            if ref in found:
                glob.add("duplicate_namespace", ref)
            found[ref] = f
    quarantined_expected = {r for r in quarantine if r in expected}
    for r in sorted(quarantined_expected & set(found)):
        glob.add("quarantined_content_on_drive", r)       # flagged content that was uploaded before the gate fired
    report["global"]["quarantined_refs"] = sorted(quarantined_expected)
    for ref in sorted(set(expected) - set(found) - quarantined_expected):
        glob.add("namespace_missing", ref)
    for ref in sorted(set(found) - set(expected)):
        glob.add("namespace_stale", ref)
    for name in sorted(manifests):          # every manifest must belong to exactly one live namespace
        if name.startswith("retired__"):
            continue
        owner = [r for r in expected if manifest_name(r) == name]
        if not owner:
            glob.add("manifest_without_namespace", name)
    report["global"]["expected_namespaces"] = len(expected)
    report["global"]["found_namespaces"] = len(found)

    rnd = random.Random(seed)
    hashes: dict[str, tuple[int, str, str]] = {}
    deep_pool = []
    totals = {"files_expected": 0, "files_verified": 0, "bytes_expected": 0}
    for ref in sorted(set(expected) & set(found)):
        if ref in quarantined_expected:
            continue
        p = Problems()
        commit = expected[ref]
        folder = found[ref]
        props = folder.get("appProperties") or {}
        if props.get(P_MIRROR) != token:
            p.add("namespace_foreign", ref)
        nss = state.get("namespaces", {}).get(ref)
        if not nss:
            p.add("state_missing", ref)
        else:
            if nss.get("status") != "complete":
                p.add("state_incomplete", nss.get("status"))
            if nss.get("commit") != commit:
                p.add("state_stale_commit", {"state": nss.get("commit"), "remote": commit})
            if nss.get("folder_id") != folder["id"] or nss.get("nsid") != props.get(P_NS):
                p.add("state_folder_mismatch", ref)
        manifest = _json(drive, manifests.get(manifest_name(ref)))
        if manifest is None:
            p.add("manifest_missing", manifest_name(ref))
        else:
            if manifest.get("commit", {}).get("sha") != commit:
                p.add("manifest_stale_commit", {"manifest": manifest.get("commit", {}).get("sha"), "remote": commit})
            if manifest.get("ref") != ref or (manifest.get("source") or {}).get("ref") != ref:
                p.add("manifest_ref_mismatch", {"manifest": manifest.get("ref"), "expected": ref})
            kind = {"heads": "branch", "tags": "tag", "pull": "pull_head"}[ref.split("/")[1] if ref.split("/")[1] in
                                                                         ("heads", "tags") else "pull"]
            if (manifest.get("source") or {}).get("kind") != kind:
                p.add("manifest_kind_mismatch", {"manifest": (manifest.get("source") or {}).get("kind"),
                                                 "expected": kind})
        if only_refs is not None and ref not in only_refs:
            # Scoped run: cheap structural checks only for namespaces this sync did not touch.
            if manifest is not None and manifest.get("file_count") != len(
                    [e for e in git.tree_entries(commit).values() if e.mode in FILE_MODES]):
                p.add("manifest_file_count_mismatch", ref)
            report["namespaces"][ref] = {"commit": commit, "files": manifest.get("file_count") if manifest else None,
                                         "ok": not p, "scope": "shallow", "problems": p.counts, "samples": p.samples}
            continue

        entries = git.tree_entries(commit)
        want = {e.path: e for e in entries.values() if e.mode in FILE_MODES}
        for e in entries.values():
            if e.mode not in FILE_MODES:
                p.add("unsupported_entry_not_mirrored", {"path": e.path, "mode": e.mode})
        files, dirs, dup_dirs = _walk(drive, folder["id"])
        for d in dup_dirs:
            p.add("duplicate_folder", d)
        want_dirs = {""} | {"/".join(x.split("/")[:i]) for x in want for i in range(1, x.count("/") + 1)}
        for d in sorted(set(dirs) - want_dirs):
            p.add("extra_folder", d)
        manifest_ids = {k: v["id"] for k, v in (manifest or {}).get("files", {}).items()}
        for path, e in want.items():
            totals["files_expected"] += 1
            totals["bytes_expected"] += e.size or 0
            items = files.get(path, [])
            if not items:
                # Listing can lag behind writes; confirm by direct, strongly consistent GET.
                mid = manifest_ids.get(path)
                it = drive.get(mid) if mid else None
                parent_ok = it and not it.get("trashed") and dirs.get(path.rsplit("/", 1)[0] if "/" in path else "") \
                    in (it.get("parents") or [])
                if it and parent_ok and it.get("name") == path.rsplit("/", 1)[-1]:
                    items = [it]
                    p.add("listing_lag_resolved_by_get", path)
                else:
                    p.add("file_missing", path)
                    continue
            if len(items) > 1:
                p.add("duplicate_file", path)
            it = items[0]
            if e.oid not in hashes:            # read_blob itself re-verifies the git object id
                data = git.read_blob(e.oid)
                hashes[e.oid] = (len(data), hashlib.sha256(data).hexdigest(), hashlib.md5(data).hexdigest())
            size, sha, md5 = hashes[e.oid]
            if it.get("mimeType", "").startswith(GOOGLE_APPS_PREFIX):
                p.add("converted_to_google_format", path)
                continue
            if str(it.get("size")) != str(size):
                p.add("size_mismatch", {"path": path, "drive": it.get("size"), "git": size})
                continue
            server = it.get("sha256Checksum")
            if server:
                if server != sha:
                    p.add("checksum_mismatch", path)
                    continue
            elif it.get("md5Checksum"):
                if it["md5Checksum"] != md5:
                    p.add("checksum_mismatch", path)
                    continue
                p.add("verified_by_md5_only", path)
            else:
                p.add("no_server_checksum", path)
                continue
            ip = it.get("appProperties") or {}
            if ip.get(P_BLOB) != e.oid or ip.get(P_MODE) != e.mode:
                p.add("label_mismatch", path)
            if manifest_ids.get(path) not in (None, it["id"]):
                p.add("manifest_id_mismatch", path)
            totals["files_verified"] += 1
            deep_pool.append((ref, path, it["id"], e.oid))
        for path in sorted(set(files) - set(want)):
            p.add("extra_file", path)
        report["namespaces"][ref] = {"commit": commit, "files": len(want), "ok": not p, "scope": "full",
                                     "problems": p.counts, "samples": p.samples}

    if deep_sample and deep_pool:
        picks = deep_pool if deep_sample >= len(deep_pool) else rnd.sample(deep_pool, deep_sample)
        bad = 0
        for ref, path, fid, oid in picks:
            if git_blob_sha1(drive.download(fid)) != oid:
                bad += 1
                glob.add("deep_download_mismatch", f"{ref}:{path}")
        report["global"]["deep_downloads"] = {"checked": len(picks), "mismatched": bad}

    if max_age_hours is not None:
        lc = state.get("last_complete_run", {})
        import datetime as dt
        ts = lc.get("finished_at")
        if not ts:
            glob.add("never_completed", "no complete run recorded")
        else:
            age = (dt.datetime.now(dt.timezone.utc) -
                   dt.datetime.fromisoformat(ts.replace("Z", "+00:00"))).total_seconds() / 3600
            report["global"]["hours_since_last_complete_run"] = round(age, 2)
            if age > max_age_hours:
                glob.add("stale_mirror", f"last complete run {age:.1f} h ago")
    report["global"].update(totals)
    report["global"]["scope"] = "full" if only_refs is None else f"changed-namespaces:{len(only_refs)}"
    return _finish(report, glob)


# Problems that are informational and do not fail verification.
INFO_ONLY = {"listing_lag_resolved_by_get", "verified_by_md5_only", "unsupported_entry_not_mirrored"}


def _finish(report: dict, glob: Problems) -> dict:
    report["global"]["problems"] = glob.counts
    report["global"]["samples"] = glob.samples
    failing_ns = [r for r, v in report["namespaces"].items()
                  if set(v["problems"]) - INFO_ONLY]
    report["failing_namespaces"] = failing_ns
    report["result"] = "PASS" if not failing_ns and not (set(glob.counts) - INFO_ONLY) else "FAIL"
    report["finished_at"] = utcnow()
    return report


def _json(drive, item):
    if not item:
        return None
    import json
    try:
        return json.loads(drive.download(item["id"]))
    except Exception:
        return None

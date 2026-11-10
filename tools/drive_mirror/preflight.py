"""Pre-implementation checks: authentication, destination permissions, quota.

Output is safe for public workflow logs: no account e-mail, no token, no
folder name — only booleans, counts and the account *type*.
"""
from __future__ import annotations

import os

from .drive import DriveClient, DriveError
from .sync import utcnow


def preflight(drive: DriveClient, parent_folder_id: str, drive_id: str | None, expected_bytes: int = 0,
              write_probe: bool = True) -> dict:
    rep = {"checked_at": utcnow(), "checks": {}, "result": "PASS"}
    c = rep["checks"]

    def fail(name, msg, blocked=True):
        c[name] = {"ok": False, "detail": msg}
        if rep["result"] == "PASS" or blocked:
            rep["result"] = "BLOCKED" if blocked else "FAIL"

    try:
        _, about = drive._json("GET", "/drive/v3/about", {"fields": "user(emailAddress),storageQuota"}, kind="read")
        email = (about.get("user") or {}).get("emailAddress", "")
        c["authentication"] = {"ok": True, "account_type":
                               "service_account" if email.endswith(".gserviceaccount.com") else "user"}
        q = about.get("storageQuota") or {}
        limit = int(q["limit"]) if q.get("limit") else None
        usage = int(q.get("usage", 0))
        free = (limit - usage) if limit is not None else None
        c["my_drive_quota"] = {"ok": True, "limit_bytes": limit, "usage_bytes": usage, "free_bytes": free,
                               "note": "unlimited or pooled" if limit is None else None}
        if drive_id is None and free is not None and expected_bytes and free < expected_bytes * 1.1:
            fail("my_drive_quota", f"free {free} B < 110% of expected mirror size {expected_bytes} B", blocked=False)
    except DriveError as e:
        fail("authentication", str(e))
        return rep

    if drive_id:
        try:
            _, d = drive._json("GET", f"/drive/v3/drives/{drive_id}", {"fields": "id,capabilities"}, kind="read")
            caps = d.get("capabilities") or {}
            c["shared_drive"] = {"ok": bool(caps.get("canAddChildren")), "can_add_children": caps.get("canAddChildren"),
                                 "can_trash_children": caps.get("canTrashChildren")}
            if not caps.get("canAddChildren"):
                fail("shared_drive", "principal cannot add files to the shared drive")
        except DriveError as e:
            fail("shared_drive", str(e))

    if parent_folder_id == "root":
        c["destination"] = {"ok": True, "detail": "My Drive root"}
    else:
        f = None
        try:
            f = drive.get(parent_folder_id, "id,mimeType,trashed,capabilities(canAddChildren,canTrashChildren)")
        except DriveError as e:
            fail("destination", str(e))
        if f is None and "destination" not in c:
            fail("destination", "parent folder not found or not visible to this principal")
        elif f is not None:
            caps = f.get("capabilities") or {}
            ok = f.get("mimeType") == "application/vnd.google-apps.folder" and not f.get("trashed") \
                and caps.get("canAddChildren") is not False
            c["destination"] = {"ok": ok, "can_add_children": caps.get("canAddChildren"),
                                "can_trash_children": caps.get("canTrashChildren")}
            if not ok:
                fail("destination", "parent is not a writable, untrashed folder")

    if rep["result"] == "PASS" and write_probe:
        try:
            probe = drive.create_file(f"_drive_mirror_probe_{os.urandom(4).hex()}.bin", parent_folder_id,
                                      b"\x00probe\xff", "application/octet-stream", {"mm_role": "probe"})
            ok = probe.get("size") == "7" and not probe.get("mimeType", "").startswith("application/vnd.google-apps.")
            drive.trash(probe["id"])
            c["write_probe"] = {"ok": ok, "server_sha256_available": bool(probe.get("sha256Checksum")),
                                "server_md5_available": bool(probe.get("md5Checksum"))}
            if not ok:
                fail("write_probe", "probe file was not stored byte-for-byte", blocked=False)
        except DriveError as e:
            fail("write_probe", str(e))
    return rep

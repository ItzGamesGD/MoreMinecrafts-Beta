"""Minimal Google Drive v3 REST client (stdlib only).

Design points:
  * Every create uses an ID from files.generateIds, so a retried create whose
    first attempt actually succeeded is detected (HTTP 409) instead of producing
    a duplicate.
  * Uploads never request conversion: the target mimeType is the file's own
    type (or application/octet-stream) and never application/vnd.google-apps.*,
    so Drive stores the original bytes.
  * Listings that Drive marks `incompleteSearch` raise, so no destructive
    decision is ever made from a partial listing.
  * 429 / 5xx / rate-limit 403 / network errors are retried with exponential
    backoff and jitter; 401 refreshes the token once.
"""
from __future__ import annotations

import collections
import json
import os
import random
import socket
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

from .layout import FOLDER_MIME, GOOGLE_APPS_PREFIX, q_str

FILE_FIELDS = "id,name,mimeType,parents,size,md5Checksum,sha256Checksum,appProperties,trashed"
RESUMABLE_THRESHOLD = 5 * 1024 * 1024
# Google documents generateIds ids for create/copy; the exact error for a reused id is
# treated as either HTTP 409 or a 400 with one of these reasons.
ID_IN_USE = {"fileIdInUse", "duplicate", "alreadyExists"}
RETRYABLE_403 = {"rateLimitExceeded", "userRateLimitExceeded", "backendError", "sharingRateLimitExceeded"}


class DriveError(RuntimeError):
    def __init__(self, status: int, reason: str, message: str):
        super().__init__(f"Drive HTTP {status} {reason}: {message}")
        self.status, self.reason = status, reason


class IncompleteListing(RuntimeError):
    pass


class DriveClient:
    def __init__(self, token_provider, base: str = "https://www.googleapis.com", drive_id: str | None = None,
                 timeout: float = 300, max_attempts: int = 8, backoff_base: float = 1.0):
        self.tokens = token_provider
        self.base = base.rstrip("/")
        self.drive_id = drive_id
        self.timeout = timeout
        self.max_attempts = max_attempts
        self.backoff_base = backoff_base
        self.stats = collections.Counter()
        self._stats_lock = threading.Lock()
        self._ids: list[str] = []
        self._ids_lock = threading.Lock()
        self._opener = urllib.request.build_opener()

    # ------------------------------------------------------------------ transport
    def _count(self, key: str, n: int = 1) -> None:
        with self._stats_lock:
            self.stats[key] += n

    def _request(self, method: str, path: str, params: dict | None = None, body: bytes | None = None,
                 headers: dict | None = None, absolute_url: str | None = None, kind: str = "read",
                 allow_status: tuple = ()):
        params = dict(params or {})
        if absolute_url is None:
            params.setdefault("supportsAllDrives", "true")
            url = f"{self.base}{path}?{urllib.parse.urlencode(params)}"
        else:
            url = absolute_url
        refreshed = False
        last_exc = None
        for attempt in range(1, self.max_attempts + 1):
            hdrs = dict(headers or {})
            hdrs["Authorization"] = f"Bearer {self.tokens.token()}"
            hdrs.setdefault("User-Agent", "drive-mirror/1")
            req = urllib.request.Request(url, data=body, method=method, headers=hdrs)
            self._count(f"http_{kind}")
            try:
                with self._opener.open(req, timeout=self.timeout) as resp:
                    data = resp.read()
                    return resp.status, dict(resp.headers), data
            except urllib.error.HTTPError as e:
                data = e.read()
                if e.code in allow_status:
                    return e.code, dict(e.headers), data
                reason, message = _error_reason(data)
                if e.code == 401 and not refreshed:
                    refreshed = True
                    self.tokens.invalidate()
                    continue
                retryable = e.code in (429, 500, 502, 503, 504) or (e.code == 403 and reason in RETRYABLE_403)
                if not retryable or attempt == self.max_attempts:
                    raise DriveError(e.code, reason, message) from None
                last_exc = DriveError(e.code, reason, message)
            except (urllib.error.URLError, socket.timeout, ConnectionError, TimeoutError) as e:
                if attempt == self.max_attempts:
                    raise
                last_exc = e
            self._count("retries")
            delay = min(64.0, self.backoff_base * (2 ** (attempt - 1))) * (0.5 + random.random() / 2)
            time.sleep(delay)
        raise last_exc  # pragma: no cover

    def _json(self, method, path, params=None, obj=None, kind="write", allow_status=()):
        body = json.dumps(obj).encode() if obj is not None else None
        headers = {"Content-Type": "application/json; charset=UTF-8"} if obj is not None else {}
        status, _, data = self._request(method, path, params, body, headers, kind=kind, allow_status=allow_status)
        return status, (json.loads(data) if data else {})

    # ------------------------------------------------------------------ ids
    def new_id(self) -> str:
        with self._ids_lock:
            if not self._ids:
                _, data = self._json("GET", "/drive/v3/files/generateIds",
                                     {"count": 1000, "space": "drive", "type": "files"}, kind="read")
                self._ids = list(data["ids"])
            return self._ids.pop()

    # ------------------------------------------------------------------ reads
    def get(self, file_id: str, fields: str = FILE_FIELDS) -> dict | None:
        status, data = self._json("GET", f"/drive/v3/files/{file_id}", {"fields": fields},
                                  kind="read", allow_status=(404,))
        return None if status == 404 else data

    def list(self, q: str, fields: str = FILE_FIELDS, page_size: int = 1000):
        params = {"q": q, "fields": f"nextPageToken,incompleteSearch,files({fields})",
                  "pageSize": page_size, "includeItemsFromAllDrives": "true"}
        if self.drive_id:
            params.update(corpora="drive", driveId=self.drive_id)
        token = None
        while True:
            if token:
                params["pageToken"] = token
            _, data = self._json("GET", "/drive/v3/files", params, kind="read")
            if data.get("incompleteSearch"):
                raise IncompleteListing(f"Drive reported an incomplete listing for q={q!r}")
            yield from data.get("files", [])
            token = data.get("nextPageToken")
            if not token:
                return

    def children(self, folder_id: str, extra_q: str = ""):
        q = f"{q_str(folder_id)} in parents and trashed = false"
        if extra_q:
            q += " and " + extra_q
        return self.list(q)

    def download(self, file_id: str) -> bytes:
        _, _, data = self._request("GET", f"/drive/v3/files/{file_id}", {"alt": "media"}, kind="read")
        return data

    # ------------------------------------------------------------------ writes
    def create_folder(self, name: str, parent: str, props: dict) -> dict:
        meta = {"id": self.new_id(), "name": name, "mimeType": FOLDER_MIME, "parents": [parent],
                "appProperties": props}
        return self._create_with_id("/drive/v3/files", meta)

    def create_file(self, name: str, parent: str, content: bytes, mime: str, props: dict) -> dict:
        _check_mime(mime)
        meta = {"id": self.new_id(), "name": name, "mimeType": mime, "parents": [parent], "appProperties": props}
        return self._upload("POST", "/upload/drive/v3/files", meta, content, mime)

    def update_content(self, file_id: str, content: bytes, mime: str, props: dict | None,
                       name: str | None = None) -> dict:
        _check_mime(mime)
        meta: dict = {"mimeType": mime}
        if props is not None:
            meta["appProperties"] = props
        if name is not None:
            meta["name"] = name
        return self._upload("PATCH", f"/upload/drive/v3/files/{file_id}", meta, content, mime)

    def update_meta(self, file_id: str, name: str | None = None, add_parent: str | None = None,
                    remove_parent: str | None = None, props: dict | None = None,
                    trashed: bool | None = None) -> dict:
        params = {"fields": FILE_FIELDS}
        if add_parent:
            params["addParents"] = add_parent
        if remove_parent:
            params["removeParents"] = remove_parent
        meta: dict = {}
        if name is not None:
            meta["name"] = name
        if props is not None:
            meta["appProperties"] = props
        if trashed is not None:
            meta["trashed"] = trashed
        _, data = self._json("PATCH", f"/drive/v3/files/{file_id}", params, meta)
        return data

    def trash(self, file_id: str) -> None:
        """Recoverable delete (Drive trash). The mirror never hard-deletes."""
        status, _ = self._json("PATCH", f"/drive/v3/files/{file_id}", {"fields": "id"},
                               {"trashed": True}, allow_status=(404,))
        if status == 404:
            self._count("trash_already_gone")

    def copy(self, src_id: str, name: str, parent: str, props: dict) -> dict:
        meta = {"id": self.new_id(), "name": name, "parents": [parent], "appProperties": props}
        return self._create_with_id(f"/drive/v3/files/{src_id}/copy", meta)

    # ------------------------------------------------------------------ helpers
    def _create_with_id(self, path: str, meta: dict) -> dict:
        status, data = self._json("POST", path, {"fields": FILE_FIELDS}, meta, allow_status=(400, 409))
        if status in (400, 409):
            reason, message = _error_reason(json.dumps(data).encode())
            if status == 409 or reason in ID_IN_USE:
                # An earlier attempt with this pre-generated id already succeeded.
                return self._recover_conflict(meta)
            raise DriveError(status, reason, message)
        return data

    def _upload(self, method: str, path: str, meta: dict, content: bytes, mime: str) -> dict:
        self._count("bytes_uploaded", len(content))
        if len(content) < RESUMABLE_THRESHOLD:
            boundary = "mm" + os.urandom(16).hex()
            while boundary.encode() in content:
                boundary = "mm" + os.urandom(16).hex()
            body = (f"--{boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".encode()
                    + json.dumps(meta).encode()
                    + f"\r\n--{boundary}\r\nContent-Type: {mime}\r\n\r\n".encode()
                    + content + f"\r\n--{boundary}--".encode())
            status, _, data = self._request(
                method, path, {"uploadType": "multipart", "fields": FILE_FIELDS}, body,
                {"Content-Type": f"multipart/related; boundary={boundary}"}, kind="write",
                allow_status=(400, 409) if method == "POST" else ())
            if status in (400, 409):
                return self._conflict_or_raise(status, data, meta)
            return json.loads(data)
        # Resumable session: one PUT of the whole body; a failed session is restarted.
        for attempt in range(3):
            status, headers, data = self._request(
                method, path, {"uploadType": "resumable", "fields": FILE_FIELDS},
                json.dumps(meta).encode(),
                {"Content-Type": "application/json; charset=UTF-8", "X-Upload-Content-Type": mime,
                 "X-Upload-Content-Length": str(len(content))}, kind="write",
                allow_status=(400, 409) if method == "POST" else ())
            if status in (400, 409):
                return self._conflict_or_raise(status, data, meta)
            location = {k.lower(): v for k, v in headers.items()}.get("location")
            try:
                _, _, data = self._request("PUT", "", body=content, absolute_url=location,
                                           headers={"Content-Type": mime}, kind="write")
                return json.loads(data)
            except DriveError as e:
                if e.status == 404 and attempt < 2:  # session expired
                    continue
                raise
        raise DriveError(500, "resumable", "upload session kept failing")

    def _conflict_or_raise(self, status: int, data: bytes, meta: dict) -> dict:
        reason, message = _error_reason(data)
        if status == 409 or reason in ID_IN_USE:
            return self._recover_conflict(meta)
        raise DriveError(status, reason, message)

    def _recover_conflict(self, meta: dict) -> dict:
        existing = self.get(meta["id"])
        if existing is None:
            raise DriveError(409, "conflict", f"id {meta['id']} in use but not readable")
        self._count("create_conflict_recovered")
        return existing


def _check_mime(mime: str) -> None:
    if mime.startswith(GOOGLE_APPS_PREFIX):
        raise ValueError("refusing to upload with a Google Apps target type (would convert the file)")


def _error_reason(data: bytes) -> tuple[str, str]:
    try:
        err = json.loads(data)["error"]
        reasons = [e.get("reason", "") for e in err.get("errors", [])]
        return (reasons[0] if reasons else str(err.get("status", ""))), str(err.get("message", ""))[:300]
    except Exception:
        return "", data[:200].decode(errors="replace")

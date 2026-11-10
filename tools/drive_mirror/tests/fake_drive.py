"""In-memory emulator of the subset of the Google Drive v3 REST API the mirror uses.

It is intentionally strict where real Drive is strict, so tests fail on the
mistakes that would fail against Google:
  * create/copy ids must come from files.generateIds; reusing one -> 409
  * appProperties: key+value <= 124 bytes; PATCH merges keys (null deletes)
  * trashing a folder hides every descendant from `trashed = false` queries
  * only fields named in `fields=` are returned
  * md5/sha1/sha256 checksums and size are computed from the stored bytes
  * unsupported `q` syntax -> 400, so the client cannot rely on extensions
  * a requested Google Apps mimeType on upload "converts" (mangles) the bytes
Fault injection: periodic 503s, rate-limit 403s, listing lag (items created after T are
not yet listed), write counters.
This is NOT evidence about Google's real service; it exercises our client.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

FOLDER = "application/vnd.google-apps.folder"


class Store:
    def __init__(self, token: str = "test-token"):
        self.token = token
        self.files: dict[str, dict] = {}
        self.blobs: dict[str, bytes] = {}
        self.issued: set[str] = set()
        self.sessions: dict[str, dict] = {}
        self.lock = threading.RLock()
        self.counters = {"requests": 0, "writes": 0, "reads": 0, "uploads": 0}
        self.faults = {"fail_every": 0, "ratelimit_every": 0, "list_hide_created_after": 0.0,
                       "lose_response_every": 0}   # apply the write, then answer 503 (lost response)
        self.quota_limit = 15 * 1024 ** 3
        self.seq = 0
        self.by_parent: dict[str, dict[str, None]] = {}
        self.root = self._mk({"id": "root", "name": "My Drive", "mimeType": FOLDER, "parents": []})

    def _mk(self, f: dict) -> dict:
        self.seq += 1
        f.setdefault("appProperties", {})
        f.setdefault("trashed", False)
        f["_created"] = time.time()
        f["_seq"] = self.seq
        self.files[f["id"]] = f
        for p in f.get("parents") or []:
            self.by_parent.setdefault(p, {})[f["id"]] = None
        return f

    def set_parents(self, f: dict, parents: list[str]) -> None:
        for p in f["parents"]:
            self.by_parent.get(p, {}).pop(f["id"], None)
        f["parents"] = parents
        for p in parents:
            self.by_parent.setdefault(p, {})[f["id"]] = None

    def effectively_trashed(self, f: dict) -> bool:
        seen = set()
        while f:
            if f["trashed"]:
                return True
            if f["id"] in seen or not f.get("parents"):
                return False
            seen.add(f["id"])
            f = self.files.get(f["parents"][0])
        return False

    def public(self, f: dict) -> dict:
        out = {k: v for k, v in f.items() if not k.startswith("_")}
        out["trashed"] = self.effectively_trashed(f)
        if f.get("_blob") is not None:
            data = self.blobs[f["_blob"]]
            out["size"] = str(len(data))
            out["md5Checksum"] = hashlib.md5(data).hexdigest()
            out["sha1Checksum"] = hashlib.sha1(data).hexdigest()
            out["sha256Checksum"] = f["_blob"]
        return out

    def put_bytes(self, data: bytes) -> str:
        h = hashlib.sha256(data).hexdigest()
        self.blobs.setdefault(h, data)
        return h


class HttpError(Exception):
    def __init__(self, code: int, reason: str, message: str = ""):
        self.code, self.reason, self.message = code, reason, message or reason


_STR = r"'((?:[^'\\]|\\.)*)'"
_CLAUSES = [
    ("parent", re.compile(_STR + r"\s+in\s+parents")),
    ("trashed", re.compile(r"trashed\s*=\s*(true|false)")),
    ("name", re.compile(r"name\s*=\s*" + _STR)),
    ("mime", re.compile(r"mimeType\s*(=|!=)\s*" + _STR)),
    ("prop", re.compile(r"appProperties\s+has\s*\{\s*key\s*=\s*" + _STR + r"\s+and\s+value\s*=\s*" + _STR + r"\s*\}")),
]


def _unq(s: str) -> str:
    return re.sub(r"\\(.)", r"\1", s)


def parse_q(q: str):
    preds, pos = [], 0
    q = q.strip()
    while True:
        for kind, rx in _CLAUSES:
            m = rx.match(q, pos)
            if m:
                break
        else:
            raise HttpError(400, "invalid", f"unsupported query at {q[pos:pos + 40]!r}")
        g = m.groups()
        if kind == "parent":
            preds.append(("parent", _unq(g[0])))
        elif kind == "trashed":
            preds.append(("trashed", g[0] == "true"))
        elif kind == "name":
            preds.append(lambda f, v=_unq(g[0]): f["name"] == v)
        elif kind == "mime":
            preds.append(lambda f, op=g[0], v=_unq(g[1]): (f["mimeType"] == v) == (op == "="))
        else:
            preds.append(lambda f, k=_unq(g[0]), v=_unq(g[1]): f.get("appProperties", {}).get(k) == v)
        pos = m.end()
        rest = q[pos:]
        if not rest.strip():
            return preds
        m2 = re.match(r"\s+and\s+", q[pos:])
        if not m2:
            raise HttpError(400, "invalid", f"expected 'and' at {rest[:40]!r}")
        pos += m2.end()


def filter_fields(obj: dict, spec: str | None) -> dict:
    if not spec:
        return {k: obj[k] for k in ("id", "name", "mimeType") if k in obj}
    out = {}
    for part in _split_fields(spec):
        if "(" in part:
            key, inner = part[:part.index("(")], part[part.index("(") + 1:-1]
            if key in obj:
                out[key] = [filter_fields(x, inner) for x in obj[key]] if isinstance(obj[key], list) \
                    else filter_fields(obj[key], inner)
        elif part == "*":
            return dict(obj)
        elif part in obj:
            out[part] = obj[part]
    return out


def _split_fields(spec: str):
    depth, cur, parts = 0, "", []
    for ch in spec:
        if ch == "," and depth == 0:
            parts.append(cur.strip())
            cur = ""
            continue
        depth += ch == "("
        depth -= ch == ")"
        cur += ch
    if cur.strip():
        parts.append(cur.strip())
    return parts


def _check_props(props: dict) -> None:
    for k, v in props.items():
        if v is not None and len(k.encode()) + len(str(v).encode()) > 124:
            raise HttpError(400, "invalid", f"appProperty {k} too large")


class Handler(BaseHTTPRequestHandler):
    store: Store = None
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):  # silence
        pass

    def _send(self, code: int, obj=None, raw: bytes | None = None, headers: dict | None = None):
        body = raw if raw is not None else (json.dumps(obj).encode() if obj is not None else b"")
        self.send_response(code)
        for k, v in (headers or {}).items():
            self.send_header(k, v)
        self.send_header("Content-Type", "application/json" if raw is None else "application/octet-stream")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle(self, method: str):
        st = self.store
        url = urllib.parse.urlsplit(self.path)
        qs = {k: v[-1] for k, v in urllib.parse.parse_qs(url.query, keep_blank_values=True).items()}
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else b""
        try:
            if self.headers.get("Authorization") != f"Bearer {st.token}":
                raise HttpError(401, "authError", "Invalid Credentials")
            with st.lock:
                st.counters["requests"] += 1
                n = st.counters["requests"]
                if method != "GET":
                    st.counters["writes"] += 1
                else:
                    st.counters["reads"] += 1
                fe, re_ = st.faults["fail_every"], st.faults["ratelimit_every"]
            if fe and n % fe == 0:
                raise HttpError(503, "backendError", "injected")
            if re_ and n % re_ == 0:
                raise HttpError(403, "userRateLimitExceeded", "injected")
            code, obj, extra = self._route(method, url.path, qs, body)
            lr = st.faults["lose_response_every"]
            if lr and method != "GET" and n % lr == 0:
                raise HttpError(503, "backendError", "injected after commit")
            if isinstance(obj, (bytes, bytearray)):
                self._send(code, raw=bytes(obj), headers=extra)
            else:
                self._send(code, obj, headers=extra)
        except HttpError as e:
            self._send(e.code, {"error": {"code": e.code, "message": e.message,
                                          "errors": [{"reason": e.reason, "message": e.message}]}})

    do_GET = lambda self: self._handle("GET")
    do_POST = lambda self: self._handle("POST")
    do_PATCH = lambda self: self._handle("PATCH")
    do_PUT = lambda self: self._handle("PUT")
    do_DELETE = lambda self: self._handle("DELETE")

    # ------------------------------------------------------------------ routing
    def _route(self, method, path, qs, body):
        st = self.store
        fields = qs.get("fields")
        if path == "/drive/v3/about" and method == "GET":
            with st.lock:
                usage = sum(len(st.blobs[f["_blob"]]) for f in st.files.values() if f.get("_blob"))
            return 200, filter_fields({"user": {"emailAddress": "fake@example.invalid"},
                                       "storageQuota": {"limit": str(st.quota_limit), "usage": str(usage)}},
                                      fields), None
        if path == "/drive/v3/files/generateIds" and method == "GET":
            ids = [os.urandom(16).hex()[:28] + "fk" + os.urandom(2).hex()[:3] for _ in range(int(qs.get("count", 10)))]
            with st.lock:
                st.issued.update(ids)
            return 200, {"ids": ids, "space": "drive", "kind": "drive#generatedIds"}, None
        if path == "/drive/v3/files" and method == "GET":
            return 200, self._list(qs), None
        if path == "/drive/v3/files" and method == "POST":
            meta = json.loads(body or b"{}")
            return 200, filter_fields(st.public(self._create(meta, None)), fields), None
        m = re.fullmatch(r"/drive/v3/files/([^/]+)/copy", path)
        if m and method == "POST":
            return 200, filter_fields(st.public(self._copy(m.group(1), json.loads(body or b"{}"))), fields), None
        m = re.fullmatch(r"/drive/v3/files/([^/]+)", path)
        if m:
            fid = m.group(1)
            with st.lock:
                f = st.files.get(fid)
                if f is None or fid == "root" and method != "GET":
                    raise HttpError(404, "notFound", f"File not found: {fid}")
                if method == "GET":
                    if qs.get("alt") == "media":
                        if f.get("_blob") is None:
                            raise HttpError(403, "fileNotDownloadable", "folder")
                        return 200, st.blobs[f["_blob"]], None
                    return 200, filter_fields(st.public(f), fields), None
                if method == "PATCH":
                    self._patch_meta(f, json.loads(body or b"{}"), qs)
                    return 200, filter_fields(st.public(f), fields), None
                if method == "DELETE":
                    st.set_parents(f, [])
                    del st.files[fid]
                    return 204, None, None
        m = re.fullmatch(r"/upload/drive/v3/files(?:/([^/]+))?", path)
        if m:
            fid = m.group(1)
            ut = qs.get("uploadType")
            if ut == "multipart":
                meta, data = _parse_multipart(self.headers.get("Content-Type", ""), body)
                return 200, filter_fields(st.public(self._write(method, fid, meta, data)), fields), None
            if ut == "resumable":
                sid = os.urandom(8).hex()
                with st.lock:
                    st.sessions[sid] = {"method": method, "fid": fid, "meta": json.loads(body or b"{}"),
                                        "fields": fields, "expect": int(self.headers.get("X-Upload-Content-Length", -1))}
                host = self.headers.get("Host")
                return 200, {}, {"Location": f"http://{host}/upload/session/{sid}"}
            raise HttpError(400, "invalid", f"uploadType {ut}")
        m = re.fullmatch(r"/upload/session/([0-9a-f]+)", path)
        if m and method == "PUT":
            with st.lock:
                sess = st.sessions.pop(m.group(1), None)
            if sess is None:
                raise HttpError(404, "notFound", "upload session")
            if sess["expect"] >= 0 and sess["expect"] != len(body):
                raise HttpError(400, "invalid", "length mismatch")
            st.counters["uploads"] += 1
            return 200, filter_fields(st.public(self._write(sess["method"], sess["fid"], sess["meta"], body)),
                                      sess["fields"]), None
        m = re.fullmatch(r"/drive/v3/drives/([^/]+)", path)
        if m and method == "GET":
            return 200, filter_fields({"id": m.group(1), "capabilities": {"canAddChildren": True,
                                                                           "canTrashChildren": True}}, fields), None
        raise HttpError(404, "notFound", f"no route {method} {path}")

    def _list(self, qs):
        st = self.store
        preds = parse_q(qs.get("q", ""))
        trashed_pred = [p for p in preds if isinstance(p, tuple) and p[0] == "trashed"]
        parent_pred = [p[1] for p in preds if isinstance(p, tuple) and p[0] == "parent"]
        other = [p for p in preds if not isinstance(p, tuple)]
        hide_after = st.faults["list_hide_created_after"]   # models search-index lag on recent writes
        with st.lock:
            items = []
            if parent_pred:
                cand = [st.files[i] for i in st.by_parent.get(parent_pred[0], {}) if i in st.files]
                cand = [f for f in cand if all(p in f["parents"] for p in parent_pred)]
            else:
                cand = list(st.files.values())
            for f in sorted(cand, key=lambda f: f["_seq"]):
                if f["id"] == "root":
                    continue
                if hide_after and f["_created"] > hide_after:
                    continue
                t = st.effectively_trashed(f)
                if any(t != want for _, want in trashed_pred):
                    continue
                if all(p(f) for p in other):
                    items.append(st.public(f))
        size = min(int(qs.get("pageSize", 100)), 1000)
        start = int(qs.get("pageToken") or 0)
        page = items[start:start + size]
        out = {"files": page, "incompleteSearch": False}
        if start + size < len(items):
            out["nextPageToken"] = str(start + size)
        return filter_fields(out, qs.get("fields"))

    def _parent_ok(self, pid):
        f = self.store.files.get(pid)
        if f is None or f["mimeType"] != FOLDER or self.store.effectively_trashed(f):
            raise HttpError(404, "notFound", f"File not found: {pid}")

    def _create(self, meta: dict, data: bytes | None) -> dict:
        st = self.store
        with st.lock:
            fid = meta.get("id")
            if fid is None:
                fid = os.urandom(12).hex()
            elif fid in st.files:
                raise HttpError(409, "fileIdInUse", "A file already exists with the provided ID.")
            elif fid not in st.issued:
                raise HttpError(400, "fileIdNotUsable", "ID was not generated by generateIds")
            if not meta.get("name"):
                raise HttpError(400, "required", "name")
            parents = meta.get("parents") or ["root"]
            for p in parents:
                self._parent_ok(p)
            props = meta.get("appProperties") or {}
            _check_props(props)
            mime = meta.get("mimeType") or "application/octet-stream"
            f = {"id": fid, "name": meta["name"], "mimeType": mime, "parents": list(parents),
                 "appProperties": {k: v for k, v in props.items() if v is not None}}
            if mime != FOLDER:
                if mime.startswith("application/vnd.google-apps."):
                    data = b"CONVERTED:" + (data or b"")[::-1]
                f["_blob"] = st.put_bytes(data or b"")
            return st._mk(f)

    def _write(self, method, fid, meta, data):
        st = self.store
        if method == "POST":
            return self._create(meta, data)
        with st.lock:
            f = st.files.get(fid)
            if f is None:
                raise HttpError(404, "notFound", f"File not found: {fid}")
            if f["mimeType"] == FOLDER:
                raise HttpError(400, "invalid", "cannot upload content to a folder")
            self._patch_meta(f, {k: v for k, v in meta.items() if k != "mimeType"}, {})
            mime = meta.get("mimeType") or f["mimeType"]
            if mime.startswith("application/vnd.google-apps."):
                data = b"CONVERTED:" + data[::-1]
            f["mimeType"] = mime
            f["_blob"] = st.put_bytes(data)
            return f

    def _copy(self, src_id, meta):
        st = self.store
        with st.lock:
            src = st.files.get(src_id)
            if src is None:
                raise HttpError(404, "notFound", f"File not found: {src_id}")
            if src["mimeType"] == FOLDER:
                raise HttpError(403, "cannotCopyFile", "folders cannot be copied")
            merged = {"name": meta.get("name", src["name"]), "mimeType": src["mimeType"],
                      "parents": meta.get("parents") or src["parents"], "id": meta.get("id"),
                      "appProperties": {**src["appProperties"], **(meta.get("appProperties") or {})}}
            return self._create(merged, st.blobs[src["_blob"]])

    def _patch_meta(self, f, meta, qs):
        st = self.store
        if "name" in meta:
            if not meta["name"]:
                raise HttpError(400, "invalid", "empty name")
            f["name"] = meta["name"]
        if "appProperties" in meta:
            _check_props(meta["appProperties"] or {})
            for k, v in (meta["appProperties"] or {}).items():
                if v is None:
                    f["appProperties"].pop(k, None)
                else:
                    f["appProperties"][k] = v
        if "trashed" in meta:
            f["trashed"] = bool(meta["trashed"])
        add = [p for p in (qs.get("addParents") or "").split(",") if p]
        rem = [p for p in (qs.get("removeParents") or "").split(",") if p]
        for p in add:
            self._parent_ok(p)
        parents = [p for p in f["parents"] if p not in rem]
        for p in add:
            if p not in parents:
                parents.append(p)
        if len(parents) > 1:
            raise HttpError(403, "teamDrivesParentLimit", "an item can only have one parent")
        if not parents:
            raise HttpError(400, "invalid", "an item must have a parent")
        st.set_parents(f, parents)


def _parse_multipart(ctype: str, body: bytes):
    m = re.search(r"boundary=([^;]+)", ctype)
    if not m or not ctype.startswith("multipart/related"):
        raise HttpError(400, "badContent", "multipart/related with boundary required")
    b = ("--" + m.group(1)).encode()
    parts = body.split(b)
    # parts: [b"", part1, part2, b"--"]
    if len(parts) < 4 or not parts[-1].startswith(b"--"):
        raise HttpError(400, "badContent", "malformed multipart body")
    def split(part):
        head, _, payload = part.partition(b"\r\n\r\n")
        assert payload.endswith(b"\r\n")
        return head, payload[:-2]
    _, meta_raw = split(parts[1])
    _, data = split(parts[2])
    return json.loads(meta_raw), data


class FakeDrive:
    def __init__(self, token: str = "test-token"):
        self.store = Store(token)
        handler = type("H", (Handler,), {"store": self.store})
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        self.server.daemon_threads = True
        self.base = f"http://127.0.0.1:{self.server.server_address[1]}"
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    def __enter__(self):
        self.thread.start()
        return self

    def __exit__(self, *a):
        self.server.shutdown()
        self.server.server_close()

    # convenience for tests
    def new_folder(self, name: str, parent: str = "root") -> str:
        with self.store.lock:
            return self.store._mk({"id": os.urandom(10).hex(), "name": name, "mimeType": FOLDER,
                                   "parents": [parent]})["id"]

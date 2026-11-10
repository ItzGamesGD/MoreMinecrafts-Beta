"""Read-only access to the source Git repository.

All mirror decisions are derived from a freshly fetched bare repository plus a
`git ls-remote` taken after the fetch. Nothing here pushes or writes refs to
the remote.
"""
from __future__ import annotations

import hashlib
import os
import subprocess
import threading
from dataclasses import dataclass, field


class GitError(RuntimeError):
    pass


@dataclass(frozen=True)
class TreeEntry:
    path: str
    mode: str          # "100644", "100755", "120000" (symlink), "160000" (gitlink)
    type: str          # "blob" or "commit"
    oid: str
    size: int | None   # None for gitlinks


@dataclass
class RefSnapshot:
    """Remote ref state captured for one run."""
    heads: dict[str, str] = field(default_factory=dict)       # refs/heads/x -> commit sha
    tags: dict[str, str] = field(default_factory=dict)        # refs/tags/x -> tag/commit object sha
    tags_peeled: dict[str, str] = field(default_factory=dict) # refs/tags/x -> commit sha
    pulls: dict[str, str] = field(default_factory=dict)       # refs/pull/N/head|merge -> sha
    other: dict[str, str] = field(default_factory=dict)
    head_symref: str | None = None                            # e.g. refs/heads/main

    def pull_heads(self) -> dict[str, str]:
        return {r: s for r, s in self.pulls.items() if r.endswith("/head")}


class GitSource:
    def __init__(self, work_dir: str, repo_url: str, git: str = "git"):
        self.work_dir = work_dir
        self.repo_url = repo_url
        self.git = git
        self._batch = None
        self._batch_lock = threading.Lock()
        self._sha256_cache: dict[str, str] = {}

    # ------------------------------------------------------------------ plumbing
    def run(self, *args: str, input: bytes | None = None, check: bool = True) -> bytes:
        env = dict(os.environ)
        env.setdefault("GIT_TERMINAL_PROMPT", "0")
        proc = subprocess.run(
            [self.git, "--git-dir", self.work_dir, *args],
            input=input, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env,
        )
        if check and proc.returncode != 0:
            raise GitError(f"git {' '.join(args[:3])}... failed ({proc.returncode}): "
                           f"{proc.stderr.decode(errors='replace').strip()[:500]}")
        return proc.stdout

    def ensure_repo(self) -> None:
        if not os.path.isdir(os.path.join(self.work_dir, "objects")):
            os.makedirs(self.work_dir, exist_ok=True)
            subprocess.run([self.git, "init", "--bare", "-q", self.work_dir], check=True)
        # Never configure a push URL; the mirror is read-only toward GitHub.
        existing = self.run("remote", check=False).decode().split()
        if "origin" in existing:
            self.run("remote", "set-url", "origin", self.repo_url)
        else:
            self.run("remote", "add", "origin", self.repo_url)
        self.run("config", "remote.origin.pushurl", "DISABLED-read-only-mirror")

    # ------------------------------------------------------------------ refs
    def ls_remote(self) -> RefSnapshot:
        out = self.run("ls-remote", "--symref", "origin").decode()
        snap = RefSnapshot()
        peeled: dict[str, str] = {}
        for line in out.splitlines():
            if not line.strip():
                continue
            if line.startswith("ref: "):
                target, name = line[5:].split("\t", 1)
                if name == "HEAD":
                    snap.head_symref = target
                continue
            sha, name = line.split("\t", 1)
            if name.endswith("^{}"):
                peeled[name[:-3]] = sha
            elif name.startswith("refs/heads/"):
                snap.heads[name] = sha
            elif name.startswith("refs/tags/"):
                snap.tags[name] = sha
            elif name.startswith("refs/pull/"):
                snap.pulls[name] = sha
            elif name != "HEAD":
                snap.other[name] = sha
        for t, sha in snap.tags.items():
            snap.tags_peeled[t] = peeled.get(t, sha)
        return snap

    def fetch(self, include_tags: bool = True, include_pulls: bool = False) -> None:
        specs = ["+refs/heads/*:refs/mirror/heads/*"]
        if include_tags:
            specs.append("+refs/tags/*:refs/mirror/tags/*")
        if include_pulls:
            specs.append("+refs/pull/*/head:refs/mirror/pull/*/head")
        self.run("fetch", "--prune", "--no-tags", "--quiet", "origin", *specs)

    def local_refs(self) -> dict[str, str]:
        out = self.run("for-each-ref", "--format=%(refname) %(objectname)", "refs/mirror/").decode()
        res = {}
        for line in out.splitlines():
            name, sha = line.split(" ")
            res["refs/" + name[len("refs/mirror/"):]] = sha
        return res

    def snapshot(self, include_tags: bool = True, include_pulls: bool = False, attempts: int = 4) -> RefSnapshot:
        """Fetch, then confirm the fetched refs equal a post-fetch ls-remote.

        A push racing the fetch produces a mismatch; we refetch until stable so
        that every namespace is synced from a ref value the remote really had.
        """
        self.ensure_repo()
        last_diff = None
        for _ in range(attempts):
            self.fetch(include_tags, include_pulls)
            snap = self.ls_remote()
            local = self.local_refs()
            if not include_pulls:
                local = {r: s for r, s in local.items() if not r.startswith("refs/pull/")}
            remote = dict(snap.heads)
            if include_tags:
                remote.update(snap.tags)
            if include_pulls:
                remote.update(snap.pull_heads())
            if local == remote:
                return snap
            last_diff = sorted(set(local.items()) ^ set(remote.items()))[:10]
        raise GitError(f"remote refs kept changing during fetch; sample diff: {last_diff}")

    # ------------------------------------------------------------------ objects
    def peel_to_commit(self, sha: str) -> str:
        return self.run("rev-parse", f"{sha}^{{commit}}").decode().strip()

    def tree_of(self, commit: str) -> str:
        return self.run("rev-parse", f"{commit}^{{tree}}").decode().strip()

    def tree_entries(self, commit: str) -> dict[str, TreeEntry]:
        raw = self.run("ls-tree", "-r", "-l", "-z", "--full-tree", commit)
        entries: dict[str, TreeEntry] = {}
        for rec in raw.split(b"\0"):
            if not rec:
                continue
            meta, path_b = rec.split(b"\t", 1)
            mode, typ, oid, size = meta.decode().split()
            path = path_b.decode("utf-8")  # raises on non-UTF-8: such paths are reported, never skipped
            entries[path] = TreeEntry(path, mode, typ, oid, None if size == "-" else int(size))
        return entries

    def commit_meta(self, commit: str) -> dict:
        fmt = "%H%x00%T%x00%P%x00%an%x00%aI%x00%cn%x00%cI%x00%s"
        parts = self.run("show", "-s", f"--format={fmt}", commit).decode().split("\0")
        return {
            "sha": parts[0], "tree": parts[1], "parents": parts[2].split(),
            "author": parts[3], "authored_at": parts[4],
            "committer": parts[5], "committed_at": parts[6], "subject": parts[7].rstrip("\n"),
        }

    def is_ancestor(self, a: str, b: str) -> bool | None:
        if not self.has_object(a) or not self.has_object(b):
            return None
        proc = subprocess.run([self.git, "--git-dir", self.work_dir, "merge-base", "--is-ancestor", a, b])
        return proc.returncode == 0

    def has_object(self, sha: str) -> bool:
        proc = subprocess.run([self.git, "--git-dir", self.work_dir, "cat-file", "-e", sha],
                              stderr=subprocess.DEVNULL)
        return proc.returncode == 0

    def read_blob(self, oid: str) -> bytes:
        with self._batch_lock:
            if self._batch is None:
                self._batch = subprocess.Popen(
                    [self.git, "--git-dir", self.work_dir, "cat-file", "--batch"],
                    stdin=subprocess.PIPE, stdout=subprocess.PIPE)
            self._batch.stdin.write(oid.encode() + b"\n")
            self._batch.stdin.flush()
            header = self._batch.stdout.readline().decode().split()
            if len(header) != 3 or header[1] != "blob":
                raise GitError(f"object {oid} is not a readable blob: {header}")
            size = int(header[2])
            data = self._batch.stdout.read(size)
            self._batch.stdout.read(1)  # trailing LF
        if git_blob_sha1(data) != oid:
            raise GitError(f"blob {oid} failed object-id verification")
        return data

    def blob_sha256(self, oid: str) -> str:
        h = self._sha256_cache.get(oid)
        if h is None:
            h = hashlib.sha256(self.read_blob(oid)).hexdigest()
            self._sha256_cache[oid] = h
        return h

    def close(self) -> None:
        if self._batch is not None:
            self._batch.stdin.close()
            self._batch.wait()
            self._batch = None


def git_blob_sha1(data: bytes) -> str:
    h = hashlib.sha1()
    h.update(b"blob %d\0" % len(data))
    h.update(data)
    return h.hexdigest()

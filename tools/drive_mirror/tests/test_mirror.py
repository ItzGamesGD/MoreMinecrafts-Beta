"""End-to-end tests: real git remote + real CLI subprocesses + Drive emulator.

Run:  PYTHONPATH=tools python3 -m unittest discover -s tools/drive_mirror/tests -v
"""
from __future__ import annotations

import gzip
import json
import os
import random
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, TOOLS)
sys.path.insert(0, HERE)

from fake_drive import FakeDrive  # noqa: E402
from drive_mirror.layout import encode_component, decode_component  # noqa: E402

MIRROR = "mirror-under-test"


def sh(*args, cwd=None, env=None, check=True):
    p = subprocess.run(args, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if check and p.returncode:
        raise AssertionError(f"{args} failed: {p.stderr.decode()[-2000:]}")
    return p


class Repo:
    """A bare 'remote' plus a working clone used to author commits."""

    def __init__(self, root):
        self.remote = os.path.join(root, "remote.git")
        self.work = os.path.join(root, "work")
        sh("git", "init", "-q", "--bare", "-b", "main", self.remote)
        sh("git", "init", "-q", "-b", "main", self.work)
        for k, v in [("user.name", "t"), ("user.email", "t@example.invalid"), ("commit.gpgsign", "false"),
                     ("core.autocrlf", "false")]:
            self.git("config", k, v)
        self.git("remote", "add", "origin", self.remote)

    def git(self, *a, check=True):
        return sh("git", *a, cwd=self.work, check=check)

    def write(self, path, data: bytes | str, mode=None):
        full = os.path.join(self.work, path)
        os.makedirs(os.path.dirname(full) or self.work, exist_ok=True)
        with open(full, "wb") as fh:
            fh.write(data.encode() if isinstance(data, str) else data)
        if mode:
            os.chmod(full, mode)

    def commit(self, msg):
        self.git("add", "-A")
        self.git("commit", "-q", "--allow-empty", "-m", msg)
        return self.git("rev-parse", "HEAD").stdout.decode().strip()

    def push(self, *refs, force=False):
        self.git("push", "-q", *(["--force"] if force else []), "origin", *refs)


class MirrorTestCase(unittest.TestCase):
    maxDiff = None

    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="drive-mirror-test-")
        self.repo = Repo(self.tmp)
        self.fake = FakeDrive().__enter__()
        self.parent = self.fake.new_folder("Mirrors")
        self.env = dict(os.environ, PYTHONPATH=TOOLS, DRIVE_ACCESS_TOKEN="test-token",
                        DRIVE_AUTH_MODE="static", DRIVE_API_BASE=self.fake.base)
        self.work_dir = os.path.join(self.tmp, "cache.git")

    def tearDown(self):
        self.fake.__exit__()
        shutil.rmtree(self.tmp, ignore_errors=True)

    # ------------------------------------------------------------------ helpers
    def cli(self, cmd, *extra, expect=0, env=None):
        report = os.path.join(self.tmp, f"report-{cmd}-{time.time_ns()}.json")
        args = [sys.executable, "-m", "drive_mirror", cmd, "--repo-url", self.repo.remote,
                "--work-dir", self.work_dir, "--report", report]
        if cmd in ("sync", "verify"):
            args += ["--mirror-name", MIRROR, "--parent-folder-id", self.parent]
        if cmd == "sync":
            args += ["--lock-settle-s", "0.05", "--workers", "4"]
        p = subprocess.run(args + list(extra), env=env or self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if expect is not None and p.returncode != expect:
            self.fail(f"{cmd} exit {p.returncode} != {expect}\nstdout:{p.stdout.decode()[-3000:]}\n"
                      f"stderr:{p.stderr.decode()[-3000:]}")
        rep = None
        if os.path.exists(report):
            with open(report) as fh:
                rep = json.load(fh)
        return p.returncode, rep

    def sync(self, *extra, expect=0):
        return self.cli("sync", *extra, expect=expect)[1]

    def verify_pass(self, *extra):
        code, rep = self.cli("verify", *extra, expect=None)
        if rep["result"] != "PASS":
            bad = {r: v["problems"] for r, v in rep["namespaces"].items() if v["problems"]}
            self.fail(f"verify FAIL: global={rep['global']['problems']} ns={bad}")
        self.assertEqual(code, 0)
        return rep

    def drive_snapshot(self, include_meta=False):
        st = self.fake.store
        with st.lock:
            snap = {}
            for f in st.files.values():
                role = f["appProperties"].get("mm_role")
                if not include_meta and role in ("metafile",):
                    continue
                snap[f["id"]] = (f["name"], tuple(f["parents"]), f.get("_blob"), f["trashed"],
                                 tuple(sorted(f["appProperties"].items())))
            return snap

    def ns_folder(self, ref_short, container="branches"):
        st = self.fake.store
        name = encode_component(ref_short)
        with st.lock:
            cont = [f for f in st.files.values() if f["name"] == container and f["mimeType"].endswith("folder")
                    and not st.effectively_trashed(f)]
            assert len(cont) == 1, cont
            hits = [f for f in st.files.values() if f["parents"] == [cont[0]["id"]] and f["name"] == name
                    and not st.effectively_trashed(f)]
            return hits[0] if hits else None

    def drive_file(self, ref_short, path):
        st = self.fake.store
        node = self.ns_folder(ref_short)
        for part in path.split("/"):
            with st.lock:
                kids = [f for f in st.files.values() if f["parents"] == [node["id"]] and f["name"] == part
                        and not st.effectively_trashed(f)]
            if not kids:
                return None
            node = kids[0]
        return node

    def seed(self):
        r = self.repo
        rnd = random.Random(7)
        r.write("README.md", "# hello\n")
        r.write("src/main/java/a/B.java", "class B {}\n")
        r.write("assets/tex.png", bytes(rnd.getrandbits(8) for _ in range(5000)))
        r.write("data/structure.nbt", gzip.compress(b"\x0a\x00\x00" + bytes(range(256)) * 20, mtime=0))
        r.write("empty.txt", b"")
        r.write("gradlew", "#!/bin/sh\necho hi\n", mode=0o755)
        r.write("docs/한글 파일 #1 %20 'q'.md", "unicode path\n")
        r.write("deep/a/b/c/d/e/f.txt", "deep\n")
        r.write("crlf.txt", b"a\r\nb\r\n")
        r.write("big/blob.bin", os.urandom(6 * 1024 * 1024 + 123))   # forces the resumable upload path
        os.symlink("README.md", os.path.join(r.work, "link-to-readme"))
        main = r.commit("seed")
        r.git("checkout", "-q", "-b", "feature/sky-tower")
        r.write("feature.txt", "feature only\n")
        r.commit("feature")
        r.git("checkout", "-q", "main")
        r.git("tag", "v1-light")
        r.git("tag", "-a", "v1-annotated", "-m", "annotated")
        r.push("main", "feature/sky-tower", "v1-light", "v1-annotated")
        return main


class EndToEnd(MirrorTestCase):
    def test_full_lifecycle(self):
        self.seed()
        # 1. initial sync
        rep = self.sync()
        self.assertEqual(rep["status"], "complete")
        v = self.verify_pass("--deep-sample", "1000")
        self.assertEqual(v["global"]["expected_namespaces"], 4)       # 2 branches + 2 tags
        self.assertEqual(v["global"]["deep_downloads"]["mismatched"], 0)
        f = self.drive_file("main", "docs/한글 파일 #1 %20 'q'.md")
        self.assertIsNotNone(f)
        self.assertIsNotNone(self.ns_folder("feature/sky-tower"))
        self.assertEqual(self.ns_folder("feature/sky-tower")["name"], "feature%2Fsky-tower")
        link = self.drive_file("main", "link-to-readme")
        self.assertEqual(self.fake.store.blobs[link["_blob"]], b"README.md")
        self.assertEqual(link["appProperties"]["mm_mode"], "120000")
        self.assertEqual(self.drive_file("main", "gradlew")["appProperties"]["mm_mode"], "100755")
        # identical content across branches is copied server-side, not re-uploaded
        feat = next(n for n in rep["namespaces"] if n["ref"] == "refs/heads/feature/sky-tower")
        main = next(n for n in rep["namespaces"] if n["ref"] == "refs/heads/main")
        self.assertEqual(main["copies"] + main["uploads"], 11)
        self.assertGreaterEqual(feat["copies"], 10)

        # 2. idempotency: nothing in any namespace changes
        before = self.drive_snapshot()
        rep2 = self.sync()
        self.assertTrue(all(n["action"] == "noop" for n in rep2["namespaces"]), rep2["namespaces"])
        self.assertEqual(before, self.drive_snapshot())
        self.verify_pass()

        # 3. add / modify / delete / rename / rename+modify / mode change / dir delete
        r = self.repo
        renamed_id = self.drive_file("main", "src/main/java/a/B.java")["id"]
        os.makedirs(os.path.join(r.work, "src/main/java/z"))
        r.git("mv", "src/main/java/a/B.java", "src/main/java/z/Moved.java")
        r.write("README.md", "# hello v2\n")
        r.write("new/added.json", '{"x": 1}\n')
        os.remove(os.path.join(r.work, "crlf.txt"))
        shutil.rmtree(os.path.join(r.work, "deep"))
        r.git("mv", "empty.txt", "renamed-and-modified.txt")
        r.write("renamed-and-modified.txt", "now has content\n")
        os.chmod(os.path.join(r.work, "assets/tex.png"), 0o755)
        r.commit("changes")
        r.push("main")
        rep3 = self.sync()
        m3 = next(n for n in rep3["namespaces"] if n["ref"] == "refs/heads/main")
        self.assertEqual(m3["source"], "manifest")
        self.assertEqual(m3["moves"], 1)
        self.assertEqual(self.drive_file("main", "src/main/java/z/Moved.java")["id"], renamed_id)
        self.assertIsNone(self.drive_file("main", "deep"))
        self.assertIsNone(self.drive_file("main", "src/main/java/a"))
        self.assertEqual(self.drive_file("main", "assets/tex.png")["appProperties"]["mm_mode"], "100755")
        self.verify_pass("--deep-sample", "1000")

        # 4. branch creation (server-side copies) and deletion (retired, not trashed)
        r.git("checkout", "-q", "-b", "work/st01-p8b2")
        r.commit("empty")
        r.push("work/st01-p8b2")
        rep4 = self.sync()
        n4 = next(n for n in rep4["namespaces"] if n["ref"] == "refs/heads/work/st01-p8b2")
        self.assertEqual(n4["action"], "created")
        self.assertEqual(n4["uploads"], 0)
        self.verify_pass()
        r.git("push", "-q", "origin", "--delete", "feature/sky-tower")
        rep5 = self.sync()
        self.assertEqual([x["ref"] for x in rep5["retired"]], ["refs/heads/feature/sky-tower"])
        self.assertIsNone(self.ns_folder("feature/sky-tower"))
        retired = [f for f in self.fake.store.files.values() if f["name"].startswith("branches__feature%2Fsky-tower@")]
        self.assertEqual(len(retired), 1)
        self.assertFalse(self.fake.store.effectively_trashed(retired[0]))
        self.verify_pass()

        # 5. force-push to an unrelated history leaves no stale files
        r.git("checkout", "-q", "--orphan", "rewrite")
        r.git("rm", "-q", "-rf", ".")
        r.write("only-this.txt", "rewritten history\n")
        r.write("README.md", "# hello v2\n")
        r.commit("orphan")
        r.git("push", "-q", "--force", "origin", "rewrite:work/st01-p8b2")
        rep6 = self.sync()
        n6 = next(n for n in rep6["namespaces"] if n["ref"] == "refs/heads/work/st01-p8b2")
        self.assertTrue(n6["forced_update"])
        self.assertIsNone(self.drive_file("work/st01-p8b2", "big"))
        v6 = self.verify_pass()
        self.assertEqual(v6["namespaces"]["refs/heads/work/st01-p8b2"]["files"], 2)

        # 6. a re-created branch gets a fresh namespace incarnation
        r.git("checkout", "-q", "main")
        r.git("push", "-q", "origin", "main:feature/sky-tower")
        self.sync()
        self.verify_pass()

    def test_metadata_and_manifest(self):
        main = self.seed()
        self.sync()
        st = self.fake.store
        meta = {f["name"]: f for f in st.files.values() if f["appProperties"].get("mm_role") == "metafile"
                and not st.effectively_trashed(f)}
        state = json.loads(st.blobs[meta["state.json"]["_blob"]])
        refs = json.loads(st.blobs[meta["refs.json"]["_blob"]])
        man = json.loads(st.blobs[meta["branches__main.json"]["_blob"]])
        self.assertEqual(state["namespaces"]["refs/heads/main"]["commit"], main)
        self.assertEqual(refs["heads"]["refs/heads/main"], main)
        self.assertEqual(refs["head_symref"], "refs/heads/main")
        self.assertNotEqual(refs["tags"]["refs/tags/v1-annotated"], refs["tags_peeled"]["refs/tags/v1-annotated"])
        self.assertEqual(man["commit"]["sha"], main)
        self.assertEqual(man["file_count"], 11)
        blob = self.repo.git("rev-parse", "HEAD:big/blob.bin").stdout.decode().strip()
        self.assertEqual(man["files"]["big/blob.bin"]["blob"], blob)
        self.assertEqual(state["last_run"]["status"], "complete")

    def test_interrupted_sync_recovers(self):
        self.seed()
        self.sync()
        r = self.repo
        r.git("checkout", "-q", "-b", "bulk")
        for i in range(400):
            r.write(f"bulk/{i // 50}/f{i}.txt", f"content {i} {os.urandom(8).hex()}\n")
        r.commit("bulk")
        r.push("bulk")
        args = [sys.executable, "-m", "drive_mirror", "sync", "--repo-url", r.remote, "--work-dir", self.work_dir,
                "--mirror-name", MIRROR, "--parent-folder-id", self.parent, "--lock-settle-s", "0.05",
                "--workers", "2"]
        start = self.fake.store.counters["writes"]
        proc = subprocess.Popen(args, env=self.env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        while self.fake.store.counters["writes"] - start < 150 and proc.poll() is None:
            time.sleep(0.01)
        proc.send_signal(signal.SIGKILL)
        proc.wait()
        code, rep = self.cli("verify", expect=None)
        self.assertEqual(rep["result"], "FAIL")                      # interrupted state is detected
        self.assertIn("state_incomplete", rep["namespaces"]["refs/heads/bulk"]["problems"])
        # the dead run's lease must not block for its whole TTL when explicitly broken
        rep2 = self.sync("--break-lock")
        bulk = next(n for n in rep2["namespaces"] if n["ref"] == "refs/heads/bulk")
        self.assertEqual(bulk["source"], "crawl")
        v = self.verify_pass()                                       # includes duplicate/extra-file checks
        self.assertEqual(v["namespaces"]["refs/heads/bulk"]["files"], 411)
        self.assertEqual(v["namespaces"]["refs/heads/bulk"]["problems"], {})

    def test_lease_blocks_concurrent_writer(self):
        self.seed()
        self.sync()
        st = self.fake.store
        lock = next(f for f in st.files.values() if f["name"] == "lock.json")
        st.blobs["held"] = json.dumps({"owner": "other-run", "token": "x", "expires_at": time.time() + 600,
                                       "released": False}).encode()
        lock["_blob"] = "held"
        code, rep = self.cli("sync", expect=3)
        self.assertEqual(rep["result"], "LOCKED")

    def test_transient_errors_and_rate_limits_are_retried(self):
        self.seed()
        self.fake.store.faults.update(fail_every=7, ratelimit_every=11, lose_response_every=5)
        rep = self.sync()
        self.assertGreater(rep["drive_stats"].get("retries", 0), 10)
        # writes that succeeded server-side but lost their response were not duplicated
        self.assertGreater(rep["drive_stats"].get("create_conflict_recovered", 0), 0)
        self.fake.store.faults.update(fail_every=0, ratelimit_every=0, lose_response_every=0)
        self.verify_pass()

    def test_drift_detected_and_reconciled(self):
        self.seed()
        self.sync()
        st = self.fake.store
        readme = self.drive_file("main", "README.md")
        gradlew = self.drive_file("main", "gradlew")
        nbt = self.drive_file("main", "data/structure.nbt")
        with st.lock:
            readme["_blob"] = st.put_bytes(b"tampered")                 # content drift
            gradlew["trashed"] = True                                   # missing file
            st._mk({"id": "extra1", "name": "stray.txt", "mimeType": "text/plain",
                    "parents": [self.ns_folder("main")["id"]], "_blob": st.put_bytes(b"x")})
            st._mk({"id": "gdoc1", "name": "structure.nbt", "mimeType": "application/vnd.google-apps.document",
                    "parents": nbt["parents"], "_blob": st.put_bytes(b"doc")})  # converted duplicate
        code, rep = self.cli("verify", expect=1)
        probs = rep["namespaces"]["refs/heads/main"]["problems"]
        for k in ("checksum_mismatch", "file_missing", "extra_file", "duplicate_file"):
            self.assertIn(k, probs)
        # incremental trusts the manifest and cannot see drift; reconcile crawls and repairs
        self.sync("--mode", "reconcile")
        self.verify_pass("--deep-sample", "1000")

    def test_listing_lag_is_tolerated_by_verify(self):
        self.seed()
        self.sync()
        self.repo.write("late.txt", "late\n")
        self.repo.commit("late")
        self.repo.push("main")
        cutoff = time.time()
        self.sync()
        self.fake.store.faults["list_hide_created_after"] = cutoff   # files from the last sync not yet listed
        code, rep = self.cli("verify", expect=None)
        main = rep["namespaces"]["refs/heads/main"]
        self.assertIn("listing_lag_resolved_by_get", main["problems"])
        self.assertEqual(rep["result"], "PASS")

    def test_mass_retire_guard_and_dry_run(self):
        self.seed()
        r = self.repo
        for i in range(8):
            r.git("branch", f"tmp/{i}")
        r.git("push", "-q", "origin", *[f"tmp/{i}" for i in range(8)])
        self.sync()
        r.git("push", "-q", "origin", "--delete", *[f"tmp/{i}" for i in range(8)])
        before = self.drive_snapshot(include_meta=True)
        rep = self.sync("--dry-run")
        self.assertEqual(len(rep["retired"]), 8)
        self.assertEqual(before, self.drive_snapshot(include_meta=True))   # dry run wrote nothing
        code, rep = self.cli("sync", expect=5)
        self.assertEqual(rep["result"], "SAFETY_STOP")
        rep = self.sync("--allow-mass-retire")
        self.assertEqual(len(rep["retired"]), 8)
        self.verify_pass()

    def test_time_budget_produces_resumable_incomplete_run(self):
        self.seed()
        code, rep = self.cli("sync", "--time-budget-min", "0.0001", expect=2)
        self.assertEqual(rep["status"], "incomplete")
        self.sync()
        self.verify_pass()

    def test_pull_heads_orphaned(self):
        self.seed()
        r = self.repo
        r.git("checkout", "-q", "-b", "gone")
        r.write("orphan-work.txt", "only reachable via PR ref\n")
        r.commit("pr work")
        sha = r.git("rev-parse", "HEAD").stdout.decode().strip()
        r.git("push", "-q", "origin", f"{sha}:refs/pull/7/head", "main:refs/pull/8/head")
        rep = self.sync("--pull-heads", "orphaned")
        refs = {n["ref"] for n in rep["namespaces"]}
        self.assertIn("refs/pull/7/head", refs)
        self.assertNotIn("refs/pull/8/head", refs)     # contained in main: metadata only
        self.verify_pass("--pull-heads", "orphaned")

    def test_blocked_without_credentials(self):
        self.seed()
        env = {k: v for k, v in self.env.items() if not k.startswith("DRIVE_")}
        env["DRIVE_API_BASE"] = self.fake.base
        code, rep = self.cli("sync", expect=4, env=env)
        self.assertEqual(rep["result"], "BLOCKED")
        env["DRIVE_AUTH_MODE"], env["DRIVE_ACCESS_TOKEN"] = "static", "wrong"
        code, rep = self.cli("sync", expect=4, env=env)
        self.assertEqual(rep["result"], "BLOCKED")

    def test_preflight(self):
        args = [sys.executable, "-m", "drive_mirror", "preflight", "--parent-folder-id", self.parent,
                "--expected-bytes", "1000", "--write-probe"]
        p = subprocess.run(args, env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out = json.loads(p.stdout)
        self.assertEqual(p.returncode, 0, out)
        self.assertEqual(out["result"], "PASS")
        self.assertTrue(out["checks"]["write_probe"]["ok"])
        self.assertNotIn("example.invalid", p.stdout.decode())          # no account e-mail in logs
        self.assertNotIn(self.parent, p.stdout.decode())                # no folder ids either


class Units(unittest.TestCase):
    def test_component_encoding_roundtrip(self):
        for name in ["main", "work/st01-p8b2-royal-chest-runtime-20261110", "a%2Fb", "한글/브랜치", "x y#z"]:
            enc = encode_component(name)
            self.assertNotIn("/", enc)
            self.assertEqual(decode_component(enc), name)
        self.assertNotEqual(encode_component("a/b"), encode_component("a%2Fb"))


if __name__ == "__main__":
    unittest.main()

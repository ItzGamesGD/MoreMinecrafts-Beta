"""Post-audit hardening tests: PR-only refs, ref disappearance, manifest mapping, namespace
isolation, current-state reproducibility, security gate, reconcile cadence, scoped verification.

Same approach as test_mirror.py: a real git remote, the real CLI as subprocesses, the Drive emulator.
These tests do not touch Google Drive.
"""
from __future__ import annotations

import http.server
import json
import os
import subprocess
import sys
import threading
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.dirname(os.path.dirname(HERE)))

from test_mirror import MIRROR, MirrorTestCase, sh  # noqa: E402

SECRET_MARKER = "SUPERSECRETVALUE0123456789"


def write_json(path, obj):
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh)


def read_json(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def head(repo, ref="HEAD"):
    return repo.git("rev-parse", ref).stdout.decode().strip()


class StubGitHub:
    """Tiny GitHub REST stand-in: GET /repos/o/r/pulls[/N] only."""

    def __init__(self, prs: dict[int, dict]):
        self.prs = prs
        outer = self

        class H(http.server.BaseHTTPRequestHandler):
            def log_message(self, *a):
                pass

            def do_GET(self):
                path = self.path.split("?")[0]
                if path == "/repos/o/r/pulls":
                    body, code = list(outer.prs.values()), 200
                elif path.startswith("/repos/o/r/pulls/") and int(path.rsplit("/", 1)[1]) in outer.prs:
                    body, code = outer.prs[int(path.rsplit("/", 1)[1])], 200
                else:
                    body, code = {"message": "Not Found"}, 404
                data = json.dumps(body).encode()
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)

        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), H)
        self.server.daemon_threads = True
        self.url = f"http://127.0.0.1:{self.server.server_address[1]}"
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def close(self):
        self.server.shutdown()
        self.server.server_close()

    @staticmethod
    def pr(n, head_ref, head_sha, state="closed", merged=False):
        return {"number": n, "title": f"PR {n}", "state": state, "draft": False, "merged_at": None,
                "created_at": "2026-01-01T00:00:00Z", "updated_at": "2026-01-02T00:00:00Z", "closed_at": None,
                "user": {"login": "u"}, "head": {"ref": head_ref, "sha": head_sha, "repo": {"full_name": "o/r"}},
                "base": {"ref": "main", "sha": "0" * 40}, "merge_commit_sha": None,
                "html_url": f"https://example.invalid/pull/{n}"}


class PrRefs(MirrorTestCase):
    def pr_only_commit(self, n=7):
        r = self.repo
        r.git("checkout", "-q", "-b", f"tmp-pr-{n}", "main")
        r.write(f"pr{n}-only.txt", f"only reachable through refs/pull/{n}/head\n")
        sha = r.commit(f"pr {n} work")
        r.git("push", "-q", "origin", f"{sha}:refs/pull/{n}/head")
        r.git("checkout", "-q", "main")
        r.git("branch", "-q", "-D", f"tmp-pr-{n}")
        return sha

    # 1 + 3 + 4 ------------------------------------------------------------------------------
    def test_pr_only_refs_are_in_default_scope_with_exact_manifests_and_isolated_namespaces(self):
        self.seed()
        r = self.repo
        sha7 = self.pr_only_commit(7)
        # a PR head that a branch contains is NOT PR-only: metadata only, no namespace
        r.git("push", "-q", "origin", "main:refs/pull/8/head")
        # a *branch* whose name looks like a PR ref must not collide with the PR namespace
        r.git("checkout", "-q", "-b", "pull/7/head", "main")
        r.write("branch-named-like-pr.txt", "ordinary branch\n")
        r.commit("branch pull/7/head")
        r.push("pull/7/head")
        r.git("checkout", "-q", "main")

        rep = self.sync()                                  # NOTE: no --pull-heads flag: default scope
        refs = {n["ref"] for n in rep["namespaces"]}
        self.assertIn("refs/pull/7/head", refs)
        self.assertNotIn("refs/pull/8/head", refs)
        self.assertIn("refs/heads/pull/7/head", refs)
        self.assertEqual(rep["source"]["pull_heads_advertised"], 2)
        self.assertEqual(rep["source"]["pull_heads_mirrored"], 1)

        # isolation: different containers, different folders, different content
        pr_ns = self.ns_folder("7", container="pulls")
        br_ns = self.ns_folder("pull/7/head")
        self.assertIsNotNone(pr_ns)
        self.assertIsNotNone(br_ns)
        self.assertNotEqual(pr_ns["id"], br_ns["id"])
        self.assertEqual(br_ns["name"], "pull%2F7%2Fhead")
        st = self.fake.store
        with st.lock:
            parent = {f["id"]: f for f in st.files.values()}
            self.assertEqual(parent[pr_ns["parents"][0]]["name"], "pulls")
            self.assertEqual(parent[br_ns["parents"][0]]["name"], "branches")

        def child(ns, name):
            with st.lock:
                return [f for f in st.files.values() if f["parents"] == [ns["id"]] and f["name"] == name
                        and not st.effectively_trashed(f)]
        self.assertEqual(len(child(pr_ns, "pr7-only.txt")), 1)
        self.assertEqual(len(child(pr_ns, "branch-named-like-pr.txt")), 0)
        self.assertEqual(len(child(br_ns, "branch-named-like-pr.txt")), 1)
        self.assertEqual(len(child(br_ns, "pr7-only.txt")), 0)

        # exact ref -> manifest mapping, for every namespace of every kind
        remote = {}
        out = sh("git", "ls-remote", self.repo.remote).stdout.decode().splitlines()
        for line in out:
            sha, ref = line.split("\t")
            if not ref.endswith("^{}"):
                remote[ref] = sha
        meta = {f["name"]: json.loads(st.blobs[f["_blob"]]) for f in st.files.values()
                if f["appProperties"].get("mm_role") == "metafile" and "__" in f["name"]
                and not st.effectively_trashed(f)}
        mirrored = {n["ref"] for n in rep["namespaces"]}
        self.assertEqual(len(meta), len(mirrored))                 # one manifest per ref, no more
        for ref in mirrored:
            kind = "pulls" if ref.startswith("refs/pull/") else "tags" if ref.startswith("refs/tags/") else "branches"
            short = ref.split("/", 2)[2] if kind != "pulls" else ref.split("/")[2]
            name = f"{kind}__{short.replace('/', '%2F')}.json"
            self.assertIn(name, meta, ref)
            man = meta[name]
            self.assertEqual(man["ref"], ref)
            self.assertEqual(man["source"]["ref"], ref)
            expect_sha = remote[ref] if kind != "tags" else json.loads(
                st.blobs[next(f["_blob"] for f in st.files.values() if f["name"] == "refs.json"
                              and not st.effectively_trashed(f))])["tags_peeled"][ref]
            self.assertEqual(man["commit"]["sha"], expect_sha, ref)
            self.assertEqual(man["source"]["kind"], {"pulls": "pull_head", "tags": "tag", "branches": "branch"}[kind])
        self.assertEqual(meta["pulls__7.json"]["commit"]["sha"], sha7)
        self.assertFalse(meta["pulls__7.json"]["source"]["contained_in_any_branch"])
        self.verify_pass("--deep-sample", "1000")

    def test_pr_metadata_is_recorded_in_pr_manifest(self):
        self.seed()
        sha7 = self.pr_only_commit(7)
        gh = StubGitHub({7: StubGitHub.pr(7, "feature/old", sha7)})
        try:
            env = dict(self.env, GITHUB_API_URL=gh.url)
            code, rep = self.cli("sync", "--github-repo", "o/r", env=env)
        finally:
            gh.close()
        self.assertEqual(rep["status"], "complete")
        st = self.fake.store
        man = json.loads(st.blobs[next(f["_blob"] for f in st.files.values() if f["name"] == "pulls__7.json")])
        self.assertEqual(man["source"]["pr"]["head_ref"], "feature/old")
        self.assertEqual(man["source"]["pr"]["state"], "closed")

    # 2 ---------------------------------------------------------------------------------------
    def test_pr_ref_disappearance_distinguishes_inaccessible_from_deleted(self):
        self.seed()
        sha7 = self.pr_only_commit(7)
        gh = StubGitHub({7: StubGitHub.pr(7, "x", sha7)})
        env = dict(self.env, GITHUB_API_URL=gh.url)
        try:
            self.cli("sync", "--github-repo", "o/r", env=env)
            self.verify_pass()
            # The head ref vanishes from the remote while the PR still exists on GitHub: anomaly, not deletion.
            sh("git", "--git-dir", self.repo.remote, "update-ref", "-d", "refs/pull/7/head")
            code, rep = self.cli("sync", "--github-repo", "o/r", env=env)
            self.assertEqual(code, 0)
            self.assertEqual(rep["retired"], [])
            self.assertIn("refs/pull/7/head", rep["retire_deferred"])
            self.assertIn("inaccessible", rep["retire_deferred"]["refs/pull/7/head"])
            self.assertIsNotNone(self.ns_folder("7", container="pulls"))     # nothing moved
            code, v = self.cli("verify", expect=None)
            self.assertEqual(v["result"], "FAIL")
            self.assertIn("ref_unconfirmed", v["global"]["problems"])
            # Now GitHub also says the PR does not exist: deletion is confirmed, namespace is retired, not trashed.
            gh.prs.clear()
            code, rep = self.cli("sync", "--github-repo", "o/r", env=env)
            self.assertEqual([x["ref"] for x in rep["retired"]], ["refs/pull/7/head"])
            self.assertIsNone(self.ns_folder("7", container="pulls"))
            retired = [f for f in self.fake.store.files.values() if f["name"].startswith("pulls__7@")]
            self.assertEqual(len(retired), 1)
            self.assertFalse(self.fake.store.effectively_trashed(retired[0]))
            self.verify_pass()
        finally:
            gh.close()

    def test_pr_namespace_is_sticky_when_a_branch_later_contains_the_head(self):
        self.seed()
        sha7 = self.pr_only_commit(7)
        self.sync()
        self.assertIsNotNone(self.ns_folder("7", container="pulls"))
        self.repo.git("push", "-q", "origin", f"{sha7}:refs/heads/adopted-pr7")      # a branch now contains it
        rep = self.sync()
        self.assertEqual(rep["retired"], [])
        self.assertIn("refs/pull/7/head", rep["pull_heads_sticky"])
        self.assertIsNotNone(self.ns_folder("7", container="pulls"))
        self.verify_pass()

    def test_branch_probe_failure_is_inaccessible_not_deleted(self):
        """In-process: the full listing omits a branch and the targeted probe errors out."""
        from drive_mirror.drive import DriveClient
        from drive_mirror.auth import StaticToken
        from drive_mirror.gitsrc import GitSource
        from drive_mirror.sync import SyncConfig, SyncRun

        self.seed()
        self.sync()
        self.repo.git("push", "-q", "origin", "--delete", "feature/sky-tower")

        class Unreachable(GitSource):
            def probe_ref(self, ref):
                return None

        drive = DriveClient(StaticToken("test-token"), base=self.fake.base)
        cfg = SyncConfig(mirror_name=MIRROR, parent_folder_id=self.parent, repo_url=self.repo.remote,
                         lock_settle_s=0.05, allow_ungated=True)
        rep = SyncRun(drive, Unreachable(self.work_dir, self.repo.remote), cfg).run()
        self.assertEqual(rep["retired"], [])
        self.assertIn("inaccessible", rep["retire_deferred"]["refs/heads/feature/sky-tower"])
        self.assertIsNotNone(self.ns_folder("feature/sky-tower"))
        # with a probe that succeeds and finds nothing, the deletion is confirmed
        rep = self.sync()
        self.assertEqual([x["ref"] for x in rep["retired"]], ["refs/heads/feature/sky-tower"])

    # mismatch detection ---------------------------------------------------------------------
    def test_verifier_detects_manifest_ref_mismatch_and_orphan_manifest(self):
        self.seed()
        self.sync()
        st = self.fake.store
        with st.lock:
            man = next(f for f in st.files.values() if f["name"] == "branches__main.json"
                       and not st.effectively_trashed(f))
            doc = json.loads(st.blobs[man["_blob"]])
            doc["ref"] = "refs/heads/other"
            man["_blob"] = st.put_bytes(json.dumps(doc).encode())
            orphan = next(f for f in st.files.values() if f["name"] == "branches__main.json")
            st._mk({"id": "orphanmf", "name": "branches__ghost.json", "mimeType": "application/json",
                    "parents": orphan["parents"], "_blob": st.put_bytes(b"{}")})
        code, rep = self.cli("verify", expect=1)
        self.assertIn("manifest_ref_mismatch", rep["namespaces"]["refs/heads/main"]["problems"])
        self.assertIn("manifest_without_namespace", rep["global"]["problems"])


class ResumableUploadRetry(MirrorTestCase):
    def test_lost_response_on_resumable_put_does_not_duplicate_or_fail(self):
        """The final PUT of a large upload applies server-side but its response is lost; the retry restarts
        the session with the same pre-generated id. That must be recognised as 'already created'."""
        from drive_mirror.auth import StaticToken
        from drive_mirror.drive import DriveClient

        drive = DriveClient(StaticToken("test-token"), base=self.fake.base, backoff_base=0.01)
        data = os.urandom(6 * 1024 * 1024 + 7)                         # above the 5 MiB resumable threshold
        self.fake.store.faults["lose_next_put"] = True
        item = drive.create_file("big.bin", self.parent, data, "application/octet-stream", {"mm_role": "file"})
        self.assertEqual(drive.stats["create_conflict_recovered"], 1)
        self.assertEqual(item["size"], str(len(data)))
        self.assertEqual(drive.download(item["id"]), data)
        with self.fake.store.lock:
            same = [f for f in self.fake.store.files.values() if f["name"] == "big.bin"]
        self.assertEqual(len(same), 1)                                  # no duplicate


class IndexReproducibility(MirrorTestCase):
    def inventory(self, name):
        out = os.path.join(self.tmp, name)
        sh(sys.executable, "-m", "drive_mirror", "inventory", "--repo-url", self.repo.remote,
           "--work-dir", self.work_dir, "--report", out, env=self.env)
        return out

    def test_lineage_and_current_state_are_reproducible(self):
        self.seed()
        r = self.repo
        r.git("checkout", "-q", "-b", "checkpoint/wave1-unified-20260101", "main")
        r.write("cp.txt", "checkpoint\n")
        r.commit("cp")
        r.git("checkout", "-q", "-b", "work/a", "checkpoint/wave1-unified-20260101")
        r.write("a.txt", "a\n")
        r.commit("a")
        r.git("checkout", "-q", "-b", "work/b", "work/a")
        r.write("b.txt", "b\n")
        r.commit("b")
        r.push("checkpoint/wave1-unified-20260101", "work/a", "work/b")
        inv1, inv2 = self.inventory("inv1.json"), self.inventory("inv2.json")
        j1, j2 = read_json(inv1), read_json(inv2)
        for j in (j1, j2):
            j.pop("generated_at")
        self.assertEqual(j1, j2)                                    # inventory is stable except its timestamp

        def run(args, out):
            sh(sys.executable, "-m", "drive_mirror", *args, "--report", out, env=self.env)
            with open(out, "rb") as fh:
                return fh.read()

        lin_args = ["lineage", "--repo-url", self.repo.remote, "--work-dir", self.work_dir, "--inventory", inv1]
        l1, l2 = run(lin_args, os.path.join(self.tmp, "l1.json")), run(lin_args, os.path.join(self.tmp, "l2.json"))
        self.assertEqual(l1, l2)
        facts = os.path.join(self.tmp, "facts.json")
        write_json(facts, {"unresolved_uncertainties": [{"id": "U-1", "statement": "x"}],
                           "owner_decisions_required": [{"id": "D-1"}]})
        cs_args = ["current-state", "--inventory", inv1, "--lineage", os.path.join(self.tmp, "l1.json"),
                   "--facts", facts]
        c1, c2 = run(cs_args, os.path.join(self.tmp, "c1.json")), run(cs_args, os.path.join(self.tmp, "c2.json"))
        self.assertEqual(c1, c2)                                    # byte-identical from identical inputs
        cs = json.loads(c1)
        self.assertEqual(cs["generated_at"], read_json(inv1)["generated_at"])
        self.assertEqual(cs["source_refs"]["default_branch"]["sha"], head(r, "main"))
        self.assertEqual(cs["source_refs"]["latest_unified_checkpoint"]["name"], "checkpoint/wave1-unified-20260101")
        self.assertIn("NOT ESTABLISHED", cs["computed"]["integration"]["integrated_project_state"])
        stack = cs["computed"]["stacks_on_latest_checkpoint"]
        self.assertEqual([s["root"] for s in stack], ["work/a"])
        self.assertEqual(stack[0]["tips"], ["work/b"])
        self.assertTrue(any(u["id"] == "U-1" for u in cs["unresolved_uncertainties"]))
        self.assertTrue(cs["scope"]["does_not_cover"])
        self.assertEqual(sorted(cs["computed"]["branches"], key=lambda b: b["name"]), cs["computed"]["branches"])


class SecurityGate(MirrorTestCase):
    def leaky_repo(self):
        r = self.repo
        r.write("README.md", "# t\n")
        r.write("notes.txt", "harmless line\n")
        r.commit("base")
        r.git("checkout", "-q", "-b", "clean")                       # branches off BEFORE the leak
        r.git("checkout", "-q", "main")
        r.write("notes.txt", f"harmless line\nkey = {SECRET_MARKER}\n")
        leak = r.commit("add flagged line")
        r.git("checkout", "-q", "-b", "leaky")
        r.git("checkout", "-q", "-b", "fixed")                       # contains the leak commit, then removes the line
        r.write("notes.txt", "harmless line\n")
        r.commit("remove flagged line")
        r.git("checkout", "-q", "main")
        r.push("main", "clean", "leaky", "fixed")
        self.leak = leak
        return leak

    def report(self, leak, secret=SECRET_MARKER):
        fp = f"{leak}:notes.txt:generic-api-key:2"
        path = os.path.join(self.tmp, "gitleaks.json")
        write_json(path, [{"RuleID": "generic-api-key", "File": "notes.txt", "StartLine": 2, "EndLine": 2,
                           "Commit": leak, "Fingerprint": fp, "Entropy": 4.1, "Secret": secret,
                           "Match": f"key = {secret}"}])
        return path, fp

    def approved(self, fp, status=None):
        path = os.path.join(self.tmp, "approved.json")
        findings = [] if status is None else [{"fingerprint": fp, "status": status, "rule": "generic-api-key",
                                               "file": "notes.txt", "line": 2, "commit": fp.split(":")[0]}]
        write_json(path, {"schema": "drive-mirror-approved-findings/1", "findings": findings})
        return path

    def gate(self, report, approved, expect, tags=True):
        out = os.path.join(self.tmp, f"q-{expect}.json")
        snap = os.path.join(self.tmp, f"snap-{expect}.json")
        sh(sys.executable, "-m", "drive_mirror", "prepare", "--repo-url", self.repo.remote,
           "--work-dir", self.work_dir, "--snapshot-out", snap, env=self.env)
        args = [sys.executable, "-m", "drive_mirror", "secgate", "--repo-url", self.repo.remote,
                "--work-dir", self.work_dir, "--source-snapshot", snap, "--scan-report", report,
                "--approved", approved, "--quarantine-out", out]
        p = subprocess.run(args, env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        text = p.stdout.decode() + p.stderr.decode()
        self.assertNotIn(SECRET_MARKER, text)                          # values never reach logs
        if hasattr(self, "fake_token"):
            self.assertNotIn(self.fake_token, text)
        self.assertEqual(p.returncode, expect, text[-1500:])
        doc = read_json(out) if os.path.exists(out) else None
        if doc is not None:
            self.assertNotIn(SECRET_MARKER, json.dumps(doc))
        return out, doc

    def test_gate_blocks_unreviewed_and_proposed_and_quarantines_only_affected_tips(self):
        leak = self.leaky_repo()
        rep, fp = self.report(leak)
        for status in (None, "proposed"):
            out, doc = self.gate(rep, self.approved(fp, status), expect=6)
            self.assertEqual(doc["verdict"], "BLOCKED")
            # `leaky` and `main` tips still contain the flagged line; `fixed` removed it; `clean` predates it.
            self.assertEqual(sorted(doc["refs"]), ["refs/heads/leaky", "refs/heads/main"])
            self.assertEqual(doc["counts"]["unreviewed" if status is None else "proposed_awaiting_owner"], 1)

    def test_gate_passes_only_for_approved_status(self):
        leak = self.leaky_repo()
        rep, fp = self.report(leak)
        out, doc = self.gate(rep, self.approved(fp, "approved"), expect=0)
        self.assertEqual(doc["verdict"], "PASS")
        self.assertEqual(doc["refs"], {})

    def test_gate_fails_closed_on_unreadable_or_malformed_input(self):
        leak = self.leaky_repo()
        _, fp = self.report(leak)
        bad = os.path.join(self.tmp, "bad.json")
        with open(bad, "w") as fh:
            fh.write("{not json")
        self.gate(bad, self.approved(fp, "approved"), expect=7)
        self.gate(os.path.join(self.tmp, "missing.json"), self.approved(fp, "approved"), expect=7)
        malformed = os.path.join(self.tmp, "mal.json")
        write_json(malformed, [{"RuleID": "x"}])
        self.gate(malformed, self.approved(fp, "approved"), expect=7)

    def test_quarantined_refs_are_not_uploaded_and_never_silent(self):
        leak = self.leaky_repo()
        rep, fp = self.report(leak)
        qpath, doc = self.gate(rep, self.approved(fp), expect=6)
        srep = self.sync("--quarantine-file", qpath, "--allow-external-scan-report")
        by = {n["ref"]: n for n in srep["namespaces"]}
        self.assertEqual(by["refs/heads/leaky"]["action"], "quarantined")
        self.assertEqual(by["refs/heads/main"]["action"], "quarantined")
        self.assertEqual(by["refs/heads/clean"]["status"], "complete")
        self.assertEqual(by["refs/heads/fixed"]["status"], "complete")
        self.assertIsNone(self.ns_folder("leaky"))
        self.assertIsNone(self.ns_folder("main"))
        # the flagged bytes exist nowhere on Drive
        st = self.fake.store
        self.assertFalse(any(SECRET_MARKER.encode() in b for b in st.blobs.values()))
        # verification with the quarantine file passes and *names* what was excluded ...
        v = self.verify_pass("--quarantine-file", qpath)
        self.assertEqual(v["global"]["quarantined_refs"], ["refs/heads/leaky", "refs/heads/main"])
        # ... and without it the same mirror is reported as incomplete
        code, v2 = self.cli("verify", expect=1)
        self.assertEqual(v2["global"]["problems"]["namespace_missing"], 2)

    def test_content_uploaded_before_the_gate_fired_is_reported(self):
        leak = self.leaky_repo()
        rep, fp = self.report(leak)
        self.sync()                                                   # no gate yet: everything uploaded
        qpath, _ = self.gate(rep, self.approved(fp), expect=6)
        code, v = self.cli("verify", "--quarantine-file", qpath, expect=1)
        self.assertEqual(v["global"]["problems"]["quarantined_content_on_drive"], 2)

    @unittest.skipUnless(os.environ.get("GITLEAKS_BIN"), "set GITLEAKS_BIN to a pinned gitleaks binary")
    def test_real_gitleaks_report_end_to_end(self):
        r = self.repo
        r.write("README.md", "# t\n")
        r.commit("base")
        # A randomly generated, GitHub-token-shaped string: matches the scanner's rule, is not a credential.
        import random
        import string
        rnd = random.Random(1234)
        self.fake_token = "ghp_" + "".join(rnd.choices(string.ascii_letters + string.digits, k=36))
        r.write("cfg.env", f"github_token = {self.fake_token}\n")
        r.commit("add example credentials")
        r.push("main")
        rep = os.path.join(self.tmp, "gl.json")
        sh("git", "clone", "-q", "--mirror", r.remote, os.path.join(self.tmp, "gl.git"))
        p = subprocess.run([os.environ["GITLEAKS_BIN"], "git", "--no-banner", "--no-color", "--redact=100",
                            "--report-format", "json", "--report-path", rep, "--log-opts=--all",
                            os.path.join(self.tmp, "gl.git")], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.assertEqual(p.returncode, 1, p.stderr.decode()[-500:])
        out, doc = self.gate(rep, self.approved("x:y:z:1"), expect=6)
        self.assertEqual(doc["verdict"], "BLOCKED")
        self.assertIn("refs/heads/main", doc["refs"])
        self.assertNotIn(self.fake_token, json.dumps(doc))


class ReconcileCadenceAndScopedVerify(MirrorTestCase):
    def test_incremental_run_upgrades_itself_to_reconcile_from_persisted_state(self):
        self.seed()
        rep = self.sync("--reconcile-after-hours", "24")
        self.assertEqual(rep["mode"], "reconcile")                      # never reconciled before
        self.assertIn("never", rep["mode_reason"])
        rep = self.sync("--reconcile-after-hours", "24")
        self.assertEqual(rep["mode"], "incremental")                    # reconciled moments ago
        self.assertTrue(all(n["action"] == "noop" for n in rep["namespaces"]))
        # ageing the persisted timestamp makes the next incremental run reconcile again
        st = self.fake.store
        with st.lock:
            f = next(f for f in st.files.values() if f["name"] == "state.json" and not st.effectively_trashed(f))
            doc = json.loads(st.blobs[f["_blob"]])
            doc["last_reconcile_at"] = "2020-01-01T00:00:00Z"
            f["_blob"] = st.put_bytes(json.dumps(doc).encode())
        rep = self.sync("--reconcile-after-hours", "24")
        self.assertEqual(rep["mode"], "reconcile")

    def test_scoped_verify_reads_far_less_and_full_verify_still_catches_drift(self):
        self.seed()
        r = self.repo
        self.sync()
        r.write("changed.txt", "x\n")
        r.commit("change main")
        r.push("main")
        code, srep = self.cli("sync")
        report_file = os.path.join(self.tmp, "last-sync.json")
        write_json(report_file, srep)
        st = self.fake.store
        # drift in a namespace the last sync did not touch
        feat = self.drive_file("feature/sky-tower", "feature.txt")
        with st.lock:
            feat["_blob"] = st.put_bytes(b"X" * len(st.blobs[feat["_blob"]]))   # same size: checksum path
        r0 = st.counters["reads"]
        code, scoped = self.cli("verify", "--sync-report", report_file, expect=None)
        scoped_reads = st.counters["reads"] - r0
        self.assertEqual(scoped["global"]["scope"], "changed-namespaces:1")
        self.assertEqual(scoped["result"], "PASS")                      # documented limitation of scoped runs
        r0 = st.counters["reads"]
        code, full = self.cli("verify", expect=1)                       # the authority catches it
        full_reads = st.counters["reads"] - r0
        self.assertIn("checksum_mismatch", full["namespaces"]["refs/heads/feature/sky-tower"]["problems"])
        self.assertLess(scoped_reads, full_reads)


if __name__ == "__main__":
    unittest.main()

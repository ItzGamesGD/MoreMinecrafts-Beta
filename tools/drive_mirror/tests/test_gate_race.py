"""Gate/sync race: the security gate and the uploader must act on the same immutable, gate-examined snapshot.

Every scenario simulates the remote changing *between* the gate and the sync, deterministically (a push in the
test, no timing). "Unapproved content" is a file holding LEAK, which the scanner flags; the tests assert on the exact
commits and blobs that reach Drive, not only on file counts.

The deterministic scanner below honours the contract of `gitleaks git --log-opts=...`: it examines only the commits it
is given, so a commit that was never handed to it is exactly as invisible as it would be to the real scanner.
`test_real_gitleaks_race` runs the headline scenario with the real, pinned binary (needs GITLEAKS_BIN).
"""
from __future__ import annotations

import hashlib
import json
import os
import stat
import subprocess
import sys
import time
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from test_mirror import MirrorTestCase, sh  # noqa: E402

LEAK = "LEAKMARK-4f9c1e7a2b6d-NOT-A-CREDENTIAL"

FAKE_SCANNER = r'''#!{python}
import json, os, re, subprocess, sys
args = sys.argv[1:]
if args[:1] == ["version"]:
    print("8.30.1-deterministic-test-scanner"); sys.exit(0)
report = args[args.index("--report-path") + 1]
opts = next(a for a in args if a.startswith("--log-opts="))[len("--log-opts="):].split()
gitdir = args[-1]
if os.environ.get("SCAN_LOG"):
    with open(os.environ["SCAN_LOG"], "a") as fh:
        fh.write(json.dumps({{"log_opts": opts}}) + "\n")
roots = [o for o in opts if not o.startswith("-")]
if "--all" in opts or not roots:
    sys.exit(2)                       # this scanner only scans explicit commits
git = lambda *a: subprocess.run(["git", "--git-dir", gitdir, *a], stdout=subprocess.PIPE, check=True).stdout.decode()
findings = []
for c in git("rev-list", "--full-history", *roots).split():
    cur, ln = None, 0
    for line in git("show", "--format=", "-p", "-U0", c).split("\n"):
        if line.startswith("+++ b/"):
            cur = line[6:]
        elif line.startswith("@@"):
            ln = int(re.search(r"\+(\d+)", line).group(1)) - 1
        elif line.startswith("+") and not line.startswith("+++"):
            ln += 1
            if "LEAKMARK-" in line:
                findings.append({{"RuleID": "generic-api-key", "File": cur, "StartLine": ln, "EndLine": ln, "Commit": c,
                                  "Fingerprint": f"{{c}}:{{cur}}:generic-api-key:{{ln}}", "Entropy": 4.0,
                                  "Secret": "REDACTED", "Match": "REDACTED"}})
with open(report, "w") as fh:
    json.dump(findings, fh)
sys.exit(1 if findings else 0)
'''


def sha256_file(path):
    with open(path, "rb") as fh:
        return hashlib.sha256(fh.read()).hexdigest()


class GateRaceCase(MirrorTestCase):
    strict_gate = True          # the generic helper must never add --allow-ungated here

    def setUp(self):
        super().setUp()
        self.scanner = os.path.join(self.tmp, "scanner.py")
        with open(self.scanner, "w") as fh:
            fh.write(FAKE_SCANNER.format(python=sys.executable))
        os.chmod(self.scanner, os.stat(self.scanner).st_mode | stat.S_IXUSR)
        self.approved_list = os.path.join(self.tmp, "approved.json")
        with open(self.approved_list, "w") as fh:
            json.dump({"schema": "drive-mirror-approved-findings/1", "findings": []}, fh)
        self.scan_log = os.path.join(self.tmp, "scan.log")
        self.env["SCAN_LOG"] = self.scan_log
        self.n = 0

    # ------------------------------------------------------------------ the two stages
    def freeze(self, expect=0, work_dir=None):
        """Stage 1: fetch once, write the frozen snapshot."""
        self.n += 1
        out = os.path.join(self.tmp, f"snapshot-{self.n}.json")
        p = subprocess.run([sys.executable, "-m", "drive_mirror", "prepare", "--repo-url", self.repo.remote,
                            "--work-dir", work_dir or self.work_dir, "--snapshot-out", out],
                           env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.assertEqual(p.returncode, expect, p.stderr.decode()[-800:])
        return out

    def gate(self, snapshot, expect=None, scanner=None, extra=()):
        """Stage 2: scan exactly the snapshot's commits and write the approval. Exit 0 (pass) or 6 (quarantine) are both OK."""
        self.n += 1
        out = os.path.join(self.tmp, f"approval-{self.n}.json")
        args = [sys.executable, "-m", "drive_mirror", "secgate", "--repo-url", self.repo.remote,
                "--work-dir", self.work_dir, "--source-snapshot", snapshot, "--approved", self.approved_list,
                "--quarantine-out", out, *extra]
        if scanner != "none":
            args += ["--run-scanner", scanner or self.scanner]
        p = subprocess.run(args, env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        text = p.stdout.decode() + p.stderr.decode()
        self.assertNotIn(LEAK, text)
        if expect is None:
            self.assertIn(p.returncode, (0, 6), text[-800:])
        else:
            self.assertEqual(p.returncode, expect, text[-800:])
        return out

    def approve(self):
        """freeze + gate in one go (nothing can change in between inside one test step)."""
        return self.gate(self.freeze())

    def gsync(self, approval, *extra, expect=0, sha=True):
        args = ["--quarantine-file", approval, *extra]
        if sha:
            args += ["--approval-sha256", sha256_file(approval)]
        return self.cli("sync", *args, expect=expect)

    # ------------------------------------------------------------------ remote changes
    def base(self):
        r = self.repo
        r.write("README.md", "# t\n")
        r.write("src/a.txt", "a\n")
        c1 = r.commit("base")
        r.git("checkout", "-q", "-b", "feature")
        r.write("feature.txt", "f\n")
        cf = r.commit("feature")
        r.git("checkout", "-q", "main")
        r.git("tag", "v1")
        r.push("main", "feature", "v1")
        return c1, cf

    def leak_commit(self, branch="main", name="secret.env", push=True, force=False):
        r = self.repo
        r.git("checkout", "-q", branch)
        r.write(name, f"api_key = {LEAK}\n")
        c = r.commit(f"add {name}")
        if push:
            r.push(branch, force=force)
        r.git("checkout", "-q", "main")
        return c

    # ------------------------------------------------------------------ what reached Drive
    def drive_manifests(self):
        st = self.fake.store
        out = {}
        with st.lock:
            for f in st.files.values():
                if f["appProperties"].get("mm_role") == "metafile" and f["name"].endswith(".json") \
                        and not f["name"].startswith("retired__") and not f["trashed"]:
                    try:
                        doc = json.loads(st.blobs[f["_blob"]])
                    except Exception:
                        continue
                    if isinstance(doc, dict) and "ref" in doc and isinstance(doc.get("commit"), dict):
                        out[doc["ref"]] = doc["commit"]["sha"]
        return out

    def drive_content(self):
        st = self.fake.store
        with st.lock:
            return [st.blobs[f["_blob"]] for f in st.files.values() if f.get("_blob") is not None]

    def assert_no_leak_on_drive(self):
        self.assertFalse(any(LEAK.encode() in b for b in self.drive_content()), "unapproved content reached Drive")
        st = self.fake.store
        with st.lock:
            oids = {f["appProperties"].get("mm_blob") for f in st.files.values()}
        p = subprocess.run(["git", "hash-object", "--stdin"], input=f"api_key = {LEAK}\n".encode(),
                           stdout=subprocess.PIPE)
        self.assertNotIn(p.stdout.decode().strip(), oids)

    @staticmethod
    def read(path):
        with open(path) as fh:
            return fh.read()

    @staticmethod
    def write_json(path, obj):
        with open(path, "w") as fh:
            json.dump(obj, fh)

    @staticmethod
    def load(path):
        with open(path) as fh:
            return json.load(fh)

    def tip(self, ref):
        return self.repo.git("rev-parse", ref).stdout.decode().strip()


class RaceScenarios(GateRaceCase):
    # 10 --------------------------------------------------------------------------------------------------
    def test_valid_unchanged_snapshot_synchronizes_normally(self):
        c1, cf = self.base()
        approval = self.approve()
        rep = self.gsync(approval)[1]
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(rep["gate"]["mode"], "approved-snapshot")
        self.assertEqual(rep["gate"]["scan_binding"], "frozen-commits")
        self.assertEqual(self.drive_manifests(), {"refs/heads/main": c1, "refs/heads/feature": cf, "refs/tags/v1": c1})
        # verify against the approved snapshot: PASS, and it never fetches
        code, v = self.cli("verify", "--quarantine-file", approval, "--approval-sha256", sha256_file(approval),
                           "--deep-sample", "50", expect=None)
        self.assertEqual((code, v["result"]), (0, "PASS"), v["global"])
        # idempotent repeat on the same approval, and a reconcile on it
        rep2 = self.gsync(approval)[1]
        self.assertEqual({n["action"] for n in rep2["namespaces"]}, {"noop"})
        rep3 = self.gsync(approval, "--mode", "reconcile")[1]
        self.assertEqual(rep3["status"], "complete")
        self.assertEqual(sum(n.get(k, 0) or 0 for n in rep3["namespaces"] for k in ("uploads", "copies", "updates",
                                                                                    "moves", "trashed")), 0)
        self.assert_no_leak_on_drive()

    def test_gate_scans_exactly_the_snapshots_commits(self):
        c1, cf = self.base()
        snap = self.freeze()
        self.gate(snap)
        with open(self.scan_log) as fh:
            opts = json.loads(fh.readline())["log_opts"]
        self.assertNotIn("--all", opts)
        self.assertEqual(sorted(o for o in opts if not o.startswith("-")), sorted({c1, cf}))

    # 1 ---------------------------------------------------------------------------------------------------
    def test_branch_advancing_after_the_gate_is_not_uploaded(self):
        c1, cf = self.base()
        approval = self.approve()
        late = self.leak_commit("main")                        # the race: main advances after the gate
        rep = self.gsync(approval)[1]
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(self.drive_manifests()["refs/heads/main"], c1)          # approved commit, not the new tip
        self.assertNotEqual(late, c1)
        self.assert_no_leak_on_drive()
        # the NEXT run gates the new state first; the flagged tip is then quarantined, still not uploaded
        approval2 = self.approve()
        rep2 = self.gsync(approval2)[1]
        by = {n["ref"]: n for n in rep2["namespaces"]}
        self.assertEqual(by["refs/heads/main"]["action"], "quarantined")
        self.assertEqual(self.drive_manifests()["refs/heads/main"], c1)
        self.assert_no_leak_on_drive()

    def test_negative_control_the_ungated_flow_uploads_post_gate_content(self):
        """The previous behaviour, kept only behind --allow-ungated: it re-resolves the moving ref and uploads the
        commit the gate never saw. Demonstrates the invariant violation the handoff now prevents."""
        c1, cf = self.base()
        self.approve()                                          # a gate ran ...
        late = self.leak_commit("main")                         # ... then main advanced
        rep = self.cli("sync", "--allow-ungated")[1]            # legacy: ignores the gate, fetches the new tip
        self.assertEqual(rep["gate"]["mode"], "UNGATED")
        self.assertEqual(self.drive_manifests()["refs/heads/main"], late)
        self.assertTrue(any(LEAK.encode() in b for b in self.drive_content()))

    # 2 ---------------------------------------------------------------------------------------------------
    def test_a_branch_created_after_the_gate_is_not_uploaded(self):
        c1, cf = self.base()
        approval = self.approve()
        self.repo.git("checkout", "-q", "-b", "late", "main")
        self.repo.write("late.txt", f"{LEAK}\n")
        self.repo.commit("late branch")
        self.repo.push("late")
        self.repo.git("checkout", "-q", "main")
        rep = self.gsync(approval)[1]
        self.assertNotIn("refs/heads/late", {n["ref"] for n in rep["namespaces"]})
        self.assertNotIn("refs/heads/late", self.drive_manifests())
        self.assertIsNone(self.ns_folder("late"))
        self.assert_no_leak_on_drive()

    # 3 ---------------------------------------------------------------------------------------------------
    def test_force_push_after_the_gate_keeps_the_approved_commit(self):
        c1, cf = self.base()
        approval = self.approve()
        self.repo.git("checkout", "-q", "main")
        self.repo.git("reset", "-q", "--hard", "HEAD~0")
        self.repo.write("rewritten.env", f"k={LEAK}\n")
        forced = self.repo.commit("rewrite history")
        self.repo.git("push", "-q", "--force", "origin", f"{forced}:refs/heads/feature")   # feature force-pushed
        self.gsync(approval)
        m = self.drive_manifests()
        self.assertEqual(m["refs/heads/feature"], cf)
        self.assertNotEqual(m["refs/heads/feature"], forced)
        self.assert_no_leak_on_drive()

    # 4 ---------------------------------------------------------------------------------------------------
    def test_a_branch_deleted_after_the_gate_is_mirrored_from_the_approved_snapshot_then_retired(self):
        c1, cf = self.base()
        approval = self.approve()
        self.repo.git("push", "-q", "origin", "--delete", "feature")
        rep = self.gsync(approval)[1]
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(self.drive_manifests()["refs/heads/feature"], cf)       # approved state, objects are local
        self.assertEqual(rep["retired"], [])
        # a fresh gate observes the deletion; the (twice confirmed) deletion retires the namespace
        rep2 = self.gsync(self.approve())[1]
        self.assertEqual([e["ref"] for e in rep2["retired"]], ["refs/heads/feature"])
        self.assertNotIn("refs/heads/feature", self.drive_manifests())

    def test_a_ref_that_is_inaccessible_rather_than_deleted_is_not_retired(self):
        c1, cf = self.base()
        self.gsync(self.approve())
        self.repo.git("push", "-q", "origin", "--delete", "feature")
        approval2 = self.approve()                                # the new snapshot no longer lists `feature`
        # the remote now cannot answer the confirmation queries (deleted? or merely unreachable?)
        sh("git", "--git-dir", self.work_dir, "remote", "set-url", "origin", os.path.join(self.tmp, "unreachable.git"))
        before = self.drive_snapshot(include_meta=True)
        code, _ = self.gsync(approval2, expect=None)
        self.assertNotEqual(code, 0)                              # cannot confirm: the run does not pretend it did
        self.assertEqual(self.drive_manifests()["refs/heads/feature"], cf)
        self.assertIsNotNone(self.ns_folder("feature"))           # nothing retired, nothing moved
        self.assertEqual(self.drive_snapshot(), {k: v for k, v in before.items() if k in self.drive_snapshot()})

    # 5 ---------------------------------------------------------------------------------------------------
    def test_a_pr_only_ref_advancing_after_the_gate_is_not_uploaded(self):
        c1, cf = self.base()
        r = self.repo
        r.git("checkout", "-q", "-b", "tmp-pr", "main")
        r.write("pr.txt", "pr work\n")
        p1 = r.commit("pr work")
        r.git("push", "-q", "origin", f"{p1}:refs/pull/7/head")
        r.git("checkout", "-q", "main")
        r.git("branch", "-q", "-D", "tmp-pr")
        approval = self.approve()
        r.git("checkout", "-q", "-b", "tmp-pr2", p1)
        r.write("pr-leak.txt", f"{LEAK}\n")
        p2 = r.commit("pr advances")
        r.git("push", "-q", "--force", "origin", f"{p2}:refs/pull/7/head")
        r.git("checkout", "-q", "main")
        self.gsync(approval)
        self.assertEqual(self.drive_manifests()["refs/pull/7/head"], p1)
        self.assert_no_leak_on_drive()

    # 6 ---------------------------------------------------------------------------------------------------
    def test_a_quarantined_commit_cannot_enter_through_another_namespace(self):
        c1, cf = self.base()
        flagged = self.leak_commit("feature", name="leak.env")
        r = self.repo
        r.git("push", "-q", "origin", f"{flagged}:refs/tags/v-leak")         # an alias that already exists at gate time
        approval = self.approve()
        doc = self.load(approval)
        self.assertEqual(sorted(doc["refs"]), ["refs/heads/feature", "refs/tags/v-leak"])   # both namespaces quarantined
        # aliases created after the gate: a PR head and a branch at the flagged commit
        r.git("push", "-q", "origin", f"{flagged}:refs/pull/9/head")
        r.git("push", "-q", "origin", f"{flagged}:refs/heads/alias")
        rep = self.gsync(approval)[1]
        by = {n["ref"]: n["action"] for n in rep["namespaces"]}
        self.assertEqual(by["refs/heads/feature"], "quarantined")
        self.assertEqual(by["refs/tags/v-leak"], "quarantined")
        self.assertNotIn("refs/heads/alias", by)
        self.assertNotIn("refs/pull/9/head", by)
        self.assertEqual(set(self.drive_manifests()), {"refs/heads/main", "refs/tags/v1"})
        self.assert_no_leak_on_drive()
        # the next gate sees the aliases too and quarantines them as well
        doc2 = self.load(self.approve())
        self.assertEqual(sorted(doc2["refs"]), ["refs/heads/alias", "refs/heads/feature", "refs/pull/9/head",
                                                "refs/tags/v-leak"])

    # 7 ---------------------------------------------------------------------------------------------------
    def test_a_resumed_run_stays_on_the_approved_snapshot_after_the_source_changed(self):
        c1, cf = self.base()
        approval = self.approve()
        code, rep = self.gsync(approval, "--time-budget-min", "0.0001", expect=2)
        self.assertEqual(rep["status"], "incomplete")
        self.leak_commit("main")                                  # source changes between the attempts
        self.leak_commit("feature", name="f.env")
        rep = self.gsync(approval)[1]                             # resume with the SAME approval
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(self.drive_manifests(), {"refs/heads/main": c1, "refs/heads/feature": cf, "refs/tags/v1": c1})
        self.assert_no_leak_on_drive()

    def test_a_killed_sync_resumes_on_the_same_approval_after_the_source_changed(self):
        import signal
        c1, cf = self.base()
        r = self.repo
        r.git("checkout", "-q", "-b", "bulk")
        for i in range(400):
            r.write(f"bulk/{i // 50}/f{i}.txt", f"content {i} {os.urandom(8).hex()}\n")
        cb = r.commit("bulk")
        r.push("bulk")
        r.git("checkout", "-q", "main")
        approval = self.approve()
        args = [sys.executable, "-m", "drive_mirror", "sync", "--repo-url", r.remote, "--work-dir", self.work_dir,
                "--mirror-name", "mirror-under-test", "--parent-folder-id", self.parent, "--lock-settle-s", "0.05",
                "--workers", "2", "--quarantine-file", approval, "--approval-sha256", sha256_file(approval)]
        start = self.fake.store.counters["writes"]
        proc = subprocess.Popen(args, env=self.env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        while self.fake.store.counters["writes"] - start < 150 and proc.poll() is None:
            time.sleep(0.01)
        proc.send_signal(signal.SIGKILL)
        proc.wait()
        self.leak_commit("bulk", name="bulk/late.env")           # the source moves while the sync is dead
        self.leak_commit("main")
        rep = self.gsync(approval, "--break-lock")[1]             # resume: same approval, no new fetch
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(self.drive_manifests(), {"refs/heads/main": c1, "refs/heads/feature": cf,
                                                  "refs/heads/bulk": cb, "refs/tags/v1": c1})
        self.assert_no_leak_on_drive()
        code, v = self.cli("verify", "--quarantine-file", approval, "--approval-sha256", sha256_file(approval),
                           expect=None)
        self.assertEqual((code, v["result"]), (0, "PASS"), v["global"])

    def test_resume_after_the_cache_moved_fails_closed(self):
        c1, cf = self.base()
        approval = self.approve()
        self.gsync(approval, "--time-budget-min", "0.0001", expect=2)
        self.leak_commit("main")
        self.freeze()                                             # something fetched: the cache now holds newer refs
        before = self.drive_snapshot(include_meta=True)
        code, rep = self.gsync(approval, expect=7)
        self.assertEqual(rep["result"], "GATE_ERROR")
        self.assertIn("no longer matches", rep["reason"])
        self.assertEqual(before, self.drive_snapshot(include_meta=True))        # not one byte written
        self.assert_no_leak_on_drive()

    def test_an_older_approval_cannot_roll_the_mirror_back(self):
        c1, cf = self.base()
        older = self.approve()
        time.sleep(1.2)                                           # created_at has one-second resolution
        newer = self.approve()
        self.gsync(newer)
        before = self.drive_snapshot(include_meta=True)
        code, rep = self.gsync(older, expect=7)
        self.assertIn("older than the one this mirror last applied", rep["reason"])
        self.assertEqual(before, self.drive_snapshot(include_meta=True))
        self.gsync(newer)                                         # the same (newer) approval can resume freely

    # 8 ---------------------------------------------------------------------------------------------------
    def test_reconcile_after_the_source_changed_cannot_bring_in_new_content(self):
        c1, cf = self.base()
        approval = self.approve()
        self.gsync(approval)
        self.leak_commit("main")
        # Drive drift + source change, then a reconcile on the OLD approval: restores approved bytes only
        f = self.drive_file("main", "src/a.txt")
        self.fake.store.files[f["id"]]["trashed"] = True
        rep = self.gsync(approval, "--mode", "reconcile")[1]
        self.assertEqual(rep["status"], "complete")
        self.assertIsNotNone(self.drive_file("main", "src/a.txt"))               # drift repaired ...
        self.assertEqual(self.drive_manifests()["refs/heads/main"], c1)           # ... from the approved commit
        self.assertIsNone(self.drive_file("main", "secret.env"))
        self.assert_no_leak_on_drive()

    def test_quarantined_refs_stay_out_across_retry_and_reconcile(self):
        c1, cf = self.base()
        self.leak_commit("feature", name="leak.env")
        approval = self.approve()
        for extra in ((), ("--time-budget-min", "0.0001"), ("--mode", "reconcile")):     # plain, interrupted, reconcile
            code, _ = self.gsync(approval, *extra, expect=None)
            self.assertIn(code, (0, 2))
            self.assertNotIn("refs/heads/feature", self.drive_manifests())
        self.assertNotIn("refs/heads/feature", self.drive_manifests())
        self.assertIsNone(self.ns_folder("feature"))
        self.assert_no_leak_on_drive()

    # 9 ---------------------------------------------------------------------------------------------------
    def assert_fails_closed(self, args, reason_part=None, expect=7):
        before = self.drive_snapshot(include_meta=True)
        code, rep = self.cli("sync", *args, expect=expect)
        self.assertEqual(rep["result"], "GATE_ERROR")
        if reason_part:
            self.assertIn(reason_part, rep["reason"])
        self.assertEqual(before, self.drive_snapshot(include_meta=True))        # Drive untouched, root not even created
        self.assert_no_leak_on_drive()

    def test_missing_or_malformed_or_mismatched_handoff_fails_closed(self):
        self.base()
        approval = self.approve()
        good = self.load(approval)
        # (a) no approval at all, without the explicit unsafe flag
        self.assert_fails_closed([], "needs an approved source snapshot")
        # (b) missing file / not JSON / truncated
        self.assert_fails_closed(["--quarantine-file", os.path.join(self.tmp, "nope.json")], "unreadable")
        bad = os.path.join(self.tmp, "bad.json")
        for text in ("{not json", self.read(approval)[:200], "[]", "{}"):
            with open(bad, "w") as fh:
                fh.write(text)
            self.assert_fails_closed(["--quarantine-file", bad])
        # (c) a legacy (v1) quarantine file carries no snapshot
        legacy = os.path.join(self.tmp, "legacy.json")
        self.write_json(legacy, {"schema": "drive-mirror-quarantine/1", "verdict": "PASS", "refs": {}})
        self.assert_fails_closed(["--quarantine-file", legacy], "needs an approved source snapshot")

        def tampered(mutate, name):
            doc = json.loads(json.dumps(good))
            mutate(doc)
            path = os.path.join(self.tmp, name)
            self.write_json(path, doc)
            return path
        # (d) a ref pointed at another commit inside the embedded snapshot
        other = "0" * 40
        p = tampered(lambda d: d["source_snapshot"]["heads"].__setitem__("refs/heads/main", other), "t1.json")
        self.assert_fails_closed(["--quarantine-file", p], "digest mismatch")
        # (e) the quarantine list edited (a quarantined ref silently released)
        p = tampered(lambda d: d.__setitem__("refs", {"refs/heads/x": {"reason": "r", "fingerprints": ["a:b:c:1"]}}),
                     "t2.json")
        self.assert_fails_closed(["--quarantine-file", p], "digest mismatch")
        # (f) consistent re-signing with a different snapshot still fails the checksum recorded by the gate step
        self.assert_fails_closed(["--quarantine-file", approval, "--approval-sha256", "0" * 64], "checksum")
        # (g) an approval replaced by another valid approval of another snapshot
        self.leak_commit("main")
        other_approval = self.approve()
        self.assert_fails_closed(["--quarantine-file", other_approval, "--approval-sha256", sha256_file(approval)],
                                 "checksum")
        # (h) a valid approval whose objects are not in this cache (fresh work dir): fail closed, never fetch
        before = self.drive_snapshot(include_meta=True)
        p = subprocess.run([sys.executable, "-m", "drive_mirror", "sync", "--repo-url", self.repo.remote,
                            "--work-dir", os.path.join(self.tmp, "other-cache.git"), "--mirror-name", "m",
                            "--parent-folder-id", self.parent, "--quarantine-file", approval,
                            "--approval-sha256", sha256_file(approval)],
                           env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self.assertEqual(p.returncode, 7, p.stderr.decode()[-500:])
        self.assertEqual(before, self.drive_snapshot(include_meta=True))
        self.assertFalse(os.path.exists(os.path.join(self.tmp, "other-cache.git", "refs", "mirror")))

    def test_an_external_scan_report_is_not_trusted_without_an_explicit_flag(self):
        self.base()
        snap = self.freeze()
        report = os.path.join(self.tmp, "external.json")
        self.write_json(report, [])
        approval = self.gate(snap, scanner="none", extra=("--scan-report", report))
        self.assertEqual(self.load(approval)["scan"]["binding"], "external-report")
        self.assert_fails_closed(["--quarantine-file", approval], "not produced by")
        self.gsync(approval, "--allow-external-scan-report")                       # explicit, recorded opt-in

    def test_gate_refuses_a_snapshot_the_cache_no_longer_matches_or_that_is_invalid(self):
        self.base()
        snap = self.freeze()
        self.leak_commit("main")
        self.freeze()                                             # a second fetch moved refs/mirror/*
        self.gate(snap, expect=7)                                  # the scan would not be about this snapshot
        bad = os.path.join(self.tmp, "badsnap.json")
        for text in ("{x", "{}", json.dumps({"schema": "drive-mirror-source-snapshot/1"})):
            with open(bad, "w") as fh:
                fh.write(text)
            self.gate(bad, expect=7)
        self.gate(os.path.join(self.tmp, "missing.json"), expect=7)
        doc = self.load(snap)
        doc["heads"]["refs/heads/main"] = "1" * 40
        with open(bad, "w") as fh:
            json.dump(doc, fh)
        self.gate(bad, expect=7)

    def test_a_failing_scanner_fails_closed(self):
        self.base()
        snap = self.freeze()
        broken = os.path.join(self.tmp, "broken.sh")
        with open(broken, "w") as fh:
            fh.write("#!/bin/sh\nexit 3\n")
        os.chmod(broken, 0o755)
        self.gate(snap, expect=7, scanner=broken)
        self.gate(snap, expect=7, scanner=os.path.join(self.tmp, "absent-scanner"))


@unittest.skipUnless(os.environ.get("GITLEAKS_BIN"), "set GITLEAKS_BIN to a pinned gitleaks binary")
class RealGitleaksRace(GateRaceCase):
    def test_real_gitleaks_race(self):
        import random
        import string
        rnd = random.Random(99)
        token = "ghp_" + "".join(rnd.choices(string.ascii_letters + string.digits, k=36))   # shaped like a token, is not one
        c1, cf = self.base()
        snap = self.freeze()
        approval = self.gate(snap, scanner=os.environ["GITLEAKS_BIN"])
        doc = self.load(approval)
        self.assertEqual(doc["verdict"], "PASS")
        self.assertEqual(doc["scan"]["binding"], "frozen-commits")
        self.assertIn("8.30.1", doc["scan"]["scanner"]["version"])
        self.repo.write("cfg.env", f"github_token = {token}\n")
        self.repo.commit("add token after the gate")
        self.repo.push("main")
        rep = self.gsync(approval)[1]
        self.assertEqual(rep["status"], "complete")
        self.assertEqual(self.drive_manifests()["refs/heads/main"], c1)
        self.assertFalse(any(token.encode() in b for b in self.drive_content()))
        # a new gate on the new state does see it (the scanner is real) and quarantines main
        doc2 = self.load(self.gate(self.freeze(), scanner=os.environ["GITLEAKS_BIN"]))
        self.assertEqual(doc2["verdict"], "BLOCKED")
        self.assertIn("refs/heads/main", doc2["refs"])
        self.assertNotIn(token, json.dumps(doc2))


if __name__ == "__main__":
    unittest.main()

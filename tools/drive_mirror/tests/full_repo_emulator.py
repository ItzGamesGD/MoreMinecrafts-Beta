"""Mirror an entire real repository into the Drive emulator and verify it.

This measures the engine on real data (all branches, every tracked file) and
proves snapshot equivalence end to end. It is NOT evidence about Google's
service: quotas, rate limits and latency of real Drive are not modelled.

PYTHONPATH=tools python3 tools/drive_mirror/tests/full_repo_emulator.py \
    --repo-url https://github.com/ItzGamesGD/MoreMinecrafts-Beta --work-dir /tmp/x.git --out-dir /tmp/out
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
from fake_drive import FakeDrive  # noqa: E402


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo-url", required=True)
    ap.add_argument("--work-dir", required=True)
    ap.add_argument("--out-dir", required=True)
    ap.add_argument("--workers", default="8")
    ap.add_argument("--deep-sample", default="500")
    a = ap.parse_args()
    os.makedirs(a.out_dir, exist_ok=True)
    summary = {"repo_url": a.repo_url, "steps": {}}
    with FakeDrive() as fake:
        parent = fake.new_folder("Mirrors")
        env = dict(os.environ, PYTHONPATH=TOOLS, DRIVE_AUTH_MODE="static", DRIVE_ACCESS_TOKEN="test-token",
                   DRIVE_API_BASE=fake.base)

        def cli(step, cmd, *extra):
            report = os.path.join(a.out_dir, f"{step}.json")
            args = [sys.executable, "-m", "drive_mirror", cmd, "--repo-url", a.repo_url, "--work-dir", a.work_dir,
                    "--mirror-name", "MoreMinecrafts-Beta", "--parent-folder-id", parent, "--report", report, *extra]
            w0, t0 = fake.store.counters["writes"], time.time()
            r0 = fake.store.counters["reads"]
            p = subprocess.run(args, env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            with open(os.path.join(a.out_dir, f"{step}.stderr.log"), "wb") as fh:
                fh.write(p.stderr)
            rep = json.load(open(report))
            summary["steps"][step] = {"exit": p.returncode, "seconds": round(time.time() - t0, 1),
                                      "http_writes": fake.store.counters["writes"] - w0,
                                      "http_reads": fake.store.counters["reads"] - r0}
            print(step, json.dumps(summary["steps"][step]), flush=True)
            return p.returncode, rep

        def ns_snapshot():
            st = fake.store
            with st.lock:
                return {f["id"]: (f["name"], tuple(f["parents"]), f.get("_blob"), f["trashed"],
                                  tuple(sorted(f["appProperties"].items())))
                        for f in st.files.values() if f["appProperties"].get("mm_role") in ("file", "dir", "ns")}

        code, rep = cli("1-initial-sync", "sync", "--workers", a.workers, "--lock-settle-s", "0.1")
        s = summary["steps"]["1-initial-sync"]
        s["status"] = rep["status"]
        s["namespaces"] = len(rep["namespaces"])
        s["uploads"] = sum(n.get("uploads", 0) for n in rep["namespaces"])
        s["copies"] = sum(n.get("copies", 0) for n in rep["namespaces"])
        s["folders_created"] = sum(n.get("folders_created", 0) for n in rep["namespaces"])
        s["bytes_uploaded"] = rep["drive_stats"].get("bytes_uploaded")

        code, ver = cli("2-verify", "verify", "--deep-sample", a.deep_sample)
        g = ver["global"]
        summary["steps"]["2-verify"].update(result=ver["result"], expected_namespaces=g["expected_namespaces"],
                                            found_namespaces=g["found_namespaces"],
                                            files_expected=g["files_expected"], files_verified=g["files_verified"],
                                            bytes_expected=g["bytes_expected"], deep=g.get("deep_downloads"),
                                            problems=g["problems"],
                                            failing_namespaces=ver["failing_namespaces"])

        before = ns_snapshot()
        code, rep = cli("3-idempotent-sync", "sync", "--workers", a.workers, "--lock-settle-s", "0.1")
        summary["steps"]["3-idempotent-sync"].update(
            status=rep["status"], noop_namespaces=sum(1 for n in rep["namespaces"] if n["action"] == "noop"),
            namespace_items_changed=sum(1 for k, v in ns_snapshot().items() if before.get(k) != v)
            + len(set(before) - set(ns_snapshot())))

        before = ns_snapshot()
        code, rep = cli("4-reconcile-sync", "sync", "--mode", "reconcile", "--workers", a.workers,
                        "--lock-settle-s", "0.1")
        after = ns_snapshot()
        summary["steps"]["4-reconcile-sync"].update(
            status=rep["status"], namespace_items_changed=sum(1 for k, v in after.items() if before.get(k) != v)
            + len(set(before) - set(after)))

        code, ver = cli("5-verify-after-reconcile", "verify")
        summary["steps"]["5-verify-after-reconcile"].update(result=ver["result"],
                                                            files_verified=ver["global"]["files_verified"])
        summary["emulator_items"] = len(fake.store.files)
        summary["emulator_unique_blobs"] = len(fake.store.blobs)
    with open(os.path.join(a.out_dir, "SUMMARY.json"), "w") as fh:
        json.dump(summary, fh, indent=1)
    print(json.dumps(summary, indent=1))
    ok = (summary["steps"]["2-verify"]["result"] == "PASS"
          and summary["steps"]["5-verify-after-reconcile"]["result"] == "PASS"
          and summary["steps"]["3-idempotent-sync"]["namespace_items_changed"] == 0
          and summary["steps"]["4-reconcile-sync"]["namespace_items_changed"] == 0)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())

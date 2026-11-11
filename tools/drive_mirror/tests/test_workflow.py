"""Deployment-readiness checks of `.github/workflows/drive-mirror.yml`, and of the CLI path its dry run relies on.

These are static checks of the workflow text (stdlib only, no YAML dependency) plus one end-to-end CLI check on the
Drive emulator. They do not prove anything about GitHub Actions execution or real Google Drive.
"""
from __future__ import annotations

import json
import os
import re
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import test_mirror as T  # noqa: E402  (module import, so its test classes are not collected twice)

REPO_ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
WORKFLOW = os.path.join(REPO_ROOT, ".github", "workflows", "drive-mirror.yml")


def _read() -> str:
    with open(WORKFLOW, encoding="utf-8") as fh:
        return fh.read()


def _jobs(text: str) -> dict[str, str]:
    """Split the text under the top-level `jobs:` key into {job id: block} (jobs are indented two spaces)."""
    body = text.split("\njobs:\n", 1)[1]
    parts = re.split(r"(?m)^  ([A-Za-z0-9_-]+):\s*$", body)
    return {parts[i]: parts[i + 1] for i in range(1, len(parts) - 1, 2)}


def _step(block: str, name: str) -> str:
    """The text of the step whose `- name:` is `name` (up to the next step at the same indentation)."""
    m = re.search(r"(?m)^      - name: " + re.escape(name) + r"\s*$(.*?)(?=^      (?:- |#)|\Z)", block, re.S)
    assert m, f"step {name!r} not found"
    return m.group(1)


class WorkflowStatic(unittest.TestCase):
    def test_every_action_is_pinned_to_a_full_commit_sha(self):
        uses = re.findall(r"(?m)^\s*(?:-\s*)?uses:\s*(\S+)", _read())
        self.assertTrue(uses)
        for ref in uses:
            self.assertRegex(ref, r"@[0-9a-f]{40}$", f"{ref} is not pinned to a commit SHA")

    def test_nothing_runs_unless_drive_mirror_enabled_is_true(self):
        jobs = _jobs(_read())
        for name in ("relay", "mirror"):
            cond = re.search(r"(?m)^    if:\s*>-?\s*\n((?:      .*\n)+)", jobs[name])
            self.assertIsNotNone(cond, f"{name} has no multi-line `if`")
            self.assertIn("vars.DRIVE_MIRROR_ENABLED == 'true'", cond.group(1), name)
        # the only job allowed to run while disabled just reports BLOCKED
        self.assertIn("vars.DRIVE_MIRROR_ENABLED != 'true'", jobs["not-configured"])
        self.assertNotIn("secrets.", jobs["not-configured"])

    def test_workflow_does_not_enable_or_configure_itself(self):
        text = _read()
        self.assertNotRegex(text, r"gh\s+(variable|secret)\s+set")
        self.assertNotRegex(text, r"(?m)^\s*DRIVE_MIRROR_ENABLED\s*:")

    def test_credentials_environment_and_oidc_exist_only_in_the_mirror_job(self):
        jobs = _jobs(_read())
        self.assertIn("environment: drive-mirror", jobs["mirror"])
        self.assertIn("id-token: write", jobs["mirror"])
        for name in ("relay", "not-configured"):
            self.assertNotIn("secrets.", jobs[name], name)
            self.assertNotIn("id-token", jobs[name], name)
            self.assertNotIn("environment:", jobs[name], name)
        self.assertIn("actions: write", jobs["relay"])
        self.assertNotIn("contents:", jobs["relay"].replace("contents: read", ""))   # relay gets no repository access

    def test_default_permissions_are_read_only_and_no_unsafe_triggers(self):
        text = _read()
        self.assertRegex(text, r"(?m)^permissions:\n  contents: read\n")
        triggers = text.split("\non:\n", 1)[1].split("\npermissions:", 1)[0]
        keys = set(re.findall(r"(?m)^  ([a-z_]+):", triggers))
        self.assertEqual(keys, {"push", "create", "delete", "pull_request_target", "schedule", "workflow_dispatch"})
        self.assertNotIn("workflow_run", keys)
        self.assertNotIn("pull_request", keys)

    def test_mirror_job_never_checks_out_pull_request_code(self):
        mirror = _jobs(_read())["mirror"]
        for m in re.finditer(r"actions/checkout@[0-9a-f]{40}[^\n]*\n((?:          .*\n)+)", mirror):
            self.assertNotIn("pull_request", m.group(1))
            self.assertNotIn("head.sha", m.group(1))
            self.assertIn("persist-credentials: false", m.group(1))

    def test_security_gate_runs_before_the_first_sync_and_fails_closed(self):
        mirror = _jobs(_read())["mirror"]
        gate, sync = mirror.index("name: Secret scan and gate"), mirror.index("name: Sync")
        self.assertLess(mirror.index("name: Preflight"), gate)
        self.assertLess(gate, sync)
        self.assertIn("refusing to sync (fail closed)", _step(mirror, "Secret scan and gate"))
        self.assertIn("sha256sum -c -", _step(mirror, "Install pinned, checksum-verified gitleaks"))
        self.assertIn("--quarantine-file", _step(mirror, "Sync"))
        self.assertIn("--quarantine-file", _step(mirror, "Independent verification"))

    def test_verify_is_not_run_after_a_dry_run(self):
        """A dry run writes nothing, so a fresh test namespace does not exist and `verify` would fail it (mirror_root)."""
        step = _step(_jobs(_read())["mirror"], "Independent verification")
        cond = re.search(r"(?m)^        if:\s*(.*)$", step).group(1)
        self.assertIn("!= 'dry-run'", cond)
        self.assertIn("== 'verify-only'", cond)      # verify-only must still verify
        self.assertIn("steps.sync.outputs.exit == '0'", cond)   # and a failed or unfinished sync is not verified

    def test_staleness_check_is_skipped_for_dry_runs(self):
        step = _step(_jobs(_read())["mirror"], "Staleness check")
        self.assertIn("!= 'dry-run'", step)


class DryRunOnFreshNamespace(T.MirrorTestCase):
    """The first documented step of the controlled live test is a dry run against a namespace that does not exist."""

    def test_dry_run_on_a_never_synced_namespace_succeeds_and_creates_no_mirror(self):
        self.seed()
        before = self.drive_snapshot(include_meta=True)
        code, rep = self.cli("sync", "--dry-run", expect=None)
        self.assertEqual(code, 0)
        self.assertEqual(rep["status"], "complete")
        self.assertTrue(rep["dry_run"])
        self.assertEqual(before, self.drive_snapshot(include_meta=True))      # nothing written to Drive
        # ... which is exactly why the workflow must not run `verify` afterwards
        code, ver = self.cli("verify", expect=None)
        self.assertEqual(code, 1)
        self.assertIn("mirror_root", ver["global"]["problems"])
        json.dumps(ver)


if __name__ == "__main__":
    unittest.main()

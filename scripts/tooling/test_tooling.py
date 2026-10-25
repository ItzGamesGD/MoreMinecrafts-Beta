import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


TOOL_DIR = Path(__file__).parent
INTEGRATION = TOOL_DIR / "integration_manifest.py"
COVERAGE = TOOL_DIR / "gametest_coverage.py"


def run(*args, cwd=None):
    return subprocess.run(args, cwd=cwd, text=True, capture_output=True)


class IntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for args in (("init", "-q"), ("config", "user.name", "Fixture"),
                     ("config", "user.email", "fixture@example.test")):
            self.assertEqual(run("git", *args, cwd=self.root).returncode, 0)

    def commit(self, name):
        self.assertEqual(run("git", "add", "-A", cwd=self.root).returncode, 0)
        self.assertEqual(run("git", "commit", "-qm", name, cwd=self.root).returncode, 0)
        return run("git", "rev-parse", "HEAD", cwd=self.root).stdout.strip()

    def report(self, base, a, b):
        output = self.root / "report.json"
        markdown = self.root / "report.md"
        result = run(sys.executable, str(INTEGRATION), base, a, b,
                     "--json-report", str(output), "--markdown-report", str(markdown), cwd=self.root)
        self.assertTrue(output.exists(), result.stderr)
        self.assertIn("# Integration manifest", markdown.read_text())
        return result, json.loads(output.read_text())

    def test_complete_large_trees_and_shared_endpoints(self):
        (self.root / "same.txt").write_text("base")
        (self.root / "conflict.txt").write_text("base")
        base = self.commit("base")
        for i in range(310):
            (self.root / f"a{i:03}.txt").write_text(str(i))
        (self.root / "same.txt").write_text("equal")
        (self.root / "conflict.txt").write_text("A")
        a = self.commit("A")
        self.assertEqual(run("git", "checkout", "-q", "-b", "other", base, cwd=self.root).returncode, 0)
        (self.root / "same.txt").write_text("equal")
        (self.root / "conflict.txt").write_text("B")
        (self.root / "only-b.txt").write_text("B")
        b = self.commit("B")
        result, report = self.report(base, a, b)
        self.assertEqual(result.returncode, 2)
        self.assertEqual(len(report["a_only"]), 310)
        self.assertEqual(report["b_only"], ["only-b.txt"])
        self.assertEqual(report["identical_shared"], ["same.txt"])
        self.assertEqual(report["differing_shared"], ["conflict.txt"])

    def test_deletion_and_rename_are_hazards(self):
        (self.root / "old.txt").write_text("rename content")
        (self.root / "gone.txt").write_text("delete content")
        base = self.commit("base")
        (self.root / "old.txt").rename(self.root / "new.txt")
        (self.root / "gone.txt").unlink()
        a = self.commit("A")
        result, report = self.report(base, a, base)
        self.assertEqual(result.returncode, 2)
        self.assertIn("gone.txt", report["structural_hazards"]["deletions"])
        self.assertEqual(report["structural_hazards"]["renames"][0]["from"], "old.txt")

    def test_missing_object_fails_explicitly(self):
        (self.root / "base.txt").write_text("base")
        base = self.commit("base")
        result, report = self.report(base, base, "0" * 40)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(report["errors"])

    def test_identical_shared_endpoint_needs_no_review(self):
        (self.root / "shared.txt").write_text("base")
        base = self.commit("base")
        (self.root / "shared.txt").write_text("same")
        a = self.commit("A")
        self.assertEqual(run("git", "checkout", "-q", "-b", "other", base, cwd=self.root).returncode, 0)
        (self.root / "shared.txt").write_text("same")
        b = self.commit("B")
        result, report = self.report(base, a, b)
        self.assertEqual(result.returncode, 0)
        self.assertEqual(report["identical_shared"], ["shared.txt"])

    def test_mode_change_needs_review(self):
        path = self.root / "script.sh"
        path.write_text("true\n")
        base = self.commit("base")
        path.chmod(0o755)
        a = self.commit("A")
        result, report = self.report(base, a, base)
        self.assertEqual(result.returncode, 2)
        self.assertEqual(report["structural_hazards"]["mode_changes"], ["script.sh"])


class GameTestTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "src/gametest/java/example"
        self.source.mkdir(parents=True)
        self.config = self.root / "src/gametest/resources/fabric.mod.json"
        self.config.parent.mkdir(parents=True)
        self.allowlist = self.root / "scripts/tooling/gametest_exclusions.json"
        self.allowlist.parent.mkdir(parents=True)
        self.allowlist.write_text('{"excluded_classes": []}')

    def java(self, name):
        (self.source / f"{name}.java").write_text(
            "package example;\nimport net.fabricmc.fabric.api.gametest.v1.GameTest;\n"
            f"public class {name} {{\n @GameTest(maxTicks = 20)\n"
            " public void runs(GameTestHelper ctx) {}\n // @GameTest public void fake() {}\n}"
        )

    def check(self):
        output = self.root / "report.json"
        markdown = self.root / "report.md"
        result = run(sys.executable, str(COVERAGE), "--root", str(self.root),
                     "--json-report", str(output), "--markdown-report", str(markdown))
        self.assertTrue(output.exists(), result.stderr)
        self.assertIn("# GameTest coverage", markdown.read_text())
        return result, json.loads(output.read_text())

    def test_unregistered_and_intentionally_excluded(self):
        for name in ("Registered", "Missing", "Excluded"):
            self.java(name)
        self.config.write_text(json.dumps({"entrypoints": {"fabric-gametest": ["example.Registered"]}}))
        self.allowlist.write_text(json.dumps({"excluded_classes": [
            {"class": "example.Excluded", "reason": "Fixture deliberately excluded"}]}))
        result, report = self.check()
        self.assertEqual(result.returncode, 2)
        self.assertEqual(report["unregistered_classes"], ["example.Missing"])
        self.assertEqual(report["excluded_classes"], ["example.Excluded"])
        self.assertEqual(report["registered_classes"], ["example.Registered"])
        self.assertEqual(len(report["discovered_methods"]), 3)

    def test_stale_allowlist_and_malformed_config(self):
        self.java("Present")
        self.allowlist.write_text(json.dumps({"excluded_classes": [
            {"class": "example.Gone", "reason": "Old fixture"}]}))
        self.config.write_text("{malformed")
        result, report = self.check()
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(report["errors"])
        self.assertEqual(report["stale_exclusions"], ["example.Gone"])

    def test_missing_entrypoint_configuration(self):
        self.java("Present")
        result, report = self.check()
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(report["errors"])

    def test_test_named_class_without_annotation_is_ignored(self):
        self.java("Actual")
        (self.source / "FakeGameTests.java").write_text("package example; public class FakeGameTests {}")
        self.config.write_text(json.dumps({"entrypoints": {"fabric-gametest": ["example.Actual"]}}))
        result, report = self.check()
        self.assertEqual(result.returncode, 0)
        self.assertEqual(list(report["discovered_classes"]), ["example.Actual"])


if __name__ == "__main__":
    unittest.main()

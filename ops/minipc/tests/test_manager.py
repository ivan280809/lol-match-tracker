import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from manager import validate_release, lock, Manager, write_json
from release_manifest import check_tests, schema_hash

SHA = "a" * 40
RELEASE = dict(version=1, repository="ivan280809/lol-match-tracker", branch="master", sha=SHA,
               image="ghcr.io/ivan280809/lol-match-tracker@sha256:" + "b" * 64,
               run_id=123, run_attempt=1, platform="linux/amd64", tests=10, schema_sha256="c" * 64)
RUN = dict(status="completed", conclusion="success", event="push", head_branch="master",
           path=".github/workflows/publish-ghcr.yml", head_repository={"full_name": "ivan280809/lol-match-tracker"},
           head_sha=SHA, id=123, run_attempt=1)


class ReleasePolicy(unittest.TestCase):
    def test_successful_current_master(self):
        validate_release(RELEASE, RUN, SHA)

    def test_rejects_failed_pending_pr_fork_and_wrong_workflow(self):
        for key, value in [("conclusion", "failure"), ("status", "queued"), ("event", "pull_request"),
                           ("head_branch", "feature"), ("path", ".github/workflows/other.yml"),
                           ("head_repository", {"full_name": "attacker/fork"})]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                validate_release(RELEASE, dict(RUN, **{key: value}), SHA)

    def test_rejects_mutable_tag_wrong_run_attempt_or_source(self):
        for key, value in [("image", "ghcr.io/ivan280809/lol-match-tracker:latest"), ("sha", "d" * 40),
                           ("run_id", 124), ("run_attempt", 2), ("tests", 0), ("schema_sha256", "")]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                validate_release(dict(RELEASE, **{key: value}), RUN, SHA)
        with self.assertRaises(ValueError):
            validate_release(RELEASE, RUN, "e" * 40)

    def test_second_operation_cannot_acquire_lock(self):
        with tempfile.TemporaryDirectory() as d:
            with lock(Path(d)):
                with self.assertRaises(RuntimeError):
                    with lock(Path(d)):
                        self.fail("Second lock unexpectedly acquired")

    def test_schema_changes_stop_before_docker(self):
        with tempfile.TemporaryDirectory() as d:
            m = object.__new__(Manager)
            m.root = Path(d)
            write_json(m.root / "current.json", dict(RELEASE, image=RELEASE["image"].replace("b" * 64, "e" * 64)))
            with patch.object(m, "run", side_effect=AssertionError("Docker must not run")):
                with self.assertRaisesRegex(RuntimeError, "Migration fingerprint changed"):
                    m.deploy(dict(RELEASE, schema_sha256="d" * 64))

    def test_pending_transaction_stops_new_deployment(self):
        with tempfile.TemporaryDirectory() as d:
            m = object.__new__(Manager)
            m.root = Path(d)
            write_json(m.root / "pending.json", {})
            with self.assertRaisesRegex(RuntimeError, "Unfinished deployment"):
                m.deploy(RELEASE)

    def test_rollback_refuses_schema_mismatch(self):
        with tempfile.TemporaryDirectory() as d:
            m = object.__new__(Manager)
            m.root = Path(d)
            write_json(m.root / "current.json", RELEASE)
            write_json(m.root / "previous.json", dict(RELEASE, schema_sha256="d" * 64))
            with self.assertRaisesRegex(RuntimeError, "Schema compatibility"):
                m.rollback()

    def deployment_fixture(self, directory):
        m = object.__new__(Manager)
        m.root = Path(directory)
        (m.root / "history").mkdir()
        m.config = {"project": "test-only"}
        old = dict(RELEASE, image=RELEASE["image"].replace("b" * 64, "e" * 64), run_id=99)
        write_json(m.root / "current.json", old)
        def run(args):
            if args[1] == "info":
                return b"linux/x86_64"
            if args[1] == "image":
                return json.dumps({"org.opencontainers.image.revision": SHA,
                                   "dev.iroberto.workflow-run": "123"}).encode()
            return b""
        m.run = run
        m.api = lambda path: {"sha": SHA}
        m.compose = lambda *args, **kwargs: b""
        m.backup = lambda image=None: "verified.dump"
        m.schema_state = lambda image=None: "unchanged"
        return m, old

    def test_failed_health_restores_previous_and_keeps_current_state(self):
        with tempfile.TemporaryDirectory() as d:
            m, old = self.deployment_fixture(d)
            with patch.object(m, "healthy", side_effect=[RuntimeError("bad health"), None]) as health:
                with self.assertRaisesRegex(RuntimeError, "previous image restored"):
                    m.deploy(RELEASE)
            self.assertEqual([RELEASE["image"], old["image"]], [c.args[0] for c in health.call_args_list])
            self.assertFalse((m.root / "pending.json").exists())
            self.assertEqual(old, json.loads((m.root / "current.json").read_text()))
            with self.assertRaisesRegex(RuntimeError, "already failed health"):
                m.deploy(RELEASE)

    def test_failed_health_after_schema_change_never_restarts_old_image(self):
        with tempfile.TemporaryDirectory() as d:
            m, old = self.deployment_fixture(d)
            m.schema_state = unittest.mock.Mock(side_effect=["before", "changed"])
            with patch.object(m, "healthy", side_effect=RuntimeError("bad health")) as health:
                with self.assertRaisesRegex(RuntimeError, "manual recovery required"):
                    m.deploy(RELEASE)
            self.assertEqual(1, health.call_count)
            self.assertTrue((m.root / "pending.json").exists())
            self.assertEqual(old, json.loads((m.root / "current.json").read_text()))

    def test_success_is_recorded_only_after_health(self):
        with tempfile.TemporaryDirectory() as d:
            m, old = self.deployment_fixture(d)
            def healthy(image):
                self.assertTrue((m.root / "pending.json").exists())
                self.assertEqual(old, json.loads((m.root / "current.json").read_text()))
            m.healthy = healthy
            m.deploy(RELEASE)
            self.assertEqual(RELEASE["image"], json.loads((m.root / "current.json").read_text())["image"])
            self.assertFalse((m.root / "pending.json").exists())

    def test_test_report_policy(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            with self.assertRaises(ValueError):
                check_tests(root)
            for key in ("skipped", "errors", "failures"):
                (root / "TEST-example.xml").write_text('<testsuite tests="4" ' + key + '="1"/>')
                with self.assertRaises(ValueError):
                    check_tests(root)
            (root / "TEST-example.xml").write_text('<testsuite tests="4" skipped="0" errors="0" failures="0"/>')
            self.assertEqual(4, check_tests(root))

    def test_migration_hash_covers_names_contents_and_normalizes_newlines(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            p = root / "V1__one.sql"
            p.write_bytes(b"SELECT 1;\r\n")
            initial = schema_hash(root)
            p.write_bytes(b"SELECT 1;\n")
            self.assertEqual(initial, schema_hash(root))
            p.write_bytes(b"SELECT 2;\n")
            self.assertNotEqual(initial, schema_hash(root))
            p.write_bytes(b"SELECT 1;\n")
            p.rename(root / "V2__one.sql")
            self.assertNotEqual(initial, schema_hash(root))


if __name__ == "__main__":
    unittest.main()

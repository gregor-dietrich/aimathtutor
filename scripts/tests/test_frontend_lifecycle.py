"""Exercise the frontend manifest checks without Maven or network access."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class FrontendLifecycleTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        source = Path(__file__).resolve().parents[1]
        shutil.copytree(source, self.root / "scripts")
        self.environment = dict(os.environ)

    def run_check(self, function):
        return subprocess.run(
            ["bash", "-c", f". scripts/lib/frontend.sh && {function}"],
            cwd=self.root,
            env=self.environment,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_install_accepts_committed_manifests(self):
        for name in ("package.json", "package-lock.json"):
            (self.root / name).write_text("committed contents\n")

        result = self.run_check("require_frontend_manifest")

        self.assertEqual(result.returncode, 0, result.stderr)

    def test_install_refuses_missing_manifests(self):
        result = self.run_check("require_frontend_manifest")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("make regen-frontend", result.stderr)

    def check_committed_manifest(self):
        return self.run_check("require_committed_frontend_manifest")

    def commit_manifests(self):
        for name in ("package.json", "package-lock.json"):
            (self.root / name).write_text("committed contents\n")
        git = ["git", "-c", "user.name=test", "-c", "user.email=test@example.com", "-c", "commit.gpgsign=false"]
        subprocess.run(["git", "init", "-q"], cwd=self.root, check=True)
        subprocess.run(["git", "add", "package.json", "package-lock.json"], cwd=self.root, check=True)
        subprocess.run([*git, "commit", "-q", "-m", "manifests"], cwd=self.root, check=True)

    def test_release_accepts_committed_manifests(self):
        self.commit_manifests()

        result = self.check_committed_manifest()

        self.assertEqual(result.returncode, 0, result.stderr)

    def test_release_refuses_changed_lockfile(self):
        self.commit_manifests()
        (self.root / "package-lock.json").write_text("regenerated contents\n")

        result = self.check_committed_manifest()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("committed lockfile", result.stderr)

    def test_release_refuses_missing_lockfile(self):
        self.commit_manifests()
        (self.root / "package-lock.json").unlink()

        result = self.check_committed_manifest()

        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()

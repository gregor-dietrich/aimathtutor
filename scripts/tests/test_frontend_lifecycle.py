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

    def run_makefile_recipe(self, makefile):
        """Run the frontend-manifest recipe line as make does: through /bin/sh."""
        lines = makefile.read_text().splitlines()
        recipe = lines[lines.index("frontend-manifest:") + 1]
        return subprocess.run(
            ["/bin/sh", "-c", recipe.removeprefix("\t").removeprefix("@")],
            cwd=self.root,
            env=self.environment,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_makefile_recipe_accepts_committed_manifests(self):
        for name in ("package.json", "package-lock.json"):
            (self.root / name).write_text("committed contents\n")

        result = self.run_makefile_recipe(Path(__file__).resolve().parents[2] / "Makefile")

        self.assertEqual(result.returncode, 0, result.stderr)

    def test_makefile_recipe_refuses_missing_manifests(self):
        result = self.run_makefile_recipe(Path(__file__).resolve().parents[2] / "Makefile")

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

    def test_release_refuses_untaggable_versions_before_anything_runs(self):
        devkit = self.root / "devkit"
        (devkit / "scripts" / "lib").mkdir(parents=True)
        (devkit / "scripts" / "lib" / "get_maven.sh").write_text("")
        bin_dir = self.root / "bin"
        bin_dir.mkdir()
        git = bin_dir / "git"
        git.write_text(f"#!/bin/sh\necho \"$@\" >> '{self.root}/git.log'\n")
        git.chmod(0o755)
        self.environment.update(PROJECT_ROOT=str(self.root), DEVKIT=str(devkit), PATH=f"{bin_dir}:{os.environ['PATH']}")

        for version in ("5.1.0-beta1", "5.1.0-SNAPSHOT", "05.1.0"):
            with self.subTest(version=version):
                result = subprocess.run(
                    ["bash", "scripts/release.sh"],
                    cwd=self.root,
                    env={**self.environment, "REVISION": version},
                    capture_output=True,
                    text=True,
                    check=False,
                )

                self.assertNotEqual(result.returncode, 0)
                self.assertIn(f"not '{version}'", result.stderr)
                self.assertIn("nothing was tagged or pushed", result.stderr)
                # Only require_valid_revision's check-ref-format may have run, no switch or pull.
                calls = (self.root / "git.log").read_text().split("\n")
                self.assertTrue(all(call.startswith("check-ref-format") for call in calls if call), calls)


if __name__ == "__main__":
    unittest.main()

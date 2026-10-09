"""Exercise frontend lifecycle scripts without Maven or network access."""

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
        helpers = self.root / "scripts/lib"
        (helpers / "get_maven.sh").write_text('MVN_CMD="./mvnw"\n')
        self.write_executable("mvnw", """#!/bin/bash
set -e
case " $* " in
    *" -Pregen-frontend "*)
        printf '{"lockfileVersion":3}\n' > package-lock.json
        printf '{}\n' > package.json
        ;;
    *" install "*)
        test -s package.json
        test -s package-lock.json
        touch installed
        ;;
esac
""")
        self.write_executable("bin/make", "#!/bin/bash\nexit 0\n")
        (self.root / "scripts/check_frontend_deps.py").write_text("")
        self.environment = dict(os.environ)
        self.environment["PATH"] = f"{self.root / 'bin'}:{os.environ['PATH']}"

    def write_executable(self, relative_path, content):
        path = self.root / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        path.chmod(0o755)

    def run_script(self, name):
        return subprocess.run(
            ["bash", f"scripts/{name}.sh"],
            cwd=self.root,
            env=self.environment,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_clean_preserves_committed_manifests(self):
        for name in ("package.json", "package-lock.json"):
            (self.root / name).write_text("committed contents\n")
        (self.root / "node_modules").mkdir()
        (self.root / "target").mkdir()

        result = self.run_script("clean")

        self.assertEqual(result.returncode, 0, result.stderr)
        for name in ("package.json", "package-lock.json"):
            self.assertTrue((self.root / name).is_file(), name)
            self.assertEqual((self.root / name).read_text(), "committed contents\n")
        self.assertFalse((self.root / "node_modules").exists())
        self.assertFalse((self.root / "target").exists())

    def test_install_bootstraps_missing_manifests_before_build(self):
        result = self.run_script("install")

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((self.root / "installed").is_file())


if __name__ == "__main__":
    unittest.main()

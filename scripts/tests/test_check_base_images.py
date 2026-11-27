"""Exercise scripts/check_base_images.py with fake docker and keytool executables."""

import json
import os
from pathlib import Path
import re
import runpy
import shutil
import subprocess
import sys
import tempfile
import unittest

REPOSITORY = Path(__file__).resolve().parents[2]
DIGEST = "a" * 64
ALPINE = "src/main/docker/Dockerfile.alpine"
UBUNTU = "src/main/docker/Dockerfile.ubuntu"
GOOD_LISTING = "\n".join(
    [
        "Keystore type: PKCS12",
        "Owner: CN=emSign Root CA - G1, O=eMudhra Technologies Limited, OU=emSign PKI, C=IN",
        "Issuer: CN=emSign Root CA - G1, O=eMudhra Technologies Limited, OU=emSign PKI, C=IN",
        "Owner: OU=No Common Name Root, O=Example, C=US",
        "Owner: CN=emSign Root CA - C1X, O=Example, C=US",
        "Owner: CN=Example Root, O=Example, C=US",
    ]
)

FAKE_DOCKER = f"""#!{sys.executable}
import json, os, re, sys
state = json.load(open(os.environ["FAKE_STATE"]))
args = sys.argv[1:]
if args[:3] == ["buildx", "imagetools", "inspect"]:
    reference = args[3]
    if args[4:] != ["--format", "{{{{json .Image}}}}"]:
        sys.stderr.write("unexpected inspect arguments: " + repr(args))
        sys.exit(3)
    entry = next(v for k, v in state["inspect"].items() if k in reference)
    sys.stderr.write(entry.get("stderr", ""))
    sys.stdout.write(entry.get("stdout", ""))
    sys.exit(entry.get("exit", 0))
if args[:2] == ["buildx", "build"]:
    dockerfile = sys.stdin.read()
    reference = re.search(r"^FROM (\\S+) AS base", dockerfile, re.M).group(1)
    platform = args[args.index("--platform") + 1]
    distro = next(k for k in state["build"] if k in reference)
    with open(os.environ["FAKE_BUILDS"], "a") as log:
        log.write(distro + "|" + platform + "\\n")
    image = json.loads(next(v for k, v in state["inspect"].items() if k in reference)["stdout"])
    if "config" in image:
        image = {{image["os"] + "/" + image["architecture"]: image}}
    env = image[platform]["config"]["Env"]
    java_home = next(e for e in env if e.startswith("JAVA_HOME=")).split("=", 1)[1]
    expected = (
        "FROM " + reference + " AS base\\n"
        "FROM scratch\\n"
        "COPY --from=base " + java_home + "/lib/security/cacerts /cacerts\\n"
    )
    if dockerfile != expected:
        sys.stderr.write("unexpected generated Dockerfile: " + repr(dockerfile))
        sys.exit(3)
    entry = state["build"][distro].get(platform, {{}})
    if entry.get("exit", 0):
        sys.stderr.write(entry.get("stderr", "build failed"))
        sys.exit(entry["exit"])
    destination = next(a for a in args if a.startswith("type=local,dest=")).split("dest=")[1]
    os.makedirs(destination)
    open(os.path.join(destination, "cacerts"), "w").write(distro + "|" + platform)
    sys.exit(0)
sys.exit(2)
"""

FAKE_KEYTOOL = f"""#!{sys.executable}
import json, os, sys
state = json.load(open(os.environ["FAKE_STATE"]))
with open(os.environ["FAKE_ARGS"], "a") as log:
    log.write(json.dumps(sys.argv[1:]) + "\\n")
distro, platform = open(sys.argv[sys.argv.index("-keystore") + 1]).read().split("|")
entry = state["keytool"][distro][platform]
sys.stderr.write(entry.get("stderr", ""))
sys.stdout.write(entry.get("stdout", ""))
sys.exit(entry.get("exit", 0))
"""


def image_config(version: str = "jdk-25.0.5.1+2", java_home: str | None = "/opt/java/openjdk") -> dict:
    env = ([f"JAVA_HOME={java_home}"] if java_home else []) + ([f"JAVA_VERSION={version}"] if version else [])
    return {"config": {"Env": env}, "os": "linux", "architecture": "amd64"}


def index(**overrides: dict | None) -> str:
    platforms = {"linux/amd64": image_config(), "linux/arm64": image_config(), "linux/s390x": image_config()}
    for key, config in overrides.items():
        platform = key.replace("_", "/")
        if config is None:
            platforms.pop(platform, None)
        else:
            platforms[platform] = config
    return json.dumps(platforms)


class CheckBaseImagesTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        base = Path(directory.name)
        self.root = base / "root"
        self.bin = base / "bin"
        self.bin.mkdir()
        shutil.copytree(REPOSITORY / "scripts", self.root / "scripts", ignore=shutil.ignore_patterns("__pycache__"))
        (self.root / "src/main/docker").mkdir(parents=True)
        self.write_dockerfile(ALPINE, f"eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:{DIGEST}")
        self.write_dockerfile(UBUNTU, f"eclipse-temurin:25.0.5.1_2-jre-resolute@sha256:{DIGEST}")
        for name, source in (("docker", FAKE_DOCKER), ("keytool", FAKE_KEYTOOL)):
            (self.bin / name).write_text(source)
            (self.bin / name).chmod(0o755)
        self.state = {
            "inspect": {"alpine": {"stdout": index()}, "resolute": {"stdout": index()}},
            "build": {"alpine": {}, "resolute": {}},
            "keytool": {
                distro: {p: {"stdout": GOOD_LISTING} for p in ("linux/amd64", "linux/arm64")}
                for distro in ("alpine", "resolute")
            },
        }
        self.args_log = base / "keytool-args.log"
        self.builds_log = base / "builds.log"
        self.path = str(self.bin)

    def write_dockerfile(self, relative: str, reference: str | None):
        lines = ["# a comment mentioning FROM nothing", "ARG X=1", f"FROM {reference}" if reference else "", "USER 185"]
        (self.root / relative).write_text("\n".join(lines) + "\n")

    def run_check(self):
        state = self.root.parent / "state.json"
        state.write_text(json.dumps(self.state))
        environment = {**os.environ, "PATH": self.path, "FAKE_STATE": str(state), "FAKE_ARGS": str(self.args_log),
                       "FAKE_BUILDS": str(self.builds_log)}
        return subprocess.run(
            [sys.executable, "-B", "scripts/check_base_images.py"],
            cwd=self.root, env=environment, capture_output=True, text=True, check=False,
        )

    def assert_fails(self, *fragments: str):
        result = self.run_check()
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertNotIn("Traceback", result.stderr)
        for fragment in fragments:
            self.assertIn(fragment, result.stderr)
        return result

    def builds(self) -> list[str]:
        return self.builds_log.read_text().splitlines() if self.builds_log.exists() else []

    def test_passes_for_matching_version_and_clean_listing(self):
        result = self.run_check()

        self.assertEqual(result.returncode, 0, result.stderr)
        for dockerfile in (ALPINE, UBUNTU):
            for platform in ("linux/amd64", "linux/arm64"):
                self.assertIn(f"{dockerfile}: {platform}: JAVA_VERSION jdk-25.0.5.1+2 matches the tag", result.stdout)
                self.assertIn(
                    f"{dockerfile}: {platform}: cacerts holds 4 certificates, none of the 4 forbidden roots", result.stdout
                )
        self.assertIn(f"{ALPINE}: linux/s390x: JAVA_VERSION jdk-25.0.5.1+2 matches the tag", result.stdout)

    def test_passes_the_listing_through_english_keytool(self):
        self.run_check()

        calls = [json.loads(line) for line in self.args_log.read_text().splitlines()]
        self.assertEqual(len(calls), 4)
        for call in calls:
            self.assertIn("-J-Duser.language=en", call)
            self.assertIn("-list", call)
            self.assertIn("-v", call)
            self.assertEqual(call[call.index("-storepass") + 1], "changeit")

    def test_fails_for_each_forbidden_root(self):
        for root in FORBIDDEN_ROOTS:
            with self.subTest(root=root):
                self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = (
                    GOOD_LISTING + f"\nOwner: CN={root}, O=eMudhra Technologies Limited, C=IN"
                )
                self.assert_fails(f"{ALPINE}: linux/amd64: cacerts holds {root}")

    def test_fails_for_a_root_that_ends_the_line(self):
        self.state["keytool"]["resolute"]["linux/arm64"]["stdout"] = GOOD_LISTING + "\nOwner: CN=AAA Certificate Services"

        self.assert_fails(f"{UBUNTU}: linux/arm64: cacerts holds AAA Certificate Services")

    def test_fails_when_the_root_is_on_arm64_only(self):
        self.state["keytool"]["alpine"]["linux/arm64"]["stdout"] = GOOD_LISTING + "\nOwner: CN=emSign Root CA - C1, C=IN"

        result = self.assert_fails(f"{ALPINE}: linux/arm64: cacerts holds emSign Root CA - C1")
        self.assertNotIn("linux/amd64: cacerts holds", result.stderr)

    def test_fails_when_the_version_does_not_match_the_tag(self):
        self.write_dockerfile(ALPINE, f"eclipse-temurin:25.0.6_7-jre-alpine@sha256:{DIGEST}")

        result = self.assert_fails(f"{ALPINE}: linux/amd64: JAVA_VERSION is jdk-25.0.5.1+2, the tag says jdk-25.0.6+7")
        self.assertNotIn(UBUNTU, result.stderr)

    def test_fails_when_one_platform_differs(self):
        self.state["inspect"]["alpine"]["stdout"] = index(linux_s390x=image_config("jdk-25.0.4+1"))

        result = self.assert_fails(f"{ALPINE}: linux/s390x: JAVA_VERSION is jdk-25.0.4+1")
        self.assertNotIn("linux/amd64: JAVA_VERSION", result.stderr)

    def test_fails_when_java_version_is_missing(self):
        self.state["inspect"]["alpine"]["stdout"] = index(linux_arm64=image_config(version=""))

        self.assert_fails(f"{ALPINE}: linux/arm64: JAVA_VERSION missing from the image config")

    def test_fails_when_java_home_is_missing(self):
        self.state["inspect"]["alpine"]["stdout"] = index(linux_arm64=image_config(java_home=None))

        self.assert_fails(f"{ALPINE}: linux/arm64: JAVA_HOME missing from the image config")

    def test_fails_when_arm64_is_absent_from_the_index(self):
        self.state["inspect"]["alpine"]["stdout"] = index(linux_arm64=None)

        self.assert_fails(f"{ALPINE}: linux/arm64: platform missing from the image index")

    def test_single_platform_manifest_lacks_a_release_platform(self):
        self.state["inspect"]["alpine"]["stdout"] = json.dumps(image_config())

        result = self.assert_fails(f"{ALPINE}: linux/arm64: platform missing from the image index")
        self.assertNotIn("JAVA_VERSION is", result.stderr)
        self.assertIn(f"{ALPINE}: linux/amd64: cacerts holds 4 certificates", result.stdout)

    def test_single_platform_manifest_still_checks_its_cacerts(self):
        self.state["inspect"]["alpine"]["stdout"] = json.dumps(image_config())
        self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = GOOD_LISTING + "\nOwner: CN=AAA Certificate Services"

        self.assert_fails(f"{ALPINE}: linux/amd64: cacerts holds AAA Certificate Services")

    def test_fails_when_keytool_exits_non_zero(self):
        self.state["keytool"]["alpine"]["linux/amd64"] = {
            "exit": 3,
            "stdout": "keytool error: java.io.IOException: keystore password was incorrect\n\tat sun.security.Stack.line\n",
        }

        result = self.assert_fails(
            f"{ALPINE}: linux/amd64: `keytool -J-Duser.language=en -list` exited 3: "
            "keytool error: java.io.IOException: keystore password was incorrect"
        )
        self.assertNotIn("sun.security.Stack", result.stderr)

    def test_reports_keytool_stderr_when_it_has_any(self):
        self.state["keytool"]["alpine"]["linux/amd64"] = {"exit": 3, "stderr": "keystore was tampered with"}

        self.assert_fails(f"{ALPINE}: linux/amd64: `keytool -J-Duser.language=en -list` exited 3: keystore was tampered with")

    def test_fails_when_keytool_prints_no_owner_line(self):
        self.state["keytool"]["alpine"]["linux/amd64"] = {"stdout": "Eigentuemer: CN=Example\n"}

        self.assert_fails(f"{ALPINE}: linux/amd64: keytool listed no `Owner:` line")

    def test_fails_when_inspect_fails(self):
        self.state["inspect"]["alpine"] = {"exit": 1, "stderr": "manifest unknown"}

        self.assert_fails(f"{ALPINE}: `docker buildx imagetools` exited 1: manifest unknown")

    def test_fails_when_the_extraction_build_fails(self):
        self.state["build"]["resolute"]["linux/arm64"] = {"exit": 1, "stderr": "no such file cacerts"}

        self.assert_fails(f"{UBUNTU}: linux/arm64: `docker buildx build` exited 1: no such file cacerts")

    def test_fails_when_docker_is_missing(self):
        (self.bin / "docker").unlink()

        self.assert_fails(f"{ALPINE}: `docker buildx imagetools` could not run", f"{UBUNTU}: `docker buildx imagetools` could not run")

    def test_fails_for_a_dockerfile_without_from(self):
        self.write_dockerfile(ALPINE, None)

        self.assert_fails(f"{ALPINE}: expected exactly one FROM, found 0")

    def test_fails_for_two_froms(self):
        reference = f"eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:{DIGEST}"
        (self.root / ALPINE).write_text(f"FROM {reference}\nFROM {reference}\n")

        self.assert_fails(f"{ALPINE}: expected exactly one FROM, found 2")

    def test_fails_for_a_reference_without_a_digest(self):
        self.write_dockerfile(ALPINE, "eclipse-temurin:25.0.5.1_2-jre-alpine")

        self.assert_fails(f"{ALPINE}: FROM eclipse-temurin:25.0.5.1_2-jre-alpine is not eclipse-temurin:")

    def test_reports_both_dockerfiles_in_one_run(self):
        self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = GOOD_LISTING + "\nOwner: CN=AAA Certificate Services"
        self.state["inspect"]["resolute"] = {"exit": 1, "stderr": "boom"}

        self.assert_fails(f"{ALPINE}: linux/amd64: cacerts holds AAA Certificate Services", f"{UBUNTU}: `docker buildx imagetools` exited 1: boom")

    def test_names_match_the_trusted_roots_rule(self):
        rule = " ".join((REPOSITORY / ".claude/rules/docker-conventions.md").read_text().split())
        sentence = re.search(r"`cacerts` lacks (.*?); its ", rule)

        self.assertIsNotNone(sentence)
        names = set(re.findall(r"`([^`]+)`", sentence.group(1)))
        self.assertTrue(names)
        self.assertEqual(names, set(FORBIDDEN_ROOTS))

    def test_platforms_match_the_release_script(self):
        literal = re.search(r'^PLATFORMS="([^"]+)"', (REPOSITORY / "scripts/lib/images.sh").read_text(), re.M)

        self.assertIsNotNone(literal)
        self.assertEqual(list(runpy_names()["RELEASE_PLATFORMS"]), literal.group(1).split(","))

    def test_dockerfiles_match_the_release_script(self):
        found = re.findall(r'^DOCKERFILE_[A-Z]+="([^"]+)"', (REPOSITORY / "scripts/lib/images.sh").read_text(), re.M)

        self.assertTrue(found)
        self.assertEqual(set(runpy_names()["DOCKERFILES"]), set(found))

    def test_fails_for_a_from_hidden_by_a_line_continuation(self):
        reference = f"eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:{DIGEST}"
        (self.root / ALPINE).write_text(f"FROM\\\n busybox:1.36\nRUN echo \\\nFROM {reference}\n")

        self.assert_fails(f"{ALPINE}: expected exactly one FROM, found 2")

    def test_fails_for_a_bare_from_at_the_end_of_the_file(self):
        (self.root / ALPINE).write_text("ARG X=1\nFROM")

        self.assert_fails(f"{ALPINE}: unsupported FROM line 'FROM'")

    def test_fails_for_a_bare_from_before_another_instruction(self):
        (self.root / ALPINE).write_text("FROM\nUSER 185\n")

        self.assert_fails(f"{ALPINE}: unsupported FROM line 'FROM'")

    def test_fails_for_a_from_with_a_flag(self):
        (self.root / ALPINE).write_text(f"FROM --platform=linux/amd64 eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:{DIGEST}\n")

        self.assert_fails(f"{ALPINE}: unsupported FROM line")

    def test_accepts_a_lower_case_from_with_a_tab_and_a_stage_name(self):
        (self.root / ALPINE).write_text(f"from\teclipse-temurin:25.0.5.1_2-jre-alpine@sha256:{DIGEST} AS runtime\n")

        result = self.run_check()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(f"{ALPINE}: linux/amd64: cacerts holds 4 certificates", result.stdout)

    def test_fails_for_a_forbidden_name_that_is_not_the_first_rdn(self):
        self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = (
            GOOD_LISTING + "\nOwner: O=Comodo CA Limited, CN=AAA Certificate Services, C=GB"
        )

        self.assert_fails(f"{ALPINE}: linux/amd64: cacerts holds AAA Certificate Services")

    def test_ignores_a_forbidden_name_in_an_issuer_line(self):
        self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = (
            GOOD_LISTING + "\nOwner: CN=Example Two, O=Example\nIssuer: CN=AAA Certificate Services, O=Comodo CA Limited"
        )

        result = self.run_check()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(f"{ALPINE}: linux/amd64: cacerts holds 5 certificates", result.stdout)

    def test_fails_for_a_java_home_that_is_not_a_plain_path(self):
        for java_home in ("/opt/java\nRUN touch /pwned", "/opt/java openjdk"):
            with self.subTest(java_home=java_home):
                self.builds_log.unlink(missing_ok=True)
                self.state["inspect"]["alpine"]["stdout"] = index(linux_arm64=image_config(java_home=java_home))

                self.assert_fails(f"{ALPINE}: linux/arm64: JAVA_HOME is not a plain absolute path: {java_home!r}")
                self.assertNotIn("alpine|linux/arm64", self.builds())
                self.assertIn("alpine|linux/amd64", self.builds())

    def test_joins_the_java_home_of_each_platform(self):
        self.state["inspect"]["alpine"]["stdout"] = index(linux_arm64=image_config(java_home="/usr/lib/jvm/jdk"))

        result = self.run_check()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(self.builds()), 4)

    def test_fails_for_inspect_output_of_the_wrong_shape(self):
        outputs = {
            "not json": ("not json", "is not JSON"),
            "list": ("[]", "is not a JSON object"),
            "string platform": (json.dumps({"linux/amd64": "x"}), "unexpected shape for linux/amd64"),
            "string config": (json.dumps({"linux/amd64": {"config": "x"}}), "unexpected shape for linux/amd64"),
            "env not a list": (json.dumps({"linux/amd64": {"config": {"Env": "x"}}}), "unexpected shape for linux/amd64"),
            "env of numbers": (json.dumps({"linux/amd64": {"config": {"Env": [1]}}}), "unexpected shape for linux/amd64"),
        }
        for name, (stdout, message) in outputs.items():
            with self.subTest(name):
                self.state["inspect"]["alpine"]["stdout"] = stdout

                self.assert_fails(f"{ALPINE}: image inspect output", message)

    def test_reports_a_version_mismatch_and_a_forbidden_root_together(self):
        self.write_dockerfile(ALPINE, f"eclipse-temurin:25.0.6_7-jre-alpine@sha256:{DIGEST}")
        self.state["keytool"]["alpine"]["linux/amd64"]["stdout"] = GOOD_LISTING + "\nOwner: CN=emSign Root CA - C1"

        self.assert_fails(
            f"{ALPINE}: linux/amd64: JAVA_VERSION is jdk-25.0.5.1+2, the tag says jdk-25.0.6+7",
            f"{ALPINE}: linux/amd64: cacerts holds emSign Root CA - C1",
        )

    def test_prints_the_cacerts_line_on_a_version_mismatch(self):
        self.write_dockerfile(ALPINE, f"eclipse-temurin:25.0.6_7-jre-alpine@sha256:{DIGEST}")

        result = self.assert_fails(f"{ALPINE}: linux/amd64: JAVA_VERSION is")
        self.assertIn(f"{ALPINE}: linux/amd64: cacerts holds 4 certificates, none of the 4 forbidden roots", result.stdout)
        self.assertNotIn(f"{ALPINE}: linux/amd64: JAVA_VERSION jdk", result.stdout)

    def test_fails_for_a_missing_dockerfile(self):
        (self.root / ALPINE).unlink()

        self.assert_fails(f"FAIL {ALPINE}: ", "No such file or directory")


def runpy_names() -> dict:
    return runpy.run_path(str(REPOSITORY / "scripts/check_base_images.py"))


FORBIDDEN_ROOTS = runpy_names()["FORBIDDEN_ROOTS"]


if __name__ == "__main__":
    unittest.main()

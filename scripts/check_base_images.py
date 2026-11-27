#!/usr/bin/env python3
"""Check the Temurin base image each Dockerfile pins.

For every Dockerfile: the pinned digest's JAVA_VERSION matches the tag's
version, and the base's cacerts holds none of the roots the compromised eMudhra
intermediates chain to.
Needs Docker with buildx, keytool and network access.
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Keep in step with DOCKERFILE_* in scripts/lib/images.sh.
DOCKERFILES = ("src/main/docker/Dockerfile.alpine", "src/main/docker/Dockerfile.ubuntu")
# Keep in step with .claude/rules/docker-conventions.md, "Trusted roots come from the Temurin base".
FORBIDDEN_ROOTS = (
    "emSign ECC Root CA - G3",
    "emSign ECC Root CA - C3",
    "emSign Root CA - C1",
    "AAA Certificate Services",
)
# Keep in step with PLATFORMS in scripts/lib/images.sh.
RELEASE_PLATFORMS = ("linux/amd64", "linux/arm64")
# Well under the workflow's `timeout-minutes: 10`, so a hung command fails here with its message.
COMMAND_TIMEOUT_SECONDS = 180

REFERENCE = re.compile(
    r"eclipse-temurin:(?P<version>\d+(?:\.\d+)*)_(?P<build>\d+)-jre-[a-z]+@sha256:[0-9a-f]{64}"
)
# Every line whose first token is the keyword counts, whatever follows it.
FROM_LINE = re.compile(r"^[ \t]*FROM\b.*$", re.IGNORECASE | re.MULTILINE)
FROM_FORM = re.compile(r"FROM[ \t]+(?P<reference>\S+)(?:[ \t]+AS[ \t]+\S+)?", re.IGNORECASE)
JAVA_HOME_PATH = re.compile(r"/[A-Za-z0-9._/-]+")


class CheckError(Exception):
    """A failed check, with the message to print."""


def run(command: list[str], stdin: str | None = None) -> str:
    """Return the command's output; a command that fails or cannot run fails the check."""
    name = " ".join(command[:3])
    try:
        result = subprocess.run(
            command, input=stdin, capture_output=True, text=True, check=False, timeout=COMMAND_TIMEOUT_SECONDS
        )
    except (OSError, subprocess.TimeoutExpired) as error:
        raise CheckError(f"`{name}` could not run: {error}") from error
    if result.returncode != 0:
        # keytool writes its errors to stdout, with a stack trace after the first line.
        stdout_lines = result.stdout.strip().splitlines()
        detail = result.stderr.strip() or (stdout_lines[0] if stdout_lines else "")
        raise CheckError(f"`{name}` exited {result.returncode}: {detail}")
    return result.stdout


def parse_reference(dockerfile: Path) -> tuple[str, str]:
    """Return the image reference and the expected JAVA_VERSION of the single FROM."""
    instructions = FROM_LINE.findall(dockerfile.read_text())
    if len(instructions) != 1:
        raise CheckError(f"expected exactly one FROM, found {len(instructions)}")
    line = instructions[0].strip()
    form = FROM_FORM.fullmatch(line)
    if form is None:
        raise CheckError(f"unsupported FROM line {line!r}: expected FROM <reference> [AS <name>]")
    reference = form["reference"]
    match = REFERENCE.fullmatch(reference)
    if match is None:
        raise CheckError(
            f"FROM {reference} is not eclipse-temurin:<version>_<build>-jre-<distro>@sha256:<digest>"
        )
    return reference, f"jdk-{match['version']}+{match['build']}"


def platform_configs(reference: str) -> dict[str, dict]:
    output = run(
        ["docker", "buildx", "imagetools", "inspect", reference, "--format", "{{json .Image}}"]
    )
    try:
        image = json.loads(output)
    except json.JSONDecodeError as error:
        raise CheckError(f"image inspect output is not JSON: {error}") from error
    if not isinstance(image, dict):
        raise CheckError("image inspect output is not a JSON object")
    configs = (
        {f"{image.get('os', '?')}/{image.get('architecture', '?')}": image}
        if "config" in image
        else image
    )
    for platform, config in configs.items():
        if not has_expected_shape(config):
            raise CheckError(f"image inspect output has an unexpected shape for {platform}")
    return configs


def has_expected_shape(config: object) -> bool:
    """Whether the platform value is an object whose `config` is an object with a list of string `Env`."""
    if not isinstance(config, dict):
        return False
    inner = config.get("config")
    if inner is None:
        return True
    if not isinstance(inner, dict):
        return False
    environment = inner.get("Env")
    return environment is None or (
        isinstance(environment, list) and all(isinstance(entry, str) for entry in environment)
    )


def env_value(config: dict, name: str) -> str | None:
    environment = (config.get("config") or {}).get("Env") or []
    prefix = f"{name}="
    return next(
        (entry.removeprefix(prefix) for entry in environment if entry.startswith(prefix)), None
    )


def check_versions(configs: dict[str, dict], expected: str) -> tuple[dict[str, str], list[str]]:
    """Return the JAVA_VERSION of each platform that matches the tag, and the failures."""
    matched: dict[str, str] = {}
    failures = [
        f"{platform}: platform missing from the image index"
        for platform in RELEASE_PLATFORMS
        if platform not in configs
    ]
    for platform, config in configs.items():
        found = env_value(config, "JAVA_VERSION")
        if found is None:
            failures.append(f"{platform}: JAVA_VERSION missing from the image config")
        elif found != expected:
            failures.append(f"{platform}: JAVA_VERSION is {found}, the tag says {expected}")
        else:
            matched[platform] = found
    return matched, failures


def extract_cacerts(reference: str, platform: str, java_home: str, directory: Path) -> Path:
    """Copy the platform's cacerts out of the image without emulation or a container."""
    context = directory / "context"
    context.mkdir()
    dockerfile = (
        f"FROM {reference} AS base\n"
        "FROM scratch\n"
        f"COPY --from=base {java_home}/lib/security/cacerts /cacerts\n"
    )
    output = directory / "out"
    run(
        ["docker", "buildx", "build", "--platform", platform, "--output", f"type=local,dest={output}", "-f", "-", str(context)],
        stdin=dockerfile,
    )
    return output / "cacerts"


def owners(cacerts: Path) -> list[str]:
    """Return each certificate's subject. Any other language's keytool prints no `Owner:` line."""
    output = run(
        ["keytool", "-J-Duser.language=en", "-list", "-v", "-keystore", str(cacerts), "-storepass", "changeit"]
    )
    lines = [
        line.removeprefix("Owner: ") for line in output.splitlines() if line.startswith("Owner: ")
    ]
    if not lines:
        raise CheckError("keytool listed no `Owner:` line, so nothing was checked")
    return lines


def common_names(owner: str) -> set[str]:
    return {match.group(1) for match in re.finditer(r"CN=([^,]*)(?:,|$)", owner)}


def check_cacerts(reference: str, platform: str, config: dict) -> int:
    """Return the number of certificates, or raise CheckError naming the roots found."""
    java_home = env_value(config, "JAVA_HOME")
    if java_home is None:
        raise CheckError("JAVA_HOME missing from the image config")
    if JAVA_HOME_PATH.fullmatch(java_home) is None:
        raise CheckError(f"JAVA_HOME is not a plain absolute path: {java_home!r}")
    with tempfile.TemporaryDirectory() as directory:
        subjects = owners(extract_cacerts(reference, platform, java_home, Path(directory)))
    names = {name for subject in subjects for name in common_names(subject)}
    found = sorted(set(FORBIDDEN_ROOTS) & names)
    if found:
        raise CheckError(f"cacerts holds {', '.join(found)}")
    return len(subjects)


def check_dockerfile(relative: str) -> tuple[list[str], list[str]]:
    """Return the success lines and the failures for one Dockerfile."""
    try:
        reference, expected = parse_reference(ROOT / relative)
        configs = platform_configs(reference)
    except (CheckError, OSError) as error:
        return [], [f"{relative}: {error}"]
    matched, version_failures = check_versions(configs, expected)
    successes = [
        f"{relative}: {platform}: JAVA_VERSION {found} matches the tag"
        for platform, found in matched.items()
    ]
    failures = [f"{relative}: {failure}" for failure in version_failures]
    for platform in RELEASE_PLATFORMS:
        if platform not in configs:
            continue
        try:
            count = check_cacerts(reference, platform, configs[platform])
        except CheckError as error:
            failures.append(f"{relative}: {platform}: {error}")
        else:
            successes.append(
                f"{relative}: {platform}: cacerts holds {count} certificates, "
                f"none of the {len(FORBIDDEN_ROOTS)} forbidden roots"
            )
    return successes, failures


def main() -> int:
    failures: list[str] = []
    for relative in DOCKERFILES:
        successes, problems = check_dockerfile(relative)
        for line in successes:
            print(line)
        failures += problems
    for failure in failures:
        print(f"FAIL {failure}", file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())

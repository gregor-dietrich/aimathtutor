#!/usr/bin/env python3
"""Verify committed frontend deps against pinned minimums and Vaadin's own version manifests."""

import json
import os
import re
import sys
import zipfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple, cast

# Repo root is the parent of the scripts/ directory holding this file, so the
# check works regardless of the caller's current working directory.
REPO_ROOT: Path = Path(__file__).resolve().parent.parent

# Security-sensitive Flow "default dependencies" that Vaadin's frontend generator
# can silently re-pin below a safe minimum on rebuild. One "<npm-package>": "<min>".
MIN_PINS: Dict[str, str] = {"react-router": "7.15.0"}

# A concrete dotted version (e.g. "25.2.0"); excludes npm "$ref" overrides and "$var".
SEMVER = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+")

# Remedy printed when a @vaadin/* component has drifted off its manifest version.
REGEN_HINT = (
    "Run 'scripts/regen-frontend.sh' to regenerate the manifest at the current Vaadin "
    "version, or just run 'make install', which now does this automatically."
)


def read_vaadin_version(pom_path: Path) -> str:
    """Extract <vaadin.version> from the root pom.xml properties block."""
    text = pom_path.read_text(encoding="utf-8")
    match = re.search(r"<vaadin\.version>\s*([0-9]+\.[0-9]+\.[0-9]+)\s*</vaadin\.version>", text)
    if not match:
        raise ValueError(f"Could not read <vaadin.version> from {pom_path}.")
    return match.group(1)


def maven_local_repo() -> Path:
    """Resolve the Maven local repository path from ~/.m2/settings.xml or the default."""
    settings = Path.home() / ".m2" / "settings.xml"
    if settings.is_file():
        match = re.search(r"<localRepository>\s*([^<]+?)\s*</localRepository>", settings.read_text(encoding="utf-8"))
        if match:
            return Path(os.path.expanduser(match.group(1).strip()))
    return Path.home() / ".m2" / "repository"


def collect_manifest_versions(obj: Any, out: Dict[str, str]) -> None:
    """Recursively record npmName -> jsVersion pairs from a Vaadin versions manifest."""
    if isinstance(obj, dict):
        mapping = cast(Dict[str, Any], obj)
        npm = mapping.get("npmName")
        js = mapping.get("jsVersion")
        if isinstance(npm, str) and isinstance(js, str):
            out.setdefault(npm, js)
        for value in mapping.values():
            collect_manifest_versions(value, out)
    elif isinstance(obj, list):
        for value in cast(List[Any], obj):
            collect_manifest_versions(value, out)


def load_bundle_versions(jar_path: Path, out: Dict[str, str]) -> None:
    """Fill in @vaadin/* versions from a Vaadin bundle jar's package-lock.json (gaps only)."""
    with zipfile.ZipFile(jar_path) as jar:
        members = [n for n in jar.namelist() if n.endswith("/package-lock.json") and "node_modules" not in n]
        if not members:
            return
        lock = cast(Dict[str, Any], json.loads(jar.read(members[0])))
    packages = cast(Dict[str, Any], lock.get("packages", {}))
    for key, raw_entry in packages.items():
        name = key.split("node_modules/")[-1]
        if name.startswith("@vaadin/") and isinstance(raw_entry, dict):
            entry = cast(Dict[str, Any], raw_entry)
            version = entry.get("version")
            if isinstance(version, str):
                out.setdefault(name, version)


def load_expected_versions(repo: Path, version: str) -> Dict[str, str]:
    """Build the authoritative npmName -> version map from resolved Vaadin jars."""
    core_jar = repo / "com" / "vaadin" / "vaadin-core-internal" / version / f"vaadin-core-internal-{version}.jar"
    if not core_jar.is_file():
        raise FileNotFoundError(
            f"Vaadin manifest jar not found: {core_jar}\n"
            "Run 'mvn compile' or 'make install' first so the Vaadin jars are resolved locally."
        )
    expected: Dict[str, str] = {}
    with zipfile.ZipFile(core_jar) as jar:
        collect_manifest_versions(json.loads(jar.read("vaadin-core-versions.json")), expected)

    # A few @vaadin/* packages (e.g. common-frontend, vaadin-themable-mixin) are shipped
    # by Vaadin but omitted from vaadin-core-versions.json. Vaadin's pre-built bundle jar
    # carries their resolved versions; use it to fill the gaps (core manifest still wins).
    for artifact in ("vaadin-dev-bundle", "vaadin-prod-bundle"):
        bundle_jar = repo / "com" / "vaadin" / artifact / version / f"{artifact}-{version}.jar"
        if bundle_jar.is_file():
            load_bundle_versions(bundle_jar, expected)
            break
    return expected


def collect_pkg_json_vaadin(obj: Any, out: Dict[str, Set[str]]) -> None:
    """Recursively collect concrete @vaadin/* version declarations from package.json."""
    if isinstance(obj, dict):
        mapping = cast(Dict[str, Any], obj)
        for key, value in mapping.items():
            if isinstance(value, str) and key.startswith("@vaadin/") and SEMVER.match(value):
                out.setdefault(key, set()).add(value)
            else:
                collect_pkg_json_vaadin(value, out)
    elif isinstance(obj, list):
        for value in cast(List[Any], obj):
            collect_pkg_json_vaadin(value, out)


def collect_pkg_json_pin(obj: Any, pkg: str, out: Set[str]) -> None:
    """Recursively collect concrete version declarations for a specific package name."""
    if isinstance(obj, dict):
        mapping = cast(Dict[str, Any], obj)
        for key, value in mapping.items():
            if key == pkg and isinstance(value, str) and SEMVER.match(value):
                out.add(value)
            else:
                collect_pkg_json_pin(value, pkg, out)
    elif isinstance(obj, list):
        for value in cast(List[Any], obj):
            collect_pkg_json_pin(value, pkg, out)


def collect_lock_versions(lock: Dict[str, Any], prefix: str) -> Dict[str, Set[str]]:
    """Collect resolved versions from package-lock.json for packages under a name prefix."""
    out: Dict[str, Set[str]] = {}
    packages = lock.get("packages", {})
    if isinstance(packages, dict):
        packages_map = cast(Dict[str, Any], packages)
        for key, raw_entry in packages_map.items():
            name = key.split("node_modules/")[-1]
            if name.startswith(prefix) and isinstance(raw_entry, dict):
                entry = cast(Dict[str, Any], raw_entry)
                version = entry.get("version")
                if isinstance(version, str):
                    out.setdefault(name, set()).add(version)
    return out


def parse_version(value: str) -> Tuple[int, ...]:
    """Parse a dotted version string into a tuple of integers for ordered comparison."""
    parts: List[int] = []
    for chunk in value.split("."):
        digits = re.match(r"[0-9]+", chunk)
        parts.append(int(digits.group(0)) if digits else 0)
    return tuple(parts)


def check_min_pins(pkg_json: Dict[str, Any], pkg_lock: Optional[Dict[str, Any]]) -> List[str]:
    """Check that security-pinned packages meet their minimum version in both files."""
    errors: List[str] = []
    for pkg, minimum in MIN_PINS.items():
        json_versions: Set[str] = set()
        collect_pkg_json_pin(pkg_json, pkg, json_versions)
        if not json_versions:
            errors.append(f"{pkg} missing version in package.json (require >= {minimum}).")
        for found in sorted(json_versions):
            if parse_version(found) < parse_version(minimum):
                errors.append(f"{pkg} is {found} in package.json (require >= {minimum}).")

        if pkg_lock is not None:
            lock_versions = collect_lock_versions(pkg_lock, pkg).get(pkg, set())
            if not lock_versions:
                errors.append(f"{pkg} missing version in package-lock.json (require >= {minimum}).")
            for found in sorted(lock_versions):
                if parse_version(found) < parse_version(minimum):
                    errors.append(f"{pkg} is {found} in package-lock.json (require >= {minimum}).")
    return errors


def check_vaadin_versions(
    label: str, found: Dict[str, Set[str]], expected: Dict[str, str]
) -> Tuple[List[str], bool]:
    """Check that every @vaadin/* version matches Vaadin's manifest; returns (errors, drift)."""
    errors: List[str] = []
    drift = False
    for pkg in sorted(found):
        for version in sorted(found[pkg]):
            if pkg not in expected:
                errors.append(f"{pkg} ({version}) in {label} has no entry in the Vaadin version manifest.")
            elif version != expected[pkg]:
                errors.append(f"{pkg} is {version} in {label}, expected {expected[pkg]} per the Vaadin manifest.")
                drift = True
    return errors, drift


def main() -> None:
    errors: List[str] = []
    drift = False

    pkg_json_path = REPO_ROOT / "package.json"
    pkg_lock_path = REPO_ROOT / "package-lock.json"
    pom_path = REPO_ROOT / "pom.xml"

    print("Checking pinned frontend dependencies...")
    pkg_json: Dict[str, Any] = json.loads(pkg_json_path.read_text(encoding="utf-8"))
    pkg_lock: Optional[Dict[str, Any]] = None
    if pkg_lock_path.is_file():
        pkg_lock = json.loads(pkg_lock_path.read_text(encoding="utf-8"))

    # Check 1: security-sensitive minimum version pins.
    errors.extend(check_min_pins(pkg_json, pkg_lock))

    # Check 2: @vaadin/* components must match Vaadin's own version manifest.
    version = read_vaadin_version(pom_path)
    print(f"Verifying @vaadin/* components against Vaadin {version} manifest...")
    try:
        expected = load_expected_versions(maven_local_repo(), version)
    except (FileNotFoundError, KeyError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        print("Frontend dependency check FAILED.", file=sys.stderr)
        sys.exit(1)

    json_vaadin: Dict[str, Set[str]] = {}
    collect_pkg_json_vaadin(pkg_json, json_vaadin)
    json_errors, json_drift = check_vaadin_versions("package.json", json_vaadin, expected)
    errors.extend(json_errors)
    drift = drift or json_drift

    if pkg_lock is not None:
        lock_vaadin = collect_lock_versions(pkg_lock, "@vaadin/")
        lock_errors, lock_drift = check_vaadin_versions("package-lock.json", lock_vaadin, expected)
        errors.extend(lock_errors)
        drift = drift or lock_drift

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        if drift:
            print(f"       {REGEN_HINT}", file=sys.stderr)
        print("Frontend dependency check FAILED.", file=sys.stderr)
        sys.exit(1)

    print("Frontend dependency check passed.")


if __name__ == "__main__":
    main()

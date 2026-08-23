#!/usr/bin/env python3
"""Report pom.xml version properties whose artifacts have a newer release on Maven Central.

A version property is the unit of work: it is what you edit to bump, and one property can
pin several artifacts (${vaadin.version} covers three). The artifacts behind each property
are resolved from the pom so Maven Central can be queried by real groupId:artifactId.

Multi-module projects are scanned as the root pom plus every <modules> pom; module poms
inherit the root pom's properties, and dependencies between the project's own modules are
skipped (they are not published to Maven Central).
"""

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import NamedTuple

import requests

# Repo root is the parent of the scripts/ directory holding this file, so the
# check works regardless of the caller's current working directory.
REPO_ROOT: Path = Path(__file__).resolve().parent.parent
POM_PATH: Path = REPO_ROOT / "pom.xml"

POM_NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# Elements that declare a Maven coordinate: plain dependencies (incl. dependencyManagement),
# build plugins (incl. the ones inside profiles), annotationProcessorPaths entries and
# build extensions.
COORDINATE_TAGS = ("dependency", "plugin", "path", "extension")

# <plugin> may omit <groupId>; Maven then assumes its own plugin group.
DEFAULT_PLUGIN_GROUP = "org.apache.maven.plugins"

MAVEN_CENTRAL = "https://repo1.maven.org/maven2"
REQUEST_TIMEOUT = 15

PLACEHOLDER = re.compile(r"\$\{([^}]+)\}")
SINGLE_PLACEHOLDER = re.compile(r"^\$\{([^}]+)\}$")

# Guard against a property cycle (${a} -> ${b} -> ${a}) while resolving placeholders.
MAX_RESOLVE_DEPTH = 10


class Coordinate(NamedTuple):
    """A Maven groupId:artifactId."""

    group: str
    artifact: str

    def __str__(self) -> str:
        return f"{self.group}:{self.artifact}"


class Pin(NamedTuple):
    """One edit site in the pom: a version property and every artifact it pins."""

    name: str  # property name, or "group:artifact" when a version is hardcoded
    version: str
    coordinates: list[Coordinate]
    is_property: bool


class PomScan(NamedTuple):
    """Everything the pom parse yields."""

    pins: list[Pin]
    artifact_count: int
    notes: list[str]  # human-readable reasons for declarations that were skipped
    unmapped_properties: list[str]  # *.version properties no artifact resolves to


def get_properties(root: ET.Element) -> dict[str, str]:
    """Collect every <properties> entry in the pom (project level plus profiles)."""
    properties: dict[str, str] = {}
    for block in root.iterfind(".//m:properties", POM_NS):
        for entry in block:
            name = entry.tag.split("}")[-1]
            properties.setdefault(name, (entry.text or "").strip())
    return properties


def resolve(value: str, properties: dict[str, str], depth: int = 0) -> str | None:
    """Expand ${...} placeholders from the pom properties; None if any cannot be resolved."""
    if "${" not in value:
        return value
    if depth >= MAX_RESOLVE_DEPTH:
        return None
    resolved = value
    for name in PLACEHOLDER.findall(value):
        replacement = properties.get(name)
        if replacement is None:
            return None
        expanded = resolve(replacement, properties, depth + 1)
        if expanded is None:
            return None
        resolved = resolved.replace(f"${{{name}}}", expanded)
    return resolved


def child_text(element: ET.Element, name: str) -> str | None:
    """Return the trimmed text of a direct child element, or None if absent/empty."""
    child = element.find(f"m:{name}", POM_NS)
    if child is None or child.text is None:
        return None
    text = child.text.strip()
    return text or None


def project_coordinate(root: ET.Element) -> Coordinate | None:
    """The pom's own groupId:artifactId, with the groupId inherited from <parent> if absent."""
    artifact = child_text(root, "artifactId")
    group = child_text(root, "groupId")
    if group is None:
        parent = root.find("m:parent", POM_NS)
        if parent is not None:
            group = child_text(parent, "groupId")
    return Coordinate(group, artifact) if group and artifact else None


def find_pom_paths(root_pom: Path) -> list[Path]:
    """The root pom followed by the pom of every <modules> entry it declares."""
    poms = [root_pom]
    root = ET.parse(root_pom).getroot()
    for module in root.iterfind(".//m:modules/m:module", POM_NS):
        module_pom = root_pom.parent / (module.text or "").strip() / "pom.xml"
        if module_pom.is_file() and module_pom not in poms:
            poms.append(module_pom)
    return poms


def get_version_pins(pom_paths: list[Path]) -> PomScan:
    """Group every version-pinned artifact in the poms under the property that pins it.

    Artifacts without a version of their own are managed by an imported BOM and cannot be
    bumped here; those and any unresolvable declaration are summarised in the notes instead.
    """
    roots = [ET.parse(path).getroot() for path in pom_paths]
    # Module poms inherit the root pom's properties; a module's own definition wins.
    base_properties = get_properties(roots[0])
    # The reactor's own modules are not published to Maven Central; dependencies between
    # them are skipped rather than looked up.
    reactor = {c for c in (project_coordinate(root) for root in roots) if c is not None}

    pins: dict[str, Pin] = {}
    notes: list[str] = []
    all_properties: dict[str, str] = dict(base_properties)
    artifacts = 0
    managed = 0
    internal = 0

    for root in roots:
        properties = {**base_properties, **get_properties(root)}
        all_properties.update(properties)

        for element in root.iter():
            tag = element.tag.split("}")[-1]
            if tag not in COORDINATE_TAGS:
                continue

            raw_artifact = child_text(element, "artifactId")
            if raw_artifact is None:
                continue  # e.g. spotless' <eclipse> block, which is not a coordinate

            raw_version = child_text(element, "version")
            if raw_version is None:
                managed += 1  # version comes from an imported BOM; nothing to bump here
                continue

            raw_group = child_text(element, "groupId")
            if raw_group is None:
                if tag != "plugin":
                    continue
                raw_group = DEFAULT_PLUGIN_GROUP

            group = resolve(raw_group, properties)
            artifact = resolve(raw_artifact, properties)
            if group is None or artifact is None:
                notes.append(f"{raw_group}:{raw_artifact}:{raw_version} — unresolved property, skipped")
                continue

            coordinate = Coordinate(group, artifact)
            if coordinate in reactor:
                internal += 1
                continue

            version = resolve(raw_version, properties)
            if version is None:
                notes.append(f"{raw_group}:{raw_artifact}:{raw_version} — unresolved property, skipped")
                continue

            match = SINGLE_PLACEHOLDER.match(raw_version)
            # A hardcoded version has no property to bump, so the coordinate itself is the edit site.
            name = match.group(1) if match else str(coordinate)

            pin = pins.get(name)
            if pin is None:
                pins[name] = Pin(name, version, [coordinate], match is not None)
                artifacts += 1
            elif coordinate not in pin.coordinates:
                # The same property legitimately pins several artifacts; a differing version under
                # one name means a profile overrode the property, which changes what a bump means.
                if version != pin.version:
                    notes.append(f"{name} resolves to both {pin.version} and {version}, checking {pin.version}")
                pin.coordinates.append(coordinate)
                artifacts += 1

    if managed:
        notes.append(f"{managed} artifact(s) without an explicit version (managed by a BOM), skipped")
    if internal:
        notes.append(f"{internal} dependency declaration(s) on the project's own modules, skipped")

    unmapped = sorted(name for name in all_properties if name.endswith(".version") and name not in pins)
    return PomScan(sorted(pins.values()), artifacts, notes, unmapped)


def is_stable(version: str) -> bool:
    """True for purely numeric versions; rejects 4.0.0-beta-4, 3.38.0.CR1, 26.0.alpha1, ..."""
    return all(chunk.isdigit() for chunk in version.split("."))


def parse_version(version: str) -> tuple[int, ...]:
    """Parse a numeric version into a tuple of ints for ordered comparison."""
    return tuple(int(chunk) for chunk in version.split("."))


def get_latest_version(group: str, artifact: str, session: requests.Session | None = None) -> str | None:
    """Return the newest stable version of an artifact on Maven Central, or None on failure.

    Central's <latest>/<release> markers routinely point at pre-releases (maven-compiler-plugin
    advertises 4.0.0-beta-4, quarkus-bom 3.38.0.CR1), which would report false upgrades, so the
    stable entries of <versions> decide. The markers are only a fallback for artifacts that
    publish no purely numeric version at all.
    """
    url = f"{MAVEN_CENTRAL}/{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    get = session.get if session is not None else requests.get
    try:
        response = get(url, timeout=REQUEST_TIMEOUT)
    except requests.RequestException as exc:
        print(f"Error: {group}:{artifact} — {exc}", file=sys.stderr)
        return None
    if response.status_code != 200:
        print(f"Error: status {response.status_code} for {group}:{artifact}", file=sys.stderr)
        return None

    try:
        versioning = ET.fromstring(response.content).find("versioning")
    except ET.ParseError as exc:
        print(f"Error: {group}:{artifact} — malformed metadata ({exc})", file=sys.stderr)
        return None
    if versioning is None:
        print(f"Error: {group}:{artifact} — metadata without <versioning>", file=sys.stderr)
        return None

    stable = [
        (candidate.text or "").strip()
        for candidate in versioning.iterfind("versions/version")
        if is_stable((candidate.text or "").strip())
    ]
    if stable:
        return max(stable, key=parse_version)

    for marker in ("release", "latest"):
        element = versioning.find(marker)
        if element is not None and element.text and element.text.strip():
            return element.text.strip()

    print(f"Error: {group}:{artifact} — no version found in metadata", file=sys.stderr)
    return None


def get_bump_target(pin: Pin, session: requests.Session) -> tuple[str | None, list[str]]:
    """Find the newest version publishable for every artifact a pin covers, plus any notes.

    One property bumped to a version that not all of its artifacts have released would break
    the build, so the lowest of their latest releases wins (they usually agree).
    """
    notes: list[str] = []
    latest_per_artifact: dict[Coordinate, str] = {}
    for coordinate in pin.coordinates:
        latest = get_latest_version(coordinate.group, coordinate.artifact, session)
        if latest is None:
            notes.append(f"could not check {coordinate}")
            continue
        latest_per_artifact[coordinate] = latest

    if not latest_per_artifact:
        return None, notes

    candidates = list(latest_per_artifact.values())
    stable = [version for version in candidates if is_stable(version)]
    target = min(stable, key=parse_version) if stable else candidates[0]

    if len(set(candidates)) > 1:
        ahead = sorted(f"{coordinate} at {v}" for coordinate, v in latest_per_artifact.items() if v != target)
        notes.append(f"{pin.name} held at {target} by its oldest artifact; newer available: {', '.join(ahead)}")
    return target, notes


def compare(current: str, latest: str) -> str:
    """Classify a pinned version against the latest stable release."""
    if current == latest:
        return "current"
    if not is_stable(current):
        return "prerelease"  # pinned pre-release; numeric comparison would be misleading
    return "outdated" if parse_version(latest) > parse_version(current) else "ahead"


def describe(pin: Pin) -> str:
    """Render a pin's artifacts, dropping the groupId when it is shared by all of them."""
    if len(pin.coordinates) == 1:
        return str(pin.coordinates[0])
    groups = {coordinate.group for coordinate in pin.coordinates}
    if len(groups) == 1:
        return f"{groups.pop()}:({', '.join(c.artifact for c in pin.coordinates)})"
    return ", ".join(str(coordinate) for coordinate in pin.coordinates)


def main() -> None:
    if not POM_PATH.is_file():
        print(f"ERROR: {POM_PATH} not found.", file=sys.stderr)
        sys.exit(1)

    scan = get_version_pins(find_pom_paths(POM_PATH))
    print(
        f"Checking {len(scan.pins)} version properties "
        f"({scan.artifact_count} artifacts) against Maven Central...\n"
    )

    results: list[tuple[Pin, str, str]] = []  # pin, latest, status
    unchecked: list[Pin] = []
    notes = list(scan.notes)

    with requests.Session() as session:
        for pin in scan.pins:
            target, pin_notes = get_bump_target(pin, session)
            notes.extend(pin_notes)
            if target is None:
                unchecked.append(pin)
                continue
            results.append((pin, target, compare(pin.version, target)))

    outdated = [entry for entry in results if entry[2] == "outdated"]
    if outdated:
        width = max(len(pin.name) for pin, _, _ in outdated)
        print(f"Outdated ({len(outdated)}):")
        for pin, target, _ in outdated:
            arrow = f"{pin.version} -> {target}"
            print(f"  {pin.name:<{width}}  {arrow:<22}  {describe(pin)}")
        print()

    print(f"Up to date: {sum(1 for entry in results if entry[2] == 'current')}")
    for pin, target, status in results:
        if status == "ahead":
            print(f"  {pin.name} pinned at {pin.version}, newer than Central's {target}")
        elif status == "prerelease":
            print(f"  {pin.name} pinned at pre-release {pin.version} (latest stable {target})")

    if unchecked:
        print(f"\nCould not check ({len(unchecked)}):")
        for pin in unchecked:
            print(f"  {pin.name} ({pin.version})")

    if notes or scan.unmapped_properties:
        print("\nNotes:")
        for note in notes:
            print(f"  {note}")
        for name in scan.unmapped_properties:
            print(f"  property {name} pins no Maven artifact, not checked")


if __name__ == "__main__":
    main()

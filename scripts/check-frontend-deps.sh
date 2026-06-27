#!/bin/bash

# Guards against Vaadin's frontend build silently reverting security-sensitive npm
# dependencies below their pinned minimum. Vaadin re-pins its bundled "default
# dependencies" (e.g. react-router) on every prepare/build-frontend; the
# @NpmPackage annotations in the Java sources keep them pinned. This check fails
# the build if a regression slips into package.json or package-lock.json.

. "$(dirname "$0")"/lib/get_dir.sh

set -e

cd "$DIR/.."

# Minimum allowed versions, one "<npm-package> <min-version>" per line.
PINS="react-router 7.15.0"

# True if $1 >= $2 (dotted numeric versions).
version_ge() {
    [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

fail=0

check_version() {
    local label="$1" pkg="$2" min="$3" found="$4"
    # Fail closed: a pinned dependency with no concrete version found is a problem,
    # not a pass.
    if [ -z "$found" ]; then
        echo "ERROR: $pkg missing version in $label (require >= $min)." >&2
        fail=1
        return 0
    fi
    if ! version_ge "$found" "$min"; then
        echo "ERROR: $pkg is $found in $label (require >= $min)." >&2
        fail=1
    fi
}

while read -r pkg min; do
    [ -z "$pkg" ] && continue

    # Every concrete (non "$var") version declared for the package in package.json:
    # the top-level dependencies block and the vaadin.dependencies block. If none is
    # found the dependency is treated as missing and rejected (fail closed).
    pkg_versions=$(grep -oE "\"$pkg\"[[:space:]]*:[[:space:]]*\"[0-9][^\"]*\"" package.json \
        | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' || true)
    if [ -z "$pkg_versions" ]; then
        check_version "package.json" "$pkg" "$min" ""
    else
        while read -r v; do
            check_version "package.json" "$pkg" "$min" "$v"
        done <<< "$pkg_versions"
    fi

    # Resolved version recorded in the lockfile (node_modules/<pkg> entry).
    if [ -f package-lock.json ]; then
        lock_v=$(grep -A3 "\"node_modules/$pkg\"" package-lock.json \
            | grep -oE '"version"[[:space:]]*:[[:space:]]*"[0-9]+\.[0-9]+\.[0-9]+"' \
            | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1 || true)
        check_version "package-lock.json" "$pkg" "$min" "$lock_v"
    fi
done <<EOF
$PINS
EOF

# Vaadin platform component version uniformity.
# Every @vaadin/* npm package on the platform version line (same major as
# <vaadin.version> in pom.xml) must match that exact version. Guards against
# Vaadin's frontend generator freezing stale component versions in the npm
# "overrides" block: it adds new entries at the current version but never re-bumps
# existing ones, so a platform upgrade can silently leave components pinned to the
# old minor (a mixed, unsupported frontend). Independently-versioned @vaadin
# packages (common-frontend, router, vaadin-usage-statistics, ...) use a different
# major and are intentionally excluded.
platform_version=$(grep -oE '<vaadin\.version>[0-9]+\.[0-9]+\.[0-9]+</vaadin\.version>' pom.xml \
    | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
if [ -z "$platform_version" ]; then
    echo "ERROR: could not read <vaadin.version> from pom.xml." >&2
    fail=1
else
    platform_major="${platform_version%%.*}"
    # package.json: literal "@vaadin/<name>": "<ver>" declarations (dependencies + overrides).
    json_drift=$(grep -oE '"@vaadin/[a-z0-9-]+"[[:space:]]*:[[:space:]]*"'"$platform_major"'\.[0-9]+\.[0-9]+"' package.json 2>/dev/null \
        | grep -oE "$platform_major"'\.[0-9]+\.[0-9]+' | sort -u | grep -vxF "$platform_version" || true)
    if [ -n "$json_drift" ]; then
        echo "ERROR: package.json has @vaadin/* platform components not at ${platform_version}: $(echo "$json_drift" | tr '\n' ' ')" >&2
        echo "       Run 'make regen-frontend' to regenerate the manifest at the current Vaadin version." >&2
        fail=1
    fi
    # package-lock.json: resolved versions taken from the tarball URLs.
    if [ -f package-lock.json ]; then
        lock_drift=$(grep -oE '/@vaadin/[a-z0-9-]+/-/[a-z0-9-]+-'"$platform_major"'\.[0-9]+\.[0-9]+\.tgz' package-lock.json \
            | grep -oE "$platform_major"'\.[0-9]+\.[0-9]+' | sort -u | grep -vxF "$platform_version" || true)
        if [ -n "$lock_drift" ]; then
            echo "ERROR: package-lock.json has resolved @vaadin/* platform components not at ${platform_version}: $(echo "$lock_drift" | tr '\n' ' ')" >&2
            echo "       Run 'make regen-frontend' to regenerate the manifest at the current Vaadin version." >&2
            fail=1
        fi
    fi
fi

if [ "$fail" -ne 0 ]; then
    echo "Frontend dependency check FAILED." >&2
    cd - > /dev/null
    exit 1
fi

echo "Frontend dependency check passed."

cd - > /dev/null

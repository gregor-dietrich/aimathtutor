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

if [ "$fail" -ne 0 ]; then
    echo "Frontend dependency check FAILED." >&2
    cd - > /dev/null
    exit 1
fi

echo "Frontend dependency check passed."

cd - > /dev/null

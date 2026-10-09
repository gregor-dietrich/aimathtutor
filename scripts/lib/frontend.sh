#!/bin/bash

# Frontend manifest checks shared by install.sh and release.sh. Builds install the frontend with npm ci from
# the committed package-lock.json, so no build resolves npm dependencies nobody reviewed; only
# `make regen-frontend` recreates the manifests. Source it and call its functions from the repository root.

FRONTEND_MANIFESTS=(package.json package-lock.json)

# Fail if a manifest is missing, instead of letting a build generate or resolve one.
require_frontend_manifest() {
    local manifest
    for manifest in "${FRONTEND_MANIFESTS[@]}"; do
        if [ ! -f "$manifest" ]; then
            echo "Error: $manifest is missing. Restore it with git, or run make regen-frontend and commit the result." >&2
            return 1
        fi
    done
}

# Fail unless the manifests are tracked and match HEAD, so a release ships exactly the committed lockfile.
require_committed_frontend_manifest() {
    if ! git ls-files --error-unmatch "${FRONTEND_MANIFESTS[@]}" > /dev/null 2>&1 \
        || ! git diff --quiet HEAD -- "${FRONTEND_MANIFESTS[@]}"; then
        echo "Error: package.json or package-lock.json is missing or differs from the committed version." >&2
        echo "A release builds from the committed lockfile: restore it, or run make regen-frontend and commit the result." >&2
        return 1
    fi
}

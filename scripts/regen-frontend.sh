#!/bin/bash

# Regenerates the committed frontend manifest (package.json / package-lock.json)
# from scratch at the current Vaadin platform version.
#
# Why deletion first: Vaadin only *updates* an existing package.json, and updating
# preserves stale entries in the npm "overrides" block (component versions get
# frozen at the minor they were first written and never re-bumped). The only way
# to clear that drift is to remove the manifest and let Vaadin generate it fresh.
#
# Why the regen-frontend profile: a plain build either skips the frontend (when a
# compatible bundle already exists) or, in production, deletes the generated npm
# files afterwards. The regen-frontend profile sets forceProductionBuild=true (so
# npm actually resolves) and cleanFrontendFiles=false (so the files are kept).

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Regenerating frontend manifest (package.json / package-lock.json)..."

rm -f package.json package-lock.json
rm -rf node_modules

${MVN_CMD} -q clean compile -Pregen-frontend -Drevision="${REVISION}"

if [ ! -f package.json ] || [ ! -f package-lock.json ]; then
    echo "ERROR: frontend manifest was not generated." >&2
    cd - > /dev/null
    exit 1
fi

echo "Frontend manifest regenerated. Review the diff before committing."

cd - > /dev/null

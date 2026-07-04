#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

echo "Running install..."

make check

REVISION=${REVISION:-1.0.0-SNAPSHOT}

${MVN_CMD} -q clean install -DskipTests -Drevision="${REVISION}"

echo "Checking frontend dependency manifest..."

if ! python3 "scripts/check_frontend_deps.py"; then
    echo "Frontend manifest is out of date for the current Vaadin version; regenerating..."
    scripts/regen-frontend.sh
    python3 "scripts/check_frontend_deps.py"
fi

echo "Install completed."

cd - > /dev/null

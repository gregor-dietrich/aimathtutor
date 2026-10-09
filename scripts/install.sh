#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"
. "$DIR/lib/frontend.sh"

set -e

cd "$DIR/.."

echo "Running install..."

make check

REVISION=${REVISION:-1.0.0-SNAPSHOT}

# The Quarkus build runs npm ci against the committed lockfile; never regenerate it here.
require_frontend_manifest

python3 "scripts/check_frontend_deps.py"

${MVN_CMD} -q clean install -DskipTests -Drevision="${REVISION}"

echo "Install completed."

cd - > /dev/null

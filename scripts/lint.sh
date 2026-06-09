#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Running lint checks..."

${MVN_CMD} -q compile test-compile spotless:check checkstyle:check spotbugs:check pmd:check pmd:cpd-check ${PL_ARG} -Drevision="${REVISION}"

echo "Checking pinned frontend dependencies..."

"$DIR/check-frontend-deps.sh"

echo "Lint checks completed."

cd - > /dev/null

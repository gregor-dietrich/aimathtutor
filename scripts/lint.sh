#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Running lint checks..."

${MVN_CMD} -q compile spotless:apply checkstyle:check spotbugs:check pmd:check pmd:cpd-check -Drevision="${REVISION}"

echo "Lint checks completed."

cd - > /dev/null

#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Running lint checks..."

${MVN_CMD} -q spotless:apply ${PL_ARG} -Drevision="${REVISION}"

echo "Lint checks completed."

cd - > /dev/null

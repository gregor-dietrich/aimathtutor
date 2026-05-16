#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Formatting Java sources..."

${MVN_CMD} -q spotless:apply ${PL_ARG} -Drevision="${REVISION}"

echo "Java source formatting completed."

cd - > /dev/null

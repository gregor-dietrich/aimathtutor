#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

REVISION=${REVISION:-1.0.0-SNAPSHOT}

echo "Running tests..."

${MVN_CMD} -q verify -Dquarkus.log.console.enabled=false -Dquarkus.log.file.enabled=false -Drevision="${REVISION}"

echo "Tests completed."

cd - > /dev/null

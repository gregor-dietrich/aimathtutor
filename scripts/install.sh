#!/bin/bash

. "$(dirname "$0")"/lib/get_dir.sh
. "$DIR/lib/get_maven.sh"

set -e

cd "$DIR/.."

echo "Running install..."

make check

REVISION=${REVISION:-1.0.0-SNAPSHOT}

${MVN_CMD} -q clean install -DskipTests -Drevision="${REVISION}"

echo "Install completed."

cd - > /dev/null
